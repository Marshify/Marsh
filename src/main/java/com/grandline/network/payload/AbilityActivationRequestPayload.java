package com.grandline.network.payload;

import com.grandline.GrandLineMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client → server: the player pressed the key for ability slot {@code slot}.
 *
 * <p>This is only an <em>intent</em>. The client never says which ability, how much damage, or whether it is off
 * cooldown; the server resolves and validates all of that itself.
 */
public record AbilityActivationRequestPayload(int slot) implements CustomPacketPayload {
	public static final Type<AbilityActivationRequestPayload> TYPE = new Type<>(GrandLineMod.id("ability_activation_request"));
	public static final StreamCodec<RegistryFriendlyByteBuf, AbilityActivationRequestPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, AbilityActivationRequestPayload::slot,
			AbilityActivationRequestPayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
