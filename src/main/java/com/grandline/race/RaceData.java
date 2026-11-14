package com.grandline.race;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A player's race progression, saved with the player and kept through death. {@code stage} is the evolution (1-4).
 * {@code flags} hold one-time facts (unlocks, accepted and finished quests, trials cleared...), {@code counters} hold
 * progress numbers and timestamps (quest progress, awakening points, cooldown ends in world time), {@code upgrades}
 * the chosen V4 upgrades, and {@code energy} the awakening meter (0-100).
 */
public record RaceData(String race, int stage, List<String> flags, Map<String, Long> counters, List<String> upgrades, int energy) {
	public static final Codec<RaceData> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.STRING.fieldOf("race").forGetter(RaceData::race),
			Codec.INT.fieldOf("stage").forGetter(RaceData::stage),
			Codec.STRING.listOf().optionalFieldOf("flags", List.of()).forGetter(RaceData::flags),
			Codec.unboundedMap(Codec.STRING, Codec.LONG).optionalFieldOf("counters", Map.of()).forGetter(RaceData::counters),
			Codec.STRING.listOf().optionalFieldOf("upgrades", List.of()).forGetter(RaceData::upgrades),
			Codec.INT.optionalFieldOf("energy", 0).forGetter(RaceData::energy)).apply(i, RaceData::new));

	public static RaceData fresh(Race race) {
		return new RaceData(race.id(), 1, List.of(), Map.of(), List.of(), 0);
	}

	public Race kind() {
		return Race.byId(race);
	}

	public boolean has(String flag) {
		return flags.contains(flag);
	}

	public long counter(String key) {
		return counters.getOrDefault(key, 0L);
	}

	public RaceData withFlag(String flag) {
		if (flags.contains(flag)) {
			return this;
		}
		List<String> f = new ArrayList<>(flags);
		f.add(flag);
		return new RaceData(race, stage, List.copyOf(f), counters, upgrades, energy);
	}

	public RaceData withoutFlag(String flag) {
		List<String> f = new ArrayList<>(flags);
		f.remove(flag);
		return new RaceData(race, stage, List.copyOf(f), counters, upgrades, energy);
	}

	public RaceData withCounter(String key, long value) {
		Map<String, Long> c = new HashMap<>(counters);
		c.put(key, value);
		return new RaceData(race, stage, flags, Map.copyOf(c), upgrades, energy);
	}

	public RaceData withStage(int s) {
		return new RaceData(race, Math.max(1, Math.min(4, s)), flags, counters, upgrades, energy);
	}

	public RaceData withRace(Race r) {
		return new RaceData(r.id(), stage, flags, counters, upgrades, energy);
	}

	public RaceData withUpgrades(List<String> u) {
		return new RaceData(race, stage, flags, counters, List.copyOf(u), energy);
	}

	public RaceData withEnergy(int e) {
		return new RaceData(race, stage, flags, counters, upgrades, Math.max(0, Math.min(100, e)));
	}
}
