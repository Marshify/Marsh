package com.grandline.world.plan;

import com.grandline.world.plan.Anchor.Type;
import com.grandline.world.plan.Terrain.Ground;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Half;

import java.util.List;

/**
 * Sunscorch Dunes (Lv 75), rebuilt: a sandy island about 150 x 133 blocks with rolling dunes, a flat village plateau
 * in the middle and a raised upland to the north carrying two pyramids.
 * <ul>
 * <li>South: a timber pier and an arrival plaza with a dockside shelter; a broad road runs north to the market.</li>
 * <li>Centre: a walled-in market square (a well, striped stalls, benches, pottery) ringed by an inn, a trading house
 * with a courtyard and six homes, all flat-roofed sandstone with parapets, awnings, exterior stairs and furnished
 * interiors. Doors stand open so the interiors can be walked into in adventure mode.</li>
 * <li>North: the Great Pyramid (33 wide, 23 tall) with a pillared gateway, a corridor and an altar chamber, and the
 * weathered Lesser Pyramid (23 wide, 15 tall) with a burial room. Between them, south of both, the raider ruins.</li>
 * <li>West: a palm oasis with a pool, grass, reeds and a ruined colonnade. East: the bandit camp of tents and cover.</li>
 * <li>The quest giver stands at the village's east gate, on the road to the bandit camp.</li>
 * </ul>
 * Island-local coordinates: +x east, +z south, sea level 62. Built entirely from code: identical in every world and
 * assembled chunk by chunk. Worlds created before this design keep the original Sunscorch
 * ({@link FirstSeaPlans} legacy plan).
 */
final class SunscorchDunes extends BasePlan {
	/** Village ground (top block). */
	static final int G = 66;

	// ---- landmarks (island-local) ----
	static final int MARKET_X = 0, MARKET_Z = 16;
	static final int GREAT_X = -18, GREAT_Z = -48, GREAT_HALF = 16, GREAT_H = 22, GREAT_BASE = 69;
	static final int LESSER_X = 28, LESSER_Z = -50, LESSER_HALF = 11, LESSER_H = 15, LESSER_BASE = 69;
	static final int RUINS_X = 8, RUINS_Z = -27, RUINS_Y = 68;
	static final int CAMP_X = 57, CAMP_Z = 5, CAMP_Y = 67;
	static final int OASIS_X = -52, OASIS_Z = 10;
	static final int QUEST_X = 32, QUEST_Z = 18;
	static final int DOCK_X = 8, DOCK_LAND_Z = 53, DOCK_END_Z = 84, DOCK_Y = 64, PLAZA_Z = 48;

	// ---- roads ----
	static final double[] MAIN_ROAD = {DOCK_X, 50, 7, 40, 4, 31, 1, 26};
	static final double[] EAST_ROAD = {11, 17, 22, 18, QUEST_X - 1, 18, 42, 13, CAMP_X - 9, 8};
	static final double[] NORTH_ROAD = {0, 6, 2, -4, 5, -14, RUINS_X - 2, -20};
	static final double[] GREAT_ROAD = {RUINS_X - 6, -26, -8, -29, GREAT_X, -31};
	static final double[] LESSER_ROAD = {RUINS_X + 6, -28, 20, -33, LESSER_X, -37};
	static final double[] WEST_ROAD = {-11, 16, -24, 18, -36, 16, OASIS_X + 9, 12};
	static final double[] SOUTH_LANE = {-8, 26, -16, 32, -24, 34};
	static final double[] SE_LANE = {9, 26, 16, 31, 20, 34};

	/** A building: centre, w along its front, d front to back, facing, floors, look, use. */
	record Bldg(int cx, int cz, int w, int d, Direction front, int floors, Use use, Block wall, Block trim, Block awning, int stairsSide) {
	}

	enum Use { INN, TRADE, HOME_WEAVER, HOME_POTTER, HOME_FAMILY, HOME_ELDER, HOME_SCHOLAR, HOME_FISHER, SHELTER }

	static final List<Bldg> BUILDINGS = List.of(
			new Bldg(-21, 11, 11, 9, Direction.EAST, 2, Use.INN, Blocks.SMOOTH_SANDSTONE, Blocks.CUT_SANDSTONE, Blocks.RED_WOOL, 0),
			new Bldg(18, 8, 13, 10, Direction.WEST, 1, Use.TRADE, Blocks.SANDSTONE, Blocks.CUT_SANDSTONE, Blocks.CYAN_WOOL, 0),
			new Bldg(-6, -3, 9, 7, Direction.SOUTH, 2, Use.HOME_ELDER, Blocks.SMOOTH_SANDSTONE, Blocks.CHISELED_SANDSTONE, Blocks.ORANGE_WOOL, 0),
			new Bldg(10, -4, 7, 7, Direction.SOUTH, 1, Use.HOME_POTTER, Blocks.SANDSTONE, Blocks.CUT_SANDSTONE, Blocks.LIME_WOOL, 1),
			new Bldg(-23, 33, 8, 7, Direction.NORTH, 1, Use.HOME_FAMILY, Blocks.SMOOTH_SANDSTONE, Blocks.CUT_SANDSTONE, Blocks.YELLOW_WOOL, -1),
			new Bldg(-7, 35, 7, 7, Direction.NORTH, 1, Use.HOME_WEAVER, Blocks.SANDSTONE, Blocks.SMOOTH_SANDSTONE, Blocks.MAGENTA_WOOL, 1),
			new Bldg(21, 37, 9, 8, Direction.NORTH, 2, Use.HOME_SCHOLAR, Blocks.SMOOTH_SANDSTONE, Blocks.CUT_SANDSTONE, Blocks.BLUE_WOOL, 0),
			new Bldg(-36, 26, 7, 7, Direction.EAST, 1, Use.HOME_FISHER, Blocks.SANDSTONE, Blocks.CUT_SANDSTONE, Blocks.LIGHT_BLUE_WOOL, -1),
			new Bldg(18, 47, 7, 5, Direction.WEST, 1, Use.SHELTER, Blocks.CUT_SANDSTONE, Blocks.SMOOTH_SANDSTONE, Blocks.ORANGE_WOOL, 0));

	/** Palms: x, z, height, lean direction (0 N, 1 E, 2 S, 3 W). */
	static final int[][] PALMS = {
			// oasis
			{-61, 5, 8, 3}, {-58, 18, 7, 2}, {-44, 3, 7, 0}, {-45, 18, 9, 1}, {-63, 13, 6, 3}, {-50, 0, 8, 0},
			// village and market
			{-9, 7, 7, 3}, {9, 25, 6, 1}, {-12, 26, 8, 2}, {28, 8, 7, 1}, {-31, 4, 6, 0}, {30, 28, 7, 1}, {-30, 40, 6, 2},
			// dock and beaches
			{-2, 48, 7, 3}, {20, 55, 6, 2}, {-40, 46, 7, 2}, {44, 40, 6, 1}, {-68, 30, 6, 3}, {66, -18, 6, 1}, {40, 52, 6, 2},
			// pyramid road and ruins
			{-4, -16, 7, 3}, {20, -20, 6, 1}};

	/** Small boulder clusters (rock outcrops) at fixed spots, away from roads. */
	static final int[][] ROCKS = {{-38, -22}, {36, -24}, {60, 30}, {-62, -14}, {52, -32}, {66, -34}, {-48, 36}, {26, 44}, {-30, -36}, {66, 18}};

	/** Lamp posts along the roads. */
	static final int[][] LAMPS = {{5, 50}, {11, 44}, {5, 38}, {1, 30}, {24, 20}, {38, 16}, {3, -2}, {7, -12}, {-22, 20}, {-34, 18},
			{OASIS_X + 8, 15}, {-6, -27}, {18, -31}, {-10, 29}, {13, 29}};

	SunscorchDunes() {
		super("Sunscorch Dunes", 0.07);
		BlockState road = s(Blocks.SMOOTH_SANDSTONE);
		BlockState lane = s(Blocks.SANDSTONE);
		terrain.mass(0, 0, 66, 54, 3.6, 4.0, Ground.SAND)                  // the island: a broad, flat-topped sand table
				.mass(-2, -46, 48, 22, 6.8, 3.0, Ground.SAND)              // the northern upland under the pyramids
				.mass(CAMP_X, CAMP_Z, 20, 18, 4.4, 3.0, Ground.SAND)       // the eastern lobe (bandit camp)
				.mass(OASIS_X - 4, OASIS_Z, 20, 22, 3.8, 3.0, Ground.SAND) // the western lobe (oasis)
				.mass(DOCK_X, 44, 18, 10, 3.2, 3.0, Ground.SAND)           // the southern shore by the dock
				// Dunes: soft, rounded mounds between the landmarks.
				.mass(-38, -24, 13, 8, 7.8, 1.6, Ground.SAND)
				.mass(38, -24, 10, 7, 7.2, 1.6, Ground.SAND)
				.mass(58, 30, 11, 8, 6.6, 1.6, Ground.SAND)
				.mass(-50, 36, 12, 8, 6.2, 1.6, Ground.SAND)
				.mass(-64, -14, 9, 7, 6.0, 1.6, Ground.SAND)
				.mass(30, 44, 9, 6, 5.6, 1.6, Ground.SAND)
				.mass(-30, -38, 8, 6, 8.5, 1.6, Ground.SAND)
				.mass(60, -26, 9, 6, 8.0, 1.3, Ground.RED_SAND)           // a red-sand ridge with exposed rock (north-east)
				// Level ground for the settlement and the landmarks.
				.flat(0, 14, 36, 28, G, 8, null)                           // the village plateau
				.flat(GREAT_X, GREAT_Z, GREAT_HALF + 4, GREAT_HALF + 4, GREAT_BASE, 6, null)
				.flat(LESSER_X, LESSER_Z, LESSER_HALF + 4, LESSER_HALF + 4, LESSER_BASE, 5, null)
				.flat(RUINS_X, RUINS_Z, 9, 8, RUINS_Y, 5, null)
				.flat(CAMP_X, CAMP_Z, 11, 10, CAMP_Y, 5, null)
				.flat(OASIS_X, OASIS_Z, 14, 12, G, 5, null)
				.flat(OASIS_X, OASIS_Z, 7, 5, 65, 2, s(Blocks.WATER))      // the oasis pool
				.flat(QUEST_X, QUEST_Z, 4, 4, G, 4, null)
				.flat(DOCK_X, PLAZA_Z, 7, 5, 65, 4, s(Blocks.SMOOTH_SANDSTONE)) // the arrival plaza
				.flat(MARKET_X, MARKET_Z, 11, 9, G, 0, s(Blocks.SMOOTH_SANDSTONE)) // the market square
				// Roads (first matching path wins): smooth sandstone main roads, sandstone lanes.
				.path(road, 4, MAIN_ROAD).path(road, 3, EAST_ROAD).path(road, 3, NORTH_ROAD)
				.path(lane, 3, GREAT_ROAD).path(lane, 3, LESSER_ROAD).path(lane, 3, WEST_ROAD)
				.path(lane, 2, SOUTH_LANE).path(lane, 2, SE_LANE);
		for (Bldg b : BUILDINGS) {
			if (b.use() == Use.SHELTER) {
				continue;
			}
			int[] door = doorStep(b);
			double dx = MARKET_X - door[0], dz = MARKET_Z - door[1], len = Math.hypot(dx, dz);
			double reach = Math.max(0, len - 9.5);
			terrain.path(lane, 2, door[0], door[1], door[0] + dx / len * reach, door[1] + dz / len * reach);
		}
		terrain.reach(DOCK_END_Z + 4);

		anchor(Anchor.at(Type.QUEST, QUEST_X, Anchor.SURFACE, QUEST_Z, 90));          // at the east gate, facing the village
		anchor(new Anchor(Type.ARRIVAL, DOCK_X, Anchor.SURFACE, PLAZA_Z - 1, 0, 180)); // the arrival plaza, facing the village
		int[] inn = doorStep(BUILDINGS.get(0));
		anchor(Anchor.at(Type.HOME, inn[0] + 1, Anchor.SURFACE, inn[1] + 2, 90));     // the Innkeeper, outside the inn
		anchor(Anchor.at(Type.FISHERMAN, DOCK_X + 4, DOCK_Y + 1, DOCK_END_Z - 2, 0)); // on the pier's T-head
		anchor(Anchor.at(Type.DOCK, DOCK_X - 9, 63, DOCK_END_Z - 3));
		anchor(Anchor.area(Type.ENEMY_CAMP, CAMP_X, CAMP_Z, 6));                      // the bandit camp
		anchor(Anchor.area(Type.ENEMY_CAMP, RUINS_X, RUINS_Z, 6));                    // the raider ruins
		anchor(Anchor.area(Type.BOSS_ARENA, RUINS_X, RUINS_Z, 5));                    // quest bosses appear in the ruins
		anchor(Anchor.of(Type.TREASURE, -66, 26));
	}

