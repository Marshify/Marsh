package com.grandline.network.payload;

import com.grandline.GrandLineMod;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

/**
 * Server → client: this player's full race progression (the saved RaceData as NBT), plus the live state: ticks left on
 * the V3 ability and V4 awakening, and the world time the V3 ability is ready again.
 */
public record RaceSyncPayload(CompoundTag data, int v3Ticks, int v4Ticks, int v3Ready) implements FabricPacket {
	public static final PacketType<RaceSyncPayload> TYPE = PacketType.create(GrandLineMod.id("race_sync"),
			buf -> new RaceSyncPayload(buf.readNbt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt()));

	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeNbt(data);
		buf.writeVarInt(v3Ticks);
		buf.writeVarInt(v4Ticks);
		buf.writeVarInt(v3Ready);
	}

	@Override
	public PacketType<?> getType() {
		return TYPE;
	}
}
