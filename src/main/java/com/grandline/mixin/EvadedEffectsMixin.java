package com.grandline.mixin;

import com.grandline.technique.Dodges;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** An attack Instinct evaded brings nothing with it: no knockback and no harmful effect. */
@Mixin(LivingEntity.class)
public abstract class EvadedEffectsMixin {
	@Inject(method = "knockback", at = @At("HEAD"), cancellable = true)
	private void grandline$noKnockback(double strength, double x, double z, CallbackInfo ci) {
		if (Dodges.justEvaded((LivingEntity) (Object) this)) {
			ci.cancel();
		}
	}

	@Inject(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z", at = @At("HEAD"), cancellable = true)
	private void grandline$noEffect(MobEffectInstance effect, Entity source, CallbackInfoReturnable<Boolean> cir) {
		if (effect.getEffect().getCategory() == MobEffectCategory.HARMFUL && Dodges.justEvaded((LivingEntity) (Object) this)) {
			cir.setReturnValue(false);
		}
	}
}
