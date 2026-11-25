package com.grandline.client.fx;

import com.grandline.GrandLineMod;
import com.grandline.client.settings.CameraShake;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Reusable layered explosions (Bomb first; usable by guns, bosses and other fruits). One {@link Spec} sets each layer
 * independently: impact flash, procedurally deformed fireball mesh in the FIRE material (GLSL turbulence, heat gradient,
 * fresnel-cooled rim, noise dissolve), emissive inner core, expanding 3D pressure waves, heat-haze shimmer, flipbook
 * fire puffs turning into layered flipbook smoke, velocity-stretched embers, terrain-coloured bouncing debris, a
 * surface-aligned scorch decal (optional ground cracks), camera shake and duration. Shapes: radial, vertical plume,
 * directional (propulsion) and charged (bigger, more layers). Everything is client-side and cosmetic; terrain is never
 * touched. Density follows the VFX quality preset; distant explosions are culled or drawn with fewer facets.
 */
public final class ExplosionFx {
	public static final ResourceLocation SMOKE_SHEET = GrandLineMod.id("textures/fx/smoke_sheet.png");
	public static final ResourceLocation FIRE_SHEET = GrandLineMod.id("textures/fx/fireball_sheet.png");
	public static final ResourceLocation SCORCH = GrandLineMod.id("textures/fx/scorch.png");

	// Bomb palette (docs/BOMB.md).
	public static final int CORE = 0xFFF4BD, HOT = 0xFFCE54, FIRE = 0xFF8C2B, OUTER = 0xD94A23, CASING = 0x383B40, SMOKE = 0x77716C;

	public enum Shape {
		RADIAL, VERTICAL, DIRECTIONAL
	}

	/**
	 * @param radius     gameplay radius (the fireball itself stays well inside it)
	 * @param power      0..1 overall intensity (layers, counts, sizes, durations, shake)
	 * @param shockwaves expanding pressure rings (3D for 3+)
	 * @param scorch     leave a scorch decal on the ground below
	 * @param cracks     ground cracks with the scorch (big blasts)
	 * @param shake      max camera shake in degrees at the centre
	 */
	public record Spec(Shape shape, double radius, float power, int shockwaves, boolean scorch, boolean cracks, float shake) {
	}

	private ExplosionFx() {
	}

	/** Spawns an explosion at {@code at}; {@code dir} is the blast direction (up for ground blasts). */
	public static void spawn(ClientLevel level, Vec3 at, Vec3 dir, Spec spec) {
		Vec3 cam = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
		double dist = cam.distanceTo(at);
		if (dist > 128) {
			return;
		}
		Vec3 d = dir.lengthSqr() < 1e-6 ? new Vec3(0, 1, 0) : dir.normalize();
		GrandLineFx.add(new Blast(level, at, d, spec));
		RandomSource r = level.random;
		float p = spec.power();
		// Embers: hot, fast, stretched along their velocity, falling and bouncing (PhysicsSparks).
		GrandLineFx.add(new PhysicsSparks(level, at, spec.shape() == Shape.RADIAL ? new Vec3(0, 0.6, 0) : d, (int) (10 + 26 * p), 0.95,
				0.35 + 0.35 * p, 16 + (int) (14 * p), 0.05, HOT, OUTER, true));
		// Ground below: debris, dust and the scorch decal.
		BlockHitResult hit = level.clip(new ClipContext(at.add(0, 0.5, 0), at.subtract(0, 2.5 + spec.radius() * 0.4, 0), ClipContext.Block.COLLIDER,
				ClipContext.Fluid.NONE, Minecraft.getInstance().player));
		if (hit.getType() == HitResult.Type.BLOCK) {
			Vec3 g = hit.getLocation();
			SpringFx.ground(level, g, spec.radius() * 0.8, (int) (6 + 14 * p));
			if (spec.scorch()) {
				GrandLineFx.add(new Scorch(g.add(0, 0.02, 0), spec.radius() * (0.55 + 0.25 * p), 120 + (int) (80 * p)));
				if (spec.cracks()) {
					GrandLineFx.add(new SpringFx.Crater(level, g, spec.radius() * 0.45));
				}
			}
		}
		if (spec.shake() > 0) {
			CameraShake.impact(dist * 16 / Math.max(16, spec.radius() * 3), spec.shake());
		}
		for (int i = FxSprites.count((int) (2 + 4 * p)); i > 0; i--) {
			FxSprites.ember(at.add(FxSprites.randomDir(r).scale(spec.radius() * 0.2)), FxSprites.randomDir(r).scale(0.05).add(0, 0.06, 0),
					0.25 + 0.2 * p, 14 + r.nextInt(8), HOT, OUTER);
		}
	}

