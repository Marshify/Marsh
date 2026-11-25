package com.grandline.client.fx;

import com.grandline.audio.GrandLineSounds;
import com.grandline.client.audio.GrandLineAudio;
import com.grandline.client.settings.CameraShake;
import com.grandline.network.payload.FxEventPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Smoke Fruit visuals and the reusable volumetric-style smoke renderer.
 *
 * <p>Smoke is drawn as layered, procedurally deformed shells in the SMOKE material (GLSL fbm density, cool-gray
 * self-shading, fresnel-soft edges, noise dissolve; plain translucent fallback under shader packs): a dense core, a
 * medium cloud and broken outer wisps per {@link #volume}, plus spline ribbons, flipbook puffs and a reusable
 * deformed-funnel {@link #vortex}. Limbs turn to smoke by hiding the real model part ({@link #hidden}, rendering only;
 * skin and armour come back untouched) and drawing connected smoke geometry from the shoulder; Elemental Reflex
 * disperses the struck region along the attack and draws it back. Detail follows the VFX preset; far effects are culled.
 */
public final class SmokeFx {
	public static final int HIGHLIGHT = 0xE2E5EB, LIGHT = 0xC4C9D2, MAIN = 0xA0A7B2, DENSE = 0x737D8B, SHADOW = 0x505965, IMPACT = 0xD2DCE6;
	/** Hidden model parts per player id: bit 1 right arm, 2 left arm, 4 head, 8 body, 16 legs → game time until. */
	private static final Map<Integer, long[]> HIDDEN = new HashMap<>();
	private static final Map<Integer, Projectile> SHOTS = new HashMap<>();
	private static final Map<Integer, Flight> FLIGHTS = new HashMap<>();
	private static long lastReflexSound, lastBurstSound;

	public static final int RIGHT_ARM = 1, LEFT_ARM = 2, HEAD = 4, BODY = 8, LEGS = 16;

	private SmokeFx() {
	}

	static boolean handles(String effect) {
		return effect.startsWith("smoke_");
	}

	static long now() {
		ClientLevel l = Minecraft.getInstance().level;
		return l == null ? 0 : l.getGameTime();
	}

	/** Model parts of {@code entity} currently turned to smoke (rendering only). */
	public static int hidden(int entity) {
		long[] h = HIDDEN.get(entity);
		if (h == null) {
			return 0;
		}
		long t = now();
		int mask = 0;
		for (int b = 0; b < 5; b++) {
			if (h[b] > t) {
				mask |= 1 << b;
			}
		}
		return mask;
	}

	static void hide(int entity, int mask, int ticks) {
		long[] h = HIDDEN.computeIfAbsent(entity, k -> new long[5]);
		long until = now() + ticks;
		for (int b = 0; b < 5; b++) {
			if ((mask & 1 << b) != 0) {
				h[b] = Math.max(h[b], until);
			}
		}
	}

	static void show(int entity, int mask) {
		long[] h = HIDDEN.get(entity);
		if (h != null) {
			for (int b = 0; b < 5; b++) {
				if ((mask & 1 << b) != 0) {
					h[b] = 0;
				}
			}
		}
	}

	static void clearAll() {
		HIDDEN.clear();
		SHOTS.clear();
		FLIGHTS.clear();
	}

	// ------------------------------------------------------------------------------------------------ renderer

	/**
	 * A volumetric-style smoke mass: {@code layers} shells (dense core, medium cloud, outer wisps), each a deformed
	 * sphere evolving with {@code time}. {@code alpha} overall opacity, {@code dissolve} 0..1 thinning out.
	 */
	public static void volume(FxDraw d, Vec3 c, double r, double seed, float time, float alpha, float dissolve, int layers, int tint) {
		double dist = c.distanceTo(d.cameraPosition());
		if (dist > 96 || alpha <= 0.01f || r <= 0.02) {
			return;
		}
		int lod = dist > 40 ? 5 : dist > 16 ? 7 : 9;
		int lat = Math.max(4, (int) Math.round(lod * WindMesh.detail())), lon = lat * 2;
		int n = Math.min(layers, 1 + (int) Math.round(WindMesh.detail() * 2)); // Low 1-2 layers, Ultra 3+
		// Soft billowing body: seeded flipbook smoke billboards churning inside the volume (density without hard edges).
		int puffs = 3 + (int) Math.round(3 * WindMesh.detail());
		for (int i = 0; i < puffs; i++) {
			double h1 = Math.sin(seed * 12.9898 + i * 78.233) * 43758.5453, h2 = Math.sin(seed * 39.3468 + i * 11.135) * 24634.6345;
			double u = h1 - Math.floor(h1), v = h2 - Math.floor(h2);
			double ang = u * Math.PI * 2 + time * 0.02 * (i % 2 == 0 ? 1 : -1), el = (v - 0.5) * 1.4;
			Vec3 p = c.add(Math.cos(ang) * Math.cos(el) * r * 0.45, Math.sin(el) * r * 0.4, Math.sin(ang) * Math.cos(el) * r * 0.45);
			int frame = (int) (u * 4) & 3;
			int col = FxSprites.lerpColor(i % 3 == 0 ? DENSE : MAIN, tint, 0.4f);
			d.billboard(ExplosionFx.SMOKE_SHEET, false, p, r * (0.75 + 0.35 * v), u * 6 + time * 0.01, FxDraw.rgba(col, alpha * 0.55f * (1 - dissolve)),
					frame / 4f, (frame + 1) / 4f, 0, 1);
		}
		WindRender.with(WindRender.Material.SMOKE, () -> {
			for (int k = 0; k < Math.max(1, n); k++) {
				double rk = r * (0.55 + 0.3 * k);
				float ak = alpha * (k == 0 ? 0.55f : k == 1 ? 0.35f : 0.22f);
				shell(d, c.add(0, r * 0.05 * k, 0), rk, seed + k * 3.7, time * (1 + 0.25f * k), lat, lon, tint, ak, Math.min(1, dissolve + 0.12f * k),
						0.15f + 0.3f * k, 0.18 + 0.08 * k);
			}
		});
	}

	/** One deformed shell: v carries the density layer for the shader. */
	static void shell(FxDraw d, Vec3 c, double r, double seed, float a, int lat, int lon, int rgb, float alpha, float dissolve, float layer, double rough) {
		Vec3[] q = new Vec3[4];
		float[] fr = new float[4], us = new float[4], vs = new float[4], al = {alpha, alpha, alpha, alpha};
		for (int i = 0; i < lat; i++) {
			for (int j = 0; j < lon; j++) {
				int[][] idx = {{i, j}, {i + 1, j}, {i + 1, j + 1}, {i, j + 1}};
				for (int k = 0; k < 4; k++) {
					double th = Math.PI * idx[k][0] / lat, ph = Math.PI * 2 * idx[k][1] / lon;
					Vec3 nrm = new Vec3(Math.sin(th) * Math.cos(ph), Math.cos(th), Math.sin(th) * Math.sin(ph));
					double disp = 1 + rough * (Math.sin(3 * th + a * 0.11 + seed) * Math.sin(2 * ph + a * 0.07 + seed * 0.5)
							+ 0.6 * Math.sin(5 * ph - a * 0.13 + seed * 1.3) * Math.sin(4 * th + seed));
					q[k] = c.add(nrm.scale(r * disp));
					fr[k] = WindRender.fresnel(d, q[k], nrm);
					al[k] = alpha * near(q[k].distanceTo(d.cameraPosition()));
					us[k] = idx[k][1] / (float) lon;
					vs[k] = layer + 0.1f * idx[k][0] / lat;
				}
				WindRender.quad(d, false, q, us, vs, rgb, al, dissolve, fr);
			}
		}
	}

	/** Smoke right in front of the camera thins out, so it never becomes an opaque wall over the view. */
	static float near(double dist) {
		return (float) Mth.clamp((dist - 1.0) / 7.0, 0.06, 1);
	}

	/**
	 * A reusable 3D vortex: a tapered, deformed funnel (not stacked rings) turning at {@code spin} radians/tick, from
	 * {@code rBottom} at the base to {@code rTop} at {@code height}; surface ripples and a density band per layer.
	 */
	public static void vortex(FxDraw d, Vec3 base, double height, double rBottom, double rTop, float a, double spin, double seed, int rgb, float alpha,
			float dissolve, float layer) {
		if (base.distanceTo(d.cameraPosition()) > 128 || alpha <= 0.01f) {
			return;
		}
		WindMesh.Patch patch = (s, t) -> {
			double h = t * height;
			double r = rBottom + (rTop - rBottom) * Math.pow(t, 0.85);
			double ang = s * Math.PI * 2 + a * spin + t * 1.6 + seed;
			double ripple = 1 + 0.12 * Math.sin(ang * 3 + a * 0.2 + seed) + 0.08 * Math.sin(t * 9 - a * 0.25);
			return base.add(Math.cos(ang) * r * ripple, h, Math.sin(ang) * r * ripple);
		};
		Vec3 cam = d.cameraPosition();
		WindRender.with(WindRender.Material.SMOKE, () -> WindMesh.surface(d, false, patch, WindMesh.segs(20), WindMesh.segs(8), rgb,
				(s, t) -> alpha * (float) Math.min(1, Math.min(t * 6, (1 - t) * 3)) * near(patch.at(s, t).distanceTo(cam)), dissolve, a * spin * 0.15));
	}

	/** Soft flipbook puffs of smoke (secondary dissipating smoke). */
	static void puffs(ClientLevel level, Vec3 at, double spread, int count, double size, int life, Vec3 drift) {
		RandomSource r = level.random;
		for (int i = FxSprites.count(count); i > 0; i--) {
			Vec3 p = at.add(FxSprites.randomDir(r).scale(spread * r.nextDouble()));
			FxSprites.sprite(FxDraw.CLOUD, false, p, drift.add(FxSprites.randomDir(r).scale(0.03)), size * (0.7 + 0.6 * r.nextDouble()), size / life * 1.4,
					-0.002, 0.9, life + r.nextInt(life / 2 + 1), MAIN, LIGHT, 0.32f);
		}
	}

	static Vec3 shoulder(Entity e, float partial, boolean left) {
		float yaw = (e instanceof LivingEntity l ? Mth.rotLerp(partial, l.yBodyRotO, l.yBodyRot) : e.getYRot()) * Mth.DEG_TO_RAD;
		Vec3 right = new Vec3(-Mth.cos(yaw), 0, -Mth.sin(yaw));
		return e.getPosition(partial).add(0, 1.38, 0).add(right.scale(left ? -0.36 : 0.36));
	}

	/** A connected smoke limb: blobs along a curve from the shoulder through {@code mid} to {@code tip}. */
	static void limb(FxDraw d, Vec3 from, Vec3 mid, Vec3 tip, double r0, double r1, float a, float alpha, double seed) {
		int n = 4 + (int) Math.round(4 * WindMesh.detail());
		List<Vec3> pts = new ArrayList<>();
		for (int i = 0; i <= n; i++) {
			double t = i / (double) n;
			Vec3 p = from.scale((1 - t) * (1 - t)).add(mid.scale(2 * t * (1 - t))).add(tip.scale(t * t));
			pts.add(p);
			double r = r0 + (r1 - r0) * t;
			WindRender.with(WindRender.Material.SMOKE, () -> shell(d, p, r, seed + t * 5, a, 4, 8, DENSE, alpha, 0, 0.1f, 0.25));
		}
		double[] w = new double[pts.size()];
		float[] al = new float[pts.size()];
		for (int i = 0; i < w.length; i++) {
			w[i] = (r0 + (r1 - r0) * i / (w.length - 1.0)) * 1.6;
			al[i] = alpha * 0.35f;
		}
		java.util.Collections.reverse(pts);
		d.ribbonSoft(FxDraw.STREAK, pts, w, FxDraw.rgba(LIGHT, 1), al, a * 0.05f);
	}

	static void shake(Vec3 at, float deg) {
		Vec3 cam = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
		CameraShake.impact(cam.distanceTo(at), deg);
	}

	// ------------------------------------------------------------------------------------------------ beats

	static void handle(FxEventPayload p, ClientLevel level, Entity caster, boolean own) {
		int id = p.caster();
		float age = caster == null ? 0 : caster.tickCount;
		RandomSource r = level.random;
		switch (p.effect()) {
			case "smoke_reflex" -> {
				// The struck region turns to smoke, is pushed through along the attack and drawn back.
				double h = p.size();
				int region = h > 1.45 ? HEAD : h < 0.75 ? LEGS : BODY | RIGHT_ARM | LEFT_ARM;
				hide(id, region, 9);
				GrandLineFx.add(new Disperse(id, p.pos(), p.dir(), region));
				if (now() - lastReflexSound >= 4 || now() < lastReflexSound) {
					lastReflexSound = now();
					GrandLineAudio.play(GrandLineSounds.SMOKE_REFLEX, p.pos(), 0.7f, 0.95f + r.nextFloat() * 0.15f);
				}
			}
			case "smoke_slam" -> {
				BodyMotion.smoke(id, age, 0);
				if (caster != null) {
					GrandLineFx.add(new SlamArm(id, p.pos().add(p.dir()), (int) p.speed()));
				}
				GrandLineAudio.play(GrandLineSounds.SMOKE_SLAM_CAST, p.pos(), 1f, 1f);
			}
			case "smoke_slam_impact" -> {
				GrandLineFx.add(new Burst(p.pos().add(0, 0.6, 0), p.size(), 26, true));
				GrandLineFx.add(new BladeFx.Flash(p.pos().add(0, 0.5, 0), 0.6, 2));
				SpringFx.ground(level, p.pos(), p.size(), 10);
				GrandLineFx.add(new ExplosionFx.Scorch(p.pos().add(0, 0.02, 0), p.size() * 0.5, 100));
				puffs(level, p.pos().add(0, 0.4, 0), p.size() * 0.6, 8, 0.9, 30, new Vec3(0, 0.02, 0));
				GrandLineAudio.play(GrandLineSounds.SMOKE_SLAM_IMPACT, p.pos(), 1f, 0.95f + r.nextFloat() * 0.1f);
				shake(p.pos(), 1.2f);
			}
			case "smoke_blast" -> {
				BodyMotion.smoke(id, age, 1);
				hide(id, RIGHT_ARM, 10);
				Projectile s = new Projectile(p.pos(), p.dir().scale(p.speed()), 0, 0.45, 40);
				SHOTS.put((int) p.range(), s);
				GrandLineFx.add(s);
				GrandLineAudio.play(GrandLineSounds.SMOKE_BLAST_CAST, p.pos(), 1f, 1f);
			}
			case "smoke_blast_end" -> {
				Projectile s = SHOTS.remove((int) p.range());
				if (s != null) {
					s.done = true;
				}
			}
			case "smoke_cloud" -> {
				Cloud c = new Cloud(p.pos(), p.size(), (int) p.speed());
				GrandLineFx.add(c);
				GrandLineAudio.loop(GrandLineSounds.SMOKE_CLOUD_LOOP, () -> c.at, () -> !c.done && c.age < c.life, 0.7f, () -> 1.0, null);
			}
			case "smoke_slash" -> {
				GrandLineFx.add(new Slash(p.pos(), (int) p.speed(), r.nextDouble() * Math.PI));
				if (now() != lastBurstSound) {
					lastBurstSound = now();
					GrandLineAudio.play(GrandLineSounds.SMOKE_SLASH, p.pos(), 0.8f, 0.9f + r.nextFloat() * 0.25f);
				}
			}
			case "smoke_cloud_pulse" -> GrandLineFx.add(new Ring(p.pos().add(0, 0.15, 0), p.size() * 0.9, 8, LIGHT));
			case "smoke_liberation" -> {
				BodyMotion.smoke(id, age, 2);
				hide(id, RIGHT_ARM | LEFT_ARM, 24);
				GrandLineFx.add(new Vortex(p.pos(), p.size(), p.range(), (int) p.speed()));
				GrandLineFx.add(new Ring(p.pos().add(0, 0.1, 0), p.size() * 1.3, 14, IMPACT));
				SpringFx.ground(level, p.pos(), p.size() * 0.7, 14);
				GrandLineAudio.play(GrandLineSounds.SMOKE_LIBERATION, p.pos(), 1f, 1f);
				shake(p.pos(), 0.9f);
			}
			case "smoke_liberation_hit" -> {
				puffs(level, p.pos(), 0.5, 3, 0.6, 14, p.dir().scale(0.08));
				if (now() != lastBurstSound) {
					lastBurstSound = now();
					GrandLineAudio.play(GrandLineSounds.SMOKE_LIBERATION_HIT, p.pos(), 0.7f, 0.9f + r.nextFloat() * 0.2f);
				}
			}
			case "smoke_bomber_start" -> {
				BodyMotion.smoke(id, age, 3);
				Flight f = new Flight(id);
				Flight old = FLIGHTS.put(id, f);
				if (old != null) {
					old.done = true;
				}
				GrandLineFx.add(f);
				GrandLineAudio.play(GrandLineSounds.SMOKE_BOMBER_START, p.pos().add(0, 1, 0), 1f, 1f);
				if (caster != null) {
					GrandLineAudio.loop(GrandLineSounds.SMOKE_BOMBER_LOOP, () -> caster.position().add(0, 1, 0), () -> !f.done && !f.ending && caster.isAlive(),
							0.7f, () -> 0.9 + 0.3 * f.speedK, null);
				}
			}
			case "smoke_bomber_end" -> {
				BodyMotion.stop(id, BodyMotion.Kind.SMOKE, age);
				Flight f = FLIGHTS.remove(id);
				if (f != null) {
					f.end();
				}
				GrandLineAudio.play(GrandLineSounds.SMOKE_REFORM, p.pos().add(0, 1, 0), 0.9f, 1f);
			}
			case "smoke_bomber" -> {
				Flight f = FLIGHTS.get(id);
				if (f != null) {
					f.heard = 0;
				}
			}
			case "smoke_bomb" -> {
				Projectile s = new Projectile(p.pos(), p.dir(), p.speed(), 0.32, 70);
				SHOTS.put((int) p.range(), s);
				GrandLineFx.add(s);
				GrandLineAudio.play(GrandLineSounds.SMOKE_BOMB_DROP, p.pos(), 0.5f, 0.9f + r.nextFloat() * 0.2f);
			}
			case "smoke_bomb_hit" -> {
				Projectile s = SHOTS.remove((int) p.range());
				if (s != null) {
					s.done = true;
				}
				if (p.size() > 0) {
					GrandLineFx.add(new Burst(p.pos(), p.size(), 22, false));
					GrandLineFx.add(new Ring(p.pos().add(0, 0.1, 0), p.size() * 1.2, 9, LIGHT));
					SpringFx.ground(level, p.pos(), 1.2, 4);
					if (now() - lastBurstSound >= 2 || now() < lastBurstSound) {
						lastBurstSound = now();
						GrandLineAudio.play(GrandLineSounds.SMOKE_BOMB_BURST, p.pos(), 0.75f, 0.9f + r.nextFloat() * 0.2f);
					}
				}
			}
			default -> { }
		}
	}

	// ------------------------------------------------------------------------------------------------ effects

	/** Elemental Reflex: the region bursts into smoke pushed along the attack, then flows back and reforms. */
	static final class Disperse extends GrandLineFx.Effect {
		final Vec3 contact, dir;
		final int region;
		final double seed = Math.random() * 50;

		Disperse(int caster, Vec3 contact, Vec3 dir, int region) {
			super(caster);
			this.contact = contact;
			this.dir = dir.lengthSqr() < 1e-6 ? new Vec3(0, 0, 1) : dir.normalize();
			this.region = region;
		}

		@Override
		boolean tick(ClientLevel level) {
			return ++age < 14;
		}

		@Override
		void render(FxDraw d, ClientLevel level) {
			Entity e = caster(level);
			if (e == null) {
				return;
			}
			float a = age + d.partial;
			Vec3 body = e.getPosition(d.partial);
			Vec3 anchor = contact.subtract(e.position()).add(body);
			// Out along the attack (0..4), then drawn back (4..12); dense where the body was.
			double out = a < 4 ? WindMesh.ease(a / 4) * 0.9 : 0.9 * (1 - WindMesh.ease(Math.min(1, (a - 4) / 8)));
			float fade = a < 9 ? 1 : Math.max(0, 1 - (a - 9) / 5f);
			double size = (region & HEAD) != 0 ? 0.38 : (region & LEGS) != 0 ? 0.42 : 0.5;
			volume(d, anchor, size, seed, a * 2, 0.75f * fade, 1 - fade, 2, MAIN);
			Vec3 pushed = anchor.add(dir.scale(out));
			volume(d, pushed, size * 0.7, seed + 9, a * 2, 0.5f * fade, 1 - fade * 0.8f, 1, LIGHT);
			List<Vec3> pts = List.of(pushed.add(dir.scale(0.4)), pushed, anchor.add(dir.scale(out * 0.4)), anchor);
			d.ribbonSoft(FxDraw.STREAK, pts, new double[]{0.15, 0.3, 0.32, 0.22}, FxDraw.rgba(LIGHT, 1), new float[]{0, 0.4f * fade, 0.35f * fade, 0.1f}, a * 0.1f);
		}
	}

	/** Smoke Slam: the right arm becomes smoke, reaches out and slams down on the spot, then flows back. */
	static final class SlamArm extends GrandLineFx.Effect {
		final Vec3 at;
		final int hitTick;
		final double seed = Math.random() * 50;

		SlamArm(int caster, Vec3 at, int hitTick) {
			super(caster);
			this.at = at;
			this.hitTick = hitTick;
		}

		@Override
		boolean tick(ClientLevel level) {
			age++;
			if (age == 2) {
				hide(caster, RIGHT_ARM, hitTick + 10);
			}
			return age < hitTick + 12;
		}

		@Override
		void render(FxDraw d, ClientLevel level) {
			Entity e = caster(level);
			if (e == null) {
				return;
			}
			float a = age + d.partial;
			Vec3 sh = shoulder(e, d.partial, false);
			Vec3 hand = GrandLineFx.handPosition(e, d.partial, false);
			// Reach: 0 at the hand → 1 at the impact spot, then back.
			double reach = a < 3 ? 0 : a < hitTick ? WindMesh.ease((a - 3) / (hitTick - 3)) : a < hitTick + 4 ? 1 : 1 - WindMesh.ease((a - hitTick - 4) / 8);
			float dens = Math.min(1, a / 3f) * (a > hitTick + 8 ? Math.max(0, 1 - (a - hitTick - 8) / 4f) : 1);
			Vec3 tip = hand.lerp(at.add(0, 0.4, 0), reach);
			Vec3 mid = sh.lerp(tip, 0.5).add(0, 1.2 * reach * (a < hitTick ? 1 : 0.4), 0);
			limb(d, sh, mid, tip, 0.16, 0.16 + 0.45 * reach, a * 2, 0.85f * dens, seed);
		}
	}

	/** An expanding burst of compressed smoke (impacts, detonations): dense core rushing out and thinning. */
	static final class Burst extends GrandLineFx.Effect {
		final Vec3 at;
		final double radius;
		final int life;
		final boolean ground;
		final double seed = Math.random() * 50;

		Burst(Vec3 at, double radius, int life, boolean ground) {
			super(-1);
			this.at = at;
			this.radius = radius;
			this.life = life;
			this.ground = ground;
		}

		@Override
		boolean tick(ClientLevel level) {
			return ++age < life;
		}

		@Override
		void render(FxDraw d, ClientLevel level) {
			float a = age + d.partial;
			float t = a / life;
			double r = radius * (0.35 + 0.65 * WindMesh.ease(Math.min(1, a / 5)));
			volume(d, at.add(0, ground ? r * 0.2 + t * 0.6 : t * 0.4, 0), r * (ground ? 0.75 : 0.85), seed, a * 1.5f, 0.8f * (1 - t * 0.5f), t * 0.9f, 3, MAIN);
			if (ground && a < 8) {
				float k = a / 8;
				Vec3[] b = WindMesh.basis(new Vec3(0, 1, 0));
				WindRender.with(WindRender.Material.WIND, () -> WindMesh.ring(d, false, at.subtract(0, 0.5, 0), b[0], b[1], radius * (0.3 + 1.1 * k),
						radius * (0.6 + 1.2 * k), 0, 360, IMPACT, 0.35f * (1 - k), k, k));
			}
		}
	}

	/** A flat pressure ring of smoke-gray air expanding along the ground. */
	static final class Ring extends GrandLineFx.Effect {
		final Vec3 at;
		final double radius;
		final int life, colour;

		Ring(Vec3 at, double radius, int life, int colour) {
			super(-1);
			this.at = at;
			this.radius = radius;
			this.life = life;
			this.colour = colour;
		}

		@Override
		boolean tick(ClientLevel level) {
			return ++age < life;
		}

		@Override
		void render(FxDraw d, ClientLevel level) {
			float t = (age + d.partial) / life;
			double r = radius * WindMesh.ease(Math.sqrt(t));
			Vec3[] b = WindMesh.basis(new Vec3(0, 1, 0));
			WindRender.with(WindRender.Material.SMOKE, () -> WindMesh.ring(d, false, at, b[0], b[1], Math.max(0, r - radius * 0.3), r, 0, 360, colour,
					0.6f * (1 - t), t * 0.8f, t));
		}
	}

	/** A travelling smoke mass (Smoke Blast, falling smoke bombs): same motion as the server, with a turbulent trail. */
	static final class Projectile extends GrandLineFx.Effect {
		Vec3 pos, prev, vel;
		final double gravity, radius;
		final int maxLife;
		final List<Vec3> trail = new ArrayList<>();
		final double seed = Math.random() * 50;

		Projectile(Vec3 pos, Vec3 vel, double gravity, double radius, int maxLife) {
			super(-1);
			this.pos = pos;
			this.prev = pos;
			this.vel = vel;
			this.gravity = gravity;
			this.radius = radius;
			this.maxLife = maxLife;
		}

		@Override
		boolean tick(ClientLevel level) {
			if (++age > maxLife) {
				return false;
			}
			prev = pos;
			pos = pos.add(vel);
			if (gravity > 0) {
				vel = vel.scale(0.98).add(0, -gravity, 0);
			}
			trail.add(0, prev);
			while (trail.size() > 4 + (int) (4 * WindMesh.detail())) {
				trail.remove(trail.size() - 1);
			}
			if (age % 2 == 0) {
				FxSprites.sprite(FxDraw.CLOUD, false, prev, vel.scale(-0.05), radius * 0.9, radius * 0.06, -0.002, 0.9, 14, MAIN, LIGHT, 0.3f);
			}
			return true;
		}

		@Override
		void render(FxDraw d, ClientLevel level) {
			Vec3 p = prev.lerp(pos, d.partial);
			float a = age + d.partial;
			volume(d, p, radius, seed, a * 3, 0.9f, 0, 2, DENSE);
			if (trail.size() > 1) {
				List<Vec3> pts = new ArrayList<>(trail);
				pts.add(0, p);
				double[] w = new double[pts.size()];
				float[] al = new float[pts.size()];
				for (int i = 0; i < w.length; i++) {
					double k = i / (w.length - 1.0);
					w[i] = radius * 1.6 * (1 - 0.6 * k);
					al[i] = (float) (0.45 * (1 - k));
				}
				d.ribbonSoft(FxDraw.STREAK, pts, w, FxDraw.rgba(LIGHT, 1), al, a * 0.08f);
			}
		}
	}

	/** Smoke Blast's lingering cloud: grows in, churns in place, thins out after its duration. */
	static final class Cloud extends GrandLineFx.Effect {
		final Vec3 at;
		final double radius;
		final int life;
		final double[] seeds = new double[5];

		Cloud(Vec3 at, double radius, int duration) {
			super(-1);
			this.at = at;
			this.radius = radius;
			this.life = duration;
			for (int i = 0; i < seeds.length; i++) {
				seeds[i] = Math.random() * 50;
			}
		}

		@Override
		boolean tick(ClientLevel level) {
			age++;
			if (age % 6 == 0 && age < life) {
				puffs(level, at.add(0, 0.8, 0), radius * 0.8, 2, 1.0, 26, new Vec3(0, 0.01, 0));
			}
			return age < life + 18;
		}

		@Override
		void render(FxDraw d, ClientLevel level) {
			float a = age + d.partial;
			float grow = (float) WindMesh.ease(Math.min(1, a / 6));
			float out = a < life ? 0 : Math.min(1, (a - life) / 18f);
			volume(d, at.add(0, 0.9, 0), radius * 0.55 * grow, seeds[0], a, 0.8f * (1 - out), out, 3, DENSE);
			int n = 2 + (int) Math.round(2 * WindMesh.detail());
			for (int i = 0; i < n; i++) {
				double ang = i * Math.PI * 2 / n + a * 0.03;
				Vec3 c = at.add(Math.cos(ang) * radius * 0.5, 0.6 + 0.3 * Math.sin(a * 0.05 + i), Math.sin(ang) * radius * 0.5);
				volume(d, c, radius * 0.38 * grow, seeds[i + 1], a * 1.2f, 0.55f * (1 - out), out, 2, MAIN);
			}
		}
	}

	/** A smoke slash cutting through the cloud: two intersecting curved blades, lighter than the smoke around them. */
	static final class Slash extends GrandLineFx.Effect {
		final Vec3 at;
		final int index;
		final double roll;

		Slash(Vec3 at, int index, double roll) {
			super(-1);
			this.at = at;
			this.index = index;
			this.roll = roll;
		}

		@Override
		boolean tick(ClientLevel level) {
			return ++age < 7;
		}

		@Override
		void render(FxDraw d, ClientLevel level) {
			float t = (age + d.partial) / 7f;
			for (int k = 0; k < 2; k++) {
				double ang = roll + k * Math.PI / 2 + index * 0.9;
				Vec3 fwd = new Vec3(Math.cos(ang), 0.35 * (k == 0 ? 1 : -1), Math.sin(ang)).normalize();
				Vec3[] b = WindMesh.basis(fwd);
				Vec3 side = b[0], up = fwd.cross(side).normalize();
				Vec3 centre = at.subtract(fwd.scale(0.9 - 0.8 * t));
				WindRender.with(WindRender.Material.SMOKE, () -> WindMesh.crescent(d, false, centre, fwd, side, up, 1.2, 0.35, 150, HIGHLIGHT,
						0.85f * (1 - t), t * 0.7f, t));
			}
		}
	}

	/** Smoke Liberation: a dense core column, two counter-turning deformed funnels, spiral ribbons and outer clouds. */
	static final class Vortex extends GrandLineFx.Effect {
		final Vec3 at;
		final double radius, height;
		final int life;
		final double seed = Math.random() * 50;

		Vortex(Vec3 at, double radius, double height, int life) {
			super(-1);
			this.at = at;
			this.radius = radius;
			this.height = height;
			this.life = life;
		}

		@Override
		boolean tick(ClientLevel level) {
			age++;
			if (age % 3 == 0 && age < life) {
				double r = radius * grow(age);
				for (int i = FxSprites.count(3); i > 0; i--) {
					double ang = level.random.nextDouble() * Math.PI * 2;
					Vec3 p = at.add(Math.cos(ang) * r, level.random.nextDouble() * height * 0.6, Math.sin(ang) * r);
					FxSprites.sprite(FxDraw.CLOUD, false, p, new Vec3(-Math.sin(ang), 0.05, Math.cos(ang)).scale(0.25), 1.2, 0.04, -0.002, 0.9, 20,
							MAIN, LIGHT, 0.3f);
				}
			}
			return age < life + 16;
		}

		double grow(float a) {
			return 1 - Math.pow(1 - Math.min(1, a / 8.0), 3);
		}

		@Override
		void render(FxDraw d, ClientLevel level) {
			float a = age + d.partial;
			double g = grow(a);
			float out = a < life ? 0 : Math.min(1, (a - life) / 16f);
			double r = radius * g;
			double h = height * (0.4 + 0.6 * g);
			// Dense central formation (stacked masses that churn upward, the vortex's core).
			for (int i = 0; i < 2; i++) {
				volume(d, at.add(0, 0.8 + i * h * 0.3, 0), (0.9 + 0.4 * i) * g + 0.2, seed + i, a * 2, 0.38f * (1 - out), out, 2, DENSE);
			}
			// Two funnels turning in opposite directions at different speeds: inner dense, outer soft.
			vortex(d, at, h, r * 0.35, r * 0.75, a, 0.22, seed, DENSE, 0.12f * (1 - out), Math.max(out, 0.4f), 0.2f);
			vortex(d, at, h * 1.05, r * 0.6, r, a, -0.14, seed + 2, MAIN, 0.07f * (1 - out), Math.min(1, out + 0.5f), 0.6f);
			// Spiral smoke ribbons climbing round the funnel.
			for (int k = 0; k < 3; k++) {
				List<Vec3> pts = new ArrayList<>();
				int n = 10;
				for (int i = 0; i <= n; i++) {
					double t = i / (double) n;
					double ang = a * 0.3 + k * 2.1 + t * 5;
					double rr = r * (0.5 + 0.45 * t);
					pts.add(at.add(Math.cos(ang) * rr, t * h, Math.sin(ang) * rr));
				}
				double[] w = new double[pts.size()];
				float[] al = new float[pts.size()];
				for (int i = 0; i < w.length; i++) {
					w[i] = 0.5 + 0.4 * i / w.length;
					al[i] = 0.4f * (1 - out) * (float) Math.sin(Math.PI * i / (w.length - 1));
				}
				d.ribbonSoft(FxDraw.STREAK, pts, w, FxDraw.rgba(LIGHT, 1), al, a * 0.1f);
			}
		}
	}

	/** Smoke Bomber: both arms become smoke streams from the shoulders, stronger with speed; reform when it ends. */
	static final class Flight extends GrandLineFx.Effect {
		final List<Vec3> left = new ArrayList<>(), right = new ArrayList<>();
		final double seed = Math.random() * 50;
		boolean ending;
		int endAge, heard;
		float speedK;
		Vec3 last;

		Flight(int caster) {
			super(caster);
		}

		void end() {
			ending = true;
		}

		@Override
		void cancel() {
			ending = true;
			show(caster, RIGHT_ARM | LEFT_ARM);
		}

		@Override
		boolean tick(ClientLevel level) {
			age++;
			Entity e = caster(level);
			if (e == null || ++heard > 40) {
				show(caster, RIGHT_ARM | LEFT_ARM);
				return false;
			}
			if (ending) {
				if (++endAge == 6) {
					show(caster, RIGHT_ARM | LEFT_ARM); // arms reform as the smoke flows back
				}
				if (!left.isEmpty()) {
					left.remove(left.size() - 1);
					right.remove(right.size() - 1);
				}
				return endAge < 10;
			}
			if (age >= 3) {
				hide(caster, RIGHT_ARM | LEFT_ARM, 6);
			}
			Vec3 pos = e.position();
			double speed = last == null ? 0 : pos.distanceTo(last);
			last = pos;
			speedK = (float) Mth.clamp(speed / 0.3, 0, 1);
			left.add(0, shoulder(e, 1, true));
			right.add(0, shoulder(e, 1, false));
			int keep = 5 + (int) (4 * WindMesh.detail());
			while (left.size() > keep) {
				left.remove(left.size() - 1);
				right.remove(right.size() - 1);
			}
			if (age % Math.max(1, 4 - (int) (3 * speedK)) == 0) {
				puffs(level, pos.add(0, 1.0, 0), 0.4, 1, 0.5 + 0.3 * speedK, 16, new Vec3(0, -0.03, 0));
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
			float grow = Math.min(1, a / 6f) * (ending ? Math.max(0, 1 - endAge / 8f) : 1);
			for (boolean isLeft : new boolean[]{false, true}) {
				Vec3 sh = shoulder(e, d.partial, isLeft);
				Vec3 down = sh.add(0, -0.7 - 0.3 * speedK, 0).subtract(e.getDeltaMovement().scale(2.5));
				limb(d, sh, sh.lerp(down, 0.5).add(0, -0.1, 0), down, 0.16, 0.24 + 0.1 * speedK, a * 2, 0.8f * grow, seed + (isLeft ? 7 : 0));
				List<Vec3> hist = isLeft ? left : right;
				if (hist.size() > 1) {
					List<Vec3> pts = new ArrayList<>();
					pts.add(down);
					for (Vec3 h : hist) {
						pts.add(h.add(0, -0.8, 0));
					}
					double[] w = new double[pts.size()];
					float[] al = new float[pts.size()];
					for (int i = 0; i < w.length; i++) {
						double k = i / (w.length - 1.0);
						w[i] = (0.35 + 0.35 * speedK) * (1 - 0.5 * k);
						al[i] = (float) ((0.25 + 0.3 * speedK) * (1 - k)) * grow;
					}
					d.ribbonSoft(FxDraw.STREAK, pts, w, FxDraw.rgba(LIGHT, 1), al, a * 0.1f);
				}
			}
		}
	}
}
