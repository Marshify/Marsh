package com.grandline.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** First person: body attachments (forearm blades, rotors) on the visible hands, in the same arm space as third person. */
@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererHandAttachmentMixin {
	@Inject(method = "renderRightHand", at = @At("TAIL"))
	private void grandline$rightHand(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player, CallbackInfo ci) {
		draw(pose, buffers, light, player, false);
	}

	@Inject(method = "renderLeftHand", at = @At("TAIL"))
	private void grandline$leftHand(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player, CallbackInfo ci) {
		draw(pose, buffers, light, player, true);
	}

	private void draw(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player, boolean left) {
		var model = ((PlayerRenderer) (Object) this).getModel();
		pose.pushPose();
		(left ? model.leftArm : model.rightArm).translateAndRotate(pose);
		com.grandline.client.fx.BodyAttachments.drawArm(pose, buffers, light, player.getId(),
				player.tickCount + net.minecraft.client.Minecraft.getInstance().getFrameTime(), left, "slim".equals(player.getModelName()));
		com.grandline.client.fx.SpikeFx.drawHand(pose, buffers, light, player.getId(), left);
		com.grandline.client.fx.BombFx.drawArm(pose, buffers, light, player.getId(), player.tickCount + net.minecraft.client.Minecraft.getInstance().getFrameTime(), left);
		com.grandline.client.fx.SpringLimbs.drawArm(pose, buffers, light, player, player.tickCount + net.minecraft.client.Minecraft.getInstance().getFrameTime(), left);
		pose.popPose();
	}
}
