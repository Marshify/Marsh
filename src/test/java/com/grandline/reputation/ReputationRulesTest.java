package com.grandline.reputation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReputationRulesTest {
	private final ReputationRules r = ReputationRules.DEFAULTS;

	@Test
	void pveCapsClampEachReward() {
		assertEquals(120, r.afterEnemy(0, 120), "enemy gives its level");
		assertEquals(250_000, r.afterEnemy(249_950, 300), "clamped at the 250K enemy cap");
		assertEquals(300_000, r.afterEnemy(300_000, 300), "nothing past the enemy cap");
		assertEquals(25 * 400, r.afterBoss(0, 400), "boss gives 25 x level");
		assertEquals(2_500_000, r.afterBoss(2_499_000, 400), "clamped at the 2.5M boss cap");
		assertEquals(2_600_000, r.afterBoss(2_600_000, 400), "nothing past the boss cap");
	}

	@Test
	void payoutMultiplierAndLossThresholds() {
		assertEquals(3.5 * 500, r.payout(500, 1_000_000));
		assertEquals(2_250, r.payout(500, 2_500_000), "level 500 at exactly 2.5M pays 2,250");
		assertEquals(2_250, r.loss(2_500_000, r.payout(500, 2_500_000)), "and costs the victim 2,250");
		assertEquals(0, r.loss(2_499_999, 1_000), "no loss below 2.5M");
		assertEquals(2_000, r.loss(4_000_000, 1_000), "double loss from 4M");
		assertEquals(7.0, r.multiplier(10_000_000));
		assertEquals(7.0, r.multiplier(29_000_000));
		assertEquals(4.5 + 2.5 * (6_250_000 - 2_500_000) / 7_500_000.0, r.multiplier(6_250_000), 1e-9, "linear 2.5M -> 10M");
		assertEquals(Math.round((4.5 + 2.5 * 0.5) * 333), r.payout(333, 6_250_000), "rounded once at the end");
	}

	@Test
	void capAndFloor() {
		assertEquals(30_000_000, r.clamp(31_000_000));
		assertEquals(0, r.clamp(-5));
	}

	@Test
	void levelGap() {
		assertTrue(r.levelsEligible(75, 100));
		assertFalse(r.levelsEligible(74, 100));
	}

	@Test
	void tiers() {
		assertEquals(0, ReputationRules.tier(499_999));
		assertEquals(1, ReputationRules.tier(500_000));
		assertEquals(10, ReputationRules.tier(5_000_000));
		assertEquals(11, ReputationRules.tier(6_000_000));
		assertEquals(25, ReputationRules.tier(30_000_000));
	}
}
