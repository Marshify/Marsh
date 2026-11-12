package com.grandline.world;

/** Visual theme of a hand-designed island: terrain style plus its landmark buildings. */
public enum IslandTheme {
	TOWN, JUNGLE, SNOW, DESERT, VOLCANO, FORTRESS, VILLAGE, BRICK_TOWN, PRISON, CAVE, HARBOR, SKY, COLOSSEUM, PALACE, CRYSTAL,
	/** Plain tropical island (random islands outside the Grand Line seas). */
	PLAIN;

	public static IslandTheme byName(String name) {
		for (IslandTheme theme : values()) {
			if (theme.name().equalsIgnoreCase(name)) {
				return theme;
			}
		}
		return PLAIN;
	}
}
