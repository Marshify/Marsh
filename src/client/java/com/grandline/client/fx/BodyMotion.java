package com.grandline.client.fx;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

/**
 * Continuous, rotation-driven body animation for players (Spin's moves, and any later whirling, spinning or rotor
 * move): sustained states with smooth spin-up and spin-down instead of MovePoses' one-shot windup/release.
 *
 * <ul>
 *   <li>{@link Kind#ARM_WHIRL}: arms reach forward and whirl in tight circles, faster the longer it's held.</li>
 *   <li>{@link Kind#BODY_SPIN}: arms out, the whole model spins about its vertical axis and leans into its travel.</li>
 *   <li>{@link Kind#ROTOR}: arms raised above the shoulders sweeping in fast cones, legs hanging, the body pitching
 *   with speed and banking into turns.</li>
 *   <li>{@link Kind#SLASH}: an upper-body twist with alternating arm sweeps synced to each released blade, and a
 *   two-arm finishing sweep.</li>
 * </ul>
 *
 * Arms are aimed by direction ({@link #aim}): every pose is a rotation about the shoulder joint, so hands stay attached
 * and never bend past the model's joints. Whole-body rotation happens only in the renderer's pose stack
 * ({@link #applyBody}); the camera and the player's real facing are never touched.
 */
public final class BodyMotion {
	public enum Kind {
		ARM_WHIRL, BODY_SPIN, ROTOR, SLASH,
		/** Three-step melee: right horizontal, left diagonal, two-arm cross ({@link #strike}). */
		STRIKE,
		/** Low forward dash stance with both arms trailing; {@link #finish} plays a crossing slash. */
		RUSH,
		/** A controlled spinning flurry of alternating cuts; {@link #finish} opens into a wide final sweep. */
		DANCE,
		/** Both arms straight up holding a rotor each; legs hang; leans and banks like {@link #ROTOR}. */
		ARM_ROTOR,
		/** A fast straight punch along the aim (pulled back for a moment first), the other arm and torso recoiling. */
		PUNCH_EXTEND,
		/**
		 * Spring-legged movement ({@link #spring}): variant 0 a crouched launch then a lean into the dash, 1 a vertical
		 * rise and slam, 2/3 bouncing; {@link #contact} turns the legs onto the real contact surface for a moment.
		 */
		SPRING_MOVE,
		/**
		 * Bomb ({@link #bomb}): 0 throw (wind-up / charging stance, {@link #release} throws), 1 directing a targeted
		 * charge, 2 propulsion dash and grab, 3 ring toss, 4 planting mines, 5 Self Destruct tension, 6 explosive jump.
		 */
		BOMB,
		/** Smoke ({@link #smoke}): 0 Smoke Slam, 1 Smoke Blast, 2 Smoke Liberation, 3 Smoke Bomber flight. */
		SMOKE,
		/** Spike ({@link #spike}): 0 Spike Summon (arm directed at the target, sharp release), 1 Spike Barrage (tension, thrust). */
		SPIKE
	}

	private static final class State {
		final Kind kind;
		final float start;
		float end = -1;       // age the move ended (spin-down starts), or -1 while active
		float lastAge;
		double phase;         // accumulated rotation (radians)
		double spin;          // accumulated whole-body yaw (degrees)
		float lean, bank;     // smoothed body lean / bank (degrees)
		float slashAge = -99; // last blade release (SLASH)
		boolean slashLeft, finale;
		int variant;
		Vec3 contact;     // world normal of the last surface contact (SPRING_MOVE)
		float yawOffset;  // smoothed turn toward the travel direction (degrees)

		State(Kind kind, float start) {
			this.kind = kind;
			this.start = start;
			this.lastAge = start;
		}
	}

	/** Ticks to spin up and to spin down. */
	static final float SPIN_UP = 6, SPIN_DOWN = 10;

	private static final Map<Integer, State> STATES = new HashMap<>();

	private BodyMotion() {
	}

	public static void start(int entity, Kind kind, float age) {
		STATES.put(entity, new State(kind, age));
	}

	/** Begins the smooth spin-down of {@code kind} (no-op if another state replaced it). */
	public static void stop(int entity, Kind kind, float age) {
		State s = STATES.get(entity);
		if (s != null && s.kind == kind && s.end < 0) {
			s.end = age;
		}
	}

	/** SLASH: a blade leaves the left or right hand now (or the two-arm finishing sweep). */
	public static void slash(int entity, float age, boolean left, boolean finale) {
		State s = STATES.get(entity);
		if (s == null || s.kind != Kind.SLASH) {
			s = new State(Kind.SLASH, age);
			STATES.put(entity, s);
		}
		s.slashAge = age;
		s.slashLeft = left;
		s.finale = finale;
	}

	/** STRIKE: combo step {@code step} (0, 1, 2) swings now. */
	public static void strike(int entity, float age, int step) {
		State s = new State(Kind.STRIKE, age);
		s.slashAge = age;
		s.variant = step;
		STATES.put(entity, s);
	}

