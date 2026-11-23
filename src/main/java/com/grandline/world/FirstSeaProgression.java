package com.grandline.world;

import net.minecraft.util.RandomSource;

import java.util.List;

/**
 * The First Sea level ladder (Lv 1-700) in one place. Island levels, enemy and boss levels, quest rewards, NPC dialogue,
 * quest navigation and the "new quests" cards all read from here, so the progression is changed by editing this table.
 *
 * <p>Each stage is an island, or one region of an island ({@link #UPPER} for the upper Cloudtop Isles). A stage's first
 * level is where it unlocks; at a shared boundary the next island takes over. Dawnreach Harbor is the central hub (Lv 1,
 * no quests) and is not a stage.
 */
public final class FirstSeaProgression {
	/** Regions of an island: the island as a whole (or its lower part), and its upper part. */
	public static final int LOWER = 0, UPPER = 1;

	/** One rung of the ladder. {@code marine}: only Marines start here (null for both sides). */
	public record Stage(int number, String island, int region, int min, int max, Boolean marine) {
		public String label() {
			return region == UPPER ? island + " (Upper)" : island;
		}

		/** The level the island's quests are pitched at: the middle of its range. */
		public int questLevel() {
			return (min + max) / 2;
		}

		/** Quest bosses stand near the top of the range. */
		public int bossLevel() {
			return Math.max(min, max - Math.max(1, (max - min) / 10));
		}

		/**
		 * A regular enemy's level: the first camp holds the basic enemies (the lower 40% of the range), other camps the
		 * stronger ones (35-75%). Bosses take {@link #bossLevel()}.
		 */
		public int enemyLevel(int band, RandomSource random) {
			int span = Math.max(1, max - min);
			int lo = band == 0 ? min : min + (int) Math.round(span * 0.35);
			int hi = band == 0 ? min + (int) Math.round(span * 0.4) : min + (int) Math.round(span * 0.75);
			hi = Math.max(lo, hi);
			return lo + random.nextInt(hi - lo + 1);
		}
	}

	public static final List<Stage> STAGES = List.of(
			new Stage(1, "Millbrook Village", LOWER, 1, 10, false),
			new Stage(1, "Ironhold Bastion", LOWER, 1, 10, true), // the Marines' starting island (was Redbrick Port's stage)
			new Stage(2, "Verdant Wilds", LOWER, 10, 30, null),
			new Stage(3, "Saltmarket Docks", LOWER, 30, 60, null),
			new Stage(4, "Sunscorch Dunes", LOWER, 60, 90, null),
			new Stage(5, "Frostpeak Isle", LOWER, 90, 120, null),
			new Stage(6, "Redbrick Port", LOWER, 120, 150, null), // was Ironhold Bastion's stage
			new Stage(7, "Cloudtop Isles", LOWER, 150, 190, null),
			new Stage(8, "Stonewall Prison", LOWER, 190, 250, null),
			new Stage(9, "Gladiator's Ring", LOWER, 250, 300, null),
			new Stage(10, "Emberfall Isle", LOWER, 300, 375, null),
			new Stage(11, "Lotus Palace", LOWER, 375, 450, null),
			new Stage(12, "Cloudtop Isles", UPPER, 450, 625, null),
			new Stage(13, "Crystal Fountain", LOWER, 625, 700, null));

	/** The level that completes the First Sea (leveling continues past it). */
	public static final int FIRST_SEA_COMPLETE = 700;

	private FirstSeaProgression() {
	}

	/** The stage for an island's region, or null (hub, other seas, unknown islands). */
	public static Stage stage(String island, int region) {
		for (Stage s : STAGES) {
			if (s.island().equals(island) && s.region() == region) {
				return s;
			}
		}
		return null;
	}

	/** An island's unlock level (its lowest region), or {@code fallback} when it is not on the ladder. */
	public static int islandLevel(String island, int fallback) {
		Stage s = stage(island, LOWER);
		return s != null ? s.min() : fallback;
	}

	/** Whether this island has an upper region on the ladder. */
	public static boolean hasUpper(String island) {
		return stage(island, UPPER) != null;
	}

	/** The lowest and highest level enemies on this island may have (all regions together). */
	public static int[] islandSpan(String island) {
		int lo = Integer.MAX_VALUE, hi = Integer.MIN_VALUE;
		for (Stage s : STAGES) {
			if (s.island().equals(island)) {
				lo = Math.min(lo, s.min());
				hi = Math.max(hi, s.max());
			}
		}
		return lo == Integer.MAX_VALUE ? null : new int[]{lo, hi};
	}

	/**
	 * Where a player of this level should be quest-hunting: the highest stage they have reached, using their own side's
	 * starting island for stage 1. Shared boundaries go to the next island (Lv 10 is Verdant Wilds, not Millbrook).
	 */
	public static Stage stageFor(int level, boolean marine) {
		Stage best = null;
		for (Stage s : STAGES) {
			if (s.marine() != null && s.marine() != marine) {
				continue;
			}
			if (s.min() <= level && (best == null || s.min() >= best.min())) {
				best = s;
			}
		}
		return best;
	}

	/** Experience to go from {@code level} to the next, by the game's leveling rule. */
	static long toNext(int level) {
		var cfg = com.grandline.GrandLineMod.config();
		return cfg.baseExperiencePerLevel + cfg.experienceGrowthPerLevel * Math.max(0, level - 1);
	}

	/**
	 * Experience for one regular quest of this stage: 1.6 times what a player at the middle of the range needs for a
	 * level. Early stages (narrow ranges, small levels) go by in a few quests each; later ones need more quests but
	 * each quest still moves a player about one to two levels, so there is no wall. Boss quests pay five times this.
	 */
	public static long questExperience(Stage s) {
		return Math.round(toNext(s.questLevel()) * 1.6);
	}

	/** Money for one regular quest: rises with the stage alongside experience (bosses pay five times). */
	public static long questCurrency(Stage s) {
		return 40 + 20L * s.questLevel();
	}
}
