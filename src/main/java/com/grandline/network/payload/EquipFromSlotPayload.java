package com.grandline.network.payload;

import com.grandline.GrandLineMod;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.FriendlyByteBuf;

/** Client to server - equip the accessory in this inventory slot (0-35); the server validates. */
public record EquipFromSlotPayload(int slot) implements FabricPacket {
	public static final PacketType<EquipFromSlotPayload> TYPE = PacketType.create(GrandLineMod.id("equip_from_slot"), buf -> new EquipFromSlotPayload(buf.readVarInt()));

	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeVarInt(slot);
	}

	@Override
	public PacketType<?> getType() {
		return TYPE;
	}
}
