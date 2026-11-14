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

/** The rebuilt Stonewall Prison: old worlds keep the original; a fortress island with the arena, camps and NPCs in place. */
public final class GrandLineStonewallGameTests implements FabricGameTest {
	private static void setLayout(IslandRegistry registry, int layout) {
		try {
			var field = IslandRegistry.class.getDeclaredField("layout");
			field.setAccessible(true);
			field.setInt(registry, layout);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(e);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "stonewall_plan")
	public void fortressIslandWithArenaCampsAndGate(GameTestHelper helper) {
		IslandRegistry registry = IslandRegistry.get(helper.getLevel().getServer());
		int before = registry.layout();
		IslandPlan legacy = IslandPlans.legacy("Stonewall Prison");
		setLayout(registry, 8);
		helper.assertTrue(legacy != null && IslandPlans.get("Stonewall Prison") == legacy, "a layout-8 world keeps the original prison");
		setLayout(registry, IslandRegistry.CURRENT_LAYOUT);
		IslandPlan plan = IslandPlans.get("Stonewall Prison");
		helper.assertTrue(plan != legacy && IslandRegistry.CURRENT_LAYOUT >= 9, "new worlds get the rebuild");
		Terrain t = plan.terrain();
		helper.assertTrue(t.ground(0, -10) == 66 && t.ground(0, 48) == 66, "the fortress ground and the forecourt are level");
		for (int[] p : new int[][]{{-56, -10}, {56, -10}, {0, -62}}) {
			helper.assertTrue(t.ground(p[0], p[1]) > Terrain.WATER_TOP, "land runs round the walls at " + p[0] + "," + p[1]);
		}
		helper.assertTrue(t.ground(0, 100) < Terrain.WATER_TOP, "open sea beyond the pier");
		Anchor quest = plan.anchors().stream().filter(a -> a.type() == Anchor.Type.QUEST).findFirst().orElseThrow();
		Anchor home = plan.anchors().stream().filter(a -> a.type() == Anchor.Type.HOME).findFirst().orElseThrow();
		Anchor boss = plan.anchors().stream().filter(a -> a.type() == Anchor.Type.BOSS_ARENA).findFirst().orElseThrow();
		List<Anchor> camps = plan.anchors().stream().filter(a -> a.type() == Anchor.Type.ENEMY_CAMP).toList();
		helper.assertTrue(quest.z() > 33 && quest.y() == 67 && home.z() > 33 && home.y() == 67, "quest giver and innkeeper wait in the forecourt outside the gate");
		helper.assertTrue(boss.x() == 0 && boss.z() < -28 && boss.z() > -50 && boss.y() == 68, "quest bosses fight in the Warden's arena (north wing)");
		helper.assertTrue(camps.size() == 2 && camps.get(0).z() > -28 && camps.get(0).z() < 11 && camps.get(0).y() == 68,
				"the first (basic) camp is the courtyard");
		helper.assertTrue(camps.get(1).z() < -55, "the stronger camp patrols the yard outside the north wall");
		for (Anchor camp : camps) {
			helper.assertTrue(Math.hypot(quest.x() - camp.x(), quest.z() - camp.z()) >= camp.radius() + 20, "the quest giver is clear of " + camp);
		}
		setLayout(registry, before);
		helper.succeed();
	}
}
