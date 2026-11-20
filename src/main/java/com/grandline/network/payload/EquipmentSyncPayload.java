package com.grandline.network.payload;

import com.grandline.GrandLineMod;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.FriendlyByteBuf;

import java.util.List;
import java.util.Map;

/**
 * Server → owner: the equipment they own (item ids) and the equipped weapon of each category ("sword", "gun", "rod" →
 * item id), for the Backpack. Sent on join and whenever it changes.
 */
public record EquipmentSyncPayload(List<String> owned, Map<String, String> equipped) implements FabricPacket {
	public static final PacketType<EquipmentSyncPayload> TYPE = PacketType.create(GrandLineMod.id("equipment_sync"), buf -> new EquipmentSyncPayload(
			buf.readList(FriendlyByteBuf::readUtf),
			buf.readMap(FriendlyByteBuf::readUtf, FriendlyByteBuf::readUtf)));

	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeCollection(owned, FriendlyByteBuf::writeUtf);
		buf.writeMap(equipped, FriendlyByteBuf::writeUtf, FriendlyByteBuf::writeUtf);
	}

	@Override
	public PacketType<?> getType() {
		return TYPE;
	}
}
