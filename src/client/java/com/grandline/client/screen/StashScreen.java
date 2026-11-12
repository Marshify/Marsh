package com.grandline.client.screen;

import com.grandline.client.ClientProfileCache;
import com.grandline.client.ui.GoldButton;
import com.grandline.client.ui.GrandLineUi;
import com.grandline.fruit.Fruits;
import com.grandline.network.payload.FruitActionPayload;
import com.grandline.network.payload.ProfileSyncPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Grid of stored fruits (rarity-coloured tiles with counts). Select a tile, then Withdraw. Data comes from the server sync. */
public final class StashScreen extends Screen {
	private static final int TILE = 44;
	private static final int COLUMNS = 4;

	private String selected;
	private GoldButton withdraw;
	private int left;
	private int top;
	private int panelWidth;

	public StashScreen() {
		super(Component.translatable("screen.grandline.stash.title"));
	}

	@Override
	protected void init() {
		panelWidth = COLUMNS * (TILE + 4) + 16;
		left = width / 2 - panelWidth / 2;
		top = Math.max(10, height / 2 - 110);
		withdraw = addRenderableWidget(new GoldButton(left + panelWidth / 2 - 50, top + 200, 100, 18,
				Component.translatable("screen.grandline.stash.withdraw"), b -> {
			if (selected != null) {
				FruitDialogs.send(FruitActionPayload.STASH_WITHDRAW, selected, 0);
			}
		}));
	}

	private List<Fruits.FruitDef> stored() {
		List<Fruits.FruitDef> list = new ArrayList<>();
		ProfileSyncPayload view = ClientProfileCache.view();
		if (view != null) {
			for (Fruits.FruitDef fruit : Fruits.all()) {
				if (view.profile().powers().stashCount(fruit.id()) > 0) {
					list.add(fruit);
				}
			}
		}
		return list;
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		renderBackground(graphics);
		int contentTop = GrandLineUi.panel(graphics, font, left, top, panelWidth, 222, title);
		List<Fruits.FruitDef> fruits = stored();
		ProfileSyncPayload view = ClientProfileCache.view();

		if (fruits.isEmpty()) {
			graphics.drawWordWrap(font, Component.translatable("screen.grandline.stash.empty"), left + 10, contentTop + 10, panelWidth - 20, 0xA9B4C4);
		}
		if (selected != null && fruits.stream().noneMatch(f -> f.id().equals(selected))) {
			selected = null;
		}

		for (int i = 0; i < fruits.size(); i++) {
			Fruits.FruitDef fruit = fruits.get(i);
			int x = left + 8 + (i % COLUMNS) * (TILE + 4);
			int y = contentTop + (i / COLUMNS) * (TILE + 4);
			boolean hovered = mouseX >= x && mouseX < x + TILE && mouseY >= y && mouseY < y + TILE;
			GrandLineUi.tile(graphics, x, y, TILE, TILE, fruit.rarity().color(), fruit.id().equals(selected) || hovered);
			graphics.pose().pushPose();
			graphics.pose().translate(x + 6, y + 4, 0);
			graphics.pose().scale(2, 2, 1);
			graphics.renderItem(new ItemStack(fruit.item()), 0, 0);
			graphics.pose().popPose();
			int count = view == null ? 0 : view.profile().powers().stashCount(fruit.id());
			graphics.pose().pushPose();
			graphics.pose().translate(0, 0, 200);
			graphics.drawString(font, String.valueOf(count), x + 4, y + TILE - 11, 0xFFFFFF);
			Component name = Component.translatable(fruit.power().translationKey());
			graphics.drawString(font, name, x + TILE - 3 - font.width(name), y + TILE - 11, 0xFFFFFF);
			graphics.pose().popPose();
		}

		if (view != null) {
			graphics.drawCenteredString(font, Component.translatable("screen.grandline.stash.capacity",
					view.profile().powers().stashTotal(), view.stashCapacity()), left + panelWidth / 2, top + 186, 0xA9B4C4);
		}
		withdraw.active = selected != null;
		super.render(graphics, mouseX, mouseY, partialTick);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		List<Fruits.FruitDef> fruits = stored();
		int contentTop = top + 26;
		for (int i = 0; i < fruits.size(); i++) {
			int x = left + 8 + (i % COLUMNS) * (TILE + 4);
			int y = contentTop + (i / COLUMNS) * (TILE + 4);
			if (mouseX >= x && mouseX < x + TILE && mouseY >= y && mouseY < y + TILE) {
				selected = fruits.get(i).id();
				return true;
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
