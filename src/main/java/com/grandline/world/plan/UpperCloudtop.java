package com.grandline.world.plan;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChainBlock;
import net.minecraft.world.level.block.WallBannerBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Upper Cloudtop Isles (the Lv 450-625 region of {@link CloudtopIsles}), built to the owner's specification and the
 * approved blueprint (docs/UPPER_CLOUDTOP_BLUEPRINT.md): two enormous floating landmasses on cloud banks, joined by a
 * giant tree root.
 * <ul>
 * <li>The Ancient Tree landmass (145 x 125, ground y 200): the giant tree (85 tall), its roots, the colourful settlement
 * on a raised terrace, the combat field under an arching root, the classical pavilion, two curved arcades.</li>
 * <li>The root bridge (about 58 long, 9 wide, y 200 to 204).</li>
 * <li>The Grand Sanctuary (155 x 135, ground y 204): the stepped temple (five tiers, blue geometric inlays) with its
 * monumental staircase and summit pavilion, the ceremonial courtyard with striped columns, the golden bell, the
 * monumental archways, the tall white buildings, ruins, the combat field.</li>
 * </ul>
 * Island-local coordinates of the Cloudtop Isles plan. Every method writes through the chunk-clipped canvas.
 */
final class UpperCloudtop {
	private UpperCloudtop() {
	}

	// Ancient Tree landmass.
	static final int T_X = -95, T_Z = -165, T_RX = 72, T_RZ = 62, T_Y = 200;
	static final int TRUNK_X = -110, TRUNK_Z = -170, TRUNK_BASE = 203;
	static final int SET_X1 = -158, SET_X2 = -118, SET_Z1 = -166, SET_Z2 = -126, SET_Y = 202;
	// Grand Sanctuary.
	static final int S_X = 95, S_Z = -165, S_RX = 77, S_RZ = 67, S_Y = 204;
	/** Temple base and tier inset; the staircase's x span and first/last step. */
	static final int TM_X1 = 78, TM_X2 = 142, TM_Z1 = -217, TM_Z2 = -163, TIER_H = 7, TIER_IN = 4, TIERS = 5;
	static final int ST_X1 = 103, ST_X2 = 117, ST_Z0 = -143, ST_STEPS = 35;
	static final int TOP_Y = S_Y + TIERS * TIER_H; // 239, the upper platform
	static final int PV_X1 = 98, PV_X2 = 122, PV_Z1 = -198, PV_Z2 = -182;
	static final int CY_X1 = 85, CY_X2 = 135, CY_Z1 = -152, CY_Z2 = -108;
	static final int BELL_X = 88, BELL_Z = -125;
	/** The red arcade on the Ancient Tree landmass; the return arch is its middle bay. */
	static final int RED_X1 = -150, RED_Z1 = -206;

	static BlockState s(Block b) {
		return b.defaultBlockState();
	}

	static double unit(int x, int z, int salt) {
		return CloudtopIslesV7.unit(x, z, salt);
	}

	static double unit3(int x, int y, int z, int salt) {
		return CloudtopIslesV7.unit(x * 7 + y, z * 3 - y, salt);
	}

	// ==================================================================================================== terrain

	/** 0 in the middle, 1 at the irregular edge: a squarish outline with rounded corners, lobes and inlets. */
	static double shape(double x, double z, int cx, int cz, int rx, int rz, int seed) {
		double dx = (x - cx) / rx, dz = (z - cz) / rz;
		double ang = Math.atan2(dz, dx);
		double base = Math.pow(Math.pow(Math.abs(dx), 2.5) + Math.pow(Math.abs(dz), 2.5), 0.4);
		double wob = 1 + 0.06 * Math.sin(3 * ang + seed) + 0.05 * Math.sin(5 * ang + 2.1 * seed) + 0.035 * Math.sin(8 * ang + 0.7 * seed);
		return base / wob;
	}

	/** Ancient Tree ground: 200, the settlement terrace 202 (stepping down at its edges), the tree's mound up to 203. */
	static int treeGround(int x, int z) {
		int dxo = Math.max(0, Math.max(SET_X1 - x, x - SET_X2)), dzo = Math.max(0, Math.max(SET_Z1 - z, z - SET_Z2));
		double d = Math.hypot(dxo, dzo);
		int settle = d <= 0 ? 2 : d <= 2.5 ? 1 : 0;
		double r = Math.hypot(x - TRUNK_X, z - TRUNK_Z);
		int mound = r < 7 ? 3 : r < 11 ? 2 : r < 16 ? 1 : 0;
		return T_Y + Math.max(settle, mound);
	}

	static int sanctuaryGround(int x, int z) {
		return S_Y;
	}

	static BlockState rock(int x, int y, int z) {
		double u = unit3(x, y, z, 5);
		return u < 0.4 ? s(Blocks.STONE) : u < 0.7 ? s(Blocks.ANDESITE) : u < 0.9 ? s(Blocks.TUFF) : s(Blocks.CALCITE);
	}

	/** Cloud material: snow on top, light grey wool in the recesses below, wool, a little calcite and smooth quartz. */
	static BlockState cloud(int x, int y, int z, double dy, boolean shell) {
		if (!shell) {
			return s(Blocks.WHITE_CONCRETE);
		}
		double u = unit3(x, y, z, 77);
		if (dy > 0.3) {
			return u < 0.75 ? s(Blocks.SNOW_BLOCK) : s(Blocks.WHITE_WOOL);
		}
		if (dy < -0.25 && dy > -0.75 && u < 0.22) {
			return s(Blocks.LIGHT_GRAY_WOOL);
		}
		return u < 0.08 ? s(Blocks.CALCITE) : u < 0.13 ? s(Blocks.SMOOTH_QUARTZ) : u < 0.6 ? s(Blocks.WHITE_WOOL) : s(Blocks.SNOW_BLOCK);
	}

	/**
	 * A landmass: grass on dirt on stone (12-20 thick, rock showing round the edge, rocky stretches of perimeter), a deep
	 * rounded cloud underneath (15-25), and a ring of big overlapping cloud banks round the rim hiding the rock below the
	 * grass. Nothing hangs down in spikes.
	 */
	static void land(Canvas c, int cx, int cz, int rx, int rz, CloudtopIslesV7.Height h, int seed) {
		if (!c.near(cx, cz, Math.max(rx, rz) + 16)) {
			return;
		}
		for (int x = cx - rx - 6; x <= cx + rx + 6; x++) {
			for (int z = cz - rz - 6; z <= cz + rz + 6; z++) {
				if (!c.inChunk(x, z)) {
					continue;
				}
				double e = shape(x, z, cx, cz, rx, rz, seed);
				if (e > 1.0) {
					continue;
				}
				int top = h.at(x, z);
				double ang = Math.atan2(z - cz, x - cx);
				boolean rocky = e > 0.9 && Math.sin(3 * ang + seed * 1.7) > 0.55;
				double core = Math.sqrt(Math.max(0, 1 - e * e));
				int t = 12 + (int) Math.round(8 * core);
				c.set(x, top, z, rocky ? rock(x, top, z) : s(Blocks.GRASS_BLOCK));
				for (int k = 1; k <= t; k++) {
					c.set(x, top - k, z, k <= 3 && !rocky && e < 0.93 ? s(Blocks.DIRT) : e > 0.85 || k <= 4 ? rock(x, top - k, z) : s(Blocks.STONE));
				}
				double lump = unit(x / 4, z / 4, seed) * 0.6 + unit(x, z, seed + 1) * 0.4;
				int d = 4 + (int) Math.round(21 * Math.pow(core, 0.7) * (0.8 + 0.2 * lump));
				for (int k = 1; k <= d; k++) {
					int y = top - t - k;
					boolean bottom = k > d - 4;
					BlockState b = bottom ? (unit3(x, y, z, 81) < 0.5 ? s(Blocks.WHITE_WOOL) : unit3(x, y, z, 82) < 0.5 ? s(Blocks.SNOW_BLOCK) : s(Blocks.WHITE_CONCRETE))
							: cloud(x, y, z, -0.5, e > 0.82);
					c.set(x, y, z, b);
				}
			}
		}
		// Cloud banks round the rim: big overlapping puffs, their tops a couple of blocks under the grass.
		int n = (int) Math.round(Math.PI * (rx + rz) / 7.0);
		for (int i = 0; i < n; i++) {
			double a = (i + unit(i, seed, 3) * 0.5) * 2 * Math.PI / n;
			double ca = Math.cos(a), sa = Math.sin(a);
			double r = 0.5;
			while (r < 1.6 && shape(cx + ca * rx * r, cz + sa * rz * r, cx, cz, rx, rz, seed) < 1.02) {
				r += 0.01;
			}
			int px = (int) Math.round(cx + ca * rx * r), pz = (int) Math.round(cz + sa * rz * r);
			int top = h.at(px, pz);
			double u = unit(i, seed, 9);
			double prx = 7 + u * 4, pry = 4 + u * 2.5, prz = 6.5 + unit(seed, i, 10) * 4;
			bank(c, px, top - 5 - (int) (u * 2), pz, prx, pry, prz, top - 1);
			bank(c, (int) Math.round(px + ca * 3), top - 14, (int) Math.round(pz + sa * 3), prx * 0.9, pry * 1.1, prz * 0.9, top - 4);
		}
	}

	/** One rounded cloud volume (every block joins its middle, so nothing floats alone). */
	static void bank(Canvas c, int cx, int cy, int cz, double rx, double ry, double rz, int maxY) {
		if (!c.near(cx, cz, (int) Math.ceil(Math.max(rx, rz)) + 2)) {
			return;
		}
		for (int x = (int) (cx - rx - 1); x <= cx + rx + 1; x++) {
			for (int z = (int) (cz - rz - 1); z <= cz + rz + 1; z++) {
				if (!c.inChunk(x, z)) {
					continue;
				}
				for (int y = (int) (cy - ry - 1); y <= Math.min(maxY, cy + ry + 1); y++) {
					if (CloudtopIsles.puffInside(x, y, z, cx, cy, cz, rx, ry, rz) && CloudtopIsles.puffConnected(x, y, z, cx, cy, cz, rx, ry, rz)) {
						double dx = (x - cx) / rx, dy = (y - cy) / ry, dz = (z - cz) / rz;
						boolean shell = dx * dx + dy * dy + dz * dz > 0.48;
						c.set(x, y, z, cloud(x, y, z, dy, shell));
					}
				}
			}
		}
	}

