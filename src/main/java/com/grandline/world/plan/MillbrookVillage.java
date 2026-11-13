package com.grandline.world.plan;

import com.grandline.world.plan.Anchor.Type;
import com.grandline.world.plan.Terrain.Ground;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;

import java.util.ArrayList;
import java.util.List;

/**
 * Millbrook Village (Lv 30), rebuilt: a gently rolling grassy island about 138 x 122 blocks. A broad wooden dock on the
 * south shore is the arrival point; a road runs north to the village lawn, ringed by seven cottages (an inn, a smithy,
 * a bakery, a farmhouse, a weaver's cottage, the elder's house and a fisher's cottage). A tall stone-and-timber
 * windmill stands on a knoll to the west, a squatter one among the wheat to the north-east. East of the village, past
 * the quest giver's notice board, are two combat fields: an open training meadow and, farther out, a rough bandit camp.
 * Small landings on the west and north-east shores.
 *
 * <p>Island-local coordinates: +x east, +z south, sea level 62. Built entirely from code, so it is identical in every
 * world and assembles chunk by chunk. Worlds created before this design keep the original Millbrook
 * ({@link FirstSeaPlans} legacy plan).
 */
final class MillbrookVillage extends BasePlan {
	/** Village ground level (top block); buildings sit on a one-block plinth above it. */
	static final int G = 66;

	// ---- landmarks (island-local) ----
	static final int LAWN_X = -6, LAWN_Z = -2, LAWN_R = 12;
	static final int WMILL_X = -48, WMILL_Z = -6, WMILL_Y = 68;
	static final int EMILL_X = 38, EMILL_Z = -28, EMILL_Y = 67;
	static final int QUEST_X = 29, QUEST_Z = 12;
	static final int FIELD_A_X = 46, FIELD_A_Z = 0, FIELD_R = 9;
	static final int FIELD_B_X = 48, FIELD_B_Z = 28, FIELD_B_Y = 67;
	static final int DOCK_X = -3, DOCK_LAND_Z = 50, DOCK_END_Z = 77, DOCK_Y = 64;
	static final int WEST_LANDING_Z = 4, NE_LANDING_X = 40;

	// ---- roads ----
	static final double[] MAIN_ROAD = {-3, 48, -3, 38, -4, 26, -5, 12};
	static final double[] WEST_ROAD = {-19, -2, -30, -3, -43, -5};
	static final double[] WEST_SHORE = {-48, 1, -56, 4, -65, 4};
	static final double[] EAST_ROAD = {6, 1, 16, 4, 24, 9, 28, 12};
	static final double[] FIELD_A_ROAD = {31, 10, 37, 4};
	static final double[] FIELD_B_ROAD = {30, 15, 39, 22};
	static final double[] EMILL_ROAD = {17, 3, 24, -2, 30, -10, 35, -22};
	static final double[] NE_ROAD = {39, -34, 40, -44};

	/** A cottage: centre, size (w along its front, d front to back), facing, look and use. */
	record Cottage(int cx, int cz, int w, int d, Direction front, Style style, Use use, int floors, boolean ridgeAcross) {
	}

	/** Materials of one cottage. */
	record Style(Block wall, Block upper, Block base, Block frame, Block beam, Block roof, Block roofSlab, Block gable, Block door) {
	}

	enum Use { INN, SMITHY, BAKERY, FARMHOUSE, WEAVER, ELDER, FISHER }

	static final Style OAK = new Style(Blocks.OAK_PLANKS, Blocks.OAK_PLANKS, Blocks.COBBLESTONE, Blocks.OAK_LOG, Blocks.STRIPPED_OAK_LOG,
			Blocks.SPRUCE_STAIRS, Blocks.SPRUCE_SLAB, Blocks.OAK_PLANKS, Blocks.SPRUCE_DOOR);
	static final Style SPRUCE = new Style(Blocks.SPRUCE_PLANKS, Blocks.SPRUCE_PLANKS, Blocks.STONE_BRICKS, Blocks.SPRUCE_LOG, Blocks.STRIPPED_SPRUCE_LOG,
			Blocks.DARK_OAK_STAIRS, Blocks.DARK_OAK_SLAB, Blocks.SPRUCE_PLANKS, Blocks.OAK_DOOR);
	static final Style TIMBER = new Style(Blocks.WHITE_TERRACOTTA, Blocks.OAK_PLANKS, Blocks.COBBLESTONE, Blocks.DARK_OAK_LOG, Blocks.STRIPPED_DARK_OAK_LOG,
			Blocks.SPRUCE_STAIRS, Blocks.SPRUCE_SLAB, Blocks.SPRUCE_PLANKS, Blocks.DARK_OAK_DOOR);
	static final Style STONE = new Style(Blocks.COBBLESTONE, Blocks.SPRUCE_PLANKS, Blocks.STONE_BRICKS, Blocks.SPRUCE_LOG, Blocks.STRIPPED_SPRUCE_LOG,
			Blocks.STONE_BRICK_STAIRS, Blocks.STONE_BRICK_SLAB, Blocks.SPRUCE_PLANKS, Blocks.SPRUCE_DOOR);
	static final Style BIRCH = new Style(Blocks.BIRCH_PLANKS, Blocks.BIRCH_PLANKS, Blocks.COBBLESTONE, Blocks.SPRUCE_LOG, Blocks.STRIPPED_SPRUCE_LOG,
			Blocks.SPRUCE_STAIRS, Blocks.SPRUCE_SLAB, Blocks.BIRCH_PLANKS, Blocks.SPRUCE_DOOR);

	static final List<Cottage> COTTAGES = List.of(
			new Cottage(-6, -25, 11, 8, Direction.SOUTH, TIMBER, Use.INN, 2, false),       // north of the lawn
			new Cottage(-26, -14, 7, 9, Direction.EAST, OAK, Use.FARMHOUSE, 1, true),      // north-west
			new Cottage(-28, 8, 8, 7, Direction.EAST, BIRCH, Use.WEAVER, 1, false),        // west
			new Cottage(-20, 23, 7, 7, Direction.NORTH, OAK, Use.FISHER, 1, true),         // south-west, by the road
			new Cottage(12, 22, 9, 7, Direction.NORTH, STONE, Use.SMITHY, 1, false),       // south-east
			new Cottage(17, -9, 7, 9, Direction.WEST, SPRUCE, Use.BAKERY, 1, true),        // east
			new Cottage(12, -24, 8, 8, Direction.SOUTH, SPRUCE, Use.ELDER, 1, false));     // north-east

	private final List<int[]> trees = new ArrayList<>();
	private final List<int[]> bushes = new ArrayList<>();
	private final List<int[]> lamps = new ArrayList<>();

	MillbrookVillage() {
		super("Millbrook Village", 0.05);
		BlockState path = s(Blocks.DIRT_PATH);
		BlockState edge = s(Blocks.COARSE_DIRT);
		terrain.mass(0, 0, 65, 59, 3.4, 4.0, Ground.GRASS)              // the island: a broad, flat-topped green
				.mass(-30, -34, 24, 16, 5.5, 2.0, Ground.GRASS)         // a soft rise behind the village (north-west)
				.mass(FIELD_B_X + 2, FIELD_B_Z, 16, 13, 4.0, 2.0, Ground.GRASS) // the rough eastern rise of field B
				.flat(-4, -2, 36, 30, G, 9, null)                       // the village plateau
				.flat(WMILL_X, WMILL_Z, 6, 6, WMILL_Y, 9, null)         // knoll under the west windmill
				.flat(EMILL_X, EMILL_Z, 7, 7, EMILL_Y, 7, null)         // east windmill yard
				.flat(QUEST_X, QUEST_Z, 5, 5, G, 5, null)               // notice board clearing
				.flat(FIELD_A_X, FIELD_A_Z, FIELD_R + 1, FIELD_R + 1, G, 6, null)
				.flat(FIELD_B_X, FIELD_B_Z, FIELD_R + 1, FIELD_R, FIELD_B_Y, 6, null)
				.flat(DOCK_X, 47, 5, 4, 65, 4, s(Blocks.STONE_BRICKS))  // arrival plaza at the head of the dock
				// Roads: packed earth with coarse dirt shoulders (first matching path wins, so the core is listed first).
				.path(path, 3, MAIN_ROAD).path(path, 2.6, WEST_ROAD).path(path, 2.2, WEST_SHORE).path(path, 3, EAST_ROAD)
				.path(path, 2.4, FIELD_A_ROAD).path(path, 2.4, FIELD_B_ROAD).path(path, 2.4, EMILL_ROAD).path(path, 2.2, NE_ROAD)
				.path(path, 2.2, ring(LAWN_X, LAWN_Z, LAWN_R + 1.5, 20));
		for (Cottage cottage : COTTAGES) {
			int[] door = doorStep(cottage);
			double dx = LAWN_X - door[0], dz = LAWN_Z - door[1], len = Math.hypot(dx, dz);
			double reach = Math.max(0, len - (LAWN_R + 1.5));
			terrain.path(path, 2, door[0], door[1], door[0] + dx / len * reach, door[1] + dz / len * reach);
		}
		terrain.path(edge, 4.4, MAIN_ROAD).path(edge, 4.2, EAST_ROAD).path(edge, 3.6, WEST_ROAD)
				.reach(DOCK_END_Z + 4);

		anchor(Anchor.at(Type.QUEST, QUEST_X, Anchor.SURFACE, QUEST_Z, 90));   // faces west, toward the village
		anchor(new Anchor(Type.ARRIVAL, DOCK_X, Anchor.SURFACE, 46, 0, 180));    // the dock's arrival plaza, facing the village
		anchor(Anchor.at(Type.HOME, -6, Anchor.SURFACE, -19, 0));                 // the Innkeeper, outside the inn
		anchor(Anchor.at(Type.SWORDSMITH, 14, Anchor.SURFACE, 16, 180));          // the Swordsmith, outside the smithy
		anchor(Anchor.at(Type.FISHERMAN, 1, DOCK_Y + 1, 72, 90));                 // on the dock's T-head
		anchor(Anchor.at(Type.DOCK, -11, 63, 74));
		anchor(Anchor.area(Type.ENEMY_CAMP, FIELD_A_X, FIELD_A_Z, 6));
		anchor(Anchor.area(Type.ENEMY_CAMP, FIELD_B_X, FIELD_B_Z, 6));
		anchor(Anchor.area(Type.BOSS_ARENA, FIELD_B_X, FIELD_B_Z, 6));            // quest bosses appear in the bandit camp
		anchor(Anchor.of(Type.TREASURE, -42, 32));
		placeNature();
	}

