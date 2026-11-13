package com.grandline.client.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Drawing kit for the Marsh UI sprite sheets (assets/marsh/textures/gui, overridable by the Marsh UI resource pack):
 * nine-sliced windows, buttons, rows and bars at any size. Sprite rectangles are measured from the sheets.
 */
public final class MarshUi {
	public static final ResourceLocation BUTTONS = new ResourceLocation("marsh", "textures/gui/buttons.png");
	public static final ResourceLocation WINDOW = new ResourceLocation("marsh", "textures/gui/window.png");
	public static final ResourceLocation ROWS = new ResourceLocation("marsh", "textures/gui/rows.png");
	public static final ResourceLocation BARS = new ResourceLocation("marsh", "textures/gui/hud_bars.png");
	public static final ResourceLocation HUD_BUTTONS = new ResourceLocation("marsh", "textures/gui/hud_buttons.png");
	public static final ResourceLocation ABILITIES = new ResourceLocation("marsh", "textures/gui/hud_abilities.png");
	public static final ResourceLocation ITEM_FRAME = new ResourceLocation("marsh", "textures/gui/item_frame.png");

	/** Button colours, in sheet row order. */
	public enum Color { YELLOW, GRAY, GREEN, RED, BLUE, DARK }

	/** Button states, in sheet column order. */
	public enum State { NORMAL, HOVER, PRESSED }

	/** Stat row tints, in sheet row order. */
	public enum Row { DARK, MELEE, DEFENSE, SWORD, GUN, FRUIT }

	/** Window title bar height in the sheet (scaled with the window). */
	public static final int TITLE_HEIGHT = 24;

	private MarshUi() {
	}

	/** Draws a texture region stretched to (w, h) with a fixed border of {@code b} pixels kept unstretched. */
	public static void nineSlice(GuiGraphics g, ResourceLocation tex, int texW, int texH, int x, int y, int w, int h,
			int u, int v, int uw, int vh, int b) {
		int bx = Math.min(b, w / 2);
		int by = Math.min(b, h / 2);
		int midW = w - 2 * bx;
		int midH = h - 2 * by;
		int srcMidW = uw - 2 * b;
		int srcMidH = vh - 2 * b;
		// corners
		g.blit(tex, x, y, bx, by, u, v, bx, by, texW, texH);
		g.blit(tex, x + w - bx, y, bx, by, u + uw - bx, v, bx, by, texW, texH);
		g.blit(tex, x, y + h - by, bx, by, u, v + vh - by, bx, by, texW, texH);
		g.blit(tex, x + w - bx, y + h - by, bx, by, u + uw - bx, v + vh - by, bx, by, texW, texH);
		// edges and centre (stretched)
		if (midW > 0) {
			g.blit(tex, x + bx, y, midW, by, u + b, v, srcMidW, by, texW, texH);
			g.blit(tex, x + bx, y + h - by, midW, by, u + b, v + vh - by, srcMidW, by, texW, texH);
		}
		if (midH > 0) {
			g.blit(tex, x, y + by, bx, midH, u, v + b, bx, srcMidH, texW, texH);
			g.blit(tex, x + w - bx, y + by, bx, midH, u + uw - bx, v + b, bx, srcMidH, texW, texH);
		}
		if (midW > 0 && midH > 0) {
			g.blit(tex, x + bx, y + by, midW, midH, u + b, v + b, srcMidW, srcMidH, texW, texH);
		}
	}

	/** A window with a yellow title bar and a centred title. Returns the y where content can start. */
	public static int window(GuiGraphics g, Font font, int x, int y, int w, int h, Component title) {
		// Title bar and body are sliced separately so the title bar keeps its height at any window size.
		nineSlice(g, WINDOW, 256, 256, x, y, w, TITLE_HEIGHT, 0, 0, 240, TITLE_HEIGHT, 6);
		nineSlice(g, WINDOW, 256, 256, x, y + TITLE_HEIGHT, w, h - TITLE_HEIGHT, 0, TITLE_HEIGHT, 240, 180 - TITLE_HEIGHT, 6);
		g.pose().pushPose();
		float scale = 1.5f;
		g.pose().translate(x + w / 2f - font.width(title) * scale / 2f, y + 6, 0);
		g.pose().scale(scale, scale, 1);
		g.drawString(font, title, 0, 0, 0xFFFFFF, true);
		g.pose().popPose();
		return y + TITLE_HEIGHT + 4;
	}

