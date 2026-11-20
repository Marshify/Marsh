package com.grandline.client.fx;

import java.util.Map;

/**
 * Colours of a sword's effects: the bright edge, the glowing core, an optional dark interior (drawn with ordinary
 * transparency so it reads as black or deep crimson rather than light), and two spark colours.
 */
record SwordPalette(int edge, int core, int dark, boolean darkCore, int sparkFrom, int sparkTo) {
	static final SwordPalette WHITE = new SwordPalette(0xFFFFFF, 0xBFD8FF, 0x2A3442, false, 0xFFFFFF, 0x9FB8D8);
	private static final Map<String, SwordPalette> ALL = Map.ofEntries(
			Map.entry("white", WHITE),
			Map.entry("cutlass", new SwordPalette(0xFFF6DA, 0xE8C878, 0x3A3020, false, 0xFFF6DA, 0xC9A050)),
			Map.entry("teal", new SwordPalette(0xE9FFFA, 0x4FE0C4, 0x0B2A26, false, 0xE9FFFA, 0x3FBFA8)),
			Map.entry("stone", new SwordPalette(0xF0E8D6, 0xA89A82, 0x2C261E, false, 0xFFE8B0, 0x8A7F70)),
			Map.entry("reef", new SwordPalette(0xFF4038, 0x9A0A14, 0x0E0204, true, 0xFF6A50, 0x5A0008)),
			Map.entry("azure", new SwordPalette(0xD6F0FF, 0x2E8BFF, 0x061A3A, false, 0xD6F0FF, 0x2E8BFF)),
			Map.entry("fire", new SwordPalette(0xFFE48A, 0xFF6A10, 0x3A1A0A, false, 0xFFF0A0, 0xFF4A0A)),
			Map.entry("warden", new SwordPalette(0xEAF4FF, 0x2F6BFF, 0x081A44, false, 0xEAF4FF, 0x3A7BFF)),
			Map.entry("twin", new SwordPalette(0xFFF4C0, 0x7FE0FF, 0x10242E, false, 0xFFE9A0, 0x7FE0FF)),
			Map.entry("wraith", new SwordPalette(0xFFFFFF, 0xA8D4FF, 0x1A2A40, false, 0xFFFFFF, 0x9CC8FF)),
			Map.entry("bisento", new SwordPalette(0xFFFFFF, 0xDCE6FF, 0x30384A, false, 0xFFFFFF, 0xC8D4F0)),
			Map.entry("tide", new SwordPalette(0xDAF6FF, 0x1E86E0, 0x04203C, false, 0xDAF6FF, 0x2F9AF0)),
			Map.entry("storm", new SwordPalette(0xFFFBD8, 0x6FB8FF, 0x1A1F2E, false, 0xFFFBD8, 0x8FD0FF)),
			Map.entry("saber", new SwordPalette(0xFF5A5A, 0xE0142A, 0x2A0004, true, 0xFF8A80, 0xB0001A)),
			Map.entry("eclipse", new SwordPalette(0x8CFFAA, 0x14E05A, 0x010703, true, 0xB8FFC8, 0x0FA040)));

	static SwordPalette of(String name) {
		return ALL.getOrDefault(name, WHITE);
	}
}
