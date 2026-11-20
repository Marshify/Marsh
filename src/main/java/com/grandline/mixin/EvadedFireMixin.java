package com.grandline.mixin;

import com.grandline.technique.Dodges;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** An attack Instinct evaded does not set the target alight either. */
@Mixin(Entity.class)
public abstract class EvadedFireMixin {
	@Inject(method = "setSecondsOnFire", at = @At("HEAD"), cancellable = true)
	private void grandline$noFire(int seconds, CallbackInfo ci) {
		if (Dodges.justEvaded((Entity) (Object) this)) {
			ci.cancel();
		}
	}
}
