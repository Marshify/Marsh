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
		IslandPlan v9 = IslandPlans.interim("Stonewall Prison", 9);
		setLayout(registry, 12);
		helper.assertTrue(v9 != null && IslandPlans.get("Stonewall Prison") == v9 && IslandPlans.usesInterimPlan("Stonewall Prison"),
				"a layout 9-14 world keeps the 0.33.0 prison");
		helper.assertTrue(com.grandline.treasure.TreasureSpots.interim(v9).size() == 4, "and its chest spots");
		setLayout(registry, IslandRegistry.CURRENT_LAYOUT);
		IslandPlan plan = IslandPlans.get("Stonewall Prison");
		helper.assertTrue(plan != legacy && plan != v9 && IslandRegistry.CURRENT_LAYOUT >= 15, "new worlds get the round fortress");
		Anchor quest = plan.anchors().stream().filter(a -> a.type() == Anchor.Type.QUEST).findFirst().orElseThrow();
		Anchor home = plan.anchors().stream().filter(a -> a.type() == Anchor.Type.HOME).findFirst().orElseThrow();
		Anchor boss = plan.anchors().stream().filter(a -> a.type() == Anchor.Type.BOSS_ARENA).findFirst().orElseThrow();
		List<Anchor> camps = plan.anchors().stream().filter(a -> a.type() == Anchor.Type.ENEMY_CAMP).toList();
		helper.assertTrue(quest.z() > 30 && quest.y() == 67, "the Jail Keeper waits inside the gate");
		helper.assertTrue(home.z() > 10 && home.y() == 67, "the Head Jailer stands by the tower's door");
		helper.assertTrue(boss.y() == 67, "the bosses hold the central courtyard and tower");
		helper.assertTrue(camps.size() == 2, "two inmate areas: the outer enclosure's west and east");
		for (Anchor camp : camps) {
			helper.assertTrue(Math.hypot(quest.x() - camp.x(), quest.z() - camp.z()) >= camp.radius() + 20, "the quest giver is clear of " + camp);
		}
		helper.assertTrue(plan.extent() >= 74 && plan.extent() <= 90, "a compact fortress (reach " + plan.extent() + ")");
		helper.assertTrue(plan.terrain().ground(0, 30) == 66 && plan.terrain().ground(-30, -10) == 66,
				"the fortress floor is land in the terrain (keepers and spawns find it)");
		helper.assertTrue(plan.terrain().ground(0, 70) < com.grandline.world.plan.Terrain.WATER_TOP, "open sea by the pier");
		setLayout(registry, before);
		helper.succeed();
	}
}
