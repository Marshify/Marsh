package com.grandline.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.Random;

/**
 * Builds one designed island inside one chunk. Everything is computed from the island spec and the world seed (no
 * world reads), and every write is clipped to the current chunk, so the island is assembled seamlessly as its chunks
 * generate in any order.
 *
 * <p>Assumes the Grand Line sea: water surface at y = {@link #WATER_TOP}, sea floor at y = {@link #FLOOR}.
 */
public final class IslandBuilder {
	public static final int WATER_TOP = 62;
	public static final int FLOOR = 39;
	/** Radius around the centre kept flat and free of buildings (NPCs stand here). */
	public static final int PLAZA = 8;

	private final WorldGenLevel level;
	private final SeaLayout.Spec spec;
	private final int minX;
	private final int minZ;
	private final long seed;
	private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
	private final double phaseA;
	private final double phaseB;
	private final int peak;

	public IslandBuilder(WorldGenLevel level, SeaLayout.Spec spec, int chunkMinX, int chunkMinZ) {
		this.level = level;
		this.spec = spec;
		this.minX = chunkMinX;
		this.minZ = chunkMinZ;
		this.seed = level.getSeed() ^ (spec.name().hashCode() * 0x9E3779B97F4A7C15L);
		Random random = new Random(seed);
		this.phaseA = random.nextDouble() * Math.PI * 2;
		this.phaseB = random.nextDouble() * Math.PI * 2;
		this.peak = switch (spec.theme()) {
			case CAVE -> 9;
			case SNOW, VOLCANO -> 8;
			case TOWN, HARBOR, COLOSSEUM, PALACE, CRYSTAL, BRICK_TOWN -> 4;
			default -> 6;
		};
	}

	/** Whether any part of the island (including its underwater shelf and tall structures) can touch this chunk. */
	public static boolean touches(SeaLayout.Spec spec, int chunkMinX, int chunkMinZ) {
		int reach = (int) (spec.radius() * 1.45) + 4;
		int nearestX = Math.max(chunkMinX, Math.min(spec.x(), chunkMinX + 15));
		int nearestZ = Math.max(chunkMinZ, Math.min(spec.z(), chunkMinZ + 15));
		long dx = nearestX - spec.x();
		long dz = nearestZ - spec.z();
		return dx * dx + dz * dz <= (long) reach * reach;
	}

	// ------------------------------------------------------------------------------------------------ terrain

	/** Normalised distance from the centre: 1.0 at the beach edge (wobbly coastline). */
	private double norm(int lx, int lz) {
		double distance = Math.sqrt(lx * lx + lz * lz);
		double angle = Math.atan2(lz, lx);
		double edge = spec.radius() * (1.0 + 0.13 * Math.sin(angle * 3 + phaseA) + 0.07 * Math.sin(angle * 5 + phaseB));
		return distance / edge;
	}

	/** Surface height at island-local coordinates (lx, lz), or below water for open sea. */
	public int ground(int lx, int lz) {
		double d = norm(lx, lz);
		if (d > 1.0) {
			return WATER_TOP - 1 - (int) Math.round((d - 1.0) * 20);
		}
		double plazaD = norm(PLAZA, 0) * 1.1;
		double effective = Math.max(d, plazaD); // flat plaza in the middle
		return WATER_TOP + 1 + (int) Math.round(peak * (1.0 - Math.pow(effective, 1.7)));
	}

	private boolean beach(int lx, int lz) {
		double d = norm(lx, lz);
		return d > 0.8 || ground(lx, lz) <= WATER_TOP + 1;
	}

	private BlockState top() {
		return switch (spec.theme()) {
			case SNOW -> Blocks.SNOW_BLOCK.defaultBlockState();
			case DESERT, COLOSSEUM -> Blocks.SAND.defaultBlockState();
			case VOLCANO -> Blocks.COARSE_DIRT.defaultBlockState();
			case FORTRESS, PRISON, CAVE -> Blocks.STONE.defaultBlockState();
			case CRYSTAL -> Blocks.CALCITE.defaultBlockState();
			default -> Blocks.GRASS_BLOCK.defaultBlockState();
		};
	}

