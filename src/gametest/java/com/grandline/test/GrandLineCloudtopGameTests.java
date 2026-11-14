package com.grandline.test;

import com.grandline.world.FirstSeaProgression;
import com.grandline.world.IslandRegistry;
import com.grandline.world.plan.Anchor;
import com.grandline.world.plan.CloudtopAccess;
import com.grandline.world.plan.IslandPlan;
import com.grandline.world.plan.IslandPlans;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

import java.util.List;

/** The rebuilt Cloudtop Isles: old worlds keep the old island; both regions have their spots; heights fit the world. */
public final class GrandLineCloudtopGameTests implements FabricGameTest {
	private static void setLayout(IslandRegistry registry, int layout) {
		try {
			var field = IslandRegistry.class.getDeclaredField("layout");
			field.setAccessible(true);
			field.setInt(registry, layout);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(e);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "cloudtop_plan")
	public void bothRegionsHaveTheirOwnSpotsAndOldWorldsKeepTheOriginal(GameTestHelper helper) {
		IslandRegistry registry = IslandRegistry.get(helper.getLevel().getServer());
		int before = registry.layout();
		IslandPlan legacy = IslandPlans.legacy("Cloudtop Isles");
		setLayout(registry, 6);
		helper.assertTrue(legacy != null && IslandPlans.get("Cloudtop Isles") == legacy, "a layout-6 world keeps the original Cloudtop");
		helper.assertTrue(legacy.anchors().stream().anyMatch(a -> a.type() == Anchor.Type.QUEST && a.region() == FirstSeaProgression.UPPER),
				"the original island has its upper region too (since 0.30.0)");
		setLayout(registry, IslandRegistry.CURRENT_LAYOUT);
		IslandPlan plan = IslandPlans.get("Cloudtop Isles");
		helper.assertTrue(plan != legacy && IslandRegistry.CURRENT_LAYOUT >= 7, "new worlds get the rebuild");
		for (int region : new int[]{FirstSeaProgression.LOWER, FirstSeaProgression.UPPER}) {
			for (Anchor.Type t : List.of(Anchor.Type.QUEST, Anchor.Type.ENEMY_CAMP, Anchor.Type.BOSS_ARENA)) {
				long n = plan.anchors().stream().filter(a -> a.type() == t && a.region() == region).count();
				helper.assertTrue(n >= 1, "region " + region + " has a " + t);
			}
			long camps = plan.anchors().stream().filter(a -> a.type() == Anchor.Type.ENEMY_CAMP && a.region() == region).count();
			helper.assertTrue(camps == 2, "region " + region + " has a basic and a stronger camp: " + camps);
		}
		Anchor lowerQuest = plan.anchors().stream().filter(a -> a.type() == Anchor.Type.QUEST && a.region() == 0).findFirst().orElseThrow();
		Anchor upperQuest = plan.anchors().stream().filter(a -> a.type() == Anchor.Type.QUEST && a.region() == 1).findFirst().orElseThrow();
		helper.assertTrue(upperQuest.y() > lowerQuest.y() + 60, "the upper quest giver is high above the lower one");
		// Everything fits inside the world's build height (the overworld builds to y 319).
		for (Anchor a : plan.anchors()) {
			helper.assertTrue(a.y() == Anchor.SURFACE || (a.y() > 60 && a.y() < 300), a + " is inside the build height");
		}
		int[][] portals = CloudtopAccess.portals();
		helper.assertTrue(portals[1][1] > portals[0][1] + 60 && portals[3][1] < portals[2][1] - 60, "the portals connect the gate and the sanctuary");
		setLayout(registry, before);
		helper.succeed();
	}
}
