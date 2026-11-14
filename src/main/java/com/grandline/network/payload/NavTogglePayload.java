package com.grandline.network.payload;

import com.grandline.GrandLineMod;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.FriendlyByteBuf;

/** Client → server: turn quest navigation on or off. */
public record NavTogglePayload(boolean enabled) implements FabricPacket {
	public static final PacketType<NavTogglePayload> TYPE = PacketType.create(GrandLineMod.id("nav_toggle"),
			buf -> new NavTogglePayload(buf.readBoolean()));

	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeBoolean(enabled);
	}

	@Override
	public PacketType<?> getType() {
		return TYPE;
	}
}
