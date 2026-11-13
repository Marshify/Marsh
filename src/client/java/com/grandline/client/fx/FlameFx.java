package com.grandline.client.fx;

import com.grandline.client.settings.CameraShake;
import com.grandline.fruit.flame.FlameMoves;
import com.grandline.network.payload.FxEventPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Flame Fruit visuals. Each server beat starts a shaped effect drawn with Grand Line's own textures: a white-hot core,
 * turbulent flame-tongue shells and ribbon wakes for fireballs; a curved flame sheet with a bright leading edge for the
 * wave; a low ring wall for the burst; a flame mass, ground marker and blast ring for the finisher; streamers for flight.
 * Sparks and embers (FxSprites) are secondary detail and scale with the density setting; the shapes never do.
 */
final class FlameFx {
	static final int WHITE = 0xFFF4D6;
	static final int YELLOW = 0xFFC93C;
	static final int ORANGE = 0xFF6E14;
	static final int RED = 0xD52A0C;
	static final int DEEP = 0x7E1606;
	static final int SMOKE = 0x2C2522;

	private FlameFx() {
	}

	static boolean handles(String effect) {
		return effect.startsWith("flame_") || effect.equals("flight_end");
	}

	// ------------------------------------------------------------------------------------------------ beats

	static void handle(FxEventPayload p, ClientLevel level, Entity caster, boolean own) {
		Minecraft mc = Minecraft.getInstance();
		int id = p.caster();
		float age = caster == null ? 0 : caster.tickCount;
		switch (p.effect()) {
			case "flame_shot_coil" -> {
				if (caster != null) {
					MovePoses.windup(id, MovePoses.Kind.READY, age);
					GrandLineFx.add(new Coil(id, FlameMoves.SHOT_COIL_TICKS + FlameMoves.SHOTS * FlameMoves.SHOT_INTERVAL + 2));
				}
			}
			case "flame_shot" -> {
				boolean left = ((int) p.size()) % 2 == 1;
				if (caster != null) {
					MovePoses.release(id, MovePoses.Kind.PUNCH, age, left);
				}
				if (own && caster != null) {
					mc.player.swing(left ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
				}
				GrandLineFx.add(new Fireball(id, p.pos(), p.dir(), p.speed(), p.range(), FlameMoves.SHOT_SIZE, 7, false));
				burst(level, p.pos(), p.dir(), 0.35, 5);
			}
			case "flame_shot_hit" -> {
				GrandLineFx.endProjectile(id, p.pos());
				int kind = (int) p.size();
				GrandLineFx.add(kind == 0 ? new Fizzle(p.pos()) : new Impact(p.pos(), kind == 2 ? 0.9 : 0.6, kind == 2 ? 10 : 8));
				if (kind == 2) {
					shake(mc, p.pos(), own ? 1.0f : 0.6f);
				}
			}
			case "flame_wave_windup" -> {
				if (caster != null) {
					MovePoses.windup(id, MovePoses.Kind.SWEEP, age);
					GrandLineFx.add(new ArmFlames(id, FlameMoves.WAVE_WINDUP_TICKS + 1));
				}
			}
			case "flame_wave" -> {
				if (caster != null) {
					MovePoses.release(id, MovePoses.Kind.SWEEP, age, false);
				}
				if (own) {
					mc.player.swing(InteractionHand.MAIN_HAND);
				}
				GrandLineFx.add(new Wave(id, p.pos(), p.dir(), p.speed(), p.range(), p.size()));
			}
			case "flame_wave_end" -> GrandLineFx.endWave(id, p.size());
			case "flame_wave_hit" -> {
				GrandLineFx.add(new Impact(p.pos(), 0.8, 9));
				shake(mc, p.pos(), own ? 0.9f : 0.5f);
			}
			case "flame_ring_gather" -> {
				if (caster != null) {
					MovePoses.windup(id, MovePoses.Kind.PLANT, age);
				}
				GrandLineFx.add(new Gather(id, p.pos(), FlameMoves.RING_GATHER_TICKS + 1));
			}
			case "flame_ring" -> {
				if (caster != null) {
					MovePoses.release(id, MovePoses.Kind.PLANT, age, false);
				}
				GrandLineFx.add(new GroundRing(p.pos(), p.speed(), p.range(), p.size()));
				shake(mc, p.pos(), own ? 1.2f : 0.8f);
			}
			case "flame_nova_charge" -> {
				if (caster != null) {
					MovePoses.windup(id, MovePoses.Kind.HEAVY, age);
					NovaCharge charge = GrandLineFx.find(NovaCharge.class, id);
					if (charge == null) {
						GrandLineFx.add(new NovaCharge(id, p.size()));
					} else {
						charge.update(p.size());
					}
				}
			}
			case "flame_nova" -> {
				NovaCharge charge = GrandLineFx.find(NovaCharge.class, id);
				if (charge != null) {
					charge.released = true;
				}
				if (caster != null) {
					MovePoses.release(id, MovePoses.Kind.HEAVY, age, false);
				}
				if (own) {
					mc.player.swing(InteractionHand.MAIN_HAND);
				}
				GrandLineFx.add(new Fireball(id, p.pos(), p.dir(), p.speed(), p.range() + 4, p.size(), 13, true));
				GrandLineFx.add(new TargetMarker(id, p.pos().add(p.dir().scale(p.range())), FlameMoves.novaRadius(p.size())));
			}
			case "flame_nova_impact" -> {
				GrandLineFx.endProjectile(id, p.pos());
				GrandLineFx.endMarker(id);
				GrandLineFx.add(new NovaImpact(p.pos(), p.size()));
				shake(mc, p.pos(), own ? 2.4f : 1.8f);
			}
			case "flame_flight_start" -> {
				if (caster != null) {
					MovePoses.windup(id, MovePoses.Kind.FLIGHT, age);
					GrandLineFx.add(new Flight(id));
				}
				GrandLineFx.add(new Ignition(p.pos()));
			}
			case "flight_end" -> {
				MovePoses.clear(id, MovePoses.Kind.FLIGHT);
				Flight flight = GrandLineFx.find(Flight.class, id);
				if (flight != null) {
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

	/** Small puff at a launch point: sparks thrown forward. */
	private static void burst(ClientLevel level, Vec3 at, Vec3 dir, double speed, int base) {
		RandomSource r = level.random;
		for (int i = FxSprites.count(base); i > 0; i--) {
			Vec3 v = dir.scale(speed * (0.4 + r.nextDouble())).add(FxSprites.randomDir(r).scale(0.06));
			FxSprites.spark(at, v, 0.07, 5 + r.nextInt(4), WHITE, ORANGE);
		}
	}

	// ------------------------------------------------------------------------------------------------ helpers

	/** Billboard roll that points a flame tongue's tip along {@code dir} on screen. */
	static double rollToward(FxDraw d, Vec3 dir) {
		double sx = dir.dot(d.cameraRight());
		double sy = dir.dot(d.cameraUp());
		return Math.atan2(-sx, sy);
	}

	static float t(int age, float partial, int life) {
		return Math.min(1f, (age + partial) / life);
	}

	// ------------------------------------------------------------------------------------------------ effects

	/** Z windup: tongues of fire spiral tightly round the casting hand around a hot core. */
	static final class Coil extends GrandLineFx.Effect {
		final int life;

		Coil(int caster, int life) {
			super(caster);
			this.life = life;
		}

		@Override
		boolean tick(ClientLevel level) {
			Entity c = caster(level);
			if (c != null && age % 2 == 0) {
				Vec3 hand = GrandLineFx.handPosition(c, 1f, false);
				for (int i = FxSprites.count(2); i > 0; i--) {
					Vec3 off = FxSprites.randomDir(level.random).scale(0.55);
					FxSprites.spark(hand.add(off), off.scale(-0.12), 0.05, 5, YELLOW, ORANGE);
				}
			}
			return ++age < life;
		}

		@Override
		void render(FxDraw d, ClientLevel level) {
			Entity c = caster(level);
			if (c == null) {
				return;
			}
			Vec3 hand = GrandLineFx.handPosition(c, d.partial, false);
			Vec3 look = c.getViewVector(d.partial);
			Vec3 a = look.cross(new Vec3(0, 1, 0));
			a = a.lengthSqr() < 1e-4 ? new Vec3(1, 0, 0) : a.normalize();
			Vec3 b = a.cross(look).normalize();
			float tt = age + d.partial;
			float tighten = Math.min(1f, tt / FlameMoves.SHOT_COIL_TICKS);
			float fade = tt > life - 3 ? (life - tt) / 3f : 1f;
			double radius = 0.34 - 0.16 * tighten;
			for (int i = 0; i < 9; i++) {
				double ang = tt * 0.75 + i * 0.7;
				Vec3 p = hand.add(a.scale(Math.cos(ang) * radius)).add(b.scale(Math.sin(ang) * radius)).add(look.scale((i - 4) * 0.035));
				Vec3 tangent = a.scale(-Math.sin(ang)).add(b.scale(Math.cos(ang)));
				d.flame(p, 0.11 + 0.03 * tighten, rollToward(d, tangent.scale(-1)), i + (int) tt, FxDraw.rgba(i % 2 == 0 ? ORANGE : YELLOW, 0.95f * fade));
			}
			d.billboard(FxDraw.GLOW, true, hand, 0.16 + 0.12 * tighten + 0.02 * Math.sin(tt * 1.7), 0, FxDraw.rgba(WHITE, 0.95f * fade));
			d.billboard(FxDraw.GLOW, true, hand, 0.45, 0, FxDraw.rgba(ORANGE, 0.35f * fade));
		}
	}

	/** A fireball in flight: white-hot core, turbulent tongue shell, tapering ribbon wake, embers shed behind it. */
	static final class Fireball extends GrandLineFx.Effect implements GrandLineFx.Projectile {
		final Vec3 dir;
		final double speed, range, size;
		final int wakeLength;
		final boolean big;
		Vec3 pos, prev;
		double travelled;
		final List<Vec3> history = new ArrayList<>();

		Fireball(int caster, Vec3 start, Vec3 dir, double speed, double range, double size, int wakeLength, boolean big) {
			super(caster);
			this.dir = dir.lengthSqr() < 1e-6 ? new Vec3(0, 0, 1) : dir.normalize();
			this.speed = speed;
			this.range = range;
			this.size = size;
			this.wakeLength = wakeLength;
			this.big = big;
			this.pos = start;
			this.prev = start;
			history.add(start);
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
		boolean followsCaster() {
			return false; // in flight it no longer depends on the caster
		}

		@Override
		boolean tick(ClientLevel level) {
			prev = pos;
			pos = pos.add(dir.scale(speed));
			travelled += speed;
			history.add(0, pos);
			while (history.size() > wakeLength) {
				history.remove(history.size() - 1);
			}
			RandomSource r = level.random;
			for (int i = FxSprites.count(big ? 4 : 1); i > 0; i--) {
				Vec3 at = pos.subtract(dir.scale(size * (0.3 + r.nextDouble() * 0.6))).add(FxSprites.randomDir(r).scale(size * 0.3));
				FxSprites.ember(at, FxSprites.randomDir(r).scale(0.02).add(0, 0.02, 0), size * (0.18 + r.nextDouble() * 0.12), 8 + r.nextInt(8),
						YELLOW, RED);
			}
			age++;
			return travelled < range + speed * 2;
		}

		@Override
		void render(FxDraw d, ClientLevel level) {
			Vec3 p = prev.lerp(pos, d.partial);
			float tt = age + d.partial;
			// Wake: two ribbons (outer orange, inner yellow) along the path just flown.
			List<Vec3> pts = new ArrayList<>();
			pts.add(p);
			for (int i = 1; i < history.size(); i++) {
				pts.add(history.get(i));
			}
			if (pts.size() >= 2) {
				int n = pts.size();
				double[] w = new double[n];
				double[] wi = new double[n];
				float[] al = new float[n];
				for (int i = 0; i < n; i++) {
					float k = i / (float) (n - 1);
					w[i] = size * 1.5 * (1 - k * 0.85);
					wi[i] = size * 0.6 * (1 - k);
					al[i] = 0.85f * (1 - k);
				}
				d.ribbon(FxDraw.STREAK, pts, w, FxDraw.rgba(ORANGE, 1), al, tt * 0.15f);
				d.ribbon(FxDraw.STREAK, pts, wi, FxDraw.rgba(YELLOW, 1), al, tt * 0.21f);
			}
			// Shell: flame tongues trailing off the back, each wobbling on its own.
			double roll = rollToward(d, dir.scale(-1));
			int tongues = big ? 9 : 5;
			Vec3 a = dir.cross(new Vec3(0, 1, 0));
			a = a.lengthSqr() < 1e-4 ? new Vec3(1, 0, 0) : a.normalize();
			Vec3 b = a.cross(dir).normalize();
			for (int i = 0; i < tongues; i++) {
				double ang = i * Math.PI * 2 / tongues + tt * 0.5;
				double wob = Math.sin(tt * 1.3 + i * 2.1);
				Vec3 off = a.scale(Math.cos(ang) * size * 0.28).add(b.scale(Math.sin(ang) * size * 0.28)).subtract(dir.scale(size * (0.15 + 0.1 * wob)));
				d.flame(p.add(off), size * (0.62 + 0.12 * wob), roll + wob * 0.25, i + (int) (tt * 1.5), FxDraw.rgba(i % 3 == 0 ? YELLOW : ORANGE, 0.92f));
			}
			// Core: a hard white-hot centre inside a warm glow.
			d.billboard(FxDraw.GLOW, true, p, size * 1.25, 0, FxDraw.rgba(ORANGE, 0.55f));
			d.billboard(FxDraw.GLOW, true, p, size * 0.62, 0, FxDraw.rgba(WHITE, 1f));
			if (big) {
				d.billboard(FxDraw.GLOW, true, p, size * 2.2, 0, FxDraw.rgba(RED, 0.25f));
			}
		}
	}

	/** Contact: a sharp white flash, a star of light, flame tongues bursting outward and a thin shock ring. */
	static final class Impact extends GrandLineFx.Effect {
		final Vec3 pos;
		final double scale;
		final int life;
		final Vec3[] dirs = new Vec3[10];

		Impact(Vec3 pos, double scale, int life) {
			super(-1);
			this.pos = pos;
			this.scale = scale;
			this.life = life;
			RandomSource r = RandomSource.create();
			for (int i = 0; i < dirs.length; i++) {
				dirs[i] = FxSprites.randomDir(r);
			}
			for (int i = FxSprites.count((int) (14 * scale)); i > 0; i--) {
				FxSprites.spark(pos, FxSprites.randomDir(r).scale(0.22 + r.nextDouble() * 0.2), 0.08 * scale, 6 + r.nextInt(5), WHITE, ORANGE);
			}
			for (int i = FxSprites.count((int) (7 * scale)); i > 0; i--) {
				FxSprites.ember(pos, FxSprites.randomDir(r).scale(0.08).add(0, 0.06, 0), 0.2 * scale, 10 + r.nextInt(8), YELLOW, RED);
			}
		}

		@Override
		boolean tick(ClientLevel level) {
			return ++age < life;
		}

		@Override
		void render(FxDraw d, ClientLevel level) {
			float t = t(age, d.partial, life);
			float tf = (age + d.partial);
			// Flash: pops to full size in two ticks, gone by the fifth.
			if (tf < 5) {
				float k = tf < 2 ? tf / 2 : 1 - (tf - 2) / 3;
				d.billboard(FxDraw.GLOW, true, pos, scale * (0.5 + 1.1 * Math.min(1, tf / 2)), 0, FxDraw.rgba(WHITE, k));
				d.billboard(FxDraw.SPARK, true, pos, scale * 1.4 * (1 - tf / 5), Math.PI / 4, FxDraw.rgba(WHITE, k));
			}
			// Burst: tongues fly out from the contact point, tips leading.
			float e = 1 - (1 - t) * (1 - t);
			for (int i = 0; i < dirs.length; i++) {
				Vec3 p = pos.add(dirs[i].scale(scale * 1.3 * e));
				d.flame(p, scale * 0.42 * (1 - t * 0.6), rollToward(d, dirs[i]), i + age, FxDraw.rgba(i % 2 == 0 ? ORANGE : YELLOW, 0.95f * (1 - t)));
			}
			d.billboard(FxDraw.GLOW, true, pos, scale * (0.9 + e), 0, FxDraw.rgba(ORANGE, 0.5f * (1 - t)));
			d.groundRing(pos, scale * 1.8 * e, 0.3 * scale, 28, FxDraw.rgba(YELLOW, 0.8f * (1 - t)));
		}
	}

	/** A shot that burns out at the end of its range: a soft orange puff that shrinks away. */
	static final class Fizzle extends GrandLineFx.Effect {
		final Vec3 pos;

		Fizzle(Vec3 pos) {
			super(-1);
			this.pos = pos;
			RandomSource r = RandomSource.create();
			for (int i = FxSprites.count(4); i > 0; i--) {
				FxSprites.smoke(pos, FxSprites.randomDir(r).scale(0.02).add(0, 0.02, 0), 0.2, 16, SMOKE);
			}
		}

		@Override
		boolean tick(ClientLevel level) {
			return ++age < 6;
		}

		@Override
		void render(FxDraw d, ClientLevel level) {
			float t = t(age, d.partial, 6);
			d.billboard(FxDraw.GLOW, true, pos, 0.35 * (1 - t), 0, FxDraw.rgba(ORANGE, 0.8f * (1 - t)));
		}
	}

	/** X windup: flames run along the casting arm from shoulder to hand. */
	static final class ArmFlames extends GrandLineFx.Effect {
		final int life;

		ArmFlames(int caster, int life) {
			super(caster);
			this.life = life;
		}

		@Override
		boolean tick(ClientLevel level) {
			return ++age < life;
		}

		@Override
		void render(FxDraw d, ClientLevel level) {
			Entity c = caster(level);
			if (c == null) {
				return;
			}
			Vec3 hand = GrandLineFx.handPosition(c, d.partial, false);
			Vec3 shoulder = GrandLineFx.shoulderPosition(c, d.partial);
			float tt = age + d.partial;
			float grow = Math.min(1f, tt / 4f);
			for (int i = 0; i < 6; i++) {
				double k = i / 5.0;
				Vec3 p = shoulder.lerp(hand, k).add(0, 0.05 * Math.sin(tt + i), 0);
				d.flame(p, (0.12 + 0.1 * k) * grow, 0.2 * Math.sin(tt * 0.8 + i), i + (int) tt, FxDraw.rgba(k > 0.6 ? YELLOW : ORANGE, 0.9f));
			}
			d.billboard(FxDraw.GLOW, true, hand, 0.18 * grow, 0, FxDraw.rgba(WHITE, 0.8f));
		}
	}

	/** X: a broad curved wall of flame sliding forward over the ground, bright along its leading edge. */
	static final class Wave extends GrandLineFx.Effect {
		final Vec3 origin, dir, side;
		final double speed, range, width;
		double end = -1;
		int fade = -1;

		Wave(int caster, Vec3 origin, Vec3 dir, double speed, double range, double width) {
			super(caster);
			this.origin = origin;
			this.dir = new Vec3(dir.x, 0, dir.z).normalize();
			this.side = new Vec3(-this.dir.z, 0, this.dir.x);
			this.speed = speed;
			this.range = range;
			this.width = width;
		}

		@Override
		boolean followsCaster() {
			return false;
		}

		void stop(double distance) {
			end = distance;
		}

		double front(float partial) {
			double f = speed * (age + partial + 1);
			double limit = end >= 0 ? end : range;
			return Math.min(f, limit);
		}

		@Override
		boolean tick(ClientLevel level) {
			age++;
			double f = front(0);
			if (fade < 0 && (f >= range || end >= 0 && f >= end)) {
				fade = 0;
			}
			if (fade >= 0) {
				fade++;
			}
			RandomSource r = level.random;
			for (int i = FxSprites.count(7); i > 0; i--) {
				double l = (r.nextDouble() - 0.5) * width;
				double lead = f - FlameMoves.WAVE_BOW * Math.pow(l / (width / 2), 2);
				Vec3 at = origin.add(dir.scale(lead)).add(side.scale(l)).add(0, 0.2 + r.nextDouble() * 1.6, 0);
				FxSprites.ember(at, dir.scale(0.05).add(0, 0.05, 0), 0.22, 10 + r.nextInt(8), YELLOW, RED);
			}
			return fade < 6;
		}

		@Override
		void render(FxDraw d, ClientLevel level) {
			double f = front(d.partial);
			float tt = age + d.partial;
			float alpha = fade < 0 ? 1f : Math.max(0, 1 - (fade + d.partial) / 6f);
			float rise = Math.min(1f, 0.35f + tt / 4f);
			int segments = 18;
			double half = width / 2;
			// Three layers back to front: a dim trailing sheet, the main flame sheet, and a hot inner sheet.
			sheet(d, f - 1.1, half, segments, FlameMoves.WAVE_HEIGHT * 0.7 * rise, -0.4, RED, 0.45f * alpha, tt);
			sheet(d, f, half, segments, FlameMoves.WAVE_HEIGHT * rise, -0.6, ORANGE, 0.9f * alpha, tt);
			sheet(d, f + 0.15, half, segments, FlameMoves.WAVE_HEIGHT * 0.55 * rise, -0.3, YELLOW, 0.85f * alpha, tt + 2);
			// Leading edge: a white-hot seam along the foot of the wave.
			for (int i = 0; i <= segments; i++) {
				double l = -half + width * i / segments;
				double lead = f - FlameMoves.WAVE_BOW * Math.pow(l / half, 2) + 0.2;
				Vec3 p = origin.add(dir.scale(lead)).add(side.scale(l)).add(0, 0.25, 0);
				d.billboard(FxDraw.GLOW, true, p, 0.42, 0, FxDraw.rgba(WHITE, 0.55f * alpha));
			}
		}

		private void sheet(FxDraw d, double front, double half, int segments, double height, double lean, int color, float alpha, float tt) {
			if (front <= 0.05) {
				return;
			}
			// Continuous body: one flame band along the whole arc (bright foot, ragged top), scrolling sideways.
			for (int i = 0; i < segments; i++) {
				double l0 = -half + 2 * half * i / segments, l1 = -half + 2 * half * (i + 1) / segments;
				Vec3 b0 = base(front, l0, half), b1 = base(front, l1, half);
				double h0 = height * (1 - 0.45 * (l0 / half) * (l0 / half)), h1 = height * (1 - 0.45 * (l1 / half) * (l1 / half));
				Vec3 t0 = b0.add(dir.scale(lean)).add(0, h0, 0), t1 = b1.add(dir.scale(lean)).add(0, h1, 0);
				float u0 = i / (float) segments * 2 + tt * 0.04f, u1 = (i + 1) / (float) segments * 2 + tt * 0.04f;
				d.quad(FxDraw.BAND, true, b0, b1, t1, t0, FxDraw.rgba(color, alpha), u0, u1, 0, 1);
			}
			// Tongues on top: wide, overlapping, each flickering to its own height, so the crest is a living flame line.
			int tongues = segments / 2 + 2;
			for (int i = 0; i < tongues; i++) {
				double l = -half + 2 * half * (i + 0.5) / tongues;
				double span = 2 * half / tongues * 1.8;
				double flick = 0.8 + 0.3 * Math.sin(tt * 1.1 + i * 2.3);
				double h = height * (1 - 0.45 * (l / half) * (l / half)) * 1.25 * flick;
				Vec3 c0 = base(front, l - span / 2, half), c1 = base(front, l + span / 2, half);
				Vec3 top0 = c0.add(dir.scale(lean * 1.2)).add(0, h, 0), top1 = c1.add(dir.scale(lean * 1.2)).add(0, h, 0);
				int frame = Math.floorMod(i + (int) (tt * 0.7), 4);
				d.quad(FxDraw.FLAME, true, c0, c1, top1, top0, FxDraw.rgba(color, alpha * 0.85f), frame / 4f, (frame + 1) / 4f, 0, 1);
			}
		}

		private Vec3 base(double front, double l, double half) {
			double lead = front - FlameMoves.WAVE_BOW * (l / half) * (l / half);
			return origin.add(dir.scale(lead)).add(side.scale(l));
		}
	}

	/** C windup: three spiral arms of flame slide in across the ground to the caster's planted feet. */
	static final class Gather extends GrandLineFx.Effect {
		final Vec3 center;
		final int life;

		Gather(int caster, Vec3 center, int life) {
			super(caster);
			this.center = center;
			this.life = life;
		}

		@Override
		boolean tick(ClientLevel level) {
			return ++age < life;
		}

		@Override
		void render(FxDraw d, ClientLevel level) {
			float tt = age + d.partial;
			float t = Math.min(1f, tt / FlameMoves.RING_GATHER_TICKS);
			for (int k = 0; k < 3; k++) {
				for (int j = 0; j < 6; j++) {
					double ang = k * Math.PI * 2 / 3 + j * 0.45 - tt * 0.35;
					double radius = (4.6 - j * 0.6) * (1 - t * 0.85);
					Vec3 p = center.add(Math.cos(ang) * radius, 0.25, Math.sin(ang) * radius);
					Vec3 inward = center.subtract(p);
					d.flame(p, 0.26 - j * 0.02, rollToward(d, inward.scale(-1)), j + (int) tt, FxDraw.rgba(j < 2 ? YELLOW : ORANGE, 0.9f * Math.min(1, tt / 2)));
				}
			}
			d.groundRing(center.add(0, 0.06, 0), 0.5 + 0.5 * t, 0.5, 20, FxDraw.rgba(YELLOW, 0.8f * t));
			d.billboard(FxDraw.GLOW, true, center.add(0, 0.4, 0), 0.4 + 0.5 * t, 0, FxDraw.rgba(ORANGE, 0.7f * t));
		}
	}

	/** C: a low wall of fire racing outward along the ground; the middle stays clear so the caster stays readable. */
	static final class GroundRing extends GrandLineFx.Effect {
		final Vec3 center;
		final double speed, radius, height;
		int over = -1;

		GroundRing(Vec3 center, double speed, double radius, double height) {
			super(-1);
			this.center = center.add(0, 0.05, 0);
			this.speed = speed;
			this.radius = radius;
			this.height = height;
		}

		double r(float partial) {
			return Math.min(radius, speed * (age + partial + 1));
		}

		@Override
		boolean tick(ClientLevel level) {
			age++;
			double r = r(0);
			if (over < 0 && r >= radius) {
				over = 0;
			}
			if (over >= 0) {
				over++;
			}
			RandomSource rs = level.random;
			for (int i = FxSprites.count(12); i > 0; i--) {
				double ang = rs.nextDouble() * Math.PI * 2;
				Vec3 at = center.add(Math.cos(ang) * r, 0.2, Math.sin(ang) * r);
				FxSprites.ember(at, new Vec3(Math.cos(ang) * 0.05, 0.07, Math.sin(ang) * 0.05), 0.24, 9 + rs.nextInt(6), YELLOW, RED);
			}
			return over < 5;
		}

		@Override
		void render(FxDraw d, ClientLevel level) {
			double r = r(d.partial);
			float tt = age + d.partial;
			float alpha = over < 0 ? 1f : Math.max(0, 1 - (over + d.partial) / 5f);
			double shrink = 1 - 0.35 * r / radius;
			int seg = 64;
			d.ringWall(center, Math.max(0.1, r - 0.6), height * 0.6 * shrink, seg, FxDraw.rgba(RED, 0.5f * alpha), tt * 0.05f);
			d.ringWall(center, r, height * shrink, seg, FxDraw.rgba(ORANGE, 0.95f * alpha), tt * 0.08f);
			d.ringWall(center, r + 0.15, height * 0.5 * shrink, seg, FxDraw.rgba(YELLOW, 0.9f * alpha), -tt * 0.06f);
			d.groundRing(center, r, 1.0, seg, FxDraw.rgba(WHITE, 0.6f * alpha));
			d.groundRing(center, r * 0.6, 0.6, seg, FxDraw.rgba(DEEP, 0.4f * alpha));
		}
	}

	/** V while held: a churning mass of fire grows over the raised hands, pulling sparks in. */
	static final class NovaCharge extends GrandLineFx.Effect {
		double charge, shown;
		int lastBeat;
		boolean released;

		NovaCharge(int caster, double charge) {
			super(caster);
			this.charge = charge;
		}

		void update(double charge) {
			this.charge = charge;
			this.lastBeat = age;
		}

		Vec3 center(Entity c, float partial) {
			Minecraft mc = Minecraft.getInstance();
			double size = FlameMoves.novaSize(shown);
			if (c == mc.player && mc.options.getCameraType().isFirstPerson()) {
				// First person: hold it up and ahead so it sits at the top of the view, clear of the crosshair.
				return c.getEyePosition(partial).add(c.getViewVector(partial).scale(2.2)).add(0, 0.95 + size * 0.25, 0);
			}
			return c.getPosition(partial).add(0, c.getBbHeight() + 1.0 + size * 0.5, 0);
		}

		@Override
		boolean tick(ClientLevel level) {
			age++;
			shown += (charge - shown) * 0.35;
			Entity c = caster(level);
			if (released || c == null || age - lastBeat > 12) {
				return false;
			}
			Vec3 at = center(c, 1f);
			for (int i = FxSprites.count(4); i > 0; i--) {
				Vec3 off = FxSprites.randomDir(level.random).scale(2.6);
				FxSprites.spark(at.add(off), off.scale(-0.09), 0.08, 9, YELLOW, ORANGE);
			}
			return true;
		}

		@Override
		void render(FxDraw d, ClientLevel level) {
			Entity c = caster(level);
			if (c == null) {
				return;
			}
			double size = FlameMoves.novaSize(shown);
			Vec3 p = center(c, d.partial);
			float tt = age + d.partial;
			float in = Math.min(1f, tt / 5f);
			d.billboard(FxDraw.GLOW, true, p, size * 2.0 * in, 0, FxDraw.rgba(RED, 0.3f));
			int tongues = 12;
			for (int i = 0; i < tongues; i++) {
				double ang = i * Math.PI * 2 / tongues + tt * 0.25;
				double el = Math.sin(i * 1.7 + tt * 0.2) * 0.9;
				Vec3 radial = new Vec3(Math.cos(ang) * Math.cos(el), Math.sin(el), Math.sin(ang) * Math.cos(el));
				double wob = Math.sin(tt * 1.4 + i);
				d.flame(p.add(radial.scale(size * 0.42 * in)), size * (0.5 + 0.1 * wob) * in, rollToward(d, radial), i + (int) tt,
						FxDraw.rgba(i % 3 == 0 ? YELLOW : ORANGE, 0.9f));
			}
			d.billboard(FxDraw.GLOW, true, p, size * 0.9 * in, 0, FxDraw.rgba(ORANGE, 0.7f));
			d.billboard(FxDraw.GLOW, true, p, size * 0.5 * in, 0, FxDraw.rgba(WHITE, 1f));
		}
	}

	/** V in flight: the blast area, marked on the ground where the fireball will land, pulsing until impact. */
	static final class TargetMarker extends GrandLineFx.Effect {
		final Vec3 at;
		final double radius;

		TargetMarker(int caster, Vec3 at, double radius) {
			super(caster);
			this.at = at.add(0, 0.08, 0);
			this.radius = radius;
		}

		@Override
		boolean followsCaster() {
			return false;
		}

		@Override
		boolean tick(ClientLevel level) {
			return ++age < 60;
		}

		@Override
		void render(FxDraw d, ClientLevel level) {
			float tt = age + d.partial;
			float pulse = 0.55f + 0.25f * Mth.sin(tt * 0.6f);
			float in = Math.min(1f, tt / 4f);
			d.groundRing(at, radius * in, 0.35, 56, FxDraw.rgba(ORANGE, pulse));
			d.groundRing(at, radius * 0.5 * in, radius, 56, FxDraw.rgba(RED, 0.12f));
			for (int i = 0; i < 8; i++) {
				double ang = i * Math.PI / 4 + tt * 0.04;
				Vec3 p = at.add(Math.cos(ang) * radius * in, 0.1, Math.sin(ang) * radius * in);
				d.billboard(FxDraw.GLOW, true, p, 0.3, 0, FxDraw.rgba(YELLOW, pulse));
			}
		}
	}

	/** V impact: a big flash, a fast ring of fire to the blast radius, a burst of tongues, then slow embers and smoke. */
	static final class NovaImpact extends GrandLineFx.Effect {
		final Vec3 pos;
		final double radius;
		final Vec3[] dirs = new Vec3[16];

		NovaImpact(Vec3 pos, double radius) {
			super(-1);
			this.pos = pos;
			this.radius = radius;
			RandomSource r = RandomSource.create();
			for (int i = 0; i < dirs.length; i++) {
				Vec3 v = FxSprites.randomDir(r);
				dirs[i] = new Vec3(v.x, Math.abs(v.y) * 0.8 + 0.2, v.z).normalize();
			}
			for (int i = FxSprites.count(70); i > 0; i--) {
				double ang = r.nextDouble() * Math.PI * 2, dist = Math.sqrt(r.nextDouble()) * radius;
				Vec3 at = pos.add(Math.cos(ang) * dist, 0.2 + r.nextDouble() * 1.5, Math.sin(ang) * dist);
				FxSprites.ember(at, new Vec3(Math.cos(ang) * 0.03, 0.03 + r.nextDouble() * 0.04, Math.sin(ang) * 0.03), 0.25 + r.nextDouble() * 0.2,
						20 + r.nextInt(16), YELLOW, DEEP);
			}
			for (int i = FxSprites.count(30); i > 0; i--) {
				FxSprites.spark(pos.add(0, 0.5, 0), FxSprites.randomDir(r).scale(0.5 + r.nextDouble() * 0.4), 0.14, 8 + r.nextInt(6), WHITE, ORANGE);
			}
			for (int i = FxSprites.count(14); i > 0; i--) {
				double ang = r.nextDouble() * Math.PI * 2, dist = r.nextDouble() * radius * 0.7;
				FxSprites.smoke(pos.add(Math.cos(ang) * dist, 0.6 + r.nextDouble(), Math.sin(ang) * dist), new Vec3(0, 0.03, 0),
						0.8 + r.nextDouble() * 0.6, 30 + r.nextInt(14), SMOKE);
			}
		}

		@Override
		boolean tick(ClientLevel level) {
			return ++age < 22;
		}

		@Override
		void render(FxDraw d, ClientLevel level) {
			float tf = age + d.partial;
			float ringT = Math.min(1f, tf / 5f);
			float e = 1 - (1 - ringT) * (1 - ringT);
			// Flash.
			if (tf < 6) {
				float k = tf < 2 ? tf / 2 : 1 - (tf - 2) / 4;
				d.billboard(FxDraw.GLOW, true, pos.add(0, 1, 0), radius * 0.75 * Math.min(1, tf / 2), 0, FxDraw.rgba(WHITE, k));
				d.billboard(FxDraw.SPARK, true, pos.add(0, 1, 0), radius * 0.9 * (1 - tf / 6), Math.PI / 4, FxDraw.rgba(WHITE, k));
			}
			// Fast ring of fire out to the real blast radius.
			float ringAlpha = tf < 8 ? 1f : Math.max(0, 1 - (tf - 8) / 8f);
			double r = radius * e;
			d.ringWall(pos.add(0, 0.05, 0), r, 1.8 * (1 - ringT * 0.5), 72, FxDraw.rgba(ORANGE, 0.95f * ringAlpha), tf * 0.08f);
			d.ringWall(pos.add(0, 0.05, 0), r + 0.2, 0.9, 72, FxDraw.rgba(YELLOW, 0.85f * ringAlpha), -tf * 0.06f);
			d.groundRing(pos.add(0, 0.07, 0), r, 1.4, 72, FxDraw.rgba(WHITE, 0.6f * ringAlpha));
			// Dome of tongues bursting up and out.
			float bt = Math.min(1f, tf / 9f);
			float be = 1 - (1 - bt) * (1 - bt);
			for (int i = 0; i < dirs.length; i++) {
				Vec3 p = pos.add(0, 0.4, 0).add(dirs[i].scale(radius * 0.55 * be));
				d.flame(p, radius * 0.2 * (1 - bt * 0.5), rollToward(d, dirs[i]), i + age, FxDraw.rgba(i % 2 == 0 ? ORANGE : YELLOW, 0.95f * (1 - bt)));
			}
			d.billboard(FxDraw.GLOW, true, pos.add(0, 0.8, 0), radius * (0.5 + 0.4 * be), 0, FxDraw.rgba(RED, 0.4f * (1 - bt)));
		}
	}

	/** F take-off: a ring of fire bursts from the feet and a column of tongues lifts with the caster. */
	static final class Ignition extends GrandLineFx.Effect {
		final Vec3 pos;

		Ignition(Vec3 pos) {
			super(-1);
			this.pos = pos.add(0, 0.06, 0);
			RandomSource r = RandomSource.create();
			for (int i = FxSprites.count(16); i > 0; i--) {
				double ang = r.nextDouble() * Math.PI * 2;
				FxSprites.spark(this.pos, new Vec3(Math.cos(ang) * 0.3, 0.05 + r.nextDouble() * 0.15, Math.sin(ang) * 0.3), 0.08, 8, WHITE, ORANGE);
			}
		}

		@Override
		boolean tick(ClientLevel level) {
			return ++age < 10;
		}

		@Override
		void render(FxDraw d, ClientLevel level) {
			float t = t(age, d.partial, 10);
			float e = 1 - (1 - t) * (1 - t);
			d.groundRing(pos, 0.4 + 2.6 * e, 0.6, 40, FxDraw.rgba(YELLOW, 0.9f * (1 - t)));
			d.ringWall(pos, 0.4 + 2.4 * e, 0.8 * (1 - t), 40, FxDraw.rgba(ORANGE, 0.9f * (1 - t)), t);
			for (int i = 0; i < 6; i++) {
				double ang = i * Math.PI / 3;
				Vec3 p = pos.add(Math.cos(ang) * 0.35, 0.3 + e * 1.4 + i * 0.1, Math.sin(ang) * 0.35);
				d.flame(p, 0.3 * (1 - t), 0, i + age, FxDraw.rgba(ORANGE, 0.9f * (1 - t)));
			}
		}
	}

	/** F: two flame streamers from the lower back that follow every turn, and fire licking at the feet. */
	static final class Flight extends GrandLineFx.Effect {
		static final int LENGTH = 16;
		final List<Vec3> left = new ArrayList<>();
		final List<Vec3> right = new ArrayList<>();
		boolean ending;
		int fade;

		Flight(int caster) {
			super(caster);
		}

		@Override
		void cancel() {
			ending = true; // fade out the streamers rather than cutting them
		}

		static Vec3 anchor(Entity c, float partial, boolean leftSide) {
			float yaw = Mth.lerp(partial, c.yRotO, c.getYRot()) * Mth.DEG_TO_RAD;
			Vec3 forward = new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw));
			Vec3 rightV = new Vec3(-Mth.cos(yaw), 0, -Mth.sin(yaw));
			return c.getPosition(partial).add(0, 0.85, 0).subtract(forward.scale(0.25)).add(rightV.scale(leftSide ? -0.2 : 0.2));
		}

		@Override
		boolean tick(ClientLevel level) {
			age++;
			Entity c = caster(level);
			if (ending || c == null) {
				fade++;
				if (!left.isEmpty()) {
					left.remove(left.size() - 1);
					right.remove(right.size() - 1);
				}
				return fade < 10 && !left.isEmpty();
			}
			left.add(0, anchor(c, 1f, true));
			right.add(0, anchor(c, 1f, false));
			while (left.size() > LENGTH) {
				left.remove(left.size() - 1);
				right.remove(right.size() - 1);
			}
			RandomSource r = level.random;
			for (int i = FxSprites.count(2); i > 0; i--) {
				Vec3 at = (r.nextBoolean() ? left : right).get(0);
				FxSprites.ember(at, FxSprites.randomDir(r).scale(0.02), 0.18, 10, YELLOW, RED);
			}
			return true;
		}

		@Override
		void render(FxDraw d, ClientLevel level) {
			Entity c = caster(level);
			float tt = age + d.partial;
			float alpha = ending ? Math.max(0, 1 - (fade + d.partial) / 10f) : 1f;
			streamer(d, c, left, true, alpha, tt);
			streamer(d, c, right, false, alpha, tt);
			Minecraft mc = Minecraft.getInstance();
			boolean firstPerson = c == mc.player && mc.options.getCameraType().isFirstPerson();
			if (c != null && !ending && !firstPerson) {
				Vec3 feet = c.getPosition(d.partial);
				for (int i = 0; i < 3; i++) {
					d.flame(feet.add(0.1 * Math.cos(i * 2.1), -0.1, 0.1 * Math.sin(i * 2.1)), 0.32, Math.PI, i + (int) tt, FxDraw.rgba(ORANGE, 0.85f));
				}
				d.billboard(FxDraw.GLOW, true, feet.add(0, 0.1, 0), 0.5, 0, FxDraw.rgba(YELLOW, 0.5f));
			}
		}

		private void streamer(FxDraw d, Entity c, List<Vec3> pts, boolean leftSide, float alpha, float tt) {
			if (pts.size() < 2) {
				return;
			}
			List<Vec3> all = new ArrayList<>();
			if (c != null && !ending) {
				all.add(anchor(c, d.partial, leftSide));
			}
			all.addAll(pts);
			int n = all.size();
			double[] w = new double[n];
			double[] wi = new double[n];
			float[] al = new float[n];
			for (int i = 0; i < n; i++) {
				float k = i / (float) (n - 1);
				w[i] = 0.6 * (1 - k * 0.8) * (0.85 + 0.15 * Math.sin(tt * 0.9 + i));
				wi[i] = 0.25 * (1 - k);
				al[i] = alpha * 0.85f * (1 - k);
			}
			d.ribbon(FxDraw.STREAK, all, w, FxDraw.rgba(ORANGE, 1), al, tt * 0.12f);
			d.ribbon(FxDraw.STREAK, all, wi, FxDraw.rgba(YELLOW, 1), al, tt * 0.2f);
		}
	}
}