	// ==================================================================================================== layout

	static double[] ring(double cx, double cz, double r, int n) {
		double[] pts = new double[(n + 1) * 2];
		for (int i = 0; i <= n; i++) {
			double a = i * Math.PI * 2 / n;
			pts[i * 2] = cx + Math.cos(a) * r;
			pts[i * 2 + 1] = cz + Math.sin(a) * r;
		}
		return pts;
	}

	/** Island position of the block just outside a cottage's door. */
	static int[] doorStep(Cottage c) {
		Frame f = new Frame(null, c, 0);
		int u = (c.w() - 1) / 2;
		return new int[]{f.x(u, c.d()), f.z(u, c.d())};
	}

	static long hash(int x, int z) {
		long h = x * 0x9E3779B97F4A7C15L ^ z * 0xC2B2AE3D27D4EB4FL;
		h ^= h >>> 29;
		h *= 0xBF58476D1CE4E5B9L;
		return h ^ (h >>> 32);
	}

	static double unit(int x, int z, int salt) {
		return ((hash(x * 31 + salt, z * 17 - salt) >>> 11) & 0xFFFF) / 65536.0;
	}

	/** Areas kept free of trees, bushes and flowers: buildings, landmarks, fields, the dock and the notice board. */
	boolean built(int x, int z, int margin) {
		for (Cottage c : COTTAGES) {
			int[] box = footprint(c);
			int extra = c.use() == Use.SMITHY ? 4 : 0;
			if (x >= box[0] - margin - extra && x <= box[2] + margin + extra && z >= box[1] - margin && z <= box[3] + margin) {
				return true;
			}
		}
		return near(x, z, WMILL_X, WMILL_Z, 7 + margin) || near(x, z, EMILL_X, EMILL_Z, 7 + margin)
				|| (x >= EMILL_X - 14 && x <= EMILL_X - 5 && z >= EMILL_Z - 8 && z <= EMILL_Z + 6) // wheat field
				|| near(x, z, QUEST_X + 1, QUEST_Z, 4 + margin)
				|| near(x, z, FIELD_A_X, FIELD_A_Z, FIELD_R + margin) || near(x, z, FIELD_B_X, FIELD_B_Z, FIELD_R + margin)
				|| (Math.abs(x - DOCK_X) <= 7 && z >= 42);
	}

	static boolean near(int x, int z, int cx, int cz, int r) {
		return (x - cx) * (x - cx) + (z - cz) * (z - cz) <= r * r;
	}

	/** Trees and bushes at fixed spots: framing the village and the shore, never in sightlines or fields. */
	private void placeNature() {
		int[][] oaks = {{-40, -28}, {-52, 18}, {-38, 36}, {-14, 40}, {16, 40}, {28, 32}, {-56, -24}, {-22, -42}, {4, -42},
				{24, -40}, {58, 12}, {-60, 30}, {-30, 30}, {30, -14}, {-44, 12}, {62, -10}, {-8, -44}};
		int[][] birches = {{-46, -20}, {-34, 44}, {6, 44}, {22, -46}, {-64, -8}, {56, 40}, {-18, -36}};
		for (int[] t : oaks) {
			trees.add(new int[]{t[0], t[1], 0, 5 + (int) (unit(t[0], t[1], 1) * 2)});
		}
		for (int[] t : birches) {
			trees.add(new int[]{t[0], t[1], 1, 6 + (int) (unit(t[0], t[1], 2) * 2)});
		}
		int[][] bush = {{-36, -24}, {-49, 22}, {-42, 33}, {-11, 38}, {19, 37}, {33, -12}, {-23, -40}, {7, -39}, {-58, 26},
				{55, 15}, {60, -6}, {-60, -12}, {-33, 26}, {27, 30}, {-16, 16}, {4, 14}, {-30, -26}, {22, -30}, {36, 42}};
		for (int[] b : bush) {
			bushes.add(b);
		}
		// Lamp posts: along the main road and at the junctions.
		int[][] lampSpots = {{-6, 44}, {0, 36}, {-7, 28}, {-1, 20}, {-8, 13}, {8, 6}, {20, 3}, {26, 7}, {-20, -5}, {-37, -2},
				{-16, 6}, {4, -10}, {-16, -10}, {33, -18}, {35, 20}, {34, 6}};
		for (int[] l : lampSpots) {
			lamps.add(l);
		}
	}

	// ==================================================================================================== build

	@Override
	public void build(Canvas c) {
		groundCover(c);
		dock(c);
		landing(c, -64, WEST_LANDING_Z, Direction.WEST);
		landing(c, NE_LANDING_X, -45, Direction.NORTH);
		lawn(c);
		for (Cottage cottage : COTTAGES) {
			int[] box = footprint(cottage);
			if (c.near((box[0] + box[2]) / 2, (box[1] + box[3]) / 2, 16)) {
				cottage(c, cottage);
			}
		}
		if (c.near(WMILL_X, WMILL_Z, 16)) {
			windmill(c, WMILL_X, WMILL_Z, WMILL_Y + 1, Direction.WEST, 4, 7, 6, 9, Direction.EAST);
		}
		if (c.near(EMILL_X, EMILL_Z, 16)) {
			windmill(c, EMILL_X, EMILL_Z, EMILL_Y + 1, Direction.EAST, 5, 5, 5, 7, Direction.SOUTH);
			wheatField(c);
		}
		noticeBoard(c);
		fieldA(c);
		fieldB(c);
		for (int[] t : trees) {
			if (c.near(t[0], t[1], 4)) {
				if (t[2] == 0) {
					Kit.roundTree(c, t[0], t[1], t[3], Blocks.OAK_LOG, Blocks.OAK_LEAVES);
				} else {
					Kit.roundTree(c, t[0], t[1], t[3], Blocks.BIRCH_LOG, Blocks.BIRCH_LEAVES);
				}
			}
		}
		for (int[] b : bushes) {
			if (c.near(b[0], b[1], 2)) {
				bush(c, b[0], b[1]);
			}
		}
		for (int[] l : lamps) {
			if (c.inChunk(l[0], l[1])) {
				lampPost(c, l[0], l[1]);
			}
		}
		treasure(c);
	}

	// ---------------------------------------------------------------------------------------------------- ground

	/** Flowers in loose patches, short grass, and a few tufts of tall grass on open grass; never on roads or in yards. */
	private void groundCover(Canvas c) {
		Block[] flowers = {Blocks.POPPY, Blocks.DANDELION, Blocks.CORNFLOWER, Blocks.OXEYE_DAISY, Blocks.AZURE_BLUET, Blocks.ALLIUM};
		for (int x = -72; x <= 72; x++) {
			for (int z = -64; z <= 64; z++) {
				if (!c.inChunk(x, z)) {
					continue;
				}
				int g = c.ground(x, z);
				if (g <= Terrain.WATER_TOP + 1 || c.terrain().paving(x, z) != null || built(x, z, 1)) {
					continue;
				}
				double patch = (Math.sin(x * 0.21 + 1.3) + Math.sin(z * 0.17 + 0.4) + Math.sin((x + z) * 0.11)) / 3.0;
				double r = unit(x, z, 7);
				boolean lawn = near(x, z, LAWN_X, LAWN_Z, LAWN_R);
				if (patch > 0.45 && r < (lawn ? 0.05 : 0.16)) {
					c.set(x, g + 1, z, s(flowers[(int) (unit(x, z, 9) * flowers.length)]));
				} else if (r > 0.9) {
					c.set(x, g + 1, z, s(Blocks.GRASS));
				} else if (r > 0.885 && !lawn) {
					c.set(x, g + 1, z, s(Blocks.TALL_GRASS).setValue(BlockStateProperties.DOUBLE_BLOCK_HALF,
							net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER));
					c.set(x, g + 2, z, s(Blocks.TALL_GRASS).setValue(BlockStateProperties.DOUBLE_BLOCK_HALF,
							net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER));
				}
			}
		}
	}

