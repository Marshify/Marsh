package com.grandline.network.payload;

import com.grandline.GrandLineMod;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.FriendlyByteBuf;

/** Server → client: the fight is over (caught or got away), with the mastery it earned. */
public record FishingEndPayload(boolean success, String item, long xp, boolean levelUp) implements FabricPacket {
	public static final PacketType<FishingEndPayload> TYPE = PacketType.create(GrandLineMod.id("fishing_end"),
			buf -> new FishingEndPayload(buf.readBoolean(), buf.readUtf(64), buf.readVarLong(), buf.readBoolean()));

	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeBoolean(success);
		buf.writeUtf(item, 64);
		buf.writeVarLong(xp);
		buf.writeBoolean(levelUp);
	}

	@Override
	public PacketType<?> getType() {
		return TYPE;
	}
}
