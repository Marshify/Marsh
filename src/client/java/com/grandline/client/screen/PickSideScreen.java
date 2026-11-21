package com.grandline.client.screen;

import com.grandline.client.network.ClientNetworking;
import com.grandline.client.ui.MarshUi;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/**
 * Pick a Side: Pirates or Marines, shown every time you join (and from the Menu). A compact menu in the middle of the
 * screen: a small title, two translucent cards side by side (emblem, name, the side's rules; red for Pirates, blue for
 * Marines; the current side marked "Selected"), the current side underneath and a close button once a side is chosen.
 * The world stays visible behind it (light dimming). Click a card to join that side. Before your first choice the menu
 * cannot be closed without picking.
 *
 * <p>The menu is laid out at a fixed natural size, then scaled as a whole to fit about 45% of the screen width and 40% of
 * its height (see {@link #scale}), so it looks the same at every resolution and GUI scale.
 */
public final class PickSideScreen extends Screen {
	private static final int CARD_W = 104;
	private static final int CARD_H = 92;
	private static final int GAP = 6;
	private static final int TITLE_H = 16;
	private static final int FOOTER_H = 14;
	private static final int MENU_W = CARD_W * 2 + GAP;
	private static final int MENU_H = TITLE_H + CARD_H + FOOTER_H;
	private static final int CARD_BACK = 0xA60B1422;   // ~65% opaque dark navy
	private static final int CARD_HOVER = 0xB8152238;
	private static final int[] ACCENT = {0xFFC8323A, 0xFF3A78E0};
	private static final int[] BANNER = {0xB0A01818, 0xB01848B0};
	private static final int SELECTED = 0xFFFFD24B;
	private static final float RULE_SCALE = 0.75f;

	private final int current;
	private final boolean required;

	/** @param current the side the server has saved (-1 = none); {@code required}: on join with no side yet. */
	public PickSideScreen(int current, boolean required) {
		super(Component.translatable("screen.grandline.side.title"));
		this.current = current;
		this.required = required;
	}

	/**
	 * The menu's scale: as large as fits in ~45% x 40% of the screen, rounded down to half-pixel steps on the real screen
	 * (the pixel font stays sharp, and the menu looks the same whatever the GUI scale); never below 1.5 screen pixels per
	 * unit, where the small text would stop being readable.
	 */
	private float scale() {
		double gui = minecraft != null ? minecraft.getWindow().getGuiScale() : 2;
		double fit = Math.min(width * 0.45 / MENU_W, height * 0.40 / MENU_H);
		double pixels = Math.max(1.5, Math.floor(fit * gui * 2) / 2.0);
		return (float) (pixels / gui);
	}

	private float originX() {
		return width / 2f - MENU_W * scale() / 2f;
	}

	private float originY() {
		return height / 2f - MENU_H * scale() / 2f;
	}

	/** Mouse position in the menu's own (unscaled) units. */
	private double localX(double mouseX) {
		return (mouseX - originX()) / scale();
	}

	private double localY(double mouseY) {
		return (mouseY - originY()) / scale();
	}

	/** Card {@code side} in menu units: x, y, w, h. */
	private static int[] card(int side) {
		return new int[]{side == 0 ? 0 : CARD_W + GAP, TITLE_H, CARD_W, CARD_H};
	}

	private static int[] closeBox() {
		return new int[]{MENU_W - 11, 1, 11, 11};
	}

	private static boolean inside(int[] b, double x, double y) {
		return x >= b[0] && x < b[0] + b[2] && y >= b[1] && y < b[1] + b[3];
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		g.fill(0, 0, width, height, 0x26000000); // ~15% dimming: the world stays clearly visible
		float s = scale();
		double mx = localX(mouseX), my = localY(mouseY);
		g.pose().pushPose();
		g.pose().translate(originX(), originY(), 0);
		g.pose().scale(s, s, 1);

		// Title: small, with a shadow, no backing strip.
		g.pose().pushPose();
		g.pose().translate(MENU_W / 2f - font.width(title) * 0.625f, 2, 0);
		g.pose().scale(1.25f, 1.25f, 1);
		g.drawString(font, title, 0, 0, 0xFFFFFF, true);
		g.pose().popPose();
		if (!required) {
			MarshUi.closeButton(g, closeBox()[0], closeBox()[1], closeBox()[2], inside(closeBox(), mx, my));
		}

		for (int side = 0; side < 2; side++) {
			drawCard(g, side, inside(card(side), mx, my));
		}

		// The current side, just under the cards.
		if (current >= 0) {
			Component info = Component.translatable("screen.grandline.side.current",
					Component.translatable(current == 0 ? "team.grandline.pirates" : "team.grandline.marines")
							.withStyle(st -> st.withColor(ACCENT[current] & 0xFFFFFF)));
			float f = 0.8f;
			float w = font.width(info) * f;
			g.pose().pushPose();
			g.pose().translate(MENU_W / 2f - w / 2f, TITLE_H + CARD_H + 4, 0);
			g.pose().scale(f, f, 1);
			g.drawString(font, info, 0, 0, 0xD7DEE8, true);
			g.pose().popPose();
		}
		g.pose().popPose();
		super.render(g, mouseX, mouseY, partialTick);
	}

