package com.grandline.player;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerProfileCodecTest {
	private static final PlayerProfile SAMPLE = new PlayerProfile(PlayerProfile.CURRENT_DATA_VERSION, 42, 1234L, 987_654_321L, 17,
			PlayerStats.EMPTY.with(StatType.MELEE, 30).with(StatType.DEFENSE, 12).with(StatType.FRUIT, 5),
			new PowerState("grandline:brawler", "grandline:ember", java.util.Map.of("grandline:brawler", 120L, "grandline:ember", 7L),
					java.util.Map.of("grandline:gale", 2), 123_456L));

	@Test
	void nbtRoundTripPreservesEveryField() {
		Tag encoded = PlayerProfile.CODEC.encodeStart(NbtOps.INSTANCE, SAMPLE).getOrThrow(false, message -> { });
		PlayerProfile decoded = PlayerProfile.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow(false, message -> { });
		assertEquals(SAMPLE, decoded);
	}

	@Test
	void missingFieldsFallBackToDefaults() {
		PlayerProfile decoded = PlayerProfile.CODEC.parse(NbtOps.INSTANCE, new CompoundTag()).getOrThrow(false, message -> { });
		assertEquals(PlayerProfile.createDefault(0), decoded);
	}

	@Test
	void corruptedValuesAreSanitizedOnLoad() {
		JsonObject json = new JsonObject();
		json.addProperty("level", -3);
		json.addProperty("experience", -50);
		json.addProperty("currency", -1);
		json.addProperty("unallocated_stat_points", -8);

		PlayerProfile decoded = PlayerProfile.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow(false, message -> { });
		assertEquals(new PlayerProfile(PlayerProfile.CURRENT_DATA_VERSION, 1, 0, 0, 0, PlayerStats.EMPTY, PowerState.DEFAULT), decoded);
	}

	/** A save written by milestone 1 (data version 1, no stats; data_version omitted because it was the default). */
	@Test
	void version1SaveMigratesToCurrentVersion() {
		CompoundTag v1 = new CompoundTag();
		v1.putInt("level", 3);
		v1.putLong("experience", 10L);
		v1.putLong("currency", 777L);
		v1.putInt("unallocated_stat_points", 6);

		PlayerProfile migrated = PlayerProfile.CODEC.parse(NbtOps.INSTANCE, v1).getOrThrow(false, message -> { });
		assertEquals(new PlayerProfile(PlayerProfile.CURRENT_DATA_VERSION, 3, 10L, 777L, 6, PlayerStats.EMPTY, PowerState.DEFAULT), migrated);
	}

	@Test
	void negativeStatValuesAreDroppedOnLoad() {
		JsonObject stats = new JsonObject();
		stats.addProperty("melee", -40);
		stats.addProperty("sword", 7);
		JsonObject json = new JsonObject();
		json.add("stats", stats);

		PlayerProfile decoded = PlayerProfile.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow(false, message -> { });
		assertEquals(0, decoded.stats().get(StatType.MELEE));
		assertEquals(7, decoded.stats().get(StatType.SWORD));
	}

	@Test
	void networkCodecRoundTrip() {
		FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
		SAMPLE.write(buffer);
		assertEquals(SAMPLE, PlayerProfile.read(buffer));
		assertEquals(0, buffer.readableBytes(), "codec must consume exactly what it wrote");
	}
}
