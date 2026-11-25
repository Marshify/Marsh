package com.grandline.client.fx;

import com.grandline.audio.GrandLineSounds;
import com.grandline.client.audio.GrandLineAudio;
import com.grandline.client.settings.CameraShake;
import com.grandline.fruit.spring.SpringMoves;
import com.grandline.network.payload.FxEventPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Spring Fruit visuals from the server's beats (see SpringMoves): procedural spring limbs ({@link SpringLimbs},
 * {@link SpringMesh}), poses ({@link BodyMotion}), surface-aligned pressure rings in the metal material, afterimages,
 * momentum trails and speed lines whose strength follows the real speed, and for the slam a cosmetic crater, cracks,
 * terrain-coloured debris and dust (all client-side, terrain untouched). Physical, not elemental: no glow, short flashes.
 */
final class SpringFx {
	static final int EDGE = 0xE4E9EF;
	static final int COIL = 0xA9B5C0;
	static final int SHADOW = 0x687786;
	static final int PRESSURE = 0xC5DCEB;
	static final int IMPACT = 0xF5F8FC;
	static final int DEBRIS = 0xB6A48A;
	static final int SPARK = 0xFFC56D;
	static final int SPARK_COOL = 0x8A4A20;

	/** Last rebound/collision sound per caster (concurrency limit). */
	private static final Map<Integer, Float> LAST_SOUND = new HashMap<>();

	private SpringFx() {
	}

	static boolean handles(String effect) {
		return effect.startsWith("spring_");
	}

