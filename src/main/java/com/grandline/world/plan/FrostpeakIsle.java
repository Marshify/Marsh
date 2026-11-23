package com.grandline.world.plan;

import com.grandline.world.plan.Anchor.Type;
import com.grandline.world.plan.Terrain.Ground;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * Frostpeak Isle (Snow Island / Frozen Village), rebuilt to the owner's specification (see docs/FROSTPEAK_ISLE.md).
 * Island-local coordinates: +x east, +z south (the coast with the two docks); sea level y 62.
 * <ul>
 * <li>Layered snowy landmass about 128 x 112 with dark tuff/deepslate cliffs: lower coast y 66, terraces 69 / 73 / 77,
 * village plateau 81, arena plateau 82, a northern ridge to ~89.</li>
 * <li>West: the circular frozen arena (outer diameter 43, a 3-wide brown ring road, a frozen lake 19 x 17). East: the
 * timber village (six houses, three open-frame watchtowers, palisades, winding brown roads).</li>
 * <li>Giant cyan ice crystals (20-30 tall) and medium ones round the arena and the north; dark rock spires round the
 * coast; flat sheets of sea ice.</li>
 * <li>South coast: two docks, two market stalls, a wooden staircase and ladder up the cliff; a tunnel down to the ice
 * cavern (34 x 28) with hanging icicles, a frozen pool, ledges and a small campsite.</li>
 * </ul>
 * Built from code, identical in every world. Older worlds keep the original Frostpeak ({@link FirstSeaPlans} legacy).
 */
public final class FrostpeakIsle extends BasePlan {
	static final int SEA = 62, DOCK = 64, COAST = 66, T1 = 69, T2 = 73, T3 = 77, VILLAGE = 81, ARENA = 82;
	static final int ARENA_X = -24, ARENA_Z = -17;
	static final double ARENA_R = 21.5;
	static final int CAVE_X = 5, CAVE_Z = 8, CAVE_FLOOR = 57;

