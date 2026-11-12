package com.grandline.network.payload;

import com.grandline.GrandLineMod;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.FriendlyByteBuf;

/**
 * Client → server: the player pressed the key for ability slot {@code slot}.
 *
 * <p>This is only an <em>intent</em>. The client never says which ability, how much damage, or whether it is off
 * cooldown; the server resolves and validates all of that itself.
 */
public record AbilityActivationRequestPayload(int slot) implements FabricPacket {
	public static final PacketType<AbilityActivationRequestPayload> TYPE = PacketType.create(
			GrandLineMod.id("ability_activation_request"), buf -> new AbilityActivationRequestPayload(buf.readVarInt()));

	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeVarInt(slot);
	}

	@Override
	public PacketType<?> getType() {
		return TYPE;
	}
}
