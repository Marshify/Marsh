package com.grandline.network.payload;

import com.grandline.GrandLineMod;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.FriendlyByteBuf;

/**
 * Client → server: asks the server to re-send the player's profile and cooldowns.
 * Carries no data, so it cannot influence authoritative state. Rate limited server-side.
 */
public record RequestProfileSyncPayload() implements FabricPacket {
	public static final RequestProfileSyncPayload INSTANCE = new RequestProfileSyncPayload();
	public static final PacketType<RequestProfileSyncPayload> TYPE =
			PacketType.create(GrandLineMod.id("request_profile_sync"), buf -> INSTANCE);

	@Override
	public void write(FriendlyByteBuf buf) {
	}

	@Override
	public PacketType<?> getType() {
		return TYPE;
	}
}
