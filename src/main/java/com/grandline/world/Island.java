package com.grandline.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;

/**
 * A generated island. {@code level} is the recommended player level (grows with distance from spawn); the sea tier
 * is derived from it. {@code npcsSpawned} records whether its NPCs have been placed.
 */
public record Island(long id, int x, int z, int radius, int level, String name, boolean starter, boolean npcsSpawned, String theme) {
	public static final Codec<Island> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.LONG.fieldOf("id").forGetter(Island::id),
			Codec.INT.fieldOf("x").forGetter(Island::x),
			Codec.INT.fieldOf("z").forGetter(Island::z),
			Codec.INT.fieldOf("radius").forGetter(Island::radius),
			Codec.INT.fieldOf("level").forGetter(Island::level),
			Codec.STRING.fieldOf("name").forGetter(Island::name),
			Codec.BOOL.optionalFieldOf("starter", false).forGetter(Island::starter),
			Codec.BOOL.optionalFieldOf("npcs_spawned", false).forGetter(Island::npcsSpawned),
			Codec.STRING.optionalFieldOf("theme", "plain").forGetter(Island::theme)
	).apply(instance, Island::new));

	/** Sea: designed islands belong to the sea they are in; random islands use their level (1 up to 700, 2 up to 1500). */
	public int sea() {
		if (designed()) {
			return SeaLayout.seaAt(x);
		}
		return level <= 700 ? 1 : level <= 1500 ? 2 : 3;
	}

	public boolean designed() {
		return !"plain".equals(theme);
	}

	public IslandTheme themeEnum() {
		return IslandTheme.byName(theme);
	}

	public Island withNpcsSpawned() {
		return new Island(id, x, z, radius, level, name, starter, true, theme);
	}

	public void write(FriendlyByteBuf buf) {
		buf.writeLong(id);
		buf.writeVarInt(x);
		buf.writeVarInt(z);
		buf.writeVarInt(radius);
		buf.writeVarInt(level);
		buf.writeUtf(name, 64);
		buf.writeBoolean(starter);
		buf.writeUtf(theme, 32);
	}

	public static Island read(FriendlyByteBuf buf) {
		return new Island(buf.readLong(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readUtf(64),
				buf.readBoolean(), false, buf.readUtf(32));
	}
}
