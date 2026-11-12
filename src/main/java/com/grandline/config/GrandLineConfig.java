package com.grandline.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
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

	/**
	 * Config schema version. 1 = milestone 1/2 values; 2 = rebalanced stats with diminishing returns and ability
	 * settings. Older files have balance keys reset to the new defaults once, other keys are kept.
	 */
	public static final int CURRENT_CONFIG_VERSION = 4;

	public int configVersion = CURRENT_CONFIG_VERSION;

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

	// --- Stats ---
	// Every stat uses diminishing returns: the first statEarlyPoints points give the "PerPoint" value each, every
	// point after that gives the "PerLatePoint" value, and the total is capped.
	/** Maximum points that can be invested in a single stat. */
	public int maxPointsPerStat = 2800;
	/** Number of points per stat that give the full (early) bonus. */
	public int statEarlyPoints = 20;
	/** Extra max health (half-hearts) per early Defense point. */
	public double defenseHealthPerPoint = 1.0;
	/** Extra max health (half-hearts) per Defense point after the early points. */
	public double defenseHealthPerLatePoint = 0.05;
	/** Upper bound for the Defense health bonus (half-hearts). */
	public double maxBonusHealth = 200.0;
	/** Extra fist damage per early Melee point (empty main hand). */
	public double meleeDamagePerPoint = 0.25;
	/** Extra fist damage per Melee point after the early points. */
	public double meleeDamagePerLatePoint = 0.02;
	/** Extra damage per early Sword point (holding a sword). */
	public double swordDamagePerPoint = 0.25;
	/** Extra damage per Sword point after the early points. */
	public double swordDamagePerLatePoint = 0.02;
	/** Upper bound for the Melee and Sword damage bonuses. */
	public double maxBonusDamage = 60.0;
	/** Ability power (fraction, 0.02 = +2%) per early Melee point (fighting style moves) or Fruit point (fruit moves). */
	public double abilityPowerPerPoint = 0.02;
	/** Ability power per point after the early points. */
	public double abilityPowerPerLatePoint = 0.002;
	/** Upper bound for ability power (3.0 = +300% damage). */
	public double maxAbilityPower = 3.0;

	// --- Abilities & mastery (milestone 3) ---
	/** Mastery experience per mastery level. */
	public long masteryExperiencePerLevel = 25;
	/** Highest mastery level. */
	public int maxMastery = 600;
	/** Mastery experience per point of a killed enemy's max health, given to the power that landed the killing blow. */
	public double masteryPerKilledHealth = 1.0;

	/** Max energy at level 1. */
	public double baseEnergy = 100.0;
	/** Extra max energy per level above 1. */
	public double energyPerLevel = 2.0;
	/** Fraction of max energy regenerated per second. */
	public double energyRegenPerSecond = 0.08;

	// --- Fruits (milestone 4) ---
	/** Shop price per rarity: common, uncommon, rare, legendary, mythical. */
	public long[] fruitPrices = {2_500, 10_000, 40_000, 150_000, 500_000};
	/** Gacha price at level 1. Price per spin = gachaCost + gachaCostPerLevel × (level − 1). */
	public long gachaCost = 25_000;
	/** Extra gacha price per player level above 1. */
	public long gachaCostPerLevel = 150;
	/** Minimum player level to use the gacha. */
	public int gachaMinLevel = 50;
	/** Minutes a player must wait between gacha rolls. */
	public int gachaCooldownMinutes = 30;
	/** How many fruits the dealer offers at once. */
	public int dealerStockSize = 3;
	/** Minutes between dealer stock rotations. */
	public int dealerRotationMinutes = 240;
	/** Most fruits a player can keep in their stash. */
	public int stashCapacity = 20;
	/** Average minutes between natural fruit spawns near a random online player (0 = off; buried treasure replaces them). */
	public int naturalFruitSpawnMinutes = 0;
	/** Currency cost to refund (reset) all stat points. 0 = free. */
	public long statRefundCost = 1_000;

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
				JsonElement json = JsonParser.parseReader(reader);
				GrandLineConfig loaded = GSON.fromJson(json, GrandLineConfig.class);
				if (loaded != null) {
					int fileVersion = json.isJsonObject() && json.getAsJsonObject().has("configVersion")
							? json.getAsJsonObject().get("configVersion").getAsInt() : 1;
					loaded.migrateFrom(fileVersion);
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

	/** Upgrades values written by an older version of the mod. */
	void migrateFrom(int fileVersion) {
		if (fileVersion < 2) {
			GrandLineConfig fresh = defaults();
			defenseHealthPerPoint = fresh.defenseHealthPerPoint;
			meleeDamagePerPoint = fresh.meleeDamagePerPoint;
			swordDamagePerPoint = fresh.swordDamagePerPoint;
			maxBonusDamage = fresh.maxBonusDamage;
			GrandLineMod.LOGGER.info("Migrated config from version {} to {} (stat balance reset to new defaults)", fileVersion,
					CURRENT_CONFIG_VERSION);
		}
		if (fileVersion < 3) {
			gachaCost = defaults().gachaCost; // level-scaled gacha pricing replaced the flat 5,000 price
		}
		if (fileVersion < 4) {
			naturalFruitSpawnMinutes = 0; // fruits now come from buried treasure instead
		}
		configVersion = CURRENT_CONFIG_VERSION;
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
		statEarlyPoints = clamp(statEarlyPoints, 0, 1_000_000);
		defenseHealthPerLatePoint = clamp(defenseHealthPerLatePoint, 0, 100);
		meleeDamagePerLatePoint = clamp(meleeDamagePerLatePoint, 0, 100);
		swordDamagePerLatePoint = clamp(swordDamagePerLatePoint, 0, 100);
		abilityPowerPerPoint = clamp(abilityPowerPerPoint, 0, 10);
		abilityPowerPerLatePoint = clamp(abilityPowerPerLatePoint, 0, 10);
		maxAbilityPower = clamp(maxAbilityPower, 0, 100);
		masteryExperiencePerLevel = clamp(masteryExperiencePerLevel, 1, 1_000_000);
		maxMastery = clamp(maxMastery, 1, 100_000);
		masteryPerKilledHealth = clamp(masteryPerKilledHealth, 0, 1_000_000);
		baseEnergy = clamp(baseEnergy, 1, 1_000_000);
		energyPerLevel = clamp(energyPerLevel, 0, 10_000);
		energyRegenPerSecond = clamp(energyRegenPerSecond, 0, 10);
		if (fruitPrices == null || fruitPrices.length != 5) {
			fruitPrices = defaults().fruitPrices;
		}
		for (int i = 0; i < fruitPrices.length; i++) {
			fruitPrices[i] = clamp(fruitPrices[i], 0, maxCurrency);
		}
		gachaCost = clamp(gachaCost, 0, maxCurrency);
		gachaCostPerLevel = clamp(gachaCostPerLevel, 0, maxCurrency);
		gachaMinLevel = clamp(gachaMinLevel, 1, 100_000);
		gachaCooldownMinutes = clamp(gachaCooldownMinutes, 0, 60 * 24 * 7);
		dealerStockSize = clamp(dealerStockSize, 1, 6);
		dealerRotationMinutes = clamp(dealerRotationMinutes, 1, 60 * 24 * 7);
		stashCapacity = clamp(stashCapacity, 0, 1000);
		naturalFruitSpawnMinutes = clamp(naturalFruitSpawnMinutes, 0, 60 * 24 * 7);
		statRefundCost = clamp(statRefundCost, 0, maxCurrency);
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
