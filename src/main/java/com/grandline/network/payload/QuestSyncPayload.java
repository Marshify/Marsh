package com.grandline.network.payload;

import com.grandline.GrandLineMod;
import com.grandline.npc.QuestState;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.FriendlyByteBuf;

import java.util.Optional;

/** Server → client: the active quest (or none), for the quest tracker. Display-only. */
public record QuestSyncPayload(Optional<QuestState> quest) implements FabricPacket {
	public static final PacketType<QuestSyncPayload> TYPE = PacketType.create(GrandLineMod.id("quest_sync"),
			buf -> new QuestSyncPayload(buf.readOptional(QuestState::read)));

	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeOptional(quest, (b, q) -> q.write(b));
	}

	@Override
	public PacketType<?> getType() {
		return TYPE;
	}
}
