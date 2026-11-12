package com.grandline.player.reward;

import com.grandline.config.GrandLineConfig;

/**
 * Pure reward maths for mob kills, plus the per-minute anti-farm budget. No Minecraft state, unit tested directly.
 */
public record RewardRules(double experiencePerMobHealth, double currencyPerMobHealth, long maxExperiencePerMinute,
		long maxCurrencyPerMinute) {
	public static final int WINDOW_TICKS = 20 * 60;

	public static RewardRules fromConfig(GrandLineConfig config) {
		return new RewardRules(config.experiencePerMobHealth, config.currencyPerMobHealth,
				config.maxKillExperiencePerMinute, config.maxKillCurrencyPerMinute);
	}

	/** A reward before the anti-farm budget is applied. */
	public record Reward(long experience, long currency) {
		public static final Reward NONE = new Reward(0, 0);

		public boolean isEmpty() {
			return experience <= 0 && currency <= 0;
		}
	}

	/** Base reward for killing a hostile mob with the given max health. */
	public Reward rewardFor(float mobMaxHealth) {
		if (!(mobMaxHealth > 0) || Float.isInfinite(mobMaxHealth)) {
			return Reward.NONE;
		}
		return new Reward(Math.round(mobMaxHealth * experiencePerMobHealth), Math.round(mobMaxHealth * currencyPerMobHealth));
	}

	/**
	 * Mutable per-player budget over a rolling one-minute window (reset when the window expires).
	 * Accessed only from the server thread.
	 */
	public static final class Budget {
		private int windowStartTick = Integer.MIN_VALUE;
		private long experienceEarned;
		private long currencyEarned;

		/** Returns how much of {@code reward} may actually be granted now, and records it as earned. */
		public Reward claim(RewardRules rules, Reward reward, int nowTick) {
			if (windowStartTick == Integer.MIN_VALUE || nowTick - windowStartTick >= WINDOW_TICKS || nowTick < windowStartTick) {
				windowStartTick = nowTick;
				experienceEarned = 0;
				currencyEarned = 0;
			}

			long experience = limit(reward.experience(), experienceEarned, rules.maxExperiencePerMinute());
			long currency = limit(reward.currency(), currencyEarned, rules.maxCurrencyPerMinute());
			experienceEarned += experience;
			currencyEarned += currency;
			return new Reward(experience, currency);
		}

		private static long limit(long wanted, long alreadyEarned, long cap) {
			if (wanted <= 0) {
				return 0;
			}
			if (cap <= 0) {
				return wanted; // cap disabled
			}
			return Math.max(0, Math.min(wanted, cap - alreadyEarned));
		}
	}
}
