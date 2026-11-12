package com.grandline.client.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** Shared look for every Grand Line screen: navy panels, gold title bar and trim. */
public final class GrandLineUi {
	public static final int NAVY = 0xF0101C2E;
	public static final int NAVY_LIGHT = 0xF0233A57;
	public static final int NAVY_DARK = 0xF0091220;
	public static final int GOLD = 0xFFE8B84A;
	public static final int GOLD_DARK = 0xFFB0822A;
	public static final int BORDER = 0xFF05090F;

	private GrandLineUi() {
	}

	/** A panel with a gold title bar. Returns the y coordinate where content starts. */
	public static int panel(GuiGraphics graphics, Font font, int x, int y, int width, int height, Component title) {
		graphics.fill(x - 2, y - 2, x + width + 2, y + height + 2, BORDER);
		graphics.fill(x, y, x + width, y + height, NAVY);
		graphics.fill(x, y, x + width, y + 20, GOLD);
		graphics.fill(x, y + 17, x + width, y + 20, GOLD_DARK);
		scaled(graphics, font, title, x + width / 2f - font.width(title) * 0.65f, y + 4, 1.3f, 0x1A1205, false);
		graphics.fill(x, y + 20, x + width, y + 21, BORDER);
		return y + 26;
	}

	/** A coloured tile (rarity frames etc.) with a darker inner border. */
	public static void tile(GuiGraphics graphics, int x, int y, int width, int height, int color, boolean highlighted) {
		int frame = 0xFF000000 | color;
		graphics.fill(x, y, x + width, y + height, highlighted ? 0xFFFFFFFF : frame);
		graphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, darken(frame, 0.45f));
		graphics.fill(x + 2, y + 2, x + width - 2, y + 4, darken(frame, 0.75f));
	}

	public static int darken(int argb, float factor) {
		int a = argb >>> 24;
		int r = (int) (((argb >> 16) & 0xFF) * factor);
		int g = (int) (((argb >> 8) & 0xFF) * factor);
		int b = (int) ((argb & 0xFF) * factor);
		return (a << 24) | (r << 16) | (g << 8) | b;
	}

	public static void scaled(GuiGraphics graphics, Font font, Component text, float x, float y, float scale, int color, boolean shadow) {
		graphics.pose().pushPose();
		graphics.pose().translate(x, y, 0);
		graphics.pose().scale(scale, scale, 1);
		graphics.drawString(font, text, 0, 0, color, shadow);
		graphics.pose().popPose();
	}
}
