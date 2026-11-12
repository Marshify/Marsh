package com.grandline.player;

import com.mojang.serialization.Codec;
import net.minecraft.network.FriendlyByteBuf;

import java.util.EnumMap;
import java.util.Map;

/**
 * Immutable record of how many points a player has invested in each {@link StatType}.
 * Stats that were never invested in are simply absent (treated as 0).
 */
public record PlayerStats(Map<StatType, Integer> points) {
	public static final PlayerStats EMPTY = new PlayerStats(Map.of());

	public static final Codec<PlayerStats> CODEC = Codec.unboundedMap(StatType.CODEC, Codec.INT)
			.xmap(PlayerStats::new, PlayerStats::points);

	public PlayerStats {
		EnumMap<StatType, Integer> copy = new EnumMap<>(StatType.class);
		points.forEach((stat, value) -> {
			if (stat != null && value != null && value > 0) {
				copy.put(stat, value);
			}
		});
		points = Map.copyOf(copy);
	}

	public int get(StatType stat) {
		return points.getOrDefault(stat, 0);
	}

	public PlayerStats with(StatType stat, int value) {
		EnumMap<StatType, Integer> copy = new EnumMap<>(StatType.class);
		copy.putAll(points);
		copy.put(stat, value);
		return new PlayerStats(copy);
	}

	/** Total points invested across every stat. */
	public long total() {
		long total = 0;
		for (int value : points.values()) {
			total += value;
		}
		return total;
	}

	/** Returns a copy where every value is clamped to {@code [0, maxPerStat]}. */
	public PlayerStats clamped(int maxPerStat) {
		EnumMap<StatType, Integer> copy = new EnumMap<>(StatType.class);
		points.forEach((stat, value) -> copy.put(stat, Math.max(0, Math.min(maxPerStat, value))));
		return new PlayerStats(copy);
	}

	public void write(FriendlyByteBuf buf) {
		for (StatType stat : StatType.values()) {
			buf.writeVarInt(get(stat));
		}
	}

	public static PlayerStats read(FriendlyByteBuf buf) {
		EnumMap<StatType, Integer> values = new EnumMap<>(StatType.class);
		for (StatType stat : StatType.values()) {
			values.put(stat, buf.readVarInt());
		}
		return new PlayerStats(values);
	}
}