	public static void button(GuiGraphics g, int x, int y, int w, int h, Color color, State state) {
		nineSlice(g, BUTTONS, 256, 192, x, y, w, h, state.ordinal() * 64, color.ordinal() * 24, 60, 20, 4);
	}

	/** The red close button (20x20 sprite), hovered or not. */
	public static void closeButton(GuiGraphics g, int x, int y, int size, boolean hover) {
		g.blit(BUTTONS, x, y, size, size, hover ? 24 : 0, 144, 20, 20, 256, 192);
	}

	public static void row(GuiGraphics g, int x, int y, int w, int h, Row row) {
		nineSlice(g, ROWS, 256, 192, x, y, w, h, 0, row.ordinal() * 30, 220, 28, 6);
	}

	/** A HUD bar: frame plus a fill clipped to {@code fraction}. Kind: 0 health (green), 1 energy (cyan). */
	public static void bar(GuiGraphics g, int x, int y, int w, int h, double fraction, int kind) {
		nineSlice(g, BARS, 256, 128, x, y, w, h, 0, 0, 180, 22, 4);
		int fillW = (int) Math.round(w * Math.max(0, Math.min(1, fraction)));
		if (fillW > 0) {
			g.enableScissor(x, y, x + fillW, y + h);
			nineSlice(g, BARS, 256, 128, x, y, w, h, 0, kind == 0 ? 24 : 48, 180, 22, 4);
			g.disableScissor();
		}
	}

	/** Level/XP bar: tick-marked frame plus fill. */
	public static void xpBar(GuiGraphics g, int x, int y, int w, int h, double fraction) {
		nineSlice(g, BARS, 256, 128, x, y, w, h, 0, 72, 180, 16, 3);
		int fillW = (int) Math.round(w * Math.max(0, Math.min(1, fraction)));
		if (fillW > 0) {
			g.enableScissor(x, y, x + fillW, y + h);
			nineSlice(g, BARS, 256, 128, x, y, w, h, 0, 92, 180, 16, 3);
			g.disableScissor();
		}
	}

	/** Big yellow HUD/menu button (84x28 sprite). */
	public static void menuButton(GuiGraphics g, int x, int y, int w, int h, State state) {
		nineSlice(g, HUD_BUTTONS, 128, 96, x, y, w, h, 0, state.ordinal() * 32, 84, 28, 5);
	}

	/** Small blue notification badge (16x16). */
	public static void badge(GuiGraphics g, int x, int y, int size) {
		g.blit(HUD_BUTTONS, x, y, size, size, 96, 0, 16, 16, 128, 96);
	}

	/** Item frame (40x40 gold-bordered slot). */
	public static void itemFrame(GuiGraphics g, int x, int y, int size) {
		nineSlice(g, ITEM_FRAME, 128, 64, x, y, size, size, 0, 0, 40, 40, 5);
	}

	/** Ability slot. Variant: 0 dark, 1 highlighted (gold edge), 2 on cooldown (purple). */
	public static void abilitySlot(GuiGraphics g, int x, int y, int size, int variant) {
		g.blit(ABILITIES, x, y, size, size, 0, variant * 34, 32, 32, 256, 128);
	}

	public static void keyCap(GuiGraphics g, int x, int y, int w, int h) {
		g.blit(ABILITIES, x, y, w, h, 9, 104, 14, 10, 256, 128);
	}

	/** Text with a dark outline (readable on any sprite). */
	public static void outlined(GuiGraphics g, Font font, Component text, int x, int y, int color) {
		for (int dx = -1; dx <= 1; dx++) {
			for (int dy = -1; dy <= 1; dy++) {
				if (dx != 0 || dy != 0) {
					g.drawString(font, text.getString(), x + dx, y + dy, 0xFF1A1206, false);
				}
			}
		}
		g.drawString(font, text, x, y, color, false);
	}
}
