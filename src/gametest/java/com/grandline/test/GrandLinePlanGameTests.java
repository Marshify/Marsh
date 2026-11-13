package com.grandline.test;

import com.grandline.world.SeaLayout;
import com.grandline.world.plan.Anchor;
import com.grandline.world.plan.IslandPlan;
import com.grandline.world.plan.IslandPlans;
import com.grandline.world.plan.Terrain;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Every First Sea island is planned, and each plan is playable: anchors on the island, NPC spots on land, a dock. */
public final class GrandLinePlanGameTests implements FabricGameTest {
	@GameTest(template = EMPTY_STRUCTURE, batch = "plans")
	public void everyFirstSeaIslandHasAPlayablePlan(GameTestHelper helper) {
		int planned = 0;
		for (SeaLayout.Spec spec : SeaLayout.all()) {
			if (spec.sea() != 1) {
				continue;
			}
			IslandPlan plan = IslandPlans.get(spec.name());
			helper.assertTrue(plan != null, spec.name() + " has a plan");
			planned++;
			Terrain terrain = plan.terrain();
			helper.assertTrue(terrain.ground(0, 0) >= Terrain.WATER_TOP, spec.name() + " centre is land");
			boolean quest = false;
			boolean dock = false;
			boolean navigator = false;
			for (Anchor a : plan.anchors()) {
				helper.assertTrue(a.x() * a.x() + a.z() * a.z() <= plan.extent() * plan.extent(),
						spec.name() + " " + a.type() + " anchor lies inside the generated area");
				switch (a.type()) {
					case QUEST, FISHERMAN, NAVIGATOR, FRUIT_DEALER, GUNSMITH -> {
						if (a.y() == Anchor.SURFACE) {
							// NPCs on the surface must stand on land or a pier, never in open water.
							boolean land = terrain.ground(a.x(), a.z()) >= Terrain.WATER_TOP;
							helper.assertTrue(land || a.type() == Anchor.Type.FISHERMAN || a.type() == Anchor.Type.NAVIGATOR,
									spec.name() + " " + a.type() + " stands on land");
						}
						// Starter islands are safe zones (no enemies): they have a gun shop instead of quests.
						quest |= a.type() == Anchor.Type.QUEST || (spec.starter() && a.type() == Anchor.Type.GUNSMITH);
						navigator |= a.type() == Anchor.Type.NAVIGATOR;
					}
					case DOCK -> dock = true;
					default -> {
					}
				}
			}
			helper.assertTrue(quest, spec.name() + " has a quest giver");
			helper.assertTrue(dock, spec.name() + " has a dock");
			helper.assertTrue(!spec.starter() || navigator, spec.name() + " (starter) has a navigator");
		}
		helper.assertTrue(planned == 14, "planned First Sea islands: " + planned);
		helper.succeed();
	}
}
