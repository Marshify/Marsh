package com.grandline.gear;

import com.grandline.combat.EnergyService;
import com.grandline.data.ModAttachments;
import com.grandline.network.ServerRequestGuard;
import com.grandline.network.payload.GearSyncPayload;
import com.grandline.network.payload.UnequipGearPayload;
import com.grandline.player.StatType;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/**
 * Server-side accessory equipment: one accessory slot and two ring slots, saved with the player. While equipped, an
 * item's {@link GearBonuses} apply: damage percentages per combat category (read by ability damage, melee/sword hits
 * and gun shots, once per hit), plus max health and max energy. Changing gear never refills health or energy, and a
 * lower cap clamps the current value.
 */
public final class GearService {
	/** Saved with the player, like stat health, so relogging never clips health that the bonus allows. */
	private static final UUID HEALTH_MODIFIER = UUID.fromString("6f1d2c4e-0b5a-4f3e-9c1a-8a7e5d2b1c11");

	private GearService() {
	}

	public static void register() {
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> apply(handler.getPlayer()));
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> apply(newPlayer));
		ServerPlayNetworking.registerGlobalReceiver(UnequipGearPayload.TYPE, (packet, player, sender) -> {
			if (ServerRequestGuard.check(player, UnequipGearPayload.TYPE) != ServerRequestGuard.Result.ACCEPTED) {
				return;
			}
			if (packet.slot() < 0 || packet.slot() > 2) {
				ServerRequestGuard.reject(player, UnequipGearPayload.TYPE, ServerRequestGuard.Result.REJECTED_INVALID);
				return;
			}
			unequip(player, packet.slot());
		});
	}

	public static EquippedGear get(ServerPlayer player) {
		return player.getAttachedOrElse(ModAttachments.EQUIPPED_GEAR, EquippedGear.EMPTY);
	}

	/** Equips the accessory in {@code hand}. Returns the slot used, or -1 if the item is not an accessory. */
	public static int equip(ServerPlayer player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (!(stack.getItem() instanceof AccessoryItem accessory)) {
			return -1;
		}
		EquippedGear gear = get(player);
		int slot;
		if (accessory.slot() == AccessoryItem.Slot.ACCESSORY) {
			slot = EquippedGear.SLOT_ACCESSORY;
		} else {
			slot = gear.ring1().isEmpty() ? EquippedGear.SLOT_RING_1 : gear.ring2().isEmpty() ? EquippedGear.SLOT_RING_2 : EquippedGear.SLOT_RING_1;
		}
		String previous = gear.get(slot);
		String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
		stack.shrink(1);
		player.setAttached(ModAttachments.EQUIPPED_GEAR, gear.with(slot, id));
		giveBack(player, previous);
		apply(player);
		return slot;
	}

	/** Moves the item in {@code slot} back to the inventory (or drops it if full). */
	public static boolean unequip(ServerPlayer player, int slot) {
		EquippedGear gear = get(player);
		String previous = gear.get(slot);
		if (previous.isEmpty()) {
			return false;
		}
		player.setAttached(ModAttachments.EQUIPPED_GEAR, gear.with(slot, ""));
		giveBack(player, previous);
		apply(player);
		return true;
	}

	private static void giveBack(ServerPlayer player, String itemId) {
		if (itemId.isEmpty()) {
			return;
		}
		ResourceLocation id = ResourceLocation.tryParse(itemId);
		Item item = id == null ? null : BuiltInRegistries.ITEM.get(id);
		if (item == null || item == net.minecraft.world.item.Items.AIR) {
			return;
		}
		ItemStack returned = new ItemStack(item);
		if (!player.getInventory().add(returned)) {
			player.drop(returned, false);
		}
	}

	/** Total bonuses of everything equipped. */
	public static GearBonuses bonuses(ServerPlayer player) {
		EquippedGear gear = get(player);
		GearBonuses total = GearBonuses.NONE;
		for (int slot = 0; slot < 3; slot++) {
			ResourceLocation id = ResourceLocation.tryParse(gear.get(slot));
			if (id != null && BuiltInRegistries.ITEM.get(id) instanceof AccessoryItem accessory) {
				total = total.plus(accessory.kind().bonuses());
			}
		}
		return total;
	}

	/** Damage multiplier for a hit of this category: 1.0 plus the equipped bonuses (e.g. 1.15). */
	public static double damageMultiplier(ServerPlayer player, StatType category) {
		return 1.0 + bonuses(player).damage(category);
	}

	public static double bonusEnergy(ServerPlayer player) {
		return bonuses(player).energy();
	}

	/** Re-applies max health and energy caps (clamping, never refilling) and syncs the client. */
	public static void apply(ServerPlayer player) {
		GearBonuses bonuses = bonuses(player);
		AttributeInstance health = player.getAttribute(Attributes.MAX_HEALTH);
		if (health != null) {
			AttributeModifier existing = health.getModifier(HEALTH_MODIFIER);
			if (existing == null || existing.getAmount() != bonuses.health()) {
				health.removeModifier(HEALTH_MODIFIER);
				if (bonuses.health() != 0) {
					health.addPermanentModifier(new AttributeModifier(HEALTH_MODIFIER, "Grand Line gear", bonuses.health(),
							AttributeModifier.Operation.ADDITION));
				}
			}
		}
		if (player.getHealth() > player.getMaxHealth()) {
			player.setHealth(player.getMaxHealth());
		}
		EnergyService.clampToMax(player);
		EnergyService.sync(player, true);
		sync(player);
	}

	public static void sync(ServerPlayer player) {
		if (ServerPlayNetworking.canSend(player, GearSyncPayload.TYPE)) {
			ServerPlayNetworking.send(player, new GearSyncPayload(get(player), bonuses(player)));
		}
	}
}
