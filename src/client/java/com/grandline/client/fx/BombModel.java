package com.grandline.client.fx;

import com.grandline.GrandLineMod;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Procedural Bomb models in the entity pipeline (lit, textured from {@code textures/entity/bomb.png}, so shader packs
 * treat them like any entity): the spherical bomb (dark riveted casing, brass collar, curved twisted fuse with a
 * burning tip of crossed additive sparks, optional emissive heat shell) and the low-profile land mine (casing, tread
 * pressure plate, small arming light). Local frame: origin at the bomb's centre (mine: on the ground), +Y up.
 * Trigonometry for the sphere is cached.
 */
public final class BombModel {
	public static final ResourceLocation TEX = GrandLineMod.id("textures/entity/bomb.png");
	private static final int LON = 14, LAT = 9;
	private static final float[][] SIN = new float[2][], COS = new float[2][];

	static {
		SIN[0] = new float[LAT + 1];
		COS[0] = new float[LAT + 1];
		for (int i = 0; i <= LAT; i++) {
			SIN[0][i] = (float) Math.sin(Math.PI * i / LAT);
			COS[0][i] = (float) Math.cos(Math.PI * i / LAT);
		}
		SIN[1] = new float[LON + 1];
		COS[1] = new float[LON + 1];
		for (int j = 0; j <= LON; j++) {
			SIN[1][j] = (float) Math.sin(Math.PI * 2 * j / LON);
			COS[1][j] = (float) Math.cos(Math.PI * 2 * j / LON);
		}
	}

	private BombModel() {
	}

	/**
	 * A bomb of {@code radius}; {@code fuse} 0..1 how much fuse is left; {@code heat} 0..1 orange glow (Self Destruct);
	 * {@code burn} flicker phase (ticks) for the spark.
	 */
	public static void bomb(PoseStack pose, MultiBufferSource buffers, int light, float radius, float fuse, float heat, float burn) {
		PoseStack.Pose last = pose.last();
		Matrix4f m = last.pose();
		Matrix3f n = last.normal();
		VertexConsumer c = buffers.getBuffer(RenderType.entityCutoutNoCull(TEX));
		float tr = 1, tg = 1 - 0.35f * heat, tb = 1 - 0.6f * heat;
		sphere(c, m, n, light, radius, tr, tg, tb, 1);
		// Collar: a short brass cylinder on top.
		float cr = radius * 0.3f, y0 = radius * 0.9f, y1 = radius * 1.12f;
		for (int j = 0; j < 8; j++) {
			double a0 = j * Math.PI / 4, a1 = (j + 1) * Math.PI / 4;
			float x0 = (float) Math.cos(a0) * cr, z0 = (float) Math.sin(a0) * cr, x1 = (float) Math.cos(a1) * cr, z1 = (float) Math.sin(a1) * cr;
			float nx = (float) Math.cos((a0 + a1) / 2), nz = (float) Math.sin((a0 + a1) / 2);
			quad(c, m, n, light, x0, y0, z0, x1, y0, z1, x1, y1, z1, x0, y1, z0, 40 / 64f + j / 64f, 41 / 64f + j / 64f, 0, 0.25f, nx, 0, nz, tr, tg, tb, 1);
			quad(c, m, n, light, 0, y1, 0, x0, y1, z0, x1, y1, z1, 0, y1, 0, 40 / 64f, 48 / 64f, 0.3f, 0.35f, 0, 1, 0, tr, tg, tb, 1);
		}
		// Fuse: a curved twisted cord (4 segments) whose length is what is left to burn.
		float len = radius * (0.25f + 0.55f * fuse);
		float w = radius * 0.07f;
		float px = 0, py = y1, pz = 0;
		for (int s = 0; s < 4; s++) {
			float t1 = (s + 1) / 4f;
			float qx = (float) (Math.sin(t1 * 1.2) * len * 0.45), qy = y1 + t1 * len * 0.85f, qz = 0;
			float v0 = s / 4f, v1 = (s + 1) / 4f;
			quad(c, m, n, light, px - w, py, pz, px + w, py, pz, qx + w, qy, qz, qx - w, qy, qz, 32 / 64f, 40 / 64f, v0, v1, 0, 0, 1, tr, tg, tb, 1);
			quad(c, m, n, light, px, py, pz - w, px, py, pz + w, qx, qy, qz + w, qx, qy, qz - w, 32 / 64f, 40 / 64f, v0, v1, 1, 0, 0, tr, tg, tb, 1);
			px = qx;
			py = qy;
			pz = qz;
		}
		// Burning tip: crossed additive spark quads that flicker.
		if (fuse > 0.001f) {
			float f = 0.75f + 0.25f * (float) Math.sin(burn * 2.7) + 0.15f * (float) Math.sin(burn * 7.1);
			float s = radius * (0.22f + 0.1f * fuse) * f;
			VertexConsumer g = buffers.getBuffer(FxRenderTypes.glow(FxDraw.SPARK));
			for (int k = 0; k < 3; k++) {
				double a = k * Math.PI / 3 + burn * 0.4;
				float ax = (float) Math.cos(a) * s, az = (float) Math.sin(a) * s;
				glowQuad(g, m, px - ax, py - s, pz - az, px + ax, py - s, pz + az, px + ax, py + s, pz + az, px - ax, py + s, pz - az, 1f, 0.8f, 0.35f, 0.95f);
			}
			float h = s * 0.5f;
			glowQuad(g, m, px - h, py, pz - h, px + h, py, pz - h, px + h, py, pz + h, px - h, py, pz + h, 1f, 0.96f, 0.74f, 1f);
		}
		// Heat: an emissive orange shell over the casing that grows with the charge.
		if (heat > 0.02f) {
			VertexConsumer e = buffers.getBuffer(RenderType.entityTranslucentEmissive(TEX));
			sphere(e, m, n, 0xF000F0, radius * 1.015f, 1f, 0.45f + 0.2f * heat, 0.15f, Math.min(0.8f, heat * 0.7f));
		}
	}

