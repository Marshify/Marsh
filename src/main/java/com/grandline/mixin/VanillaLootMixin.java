package com.grandline.mixin;

import com.grandline.entity.GrandLineEnemy;
import com.grandline.world.AdventureRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adventure mode: vanilla creatures drop no vanilla loot (Grand Line enemies keep their own drops). */
@Mixin(LivingEntity.class)
public abstract class VanillaLootMixin {
	@Inject(method = "dropAllDeathLoot", at = @At("HEAD"), cancellable = true)
	private void grandline$noVanillaLoot(DamageSource source, CallbackInfo ci) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (!self.level().isClientSide && !(self instanceof Player) && !(self instanceof GrandLineEnemy) && AdventureRules.activeIn(self.level())) {
			ci.cancel();
		}
	}
}
