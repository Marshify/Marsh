package com.grandline.client.fx;

import com.grandline.client.settings.ClientSettings;
import net.minecraft.world.phys.Vec3;

/**
 * Procedural mesh builders for air, wind and pressure effects, drawn through {@link WindRender}. Everything is a
 * parametric surface patch (s across, t along; s maps to texture u, t to v where t = 0 is the bright leading edge), so
 * one tessellator serves crescent blades, vortex bands, rotor discs and shockwaves. Reusable by any fruit, sword or boss.
 *
 * <p>Detail follows the effect quality preset ({@link #detail}): the shapes keep their outline at every level, only
 * their segment counts change (a simple level-of-detail).
 */
public final class WindMesh {
	/** A point on a surface patch. */
	@FunctionalInterface
	public interface Patch {
		Vec3 at(double s, double t);
	}

	/** Opacity over the patch (0..1). */
	@FunctionalInterface
	public interface Fade {
		float at(double s, double t);
	}

	private WindMesh() {
	}

	/** Segment multiplier for the quality preset: Low 0.5, Medium 0.75, High 1, Ultra 1.4. */
	public static double detail() {
		return switch (ClientSettings.get().fxDensity) {
			case 0 -> 0.5;
			case 1 -> 0.75;
			case 2 -> 1.0;
			default -> 1.4;
		};
	}

	static int segs(int base) {
		return Math.max(2, (int) Math.round(base * detail()));
	}

	/**
	 * Tessellates a patch into {@code nu × nv} quads with per-vertex fresnel (from the patch's own normals) and alpha.
	 * {@code uScroll} slides the texture along s (animated UV scrolling).
	 */
	public static void surface(FxDraw d, boolean glow, Patch patch, int nu, int nv, int rgb, Fade fade, float dissolve, double uScroll) {
		Vec3[] p = new Vec3[4];
		float[] u = new float[4], v = new float[4], a = new float[4], f = new float[4];
		double eps = 1e-3;
		for (int i = 0; i < nu; i++) {
			for (int j = 0; j < nv; j++) {
				double[][] corners = {{i, j}, {i + 1, j}, {i + 1, j + 1}, {i, j + 1}};
				boolean visible = false;
				for (int k = 0; k < 4; k++) {
					double s = corners[k][0] / nu, t = corners[k][1] / nv;
					p[k] = patch.at(s, t);
					Vec3 ds = patch.at(Math.min(1, s + eps), t).subtract(patch.at(Math.max(0, s - eps), t));
					Vec3 dt = patch.at(s, Math.min(1, t + eps)).subtract(patch.at(s, Math.max(0, t - eps)));
					Vec3 n = ds.cross(dt);
					f[k] = n.lengthSqr() < 1e-12 ? 0.5f : WindRender.fresnel(d, p[k], n);
					u[k] = (float) (s + uScroll);
					v[k] = (float) t;
					a[k] = fade.at(s, t);
					visible |= a[k] > 0.003f;
				}
				if (visible) {
					WindRender.quad(d, glow, p, u, v, rgb, a, dissolve, f);
				}
			}
		}
	}

	/**
	 * A crescent wind blade: an arc of {@code arcDegrees} bulging along {@code forward} in the plane of
	 * {@code forward} and {@code side}, {@code width} thick at its middle, tapering to sharp tips, with its leading
	 * (bright) edge on the outside and a slight bow along {@code up} so it reads as curved in 3D.
	 */
	public static void crescent(FxDraw d, boolean glow, Vec3 centre, Vec3 forward, Vec3 side, Vec3 up, double radius, double width,
			double arcDegrees, int rgb, float alpha, float dissolve, double uScroll) {
		double arc = Math.toRadians(arcDegrees);
		Patch patch = (s, t) -> {
			double a = (s - 0.5) * arc;
			double taper = Math.pow(Math.sin(Math.PI * s), 0.7);
			double r = radius - t * width * taper;
			double bow = (s - 0.5) * (s - 0.5) * radius * 0.35;
			return centre.add(forward.scale(Math.cos(a) * r)).add(side.scale(Math.sin(a) * r)).add(up.scale(bow));
		};
		surface(d, glow, patch, segs(14), segs(3), rgb, (s, t) -> alpha * (float) (1 - t * 0.6), dissolve, uScroll);
	}

	/**
	 * One band of a vortex: a ribbon spiralling round {@code axis} from height {@code h0} to {@code h1} (blocks above
	 * {@code base}), covering {@code spanDegrees} of turn starting at {@code angle}. The radius follows
	 * {@code rBottom → rTop} with height (a funnel when rTop > rBottom). Its fade is softest at the ends.
	 */
	public static void vortexBand(FxDraw d, boolean glow, Vec3 base, double h0, double h1, double rBottom, double rTop, double height,
			double angle, double spanDegrees, double climbTurns, int rgb, float alpha, float dissolve, double uScroll) {
		double span = Math.toRadians(spanDegrees);
		Patch patch = (s, t) -> {
			double h = h0 + (h1 - h0) * (1 - t) + s * (h1 - h0) * climbTurns;
			double hh = Math.max(0, Math.min(height, h)) / height;
			double r = rBottom + (rTop - rBottom) * Math.pow(hh, 0.8);
			double a = angle + s * span;
			return base.add(Math.cos(a) * r, h, Math.sin(a) * r);
		};
		surface(d, glow, patch, segs(12), segs(2), rgb, (s, t) -> alpha * (float) Math.sin(Math.PI * s), dissolve, uScroll);
	}

	/** A flat ring in the plane of {@code a} and {@code b} round {@code centre}: shockwaves, rotor discs, ground currents. */
	public static void ring(FxDraw d, boolean glow, Vec3 centre, Vec3 a, Vec3 b, double inner, double outer, double startAngle,
			double spanDegrees, int rgb, float alpha, float dissolve, double uScroll) {
		double span = Math.toRadians(spanDegrees);
		Patch patch = (s, t) -> {
			double ang = startAngle + s * span;
			double r = outer - t * (outer - inner);
			return centre.add(a.scale(Math.cos(ang) * r)).add(b.scale(Math.sin(ang) * r));
		};
		surface(d, glow, patch, segs(spanDegrees >= 300 ? 28 : 12), 1, rgb,
				(s, t) -> alpha * (float) (spanDegrees >= 300 ? 1 : Math.sin(Math.PI * s)), dissolve, uScroll);
	}

	/** A pressure wall: a short upright ring that expands (shockwaves seen from the side). */
	public static void wall(FxDraw d, boolean glow, Vec3 centre, double radius, double height, int rgb, float alpha, float dissolve, double uScroll) {
		Patch patch = (s, t) -> {
			double ang = s * Math.PI * 2;
			return centre.add(Math.cos(ang) * radius, (1 - t) * height, Math.sin(ang) * radius);
		};
		surface(d, glow, patch, segs(28), 1, rgb, (s, t) -> alpha * (float) (1 - t * 0.7), dissolve, uScroll);
	}

	/** Two unit vectors perpendicular to {@code n} (and each other). */
	public static Vec3[] basis(Vec3 n) {
		Vec3 f = n.normalize();
		Vec3 helper = Math.abs(f.y) > 0.95 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
		Vec3 a = f.cross(helper).normalize();
		return new Vec3[]{a, f.cross(a).normalize()};
	}

	/** Smoothstep easing. */
	public static double ease(double t) {
		t = Math.max(0, Math.min(1, t));
		return t * t * (3 - 2 * t);
	}
}
