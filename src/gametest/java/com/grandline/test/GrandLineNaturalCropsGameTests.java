package com.grandline.test;

import com.grandline.world.NaturalCrops;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;

/** Natural pumpkins, melons and mushrooms are kept out of world generation only. */
public final class GrandLineNaturalCropsGameTests implements FabricGameTest {
	@GameTest(template = EMPTY_STRUCTURE, batch = "natural_crops")
	public void onlyWorldGenerationIsAffected(GameTestHelper helper) {
		var level = helper.getLevel();
		var blocked = NaturalCrops.blockedFeatures(level);
		helper.assertTrue(blocked.size() == NaturalCrops.BLOCKED.size(), "every blocked feature exists in the registry: " + blocked.size());
		var registry = level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE);
		var hugeMushroom = registry.get(new ResourceLocation("minecraft", "huge_red_mushroom"));
		helper.assertTrue(blocked.contains(hugeMushroom), "giant mushrooms are blocked");
		// Outside chunk generation (bone meal, commands) nothing is blocked: growth and farming work as usual.
		helper.assertTrue(!NaturalCrops.skip(hugeMushroom, level), "bone-mealed mushrooms still grow");
		var tree = registry.get(new ResourceLocation("minecraft", "oak"));
		helper.assertTrue(!blocked.contains(tree), "trees and other vegetation are untouched");
		// The Grand Line sea biome no longer lists them either.
		var sea = level.registryAccess().registryOrThrow(Registries.BIOME).get(com.grandline.GrandLineMod.id("grand_line_sea"));
		helper.assertTrue(sea != null, "the sea biome exists");
		var placed = level.registryAccess().registryOrThrow(Registries.PLACED_FEATURE);
		for (String id : new String[]{"patch_pumpkin", "brown_mushroom_normal", "red_mushroom_normal"}) {
			var f = placed.get(new ResourceLocation("minecraft", id));
			boolean listed = sea.getGenerationSettings().features().stream().anyMatch(set -> set.stream().anyMatch(h -> h.value() == f));
			helper.assertTrue(!listed, "the sea biome does not list " + id);
		}
		helper.succeed();
	}
}
