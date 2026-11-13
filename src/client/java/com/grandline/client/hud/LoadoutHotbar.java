package com.grandline.client.hud;

import com.grandline.client.ClientProfileCache;
import com.grandline.client.GrandLineKeys;
import com.grandline.client.settings.ClientSettings;
import com.grandline.client.ui.MarshUi;
import com.grandline.combat.ability.PowerSource;
import com.grandline.fruit.Fruits;
import com.grandline.gear.GearSwordItem;
import com.grandline.gear.GunItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The Grand Line hotbar: the vanilla 9 slots (same keys, scrolling and item behaviour) drawn with the Marsh UI slot
 * sprites, plus a loadout panel (Style, Fruit, Sword, Gun) showing which one powers the move keys. The loadout key
 * cycles through it: Style/Fruit switch the move keys and select a hand without a weapon; Sword/Gun select that
 * weapon's hotbar slot.
 */
public final class LoadoutHotbar {
	public enum Loadout { STYLE, FRUIT, SWORD, GUN }

	private static final int SLOT = 20;
	private static final int GAP = 1;

	private LoadoutHotbar() {
	}

	public static boolean replacesVanilla() {
		Minecraft mc = Minecraft.getInstance();
		return ClientSettings.get().customHotbar && mc.player != null && !mc.player.isSpectator() && ClientProfileCache.view() != null;
	}

