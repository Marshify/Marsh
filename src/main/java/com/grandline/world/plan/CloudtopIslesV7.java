package com.grandline.world.plan;

import com.grandline.world.plan.Anchor.Type;
import com.grandline.world.plan.Terrain.Ground;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/**
 * Cloudtop Isles as rebuilt in layout 7 (kept for worlds made with layouts 7-11; newer worlds use {@link CloudtopIsles}): a sky archipelago climbing from the sea into the clouds, in two progression regions.
 * <pre>
 *  section               centre (local)   ground y   role
 *  Crescent Harbor       arc round (0, 30)   66      arrival (lagoon pier), fisherman, two tall rocks, lake + wharf
 *  Cloud Temple island   (-62, -26)          92      big temple; lower camp (basic) and lower boss spot in front
 *  Forest island         (-40, -74)          106     six trees, a flat-sided rock; lower camp (stronger)
 *  Sky Village           (12, -60)           124/127 six houses, plaza, lower quest giver, innkeeper; waterfall to the lake
 *  Sanctuary entrance    (60, -100)          142     weathered temple with the cloud portal up to the sanctuary
 *  Upper plateau A       (-10, -40)          218     settlement, upper quest giver, return doorway, the root bridge
 *  Upper plateau B       (88, -50)           226     the great bell, the ancient temple, the upper boss arena, upper camps
 * </pre>
 * Every floating landmass sits on a thick, rounded cloud base. The lower chain is walkable: stepped bridges (one block
 * up per block) with cloud landings. The sanctuary is reached through the entrance temple's cloud portal and left through
 * the return doorway (see {@link com.grandline.world.CloudtopPortals}). Island-local coordinates: +x east, +z south.
 * Built from code, identical in every world; older worlds keep the original Cloudtop ({@link FirstSeaPlans} legacy).
 */
final class CloudtopIslesV7 extends BasePlan {
	static final int SEA_G = 66;
	// Lower chain.
	static final int TEMPLE_X = -62, TEMPLE_Z = -26, TEMPLE_Y = 92, TEMPLE_RX = 22, TEMPLE_RZ = 18;
	static final int FOREST_X = -40, FOREST_Z = -74, FOREST_Y = 106, FOREST_RX = 14, FOREST_RZ = 12;
	static final int VILLAGE_X = 12, VILLAGE_Z = -60, VILLAGE_Y = 124, VILLAGE_RX = 30, VILLAGE_RZ = 24;
	static final int GATE_X = 60, GATE_Z = -100, GATE_Y = 142, GATE_RX = 14, GATE_RZ = 12;
	// Upper sanctuary.
	static final int PA_X = -10, PA_Z = -40, PA_Y = 218, PA_RX = 34, PA_RZ = 28;
	static final int PB_X = 88, PB_Z = -50, PB_Y = 226, PB_RX = 50, PB_RZ = 40;
	static final int LAKE_X = 16, LAKE_Z = -31;
	/** The waterfall falls from the village's south edge at this column into the lake. */
	static final int FALL_X = 16, FALL_Z = -36;

	/** Portal spots (island-local): the entrance temple's cloud shaft and the sanctuary's return doorway. */
	public static final int PORTAL_UP_X = GATE_X, PORTAL_UP_Y = GATE_Y + 1, PORTAL_UP_Z = GATE_Z - 3;
	public static final int ARRIVE_UP_X = PA_X - 14, ARRIVE_UP_Y = PA_Y + 1, ARRIVE_UP_Z = PA_Z + 6;
	public static final int PORTAL_DOWN_X = PA_X - 20, PORTAL_DOWN_Y = PA_Y + 1, PORTAL_DOWN_Z = PA_Z + 6;
	public static final int ARRIVE_DOWN_X = GATE_X, ARRIVE_DOWN_Y = GATE_Y + 1, ARRIVE_DOWN_Z = GATE_Z + 7;

	/** Village houses: corner, size, floors, door side, roof block, wall block. */
	record House(int x, int z, int w, int d, Direction front, Block roof, Block wall, int kind) {
	}

	static final House[] HOUSES = {
			new House(-6, -76, 7, 6, Direction.SOUTH, Blocks.SPRUCE_STAIRS, Blocks.SPRUCE_PLANKS, 0),
			new House(4, -80, 6, 6, Direction.SOUTH, Blocks.DARK_OAK_STAIRS, Blocks.CALCITE, 1),
			new House(24, -78, 8, 6, Direction.SOUTH, Blocks.SPRUCE_STAIRS, Blocks.BIRCH_PLANKS, 2),
			new House(-12, -56, 6, 7, Direction.EAST, Blocks.DARK_OAK_STAIRS, Blocks.CALCITE, 3),
			new House(30, -58, 7, 7, Direction.WEST, Blocks.SPRUCE_STAIRS, Blocks.SPRUCE_PLANKS, 4),
			new House(22, -46, 6, 5, Direction.NORTH, Blocks.DARK_OAK_STAIRS, Blocks.CALCITE, 5)};

