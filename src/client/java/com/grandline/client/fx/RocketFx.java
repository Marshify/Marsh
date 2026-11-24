package com.grandline.client.fx;

import com.grandline.GrandLineMod;
import com.grandline.audio.GrandLineSounds;
import com.grandline.client.audio.GrandLineAudio;
import com.grandline.client.settings.CameraShake;
import com.grandline.client.settings.ClientSettings;
import com.grandline.fruit.rocket.RocketMoves;
import com.grandline.network.payload.FxEventPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Rocket Fruit visuals, drawn from the server's beats (see RocketMoves). Layers, from the core out:
 * <ul>
 *   <li>a real 3D rocket model (hull, nose cone, fins, nozzle; shaded per face) built from quads every frame;</li>
 *   <li>an additive white-hot exhaust core and flame tongues at the nozzle, flickering;</li>
 *   <li>a smoke ribbon trail that thins and fades with age;</li>
 *   <li>explosions: a flash, an expanding fireball, a ground shockwave ring, sparks, debris embers and rolling smoke;</li>
 *   <li>camera shake by distance.</li>
 * </ul>
 * Secondary detail (sparks, smoke puffs, ribbon length) follows the effect quality preset (Low, Medium, High, Ultra);
 * the rocket, its exhaust core and the blast shapes always show. Everything here uses the vanilla render pipeline
 * (no custom shader programs), so it draws the same with or without shader packs.
 */
final class RocketFx {
	static final ResourceLocation HULL = GrandLineMod.id("textures/fx/rocket_hull.png");
	static final int WHITE = 0xFFF6E0;
	static final int YELLOW = 0xFFD04A;
	static final int ORANGE = 0xFF7A1E;
	static final int RED = 0xD8301A;
	static final int SMOKE = 0x3A3533;
	static final int HULL_RGB = 0xE8E6E2;
	static final int PAINT = 0xD94A3A; // the fruit's colour: nose and fins
	static final int NOZZLE = 0x3A3A40;

	private RocketFx() {
	}

	static boolean handles(String effect) {
		return effect.startsWith("rocket_") || effect.equals("flight_end");
	}

	/** Ribbon samples kept per trail at the current quality preset. */
	static int trailLength() {
		return switch (ClientSettings.get().fxDensity) {
			case 0 -> 6;
			case 1 -> 10;
			case 2 -> 16;
			default -> 24;
		};
	}

	// ------------------------------------------------------------------------------------------------ beats

