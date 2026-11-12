package com.grandline.network.payload;

import com.grandline.GrandLineMod;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * Server → client: the full set of active ability cooldowns for the receiving player, expressed as remaining ticks.
 * The server remains the only authority on whether an ability is ready; the client uses this purely for display.
 */
public record CooldownSyncPayload(List<Entry> cooldowns) implements FabricPacket {
	/** Hard cap so a bug can never produce an oversized packet. */
	public static final int MAX_ENTRIES = 256;

	public static final PacketType<CooldownSyncPayload> TYPE = PacketType.create(GrandLineMod.id("cooldown_sync"), CooldownSyncPayload::read);

	public CooldownSyncPayload {
		if (cooldowns.size() > MAX_ENTRIES) {
			throw new IllegalArgumentException("Too many cooldown entries: " + cooldowns.size());
		}
		cooldowns = List.copyOf(cooldowns);
	}

	private static CooldownSyncPayload read(FriendlyByteBuf buf) {
		int size = buf.readVarInt();
		if (size < 0 || size > MAX_ENTRIES) {
			throw new IllegalArgumentException("Invalid cooldown entry count: " + size);
		}

		List<Entry> entries = new ArrayList<>(size);
		for (int i = 0; i < size; i++) {
			entries.add(new Entry(buf.readResourceLocation(), buf.readVarInt(), buf.readVarInt()));
		}
		return new CooldownSyncPayload(entries);
	}

	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeVarInt(cooldowns.size());
		for (Entry entry : cooldowns) {
			buf.writeResourceLocation(entry.abilityId());
			buf.writeVarInt(entry.remainingTicks());
			buf.writeVarInt(entry.totalTicks());
		}
	}

	@Override
	public PacketType<?> getType() {
		return TYPE;
	}

	public record Entry(ResourceLocation abilityId, int remainingTicks, int totalTicks) {
	}
}
