package com.grandline.network.payload;

import com.grandline.GrandLineMod;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.FriendlyByteBuf;

/**
 * Client → server: "spend {@code amount} of my unallocated points on stat {@code statId}".
 *
 * <p>Only an intent: the server checks the stat id, the amount, the player's available points and the per-stat cap,
 * and computes the result itself. The client never sends the resulting stat values.
 */
public record AllocateStatPayload(int statId, int amount) implements FabricPacket {
	/** Largest amount accepted in one request (the UI offers +1 / +5 / +10). */
	public static final int MAX_AMOUNT_PER_REQUEST = 100;

	public static final PacketType<AllocateStatPayload> TYPE = PacketType.create(GrandLineMod.id("allocate_stat"),
			buf -> new AllocateStatPayload(buf.readVarInt(), buf.readVarInt()));

	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeVarInt(statId);
		buf.writeVarInt(amount);
	}

	@Override
	public PacketType<?> getType() {
		return TYPE;
	}
}