	private void drawCard(GuiGraphics g, int side, boolean over) {
		int[] b = card(side);
		int x = b[0], y = b[1], w = b[2], h = b[3];
		int accent = ACCENT[side];
		// Translucent body, a thin accent border (brighter on hover, gold when it is your side).
		g.fill(x, y, x + w, y + h, over ? CARD_HOVER : CARD_BACK);
		int border = current == side ? SELECTED : over ? brighten(accent) : (accent & 0xFFFFFF) | 0xB0000000;
		g.fill(x, y, x + w, y + 1, border);
		g.fill(x, y + h - 1, x + w, y + h, border);
		g.fill(x, y + 1, x + 1, y + h - 1, border);
		g.fill(x + w - 1, y + 1, x + w, y + h - 1, border);
		if (over) {
			g.fill(x + 1, y + 1, x + w - 1, y + 2, 0x60FFFFFF); // a faint top sheen on hover
		}
		// Emblem.
		ItemStack emblem = new ItemStack(side == 0 ? Items.SKELETON_SKULL : Items.SHIELD);
		g.pose().pushPose();
		g.pose().translate(x + w / 2f - 10, y + 6, 0);
		g.pose().scale(1.25f, 1.25f, 1);
		g.renderItem(emblem, 0, 0);
		g.pose().popPose();
		// Name on a slim accent banner.
		Component name = Component.translatable(side == 0 ? "team.grandline.pirates" : "team.grandline.marines");
		g.fill(x + 1, y + 30, x + w - 1, y + 42, BANNER[side]);
		g.drawString(font, name, x + w / 2 - font.width(name) / 2, y + 32, 0xFFFFFF, true);
		// Rules, small and wrapped.
		String base = "screen.grandline.side." + (side == 0 ? "pirates" : "marines") + ".line";
		float ly = y + 47;
		int wrap = (int) ((w - 10) / RULE_SCALE);
		for (int i = 0; i < 3; i++) {
			List<FormattedCharSequence> parts = font.split(Component.translatable(base + (i + 1)), wrap);
			for (FormattedCharSequence part : parts) {
				g.pose().pushPose();
				g.pose().translate(x + w / 2f - font.width(part) * RULE_SCALE / 2f, ly, 0);
				g.pose().scale(RULE_SCALE, RULE_SCALE, 1);
				g.drawString(font, part, 0, 0, 0xE4E9F0, true);
				g.pose().popPose();
				ly += 7;
			}
			ly += 3;
		}
		// "Selected" tag on your current side.
		if (current == side) {
			Component tag = Component.translatable("screen.grandline.side.selected");
			float f = 0.7f;
			int tw = (int) Math.ceil(font.width(tag) * f) + 6;
			int tx = x + w - tw - 3, ty = y + 3;
			g.fill(tx, ty, tx + tw, ty + 9, 0xD0201606);
			g.fill(tx, ty, tx + tw, ty + 1, SELECTED);
			g.pose().pushPose();
			g.pose().translate(tx + 3, ty + 2, 0);
			g.pose().scale(f, f, 1);
			g.drawString(font, tag, 0, 0, SELECTED, false);
			g.pose().popPose();
		}
	}

	private static int brighten(int argb) {
		int r = Math.min(255, ((argb >> 16) & 0xFF) + 60), gr = Math.min(255, ((argb >> 8) & 0xFF) + 60), b = Math.min(255, (argb & 0xFF) + 60);
		return 0xFF000000 | r << 16 | gr << 8 | b;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == 0) {
			double mx = localX(mouseX), my = localY(mouseY);
			if (!required && inside(closeBox(), mx, my)) {
				onClose();
				return true;
			}
			for (int side = 0; side < 2; side++) {
				if (inside(card(side), mx, my)) {
					ClientNetworking.requestChooseTeam(side);
					minecraft.setScreen(null);
					return true;
				}
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return !required;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
