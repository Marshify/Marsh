package com.grandline.network.payload;

import com.grandline.GrandLineMod;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.FriendlyByteBuf;

/** Server → client: the player's Fishing Mastery (for the HUD and menus). */
public record FishingMasteryPayload(int level, long xp, long next, int catches) implements FabricPacket {
	public static final PacketType<FishingMasteryPayload> TYPE = PacketType.create(GrandLineMod.id("fishing_mastery"),
			buf -> new FishingMasteryPayload(buf.readVarInt(), buf.readVarLong(), buf.readVarLong(), buf.readVarInt()));

	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeVarInt(level);
		buf.writeVarLong(xp);
		buf.writeVarLong(next);
		buf.writeVarInt(catches);
	}

	@Override
	public PacketType<?> getType() {
		return TYPE;
	}
}
