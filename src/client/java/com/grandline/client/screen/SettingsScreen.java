package com.grandline.client.screen;

import com.grandline.client.settings.ClientSettings;
import com.grandline.client.ui.MarshButton;
import com.grandline.client.ui.MarshUi;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Settings: On/Off rows for visual and HUD options, plus HUD layout editing. Saved to config/grandline-client.json. */
public final class SettingsScreen extends Screen {
	private static final int WIDTH = 320;
	private static final int ROW = 34;

	private record Toggle(String key, Supplier<Boolean> get, Consumer<Boolean> set) {
	}

	private final Screen parent;
	private final List<Object> rows = new ArrayList<>(); // section title (String) or Toggle
	private int left;
	private int top;
	private int height;

	public SettingsScreen(Screen parent) {
		super(Component.translatable("screen.grandline.settings.title"));
		this.parent = parent;
		ClientSettings s = ClientSettings.get();
		rows.add("screen.grandline.settings.section.visuals");
		rows.add(new Toggle("ally_vfx", () -> s.allyVfx, v -> s.allyVfx = v));
		rows.add(new Toggle("camera_shake", () -> s.cameraShake, v -> s.cameraShake = v));
		rows.add("screen.grandline.settings.section.hud");
		rows.add(new Toggle("move_list", () -> s.showMoveList, v -> s.showMoveList = v));
		rows.add(new Toggle("quest_tracker", () -> s.showQuestTracker, v -> s.showQuestTracker = v));
		rows.add(new Toggle("menu_button", () -> s.showMenuButton, v -> s.showMenuButton = v));
		rows.add(new Toggle("custom_hotbar", () -> s.customHotbar, v -> s.customHotbar = v));
	}

	@Override
	protected void init() {
		int contentHeight = 0;
		for (Object row : rows) {
			contentHeight += row instanceof String ? 14 : ROW;
		}
		height = MarshUi.TITLE_HEIGHT + 8 + contentHeight + ROW + 6;
		left = width / 2 - WIDTH / 2;
		top = Math.max(4, this.height / 2 - height / 2);
		int y = top + MarshUi.TITLE_HEIGHT + 6;
		for (Object row : rows) {
			if (row instanceof Toggle toggle) {
				MarshButton on = addRenderableWidget(new MarshButton(left + WIDTH - 118, y + 7, 52, 20, Component.translatable("screen.grandline.settings.on"),
						MarshUi.Color.GRAY, () -> set(toggle, true)));
				MarshButton off = addRenderableWidget(new MarshButton(left + WIDTH - 62, y + 7, 52, 20, Component.translatable("screen.grandline.settings.off"),
						MarshUi.Color.GRAY, () -> set(toggle, false)));
				toggleButtons.add(new MarshButton[]{on, off});
				toggleOf.add(toggle);
				y += ROW;
			} else {
				y += 14;
			}
		}
		addRenderableWidget(new MarshButton(left + 10, y + 6, 140, 20, Component.translatable("screen.grandline.settings.move_hud"),
				MarshUi.Color.YELLOW, () -> minecraft.setScreen(new HudEditScreen(this))));
		addRenderableWidget(new MarshButton(left + 158, y + 6, 100, 20, Component.translatable("screen.grandline.settings.reset_hud"),
				MarshUi.Color.BLUE, () -> {
					ClientSettings.get().hudOffsets.clear();
					ClientSettings.get().save();
				}));
		refresh();
	}

	private final List<MarshButton[]> toggleButtons = new ArrayList<>();
	private final List<Toggle> toggleOf = new ArrayList<>();

	private void set(Toggle toggle, boolean value) {
		toggle.set().accept(value);
		ClientSettings.get().save();
		refresh();
	}

	private void refresh() {
		for (int i = 0; i < toggleOf.size(); i++) {
			boolean v = toggleOf.get(i).get().get();
			toggleButtons.get(i)[0].setColor(v ? MarshUi.Color.GREEN : MarshUi.Color.GRAY);
			toggleButtons.get(i)[1].setColor(v ? MarshUi.Color.GRAY : MarshUi.Color.RED);
		}
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		renderBackground(g);
		MarshUi.window(g, font, left, top, WIDTH, height, title);
		boolean overClose = closeHit(mouseX, mouseY);
		MarshUi.closeButton(g, left + WIDTH - 22, top + 3, 18, overClose);
		int y = top + MarshUi.TITLE_HEIGHT + 6;
		for (Object row : rows) {
			if (row instanceof String section) {
				g.drawString(font, Component.translatable(section), left + 10, y + 3, 0xA9B2C0);
				y += 14;
			} else if (row instanceof Toggle toggle) {
				MarshUi.row(g, left + 6, y, WIDTH - 12, ROW - 2, MarshUi.Row.DARK);
				MarshUi.outlined(g, font, Component.translatable("setting.grandline." + toggle.key()), left + 14, y + 6, 0xFFFFFF);
				// Description at 75% size, wrapped to stay clear of the On/Off buttons.
				int descWidth = (int) ((WIDTH - 14 - 124) / 0.75f);
				int dy = 0;
				for (var part : font.split(Component.translatable("setting.grandline." + toggle.key() + ".desc"), descWidth)) {
					g.pose().pushPose();
					g.pose().translate(left + 14, y + 17 + dy, 0);
					g.pose().scale(0.75f, 0.75f, 1);
					g.drawString(font, part, 0, 0, 0xA9B2C0, false);
					g.pose().popPose();
					dy += 7;
				}
				y += ROW;
			}
		}
		super.render(g, mouseX, mouseY, partialTick);
	}

	private boolean closeHit(double mouseX, double mouseY) {
		return mouseX >= left + WIDTH - 22 && mouseX < left + WIDTH - 4 && mouseY >= top + 3 && mouseY < top + 21;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == 0 && closeHit(mouseX, mouseY)) {
			onClose();
			return true;
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public void onClose() {
		minecraft.setScreen(parent instanceof MenuScreen ? null : parent);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
