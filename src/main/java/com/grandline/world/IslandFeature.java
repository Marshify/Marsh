package com.grandline.world;

import com.grandline.GrandLineMod;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.features.TreeFeatures;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.Random;

/**
 * Raises an island out of the ocean. The sea is divided into square regions of {@link #CELL_CHUNKS}×{@link #CELL_CHUNKS}
 * chunks; each region gets exactly one island in a chunk chosen from the world seed, and chunk (0, 0) always holds the
 * starter island. The feature only builds where there is real ocean (enough water depth), so it is safe in any world.
 *
 * <p>Islands stay within ±20 blocks of the chunk centre, inside the 3×3-chunk area a feature may write to.
 */
public final class IslandFeature extends Feature<NoneFeatureConfiguration> {
	public static final int CELL_CHUNKS = 10;
	private static final int MAX_EXTENT = 20;

	public IslandFeature(Codec<NoneFeatureConfiguration> codec) {
		super(codec);
	}

	/** Whether {@code chunk} is the island chunk of its region (or the starter chunk). */
	public static boolean isIslandChunk(long seed, ChunkPos chunk) {
		if (chunk.x == 0 && chunk.z == 0) {
			return true;
		}
		int cellX = Math.floorDiv(chunk.x, CELL_CHUNKS);
		int cellZ = Math.floorDiv(chunk.z, CELL_CHUNKS);
		if (cellX == 0 && cellZ == 0) {
			return false; // the starter island is this region's island
		}
		Random random = new Random(cellSeed(seed, cellX, cellZ));
		// Keep a margin of one chunk so neighbouring regions' islands never touch.
		int localX = 1 + random.nextInt(CELL_CHUNKS - 2);
		int localZ = 1 + random.nextInt(CELL_CHUNKS - 2);
		return Math.floorMod(chunk.x, CELL_CHUNKS) == localX && Math.floorMod(chunk.z, CELL_CHUNKS) == localZ;
	}

	private static long cellSeed(long seed, int cellX, int cellZ) {
		return seed ^ (cellX * 341873128712L) ^ (cellZ * 132897987541L) ^ 0x6A09E667F3BCC909L;
	}

	@Override
	public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
		WorldGenLevel level = context.level();
		ChunkPos chunk = new ChunkPos(context.origin());
		long seed = level.getSeed();
		if (!isIslandChunk(seed, chunk)) {
			return false;
		}

