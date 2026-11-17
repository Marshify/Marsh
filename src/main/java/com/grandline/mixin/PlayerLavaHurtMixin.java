package com.grandline.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Touching lava (the side of a lavafall, say) neither burns nor sets a player alight. */
@Mixin(Entity.class)
public abstract class PlayerLavaHurtMixin {
	@Inject(method = "lavaHurt", at = @At("HEAD"), cancellable = true)
	private void grandline$noLavaHurt(CallbackInfo ci) {
		if ((Object) this instanceof Player) {
			ci.cancel();
		}
	}
}
