package com.grandline.world;

import java.util.Random;

/** Deterministic, original island names built from syllables. */
public final class IslandNames {
	private static final String[] FIRST = {"Mar", "Sal", "Cor", "Bri", "Ves", "Tor", "Gal", "Kel", "Dun", "Ash", "Mer", "Rho",
			"Zan", "Pel", "Wyn", "Cal", "Thal", "Ost", "Lun", "Fen", "Gul", "Hal", "Nor", "Sab"};
	private static final String[] SECOND = {"row", "reach", "haven", "moor", "wick", "dale", "crest", "holm", "fell", "tide",
			"point", "rock", "spire", "fall", "mere", "shore", "watch", "gate"};
	private static final String[] KIND = {"Isle", "Island", "Key", "Atoll", "Rock", "Cay", "Reef", "Bluff"};

	private IslandNames() {
	}

	public static String generate(long seed) {
		Random random = new Random(seed);
		return FIRST[random.nextInt(FIRST.length)] + SECOND[random.nextInt(SECOND.length)] + " " + KIND[random.nextInt(KIND.length)];
	}
}
