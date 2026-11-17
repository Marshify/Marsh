package com.grandline.test;

import com.grandline.treasure.TreasureSpots;
import com.grandline.world.IslandRegistry;
import com.grandline.world.plan.Anchor;
import com.grandline.world.plan.IslandPlan;
import com.grandline.world.plan.IslandPlans;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** The rebuilt Redbrick Port: old worlds keep the original; the naval fortress has its plaza, anchors and chests. */
public final class GrandLineRedbrickGameTests implements FabricGameTest {
	private static void setLayout(IslandRegistry registry, int layout) {
		try {
			var field = IslandRegistry.class.getDeclaredField("layout");
			field.setAccessible(true);
			field.setInt(registry, layout);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(e);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "redbrick_plan")
	public void fortressAndOldWorlds(GameTestHelper helper) {
		IslandRegistry registry = IslandRegistry.get(helper.getLevel().getServer());
		int before = registry.layout();
		IslandPlan legacy = IslandPlans.legacy("Redbrick Port");
		setLayout(registry, 15);
		helper.assertTrue(legacy != null && IslandPlans.get("Redbrick Port") == legacy, "a layout-15 world keeps the original Redbrick Port");
		helper.assertTrue(TreasureSpots.legacy("Redbrick Port").size() == 4, "and its chest spots");
		setLayout(registry, IslandRegistry.CURRENT_LAYOUT);
		IslandPlan plan = IslandPlans.get("Redbrick Port");
		helper.assertTrue(plan != legacy && IslandRegistry.CURRENT_LAYOUT >= 16, "new worlds get the naval fortress");
		helper.assertTrue(plan.extent() >= 70, "the plan reaches the tower islets and the harbour platforms");
		Anchor arrival = plan.anchors().stream().filter(a -> a.type() == Anchor.Type.ARRIVAL).findFirst().orElseThrow();
		helper.assertTrue(arrival.z() > 30 && arrival.y() == 66, "Marines arrive on the harbour apron, south of the headquarters");
		helper.assertTrue(plan.anchors().stream().filter(a -> a.type() == Anchor.Type.ENEMY_CAMP).count() == 2, "two camps");
		helper.assertTrue(plan.anchors().stream().anyMatch(a -> a.type() == Anchor.Type.QUEST), "a quest giver");
		var t = plan.terrain();
		helper.assertTrue(t.ground(0, 30) == 65 && t.ground(-40, 0) == 65 && t.ground(40, 0) == 65,
				"the plaza and both wings are land at the plaza height");
		helper.assertTrue(t.ground(-15, 33) < 62 && t.ground(15, 33) < 62, "the recessed pools beside the apron stay water");
		helper.assertTrue(t.ground(-57, 32) > 62 && t.ground(57, 32) > 62 && t.ground(-46, 30) < 62,
				"the southern towers stand on their own islets, apart from the plaza");
		helper.assertTrue(TreasureSpots.forIsland("Redbrick Port").size() == 4, "four chests");
		setLayout(registry, before);
		helper.succeed();
	}
}