	/** Ground plants over grass in a box (grass, ferns, dandelions, poppies), reading the world so nothing sits on stone. */
	static void plants(Canvas c, int x1, int z1, int x2, int z2, CloudtopIslesV7.Height h, int salt) {
		for (int x = x1; x <= x2; x++) {
			for (int z = z1; z <= z2; z++) {
				if (!c.inChunk(x, z)) {
					continue;
				}
				int y = h.at(x, z);
				if (c.level().getBlockState(c.world(x, y, z)).getBlock() != Blocks.GRASS_BLOCK || !c.level().getBlockState(c.world(x, y + 1, z)).isAir()) {
					continue;
				}
				double r = unit(x, z, salt);
				if (r < 0.025) {
					c.set(x, y + 1, z, s(r < 0.012 ? Blocks.DANDELION : Blocks.POPPY));
				} else if (r > 0.9) {
					c.set(x, y + 1, z, s(r > 0.97 ? Blocks.FERN : Blocks.GRASS));
				}
			}
		}
	}

	/** A worn path (dirt path, coarse dirt, packed mud) along a polyline, at the ground height. */
	static void dirtPath(Canvas c, CloudtopIslesV7.Height h, double width, int... pts) {
		line(c, width, pts, (x, z) -> {
			int y = h.at(x, z);
			double u = unit(x, z, 29);
			c.set(x, y, z, u < 0.6 ? s(Blocks.DIRT_PATH) : u < 0.8 ? s(Blocks.COARSE_DIRT) : s(Blocks.PACKED_MUD));
			c.fill(x, y + 1, z, x, y + 4, z, s(Blocks.AIR));
		});
	}

	/** A pale paved way (smooth quartz with quartz brick flecks) along a polyline, at ground y. */
	static void quartzPath(Canvas c, int y, double width, int... pts) {
		line(c, width, pts, (x, z) -> {
			c.set(x, y, z, unit(x, z, 31) < 0.75 ? s(Blocks.SMOOTH_QUARTZ) : s(Blocks.QUARTZ_BRICKS));
			c.fill(x, y + 1, z, x, y + 4, z, s(Blocks.AIR));
		});
	}

	interface Column {
		void at(int x, int z);
	}

	static void line(Canvas c, double width, int[] pts, Column col) {
		for (int i = 0; i + 3 < pts.length; i += 2) {
			int x1 = pts[i], z1 = pts[i + 1], x2 = pts[i + 2], z2 = pts[i + 3];
			int n = Math.max(1, Math.max(Math.abs(x2 - x1), Math.abs(z2 - z1)));
			if (!c.near((x1 + x2) / 2, (z1 + z2) / 2, n / 2 + (int) width + 3)) {
				continue;
			}
			java.util.Set<Long> done = new java.util.HashSet<>();
			for (int k = 0; k <= n * 2; k++) {
				double px = x1 + (x2 - x1) * (k / (2.0 * n)), pz = z1 + (z2 - z1) * (k / (2.0 * n));
				for (int x = (int) Math.floor(px - width); x <= Math.ceil(px + width); x++) {
					for (int z = (int) Math.floor(pz - width); z <= Math.ceil(pz + width); z++) {
						if (Math.hypot(x - px, z - pz) <= width / 2 + 0.3 && c.inChunk(x, z) && done.add(((long) x << 32) ^ (z & 0xffffffffL))) {
							col.at(x, z);
						}
					}
				}
			}
		}
	}

	// ==================================================================================================== wood

	/** Dark bark: dark oak wood, vertical dark oak log streaks, brown terracotta patches. */
	static BlockState bark(int x, int y, int z) {
		double u = unit3(x / 2, y / 3, z / 2, 41);
		double v = unit3(x, y, z, 42);
		return u < 0.12 ? s(Blocks.BROWN_TERRACOTTA) : v < 0.3 ? s(Blocks.DARK_OAK_LOG) : s(Blocks.DARK_OAK_WOOD);
	}

	/** A tube from a to b with radius r0..r1, of bark with a stripped core. */
	static void tube(Canvas c, double ax, double ay, double az, double bx, double by, double bz, double r0, double r1) {
		double len = Math.sqrt((bx - ax) * (bx - ax) + (by - ay) * (by - ay) + (bz - az) * (bz - az));
		int steps = (int) Math.ceil(len * 2) + 1;
		int rMax = (int) Math.ceil(Math.max(r0, r1)) + 1;
		if (!c.near((int) ((ax + bx) / 2), (int) ((az + bz) / 2), (int) (len / 2) + rMax + 1)) {
			return;
		}
		for (int i = 0; i <= steps; i++) {
			double t = i / (double) steps;
			double px = ax + (bx - ax) * t, py = ay + (by - ay) * t, pz = az + (bz - az) * t, r = r0 + (r1 - r0) * t;
			for (int x = (int) Math.floor(px - r); x <= Math.ceil(px + r); x++) {
				for (int z = (int) Math.floor(pz - r); z <= Math.ceil(pz + r); z++) {
					if (!c.inChunk(x, z)) {
						continue;
					}
					for (int y = (int) Math.floor(py - r); y <= Math.ceil(py + r); y++) {
						double d = Math.sqrt((x - px) * (x - px) + (y - py) * (y - py) + (z - pz) * (z - pz));
						if (d <= r) {
							c.set(x, y, z, d > r - 1.3 ? bark(x, y, z) : s(Blocks.STRIPPED_DARK_OAK_WOOD));
						}
					}
				}
			}
		}
	}

	/**
	 * A broad, flattened, faceted foliage mass: stacked octagonal layers in steps of two, green concrete inside with green
	 * terracotta, lime terracotta catching the light on top, and a rim of oak and dark oak leaves softening the edges.
	 */
	static void foliage(Canvas c, int cx, int cy, int cz, double r, int ry) {
		if (!c.near(cx, cz, (int) r + 2)) {
			return;
		}
		for (int dy = -ry; dy <= ry; dy++) {
			double rk = r * Math.sqrt(Math.max(0, 1 - Math.pow(dy / (ry + 0.6), 2)));
			rk = Math.max(1, Math.round(rk / 2.0) * 2.0);
			for (int dx = (int) -rk - 1; dx <= rk + 1; dx++) {
				for (int dz = (int) -rk - 1; dz <= rk + 1; dz++) {
					double d = Math.max(Math.max(Math.abs(dx), Math.abs(dz)), (Math.abs(dx) + Math.abs(dz)) / 1.35);
					if (d > rk) {
						continue;
					}
					int x = cx + dx, y = cy + dy, z = cz + dz;
					double u = unit3(x, y, z, 51);
					BlockState b;
					if (d > rk - 1.3) {
						b = Kit.leaves(u < 0.5 ? Blocks.OAK_LEAVES : Blocks.DARK_OAK_LEAVES);
					} else if (dy == ry) {
						b = u < 0.45 ? s(Blocks.LIME_TERRACOTTA) : s(Blocks.GREEN_CONCRETE);
					} else if (dy == -ry) {
						b = s(Blocks.GREEN_TERRACOTTA);
					} else {
						b = u < 0.22 ? s(Blocks.GREEN_TERRACOTTA) : s(Blocks.GREEN_CONCRETE);
					}
					c.set(x, y, z, b);
				}
			}
		}
	}

	/** Branches of the giant tree: compass angle (deg), start y, reach, rise, base radius. */
	static final double[][] BRANCHES = {{15, 236, 21, 10, 2.8}, {80, 241, 19, 12, 2.6}, {145, 235, 22, 9, 3.0}, {205, 243, 19, 12, 2.6},
			{265, 238, 21, 11, 2.8}, {325, 247, 16, 13, 2.3}, {110, 252, 13, 14, 2.0}, {240, 253, 13, 14, 2.0}};

	/**
	 * The giant ancient tree: an irregular dark trunk 15 across flaring into the roots, angular branches, broad faceted
	 * canopy masses at the branch ends and a crown on top (y 289).
	 */
	static void giantTree(Canvas c) {
		if (!c.near(TRUNK_X, TRUNK_Z, 48)) {
			return;
		}
		for (int y = TRUNK_BASE - 2; y <= 270; y++) {
			double base = y <= 208 ? 7.5 + (208 - y) * 0.8 : y <= 245 ? 7.5 - (y - 208) * 0.02 : Math.max(2.6, 6.8 - (y - 245) * 0.17);
			for (int dx = -13; dx <= 13; dx++) {
				for (int dz = -13; dz <= 13; dz++) {
					int x = TRUNK_X + dx, z = TRUNK_Z + dz;
					if (!c.inChunk(x, z)) {
						continue;
					}
					double ang = Math.atan2(dz, dx);
					double rr = base * (1 + 0.11 * Math.sin(5 * ang + y * 0.09) + 0.06 * Math.sin(3 * ang + 1.3));
					double d = Math.hypot(dx, dz);
					if (d <= rr) {
						c.set(x, y, z, d > rr - 1.6 ? bark(x, y, z) : s(Blocks.STRIPPED_DARK_OAK_WOOD));
					}
				}
			}
		}
		for (double[] b : BRANCHES) {
			double a = Math.toRadians(b[0]);
			double ux = Math.cos(a), uz = Math.sin(a);
			double y0 = b[1], reach = b[2], rise = b[3], r = b[4];
			double kink = (b[0] % 2 == 0 ? 1 : -1) * 2.5;
			double x0 = TRUNK_X + ux * 4, z0 = TRUNK_Z + uz * 4;
			double x1 = TRUNK_X + ux * reach * 0.5, z1 = TRUNK_Z + uz * reach * 0.5, y1 = y0 + rise * 0.65;
			double x2 = TRUNK_X + ux * reach - uz * kink, z2 = TRUNK_Z + uz * reach + ux * kink, y2 = y0 + rise;
			tube(c, x0, y0, z0, x1, y1, z1, r, r * 0.8);
			tube(c, x1, y1, z1, x2, y2, z2, r * 0.8, r * 0.55);
		}
		for (double[] b : BRANCHES) {
			double a = Math.toRadians(b[0]);
			double kink = (b[0] % 2 == 0 ? 1 : -1) * 2.5;
			int fx = (int) Math.round(TRUNK_X + Math.cos(a) * b[2] - Math.sin(a) * kink), fz = (int) Math.round(TRUNK_Z + Math.sin(a) * b[2] + Math.cos(a) * kink);
			foliage(c, fx, (int) (b[1] + b[3]) + 2, fz, 9 + unit(fx, fz, 5) * 3, 4);
		}
		foliage(c, TRUNK_X, 272, TRUNK_Z, 14, 5);
		foliage(c, TRUNK_X + 2, 281, TRUNK_Z - 1, 11, 4);
		foliage(c, TRUNK_X - 1, 287, TRUNK_Z + 1, 5, 2);
	}