		int centerX = chunk.getMinBlockX() + 8;
		int centerZ = chunk.getMinBlockZ() + 8;
		int surface = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, centerX, centerZ);
		int floor = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, centerX, centerZ);
		if (surface - floor < 4 || level.getBlockState(new BlockPos(centerX, surface - 1, centerZ)).getFluidState().isEmpty()) {
			return false; // not open water here
		}
		int waterTop = surface - 1;

		boolean starter = chunk.x == 0 && chunk.z == 0;
		Random random = new Random(seed ^ chunk.toLong() * 0x9E3779B97F4A7C15L);
		int radius = starter ? 15 : 9 + random.nextInt(6);
		int peak = starter ? 7 : 4 + random.nextInt(7);
		double phaseA = random.nextDouble() * Math.PI * 2;
		double phaseB = random.nextDouble() * Math.PI * 2;

		BlockState grass = Blocks.GRASS_BLOCK.defaultBlockState();
		BlockState dirt = Blocks.DIRT.defaultBlockState();
		BlockState sand = Blocks.SAND.defaultBlockState();
		BlockState sandstone = Blocks.SANDSTONE.defaultBlockState();
		BlockState stone = Blocks.STONE.defaultBlockState();
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

		for (int dx = -MAX_EXTENT; dx <= MAX_EXTENT; dx++) {
			for (int dz = -MAX_EXTENT; dz <= MAX_EXTENT; dz++) {
				double distance = Math.sqrt(dx * dx + dz * dz);
				double angle = Math.atan2(dz, dx);
				double edge = radius * (1.0 + 0.16 * Math.sin(angle * 3 + phaseA) + 0.09 * Math.sin(angle * 5 + phaseB));
				double d = distance / edge;
				if (d > 1.3) {
					continue;
				}

				int x = centerX + dx;
				int z = centerZ + dz;
				int columnFloor = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z);
				int top;
				BlockState topBlock;
				if (d <= 1.0) {
					top = waterTop + (int) Math.round(peak * (1.0 - Math.pow(d, 1.6)));
					boolean beach = d > 0.72 || top <= waterTop + 1;
					topBlock = beach ? sand : grass;
				} else {
					top = waterTop - 1 - (int) Math.round((d - 1.0) * 14); // underwater sand shelf
					topBlock = sand;
				}
				if (top <= columnFloor) {
					continue;
				}

				for (int y = columnFloor; y <= top; y++) {
					BlockState state;
					if (y == top) {
						state = topBlock;
					} else if (y >= top - 3) {
						state = topBlock == grass ? dirt : sand;
					} else if (y >= top - 5 && topBlock == sand) {
						state = sandstone;
					} else {
						state = stone;
					}
					level.setBlock(pos.set(x, y, z), state, 2);
				}
			}
		}

		plantTrees(context, level, random, centerX, centerZ, radius, waterTop, starter);
		registerIsland(level, chunk, centerX, centerZ, radius, starter);
		return true;
	}

	private static void plantTrees(FeaturePlaceContext<NoneFeatureConfiguration> context, WorldGenLevel level, Random random,
			int centerX, int centerZ, int radius, int waterTop, boolean starter) {
		var registry = level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE);
		@SuppressWarnings("unchecked")
		ResourceKey<ConfiguredFeature<?, ?>>[] kinds = new ResourceKey[]{TreeFeatures.JUNGLE_TREE_NO_VINE, TreeFeatures.OAK, TreeFeatures.FANCY_OAK};
		int trees = Math.max(2, radius / 3);
		RandomSource treeRandom = RandomSource.create(random.nextLong());
		for (int i = 0; i < trees; i++) {
			double angle = random.nextDouble() * Math.PI * 2;
			double distance = (starter ? 0.5 : 0.15) * radius + random.nextDouble() * (starter ? 0.25 : 0.45) * radius;
			int x = centerX + (int) Math.round(Math.cos(angle) * distance);
			int z = centerZ + (int) Math.round(Math.sin(angle) * distance);
			int y = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
			BlockPos ground = new BlockPos(x, y - 1, z);
			if (y <= waterTop + 1 || !level.getBlockState(ground).is(Blocks.GRASS_BLOCK)) {
				continue;
			}
			Holder<ConfiguredFeature<?, ?>> tree = registry.getHolder(kinds[random.nextInt(kinds.length)]).orElse(null);
			if (tree != null) {
				tree.value().place(level, context.chunkGenerator(), treeRandom, ground.above());
			}
		}
	}

	private static void registerIsland(WorldGenLevel level, ChunkPos chunk, int x, int z, int radius, boolean starter) {
		var serverLevel = level.getLevel();
		if (serverLevel.dimension() != net.minecraft.world.level.Level.OVERWORLD) {
			return;
		}
		double distance = Math.sqrt((double) x * x + (double) z * z);
		int recommended = starter ? 1 : (int) Math.max(1, Math.min(GrandLineMod.config().maxLevel, 1 + distance * 0.5));
		String name = starter ? "Dawnreach Isle" : IslandNames.generate(level.getSeed() ^ chunk.toLong());
		IslandRegistry registry = IslandRegistry.current(); // never touch vanilla data storage from a worldgen thread
		if (registry != null) {
			registry.add(new Island(chunk.toLong(), x, z, radius, recommended, name, starter, false));
		} else {
			GrandLineMod.LOGGER.warn("Island {} generated before the island registry was loaded", name);
		}
	}
}
