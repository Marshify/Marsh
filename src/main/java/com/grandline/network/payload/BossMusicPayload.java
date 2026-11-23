package com.grandline.network.payload;

import com.grandline.GrandLineMod;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.FriendlyByteBuf;

/**
 * Server to client - the boss fight this player is taking part in, for the soundtrack: {@code tier} 0 none, 1 standard,
 * 2 major, 3 final (see {@link com.grandline.audio.BossEncounters}); {@code sea} picks that sea's boss music.
 */
public record BossMusicPayload(int tier, int sea) implements FabricPacket {
	public static final PacketType<BossMusicPayload> TYPE = PacketType.create(GrandLineMod.id("boss_music"),
			buf -> new BossMusicPayload(buf.readVarInt(), buf.readVarInt()));

	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeVarInt(tier);
		buf.writeVarInt(sea);
	}

	@Override
	public PacketType<?> getType() {
		return TYPE;
	}
}
