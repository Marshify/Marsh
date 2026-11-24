package com.grandline.network.payload;

import com.grandline.GrandLineMod;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.FriendlyByteBuf;

/**
 * Client → server: the attack button was pressed with the fruit's M1 active (an empty loadout hand on the fruit). The
 * server decides the combo step, timing and every hit ({@link com.grandline.combat.ability.FruitStrikes}); the client
 * sends no vanilla attack, so no punch damage is ever added.
 */
public record FruitStrikePayload() implements FabricPacket {
	public static final PacketType<FruitStrikePayload> TYPE = PacketType.create(GrandLineMod.id("fruit_strike"), buf -> new FruitStrikePayload());

	@Override
	public void write(FriendlyByteBuf buf) {
	}

	@Override
	public PacketType<?> getType() {
		return TYPE;
	}
}