	private static void bush(Canvas c, int x, int z) {
		int g = c.ground(x, z);
		if (g <= Terrain.WATER_TOP) {
			return;
		}
		BlockState leaf = Kit.leaves(Blocks.OAK_LEAVES);
		c.set(x, g + 1, z, unit(x, z, 3) < 0.5 ? s(Blocks.FLOWERING_AZALEA) : s(Blocks.AZALEA));
		c.set(x + 1, g + 1, z, leaf);
		c.set(x, g + 1, z + 1, leaf);
		if (unit(x, z, 4) < 0.5) {
			c.set(x - 1, g + 1, z, leaf);
			c.set(x, g + 2, z, leaf);
		}
	}

	private static void lampPost(Canvas c, int x, int z) {
		int g = c.ground(x, z);
		c.set(x, g + 1, z, s(Blocks.STONE_BRICK_WALL));
		c.fill(x, g + 2, z, x, g + 3, z, s(Blocks.SPRUCE_FENCE));
		c.set(x, g + 4, z, s(Blocks.LANTERN));
	}

	// ---------------------------------------------------------------------------------------------------- lawn

	/** The village green: open grass, a big old oak at its heart with a stone ring bench, flower beds at the edge. */
	private void lawn(Canvas c) {
		if (!c.near(LAWN_X, LAWN_Z, LAWN_R + 4)) {
			return;
		}
		int g = G;
		// Ring of stone around the tree, with benches (stairs) facing out.
		for (int dx = -3; dx <= 3; dx++) {
			for (int dz = -3; dz <= 3; dz++) {
				double d = Math.sqrt(dx * dx + dz * dz);
				if (d > 2.6 && d <= 3.5) {
					c.set(LAWN_X + dx, g, LAWN_Z + dz, s(Blocks.STONE_BRICKS));
					c.set(LAWN_X + dx, g + 1, LAWN_Z + dz, Kit.slab(Blocks.STONE_BRICK_SLAB, false));
				} else if (d <= 2.6) {
					c.set(LAWN_X + dx, g, LAWN_Z + dz, s(Blocks.ROOTED_DIRT));
				}
			}
		}
		// The old oak: a thick trunk and a broad crown.
		c.fill(LAWN_X, g + 1, LAWN_Z, LAWN_X, g + 7, LAWN_Z, s(Blocks.OAK_LOG));
		for (Direction d : Direction.Plane.HORIZONTAL) {
			c.set(LAWN_X + d.getStepX(), g + 1, LAWN_Z + d.getStepZ(), s(Blocks.OAK_LOG));
			c.set(LAWN_X + d.getStepX() * 2, g + 6, LAWN_Z + d.getStepZ() * 2,
					s(Blocks.OAK_LOG).setValue(RotatedPillarBlock.AXIS, d.getAxis()));
		}
		BlockState leaf = Kit.leaves(Blocks.OAK_LEAVES);
		for (int dy = 0; dy <= 4; dy++) {
			int r = dy == 0 ? 4 : dy == 4 ? 2 : 5 - (dy / 2);
			for (int dx = -r; dx <= r; dx++) {
				for (int dz = -r; dz <= r; dz++) {
					double d = Math.sqrt(dx * dx + dz * dz);
					if (d <= r + 0.3 && !(d > r - 0.7 && unit(dx, dz + dy * 7, 5) < 0.35)) {
						if (dx == 0 && dz == 0 && dy < 2) {
							continue;
						}
						c.set(LAWN_X + dx, g + 6 + dy, LAWN_Z + dz, leaf);
					}
				}
			}
		}
		// Flower clusters at four points on the lawn edge: an azalea with tulips around it.
		int[][] beds = {{LAWN_X - 9, LAWN_Z - 7}, {LAWN_X + 9, LAWN_Z - 7}, {LAWN_X - 9, LAWN_Z + 7}, {LAWN_X + 9, LAWN_Z + 6}};
		Block[] bloom = {Blocks.RED_TULIP, Blocks.PINK_TULIP, Blocks.WHITE_TULIP, Blocks.ORANGE_TULIP};
		for (int i = 0; i < beds.length; i++) {
			int bx = beds[i][0], bz = beds[i][1];
			c.set(bx, g + 1, bz, s(Blocks.FLOWERING_AZALEA));
			for (int[] o : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {-1, -1}}) {
				c.set(bx + o[0], g, bz + o[1], s(Blocks.GRASS_BLOCK));
				c.set(bx + o[0], g + 1, bz + o[1], s(bloom[(i + Math.abs(o[0] + o[1])) % 4]));
			}
		}
	}

	// ---------------------------------------------------------------------------------------------------- docks

	/**
	 * The main dock: a stone arrival plaza with a timber arch, then a broad plank pier (5 wide, posts every 4 blocks,
	 * rails, lanterns) out to a wide T-head with cargo and a moored boat.
	 */
	private void dock(Canvas c) {
		if (!c.near(DOCK_X, (DOCK_LAND_Z + DOCK_END_Z) / 2, 24)) {
			return;
		}
		// Plaza paving: worn stone bricks.
		for (int x = DOCK_X - 5; x <= DOCK_X + 5; x++) {
			for (int z = 43; z <= 51; z++) {
				double dx = (x - DOCK_X) / 5.0, dz = (z - 47) / 4.0;
				if (dx * dx + dz * dz <= 1.0) {
					double r = unit(x, z, 11);
					c.set(x, 65, z, r < 0.18 ? s(Blocks.CRACKED_STONE_BRICKS) : r < 0.32 ? s(Blocks.MOSSY_STONE_BRICKS)
							: r < 0.42 ? s(Blocks.COBBLESTONE) : s(Blocks.STONE_BRICKS));
					c.fill(x, 66, z, x, 68, z, s(Blocks.AIR));
				}
			}
		}
		// Plaza edge: cobble kerb where it meets the beach.
		// Timber arch over the dock entrance.
		int archZ = DOCK_LAND_Z + 1;
		for (int x : new int[]{DOCK_X - 3, DOCK_X + 3}) {
			c.fill(x, 62, archZ, x, 69, archZ, s(Blocks.SPRUCE_LOG));
			c.set(x, 70, archZ, Kit.slab(Blocks.SPRUCE_SLAB, false));
		}
		c.fill(DOCK_X - 4, 69, archZ, DOCK_X + 4, 69, archZ, s(Blocks.STRIPPED_SPRUCE_LOG).setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
		c.set(DOCK_X - 4, 69, archZ, Kit.stairs(Blocks.SPRUCE_STAIRS, Direction.EAST, true));
		c.set(DOCK_X + 4, 69, archZ, Kit.stairs(Blocks.SPRUCE_STAIRS, Direction.WEST, true));
		c.set(DOCK_X - 1, 68, archZ, s(Blocks.LANTERN).setValue(LanternBlock.HANGING, true));
		c.set(DOCK_X + 1, 68, archZ, s(Blocks.LANTERN).setValue(LanternBlock.HANGING, true));
		// Step down from the plaza (65) to the deck (64).
		for (int x = DOCK_X - 2; x <= DOCK_X + 2; x++) {
			c.set(x, 65, DOCK_LAND_Z, Kit.slab(Blocks.STONE_BRICK_SLAB, false));
		}
		// The pier.
		for (int z = DOCK_LAND_Z + 1; z <= DOCK_END_Z; z++) {
			boolean head = z >= DOCK_END_Z - 5;
			int half = head ? 7 : 2;
			for (int x = DOCK_X - half; x <= DOCK_X + half; x++) {
				int w = x - DOCK_X;
				boolean edgeX = Math.abs(w) == half;
				boolean outer = edgeX || z == DOCK_END_Z || (head && z == DOCK_END_Z - 5 && Math.abs(w) > 2);
				c.set(x, DOCK_Y, z, (w == 0 && !head) || (head && z == DOCK_END_Z - 3) ? s(Blocks.STRIPPED_SPRUCE_LOG)
						.setValue(RotatedPillarBlock.AXIS, head && z == DOCK_END_Z - 3 ? Direction.Axis.X : Direction.Axis.Z) : s(Blocks.SPRUCE_PLANKS));
				c.fill(x, DOCK_Y + 1, z, x, DOCK_Y + 4, z, s(Blocks.AIR));
				// Under-deck beam and posts down to the sea floor.
				boolean post = outer && ((z - DOCK_LAND_Z) % 4 == 0 || z == DOCK_END_Z) && (edgeX || Math.abs(w) % 4 == 3 || head);
				if (post && (edgeX || z == DOCK_END_Z || z == DOCK_END_Z - 5)) {
					int floor = Math.min(c.ground(x, z), Terrain.WATER_TOP - 1);
					c.fill(x, floor, z, x, DOCK_Y - 1, z, s(Blocks.SPRUCE_LOG));
					c.set(x, DOCK_Y + 1, z, s(Blocks.SPRUCE_FENCE));
					if ((z - DOCK_LAND_Z) % 8 == 0 || z == DOCK_END_Z) {
						c.set(x, DOCK_Y + 2, z, s(Blocks.LANTERN));
					}
				} else if (edgeX && !head) {
					c.set(x, DOCK_Y - 1, z, s(Blocks.STRIPPED_SPRUCE_LOG).setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z));
					// Rails along the walkway, with gaps for boats every 8 blocks.
					if ((z - DOCK_LAND_Z) % 8 != 4) {
						c.set(x, DOCK_Y + 1, z, Kit.connected(Blocks.SPRUCE_FENCE, false));
					}
				} else if (head && z == DOCK_END_Z && Math.abs(w) < half) {
					c.set(x, DOCK_Y + 1, z, Kit.connected(Blocks.SPRUCE_FENCE, true));
				}
			}
		}
		// Cargo on the T-head: barrels, crates (barrels on their side), hay and a net (cobweb-free: string fences).
		int hz = DOCK_END_Z - 4;
		c.set(DOCK_X - 6, DOCK_Y + 1, hz, Canvas.facing(s(Blocks.BARREL), Direction.UP));
		c.set(DOCK_X - 6, DOCK_Y + 1, hz + 1, Canvas.facing(s(Blocks.BARREL), Direction.UP));
		c.set(DOCK_X - 6, DOCK_Y + 2, hz, Canvas.facing(s(Blocks.BARREL), Direction.UP));
		c.set(DOCK_X - 5, DOCK_Y + 1, hz, Canvas.facing(s(Blocks.BARREL), Direction.EAST));
		c.set(DOCK_X + 5, DOCK_Y + 1, hz, s(Blocks.HAY_BLOCK));
		c.set(DOCK_X + 6, DOCK_Y + 1, hz, s(Blocks.HAY_BLOCK));
		c.set(DOCK_X + 6, DOCK_Y + 1, hz + 1, s(Blocks.BARREL));
		c.set(DOCK_X + 6, DOCK_Y + 2, hz, Kit.slab(Blocks.SPRUCE_SLAB, false));
		// Cargo near the land end, out of the walkway.
		c.set(DOCK_X - 5, 66, 45, Canvas.facing(s(Blocks.BARREL), Direction.UP));
		c.set(DOCK_X - 5, 66, 46, Canvas.facing(s(Blocks.BARREL), Direction.UP));
		c.set(DOCK_X - 4, 66, 45, Kit.stairs(Blocks.SPRUCE_STAIRS, Direction.SOUTH, false));
		c.set(DOCK_X + 5, 66, 46, s(Blocks.HAY_BLOCK));
		c.set(DOCK_X + 5, 66, 45, s(Blocks.COMPOSTER));
	}

	/** A small landing: a short plank jetty from the shore, a mooring post with a lantern and a barrel. */
	private void landing(Canvas c, int x, int z, Direction out) {
		if (!c.near(x, z, 16)) {
			return;
		}
		int y = Terrain.WATER_TOP + 1;
		boolean alongX = out.getAxis() == Direction.Axis.X;
		// Walk back from the given point to the shore so the jetty starts on land.
		int sx = x, sz = z;
		for (int i = 0; i < 12 && c.ground(sx, sz) <= Terrain.WATER_TOP; i++) {
			sx -= out.getStepX();
			sz -= out.getStepZ();
		}
		for (int i = 0; i <= 9; i++) {
			for (int w = -1; w <= 1; w++) {
				int px = sx + out.getStepX() * i + (alongX ? 0 : w);
				int pz = sz + out.getStepZ() * i + (alongX ? w : 0);
				c.set(px, y, pz, w == 0 ? s(Blocks.OAK_PLANKS) : s(Blocks.SPRUCE_PLANKS));
				c.fill(px, y + 1, pz, px, y + 3, pz, s(Blocks.AIR));
				if (Math.abs(w) == 1 && (i == 3 || i == 9)) {
					c.fill(px, Math.min(c.ground(px, pz), Terrain.WATER_TOP - 1), pz, px, y - 1, pz, s(Blocks.SPRUCE_LOG));
					c.set(px, y + 1, pz, s(Blocks.SPRUCE_FENCE));
					if (i == 9 && w == 1) {
						c.set(px, y + 2, pz, s(Blocks.LANTERN));
					}
				}
			}
		}
		int bx = sx + out.getStepX() * 2 + (alongX ? 0 : 2);
		int bz = sz + out.getStepZ() * 2 + (alongX ? 2 : 0);
		if (c.ground(bx, bz) > Terrain.WATER_TOP) {
			c.set(bx, c.ground(bx, bz) + 1, bz, Canvas.facing(s(Blocks.BARREL), Direction.UP));
		}
	}

	// ---------------------------------------------------------------------------------------------------- cottages

	/** Maps a cottage's local (u along the front, v from back to front) into island coordinates and rotates states. */
	static final class Frame {
		final Canvas c;
		final int ox, oz, y;
		final Rotation rot;
		final int cu, cv;

		Frame(Canvas c, Cottage k, int y) {
			this.c = c;
			this.ox = k.cx();
			this.oz = k.cz();
			this.y = y;
			this.rot = switch (k.front()) {
				case WEST -> Rotation.CLOCKWISE_90;
				case NORTH -> Rotation.CLOCKWISE_180;
				case EAST -> Rotation.COUNTERCLOCKWISE_90;
				default -> Rotation.NONE;
			};
			this.cu = (k.w() - 1) / 2;
			this.cv = (k.d() - 1) / 2;
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

	/** Island bounding box (minX, minZ, maxX, maxZ) of a cottage's walls. */
	static int[] footprint(Cottage k) {
		Frame f = new Frame(null, k, 0);
		int x1 = f.x(0, 0), z1 = f.z(0, 0), x2 = f.x(k.w() - 1, k.d() - 1), z2 = f.z(k.w() - 1, k.d() - 1);
		return new int[]{Math.min(x1, x2), Math.min(z1, z2), Math.max(x1, x2), Math.max(z1, z2)};
	}

	static BlockState log(Block block, Direction.Axis axis) {
		return s(block).setValue(RotatedPillarBlock.AXIS, axis);
	}

	static BlockState stair(Block block, Direction facing, boolean top) {
		return Kit.stairs(block, facing, top);
	}

	/**
	 * A cottage on a cobble plinth (floor one block above the ground): log corner posts, a stone base course, plank or
	 * plaster walls, a timber beam under the eaves, framed windows with shutters, a door with a step and a lantern, a
	 * gable roof with a one-block overhang, a chimney, and a furnished interior with a ceiling.
	 */
	private void cottage(Canvas c, Cottage k) {
		int floorY = G + 1;
		Frame f = new Frame(c, k, floorY);
		int w = k.w(), d = k.d();
		Style st = k.style();
		int[] box = footprint(k);
		c.foundation(box[0], box[1], box[2], box[3], floorY, s(Blocks.COBBLESTONE));
		// Floor.
		f.fill(1, 0, 1, w - 2, 0, d - 2, s(Blocks.SPRUCE_PLANKS));
		int storey = 5;          // floor, 3 rows of wall, a beam row
		int top = k.floors() * storey; // dy of the roof plate
		for (int fl = 0; fl < k.floors(); fl++) {
			int base = fl * storey;
			Block wall = fl == 0 ? st.wall() : st.upper();
			for (int u = 0; u < w; u++) {
				for (int v = 0; v < d; v++) {
					boolean eu = u == 0 || u == w - 1, ev = v == 0 || v == d - 1;
					if (!eu && !ev) {
						f.fill(u, base + 1, v, u, base + 4, v, s(Blocks.AIR));
						continue;
					}
					if (eu && ev) {
						f.fill(u, base + 1, v, u, base + 4, v, s(st.frame()));
						continue;
					}
					f.set(u, base + 1, v, fl == 0 ? s(st.base()) : s(wall));
					f.set(u, base + 2, v, s(wall));
					f.set(u, base + 3, v, s(wall));
					f.set(u, base + 4, v, log(st.beam(), ev ? Direction.Axis.X : Direction.Axis.Z));
				}
			}
			// Floor/ceiling between storeys, and the top plate.
			f.fill(0, base + 5, 0, w - 1, base + 5, d - 1, s(Blocks.SPRUCE_PLANKS));
			windows(f, k, base);
			f.set((w - 1) / 2, base + 4, (d - 1) / 2, s(Blocks.LANTERN).setValue(LanternBlock.HANGING, true));
		}
		// Upper storeys: a ladder up from the back corner.
		if (k.floors() > 1) {
			for (int dy = 1; dy <= storey; dy++) {
				f.set(w - 2, dy, 1, s(Blocks.LADDER).setValue(LadderBlock.FACING, Direction.SOUTH));
			}
			f.set(w - 2, storey, 1, s(Blocks.LADDER).setValue(LadderBlock.FACING, Direction.SOUTH));
		}
		// Door, step and porch lantern.
		int du = (w - 1) / 2;
		door(f, du, 1, d - 1, st.door());
		f.set(du, 0, d, stair(Blocks.STONE_BRICK_STAIRS, Direction.NORTH, false));
		f.set(du - 1, 0, d, Kit.slab(Blocks.STONE_BRICK_SLAB, false));
		f.set(du + 1, 0, d, Kit.slab(Blocks.STONE_BRICK_SLAB, false));
		f.set(du + 1, 3, d, s(Blocks.LANTERN).setValue(LanternBlock.HANGING, true));
		f.set(du + 1, 4, d, stair(st.roof(), Direction.NORTH, true));
		f.set(du, 4, d, stair(st.roof(), Direction.NORTH, true));
		f.set(du - 1, 4, d, stair(st.roof(), Direction.NORTH, true));
		roof(f, k, top + 1);
		chimney(f, k, top);
		furnish(f, k);
	}

	private static void door(Frame f, int u, int dy, int v, Block door) {
		BlockState base = s(door).setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH)
				.setValue(BlockStateProperties.DOOR_HINGE, net.minecraft.world.level.block.state.properties.DoorHingeSide.LEFT);
		f.set(u, dy, v, base.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER));
		f.set(u, dy + 1, v, base.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER));
		f.set(u, dy + 2, v, log(Blocks.STRIPPED_SPRUCE_LOG, Direction.Axis.X));
	}

	/** Two-high windows with open shutters on the front and back, single windows on the sides. */
	private static void windows(Frame f, Cottage k, int base) {
		int w = k.w(), d = k.d();
		int du = (w - 1) / 2;
		List<Integer> front = new ArrayList<>();
		for (int u : new int[]{du - 2, du + 2}) {
			if (u >= 2 && u <= w - 3) {
				front.add(u);
			}
		}
		if (w >= 10) {
			front.add(1 + 0);
		}
		for (int u : front) {
			if (u == du || u <= 0 || u >= w - 1) {
				continue;
			}
			for (int v : new int[]{0, d - 1}) {
				if (base > 0 || v == 0 || Math.abs(u - du) > 1) {
					f.set(u, base + 2, v, Kit.connected(Blocks.GLASS_PANE, true));
					f.set(u, base + 3, v, Kit.connected(Blocks.GLASS_PANE, true));
					int out = v == 0 ? -1 : d;
					Direction outward = v == 0 ? Direction.NORTH : Direction.SOUTH;
					for (int side : new int[]{-1, 1}) {
						int su = u + side;
						if (su > 0 && su < w - 1 && su != du && !front.contains(su)) {
							BlockState shutter = s(Blocks.SPRUCE_TRAPDOOR).setValue(TrapDoorBlock.FACING, outward)
									.setValue(TrapDoorBlock.OPEN, true).setValue(TrapDoorBlock.HALF, Half.BOTTOM);
							f.set(su, base + 2, out, shutter);
							f.set(su, base + 3, out, shutter);
						}
					}
					// Window box below the front windows.
					if (v == d - 1 && base == 0) {
						f.set(u, base + 1, out, s(Blocks.SPRUCE_TRAPDOOR).setValue(TrapDoorBlock.FACING, Direction.SOUTH)
								.setValue(TrapDoorBlock.HALF, Half.TOP));
					}
				}
			}
		}
		for (int v : d >= 9 ? new int[]{2, d - 3} : new int[]{(d - 1) / 2}) {
			for (int u : new int[]{0, w - 1}) {
				f.set(u, base + 2, v, Kit.connected(Blocks.GLASS_PANE, false));
				f.set(u, base + 3, v, Kit.connected(Blocks.GLASS_PANE, false));
			}
		}
	}

	/**
	 * Gable roof starting at {@code dy}: the ridge runs along the front (u) unless {@code ridgeAcross}, overhanging one
	 * block all round, gable ends filled and given a small window, ridge capped with slabs.
	 */
	private static void roof(Frame f, Cottage k, int dy) {
		int w = k.w(), d = k.d();
		Style st = k.style();
		boolean alongU = !k.ridgeAcross();
		int span = alongU ? d + 2 : w + 2;      // overhang included
		int lo = -1, hi = alongU ? d : w;
		for (int i = 0; i <= span / 2; i++) {
			int a = lo + i, b = hi - i;
			int yy = dy + i;
			if (a > b) {
				break;
			}
			if (a == b) {
				for (int t = -1; t <= (alongU ? w : d); t++) {
					set(f, alongU, t, yy, a, Kit.slab(st.roofSlab(), false));
				}
				break;
			}
			for (int t = -1; t <= (alongU ? w : d); t++) {
				set(f, alongU, t, yy, a, stair(st.roof(), alongU ? Direction.SOUTH : Direction.EAST, false));
				set(f, alongU, t, yy, b, stair(st.roof(), alongU ? Direction.NORTH : Direction.WEST, false));
				if (b == a + 1) {
					set(f, alongU, t, yy + 1, a, Kit.slab(st.roofSlab(), false));
					set(f, alongU, t, yy + 1, b, Kit.slab(st.roofSlab(), false));
				}
			}
			// Gable ends (inside the overhang) and the attic between.
			for (int m = a + 1; m <= b - 1; m++) {
				for (int t = 0; t <= (alongU ? w - 1 : d - 1); t++) {
					boolean end = t == 0 || t == (alongU ? w - 1 : d - 1);
					set(f, alongU, t, yy, m, end ? s(st.gable()) : s(Blocks.AIR));
				}
			}
			// Underside of the eaves.
			if (i == 0) {
				for (int t = -1; t <= (alongU ? w : d); t++) {
					set(f, alongU, t, yy - 1, a, stair(st.roof(), alongU ? Direction.NORTH : Direction.WEST, true));
					set(f, alongU, t, yy - 1, b, stair(st.roof(), alongU ? Direction.SOUTH : Direction.EAST, true));
				}
			}
		}
		// Small round window high in each gable.
		int mid = alongU ? (d - 1) / 2 : (w - 1) / 2;
		for (int t : new int[]{0, alongU ? w - 1 : d - 1}) {
			set(f, alongU, t, dy + 2, mid, Kit.connected(Blocks.GLASS_PANE, !alongU));
		}
	}

	/** Roof helper: t runs along the ridge, m across it. */
	private static void set(Frame f, boolean alongU, int t, int dy, int m, BlockState state) {
		if (alongU) {
			f.set(t, dy, m, state);
		} else {
			f.set(m, dy, t, state.rotate(Rotation.NONE));
		}
	}

	/** Cobblestone chimney against one end wall, rising past the ridge, with a smoking campfire on top. */
	private static void chimney(Frame f, Cottage k, int top) {
		int w = k.w(), d = k.d();
		boolean alongU = !k.ridgeAcross();
		int u = alongU ? w : 1, v = alongU ? 1 : -1;
		if (k.use() == Use.SMITHY) {
			return; // the smithy's forge has its own stack
		}
		int peak = top + 1 + (alongU ? (d + 2) / 2 : (w + 2) / 2) + 1;
		f.fill(u, -1, v, u, peak, v, s(Blocks.COBBLESTONE));
		f.set(u, peak + 1, v, s(Blocks.CAMPFIRE).setValue(CampfireBlock.SIGNAL_FIRE, false));
	}

	// ---- interiors (local: back wall v = 1, front v = d - 2; the door is at the middle of the front) ----

	private static BlockState barrel() {
		return Canvas.facing(s(Blocks.BARREL), Direction.UP);
	}

	private static void bed(Frame f, int u, int v, Block bed) {
		BlockState b = s(bed).setValue(BedBlock.FACING, Direction.NORTH);
		f.set(u, 1, v, b.setValue(BedBlock.PART, BedPart.HEAD));
		f.set(u, 1, v + 1, b.setValue(BedBlock.PART, BedPart.FOOT));
	}

	private static void table(Frame f, int u, int dy, int v) {
		f.set(u, dy + 1, v, s(Blocks.OAK_FENCE));
		f.set(u, dy + 2, v, s(Blocks.SPRUCE_PRESSURE_PLATE));
	}

	private static void furnish(Frame f, Cottage k) {
		int w = k.w(), d = k.d();
		int r = w - 2, b = 1, fr = d - 2; // right column, back row, front row
		switch (k.use()) {
			case INN -> {
				// Bar along the back with barrels behind it, two tables with chairs, a rug.
				for (int u = 2; u <= w - 4; u++) {
					f.set(u, 1, b + 1, stair(Blocks.SPRUCE_STAIRS, Direction.NORTH, true));
					f.set(u, 1, b, u % 2 == 0 ? barrel() : s(Blocks.BOOKSHELF));
				}
				f.set(2, 2, b + 1, s(Blocks.LANTERN));
				table(f, 2, 0, fr - 1);
				f.set(1, 1, fr - 1, stair(Blocks.SPRUCE_STAIRS, Direction.WEST, false));
				f.set(3, 1, fr - 1, stair(Blocks.SPRUCE_STAIRS, Direction.EAST, false));
				table(f, w - 3, 0, fr - 1);
				f.set(w - 4, 1, fr - 1, stair(Blocks.SPRUCE_STAIRS, Direction.WEST, false));
				f.fill(4, 0, fr - 1, w - 5, 0, fr, s(Blocks.RED_WOOL));
				f.fill(4, 1, fr - 1, w - 5, 1, fr - 1, s(Blocks.AIR));
				// Upstairs: three beds, a wardrobe and a rug.
				for (int i = 0; i < 3; i++) {
					bed2(f, 1 + i * 2, 6, b, i == 0 ? Blocks.RED_BED : i == 1 ? Blocks.BLUE_BED : Blocks.GREEN_BED);
				}
				f.set(r, 6, fr, barrel());
				f.set(r - 1, 6, fr, barrel());
				f.fill(2, 5, fr - 1, 5, 5, fr, s(Blocks.WHITE_WOOL));
				f.set(1, 6, fr, s(Blocks.POTTED_RED_TULIP));
			}
			case SMITHY -> {
				f.set(1, 1, b, s(Blocks.ANVIL));
				f.set(2, 1, b, s(Blocks.SMITHING_TABLE));
				f.set(3, 1, b, s(Blocks.GRINDSTONE).setValue(BlockStateProperties.ATTACH_FACE,
						net.minecraft.world.level.block.state.properties.AttachFace.FLOOR));
				f.set(r, 1, b, s(Blocks.BLAST_FURNACE).setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH));
				f.set(r - 1, 1, b, s(Blocks.FURNACE).setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH));
				f.set(r, 1, fr, barrel());
				f.set(1, 1, fr, barrel());
				f.set(r, 2, b + 1, s(Blocks.LANTERN));
				// Weapon rack: fences with the swords' tips (iron bars) on the side wall.
				f.set(1, 2, 2, s(Blocks.SPRUCE_FENCE));
				f.set(1, 3, 2, s(Blocks.IRON_BARS));
				f.set(1, 2, 3, s(Blocks.SPRUCE_FENCE));
				f.set(1, 3, 3, s(Blocks.IRON_BARS));
				forge(f, k);
			}
			case BAKERY -> {
				f.set(1, 1, b, s(Blocks.SMOKER).setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH));
				f.set(2, 1, b, s(Blocks.SMOKER).setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH));
				f.set(3, 1, b, s(Blocks.FURNACE).setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH));
				f.set(r, 1, b, s(Blocks.HAY_BLOCK));
				f.set(r, 2, b, s(Blocks.HAY_BLOCK));
				f.set(r, 1, b + 1, barrel());
				f.set(r, 1, fr, s(Blocks.CRAFTING_TABLE));
				table(f, 2, 0, fr - 1);
				f.set(1, 1, fr - 1, stair(Blocks.SPRUCE_STAIRS, Direction.WEST, false));
				f.set(1, 1, fr, s(Blocks.COMPOSTER));
				f.set(3, 1, b + 2, s(Blocks.CAKE));
			}
			case FARMHOUSE -> {
				bed(f, 1, b, Blocks.YELLOW_BED);
				f.set(r, 1, b, s(Blocks.CRAFTING_TABLE));
				f.set(r - 1, 1, b, barrel());
				f.set(r, 1, fr, s(Blocks.COMPOSTER));
				f.set(r, 1, fr - 1, s(Blocks.HAY_BLOCK));
				table(f, 2, 0, fr - 1);
				f.set(3, 1, fr - 1, stair(Blocks.OAK_STAIRS, Direction.EAST, false));
				f.set(1, 1, fr, s(Blocks.POTTED_DANDELION));
				f.fill(2, 0, b + 2, r - 1, 0, b + 3, s(Blocks.YELLOW_WOOL));
			}
			case WEAVER -> {
				bed(f, 1, b, Blocks.LIGHT_BLUE_BED);
				f.set(r, 1, b, s(Blocks.LOOM).setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH));
				f.set(r - 1, 1, b, s(Blocks.WHITE_WOOL));
				f.set(r - 1, 2, b, s(Blocks.LIGHT_BLUE_WOOL));
				f.set(r, 1, fr, barrel());
				table(f, 3, 0, fr - 1);
				f.set(3, 1, fr, stair(Blocks.BIRCH_STAIRS, Direction.NORTH, false));
				f.set(1, 1, fr, s(Blocks.POTTED_BLUE_ORCHID));
			}
			case ELDER -> {
				f.set(1, 1, b, s(Blocks.BOOKSHELF));
				f.set(1, 2, b, s(Blocks.BOOKSHELF));
				f.set(2, 1, b, s(Blocks.BOOKSHELF));
				f.set(r, 1, b, s(Blocks.LECTERN).setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH));
				bed(f, r, b + 2, Blocks.PURPLE_BED);
				table(f, 3, 0, fr - 1);
				f.set(4, 1, fr - 1, stair(Blocks.SPRUCE_STAIRS, Direction.EAST, false));
				f.set(1, 1, fr, s(Blocks.POTTED_FERN));
				f.fill(2, 0, b + 1, 4, 0, b + 2, s(Blocks.PURPLE_WOOL));
			}
			case FISHER -> {
				bed(f, 1, b, Blocks.CYAN_BED);
				f.set(r, 1, b, s(Blocks.BARREL));
				f.set(r, 2, b, s(Blocks.BARREL));
				f.set(r, 1, fr, s(Blocks.CAULDRON));
				f.set(r - 1, 1, b, s(Blocks.CRAFTING_TABLE));
				table(f, 2, 0, fr - 1);
				f.set(1, 1, fr, s(Blocks.POTTED_OAK_SAPLING));
			}
		}
	}

	private static void bed2(Frame f, int u, int dy, int v, Block bed) {
		BlockState b = s(bed).setValue(BedBlock.FACING, Direction.NORTH);
		f.set(u, dy, v, b.setValue(BedBlock.PART, BedPart.HEAD));
		f.set(u, dy, v + 1, b.setValue(BedBlock.PART, BedPart.FOOT));
	}

	/** The smithy's open forge on its right side: a stone hearth with a fire, a stack, and a slab lean-to on posts. */
	private static void forge(Frame f, Cottage k) {
		int w = k.w(), d = k.d();
		int u0 = w, u1 = w + 3;
		f.fill(u0, 0, 0, u1, 0, d - 2, s(Blocks.COBBLESTONE));
		f.fill(u0, 1, 0, u1, 4, d - 2, s(Blocks.AIR));
		// Hearth against the back.
		f.fill(u0, 1, 0, u0 + 2, 1, 1, s(Blocks.STONE_BRICKS));
		f.set(u0 + 1, 1, 1, s(Blocks.CAMPFIRE).setValue(CampfireBlock.SIGNAL_FIRE, false));
		f.set(u0 + 1, 2, 1, s(Blocks.IRON_BARS));
		f.fill(u0, 2, 0, u0 + 2, 2, 0, s(Blocks.STONE_BRICKS));
		f.fill(u0 + 1, 3, 0, u0 + 1, 9, 0, s(Blocks.STONE_BRICKS));
		f.set(u0 + 1, 10, 0, s(Blocks.CAMPFIRE).setValue(CampfireBlock.SIGNAL_FIRE, false));
		f.set(u0 + 2, 1, 3, s(Blocks.ANVIL));
		f.set(u0 + 2, 1, 4, s(Blocks.WATER_CAULDRON).setValue(BlockStateProperties.LEVEL_CAULDRON, 3));
		// Posts and a slab lean-to.
		for (int v : new int[]{0, d - 2}) {
			f.fill(u1, 1, v, u1, 3, v, s(Blocks.SPRUCE_FENCE));
		}
		f.fill(u0, 4, 0, u1, 4, d - 2, Kit.slab(Blocks.SPRUCE_SLAB, false));
		f.fill(u0 + 1, 3, 0, u0 + 1, 3, 0, s(Blocks.STONE_BRICKS));
	}

	// ---------------------------------------------------------------------------------------------------- windmills

	/**
	 * A windmill: a round stone base with a door and slit windows, a tapering timber tower, a conical cap, and four
	 * sails on a hub facing {@code sails}. Inside: millstones, sacks (hay) and barrels, and a ladder to a loft.
	 */
	private static void windmill(Canvas c, int cx, int cz, int y, Direction sails, int baseR, int baseH, int upperH, int arm, Direction door) {
		c.foundation(cx - baseR - 1, cz - baseR - 1, cx + baseR + 1, cz + baseR + 1, y - 1, s(Blocks.COBBLESTONE));
		// Low stone apron around the base.
		for (int dx = -baseR - 1; dx <= baseR + 1; dx++) {
			for (int dz = -baseR - 1; dz <= baseR + 1; dz++) {
				double dd = Math.sqrt(dx * dx + dz * dz);
				if (dd <= baseR + 1.4 && dd > baseR + 0.4) {
					c.set(cx + dx, y - 1, cz + dz, unit(dx, dz, 21) < 0.3 ? s(Blocks.MOSSY_COBBLESTONE) : s(Blocks.STONE_BRICKS));
				}
			}
		}
		int upperR = baseR - 1;
		int topY = y + baseH + upperH;
		for (int dx = -baseR; dx <= baseR; dx++) {
			for (int dz = -baseR; dz <= baseR; dz++) {
				double dd = Math.sqrt(dx * dx + dz * dz);
				if (dd > baseR + 0.4) {
					continue;
				}
				boolean shell = dd > baseR - 0.6;
				c.set(cx + dx, y, cz + dz, s(Blocks.SPRUCE_PLANKS));
				for (int yy = y + 1; yy <= y + baseH; yy++) {
					BlockState wall = (yy - y) % 3 == 0 ? s(Blocks.STONE_BRICKS) : unit(dx + yy, dz, 13) < 0.2
							? s(Blocks.MOSSY_COBBLESTONE) : s(Blocks.COBBLESTONE);
					c.set(cx + dx, yy, cz + dz, shell ? wall : s(Blocks.AIR));
				}
				if (dd <= upperR + 0.4) {
					boolean ushell = dd > upperR - 0.6;
					c.set(cx + dx, y + baseH, cz + dz, ushell ? s(Blocks.STRIPPED_SPRUCE_LOG) : s(Blocks.SPRUCE_PLANKS));
					for (int yy = y + baseH + 1; yy <= topY; yy++) {
						boolean rib = ushell && (dx == 0 || dz == 0 || Math.abs(dx) == Math.abs(dz));
						c.set(cx + dx, yy, cz + dz, ushell ? (rib ? s(Blocks.SPRUCE_LOG) : s(Blocks.SPRUCE_PLANKS)) : s(Blocks.AIR));
					}
				} else if (dd > upperR + 0.4) {
					// Ledge where the timber tower steps in.
					c.set(cx + dx, y + baseH + 1, cz + dz, Kit.slab(Blocks.STONE_BRICK_SLAB, false));
				}
			}
		}
		// Windows: slits in the stone, glass in the timber.
		for (Direction dir : Direction.Plane.HORIZONTAL) {
			if (dir == door) {
				continue;
			}
			c.set(cx + dir.getStepX() * baseR, y + 3, cz + dir.getStepZ() * baseR, Kit.connected(Blocks.GLASS_PANE, dir.getAxis() == Direction.Axis.Z));
			if (dir != sails) {
				c.set(cx + dir.getStepX() * upperR, y + baseH + 3, cz + dir.getStepZ() * upperR,
						Kit.connected(Blocks.GLASS_PANE, dir.getAxis() == Direction.Axis.Z));
			}
		}
		// Door with a stone surround and a lantern.
		int ddx = door.getStepX() * baseR, ddz = door.getStepZ() * baseR;
		BlockState dl = s(Blocks.SPRUCE_DOOR).setValue(BlockStateProperties.HORIZONTAL_FACING, door.getOpposite())
				.setValue(BlockStateProperties.DOOR_HINGE, net.minecraft.world.level.block.state.properties.DoorHingeSide.LEFT);
		c.set(cx + ddx, y + 1, cz + ddz, dl.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER));
		c.set(cx + ddx, y + 2, cz + ddz, dl.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER));
		c.set(cx + ddx, y + 3, cz + ddz, s(Blocks.STONE_BRICKS));
		c.set(cx + ddx + door.getStepX(), y, cz + ddz + door.getStepZ(), Kit.stairs(Blocks.STONE_BRICK_STAIRS, door.getOpposite(), false));
		Direction side = door.getClockWise();
		c.set(cx + ddx + door.getStepX() + side.getStepX(), y + 3, cz + ddz + door.getStepZ() + side.getStepZ(),
				s(Blocks.LANTERN).setValue(LanternBlock.HANGING, false));
		c.set(cx + ddx + door.getStepX() + side.getStepX(), y + 2, cz + ddz + door.getStepZ() + side.getStepZ(), s(Blocks.SPRUCE_FENCE));
		c.set(cx + ddx + door.getStepX() + side.getStepX(), y + 1, cz + ddz + door.getStepZ() + side.getStepZ(), s(Blocks.SPRUCE_FENCE));
		// Conical cap.
		for (int i = 0; i <= upperR + 1; i++) {
			int r = upperR + 1 - i;
			for (int dx = -r; dx <= r; dx++) {
				for (int dz = -r; dz <= r; dz++) {
					double dd = Math.sqrt(dx * dx + dz * dz);
					if (dd <= r + 0.4 && dd > r - 1.0) {
						Direction out = Math.abs(dx) >= Math.abs(dz) ? (dx > 0 ? Direction.WEST : Direction.EAST) : (dz > 0 ? Direction.NORTH : Direction.SOUTH);
						c.set(cx + dx, topY + 1 + i, cz + dz, r == 0 ? s(Blocks.DARK_OAK_LOG) : Kit.stairs(Blocks.DARK_OAK_STAIRS, out, false));
					} else if (dd <= r - 1.0) {
						c.set(cx + dx, topY + 1 + i, cz + dz, s(Blocks.DARK_OAK_PLANKS));
					}
				}
			}
		}
		c.set(cx, topY + upperR + 3, cz, s(Blocks.LIGHTNING_ROD));
		// Inside: millstone, sacks, barrels, a ladder to the loft (at the back wall) and a loft floor.
		Direction back = door.getOpposite();
		int lx = cx + back.getStepX() * (baseR - 1), lz = cz + back.getStepZ() * (baseR - 1);
		for (int yy = y + 1; yy <= y + baseH; yy++) {
			c.set(lx, yy, lz, s(Blocks.LADDER).setValue(LadderBlock.FACING, door));
		}
		c.set(cx, y + 1, cz, s(Blocks.SMOOTH_STONE));
		c.set(cx, y + 2, cz, Kit.slab(Blocks.SMOOTH_STONE_SLAB, false));
		c.fill(cx, y + 3, cz, cx, y + baseH - 1, cz, s(Blocks.STRIPPED_SPRUCE_LOG));
		c.set(cx + side.getStepX() * 2, y + 1, cz + side.getStepZ() * 2, s(Blocks.HAY_BLOCK));
		c.set(cx + side.getStepX() * 2 + back.getStepX(), y + 1, cz + side.getStepZ() * 2 + back.getStepZ(), s(Blocks.HAY_BLOCK));
		c.set(cx - side.getStepX() * 2, y + 1, cz - side.getStepZ() * 2, barrel());
		c.set(cx - side.getStepX() * 2 + back.getStepX(), y + 1, cz - side.getStepZ() * 2 + back.getStepZ(), barrel());
		c.set(cx - side.getStepX() * 2, y + 2, cz - side.getStepZ() * 2, s(Blocks.LANTERN));
		// Loft (the plank floor at y + baseH, with an opening for the ladder) holds a grindstone and sacks.
		c.set(lx, y + baseH, lz, s(Blocks.AIR));
		c.set(cx + side.getStepX(), y + baseH + 1, cz + side.getStepZ(), s(Blocks.GRINDSTONE).setValue(BlockStateProperties.ATTACH_FACE,
				net.minecraft.world.level.block.state.properties.AttachFace.FLOOR));
		c.set(cx - side.getStepX(), y + baseH + 1, cz - side.getStepZ(), s(Blocks.HAY_BLOCK));
		// Sails: a hub on an axle out of the sails side, four arms with white sails.
		int hubY = topY - 1;
		int hx = cx + sails.getStepX() * (upperR + 2), hz = cz + sails.getStepZ() * (upperR + 2);
		c.set(cx + sails.getStepX() * (upperR + 1), hubY, cz + sails.getStepZ() * (upperR + 1),
				log(Blocks.STRIPPED_SPRUCE_LOG, sails.getAxis()));
		c.set(hx, hubY, hz, log(Blocks.DARK_OAK_LOG, sails.getAxis()));
		Direction across = sails.getClockWise();
		// Four arms (stocks) of stripped logs; each carries a solid cloth sail two blocks wide on its trailing side,
		// with a timber batten at the tip, so the blades read as a pinwheel from every side.
		for (int i = 1; i <= arm; i++) {
			for (int sgn : new int[]{1, -1}) {
				int vy = hubY + sgn * i;
				c.set(hx, vy, hz, log(Blocks.STRIPPED_SPRUCE_LOG, Direction.Axis.Y));
				int ax = hx + across.getStepX() * sgn * i, az = hz + across.getStepZ() * sgn * i;
				c.set(ax, hubY, az, log(Blocks.STRIPPED_SPRUCE_LOG, across.getAxis()));
				if (i < 2) {
					continue;
				}
				BlockState cloth = i == arm ? s(Blocks.SPRUCE_PLANKS) : s(Blocks.WHITE_WOOL);
				for (int k = 1; k <= 2; k++) {
					c.set(hx + across.getStepX() * sgn * k, vy, hz + across.getStepZ() * sgn * k, cloth);
					c.set(ax, hubY - sgn * k, az, cloth);
				}
			}
		}
	}

	/** Wheat beside the east windmill, fenced, with a water channel and a few hay bales. */
	private void wheatField(Canvas c) {
		int x1 = EMILL_X - 14, x2 = EMILL_X - 6, z1 = EMILL_Z - 7, z2 = EMILL_Z + 5;
		int y = c.ground(EMILL_X - 10, EMILL_Z);
		for (int x = x1; x <= x2; x++) {
			for (int z = z1; z <= z2; z++) {
				boolean edge = x == x1 || x == x2 || z == z1 || z == z2;
				int g = c.ground(x, z);
				if (edge) {
					c.set(x, g + 1, z, Kit.connected(Blocks.OAK_FENCE, z == z1 || z == z2));
					continue;
				}
				if (x == (x1 + x2) / 2) {
					c.set(x, g, z, s(Blocks.WATER));
					c.set(x, g + 1, z, s(Blocks.AIR));
				} else {
					c.set(x, g, z, s(Blocks.FARMLAND).setValue(BlockStateProperties.MOISTURE, 7));
					c.set(x, g + 1, z, s(Blocks.WHEAT).setValue(BlockStateProperties.AGE_7, 7));
				}
			}
		}
		int gate = (x1 + x2) / 2 + 2;
		c.set(gate, c.ground(gate, z2) + 1, z2, s(Blocks.OAK_FENCE_GATE).setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH));
		c.set(x2 + 2, c.ground(x2 + 2, z2) + 1, z2, s(Blocks.HAY_BLOCK));
		c.set(x2 + 2, c.ground(x2 + 2, z2 - 1) + 1, z2 - 1, s(Blocks.HAY_BLOCK));
		c.set(x2 + 2, c.ground(x2 + 2, z2) + 2, z2, s(Blocks.HAY_BLOCK));
	}

	// ---------------------------------------------------------------------------------------------------- east side

	/** The quest giver's notice board: two log posts, a plank board with a slab roof, a lantern, crates. */
	private static void noticeBoard(Canvas c) {
		if (!c.near(QUEST_X, QUEST_Z, 8)) {
			return;
		}
		int bx = QUEST_X + 2, g = G;
		for (int z = QUEST_Z - 2; z <= QUEST_Z + 2; z++) {
			boolean post = Math.abs(z - QUEST_Z) == 2;
			if (post) {
				c.fill(bx, g + 1, z, bx, g + 4, z, s(Blocks.SPRUCE_LOG));
			} else {
				c.set(bx, g + 2, z, s(Blocks.SPRUCE_PLANKS));
				c.set(bx, g + 3, z, s(Blocks.SPRUCE_PLANKS));
				c.set(bx - 1, g + 2, z, s(Blocks.SPRUCE_TRAPDOOR).setValue(TrapDoorBlock.FACING, Direction.WEST)
						.setValue(TrapDoorBlock.OPEN, true));
			}
			c.set(bx, g + 5, z, Kit.slab(Blocks.SPRUCE_SLAB, false));
			c.set(bx - 1, g + 5, z, Kit.stairs(Blocks.SPRUCE_STAIRS, Direction.EAST, false));
			c.set(bx + 1, g + 5, z, Kit.stairs(Blocks.SPRUCE_STAIRS, Direction.WEST, false));
		}
		c.set(bx - 1, g + 4, QUEST_Z, s(Blocks.LANTERN).setValue(LanternBlock.HANGING, true));
		c.set(bx + 1, g + 1, QUEST_Z - 3, Canvas.facing(s(Blocks.BARREL), Direction.UP));
		c.set(bx + 1, g + 1, QUEST_Z + 3, s(Blocks.HAY_BLOCK));
		c.set(bx, g + 1, QUEST_Z + 3, Canvas.facing(s(Blocks.BARREL), Direction.UP));
		// A gravel apron where players talk to the quest giver.
		for (int x = QUEST_X - 2; x <= QUEST_X + 1; x++) {
			for (int z = QUEST_Z - 2; z <= QUEST_Z + 2; z++) {
				if (Math.abs(x - QUEST_X) + Math.abs(z - QUEST_Z) <= 3 && c.terrain().paving(x, z) == null) {
					c.set(x, g, z, unit(x, z, 31) < 0.5 ? s(Blocks.GRAVEL) : s(Blocks.COARSE_DIRT));
				}
			}
		}
	}

	/** Combat field A: an open training meadow ringed by a broken fence, two straw dummies at its edge. */
	private static void fieldA(Canvas c) {
		if (!c.near(FIELD_A_X, FIELD_A_Z, FIELD_R + 4)) {
			return;
		}
		for (int i = 0; i < 48; i++) {
			double a = i * Math.PI * 2 / 48;
			int x = FIELD_A_X + (int) Math.round(Math.cos(a) * (FIELD_R + 1));
			int z = FIELD_A_Z + (int) Math.round(Math.sin(a) * (FIELD_R + 1));
			if (c.terrain().paving(x, z) != null || (i % 12) >= 8) {
				continue; // gaps for the road and every so often
			}
			c.set(x, c.ground(x, z) + 1, z, s(Blocks.OAK_FENCE));
		}
		for (int[] p : new int[][]{{FIELD_A_X - 6, FIELD_A_Z - 6}, {FIELD_A_X + 6, FIELD_A_Z + 5}}) {
			int g = c.ground(p[0], p[1]);
			c.set(p[0], g + 1, p[1], s(Blocks.OAK_FENCE));
			c.set(p[0], g + 2, p[1], s(Blocks.HAY_BLOCK));
			c.set(p[0], g + 3, p[1], s(Blocks.CARVED_PUMPKIN).setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST));
		}
		int g = c.ground(FIELD_A_X + 8, FIELD_A_Z - 3);
		c.set(FIELD_A_X + 8, g + 1, FIELD_A_Z - 3, log(Blocks.OAK_LOG, Direction.Axis.Z));
		c.set(FIELD_A_X + 8, g + 1, FIELD_A_Z - 2, log(Blocks.OAK_LOG, Direction.Axis.Z));
	}

	/** Combat field B: a rough bandit camp on a low rise, patchy earth, mossy boulders, a tent, a fire and a wagon. */
	private static void fieldB(Canvas c) {
		if (!c.near(FIELD_B_X, FIELD_B_Z, FIELD_R + 6)) {
			return;
		}
		for (int x = FIELD_B_X - FIELD_R; x <= FIELD_B_X + FIELD_R; x++) {
			for (int z = FIELD_B_Z - FIELD_R; z <= FIELD_B_Z + FIELD_R; z++) {
				if (!near(x, z, FIELD_B_X, FIELD_B_Z, FIELD_R) || c.terrain().paving(x, z) != null) {
					continue;
				}
				double r = unit(x, z, 41);
				double patch = Math.sin(x * 0.5) + Math.cos(z * 0.45);
				int g = c.ground(x, z);
				if (patch > 0.6) {
					c.set(x, g, z, r < 0.5 ? s(Blocks.COARSE_DIRT) : s(Blocks.ROOTED_DIRT));
				} else if (patch < -1.2) {
					c.set(x, g, z, s(Blocks.GRAVEL));
				}
			}
		}
		// Boulders at the edge.
		for (int[] p : new int[][]{{FIELD_B_X - 8, FIELD_B_Z - 3}, {FIELD_B_X + 7, FIELD_B_Z + 6}, {FIELD_B_X + 2, FIELD_B_Z - 9}}) {
			int g = c.ground(p[0], p[1]);
			c.set(p[0], g + 1, p[1], s(Blocks.MOSSY_COBBLESTONE));
			c.set(p[0] + 1, g + 1, p[1], s(Blocks.COBBLESTONE));
			c.set(p[0], g + 1, p[1] + 1, s(Blocks.MOSSY_COBBLESTONE));
			c.set(p[0], g + 2, p[1], s(Blocks.MOSSY_COBBLESTONE_SLAB));
		}
		// Bandit tent, fire and stores at the far (east) side.
		int tx = FIELD_B_X + 6, tz = FIELD_B_Z - 4;
		Kit.tent(c, tx, tz, c.ground(tx, tz), Blocks.BROWN_WOOL);
		int g = c.ground(FIELD_B_X + 3, FIELD_B_Z + 1);
		c.set(FIELD_B_X + 3, g + 1, FIELD_B_Z + 1, s(Blocks.CAMPFIRE).setValue(CampfireBlock.SIGNAL_FIRE, false));
		c.set(FIELD_B_X + 2, g + 1, FIELD_B_Z + 1, log(Blocks.OAK_LOG, Direction.Axis.Z));
		c.set(FIELD_B_X + 4, g + 1, FIELD_B_Z + 2, log(Blocks.OAK_LOG, Direction.Axis.X));
		c.set(FIELD_B_X + 8, c.ground(FIELD_B_X + 8, FIELD_B_Z) + 1, FIELD_B_Z, Canvas.facing(s(Blocks.BARREL), Direction.UP));
		c.set(FIELD_B_X + 8, c.ground(FIELD_B_X + 8, FIELD_B_Z + 1) + 1, FIELD_B_Z + 1, Canvas.facing(s(Blocks.BARREL), Direction.EAST));
		// A broken wagon at the south edge.
		int wx = FIELD_B_X - 3, wz = FIELD_B_Z + 8, wg = c.ground(wx, wz);
		c.fill(wx, wg + 1, wz, wx + 3, wg + 1, wz, Kit.slab(Blocks.SPRUCE_SLAB, true));
		c.set(wx, wg + 1, wz + 1, log(Blocks.STRIPPED_OAK_LOG, Direction.Axis.Z));
		c.set(wx + 3, wg + 1, wz - 1, log(Blocks.STRIPPED_OAK_LOG, Direction.Axis.Z));
		c.set(wx + 1, wg + 2, wz, s(Blocks.HAY_BLOCK));
		c.set(wx + 2, wg + 2, wz, Canvas.facing(s(Blocks.BARREL), Direction.UP));
		c.set(wx + 4, wg + 1, wz, s(Blocks.SPRUCE_FENCE));
		// A dead bush or two.
		c.set(FIELD_B_X - 5, c.ground(FIELD_B_X - 5, FIELD_B_Z + 4) + 1, FIELD_B_Z + 4, s(Blocks.DEAD_BUSH));
		c.set(FIELD_B_X + 5, c.ground(FIELD_B_X + 5, FIELD_B_Z + 7) + 1, FIELD_B_Z + 7, s(Blocks.DEAD_BUSH));
	}
}
