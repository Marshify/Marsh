package com.grandline.client.fx;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * The shapes sword moves are drawn with: blade trails, crescents and blade formations, rolling orbs and vortices,
 * shockwave rings and shells, ground fractures, beams, lightning, clouds and flashes. All are meshes built from Grand
 * Line's own textures (ribbons and textured quads), coloured by a {@link SwordPalette}.
 */
final class SwordShapes {
	static final Vec3 UP = new Vec3(0, 1, 0);
	static final RandomSource RANDOM = RandomSource.create();

	private SwordShapes() {
	}

	static float[] rgb(int c) {
		float[] f = FxDraw.rgba(c, 1);
		return new float[]{f[0], f[1], f[2]};
	}

	static Vec3 right(Vec3 f) {
		return new Vec3(-f.z, 0, f.x);
	}

	static Vec3 flat(Vec3 v) {
		Vec3 f = new Vec3(v.x, 0, v.z);
		return f.lengthSqr() < 1e-6 ? new Vec3(0, 0, 1) : f.normalize();
	}

	/** A unit vector perpendicular to {@code dir} (horizontal when possible). */
	static Vec3 side(Vec3 dir) {
		Vec3 s = dir.cross(UP);
		return s.lengthSqr() < 1e-6 ? new Vec3(1, 0, 0) : s.normalize();
	}

	/** {@code v} rotated by {@code angle} around the unit axis {@code axis}. */
	static Vec3 rotate(Vec3 v, Vec3 axis, double angle) {
		double c = Math.cos(angle), s = Math.sin(angle);
		return v.scale(c).add(axis.cross(v).scale(s)).add(axis.scale(axis.dot(v) * (1 - c)));
	}

	static float smooth(float t) {
		t = Mth.clamp(t, 0, 1);
		return t * t * (3 - 2 * t);
	}

	/**
	 * A crescent quad: convex side toward {@code fwd}, spanning {@code halfWidth} along {@code widthAxis}, {@code depth}
	 * deep. Drawn as a glowing core, an optional dark interior and a bright edge.
	 */
	static void crescent(FxDraw d, Vec3 center, Vec3 fwd, Vec3 widthAxis, double halfWidth, double depth, SwordPalette pal, float alpha) {
		Vec3 w = widthAxis.scale(halfWidth);
		Vec3 back = fwd.scale(-depth * 0.5), front = fwd.scale(depth * 0.5);
		Vec3 bl = center.subtract(w).add(back), br = center.add(w).add(back), tr = center.add(w).add(front), tl = center.subtract(w).add(front);
		d.quad(FxDraw.CRESCENT_FILL, true, bl, br, tr, tl, FxDraw.rgba(pal.core(), alpha * 0.55f), 0, 1, 0, 1);
		if (pal.darkCore()) {
			Vec3 in = fwd.scale(-depth * 0.06);
			d.quad(FxDraw.CRESCENT_FILL, false, bl.add(in), br.add(in), tr.add(in), tl.add(in), FxDraw.rgba(pal.dark(), alpha * 0.85f), 0.08f, 0.92f, 0.1f, 1);
		}
		d.quad(FxDraw.CRESCENT_EDGE, true, bl, br, tr, tl, FxDraw.rgba(pal.edge(), alpha), 0, 1, 0, 1);
		d.quad(FxDraw.CRESCENT_EDGE, true, bl, br, tr, tl, FxDraw.rgba(pal.core(), alpha * 0.6f), 0, 1, 0, 1);
	}

	static void sparks(Vec3 at, Vec3 dir, int base, double speed, SwordPalette pal) {
		for (int i = 0; i < FxSprites.count(base); i++) {
			Vec3 v = new Vec3(RANDOM.nextGaussian(), RANDOM.nextGaussian() * 0.6 + 0.3, RANDOM.nextGaussian()).normalize()
					.scale(speed * (0.4 + RANDOM.nextDouble())).add(dir.scale(speed * 0.5));
			FxSprites.spark(at, v, 0.05 + RANDOM.nextDouble() * 0.05, 6 + RANDOM.nextInt(6), pal.sparkFrom(), pal.sparkTo());
		}
	}

