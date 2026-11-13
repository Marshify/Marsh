package com.grandline.client.fx;

import com.grandline.GrandLineMod;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.List;

/** One frame's drawing context for move effects: camera-relative quads, camera-facing billboards and ribbons. */
public final class FxDraw {
	public static final ResourceLocation GLOW = GrandLineMod.id("textures/fx/glow.png");
	public static final ResourceLocation FLAME = GrandLineMod.id("textures/fx/flame.png");
	public static final ResourceLocation BAND = GrandLineMod.id("textures/fx/band.png");
	public static final ResourceLocation STREAK = GrandLineMod.id("textures/fx/streak.png");
	public static final ResourceLocation SPARK = GrandLineMod.id("textures/fx/spark.png");

	public final float partial;
	public final Vec3 camPos;
	private final Matrix4f matrix;
	private final MultiBufferSource buffers;
	private final Vec3 right;
	private final Vec3 up;

	FxDraw(PoseStack pose, Camera camera, MultiBufferSource buffers, float partial) {
		this.partial = partial;
		this.camPos = camera.getPosition();
		this.matrix = pose.last().pose();
		this.buffers = buffers;
		Vector3f l = camera.getLeftVector();
		Vector3f u = camera.getUpVector();
		this.right = new Vec3(-l.x(), -l.y(), -l.z());
		this.up = new Vec3(u.x(), u.y(), u.z());
	}

	public Vec3 cameraRight() {
		return right;
	}

	public Vec3 cameraUp() {
		return up;
	}

	private VertexConsumer consumer(ResourceLocation tex, boolean glow) {
		return buffers.getBuffer(glow ? FxRenderTypes.glow(tex) : FxRenderTypes.soft(tex));
	}

	private void v(VertexConsumer c, Vec3 p, float r, float g, float b, float a, float u, float vv) {
		c.vertex(matrix, (float) (p.x - camPos.x), (float) (p.y - camPos.y), (float) (p.z - camPos.z)).color(r, g, b, a).uv(u, vv).endVertex();
	}

	/** A quad through four corners (bottom-left, bottom-right, top-right, top-left) with a UV rectangle. */
	public void quad(ResourceLocation tex, boolean glow, Vec3 bl, Vec3 br, Vec3 tr, Vec3 tl, float[] rgba, float u0, float u1, float v0, float v1) {
		if (rgba[3] <= 0.003f) {
			return;
		}
		VertexConsumer c = consumer(tex, glow);
		v(c, bl, rgba[0], rgba[1], rgba[2], rgba[3], u0, v1);
		v(c, br, rgba[0], rgba[1], rgba[2], rgba[3], u1, v1);
		v(c, tr, rgba[0], rgba[1], rgba[2], rgba[3], u1, v0);
		v(c, tl, rgba[0], rgba[1], rgba[2], rgba[3], u0, v0);
	}

	/** A camera-facing square of half-size {@code half}, rolled by {@code roll} radians (texture top = roll 0 up). */
	public void billboard(ResourceLocation tex, boolean glow, Vec3 p, double half, double roll, float[] rgba, float u0, float u1, float v0, float v1) {
		// Near the eye (first-person hands, a shot just leaving the hand) effects would fill the screen: cap their
		// on-screen size and soften them so the view stays readable.
		double dist = p.distanceTo(camPos);
		if (dist < 3) {
			half = Math.min(half, dist * 0.28);
			rgba = new float[]{rgba[0], rgba[1], rgba[2], rgba[3] * nearFade(dist)};
		}
		double cs = Math.cos(roll), sn = Math.sin(roll);
		Vec3 rx = right.scale(cs).add(up.scale(sn)).scale(half);
		Vec3 uy = up.scale(cs).subtract(right.scale(sn)).scale(half);
		quad(tex, glow, p.subtract(rx).subtract(uy), p.add(rx).subtract(uy), p.add(rx).add(uy), p.subtract(rx).add(uy), rgba, u0, u1, v0, v1);
	}

	public void billboard(ResourceLocation tex, boolean glow, Vec3 p, double half, double roll, float[] rgba) {
		billboard(tex, glow, p, half, roll, rgba, 0, 1, 0, 1);
	}

	/** 0 right at the eye, 1 from three blocks away. */
	static float nearFade(double dist) {
		double t = Math.max(0, Math.min(1, (dist - 0.35) / 2.65));
		return (float) (0.35 + 0.65 * t * t * (3 - 2 * t));
	}

