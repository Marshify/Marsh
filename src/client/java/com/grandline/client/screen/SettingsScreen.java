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

/** Settings (a scrolling list when taller than the screen): On/Off rows for visual and HUD options, plus HUD layout editing. Saved to config/grandline-client.json. */
public final class SettingsScreen extends Screen {
	private static final int WIDTH = 320;
	private static final int ROW = 34;

	private record Toggle(String key, Supplier<Boolean> get, Consumer<Boolean> set) {
	}

	/** A setting with a few levels (Low / Medium / High by default). */
	private record Choice(String key, Supplier<Integer> get, Consumer<Integer> set, String[] labels) {
		Choice(String key, Supplier<Integer> get, Consumer<Integer> set) {
			this(key, get, set, new String[]{"low", "medium", "high"});
		}
	}

	private static final String[] VOLUME = {"vol_off", "low", "medium", "vol_full"};

	/** A 0-100% slider (applied as it moves, saved when released). */
	private record Slider(String key, Supplier<Integer> get, Consumer<Integer> set) {
	}

	private final List<MarshButton[]> choiceButtons = new ArrayList<>();
	private final List<Choice> choiceOf = new ArrayList<>();

	private final Screen parent;
	private final List<Object> rows = new ArrayList<>(); // section title (String) or Toggle
	private int left;
	private int top;
	private int panelHeight;
	/** The scrolling list: everything below the title bar. */
	private int viewTop, viewBottom, contentHeight, scroll;
	private boolean draggingBar;
	private final List<net.minecraft.client.gui.components.AbstractWidget> scrolled = new ArrayList<>();
	private final List<Integer> baseY = new ArrayList<>();

	public SettingsScreen(Screen parent) {
		super(Component.translatable("screen.grandline.settings.title"));
		this.parent = parent;
		ClientSettings s = ClientSettings.get();
		rows.add("screen.grandline.settings.section.visuals");
		rows.add(new Toggle("ally_vfx", () -> s.allyVfx, v -> s.allyVfx = v));
		rows.add(new Toggle("camera_shake", () -> s.cameraShake, v -> s.cameraShake = v));
		rows.add(new Toggle("stylized_lighting", () -> s.stylizedLighting, v -> {
			s.stylizedLighting = v;
			if (net.minecraft.client.Minecraft.getInstance().levelRenderer != null) {
				net.minecraft.client.Minecraft.getInstance().levelRenderer.allChanged(); // re-light every chunk
			}
		}));
		rows.add(new Choice("fx_density", () -> s.fxDensity, v -> s.fxDensity = v, new String[]{"low", "medium", "high", "ultra"}));
		rows.add("screen.grandline.settings.section.camera");
		rows.add(new Toggle("custom_camera", () -> s.customCamera, v -> s.customCamera = v));
		rows.add(new Choice("camera_distance", () -> s.cameraDistance, v -> s.cameraDistance = v, new String[]{"near", "medium", "far"}));
		rows.add(new Choice("camera_sensitivity", () -> s.cameraSensitivity, v -> s.cameraSensitivity = v));
		rows.add(new Toggle("reduced_motion", () -> s.reducedMotion, v -> s.reducedMotion = v));
		rows.add("screen.grandline.settings.section.sound");
		rows.add(new Choice("ability_sfx", () -> s.abilitySfx, v -> s.abilitySfx = v, VOLUME));
		rows.add(new Choice("weapon_sfx", () -> s.weaponSfx, v -> s.weaponSfx = v, VOLUME));
		rows.add(new Choice("movement_sfx", () -> s.movementSfx, v -> s.movementSfx = v, VOLUME));
		rows.add(new Choice("environment_sfx", () -> s.environmentSfx, v -> s.environmentSfx = v, VOLUME));
		rows.add(new Slider("soundtrack_volume", () -> s.soundtrackVolume, v -> s.soundtrackVolume = v));
		rows.add("screen.grandline.settings.section.hud");
		rows.add(new Toggle("move_list", () -> s.showMoveList, v -> s.showMoveList = v));
		rows.add(new Toggle("quest_tracker", () -> s.showQuestTracker, v -> s.showQuestTracker = v));
		rows.add(new Toggle("menu_button", () -> s.showMenuButton, v -> s.showMenuButton = v));
		rows.add(new Toggle("custom_hotbar", () -> s.customHotbar, v -> s.customHotbar = v));
	}

