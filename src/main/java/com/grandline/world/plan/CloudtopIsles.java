package com.grandline.world.plan;

import com.grandline.world.plan.Anchor.Type;
import com.grandline.world.plan.Terrain.Ground;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChainBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.WallBannerBlock;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Cloudtop Isles (layout 13): the sky islands of the lower region and, above them to the north, the Upper Cloudtop Isles.
 * <pre>
 *  piece                    centre (local)   ground y   role
 *  Cloudfoot Cove           (0, 30)          66         arrival, pier and fisherman, the pool under the waterfall
 *  Mistfall island          (28, -4)         98         the waterfall island; lower camp (basic)
 *  Bloomhold                (-44, -40)       100        grassy village: colourful houses, round domed towers, plaza,
 *                                                       lower quest giver and home
 *  Azure Rise               (36, -64)        114        the temple: blue pediment, columns, the curved arched colonnade
 *                                                       behind it; lower camp (stronger), the sky arch up
 *  Skyreach Citadel         (-150, 15)       124        the castle: blue dome, wings, spires, entrance; the domed
 *                                                       chamber (ring chandelier, chains, banners, ladders, gallery,
 *                                                       the floor mechanism with its gold hub): the lower boss
 *  Ancient Tree landmass    (-95, -165)      200        the upper region, see {@link UpperCloudtop}
 *  Grand Sanctuary          (95, -165)       204        the upper region, see {@link UpperCloudtop}
 * </pre>
 * The lower islands are walked: bridges climb at most one block per block, with cloud platforms between. The upper
 * region is reached through the translucent sky arch on Azure Rise and left through the middle bay of the red arcade on
 * the Ancient Tree landmass (see {@link com.grandline.world.CloudtopPortals}). No island hangs over another. Island-local
 * coordinates: +x east, +z south. Built from code, the same in every world. Worlds made before layout 13 keep their
 * island ({@link CloudtopIslesV12}, {@link CloudtopIslesV7}, or the original).
 */
final class CloudtopIsles extends BasePlan {
	static final int SEA_G = 66;
	static final int WF_X = 28, WF_Z = -4, WF_Y = 98, WF_RX = 13, WF_RZ = 10;
	static final int V_X = -44, V_Z = -40, V_Y = 100, V_RX = 30, V_RZ = 24;
	static final int T_X = 36, T_Z = -64, T_Y = 114, T_RX = 23, T_RZ = 23;
	/**
	 * Skyreach Citadel, built in its own coordinates (centre (-10, -142)) and moved by (CIT_DX, CIT_DZ) to its place in the
	 * lower region, centre (-150, 15), ground y 124.
	 */
	static final int K_X = -10, K_Z = -142, K_Y = 124, K_RX = 46, K_RZ = 34, CIT_DX = -140, CIT_DZ = 157;
	/** The castle's domed chamber: centre and inner radius; the dome springs from {@link #DOME_Y}. */
	static final int CH_X = -10, CH_Z = -148, CH_R = 12, DOME_Y = K_Y + 21;
	/** The waterfall's falling columns (x = FALL_X - 1 .. FALL_X) and the pool under it. */
	static final int FALL_X = 28, FALL_Z = 7, POOL_X = 28, POOL_Z = 11;
	/** Cloud platforms on the walking route: x, z, radius, top. */
	static final int[][] PADS = {{-30, 11, 4, 79}, {2, -20, 3, 99}, {0, -52, 3, 107}, {-92, -14, 3, 111}};

	/** Portal spots (island-local): the sky arch on Azure Rise and the return arch at the citadel courtyard's edge. */
	public static final int PORTAL_UP_X = 50, PORTAL_UP_Y = T_Y + 1, PORTAL_UP_Z = -50;
	public static final int ARRIVE_UP_X = -135, ARRIVE_UP_Y = UpperCloudtop.T_Y + 1, ARRIVE_UP_Z = -192;
	public static final int PORTAL_DOWN_X = -135, PORTAL_DOWN_Y = UpperCloudtop.T_Y + 1, PORTAL_DOWN_Z = -202;
	public static final int ARRIVE_DOWN_X = 45, ARRIVE_DOWN_Y = T_Y + 1, ARRIVE_DOWN_Z = -50;

	/** Bloomhold's houses: corner, size, door side, walls, roof stairs, roof slab. */
	record Cottage(int x, int z, int w, int d, Direction front, Block wall, Block roof, Block roofSlab) {
	}

	static final Cottage[] COTTAGES = {
			new Cottage(-62, -46, 7, 6, Direction.EAST, Blocks.YELLOW_TERRACOTTA, Blocks.RED_NETHER_BRICK_STAIRS, Blocks.RED_NETHER_BRICK_SLAB),
			new Cottage(-54, -58, 7, 6, Direction.SOUTH, Blocks.LIGHT_BLUE_CONCRETE, Blocks.WAXED_CUT_COPPER_STAIRS, Blocks.WAXED_CUT_COPPER_SLAB),
			new Cottage(-40, -57, 6, 6, Direction.SOUTH, Blocks.PINK_TERRACOTTA, Blocks.DARK_PRISMARINE_STAIRS, Blocks.DARK_PRISMARINE_SLAB),
			new Cottage(-30, -52, 7, 6, Direction.WEST, Blocks.WHITE_CONCRETE, Blocks.CRIMSON_STAIRS, Blocks.CRIMSON_SLAB),
			new Cottage(-62, -34, 6, 7, Direction.EAST, Blocks.WHITE_TERRACOTTA, Blocks.WARPED_STAIRS, Blocks.WARPED_SLAB),
			new Cottage(-54, -28, 6, 6, Direction.NORTH, Blocks.ORANGE_TERRACOTTA, Blocks.PRISMARINE_BRICK_STAIRS, Blocks.PRISMARINE_BRICK_SLAB)};

	/** Bloomhold's round domed towers: centre, dome colour. */
	static final int[][] TOWERS = {{-44, -56}, {-24, -40}, {-32, -28}};
	static final Block[] TOWER_DOMES = {Blocks.BLUE_CONCRETE, Blocks.ORANGE_CONCRETE, Blocks.CYAN_CONCRETE};

	CloudtopIsles() {
		super("Cloudtop Isles", 0.05);
		// Cloudfoot Cove: a low green cove at sea level, the pool under the waterfall on its north-east side.
		terrain.mass(0, 30, 26, 18, 4.0, 3.0, Ground.GRASS)
				.mass(24, 14, 14, 11, 3.5, 3.0, Ground.GRASS)
				.mass(-16, 24, 10, 8, 3.0, 3.0, Ground.GRASS)
				.flat(0, 36, 7, 5, SEA_G, 3, null)                          // the arrival green
				.flat(-14, 25, 4, 3, SEA_G, 2, null)                        // the foot of the first bridge
				.flat(POOL_X, POOL_Z, 5, 4, 64, 2, s(Blocks.WATER))         // the pool the waterfall falls into
				.path(s(Blocks.DIRT_PATH), 2.6, 0, 46, 0, 34, -8, 28, -11, 27)
				.path(s(Blocks.DIRT_PATH), 2.2, 0, 34, 14, 22, POOL_X - 4, POOL_Z + 6)
				.reach(300);
		// Lower region (Lv 150-190).
		anchor(new Anchor(Type.ARRIVAL, 0, Anchor.SURFACE, 36, 0, 180));
		// The lower region's NPCs and enemies follow IslandRosters.CLOUDTOP (the upper region keeps its own).
		anchor(Anchor.of(Type.FISHERMAN, 22, 14));                    // the Angler, by the pool under the falls
		anchor(Anchor.at(Type.DOCK, 5, 63, 58));
		anchor(Anchor.at(Type.QUEST, -40, V_Y + 1, -34, 180));
		anchor(Anchor.at(Type.HOME, -49, V_Y + 1, -36, 90));
		anchor(new Anchor(Type.ENEMY_CAMP, T_X, T_Y, T_Z, 18));                 // Cloudstep Raiders: round the temple on Azure Rise
		anchor(new Anchor(Type.ENEMY_CAMP, K_X + CIT_DX, K_Y, K_Z + CIT_DZ, 30)); // Duskwardens: Skyreach Citadel's grounds
		// Upper region (Lv 450-625): the Upper Cloudtop Isles.
		anchor(Anchor.at(Type.QUEST, -137, UpperCloudtop.SET_Y + 1, -146, 180).upper());
		anchor(Anchor.at(Type.ENEMY_CAMP, -70, UpperCloudtop.T_Y, -150).upper());
		anchor(Anchor.at(Type.ENEMY_CAMP, 62, UpperCloudtop.S_Y, -126).upper());
		anchor(Anchor.at(Type.BOSS_ARENA, 110, UpperCloudtop.S_Y, -128).upper());
	}

	// ==================================================================================================== helpers

	static double unit(int x, int z, int salt) {
		return CloudtopIslesV7.unit(x, z, salt);
	}

	static BlockState stair(Block b, Direction facing, boolean top) {
		return Kit.stairs(b, facing, top);
	}

	static BlockState lantern(Block b, boolean hanging) {
		return s(b).setValue(LanternBlock.HANGING, hanging);
	}

	/** Weathered castle stone: stone bricks with cracked and mossy ones mixed in. */
	static BlockState aged(int x, int y, int z) {
		double u = unit(x * 5 + y, z, 19);
		return u < 0.16 ? s(Blocks.CRACKED_STONE_BRICKS) : u < 0.30 ? s(Blocks.MOSSY_STONE_BRICKS) : u < 0.36 ? s(Blocks.ANDESITE)
				: s(Blocks.STONE_BRICKS);
	}

	/** Direction from (dx, dz) toward the centre, along the dominant axis. */
	static Direction inward(int dx, int dz) {
		return Math.abs(dx) >= Math.abs(dz) ? (dx > 0 ? Direction.WEST : Direction.EAST) : (dz > 0 ? Direction.NORTH : Direction.SOUTH);
	}

	/** A soft, rounded cloud: an ellipsoid of wool and snow with a lumpy surface (never walked on, only looked at). */
	static void puff(Canvas c, int cx, int cy, int cz, double rx, double ry, double rz) {
		if (!c.near(cx, cz, (int) Math.ceil(Math.max(rx, rz)) + 1)) {
			return;
		}
		for (int x = (int) (cx - rx - 1); x <= cx + rx + 1; x++) {
			for (int z = (int) (cz - rz - 1); z <= cz + rz + 1; z++) {
				if (!c.inChunk(x, z)) {
					continue;
				}
				for (int y = (int) (cy - ry - 1); y <= cy + ry + 1; y++) {
					if (puffInside(x, y, z, cx, cy, cz, rx, ry, rz) && puffConnected(x, y, z, cx, cy, cz, rx, ry, rz)) {
						double dx = (x - cx) / rx, dy = (y - cy) / ry, dz = (z - cz) / rz;
						double d = dx * dx + dy * dy + dz * dz;
						boolean shell = d > 0.88 * 0.55;
						c.set(x, y, z, shell ? (unit(x, z + y, 62) < 0.5 ? s(Blocks.WHITE_WOOL) : s(Blocks.SNOW_BLOCK)) : s(Blocks.WHITE_CONCRETE));
					}
				}
			}
		}
	}

