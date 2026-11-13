package com.grandline.client.fx;

import com.grandline.network.payload.FxEventPayload;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Saber visuals (the Saber's own hits and the Saber Warden's attacks): crisp steel-white sword trails that sweep with
 * the blade, red ground arcs that show where a telegraphed cut will land (filling up until it strikes), a charging
 * glint on the blade, and the flying crescent of sword energy. Drawn with Grand Line's own glow/streak textures.
 */
final class SaberFx {
	static final int WHITE = 0xFFFFFF;
	static final int STEEL = 0xCFE6FF;
	static final int BLUE = 0x79B6FF;
	static final int WARN = 0xFF3B2E;
	static final int WARN_DARK = 0x8E1208;

	private SaberFx() {
	}

	static boolean handles(String effect) {
		return effect.startsWith("boss_") || effect.equals("saber_hit");
	}

	static void handle(FxEventPayload p, ClientLevel level, Entity caster, boolean own) {
		Vec3 forward = p.dir().lengthSqr() < 1e-6 ? new Vec3(0, 0, 1) : p.dir().normalize();
		switch (p.effect()) {
			case "saber_hit" -> {
				GrandLineFx.add(new Slash(-1, p.pos().subtract(forward.scale(0.9)).add(0, -0.3, 0), forward, 1.15, 0, 2.5F, 0.75));
				GrandLineFx.add(new Flash(p.pos(), 0.55, 5, STEEL));
				sparks(p.pos(), forward, 6, 0.25);
			}
			case "boss_slash" -> {
				int style = (int) p.size();
				GrandLineFx.add(new Slash(p.caster(), p.pos(), forward, p.range() * 0.82, style, style == 3 ? 3.5F : 3F, 1.0));
				Vec3 tip = p.pos().add(forward.scale(p.range() * 0.7));
				sparks(tip, forward, 10, 0.35);
			}
			case "boss_warn_slash" -> GrandLineFx.add(new WarnArc(p.caster(), p.pos(), forward, p.range(), 120, (int) p.size()));
			case "boss_warn_combo" -> {
				GrandLineFx.add(new WarnArc(p.caster(), p.pos(), forward, p.range(), 110, (int) p.size()));
				GrandLineFx.add(new BladeGlint(p.caster(), (int) p.size(), WARN));
			}
			case "boss_warn_fly" -> GrandLineFx.add(new BladeGlint(p.caster(), (int) p.size(), BLUE));
			case "boss_wave" -> GrandLineFx.add(new Wave(p.caster(), p.pos(), forward, p.speed(), p.range()));
			case "boss_wave_end" -> {
				GrandLineFx.endProjectile(p.caster(), p.pos());
				GrandLineFx.add(new Flash(p.pos(), 1.1, 7, STEEL));
				sparks(p.pos(), new Vec3(0, 0.4, 0), 16, 0.45);
			}
			default -> { }
		}
	}

	static void sparks(Vec3 at, Vec3 dir, int base, double speed) {
		RandomSource r = RandomSource.create();
		for (int i = 0; i < FxSprites.count(base); i++) {
			Vec3 v = new Vec3(r.nextGaussian(), r.nextGaussian() * 0.6 + 0.3, r.nextGaussian()).normalize().scale(speed * (0.4 + r.nextDouble()))
					.add(dir.scale(speed * 0.5));
			FxSprites.spark(at, v, 0.06 + r.nextDouble() * 0.05, 6 + r.nextInt(6), WHITE, BLUE);
		}
	}

	static Vec3 right(Vec3 f) {
		return new Vec3(-f.z, 0, f.x);
	}

	// ------------------------------------------------------------------------------------------------ sword trail

	/**
	 * A sword trail: the arc the blade tip draws. The head sweeps the arc over {@code sweep} ticks with a tapering tail
	 * behind it; then the whole trail fades. Styles: 0 a flat cut right to left, 1 rising left to right, 2 falling right
	 * to left, 3 an overhead chop.
	 */
	static final class Slash extends GrandLineFx.Effect {
		final Vec3 center;
		final Vec3 forward;
		final double radius;
		final int style;
		final float sweep;
		final double scale;

