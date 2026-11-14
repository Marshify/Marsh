package com.grandline.client.mixin;

import com.grandline.client.ClientNavigation;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Outlines the quest giver this player is navigating to, for this player only (no glowing effect on the NPC itself). */
@Mixin(Minecraft.class)
public abstract class NavigationGlowMixin {
	@Inject(method = "shouldEntityAppearGlowing", at = @At("HEAD"), cancellable = true)
	private void grandline$navigationOutline(Entity entity, CallbackInfoReturnable<Boolean> cir) {
		if (ClientNavigation.highlighted(entity)) {
			cir.setReturnValue(true);
		}
	}
}
