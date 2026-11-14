package com.grandline.network.payload;

import com.grandline.GrandLineMod;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.FriendlyByteBuf;

/** Server → client: where this player's quest navigation points (enabled flag, target name, island, position, and the NPC's entity id if it is loaded, else -1). */
public record NavTargetPayload(boolean enabled, String name, String island, int x, int y, int z, int entityId) implements FabricPacket {
	public static final PacketType<NavTargetPayload> TYPE = PacketType.create(GrandLineMod.id("nav_target"),
			buf -> new NavTargetPayload(buf.readBoolean(), buf.readUtf(64), buf.readUtf(64), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt()));

	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeBoolean(enabled);
		buf.writeUtf(name, 64);
		buf.writeUtf(island, 64);
		buf.writeVarInt(x);
		buf.writeVarInt(y);
		buf.writeVarInt(z);
		buf.writeVarInt(entityId);
	}

	@Override
	public PacketType<?> getType() {
		return TYPE;
	}
}