	static void motes(Vec3 at, double spread, int base, SwordPalette pal, int life) {
		for (int i = 0; i < FxSprites.count(base); i++) {
			Vec3 p = at.add(RANDOM.nextGaussian() * spread, RANDOM.nextGaussian() * spread * 0.6, RANDOM.nextGaussian() * spread);
			Vec3 v = new Vec3(RANDOM.nextGaussian() * 0.01, 0.01 + RANDOM.nextDouble() * 0.03, RANDOM.nextGaussian() * 0.01);
			FxSprites.sprite(FxDraw.GLOW, true, p, v, 0.07 + RANDOM.nextDouble() * 0.06, -0.003, -0.001, 0.96, life + RANDOM.nextInt(life / 2 + 1),
					pal.edge(), pal.core(), 0.9f);
		}
	}

	static void dust(Vec3 at, double spread, int base, int color) {
		for (int i = 0; i < FxSprites.count(base); i++) {
			Vec3 p = at.add(RANDOM.nextGaussian() * spread, RANDOM.nextDouble() * 0.4, RANDOM.nextGaussian() * spread);
			Vec3 v = new Vec3(RANDOM.nextGaussian() * 0.05, 0.02 + RANDOM.nextDouble() * 0.05, RANDOM.nextGaussian() * 0.05);
			FxSprites.sprite(FxDraw.CLOUD, false, p, v, 0.4 + RANDOM.nextDouble() * 0.4, 0.03, -0.002, 0.92, 22 + RANDOM.nextInt(12), color, color, 0.55f);
		}
	}

	// ============================================================================================ blade trail

	/**
	 * A blade trail: the arc the weapon's edge draws, following the caster. Styles: 0 flat right-to-left, 1 rising
	 * left-to-right, 2 falling right-to-left, 3 overhead chop, 4 thrust, 5 full spin, 6 flat left-to-right, 7 crossing pair.
	 */
	static final class Trail extends GrandLineFx.Effect {
		final Vec3 anchor;
		final Vec3 offset;
		final Vec3 forward;
		final double radius;
		final int style;
		final float sweep;
		final SwordPalette pal;
		final double scale;

		Trail(int caster, Vec3 anchor, Vec3 casterPos, Vec3 forward, double radius, int style, SwordPalette pal, double scale) {
			super(caster);
			this.anchor = anchor;
			this.offset = anchor.subtract(casterPos);
			this.forward = flat(forward);
			this.radius = radius;
			this.style = style;
			this.sweep = style == 5 ? 5f : style == 4 ? 2.5f : 3f;
			this.pal = pal;
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

		Vec3 point(Vec3 c, double t, int s) {
			Vec3 r = right(forward);
			switch (s) {
				case 3 -> {
					double a = Mth.lerp(t, -0.5, 1.8);
					return c.add(0, 0.5, 0).add(forward.scale(Math.sin(a) * radius)).add(UP.scale(Math.cos(a) * radius * 0.9));
				}
				case 4 -> {
					return c.add(forward.scale(Mth.lerp(t, 0.3, radius * 1.25))).add(r.scale(0.15)).add(0, -0.05, 0);
				}
				case 5 -> {
					double a = Mth.lerp(t, 0, Math.PI * 2) + Math.PI / 2;
					return c.add(forward.scale(Math.sin(a) * radius)).add(r.scale(Math.cos(a) * radius)).add(0, -0.2, 0);
				}
				default -> {
					double from = (s == 1 || s == 6) ? -1.3 : 1.3, to = -from;
					double tilt = s == 1 ? 0.3 : s == 2 ? -0.3 : 0.1;
					double a = Mth.lerp(t, from, to);
					return c.add(forward.scale(Math.cos(a) * radius)).add(r.scale(Math.sin(a) * radius)).add(UP.scale(Math.sin(a) * radius * tilt));
				}
			}
		}

		@Override
		void render(FxDraw d, ClientLevel level) {
			Entity e = caster(level);
			Vec3 c = e == null ? anchor : e.getPosition(d.partial).add(offset);
			if (style == 7) {
				draw(d, c, 1);
				draw(d, c, 2);
			} else {
				draw(d, c, style);
			}
		}

		private void draw(FxDraw d, Vec3 c, int s) {
			float time = age + d.partial;
			double head = Math.min(1, time / sweep);
			double tail = Math.max(0, head - (s == 5 ? 0.6 : 0.75));
			float fade = time <= sweep ? 1 : Math.max(0, 1 - (time - sweep) / 8f);
			if (fade <= 0) {
				return;
			}
			if (time > sweep) {
				tail = Math.min(head, tail + (time - sweep) / 8.0 * (head - tail));
			}
			int n = 18;
			List<Vec3> pts = new ArrayList<>();
			double[] wide = new double[n], thin = new double[n];
			float[] a1 = new float[n], a2 = new float[n], a3 = new float[n];
			for (int i = 0; i < n; i++) {
				double k = i / (double) (n - 1);
				pts.add(point(c, Mth.lerp(k, head, tail), s));
				double taper = Math.sin(Math.PI * Math.min(1, (1 - k) * 1.15)) * 0.6 + (1 - k) * 0.4;
				wide[i] = 0.7 * scale * taper;
				thin[i] = 0.16 * scale * taper;
				a1[i] = (float) (0.5 * fade * (1 - k * 0.85));
				a2[i] = (float) (0.95 * fade * (1 - k));
				a3[i] = (float) (0.65 * fade * (1 - k * 0.7));
			}
			if (pal.darkCore()) {
				double[] dark = new double[n];
				for (int i = 0; i < n; i++) {
					dark[i] = wide[i] * 0.75;
				}
				d.ribbonSoft(FxDraw.GLOW, pts, dark, rgb(pal.dark()), a3, 0);
			}
			d.ribbon(FxDraw.GLOW, pts, wide, rgb(pal.core()), a1, 0);
			d.ribbon(FxDraw.STREAK, pts, thin, rgb(pal.edge()), a2, 0);
		}
	}

