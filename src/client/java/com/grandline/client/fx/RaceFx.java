package com.grandline.client.fx;

import com.grandline.GrandLineMod;
import com.grandline.client.race.ClientRace;
import com.grandline.network.payload.FxEventPayload;
import com.grandline.race.Race;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Race visuals. Cosmetics are drawn every frame on every visible player from their synced race look (race, stage,
 * transformation), so everyone sees the same thing: Rabbit ears, a Shark fin, an Angel halo (wings from V2), a Cyborg
 * visor (shoulder lights from V2), Ghoul eyes (glowing at night or from V2, tendrils while their power is active).
 * They grow with the stage and flare while a V3 ability or V4 awakening is on. Bursts mark V3, V4 and evolution; an
 * aura (refreshed by the server every second) shows an active ability.
 */
final class RaceFx {
	static final ResourceLocation EAR = GrandLineMod.id("textures/fx/race_ear.png");
	static final ResourceLocation FIN = GrandLineMod.id("textures/fx/race_fin.png");
	static final ResourceLocation WING = GrandLineMod.id("textures/fx/race_wing.png");
	static final ResourceLocation VISOR = GrandLineMod.id("textures/fx/race_visor.png");

	private RaceFx() {
	}

	static boolean handles(String effect) {
		return effect.startsWith("race_");
	}

	/** Main and accent colour per race. */
	static int main(Race race) {
		return switch (race) {
			case HUMAN -> 0xFF5A2E;
			case SHARK -> 0x3AB0FF;
			case ANGEL -> 0xFFD970;
			case RABBIT -> 0x6BFF86;
			case CYBORG -> 0x4FE6FF;
			case GHOUL -> 0xD0142E;
		};
	}

	static int accent(Race race) {
		return switch (race) {
			case HUMAN -> 0xFFD25A;
			case SHARK -> 0xD8F6FF;
			case ANGEL -> 0xFFFFFF;
			case RABBIT -> 0xEFFFEF;
			case CYBORG -> 0xFFFFFF;
			case GHOUL -> 0x4A0010;
		};
	}

	static void handle(FxEventPayload p, ClientLevel level, Entity caster) {
		String e = p.effect();
		if (e.equals("race_evolve")) {
			GrandLineFx.add(new Burst(p.caster(), p.pos(), 0xFFD25A, 0xFFFFFF, 2.2 + p.size() * 0.4, 30, 3.5, 2));
			return;
		}
		Race race = Race.byId(e.substring(e.lastIndexOf('_') + 1));
		if (e.startsWith("race_pulse_")) {
			GrandLineFx.add(new Burst(p.caster(), p.pos(), main(race), accent(race), p.size(), 14, 0, 4));
		} else if (e.startsWith("race_v3_")) {
			GrandLineFx.add(new Burst(p.caster(), p.pos(), main(race), accent(race), 3.0, 22, 2.6, styleOf(race)));
		} else if (e.startsWith("race_v4_")) {
			GrandLineFx.add(new Burst(p.caster(), p.pos(), main(race), accent(race), 5.0, 34, 5.0, styleOf(race)));
			GrandLineFx.add(new Burst(p.caster(), p.pos(), accent(race), main(race), 2.5, 20, 0, styleOf(race)));
		} else if (e.startsWith("race_aura_")) {
			Aura aura = GrandLineFx.find(Aura.class, p.caster());
			if (aura == null) {
				GrandLineFx.add(new Aura(p.caster(), race, p.size() > 1.2));
			} else {
				aura.refresh(p.size() > 1.2);
			}
		}
	}

	/** Burst styles: 0 shockwave, 1 water spiral, 2 light column, 3 speed lines, 4 electric pulse, 5 blood spiral. */
	static int styleOf(Race race) {
		return race.ordinal();
	}

	// ------------------------------------------------------------------------------------------------ bursts

	static final class Burst extends GrandLineFx.Effect {
		final Vec3 at;
		final int color, accent, life, style;
		final double radius, height;