	/** Which loadout entry currently powers the move keys. */
	public static Loadout active() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null) {
			var held = mc.player.getMainHandItem().getItem();
			if (held instanceof GearSwordItem) {
				return Loadout.SWORD;
			}
			if (held instanceof GunItem) {
				return Loadout.GUN;
			}
		}
		return GrandLineKeys.activeSource() == PowerSource.FRUIT ? Loadout.FRUIT : Loadout.STYLE;
	}

	/** Item shown for a loadout entry (empty if the player has none). */
	public static ItemStack icon(Loadout loadout) {
		Minecraft mc = Minecraft.getInstance();
		Player player = mc.player;
		return switch (loadout) {
			case STYLE -> new ItemStack(Items.BLAZE_POWDER);
			case FRUIT -> {
				var profile = ClientProfileCache.profile();
				var def = profile != null && profile.powers().hasFruit() ? Fruits.get(profile.powers().fruit()) : null;
				yield def == null ? ItemStack.EMPTY : new ItemStack(def.item());
			}
			case SWORD -> itemIn(player, GearSwordItem.class);
			case GUN -> itemIn(player, GunItem.class);
		};
	}

	/** The hotbar item of {@code type}, or EMPTY when there is none (never asks the inventory for slot -1). */
	private static ItemStack itemIn(Player player, Class<?> type) {
		if (player == null) {
			return ItemStack.EMPTY;
		}
		int slot = hotbarSlotOf(player, type);
		return slot < 0 ? ItemStack.EMPTY : player.getInventory().getItem(slot);
	}

	/** First hotbar slot holding an item of {@code type} (the selected slot wins), or -1. */
	private static int hotbarSlotOf(Player player, Class<?> type) {
		Inventory inv = player.getInventory();
		if (type.isInstance(inv.getSelected().getItem())) {
			return inv.selected;
		}
		for (int i = 0; i < 9; i++) {
			if (type.isInstance(inv.getItem(i).getItem())) {
				return i;
			}
		}
		return -1;
	}

	/** Loadout key: move to the next available entry. */
	public static void cycle() {
		Minecraft mc = Minecraft.getInstance();
		Player player = mc.player;
		if (player == null) {
			return;
		}
		Loadout current = active();
		for (int step = 1; step <= 4; step++) {
			Loadout next = Loadout.values()[(current.ordinal() + step) % 4];
			if (select(next)) {
				return;
			}
		}
	}

	/** Selects a loadout entry. Returns false if the player does not have it. */
	public static boolean select(Loadout loadout) {
		Minecraft mc = Minecraft.getInstance();
		Player player = mc.player;
		switch (loadout) {
			case SWORD, GUN -> {
				int slot = hotbarSlotOf(player, loadout == Loadout.SWORD ? GearSwordItem.class : GunItem.class);
				if (slot < 0) {
					return false;
				}
				selectSlot(slot);
				return true;
			}
			default -> {
				var profile = ClientProfileCache.profile();
				if (loadout == Loadout.FRUIT && (profile == null || !profile.powers().hasFruit())) {
					return false;
				}
				GrandLineKeys.choose(loadout == Loadout.FRUIT ? PowerSource.FRUIT : PowerSource.STYLE);
				// A held weapon would override the move keys: switch to a hotbar slot without one.
				var held = player.getMainHandItem().getItem();
				if (held instanceof GearSwordItem || held instanceof GunItem) {
					for (int i = 0; i < 9; i++) {
						var item = player.getInventory().getItem(i).getItem();
						if (!(item instanceof GearSwordItem) && !(item instanceof GunItem)) {
							selectSlot(i);
							break;
						}
					}
				}
				return true;
			}
		}
	}

	private static void selectSlot(int slot) {
		Minecraft mc = Minecraft.getInstance();
		mc.player.getInventory().selected = slot;
		if (mc.getConnection() != null) {
			mc.getConnection().send(new ServerboundSetCarriedItemPacket(slot));
		}
	}

	// ------------------------------------------------------------------------------------------------ drawing

	public static void render(GuiGraphics g, float partialTick) {
		Minecraft mc = Minecraft.getInstance();
		Player player = mc.player;
		Font font = mc.font;
		int width = 9 * SLOT + 8 * GAP;
		int x0 = g.guiWidth() / 2 - width / 2;
		int y = g.guiHeight() - SLOT - 2;
		Inventory inv = player.getInventory();
		for (int i = 0; i < 9; i++) {
			int x = x0 + i * (SLOT + GAP);
			MarshUi.abilitySlot(g, x, y, SLOT, i == inv.selected ? 1 : 0);
			ItemStack stack = inv.getItem(i);
			if (!stack.isEmpty()) {
				g.renderItem(player, stack, x + 2, y + 2, i + 1);
				g.renderItemDecorations(font, stack, x + 2, y + 2);
			}
			keyNumber(g, font, String.valueOf(i + 1), x + 1, y - 3);
		}
		// Offhand slot, on the side vanilla uses.
		ItemStack offhand = player.getOffhandItem();
		int left = x0 - 4;
		if (!offhand.isEmpty()) {
			left -= SLOT;
			MarshUi.abilitySlot(g, left, y, SLOT, 0);
			g.renderItem(player, offhand, left + 2, y + 2, 10);
			g.renderItemDecorations(font, offhand, left + 2, y + 2);
			left -= 4;
		}
		// Loadout panel: Style, Fruit, Sword, Gun.
		Loadout active = active();
		int lx = left - 4 * (SLOT + GAP) - 2;
		String[] labels = {"style", "fruit", "sword", "gun"};
		for (Loadout l : Loadout.values()) {
			int x = lx + l.ordinal() * (SLOT + GAP);
			ItemStack icon = icon(l);
			MarshUi.abilitySlot(g, x, y, SLOT, l == active ? 1 : icon.isEmpty() ? 2 : 0);
			if (!icon.isEmpty()) {
				g.renderFakeItem(icon, x + 2, y + 2);
			}
			if (l == active) {
				Component label = Component.translatable("screen.grandline.items.loadout." + labels[l.ordinal()]);
				g.pose().pushPose();
				g.pose().translate(x + SLOT / 2f - font.width(label) * 0.35f, y - 7, 0);
				g.pose().scale(0.7f, 0.7f, 1);
				g.drawString(font, label, 0, 0, 0xFFD25A, true);
				g.pose().popPose();
			}
		}
		Component key = Component.literal(GrandLineKeys.LOADOUT.getTranslatedKeyMessage().getString());
		keyNumber(g, font, key.getString(), lx - font.width(key) - 6, y + 6);
	}

	private static void keyNumber(GuiGraphics g, Font font, String text, int x, int y) {
		int w = Math.max(9, font.width(text) + 3);
		MarshUi.keyCap(g, x, y, w, 9);
		g.pose().pushPose();
		g.pose().translate(0, 0, 200);
		g.drawString(font, text, x + w / 2 - font.width(text) / 2 + 1, y + 1, 0xFFFFFF, false);
		g.pose().popPose();
	}
}
