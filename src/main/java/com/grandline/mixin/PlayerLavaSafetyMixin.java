package com.grandline.mixin;

import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Players walk on still lava as if it were floor (the way striders do), so they can never fall into a lava pool. */
@Mixin(LivingEntity.class)
public abstract class PlayerLavaSafetyMixin {
	@Inject(method = "canStandOnFluid", at = @At("HEAD"), cancellable = true)
	private void grandline$standOnLava(FluidState fluid, CallbackInfoReturnable<Boolean> cir) {
		if ((Object) this instanceof Player && fluid.is(FluidTags.LAVA)) {
			cir.setReturnValue(true);
		}
	}
}