		Burst(int caster, Vec3 at, int color, int accent, double radius, int life, double height, int style) {
			super(caster);
			this.at = at;
			this.color = color;
			this.accent = accent;
			this.radius = radius;
			this.life = life;
			this.height = height;
			this.style = style;
			RandomSource r = RandomSource.create();
			int n = FxSprites.count(height > 0 ? 26 : 10);
			for (int i = 0; i < n; i++) {
				double a = r.nextDouble() * Math.PI * 2;
				Vec3 out = new Vec3(Math.cos(a), 0, Math.sin(a));
				Vec3 vel = switch (style) {
					case 1, 5 -> out.scale(0.08).add(new Vec3(-out.z, 0, out.x).scale(0.18)).add(0, 0.12 + r.nextDouble() * 0.12, 0);
					case 2 -> new Vec3(r.nextGaussian() * 0.03, -0.02 - r.nextDouble() * 0.03, r.nextGaussian() * 0.03);
					case 3 -> out.scale(0.45 + r.nextDouble() * 0.3).add(0, 0.02, 0);
					case 4 -> out.scale(0.3 + r.nextDouble() * 0.25).add(0, r.nextGaussian() * 0.12, 0);
					default -> out.scale(0.22 + r.nextDouble() * 0.2).add(0, 0.15 + r.nextDouble() * 0.2, 0);
				};
				Vec3 start = style == 2 ? at.add(out.scale(r.nextDouble() * 0.9)).add(0, 1 + r.nextDouble() * 2.5, 0) : at.add(0, 0.2 + r.nextDouble(), 0);
				if (style == 0 || style == 5) {
					FxSprites.ember(start, vel, 0.08 + r.nextDouble() * 0.06, 14 + r.nextInt(12), accent, color);
				} else {
					FxSprites.spark(start, vel, 0.06 + r.nextDouble() * 0.06, 10 + r.nextInt(14), accent, color);
				}
			}
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
			float t = (age + d.partial) / life;
			float fade = 1 - t;
			double ease = 1 - Math.pow(1 - t, 3);
			Vec3 base = at.add(0, 0.05, 0);
			d.groundRing(base, radius * ease, 0.5 + radius * 0.08, 40, FxDraw.rgba(color, 0.9f * fade));
			d.groundRing(base, radius * ease * 0.7, 0.25, 32, FxDraw.rgba(accent, 0.7f * fade));
			if (height > 0) {
				double h = height * (style == 2 ? 1.6 : 1) * Math.min(1, t * 4);
				d.ringWall(at, 0.6 + ease * (style == 4 ? radius * 0.5 : 0.5), h, 24, FxDraw.rgba(color, 0.6f * fade), t * 2);
				if (style == 2) {
					d.ringWall(at, 0.35, h * 1.4, 16, FxDraw.rgba(accent, 0.8f * fade), -t * 3);
				}
			}
			if (style == 1 || style == 5) { // a helix around the body
				List<Vec3> pts = new ArrayList<>();
				int n = 22;
				double[] w = new double[n];
				float[] a = new float[n];
				for (int i = 0; i < n; i++) {
					double k = i / (double) (n - 1);
					double ang = k * Math.PI * 4 + t * 8;
					double r = 0.9 - k * 0.3;
					pts.add(at.add(Math.cos(ang) * r, k * 2.6 * Math.min(1, t * 3), Math.sin(ang) * r));
					w[i] = 0.35 * (1 - k * 0.6);
					a[i] = 0.8f * fade * (float) (1 - k * 0.7);
				}
				d.ribbon(FxDraw.GLOW, pts, w, rgb(color), a, t);
			}
			if (style == 3) { // speed lines
				for (int i = 0; i < 10; i++) {
					double ang = i * Math.PI / 5 + 0.3;
					Vec3 dir = new Vec3(Math.cos(ang), 0, Math.sin(ang));
					Vec3 p0 = at.add(0, 0.4 + (i % 3) * 0.5, 0).add(dir.scale(0.6 + ease * radius * 0.6));
					d.ribbon(FxDraw.STREAK, List.of(p0.add(dir.scale(1.4)), p0), new double[]{0.02, 0.16}, rgb(accent),
							new float[]{0, 0.9f * fade}, 0);
				}
			}
			d.billboard(FxDraw.GLOW, true, at.add(0, 1, 0), 1.2 * (0.6 + t), 0, FxDraw.rgba(color, 0.6f * fade));
		}
	}

