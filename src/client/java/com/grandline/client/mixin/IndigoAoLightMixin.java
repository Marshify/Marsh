package com.grandline.client.mixin;

import com.grandline.client.lighting.StylizedLighting;
import net.fabricmc.fabric.impl.client.indigo.renderer.aocalc.AoCalculator;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Indigo's own copy of the light lookup for smooth-lit blocks: the same stylized sky light (see StylizedLighting). */
@Mixin(value = AoCalculator.class, remap = false)
public abstract class IndigoAoLightMixin {
	@Inject(method = "getLightmapCoordinates", at = @At("RETURN"), cancellable = true, require = 0)
	private static void grandline$stylizedSkyLight(BlockAndTintGetter level, BlockState state, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
		int packed = cir.getReturnValueI();
		int adjusted = StylizedLighting.adjustPacked(level, pos, packed);
		if (adjusted != packed) {
			cir.setReturnValue(adjusted);
		}
	}
}