	@Override
	protected void init() {
		scrolled.clear();
		baseY.clear();
		toggleButtons.clear();
		toggleOf.clear();
		choiceButtons.clear();
		choiceOf.clear();
		contentHeight = 0;
		for (Object row : rows) {
			contentHeight += row instanceof String ? 14 : ROW;
		}
		contentHeight += ROW + 4; // the HUD layout buttons
		// The window fits the screen; the list scrolls when it is taller than that.
		panelHeight = Math.min(MarshUi.TITLE_HEIGHT + 8 + contentHeight + 4, this.height - 8);
		left = width / 2 - WIDTH / 2;
		top = Math.max(4, this.height / 2 - panelHeight / 2);
		viewTop = top + MarshUi.TITLE_HEIGHT + 4;
		viewBottom = top + panelHeight - 4;
		int y = viewTop + 2;
		for (Object row : rows) {
			if (row instanceof Toggle toggle) {
				MarshButton on = add(new MarshButton(left + WIDTH - 118, y + 7, 52, 20, Component.translatable("screen.grandline.settings.on"),
						MarshUi.Color.GRAY, () -> set(toggle, true)));
				MarshButton off = add(new MarshButton(left + WIDTH - 62, y + 7, 52, 20, Component.translatable("screen.grandline.settings.off"),
						MarshUi.Color.GRAY, () -> set(toggle, false)));
				toggleButtons.add(new MarshButton[]{on, off});
				toggleOf.add(toggle);
				y += ROW;
			} else if (row instanceof Choice choice) {
				String[] labels = choice.labels();
				MarshButton[] buttons = new MarshButton[labels.length];
				for (int i = 0; i < labels.length; i++) {
					int level = i;
					buttons[i] = add(new MarshButton(left + WIDTH - 10 - labels.length * 37 + i * 37, y + 7, 35, 20,
							Component.translatable("screen.grandline.settings." + labels[i]), MarshUi.Color.GRAY, () -> {
								choice.set().accept(level);
								ClientSettings.get().save();
								refresh();
							}));
				}
				choiceButtons.add(buttons);
				choiceOf.add(choice);
				y += ROW;
			} else if (row instanceof Slider slider) {
				add(new net.minecraft.client.gui.components.AbstractSliderButton(left + WIDTH - 120, y + 7, 110, 20,
						Component.empty(), slider.get().get() / 100.0) {
					{
						updateMessage();
					}

					@Override
					protected void updateMessage() {
						setMessage(Component.literal(Math.round(value * 100) + "%"));
					}

					@Override
					protected void applyValue() {
						slider.set().accept((int) Math.round(value * 100)); // the music follows at once
					}

					@Override
					public void onRelease(double mouseX, double mouseY) {
						super.onRelease(mouseX, mouseY);
						ClientSettings.get().save();
					}
				});
				y += ROW;
			} else {
				y += 14;
			}
		}
		add(new MarshButton(left + 10, y + 6, 140, 20, Component.translatable("screen.grandline.settings.move_hud"),
				MarshUi.Color.YELLOW, () -> minecraft.setScreen(new HudEditScreen(this))));
		add(new MarshButton(left + 158, y + 6, 100, 20, Component.translatable("screen.grandline.settings.reset_hud"),
				MarshUi.Color.BLUE, () -> {
					ClientSettings.get().hudOffsets.clear();
					ClientSettings.get().save();
				}));
		refresh();
		setScroll(scroll); // keeps the position when the window is resized
	}

	private <T extends net.minecraft.client.gui.components.AbstractWidget> T add(T button) {
		scrolled.add(button);
		baseY.add(button.getY());
		return addRenderableWidget(button);
	}

