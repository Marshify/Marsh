package com.grandline.mixin;

import com.grandline.world.AdventureRules;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

/** Adventure mode: explosions hurt and push, but break no blocks. */
@Mixin(Explosion.class)
public abstract class ExplosionNoBlockDamageMixin {
	@Shadow @Final private Level level;
	@Shadow @Final private ObjectArrayList<BlockPos> toBlow;

	@Inject(method = "finalizeExplosion", at = @At("HEAD"))
	private void grandline$keepBlocks(boolean spawnParticles, CallbackInfo ci) {
		if (!level.isClientSide && AdventureRules.activeIn(level)) {
			toBlow.clear();
		}
	}
}
