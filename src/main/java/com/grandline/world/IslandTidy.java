package com.grandline.world;

import com.grandline.GrandLineMod;
import com.grandline.world.plan.IslandPlans;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseCoralPlantTypeBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.GrowingPlantBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

/**
 * Fixes decorations on the planned islands that have nothing holding them up (islands are built chunk by chunk from
 * code, so a lantern, ladder or banner can end up beside air instead of the block it was meant to hang on). Once per
 * island chunk, after it and its neighbours are loaded (in new and existing worlds alike):
 * <ul>
 * <li>a lantern that cannot stay switches between standing and hanging if that holds, else is removed;</li>
 * <li>ladders, wall banners, wall torches and other wall-mounted blocks turn to a facing with a wall behind, else are
 * removed;</li>
 * <li>a flower pot, decorated pot or campfire floating over a bottom slab drops into the slab's place (onto the solid
 * block under it), or is removed if there is only air;</li>
 * <li>loose snow layers with nothing full under them are removed;</li>
 * <li>plants and vines that cannot grow where they are (grass on stone, a dead bush on planks) are removed; a dirt path
 * under a block turns back to dirt.</li>
 * <li>known build mistakes in already generated islands are corrected ({@link #repairs}).</li>
 * </ul>
 * Nothing else is touched. {@link #VERSION} is bumped when the rules change so every chunk is checked again.
 */
public final class IslandTidy {
	public static final int VERSION = 3; // 3: Saltmarket Docks' gateway steps face the right way
	private static final int MIN_Y = 55, MAX_Y = 260;
	private static final Queue<Long> PENDING = new ArrayDeque<>();
	private static final Set<Long> QUEUED = new HashSet<>();

	private IslandTidy() {
	}

	/** Chunks already tidied (with the rules version they were tidied with). */
	public static final class Done extends SavedData {
		int version = VERSION;
		final Set<Long> chunks = new HashSet<>();

		static Done load(CompoundTag tag) {
			Done d = new Done();
			d.version = tag.getInt("version");
			if (d.version == VERSION) {
				for (long l : tag.getLongArray("chunks")) {
					d.chunks.add(l);
				}
			}
			d.version = VERSION;
			return d;
		}

		@Override
		public CompoundTag save(CompoundTag tag) {
			tag.putInt("version", version);
			tag.putLongArray("chunks", chunks.stream().mapToLong(Long::longValue).toArray());
			return tag;
		}

		static Done get(ServerLevel level) {
			return level.getDataStorage().computeIfAbsent(Done::load, Done::new, "grandline_island_tidy");
		}
	}

