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

/** The rebuilt Gladiator's Ring: old worlds keep the old ring, the plan keeps its gameplay spots, arrivals are safe. */
public final class GrandLineGladiatorGameTests implements FabricGameTest {
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

	@GameTest(template = EMPTY_STRUCTURE, batch = "gladiator_plan")
	public void olderWorldsKeepTheOriginalIsland(GameTestHelper helper) {
		IslandRegistry registry = IslandRegistry.get(helper.getLevel().getServer());
		int before = registry.layout();
		IslandPlan legacy = IslandPlans.legacy("Gladiator's Ring");
		helper.assertTrue(legacy != null, "the original plan is kept");
		setLayout(registry, 5);
		helper.assertTrue(IslandPlans.get("Gladiator's Ring") == legacy, "a layout-5 world generates the original ring");
		setLayout(registry, IslandRegistry.CURRENT_LAYOUT);
		IslandPlan rebuilt = IslandPlans.get("Gladiator's Ring");
		helper.assertTrue(rebuilt != legacy && IslandRegistry.CURRENT_LAYOUT >= 6, "new worlds get the rebuild");
		helper.assertTrue(IslandPlans.get("Millbrook Village") == IslandPlans.get("Millbrook Village"), "other islands unaffected");
		setLayout(registry, before);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "gladiator_plan")
	public void planKeepsEveryGameplaySpotAndKeepsArrivalsSafe(GameTestHelper helper) {
		IslandRegistry registry = IslandRegistry.get(helper.getLevel().getServer());
		int before = registry.layout();
		setLayout(registry, IslandRegistry.CURRENT_LAYOUT);
		IslandPlan plan = IslandPlans.get("Gladiator's Ring");
		for (Anchor.Type t : List.of(Anchor.Type.QUEST, Anchor.Type.HOME, Anchor.Type.FISHERMAN, Anchor.Type.DOCK, Anchor.Type.ARRIVAL,
				Anchor.Type.BOSS_ARENA, Anchor.Type.TREASURE)) {
			helper.assertTrue(plan.anchors().stream().anyMatch(a -> a.type() == t), "has a " + t + " spot");
		}
		List<Anchor> camps = plan.anchors().stream().filter(a -> a.type() == Anchor.Type.ENEMY_CAMP).toList();
		helper.assertTrue(camps.size() == 2, "two combat areas: " + camps.size());
		Terrain terrain = plan.terrain();
		// Safe spots: no camp (leash = radius + 6, plus a margin) reaches the quest giver, innkeeper or arrival plaza.
		for (Anchor.Type t : List.of(Anchor.Type.QUEST, Anchor.Type.HOME, Anchor.Type.ARRIVAL)) {
			Anchor a = one(plan, t);
			for (Anchor camp : camps) {
				double d = Math.hypot(a.x() - camp.x(), a.z() - camp.z());
				helper.assertTrue(d >= camp.radius() + 12, t + " is " + d + " blocks from a camp");
			}
			int ground = terrain.ground(a.x(), a.z());
			helper.assertTrue(ground > Terrain.WATER_TOP, t + " stands on land (" + ground + ")");
			if (a.y() != Anchor.SURFACE) {
				helper.assertTrue(a.y() == ground + 1, t + " stands on the ground: y " + a.y() + ", ground " + ground);
			}
		}
		// Camps are on land, on level ground.
		for (Anchor camp : camps) {
			int g = terrain.ground(camp.x(), camp.z());
			helper.assertTrue(g > Terrain.WATER_TOP + 2, "camp on dry land");
			for (int[] o : new int[][]{{3, 0}, {-3, 0}, {0, 3}, {0, -3}}) {
				helper.assertTrue(Math.abs(terrain.ground(camp.x() + o[0], camp.z() + o[1]) - g) <= 1, "camp ground is level");
			}
		}
		// Size: about 165 x 150 of land (pier excluded).
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
		helper.assertTrue(w >= 150 && w <= 175 && d >= 135 && d <= 160, "island size " + w + " x " + d);
		setLayout(registry, before);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "gladiator_plan")
	public void bossesFightOnTheArenaFloorAndCampsStayOutside(GameTestHelper helper) {
		IslandRegistry registry = IslandRegistry.get(helper.getLevel().getServer());
		int before = registry.layout();
		setLayout(registry, IslandRegistry.CURRENT_LAYOUT);
		IslandPlan plan = IslandPlans.get("Gladiator's Ring");
		Anchor boss = one(plan, Anchor.Type.BOSS_ARENA);
		helper.assertTrue(boss.y() == 70, "the boss stands on the arena floor (y 70): " + boss.y());
		// Camps are outside the colosseum (92 x 80 around (0, -8)).
		for (Anchor camp : plan.anchors().stream().filter(a -> a.type() == Anchor.Type.ENEMY_CAMP).toList()) {
			double px = (camp.x() - 0) / 46.0, pz = (camp.z() + 8) / 40.0;
			helper.assertTrue(px * px + pz * pz > 1.2, "camp at " + camp.x() + "," + camp.z() + " is outside the colosseum");
		}
		setLayout(registry, before);
		helper.succeed();
	}
}
