package com.grandline.client.fx;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;

/**
 * Spring limbs (reusable): a forearm that becomes a spring and shoots the fist out ({@link #knock}), and legs that
 * stand on springs ({@link #legs}). Drawn in the limb's own model space, so the spring is always joined at the wrist or
 * the foot; lengths follow animation curves (fast extension, elastic overshooting retraction) and, for the legs, the
 * real distance to the ground (anchored springs while rising) and contact pulses (compress, then extend).
 * Everything times out on its own.
 */
public final class SpringLimbs {
	private static final class Arm {
		float start, reach;
	}

	private static final class Legs {
		float start, until, pulse = -99, pulseDepth;
		boolean anchored;
	}

	private static final Map<Integer, Arm> ARMS = new HashMap<>();
	private static final Map<Integer, Legs> LEGS = new HashMap<>();

	private SpringLimbs() {
	}

	/** The right forearm springs out {@code reach} blocks now and snaps back. */
	public static void knock(int entity, float age, float reach) {
		Arm a = new Arm();
		a.start = age;
		a.reach = reach;
		ARMS.put(entity, a);
	}

	/** Leg springs for {@code hold} ticks; {@code anchored}: they reach all the way down to the ground (Spring Cannon). */
	public static void legs(int entity, float age, float hold, boolean anchored) {
		Legs l = LEGS.computeIfAbsent(entity, k -> new Legs());
		if (age > l.until + 4) {
			l.start = age;
		}
		l.until = age + hold;
		l.anchored = anchored;
	}

	/** A contact: the leg springs compress by {@code depth} (0..1) and spring back. */
	public static void pulse(int entity, float age, float depth) {
		Legs l = LEGS.get(entity);
		if (l != null) {
			l.pulse = age;
			l.pulseDepth = depth;
		}
	}

	public static void retract(int entity, float age) {
		Legs l = LEGS.get(entity);
		if (l != null) {
			l.until = Math.min(l.until, age);
		}
	}

	public static void clearAll() {
		ARMS.clear();
		LEGS.clear();
	}

	/** Fist distance along the arm: compress, shoot out, hold, then an elastic return that overshoots and settles. */
	static float armExtension(Arm a, float age) {
		float t = age - a.start;
		if (t < 0 || t > 9) {
			return -1;
		}
		if (t < 0.6f) {
			return 0;
		}
		if (t < 2) {
			return (float) (a.reach * WindMesh.ease((t - 0.6) / 1.4));
		}
		if (t < 3) {
			return a.reach;
		}
		float r = (t - 3) / 6f;
		float back = (float) (1 - r) * (1 - r);
		return (float) (a.reach * back + Math.sin(r * Math.PI * 3) * 0.18 * (1 - r));
	}

	// ------------------------------------------------------------------------------------------------ drawing

