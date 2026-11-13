package com.grandline.world;

import com.mojang.serialization.Codec;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Re-places Dawnreach's saved fountain in the last decoration step, so sea decoration that runs after the islands
 * (seagrass growing in any open water) never ends up inside it: the fountain is exactly what the file holds.
 */
public final class FountainFeature extends Feature<NoneFeatureConfiguration> {
	public FountainFeature(Codec<NoneFeatureConfiguration> codec) {
		super(codec);
	}

	@Override
	public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
		ChunkPos chunk = new ChunkPos(context.origin());
		return com.grandline.world.plan.DawnreachHarbor.placeFountainInChunk(context.level(), chunk.getMinBlockX(), chunk.getMinBlockZ());
	}
}