	CloudtopIslesV7() {
		super("Cloudtop Isles", 0.05);
		// Crescent Harbor: overlapping hills along an arc open to the south, enclosing a sheltered lagoon.
		double[][] arc = {{-37, 23}, {-31, 8}, {-19, -3}, {-3, -8}, {13, -6}, {27, 3}, {36, 17}};
		for (double[] p : arc) {
			terrain.mass(p[0], p[1], 15, 12, 4.0, 3.0, Ground.GRASS);
		}
		terrain.mass(LAKE_X, LAKE_Z + 2, 14, 11, 4.2, 3.0, Ground.GRASS)       // the north lobe holding the lake
				.flat(LAKE_X, LAKE_Z, 6, 4, 64, 2, s(Blocks.WATER))           // the freshwater lake (fed by the waterfall)
				.flat(0, 4, 6, 3, SEA_G, 3, null)                             // the arrival landing
				.path(s(Blocks.DIRT_PATH), 3, 0, 4, -12, 6, -24, 10, -32, 14)
				.path(s(Blocks.DIRT_PATH), 2.4, 0, 4, 8, -6, LAKE_X, LAKE_Z + 6)
				.reach(175);
		// Lower region (Lv 150-190).
		anchor(new Anchor(Type.ARRIVAL, 0, Anchor.SURFACE, 3, 0, 180));
		anchor(Anchor.at(Type.FISHERMAN, 1, 65, 18, 180));
		anchor(Anchor.at(Type.DOCK, 5, 63, 17));
		anchor(Anchor.at(Type.QUEST, 10, VILLAGE_Y + 1, -60, 90));
		anchor(Anchor.at(Type.HOME, 6, VILLAGE_Y + 1, -64, 270));
		anchor(Anchor.at(Type.ENEMY_CAMP, TEMPLE_X, TEMPLE_Y, TEMPLE_Z + 5));
		anchor(Anchor.at(Type.ENEMY_CAMP, FOREST_X, FOREST_Y, FOREST_Z));
		anchor(Anchor.at(Type.BOSS_ARENA, TEMPLE_X, TEMPLE_Y, TEMPLE_Z + 2));
		// Upper region (Lv 450-625).
		anchor(Anchor.at(Type.QUEST, PA_X + 4, PA_Y + 1, PA_Z + 4, 90).upper());
		anchor(Anchor.at(Type.ENEMY_CAMP, PB_X - 24, PB_Y, PB_Z + 18).upper());
		anchor(Anchor.at(Type.ENEMY_CAMP, PB_X + 14, PB_Y, PB_Z + 22).upper());
		anchor(Anchor.at(Type.BOSS_ARENA, PB_X, PB_Y, PB_Z - 28).upper());
		anchor(Anchor.of(Type.TREASURE, -38, 30));
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

	static BlockState stair(Block b, Direction facing, boolean top) {
		return Kit.stairs(b, facing, top);
	}

	static BlockState lantern(boolean hanging) {
		return s(Blocks.LANTERN).setValue(LanternBlock.HANGING, hanging);
	}

	static BlockState aged(int x, int y, int z) {
		double u = unit(x * 5 + y, z, 9);
		return u < 0.14 ? s(Blocks.CRACKED_STONE_BRICKS) : u < 0.26 ? s(Blocks.MOSSY_STONE_BRICKS) : s(Blocks.STONE_BRICKS);
	}

	/** 0 at the centre, 1 at the (gently wobbling) edge of an elliptical landmass. */
	static double edge(int x, int z, int cx, int cz, int rx, int rz, int seed) {
		double dx = (x - cx) / (double) rx, dz = (z - cz) / (double) rz;
		double ang = Math.atan2(dz, dx);
		double wob = 1 + 0.07 * Math.sin(ang * 3 + seed) + 0.05 * Math.sin(ang * 5 + seed * 2.3);
		return Math.sqrt(dx * dx + dz * dz) / wob;
	}

	/** Surface height of a floating landmass at (x, z); the village has a raised east terrace. */
	interface Height {
		int at(int x, int z);
	}

	/**
	 * A floating landmass on a cloud: the land (top block, two of soil, then stone), resting on a thick rounded cloud whose
	 * depth swells toward the middle and whose rim bulges out past the land in soft lumps. White wool and snow on the
	 * outside, white concrete inside, light grey wool along the underside shadow. Solid all the way through.
	 */
	static void floating(Canvas c, int cx, int cz, int rx, int rz, Height height, Block top, Block soil, int cloudDepth, int seed) {
		for (int x = cx - rx - 5; x <= cx + rx + 5; x++) {
			for (int z = cz - rz - 5; z <= cz + rz + 5; z++) {
				if (!c.inChunk(x, z)) {
					continue;
				}
				double e = edge(x, z, cx, cz, rx, rz, seed);
				int y = height.at(x, z);
				double lump = unit(x / 3, z / 3, seed) * 0.6 + unit(x, z, seed + 1) * 0.4;
				if (e <= 1.0) {
					c.set(x, y, z, s(top));
					c.set(x, y - 1, z, s(soil));
					c.set(x, y - 2, z, s(soil));
					c.set(x, y - 3, z, s(Blocks.STONE));
					int depth = (int) Math.round(cloudDepth * Math.pow(Math.max(0, 1 - e * e), 0.55) + lump * 3) + 2;
					for (int k = 4; k <= 3 + depth; k++) {
						boolean shell = k >= 3 + depth - 1 || e > 0.82;
						BlockState b = shell ? (unit(x, z + k, seed + 2) < 0.5 ? s(Blocks.WHITE_WOOL) : s(Blocks.SNOW_BLOCK)) : s(Blocks.WHITE_CONCRETE);
						c.set(x, y - k, z, b);
					}
					// Invisible light under the cloud, so the underside reads as bright cloud rather than a black shadow.
					if (Math.floorMod(x, 4) == 0 && Math.floorMod(z, 4) == 0) {
						c.set(x, y - 4 - depth, z, s(Blocks.LIGHT).setValue(net.minecraft.world.level.block.LightBlock.LEVEL, 13));
					}
				} else if (e <= 1.22) {
					// The rim: rounded cloud lumps poking out under the land's edge.
					double t = (e - 1.0) / 0.22;
					if (lump > 0.25 + t * 0.6) {
						int base = height.at(x, z);
						int rimTop = base - 2 - (int) Math.round(t * 2);
						int rimBot = base - 4 - (int) Math.round((1 - t) * 4 * lump);
						for (int yy = rimBot; yy <= rimTop; yy++) {
							c.set(x, yy, z, unit(x, yy, seed + 3) < 0.6 ? s(Blocks.WHITE_WOOL) : s(Blocks.SNOW_BLOCK));
						}
					}
				}
			}
		}
	}

	/** A small round cloud pad (a bridge landing or a drifting fragment), {@code top} the walkable surface. */
	static void cloudPad(Canvas c, int cx, int cz, int r, int top, boolean walkable) {
		for (int x = cx - r - 1; x <= cx + r + 1; x++) {
			for (int z = cz - r - 1; z <= cz + r + 1; z++) {
				double d = Math.hypot(x - cx, z - cz) / (r + 0.4);
				if (d > 1 || !c.inChunk(x, z)) {
					continue;
				}
				int depth = 1 + (int) Math.round((1 - d * d) * (r * 0.9)) + (int) (unit(x, z, 77) * 2);
				for (int k = 0; k <= depth; k++) {
					BlockState b = unit(x, z + k, 78) < 0.55 ? s(Blocks.WHITE_WOOL) : s(Blocks.SNOW_BLOCK);
					c.set(x, top - k, z, b);
				}
				if (walkable && d < 0.8) {
					c.set(x, top, z, s(Blocks.SNOW_BLOCK));
				}
			}
		}
	}

	/**
	 * A stepped sky bridge between two points (y = the walkway block): 3 wide pale stone with stone brick wall rails,
	 * stairs where it climbs (at most one block up per block across), lanterns on posts every eight blocks, and small
	 * cloud puffs hanging under it.
	 */
	static void skyBridge(Canvas c, int x1, int y1, int z1, int x2, int y2, int z2) {
		int n = Math.max(Math.abs(x2 - x1), Math.abs(z2 - z1));
		if (n == 0) {
			return;
		}
		double ux = (x2 - x1) / (double) n, uz = (z2 - z1) / (double) n;
		Direction along = Math.abs(ux) >= Math.abs(uz) ? (ux > 0 ? Direction.EAST : Direction.WEST) : (uz > 0 ? Direction.SOUTH : Direction.NORTH);
		Direction side = along.getClockWise();
		// Pass 1: rails, lanterns and hanging cloud puffs. Pass 2: the walkway and its headroom, laid over anything from
		// pass 1 (on a diagonal bridge a rail from one row can fall on the walkway of a row further along).
		for (int i = 0; i <= n; i++) {
			int x = (int) Math.round(x1 + ux * i), z = (int) Math.round(z1 + uz * i);
			int y = y1 + (int) Math.round((y2 - y1) * (i / (double) n));
			for (int w : new int[]{-2, 2}) {
				if (i < 2 || i > n - 2) {
					continue; // the ends meet land or a landing (and must not cut into a neighbouring bridge's walkway)
				}
				int rx = x + side.getStepX() * w, rz = z + side.getStepZ() * w;
				c.set(rx, y - 1, rz, s(Blocks.SMOOTH_STONE));
				c.set(rx, y, rz, s(Blocks.STONE_BRICKS));
				c.set(rx, y + 1, rz, s(Blocks.STONE_BRICK_WALL));
				if (i % 8 == 4) {
					c.set(rx, y + 2, rz, lantern(false));
				}
			}
			if (i % 6 == 3) {
				for (int k = 2; k <= 4; k++) {
					for (int w = -1; w <= 1; w++) {
						if (k == 4 && w != 0) {
							continue;
						}
						c.set(x + side.getStepX() * w, y - k, z + side.getStepZ() * w, s(Blocks.WHITE_WOOL));
					}
				}
			}
		}
		int prevY = y1;
		for (int i = 0; i <= n; i++) {
			int x = (int) Math.round(x1 + ux * i), z = (int) Math.round(z1 + uz * i);
			int y = y1 + (int) Math.round((y2 - y1) * (i / (double) n));
			boolean climb = y > prevY, drop = y < prevY;
			for (int w = -1; w <= 1; w++) {
				int bx = x + side.getStepX() * w, bz = z + side.getStepZ() * w;
				BlockState deck = climb ? stair(Blocks.POLISHED_DIORITE_STAIRS, along, false)
						: drop ? stair(Blocks.POLISHED_DIORITE_STAIRS, along.getOpposite(), false) : s(Blocks.POLISHED_DIORITE);
				c.set(bx, y, bz, deck);
				c.set(bx, y - 1, bz, s(Blocks.SMOOTH_STONE));
				c.fill(bx, y + 1, bz, bx, y + 3, bz, s(Blocks.AIR));
			}
			prevY = y;
		}
	}

	/** A round broad-leaf tree standing on {@code y} (the ground block): a sturdy trunk and a layered canopy. */
	static void tree(Canvas c, int x, int y, int z, int h, Block log, Block leaves) {
		c.fill(x, y + 1, z, x, y + h, z, s(log));
		BlockState leaf = Kit.leaves(leaves);
		for (int dy = h - 2; dy <= h + 2; dy++) {
			int r = dy >= h + 2 ? 1 : dy == h - 2 ? 2 : 3;
			for (int dx = -r; dx <= r; dx++) {
				for (int dz = -r; dz <= r; dz++) {
					if (dx * dx + dz * dz <= r * r + 1 && (dx != 0 || dz != 0 || dy > h)) {
						if (unit(x + dx, z + dz, dy) > 0.12 || Math.abs(dx) + Math.abs(dz) < r) {
							c.set(x + dx, y + dy, z + dz, leaf);
						}
					}
				}
			}
		}
	}

	// ==================================================================================================== build

	@Override
	public void build(Canvas c) {
		crescent(c);
		if (c.near(TEMPLE_X, TEMPLE_Z, TEMPLE_RX + 8)) {
			floating(c, TEMPLE_X, TEMPLE_Z, TEMPLE_RX, TEMPLE_RZ, (x, z) -> TEMPLE_Y, Blocks.CALCITE, Blocks.CALCITE, 16, 3);
			paleGround(c, TEMPLE_X, TEMPLE_Z, TEMPLE_RX, TEMPLE_RZ, TEMPLE_Y);
			cloudTemple(c);
		}
		if (c.near(FOREST_X, FOREST_Z, FOREST_RX + 8)) {
			floating(c, FOREST_X, FOREST_Z, FOREST_RX, FOREST_RZ, (x, z) -> FOREST_Y, Blocks.GRASS_BLOCK, Blocks.DIRT, 11, 5);
			forest(c);
		}
		if (c.near(VILLAGE_X, VILLAGE_Z, VILLAGE_RX + 8)) {
			floating(c, VILLAGE_X, VILLAGE_Z, VILLAGE_RX, VILLAGE_RZ, CloudtopIslesV7::villageHeight, Blocks.GRASS_BLOCK, Blocks.DIRT, 20, 7);
			village(c);
		}
		waterfall(c);
		if (c.near(GATE_X, GATE_Z, GATE_RX + 8)) {
			floating(c, GATE_X, GATE_Z, GATE_RX, GATE_RZ, (x, z) -> GATE_Y, Blocks.GRASS_BLOCK, Blocks.DIRT, 12, 11);
			sanctuaryGate(c);
		}
		// The climbing route: crescent -> landing -> temple -> forest -> landing -> village -> sanctuary entrance.
		cloudPad(c, -49, 7, 3, 77, true);
		cloudPad(c, -19, -93, 3, 115, true);
		skyBridge(c, -34, SEA_G + 1, 14, -48, 78, 8);
		skyBridge(c, -49, 78, 5, -55, TEMPLE_Y, -10);
		skyBridge(c, -50, TEMPLE_Y, -38, -44, FOREST_Y, -62);
		skyBridge(c, -27, FOREST_Y, -76, -20, 116, -92);
		skyBridge(c, -18, 116, -92, -8, VILLAGE_Y, -74);
		skyBridge(c, 40, VILLAGE_Y + 3, -70, 50, GATE_Y, -94);
		// Upper sanctuary.
		if (c.near(PA_X, PA_Z, PA_RX + 8)) {
			floating(c, PA_X, PA_Z, PA_RX, PA_RZ, (x, z) -> PA_Y, Blocks.GRASS_BLOCK, Blocks.DIRT, 22, 13);
			plateauA(c);
		}
		if (c.near(PB_X, PB_Z, PB_RX + 8)) {
			floating(c, PB_X, PB_Z, PB_RX, PB_RZ, (x, z) -> PB_Y, Blocks.GRASS_BLOCK, Blocks.DIRT, 28, 17);
			plateauB(c);
		}
		rootBridge(c);
		// Drifting cloud fragments between the layers.
		for (int[] f : new int[][]{{-20, -30, 104}, {44, -40, 112}, {-70, -60, 120}, {30, 10, 96}, {70, -70, 160}, {-40, -10, 196},
				{30, -100, 190}, {120, -10, 205}, {-60, -90, 175}}) {
			if (c.near(f[0], f[1], 6)) {
				cloudPad(c, f[0], f[1], 3 + (int) (unit(f[0], f[1], 81) * 2), f[2], false);
			}
		}
		treasure(c);
	}

	// ---------------------------------------------------------------------------------------------------- crescent

	/**
	 * The crescent harbor: grass with flowers, two tall grey rocks at the back and four smaller formations, the lake with
	 * its wharf, the arrival landing and the pier into the lagoon, a few birches.
	 */
	private void crescent(Canvas c) {
		for (int x = -60; x <= 60; x++) {
			for (int z = -50; z <= 45; z++) {
				if (!c.inChunk(x, z)) {
					continue;
				}
				int g = c.ground(x, z);
				if (g <= Terrain.WATER_TOP + 1 || c.terrain().paving(x, z) != null || c.terrain().top(x, z).getBlock() != Blocks.GRASS_BLOCK) {
					continue;
				}
				double r = unit(x, z, 31);
				double patch = (Math.sin(x * 0.2) + Math.sin(z * 0.17 + 1) + Math.sin((x + z) * 0.1)) / 3;
				if (patch > 0.4 && r < 0.14) {
					c.set(x, g + 1, z, s(r < 0.05 ? Blocks.CORNFLOWER : r < 0.09 ? Blocks.OXEYE_DAISY : Blocks.ALLIUM));
				} else if (r > 0.88) {
					c.set(x, g + 1, z, s(Blocks.GRASS));
				}
			}
		}
		rock(c, -30, 4, 18, 4);   // the two great rocks at the back
		rock(c, 30, 2, 15, 4);
		rock(c, -40, 20, 6, 2);
		rock(c, 38, 16, 5, 2);
		rock(c, -12, -6, 4, 2);
		rock(c, 4, -12, 5, 2);
		for (int[] t : new int[][]{{-22, 12}, {-36, 10}, {24, 10}, {12, -2}, {-6, 2}}) {
			if (c.near(t[0], t[1], 4)) {
				tree(c, t[0], c.ground(t[0], t[1]), t[1], 5, Blocks.BIRCH_LOG, Blocks.BIRCH_LEAVES);
			}
		}
		// Lake wharf: a short jetty from the south shore.
		if (c.near(LAKE_X, LAKE_Z, 10)) {
			for (int z = LAKE_Z + 5; z >= LAKE_Z + 1; z--) {
				for (int x = LAKE_X - 1; x <= LAKE_X; x++) {
					c.set(x, 65, z, s(Blocks.SPRUCE_PLANKS));
					c.fill(x, 66, z, x, 68, z, s(Blocks.AIR));
				}
				if (z == LAKE_Z + 1) {
					c.set(LAKE_X - 1, 66, z, s(Blocks.SPRUCE_FENCE));
					c.set(LAKE_X - 1, 67, z, lantern(false));
				}
			}
			c.fill(LAKE_X - 1, 61, LAKE_Z + 1, LAKE_X - 1, 64, LAKE_Z + 1, s(Blocks.SPRUCE_LOG));
		}
		// The lagoon pier (arrival): from the landing south into the sheltered water.
		if (c.near(0, 12, 12)) {
			for (int z = 6; z <= 20; z++) {
				for (int x = -1; x <= 1; x++) {
					c.set(x, 64, z, x == 0 ? s(Blocks.STRIPPED_SPRUCE_LOG).setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z) : s(Blocks.SPRUCE_PLANKS));
					c.fill(x, 65, z, x, 67, z, s(Blocks.AIR));
				}
				for (int x : new int[]{-2, 2}) {
					if (z % 4 == 0 || z == 20) {
						c.fill(x, Math.min(c.ground(x, z), 61), z, x, 64, z, s(Blocks.SPRUCE_LOG));
						c.set(x, 65, z, s(Blocks.SPRUCE_FENCE));
						if (z % 8 == 4) {
							c.set(x, 66, z, lantern(false));
						}
					} else {
						c.set(x, 64, z, s(Blocks.SPRUCE_PLANKS));
						c.set(x, 65, z, Kit.connected(Blocks.SPRUCE_FENCE, false));
					}
				}
			}
			c.set(-1, 65, 19, Canvas.facing(s(Blocks.BARREL), Direction.UP));
			// Signpost toward the bridges.
			int g = c.ground(-6, 4);
			c.fill(-6, g + 1, 4, -6, g + 2, 4, s(Blocks.SPRUCE_FENCE));
			c.set(-6, g + 3, 4, lantern(false));
		}
	}

