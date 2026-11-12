package com.grandline.player;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProgressionRulesTest {
	private static final ProgressionRules RULES = new ProgressionRules(100, 3, 100, 50, 1_000_000);

	private static PlayerProfile fresh() {
		return PlayerProfile.createDefault(0);
	}

	@Test
	void experienceCurveGrowsLinearly() {
		assertEquals(100, RULES.experienceToNextLevel(1));
		assertEquals(150, RULES.experienceToNextLevel(2));
		assertEquals(200, RULES.experienceToNextLevel(3));
		assertEquals(0, RULES.experienceToNextLevel(100), "no further experience needed at the cap");
	}

	@Test
	void addingExperienceLevelsUpAndGrantsStatPoints() {
		// 100 (1->2) + 150 (2->3) = 250, plus 10 left over
		PlayerProfile result = RULES.addExperience(fresh(), 260);
		assertEquals(3, result.level());
		assertEquals(10, result.experience());
		assertEquals(6, result.unallocatedStatPoints());
	}

	@Test
	void experienceBelowThresholdDoesNotLevel() {
		PlayerProfile result = RULES.addExperience(fresh(), 99);
		assertEquals(1, result.level());
		assertEquals(99, result.experience());
		assertEquals(0, result.unallocatedStatPoints());
	}

	@Test
	void levelIsCappedAndOverflowIsSafe() {
		PlayerProfile result = RULES.addExperience(fresh(), Long.MAX_VALUE);
		assertEquals(100, result.level());
		assertEquals(0, result.experience());
		assertEquals(99 * 3, result.unallocatedStatPoints());

		PlayerProfile again = RULES.addExperience(result, Long.MAX_VALUE);
		assertEquals(result, again, "adding experience at the cap changes nothing");
	}

	@Test
	void negativeExperienceIsRejected() {
		assertThrows(IllegalArgumentException.class, () -> RULES.addExperience(fresh(), -1));
	}

	@Test
	void currencyIsClampedToValidRange() {
		assertEquals(500, RULES.addCurrency(fresh(), 500).currency());
		assertEquals(0, RULES.addCurrency(fresh(), -500).currency());
		assertEquals(1_000_000, RULES.addCurrency(fresh(), Long.MAX_VALUE).currency());
		assertEquals(0, RULES.addCurrency(fresh().withCurrency(10), Long.MIN_VALUE).currency());
	}

	@Test
	void setLevelAdjustsStatPointsWithoutGoingNegative() {
		PlayerProfile raised = RULES.setLevel(fresh(), 11);
		assertEquals(11, raised.level());
		assertEquals(30, raised.unallocatedStatPoints());

		PlayerProfile lowered = RULES.setLevel(raised.withUnallocatedStatPoints(5), 1);
		assertEquals(1, lowered.level());
		assertEquals(0, lowered.unallocatedStatPoints());

		assertEquals(100, RULES.setLevel(fresh(), 5000).level(), "clamped to max level");
		assertEquals(1, RULES.setLevel(fresh(), -5).level(), "clamped to min level");
	}
}
