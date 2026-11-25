package com.grandline.client.mixin;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Rendering-only part hiding: Self Destruct's bomb head replaces the head (and hat layer); Smoke's elemental limbs and
 * Elemental Reflex dispersal hide the parts turned to smoke. Restored automatically when the state ends.
 */
@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererBombHeadMixin {
	/** Spiky Ball: the whole player (model, armour, held items, cosmetics) is replaced by the ball while it rolls. */
	@Inject(method = "render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
			at = @At("HEAD"), cancellable = true)
	private void grandline$ball(AbstractClientPlayer player, float yaw, float partial, com.mojang.blaze3d.vertex.PoseStack pose,
			net.minecraft.client.renderer.MultiBufferSource buffers, int light, CallbackInfo ci) {
		if (com.grandline.client.fx.SpikeFx.ballActive(player.getId())) {
			ci.cancel();
		}
	}

	@Inject(method = "setModelProperties", at = @At("TAIL"))
	private void grandline$bombHead(AbstractClientPlayer player, CallbackInfo ci) {
		var model = ((PlayerRenderer) (Object) this).getModel();
		if (com.grandline.client.fx.BombFx.headActive(player.getId())) {
			model.head.visible = false;
			model.hat.visible = false;
		}
		// Elemental limbs (Smoke): the parts turned to smoke are hidden while smoke geometry replaces them.
		int smoke = com.grandline.client.fx.SmokeFx.hidden(player.getId());
		if (smoke != 0) {
			if ((smoke & com.grandline.client.fx.SmokeFx.RIGHT_ARM) != 0) {
				model.rightArm.visible = false;
				model.rightSleeve.visible = false;
			}
			if ((smoke & com.grandline.client.fx.SmokeFx.LEFT_ARM) != 0) {
				model.leftArm.visible = false;
				model.leftSleeve.visible = false;
			}
			if ((smoke & com.grandline.client.fx.SmokeFx.HEAD) != 0) {
				model.head.visible = false;
				model.hat.visible = false;
			}
			if ((smoke & com.grandline.client.fx.SmokeFx.BODY) != 0) {
				model.body.visible = false;
				model.jacket.visible = false;
			}
			if ((smoke & com.grandline.client.fx.SmokeFx.LEGS) != 0) {
				model.rightLeg.visible = false;
				model.leftLeg.visible = false;
				model.rightPants.visible = false;
				model.leftPants.visible = false;
			}
		}
	}
}