	// ============================================================================================ crescent shots

	/**
	 * A travelling crescent (or a formation of {@code blades} crescents rolled round the flight line). {@code roll} tilts
	 * the blade (0 = flat, π/2 = upright); {@code size} is its half width. Ended by the server's impact beat.
	 */
	static final class Crescent extends GrandLineFx.Effect implements GrandLineFx.Projectile {
		final Vec3 start;
		final Vec3 dir;
		final double speed;
		final double range;
		final double size;
		final double roll;
		final int blades;
		final SwordPalette pal;
		final boolean wisps;
		Vec3 stopAt;
		int stopAge = -1;

		Crescent(int caster, Vec3 start, Vec3 dir, double speed, double range, double size, double roll, int blades, SwordPalette pal, boolean wisps) {
			super(caster);
			this.start = start;
			this.dir = dir.normalize();
			this.speed = speed;
			this.range = range;
			this.size = size;
			this.roll = roll;
			this.blades = blades;
			this.pal = pal;
			this.wisps = wisps;
		}

		@Override
		boolean followsCaster() {
			return false;
		}

		double travelled(float time) {
			return Math.min(range, speed * time);
		}

		@Override
		public Vec3 position() {
			return stopAt != null ? stopAt : start.add(dir.scale(travelled(age)));
		}

		@Override
		public void end(Vec3 at) {
			if (stopAt == null) {
				stopAt = at;
				stopAge = age;
			}
		}

		@Override
		boolean tick(ClientLevel level) {
			age++;
			if (stopAt == null && wisps && age % 2 == 0) {
				motes(position(), size * 0.35, 2, pal, 10);
			}
			if (stopAt == null && travelled(age) >= range) {
				end(position());
			}
			return stopAge < 0 ? age < 200 : age - stopAge < 5;
		}

		@Override
		void render(FxDraw d, ClientLevel level) {
			float time = age + d.partial;
			Vec3 pos = stopAt != null ? stopAt : start.add(dir.scale(travelled(time)));
			float fade = stopAge < 0 ? Math.min(1, time / 1.5f) : Math.max(0, 1 - (time - stopAge) / 5f);
			double grow = stopAge < 0 ? 1 : 1 + (time - stopAge) * 0.12;
			Vec3 side = side(dir);
			Vec3 up = side.cross(dir).normalize();
			for (int b = 0; b < blades; b++) {
				double r = roll + (blades > 1 ? b * Math.PI / blades : 0);
				Vec3 w = side.scale(Math.cos(r)).add(up.scale(Math.sin(r)));
				crescent(d, pos, dir, w, size * grow, size * 0.95 * grow, pal, fade);
				// A fading echo just behind it reads as the blade's trail.
				crescent(d, pos.subtract(dir.scale(speed * 0.9)), dir, w, size * 0.92 * grow, size * 0.85 * grow, pal, fade * 0.35f);
			}
		}
	}

	// ============================================================================================ rolling orb of ribbons

	/** A rotating ball of curved blade ribbons (a rolling cyclone); travels and is ended by the server. */
	static final class RibbonOrb extends GrandLineFx.Effect implements GrandLineFx.Projectile {
		final Vec3 start;
		final Vec3 dir;
		final double speed;
		final double range;
		final double radius;
		final SwordPalette pal;
		Vec3 stopAt;
		int stopAge = -1;

