package com.grandline.network.payload;

import com.grandline.GrandLineMod;
import com.grandline.player.PlayerProfile;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.FriendlyByteBuf;

/**
 * Server → client: the authoritative profile of the receiving player.
 * Clients only display this data; the server never registers a handler for it, so it cannot be sent back.
 */
public record ProfileSyncPayload(PlayerProfile profile) implements FabricPacket {
	public static final PacketType<ProfileSyncPayload> TYPE =
			PacketType.create(GrandLineMod.id("profile_sync"), buf -> new ProfileSyncPayload(PlayerProfile.read(buf)));

	@Override
	public void write(FriendlyByteBuf buf) {
		profile.write(buf);
	}

	@Override
	public PacketType<?> getType() {
		return TYPE;
	}
}
