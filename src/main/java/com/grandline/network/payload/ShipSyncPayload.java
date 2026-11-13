package com.grandline.network.payload;

import com.grandline.GrandLineMod;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;

/** Server → client: owned ships, the one Call Ship launches, and the sink cooldown left. */
public record ShipSyncPayload(List<String> owned, String selected, long sunkMillisLeft) implements FabricPacket {
	public static final PacketType<ShipSyncPayload> TYPE = PacketType.create(GrandLineMod.id("ship_sync"), buf -> {
		int n = Math.min(buf.readVarInt(), 16);
		List<String> owned = new ArrayList<>();
		for (int i = 0; i < n; i++) {
			owned.add(buf.readUtf(32));
		}
		return new ShipSyncPayload(List.copyOf(owned), buf.readUtf(32), buf.readVarLong());
	});

	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeVarInt(owned.size());
		owned.forEach(id -> buf.writeUtf(id, 32));
		buf.writeUtf(selected, 32);
		buf.writeVarLong(sunkMillisLeft);
	}

	@Override
	public PacketType<?> getType() {
		return TYPE;
	}
}
