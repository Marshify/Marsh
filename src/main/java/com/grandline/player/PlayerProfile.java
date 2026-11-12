package com.grandline.player;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;

/**
 * Immutable snapshot of a player's persistent RPG progression.
 *
 * <p>Profiles are only ever created or modified on the logical server (see {@link PlayerProfileService}).
 * Immutability means a profile handed to networking or commands can never be mutated behind the service's back.
 *
 * <p>Data versions: 1 = level/experience/currency/points (milestone 1); 2 = adds {@link PlayerStats}. Version 1 saves
 * load unchanged with empty stats, because every field is optional with a default. 3 = adds {@link PowerState}
 * (fighting style, fruit, mastery); older saves get the default Brawler style and no fruit.
 *
 * @param dataVersion           schema version, used for migrations
 * @param level                 current level, starting at 1
 * @param experience            experience accumulated towards the next level
 * @param currency              spendable currency
 * @param unallocatedStatPoints stat points earned but not yet spent
 * @param stats                 points invested per stat
 * @param powers                fighting style, fruit and mastery
 */
public record PlayerProfile(int dataVersion, int level, long experience, long currency, int unallocatedStatPoints, PlayerStats stats,
		PowerState powers) {
	public static final int CURRENT_DATA_VERSION = 3;
	public static final int MIN_LEVEL = 1;

	/**
	 * Disk codec. Every field is optional with a safe default, so older or partially corrupted saves still load.
	 * Decoded values are sanitized; see {@link #sanitized()}.
	 */
	public static final Codec<PlayerProfile> CODEC = RecordCodecBuilder.<PlayerProfile>create(instance -> instance.group(
			Codec.INT.optionalFieldOf("data_version", 1).forGetter(PlayerProfile::dataVersion),
			Codec.INT.optionalFieldOf("level", MIN_LEVEL).forGetter(PlayerProfile::level),
			Codec.LONG.optionalFieldOf("experience", 0L).forGetter(PlayerProfile::experience),
			Codec.LONG.optionalFieldOf("currency", 0L).forGetter(PlayerProfile::currency),
			Codec.INT.optionalFieldOf("unallocated_stat_points", 0).forGetter(PlayerProfile::unallocatedStatPoints),
			PlayerStats.CODEC.optionalFieldOf("stats", PlayerStats.EMPTY).forGetter(PlayerProfile::stats),
			PowerState.CODEC.optionalFieldOf("powers", PowerState.DEFAULT).forGetter(PlayerProfile::powers)
	).apply(instance, PlayerProfile::new)).xmap(PlayerProfile::sanitized, profile -> profile);

	public PlayerProfile {
		stats = stats == null ? PlayerStats.EMPTY : stats;
		powers = powers == null ? PowerState.DEFAULT : powers;
	}

	/** Writes the profile for server → client synchronization. Mirror of {@link #read}. */
	public void write(FriendlyByteBuf buf) {
		buf.writeVarInt(dataVersion);
		buf.writeVarInt(level);
		buf.writeVarLong(experience);
		buf.writeVarLong(currency);
		buf.writeVarInt(unallocatedStatPoints);
		stats.write(buf);
		powers.write(buf);
	}

	public static PlayerProfile read(FriendlyByteBuf buf) {
		return new PlayerProfile(buf.readVarInt(), buf.readVarInt(), buf.readVarLong(), buf.readVarLong(), buf.readVarInt(),
				PlayerStats.read(buf), PowerState.read(buf));
	}

	public static PlayerProfile createDefault(long startingCurrency) {
		return new PlayerProfile(CURRENT_DATA_VERSION, MIN_LEVEL, 0L, Math.max(0L, startingCurrency), 0, PlayerStats.EMPTY, PowerState.DEFAULT);
	}

	/**
	 * Returns a copy with every field forced into a valid range and the data version upgraded to the current one.
	 * Applied after loading from disk so that a hand-edited or corrupted save cannot produce negative currency,
	 * level 0, negative stats, etc.
	 */
	public PlayerProfile sanitized() {
		return new PlayerProfile(
				CURRENT_DATA_VERSION,
				Math.max(MIN_LEVEL, level),
				Math.max(0L, experience),
				Math.max(0L, currency),
				Math.max(0, unallocatedStatPoints),
				stats.clamped(Integer.MAX_VALUE),
				powers
		);
	}

	public PlayerProfile withLevel(int newLevel) {
		return new PlayerProfile(dataVersion, newLevel, experience, currency, unallocatedStatPoints, stats, powers);
	}

	public PlayerProfile withExperience(long newExperience) {
		return new PlayerProfile(dataVersion, level, newExperience, currency, unallocatedStatPoints, stats, powers);
	}

	public PlayerProfile withCurrency(long newCurrency) {
		return new PlayerProfile(dataVersion, level, experience, newCurrency, unallocatedStatPoints, stats, powers);
	}

	public PlayerProfile withUnallocatedStatPoints(int points) {
		return new PlayerProfile(dataVersion, level, experience, currency, points, stats, powers);
	}

	public PlayerProfile withPowers(PowerState newPowers) {
		return new PlayerProfile(dataVersion, level, experience, currency, unallocatedStatPoints, stats, newPowers);
	}

	public PlayerProfile withStats(PlayerStats newStats) {
		return new PlayerProfile(dataVersion, level, experience, currency, unallocatedStatPoints, newStats, powers);
	}
}
