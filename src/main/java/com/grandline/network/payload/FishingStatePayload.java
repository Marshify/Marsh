package com.grandline.network.payload;

import com.grandline.GrandLineMod;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.FriendlyByteBuf;

/** Server → client: the catch minigame this tick (the server runs it; the client only draws it). */
public record FishingStatePayload(float fish, float zone, float progress, boolean inZone) implements FabricPacket {
	public static final PacketType<FishingStatePayload> TYPE = PacketType.create(GrandLineMod.id("fishing_state"),
			buf -> new FishingStatePayload(buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readBoolean()));

	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeFloat(fish);
		buf.writeFloat(zone);
		buf.writeFloat(progress);
		buf.writeBoolean(inZone);
	}

	@Override
	public PacketType<?> getType() {
		return TYPE;
	}
}