		RibbonOrb(int caster, Vec3 start, Vec3 dir, double speed, double range, double radius, SwordPalette pal) {
			super(caster);
			this.start = start;
			this.dir = dir.normalize();
			this.speed = speed;
			this.range = range;
			this.radius = radius;
			this.pal = pal;
		}

		@Override
		boolean followsCaster() {
			return false;
		}

		@Override
		public Vec3 position() {
			return stopAt != null ? stopAt : start.add(dir.scale(Math.min(range, speed * age)));
		}

		@Override
		public void end(Vec3 at) {
			if (stopAt == null) {
				stopAt = at;
				stopAge = age;
			}
		}

		@Override
		boolean tick(ClientLevel level) {
			age++;
			if (stopAt == null && speed * age >= range) {
				end(position());
			}
			return stopAge < 0 ? age < 200 : age - stopAge < 6;
		}

		@Override
		void render(FxDraw d, ClientLevel level) {
			float time = age + d.partial;
			Vec3 c = stopAt != null ? stopAt : start.add(dir.scale(Math.min(range, speed * time)));
			float fade = stopAge < 0 ? Math.min(1, time / 2f) : Math.max(0, 1 - (time - stopAge) / 6f);
			double r = radius * (stopAge < 0 ? 1 : 1 + (time - stopAge) * 0.25);
			ribbonBall(d, c, r, time * 0.55, 5, pal, fade);
		}
	}

	/** {@code count} curved blade ribbons wrapped round a ball, spinning by {@code spin}. */
	static void ribbonBall(FxDraw d, Vec3 c, double r, double spin, int count, SwordPalette pal, float fade) {
		for (int k = 0; k < count; k++) {
			Vec3 axis = rotate(new Vec3(0, 1, 0), new Vec3(1, 0, 0), k * 0.9 + 0.3);
			axis = rotate(axis, UP, k * 1.7 + spin * 0.3).normalize();
			Vec3 a0 = side(axis);
			Vec3 a1 = axis.cross(a0).normalize();
			int n = 12;
			List<Vec3> pts = new ArrayList<>();
			double[] w = new double[n], thin = new double[n];
			float[] al = new float[n], al2 = new float[n];
			double phase = spin * (1 + k * 0.15) + k;
			for (int i = 0; i < n; i++) {
				double t = i / (double) (n - 1);
				double a = phase - t * 2.2;
				pts.add(c.add(a0.scale(Math.cos(a) * r)).add(a1.scale(Math.sin(a) * r)));
				double taper = Math.sin(Math.PI * (1 - t) * 0.95 + 0.05);
				w[i] = 0.32 * r * taper;
				thin[i] = 0.08 * r * taper;
				al[i] = (float) (0.55 * fade * (1 - t));
				al2[i] = (float) (0.95 * fade * (1 - t));
			}
			d.ribbon(FxDraw.GLOW, pts, w, rgb(pal.core()), al, 0);
			d.ribbon(FxDraw.STREAK, pts, thin, rgb(pal.edge()), al2, 0);
		}
		d.billboard(FxDraw.GLOW, true, c, r * 0.6, 0, FxDraw.rgba(pal.core(), 0.35f * fade));
	}

	// ============================================================================================ vortex / tornado

	/**
	 * A spinning column of wind ribbons. {@code centre} supplies where it is each frame (the caster, a fixed point or a
	 * travelling point). {@code radius} at the foot, widening by {@code flare} toward the top of {@code height}.
	 */
	static class Vortex extends GrandLineFx.Effect {
		interface Centre {
			Vec3 at(Vortex v, ClientLevel level, float partial);
		}

		final Centre centre;
		final double radius;
		final double height;
		final double flare;
		final int life;
		final SwordPalette pal;
		final int strands;
		final boolean water;
		float fadeOut = 6;

		Vortex(int caster, Centre centre, double radius, double height, double flare, int life, SwordPalette pal, int strands, boolean water) {
			super(caster);
			this.centre = centre;
			this.radius = radius;
			this.height = height;
			this.flare = flare;
			this.life = life;
			this.pal = pal;
			this.strands = strands;
			this.water = water;
		}

		@Override
		boolean followsCaster() {
			return false;
		}

