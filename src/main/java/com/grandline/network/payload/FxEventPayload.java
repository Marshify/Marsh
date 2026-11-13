package com.grandline.network.payload;

import com.grandline.GrandLineMod;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;

/**
 * Server → client: a visual beat of a move (e.g. "flame_blast_windup", "flame_blast_launch", "flame_blast_impact").
 * Sent at the exact server tick the beat happens to every player near it, so the caster and onlookers see the same
 * timing. Purely cosmetic: damage and hit detection never depend on it.
 *
 * @param caster entity id of the player using the move
 * @param pos    where the beat happens (launch point, impact point...)
 * @param dir    travel direction for launches (zero otherwise)
 * @param speed  blocks per tick for launches
 * @param range  maximum travel distance for launches
 */
public record FxEventPayload(String effect, int caster, Vec3 pos, Vec3 dir, float speed, float range) implements FabricPacket {
	public static final PacketType<FxEventPayload> TYPE = PacketType.create(GrandLineMod.id("fx_event"), buf -> new FxEventPayload(
			buf.readUtf(64), buf.readVarInt(), new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()),
			new Vec3(buf.readFloat(), buf.readFloat(), buf.readFloat()), buf.readFloat(), buf.readFloat()));

	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeUtf(effect, 64);
		buf.writeVarInt(caster);
		buf.writeDouble(pos.x);
		buf.writeDouble(pos.y);
		buf.writeDouble(pos.z);
		buf.writeFloat((float) dir.x);
		buf.writeFloat((float) dir.y);
		buf.writeFloat((float) dir.z);
		buf.writeFloat(speed);
		buf.writeFloat(range);
	}

	@Override
	public PacketType<?> getType() {
		return TYPE;
	}
}