	static void handle(FxEventPayload p, ClientLevel level, Entity caster, boolean own) {
		Minecraft mc = Minecraft.getInstance();
		int id = p.caster();
		float age = caster == null ? 0 : caster.tickCount;
		switch (p.effect()) {
			case "rocket_fist_windup" -> {
				if (caster != null) {
					MovePoses.windup(id, MovePoses.Kind.READY, age);
				}
				GrandLineAudio.play(GrandLineSounds.ROCKET_CRASH_CHARGE, p.pos(), 0.5f, 1.4f);
			}
			case "rocket_fist" -> {
				if (caster != null) {
					MovePoses.release(id, MovePoses.Kind.PUNCH, age, false);
				}
				if (own && caster != null) {
					mc.player.swing(InteractionHand.MAIN_HAND);
				}
				Missile m = new Missile(id, p.pos(), p.dir(), p.speed(), p.range(), 0.75);
				GrandLineFx.add(m);
				muzzle(level, p.pos(), p.dir(), 8);
				GrandLineAudio.play(GrandLineSounds.ROCKET_FIST_CAST, p.pos(), 1f, 1f);
				GrandLineAudio.loop(GrandLineSounds.ROCKET_FIST_TRAVEL, m::position, () -> !m.done, 0.6f, () -> 1.0, null);
			}
			case "rocket_fist_hit" -> {
				GrandLineFx.endProjectile(id, p.pos());
				int kind = (int) p.size();
				GrandLineFx.add(new Explosion(p.pos(), kind == 0 ? 0.8 : RocketMoves.FIST_BLAST, kind == 0 ? 8 : 14));
				GrandLineAudio.play(GrandLineSounds.ROCKET_FIST_IMPACT, p.pos(), kind == 0 ? 0.5f : 1f, kind == 0 ? 1.3f : 1f);
				if (kind > 0) {
					shake(mc, p.pos(), own ? 1.2f : 0.8f);
				}
			}
			case "rocket_strike_mark" -> {
				if (caster != null) {
					MovePoses.windup(id, MovePoses.Kind.RAISE, age);
				}
				int life = RocketMoves.STRIKE_WINDUP_TICKS + RocketMoves.STRIKE_INTERVAL * (RocketMoves.STRIKE_ROCKETS - 1)
						+ RocketMoves.STRIKE_FLIGHT_TICKS + 2;
				GrandLineFx.add(new Marker(id, p.pos(), p.size(), life));
				GrandLineAudio.play(GrandLineSounds.ROCKET_STRIKE_CAST, p.pos(), 0.9f, 1f);
			}
			case "rocket_strike_launch" -> {
				Vec3 spot = p.pos().add(p.dir());
				StrikeRocket r = new StrikeRocket(id, p.pos(), spot, (int) p.speed());
				GrandLineFx.add(r);
				muzzle(level, p.pos(), new Vec3(0, 1, 0), 4);
				GrandLineAudio.play(GrandLineSounds.ROCKET_STRIKE_CAST, p.pos(), 0.6f, 1.1f + (float) p.size() * 0.06f);
				// The whistle of the dive starts as the rocket turns over.
				GrandLineFx.add(new Delayed((int) p.speed() / 2, () -> GrandLineAudio.play(GrandLineSounds.ROCKET_STRIKE_TRAVEL, spot, 0.8f, 1f)));
			}
			case "rocket_strike_hit" -> {
				GrandLineFx.add(new Explosion(p.pos(), RocketMoves.STRIKE_BLAST, 14));
				GrandLineAudio.play(GrandLineSounds.ROCKET_STRIKE_IMPACT, p.pos(), 1f, 0.9f + (float) p.size() * 0.05f);
				shake(mc, p.pos(), own ? 0.9f : 0.7f);
			}
			case "rocket_crash_windup" -> {
				if (caster != null) {
					MovePoses.windup(id, MovePoses.Kind.PLANT, age);
					GrandLineFx.add(new Thrust(id, RocketMoves.CRASH_WINDUP_TICKS + RocketMoves.CRASH_MAX_TICKS + 2, true));
				}
				GrandLineAudio.play(GrandLineSounds.ROCKET_CRASH_CHARGE, p.pos(), 1f, 1f);
			}
			case "rocket_crash_launch" -> {
				if (caster != null) {
					MovePoses.release(id, MovePoses.Kind.HEAVY, age, false);
				}
				GrandLineFx.add(new Ring(p.pos(), 2.4, 10, false));
				GrandLineAudio.play(GrandLineSounds.ROCKET_CRASH_CAST, p.pos(), 1f, 1f);
				shake(mc, p.pos(), own ? 0.8f : 0.4f);
			}
			case "rocket_crash_impact", "rocket_crash_end" -> {
				MovePoses.clear(id, MovePoses.Kind.HEAVY);
				Thrust t = GrandLineFx.find(Thrust.class, id);
				if (t != null) {
					t.ending = true;
				}
				if (p.effect().equals("rocket_crash_impact")) {
					GrandLineFx.add(new Explosion(p.pos(), p.size(), 20));
					GrandLineFx.add(new Ring(p.pos(), p.size() * 1.5, 14, true));
					GrandLineAudio.play(GrandLineSounds.ROCKET_CRASH_IMPACT, p.pos(), 1.2f, 0.95f);
					shake(mc, p.pos(), own ? 2.2f : 1.6f);
				}
			}
			case "rocket_flight_start" -> {
				if (caster != null) {
					MovePoses.windup(id, MovePoses.Kind.FLIGHT, age);
					Thrust flight = new Thrust(id, Integer.MAX_VALUE, false);
					GrandLineFx.add(flight);
					GrandLineAudio.loop(GrandLineSounds.ROCKET_FLIGHT_TRAVEL, () -> {
						Entity c = flight.caster(level);
						return c == null ? null : c.position();
					}, () -> !flight.done && !flight.ending, 0.8f, () -> {
						Entity c = flight.caster(level);
						return c == null ? 1.0 : 0.9 + Math.min(0.3, c.getDeltaMovement().length() * 0.4);
					}, GrandLineSounds.ROCKET_FLIGHT_AFTERMATH);
				}
				GrandLineFx.add(new Ring(p.pos(), 2.0, 10, false));
				GrandLineAudio.play(GrandLineSounds.ROCKET_FLIGHT_CAST, p.pos(), 1f, 1.05f);
			}
			case "flight_end" -> {
				Thrust flight = GrandLineFx.find(Thrust.class, id);
				if (flight != null && !flight.crash) {
					flight.ending = true;
				}
			}
			default -> {
			}
		}
	}

