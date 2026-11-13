package com.grandline.mixin;

import com.grandline.GrandLineMod;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Infinite hunger: food and saturation are held full and exhaustion never builds up. Cancelling the whole tick also
 * removes starvation damage and vanilla natural regeneration (Grand Line's HealthRegen replaces it).
 */
@Mixin(FoodData.class)
public abstract class FoodDataMixin {
	@Shadow private int foodLevel;
	@Shadow private float saturationLevel;
	@Shadow private float exhaustionLevel;

	@Inject(method = "tick", at = @At("HEAD"), cancellable = true)
	private void grandline$alwaysFull(Player player, CallbackInfo ci) {
		if (GrandLineMod.config().infiniteHunger) {
			foodLevel = 20;
			saturationLevel = 20.0F;
			exhaustionLevel = 0.0F;
			ci.cancel();
		}
	}

	@Inject(method = "addExhaustion", at = @At("HEAD"), cancellable = true)
	private void grandline$noExhaustion(float amount, CallbackInfo ci) {
		if (GrandLineMod.config().infiniteHunger) {
			ci.cancel();
		}
	}
}
