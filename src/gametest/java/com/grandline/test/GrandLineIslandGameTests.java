package com.grandline.test;

import com.grandline.world.Island;
import com.grandline.world.IslandRegistry;
import com.grandline.world.ModWorld;
import com.grandline.world.SeaLayout;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.ChunkPos;

/** Existing worlds that saved Hollow Islet lose it from the island registry on load; other islands stay. */
public final class GrandLineIslandGameTests implements FabricGameTest {
	@GameTest(template = EMPTY_STRUCTURE, batch = "islands_retired")
	public void savedHollowIsletIsDroppedOnLoad(GameTestHelper helper) {
		IslandRegistry registry = IslandRegistry.get(helper.getLevel().getServer());
		// Hollow Islet as an older save stored it (designed "cave" island at -720, 480).
		long hollowId = ChunkPos.asLong(-720 >> 4, 480 >> 4);
		registry.add(new Island(hollowId, -720, 480, 18, 100, "Hollow Islet", false, true, "cave"));
		// A current designed island and a random island must survive.
		SeaLayout.Spec crystal = SeaLayout.all().stream().filter(s -> s.name().equals("Crystal Fountain")).findFirst().orElseThrow();
		long crystalId = SeaLayout.idOf(crystal);
		registry.add(new Island(crystalId, crystal.x(), crystal.z(), crystal.radius(), crystal.level(), crystal.name(), false, true, "crystal"));
		long randomId = ChunkPos.asLong(9000 >> 4, 9000 >> 4);
		registry.add(new Island(randomId, 9000, 9000, 20, 40, "Test Isle", false, true, "plain"));

		ModWorld.removeRetiredDesignedIslands(registry);
		helper.assertTrue(registry.get(hollowId) == null, "Hollow Islet removed from the registry");
		helper.assertTrue(registry.get(crystalId) != null, "current designed islands are kept");
		helper.assertTrue(registry.get(randomId) != null, "random islands are kept");
		helper.assertTrue(ModWorld.removeRetiredDesignedIslands(registry) == 0, "second load removes nothing");

		registry.removeIf(i -> i.id() == crystalId || i.id() == randomId); // leave the test world as it was
		helper.succeed();
	}
}
