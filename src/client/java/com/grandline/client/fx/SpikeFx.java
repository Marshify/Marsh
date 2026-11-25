package com.grandline.client.fx;

import com.grandline.GrandLineMod;
import com.grandline.audio.GrandLineSounds;
import com.grandline.client.audio.GrandLineAudio;
import com.grandline.client.settings.CameraShake;
import com.grandline.network.payload.FxEventPayload;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Spike Fruit visuals and sounds from the server's beats (see SpikeMoves): procedural ground spikes that emerge from
 * their base and sink back ({@link SpikeMesh}), fracture decals, terrain-coloured debris and dust, pressure rings,
 * forearm spikes on the animated arms with rotation trails (Whirlwind), the rolling spiked ball replacing the whole
 * player model with physically synchronised rolling, and the Spike Barrage stance. All cosmetic; terrain untouched.
 */
public final class SpikeFx {
	public static final int EDGE = 0xE3DCE5, MATERIAL = 0xBFB5C6, SHADE = 0x918798, SHADOW = 0x605969, TRAIL = 0xD6C6D9, DUST = 0xB8AF9F;
	public static final ResourceLocation CRACK = GrandLineMod.id("textures/fx/spike_crack.png");

	/** Forearm spikes: caster → {start game time, end game time (or -1)}. */
	private static final Map<Integer, long[]> ARMS = new HashMap<>();
	private static final Map<Integer, Ball> BALLS = new HashMap<>();
	private static final Map<Integer, Whirl> WHIRLS = new HashMap<>();
	private static long lastRiseSound;
	private static int risesThisTick;

	private SpikeFx() {
	}

	static boolean handles(String effect) {
		return effect.startsWith("spike_");
	}

	static long now() {
		ClientLevel l = Minecraft.getInstance().level;
		return l == null ? 0 : l.getGameTime();
	}

	/** Whether the spiked ball replaces this player's model now (the normal model, armour and items are not drawn). */
	public static boolean ballActive(int entity) {
		Ball b = BALLS.get(entity);
		return b != null && !b.done;
	}

	static void clearAll() {
		ARMS.clear();
		BALLS.clear();
		WHIRLS.clear();
	}

	static int light(ClientLevel level, Vec3 p) {
		return LevelRenderer.getLightColor(level, BlockPos.containing(p));
	}

	static void shake(Vec3 at, float deg) {
		CameraShake.impact(Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().distanceTo(at), deg);
	}

	/** Eruption sounds with a concurrency limit (at most 2 per tick, later ones quieter). */
	static void rise(GrandLineSounds.Cue cue, Vec3 at, float volume, float pitch) {
		long t = now();
		if (t != lastRiseSound) {
			lastRiseSound = t;
			risesThisTick = 0;
		}
		if (++risesThisTick <= 2) {
			GrandLineAudio.play(cue, at, volume / risesThisTick, pitch);
		}
	}