	/** A grey rock formation: stacked andesite, stone and tuff in irregular tiers. */
	private static void rock(Canvas c, int x, int z, int h, int r) {
		if (!c.near(x, z, r + 2)) {
			return;
		}
		int g = c.ground(x, z);
		for (int y = 1; y <= h; y++) {
			double rr = r * (1.0 - (y / (double) (h + 1)) * 0.7) + 0.4;
			for (int dx = -r - 1; dx <= r + 1; dx++) {
				for (int dz = -r - 1; dz <= r + 1; dz++) {
					double d = Math.hypot(dx, dz * 1.2) + unit(x + dx, z + dz + y, 41) * 0.8;
					if (d <= rr) {
						double u = unit(x + dx, y, z + dz);
						c.set(x + dx, g + y, z + dz, u < 0.45 ? s(Blocks.ANDESITE) : u < 0.75 ? s(Blocks.STONE) : u < 0.9 ? s(Blocks.TUFF) : s(Blocks.COBBLESTONE));
					}
				}
			}
		}
	}

	// ---------------------------------------------------------------------------------------------------- temple island

	/** Pale, bare ground: calcite with smooth stone, diorite and gravel patches. */
	private static void paleGround(Canvas c, int cx, int cz, int rx, int rz, int y) {
		for (int x = cx - rx; x <= cx + rx; x++) {
			for (int z = cz - rz; z <= cz + rz; z++) {
				if (!c.inChunk(x, z) || edge(x, z, cx, cz, rx, rz, 3) > 1.0) {
					continue;
				}
				double p = (Math.sin(x * 0.3) + Math.sin(z * 0.27 + 2)) / 2;
				double u = unit(x, z, 51);
				c.set(x, y, z, p > 0.5 ? s(Blocks.SMOOTH_STONE) : u < 0.1 ? s(Blocks.DIORITE) : u < 0.16 ? s(Blocks.GRAVEL) : s(Blocks.CALCITE));
			}
		}
	}