		@Override
		boolean tick(ClientLevel level) {
			age++;
			Vec3 c = centre.at(this, level, 1);
			if (age < life && age % 2 == 0) {
				double a = RANDOM.nextDouble() * Math.PI * 2;
				Vec3 p = c.add(Math.cos(a) * radius, RANDOM.nextDouble() * height * 0.6, Math.sin(a) * radius);
				if (water) {
					FxSprites.sprite(FxDraw.SHELL, true, p, new Vec3(-Math.sin(a) * 0.12, 0.04, Math.cos(a) * 0.12), 0.08 + RANDOM.nextDouble() * 0.07,
							0, -0.002, 0.95, 14, 0xE8FAFF, 0x6FC8FF, 0.85f);
				} else {
					FxSprites.sprite(FxDraw.CLOUD, false, p, new Vec3(-Math.sin(a) * 0.15, 0.03, Math.cos(a) * 0.15), 0.35, 0.02, 0, 0.92, 14,
							pal.core(), pal.edge(), 0.25f);
				}
			}
			return age < life + fadeOut;
		}

		@Override
		void render(FxDraw d, ClientLevel level) {
			float time = age + d.partial;
			Vec3 c = centre.at(this, level, d.partial);
			float fade = smooth(time / 4f) * (time < life ? 1 : Math.max(0, 1 - (time - life) / fadeOut));
			if (fade <= 0) {
				return;
			}
			for (int k = 0; k < strands; k++) {
				int n = 16;
				List<Vec3> pts = new ArrayList<>();
				double[] w = new double[n], thin = new double[n];
				float[] al = new float[n], al2 = new float[n];
				double base = k * Math.PI * 2 / strands + time * 0.55;
				double lift = (k % 3) * height * 0.12;
				for (int i = 0; i < n; i++) {
					double t = i / (double) (n - 1);
					double y = lift + t * height * 0.8;
					double rr = radius * (1 + flare * (y / Math.max(0.1, height)));
					double a = base - t * 3.2;
					pts.add(c.add(Math.cos(a) * rr, y, Math.sin(a) * rr));
					double taper = Math.sin(Math.PI * Math.min(1, (1 - t) * 1.1 + 0.02));
					w[i] = 0.45 * taper * (water ? 1.3 : 1);
					thin[i] = 0.1 * taper;
					al[i] = (float) (0.45 * fade * (1 - t * 0.7));
					al2[i] = (float) (0.85 * fade * (1 - t));
				}
				d.ribbon(FxDraw.GLOW, pts, w, rgb(pal.core()), al, (float) (time * 0.05));
				d.ribbon(FxDraw.STREAK, pts, thin, rgb(pal.edge()), al2, 0);
			}
			d.groundRing(c.add(0, 0.08, 0), radius * 1.05, 0.5, 28, FxDraw.rgba(pal.core(), 0.35f * fade));
		}
	}

	// ============================================================================================ rings and shells

	/** A ring racing outward along the ground (a band wall and a bright ground ring), up to {@code radius}. */
	static final class Shockwave extends GrandLineFx.Effect {
		final Vec3 c;
		final double speed;
		final double radius;
		final double height;
		final SwordPalette pal;

		Shockwave(Vec3 c, double speed, double radius, double height, SwordPalette pal) {
			super(-1);
			this.c = c.add(0, 0.05, 0);
			this.speed = speed;
			this.radius = radius;
			this.height = height;
			this.pal = pal;
		}

		@Override
		boolean tick(ClientLevel level) {
			return ++age < radius / speed + 6;
		}

		@Override
		void render(FxDraw d, ClientLevel level) {
			float time = age + d.partial;
			double r = Math.min(radius, speed * time);
			float fade = time * speed < radius ? 1 : Math.max(0, 1 - (time - (float) (radius / speed)) / 6f);
			float thin = (float) (1 - 0.5 * r / radius);
			d.ringWall(c, r, height * thin, 36, FxDraw.rgba(pal.core(), 0.55f * fade), (float) (time * 0.06));
			d.ringWall(c, r * 0.98, height * 0.5 * thin, 36, FxDraw.rgba(pal.edge(), 0.6f * fade), (float) (-time * 0.08));
			d.groundRing(c, r, 0.9, 36, FxDraw.rgba(pal.edge(), 0.75f * fade));
			d.groundRing(c, r * 0.6, 1.6, 30, FxDraw.rgba(pal.core(), 0.25f * fade));
		}
	}

	/** A translucent sphere swelling to {@code radius} and fading (a pressure shell). */
	static final class Shell extends GrandLineFx.Effect {
		final Vec3 c;
		final double radius;
		final int grow;
		final int life;
		final SwordPalette pal;

		Shell(Vec3 c, double radius, int grow, int life, SwordPalette pal) {
			super(-1);
			this.c = c;
			this.radius = radius;
			this.grow = grow;
			this.life = life;
			this.pal = pal;
		}

