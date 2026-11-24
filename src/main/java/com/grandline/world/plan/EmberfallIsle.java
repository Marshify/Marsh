package com.grandline.world.plan;

import com.grandline.world.plan.Anchor.Type;
import com.grandline.world.plan.Terrain.Ground;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.RailShape;

/**
 * Emberfall Isle, rebuilt: a dark volcanic island (about 160 x 160) seen from the south dock as a small village and
 * crimson trees in front, a towering active volcano in the middle, and a raised military plateau behind it.
 * <ul>
 * <li>South: the pier and a village of six dark-stone buildings (the mayor's house, barracks, storehouse, three homes)
 * round a small square; the quest giver stands outside the mayor's house facing the volcano.</li>
 * <li>Between village and volcano (east): the soldiers' training ground, the first combat area.</li>
 * <li>The volcano (base ~84 across, summit ~57 above the plain): ridged black slopes with lava grooves, a crater with a
 * lava lake and vents, two lava streams in banked channels to lava pools at the foot, smoke rings drawn by the client.</li>
 * <li>Inside: a lit tunnel from the training ground into a domed boss chamber with a lava lake, a central basalt
 * platform (quest bosses), bridges, ledges, magma veins and a lava fall from the crater.</li>
 * <li>Behind (north): a basalt plateau 12 blocks up, reached by a stair ramp on the east; the spies' area, the second
 * combat area. In its west cliff: the hidden mine (entrance, corridors, a drill chamber, a ladder shaft to the top).</li>
 * </ul>
 * Lava only sits in contained channels, pools and the crater; trees are crimson (fireproof) and wood is kept away from
 * lava. Island-local coordinates: +x east, +z south; built from code. Older worlds keep the original island.
 */
final class EmberfallIsle extends BasePlan {
	static final int G = 66;
	static final int VX = 0, VZ = -18;                 // volcano centre
	static final int VR = 42, VH = 60;                 // base radius, height above the sea
	static final int CHAMBER_Y = 70, CHAMBER_R = 17, CHAMBER_TOP = 92;
	static final int PLATEAU_Y = 78, PX1 = -44, PX2 = 44, PZ1 = -86, PZ2 = -62;
	static final int TRAIN_X = 20, TRAIN_Z = 30;
	static final int SPY_X = 0, SPY_Z = -74;
	static final int QUEST_X = -8, QUEST_Z = 44;
	static final int DOCK_LAND_Z = 72, DOCK_END_Z = 98, DOCK_Y = 64;

	static final double[] MAIN_ROAD = {0, 70, 0, 58, 1, 46, 1, 40};
	static final double[] TRAIN_ROAD = {1, 40, 8, 36, 14, 34};
	static final double[] TUNNEL_ROAD = {1, 40, 0, 32, 0, 27};
	static final double[] EAST_ROAD = {14, 34, 32, 22, 44, 0, 42, -30, 30, -48};
	static final double[] QUEST_LANE = {1, 44, -8, 44};
	static final double[] WEST_TRAIL = {-8, 44, -30, 38, -48, 12, -54, -20, -56, -54, -55, -74};
	static final double[] PLATEAU_PATH = {30, -62, 20, -70, 0, -76, -20, -76, -36, -72};

	/** A village building: wall box, floors, door side, use. */
	record House(int x1, int z1, int x2, int z2, int floors, Direction front, Use use) {
	}

	enum Use { MAYOR, BARRACKS, STORE, HOME_A, HOME_B, HOME_C }

	static final House[] HOUSES = {
			new House(-16, 47, -4, 55, 2, Direction.NORTH, Use.MAYOR),
			new House(8, 48, 18, 56, 1, Direction.WEST, Use.BARRACKS),
			new House(-24, 58, -14, 66, 1, Direction.EAST, Use.STORE),
			new House(8, 60, 15, 66, 1, Direction.WEST, Use.HOME_A),
			new House(-30, 47, -23, 54, 1, Direction.EAST, Use.HOME_B),
			new House(20, 60, 27, 67, 1, Direction.NORTH, Use.HOME_C)};

	static final int[][] CRIMSON = {{-36, 30}, {-28, 40}, {28, 46}, {36, 38}, {-40, 56}, {34, 58}, {-20, 34}, {40, 16}, {-44, 18},
			{12, 22}, {30, 12}, {-14, 70}};

