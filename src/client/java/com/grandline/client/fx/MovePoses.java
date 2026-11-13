package com.grandline.client.fx;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

import java.util.HashMap;
import java.util.Map;

/**
 * Body poses for move casts, per player entity, driven by the server's beats so every viewer sees the caster move in
 * step with the effect: a windup pose (held until the release beat), a sharp snap into the release pose on the
 * release tick, then a follow-through back to normal. Times are in the entity's own age ticks.
 */
public final class MovePoses {
	public enum Kind {
		/** Casting hand raised, fire coiling round it. */
		READY,
		/** Straight arm thrust (right or left). */
		PUNCH,
		/** Arm across the chest, then swept out to the side. */
		SWEEP,
		/** Feet planted, arms low, then thrown down and out. */
		PLANT,
		/** Both arms raised overhead holding a mass, then hurled forward with a lean. */
		HEAVY,
		/** Arms swept back while flying (held until cleared). */
		FLIGHT
	}

	private record Anim(Kind kind, float start, float release, boolean left) {
	}

	static final float SNAP = 2.5f;
	static final float FOLLOW = 7f;
	private static final Map<Integer, Anim> ANIMS = new HashMap<>();

	private MovePoses() {
	}

	/** Starts (or refreshes) a windup / hold pose. */
	public static void windup(int entity, Kind kind, float age) {
		Anim a = ANIMS.get(entity);
		if (a != null && a.kind() == kind && a.release() < 0) {
			ANIMS.put(entity, new Anim(kind, a.start(), -1, a.left()));
			return;
		}
		ANIMS.put(entity, new Anim(kind, age, -1, false));
	}

	/** Plays the release snap and follow-through of {@code kind}. */
	public static void release(int entity, Kind kind, float age, boolean left) {
		Anim a = ANIMS.get(entity);
		float start = a != null && a.release() < 0 ? a.start() : age - 4;
		ANIMS.put(entity, new Anim(kind, start, age, left));
	}

	public static void clear(int entity, Kind kind) {
		Anim a = ANIMS.get(entity);
		if (a != null && a.kind() == kind) {
			ANIMS.remove(entity);
		}
	}

	public static void clearAll() {
		ANIMS.clear();
	}

	private static float ease(float t) {
		t = Mth.clamp(t, 0, 1);
		return t * t * (3 - 2 * t);
	}

	private static float out(float t) {
		t = Mth.clamp(t, 0, 1);
		return 1 - (1 - t) * (1 - t);
	}

	/** Longest a windup pose waits for its release before relaxing. */
	private static float maxHold(Kind kind) {
		return switch (kind) {
			case HEAVY -> 70;
			case FLIGHT -> Float.MAX_VALUE;
			case READY -> 24;
			default -> 14;
		};
	}

	/** Applies the pose on top of vanilla's (called at the end of HumanoidModel.setupAnim for players). */
	public static void apply(int entity, float age, ModelPart head, ModelPart body, ModelPart rightArm, ModelPart leftArm) {
		Anim a = ANIMS.get(entity);
		if (a == null) {
			return;
		}
		float w;    // 0..1 how far into the windup pose
		float r;    // 0..1 how far the release snap has gone (0 before release)
		float back; // 0..1 how far the follow-through has relaxed back to normal
		if (a.release() < 0) {
			float t = age - a.start();
			if (t > maxHold(a.kind()) || t < -2) {
				ANIMS.remove(entity);
				return;
			}
			w = ease(t / 4f);
			r = 0;
			back = 0;
		} else {
			float t = age - a.release();
			if (t > SNAP + FOLLOW) {
				ANIMS.remove(entity);
				return;
			}
			w = 1;
			r = out(t / SNAP);
			back = t < SNAP ? 0 : ease((t - SNAP) / FOLLOW);
		}
		float forward = -Mth.HALF_PI + head.xRot;
		switch (a.kind()) {
			case READY -> {
				blend(rightArm, w, forward * 0.75f, -0.35f, 0.1f);
			}
			case PUNCH -> {
				ModelPart arm = a.left() ? leftArm : rightArm;
				float s = a.left() ? -1 : 1;
				// windup pose: drawn back; release: straight along the look direction.
				float x = Mth.lerp(r, forward * 0.55f + 0.5f, forward - 0.05f);
				float y = Mth.lerp(r, 0.4f * s, -0.1f * s);
				float twist = Mth.lerp(r, 0.3f * s, -0.3f * s) * (1 - back);
				blend(arm, w * (1 - back), x, y, 0);
				turn(body, rightArm, leftArm, twist);
			}
			case SWEEP -> {
				float y = Mth.lerp(r, 1.0f, -0.9f);
				float twist = Mth.lerp(r, 0.5f, -0.45f) * (1 - back);
				blend(rightArm, w * (1 - back), -1.45f, y, 0);
				turn(body, rightArm, leftArm, twist);
			}
			case PLANT -> {
				float k = w * (1 - back);
				float x = Mth.lerp(r, 0.45f, -0.5f);
				float z = Mth.lerp(r, 0.55f, 1.15f);
				blend(rightArm, k, x, 0, z);
				blend(leftArm, k, x, 0, -z);
				body.xRot += 0.3f * k * (1 - r * 0.5f);
			}
			case HEAVY -> {
				float k = w * (1 - back);
				float x = Mth.lerp(r, -Mth.PI + 0.25f, forward);
				blend(rightArm, k, x, 0, Mth.lerp(r, -0.18f, 0));
				blend(leftArm, k, x, 0, Mth.lerp(r, 0.18f, 0));
				body.xRot += 0.35f * r * k;
			}
			case FLIGHT -> {
				blend(rightArm, w, 0.55f, 0, 0.35f);
				blend(leftArm, w, 0.55f, 0, -0.35f);
				body.xRot += 0.18f * w;
			}
		}
	}

	private static void blend(ModelPart arm, float k, float x, float y, float z) {
		arm.xRot = Mth.lerp(k, arm.xRot, x);
		arm.yRot = Mth.lerp(k, arm.yRot, y);
		arm.zRot = Mth.lerp(k, arm.zRot, z);
	}

	/** Turns the torso and carries both shoulders with it (as vanilla's attack swing does). */
	private static void turn(ModelPart body, ModelPart rightArm, ModelPart leftArm, float twist) {
		if (Math.abs(twist) < 1e-3) {
			return;
		}
		body.yRot = twist;
		rightArm.z = Mth.sin(twist) * 5.0F;
		rightArm.x = -Mth.cos(twist) * 5.0F;
		leftArm.z = -Mth.sin(twist) * 5.0F;
		leftArm.x = Mth.cos(twist) * 5.0F;
		rightArm.yRot += twist;
		leftArm.yRot += twist;
	}
}
