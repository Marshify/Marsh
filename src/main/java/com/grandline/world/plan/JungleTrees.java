package com.grandline.world.plan;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

/**
 * Giant jungle trees, built as a complete block model first and then written chunk by chunk. Because the model is a
 * pure function of the tree's position (seeded), every chunk the tree touches writes the same blocks, so trees line up
 * across chunk borders.
 *
 * <p>Shape: a 2x2 jungle-log trunk that runs up into the crown, buttress roots, and several offset log branches that
 * climb outward. The crown is made of overlapping leaf clusters (thick in the middle, stepped and ragged at the edges):
 * one around the trunk top, a couple beside it, and one on every branch tip, so every leaf mass touches a log or
 * another cluster. Leaves are persistent (they never decay).
 */
final class JungleTrees {
	private JungleTrees() {
	}

	/** A tree's blocks in island-local coordinates. Logs win over leaves; vines only go into empty spaces. */
	static final class Model {
		final Map<Long, BlockState> blocks = new LinkedHashMap<>();
		int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;

		static long key(int x, int y, int z) {
			return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (y & 0xFFF);
		}

		static int x(long key) {
			return (int) (key >> 38) << 6 >> 6;
		}

		static int z(long key) {
			return (int) ((key >> 12) & 0x3FFFFFF) << 6 >> 6;
		}

		static int y(long key) {
			return (int) (key & 0xFFF);
		}

		boolean has(int x, int y, int z) {
			return blocks.containsKey(key(x, y, z));
		}

		boolean isLog(int x, int y, int z) {
			BlockState s = blocks.get(key(x, y, z));
			return s != null && s.is(Blocks.JUNGLE_LOG);
		}

		void log(int x, int y, int z, Direction.Axis axis) {
			put(x, y, z, Blocks.JUNGLE_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis));
		}

		void leaf(int x, int y, int z) {
			if (!has(x, y, z)) {
				put(x, y, z, LEAF);
			}
		}

		void put(int x, int y, int z, BlockState state) {
			blocks.put(key(x, y, z), state);
			minX = Math.min(minX, x);
			maxX = Math.max(maxX, x);
			minZ = Math.min(minZ, z);
			maxZ = Math.max(maxZ, z);
		}