	/** One of the four flame tongues of flame.png. */
	public void flame(Vec3 p, double half, double roll, int frame, float[] rgba) {
		int f = Math.floorMod(frame, 4);
		billboard(FLAME, true, p, half, roll, rgba, f / 4f, (f + 1) / 4f, 0, 1);
	}

	/**
	 * A ribbon through {@code points} (head first), always turned to face the camera; width and colour fade along it.
	 * {@code widths} and {@code alphas} are per point.
	 */
	public void ribbon(ResourceLocation tex, List<Vec3> points, double[] widths, float[] rgb, float[] alphas, float vScroll) {
		int n = points.size();
		for (int i = 0; i + 1 < n; i++) {
			Vec3 a = points.get(i), b = points.get(i + 1);
			Vec3 seg = b.subtract(a);
			if (seg.lengthSqr() < 1e-6) {
				continue;
			}
			Vec3 toCam = camPos.subtract(a);
			Vec3 side = seg.cross(toCam);
			if (side.lengthSqr() < 1e-8) {
				continue;
			}
			side = side.normalize();
			double da = a.distanceTo(camPos), db = b.distanceTo(camPos);
			double wa = da < 3 ? Math.min(widths[i], da * 0.5) : widths[i];
			double wb = db < 3 ? Math.min(widths[i + 1], db * 0.5) : widths[i + 1];
			float aa = alphas[i] * (da < 3 ? nearFade(da) : 1), ab = alphas[i + 1] * (db < 3 ? nearFade(db) : 1);
			Vec3 sa = side.scale(wa / 2), sb = side.scale(wb / 2);
			float va = vScroll + i / (float) (n - 1), vb = vScroll + (i + 1) / (float) (n - 1);
			VertexConsumer c = consumer(tex, true);
			v(c, a.subtract(sa), rgb[0], rgb[1], rgb[2], aa, 0, va);
			v(c, a.add(sa), rgb[0], rgb[1], rgb[2], aa, 1, va);
			v(c, b.add(sb), rgb[0], rgb[1], rgb[2], ab, 1, vb);
			v(c, b.subtract(sb), rgb[0], rgb[1], rgb[2], ab, 0, vb);
		}
	}

	/**
	 * A ring wall: a vertical band around a circle (texture band.png, bright foot), {@code segments} quads, open at
	 * the top. Optionally lifted by {@code baseY} above {@code center}'s y.
	 */
	public void ringWall(Vec3 center, double radius, double height, int segments, float[] rgba, float uScroll) {
		for (int i = 0; i < segments; i++) {
			double a0 = i * Math.PI * 2 / segments, a1 = (i + 1) * Math.PI * 2 / segments;
			Vec3 p0 = center.add(Math.cos(a0) * radius, 0, Math.sin(a0) * radius);
			Vec3 p1 = center.add(Math.cos(a1) * radius, 0, Math.sin(a1) * radius);
			float u0 = uScroll + i * 3f / segments, u1 = uScroll + (i + 1) * 3f / segments;
			quad(BAND, true, p0, p1, p1.add(0, height, 0), p0.add(0, height, 0), rgba, u0, u1, 0, 1);
		}
	}

	/** A flat ring lying on the ground (an annulus of glow), {@code width} wide. */
	public void groundRing(Vec3 center, double radius, double width, int segments, float[] rgba) {
		double r0 = Math.max(0, radius - width / 2), r1 = radius + width / 2;
		for (int i = 0; i < segments; i++) {
			double a0 = i * Math.PI * 2 / segments, a1 = (i + 1) * Math.PI * 2 / segments;
			Vec3 i0 = center.add(Math.cos(a0) * r0, 0, Math.sin(a0) * r0), i1 = center.add(Math.cos(a1) * r0, 0, Math.sin(a1) * r0);
			Vec3 o0 = center.add(Math.cos(a0) * r1, 0, Math.sin(a0) * r1), o1 = center.add(Math.cos(a1) * r1, 0, Math.sin(a1) * r1);
			quad(STREAK, true, i0, i1, o1, o0, rgba, 0, 1, 0, 1);
		}
	}

	/** Colour with alpha (premixed helper). */
	public static float[] rgba(int rgb, float alpha) {
		return new float[]{((rgb >> 16) & 255) / 255f, ((rgb >> 8) & 255) / 255f, (rgb & 255) / 255f, Math.max(0, Math.min(1, alpha))};
	}
}