	public static void register() {
		ServerChunkEvents.CHUNK_LOAD.register((level, chunk) -> {
			if (level.dimension() == Level.OVERWORLD) {
				synchronized (PENDING) {
					long key = chunk.getPos().toLong();
					if (QUEUED.add(key)) {
						PENDING.add(key);
					}
				}
			}
		});
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			ServerLevel level = server.overworld();
			Done done = Done.get(level);
			int budget = 2;                                   // chunks per tick
			int checked = 0;
			synchronized (PENDING) {
				int n = PENDING.size();
				while (budget > 0 && checked < n && !PENDING.isEmpty()) {
					long key = PENDING.poll();
					checked++;
					ChunkPos cp = new ChunkPos(key);
					if (done.chunks.contains(key) || !onIsland(level, cp)) {
						QUEUED.remove(key);
						continue;
					}
					if (!neighboursLoaded(level, cp)) {
						PENDING.add(key);                         // try again later
						continue;
					}
					QUEUED.remove(key);
					int fixed = tidy(level, cp);
					done.chunks.add(key);
					done.setDirty();
					budget--;
					if (fixed > 0) {
						GrandLineMod.LOGGER.debug("Island tidy: fixed {} decorations in chunk {}", fixed, cp);
					}
				}
			}
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			synchronized (PENDING) {
				PENDING.clear();
				QUEUED.clear();
			}
		});
	}

	static boolean onIsland(ServerLevel level, ChunkPos cp) {
		int cx = cp.getMiddleBlockX(), cz = cp.getMiddleBlockZ();
		for (Island island : IslandRegistry.nearby(level.getServer(), cx, cz, 300)) {
			var plan = IslandPlans.get(island);
			if (plan == null) {
				continue;
			}
			long r = plan.extent() + 12;
			long dx = cx - island.x(), dz = cz - island.z();
			if (dx * dx + dz * dz <= r * r) {
				return true;
			}
		}
		return false;
	}

	static boolean neighboursLoaded(ServerLevel level, ChunkPos cp) {
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				if (!level.hasChunk(cp.x + dx, cp.z + dz)) {
					return false;
				}
			}
		}
		return true;
	}

	/** Tidies one chunk; returns how many blocks were fixed or removed. */
	public static int tidy(ServerLevel level, ChunkPos cp) {
		int fixed = repairs(level, cp);
		BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
		for (int x = cp.getMinBlockX(); x <= cp.getMaxBlockX(); x++) {
			for (int z = cp.getMinBlockZ(); z <= cp.getMaxBlockZ(); z++) {
				for (int y = MIN_Y; y < MAX_Y; y++) {
					BlockState s = level.getBlockState(m.set(x, y, z));
					if (s.isAir() || s.is(Blocks.WATER)) {
						continue;
					}
					if (fix(level, m.immutable(), s)) {
						fixed++;
					}
				}
			}
		}
		return fixed;
	}

	/** Fixes for build mistakes in already generated islands (version 3: Saltmarket Docks' gateway steps). */
	private static int repairs(ServerLevel level, ChunkPos cp) {
		int fixed = 0;
		for (Island island : IslandRegistry.get(level.getServer()).all()) {
			if (island.name().equals("Saltmarket Docks") && IslandPlans.get(island) instanceof com.grandline.world.plan.SaltmarketDocks) {
				fixed += com.grandline.world.plan.SaltmarketDocks.repairGateSteps(level, island, cp);
			}
		}
		return fixed;
	}

	private static boolean fullTop(ServerLevel level, BlockPos p) {
		return level.getBlockState(p).isFaceSturdy(level, p, Direction.UP);
	}

	static boolean fix(ServerLevel level, BlockPos p, BlockState s) {
		Block b = s.getBlock();
		boolean survives = s.canSurvive(level, p);
		// Pots and campfires: vanilla lets them stand anywhere, but over a slab or air they float.
		if (b instanceof FlowerPotBlock || b == Blocks.DECORATED_POT || b instanceof CampfireBlock) {
			BlockPos below = p.below();
			BlockState under = level.getBlockState(below);
			if (fullTop(level, below)) {
				return false;
			}
			if (under.getBlock() instanceof SlabBlock && under.getValue(SlabBlock.TYPE) == SlabType.BOTTOM && fullTop(level, below.below())) {
				level.setBlock(below, s, 2);                     // drop into the slab's place
				level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
				return true;
			}
			if (under.isAir()) {
				level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
				return true;
			}
			return false;                                       // on a fence, stairs, a table: it stands on something
		}
		if (survives) {
			return false;
		}
		if (b instanceof LanternBlock) {
			BlockState flipped = s.setValue(LanternBlock.HANGING, !s.getValue(LanternBlock.HANGING));
			level.setBlock(p, flipped.canSurvive(level, p) ? flipped : Blocks.AIR.defaultBlockState(), 2);
			return true;
		}
		if (b instanceof net.minecraft.world.level.block.SnowLayerBlock) {
			level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);           // a snow layer on a slab, stair or nothing
			return true;
		}
		if (b == Blocks.DIRT_PATH) {
			level.setBlock(p, Blocks.DIRT.defaultBlockState(), 2);
			return true;
		}
		if (b instanceof BushBlock || b instanceof VineBlock || b instanceof GrowingPlantBlock || b instanceof SugarCaneBlock
				|| b instanceof CactusBlock || b instanceof BaseCoralPlantTypeBlock) {
			level.setBlock(p, s.getFluidState().isEmpty() ? Blocks.AIR.defaultBlockState() : Blocks.WATER.defaultBlockState(), 2);
			return true;
		}
		if ((b instanceof net.minecraft.world.level.block.LadderBlock || b instanceof net.minecraft.world.level.block.WallBannerBlock
				|| b instanceof net.minecraft.world.level.block.WallTorchBlock || b instanceof net.minecraft.world.level.block.WallSignBlock
				|| b instanceof net.minecraft.world.level.block.WallSkullBlock) && s.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
			// Wall-mounted: ladders, wall banners, wall torches, wall signs... turn to face away from a wall.
			for (Direction d : Direction.Plane.HORIZONTAL) {
				BlockState turned = s.setValue(BlockStateProperties.HORIZONTAL_FACING, d);
				if (turned.canSurvive(level, p)) {
					level.setBlock(p, turned, 2);
					return true;
				}
			}
			level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
			return true;
		}
		return false;                                           // anything else is left as built
	}
}