		@Override
		boolean tick(ClientLevel level) {
			return ++age < life;
		}

		@Override
		void render(FxDraw d, ClientLevel level) {
			float time = age + d.partial;
			double r = radius * (1 - Math.pow(1 - Math.min(1, time / grow), 3));
			float fade = time < grow ? 1 : Math.max(0, 1 - (time - grow) / (life - grow));
			// Stacked shells read as a solid sphere from every side; the rim brightens where it is seen edge-on.
			d.billboard(FxDraw.SHELL, true, c, r, time * 0.05, FxDraw.rgba(pal.edge(), 0.75f * fade));
			d.billboard(FxDraw.SHELL, true, c, r * 0.93, -time * 0.04, FxDraw.rgba(pal.core(), 0.45f * fade));
			d.billboard(FxDraw.GLOW, true, c, r * 0.55, 0, FxDraw.rgba(pal.edge(), 0.25f * fade));
			// Latitude bands give it depth.
			for (int i = -2; i <= 2; i++) {
				double y = i * r * 0.32;
				double rr = Math.sqrt(Math.max(0, r * r - y * y));
				d.groundRing(c.add(0, y, 0), rr, 0.22, 32, FxDraw.rgba(pal.edge(), 0.3f * fade));
			}
		}
	}

	/** A quick bright flash. */
	static final class Flash extends GrandLineFx.Effect {
		final Vec3 c;
		final double size;
		final int life;
		final int color;
		final int color2;

		Flash(Vec3 c, double size, int life, int color, int color2) {
			super(-1);
			this.c = c;
			this.size = size;
			this.life = life;
			this.color = color;
			this.color2 = color2;
		}

		@Override
		boolean tick(ClientLevel level) {
			return ++age < life;
		}

		@Override
		void render(FxDraw d, ClientLevel level) {
			float t = (age + d.partial) / life;
			float a = (float) Math.pow(1 - t, 1.5);
			d.billboard(FxDraw.GLOW, true, c, size * (0.7 + t * 0.6), 0, FxDraw.rgba(color2, 0.8f * a));
			d.billboard(FxDraw.GLOW, true, c, size * 0.45 * (1 + t * 0.3), 0, FxDraw.rgba(color, a));
			d.billboard(FxDraw.SPARK, true, c, size * 0.9 * (1 + t), t * 2, FxDraw.rgba(color, 0.7f * a));
		}
	}

	// ============================================================================================ ground fractures

	/**
	 * Branching cracks running out from a point along the ground, glowing in the palette (nothing in the world changes:
	 * they are drawn on top of it and fade). {@code count} main cracks, each with a branch or two.
	 */
	static final class Fractures extends GrandLineFx.Effect {
		final List<List<Vec3>> cracks = new ArrayList<>();
		final SwordPalette pal;
		final int life;
		final boolean glow;

		Fractures(Vec3 c, Vec3 forward, int count, double length, SwordPalette pal, int life, boolean glow) {
			super(-1);
			this.pal = pal;
			this.life = life;
			this.glow = glow;
			Vec3 base = c.add(0, 0.04, 0);
			Vec3 f = flat(forward);
			for (int k = 0; k < count; k++) {
				double angle = (count == 3 ? (k - 1) * 0.75 : k * Math.PI * 2 / count) + RANDOM.nextGaussian() * 0.15;
				Vec3 dir = rotate(f, UP, angle);
				List<Vec3> main = path(base, dir, length * (0.8 + RANDOM.nextDouble() * 0.4));
				cracks.add(main);
				for (int b = 0; b < 2; b++) {
					int from = 2 + RANDOM.nextInt(Math.max(1, main.size() - 3));
					Vec3 bd = rotate(dir, UP, (b == 0 ? 1 : -1) * (0.5 + RANDOM.nextDouble() * 0.4));
					cracks.add(path(main.get(Math.min(from, main.size() - 1)), bd, length * 0.4));
				}
			}
		}

		private static List<Vec3> path(Vec3 from, Vec3 dir, double length) {
			List<Vec3> pts = new ArrayList<>();
			Vec3 p = from;
			Vec3 d = dir;
			int n = Math.max(4, (int) (length / 0.5));
			for (int i = 0; i < n; i++) {
				pts.add(p);
				d = rotate(d, UP, RANDOM.nextGaussian() * 0.35).normalize();
				p = p.add(d.scale(length / n));
			}
			pts.add(p);
			return pts;
		}

