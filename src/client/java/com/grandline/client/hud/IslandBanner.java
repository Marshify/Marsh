package com.grandline.client.hud;

import com.grandline.network.payload.IslandArrivalPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Island arrival banner at the top centre: the island's name in large gold lettering between two thin rules, with
 * its level and sea underneath. Fades in (0.4 s), holds (~3 s), fades out (0.6 s). Sits above the combat banner
 * (y 34) and the quest tracker (y 52) so they never overlap.
 */
public final class IslandBanner {
	private static final long FADE_IN = 400, HOLD = 3000, FADE_OUT = 600;
	private static IslandArrivalPayload current;
	private static long shownAt;

	private IslandBanner() {
	}

	public static void register() {
		ClientPlayNetworking.registerGlobalReceiver(IslandArrivalPayload.TYPE, (packet, player, sender) -> {
			current = packet;
			shownAt = System.currentTimeMillis();
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> current = null);
		HudRenderCallback.EVENT.register((graphics, delta) -> render(graphics));
	}

	/** 0..1 opacity right now (0 when no banner is showing). */
	static float alpha() {
		if (current == null) {
			return 0;
		}
		long t = System.currentTimeMillis() - shownAt;
		if (t < FADE_IN) {
			float k = t / (float) FADE_IN;
			return k * k * (3 - 2 * k);
		}
		if (t < FADE_IN + HOLD) {
			return 1;
		}
		float k = 1 - (t - FADE_IN - HOLD) / (float) FADE_OUT;
		if (k <= 0) {
			current = null;
			return 0;
		}
		return k * k * (3 - 2 * k);
	}

	private static int argb(int rgb, float a) {
		return (Math.max(5, Math.round(a * 255)) << 24) | rgb;
	}

	private static void render(GuiGraphics g) {
		Minecraft mc = Minecraft.getInstance();
		float a = alpha();
		if (a <= 0.02f || mc.options.hideGui || mc.player == null) {
			return;
		}
		Font font = mc.font;
		int cx = g.guiWidth() / 2;
		float scale = 1.8f;
		Component name = Component.literal(current.name());
		int nameW = Math.round(font.width(name) * scale);
		int top = 7;
		// Slight drop-in as it fades in.
		long t = System.currentTimeMillis() - shownAt;
		float slide = t < FADE_IN ? (1 - t / (float) FADE_IN) * -4 : 0;
		g.pose().pushPose();
		g.pose().translate(0, slide, 0);
		// Soft dark band behind the text for legibility on any sky.
		int bandW = nameW + 60;
		g.fillGradient(cx - bandW / 2, top - 3, cx + bandW / 2, top + 30, argb(0x000000, a * 0.45f), argb(0x000000, a * 0.05f));
		// Name: outlined gold.
		g.pose().pushPose();
		g.pose().translate(cx - nameW / 2f, top, 0);
		g.pose().scale(scale, scale, 1);
		int outline = argb(0x2A1A05, a);
		for (int[] o : new int[][]{{-1, 0}, {1, 0}, {0, -1}, {0, 1}}) {
			g.drawString(font, name, o[0], o[1], outline, false);
		}
		g.drawString(font, name, 0, 0, argb(0xFFD866, a), false);
		g.pose().popPose();
		// Rules either side, and the subtitle.
		int ruleY = top + 21;
		int ruleHalf = Math.max(30, nameW / 2 + 8);
		g.fill(cx - ruleHalf, ruleY, cx - 26, ruleY + 1, argb(0xE8C25A, a * 0.9f));
		g.fill(cx + 26, ruleY, cx + ruleHalf, ruleY + 1, argb(0xE8C25A, a * 0.9f));
		Component sub = Component.translatable("hud.grandline.island_arrival", current.level(),
				Component.translatable("screen.grandline.map.sea_name." + current.sea()));
		g.pose().pushPose();
		float sub0 = 0.75f;
		g.pose().translate(cx - font.width(sub) * sub0 / 2f, ruleY - 2.5f, 0);
		g.pose().scale(sub0, sub0, 1);
		g.drawString(font, sub, 0, 0, argb(0xF4E6C2, a), true);
		g.pose().popPose();
		g.pose().popPose();
	}
}
