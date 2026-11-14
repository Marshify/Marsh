package com.grandline.network.payload;

import com.grandline.GrandLineMod;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.FriendlyByteBuf;

/** Server → client: a fish is on the line; open the catch minigame. */
public record FishingStartPayload(String item, int tier, float difficulty, float zone) implements FabricPacket {
	public static final PacketType<FishingStartPayload> TYPE = PacketType.create(GrandLineMod.id("fishing_start"),
			buf -> new FishingStartPayload(buf.readUtf(64), buf.readVarInt(), buf.readFloat(), buf.readFloat()));

	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeUtf(item, 64);
		buf.writeVarInt(tier);
		buf.writeFloat(difficulty);
		buf.writeFloat(zone);
	}

	@Override
	public PacketType<?> getType() {
		return TYPE;
	}
}