	/**
	 * The Cloud Temple at the island's rear: a three-step podium with a broad 9-wide stair, a colonnade of twelve stone
	 * columns round a pillared hall (an altar, lanterns, a carved frieze), an upper terrace on the hall's roof reached by
	 * side stairs, and a raised rooftop platform kept free for a merchant.
	 */
	private void cloudTemple(Canvas c) {
		int cx = TEMPLE_X, z0 = TEMPLE_Z - 16, z1 = TEMPLE_Z - 2, x0 = cx - 10, x1 = cx + 10, y = TEMPLE_Y;
		// Podium (3 high) and stair.
		for (int k = 0; k < 3; k++) {
			c.fill(x0 - 2 + k, y + 1 + k, z0 - 2 + k, x1 + 2 - k, y + 1 + k, z1 + 2 - k, aged(cx, y + k, z0));
		}
		for (int k = 0; k < 3; k++) {
			for (int x = cx - 4; x <= cx + 4; x++) {
				c.set(x, y + 1 + k, z1 + 3 - k, stair(Blocks.STONE_BRICK_STAIRS, Direction.NORTH, false));
			}
		}
		int fy = y + 3; // hall floor
		c.fill(x0 + 1, fy, z0 + 1, x1 - 1, fy, z1 - 1, s(Blocks.POLISHED_DIORITE));
		// Colonnade.
		for (int x = x0; x <= x1; x += 4) {
			for (int z : new int[]{z0, z1}) {
				column(c, x, fy + 1, z, 8);
			}
		}
		for (int z = z0 + 4; z <= z1 - 4; z += 5) {
			column(c, x0, fy + 1, z, 8);
			column(c, x1, fy + 1, z, 8);
		}
		// Hall (cella) walls with the entrance on the south.
		int hx0 = cx - 6, hx1 = cx + 6, hz0 = z0 + 2, hz1 = z1 - 3;
		for (int x = hx0; x <= hx1; x++) {
			for (int z = hz0; z <= hz1; z++) {
				boolean wall = x == hx0 || x == hx1 || z == hz0 || z == hz1;
				for (int yy = fy + 1; yy <= fy + 7; yy++) {
					boolean door = z == hz1 && Math.abs(x - cx) <= 2 && yy <= fy + 5;
					c.set(x, yy, z, wall && !door ? (yy == fy + 6 ? s(Blocks.CHISELED_STONE_BRICKS) : aged(x, yy, z)) : s(Blocks.AIR));
				}
				c.set(x, fy + 8, z, s(Blocks.SMOOTH_STONE));
			}
		}
		for (int x = cx - 3; x <= cx + 3; x += 3) {
			c.fill(x, fy + 1, hz0 + 3, x, fy + 7, hz0 + 3, s(Blocks.QUARTZ_PILLAR));
		}
		c.fill(cx - 1, fy + 1, hz0 + 1, cx + 1, fy + 1, hz0 + 1, s(Blocks.CHISELED_QUARTZ_BLOCK));
		c.set(cx, fy + 2, hz0 + 1, s(Blocks.SEA_LANTERN));
		c.set(cx - 4, fy + 7, hz0 + 5, lantern(true));
		c.set(cx + 4, fy + 7, hz0 + 5, lantern(true));
		c.set(cx, fy + 7, hz1 - 2, lantern(true));
		// Entablature over the colonnade, the upper terrace (walkable roof) and its parapet.
		c.fill(x0, fy + 9, z0, x1, fy + 9, z1, s(Blocks.SMOOTH_STONE));
		for (int x = x0; x <= x1; x++) {
			for (int z = z0; z <= z1; z++) {
				boolean rim = x == x0 || x == x1 || z == z0 || z == z1;
				if (rim) {
					c.set(x, fy + 10, z, (x + z) % 2 == 0 ? s(Blocks.STONE_BRICK_WALL) : Kit.slab(Blocks.STONE_BRICK_SLAB, false));
				}
			}
		}
		// Side stairs from the podium to the terrace (west side).
		for (int k = 0; k < 9; k++) {
			c.set(x0 - 1, fy + 1 + k, z1 - 1 - k, stair(Blocks.STONE_BRICK_STAIRS, Direction.NORTH, false));
			c.fill(x0 - 1, fy, z1 - 1 - k, x0 - 1, fy + k, z1 - 1 - k, s(Blocks.STONE_BRICKS));
		}
		c.set(x0, fy + 10, z1 - 9, s(Blocks.AIR));
		c.set(x0, fy + 10, z1 - 10, s(Blocks.AIR));
		// The rooftop platform (raised, for a merchant or special NPC).
		c.fill(cx - 3, fy + 10, z0 + 4, cx + 3, fy + 10, z0 + 8, s(Blocks.POLISHED_DIORITE));
		for (int x = cx - 3; x <= cx + 3; x += 6) {
			c.fill(x, fy + 11, z0 + 4, x, fy + 13, z0 + 4, s(Blocks.QUARTZ_PILLAR));
			c.fill(x, fy + 11, z0 + 8, x, fy + 13, z0 + 8, s(Blocks.QUARTZ_PILLAR));
		}
		c.fill(cx - 3, fy + 14, z0 + 4, cx + 3, fy + 14, z0 + 8, Kit.slab(Blocks.SMOOTH_QUARTZ_SLAB, false));
		// Weathered stones in front: broken column drums lying in the courtyard edges (clear of the fight).
		for (int[] p : new int[][]{{cx - 14, TEMPLE_Z + 8}, {cx + 15, TEMPLE_Z + 6}}) {
			c.set(p[0], y + 1, p[1], s(Blocks.QUARTZ_PILLAR).setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
			c.set(p[0] + 1, y + 1, p[1], s(Blocks.QUARTZ_PILLAR).setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
		}
	}

	/** A stone column: a chiseled base, quartz shaft, a chiseled capital. */
	private static void column(Canvas c, int x, int y, int z, int h) {
		c.set(x, y, z, s(Blocks.CHISELED_STONE_BRICKS));
		c.fill(x, y + 1, z, x, y + h - 2, z, s(Blocks.QUARTZ_PILLAR));
		c.set(x, y + h - 1, z, s(Blocks.CHISELED_STONE_BRICKS));
	}

	// ---------------------------------------------------------------------------------------------------- forest island

	/** Six trees round an open middle, a tall flat-sided rock at the north-east edge, grass and flowers. */
	private void forest(Canvas c) {
		int y = FOREST_Y;
		int[][] trees = {{-10, -5, 6, 0}, {-8, 6, 5, 1}, {0, -9, 7, 0}, {9, -6, 5, 1}, {10, 5, 6, 0}, {-2, 9, 5, 0}};
		for (int[] t : trees) {
			tree(c, FOREST_X + t[0], y, FOREST_Z + t[1], t[2], t[3] == 0 ? Blocks.OAK_LOG : Blocks.BIRCH_LOG,
					t[3] == 0 ? Blocks.OAK_LEAVES : Blocks.BIRCH_LEAVES);
		}
		int rx = FOREST_X + 9, rz = FOREST_Z - 1;
		for (int dx = 0; dx <= 2; dx++) {
			for (int dz = 0; dz <= 4; dz++) {
				int h = 7 - dz + (dx == 1 ? 1 : 0);
				c.fill(rx + dx, y + 1, rz + dz, rx + dx, y + h, rz + dz, dx == 1 ? s(Blocks.SMOOTH_BASALT) : s(Blocks.POLISHED_BASALT));
			}
		}
		for (int x = FOREST_X - FOREST_RX; x <= FOREST_X + FOREST_RX; x++) {
			for (int z = FOREST_Z - FOREST_RZ; z <= FOREST_Z + FOREST_RZ; z++) {
				if (!c.inChunk(x, z) || edge(x, z, FOREST_X, FOREST_Z, FOREST_RX, FOREST_RZ, 5) > 0.92) {
					continue;
				}
				double r = unit(x, z, 61);
				if (Math.hypot(x - FOREST_X, z - FOREST_Z) < 5) {
					continue; // the open middle
				}
				if (r < 0.06) {
					c.set(x, y + 1, z, s(r < 0.03 ? Blocks.POPPY : Blocks.DANDELION));
				} else if (r > 0.85) {
					c.set(x, y + 1, z, s(Blocks.GRASS));
				}
			}
		}
	}

	// ---------------------------------------------------------------------------------------------------- village

	/** The village's ground: 124, with a raised east terrace at 127 (stone steps between them). */
	static int villageHeight(int x, int z) {
		return x >= VILLAGE_X + 14 ? VILLAGE_Y + 3 : x >= VILLAGE_X + 11 ? VILLAGE_Y + (x - VILLAGE_X - 10) : VILLAGE_Y;
	}

	/**
	 * The Sky Village: a winding path from the west bridge through the plaza (a well and benches, the quest giver and the
	 * innkeeper), past six houses to the east terrace and the bridge up to the sanctuary; flowers, trees and lanterns; the
	 * waterfall pouring off the south edge.
	 */
	private void village(Canvas c) {
		double[] path = {-8, -74, -2, -68, 4, -62, 10, -60, 18, -62, 24, -66, 30, -68, 38, -70};
		for (int x = VILLAGE_X - VILLAGE_RX; x <= VILLAGE_X + VILLAGE_RX; x++) {
			for (int z = VILLAGE_Z - VILLAGE_RZ; z <= VILLAGE_Z + VILLAGE_RZ; z++) {
				if (!c.inChunk(x, z) || edge(x, z, VILLAGE_X, VILLAGE_Z, VILLAGE_RX, VILLAGE_RZ, 7) > 0.98) {
					continue;
				}
				int y = villageHeight(x, z);
				boolean onPath = false;
				for (int i = 0; i + 3 < path.length; i += 2) {
					if (distSeg(x, z, path[i], path[i + 1], path[i + 2], path[i + 3]) <= 1.3) {
						onPath = true;
					}
				}
				boolean plaza = Math.hypot(x - 8, z - 60 - 0) < 5.5 && z <= -55 && z >= -66;
				if (plaza) {
					c.set(x, y, z, unit(x, z, 71) < 0.3 ? s(Blocks.CALCITE) : s(Blocks.POLISHED_DIORITE));
				} else if (onPath) {
					c.set(x, y, z, x >= VILLAGE_X + 11 && x <= VILLAGE_X + 13 ? stair(Blocks.STONE_BRICK_STAIRS, Direction.EAST, false)
							: unit(x, z, 72) < 0.25 ? s(Blocks.COBBLESTONE) : s(Blocks.DIRT_PATH));
				} else {
					double r = unit(x, z, 73);
					if (r < 0.05) {
						c.set(x, y + 1, z, s(r < 0.02 ? Blocks.AZURE_BLUET : r < 0.035 ? Blocks.LILY_OF_THE_VALLEY : Blocks.PINK_TULIP));
					} else if (r > 0.86) {
						c.set(x, y + 1, z, s(Blocks.GRASS));
					}
				}
			}
		}
		// Plaza: the well and benches.
		int py = VILLAGE_Y;
		c.fill(7, py + 1, -61, 9, py + 1, -59, s(Blocks.STONE_BRICKS));
		c.set(8, py + 1, -60, s(Blocks.WATER));
		for (int[] p : new int[][]{{7, -61}, {9, -61}, {7, -59}, {9, -59}}) {
			c.fill(p[0], py + 2, p[1], p[0], py + 3, p[1], s(Blocks.SPRUCE_FENCE));
		}
		c.fill(7, py + 4, -61, 9, py + 4, -59, Kit.slab(Blocks.SPRUCE_SLAB, false));
		c.set(8, py + 3, -60, lantern(true));
		c.set(5, py + 1, -57, stair(Blocks.SPRUCE_STAIRS, Direction.SOUTH, false));
		c.set(11, py + 1, -57, stair(Blocks.SPRUCE_STAIRS, Direction.SOUTH, false));
		for (House h : HOUSES) {
			house(c, h);
		}
		for (int[] t : new int[][]{{-12, -66, 5}, {-4, -46, 6}, {36, -50, 5}, {18, -80, 6}, {34, -76, 5}}) {
			tree(c, t[0], villageHeight(t[0], t[1]), t[1], t[2], Blocks.OAK_LOG, Blocks.OAK_LEAVES);
		}
		for (int[] l : new int[][]{{0, -70}, {14, -64}, {26, -64}, {36, -68}, {2, -56}}) {
			int y = villageHeight(l[0], l[1]);
			c.fill(l[0], y + 1, l[1], l[0], y + 2, l[1], s(Blocks.SPRUCE_FENCE));
			c.set(l[0], y + 3, l[1], lantern(false));
		}
		// A rail along the south edge by the plaza (fall protection near the NPCs).
		for (int x = -2; x <= 14; x++) {
			int z = -40 + (int) Math.round(Math.abs(x - 6) * 0.15);
			if (x == FALL_X || x == FALL_X - 1) {
				continue;
			}
			c.set(x, VILLAGE_Y + 1, z, Kit.connected(Blocks.SPRUCE_FENCE, true));
		}
	}

	static double distSeg(double px, double pz, double ax, double az, double bx, double bz) {
		double dx = bx - ax, dz = bz - az, len = dx * dx + dz * dz;
		double t = len == 0 ? 0 : Math.max(0, Math.min(1, ((px - ax) * dx + (pz - az) * dz) / len));
		return Math.hypot(px - (ax + t * dx), pz - (az + t * dz));
	}

	/** A small house: pale stone base, plank or calcite walls, a pitched roof, a door standing open, a furnished room. */
	private static void house(Canvas c, House h) {
		int y = villageHeight(h.x() + h.w() / 2, h.z() + h.d() / 2);
		int x1 = h.x(), z1 = h.z(), x2 = h.x() + h.w() - 1, z2 = h.z() + h.d() - 1;
		for (int x = x1; x <= x2; x++) {
			for (int z = z1; z <= z2; z++) {
				boolean wall = x == x1 || x == x2 || z == z1 || z == z2;
				boolean corner = (x == x1 || x == x2) && (z == z1 || z == z2);
				c.set(x, y, z, s(Blocks.STONE_BRICKS));
				c.set(x, y + 1, z, wall ? s(Blocks.STONE_BRICKS) : s(Blocks.SPRUCE_PLANKS));
				for (int yy = y + 2; yy <= y + 4; yy++) {
					c.set(x, yy, z, wall ? (corner ? s(Blocks.STRIPPED_SPRUCE_LOG) : s(h.wall())) : s(Blocks.AIR));
				}
				c.set(x, y + 1, z, wall ? s(Blocks.STONE_BRICKS) : s(Blocks.SPRUCE_PLANKS));
			}
		}
		// Windows.
		for (int x = x1 + 2; x < x2; x += 3) {
			c.set(x, y + 3, z1, Kit.connected(Blocks.GLASS_PANE, true));
			c.set(x, y + 3, z2, Kit.connected(Blocks.GLASS_PANE, true));
		}
		for (int z = z1 + 2; z < z2; z += 3) {
			c.set(x1, y + 3, z, Kit.connected(Blocks.GLASS_PANE, false));
			c.set(x2, y + 3, z, Kit.connected(Blocks.GLASS_PANE, false));
		}
		// Door.
		Direction f = h.front();
		int dx = f == Direction.EAST ? x2 : f == Direction.WEST ? x1 : (x1 + x2) / 2;
		int dz = f == Direction.SOUTH ? z2 : f == Direction.NORTH ? z1 : (z1 + z2) / 2;
		BlockState door = s(Blocks.SPRUCE_DOOR).setValue(BlockStateProperties.HORIZONTAL_FACING, f).setValue(BlockStateProperties.OPEN, true)
				.setValue(BlockStateProperties.DOOR_HINGE, DoorHingeSide.LEFT);
		c.set(dx, y + 2, dz, door.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER));
		c.set(dx, y + 3, dz, door.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER));
		c.set(dx + f.getStepX(), y + 1, dz + f.getStepZ(), stair(Blocks.STONE_BRICK_STAIRS, f.getOpposite(), false));
		c.set(dx + f.getStepX() + f.getClockWise().getStepX(), y + 4, dz + f.getStepZ() + f.getClockWise().getStepZ(), lantern(true));
		// Pitched roof along the longer side, with overhang; heights vary with the house.
		boolean alongX = h.w() >= h.d();
		int lo = (alongX ? z1 : x1) - 1, hi = (alongX ? z2 : x2) + 1, a1 = (alongX ? x1 : z1) - 1, a2 = (alongX ? x2 : z2) + 1;
		int base = y + 5 + (h.kind() % 2);
		for (int i = 0; lo + i <= hi - i; i++) {
			for (int t = a1; t <= a2; t++) {
				int p = lo + i, q = hi - i;
				if (p == q) {
					put(c, alongX, t, base + i, p, Kit.slab(h.roof() == Blocks.SPRUCE_STAIRS ? Blocks.SPRUCE_SLAB : Blocks.DARK_OAK_SLAB, false));
				} else {
					put(c, alongX, t, base + i, p, stair(h.roof(), alongX ? Direction.SOUTH : Direction.EAST, false));
					put(c, alongX, t, base + i, q, stair(h.roof(), alongX ? Direction.NORTH : Direction.WEST, false));
				}
				if ((t == a1 + 1 || t == a2 - 1) && i > 0) {
					for (int m = p + 1; m < q; m++) {
						put(c, alongX, t, base + i, m, s(h.wall()));
					}
				}
			}
		}
		// Raise the walls to meet a higher roof base.
		if (base > y + 5) {
			for (int x = x1; x <= x2; x++) {
				for (int z = z1; z <= z2; z++) {
					if (x == x1 || x == x2 || z == z1 || z == z2) {
						c.set(x, y + 5, z, s(h.wall()));
					}
				}
			}
		}
		// Furniture.
		int ix = x1 + 1, iz = z1 + 1;
		c.set(ix, y + 2, iz, s(Blocks.RED_BED).setValue(BedBlock.FACING, Direction.NORTH).setValue(BedBlock.PART, BedPart.HEAD));
		c.set(ix, y + 2, iz + 1, s(Blocks.RED_BED).setValue(BedBlock.FACING, Direction.NORTH).setValue(BedBlock.PART, BedPart.FOOT));
		c.set(x2 - 1, y + 2, iz, h.kind() % 3 == 0 ? s(Blocks.CRAFTING_TABLE) : h.kind() % 3 == 1 ? s(Blocks.LOOM) : s(Blocks.BOOKSHELF));
		c.set(x2 - 1, y + 2, z2 - 1, Canvas.facing(s(Blocks.BARREL), Direction.UP));
		c.set(ix + 1, y + 2, iz, s(Blocks.FLOWER_POT));
		c.set((x1 + x2) / 2, y + 4, (z1 + z2) / 2, lantern(true));
	}

