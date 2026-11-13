package com.grandline.client.screen;

import com.grandline.client.hud.ProfileHud;
import com.grandline.client.ui.MarshUi;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * The Menu: opens right where the HUD's Menu button sits (the HUD stays visible behind it) and turns it into a grid of
 * big buttons: Stats, Items, Settings, Map and Close. The mouse is free while it is open.
 */
public final class MenuScreen extends Screen {
	public MenuScreen() {
		super(Component.translatable("screen.grandline.menu.title"));
	}

	@Override
	protected void init() {
		int[] box = ProfileHud.menuButtonBox();
		int x = box[0];
		int y = box[1];
		int w = (box[2] - 4) / 2;
		int h = 22;
		add(x, y, w, h, "screen.grandline.menu.stats", () -> minecraft.setScreen(new StatsScreen()));
		add(x + w + 4, y, w, h, "screen.grandline.menu.items", () -> minecraft.setScreen(new ItemsScreen(ItemsScreen.Tab.BUILD)));
		add(x, y + h + 4, w, h, "screen.grandline.menu.settings", () -> minecraft.setScreen(new SettingsScreen(this)));
		add(x + w + 4, y + h + 4, w, h, "screen.grandline.menu.map", () -> minecraft.setScreen(new WorldMapScreen()));
		add(x, y + 2 * (h + 4), w, h, "screen.grandline.menu.ranks", () -> minecraft.setScreen(new LeaderboardScreen()));
		add(x + w + 4, y + 2 * (h + 4), w, h, "screen.grandline.menu.close", this::onClose);
	}

	private void add(int x, int y, int w, int h, String key, Runnable action) {
		addRenderableWidget(new Tile(x, y, w, h, Component.translatable(key), action));
	}

	@Override
	public void renderBackground(GuiGraphics graphics) {
		// No dimming: the HUD and world stay visible, like a pop-out from the Menu button.
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	/** Big yellow menu tile (hud_buttons sprite). */
	private static final class Tile extends AbstractButton {
		private final Runnable action;

		Tile(int x, int y, int w, int h, Component message, Runnable action) {
			super(x, y, w, h, message);
			this.action = action;
		}

		@Override
		public void onPress() {
			action.run();
		}

		@Override
		protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
			MarshUi.menuButton(g, getX(), getY(), width, height, isHoveredOrFocused() ? MarshUi.State.HOVER : MarshUi.State.NORMAL);
			var font = net.minecraft.client.Minecraft.getInstance().font;
			MarshUi.outlined(g, font, getMessage(), getX() + width / 2 - font.width(getMessage()) / 2, getY() + (height - 8) / 2, 0xFFFFFF);
		}

		@Override
		protected void updateWidgetNarration(NarrationElementOutput output) {
			defaultButtonNarrationText(output);
		}
	}
}
