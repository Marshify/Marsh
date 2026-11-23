package com.grandline.mixin;

import com.grandline.world.NaturalCrops;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Skips natural pumpkin, melon and mushroom features while Grand Line chunks generate (see NaturalCrops). */
@Mixin(ConfiguredFeature.class)
public abstract class ConfiguredFeatureMixin {
	@Inject(method = "place", at = @At("HEAD"), cancellable = true)
	private void grandline$noNaturalCrops(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos pos,
			CallbackInfoReturnable<Boolean> cir) {
		if (NaturalCrops.skip((ConfiguredFeature<?, ?>) (Object) this, level)) {
			cir.setReturnValue(false);
		}
	}
}