	private static void put(Canvas c, boolean alongX, int t, int y, int m, BlockState state) {
		if (alongX) {
			c.set(t, y, m, state);
		} else {
			c.set(m, y, t, state);
		}
	}

	/**
	 * The waterfall: a source in a stone spout on the village's south edge and a falling column straight down into the
	 * crescent's lake. Placed as falling water so it looks right at once and never spreads sideways.
	 */
	private void waterfall(Canvas c) {
		if (!c.near(FALL_X, FALL_Z, 3)) {
			return;
		}
		int top = VILLAGE_Y;
		for (int x = FALL_X - 1; x <= FALL_X; x++) {
			// The spout: a short stone channel cut into the edge.
			c.set(x, top, FALL_Z - 2, s(Blocks.WATER));
			c.set(x, top, FALL_Z - 1, s(Blocks.WATER));
			c.set(x, top - 1, FALL_Z - 2, s(Blocks.STONE_BRICKS));
			c.set(x, top - 1, FALL_Z - 1, s(Blocks.STONE_BRICKS));
			BlockState falling = s(Blocks.WATER).setValue(LiquidBlock.LEVEL, 8);
			for (int y = top; y >= 65; y--) {
				c.set(x, y, FALL_Z, falling);
			}
			c.tickFluid(x, top, FALL_Z);
		}
		for (int y = top - 6; y >= 70; y -= 6) {
			c.set(FALL_X + 1, y, FALL_Z + 1, s(Blocks.LIGHT).setValue(net.minecraft.world.level.block.LightBlock.LEVEL, 12));
		}
		c.set(FALL_X - 2, top, FALL_Z - 2, s(Blocks.STONE_BRICKS));
		c.set(FALL_X + 1, top, FALL_Z - 2, s(Blocks.STONE_BRICKS));
		c.set(FALL_X - 2, top, FALL_Z - 1, s(Blocks.STONE_BRICKS));
		c.set(FALL_X + 1, top, FALL_Z - 1, s(Blocks.STONE_BRICKS));
	}

