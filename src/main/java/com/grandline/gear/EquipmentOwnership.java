package com.grandline.gear;

import com.grandline.GrandLineMod;
import com.grandline.data.ModAttachments;
import com.grandline.network.payload.EquipmentSyncPayload;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
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
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Permanent equipment ownership and the Backpack: every sword, gun, fishing rod and accessory ({@link #all()}) is an
 * unlock a player owns once, for good. The record ({@link OwnedEquipment}, saved with the player and kept through death)
 * is the authority.
 *
 * <p>Weapons (see {@link WeaponCategory}): one sword, one gun and one rod can be equipped at a time. The equipped one is
 * held as an item (swords and guns in hotbar slots 1-4, the loadout keys); the others are kept in the Backpack (Items →
 * Backpack) and are not items at all, so there is nothing to duplicate. Equipping a weapon puts it in the slot of the one
 * it replaces, keeping the hotbar arrangement; unequipping takes it out of the hotbar. Neither touches mastery or
 * cooldowns (both belong to the weapon's power, not the item). Accessories are held in the inventory until worn in
 * their gear slots, as before.
 *
 * <p>The reconciler (each second, and on join and respawn) keeps that true whatever happened to the stacks (pickups,
 * containers, the crafting grid, dragging, commands, death): each held item is exactly the equipped weapons and owned
 * accessories, once each (extra copies are removed; a lost one comes back when there is room), and equipment the player
 * does not own is removed (in creative mode, taking it from the creative menu makes it owned, and equips a weapon).
 * Equipment cannot be dropped. It also mends any damaged item (durability is off in Grand Line).
 *
 * <p>Saves from before ownership are migrated once (everything held counts as owned); saves from before the Backpack
 * once more (the weapon in hand or the hotbar becomes the equipped one of its category; the others go to the Backpack,
 * still owned; a held fishing rod becomes owned).
 */
public final class EquipmentOwnership {
	public enum Grant {
		/** Now owned and in the inventory (a weapon: equipped, as nothing of its category was). */
		GRANTED,
		/** Now owned; the inventory was full, so it arrives as soon as there is room. */
		GRANTED_NO_ROOM,
		/** Now owned and waiting in the Backpack (a weapon of its category is already equipped). */
		STORED,
		/** Already owned: nothing given. */
		ALREADY_OWNED,
		/** Not a sword, gun, rod or accessory. */
		NOT_EQUIPMENT
	}

	/** The outcome of an equip / unequip request. */
	public enum Change {
		EQUIPPED, UNEQUIPPED, ALREADY, NOT_OWNED, NOT_WEAPON, NOT_EQUIPPED, NO_ROOM;

		public boolean done() {
			return this == EQUIPPED || this == UNEQUIPPED;
		}
	}

	private static final int RECONCILE_INTERVAL = 20;
	/** What each player's client was last told, so a change is sent once. */
	private static final Map<UUID, OwnedEquipment> SENT = new ConcurrentHashMap<>();
	private static List<Item> all;

	private EquipmentOwnership() {
	}

	public static void register() {
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			SENT.remove(handler.getPlayer().getUUID());
			reconcile(handler.getPlayer());
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> SENT.remove(handler.getPlayer().getUUID()));
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
			SENT.remove(newPlayer.getUUID());
			reconcile(newPlayer);
		});
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

	/** Everything that is owned equipment: swords, guns and accessories ({@link Gear#all()}) and the fishing rod. */
	public static List<Item> all() {
		if (all == null) {
			List<Item> list = new ArrayList<>(Gear.all());
			list.add(com.grandline.registry.ModItems.STARTER_FISHING_ROD);
			all = List.copyOf(list);
		}
		return all;
	}

	public static boolean isEquipment(Item item) {
		return item != null && item != Items.AIR && all().contains(item);
	}

	public static String id(Item item) {
		return BuiltInRegistries.ITEM.getKey(item).toString();
	}

	public static Item item(String id) {
		ResourceLocation rl = ResourceLocation.tryParse(id);
		return rl == null ? Items.AIR : BuiltInRegistries.ITEM.get(rl);
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

	/** The equipped weapon of {@code category}, or AIR. */
	public static Item equipped(ServerPlayer player, WeaponCategory category) {
		String id = record(player).equippedIn(category.key);
		return id.isEmpty() ? Items.AIR : item(id);
	}

	public static boolean isEquipped(ServerPlayer player, Item item) {
		return isEquipment(item) && record(player).isEquipped(id(item));
	}

	// ============================================================================================== changes

	/**
	 * Gives the player {@code item} for good, unless it is not equipment or they already own it (then nothing is given and
	 * nothing else changes: callers keep their other rewards and take no money). A weapon is equipped (in the hotbar) when
	 * nothing of its category is; otherwise it goes to the Backpack.
	 */
	public static Grant grant(ServerPlayer player, Item item) {
		if (!isEquipment(item)) {
			return Grant.NOT_EQUIPMENT;
		}
		OwnedEquipment own = prepared(player);
		String id = id(item);
		if (own.owns(id)) {
			return Grant.ALREADY_OWNED;
		}
		own = own.with(id);
		WeaponCategory category = WeaponCategory.of(item);
		GrandLineMod.LOGGER.info("{} now owns {}", player.getGameProfile().getName(), id);
		if (category != null && !own.equippedIn(category.key).isEmpty()) {
			player.setAttached(ModAttachments.OWNED_EQUIPMENT, own);
			player.displayClientMessage(Component.translatable("message.grandline.equipment.stored", new ItemStack(item).getHoverName())
					.withStyle(ChatFormatting.GREEN), true);
			sync(player);
			return Grant.STORED;
		}
		if (category != null) {
			own = own.withEquipped(category.key, id);
		}
		player.setAttached(ModAttachments.OWNED_EQUIPMENT, own);
		boolean added = category != null ? placeNew(player, item, category) >= 0 : player.getInventory().add(new ItemStack(item));
		sync(player);
		return added ? Grant.GRANTED : Grant.GRANTED_NO_ROOM;
	}

	/** Admin only: takes ownership away (the held copy is removed by the reconciler). */
	public static boolean revoke(ServerPlayer player, Item item) {
		OwnedEquipment own = prepared(player);
		if (!isEquipment(item) || !own.owns(id(item))) {
			return false;
		}
		player.setAttached(ModAttachments.OWNED_EQUIPMENT, own.without(id(item)));
		reconcile(player);
		return true;
	}

	/**
	 * Equips an owned item and selects it: an accessory goes to its slot; a weapon replaces the equipped one of its
	 * category (see {@link #setEquipped}) and is then put in hand. Never creates or removes ownership.
	 */
	public static boolean equip(ServerPlayer player, Item item) {
		if (!owns(player, item)) {
			return false;
		}
		reconcile(player);
		Inventory inv = player.getInventory();
		if (item instanceof AccessoryItem) {
			for (int i = 0; i < inv.items.size(); i++) {
				if (inv.items.get(i).is(item)) {
					return GearService.equipFromInventory(player, i) >= 0;
				}
			}
			return false; // an equipped accessory is already equipped
		}
		Change change = setEquipped(player, item, true);
		if (!change.done() && change != Change.ALREADY) {
			return false;
		}
		for (int i = 0; i < Inventory.getSelectionSize(); i++) {
			if (inv.items.get(i).is(item)) {
				inv.selected = i;
				return true;
			}
		}
		return true; // held, but outside the hotbar (a rod in a full hotbar)
	}

	/**
	 * The Backpack's Equip / Unequip, validated here. Equipping a weapon unequips the one of its category: the new
	 * weapon takes that weapon's hotbar slot (the arrangement is kept; if the old one was in hand, the new one is), and the
	 * old one goes back to the Backpack. Unequipping takes the weapon out of the hotbar; it stays owned. Mastery and
	 * cooldowns are never touched. Repeating a request changes nothing (safe against rapid clicks).
	 */
	public static Change setEquipped(ServerPlayer player, Item item, boolean equip) {
		WeaponCategory category = WeaponCategory.of(item);
		if (category == null || !isEquipment(item)) {
			return Change.NOT_WEAPON;
		}
		OwnedEquipment own = prepared(player);
		String id = id(item);
		if (!own.owns(id)) {
			return Change.NOT_OWNED;
		}
		String current = own.equippedIn(category.key);
		Inventory inv = player.getInventory();
		if (equip) {
			if (current.equals(id)) {
				return Change.ALREADY;
			}
			Item old = current.isEmpty() ? Items.AIR : item(current);
			int slot = old == Items.AIR ? -1 : slotOf(inv, old);
			if (slot >= 0) {
				inv.setItem(slot, new ItemStack(item)); // same slot: the hotbar arrangement is kept
			} else if (placeNew(player, item, category) < 0) {
				return Change.NO_ROOM;
			}
			player.setAttached(ModAttachments.OWNED_EQUIPMENT, own.withEquipped(category.key, id));
			GrandLineMod.LOGGER.debug("{} equipped {} (was {})", player.getGameProfile().getName(), id, current);
		} else {
			if (!current.equals(id)) {
				return Change.NOT_EQUIPPED;
			}
			player.setAttached(ModAttachments.OWNED_EQUIPMENT, own.withEquipped(category.key, ""));
		}
		reconcile(player); // removes the old weapon's copies (cursor, crafting grid, anywhere) and syncs
		return equip ? Change.EQUIPPED : Change.UNEQUIPPED;
	}

	/** Unequips an accessory slot (0 accessory, 1 and 2 rings); ownership is unchanged. */
	public static boolean unequip(ServerPlayer player, int gearSlot) {
		return GearService.unequip(player, gearSlot);
	}

	/** The first inventory slot (hotbar first) holding {@code item}, or -1. */
	private static int slotOf(Inventory inv, Item item) {
		for (int i = 0; i < inv.items.size(); i++) {
			if (inv.items.get(i).is(item)) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Puts a fresh copy of an equipped weapon where it belongs: a sword or gun in a free slot among 1-4 (the loadout keys),
	 * a rod in a free hotbar slot, else anywhere in the inventory. Returns the slot, or -1 if there is no room.
	 */
	private static int placeNew(ServerPlayer player, Item item, WeaponCategory category) {
		Inventory inv = player.getInventory();
		int from = category.loadoutSlot() ? 0 : com.grandline.player.loadout.LoadoutOrder.SIZE;
		int to = category.loadoutSlot() ? com.grandline.player.loadout.LoadoutOrder.SIZE : Inventory.getSelectionSize();
		for (int pass = 0; pass < 2; pass++) {
			for (int i = from; i < to; i++) {
				if (inv.items.get(i).isEmpty()) {
					inv.setItem(i, new ItemStack(item));
					return i;
				}
			}
			from = 0;
			to = inv.items.size();
		}
		return -1;
	}

	// ============================================================================================== reconcile

	/** The record with both one-time migrations done. */
	private static OwnedEquipment prepared(ServerPlayer player) {
		OwnedEquipment own = record(player);
		if (!own.migrated()) {
			own = migrate(player);
		}
		if (own.equipVersion() < OwnedEquipment.EQUIP_VERSION) {
			own = migrateEquipped(player, own);
		}
		return own;
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

	/**
	 * One-time, for saves from before the Backpack: in each category, the owned weapon the player holds most prominently
	 * (in hand, then the hotbar, then the inventory) becomes the equipped one; every other owned weapon stays owned, in
	 * the Backpack. A held fishing rod (not owned equipment before) becomes owned. Nothing is lost.
	 */
	static OwnedEquipment migrateEquipped(ServerPlayer player, OwnedEquipment own) {
		List<Holder> holders = new ArrayList<>(holders(player));
		holders.sort((a, b) -> Integer.compare(a.priority(), b.priority()));
		for (Holder h : holders) {
			Item item = h.stack().getItem();
			WeaponCategory category = WeaponCategory.of(item);
			if (category == WeaponCategory.ROD && isEquipment(item)) {
				own = own.with(id(item));
			}
			if (category != null && own.owns(id(item)) && own.equippedIn(category.key).isEmpty()) {
				own = own.withEquipped(category.key, id(item));
			}
		}
		own = own.withEquipVersion(OwnedEquipment.EQUIP_VERSION);
		player.setAttached(ModAttachments.OWNED_EQUIPMENT, own);
		GrandLineMod.LOGGER.info("Backpack equipment set up for {}: equipped {}", player.getGameProfile().getName(), own.equipped());
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

	/** Makes the player's held equipment match what they own and have equipped (see the class notes). */
	public static void reconcile(ServerPlayer player) {
		if (player.isRemoved()) {
			return;
		}
		OwnedEquipment own = prepared(player);
		OwnedEquipment before = own;
		List<Holder> holders = holders(player);
		for (Holder h : holders) {
			if (h.stack().getDamageValue() > 0) {
				h.stack().setDamageValue(0); // durability is gone: old damage is mended
			}
		}
		// Creative mode: taking equipment from the creative menu makes it yours (and a weapon, equipped) instead of
		// removing it. Done first, so the weapon it replaces is put away below.
		if (player.gameMode.getGameModeForPlayer() == net.minecraft.world.level.GameType.CREATIVE) {
			List<Holder> sorted = new ArrayList<>(holders);
			sorted.sort((a, b) -> Integer.compare(a.priority(), b.priority()));
			for (Holder h : sorted) {
				Item item = h.stack().getItem();
				if (isEquipment(item) && !own.owns(id(item))) {
					own = own.with(id(item));
					WeaponCategory category = WeaponCategory.of(item);
					if (category != null) {
						own = own.withEquipped(category.key, id(item));
					}
				}
			}
		}
		// Equipped entries must be owned weapons of their own category.
		for (WeaponCategory category : WeaponCategory.values()) {
			String id = own.equippedIn(category.key);
			if (!id.isEmpty() && (!own.owns(id) || WeaponCategory.of(item(id)) != category)) {
				own = own.withEquipped(category.key, "");
			}
		}
		if (!own.equals(before)) {
			player.setAttached(ModAttachments.OWNED_EQUIPMENT, own);
		}
		EquippedGear gear = GearService.get(player);
		EquippedGear newGear = gear;
		boolean changed = false;
		for (Item item : all()) {
			String id = id(item);
			WeaponCategory category = WeaponCategory.of(item);
			boolean owned = own.owns(id);
			// A weapon is held only while equipped; an accessory whenever owned (unless worn).
			boolean held = owned && (category == null || own.equippedIn(category.key).equals(id));
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
				if (held && !kept) {
					kept = true;
					if (h.stack().getCount() > 1) {
						h.stack().setCount(1);
						changed = true;
					}
				} else {
					remove(player, h); // a copy, unowned, or a weapon that is in the Backpack
					changed = true;
				}
			}
			if (held && !kept) {
				// Lost somewhere (or never delivered): put it back when there is room.
				boolean back = category != null ? placeNew(player, item, category) >= 0 : player.getInventory().add(new ItemStack(item));
				if (back) {
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
		sync(player);
	}

	private static void remove(ServerPlayer player, Holder h) {
		if (h.container() == null) {
			player.containerMenu.setCarried(ItemStack.EMPTY);
		} else {
			h.container().setItem(h.slot(), ItemStack.EMPTY);
		}
	}

	// ============================================================================================== sync

	/** Sends the player's owned and equipped weapons to their client when they changed since it was last told. */
	public static void sync(ServerPlayer player) {
		OwnedEquipment own = record(player);
		if (own.equals(SENT.get(player.getUUID())) || player.connection == null
				|| !ServerPlayNetworking.canSend(player, EquipmentSyncPayload.TYPE)) {
			return;
		}
		SENT.put(player.getUUID(), own);
		ServerPlayNetworking.send(player, new EquipmentSyncPayload(List.copyOf(own.ids()), own.equipped()));
	}
}
