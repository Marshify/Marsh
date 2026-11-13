package com.grandline.client.fx;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/**
 * Render types for move effects. {@link #glow}: additive light (fire, sparks, flashes) that brightens what is behind it
 * and keeps its own alpha. {@link #soft}: ordinary translucency for smoke and dark shapes. Both draw both faces, test
 * depth (so walls hide effects behind them) but never write it (layers blend instead of cutting each other).
 */
public final class FxRenderTypes extends RenderType {
	private static final Map<ResourceLocation, RenderType> GLOW = new HashMap<>();
	private static final Map<ResourceLocation, RenderType> SOFT = new HashMap<>();

	private FxRenderTypes(String name, VertexFormat format, VertexFormat.Mode mode, int size, boolean crumbling, boolean sort,
			Runnable setup, Runnable clear) {
		super(name, format, mode, size, crumbling, sort, setup, clear);
	}

	public static RenderType glow(ResourceLocation texture) {
		return GLOW.computeIfAbsent(texture, t -> create("grandline_fx_glow", DefaultVertexFormat.POSITION_COLOR_TEX, VertexFormat.Mode.QUADS, 4096,
				false, false, CompositeState.builder()
						.setShaderState(POSITION_COLOR_TEX_SHADER)
						.setTextureState(new TextureStateShard(t, true, false))
						.setTransparencyState(LIGHTNING_TRANSPARENCY)
						.setCullState(NO_CULL)
						.setDepthTestState(LEQUAL_DEPTH_TEST)
						.setWriteMaskState(COLOR_WRITE)
						.setLightmapState(NO_LIGHTMAP)
						.createCompositeState(false)));
	}

	public static RenderType soft(ResourceLocation texture) {
		return SOFT.computeIfAbsent(texture, t -> create("grandline_fx_soft", DefaultVertexFormat.POSITION_COLOR_TEX, VertexFormat.Mode.QUADS, 4096,
				false, true, CompositeState.builder()
						.setShaderState(POSITION_COLOR_TEX_SHADER)
						.setTextureState(new TextureStateShard(t, true, false))
						.setTransparencyState(TRANSLUCENT_TRANSPARENCY)
						.setCullState(NO_CULL)
						.setDepthTestState(LEQUAL_DEPTH_TEST)
						.setWriteMaskState(COLOR_WRITE)
						.setLightmapState(NO_LIGHTMAP)
						.createCompositeState(false)));
	}
}
