package com.grandline.client.mixin;

import com.grandline.client.ClientNavigation;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The navigation outline is Grand Line gold. */
@Mixin(Entity.class)
public abstract class NavigationGlowColorMixin {
	@Inject(method = "getTeamColor", at = @At("HEAD"), cancellable = true)
	private void grandline$goldOutline(CallbackInfoReturnable<Integer> cir) {
		if (((Entity) (Object) this).level().isClientSide && ClientNavigation.highlighted((Entity) (Object) this)) {
			cir.setReturnValue(0xFFD866);
		}
	}
}
