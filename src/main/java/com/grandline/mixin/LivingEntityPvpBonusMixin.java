package com.grandline.mixin;

import com.grandline.reputation.ReputationService;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Applies reputation-tier PvP bonuses (only player-versus-player damage; never mobs; off by default). */
@Mixin(LivingEntity.class)
public abstract class LivingEntityPvpBonusMixin {
	@ModifyVariable(method = "hurt", at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private float grandline$pvpBonus(float amount, DamageSource source) {
		return ReputationService.pvpDamage((LivingEntity) (Object) this, source, amount);
	}
}
