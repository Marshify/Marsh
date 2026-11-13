package com.grandline.mixin;

import com.grandline.world.VanillaSpawns;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** No vanilla natural spawning in Grand Line worlds (see {@link VanillaSpawns}). */
@Mixin(NaturalSpawner.class)
public abstract class NaturalSpawnerMixin {
	/** The per-tick natural spawning of every mob category, in every dimension. */
	@Inject(method = "spawnForChunk", at = @At("HEAD"), cancellable = true)
	private static void grandline$noNaturalSpawns(ServerLevel level, LevelChunk chunk, NaturalSpawner.SpawnState state,
			boolean friendlies, boolean enemies, boolean persistent, CallbackInfo ci) {
		if (VanillaSpawns.blocked()) {
			ci.cancel();
		}
	}

	/** Animals placed while chunks generate. */
	@Inject(method = "spawnMobsForChunkGeneration", at = @At("HEAD"), cancellable = true)
	private static void grandline$noWorldgenSpawns(ServerLevelAccessor level, Holder<Biome> biome, ChunkPos chunk, RandomSource random,
			CallbackInfo ci) {
		if (VanillaSpawns.blocked()) {
			ci.cancel();
		}
	}
}
