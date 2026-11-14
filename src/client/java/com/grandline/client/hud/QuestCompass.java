package com.grandline.client.hud;

import com.grandline.client.ClientNavigation;
import com.grandline.network.payload.NavTargetPayload;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Quest compass (top right): a navy dial with a gold rim and a gold arrow that turns toward the quest giver as the
 * player moves and looks around, a rotating north tick, and underneath the target's name, island and distance.
 * Hidden when navigation is off. [N] toggles it.
 */
public final class QuestCompass {
	private static float shownAngle;

	private QuestCompass() {
	}

	public static void register() {
		HudRenderCallback.EVENT.register((graphics, delta) -> render(graphics, delta));
	}

	private static void render(GuiGraphics g, float partial) {
		Minecraft mc = Minecraft.getInstance();
		NavTargetPayload t = ClientNavigation.target();
		if (mc.player == null || mc.options.hideGui || t == null || !t.enabled() || t.island().isEmpty() || mc.screen != null) {
			return;
		}
		Font font = mc.font;
		int r = 20;
		int cx = g.guiWidth() - r - 10, cy = r + 10;
		Vec3 eye = mc.player.getEyePosition(partial);
		double dx = t.x() + 0.5 - eye.x, dz = t.z() + 0.5 - eye.z;
		double dist = Math.sqrt(dx * dx + dz * dz + Math.pow(t.y() - eye.y, 2));
		float bearing = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90f; // Minecraft yaw facing the target
		float rel = Mth.wrapDegrees(bearing - mc.player.getViewYRot(partial));
		shownAngle += Mth.wrapDegrees(rel - shownAngle) * 0.35f; // a little needle damping
		// Dial: rim, face, inner ring.
		disc(g, cx, cy, r + 2, 0xFFE8C25A);
		disc(g, cx, cy, r, 0xF2162034);
		ring(g, cx, cy, r - 3, 0x55E8C25A);
		// North tick, turning with the player's view.
		float north = (float) Math.toRadians(-180 - mc.player.getViewYRot(partial));
		int nx = cx + Math.round(Mth.sin(north) * (r - 4)), ny = cy - Math.round(Mth.cos(north) * (r - 4));
		g.drawCenteredString(font, "N", nx, ny - 4, 0xFFA8B4C8);
		// Needle toward the target.
		g.pose().pushPose();
		g.pose().translate(cx, cy, 0);
		g.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(shownAngle));
		for (int i = 0; i < r - 5; i++) {
			int half = Math.max(0, (int) ((r - 5 - i) * 0.28f));
			g.fill(-half, -i - 1, half + 1, -i, 0xFFFFD866);
		}
		g.fill(-1, 0, 2, r - 9, 0xFF6B7A94);
		g.pose().popPose();
		disc(g, cx, cy, 2, 0xFFFFFFFF);
		// Labels.
		Component name = Component.translatable(t.name());
		String meters = dist < 6 ? Component.translatable("hud.grandline.nav.here").getString() : Math.round(dist) + " m";
		int ly = cy + r + 6;
		right(g, font, name, cx + r, ly, 0xFFD866);
		right(g, font, Component.literal(t.island()), cx + r, ly + 10, 0xFFFFFF);
		right(g, font, Component.literal(meters + "   [N]"), cx + r, ly + 20, 0xA8B4C8);
	}

	private static void right(GuiGraphics g, Font font, Component text, int rightX, int y, int color) {
		g.drawString(font, text, rightX - font.width(text), y, color, true);
	}

	private static void disc(GuiGraphics g, int cx, int cy, int r, int color) {
		for (int y = -r; y <= r; y++) {
			int w = (int) Math.round(Math.sqrt(r * r - y * y + 0.25));
			g.fill(cx - w, cy + y, cx + w + 1, cy + y + 1, color);
		}
	}

	private static void ring(GuiGraphics g, int cx, int cy, int r, int color) {
		for (int a = 0; a < 360; a += 4) {
			double rad = Math.toRadians(a);
			int x = cx + (int) Math.round(Math.cos(rad) * r), y = cy + (int) Math.round(Math.sin(rad) * r);
			g.fill(x, y, x + 1, y + 1, color);
		}
	}
}
