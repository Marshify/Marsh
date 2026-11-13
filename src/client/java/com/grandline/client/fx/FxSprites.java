package com.grandline.client.fx;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Grand Line's own secondary particles (sparks, embers, smoke), drawn with the effect textures instead of vanilla
 * particles. Counts scale with the effect-density setting; the core shapes of a move never depend on these.
 */
public final class FxSprites {
	private static final class Sprite {
		ResourceLocation tex;
		boolean glow;
		Vec3 pos, prev, vel;
		double size, growth, gravity, drag;
		int age, life, frame;
		int colorFrom, colorTo;
		float alpha;
		double roll, spin;
	}

	private static final List<Sprite> SPRITES = new ArrayList<>();
	private static final int MAX = 1800;

	private FxSprites() {
	}

	/** How many of {@code base} sprites to spawn at the current density setting (at least one when base > 0). */
	public static int count(int base) {
		double f = switch (com.grandline.client.settings.ClientSettings.get().fxDensity) {
			case 0 -> 0.3;
			case 1 -> 0.65;
			default -> 1.0;
		};
		return base <= 0 ? 0 : Math.max(1, (int) Math.round(base * f));
	}

	/** Spark: a hot little star that streaks and shrinks. */
	public static void spark(Vec3 pos, Vec3 vel, double size, int life, int colorFrom, int colorTo) {
		add(FxDraw.SPARK, true, pos, vel, size, -size / life * 0.6, 0.03, 0.9, life, colorFrom, colorTo, 1f);
	}

	/** Ember: a small flame tongue that rises and fades. */
	public static void ember(Vec3 pos, Vec3 vel, double size, int life, int colorFrom, int colorTo) {
		Sprite s = add(FxDraw.FLAME, true, pos, vel, size, -size / life * 0.5, -0.004, 0.93, life, colorFrom, colorTo, 0.95f);
		if (s != null) {
			s.frame = (int) (Math.random() * 4);
		}
	}

	/** Smoke: a soft dark puff that swells and drifts up. */
	public static void smoke(Vec3 pos, Vec3 vel, double size, int life, int color) {
		add(FxDraw.GLOW, false, pos, vel, size, size / life * 1.2, -0.006, 0.9, life, color, color, 0.45f);
	}

	private static Sprite add(ResourceLocation tex, boolean glow, Vec3 pos, Vec3 vel, double size, double growth, double gravity, double drag,
			int life, int colorFrom, int colorTo, float alpha) {
		if (SPRITES.size() >= MAX) {
			return null;
		}
		Sprite s = new Sprite();
		s.tex = tex;
		s.glow = glow;
		s.pos = pos;
		s.prev = pos;
		s.vel = vel;
		s.size = size;
		s.growth = growth;
		s.gravity = gravity;
		s.drag = drag;
		s.life = Math.max(1, life);
		s.colorFrom = colorFrom;
		s.colorTo = colorTo;
		s.alpha = alpha;
		s.roll = Math.random() * Math.PI * 2;
		s.spin = (Math.random() - 0.5) * 0.3;
		SPRITES.add(s);
		return s;
	}

	static void tick() {
		for (Iterator<Sprite> it = SPRITES.iterator(); it.hasNext(); ) {
			Sprite s = it.next();
			s.age++;
			if (s.age >= s.life || s.size <= 0.005) {
				it.remove();
				continue;
			}
			s.prev = s.pos;
			s.pos = s.pos.add(s.vel);
			s.vel = s.vel.scale(s.drag).add(0, -s.gravity, 0);
			s.size += s.growth;
			s.roll += s.spin;
		}
	}

	static void render(FxDraw d) {
		for (Sprite s : SPRITES) {
			float t = Math.min(1f, (s.age + d.partial) / s.life);
			int color = lerpColor(s.colorFrom, s.colorTo, t);
			float a = s.alpha * (t < 0.15f ? t / 0.15f : 1f) * (1 - t * t);
			Vec3 p = s.prev.lerp(s.pos, d.partial);
			float[] rgba = FxDraw.rgba(color, a);
			if (s.tex == FxDraw.FLAME) {
				d.flame(p, s.size, s.roll * 0.3, s.frame, rgba);
			} else {
				d.billboard(s.tex, s.glow, p, s.size, s.roll, rgba);
			}
		}
	}

	static boolean any() {
		return !SPRITES.isEmpty();
	}

	static void clear() {
		SPRITES.clear();
	}

	static int lerpColor(int a, int b, float t) {
		int r = (int) Mth.lerp(t, (a >> 16) & 255, (b >> 16) & 255);
		int g = (int) Mth.lerp(t, (a >> 8) & 255, (b >> 8) & 255);
		int bl = (int) Mth.lerp(t, a & 255, b & 255);
		return (r << 16) | (g << 8) | bl;
	}

	static Vec3 randomDir(RandomSource random) {
		return new Vec3(random.nextGaussian(), random.nextGaussian(), random.nextGaussian()).normalize();
	}
}
