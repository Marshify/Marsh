package com.grandline.client.hud;

import com.grandline.client.ClientProfileCache;
import com.grandline.client.GrandLineKeys;
import com.grandline.client.settings.ClientSettings;
import com.grandline.client.ui.MarshUi;
import com.grandline.combat.ability.PowerSource;
import com.grandline.fruit.Fruits;
import com.grandline.gear.GearSwordItem;
import com.grandline.gear.GunItem;
import com.grandline.network.payload.LoadoutOrderPayload;
import com.grandline.player.loadout.LoadoutOrder;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/**
 * The Grand Line hotbar, one bar numbered 1-9 in place of the vanilla one: keys 1-4 are the loadout entries (Fighting
 * Style, Fruit, Sword, Gun in the player's saved order), keys 5-9 the hotbar slots 5-9. Only tiles with something in
 * them are drawn (an unequipped entry, an empty slot: nothing), packed together with no gaps. The server keeps hotbar
 * slots 1-4 for the sword and gun (HotbarLayout), so every item has a tile. Number keys 1-4 select the loadout entry in that
 * position: Style/Fruit switch the move keys and select a hand without a weapon; Sword/Gun select that weapon's
 * hotbar slot. Keys 5-9 and scrolling keep their vanilla behaviour. The order is edited in {@code LoadoutEditScreen}.
 */
public final class LoadoutHotbar {
	public enum Loadout {
		STYLE("style"), FRUIT("fruit"), SWORD("sword"), GUN("gun");

		public final String label;

		Loadout(String label) {
			this.label = label;
		}

		public Component displayName() {
			return Component.translatable("screen.grandline.items.loadout." + label);
		}
	}

	/** Entry ids by position (see {@link LoadoutOrder}); the server sends the saved order on join. */
	private static List<Integer> order = LoadoutOrder.DEFAULT;

	private static final int SLOT = 20;
	private static final int GAP = 1;

	private LoadoutHotbar() {
	}

	public static boolean replacesVanilla() {
		Minecraft mc = Minecraft.getInstance();
		return ClientSettings.get().customHotbar && mc.player != null && !mc.player.isSpectator() && ClientProfileCache.view() != null;
	}

	/**
	 * Whether a loadout entry is equipped: the fighting style always (once the profile is known), the fruit when one has
	 * been eaten, a sword or gun when it is in the hotbar (equipping puts it there). A fruit item, or a weapon carried in
	 * the main inventory, does not count.
	 */
	public static boolean equipped(Loadout loadout) {
		if (loadout == Loadout.STYLE) {
			return ClientProfileCache.profile() != null;
		}
		return !icon(loadout).isEmpty();
	}

	/** The positions of equipped entries, in the saved order (what the combat bar draws). */
	public static List<Integer> visiblePositions() {
		boolean[] eq = new boolean[LoadoutOrder.SIZE];
		for (Loadout l : Loadout.values()) {
			eq[l.ordinal()] = equipped(l);
		}
		return LoadoutOrder.visiblePositions(order, eq);
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
		return GrandLineKeys.activeSource() == PowerSource.FRUIT && equipped(Loadout.FRUIT) ? Loadout.FRUIT : Loadout.STYLE;
	}

