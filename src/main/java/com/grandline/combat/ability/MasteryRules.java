package com.grandline.combat.ability;

import com.grandline.config.GrandLineConfig;

/** Pure mastery maths: mastery level = experience / experiencePerLevel, capped. */
public record MasteryRules(long experiencePerLevel, int maxLevel, long experiencePerHit) {
	public static MasteryRules fromConfig(GrandLineConfig config) {
		return new MasteryRules(config.masteryExperiencePerLevel, config.maxMastery, config.masteryExperiencePerHit);
	}

	public int level(long experience) {
		if (experience <= 0 || experiencePerLevel <= 0) {
			return 0;
		}
		return (int) Math.min(maxLevel, experience / experiencePerLevel);
	}

	/** Experience to grant for a move that hit {@code targetsHit} entities. */
	public long experienceFor(int targetsHit) {
		return Math.max(0, targetsHit) * experiencePerHit;
	}
}
