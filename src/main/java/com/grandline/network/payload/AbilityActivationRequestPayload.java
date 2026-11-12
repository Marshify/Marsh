package com.grandline.network.payload;

import com.grandline.GrandLineMod;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.FriendlyByteBuf;

/**
 * Client → server: the player pressed the key for move {@code slot} (0–3 = Z, X, C, V) of their fighting style
 * ({@code source} 0) or fruit ({@code source} 1).
 *
 * <p>This is only an <em>intent</em>. The client never says which ability, how much damage, or whether it is off
 * cooldown; the server checks ownership, mastery, energy and cooldown and runs the move itself.
 */
public record AbilityActivationRequestPayload(int source, int slot) implements FabricPacket {
	public static final PacketType<AbilityActivationRequestPayload> TYPE = PacketType.create(
			GrandLineMod.id("ability_activation_request"), buf -> new AbilityActivationRequestPayload(buf.readVarInt(), buf.readVarInt()));

	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeVarInt(source);
		buf.writeVarInt(slot);
	}

	@Override
	public PacketType<?> getType() {
		return TYPE;
	}
}
