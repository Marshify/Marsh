package com.grandline.network.payload;

import com.grandline.GrandLineMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Server → client: the full set of active ability cooldowns for the receiving player, expressed as remaining ticks.
 * The server remains the only authority on whether an ability is ready; the client uses this purely for display.
 */
public record CooldownSyncPayload(List<Entry> cooldowns) implements CustomPacketPayload {
	/** Hard cap so a bug can never produce an oversized packet. */
	public static final int MAX_ENTRIES = 256;

	public static final Type<CooldownSyncPayload> TYPE = new Type<>(GrandLineMod.id("cooldown_sync"));

	public static final StreamCodec<RegistryFriendlyByteBuf, CooldownSyncPayload> CODEC = StreamCodec.composite(
			Entry.STREAM_CODEC.apply(ByteBufCodecs.collection(ArrayList::new, MAX_ENTRIES)), CooldownSyncPayload::cooldowns,
			CooldownSyncPayload::new
	);

	public CooldownSyncPayload {
		cooldowns = List.copyOf(cooldowns);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public record Entry(Identifier abilityId, int remainingTicks, int totalTicks) {
		public static final StreamCodec<RegistryFriendlyByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
				Identifier.STREAM_CODEC, Entry::abilityId,
				ByteBufCodecs.VAR_INT, Entry::remainingTicks,
				ByteBufCodecs.VAR_INT, Entry::totalTicks,
				Entry::new
		);
	}
}
