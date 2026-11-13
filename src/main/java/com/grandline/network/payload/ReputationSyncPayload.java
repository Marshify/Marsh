package com.grandline.network.payload;

import com.grandline.GrandLineMod;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.FriendlyByteBuf;

/** Server to client - the player's Bounty and Honor, active side (-1 none, 0 pirates, 1 marines), combat tag and risk. */
public record ReputationSyncPayload(long bounty, long honor, int side, int tagTicks, boolean atRisk) implements FabricPacket {
	public static final PacketType<ReputationSyncPayload> TYPE = PacketType.create(GrandLineMod.id("reputation_sync"),
			buf -> new ReputationSyncPayload(buf.readVarLong(), buf.readVarLong(), buf.readVarInt(), buf.readVarInt(), buf.readBoolean()));

	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeVarLong(bounty);
		buf.writeVarLong(honor);
		buf.writeVarInt(side);
		buf.writeVarInt(tagTicks);
		buf.writeBoolean(atRisk);
	}

	@Override
	public PacketType<?> getType() {
		return TYPE;
	}
}
