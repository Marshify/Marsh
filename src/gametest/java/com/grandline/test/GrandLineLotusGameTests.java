package com.grandline.test;

import com.grandline.player.rules.OceanWeakness;
import com.grandline.treasure.TreasureSpots;
import com.grandline.world.Island;
import com.grandline.world.IslandRegistry;
import com.grandline.world.LotusWhirlpools;
import com.grandline.world.plan.IslandPlan;
import com.grandline.world.plan.IslandPlans;
import com.grandline.world.plan.LotusPalace;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/** The rebuilt Lotus Palace: old worlds keep the original; the linked whirlpools carry players both ways, each on their own. */
public final class GrandLineLotusGameTests implements FabricGameTest {
	private static void setLayout(IslandRegistry registry, int layout) {
		try {
			var field = IslandRegistry.class.getDeclaredField("layout");
			field.setAccessible(true);
			field.setInt(registry, layout);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(e);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "lotus_plan")
	public void palaceAndOldWorlds(GameTestHelper helper) {
		IslandRegistry registry = IslandRegistry.get(helper.getLevel().getServer());
		int before = registry.layout();
		IslandPlan legacy = IslandPlans.legacy("Lotus Palace");
		setLayout(registry, 16);
		helper.assertTrue(legacy != null && IslandPlans.get("Lotus Palace") == legacy, "a layout-16 world keeps the original Lotus Palace");
		helper.assertTrue(TreasureSpots.legacy("Lotus Palace").size() == 4, "and its chest spots");
		setLayout(registry, 17);
		IslandPlan v17 = IslandPlans.get("Lotus Palace");
		helper.assertTrue(v17 instanceof LotusPalace && v17.getClass() != LotusPalace.class,
				"a layout-17 world keeps the palace without the cavern overhaul (no half-old, half-new cavern)");
		setLayout(registry, IslandRegistry.CURRENT_LAYOUT);
		IslandPlan plan = IslandPlans.get("Lotus Palace");
		helper.assertTrue(plan.getClass() == LotusPalace.class && IslandRegistry.CURRENT_LAYOUT >= 18, "new worlds get the overhauled cavern");
		helper.assertTrue(plan instanceof LotusPalace && IslandRegistry.CURRENT_LAYOUT >= 17, "new worlds get the sunken palace");
		helper.assertTrue(plan.extent() >= 220, "the plan reaches the whole palace pocket");
		var t = plan.terrain();
		helper.assertTrue(t.ground(0, -30) > 62 && t.ground(-26, 18) > 62 && t.ground(27, 19) > 62, "three islets above the sea");
		helper.assertTrue(t.ground(0, 0) < 62 && t.ground(0, -12) < 62 && t.ground(-12, 6) < 62, "open water round the whirlpool");
		helper.assertTrue(TreasureSpots.forIsland("Lotus Palace").size() == 4, "four chests");
		setLayout(registry, before);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "lotus_portals", timeoutTicks = 200)
	public void whirlpoolsCarryPlayersBothWaysEachOnTheirOwn(GameTestHelper helper) {
		var server = helper.getLevel().getServer();
		IslandRegistry registry = IslandRegistry.get(server);
		int before = registry.layout();
		setLayout(registry, IslandRegistry.CURRENT_LAYOUT);
		boolean added = false;
		if (registry.all().stream().noneMatch(i -> i.name().equals("Lotus Palace"))) {
			var o = helper.absolutePos(net.minecraft.core.BlockPos.ZERO);
			registry.add(new Island(990120, o.getX(), o.getZ() + 400, 36, 450, "Lotus Palace", false, false, "palace"));
			added = true;
		}
		Vec3[] c = LotusWhirlpools.centres(server);
		helper.assertTrue(c != null, "the rebuilt island has two whirlpools");
		var level = helper.getLevel();
		ServerPlayer a = helper.makeMockServerPlayerInLevel();
		ServerPlayer b = helper.makeMockServerPlayerInLevel();
		LotusWhirlpools.reset(a);
		LotusWhirlpools.reset(b);
		a.teleportTo(level, c[0].x, c[0].y, c[0].z, 0, 0);
		b.teleportTo(level, c[0].x + 7, c[0].y, c[0].z, 0, 0);
		helper.assertTrue(OceanWeakness.multiplier(b) == 0, "no sea damage while standing in the surface whirlpool");
		boolean finalAdded = added;
		helper.runAfterDelay(12, () -> {
			helper.assertTrue(a.position().distanceTo(c[1]) < 1.5 && LotusWhirlpools.lock(a) == 1, "A went down into the palace whirlpool");
			helper.assertTrue(b.position().distanceTo(c[0]) > 5, "B (outside the trigger) stayed at the surface");
			b.teleportTo(level, c[0].x, c[0].y, c[0].z, 0, 0);
			helper.runAfterDelay(12, () -> {
				helper.assertTrue(b.position().distanceTo(c[1]) < 1.5, "B followed on their own");
				helper.assertTrue(a.position().distanceTo(c[1]) < 1.5, "A, still standing where they arrived, was not sent straight back");
				a.teleportTo(level, c[1].x + 6, c[1].y, c[1].z, 0, 0);
				helper.runAfterDelay(4, () -> {
					a.teleportTo(level, c[1].x, c[1].y, c[1].z, 0, 0);
					helper.runAfterDelay(50, () -> {
						helper.assertTrue(a.position().distanceTo(c[0]) < 1.5, "A stepped out and back in: carried up to the surface");
						helper.assertTrue(b.position().distanceTo(c[1]) < 1.5, "B is still down in the palace");
						a.discard();
						b.discard();
						if (finalAdded) {
							registry.removeIf(i -> i.id() == 990120);
						}
						setLayout(registry, before);
						helper.succeed();
					});
				});
			});
		});
	}
}