	private static void shake(Minecraft mc, Vec3 at, float degrees) {
		if (mc.player != null) {
			CameraShake.impact(mc.player.position().distanceTo(at), degrees);
		}
	}

	/** Launch flash: sparks and a puff of smoke thrown along {@code dir}. */
	private static void muzzle(ClientLevel level, Vec3 at, Vec3 dir, int base) {
		RandomSource r = level.random;
		Vec3 d = dir.normalize();
		for (int i = FxSprites.count(base); i > 0; i--) {
			FxSprites.spark(at, d.scale(0.2 + r.nextDouble() * 0.3).add(FxSprites.randomDir(r).scale(0.08)), 0.07, 5 + r.nextInt(4), WHITE, ORANGE);
		}
		for (int i = FxSprites.count(base / 2); i > 0; i--) {
			FxSprites.smoke(at, d.scale(-0.04).add(FxSprites.randomDir(r).scale(0.03)), 0.35, 18 + r.nextInt(10), SMOKE);
		}
	}

	// ------------------------------------------------------------------------------------------------ the model

	/**
	 * Draws the rocket: a {@code SIDES}-sided hull, a cone nose, four fins and a dark nozzle, nose along {@code dir},
	 * {@code scale} blocks long. Faces are shaded by a fixed light so the shape reads in any lighting.
	 */
	static void drawRocket(FxDraw d, Vec3 tail, Vec3 dir, double scale, double roll) {
		final int sides = 8;
		Vec3 f = dir.normalize();
		Vec3 a = Math.abs(f.y) > 0.95 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
		Vec3 u = f.cross(a).normalize();
		Vec3 v = f.cross(u).normalize();
		double rad = 0.11 * scale, body = 0.62 * scale, nose = 0.28 * scale;
		Vec3 noseBase = tail.add(f.scale(body)), tip = noseBase.add(f.scale(nose));
		for (int i = 0; i < sides; i++) {
			double t0 = roll + i * Math.PI * 2 / sides, t1 = roll + (i + 1) * Math.PI * 2 / sides;
			Vec3 o0 = u.scale(Math.cos(t0) * rad).add(v.scale(Math.sin(t0) * rad));
			Vec3 o1 = u.scale(Math.cos(t1) * rad).add(v.scale(Math.sin(t1) * rad));
			Vec3 n = o0.add(o1).normalize();
			float uu0 = i / (float) sides, uu1 = (i + 1) / (float) sides;
			// Hull side.
			d.quad(HULL, false, tail.add(o0), tail.add(o1), noseBase.add(o1), noseBase.add(o0), shade(HULL_RGB, n), uu0, uu1, 0, 0.75f);
			// Nose cone (a quad collapsed to the tip).
			Vec3 nn = n.add(f.scale(0.6)).normalize();
			d.quad(HULL, false, noseBase.add(o0), noseBase.add(o1), tip, tip, shade(PAINT, nn), uu0, uu1, 0.75f, 1f);
			// Nozzle bell: a short flared ring behind the hull.
			Vec3 b0 = o0.scale(1.25).subtract(f.scale(0.08 * scale)), b1 = o1.scale(1.25).subtract(f.scale(0.08 * scale));
			d.quad(HULL, false, tail.add(b0), tail.add(b1), tail.add(o1.scale(0.9)), tail.add(o0.scale(0.9)), shade(NOZZLE, n.scale(-1)), uu0, uu1, 0, 0.2f);
		}
		// Four fins at the tail.
		for (int k = 0; k < 4; k++) {
			double t = roll + Math.PI / 4 + k * Math.PI / 2;
			Vec3 out = u.scale(Math.cos(t)).add(v.scale(Math.sin(t)));
			Vec3 root0 = tail.add(out.scale(rad)), root1 = root0.add(f.scale(0.22 * scale));
			Vec3 tip0 = tail.add(out.scale(rad + 0.13 * scale)).subtract(f.scale(0.05 * scale)), tip1 = tip0.add(f.scale(0.1 * scale));
			d.quad(HULL, false, root0, tip0, tip1, root1, shade(PAINT, out.cross(f)), 0, 0.2f, 0, 0.2f);
		}
	}