	static void handle(FxEventPayload p, ClientLevel level, Entity caster, boolean own) {
		Minecraft mc = Minecraft.getInstance();
		int id = p.caster();
		float age = caster == null ? 0 : caster.tickCount;
		RandomSource r = level.random;
		switch (p.effect()) {
			case "spring_knock" -> {
				if (caster != null) {
					BodyMotion.punch(id, age);
					SpringLimbs.knock(id, age, (float) p.range());
				}
				Vec3 hand = p.pos().add(p.dir().scale(0.6));
				GrandLineFx.add(new Ring(hand, p.dir(), 1.1, 6, PRESSURE, false));
				for (int i = FxSprites.count(5); i > 0; i--) {
					FxSprites.sprite(FxDraw.SPARK, true, hand.add(FxSprites.randomDir(r).scale(0.3)), p.dir().scale(0.9 + r.nextDouble() * 0.5),
							0.035, 0, 0, 0.8, 5, PRESSURE, SHADOW, 0.55f);
				}
				GrandLineAudio.play(GrandLineSounds.SPRING_KNOCK_CAST, p.pos(), 0.9f, 1f + r.nextFloat() * 0.08f);
			}
			case "spring_knock_hit" -> {
				boolean direct = p.size() > 0;
				GrandLineFx.add(new Ring(p.pos(), p.dir(), direct ? 1.6 : 1.1, 7, IMPACT, direct));
				GrandLineFx.add(new PhysicsSparks(level, p.pos(), p.dir().scale(-0.5).add(0, 0.4, 0), 5, 0.8, 0.22, 8, 0.04, SPARK, SPARK_COOL, true));
				GrandLineFx.add(new BladeFx.Flash(p.pos(), 0.35, 2));
				GrandLineAudio.play(GrandLineSounds.SPRING_KNOCK_IMPACT, p.pos(), 1f, 0.95f + r.nextFloat() * 0.1f);
				shake(mc, p.pos(), direct ? 0.6f : 0.35f);
			}
			case "spring_snipe_windup" -> {
				if (caster != null) {
					BodyMotion.spring(id, age, 0);
					SpringLimbs.legs(id, age, 30, false);
					SpringLimbs.pulse(id, age, 0.8f);
				}
				GrandLineAudio.play(GrandLineSounds.SPRING_SNIPE_CAST, p.pos(), 1f, 1f);
			}
			case "spring_snipe" -> {
				ground(level, p.pos(), 1.8, 8);
				GrandLineFx.add(new Ring(p.pos().add(0, 0.1, 0), new Vec3(0, 1, 0), 2.2, 8, PRESSURE, true));
				if (caster != null) {
					GrandLineFx.add(new Motion(id, SpringMoves.SNIPE_SPEED, 9));
				}
			}
			case "spring_collide" -> {
				boolean direct = p.size() > 0;
				GrandLineFx.add(new Ring(p.pos(), p.dir(), direct ? 1.8 : 1.2, 7, IMPACT, direct));
				GrandLineFx.add(new PhysicsSparks(level, p.pos(), p.dir().scale(0.4).add(0, 0.5, 0), 6, 0.8, 0.25, 9, 0.045, SPARK, SPARK_COOL, true));
				GrandLineFx.add(new BladeFx.Flash(p.pos(), 0.4, 2));
				if (soundFree(id, age)) {
					GrandLineAudio.play(GrandLineSounds.SPRING_SNIPE_IMPACT, p.pos(), 1f, 0.95f + r.nextFloat() * 0.1f);
				}
				shake(mc, p.pos(), own ? 0.5f : 0.3f);
			}
			case "spring_cannon" -> {
				if (caster != null) {
					BodyMotion.spring(id, age, 1);
					SpringLimbs.legs(id, age, 40, true);
					SpringLimbs.pulse(id, age, 0.7f);
				}
				ground(level, p.pos(), 1.5, 6);
				GrandLineAudio.play(GrandLineSounds.SPRING_CANNON_CAST, p.pos(), 1f, 1f);
			}
			case "spring_cannon_slam" -> {
				BodyMotion.finish(id, BodyMotion.Kind.SPRING_MOVE, age + 6);
				if (caster != null) {
					GrandLineFx.add(new Motion(id, 2.0, 6));
				}
			}
			case "spring_cannon_impact" -> {
				Vec3 at = p.pos();
				double radius = p.size();
				SpringLimbs.pulse(id, age, 1f);
				SpringLimbs.retract(id, age + 4);
				GrandLineFx.add(new Crater(level, at, radius * 0.55));
				GrandLineFx.add(new Ring(at.add(0, 0.12, 0), new Vec3(0, 1, 0), radius + 1.5, 12, PRESSURE, true));
				GrandLineFx.add(new Ring(at.add(0, 0.2, 0), new Vec3(0, 1, 0), radius * 0.7, 8, IMPACT, false));
				ground(level, at, radius, 18);
				GrandLineAudio.play(GrandLineSounds.SPRING_CANNON_IMPACT, at, 1.1f, 1f);
				shake(mc, at, own ? 1.4f : 1.0f);
			}
			case "spring_emperor" -> {
				if (caster != null) {
					BodyMotion.spring(id, age, 2);
					SpringLimbs.legs(id, age, (float) p.range() + 5, false);
					SpringLimbs.pulse(id, age, 0.8f);
					GrandLineFx.add(new Motion(id, SpringMoves.EMPEROR_SPEED, (int) p.range() + 2));
				}
				GrandLineAudio.play(GrandLineSounds.SPRING_SNIPE_CAST, p.pos(), 0.8f, 1.1f);
			}
			case "spring_rebound" -> {
				Vec3 n = p.dir();
				double force = Math.min(1, p.speed() / 2.0);
				if (caster != null) {
					BodyMotion.contact(id, age, n);
					SpringLimbs.pulse(id, age, (float) (0.5 + 0.5 * force));
				}
				// The ring lies on the surface that was hit: at the feet for the ground, at the side for a wall.
				Vec3 at = n.y > 0.7 ? p.pos().add(0, 0.08, 0) : p.pos().add(0, 0.9, 0).subtract(n.scale(0.35));
				GrandLineFx.add(new Ring(at, n, 1.0 + force * 1.4, 7, PRESSURE, false));
				if (n.y > 0.7) {
					ground(level, p.pos(), 1.0 + force, (int) (3 + force * 5));
				} else {
					GrandLineFx.add(new PhysicsSparks(level, at, n.add(0, 0.3, 0), (int) (2 + force * 4), 0.7, 0.18, 7, 0.035, SPARK, SPARK_COOL, true));
				}
				if (soundFree(id, age)) {
					GrandLineAudio.play(GrandLineSounds.SPRING_REBOUND, at, (float) (0.7 + 0.4 * force), (float) (0.9 + 0.25 * force + r.nextFloat() * 0.08));
				}
			}
			case "spring_leap" -> {
				if (caster != null) {
					BodyMotion.spring(id, age, 3);
					SpringLimbs.legs(id, age, SpringMoves.LEAP_MAX_TICKS + 10, false);
					SpringLimbs.pulse(id, age, 0.9f);
					Motion m = new Motion(id, SpringMoves.LEAP_MAX_SPEED, SpringMoves.LEAP_MAX_TICKS + 4);
					GrandLineFx.add(m);
					GrandLineAudio.loop(GrandLineSounds.SPRING_LEAP_LOOP, () -> {
						Entity c = m.caster(level);
						return c == null ? null : c.position();
					}, () -> !m.done && !m.ending, 0.7f, () -> 0.85 + Math.min(0.45, m.speed * 0.3), null);
				}
				ground(level, p.pos(), 1.4, 6);
				GrandLineFx.add(new Ring(p.pos().add(0, 0.1, 0), new Vec3(0, 1, 0), 1.8, 7, PRESSURE, false));
				GrandLineAudio.play(GrandLineSounds.SPRING_SNIPE_CAST, p.pos(), 0.8f, 1.15f);
			}
			case "spring_move_end" -> {
				BodyMotion.stop(id, BodyMotion.Kind.SPRING_MOVE, age);
				SpringLimbs.retract(id, age + 2);
				Motion m = GrandLineFx.find(Motion.class, id);
				if (m != null) {
					m.end();
				}
				GrandLineAudio.play(GrandLineSounds.SPRING_RETRACT, p.pos(), 0.7f, 1f);
			}
			default -> {
			}
		}
	}

