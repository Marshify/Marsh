package com.grandline.client.render.aura;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.Entity;

/**
 * The held weapon under Aura: drawn through a buffer wrapper that darkens every vertex to the coating's charcoal (the
 * weapon keeps its shape and detail, only blackened, with its highlights still faintly showing).
 */
public final class CoatedBuffers implements MultiBufferSource {
	private static final float SHADE = 0.22f;
	private final MultiBufferSource delegate;

	private CoatedBuffers(MultiBufferSource delegate) {
		this.delegate = delegate;
	}

	/** {@code buffers}, darkened if {@code holder} has Aura on. */
	public static MultiBufferSource forHolder(Entity holder, MultiBufferSource buffers) {
		if (holder == null || com.grandline.client.technique.ClientTechniques.auraStageOf(holder) < 0 || buffers instanceof CoatedBuffers) {
			return buffers;
		}
		return new CoatedBuffers(buffers);
	}

	@Override
	public VertexConsumer getBuffer(RenderType type) {
		return new Dark(delegate.getBuffer(type));
	}

	private record Dark(VertexConsumer v) implements VertexConsumer {
		@Override
		public VertexConsumer vertex(double x, double y, double z) {
			v.vertex(x, y, z);
			return this;
		}

		@Override
		public VertexConsumer color(int r, int g, int b, int a) {
			v.color((int) (r * SHADE + 6), (int) (g * SHADE + 7), (int) (b * SHADE + 10), a);
			return this;
		}

		@Override
		public VertexConsumer uv(float u, float vv) {
			v.uv(u, vv);
			return this;
		}

		@Override
		public VertexConsumer overlayCoords(int u, int vv) {
			v.overlayCoords(u, vv);
			return this;
		}

		@Override
		public VertexConsumer uv2(int u, int vv) {
			v.uv2(u, vv);
			return this;
		}

		@Override
		public VertexConsumer normal(float x, float y, float z) {
			v.normal(x, y, z);
			return this;
		}

		@Override
		public void endVertex() {
			v.endVertex();
		}

		@Override
		public void vertex(float x, float y, float z, float r, float g, float b, float a, float u, float vv, int overlay, int light, float nx, float ny, float nz) {
			v.vertex(x, y, z, r * SHADE + 0.025f, g * SHADE + 0.03f, b * SHADE + 0.04f, a, u, vv, overlay, light, nx, ny, nz);
		}

		@Override
		public void defaultColor(int r, int g, int b, int a) {
			v.defaultColor(r, g, b, a);
		}

		@Override
		public void unsetDefaultColor() {
			v.unsetDefaultColor();
		}
	}
}
