package com.grandline.client.mixin;

import com.grandline.client.camera.GrandLineCamera;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Picking (what the reticle points at) follows the Grand Line camera's direction, from the player's eyes, so the
 * reticle and what gets attacked or used agree even while the character faces elsewhere. FOV responses scale the FOV.
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererCameraMixin {
	@Unique
	private float grandline$yRot, grandline$xRot, grandline$yRotO, grandline$xRotO;
	@Unique
	private boolean grandline$swapped;

	@Inject(method = "pick", at = @At("HEAD"))
	private void grandline$pickAlongCamera(float partial, CallbackInfo ci) {
		Entity e = Minecraft.getInstance().getCameraEntity();
		grandline$swapped = false;
		if (e != null && e == Minecraft.getInstance().player && GrandLineCamera.active() && !GrandLineCamera.facingLocked()) {
			grandline$yRot = e.getYRot();
			grandline$xRot = e.getXRot();
			grandline$yRotO = e.yRotO;
			grandline$xRotO = e.xRotO;
			e.setYRot(GrandLineCamera.yaw());
			e.setXRot(GrandLineCamera.pitch());
			e.yRotO = GrandLineCamera.yaw();
			e.xRotO = GrandLineCamera.pitch();
			grandline$swapped = true;
		}
	}

	@Inject(method = "pick", at = @At("RETURN"))
	private void grandline$restoreAfterPick(float partial, CallbackInfo ci) {
		if (grandline$swapped) {
			Entity e = Minecraft.getInstance().getCameraEntity();
			if (e != null) {
				e.setYRot(grandline$yRot);
				e.setXRot(grandline$xRot);
				e.yRotO = grandline$yRotO;
				e.xRotO = grandline$xRotO;
			}
			grandline$swapped = false;
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
