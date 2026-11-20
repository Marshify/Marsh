package com.grandline.network.payload;

import com.grandline.GrandLineMod;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.FriendlyByteBuf;

/**
 * Client → server: the Backpack's Equip ({@code equip} true) or Unequip button for an owned weapon (item id). The server
 * checks ownership and category and does the swap (see EquipmentOwnership.setEquipped).
 */
public record EquipWeaponPayload(String itemId, boolean equip) implements FabricPacket {
	public static final PacketType<EquipWeaponPayload> TYPE = PacketType.create(GrandLineMod.id("equip_weapon"),
			buf -> new EquipWeaponPayload(buf.readUtf(128), buf.readBoolean()));

	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeUtf(itemId, 128);
		buf.writeBoolean(equip);
	}

	@Override
	public PacketType<?> getType() {
		return TYPE;
	}
}
