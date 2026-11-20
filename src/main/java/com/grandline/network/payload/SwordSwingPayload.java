package com.grandline.network.payload;

import com.grandline.GrandLineMod;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.FriendlyByteBuf;

/**
 * Client → server: the attack button was pressed with a sword in hand (a swing, hit or miss). The server decides the
 * combo step, its timing and any finisher; the vanilla attack packet still carries the normal melee hit.
 */
public record SwordSwingPayload() implements FabricPacket {
	public static final PacketType<SwordSwingPayload> TYPE = PacketType.create(GrandLineMod.id("sword_swing"), buf -> new SwordSwingPayload());

	@Override
	public void write(FriendlyByteBuf buf) {
	}

	@Override
	public PacketType<?> getType() {
		return TYPE;
	}
}