	static void handle(FxEventPayload p, ClientLevel level, Entity caster, boolean own) {
		int id = p.caster();
		float age = caster == null ? 0 : caster.tickCount;
		RandomSource r = level.random;
		switch (p.effect()) {
			case "spike_rise", "spike_rise_final" -> {
				int variant = (int) (p.size() / 100);
				double radius = p.size() - variant * 100;
				boolean big = variant == 1;
				GrandLineFx.add(new Rise(p.pos(), p.speed(), radius, (int) p.range(), r.nextFloat() * 6.28f, variant));
				GrandLineFx.add(new Crack(p.pos().add(0, 0.02, 0), radius * (big ? 3.2 : 2.2), 60 + (big ? 40 : 0)));
				SpringFx.ground(level, p.pos(), radius * 1.5, big ? 12 : variant == 2 ? 3 : 7);
				GrandLineFx.add(new PhysicsSparks(level, p.pos().add(0, 0.2, 0), new Vec3(0, 1, 0), big ? 10 : 4, 0.7, 0.3 + 0.1 * p.speed() / 4, 18, 0.07,
						SpringFx.groundColour(level, p.pos()), SHADOW, false));
				if (variant != 2) {
					GrandLineFx.add(new SpringFx.Ring(p.pos().add(0, 0.1, 0), new Vec3(0, 1, 0), radius * 3 + 1, 9, DUST, true));
				}
				if (big) {
					GrandLineFx.add(new BladeFx.Flash(p.pos().add(0, p.speed() * 0.4, 0), 0.7, 2));
					shake(p.pos(), 1.4f);
				}
				rise(p.effect().equals("spike_rise_final") ? GrandLineSounds.SPIKE_RUPTURE_FINAL : big ? GrandLineSounds.SPIKE_RISE_HEAVY
						: variant == 2 ? GrandLineSounds.SPIKE_RUPTURE : GrandLineSounds.SPIKE_RISE, p.pos(), variant == 2 ? 0.6f : 1f,
						0.9f + r.nextFloat() * 0.2f + (float) (0.25 - radius * 0.15));
			}
			case "spike_summon_cast" -> {
				BodyMotion.spike(id, age, 0);
				if (caster != null) {
					long start = now();
					GrandLineAudio.loop(GrandLineSounds.SPIKE_RUMBLE, () -> caster.position(), () -> now() - start < 30 && caster.isAlive(), 0.6f, () -> 1.0,
							null);
				}
			}
			case "spike_summon_aim" -> GrandLineFx.add(new Aim(p.pos(), p.size(), 3));
			case "spike_summon_release" -> BodyMotion.release(id, age);
			case "spike_summon_impact" -> {
				for (int i = FxSprites.count(4 + (int) (8 * p.speed())); i > 0; i--) {
					FxSprites.sprite(FxDraw.CLOUD, false, p.pos().add(FxSprites.randomDir(r).multiply(1, 0.2, 1).scale(p.size() * 0.6)).add(0, 0.3, 0),
							FxSprites.randomDir(r).multiply(1, 0.3, 1).scale(0.08).add(0, 0.03, 0), 0.8, 0.05, -0.002, 0.9, 30, DUST, 0xD8D0C2, 0.4f);
				}
			}
			case "spike_whirl" -> {
				BodyMotion.start(id, BodyMotion.Kind.BODY_SPIN, age);
				ARMS.put(id, new long[]{now(), -1});
				GrandLineAudio.play(GrandLineSounds.SPIKE_ARMS, p.pos().add(0, 1, 0), 1f, 1f);
				SpringFx.ground(level, p.pos(), 1.5, 8);
				if (caster != null) {
					Whirl w = new Whirl(id);
					Whirl old = WHIRLS.put(id, w);
					if (old != null) {
						old.done = true;
					}
					GrandLineFx.add(w);
					GrandLineAudio.loop(GrandLineSounds.SPIKE_WHIRL_LOOP, () -> caster.position().add(0, 1, 0), () -> !w.done && w.end < 0, 0.8f,
							() -> 0.85 + 0.35 * w.intensity, null);
				}
			}
			case "spike_whirl_tick" -> {
				Whirl w = WHIRLS.get(id);
				if (w != null) {
					w.radius = p.size();
				}
			}
			case "spike_whirl_hit" -> {
				GrandLineFx.add(new BladeFx.Flash(p.pos(), 0.3, 2));
				GrandLineFx.add(new PhysicsSparks(level, p.pos(), p.dir(), 4, 0.6, 0.25, 8, 0.04, EDGE, SHADOW, false));
				rise(GrandLineSounds.SPIKE_STAB, p.pos(), 0.7f, 0.9f + r.nextFloat() * 0.3f);
			}
			case "spike_whirl_end" -> {
				BodyMotion.stop(id, BodyMotion.Kind.BODY_SPIN, age);
				long[] a = ARMS.get(id);
				if (a != null) {
					a[1] = now();
				}
				Whirl w = WHIRLS.remove(id);
				if (w != null) {
					w.end = w.age;
				}
			}
			case "spike_ball" -> {
				Ball old = BALLS.remove(id);
				if (old != null) {
					old.done = true;
				}
				Ball b = new Ball(id, (float) p.size() / 2);
				BALLS.put(id, b);
				GrandLineFx.add(b);
				GrandLineAudio.play(GrandLineSounds.SPIKE_BALL_START, p.pos().add(0, 1, 0), 1f, 1f);
				SpringFx.ground(level, p.pos(), 1.5, 8);
				if (caster != null) {
					GrandLineAudio.loop(GrandLineSounds.SPIKE_BALL_LOOP, () -> caster.position(), () -> !b.done && !b.ending, 0.8f,
							() -> 0.7 + 0.5 * Math.min(1, b.speed / 0.35), null);
				}
			}
			case "spike_ball_hit" -> {
				GrandLineFx.add(new BladeFx.Flash(p.pos(), p.speed() > 0 ? 0.3 : 0.5, 2));
				GrandLineFx.add(new PhysicsSparks(level, p.pos(), p.dir().add(0, 0.5, 0), 5, 0.7, 0.3, 10, 0.05, EDGE, SHADOW, false));
				rise(GrandLineSounds.SPIKE_BALL_HIT, p.pos(), p.speed() > 0 ? 0.6f : 1f, 0.95f + r.nextFloat() * 0.15f);
				if (own) {
					shake(p.pos(), p.speed() > 0 ? 0.3f : 0.7f);
				}
			}
			case "spike_ball_jump" -> SpringFx.ground(level, p.pos(), 1.0, 4);
			case "spike_ball_land" -> {
				SpringFx.ground(level, p.pos(), 1.6, 8);
				GrandLineFx.add(new SpringFx.Ring(p.pos().add(0, 0.1, 0), new Vec3(0, 1, 0), 2.5, 8, DUST, false));
				rise(GrandLineSounds.SPIKE_BALL_HIT, p.pos(), 0.6f, 0.8f);
			}
			case "spike_ball_end" -> {
				Ball b = BALLS.get(id);
				if (b != null) {
					b.end();
				}
				GrandLineAudio.play(GrandLineSounds.SPIKE_BALL_END, p.pos().add(0, 1, 0), 1f, 1f);
				SpringFx.ground(level, p.pos(), 1.2, 6);
			}
			case "spike_barrage_cast" -> {
				BodyMotion.spike(id, age, 1);
				if (caster != null) {
					long start = now();
					GrandLineAudio.loop(GrandLineSounds.SPIKE_TENSION, () -> caster.position(), () -> now() - start < 62 && BodyMotion.active(id, BodyMotion.Kind.SPIKE)
							&& caster.isAlive(), 0.7f, () -> 0.9 + 0.3 * Math.min(1, (now() - start) / 60.0), null);
				}
			}
			case "spike_barrage_charge" -> {
				if (p.speed() > 0.1) {
					SpringFx.ground(level, p.pos(), 0.6 + p.speed(), 2);
				}
			}
			case "spike_barrage_release" -> BodyMotion.release(id, age);
			case "spike_barrage_stop" -> SpringFx.ground(level, p.pos(), 0.8, 4);
			default -> { }
		}
	}