		@Override
		boolean tick(ClientLevel level) {
			return ++age < life;
		}

		@Override
		void render(FxDraw d, ClientLevel level) {
			float time = age + d.partial;
			float reveal = Math.min(1, time / 4f);
			float fade = time < life - 10 ? 1 : Math.max(0, (life - time) / 10f);
			for (List<Vec3> crack : cracks) {
				int n = Math.max(2, (int) Math.ceil(crack.size() * reveal));
				for (int i = 0; i + 1 < n && i + 1 < crack.size(); i++) {
					Vec3 a = crack.get(i), b = crack.get(i + 1);
					Vec3 s = right(flat(b.subtract(a)));
					double w = 0.13 * (1 - i / (double) crack.size()) + 0.04;
					d.quadSoft(FxDraw.STREAK, a.subtract(s.scale(w)), a.add(s.scale(w)), b.add(s.scale(w * 0.85)), b.subtract(s.scale(w * 0.85)),
							FxDraw.rgba(pal.dark(), 0.9f * fade));
					if (glow) {
						d.quad(FxDraw.STREAK, true, a.subtract(s.scale(w * 0.45)), a.add(s.scale(w * 0.45)), b.add(s.scale(w * 0.4)), b.subtract(s.scale(w * 0.4)),
								FxDraw.rgba(pal.edge(), 0.8f * fade), 0, 1, 0, 1);
					}
				}
			}
		}
	}

	// ============================================================================================ beams and lightning

	/** A straight beam from {@code from} to {@code to}, {@code width} wide, flaring then fading. */
	static final class Beam extends GrandLineFx.Effect {
		final Vec3 from;
		final Vec3 to;
		final double width;
		final SwordPalette pal;

		Beam(Vec3 from, Vec3 to, double width, SwordPalette pal) {
			super(-1);
			this.from = from;
			this.to = to;
			this.width = width;
			this.pal = pal;
		}

		@Override
		boolean tick(ClientLevel level) {
			return ++age < 10;
		}

		@Override
		void render(FxDraw d, ClientLevel level) {
			float t = (age + d.partial) / 10f;
			float a = (float) Math.pow(1 - t, 1.2);
			double w = width * (1 + 1.5 * Math.sin(Math.min(1, t * 3) * Math.PI) * (1 - t));
			List<Vec3> pts = List.of(from, from.add(to.subtract(from).scale(0.5)), to);
			d.ribbon(FxDraw.GLOW, pts, new double[]{w * 3.5, w * 3.2, w * 3.0}, rgb(pal.core()), new float[]{0.55f * a, 0.5f * a, 0.45f * a}, 0);
			d.ribbon(FxDraw.STREAK, pts, new double[]{w, w, w}, rgb(pal.edge()), new float[]{a, a, a}, 0);
		}
	}

	/** A jagged lightning path between two points, flickering for a few ticks, with side forks. */
	static final class Lightning extends GrandLineFx.Effect {
		final List<List<Vec3>> bolts = new ArrayList<>();
		final SwordPalette pal;
		final int life;
		final double width;

		Lightning(Vec3 from, Vec3 to, double jag, int forks, double width, int life, SwordPalette pal) {
			super(-1);
			this.pal = pal;
			this.life = life;
			this.width = width;
			List<Vec3> main = jagged(from, to, jag, 12);
			bolts.add(main);
			for (int f = 0; f < forks; f++) {
				Vec3 at = main.get(2 + RANDOM.nextInt(Math.max(1, main.size() - 4)));
				Vec3 dir = to.subtract(from).normalize().add(RANDOM.nextGaussian() * 0.8, RANDOM.nextGaussian() * 0.3, RANDOM.nextGaussian() * 0.8).normalize();
				bolts.add(jagged(at, at.add(dir.scale(from.distanceTo(to) * (0.2 + RANDOM.nextDouble() * 0.25))), jag * 0.6, 6));
			}
		}

		static List<Vec3> jagged(Vec3 from, Vec3 to, double jag, int n) {
			List<Vec3> pts = new ArrayList<>();
			for (int i = 0; i <= n; i++) {
				double t = i / (double) n;
				Vec3 p = from.add(to.subtract(from).scale(t));
				if (i > 0 && i < n) {
					p = p.add(RANDOM.nextGaussian() * jag, RANDOM.nextGaussian() * jag * 0.4, RANDOM.nextGaussian() * jag);
				}
				pts.add(p);
			}
			return pts;
		}

		@Override
		boolean tick(ClientLevel level) {
			return ++age < life;
		}

