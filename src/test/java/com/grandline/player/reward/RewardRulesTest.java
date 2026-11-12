package com.grandline.player.reward;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RewardRulesTest {
	private static final RewardRules RULES = new RewardRules(1.0, 0.5, 50, 20);

	@Test
	void rewardScalesWithMobHealth() {
		assertEquals(new RewardRules.Reward(20, 10), RULES.rewardFor(20f));
		assertEquals(new RewardRules.Reward(300, 150), RULES.rewardFor(300f));
	}

	@Test
	void invalidHealthGivesNothing() {
		assertTrue(RULES.rewardFor(0f).isEmpty());
		assertTrue(RULES.rewardFor(-5f).isEmpty());
		assertTrue(RULES.rewardFor(Float.NaN).isEmpty());
		assertTrue(RULES.rewardFor(Float.POSITIVE_INFINITY).isEmpty());
	}

	@Test
	void budgetCapsRewardsWithinOneMinute() {
		RewardRules.Budget budget = new RewardRules.Budget();
		RewardRules.Reward zombie = RULES.rewardFor(20f);

		assertEquals(new RewardRules.Reward(20, 10), budget.claim(RULES, zombie, 0));
		assertEquals(new RewardRules.Reward(20, 10), budget.claim(RULES, zombie, 10));
		assertEquals(new RewardRules.Reward(10, 0), budget.claim(RULES, zombie, 20), "partially capped");
		assertTrue(budget.claim(RULES, zombie, 30).isEmpty(), "fully capped");

		assertEquals(new RewardRules.Reward(20, 10), budget.claim(RULES, zombie, RewardRules.WINDOW_TICKS),
				"budget resets after the window");
	}

	@Test
	void zeroCapDisablesTheLimit() {
		RewardRules uncapped = new RewardRules(1.0, 1.0, 0, 0);
		RewardRules.Budget budget = new RewardRules.Budget();
		for (int i = 0; i < 100; i++) {
			assertEquals(new RewardRules.Reward(100, 100), budget.claim(uncapped, uncapped.rewardFor(100f), i));
		}
	}
}
