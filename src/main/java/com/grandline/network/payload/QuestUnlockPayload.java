package com.grandline.network.payload;

import com.grandline.GrandLineMod;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.FriendlyByteBuf;

/** Server → client: the player reached the level for a new island's quests; show the "new quest available" card. */
public record QuestUnlockPayload(String name, int level, int sea) implements FabricPacket {
	public static final PacketType<QuestUnlockPayload> TYPE = PacketType.create(GrandLineMod.id("quest_unlock"),
			buf -> new QuestUnlockPayload(buf.readUtf(64), buf.readVarInt(), buf.readVarInt()));

	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeUtf(name, 64);
		buf.writeVarInt(level);
		buf.writeVarInt(sea);
	}

	@Override
	public PacketType<?> getType() {
		return TYPE;
	}
}