	private static boolean soundFree(int id, float age) {
		Float last = LAST_SOUND.get(id);
		if (last != null && age - last < 2 && age >= last) {
			return false;
		}
		LAST_SOUND.put(id, age);
		return true;
	}

	private static void shake(Minecraft mc, Vec3 at, float degrees) {
		if (mc.player != null) {
			CameraShake.impact(mc.player.position().distanceTo(at), degrees);
		}
	}

	/** Colour of the ground under a point, blended toward the neutral debris colour. */
	static int groundColour(ClientLevel level, Vec3 at) {
		BlockPos pos = BlockPos.containing(at.x, at.y - 0.2, at.z);
		var state = level.getBlockState(pos);
		int c = state.isAir() ? DEBRIS : state.getMapColor(level, pos).col;
		return FxSprites.lerpColor(c, DEBRIS, 0.3f);
	}

	/** Ground debris (bouncing chips) and a low dust ring, coloured like the ground. */
	static void ground(ClientLevel level, Vec3 at, double radius, int count) {
		int colour = groundColour(level, at);
		GrandLineFx.add(new PhysicsSparks(level, at.add(0, 0.15, 0), new Vec3(0, 1, 0), count, 0.9, 0.3, 18, 0.06, colour,
				FxSprites.lerpColor(colour, 0x000000, 0.4f), false));
		RandomSource r = level.random;
		for (int i = FxSprites.count(count); i > 0; i--) {
			double a = r.nextDouble() * Math.PI * 2;
			Vec3 out = new Vec3(Math.cos(a), 0, Math.sin(a));
			FxSprites.sprite(FxDraw.CLOUD, false, at.add(out.scale(radius * 0.3)).add(0, 0.2, 0), out.scale(0.12 + r.nextDouble() * 0.1 * radius / 2)
					.add(0, 0.015, 0), 0.4, 0.035, -0.002, 0.88, 20 + r.nextInt(12), colour, 0xB8C1C9, 0.4f);
		}
	}

