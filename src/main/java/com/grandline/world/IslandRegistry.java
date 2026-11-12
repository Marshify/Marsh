package com.grandline.world;

import com.grandline.GrandLineMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Every island generated in the overworld, saved with the world ({@code data/grandline_islands.dat}). Islands are
 * added from world-generation threads, so all access is synchronized.
 */
public final class IslandRegistry extends SavedData {
	private static final String NAME = "grandline_islands";

	private final Map<Long, Island> islands = new LinkedHashMap<>();
	private boolean spawnPlaced;

	public static IslandRegistry get(ServerLevel overworld) {
		synchronized (IslandRegistry.class) {
			return overworld.getDataStorage().computeIfAbsent(IslandRegistry::load, IslandRegistry::new, NAME);
		}
	}

	public static IslandRegistry get(MinecraftServer server) {
		return get(server.overworld());
	}

	public synchronized void add(Island island) {
		if (!islands.containsKey(island.id())) {
			islands.put(island.id(), island);
			setDirty();
		}
	}

	public synchronized void update(Island island) {
		islands.put(island.id(), island);
		setDirty();
	}

	public synchronized List<Island> all() {
		return new ArrayList<>(islands.values());
	}

	public synchronized Island get(long id) {
		return islands.get(id);
	}

	public synchronized boolean spawnPlaced() {
		return spawnPlaced;
	}

	public synchronized void markSpawnPlaced() {
		spawnPlaced = true;
		setDirty();
	}

	@Override
	public synchronized CompoundTag save(CompoundTag tag) {
		ListTag list = new ListTag();
		for (Island island : islands.values()) {
			Island.CODEC.encodeStart(NbtOps.INSTANCE, island).result().ifPresent(list::add);
		}
		tag.put("islands", list);
		tag.putBoolean("spawn_placed", spawnPlaced);
		return tag;
	}

	private static IslandRegistry load(CompoundTag tag) {
		IslandRegistry registry = new IslandRegistry();
		for (Tag entry : tag.getList("islands", Tag.TAG_COMPOUND)) {
			Island.CODEC.parse(NbtOps.INSTANCE, entry).resultOrPartial(GrandLineMod.LOGGER::warn)
					.ifPresent(island -> registry.islands.put(island.id(), island));
		}
		registry.spawnPlaced = tag.getBoolean("spawn_placed");
		return registry;
	}

	public static Collection<Island> nearby(MinecraftServer server, int x, int z, int range) {
		List<Island> result = new ArrayList<>();
		for (Island island : get(server).all()) {
			long dx = island.x() - x;
			long dz = island.z() - z;
			if (dx * dx + dz * dz <= (long) range * range) {
				result.add(island);
			}
		}
		return result;
	}
}