		Slash(int caster, Vec3 center, Vec3 forward, double radius, int style, float sweep, double scale) {
			super(caster);
			this.center = center;
			this.forward = forward.multiply(1, 0, 1).normalize();
			this.radius = radius;
			this.style = style;
			this.sweep = sweep;
			this.scale = scale;
		}

		@Override
		boolean followsCaster() {
			return false;
		}

		@Override
		boolean tick(ClientLevel level) {
			return ++age < sweep + 8;
		}

		Vec3 point(double t) {
			Vec3 r = right(forward);
			Vec3 up = new Vec3(0, 1, 0);
			if (style == 3) {
				double a = Mth.lerp(t, -0.45, 1.75); // from above and behind the head to low in front
				return center.add(0, 0.6, 0).add(forward.scale(Math.sin(a) * radius)).add(up.scale(Math.cos(a) * radius * 0.9));
			}
			double from = style == 1 ? -1.25 : 1.25, to = -from;
			double tilt = style == 1 ? 0.28 : style == 2 ? -0.28 : 0.12;
			double a = Mth.lerp(t, from, to);
			return center.add(forward.scale(Math.cos(a) * radius)).add(r.scale(Math.sin(a) * radius)).add(up.scale(Math.sin(a) * radius * tilt));
		}

		@Override
		void render(FxDraw d, ClientLevel level) {
			float time = age + d.partial;
			double head = Math.min(1, time / sweep);
			double tail = Math.max(0, head - 0.75);
			float fade = time <= sweep ? 1 : Math.max(0, 1 - (time - sweep) / 8f);
			if (fade <= 0) {
				return;
			}
			if (time > sweep) {
				tail = Math.min(head, tail + (time - sweep) / 8.0 * (head - tail));
			}
			int n = 18;
			List<Vec3> pts = new ArrayList<>();
			double[] wide = new double[n];
			double[] thin = new double[n];
			float[] a1 = new float[n];
			float[] a2 = new float[n];
			for (int i = 0; i < n; i++) {
				double k = i / (double) (n - 1); // 0 = head
				pts.add(point(Mth.lerp(k, head, tail)));
				double taper = Math.sin(Math.PI * Math.min(1, (1 - k) * 1.15)) * 0.6 + (1 - k) * 0.4;
				wide[i] = 0.75 * scale * taper;
				thin[i] = 0.18 * scale * taper;
				a1[i] = (float) (0.55 * fade * (1 - k * 0.85));
				a2[i] = (float) (0.95 * fade * (1 - k));
			}
			d.ribbon(FxDraw.GLOW, pts, wide, rgb(BLUE), a1, 0);
			d.ribbon(FxDraw.STREAK, pts, thin, rgb(WHITE), a2, 0);
			if (time < sweep + 1) {
				d.billboard(FxDraw.GLOW, true, pts.get(0), 0.32 * scale, 0, FxDraw.rgba(STEEL, 0.9f * fade));
			}
		}
	}

	static float[] rgb(int c) {
		float[] f = FxDraw.rgba(c, 1);
		return new float[]{f[0], f[1], f[2]};
	}

	// ------------------------------------------------------------------------------------------------ telegraphs

	/** A red arc on the ground in front of the boss, filling outward until the cut lands, then flashing. */
	static final class WarnArc extends GrandLineFx.Effect {
		final Vec3 origin;
		final Vec3 forward;
		final double range;
		final double half;
		final int ticks;

		WarnArc(int caster, Vec3 origin, Vec3 forward, double range, double degrees, int ticks) {
			super(caster);
			this.origin = origin.add(0, 0.06, 0);
			this.forward = forward.multiply(1, 0, 1).normalize();
			this.range = range;
			this.half = Math.toRadians(degrees / 2);
			this.ticks = Math.max(1, ticks);
		}

		@Override
		boolean tick(ClientLevel level) {
			return ++age < ticks + 4;
		}

		Vec3 at(double a, double r, Vec3 base) {
			Vec3 side = right(forward);
			return base.add(forward.scale(Math.cos(a) * r)).add(side.scale(Math.sin(a) * r));
		}

