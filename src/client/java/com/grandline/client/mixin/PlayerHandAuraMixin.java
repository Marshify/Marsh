package com.grandline.client.mixin;

import com.grandline.client.render.aura.AuraLayer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** First person: the Aura coating over the bare arm. */
@Mixin(PlayerRenderer.class)
public abstract class PlayerHandAuraMixin {
	@Inject(method = "renderRightHand", at = @At("TAIL"))
	private void grandline$auraRight(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player, CallbackInfo ci) {
		AuraLayer.renderArm(pose, buffers, light, player, ((PlayerRenderer) (Object) this).getModel().rightArm, true);
	}

	@Inject(method = "renderLeftHand", at = @At("TAIL"))
	private void grandline$auraLeft(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player, CallbackInfo ci) {
		AuraLayer.renderArm(pose, buffers, light, player, ((PlayerRenderer) (Object) this).getModel().leftArm, false);
	}
}
