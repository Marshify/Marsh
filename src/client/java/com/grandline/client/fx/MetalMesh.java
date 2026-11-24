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
 * Solid steel geometry built from quads (reusable for limb blades, rotors, thrown blades, armour plates). Drawn
 * through Minecraft's own entity render types with real normals and world light, so it is lit like any model, casts
 * no glow, and shader packs (Iris/BSL) treat it as ordinary entity geometry: the most compatible path for solid
 * metal. Metallic look comes from the pixel texture (bright edge rows, brushed steel, dark spine) and per-face
 * shading; translucent copies serve as motion-blur ghosts.
 *
 * <p>Local frame of {@link #blade}: the blade runs along +Y from its root at the origin, its flat faces look along
 * ±Z, and its sharpened edge is on the -X side (the spine on +X).
 */
public final class MetalMesh {
	public static final ResourceLocation STEEL = GrandLineMod.id("textures/entity/blade_steel.png");

	private MetalMesh() {
	}

	/**
	 * A sharpened blade: {@code length} long, {@code width} wide at the root, {@code thickness} at the spine, tapering
	 * to a point. The cross-section is a wedge (spine to edge), so the edge catches light like real steel.
	 */
	public static void blade(PoseStack pose, MultiBufferSource buffers, int light, float length, float width, float thickness, float alpha) {
		if (length <= 0.001f || alpha <= 0.003f) {
			return;
		}
		VertexConsumer c = buffers.getBuffer(alpha >= 0.999f ? RenderType.entityCutoutNoCull(STEEL) : RenderType.entityTranslucent(STEEL));
		PoseStack.Pose last = pose.last();
		Matrix4f m = last.pose();
		Matrix3f n = last.normal();
		float t = thickness / 2;
		float tipLen = Math.min(length * 0.35f, width * 1.6f);
		float body = length - tipLen;
		// Points: spine (x = +w/2) and edge (x = -w/2) along y; the edge tapers to the tip at (−w/2·0.2, length).
		float sx = width / 2, ex = -width / 2;
		Vector3f s0 = new Vector3f(sx, 0, 0), s1 = new Vector3f(sx * 0.7f, body, 0), tip = new Vector3f(ex * 0.3f, length, 0);
		Vector3f e0 = new Vector3f(ex, 0, 0), e1 = new Vector3f(ex, body, 0);
		for (int side = -1; side <= 1; side += 2) {
			float z = t * side;
			// Each face: spine (thick) to edge (sharp, z = 0): a wedge.
			Vector3f sp0 = new Vector3f(s0.x, s0.y, z), sp1 = new Vector3f(s1.x, s1.y, z * 0.8f);
			float nz = side;
			// Main face (bright near the edge: v = 0 rows are the edge highlight).
			quad(c, m, n, light, alpha, sp0, sp1, e1, e0, 0.3f, 0.12f * side, nz, 1f, 0.15f);
			// Tip face.
			quad(c, m, n, light, alpha, sp1, new Vector3f(tip.x, tip.y, 0), new Vector3f(tip.x, tip.y, 0), e1, 0.4f, 0.2f, nz, 0.95f, 0.1f);
		}
		// Spine (flat back) and root cap.
		quad(c, m, n, light, alpha, new Vector3f(s0.x, 0, -t), new Vector3f(s1.x, body, -t * 0.8f), new Vector3f(s1.x, body, t * 0.8f),
				new Vector3f(s0.x, 0, t), 1, 0, 0, 0.6f, 0.9f);
		quad(c, m, n, light, alpha, new Vector3f(s1.x, body, -t * 0.8f), new Vector3f(tip.x, tip.y, 0), new Vector3f(tip.x, tip.y, 0),
				new Vector3f(s1.x, body, t * 0.8f), 0.7f, 0.7f, 0, 0.55f, 0.9f);
		quad(c, m, n, light, alpha, new Vector3f(s0.x, 0, -t), new Vector3f(s0.x, 0, t), new Vector3f(e0.x, 0, 0), new Vector3f(e0.x, 0, 0),
				0, -1, 0, 0.5f, 0.95f);
	}

	/** One textured quad with a face normal; {@code shade} dims it (fake ambient occlusion), {@code v} picks the texture row band. */
	private static void quad(VertexConsumer c, Matrix4f m, Matrix3f n, int light, float alpha, Vector3f a, Vector3f b, Vector3f cc, Vector3f d,
			float nx, float ny, float nz, float shade, float v) {
		Vector3f normal = new Vector3f(nx, ny, nz).normalize();
		float[][] uv = {{0, v + 0.8f}, {1, v + 0.8f}, {1, v}, {0, v}};
		Vector3f[] p = {a, b, cc, d};
		for (int i = 0; i < 4; i++) {
			c.vertex(m, p[i].x, p[i].y, p[i].z).color(shade, shade, Math.min(1f, shade * 1.04f), alpha)
					.uv(uv[i][0], Math.min(1f, Math.max(0f, uv[i][1] > 1 ? uv[i][1] - 1 : uv[i][1])))
					.overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(n, normal.x, normal.y, normal.z).endVertex();
		}
	}
}
