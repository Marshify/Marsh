package com.grandline.test;

import com.grandline.treasure.TreasureSpots;
import com.grandline.treasure.TreasureTier;
import com.grandline.world.IslandRegistry;
import com.grandline.world.plan.Anchor;
import com.grandline.world.plan.IslandPlan;
import com.grandline.world.plan.IslandPlans;
import com.grandline.world.plan.Terrain;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

import java.util.List;

/** The rebuilt Saltmarket Docks (pirate harbour): old worlds keep the original; the town, harbour and wilderness line up. */
public final class GrandLineHarbourGameTests implements FabricGameTest {
	private static void setLayout(IslandRegistry registry, int layout) {
		try {
			var field = IslandRegistry.class.getDeclaredField("layout");
			field.setAccessible(true);
			field.setInt(registry, layout);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(e);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "harbour_plan")
	public void harbourTownWithWildernessAndOldWorldsKeepTheOriginal(GameTestHelper helper) {
		IslandRegistry registry = IslandRegistry.get(helper.getLevel().getServer());
		int before = registry.layout();
		IslandPlan legacy = IslandPlans.legacy("Saltmarket Docks");
		setLayout(registry, 9);
		helper.assertTrue(legacy != null && IslandPlans.get("Saltmarket Docks") == legacy, "a layout-9 world keeps the original Saltmarket");
		helper.assertTrue(!TreasureSpots.legacy("Saltmarket Docks").isEmpty(), "and its own treasure spots");
		setLayout(registry, IslandRegistry.CURRENT_LAYOUT);
		IslandPlan plan = IslandPlans.get("Saltmarket Docks");
		helper.assertTrue(plan != legacy && IslandRegistry.CURRENT_LAYOUT >= 10, "new worlds get the pirate harbour");
		Terrain t = plan.terrain();
		helper.assertTrue(t.ground(0, 0) == 67 && t.ground(-60, 30) == 67 && t.ground(60, -30) == 67, "the walled town is level at y 67");
		helper.assertTrue(t.ground(0, 58) == 64 && t.ground(-70, 60) == 64, "the promenade is at y 64");
		helper.assertTrue(t.ground(-100, 42) == 64, "the lighthouse rock top is paved at y 64");
		helper.assertTrue(t.ground(-80, 42) < Terrain.WATER_TOP, "a channel separates the lighthouse rock (crossed by the causeway)");
		for (int[] p : new int[][]{{-45, -95}, {42, -100}, {0, -120}}) {
			int g = t.ground(p[0], p[1]);
			helper.assertTrue(g > Terrain.WATER_TOP + 1 && g <= 76, "rolling wilderness behind the town at " + p[0] + "," + p[1] + ": " + g);
		}
		helper.assertTrue(t.ground(0, 130) < Terrain.WATER_TOP, "open sea beyond the piers");
		Anchor quest = one(plan, Anchor.Type.QUEST), home = one(plan, Anchor.Type.HOME), boss = one(plan, Anchor.Type.BOSS_ARENA);
		Anchor arrival = one(plan, Anchor.Type.ARRIVAL);
		List<Anchor> camps = plan.anchors().stream().filter(a -> a.type() == Anchor.Type.ENEMY_CAMP).toList();
		helper.assertTrue(quest.y() == 68 && Math.abs(quest.x()) < 20 && quest.z() < 0 && quest.z() > -36, "the Pirate Adventurer stands inside the landward gate");
		helper.assertTrue(home.y() == 68 && Math.abs(home.x()) < 20 && home.z() > 40, "the Set Home Point is inside the harbour gate");
		helper.assertTrue(arrival.y() == 65 && arrival.z() >= 52 && arrival.z() <= 66, "players arrive on the promenade");
		helper.assertTrue(camps.size() == 2 && camps.stream().allMatch(c -> c.z() < -60), "both camps are in the wilderness behind the town");
		helper.assertTrue(boss.z() < -100, "the boss stands at the far end of the stronger camp");
		for (Anchor camp : camps) {
			helper.assertTrue(Math.hypot(quest.x() - camp.x(), quest.z() - camp.z()) >= camp.radius() + 40, "the quest giver is clear of " + camp);
		}
		var spots = TreasureSpots.forIsland("Saltmarket Docks");
		helper.assertTrue(spots.size() >= 4 && spots.stream().filter(s -> s.tier() == TreasureTier.LEGENDARY).count() == 1,
				"the harbour has its chests, one legendary");
		setLayout(registry, before);
		helper.succeed();
	}

	private static Anchor one(IslandPlan plan, Anchor.Type type) {
		return plan.anchors().stream().filter(a -> a.type() == type).findFirst().orElseThrow();
	}
}