	/** Colour lit from a fixed sun direction (top front), so hull faces stay readable at night and under shaders. */
	static float[] shade(int rgb, Vec3 normal) {
		Vec3 light = new Vec3(0.35, 0.85, 0.4).normalize();
		float k = (float) (0.55 + 0.45 * Math.max(0, normal.normalize().dot(light)));
		float[] c = FxDraw.rgba(rgb, 1f);
		return new float[]{c[0] * k, c[1] * k, c[2] * k, 1f};
	}

	/** The exhaust at a nozzle pointing along {@code back}: white core, flickering flame tongues and a soft glow. */
	static void drawExhaust(FxDraw d, Vec3 nozzle, Vec3 back, double size, int age, float alpha) {
		double flick = 0.85 + 0.15 * Math.sin(age * 2.7);
		d.billboard(FxDraw.GLOW, true, nozzle, 0.55 * size * flick, 0, FxDraw.rgba(ORANGE, 0.55f * alpha));
		d.billboard(FxDraw.GLOW, true, nozzle, 0.22 * size, 0, FxDraw.rgba(WHITE, 0.95f * alpha));
		for (int i = 0; i < 3; i++) {
			Vec3 p = nozzle.add(back.scale((0.18 + i * 0.16) * size));
			d.flame(p, (0.26 - i * 0.05) * size * flick, age * 0.4 + i, (age + i) & 3, FxDraw.rgba(i == 0 ? YELLOW : i == 1 ? ORANGE : RED, (0.9f - i * 0.2f) * alpha));
		}
	}

	/** A smoke ribbon through {@code points} (newest first), thinning and fading toward the tail. */
	static void drawTrail(FxDraw d, List<Vec3> points, double width, float alpha) {
		if (points.size() < 2) {
			return;
		}
		int n = points.size();
		double[] widths = new double[n];
		float[] alphas = new float[n];
		for (int i = 0; i < n; i++) {
			double t = i / (double) (n - 1);
			widths[i] = width * (0.4 + t * 1.2);
			alphas[i] = (float) ((1 - t) * 0.55 * alpha);
		}
		d.ribbonSoft(FxDraw.CLOUD, points, widths, FxDraw.rgba(0x8A8480, 1f), alphas, 0);
		float[] hot = new float[n];
		double[] hw = new double[n];
		for (int i = 0; i < n; i++) {
			double t = i / (double) (n - 1);
			hot[i] = (float) (Math.max(0, 1 - t * 3) * 0.8 * alpha);
			hw[i] = width * 0.35;
		}
		d.ribbon(FxDraw.STREAK, points, hw, FxDraw.rgba(ORANGE, 1f), hot, 0);
	}

	// ------------------------------------------------------------------------------------------------ effects

	/** Z: the fist-rocket, flying straight until the server's impact beat. */
	static final class Missile extends GrandLineFx.Effect implements GrandLineFx.Projectile {
		final Vec3 start, dir;
		final double speed, range, scale;
		final List<Vec3> trail = new ArrayList<>();
		Vec3 pos, prev;

