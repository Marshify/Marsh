package com.grandline.world;

import com.mojang.serialization.Codec;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** The late pass of designed islands (after the world's own decoration): see IslandPlan#afterDecoration. */
public final class SeaIslandsLateFeature extends Feature<NoneFeatureConfiguration> {
	public SeaIslandsLateFeature(Codec<NoneFeatureConfiguration> codec) {
		super(codec);
	}

	@Override
	public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
		ChunkPos chunk = new ChunkPos(context.origin());
		boolean placed = false;
		for (SeaLayout.Spec spec : SeaLayout.all()) {
			com.grandline.world.plan.IslandPlan plan = com.grandline.world.plan.IslandPlans.get(spec.name());
			if (plan != null && com.grandline.world.plan.IslandPlans.touches(spec, plan, chunk.getMinBlockX(), chunk.getMinBlockZ())) {
				com.grandline.world.plan.IslandPlans.afterDecoration(context.level(), spec, plan, chunk.getMinBlockX(), chunk.getMinBlockZ());
				placed = true;
			}
		}
		return placed;
	}
}
