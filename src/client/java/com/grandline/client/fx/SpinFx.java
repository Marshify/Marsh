package com.grandline.client.fx;

import com.grandline.audio.GrandLineSounds;
import com.grandline.client.audio.GrandLineAudio;
import com.grandline.client.settings.CameraShake;
import com.grandline.fruit.spin.SpinMoves;
import com.grandline.network.payload.FxEventPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Spin Fruit visuals, driven by the server's beats (see SpinMoves). The look is compressed air: translucent, fast,
 * sharp-edged surfaces built from {@link WindMesh} geometry drawn through {@link WindRender} (custom airflow shader
 * with a plain-blend fallback), ribbon trails, velocity-streaked wind particles and terrain-coloured debris with
 * simple bounce physics. Poses come from {@link BodyMotion}.
 *
 * <ul>
 *   <li>Razor Wind: overlapping crescent blades thrown from the whirling hands, arm-anchored ribbon trails and wind
 *   streaks, denser the longer the key is held; white flashes on hits.</li>
 *   <li>Tornado Assault: a funnel of independently rotating, counter-rotating spiral bands round the spinning player,
 *   spiral ribbons, ground currents, pressure rings and orbiting debris; ends in a pressure wave and dissolves.</li>
 *   <li>Air Slasher: 3D crescent blades with bright leading edges and tip trails; a larger layered finishing blade
 *   with a shockwave on impact.</li>
 *   <li>Helicopter Flight: wind cones swept by the rotor arms, circular hand trails, a lift ring, downwash and
 *   ground dust when hovering low; spins down when it ends.</li>
 * </ul>
 * Effects farther than {@link #CULL} blocks from the camera aren't drawn; particle counts and mesh detail follow the
 * quality preset; gameplay never depends on any of it.
 */
final class SpinFx {
	static final double CULL = 80;

	private SpinFx() {
	}

	static boolean handles(String effect) {
		return effect.startsWith("spin_");
	}

	// ------------------------------------------------------------------------------------------------ beats

	static void handle(FxEventPayload p, ClientLevel level, Entity caster, boolean own) {
		Minecraft mc = Minecraft.getInstance();
		int id = p.caster();
		float age = caster == null ? 0 : caster.tickCount;
		switch (p.effect()) {
			case "spin_razor_start" -> {
				if (caster != null) {
					BodyMotion.start(id, BodyMotion.Kind.ARM_WHIRL, age);
					RazorField field = new RazorField(id);
					GrandLineFx.add(field);
					GrandLineAudio.loop(GrandLineSounds.SPIN_RAZOR_LOOP, () -> {
						Entity c = field.caster(level);
						return c == null ? null : c.position();
					}, () -> !field.done && !field.ending, 0.85f, () -> 0.9 + 0.25 * BodyMotion.intensity(id, field.age + age), null);
				}
				GrandLineAudio.play(GrandLineSounds.SPIN_RAZOR_CHARGE, p.pos(), 1f, 1f);
			}
			case "spin_razor_hit" -> {
				GrandLineFx.add(new Flash(p.pos(), 0.7, 3));
				GrandLineAudio.play(GrandLineSounds.SPIN_RAZOR_IMPACT, p.pos(), 0.8f, 0.95f + level.random.nextFloat() * 0.15f);
			}
			case "spin_razor_end" -> {
				BodyMotion.stop(id, BodyMotion.Kind.ARM_WHIRL, age);
				RazorField field = GrandLineFx.find(RazorField.class, id);
				if (field != null) {
					field.ending = true;
				}
				GrandLineAudio.play(GrandLineSounds.SPIN_RAZOR_AFTERMATH, p.pos(), 0.9f, 1f);
			}
			case "spin_tornado_start" -> {
				if (caster != null) {
					BodyMotion.start(id, BodyMotion.Kind.BODY_SPIN, age);
				}
				GrandLineFx.add(new PressureRing(p.pos().add(0, 0.1, 0), 2.5, 8, false));
				GrandLineAudio.play(GrandLineSounds.SPIN_TORNADO_CAST, p.pos(), 1f, 1f);
			}
			case "spin_tornado" -> {
				if (caster != null) {
					Tornado t = new Tornado(id, (int) p.range(), p.size());
					GrandLineFx.add(t);
					GrandLineAudio.loop(GrandLineSounds.SPIN_TORNADO_LOOP, () -> {
						Entity c = t.caster(level);
						return c == null ? null : c.position();
					}, () -> !t.done && !t.ending, 1f, () -> {
						Entity c = t.caster(level);
						return c == null ? 1.0 : 0.9 + Math.min(0.25, c.getDeltaMovement().horizontalDistance() * 0.4);
					}, null);
				}
			}
			case "spin_tornado_hit" -> {
				GrandLineFx.add(new Flash(p.pos(), 0.55, 3));
				GrandLineAudio.play(GrandLineSounds.SPIN_TORNADO_IMPACT, p.pos(), 0.8f, 1f);
			}
			case "spin_tornado_end" -> {
				BodyMotion.stop(id, BodyMotion.Kind.BODY_SPIN, age);
				Tornado t = GrandLineFx.find(Tornado.class, id);
				if (t != null) {
					t.end(level);
				}
				if (p.size() > 0) { // the full finisher (not an interruption)
					GrandLineFx.add(new PressureRing(p.pos().add(0, 0.15, 0), SpinMoves.TORNADO_RADIUS + 2.5, 12, true));
					GrandLineAudio.play(GrandLineSounds.SPIN_TORNADO_AFTERMATH, p.pos(), 1.1f, 1f);
					shake(mc, p.pos(), own ? 1.2f : 0.8f);
				}
			}
			case "spin_slasher_windup" -> {
				if (caster != null) {
					BodyMotion.slash(id, age + 4, true, false); // twist back, ready for the first sweep
				}
			}
			case "spin_slasher_finale" -> {
				if (caster != null) {
					BodyMotion.slash(id, age + 6, false, true); // both arms up and back, held until the release
				}
			}
			case "spin_blade" -> {
				int n = (int) p.size();
				boolean last = n == 5;
				if (caster != null) {
					BodyMotion.slash(id, age, !last && n % 2 == 1, last);
				}
				Blade b = new Blade(id, p.pos(), p.dir(), p.speed(), p.range(), n);
				GrandLineFx.add(b);
				GrandLineAudio.play(last ? GrandLineSounds.SPIN_SLASHER_CAST_FINAL : GrandLineSounds.SPIN_SLASHER_CAST, p.pos(), 1f,
						last ? 1f : 1f + n * 0.04f);
				GrandLineAudio.loop(GrandLineSounds.SPIN_SLASHER_TRAVEL, b::position, () -> !b.done, last ? 0.9f : 0.45f, () -> last ? 0.85 : 1.1, null);
				if (last) {
					shake(mc, p.pos(), own ? 0.6f : 0.3f);
				}
			}
			case "spin_blade_hit" -> {
				int n = (int) p.size();
				boolean last = n == 5;
				GrandLineFx.endProjectile(id, p.pos());
				int kind = (int) p.speed();
				if (kind > 0) {
					GrandLineFx.add(new Flash(p.pos(), last ? 1.6 : 0.7, last ? 4 : 3));
					GrandLineFx.add(new PressureRing(p.pos(), last ? 4.5 : 1.6, last ? 12 : 7, last));
					if (last) {
						GrandLineFx.add(new ImpactFrame(p.pos()));
					}
					shake(mc, p.pos(), last ? (own ? 1.6f : 1.1f) : 0.25f);
				}
				GrandLineAudio.play(last ? GrandLineSounds.SPIN_SLASHER_IMPACT_FINAL : GrandLineSounds.SPIN_SLASHER_IMPACT, p.pos(),
						kind == 0 ? 0.4f : 1f, kind == 0 ? 1.3f : 1f);
			}
			case "spin_heli_start" -> {
				if (caster != null) {
					BodyMotion.start(id, BodyMotion.Kind.ROTOR, age);
					Rotor rotor = new Rotor(id);
					GrandLineFx.add(rotor);
					GrandLineAudio.loop(GrandLineSounds.SPIN_HELI_LOOP, () -> {
						Entity c = rotor.caster(level);
						return c == null ? null : c.position();
					}, () -> !rotor.done && !rotor.ending, 0.85f, () -> {
						Entity c = rotor.caster(level);
						return c == null ? 1.0 : 0.92 + Math.min(0.2, c.getDeltaMovement().length() * 0.5);
					}, GrandLineSounds.SPIN_HELI_AFTERMATH);
				}
				GrandLineAudio.play(GrandLineSounds.SPIN_HELI_CAST, p.pos(), 1f, 1f);
			}
			default -> {
			}
		}
	}

	/** Helicopter Flight ends (the shared flight_end beat). */
	static void flightEnd(int id, Entity caster) {
		Rotor rotor = GrandLineFx.find(Rotor.class, id);
		if (rotor != null) {
			BodyMotion.stop(id, BodyMotion.Kind.ROTOR, caster == null ? 0 : caster.tickCount);
			rotor.ending = true;
		}
	}

	private static void shake(Minecraft mc, Vec3 at, float degrees) {
		if (mc.player != null) {
			CameraShake.impact(mc.player.position().distanceTo(at), degrees);
		}
	}

	private static boolean culled(FxDraw d, Vec3 at) {
		return at.distanceToSqr(d.cameraPosition()) > CULL * CULL;
	}

	/** A ribbon through {@code pts} (newest first), thinning and fading toward the tail. */
	static void trail(FxDraw d, boolean glow, List<Vec3> pts, double width, int rgb, float alpha) {
		int n = pts.size();
		if (n < 2) {
			return;
		}
		double[] w = new double[n];
		float[] a = new float[n];
		for (int i = 0; i < n; i++) {
			double t = i / (double) (n - 1);
			w[i] = width * (1 - t * 0.8);
			a[i] = (float) (alpha * (1 - t));
		}
		float[] c = FxDraw.rgba(rgb, 1f);
		if (glow) {
			d.ribbon(FxDraw.STREAK, pts, w, c, a, 0);
		} else {
			d.ribbonSoft(FxDraw.STREAK, pts, w, c, a, 0);
		}
	}

	/** A fast, thin wind streak sprite (velocity-stretched look from a streak texture moving along its direction). */
	static void streak(Vec3 pos, Vec3 vel, int life) {
		FxSprites.sprite(FxDraw.SPARK, true, pos, vel, 0.05, -0.002, 0, 0.92, life, WindRender.CORE, WindRender.AIRFLOW, 0.55f);
	}

	/** Terrain colour under a point, blended with the debris palette colour. */
	static int groundColour(ClientLevel level, Vec3 at) {
		BlockPos pos = BlockPos.containing(at.x, at.y - 0.2, at.z);
		BlockState state = level.getBlockState(pos);
		int c = state.isAir() ? WindRender.DEBRIS : state.getMapColor(level, pos).col;
		return FxSprites.lerpColor(c, WindRender.DEBRIS, 0.35f);
	}

	// ------------------------------------------------------------------------------------------------ Razor Wind

	/** The cutting field in front of the whirling arms, until the end beat (then a short wind-down). */
	static final class RazorField extends GrandLineFx.Effect {
		record Gust(Vec3 origin, Vec3 dir, double roll, int born, boolean left) {
		}

		final List<Gust> gusts = new ArrayList<>();
		final List<Vec3> rightTrail = new ArrayList<>(), leftTrail = new ArrayList<>();
		boolean ending;
		int endAge;

		RazorField(int caster) {
			super(caster);
		}

		@Override
		void cancel() {
			ending = true;
		}

		@Override
		boolean tick(ClientLevel level) {
			age++;
			gusts.removeIf(g -> age - g.born > 6);
			if (ending && ++endAge > 10 && gusts.isEmpty()) {
				return false;
			}
			Entity c = caster(level);
			if (c == null) {
				return false;
			}
			if (!ending) {
				float k = BodyMotion.intensity(caster, c.tickCount);
				RandomSource r = level.random;
				int count = Math.max(1, FxSprites.count((int) Math.round(2 + 3 * k)));
				Vec3 look = c.getLookAngle();
				for (int i = 0; i < count; i++) {
					boolean left = (age + i) % 2 == 0;
					double yaw = (r.nextDouble() - 0.5) * 0.8, pitch = (r.nextDouble() - 0.5) * 0.45;
					Vec3 dir = rotate(look, yaw, pitch);
					gusts.add(new Gust(BodyMotion.hand(c, 0, left), dir, r.nextDouble() * Math.PI, age, left));
				}
				for (int i = FxSprites.count(3); i > 0; i--) {
					Vec3 from = BodyMotion.hand(c, 0, r.nextBoolean());
					streak(from, rotate(look, (r.nextDouble() - 0.5) * 0.9, (r.nextDouble() - 0.5) * 0.5).scale(0.9 + r.nextDouble() * 0.5), 6);
				}
			}
			return true;
		}

		@Override
		void render(FxDraw d, ClientLevel level) {
			Entity c = caster(level);
			if (c == null || culled(d, c.position())) {
				return;
			}
			float age = c.tickCount + d.partial;
			float k = ending ? Math.max(0, 1 - (endAge + d.partial) / 10f) : 0.55f + 0.45f * BodyMotion.intensity(caster, age);
			// Arm-anchored circular trails: the hands' last positions round their whirl.
			sampleTrail(rightTrail, BodyMotion.hand(c, d.partial, false));
			sampleTrail(leftTrail, BodyMotion.hand(c, d.partial, true));
			trail(d, true, rightTrail, 0.16, WindRender.EDGE, 0.55f * k);
			trail(d, true, leftTrail, 0.16, WindRender.EDGE, 0.55f * k);
			// The blades: crescents flying out, growing, dissolving at their edges.
			for (Gust g : gusts) {
				double t = Mth.clamp((this.age - g.born + d.partial) / 6.0, 0, 1);
				Vec3 centre = g.origin.add(g.dir.scale(t * SpinMoves.RAZOR_RANGE));
				Vec3[] b = WindMesh.basis(g.dir);
				Vec3 side = b[0].scale(Math.cos(g.roll)).add(b[1].scale(Math.sin(g.roll)));
				Vec3 up = g.dir.cross(side).normalize();
				double radius = 0.35 + t * 1.1;
				float alpha = (float) (k * (1 - t) * 0.85);
				WindMesh.crescent(d, false, centre.subtract(g.dir.scale(radius * 0.8)), g.dir, side, up, radius, radius * 0.45, 110,
						WindRender.AIRFLOW, alpha, (float) Math.max(0, t * 1.3 - 0.3), age * 0.15);
				WindMesh.crescent(d, true, centre.subtract(g.dir.scale(radius * 0.8)), g.dir, side, up, radius, radius * 0.12, 100,
						WindRender.CORE, alpha * 0.7f, (float) Math.max(0, t * 1.3 - 0.3), age * 0.15);
			}
		}

		void sampleTrail(List<Vec3> list, Vec3 p) {
			if (list.isEmpty() || list.get(0).distanceToSqr(p) > 0.0004) {
				list.add(0, p);
			}
			while (list.size() > 4 + (int) (6 * WindMesh.detail())) {
				list.remove(list.size() - 1);
			}
		}
	}

	/** {@code v} turned by {@code yaw} about the vertical and {@code pitch} about its horizontal side. */
	static Vec3 rotate(Vec3 v, double yaw, double pitch) {
		Vec3 r = v.yRot((float) yaw);
		Vec3 side = r.cross(new Vec3(0, 1, 0));
		if (side.lengthSqr() < 1e-6) {
			return r;
		}
		side = side.normalize();
		double c = Math.cos(pitch), s = Math.sin(pitch);
		return r.scale(c).add(side.cross(r).scale(s)).add(side.scale(side.dot(r) * (1 - c))).normalize();
	}

	// ------------------------------------------------------------------------------------------------ Tornado Assault

	/** A cosmetic debris or dust particle: orbits while the tornado holds it, then flies out and bounces. */
	static final class Debris {
		double angle, radius, height, spin;
		Vec3 pos, vel = Vec3.ZERO, prev;
		final int colour;
		final double size;
		boolean free;
		int life = 40;

		Debris(double angle, double radius, double height, double spin, int colour, double size) {
			this.angle = angle;
			this.radius = radius;
			this.height = height;
			this.spin = spin;
			this.colour = colour;
			this.size = size;
		}

		/** Free flight: gravity, drag, and a damped bounce off the ground (block tops), all client-side. */
		void physics(ClientLevel level) {
			prev = pos;
			vel = vel.add(0, -0.04, 0).scale(0.97);
			Vec3 next = pos.add(vel);
			BlockPos below = BlockPos.containing(next);
			if (vel.y < 0 && !level.getBlockState(below).getCollisionShape(level, below).isEmpty()) {
				double top = below.getY() + level.getBlockState(below).getCollisionShape(level, below).max(net.minecraft.core.Direction.Axis.Y);
				next = new Vec3(next.x, top + 0.02, next.z);
				vel = new Vec3(vel.x * 0.6, -vel.y * 0.4, vel.z * 0.6); // bounce
			}
			pos = next;
			life--;
		}
	}

	/** The whirlwind round the spinning player: layered rotating bands, spiral ribbons, currents and debris. */
	static final class Tornado extends GrandLineFx.Effect {
		final int duration;
		final double radius;
		final List<Debris> debris = new ArrayList<>();
		final List<Vec3> ribbonA = new ArrayList<>(), ribbonB = new ArrayList<>();
		boolean ending;
		int endAge;
		Vec3 lastCentre;
		double speed;

		Tornado(int caster, int duration, double radius) {
			super(caster);
			this.duration = duration;
			this.radius = radius;
		}

		@Override
		void cancel() {
			ending = true;
		}

		void end(ClientLevel level) {
			if (ending) {
				return;
			}
			ending = true;
			// Debris is thrown outward and falls naturally.
			for (Debris b : debris) {
				if (!b.free && b.pos != null && lastCentre != null) {
					b.free = true;
					Vec3 out = b.pos.subtract(lastCentre).multiply(1, 0, 1);
					out = out.lengthSqr() < 1e-4 ? new Vec3(1, 0, 0) : out.normalize();
					Vec3 tangent = new Vec3(-out.z, 0, out.x).scale(b.spin * b.radius);
					b.vel = out.scale(0.35 + level.random.nextDouble() * 0.25).add(tangent).add(0, 0.2 + level.random.nextDouble() * 0.25, 0);
				}
			}
		}

		@Override
		boolean tick(ClientLevel level) {
			age++;
			Entity c = caster(level);
			if (c == null && !ending) {
				ending = true;
			}
			if (ending) {
				endAge++;
			}
			if (c != null) {
				Vec3 centre = c.position();
				speed = lastCentre == null ? 0 : centre.subtract(lastCentre).horizontalDistance();
				lastCentre = centre;
				if (!ending) {
					// New debris and dust picked up from the ground, coloured like the ground.
					RandomSource r = level.random;
					int colour = groundColour(level, centre);
					for (int i = FxSprites.count(3); i > 0; i--) {
						debris.add(new Debris(r.nextDouble() * Math.PI * 2, 0.8 + r.nextDouble() * 2.2, 0.1, 0.25 + r.nextDouble() * 0.2,
								r.nextFloat() < 0.6 ? colour : WindRender.HAZE, 0.05 + r.nextDouble() * 0.06));
					}
					int cap = (int) (90 * WindMesh.detail());
					while (debris.size() > cap) {
						debris.remove(0);
					}
				}
			}
			for (Debris b : debris) {
				if (b.free) {
					b.physics(level);
				} else if (lastCentre != null) {
					b.angle += b.spin;
					b.height = Math.min(4.2, b.height + 0.08 + b.spin * 0.1);
					b.radius = Math.min(3.4, b.radius + 0.02);
					b.prev = b.pos;
					b.pos = lastCentre.add(Math.cos(b.angle) * b.radius, b.height, Math.sin(b.angle) * b.radius);
					if (b.height >= 4.2) {
						b.free = true; // flung out of the top
						b.vel = new Vec3(Math.cos(b.angle), 0.1, Math.sin(b.angle)).scale(0.3);
					}
				}
			}
			debris.removeIf(b -> b.life <= 0 || b.pos == null && b.free);
			return !(ending && endAge > 14 && debris.isEmpty());
		}

		@Override
		void render(FxDraw d, ClientLevel level) {
			Entity c = caster(level);
			Vec3 base = c != null ? c.getPosition(d.partial) : lastCentre;
			if (base == null || culled(d, base)) {
				return;
			}
			float t = age + d.partial;
			float grow = (float) WindMesh.ease(t / 6.0);
			float fade = ending ? Math.max(0, 1 - (endAge + d.partial) / 14f) : 1f;
			float dissolve = ending ? (endAge + d.partial) / 14f : 0;
			float vel = (float) Mth.clamp(speed / SpinMoves.TORNADO_SPEED, 0, 1);
			float k = grow * fade * (0.75f + 0.25f * vel);
			double breathe = 1 + 0.08 * Math.sin(t * 0.45);
			double height = 4.6;
			if (k > 0.01) {
				// Layers: inner fast, outer slower and counter-rotating; each layer is a few spiral bands with gaps,
				// so the player stays visible inside.
				int layers = WindMesh.detail() >= 1.4 ? 4 : WindMesh.detail() >= 1 ? 3 : 2;
				for (int L = 0; L < layers; L++) {
					double dirSign = L % 2 == 0 ? 1 : -1;
					double omega = (0.55 - L * 0.08) * dirSign;
					double rb = (0.55 + L * 0.25) * breathe, rt = (radius * 0.55 + L * 0.45) * breathe;
					int col = L == 0 ? WindRender.EDGE : L == 1 ? WindRender.AIRFLOW : WindRender.VORTEX;
					for (int band = 0; band < 3; band++) {
						double ang = t * omega + band * Math.PI * 2 / 3 + L;
						double h0 = band * 1.2 + 0.1, h1 = h0 + 1.9;
						WindMesh.vortexBand(d, L == 0, base, h0, h1, rb, rt, height, ang, 150, 0.35 * dirSign, col,
								k * (0.26f - L * 0.05f), dissolve, t * 0.08 * dirSign);
					}
				}
				// Two counter-rotating spiral ribbons climbing the funnel.
				helix(d, base, t, 1, height, breathe, k);
				helix(d, base, t, -1, height, breathe, k);
				// Ground currents: arcs racing round the base.
				for (int i = 0; i < 3; i++) {
					WindMesh.ring(d, false, base.add(0, 0.06, 0), new Vec3(1, 0, 0), new Vec3(0, 0, 1), 1.6 + i * 0.9, 1.75 + i * 0.9,
							t * (0.5 - i * 0.12) + i * 2, 110, WindRender.HAZE, k * 0.2f, dissolve, t * 0.1);
				}
				// Pressure rings pulsing outward from the base twice a second.
				double pulse = (t % 10) / 10.0;
				WindMesh.wall(d, false, base, 1 + pulse * (radius + 1.5), 0.5, WindRender.EDGE, k * (float) (1 - pulse) * 0.18f, dissolve, t * 0.05);
			}
			// Debris and dust (velocity-stretched by drawing a short segment from last to current position).
			for (Debris b : debris) {
				if (b.pos == null) {
					continue;
				}
				Vec3 at = b.prev == null ? b.pos : b.prev.lerp(b.pos, d.partial);
				float a = Math.min(1f, b.life / 10f);
				d.billboardSoft(FxDraw.CLOUD, at, b.size * 1.6, 0, FxDraw.rgba(b.colour, 0.85f * a));
			}
		}

		private void helix(FxDraw d, Vec3 base, float t, int sign, double height, double breathe, float k) {
			List<Vec3> pts = new ArrayList<>();
			int n = WindMesh.segs(18);
			for (int i = 0; i <= n; i++) {
				double h = height * (1 - i / (double) n);
				double r = (0.6 + (radius * 0.6 - 0.6) * Math.pow(h / height, 0.8)) * breathe;
				double a = sign * (t * 0.7 + h * 1.6);
				pts.add(base.add(Math.cos(a) * r, h, Math.sin(a) * r));
			}
			trail(d, true, pts, 0.22, WindRender.CORE, 0.45f * k);
		}
	}

	/** An expanding pressure wave: a flat ring on the ground and, for big ones, an upright wall. */
	static final class PressureRing extends GrandLineFx.Effect {
		final Vec3 at;
		final double radius;
		final int life;
		final boolean big;

		PressureRing(Vec3 at, double radius, int life, boolean big) {
			super(-1);
			this.at = at;
			this.radius = radius;
			this.life = life;
			this.big = big;
		}

		@Override
		boolean tick(ClientLevel level) {
			if (age == 0 && big) {
				RandomSource r = level.random;
				int colour = groundColour(level, at);
				for (int i = FxSprites.count(16); i > 0; i--) {
					double a = r.nextDouble() * Math.PI * 2;
					Vec3 out = new Vec3(Math.cos(a), 0, Math.sin(a));
					FxSprites.sprite(FxDraw.CLOUD, false, at.add(out.scale(0.6)), out.scale(0.3 + r.nextDouble() * 0.2).add(0, 0.03, 0),
							0.35, 0.03, -0.004, 0.88, 22 + r.nextInt(10), colour, WindRender.HAZE, 0.5f);
				}
			}
			return ++age < life;
		}

		@Override
		void render(FxDraw d, ClientLevel level) {
			if (culled(d, at)) {
				return;
			}
			float t = (age + d.partial) / life;
			double r = radius * WindMesh.ease(Math.sqrt(t));
			float a = (1 - t);
			WindMesh.ring(d, true, at, new Vec3(1, 0, 0), new Vec3(0, 0, 1), Math.max(0, r - 0.35), r, 0, 360, WindRender.EDGE, a * 0.4f, t * 0.8f, t);
			if (big) {
				WindMesh.wall(d, false, at, r, 1.4 * (1 - t * 0.5), WindRender.AIRFLOW, a * 0.4f, t * 0.9f, t * 2);
			}
		}
	}

	/** A brief white flash on a hit: small, short, never blinding. */
	static final class Flash extends GrandLineFx.Effect {
		final Vec3 at;
		final double size;
		final int life;

		Flash(Vec3 at, double size, int life) {
			super(-1);
			this.at = at;
			this.size = size;
			this.life = life;
		}

		@Override
		boolean tick(ClientLevel level) {
			if (age == 0) {
				RandomSource r = level.random;
				for (int i = FxSprites.count(5); i > 0; i--) {
					streak(at, FxSprites.randomDir(r).scale(0.25 + r.nextDouble() * 0.2), 5);
				}
			}
			return ++age < life;
		}

		@Override
		void render(FxDraw d, ClientLevel level) {
			float t = (age + d.partial) / life;
			d.billboard(FxDraw.GLOW, true, at, size * (0.6 + t * 0.6), 0, FxDraw.rgba(WindRender.CORE, 0.65f * (1 - t)));
		}
	}

	/** The finishing blade's impact frame: a short bright burst ring that freezes for two ticks, then dissolves. */
	static final class ImpactFrame extends GrandLineFx.Effect {
		final Vec3 at;

		ImpactFrame(Vec3 at) {
			super(-1);
			this.at = at;
		}

		@Override
		boolean tick(ClientLevel level) {
			return ++age < 6;
		}

		@Override
		void render(FxDraw d, ClientLevel level) {
			float t = age < 2 ? 0 : (age - 2 + d.partial) / 4f; // held for two ticks: the "frame"
			Vec3 toCam = d.cameraPosition().subtract(at).normalize();
			Vec3[] b = WindMesh.basis(toCam);
			WindMesh.ring(d, true, at.add(toCam.scale(0.3)), b[0], b[1], 0.2, 2.2 + t * 1.5, 0, 360, WindRender.CORE, 0.8f * (1 - t), t, 0);
		}
	}

	// ------------------------------------------------------------------------------------------------ Air Slasher

	/** One wind blade in flight: a 3D crescent with a bright leading edge, tip trails and shed fragments. */
	static final class Blade extends GrandLineFx.Effect implements GrandLineFx.Projectile {
		final Vec3 dir, side, up;
		final double speed, range;
		final int index;
		final boolean last;
		final List<Vec3> tipA = new ArrayList<>(), tipB = new ArrayList<>();
		Vec3 pos, prev;
		int fade = -1;

		Blade(int caster, Vec3 start, Vec3 dir, double speed, double range, int index) {
			super(caster);
			this.dir = dir.normalize();
			this.speed = speed;
			this.range = range;
			this.index = index;
			this.last = index == 5;
			// Small blades lean alternately, like the arm sweeps that threw them; the finisher flies level.
			double roll = last ? 0 : (index % 2 == 0 ? 0.35 : -0.35);
			Vec3 flatSide = this.dir.cross(new Vec3(0, 1, 0));
			flatSide = flatSide.lengthSqr() < 1e-6 ? new Vec3(1, 0, 0) : flatSide.normalize();
			Vec3 flatUp = flatSide.cross(this.dir).normalize();
			this.side = flatSide.scale(Math.cos(roll)).add(flatUp.scale(Math.sin(roll)));
			this.up = this.side.cross(this.dir).normalize();
			this.pos = start;
			this.prev = start;
		}

		double radius() {
			return last ? 1.9 : 0.85;
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
			fade = 0; // dissolve over a few ticks instead of vanishing
		}

		@Override
		boolean tick(ClientLevel level) {
			age++;
			if (fade >= 0) {
				return ++fade < 5;
			}
			prev = pos;
			pos = pos.add(dir.scale(speed));
			double r = radius();
			addTip(tipA, pos.add(side.scale(r * 0.92)).subtract(dir.scale(r * 0.35)));
			addTip(tipB, pos.subtract(side.scale(r * 0.92)).subtract(dir.scale(r * 0.35)));
			RandomSource rnd = level.random;
			if (rnd.nextFloat() < (last ? 0.9f : 0.4f)) {
				Vec3 at = pos.add(side.scale((rnd.nextDouble() - 0.5) * r * 1.6));
				streak(at, dir.scale(-0.15).add(FxSprites.randomDir(rnd).scale(0.04)), 6);
			}
			return age * speed < range + 3;
		}

		private void addTip(List<Vec3> list, Vec3 p) {
			list.add(0, p);
			while (list.size() > (last ? 12 : 7) * WindMesh.detail()) {
				list.remove(list.size() - 1);
			}
		}

		@Override
		void render(FxDraw d, ClientLevel level) {
			Vec3 at = fade >= 0 ? pos : prev.lerp(pos, d.partial);
			if (culled(d, at)) {
				return;
			}
			float alive = fade >= 0 ? Math.max(0, 1 - (fade + d.partial) / 5f) : 1;
			float dissolve = fade >= 0 ? 1 - alive : 0;
			float t = age + d.partial;
			double r = radius();
			double stretch = 1 + Math.min(0.4, speed * 0.12); // velocity stretch along the travel
			Vec3 centre = at.subtract(dir.scale(r * 0.75 * stretch));
			Vec3 fwd = dir.scale(stretch);
			// Translucent trailing body, then the bright cutting edge on top.
			WindMesh.crescent(d, false, centre, fwd, side, up, r, r * (last ? 0.55 : 0.45), last ? 150 : 125, WindRender.AIRFLOW,
					0.75f * alive, dissolve, t * 0.3);
			WindMesh.crescent(d, true, centre, fwd, side, up, r, r * 0.14, last ? 140 : 118, WindRender.CORE, 0.9f * alive, dissolve, t * 0.3);
			if (last) {
				// Layered: a second, slightly larger shell and a faint pressure haze ahead of the edge.
				WindMesh.crescent(d, false, centre.subtract(dir.scale(0.25)), fwd, side, up, r * 1.15, r * 0.35, 160, WindRender.EDGE,
						0.4f * alive, dissolve, -t * 0.2);
				WindMesh.crescent(d, false, centre.add(dir.scale(0.2)), fwd, side, up, r * 1.05, r * 0.2, 150, WindRender.HAZE,
						0.18f * alive, dissolve, t * 0.5);
			}
			trail(d, true, tipA, last ? 0.22 : 0.12, WindRender.EDGE, 0.6f * alive);
			trail(d, true, tipB, last ? 0.22 : 0.12, WindRender.EDGE, 0.6f * alive);
		}
	}

	// ------------------------------------------------------------------------------------------------ Helicopter Flight

	/** The rotor of arms: swept wind cones following each arm, circular hand trails, lift ring, downwash, dust. */
	static final class Rotor extends GrandLineFx.Effect {
		final List<Vec3> rightTrail = new ArrayList<>(), leftTrail = new ArrayList<>();
		boolean ending;
		int endAge;

		Rotor(int caster) {
			super(caster);
		}

		@Override
		void cancel() {
			ending = true;
		}

		@Override
		boolean tick(ClientLevel level) {
			age++;
			if (ending && ++endAge > 12) {
				return false;
			}
			Entity c = caster(level);
			if (c == null) {
				return false;
			}
			RandomSource r = level.random;
			float k = BodyMotion.intensity(caster, c.tickCount);
			// Downwash: wind streaks pushed down from the rotor.
			for (int i = FxSprites.count((int) (3 * k) + 1); i > 0; i--) {
				Vec3 from = BodyMotion.hand(c, 0, r.nextBoolean()).add((r.nextDouble() - 0.5) * 0.6, 0, (r.nextDouble() - 0.5) * 0.6);
				streak(from, new Vec3(0, -0.35 - r.nextDouble() * 0.2, 0).add(c.getDeltaMovement().scale(0.5)), 7);
			}
			// Ground dust when hovering low: a ring of dust pushed outward under the player.
			Vec3 feet = c.position();
			for (int dy = 0; dy < 5; dy++) {
				BlockPos pos = BlockPos.containing(feet.x, feet.y - dy - 0.1, feet.z);
				if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) {
					double ground = pos.getY() + 1.02;
					float strength = (1 - dy / 5f) * k;
					int colour = groundColour(level, new Vec3(feet.x, ground, feet.z));
					for (int i = FxSprites.count((int) (4 * strength)); i > 0; i--) {
						double a = r.nextDouble() * Math.PI * 2;
						Vec3 out = new Vec3(Math.cos(a), 0, Math.sin(a));
						FxSprites.sprite(FxDraw.CLOUD, false, new Vec3(feet.x, ground, feet.z).add(out.scale(0.6 + r.nextDouble() * 0.5)),
								out.scale(0.18 + r.nextDouble() * 0.12).add(0, 0.01, 0), 0.25, 0.025, 0, 0.9, 16 + r.nextInt(8), colour,
								WindRender.HAZE, 0.4f * strength);
					}
					break;
				}
			}
			return true;
		}

		@Override
		void render(FxDraw d, ClientLevel level) {
			Entity c = caster(level);
			if (c == null || culled(d, c.position())) {
				return;
			}
			float age = c.tickCount + d.partial;
			float k = BodyMotion.intensity(caster, age);
			if (k < 0.02f) {
				return;
			}
			double phase = BodyMotion.phase(caster);
			for (boolean left : new boolean[]{false, true}) {
				Vec3 hand = BodyMotion.hand(c, d.partial, left);
				List<Vec3> tr = left ? leftTrail : rightTrail;
				tr.add(0, hand);
				while (tr.size() > 5 + (int) (5 * WindMesh.detail())) {
					tr.remove(tr.size() - 1);
				}
				trail(d, true, tr, 0.14, WindRender.CORE, 0.6f * k);
				// The wind cone each arm sweeps: an arc of surface trailing behind the arm's current angle.
				float yaw = Mth.rotLerp(d.partial, c.yRotO, c.getYRot());
				double yr = Math.toRadians(yaw);
				Vec3 fwd = new Vec3(-Math.sin(yr), 0, Math.cos(yr));
				Vec3 right = new Vec3(-Math.cos(yr), 0, -Math.sin(yr));
				Vec3 out = left ? right.scale(-1) : right;
				Vec3 shoulder = c.getPosition(d.partial).add(0, 1.375, 0).add(out.scale(0.32));
				double cone = Math.toRadians(32), len = 0.68;
				Vec3 axisCentre = shoulder.add(out.scale(0.25 * len * 0.97)).add(0, Math.cos(cone) * len, 0);
				double ringR = Math.sin(cone) * len + 0.35; // the cone rim, widened by the wind
				double armAngle = phase + (left ? Math.PI : 0);
				// Ring in the plane of out and fwd (the arm's cone base); the arc trails the arm.
				WindMesh.ring(d, false, axisCentre, out, fwd, ringR * 0.35, ringR, armAngle - Math.toRadians(150), 150,
						WindRender.AIRFLOW, 0.6f * k, 0, age * 0.2);
				WindMesh.ring(d, true, axisCentre, out, fwd, ringR * 0.85, ringR * 1.05, armAngle - Math.toRadians(120), 120,
						WindRender.CORE, 0.7f * k, 0, age * 0.2);
			}
			// The lift ring: a broad, faint rotating disc of air above the head.
			Vec3 top = c.getPosition(d.partial).add(0, 2.15, 0);
			WindMesh.ring(d, false, top, new Vec3(1, 0, 0), new Vec3(0, 0, 1), 0.4, 1.5, phase * 0.5, 300, WindRender.HAZE, 0.3f * k, 0, age * 0.1);
		}
	}
}
