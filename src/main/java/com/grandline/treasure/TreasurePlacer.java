package com.grandline.treasure;

import com.grandline.GrandLineMod;
import com.grandline.world.Island;
import com.grandline.world.IslandRegistry;
import com.grandline.world.plan.IslandPlans;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Puts each island's treasure chests in place when their chunk loads, in new worlds and in worlds generated before
 * chests existed alike. A chest is only placed where its spot is still open (air or plants, standing on a solid
 * floor), so it never replaces part of a build. Islands an older world still has in their pre-rebuild layout use that
 * layout's own spots (TreasureSpots.legacy), or none.
 */
public final class TreasurePlacer {
	/** A spot in world coordinates. */
	public record Placed(String island, TreasureSpot spot, BlockPos pos) {
	}

	private static final Queue<Long> PENDING = new ConcurrentLinkedQueue<>();
	private static Map<Long, List<Placed>> byChunk;
	private static final Set<BlockPos> WARNED = new HashSet<>();

	private TreasurePlacer() {
	}

	/** Positions this world has had chests placed at, so chests whose spot was later moved or removed are cleared. */
	public static final class Placements extends net.minecraft.world.level.saveddata.SavedData {
		final Set<Long> positions = new HashSet<>();

		static Placements load(net.minecraft.nbt.CompoundTag tag) {
			Placements p = new Placements();
			for (long l : tag.getLongArray("positions")) {
				p.positions.add(l);
			}
			return p;
		}

		@Override
		public net.minecraft.nbt.CompoundTag save(net.minecraft.nbt.CompoundTag tag) {
			tag.putLongArray("positions", positions.stream().mapToLong(Long::longValue).toArray());
			return tag;
		}

		static Placements get(ServerLevel level) {
			return level.getDataStorage().computeIfAbsent(Placements::load, Placements::new, "grandline_treasure");
		}
	}

	public static void register() {
		ServerChunkEvents.CHUNK_LOAD.register((level, chunk) -> {
			if (level.dimension() == Level.OVERWORLD) {
				PENDING.add(chunk.getPos().toLong()); // never touch the world inside the load callback
			}
		});
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			Long chunk;
			while ((chunk = PENDING.poll()) != null) {
				for (Placed p : index(server).getOrDefault(chunk, List.of())) {
					place(server.overworld(), p);
				}
				clearStale(server.overworld(), chunk);
			}
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			byChunk = null;
			PENDING.clear();
			WARNED.clear();
		});
	}

	/** Every chest spot of this world, in world coordinates. */
	public static List<Placed> all(MinecraftServer server) {
		List<Placed> out = new ArrayList<>();
		index(server).values().forEach(out::addAll);
		return out;
	}

	private static synchronized Map<Long, List<Placed>> index(MinecraftServer server) {
		if (byChunk == null) {
			Map<Long, List<Placed>> map = new HashMap<>();
			for (Island island : IslandRegistry.get(server).all()) {
				List<TreasureSpot> spots = IslandPlans.usesCurrentPlan(island.name()) ? TreasureSpots.forIsland(island.name())
						: IslandPlans.usesInterimPlan(island.name()) ? TreasureSpots.interim(IslandPlans.get(island.name())) : TreasureSpots.legacy(island.name());
				for (TreasureSpot s : spots) {
					BlockPos pos = new BlockPos(island.x() + s.x(), s.y(), island.z() + s.z());
					map.computeIfAbsent(new ChunkPos(pos).toLong(), k -> new ArrayList<>()).add(new Placed(island.name(), s, pos));
				}
			}
			byChunk = map;
		}
		return byChunk;
	}

	/** Places one chest if its spot is open; returns whether a chest of the right kind now stands there. */
	public static boolean place(ServerLevel level, Placed p) {
		TreasureChestBlock block = TreasureBlocks.get(p.spot().tier());
		BlockState here = level.getBlockState(p.pos());
		BlockState want = block.defaultBlockState().setValue(TreasureChestBlock.FACING, p.spot().facing());
		if (here.getBlock() instanceof TreasureChestBlock) {
			if (!here.equals(want)) {
				level.setBlock(p.pos(), want, 2); // a spot whose tier or facing was revised
			}
			record(level, p.pos());
			return true;
		}
		BlockPos below = p.pos().below();
		if (!(here.isAir() || here.canBeReplaced()) || !level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) {
			if (WARNED.add(p.pos())) {
				GrandLineMod.LOGGER.warn("Treasure spot on {} at {} is blocked ({}), no chest placed", p.island(), p.pos(), here);
			}
			return false;
		}
		level.setBlock(p.pos(), want, 2);
		record(level, p.pos());
		return true;
	}

	private static void record(ServerLevel level, BlockPos pos) {
		Placements data = Placements.get(level);
		if (data.positions.add(pos.asLong())) {
			data.setDirty();
		}
	}

	/** Removes chests this world placed in {@code chunk} at spots that no longer exist. */
	static void clearStale(ServerLevel level, long chunk) {
		Placements data = Placements.get(level);
		if (data.positions.isEmpty()) {
			return;
		}
		Set<Long> current = new HashSet<>();
		for (Placed p : index(level.getServer()).getOrDefault(chunk, List.of())) {
			current.add(p.pos().asLong());
		}
		ChunkPos cp = new ChunkPos(chunk);
		boolean changed = data.positions.removeIf(l -> {
			BlockPos pos = BlockPos.of(l);
			if (pos.getX() >> 4 != cp.x || pos.getZ() >> 4 != cp.z || current.contains(l)) {
				return false;
			}
			if (level.getBlockState(pos).getBlock() instanceof TreasureChestBlock) {
				level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
			}
			return true;
		});
		if (changed) {
			data.setDirty();
		}
	}

	/** Dev: removes every treasure chest on the planned islands that is not at a current spot. Returns how many. */
	public static int cleanAll(ServerLevel level) {
		Set<Long> current = new HashSet<>();
		for (Placed p : all(level.getServer())) {
			current.add(p.pos().asLong());
		}
		int removed = 0;
		BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
		for (Island island : IslandRegistry.get(level.getServer()).all()) {
			var plan = IslandPlans.get(island);
			if (plan == null) {
				continue;
			}
			int r = plan.extent();
			for (int x = island.x() - r; x <= island.x() + r; x++) {
				for (int z = island.z() - r; z <= island.z() + r; z++) {
					if (!level.hasChunk(x >> 4, z >> 4)) {
						continue;
					}
					for (int y = 60; y < 200; y++) {
						if (level.getBlockState(m.set(x, y, z)).getBlock() instanceof TreasureChestBlock && !current.contains(m.asLong())) {
							level.setBlock(m, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
							removed++;
						}
					}
				}
			}
		}
		return removed;
	}
}
