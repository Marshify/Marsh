package com.grandline.network.payload;

import com.grandline.GrandLineMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client → server: asks the server to re-send the player's profile and cooldowns.
 * Carries no data, so it cannot influence authoritative state. Rate limited server-side.
 */
public record RequestProfileSyncPayload() implements CustomPacketPayload {
	public static final RequestProfileSyncPayload INSTANCE = new RequestProfileSyncPayload();
	public static final Type<RequestProfileSyncPayload> TYPE = new Type<>(GrandLineMod.id("request_profile_sync"));
	public static final StreamCodec<RegistryFriendlyByteBuf, RequestProfileSyncPayload> CODEC = StreamCodec.unit(INSTANCE);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
