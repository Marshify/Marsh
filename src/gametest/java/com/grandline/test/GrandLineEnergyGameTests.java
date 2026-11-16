package com.grandline.test;

import com.grandline.combat.EnergyService;
import com.grandline.player.PlayerProfileService;
import com.grandline.player.StatType;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/** Max energy = 100 + 5 per Melee point; fixed move costs; allocation keeps current energy; regeneration caps at max. */
public final class GrandLineEnergyGameTests implements FabricGameTest {
	@GameTest(template = EMPTY_STRUCTURE, batch = "energy")
	public void maxEnergyFollowsMeleeAndCostsAreFixed(GameTestHelper helper) {
		int[][] table = {{0, 100}, {1, 105}, {100, 600}, {500, 2600}, {1000, 5100}, {2000, 10100}, {2550, 12850}};
		for (int[] row : table) {
			helper.assertTrue(EnergyService.max(row[0]) == row[1], "Melee " + row[0] + " gives " + row[1] + ": " + EnergyService.max(row[0]));
		}
		ServerPlayer p = helper.makeMockServerPlayerInLevel();
		GrandLineRaceGameTests.neutral(p);
		PlayerProfileService.update(p, pr -> pr.withLevel(200).withUnallocatedStatPoints(200)
				.withStats(pr.stats().with(StatType.MELEE, 100)));
		helper.assertTrue(EnergyService.max(p) == 600, "100 Melee at level 200: 600 max (level adds nothing): " + EnergyService.max(p));
		EnergyService.refill(p);
		// A 7% move costs a fixed 7 energy, not 7% of 600.
		helper.assertTrue(Math.abs(EnergyService.cost(p, 0.07) - 7.0) < 1e-9, "fixed cost: " + EnergyService.cost(p, 0.07));
		helper.assertTrue(EnergyService.trySpend(p, 0.07) && Math.abs(EnergyService.current(p) - 593) < 1e-6, "spent 7");
		// Allocating 10 more Melee: max 650, current energy unchanged.
		double before = EnergyService.current(p);
		PlayerProfileService.allocateStat(p, StatType.MELEE, 10);
		helper.assertTrue(EnergyService.max(p) == 650 && Math.abs(EnergyService.current(p) - before) < 1e-6,
				"allocation raises max to 650 and keeps current: " + EnergyService.max(p) + " / " + EnergyService.current(p));
		// Not enough energy: nothing is spent.
		PlayerProfileService.update(p, pr -> pr.withStats(pr.stats().with(StatType.MELEE, 0)));
		EnergyService.clampToMax(p);
		helper.assertTrue(EnergyService.current(p) <= 100, "a lower max clamps current energy");
		EnergyService.restore(p, -1); // no-op guard
		while (EnergyService.trySpend(p, 0.3)) {
			// drain
		}
		double left = EnergyService.current(p);
		helper.assertTrue(left >= 0 && left < 30 && !EnergyService.trySpend(p, 0.3) && EnergyService.current(p) == left,
				"too little energy: refused, nothing spent, never negative: " + left);
		helper.runAfterDelay(60, () -> {
			helper.assertTrue(EnergyService.current(p) > left && EnergyService.current(p) <= EnergyService.max(p), "regenerates, capped at max");
			helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
			helper.succeed();
		});
	}
}
