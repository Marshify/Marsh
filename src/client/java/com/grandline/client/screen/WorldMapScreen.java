package com.grandline.client.screen;

import com.grandline.GrandLineMod;
import com.grandline.client.ClientProfileCache;
import com.grandline.world.Island;
import com.grandline.world.SeaLayout;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Illustrated sea chart of the sea the player is in: framed parchment border, glowing ocean, themed island art with
 * names and levels, drifting clouds, a compass rose, the player marker with view cone and a "Recommended" tag on the
 * best island for the player's level. The whole sea is fitted to the screen.
 */
public final class WorldMapScreen extends Screen {
	private static final int ICON_W = 128;
	private static final int ICON_H = 96;
	private static final ResourceLocation CLOUD = GrandLineMod.id("textures/gui/map/cloud.png");

	public WorldMapScreen() {
		super(Component.translatable("screen.grandline.map.title"));
	}

	private static ResourceLocation icon(Island island) {
		return GrandLineMod.id("textures/gui/map/" + island.theme() + ".png");
	}

	private int currentSea() {
		return minecraft.player == null ? 1 : SeaLayout.seaAt(minecraft.player.getX());
	}

	private List<Island> islandsInSea() {
		int sea = currentSea();
		List<Island> list = new ArrayList<>();
		boolean anyDesigned = ClientProfileCache.islands().stream().anyMatch(Island::designed);
		for (Island island : ClientProfileCache.islands()) {
			if (anyDesigned ? island.designed() && island.sea() == sea : island.sea() == sea) {
				list.add(island);
			}
		}
		return list;
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		renderBackground(graphics);
		int left = 16;
		int top = 12;
		int right = width - 16;
		int bottom = height - 12;

		// Wooden frame, parchment trim, ocean.
		graphics.fill(left - 4, top - 4, right + 4, bottom + 4, 0xFF5A3A1E);
		graphics.fill(left - 2, top - 2, right + 2, bottom + 2, 0xFF8A5E34);
		graphics.fill(left, top, right, bottom, 0xFFE2CE9C);
		int ol = left + 6;
		int ot = top + 6;
		int or = right - 6;
		int ob = bottom - 6;
		graphics.fillGradient(ol, ot, or, (ot + ob) / 2, 0xFF2A7FCF, 0xFF4BA6EA);
		graphics.fillGradient(ol, (ot + ob) / 2, or, ob, 0xFF4BA6EA, 0xFF2A7FCF);
		for (int x = ol; x < or; x += 32) {
			graphics.fill(x, ot, x + 1, ob, 0x18FFFFFF);
		}
		for (int y = ot; y < ob; y += 32) {
			graphics.fill(ol, y, or, y + 1, 0x18FFFFFF);
		}
		for (int x = ol + 4; x < or - 8; x += 14) {
			graphics.fill(x, ot + 2, x + 7, ot + 3, 0xC0FFFFFF);
			graphics.fill(x, ob - 3, x + 7, ob - 2, 0xC0FFFFFF);
		}

		List<Island> islands = islandsInSea();
		if (islands.isEmpty()) {
			graphics.drawCenteredString(font, Component.translatable("screen.grandline.map.empty"), width / 2, height / 2, 0xFFFFFF);
			super.render(graphics, mouseX, mouseY, partialTick);
			return;
		}

		// Fit the sea's islands into the chart.
		double minX = Double.MAX_VALUE;
		double maxX = -Double.MAX_VALUE;
		double minZ = Double.MAX_VALUE;
		double maxZ = -Double.MAX_VALUE;
		for (Island island : islands) {
			minX = Math.min(minX, island.x());
			maxX = Math.max(maxX, island.x());
			minZ = Math.min(minZ, island.z());
			maxZ = Math.max(maxZ, island.z());
		}
		int padX = 50;
		int padTop = 40;
		int padBottom = 46;
		double spanX = Math.max(1, maxX - minX);
		double spanZ = Math.max(1, maxZ - minZ);
		double scale = Math.min((or - ol - padX * 2) / spanX, (ob - ot - padTop - padBottom) / spanZ);
		double originX = (ol + or) / 2.0 - (minX + maxX) / 2.0 * scale;
		double originZ = (ot + padTop + ob - padBottom) / 2.0 - (minZ + maxZ) / 2.0 * scale;

		int playerLevel = ClientProfileCache.profile() == null ? 1 : ClientProfileCache.profile().level();
		Island recommended = null;
		// Recommended: the highest-level non-starter island the player can handle (a little above their level).
		for (Island island : islands) {
			if (!island.starter() && island.level() <= playerLevel + 15
					&& (recommended == null || island.level() > recommended.level())) {
				recommended = island;
			}
		}

		// Size icons from the closest pair of islands so they never overlap.
		double closest = Double.MAX_VALUE;
		for (int i = 0; i < islands.size(); i++) {
			for (int j = i + 1; j < islands.size(); j++) {
				double dx = (islands.get(i).x() - islands.get(j).x()) * scale;
				double dz = (islands.get(i).z() - islands.get(j).z()) * scale;
				closest = Math.min(closest, Math.sqrt(dx * dx + dz * dz));
			}
		}
		float iconScale = (float) Mth.clamp(Math.min(closest * 1.45 / ICON_W, height / 330.0), 0.25, 1.1);
		float labelScale = Mth.clamp(iconScale * 1.3f, 0.5f, 1.0f);
		com.mojang.blaze3d.systems.RenderSystem.enableBlend();
		com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
		for (Island island : islands) {
			int sx = (int) Math.round(originX + island.x() * scale);
			int sy = (int) Math.round(originZ + island.z() * scale);
			graphics.pose().pushPose();
			graphics.pose().translate(sx - ICON_W * iconScale / 2, sy - ICON_H * iconScale * 0.62, 0);
			graphics.pose().scale(iconScale, iconScale, 1);
			graphics.blit(icon(island), 0, 0, 0, 0, ICON_W, ICON_H, ICON_W, ICON_H);
			graphics.pose().popPose();

		}
		// Labels after all icons so no icon covers a label.
		for (Island island : islands) {
			int sx = (int) Math.round(originX + island.x() * scale);
			int sy = (int) Math.round(originZ + island.z() * scale);
			Component name = Component.literal(island.name());
			Component lvl = Component.translatable("screen.grandline.map.level", island.level());
			int color = island.level() <= playerLevel ? 0x9CFF9C : island.level() <= playerLevel + 50 ? 0xFFE080 : 0xFF9C9C;
			graphics.pose().pushPose();
			graphics.pose().translate(sx, sy + ICON_H * iconScale * 0.3, 0);
			graphics.pose().scale(labelScale, labelScale, 1);
			int w = Math.max(font.width(name), font.width(lvl)) + 8;
			graphics.fill(-w / 2, 0, w / 2, 21, 0xC0101C2E);
			graphics.drawCenteredString(font, name, 0, 2, 0xFFFFFF);
			graphics.drawCenteredString(font, lvl, 0, 11, color);
			graphics.pose().popPose();
		}

		// Clouds drifting slowly across the chart.
		long time = System.currentTimeMillis() / 60;
		// A few soft clouds along the edges only.
		int[][] clouds = {{0, 0}, {400, 1}, {200, 2}, {650, 3}};
		graphics.enableScissor(ol, ot, or, ob);
		graphics.setColor(1, 1, 1, 0.8f);
		for (int[] c : clouds) {
			int span = or - ol + 128;
			int cx = ol - 128 + (int) ((c[0] + time / 4) % span);
			int cy = c[1] % 2 == 0 ? ot - 22 : ob - 40;
			graphics.pose().pushPose();
			graphics.pose().translate(cx, cy, 0);
			graphics.pose().scale(0.8f, 0.8f, 1);
			graphics.blit(CLOUD, 0, 0, 0, 0, 128, 64, 128, 64);
			graphics.pose().popPose();
		}
		graphics.setColor(1, 1, 1, 1);
		graphics.disableScissor();

		if (recommended != null) {
			// "?" badge and tag to the upper right of the recommended island's art.
			int rx = (int) Math.round(originX + recommended.x() * scale + ICON_W * iconScale * 0.42);
			int ry = (int) Math.round(originZ + recommended.z() * scale - ICON_H * iconScale * 0.55);
			graphics.pose().pushPose();
			graphics.pose().translate(rx, ry, 0);
			graphics.pose().scale(labelScale, labelScale, 1);
			Component text = Component.translatable("screen.grandline.map.recommended");
			int w = font.width(text) + 8;
			graphics.fill(-7, -16, 7, -2, 0xFFFFD24B);
			graphics.fill(-6, -15, 6, -3, 0xFF1A1A1A);
			graphics.drawCenteredString(font, "?", 0, -13, 0xFFFFD24B);
			graphics.fill(-w / 2, 0, w / 2, 11, 0xE0141414);
			graphics.drawCenteredString(font, text, 0, 2, 0xFFFFFF);
			graphics.pose().popPose();
		}

		// Player marker with heading cone.
		if (minecraft.player != null) {
			int px = (int) Math.round(originX + minecraft.player.getX() * scale);
			int py = (int) Math.round(originZ + minecraft.player.getZ() * scale);
			float yaw = minecraft.player.getYRot() * Mth.DEG_TO_RAD;
			for (int i = 4; i < 22; i++) {
				int spread = i / 3;
				int ax = px + Math.round(-Mth.sin(yaw) * i);
				int ay = py + Math.round(Mth.cos(yaw) * i);
				graphics.fill(ax - spread, ay - spread, ax + spread + 1, ay + spread + 1, 0x60FFE13C);
			}
			graphics.fill(px - 5, py - 5, px + 6, py + 6, 0xFFFFFFFF);
			graphics.fill(px - 4, py - 4, px + 5, py + 5, 0xFF1A1A1A);
		}

		// Compass rose.
		int cx = or - 34;
		int cy = ot + 34;
		for (int i = -16; i <= 16; i++) {
			graphics.fill(cx + i, cy, cx + i + 1, cy + 1, 0xC0FFFFFF);
			graphics.fill(cx, cy + i, cx + 1, cy + i + 1, 0xC0FFFFFF);
		}
		graphics.drawCenteredString(font, "N", cx + 1, cy - 28, 0xFFFFFF);

		graphics.drawString(font, Component.translatable("screen.grandline.map.sea_title", currentSea()).getString().toUpperCase(Locale.ROOT),
				ol + 8, ot + 8, 0xFFFFFF);
		super.render(graphics, mouseX, mouseY, partialTick);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
