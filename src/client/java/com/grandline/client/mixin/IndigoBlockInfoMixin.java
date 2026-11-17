package com.grandline.client.mixin;

import com.grandline.client.lighting.StylizedLighting;
import net.fabricmc.fabric.impl.client.indigo.renderer.render.BlockRenderInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fabric's Indigo renderer (the one Fabric uses for terrain) decides ambient occlusion here rather than in vanilla's
 * ModelBlockRenderer, so floating-island cloud gets flat lighting here too (see StylizedLighting#flatCloud).
 */
@Mixin(value = BlockRenderInfo.class, remap = false)
public abstract class IndigoBlockInfoMixin {
	@Shadow
	boolean useAo;

	@Inject(method = "prepareForBlock", at = @At("TAIL"), require = 0)
	private void grandline$noAoOnClouds(BlockState state, BlockPos pos, boolean modelAo, CallbackInfo ci) {
		if (useAo && StylizedLighting.flatCloud(state, pos)) {
			useAo = false;
		}
	}
}
