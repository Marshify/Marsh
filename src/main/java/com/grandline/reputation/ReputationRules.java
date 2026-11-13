package com.grandline.reputation;

import com.grandline.config.GrandLineConfig;

/**
 * Bounty/Honor maths, free of game state so it can be unit tested.
 *
 * <ul>
 *   <li>PvE: enemies give {@code level x reputationPerEnemyLevel} until the total reaches the enemy cap (250K by
 *   default); bosses give {@code 25 x level} until the boss cap (2.5M). Each reward is clamped at its own cap.</li>
 *   <li>PvP payout: {@code multiplier(victimTotal) x victimLevel}, rounded once at the end. The multiplier is 3.5
 *   below 2.5M, 4.5 at 2.5M, rising linearly to 7 at 10M, then 7.</li>
 *   <li>PvP loss for the victim: 0 below 2.5M, 1x payout from 2.5M to below 4M, 2x payout from 4M.</li>
 *   <li>Every total is clamped to [0, max] (30M).</li>
 * </ul>
 */
public record ReputationRules(long max, double perEnemyLevel, long enemyCap, double perBossLevel, long bossCap, double levelRatio,
		long riskThreshold) {
	public static final long LOW_LOSS_END = 4_000_000L;
	public static final long MULTIPLIER_TOP = 10_000_000L;

	public static ReputationRules fromConfig(GrandLineConfig c) {
		return new ReputationRules(c.reputationMax, c.reputationPerEnemyLevel, c.reputationEnemyCap, c.reputationPerBossLevel,
				c.reputationBossCap, c.pvpLevelRatio, c.reputationRiskThreshold);
	}

	public static final ReputationRules DEFAULTS = fromConfig(GrandLineConfig.defaults());

	/** New total after killing an ordinary Grand Line enemy of {@code level}. */
	public long afterEnemy(long total, int level) {
		return pve(total, Math.round(level * perEnemyLevel), enemyCap);
	}

	/** New total after killing a boss of {@code level}. */
	public long afterBoss(long total, int level) {
		return pve(total, Math.round(level * perBossLevel), bossCap);
	}

	private long pve(long total, long reward, long cap) {
		if (total >= cap || reward <= 0) {
			return total;
		}
		return clamp(Math.min(total + reward, cap));
	}

	/** PvP multiplier for a victim holding {@code victimTotal}. */
	public double multiplier(long victimTotal) {
		if (victimTotal < riskThreshold) {
			return 3.5;
		}
		if (victimTotal >= MULTIPLIER_TOP) {
			return 7.0;
		}
		double t = (victimTotal - riskThreshold) / (double) (MULTIPLIER_TOP - riskThreshold);
		return 4.5 + (7.0 - 4.5) * t;
	}

	/** Reputation paid for killing a victim of {@code victimLevel} holding {@code victimTotal} (rounded once). */
	public long payout(int victimLevel, long victimTotal) {
		return Math.round(multiplier(victimTotal) * victimLevel);
	}

	/** What the victim loses (computed from the payout before the winner's own cap is applied). */
	public long loss(long victimTotal, long payout) {
		if (victimTotal < riskThreshold) {
			return 0;
		}
		return victimTotal < LOW_LOSS_END ? payout : 2 * payout;
	}

	/** Whether two players' levels are close enough for PvP reputation (lower >= ratio x higher). */
	public boolean levelsEligible(int a, int b) {
		int low = Math.min(a, b);
		int high = Math.max(a, b);
		return high <= 0 || low >= levelRatio * high;
	}

	public long clamp(long total) {
		return Math.max(0, Math.min(max, total));
	}

	/** Reputation tier for PvP bonuses: one per 500K up to 5M (10), then one per 1M up to 20M (25). */
	public static int tier(long total) {
		if (total < 500_000L) {
			return 0;
		}
		if (total <= 5_000_000L) {
			return (int) (total / 500_000L);
		}
		return (int) Math.min(25, 10 + (total - 5_000_000L) / 1_000_000L);
	}

	/** Cosmetic title index (0-7) for the thresholds 0, 500K, 1M, 2.5M, 5M, 10M, 20M, 30M. */
	public static int title(long total) {
		long[] steps = {500_000L, 1_000_000L, 2_500_000L, 5_000_000L, 10_000_000L, 20_000_000L, 30_000_000L};
		int i = 0;
		while (i < steps.length && total >= steps[i]) {
			i++;
		}
		return i;
	}
}
