package com.grandline.client.mixin;

import com.grandline.client.lighting.StylizedLighting;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The light every block face, fluid and block entity is rendered with: see StylizedLighting. */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererLightMixin {
	@Inject(method = "getLightColor(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)I",
			at = @At("RETURN"), cancellable = true)
	private static void grandline$stylizedSkyLight(BlockAndTintGetter level, BlockState state, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
		int packed = cir.getReturnValueI();
		int adjusted = StylizedLighting.adjustPacked(level, pos, packed);
		if (adjusted != packed) {
			cir.setReturnValue(adjusted);
		}
	}
}
