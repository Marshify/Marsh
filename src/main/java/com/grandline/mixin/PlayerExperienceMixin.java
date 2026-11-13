package com.grandline.mixin;

import com.grandline.GrandLineMod;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Players never gain vanilla experience points or levels (Grand Line's own experience is separate and unaffected). */
@Mixin(Player.class)
public abstract class PlayerExperienceMixin {
	@Inject(method = "giveExperiencePoints", at = @At("HEAD"), cancellable = true)
	private void grandline$noPoints(int amount, CallbackInfo ci) {
		if (GrandLineMod.config().disableVanillaExperience && amount > 0) {
			ci.cancel();
		}
	}

	@Inject(method = "giveExperienceLevels", at = @At("HEAD"), cancellable = true)
	private void grandline$noLevels(int amount, CallbackInfo ci) {
		if (GrandLineMod.config().disableVanillaExperience && amount > 0) {
			ci.cancel();
		}
	}
}
