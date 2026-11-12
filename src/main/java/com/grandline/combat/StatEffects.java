package com.grandline.combat;

import com.grandline.player.PlayerProfile;
import com.grandline.player.PlayerProfileService;
import com.grandline.player.StatRules;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;

import java.util.UUID;

/**
 * Turns invested stats into real, server-side gameplay effects using vanilla attribute modifiers, so combat, the
 * health bar and the vanilla attribute tooltip all respect them automatically.
 *
 * <ul>
 *     <li>Defense → bonus max health.</li>
 *     <li>Melee → bonus attack damage while the main hand is empty.</li>
 *     <li>Sword → bonus attack damage while holding a {@link SwordItem}.</li>
 * </ul>
 *
 * <p>All modifiers are recomputed from the profile on join, on respawn, on every profile change, and (for the
 * held-item-dependent damage bonus) every tick when the value changes. The damage modifier is transient. The health
 * modifier is saved with the player on purpose: vanilla clamps the saved health to max health while loading, before
 * any mod code runs, so a transient health bonus would make players lose their extra health on every relog.
 */
public final class StatEffects {
	private static final UUID HEALTH_MODIFIER = UUID.fromString("6f1d2c4e-0b5a-4f3e-9c1a-8a7e5d2b1c01");
	private static final UUID DAMAGE_MODIFIER = UUID.fromString("6f1d2c4e-0b5a-4f3e-9c1a-8a7e5d2b1c02");

	private StatEffects() {
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				refreshDamage(player, PlayerProfileService.get(player));
			}
		});
	}

	/** Applies every stat effect for the given profile. */
	public static void apply(ServerPlayer player, PlayerProfile profile) {
		StatRules rules = PlayerProfileService.statRules();

		setModifier(player, Attributes.MAX_HEALTH, HEALTH_MODIFIER, "Grand Line defense", rules.bonusHealth(profile.stats()), true);
		if (player.getHealth() > player.getMaxHealth()) {
			player.setHealth(player.getMaxHealth());
		}

		refreshDamage(player, profile);
	}

	/** The damage bonus the player should currently have, based on what they are holding. */
	public static double currentDamageBonus(ServerPlayer player, PlayerProfile profile) {
		StatRules rules = PlayerProfileService.statRules();
		ItemStack held = player.getMainHandItem();

		if (held.isEmpty()) {
			return rules.bonusMeleeDamage(profile.stats());
		}
		if (held.getItem() instanceof SwordItem) {
			return rules.bonusSwordDamage(profile.stats());
		}
		return 0.0;
	}

	private static void refreshDamage(ServerPlayer player, PlayerProfile profile) {
		setModifier(player, Attributes.ATTACK_DAMAGE, DAMAGE_MODIFIER, "Grand Line melee/sword", currentDamageBonus(player, profile), false);
	}

	private static void setModifier(ServerPlayer player, Attribute attribute, UUID id, String name, double amount, boolean saved) {
		AttributeInstance instance = player.getAttribute(attribute);
		if (instance == null) {
			return;
		}

		AttributeModifier existing = instance.getModifier(id);
		if (existing != null && existing.getAmount() == amount) {
			return; // unchanged, avoid needless attribute sync packets
		}

		instance.removeModifier(id);
		if (amount != 0.0) {
			AttributeModifier modifier = new AttributeModifier(id, name, amount, AttributeModifier.Operation.ADDITION);
			if (saved) {
				instance.addPermanentModifier(modifier);
			} else {
				instance.addTransientModifier(modifier);
			}
		}
	}
}