		@Override
		void render(FxDraw d, ClientLevel level) {
			Entity boss = caster(level);
			Vec3 base = boss == null ? origin : boss.getPosition(d.partial).add(0, 0.06, 0);
			float time = age + d.partial;
			float fill = Math.min(1, time / ticks);
			float flash = time > ticks ? Math.max(0, 1 - (time - ticks) / 4f) : 0;
			float pulse = 0.75f + 0.25f * Mth.sin(time * 1.3f);
			int seg = 14;
			for (int i = 0; i < seg; i++) {
				double a0 = Mth.lerp(i / (double) seg, -half, half), a1 = Mth.lerp((i + 1) / (double) seg, -half, half);
				// Filled part grows from the boss outward.
				double rf = range * fill;
				d.quad(FxDraw.GLOW, true, base, base, at(a1, rf, base), at(a0, rf, base),
						FxDraw.rgba(WARN_DARK, (0.22f + flash * 0.5f) * pulse), 0.5f, 0.5f, 0.5f, 0.5f);
				// Bright rim at full range.
				Vec3 i0 = at(a0, range - 0.12, base), i1 = at(a1, range - 0.12, base);
				Vec3 o0 = at(a0, range + 0.12, base), o1 = at(a1, range + 0.12, base);
				d.quad(FxDraw.STREAK, true, i0, i1, o1, o0, FxDraw.rgba(WARN, (0.55f + flash * 0.45f) * pulse), 0, 1, 0, 1);
			}
			// Side edges.
			for (double a : new double[]{-half, half}) {
				Vec3 e = at(a, range, base);
				Vec3 side = right(forward).scale(0.08);
				d.quad(FxDraw.STREAK, true, base.subtract(side), base.add(side), e.add(side), e.subtract(side),
						FxDraw.rgba(WARN, 0.45f * pulse), 0, 1, 0, 1);
			}
		}
	}

	/** A glint building on the blade during a wind-up (blue: a flying slash is coming; red: the heavy combo). */
	static final class BladeGlint extends GrandLineFx.Effect {
		final int ticks;
		final int color;

		BladeGlint(int caster, int ticks, int color) {
			super(caster);
			this.ticks = Math.max(1, ticks);
			this.color = color;
		}

		@Override
		boolean tick(ClientLevel level) {
			Entity boss = caster(level);
			if (boss != null && age % 2 == 0) {
				Vec3 hand = GrandLineFx.handPosition(boss, 1, false);
				RandomSource r = RandomSource.create();
				Vec3 from = hand.add(r.nextGaussian() * 0.8, r.nextGaussian() * 0.6, r.nextGaussian() * 0.8);
				FxSprites.spark(from, hand.subtract(from).scale(0.22), 0.05, 5, WHITE, color);
			}
			return ++age < ticks + 2;
		}

		@Override
		void render(FxDraw d, ClientLevel level) {
			Entity boss = caster(level);
			if (boss == null) {
				return;
			}
			float time = age + d.partial;
			float k = Math.min(1, time / ticks);
			Vec3 hand = GrandLineFx.handPosition(boss, d.partial, false);
			float flicker = 0.85f + 0.15f * Mth.sin(time * 2.1f);
			d.billboard(FxDraw.GLOW, true, hand, 0.25 + 0.55 * k, 0, FxDraw.rgba(color, 0.45f * k * flicker));
			d.billboard(FxDraw.SPARK, true, hand, 0.15 + 0.35 * k, time * 0.25, FxDraw.rgba(WHITE, 0.9f * k));
		}
	}

	// ------------------------------------------------------------------------------------------------ flying slash

	/** The crescent of sword energy: a bright, bowed blade of light flying level, with afterimages behind it. */
	static final class Wave extends GrandLineFx.Effect implements GrandLineFx.Projectile {
		final Vec3 start;
		final Vec3 dir;
		final double speed;
		final double range;
		Vec3 endAt;
		int endAge = -1;

