package com.grandline.mixin;

import com.grandline.race.RacePassives;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Race damage: the attacker's bonus and the victim's reduction on the way in, lifesteal once the hit has landed. */
@Mixin(LivingEntity.class)
public abstract class RaceDamageMixin {
	@ModifyVariable(method = "hurt", at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private float grandline$raceDamage(float amount, DamageSource source) {
		return RacePassives.modifyDamage((LivingEntity) (Object) this, source, amount);
	}

	@Inject(method = "hurt", at = @At("RETURN"))
	private void grandline$raceAfterHurt(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
		RacePassives.afterHurt((LivingEntity) (Object) this, source, amount, cir.getReturnValueZ());
	}
}
