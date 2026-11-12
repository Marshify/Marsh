package com.grandline.player;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class StatRulesTest {
	private static final StatRules RULES = new StatRules(10, 0.5, 4.0, 0.25, 0.1, 2.0);

	private static PlayerProfile withPoints(int points) {
		return PlayerProfile.createDefault(0).withUnallocatedStatPoints(points);
	}

	@Test
	void allocationMovesPointsIntoStat() {
		StatRules.Allocation result = RULES.allocate(withPoints(6), StatType.DEFENSE, 4);
		assertEquals(StatRules.AllocationResult.SUCCESS, result.result());
		assertEquals(4, result.profile().stats().get(StatType.DEFENSE));
		assertEquals(2, result.profile().unallocatedStatPoints());
	}

	@Test
	void invalidAllocationsLeaveProfileUnchanged() {
		PlayerProfile profile = withPoints(3);
		assertResult(StatRules.AllocationResult.INVALID_AMOUNT, profile, RULES.allocate(profile, StatType.MELEE, 0));
		assertResult(StatRules.AllocationResult.INVALID_AMOUNT, profile, RULES.allocate(profile, StatType.MELEE, -5));
		assertResult(StatRules.AllocationResult.NOT_ENOUGH_POINTS, profile, RULES.allocate(profile, StatType.MELEE, 4));

		PlayerProfile nearCap = withPoints(50).withStats(PlayerStats.EMPTY.with(StatType.SWORD, 9));
		assertResult(StatRules.AllocationResult.STAT_AT_CAP, nearCap, RULES.allocate(nearCap, StatType.SWORD, 2));
		assertEquals(StatRules.AllocationResult.SUCCESS, RULES.allocate(nearCap, StatType.SWORD, 1).result());
	}

	private static void assertResult(StatRules.AllocationResult expected, PlayerProfile input, StatRules.Allocation actual) {
		assertEquals(expected, actual.result());
		assertSame(input, actual.profile(), "rejected allocations must return the input profile unchanged");
	}

	@Test
	void resetRefundsEveryInvestedPoint() {
		PlayerProfile profile = withPoints(2).withStats(PlayerStats.EMPTY.with(StatType.MELEE, 5).with(StatType.GUN, 3));
		PlayerProfile reset = RULES.resetStats(profile);
		assertEquals(10, reset.unallocatedStatPoints());
		assertEquals(0, reset.stats().total());
	}

	@Test
	void bonusesScaleAndRespectCaps() {
		PlayerStats stats = PlayerStats.EMPTY.with(StatType.DEFENSE, 6).with(StatType.MELEE, 4).with(StatType.SWORD, 5);
		assertEquals(3.0, RULES.bonusHealth(stats), 1e-9);
		assertEquals(1.0, RULES.bonusMeleeDamage(stats), 1e-9);
		assertEquals(0.5, RULES.bonusSwordDamage(stats), 1e-9);

		PlayerStats huge = PlayerStats.EMPTY.with(StatType.DEFENSE, 1000).with(StatType.MELEE, 1000);
		assertEquals(4.0, RULES.bonusHealth(huge), 1e-9, "health bonus capped");
		assertEquals(2.0, RULES.bonusMeleeDamage(huge), 1e-9, "damage bonus capped");
		assertEquals(0.0, RULES.bonusSwordDamage(PlayerStats.EMPTY), 1e-9);
	}
}
