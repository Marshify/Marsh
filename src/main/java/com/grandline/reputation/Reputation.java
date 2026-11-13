package com.grandline.reputation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * A player's two independent totals (Pirate Bounty and Marine Honor) and a message to show at their next login
 * (e.g. the result of a combat-tag disconnect). Saved with the player; kept on death.
 */
public record Reputation(long bounty, long honor, String notice) {
	public static final Reputation ZERO = new Reputation(0, 0, "");

	public static final Codec<Reputation> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.LONG.optionalFieldOf("bounty", 0L).forGetter(Reputation::bounty),
			Codec.LONG.optionalFieldOf("honor", 0L).forGetter(Reputation::honor),
			Codec.STRING.optionalFieldOf("notice", "").forGetter(Reputation::notice)
	).apply(instance, Reputation::new));

	public long get(boolean pirate) {
		return pirate ? bounty : honor;
	}

	public Reputation with(boolean pirate, long value) {
		return pirate ? new Reputation(value, honor, notice) : new Reputation(bounty, value, notice);
	}

	public Reputation withNotice(String text) {
		return new Reputation(bounty, honor, text);
	}
}
