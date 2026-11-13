package com.grandline.ship;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * World-wide ship records: each owner's one active ship (any other ship of theirs removes itself when it next ticks,
 * even if it was in an unloaded chunk when replaced) and when a sunk ship may be launched again.
 */
public final class ShipData extends SavedData {
	private static final String NAME = "grandline_ships";
	private final Map<UUID, UUID> active = new HashMap<>();
	private final Map<UUID, Long> sunkUntil = new HashMap<>();
	private final Map<UUID, Long> calledAt = new HashMap<>();

	public static ShipData get(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(ShipData::load, ShipData::new, NAME);
	}

	public UUID active(UUID owner) {
		return active.get(owner);
	}

	public void setActive(UUID owner, UUID ship) {
		active.put(owner, ship);
		setDirty();
	}

	public void clearActive(UUID owner, UUID ship) {
		if (ship.equals(active.get(owner))) {
			active.remove(owner);
			setDirty();
		}
	}

	public long sunkUntil(UUID owner) {
		return sunkUntil.getOrDefault(owner, 0L);
	}

	public void setSunkUntil(UUID owner, long millis) {
		sunkUntil.put(owner, millis);
		setDirty();
	}

	/** Call cooldown is not saved: a restart resets it. */
	public long calledAt(UUID owner) {
		return calledAt.getOrDefault(owner, 0L);
	}

	public void setCalledAt(UUID owner, long millis) {
		calledAt.put(owner, millis);
	}

	@Override
	public CompoundTag save(CompoundTag tag) {
		CompoundTag a = new CompoundTag();
		active.forEach((owner, ship) -> a.putUUID(owner.toString(), ship));
		tag.put("active", a);
		CompoundTag s = new CompoundTag();
		sunkUntil.forEach((owner, until) -> s.putLong(owner.toString(), until));
		tag.put("sunk", s);
		return tag;
	}

	static ShipData load(CompoundTag tag) {
		ShipData data = new ShipData();
		CompoundTag a = tag.getCompound("active");
		for (String key : a.getAllKeys()) {
			data.active.put(UUID.fromString(key), a.getUUID(key));
		}
		CompoundTag s = tag.getCompound("sunk");
		for (String key : s.getAllKeys()) {
			data.sunkUntil.put(UUID.fromString(key), s.getLong(key));
		}
		return data;
	}
}
