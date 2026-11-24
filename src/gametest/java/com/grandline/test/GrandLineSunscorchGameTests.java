package com.grandline.test;

import com.grandline.world.IslandRegistry;
import com.grandline.world.plan.Anchor;
import com.grandline.world.plan.IslandPlan;
import com.grandline.world.plan.IslandPlans;
import com.grandline.world.plan.Terrain;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

import java.util.List;

/** The rebuilt Sunscorch Dunes: old worlds keep the old island, the plan keeps its gameplay spots, arrivals are safe. */
public final class GrandLineSunscorchGameTests implements FabricGameTest {
	private static void setLayout(IslandRegistry registry, int layout) {
		try {
			var field = IslandRegistry.class.getDeclaredField("layout");
			field.setAccessible(true);
			field.setInt(registry, layout);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(e);
		}
	}

	private static Anchor one(IslandPlan plan, Anchor.Type type) {
		return plan.anchors().stream().filter(a -> a.type() == type).findFirst().orElseThrow();
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "sunscorch_plan")
	public void olderWorldsKeepTheOriginalIsland(GameTestHelper helper) {
		IslandRegistry registry = IslandRegistry.get(helper.getLevel().getServer());
		int before = registry.layout();
		IslandPlan legacy = IslandPlans.legacy("Sunscorch Dunes");
		helper.assertTrue(legacy != null, "the original plan is kept");
		setLayout(registry, 4);
		helper.assertTrue(IslandPlans.get("Sunscorch Dunes") == legacy, "a layout-4 world generates the original Sunscorch");
		setLayout(registry, IslandRegistry.CURRENT_LAYOUT);
		IslandPlan rebuilt = IslandPlans.get("Sunscorch Dunes");
		helper.assertTrue(rebuilt != legacy && IslandRegistry.CURRENT_LAYOUT >= 5, "new worlds get the rebuild");
		helper.assertTrue(IslandPlans.get("Millbrook Village") == IslandPlans.get("Millbrook Village"), "other islands unaffected");
		setLayout(registry, before);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "sunscorch_plan")
	public void planKeepsEveryGameplaySpotAndKeepsArrivalsSafe(GameTestHelper helper) {
		IslandRegistry registry = IslandRegistry.get(helper.getLevel().getServer());
		int before = registry.layout();
		setLayout(registry, IslandRegistry.CURRENT_LAYOUT);
		try {
			planChecks(helper);
		} finally {
			setLayout(registry, before); // also when a check fails: a failed test must not leave the test world on another layout
		}
		helper.succeed();
	}

	private static void planChecks(GameTestHelper helper) {
		IslandPlan plan = IslandPlans.get("Sunscorch Dunes");
		helper.assertTrue(plan.anchors().stream().noneMatch(a -> a.type() == Anchor.Type.BOSS_ARENA), "no boss on Sunscorch");
		for (Anchor.Type t : List.of(Anchor.Type.QUEST, Anchor.Type.HOME, Anchor.Type.DOCK, Anchor.Type.ARRIVAL, Anchor.Type.TREASURE)) {
			helper.assertTrue(plan.anchors().stream().anyMatch(a -> a.type() == t), "has a " + t + " spot");
		}
		List<Anchor> camps = plan.anchors().stream().filter(a -> a.type() == Anchor.Type.ENEMY_CAMP).toList();
		helper.assertTrue(camps.size() == 2, "two combat areas: " + camps.size());
		Terrain terrain = plan.terrain();
		// Safe spots: no enemy spawn spot (Dune Prowlers, Sandwatch Captains) is near the quest giver, home point or arrival.
		var roster = com.grandline.world.plan.IslandRoster.of(com.grandline.world.IslandRegistry.get(helper.getLevel().getServer()).byName("Sunscorch Dunes"));
		for (Anchor.Type t : List.of(Anchor.Type.QUEST, Anchor.Type.HOME, Anchor.Type.ARRIVAL)) {
			Anchor a = one(plan, t);
			if (roster != null) {
				for (var s : roster.spots()) {
					double d = Math.hypot(a.x() - s.x(), a.z() - s.z());
					helper.assertTrue(d > 11, t + " is " + d + " blocks from an enemy spot");
				}
			}
			int ground = terrain.ground(a.x(), a.z());
			helper.assertTrue(ground > Terrain.WATER_TOP, t + " stands on land (" + ground + ")");
			if (a.y() != Anchor.SURFACE) {
				helper.assertTrue(a.y() == ground + 1, t + " stands on the ground: y " + a.y() + ", ground " + ground);
			}
		}
		// Combat areas are on land.
		for (Anchor camp : camps) {
			helper.assertTrue(terrain.ground(camp.x(), camp.z()) > Terrain.WATER_TOP + 2, "camp on dry land");
		}
		// Size: about 150 x 133 of land (pier excluded).
		int minX = 0, maxX = 0, minZ = 0, maxZ = 0;
		for (int x = -100; x <= 100; x++) {
			for (int z = -100; z <= 100; z++) {
				if (terrain.ground(x, z) > Terrain.WATER_TOP) {
					minX = Math.min(minX, x);
					maxX = Math.max(maxX, x);
					minZ = Math.min(minZ, z);
					maxZ = Math.max(maxZ, z);
				}
			}
		}
		int w = maxX - minX + 1, d = maxZ - minZ + 1;
		helper.assertTrue(w >= 140 && w <= 165 && d >= 125 && d <= 145, "island size " + w + " x " + d);

	}
}
