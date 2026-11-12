package com.grandline.combat.ability;

import com.grandline.config.GrandLineConfig;

/** Pure mastery maths: mastery level = experience / experiencePerLevel, capped. */
public record MasteryRules(long experiencePerLevel, int maxLevel, double perKilledHealth) {
	public static MasteryRules fromConfig(GrandLineConfig config) {
		return new MasteryRules(config.masteryExperiencePerLevel, config.maxMastery, config.masteryPerKilledHealth);
	}

	public int level(long experience) {
		if (experience <= 0 || experiencePerLevel <= 0) {
			return 0;
		}
		return (int) Math.min(maxLevel, experience / experiencePerLevel);
	}

	/** Mastery experience for killing an enemy with {@code maxHealth} (at least 1 for any kill). */
	public long experienceForKill(float maxHealth) {
		if (!(maxHealth > 0) || Float.isInfinite(maxHealth)) {
			return 0;
		}
		return Math.max(1L, Math.round(maxHealth * perKilledHealth));
	}
}
