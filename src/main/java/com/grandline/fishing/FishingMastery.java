package com.grandline.fishing;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** A player's Fishing Mastery: level (1..100) and experience toward the next level. Saved, kept through death. */
public record FishingMastery(int level, long xp, int catches) {
	public static final FishingMastery START = new FishingMastery(1, 0, 0);
	public static final Codec<FishingMastery> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.INT.fieldOf("level").forGetter(FishingMastery::level),
			Codec.LONG.fieldOf("xp").forGetter(FishingMastery::xp),
			Codec.INT.optionalFieldOf("catches", 0).forGetter(FishingMastery::catches)).apply(i, FishingMastery::new));

	/** Adds experience (levelling up as needed) and counts a catch. */
	public FishingMastery withCatch(long gained) {
		int lv = level;
		long x = xp + gained;
		while (lv < FishingRules.maxLevel() && x >= FishingRules.xpToNext(lv)) {
			x -= FishingRules.xpToNext(lv);
			lv++;
		}
		if (lv >= FishingRules.maxLevel()) {
			x = 0;
		}
		return new FishingMastery(lv, x, catches + 1);
	}
}
