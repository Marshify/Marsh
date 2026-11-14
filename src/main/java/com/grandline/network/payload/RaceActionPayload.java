package com.grandline.network.payload;

import com.grandline.GrandLineMod;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.FriendlyByteBuf;

/**
 * Client → server: a race action. {@code action}: 0 use V3 ability, 1 awaken (V4), 2 choose upgrade {@code arg}
 * (tier * 2 + option). The server re-checks everything.
 */
public record RaceActionPayload(int action, int arg) implements FabricPacket {
	public static final int V3 = 0, V4 = 1, UPGRADE = 2;
	public static final PacketType<RaceActionPayload> TYPE = PacketType.create(GrandLineMod.id("race_action"),
			buf -> new RaceActionPayload(buf.readVarInt(), buf.readVarInt()));

	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeVarInt(action);
		buf.writeVarInt(arg);
	}

	@Override
	public PacketType<?> getType() {
		return TYPE;
	}
}