	// ------------------------------------------------------------------------------------------------ active aura

	/** Shown while a V3 ability or V4 awakening is active (the server refreshes it every second). */
	static final class Aura extends GrandLineFx.Effect {
		final Race race;
		boolean big;
		int until = 26;

		Aura(int caster, Race race, boolean big) {
			super(caster);
			this.race = race;
			this.big = big;
		}

		void refresh(boolean big) {
			this.big = big;
			until = age + 26;
		}

		@Override
		boolean tick(ClientLevel level) {
			Entity c = caster(level);
			if (c != null && age % 2 == 0) {
				RandomSource r = c.level().random;
				Vec3 p = c.position().add(r.nextGaussian() * 0.35, 0.1 + r.nextDouble() * 1.6, r.nextGaussian() * 0.35);
				Vec3 v = race == Race.ANGEL ? new Vec3(0, -0.02, 0) : race == Race.GHOUL ? new Vec3(0, 0.01, 0) : new Vec3(0, 0.05, 0);
				FxSprites.spark(p, v, big ? 0.1 : 0.07, 12, accent(race), main(race));
			}
			return ++age < until;
		}

		@Override
		void render(FxDraw d, ClientLevel level) {
			Entity c = caster(level);
			if (c == null) {
				return;
			}
			float time = age + d.partial;
			float fade = Math.min(1, (until - time) / 6f);
			float pulse = 0.7f + 0.3f * Mth.sin(time * 0.35f);
			Vec3 feet = c.getPosition(d.partial).add(0, 0.05, 0);
			double s = big ? 1.4 : 1;
			d.groundRing(feet, 0.85 * s, 0.22, 28, FxDraw.rgba(main(race), 0.75f * fade * pulse));
			if (big) {
				d.groundRing(feet, 1.35 + 0.1 * Mth.sin(time * 0.2f), 0.12, 36, FxDraw.rgba(accent(race), 0.6f * fade));
				d.ringWall(feet, 0.75, 2.1, 20, FxDraw.rgba(main(race), 0.28f * fade * pulse), time * 0.04f);
			}
			d.billboard(FxDraw.GLOW, true, feet.add(0, 1.0, 0), 0.9 * s, 0, FxDraw.rgba(main(race), 0.22f * fade * pulse));
		}
	}

	// ------------------------------------------------------------------------------------------------ cosmetics

	static void renderCosmetics(FxDraw d, ClientLevel level) {
		Minecraft mc = Minecraft.getInstance();
		for (Player p : level.players()) {
			ClientRace.Look look = ClientRace.look(p);
			if (look == null || p.isInvisible() || p.isSpectator() || p.distanceToSqr(mc.gameRenderer.getMainCamera().getPosition()) > 64 * 64) {
				continue;
			}
			boolean firstPerson = p == mc.player && mc.options.getCameraType().isFirstPerson();
			if (firstPerson || p.isVisuallySwimming() || p.isFallFlying() || p.isSleeping() || p.getPose() == Pose.SPIN_ATTACK) {
				continue;
			}
			draw(d, p, look, level);
		}
	}