	static boolean puffInside(int x, int y, int z, int cx, int cy, int cz, double rx, double ry, double rz) {
		double dx = (x - cx) / rx, dy = (y - cy) / ry, dz = (z - cz) / rz;
		return dx * dx + dy * dy + dz * dz <= 0.88 + unit(x / 2, z / 2 + y, 61) * 0.22;
	}

	/** Whether every block on the step-by-step walk from (x, y, z) to the centre is inside, so no lump floats alone. */
	static boolean puffConnected(int x, int y, int z, int cx, int cy, int cz, double rx, double ry, double rz) {
		while (x != cx || y != cy || z != cz) {
			if (!puffInside(x, y, z, cx, cy, cz, rx, ry, rz)) {
				return false;
			}
			double ax = Math.abs(x - cx) / rx, ay = Math.abs(y - cy) / ry, az = Math.abs(z - cz) / rz;
			if (ax >= ay && ax >= az) {
				x -= Integer.signum(x - cx);
			} else if (ay >= az) {
				y -= Integer.signum(y - cy);
			} else {
				z -= Integer.signum(z - cz);
			}
		}
		return true;
	}

	/** A big rounded cloud: a cluster of overlapping puffs, broad and flat-bottomed. */
	static void bigCloud(Canvas c, int cx, int cy, int cz, int size) {
		if (!c.near(cx, cz, size * 3)) {
			return;
		}
		puff(c, cx, cy, cz, size * 1.4, size * 0.75, size);
		puff(c, cx - size, cy - 1, cz + 1, size * 0.9, size * 0.55, size * 0.8);
		puff(c, cx + size + 1, cy - 1, cz - 1, size * 0.9, size * 0.5, size * 0.75);
		puff(c, cx + 1, cy + size / 2, cz, size * 0.8, size * 0.6, size * 0.7);
		puff(c, cx - 2, cy, cz - size + 1, size * 0.8, size * 0.5, size * 0.6);
	}

	/**
	 * A narrow sky bridge (y = the walkway): three blocks of pale quartz and calcite over a dark deepslate edge band, stairs
	 * where it climbs (at most one block up per block across), a thin dark post with a lantern every ten blocks.
	 */
	static void skyBridge(Canvas c, int x1, int y1, int z1, int x2, int y2, int z2) {
		int n = Math.max(Math.abs(x2 - x1), Math.abs(z2 - z1));
		if (n == 0 || !c.near((x1 + x2) / 2, (z1 + z2) / 2, n / 2 + 4)) {
			return;
		}
		double ux = (x2 - x1) / (double) n, uz = (z2 - z1) / (double) n;
		Direction along = Math.abs(ux) >= Math.abs(uz) ? (ux > 0 ? Direction.EAST : Direction.WEST) : (uz > 0 ? Direction.SOUTH : Direction.NORTH);
		Direction side = along.getClockWise();
		int prevY = y1;
		for (int i = 0; i <= n; i++) {
			int x = (int) Math.round(x1 + ux * i), z = (int) Math.round(z1 + uz * i);
			int y = y1 + (int) Math.round((y2 - y1) * (i / (double) n));
			boolean climb = y > prevY, drop = y < prevY;
			for (int w = -1; w <= 1; w++) {
				int bx = x + side.getStepX() * w, bz = z + side.getStepZ() * w;
				Block flat = w == 0 ? Blocks.SMOOTH_QUARTZ : Blocks.CALCITE;
				Block steps = w == 0 ? Blocks.SMOOTH_QUARTZ_STAIRS : Blocks.QUARTZ_STAIRS;
				c.set(bx, y, bz, climb ? stair(steps, along, false) : drop ? stair(steps, along.getOpposite(), false) : s(flat));
				if (climb || drop) {
					// An upside-down stair under each step, so the underside is one smooth slope instead of a saw edge.
					c.set(bx, y - 1, bz, stair(steps, climb ? along.getOpposite() : along, true));
				} else {
					c.set(bx, y - 1, bz, w == 0 ? s(Blocks.SMOOTH_QUARTZ) : s(Blocks.CALCITE));
				}
				c.fill(bx, y + 1, bz, bx, y + 3, bz, s(Blocks.AIR));
			}
			if (i % 10 == 5 && i > 2 && i < n - 2 && !climb && !drop) {
				int sw = (i / 10) % 2 == 0 ? 1 : -1;
				int px = x + side.getStepX() * sw, pz = z + side.getStepZ() * sw;
				c.set(px, y + 1, pz, s(Blocks.POLISHED_BLACKSTONE_WALL));
				c.set(px, y + 2, pz, lantern(Blocks.LANTERN, false));
			}
			prevY = y;
		}
	}

	/**
	 * A floating landmass on a cloud (as in {@link CloudtopIslesV7#floating}), except that a rim lump is only placed
	 * where it touches the land or the lump next to it toward the middle, so no piece of cloud hangs alone.
	 */
	static void floating(Canvas c, int cx, int cz, int rx, int rz, CloudtopIslesV7.Height height, Block top, Block soil, int cloudDepth, int seed) {
		for (int x = cx - rx - 5; x <= cx + rx + 5; x++) {
			for (int z = cz - rz - 5; z <= cz + rz + 5; z++) {
				if (!c.inChunk(x, z)) {
					continue;
				}
				double e = CloudtopIslesV7.edge(x, z, cx, cz, rx, rz, seed);
				int y = height.at(x, z);
				if (e <= 1.0) {
					double lump = unit(x / 3, z / 3, seed) * 0.6 + unit(x, z, seed + 1) * 0.4;
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
					if (Math.floorMod(x, 4) == 0 && Math.floorMod(z, 4) == 0) {
						c.set(x, y - 4 - depth, z, s(Blocks.LIGHT).setValue(net.minecraft.world.level.block.LightBlock.LEVEL, 13));
					}
				} else {
					int[] rim = rim(x, z, cx, cz, rx, rz, height, seed);
					if (rim != null) {
						for (int yy = rim[0]; yy <= rim[1]; yy++) {
							c.set(x, yy, z, unit(x, yy, seed + 3) < 0.6 ? s(Blocks.WHITE_WOOL) : s(Blocks.SNOW_BLOCK));
						}
					}
				}
			}
		}
	}

	/** The rim lump's y range at a column outside the land, or null: it must overlap the column toward the middle. */
	static int[] rim(int x, int z, int cx, int cz, int rx, int rz, CloudtopIslesV7.Height height, int seed) {
		int[] r = rimRaw(x, z, cx, cz, rx, rz, height, seed);
		int guard = 0;
		int px = x, pz = z;
		int[] cur = r;
		while (cur != null && guard++ < 12) {
			int sx = Integer.signum(cx - px), sz = Integer.signum(cz - pz);
			double fx = Math.abs(px - cx) / (double) rx, fz = Math.abs(pz - cz) / (double) rz;
			if (fx >= fz) {
				px += sx;
			} else {
				pz += sz;
			}
			if (CloudtopIslesV7.edge(px, pz, cx, cz, rx, rz, seed) <= 1.0) {
				return r; // reached the land, which spans every rim height below its top
			}
			int[] in = rimRaw(px, pz, cx, cz, rx, rz, height, seed);
			if (in == null || in[1] < cur[0] || in[0] > cur[1]) {
				return null;
			}
			cur = in;
		}
		return null;
	}

	static int[] rimRaw(int x, int z, int cx, int cz, int rx, int rz, CloudtopIslesV7.Height height, int seed) {
		double e = CloudtopIslesV7.edge(x, z, cx, cz, rx, rz, seed);
		if (e <= 1.0 || e > 1.22) {
			return null;
		}
		double lump = unit(x / 3, z / 3, seed) * 0.6 + unit(x, z, seed + 1) * 0.4;
		double t = (e - 1.0) / 0.22;
		if (lump <= 0.25 + t * 0.6) {
			return null;
		}
		int base = height.at(x, z);
		int rimTop = base - 2 - (int) Math.round(t * 2);
		int rimBot = base - 4 - (int) Math.round((1 - t) * 4 * lump);
		return new int[]{rimBot, rimTop};
	}

	/** A walkable cloud platform: a round pad with a flat snow top and a deep rounded underside. */
	static void pad(Canvas c, int[] p) {
		if (!c.near(p[0], p[1], p[2] + 6)) {
			return;
		}
		CloudtopIslesV7.cloudPad(c, p[0], p[1], p[2], p[3], true);
		puff(c, p[0] + 2, p[3] - 3, p[1] + 1, p[2] + 2.5, 2.5, p[2] + 1.5);
		CloudtopIslesV7.cloudPad(c, p[0], p[1], p[2], p[3], true); // the walking top stays flat over the puff
		c.fill(p[0] - p[2], p[3] + 1, p[1] - p[2], p[0] + p[2], p[3] + 3, p[1] + p[2], s(Blocks.AIR));
	}

	/**
	 * A translucent arch standing in the air: a half ring of light blue glass edged with white glass, in the vertical
	 * plane through (cx, cz) along x ({@code alongX}) or along z, two blocks deep.
	 */
	static void skyArch(Canvas c, int cx, int baseY, int cz, int radius, boolean alongX) {
		if (!c.near(cx, cz, radius + 3)) {
			return;
		}
		for (int t = -radius - 2; t <= radius + 2; t++) {
			for (int dy = 0; dy <= radius + 2; dy++) {
				double d = Math.hypot(t, dy);
				if (d < radius - 0.5 || d > radius + 2.2) {
					continue;
				}
				BlockState b = d > radius + 1.3 || d < radius + 0.2 ? s(Blocks.WHITE_STAINED_GLASS) : s(Blocks.LIGHT_BLUE_STAINED_GLASS);
				for (int k = 0; k <= 1; k++) {
					if (alongX) {
						c.set(cx + t, baseY + dy, cz + k, b);
					} else {
						c.set(cx + k, baseY + dy, cz + t, b);
					}
				}
			}
		}
	}

	/**
	 * A walk-through sky arch (a portal): pale quartz piers and a glass arch over a five-wide, six-high opening, in the
	 * plane along x ({@code alongX}) or along z, the opening centred on (x, z) above the floor y.
	 */
	static void portalArch(Canvas c, int x, int y, int z, boolean alongX) {
		if (!c.near(x, z, 8)) {
			return;
		}
		for (int t = -5; t <= 5; t++) {
			for (int dy = 0; dy <= 10; dy++) {
				int at = Math.abs(t);
				BlockState b = null;
				if (dy <= 5) {
					if (at >= 3 && at <= 4) {
						b = dy == 0 ? s(Blocks.CHISELED_QUARTZ_BLOCK) : s(Blocks.QUARTZ_PILLAR);
					}
				} else {
					double d = Math.hypot(t, dy - 5);
					if (d >= 3 && d <= 4.6) {
						b = d > 4 ? s(Blocks.WHITE_STAINED_GLASS) : s(Blocks.LIGHT_BLUE_STAINED_GLASS);
					}
				}
				if (b == null && at <= 2 && dy >= 1 && dy <= 7) {
					b = s(Blocks.AIR);
				}
				if (b != null) {
					if (alongX) {
						c.set(x + t, y + dy, z, b);
					} else {
						c.set(x, y + dy, z + t, b);
					}
				}
			}
		}
		// A pale step under the opening, a lantern on each pier.
		for (int t = -4; t <= 4; t++) {
			for (int k = -1; k <= 1; k++) {
				if (alongX) {
					c.set(x + t, y, z + k, s(Blocks.SMOOTH_QUARTZ));
				} else {
					c.set(x + k, y, z + t, s(Blocks.SMOOTH_QUARTZ));
				}
			}
		}
		for (int t : new int[]{-4, 4}) {
			if (alongX) {
				c.set(x + t, y + 6, z, lantern(Blocks.SOUL_LANTERN, false));
			} else {
				c.set(x, y + 6, z + t, lantern(Blocks.SOUL_LANTERN, false));
			}
		}
	}

