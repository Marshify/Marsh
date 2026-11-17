package com.grandline.test;

import com.grandline.treasure.TreasureSpots;
import com.grandline.world.IslandRegistry;
import com.grandline.world.plan.Anchor;
import com.grandline.world.plan.IslandPlan;
import com.grandline.world.plan.IslandPlans;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** The rebuilt Crystal Fountain: old worlds keep the original; the new city has its spots, harbour and chests. */
public final class GrandLineCrystalFountainGameTests implements FabricGameTest {
	private static void setLayout(IslandRegistry registry, int layout) {
		try {
			var field = IslandRegistry.class.getDeclaredField("layout");
			field.setAccessible(true);
			field.setInt(registry, layout);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(e);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "crystal_plan")
	public void cityHarbourScrapyardAndOldWorlds(GameTestHelper helper) {
		IslandRegistry registry = IslandRegistry.get(helper.getLevel().getServer());
		int before = registry.layout();
		IslandPlan legacy = IslandPlans.legacy("Crystal Fountain");
		setLayout(registry, 13);
		helper.assertTrue(legacy != null && IslandPlans.get("Crystal Fountain") == legacy, "a layout-13 world keeps the original Crystal Fountain");
		helper.assertTrue(TreasureSpots.legacy("Crystal Fountain").size() == 4, "and its chest spots");
		setLayout(registry, IslandRegistry.CURRENT_LAYOUT);
		IslandPlan plan = IslandPlans.get("Crystal Fountain");
		helper.assertTrue(plan != legacy && IslandRegistry.CURRENT_LAYOUT >= 14, "new worlds get the rebuilt city");
		helper.assertTrue(plan.extent() >= 280, "the plan reaches the pier ends and the scrapyard");
		Anchor arrival = plan.anchors().stream().filter(a -> a.type() == Anchor.Type.ARRIVAL).findFirst().orElseThrow();
		Anchor quest = plan.anchors().stream().filter(a -> a.type() == Anchor.Type.QUEST).findFirst().orElseThrow();
		Anchor boss = plan.anchors().stream().filter(a -> a.type() == Anchor.Type.BOSS_ARENA).findFirst().orElseThrow();
		helper.assertTrue(arrival.z() > 160 && arrival.y() == 66, "players arrive on the harbour plaza");
		helper.assertTrue(quest.z() > 130 && quest.y() == 73, "the quest giver waits inside the gate");
		helper.assertTrue(boss.x() > 160, "the boss fights in the scrapyard");
		helper.assertTrue(plan.anchors().stream().filter(a -> a.type() == Anchor.Type.ENEMY_CAMP).count() == 2, "two camps");
		var spots = TreasureSpots.forIsland("Crystal Fountain");
		helper.assertTrue(spots.size() == 4 && !TreasureSpots.NONE_BY_DESIGN.contains("Crystal Fountain"), "four chests");
		setLayout(registry, before);
		helper.succeed();
	}
}
