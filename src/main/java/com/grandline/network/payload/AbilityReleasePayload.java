package com.grandline.network.payload;

import com.grandline.GrandLineMod;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.FriendlyByteBuf;

/**
 * Client → server: the player let go of a held move key. The server measures the charge time itself from its own tick
 * counter; this packet carries no timing, and a missing release is handled by the server's maximum charge time.
 */
public record AbilityReleasePayload(int source, int slot) implements FabricPacket {
	public static final PacketType<AbilityReleasePayload> TYPE = PacketType.create(GrandLineMod.id("ability_release"),
			buf -> new AbilityReleasePayload(buf.readVarInt(), buf.readVarInt()));

	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeVarInt(source);
		buf.writeVarInt(slot);
	}

	@Override
	public PacketType<?> getType() {
		return TYPE;
	}
}
