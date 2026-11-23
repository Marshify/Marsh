package com.grandline.network.payload;

import com.grandline.GrandLineMod;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.FriendlyByteBuf;

import java.util.Map;

/**
 * Server → client: the renamed names (see GrandLineNames): translation key → new text, and original island name → new
 * island name. Sent on join and whenever a name changes, so every player sees the server's names.
 */
public record NamesSyncPayload(Map<String, String> keys, Map<String, String> islands) implements FabricPacket {
	public static final PacketType<NamesSyncPayload> TYPE = PacketType.create(GrandLineMod.id("names_sync"), buf -> new NamesSyncPayload(
			buf.readMap(FriendlyByteBuf::readUtf, FriendlyByteBuf::readUtf),
			buf.readMap(FriendlyByteBuf::readUtf, FriendlyByteBuf::readUtf)));

	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeMap(keys, FriendlyByteBuf::writeUtf, FriendlyByteBuf::writeUtf);
		buf.writeMap(islands, FriendlyByteBuf::writeUtf, FriendlyByteBuf::writeUtf);
	}

	@Override
	public PacketType<?> getType() {
		return TYPE;
	}
}
