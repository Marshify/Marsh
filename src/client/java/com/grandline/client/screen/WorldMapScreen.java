package com.grandline.client.screen;

import com.grandline.client.ClientProfileCache;
import com.grandline.client.ui.GrandLineUi;
import com.grandline.world.Island;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * Sea chart of every discovered island: names, recommended level, sea-tier rings around spawn and the player's
 * position and heading. Scroll to zoom, drag to pan.
 */
public final class WorldMapScreen extends Screen {
	/** Distance from spawn where Sea 2 and Sea 3 begin (level 700 and 1500 at 0.5 levels per block). */
	private static final int[] SEA_RINGS = {1398, 2998};

	private double blocksPerPixel = 2.5;
	private double centerX;
	private double centerZ;
	private boolean centered;

	public WorldMapScreen() {
		super(Component.translatable("screen.grandline.map.title"));
	}

	@Override
	protected void init() {
		if (!centered && minecraft.player != null) {
			centerX = minecraft.player.getX();
			centerZ = minecraft.player.getZ();
			centered = true;
		}
	}

	private int left() {
		return 20;
	}

	private int top() {
		return 20;
	}

	private int mapWidth() {
		return width - 40;
	}

	private int mapHeight() {
		return height - 40;
	}

	private int toScreenX(double worldX) {
		return (int) Math.round(left() + mapWidth() / 2.0 + (worldX - centerX) / blocksPerPixel);
	}

	private int toScreenY(double worldZ) {
		return (int) Math.round(top() + 26 + (mapHeight() - 26) / 2.0 + (worldZ - centerZ) / blocksPerPixel);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		renderBackground(graphics);
		int contentTop = GrandLineUi.panel(graphics, font, left(), top(), mapWidth(), mapHeight(), title);
		int x0 = left() + 4;
		int x1 = left() + mapWidth() - 4;
		int y0 = contentTop;
		int y1 = top() + mapHeight() - 4;
		graphics.fill(x0, y0, x1, y1, 0xFF13406B);
		graphics.enableScissor(x0, y0, x1, y1);

		// Sea tier rings around spawn.
		for (int i = 0; i < SEA_RINGS.length; i++) {
			ring(graphics, toScreenX(0), toScreenY(0), SEA_RINGS[i] / blocksPerPixel, i == 0 ? 0x806BC8FF : 0x80FF8A6B);
			graphics.drawString(font, Component.translatable("screen.grandline.map.sea", i + 2), toScreenX(0) + 4,
					toScreenY(-SEA_RINGS[i]) + 2, i == 0 ? 0x9ADCFF : 0xFFB09A);
		}

		for (Island island : ClientProfileCache.islands()) {
			int sx = toScreenX(island.x());
			int sy = toScreenY(island.z());
			int r = Math.max(3, (int) Math.round(island.radius() / blocksPerPixel));
			disc(graphics, sx, sy, r + 1, 0xFFE8D29A);
			disc(graphics, sx, sy, r, island.starter() ? 0xFF6FCF5A : 0xFF4FA845);
			boolean hovered = (mouseX - sx) * (mouseX - sx) + (mouseY - sy) * (mouseY - sy) <= (r + 4) * (r + 4);
			// Labels only when zoomed in (or hovered) so a zoomed-out chart stays readable.
			if (blocksPerPixel <= 3.0 || hovered) {
				Component name = Component.literal(island.name());
				graphics.drawCenteredString(font, name, sx, sy - r - 20, 0xFFFFFF);
				graphics.drawCenteredString(font, Component.translatable("screen.grandline.map.level", island.level()), sx, sy - r - 10,
						island.level() <= (ClientProfileCache.profile() == null ? 1 : ClientProfileCache.profile().level()) ? 0x9CFF9C : 0xFF9C9C);
			}
		}

		if (minecraft.player != null) {
			int px = toScreenX(minecraft.player.getX());
			int py = toScreenY(minecraft.player.getZ());
			float yaw = minecraft.player.getYRot() * Mth.DEG_TO_RAD;
			// Arrow: a dot plus a short heading line.
			for (int i = 0; i < 9; i++) {
				int ax = px + Math.round(-Mth.sin(yaw) * i);
				int ay = py + Math.round(Mth.cos(yaw) * i);
				graphics.fill(ax - 1, ay - 1, ax + 1, ay + 1, 0xFFFF4040);
			}
			disc(graphics, px, py, 3, 0xFFFFFFFF);
			disc(graphics, px, py, 2, 0xFFFF4040);
		}
		graphics.disableScissor();

		graphics.drawString(font, Component.translatable("screen.grandline.map.help"), x0 + 4, y1 - 12, 0xC9D3E0);
		if (ClientProfileCache.islands().isEmpty()) {
			graphics.drawCenteredString(font, Component.translatable("screen.grandline.map.empty"), width / 2, height / 2, 0xFFFFFF);
		}
		super.render(graphics, mouseX, mouseY, partialTick);
	}

	private static void disc(GuiGraphics graphics, int cx, int cy, int r, int color) {
		for (int dy = -r; dy <= r; dy++) {
			int half = (int) Math.sqrt(r * r - dy * dy);
			graphics.fill(cx - half, cy + dy, cx + half + 1, cy + dy + 1, color);
		}
	}

	private static void ring(GuiGraphics graphics, int cx, int cy, double r, int color) {
		int steps = (int) Math.max(32, Math.min(720, r * 2));
		for (int i = 0; i < steps; i++) {
			double a = i * Math.PI * 2 / steps;
			int x = cx + (int) Math.round(Math.cos(a) * r);
			int y = cy + (int) Math.round(Math.sin(a) * r);
			graphics.fill(x, y, x + 1, y + 1, color);
		}
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
		blocksPerPixel = Mth.clamp(blocksPerPixel * (delta > 0 ? 0.8 : 1.25), 0.5, 80);
		return true;
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		centerX -= dragX * blocksPerPixel;
		centerZ -= dragY * blocksPerPixel;
		return true;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