	EmberfallIsle() {
		super("Emberfall Isle", 0.06);
		BlockState road = s(Blocks.POLISHED_BLACKSTONE_BRICKS), trail = s(Blocks.GRAVEL);
		terrain.mass(0, -6, 74, 72, 3.4, 3.6, Ground.BASALT)            // the island: a broad dark plain
				.mass(VX, VZ, VR, VR - 2, VH, 1.15, Ground.BASALT)        // the volcano
				.mass(-50, 30, 18, 12, 4.6, 2.6, Ground.BASALT)           // rocky ground west of the village
				.mass(52, 36, 16, 12, 4.2, 2.6, Ground.BASALT)
				// The rear military plateau: three overlapping lobes with steep, blended rock slopes (a natural shelf).
				.flat(-24, -74, 22, 13, PLATEAU_Y, 7, null).flat(24, -74, 22, 13, PLATEAU_Y, 7, null).flat(0, -72, 20, 12, PLATEAU_Y, 7, null)
				.flat(0, 58, 32, 14, G, 5, null)                           // the village
				.flat(TRAIN_X, TRAIN_Z, 14, 9, G, 5, null)                 // the soldiers' ground
				.flat(0, DOCK_LAND_Z - 2, 6, 3, 65, 3, s(Blocks.POLISHED_BLACKSTONE_BRICKS))
				.path(road, 4, MAIN_ROAD).path(road, 3, TRAIN_ROAD).path(road, 3, TUNNEL_ROAD).path(road, 3, EAST_ROAD)
				.path(road, 2.4, QUEST_LANE).path(trail, 2.4, WEST_TRAIL).path(s(Blocks.POLISHED_BLACKSTONE), 3, PLATEAU_PATH)
				.shoreMix(s(Blocks.BLACKSTONE), s(Blocks.BASALT), s(Blocks.GRAVEL), s(Blocks.TUFF), s(Blocks.BLACKSTONE))
				.reach(DOCK_END_Z + 4);
		anchor(Anchor.at(Type.QUEST, QUEST_X, G + 1, QUEST_Z, 180));                       // outside the mayor's house, facing the volcano
		anchor(new Anchor(Type.ARRIVAL, 0, Anchor.SURFACE, DOCK_LAND_Z - 3, 0, 180));
		anchor(Anchor.at(Type.HOME, 6, G + 1, 68, 90));                                    // the Innkeeper, by the dock road
		anchor(Anchor.at(Type.DOCK, -8, 63, DOCK_END_Z - 3));
		// NPCs and enemies follow IslandRosters.EMBERFALL; these anchors mark the same places for navigation and safe zones.
		anchor(Anchor.area(Type.ENEMY_CAMP, TRAIN_X, TRAIN_Z, 7));                         // Ashguard Troopers
		anchor(Anchor.at(Type.ENEMY_CAMP, SPY_X, PLATEAU_Y, SPY_Z, 8));                    // Cinder Infiltrators on the plateau
		anchor(Anchor.at(Type.BOSS_ARENA, VX, CHAMBER_Y + 1, VZ));                         // the Emberfall Marshal, in the chamber
		anchor(Anchor.of(Type.TREASURE, 46, -40));
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

	static BlockState stair(Block b, Direction f, boolean top) {
		return Kit.stairs(b, f, top);
	}

	static BlockState lantern(boolean hanging) {
		return s(Blocks.LANTERN).setValue(LanternBlock.HANGING, hanging);
	}

	static BlockState soulLantern(boolean hanging) {
		return s(Blocks.SOUL_LANTERN).setValue(LanternBlock.HANGING, hanging);
	}

	/** Distance from the volcano's axis, scaled so the base is 1. */
	static double volcanoD(int x, int z) {
		return Math.hypot((x - VX) / (double) VR, (z - VZ) / (double) (VR - 2));
	}

	static boolean built(int x, int z) {
		for (House h : HOUSES) {
			if (x >= h.x1() - 3 && x <= h.x2() + 3 && z >= h.z1() - 3 && z <= h.z2() + 3) {
				return true;
			}
		}
		return near(x, z, TRAIN_X, TRAIN_Z, 12) || (Math.abs(x) <= 8 && z >= 66) || near(x, z, QUEST_X, QUEST_Z, 4)
				|| (x >= PX1 - 2 && x <= PX2 + 2 && z >= PZ1 - 2 && z <= PZ2 + 2) || Math.abs(x) <= 4 && z >= 20 && z <= 30;
	}

	// ==================================================================================================== build

	@Override
	public void build(Canvas c) {
		groundCover(c);
		if (c.near(VX, VZ, VR + 4)) {
			volcano(c);
			chamber(c);
			lavaFall(c);
			tunnel(c);
		}
		lavaStreams(c);
		village(c);
		dock(c);
		if (c.near(TRAIN_X, TRAIN_Z, 16)) {
			trainingGround(c);
		}
		plateau(c);
		mine(c);
		for (int[] t : CRIMSON) {
			if (c.near(t[0], t[1], 6)) {
				crimsonTree(c, t[0], t[1], 6 + (int) (unit(t[0], t[1], 4) * 4));
			}
		}
		for (int[] l : new int[][]{{-3, 64}, {3, 64}, {-3, 54}, {6, 42}, {12, 36}, {2, 30}, {-2, 30}, {30, 24}, {44, -8}, {44, -28}}) {
			if (c.inChunk(l[0], l[1])) {
				int g = c.ground(l[0], l[1]);
				c.set(l[0], g + 1, l[1], s(Blocks.POLISHED_BLACKSTONE_WALL));
				c.set(l[0], g + 2, l[1], s(Blocks.POLISHED_BLACKSTONE_WALL));
				c.set(l[0], g + 3, l[1], lantern(false));
			}
		}
		treasure(c);
	}

	// ---------------------------------------------------------------------------------------------------- ground

	/**
	 * Dark ground: blackstone with tuff and andesite patches, gravel, scorched dirt, thin glowing magma fissures, and
	 * ridged volcano slopes (basalt ribs running downhill).
	 */
	private void groundCover(Canvas c) {
		for (int x = -84; x <= 84; x++) {
			for (int z = -96; z <= 84; z++) {
				if (!c.inChunk(x, z)) {
					continue;
				}
				int g = c.ground(x, z);
				if (g <= Terrain.WATER_TOP + 1 || c.terrain().paving(x, z) != null) {
					continue;
				}
				double r = unit(x, z, 7);
				double vd = volcanoD(x, z);
				if (vd < 1.0) {
					// Slopes: radial ribs of basalt and smooth basalt, a few magma seams, scattered rubble.
					double ang = Math.atan2(z - VZ, x - VX);
					double rib = Math.sin(ang * 22 + vd * 3);
					c.set(x, g, z, rib > 0.75 ? s(Blocks.BASALT) : rib < -0.85 ? s(Blocks.SMOOTH_BASALT) : r < 0.08 ? s(Blocks.TUFF)
							: r < 0.11 && vd > 0.3 ? s(Blocks.MAGMA_BLOCK) : s(Blocks.BLACKSTONE));
					continue;
				}
				if (built(x, z)) {
					continue;
				}
				double patch = (Math.sin(x * 0.19 + 0.4) + Math.sin(z * 0.15 + 1.7) + Math.sin((x - z) * 0.08)) / 3;
				double fissure = Math.abs(Math.sin(x * 0.11 + z * 0.07) + Math.sin(z * 0.13 - x * 0.05));
				if (fissure < 0.04 && z < 40) {
					c.set(x, g, z, s(Blocks.MAGMA_BLOCK));
				} else if (patch > 0.55) {
					c.set(x, g, z, r < 0.5 ? s(Blocks.TUFF) : s(Blocks.ANDESITE));
				} else if (patch < -0.6) {
					c.set(x, g, z, r < 0.5 ? s(Blocks.COARSE_DIRT) : s(Blocks.GRAVEL));
				} else if (r < 0.006) {
					c.set(x, g + 1, z, s(Blocks.DEAD_BUSH));
				}
			}
		}
	}

	/** A crimson tree: a dark crimson stem with two branches and a broad, layered canopy of nether wart and shroomlight. */
	static void crimsonTree(Canvas c, int x, int z, int h) {
		int g = c.ground(x, z);
		if (g <= Terrain.WATER_TOP + 1) {
			return;
		}
		c.fill(x, g + 1, z, x, g + h, z, s(Blocks.CRIMSON_STEM));
		int bx = unit(x, z, 9) < 0.5 ? 1 : -1;
		c.set(x + bx, g + h - 2, z, s(Blocks.CRIMSON_STEM).setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
		c.set(x + bx * 2, g + h - 1, z, s(Blocks.CRIMSON_STEM));
		c.set(x, g + h - 1, z - bx, s(Blocks.CRIMSON_STEM).setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z));
		for (int dy = h - 2; dy <= h + 2; dy++) {
			int r = dy == h + 2 ? 1 : dy == h - 2 ? 2 : 3;
			for (int dx = -r; dx <= r; dx++) {
				for (int dz = -r; dz <= r; dz++) {
					if (dx * dx + dz * dz <= r * r + 1 && (dx != 0 || dz != 0 || dy > h)) {
						double u = unit(x + dx, z + dz + dy, 10);
						if (u < 0.9 || Math.abs(dx) + Math.abs(dz) < r) {
							c.set(x + dx, g + dy, z + dz, u < 0.05 ? s(Blocks.SHROOMLIGHT) : s(Blocks.NETHER_WART_BLOCK));
						}
					}
				}
			}
		}
		// Hanging vines under the canopy.
		c.set(x + 2, g + h - 3, z, s(Blocks.WEEPING_VINES));
		c.set(x - 2, g + h - 3, z + 1, s(Blocks.WEEPING_VINES));
	}

	// ---------------------------------------------------------------------------------------------------- volcano

	/**
	 * The volcano's summit: a crater (radius 13) with thick walls, a jagged rim with protrusions, a lava lake ringed with
	 * magma, steaming vents (campfires under the rim stones), and a shaft dropping lava into the boss chamber below.
	 */
	private void volcano(Canvas c) {
		int peak = c.ground(VX, VZ);
		int lakeY = peak - 14;
		for (int dx = -16; dx <= 16; dx++) {
			for (int dz = -16; dz <= 16; dz++) {
				int x = VX + dx, z = VZ + dz;
				if (!c.inChunk(x, z)) {
					continue;
				}
				double d = Math.hypot(dx, dz) + unit(x, z, 21) * 1.2;
				int g = c.ground(x, z);
				if (d <= 13) {
					int floor = lakeY + (int) Math.round(Math.max(0, d - 9) * 1.4);
					c.fill(x, floor + 1, z, x, g + 3, z, s(Blocks.AIR));
					c.set(x, floor, z, d < 9 ? s(Blocks.LAVA) : s(Blocks.MAGMA_BLOCK));
					c.set(x, floor - 1, z, s(Blocks.BASALT));
				} else if (d <= 16) {
					// The rim: jagged, raised in places.
					int extra = (int) Math.round(unit(x / 2, z / 2, 22) * 4);
					c.fill(x, g + 1, z, x, g + extra, z, unit(x, z, 23) < 0.5 ? s(Blocks.BLACKSTONE) : s(Blocks.BASALT));
				}
			}
		}
		// Vents round the crater floor's edge (smoke columns).
		for (int k = 0; k < 5; k++) {
			double a = k * Math.PI * 2 / 5 + 0.4;
			int x = VX + (int) Math.round(Math.cos(a) * 11), z = VZ + (int) Math.round(Math.sin(a) * 11);
			if (c.inChunk(x, z)) {
				int floor = lakeY + 3;
				c.set(x, floor, z, s(Blocks.CAMPFIRE).setValue(CampfireBlock.SIGNAL_FIRE, true));
				c.set(x, floor - 1, z, s(Blocks.HAY_BLOCK));
			}
		}
		// Jagged protrusions and lava grooves on the outer slopes.
		for (int k = 0; k < 9; k++) {
			double a = k * Math.PI * 2 / 9 + 0.2;
			for (int i = 0; i < 6; i++) {
				double rr = 18 + i * 3.4;
				int x = VX + (int) Math.round(Math.cos(a) * rr), z = VZ + (int) Math.round(Math.sin(a) * rr);
				if (c.inChunk(x, z) && unit(x, z, 24) < 0.7) {
					int g = c.ground(x, z);
					int h = 1 + (int) (unit(x, z, 25) * 3);
					c.fill(x, g + 1, z, x, g + h, z, s(Blocks.BASALT));
				}
			}
		}
	}

	/**
	 * The two lava streams: from springs high on the south-west and east slopes, down banked channels (one block deep,
	 * blackstone banks either side) into lava pools at the foot, rimmed with magma. Every lava block is a source in a
	 * closed channel, so nothing spreads.
	 */
	private void lavaStreams(Canvas c) {
		lavaStream(c, new double[]{-6, -5, -12, 2, -20, 10, -26, 18}, -27, 21);
		lavaStream(c, new double[]{12, -16, 22, -18, 32, -12, 38, -4}, 40, -2);
	}

	private void lavaStream(Canvas c, double[] pts, int poolX, int poolZ) {
		java.util.Set<Long> done = new java.util.HashSet<>();
		for (int i = 0; i + 3 < pts.length; i += 2) {
			double ax = pts[i], az = pts[i + 1], bx = pts[i + 2], bz = pts[i + 3];
			int n = (int) Math.ceil(Math.max(Math.abs(bx - ax), Math.abs(bz - az)) * 2);
			for (int k = 0; k <= n; k++) {
				int x = (int) Math.round(ax + (bx - ax) * k / n), z = (int) Math.round(az + (bz - az) * k / n);
				if (!c.inChunk(x, z) || !done.add(((long) x << 32) ^ (z & 0xffffffffL))) {
					continue;
				}
				int g = c.ground(x, z);
				c.set(x, g, z, s(Blocks.LAVA));
				c.set(x, g - 1, z, s(Blocks.MAGMA_BLOCK));
				c.fill(x, g + 1, z, x, g + 2, z, s(Blocks.AIR));
			}
		}
		// Banks: every neighbour of a lava cell that isn't lava is raised one block above that cell.
		for (int i = 0; i + 3 < pts.length; i += 2) {
			double ax = pts[i], az = pts[i + 1], bx = pts[i + 2], bz = pts[i + 3];
			int n = (int) Math.ceil(Math.max(Math.abs(bx - ax), Math.abs(bz - az)) * 2);
			for (int k = 0; k <= n; k++) {
				int x = (int) Math.round(ax + (bx - ax) * k / n), z = (int) Math.round(az + (bz - az) * k / n);
				int g = c.ground(x, z);
				for (int[] o : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {-1, -1}, {1, -1}, {-1, 1}}) {
					int nx = x + o[0], nz = z + o[1];
					if (!c.inChunk(nx, nz) || done.contains(((long) nx << 32) ^ (nz & 0xffffffffL))) {
						continue;
					}
					int ng = c.ground(nx, nz);
					int top = Math.max(ng, g + 1);
					c.fill(nx, ng, nz, nx, top, nz, unit(nx, nz, 31) < 0.3 ? s(Blocks.MAGMA_BLOCK) : s(Blocks.BLACKSTONE));
				}
			}
		}
		// The pool at the foot.
		if (c.near(poolX, poolZ, 6)) {
			int g = c.ground(poolX, poolZ);
			for (int dx = -4; dx <= 4; dx++) {
				for (int dz = -3; dz <= 3; dz++) {
					double d = Math.hypot(dx / 4.0, dz / 3.0);
					int x = poolX + dx, z = poolZ + dz;
					if (d <= 1.0) {
						c.set(x, g, z, d < 0.7 ? s(Blocks.LAVA) : s(Blocks.MAGMA_BLOCK));
						c.set(x, g - 1, z, s(Blocks.BLACKSTONE));
						c.fill(x, g + 1, z, x, g + 3, z, s(Blocks.AIR));
					} else if (d <= 1.3) {
						c.fill(x, c.ground(x, z), z, x, g + 1, z, s(Blocks.BLACKSTONE));
					}
				}
			}
		}
	}

