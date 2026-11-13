package com.grandline.world;

import com.mojang.serialization.Codec;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Builds the part of every designed Grand Line island that falls inside the chunk being generated: hand-designed
 * plans where they exist ({@link com.grandline.world.plan.IslandPlans}), the generic themed builder otherwise.
 */
public final class SeaIslandsFeature extends Feature<NoneFeatureConfiguration> {
	public SeaIslandsFeature(Codec<NoneFeatureConfiguration> codec) {
		super(codec);
	}

	@Override
	public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
		ChunkPos chunk = new ChunkPos(context.origin());
		boolean placed = false;
		for (SeaLayout.Spec spec : SeaLayout.all()) {
			com.grandline.world.plan.IslandPlan plan = com.grandline.world.plan.IslandPlans.get(spec.name());
			if (plan != null) {
				if (com.grandline.world.plan.IslandPlans.touches(spec, plan, chunk.getMinBlockX(), chunk.getMinBlockZ())) {
					com.grandline.world.plan.IslandPlans.build(context.level(), spec, plan, chunk.getMinBlockX(), chunk.getMinBlockZ());
					placed = true;
				}
				continue;
			}
			if (IslandBuilder.touches(spec, chunk.getMinBlockX(), chunk.getMinBlockZ())) {
				new IslandBuilder(context.level(), spec, chunk.getMinBlockX(), chunk.getMinBlockZ()).build();
				placed = true;
			}
		}
		return placed;
	}
}
