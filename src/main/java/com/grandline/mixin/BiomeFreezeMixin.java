package com.grandline.mixin;

import com.grandline.world.ModWorld;
import com.grandline.world.plan.Terrain;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * In Grand Line worlds the sea never freezes: snowy islands (snowy plains, see IslandBiomes) reach down to their
 * beaches, and the game would otherwise ice over the water along their shores and round their docks.
 */
@Mixin(Biome.class)
public abstract class BiomeFreezeMixin {
	@Inject(method = "shouldFreeze(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;Z)Z", at = @At("HEAD"), cancellable = true)
	private void grandline$noSeaIce(LevelReader level, BlockPos pos, boolean mustBeAtEdge, CallbackInfoReturnable<Boolean> cir) {
		if (pos.getY() > Terrain.WATER_TOP) {
			return;
		}
		ServerLevel server = level instanceof ServerLevel s ? s : level instanceof WorldGenRegion r ? r.getLevel() : null;
		if (server != null && ModWorld.isGrandLineWorld(server.getServer().overworld())) {
			cir.setReturnValue(false);
		}
	}
}
