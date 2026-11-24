package com.grandline.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Whole-body spin, lean and bank for {@link com.grandline.client.fx.BodyMotion} (rendering only; the camera is untouched). */
@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererBodyMotionMixin {
	@Inject(method = "setupRotations(Lnet/minecraft/client/player/AbstractClientPlayer;Lcom/mojang/blaze3d/vertex/PoseStack;FFF)V", at = @At("TAIL"))
	private void grandline$bodyMotion(AbstractClientPlayer player, PoseStack pose, float ageInTicks, float rotationYaw, float partialTicks,
			CallbackInfo ci) {
		com.grandline.client.fx.BodyMotion.applyBody(player, pose, player.tickCount + partialTicks);
	}
}
