package com.grandline.world.structure;

import com.grandline.entity.GrandLineEntities;
import com.grandline.entity.Islander;
import com.grandline.world.Island;
import com.grandline.world.IslandRegistry;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Spawns what structure markers ask for, exactly once per world: "npc:&lt;role&gt;" markers get an islander,
 * "dock" markers get a boat. Which markers are done is saved with the world ({@code data/grandline_structure_population}),
 * so reloads, restarts and chunk unloads never duplicate them. Spawning waits until the marker's chunk is loaded and
 * happens on the next server tick (never inside the chunk-load callback).
 */
public final class StructurePopulation {
	private static final String NAME = "grandline_structure_population";
	private static final ConcurrentLinkedQueue<Long> PENDING_CHUNKS = new ConcurrentLinkedQueue<>();

	private StructurePopulation() {
	}

	public static void register() {
		ServerChunkEvents.CHUNK_LOAD.register((level, chunk) -> {
			if (level.dimension() == Level.OVERWORLD) {
				PENDING_CHUNKS.add(chunk.getPos().toLong());
			}
		});
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (PENDING_CHUNKS.isEmpty()) {
				return;
			}
			ServerLevel level = server.overworld();
			Set<Long> chunks = new HashSet<>();
			Long next;
			while ((next = PENDING_CHUNKS.poll()) != null) {
				chunks.add(next);
			}
			Done done = done(level);
			for (StructurePlacements.Marker marker : StructurePlacements.markers(server)) {
				long chunk = net.minecraft.world.level.ChunkPos.asLong(marker.pos().getX() >> 4, marker.pos().getZ() >> 4);
				if (!chunks.contains(chunk) || done.contains(key(marker)) || !level.hasChunk(marker.pos().getX() >> 4, marker.pos().getZ() >> 4)) {
					continue;
				}
				if (spawn(level, marker)) {
					done.add(key(marker));
				}
			}
		});
	}

	private static String key(StructurePlacements.Marker marker) {
		return marker.placement().id() + "#" + marker.index() + "@" + marker.pos().asLong();
	}

	/** Returns true if the marker is handled (spawned, or needs nothing). */
	private static boolean spawn(ServerLevel level, StructurePlacements.Marker marker) {
		BlockPos pos = marker.pos();
		if (marker.name().equals("dock")) {
			var boat = EntityType.BOAT.create(level);
			if (boat != null) {
				// Float it on top of the water above the marker: a boat spawned fully under water sinks.
				BlockPos top = pos;
				while (!level.getFluidState(top).isEmpty() && top.getY() < level.getMaxBuildHeight() - 1) {
					top = top.above();
				}
				boat.moveTo(pos.getX() + 0.5, top.getY(), pos.getZ() + 0.5, 0, 0);
				level.addFreshEntity(boat);
			}
			return true;
		}
		if (marker.name().startsWith("npc:")) {
			Islander.Role role;
			try {
				role = Islander.Role.valueOf(marker.name().substring(4).toUpperCase(java.util.Locale.ROOT));
			} catch (IllegalArgumentException e) {
				return true; // unknown role: ignore, do not retry forever
			}
			Island island = island(level, marker.placement().island());
			Islander npc = GrandLineEntities.ISLANDER.create(level);
			if (npc != null) {
				npc.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
				npc.setup(role, island == null ? 0 : island.id(), island == null ? 1 : island.level());
				npc.setPersistenceRequired();
				level.addFreshEntity(npc);
			}
			return true;
		}
		return true; // origin, quest, arrival:* ... are positions only
	}

	private static Island island(ServerLevel level, String name) {
		if (name == null || name.isEmpty()) {
			return null;
		}
		for (Island island : IslandRegistry.get(level.getServer()).all()) {
			if (island.name().equals(name)) {
				return island;
			}
		}
		return null;
	}

	private static Done done(ServerLevel level) {
		return level.getDataStorage().computeIfAbsent(Done::load, Done::new, NAME);
	}

	/** How many markers have been populated (tests). */
	public static int doneCount(ServerLevel level) {
		return done(level).keys.size();
	}

	static final class Done extends SavedData {
		private final Set<String> keys = new HashSet<>();

		boolean contains(String key) {
			return keys.contains(key);
		}

		void add(String key) {
			keys.add(key);
			setDirty();
		}

		@Override
		public CompoundTag save(CompoundTag tag) {
			ListTag list = new ListTag();
			keys.forEach(k -> list.add(StringTag.valueOf(k)));
			tag.put("done", list);
			return tag;
		}

		static Done load(CompoundTag tag) {
			Done done = new Done();
			tag.getList("done", 8).forEach(t -> done.keys.add(t.getAsString()));
			return done;
		}
	}
}
