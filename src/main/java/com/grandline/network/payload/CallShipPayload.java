package com.grandline.network.payload;

import com.grandline.GrandLineMod;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.FriendlyByteBuf;

/** Client → server: Call Ship key. The server picks the ship and the water and checks everything. */
public record CallShipPayload() implements FabricPacket {
	public static final PacketType<CallShipPayload> TYPE = PacketType.create(GrandLineMod.id("call_ship"), buf -> new CallShipPayload());

	@Override
	public void write(FriendlyByteBuf buf) {
	}

	@Override
	public PacketType<?> getType() {
		return TYPE;
	}
}
