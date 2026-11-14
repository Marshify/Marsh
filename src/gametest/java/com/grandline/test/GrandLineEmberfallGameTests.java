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

/** The rebuilt Emberfall Isle: old worlds keep the original; the layout's order (village, volcano, plateau) and spots. */
public final class GrandLineEmberfallGameTests implements FabricGameTest {
	private static void setLayout(IslandRegistry registry, int layout) {
		try {
			var field = IslandRegistry.class.getDeclaredField("layout");
			field.setAccessible(true);
			field.setInt(registry, layout);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(e);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "emberfall_plan")
	public void villageInFrontVolcanoInTheMiddlePlateauBehind(GameTestHelper helper) {
		IslandRegistry registry = IslandRegistry.get(helper.getLevel().getServer());
		int before = registry.layout();
		IslandPlan legacy = IslandPlans.legacy("Emberfall Isle");
		setLayout(registry, 7);
		helper.assertTrue(legacy != null && IslandPlans.get("Emberfall Isle") == legacy, "a layout-7 world keeps the original Emberfall");
		setLayout(registry, IslandRegistry.CURRENT_LAYOUT);
		IslandPlan plan = IslandPlans.get("Emberfall Isle");
		Terrain t = plan.terrain();
		int summit = t.ground(0, -18);
		helper.assertTrue(summit - 66 >= 50 && summit - 66 <= 72, "the volcano rises 50-72 above the plain: " + (summit - 66));
		helper.assertTrue(t.ground(0, -74) == 78, "the plateau behind the volcano is 12 above the plain: " + t.ground(0, -74));
		helper.assertTrue(t.ground(0, 58) == 66 && t.ground(20, 30) == 66, "village and soldiers' ground are flat at the plain");
		Anchor quest = plan.anchors().stream().filter(a -> a.type() == Anchor.Type.QUEST).findFirst().orElseThrow();
		List<Anchor> camps = plan.anchors().stream().filter(a -> a.type() == Anchor.Type.ENEMY_CAMP).toList();
		helper.assertTrue(camps.size() == 2 && camps.get(0).z() > 0 && camps.get(1).z() < -60, "soldiers in front, spies on the plateau behind");
		helper.assertTrue(quest.z() > camps.get(0).z() && quest.z() < 60 && quest.y() == t.ground(quest.x(), quest.z()) + 1,
				"the quest giver stands at the mayor's house, between the village and the soldiers");
		for (Anchor camp : camps) {
			helper.assertTrue(Math.hypot(quest.x() - camp.x(), quest.z() - camp.z()) >= camp.radius() + 18, "the quest giver is clear of " + camp);
		}
		Anchor boss = plan.anchors().stream().filter(a -> a.type() == Anchor.Type.BOSS_ARENA).findFirst().orElseThrow();
		helper.assertTrue(boss.x() == 0 && boss.z() == -18 && boss.y() < summit - 30, "quest bosses fight in the chamber inside the volcano");
		setLayout(registry, before);
		helper.succeed();
	}
}