	// ==================================================================================================== helpers

	static long hash(int x, int z) {
		long h = x * 0x9E3779B97F4A7C15L ^ z * 0xC2B2AE3D27D4EB4FL;
		h ^= h >>> 29;
		h *= 0xBF58476D1CE4E5B9L;
		return h ^ (h >>> 32);
	}

	static double unit(int x, int z, int salt) {
		return ((hash(x * 31 + salt, z * 17 - salt) >>> 11) & 0xFFFF) / 65536.0;
	}

	static boolean near(int x, int z, int cx, int cz, int r) {
		return (x - cx) * (x - cx) + (z - cz) * (z - cz) <= r * r;
	}

	/** Island position of the block just outside a building's door. */
	static int[] doorStep(Bldg b) {
		Frame f = new Frame(null, b, 0);
		int u = (b.w() - 1) / 2;
		return new int[]{f.x(u, b.d()), f.z(u, b.d())};
	}

	/** Island bounding box (minX, minZ, maxX, maxZ) of a building's walls. */
	static int[] footprint(Bldg b) {
		Frame f = new Frame(null, b, 0);
		int x1 = f.x(0, 0), z1 = f.z(0, 0), x2 = f.x(b.w() - 1, b.d() - 1), z2 = f.z(b.w() - 1, b.d() - 1);
		return new int[]{Math.min(x1, x2), Math.min(z1, z2), Math.max(x1, x2), Math.max(z1, z2)};
	}

	/** Areas kept free of plants and rocks: buildings and their porches, landmarks, camps, the dock and the market. */
	static boolean built(int x, int z, int margin) {
		for (Bldg b : BUILDINGS) {
			int[] box = footprint(b);
			int m = margin + 3;
			if (x >= box[0] - m && x <= box[2] + m && z >= box[1] - m && z <= box[3] + m) {
				return true;
			}
		}
		if (x >= 22 && x <= 30 && z >= 0 && z <= 16) { // the trading house courtyard
			return true;
		}
		return Math.abs(x - GREAT_X) <= GREAT_HALF + 4 + margin && Math.abs(z - GREAT_Z) <= GREAT_HALF + 6 + margin
				|| Math.abs(x - LESSER_X) <= LESSER_HALF + 4 + margin && Math.abs(z - LESSER_Z) <= LESSER_HALF + 5 + margin
				|| near(x, z, RUINS_X, RUINS_Z, 10 + margin) || near(x, z, CAMP_X, CAMP_Z, 11 + margin)
				|| near(x, z, MARKET_X, MARKET_Z, 12 + margin) || near(x, z, QUEST_X + 1, QUEST_Z, 4 + margin)
				|| near(x, z, OASIS_X, OASIS_Z, 9 + margin)
				|| (Math.abs(x - DOCK_X) <= 9 && z >= 42);
	}

	static BlockState stair(Block block, Direction facing, boolean top) {
		return Kit.stairs(block, facing, top);
	}

	static BlockState log(Block block, Direction.Axis axis) {
		return s(block).setValue(RotatedPillarBlock.AXIS, axis);
	}

	static BlockState lantern(boolean hanging) {
		return s(Blocks.LANTERN).setValue(LanternBlock.HANGING, hanging);
	}

	static BlockState candles(int n, boolean lit) {
		return s(Blocks.CANDLE).setValue(CandleBlock.CANDLES, n).setValue(CandleBlock.LIT, lit);
	}