	/**
	 * A root along a polyline of (x, centre y, z) points, half-width w0..w1 and half-height hh0..hh1: an elliptical
	 * section of bark, its worn top of stripped dark oak, with dark oak stairs where its top climbs or drops by one so it
	 * can be walked.
	 */
	static void root(Canvas c, double[][] pts, double w0, double w1, double hh0, double hh1, boolean stairs) {
		double total = 0;
		for (int i = 1; i < pts.length; i++) {
			total += Math.hypot(pts[i][0] - pts[i - 1][0], pts[i][2] - pts[i - 1][2]);
		}
		double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, minZ = Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;
		for (double[] p : pts) {
			minX = Math.min(minX, p[0]);
			maxX = Math.max(maxX, p[0]);
			minZ = Math.min(minZ, p[2]);
			maxZ = Math.max(maxZ, p[2]);
		}
		int wMax = (int) Math.ceil(Math.max(w0, w1)) + 2;
		if (!c.near((int) ((minX + maxX) / 2), (int) ((minZ + maxZ) / 2), (int) Math.max(maxX - minX, maxZ - minZ) / 2 + wMax)) {
			return;
		}
		// Per column: the highest top, so stairs can be placed after.
		java.util.Map<Long, Integer> tops = new java.util.HashMap<>();
		java.util.List<int[]> centre = new java.util.ArrayList<>();
		double run = 0;
		for (int i = 1; i < pts.length; i++) {
			double[] a = pts[i - 1], b = pts[i];
			double seg = Math.hypot(b[0] - a[0], b[2] - a[2]);
			int steps = (int) Math.ceil(seg * 3) + 1;
			for (int k = 0; k <= steps; k++) {
				double t = k / (double) steps;
				double px = a[0] + (b[0] - a[0]) * t, py = a[1] + (b[1] - a[1]) * t, pz = a[2] + (b[2] - a[2]) * t;
				double f = (run + seg * t) / Math.max(1, total);
				double w = w0 + (w1 - w0) * f, hh = hh0 + (hh1 - hh0) * f;
				w *= 1 + 0.12 * Math.sin(px * 0.4 + pz * 0.3);
				int cx = (int) Math.round(px), cz = (int) Math.round(pz);
				if (centre.isEmpty() || centre.get(centre.size() - 1)[0] != cx || centre.get(centre.size() - 1)[1] != cz) {
					centre.add(new int[]{cx, cz});
				}
				for (int x = (int) Math.floor(px - w); x <= Math.ceil(px + w); x++) {
					for (int z = (int) Math.floor(pz - w); z <= Math.ceil(pz + w); z++) {
						double d = Math.hypot(x - px, z - pz);
						if (d > w || !c.inChunk(x, z)) {
							continue;
						}
						// A walkable root (stairs) has a flat deck across the middle 60% of its width; only the sides round off.
						double v = !stairs ? hh * Math.sqrt(1 - (d / w) * (d / w))
								: d <= 0.6 * w ? hh : hh * Math.sqrt(Math.max(0, 1 - Math.pow((d - 0.6 * w) / (0.4 * w), 2)));
						int yTop = (int) Math.round(py + v), yBot = (int) Math.round(py - v);
						for (int y = yBot; y <= yTop; y++) {
							c.set(x, y, z, bark(x, y, z));
						}
						long key = ((long) x << 32) ^ (z & 0xffffffffL);
						tops.merge(key, yTop, Math::max);
					}
				}
			}
			run += seg;
		}
		for (var e : tops.entrySet()) {
			int x = (int) (e.getKey() >> 32), z = (int) (long) e.getKey().intValue();
			int y = e.getValue();
			c.set(x, y, z, unit(x, z, 43) < 0.8 ? s(Blocks.STRIPPED_DARK_OAK_WOOD) : s(Blocks.DARK_OAK_WOOD));
			c.fill(x, y + 1, z, x, y + 3, z, s(Blocks.AIR));
		}
		if (!stairs) {
			return;
		}
		// Stairs along the middle where the top steps by one.
		for (int i = 1; i < centre.size(); i++) {
			int[] p = centre.get(i - 1), q = centre.get(i);
			Integer tp = tops.get(((long) p[0] << 32) ^ (p[1] & 0xffffffffL)), tq = tops.get(((long) q[0] << 32) ^ (q[1] & 0xffffffffL));
			if (tp == null || tq == null || Math.abs(tq - tp) != 1) {
				continue;
			}
			Direction d = q[0] != p[0] ? (q[0] > p[0] ? Direction.EAST : Direction.WEST) : (q[1] > p[1] ? Direction.SOUTH : Direction.NORTH);
			Direction side = d.getClockWise();
			for (int k = -2; k <= 2; k++) {
				int x = q[0] + side.getStepX() * k, z = q[1] + side.getStepZ() * k;
				Integer t = tops.get(((long) x << 32) ^ (z & 0xffffffffL));
				if (t != null && t.equals(tq)) {
					c.set(x, tq, z, Kit.stairs(Blocks.DARK_OAK_STAIRS, tq > tp ? d : d.getOpposite(), false));
				}
			}
		}
	}

	/** The giant roots on the Ancient Tree landmass (the east one arches over the combat field and becomes the bridge). */
	static void roots(Canvas c) {
		root(c, new double[][]{{-103, 204, -170}, {-97, 204, -168}, {-90, 207, -167}, {-80, 211, -166}, {-62, 211, -166},
				{-52, 208, -165}, {-44, 202, -165}, {-37, 198, -165}}, 4.5, 4, 3.2, 2.2, true);
		root(c, new double[][]{{-108, 202, -176}, {-104, 200.5, -195}, {-98, 200, -218}}, 3.8, 1.6, 2.4, 1.4, false);
		root(c, new double[][]{{-115, 202, -175}, {-128, 200.5, -182}, {-145, 200, -194}}, 3.4, 1.5, 2.2, 1.3, false);
		root(c, new double[][]{{-117, 202, -171}, {-140, 200.5, -173}, {-160, 200, -177}}, 3.4, 1.5, 2.2, 1.3, false);
		root(c, new double[][]{{-111, 202, -163}, {-109, 200.5, -150}, {-113, 200, -125}}, 3.2, 1.5, 2.0, 1.3, false);
		root(c, new double[][]{{-104, 202, -176}, {-88, 200.5, -186}, {-78, 200, -203}}, 3.0, 1.4, 2.0, 1.2, false);
	}

	/** The bridge root's walking line: (x, z) points and the walk y at each (the root's flat top). */
	static final int[][] BRIDGE = {{-38, -165, 200}, {-24, -160, 201}, {-8, -157, 202}, {10, -160, 203}, {27, -165, 204}};

	/**
	 * The root bridge: a giant root 9 wide on top with irregular raised bark along its sides, a rounded body below, rising
	 * four blocks from the Ancient Tree landmass to the sanctuary, dark oak stairs where it rises. No railings.
	 */
	static void rootBridge(Canvas c) {
		if (!c.near(-5, -161, 40)) {
			return;
		}
		// Pass 1: the body and the raised bark along the sides; pass 2: the walking top over it (on the curves a side of
		// one stretch can fall inside the walk of the next).
		for (int pass = 0; pass < 2; pass++) {
			for (int i = 1; i < BRIDGE.length; i++) {
				int[] a = BRIDGE[i - 1], b = BRIDGE[i];
				double seg = Math.hypot(b[0] - a[0], b[1] - a[1]);
				int steps = (int) Math.ceil(seg * 3);
				double ux = (b[0] - a[0]) / seg, uz = (b[1] - a[1]) / seg;
				for (int k = 0; k <= steps; k++) {
					double t = k / (double) steps;
					double px = a[0] + (b[0] - a[0]) * t, pz = a[1] + (b[1] - a[1]) * t;
					int walk = (int) Math.floor(a[2] + (b[2] - a[2]) * t + 0.5);
					for (double o = -6; o <= 6; o += 0.5) {
						int x = (int) Math.round(px - uz * o), z = (int) Math.round(pz + ux * o);
						if (!c.inChunk(x, z)) {
							continue;
						}
						double ao = Math.abs(o);
						if (pass == 0) {
							int depth = 1 + (int) Math.round(8 * Math.sqrt(Math.max(0, 1 - Math.pow(ao / 6.2, 2))));
							int top = walk;
							if (ao > 4.6) {
								double u = unit(x, z, 61);
								top = walk + (u > 0.55 ? 1 : 0) + (u > 0.85 ? 1 : 0) - (ao > 5.6 ? 1 : 0);
							}
							for (int y = walk - depth; y <= top; y++) {
								c.set(x, y, z, bark(x, y, z));
							}
						} else if (ao <= 4.6) {
							c.set(x, walk, z, walkBlock(x, z));
							c.fill(x, walk + 1, z, x, walk + 4, z, s(Blocks.AIR));
						}
					}
				}
			}
		}
		// Stairs where the walk rises a block (one row across the walking width at each of the four rises).
		for (int i = 1; i < BRIDGE.length; i++) {
			int[] a = BRIDGE[i - 1], b = BRIDGE[i];
			double seg = Math.hypot(b[0] - a[0], b[1] - a[1]);
			double ux = (b[0] - a[0]) / seg, uz = (b[1] - a[1]) / seg;
			double t = 0.5;
			double px = a[0] + (b[0] - a[0]) * t, pz = a[1] + (b[1] - a[1]) * t;
			Direction along = Math.abs(ux) >= Math.abs(uz) ? (ux > 0 ? Direction.EAST : Direction.WEST) : (uz > 0 ? Direction.SOUTH : Direction.NORTH);
			for (double o = -4.5; o <= 4.5; o += 0.5) {
				int x = (int) Math.round(px - uz * o), z = (int) Math.round(pz + ux * o);
				int walkHere = (int) Math.floor(a[2] + (b[2] - a[2]) * t + 0.5);
				// the row just before the rise gets stairs up to the next level
				int sx = x - along.getStepX(), sz = z - along.getStepZ();
				if (c.inChunk(sx, sz) && walkHere > a[2]) {
					c.set(sx, walkHere, sz, Kit.stairs(Blocks.DARK_OAK_STAIRS, along, false));
					c.fill(sx, walkHere + 1, sz, sx, walkHere + 3, sz, s(Blocks.AIR));
				}
			}
		}
	}

	static BlockState walkBlock(int x, int z) {
		double u = unit(x, z, 63);
		return u < 0.7 ? s(Blocks.STRIPPED_DARK_OAK_WOOD) : u < 0.9 ? s(Blocks.DARK_OAK_WOOD) : Kit.slab(Blocks.DARK_OAK_SLAB, true);
	}

	// ==================================================================================================== buildings