	/**
	 * The boss chamber inside the volcano: a dome (radius 17, floor y 70, top y 92) with a lava lake round a central
	 * basalt platform (quest bosses stand here), two blackstone bridges to it, a floor ledge round the lake, jagged
	 * basalt columns, magma veins in the walls, soul lanterns, and the lava fall from the crater.
	 */
	private void chamber(Canvas c) {
		for (int dx = -CHAMBER_R - 1; dx <= CHAMBER_R + 1; dx++) {
			for (int dz = -CHAMBER_R - 1; dz <= CHAMBER_R + 1; dz++) {
				int x = VX + dx, z = VZ + dz;
				if (!c.inChunk(x, z)) {
					continue;
				}
				double d = Math.hypot(dx, dz);
				if (d > CHAMBER_R + 0.5) {
					continue;
				}
				int roof = CHAMBER_TOP - (int) Math.round(d * d / (CHAMBER_R * CHAMBER_R) * 18);
				c.fill(x, CHAMBER_Y - 3, z, x, CHAMBER_Y - 1, z, s(Blocks.BASALT));
				boolean lake = d <= 10 && d > 3.6;
				boolean bridge = (Math.abs(dx) <= 1 && dz > 0) || (Math.abs(dz) <= 1 && dx < 0);
				if (d <= 3.6) {
					c.set(x, CHAMBER_Y, z, s(Blocks.POLISHED_BASALT)); // the boss platform
				} else if (lake && !bridge) {
					c.set(x, CHAMBER_Y, z, s(Blocks.LAVA));
					c.set(x, CHAMBER_Y - 1, z, s(Blocks.MAGMA_BLOCK));
				} else if (lake) {
					c.set(x, CHAMBER_Y, z, s(Blocks.POLISHED_BLACKSTONE_BRICKS));
				} else {
					c.set(x, CHAMBER_Y, z, unit(x, z, 41) < 0.15 ? s(Blocks.MAGMA_BLOCK) : s(Blocks.BLACKSTONE));
				}
				c.fill(x, CHAMBER_Y + 1, z, x, roof, z, s(Blocks.AIR));
				// Walls and roof: blackstone with magma veins.
				c.set(x, roof + 1, z, unit(x, roof, 42) < 0.12 ? s(Blocks.MAGMA_BLOCK) : s(Blocks.BLACKSTONE));
				if (d > CHAMBER_R - 0.6) {
					for (int y = CHAMBER_Y + 1; y <= roof; y++) {
						c.set(x, y, z, unit(x, y, z * 3) < 0.1 ? s(Blocks.MAGMA_BLOCK) : s(Blocks.BLACKSTONE));
					}
				}
			}
		}
		// Jagged basalt columns on the ledge.
		for (int k = 0; k < 7; k++) {
			double a = k * Math.PI * 2 / 7 + 0.6;
			int x = VX + (int) Math.round(Math.cos(a) * 14), z = VZ + (int) Math.round(Math.sin(a) * 14);
			if (c.inChunk(x, z) && !(Math.abs(x - VX) <= 2 && z > VZ)) {
				int h = 4 + (int) (unit(x, z, 43) * 6);
				c.fill(x, CHAMBER_Y + 1, z, x, CHAMBER_Y + h, z, s(Blocks.BASALT));
				c.set(x, CHAMBER_Y + 2, z + 1, soulLantern(false));
			}
		}
		// Lanterns over the bridges, edge of the platform.
		c.set(VX + 2, CHAMBER_Y + 1, VZ + 2, soulLantern(false));
		c.set(VX - 2, CHAMBER_Y + 1, VZ - 2, soulLantern(false));
	}