	/** Whether the selection is a loadout entry (keys 1-4) rather than an item in slots 5-9. */
	public static boolean loadoutSelected() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) {
			return true;
		}
		var held = mc.player.getMainHandItem().getItem();
		return mc.player.getInventory().selected < LoadoutOrder.SIZE || held instanceof GearSwordItem || held instanceof GunItem;
	}

	/** If the selected entry is no longer equipped (fruit removed), switch to an equipped one. Runs every tick. */
	private static void keepSelectionEquipped() {
		if (GrandLineKeys.activeSource() == PowerSource.FRUIT && !equipped(Loadout.FRUIT)) {
			GrandLineKeys.choose(PowerSource.STYLE);
		}
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

	public static List<Integer> order() {
		return order;
	}

	/** Changes the order on this client only (the editor saves it to the server when it closes). */
	public static void setOrder(List<Integer> newOrder) {
		order = LoadoutOrder.sanitize(newOrder);
	}

	/** Sends the current order to the server to be saved. */
	public static void saveOrder() {
		if (ClientPlayNetworking.canSend(LoadoutOrderPayload.TYPE)) {
			ClientPlayNetworking.send(new LoadoutOrderPayload(order));
		}
	}

	/** The loadout entry in {@code position} (0-3). */
	public static Loadout at(int position) {
		return Loadout.values()[order.get(position)];
	}

	/** Number key {@code position + 1}: selects the entry in that position, or says why it can't. */
	public static void selectPosition(int position) {
		Minecraft mc = Minecraft.getInstance();
		Loadout loadout = at(position);
		if (mc.player != null && !select(loadout)) {
			mc.player.displayClientMessage(Component.translatable("message.grandline.loadout.empty." + loadout.label).withStyle(ChatFormatting.RED), true);
		}
	}

	/**
	 * While the Grand Line hotbar is shown, number keys 1-4 select loadout positions instead of hotbar slots 1-4.
	 * Runs before vanilla reads the hotbar keys each tick, so the clicks never reach the vanilla hotbar.
	 */
	public static void register() {
		ClientTickEvents.START_CLIENT_TICK.register(client -> {
			if (client.player == null || !replacesVanilla()) {
				return;
			}
			keepSelectionEquipped();
			if (client.screen != null) {
				return;
			}
			// Creative "save/load toolbar" (activator + number) keeps working.
			if (client.player.isCreative() && (client.options.keySaveHotbarActivator.isDown() || client.options.keyLoadHotbarActivator.isDown())) {
				return;
			}
			for (int i = 0; i < LoadoutOrder.SIZE; i++) {
				boolean pressed = false;
				while (client.options.keyHotbarSlots[i].consumeClick()) {
					pressed = true;
				}
				if (pressed) {
					selectPosition(i);
				}
			}
		});
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
				int sel = player.getInventory().selected;
				if (held instanceof GearSwordItem || held instanceof GunItem || sel >= LoadoutOrder.SIZE) {
					// An empty hand among slots 1-4 (kept for weapons), else any slot without a weapon.
					int pick = -1;
					for (int i = 0; i < 9 && pick < 0; i++) {
						if (i < LoadoutOrder.SIZE && player.getInventory().getItem(i).isEmpty()) {
							pick = i;
						}
					}
					for (int i = 0; i < 9 && pick < 0; i++) {
						var item = player.getInventory().getItem(i).getItem();
						if (!(item instanceof GearSwordItem) && !(item instanceof GunItem)) {
							pick = i;
						}
					}
					if (pick >= 0) {
						selectSlot(pick);
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

	/**
	 * One bar in place of the vanilla hotbar: tiles 1-4 are the equipped loadout entries (Fighting Style, Fruit, Sword,
	 * Gun in the saved order), tiles 5-9 the hotbar slots 5-9. Only tiles with something in them are drawn, packed
	 * together and centred; each keeps its own key number. The offhand sits to the left, as in vanilla.
	 */
	public static void render(GuiGraphics g, float partialTick) {
		Minecraft mc = Minecraft.getInstance();
		Player player = mc.player;
		Font font = mc.font;
		Inventory inv = player.getInventory();
		List<Integer> loadout = visiblePositions();
		List<Integer> items = new java.util.ArrayList<>();
		for (int i = LoadoutOrder.SIZE; i < 9; i++) {
			if (!inv.getItem(i).isEmpty()) {
				items.add(i);
			}
		}
		int count = loadout.size() + items.size();
		if (count == 0) {
			return;
		}
		int width = count * SLOT + (count - 1) * GAP;
		int x0 = g.guiWidth() / 2 - width / 2;
		int y = g.guiHeight() - SLOT - 2;
		boolean onLoadout = loadoutSelected();
		Loadout active = active();
		int k = 0;
		for (int pos : loadout) {
			Loadout l = at(pos);
			int x = x0 + k++ * (SLOT + GAP);
			boolean sel = onLoadout && l == active;
			MarshUi.abilitySlot(g, x, y, SLOT, sel ? 1 : 0);
			ItemStack icon = icon(l);
			if (!icon.isEmpty()) {
				g.renderFakeItem(icon, x + 2, y + 2);
			}
			keyNumber(g, font, String.valueOf(pos + 1), x + 1, y - 3);
			if (sel) {
				label(g, font, l.displayName(), x, y);
			}
		}
		for (int i : items) {
			int x = x0 + k++ * (SLOT + GAP);
			MarshUi.abilitySlot(g, x, y, SLOT, !onLoadout && i == inv.selected ? 1 : 0);
			ItemStack stack = inv.getItem(i);
			g.renderItem(player, stack, x + 2, y + 2, i + 1);
			g.renderItemDecorations(font, stack, x + 2, y + 2);
			keyNumber(g, font, String.valueOf(i + 1), x + 1, y - 3);
		}
		ItemStack offhand = player.getOffhandItem();
		if (!offhand.isEmpty()) {
			int left = x0 - 4 - SLOT;
			MarshUi.abilitySlot(g, left, y, SLOT, 0);
			g.renderItem(player, offhand, left + 2, y + 2, 10);
			g.renderItemDecorations(font, offhand, left + 2, y + 2);
		}
	}

	private static void label(GuiGraphics g, Font font, Component label, int x, int y) {
		g.pose().pushPose();
		g.pose().translate(x + SLOT / 2f - font.width(label) * 0.35f, y - 10, 0);
		g.pose().scale(0.7f, 0.7f, 1);
		g.drawString(font, label, 0, 0, 0xFFD25A, true);
		g.pose().popPose();
	}

	public static void keyNumber(GuiGraphics g, Font font, String text, int x, int y) {
		int w = Math.max(9, font.width(text) + 3);
		MarshUi.keyCap(g, x, y, w, 9);
		g.pose().pushPose();
		g.pose().translate(0, 0, 200);
		g.drawString(font, text, x + w / 2 - font.width(text) / 2 + 1, y + 1, 0xFFFFFF, false);
		g.pose().popPose();
	}
}
