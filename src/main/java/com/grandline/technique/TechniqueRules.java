package com.grandline.technique;

import com.grandline.GrandLineMod;

/** Stage and charge tables (from the config), shared by server and client. */
public final class TechniqueRules {
	public static final int AURA_STAGES = 6;

	private TechniqueRules() {
	}

	/** Aura stage 0-5 for {@code xp}. */
	public static int auraStage(long xp) {
		long[] t = GrandLineMod.config().auraStageXp;
		int stage = 0;
		for (int i = 0; i < t.length; i++) {
			if (xp >= t[i]) {
				stage = i + 1;
			}
		}
		return stage;
	}

	/** XP needed for the next Aura stage, or -1 at the last stage. */
	public static long auraNextXp(long xp) {
		int s = auraStage(xp);
		long[] t = GrandLineMod.config().auraStageXp;
		return s >= t.length ? -1 : t[s];
	}

	public static double auraDamageBonus(int stage) {
		return GrandLineMod.config().auraDamageBonus[Math.max(0, Math.min(5, stage))] / 100.0;
	}

	public static double auraReduction(int stage) {
		return GrandLineMod.config().auraDamageReduction[Math.max(0, Math.min(5, stage))] / 100.0;
	}

	/** Maximum dodge charges for {@code xp} (2 to 8 with the default table). */
	public static int maxDodges(long xp) {
		long[] t = GrandLineMod.config().instinctChargeXp;
		int max = GrandLineMod.config().instinctBaseCharges;
		for (long need : t) {
			if (xp >= need) {
				max++;
			}
		}
		return max;
	}

	/** XP needed for the next extra charge, or -1 when all are unlocked. */
	public static long instinctNextXp(long xp) {
		for (long need : GrandLineMod.config().instinctChargeXp) {
			if (xp < need) {
				return need;
			}
		}
		return -1;
	}

	public static int regenTicks() {
		return GrandLineMod.config().instinctRegenSeconds * 20;
	}
}
