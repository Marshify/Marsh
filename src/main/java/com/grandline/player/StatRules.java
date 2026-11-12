package com.grandline.player;

import com.grandline.config.GrandLineConfig;

/**
 * Pure stat maths: allocation validation and derived bonuses. No Minecraft state, so it is unit tested directly.
 */
public record StatRules(int maxPointsPerStat, double defenseHealthPerPoint, double maxBonusHealth,
		double meleeDamagePerPoint, double swordDamagePerPoint, double maxBonusDamage) {
	public static StatRules fromConfig(GrandLineConfig config) {
		return new StatRules(config.maxPointsPerStat, config.defenseHealthPerPoint, config.maxBonusHealth,
				config.meleeDamagePerPoint, config.swordDamagePerPoint, config.maxBonusDamage);
	}

	public enum AllocationResult {
		SUCCESS,
		INVALID_AMOUNT,
		NOT_ENOUGH_POINTS,
		STAT_AT_CAP
	}

	/** Outcome of an allocation attempt; {@code profile} is the unchanged input unless {@code result} is SUCCESS. */
	public record Allocation(AllocationResult result, PlayerProfile profile) {
	}

	/**
	 * Moves {@code amount} unallocated points into {@code stat}. Rejected (profile unchanged) if the amount is not
	 * positive, the player lacks the points, or the stat would exceed its cap.
	 */
	public Allocation allocate(PlayerProfile profile, StatType stat, int amount) {
		if (amount <= 0) {
			return new Allocation(AllocationResult.INVALID_AMOUNT, profile);
		}
		if (amount > profile.unallocatedStatPoints()) {
			return new Allocation(AllocationResult.NOT_ENOUGH_POINTS, profile);
		}

		int current = profile.stats().get(stat);
		if ((long) current + amount > maxPointsPerStat) {
			return new Allocation(AllocationResult.STAT_AT_CAP, profile);
		}

		PlayerProfile updated = profile
				.withStats(profile.stats().with(stat, current + amount))
				.withUnallocatedStatPoints(profile.unallocatedStatPoints() - amount);
		return new Allocation(AllocationResult.SUCCESS, updated);
	}

	/** Refunds every invested point back to the unallocated pool. */
	public PlayerProfile resetStats(PlayerProfile profile) {
		long refunded = profile.unallocatedStatPoints() + profile.stats().total();
		return profile.withStats(PlayerStats.EMPTY).withUnallocatedStatPoints((int) Math.min(Integer.MAX_VALUE, refunded));
	}

	/** Bonus max health (in half-hearts) granted by Defense. */
	public double bonusHealth(PlayerStats stats) {
		return Math.min(maxBonusHealth, stats.get(StatType.DEFENSE) * defenseHealthPerPoint);
	}

	/** Bonus attack damage while the main hand is empty. */
	public double bonusMeleeDamage(PlayerStats stats) {
		return Math.min(maxBonusDamage, stats.get(StatType.MELEE) * meleeDamagePerPoint);
	}

	/** Bonus attack damage while holding a sword. */
	public double bonusSwordDamage(PlayerStats stats) {
		return Math.min(maxBonusDamage, stats.get(StatType.SWORD) * swordDamagePerPoint);
	}
}
