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
		try {
			IslandPlan legacy = IslandPlans.legacy("Cloudtop Isles");
			setLayout(registry, 6);
			helper.assertTrue(legacy != null && IslandPlans.get("Cloudtop Isles") == legacy, "a layout-6 world keeps the original Cloudtop");
			helper.assertTrue(legacy.anchors().stream().anyMatch(a -> a.type() == Anchor.Type.QUEST && a.region() == FirstSeaProgression.UPPER),
					"the original island has its upper region too (since 0.30.0)");
			IslandPlan first = IslandPlans.interim("Cloudtop Isles", 7), second = IslandPlans.interim("Cloudtop Isles", 12);
			helper.assertTrue(first != null && second != null && first != second, "two kept rebuilds");
			for (int layout = 7; layout <= 12; layout++) {
				setLayout(registry, layout);
				IslandPlan kept = layout < 12 ? first : second;
				helper.assertTrue(IslandPlans.get("Cloudtop Isles") == kept && IslandPlans.usesInterimPlan("Cloudtop Isles")
						&& !IslandPlans.usesCurrentPlan("Cloudtop Isles"), "a layout-" + layout + " world keeps " + kept.getClass().getSimpleName());
				helper.assertTrue(CloudtopAccess.portals(kept) != null, "a kept rebuild keeps its portals");
				helper.assertTrue(com.grandline.treasure.TreasureSpots.interim(kept).size() == 4, "and its chest spots");
			}
			setLayout(registry, IslandRegistry.CURRENT_LAYOUT);
			IslandPlan plan = IslandPlans.get("Cloudtop Isles");
			helper.assertTrue(plan != legacy && plan != first && plan != second && IslandRegistry.CURRENT_LAYOUT >= 13
					&& IslandPlans.usesCurrentPlan("Cloudtop Isles"), "new worlds get the Upper Cloudtop Isles");
			helper.assertTrue(CloudtopAccess.portals(legacy) == null, "the original island has no portals");
			for (int region : new int[]{FirstSeaProgression.LOWER, FirstSeaProgression.UPPER}) {
				for (Anchor.Type t : List.of(Anchor.Type.QUEST, Anchor.Type.ENEMY_CAMP, Anchor.Type.BOSS_ARENA)) {
					long n = plan.anchors().stream().filter(a -> a.type() == t && a.region() == region).count();
					// The lower region has no boss (its population: IslandRosters.CLOUDTOP); the upper keeps its own.
					helper.assertTrue(t == Anchor.Type.BOSS_ARENA && region == FirstSeaProgression.LOWER ? n == 0 : n >= 1, "region " + region + " has " + n + " " + t);
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
			int[][] portals = CloudtopAccess.portals(plan);
			helper.assertTrue(portals[1][1] > portals[0][1] + 60 && portals[3][1] < portals[2][1] - 60, "the sky arches connect Azure Rise and the citadel");
	} finally {
			setLayout(registry, before); // also when a check fails
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "cloudtop_plan")
	public void upperCloudtopSpotsArchesAndChests(GameTestHelper helper) {
		IslandRegistry registry = IslandRegistry.get(helper.getLevel().getServer());
		int before = registry.layout();
		try {
			setLayout(registry, IslandRegistry.CURRENT_LAYOUT);
			IslandPlan plan = IslandPlans.get("Cloudtop Isles");
			List<Anchor> anchors = plan.anchors();
			Anchor lowerQuest = anchors.stream().filter(a -> a.type() == Anchor.Type.QUEST && a.region() == FirstSeaProgression.LOWER).findFirst().orElseThrow();
			Anchor upperBoss = anchors.stream().filter(a -> a.type() == Anchor.Type.BOSS_ARENA && a.region() == FirstSeaProgression.UPPER).findFirst().orElseThrow();
			helper.assertTrue(lowerQuest.y() == 101, "the lower quest giver stands in Bloomhold (ground 100)");
			helper.assertTrue(upperBoss.y() == 204 && upperBoss.x() > 60, "the upper boss is in the Grand Sanctuary's courtyard (ground 204)");
			helper.assertTrue(anchors.stream().anyMatch(a -> a.type() == Anchor.Type.ARRIVAL) && anchors.stream().anyMatch(a -> a.type() == Anchor.Type.DOCK)
					&& anchors.stream().anyMatch(a -> a.type() == Anchor.Type.FISHERMAN) && anchors.stream().anyMatch(a -> a.type() == Anchor.Type.HOME),
					"arrival, dock, fisherman and home are all there");
			// The sky arches: each arrival is clear of the other arch (no bouncing back), on the right island.
			int[][] p = CloudtopAccess.portals(plan);
			for (int k = 0; k < 2; k++) {
				int[] arrive = p[k * 2 + 1], back = p[(k * 2 + 2) % 4];
				double d = Math.hypot(arrive[0] - back[0], arrive[2] - back[2]);
				helper.assertTrue(d > 4, "arriving " + (k == 0 ? "up" : "down") + " lands clear of the arch back: " + d);
			}
			helper.assertTrue(p[0][1] == 115 && p[1][1] == 201, "the arch up stands on Azure Rise and lands on the Ancient Tree landmass");
			// Chests: on the new islands, the legendary up in the citadel.
			var spots = com.grandline.treasure.TreasureSpots.forIsland("Cloudtop Isles");
			helper.assertTrue(spots.size() == 5, "five chests: " + spots.size());
			helper.assertTrue(spots.stream().anyMatch(s -> s.tier() == com.grandline.treasure.TreasureTier.LEGENDARY && s.y() > 230),
					"the legendary is on the temple's summit platform");
	} finally {
			setLayout(registry, before); // also when a check fails
		}
		helper.succeed();
	}
}
