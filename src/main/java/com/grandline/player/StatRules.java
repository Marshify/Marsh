package com.grandline.player;

import com.grandline.config.GrandLineConfig;

/**
 * Pure stat maths: allocation validation and derived bonuses. No Minecraft state, so it is unit tested directly.
 *
 * <p>Every bonus follows a {@link Curve}: the first {@code earlyPoints} points are worth {@code early} each, later
 * points {@code late} each, and the total is capped. This makes the first points noticeable without making maxed
 * players absurdly strong.
 */
public record StatRules(int maxPointsPerStat, Curve health, Curve melee, Curve sword, Curve abilityPower) {
	/** Diminishing-returns curve. */
	public record Curve(int earlyPoints, double early, double late, double cap) {
		public double value(int points) {
			if (points <= 0) {
				return 0.0;
			}
			int earlyCount = Math.min(points, earlyPoints);
			int lateCount = points - earlyCount;
			return Math.min(cap, earlyCount * early + lateCount * late);
		}
	}

	public static StatRules fromConfig(GrandLineConfig config) {
		int earlyPoints = config.statEarlyPoints;
		return new StatRules(config.maxPointsPerStat,
				new Curve(earlyPoints, config.defenseHealthPerPoint, config.defenseHealthPerLatePoint, config.maxBonusHealth),
				new Curve(earlyPoints, config.meleeDamagePerPoint, config.meleeDamagePerLatePoint, config.maxBonusDamage),
				new Curve(earlyPoints, config.swordDamagePerPoint, config.swordDamagePerLatePoint, config.maxBonusDamage),
				new Curve(earlyPoints, config.abilityPowerPerPoint, config.abilityPowerPerLatePoint, config.maxAbilityPower));
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
		return health.value(stats.get(StatType.DEFENSE));
	}

	/** Bonus attack damage while the main hand is empty. */
	public double bonusMeleeDamage(PlayerStats stats) {
		return melee.value(stats.get(StatType.MELEE));
	}

	/** Bonus attack damage while holding a sword. */
	public double bonusSwordDamage(PlayerStats stats) {
		return sword.value(stats.get(StatType.SWORD));
	}

	/** Damage multiplier for moves powered by {@code stat} (Melee for fighting styles, Fruit for fruits). */
	public double abilityMultiplier(PlayerStats stats, StatType stat) {
		return 1.0 + abilityPower.value(stats.get(stat));
	}
}
