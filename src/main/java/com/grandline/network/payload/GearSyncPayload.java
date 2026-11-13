package com.grandline.network.payload;

import com.grandline.GrandLineMod;
import com.grandline.gear.EquippedGear;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.FriendlyByteBuf;

/** Server → client: the player's equipped accessory and rings and their total bonuses, as the server computes them. */
public record GearSyncPayload(EquippedGear gear, com.grandline.gear.GearBonuses bonuses) implements FabricPacket {
	public static final PacketType<GearSyncPayload> TYPE = PacketType.create(GrandLineMod.id("gear_sync"),
			buf -> new GearSyncPayload(EquippedGear.read(buf), com.grandline.gear.GearBonuses.read(buf)));

	@Override
	public void write(FriendlyByteBuf buf) {
		gear.write(buf);
		bonuses.write(buf);
	}

	@Override
	public PacketType<?> getType() {
		return TYPE;
	}
}
