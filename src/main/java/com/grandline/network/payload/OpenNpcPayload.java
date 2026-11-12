package com.grandline.network.payload;

import com.grandline.GrandLineMod;
import com.grandline.npc.QuestState;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Server → client: opens a conversation with an island NPC and carries everything the screen shows (quest offers,
 * active quest, dealer stock and restock timer, gacha cost and cooldown, value of carried fish). Display-only; every
 * choice goes back through a validated request.
 */
public record OpenNpcPayload(int entityId, int role, String islandName, int islandLevel, Optional<QuestState> questOffer,
		Optional<QuestState> bossOffer, Optional<QuestState> activeQuest, List<String> stock, List<Long> prices, long restockMillis, long gachaCost,
		long gachaRemainingMillis, long fishValue) implements FabricPacket {
	public static final PacketType<OpenNpcPayload> TYPE = PacketType.create(GrandLineMod.id("open_npc"), OpenNpcPayload::read);

	private static OpenNpcPayload read(FriendlyByteBuf buf) {
		int entityId = buf.readVarInt();
		int role = buf.readVarInt();
		String island = buf.readUtf(64);
		int level = buf.readVarInt();
		Optional<QuestState> offer = buf.readOptional(QuestState::read);
		Optional<QuestState> boss = buf.readOptional(QuestState::read);
		Optional<QuestState> active = buf.readOptional(QuestState::read);
		int size = buf.readVarInt();
		if (size < 0 || size > 64) {
			throw new IllegalArgumentException("Invalid stock size " + size);
		}
		List<String> stock = new ArrayList<>();
		List<Long> prices = new ArrayList<>();
		for (int i = 0; i < size; i++) {
			stock.add(buf.readUtf(256));
			prices.add(buf.readVarLong());
		}
		return new OpenNpcPayload(entityId, role, island, level, offer, boss, active, stock, prices, buf.readVarLong(), buf.readVarLong(),
				buf.readVarLong(), buf.readVarLong());
	}

	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeVarInt(entityId);
		buf.writeVarInt(role);
		buf.writeUtf(islandName, 64);
		buf.writeVarInt(islandLevel);
		buf.writeOptional(questOffer, (b, q) -> q.write(b));
		buf.writeOptional(bossOffer, (b, q) -> q.write(b));
		buf.writeOptional(activeQuest, (b, q) -> q.write(b));
		buf.writeVarInt(stock.size());
		for (int i = 0; i < stock.size(); i++) {
			buf.writeUtf(stock.get(i), 256);
			buf.writeVarLong(prices.get(i));
		}
		buf.writeVarLong(restockMillis);
		buf.writeVarLong(gachaCost);
		buf.writeVarLong(gachaRemainingMillis);
		buf.writeVarLong(fishValue);
	}

	@Override
	public PacketType<?> getType() {
		return TYPE;
	}
}
