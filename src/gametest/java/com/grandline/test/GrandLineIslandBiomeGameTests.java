package com.grandline.test;

import com.grandline.world.IslandBiomes;
import com.grandline.world.IslandTheme;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Island biomes: desert and snowy islands get those biomes, and every theme maps to a biome the game has. */
public final class GrandLineIslandBiomeGameTests implements FabricGameTest {
	@GameTest(template = EMPTY_STRUCTURE, batch = "island_biomes")
	public void themesMapToRealBiomes(GameTestHelper helper) {
		helper.assertTrue(IslandBiomes.biomeFor(IslandTheme.DESERT).location().toString().equals("minecraft:desert"), "desert islands are desert");
		helper.assertTrue(IslandBiomes.biomeFor(IslandTheme.SNOW).location().toString().equals("minecraft:snowy_plains"), "snowy islands are snowy plains");
		var biomes = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
		for (IslandTheme theme : IslandTheme.values()) {
			helper.assertTrue(biomes.getHolder(IslandBiomes.biomeFor(theme)).isPresent(), theme + " maps to a registered biome");
		}
		helper.succeed();
	}
}
