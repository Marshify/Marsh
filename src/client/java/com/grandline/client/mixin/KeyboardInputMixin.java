package com.grandline.client.mixin;

import com.grandline.client.camera.GrandLineCamera;
import com.grandline.client.camera.ShiftLockKeys;
import net.minecraft.client.player.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * After the keyboard is read: no player-controlled crouching in Grand Line play (Shift is Shift Lock; riding keeps it to
 * get off, creative and spectator keep it to fly down), and exploration movement steered relative to the camera.
 */
@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin {
	@Inject(method = "tick", at = @At("TAIL"))
	private void grandline$noSneakAndSteer(boolean slow, float slowFactor, CallbackInfo ci) {
		KeyboardInput self = (KeyboardInput) (Object) this;
		self.shiftKeyDown = ShiftLockKeys.sneakAllowed();
		GrandLineCamera.steer(self);
	}
}
