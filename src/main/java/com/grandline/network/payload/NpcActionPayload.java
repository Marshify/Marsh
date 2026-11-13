package com.grandline.network.payload;

import com.grandline.GrandLineMod;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.FriendlyByteBuf;

/** Client → server: a choice made in an NPC conversation. The server re-checks the NPC, distance and conditions. */
public record NpcActionPayload(int entityId, int action) implements FabricPacket {
	public static final int ACCEPT_QUEST = 0;
	public static final int ACCEPT_BOSS = 1;
	public static final int ABANDON_QUEST = 2;
	public static final int SELL_FISH = 3;
	/** Sail to sea {@code SAIL_TO_SEA + n - 1} (n = 1..3). */
	public static final int SAIL_TO_SEA = 10;
	/** Buy ship class {@code BUY_SHIP + ordinal} (it launches straight away). */
	public static final int BUY_SHIP = 20;
	/** Launch an owned ship class {@code LAUNCH_SHIP + ordinal} near the Navigator. */
	public static final int LAUNCH_SHIP = 30;
	/** Buy gun {@code BUY_GUN + i} from the Gunsmith's stock ({@link com.grandline.npc.GunShop#STOCK}). */
	public static final int BUY_GUN = 40;

	public static final PacketType<NpcActionPayload> TYPE = PacketType.create(GrandLineMod.id("npc_action"),
			buf -> new NpcActionPayload(buf.readVarInt(), buf.readVarInt()));

	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeVarInt(entityId);
		buf.writeVarInt(action);
	}

	@Override
	public PacketType<?> getType() {
		return TYPE;
	}
}
