package com.grandline.combat.ability;

import com.grandline.config.GrandLineConfig;

/** Pure mastery maths: mastery level = 1 + experience / experiencePerLevel, capped. */
public record MasteryRules(long experiencePerLevel, int maxLevel, double perKilledHealth) {
	public static MasteryRules fromConfig(GrandLineConfig config) {
		return new MasteryRules(config.masteryExperiencePerLevel, config.maxMastery, config.masteryPerKilledHealth);
	}

	/** Mastery level. Like Blox Fruits, a fresh power starts at mastery 1. */
	public int level(long experience) {
		if (experience <= 0 || experiencePerLevel <= 0) {
			return 1;
		}
		return (int) Math.min(maxLevel, 1 + experience / experiencePerLevel);
	}

	/** Mastery experience for killing an enemy with {@code maxHealth} (at least 1 for any kill). */
	public long experienceForKill(float maxHealth) {
		if (!(maxHealth > 0) || Float.isInfinite(maxHealth)) {
			return 0;
		}
		return Math.max(1L, Math.round(maxHealth * perKilledHealth));
	}
}
