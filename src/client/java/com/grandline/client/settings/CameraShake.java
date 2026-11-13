package com.grandline.client.settings;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

/** A short, decaying camera shake (move impacts). Applied to the view only, never to the player's aim. */
public final class CameraShake {
	private static float strength;
	private static long startNanos;
	private static final float DURATION = 0.35f;

	private CameraShake() {
	}

	/** Shake strength in degrees, scaled down with distance; ignored if disabled in Settings. */
	public static void impact(double distance, float maxDegrees) {
		if (!ClientSettings.get().cameraShake || distance > 16) {
			return;
		}
		strength = Math.max(strength * remaining(), maxDegrees * (float) (1 - distance / 16));
		startNanos = System.nanoTime();
	}

	private static float remaining() {
		float t = (System.nanoTime() - startNanos) / 1.0e9f;
		return t >= DURATION ? 0 : 1 - t / DURATION;
	}

	public static void apply(PoseStack pose) {
		float r = remaining();
		if (r <= 0 || strength <= 0 || Minecraft.getInstance().isPaused()) {
			return;
		}
		float t = (System.nanoTime() - startNanos) / 1.0e9f;
		float s = strength * r * r;
		pose.mulPose(Axis.ZP.rotationDegrees(Mth.sin(t * 90f) * s));
		pose.mulPose(Axis.XP.rotationDegrees(Mth.sin(t * 71f + 1.3f) * s * 0.6f));
	}
}
