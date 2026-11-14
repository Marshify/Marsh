package com.grandline.network.payload;

import com.grandline.GrandLineMod;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.FriendlyByteBuf;

/** Server → clients: how a player looks (race, evolution, and 0 normal / 1 V3 active / 2 V4 awakened), for cosmetics and auras. */
public record RaceLookPayload(int entityId, String race, int stage, int mode) implements FabricPacket {
	public static final PacketType<RaceLookPayload> TYPE = PacketType.create(GrandLineMod.id("race_look"),
			buf -> new RaceLookPayload(buf.readVarInt(), buf.readUtf(16), buf.readVarInt(), buf.readVarInt()));

	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeVarInt(entityId);
		buf.writeUtf(race, 16);
		buf.writeVarInt(stage);
		buf.writeVarInt(mode);
	}

	@Override
	public PacketType<?> getType() {
		return TYPE;
	}
}
