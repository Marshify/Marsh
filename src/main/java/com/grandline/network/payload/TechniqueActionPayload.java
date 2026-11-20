package com.grandline.network.payload;

import com.grandline.GrandLineMod;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.FriendlyByteBuf;

/**
 * Client → server: a technique key was pressed (see TechniqueService: 0 Dash, 1 Flash Step, 2 Aura, 3 Instinct). For
 * Dash, the movement keys held ({@code forward}: W +1, S -1; {@code strafe}: A +1, D -1). The server checks everything.
 */
public record TechniqueActionPayload(int action, float forward, float strafe) implements FabricPacket {
	public static final PacketType<TechniqueActionPayload> TYPE = PacketType.create(GrandLineMod.id("technique_action"),
			buf -> new TechniqueActionPayload(buf.readVarInt(), buf.readFloat(), buf.readFloat()));

	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeVarInt(action);
		buf.writeFloat(forward);
		buf.writeFloat(strafe);
	}

	@Override
	public PacketType<?> getType() {
		return TYPE;
	}
}