		/** Writes the part of the tree inside the canvas's chunk. */
		void write(Canvas c) {
			for (Map.Entry<Long, BlockState> e : blocks.entrySet()) {
				long k = e.getKey();
				c.set(x(k), y(k), z(k), e.getValue());
			}
		}
	}

	static final BlockState LEAF = Kit.leaves(Blocks.JUNGLE_LEAVES);

	/** A vine holding on to the block on its {@code toward} side. */
	static BlockState sideVine(Direction toward) {
		BlockState v = Blocks.VINE.defaultBlockState();
		return switch (toward) {
			case NORTH -> v.setValue(VineBlock.NORTH, true);
			case SOUTH -> v.setValue(VineBlock.SOUTH, true);
			case EAST -> v.setValue(VineBlock.EAST, true);
			default -> v.setValue(VineBlock.WEST, true);
		};
	}

	private static long hash(int x, int y, int z) {
		long h = x * 0x9E3779B97F4A7C15L ^ y * 0xD6E8FEB86659FD93L ^ z * 0xC2B2AE3D27D4EB4FL;
		h ^= h >>> 29;
		h *= 0xBF58476D1CE4E5B9L;
		return h ^ (h >>> 32);
	}

	/** -0.5..0.5, fixed per block: roughens cluster edges. */
	private static double grain(int x, int y, int z) {
		return ((hash(x, y, z) >>> 11) & 1023) / 1023.0 - 0.5;
	}

	/**
	 * One leaf cluster: an irregular lens around (cx, cy, cz). Layers shrink above and below the middle (thick centre,
	 * stepped edges); edge blocks are dropped or added by a per-block grain so no two clusters share a silhouette.
	 */
	static void cluster(Model m, double cx, int cy, double cz, double radius, int below, int above) {
		for (int dy = -below; dy <= above; dy++) {
			double t = dy < 0 ? -dy / (below + 1.0) : dy / (above + 1.0);
			double layer = radius * (1.0 - Math.pow(t, 1.7));
			int r = (int) Math.ceil(layer + 1);
			for (int x = (int) Math.floor(cx - r); x <= (int) Math.ceil(cx + r); x++) {
				for (int z = (int) Math.floor(cz - r); z <= (int) Math.ceil(cz + r); z++) {
					double d = Math.hypot(x + 0.5 - cx, z + 0.5 - cz);
					if (d <= layer + grain(x, cy + dy, z) * 1.3) {
						m.leaf(x, cy + dy, z);
					}
				}
			}
		}
	}

	/** Log line from (x0, y0, z0) to (x1, y1, z1) that stays face-connected (no diagonal-only steps). */
	static void limb(Model m, int x0, int y0, int z0, int x1, int y1, int z1) {
		int x = x0, y = y0, z = z0;
		int steps = Math.max(Math.abs(x1 - x0), Math.max(Math.abs(z1 - z0), Math.abs(y1 - y0)));
		Direction.Axis main = Math.abs(x1 - x0) >= Math.abs(z1 - z0) ? Direction.Axis.X : Direction.Axis.Z;
		m.log(x0, y0, z0, main); // the branch's first log, against the trunk
		for (int i = 1; i <= steps; i++) {
			double t = i / (double) steps;
			int nx = (int) Math.round(x0 + (x1 - x0) * t);
			int ny = (int) Math.round(y0 + (y1 - y0) * t);
			int nz = (int) Math.round(z0 + (z1 - z0) * t);
			// Fill the corners so every log touches the previous one by a face.
			if (nx != x) {
				m.log(nx, y, z, Direction.Axis.X);
			}
			if (nz != z) {
				m.log(nx, y, nz, Direction.Axis.Z);
			}
			if (ny != y) {
				m.log(nx, ny, nz, Direction.Axis.Y);
			}
			m.log(nx, ny, nz, ny != y ? Direction.Axis.Y : main);
			x = nx;
			y = ny;
			z = nz;
		}
	}

	/**
	 * A giant jungle tree with its 2x2 trunk at (x..x+1, z..z+1) on ground {@code g}. {@code away} (radians, or NaN)
	 * points the branches away from something that must stay in view (the main trail, the temple). {@code grand} makes
	 * the boss-clearing tree: longer roots, more and longer branches, a wider crown.
	 */
	static Model giant(int x, int z, int g, int height, double away, boolean grand) {
		Model m = new Model();
		Random rng = new Random(hash(x, 7, z));
		int top = g + height;
		double cx = x + 1.0, cz = z + 1.0; // centre of the 2x2 trunk
		// Crown first (logs placed afterwards replace any leaves they pass through).
		double crownR = (grand ? 6.5 : 4.0) + rng.nextDouble() * 1.5;
		cluster(m, cx, top, cz, crownR, 2, grand ? 3 : 2);
		int extra = 1 + rng.nextInt(2);
		for (int i = 0; i < extra; i++) {
			double a = rng.nextDouble() * Math.PI * 2;
			double r = crownR * (0.55 + rng.nextDouble() * 0.25);
			cluster(m, cx + Math.cos(a) * r, top - 1 + rng.nextInt(3), cz + Math.sin(a) * r, crownR * 0.6, 1, 2);
		}
		// Branches: offset, climbing outward, each ending in its own cluster.
		int branches = grand ? 6 : 3 + rng.nextInt(3);
		double base = rng.nextDouble() * Math.PI * 2;
		for (int i = 0; i < branches; i++) {
			double a = Double.isNaN(away)
					? base + i * (Math.PI * 2 / branches) + (rng.nextDouble() - 0.5) * 0.9
					: away + (rng.nextDouble() - 0.5) * 2.0;
			int startY = g + (int) Math.round(height * (0.5 + rng.nextDouble() * 0.35));
			int length = (grand ? 5 : 3) + rng.nextInt(grand ? 4 : 3);
			int sx = (int) Math.floor(cx + Math.cos(a) * 1.2), sz = (int) Math.floor(cz + Math.sin(a) * 1.2);
			int ex = (int) Math.floor(cx + Math.cos(a) * (1.2 + length)), ez = (int) Math.floor(cz + Math.sin(a) * (1.2 + length));
			int ey = Math.min(startY + length / 2 + 1, top - 1);
			Model tip = new Model();
			cluster(tip, ex + 0.5, ey + 1, ez + 0.5, (grand ? 3.2 : 2.4) + rng.nextDouble(), 1, 2);
			tip.blocks.forEach((k, s) -> m.leaf(Model.x(k), Model.y(k), Model.z(k)));
			limb(m, sx, startY, sz, ex, ey, ez);
		}
		// Trunk: 2x2, through the crown's middle so the crown sits on it.
		for (int y = g + 1; y <= top + 1; y++) {
			for (int dx = 0; dx <= 1; dx++) {
				for (int dz = 0; dz <= 1; dz++) {
					m.log(x + dx, y, z + dz, Direction.Axis.Y);
				}
			}
		}
		// Buttress roots flaring out at the base.
		int[][] sides = {{-1, 0}, {2, 0}, {0, -1}, {0, 2}, {-1, 1}, {2, 1}, {1, -1}, {1, 2}};
		for (int[] s : sides) {
			if (rng.nextInt(3) == 0 && !grand) {
				continue;
			}
			int rh = grand ? 3 + rng.nextInt(3) : 1 + rng.nextInt(2);
			int rx = x + s[0], rz = z + s[1];
			for (int y = g; y <= g + rh; y++) {
				m.log(rx, y, rz, Direction.Axis.Y);
			}
			if (grand) {
				int ox = s[0] < 0 ? -1 : s[0] > 1 ? 1 : 0, oz = s[1] < 0 ? -1 : s[1] > 1 ? 1 : 0;
				for (int y = g; y <= g + rh / 2; y++) {
					m.log(rx + ox, y, rz + oz, ox != 0 ? Direction.Axis.X : Direction.Axis.Z);
				}
			}
		}
		prune(m, x, g + 1, z);
		addVines(m, x, z, g, cx, cz, crownR);
		return m;
	}

	/**
	 * A medium jungle tree: 1x1 trunk into an irregular crown of one or two overlapping clusters, sometimes a short
	 * branch carrying a third, with vines hanging from the crown's sides.
	 */
	static Model medium(int x, int z, int g, int height) {
		Model m = new Model();
		Random rng = new Random(hash(x, 3, z));
		int top = g + height;
		double r = 2.4 + rng.nextDouble() * 1.1;
		cluster(m, x + 0.5, top, z + 0.5, r, 1, 2);
		if (rng.nextBoolean()) {
			double a = rng.nextDouble() * Math.PI * 2;
			cluster(m, x + 0.5 + Math.cos(a) * r * 0.7, top + rng.nextInt(2), z + 0.5 + Math.sin(a) * r * 0.7, r * 0.7, 1, 1);
		}
		if (height >= 7 && rng.nextInt(3) == 0) {
			double a = rng.nextDouble() * Math.PI * 2;
			int ex = x + (int) Math.round(Math.cos(a) * 3), ez = z + (int) Math.round(Math.sin(a) * 3), ey = top - 2;
			cluster(m, ex + 0.5, ey + 1, ez + 0.5, 1.8, 1, 1);
			limb(m, x, top - 4, z, ex, ey, ez);
		}
		for (int y = g + 1; y <= top; y++) {
			m.log(x, y, z, Direction.Axis.Y);
		}
		prune(m, x, g + 1, z);
		crownVines(m, 12);
		return m;
	}

	/** Drops any block that is not face-connected to the trunk (e.g. a lone edge leaf), so nothing floats. */
	private static void prune(Model m, int x, int y, int z) {
		java.util.Set<Long> reached = new java.util.HashSet<>();
		java.util.ArrayDeque<Long> queue = new java.util.ArrayDeque<>();
		long start = Model.key(x, y, z);
		reached.add(start);
		queue.add(start);
		int[][] dirs = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
		while (!queue.isEmpty()) {
			long k = queue.poll();
			for (int[] d : dirs) {
				long n = Model.key(Model.x(k) + d[0], Model.y(k) + d[1], Model.z(k) + d[2]);
				if (m.blocks.containsKey(n) && reached.add(n)) {
					queue.add(n);
				}
			}
		}
		m.blocks.keySet().retainAll(reached);
	}

	/**
	 * Vines hanging beside the crown's outer leaves, attached to the leaf's side like vanilla jungle trees: the top vine
	 * holds on to the leaf, each vine below holds on to the one above (same face), so a later block update never strips
	 * them.
	 */
	private static void addVines(Model m, int x, int z, int g, double cx, double cz, double crownR) {
		crownVines(m, 9);
		trunkVinesOn(m, x, z, g);
	}

	/** Side-attached strands beside about one in {@code rarity} outer leaf faces. */
	private static void crownVines(Model m, int rarity) {
		java.util.List<int[]> strands = new java.util.ArrayList<>();
		Direction[] sides = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
		for (Map.Entry<Long, BlockState> e : m.blocks.entrySet()) {
			if (e.getValue() != LEAF) {
				continue;
			}
			long k = e.getKey();
			int lx = Model.x(k), ly = Model.y(k), lz = Model.z(k);
			for (Direction d : sides) {
				int vx = lx + d.getStepX(), vz = lz + d.getStepZ();
				if (m.has(vx, ly, vz) || m.has(vx, ly - 1, vz) || Math.floorMod(hash(vx, ly, vz), rarity) != 0) {
					continue;
				}
				strands.add(new int[]{vx, ly, vz, d.getOpposite().ordinal(), 2 + Math.floorMod(hash(vz, ly, vx), 4)});
			}
		}
		for (int[] st : strands) {
			BlockState v = sideVine(Direction.from3DDataValue(st[3]));
			for (int i = 0; i < st[4]; i++) {
				int y = st[1] - i;
				if (m.has(st[0], y, st[2])) {
					break;
				}
				m.put(st[0], y, st[2], v);
			}
		}
	}

	/** Vines on the four faces of a 2x2 trunk, low down. */
	private static void trunkVinesOn(Model m, int x, int z, int g) {
		for (int y = g + 2; y < g + 12; y++) {
			int[][] faces = {{x - 1, z, 0}, {x - 1, z + 1, 0}, {x + 2, z, 1}, {x + 2, z + 1, 1}, {x, z - 1, 2}, {x + 1, z - 1, 2}, {x, z + 2, 3}, {x + 1, z + 2, 3}};
			for (int[] f : faces) {
				if (m.has(f[0], y, f[1]) || Math.floorMod(hash(f[0], y, f[1]), 3) != 0) {
					continue;
				}
				m.put(f[0], y, f[1], sideVine(f[2] == 0 ? Direction.EAST : f[2] == 1 ? Direction.WEST : f[2] == 2 ? Direction.SOUTH : Direction.NORTH));
			}
		}
	}
}