	// ------------------------------------------------------------------------------------------------ body layer

	/** Forearm spikes on both arms (third person, in arm model space); they grow out and retract smoothly. */
	public static void drawLayer(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player, PlayerModel<AbstractClientPlayer> model,
			float age) {
		float k = armSpikes(player.getId());
		if (k <= 0) {
			return;
		}
		for (boolean left : new boolean[]{false, true}) {
			pose.pushPose();
			(left ? model.leftArm : model.rightArm).translateAndRotate(pose);
			drawArm(pose, buffers, light, k, left);
			pose.popPose();
		}
	}

	/** First person: the same spikes on the visible arm. */
	public static void drawHand(PoseStack pose, MultiBufferSource buffers, int light, int id, boolean left) {
		float k = armSpikes(id);
		if (k > 0) {
			drawArm(pose, buffers, light, k, left);
		}
	}

	static float armSpikes(int id) {
		long[] a = ARMS.get(id);
		if (a == null) {
			return 0;
		}
		float t = now() + Minecraft.getInstance().getFrameTime() - a[0];
		float grow = Mth.clamp(t / 4f, 0, 1);
		if (a[1] >= 0) {
			float out = (now() + Minecraft.getInstance().getFrameTime() - a[1]) / 6f;
			if (out >= 1) {
				ARMS.remove(id);
				return 0;
			}
			grow *= 1 - out;
		}
		return (float) WindMesh.ease(grow);
	}

