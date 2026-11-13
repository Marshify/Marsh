package com.grandline.network.payload;

import com.grandline.GrandLineMod;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.FriendlyByteBuf;

/** Server to client - on join, ask the player to pick a side (current side, -1 = none). */
public record TeamPromptPayload(int team) implements FabricPacket {
	public static final PacketType<TeamPromptPayload> TYPE = PacketType.create(GrandLineMod.id("team_prompt"), buf -> new TeamPromptPayload(buf.readVarInt()));

	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeVarInt(team);
	}

	@Override
	public PacketType<?> getType() {
		return TYPE;
	}
}
