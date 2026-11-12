package com.grandline.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.grandline.GrandLineMod;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Server-side gameplay configuration, stored as {@code config/grandline.json}.
 *
 * <p>Values are always validated and clamped after loading so that a hand-edited file can never
 * put the server into an invalid state. On a dedicated server this file is the single source of
 * truth; clients never send configuration values to the server.
 */
public final class GrandLineConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

	/** Highest level a player can reach. */
	public int maxLevel = 2800;
	/** Unallocated stat points granted per level gained. */
	public int statPointsPerLevel = 3;
	/** Experience required to go from level 1 to level 2. */
	public long baseExperiencePerLevel = 100;
	/** Additional experience required for every level beyond the first. */
	public long experienceGrowthPerLevel = 50;
	/** Currency given to brand-new profiles. */
	public long startingCurrency = 0;
	/** Upper bound for currency, protects against overflow and economy exploits. */
	public long maxCurrency = 1_000_000_000_000L;
	/** Minimum number of server ticks between two accepted requests of the same type from one player. */
	public int requestCooldownTicks = 4;

	// --- Stats (milestone 2) ---
	/** Maximum points that can be invested in a single stat. */
	public int maxPointsPerStat = 2800;
	/** Extra max health (half-hearts) per Defense point. */
	public double defenseHealthPerPoint = 0.2;
	/** Upper bound for the Defense health bonus. */
	public double maxBonusHealth = 200.0;
	/** Extra attack damage per Melee point, applied while the main hand is empty. */
	public double meleeDamagePerPoint = 0.05;
	/** Extra attack damage per Sword point, applied while holding a sword. */
	public double swordDamagePerPoint = 0.05;
	/** Upper bound for the Melee and Sword damage bonuses. */
	public double maxBonusDamage = 100.0;

	// --- Mob kill rewards (milestone 2) ---
	/** Experience granted per point of the killed hostile mob's max health. */
	public double experiencePerMobHealth = 1.0;
	/** Currency granted per point of the killed hostile mob's max health. */
	public double currencyPerMobHealth = 0.5;
	/** Anti-farm cap: most experience a player can earn from mob kills per minute. 0 disables the cap. */
	public long maxKillExperiencePerMinute = 5_000;
	/** Anti-farm cap: most currency a player can earn from mob kills per minute. 0 disables the cap. */
	public long maxKillCurrencyPerMinute = 2_500;

	public static GrandLineConfig defaults() {
		return new GrandLineConfig();
	}

	/**
	 * Loads the config, creating it with defaults if missing. A malformed file is backed up and replaced by
	 * defaults rather than crashing the server.
	 */
	public static GrandLineConfig loadOrCreate(Path path) {
		GrandLineConfig config = defaults();

		if (Files.exists(path)) {
			try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
				GrandLineConfig loaded = GSON.fromJson(reader, GrandLineConfig.class);
				if (loaded != null) {
					config = loaded;
				}
			} catch (IOException | JsonParseException e) {
				GrandLineMod.LOGGER.error("Failed to read {}, backing it up and using defaults", path, e);
				backupBrokenFile(path);
			}
		}

		config.validate();
		save(path, config);
		return config;
	}

	/** Clamps every value into a safe range. */
	public void validate() {
		maxLevel = clamp(maxLevel, 1, 100_000);
		statPointsPerLevel = clamp(statPointsPerLevel, 0, 1_000);
		baseExperiencePerLevel = clamp(baseExperiencePerLevel, 1, 1_000_000_000L);
		experienceGrowthPerLevel = clamp(experienceGrowthPerLevel, 0, 1_000_000_000L);
		maxCurrency = clamp(maxCurrency, 0, Long.MAX_VALUE / 2);
		startingCurrency = clamp(startingCurrency, 0, maxCurrency);
		requestCooldownTicks = clamp(requestCooldownTicks, 0, 20 * 60);
		maxPointsPerStat = clamp(maxPointsPerStat, 0, 1_000_000);
		defenseHealthPerPoint = clamp(defenseHealthPerPoint, 0, 100);
		maxBonusHealth = clamp(maxBonusHealth, 0, 1000);
		meleeDamagePerPoint = clamp(meleeDamagePerPoint, 0, 100);
		swordDamagePerPoint = clamp(swordDamagePerPoint, 0, 100);
		maxBonusDamage = clamp(maxBonusDamage, 0, 2000);
		experiencePerMobHealth = clamp(experiencePerMobHealth, 0, 1_000_000);
		currencyPerMobHealth = clamp(currencyPerMobHealth, 0, 1_000_000);
		maxKillExperiencePerMinute = clamp(maxKillExperiencePerMinute, 0, Long.MAX_VALUE / 2);
		maxKillCurrencyPerMinute = clamp(maxKillCurrencyPerMinute, 0, Long.MAX_VALUE / 2);
	}

	private static void save(Path path, GrandLineConfig config) {
		try {
			Files.createDirectories(path.getParent());
			Path tmp = path.resolveSibling(path.getFileName() + ".tmp");
			try (Writer writer = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
				GSON.toJson(config, writer);
			}
			Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		} catch (IOException e) {
			GrandLineMod.LOGGER.warn("Could not write config file {}", path, e);
		}
	}

	private static void backupBrokenFile(Path path) {
		try {
			Files.move(path, path.resolveSibling(path.getFileName() + ".broken"), StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			GrandLineMod.LOGGER.warn("Could not back up broken config file {}", path, e);
		}
	}

	private static int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}

	private static double clamp(double value, double min, double max) {
		return Double.isNaN(value) ? min : Math.max(min, Math.min(max, value));
	}

	private static long clamp(long value, long min, long max) {
		return Math.max(min, Math.min(max, value));
	}
}
