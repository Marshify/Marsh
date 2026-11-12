package com.grandline.network.payload;

import com.grandline.GrandLineMod;
import com.grandline.world.Island;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;

/** Server → client: the islands this player has discovered, for the world map. */
public record IslandListPayload(List<Island> islands) implements FabricPacket {
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
		return new IslandListPayload(islands);
	}

	@Override
	public void write(FriendlyByteBuf buf) {
		int size = Math.min(islands.size(), MAX_ISLANDS);
		buf.writeVarInt(size);
		for (int i = 0; i < size; i++) {
			islands.get(i).write(buf);
		}
	}

	@Override
	public PacketType<?> getType() {
		return TYPE;
	}
}
