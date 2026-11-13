package com.grandline.player;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;

import java.util.HashMap;
import java.util.Map;

/**
 * The player's combat powers: equipped fighting style, eaten fruit (empty string = none), mastery experience per
 * power id, the fruit stash (fruit id → count) and when the next gacha roll is allowed (epoch milliseconds). Immutable. Power ids are {@code namespace:path} strings so unknown ids from removed content are kept
 * harmlessly instead of failing to load.
 */
public record PowerState(String fightingStyle, String fruit, Map<String, Long> mastery, Map<String, Integer> stash,
		long gachaReadyAtMillis) {
	public static final String DEFAULT_STYLE = "grandline:brawler";
	public static final PowerState DEFAULT = new PowerState(DEFAULT_STYLE, "", Map.of(), Map.of(), 0L);

	public static final Codec<PowerState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.STRING.optionalFieldOf("style", DEFAULT_STYLE).forGetter(PowerState::fightingStyle),
			Codec.STRING.optionalFieldOf("fruit", "").forGetter(PowerState::fruit),
			Codec.unboundedMap(Codec.STRING, Codec.LONG).optionalFieldOf("mastery", Map.of()).forGetter(PowerState::mastery),
			Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("stash", Map.of()).forGetter(PowerState::stash),
			Codec.LONG.optionalFieldOf("gacha_ready_at", 0L).forGetter(PowerState::gachaReadyAtMillis)
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
		Map<String, Integer> stashCopy = new HashMap<>();
		if (stash != null) {
			stash.forEach((id, count) -> {
				if (id != null && count != null && count > 0) {
					stashCopy.put(id, count);
				}
			});
		}
		stash = Map.copyOf(stashCopy);
		gachaReadyAtMillis = Math.max(0L, gachaReadyAtMillis);
	}

	public int stashCount(String fruitId) {
		return stash.getOrDefault(fruitId, 0);
	}

	public int stashTotal() {
		int total = 0;
		for (int count : stash.values()) {
			total += count;
		}
		return total;
	}

	/** Drops the eaten fruit and stash entries whose fruit no longer exists (removed from the roster). Mastery is kept. */
	public PowerState withoutUnknownFruits(java.util.function.Predicate<String> exists) {
		String keptFruit = fruit.isEmpty() || exists.test(fruit) ? fruit : "";
		Map<String, Integer> keptStash = new HashMap<>();
		stash.forEach((id, count) -> {
			if (exists.test(id)) {
				keptStash.put(id, count);
			}
		});
		return new PowerState(fightingStyle, keptFruit, mastery, keptStash, gachaReadyAtMillis);
	}

	public PowerState withStashCount(String fruitId, int count) {
		Map<String, Integer> copy = new HashMap<>(stash);
		if (count <= 0) {
			copy.remove(fruitId);
		} else {
			copy.put(fruitId, count);
		}
		return new PowerState(fightingStyle, fruit, mastery, copy, gachaReadyAtMillis);
	}

	public PowerState withGachaReadyAt(long millis) {
		return new PowerState(fightingStyle, fruit, mastery, stash, millis);
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
		return new PowerState(fightingStyle, fruit, copy, stash, gachaReadyAtMillis);
	}

	public PowerState withFruit(String newFruit) {
		return new PowerState(fightingStyle, newFruit, mastery, stash, gachaReadyAtMillis);
	}

	public PowerState withFightingStyle(String style) {
		return new PowerState(style, fruit, mastery, stash, gachaReadyAtMillis);
	}

	public void write(FriendlyByteBuf buf) {
		buf.writeUtf(fightingStyle, 256);
		buf.writeUtf(fruit, 256);
		buf.writeVarInt(mastery.size());
		mastery.forEach((id, xp) -> {
			buf.writeUtf(id, 256);
			buf.writeVarLong(xp);
		});
		buf.writeVarInt(stash.size());
		stash.forEach((id, count) -> {
			buf.writeUtf(id, 256);
			buf.writeVarInt(count);
		});
		buf.writeVarLong(gachaReadyAtMillis);
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
		int stashSize = buf.readVarInt();
		if (stashSize < 0 || stashSize > 1024) {
			throw new IllegalArgumentException("Invalid stash entry count: " + stashSize);
		}
		Map<String, Integer> stash = new HashMap<>();
		for (int i = 0; i < stashSize; i++) {
			stash.put(buf.readUtf(256), buf.readVarInt());
		}
		return new PowerState(style, fruit, mastery, stash, buf.readVarLong());
	}
}