		Missile(int caster, Vec3 start, Vec3 dir, double speed, double range, double scale) {
			super(caster);
			this.start = start;
			this.dir = dir.normalize();
			this.speed = speed;
			this.range = range;
			this.scale = scale;
			this.pos = start;
			this.prev = start;
		}

		@Override
		boolean followsCaster() {
			return false;
		}

		@Override
		public Vec3 position() {
			return pos;
		}

		@Override
		public void end(Vec3 at) {
			pos = at;
			done = true;
		}

		@Override
		boolean tick(ClientLevel level) {
			prev = pos;
			pos = pos.add(dir.scale(speed));
			trail.add(0, pos.subtract(dir.scale(0.4 * scale)));
			while (trail.size() > trailLength()) {
				trail.remove(trail.size() - 1);
			}
			if (level.random.nextFloat() < 0.6f) {
				FxSprites.smoke(pos.subtract(dir.scale(0.5)), FxSprites.randomDir(level.random).scale(0.015), 0.22, 14, SMOKE);
			}
			return ++age * speed < range + 2;
		}

		@Override
		void render(FxDraw d, ClientLevel level) {
			Vec3 at = prev.lerp(pos, d.partial);
			Vec3 tail = at.subtract(dir.scale(0.9 * scale * 0.5));
			List<Vec3> pts = new ArrayList<>(trail);
			pts.add(0, tail);
			drawTrail(d, pts, 0.22 * scale, 1f);
			drawExhaust(d, tail.subtract(dir.scale(0.05)), dir.scale(-1), 0.8 * scale, age, 1f);
			drawRocket(d, tail, dir, scale, age * 0.35);
		}
	}

	/** X: one strike rocket on its climb-and-dive arc (a curve the server's impact tick matches). */
	static final class StrikeRocket extends GrandLineFx.Effect {
		final Vec3 from, to, control;
		final int flight;
		final List<Vec3> trail = new ArrayList<>();

		StrikeRocket(int caster, Vec3 from, Vec3 to, int flight) {
			super(caster);
			this.from = from;
			this.to = to;
			this.flight = Math.max(1, flight);
			double up = 7 + from.distanceTo(to) * 0.15;
			this.control = from.lerp(to, 0.35).add(0, up, 0);
		}

		@Override
		boolean followsCaster() {
			return false;
		}

		Vec3 at(double t) {
			double s = 1 - t;
			return from.scale(s * s).add(control.scale(2 * s * t)).add(to.scale(t * t));
		}

		@Override
		boolean tick(ClientLevel level) {
			age++;
			trail.add(0, at(Math.min(1, age / (double) flight)));
			while (trail.size() > trailLength()) {
				trail.remove(trail.size() - 1);
			}
			return age < flight;
		}

		@Override
		void render(FxDraw d, ClientLevel level) {
			double t = Math.min(1, (age + d.partial) / flight);
			Vec3 p = at(t);
			Vec3 dir = at(Math.min(1, t + 0.02)).subtract(at(Math.max(0, t - 0.02)));
			if (dir.lengthSqr() < 1e-6) {
				dir = new Vec3(0, -1, 0);
			}
			dir = dir.normalize();
			List<Vec3> pts = new ArrayList<>(trail);
			pts.add(0, p);
			drawTrail(d, pts, 0.16, 1f);
			drawExhaust(d, p.subtract(dir.scale(0.05)), dir.scale(-1), 0.55, age, 1f);
			drawRocket(d, p, dir, 0.6, age * 0.5);
		}
	}

	/** X: the target area, a pulsing red ring on the ground until the last rocket lands. */
	static final class Marker extends GrandLineFx.Effect {
		final Vec3 at;
		final double radius;
		final int life;

		Marker(int caster, Vec3 at, double radius, int life) {
			super(caster);
			this.at = at.add(0, 0.05, 0);
			this.radius = radius;
			this.life = life;
		}

