package com.grandline.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;

/**
 * A generated island. {@code level} is the recommended player level (grows with distance from spawn); the sea tier
 * is derived from it. {@code npcsSpawned} records whether its NPCs have been placed.
 */
public record Island(long id, int x, int z, int radius, int level, String name, boolean starter, boolean npcsSpawned) {
	public static final Codec<Island> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.LONG.fieldOf("id").forGetter(Island::id),
			Codec.INT.fieldOf("x").forGetter(Island::x),
			Codec.INT.fieldOf("z").forGetter(Island::z),
			Codec.INT.fieldOf("radius").forGetter(Island::radius),
			Codec.INT.fieldOf("level").forGetter(Island::level),
			Codec.STRING.fieldOf("name").forGetter(Island::name),
			Codec.BOOL.optionalFieldOf("starter", false).forGetter(Island::starter),
			Codec.BOOL.optionalFieldOf("npcs_spawned", false).forGetter(Island::npcsSpawned)
	).apply(instance, Island::new));

	/** Sea tier: 1 up to level 700, 2 up to 1500, 3 beyond. */
	public int sea() {
		return level <= 700 ? 1 : level <= 1500 ? 2 : 3;
	}

	public Island withNpcsSpawned() {
		return new Island(id, x, z, radius, level, name, starter, true);
	}

	public void write(FriendlyByteBuf buf) {
		buf.writeLong(id);
		buf.writeVarInt(x);
		buf.writeVarInt(z);
		buf.writeVarInt(radius);
		buf.writeVarInt(level);
		buf.writeUtf(name, 64);
		buf.writeBoolean(starter);
	}

	public static Island read(FriendlyByteBuf buf) {
		return new Island(buf.readLong(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readUtf(64),
				buf.readBoolean(), false);
	}
}
