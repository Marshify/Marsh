package com.grandline.test;

import com.grandline.GrandLineMod;
import com.grandline.world.plan.Anchor;
import com.grandline.world.plan.IslandPlan;
import com.grandline.world.plan.IslandPlans;
import com.grandline.world.plan.Terrain;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Verdant Wilds redesign: measures the real footprint from the terrain (land = ground above the water) and checks it
 * against the brief: main island ~112 x 90, west island ~60 x 52, a 14-22 block channel, anchors where they belong.
 */
public final class GrandLineVerdantWildsGameTests implements FabricGameTest {
	private static boolean land(Terrain t, int x, int z) {
		return t.ground(x, z) > Terrain.WATER_TOP;
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "verdant_layout")
	public void verdantWildsMatchesTheBrief(GameTestHelper helper) {
		// The redesign (this test server's world may predate it and so use the legacy plan, which is the point of layouts).
		IslandPlan plan = IslandPlans.all().stream().filter(p -> p.name().equals("Verdant Wilds")).findFirst().orElse(null);
		helper.assertTrue(plan != null && IslandPlans.legacy("Verdant Wilds") != null && IslandPlans.legacy("Verdant Wilds") != plan,
				"redesigned plan plus the legacy one for old worlds");
		Terrain t = plan.terrain();
		int split = -66; // between the islands
		int[] main = {Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE};
		int[] west = {Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE};
		for (int x = -160; x <= 80; x++) {
			for (int z = -80; z <= 80; z++) {
				if (!land(t, x, z)) {
					continue;
				}
				int[] box = x > split ? main : west;
				box[0] = Math.min(box[0], x);
				box[1] = Math.max(box[1], x);
				box[2] = Math.min(box[2], z);
				box[3] = Math.max(box[3], z);
			}
		}
		int mainW = main[1] - main[0] + 1, mainD = main[3] - main[2] + 1;
		int westW = west[1] - west[0] + 1, westD = west[3] - west[2] + 1;
		// Channel: open water between the west island's east shore and the main island's west shore, row by row.
		int minGap = Integer.MAX_VALUE, maxGap = 0, bridgeGap = -1;
		for (int z = Math.max(main[2], west[2]); z <= Math.min(main[3], west[3]); z++) {
			int westEdge = Integer.MIN_VALUE, mainEdge = Integer.MAX_VALUE;
			for (int x = -160; x <= split; x++) {
				if (land(t, x, z)) {
					westEdge = Math.max(westEdge, x);
				}
			}
			for (int x = split + 1; x <= 80; x++) {
				if (land(t, x, z)) {
					mainEdge = Math.min(mainEdge, x);
				}
			}
			if (westEdge == Integer.MIN_VALUE || mainEdge == Integer.MAX_VALUE) {
				continue;
			}
			int gap = mainEdge - westEdge - 1;
			minGap = Math.min(minGap, gap);
			maxGap = Math.max(maxGap, gap);
			if (z == 6) {
				bridgeGap = gap;
			}
		}
		GrandLineMod.LOGGER.info("Verdant Wilds measured: main {} x {} (x {}..{}, z {}..{}), west {} x {} (x {}..{}, z {}..{}), "
						+ "channel {}..{} blocks (at the bridge {}), highest ground {}",
				mainW, mainD, main[0], main[1], main[2], main[3], westW, westD, west[0], west[1], west[2], west[3],
				minGap, maxGap, bridgeGap, t.ground(0, -16));
		helper.assertTrue(mainW >= 104 && mainW <= 120 && mainD >= 82 && mainD <= 98, "main island ~112 x 90, got " + mainW + " x " + mainD);
		helper.assertTrue(westW >= 54 && westW <= 66 && westD >= 46 && westD <= 58, "west island ~60 x 52, got " + westW + " x " + westD);
		helper.assertTrue(minGap >= 14 && bridgeGap >= 14 && bridgeGap <= 22, "channel 14-22 wide, got " + minGap + ".." + maxGap
				+ " (bridge " + bridgeGap + ")");
		helper.assertTrue(t.ground(0, -16) == 70, "temple terrace at y 70");
		for (Anchor a : plan.anchors()) {
			if (a.type() == Anchor.Type.BOSS_ARENA) {
				helper.assertTrue(a.x() < split && land(t, a.x(), a.z()), "boss clearing is on the west island");
			}
			if (a.type() == Anchor.Type.QUEST || a.type() == Anchor.Type.ENEMY_CAMP) {
				helper.assertTrue(a.x() > split && land(t, a.x(), a.z()), a.type() + " is on the main island");
			}
		}
		helper.succeed();
	}
}
