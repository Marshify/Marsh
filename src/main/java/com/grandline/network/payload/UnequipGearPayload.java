package com.grandline.network.payload;

import com.grandline.GrandLineMod;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.FriendlyByteBuf;

/** Client → server: take the item out of an equipment slot (0 accessory, 1-2 rings). The server validates. */
public record UnequipGearPayload(int slot) implements FabricPacket {
	public static final PacketType<UnequipGearPayload> TYPE = PacketType.create(GrandLineMod.id("unequip_gear"),
			buf -> new UnequipGearPayload(buf.readVarInt()));

	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeVarInt(slot);
	}

	@Override
	public PacketType<?> getType() {
		return TYPE;
	}
}