	private static void sphere(VertexConsumer c, Matrix4f m, Matrix3f n, int light, float r, float cr, float cg, float cb, float a) {
		for (int i = 0; i < LAT; i++) {
			for (int j = 0; j < LON; j++) {
				float[] p = new float[12], nn = new float[12];
				int k = 0;
				int[][] idx = {{i, j}, {i + 1, j}, {i + 1, j + 1}, {i, j + 1}};
				for (int[] q : idx) {
					float sx = SIN[0][q[0]] * COS[1][q[1]], sy = COS[0][q[0]], sz = SIN[0][q[0]] * SIN[1][q[1]];
					p[k] = sx * r;
					p[k + 1] = sy * r;
					p[k + 2] = sz * r;
					nn[k] = sx;
					nn[k + 1] = sy;
					nn[k + 2] = sz;
					k += 3;
				}
				float u0 = j / (float) LON * 0.5f, u1 = (j + 1) / (float) LON * 0.5f, v0 = i / (float) LAT, v1 = (i + 1) / (float) LAT;
				float[] us = {u0, u0, u1, u1}, vs = {v0, v1, v1, v0};
				for (int q = 0; q < 4; q++) {
					c.vertex(m, p[q * 3], p[q * 3 + 1], p[q * 3 + 2]).color(cr, cg, cb, a).uv(us[q], vs[q]).overlayCoords(OverlayTexture.NO_OVERLAY)
							.uv2(light).normal(n, nn[q * 3], nn[q * 3 + 1], nn[q * 3 + 2]).endVertex();
				}
			}
		}
	}

