package com.grandline.client.fx;

import com.grandline.audio.GrandLineSounds;
import com.grandline.client.audio.GrandLineAudio;
import com.grandline.client.settings.CameraShake;
import com.grandline.fruit.blade.BladeMoves;
import com.grandline.network.payload.FxEventPayload;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Blade Fruit visuals from the server's beats (see BladeMoves): steel growing from the limbs ({@link BodyAttachments}),
 * strike poses ({@link BodyMotion}), curved slash surfaces drawn with the metal material ({@link WindRender.Material#METAL}:
 * a sharp bright edge, brushed steel and a sliding specular band), solid lit steel projectiles ({@link MetalMesh}),
 * afterimages ({@link Afterimages}) and physical sparks ({@link PhysicsSparks}). Restrained by design: sparks and short
 * flashes on contact, no glowing blades, no explosions.
 */
final class BladeFx {
	static final int EDGE = 0xF3F8FF;
	static final int STEEL = 0xC5D2DE;
	static final int SHADE = 0x8597A8;
	static final int DARK = 0x465563;
	static final int TRAIL = 0xD5E9F7;
	static final int SPARK = 0xFFC56D;
	static final int SPARK_COOL = 0x8A4A20;
	static final double CULL = 80;

	private BladeFx() {
	}

	static boolean handles(String effect) {
		return effect.startsWith("blade_");
	}

	// ------------------------------------------------------------------------------------------------ beats

	static void handle(FxEventPayload p, ClientLevel level, Entity caster, boolean own) {
		Minecraft mc = Minecraft.getInstance();
		int id = p.caster();
		float age = caster == null ? 0 : caster.tickCount;
		RandomSource r = level.random;
		switch (p.effect()) {
			case "blade_deflect" -> {
				// Sparks fly back toward the attacker from the real contact point; a small steel ring; a brief sheen.
				Vec3 back = p.dir().scale(-1);
				GrandLineFx.add(new PhysicsSparks(level, p.pos(), back, 9, 0.7, 0.28, 10, 0.05, SPARK, SPARK_COOL, true));
				GrandLineFx.add(new Ring(p.pos(), back, 0.9, 6, false));
				GrandLineFx.add(new Flash(p.pos(), 0.28, 2));
				BodyAttachments.show(id, BodyAttachments.Kind.STEEL_SHEEN, age, 2);
				GrandLineAudio.play(GrandLineSounds.BLADE_DEFLECT, p.pos(), 0.9f, 0.9f + r.nextFloat() * 0.25f);
			}
			case "blade_strike" -> {
				int step = (int) p.size();
				if (caster != null) {
					if (BodyAttachments.progress(id, BodyAttachments.Kind.FOREARM_BLADES, age) <= 0) {
						GrandLineAudio.play(GrandLineSounds.BLADE_STRIKE_CHARGE, p.pos(), 0.7f, 1.1f);
					}
					BodyAttachments.show(id, BodyAttachments.Kind.FOREARM_BLADES, age, BladeMoves.STRIKE_RESET);
					BodyMotion.strike(id, age, step);
					GrandLineFx.add(strikeSlash(caster, step));
				}
				GrandLineAudio.play(step == 2 ? GrandLineSounds.BLADE_STRIKE_CAST_FINAL : GrandLineSounds.BLADE_STRIKE_CAST, p.pos(), 0.9f,
						step == 1 ? 1.08f : 1f);
			}
			case "blade_strike_hit" -> {
				boolean last = p.size() >= 2;
				GrandLineFx.add(new PhysicsSparks(level, p.pos(), p.dir().scale(-0.4).add(0, 0.5, 0), last ? 10 : 6, 0.8, 0.22, 9, 0.045, SPARK, SPARK_COOL, true));
				GrandLineFx.add(new Flash(p.pos(), last ? 0.5 : 0.35, 2));
				GrandLineAudio.play(last ? GrandLineSounds.BLADE_STRIKE_IMPACT_FINAL : GrandLineSounds.BLADE_STRIKE_IMPACT, p.pos(), 0.9f,
						0.95f + r.nextFloat() * 0.1f);
				if (last) {
					shake(mc, p.pos(), own ? 0.6f : 0.3f);
				}
			}
			case "blade_rush_windup" -> {
				if (caster != null) {
					BodyAttachments.show(id, BodyAttachments.Kind.FOREARM_BLADES, age, 20);
					BodyMotion.start(id, BodyMotion.Kind.RUSH, age);
				}
				GrandLineAudio.play(GrandLineSounds.BLADE_STRIKE_CHARGE, p.pos(), 0.8f, 1f);
			}
			case "blade_rush" -> {
				if (caster != null) {
					GrandLineFx.add(new Rush(id, p.dir()));
				}
				GrandLineAudio.play(GrandLineSounds.BLADE_RUSH_CAST, p.pos(), 1f, 1f);
			}
			case "blade_rush_hit" -> {
				GrandLineFx.add(new PhysicsSparks(level, p.pos(), p.dir().scale(0.6).add(0, 0.4, 0), 8, 0.8, 0.3, 9, 0.05, SPARK, SPARK_COOL, true));
				GrandLineFx.add(new Flash(p.pos(), 0.45, 2));
				GrandLineAudio.play(GrandLineSounds.BLADE_STRIKE_IMPACT, p.pos(), 0.9f, 1f);
			}
			case "blade_rush_end" -> {
				Rush rush = GrandLineFx.find(Rush.class, id);
				if (rush != null) {
					rush.end();
				}
				if (p.size() > 0 && caster != null) {
					BodyMotion.finish(id, BodyMotion.Kind.RUSH, age);
					BodyAttachments.show(id, BodyAttachments.Kind.FOREARM_BLADES, age, 8);
					// The crossing finisher: two steel crescents in an X just ahead.
					Vec3 f = flat(p.dir());
					Vec3 side = new Vec3(-f.z, 0, f.x);
					Vec3 c = p.pos().add(0, 1.1, 0).add(f.scale(0.6));
					GrandLineFx.add(new Slash(c, f, tilt(side, f, 40), 2.4, 0.5, 150, 9, true));
					GrandLineFx.add(new Slash(c, f, tilt(side, f, -40), 2.4, 0.5, 150, 9, true));
					GrandLineFx.add(new Ring(c.add(f.scale(1.2)), f, 2.2, 7, true));
					GrandLineAudio.play(GrandLineSounds.BLADE_RUSH_IMPACT, p.pos(), 1f, 1f);
					shake(mc, p.pos(), own ? 1.1f : 0.6f);
				} else {
					BodyMotion.stop(id, BodyMotion.Kind.RUSH, age);
				}
			}
			case "blade_dance" -> {
				if (caster != null) {
					BodyAttachments.show(id, BodyAttachments.Kind.FOREARM_BLADES, age, (float) p.range() + 6);
					BodyMotion.start(id, BodyMotion.Kind.DANCE, age);
					GrandLineFx.add(new Dance(id, (int) p.range()));
				}
				GrandLineAudio.play(GrandLineSounds.BLADE_DANCE_CAST, p.pos(), 0.9f, 1f);
			}
			case "blade_dance_hit" -> {
				GrandLineFx.add(new PhysicsSparks(level, p.pos(), new Vec3(0, 0.5, 0), p.size() >= 3 ? 8 : 4, 0.9, 0.22, 8, 0.045, SPARK, SPARK_COOL, true));
				// One hit sound per tick per caster at most, so a crowd doesn't stack into a roar.
				long key = (long) id * 100_000 + (long) (age * 10);
				if (Dance.lastHitSound != key) {
					Dance.lastHitSound = key;
					GrandLineAudio.play(GrandLineSounds.BLADE_DANCE_IMPACT, p.pos(), 0.7f, 0.95f + r.nextFloat() * 0.15f);
				}
			}
			case "blade_dance_final" -> {
				BodyMotion.finish(id, BodyMotion.Kind.DANCE, age);
				Vec3 c = p.pos().add(0, 1.0, 0);
				GrandLineFx.add(new Sweep(c, p.size()));
				GrandLineFx.add(new Ring(p.pos().add(0, 0.1, 0), new Vec3(0, 1, 0), p.size() + 1.5, 10, true));
				GrandLineAudio.play(GrandLineSounds.BLADE_DANCE_CAST_FINAL, p.pos(), 1f, 1f);
				shake(mc, p.pos(), own ? 0.9f : 0.5f);
			}
			case "blade_dance_end" -> BodyMotion.stop(id, BodyMotion.Kind.DANCE, age);
			case "blade_barrage_windup" -> {
				if (caster != null) {
					BodyAttachments.show(id, BodyAttachments.Kind.FOREARM_BLADES, age, 34);
					BodyMotion.slash(id, age + 3, true, false);
				}
			}
			case "blade_barrage_finale" -> {
				if (caster != null) {
					BodyMotion.slash(id, age + 5, false, true);
				}
			}
			case "blade_proj" -> {
				int n = (int) p.size();
				boolean last = n == 6;
				if (caster != null) {
					BodyMotion.slash(id, age, !last && n % 2 == 1, last);
				}
				Thrown t = new Thrown(id, p.pos(), p.dir(), p.speed(), p.range(), last);
				GrandLineFx.add(t);
				GrandLineAudio.play(GrandLineSounds.BLADE_BARRAGE_CAST, p.pos(), last ? 1.1f : 0.8f, last ? 0.85f : 1f + n * 0.03f);
				if (last) {
					GrandLineAudio.play(GrandLineSounds.BLADE_STRIKE_CAST_FINAL, p.pos(), 0.9f, 0.9f);
				}
				GrandLineAudio.loop(GrandLineSounds.BLADE_BARRAGE_TRAVEL, t::position, () -> !t.done && t.fade < 0, last ? 0.8f : 0.35f, () -> last ? 0.9 : 1.15, null);
			}
			case "blade_proj_hit" -> {
				boolean last = p.size() >= 6;
				int kind = (int) p.speed();
				GrandLineFx.endProjectile(id, p.pos());
				if (kind > 0) {
					Vec3 back = p.dir().scale(-1).add(0, 0.35, 0);
					GrandLineFx.add(new PhysicsSparks(level, p.pos(), back, last ? 18 : 7, 0.85, last ? 0.35 : 0.25, 12, 0.05, SPARK, SPARK_COOL, true));
					GrandLineFx.add(new PhysicsSparks(level, p.pos(), back, last ? 6 : 2, 0.9, 0.18, 22, 0.06, STEEL, DARK, false)); // fragments
					GrandLineFx.add(new Flash(p.pos(), last ? 0.8 : 0.35, last ? 3 : 2));
					GrandLineFx.add(new Ring(p.pos(), p.dir(), last ? 2.8 : 1.0, last ? 9 : 6, last));
					if (last) {
						shake(mc, p.pos(), own ? 1.2f : 0.7f);
					}
				}
				GrandLineAudio.play(last ? GrandLineSounds.BLADE_BARRAGE_IMPACT_FINAL : GrandLineSounds.BLADE_BARRAGE_IMPACT, p.pos(),
						kind == 0 ? 0.4f : 1f, 0.95f + r.nextFloat() * 0.1f);
			}
			case "blade_flight_start" -> {
				if (caster != null) {
					BodyMotion.start(id, BodyMotion.Kind.ARM_ROTOR, age);
					BodyAttachments.show(id, BodyAttachments.Kind.ROTOR, age, 20 * 60);
					Flight flight = new Flight(id);
					GrandLineFx.add(flight);
					GrandLineAudio.loop(GrandLineSounds.BLADE_FLIGHT_LOOP, () -> {
						Entity c = flight.caster(level);
						return c == null ? null : c.position();
					}, () -> !flight.done && !flight.ending, 0.85f, () -> {
						Entity c = flight.caster(level);
						return c == null ? 1.0 : 0.92 + Math.min(0.2, c.getDeltaMovement().length() * 0.5);
					}, GrandLineSounds.BLADE_FLIGHT_AFTERMATH);
				}
				GrandLineAudio.play(GrandLineSounds.BLADE_FLIGHT_CAST, p.pos(), 1f, 1f);
			}
			default -> {
			}
		}
	}

	/** Blade Flight ends (the shared flight_end beat): spin down, then retract the rotors. */
	static void flightEnd(int id, Entity caster) {
		Flight flight = GrandLineFx.find(Flight.class, id);
		if (flight != null) {
			float age = caster == null ? 0 : caster.tickCount;
			BodyMotion.stop(id, BodyMotion.Kind.ARM_ROTOR, age);
			BodyAttachments.hide(id, BodyAttachments.Kind.ROTOR, age + 8);
			flight.ending = true;
		}
	}

	private static void shake(Minecraft mc, Vec3 at, float degrees) {
		if (mc.player != null) {
			CameraShake.impact(mc.player.position().distanceTo(at), degrees);
		}
	}

	static Vec3 flat(Vec3 v) {
		Vec3 f = new Vec3(v.x, 0, v.z);
		return f.lengthSqr() < 1e-6 ? new Vec3(0, 0, 1) : f.normalize();
	}

	/** {@code side} turned by {@code degrees} about {@code axis} (to tilt a slash plane). */
	static Vec3 tilt(Vec3 side, Vec3 axis, double degrees) {
		double a = Math.toRadians(degrees);
		Vec3 up = axis.cross(side).normalize();
		return side.scale(Math.cos(a)).add(up.scale(Math.sin(a))).normalize();
	}

	/** The slash surface of a combo step, in front of the caster at chest height. */
	static Slash strikeSlash(Entity caster, int step) {
		Vec3 f = flat(caster.getLookAngle());
		Vec3 side = new Vec3(-f.z, 0, f.x);
		Vec3 c = caster.position().add(0, 1.15, 0).add(f.scale(0.3));
		return switch (step) {
			case 0 -> new Slash(c, f, side, 2.6, 0.45, 140, 7, false);
			case 1 -> new Slash(c, f, tilt(side, f, -42), 2.6, 0.45, 130, 7, false);
			default -> new Slash(c, f, tilt(side, f, 30), 3.0, 0.55, 150, 9, true);
		};
	}

	static boolean culled(FxDraw d, Vec3 at) {
		return at.distanceToSqr(d.cameraPosition()) > CULL * CULL;
	}

	// ------------------------------------------------------------------------------------------------ effects

	/** A curved steel slash: appears in two ticks, holds, then dissolves from its edges (metal shader). */
	static final class Slash extends GrandLineFx.Effect {
		final Vec3 centre, forward, side, up;
		final double radius, width, arc;
		final int life;
		final boolean heavy;

		Slash(Vec3 centre, Vec3 forward, Vec3 side, double radius, double width, double arc, int life, boolean heavy) {
			super(-1);
			this.centre = centre;
			this.forward = forward;
			this.side = side;
			this.up = forward.cross(side).normalize();
			this.radius = radius;
			this.width = width;
			this.arc = arc;
			this.life = life;
			this.heavy = heavy;
		}

		@Override
		boolean tick(ClientLevel level) {
			if (age == 1 && heavy) {
				Vec3 tip = centre.add(forward.scale(radius)).add(side.scale(radius * 0.6));
				GrandLineFx.add(new PhysicsSparks(level, tip, side.add(0, 0.3, 0), 4, 0.6, 0.2, 7, 0.04, SPARK, SPARK_COOL, true));
			}
			return ++age < life;
		}

		@Override
		void render(FxDraw d, ClientLevel level) {
			if (culled(d, centre)) {
				return;
			}
			float t = (age + d.partial) / life;
			float in = (float) WindMesh.ease(t * life / 2.0);
			float dissolve = (float) Math.max(0, (t - 0.35) / 0.65);
			Vec3 c = centre.subtract(forward.scale(radius * 0.55));
			WindRender.with(WindRender.Material.METAL, () -> {
				WindMesh.crescent(d, false, c, forward, side, up, radius, width, arc, STEEL, 0.75f * in, dissolve, t * 0.6);
				WindMesh.crescent(d, true, c, forward, side, up, radius, width * 0.18, arc * 0.95, EDGE, 0.55f * in, dissolve, t * 0.6);
			});
		}
	}

	/** The final circular sweep of Blade Dance: a full ring of steel cut round the caster, dissolving outward. */
	static final class Sweep extends GrandLineFx.Effect {
		final Vec3 centre;
		final double radius;

		Sweep(Vec3 centre, double radius) {
			super(-1);
			this.centre = centre;
			this.radius = radius;
		}

		@Override
		boolean tick(ClientLevel level) {
			return ++age < 10;
		}

		@Override
		void render(FxDraw d, ClientLevel level) {
			float t = (age + d.partial) / 10f;
			double r = radius * (0.55 + 0.45 * WindMesh.ease(t * 2));
			WindRender.with(WindRender.Material.METAL, () -> {
				WindMesh.ring(d, false, centre, new Vec3(1, 0, 0), new Vec3(0, 0, 1), r - 0.7, r, t * 4, 360, STEEL, 0.6f * (1 - t), t, t);
				WindMesh.ring(d, true, centre, new Vec3(1, 0, 0), new Vec3(0, 0, 1), r - 0.12, r, t * 4, 360, EDGE, 0.5f * (1 - t), t, t);
			});
		}
	}

	/** A metallic pressure ring (deflections, impacts), in the plane facing {@code normal}. */
	static final class Ring extends GrandLineFx.Effect {
		final Vec3 at, normal;
		final double radius;
		final int life;
		final boolean wall;

		Ring(Vec3 at, Vec3 normal, double radius, int life, boolean wall) {
			super(-1);
			this.at = at;
			this.normal = normal.lengthSqr() < 1e-6 ? new Vec3(0, 1, 0) : normal.normalize();
			this.radius = radius;
			this.life = life;
			this.wall = wall;
		}

		@Override
		boolean tick(ClientLevel level) {
			return ++age < life;
		}

		@Override
		void render(FxDraw d, ClientLevel level) {
			if (culled(d, at)) {
				return;
			}
			float t = (age + d.partial) / life;
			double r = radius * WindMesh.ease(Math.sqrt(t));
			Vec3[] b = WindMesh.basis(normal);
			WindRender.with(WindRender.Material.METAL, () -> {
				WindMesh.ring(d, true, at, b[0], b[1], Math.max(0, r - 0.12 - radius * 0.08), r, 0, 360, TRAIL, 0.55f * (1 - t), t * 0.8f, t);
				if (wall) {
					WindMesh.wall(d, false, at.subtract(0, 0.4, 0), r * 0.9, 0.8 * (1 - t), STEEL, 0.25f * (1 - t), t, t);
				}
			});
		}
	}

	/** A small, short contact flash (never blinding). */
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
			return ++age < life;
		}

		@Override
		void render(FxDraw d, ClientLevel level) {
			float t = (age + d.partial) / life;
			d.billboard(FxDraw.SPARK, true, at, size * (1 + t), age * 0.7, FxDraw.rgba(EDGE, 0.8f * (1 - t)));
			d.billboard(FxDraw.GLOW, true, at, size * 0.8, 0, FxDraw.rgba(SPARK, 0.35f * (1 - t)));
		}
	}

	/** Razor Rush in flight: afterimages, blade-hand ribbons and speed lines until the end beat. */
	static final class Rush extends GrandLineFx.Effect {
		final Vec3 dir;
		final Afterimages ghosts;
		final List<Vec3> right = new ArrayList<>(), left = new ArrayList<>();
		boolean ending;
		int endAge;

		Rush(int caster, Vec3 dir) {
			super(caster);
			this.dir = dir.normalize();
			this.ghosts = new Afterimages(caster, 7, STEEL);
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
			if (ending && ++endAge > 6 || age > 30) {
				ghosts.stop();
				return false;
			}
			Entity c = caster(level);
			if (c == null) {
				ghosts.stop();
				return false;
			}
			if (!ending) {
				RandomSource r = level.random;
				for (int i = FxSprites.count(4); i > 0; i--) {
					Vec3 at = c.position().add((r.nextDouble() - 0.5) * 1.6, 0.2 + r.nextDouble() * 1.6, (r.nextDouble() - 0.5) * 1.6);
					FxSprites.sprite(FxDraw.SPARK, true, at, dir.scale(-0.8), 0.04, 0, 0, 0.8, 4, TRAIL, SHADE, 0.6f);
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
			float alpha = ending ? Math.max(0, 1 - (endAge + d.partial) / 6f) : 1;
			track(right, BodyMotion.hand(c, d.partial, false));
			track(left, BodyMotion.hand(c, d.partial, true));
			trail(d, right, 0.18, alpha);
			trail(d, left, 0.18, alpha);
		}

		private void track(List<Vec3> list, Vec3 p) {
			if (ending) {
				return;
			}
			list.add(0, p);
			while (list.size() > 6 + (int) (6 * WindMesh.detail())) {
				list.remove(list.size() - 1);
			}
		}
	}

	/** A steel ribbon trail (newest point first), tapering and fading. */
	static void trail(FxDraw d, List<Vec3> pts, double width, float alpha) {
		int n = pts.size();
		if (n < 2) {
			return;
		}
		double[] w = new double[n];
		float[] a = new float[n];
		for (int i = 0; i < n; i++) {
			double t = i / (double) (n - 1);
			w[i] = width * (1 - t * 0.85);
			a[i] = (float) (alpha * 0.7 * (1 - t));
		}
		d.ribbon(FxDraw.STREAK, pts, w, FxDraw.rgba(TRAIL, 1f), a, 0);
	}

	/** Blade Dance in progress: cuts all round the turning caster, with rotating blade-hand ribbons. */
	static final class Dance extends GrandLineFx.Effect {
		static long lastHitSound;
		final int duration;
		final List<Vec3> right = new ArrayList<>(), left = new ArrayList<>();

		Dance(int caster, int duration) {
			super(caster);
			this.duration = duration;
		}

		@Override
		boolean tick(ClientLevel level) {
			age++;
			Entity c = caster(level);
			if (c == null || age > duration) {
				return false;
			}
			// A new cut every 3 ticks at the angle the body has turned to: horizontal and diagonal in turn.
			if (age % 3 == 0 && age < BladeMoves.DANCE_FINISH) {
				RandomSource r = level.random;
				double yaw = Math.toRadians(c.getYRot()) + age * 0.42 + r.nextDouble() * 0.6;
				Vec3 f = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
				Vec3 side = new Vec3(-f.z, 0, f.x);
				double tiltDeg = (age / 3) % 2 == 0 ? (r.nextDouble() - 0.5) * 20 : (r.nextBoolean() ? 40 : -40);
				GrandLineFx.add(new Slash(c.position().add(0, 0.8 + r.nextDouble() * 0.7, 0), f, tilt(side, f, tiltDeg),
						2.2 + r.nextDouble() * 1.6, 0.4, 120, 6, false));
			}
			return true;
		}

		@Override
		void render(FxDraw d, ClientLevel level) {
			Entity c = caster(level);
			if (c == null || culled(d, c.position())) {
				return;
			}
			add(right, BodyMotion.hand(c, d.partial, false));
			add(left, BodyMotion.hand(c, d.partial, true));
			trail(d, right, 0.2, 1);
			trail(d, left, 0.2, 1);
		}

		private void add(List<Vec3> list, Vec3 p) {
			list.add(0, p);
			while (list.size() > 8 + (int) (8 * WindMesh.detail())) {
				list.remove(list.size() - 1);
			}
		}
	}

	/** One thrown steel blade: solid lit geometry pointing along its flight, spinning edge-first, with a steel trail. */
	static final class Thrown extends GrandLineFx.Effect implements GrandLineFx.Projectile {
		final Vec3 dir;
		final double speed, range;
		final boolean big;
		final List<Vec3> trail = new ArrayList<>();
		Vec3 pos, prev;
		int fade = -1;

		Thrown(int caster, Vec3 start, Vec3 dir, double speed, double range, boolean big) {
			super(caster);
			this.dir = dir.normalize();
			this.speed = speed;
			this.range = range;
			this.big = big;
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
			prev = at;
			fade = 0;
		}

		@Override
		boolean tick(ClientLevel level) {
			age++;
			if (fade >= 0) {
				if (!trail.isEmpty()) {
					trail.remove(trail.size() - 1);
				}
				return ++fade < 5;
			}
			prev = pos;
			pos = pos.add(dir.scale(speed));
			trail.add(0, pos);
			while (trail.size() > (big ? 9 : 6) * WindMesh.detail()) {
				trail.remove(trail.size() - 1);
			}
			return age * speed < range + 3;
		}

		@Override
		void render(FxDraw d, ClientLevel level) {
			Vec3 at = prev.lerp(pos, d.partial);
			if (culled(d, at)) {
				return;
			}
			float alive = fade >= 0 ? Math.max(0, 1 - (fade + d.partial) / 5f) : 1;
			List<Vec3> pts = new ArrayList<>(trail);
			pts.add(0, at);
			trail(d, pts, big ? 0.32 : 0.16, alive);
			if (fade >= 0) {
				return; // the blade itself shattered on impact; only its trail fades
			}
			// Solid steel: oriented so its length runs along the flight, rolling slowly about it.
			PoseStack pose = d.pose;
			pose.pushPose();
			Vec3 tail = at.subtract(dir.scale(big ? 1.3 : 0.75));
			pose.translate(tail.x - d.camPos.x, tail.y - d.camPos.y, tail.z - d.camPos.z);
			pose.mulPose(new Quaternionf().rotationTo(new Vector3f(0, 1, 0), new Vector3f((float) dir.x, (float) dir.y, (float) dir.z)));
			pose.mulPose(Axis.YP.rotation((age + d.partial) * 0.5f));
			int light = LevelRenderer.getLightColor(level, BlockPos.containing(at));
			MetalMesh.blade(pose, d.buffers, light, big ? 1.4f : 0.8f, big ? 0.34f : 0.18f, big ? 0.07f : 0.045f, 1f);
			pose.popPose();
		}
	}

	/** Blade Flight: rotor-tip ribbons, downwash and ground dust (the rotors themselves are BodyAttachments). */
	static final class Flight extends GrandLineFx.Effect {
		final List<Vec3> tipR = new ArrayList<>(), tipL = new ArrayList<>();
		boolean ending;
		int endAge;

		Flight(int caster) {
			super(caster);
		}

		@Override
		void cancel() {
			ending = true;
		}

		@Override
		boolean tick(ClientLevel level) {
			age++;
			if (ending && ++endAge > 14) {
				return false;
			}
			Entity c = caster(level);
			if (c == null) {
				return false;
			}
			RandomSource r = level.random;
			float k = BodyMotion.intensity(caster, c.tickCount);
			for (int i = FxSprites.count((int) (3 * k) + 1); i > 0; i--) {
				Vec3 from = BodyMotion.hand(c, 0, r.nextBoolean()).add((r.nextDouble() - 0.5) * 1.6, 0, (r.nextDouble() - 0.5) * 1.6);
				FxSprites.sprite(FxDraw.SPARK, true, from, new Vec3(0, -0.4 - r.nextDouble() * 0.2, 0).add(c.getDeltaMovement().scale(0.5)),
						0.04, 0, 0, 0.95, 7, TRAIL, SHADE, 0.45f);
			}
			Vec3 feet = c.position();
			for (int dy = 0; dy < 5; dy++) {
				BlockPos pos = BlockPos.containing(feet.x, feet.y - dy - 0.1, feet.z);
				if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) {
					float strength = (1 - dy / 5f) * k;
					int colour = level.getBlockState(pos).getMapColor(level, pos).col;
					for (int i = FxSprites.count((int) (4 * strength)); i > 0; i--) {
						double a = r.nextDouble() * Math.PI * 2;
						Vec3 out = new Vec3(Math.cos(a), 0, Math.sin(a));
						FxSprites.sprite(FxDraw.CLOUD, false, new Vec3(feet.x, pos.getY() + 1.02, feet.z).add(out.scale(0.5 + r.nextDouble() * 0.5)),
								out.scale(0.2 + r.nextDouble() * 0.12).add(0, 0.01, 0), 0.25, 0.025, 0, 0.9, 16 + r.nextInt(8), colour, 0xB8C1C9, 0.4f * strength);
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
			// Circular ribbons traced by the rotor tips: each hub at a hand, blades about 1 block long.
			double a = BodyAttachments.rotorAngle(caster, age);
			for (boolean leftHand : new boolean[]{false, true}) {
				Vec3 hub = BodyMotion.hand(c, d.partial, leftHand).add(0, 0.05, 0);
				double ang = a + (leftHand ? Math.PI / 2 : 0);
				Vec3 tip = hub.add(Math.cos(ang) * 1.0, 0.1, Math.sin(ang) * 1.0);
				List<Vec3> list = leftHand ? tipL : tipR;
				list.add(0, tip);
				while (list.size() > 5 + (int) (5 * WindMesh.detail())) {
					list.remove(list.size() - 1);
				}
				trail(d, list, 0.12, 0.8f * k);
				// A faint swept disc behind the blades: the motion blur of the spinning steel.
				WindRender.with(WindRender.Material.METAL, () -> WindMesh.ring(d, false, hub.add(0, 0.12, 0), new Vec3(1, 0, 0), new Vec3(0, 0, 1),
						0.25, 1.05, ang - Math.toRadians(140), 140, SHADE, 0.28f * k, 0, age * 0.1));
			}
		}
	}
}
