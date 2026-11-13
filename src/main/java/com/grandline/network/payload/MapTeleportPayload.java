package com.grandline.network.payload;

import com.grandline.GrandLineMod;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.FriendlyByteBuf;

/** Client to server - creative players clicked an island on the sea chart; the server checks creative mode itself. */
public record MapTeleportPayload(long islandId) implements FabricPacket {
	public static final PacketType<MapTeleportPayload> TYPE = PacketType.create(GrandLineMod.id("map_teleport"),
			buf -> new MapTeleportPayload(buf.readLong()));

	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeLong(islandId);
	}

	@Override
	public PacketType<?> getType() {
		return TYPE;
	}
}
