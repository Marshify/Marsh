package com.grandline.client.screen;

import com.grandline.client.ClientGear;
import com.grandline.client.ClientProfileCache;
import com.grandline.client.network.ClientNetworking;
import com.grandline.client.ui.MarshUi;
import com.grandline.fruit.FruitItem;
import com.grandline.fruit.Fruits;
import com.grandline.gear.AccessoryItem;
import com.grandline.gear.Gear;
import com.grandline.gear.GearSwordItem;
import com.grandline.gear.GunItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * Items: a tab column (Backpack, Build, Stash) beside a window.
 * <ul>
 *   <li>Backpack: every weapon you own (equipped ones marked with a green check), the fruits and accessories you carry
 *   and your fish. Select an item for its details and to equip or unequip it: equipping a sword, gun or rod replaces the
 *   one of its kind, in the same hotbar slot (the server does the swap and keeps both weapons' mastery).</li>
 *   <li>Build: your loadout (fighting style, gun, fruit, sword), accessory and ring slots (click to unequip) and the
 *   Build Stats of what you wear, as the server computes them.</li>
 *   <li>Stash: opens your fruit stash.</li>
 * </ul>
 */
public final class ItemsScreen extends Screen {
	public enum Tab { BACKPACK, BUILD, STASH }

	private static final int WIDTH = 330;
	private static final int HEIGHT = 210;
	private static final int TAB = 52;

	private Tab tab;
	private int left;
	private int top;
	private ItemStack hovered = ItemStack.EMPTY;
	/** The Backpack entry whose details are open, or null. */
	private Entry selected;
	/** First Backpack row shown (the grid scrolls when there are more than 28 entries). */
	private int scrollRow;
	/** An Equip / Unequip waiting for the server's answer: the button is locked until it arrives (or 1.5 s pass). */
	private String pendingKey;
	private boolean pendingWasEquipped;
	private long pendingUntil;
	/** Set after a request: the button wakes up again once the mouse leaves it. */
	private boolean needsRehover;

	private static final int COLUMNS = 7;
	private static final int ROWS = 4;
	private static final int PANEL_W = 196;
	private static final int PANEL_H = 128;
	private static final int EQUIPPED_GREEN = 0xFF3FCF6A;

	/**
	 * A Backpack tile: an owned weapon ({@code weapon} set), an accessory or fruit carried in inventory slot
	 * {@code invSlot}, or a kind of fish ({@code fishCount}).
	 */
	private record Entry(ItemStack stack, net.minecraft.world.item.Item weapon, int invSlot, int fishCount) {
		boolean equipped() {
			return weapon != null && com.grandline.client.ClientEquipment.isEquipped(weapon);
		}

		String key() {
			return weapon != null ? "w:" + com.grandline.gear.EquipmentOwnership.id(weapon) : invSlot >= 0 ? "s:" + invSlot + ":" + stack.getItem()
					: "f:" + stack.getItem();
		}
	}

	public ItemsScreen(Tab tab) {
		super(Component.translatable("screen.grandline.items.title"));
		this.tab = tab;
	}

	@Override
	protected void init() {
		left = (width - WIDTH - TAB - 6) / 2 + TAB + 6;
		top = Math.max(4, height / 2 - HEIGHT / 2);
	}

	private int[] tabBox(Tab t) {
		int y = top + 10 + t.ordinal() * (TAB + 6);
		return new int[]{left - TAB - 6, y, TAB, TAB};
	}

	private int[] gearSlot(int slot) {
		return new int[]{left + 12 + slot * 46, top + MarshUi.TITLE_HEIGHT + 64, 40, 40};
	}

	/** The tile of entry {@code i} (null when scrolled out of view). */
	private int[] backpackSlot(int i) {
		int row = i / COLUMNS - scrollRow;
		if (row < 0 || row >= ROWS) {
			return null;
		}
		return new int[]{left + 10 + (i % COLUMNS) * 44, top + MarshUi.TITLE_HEIGHT + 8 + row * 44, 40, 40};
	}

	private int[] panelBox() {
		return new int[]{left + WIDTH / 2 - PANEL_W / 2, top + MarshUi.TITLE_HEIGHT + 22, PANEL_W, PANEL_H};
	}

	private int[] panelButton() {
		int[] p = panelBox();
		return new int[]{p[0] + p[2] - 82, p[1] + p[3] - 26, 72, 18};
	}

	/** Inventory indices of Grand Line items (fruits, swords, guns, accessories). */
	/** The fish bag as stacks (count = how many of that fish). */
	private List<ItemStack> fish() {
		List<ItemStack> out = new ArrayList<>();
		com.grandline.client.ClientFishBag.fish().forEach((id, n) -> {
			var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.tryParse(id));
			if (item != Items.AIR && n > 0) {
				ItemStack st = new ItemStack(item);
				st.getOrCreateTag().putInt("grandline_bag_count", n); // the real count (can exceed a stack)
				out.add(st);
			}
		});
		return out;
	}

	/** Everything shown in the Backpack, in order: owned weapons, carried fruits and accessories, then the fish bag. */
	private List<Entry> entries() {
		List<Entry> list = new ArrayList<>();
		var inv = minecraft.player.getInventory();
		for (var weapon : com.grandline.client.ClientEquipment.ownedWeapons()) {
			// The equipped weapon is shown as the item actually held (it may carry a name); the others as plain items.
			ItemStack shown = new ItemStack(weapon);
			for (ItemStack held : inv.items) {
				if (held.is(weapon)) {
					shown = held;
					break;
				}
			}
			list.add(new Entry(shown, weapon, -1, 0));
		}
		for (int i = 0; i < inv.items.size(); i++) {
			var item = inv.items.get(i).getItem();
			if (item instanceof FruitItem || item instanceof AccessoryItem) {
				list.add(new Entry(inv.items.get(i), null, i, 0));
			}
		}
		for (ItemStack fish : fish()) {
			list.add(new Entry(fish, null, -1, fish.getTag() == null ? 1 : fish.getTag().getInt("grandline_bag_count")));
		}
		return list;
	}

	private int maxScroll(int count) {
		return Math.max(0, (count + COLUMNS - 1) / COLUMNS - ROWS);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		renderBackground(g);
		hovered = ItemStack.EMPTY;
		// Tabs.
		ItemStack[] tabIcons = {new ItemStack(Items.BUNDLE), new ItemStack(Gear.SWORDSMAN_HAT), new ItemStack(Items.CHEST)};
		for (Tab t : Tab.values()) {
			int[] b = tabBox(t);
			boolean over = inside(b, mouseX, mouseY);
			MarshUi.button(g, b[0], b[1], b[2], b[3], t == tab ? MarshUi.Color.YELLOW : MarshUi.Color.DARK, over ? MarshUi.State.HOVER : MarshUi.State.NORMAL);
			g.pose().pushPose();
			g.pose().translate(b[0] + b[2] / 2f - 12, b[1] + 6, 0);
			g.pose().scale(1.5f, 1.5f, 1);
			g.renderItem(tabIcons[t.ordinal()], 0, 0);
			g.pose().popPose();
			Component label = Component.translatable("screen.grandline.items.tab." + t.name().toLowerCase(java.util.Locale.ROOT));
			MarshUi.outlined(g, font, label, b[0] + b[2] / 2 - font.width(label) / 2, b[1] + b[3] - 12, 0xFFFFFF);
		}
		MarshUi.window(g, font, left, top, WIDTH, HEIGHT, title);
		MarshUi.closeButton(g, left + WIDTH - 22, top + 3, 18, closeHit(mouseX, mouseY));
		if (tab == Tab.BACKPACK) {
			renderBackpack(g, mouseX, mouseY);
		} else {
			renderBuild(g, mouseX, mouseY);
		}
		super.render(g, mouseX, mouseY, partialTick);
		if (!hovered.isEmpty()) {
			g.renderTooltip(font, hovered, mouseX, mouseY);
		}
	}

	private void renderBackpack(GuiGraphics g, int mouseX, int mouseY) {
		List<Entry> entries = entries();
		scrollRow = Math.min(scrollRow, maxScroll(entries.size()));
		if (selected != null && entries.stream().noneMatch(e -> e.key().equals(selected.key()))) {
			selected = null; // gone (eaten, worn, revoked)
		} else if (selected != null) {
			String key = selected.key();
			selected = entries.stream().filter(e -> e.key().equals(key)).findFirst().orElse(null);
		}
		if (entries.isEmpty()) {
			g.drawCenteredString(font, Component.translatable("screen.grandline.items.empty"), left + WIDTH / 2, top + 90, 0xA9B2C0);
		}
		boolean panelOpen = selected != null;
		for (int i = 0; i < entries.size(); i++) {
			int[] b = backpackSlot(i);
			if (b == null) {
				continue;
			}
			Entry e = entries.get(i);
			drawSlot(g, b, e.stack(), panelOpen ? -1 : mouseX, panelOpen ? -1 : mouseY);
			if (e.fishCount() > 0) {
				String n = String.valueOf(e.fishCount());
				g.pose().pushPose();
				g.pose().translate(0, 0, 300);
				g.drawString(font, n, b[0] + b[2] - 3 - font.width(n), b[1] + b[3] - 10, 0xFFFFFF, true);
				g.pose().popPose();
			}
			if (e.equipped()) {
				equippedMark(g, b);
			}
			if (selected != null && e.key().equals(selected.key())) {
				outline(g, b[0] + 1, b[1] + 1, b[2] - 2, b[3] - 2, 0xFFFFD25A);
			}
		}
		int max = maxScroll(entries.size());
		if (max > 0) {
			// A slim scroll bar at the grid's right edge.
			int x = left + WIDTH - 9, y0 = top + MarshUi.TITLE_HEIGHT + 8, h = ROWS * 44 - 4;
			g.fill(x, y0, x + 3, y0 + h, 0x40000000);
			int thumb = Math.max(12, h * ROWS / (ROWS + max));
			int ty = y0 + (h - thumb) * scrollRow / max;
			g.fill(x, ty, x + 3, ty + thumb, 0xC0E8B84A);
		}
		g.drawString(font, Component.translatable("screen.grandline.items.backpack_hint"), left + 10, top + HEIGHT - 14, 0xA9B2C0, false);
		if (selected != null) {
			renderInfoPanel(g, selected, mouseX, mouseY);
		}
	}

	/** The equipped mark: a subtle green outline and a small green check badge in the top-right corner. */
	private static void equippedMark(GuiGraphics g, int[] b) {
		g.pose().pushPose();
		g.pose().translate(0, 0, 310);
		outline(g, b[0] + 2, b[1] + 2, b[2] - 4, b[3] - 4, 0x993FCF6A);
		int x = b[0] + b[2] - 13, y = b[1] + 3;
		g.fill(x, y, x + 10, y + 10, 0xFF1E7A3C);
		g.fill(x + 1, y + 1, x + 9, y + 9, EQUIPPED_GREEN);
		int[][] tick = {{2, 5}, {3, 6}, {4, 7}, {5, 6}, {6, 5}, {7, 4}, {8, 3}};
		for (int[] t : tick) {
			g.fill(x + t[0], y + t[1] - 1, x + t[0] + 1, y + t[1] + 1, 0xFFFFFFFF);
		}
		g.pose().popPose();
	}

	private static void outline(GuiGraphics g, int x, int y, int w, int h, int color) {
		g.fill(x, y, x + w, y + 1, color);
		g.fill(x, y + h - 1, x + w, y + h, color);
		g.fill(x, y + 1, x + 1, y + h - 1, color);
		g.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
	}

	/** Rarity id of an item shown in the Backpack ("common" ... "mythical"). */
	private static String rarityOf(ItemStack stack) {
		var item = stack.getItem();
		var spec = com.grandline.gear.sword.Swords.spec(item);
		if (spec != null) {
			return spec.rarity();
		}
		var power = Gear.powerFor(item);
		if (power != null) {
			return power.rarity();
		}
		if (item instanceof FruitItem) {
			for (var def : Fruits.all()) {
				if (def.item() == item) {
					return def.rarity().name().toLowerCase(java.util.Locale.ROOT);
				}
			}
		}
		return switch (stack.getRarity()) {
			case UNCOMMON -> "uncommon";
			case RARE -> "rare";
			case EPIC -> "legendary";
			default -> "common";
		};
	}

	private static int rarityColor(String rarity) {
		try {
			return com.grandline.fruit.FruitRarity.valueOf(rarity.toUpperCase(java.util.Locale.ROOT)).color();
		} catch (IllegalArgumentException e) {
			return 0xFFFFFF;
		}
	}

	/** The selected item's details: texture, name, kind, rarity, mastery, status and Equip / Unequip. */
	private void renderInfoPanel(GuiGraphics g, Entry e, int mouseX, int mouseY) {
		int[] p = panelBox();
		g.pose().pushPose();
		g.pose().translate(0, 0, 400);
		g.fill(left + 4, top + MarshUi.TITLE_HEIGHT, left + WIDTH - 4, top + HEIGHT - 4, 0x90000000);
		MarshUi.window(g, font, p[0], p[1], p[2], p[3], e.stack().getHoverName());
		MarshUi.closeButton(g, p[0] + p[2] - 22, p[1] + 3, 18, panelCloseHit(mouseX, mouseY));
		int cy = p[1] + MarshUi.TITLE_HEIGHT + 6;
		// The texture, enlarged.
		MarshUi.itemFrame(g, p[0] + 10, cy, 56);
		g.pose().pushPose();
		g.pose().translate(p[0] + 14, cy + 4, 0);
		g.pose().scale(3f, 3f, 1);
		g.renderItem(e.stack(), 0, 0);
		g.pose().popPose();
		if (e.equipped()) {
			equippedMark(g, new int[]{p[0] + 10, cy, 56, 56});
		}
		int tx = p[0] + 74, ty = cy + 2;
		var category = e.weapon() != null ? com.grandline.gear.WeaponCategory.of(e.weapon()) : null;
		if (category != null) {
			g.drawString(font, Component.translatable("screen.grandline.items.info.category." + category.key), tx, ty, 0xC9D3E0, false);
			ty += 11;
		}
		String rarity = rarityOf(e.stack());
		g.drawString(font, Component.translatable("screen.grandline.items.info.rarity",
				Component.translatable("rarity.grandline." + rarity).withStyle(st -> st.withColor(rarityColor(rarity)))), tx, ty, 0xC9D3E0, false);
		ty += 11;
		if (category != null) {
			g.drawString(font, masteryLine(e.weapon(), category), tx, ty, 0xC9D3E0, false);
			ty += 11;
			Component status = e.equipped()
					? Component.translatable("screen.grandline.items.info.equipped").withStyle(st -> st.withColor(EQUIPPED_GREEN))
					: Component.translatable("screen.grandline.items.info.stored").withStyle(st -> st.withColor(0xA9B2C0));
			g.drawString(font, Component.translatable("screen.grandline.items.info.status", status), tx, ty, 0xC9D3E0, false);
			ty += 11;
			var current = com.grandline.client.ClientEquipment.equipped(category);
			if (!e.equipped() && current != null) {
				Component replaces = Component.translatable("screen.grandline.items.info.replaces", new ItemStack(current).getHoverName());
				g.pose().pushPose();
				g.pose().translate(tx, ty, 0);
				g.pose().scale(0.75f, 0.75f, 1);
				g.drawString(font, replaces, 0, 0, 0x8F9AAA, false);
				g.pose().popPose();
			}
		} else if (e.stack().getItem() instanceof AccessoryItem) {
			g.pose().pushPose();
			g.pose().translate(p[0] + 10, p[1] + p[3] - 36, 0);
			g.pose().scale(0.75f, 0.75f, 1);
			g.drawString(font, Component.translatable("screen.grandline.items.info.accessory_hint"), 0, 0, 0x8F9AAA, false);
			g.pose().popPose();
		}
		// Equip / Unequip.
		if (category != null || e.stack().getItem() instanceof AccessoryItem) {
			int[] b = panelButton();
			boolean unequip = e.equipped();
			boolean over = inside(b, mouseX, mouseY);
			if (!over && pendingKey == null) {
				needsRehover = false;
			}
			MarshUi.button(g, b[0], b[1], b[2], b[3], pending() ? MarshUi.Color.GRAY : unequip ? MarshUi.Color.RED : MarshUi.Color.GREEN,
					pending() ? MarshUi.State.PRESSED : over ? MarshUi.State.HOVER : MarshUi.State.NORMAL);
			Component label = Component.translatable(unequip ? "screen.grandline.items.info.unequip" : "screen.grandline.items.info.equip");
			MarshUi.outlined(g, font, label, b[0] + b[2] / 2 - font.width(label) / 2, b[1] + 5, 0xFFFFFF);
		}
		g.pose().popPose();
	}

	private Component masteryLine(net.minecraft.world.item.Item weapon, com.grandline.gear.WeaponCategory category) {
		if (category == com.grandline.gear.WeaponCategory.ROD) {
			return Component.translatable("screen.grandline.items.info.fishing_mastery", com.grandline.client.ClientFishing.level());
		}
		var power = Gear.powerFor(weapon);
		var view = ClientProfileCache.view();
		int level = 1;
		if (power != null && view != null && view.masteryExperiencePerLevel() > 0) {
			long xp = view.profile().powers().masteryXp(power.id().toString());
			level = (int) Math.min(view.maxMastery(), 1 + xp / view.masteryExperiencePerLevel());
		}
		return Component.translatable("screen.grandline.items.info.mastery", level);
	}

	/**
	 * Whether the Equip / Unequip button is locked: while a request waits for the server's answer (at most 1.5 s), and
	 * after it, until the mouse has left the button (so rapid clicks in place never undo what the first one did).
	 */
	private boolean pending() {
		if (pendingKey != null) {
			boolean answered = selected != null && selected.key().equals(pendingKey) && selected.weapon() != null
					&& selected.equipped() != pendingWasEquipped;
			if (answered || net.minecraft.Util.getMillis() > pendingUntil) {
				pendingKey = null;
				needsRehover = true;
			}
		}
		return pendingKey != null || needsRehover;
	}

	private boolean panelCloseHit(double mouseX, double mouseY) {
		int[] p = panelBox();
		return mouseX >= p[0] + p[2] - 22 && mouseX < p[0] + p[2] - 4 && mouseY >= p[1] + 3 && mouseY < p[1] + 21;
	}

	private void renderBuild(GuiGraphics g, int mouseX, int mouseY) {
		int y = top + MarshUi.TITLE_HEIGHT + 6;
		// Loadout: fighting style, gun, fruit, sword.
		var profile = ClientProfileCache.profile();
		ItemStack fruit = profile != null && profile.powers().hasFruit() ? stackForFruit(profile.powers().fruit()) : ItemStack.EMPTY;
		ItemStack[] loadout = {new ItemStack(Items.BLAZE_POWDER), first(GunItem.class), fruit, first(GearSwordItem.class)};
		String[] labels = {"style", "gun", "fruit", "sword"};
		for (int i = 0; i < 4; i++) {
			int[] b = {left + 12 + i * 46, y, 40, 40};
			drawSlot(g, b, loadout[i], mouseX, mouseY);
			Component label = Component.translatable("screen.grandline.items.loadout." + labels[i]);
			g.drawString(font, label, b[0] + 20 - font.width(label) / 2, b[1] + 42, 0xC9D3E0, false);
		}
		// Accessory and rings.
		var gear = ClientGear.gear();
		String[] gearLabels = {"screen.grandline.gear.accessory", "screen.grandline.gear.ring", "screen.grandline.gear.ring"};
		for (int slot = 0; slot < 3; slot++) {
			int[] b = gearSlot(slot);
			drawSlot(g, b, stackOf(gear.get(slot)), mouseX, mouseY);
			Component label = Component.translatable(gearLabels[slot]);
			g.drawString(font, label, b[0] + 20 - font.width(label) / 2, b[1] + 42, 0xC9D3E0, false);
		}
		g.drawString(font, Component.translatable("screen.grandline.gear.hint"), left + 12, top + HEIGHT - 14, 0xA9B2C0, false);
		// Build Stats.
		int sx = left + 200;
		int sy = y;
		MarshUi.row(g, sx - 6, sy - 4, WIDTH - 200, HEIGHT - MarshUi.TITLE_HEIGHT - 12, MarshUi.Row.DARK);
		MarshUi.outlined(g, font, Component.translatable("screen.grandline.items.build_stats"), sx, sy, 0xFFFFFF);
		sy += 13;
		boolean any = false;
		for (int slot = 0; slot < 3; slot++) {
			ItemStack stack = stackOf(gear.get(slot));
			if (stack.getItem() instanceof AccessoryItem accessory) {
				any = true;
				g.drawString(font, stack.getHoverName(), sx, sy, 0xFFD25A, false);
				sy += 10;
				for (Component line : accessory.kind().bonuses().describe()) {
					sy += drawSmall(g, line, sx + 4, sy);
				}
				sy += 2;
			}
		}
		if (!any) {
			g.drawString(font, Component.translatable("screen.grandline.gear.none"), sx, sy, 0x7A8290, false);
		} else {
			sy += 2;
			g.drawString(font, Component.translatable("screen.grandline.gear.bonuses"), sx, sy, 0x8FB8FF, false);
			sy += 10;
			for (Component line : ClientGear.bonuses().describe()) {
				sy += drawSmall(g, line, sx + 4, sy);
			}
		}
	}

	/** Draws a bonus line at 75% size, wrapped to the Build Stats panel. Returns the height used. */
	private int drawSmall(GuiGraphics g, Component text, int x, int y) {
		int maxWidth = (int) ((left + WIDTH - 10 - x) / 0.75f);
		int used = 0;
		for (var part : font.split(text, maxWidth)) {
			g.pose().pushPose();
			g.pose().translate(x, y + used, 0);
			g.pose().scale(0.75f, 0.75f, 1);
			g.drawString(font, part, 0, 0, 0x9CC3FF, false);
			g.pose().popPose();
			used += 8;
		}
		return used;
	}

	private void drawSlot(GuiGraphics g, int[] b, ItemStack stack, int mouseX, int mouseY) {
		MarshUi.itemFrame(g, b[0], b[1], b[2]);
		if (!stack.isEmpty()) {
			g.pose().pushPose();
			g.pose().translate(b[0] + 4, b[1] + 4, 0);
			g.pose().scale(2f, 2f, 1);
			g.renderItem(stack, 0, 0);
			g.pose().popPose();
			if (inside(b, mouseX, mouseY)) {
				hovered = stack;
			}
		}
	}

	private ItemStack first(Class<?> type) {
		var player = minecraft.player;
		if (type.isInstance(player.getMainHandItem().getItem())) {
			return player.getMainHandItem();
		}
		for (ItemStack stack : player.getInventory().items) {
			if (type.isInstance(stack.getItem())) {
				return stack;
			}
		}
		return ItemStack.EMPTY;
	}

	private static ItemStack stackForFruit(String fruitId) {
		var def = Fruits.get(fruitId);
		return def == null ? ItemStack.EMPTY : new ItemStack(def.item());
	}

	private static ItemStack stackOf(String itemId) {
		ResourceLocation id = ResourceLocation.tryParse(itemId);
		return itemId.isEmpty() || id == null ? ItemStack.EMPTY : new ItemStack(BuiltInRegistries.ITEM.get(id));
	}

	private static boolean inside(int[] b, double x, double y) {
		return x >= b[0] && x < b[0] + b[2] && y >= b[1] && y < b[1] + b[3];
	}

	private boolean closeHit(double mouseX, double mouseY) {
		return mouseX >= left + WIDTH - 22 && mouseX < left + WIDTH - 4 && mouseY >= top + 3 && mouseY < top + 21;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button != 0) {
			return super.mouseClicked(mouseX, mouseY, button);
		}
		if (closeHit(mouseX, mouseY)) {
			onClose();
			return true;
		}
		for (Tab t : Tab.values()) {
			if (inside(tabBox(t), mouseX, mouseY)) {
				if (t == Tab.STASH) {
					minecraft.setScreen(new StashScreen());
				} else {
					tab = t;
						selected = null;
				}
				return true;
			}
		}
		if (tab == Tab.BUILD) {
			for (int slot = 0; slot < 3; slot++) {
				if (inside(gearSlot(slot), mouseX, mouseY) && !ClientGear.gear().get(slot).isEmpty()) {
					ClientNetworking.requestUnequip(slot);
					return true;
				}
			}
		} else if (tab == Tab.BACKPACK) {
			if (selected != null) {
				if (panelCloseHit(mouseX, mouseY)) {
					selected = null;
					return true;
				}
				int[] b = panelButton();
				boolean actionable = selected.weapon() != null || selected.stack().getItem() instanceof AccessoryItem;
				if (actionable && inside(b, mouseX, mouseY)) {
					if (!pending()) { // one request at a time: rapid clicks never flip it back and forth
						pendingKey = selected.key();
						pendingWasEquipped = selected.equipped();
						pendingUntil = net.minecraft.Util.getMillis() + 1500;
						if (selected.weapon() != null) {
							com.grandline.client.ClientEquipment.request(selected.weapon(), !selected.equipped());
						} else {
							ClientNetworking.requestEquipFromSlot(selected.invSlot());
						}
						minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
								net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_GENERIC, 1.0F));
					}
					return true;
				}
				if (!inside(panelBox(), mouseX, mouseY)) {
					selected = null; // a click outside closes the details
				}
				return true;
			}
			List<Entry> entries = entries();
			for (int i = 0; i < entries.size(); i++) {
				int[] b = backpackSlot(i);
				if (b != null && inside(b, mouseX, mouseY)) {
					selected = entries.get(i);
					pendingKey = null;
					needsRehover = false;
					minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
							net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
					return true;
				}
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
		if (tab == Tab.BACKPACK && selected == null) {
			scrollRow = Math.max(0, Math.min(maxScroll(entries().size()), scrollRow - (int) Math.signum(delta)));
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, delta);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE && selected != null) {
			selected = null; // Esc closes the details first, then the screen
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
