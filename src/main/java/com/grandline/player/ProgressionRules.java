package com.grandline.player;

import com.grandline.config.GrandLineConfig;

/**
 * Pure, side-effect-free progression math. Contains no Minecraft state so it can be unit tested directly.
 *
 * <p>Experience curve: going from level {@code L} to {@code L + 1} costs
 * {@code baseExperience + growth * (L - 1)} experience.
 */
public record ProgressionRules(int maxLevel, int statPointsPerLevel, long baseExperience, long growthPerLevel, long maxCurrency) {
	public static ProgressionRules fromConfig(GrandLineConfig config) {
		return new ProgressionRules(config.maxLevel, config.statPointsPerLevel, config.baseExperiencePerLevel,
				config.experienceGrowthPerLevel, config.maxCurrency);
	}

	/** Experience needed to advance from {@code level} to the next level. Returns 0 at the level cap. */
	public long experienceToNextLevel(int level) {
		if (level >= maxLevel) {
			return 0L;
		}

		return saturatedAdd(baseExperience, saturatedMultiply(growthPerLevel, Math.max(0, level - 1)));
	}

	/**
	 * Adds experience and performs any resulting level-ups, granting stat points per level gained.
	 * Negative amounts are rejected. Experience never accumulates past the level cap.
	 */
	public PlayerProfile addExperience(PlayerProfile profile, long amount) {
		if (amount < 0) {
			throw new IllegalArgumentException("Experience amount must not be negative: " + amount);
		}

		int level = profile.level();
		long experience = saturatedAdd(profile.experience(), amount);
		int levelsGained = 0;

		while (level < maxLevel) {
			long needed = experienceToNextLevel(level);
			if (experience < needed) {
				break;
			}

			experience -= needed;
			level++;
			levelsGained++;
		}

		if (level >= maxLevel) {
			level = maxLevel;
			experience = 0L;
		}

		int points = saturatedAddInt(profile.unallocatedStatPoints(), saturatedMultiplyInt(levelsGained, statPointsPerLevel));
		return new PlayerProfile(profile.dataVersion(), level, experience, profile.currency(), points, profile.stats());
	}

	/**
	 * Directly sets the level (admin operation). Stat points are adjusted by the level difference so that
	 * raising a level grants points and lowering it never leaves a negative balance. Experience is reset.
	 */
	public PlayerProfile setLevel(PlayerProfile profile, int newLevel) {
		int clamped = Math.max(PlayerProfile.MIN_LEVEL, Math.min(maxLevel, newLevel));
		int delta = clamped - profile.level();
		int points = Math.max(0, saturatedAddInt(profile.unallocatedStatPoints(), saturatedMultiplyInt(delta, statPointsPerLevel)));
		return new PlayerProfile(profile.dataVersion(), clamped, 0L, profile.currency(), points, profile.stats());
	}

	/** Adds (or with a negative amount, removes) currency, clamped to {@code [0, maxCurrency]}. */
	public PlayerProfile addCurrency(PlayerProfile profile, long amount) {
		long result = saturatedAdd(profile.currency(), amount);
		return profile.withCurrency(Math.max(0L, Math.min(maxCurrency, result)));
	}

	private static long saturatedAdd(long a, long b) {
		long r = a + b;
		if (((a ^ r) & (b ^ r)) < 0) {
			return a < 0 ? Long.MIN_VALUE : Long.MAX_VALUE;
		}
		return r;
	}

	private static long saturatedMultiply(long a, long b) {
		long hi = Math.multiplyHigh(a, b);
		long lo = a * b;
		if ((hi == 0 && lo >= 0) || (hi == -1 && lo < 0)) {
			return lo;
		}
		return (a < 0) == (b < 0) ? Long.MAX_VALUE : Long.MIN_VALUE;
	}

	private static int saturatedAddInt(int a, int b) {
		return (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, (long) a + b));
	}

	private static int saturatedMultiplyInt(int a, int b) {
		return (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, (long) a * b));
	}
}
