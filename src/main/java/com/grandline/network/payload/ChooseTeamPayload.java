package com.grandline.network.payload;

import com.grandline.GrandLineMod;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.FriendlyByteBuf;

/** Client to server - join a side (0 pirates, 1 marines); the server validates. */
public record ChooseTeamPayload(int team) implements FabricPacket {
	public static final PacketType<ChooseTeamPayload> TYPE = PacketType.create(GrandLineMod.id("choose_team"), buf -> new ChooseTeamPayload(buf.readVarInt()));

	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeVarInt(team);
	}

	@Override
	public PacketType<?> getType() {
		return TYPE;
	}
}
