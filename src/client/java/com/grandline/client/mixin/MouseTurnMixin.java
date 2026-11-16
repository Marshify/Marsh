package com.grandline.client.mixin;

import com.grandline.client.camera.GrandLineCamera;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** With the Grand Line camera active, the mouse turns the camera (and the character only when facing is locked). */
@Mixin(MouseHandler.class)
public abstract class MouseTurnMixin {
	@Redirect(method = "turnPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"))
	private void grandline$turnCamera(LocalPlayer player, double dx, double dy) {
		if (GrandLineCamera.active()) {
			GrandLineCamera.turn(player, dx, dy);
		} else {
			player.turn(dx, dy);
		}
	}
}
