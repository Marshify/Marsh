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
	/**
	 * Island layout version the world was created with. Worlds saved before versioning load as 1 and keep generating
	 * the islands they started with (no half-old, half-new islands); new worlds get {@link #CURRENT_LAYOUT}.
	 */
	public static final int CURRENT_LAYOUT = 3;
	private int layout = CURRENT_LAYOUT;

	/**
	 * Loaded once on the main thread when the overworld loads. World-generation threads must only use this instance:
	 * vanilla's data storage is not thread-safe, and touching it from worker threads can hang the server.
	 */
	private static volatile IslandRegistry instance;

	public static void register() {
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents.LOAD.register((server, level) -> {
			if (level.dimension() == net.minecraft.world.level.Level.OVERWORLD) {
				instance = level.getDataStorage().computeIfAbsent(IslandRegistry::load, IslandRegistry::new, NAME);
			}
		});
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(server -> instance = null);
	}

	/** The registry for the running server, or null before the overworld has loaded. Safe from any thread. */
	public static IslandRegistry current() {
		return instance;
	}

	public static IslandRegistry get(ServerLevel overworld) {
		IslandRegistry registry = instance;
		if (registry == null) {
			registry = overworld.getDataStorage().computeIfAbsent(IslandRegistry::load, IslandRegistry::new, NAME);
			instance = registry;
		}
		return registry;
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

	/** Removes every island matching {@code filter}; returns how many were removed. Generated blocks are not touched. */
	public synchronized int removeIf(java.util.function.Predicate<Island> filter) {
		int before = islands.size();
		islands.values().removeIf(filter);
		int removed = before - islands.size();
		if (removed > 0) {
			setDirty();
		}
		return removed;
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
		tag.putInt("layout", layout);
		return tag;
	}

	private static IslandRegistry load(CompoundTag tag) {
		IslandRegistry registry = new IslandRegistry();
		for (Tag entry : tag.getList("islands", Tag.TAG_COMPOUND)) {
			Island.CODEC.parse(NbtOps.INSTANCE, entry).resultOrPartial(GrandLineMod.LOGGER::warn)
					.ifPresent(island -> registry.islands.put(island.id(), island));
		}
		registry.spawnPlaced = tag.getBoolean("spawn_placed");
		registry.layout = tag.contains("layout") ? tag.getInt("layout") : 1;
		return registry;
	}

	/** The island layout version this world generates (see {@link #CURRENT_LAYOUT}). */
	public int layout() {
		return layout;
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
