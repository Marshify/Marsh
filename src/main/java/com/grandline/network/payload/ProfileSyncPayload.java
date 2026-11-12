package com.grandline.network.payload;

import com.grandline.GrandLineMod;
import com.grandline.player.PlayerProfile;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server → client: the authoritative profile of the receiving player.
 * Clients only display this data; it is never sent back to the server.
 */
public record ProfileSyncPayload(PlayerProfile profile) implements CustomPacketPayload {
	public static final Type<ProfileSyncPayload> TYPE = new Type<>(GrandLineMod.id("profile_sync"));
	public static final StreamCodec<RegistryFriendlyByteBuf, ProfileSyncPayload> CODEC =
			PlayerProfile.STREAM_CODEC.map(ProfileSyncPayload::new, ProfileSyncPayload::profile).cast();

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