	/** Settlement and sanctuary building materials. */
	record Style(Block wall, Block trim, Block pillar, Block panel, Block frame, BlockState edge, boolean openWindows) {
	}

	static final Style WHITE = new Style(Blocks.WHITE_CONCRETE, Blocks.SMOOTH_QUARTZ, Blocks.QUARTZ_PILLAR, Blocks.LIGHT_GRAY_CONCRETE,
			Blocks.POLISHED_ANDESITE, Kit.slab(Blocks.QUARTZ_SLAB, false), false);
	static final Style WHITE_OPEN = new Style(Blocks.WHITE_CONCRETE, Blocks.SMOOTH_QUARTZ, Blocks.QUARTZ_PILLAR, Blocks.LIGHT_GRAY_CONCRETE,
			Blocks.POLISHED_ANDESITE, Kit.slab(Blocks.QUARTZ_SLAB, false), true);
	static final Style YELLOW = new Style(Blocks.YELLOW_CONCRETE, Blocks.SMOOTH_SANDSTONE, Blocks.YELLOW_TERRACOTTA, Blocks.YELLOW_TERRACOTTA,
			Blocks.POLISHED_ANDESITE, Kit.slab(Blocks.SMOOTH_SANDSTONE_SLAB, false), false);
	static final Style PINK = new Style(Blocks.PINK_CONCRETE, Blocks.WHITE_CONCRETE, Blocks.WHITE_CONCRETE, Blocks.PINK_TERRACOTTA,
			Blocks.WHITE_CONCRETE, Kit.slab(Blocks.QUARTZ_SLAB, false), false);

	/** Roof finishes: a flat roof with a parapet, or a low stepped pyramid of terracotta. */
	enum Roof { FLAT, BLUE, ORANGE, ANDESITE }

	/**
	 * A narrow multistory building: corner trims, vertical supports, narrow windows two high on every floor (glass, or
	 * dark openings with a quartz sill), recessed panels, floors every four with a ladder, a door with a step and a
	 * finished roof. {@code damaged} breaks one top corner (cracked stone and rubble).
	 */
	static void building(Canvas c, int x1, int z1, int w, int d, int y, int h, Style st, Direction front, Roof roof, boolean damaged) {
		int x2 = x1 + w - 1, z2 = z1 + d - 1;
		if (!c.near((x1 + x2) / 2, (z1 + z2) / 2, Math.max(w, d) / 2 + 4)) {
			return;
		}
		c.fill(x1, y - 3, z1, x2, y, z2, s(st.trim()));
		for (int x = x1; x <= x2; x++) {
			for (int z = z1; z <= z2; z++) {
				boolean ex = x == x1 || x == x2, ez = z == z1 || z == z2;
				if (!ex && !ez) {
					c.fill(x, y + 1, z, x, y + h - 1, z, s(Blocks.AIR));
					for (int f = 4; f < h; f += 4) {
						c.set(x, y + f, z, s(Blocks.SMOOTH_QUARTZ));
					}
					c.set(x, y + h, z, s(st.trim()));
					continue;
				}
				boolean corner = ex && ez;
				int along = ex ? z - z1 : x - x1;
				int span = ex ? d : w;
				boolean pillar = !corner && along % 4 == 0 && along != span - 1;
				for (int k = 1; k <= h; k++) {
					BlockState b;
					int inFloor = (k - 1) % 4;
					if (corner) {
						b = s(st.trim());
					} else if (k == h) {
						b = s(st.trim());
					} else if (pillar) {
						b = st.pillar() == Blocks.QUARTZ_PILLAR ? s(Blocks.QUARTZ_PILLAR) : s(st.pillar());
					} else if ((inFloor == 1 || inFloor == 2) && along % 2 == 1 && k < h - 1) {
						if (st.openWindows()) {
							b = s(Blocks.AIR);
						} else {
							b = Kit.connected((along / 2) % 2 == 0 ? Blocks.BLACK_STAINED_GLASS_PANE : Blocks.GRAY_STAINED_GLASS_PANE, !ex);
						}
					} else if (inFloor == 0 && along % 2 == 1 && k > 1) {
						b = st.openWindows() ? Kit.stairs(Blocks.QUARTZ_STAIRS, outward(x, z, x1, x2, z1, z2).getOpposite(), true) : s(st.panel());
					} else if (inFloor == 3 && along % 2 == 1) {
						b = s(st.frame());
					} else {
						b = s(st.wall());
					}
					c.set(x, y + k, z, b);
				}
			}
		}
		// Ladder against the back wall, through the floors.
		Direction back = front.getOpposite();
		int lx, lz;
		if (front.getAxis() == Direction.Axis.Z) {
			lx = x1 + 2; // along the wall at an even step: the odd ones are windows, which would leave the ladder hanging
			lz = back == Direction.NORTH ? z1 + 1 : z2 - 1;
		} else {
			lx = back == Direction.WEST ? x1 + 1 : x2 - 1;
			lz = z1 + 2;
		}
		for (int k = 1; k < h; k++) {
			c.set(lx, y + k, lz, Canvas.facing(s(Blocks.LADDER), front));
		}
		for (int f = 4; f < h; f += 4) {
			int mx = (x1 + x2) / 2, mz = (z1 + z2) / 2;
			c.set(mx, y + f - 1, mz, CloudtopIsles.lantern(Blocks.LANTERN, true));
		}
		c.set((x1 + x2) / 2, y + h - 1, (z1 + z2) / 2, CloudtopIsles.lantern(Blocks.LANTERN, true));
		// Door in the middle of the front, a quartz step outside.
		int dx = front == Direction.EAST ? x2 : front == Direction.WEST ? x1 : (x1 + x2) / 2;
		int dz = front == Direction.SOUTH ? z2 : front == Direction.NORTH ? z1 : (z1 + z2) / 2;
		Kit.door(c, dx, y + 1, dz, Blocks.DARK_OAK_DOOR, front.getOpposite());
		c.set(dx, y + 3, dz, s(st.frame()));
		int ox = dx + front.getStepX(), oz = dz + front.getStepZ();
		c.set(ox, y, oz, s(Blocks.SMOOTH_QUARTZ));
		c.fill(ox, y + 1, oz, ox, y + 2, oz, s(Blocks.AIR));
		// Roof.
		switch (roof) {
			case FLAT -> {
				for (int x = x1; x <= x2; x++) {
					for (int z = z1; z <= z2; z++) {
						if (x == x1 || x == x2 || z == z1 || z == z2) {
							c.set(x, y + h + 1, z, st.edge());
						}
					}
				}
			}
			case ANDESITE -> {
				for (int x = x1 - 1; x <= x2 + 1; x++) {
					for (int z = z1 - 1; z <= z2 + 1; z++) {
						c.set(x, y + h + 1, z, Kit.slab(Blocks.POLISHED_ANDESITE_SLAB, false));
					}
				}
			}
			case BLUE, ORANGE -> {
				Block t = roof == Roof.BLUE ? Blocks.BLUE_TERRACOTTA : Blocks.ORANGE_TERRACOTTA;
				for (int i = 0; x1 - 1 + i <= x2 + 1 - i && z1 - 1 + i <= z2 + 1 - i; i++) {
					c.fill(x1 - 1 + i, y + h + 1 + i, z1 - 1 + i, x2 + 1 - i, y + h + 1 + i, z2 + 1 - i, s(t));
				}
			}
		}
		if (damaged) {
			int cx = x2, cz = z1;
			for (int k = h - 5; k <= h + 3; k++) {
				for (int a = 0; a <= 3; a++) {
					int depth = (k - (h - 5)) / 2;
					if (a <= depth) {
						c.set(cx - a, y + k, cz, s(Blocks.AIR));
						c.set(cx, y + k, cz + a, s(Blocks.AIR));
					}
				}
			}
			for (int k = h - 6; k <= h - 3; k++) {
				c.set(cx - 1, y + k, cz, s(Blocks.CRACKED_STONE_BRICKS));
				c.set(cx, y + k, cz + 1, s(Blocks.CRACKED_STONE_BRICKS));
			}
			c.set(cx + 1, y + 1, cz - 1, s(Blocks.ANDESITE));
			c.set(cx + 2, y + 1, cz, Kit.stairs(Blocks.STONE_BRICK_STAIRS, Direction.WEST, false));
		}
	}

	static Direction outward(int x, int z, int x1, int x2, int z1, int z2) {
		return x == x1 ? Direction.WEST : x == x2 ? Direction.EAST : z == z1 ? Direction.NORTH : Direction.SOUTH;
	}

	/**
	 * A round domed tower: white walls with quartz trim and a coloured band, narrow dark windows, floors with a ladder,
	 * a door, a quartz slab eave and a dome of stepped concentric rings with a pale tip.
	 */
	static void domedTower(Canvas c, int cx, int cz, int y, int r, int h, Block ring1, Block ring2, Block cap, Block band, Direction door) {
		if (!c.near(cx, cz, r + 4)) {
			return;
		}
		for (int dx = -r; dx <= r; dx++) {
			for (int dz = -r; dz <= r; dz++) {
				double d = Math.hypot(dx, dz);
				if (d > r + 0.4) {
					continue;
				}
				int x = cx + dx, z = cz + dz;
				c.fill(x, y - 3, z, x, y, z, s(Blocks.SMOOTH_QUARTZ));
				boolean shell = d > r - 0.8;
				for (int k = 1; k <= h; k++) {
					BlockState b;
					if (!shell) {
						b = k % 5 == 0 && k < h ? s(Blocks.SMOOTH_QUARTZ) : s(Blocks.AIR);
					} else if (k == 1 || k == h) {
						b = s(Blocks.SMOOTH_QUARTZ);
					} else if (k == h - 2) {
						b = s(band);
					} else if ((dx == 0 || dz == 0) && k % 5 >= 2 && k % 5 <= 3
							&& !(Integer.signum(dx) == -door.getStepX() && Integer.signum(dz) == -door.getStepZ())) { // no window behind the ladder
						b = Kit.connected(Blocks.BLACK_STAINED_GLASS_PANE, dz == 0 ? false : true);
					} else {
						b = s(Blocks.WHITE_CONCRETE);
					}
					c.set(x, y + k, z, b);
				}
				if (!shell) {
					c.set(x, y + h, z, s(Blocks.SMOOTH_QUARTZ));
				}
			}
		}
		// Eave, dome.
		for (int dx = -r - 1; dx <= r + 1; dx++) {
			for (int dz = -r - 1; dz <= r + 1; dz++) {
				double d = Math.hypot(dx, dz);
				if (d <= r + 1.4 && d > r - 0.6) {
					c.set(cx + dx, y + h + 1, cz + dz, Kit.slab(Blocks.QUARTZ_SLAB, false));
				}
			}
		}
		int R = r;
		for (int k = 0; k <= R; k++) {
			double rk = Math.round(R * Math.sqrt(Math.max(0, 1 - Math.pow(k / (R + 0.5), 2))));
			for (int dx = -R; dx <= R; dx++) {
				for (int dz = -R; dz <= R; dz++) {
					double d = Math.hypot(dx, dz);
					if (d <= rk + 0.4) {
						Block b = k == R || rk <= 1 ? cap : k % 2 == 0 ? ring1 : ring2;
						c.set(cx + dx, y + h + 1 + k, cz + dz, d > rk - 1.2 ? s(b) : s(Blocks.WHITE_CONCRETE));
					}
				}
			}
		}
		c.set(cx, y + h + R + 2, cz, Kit.slab(Blocks.QUARTZ_SLAB, false));
		// Door and ladder.
		int ddx = door.getStepX() * r, ddz = door.getStepZ() * r;
		Kit.door(c, cx + ddx, y + 1, cz + ddz, Blocks.DARK_OAK_DOOR, door.getOpposite());
		c.set(cx + ddx + door.getStepX(), y, cz + ddz + door.getStepZ(), s(Blocks.SMOOTH_QUARTZ));
		c.fill(cx + ddx + door.getStepX(), y + 1, cz + ddz + door.getStepZ(), cx + ddx + door.getStepX(), y + 2, cz + ddz + door.getStepZ(), s(Blocks.AIR));
		Direction back = door.getOpposite();
		int lx = cx + back.getStepX() * (r - 1), lz = cz + back.getStepZ() * (r - 1);
		for (int k = 1; k < h; k++) {
			c.set(lx, y + k, lz, Canvas.facing(s(Blocks.LADDER), door));
		}
		for (int k = 4; k < h; k += 5) {
			c.set(cx, y + k, cz, CloudtopIsles.lantern(Blocks.LANTERN, true));
		}
	}

