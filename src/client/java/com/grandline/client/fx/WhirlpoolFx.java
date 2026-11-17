package com.grandline.client.fx;

import com.grandline.network.payload.FxEventPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Lotus Palace's whirlpools. The server sends a beat each second to players near a whirlpool; while beats keep coming
 * the whirlpool is drawn: three spiral bands lying on the water (or the court floor), cyan at the rim deepening to
 * blue, turning inward toward a dark centre, over a faint cyan disc, with water spray and bubbles rising along the
 * bands. A "whirlpool_burst" beat (someone passing through) makes it spin faster and brighter for a moment.
 */
final class WhirlpoolFx {
	private static final List<Pool> POOLS = new ArrayList<>();

	private WhirlpoolFx() {
	}

	static boolean handles(String effect) {
		return effect.startsWith("whirlpool");
	}

	static void handle(FxEventPayload p) {
		Pool pool = null;
		for (Pool q : POOLS) {
			if (!q.done && q.centre.distanceToSqr(p.pos()) < 1) {
				pool = q;
			}
		}
		if (pool == null) {
			pool = new Pool(p.pos(), p.size(), p.speed() < 0.5f);
			POOLS.add(pool);
			GrandLineFx.add(pool);
		}
		pool.life = 50;
		if (p.effect().equals("whirlpool_burst")) {
			pool.burst = 30;
		}
		POOLS.removeIf(q -> q.done);
	}

	static final class Pool extends GrandLineFx.Effect {
		final Vec3 centre;
		final double radius;
		final boolean surface;
		int life = 50;
		int burst;
		double spin;
		double spinO;
		float fade;

		Pool(Vec3 centre, double radius, boolean surface) {
			super(-1);
			this.centre = centre;
			this.radius = radius;
			this.surface = surface;
		}

		@Override
		boolean followsCaster() {
			return false;
		}

		@Override
		boolean tick(ClientLevel level) {
			age++;
			life--;
			if (burst > 0) {
				burst--;
			}
			fade = Math.min(1, Math.min(age / 20f, Math.max(0, life) / 20f));
			spinO = spin;
			spin += 0.045 * (1 + (burst > 0 ? burst / 10.0 : 0));
			Minecraft mc = Minecraft.getInstance();
			if (mc.player != null && mc.player.position().distanceToSqr(centre) < 48 * 48) {
				RandomSource r = level.random;
				int n = burst > 0 ? 8 : 3;
				for (int i = 0; i < n; i++) {
					double d = r.nextDouble() * radius, a = r.nextDouble() * Math.PI * 2;
					double x = centre.x + Math.cos(a) * d, z = centre.z + Math.sin(a) * d;
					double tx = -Math.sin(a) * 0.12, tz = Math.cos(a) * 0.12;
					if (surface) {
						level.addParticle(ParticleTypes.SPLASH, x, centre.y + 0.95, z, tx, 0.1, tz);
						level.addParticle(ParticleTypes.BUBBLE, x, centre.y + 0.3, z, tx * 0.3, 0.05, tz * 0.3);
					} else {
						level.addParticle(ParticleTypes.FALLING_WATER, x, centre.y + 0.4 + r.nextDouble() * 1.5, z, 0, 0, 0);
						level.addParticle(ParticleTypes.BUBBLE_POP, x, centre.y + 0.15, z, tx * 0.4, 0.04, tz * 0.4);
					}
				}
			}
			return life > 0;
		}

		@Override
		void render(FxDraw d, ClientLevel level) {
			double s = spinO + (spin - spinO) * d.partial;
			float alpha = fade * (burst > 0 ? 1f : 0.85f);
			double y = centre.y + (surface ? 0.93 : 0.04);
			Vec3 c = new Vec3(centre.x, y, centre.z);
			// faint disc under the bands
			disc(d, c, radius, FxDraw.rgba(0x37B5D8, 0.22f * alpha), false);
			// three spiral bands, rim to centre
			int arms = 3, steps = 34;
			for (int arm = 0; arm < arms; arm++) {
				double a0 = s + arm * Math.PI * 2 / arms;
				for (int i = 0; i < steps; i++) {
					double t0 = i / (double) steps, t1 = (i + 1) / (double) steps;
					double r0 = radius * (1 - t0) + 0.35, r1 = radius * (1 - t1) + 0.35;
					double ang0 = a0 + t0 * Math.PI * 1.7, ang1 = a0 + t1 * Math.PI * 1.7;
					double w0 = 0.35 + radius * 0.16 * (1 - t0), w1 = 0.35 + radius * 0.16 * (1 - t1);
					int col = mix(0x7FF0F6, 0x1E4FC0, t0);
					float a = alpha * (float) (0.55 + 0.4 * t0);
					band(d, c, ang0, r0, w0, ang1, r1, w1, FxDraw.rgba(col, a));
				}
			}
			// the dark centre
			disc(d, c.add(0, 0.01, 0), Math.max(1.2, radius * 0.22), FxDraw.rgba(0x08122E, 0.9f * fade), false);
		}

		private static void band(FxDraw d, Vec3 c, double a0, double r0, double w0, double a1, double r1, double w1, float[] rgba) {
			Vec3 i0 = c.add(Math.cos(a0) * (r0 - w0 / 2), 0, Math.sin(a0) * (r0 - w0 / 2));
			Vec3 o0 = c.add(Math.cos(a0) * (r0 + w0 / 2), 0, Math.sin(a0) * (r0 + w0 / 2));
			Vec3 i1 = c.add(Math.cos(a1) * (r1 - w1 / 2), 0, Math.sin(a1) * (r1 - w1 / 2));
			Vec3 o1 = c.add(Math.cos(a1) * (r1 + w1 / 2), 0, Math.sin(a1) * (r1 + w1 / 2));
			d.quad(FxDraw.GLOW, true, i0, o0, o1, i1, rgba, 0.45f, 0.55f, 0.45f, 0.55f);
			d.quad(FxDraw.GLOW, true, i1, o1, o0, i0, rgba, 0.45f, 0.55f, 0.45f, 0.55f); // seen from below too
		}

		private static void disc(FxDraw d, Vec3 c, double r, float[] rgba, boolean glow) {
			Vec3 a = c.add(-r, 0, -r), b = c.add(r, 0, -r), e = c.add(r, 0, r), f = c.add(-r, 0, r);
			d.quad(FxDraw.GLOW, glow, a, b, e, f, rgba, 0, 1, 0, 1);
			d.quad(FxDraw.GLOW, glow, f, e, b, a, rgba, 0, 1, 0, 1);
		}

		private static int mix(int a, int b, double t) {
			int r = (int) (((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
			int g = (int) (((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
			int bl = (int) ((a & 255) * (1 - t) + (b & 255) * t);
			return (r << 16) | (g << 8) | bl;
		}
	}
}
