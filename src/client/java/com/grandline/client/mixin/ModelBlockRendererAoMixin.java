package com.grandline.client.mixin;

import com.grandline.client.lighting.StylizedLighting;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Floating islands' cloud bases are drawn without ambient occlusion (see StylizedLighting#flatCloud): their jagged,
 * stepped surface is nearly all corners, and corner darkening turned whole sides of the islands grey-black.
 */
@Mixin(ModelBlockRenderer.class)
public abstract class ModelBlockRendererAoMixin {
	@Redirect(method = "tesselateBlock", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/resources/model/BakedModel;useAmbientOcclusion()Z"))
	private boolean grandline$noAoOnClouds(BakedModel model, BlockAndTintGetter level, BakedModel modelArg, BlockState state, BlockPos pos,
			com.mojang.blaze3d.vertex.PoseStack poseStack, com.mojang.blaze3d.vertex.VertexConsumer consumer, boolean checkSides,
			net.minecraft.util.RandomSource random, long seed, int overlay) {
		return model.useAmbientOcclusion() && !StylizedLighting.flatCloud(state, pos);
	}
}