	// ==================================================================================================== Ancient Tree landmass

	static void ancientTree(Canvas c) {
		CloudtopIslesV7.Height h = UpperCloudtop::treeGround;
		land(c, T_X, T_Z, T_RX, T_RZ, h, 31);
		roots(c);
		giantTree(c);
		settlement(c, h);
		field(c, h);
		pavilion(c);
		arcade(c, RED_X1, RED_Z1, T_Y, true);
		arcade(c, -115, -115, T_Y, false);
		// Paths (after the roots, so they cut a way through where they cross one).
		dirtPath(c, h, 3, -135, -192, -136, -172, -137, -152);
		dirtPath(c, h, 3, -130, -146, -118, -147, -98, -150);
		dirtPath(c, h, 3, -60, -172, -60, -179); // stops short of the pavilion steps (it clears above itself)
		dirtPath(c, h, 2.5, -137, -140, -122, -126, -100, -116);
		dirtPath(c, h, 3, -45, -160, -38, -165);
		for (int[] t : new int[][]{{-90, -133, 5}, {-50, -170, 6}, {-56, -131, 5}, {-82, -201, 6}, {-40, -190, 5}, {-120, -128, 5}, {-160, -152, 4}}) {
			smallTree(c, t[0], treeGround(t[0], t[1]), t[1], t[2], t[0] % 3 == 0);
		}
		plants(c, T_X - T_RX, T_Z - T_RZ, T_X + T_RX, T_Z + T_RZ, h, 17);
	}

	/** The colourful settlement on its terrace: eight buildings round a plaza, paths to every door, lamps. */
	static void settlement(Canvas c, CloudtopIslesV7.Height h) {
		if (!c.near(-138, -146, 30)) {
			return;
		}
		int y = SET_Y;
		building(c, -153, -162, 7, 9, y, 15, WHITE, Direction.SOUTH, Roof.FLAT, false);
		building(c, -144, -163, 8, 10, y, 17, YELLOW, Direction.SOUTH, Roof.FLAT, false);
		domedTower(c, -129, -159, y, 4, 15, Blocks.BLUE_CONCRETE, Blocks.BLUE_TERRACOTTA, Blocks.LIGHT_BLUE_CONCRETE, Blocks.LIGHT_BLUE_CONCRETE, Direction.SOUTH);
		building(c, -155, -150, 9, 9, y, 10, PINK, Direction.EAST, Roof.FLAT, false);
		domedTower(c, -124, -146, y, 3, 12, Blocks.GREEN_CONCRETE, Blocks.GREEN_TERRACOTTA, Blocks.LIME_TERRACOTTA, Blocks.LIME_TERRACOTTA, Direction.WEST);
		building(c, -154, -138, 8, 10, y, 18, YELLOW, Direction.EAST, Roof.FLAT, false);
		building(c, -144, -136, 7, 7, y, 9, WHITE_OPEN, Direction.NORTH, Roof.FLAT, false);
		building(c, -134, -137, 7, 9, y, 13, WHITE_OPEN, Direction.NORTH, Roof.FLAT, false);
		// The plaza: smooth sandstone slabs framed by packed mud.
		for (int x = -143; x <= -131; x++) {
			for (int z = -151; z <= -141; z++) {
				boolean rim = x == -143 || x == -131 || z == -151 || z == -141;
				c.set(x, y, z, rim ? s(Blocks.PACKED_MUD) : Kit.slab(Blocks.SMOOTH_SANDSTONE_SLAB, true));
				c.fill(x, y + 1, z, x, y + 3, z, s(Blocks.AIR));
			}
		}
		for (int[] l : new int[][]{{-143, -151}, {-131, -151}, {-143, -141}, {-131, -141}}) {
			CloudtopIsles.lampPost(c, l[0], y, l[1]);
		}
		// Short paths to the doors.
		dirtPath(c, h, 2, -150, -153, -143, -150);
		dirtPath(c, h, 2, -140, -153, -139, -151);
		dirtPath(c, h, 2, -129, -154, -132, -151);
		dirtPath(c, h, 2, -146, -146, -143, -146);
		dirtPath(c, h, 2, -128, -146, -131, -146);
		dirtPath(c, h, 2, -146, -133, -141, -141);
		dirtPath(c, h, 2, -141, -137, -139, -141);
		dirtPath(c, h, 2, -131, -138, -133, -141);
	}

	/** The combat field: open grass, sparse rocks, broken walls, a ruined arch; the east root arches over it. */
	static void field(Canvas c, CloudtopIslesV7.Height h) {
		if (!c.near(-70, -150, 36)) {
			return;
		}
		int y = T_Y;
		for (int[] r : new int[][]{{-85, -160, 3, 2}, {-48, -145, 4, 2}, {-92, -140, 2, 1}}) {
			CloudtopIsles.rock(c, r[0], y, r[1], r[2], r[3]);
		}
		brokenWall(c, -80, -135, -74, -135, y);
		brokenWall(c, -60, -175, -60, -170, y);
		brokenWall(c, -94, -152, -94, -146, y);
		// A ruined arch: one pier whole, the other broken, part of the ring.
		int ax = -66, az = -140;
		for (int k = 1; k <= 7; k++) {
			c.set(ax - 3, y + k, az, k % 3 == 0 ? s(Blocks.MOSSY_STONE_BRICKS) : s(Blocks.STONE_BRICKS));
			if (k <= 4) {
				c.set(ax + 3, y + k, az, k == 4 ? Kit.stairs(Blocks.STONE_BRICK_STAIRS, Direction.WEST, true) : s(Blocks.STONE_BRICKS));
			}
		}
		c.set(ax - 2, y + 8, az, s(Blocks.STONE_BRICKS));
		c.set(ax - 1, y + 8, az, Kit.stairs(Blocks.STONE_BRICK_STAIRS, Direction.WEST, true));
		c.set(ax - 2, y + 7, az, Kit.stairs(Blocks.STONE_BRICK_STAIRS, Direction.EAST, true));
		c.set(ax + 2, y + 1, az + 1, s(Blocks.COBBLESTONE));
		c.set(ax + 1, y + 1, az - 1, Kit.slab(Blocks.STONE_BRICK_SLAB, false));
	}

	/** A low broken wall from (x1, z1) to (x2, z2): stone bricks, mossy and cobble, uneven height, stair and slab tops. */
	static void brokenWall(Canvas c, int x1, int z1, int x2, int z2, int y) {
		int n = Math.max(Math.abs(x2 - x1), Math.abs(z2 - z1));
		for (int i = 0; i <= n; i++) {
			int x = x1 + Integer.signum(x2 - x1) * i, z = z1 + Integer.signum(z2 - z1) * i;
			int hgt = 1 + (int) (unit(x, z, 71) * 3);
			for (int k = 1; k <= hgt; k++) {
				double u = unit3(x, k, z, 72);
				c.set(x, y + k, z, u < 0.5 ? s(Blocks.STONE_BRICKS) : u < 0.8 ? s(Blocks.MOSSY_STONE_BRICKS) : s(Blocks.COBBLESTONE));
			}
			c.set(x, y + hgt + 1, z, unit(x, z, 73) < 0.5 ? Kit.slab(Blocks.STONE_BRICK_SLAB, false) : s(Blocks.AIR));
		}
	}

