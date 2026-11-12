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
 * @param dataVersion           schema version, used for future migrations
 * @param level                 current level, starting at 1
 * @param experience            experience accumulated towards the next level
 * @param currency              spendable currency
 * @param unallocatedStatPoints stat points earned but not yet spent
 */
public record PlayerProfile(int dataVersion, int level, long experience, long currency, int unallocatedStatPoints) {
	public static final int CURRENT_DATA_VERSION = 1;
	public static final int MIN_LEVEL = 1;

	/**
	 * Disk codec. Every field is optional with a safe default, so older or partially corrupted saves still load.
	 * Decoded values are sanitized; see {@link #sanitized()}.
	 */
	public static final Codec<PlayerProfile> CODEC = RecordCodecBuilder.<PlayerProfile>create(instance -> instance.group(
			Codec.INT.optionalFieldOf("data_version", CURRENT_DATA_VERSION).forGetter(PlayerProfile::dataVersion),
			Codec.INT.optionalFieldOf("level", MIN_LEVEL).forGetter(PlayerProfile::level),
			Codec.LONG.optionalFieldOf("experience", 0L).forGetter(PlayerProfile::experience),
			Codec.LONG.optionalFieldOf("currency", 0L).forGetter(PlayerProfile::currency),
			Codec.INT.optionalFieldOf("unallocated_stat_points", 0).forGetter(PlayerProfile::unallocatedStatPoints)
	).apply(instance, PlayerProfile::new)).xmap(PlayerProfile::sanitized, profile -> profile);

	/** Writes the profile for server → client synchronization. Mirror of {@link #read}. */
	public void write(FriendlyByteBuf buf) {
		buf.writeVarInt(dataVersion);
		buf.writeVarInt(level);
		buf.writeVarLong(experience);
		buf.writeVarLong(currency);
		buf.writeVarInt(unallocatedStatPoints);
	}

	public static PlayerProfile read(FriendlyByteBuf buf) {
		return new PlayerProfile(buf.readVarInt(), buf.readVarInt(), buf.readVarLong(), buf.readVarLong(), buf.readVarInt());
	}

	public static PlayerProfile createDefault(long startingCurrency) {
		return new PlayerProfile(CURRENT_DATA_VERSION, MIN_LEVEL, 0L, Math.max(0L, startingCurrency), 0);
	}

	/**
	 * Returns a copy with every field forced into a valid range. Applied after loading from disk so that a
	 * hand-edited or corrupted save cannot produce negative currency, level 0, etc.
	 */
	public PlayerProfile sanitized() {
		return new PlayerProfile(
				CURRENT_DATA_VERSION,
				Math.max(MIN_LEVEL, level),
				Math.max(0L, experience),
				Math.max(0L, currency),
				Math.max(0, unallocatedStatPoints)
		);
	}

	public PlayerProfile withLevel(int newLevel) {
		return new PlayerProfile(dataVersion, newLevel, experience, currency, unallocatedStatPoints);
	}

	public PlayerProfile withExperience(long newExperience) {
		return new PlayerProfile(dataVersion, level, newExperience, currency, unallocatedStatPoints);
	}

	public PlayerProfile withCurrency(long newCurrency) {
		return new PlayerProfile(dataVersion, level, experience, newCurrency, unallocatedStatPoints);
	}

	public PlayerProfile withUnallocatedStatPoints(int points) {
		return new PlayerProfile(dataVersion, level, experience, currency, points);
	}
}
