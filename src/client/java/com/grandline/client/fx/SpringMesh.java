package com.grandline.client.fx;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;

/**
 * A procedural helical spring (reusable for any elastic limb, launcher or contraption): a round wire swept along a
 * helix from the origin along +Y. Length changes only the pitch between coils (compressed coils close up, stretched
 * coils open out) while the wire keeps its thickness: the geometry is rebuilt from the parameters, never scaled.
 * Sine/cosine tables are cached per level of detail, so a frame only does multiply-adds. Drawn through the vanilla
 * entity pipeline with real normals and world light ({@link MetalMesh#STEEL}), so it is lit like a model and shader
 * packs treat it as ordinary entity geometry.
 */
public final class SpringMesh {
	private record Table(int seg, int sides, float[] cs, float[] sn, float[] ws, float[] wc) {
	}

	private static final Map<Long, Table> TABLES = new HashMap<>();

	private SpringMesh() {
	}

	private static Table table(int seg, int sides) {
		return TABLES.computeIfAbsent((long) seg << 16 | sides, k -> {
			float[] cs = new float[seg + 1], sn = new float[seg + 1], ws = new float[sides + 1], wc = new float[sides + 1];
			for (int i = 0; i <= seg; i++) {
				double a = i * Math.PI * 2 / seg;
				cs[i] = (float) Math.cos(a);
				sn[i] = (float) Math.sin(a);
			}
			for (int j = 0; j <= sides; j++) {
				double a = j * Math.PI * 2 / sides;
				wc[j] = (float) Math.cos(a);
				ws[j] = (float) Math.sin(a);
			}
			return new Table(seg, sides, cs, sn, ws, wc);
		});
	}

	/**
	 * Draws a spring of {@code coils} turns, coil radius {@code radius}, wire radius {@code wire}, {@code length} long
	 * (any length: the pitch follows), starting at angle {@code phase}.
	 */
	public static void draw(PoseStack pose, MultiBufferSource buffers, int light, int coils, float radius, float wire, float length, float phase,
			float alpha) {
		if (length <= 0.001f || alpha <= 0.003f) {
			return;
		}
		double detail = WindMesh.detail();
		int seg = Math.max(6, (int) Math.round(10 * detail));
		int sides = detail >= 1 ? 6 : 4;
		Table t = table(seg, sides);
		VertexConsumer c = buffers.getBuffer(alpha >= 0.999f ? RenderType.entityCutoutNoCull(MetalMesh.STEEL) : RenderType.entityTranslucent(MetalMesh.STEEL));
		PoseStack.Pose last = pose.last();
		Matrix4f m = last.pose();
		Matrix3f n = last.normal();
		float pitch = length / coils;
		float cp = (float) Math.cos(phase), sp = (float) Math.sin(phase);
		int total = coils * seg;
		// Per ring of the tube: centre on the helix, and the frame (radial out, up) for the wire cross-section.
		float[] prev = null;
		for (int k = 0; k <= total; k++) {
			int i = k % seg;
			float ca = t.cs[i] * cp - t.sn[i] * sp, sa = t.sn[i] * cp + t.cs[i] * sp;
			float y = pitch * k / seg;
			float[] ring = new float[(sides + 1) * 6];
			for (int j = 0; j <= sides; j++) {
				// Cross-section in the radial (ca, 0, sa) / axial (0, 1, 0) plane.
				float ox = ca * t.wc[j], oy = t.ws[j], oz = sa * t.wc[j];
				ring[j * 6] = ca * radius + ox * wire;
				ring[j * 6 + 1] = y + oy * wire;
				ring[j * 6 + 2] = sa * radius + oz * wire;
				ring[j * 6 + 3] = ox;
				ring[j * 6 + 4] = oy;
				ring[j * 6 + 5] = oz;
			}
			if (prev != null) {
				for (int j = 0; j < sides; j++) {
					float v0 = j / (float) sides, v1 = (j + 1) / (float) sides;
					vtx(c, m, n, light, alpha, prev, j, 0, v0);
					vtx(c, m, n, light, alpha, ring, j, 1, v0);
					vtx(c, m, n, light, alpha, ring, j + 1, 1, v1);
					vtx(c, m, n, light, alpha, prev, j + 1, 0, v1);
				}
			}
			prev = ring;
		}
	}

	private static void vtx(VertexConsumer c, Matrix4f m, Matrix3f n, int light, float alpha, float[] ring, int j, float u, float v) {
		int o = j * 6;
		// Outward-facing parts of the wire are brighter (the edge highlight row of the steel texture).
		float shade = 0.75f + 0.25f * Math.max(0, ring[o + 4]);
		c.vertex(m, ring[o], ring[o + 1], ring[o + 2]).color(shade, shade, Math.min(1f, shade * 1.04f), alpha).uv(u, 0.1f + v * 0.8f)
				.overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(n, ring[o + 3], ring[o + 4], ring[o + 5]).endVertex();
	}
}