	// ---------------------------------------------------------------------------------------------------- sanctuary entrance

	/**
	 * The sanctuary gate: a weathered temple (a stepped base, four corner piers, a carved lintel band, a cracked stone
	 * roof) whose chamber holds a shaft of cloud rising from a ringed floor: stepping into the cloud carries a player up to
	 * the Upper Sanctuary. Grass and two trees round it, a broad stair from the village bridge.
	 */
	private void sanctuaryGate(Canvas c) {
		int cx = GATE_X, cz = GATE_Z - 3, y = GATE_Y;
		int x0 = cx - 6, x1 = cx + 6, z0 = cz - 6, z1 = cz + 6;
		for (int x = x0 - 1; x <= x1 + 1; x++) {
			for (int z = z0 - 1; z <= z1 + 1; z++) {
				c.set(x, y + 1, z, aged(x, y + 1, z));
			}
		}
		for (int x = x0; x <= x1; x++) {
			for (int z = z0; z <= z1; z++) {
				boolean wall = x == x0 || x == x1 || z == z0 || z == z1;
				boolean pier = (x == x0 || x == x1) && (z == z0 || z == z1);
				for (int yy = y + 2; yy <= y + 10; yy++) {
					boolean door = z == z1 && Math.abs(x - cx) <= 2 && yy <= y + 7;
					BlockState b = pier ? s(Blocks.POLISHED_ANDESITE) : yy == y + 8 ? s(Blocks.CHISELED_STONE_BRICKS) : aged(x, yy, z);
					c.set(x, yy, z, wall && !door ? b : s(Blocks.AIR));
				}
				c.set(x, y + 11, z, unit(x, z, 91) < 0.15 ? s(Blocks.AIR) : s(Blocks.CRACKED_STONE_BRICKS)); // a broken roof
			}
		}
		c.fill(cx - 3, y + 8, z1, cx + 3, y + 8, z1, s(Blocks.CHISELED_STONE_BRICKS));
		// The cloud shaft: a ring of quartz round a 3 x 3 floor of cloud, cloud columns rising to the roof's hole.
		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) {
				boolean ring = Math.abs(dx) == 2 || Math.abs(dz) == 2;
				c.set(cx + dx, y + 1, cz + dz, ring ? s(Blocks.CHISELED_QUARTZ_BLOCK) : s(Blocks.SNOW_BLOCK));
			}
		}
		for (int[] p : new int[][]{{-2, -2}, {2, -2}, {-2, 2}, {2, 2}}) {
			c.fill(cx + p[0], y + 2, cz + p[1], cx + p[0], y + 9, cz + p[1], s(Blocks.QUARTZ_PILLAR));
			c.set(cx + p[0], y + 10, cz + p[1], s(Blocks.SEA_LANTERN));
		}
		c.fill(cx - 1, y + 11, cz - 1, cx + 1, y + 11, cz + 1, s(Blocks.AIR)); // open to the sky above the shaft
		// Stair and steps from the bridge landing.
		for (int x = cx - 2; x <= cx + 2; x++) {
			c.set(x, y + 1, z1 + 1, stair(Blocks.STONE_BRICK_STAIRS, Direction.NORTH, false));
		}
		tree(c, cx - 10, y, GATE_Z + 4, 5, Blocks.OAK_LOG, Blocks.OAK_LEAVES);
		tree(c, cx + 10, y, GATE_Z + 6, 5, Blocks.BIRCH_LOG, Blocks.BIRCH_LEAVES);
	}

	// ---------------------------------------------------------------------------------------------------- upper sanctuary

	/** Plateau A: four houses round a green, the upper quest giver's post, the glowing return doorway, trees. */
	private void plateauA(Canvas c) {
		int y = PA_Y;
		House[] hs = {new House(PA_X - 2, PA_Z - 20, 7, 6, Direction.SOUTH, Blocks.DARK_OAK_STAIRS, Blocks.CALCITE, 1),
				new House(PA_X + 10, PA_Z - 18, 6, 6, Direction.SOUTH, Blocks.SPRUCE_STAIRS, Blocks.SPRUCE_PLANKS, 2),
				new House(PA_X + 14, PA_Z + 6, 6, 7, Direction.WEST, Blocks.DARK_OAK_STAIRS, Blocks.CALCITE, 3),
				new House(PA_X - 4, PA_Z + 14, 7, 6, Direction.NORTH, Blocks.SPRUCE_STAIRS, Blocks.BIRCH_PLANKS, 4)};
		for (House h : hs) {
			houseAt(c, h, y);
		}
		// The green: a path ring and a stone circle.
		for (int x = PA_X - 26; x <= PA_X + 26; x++) {
			for (int z = PA_Z - 22; z <= PA_Z + 22; z++) {
				if (!c.inChunk(x, z) || edge(x, z, PA_X, PA_Z, PA_RX, PA_RZ, 13) > 0.95) {
					continue;
				}
				double d = Math.hypot(x - PA_X, z - PA_Z);
				if (Math.abs(d - 8) < 1.1 || (Math.abs(z - (PA_Z - 2)) <= 1 && x > PA_X + 8)) {
					c.set(x, y, z, unit(x, z, 101) < 0.3 ? s(Blocks.COBBLESTONE) : s(Blocks.DIRT_PATH));
				} else if (unit(x, z, 102) < 0.04) {
					c.set(x, y + 1, z, s(Blocks.LILY_OF_THE_VALLEY));
				}
			}
		}
		// The return doorway: a quartz arch filled with a sheet of cloud light.
		int dx = PORTAL_DOWN_X, dz = PORTAL_DOWN_Z;
		for (int k = -2; k <= 2; k++) {
			for (int yy = y + 1; yy <= y + 6; yy++) {
				boolean side = Math.abs(k) == 2;
				boolean topRow = yy == y + 6;
				c.set(dx, yy, dz + k, side || topRow ? s(Blocks.CHISELED_QUARTZ_BLOCK) : s(Blocks.AIR));
			}
		}
		c.set(dx, y + 7, dz, s(Blocks.SEA_LANTERN));
		c.fill(dx, y, dz - 1, dx, y, dz + 1, s(Blocks.SNOW_BLOCK));
		// Quest giver's post (a lectern under a small canopy).
		int qx = PA_X + 6, qz = PA_Z + 4;
		c.fill(qx, y + 1, qz - 1, qx, y + 3, qz - 1, s(Blocks.SPRUCE_FENCE));
		c.fill(qx, y + 1, qz + 1, qx, y + 3, qz + 1, s(Blocks.SPRUCE_FENCE));
		c.fill(qx - 1, y + 4, qz - 1, qx, y + 4, qz + 1, s(Blocks.LIGHT_BLUE_WOOL));
		c.set(qx, y + 1, qz, s(Blocks.LECTERN));
		for (int[] t : new int[][]{{PA_X - 18, PA_Z - 10, 6}, {PA_X + 22, PA_Z - 8, 6}, {PA_X - 14, PA_Z + 18, 5}, {PA_X + 4, PA_Z + 24, 5}}) {
			tree(c, t[0], y, t[1], t[2], Blocks.BIRCH_LOG, Blocks.BIRCH_LEAVES);
		}
	}

	/** A house standing on a fixed level {@code y} (for the plateaus). */
	private static void houseAt(Canvas c, House h, int y) {
		House shifted = h;
		// Reuse the village builder by temporarily treating y as the village height: build directly here instead.
		int x1 = h.x(), z1 = h.z(), x2 = h.x() + h.w() - 1, z2 = h.z() + h.d() - 1;
		for (int x = x1; x <= x2; x++) {
			for (int z = z1; z <= z2; z++) {
				boolean wall = x == x1 || x == x2 || z == z1 || z == z2;
				boolean corner = (x == x1 || x == x2) && (z == z1 || z == z2);
				c.set(x, y, z, s(Blocks.STONE_BRICKS));
				c.set(x, y + 1, z, wall ? s(Blocks.STONE_BRICKS) : s(Blocks.SPRUCE_PLANKS));
				for (int yy = y + 2; yy <= y + 4; yy++) {
					c.set(x, yy, z, wall ? (corner ? s(Blocks.STRIPPED_DARK_OAK_LOG) : s(h.wall())) : s(Blocks.AIR));
				}
			}
		}
		Direction f = shifted.front();
		int dx = f == Direction.EAST ? x2 : f == Direction.WEST ? x1 : (x1 + x2) / 2;
		int dz = f == Direction.SOUTH ? z2 : f == Direction.NORTH ? z1 : (z1 + z2) / 2;
		BlockState door = s(Blocks.DARK_OAK_DOOR).setValue(BlockStateProperties.HORIZONTAL_FACING, f).setValue(BlockStateProperties.OPEN, true)
				.setValue(BlockStateProperties.DOOR_HINGE, DoorHingeSide.LEFT);
		c.set(dx, y + 2, dz, door.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER));
		c.set(dx, y + 3, dz, door.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER));
		c.set(dx + f.getStepX(), y + 1, dz + f.getStepZ(), stair(Blocks.STONE_BRICK_STAIRS, f.getOpposite(), false));
		for (int x = x1 + 2; x < x2; x += 3) {
			c.set(x, y + 3, z1, Kit.connected(Blocks.GLASS_PANE, true));
			c.set(x, y + 3, z2, Kit.connected(Blocks.GLASS_PANE, true));
		}
		boolean alongX = h.w() >= h.d();
		int lo = (alongX ? z1 : x1) - 1, hi = (alongX ? z2 : x2) + 1, a1 = (alongX ? x1 : z1) - 1, a2 = (alongX ? x2 : z2) + 1;
		int base = y + 5;
		for (int i = 0; lo + i <= hi - i; i++) {
			for (int t = a1; t <= a2; t++) {
				int p = lo + i, q = hi - i;
				if (p == q) {
					put(c, alongX, t, base + i, p, Kit.slab(Blocks.DARK_OAK_SLAB, false));
				} else {
					put(c, alongX, t, base + i, p, stair(h.roof(), alongX ? Direction.SOUTH : Direction.EAST, false));
					put(c, alongX, t, base + i, q, stair(h.roof(), alongX ? Direction.NORTH : Direction.WEST, false));
				}
				if ((t == a1 + 1 || t == a2 - 1) && i > 0) {
					for (int m = p + 1; m < q; m++) {
						put(c, alongX, t, base + i, m, s(h.wall()));
					}
				}
			}
		}
		c.set(x1 + 1, y + 2, z1 + 1, s(Blocks.WHITE_BED).setValue(BedBlock.FACING, Direction.NORTH).setValue(BedBlock.PART, BedPart.HEAD));
		c.set(x1 + 1, y + 2, z1 + 2, s(Blocks.WHITE_BED).setValue(BedBlock.FACING, Direction.NORTH).setValue(BedBlock.PART, BedPart.FOOT));
		c.set(x2 - 1, y + 2, z1 + 1, s(Blocks.BOOKSHELF));
		c.set(x2 - 1, y + 2, z2 - 1, Canvas.facing(s(Blocks.BARREL), Direction.UP));
		c.set((x1 + x2) / 2, y + 4, (z1 + z2) / 2, lantern(true));
	}

	/**
	 * Plateau B: the great bell on a timber-and-stone frame over a raised round dais, stone paths, the ancient temple at the
	 * east end (a broad stair, tall columns, a pillared hall with side chambers and a spot for an instructor), and the boss
	 * arena on the north side (a 34 x 24 stone floor inside a wall, no gaps).
	 */
	private void plateauB(Canvas c) {
		int y = PB_Y;
		// Paths: from the root bridge east to the temple, a branch north to the arena.
		for (int x = PB_X - PB_RX; x <= PB_X + PB_RX; x++) {
			for (int z = PB_Z - PB_RZ; z <= PB_Z + PB_RZ; z++) {
				if (!c.inChunk(x, z) || edge(x, z, PB_X, PB_Z, PB_RX, PB_RZ, 17) > 0.96) {
					continue;
				}
				boolean main = Math.abs(z - PB_Z) <= 1 && x < PB_X + 30;
				boolean branch = Math.abs(x - PB_X) <= 1 && z < PB_Z && z > PB_Z - 16;
				if (main || branch) {
					c.set(x, y, z, unit(x, z, 111) < 0.3 ? s(Blocks.MOSSY_COBBLESTONE) : s(Blocks.STONE_BRICKS));
				} else if (unit(x, z, 112) > 0.9) {
					c.set(x, y + 1, z, s(Blocks.GRASS));
				}
			}
		}
		bell(c, PB_X - 8, y, PB_Z);
		ancientTemple(c, PB_X + 36, y, PB_Z);
		bossArena(c, PB_X, y, PB_Z - 28);
		for (int[] t : new int[][]{{PB_X - 36, PB_Z - 20, 7}, {PB_X - 30, PB_Z + 26, 6}, {PB_X + 20, PB_Z + 30, 6}, {PB_X - 10, PB_Z + 34, 5}}) {
			tree(c, t[0], y, t[1], t[2], Blocks.DARK_OAK_LOG, Blocks.DARK_OAK_LEAVES);
		}
	}

	/** The great bell: a round dais, two massive posts with braces, a beam, and a bell of gold and copper hanging free. */
	private static void bell(Canvas c, int cx, int y, int cz) {
		for (int dx = -7; dx <= 7; dx++) {
			for (int dz = -7; dz <= 7; dz++) {
				double d = Math.hypot(dx, dz);
				if (d <= 7.2) {
					c.set(cx + dx, y + 1, cz + dz, d > 6.2 ? s(Blocks.STONE_BRICKS) : s(Blocks.POLISHED_DIORITE));
				}
				if (d <= 6.2 && d > 5.2) {
					c.set(cx + dx, y + 2, cz + dz, s(Blocks.STONE_BRICK_WALL));
				}
			}
		}
		for (int dz : new int[]{-1, 1}) {
			c.set(cx, y + 2, cz + dz * 6, s(Blocks.AIR)); // openings in the low wall
		}
		for (int dx : new int[]{-5, 5}) {
			c.fill(cx + dx, y + 2, cz - 1, cx + dx, y + 16, cz + 1, s(Blocks.DARK_OAK_LOG));
			c.set(cx + dx, y + 2, cz, s(Blocks.CHISELED_STONE_BRICKS));
			c.set(cx + dx + (dx > 0 ? -1 : 1), y + 15, cz, stair(Blocks.DARK_OAK_STAIRS, dx > 0 ? Direction.EAST : Direction.WEST, true));
		}
		c.fill(cx - 6, y + 17, cz - 1, cx + 6, y + 17, cz + 1, s(Blocks.STRIPPED_DARK_OAK_LOG).setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
		c.fill(cx - 6, y + 18, cz, cx + 6, y + 18, cz, Kit.slab(Blocks.DARK_OAK_SLAB, false));
		// The bell: chains, a crown, and a flaring body (radius 1 -> 3) of gold with a copper lip.
		c.fill(cx, y + 15, cz, cx, y + 16, cz, s(Blocks.CHAIN));
		for (int k = 0; k <= 6; k++) {
			int yy = y + 14 - k;
			double r = k == 0 ? 1 : 1.2 + k * 0.35;
			for (int dx = -3; dx <= 3; dx++) {
				for (int dz = -3; dz <= 3; dz++) {
					double d = Math.hypot(dx, dz);
					if (d <= r && (d > r - 1.2 || k == 0)) {
						c.set(cx + dx, yy, cz + dz, k == 6 ? s(Blocks.CUT_COPPER) : s(Blocks.GOLD_BLOCK));
					}
				}
			}
		}
		c.set(cx, y + 9, cz, s(Blocks.CHAIN));
		c.set(cx, y + 8, cz, s(Blocks.COPPER_BLOCK)); // the clapper
	}

	/**
	 * The ancient temple at the plateau's east end, facing west: a broad stair, eight tall columns along the front, a
	 * pillared hall with a glowing dais, two side chambers, decorative bands, and a roof parapet.
	 */
	private static void ancientTemple(Canvas c, int cx, int y, int cz) {
		int x0 = cx - 8, x1 = cx + 8, z0 = cz - 12, z1 = cz + 12;
		c.fill(x0 - 2, y + 1, z0 - 2, x1 + 2, y + 2, z1 + 2, s(Blocks.STONE_BRICKS));
		for (int k = 0; k < 2; k++) {
			for (int z = cz - 6; z <= cz + 6; z++) {
				c.set(x0 - 3 - k, y + 1 + (1 - k), z, stair(Blocks.STONE_BRICK_STAIRS, Direction.EAST, false));
			}
		}
		int fy = y + 3;
		for (int z = z0; z <= z1; z += 3) {
			c.set(x0 - 1, fy, z, s(Blocks.CHISELED_STONE_BRICKS));
			c.fill(x0 - 1, fy + 1, z, x0 - 1, fy + 11, z, s(Blocks.QUARTZ_PILLAR));
			c.set(x0 - 1, fy + 12, z, s(Blocks.CHISELED_STONE_BRICKS));
		}
		for (int x = x0; x <= x1; x++) {
			for (int z = z0; z <= z1; z++) {
				boolean wall = x == x0 || x == x1 || z == z0 || z == z1;
				boolean partition = (z == cz - 6 || z == cz + 6) && x > x0 + 2;
				for (int yy = fy; yy <= fy + 12; yy++) {
					boolean door = x == x0 && Math.abs(z - cz) <= 2 && yy <= fy + 6 && yy > fy;
					boolean sideDoor = partition && x >= cx - 1 && x <= cx + 1 && yy > fy && yy <= fy + 3;
					BlockState b = yy == fy ? s(Blocks.POLISHED_DIORITE) : yy == fy + 8 ? s(Blocks.CHISELED_STONE_BRICKS) : aged(x, yy, z);
					if (yy == fy) {
						c.set(x, yy, z, b);
					} else {
						c.set(x, yy, z, (wall || partition) && !door && !sideDoor ? b : s(Blocks.AIR));
					}
				}
				c.set(x, fy + 13, z, s(Blocks.SMOOTH_STONE));
				if (wall) {
					c.set(x, fy + 14, z, (x + z) % 2 == 0 ? s(Blocks.STONE_BRICK_WALL) : Kit.slab(Blocks.STONE_BRICK_SLAB, false));
				}
			}
		}
		for (int z : new int[]{cz - 3, cz + 3}) {
			for (int x : new int[]{cx - 3, cx + 3}) {
				c.fill(x, fy + 1, z, x, fy + 12, z, s(Blocks.QUARTZ_PILLAR));
			}
		}
		c.fill(cx + 4, fy + 1, cz - 1, cx + 6, fy + 1, cz + 1, s(Blocks.CHISELED_QUARTZ_BLOCK));
		c.set(cx + 5, fy + 2, cz, s(Blocks.SEA_LANTERN));
		c.set(cx, fy + 11, cz, lantern(true));
		c.set(cx, fy + 3, cz - 9, lantern(true));
		c.set(cx, fy + 3, cz + 9, lantern(true));
		c.set(cx + 6, fy + 1, cz - 9, s(Blocks.BOOKSHELF));
		c.set(cx + 6, fy + 1, cz + 9, s(Blocks.LECTERN));
	}

	/** The boss arena: a wide stone floor (34 x 24) with a solid wall and torches, two gates facing the bell. */
	private static void bossArena(Canvas c, int cx, int y, int cz) {
		int x0 = cx - 17, x1 = cx + 17, z0 = cz - 12, z1 = cz + 12;
		for (int x = x0; x <= x1; x++) {
			for (int z = z0; z <= z1; z++) {
				boolean wall = x == x0 || x == x1 || z == z0 || z == z1;
				c.fill(x, y - 3, z, x, y - 1, z, s(Blocks.STONE));
				c.set(x, y, z, wall ? s(Blocks.STONE_BRICKS) : (Math.abs(x - cx) + Math.abs(z - cz)) % 7 == 0 ? s(Blocks.CHISELED_STONE_BRICKS)
						: unit(x, z, 121) < 0.2 ? s(Blocks.CRACKED_STONE_BRICKS) : s(Blocks.STONE_BRICKS));
				c.fill(x, y + 1, z, x, y + 6, z, s(Blocks.AIR));
				if (wall) {
					boolean gate = z == z1 && Math.abs(x - cx) <= 2;
					if (!gate) {
						c.fill(x, y + 1, z, x, y + 3, z, aged(x, y + 2, z));
						c.set(x, y + 4, z, (x + z) % 2 == 0 ? s(Blocks.STONE_BRICK_WALL) : Kit.slab(Blocks.STONE_BRICK_SLAB, false));
						if ((x - x0) % 8 == 0 && (z == z0 || z == z1)) {
							c.set(x, y + 5, z, s(Blocks.TORCH));
						}
					}
				}
			}
		}
	}

	/**
	 * The giant root bridge from plateau A to plateau B: a thick, twisting root (dark oak wood) arching between the
	 * plateaus, flat-topped and four wide to walk on, with mossy patches, side tendrils and roots hanging beneath.
	 */
	private void rootBridge(Canvas c) {
		int ax = PA_X + PA_RX - 4, ay = PA_Y, az = PA_Z;
		int bx = PB_X - PB_RX + 4, by = PB_Y, bz = PB_Z;
		if (!c.near((ax + bx) / 2, (az + bz) / 2, 30)) {
			return;
		}
		int n = Math.max(Math.abs(bx - ax), Math.abs(bz - az));
		for (int i = 0; i <= n; i++) {
			double t = i / (double) n;
			double x = ax + (bx - ax) * t;
			double z = az + (bz - az) * t + Math.sin(t * Math.PI) * 6; // a sideways twist
			int top = (int) Math.round(ay + (by - ay) * t + Math.sin(t * Math.PI) * 4); // an arch
			int r = 3 + (int) Math.round(Math.sin(t * Math.PI * 3) * 0.6);
			for (int dx = -r - 1; dx <= r + 1; dx++) {
				for (int dz = -r - 1; dz <= r + 1; dz++) {
					for (int dy = -r - 2; dy <= 0; dy++) {
						double d = Math.sqrt(dx * dx + dz * dz + (dy + r * 0.6) * (dy + r * 0.6) * 0.8);
						if (d <= r + 0.5) {
							int bxp = (int) Math.round(x) + dx, bzp = (int) Math.round(z) + dz;
							boolean surface = dy == 0 && Math.abs(dz) <= 2;
							c.set(bxp, top + dy, bzp, surface ? (unit(bxp, bzp, 131) < 0.25 ? s(Blocks.MOSS_BLOCK) : s(Blocks.ROOTED_DIRT))
									: s(Blocks.DARK_OAK_WOOD));
							if (surface) {
								c.fill(bxp, top + 1, bzp, bxp, top + 3, bzp, s(Blocks.AIR));
							}
						}
					}
				}
			}
			// Bark ridges along the edges as a natural rail, and hanging roots below.
			int rx = (int) Math.round(x), rz = (int) Math.round(z);
			c.set(rx, top + 1, rz - 3, s(Blocks.DARK_OAK_WOOD));
			c.set(rx, top + 1, rz + 3, s(Blocks.DARK_OAK_WOOD));
			if (i % 5 == 2) {
				for (int k = r + 2; k < r + 6; k++) {
					c.set(rx, top - k, rz, s(Blocks.HANGING_ROOTS));
				}
			}
		}
	}
}
