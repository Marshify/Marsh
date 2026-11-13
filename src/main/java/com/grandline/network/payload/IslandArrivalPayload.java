package com.grandline.network.payload;

import com.grandline.GrandLineMod;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.FriendlyByteBuf;

/** Server → client: the player has arrived on an island; show its name banner. */
public record IslandArrivalPayload(String name, int level, int sea) implements FabricPacket {
	public static final PacketType<IslandArrivalPayload> TYPE = PacketType.create(GrandLineMod.id("island_arrival"),
			buf -> new IslandArrivalPayload(buf.readUtf(64), buf.readVarInt(), buf.readVarInt()));

	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeUtf(name, 64);
		buf.writeVarInt(level);
		buf.writeVarInt(sea);
	}

	@Override
	public PacketType<?> getType() {
		return TYPE;
	}
}
