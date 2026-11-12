package com.grandline.network.payload;

import com.grandline.GrandLineMod;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.FriendlyByteBuf;

/** Server → client: outcome of a fruit/shop action, so open screens can show feedback. {@code key} is a translation key. */
public record ActionResultPayload(boolean success, String key) implements FabricPacket {
	public static final PacketType<ActionResultPayload> TYPE = PacketType.create(GrandLineMod.id("action_result"),
			buf -> new ActionResultPayload(buf.readBoolean(), buf.readUtf(256)));

	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeBoolean(success);
		buf.writeUtf(key, 256);
	}

	@Override
	public PacketType<?> getType() {
		return TYPE;
	}
}