	private BlockState under() {
		return switch (spec.theme()) {
			case SNOW -> Blocks.PACKED_ICE.defaultBlockState();
			case DESERT, COLOSSEUM -> Blocks.SANDSTONE.defaultBlockState();
			case VOLCANO -> Blocks.BASALT.defaultBlockState();
			case FORTRESS, PRISON, CAVE, CRYSTAL -> Blocks.STONE.defaultBlockState();
			default -> Blocks.DIRT.defaultBlockState();
		};
	}

	private BlockState plaza() {
		return switch (spec.theme()) {
			case TOWN, BRICK_TOWN, HARBOR -> Blocks.STONE_BRICKS.defaultBlockState();
			case PALACE -> Blocks.PURPUR_BLOCK.defaultBlockState();
			case CRYSTAL -> Blocks.QUARTZ_BLOCK.defaultBlockState();
			case FORTRESS, PRISON -> Blocks.POLISHED_ANDESITE.defaultBlockState();
			case COLOSSEUM -> Blocks.SMOOTH_SANDSTONE.defaultBlockState();
			default -> null;
		};
	}

	private void terrain() {
		BlockState sand = Blocks.SAND.defaultBlockState();
		BlockState stone = Blocks.STONE.defaultBlockState();
		BlockState plaza = plaza();
		for (int x = minX; x < minX + 16; x++) {
			for (int z = minZ; z < minZ + 16; z++) {
				int lx = x - spec.x();
				int lz = z - spec.z();
				if (norm(lx, lz) > 1.45) {
					continue;
				}
				int top = ground(lx, lz);
				if (top <= FLOOR) {
					continue;
				}
				boolean isBeach = beach(lx, lz);
				boolean isPlaza = plaza != null && lx * lx + lz * lz <= PLAZA * PLAZA;
				BlockState surface = isPlaza ? plaza : isBeach ? (spec.theme() == IslandTheme.VOLCANO ? Blocks.BLACKSTONE.defaultBlockState()
						: spec.theme() == IslandTheme.SNOW && top > WATER_TOP ? Blocks.SNOW_BLOCK.defaultBlockState() : sand) : top();
				for (int y = FLOOR; y <= top; y++) {
					BlockState state = y == top ? surface : y >= top - 3 ? (isBeach ? sand : under()) : stone;
					set(x, y, z, state);
				}
				if (top >= WATER_TOP) {
					for (int y = top + 1; y <= WATER_TOP; y++) {
						set(x, y, z, Blocks.AIR.defaultBlockState());
					}
				}
			}
		}
	}

	// ------------------------------------------------------------------------------------------------ build

	public void build() {
		terrain();
		Random random = new Random(seed);
		switch (spec.theme()) {
			case TOWN -> town(random, Blocks.OAK_PLANKS.defaultBlockState(), Blocks.RED_TERRACOTTA.defaultBlockState(), 7);
			case BRICK_TOWN -> town(random, Blocks.BRICKS.defaultBlockState(), Blocks.DARK_OAK_PLANKS.defaultBlockState(), 6);
			case JUNGLE -> jungle(random);
			case SNOW -> snow(random);
			case DESERT -> desert(random);
			case VOLCANO -> volcano(random);
			case FORTRESS -> fortress();
			case VILLAGE -> village(random);
			case PRISON -> prison();
			case CAVE -> cave();
			case HARBOR -> harbor(random);
			case SKY -> sky(random);
			case COLOSSEUM -> colosseum();
			case PALACE -> palace(random);
			case CRYSTAL -> crystal(random);
			case PLAIN -> trees(random, 6, false);
		}
	}

	/** Places trees at random spots outside the plaza. */
	private void trees(Random random, int count, boolean jungle) {
		for (int i = 0; i < count; i++) {
			double angle = random.nextDouble() * Math.PI * 2;
			double distance = PLAZA + 3 + random.nextDouble() * (spec.radius() * 0.75 - PLAZA - 3);
			int lx = (int) Math.round(Math.cos(angle) * distance);
			int lz = (int) Math.round(Math.sin(angle) * distance);
			if (beach(lx, lz)) {
				continue;
			}
			if (jungle) {
				tree(lx, lz, Blocks.JUNGLE_LOG.defaultBlockState(), Blocks.JUNGLE_LEAVES.defaultBlockState(), 7 + random.nextInt(5), 3);
			} else {
				tree(lx, lz, Blocks.OAK_LOG.defaultBlockState(), Blocks.OAK_LEAVES.defaultBlockState(), 4 + random.nextInt(2), 2);
			}
		}
	}

