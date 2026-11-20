package com.grandline.mixin;

import com.grandline.technique.Dodges;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Instinct evasion and elemental bodies (cancel the hit), Aura's bonus and reduction, Aura XP for landed hits. */
@Mixin(LivingEntity.class)
public abstract class TechniqueDamageMixin {
	@Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
	private void grandline$evade(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
		if (Dodges.evades((LivingEntity) (Object) this, source, amount)) {
			cir.setReturnValue(false);
		}
	}

	@ModifyVariable(method = "hurt", at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private float grandline$aura(float amount, DamageSource source) {
		return Dodges.modify((LivingEntity) (Object) this, source, amount);
	}

	@Inject(method = "hurt", at = @At("RETURN"))
	private void grandline$auraXp(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
		Dodges.afterHurt((LivingEntity) (Object) this, source, amount, cir.getReturnValueZ());
	}
}
