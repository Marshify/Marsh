package com.grandline.client.mixin;

import com.grandline.client.camera.GrandLineCamera;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Attacking or using an item first turns the character to the camera's aim and tells the server (see GrandLineCamera). */
@Mixin(Minecraft.class)
public abstract class MinecraftAimMixin {
	@Inject(method = "startAttack", at = @At("HEAD"))
	private void grandline$aimAttack(CallbackInfoReturnable<Boolean> cir) {
		GrandLineCamera.faceCamera(true);
	}

	@Inject(method = "startUseItem", at = @At("HEAD"))
	private void grandline$aimUse(CallbackInfo ci) {
		GrandLineCamera.faceCamera(true);
	}
}
