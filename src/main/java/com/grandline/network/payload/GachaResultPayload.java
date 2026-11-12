package com.grandline.network.payload;

import com.grandline.GrandLineMod;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.FriendlyByteBuf;

/** Server → client: the fruit the server rolled, so the client can play the reveal animation. Display-only. */
public record GachaResultPayload(String fruitId) implements FabricPacket {
	public static final PacketType<GachaResultPayload> TYPE = PacketType.create(GrandLineMod.id("gacha_result"),
			buf -> new GachaResultPayload(buf.readUtf(256)));

	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeUtf(fruitId, 256);
	}

	@Override
	public PacketType<?> getType() {
		return TYPE;
	}
}
