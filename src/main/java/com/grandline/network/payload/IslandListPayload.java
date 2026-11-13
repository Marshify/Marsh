package com.grandline.network.payload;

import com.grandline.GrandLineMod;
import com.grandline.world.Island;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;

/** Server → client: the islands on this player's chart, and which of them the player has discovered (by id). */
public record IslandListPayload(List<Island> islands, List<Long> discovered) implements FabricPacket {
	public static final int MAX_ISLANDS = 4096;

	public static final PacketType<IslandListPayload> TYPE = PacketType.create(GrandLineMod.id("island_list"), IslandListPayload::read);

	private static IslandListPayload read(FriendlyByteBuf buf) {
		int size = buf.readVarInt();
		if (size < 0 || size > MAX_ISLANDS) {
			throw new IllegalArgumentException("Invalid island count " + size);
		}
		List<Island> islands = new ArrayList<>(size);
		for (int i = 0; i < size; i++) {
			islands.add(Island.read(buf));
		}
		int known = buf.readVarInt();
		if (known < 0 || known > MAX_ISLANDS) {
			throw new IllegalArgumentException("Invalid discovered count " + known);
		}
		List<Long> discovered = new ArrayList<>(known);
		for (int i = 0; i < known; i++) {
			discovered.add(buf.readLong());
		}
		return new IslandListPayload(islands, discovered);
	}

	@Override
	public void write(FriendlyByteBuf buf) {
		int size = Math.min(islands.size(), MAX_ISLANDS);
		buf.writeVarInt(size);
		for (int i = 0; i < size; i++) {
			islands.get(i).write(buf);
		}
		int known = Math.min(discovered.size(), MAX_ISLANDS);
		buf.writeVarInt(known);
		for (int i = 0; i < known; i++) {
			buf.writeLong(discovered.get(i));
		}
	}

	@Override
	public PacketType<?> getType() {
		return TYPE;
	}
}
