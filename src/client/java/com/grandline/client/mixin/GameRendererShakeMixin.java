package com.grandline.client.mixin;

import com.grandline.client.settings.CameraShake;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds the move impact camera shake where vanilla applies its hurt camera tilt. */
@Mixin(GameRenderer.class)
public abstract class GameRendererShakeMixin {
	@Inject(method = "bobHurt", at = @At("HEAD"))
	private void grandline$shake(PoseStack pose, float partialTick, CallbackInfo ci) {
		CameraShake.apply(pose);
	}
}