	/** A door standing open (adventure mode can't open doors, so they are left open), facing out of the building. */
	static BlockState openDoor(Block door, Direction facing, DoubleBlockHalf half) {
		return s(door).setValue(BlockStateProperties.HORIZONTAL_FACING, facing).setValue(BlockStateProperties.OPEN, true)
				.setValue(BlockStateProperties.DOOR_HINGE, DoorHingeSide.LEFT).setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, half);
	}

	// ==================================================================================================== build

	@Override
	public void build(Canvas c) {
		groundCover(c);
		oasis(c);
		dock(c);
		market(c);
		for (Bldg b : BUILDINGS) {
			int[] box = footprint(b);
			if (c.near((box[0] + box[2]) / 2, (box[1] + box[3]) / 2, 20)) {
				building(c, b);
			}
		}
		courtyard(c);
		questPost(c);
		if (c.near(GREAT_X, GREAT_Z, GREAT_HALF + 8)) {
			greatPyramid(c);
		}
		if (c.near(LESSER_X, LESSER_Z, LESSER_HALF + 8)) {
			lesserPyramid(c);
		}
		if (c.near(RUINS_X, RUINS_Z, 14)) {
			raiderRuins(c);
		}
		if (c.near(CAMP_X, CAMP_Z, 14)) {
			banditCamp(c);
		}
		for (int[] r : ROCKS) {
			if (c.near(r[0], r[1], 3)) {
				rock(c, r[0], r[1]);
			}
		}
		for (int[] p : PALMS) {
			if (c.near(p[0], p[1], 6)) {
				palm(c, p[0], p[1], p[2], p[3]);
			}
		}
		for (int[] l : LAMPS) {
			if (c.inChunk(l[0], l[1])) {
				lampPost(c, l[0], l[1]);
			}
		}
		treasure(c);
	}

	// ---------------------------------------------------------------------------------------------------- ground

	/**
	 * Surface detail: worn road surfaces (a mix of smooth, cut and plain sandstone), red sand drifts on the dunes,
	 * exposed sandstone on steep dune faces, and sparse dead bushes and cacti on open sand (never on roads, near
	 * buildings or in the combat areas).
	 */
	private void groundCover(Canvas c) {
		BlockState road = s(Blocks.SMOOTH_SANDSTONE), lane = s(Blocks.SANDSTONE);
		for (int x = -80; x <= 80; x++) {
			for (int z = -72; z <= 72; z++) {
				if (!c.inChunk(x, z)) {
					continue;
				}
				int g = c.ground(x, z);
				if (g <= Terrain.WATER_TOP + 1) {
					continue;
				}
				BlockState paved = c.terrain().paving(x, z);
				double r = unit(x, z, 7);
				if (paved != null) {
					if (paved == road && !near(x, z, MARKET_X, MARKET_Z, 10)) {
						c.set(x, g, z, r < 0.14 ? s(Blocks.CUT_SANDSTONE) : r < 0.22 ? s(Blocks.SANDSTONE) : road);
					} else if (paved == lane) {
						c.set(x, g, z, r < 0.18 ? s(Blocks.SMOOTH_SANDSTONE) : r < 0.26 ? s(Blocks.SAND) : lane);
					}
					continue;
				}
				// Steep faces show the sandstone under the sand.
				int drop = 0;
				for (int[] n : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
					drop = Math.max(drop, g - c.ground(x + n[0], z + n[1]));
				}
				boolean redRidge = near(x, z, 60, -26, 11);
				if (drop >= 2) {
					c.set(x, g, z, redRidge ? s(Blocks.SMOOTH_RED_SANDSTONE) : r < 0.5 ? s(Blocks.SANDSTONE) : s(Blocks.SMOOTH_SANDSTONE));
					c.set(x, g - 1, z, redRidge ? s(Blocks.RED_SANDSTONE) : s(Blocks.SANDSTONE));
					continue;
				}
				if (built(x, z, 0)) {
					continue;
				}
				double drift = (Math.sin(x * 0.13 + 0.7) + Math.sin(z * 0.11 + 2.1) + Math.sin((x - z) * 0.07)) / 3.0;
				if (drift > 0.55 && g >= G + 2 && !redRidge) {
					c.set(x, g, z, s(Blocks.RED_SAND));
				}
				if (r < 0.022) {
					c.set(x, g + 1, z, s(Blocks.DEAD_BUSH));
				} else if (r > 0.994 && g >= G && cactusRoom(c, x, z, g)) {
					int h = 1 + (int) (unit(x, z, 8) * 3);
					c.fill(x, g + 1, z, x, g + h, z, s(Blocks.CACTUS));
				}
			}
		}
	}

	/** Cacti need sand below and nothing beside them. */
	private static boolean cactusRoom(Canvas c, int x, int z, int g) {
		if (c.terrain().top(x, z).getBlock() != Blocks.SAND) {
			return false;
		}
		for (int[] n : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
			if (c.ground(x + n[0], z + n[1]) > g || c.terrain().paving(x + n[0], z + n[1]) != null) {
				return false;
			}
		}
		return true;
	}

	private static void lampPost(Canvas c, int x, int z) {
		int g = c.ground(x, z);
		c.set(x, g + 1, z, s(Blocks.SANDSTONE_WALL));
		c.set(x, g + 2, z, s(Blocks.SANDSTONE_WALL));
		c.set(x, g + 3, z, s(Blocks.JUNGLE_FENCE));
		c.set(x, g + 4, z, lantern(false));
	}

	/** A boulder cluster: rounded sandstone and terracotta stones, two or three blocks high. */
	private static void rock(Canvas c, int x, int z) {
		int g = c.ground(x, z);
		if (g <= Terrain.WATER_TOP + 1) {
			return;
		}
		Block[] stone = {Blocks.SANDSTONE, Blocks.SMOOTH_SANDSTONE, Blocks.TERRACOTTA, Blocks.ORANGE_TERRACOTTA, Blocks.CUT_SANDSTONE};
		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) {
				double d = Math.hypot(dx, dz * 1.2);
				double u = unit(x + dx, z + dz, 21);
				int h = d <= 0.5 ? 3 : d <= 1.5 ? (u < 0.6 ? 2 : 1) : d <= 2.3 && u < 0.45 ? 1 : 0;
				int base = c.ground(x + dx, z + dz);
				for (int y = 0; y < h; y++) {
					c.set(x + dx, base + 1 + y, z + dz, s(stone[(int) (unit(x + dx, z + dz + y, 22) * stone.length)]));
				}
				if (h == 1 && u > 0.85) {
					c.set(x + dx, base + 2, z + dz, Kit.slab(Blocks.SANDSTONE_SLAB, false));
				}
			}
		}
		c.set(x + 2, c.ground(x + 2, z + 1) + 1, z + 1, s(Blocks.DEAD_BUSH));
	}

	/**
	 * A palm: a trunk that leans and curves over its height, a crown of eight drooping fronds of jungle leaves with a
	 * tuft on top, and a cluster of coconuts (cocoa pods) under the crown.
	 */
	static void palm(Canvas c, int x, int z, int height, int lean) {
		int g = c.ground(x, z);
		if (g <= Terrain.WATER_TOP) {
			return;
		}
		int lx = lean == 1 ? 1 : lean == 3 ? -1 : 0, lz = lean == 2 ? 1 : lean == 0 ? -1 : 0;
		int tx = x, tz = z, ty = g;
		for (int i = 1; i <= height; i++) {
			double t = i / (double) height;
			int off = (int) Math.round(t * t * 2.4);
			tx = x + lx * off;
			tz = z + lz * off;
			ty = g + i;
			c.set(tx, ty, tz, log(Blocks.JUNGLE_LOG, Direction.Axis.Y));
			// Fill the diagonal gap where the trunk steps sideways, so it reads as one curved trunk.
			int prev = (int) Math.round(((i - 1) / (double) height) * ((i - 1) / (double) height) * 2.4);
			if (prev != off) {
				c.set(x + lx * prev, ty, z + lz * prev, log(Blocks.JUNGLE_LOG, Direction.Axis.Y));
			}
		}
		BlockState leaf = Kit.leaves(Blocks.JUNGLE_LEAVES);
		c.set(tx, ty + 1, tz, leaf);
		int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
		for (int[] d : dirs) {
			boolean diag = d[0] != 0 && d[1] != 0;
			int len = diag ? 3 : 4;
			for (int k = 1; k <= len; k++) {
				int dy = k == 1 ? 1 : k == 2 ? 0 : k == 3 ? (diag ? -1 : 0) : -1;
				c.set(tx + d[0] * k, ty + dy, tz + d[1] * k, leaf);
			}
			// Drooping frond tip.
			c.set(tx + d[0] * len, ty - (diag ? 2 : 2), tz + d[1] * len, leaf);
		}
		c.set(tx, ty + 1, tz + 1, leaf);
		c.set(tx, ty + 1, tz - 1, leaf);
		c.set(tx + 1, ty + 1, tz, leaf);
		c.set(tx - 1, ty + 1, tz, leaf);
		// Coconuts hang against the top of the trunk.
		c.set(tx + 1, ty - 1, tz, s(Blocks.COCOA).setValue(CocoaBlock.FACING, Direction.WEST).setValue(CocoaBlock.AGE, 2));
		c.set(tx, ty - 1, tz + 1, s(Blocks.COCOA).setValue(CocoaBlock.FACING, Direction.NORTH).setValue(CocoaBlock.AGE, 2));
	}

	// ---------------------------------------------------------------------------------------------------- oasis

	/**
	 * The oasis: a clear pool two blocks deep with a sandstone lip on the north, a ring of grass with ferns, reeds
	 * (sugar cane) at the water's edge, lily pads, a ruined colonnade with seats on the west, and palms around it.
	 */
	private void oasis(Canvas c) {
		if (!c.near(OASIS_X, OASIS_Z, 16)) {
			return;
		}
		for (int x = OASIS_X - 15; x <= OASIS_X + 15; x++) {
			for (int z = OASIS_Z - 13; z <= OASIS_Z + 13; z++) {
				if (!c.inChunk(x, z)) {
					continue;
				}
				double dx = (x - OASIS_X) / 7.0, dz = (z - OASIS_Z) / 5.0, e = Math.sqrt(dx * dx + dz * dz);
				int g = c.ground(x, z);
				if (e <= 1.0) {
					c.set(x, 64, z, s(Blocks.WATER));
					c.set(x, 63, z, s(Blocks.SAND));
					if (unit(x, z, 31) < 0.06 && e < 0.8) {
						c.set(x, 66, z, s(Blocks.LILY_PAD));
					}
				} else if (e <= 1.9 && c.terrain().paving(x, z) == null) {
					// Grass ring, thinning out into the sand.
					double r = unit(x, z, 32);
					if (e <= 1.45 || r < 0.55) {
						c.set(x, g, z, s(Blocks.GRASS_BLOCK));
						c.set(x, g - 1, z, s(Blocks.DIRT));
						if (e <= 1.25 && r < 0.18 && (z < OASIS_Z || x > OASIS_X + 3)) {
							c.set(x, g + 1, z, s(Blocks.SUGAR_CANE));
							c.set(x, g + 2, z, s(Blocks.SUGAR_CANE));
						} else if (r > 0.8) {
							c.set(x, g + 1, z, s(Blocks.GRASS));
						} else if (r > 0.72) {
							c.set(x, g + 1, z, s(Blocks.FERN));
						}
					}
				}
			}
		}
		// The ruined colonnade on the west bank: four columns (two broken), a lintel, and stone seats.
		int cx = OASIS_X - 10;
		int[] heights = {5, 3, 5, 2};
		for (int i = 0; i < 4; i++) {
			int z = OASIS_Z - 5 + i * 3;
			int g = c.ground(cx, z);
			c.set(cx, g + 1, z, s(Blocks.CHISELED_SANDSTONE));
			c.fill(cx, g + 2, z, cx, g + heights[i], z, s(Blocks.CUT_SANDSTONE));
			if (heights[i] >= 5) {
				c.set(cx, g + 6, z, s(Blocks.CHISELED_SANDSTONE));
			}
		}
		int g0 = c.ground(cx, OASIS_Z - 5);
		c.fill(cx, g0 + 7, OASIS_Z - 5, cx, g0 + 7, OASIS_Z + 1, Kit.slab(Blocks.SMOOTH_SANDSTONE_SLAB, false));
		c.set(cx + 1, c.ground(cx + 1, OASIS_Z + 4) + 1, OASIS_Z + 4, s(Blocks.CUT_SANDSTONE)); // a fallen drum
		c.set(cx + 2, c.ground(cx + 2, OASIS_Z + 5) + 1, OASIS_Z + 5, Kit.slab(Blocks.CUT_SANDSTONE_SLAB, false));
		for (int z : new int[]{OASIS_Z - 3, OASIS_Z + 3}) {
			int g = c.ground(cx + 3, z);
			c.set(cx + 3, g + 1, z, stair(Blocks.SMOOTH_SANDSTONE_STAIRS, Direction.WEST, false));
		}
		int lip = c.ground(OASIS_X, OASIS_Z - 6);
		c.fill(OASIS_X - 2, lip + 1, OASIS_Z - 6, OASIS_X + 2, lip + 1, OASIS_Z - 6, Kit.slab(Blocks.SMOOTH_SANDSTONE_SLAB, false));
		c.set(OASIS_X, lip + 1, OASIS_Z - 6, s(Blocks.POTTED_FERN));
	}

	// ---------------------------------------------------------------------------------------------------- dock

	/**
	 * The arrival plaza (smooth sandstone with a cut-sandstone border) and a long timber pier: a five-wide walkway on log
	 * posts down to the sea floor with fence rails and lanterns, ending in a wide T-head with cargo; mooring posts.
	 */
	private void dock(Canvas c) {
		if (!c.near(DOCK_X, (DOCK_LAND_Z + DOCK_END_Z) / 2, 26)) {
			return;
		}
		for (int x = DOCK_X - 7; x <= DOCK_X + 7; x++) {
			for (int z = PLAZA_Z - 5; z <= PLAZA_Z + 5; z++) {
				double dx = (x - DOCK_X) / 7.0, dz = (z - PLAZA_Z) / 5.0, e = dx * dx + dz * dz;
				if (e <= 1.0) {
					c.set(x, 65, z, e > 0.7 ? s(Blocks.CUT_SANDSTONE) : unit(x, z, 41) < 0.15 ? s(Blocks.SANDSTONE) : s(Blocks.SMOOTH_SANDSTONE));
					c.fill(x, 66, z, x, 68, z, s(Blocks.AIR));
				}
			}
		}
		// Gateway posts at the head of the pier.
		for (int x : new int[]{DOCK_X - 3, DOCK_X + 3}) {
			c.fill(x, 66, DOCK_LAND_Z, x, 69, DOCK_LAND_Z, s(Blocks.CUT_SANDSTONE));
			c.set(x, 70, DOCK_LAND_Z, s(Blocks.CHISELED_SANDSTONE));
			c.set(x, 71, DOCK_LAND_Z, lantern(false));
		}
		// Step down from the plaza to the deck.
		for (int x = DOCK_X - 2; x <= DOCK_X + 2; x++) {
			c.set(x, 65, DOCK_LAND_Z - 1, Kit.slab(Blocks.SMOOTH_SANDSTONE_SLAB, false));
			c.fill(x, 66, DOCK_LAND_Z - 1, x, 68, DOCK_LAND_Z - 1, s(Blocks.AIR));
		}
		for (int z = DOCK_LAND_Z; z <= DOCK_END_Z; z++) {
			boolean head = z >= DOCK_END_Z - 6;
			int half = head ? 8 : 2;
			for (int x = DOCK_X - half; x <= DOCK_X + half; x++) {
				int w = x - DOCK_X;
				boolean edgeX = Math.abs(w) == half;
				c.set(x, DOCK_Y, z, (w == 0 && !head) || (head && z == DOCK_END_Z - 3)
						? log(Blocks.STRIPPED_JUNGLE_LOG, head && z == DOCK_END_Z - 3 ? Direction.Axis.X : Direction.Axis.Z) : s(Blocks.JUNGLE_PLANKS));
				c.fill(x, DOCK_Y + 1, z, x, DOCK_Y + 4, z, s(Blocks.AIR));
				boolean post = (edgeX && ((z - DOCK_LAND_Z) % 4 == 0 || z == DOCK_END_Z || z == DOCK_END_Z - 6))
						|| (head && (z == DOCK_END_Z || z == DOCK_END_Z - 6) && Math.abs(w) % 4 == 0);
				if (post) {
					int floor = Math.min(c.ground(x, z), Terrain.WATER_TOP - 1);
					c.fill(x, floor, z, x, DOCK_Y - 1, z, s(Blocks.JUNGLE_LOG));
					if (edgeX || z == DOCK_END_Z) {
						c.set(x, DOCK_Y + 1, z, s(Blocks.JUNGLE_FENCE));
						if ((z - DOCK_LAND_Z) % 8 == 0 || z == DOCK_END_Z) {
							c.set(x, DOCK_Y + 2, z, lantern(false));
						}
					}
				} else if (edgeX && !head) {
					c.set(x, DOCK_Y - 1, z, log(Blocks.STRIPPED_JUNGLE_LOG, Direction.Axis.Z));
					if ((z - DOCK_LAND_Z) % 8 != 6) {
						c.set(x, DOCK_Y + 1, z, Kit.connected(Blocks.JUNGLE_FENCE, false)); // rope rail (gaps to board boats)
					}
				} else if (head && z == DOCK_END_Z && Math.abs(w) < half) {
					c.set(x, DOCK_Y + 1, z, Kit.connected(Blocks.JUNGLE_FENCE, true));
				} else if (head && edgeX && z != DOCK_END_Z - 3) {
					c.set(x, DOCK_Y + 1, z, Kit.connected(Blocks.JUNGLE_FENCE, false));
				}
			}
		}
		// Cargo on the T-head: barrels, crates, a covered stack and sacks (hay).
		int hz = DOCK_END_Z - 5;
		int y = DOCK_Y + 1;
		c.set(DOCK_X - 7, y, hz, Canvas.facing(s(Blocks.BARREL), Direction.UP));
		c.set(DOCK_X - 6, y, hz, Canvas.facing(s(Blocks.BARREL), Direction.UP));
		c.set(DOCK_X - 7, y + 1, hz, Canvas.facing(s(Blocks.BARREL), Direction.UP));
		c.set(DOCK_X - 7, y, hz + 1, s(Blocks.BARREL));
		c.set(DOCK_X - 5, y, hz, s(Blocks.HAY_BLOCK));
		c.set(DOCK_X + 6, y, hz + 4, Canvas.facing(s(Blocks.BARREL), Direction.UP));
		c.set(DOCK_X + 7, y, hz + 4, Canvas.facing(s(Blocks.BARREL), Direction.UP));
		c.set(DOCK_X + 7, y, hz + 3, s(Blocks.ORANGE_WOOL));
		c.set(DOCK_X + 7, y + 1, hz + 4, Kit.slab(Blocks.JUNGLE_SLAB, false));
		// Cargo along the plaza edge, out of the walkway.
		c.set(DOCK_X - 6, 66, PLAZA_Z + 2, Canvas.facing(s(Blocks.BARREL), Direction.UP));
		c.set(DOCK_X - 6, 66, PLAZA_Z + 1, Canvas.facing(s(Blocks.BARREL), Direction.UP));
		c.set(DOCK_X - 5, 66, PLAZA_Z + 2, s(Blocks.DECORATED_POT));
		c.set(DOCK_X - 6, 67, PLAZA_Z + 2, s(Blocks.DECORATED_POT));
	}

	// ---------------------------------------------------------------------------------------------------- market

	/**
	 * The market square: a paved plaza with a patterned border, a roofed well at its heart, six striped stalls set
	 * around the edge with goods on their counters, stone benches, pottery and a cart.
	 */
	private void market(Canvas c) {
		if (!c.near(MARKET_X, MARKET_Z, 14)) {
			return;
		}
		// Paving pattern: a ring of cut sandstone and a star of chiseled sandstone around the well.
		for (int x = MARKET_X - 11; x <= MARKET_X + 11; x++) {
			for (int z = MARKET_Z - 9; z <= MARKET_Z + 9; z++) {
				double dx = (x - MARKET_X) / 11.0, dz = (z - MARKET_Z) / 9.0, e = Math.sqrt(dx * dx + dz * dz);
				if (e > 1.0) {
					continue;
				}
				int ring = (int) Math.round(Math.hypot(x - MARKET_X, z - MARKET_Z));
				BlockState b = e > 0.9 ? s(Blocks.CUT_SANDSTONE) : ring == 5 ? s(Blocks.CUT_SANDSTONE)
						: (ring == 4 && (x == MARKET_X || z == MARKET_Z)) ? s(Blocks.CHISELED_SANDSTONE) : s(Blocks.SMOOTH_SANDSTONE);
				c.set(x, G, z, b);
				c.fill(x, G + 1, z, x, G + 4, z, s(Blocks.AIR));
			}
		}
		well(c, MARKET_X, MARKET_Z);
		// Stalls: at the six edge points, counters facing the well.
		Block[][] goods = {{Blocks.MELON, Blocks.PUMPKIN}, {Blocks.HAY_BLOCK, Blocks.BARREL}, {Blocks.DECORATED_POT, Blocks.DECORATED_POT},
				{Blocks.ORANGE_WOOL, Blocks.RED_WOOL}, {Blocks.BOOKSHELF, Blocks.BARREL}, {Blocks.CACTUS, Blocks.POTTED_DEAD_BUSH}};
		Block[] awn = {Blocks.ORANGE_WOOL, Blocks.RED_WOOL, Blocks.CYAN_WOOL, Blocks.YELLOW_WOOL, Blocks.LIME_WOOL, Blocks.MAGENTA_WOOL};
		int[][] spots = {{-8, -3}, {0, -7}, {8, -3}, {8, 4}, {-8, 4}, {-5, 7}};
		for (int i = 0; i < spots.length; i++) {
			int sx = MARKET_X + spots[i][0], sz = MARKET_Z + spots[i][1];
			Direction facing = Math.abs(spots[i][0]) >= Math.abs(spots[i][1]) ? (spots[i][0] > 0 ? Direction.WEST : Direction.EAST)
					: (spots[i][1] > 0 ? Direction.NORTH : Direction.SOUTH);
			stall(c, sx, sz, facing, awn[i], goods[i]);
		}
		// Benches facing the well, and pottery.
		for (int[] b : new int[][]{{-3, 4, 2}, {3, 4, 2}, {-3, -4, 0}, {3, -4, 0}}) {
			Direction face = b[2] == 0 ? Direction.SOUTH : Direction.NORTH;
			c.set(MARKET_X + b[0], G + 1, MARKET_Z + b[1], stair(Blocks.SMOOTH_SANDSTONE_STAIRS, face.getOpposite(), false));
		}
		c.set(MARKET_X + 10, G + 1, MARKET_Z - 1, s(Blocks.DECORATED_POT));
		c.set(MARKET_X - 10, G + 1, MARKET_Z + 1, s(Blocks.DECORATED_POT));
		c.set(MARKET_X + 4, G + 1, MARKET_Z + 8, s(Blocks.DECORATED_POT));
		c.set(MARKET_X + 5, G + 1, MARKET_Z + 8, s(Blocks.POTTED_CACTUS));
		cart(c, MARKET_X + 4, MARKET_Z - 7);
	}

	/** The market well: a sandstone ring with water, four posts, a stepped roof and a hanging lantern. */
	private static void well(Canvas c, int x, int z) {
		c.fill(x - 2, G + 1, z - 2, x + 2, G + 1, z + 2, s(Blocks.CUT_SANDSTONE));
		c.fill(x - 1, G - 3, z - 1, x + 1, G + 1, z + 1, s(Blocks.WATER));
		for (int dx : new int[]{-2, 2}) {
			for (int dz : new int[]{-2, 2}) {
				c.fill(x + dx, G + 2, z + dz, x + dx, G + 4, z + dz, s(Blocks.SANDSTONE_WALL));
			}
		}
		c.fill(x - 2, G + 5, z - 2, x + 2, G + 5, z + 2, Kit.slab(Blocks.CUT_SANDSTONE_SLAB, false));
		c.fill(x - 1, G + 5, z - 1, x + 1, G + 5, z + 1, s(Blocks.CUT_SANDSTONE));
		c.set(x, G + 6, z, s(Blocks.CHISELED_SANDSTONE));
		c.set(x, G + 4, z, lantern(true));
	}

	/** A market stall: a 3-wide counter, corner posts, a striped awning, and goods. {@code facing} is toward the buyer. */
	private static void stall(Canvas c, int x, int z, Direction facing, Block awning, Block[] goods) {
		Direction side = facing.getClockWise();
		int sx = side.getStepX(), sz = side.getStepZ(), fx = facing.getStepX(), fz = facing.getStepZ();
		for (int k = -1; k <= 1; k++) {
			int cx = x + sx * k, cz = z + sz * k;
			c.set(cx, G + 1, cz, Kit.slab(Blocks.JUNGLE_SLAB, true));
			c.set(cx - fx * 2, G + 1, cz - fz * 2, k == 0 ? Canvas.facing(s(Blocks.BARREL), Direction.UP) : s(goods[(k + 1) / 2]));
			for (int depth = -2; depth <= 1; depth++) {
				int ax = cx + fx * depth, az = cz + fz * depth;
				boolean stripe = Math.floorMod(k + depth, 2) == 0;
				c.set(ax, G + 4, az, s(stripe ? awning : Blocks.WHITE_WOOL));
			}
		}
		c.set(x + sx * -1 + fx, G + 2, z + sz * -1 + fz, goods[0] == Blocks.CACTUS ? s(Blocks.POTTED_CACTUS) : s(Blocks.FLOWER_POT));
		for (int k : new int[]{-1, 1}) {
			for (int depth : new int[]{-2, 1}) {
				int px = x + sx * k + fx * depth, pz = z + sz * k + fz * depth;
				if (depth == 1) {
					c.fill(px, G + 1, pz, px, G + 3, pz, s(Blocks.JUNGLE_FENCE));
				} else {
					c.fill(px, G + 2, pz, px, G + 3, pz, s(Blocks.JUNGLE_FENCE));
				}
			}
		}
	}

	/** A hand cart: a plank bed on two wheels (trapdoors), with a barrel and a pot, shafts resting on the ground. */
	private static void cart(Canvas c, int x, int z) {
		c.set(x, G + 1, z, Kit.slab(Blocks.SPRUCE_SLAB, true));
		c.set(x + 1, G + 1, z, Kit.slab(Blocks.SPRUCE_SLAB, true));
		c.set(x, G + 2, z, Canvas.facing(s(Blocks.BARREL), Direction.UP));
		c.set(x + 1, G + 2, z, s(Blocks.DECORATED_POT));
		for (int dz : new int[]{-1, 1}) {
			c.set(x, G + 1, z + dz, s(Blocks.SPRUCE_TRAPDOOR).setValue(TrapDoorBlock.FACING, dz < 0 ? Direction.NORTH : Direction.SOUTH)
					.setValue(TrapDoorBlock.OPEN, true));
		}
		c.set(x + 2, G + 1, z, Kit.slab(Blocks.SPRUCE_SLAB, false));
	}

	// ---------------------------------------------------------------------------------------------------- buildings

	/** Building-local coordinates: u across the front (0..w-1), v back (0) to front (d-1), dy above the floor. */
	static final class Frame {
		final Canvas c;
		final int ox, oz, y;
		final Rotation rot;
		final int cu, cv;

		Frame(Canvas c, Bldg b, int y) {
			this.c = c;
			this.ox = b.cx();
			this.oz = b.cz();
			this.y = y;
			this.rot = switch (b.front()) {
				case WEST -> Rotation.CLOCKWISE_90;
				case NORTH -> Rotation.CLOCKWISE_180;
				case EAST -> Rotation.COUNTERCLOCKWISE_90;
				default -> Rotation.NONE;
			};
			this.cu = (b.w() - 1) / 2;
			this.cv = (b.d() - 1) / 2;
		}

		int x(int u, int v) {
			int a = u - cu, b = v - cv;
			return ox + switch (rot) {
				case CLOCKWISE_90 -> -b;
				case CLOCKWISE_180 -> -a;
				case COUNTERCLOCKWISE_90 -> b;
				default -> a;
			};
		}

		int z(int u, int v) {
			int a = u - cu, b = v - cv;
			return oz + switch (rot) {
				case CLOCKWISE_90 -> a;
				case CLOCKWISE_180 -> -b;
				case COUNTERCLOCKWISE_90 -> -a;
				default -> b;
			};
		}

		void set(int u, int dy, int v, BlockState state) {
			c.set(x(u, v), y + dy, z(u, v), state.rotate(rot));
		}

		void fill(int u1, int dy1, int v1, int u2, int dy2, int v2, BlockState state) {
			for (int u = Math.min(u1, u2); u <= Math.max(u1, u2); u++) {
				for (int v = Math.min(v1, v2); v <= Math.max(v1, v2); v++) {
					for (int dy = Math.min(dy1, dy2); dy <= Math.max(dy1, dy2); dy++) {
						set(u, dy, v, state);
					}
				}
			}
		}
	}

	/**
	 * A flat-roofed sandstone building on a one-block plinth: pilaster corners, a base course, framed windows with
	 * jungle shutters, an open door under a striped awning porch, a roof deck with a crenellated parapet, and a
	 * furnished interior. Two-storey buildings have an inside stair and a front balcony; single-storey ones have an
	 * outside stair up to a roof terrace.
	 */
	private void building(Canvas c, Bldg b) {
		int floorY = G + 1;
		Frame f = new Frame(c, b, floorY);
		int w = b.w(), d = b.d();
		int[] box = footprint(b);
		c.foundation(box[0], box[1], box[2], box[3], floorY, s(Blocks.SMOOTH_SANDSTONE));
		if (b.use() == Use.SHELTER) {
			shelter(f, b);
			return;
		}
		int top = 4 * b.floors();
		f.fill(1, 0, 1, w - 2, 0, d - 2, s(b.use() == Use.TRADE ? Blocks.CUT_SANDSTONE : Blocks.SMOOTH_SANDSTONE));
		for (int fl = 0; fl < b.floors(); fl++) {
			int base = fl * 4;
			for (int u = 0; u < w; u++) {
				for (int v = 0; v < d; v++) {
					boolean eu = u == 0 || u == w - 1, ev = v == 0 || v == d - 1;
					if (!eu && !ev) {
						f.fill(u, base + 1, v, u, base + 3, v, s(Blocks.AIR));
						continue;
					}
					if (eu && ev) {
						f.fill(u, base + 1, v, u, base + 4, v, s(b.trim()));
						continue;
					}
					f.set(u, base + 1, v, fl == 0 ? s(Blocks.CUT_SANDSTONE) : s(b.wall()));
					f.set(u, base + 2, v, s(b.wall()));
					f.set(u, base + 3, v, s(b.wall()));
					f.set(u, base + 4, v, s(Blocks.CUT_SANDSTONE));
				}
			}
			f.fill(1, base + 4, 1, w - 2, base + 4, d - 2, s(Blocks.SMOOTH_SANDSTONE)); // ceiling / next floor / roof deck
			windows(f, b, base);
			f.set((w - 1) / 2, base + 3, (d - 1) / 2, lantern(true));
		}
		// Projecting beam ends (vigas) under the roof line, front and back.
		for (int u = 1; u < w - 1; u += 2) {
			f.set(u, top - 1, d, s(Blocks.STRIPPED_JUNGLE_LOG).rotate(Rotation.NONE));
			f.set(u, top - 1, -1, s(Blocks.STRIPPED_JUNGLE_LOG));
		}
		parapet(f, b, top);
		// Door, step and porch.
		int du = (w - 1) / 2;
		f.set(du, 1, d - 1, openDoor(Blocks.JUNGLE_DOOR, Direction.NORTH, DoubleBlockHalf.LOWER));
		f.set(du, 2, d - 1, openDoor(Blocks.JUNGLE_DOOR, Direction.NORTH, DoubleBlockHalf.UPPER));
		f.set(du, 3, d - 1, s(Blocks.CHISELED_SANDSTONE));
		f.set(du, 0, d, stair(Blocks.SMOOTH_SANDSTONE_STAIRS, Direction.NORTH, false));
		porch(f, b);
		if (b.floors() > 1) {
			insideStair(f, b);
			balcony(f, b);
		} else if (b.stairsSide() != 0 && d >= 6) {
			outsideStair(f, b);
		}
		furnish(f, b);
	}

	/** Windows: one or two panes high with open jungle shutters; side walls get one each. */
	private static void windows(Frame f, Bldg b, int base) {
		int w = b.w(), d = b.d(), du = (w - 1) / 2;
		BlockState pane = Kit.connected(Blocks.GLASS_PANE, true);
		for (int v : new int[]{0, d - 1}) {
			for (int u = 2; u <= w - 3; u += 3) {
				if (Math.abs(u - du) <= 1 && v == d - 1) {
					continue;
				}
				f.set(u, base + 2, v, pane);
				int out = v == 0 ? -1 : d;
				Direction outward = v == 0 ? Direction.NORTH : Direction.SOUTH;
				for (int side : new int[]{-1, 1}) {
					int su = u + side;
					if (su > 0 && su < w - 1 && Math.abs(su - du) > (v == d - 1 ? 1 : -1)) {
						f.set(su, base + 2, out, s(Blocks.JUNGLE_TRAPDOOR).setValue(TrapDoorBlock.FACING, outward)
								.setValue(TrapDoorBlock.OPEN, true).setValue(TrapDoorBlock.HALF, Half.BOTTOM));
					}
				}
				f.set(u, base + 3, out, Kit.slab(Blocks.SMOOTH_SANDSTONE_SLAB, true)); // a small lintel hood
			}
		}
		BlockState sidePane = Kit.connected(Blocks.GLASS_PANE, false);
		for (int v = 2; v <= d - 3; v += Math.max(3, d - 4)) {
			f.set(0, base + 2, v, sidePane);
			f.set(w - 1, base + 2, v, sidePane);
		}
	}

	/** A crenellated parapet round the roof deck: merlons at the corners and every other block, slabs between. */
	private static void parapet(Frame f, Bldg b, int top) {
		int w = b.w(), d = b.d();
		for (int u = 0; u < w; u++) {
			for (int v = 0; v < d; v++) {
				boolean eu = u == 0 || u == w - 1, ev = v == 0 || v == d - 1;
				if (!eu && !ev) {
					continue;
				}
				if (eu && ev) {
					f.set(u, top + 1, v, s(Blocks.CHISELED_SANDSTONE));
					f.set(u, top + 2, v, Kit.slab(Blocks.SANDSTONE_SLAB, false));
				} else {
					int along = eu ? v : u;
					f.set(u, top + 1, v, along % 2 == 0 ? s(b.trim()) : Kit.slab(Blocks.SMOOTH_SANDSTONE_SLAB, false));
				}
			}
		}
	}

	/** A striped awning over the door on two jungle posts, reaching two blocks out. */
	private static void porch(Frame f, Bldg b) {
		int w = b.w(), d = b.d(), du = (w - 1) / 2;
		for (int u = du - 2; u <= du + 2; u++) {
			for (int v = d; v <= d + 1; v++) {
				f.set(u, 4, v, s(Math.floorMod(u - du, 2) == 0 ? b.awning() : Blocks.WHITE_WOOL));
			}
		}
		for (int u : new int[]{du - 2, du + 2}) {
			f.fill(u, 1, d + 1, u, 3, d + 1, s(Blocks.JUNGLE_FENCE));
		}
		f.set(du - 1, 1, d, s(Blocks.POTTED_CACTUS));
		f.set(du + 1, 3, d - 1 + 1, lantern(true));
	}

	/** Inside stair along the back wall, rising toward u = 1, with the ceiling opened above it and a rail upstairs. */
	private static void insideStair(Frame f, Bldg b) {
		int w = b.w();
		for (int i = 1; i <= 4; i++) {
			int u = w - 2 - (i - 1);
			f.set(u, i, 1, stair(Blocks.SMOOTH_SANDSTONE_STAIRS, Direction.WEST, false));
			for (int dy = i + 1; dy <= 4; dy++) {
				f.set(u, dy, 1, s(Blocks.AIR));
			}
			f.fill(u, 1, 1, u, i - 1, 1, s(Blocks.SMOOTH_SANDSTONE));
		}
		for (int u = w - 5; u <= w - 2; u++) {
			f.set(u, 5, 2, Kit.connected(Blocks.JUNGLE_FENCE, true));
		}
	}

	/** A front balcony on the upper floor: a slab deck on corbels, fence railing, an open door onto it. */
	private static void balcony(Frame f, Bldg b) {
		int w = b.w(), d = b.d(), du = (w - 1) / 2;
		for (int u = du - 1; u <= du + 1; u++) {
			f.set(u, 4, d, s(Blocks.SMOOTH_SANDSTONE));
			f.set(u, 3, d, stair(Blocks.SANDSTONE_STAIRS, Direction.NORTH, true));
			f.set(u, 5, d + 1, Kit.connected(Blocks.JUNGLE_FENCE, true));
			f.fill(u, 5, d, u, 7, d, s(Blocks.AIR));
		}
		f.set(du - 2, 5, d, Kit.connected(Blocks.JUNGLE_FENCE, false));
		f.set(du + 2, 5, d, Kit.connected(Blocks.JUNGLE_FENCE, false));
		f.set(du, 5, d - 1, openDoor(Blocks.JUNGLE_DOOR, Direction.NORTH, DoubleBlockHalf.LOWER));
		f.set(du, 6, d - 1, openDoor(Blocks.JUNGLE_DOOR, Direction.NORTH, DoubleBlockHalf.UPPER));
		f.set(du + 1, 5, d, s(Blocks.POTTED_FERN));
	}

	/** An outside stair up the side wall to the roof terrace, with an opening in the parapet at the top. */
	private static void outsideStair(Frame f, Bldg b) {
		int w = b.w(), d = b.d();
		int u = b.stairsSide() > 0 ? w : -1;
		for (int i = 1; i <= 4; i++) {
			int v = d - i;
			f.set(u, i, v, stair(Blocks.SMOOTH_SANDSTONE_STAIRS, Direction.NORTH, false));
			f.fill(u, 0, v, u, i - 1, v, s(Blocks.SMOOTH_SANDSTONE));
			f.fill(u, i + 1, v, u, i + 3, v, s(Blocks.AIR));
		}
		f.set(u, 4, d - 5, s(Blocks.SMOOTH_SANDSTONE));
		f.fill(u, 0, d - 5, u, 3, d - 5, s(Blocks.SMOOTH_SANDSTONE));
		int wallU = b.stairsSide() > 0 ? w - 1 : 0;
		f.set(wallU, 5, d - 5, s(Blocks.AIR)); // gap in the parapet onto the roof
		f.set(u, 5, d - 4, Kit.connected(Blocks.JUNGLE_FENCE, false));
		// Roof terrace furniture: a shade cloth on posts and seats.
		int du = (w - 1) / 2;
		f.fill(du - 1, 5, 1, du - 1, 6, 1, s(Blocks.JUNGLE_FENCE));
		f.fill(du + 1, 5, 1, du + 1, 6, 1, s(Blocks.JUNGLE_FENCE));
		f.fill(du - 1, 7, 1, du + 1, 7, 2, s(b.awning()));
		f.set(du, 5, 2, stair(Blocks.SANDSTONE_STAIRS, Direction.NORTH, false));
		f.set(du + 2, 5, 3, s(Blocks.DECORATED_POT));
	}

	/** The dockside shelter: three walls, an open front facing the plaza, an awning, benches, barrels and a lantern. */
	private static void shelter(Frame f, Bldg b) {
		int w = b.w(), d = b.d();
		f.fill(0, 0, 0, w - 1, 0, d - 1, s(Blocks.SMOOTH_SANDSTONE));
		for (int u = 0; u < w; u++) {
			for (int v = 0; v < d; v++) {
				boolean eu = u == 0 || u == w - 1, back = v == 0;
				boolean frontPost = v == d - 1 && eu;
				if (eu || back) {
					f.fill(u, 1, v, u, 3, v, s(eu && (back || frontPost) ? Blocks.CUT_SANDSTONE : b.wall()));
				} else {
					f.fill(u, 1, v, u, 3, v, s(Blocks.AIR));
				}
				f.set(u, 4, v, Kit.slab(Blocks.SMOOTH_SANDSTONE_SLAB, false));
			}
		}
		for (int u = 0; u < w; u++) {
			f.set(u, 4, d, s(Math.floorMod(u, 2) == 0 ? b.awning() : Blocks.WHITE_WOOL));
		}
		f.set(1, 2, 0, s(Blocks.AIR));
		f.set(w - 2, 2, 0, s(Blocks.AIR));
		for (int u = 1; u <= w - 2; u++) {
			f.set(u, 1, 1, stair(Blocks.SMOOTH_SANDSTONE_STAIRS, Direction.NORTH, false));
		}
		f.set(1, 1, d - 2, Canvas.facing(s(Blocks.BARREL), Direction.UP));
		f.set(1, 2, d - 2, s(Blocks.POTTED_CACTUS));
		f.set(w - 2, 1, d - 2, s(Blocks.DECORATED_POT));
		f.set((w - 1) / 2, 3, 2, lantern(true));
	}

	// ---------------------------------------------------------------------------------------------------- interiors

	private static void bed(Frame f, int u, int dy, int v, Block bed) {
		f.set(u, dy, v, s(bed).setValue(BedBlock.FACING, Direction.NORTH).setValue(BedBlock.PART, BedPart.HEAD));
		f.set(u, dy, v + 1, s(bed).setValue(BedBlock.FACING, Direction.NORTH).setValue(BedBlock.PART, BedPart.FOOT));
	}

	private static void table(Frame f, int u, int dy, int v) {
		f.set(u, dy, v, s(Blocks.JUNGLE_FENCE));
		f.set(u, dy + 1, v, s(Blocks.JUNGLE_PRESSURE_PLATE));
	}

	private static void rug(Frame f, int u1, int v1, int u2, int v2, int dy, Block a, Block b) {
		for (int u = u1; u <= u2; u++) {
			for (int v = v1; v <= v2; v++) {
				boolean edge = u == u1 || u == u2 || v == v1 || v == v2;
				f.set(u, dy, v, s(edge ? a : b));
			}
		}
	}

	/** Furniture by use; every room keeps a clear walkway from the door. */
	private static void furnish(Frame f, Bldg b) {
		int w = b.w(), d = b.d(), du = (w - 1) / 2;
		switch (b.use()) {
			case INN -> {
				// Ground floor common room: a bar along the back, tables with stools, a rug, barrels.
				for (int u = 2; u <= w - 6; u++) {
					f.set(u, 1, 2, Kit.slab(Blocks.CUT_SANDSTONE_SLAB, true));
				}
				f.set(1, 1, 1, Canvas.facing(s(Blocks.BARREL), Direction.SOUTH));
				f.set(1, 2, 1, Canvas.facing(s(Blocks.BARREL), Direction.SOUTH));
				f.set(1, 1, 2, Canvas.facing(s(Blocks.BARREL), Direction.EAST));
				f.set(2, 2, 2, candles(3, true));
				for (int[] t : new int[][]{{2, 5}, {w - 3, 5}}) {
					table(f, t[0], 1, t[1]);
					f.set(t[0] - 1, 1, t[1], stair(Blocks.JUNGLE_STAIRS, Direction.WEST, false));
					f.set(t[0] + 1, 1, t[1], stair(Blocks.JUNGLE_STAIRS, Direction.EAST, false));
				}
				rug(f, du - 1, 4, du + 1, 6, 1, Blocks.RED_CARPET, Blocks.ORANGE_CARPET);
				// Upper floor: three sleeping rooms behind partitions, a corridor along the front.
				for (int u : new int[]{3, 6}) {
					f.fill(u, 5, 1, u, 7, d - 4, s(Blocks.SMOOTH_SANDSTONE));
					f.set(u, 5, d - 4, s(Blocks.AIR));
					f.set(u, 6, d - 4, s(Blocks.AIR));
				}
				bed(f, 1, 5, d - 6, Blocks.RED_BED);
				bed(f, 4, 5, d - 6, Blocks.ORANGE_BED);
				bed(f, 7, 5, d - 6, Blocks.YELLOW_BED);
				f.set(2, 5, d - 6, Canvas.facing(s(Blocks.BARREL), Direction.UP));
				f.set(5, 5, d - 6, s(Blocks.DECORATED_POT));
				f.set(8, 5, d - 6, s(Blocks.FLOWER_POT));
				// Ladder up to the rooftop terrace.
				for (int dy = 5; dy <= 8; dy++) {
					f.set(w - 2, dy, d - 2, s(Blocks.LADDER).setValue(LadderBlock.FACING, Direction.NORTH));
				}
				f.set(w - 2, 8, d - 2, s(Blocks.LADDER).setValue(LadderBlock.FACING, Direction.NORTH));
				// Rooftop terrace: a shade cloth, a table and seats, potted plants.
				f.fill(2, 9, 2, 2, 10, 2, s(Blocks.JUNGLE_FENCE));
				f.fill(2, 9, 5, 2, 10, 5, s(Blocks.JUNGLE_FENCE));
				f.fill(5, 9, 2, 5, 10, 2, s(Blocks.JUNGLE_FENCE));
				f.fill(5, 9, 5, 5, 10, 5, s(Blocks.JUNGLE_FENCE));
				for (int u = 2; u <= 5; u++) {
					for (int v = 2; v <= 5; v++) {
						f.set(u, 11, v, s(Math.floorMod(u + v, 2) == 0 ? Blocks.RED_WOOL : Blocks.WHITE_WOOL));
					}
				}
				table(f, 3, 9, 3);
				f.set(4, 9, 3, stair(Blocks.SANDSTONE_STAIRS, Direction.EAST, false));
				f.set(3, 9, 4, stair(Blocks.SANDSTONE_STAIRS, Direction.SOUTH, false));
				f.set(7, 9, 2, s(Blocks.POTTED_CACTUS));
				f.set(1, 9, d - 2, s(Blocks.DECORATED_POT));
			}
			case TRADE -> {
				// A counter across the room, shelves of stock behind it, crates and a strongbox.
				for (int u = 2; u <= w - 3; u++) {
					if (u != du) {
						f.set(u, 1, 4, Kit.slab(Blocks.CUT_SANDSTONE_SLAB, true));
					}
				}
				f.set(du, 1, 4, s(Blocks.JUNGLE_TRAPDOOR).setValue(TrapDoorBlock.FACING, Direction.SOUTH).setValue(TrapDoorBlock.OPEN, true));
				f.set(3, 2, 4, s(Blocks.FLOWER_POT));
				f.set(w - 4, 2, 4, candles(2, true));
				for (int u = 1; u <= w - 2; u++) {
					f.set(u, 1, 1, u % 3 == 0 ? s(Blocks.BOOKSHELF) : Canvas.facing(s(Blocks.BARREL), Direction.SOUTH));
					f.set(u, 2, 1, u % 3 == 1 ? s(Blocks.CHISELED_BOOKSHELF) : s(Blocks.BOOKSHELF));
				}
				f.set(1, 1, 2, s(Blocks.CHEST));
				f.set(w - 2, 1, 2, s(Blocks.LOOM));
				f.set(w - 2, 1, 3, s(Blocks.CARTOGRAPHY_TABLE));
				rug(f, du - 2, 5, du + 2, d - 2, 1, Blocks.CYAN_CARPET, Blocks.LIGHT_BLUE_CARPET);
				f.set(1, 1, d - 2, s(Blocks.DECORATED_POT));
				f.set(w - 2, 1, d - 2, s(Blocks.DECORATED_POT));
				// A back door into the courtyard.
				f.set(du, 1, 0, openDoor(Blocks.JUNGLE_DOOR, Direction.SOUTH, DoubleBlockHalf.LOWER));
				f.set(du, 2, 0, openDoor(Blocks.JUNGLE_DOOR, Direction.SOUTH, DoubleBlockHalf.UPPER));
				f.set(du - 1, 1, 1, s(Blocks.AIR));
				f.set(du + 1, 1, 1, s(Blocks.AIR));
			}
			case HOME_ELDER -> {
				bed(f, w - 2, 5, 1, Blocks.PURPLE_BED);
				f.set(w - 3, 5, 1, s(Blocks.BOOKSHELF));
				f.set(1, 5, 1, s(Blocks.LECTERN));
				rug(f, 2, 3, 4, 5, 5, Blocks.PURPLE_CARPET, Blocks.MAGENTA_CARPET);
				table(f, 2, 1, 3);
				f.set(1, 1, 3, stair(Blocks.JUNGLE_STAIRS, Direction.WEST, false));
				f.set(1, 1, d - 2, s(Blocks.DECORATED_POT));
				f.set(w - 2, 1, d - 2, Canvas.facing(s(Blocks.BARREL), Direction.UP));
			}
			case HOME_POTTER -> {
				f.set(1, 1, 1, s(Blocks.FURNACE));
				f.set(2, 1, 1, s(Blocks.DECORATED_POT));
				f.set(3, 1, 1, s(Blocks.DECORATED_POT));
				f.set(1, 1, 2, s(Blocks.CLAY));
				bed(f, w - 2, 1, 1, Blocks.LIME_BED);
				rug(f, 2, 3, 4, 4, 1, Blocks.GREEN_CARPET, Blocks.LIME_CARPET);
				f.set(1, 1, d - 2, s(Blocks.FLOWER_POT));
			}
			case HOME_FAMILY -> {
				bed(f, 1, 1, 1, Blocks.YELLOW_BED);
				bed(f, 2, 1, 1, Blocks.YELLOW_BED);
				f.set(w - 2, 1, 1, s(Blocks.CRAFTING_TABLE));
				f.set(w - 3, 1, 1, s(Blocks.SMOKER));
				table(f, 4, 1, 4);
				f.set(3, 1, 4, stair(Blocks.JUNGLE_STAIRS, Direction.WEST, false));
				f.set(5, 1, 4, stair(Blocks.JUNGLE_STAIRS, Direction.EAST, false));
				f.set(w - 2, 1, d - 2, Canvas.facing(s(Blocks.BARREL), Direction.UP));
			}
			case HOME_WEAVER -> {
				f.set(1, 1, 1, s(Blocks.LOOM));
				f.set(2, 1, 1, s(Blocks.MAGENTA_WOOL));
				f.set(2, 2, 1, s(Blocks.PINK_WOOL));
				f.set(3, 1, 1, s(Blocks.ORANGE_WOOL));
				bed(f, w - 2, 1, 1, Blocks.MAGENTA_BED);
				rug(f, 2, 3, 4, 4, 1, Blocks.MAGENTA_CARPET, Blocks.PINK_CARPET);
				f.set(1, 1, d - 2, s(Blocks.DECORATED_POT));
			}
			case HOME_SCHOLAR -> {
				for (int u = 1; u <= w - 6; u++) {
					f.set(u, 1, 2, s(Blocks.BOOKSHELF));
					f.set(u, 2, 2, s(Blocks.BOOKSHELF));
				}
				f.set(2, 1, d - 3, s(Blocks.LECTERN));
				f.set(w - 2, 1, d - 2, s(Blocks.CARTOGRAPHY_TABLE));
				f.set(w - 2, 1, d - 3, candles(4, true));
				bed(f, 1, 5, 3, Blocks.BLUE_BED);
				f.set(2, 5, 3, s(Blocks.BOOKSHELF));
				rug(f, 2, 5, 4, d - 2, 5, Blocks.BLUE_CARPET, Blocks.LIGHT_BLUE_CARPET);
			}
			case HOME_FISHER -> {
				bed(f, 1, 1, 1, Blocks.LIGHT_BLUE_BED);
				f.set(w - 2, 1, 1, Canvas.facing(s(Blocks.BARREL), Direction.UP));
				f.set(w - 2, 2, 1, Canvas.facing(s(Blocks.BARREL), Direction.UP));
				f.set(w - 3, 1, 1, s(Blocks.SMOKER));
				f.set(w - 2, 1, 2, s(Blocks.COMPOSTER));
				rug(f, 2, 3, 4, 4, 1, Blocks.LIGHT_BLUE_CARPET, Blocks.WHITE_CARPET);
			}
			default -> {
			}
		}
	}

	/** The trading house's walled courtyard behind it: a palm, a bench, crates and a water trough. */
	private void courtyard(Canvas c) {
		Bldg t = BUILDINGS.get(1);
		if (!c.near(t.cx() + 9, t.cz(), 10)) {
			return;
		}
		int x1 = t.cx() + 5, x2 = t.cx() + 11, z1 = t.cz() - 6, z2 = t.cz() + 6;
		for (int x = x1; x <= x2; x++) {
			for (int z = z1; z <= z2; z++) {
				boolean edge = x == x2 || z == z1 || z == z2;
				c.set(x, G, z, unit(x, z, 51) < 0.3 ? s(Blocks.SANDSTONE) : s(Blocks.SMOOTH_SANDSTONE));
				c.fill(x, G + 1, z, x, G + 3, z, s(Blocks.AIR));
				if (edge) {
					boolean gate = x == x2 && Math.abs(z - t.cz()) <= 1;
					if (!gate) {
						c.fill(x, G + 1, z, x, G + 2, z, s(Blocks.SANDSTONE));
						c.set(x, G + 3, z, (x + z) % 2 == 0 ? s(Blocks.CUT_SANDSTONE) : Kit.slab(Blocks.SANDSTONE_SLAB, false));
					}
				}
			}
		}
		// Contents.
		c.set(x2 - 1, G + 1, z1 + 1, Canvas.facing(s(Blocks.BARREL), Direction.UP));
		c.set(x2 - 2, G + 1, z1 + 1, Canvas.facing(s(Blocks.BARREL), Direction.UP));
		c.set(x2 - 1, G + 2, z1 + 1, Canvas.facing(s(Blocks.BARREL), Direction.UP));
		c.set(x2 - 1, G + 1, z1 + 2, s(Blocks.HAY_BLOCK));
		c.fill(x1 + 1, G + 1, z2 - 1, x1 + 3, G + 1, z2 - 1, s(Blocks.CUT_SANDSTONE));
		c.fill(x1 + 1, G + 1, z2 - 2, x1 + 3, G + 1, z2 - 2, s(Blocks.WATER));
		c.fill(x1 + 1, G, z2 - 2, x1 + 3, G, z2 - 2, s(Blocks.CUT_SANDSTONE));
		c.set(x1 + 1, G + 1, z2 - 3, s(Blocks.CUT_SANDSTONE));
		c.set(x1 + 2, G + 1, z2 - 3, s(Blocks.CUT_SANDSTONE));
		c.set(x1 + 3, G + 1, z2 - 3, s(Blocks.CUT_SANDSTONE));
		c.set(x1, G + 1, z2 - 2, s(Blocks.CUT_SANDSTONE));
		c.set(x1 + 4, G + 1, z2 - 2, s(Blocks.CUT_SANDSTONE));
		c.set(x2 - 3, G + 1, z2 - 1, stair(Blocks.SMOOTH_SANDSTONE_STAIRS, Direction.SOUTH, false));
		c.set(x2 - 2, G + 1, z2 - 1, stair(Blocks.SMOOTH_SANDSTONE_STAIRS, Direction.SOUTH, false));
		if (c.near(x1 + 3, t.cz(), 6)) {
			palm(c, x1 + 3, t.cz(), 6, 1);
		}
	}

	/** The quest giver's post at the east gate: two gate pillars with lanterns and a shade awning on posts. */
	private static void questPost(Canvas c) {
		if (!c.near(QUEST_X, QUEST_Z, 8)) {
			return;
		}
		for (int dz : new int[]{-3, 3}) {
			int x = QUEST_X - 3, z = QUEST_Z + dz;
			c.fill(x, G + 1, z, x, G + 4, z, s(Blocks.CUT_SANDSTONE));
			c.set(x, G + 5, z, s(Blocks.CHISELED_SANDSTONE));
			c.set(x, G + 6, z, lantern(false));
		}
		for (int dz = -1; dz <= 1; dz++) {
			for (int dx = 1; dx <= 2; dx++) {
				c.set(QUEST_X + dx, G + 4, QUEST_Z + dz, s(Math.floorMod(dz, 2) == 0 ? Blocks.ORANGE_WOOL : Blocks.WHITE_WOOL));
			}
		}
		c.fill(QUEST_X + 2, G + 1, QUEST_Z - 1, QUEST_X + 2, G + 3, QUEST_Z - 1, s(Blocks.JUNGLE_FENCE));
		c.fill(QUEST_X + 2, G + 1, QUEST_Z + 1, QUEST_X + 2, G + 3, QUEST_Z + 1, s(Blocks.JUNGLE_FENCE));
		c.set(QUEST_X + 2, G + 1, QUEST_Z, Canvas.facing(s(Blocks.BARREL), Direction.UP));
		c.set(QUEST_X + 2, G + 2, QUEST_Z, s(Blocks.LECTERN));
	}

	// ---------------------------------------------------------------------------------------------------- pyramids

	/** Half-width of a pyramid layer: steady slope from {@code half} at the base to 1 at the top layer. */
	static int layerHalf(int half, int height, int layer) {
		return half - (int) Math.round(layer * (half - 1) / (double) (height - 1));
	}

	/**
	 * Lays a pyramid: solid layers, stair-faced where the next layer steps in (so the faces read as slopes), banded with
	 * cut sandstone, with a glyph frieze of chiseled sandstone and terracotta. {@code weathered} drops blocks from the
	 * upper edges and drifts sand onto the lower steps.
	 */
	private static void pyramidShell(Canvas c, int cx, int cz, int half, int height, int base, boolean weathered) {
		for (int layer = 0; layer < height; layer++) {
			int h = layerHalf(half, height, layer);
			int next = layer + 1 < height ? layerHalf(half, height, layer + 1) : 0;
			int y = base + 1 + layer;
			for (int dx = -h; dx <= h; dx++) {
				for (int dz = -h; dz <= h; dz++) {
					int x = cx + dx, z = cz + dz;
					if (!c.inChunk(x, z)) {
						continue;
					}
					boolean outer = Math.abs(dx) > next || Math.abs(dz) > next;
					BlockState b;
					if (!outer) {
						b = s(Blocks.SANDSTONE);
					} else {
						boolean band = layer % 5 == 3;
						boolean frieze = layer == 6 && (dx + dz) % 3 == 0;
						b = frieze ? s(((dx + dz) / 3) % 2 == 0 ? Blocks.CHISELED_SANDSTONE : Blocks.ORANGE_TERRACOTTA)
								: band ? s(Blocks.CUT_SANDSTONE) : s(Blocks.SANDSTONE);
						// The step face: a stair facing inward on the outermost ring when the next layer steps in.
						if (h > next && (Math.abs(dx) == h || Math.abs(dz) == h) && !band && !frieze) {
							Direction in = Math.abs(dx) >= Math.abs(dz) ? (dx > 0 ? Direction.WEST : Direction.EAST)
									: (dz > 0 ? Direction.NORTH : Direction.SOUTH);
							b = stair(Blocks.SANDSTONE_STAIRS, in, false);
						}
						if (weathered && layer >= height / 3 && unit(x, z, 61 + layer) < 0.16) {
							continue; // a missing stone
						}
					}
					c.set(x, y, z, b);
				}
			}
			if (weathered && layer <= 3) {
				// Sand drifted onto the lowest steps on the windward (west) side.
				for (int dz = -h; dz <= h; dz++) {
					int x = cx - h - 1, z = cz + dz;
					if (c.inChunk(x, z) && unit(x, z, 71 + layer) < 0.5) {
						c.set(x, y, z, s(Blocks.SAND));
					}
				}
			}
		}
	}

	/**
	 * The Great Pyramid (33 wide, 22 layers, a gold capstone): a pillared gateway on the south face, a corridor lit by
	 * lanterns, and a pillared altar chamber with a glyph frieze, a raised altar, candles and soul lanterns.
	 */
	private void greatPyramid(Canvas c) {
		int cx = GREAT_X, cz = GREAT_Z, half = GREAT_HALF, base = GREAT_BASE;
		pyramidShell(c, cx, cz, half, GREAT_H, base, false);
		int topY = base + GREAT_H;
		c.set(cx, topY + 1, cz, s(Blocks.GOLD_BLOCK));
		c.set(cx, topY, cz, s(Blocks.CHISELED_SANDSTONE));
		// The chamber: 11 x 11, 6 high, at the centre.
		int fy = base + 1;
		c.fill(cx - 5, fy, cz - 5, cx + 5, fy, cz + 5, s(Blocks.SMOOTH_SANDSTONE));
		c.fill(cx - 5, fy + 1, cz - 5, cx + 5, fy + 6, cz + 5, s(Blocks.AIR));
		for (int dx = -5; dx <= 5; dx++) {
			for (int dz = -5; dz <= 5; dz++) {
				if ((Math.abs(dx) == 3 && Math.abs(dz) <= 3) || (Math.abs(dz) == 3 && Math.abs(dx) <= 3)) {
					c.set(cx + dx, fy, cz + dz, s(Blocks.ORANGE_TERRACOTTA)); // floor inlay ring
				}
			}
		}
		// Glyph frieze on the chamber walls and a blue stripe.
		for (int i = -6; i <= 6; i++) {
			for (int[] wall : new int[][]{{i, -6}, {i, 6}, {-6, i}, {6, i}}) {
				int x = cx + wall[0], z = cz + wall[1];
				c.set(x, fy + 3, z, Math.floorMod(i, 3) == 0 ? s(Blocks.CHISELED_SANDSTONE) : s(Blocks.CUT_SANDSTONE));
				c.set(x, fy + 4, z, s(Blocks.BLUE_TERRACOTTA));
			}
		}
		// Four pillars with soul lanterns.
		for (int[] p : new int[][]{{-4, -4}, {4, -4}, {-4, 4}, {4, 4}}) {
			c.fill(cx + p[0], fy + 1, cz + p[1], cx + p[0], fy + 6, cz + p[1], s(Blocks.CUT_SANDSTONE));
			c.set(cx + p[0], fy + 1, cz + p[1], s(Blocks.CHISELED_SANDSTONE));
			c.set(cx + p[0] + (p[0] > 0 ? -1 : 1), fy + 3, cz + p[1], s(Blocks.SOUL_LANTERN));
		}
		// The altar: a raised 3 x 3 dais with steps, a chiseled block and candles.
		c.fill(cx - 1, fy + 1, cz - 2, cx + 1, fy + 1, cz, s(Blocks.CUT_SANDSTONE));
		for (int dx = -1; dx <= 1; dx++) {
			c.set(cx + dx, fy + 1, cz + 1, stair(Blocks.SMOOTH_SANDSTONE_STAIRS, Direction.NORTH, false));
		}
		c.set(cx, fy + 2, cz - 1, s(Blocks.CHISELED_SANDSTONE));
		c.set(cx, fy + 3, cz - 1, candles(4, true));
		c.set(cx - 1, fy + 2, cz - 2, candles(2, true));
		c.set(cx + 1, fy + 2, cz - 2, candles(2, true));
		c.set(cx - 1, fy + 2, cz, s(Blocks.DECORATED_POT));
		c.set(cx + 1, fy + 2, cz, s(Blocks.DECORATED_POT));
		c.set(cx, fy + 6, cz, lantern(true));
		// Corridor from the south face to the chamber: 3 wide, 4 high, lanterns every 4 blocks.
		int faceZ = cz + half;
		for (int z = cz + 6; z <= faceZ + 1; z++) {
			c.fill(cx - 1, fy + 1, z, cx + 1, fy + 4, z, s(Blocks.AIR));
			c.fill(cx - 1, fy, z, cx + 1, fy, z, s(Blocks.SMOOTH_SANDSTONE));
			if ((z - cz) % 4 == 0 && z < faceZ) {
				c.set(cx - 1, fy + 4, z, lantern(true));
				c.set(cx + 1, fy + 4, z, lantern(true));
			}
		}
		// The gateway: two tall pillars proud of the face, a lintel with glyphs, steps up from the plateau.
		for (int dx : new int[]{-3, 3}) {
			for (int z = faceZ + 1; z <= faceZ + 2; z++) {
				c.fill(cx + dx, fy, z, cx + dx, fy + 7, z, s(Blocks.CUT_SANDSTONE));
				c.set(cx + dx, fy + 1, z, s(Blocks.CHISELED_SANDSTONE));
			}
			c.set(cx + dx, fy + 8, faceZ + 2, s(Blocks.CHISELED_SANDSTONE));
			c.set(cx + dx, fy + 3, faceZ + 3, s(Blocks.WALL_TORCH).setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH));
		}
		for (int dx = -3; dx <= 3; dx++) {
			for (int z = faceZ + 1; z <= faceZ + 2; z++) {
				c.set(cx + dx, fy + 5, z, s(Math.floorMod(dx, 2) == 0 ? Blocks.CHISELED_SANDSTONE : Blocks.CUT_SANDSTONE));
				c.set(cx + dx, fy + 6, z, s(Blocks.CUT_SANDSTONE));
			}
			c.set(cx + dx, fy + 7, faceZ + 2, Kit.slab(Blocks.CUT_SANDSTONE_SLAB, false));
			if (Math.abs(dx) <= 2) {
				for (int z = faceZ + 1; z <= faceZ + 2; z++) {
					c.fill(cx + dx, fy + 1, z, cx + dx, fy + 4, z, s(Blocks.AIR));
					c.set(cx + dx, fy, z, s(Blocks.SMOOTH_SANDSTONE));
				}
			}
		}
		c.fill(cx - 2, fy, faceZ + 3, cx + 2, fy, faceZ + 3, stair(Blocks.SMOOTH_SANDSTONE_STAIRS, Direction.NORTH, false));
		// Obelisks flanking the approach.
		for (int dx : new int[]{-6, 6}) {
			int x = cx + dx, z = faceZ + 6, g = c.ground(x, z);
			c.set(x, g + 1, z, s(Blocks.CHISELED_SANDSTONE));
			c.fill(x, g + 2, z, x, g + 6, z, s(Blocks.CUT_SANDSTONE));
			c.set(x, g + 7, z, Kit.slab(Blocks.SANDSTONE_SLAB, false));
			c.set(x, g + 3, z, s(Blocks.ORANGE_TERRACOTTA));
		}
	}

	/**
	 * The Lesser Pyramid (23 wide, 15 layers): weathered, with missing stones and sand drifts; an entrance on the south
	 * face with a framed doorway leads down a short passage to a burial room with a sarcophagus and grave goods.
	 */
	private void lesserPyramid(Canvas c) {
		int cx = LESSER_X, cz = LESSER_Z, half = LESSER_HALF, base = LESSER_BASE;
		pyramidShell(c, cx, cz, half, LESSER_H, base, true);
		c.set(cx, base + LESSER_H + 1, cz, s(Blocks.CHISELED_SANDSTONE));
		int fy = base + 1;
		c.fill(cx - 3, fy, cz - 3, cx + 3, fy, cz + 3, s(Blocks.SMOOTH_SANDSTONE));
		c.fill(cx - 3, fy + 1, cz - 3, cx + 3, fy + 4, cz + 3, s(Blocks.AIR));
		for (int i = -4; i <= 4; i++) {
			for (int[] wall : new int[][]{{i, -4}, {i, 4}, {-4, i}, {4, i}}) {
				c.set(cx + wall[0], fy + 2, cz + wall[1], Math.floorMod(i, 2) == 0 ? s(Blocks.CHISELED_SANDSTONE) : s(Blocks.CUT_SANDSTONE));
			}
		}
		// Sarcophagus.
		c.fill(cx - 1, fy + 1, cz - 2, cx + 1, fy + 1, cz, s(Blocks.CUT_SANDSTONE));
		c.set(cx, fy + 2, cz - 2, Kit.slab(Blocks.CUT_SANDSTONE_SLAB, false));
		c.set(cx, fy + 2, cz - 1, Kit.slab(Blocks.CUT_SANDSTONE_SLAB, false));
		c.set(cx, fy + 2, cz, Kit.slab(Blocks.CUT_SANDSTONE_SLAB, false));
		c.set(cx - 1, fy + 2, cz - 2, s(Blocks.SKELETON_SKULL));
		c.set(cx - 3, fy + 1, cz - 3, s(Blocks.DECORATED_POT));
		c.set(cx + 3, fy + 1, cz - 3, s(Blocks.DECORATED_POT));
		c.set(cx + 3, fy + 1, cz - 2, s(Blocks.GOLD_BLOCK));
		c.set(cx - 3, fy + 1, cz + 2, candles(3, true));
		c.set(cx + 3, fy + 1, cz + 2, candles(3, true));
		c.set(cx, fy + 4, cz, lantern(true));
		int faceZ = cz + half;
		for (int z = cz + 4; z <= faceZ + 1; z++) {
			c.fill(cx - 1, fy + 1, z, cx + 1, fy + 3, z, s(Blocks.AIR));
			c.fill(cx - 1, fy, z, cx + 1, fy, z, s(Blocks.SMOOTH_SANDSTONE));
		}
		c.set(cx, fy + 3, cz + 7, lantern(true));
		for (int dx : new int[]{-2, 2}) {
			c.fill(cx + dx, fy, faceZ + 1, cx + dx, fy + 4, faceZ + 1, s(Blocks.CUT_SANDSTONE));
			c.set(cx + dx, fy + 5, faceZ + 1, s(Blocks.CHISELED_SANDSTONE));
		}
		c.fill(cx - 1, fy + 4, faceZ + 1, cx + 1, fy + 4, faceZ + 1, s(Blocks.CHISELED_SANDSTONE));
		c.fill(cx - 1, fy, faceZ + 2, cx + 1, fy, faceZ + 2, stair(Blocks.SANDSTONE_STAIRS, Direction.NORTH, false));
		// A fallen block and a broken column by the entrance.
		c.set(cx + 4, c.ground(cx + 4, faceZ + 3) + 1, faceZ + 3, s(Blocks.CUT_SANDSTONE));
		int g = c.ground(cx - 5, faceZ + 3);
		c.fill(cx - 5, g + 1, faceZ + 3, cx - 5, g + 2, faceZ + 3, s(Blocks.CUT_SANDSTONE));
	}

	// ---------------------------------------------------------------------------------------------------- combat

	/**
	 * The raider ruins: an open sand floor with old paving, the stubs of a square enclosure wall (some sections broken
	 * away), broken pillars of different heights, a fallen column and torches. The middle is left open for fighting.
	 */
	private void raiderRuins(Canvas c) {
		int cx = RUINS_X, cz = RUINS_Z, y = RUINS_Y;
		for (int dx = -8; dx <= 8; dx++) {
			for (int dz = -8; dz <= 8; dz++) {
				int x = cx + dx, z = cz + dz;
				if (!c.inChunk(x, z)) {
					continue;
				}
				double u = unit(x, z, 81);
				if (Math.abs(dx) <= 6 && Math.abs(dz) <= 6 && u < 0.35) {
					c.set(x, y, z, u < 0.12 ? s(Blocks.CUT_SANDSTONE) : s(Blocks.SMOOTH_SANDSTONE)); // old paving
				}
				boolean wall = (Math.abs(dx) == 8 || Math.abs(dz) == 8) && !(Math.abs(dx) <= 2 && dz == 8) && !(Math.abs(dz) <= 1 && Math.abs(dx) == 8);
				if (wall) {
					int h = 1 + (int) (unit(x, z, 82) * 3.2);
					if (unit(x, z, 83) < 0.22) {
						h = 0; // broken away
					}
					for (int k = 1; k <= h; k++) {
						c.set(x, y + k, z, k == h && h >= 3 ? Kit.slab(Blocks.CUT_SANDSTONE_SLAB, false) : s(k == 1 ? Blocks.CUT_SANDSTONE : Blocks.SANDSTONE));
					}
				}
			}
		}
		int[][] pillars = {{-5, -5, 6}, {5, -5, 3}, {-5, 5, 2}, {5, 5, 5}, {0, -6, 4}};
		for (int[] p : pillars) {
			int x = cx + p[0], z = cz + p[1];
			c.set(x, y + 1, z, s(Blocks.CHISELED_SANDSTONE));
			c.fill(x, y + 2, z, x, y + p[2], z, s(Blocks.CUT_SANDSTONE));
			if (p[2] >= 5) {
				c.set(x, y + p[2] + 1, z, s(Blocks.CHISELED_SANDSTONE));
				c.set(x, y + p[2] + 2, z, s(Blocks.TORCH));
			}
		}
		// A fallen column lying east-west by the north wall.
		for (int dx = -3; dx <= 0; dx++) {
			c.set(cx + dx, y + 1, cz - 3, dx == -3 ? s(Blocks.CHISELED_SANDSTONE) : s(Blocks.CUT_SANDSTONE));
		}
		c.set(cx - 6, y + 1, cz + 3, Kit.slab(Blocks.SANDSTONE_SLAB, false));
		c.set(cx + 6, y + 1, cz - 2, s(Blocks.DEAD_BUSH));
	}

	/**
	 * The bandit camp: three canvas tents on the rim, crates and barrels, two campfires, a weapon rack, and low
	 * sandstone cover walls, around an open fighting ground in the middle.
	 */
	private void banditCamp(Canvas c) {
		int cx = CAMP_X, cz = CAMP_Z, y = CAMP_Y;
		tent(c, cx + 6, cz - 6, y, Blocks.BROWN_WOOL, Direction.SOUTH);
		tent(c, cx - 7, cz - 5, y, Blocks.WHITE_WOOL, Direction.SOUTH);
		tent(c, cx + 7, cz + 6, y, Blocks.LIGHT_GRAY_WOOL, Direction.WEST);
		c.set(cx - 2, y + 1, cz - 3, s(Blocks.CAMPFIRE));
		c.set(cx + 3, y + 1, cz + 3, s(Blocks.CAMPFIRE));
		for (int[] b : new int[][]{{-8, 3}, {-8, 4}, {-7, 4}, {9, 0}, {9, 1}}) {
			c.set(cx + b[0], y + 1, cz + b[1], Canvas.facing(s(Blocks.BARREL), Direction.UP));
		}
		c.set(cx - 8, y + 2, cz + 3, Canvas.facing(s(Blocks.BARREL), Direction.UP));
		c.set(cx + 9, y + 2, cz + 0, s(Blocks.HAY_BLOCK));
		// Cover walls: short broken lines of sandstone.
		for (int[] w : new int[][]{{-4, 7, 1, 0, 4}, {3, -9, 1, 0, 3}, {-10, -1, 0, 1, 3}}) {
			for (int k = 0; k < w[4]; k++) {
				int x = cx + w[0] + w[2] * k, z = cz + w[1] + w[3] * k;
				c.set(x, y + 1, z, s(Blocks.SANDSTONE));
				if (k % 2 == 0) {
					c.set(x, y + 2, z, Kit.slab(Blocks.SANDSTONE_SLAB, false));
				}
			}
		}
		// Weapon rack.
		c.fill(cx - 5, y + 1, cz + 7, cx - 5, y + 2, cz + 7, s(Blocks.SPRUCE_FENCE));
		c.fill(cx - 7, y + 1, cz + 7, cx - 7, y + 2, cz + 7, s(Blocks.SPRUCE_FENCE));
		c.set(cx - 6, y + 2, cz + 7, Kit.slab(Blocks.SPRUCE_SLAB, true));
		c.set(cx - 6, y + 1, cz + 7, s(Blocks.GRINDSTONE));
	}

	/** A canvas tent: an A-frame of wool on fence poles, open at the front, a bedroll inside. */
	private static void tent(Canvas c, int x, int z, int y, Block wool, Direction front) {
		boolean alongX = front == Direction.NORTH || front == Direction.SOUTH;
		for (int i = -2; i <= 2; i++) {
			for (int k = 0; k <= 2; k++) {
				int a = -2 + k, b = 2 - k;
				int[] p1 = alongX ? new int[]{x + a, z + i} : new int[]{x + i, z + a};
				int[] p2 = alongX ? new int[]{x + b, z + i} : new int[]{x + i, z + b};
				c.set(p1[0], y + 1 + k, p1[1], s(wool));
				c.set(p2[0], y + 1 + k, p2[1], s(wool));
			}
		}
		int fx = front.getStepX(), fz = front.getStepZ();
		// Open front: clear the face at the front end, poles either side.
		for (int k = 0; k <= 2; k++) {
			c.set(x + fx * 2, y + 1 + k, z + fz * 2, s(Blocks.AIR));
		}
		c.set(x + fx * 3 + (alongX ? -2 : 0), y + 1, z + fz * 3 + (alongX ? 0 : -2), s(Blocks.SPRUCE_FENCE));
		c.set(x + fx * 3 + (alongX ? 2 : 0), y + 1, z + fz * 3 + (alongX ? 0 : 2), s(Blocks.SPRUCE_FENCE));
		c.set(x, y + 1, z, s(Blocks.BROWN_CARPET));
		c.set(x - fx, y + 1, z - fz, s(Blocks.BROWN_CARPET));
	}
}