	/**
	 * The classical pavilion: a two-high pale plinth with steps, columns all round (9 high), a beam, and a broad low grey
	 * roof of polished andesite with stone brick eaves; pink, magenta and red banners on the columns. Open sides.
	 */
	static void pavilion(Canvas c) {
		int x1 = -72, x2 = -48, z1 = -198, z2 = -182, y = T_Y;
		if (!c.near((x1 + x2) / 2, (z1 + z2) / 2, 20)) {
			return;
		}
		for (int x = x1; x <= x2; x++) {
			for (int z = z1; z <= z2; z++) {
				boolean rim = x == x1 || x == x2 || z == z1 || z == z2;
				c.set(x, y + 1, z, rim ? s(Blocks.QUARTZ_BRICKS) : s(Blocks.SMOOTH_QUARTZ));
				c.set(x, y + 2, z, s(Blocks.SMOOTH_QUARTZ));
				c.fill(x, y + 3, z, x, y + 11, z, s(Blocks.AIR));
			}
		}
		for (int x = x1 - 1; x <= x2 + 1; x++) {
			c.set(x, y + 1, z1 - 1, Kit.slab(Blocks.QUARTZ_SLAB, false));
			c.set(x, y + 1, z2 + 1, Kit.slab(Blocks.QUARTZ_SLAB, false));
		}
		for (int z = z1; z <= z2; z++) {
			c.set(x1 - 1, y + 1, z, Kit.slab(Blocks.QUARTZ_SLAB, false));
			c.set(x2 + 1, y + 1, z, Kit.slab(Blocks.QUARTZ_SLAB, false));
		}
		for (int x = -63; x <= -57; x++) {
			c.set(x, y + 1, z2 + 1, Kit.stairs(Blocks.QUARTZ_STAIRS, Direction.NORTH, false));
			c.set(x, y + 2, z2, Kit.stairs(Blocks.QUARTZ_STAIRS, Direction.NORTH, false));
		}
		// Columns round the edge: every three along the long sides, every four along the short ones.
		Block[] banners = {Blocks.PINK_WALL_BANNER, Blocks.MAGENTA_WALL_BANNER, Blocks.RED_WALL_BANNER};
		for (int x = x1; x <= x2; x++) {
			for (int z = z1; z <= z2; z++) {
				boolean longSide = (z == z1 || z == z2) && (x - x1) % 3 == 0;
				boolean shortSide = (x == x1 || x == x2) && (z - z1) % 4 == 0;
				if (!longSide && !shortSide) {
					continue;
				}
				c.set(x, y + 3, z, s(Blocks.CHISELED_QUARTZ_BLOCK));
				c.fill(x, y + 4, z, x, y + 10, z, s(Blocks.QUARTZ_PILLAR));
				c.set(x, y + 11, z, s(Blocks.CHISELED_QUARTZ_BLOCK));
				if ((z == z1 || z == z2) && x != x1 && x != x2 && ((x - x1) / 3) % 2 == 1) {
					Direction out = z == z1 ? Direction.NORTH : Direction.SOUTH;
					c.set(x, y + 8, z + out.getStepZ(), s(banners[((x - x1) / 6) % 3]).setValue(WallBannerBlock.FACING, out));
				}
			}
		}
		// Beam and roof: a low gable along x, two blocks in per step, stone brick slab eaves.
		int by = y + 12;
		for (int x = x1; x <= x2; x++) {
			for (int z = z1; z <= z2; z++) {
				c.set(x, by, z, s(Blocks.SMOOTH_QUARTZ));
			}
		}
		int half = (z2 - z1) / 2 + 1;
		for (int x = x1 - 1; x <= x2 + 1; x++) {
			for (int z = z1 - 1; z <= z2 + 1; z++) {
				int fromEdge = Math.min(z - (z1 - 1), (z2 + 1) - z);
				int step = Math.min(4, fromEdge / 2);
				int ry = by + 1 + step;
				for (int yy = by + 1; yy < ry; yy++) {
					c.set(x, yy, z, s(Blocks.POLISHED_ANDESITE));
				}
				BlockState top;
				if (fromEdge == 0) {
					top = Kit.slab(Blocks.STONE_BRICK_SLAB, false);
				} else if (step >= 4) {
					top = Kit.slab(Blocks.POLISHED_ANDESITE_SLAB, false);
				} else if (fromEdge % 2 == 1) {
					top = Kit.stairs(Blocks.POLISHED_ANDESITE_STAIRS, z < (z1 + z2) / 2.0 ? Direction.SOUTH : Direction.NORTH, false);
				} else {
					top = s(Blocks.POLISHED_ANDESITE);
				}
				c.set(x, ry, z, top);
			}
		}
		c.set(-60, by - 1, -190, CloudtopIsles.lantern(Blocks.LANTERN, true));
		c.set(-66, by - 1, -190, CloudtopIsles.lantern(Blocks.LANTERN, true));
		c.set(-54, by - 1, -190, CloudtopIsles.lantern(Blocks.LANTERN, true));
	}

	/**
	 * A curved arcade (30 long, 9 wide): pale walls of round arches on piers, open at both ends, under a curved barrel
	 * roof, red (dark oak and red terracotta) or blue (blue terracotta and concrete, prismarine eaves). Along x.
	 */
	static void arcade(Canvas c, int x1, int z1, int y, boolean red) {
		int x2 = x1 + 29, z2 = z1 + 8;
		if (!c.near((x1 + x2) / 2, (z1 + z2) / 2, 22)) {
			return;
		}
		for (int x = x1; x <= x2; x++) {
			int along = x - x1;
			boolean pier = along % 6 == 0 || x == x2;
			int m = along % 6; // 1..5 inside a bay
			int open = pier ? 0 : 6 + (m == 3 ? 1 : 0) + (m == 2 || m == 4 ? 1 : 0) - (m == 1 || m == 5 ? 0 : 0);
			for (int z = z1; z <= z2; z++) {
				boolean side = z == z1 || z == z2;
				c.set(x, y, z, side ? s(Blocks.QUARTZ_BRICKS) : (z + x) % 2 == 0 ? s(Blocks.QUARTZ_BRICKS) : s(Blocks.SMOOTH_QUARTZ));
				for (int k = 1; k <= 10; k++) {
					BlockState b;
					if (side) {
						if (pier) {
							b = k == 1 || k == 10 ? s(Blocks.CHISELED_QUARTZ_BLOCK) : s(Blocks.QUARTZ_PILLAR);
						} else if (k <= open) {
							b = s(Blocks.AIR);
						} else if (k == open + 1) {
							b = s(Blocks.SMOOTH_QUARTZ);
						} else {
							b = k == 10 ? s(Blocks.SMOOTH_QUARTZ) : s(Blocks.QUARTZ_BRICKS);
						}
					} else if (x == x1 || x == x2) {
						int mz = z - z1; // the end walls: one wide arch
						boolean endPier = mz == 1 || mz == 7;
						int endOpen = mz == 4 ? 8 : mz == 3 || mz == 5 ? 7 : 6;
						b = endPier ? s(Blocks.QUARTZ_PILLAR) : k <= endOpen ? s(Blocks.AIR) : s(Blocks.QUARTZ_BRICKS);
					} else {
						b = s(Blocks.AIR);
					}
					c.set(x, y + k, z, b);
				}
			}
		}
		// The curved roof: a barrel across the width, eaves at the sides.
		Block fill = red ? Blocks.DARK_OAK_PLANKS : Blocks.BLUE_CONCRETE;
		Block topBlock = red ? Blocks.RED_TERRACOTTA : Blocks.BLUE_TERRACOTTA;
		Block stair = red ? Blocks.DARK_OAK_STAIRS : Blocks.PRISMARINE_STAIRS;
		Block slab = red ? Blocks.DARK_OAK_SLAB : Blocks.PRISMARINE_SLAB;
		double mid = (z1 + z2) / 2.0;
		for (int x = x1 - 1; x <= x2 + 1; x++) {
			for (int z = z1 - 1; z <= z2 + 1; z++) {
				double dz = (z - mid) / 5.5;
				int rise = (int) Math.round(4 * Math.sqrt(Math.max(0, 1 - dz * dz)));
				int top = y + 11 + rise;
				for (int yy = y + 11; yy < top; yy++) {
					c.set(x, yy, z, s(fill));
				}
				Direction down = z < mid ? Direction.NORTH : Direction.SOUTH;
				int riseNext = (int) Math.round(4 * Math.sqrt(Math.max(0, 1 - Math.pow((z + down.getStepZ() - mid) / 5.5, 2))));
				BlockState t;
				if (z == z1 - 1 || z == z2 + 1) {
					t = Kit.slab(slab, false);
				} else if (riseNext < rise) {
					t = Kit.stairs(stair, down.getOpposite(), false);
				} else {
					t = s(topBlock);
				}
				c.set(x, top, z, t);
			}
		}
		for (int x = x1 + 3; x <= x2 - 3; x += 6) {
			c.set(x, y + 10, (z1 + z2) / 2, CloudtopIsles.lantern(Blocks.LANTERN, true));
		}
	}

	/** A small stylized tree on ground y: a trunk and two faceted foliage blobs (concrete core, leaf rim). */
	static void smallTree(Canvas c, int x, int y, int z, int h, boolean dark) {
		if (!c.near(x, z, 6)) {
			return;
		}
		c.fill(x, y + 1, z, x, y + h, z, s(dark ? Blocks.DARK_OAK_LOG : unit(x, z, 3) < 0.5 ? Blocks.OAK_LOG : Blocks.STRIPPED_OAK_LOG));
		for (int dy = -1; dy <= 2; dy++) {
			int r = dy == 2 ? 1 : dy == -1 ? 2 : 3;
			for (int dx = -r; dx <= r; dx++) {
				for (int dz = -r; dz <= r; dz++) {
					if (Math.abs(dx) + Math.abs(dz) > r + 1) {
						continue;
					}
					boolean rim = Math.abs(dx) + Math.abs(dz) >= r || dy == 2;
					double u = unit3(x + dx, dy, z + dz, 91);
					BlockState b = rim ? Kit.leaves(u < 0.5 ? Blocks.OAK_LEAVES : Blocks.AZALEA_LEAVES) : u < 0.5 ? s(Blocks.GREEN_CONCRETE) : s(Blocks.GREEN_TERRACOTTA);
					if (dx != 0 || dz != 0 || dy > 0) {
						c.set(x + dx, y + h + dy, z + dz, b);
					}
				}
			}
		}
	}

	// ==================================================================================================== Grand Sanctuary

	static void sanctuary(Canvas c) {
		CloudtopIslesV7.Height h = UpperCloudtop::sanctuaryGround;
		land(c, S_X, S_Z, S_RX, S_RZ, h, 47);
		courtyard(c);
		temple(c);
		summitPavilion(c);
		bell(c);
		archway(c, 42, -166, false, 10, 17, 0, false);
		archway(c, 55, -190, false, 14, 23, 0, false);
		archway(c, 64, -214, false, 12, 19, 1, true);
		building(c, 147, -172, 7, 9, S_Y, 22, WHITE, Direction.WEST, Roof.BLUE, false);
		building(c, 157, -165, 9, 11, S_Y, 25, WHITE, Direction.WEST, Roof.ORANGE, false);
		building(c, 148, -150, 7, 7, S_Y, 12, WHITE_OPEN, Direction.WEST, Roof.FLAT, false);
		building(c, 158, -142, 9, 11, S_Y, 20, WHITE, Direction.WEST, Roof.ANDESITE, true);
		building(c, 147, -130, 7, 9, S_Y, 18, WHITE_OPEN, Direction.WEST, Roof.BLUE, false);
		ruin(c, 44, -154, S_Y);
		ruin(c, 146, -204, S_Y);
		quartzPath(c, S_Y, 3, 27, -165, 42, -166, 70, -150, 85, -141);
		quartzPath(c, S_Y, 3, 135, -136, 146, -138);
		quartzPath(c, S_Y, 2, 136, -126, 146, -126);
		quartzPath(c, S_Y, 2, 140, -150, 147, -147);
		for (int[] t : new int[][]{{30, -190, 5}, {70, -138, 6}, {140, -116, 5}, {162, -186, 5}, {78, -224, 5}, {28, -140, 4}, {54, -118, 5}}) {
			smallTree(c, t[0], S_Y, t[1], t[2], t[0] % 4 == 0);
		}
		CloudtopIsles.rock(c, 66, S_Y, -128, 3, 2);
		CloudtopIsles.rock(c, 50, S_Y, -134, 2, 1);
		plants(c, S_X - S_RX, S_Z - S_RZ, S_X + S_RX, S_Z + S_RZ, h, 19);
	}