		Wave(int caster, Vec3 start, Vec3 dir, double speed, double range) {
			super(caster);
			this.start = start;
			this.dir = dir.multiply(1, 0, 1).normalize();
			this.speed = speed;
			this.range = range;
		}

		@Override
		boolean followsCaster() {
			return false;
		}

		double travelled(float partial) {
			double t = endAge >= 0 ? endAge : age + partial;
			return Math.min(range, t * speed);
		}

		@Override
		public Vec3 position() {
			return start.add(dir.scale(travelled(0)));
		}

		@Override
		public void end(Vec3 at) {
			endAt = at;
			endAge = age;
		}

		@Override
		boolean tick(ClientLevel level) {
			age++;
			if (endAge < 0 && age % 2 == 0) {
				Vec3 p = position();
				FxSprites.spark(p.add(right(dir).scale((Math.random() - 0.5) * 2)), dir.scale(-0.05).add(0, 0.03, 0), 0.06, 7, WHITE, BLUE);
			}
			if (endAge < 0 && age * speed > range + 2) {
				endAge = age;
			}
			return endAge < 0 || age - endAge < 4;
		}

		void crescent(FxDraw d, Vec3 c, float alpha, double size) {
			Vec3 side = right(dir);
			Vec3 up = new Vec3(0, 1, 0);
			// Tilt the crescent a little, like a diagonal cut.
			Vec3 axis = side.scale(Math.cos(0.35)).add(up.scale(Math.sin(0.35)));
			int n = 15;
			List<Vec3> pts = new ArrayList<>();
			double[] wide = new double[n];
			double[] thin = new double[n];
			float[] a1 = new float[n];
			float[] a2 = new float[n];
			for (int i = 0; i < n; i++) {
				double t = i / (double) (n - 1);
				double a = Mth.lerp(t, -1.2, 1.2);
				pts.add(c.add(axis.scale(Math.sin(a) * 1.35 * size)).add(dir.scale((Math.cos(a) - 1) * 0.75 * size)));
				double belly = Math.cos(a * 1.1);
				wide[i] = 0.9 * size * belly;
				thin[i] = 0.26 * size * belly;
				a1[i] = (float) (0.6 * alpha * (0.4 + 0.6 * belly));
				a2[i] = (float) (alpha * (0.3 + 0.7 * belly));
			}
			d.ribbon(FxDraw.GLOW, pts, wide, rgb(BLUE), a1, 0);
			d.ribbon(FxDraw.STREAK, pts, thin, rgb(WHITE), a2, 0);
		}

		@Override
		void render(FxDraw d, ClientLevel level) {
			float fade = endAge < 0 ? 1 : Math.max(0, 1 - (age - endAge + d.partial) / 4f);
			Vec3 head = endAt != null ? endAt : start.add(dir.scale(travelled(d.partial)));
			crescent(d, head, fade, 1.0);
			for (int k = 1; k <= 3; k++) { // afterimages
				double back = k * 0.55;
				if (travelled(d.partial) > back) {
					crescent(d, head.subtract(dir.scale(back)), fade * (0.45f - k * 0.12f), 1.0 - k * 0.08);
				}
			}
			d.billboard(FxDraw.GLOW, true, head, 0.55, 0, FxDraw.rgba(STEEL, 0.35f * fade));
		}
	}

	/** A short bright flash where something was struck. */
	static final class Flash extends GrandLineFx.Effect {
		final Vec3 at;
		final double size;
		final int life;
		final int color;

		Flash(Vec3 at, double size, int life, int color) {
			super(-1);
			this.at = at;
			this.size = size;
			this.life = life;
			this.color = color;
		}

		@Override
		boolean tick(ClientLevel level) {
			return ++age < life;
		}

		@Override
		void render(FxDraw d, ClientLevel level) {
			float t = (age + d.partial) / life;
			float a = Math.max(0, 1 - t);
			d.billboard(FxDraw.GLOW, true, at, size * (0.6 + t), 0, FxDraw.rgba(color, 0.8f * a));
			d.billboard(FxDraw.SPARK, true, at, size * 0.7 * (0.5 + t), t * 2, FxDraw.rgba(WHITE, a));
		}
	}
}
