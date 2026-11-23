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

/** The rebuilt Ironhold Bastion: fortress terrace to the north, settlement to the south, old worlds keep the original. */
public final class GrandLineIronholdGameTests implements FabricGameTest {
	private static void setLayout(IslandRegistry registry, int layout) {
		try {
			var field = IslandRegistry.class.getDeclaredField("layout");
			field.setAccessible(true);
			field.setInt(registry, layout);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(e);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "ironhold_plan")
	public void fortressTerraceTownAndOldWorlds(GameTestHelper helper) {
		IslandRegistry registry = IslandRegistry.get(helper.getLevel().getServer());
		int before = registry.layout();
		try {
			IslandPlan legacy = IslandPlans.legacy("Ironhold Bastion");
			setLayout(registry, 19);
			helper.assertTrue(legacy != null && IslandPlans.get("Ironhold Bastion") == legacy, "a layout-19 world keeps the original Ironhold");
			helper.assertTrue(!TreasureSpots.legacy("Ironhold Bastion").isEmpty(), "and its chest spots");
			setLayout(registry, IslandRegistry.CURRENT_LAYOUT);
			IslandPlan plan = IslandPlans.get("Ironhold Bastion");
			helper.assertTrue(plan != legacy && IslandRegistry.CURRENT_LAYOUT >= 20, "new worlds get the rebuilt bastion");
			helper.assertTrue(TreasureSpots.forIsland("Ironhold Bastion").isEmpty(), "no treasure chests, by design");
			Terrain t = plan.terrain();
			int court = t.ground(0, -20), town = t.ground(-25, 30);
			helper.assertTrue(court > town, "the fortress terrace stands above the town (" + court + " vs " + town + ")");
			helper.assertTrue(t.ground(0, 75) < Terrain.WATER_TOP, "open water past the dock");
			Anchor quest = plan.anchors().stream().filter(a -> a.type() == Anchor.Type.QUEST).findFirst().orElseThrow();
			Anchor arrival = plan.anchors().stream().filter(a -> a.type() == Anchor.Type.ARRIVAL).findFirst().orElseThrow();
			Anchor boss = plan.anchors().stream().filter(a -> a.type() == Anchor.Type.BOSS_ARENA).findFirst().orElseThrow();
			helper.assertTrue(quest.z() > 7 && quest.z() < 48, "the quest giver is in the town plaza");
			helper.assertTrue(arrival.z() > 48, "players arrive at the waterfront");
			helper.assertTrue(boss.z() < 0 && Math.abs(boss.x()) < 10, "the boss fights in the courtyard");
			helper.assertTrue(plan.anchors().stream().filter(a -> a.type() == Anchor.Type.ENEMY_CAMP).count() >= 2, "enemy camps on both landforms");
		} finally {
			setLayout(registry, before);
		}
		helper.succeed();
	}
}
