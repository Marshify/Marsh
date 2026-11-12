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
	private static final PlayerProfile SAMPLE = new PlayerProfile(PlayerProfile.CURRENT_DATA_VERSION, 42, 1234L, 987_654_321L, 17);

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
		assertEquals(new PlayerProfile(PlayerProfile.CURRENT_DATA_VERSION, 1, 0, 0, 0), decoded);
	}

	@Test
	void networkCodecRoundTrip() {
		FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
		SAMPLE.write(buffer);
		assertEquals(SAMPLE, PlayerProfile.read(buffer));
		assertEquals(0, buffer.readableBytes(), "codec must consume exactly what it wrote");
	}
}