	/** PUNCH_EXTEND: a punch now. */
	public static void punch(int entity, float age) {
		STATES.put(entity, new State(Kind.PUNCH_EXTEND, age));
	}

	/** SPRING_MOVE of {@code variant} starts (stopped with {@link #stop}). */
	public static void spring(int entity, float age, int variant) {
		State s = new State(Kind.SPRING_MOVE, age);
		s.variant = variant;
		STATES.put(entity, s);
	}

	/** BOMB pose {@code variant} starts (ended with {@link #stop}). */
	public static void bomb(int entity, float age, int variant) {
		State s = new State(Kind.BOMB, age);
		s.variant = variant;
		STATES.put(entity, s);
	}

	/** SPIKE pose {@code variant} starts (released with {@link #release}, ended with {@link #stop}). */
	public static void spike(int entity, float age, int variant) {
		State s = new State(Kind.SPIKE, age);
		s.variant = variant;
		STATES.put(entity, s);
	}

	/** SMOKE pose {@code variant} starts (ended with {@link #stop}). */
	public static void smoke(int entity, float age, int variant) {
		State s = new State(Kind.SMOKE, age);
		s.variant = variant;
		STATES.put(entity, s);
	}

	/** BOMB: the throw / grab / detonation happens now. */
	public static void release(int entity, float age) {
		State s = STATES.get(entity);
		if (s != null && (s.kind == Kind.BOMB || s.kind == Kind.SPIKE)) {
			s.slashAge = age;
			s.finale = true;
			if (s.end < 0) {
				s.end = age + 6;
			}
		}
	}

	/** SPRING_MOVE: the legs met a surface with world normal {@code normal} now. */
	public static void contact(int entity, float age, Vec3 normal) {
		State s = STATES.get(entity);
		if (s != null && s.kind == Kind.SPRING_MOVE) {
			s.contact = normal;
			s.slashAge = age;
		}
	}

	/** RUSH or DANCE: the finishing cut happens now (the state then winds down on its own). */
	public static void finish(int entity, Kind kind, float age) {
		State s = STATES.get(entity);
		if (s != null && s.kind == kind) {
			s.finale = true;
			s.slashAge = age;
			if (s.end < 0) {
				s.end = age + (kind == Kind.DANCE ? 6 : 8);
			}
		}
	}

	public static boolean active(int entity, Kind kind) {
		State s = STATES.get(entity);
		return s != null && s.kind == kind;
	}

	public static void clear(int entity) {
		STATES.remove(entity);
	}

	public static void clearAll() {
		STATES.clear();
	}

	/** 0..1 how strongly the state is applied: eases in at the start and out during the spin-down. */
	static float weight(State s, float age) {
		float in = Mth.clamp((age - s.start) / 3f, 0, 1);
		float out = s.end < 0 ? 1 : 1 - Mth.clamp((age - s.end) / SPIN_DOWN, 0, 1);
		return (float) WindMesh.ease(Math.min(in, out));
	}

	/** Current rotation speed (radians per tick) for the state: spin-up, intensity growth, spin-down. */
	static double rate(State s, float age) {
		float t = age - s.start;
		double up = WindMesh.ease(t / SPIN_UP);
		double max = switch (s.kind) {
			case ARM_WHIRL -> 1.1 + 0.9 * Mth.clamp(t / 50f, 0, 1); // faster the longer it's held
			case BODY_SPIN -> 1.2;
			case ROTOR -> 1.5;
			case ARM_ROTOR -> 1.8;
			case DANCE -> 0.42; // body turns (radians per tick): two and a half turns over the move
			case SLASH, STRIKE, RUSH, PUNCH_EXTEND, SPRING_MOVE, BOMB, SMOKE, SPIKE -> 0;
		};
		double down = s.end < 0 ? 1 : 1 - WindMesh.ease((age - s.end) / SPIN_DOWN);
		return max * up * down;
	}

	/** The rotor/whirl phase for anchored emitters (radians), or 0. */
	public static double phase(int entity) {
		State s = STATES.get(entity);
		return s == null ? 0 : s.phase;
	}

	/** 0..1 how fast the state currently turns compared with its top speed. */
	public static float intensity(int entity, float age) {
		State s = STATES.get(entity);
		if (s == null) {
			return 0;
		}
		double r = rate(s, age);
		return (float) Mth.clamp(r / 1.5, 0, 1);
	}

	private static void advance(State s, float age) {
		float dt = Mth.clamp(age - s.lastAge, 0, 2);
		if (dt > 0) {
			double r = rate(s, age);
			s.phase += r * dt;
			if (s.kind == Kind.DANCE) {
				s.spin += Math.toDegrees(r) * dt;
			}
			if (s.kind == Kind.BODY_SPIN) {
				s.spin += Math.toDegrees(r * 0.9) * dt; // a little slower than the arms: readable, never a blur
			}
			s.lastAge = age;
		}
	}

	// ------------------------------------------------------------------------------------------------ model posing

