package com.grandline.network.payload;

import com.grandline.GrandLineMod;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.FriendlyByteBuf;

/** Client → server: the player is holding (or let go of) the reel, or gave up. */
public record FishingInputPayload(boolean holding, boolean giveUp) implements FabricPacket {
	public static final PacketType<FishingInputPayload> TYPE = PacketType.create(GrandLineMod.id("fishing_input"),
			buf -> new FishingInputPayload(buf.readBoolean(), buf.readBoolean()));

	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeBoolean(holding);
		buf.writeBoolean(giveUp);
	}

	@Override
	public PacketType<?> getType() {
		return TYPE;
	}
}
