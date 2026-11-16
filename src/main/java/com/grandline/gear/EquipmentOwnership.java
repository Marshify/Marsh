package com.grandline.gear;

import com.grandline.GrandLineMod;
import com.grandline.data.ModAttachments;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Permanent equipment ownership: every sword, gun and accessory ({@link Gear#all()}) is an unlock a player owns once,
 * for good. The record ({@link OwnedEquipment}, saved with the player and kept through death) is the authority; the item
 * in the inventory, hotbar loadout or accessory slot is just how the owned item is held.
 *
 * <p>The reconciler (each second, and on join and respawn) keeps that true whatever happened to the stacks (pickups,
 * containers, the crafting grid, dragging, commands): each owned item is held exactly once (extra copies are removed; a
 * lost one comes back when there is room), and equipment the player does not own is removed. Equipment cannot be
 * dropped. It also mends any damaged item (durability is off in Grand Line; see ItemStackNoDurabilityMixin).
 *
 * <p>Saves from before this system are migrated once: everything the player holds or wears is counted as owned, and
 * duplicate copies are then merged down to one (keeping the equipped or hotbar copy).
 */
public final class EquipmentOwnership {
	public enum Grant {
		/** Now owned and in the inventory. */
		GRANTED,
		/** Now owned; the inventory was full, so it arrives as soon as there is room. */
		GRANTED_NO_ROOM,
		/** Already owned: nothing given. */
		ALREADY_OWNED,
		/** Not a sword, gun or accessory. */
		NOT_EQUIPMENT
	}

	private static final int RECONCILE_INTERVAL = 20;

	private EquipmentOwnership() {
	}

	public static void register() {
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> reconcile(handler.getPlayer()));
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> reconcile(newPlayer));
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			int tick = server.getTickCount();
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				if ((tick + player.getId()) % RECONCILE_INTERVAL == 0) {
					reconcile(player);
				}
			}
		});
	}

	// ============================================================================================== queries

	public static boolean isEquipment(Item item) {
		return item != null && item != Items.AIR && Gear.all().contains(item);
	}

	public static String id(Item item) {
		return BuiltInRegistries.ITEM.getKey(item).toString();
	}

	public static OwnedEquipment record(ServerPlayer player) {
		return player.getAttachedOrElse(ModAttachments.OWNED_EQUIPMENT, OwnedEquipment.EMPTY);
	}

	public static boolean owns(ServerPlayer player, Item item) {
		return isEquipment(item) && record(player).owns(id(item));
	}

	public static boolean owns(ServerPlayer player, String equipmentId) {
		return record(player).owns(equipmentId);
	}

	/** Every owned equipment id. */
	public static Set<String> owned(ServerPlayer player) {
		return record(player).ids();
	}

	// ============================================================================================== changes

	/**
	 * Gives the player {@code item} for good, unless it is not equipment or they already own it (then nothing is given and
	 * nothing else changes: callers keep their other rewards and take no money).
	 */
	public static Grant grant(ServerPlayer player, Item item) {
		if (!isEquipment(item)) {
			return Grant.NOT_EQUIPMENT;
		}
		OwnedEquipment own = migrated(player);
		String id = id(item);
		if (own.owns(id)) {
			return Grant.ALREADY_OWNED;
		}
		player.setAttached(ModAttachments.OWNED_EQUIPMENT, own.with(id));
		boolean added = player.getInventory().add(new ItemStack(item));
		GrandLineMod.LOGGER.info("{} now owns {}", player.getGameProfile().getName(), id);
		return added ? Grant.GRANTED : Grant.GRANTED_NO_ROOM;
	}

	/** Admin only: takes ownership away (the held copy is removed by the reconciler). */
	public static boolean revoke(ServerPlayer player, Item item) {
		OwnedEquipment own = migrated(player);
		if (!isEquipment(item) || !own.owns(id(item))) {
			return false;
		}
		player.setAttached(ModAttachments.OWNED_EQUIPMENT, own.without(id(item)));
		reconcile(player);
		return true;
	}

	/**
	 * Equips an owned item: an accessory goes to its slot; a sword or gun is put in the hotbar and selected (the loadout
	 * shows it in slot 3 or 4). Never creates or removes ownership. Returns false if not owned or not held.
	 */
	public static boolean equip(ServerPlayer player, Item item) {
		if (!owns(player, item)) {
			return false;
		}
		reconcile(player);
		Inventory inv = player.getInventory();
		for (int i = 0; i < inv.items.size(); i++) {
			if (inv.items.get(i).is(item)) {
				if (item instanceof AccessoryItem) {
					return GearService.equipFromInventory(player, i) >= 0;
				}
				int target = i < Inventory.getSelectionSize() ? i : inv.getFreeSlot() >= 0 && inv.getFreeSlot() < Inventory.getSelectionSize()
						? inv.getFreeSlot() : inv.selected;
				if (target != i) {
					ItemStack moving = inv.items.get(i);
					inv.items.set(i, inv.items.get(target));
					inv.items.set(target, moving);
				}
				inv.selected = target;
				return true;
			}
		}
		return false; // an equipped accessory is already equipped
	}

	/** Unequips an accessory slot (0 accessory, 1 and 2 rings); ownership is unchanged. */
	public static boolean unequip(ServerPlayer player, int gearSlot) {
		return GearService.unequip(player, gearSlot);
	}

	// ============================================================================================== reconcile

	private static OwnedEquipment migrated(ServerPlayer player) {
		OwnedEquipment own = record(player);
		return own.migrated() ? own : migrate(player);
	}

	/** One-time: everything the player holds or wears now counts as owned (duplicates merge on the next reconcile). */
	static OwnedEquipment migrate(ServerPlayer player) {
		OwnedEquipment own = record(player);
		for (Holder h : holders(player)) {
			if (isEquipment(h.stack().getItem())) {
				own = own.with(id(h.stack().getItem()));
			}
		}
		EquippedGear gear = GearService.get(player);
		for (int slot = 0; slot < 3; slot++) {
			ResourceLocation rl = ResourceLocation.tryParse(gear.get(slot));
			if (rl != null && isEquipment(BuiltInRegistries.ITEM.get(rl))) {
				own = own.with(rl.toString());
			}
		}
		own = own.markMigrated();
		player.setAttached(ModAttachments.OWNED_EQUIPMENT, own);
		GrandLineMod.LOGGER.info("Equipment ownership migrated for {}: {}", player.getGameProfile().getName(), own.ids());
		return own;
	}

	/** A stack the player is holding somewhere, with its priority for being the copy that is kept (lower wins). */
	private record Holder(Container container, int slot, ItemStack stack, int priority) {
	}

	private static List<Holder> holders(ServerPlayer player) {
		List<Holder> out = new ArrayList<>();
		Inventory inv = player.getInventory();
		for (int i = 0; i < inv.getContainerSize(); i++) {
			ItemStack s = inv.getItem(i);
			if (!s.isEmpty()) {
				int priority = i == inv.selected ? 1 : i < Inventory.getSelectionSize() ? 2 : 4;
				out.add(new Holder(inv, i, s, priority));
			}
		}
		Container craft = player.inventoryMenu.getCraftSlots();
		for (int i = 0; i < craft.getContainerSize(); i++) {
			ItemStack s = craft.getItem(i);
			if (!s.isEmpty()) {
				out.add(new Holder(craft, i, s, 5));
			}
		}
		ItemStack carried = player.containerMenu.getCarried();
		if (!carried.isEmpty()) {
			out.add(new Holder(null, -1, carried, 3));
		}
		return out;
	}

	private static long lastNoRoomNotice;

	/** Makes the player's held equipment match what they own (see the class notes). */
	public static void reconcile(ServerPlayer player) {
		if (player.isRemoved()) {
			return;
		}
		OwnedEquipment own = migrated(player);
		List<Holder> holders = holders(player);
		for (Holder h : holders) {
			if (h.stack().getDamageValue() > 0) {
				h.stack().setDamageValue(0); // durability is gone: old damage is mended
			}
		}
		EquippedGear gear = GearService.get(player);
		EquippedGear newGear = gear;
		boolean changed = false;
		for (Item item : Gear.all()) {
			String id = id(item);
			boolean owned = own.owns(id);
			boolean kept = false;
			// An equipped accessory is the copy that counts first.
			for (int slot = 0; slot < 3; slot++) {
				if (newGear.get(slot).equals(id)) {
					if (owned && !kept) {
						kept = true;
					} else {
						newGear = newGear.with(slot, "");
					}
				}
			}
			List<Holder> copies = new ArrayList<>(holders.stream().filter(h -> h.stack().is(item) && !h.stack().isEmpty()).toList());
			copies.sort((a, b) -> Integer.compare(a.priority(), b.priority()));
			for (Holder h : copies) {
				if (owned && !kept) {
					kept = true;
					if (h.stack().getCount() > 1) {
						h.stack().setCount(1);
						changed = true;
					}
				} else {
					remove(player, h);
					changed = true;
				}
			}
			if (owned && !kept) {
				// Lost somewhere (or never delivered): put it back when there is room.
				if (player.getInventory().add(new ItemStack(item))) {
					changed = true;
				} else if (player.level().getGameTime() - lastNoRoomNotice > 200) {
					lastNoRoomNotice = player.level().getGameTime();
					player.displayClientMessage(Component.translatable("message.grandline.equipment.no_room", new ItemStack(item).getHoverName())
							.withStyle(ChatFormatting.YELLOW), true);
				}
			}
		}
		if (!newGear.equals(gear)) {
			player.setAttached(ModAttachments.EQUIPPED_GEAR, newGear);
			GearService.apply(player);
			changed = true;
		}
		if (changed) {
			player.inventoryMenu.broadcastChanges();
			player.containerMenu.broadcastChanges();
		}
	}

	private static void remove(ServerPlayer player, Holder h) {
		if (h.container() == null) {
			player.containerMenu.setCarried(ItemStack.EMPTY);
		} else {
			h.container().setItem(h.slot(), ItemStack.EMPTY);
		}
	}
}