	private static void draw(FxDraw d, Player p, ClientRace.Look look, ClientLevel level) {
		float pt = d.partial;
		Vec3 pos = p.getPosition(pt);
		float bodyYaw = Mth.rotLerp(pt, p.yBodyRotO, p.yBodyRot) * Mth.DEG_TO_RAD;
		float headYaw = Mth.rotLerp(pt, p.yHeadRotO, p.yHeadRot) * Mth.DEG_TO_RAD;
		float pitch = Mth.lerp(pt, p.xRotO, p.getXRot()) * Mth.DEG_TO_RAD;
		boolean crouch = p.isCrouching();
		Vec3 bf = new Vec3(-Mth.sin(bodyYaw), 0, Mth.cos(bodyYaw));
		Vec3 br = new Vec3(-Mth.cos(bodyYaw), 0, -Mth.sin(bodyYaw));
		Vec3 hf = new Vec3(-Mth.sin(headYaw), 0, Mth.cos(headYaw));
		Vec3 hr = new Vec3(-Mth.cos(headYaw), 0, -Mth.sin(headYaw));
		Vec3 up = new Vec3(0, 1, 0);
		// Player model: neck at 1.40 (1.12 crouching), head 0.47 tall, front face 0.24 ahead of its centre.
		double neck = crouch ? 1.12 : 1.40;
		Vec3 headCenter = pos.add(0, neck, 0).add(crouch ? bf.scale(0.25) : Vec3.ZERO).add(headUpOf(pitch, hf).scale(0.22));
		// The head pivots at the neck: forward-tilt follows the look pitch.
		Vec3 headUp = up.scale(Math.cos(pitch)).add(hf.scale(Math.sin(pitch)));
		Vec3 headFwd = hf.scale(Math.cos(pitch)).subtract(up.scale(Math.sin(pitch)));
		Vec3 headTop = pos.add(0, neck, 0).add(crouch ? bf.scale(0.25) : Vec3.ZERO).add(headUp.scale(0.47));
		float time = p.tickCount + pt;
		int stage = Math.max(1, look.stage());
		boolean active = look.mode() > 0, awakened = look.mode() == 2;
		float grow = 0.85f + 0.1f * stage + (awakened ? 0.25f : 0);
		boolean night = com.grandline.race.RacePassives.isNight(level);
		switch (look.race()) {
			case RABBIT -> {
				for (int side = -1; side <= 1; side += 2) {
					double sway = Mth.sin(time * 0.15f + side) * 0.06 + (p.isSprinting() ? 0.35 : 0.1);
					Vec3 base = headTop.add(hr.scale(side * 0.13)).subtract(headFwd.scale(0.02));
					Vec3 dir = headUp.scale(Math.cos(sway)).subtract(headFwd.scale(Math.sin(sway))).add(hr.scale(side * 0.12)).normalize();
					double h = 0.42 * grow, w = 0.1 * grow;
					Vec3 a = base.subtract(hr.scale(w)), b = base.add(hr.scale(w));
					float[] col = awakened ? FxDraw.rgba(0xD8FFD8, 1) : FxDraw.rgba(0xFFFFFF, 1);
					doubleQuad(d, EAR, false, a, b, b.add(dir.scale(h)), a.add(dir.scale(h)), col);
					if (active) {
						d.billboard(FxDraw.GLOW, true, base.add(dir.scale(h * 0.6)), 0.18, 0, FxDraw.rgba(main(Race.RABBIT), 0.5f));
					}
				}
			}
			case SHARK -> {
				Vec3 back = pos.add(0, crouch ? 1.05 : 1.25, 0).subtract(bf.scale(0.17));
				double h = 0.3 * grow, w = 0.36 * grow;
				// The fin runs along the spine, turned up to 35 degrees toward the viewer so it never vanishes edge-on.
				Vec3 toCam = d.cameraPosition().subtract(back).multiply(1, 0, 1);
				double side = toCam.lengthSqr() < 1e-6 ? 0 : Math.signum(toCam.normalize().dot(br));
				double turn = Math.toRadians(35) * side;
				Vec3 along = bf.scale(-Math.cos(turn)).add(br.scale(Math.sin(turn))).normalize();
				Vec3 a = back.subtract(along.scale(0.05)), b = back.add(along.scale(w));
				float[] col = active ? FxDraw.rgba(0xBFE8FF, 1) : FxDraw.rgba(0xFFFFFF, 1);
				Vec3 lean = up.add(along.scale(0.35)).normalize();
				doubleQuad(d, FIN, false, a, b, b.add(lean.scale(h)), a.add(lean.scale(h)), col);
				if (awakened) {
					d.groundRing(pos.add(0, 0.9 + 0.4 * Mth.sin(time * 0.2f), 0), 0.9, 0.18, 28, FxDraw.rgba(0x7FD6FF, 0.6f));
				}
			}
			case ANGEL -> {
				double haloLift = 0.18 + 0.02 * Mth.sin(time * 0.1f);
				float glow = active ? 1 : 0.7f;
				halo(d, headTop.add(headUp.scale(haloLift)), headUp, 0.21 * (awakened ? 1.3 : 1), glow);
				if (stage >= 2 || active) {
					double size = (0.45 + 0.15 * stage) * (awakened ? 1.45 : 1);
					double flap = 0.35 + 0.25 * Mth.sin(time * (active ? 0.35f : 0.12f));
					Vec3 root = pos.add(0, crouch ? 1.15 : 1.35, 0).subtract(bf.scale(0.2));
					for (int side = -1; side <= 1; side += 2) {
						Vec3 out = br.scale(side * Math.cos(flap)).subtract(bf.scale(Math.sin(flap))).normalize();
						Vec3 r0 = root.add(br.scale(side * 0.08));
						Vec3 tip = r0.add(out.scale(size));
						Vec3 lift = up.scale(size * 0.55);
						Vec3 drop = up.scale(-size * 0.75);
						float[] col = FxDraw.rgba(awakened ? 0xFFF4C8 : 0xFFFFFF, 1);
						float u0 = 0, u1 = 1; // the root is always the texture's left edge (the geometry does the mirroring)
						d.quad(WING, false, r0.add(drop.scale(0.4)), tip.add(drop), tip.add(lift), r0.add(lift.scale(0.6)), col, u0, u1, 0, 1);
						d.quad(WING, false, r0.add(lift.scale(0.6)), tip.add(lift), tip.add(drop), r0.add(drop.scale(0.4)), col, u0, u1, 1, 0);
						if (active) {
							d.billboard(FxDraw.GLOW, true, tip.add(up.scale(0.1)), 0.35, 0, FxDraw.rgba(0xFFE38A, 0.45f));
						}
					}
				}
			}
			case CYBORG -> {
				Vec3 eyes = headCenter.add(headFwd.scale(0.245)).add(headUp.scale(0.01));
				double w = 0.27, h = 0.08 * (awakened ? 1.5 : 1);
				float bright = active ? 1 : 0.75f;
				float flicker = 0.85f + 0.15f * Mth.sin(time * 1.7f);
				d.quad(VISOR, true, eyes.subtract(hr.scale(w)).subtract(headUp.scale(h)), eyes.add(hr.scale(w)).subtract(headUp.scale(h)),
						eyes.add(hr.scale(w)).add(headUp.scale(h)), eyes.subtract(hr.scale(w)).add(headUp.scale(h)),
						FxDraw.rgba(0x4FE6FF, bright * flicker), 0, 1, 0, 1);
				if (stage >= 2) {
					for (int side = -1; side <= 1; side += 2) {
						Vec3 shoulder = pos.add(0, crouch ? 1.15 : 1.38, 0).add(br.scale(side * 0.38));
						d.billboard(FxDraw.GLOW, true, shoulder, 0.1 + (active ? 0.06 : 0), 0, FxDraw.rgba(0x4FE6FF, 0.8f * flicker));
					}
				}
				if (awakened && p.tickCount % 3 == 0) {
					RandomSource r = level.random;
					Vec3 s = pos.add(r.nextGaussian() * 0.4, 0.4 + r.nextDouble() * 1.4, r.nextGaussian() * 0.4);
					FxSprites.spark(s, new Vec3(r.nextGaussian() * 0.1, r.nextGaussian() * 0.1, r.nextGaussian() * 0.1), 0.07, 4, 0xFFFFFF, 0x4FE6FF);
				}
			}
			case GHOUL -> {
				if (night || stage >= 2 || active) {
					Vec3 eyes = headCenter.add(headFwd.scale(0.245)).add(headUp.scale(0.01));
					for (int side = -1; side <= 1; side += 2) {
						d.billboard(FxDraw.GLOW, true, eyes.add(hr.scale(side * 0.11)), 0.07 + (active ? 0.04 : 0), 0, FxDraw.rgba(0xFF1A3A, 0.95f));
					}
				}
				if (active) {
					int count = awakened ? 4 : 2;
					Vec3 root = pos.add(0, crouch ? 0.85 : 1.0, 0).subtract(bf.scale(0.18));
					for (int i = 0; i < count; i++) {
						double spread = (i - (count - 1) / 2.0) * 0.55;
						List<Vec3> pts = new ArrayList<>();
						int n = 12;
						double[] wd = new double[n];
						float[] al = new float[n];
						double len = awakened ? 2.4 : 1.6;
						for (int k = 0; k < n; k++) {
							double t = k / (double) (n - 1);
							double wave = Mth.sin(time * 0.25f + i * 1.7f + (float) t * 4) * 0.25 * t;
							Vec3 dir = bf.scale(-Math.cos(spread) * 0.8).add(br.scale(Math.sin(spread))).normalize();
							pts.add(root.add(dir.scale(t * len)).add(up.scale(Math.sin(t * Math.PI) * 0.7 + wave)).add(br.scale(wave)));
							wd[k] = 0.22 * (1 - t * 0.8);
							al[k] = 0.9f * (float) (1 - t * 0.5);
						}
						d.ribbon(FxDraw.GLOW, pts, wd, rgb(0xB0102A), al, time * 0.05f);
						double[] thin = new double[n];
						for (int k = 0; k < n; k++) {
							thin[k] = wd[k] * 0.35;
						}
						d.ribbon(FxDraw.STREAK, pts, thin, rgb(0xFF6070), al, 0);
					}
				}
			}
			case HUMAN -> {
				if (awakened) {
					Vec3 eyes = headCenter.add(headFwd.scale(0.245)).add(headUp.scale(0.01));
					for (int side = -1; side <= 1; side += 2) {
						d.billboard(FxDraw.GLOW, true, eyes.add(hr.scale(side * 0.11)), 0.08, 0, FxDraw.rgba(0xFFB23A, 0.9f));
					}
				}
			}
		}
	}