	/** The fireball, core, pressure waves, heat haze and flipbook puffs of one explosion. */
	static final class Blast extends GrandLineFx.Effect {
		private static final class Puff {
			Vec3 pos, prev, vel;
			final double size;
			final int delay, life, frame0;
			final boolean fire;
			final float roll;

			Puff(Vec3 pos, Vec3 vel, double size, int delay, int life, boolean fire, int frame0, float roll) {
				this.pos = pos;
				this.prev = pos;
				this.vel = vel;
				this.size = size;
				this.delay = delay;
				this.life = life;
				this.fire = fire;
				this.frame0 = frame0;
				this.roll = roll;
			}
		}

		final Vec3 at, dir;
		final Spec spec;
		final int fireLife, life;
		final double seed;
		final List<Puff> puffs = new ArrayList<>();

		Blast(ClientLevel level, Vec3 at, Vec3 dir, Spec spec) {
			super(-1);
			this.at = at;
			this.dir = dir;
			this.spec = spec;
			this.fireLife = 9 + (int) (12 * spec.power());
			RandomSource r = level.random;
			this.seed = r.nextDouble() * 100;
			int smokeLife = 40 + (int) (50 * spec.power());
			this.life = smokeLife + 12;
			double R = spec.radius();
			// Fire puffs burst out first, then heavier smoke rises and spreads (both flipbooks).
			int fire = FxSprites.count((int) (5 + 9 * spec.power()));
			for (int i = 0; i < fire; i++) {
				Vec3 v = FxSprites.randomDir(r).add(bias(0.6)).normalize().scale(R * (0.05 + r.nextDouble() * 0.05));
				puffs.add(new Puff(at, v, R * (0.22 + r.nextDouble() * 0.18), r.nextInt(3), 8 + r.nextInt(8), true, r.nextInt(4), r.nextFloat() * 6.28f));
			}
			int smoke = FxSprites.count((int) (6 + 14 * spec.power()));
			for (int i = 0; i < smoke; i++) {
				Vec3 v = FxSprites.randomDir(r).multiply(1, 0.4, 1).add(bias(0.5)).normalize().scale(R * (0.03 + r.nextDouble() * 0.035))
						.add(0, 0.02 + r.nextDouble() * 0.03, 0);
				puffs.add(new Puff(at.add(FxSprites.randomDir(r).scale(R * 0.2)), v, R * (0.3 + r.nextDouble() * 0.25), 3 + r.nextInt(6),
						smokeLife - 10 + r.nextInt(20), false, r.nextInt(4), r.nextFloat() * 6.28f));
			}
		}

		private Vec3 bias(double k) {
			return switch (spec.shape()) {
				case VERTICAL -> new Vec3(0, 1.4 * k, 0);
				case DIRECTIONAL -> dir.scale(1.2 * k);
				default -> new Vec3(0, 0.35 * k, 0);
			};
		}

		@Override
		boolean tick(ClientLevel level) {
			age++;
			for (Puff p : puffs) {
				if (age > p.delay) {
					p.prev = p.pos;
					p.pos = p.pos.add(p.vel);
					p.vel = p.vel.scale(p.fire ? 0.82 : 0.9).add(0, p.fire ? 0.004 : 0.006, 0);
				}
			}
			return age < life;
		}

