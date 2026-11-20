package com.grandline.network.payload;

import com.grandline.GrandLineMod;
import com.grandline.technique.Techniques;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.FriendlyByteBuf;

/** Server → owner: their techniques (ownership, XP, charges), the Aura/Instinct toggles and cooldown ticks left. */
public record TechniqueSyncPayload(Techniques techniques, boolean auraOn, boolean instinctOn, int dashCooldown, int flashCooldown)
		implements FabricPacket {
	public static final PacketType<TechniqueSyncPayload> TYPE = PacketType.create(GrandLineMod.id("technique_sync"),
			buf -> new TechniqueSyncPayload(Techniques.read(buf), buf.readBoolean(), buf.readBoolean(), buf.readVarInt(), buf.readVarInt()));

	@Override
	public void write(FriendlyByteBuf buf) {
		techniques.write(buf);
		buf.writeBoolean(auraOn);
		buf.writeBoolean(instinctOn);
		buf.writeVarInt(dashCooldown);
		buf.writeVarInt(flashCooldown);
	}

	@Override
	public PacketType<?> getType() {
		return TYPE;
	}
}
