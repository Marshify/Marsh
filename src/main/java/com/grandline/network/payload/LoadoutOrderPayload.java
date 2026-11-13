package com.grandline.network.payload;

import com.grandline.GrandLineMod;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.FriendlyByteBuf;

import java.util.List;

/**
 * Loadout order, both directions: the server sends the saved order on join; the client sends a new order when the
 * loadout editor closes. The server only accepts a permutation of the four entry ids.
 */
public record LoadoutOrderPayload(List<Integer> order) implements FabricPacket {
	private static final int MAX = 8;
	public static final PacketType<LoadoutOrderPayload> TYPE = PacketType.create(GrandLineMod.id("loadout_order"), buf -> {
		int size = Math.min(buf.readVarInt(), MAX);
		Integer[] ids = new Integer[size];
		for (int i = 0; i < size; i++) {
			ids[i] = buf.readVarInt();
		}
		return new LoadoutOrderPayload(List.of(ids));
	});

	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeVarInt(order.size());
		order.forEach(buf::writeVarInt);
	}

	@Override
	public PacketType<?> getType() {
		return TYPE;
	}
}