	public FrostpeakIsle() {
		super("Frostpeak Isle", 0.14);
		terrain.mass(0, 0, 62, 52, 3.5, 3.0, Ground.STONE)
				.mass(48, 38, 18, 11, 3.0, 2.5, Ground.STONE)            // south-east peninsula
				.mass(-58, -2, 13, 20, 9.0, 2.0, Ground.STONE)           // western rocky peninsula
				.mass(-10, -46, 48, 12, 27, 1.6, Ground.STONE)           // the northern ridge
				.mass(30, -50, 16, 8, 14, 1.8, Ground.STONE)
				.flat(0, 40, 52, 10, COAST, 1, null)                     // lower coast
				.flat(-38, 40, 10, 6, 65, 1, null)
				.flat(30, 44, 10, 5, COAST, 1, null)
				.flat(-50, 30, 9, 7, 67, 1, null)
				.flat(-7, 48, 13, 3, DOCK, 0, null)                      // the dock landing
				.flat(4, 26, 42, 14, T1, 1, null)                        // first terrace
				.flat(10, 12, 46, 20, T2, 1, null)                       // middle terrace
				.flat(14, -2, 48, 22, T3, 1, null)                       // upper terrace
				.flat(7, 28, 12, 8, T1, 0, null)                         // the cove under the staircase
				.flat(25, -9, 28, 23, VILLAGE, 0, null)                  // village plateau
				.flat(12, 10, 10, 10, VILLAGE, 0, null)                  // its southern lobe (top of the ladder)
				.flat(ARENA_X, ARENA_Z, 25, 24, ARENA, 1, null)          // arena plateau
				.shoreMix(s(Blocks.STONE), s(Blocks.TUFF), s(Blocks.COBBLED_DEEPSLATE), s(Blocks.GRAVEL), s(Blocks.ANDESITE))
				.reach(96);
		anchor(Anchor.at(Type.QUEST, 22, VILLAGE + 1, -1, 0));
		anchor(Anchor.at(Type.HOME, 16, VILLAGE + 1, -7, 180));
		anchor(new Anchor(Type.ARRIVAL, -7, DOCK + 1, 47, 0, 180));
		anchor(Anchor.at(Type.FISHERMAN, -14, DOCK + 1, 62, 0));
		anchor(Anchor.at(Type.DOCK, 4, 63, 58));
		anchor(new Anchor(Type.ENEMY_CAMP, ARENA_X, ARENA + 1, ARENA_Z, 12));   // the frozen arena (basic)
		anchor(Anchor.area(Type.ENEMY_CAMP, 18, -44, 8));                       // northern wilderness (stronger)
		anchor(Anchor.at(Type.BOSS_ARENA, 0, CAVE_FLOOR + 1, 8));               // the ice cavern
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

	static BlockState stair(Block b, Direction f, boolean top) {
		return Kit.stairs(b, f, top);
	}

	static BlockState log(Block b, Direction.Axis axis) {
		return s(b).setValue(RotatedPillarBlock.AXIS, axis);
	}

	static BlockState layer(int n) {
		return s(Blocks.SNOW).setValue(SnowLayerBlock.LAYERS, n);
	}

	static BlockState lantern(boolean hanging) {
		return s(Blocks.LANTERN).setValue(LanternBlock.HANGING, hanging);
	}

	/** Dark blue-grey cliff rock. */
	static BlockState rock(int x, int y, int z) {
		double u = unit(x * 3 + y * 7, z * 5 - y * 3, 4);
		return u < 0.32 ? s(Blocks.TUFF) : u < 0.58 ? s(Blocks.DEEPSLATE) : u < 0.85 ? s(Blocks.COBBLED_DEEPSLATE) : s(Blocks.POLISHED_DEEPSLATE);
	}

	/** Brown road surface. */
	static BlockState road(int x, int z) {
		double u = unit(x, z, 9);
		return u < 0.5 ? s(Blocks.COARSE_DIRT) : u < 0.8 ? s(Blocks.PACKED_MUD) : s(Blocks.BROWN_TERRACOTTA);
	}

	static BlockState ice(int x, int y, int z) {
		return unit(x * 2 + y, z * 3 - y, 13) < 0.35 ? s(Blocks.BLUE_ICE) : s(Blocks.PACKED_ICE);
	}

	/** Paints a road at the surface, clearing any snow layer on it. */
	void paveAt(Canvas c, int x, int z) {
		if (!c.inChunk(x, z)) {
			return;
		}
		int g = c.ground(x, z);
		if (g <= SEA) {
			return;
		}
		c.set(x, g, z, road(x, z));
		c.set(x, g + 1, z, s(Blocks.AIR));
	}

	/** A road of {@code width} along a polyline of (x, z) points. */
	void road(Canvas c, double width, int... pts) {
		for (int i = 0; i + 3 < pts.length; i += 2) {
			int x1 = pts[i], z1 = pts[i + 1], x2 = pts[i + 2], z2 = pts[i + 3];
			int n = Math.max(Math.abs(x2 - x1), Math.abs(z2 - z1)) * 2;
			for (int k = 0; k <= n; k++) {
				double t = k / (double) Math.max(1, n);
				double px = x1 + (x2 - x1) * t, pz = z1 + (z2 - z1) * t;
				int r = (int) Math.ceil(width / 2);
				for (int dx = -r; dx <= r; dx++) {
					for (int dz = -r; dz <= r; dz++) {
						if (dx * dx + dz * dz <= width * width / 4 + 0.3) {
							paveAt(c, (int) Math.round(px) + dx, (int) Math.round(pz) + dz);
						}
					}
				}
			}
		}
	}

	// ==================================================================================================== build

	@Override
	public void build(Canvas c) {
		surfaces(c);
		seaIce(c);
		arena(c);
		roads(c);
		spires(c);
		village(c);
		coast(c);
		staircase(c);
		trees(c);
		cavern(c);
		// Accessibility: deepslate half-steps over the little rises on the way up from the coast (no jumping).
		Kit.stepsUp(c, -11, 32, -7, 32, Direction.NORTH, Blocks.COBBLED_DEEPSLATE_STAIRS);
		Kit.stepsUp(c, -9, 30, -9, 32, Direction.EAST, Blocks.COBBLED_DEEPSLATE_STAIRS);
		Kit.stepsUp(c, -8, 30, -8, 32, Direction.EAST, Blocks.COBBLED_DEEPSLATE_STAIRS);
		Kit.stepsUp(c, -8, 31, -6, 31, Direction.NORTH, Blocks.COBBLED_DEEPSLATE_STAIRS);
	}

	/**
	 * Snow on every top; dark rock on every exposed cliff face, with brown earth bands under some ledges; a thin snow
	 * layer here and there on open ground.
	 */
	private void surfaces(Canvas c) {
		BoundingBox box = c.chunkBox();
		int ox = c.world(0, 0, 0).getX(), oz = c.world(0, 0, 0).getZ();
		for (int wx = box.minX(); wx <= box.maxX(); wx++) {
			for (int wz = box.minZ(); wz <= box.maxZ(); wz++) {
				int x = wx - ox, z = wz - oz;
				int g = c.ground(x, z);
				if (g <= SEA + 1) {
					continue;
				}
				int low = g;
				for (int dx = -2; dx <= 2; dx++) {
					for (int dz = -2; dz <= 2; dz++) {
						if (Math.abs(dx) + Math.abs(dz) <= 2) {
							low = Math.min(low, c.ground(x + dx, z + dz));
						}
					}
				}
				int drop = g - low;
				if (drop >= 2) {
					int from = Math.max(SEA - 6, low - 2);
					for (int y = from; y < g; y++) {
						c.set(x, y, z, rock(x, y, z));
					}
					if (drop >= 3 && unit(x / 5, z / 5, 21) < 0.55) {
						c.set(x, g - 1, z, s(Blocks.BROWN_TERRACOTTA));
						c.set(x, g - 2, z, s(Blocks.COARSE_DIRT));
					}
				} else {
					c.set(x, g - 1, z, s(Blocks.SNOW_BLOCK));
				}
				c.set(x, g, z, s(Blocks.SNOW_BLOCK));
				if (drop < 2 && unit(x, z, 22) < 0.3) {
					c.set(x, g + 1, z, layer(1));
				}
			}
		}
	}

	// ---------------------------------------------------------------------------------------------- sea ice

	/** Broad, angular sheets of sea ice (packed and blue ice) at the water surface; some touch the shore. */
	private void seaIce(Canvas c) {
		int[][] sheets = {{-40, 54, 11, 6, 1}, {26, 58, 12, 7, 2}, {-70, -30, 8, 12, 3}, {70, 4, 7, 11, 4}, {6, -62, 12, 6, 5},
				{54, 54, 8, 6, 6}, {-66, 34, 7, 8, 7}, {44, -58, 9, 6, 8}};
		for (int[] sh : sheets) {
			int cx = sh[0], cz = sh[1], rx = sh[2], rz = sh[3], seed = sh[4];
			if (!c.near(cx, cz, Math.max(rx, rz) + 4)) {
				continue;
			}
			for (int x = cx - rx - 3; x <= cx + rx + 3; x++) {
				for (int z = cz - rz - 3; z <= cz + rz + 3; z++) {
					if (!c.inChunk(x, z) || c.ground(x, z) >= SEA) {
						continue;
					}
					double a = Math.atan2((z - cz) / (double) rz, (x - cx) / (double) rx);
					// angular outline: a hexagon-ish polygon with uneven vertices
					double sector = Math.floorMod((int) Math.floor((a + Math.PI) / (Math.PI / 3)), 6);
					double r = 0.8 + 0.3 * unit((int) sector, seed, 31);
					double d = Math.hypot((x - cx) / (double) rx, (z - cz) / (double) rz);
					if (d <= r) {
						c.set(x, SEA, z, unit(x / 3, z / 3, seed) < 0.3 ? s(Blocks.BLUE_ICE) : s(Blocks.PACKED_ICE));
					}
				}
			}
		}
	}

	// ---------------------------------------------------------------------------------------------- the arena

	static double ringOuter(double a) {
		return ARENA_R + 0.7 * Math.sin(a * 3 + 0.6) + 0.4 * Math.sin(a * 5 + 1.9);
	}

	/**
	 * The frozen arena: a slightly irregular brown ring road (3 wide, outer diameter ~43) round an icy basin with a frozen
	 * lake (19 x 17) a block down, smaller frozen pools, snowy islets, low ridges and 14 small ice spikes.
	 */
	private void arena(Canvas c) {
		if (!c.near(ARENA_X, ARENA_Z, 26)) {
			return;
		}
		for (int x = ARENA_X - 24; x <= ARENA_X + 24; x++) {
			for (int z = ARENA_Z - 24; z <= ARENA_Z + 24; z++) {
				if (!c.inChunk(x, z)) {
					continue;
				}
				double dx = x - ARENA_X, dz = z - ARENA_Z;
				double d = Math.hypot(dx, dz), a = Math.atan2(dz, dx);
				double outer = ringOuter(a);
				if (d > outer) {
					continue;
				}
				int g = c.ground(x, z);
				if (d > outer - 3) {
					c.set(x, g, z, road(x, z));
					c.set(x, g + 1, z, s(Blocks.AIR));
					continue;
				}
				// the lake
				double lake = Math.hypot(dx / 9.5, dz / 8.5) / (1 + 0.08 * Math.sin(a * 4 + 0.3));
				if (lake < 1) {
					boolean islet = Math.hypot(dx + 3, dz - 2) < 1.6 || Math.hypot(dx - 4, dz + 3) < 1.2;
					c.set(x, g + 1, z, s(Blocks.AIR));
					if (islet) {
						c.set(x, g, z, s(Blocks.SNOW_BLOCK));
						c.set(x, g + 1, z, layer(2));
					} else {
						c.set(x, g, z, s(Blocks.AIR));
						c.set(x, g - 1, z, unit(x / 2, z / 2, 41) < 0.4 ? s(Blocks.BLUE_ICE) : s(Blocks.PACKED_ICE));
					}
					continue;
				}
				// small frozen pools, flush with the snow
				int[][] pools = {{-14, 4, 2}, {12, 6, 3}, {6, -13, 2}, {-9, -12, 2}, {15, -5, 2}};
				for (int[] p : pools) {
					if (Math.hypot(dx - p[0], dz - p[1]) <= p[2] + 0.3 * Math.sin(a * 7)) {
						c.set(x, g, z, s(Blocks.PACKED_ICE));
						c.set(x, g + 1, z, s(Blocks.AIR));
					}
				}
			}
		}
		// low frozen ridges
		int[][] ridges = {{-15, -3, -12, -7}, {10, 11, 14, 9}, {-4, 14, 2, 15}};
		for (int[] r : ridges) {
			int n = Math.max(Math.abs(r[2] - r[0]), Math.abs(r[3] - r[1]));
			for (int k = 0; k <= n; k++) {
				int x = ARENA_X + r[0] + (r[2] - r[0]) * k / Math.max(1, n), z = ARENA_Z + r[1] + (r[3] - r[1]) * k / Math.max(1, n);
				c.set(x, c.ground(x, z) + 1, z, s(Blocks.PACKED_ICE));
			}
		}
		// small ice spikes (heights 1-6), off the lake
		int[][] spikes = {{-16, -6, 2}, {-12, 9, 4}, {-6, 15, 1}, {4, 16, 3}, {14, 13, 5}, {17, 2, 2}, {16, -10, 6}, {9, -16, 3},
				{-2, -16, 2}, {-11, -14, 5}, {-17, 3, 1}, {11, 3, 2}, {-15, 13, 3}, {2, 12, 1}};
		for (int[] sp : spikes) {
			int x = ARENA_X + sp[0], z = ARENA_Z + sp[1], h = sp[2];
			int g = c.ground(x, z);
			for (int k = 1; k <= h; k++) {
				c.set(x, g + k, z, k > h - 2 ? s(Blocks.PACKED_ICE) : s(Blocks.BLUE_ICE));
				if (k <= h / 2) {
					for (Direction d : Direction.Plane.HORIZONTAL) {
						if (unit(x + d.getStepX(), z + d.getStepZ() + k, 44) < 0.6) {
							c.set(x + d.getStepX(), g + k, z + d.getStepZ(), s(Blocks.PACKED_ICE));
						}
					}
				}
			}
		}
	}

	// ---------------------------------------------------------------------------------------------- roads

	/** The village's winding roads (main 4 wide, side paths 2-3), joined to the arena ring and the ladder top. */
	private void roads(Canvas c) {
		if (!c.near(20, -8, 40)) {
			return;
		}
		road(c, 4, -3, -15, 4, -13, 12, -10, 24, -4, 36, -6, 41, -11);        // main road from the arena ring east
		road(c, 3, 20, -5, 14, -2, 8, 6, 8, 14, 12, 18);                     // branch south to the ladder
		road(c, 2, 8, -16, 8, -12);                                          // to N1
		road(c, 2, 20, -19, 18, -9);                                         // to N2
		road(c, 2, 35, -14, 35, -8);                                         // to N3
		road(c, 2, 31, -1, 31, -4);                                          // to S1
		road(c, 2, 17, 1, 17, -3);                                           // to S2
		road(c, 2, 44, -7, 41, -9);                                          // to S3
	}

	// ---------------------------------------------------------------------------------------------- spires

	/**
	 * A giant crystal: stacked irregular cross-sections that shrink and drift (lean) as they rise, each a faceted
	 * polygon (5-7 uneven sides), solid packed ice with a blue-ice core and a few light-blue glass glints; base sunk
	 * into the ground.
	 */
	void crystal(Canvas c, int cx, int cz, int height, double rx, double rz, double leanX, double leanZ, int seed, boolean rockBase) {
		if (!c.near(cx, cz, (int) Math.max(rx, rz) + (int) (Math.abs(leanX) + Math.abs(leanZ)) * height / 2 + 6)) {
			return;
		}
		int base = c.ground(cx, cz) - 3;
		int sides = 5 + seed % 3;
		double phase = unit(seed, 7, 51) * Math.PI * 2;
		for (int k = 0; k <= height + 3; k++) {
			double t = Math.max(0, (k - 3) / (double) height);
			double sx = rx * Math.pow(1 - t, 0.85), sz = rz * Math.pow(1 - t, 0.85);
			if (sx < 0.35 && sz < 0.35) {
				if (t < 1) {
					c.set((int) Math.round(cx + leanX * height * t), base + k, (int) Math.round(cz + leanZ * height * t), s(Blocks.PACKED_ICE));
				}
				continue;
			}
			double ox = cx + leanX * height * t, oz = cz + leanZ * height * t;
			int r = (int) Math.ceil(Math.max(sx, sz)) + 1;
			for (int dx = -r; dx <= r; dx++) {
				for (int dz = -r; dz <= r; dz++) {
					int x = (int) Math.round(ox) + dx, z = (int) Math.round(oz) + dz;
					double px = x - ox, pz = z - oz;
					double a = Math.atan2(pz / Math.max(0.4, sz), px / Math.max(0.4, sx));
					// faceted outline: distance to a polygon with uneven vertices
					double seg = Math.PI * 2 / sides;
					double local = Math.floorMod((int) Math.round(Math.toDegrees(a - phase)), 360) * Math.PI / 180 % seg - seg / 2;
					double facet = Math.cos(seg / 2) / Math.cos(local) * (0.85 + 0.25 * unit((int) Math.floor((a - phase) / seg), seed, 52));
					double d = Math.hypot(px / Math.max(0.4, sx), pz / Math.max(0.4, sz));
					if (d > facet) {
						continue;
					}
					BlockState b;
					if (rockBase && k < 6) {
						b = rock(x, base + k, z);
					} else if (d < facet * 0.55) {
						b = s(Blocks.BLUE_ICE);
					} else {
						b = unit(x, z + k * 7, seed) < 0.05 ? s(Blocks.LIGHT_BLUE_STAINED_GLASS) : unit(x / 2, z / 2 + k, seed + 1) < 0.35
								? s(Blocks.BLUE_ICE) : s(Blocks.PACKED_ICE);
					}
					c.set(x, base + k, z, b);
				}
			}
		}
	}

	/** A dark triangular rock spire (deepslate and tuff), leaning, with snow on its exposed ledges. */
	void rockSpire(Canvas c, int cx, int cz, int height, double rx, double rz, double leanX, double leanZ, int seed) {
		if (!c.near(cx, cz, (int) Math.max(rx, rz) + height / 2 + 6)) {
			return;
		}
		int base = Math.min(c.ground(cx, cz), SEA) - 4;
		int top = Math.max(c.ground(cx, cz), SEA) + height;
		for (int y = base; y <= top; y++) {
			double t = Math.max(0, (y - Math.max(c.ground(cx, cz), SEA)) / (double) height);
			double sx = rx * (1 - t), sz = rz * (1 - t);
			double ox = cx + leanX * height * t, oz = cz + leanZ * height * t;
			int r = (int) Math.ceil(Math.max(sx, sz)) + 1;
			for (int dx = -r; dx <= r; dx++) {
				for (int dz = -r; dz <= r; dz++) {
					int x = (int) Math.round(ox) + dx, z = (int) Math.round(oz) + dz;
					double px = x - ox, pz = z - oz;
					// a triangle in plan: three straight sides
					double a = Math.atan2(pz, px) + seed;
					double tri = Math.cos(Math.PI / 3) / Math.cos((Math.floorMod((int) Math.round(Math.toDegrees(a)), 120) - 60) * Math.PI / 180);
					double d = Math.hypot(px / Math.max(0.5, sx), pz / Math.max(0.5, sz));
					if (d > tri * 1.15 && !(sx < 0.6 && dx == 0 && dz == 0)) {
						continue;
					}
					c.set(x, y, z, unit(x, y * 3 + z, seed) < 0.55 ? s(Blocks.DEEPSLATE) : s(Blocks.TUFF));
				}
			}
			if (sx < 0.5 && sz < 0.5) {
				break;
			}
		}
	}

	private void spires(Canvas c) {
		// giant crystals round the arena and in the north
		crystal(c, -43, -35, 28, 6, 5, 0.12, -0.08, 1, false);
		crystal(c, -28, -46, 24, 5, 5, 0.0, -0.14, 2, true);
		crystal(c, -54, -20, 22, 5, 4, -0.18, 0.0, 3, false);
		crystal(c, -6, -50, 26, 6, 5, 0.08, -0.1, 4, true);
		// medium crystals
		int[][] med = {{-50, -36, 14, 3, 3}, {-38, -50, 11, 3, 2}, {-58, -8, 12, 3, 3}, {-50, 4, 10, 2, 2}, {-46, 16, 13, 3, 3},
				{28, -44, 15, 3, 3}, {40, -40, 11, 3, 2}, {52, 30, 16, 3, 3}, {58, 40, 10, 2, 2}, {-30, 30, 9, 2, 2}, {-18, -54, 10, 2, 2}};
		for (int i = 0; i < med.length; i++) {
			int[] m = med[i];
			crystal(c, m[0], m[1], m[2], m[3], m[4], (unit(i, 3, 61) - 0.5) * 0.3, (unit(i, 4, 61) - 0.5) * 0.3, 10 + i, false);
		}
		// dark rock spires round the coast, leaning out to sea
		rockSpire(c, -66, -4, 22, 4, 3, -0.12, 0.0, 1);
		rockSpire(c, -60, 26, 16, 3, 3, -0.1, 0.06, 2);
		rockSpire(c, 64, -24, 24, 4, 4, 0.1, -0.08, 3);
		rockSpire(c, 66, 14, 14, 3, 3, 0.12, 0.0, 4);
		rockSpire(c, 26, -60, 18, 4, 3, 0.0, -0.12, 5);
		rockSpire(c, -26, -60, 13, 3, 3, 0.0, -0.1, 6);
		rockSpire(c, 42, 50, 12, 3, 2, 0.08, 0.08, 7);
		rockSpire(c, -48, 48, 15, 3, 3, -0.08, 0.1, 8);
	}


	// ==================================================================================================== village

	/** A house: footprint, wall height, roof rise, door side, large (chimney, porch). */
	record House(int x1, int z1, int x2, int z2, int wall, int rise, Direction door, boolean large) {
	}

	static final House[] HOUSES = {
			new House(4, -26, 12, -16, 5, 4, Direction.SOUTH, false),     // N1 small
			new House(16, -29, 24, -19, 5, 4, Direction.SOUTH, false),    // N2 small
			new House(30, -27, 40, -14, 6, 5, Direction.SOUTH, true),     // N3 large
			new House(26, -1, 36, 12, 6, 5, Direction.NORTH, true),       // S1 large
			new House(12, 1, 22, 14, 6, 5, Direction.NORTH, true),        // S2 large
			new House(44, -12, 52, -2, 5, 4, Direction.WEST, false)};     // S3 small

	/** Watchtowers: south-west corner of each 6 x 6 footprint. */
	static final int[][] TOWERS = {{2, -4}, {44, -18}, {38, 4}};

	private void village(Canvas c) {
		if (!c.near(25, -8, 42)) {
			return;
		}
		palisades(c);
		for (House h : HOUSES) {
			house(c, h);
		}
		for (int[] t : TOWERS) {
			tower(c, t[0], t[1]);
		}
	}

	/** A dark timber house with a steep snow-covered gable roof, narrow blue windows and an open door. */
	private void house(Canvas c, House h) {
		int x1 = h.x1(), z1 = h.z1(), x2 = h.x2(), z2 = h.z2();
		if (!c.near((x1 + x2) / 2, (z1 + z2) / 2, 14)) {
			return;
		}
		int y = VILLAGE, top = y + h.wall();
		boolean ridgeX = (x2 - x1) > (z2 - z1);
		c.foundation(x1, z1, x2, z2, y, s(Blocks.COBBLED_DEEPSLATE));
		for (int x = x1; x <= x2; x++) {
			for (int z = z1; z <= z2; z++) {
				boolean ex = x == x1 || x == x2, ez = z == z1 || z == z2;
				if (!ex && !ez) {
					c.set(x, y, z, s(Blocks.SPRUCE_PLANKS));
					c.fill(x, y + 1, z, x, top - 1, z, s(Blocks.AIR));
					c.set(x, top, z, s(Blocks.DARK_OAK_PLANKS));                   // ceiling
					continue;
				}
				int along = ez ? x - x1 : z - z1, len = ez ? x2 - x1 : z2 - z1;
				boolean post = (ex && ez) || along % 4 == 0 || along == len;
				for (int yy = y + 1; yy <= top; yy++) {
					BlockState b;
					if (post) {
						b = log(Blocks.STRIPPED_DARK_OAK_LOG, Direction.Axis.Y);
					} else if (yy == top) {
						b = log(Blocks.STRIPPED_DARK_OAK_LOG, ez ? Direction.Axis.X : Direction.Axis.Z);
					} else if (yy == y + 1) {
						b = s(Blocks.SPRUCE_PLANKS);
					} else if (along % 4 == 2 && (yy == y + 2 || yy == y + 3)) {
						b = Kit.connected(Blocks.BLUE_STAINED_GLASS_PANE, ez);          // tall narrow window
					} else if (along % 4 == 2 && yy == y + 4 && h.wall() >= 5) {
						b = stair(Blocks.DARK_OAK_STAIRS, ez ? (z == z1 ? Direction.SOUTH : Direction.NORTH) : (x == x1 ? Direction.EAST : Direction.WEST), true);
					} else {
						b = s(Blocks.DARK_OAK_PLANKS);
					}
					c.set(x, yy, z, b);
				}
			}
		}
		// roof: rows from each eave to the ridge, stepping up evenly; dark trim at the eaves and gable verges, snow on top
		int ax1 = ridgeX ? x1 - 1 : z1 - 1, ax2 = ridgeX ? x2 + 1 : z2 + 1;       // along the ridge
		int s1 = ridgeX ? z1 - 1 : x1 - 1, s2 = ridgeX ? z2 + 1 : x2 + 1;          // across it
		int half = (s2 - s1) / 2;
		for (int a = ax1; a <= ax2; a++) {
			for (int b = s1; b <= s2; b++) {
				int i = Math.min(b - s1, s2 - b);
				int hgt = top + (int) Math.round(i * (h.rise() + 0.0) / half);
				int x = ridgeX ? a : b, z = ridgeX ? b : a;
				boolean verge = a == ax1 || a == ax2;
				BlockState block;
				if (i == 0) {
					Direction up = ridgeX ? (b == s1 ? Direction.SOUTH : Direction.NORTH) : (b == s1 ? Direction.EAST : Direction.WEST);
					block = stair(Blocks.DARK_OAK_STAIRS, up, false);
				} else {
					block = verge ? s(Blocks.DARK_OAK_PLANKS) : s(Blocks.SNOW_BLOCK);
				}
				c.set(x, hgt, z, block);
				if (i > 0) {
					c.set(x, hgt + 1, z, layer(verge ? 1 : 2));
				}
				// gable walls and attic fill under the slope
				for (int yy = top + 1; yy < hgt; yy++) {
					c.set(x, yy, z, verge || a == ax1 + 1 || a == ax2 - 1 ? s(Blocks.DARK_OAK_PLANKS) : s(Blocks.AIR));
				}
			}
		}
		// door, standing open, centred (moved off a post)
		Direction d = h.door();
		int dx = d.getAxis() == Direction.Axis.Z ? (x1 + x2) / 2 : (d == Direction.WEST ? x1 : x2);
		int dz = d.getAxis() == Direction.Axis.X ? (z1 + z2) / 2 : (d == Direction.NORTH ? z1 : z2);
		if (d.getAxis() == Direction.Axis.Z && Math.floorMod(dx - x1, 4) == 0) {
			dx++;
		}
		if (d.getAxis() == Direction.Axis.X && Math.floorMod(dz - z1, 4) == 0) {
			dz++;
		}
		c.fill(dx, y + 1, dz, dx, y + 3, dz, s(Blocks.AIR));
		StonewallPrisonV9.door(c, dx, y + 1, dz, Blocks.DARK_OAK_DOOR, d);
		c.set(dx, y + 3, dz, log(Blocks.STRIPPED_DARK_OAK_LOG, d.getAxis() == Direction.Axis.Z ? Direction.Axis.X : Direction.Axis.Z));
		// interior
		Kit.furnish(c, x1 + 2, z1 + 2, x2 - 2, z2 - 2, y, Kit.Interior.HOME, d);
		c.set((x1 + x2) / 2, top - 1, (z1 + z2) / 2, lantern(true));
		if (h.large()) {
			// stone chimney (2 x 2) through the roof, two above it
			int cx = ridgeX ? x2 - 2 : x1 + 1, cz = ridgeX ? z1 + 1 : z2 - 2;
			int ridge = top + h.rise();
			for (int yy = y + 1; yy <= ridge + 2; yy++) {
				for (int ddx = 0; ddx <= 1; ddx++) {
					for (int ddz = 0; ddz <= 1; ddz++) {
						c.set(cx + ddx, yy, cz + ddz, yy >= ridge + 1 ? s(Blocks.DEEPSLATE_BRICKS) : s(Blocks.STONE_BRICKS));
					}
				}
			}
			c.set(cx, ridge + 3, cz, s(Blocks.CAMPFIRE));
			// porch: two posts and a small dark roof over the door
			Direction side = d.getClockWise();
			for (int k : new int[]{-2, 2}) {
				int px = dx + d.getStepX() * 2 + side.getStepX() * k, pz = dz + d.getStepZ() * 2 + side.getStepZ() * k;
				c.fill(px, y + 1, pz, px, y + 3, pz, log(Blocks.DARK_OAK_LOG, Direction.Axis.Y));
			}
			for (int k = -2; k <= 2; k++) {
				for (int j = 1; j <= 2; j++) {
					int px = dx + d.getStepX() * j + side.getStepX() * k, pz = dz + d.getStepZ() * j + side.getStepZ() * k;
					c.set(px, y + 4, pz, Kit.slab(Blocks.DARK_OAK_SLAB, false));
					c.set(px, y + 5, pz, layer(1));
				}
			}
		}
	}

	/**
	 * An open timber watchtower (6 x 6): dark oak corner posts, beams every 4 blocks with X braces between, a ladder up a
	 * central spine to the lookout platform at +14 (railings, lanterns) and a pitched dark roof with snow.
	 */
	private void tower(Canvas c, int x, int z) {
		if (!c.near(x + 3, z + 3, 8)) {
			return;
		}
		int b = c.ground(x + 2, z + 2);
		int plat = b + 14;
		for (int dx = 0; dx <= 5; dx++) {
			for (int dz = 0; dz <= 5; dz++) {
				c.set(x + dx, b, z + dz, road(x + dx, z + dz));
				c.fill(x + dx, b + 1, z + dz, x + dx, plat + 3, z + dz, s(Blocks.AIR));
			}
		}
		for (int[] p : new int[][]{{0, 0}, {5, 0}, {0, 5}, {5, 5}}) {
			c.fill(x + p[0], b + 1, z + p[1], x + p[0], plat + 2, z + p[1], log(Blocks.DARK_OAK_LOG, Direction.Axis.Y));
		}
		for (int lvl = b + 4; lvl < plat; lvl += 4) {
			for (int k = 1; k <= 4; k++) {
				c.set(x + k, lvl, z, log(Blocks.STRIPPED_DARK_OAK_LOG, Direction.Axis.X));
				c.set(x + k, lvl, z + 5, log(Blocks.STRIPPED_DARK_OAK_LOG, Direction.Axis.X));
				c.set(x, lvl, z + k, log(Blocks.STRIPPED_DARK_OAK_LOG, Direction.Axis.Z));
				c.set(x + 5, lvl, z + k, log(Blocks.STRIPPED_DARK_OAK_LOG, Direction.Axis.Z));
			}
			// X braces in the panel above this beam (rows 1-3, columns 1-4) on all four faces
			if (lvl + 4 <= plat) {
				int[][] cells = {{1, 1}, {4, 1}, {2, 2}, {3, 2}, {1, 3}, {4, 3}};
				for (int[] cell : cells) {
					int k = cell[0], yy = lvl + cell[1];
					c.set(x + k, yy, z, Kit.connected(Blocks.DARK_OAK_FENCE, true));
					c.set(x + k, yy, z + 5, Kit.connected(Blocks.DARK_OAK_FENCE, true));
					c.set(x, yy, z + k, Kit.connected(Blocks.DARK_OAK_FENCE, false));
					c.set(x + 5, yy, z + k, Kit.connected(Blocks.DARK_OAK_FENCE, false));
				}
			}
		}
		// platform, with a hole for the ladder
		for (int dx = -1; dx <= 6; dx++) {
			for (int dz = -1; dz <= 6; dz++) {
				boolean edge = dx == -1 || dx == 6 || dz == -1 || dz == 6;
				c.set(x + dx, plat, z + dz, edge ? Kit.slab(Blocks.DARK_OAK_SLAB, true) : s(Blocks.DARK_OAK_PLANKS));
				if (edge && !((dx == -1 || dx == 6) && (dz == -1 || dz == 6))) {
					c.set(x + dx, plat + 1, z + dz, Kit.connected(Blocks.DARK_OAK_FENCE, dz == -1 || dz == 6));
				}
			}
		}
		for (int[] p : new int[][]{{-1, -1}, {6, -1}, {-1, 6}, {6, 6}}) {
			c.set(x + p[0], plat + 1, z + p[1], s(Blocks.DARK_OAK_FENCE));
			c.set(x + p[0], plat + 2, z + p[1], lantern(false));
		}
		// ladder up a central spine
		c.fill(x + 2, b + 1, z + 2, x + 2, plat, z + 2, log(Blocks.DARK_OAK_LOG, Direction.Axis.Y));
		for (int yy = b + 1; yy <= plat; yy++) {
			c.set(x + 2, yy, z + 3, s(Blocks.LADDER).setValue(LadderBlock.FACING, Direction.SOUTH));
		}
		// roof: hipped dark oak over the posts, snow on it, a lantern hanging inside
		Kit.hipRoof(c, x, z, x + 5, z + 5, plat + 3, Blocks.DARK_OAK_STAIRS, Blocks.DARK_OAK_SLAB, Blocks.DARK_OAK_PLANKS);
		c.set(x + 2, plat + 5, z + 2, s(Blocks.SNOW_BLOCK));
		c.set(x + 3, plat + 5, z + 3, s(Blocks.SNOW_BLOCK));
		c.set(x + 2, plat + 5, z + 3, s(Blocks.SNOW_BLOCK));
		c.set(x + 3, plat + 5, z + 2, s(Blocks.SNOW_BLOCK));
		c.set(x + 3, plat + 2, z + 3, lantern(true));
	}

	/** Palisade sections: dark log stakes, alternating 3 and 4 high, along parts of the plateau's outer edge. */
	private void palisades(Canvas c) {
		double[][] arcs = {{-2.9, -1.6}, {-1.3, -0.2}, {0.1, 0.6}, {0.9, 1.4}, {2.4, 3.0}};
		for (double[] arc : arcs) {
			for (double a = arc[0]; a <= arc[1]; a += 0.012) {
				int x = (int) Math.round(25 + Math.cos(a) * 26.5), z = (int) Math.round(-9 + Math.sin(a) * 21.5);
				if (!c.inChunk(x, z) || c.ground(x, z) != VILLAGE || insideBuilding(x, z)) {
					continue;
				}
				int h = Math.floorMod(x * 7 + z * 3, 2) == 0 ? 4 : 3;
				for (int k = 1; k <= h; k++) {
					c.set(x, VILLAGE + k, z, Math.floorMod(x + z, 5) == 0 ? log(Blocks.STRIPPED_DARK_OAK_LOG, Direction.Axis.Y)
							: log(Blocks.DARK_OAK_LOG, Direction.Axis.Y));
				}
				if (h == 4) {
					c.set(x, VILLAGE + 5, z, s(Blocks.DARK_OAK_FENCE));          // the pointed stake tips
				}
			}
		}
	}

	static boolean insideBuilding(int x, int z) {
		for (House h : HOUSES) {
			if (x >= h.x1() - 2 && x <= h.x2() + 2 && z >= h.z1() - 2 && z <= h.z2() + 2) {
				return true;
			}
		}
		for (int[] t : TOWERS) {
			if (x >= t[0] - 2 && x <= t[0] + 7 && z >= t[1] - 2 && z <= t[1] + 7) {
				return true;
			}
		}
		return false;
	}


	// ==================================================================================================== coast

	/** The two docks, the two market stalls between them, and the steps up from the landing to the lower coast. */
	private void coast(Canvas c) {
		if (!c.near(-7, 52, 30)) {
			return;
		}
		dock(c, -14, 17, 1);
		dock(c, -1, 15, 2);
		// Accessibility: a boat landing one block above the water, with stairs up to the deck (see Kit.boatLanding).
		Kit.boatLanding(c, 2, 61, Direction.EAST, 0, 1, DOCK, Blocks.DARK_OAK_STAIRS, s(Blocks.DARK_OAK_PLANKS),
				log(Blocks.DARK_OAK_LOG, Direction.Axis.Y), Blocks.DARK_OAK_FENCE);
		stall(c, -14);
		stall(c, -6);
		// steps from the dock landing (64) up to the lower coast (66), across the whole landing
		for (int x = -20; x <= 6; x++) {
			for (int z = 40; z <= 50; z++) {
				if (c.inChunk(x, z) && c.ground(x, z) == DOCK && c.ground(x, z - 1) == COAST) {
					// two half-steps: a stair on the landing and the coast's edge block turned into a stair
					c.set(x, DOCK + 1, z, stair(Blocks.COBBLED_DEEPSLATE_STAIRS, Direction.NORTH, false));
					c.set(x, COAST, z - 1, stair(Blocks.COBBLED_DEEPSLATE_STAIRS, Direction.NORTH, false));
					c.set(x, COAST + 1, z - 1, s(Blocks.AIR));
				}
			}
		}
		// steps from the lower coast (66) up to the first terrace (69) beside the stalls
		for (int x = 3; x <= 5; x++) {
			for (int z = 36; z <= 46; z++) {
				int g = c.ground(x, z), gn = c.ground(x, z - 1);
				if (gn > g && gn - g <= 4) {
					for (int k = 1; k <= gn - g; k++) {
						c.set(x, g + k, z + (gn - g) - k, stair(Blocks.COBBLED_DEEPSLATE_STAIRS, Direction.NORTH, false));
						c.fill(x, g, z + (gn - g) - k, x, g + k - 1, z + (gn - g) - k, s(Blocks.COBBLED_DEEPSLATE));
					}
					break;
				}
			}
		}
	}

	/** A dark oak dock from the landing at z 48, {@code length} long and 5 wide, a little wider at its end, on log posts. */
	private void dock(Canvas c, int cx, int length, int seed) {
		int z0 = 48, z1 = z0 + length - 1;
		for (int z = z0; z <= z1; z++) {
			int half = z >= z1 - 2 ? 3 : 2;
			for (int dx = -half; dx <= half; dx++) {
				int x = cx + dx;
				c.set(x, DOCK, z, s(Blocks.DARK_OAK_PLANKS));
				c.fill(x, DOCK + 1, z, x, DOCK + 3, z, s(Blocks.AIR));
				boolean edge = Math.abs(dx) == half;
				if (edge && (z - z0) % 4 == 0 || edge && z == z1) {
					c.fill(x, Terrain.FLOOR + 5, z, x, DOCK - 1, z, log(Blocks.DARK_OAK_LOG, Direction.Axis.Y));
					c.set(x, DOCK + 1, z, s(Blocks.DARK_OAK_FENCE));
				} else if (edge && z > z0 + 1) {
					c.set(x, DOCK + 1, z, Kit.slab(Blocks.DARK_OAK_SLAB, false));
				}
			}
		}
		for (int dx = -2; dx <= 2; dx++) {
			c.set(cx + dx, DOCK + 1, z1, s(Blocks.DARK_OAK_FENCE));        // the end rail
		}
		c.set(cx - 2, DOCK + 2, z1, lantern(false));
		c.set(cx + 2, DOCK + 2, z1, lantern(false));
		// barrels and a crate
		int bz = z0 + 4 + seed;
		c.set(cx + 1, DOCK + 1, bz, Canvas.facing(s(Blocks.BARREL), Direction.UP));
		c.set(cx + 1, DOCK + 1, bz + 1, s(Blocks.SPRUCE_PLANKS));
		c.set(cx + 1, DOCK + 2, bz + 1, Canvas.facing(s(Blocks.BARREL), Direction.UP));
	}

	/** An open market stall (6 wide, 4 deep, 4 high) facing the docks: four posts, a dark pitched roof, a pale counter. */
	private void stall(Canvas c, int x1) {
		int z1 = 41, z2 = 44, x2 = x1 + 5, y = c.ground(x1 + 2, z1 + 2);
		for (int x = x1; x <= x2; x++) {
			for (int z = z1; z <= z2; z++) {
				c.set(x, y, z, s(Blocks.SPRUCE_PLANKS));
				c.fill(x, y + 1, z, x, y + 4, z, s(Blocks.AIR));
			}
		}
		for (int[] p : new int[][]{{x1, z1}, {x2, z1}, {x1, z2}, {x2, z2}}) {
			c.fill(p[0], y + 1, p[1], p[0], y + 3, p[1], log(Blocks.DARK_OAK_LOG, Direction.Axis.Y));
		}
		for (int x = x1; x <= x2; x++) {
			c.set(x, y + 4, z1, stair(Blocks.DARK_OAK_STAIRS, Direction.SOUTH, false));
			c.set(x, y + 4, z1 + 1, s(Blocks.DARK_OAK_PLANKS));
			c.set(x, y + 4, z1 + 2, s(Blocks.DARK_OAK_PLANKS));
			c.set(x, y + 4, z2, stair(Blocks.DARK_OAK_STAIRS, Direction.NORTH, false));
			c.set(x, y + 5, z1 + 1, layer(2));
			c.set(x, y + 5, z1 + 2, layer(2));
		}
		for (int x = x1 + 1; x <= x2 - 1; x++) {
			c.set(x, y + 1, z2, s(Blocks.SPRUCE_PLANKS));
			c.set(x, y + 2, z2, Kit.slab(Blocks.SMOOTH_STONE_SLAB, false));
		}
		c.set(x1 + 1, y + 1, z1, Canvas.facing(s(Blocks.BARREL), Direction.UP));
		c.set(x2 - 1, y + 1, z1, s(Blocks.SPRUCE_PLANKS));
		c.set(x1 + 2, y + 3, z1 + 1, lantern(true));
	}

	// ==================================================================================================== staircase

	/**
	 * The cliffside access in the cove (y 69) under the village's southern lobe (y 81): a low scaffold walkway, a
	 * diagonal dark oak staircase rising east on framed supports, an upper scaffold platform at y 76, and a ladder up
	 * the cliff face to the plateau.
	 */
	private void staircase(Canvas c) {
		if (!c.near(4, 22, 20)) {
			return;
		}
		int z1 = 21, z2 = 23;
		// a step from the first terrace onto the walkway (accessibility: no jump)
		for (int z = z1; z <= z2; z++) {
			c.set(-5, 70, z, stair(Blocks.DARK_OAK_STAIRS, Direction.EAST, false));
			c.fill(-5, 71, z, -5, 73, z, s(Blocks.AIR));
		}
		// lower scaffold walkway x -4..-1 at y 70
		for (int x = -4; x <= -1; x++) {
			for (int z = z1; z <= z2; z++) {
				c.set(x, 70, z, s(Blocks.DARK_OAK_PLANKS));
				c.fill(x, 71, z, x, 73, z, s(Blocks.AIR));
			}
			c.set(x, 71, z2 + 1, s(Blocks.DARK_OAK_FENCE));
		}
		// the stair: x 0..5 rising 71 -> 76, three wide, with framed supports and a south rail
		for (int i = 0; i <= 5; i++) {
			int x = i, y = 71 + i;
			for (int z = z1; z <= z2; z++) {
				c.set(x, y, z, stair(Blocks.DARK_OAK_STAIRS, Direction.EAST, false));
				c.fill(x, y + 1, z, x, y + 3, z, s(Blocks.AIR));
			}
			c.set(x, y + 1, z2 + 1, s(Blocks.DARK_OAK_FENCE));
			if (i % 2 == 0) {
				c.fill(x, T1 + 1, z2 + 1, x, y, z2 + 1, log(Blocks.DARK_OAK_LOG, Direction.Axis.Y));
				c.fill(x, T1 + 1, z1, x, y - 1, z1, log(Blocks.DARK_OAK_LOG, Direction.Axis.Y));
			}
		}
		// upper scaffold platform x 6..13 at y 76
		for (int x = 6; x <= 13; x++) {
			for (int z = 20; z <= z2; z++) {
				c.set(x, 76, z, s(Blocks.DARK_OAK_PLANKS));
				c.fill(x, 77, z, x, 79, z, s(Blocks.AIR));
			}
			c.set(x, 77, z2 + 1, s(Blocks.DARK_OAK_FENCE));
			if (x % 3 == 0) {
				c.fill(x, T1 + 1, z2, x, 75, z2, log(Blocks.DARK_OAK_LOG, Direction.Axis.Y));
				c.fill(x, T1 + 1, 20, x, 75, 20, log(Blocks.DARK_OAK_LOG, Direction.Axis.Y));
			}
			c.set(x, 72, z2, log(Blocks.STRIPPED_DARK_OAK_LOG, Direction.Axis.X));     // a horizontal beam
		}
		c.set(13, 77, 21, s(Blocks.DARK_OAK_FENCE));
		c.set(13, 78, 21, lantern(false));
		c.set(7, 77, 22, Canvas.facing(s(Blocks.BARREL), Direction.UP));
		c.set(8, 77, 22, s(Blocks.SPRUCE_PLANKS));
		// the ladder up the cliff face (on the rock at z 19) to the plateau
		for (int y = 77; y <= VILLAGE; y++) {
			c.set(12, y, 19, rock(12, y, 19));
			c.set(12, y, 20, s(Blocks.LADDER).setValue(LadderBlock.FACING, Direction.SOUTH));
		}
		c.set(12, 76, 20, s(Blocks.DARK_OAK_PLANKS));
	}

	// ==================================================================================================== trees

	/** Scattered stylised winter trees: short dark trunks, chunky canopies mostly covered in snow. */
	private void trees(Canvas c) {
		int[][] trees = {{-30, 36, 0}, {28, 36, 1}, {-44, 26, 2}, {40, 24, 0}, {-20, 24, 1}, {48, -26, 2}, {-14, -42, 0}, {16, -38, 1},
				{56, -6, 2}, {-62, 12, 0}, {-40, 8, 1}, {34, 28, 2}, {-6, 36, 0}, {58, 22, 1}, {8, -40, 2}, {-52, -44, 0}};
		for (int[] t : trees) {
			tree(c, t[0], t[1], t[2]);
		}
	}

	private void tree(Canvas c, int x, int z, int kind) {
		if (!c.near(x, z, 5)) {
			return;
		}
		int g = c.ground(x, z);
		if (g <= SEA + 1) {
			return;
		}
		int trunk = 2 + kind;
		Block wood = kind == 1 ? Blocks.DARK_OAK_LOG : Blocks.SPRUCE_LOG;
		c.fill(x, g + 1, z, x, g + trunk, z, log(wood, Direction.Axis.Y));
		int r = kind == 2 ? 3 : 2;
		BlockState leaf = Kit.leaves(Blocks.SPRUCE_LEAVES);
		for (int dx = -r; dx <= r; dx++) {
			for (int dz = -r; dz <= r; dz++) {
				int top = -1;
				for (int layerY = 0; layerY < 3; layerY++) {
					int rr = layerY == 2 ? 1 : r - layerY / 2;
					if (Math.abs(dx) <= rr && Math.abs(dz) <= rr && Math.abs(dx) + Math.abs(dz) <= rr + 1) {
						c.set(x + dx, g + trunk + 1 + layerY, z + dz, leaf);
						top = layerY;
					}
				}
				// most of the canopy's top under snow (a snow block replacing the top leaf, a layer on it); some left green
				if (top >= 0 && unit(x + dx, z + dz, 71) < 0.78) {
					int y = g + trunk + 1 + top;
					c.set(x + dx, y, z + dz, s(Blocks.SNOW_BLOCK));
					c.set(x + dx, y + 1, z + dz, layer(unit(x + dx, z + dz, 72) < 0.5 ? 1 : 2));
				}
			}
		}
	}

	// ==================================================================================================== cavern

	static boolean inCave(int x, int z) {
		double a = Math.atan2(z - CAVE_Z, x - CAVE_X);
		double w = 1 + 0.12 * Math.sin(a * 3 + 1) + 0.08 * Math.sin(a * 5 + 2.3);
		return Math.hypot((x - CAVE_X) / 17.0, (z - CAVE_Z) / 14.0) < w;
	}

	static double caveDepth(int x, int z) {
		double a = Math.atan2(z - CAVE_Z, x - CAVE_X);
		double w = 1 + 0.12 * Math.sin(a * 3 + 1) + 0.08 * Math.sin(a * 5 + 2.3);
		return Math.hypot((x - CAVE_X) / 17.0, (z - CAVE_Z) / 14.0) / w;
	}

	/**
	 * The ice cavern (about 34 x 28, floor y 57, ceiling 9-13 above): dark stone shell, a snowy floor with grey-white
	 * patches and hollows, hanging icicles, a frozen pool with a dark rim, snowy ledges with a lit niche, a campsite, and
	 * a tunnel up to the lower coast with timber-framed doorways.
	 */
	private void cavern(Canvas c) {
		if (!c.near(CAVE_X, CAVE_Z + 10, 32)) {
			return;
		}
		for (int x = CAVE_X - 22; x <= CAVE_X + 22; x++) {
			for (int z = CAVE_Z - 19; z <= CAVE_Z + 19; z++) {
				if (!c.inChunk(x, z)) {
					continue;
				}
				double d = caveDepth(x, z);
				if (d > 1.25) {
					continue;
				}
				int ceil = Math.min(CAVE_FLOOR + 9 + (int) Math.round(4 * unit(x / 4, z / 4, 81)), c.ground(x, z) - 3);
				if (d >= 1) {
					for (int y = CAVE_FLOOR - 2; y <= ceil + 2; y++) {
						c.set(x, y, z, rock(x, y, z));
					}
					continue;
				}
				for (int y = CAVE_FLOOR - 4; y < CAVE_FLOOR; y++) {
					c.set(x, y, z, rock(x, y, z));
				}
				double u = unit(x / 2, z / 2, 82);
				c.set(x, CAVE_FLOOR, z, u < 0.7 ? s(Blocks.SNOW_BLOCK) : u < 0.88 ? s(Blocks.CALCITE) : s(Blocks.POLISHED_DIORITE));
				c.fill(x, CAVE_FLOOR + 1, z, x, ceil, z, s(Blocks.AIR));
				c.fill(x, ceil + 1, z, x, ceil + 2, z, rock(x, ceil + 1, z));
				if (unit(x / 3, z / 3, 83) < 0.15) {                                   // shallow hollows
					c.set(x, CAVE_FLOOR, z, s(Blocks.SNOW_BLOCK));
					c.set(x, CAVE_FLOOR + 1, z, layer(1));
				}
			}
		}
		// raised snowy ledges along the north and west walls, and a warm-lit niche
		for (int x = CAVE_X - 22; x <= CAVE_X + 22; x++) {
			for (int z = CAVE_Z - 19; z <= CAVE_Z + 19; z++) {
				if (!c.inChunk(x, z) || !inCave(x, z) || caveDepth(x, z) < 0.78) {
					continue;
				}
				int h = (z < CAVE_Z - 6 && x < CAVE_X + 4) ? 3 : (x < CAVE_X - 9 && z > CAVE_Z - 2 && z < CAVE_Z + 8) ? 2 : 0;
				if (h > 0) {
					c.fill(x, CAVE_FLOOR, z, x, CAVE_FLOOR + h - 1, z, rock(x, CAVE_FLOOR, z));
					c.set(x, CAVE_FLOOR + h, z, s(Blocks.SNOW_BLOCK));
				}
			}
		}
		c.fill(-3, CAVE_FLOOR + 4, -8, -1, CAVE_FLOOR + 5, -5, s(Blocks.AIR));
		c.fill(-3, CAVE_FLOOR + 3, -8, -1, CAVE_FLOOR + 3, -5, s(Blocks.COBBLED_DEEPSLATE));
		c.set(-2, CAVE_FLOOR + 4, -7, lantern(false));
		c.set(-1, CAVE_FLOOR + 4, -7, Canvas.facing(s(Blocks.BARREL), Direction.UP));
		// the frozen pool (about 7 x 6) with a dark rim, and smaller ice patches
		for (int x = 8; x <= 18; x++) {
			for (int z = 11; z <= 20; z++) {
				double d = Math.hypot((x - 13) / 3.5, (z - 15) / 3.0);
				if (!c.inChunk(x, z) || !inCave(x, z)) {
					continue;
				}
				if (d < 1) {
					c.set(x, CAVE_FLOOR, z, unit(x, z, 84) < 0.5 ? s(Blocks.BLUE_ICE) : s(Blocks.PACKED_ICE));
					c.set(x, CAVE_FLOOR + 1, z, s(Blocks.AIR));
				} else if (d < 1.35) {
					c.set(x, CAVE_FLOOR, z, s(Blocks.POLISHED_DEEPSLATE));
					c.set(x, CAVE_FLOOR + 1, z, s(Blocks.AIR));
				}
			}
		}
		for (int[] p : new int[][]{{-6, 12}, {0, 0}, {10, 2}}) {
			for (int dx = -1; dx <= 1; dx++) {
				for (int dz = -1; dz <= 0; dz++) {
					c.set(p[0] + dx, CAVE_FLOOR, p[1] + dz, s(Blocks.PACKED_ICE));
					c.set(p[0] + dx, CAVE_FLOOR + 1, p[1] + dz, s(Blocks.AIR));
				}
			}
		}
		// hanging icicles
		int[][] icicles = {{-4, 4, 6}, {2, 14, 5}, {10, 0, 7}, {16, 10, 4}, {-8, 12, 3}, {6, 6, 8}, {0, -2, 4}, {12, 18, 5}};
		for (int[] ic : icicles) {
			icicle(c, ic[0], ic[1], ic[2]);
		}
		for (int[] l : new int[][]{{-2, 10}, {8, 2}, {4, 16}, {-8, 4}, {12, -2}}) {
			int x = l[0], z = l[1];
			if (!c.inChunk(x, z) || !inCave(x, z)) {
				continue;
			}
			int ceil = Math.min(CAVE_FLOOR + 9 + (int) Math.round(4 * unit(x / 4, z / 4, 81)), c.ground(x, z) - 3);
			c.fill(x, ceil - 2, z, x, ceil, z, s(Blocks.CHAIN));
			c.set(x, ceil - 3, z, lantern(true));
		}
		campsite(c);
		tunnel(c);
	}

	private void icicle(Canvas c, int x, int z, int len) {
		if (!c.near(x, z, 2) || !inCave(x, z)) {
			return;
		}
		int ceil = Math.min(CAVE_FLOOR + 9 + (int) Math.round(4 * unit(x / 4, z / 4, 81)), c.ground(x, z) - 3);
		for (int k = 0; k < len; k++) {
			int y = ceil - k;
			if (y <= CAVE_FLOOR + 2) {
				break;
			}
			boolean wide = k < len / 2;
			c.set(x, y, z, k < len - 2 ? s(Blocks.BLUE_ICE) : s(Blocks.PACKED_ICE));
			if (wide) {
				c.set(x + 1, y, z, s(Blocks.PACKED_ICE));
				c.set(x, y, z + 1, s(Blocks.PACKED_ICE));
				if (k < len / 3) {
					c.set(x + 1, y, z + 1, s(Blocks.BLUE_ICE));
				}
			}
		}
	}

	/**
	 * Where the Ability Master stands (x, y, z from the island centre): on the cavern floor a few steps east of the
	 * campsite's firepit (16, 9), beside the barrels, facing the fire (yaw 90 = west).
	 */
	public static final int[] ABILITY_MASTER_SPOT = {19, CAVE_FLOOR + 1, 9};
	public static final float ABILITY_MASTER_YAW = 90;

	/** A small campsite: a green ridge tent (5 x 4), a stone firepit, two bedrolls, barrels and a crate. */
	private void campsite(Canvas c) {
		int y = CAVE_FLOOR + 1;
		for (int z = 1; z <= 5; z++) {
			c.set(15, y, z, s(Blocks.GREEN_WOOL));
			c.set(18, y, z, s(Blocks.GREEN_WOOL));
			c.set(16, y + 1, z, s(Blocks.GREEN_WOOL));
			c.set(17, y + 1, z, s(Blocks.GREEN_WOOL));
			c.set(16, y + 2, z, Kit.slab(Blocks.DARK_OAK_SLAB, false));
			c.set(17, y + 2, z, Kit.slab(Blocks.DARK_OAK_SLAB, false));
		}
		for (int z : new int[]{0, 6}) {
			c.fill(15, y, z, 15, y + 1, z, s(Blocks.DARK_OAK_FENCE));
			c.fill(18, y, z, 18, y + 1, z, s(Blocks.DARK_OAK_FENCE));
		}
		c.set(16, y, 3, s(Blocks.GREEN_CARPET));
		c.set(17, y, 3, s(Blocks.GREEN_CARPET));
		// firepit
		int fx = 16, fz = 9;
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				c.set(fx + dx, CAVE_FLOOR, fz + dz, s(Blocks.COBBLED_DEEPSLATE));
				c.set(fx + dx, y, fz + dz, dx == 0 && dz == 0 ? s(Blocks.CAMPFIRE) : Math.abs(dx) + Math.abs(dz) == 1
						? Kit.slab(Blocks.COBBLED_DEEPSLATE_SLAB, false) : log(Blocks.DARK_OAK_LOG, dx == dz ? Direction.Axis.X : Direction.Axis.Z));
			}
		}
		// bedrolls
		for (int[] b : new int[][]{{12, 2}, {12, 5}}) {
			c.set(b[0], y, b[1], s(Blocks.GREEN_CARPET));
			c.set(b[0], y, b[1] + 1, s(Blocks.GREEN_CARPET));
			c.set(b[0] - 1, y, b[1], s(Blocks.WHITE_CARPET));
		}
		// storage
		c.set(19, y, 7, Canvas.facing(s(Blocks.BARREL), Direction.UP));
		c.set(19, y, 8, Canvas.facing(s(Blocks.BARREL), Direction.UP));
		c.set(19, y + 1, 7, Canvas.facing(s(Blocks.BARREL), Direction.UP));
		c.set(20, y, 8, s(Blocks.SPRUCE_PLANKS));
		c.set(18, y, 7, lantern(false));
	}

	/** Tunnel waypoints: x, z and standing height; straight segments between them. */
	static final int[][] TUNNEL = {{-19, 38, 67}, {-19, 29, 58}, {-19, 25, 58}, {-7, 25, 58}, {-7, 15, 58}};

	/**
	 * The tunnel from the lower coast (mouth at x -19, z 38, west of the stalls) down to the cavern (floor 57): three wide,
	 * stairs where it descends, dark stone walls, wall torches, a timber-framed mouth and a framed doorway into the cavern.
	 */
	private void tunnel(Canvas c) {
		if (!c.near(-13, 27, 20)) {
			return;
		}
		java.util.List<int[]> path = new java.util.ArrayList<>();
		for (int i = 0; i + 1 < TUNNEL.length; i++) {
			int[] p = TUNNEL[i], q = TUNNEL[i + 1];
			int n = Math.max(Math.abs(q[0] - p[0]), Math.abs(q[1] - p[1]));
			for (int k = 0; k < n; k++) {
				path.add(new int[]{p[0] + (q[0] - p[0]) * k / n, p[1] + (q[1] - p[1]) * k / n, p[2] + (q[2] - p[2]) * k / n,
						q[2] != p[2] ? 1 : 0});
			}
		}
		path.add(new int[]{TUNNEL[TUNNEL.length - 1][0], TUNNEL[TUNNEL.length - 1][1], 58, 0});
		// walls first, then the passage, so later segments never wall off earlier ones
		for (int[] p : path) {
			for (int dx = -2; dx <= 2; dx++) {
				for (int dz = -2; dz <= 2; dz++) {
					int x = p[0] + dx, z = p[1] + dz;
					if (inCave(x, z) && caveDepth(x, z) < 0.95) {
						continue;
					}
					int g = c.ground(x, z);
					for (int y = p[2] - 2; y <= Math.min(p[2] + 3, g - 1); y++) {
						c.set(x, y, z, rock(x, y, z));
					}
				}
			}
		}
		for (int i = 0; i < path.size(); i++) {
			int[] p = path.get(i);
			for (int dx = -1; dx <= 1; dx++) {
				for (int dz = -1; dz <= 1; dz++) {
					int x = p[0] + dx, z = p[1] + dz;
					c.set(x, p[2] - 1, z, p[3] == 1 ? stair(Blocks.COBBLED_DEEPSLATE_STAIRS, Direction.SOUTH, false) : s(Blocks.COBBLED_DEEPSLATE));
					c.fill(x, p[2], z, x, p[2] + 2, z, s(Blocks.AIR));
				}
			}
			if (i % 5 == 2 && p[0] == -19) {
				c.set(-20, p[2] + 1, p[1], s(Blocks.WALL_TORCH).setValue(net.minecraft.world.level.block.WallTorchBlock.FACING, Direction.EAST));
			} else if (i % 5 == 2) {
				c.set(p[0], p[2] + 1, p[1] - 1, s(Blocks.WALL_TORCH).setValue(net.minecraft.world.level.block.WallTorchBlock.FACING, Direction.SOUTH));
			}
		}
		// the mouth on the lower coast: open above, framed in dark timber
		c.fill(-21, 67, 38, -21, 69, 38, log(Blocks.DARK_OAK_LOG, Direction.Axis.Y));
		c.fill(-17, 67, 38, -17, 69, 38, log(Blocks.DARK_OAK_LOG, Direction.Axis.Y));
		c.fill(-21, 70, 38, -17, 70, 38, log(Blocks.DARK_OAK_LOG, Direction.Axis.X));
		for (int x = -20; x <= -18; x++) {
			c.fill(x, 67, 39, x, 69, 40, s(Blocks.AIR));
			c.set(x, 66, 39, s(Blocks.COBBLED_DEEPSLATE));
		}
		// reinforced doorway into the cavern
		c.fill(-9, 58, 17, -9, 61, 17, log(Blocks.DARK_OAK_LOG, Direction.Axis.Y));
		c.fill(-5, 58, 17, -5, 61, 17, log(Blocks.DARK_OAK_LOG, Direction.Axis.Y));
		c.fill(-8, 61, 17, -6, 61, 17, log(Blocks.STRIPPED_DARK_OAK_LOG, Direction.Axis.X));
	}
}
