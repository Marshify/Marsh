package com.grandline.network.payload;

import com.grandline.GrandLineMod;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.FriendlyByteBuf;

/** Server → client: current and maximum energy, for the HUD. Display-only. */
public record EnergySyncPayload(float current, float max) implements FabricPacket {
	public static final PacketType<EnergySyncPayload> TYPE = PacketType.create(GrandLineMod.id("energy_sync"),
			buf -> new EnergySyncPayload(buf.readFloat(), buf.readFloat()));

	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeFloat(current);
		buf.writeFloat(max);
	}

	@Override
	public PacketType<?> getType() {
		return TYPE;
	}
}