	private void town(Random random, BlockState wall, BlockState roof, int houses) {
		for (int i = 0; i < houses; i++) {
			double angle = i * Math.PI * 2 / houses + 0.3;
			int distance = PLAZA + 7;
			int lx = (int) Math.round(Math.cos(angle) * distance);
			int lz = (int) Math.round(Math.sin(angle) * distance);
			house(lx, lz, 7, 6, 4 + random.nextInt(2), wall, roof);
		}
		lampPosts();
		trees(random, 5, false);
	}

	private void jungle(Random random) {
		trees(random, spec.radius() / 2 + 6, true);
		// Overgrown stepped ruin.
		int rx = spec.radius() / 3;
		int rz = -spec.radius() / 4;
		int g = ground(rx, rz);
		for (int step = 0; step < 4; step++) {
			int half = 5 - step;
			fill(rx - half, g + step, rz - half, rx + half, g + step, rz + half,
					step % 2 == 0 ? Blocks.MOSSY_STONE_BRICKS.defaultBlockState() : Blocks.CRACKED_STONE_BRICKS.defaultBlockState());
		}
	}

	private void snow(Random random) {
		mountain(spec.radius() / 3, -spec.radius() / 4, 9, 18, Blocks.STONE.defaultBlockState(), Blocks.SNOW_BLOCK.defaultBlockState());
		mountain(-spec.radius() / 3, spec.radius() / 5, 7, 13, Blocks.STONE.defaultBlockState(), Blocks.SNOW_BLOCK.defaultBlockState());
		for (int i = 0; i < 6; i++) {
			double angle = random.nextDouble() * Math.PI * 2;
			double distance = PLAZA + 3 + random.nextDouble() * spec.radius() * 0.5;
			int lx = (int) Math.round(Math.cos(angle) * distance);
			int lz = (int) Math.round(Math.sin(angle) * distance);
			if (!beach(lx, lz)) {
				spruce(lx, lz, 6 + random.nextInt(3));
			}
		}
		// Frozen pond.
		int px = -spec.radius() / 3;
		int pz = -spec.radius() / 3;
		disc(px, pz, 4, 0, Blocks.ICE.defaultBlockState());
	}

	private void desert(Random random) {
		pyramid(spec.radius() / 3, spec.radius() / 4, 11, Blocks.SANDSTONE.defaultBlockState(), Blocks.CHISELED_SANDSTONE.defaultBlockState());
		for (int i = 0; i < 6; i++) {
			double angle = random.nextDouble() * Math.PI * 2;
			double distance = PLAZA + 3 + random.nextDouble() * spec.radius() * 0.55;
			palm((int) Math.round(Math.cos(angle) * distance), (int) Math.round(Math.sin(angle) * distance), 5 + random.nextInt(3));
		}
		house(-PLAZA - 6, -3, 6, 6, 4, Blocks.SMOOTH_SANDSTONE.defaultBlockState(), Blocks.CUT_SANDSTONE.defaultBlockState());
	}

	private void volcano(Random random) {
		int vx = -spec.radius() / 3;
		int vz = -spec.radius() / 5;
		int g = ground(vx, vz);
		int height = 22;
		int baseR = 13;
		for (int y = 0; y < height; y++) {
			int r = (int) Math.round(baseR * (1.0 - y / (double) height)) + 2;
			int crater = y > height - 6 ? 3 : 0;
			for (int dx = -r; dx <= r; dx++) {
				for (int dz = -r; dz <= r; dz++) {
					double d = Math.sqrt(dx * dx + dz * dz);
					if (d <= r && d >= crater) {
						set(spec.x() + vx + dx, g + y, spec.z() + vz + dz, random.nextInt(9) == 0
								? Blocks.MAGMA_BLOCK.defaultBlockState() : d > r - 1 ? Blocks.BASALT.defaultBlockState() : Blocks.BLACKSTONE.defaultBlockState());
					}
				}
			}
		}
		for (int y = height - 6; y < height - 2; y++) {
			disc(vx, vz, 2, y, Blocks.LAVA.defaultBlockState(), g);
		}
		// Lava flow down one side.
		for (int i = 0; i < 12; i++) {
			int lx = vx + i;
			int lz = vz + i / 3;
			set(spec.x() + lx, ground(lx, lz) + Math.max(0, height - 8 - i * 2) / 2, spec.z() + lz, Blocks.MAGMA_BLOCK.defaultBlockState());
		}
	}