	/** Poses arms, torso and legs (called at the end of HumanoidModel.setupAnim for players). */
	public static void apply(int entity, float age, ModelPart head, ModelPart body, ModelPart rightArm, ModelPart leftArm,
			ModelPart rightLeg, ModelPart leftLeg) {
		State s = STATES.get(entity);
		if (s == null) {
			return;
		}
		if (s.end >= 0 && age - s.end > SPIN_DOWN || age - s.start > 20 * 60 || age < s.start - 2) {
			STATES.remove(entity); // spun down, or a lost end beat (never animate forever)
			return;
		}
		advance(s, age);
		float w = weight(s, age);
		double ph = s.phase;
		switch (s.kind) {
			case ARM_WHIRL -> {
				// Arms reach forward (following the aim a little) and whirl in tight opposite circles.
				double aimUp = -head.xRot * 0.5;
				double c = Math.toRadians(14 + 10 * Math.min(1, (age - s.start) / 50f));
				aim(rightArm, w, forwardUp(aimUp, c, ph, false));
				aim(leftArm, w, forwardUp(aimUp, c, ph + Math.PI, true));
				body.xRot = Mth.lerp(w, body.xRot, 0.12f);
			}
			case BODY_SPIN -> {
				// Arms thrown out to the sides, slightly raised; the whole model spins (applyBody).
				aim(rightArm, w, dir(0.25, 0.12, 0.96, false));
				aim(leftArm, w, dir(0.25, 0.12, 0.96, true));
				rightLeg.zRot = Mth.lerp(w, rightLeg.zRot, 0.12f);
				leftLeg.zRot = Mth.lerp(w, leftLeg.zRot, -0.12f);
			}
			case ROTOR -> {
				// Arms raised above the shoulders, each hand sweeping a fast cone: a rotor of arms.
				double c = Math.toRadians(32);
				aim(rightArm, w, rotorArm(c, ph, false));
				aim(leftArm, w, rotorArm(c, ph + Math.PI, true));
				// Legs hang a little behind with a slow, loose sway (never a walk cycle).
				float sway = Mth.sin(age * 0.22f) * 0.08f;
				rightLeg.xRot = Mth.lerp(w, rightLeg.xRot, 0.22f + sway);
				leftLeg.xRot = Mth.lerp(w, leftLeg.xRot, 0.14f - sway);
				rightLeg.zRot = Mth.lerp(w, rightLeg.zRot, 0.06f);
				leftLeg.zRot = Mth.lerp(w, leftLeg.zRot, -0.06f);
				rightLeg.yRot = 0;
				leftLeg.yRot = 0;
			}
			case STRIKE -> {
				float t = age - s.slashAge;
				if (t > 14) {
					STATES.remove(entity);
					return;
				}
				float k = Mth.clamp(1 - (t - 6) / 8f, 0, 1);
				double sweep = WindMesh.ease(t / 3.0);
				switch (s.variant) {
					case 0 -> { // right forearm, flat and fast from the outside across
						aim(rightArm, k, dir(0.65, 0.05, Mth.lerp(sweep, 0.9, -0.7), false));
						body.yRot = Mth.lerp(k, body.yRot, (float) Mth.lerp(sweep, -0.3, 0.35));
					}
					case 1 -> { // left forearm, high outside down across the body
						aim(leftArm, k, dir(0.5, Mth.lerp(sweep, 0.8, -0.4), Mth.lerp(sweep, 0.6, -0.6), true));
						body.yRot = Mth.lerp(k, body.yRot, (float) Mth.lerp(sweep, 0.3, -0.35));
					}
					default -> { // both blades crossing in front
						aim(rightArm, k, dir(Mth.lerp(sweep, 0.3, 0.75), Mth.lerp(sweep, 0.45, -0.15), Mth.lerp(sweep, 1.0, -0.45), false));
						aim(leftArm, k, dir(Mth.lerp(sweep, 0.3, 0.75), Mth.lerp(sweep, 0.45, -0.15), Mth.lerp(sweep, 1.0, -0.45), true));
						body.xRot = Mth.lerp(k, body.xRot, (float) (0.22 * sweep));
					}
				}
			}
			case RUSH -> {
				if (!s.finale) {
					// Low, leaning stance; both blades held back and out, ready to cut.
					aim(rightArm, w, dir(-0.45, -0.25, 0.55, false));
					aim(leftArm, w, dir(-0.45, -0.25, 0.55, true));
					body.xRot = Mth.lerp(w, body.xRot, 0.5f);
					rightLeg.xRot = Mth.lerp(w, rightLeg.xRot, -0.55f);
					leftLeg.xRot = Mth.lerp(w, leftLeg.xRot, 0.45f);
				} else {
					double sweep = WindMesh.ease((age - s.slashAge) / 2.5);
					aim(rightArm, w, dir(Mth.lerp(sweep, 0.2, 0.8), Mth.lerp(sweep, 0.3, -0.1), Mth.lerp(sweep, 1.0, -0.5), false));
					aim(leftArm, w, dir(Mth.lerp(sweep, 0.2, 0.8), Mth.lerp(sweep, 0.3, -0.1), Mth.lerp(sweep, 1.0, -0.5), true));
					body.xRot = Mth.lerp(w, body.xRot, (float) (0.3 * (1 - sweep) + 0.15));
				}
			}
			case DANCE -> {
				if (s.finale) {
					// The wide final sweep: both blades thrown out level.
					aim(rightArm, w, dir(0.3, 0.05, 1.0, false));
					aim(leftArm, w, dir(0.3, 0.05, 1.0, true));
				} else {
					// Alternating cuts every 6 ticks: right flat, left diagonal; the body keeps turning (applyBody).
					float seg = ((age - s.start) % 12) / 6f;
					boolean right = seg < 1;
					double sweep = WindMesh.ease((seg % 1) * 1.6);
					if (right) {
						aim(rightArm, w, dir(0.6, 0.1, Mth.lerp(sweep, 0.95, -0.5), false));
						aim(leftArm, w * 0.7f, dir(-0.2, -0.1, 0.8, true));
					} else {
						aim(leftArm, w, dir(0.55, Mth.lerp(sweep, 0.7, -0.35), Mth.lerp(sweep, 0.8, -0.5), true));
						aim(rightArm, w * 0.7f, dir(-0.2, -0.1, 0.8, false));
					}
				}
			}
			case PUNCH_EXTEND -> {
				float t = age - s.start;
				if (t > 10) {
					STATES.remove(entity);
					return;
				}
				float k = Mth.clamp(1 - (t - 5) / 5f, 0, 1);
				double pitch = -head.xRot;
				if (t < 0.6f) {
					aim(rightArm, k, dir(-0.25, -0.15, 0.35, false)); // pulled back
				} else {
					// Along the aim: the head's pitch, and its yaw relative to the body (the look can lead the body).
					Vec3 d = dir(Math.cos(pitch), Math.sin(pitch), 0.05, false);
					double hy = head.yRot;
					d = new Vec3(d.x * Math.cos(hy) + d.z * Math.sin(hy), d.y, -d.x * Math.sin(hy) + d.z * Math.cos(hy));
					aim(rightArm, k, d);
				}
				aim(leftArm, k * 0.8f, dir(-0.35, -0.25, 0.45, true)); // thrown back by the recoil
				body.yRot = Mth.lerp(k, body.yRot, t < 0.6f ? 0.25f : -0.3f);
			}
			case SPRING_MOVE -> {
				float t = age - s.start;
				switch (s.variant) {
					case 0 -> { // Spring Snipe: crouch, then arms trail behind the launch
						if (t < 4) {
							body.xRot = Mth.lerp(w, body.xRot, 0.55f);
							rightLeg.xRot = Mth.lerp(w, rightLeg.xRot, -0.45f);
							leftLeg.xRot = Mth.lerp(w, leftLeg.xRot, -0.45f);
							aim(rightArm, w, dir(0.3, -0.5, 0.3, false));
							aim(leftArm, w, dir(0.3, -0.5, 0.3, true));
						} else {
							aim(rightArm, w, dir(-0.5, -0.2, 0.45, false));
							aim(leftArm, w, dir(-0.5, -0.2, 0.45, true));
						}
					}
					case 1 -> { // Spring Cannon: arms up and out on the rise, driven down for the slam
						boolean slam = s.finale;
						aim(rightArm, w, slam ? dir(0.6, -0.6, 0.3, false) : dir(0.1, 0.75, 0.65, false));
						aim(leftArm, w, slam ? dir(0.6, -0.6, 0.3, true) : dir(0.1, 0.75, 0.65, true));
					}
					default -> { // bouncing: arms out for balance
						aim(rightArm, w, dir(0.2, 0.15, 0.95, false));
						aim(leftArm, w, dir(0.2, 0.15, 0.95, true));
					}
				}
				// At a contact the legs point onto the real surface (wall or ground) while the springs compress.
				float ct = age - s.slashAge;
				if (s.contact != null && ct >= 0 && ct < 6) {
					Entity e = net.minecraft.client.Minecraft.getInstance().level == null ? null
							: net.minecraft.client.Minecraft.getInstance().level.getEntity(entity);
					if (e != null) {
						double yaw = Math.toRadians(e.getYRot()) - Math.toRadians(s.yawOffset);
						Vec3 fwd = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw)), right = new Vec3(-Math.cos(yaw), 0, -Math.sin(yaw));
						Vec3 into = s.contact.scale(-1);
						Vec3 m = new Vec3(-into.dot(right), -into.y, -into.dot(fwd)).normalize();
						float k = (float) Math.sin(Math.min(1, ct / 6f) * Math.PI);
						aim(rightLeg, k, m.add(-0.12, 0, 0).normalize());
						aim(leftLeg, k, m.add(0.12, 0, 0).normalize());
					}
				}
			}
			case BOMB -> {
				float t = age - s.start;
				float rt = age - s.slashAge;
				switch (s.variant) {
					case 0 -> { // Blast Toss: arm drawn back holding the bomb (deeper the longer it charges), then the throw
						if (!s.finale) {
							float k = Mth.clamp(t / 20f, 0, 1);
							aim(rightArm, w, dir(-0.45 - 0.25 * k, 0.55 + 0.2 * k, 0.35, false));
							aim(leftArm, w, dir(0.8, 0.2, 0.25, true));
							body.yRot = Mth.lerp(w, body.yRot, 0.35f + 0.15f * k);
						} else {
							float k = Mth.clamp(rt / 3f, 0, 1);
							double pitch = -head.xRot;
							aim(rightArm, w, dir(-0.4 + 1.4 * k, 0.75 - 0.6 * k + Math.sin(pitch) * 0.4 * k, 0.3 - 0.25 * k, false).normalize());
							aim(leftArm, w, dir(-0.3, -0.2, 0.5, true));
							body.yRot = Mth.lerp(w, body.yRot, 0.4f - 0.75f * k);
						}
					}
					case 1 -> { // Targeted Bomb: casting arm extended toward the target, other hand at the chest
						double pitch = -head.xRot;
						Vec3 d = dir(Math.cos(pitch), Math.sin(pitch), 0.08, false);
						double hy = head.yRot;
						d = new Vec3(d.x * Math.cos(hy) + d.z * Math.sin(hy), d.y, -d.x * Math.sin(hy) + d.z * Math.cos(hy));
						float pulse = s.finale ? Mth.clamp(1 - rt / 4f, 0, 1) : 0;
						aim(rightArm, w, d.add(0, 0.25 * pulse, 0).normalize());
						aim(leftArm, w, dir(0.55, 0.15, -0.35, true));
						body.yRot = Mth.lerp(w, body.yRot, -0.2f);
					}
					case 2 -> { // Bomb Grab: lean into the blast, both arms reaching; then the slap of the bomb
						if (!s.finale) {
							aim(rightArm, w, dir(0.9, 0.05, 0.3, false));
							aim(leftArm, w, dir(0.9, 0.05, 0.3, true));
						} else {
							float k = Mth.clamp(rt / 4f, 0, 1);
							aim(rightArm, w, dir(0.7, 0.55 - 0.5 * k, 0.1, false));
							aim(leftArm, w, dir(0.8, -0.1, 0.4, true));
						}
						rightLeg.xRot = Mth.lerp(w, rightLeg.xRot, 0.5f);
						leftLeg.xRot = Mth.lerp(w, leftLeg.xRot, -0.35f);
					}
					case 3 -> { // Land Mines ring: both arms fling out wide
						float k = Mth.clamp(t / 4f, 0, 1);
						aim(rightArm, w, dir(0.5 - 0.4 * k, 0.1 + 0.15 * k, 0.4 + 0.6 * k, false));
						aim(leftArm, w, dir(0.5 - 0.4 * k, 0.1 + 0.15 * k, 0.4 + 0.6 * k, true));
						if (t > 10 && s.end < 0) {
							s.end = age;
						}
					}
					case 4 -> { // planting mines: crouched, hands down to the ground
						body.xRot = Mth.lerp(w, body.xRot, 0.5f);
						aim(rightArm, w, dir(0.6, -0.8, 0.15, false));
						aim(leftArm, w, dir(0.45, -0.85, 0.25, true));
						rightLeg.xRot = Mth.lerp(w, rightLeg.xRot, -0.5f);
						leftLeg.xRot = Mth.lerp(w, leftLeg.xRot, -0.5f);
					}
					case 5 -> { // Self Destruct: braced, arms down and out, trembling harder as it charges
						float k = Mth.clamp(t / 80f, 0, 1);
						float shake = Mth.sin(age * 3.1f) * 0.05f * k + Mth.sin(age * 5.3f) * 0.03f * k;
						aim(rightArm, w, dir(0.15, -0.65, 0.55 + shake, false));
						aim(leftArm, w, dir(0.15, -0.65, 0.55 - shake, true));
						body.xRot = Mth.lerp(w, body.xRot, 0.12f + 0.1f * k);
						rightLeg.zRot = Mth.lerp(w, rightLeg.zRot, 0.16f);
						leftLeg.zRot = Mth.lerp(w, leftLeg.zRot, -0.16f);
					}
					default -> { // Explosive Jump: a quick crouch, then arms swept back and legs tucked in the air
						if (t < 2) {
							body.xRot = Mth.lerp(w, body.xRot, 0.5f);
							rightLeg.xRot = Mth.lerp(w, rightLeg.xRot, -0.6f);
							leftLeg.xRot = Mth.lerp(w, leftLeg.xRot, -0.6f);
						} else {
							aim(rightArm, w, dir(-0.45, 0.35, 0.55, false));
							aim(leftArm, w, dir(-0.45, 0.35, 0.55, true));
							rightLeg.xRot = Mth.lerp(w, rightLeg.xRot, -0.55f);
							leftLeg.xRot = Mth.lerp(w, leftLeg.xRot, 0.3f);
						}
					}
				}
				if (s.end < 0 && t > 20 * 8) {
					s.end = age; // a lost end beat never holds a pose
				}
			}
			case SPIKE -> {
				float t = age - s.start;
				float rt = age - s.slashAge;
				double pitch = -head.xRot;
				Vec3 aimDir = dir(Math.cos(pitch), Math.sin(pitch), 0.05, false);
				double hy = head.yRot;
				aimDir = new Vec3(aimDir.x * Math.cos(hy) + aimDir.z * Math.sin(hy), aimDir.y, -aimDir.x * Math.sin(hy) + aimDir.z * Math.cos(hy));
				if (s.variant == 0) { // Spike Summon: the arm points at the target while charging; a sharp upward flick on release
					float flick = s.finale ? (float) Math.sin(Math.min(1, rt / 4f) * Math.PI) : 0;
					aim(rightArm, w, aimDir.add(0, 0.6 * flick, 0).normalize());
					aim(leftArm, w, dir(0.2, -0.3, 0.5, true));
					body.yRot = Mth.lerp(w, body.yRot, -0.15f);
				} else if (!s.finale) { // Spike Barrage charge: grounded, arm drawn back, trembling with tension
					float k = Mth.clamp(t / 60f, 0, 1);
					float shake = Mth.sin(age * 2.9f) * 0.04f * k;
					aim(rightArm, w, dir(-0.5, -0.1 + shake, 0.45, false));
					aim(leftArm, w, dir(0.5, -0.2, 0.3, true));
					body.xRot = Mth.lerp(w, body.xRot, 0.15f + 0.12f * k);
					body.yRot = Mth.lerp(w, body.yRot, 0.35f);
					rightLeg.xRot = Mth.lerp(w, rightLeg.xRot, 0.3f);
					leftLeg.xRot = Mth.lerp(w, leftLeg.xRot, -0.35f);
				} else { // release: forceful forward thrust, then the arm follows the aim that steers the row
					float k = Mth.clamp(rt / 3f, 0, 1);
					aim(rightArm, w, dir(-0.5 + 1.4 * k, -0.1 - 0.4 * k, 0.45 - 0.35 * k, false).normalize().lerp(aimDir, k * 0.5).normalize());
					aim(leftArm, w, dir(-0.3, -0.3, 0.45, true));
					body.yRot = Mth.lerp(w, body.yRot, 0.35f - 0.6f * k);
				}
				if (s.end < 0 && t > 20 * 5) {
					s.end = age;
				}
			}
			case SMOKE -> {
				float t = age - s.start;
				switch (s.variant) {
					case 0 -> { // Smoke Slam: arm drawn back and up, then driven down; torso and other arm follow the force
						if (t < 3) {
							aim(rightArm, w, dir(-0.35, 0.75, 0.3, false));
							body.yRot = Mth.lerp(w, body.yRot, 0.3f);
						} else {
							float k = Mth.clamp((t - 3) / 3f, 0, 1);
							aim(rightArm, w, dir(0.3 + 0.6 * k, 0.9 - 1.3 * k, 0.15, false).normalize());
							body.yRot = Mth.lerp(w, body.yRot, 0.3f - 0.55f * k);
							body.xRot = Mth.lerp(w, body.xRot, 0.3f * k);
						}
						aim(leftArm, w, dir(-0.25, -0.45, 0.45, true));
						if (t > 16 && s.end < 0) {
							s.end = age;
						}
					}
					case 1 -> { // Smoke Blast: draw the arm back gathering smoke, then thrust it at the crosshair
						if (t < 2) {
							aim(rightArm, w, dir(-0.5, 0.1, 0.45, false));
						} else {
							double pitch = -head.xRot;
							Vec3 d = dir(Math.cos(pitch), Math.sin(pitch), 0.05, false);
							double hy = head.yRot;
							aim(rightArm, w, new Vec3(d.x * Math.cos(hy) + d.z * Math.sin(hy), d.y, -d.x * Math.sin(hy) + d.z * Math.cos(hy)));
						}
						aim(leftArm, w, dir(0.3, -0.3, 0.4, true));
						if (t > 10 && s.end < 0) {
							s.end = age;
						}
					}
					case 2 -> { // Smoke Liberation: grounded stance, both arms flung wide and slightly up
						float k = Mth.clamp(t / 3f, 0, 1);
						aim(rightArm, w, dir(0.35 - 0.25 * k, 0.1 + 0.2 * k, 0.5 + 0.5 * k, false));
						aim(leftArm, w, dir(0.35 - 0.25 * k, 0.1 + 0.2 * k, 0.5 + 0.5 * k, true));
						rightLeg.zRot = Mth.lerp(w, rightLeg.zRot, 0.18f);
						leftLeg.zRot = Mth.lerp(w, leftLeg.zRot, -0.18f);
						if (t > 26 && s.end < 0) {
							s.end = age;
						}
					}
					default -> { // Smoke Bomber: arms trailing down and back as smoke streams, legs loosely bent
						float sway = Mth.sin(age * 0.15f) * 0.06f;
						aim(rightArm, w, dir(-0.35, -0.8, 0.25 + sway, false));
						aim(leftArm, w, dir(-0.35, -0.8, 0.25 - sway, true));
						rightLeg.xRot = Mth.lerp(w, rightLeg.xRot, 0.35f);
						leftLeg.xRot = Mth.lerp(w, leftLeg.xRot, 0.15f);
					}
				}
				if (s.end < 0 && s.variant != 3 && t > 20 * 4) {
					s.end = age;
				}
			}
			case ARM_ROTOR -> {
				// Both arms straight up, each holding a rotor; legs hang loose beneath.
				aim(rightArm, w, dir(0.05, 1, 0.1, false));
				aim(leftArm, w, dir(0.05, 1, 0.1, true));
				float sway = Mth.sin(age * 0.2f) * 0.07f;
				rightLeg.xRot = Mth.lerp(w, rightLeg.xRot, 0.18f + sway);
				leftLeg.xRot = Mth.lerp(w, leftLeg.xRot, 0.1f - sway);
				rightLeg.yRot = 0;
				leftLeg.yRot = 0;
			}
			case SLASH -> {
				float t = age - s.slashAge;
				if (t > 14) {
					STATES.remove(entity);
					return;
				}
				float k = Mth.clamp(t < 0 ? 0 : 1 - (t - 6) / 8f, 0, 1); // hold, then ease back to the combat stance
				if (s.finale) {
					// Two-arm finishing sweep: from overhead-back to forward and down, torso following.
					double sweep = WindMesh.ease(t / 3.0);
					double up = Mth.lerp(sweep, 0.9, -0.25);
					aim(rightArm, k, dir(0.35, up, 0.35 + 0.4 * sweep, false));
					aim(leftArm, k, dir(0.35, up, 0.35 + 0.4 * sweep, true));
					body.xRot = Mth.lerp(k, body.xRot, (float) (0.25 * sweep));
				} else {
					// One arm sweeps across from the far side and out: the blade leaves at the middle of the arc.
					double sweep = WindMesh.ease(t / 3.0);
					double outward = Mth.lerp(sweep, -0.75, 0.85);
					boolean left = s.slashLeft;
					aim(left ? leftArm : rightArm, k, dir(0.55, 0.05, outward, left));
					body.yRot = Mth.lerp(k, body.yRot, (float) ((left ? 1 : -1) * Mth.lerp(sweep, -0.45, 0.35)));
				}
			}
		}
	}

	/** Forward-reaching whirl: the hand circles round the forward direction (pitched by {@code aimUp}). */
	private static Vec3 forwardUp(double aimUp, double cone, double phase, boolean left) {
		double up = Math.sin(aimUp) + Math.sin(cone) * Math.cos(phase);
		double out = Math.sin(cone) * Math.sin(phase) * (left ? -1 : 1) + 0.12;
		return dir(Math.cos(aimUp), up, out, left);
	}

	/** Raised rotor arm: a cone about the up-and-slightly-out axis above the shoulder. */
	private static Vec3 rotorArm(double cone, double phase, boolean left) {
		double up = Math.cos(cone);
		double out = 0.25 + Math.sin(cone) * Math.cos(phase);
		double fwd = Math.sin(cone) * Math.sin(phase);
		return dir(fwd, up, out, left);
	}

	/** A direction in body terms (forward, up, out to the arm's own side), normalised, in model space. */
	static Vec3 dir(double forward, double up, double out, boolean left) {
		// Model space: forward = -z, up = -y, the right arm's outside = -x (the left arm's = +x).
		Vec3 v = new Vec3(left ? out : -out, -up, -forward);
		return v.normalize();
	}

	/**
	 * Points an arm along {@code d} (model space) by rotating it about the shoulder: from rest (straight down, +y),
	 * xRot = asin(d.z), zRot = atan2(-d.x, d.y); blended by {@code w} over vanilla's pose.
	 */
	static void aim(ModelPart arm, float w, Vec3 d) {
		float x = (float) Math.asin(Mth.clamp(d.z, -1, 1));
		float z = (float) Math.atan2(-d.x, d.y);
		arm.xRot = Mth.lerp(w, arm.xRot, x);
		arm.yRot = Mth.lerp(w, arm.yRot, 0);
		arm.zRot = Mth.lerp(w, arm.zRot, z);
	}

	// ------------------------------------------------------------------------------------------------ whole body

	/**
	 * Whole-model rotation in the renderer (PlayerRenderer.setupRotations): the BODY_SPIN yaw spin, and lean and bank
	 * for spinning and rotor flight. Pivots at the hips so the body tips naturally. Rendering only.
	 */
	public static void applyBody(Entity entity, PoseStack pose, float age) {
		State s = STATES.get(entity.getId());
		if (s == null || s.kind == Kind.ARM_WHIRL || s.kind == Kind.SLASH || s.kind == Kind.STRIKE || s.kind == Kind.RUSH
				|| s.kind == Kind.PUNCH_EXTEND) {
			return;
		}
		if (s.kind == Kind.BOMB && s.variant != 2 && s.variant != 6 || s.kind == Kind.SMOKE && s.variant != 3) {
			return;
		}
		if (s.kind == Kind.SPRING_MOVE || s.kind == Kind.BOMB) {
			// Turn toward the travel direction and lean into it (rendering only; the camera keeps its own aim).
			float w = weight(s, age);
			Vec3 v = entity.getDeltaMovement();
			double h = Math.hypot(v.x, v.z);
			if (h > 0.05) {
				float travel = (float) Math.toDegrees(Math.atan2(-v.x, v.z));
				float want = Mth.wrapDegrees(entity.getYRot() - travel);
				s.yawOffset = Mth.lerp(0.3f, s.yawOffset, want);
			}
			s.lean = Mth.lerp(0.25f, s.lean, (float) Mth.clamp(h * 35, 0, 40));
			pose.translate(0, 0.9, 0);
			pose.mulPose(Axis.YP.rotationDegrees(s.yawOffset * w));
			pose.mulPose(Axis.XP.rotationDegrees(-s.lean * w));
			pose.translate(0, -0.9, 0);
			return;
		}
		float w = weight(s, age);
		// Lean into the direction of travel (relative to the facing), bank into turns; both smoothed.
		Vec3 v = entity.getDeltaMovement();
		double yaw = Math.toRadians(entity.getYRot());
		double forward = -v.x * Math.sin(yaw) + v.z * Math.cos(yaw);
		double sideways = -v.x * Math.cos(yaw) - v.z * Math.sin(yaw);
		float turn = Mth.wrapDegrees(entity.getYRot() - entity.yRotO);
		boolean rotor = s.kind == Kind.ROTOR || s.kind == Kind.ARM_ROTOR || s.kind == Kind.SMOKE;
		float targetLean = (float) Mth.clamp(forward * (rotor ? 70 : 30), -12, rotor ? 26 : 14);
		float targetBank = (float) Mth.clamp(-turn * 1.6 + sideways * 40, -22, 22);
		s.lean = Mth.lerp(0.15f, s.lean, targetLean);
		s.bank = Mth.lerp(0.15f, s.bank, targetBank);
		pose.translate(0, 0.9, 0);
		if (s.kind == Kind.BODY_SPIN || s.kind == Kind.DANCE) {
			pose.mulPose(Axis.YP.rotationDegrees((float) (s.spin * w)));
		}
		pose.mulPose(Axis.XP.rotationDegrees(-s.lean * w));
		pose.mulPose(Axis.ZP.rotationDegrees(s.bank * w));
		pose.translate(0, -0.9, 0);
	}

	// ------------------------------------------------------------------------------------------------ anchors

	/**
	 * Approximate world position of a hand for effect emitters: shoulder plus arm length along the same direction the
	 * model pose uses (so trails and rotor discs follow the animated arms).
	 */
	public static Vec3 hand(Entity e, float partial, boolean left) {
		State s = STATES.get(e.getId());
		float yawDeg = Mth.rotLerp(partial, e.yRotO, e.getYRot());
		if (s != null && (s.kind == Kind.BODY_SPIN || s.kind == Kind.DANCE)) {
			yawDeg -= (float) s.spin;
		}
		double yaw = Math.toRadians(yawDeg);
		Vec3 fwd = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
		Vec3 right = new Vec3(-Math.cos(yaw), 0, -Math.sin(yaw));
		Vec3 out = left ? right.scale(-1) : right;
		Vec3 shoulder = e.getPosition(partial).add(0, 1.375, 0).add(out.scale(0.32));
		if (s == null) {
			return shoulder.add(0, -0.62, 0);
		}
		double ph = s.phase + (left ? Math.PI : 0);
		Vec3 d; // body terms: forward, up, out
		switch (s.kind) {
			case ROTOR -> {
				double c = Math.toRadians(32);
				d = new Vec3(Math.sin(c) * Math.sin(ph), Math.cos(c), 0.25 + Math.sin(c) * Math.cos(ph));
			}
			case ARM_WHIRL -> {
				double c = Math.toRadians(20);
				double pitch = -Math.toRadians(e.getXRot()) * 0.5;
				d = new Vec3(Math.cos(pitch), Math.sin(pitch) + Math.sin(c) * Math.cos(ph), Math.sin(c) * Math.sin(ph) * (left ? -1 : 1) + 0.12);
			}
			case BODY_SPIN, DANCE -> d = new Vec3(0.3, 0.05, 0.95);
			case ARM_ROTOR -> d = new Vec3(0.05, 1, 0.1);
			case RUSH -> d = new Vec3(-0.45, -0.25, 0.55);
			default -> d = new Vec3(0.55, 0.05, 0.6);
		}
		d = d.normalize();
		Vec3 world = fwd.scale(d.x).add(0, d.y, 0).add(out.scale(d.z));
		return shoulder.add(world.scale(0.68));
	}
}