	private int maxScroll() {
		return Math.max(0, contentHeight + 4 - (viewBottom - viewTop));
	}

	private void setScroll(int value) {
		scroll = Math.max(0, Math.min(maxScroll(), value));
		for (int i = 0; i < scrolled.size(); i++) {
			var b = scrolled.get(i);
			b.setY(baseY.get(i) - scroll);
			// Only buttons fully inside the list can be seen or clicked.
			b.visible = b.getY() >= viewTop && b.getY() + b.getHeight() <= viewBottom;
		}
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
		if (maxScroll() > 0) {
			setScroll(scroll - (int) Math.round(delta * 20));
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, delta);
	}

	private int[] barRect() {
		int track = viewBottom - viewTop;
		int thumb = Math.max(20, track * track / (contentHeight + 4));
		int y = viewTop + (maxScroll() == 0 ? 0 : (track - thumb) * scroll / maxScroll());
		return new int[]{left + WIDTH - 6, y, 3, thumb};
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
		if (draggingBar && maxScroll() > 0) {
			int[] bar = barRect();
			int track = viewBottom - viewTop - bar[3];
			setScroll((int) Math.round((mouseY - viewTop - bar[3] / 2.0) * maxScroll() / Math.max(1, track)));
			return true;
		}
		return super.mouseDragged(mouseX, mouseY, button, dx, dy);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		draggingBar = false;
		return super.mouseReleased(mouseX, mouseY, button);
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
		for (int i = 0; i < choiceOf.size(); i++) {
			int v = choiceOf.get(i).get().get();
			for (int b = 0; b < choiceButtons.get(i).length; b++) {
				choiceButtons.get(i)[b].setColor(b == v ? MarshUi.Color.GREEN : MarshUi.Color.GRAY);
			}
		}
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		renderBackground(g);
		MarshUi.window(g, font, left, top, WIDTH, panelHeight, title);
		boolean overClose = closeHit(mouseX, mouseY);
		MarshUi.closeButton(g, left + WIDTH - 22, top + 3, 18, overClose);
		g.enableScissor(left, viewTop, left + WIDTH, viewBottom);
		int y = viewTop + 2 - scroll;
		for (Object row : rows) {
			if (row instanceof String section) {
				g.drawString(font, Component.translatable(section), left + 10, y + 3, 0xA9B2C0);
				y += 14;
			} else if (row instanceof Toggle || row instanceof Choice || row instanceof Slider) {
				String key = row instanceof Toggle toggle ? toggle.key() : row instanceof Choice choice ? choice.key() : ((Slider) row).key();
				MarshUi.row(g, left + 6, y, WIDTH - 12, ROW - 2, MarshUi.Row.DARK);
				MarshUi.outlined(g, font, Component.translatable("setting.grandline." + key), left + 14, y + 6, 0xFFFFFF);
				// Description at 75% size, wrapped to stay clear of the On/Off buttons.
				int descWidth = (int) ((WIDTH - 14 - 124) / 0.75f);
				int dy = 0;
				for (var part : font.split(Component.translatable("setting.grandline." + key + ".desc"), descWidth)) {
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
		g.disableScissor();
		super.render(g, mouseX, mouseY, partialTick);
		if (maxScroll() > 0) {
			int[] bar = barRect();
			g.fill(bar[0], viewTop, bar[0] + bar[2], viewBottom, 0x40000000);
			g.fill(bar[0], bar[1], bar[0] + bar[2], bar[1] + bar[3], draggingBar ? 0xFFFFFFFF : 0xC0FFFFFF);
		}
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
		if (button == 0 && maxScroll() > 0 && mouseX >= left + WIDTH - 9 && mouseX < left + WIDTH - 1 && mouseY >= viewTop && mouseY < viewBottom) {
			draggingBar = true;
			mouseDragged(mouseX, mouseY, button, 0, 0);
			return true;
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public void onClose() {
		ClientSettings.get().save(); // keeps a slider value changed with the keyboard
		minecraft.setScreen(parent instanceof MenuScreen ? null : parent);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