	/** Three spikes along the outer forearm and one past the fist (arm space: +Y down the arm, -X outward for the right arm). */
	static void drawArm(PoseStack pose, MultiBufferSource buffers, int light, float k, boolean left) {
		float side = left ? 1 : -1;
		for (int i = 0; i < 3; i++) {
			pose.pushPose();
			pose.translate(side * 0.2, 0.25 + i * 0.16, 0);
			pose.mulPose(Axis.ZP.rotationDegrees(side * -80 + i * side * 8));
			SpikeMesh.spike(pose, buffers, light, (0.35f + 0.08f * i) * k, 0.07f, SpikeMesh.facets(), i);
			pose.popPose();
		}
		pose.pushPose();
		pose.translate(side * 0.06, 0.68, 0);
		SpikeMesh.spike(pose, buffers, light, 0.5f * k, 0.09f, SpikeMesh.facets(), 0.3f);
		pose.popPose();
	}

	// ------------------------------------------------------------------------------------------------ effects

	/** A ground spike: emerges from its base (not scaled from the middle), holds, then sinks back into the ground. */
	static final class Rise extends GrandLineFx.Effect {
		final Vec3 base;
		final double height, radius;
		final int life, variant;
		final float twist;
		final float lean;

		Rise(Vec3 base, double height, double radius, int life, float twist, int variant) {
			super(-1);
			this.base = base;
			this.height = height;
			this.radius = radius;
			this.life = life;
			this.twist = twist;
			this.variant = variant;
			this.lean = (float) ((Math.random() - 0.5) * (variant == 2 ? 24 : 8));
		}

		@Override
		boolean tick(ClientLevel level) {
			return ++age < life;
		}

		@Override
		void render(FxDraw d, ClientLevel level) {
			if (base.distanceToSqr(d.cameraPosition()) > 96 * 96) {
				return;
			}
			float a = age + d.partial;
			// Rise: fast with a slight overshoot; sink over the last 8 ticks.
			double ext = a < 3 ? easeOutBack(a / 3) : a > life - 8 ? Math.max(0, (life - a) / 8) : 1;
			PoseStack ps = d.pose;
			ps.pushPose();
			ps.translate(base.x - d.camPos.x, base.y - d.camPos.y - (1 - ext) * height, base.z - d.camPos.z);
			ps.mulPose(Axis.YP.rotation(twist));
			ps.mulPose(Axis.ZP.rotationDegrees(lean));
			int facets = base.distanceToSqr(d.cameraPosition()) > 40 * 40 ? 4 : SpikeMesh.facets();
			SpikeMesh.spike(ps, d.buffers, light(level, base.add(0, 0.5, 0)), (float) height, (float) radius, facets, 0);
			ps.popPose();
		}

		static double easeOutBack(double t) {
			double c1 = 1.4, c3 = c1 + 1;
			return 1 + c3 * Math.pow(t - 1, 3) + c1 * Math.pow(t - 1, 2);
		}
	}

	/** A temporary ground fracture decal, fading out (no blocks change). */
	static final class Crack extends GrandLineFx.Effect {
		final Vec3 at;
		final double radius;
		final int life;
		final float roll = (float) (Math.random() * Math.PI * 2);

		Crack(Vec3 at, double radius, int life) {
			super(-1);
			this.at = at;
			this.radius = radius;
			this.life = life;
		}

		@Override
		boolean tick(ClientLevel level) {
			return ++age < life;
		}