		@Override
		void render(FxDraw d, ClientLevel level) {
			double camDist = at.distanceTo(d.cameraPosition());
			if (camDist > 128) {
				return;
			}
			float a = age + d.partial;
			double R = spec.radius();
			float p = spec.power();
			// 1. Impact frame: a brief bright core flash (small, controlled; no screen flash).
			if (a < 3) {
				float k = 1 - a / 3f;
				d.billboard(FxDraw.GLOW, true, at, Math.min(3, R * (0.35 + 0.25 * p)) * (0.6 + 0.4 * k), 0, FxDraw.rgba(CORE, 0.7f * k));
			}
			// 2. Fireball: deformed, rising, dissolving; an additive inner core under a translucent turbulent shell.
			float t = Math.min(1, a / fireLife);
			if (t < 1) {
				double grow = WindMesh.ease(Math.min(1, a / 4f));
				double rf = Math.min(2.6, R * (0.26 + 0.08 * p)) * (0.55 + 0.45 * grow) * (1 + 0.25 * t);
				Vec3 c = at.add(bias(1).scale(R * 0.35 * t));
				double stretch = spec.shape() == Shape.VERTICAL ? 1.7 : 1;
				int lod = camDist > 48 ? 6 : camDist > 20 ? 9 : 12;
				int lat = Math.max(5, (int) (lod * WindMesh.detail())), lon = lat * 2;
				float dissolve = Math.max(0, (t - 0.35f) / 0.65f);
				WindRender.with(WindRender.Material.FIRE, () -> {
					fireball(d, c, rf, stretch, a, lat, lon, heatColour(t), 0.68f * (1 - t * 0.4f), dissolve, false);
					if (t < 0.6f) {
						fireball(d, c, rf * 0.6, stretch, a + 7, Math.max(4, lat - 2), Math.max(8, lon - 4), CORE, 0.55f * (1 - t / 0.6f), 0, true);
					}
				});
			}
			// 3. Pressure waves: rings racing out along the ground (and through the air for big ones).
			for (int k = 0; k < spec.shockwaves(); k++) {
				float tk = (a - k * 2.5f) / (8 + 4 * p);
				if (tk <= 0 || tk >= 1) {
					continue;
				}
				double rr = R * (0.9 + 0.7 * p) * WindMesh.ease(Math.sqrt(tk));
				Vec3[] b;
				Vec3 c = at;
				if (k < 2) {
					b = WindMesh.basis(spec.shape() == Shape.DIRECTIONAL && k == 1 ? dir : new Vec3(0, 1, 0));
				} else {
					double ang = k * 1.1 + seed;
					b = WindMesh.basis(new Vec3(Math.cos(ang), 0.3, Math.sin(ang)));
				}
				float al = (0.42f - 0.08f * k) * (1 - tk);
				int col = k == 0 ? HOT : 0xF2E2C8;
				float ft = tk;
				WindRender.with(WindRender.Material.WIND, () -> WindMesh.ring(d, true, c, b[0], b[1], Math.max(0, rr - R * 0.25), rr, 0, 360,
						col, al, ft * 0.7f, ft));
			}
			// 4. Heat shimmer: a soft warm glow that lingers over the blast (no opaque geometry).
			if (a < 20) {
				float h = 1 - a / 20f;
				d.billboard(FxDraw.GLOW, true, at.add(bias(1).scale(R * 0.4)), R * 0.5, 0, FxDraw.rgba(FIRE, 0.18f * h * h));
			}
			// 5. Flipbook puffs: fire tongues (additive) then smoke (soft), each fading in and dissolving out.
			for (Puff pf : puffs) {
				float pa = a - pf.delay;
				if (pa <= 0 || pa >= pf.life) {
					continue;
				}
				float pt = pa / pf.life;
				Vec3 pos = pf.prev.lerp(pf.pos, d.partial);
				int frame = Math.min(3, pf.frame0 / 2 + (int) (pt * 4));
				float u0 = frame / 4f, u1 = (frame + 1) / 4f;
				if (pf.fire) {
					double s = pf.size * (0.6 + 0.6 * pt);
					int col = FxSprites.lerpColor(HOT, OUTER, pt);
					d.billboard(FIRE_SHEET, true, pos, s, pf.roll + pt, FxDraw.rgba(col, 0.85f * (1 - pt) * Math.min(1, pa / 2)), u0, u1, 0, 1);
				} else {
					double s = pf.size * (0.7 + 1.1 * WindMesh.ease(pt));
					int col = FxSprites.lerpColor(0x3C3836, SMOKE, Math.min(1, pt * 1.6f));
					float al = 0.55f * Math.min(1, pa / 5) * (1 - pt) * (1 - pt * 0.3f);
					d.billboard(SMOKE_SHEET, false, pos, s, pf.roll + pt * 0.5, FxDraw.rgba(col, al), u0, u1, 0, 1);
				}
			}
		}

