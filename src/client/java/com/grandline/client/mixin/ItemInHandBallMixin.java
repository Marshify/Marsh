package com.grandline.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** First person: no hands or held items while the player is a Spiky Ball. */
@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandBallMixin {
	@Inject(method = "renderHandsWithItems", at = @At("HEAD"), cancellable = true)
	private void grandline$ball(float partial, PoseStack pose, MultiBufferSource.BufferSource buffers, LocalPlayer player, int light, CallbackInfo ci) {
		if (com.grandline.client.fx.SpikeFx.ballActive(player.getId())) {
			ci.cancel();
		}
	}
}