	/** The lava fall: from the crater's lava lake straight down through the rock and the chamber into its lava lake. */
	private void lavaFall(Canvas c) {
		int x = VX + 4, z = VZ + 4;
		if (!c.inChunk(x, z)) {
			return;
		}
		int lakeY = c.ground(VX, VZ) - 14;
		for (int y = lakeY - 1; y > CHAMBER_Y; y--) {
			c.set(x, y, z, s(Blocks.LAVA).setValue(LiquidBlock.LEVEL, 8));
		}
	}

	/** The tunnel from the training ground (south) into the chamber: 4 wide, 5 high, framed in blackstone, lantern-lit. */
	private void tunnel(Canvas c) {
		int z0 = 28, z1 = VZ + CHAMBER_R - 1;
		for (int z = z1; z <= z0; z++) {
			int floor = z >= 20 ? G : z <= z1 + 3 ? CHAMBER_Y : G + (int) Math.round((20 - z) * (CHAMBER_Y - G) / (double) (20 - z1 - 3));
			for (int x = -2; x <= 1; x++) {
				c.set(x, floor, z, s(Blocks.POLISHED_BLACKSTONE_BRICKS));
				c.fill(x, floor + 1, z, x, floor + 5, z, s(Blocks.AIR));
			}
			for (int x : new int[]{-3, 2}) {
				c.fill(x, floor + 1, z, x, floor + 5, z, unit(x, z, 51) < 0.12 ? s(Blocks.MAGMA_BLOCK) : s(Blocks.POLISHED_BLACKSTONE));
				if (z % 5 == 0) {
					c.set(x == -3 ? -2 : 1, floor + 4, z, soulLantern(true));
				}
			}
			c.fill(-3, floor + 6, z, 2, floor + 6, z, s(Blocks.BLACKSTONE));
		}
		// The portal at the mountain's foot: a frame of polished blackstone with chains.
		int fz = z0;
		for (int x : new int[]{-4, 3}) {
			c.fill(x, G + 1, fz, x, G + 7, fz, s(Blocks.POLISHED_BLACKSTONE_BRICKS));
			c.set(x, G + 8, fz, s(Blocks.CHISELED_POLISHED_BLACKSTONE));
		}
		c.fill(-3, G + 7, fz, 2, G + 7, fz, s(Blocks.POLISHED_BLACKSTONE_BRICKS));
		c.set(-2, G + 6, fz, s(Blocks.CHAIN));
		c.set(1, G + 6, fz, s(Blocks.CHAIN));
	}

	// ---------------------------------------------------------------------------------------------------- village