	private void fortress() {
		int r = Math.min(spec.radius() - 6, 26);
		ring(r, 7, Blocks.STONE_BRICKS.defaultBlockState(), true);
		for (int i = 0; i < 4; i++) {
			double angle = Math.PI / 4 + i * Math.PI / 2;
			tower((int) Math.round(Math.cos(angle) * r), (int) Math.round(Math.sin(angle) * r), 4, 13,
					Blocks.STONE_BRICKS.defaultBlockState(), Blocks.DEEPSLATE_TILES.defaultBlockState());
		}
		tower(0, -r / 2 - 3, 6, 18, Blocks.POLISHED_ANDESITE.defaultBlockState(), Blocks.DEEPSLATE_TILES.defaultBlockState());
	}

	private void village(Random random) {
		windmill(spec.radius() / 3, -spec.radius() / 4);
		house(-PLAZA - 6, 4, 7, 6, 4, Blocks.SPRUCE_PLANKS.defaultBlockState(), Blocks.SPRUCE_PLANKS.defaultBlockState());
		house(-4, PLAZA + 6, 6, 7, 4, Blocks.OAK_PLANKS.defaultBlockState(), Blocks.SPRUCE_PLANKS.defaultBlockState());
		// Wheat fields.
		int fx = spec.radius() / 4;
		int fz = spec.radius() / 4;
		for (int dx = 0; dx < 9; dx++) {
			for (int dz = 0; dz < 7; dz++) {
				int lx = fx + dx;
				int lz = fz + dz;
				if (beach(lx, lz)) {
					continue;
				}
				int g = ground(lx, lz);
				if (dz == 3) {
					set(spec.x() + lx, g, spec.z() + lz, Blocks.WATER.defaultBlockState());
				} else {
					set(spec.x() + lx, g, spec.z() + lz, Blocks.FARMLAND.defaultBlockState());
					set(spec.x() + lx, g + 1, spec.z() + lz, Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 7));
				}
			}
		}
		trees(random, 5, false);
	}

	private void prison() {
		int r = Math.min(spec.radius() - 5, 22);
		ring(r, 6, Blocks.STONE_BRICKS.defaultBlockState(), true);
		int tx = 0;
		int tz = -r / 2 - 2;
		tower(tx, tz, 6, 26, Blocks.STONE_BRICKS.defaultBlockState(), Blocks.POLISHED_DEEPSLATE.defaultBlockState());
		int g = ground(tx, tz);
		for (int y = 4; y < 24; y += 4) {
			for (Direction side : Direction.Plane.HORIZONTAL) {
				// Bars must carry their connections: worldgen never recomputes block shapes.
				boolean alongX = side.getAxis() == Direction.Axis.Z;
				BlockState bars = Blocks.IRON_BARS.defaultBlockState()
						.setValue(net.minecraft.world.level.block.IronBarsBlock.EAST, alongX)
						.setValue(net.minecraft.world.level.block.IronBarsBlock.WEST, alongX)
						.setValue(net.minecraft.world.level.block.IronBarsBlock.NORTH, !alongX)
						.setValue(net.minecraft.world.level.block.IronBarsBlock.SOUTH, !alongX);
				set(spec.x() + tx + side.getStepX() * 6, g + y, spec.z() + tz + side.getStepZ() * 6, bars);
			}
		}
	}

	private void cave() {
		// A rocky dome with a cave opening facing south.
		int g = ground(0, 0);
		for (int dx = -9; dx <= 9; dx++) {
			for (int dz = -9; dz <= 9; dz++) {
				for (int dy = 0; dy <= 9; dy++) {
					double d = Math.sqrt(dx * dx + dz * dz + dy * dy * 1.6);
					if (d <= 9 && d >= 6.5 && !(dz > 3 && Math.abs(dx) <= 2 && dy <= 4)) {
						set(spec.x() + dx, g + 1 + dy, spec.z() + dz,
								(dx + dz + dy) % 4 == 0 ? Blocks.MOSSY_COBBLESTONE.defaultBlockState() : Blocks.STONE.defaultBlockState());
					}
				}
			}
		}
	}

	private void harbor(Random random) {
		// Wooden piers reaching out to sea.
		for (int i = 0; i < 3; i++) {
			int lx = -12 + i * 12;
			int start = (int) (spec.radius() * 0.75);
			for (int lz = start; lz < spec.radius() + 16; lz++) {
				fill(lx - 1, WATER_TOP + 1, lz, lx + 1, WATER_TOP + 1, lz, Blocks.SPRUCE_PLANKS.defaultBlockState());
				if (lz % 4 == 0) {
					fill(lx - 2, FLOOR, lz, lx - 2, WATER_TOP + 2, lz, Blocks.SPRUCE_LOG.defaultBlockState());
					fill(lx + 2, FLOOR, lz, lx + 2, WATER_TOP + 2, lz, Blocks.SPRUCE_LOG.defaultBlockState());
				}
			}
		}
		house(-PLAZA - 8, -6, 9, 7, 5, Blocks.SPRUCE_PLANKS.defaultBlockState(), Blocks.DARK_OAK_PLANKS.defaultBlockState());
		house(PLAZA + 4, -6, 9, 7, 5, Blocks.SPRUCE_PLANKS.defaultBlockState(), Blocks.DARK_OAK_PLANKS.defaultBlockState());
		house(-4, -PLAZA - 8, 8, 6, 4, Blocks.OAK_PLANKS.defaultBlockState(), Blocks.RED_TERRACOTTA.defaultBlockState());
		// Lighthouse on the shore.
		int lx = (int) (spec.radius() * 0.6);
		int lz = -(int) (spec.radius() * 0.5);
		tower(lx, lz, 3, 18, Blocks.WHITE_CONCRETE.defaultBlockState(), Blocks.RED_CONCRETE.defaultBlockState());
		set(spec.x() + lx, ground(lx, lz) + 19, spec.z() + lz, Blocks.GLOWSTONE.defaultBlockState());
		lampPosts();
	}

	private void sky(Random random) {
		trees(random, 4, false);
		// Floating islands with cloud skirts, rising in steps.
		int[][] floats = {{-14, -12, 22}, {12, -6, 34}, {0, 14, 46}, {-4, -2, 60}};
		for (int[] f : floats) {
			int cx = f[0];
			int cz = f[1];
			int base = WATER_TOP + f[2];
			for (int dx = -7; dx <= 7; dx++) {
				for (int dz = -7; dz <= 7; dz++) {
					double d = Math.sqrt(dx * dx + dz * dz);
					if (d > 7) {
						continue;
					}
					int depth = (int) Math.round((7 - d) * 0.8);
					for (int y = base - depth; y <= base; y++) {
						set(spec.x() + cx + dx, y, spec.z() + cz + dz, y == base ? Blocks.GRASS_BLOCK.defaultBlockState() : Blocks.DIRT.defaultBlockState());
					}
					if (d > 5.5) {
						set(spec.x() + cx + dx, base - depth - 1, spec.z() + cz + dz, Blocks.WHITE_WOOL.defaultBlockState());
					}
				}
			}
			if (f[2] >= 34) {
				houseAt(cx - 2, cz - 2, base + 1, 5, 5, 4, Blocks.BIRCH_PLANKS.defaultBlockState(), Blocks.WHITE_WOOL.defaultBlockState());
			}
		}
	}

	private void colosseum() {
		int r = Math.min(spec.radius() - 8, 18);
		int g = ground(0, 0);
		for (int dx = -r - 3; dx <= r + 3; dx++) {
			for (int dz = -r - 3; dz <= r + 3; dz++) {
				double d = Math.sqrt(dx * dx + dz * dz);
				if (d < r || d > r + 3) {
					continue;
				}
				double angle = Math.atan2(dz, dx);
				boolean arch = Math.abs(Math.sin(angle * 8)) < 0.25;
				int height = 12 - (int) Math.round((d - r) * 2);
				for (int y = 1; y <= height; y++) {
					if (arch && y <= 4 && d > r + 1.5) {
						continue;
					}
					set(spec.x() + dx, g + y, spec.z() + dz, y % 4 == 0 ? Blocks.CUT_SANDSTONE.defaultBlockState() : Blocks.SMOOTH_SANDSTONE.defaultBlockState());
				}
			}
		}
	}

	private void palace(Random random) {
		int pz = -PLAZA - 12;
		houseAt(-9, pz - 6, ground(0, pz) + 1, 18, 12, 7, Blocks.PURPUR_BLOCK.defaultBlockState(), Blocks.PRISMARINE_BRICKS.defaultBlockState());
		// Pagoda tiers on top.
		int g = ground(0, pz) + 9;
		for (int tier = 0; tier < 3; tier++) {
			int half = 7 - tier * 2;
			fill(-half, g + tier * 3, pz - half + 0, half, g + tier * 3, pz + half - 0, Blocks.DARK_PRISMARINE.defaultBlockState());
			fill(-half + 2, g + tier * 3 + 1, pz - half + 2, half - 2, g + tier * 3 + 2, pz + half - 2, Blocks.PURPUR_PILLAR.defaultBlockState());
		}
		tower(-14, pz + 2, 3, 14, Blocks.PURPUR_BLOCK.defaultBlockState(), Blocks.DARK_PRISMARINE.defaultBlockState());
		tower(14, pz + 2, 3, 14, Blocks.PURPUR_BLOCK.defaultBlockState(), Blocks.DARK_PRISMARINE.defaultBlockState());
		for (int i = 0; i < 6; i++) {
			double angle = random.nextDouble() * Math.PI * 2;
			double distance = PLAZA + 4 + random.nextDouble() * spec.radius() * 0.4;
			int lx = (int) Math.round(Math.cos(angle) * distance);
			int lz = (int) Math.round(Math.sin(angle) * distance);
			if (!beach(lx, lz) && lz > pz + 8) {
				tree(lx, lz, Blocks.CHERRY_LOG.defaultBlockState(), Blocks.CHERRY_LEAVES.defaultBlockState(), 5, 3);
			}
		}
	}

	private void crystal(Random random) {
		for (int i = 0; i < 9; i++) {
			double angle = i * Math.PI * 2 / 9;
			double distance = PLAZA + 6 + (i % 3) * 4;
			int lx = (int) Math.round(Math.cos(angle) * distance);
			int lz = (int) Math.round(Math.sin(angle) * distance);
			int height = 8 + random.nextInt(14);
			spire(lx, lz, height, i % 2 == 0 ? Blocks.AMETHYST_BLOCK.defaultBlockState() : Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState());
		}
		// Fountain ring around the plaza.
		int g = ground(0, 0);
		for (int dx = -PLAZA - 2; dx <= PLAZA + 2; dx++) {
			for (int dz = -PLAZA - 2; dz <= PLAZA + 2; dz++) {
				double d = Math.sqrt(dx * dx + dz * dz);
				if (d > PLAZA + 0.5 && d <= PLAZA + 1.5) {
					set(spec.x() + dx, g, spec.z() + dz, Blocks.WATER.defaultBlockState());
				}
			}
		}
	}

	// ------------------------------------------------------------------------------------------------ primitives

	private void house(int lx, int lz, int width, int depth, int height, BlockState wall, BlockState roof) {
		int g = Math.max(ground(lx, lz), Math.max(ground(lx + width, lz + depth), WATER_TOP + 1));
		houseAt(lx, lz, g + 1, width, depth, height, wall, roof);
		// Foundation down to the terrain.
		fill(lx, g - 3, lz, lx + width - 1, g, lz + depth - 1, Blocks.COBBLESTONE.defaultBlockState());
	}

	/** Box house with a door gap, windows and a stepped roof. {@code y} is the floor level. */
	private void houseAt(int lx, int lz, int y, int width, int depth, int height, BlockState wall, BlockState roof) {
		for (int dx = 0; dx < width; dx++) {
			for (int dz = 0; dz < depth; dz++) {
				boolean edge = dx == 0 || dz == 0 || dx == width - 1 || dz == depth - 1;
				for (int dy = 0; dy < height; dy++) {
					BlockState state = Blocks.AIR.defaultBlockState();
					if (edge) {
						boolean door = dz == 0 && dx == width / 2 && dy < 2;
						boolean window = dy == 2 && !door && (dx == 2 || dx == width - 3 || dz == 2 || dz == depth - 3);
						state = door ? Blocks.AIR.defaultBlockState() : window ? Blocks.GLASS.defaultBlockState() : wall;
					}
					set(spec.x() + lx + dx, y + dy, spec.z() + lz + dz, state);
				}
				set(spec.x() + lx + dx, y - 1, spec.z() + lz + dz, Blocks.SPRUCE_PLANKS.defaultBlockState());
			}
		}
		for (int step = 0; step <= Math.min(width, depth) / 2; step++) {
			fill(lx - 1 + step, y + height + step, lz - 1 + step, lx + width - step, y + height + step, lz + depth - step, roof);
		}
	}

	private void tower(int lx, int lz, int radius, int height, BlockState wall, BlockState roof) {
		int g = ground(lx, lz);
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dz = -radius; dz <= radius; dz++) {
				double d = Math.sqrt(dx * dx + dz * dz);
				if (d > radius + 0.5) {
					continue;
				}
				for (int y = FLOOR + 1; y <= g + height; y++) {
					boolean shell = d > radius - 0.8 || y <= g;
					set(spec.x() + lx + dx, y, spec.z() + lz + dz, shell ? wall : Blocks.AIR.defaultBlockState());
				}
			}
		}
		for (int y = 0; y <= radius + 2; y++) {
			int r = radius + 1 - y;
			disc(lx, lz, Math.max(0, r), height + 1 + y, roof, g);
		}
	}

	private void ring(int radius, int height, BlockState wall, boolean battlements) {
		for (int dx = -radius - 1; dx <= radius + 1; dx++) {
			for (int dz = -radius - 1; dz <= radius + 1; dz++) {
				double d = Math.sqrt(dx * dx + dz * dz);
				if (d < radius - 0.5 || d > radius + 0.7) {
					continue;
				}
				boolean gate = Math.abs(dx) <= 1 && dz > 0;
				int g = ground(dx, dz);
				for (int y = FLOOR + 1; y <= g + height; y++) {
					if (gate && y > g && y <= g + 3) {
						continue;
					}
					set(spec.x() + dx, y, spec.z() + dz, wall);
				}
				if (battlements && (dx + dz) % 2 == 0) {
					set(spec.x() + dx, g + height + 1, spec.z() + dz, wall);
				}
			}
		}
	}

	private void windmill(int lx, int lz) {
		tower(lx, lz, 3, 12, Blocks.WHITE_TERRACOTTA.defaultBlockState(), Blocks.SPRUCE_PLANKS.defaultBlockState());
		int hub = ground(lx, lz) + 11;
		int hz = lz + 4;
		for (int i = -7; i <= 7; i++) {
			set(spec.x() + lx + i, hub, spec.z() + hz, Blocks.WHITE_WOOL.defaultBlockState());
			set(spec.x() + lx, hub + i, spec.z() + hz, Blocks.WHITE_WOOL.defaultBlockState());
		}
		set(spec.x() + lx, hub, spec.z() + hz, Blocks.SPRUCE_LOG.defaultBlockState());
	}

	private void mountain(int lx, int lz, int radius, int height, BlockState body, BlockState cap) {
		int g = ground(lx, lz);
		for (int y = 0; y < height; y++) {
			int r = (int) Math.round(radius * (1.0 - y / (double) height));
			disc(lx, lz, r, y, y > height * 0.55 ? cap : body, g);
		}
	}

	private void pyramid(int lx, int lz, int half, BlockState body, BlockState trim) {
		int g = ground(lx, lz);
		for (int y = 0; y <= half; y++) {
			int h = half - y;
			fill(lx - h, g + y, lz - h, lx + h, g + y, lz + h, y % 3 == 2 ? trim : body);
		}
	}

	private void spire(int lx, int lz, int height, BlockState state) {
		int g = ground(lx, lz);
		for (int y = 0; y < height; y++) {
			int r = y < height / 3 ? 1 : 0;
			fill(lx - r, g + y, lz - r, lx + r, g + y, lz + r, state);
		}
	}

	private void tree(int lx, int lz, BlockState log, BlockState leaves, int height, int radius) {
		int g = ground(lx, lz);
		BlockState persistentLeaves = leaves.hasProperty(LeavesBlock.PERSISTENT) ? leaves.setValue(LeavesBlock.PERSISTENT, true) : leaves;
		for (int dy = -radius; dy <= radius; dy++) {
			for (int dx = -radius; dx <= radius; dx++) {
				for (int dz = -radius; dz <= radius; dz++) {
					if (dx * dx + dz * dz + dy * dy * 2 <= radius * radius + 1) {
						set(spec.x() + lx + dx, g + height + dy, spec.z() + lz + dz, persistentLeaves);
					}
				}
			}
		}
		for (int y = 1; y <= height; y++) {
			set(spec.x() + lx, g + y, spec.z() + lz, log);
		}
	}

	private void spruce(int lx, int lz, int height) {
		int g = ground(lx, lz);
		BlockState leaves = Blocks.SPRUCE_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
		for (int y = 2; y <= height + 1; y++) {
			int r = Math.max(0, (height + 1 - y) / 2);
			disc(lx, lz, r, y, leaves, g);
			set(spec.x() + lx, g + y + 1, spec.z() + lz, Blocks.SNOW.defaultBlockState());
		}
		for (int y = 1; y <= height; y++) {
			set(spec.x() + lx, g + y, spec.z() + lz, Blocks.SPRUCE_LOG.defaultBlockState());
		}
	}

	private void palm(int lx, int lz, int height) {
		if (beach(lx, lz) && ground(lx, lz) <= WATER_TOP) {
			return;
		}
		int g = ground(lx, lz);
		for (int y = 1; y <= height; y++) {
			set(spec.x() + lx, g + y, spec.z() + lz, Blocks.JUNGLE_LOG.defaultBlockState());
		}
		BlockState leaves = Blocks.JUNGLE_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
		for (Direction side : Direction.Plane.HORIZONTAL) {
			for (int i = 1; i <= 3; i++) {
				set(spec.x() + lx + side.getStepX() * i, g + height + (i == 3 ? -1 : 0), spec.z() + lz + side.getStepZ() * i, leaves);
			}
		}
		set(spec.x() + lx, g + height + 1, spec.z() + lz, leaves);
	}

	private void lampPosts() {
		int g = ground(PLAZA, 0);
		for (int i = 0; i < 4; i++) {
			int lx = i < 2 ? (i == 0 ? PLAZA : -PLAZA) : 0;
			int lz = i >= 2 ? (i == 2 ? PLAZA : -PLAZA) : 0;
			fill(lx, g + 1, lz, lx, g + 3, lz, Blocks.OAK_FENCE.defaultBlockState());
			set(spec.x() + lx, g + 4, spec.z() + lz, Blocks.LANTERN.defaultBlockState());
		}
	}

	/** Filled disc at height {@code g + dy} (or, without g, on the plaza level + dy). */
	private void disc(int lx, int lz, int radius, int dy, BlockState state, int g) {
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dz = -radius; dz <= radius; dz++) {
				if (dx * dx + dz * dz <= radius * radius + 1) {
					set(spec.x() + lx + dx, g + dy, spec.z() + lz + dz, state);
				}
			}
		}
	}

	private void disc(int lx, int lz, int radius, int dy, BlockState state) {
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dz = -radius; dz <= radius; dz++) {
				if (dx * dx + dz * dz <= radius * radius + 1) {
					set(spec.x() + lx + dx, ground(lx + dx, lz + dz) + dy, spec.z() + lz + dz, state);
				}
			}
		}
	}

	/** Island-local box fill. */
	private void fill(int x1, int y1, int z1, int x2, int y2, int z2, BlockState state) {
		for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
			for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) {
				for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++) {
					set(spec.x() + x, y, spec.z() + z, state);
				}
			}
		}
	}

	/** World-coordinate write, clipped to the chunk being generated. */
	private void set(int x, int y, int z, BlockState state) {
		if (x < minX || x >= minX + 16 || z < minZ || z >= minZ + 16 || y < level.getMinBuildHeight() || y >= level.getMaxBuildHeight()) {
			return;
		}
		level.setBlock(pos.set(x, y, z), state, 2);
	}

	@SuppressWarnings("unused")
	private static BlockState facing(BlockState state, Direction direction) {
		return state.hasProperty(BlockStateProperties.HORIZONTAL_FACING) ? state.setValue(BlockStateProperties.HORIZONTAL_FACING, direction) : state;
	}
}