	/** Blue geometric inlay on a temple tier face: base band, middle stripe, squares above and below, cyan corners. */
	static BlockState templeFace(int k, int along, boolean corner) {
		if (k == 0) {
			return s(Blocks.BLUE_TERRACOTTA);
		}
		if (k == TIER_H - 1) {
			return s(Blocks.SMOOTH_QUARTZ);
		}
		if (corner) {
			return s(Blocks.CYAN_CONCRETE);
		}
		if (k == 3) {
			return s(Blocks.BLUE_CONCRETE);
		}
		if ((k == 2 || k == 4) && Math.floorMod(along, 6) == 0) {
			return s(Blocks.LIGHT_BLUE_CONCRETE);
		}
		if ((k == 1 || k == 5) && Math.floorMod(along, 6) == 3) {
			return s(Blocks.BLUE_CONCRETE);
		}
		return s(Blocks.WHITE_CONCRETE);
	}

	/** The stepped temple: five tiers with blue inlays, quartz ledges, and the monumental staircase up the south face. */
	static void temple(Canvas c) {
		if (!c.near((TM_X1 + TM_X2) / 2, (TM_Z1 + ST_Z0) / 2, 50)) {
			return;
		}
		for (int i = 0; i < TIERS; i++) {
			int x1 = TM_X1 + TIER_IN * i, x2 = TM_X2 - TIER_IN * i, z1 = TM_Z1 + TIER_IN * i, z2 = TM_Z2 - TIER_IN * i;
			int y0 = S_Y + 1 + TIER_H * i;
			for (int x = x1; x <= x2; x++) {
				for (int z = z1; z <= z2; z++) {
					if (!c.inChunk(x, z)) {
						continue;
					}
					boolean ex = x == x1 || x == x2, ez = z == z1 || z == z2;
					for (int k = 0; k < TIER_H; k++) {
						BlockState b;
						if (ex || ez) {
							b = templeFace(k, ex ? z - z1 : x - x1, ex && ez);
						} else if (k == TIER_H - 1) {
							b = (x + z) % 2 == 0 ? s(Blocks.SMOOTH_QUARTZ) : s(Blocks.QUARTZ_BRICKS);
						} else {
							b = s(Blocks.WHITE_CONCRETE);
						}
						c.set(x, y0 + k, z, b);
					}
				}
			}
			// A quartz slab lip round each ledge.
			if (i > 0) {
				int px1 = TM_X1 + TIER_IN * (i - 1), px2 = TM_X2 - TIER_IN * (i - 1), pz1 = TM_Z1 + TIER_IN * (i - 1), pz2 = TM_Z2 - TIER_IN * (i - 1);
				for (int x = px1; x <= px2; x++) {
					c.set(x, y0, pz1, Kit.slab(Blocks.QUARTZ_SLAB, false));
					c.set(x, y0, pz2, Kit.slab(Blocks.QUARTZ_SLAB, false));
				}
				for (int z = pz1; z <= pz2; z++) {
					c.set(px1, y0, z, Kit.slab(Blocks.QUARTZ_SLAB, false));
					c.set(px2, y0, z, Kit.slab(Blocks.QUARTZ_SLAB, false));
				}
			}
		}
		// The staircase: 15 wide, one up per block from the courtyard to the platform, quartz side walls with a blue line.
		for (int i = 0; i < ST_STEPS; i++) {
			int z = ST_Z0 - i, yStep = S_Y + 1 + i;
			for (int x = ST_X1 - 2; x <= ST_X2 + 2; x++) {
				if (!c.inChunk(x, z)) {
					continue;
				}
				boolean wall = x < ST_X1 || x > ST_X2;
				for (int y = S_Y + 1; y < yStep; y++) {
					c.set(x, y, z, wall && y == yStep - 2 ? s(Blocks.BLUE_CONCRETE) : wall ? s(Blocks.SMOOTH_QUARTZ) : s(Blocks.WHITE_CONCRETE));
				}
				if (wall) {
					c.set(x, yStep, z, s(Blocks.SMOOTH_QUARTZ));
					c.set(x, yStep + 1, z, x == ST_X1 - 1 || x == ST_X2 + 1 ? Kit.slab(Blocks.QUARTZ_SLAB, false) : s(Blocks.AIR));
				} else {
					c.set(x, yStep, z, Kit.stairs(Blocks.QUARTZ_STAIRS, Direction.NORTH, false));
					c.fill(x, yStep + 1, z, x, yStep + 4, z, s(Blocks.AIR));
				}
			}
		}
		// The landing at the top, joining the platform.
		for (int z = ST_Z0 - ST_STEPS; z >= ST_Z0 - ST_STEPS - 2; z--) {
			for (int x = ST_X1 - 2; x <= ST_X2 + 2; x++) {
				c.fill(x, S_Y + 1, z, x, TOP_Y - 1, z, s(Blocks.WHITE_CONCRETE));
				c.set(x, TOP_Y, z, s(Blocks.SMOOTH_QUARTZ));
				c.fill(x, TOP_Y + 1, z, x, TOP_Y + 4, z, s(Blocks.AIR));
			}
		}
		// The first step sits in the courtyard: a blue line across its foot.
		for (int x = ST_X1 - 2; x <= ST_X2 + 2; x++) {
			c.set(x, S_Y, ST_Z0 + 1, s(Blocks.BLUE_CONCRETE));
		}
	}

	/**
	 * The summit pavilion: columns all round (9 high) on the upper platform, a pale beam with a blue band, a triangular
	 * pediment front and back (blue tympanum), a low grey roof, an open interior with a blue inlaid floor and a hanging
	 * golden fixture.
	 */
	static void summitPavilion(Canvas c) {
		if (!c.near(110, -190, 20)) {
			return;
		}
		int fy = TOP_Y;
		for (int x = PV_X1; x <= PV_X2; x++) {
			for (int z = PV_Z1; z <= PV_Z2; z++) {
				int dx = Math.abs(x - 110), dz = Math.abs(z - (PV_Z1 + PV_Z2) / 2);
				c.set(x, fy, z, (dx == dz || dx + dz == 6) ? s(Blocks.BLUE_CONCRETE) : s(Blocks.SMOOTH_QUARTZ));
				c.fill(x, fy + 1, z, x, fy + 9, z, s(Blocks.AIR));
			}
		}
		for (int x = PV_X1; x <= PV_X2; x++) {
			for (int z = PV_Z1; z <= PV_Z2; z++) {
				boolean ezs = z == PV_Z1 || z == PV_Z2, exs = x == PV_X1 || x == PV_X2;
				boolean col = ezs && (x - PV_X1) % 3 == 0 || exs && (z - PV_Z1) % 4 == 0;
				if (!col) {
					continue;
				}
				c.set(x, fy + 1, z, s(Blocks.CHISELED_QUARTZ_BLOCK));
				c.fill(x, fy + 2, z, x, fy + 8, z, s(Blocks.QUARTZ_PILLAR));
				c.set(x, fy + 9, z, s(Blocks.CHISELED_QUARTZ_BLOCK));
			}
		}
		// Ceiling/beam (y fy+10) with a blue band round the outside, then the pediments and roof.
		int by = fy + 10;
		for (int x = PV_X1 - 1; x <= PV_X2 + 1; x++) {
			for (int z = PV_Z1 - 1; z <= PV_Z2 + 1; z++) {
				boolean rim = x == PV_X1 - 1 || x == PV_X2 + 1 || z == PV_Z1 - 1 || z == PV_Z2 + 1;
				c.set(x, by, z, rim ? s(Blocks.BLUE_TERRACOTTA) : s(Blocks.SMOOTH_QUARTZ));
			}
		}
		for (int x = PV_X1 - 1; x <= PV_X2 + 1; x++) {
			int hgt = Math.max(0, Math.min(4, (13 - Math.abs(x - 110)) / 3));
			int hNext = Math.max(0, Math.min(4, (13 - Math.abs(x + (x < 110 ? 1 : -1) - 110)) / 3));
			for (int z = PV_Z1 - 1; z <= PV_Z2 + 1; z++) {
				boolean end = z == PV_Z1 - 1 || z == PV_Z2 + 1;
				for (int yy = by + 1; yy < by + 1 + hgt; yy++) {
					boolean edge = x == PV_X1 - 1 || x == PV_X2 + 1 || yy == by + hgt;
					c.set(x, yy, z, end ? (edge || hgt - (yy - by) == 0 ? s(Blocks.SMOOTH_QUARTZ) : s(Blocks.BLUE_CONCRETE)) : s(Blocks.SMOOTH_QUARTZ));
				}
				BlockState top;
				if (x == 110) {
					top = Kit.slab(Blocks.POLISHED_ANDESITE_SLAB, false);
				} else if (hNext > hgt) {
					top = Kit.stairs(Blocks.POLISHED_ANDESITE_STAIRS, x < 110 ? Direction.EAST : Direction.WEST, false);
				} else {
					top = s(Blocks.POLISHED_ANDESITE);
				}
				c.set(x, by + 1 + hgt, z, top);
			}
		}
		// The pediments: quartz stairs along the sloping edges, a pale ring in the blue.
		for (int z : new int[]{PV_Z1 - 1, PV_Z2 + 1}) {
			c.set(110, by + 2, z, s(Blocks.SMOOTH_QUARTZ));
			c.set(108, by + 1, z, s(Blocks.SMOOTH_QUARTZ));
			c.set(112, by + 1, z, s(Blocks.SMOOTH_QUARTZ));
		}
		// The hanging golden fixture.
		c.fill(110, fy + 6, -190, 110, fy + 9, -190, s(Blocks.CHAIN));
		c.set(110, fy + 5, -190, s(Blocks.GOLD_BLOCK));
		c.set(110, fy + 4, -190, CloudtopIsles.lantern(Blocks.LANTERN, true));
		for (int[] o : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
			c.set(110 + o[0], fy + 5, -190 + o[1], s(Blocks.CHAIN).setValue(ChainBlock.AXIS, o[0] != 0 ? Direction.Axis.X : Direction.Axis.Z));
			c.set(110 + o[0] * 2, fy + 5, -190 + o[1] * 2, s(Blocks.GOLD_BLOCK));
			c.set(110 + o[0] * 2, fy + 4, -190 + o[1] * 2, CloudtopIsles.lantern(Blocks.LANTERN, true));
		}
	}