		@Override
		void render(FxDraw d, ClientLevel level) {
			if (at.distanceToSqr(d.cameraPosition()) > 64 * 64) {
				return;
			}
			float t = (age + d.partial) / life;
			float a = t < 0.7f ? 0.9f : 0.9f * (1 - (t - 0.7f) / 0.3f);
			double grow = Math.min(1, (age + d.partial) / 3) * radius;
			double c = Math.cos(roll) * grow, s = Math.sin(roll) * grow;
			d.quadSoft(CRACK, at.add(-c + s, 0, -s - c), at.add(c + s, 0, s - c), at.add(c - s, 0, s + c), at.add(-c - s, 0, -s + c), new float[]{1, 1, 1, a});
		}
	}

	/** Spike Summon's charging marker: a ground ring the size of the coming eruption. */
	static final class Aim extends GrandLineFx.Effect {
		final Vec3 at;
		final double radius;
		final int life;

		Aim(Vec3 at, double radius, int life) {
			super(-1);
			this.at = at.add(0, 0.05, 0);
			this.radius = radius;
			this.life = life;
		}

		@Override
		boolean tick(ClientLevel level) {
			return ++age < life;
		}

		@Override
		void render(FxDraw d, ClientLevel level) {
			Vec3[] b = WindMesh.basis(new Vec3(0, 1, 0));
			float a = 1 - (age + d.partial) / life;
			WindRender.with(WindRender.Material.METAL, () -> WindMesh.ring(d, false, at, b[0], b[1], radius - 0.12, radius, 0, 360, TRAIL, 0.55f * a, 0, 0));
			d.quadSoft(CRACK, at.add(-0.6, 0, -0.6), at.add(0.6, 0, -0.6), at.add(0.6, 0, 0.6), at.add(-0.6, 0, 0.6), new float[]{1, 1, 1, 0.5f * a});
		}
	}

	/** Whirlwind: rotation trails following the real fist positions, a reach ring that matches the damage radius, dust. */
	static final class Whirl extends GrandLineFx.Effect {
		final List<Vec3> left = new ArrayList<>(), right = new ArrayList<>();
		double radius = 2.5;
		int end = -1;
		float intensity;

		Whirl(int caster) {
			super(caster);
		}

		@Override
		boolean tick(ClientLevel level) {
			age++;
			Entity e = caster(level);
			if (e == null || end >= 0 && age - end > 8 || age > 80) {
				return false;
			}
			intensity = BodyMotion.intensity(caster, e.tickCount);
			if (end < 0) {
				left.add(0, BodyMotion.hand(e, 1, true));
				right.add(0, BodyMotion.hand(e, 1, false));
			}
			int keep = 4 + (int) (4 * WindMesh.detail());
			while (left.size() > keep) {
				left.remove(left.size() - 1);
				right.remove(right.size() - 1);
			}
			if (end >= 0 && !left.isEmpty()) {
				left.remove(left.size() - 1);
				right.remove(right.size() - 1);
			}
			if (age % 3 == 0 && end < 0) {
				SpringFx.ground(level, e.position(), radius * 0.6, 1);
			}
			return true;
		}

		@Override
		void render(FxDraw d, ClientLevel level) {
			Entity e = caster(level);
			if (e == null) {
				return;
			}
			float fade = end < 0 ? 1 : Math.max(0, 1 - (age + d.partial - end) / 8f);
			for (List<Vec3> hist : List.of(left, right)) {
				if (hist.size() < 2) {
					continue;
				}
				double[] w = new double[hist.size()];
				float[] al = new float[hist.size()];
				for (int i = 0; i < w.length; i++) {
					double k = i / (w.length - 1.0);
					w[i] = (0.22 + 0.2 * intensity) * (1 - 0.7 * k);
					al[i] = (float) (0.55 * (1 - k)) * fade * (0.4f + 0.6f * intensity);
				}
				d.ribbonSoft(FxDraw.STREAK, hist, w, FxDraw.rgba(TRAIL, 1), al, (age + d.partial) * 0.2f);
			}
			// The reach ring: exactly the damaging radius.
			Vec3 at = e.getPosition(d.partial).add(0, 0.9, 0);
			Vec3[] b = WindMesh.basis(new Vec3(0, 1, 0));
			double r = radius;
			WindRender.with(WindRender.Material.METAL, () -> WindMesh.ring(d, false, at, b[0], b[1], r - 0.35, r, (age + d.partial) * 40, 300, TRAIL,
					0.3f * fade * intensity, 0.2f, (age + d.partial) * 0.3));
		}
	}

