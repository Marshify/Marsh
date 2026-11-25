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
import org.joml.Vector3f;

/**
 * Procedural tapered spike geometry (reusable: ground spikes, limb spikes, transformation spikes, ice or crystal
 * shards). A spike is a faceted pyramid with flat-shaded facets (each facet its own normal, so lighting gives crisp
 * edges), a highlighted leading edge per facet from the texture and a lighter tip; base at the origin, tip at +Y.
 * Drawn in the entity pipeline (lit, opaque, shader-pack friendly). Facet directions are cached per facet count.
 * The spiked ball is a faceted core with spikes on a fixed Fibonacci lattice, so it looks the same from every angle.
 */
public final class SpikeMesh {
	public static final ResourceLocation TEX = GrandLineMod.id("textures/entity/spike.png");
	private static final float[][] COS = new float[17][], SIN = new float[17][];
	/** Fixed spike directions over the ball (Fibonacci sphere). */
	static final Vector3f[] BALL_DIRS = new Vector3f[34];

	static {
		for (int n = 3; n <= 16; n++) {
			COS[n] = new float[n + 1];
			SIN[n] = new float[n + 1];
			for (int i = 0; i <= n; i++) {
				COS[n][i] = (float) Math.cos(Math.PI * 2 * i / n);
				SIN[n][i] = (float) Math.sin(Math.PI * 2 * i / n);
			}
		}
		double golden = Math.PI * (3 - Math.sqrt(5));
		for (int i = 0; i < BALL_DIRS.length; i++) {
			double y = 1 - 2 * (i + 0.5) / BALL_DIRS.length;
			double r = Math.sqrt(1 - y * y);
			double a = i * golden;
			BALL_DIRS[i] = new Vector3f((float) (Math.cos(a) * r), (float) y, (float) (Math.sin(a) * r));
		}
	}

	private SpikeMesh() {
	}

	/** Facets for the current VFX preset (Low 4, Medium 5, High 6, Ultra 8). */
	public static int facets() {
		return switch (com.grandline.client.settings.ClientSettings.get().fxDensity) {
			case 0 -> 4;
			case 1 -> 5;
			case 2 -> 6;
			default -> 8;
		};
	}

	/** A spike of {@code length} along +Y with base {@code radius}; {@code twist} rotates the facets (variety). */
	public static void spike(PoseStack pose, MultiBufferSource buffers, int light, float length, float radius, int facets, float twist) {
		if (length <= 0.01f || radius <= 0.005f) {
			return;
		}
		int n = Math.max(3, Math.min(16, facets));
		PoseStack.Pose last = pose.last();
		Matrix4f m = last.pose();
		Matrix3f nm = last.normal();
		VertexConsumer c = buffers.getBuffer(RenderType.entityCutoutNoCull(TEX));
		float ct = (float) Math.cos(twist), st = (float) Math.sin(twist);
		for (int i = 0; i < n; i++) {
			float c0 = COS[n][i] * ct - SIN[n][i] * st, s0 = SIN[n][i] * ct + COS[n][i] * st;
			float c1 = COS[n][i + 1] * ct - SIN[n][i + 1] * st, s1 = SIN[n][i + 1] * ct + COS[n][i + 1] * st;
			float x0 = c0 * radius, z0 = s0 * radius, x1 = c1 * radius, z1 = s1 * radius;
			// Flat facet normal: outward and tilted up by the taper.
			float mx = (c0 + c1) / 2, mz = (s0 + s1) / 2;
			float ny = radius / length;
			float inv = (float) (1 / Math.sqrt(mx * mx + mz * mz + ny * ny));
			float fx = mx * inv, fy = ny * inv, fz = mz * inv;
			vertex(c, m, nm, x0, 0, z0, 0, 1, fx, fy, fz, light, 0.85f);
			vertex(c, m, nm, x1, 0, z1, 0.5f, 1, fx, fy, fz, light, 0.85f);
			vertex(c, m, nm, 0, length, 0, 0.5f, 0, fx, fy, fz, light, 1f);
			vertex(c, m, nm, 0, length, 0, 0, 0, fx, fy, fz, light, 1f);
			// Base cap (seen when a spike leans or is attached to a limb).
			vertex(c, m, nm, 0, 0, 0, 0.75f, 0.5f, 0, -1, 0, light, 0.7f);
			vertex(c, m, nm, x1, 0, z1, 0.8f, 0.5f, 0, -1, 0, light, 0.7f);
			vertex(c, m, nm, x0, 0, z0, 0.8f, 0.6f, 0, -1, 0, light, 0.7f);
			vertex(c, m, nm, 0, 0, 0, 0.75f, 0.6f, 0, -1, 0, light, 0.7f);
		}
	}

	/** The spiked ball: faceted core of {@code radius}, spikes reaching {@code spikeLen} beyond it (scaled by {@code grow}). */
	public static void ball(PoseStack pose, MultiBufferSource buffers, int light, float radius, float spikeLen, float grow) {
		PoseStack.Pose last = pose.last();
		Matrix4f m = last.pose();
		Matrix3f nm = last.normal();
		VertexConsumer c = buffers.getBuffer(RenderType.entityCutoutNoCull(TEX));
		int lat = 6, lon = 10;
		for (int i = 0; i < lat; i++) {
			for (int j = 0; j < lon; j++) {
				float[][] q = new float[4][3];
				int[][] idx = {{i, j}, {i + 1, j}, {i + 1, j + 1}, {i, j + 1}};
				float cx = 0, cy = 0, cz = 0;
				for (int k = 0; k < 4; k++) {
					double th = Math.PI * idx[k][0] / lat, ph = Math.PI * 2 * idx[k][1] / lon;
					q[k][0] = (float) (Math.sin(th) * Math.cos(ph)) * radius;
					q[k][1] = (float) Math.cos(th) * radius;
					q[k][2] = (float) (Math.sin(th) * Math.sin(ph)) * radius;
					cx += q[k][0];
					cy += q[k][1];
					cz += q[k][2];
				}
				float inv = (float) (1 / Math.sqrt(cx * cx + cy * cy + cz * cz + 1e-6));
				float u0 = 0.5f + j / (float) lon * 0.5f, u1 = 0.5f + (j + 1) / (float) lon * 0.5f, v0 = i / (float) lat, v1 = (i + 1) / (float) lat;
				float[] us = {u0, u0, u1, u1}, vs = {v0, v1, v1, v0};
				for (int k = 0; k < 4; k++) {
					vertex(c, m, nm, q[k][0], q[k][1], q[k][2], us[k], vs[k], cx * inv, cy * inv, cz * inv, light, 0.9f);
				}
			}
		}
		int facets = Math.max(4, facets() - 1);
		for (int i = 0; i < BALL_DIRS.length; i++) {
			Vector3f d = BALL_DIRS[i];
			pose.pushPose();
			// Rotate +Y onto the spike direction.
			org.joml.Quaternionf rot = new org.joml.Quaternionf().rotationTo(0, 1, 0, d.x, d.y, d.z);
			pose.mulPose(rot);
			pose.translate(0, radius * 0.85f, 0);
			float len = spikeLen * grow * (0.85f + 0.3f * ((i * 37) % 10) / 10f);
			spike(pose, buffers, light, len, radius * 0.24f, facets, i * 0.7f);
			pose.popPose();
		}
	}

	private static void vertex(VertexConsumer c, Matrix4f m, Matrix3f nm, float x, float y, float z, float u, float v, float nx, float ny, float nz,
			int light, float shade) {
		c.vertex(m, x, y, z).color(shade, shade, shade, 1f).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(nm, nx, ny, nz).endVertex();
	}
}