	private void village(Canvas c) {
		if (!c.near(0, 58, 36)) {
			return;
		}
		for (House h : HOUSES) {
			house(c, h);
		}
		// The square: blackstone paving with a brazier.
		for (int x = -6; x <= 4; x++) {
			for (int z = 56; z <= 64; z++) {
				if (Math.hypot(x + 1, z - 60) <= 5.2) {
					c.set(x, G, z, unit(x, z, 61) < 0.3 ? s(Blocks.POLISHED_BLACKSTONE) : s(Blocks.POLISHED_BLACKSTONE_BRICKS));
					c.fill(x, G + 1, z, x, G + 3, z, s(Blocks.AIR));
				}
			}
		}
		c.set(-1, G + 1, 60, s(Blocks.POLISHED_BLACKSTONE_BRICKS));
		c.set(-1, G + 2, 60, s(Blocks.CAMPFIRE));
		for (int[] b : new int[][]{{-4, 58}, {2, 58}, {-4, 62}, {2, 62}}) {
			c.set(b[0], G + 1, b[1], Canvas.facing(s(Blocks.BARREL), Direction.UP));
		}
		// The quest giver's post outside the mayor's house.
		c.fill(QUEST_X - 2, G + 1, QUEST_Z, QUEST_X - 2, G + 2, QUEST_Z, s(Blocks.POLISHED_BLACKSTONE_WALL));
		c.set(QUEST_X - 2, G + 3, QUEST_Z, lantern(false));
		c.set(QUEST_X + 2, G + 1, QUEST_Z, s(Blocks.LECTERN));
	}