	/** Spiky Ball: replaces the whole player model; rolls about the axis perpendicular to its real motion. */
	static final class Ball extends GrandLineFx.Effect {
		final float radius;
		final Quaternionf spin = new Quaternionf();
		final List<Vec3> trail = new ArrayList<>();
		Vec3 last;
		double speed;
		boolean ending;
		int endAge;
		float grow;

		Ball(int caster, float radius) {
			super(caster);
			this.radius = radius;
		}

		void end() {
			ending = true;
		}

		@Override
		void cancel() {
			done = true;
			BALLS.remove(caster);
		}

		@Override
		boolean tick(ClientLevel level) {
			age++;
			Entity e = caster(level);
			if (e == null || age > 120) {
				BALLS.remove(caster);
				return false;
			}
			if (ending && ++endAge > 6) {
				BALLS.remove(caster);
				return false;
			}
			Vec3 pos = e.position();
			if (last != null) {
				Vec3 delta = pos.subtract(last);
				Vec3 flat = new Vec3(delta.x, 0, delta.z);
				speed = flat.length();
				if (speed > 1e-3) {
					// Roll: rotate about the axis perpendicular to the motion, by distance / radius.
					Vector3f axis = new Vector3f((float) flat.z, 0, (float) -flat.x).normalize();
					spin.premul(new Quaternionf().rotateAxis((float) (speed / radius), axis));
				}
				if (e.onGround() && speed > 0.08 && age % 2 == 0) {
					FxSprites.sprite(FxDraw.CLOUD, false, pos.add(0, 0.15, 0), flat.scale(-0.3).add(0, 0.02, 0), 0.6, 0.04, -0.001, 0.9, 20,
							SpringFx.groundColour(level, pos), DUST, 0.35f);
				}
			}
			last = pos;
			trail.add(0, pos.add(0, 0.15, 0));
			while (trail.size() > 6 + (int) (4 * WindMesh.detail())) {
				trail.remove(trail.size() - 1);
			}
			return true;
		}

		@Override
		void render(FxDraw d, ClientLevel level) {
			Entity e = caster(level);
			if (e == null) {
				return;
			}
			float a = age + d.partial;
			grow = ending ? Math.max(0, 1 - (endAge + d.partial) / 6f) : (float) WindMesh.ease(Math.min(1, a / 5f));
			float r = radius * (0.55f + 0.45f * grow);
			Vec3 p = e.getPosition(d.partial).add(0, r, 0);
			PoseStack ps = d.pose;
			ps.pushPose();
			ps.translate(p.x - d.camPos.x, p.y - d.camPos.y, p.z - d.camPos.z);
			ps.mulPose(spin);
			SpikeMesh.ball(ps, d.buffers, light(level, p), r * 0.62f, r * 0.55f, grow);
			ps.popPose();
			if (trail.size() > 1 && speed > 0.05) {
				List<Vec3> pts = new ArrayList<>(trail);
				double[] w = new double[pts.size()];
				float[] al = new float[pts.size()];
				for (int i = 0; i < w.length; i++) {
					double k = i / (w.length - 1.0);
					w[i] = 0.8 * (1 - 0.6 * k);
					al[i] = (float) (Math.min(1, speed / 0.35) * 0.35 * (1 - k));
				}
				d.ribbonSoft(FxDraw.STREAK, pts, w, FxDraw.rgba(TRAIL, 1), al, a * 0.2f);
			}
		}
	}
}
