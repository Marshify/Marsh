package com.grandline.client.hud;

import com.grandline.network.payload.QuestUnlockPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

/**
 * "New quest available" card: a navy Grand Line panel with a gold frame and accent bar, dropping in from above the
 * screen centre with a small bounce, holding ~4.5 s, then fading out. Shows the island and its required level.
 */
public final class QuestUnlockBanner {
	private static final long IN = 450, HOLD = 4500, OUT = 700;
	private static QuestUnlockPayload current;
	private static long shownAt;

	private QuestUnlockBanner() {
	}

	public static void register() {
		ClientPlayNetworking.registerGlobalReceiver(QuestUnlockPayload.TYPE, (packet, player, sender) -> {
			current = packet;
			shownAt = System.currentTimeMillis();
			Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0F, 0.8F));
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> current = null);
		HudRenderCallback.EVENT.register((graphics, delta) -> render(graphics));
	}

	private static int argb(int rgb, float a) {
		return (Math.max(4, Math.round(Math.min(1, a) * 255)) << 24) | rgb;
	}

	private static void render(GuiGraphics g) {
		Minecraft mc = Minecraft.getInstance();
		if (current == null || mc.options.hideGui || mc.player == null) {
			return;
		}
		long t = System.currentTimeMillis() - shownAt;
		float a;
		float drop;
		if (t < IN) {
			float k = t / (float) IN;
			a = k;
			// Ease-out-back: overshoots slightly, then settles.
			float c = 1.6f, x = k - 1;
			drop = (1 + (c + 1) * x * x * x + c * x * x) * 1f;
		} else if (t < IN + HOLD) {
			a = 1;
			drop = 1;
		} else {
			a = 1 - (t - IN - HOLD) / (float) OUT;
			drop = 1;
			if (a <= 0) {
				current = null;
				return;
			}
		}
		Font font = mc.font;
		Component title = Component.translatable("hud.grandline.quest_unlock.title");
		Component where = Component.translatable("hud.grandline.quest_unlock.where", current.name());
		Component level = Component.translatable("hud.grandline.quest_unlock.level", current.level(),
				Component.translatable("screen.grandline.map.sea_name." + current.sea()));
		float titleScale = 1.4f;
		int w = Math.max(Math.round(font.width(title) * titleScale), Math.max(font.width(where), font.width(level))) + 36;
		int h = 50;
		int cx = g.guiWidth() / 2;
		int y = Math.round(g.guiHeight() * 0.2f - 30 + 30 * drop);
		int x = cx - w / 2;
		g.pose().pushPose();
		g.pose().translate(0, 0, 400);
		// Shadow, gold frame, navy body, accent bar.
		g.fill(x + 2, y + 2, x + w + 2, y + h + 2, argb(0x000000, a * 0.35f));
		g.fill(x - 1, y - 1, x + w + 1, y + h + 1, argb(0xE8C25A, a));
		g.fillGradient(x, y, x + w, y + h, argb(0x22304A, a * 0.97f), argb(0x162034, a * 0.97f));
		g.fill(x, y, x + 3, y + h, argb(0xF2C04A, a));
		// Title.
		g.pose().pushPose();
		g.pose().translate(cx - font.width(title) * titleScale / 2f, y + 7, 0);
		g.pose().scale(titleScale, titleScale, 1);
		g.drawString(font, title, 0, 0, argb(0xFFD866, a), true);
		g.pose().popPose();
		g.fill(cx - w / 2 + 14, y + 21, cx + w / 2 - 14, y + 22, argb(0xE8C25A, a * 0.5f));
		g.drawString(font, where, cx - font.width(where) / 2, y + 26, argb(0xFFFFFF, a), true);
		g.drawString(font, level, cx - font.width(level) / 2, y + 37, argb(0xF4E6C2, a * 0.9f), true);
		g.pose().popPose();
	}
}
