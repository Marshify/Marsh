package com.grandline.test;

import com.grandline.treasure.TreasureSpots;
import com.grandline.world.IslandRegistry;
import com.grandline.world.plan.Anchor;
import com.grandline.world.plan.IslandPlan;
import com.grandline.world.plan.IslandPlans;
import com.grandline.world.plan.Terrain;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** The rebuilt Frostpeak Isle (Snow Island): layered terraces, the arena and village plateaus, NPCs in place. */
public final class GrandLineFrostpeakGameTests implements FabricGameTest {
	private static void setLayout(IslandRegistry registry, int layout) {
		try {
			var field = IslandRegistry.class.getDeclaredField("layout");
			field.setAccessible(true);
			field.setInt(registry, layout);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(e);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "frostpeak_plan")
	public void snowIslandLayersArenaVillageAndOldWorlds(GameTestHelper helper) {
		IslandRegistry registry = IslandRegistry.get(helper.getLevel().getServer());
		int before = registry.layout();
		try {
			checks(helper, registry);
		} finally {
			setLayout(registry, before); // also when a check fails: the test world must stay on its own layout
		}
		helper.succeed();
	}

	private static void checks(GameTestHelper helper, IslandRegistry registry) {
		IslandPlan legacy = IslandPlans.legacy("Frostpeak Isle");
		setLayout(registry, 10);
		helper.assertTrue(legacy != null && IslandPlans.get("Frostpeak Isle") == legacy, "a layout-10 world keeps the original Frostpeak");
		helper.assertTrue(!TreasureSpots.legacy("Frostpeak Isle").isEmpty(), "and its chest spots");
		setLayout(registry, IslandRegistry.CURRENT_LAYOUT);
		IslandPlan plan = IslandPlans.get("Frostpeak Isle");
		helper.assertTrue(plan != legacy && IslandRegistry.CURRENT_LAYOUT >= 11, "new worlds get the rebuilt snow island");
		helper.assertTrue(TreasureSpots.forIsland("Frostpeak Isle").isEmpty(), "no treasure chests, by design");
		Terrain t = plan.terrain();
		helper.assertTrue(t.ground(-24, -17) == 82, "the arena plateau is at sea +20");
		helper.assertTrue(t.ground(25, -10) == 81 && t.ground(23, -3) == 81, "the village plateau is at sea +19");
		helper.assertTrue(t.ground(-14, 48) == 64 && t.ground(-1, 48) == 64, "the dock landing is at sea +2");
		helper.assertTrue(t.ground(-7, 43) == 66, "the market stalls stand on the lower coast (+4)");
		helper.assertTrue(t.ground(4, 30) == 69 && t.ground(12, 18) == 81, "the staircase climbs from the cove (+7) to the plateau (+19)");
		helper.assertTrue(t.ground(-14, 70) < Terrain.WATER_TOP, "open water past the docks");
		Anchor quest = plan.anchors().stream().filter(a -> a.type() == Anchor.Type.QUEST).findFirst().orElseThrow();
		Anchor boss = plan.anchors().stream().filter(a -> a.type() == Anchor.Type.BOSS_ARENA).findFirst().orElseThrow();
		helper.assertTrue(quest.y() == 82 && quest.x() >= 0 && quest.x() <= 30, "the Villager is in the village");
		helper.assertTrue(boss.z() < -35, "Glacier Maw has the northern ridge to himself (the ice cavern is the Ability Teacher's)");
	}
}