	// ------------------------------------------------------------------------------------------------ effects

	/** A pressure ring in the plane facing {@code normal} (steel material), optionally with an upright wall. */
	static final class Ring extends GrandLineFx.Effect {
		final Vec3 at, normal;
		final double radius;
		final int life, colour;
		final boolean wall;

		Ring(Vec3 at, Vec3 normal, double radius, int life, int colour, boolean wall) {
			super(-1);
			this.at = at;
			this.normal = normal.lengthSqr() < 1e-6 ? new Vec3(0, 1, 0) : normal.normalize();
			this.radius = radius;
			this.life = life;
			this.colour = colour;
			this.wall = wall;
		}

		@Override
		boolean tick(ClientLevel level) {
			return ++age < life;
		}

		@Override
		void render(FxDraw d, ClientLevel level) {
			if (at.distanceToSqr(d.cameraPosition()) > 80 * 80) {
				return;
			}
			float t = (age + d.partial) / life;
			double r = radius * WindMesh.ease(Math.sqrt(t));
			Vec3[] b = WindMesh.basis(normal);
			WindRender.with(WindRender.Material.METAL, () -> {
				WindMesh.ring(d, true, at, b[0], b[1], Math.max(0, r - 0.1 - radius * 0.1), r, 0, 360, colour, 0.5f * (1 - t), t * 0.8f, t);
				if (wall) {
					WindMesh.wall(d, false, at.subtract(0, 0.3, 0), r * 0.85, 0.7 * (1 - t), COIL, 0.22f * (1 - t), t, t);
				}
			});
		}
	}

	/** Momentum trails while a spring move travels: afterimages, foot ribbons and speed lines scaled by speed. */
	static final class Motion extends GrandLineFx.Effect {
		final double maxSpeed;
		final int life;
		final Afterimages ghosts;
		final List<Vec3> feet = new ArrayList<>();
		boolean ending;
		int endAge;
		double speed;
		Vec3 last;

		Motion(int caster, double maxSpeed, int life) {
			super(caster);
			this.maxSpeed = maxSpeed;
			this.life = life;
			this.ghosts = new Afterimages(caster, 6, COIL);
			GrandLineFx.add(ghosts);
		}

		void end() {
			ending = true;
			ghosts.stop();
		}

		@Override
		void cancel() {
			end();
		}

		@Override
		boolean tick(ClientLevel level) {
			age++;
			Entity c = caster(level);
			if (c == null || age > life + 10 || ending && ++endAge > 6) {
				ghosts.stop();
				return false;
			}
			Vec3 pos = c.position();
			speed = last == null ? 0 : pos.distanceTo(last);
			last = pos;
			if (!ending) {
				feet.add(0, pos.add(0, 0.2, 0));
				while (feet.size() > 6 + (int) (6 * WindMesh.detail())) {
					feet.remove(feet.size() - 1);
				}
				float k = (float) Math.min(1, speed / maxSpeed);
				RandomSource r = level.random;
				Vec3 back = c.getDeltaMovement().lengthSqr() > 1e-4 ? c.getDeltaMovement().normalize().scale(-1) : new Vec3(0, 0, 0);
				for (int i = FxSprites.count((int) (1 + 4 * k)); i > 0; i--) {
					Vec3 at = pos.add((r.nextDouble() - 0.5) * 1.4, 0.2 + r.nextDouble() * 1.6, (r.nextDouble() - 0.5) * 1.4);
					FxSprites.sprite(FxDraw.SPARK, true, at, back.scale(0.5 + k), 0.03 + 0.02 * k, 0, 0, 0.8, 4, PRESSURE, SHADOW, 0.35f + 0.3f * k);
				}
			} else if (!feet.isEmpty()) {
				feet.remove(feet.size() - 1);
			}
			return true;
		}

