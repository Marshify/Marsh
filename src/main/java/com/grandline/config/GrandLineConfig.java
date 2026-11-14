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
	/** Gear: Black Cape max health bonus (half-hearts). */
	public double blackCapeHealth = 4.0;
	/** Gear: Black Cape max energy bonus. */
	public double blackCapeEnergy = 10.0;
	/** Gear: Pink Coat max health bonus (half-hearts). */
	public double pinkCoatHealth = 8.0;

	// ---- Bounty (Pirates) and Honor (Marines) ----
	/** Highest Bounty or Honor a player can hold. */
	public long reputationMax = 30_000_000L;
	/** Reputation per enemy level for ordinary Grand Line enemies (default: the enemy's level). */
	public double reputationPerEnemyLevel = 1.0;
	/** Enemies stop giving reputation once the active total reaches this. */
	public long reputationEnemyCap = 250_000L;
	/** Reputation per boss level for bosses. */
	public double reputationPerBossLevel = 25.0;
	/** Bosses stop giving reputation once the active total reaches this; after it, only PvP raises it. */
	public long reputationBossCap = 2_500_000L;
	/** PvP: the lower-level player must be at least this fraction of the higher player's level. */
	public double pvpLevelRatio = 0.75;
	/** PvP: at most this many reputation results per pair of players... */
	public int pvpPairLimit = 3;
	/** ...within this many hours (rolling, shared both ways). */
	public int pvpPairWindowHours = 72;
	/** Combat tag length after valid PvP damage. */
	public int combatTagSeconds = 30;
	/** Reputation at or above this is at risk (PvP losses, "at Risk" warning, disconnect penalty). */
	public long reputationRiskThreshold = 2_500_000L;
	/** PvP-only damage/defense bonuses from reputation tiers. Off until balance is tested. */
	public boolean reputationCombatBonuses = false;
	/** PvP damage bonus per tier (0.004 = +0.4%). Tiers: every 500K up to 5M, then every 1M up to 20M (25 tiers). */
	public double pvpDamagePerTier = 0.004;
	/** PvP damage reduction factor per tier (grows faster than damage). */
	public double pvpDefensePerTier = 0.006;

	// ---- Core survival rules ----
	/** Vanilla experience is disabled for players (no XP bar, no orbs, no XP gain or drops). */
	public boolean disableVanillaExperience = true;
	/** Hunger and saturation stay full; no exhaustion, starvation or vanilla natural regeneration. */
	public boolean infiniteHunger = true;
	/** Passive health regeneration: percent of maximum health per second (replaces vanilla regeneration). */
	public double baseHealthRegenPercentPerSecond = 1.0;
	/** Players never take fall damage. */
	public boolean disableFallDamage = true;
	/** Vanilla advancement messages are not announced in chat (gamerule announceAdvancements). */
	public boolean hideAdvancementAnnouncements = true;
	/** Players keep their whole inventory and equipment when they die (regardless of the keepInventory gamerule). */
	public boolean keepInventoryOnDeath = true;

	// ---- Ocean weakness (fruit users) ----
	/** Damage per second to fruit users in ocean water, as a percent of maximum health. 0 disables it. */
	public double oceanDamagePercentPerSecond = 5.0;
	/** How deep in water (blocks, from the feet) a fruit user must be before the sea weakens them. */
	public double oceanMinSubmersion = 0.6;
	/** Only ocean water counts (sea-level water in ocean biomes); false = any water. */
	public boolean oceanOnly = true;
	/** Water whose surface is above this height is not the ocean (fountains, ponds on hills). */
	public int oceanMaxSurfaceY = 63;

	// ---- Spawning ----
	/** Island new players start on and the final respawn fallback. */
	public String spawnIsland = "Millbrook Village";
	/** Starting island for players who join the Marines (new players start on spawnIsland until they pick a side). */
	public String marineSpawnIsland = "Redbrick Port";
	/** Respawn on the island where the player died (else their home island, else the spawn island). */
	public boolean islandRespawn = true;

	// ---- World ----
	/** Grand Line worlds keep permanently clear weather. */
	public boolean disableWeather = true;

	// ---- Gunsmith (Dawnreach) ----
	/** Gun prices at the Gunsmith: Slingshot, Musket. */
	public long[] gunPrices = {1_500, 12_000};

	// ---- Kill credit ----
	/** A death with no attacker of its own (fire, a fall, a lingering move) counts for the last player who hit within this many seconds. */
	public double killCreditSeconds = 15.0;

	// ---- Enemies ----
	/** Seconds after a regular island enemy dies before it respawns at its spawn point (bosses use their own timers). */
	public double enemyRespawnSeconds = 10.0;

	// ---- Saber (Verdant Wilds temple boss drop) ----
	/** Saber melee damage per hit, before the Sword stat and gear bonuses (Katana 6, Cutlass 5). */
	public double saberAttackDamage = 9.0;
	/** Saber attacks per second (Katana 1.8, Cutlass 2.0). */
	public double saberAttackSpeed = 2.0;
	/** Multiplier on the Saber's Z/X/C move damage (base 10, 12, 14). */
	public double saberMoveDamageMultiplier = 1.3;

	// ---- Saber Warden (Verdant Wilds temple boss) ----
	/** Boss health for one player. */
	public double saberBossHealth = 400.0;
	/** Extra health per additional player in the temple when the fight starts (0.5 = +50% each). */
	public double saberBossHealthPerExtraPlayer = 0.5;
	/** Damage of a plain sword swing. */
	public double saberBossSwingDamage = 6.0;
	/** Saber Slash damage. */
	public double saberBossSlashDamage = 9.0;
	/** Flying Saber Slash damage. */
	public double saberBossFlyingDamage = 8.0;
	/** Heavy Combo: damage of each of the first two cuts, and of the finishing cut. */
	public double saberBossComboDamage = 5.0;
	public double saberBossComboFinisherDamage = 9.0;
	/** No single boss hit takes more than this percent of a player's maximum health. */
	public double saberBossMaxHitPercent = 35.0;
	/** Seconds after the boss is defeated before it returns. */
	public double saberBossRespawnSeconds = 600.0;

	// ---- Swordsmith (Millbrook Village) ----
	/** Sword prices at the Swordsmith: Katana, Cutlass. */
	public long[] swordPrices = {8_000, 2_500};

	// ---- Ships ----
	/** Ship prices: Driftwood Sloop, Corsair Brig, Stormbreaker Galleon. */
	public long[] shipPrices = {5_000, 60_000, 300_000};
	/** Level needed to buy each ship. */
	public int[] shipMinLevels = {1, 60, 200};
	/** Hull points of each ship. */
	public double[] shipHull = {300, 700, 1500};
	/** Seconds between Call Ship uses. */
	public int shipCallCooldownSeconds = 30;
	/** Seconds before a sunk ship can be launched again. */
	public int shipSinkCooldownSeconds = 60;
	/** Hull regained per second (fraction of max) after 15 seconds without damage. */
	public double shipRepairPerSecond = 0.01;

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
		reputationMax = Math.max(0, reputationMax);
		reputationEnemyCap = Math.max(0, Math.min(reputationEnemyCap, reputationMax));
		reputationBossCap = Math.max(0, Math.min(reputationBossCap, reputationMax));
		reputationPerEnemyLevel = Math.max(0, reputationPerEnemyLevel);
		reputationPerBossLevel = Math.max(0, reputationPerBossLevel);
		pvpLevelRatio = Math.max(0, Math.min(1, pvpLevelRatio));
		pvpPairLimit = Math.max(0, pvpPairLimit);
		pvpPairWindowHours = Math.max(0, pvpPairWindowHours);
		combatTagSeconds = Math.max(0, combatTagSeconds);
		pvpDamagePerTier = Math.max(0, pvpDamagePerTier);
		pvpDefensePerTier = Math.max(0, pvpDefensePerTier);
		blackCapeHealth = Math.max(0, Math.min(blackCapeHealth, 200));
		blackCapeEnergy = Math.max(0, Math.min(blackCapeEnergy, 1000));
		pinkCoatHealth = Math.max(0, Math.min(pinkCoatHealth, 200));
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
		if (gunPrices == null || gunPrices.length != 2) {
			gunPrices = defaults().gunPrices;
		}
		for (int i = 0; i < gunPrices.length; i++) {
			gunPrices[i] = clamp(gunPrices[i], 0, maxCurrency);
		}
		enemyRespawnSeconds = clamp(enemyRespawnSeconds, 0, 3600);
		killCreditSeconds = clamp(killCreditSeconds, 0, 300);
		saberAttackDamage = clamp(saberAttackDamage, 1, 100);
		saberAttackSpeed = clamp(saberAttackSpeed, 0.5, 4);
		saberMoveDamageMultiplier = clamp(saberMoveDamageMultiplier, 0.1, 10);
		saberBossHealth = clamp(saberBossHealth, 1, 1_000_000);
		saberBossHealthPerExtraPlayer = clamp(saberBossHealthPerExtraPlayer, 0, 10);
		saberBossSwingDamage = clamp(saberBossSwingDamage, 0, 1000);
		saberBossSlashDamage = clamp(saberBossSlashDamage, 0, 1000);
		saberBossFlyingDamage = clamp(saberBossFlyingDamage, 0, 1000);
		saberBossComboDamage = clamp(saberBossComboDamage, 0, 1000);
		saberBossComboFinisherDamage = clamp(saberBossComboFinisherDamage, 0, 1000);
		saberBossMaxHitPercent = clamp(saberBossMaxHitPercent, 1, 100);
		saberBossRespawnSeconds = clamp(saberBossRespawnSeconds, 0, 86_400);
		if (swordPrices == null || swordPrices.length != 2) {
			swordPrices = defaults().swordPrices;
		}
		for (int i = 0; i < swordPrices.length; i++) {
			swordPrices[i] = clamp(swordPrices[i], 0, maxCurrency);
		}
		if (shipPrices == null || shipPrices.length != 3) {
			shipPrices = defaults().shipPrices;
		}
		if (shipMinLevels == null || shipMinLevels.length != 3) {
			shipMinLevels = defaults().shipMinLevels;
		}
		if (shipHull == null || shipHull.length != 3) {
			shipHull = defaults().shipHull;
		}
		for (int i = 0; i < 3; i++) {
			shipPrices[i] = clamp(shipPrices[i], 0, maxCurrency);
			shipMinLevels[i] = clamp(shipMinLevels[i], 1, 100_000);
			shipHull[i] = clamp(shipHull[i], 1, 1_000_000);
		}
		shipCallCooldownSeconds = clamp(shipCallCooldownSeconds, 0, 3600);
		shipSinkCooldownSeconds = clamp(shipSinkCooldownSeconds, 0, 3600);
		shipRepairPerSecond = clamp(shipRepairPerSecond, 0, 1);
		baseEnergy = clamp(baseEnergy, 1, 1_000_000);
		energyPerLevel = clamp(energyPerLevel, 0, 10_000);
		energyRegenPerSecond = clamp(energyRegenPerSecond, 0, 10);
		if (fruitPrices == null || fruitPrices.length != 5) {
			fruitPrices = defaults().fruitPrices;
		}
		for (int i = 0; i < fruitPrices.length; i++) {
			fruitPrices[i] = clamp(fruitPrices[i], 0, maxCurrency);
		}
		baseHealthRegenPercentPerSecond = clamp(baseHealthRegenPercentPerSecond, 0, 100);
		oceanDamagePercentPerSecond = clamp(oceanDamagePercentPerSecond, 0, 100);
		oceanMinSubmersion = clamp(oceanMinSubmersion, 0.05, 2.0);
		if (marineSpawnIsland == null || marineSpawnIsland.isBlank()) {
			marineSpawnIsland = defaults().marineSpawnIsland;
		}
		if (spawnIsland == null || spawnIsland.isBlank()) {
			spawnIsland = defaults().spawnIsland;
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