	/**
	 * A volcanic-stone building: a polished blackstone plinth, stone brick base course, andesite walls between dark oak
	 * posts, deepslate-tile roof (flat with a parapet for the barracks and storehouse, a hipped for the homes and the
	 * mayor's house), small windows, an open door with a step and a lantern, and furniture.
	 */
	private static void house(Canvas c, House h) {
		int fy = G + 1, top = fy + 4 * h.floors();
		c.foundation(h.x1(), h.z1(), h.x2(), h.z2(), fy, s(Blocks.POLISHED_BLACKSTONE_BRICKS));
		for (int x = h.x1(); x <= h.x2(); x++) {
			for (int z = h.z1(); z <= h.z2(); z++) {
				boolean ex = x == h.x1() || x == h.x2(), ez = z == h.z1() || z == h.z2();
				if (!ex && !ez) {
					c.set(x, fy, z, s(Blocks.SPRUCE_PLANKS));
					c.fill(x, fy + 1, z, x, top - 1, z, s(Blocks.AIR));
					for (int f = 1; f < h.floors(); f++) {
						c.set(x, fy + 4 * f, z, s(Blocks.DARK_OAK_PLANKS));
					}
					continue;
				}
				boolean post = (ex && ez) || (ex ? (z - h.z1()) % 4 == 0 : (x - h.x1()) % 4 == 0);
				for (int y = fy + 1; y < top; y++) {
					BlockState b = post ? s(Blocks.DARK_OAK_LOG) : (y - fy) % 4 == 0 ? s(Blocks.STRIPPED_DARK_OAK_LOG)
							.setValue(RotatedPillarBlock.AXIS, ex ? Direction.Axis.Z : Direction.Axis.X) : (y - fy) % 4 == 1 ? s(Blocks.STONE_BRICKS) : s(Blocks.ANDESITE);
					c.set(x, y, z, b);
				}
				c.set(x, top, z, s(Blocks.POLISHED_DEEPSLATE));
			}
		}
		for (int f = 0; f < h.floors(); f++) {
			int y = fy + 2 + 4 * f;
			for (int x = h.x1() + 2; x < h.x2(); x += 4) {
				c.set(x, y, h.z1(), Kit.connected(Blocks.IRON_BARS, true));
				c.set(x, y, h.z2(), Kit.connected(Blocks.IRON_BARS, true));
			}
			for (int z = h.z1() + 2; z < h.z2(); z += 4) {
				c.set(h.x1(), y, z, Kit.connected(Blocks.IRON_BARS, false));
				c.set(h.x2(), y, z, Kit.connected(Blocks.IRON_BARS, false));
			}
		}
		int dx = h.front() == Direction.EAST ? h.x2() : h.front() == Direction.WEST ? h.x1() : (h.x1() + h.x2()) / 2;
		int dz = h.front() == Direction.NORTH ? h.z1() : h.front() == Direction.SOUTH ? h.z2() : (h.z1() + h.z2()) / 2;
		BlockState door = s(Blocks.DARK_OAK_DOOR).setValue(BlockStateProperties.HORIZONTAL_FACING, h.front()).setValue(BlockStateProperties.OPEN, true)
				.setValue(BlockStateProperties.DOOR_HINGE, DoorHingeSide.LEFT);
		c.set(dx, fy + 1, dz, door.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER));
		c.set(dx, fy + 2, dz, door.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER));
		int ox = dx + h.front().getStepX(), oz = dz + h.front().getStepZ();
		c.set(ox, fy, oz, stair(Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS, h.front().getOpposite(), false));
		Direction cw = h.front().getClockWise();
		c.set(ox + cw.getStepX(), fy + 3, oz + cw.getStepZ(), lantern(true));
		boolean flat = h.use() == Use.BARRACKS || h.use() == Use.STORE;
		if (flat) {
			for (int x = h.x1(); x <= h.x2(); x++) {
				for (int z = h.z1(); z <= h.z2(); z++) {
					c.set(x, top, z, s(Blocks.DEEPSLATE_TILES));
					if (x == h.x1() || x == h.x2() || z == h.z1() || z == h.z2()) {
						c.set(x, top + 1, z, (x + z) % 2 == 0 ? s(Blocks.DEEPSLATE_TILE_WALL) : Kit.slab(Blocks.DEEPSLATE_TILE_SLAB, false));
					}
				}
			}
		} else {
			// A hipped roof of deepslate tiles: a one-block overhang, rising a block per row to a slab ridge, solid
			// underneath (it is also the ceiling of the top floor).
			Kit.hipRoof(c, h.x1() - 1, h.z1() - 1, h.x2() + 1, h.z2() + 1, top, Blocks.DEEPSLATE_TILE_STAIRS, Blocks.DEEPSLATE_TILE_SLAB,
					Blocks.DEEPSLATE_TILES);
		}
		furnish(c, h, fy);
	}

	private static void bed(Canvas c, int x, int y, int z, Block bed, Direction head) {
		c.set(x, y, z, s(bed).setValue(BedBlock.FACING, head).setValue(BedBlock.PART, BedPart.HEAD));
		c.set(x - head.getStepX(), y, z - head.getStepZ(), s(bed).setValue(BedBlock.FACING, head).setValue(BedBlock.PART, BedPart.FOOT));
	}

	private static void furnish(Canvas c, House h, int fy) {
		int y = fy + 1;
		switch (h.use()) {
			case MAYOR -> {
				// Ground floor: an office with a desk, chairs, shelves; upstairs: a bedroom; a ladder between.
				c.set(h.x1() + 3, y, h.z2() - 2, Kit.slab(Blocks.DARK_OAK_SLAB, true));
				c.set(h.x1() + 4, y, h.z2() - 2, Kit.slab(Blocks.DARK_OAK_SLAB, true));
				c.set(h.x1() + 3, y + 1, h.z2() - 2, s(Blocks.LANTERN));
				c.set(h.x1() + 4, y, h.z2() - 1, stair(Blocks.DARK_OAK_STAIRS, Direction.NORTH, false));
				c.set(h.x1() + 3, y, h.z2() - 3, stair(Blocks.DARK_OAK_STAIRS, Direction.SOUTH, false));
				for (int x = h.x1() + 1; x <= h.x1() + 5; x++) {
					c.set(x, y, h.z2() - 1, s(Blocks.BOOKSHELF));
					c.set(x, y + 1, h.z2() - 1, s(Blocks.BOOKSHELF));
				}
				c.set(h.x1() + 4, y, h.z2() - 1, s(Blocks.LECTERN));
				for (int yy = y; yy <= fy + 4; yy++) {
					c.set(h.x2() - 1, yy, h.z2() - 1, s(Blocks.LADDER).setValue(LadderBlock.FACING, Direction.NORTH));
				}
				bed(c, h.x1() + 1, fy + 5, h.z2() - 2, Blocks.RED_BED, Direction.SOUTH);
				c.set(h.x1() + 2, fy + 5, h.z2() - 1, s(Blocks.CHEST));
				c.set((h.x1() + h.x2()) / 2, fy + 7, (h.z1() + h.z2()) / 2, lantern(true));
				c.set((h.x1() + h.x2()) / 2, fy + 3, (h.z1() + h.z2()) / 2, lantern(true));
				// The porch.
				for (int x = (h.x1() + h.x2()) / 2 - 2; x <= (h.x1() + h.x2()) / 2 + 2; x++) {
					c.set(x, fy + 4, h.z1() - 1, Kit.slab(Blocks.DEEPSLATE_TILE_SLAB, false));
				}
				c.fill((h.x1() + h.x2()) / 2 - 2, fy + 1, h.z1() - 2, (h.x1() + h.x2()) / 2 - 2, fy + 3, h.z1() - 2, s(Blocks.DARK_OAK_FENCE));
				c.fill((h.x1() + h.x2()) / 2 + 2, fy + 1, h.z1() - 2, (h.x1() + h.x2()) / 2 + 2, fy + 3, h.z1() - 2, s(Blocks.DARK_OAK_FENCE));
				for (int x = (h.x1() + h.x2()) / 2 - 2; x <= (h.x1() + h.x2()) / 2 + 2; x++) {
					c.set(x, fy + 4, h.z1() - 2, Kit.slab(Blocks.DEEPSLATE_TILE_SLAB, false));
				}
			}
			case BARRACKS -> {
				for (int z = h.z1() + 1; z <= h.z2() - 2; z += 2) {
					bed(c, h.x2() - 1, y, z, Blocks.GRAY_BED, Direction.EAST);
				}
				c.fill(h.x1() + 2, y, h.z1() + 1, h.x1() + 2, y + 1, h.z1() + 1, s(Blocks.DARK_OAK_FENCE));
				c.fill(h.x1() + 4, y, h.z1() + 1, h.x1() + 4, y + 1, h.z1() + 1, s(Blocks.DARK_OAK_FENCE));
				c.set(h.x1() + 3, y + 1, h.z1() + 1, Kit.slab(Blocks.DARK_OAK_SLAB, true));
				c.set(h.x1() + 3, y, h.z1() + 1, s(Blocks.GRINDSTONE));
				c.set(h.x1() + 2, y, h.z2() - 1, s(Blocks.HAY_BLOCK));
				c.set(h.x1() + 2, y + 1, h.z2() - 1, Canvas.facing(s(Blocks.CARVED_PUMPKIN), Direction.EAST)); // a training dummy
			}
			case STORE -> {
				for (int z = h.z1() + 1; z <= h.z2() - 1; z++) {
					c.set(h.x1() + 1, y, z, Canvas.facing(s(Blocks.BARREL), Direction.UP));
					if (z % 2 == 0) {
						c.set(h.x1() + 1, y + 1, z, Canvas.facing(s(Blocks.BARREL), Direction.UP));
					}
					c.set(h.x1() + 3, y, z, z % 3 == 0 ? s(Blocks.CHEST) : s(Blocks.BOOKSHELF));
				}
				c.set(h.x2() - 2, y, h.z1() + 1, s(Blocks.HAY_BLOCK));
				c.set(h.x2() - 2, y, h.z2() - 1, s(Blocks.COMPOSTER));
			}
			default -> {
				bed(c, h.x1() + 1, y, h.z1() + 2, Blocks.BROWN_BED, Direction.NORTH);
				c.set(h.x2() - 1, y, h.z1() + 1, s(Blocks.FURNACE));
				c.set(h.x2() - 1, y, h.z2() - 1, Canvas.facing(s(Blocks.BARREL), Direction.UP));
				c.set(h.x1() + 3, y, h.z2() - 2, s(Blocks.DARK_OAK_FENCE));
				c.set(h.x1() + 3, y + 1, h.z2() - 2, s(Blocks.DARK_OAK_PRESSURE_PLATE));
				c.set((h.x1() + h.x2()) / 2, fy + 3, (h.z1() + h.z2()) / 2, lantern(true));
			}
		}
	}

	/** The pier: a five-wide deck on blackstone footings with rails and lanterns, cargo on the T-head. */
	private void dock(Canvas c) {
		if (!c.near(0, (DOCK_LAND_Z + DOCK_END_Z) / 2, 20)) {
			return;
		}
		for (int z = DOCK_LAND_Z; z <= DOCK_END_Z; z++) {
			boolean head = z >= DOCK_END_Z - 5;
			int half = head ? 7 : 2;
			for (int x = -half; x <= half; x++) {
				boolean edgeX = Math.abs(x) == half;
				c.set(x, DOCK_Y, z, s(Blocks.DARK_OAK_PLANKS));
				c.fill(x, DOCK_Y + 1, z, x, DOCK_Y + 4, z, s(Blocks.AIR));
				boolean post = edgeX && ((z - DOCK_LAND_Z) % 4 == 0 || z == DOCK_END_Z);
				if (post) {
					c.fill(x, Math.min(c.ground(x, z), Terrain.WATER_TOP - 1), z, x, DOCK_Y - 1, z, s(Blocks.POLISHED_BLACKSTONE_BRICKS));
					c.set(x, DOCK_Y + 1, z, s(Blocks.DARK_OAK_FENCE));
					if ((z - DOCK_LAND_Z) % 8 == 0) {
						c.set(x, DOCK_Y + 2, z, lantern(false));
					}
				} else if (edgeX && !head && (z - DOCK_LAND_Z) % 8 != 6) {
					c.set(x, DOCK_Y + 1, z, Kit.connected(Blocks.DARK_OAK_FENCE, false));
				}
			}
		}
		c.set(-6, DOCK_Y + 1, DOCK_END_Z - 3, Canvas.facing(s(Blocks.BARREL), Direction.UP));
		c.set(-5, DOCK_Y + 1, DOCK_END_Z - 3, Canvas.facing(s(Blocks.BARREL), Direction.UP));
		c.set(-6, DOCK_Y + 2, DOCK_END_Z - 3, Canvas.facing(s(Blocks.BARREL), Direction.UP));
		// Accessibility: a boat landing one block above the water, with stairs up to the deck (see Kit.boatLanding).
		for (int x = -1; x <= 1; x++) { // the dock's first boards step up to the shore
			c.set(x, DOCK_Y + 1, DOCK_LAND_Z, Kit.stairs(Blocks.DARK_OAK_STAIRS, Direction.NORTH, false));
		}
		Kit.stepsUp(c, -5, DOCK_LAND_Z - 6, 5, DOCK_LAND_Z - 6, Direction.NORTH, Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS);
		Kit.boatLanding(c, 2, DOCK_LAND_Z + 14, Direction.EAST, 0, 1, DOCK_Y, Blocks.DARK_OAK_STAIRS,
				s(Blocks.DARK_OAK_PLANKS), s(Blocks.POLISHED_BLACKSTONE_BRICKS), Blocks.DARK_OAK_FENCE);
	}

	// ---------------------------------------------------------------------------------------------------- combat areas

	/** The soldiers' training ground: open blackstone, three small tents, weapon racks, crates, low stone barriers. */
	private void trainingGround(Canvas c) {
		int y = G;
		for (int x = TRAIN_X - 13; x <= TRAIN_X + 13; x++) {
			for (int z = TRAIN_Z - 8; z <= TRAIN_Z + 8; z++) {
				if (c.inChunk(x, z)) {
					c.set(x, y, z, unit(x, z, 71) < 0.25 ? s(Blocks.POLISHED_BLACKSTONE) : s(Blocks.BLACKSTONE));
					c.fill(x, y + 1, z, x, y + 4, z, s(Blocks.AIR));
				}
			}
		}
		for (int[] t : new int[][]{{TRAIN_X - 10, TRAIN_Z - 5}, {TRAIN_X + 10, TRAIN_Z - 5}, {TRAIN_X + 10, TRAIN_Z + 5}}) {
			tent(c, t[0], t[1], y);
		}
		for (int[] b : new int[][]{{-6, 6, 4}, {4, -7, 3}, {-12, 1, 3}}) {
			for (int k = 0; k < b[2]; k++) {
				int x = TRAIN_X + b[0] + (b[0] == -12 ? 0 : k), z = TRAIN_Z + b[1] + (b[0] == -12 ? k : 0);
				c.set(x, y + 1, z, s(Blocks.STONE_BRICKS));
				if (k % 2 == 0) {
					c.set(x, y + 2, z, Kit.slab(Blocks.STONE_BRICK_SLAB, false));
				}
			}
		}
		for (int[] r : new int[][]{{TRAIN_X - 4, TRAIN_Z + 7}, {TRAIN_X + 3, TRAIN_Z + 7}}) {
			c.fill(r[0], y + 1, r[1], r[0], y + 2, r[1], s(Blocks.DARK_OAK_FENCE));
			c.fill(r[0] + 2, y + 1, r[1], r[0] + 2, y + 2, r[1], s(Blocks.DARK_OAK_FENCE));
			c.set(r[0] + 1, y + 2, r[1], Kit.slab(Blocks.DARK_OAK_SLAB, true));
			c.set(r[0] + 1, y + 1, r[1], s(Blocks.LIGHTNING_ROD));
		}
		c.set(TRAIN_X + 12, y + 1, TRAIN_Z, Canvas.facing(s(Blocks.BARREL), Direction.UP));
		c.set(TRAIN_X + 12, y + 1, TRAIN_Z + 1, Canvas.facing(s(Blocks.BARREL), Direction.UP));
		c.set(TRAIN_X + 12, y + 2, TRAIN_Z, Canvas.facing(s(Blocks.BARREL), Direction.UP));
	}

	/** A small military tent: grey canvas on a dark frame, open at the front. */
	private static void tent(Canvas c, int x, int z, int y) {
		for (int i = -1; i <= 1; i++) {
			for (int k = 0; k <= 1; k++) {
				c.set(x - 2 + k, y + 1 + k, z + i, s(Blocks.GRAY_WOOL));
				c.set(x + 2 - k, y + 1 + k, z + i, s(Blocks.GRAY_WOOL));
			}
			c.set(x, y + 3, z + i, s(Blocks.DARK_OAK_PLANKS));
		}
		c.set(x, y + 1, z - 1, s(Blocks.GRAY_CARPET));
	}

	/**
	 * The rear plateau (12 up, a basalt shelf with sheer, jagged edges): a stair ramp up its east side, a patrol path,
	 * three small military huts, low walls, training posts, and the open spies' ground in the middle.
	 */
	private void plateau(Canvas c) {
		if (!c.near(0, (PZ1 + PZ2) / 2, 50)) {
			return;
		}
		int y = PLATEAU_Y;
		// The stair ramp up the plateau's south-east slope (from the east road at y 66 to the top at 78).
		for (int i = 0; i <= 12; i++) {
			int z = -49 - i, yy = G + i;
			for (int x = 28; x <= 31; x++) {
				c.fill(x, G - 2, z, x, yy - 1, z, s(Blocks.BLACKSTONE));
				c.set(x, yy, z, i == 12 ? s(Blocks.POLISHED_BLACKSTONE_BRICKS) : stair(Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS, Direction.NORTH, false));
				c.fill(x, yy + 1, z, x, yy + 4, z, s(Blocks.AIR));
			}
			c.set(27, yy + 1, z, s(Blocks.POLISHED_BLACKSTONE_WALL));
			c.set(32, yy + 1, z, s(Blocks.POLISHED_BLACKSTONE_WALL));
		}
		// Huts.
		for (int[] h : new int[][]{{-34, -80}, {26, -82}, {-10, -82}}) {
			for (int dx = 0; dx <= 5; dx++) {
				for (int dz = 0; dz <= 4; dz++) {
					boolean wall = dx == 0 || dx == 5 || dz == 0 || dz == 4;
					int x = h[0] + dx, z = h[1] + dz;
					c.set(x, y, z, s(Blocks.POLISHED_DEEPSLATE));
					for (int yy = y + 1; yy <= y + 3; yy++) {
						boolean door = dz == 4 && dx == 2 && yy <= y + 2;
						c.set(x, yy, z, wall && !door ? s(Blocks.DEEPSLATE_BRICKS) : s(Blocks.AIR));
					}
					c.set(x, y + 4, z, Kit.slab(Blocks.DEEPSLATE_TILE_SLAB, false));
				}
			}
			c.set(h[0] + 1, y + 1, h[1] + 1, Canvas.facing(s(Blocks.BARREL), Direction.UP));
			c.set(h[0] + 4, y + 1, h[1] + 1, s(Blocks.CRAFTING_TABLE));
			c.set(h[0] + 2, y + 3, h[1] + 2, lantern(true));
		}
		// Low defensive walls and training posts round the open middle.
		for (int x = -24; x <= 24; x += 8) {
			for (int k = 0; k < 3; k++) {
				c.set(x + k, y + 1, SPY_Z + 11, s(Blocks.DEEPSLATE_BRICK_WALL));
			}
		}
		for (int[] p : new int[][]{{-14, -70}, {14, -70}, {-14, -78}, {14, -78}}) {
			c.fill(p[0], y + 1, p[1], p[0], y + 2, p[1], s(Blocks.DARK_OAK_FENCE));
			c.set(p[0], y + 3, p[1], s(Blocks.TARGET));
		}
		for (int[] l : new int[][]{{-30, -64}, {0, -64}, {30, -64}, {-36, -74}, {36, -74}}) {
			c.set(l[0], y + 1, l[1], s(Blocks.POLISHED_BLACKSTONE_WALL));
			c.set(l[0], y + 2, l[1], lantern(false));
		}
	}

	/**
	 * The hidden mine in the plateau's west cliff: a timber-framed entrance in the rock wall, a corridor east with support
	 * beams and lanterns, a drill chamber (a machine of iron and pistons boring into a magma seam, rails, crates), and a
	 * ladder shaft up to a hatch-hut on the plateau (the second exit).
	 */
	private void mine(Canvas c) {
		if (!c.near(-20, -74, 36)) {
			return;
		}
		int fy = G, ez = -74;
		// The entrance: a timber frame where the trail meets the foot of the plateau's west slope, the corridor cut
		// into the rock behind it.
		int ex = -54;
		for (int z = ez - 2; z <= ez + 2; z++) {
			c.fill(ex, fy + 1, z, ex, fy + 5, z, Math.abs(z - ez) == 2 ? s(Blocks.SPRUCE_LOG) : s(Blocks.AIR));
		}
		c.fill(ex, fy + 5, ez - 2, ex, fy + 5, ez + 2, s(Blocks.STRIPPED_SPRUCE_LOG).setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z));
		c.set(ex - 1, fy + 4, ez - 2, lantern(false));
		// Corridor east (3 wide, 4 high) with frames every 4 blocks.
		for (int x = ex; x <= -18; x++) {
			for (int z = ez - 1; z <= ez + 1; z++) {
				c.set(x, fy, z, s(Blocks.GRAVEL));
				c.fill(x, fy + 1, z, x, fy + 4, z, s(Blocks.AIR));
			}
			c.set(x, fy + 1, ez, s(Blocks.RAIL).setValue(BlockStateProperties.RAIL_SHAPE, RailShape.EAST_WEST));
			if ((x - ex) % 4 == 0) {
				c.fill(x, fy + 1, ez - 2, x, fy + 4, ez - 2, s(Blocks.SPRUCE_LOG));
				c.fill(x, fy + 1, ez + 2, x, fy + 4, ez + 2, s(Blocks.SPRUCE_LOG));
				c.fill(x, fy + 5, ez - 2, x, fy + 5, ez + 2, s(Blocks.STRIPPED_SPRUCE_LOG).setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z));
				c.set(x, fy + 4, ez, lantern(true));
			}
		}
		// Drill chamber (x -18..-2, z -82..-66, 8 high).
		for (int x = -18; x <= -2; x++) {
			for (int z = -82; z <= -66; z++) {
				c.set(x, fy, z, unit(x, z, 91) < 0.3 ? s(Blocks.GRAVEL) : s(Blocks.STONE));
				c.fill(x, fy + 1, z, x, fy + 8, z, s(Blocks.AIR));
			}
		}
		for (int[] p : new int[][]{{-17, -81}, {-3, -81}, {-17, -67}, {-3, -67}}) {
			c.fill(p[0], fy + 1, p[1], p[0], fy + 8, p[1], s(Blocks.SPRUCE_LOG));
		}
		// The drill: an iron housing on a frame, piston heads and a bit of pointed dripstone boring into a magma seam.
		int dx = -10, dz = -79;
		c.fill(dx - 2, fy + 1, dz, dx + 2, fy + 1, dz + 2, s(Blocks.IRON_BLOCK));
		c.fill(dx - 1, fy + 2, dz, dx + 1, fy + 4, dz + 1, s(Blocks.IRON_BLOCK));
		c.set(dx, fy + 5, dz, s(Blocks.OBSERVER));
		c.set(dx, fy + 2, dz - 1, s(Blocks.PISTON).setValue(BlockStateProperties.FACING, Direction.NORTH));
		c.set(dx - 1, fy + 2, dz - 1, s(Blocks.PISTON).setValue(BlockStateProperties.FACING, Direction.NORTH));
		c.set(dx + 1, fy + 2, dz - 1, s(Blocks.PISTON).setValue(BlockStateProperties.FACING, Direction.NORTH));
		c.fill(dx - 2, fy, dz - 3, dx + 2, fy + 3, dz - 3, s(Blocks.MAGMA_BLOCK));
		c.set(dx + 3, fy + 1, dz + 1, s(Blocks.REDSTONE_LAMP));
		c.set(dx - 3, fy + 1, dz + 1, s(Blocks.REDSTONE_LAMP));
		for (int[] b : new int[][]{{-16, -70}, {-15, -70}, {-16, -71}, {-5, -70}, {-4, -70}}) {
			c.set(b[0], fy + 1, b[1], Canvas.facing(s(Blocks.BARREL), Direction.UP));
		}
		c.set(-10, fy + 7, -74, lantern(true));
		c.set(-14, fy + 7, -70, lantern(true));
		c.set(-6, fy + 7, -78, lantern(true));
		// Second exit: a ladder shaft at (-4, -68) up to the plateau, under a small hut.
		// The shaft is in solid rock just south of the chamber; its ladder hangs on the rock behind it.
		for (int y = fy + 1; y <= PLATEAU_Y; y++) {
			c.set(-4, y, -65, s(Blocks.LADDER).setValue(LadderBlock.FACING, Direction.NORTH));
		}
		c.set(-4, fy + 1, -66, s(Blocks.AIR));
		c.set(-4, fy + 2, -66, s(Blocks.AIR));
		for (int[] p : new int[][]{{-6, -67}, {-2, -67}, {-6, -63}, {-2, -63}}) {
			c.fill(p[0], PLATEAU_Y + 1, p[1], p[0], PLATEAU_Y + 3, p[1], s(Blocks.SPRUCE_FENCE));
		}
		c.fill(-6, PLATEAU_Y + 4, -67, -2, PLATEAU_Y + 4, -63, Kit.slab(Blocks.SPRUCE_SLAB, false));
	}
}