	/** The ceremonial courtyard: grass with a pale approach, borders, cross path, striped columns, two platforms, lamps. */
	static void courtyard(Canvas c) {
		if (!c.near(110, -130, 32)) {
			return;
		}
		int y = S_Y;
		for (int x = CY_X1; x <= CY_X2; x++) {
			for (int z = CY_Z1; z <= CY_Z2; z++) {
				boolean border = x == CY_X1 || x == CY_X2 || z == CY_Z1 || z == CY_Z2;
				boolean approach = x >= ST_X1 && x <= ST_X2;
				boolean cross = z >= -130 && z <= -126;
				BlockState b = border ? s(Blocks.QUARTZ_BRICKS) : approach ? ((x == ST_X1 || x == ST_X2) ? s(Blocks.QUARTZ_BRICKS) : s(Blocks.SMOOTH_QUARTZ))
						: cross ? s(Blocks.SMOOTH_QUARTZ) : Math.floorMod(x - CY_X1, 8) == 0 || Math.floorMod(z - CY_Z1, 8) == 0 ? s(Blocks.QUARTZ_BRICKS)
						: s(Blocks.GRASS_BLOCK);
				c.set(x, y, z, b);
				c.fill(x, y + 1, z, x, y + 3, z, s(Blocks.AIR));
			}
		}
		for (int z = -148; z <= -116; z += 8) {
			for (int x : new int[]{96, 124}) {
				c.set(x, y + 1, z, s(Blocks.CHISELED_QUARTZ_BLOCK));
				for (int k = 2; k <= 11; k++) {
					c.set(x, y + k, z, k % 3 == 0 ? s(Blocks.BLUE_CONCRETE) : k % 3 == 1 ? s(Blocks.QUARTZ_PILLAR) : s(Blocks.WHITE_CONCRETE));
				}
				c.set(x, y + 12, z, s(Blocks.CHISELED_QUARTZ_BLOCK));
				c.set(x, y + 13, z, CloudtopIsles.lantern(Blocks.SOUL_LANTERN, false));
			}
		}
		// Two decorative platforms on the east side.
		for (int[] p : new int[][]{{128, -146}, {128, -122}}) {
			for (int x = p[0]; x <= p[0] + 6; x++) {
				for (int z = p[1]; z <= p[1] + 6; z++) {
					boolean rim = x == p[0] || x == p[0] + 6 || z == p[1] || z == p[1] + 6;
					c.set(x, y + 1, z, rim ? Kit.stairs(Blocks.QUARTZ_STAIRS, x == p[0] ? Direction.EAST : x == p[0] + 6 ? Direction.WEST
							: z == p[1] ? Direction.SOUTH : Direction.NORTH, false) : s(Blocks.SMOOTH_QUARTZ));
				}
			}
			c.set(p[0] + 3, y + 2, p[1] + 3, Kit.slab(Blocks.QUARTZ_SLAB, false));
		}
	}

	/**
	 * The golden bell: a body of stacked rings (gold, a yellow concrete and a yellow terracotta band, a raw gold lip
	 * flaring out at the bottom), light weighted plates round its shoulder, a raw gold clapper, hung on chains from a
	 * pale frame.
	 */
	static void bell(Canvas c) {
		if (!c.near(BELL_X, BELL_Z, 10)) {
			return;
		}
		int y = S_Y;
		for (int[] p : new int[][]{{-5, -5}, {5, -5}, {-5, 5}, {5, 5}}) {
			c.set(BELL_X + p[0], y + 1, BELL_Z + p[1], s(Blocks.CHISELED_QUARTZ_BLOCK));
			c.fill(BELL_X + p[0], y + 2, BELL_Z + p[1], BELL_X + p[0], y + 13, BELL_Z + p[1], s(Blocks.QUARTZ_PILLAR));
		}
		int beam = y + 14;
		for (int k = -5; k <= 5; k++) {
			c.set(BELL_X + k, beam, BELL_Z - 5, s(Blocks.SMOOTH_QUARTZ));
			c.set(BELL_X + k, beam, BELL_Z + 5, s(Blocks.SMOOTH_QUARTZ));
			c.set(BELL_X - 5, beam, BELL_Z + k, s(Blocks.SMOOTH_QUARTZ));
			c.set(BELL_X + 5, beam, BELL_Z + k, s(Blocks.SMOOTH_QUARTZ));
			c.set(BELL_X + k, beam, BELL_Z, s(Blocks.SMOOTH_QUARTZ));
		}
		c.set(BELL_X, beam + 1, BELL_Z, Kit.slab(Blocks.QUARTZ_SLAB, false));
		double[] radius = {1.3, 2.2, 2.7, 3.0, 3.0, 3.2, 3.6, 4.0, 4.6};
		int crown = beam - 3;
		c.fill(BELL_X, crown + 1, BELL_Z, BELL_X, beam - 1, BELL_Z, s(Blocks.CHAIN));
		for (int k = 0; k < radius.length; k++) {
			int yy = crown - k;
			double r = radius[k];
			for (int dx = -5; dx <= 5; dx++) {
				for (int dz = -5; dz <= 5; dz++) {
					double d = Math.hypot(dx, dz);
					boolean solid = k == 0 ? d <= r + 0.3 : d <= r + 0.3 && d > r - 1.1;
					if (!solid) {
						continue;
					}
					Block b = k == radius.length - 1 ? Blocks.RAW_GOLD_BLOCK : k == 3 ? Blocks.YELLOW_CONCRETE : k == 6 ? Blocks.YELLOW_TERRACOTTA : Blocks.GOLD_BLOCK;
					c.set(BELL_X + dx, yy, BELL_Z + dz, s(b));
					if (k == 1 && d > radius[0] + 0.3) {
						c.set(BELL_X + dx, yy + 1, BELL_Z + dz, s(Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE));
					}
				}
			}
		}
		c.fill(BELL_X, crown - 4, BELL_Z, BELL_X, crown - 1, BELL_Z, s(Blocks.CHAIN));
		c.set(BELL_X, crown - 5, BELL_Z, s(Blocks.RAW_GOLD_BLOCK));
	}

	/**
	 * A monumental archway (opening {@code w} wide, {@code h} high to the crown) in the plane along x ({@code alongX}) or
	 * along z: piers three thick with quartz pillar faces, a white ring with smooth quartz edges, a blue band and an orange
	 * accent at the springing; {@code lean} shears it over by one block every eight, {@code ruined} breaks the top of one
	 * side and drops rubble.
	 */
	static void archway(Canvas c, int cx, int cz, boolean alongX, int w, int h, int lean, boolean ruined) {
		if (!c.near(cx, cz, w + 6)) {
			return;
		}
		int y = S_Y;
		double ri = w / 2.0, ro = ri + 3;
		int spring = h - (int) Math.ceil(ro);
		for (int t = -(int) Math.ceil(ro); t <= Math.ceil(ro); t++) {
			for (int k = 1; k <= h; k++) {
				double at = Math.abs(t);
				boolean on;
				BlockState b;
				if (k <= spring) {
					on = at > ri - 0.01 && at <= ro;
					boolean face = at > ro - 1 || at < ri + 1;
					b = k == spring ? s(Blocks.ORANGE_TERRACOTTA) : k == 1 ? s(Blocks.SMOOTH_QUARTZ) : face ? s(Blocks.QUARTZ_PILLAR) : s(Blocks.WHITE_CONCRETE);
				} else {
					double d = Math.hypot(at, k - spring);
					on = d > ri && d <= ro;
					b = d < ri + 0.9 ? s(Blocks.SMOOTH_QUARTZ) : d > ro - 0.9 ? s(Blocks.BLUE_CONCRETE) : s(Blocks.WHITE_CONCRETE);
				}
				if (!on) {
					continue;
				}
				if (ruined && t > 0 && k > spring - 2 + (int) (unit(t, k, 5) * 3)) {
					continue;
				}
				int shift = lean * (k / 8);
				for (int depth = -1; depth <= 1; depth++) {
					int x = alongX ? cx + t + shift : cx + depth;
					int z = alongX ? cz + depth : cz + t + shift;
					c.set(x, y + k, z, b);
				}
			}
		}
		if (ruined) {
			for (int i = 0; i < 6; i++) {
				int t = (int) Math.round(ri + 1 + unit(i, cx, 3) * 4), off = (int) Math.round((unit(cz, i, 4) - 0.5) * 6);
				int x = alongX ? cx + t : cx + off, z = alongX ? cz + off : cz + t;
				c.set(x, y + 1, z, i % 2 == 0 ? s(Blocks.CRACKED_STONE_BRICKS) : Kit.slab(Blocks.QUARTZ_SLAB, false));
			}
		}
	}

	/** A ruined stone structure (9 x 9): broken walls of cracked stone bricks and andesite, stairs fallen along the top. */
	static void ruin(Canvas c, int x1, int z1, int y) {
		if (!c.near(x1 + 4, z1 + 4, 10)) {
			return;
		}
		for (int x = x1; x <= x1 + 8; x++) {
			for (int z = z1; z <= z1 + 8; z++) {
				boolean wall = x == x1 || x == x1 + 8 || z == z1 || z == z1 + 8;
				if (!wall) {
					c.set(x, y, z, unit(x, z, 13) < 0.5 ? s(Blocks.CRACKED_STONE_BRICKS) : s(Blocks.STONE_BRICKS));
					continue;
				}
				int hgt = 3 + (int) Math.round(9 * Math.pow(unit(x / 2, z / 2, 14), 1.5));
				if ((x == x1 + 4 || z == z1 + 4) && hgt > 3) {
					hgt = 0; // doorways
				}
				for (int k = 1; k <= hgt; k++) {
					double u = unit3(x, k, z, 15);
					c.set(x, y + k, z, u < 0.55 ? s(Blocks.CRACKED_STONE_BRICKS) : u < 0.8 ? s(Blocks.STONE_BRICKS) : s(Blocks.ANDESITE));
				}
				if (hgt > 0) {
					c.set(x, y + hgt + 1, z, Kit.stairs(Blocks.STONE_BRICK_STAIRS, x == x1 ? Direction.EAST : x == x1 + 8 ? Direction.WEST
							: z == z1 ? Direction.SOUTH : Direction.NORTH, false));
				}
			}
		}
	}
}
