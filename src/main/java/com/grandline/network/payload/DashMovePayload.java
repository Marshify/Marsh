package com.grandline.network.payload;

import com.grandline.GrandLineMod;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.FriendlyByteBuf;

/**
 * Server → dashing player: the dash was approved (energy spent, cooldown started); move {@code distance} blocks along
 * ({@code dirX}, {@code dirZ}) over {@code ticks} ticks. The player's own client moves itself, as Minecraft players
 * always do, so the dash is smooth and collides exactly.
 */
public record DashMovePayload(float dirX, float dirZ, float distance, int ticks) implements FabricPacket {
	public static final PacketType<DashMovePayload> TYPE = PacketType.create(GrandLineMod.id("dash_move"),
			buf -> new DashMovePayload(buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readVarInt()));

	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeFloat(dirX);
		buf.writeFloat(dirZ);
		buf.writeFloat(distance);
		buf.writeVarInt(ticks);
	}

	@Override
	public PacketType<?> getType() {
		return TYPE;
	}
}