	/** A round broad-leaf tree standing on ground y. */
	static void tree(Canvas c, int x, int y, int z, int h, Block log, Block leaves) {
		if (c.near(x, z, 5)) {
			CloudtopIslesV7.tree(c, x, y, z, h, log, leaves);
		}
	}

	/** Grass tufts and flowers over grass blocks in a box at ground y. */
	static void meadow(Canvas c, int x1, int z1, int x2, int z2, int y, int salt) {
		for (int x = x1; x <= x2; x++) {
			for (int z = z1; z <= z2; z++) {
				if (!c.inChunk(x, z) || c.level().getBlockState(c.world(x, y, z)).getBlock() != Blocks.GRASS_BLOCK
						|| !c.level().getBlockState(c.world(x, y + 1, z)).isAir()) {
					continue;
				}
				double r = unit(x, z, salt);
				if (r < 0.05) {
					c.set(x, y + 1, z, s(r < 0.015 ? Blocks.CORNFLOWER : r < 0.03 ? Blocks.OXEYE_DAISY : r < 0.04 ? Blocks.ALLIUM : Blocks.DANDELION));
				} else if (r > 0.86) {
					c.set(x, y + 1, z, s(Blocks.GRASS));
				}
			}
		}
	}

	/** Pale paving along a line (top block at y), {@code width} wide. */
	static void pave(Canvas c, int y, double width, int... pts) {
		for (int i = 0; i + 3 < pts.length; i += 2) {
			int x1 = pts[i], z1 = pts[i + 1], x2 = pts[i + 2], z2 = pts[i + 3];
			int n = Math.max(Math.abs(x2 - x1), Math.abs(z2 - z1));
			if (!c.near((x1 + x2) / 2, (z1 + z2) / 2, n / 2 + 4)) {
				continue;
			}
			for (int k = 0; k <= n; k++) {
				double px = x1 + (x2 - x1) * (k / (double) Math.max(1, n)), pz = z1 + (z2 - z1) * (k / (double) Math.max(1, n));
				for (int x = (int) Math.floor(px - width); x <= Math.ceil(px + width); x++) {
					for (int z = (int) Math.floor(pz - width); z <= Math.ceil(pz + width); z++) {
						if (Math.hypot(x - px, z - pz) <= width / 2 + 0.3) {
							double u = unit(x, z, 23);
							c.set(x, y, z, u < 0.55 ? s(Blocks.SMOOTH_STONE) : u < 0.85 ? s(Blocks.CALCITE) : s(Blocks.POLISHED_DIORITE));
							c.fill(x, y + 1, z, x, y + 2, z, s(Blocks.AIR));
						}
					}
				}
			}
		}
	}

	/** A lamp post on ground y: a dark post and a lantern on top. */
	static void lampPost(Canvas c, int x, int y, int z) {
		c.set(x, y + 1, z, s(Blocks.POLISHED_BLACKSTONE_WALL));
		c.set(x, y + 2, z, s(Blocks.POLISHED_BLACKSTONE_WALL));
		c.set(x, y + 3, z, lantern(Blocks.LANTERN, false));
	}

	/** A small grey rock outcrop on ground y. */
	static void rock(Canvas c, int x, int y, int z, int h, int r) {
		if (!c.near(x, z, r + 2)) {
			return;
		}
		for (int k = 1; k <= h; k++) {
			double rr = r * (1.0 - (k / (double) (h + 1)) * 0.75) + 0.4;
			for (int dx = -r - 1; dx <= r + 1; dx++) {
				for (int dz = -r - 1; dz <= r + 1; dz++) {
					if (Math.hypot(dx, dz * 1.15) + unit(x + dx, z + dz + k, 41) * 0.8 <= rr) {
						double u = unit(x + dx, k, z + dz);
						c.set(x + dx, y + k, z + dz, u < 0.45 ? s(Blocks.ANDESITE) : u < 0.8 ? s(Blocks.STONE) : s(Blocks.TUFF));
					}
				}
			}
		}
	}

	/** A small floating islet (grassy with a tree, or bare rock), on its own cloud. */
	static void islet(Canvas c, int cx, int cz, int r, int y, boolean grassy, int seed) {
		if (!c.near(cx, cz, r + 6)) {
			return;
		}
		floating(c, cx, cz, r, r, (x, z) -> y, grassy ? Blocks.GRASS_BLOCK : Blocks.STONE, grassy ? Blocks.DIRT : Blocks.ANDESITE,
				Math.max(4, r + 2), seed);
		if (grassy) {
			tree(c, cx, y, cz, 4 + seed % 2, Blocks.OAK_LOG, Blocks.OAK_LEAVES);
			meadow(c, cx - r, cz - r, cx + r, cz + r, y, seed);
		} else {
			rock(c, cx - 1, y, cz + 1, 3 + seed % 3, 2);
		}
	}

	/** A wall banner, blue with a spiral rosette (curly border, a ring and a whirl), facing {@code facing}. */
	static void spiralBanner(Canvas c, int x, int y, int z, Direction facing) {
		if (!c.inChunk(x, z)) {
			return;
		}
		c.set(x, y, z, s(Blocks.BLUE_WALL_BANNER).setValue(WallBannerBlock.FACING, facing));
		if (c.level().getBlockEntity(c.world(x, y, z)) instanceof BannerBlockEntity banner) {
			ListTag patterns = new ListTag();
			for (Object[] p : new Object[][]{{"cbo", DyeColor.LIGHT_BLUE}, {"mc", DyeColor.YELLOW}, {"flo", DyeColor.BLUE}, {"cs", DyeColor.LIGHT_BLUE}}) {
				CompoundTag t = new CompoundTag();
				t.putString("Pattern", (String) p[0]);
				t.putInt("Color", ((DyeColor) p[1]).getId());
				patterns.add(t);
			}
			CompoundTag tag = new CompoundTag();
			tag.put("Patterns", patterns);
			ItemStack stack = new ItemStack(Items.BLUE_BANNER);
			BlockItem.setBlockEntityData(stack, BlockEntityType.BANNER, tag);
			banner.fromItem(stack, DyeColor.BLUE);
			banner.setChanged();
		}
	}

	// ==================================================================================================== build

	@Override
	public void build(Canvas c) {
		cove(c);
		if (c.near(WF_X, WF_Z, WF_RX + 8)) {
			floating(c, WF_X, WF_Z, WF_RX, WF_RZ, (x, z) -> WF_Y, Blocks.GRASS_BLOCK, Blocks.DIRT, 14, 21);
			mistfall(c);
		}
		waterfall(c);
		if (c.near(V_X, V_Z, V_RX + 8)) {
			floating(c, V_X, V_Z, V_RX, V_RZ, (x, z) -> V_Y, Blocks.GRASS_BLOCK, Blocks.DIRT, 20, 23);
			village(c);
		}
		if (c.near(T_X, T_Z, T_RX + 8)) {
			floating(c, T_X, T_Z, T_RX, T_RZ, (x, z) -> T_Y, Blocks.CALCITE, Blocks.CALCITE, 18, 25);
			temple(c);
		}
		// The walking route: cove -> cloud -> Bloomhold; Bloomhold -> cloud -> Mistfall; Bloomhold -> cloud -> Azure Rise.
		for (int[] p : PADS) {
			pad(c, p);
		}
		skyBridge(c, -14, SEA_G + 1, 25, -27, 80, 13);
		skyBridge(c, -31, 80, 8, -40, V_Y, -18);
		skyBridge(c, -20, V_Y, -28, -1, 100, -21);
		skyBridge(c, 4, 100, -18, 17, WF_Y, -6);
		skyBridge(c, -18, V_Y, -48, -3, 108, -51);
		skyBridge(c, 3, 108, -53, 17, T_Y, -62);
		// The way to Skyreach Citadel: Bloomhold -> cloud -> citadel.
		skyBridge(c, -70, V_Y, -30, -89, 112, -16);
		skyBridge(c, -95, 112, -12, -115, K_Y, 2);
		Canvas cc = c.shifted(CIT_DX, CIT_DZ);
		if (cc.near(K_X, K_Z, K_RX + 10)) {
			floating(cc, K_X, K_Z, K_RX, K_RZ, (x, z) -> K_Y, Blocks.GRASS_BLOCK, Blocks.DIRT, 22, 27);
			citadel(cc);
		}
		// Small islands, grassy and rocky.
		islet(c, -84, 4, 6, 86, true, 1);
		islet(c, 62, -22, 5, 104, false, 2);
		islet(c, -12, -88, 6, 118, true, 3);
		islet(c, 72, -92, 4, 128, false, 4);
		islet(c, -78, -78, 5, 112, true, 5);
		// Big rounded clouds.
		bigCloud(c, 74, 100, -34, 6);
		bigCloud(c, -6, 92, 10, 5);
		bigCloud(c, 62, 84, 22, 6);
		bigCloud(c, -62, 88, 22, 6);
		bigCloud(c, 6, 132, -104, 7);
		bigCloud(c, -84, 150, -78, 7);
		bigCloud(c, -200, 112, 70, 6);
		// The Upper Cloudtop Isles.
		UpperCloudtop.ancientTree(c);
		UpperCloudtop.rootBridge(c);
		UpperCloudtop.sanctuary(c);
		// Translucent arches hanging in the sky.
		skyArch(c, 66, 76, -6, 15, false);
		skyArch(c, -86, 108, -50, 12, false);
		// Accessibility: half-steps up the beach terraces from the dock and on the way to the village (no jumping).
		Kit.stepsUp(c, 20, 40, 20, 46, Direction.WEST, Blocks.SANDSTONE_STAIRS);
		Kit.stepsUp(c, 17, 40, 17, 46, Direction.WEST, Blocks.SANDSTONE_STAIRS);
		Kit.stepsUp(c, 13, 40, 13, 46, Direction.WEST, Blocks.SANDSTONE_STAIRS);
		for (int x = -17; x <= -11; x++) { // onto the first sky bridge from the hilltop
			Kit.entryStep(c, x, SEA_G + 1, 27, Direction.NORTH, Blocks.QUARTZ_STAIRS);
		}
		for (int z = 6; z <= 10; z++) { // onto the second sky bridge from the platform between them
			Kit.entryStep(c, -29, 80, z, Direction.WEST, Blocks.QUARTZ_STAIRS);
		}
	}

	// ---------------------------------------------------------------------------------------------------- cove

