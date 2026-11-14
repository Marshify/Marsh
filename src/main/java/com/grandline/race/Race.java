package com.grandline.race;

import java.util.Locale;

/**
 * The six races. Four are standard (a new player is given one at random); Cyborg and Ghoul are unlocked through their
 * own quest lines. Each has a signature colour used by its HUD, cosmetics and effects.
 */
public enum Race {
	HUMAN(true, 0xE04A3A), SHARK(true, 0x3A8EE0), ANGEL(true, 0xFFE9A8), RABBIT(true, 0x6BE07F), CYBORG(false, 0x58C8FF), GHOUL(false, 0xB01E32);

	public final boolean standard;
	public final int color;

	Race(boolean standard, int color) {
		this.standard = standard;
		this.color = color;
	}

	public String id() {
		return name().toLowerCase(Locale.ROOT);
	}

	public static Race byId(String id) {
		for (Race r : values()) {
			if (r.id().equals(id)) {
				return r;
			}
		}
		return HUMAN;
	}
}
