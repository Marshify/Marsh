package com.grandline.client.mixin;

import com.grandline.client.camera.CrosshairPick;
import com.grandline.client.camera.GrandLineCamera;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Picking (what the reticle is on) is the true camera ray through the screen centre (see CrosshairPick), so the block
 * or entity under the reticle is the one used or attacked. FOV responses scale the FOV.
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererCameraMixin {
	@Inject(method = "pick", at = @At("HEAD"), cancellable = true)
	private void grandline$pickUnderReticle(float partial, CallbackInfo ci) {
		if (CrosshairPick.pick(Minecraft.getInstance(), partial)) {
			ci.cancel();
		}
	}

	@Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
	private void grandline$fovResponses(Camera camera, float partial, boolean useSetting, CallbackInfoReturnable<Double> cir) {
		double scale = GrandLineCamera.fovScale();
		if (scale != 1.0) {
			cir.setReturnValue(cir.getReturnValue() * scale);
		}
	}
}