		@Override
		void render(FxDraw d, ClientLevel level) {
			Entity c = caster(level);
			if (c == null || c.position().distanceToSqr(d.cameraPosition()) > 80 * 80) {
				return;
			}
			float k = (float) Math.min(1, speed / maxSpeed);
			float fade = ending ? Math.max(0, 1 - (endAge + d.partial) / 6f) : 1;
			List<Vec3> pts = new ArrayList<>(feet);
			if (!ending) {
				pts.add(0, c.getPosition(d.partial).add(0, 0.2, 0));
			}
			int n = pts.size();
			if (n < 2) {
				return;
			}
			double[] w = new double[n];
			float[] a = new float[n];
			for (int i = 0; i < n; i++) {
				double t = i / (double) (n - 1);
				w[i] = (0.12 + 0.18 * k) * (1 - t * 0.8);
				a[i] = (float) ((0.25 + 0.45 * k) * fade * (1 - t));
			}
			d.ribbon(FxDraw.STREAK, pts, w, FxDraw.rgba(PRESSURE, 1f), a, 0);
		}
	}

	/** The cosmetic crater: a dark compressed ring of ground with cracks running out, fading away (no blocks change). */
	static final class Crater extends GrandLineFx.Effect {
		final Vec3 at;
		final double radius;
		final int colour;
		final double[] cracks;

		Crater(ClientLevel level, Vec3 at, double radius) {
			super(-1);
			this.at = at.add(0, 0.03, 0);
			this.radius = radius;
			this.colour = FxSprites.lerpColor(groundColour(level, at), 0x000000, 0.45f);
			RandomSource r = level.random;
			cracks = new double[7];
			for (int i = 0; i < cracks.length; i++) {
				cracks[i] = i * Math.PI * 2 / cracks.length + (r.nextDouble() - 0.5) * 0.6;
			}
		}

		@Override
		boolean tick(ClientLevel level) {
			return ++age < 80;
		}

		@Override
		void render(FxDraw d, ClientLevel level) {
			float t = (age + d.partial) / 80f;
			float a = t < 0.75f ? 1 : 1 - (t - 0.75f) / 0.25f;
			// The bowl: concentric dark bands, darkest in the middle.
			for (int band = 0; band < 3; band++) {
				double r0 = radius * band / 3, r1 = radius * (band + 1) / 3;
				float alpha = a * (0.55f - band * 0.15f);
				int segs = 20;
				for (int i = 0; i < segs; i++) {
					double a0 = i * Math.PI * 2 / segs, a1 = (i + 1) * Math.PI * 2 / segs;
					Vec3 p0 = at.add(Math.cos(a0) * r0, 0, Math.sin(a0) * r0), p1 = at.add(Math.cos(a1) * r0, 0, Math.sin(a1) * r0);
					Vec3 p2 = at.add(Math.cos(a1) * r1, 0, Math.sin(a1) * r1), p3 = at.add(Math.cos(a0) * r1, 0, Math.sin(a0) * r1);
					d.quadSoft(FxDraw.CLOUD, p0, p1, p2, p3, FxDraw.rgba(colour, alpha));
				}
			}
			// Cracks: thin flat strips running out from the rim.
			for (double ang : cracks) {
				Vec3 dir = new Vec3(Math.cos(ang), 0, Math.sin(ang));
				Vec3 side = new Vec3(-dir.z, 0, dir.x).scale(0.06);
				Vec3 s0 = at.add(dir.scale(radius * 0.7)), s1 = at.add(dir.scale(radius * 1.8)).add(side.scale(3));
				d.quadSoft(FxDraw.STREAK, s0.subtract(side), s1.subtract(side.scale(0.2)), s1.add(side.scale(0.2)), s0.add(side),
						FxDraw.rgba(0x2A2420, 0.7f * a));
			}
		}
	}
}