	private static Vec3 headUpOf(float pitch, Vec3 hf) {
		return new Vec3(0, Math.cos(pitch), 0).add(hf.scale(Math.sin(pitch)));
	}

	/** A glowing ring (camera-facing ribbon loop, so it reads from every angle, even edge-on). */
	private static void halo(FxDraw d, Vec3 center, Vec3 axis, double radius, float alpha) {
		Vec3 a = axis.cross(new Vec3(1, 0, 0));
		if (a.lengthSqr() < 1e-4) {
			a = axis.cross(new Vec3(0, 0, 1));
		}
		a = a.normalize();
		Vec3 b = axis.cross(a).normalize();
		int n = 25;
		List<Vec3> pts = new ArrayList<>();
		double[] wide = new double[n], thin = new double[n];
		float[] al = new float[n], al2 = new float[n];
		for (int i = 0; i < n; i++) {
			double ang = i * Math.PI * 2 / (n - 1);
			pts.add(center.add(a.scale(Math.cos(ang) * radius)).add(b.scale(Math.sin(ang) * radius)));
			wide[i] = 0.12;
			thin[i] = 0.045;
			al[i] = 0.55f * alpha;
			al2[i] = 0.95f * alpha;
		}
		d.ribbon(FxDraw.GLOW, pts, wide, rgb(0xFFD970), al, 0);
		d.ribbon(FxDraw.STREAK, pts, thin, rgb(0xFFF6D0), al2, 0);
	}

	/** A quad visible from both sides. */
	private static void doubleQuad(FxDraw d, ResourceLocation tex, boolean glow, Vec3 bl, Vec3 br, Vec3 tr, Vec3 tl, float[] rgba) {
		d.quad(tex, glow, bl, br, tr, tl, rgba, 0, 1, 0, 1);
		d.quad(tex, glow, br, bl, tl, tr, rgba, 1, 0, 0, 1);
	}

	static float[] rgb(int c) {
		float[] f = FxDraw.rgba(c, 1);
		return new float[]{f[0], f[1], f[2]};
	}
}