	/** A land mine: {@code armed} 0..1 (the plate rises as it arms), {@code lamp} 0..1 brightness of the arming light. */
	public static void mine(PoseStack pose, MultiBufferSource buffers, int light, float armed, float lamp, boolean own) {
		PoseStack.Pose last = pose.last();
		Matrix4f m = last.pose();
		Matrix3f n = last.normal();
		VertexConsumer c = buffers.getBuffer(RenderType.entityCutoutNoCull(TEX));
		float r = 0.3f, h = 0.08f;
		int seg = 12;
		for (int j = 0; j < seg; j++) {
			double a0 = j * Math.PI * 2 / seg, a1 = (j + 1) * Math.PI * 2 / seg;
			float x0 = (float) Math.cos(a0) * r, z0 = (float) Math.sin(a0) * r, x1 = (float) Math.cos(a1) * r, z1 = (float) Math.sin(a1) * r;
			float nx = (float) Math.cos((a0 + a1) / 2), nz = (float) Math.sin((a0 + a1) / 2);
			// Bevelled side band.
			quad(c, m, n, light, x0, 0, z0, x1, 0, z1, x1 * 0.92f, h, z1 * 0.92f, x0 * 0.92f, h, z0 * 0.92f,
					48 / 64f + j / 12f * 16 / 64f, 48 / 64f + (j + 1) / 12f * 16 / 64f, 0.5f, 1f, nx, 0.3f, nz, 1, 1, 1, 1);
			// Top ring (casing) and plate (raised while arming).
			float pr = 0.18f, ph = h + 0.012f + 0.018f * armed;
			float tu = 56 / 64f, tv = 0.25f, su = 8 / 64f, sv = 0.25f;
			quad(c, m, n, light, x0 * 0.92f, h, z0 * 0.92f, x1 * 0.92f, h, z1 * 0.92f, x1 * pr / r, h, z1 * pr / r, x0 * pr / r, h, z0 * pr / r,
					tu + (float) Math.cos(a0) * su * 0.9f, tu + (float) Math.cos(a1) * su * 0.9f, tv + (float) Math.sin(a0) * sv * 0.9f,
					tv + (float) Math.sin(a1) * sv * 0.9f, 0, 1, 0, 1, 1, 1, 1);
			quad(c, m, n, light, x0 * pr / r, ph, z0 * pr / r, x1 * pr / r, ph, z1 * pr / r, 0, ph, 0, 0, ph, 0,
					tu + (float) Math.cos(a0) * su * 0.6f, tu + (float) Math.cos(a1) * su * 0.6f, tv + (float) Math.sin(a0) * sv * 0.6f,
					tv + (float) Math.sin(a1) * sv * 0.6f, 0, 1, 0, 0.95f, 0.95f, 0.95f, 1);
			quad(c, m, n, light, x0 * pr / r, h, z0 * pr / r, x1 * pr / r, h, z1 * pr / r, x1 * pr / r, ph, z1 * pr / r, x0 * pr / r, ph, z0 * pr / r,
					50 / 64f, 52 / 64f, 0.6f, 0.65f, nx, 0, nz, 0.8f, 0.8f, 0.8f, 1);
		}
		// Arming light: a tiny red lamp on the rim (brighter for the owner, subtle for everyone else).
		if (lamp > 0.01f) {
			VertexConsumer g = buffers.getBuffer(FxRenderTypes.glow(FxDraw.GLOW));
			float s = own ? 0.07f : 0.045f, a = lamp * (own ? 0.95f : 0.45f);
			float lx = 0.22f, ly = h + 0.03f;
			glowQuad(g, m, lx - s, ly, -s, lx + s, ly, -s, lx + s, ly, s, lx - s, ly, s, 1f, 0.25f, 0.15f, a);
			glowQuad(g, m, lx - s, ly - s, 0, lx + s, ly - s, 0, lx + s, ly + s, 0, lx - s, ly + s, 0, 1f, 0.25f, 0.15f, a);
		}
	}

	static void quad(VertexConsumer c, Matrix4f m, Matrix3f n, int light, float x0, float y0, float z0, float x1, float y1, float z1, float x2, float y2,
			float z2, float x3, float y3, float z3, float u0, float u1, float v0, float v1, float nx, float ny, float nz, float r, float g, float b, float a) {
		c.vertex(m, x0, y0, z0).color(r, g, b, a).uv(u0, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(n, nx, ny, nz).endVertex();
		c.vertex(m, x1, y1, z1).color(r, g, b, a).uv(u1, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(n, nx, ny, nz).endVertex();
		c.vertex(m, x2, y2, z2).color(r, g, b, a).uv(u1, v0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(n, nx, ny, nz).endVertex();
		c.vertex(m, x3, y3, z3).color(r, g, b, a).uv(u0, v0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(n, nx, ny, nz).endVertex();
	}

	static void glowQuad(VertexConsumer g, Matrix4f m, float x0, float y0, float z0, float x1, float y1, float z1, float x2, float y2, float z2,
			float x3, float y3, float z3, float r, float gr, float b, float a) {
		g.vertex(m, x0, y0, z0).color(r, gr, b, a).uv(0, 1).endVertex();
		g.vertex(m, x1, y1, z1).color(r, gr, b, a).uv(1, 1).endVertex();
		g.vertex(m, x2, y2, z2).color(r, gr, b, a).uv(1, 0).endVertex();
		g.vertex(m, x3, y3, z3).color(r, gr, b, a).uv(0, 0).endVertex();
	}
}
