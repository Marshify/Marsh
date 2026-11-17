package com.grandline.network.payload;

import com.grandline.GrandLineMod;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.FriendlyByteBuf;

import java.util.LinkedHashMap;
import java.util.Map;

/** Server → client: the player's fish bag (fish item id → count), sent on join and whenever it changes. */
public record FishBagPayload(Map<String, Integer> fish) implements FabricPacket {
	public static final PacketType<FishBagPayload> TYPE = PacketType.create(GrandLineMod.id("fish_bag"), buf -> {
		int size = Math.min(buf.readVarInt(), 256);
		Map<String, Integer> map = new LinkedHashMap<>();
		for (int i = 0; i < size; i++) {
			map.put(buf.readUtf(256), buf.readVarInt());
		}
		return new FishBagPayload(map);
	});

	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeVarInt(fish.size());
		fish.forEach((id, n) -> {
			buf.writeUtf(id, 256);
			buf.writeVarInt(n);
		});
	}

	@Override
	public PacketType<?> getType() {
		return TYPE;
	}
}