	/** Right forearm (arm space): spring from the wrist and the fist at its end. */
	public static void drawArm(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player, float age, boolean left) {
		Arm a = ARMS.get(player.getId());
		if (a == null || left) {
			return;
		}
		float ext = armExtension(a, age);
		if (ext < 0) {
			ARMS.remove(player.getId());
			return;
		}
		pose.pushPose();
		pose.translate(-1f / 16f, 8f / 16f, 0);
		float compress = age - a.start < 0.6f ? 0.06f : 0.12f;
		float len = Math.max(compress, ext);
		int coils = Math.max(4, Math.min(22, (int) (4 + a.reach * 2)));
		SpringMesh.draw(pose, buffers, light, coils, 0.085f, 0.022f, len, age * 0.3f, 1f);
		// The fist rides the spring's end.
		pose.translate(0, len, 0);
		fist(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(player.getSkinTextureLocation())), light);
		pose.popPose();
	}

	/** A 4×4×4 fist cube textured from the skin's right hand (bottom of the right arm). */
	private static void fist(PoseStack pose, VertexConsumer c, int light) {
		Matrix4f m = pose.last().pose();
		Matrix3f n = pose.last().normal();
		float s = 2f / 16f;
		// Skin UVs (64x64): right arm front column 44..48, rows 28..32 (the hand), bottom 48..52, 16..20.
		float u0 = 44 / 64f, u1 = 48 / 64f, v0 = 28 / 64f, v1 = 32 / 64f;
		float[][] faces = {
				{-s, 0, -s, s, 0, -s, s, 2 * s, -s, -s, 2 * s, -s, 0, 0, -1},
				{s, 0, s, -s, 0, s, -s, 2 * s, s, s, 2 * s, s, 0, 0, 1},
				{-s, 0, s, -s, 0, -s, -s, 2 * s, -s, -s, 2 * s, s, -1, 0, 0},
				{s, 0, -s, s, 0, s, s, 2 * s, s, s, 2 * s, -s, 1, 0, 0},
				{-s, 2 * s, -s, s, 2 * s, -s, s, 2 * s, s, -s, 2 * s, s, 0, 1, 0},
				{-s, 0, s, s, 0, s, s, 0, -s, -s, 0, -s, 0, -1, 0}};
		for (float[] f : faces) {
			float[][] uv = {{u0, v1}, {u1, v1}, {u1, v0}, {u0, v0}};
			for (int i = 0; i < 4; i++) {
				c.vertex(m, f[i * 3], f[i * 3 + 1], f[i * 3 + 2]).color(1f, 1f, 1f, 1f).uv(uv[i][0], uv[i][1])
						.overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(n, f[12], f[13], f[14]).endVertex();
			}
		}
	}

	/** One leg (leg space): a spring under the foot, down to the ground or a short stance spring. */
	public static void drawLeg(PoseStack pose, MultiBufferSource buffers, int light, Entity entity, float age, boolean left) {
		Legs l = LEGS.get(entity.getId());
		if (l == null) {
			return;
		}
		float out = age <= l.until ? Mth.clamp((age - l.start) / 3f, 0, 1) : 1 - Mth.clamp((age - l.until) / 4f, 0, 1);
		if (out <= 0 && age > l.until) {
			LEGS.remove(entity.getId());
			return;
		}
		float e = (float) WindMesh.ease(out);
		// Rest length: the gap to the ground under the feet (anchored springs), or a short stance spring.
		float gap = (float) gap(entity, age);
		float len = l.anchored ? Mth.clamp(gap, 0.25f, 9f) : Mth.clamp(gap, 0.3f, 1.1f);
		// Contact pulse: compress fast, overshoot back.
		float pt = age - l.pulse;
		if (pt >= 0 && pt < 7) {
			float k = pt < 1.5f ? pt / 1.5f : 1 - (pt - 1.5f) / 5.5f;
			len *= 1 - l.pulseDepth * 0.6f * (float) Math.max(0, Math.sin(Math.min(1, k) * Math.PI / 2));
		}
		pose.pushPose();
		pose.translate(left ? 0.02f : -0.02f, 12f / 16f, 0);
		int coils = Math.max(5, Math.min(26, (int) (5 + len * 2.2)));
		SpringMesh.draw(pose, buffers, light, coils, 0.1f, 0.026f, len * e, left ? 1.3f : 0, 1f);
		pose.popPose();
	}

	private static double gap(Entity entity, float age) {
		Vec3 feet = entity.getPosition(0);
		for (int dy = 0; dy < 10; dy++) {
			BlockPos p = BlockPos.containing(feet.x, feet.y - dy - 0.01, feet.z);
			var shape = entity.level().getBlockState(p).getCollisionShape(entity.level(), p);
			if (!shape.isEmpty()) {
				return Math.max(0, feet.y - (p.getY() + shape.max(net.minecraft.core.Direction.Axis.Y)));
			}
		}
		return 10;
	}

	public static boolean any(int entity) {
		return ARMS.containsKey(entity) || LEGS.containsKey(entity);
	}
}
