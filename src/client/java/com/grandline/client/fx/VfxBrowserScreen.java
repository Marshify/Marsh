package com.grandline.client.fx;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Developer VFX browser ({@code /glvfx}): browse the library by category, preview, replay or loop an effect in front
 * of you (or attached to you), change its scale and colour, switch the quality preset, force the shader fallback path
 * for comparison, and watch live diagnostics. The left panel leaves the world visible; the game keeps running.
 */
public final class VfxBrowserScreen extends Screen {
	private static final int PANEL = 230, ROW = 12;
	private final List<String> categories = new ArrayList<>();
	private String category;
	private int scroll;
	private List<VfxLibrary.Definition> shown = List.of();
	private int listTop = 40;

	public VfxBrowserScreen() {
		super(Component.literal("VFX Browser"));
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	protected void init() {
		Map<String, List<VfxLibrary.Definition>> all = VfxLibrary.byCategory();
		categories.clear();
		categories.addAll(all.keySet());
		if (category == null || !all.containsKey(category)) {
			category = categories.isEmpty() ? null : categories.get(0);
		}
		shown = category == null ? List.of() : all.get(category);
		int y = 4;
		int x = 4;
		for (String c : categories) {
			int w = font.width(c) + 8;
			if (x + w > PANEL) {
				x = 4;
				y += 16;
			}
			addRenderableWidget(Button.builder(Component.literal(c.equals(category) ? "§e" + c : c), b -> {
				category = c;
				scroll = 0;
				rebuildWidgets();
			}).bounds(x, y, w, 14).build());
			x += w + 2;
		}
		listTop = y + 20;
		int by = height - 76;
		addRenderableWidget(Button.builder(Component.literal("Play / Replay"), b -> VfxPreview.play()).bounds(4, by, 110, 16).build());
		addRenderableWidget(Button.builder(Component.literal("Loop: " + (VfxPreview.loop ? "on" : "off")), b -> {
			VfxPreview.loop = !VfxPreview.loop;
			rebuildWidgets();
		}).bounds(118, by, 108, 16).build());
		addRenderableWidget(Button.builder(Component.literal("Scale -"), b -> VfxPreview.scale = Math.max(0.25f, VfxPreview.scale - 0.25f))
				.bounds(4, by + 18, 54, 16).build());
		addRenderableWidget(Button.builder(Component.literal("Scale +"), b -> VfxPreview.scale = Math.min(5, VfxPreview.scale + 0.25f))
				.bounds(60, by + 18, 54, 16).build());
		addRenderableWidget(Button.builder(Component.literal("Colour: " + VfxPreview.COLOUR_NAMES[VfxPreview.colour]), b -> {
			VfxPreview.colour = (VfxPreview.colour + 1) % VfxPreview.COLOURS.length;
			rebuildWidgets();
		}).bounds(118, by + 18, 108, 16).build());
		addRenderableWidget(Button.builder(Component.literal("Quality: " + VfxPreview.quality()), b -> {
			VfxPreview.cycleQuality();
			rebuildWidgets();
		}).bounds(4, by + 36, 110, 16).build());
		addRenderableWidget(Button.builder(Component.literal(WindRender.forceFallback ? "Fallback: forced" : "Fallback: auto"), b -> {
			WindRender.forceFallback = !WindRender.forceFallback;
			rebuildWidgets();
		}).bounds(118, by + 36, 108, 16).build());
		addRenderableWidget(Button.builder(Component.literal(VfxPreview.anchored ? "On player" : "Ahead " + (int) VfxPreview.distance + "m"), b -> {
			if (VfxPreview.anchored) {
				VfxPreview.anchored = false;
				VfxPreview.distance = VfxPreview.distance >= 10 ? 3 : VfxPreview.distance + 3.5;
				if (VfxPreview.distance >= 10) {
					VfxPreview.distance = 10;
				}
			} else if (VfxPreview.distance >= 10) {
				VfxPreview.anchored = true;
			} else {
				VfxPreview.distance = VfxPreview.distance + 3.5 > 10 ? 10 : VfxPreview.distance + 3.5;
			}
			rebuildWidgets();
		}).bounds(4, by + 54, 110, 16).build());
		addRenderableWidget(Button.builder(Component.literal("Close"), b -> onClose()).bounds(118, by + 54, 108, 16).build());
	}

	int listTop() {
		return listTop;
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
		g.fill(0, 0, PANEL + 6, height, 0xB0101418);
		int top = listTop(), bottom = height - 96;
		int rows = Math.max(1, (bottom - top) / ROW);
		for (int i = 0; i < rows && i + scroll < shown.size(); i++) {
			VfxLibrary.Definition d = shown.get(i + scroll);
			int y = top + i * ROW;
			boolean sel = d.id().toString().equals(VfxPreview.selected);
			boolean hover = mouseX < PANEL && mouseY >= y && mouseY < y + ROW;
			if (sel || hover) {
				g.fill(2, y - 1, PANEL + 2, y + ROW - 1, sel ? 0x60FFD060 : 0x30FFFFFF);
			}
			g.drawString(font, d.id().getPath(), 6, y + 1, sel ? 0xFFFFE080 : 0xFFE0E0E0);
		}
		VfxLibrary.Definition sel = VfxPreview.selected == null ? null : VfxLibrary.get(VfxPreview.selected);
		int iy = height - 94;
		if (sel != null) {
			g.drawString(font, sel.id() + "  (" + sel.layers().size() + " layers, " + sel.length() + " ticks)", 4, iy, 0xFFFFD060);
			g.drawString(font, font.plainSubstrByWidth(sel.description(), PANEL), 4, iy + 9, 0xFFB0B0B0);
		}
		g.drawString(font, String.format("scale %.2f", VfxPreview.scale), 4, height - 12, 0xFF909090);
		int ey = 6;
		for (String part : VfxPreview.stats().split(" \\| ")) { // one stat per line so it fits at any GUI scale
			g.drawString(font, part, PANEL + 12, ey, 0xFFFFFFFF);
			ey += 10;
		}
		ey += 4;
		for (String err : VfxLibrary.errors()) {
			g.drawString(font, font.plainSubstrByWidth(err, width - PANEL - 20), PANEL + 12, ey, 0xFFFF7060);
			ey += 10;
			if (ey > height / 2) {
				break;
			}
		}
		super.render(g, mouseX, mouseY, partial);
	}

	@Override
	public boolean mouseClicked(double mx, double my, int button) {
		int top = listTop(), bottom = height - 96;
		if (mx < PANEL && my >= top && my < bottom) {
			int i = (int) ((my - top) / ROW) + scroll;
			if (i >= 0 && i < shown.size()) {
				VfxPreview.selected = shown.get(i).id().toString();
				VfxPreview.play();
				return true;
			}
		}
		return super.mouseClicked(mx, my, button);
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double delta) {
		scroll = Math.max(0, Math.min(Math.max(0, shown.size() - 5), scroll - (int) Math.signum(delta)));
		return true;
	}
}
