package com.grandline.mixin;

import com.grandline.GrandLineMod;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * No vanilla experience orbs: {@code award} is how every source spawns them (mob and player deaths, mining, smelting,
 * breeding, fishing, trading), so nothing drops; any orb already in the world vanishes without giving XP.
 */
@Mixin(ExperienceOrb.class)
public abstract class ExperienceOrbMixin {
	@Inject(method = "award", at = @At("HEAD"), cancellable = true)
	private static void grandline$noOrbs(ServerLevel level, Vec3 pos, int amount, CallbackInfo ci) {
		if (GrandLineMod.config().disableVanillaExperience) {
			ci.cancel();
		}
	}

	@Inject(method = "playerTouch", at = @At("HEAD"), cancellable = true)
	private void grandline$noPickup(Player player, CallbackInfo ci) {
		if (GrandLineMod.config().disableVanillaExperience) {
			((ExperienceOrb) (Object) this).discard();
			ci.cancel();
		}
	}
}
