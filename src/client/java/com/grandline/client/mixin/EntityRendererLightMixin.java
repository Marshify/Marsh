package com.grandline.client.mixin;

import com.grandline.client.lighting.StylizedLighting;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Entities under floating islands are lit like the ground they stand on (see StylizedLighting). */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererLightMixin {
	@Inject(method = "getSkyLightLevel", at = @At("RETURN"), cancellable = true)
	private void grandline$stylizedSkyLight(Entity entity, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
		int sky = cir.getReturnValueI();
		int adjusted = StylizedLighting.skyLight(entity.level(), pos, sky);
		if (adjusted != sky) {
			cir.setReturnValue(adjusted);
		}
	}
}