		@Override
		boolean followsCaster() {
			return false;
		}

		@Override
		boolean tick(ClientLevel level) {
			return ++age < life;
		}

		@Override
		void render(FxDraw d, ClientLevel level) {
			float pulse = (float) (0.5 + 0.5 * Math.sin((age + d.partial) * 0.8));
			float fade = Math.min(1f, (life - age) / 4f);
			d.groundRing(at, radius, 0.18, 32, FxDraw.rgba(RED, (0.45f + 0.35f * pulse) * fade));
			d.groundRing(at, radius * (1 - (age % 10) / 10.0), 0.1, 24, FxDraw.rgba(ORANGE, 0.5f * fade));
		}
	}

	/** An explosion: flash, fireball, sparks, embers and smoke, sized by its blast radius. */
	static final class Explosion extends GrandLineFx.Effect {
		final Vec3 at;
		final double radius;
		final int life;
		final List<Vec3> lobes = new ArrayList<>();

		Explosion(Vec3 at, double radius, int life) {
			super(-1);
			this.at = at;
			this.radius = radius;
			this.life = life;
		}

		@Override
		boolean tick(ClientLevel level) {
			RandomSource r = level.random;
			if (age == 0) {
				for (int i = 0; i < 6; i++) {
					lobes.add(FxSprites.randomDir(r).multiply(1, 0.6, 1).add(0, 0.35, 0));
				}
				for (int i = FxSprites.count((int) (radius * 10)); i > 0; i--) {
					FxSprites.spark(at, FxSprites.randomDir(r).scale(0.25 + r.nextDouble() * 0.35 * radius / 2.6), 0.08, 8 + r.nextInt(8), WHITE, ORANGE);
				}
				for (int i = FxSprites.count((int) (radius * 4)); i > 0; i--) {
					FxSprites.ember(at, FxSprites.randomDir(r).scale(0.15 + r.nextDouble() * 0.2).add(0, 0.2, 0), 0.06, 20 + r.nextInt(15), YELLOW, RED);
				}
				for (int i = FxSprites.count((int) (radius * 5)); i > 0; i--) {
					Vec3 o = FxSprites.randomDir(r).scale(radius * 0.4 * r.nextDouble());
					FxSprites.smoke(at.add(o).add(0, 0.3, 0), o.scale(0.04).add(0, 0.03, 0), 0.6 + radius * 0.2, 30 + r.nextInt(20), SMOKE);
				}
			}
			return ++age < life;
		}

		@Override
		void render(FxDraw d, ClientLevel level) {
			float t = (age + d.partial) / life;
			if (t < 0.25f) {
				d.billboard(FxDraw.GLOW, true, at, radius * (0.8 + t * 2), 0, FxDraw.rgba(WHITE, 1f - t * 4));
			}
			double grow = radius * (0.35 + 0.65 * Math.sqrt(t));
			float heat = Math.max(0, 1 - t * 1.4f);
			for (int i = 0; i < lobes.size(); i++) {
				Vec3 p = at.add(lobes.get(i).scale(grow * 0.55)).add(0, t * radius * 0.4, 0);
				int color = FxSprites.lerpColor(YELLOW, RED, Math.min(1, t * 2));
				d.flame(p, grow * 0.55, i * 1.3 + t, (age + i) & 3, FxDraw.rgba(color, 0.85f * heat));
			}
			d.billboard(FxDraw.GLOW, true, at, grow * 0.9, 0, FxDraw.rgba(ORANGE, 0.5f * heat));
		}
	}

	/** A shockwave ring racing out along the ground. */
	static final class Ring extends GrandLineFx.Effect {
		final Vec3 at;
		final double radius;
		final int life;
		final boolean dust;

		Ring(Vec3 at, double radius, int life, boolean dust) {
			super(-1);
			this.at = at.add(0, 0.08, 0);
			this.radius = radius;
			this.life = life;
			this.dust = dust;
		}

