package com.grandline.network.payload;

import com.grandline.GrandLineMod;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.FriendlyByteBuf;

/** Server → viewers: how a player looks: their Aura coating stage (-1 when Aura is off) and whether Instinct is on. */
public record TechniqueLookPayload(int entityId, int auraStage, boolean instinctOn) implements FabricPacket {
	public static final PacketType<TechniqueLookPayload> TYPE = PacketType.create(GrandLineMod.id("technique_look"),
			buf -> new TechniqueLookPayload(buf.readVarInt(), buf.readVarInt(), buf.readBoolean()));

	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeVarInt(entityId);
		buf.writeVarInt(auraStage);
		buf.writeBoolean(instinctOn);
	}

	@Override
	public PacketType<?> getType() {
		return TYPE;
	}
}
