package com.grandline.client.fx;

import java.util.HashMap;
import java.util.Map;

/**
 * Client-side punch animation state per player entity, driven by the server's move beats: the windup event starts
 * the shoulder pull-back, the launch event (the tick the projectile really leaves) triggers the snap and follow-through.
 * Times are in the entity's own age ticks, so remote players animate in step with what they are doing.
 */
public final class ArmAnimations {
	/** Longest the windup pose is held waiting for the launch event before it relaxes. */
	public static final float MAX_HOLD = 14;
	public static final float SNAP = 2.5f;
	public static final float FOLLOW = 7;

	private record State(float windupAge, float releaseAge) {
	}

	private static final Map<Integer, State> STATES = new HashMap<>();

	private ArmAnimations() {
	}

	public static void windup(int entityId, float age) {
		STATES.put(entityId, new State(age, -1));
	}

	public static void release(int entityId, float age) {
		State s = STATES.get(entityId);
		STATES.put(entityId, new State(s == null ? age : s.windupAge(), age));
	}

	/** Phase of the punch at this age, or null if none is playing. */
	public static Phase phase(int entityId, float age) {
		State s = STATES.get(entityId);
		if (s == null) {
			return null;
		}
		if (s.releaseAge() < 0) {
			float t = age - s.windupAge();
			if (t > MAX_HOLD || t < -1) {
				STATES.remove(entityId);
				return null;
			}
			return new Phase(Phase.Kind.WINDUP, Math.min(1f, Math.max(0f, t / 6f)));
		}
		float t = age - s.releaseAge();
		if (t < 0) {
			return new Phase(Phase.Kind.WINDUP, 1f);
		}
		if (t < SNAP) {
			return new Phase(Phase.Kind.SNAP, t / SNAP);
		}
		if (t < SNAP + FOLLOW) {
			return new Phase(Phase.Kind.FOLLOW, (t - SNAP) / FOLLOW);
		}
		STATES.remove(entityId);
		return null;
	}

	public static void clear() {
		STATES.clear();
	}

	public record Phase(Kind kind, float t) {
		public enum Kind { WINDUP, SNAP, FOLLOW }
	}
}