		@Override
		boolean tick(ClientLevel level) {
			if (dust && age == 0) {
				RandomSource r = level.random;
				for (int i = FxSprites.count(18); i > 0; i--) {
					double a = r.nextDouble() * Math.PI * 2;
					Vec3 out = new Vec3(Math.cos(a), 0, Math.sin(a));
					FxSprites.smoke(at.add(out.scale(0.8)), out.scale(0.25).add(0, 0.02, 0), 0.7, 25 + r.nextInt(10), 0x6A5E52);
				}
			}
			return ++age < life;
		}

		@Override
		void render(FxDraw d, ClientLevel level) {
			float t = (age + d.partial) / life;
			double r = radius * (1 - (1 - t) * (1 - t));
			d.groundRing(at, r, 0.35 * (1 - t) + 0.05, 40, FxDraw.rgba(0xFFE2B0, 0.7f * (1 - t)));
		}
	}

	/** C and F: the rocket exhaust at the caster's feet and back, with its smoke trail, until told to end. */
	static final class Thrust extends GrandLineFx.Effect {
		final int life;
		final boolean crash;
		final List<Vec3> trail = new ArrayList<>();
		boolean ending;
		int endAge;

		Thrust(int caster, int life, boolean crash) {
			super(caster);
			this.life = life;
			this.crash = crash;
		}

		@Override
		void cancel() {
			ending = true;
		}

		@Override
		boolean tick(ClientLevel level) {
			age++;
			if (ending && ++endAge > 8) {
				return false;
			}
			Entity c = caster(level);
			if (c == null) {
				return false;
			}
			Vec3 feet = c.position();
			if (!ending) {
				trail.add(0, feet);
				while (trail.size() > trailLength() + 4) {
					trail.remove(trail.size() - 1);
				}
				if (level.random.nextFloat() < 0.7f) {
					FxSprites.smoke(feet, c.getDeltaMovement().scale(-0.2).add(FxSprites.randomDir(level.random).scale(0.02)), 0.4, 20, SMOKE);
				}
			} else if (!trail.isEmpty()) {
				trail.remove(trail.size() - 1);
			}
			return age < life;
		}

		@Override
		void render(FxDraw d, ClientLevel level) {
			Entity c = caster(level);
			if (c == null) {
				return;
			}
			float alpha = ending ? Math.max(0, 1 - (endAge + d.partial) / 8f) : 1f;
			Vec3 feet = c.getPosition(d.partial);
			Vec3 vel = c.getDeltaMovement();
			Vec3 back = vel.lengthSqr() > 0.01 ? vel.normalize().scale(-1) : new Vec3(0, -1, 0);
			// Charging (crash windup): a sputtering flame under the feet before the launch.
			double size = crash && age < RocketMoves.CRASH_WINDUP_TICKS ? 0.5 + age * 0.1 : 1.3;
			drawExhaust(d, feet.add(0, 0.1, 0), back, size, age, alpha);
			// Twin back thrusters while flying.
			if (!crash) {
				double yaw = Math.toRadians(c.getYRot());
				Vec3 behind = new Vec3(Math.sin(yaw), 0, -Math.cos(yaw)).scale(0.3);
				Vec3 side = new Vec3(Math.cos(yaw), 0, Math.sin(yaw)).scale(0.2);
				Vec3 base = feet.add(0, 1.2, 0).add(behind);
				drawExhaust(d, base.add(side), back, 0.6, age, alpha);
				drawExhaust(d, base.subtract(side), back, 0.6, age + 3, alpha);
			}
			List<Vec3> pts = new ArrayList<>(trail);
			pts.add(0, feet);
			drawTrail(d, pts, 0.45, alpha);
		}
	}

	/** Runs an action after a few ticks (client-side cue timing). */
	static final class Delayed extends GrandLineFx.Effect {
		final int delay;
		final Runnable action;

		Delayed(int delay, Runnable action) {
			super(-1);
			this.delay = delay;
			this.action = action;
		}

		@Override
		boolean tick(ClientLevel level) {
			if (++age >= delay) {
				action.run();
				return false;
			}
			return true;
		}

		@Override
		void render(FxDraw d, ClientLevel level) {
		}
	}
}