	/** Cloudfoot Cove: the pier, the arrival green, birches and rocks, flowers, lamp posts. */
	private void cove(Canvas c) {
		if (!c.near(4, 26, 40)) {
			return;
		}
		for (int[] t : new int[][]{{-10, 36}, {12, 32}, {-20, 30}, {18, 24}}) {
			if (c.near(t[0], t[1], 5)) {
				tree(c, t[0], c.ground(t[0], t[1]), t[1], 5, Blocks.BIRCH_LOG, Blocks.BIRCH_LEAVES);
			}
		}
		for (int[] r : new int[][]{{-22, 22, 5, 2}, {14, 40, 3, 2}, {36, 18, 4, 2}}) {
			if (c.near(r[0], r[1], 4)) {
				rock(c, r[0], c.ground(r[0], r[1]), r[1], r[2], r[3]);
			}
		}
		// The pier: south from the green into the sea.
		if (c.near(0, 52, 12)) {
			for (int z = 44; z <= 60; z++) {
				for (int x = -1; x <= 1; x++) {
					c.set(x, 64, z, x == 0 ? s(Blocks.STRIPPED_SPRUCE_LOG).setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z) : s(Blocks.SPRUCE_PLANKS));
					c.fill(x, 65, z, x, 67, z, s(Blocks.AIR));
				}
				for (int x : new int[]{-2, 2}) {
					if (z % 4 == 0 || z == 60) {
						c.fill(x, Math.min(c.ground(x, z), 61), z, x, 64, z, s(Blocks.SPRUCE_LOG));
						c.set(x, 65, z, s(Blocks.SPRUCE_FENCE));
						if (z % 8 == 4) {
							c.set(x, 66, z, lantern(Blocks.LANTERN, false));
						}
					} else {
						c.set(x, 64, z, s(Blocks.SPRUCE_PLANKS));
						c.set(x, 65, z, Kit.connected(Blocks.SPRUCE_FENCE, false));
					}
				}
			}
			c.set(-1, 65, 59, Canvas.facing(s(Blocks.BARREL), Direction.UP));
		}
		for (int x = -30; x <= 42; x++) {
			for (int z = 0; z <= 50; z++) {
				if (c.inChunk(x, z) && c.ground(x, z) > Terrain.WATER_TOP + 1) {
					meadow(c, x, z, x, z, c.ground(x, z), 31);
				}
			}
		}
		// Lamp posts on the green and at the foot of the first bridge.
		for (int[] l : new int[][]{{4, 38}, {-4, 38}, {-11, 23}, {POOL_X - 7, POOL_Z + 3}}) {
			if (c.near(l[0], l[1], 2)) {
				lampPost(c, l[0], c.ground(l[0], l[1]), l[1]);
			}
		}
	}

	// ---------------------------------------------------------------------------------------------------- Mistfall

	/** Mistfall: rocks, two trees, the spring that feeds the waterfall, the lower camp (tents and a campfire). */
	private void mistfall(Canvas c) {
		int y = WF_Y;
		rock(c, WF_X + 7, y, WF_Z - 4, 6, 3);
		rock(c, WF_X - 8, y, WF_Z - 5, 4, 2);
		rock(c, WF_X + 2, y, WF_Z - 8, 3, 2);
		tree(c, WF_X + 6, y, WF_Z + 3, 5, Blocks.OAK_LOG, Blocks.OAK_LEAVES);
		tree(c, WF_X - 9, y, WF_Z + 2, 4, Blocks.OAK_LOG, Blocks.OAK_LEAVES);
		// The spring: a stone-rimmed pool that runs south into the spout.
		for (int x = FALL_X - 3; x <= FALL_X + 2; x++) {
			for (int z = FALL_Z - 6; z <= FALL_Z - 4; z++) {
				boolean rim = x == FALL_X - 3 || x == FALL_X + 2 || z == FALL_Z - 6;
				c.set(x, y, z, rim ? s(Blocks.MOSSY_COBBLESTONE) : s(Blocks.WATER));
				c.set(x, y - 1, z, s(Blocks.STONE));
				c.set(x, y + 1, z, s(Blocks.AIR));
			}
		}
		// The camp: two tents, a campfire, crates.
		int cx = WF_X - 2, cz = WF_Z - 3;
		tent(c, cx - 4, cz - 1, y, Blocks.BROWN_WOOL);
		tent(c, cx + 3, cz - 2, y, Blocks.GREEN_WOOL);
		c.set(cx, y + 1, cz + 1, s(Blocks.CAMPFIRE));
		c.set(cx - 5, y + 1, cz + 3, Canvas.facing(s(Blocks.BARREL), Direction.UP));
		c.set(cx - 4, y + 1, cz + 3, s(Blocks.SPRUCE_PLANKS));
		meadow(c, WF_X - WF_RX, WF_Z - WF_RZ, WF_X + WF_RX, WF_Z + WF_RZ, y, 7);
	}

	/** A small A-frame tent (three long, opening south) on ground y. */
	static void tent(Canvas c, int x, int z, int y, Block wool) {
		for (int dz = -1; dz <= 1; dz++) {
			c.set(x - 1, y + 1, z + dz, s(wool));
			c.set(x + 1, y + 1, z + dz, s(wool));
			c.set(x, y + 2, z + dz, s(wool));
			c.set(x, y + 1, z + dz, s(Blocks.AIR));
		}
		c.set(x, y + 1, z - 1, s(Blocks.SPRUCE_PLANKS));
		c.set(x - 1, y, z + 2, s(Blocks.COARSE_DIRT));
		c.set(x + 1, y, z + 2, s(Blocks.COARSE_DIRT));
	}

	/** The waterfall: from the spout on Mistfall's south edge straight down into the cove's pool. */
	private void waterfall(Canvas c) {
		if (!c.near(FALL_X, FALL_Z, 4)) {
			return;
		}
		for (int x = FALL_X - 1; x <= FALL_X; x++) {
			for (int z = FALL_Z - 3; z <= FALL_Z - 1; z++) {
				c.set(x, WF_Y, z, s(Blocks.WATER));
				c.set(x, WF_Y - 1, z, s(Blocks.STONE_BRICKS));
				c.set(x, WF_Y + 1, z, s(Blocks.AIR));
			}
			for (int z = FALL_Z - 3; z <= FALL_Z; z++) {
				for (int rx : new int[]{FALL_X - 2, FALL_X + 1}) {
					c.set(rx, WF_Y, z, s(Blocks.MOSSY_STONE_BRICKS));
					c.set(rx, WF_Y + 1, z, s(Blocks.AIR));
				}
			}
			BlockState falling = s(Blocks.WATER).setValue(LiquidBlock.LEVEL, 8);
			for (int y = WF_Y; y >= 65; y--) {
				c.set(x, y, FALL_Z, falling);
			}
			c.tickFluid(x, WF_Y, FALL_Z);
		}
		for (int y = WF_Y - 6; y >= 70; y -= 6) {
			c.set(FALL_X + 1, y, FALL_Z + 1, s(Blocks.LIGHT).setValue(net.minecraft.world.level.block.LightBlock.LEVEL, 12));
		}
	}

	// ---------------------------------------------------------------------------------------------------- Bloomhold

	/** Bloomhold: the plaza and fountain, six colourful cottages, three round domed towers, paths, trees, lamps. */
	private void village(Canvas c) {
		int y = V_Y;
		// Paths: from the south bridge, the two east bridges and between the houses, all to the plaza.
		pave(c, y, 3, -40, -18, -42, -34);
		pave(c, y, 3, -20, -28, -38, -38);
		pave(c, y, 3, -18, -48, -38, -42);
		pave(c, y, 2, -50, -40, -55, -43);
		pave(c, y, 2, -50, -40, -56, -36, -56, -31);
		pave(c, y, 2, -46, -46, -51, -52);
		pave(c, y, 2, -42, -46, -37, -51);
		pave(c, y, 2, -38, -44, -33, -49, -31, -49);
		pave(c, y, 2, -46, -34, -51, -29);
		pave(c, y, 2, -44, -47, -44, -52);
		pave(c, y, 2, -38, -38, -28, -40);
		pave(c, y, 2, -40, -30, -36, -28);
		// The plaza and its fountain.
		if (c.near(V_X, V_Z, 9)) {
			for (int dx = -7; dx <= 7; dx++) {
				for (int dz = -7; dz <= 7; dz++) {
					double d = Math.hypot(dx, dz);
					if (d <= 7.3) {
						int x = V_X + dx, z = V_Z + dz;
						c.set(x, y, z, d > 6.3 ? s(Blocks.POLISHED_DIORITE) : (Math.floorMod(dx + dz, 2) == 0 ? s(Blocks.CALCITE) : s(Blocks.SMOOTH_STONE)));
						c.fill(x, y + 1, z, x, y + 3, z, s(Blocks.AIR));
					}
				}
			}
			for (int dx = -3; dx <= 3; dx++) {
				for (int dz = -3; dz <= 3; dz++) {
					double d = Math.hypot(dx, dz);
					if (d <= 3.4) {
						int x = V_X + dx, z = V_Z + dz;
						c.set(x, y, z, s(Blocks.QUARTZ_BRICKS));
						c.set(x, y + 1, z, d > 2.5 ? s(Blocks.QUARTZ_BRICKS) : s(Blocks.WATER));
					}
				}
			}
			c.set(V_X, y + 1, V_Z, s(Blocks.QUARTZ_PILLAR));
			c.set(V_X, y + 2, V_Z, s(Blocks.QUARTZ_PILLAR));
			c.set(V_X, y + 3, V_Z, s(Blocks.WATER));
			c.tickFluid(V_X, y + 3, V_Z);
			for (int[] l : new int[][]{{-6, -2}, {6, 2}, {-2, 6}, {2, -6}}) {
				lampPost(c, V_X + l[0], y, V_Z + l[1]);
			}
			// Benches facing the fountain.
			for (int dz = -1; dz <= 1; dz++) {
				c.set(V_X - 5, y + 1, V_Z + dz, stair(Blocks.SPRUCE_STAIRS, Direction.WEST, false));
				c.set(V_X + 5, y + 1, V_Z + dz, stair(Blocks.SPRUCE_STAIRS, Direction.EAST, false));
			}
		}
		for (Cottage h : COTTAGES) {
			if (c.near(h.x() + h.w() / 2, h.z() + h.d() / 2, 12)) {
				cottage(c, h, y);
			}
		}
		for (int i = 0; i < TOWERS.length; i++) {
			if (c.near(TOWERS[i][0], TOWERS[i][1], 8)) {
				domedTower(c, TOWERS[i][0], TOWERS[i][1], y, 3, 11, TOWER_DOMES[i]);
			}
		}
		for (int[] t : new int[][]{{-67, -38}, {-59, -52}, {-45, -21}, {-20, -33}, {-52, -48}}) {
			tree(c, t[0], y, t[1], 5, t[0] % 2 == 0 ? Blocks.OAK_LOG : Blocks.BIRCH_LOG, t[0] % 2 == 0 ? Blocks.OAK_LEAVES : Blocks.BIRCH_LEAVES);
		}
		// Flower beds by the cottages, then the meadow everywhere else.
		for (int[] b : new int[][]{{-56, -39}, {-47, -52}, {-34, -54}, {-47, -27}}) {
			if (c.near(b[0], b[1], 3)) {
				c.set(b[0], y + 1, b[1], s(Blocks.ROSE_BUSH));
				c.set(b[0], y + 2, b[1], s(Blocks.ROSE_BUSH).setValue(net.minecraft.world.level.block.DoublePlantBlock.HALF,
						net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER));
			}
		}
		if (c.near(V_X, V_Z, V_RX + 2)) {
			meadow(c, V_X - V_RX, V_Z - V_RZ, V_X + V_RX, V_Z + V_RZ, y, 11);
		}
		// A notice board by the quest giver.
		if (c.near(-37, -34, 2)) {
			c.set(-37, y + 1, -33, s(Blocks.SPRUCE_FENCE));
			c.set(-37, y + 2, -33, Canvas.facing(s(Blocks.SPRUCE_WALL_SIGN), Direction.SOUTH));
			c.set(-37, y + 2, -34, s(Blocks.SPRUCE_PLANKS));
			c.set(-37, y + 1, -34, s(Blocks.SPRUCE_FENCE));
		}
	}

	/** A colourful cottage on ground y: framed walls, windows, a door with a lantern, a stair roof, a furnished room. */
	private static void cottage(Canvas c, Cottage h, int y) {
		int minX = h.x(), minZ = h.z(), maxX = h.x() + h.w() - 1, maxZ = h.z() + h.d() - 1;
		Kit.Palette p = new Kit.Palette(h.wall(), Blocks.STRIPPED_BIRCH_LOG, Blocks.BIRCH_PLANKS, h.roof(), h.roofSlab(), Blocks.BIRCH_DOOR, Blocks.GLASS_PANE);
		c.fill(minX, y, minZ, maxX, y, maxZ, s(Blocks.BIRCH_PLANKS));
		for (int x = minX; x <= maxX; x++) {
			for (int z = minZ; z <= maxZ; z++) {
				boolean edgeX = x == minX || x == maxX, edgeZ = z == minZ || z == maxZ;
				if (!edgeX && !edgeZ) {
					c.fill(x, y + 1, z, x, y + 4, z, s(Blocks.AIR));
					continue;
				}
				boolean corner = edgeX && edgeZ;
				for (int dy = 1; dy <= 3; dy++) {
					BlockState b = corner ? s(Blocks.STRIPPED_BIRCH_LOG) : s(h.wall());
					if (dy == 2 && !corner && Math.floorMod(edgeX ? z : x, 2) == 0) {
						b = Kit.connected(Blocks.GLASS_PANE, !edgeX);
					}
					c.set(x, y + dy, z, b);
				}
				c.set(x, y + 4, z, s(Blocks.STRIPPED_BIRCH_LOG).setValue(RotatedPillarBlock.AXIS, corner ? Direction.Axis.Y : edgeX ? Direction.Axis.Z : Direction.Axis.X));
			}
		}
		Direction front = h.front();
		int dx = front == Direction.EAST ? maxX : front == Direction.WEST ? minX : (minX + maxX) / 2;
		int dz = front == Direction.SOUTH ? maxZ : front == Direction.NORTH ? minZ : (minZ + maxZ) / 2;
		Kit.door(c, dx, y + 1, dz, Blocks.BIRCH_DOOR, front.getOpposite());
		c.set(dx + front.getStepX(), y, dz + front.getStepZ(), s(Blocks.POLISHED_DIORITE));
		c.fill(dx + front.getStepX(), y + 1, dz + front.getStepZ(), dx + front.getStepX(), y + 2, dz + front.getStepZ(), s(Blocks.AIR));
		// A lantern on a short post beside the door, a flowering bush on the other side.
		Direction side = front.getClockWise();
		int px = dx + side.getStepX() * 2 + front.getStepX(), pz = dz + side.getStepZ() * 2 + front.getStepZ();
		c.set(px, y + 1, pz, s(Blocks.BIRCH_FENCE));
		c.set(px, y + 2, pz, lantern(Blocks.LANTERN, false));
		int bx = dx - side.getStepX() * 2 + front.getStepX(), bz = dz - side.getStepZ() * 2 + front.getStepZ();
		c.set(bx, y + 1, bz, s(Blocks.FLOWERING_AZALEA_LEAVES).setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true));
		Kit.gableRoof(c, minX - 1, minZ - 1, maxX + 1, maxZ + 1, y + 4, p, maxX - minX >= maxZ - minZ);
		Kit.furnish(c, minX + 1, minZ + 1, maxX - 1, maxZ - 1, y, Kit.Interior.HOME, front);
		c.set(maxX - 1, y + 2, minZ + 1, lantern(Blocks.LANTERN, false)); // on the crafting table
	}

	/** A round tower on ground y with a coloured dome: white walls, a coloured band, windows, a door toward the plaza. */
	private static void domedTower(Canvas c, int cx, int cz, int y, int r, int h, Block dome) {
		for (int dx = -r; dx <= r; dx++) {
			for (int dz = -r; dz <= r; dz++) {
				double d = Math.hypot(dx, dz);
				if (d > r + 0.4) {
					continue;
				}
				boolean shell = d > r - 0.8;
				c.set(cx + dx, y, cz + dz, s(Blocks.POLISHED_DIORITE));
				for (int yy = y + 1; yy <= y + h; yy++) {
					int k = yy - y;
					BlockState b;
					if (!shell) {
						b = s(Blocks.AIR);
					} else if (k == h - 1) {
						b = s(dome == Blocks.ORANGE_CONCRETE ? Blocks.ORANGE_TERRACOTTA : Blocks.LIGHT_BLUE_TERRACOTTA);
					} else if ((k == 3 || k == 7) && (dx == 0 || dz == 0)) {
						b = s(Blocks.GLASS);
					} else {
						b = s(Blocks.WHITE_CONCRETE);
					}
					c.set(cx + dx, yy, cz + dz, b);
				}
			}
		}
		// The dome: a half sphere a block wider than the walls, with a gold finial.
		int R = r + 1;
		for (int dx = -R; dx <= R; dx++) {
			for (int dz = -R; dz <= R; dz++) {
				for (int dy = 0; dy <= R; dy++) {
					double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
					if (d <= R + 0.45 && d > R - 0.75) {
						c.set(cx + dx, y + h + 1 + dy, cz + dz, s(dome));
					} else if (d <= R - 0.75 && dy == 0) {
						c.set(cx + dx, y + h + 1, cz + dz, s(Blocks.BIRCH_PLANKS));
					}
				}
			}
		}
		c.set(cx, y + h + R + 2, cz, s(Blocks.GOLD_BLOCK));
		c.set(cx, y + h + R + 3, cz, s(Blocks.LIGHTNING_ROD));
		c.set(cx, y + h, cz, lantern(Blocks.LANTERN, true));
		// The door faces the plaza.
		Direction toPlaza = inward(cx - V_X, cz - V_Z);
		Kit.door(c, cx + toPlaza.getStepX() * r, y + 1, cz + toPlaza.getStepZ() * r, Blocks.BIRCH_DOOR, toPlaza.getOpposite());
		c.set(cx + toPlaza.getStepX() * (r + 1), y, cz + toPlaza.getStepZ() * (r + 1), s(Blocks.POLISHED_DIORITE));
		c.fill(cx + toPlaza.getStepX() * (r + 1), y + 1, cz + toPlaza.getStepZ() * (r + 1), cx + toPlaza.getStepX() * (r + 1), y + 2,
				cz + toPlaza.getStepZ() * (r + 1), s(Blocks.AIR));
		// A ladder up the back wall to a lookout floor under the dome.
		Direction back = toPlaza.getOpposite();
		int lx = cx + back.getStepX() * (r - 1), lz = cz + back.getStepZ() * (r - 1);
		for (int yy = y + 1; yy <= y + h; yy++) {
			c.set(lx, yy, lz, Canvas.facing(s(Blocks.LADDER), toPlaza));
		}
		for (int dx = -r + 1; dx <= r - 1; dx++) {
			for (int dz = -r + 1; dz <= r - 1; dz++) {
				if (Math.hypot(dx, dz) <= r - 0.8 && (cx + dx != lx || cz + dz != lz)) {
					c.set(cx + dx, y + 8, cz + dz, s(Blocks.BIRCH_PLANKS));
				}
			}
		}
		c.set(cx, y + 8, cz, s(Blocks.BIRCH_PLANKS));
		c.set(cx - back.getStepX(), y + 7, cz - back.getStepZ(), lantern(Blocks.LANTERN, true));
	}

	// ---------------------------------------------------------------------------------------------------- Azure Rise

	/** The temple platform's extent, its top (y), and the colonnade's centre and radius. */
	static final int TP_X1 = 26, TP_X2 = 46, TP_Z1 = -78, TP_Z2 = -61, TP_Y = T_Y + 2;
	static final int COL_X = 36, COL_Z = -66, COL_R = 18;

	/**
	 * Azure Rise: a pale plaza, the temple (stepped platform, columns all round, a cella with an altar, a blue frieze and
	 * blue pediments under a pale roof), the curved arched colonnade behind it, the arena in front, the sky arch.
	 */
	private void temple(Canvas c) {
		int y = T_Y;
		// The plaza in front, the path from the bridge, the boss arena ring.
		pave(c, y, 3, 17, -62, 22, -56, 30, -56);
		pave(c, y, 5, 26, -58, 46, -58);
		pave(c, y, 3, 40, -54, 46, -50);
		if (c.near(34, -50, 9)) {
			for (int dx = -7; dx <= 7; dx++) {
				for (int dz = -7; dz <= 7; dz++) {
					double d = Math.hypot(dx, dz);
					if (d <= 7.2 && d > 6.2) {
						c.set(34 + dx, y, -50 + dz, s(Blocks.POLISHED_DIORITE));
					} else if (d <= 6.2) {
						c.set(34 + dx, y, -50 + dz, Math.floorMod(dx * 3 + dz, 5) == 0 ? s(Blocks.LIGHT_BLUE_CONCRETE) : s(Blocks.SMOOTH_QUARTZ));
					}
				}
			}
		}
		colonnade(c);
		if (c.near(36, -70, 16)) {
			templeBuilding(c);
		}
		portalArch(c, PORTAL_UP_X, y, PORTAL_UP_Z, false);
		// Planters and lamps on the plaza.
		for (int[] l : new int[][]{{24, -58}, {48, -58}, {20, -62}}) {
			if (c.near(l[0], l[1], 2)) {
				lampPost(c, l[0], y, l[1]);
			}
		}
		for (int[] b : new int[][]{{22, -66}, {50, -66}, {22, -74}, {50, -74}}) {
			if (c.near(b[0], b[1], 2)) {
				c.set(b[0], y + 1, b[1], s(Blocks.AZALEA_LEAVES).setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true));
			}
		}
		// The stronger lower camp, west of the plaza.
		if (c.near(20, -52, 6)) {
			tent(c, 17, -51, y, Blocks.LIGHT_GRAY_WOOL);
			tent(c, 23, -49, y, Blocks.BLUE_WOOL);
			c.set(20, y + 1, -54, s(Blocks.CAMPFIRE));
			c.set(16, y + 1, -55, Canvas.facing(s(Blocks.BARREL), Direction.UP));
		}
	}

	/** The temple on its stepped platform. */
	private static void templeBuilding(Canvas c) {
		int y = T_Y;
		// The platform (two steps), with stairs up the front.
		for (int x = TP_X1; x <= TP_X2; x++) {
			for (int z = TP_Z1; z <= TP_Z2; z++) {
				c.set(x, y + 1, z, s(Blocks.SMOOTH_QUARTZ));
				c.set(x, y + 2, z, (x + z) % 2 == 0 ? s(Blocks.QUARTZ_BRICKS) : s(Blocks.SMOOTH_QUARTZ));
				c.fill(x, y + 3, z, x, y + 12, z, s(Blocks.AIR));
			}
		}
		for (int x = TP_X1 - 1; x <= TP_X2 + 1; x++) {
			c.set(x, y + 1, TP_Z2 + 1, stair(Blocks.QUARTZ_STAIRS, Direction.NORTH, false));
			c.set(x, y + 1, TP_Z1 - 1, stair(Blocks.QUARTZ_STAIRS, Direction.SOUTH, false));
		}
		for (int z = TP_Z1; z <= TP_Z2; z++) {
			c.set(TP_X1 - 1, y + 1, z, stair(Blocks.QUARTZ_STAIRS, Direction.EAST, false));
			c.set(TP_X2 + 1, y + 1, z, stair(Blocks.QUARTZ_STAIRS, Direction.WEST, false));
		}
		for (int x = 33; x <= 39; x++) {
			c.set(x, y + 2, TP_Z2 + 1, stair(Blocks.QUARTZ_STAIRS, Direction.NORTH, false));
			c.set(x, y + 1, TP_Z2 + 2, stair(Blocks.QUARTZ_STAIRS, Direction.NORTH, false));
			c.set(x, y + 1, TP_Z2 + 1, s(Blocks.SMOOTH_QUARTZ));
		}
		int fy = TP_Y; // floor
		// Columns: six along the front (a wide gap at the door), five down each side, four along the back.
		int[] front = {27, 30, 33, 39, 42, 45};
		java.util.List<int[]> cols = new java.util.ArrayList<>();
		for (int x : front) {
			cols.add(new int[]{x, -62});
			cols.add(new int[]{x, -77});
		}
		for (int z = -65; z >= -74; z -= 3) {
			cols.add(new int[]{27, z});
			cols.add(new int[]{45, z});
		}
		for (int[] col : cols) {
			c.set(col[0], fy + 1, col[1], s(Blocks.CHISELED_QUARTZ_BLOCK));
			c.fill(col[0], fy + 2, col[1], col[0], fy + 8, col[1], s(Blocks.QUARTZ_PILLAR));
			c.set(col[0], fy + 9, col[1], s(Blocks.CHISELED_QUARTZ_BLOCK));
		}
		// The cella: walls inside the columns, a door in front, windows, a blue carpet to the altar.
		for (int x = 29; x <= 43; x++) {
			for (int z = -75; z <= -64; z++) {
				boolean wall = x == 29 || x == 43 || z == -75 || z == -64;
				if (!wall) {
					continue;
				}
				for (int k = 1; k <= 9; k++) {
					boolean window = (k == 4 || k == 5) && ((x == 29 || x == 43) && Math.floorMod(z, 3) == 0);
					c.set(x, fy + k, z, window ? s(Blocks.LIGHT_BLUE_STAINED_GLASS) : k == 9 ? s(Blocks.CHISELED_QUARTZ_BLOCK) : s(Blocks.QUARTZ_BRICKS));
				}
			}
		}
		for (int x = 35; x <= 37; x++) {
			for (int k = 1; k <= 5; k++) {
				if (k == 5 && x != 36) {
					c.set(x, fy + k, -64, stair(Blocks.QUARTZ_STAIRS, x == 35 ? Direction.EAST : Direction.WEST, true));
				} else {
					c.set(x, fy + k, -64, s(Blocks.AIR));
				}
			}
		}
		for (int z = -73; z <= -65; z++) {
			c.set(36, fy + 1, z, s(Blocks.BLUE_CARPET));
		}
		c.fill(34, fy + 1, -74, 38, fy + 1, -74, s(Blocks.QUARTZ_BRICKS));
		c.set(34, fy + 1, -74, stair(Blocks.QUARTZ_STAIRS, Direction.WEST, false));
		c.set(38, fy + 1, -74, stair(Blocks.QUARTZ_STAIRS, Direction.EAST, false));
		c.set(36, fy + 2, -74, s(Blocks.LAPIS_BLOCK));
		c.set(36, fy + 3, -74, lantern(Blocks.SOUL_LANTERN, false));
		c.set(32, fy + 8, -70, lantern(Blocks.LANTERN, true));
		c.set(40, fy + 8, -70, lantern(Blocks.LANTERN, true));
		c.fill(30, fy + 9, -74, 42, fy + 9, -65, s(Blocks.SMOOTH_QUARTZ));
		c.set(32, fy + 9, -70, s(Blocks.SMOOTH_QUARTZ));
		// Entablature over the columns: a pale beam and the blue frieze, a pale cornice.
		for (int x = TP_X1; x <= TP_X2; x++) {
			for (int z = TP_Z1; z <= TP_Z2 - 1; z++) {
				boolean rim = x == TP_X1 || x == TP_X2 || z == TP_Z1 || z == TP_Z2 - 1;
				c.set(x, fy + 10, z, rim ? s(Blocks.SMOOTH_QUARTZ) : s(Blocks.QUARTZ_BRICKS));
				c.set(x, fy + 11, z, rim ? s(Blocks.BLUE_CONCRETE) : s(Blocks.QUARTZ_BRICKS));
			}
		}
		// The roof: a pale gable along z, with blue pediments at both ends and gold at the apex and corners.
		int ry = fy + 12, half = (TP_X2 - TP_X1) / 2 + 1;
		for (int i = 0; i <= half; i++) {
			int xa = TP_X1 - 1 + i, xb = TP_X2 + 1 - i, yy = ry + i;
			for (int z = TP_Z1 - 1; z <= TP_Z2; z++) {
				if (xa == xb) {
					c.set(xa, yy, z, Kit.slab(Blocks.SMOOTH_QUARTZ_SLAB, false));
					continue;
				}
				if (xa > xb) {
					continue;
				}
				c.set(xa, yy, z, stair(Blocks.QUARTZ_STAIRS, Direction.EAST, false));
				c.set(xb, yy, z, stair(Blocks.QUARTZ_STAIRS, Direction.WEST, false));
				boolean end = z == TP_Z1 - 1 || z == TP_Z2;
				for (int x = xa + 1; x <= xb - 1; x++) {
					c.set(x, yy, z, end ? (x == xa + 1 || x == xb - 1 ? s(Blocks.SMOOTH_QUARTZ) : s(Blocks.BLUE_CONCRETE)) : s(Blocks.QUARTZ_BRICKS));
				}
			}
		}
		// A pale ring motif in each pediment, gold at the top.
		for (int z : new int[]{TP_Z1 - 1, TP_Z2}) {
			for (int dx = -2; dx <= 2; dx++) {
				for (int dy = -2; dy <= 2; dy++) {
					double d = Math.hypot(dx, dy);
					if (d > 1.4 && d < 2.6) {
						c.set(36 + dx, ry + 4 + dy, z, s(Blocks.SMOOTH_QUARTZ));
					}
				}
			}
			c.set(36, ry + 4, z, s(Blocks.GOLD_BLOCK));
			c.set(36, ry + half + 1, z, s(Blocks.GOLD_BLOCK));
		}
	}

	/**
	 * The curved arched colonnade behind the temple: a half ring of pale piers and round arches with a blue band and a
	 * cornice, standing on a pale footing.
	 */
	private static void colonnade(Canvas c) {
		if (!c.near(COL_X, COL_Z - 8, COL_R + 4)) {
			return;
		}
		int y = T_Y;
		for (int dx = -COL_R - 1; dx <= COL_R + 1; dx++) {
			for (int dz = -COL_R - 1; dz <= 0; dz++) {
				double d = Math.hypot(dx, dz);
				if (d < COL_R - 0.5 || d > COL_R + 0.5) {
					continue;
				}
				int x = COL_X + dx, z = COL_Z + dz;
				double theta = Math.atan2(-dz, dx); // 0 (east) .. pi (west), through north
				double s = theta * COL_R;
				double m = s % 6.0;
				c.set(x, y, z, s(Blocks.SMOOTH_QUARTZ));
				for (int k = 1; k <= 10; k++) {
					BlockState b;
					boolean pier = m < 1.6 || s < 1.2 || s > Math.PI * COL_R - 1.2;
					int open = 5 + (int) Math.round(1.8 * Math.sin(Math.PI * Math.max(0, Math.min(1, (m - 1.6) / 4.4))));
					if (k == 9) {
						b = s(Blocks.BLUE_CONCRETE);
					} else if (k == 10) {
						b = s(Blocks.SMOOTH_QUARTZ);
					} else if (!pier && k <= open) {
						b = s(Blocks.AIR);
					} else if (pier && k == 1) {
						b = s(Blocks.CHISELED_QUARTZ_BLOCK);
					} else {
						b = pier ? s(Blocks.QUARTZ_PILLAR) : s(Blocks.QUARTZ_BRICKS);
					}
					c.set(x, y + k, z, b);
				}
				c.set(x, y + 11, z, Kit.slab(Blocks.SMOOTH_QUARTZ_SLAB, false));
			}
		}
		// The two ends: a taller pier with a gold cap.
		for (int sgn : new int[]{-1, 1}) {
			int x = COL_X + sgn * COL_R;
			c.fill(x, y + 1, COL_Z, x, y + 11, COL_Z, s(Blocks.QUARTZ_PILLAR));
			c.set(x, y + 12, COL_Z, s(Blocks.GOLD_BLOCK));
		}
	}

	// ---------------------------------------------------------------------------------------------------- Skyreach Citadel

	/**
	 * Skyreach Citadel: courtyard, the wings and corner spires, the entrance hall with its spires, the domed chamber and
	 * everything in it, the return arch.
	 */
	private void citadel(Canvas c) {
		int y = K_Y;
		// The courtyard: paving from the return arch to the doors, lamps, trees, the camps.
		pave(c, y, 4, -10, -112, -10, -126);
		pave(c, y, 3, -10, -120, -30, -124);
		pave(c, y, 3, -10, -120, 10, -124);
		if (c.near(-10, -120, 8)) {
			for (int dx = -5; dx <= 5; dx++) {
				for (int dz = -4; dz <= 4; dz++) {
					if (Math.hypot(dx, dz * 1.2) <= 5.2) {
						c.set(-10 + dx, y, -120 + dz, Math.floorMod(dx + dz, 3) == 0 ? s(Blocks.POLISHED_ANDESITE) : s(Blocks.STONE_BRICKS));
					}
				}
			}
		}
		for (int[] l : new int[][]{{-14, -116}, {-6, -116}, {-14, -125}, {-6, -125}}) {
			if (c.near(l[0], l[1], 2)) {
				lampPost(c, l[0], y, l[1]);
			}
		}
		tree(c, -24, y, -117, 6, Blocks.DARK_OAK_LOG, Blocks.DARK_OAK_LEAVES);
		tree(c, 4, y, -117, 6, Blocks.DARK_OAK_LOG, Blocks.DARK_OAK_LEAVES);
		if (c.near(K_X, K_Z, K_RX + 2)) {
			meadow(c, K_X - K_RX, K_Z - K_RZ, K_X + K_RX, K_Z + K_RZ, y, 13);
		}
		if (c.near(CH_X, CH_Z - 2, 36)) {
			wing(c, -38, -24);
			wing(c, 4, 18);
			for (int[] t : new int[][]{{-39, -157}, {-39, -139}, {19, -157}, {19, -139}}) {
				spire(c, t[0], t[1], y, 3, 24);
			}
			entrance(c);
			chamber(c);
		}
	}

	/** A castle wing (x1..x2, z -156..-140): weathered walls, windows, a blue-green hipped roof, a hall inside. */
	private static void wing(Canvas c, int x1, int x2) {
		int y = K_Y, z1 = -156, z2 = -140, top = y + 12;
		for (int x = x1; x <= x2; x++) {
			for (int z = z1; z <= z2; z++) {
				boolean edge = x == x1 || x == x2 || z == z1 || z == z2;
				c.set(x, y, z, edge ? aged(x, y, z) : s(Blocks.DARK_OAK_PLANKS));
				for (int yy = y + 1; yy <= top; yy++) {
					if (!edge) {
						c.set(x, yy, z, s(Blocks.AIR));
						continue;
					}
					boolean corner = (x == x1 || x == x2) && (z == z1 || z == z2);
					int k = yy - y;
					boolean window = !corner && (k >= 3 && k <= 5 || k >= 8 && k <= 10) && Math.floorMod(x == x1 || x == x2 ? z : x, 3) == 1;
					c.set(x, yy, z, corner ? s(Blocks.POLISHED_ANDESITE) : window ? s(Blocks.GLASS_PANE) : k == 7 || k == 12 ? s(Blocks.POLISHED_DIORITE) : aged(x, yy, z));
				}
			}
		}
		// Glass panes join along their wall.
		for (int x = x1; x <= x2; x++) {
			for (int z = z1; z <= z2; z++) {
				boolean edgeX = x == x1 || x == x2, edgeZ = z == z1 || z == z2;
				if ((edgeX || edgeZ) && !(edgeX && edgeZ)) {
					for (int yy = y + 1; yy <= top; yy++) {
						int k = yy - y;
						if ((k >= 3 && k <= 5 || k >= 8 && k <= 10) && Math.floorMod(edgeX ? z : x, 3) == 1) {
							c.set(x, yy, z, Kit.connected(Blocks.LIGHT_BLUE_STAINED_GLASS_PANE, !edgeX));
						}
					}
				}
			}
		}
		// An upper floor (a balcony along the back) and lanterns.
		c.fill(x1 + 1, y + 6, z1 + 1, x2 - 1, y + 6, z1 + 4, s(Blocks.DARK_OAK_PLANKS));
		for (int x = x1 + 1; x <= x2 - 1; x++) {
			c.set(x, y + 7, z1 + 5, Kit.connected(Blocks.DARK_OAK_FENCE, true));
		}
		int lx = (x1 + x2) / 2;
		for (int yy = y + 1; yy <= y + 6; yy++) {
			c.set(lx, yy, z1 + 4, s(Blocks.DARK_OAK_PLANKS)); // the ladder's backing, under the balcony
			c.set(lx, yy, z1 + 5, Canvas.facing(s(Blocks.LADDER), Direction.SOUTH));
		}
		c.set(lx, y + 7, z1 + 5, s(Blocks.AIR));
		c.set(lx, y + 6, z1 + 5, Canvas.facing(s(Blocks.LADDER), Direction.SOUTH));
		c.set(lx, y + 6, z1 + 4, s(Blocks.DARK_OAK_PLANKS));
		for (int x = x1 + 3; x <= x2 - 3; x += 5) {
			c.set(x, top, (z1 + z2) / 2 + 2, lantern(Blocks.LANTERN, true));
			spiralBanner(c, x, y + 7, z2 - 1, Direction.NORTH);
		}
		c.set(x1 + 2, y + 1, z2 - 2, Canvas.facing(s(Blocks.BARREL), Direction.UP));
		c.set(x2 - 2, y + 1, z2 - 2, s(Blocks.BOOKSHELF));
		c.set(x2 - 2, y + 2, z2 - 2, s(Blocks.BOOKSHELF));
		Kit.hipRoof(c, x1 - 1, z1 - 1, x2 + 1, z2 + 1, top + 1, Blocks.DARK_PRISMARINE_STAIRS, Blocks.DARK_PRISMARINE_SLAB, Blocks.DARK_PRISMARINE);
	}

	/** A round corner spire: weathered walls, slit windows, a tall pointed blue-green cap with a gold tip. */
	private static void spire(Canvas c, int cx, int cz, int y, int r, int h) {
		if (!c.near(cx, cz, r + 3)) {
			return;
		}
		for (int dx = -r; dx <= r; dx++) {
			for (int dz = -r; dz <= r; dz++) {
				double d = Math.hypot(dx, dz);
				if (d > r + 0.4) {
					continue;
				}
				boolean shell = d > r - 0.8;
				c.set(cx + dx, y, cz + dz, aged(cx + dx, y, cz + dz));
				for (int yy = y + 1; yy <= y + h; yy++) {
					int k = yy - y;
					boolean slit = shell && (dx == 0 || dz == 0) && k % 7 == 4;
					c.set(cx + dx, yy, cz + dz, !shell ? s(Blocks.AIR) : slit ? s(Blocks.LIGHT_BLUE_STAINED_GLASS) : k == h ? s(Blocks.POLISHED_DIORITE) : aged(cx + dx, yy, cz + dz));
				}
			}
		}
		// The cap: a steep cone, two layers per step.
		int R = r + 1, cy = y + h + 1;
		for (int i = 0; i <= R * 2; i++) {
			double rr = R - i / 2.0;
			for (int dx = -R; dx <= R; dx++) {
				for (int dz = -R; dz <= R; dz++) {
					double d = Math.hypot(dx, dz);
					if (d <= rr + 0.4 && d > rr - 1.0) {
						c.set(cx + dx, cy + i, cz + dz, rr < 0.6 ? s(Blocks.DARK_PRISMARINE) : stair(Blocks.DARK_PRISMARINE_STAIRS, inward(dx, dz), false));
					} else if (d <= rr - 1.0) {
						c.set(cx + dx, cy + i, cz + dz, s(Blocks.DARK_PRISMARINE));
					}
				}
			}
		}
		c.set(cx, cy + R * 2 + 1, cz, s(Blocks.GOLD_BLOCK));
		c.set(cx, cy + R * 2 + 2, cz, s(Blocks.LIGHTNING_ROD));
	}

	/** The entrance hall in front of the chamber: an arched doorway, two slender spires, battlements, a blue band. */
	private static void entrance(Canvas c) {
		int y = K_Y, x1 = -16, x2 = -4, z1 = -137, z2 = -126, top = y + 11;
		for (int x = x1; x <= x2; x++) {
			for (int z = z1; z <= z2; z++) {
				boolean edge = x == x1 || x == x2 || z <= z1 + 3 || z == z2;
				c.set(x, y, z, edge ? aged(x, y, z) : s(Blocks.POLISHED_ANDESITE));
				for (int yy = y + 1; yy <= top; yy++) {
					c.set(x, yy, z, !edge ? s(Blocks.AIR) : yy - y == 9 ? s(Blocks.BLUE_CONCRETE) : aged(x, yy, z));
				}
				c.set(x, top + 1, z, edge ? aged(x, top + 1, z) : s(Blocks.STONE_BRICK_SLAB));
				boolean rim = x == x1 || x == x2 || z == z1 || z == z2;
				if (rim && Math.floorMod(x + z, 2) == 0) {
					c.set(x, top + 2, z, aged(x, top + 2, z));
				}
			}
		}
		// The doorway arch, cut through the front and (later, after the chamber wall) through the back.
		doorway(c, z2, z2);
		for (int x = x1 + 2; x <= x2 - 2; x += 4) {
			c.set(x, top, (z1 + z2) / 2 + 2, lantern(Blocks.LANTERN, true));
		}
		spiralBanner(c, x1 + 1, y + 5, -130, Direction.EAST);
		spiralBanner(c, x2 - 1, y + 5, -130, Direction.WEST);
		spiralBanner(c, -13, y + 7, z2 + 1, Direction.SOUTH);
		spiralBanner(c, -7, y + 7, z2 + 1, Direction.SOUTH);
		// The steps down to the courtyard.
		for (int x = -12; x <= -8; x++) {
			c.set(x, y, z2 + 1, s(Blocks.POLISHED_ANDESITE));
		}
		spire(c, -18, -127, y, 2, 16);
		spire(c, -2, -127, y, 2, 16);
	}

	/** The arched doorway (five wide, seven high) from z1 to z2 through whatever stands there. */
	private static void doorway(Canvas c, int z1, int z2) {
		for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) {
			for (int x = -12; x <= -8; x++) {
				int ax = Math.abs(x + 10);
				int h = ax <= 0 ? 7 : ax == 1 ? 7 : 6;
				c.fill(x, K_Y + 1, z, x, K_Y + h - (ax == 2 ? 1 : 0), z, s(Blocks.AIR));
			}
		}
	}

	/**
	 * The domed chamber: a round hall (inner radius CH_R) of weathered stone with tall windows, a gallery ring reached by
	 * ladders, spiral banners round the wall, the floor mechanism (rings, spokes and teeth round a gold hub), the ring
	 * chandelier of soul lanterns hung on chains, and the great blue dome with ribs and a gold lantern on top.
	 */
	private static void chamber(Canvas c) {
		int y = K_Y, wallTop = DOME_Y;
		for (int dx = -CH_R - 3; dx <= CH_R + 3; dx++) {
			for (int dz = -CH_R - 3; dz <= CH_R + 3; dz++) {
				double d = Math.hypot(dx, dz);
				int x = CH_X + dx, z = CH_Z + dz;
				if (d > CH_R + 2.0 || !c.inChunk(x, z)) {
					continue;
				}
				double ang = Math.toDegrees(Math.atan2(dz, dx));
				if (d <= CH_R + 0.5) {
					c.fill(x, y + 1, z, x, wallTop, z, s(Blocks.AIR));
					c.set(x, y, z, floor(dx, dz, d, ang));
				} else {
					c.set(x, y, z, aged(x, y, z));
					for (int yy = y + 1; yy <= wallTop; yy++) {
						int k = yy - y;
						boolean window = k >= 3 && k <= 8 && isWindow(ang);
						BlockState b = window ? s(Blocks.LIGHT_BLUE_STAINED_GLASS) : k == 9 || k == 21 ? s(Blocks.POLISHED_DIORITE) : aged(x, yy, z);
						c.set(x, yy, z, b);
					}
				}
			}
		}
		// The dome: a shell of blue with light ribs, from the wall top up, overhanging the wall a little.
		int R = CH_R + 2;
		for (int dx = -R - 1; dx <= R + 1; dx++) {
			for (int dz = -R - 1; dz <= R + 1; dz++) {
				int x = CH_X + dx, z = CH_Z + dz;
				if (!c.inChunk(x, z)) {
					continue;
				}
				double ang = Math.toDegrees(Math.atan2(dz, dx));
				boolean rib = Math.abs(((ang % 45) + 45) % 45 - 22.5) > 20.5;
				for (int dy = 1; dy <= R + 1; dy++) {
					double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
					if (d <= R + 0.5 && d > R - 0.9) {
						c.set(x, wallTop + dy, z, dy <= 1 ? s(Blocks.POLISHED_DIORITE) : rib ? s(Blocks.LIGHT_BLUE_TERRACOTTA) : s(Blocks.BLUE_CONCRETE));
					} else if (d <= R - 0.9) {
						c.set(x, wallTop + dy, z, s(Blocks.AIR));
					}
				}
			}
		}
		// The lantern on top of the dome.
		int apex = wallTop + R + 1;
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				c.set(CH_X + dx, apex, CH_Z + dz, s(Blocks.GOLD_BLOCK));
				if (dx != 0 && dz != 0) {
					c.fill(CH_X + dx, apex + 1, CH_Z + dz, CH_X + dx, apex + 2, CH_Z + dz, s(Blocks.POLISHED_DIORITE));
				}
			}
		}
		c.set(CH_X, apex + 1, CH_Z, s(Blocks.SEA_LANTERN));
		c.fill(CH_X - 1, apex + 3, CH_Z - 1, CH_X + 1, apex + 3, CH_Z + 1, s(Blocks.BLUE_CONCRETE));
		c.set(CH_X, apex + 4, CH_Z, s(Blocks.GOLD_BLOCK));
		c.set(CH_X, apex + 5, CH_Z, s(Blocks.LIGHTNING_ROD));
		// Doorways: south into the entrance hall, west and east into the wings.
		doorway(c, -137, -133);
		for (int sgn : new int[]{-1, 1}) {
			int xa = CH_X + sgn * (CH_R - 1), xb = CH_X + sgn * (CH_R + 4);
			for (int z = CH_Z - 1; z <= CH_Z + 1; z++) {
				c.fill(Math.min(xa, xb), y + 1, z, Math.max(xa, xb), y + 4, z, s(Blocks.AIR));
			}
			c.fill(Math.min(xa, xb), y, CH_Z - 1, Math.max(xa, xb), y, CH_Z + 1, s(Blocks.POLISHED_ANDESITE));
		}
		gallery(c);
		chandelier(c, apex);
		// Spiral banners between the windows, facing in, each on a wall block.
		for (int a = 15; a < 360; a += 30) {
			double rad = Math.toRadians(a);
			for (double r = CH_R; r >= CH_R - 1.5; r -= 0.5) {
				int bx = (int) Math.round(CH_X + Math.cos(rad) * r), bz = (int) Math.round(CH_Z + Math.sin(rad) * r);
				Direction in = inward(bx - CH_X, bz - CH_Z);
				int wx = bx - in.getStepX(), wz = bz - in.getStepZ();
				if (Math.hypot(bx - CH_X, bz - CH_Z) <= CH_R + 0.5 && Math.hypot(wx - CH_X, wz - CH_Z) > CH_R + 0.5) {
					spiralBanner(c, bx, y + 6, bz, in);
					break;
				}
			}
		}
		// Hidden light under the gallery and round the dome's spring, so the hall is lit but still moody.
		for (int a = 0; a < 360; a += 30) {
			double rad = Math.toRadians(a + 15);
			int lx = (int) Math.round(CH_X + Math.cos(rad) * (CH_R - 1)), lz = (int) Math.round(CH_Z + Math.sin(rad) * (CH_R - 1));
			c.set(lx, y + 9, lz, s(Blocks.LIGHT).setValue(net.minecraft.world.level.block.LightBlock.LEVEL, 11));
			c.set(lx, DOME_Y + 1, lz, s(Blocks.LIGHT).setValue(net.minecraft.world.level.block.LightBlock.LEVEL, 10));
		}
		// Soul lanterns on posts round the mechanism.
		for (int a = 45; a < 360; a += 90) {
			double rad = Math.toRadians(a);
			int lx = (int) Math.round(CH_X + Math.cos(rad) * 8.5), lz = (int) Math.round(CH_Z + Math.sin(rad) * 8.5);
			c.set(lx, y + 1, lz, s(Blocks.POLISHED_BLACKSTONE_WALL));
			c.set(lx, y + 2, lz, lantern(Blocks.SOUL_LANTERN, false));
		}
	}

	/** Tall windows round the chamber, every 30 degrees, skipping the doorways (east 0, south 90, west 180). */
	private static boolean isWindow(double ang) {
		double a = ((ang % 360) + 360) % 360;
		for (int w = 30; w < 360; w += 30) {
			if (w == 90 || w == 180 || w == 0) {
				continue;
			}
			if (Math.abs(a - w) < 2.6) {
				return true;
			}
		}
		return false;
	}

	/**
	 * The chamber floor: weathered stone, with the mechanism set in the middle: an outer copper ring with teeth, an inner
	 * ring of dark stone, eight copper spokes, and the gold hub.
	 */
	private static BlockState floor(int dx, int dz, double d, double ang) {
		double a = ((ang % 360) + 360) % 360;
		if (d <= 1.5) {
			return s(Blocks.GOLD_BLOCK);
		}
		if (d <= 2.5) {
			return s(Blocks.POLISHED_BLACKSTONE);
		}
		boolean spoke = false;
		for (int k = 0; k < 8; k++) {
			double r = Math.toRadians(k * 45);
			double along = dx * Math.cos(r) + dz * Math.sin(r), across = -dx * Math.sin(r) + dz * Math.cos(r);
			spoke |= along > 0 && Math.abs(across) < 0.6;
		}
		if (d <= 5.5 && d > 4.5) {
			return s(Blocks.POLISHED_DEEPSLATE);
		}
		if (d <= 9.5 && d > 8.4) {
			return s(Blocks.WAXED_CUT_COPPER);
		}
		if (d <= 10.5 && d > 9.5 && Math.floorMod((int) Math.round(a / 7.5), 2) == 0) {
			return s(Blocks.WAXED_COPPER_BLOCK);
		}
		if (d <= 8.4 && spoke) {
			return s(Blocks.WAXED_CUT_COPPER);
		}
		if (d <= 8.4) {
			return s(Blocks.POLISHED_ANDESITE);
		}
		return Math.floorMod(dx + dz, 2) == 0 ? s(Blocks.STONE_BRICKS) : s(Blocks.POLISHED_ANDESITE);
	}

	/** The gallery: a ring of dark planks round the wall at the window tops, railed, with three ladders up. */
	private static void gallery(Canvas c) {
		int gy = K_Y + 10;
		int[][] ladders = {{CH_X, CH_Z - CH_R}, {CH_X - CH_R + 1, CH_Z - 5}, {CH_X + CH_R - 1, CH_Z - 5}};
		for (int dx = -CH_R; dx <= CH_R; dx++) {
			for (int dz = -CH_R; dz <= CH_R; dz++) {
				double d = Math.hypot(dx, dz);
				int x = CH_X + dx, z = CH_Z + dz;
				if (d <= CH_R + 0.5 && d > CH_R - 2.5) {
					c.set(x, gy, z, s(Blocks.DARK_OAK_PLANKS));
				} else if (d <= CH_R - 2.5 && d > CH_R - 3.5) {
					c.set(x, gy, z, s(Blocks.DARK_OAK_SLAB).setValue(net.minecraft.world.level.block.SlabBlock.TYPE,
							net.minecraft.world.level.block.state.properties.SlabType.TOP));
					c.set(x, gy + 1, z, s(Blocks.DARK_OAK_FENCE));
				}
			}
		}
		for (int[] l : ladders) {
			int dx = l[0] - CH_X, dz = l[1] - CH_Z;
			Direction in = inward(dx, dz);
			for (int yy = K_Y + 1; yy <= gy + 1; yy++) {
				c.set(l[0], yy, l[1], Canvas.facing(s(Blocks.LADDER), in));
			}
			c.set(l[0], gy + 2, l[1], s(Blocks.AIR));
		}
		for (int a = 0; a < 360; a += 60) {
			double rad = Math.toRadians(a);
			int lx = (int) Math.round(CH_X + Math.cos(rad) * (CH_R - 1)), lz = (int) Math.round(CH_Z + Math.sin(rad) * (CH_R - 1));
			c.set(lx, gy + 1, lz, lantern(Blocks.LANTERN, false));
		}
	}

	/**
	 * The ring chandelier: a dark ring hung from the dome on four chains, with eight soul lanterns underneath, chain
	 * spokes to a hub that carries a ninth lantern, and a chain from the hub up to the dome's crown.
	 */
	private static void chandelier(Canvas c, int apex) {
		int ry = K_Y + 16;
		double rr = 6;
		for (int dx = -7; dx <= 7; dx++) {
			for (int dz = -7; dz <= 7; dz++) {
				double d = Math.hypot(dx, dz);
				if (Math.abs(d - rr) <= 0.5) {
					c.set(CH_X + dx, ry, CH_Z + dz, s(Blocks.POLISHED_BLACKSTONE));
				}
			}
		}
		for (int a = 0; a < 360; a += 45) {
			double rad = Math.toRadians(a);
			int lx = (int) Math.round(CH_X + Math.cos(rad) * rr), lz = (int) Math.round(CH_Z + Math.sin(rad) * rr);
			c.set(lx, ry, lz, s(Blocks.POLISHED_BLACKSTONE));
			c.set(lx, ry - 1, lz, lantern(Blocks.SOUL_LANTERN, true));
		}
		int R = CH_R + 2;
		for (int[] p : new int[][]{{6, 0}, {-6, 0}, {0, 6}, {0, -6}}) {
			int top = DOME_Y + (int) Math.floor(Math.sqrt(Math.max(0, (R - 1) * (R - 1) - 36)));
			c.fill(CH_X + p[0], ry + 1, CH_Z + p[1], CH_X + p[0], top, CH_Z + p[1], s(Blocks.CHAIN));
		}
		for (int k = 1; k <= 5; k++) {
			c.set(CH_X + k, ry, CH_Z, s(Blocks.CHAIN).setValue(ChainBlock.AXIS, Direction.Axis.X));
			c.set(CH_X - k, ry, CH_Z, s(Blocks.CHAIN).setValue(ChainBlock.AXIS, Direction.Axis.X));
			c.set(CH_X, ry, CH_Z + k, s(Blocks.CHAIN).setValue(ChainBlock.AXIS, Direction.Axis.Z));
			c.set(CH_X, ry, CH_Z - k, s(Blocks.CHAIN).setValue(ChainBlock.AXIS, Direction.Axis.Z));
		}
		c.set(CH_X, ry, CH_Z, s(Blocks.GOLD_BLOCK));
		c.set(CH_X, ry - 1, CH_Z, lantern(Blocks.SOUL_LANTERN, true));
		c.fill(CH_X, ry + 1, CH_Z, CH_X, apex - 2, CH_Z, s(Blocks.CHAIN));
	}
}