		static int heatColour(float t) {
			return t < 0.3f ? FxSprites.lerpColor(CORE, HOT, t / 0.3f) : t < 0.65f ? FxSprites.lerpColor(HOT, FIRE, (t - 0.3f) / 0.35f)
					: FxSprites.lerpColor(FIRE, OUTER, (t - 0.65f) / 0.35f);
		}

		/** A noise-displaced sphere (lat × lon quads); v carries heat (cools with age), fresnel cools the rim to smoke. */
		void fireball(FxDraw d, Vec3 c, double r, double stretch, float a, int lat, int lon, int rgb, float alpha, float dissolve, boolean glow) {
			for (int i = 0; i < lat; i++) {
				for (int j = 0; j < lon; j++) {
					Vec3[] q = new Vec3[4];
					float[] fr = new float[4];
					int[][] idx = {{i, j}, {i + 1, j}, {i + 1, j + 1}, {i, j + 1}};
					for (int k = 0; k < 4; k++) {
						double th = Math.PI * idx[k][0] / lat, ph = Math.PI * 2 * idx[k][1] / lon;
						Vec3 n = new Vec3(Math.sin(th) * Math.cos(ph), Math.cos(th), Math.sin(th) * Math.sin(ph));
						double disp = 1 + 0.16 * Math.sin(3 * th + a * 0.25 + seed) * Math.sin(2 * ph + a * 0.18)
								+ 0.09 * Math.sin(5 * ph - a * 0.3 + seed * 2) * Math.sin(4 * th);
						q[k] = c.add(n.x * r * disp, n.y * r * disp * stretch + (n.y > 0 ? r * 0.12 : 0), n.z * r * disp);
						fr[k] = WindRender.fresnel(d, q[k], n);
					}
					float vv = Math.min(1, 0.1f + a / (fireLife * 1.3f));
					float[] us = {j / (float) lon, j / (float) lon, (j + 1) / (float) lon, (j + 1) / (float) lon};
					float[] vs = {vv, vv + 0.08f, vv + 0.08f, vv};
					float[] al = {alpha, alpha, alpha, alpha};
					WindRender.quad(d, glow, q, us, vs, rgb, al, dissolve, fr);
				}
			}
		}
	}

	/** A charred scorch decal on the ground (cosmetic, fades), with a dying ember glow at first. */
	static final class Scorch extends GrandLineFx.Effect {
		final Vec3 at;
		final double radius;
		final int life;
		final float roll;

		Scorch(Vec3 at, double radius, int life) {
			super(-1);
			this.at = at;
			this.radius = radius;
			this.life = life;
			this.roll = (float) (Math.random() * Math.PI * 2);
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
			float a = t < 0.7f ? 0.85f : 0.85f * (1 - (t - 0.7f) / 0.3f);
			double c = Math.cos(roll) * radius, s = Math.sin(roll) * radius;
			Vec3 p0 = at.add(-c + s, 0, -s - c), p1 = at.add(c + s, 0, s - c), p2 = at.add(c - s, 0, s + c), p3 = at.add(-c - s, 0, -s + c);
			d.quadSoft(SCORCH, p0, p1, p2, p3, new float[]{1, 1, 1, a});
			float glow = Math.max(0, 1 - (age + d.partial) / 30f);
			if (glow > 0) {
				Vec3 up = new Vec3(0, 0.01, 0);
				d.quad(FxDraw.GLOW, true, p0.add(up), p1.add(up), p2.add(up), p3.add(up), FxDraw.rgba(OUTER, 0.45f * glow), 0, 1, 0, 1);
			}
		}
	}
}
