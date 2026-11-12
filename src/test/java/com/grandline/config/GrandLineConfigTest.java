package com.grandline.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GrandLineConfigTest {
	@Test
	void version1FileGetsNewBalanceButKeepsOtherSettings(@TempDir Path dir) throws Exception {
		Path file = dir.resolve("grandline.json");
		// What milestone 2 (v0.2.0) wrote: no configVersion, old stat values, and a user-customised maxLevel.
		Files.writeString(file, """
				{ "maxLevel": 500, "defenseHealthPerPoint": 0.2, "meleeDamagePerPoint": 0.05,
				  "swordDamagePerPoint": 0.05, "maxBonusDamage": 100.0 }
				""");

		GrandLineConfig config = GrandLineConfig.loadOrCreate(file);
		assertEquals(GrandLineConfig.CURRENT_CONFIG_VERSION, config.configVersion);
		assertEquals(1.0, config.defenseHealthPerPoint, 1e-9);
		assertEquals(0.25, config.meleeDamagePerPoint, 1e-9);
		assertEquals(60.0, config.maxBonusDamage, 1e-9);
		assertEquals(500, config.maxLevel, "unrelated settings are kept");
		assertTrue(Files.readString(file).contains("\"configVersion\": 2"), "file rewritten with the new version");

		// A current-version file is not touched by migration.
		Files.writeString(file, "{ \"configVersion\": 2, \"defenseHealthPerPoint\": 3.5 }");
		assertEquals(3.5, GrandLineConfig.loadOrCreate(file).defenseHealthPerPoint, 1e-9);
	}

	@Test
	void malformedFileFallsBackToDefaults(@TempDir Path dir) throws Exception {
		Path file = dir.resolve("grandline.json");
		Files.writeString(file, "{ this is not json");
		GrandLineConfig config = GrandLineConfig.loadOrCreate(file);
		assertEquals(2800, config.maxLevel);
		assertTrue(Files.exists(dir.resolve("grandline.json.broken")));
	}
}
