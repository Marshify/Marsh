package com.grandline.client.mixin;

import com.grandline.client.audio.MusicController;
import net.minecraft.client.sounds.MusicManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** No vanilla background music in Grand Line worlds: the Grand Line soundtrack plays there instead. */
@Mixin(MusicManager.class)
public abstract class MusicManagerMixin {
	@Inject(method = "tick", at = @At("HEAD"), cancellable = true)
	private void grandline$silenceVanilla(CallbackInfo ci) {
		if (MusicController.suppressVanilla()) {
			((MusicManager) (Object) this).stopPlaying(); // stops a vanilla piece that was already playing
			ci.cancel();
		}
	}
}
