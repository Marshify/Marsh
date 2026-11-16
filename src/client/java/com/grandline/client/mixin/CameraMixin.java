package com.grandline.client.mixin;

import com.grandline.client.camera.CameraRig;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Third-person back view: the Grand Line camera places the camera after vanilla (see CameraRig). */
@Mixin(Camera.class)
public abstract class CameraMixin {
	@Inject(method = "setup", at = @At("TAIL"))
	private void grandline$actionCamera(BlockGetter level, Entity entity, boolean detached, boolean mirrored, float partial, CallbackInfo ci) {
		if (detached && !mirrored) {
			CameraRig.place((Camera) (Object) this, entity, partial);
		}
	}
}