		@Override
		void render(FxDraw d, ClientLevel level) {
			float time = age + d.partial;
			float flicker = (age % 2 == 0 ? 1f : 0.6f) * Math.max(0, 1 - time / life);
			for (int b = 0; b < bolts.size(); b++) {
				List<Vec3> pts = bolts.get(b);
				int n = pts.size();
				double[] w = new double[n], core = new double[n];
				float[] a = new float[n], a2 = new float[n];
				double scale = b == 0 ? 1 : 0.55;
				for (int i = 0; i < n; i++) {
					w[i] = width * 3 * scale;
					core[i] = width * scale;
					a[i] = 0.5f * flicker;
					a2[i] = flicker;
				}
				d.ribbon(FxDraw.GLOW, pts, w, rgb(pal.core()), a, 0);
				d.ribbon(FxDraw.STREAK, pts, core, rgb(pal.edge()), a2, 0);
			}
		}
	}

	// ============================================================================================ clouds

	/** A cluster of storm-cloud puffs over a point (static or carried by a travelling projectile). */
	static final class Cloud extends GrandLineFx.Effect implements GrandLineFx.Projectile {
		final Vec3 start;
		final Vec3 dir;
		final double speed;
		final double range;
		final double size;
		final int life;
		final SwordPalette pal;
		final double[][] puffs;
		Vec3 stopAt;
		int stopAge = -1;

		Cloud(int caster, Vec3 start, Vec3 dir, double speed, double range, double size, int life, SwordPalette pal) {
			super(caster);
			this.start = start;
			this.dir = dir.lengthSqr() < 1e-6 ? Vec3.ZERO : dir.normalize();
			this.speed = speed;
			this.range = range;
			this.size = size;
			this.life = life;
			this.pal = pal;
			this.puffs = new double[9][4];
			for (double[] p : puffs) {
				p[0] = RANDOM.nextGaussian() * size * 0.45;
				p[1] = RANDOM.nextGaussian() * size * 0.18;
				p[2] = RANDOM.nextGaussian() * size * 0.45;
				p[3] = size * (0.45 + RANDOM.nextDouble() * 0.35);
			}
		}

		@Override
		boolean followsCaster() {
			return false;
		}

		@Override
		public Vec3 position() {
			return stopAt != null ? stopAt : start.add(dir.scale(Math.min(range, speed * age)));
		}

		@Override
		public void end(Vec3 at) {
			if (stopAt == null) {
				stopAt = at;
				stopAge = age;
			}
		}

		@Override
		boolean tick(ClientLevel level) {
			age++;
			if (speed > 0 && stopAt == null && speed * age >= range) {
				end(position());
			}
			if (age % 3 == 0 && (stopAge < 0)) {
				Vec3 c = position();
				Vec3 a = c.add(RANDOM.nextGaussian() * size * 0.4, RANDOM.nextGaussian() * size * 0.15, RANDOM.nextGaussian() * size * 0.4);
				Vec3 b = c.add(RANDOM.nextGaussian() * size * 0.4, RANDOM.nextGaussian() * size * 0.15, RANDOM.nextGaussian() * size * 0.4);
				GrandLineFx.add(new Lightning(a, b, size * 0.08, 0, 0.05, 3, pal));
			}
			if (speed > 0) {
				return stopAge < 0 ? age < 200 : age - stopAge < 4;
			}
			return age < life;
		}

		@Override
		void render(FxDraw d, ClientLevel level) {
			float time = age + d.partial;
			Vec3 c = stopAt != null ? stopAt : start.add(dir.scale(Math.min(range, speed * time)));
			float fade;
			if (speed > 0) {
				fade = stopAge < 0 ? Math.min(1, time / 2f) : Math.max(0, 1 - (time - stopAge) / 4f);
			} else {
				fade = Math.min(1, time / 6f) * Math.min(1, Math.max(0, (life - time) / 8f));
			}
			for (int i = 0; i < puffs.length; i++) {
				double[] p = puffs[i];
				Vec3 at = c.add(p[0], p[1], p[2]);
				d.billboardSoft(FxDraw.CLOUD, at, p[3], time * 0.01 + i, FxDraw.rgba(pal.dark(), 0.85f * fade));
			}
			d.billboard(FxDraw.GLOW, true, c.add(0, -size * 0.15, 0), size * 0.6, 0,
					FxDraw.rgba(pal.core(), (0.18f + 0.12f * Mth.sin(time * 1.7f)) * fade));
		}
	}
}
