package com.grandline.player;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;

import java.util.HashMap;
import java.util.Map;

/**
 * The player's combat powers: equipped fighting style, eaten fruit (empty string = none) and mastery experience per
 * power id. Immutable. Power ids are {@code namespace:path} strings so unknown ids from removed content are kept
 * harmlessly instead of failing to load.
 */
public record PowerState(String fightingStyle, String fruit, Map<String, Long> mastery) {
	public static final String DEFAULT_STYLE = "grandline:brawler";
	public static final PowerState DEFAULT = new PowerState(DEFAULT_STYLE, "", Map.of());

	public static final Codec<PowerState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.STRING.optionalFieldOf("style", DEFAULT_STYLE).forGetter(PowerState::fightingStyle),
			Codec.STRING.optionalFieldOf("fruit", "").forGetter(PowerState::fruit),
			Codec.unboundedMap(Codec.STRING, Codec.LONG).optionalFieldOf("mastery", Map.of()).forGetter(PowerState::mastery)
	).apply(instance, PowerState::new));

	public PowerState {
		fightingStyle = fightingStyle == null || fightingStyle.isEmpty() ? DEFAULT_STYLE : fightingStyle;
		fruit = fruit == null ? "" : fruit;
		Map<String, Long> copy = new HashMap<>();
		if (mastery != null) {
			mastery.forEach((id, xp) -> {
				if (id != null && xp != null && xp > 0) {
					copy.put(id, xp);
				}
			});
		}
		mastery = Map.copyOf(copy);
	}

	public boolean hasFruit() {
		return !fruit.isEmpty();
	}

	public long masteryXp(String powerId) {
		return mastery.getOrDefault(powerId, 0L);
	}

	public PowerState withMasteryXp(String powerId, long xp) {
		Map<String, Long> copy = new HashMap<>(mastery);
		copy.put(powerId, Math.max(0L, xp));
		return new PowerState(fightingStyle, fruit, copy);
	}

	public PowerState withFruit(String newFruit) {
		return new PowerState(fightingStyle, newFruit, mastery);
	}

	public PowerState withFightingStyle(String style) {
		return new PowerState(style, fruit, mastery);
	}

	public void write(FriendlyByteBuf buf) {
		buf.writeUtf(fightingStyle, 256);
		buf.writeUtf(fruit, 256);
		buf.writeVarInt(mastery.size());
		mastery.forEach((id, xp) -> {
			buf.writeUtf(id, 256);
			buf.writeVarLong(xp);
		});
	}

	public static PowerState read(FriendlyByteBuf buf) {
		String style = buf.readUtf(256);
		String fruit = buf.readUtf(256);
		int size = buf.readVarInt();
		if (size < 0 || size > 1024) {
			throw new IllegalArgumentException("Invalid mastery entry count: " + size);
		}
		Map<String, Long> mastery = new HashMap<>();
		for (int i = 0; i < size; i++) {
			mastery.put(buf.readUtf(256), buf.readVarLong());
		}
		return new PowerState(style, fruit, mastery);
	}
}
