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
 *   <li>Backpack: every Grand Line item you carry; click an accessory or ring to equip it.</li>
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

	private int[] backpackSlot(int i) {
		return new int[]{left + 10 + (i % 7) * 44, top + MarshUi.TITLE_HEIGHT + 8 + (i / 7) * 44, 40, 40};
	}

	/** Inventory indices of Grand Line items (fruits, swords, guns, accessories). */
	private List<Integer> backpack() {
		List<Integer> list = new ArrayList<>();
		var items = minecraft.player.getInventory().items;
		for (int i = 0; i < items.size(); i++) {
			var item = items.get(i).getItem();
			if (item instanceof FruitItem || item instanceof AccessoryItem || item instanceof GunItem || item instanceof GearSwordItem) {
				list.add(i);
			}
		}
		return list;
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
		List<Integer> slots = backpack();
		if (slots.isEmpty()) {
			g.drawCenteredString(font, Component.translatable("screen.grandline.items.empty"), left + WIDTH / 2, top + 90, 0xA9B2C0);
			return;
		}
		for (int i = 0; i < slots.size() && i < 28; i++) {
			int[] b = backpackSlot(i);
			ItemStack stack = minecraft.player.getInventory().items.get(slots.get(i));
			drawSlot(g, b, stack, mouseX, mouseY);
		}
		g.drawString(font, Component.translatable("screen.grandline.items.backpack_hint"), left + 10, top + HEIGHT - 14, 0xA9B2C0, false);
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
			List<Integer> slots = backpack();
			for (int i = 0; i < slots.size() && i < 28; i++) {
				if (inside(backpackSlot(i), mouseX, mouseY)
						&& minecraft.player.getInventory().items.get(slots.get(i)).getItem() instanceof AccessoryItem) {
					ClientNetworking.requestEquipFromSlot(slots.get(i));
					return true;
				}
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
