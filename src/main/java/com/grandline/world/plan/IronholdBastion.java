package com.grandline.world.plan;

import com.grandline.world.plan.Anchor.Type;
import com.grandline.world.plan.Terrain.Ground;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;

/**
 * Ironhold Bastion (layout 20): a fortified maritime settlement, built to the owner's specification
 * (docs/IRONHOLD_BASTION.md). A white fortress of three great towers on a raised terrace to the north; a timber-framed
 * town around an octagon-patterned plaza to the south; a pale stone dock. Island-local coordinates: +x east, +z south.
 * <pre>
 *  piece                    where (local)                         y
 *  fortress terrace          x -44..44, z -54..7 (chamfered)       top 70 (walk 71)
 *  enclosure wall            8-sided, centre line POLY              71-78, walk 79, merlons 79-80
 *  great towers              W (-21,-22) E (21,-22): base 21       71-104 (+ flags)
 *                            central (0,-33): base 23              71-111 (+ flag)
 *  small towers              SMALL (six)                           71-89
 *  main gate                 x -3..3 in the south wall              71-79 open, posts to 84
 *  side gates                x = +-36, z -11..-7                   71-77 open
 *  town                      x -40..40, z 8..48 (chamfered)        top 65 (walk 66)
 *  staircase                 x -6..6, z 8..12                      66-70
 *  plaza / octagon           x -16..16, z 13..34 / centre (0,24)   65
 *  dock                      x -2..2, z 49..60; prongs to the sides 65
 * </pre>
 * Worlds made before layout 20 keep the original Ironhold Bastion.
 */
public final class IronholdBastion extends BasePlan {
	static final int T = 65, F = 70, SEA = 62;
	/** Enclosure wall centre line (closed polygon), clockwise from the south-west corner. */
	static final int[][] POLY = {{-31, 0}, {31, 0}, {36, -6}, {36, -36}, {26, -47}, {-26, -47}, {-36, -36}, {-36, -6}};
	/** Great towers: x, z, base radius, top radius, shaft top y, observation levels, dome radius. */
	static final double[][] GREAT = {{-21, -22, 10.5, 8, 95}, {21, -22, 10.5, 8, 95}, {0, -33, 11.5, 8.5, 97}};
	/** Small towers: x, z, and whether a cannon stands on top. */
	static final int[][] SMALL = {{-31, 0, 1}, {31, 0, 1}, {-36, -28, 0}, {36, -28, 0}, {-26, -47, 1}, {26, -47, 1}};
	/** Town buildings: x1, z1, x2, z2, roof (0 orange, 1 blue, 2 brown, 3 green), wall material, front (0 E, 1 W, 2 N). */
	static final int[][] HOUSES = {
			{-33, 12, -19, 21, 0, 0, 0}, {-32, 24, -21, 32, 1, 1, 0},
			{-31, 36, -20, 44, 2, 2, 2}, {-15, 37, -7, 44, 3, 3, 2},
			{19, 12, 33, 21, 3, 1, 1}, {21, 24, 32, 32, 2, 0, 1},
			{20, 36, 31, 44, 1, 3, 2}, {7, 37, 15, 44, 0, 2, 2}};
	/** Wooden watch platforms (north-west corner of the 6 x 6), and whether a cannon stands on it. */
	static final int[][] WATCH = {{-38, 36, 0}, {33, 36, 1}};
	/** Blocky shade trees: x, z. */
	static final int[][] TREES = {{-50, -30}, {-55, 2}, {-47, 16}, {-51, -42}, {50, -30}, {55, 2}, {47, 16}, {51, -42}, {-37, 29}, {37, 29}};
	static final int OCT_X = 0, OCT_Z = 24;

	IronholdBastion() {
		super("Ironhold Bastion", 0.07);
		terrain.platform(-44, -54, 44, 7, 8, F, Ground.STONE)       // the fortress terrace
				.platform(-40, 8, 40, 48, 8, T, Ground.STONE)        // the town
				.mass(-48, -10, 11, 34, 6, 2.4, Ground.GRASS)        // west grassy landform
				.mass(48, -10, 11, 34, 6, 2.4, Ground.GRASS)         // east grassy landform
				.mass(-47, -42, 7, 9, 4, 1.6, Ground.STONE)          // rocky shoulders at the rear corners
				.mass(47, -42, 7, 9, 4, 1.6, Ground.STONE)
				.mass(-40, 30, 7, 10, 3, 1.4, Ground.GRASS)
				.mass(40, 30, 7, 10, 3, 1.4, Ground.GRASS)
				.mass(0, -57, 42, 6, 2.5, 1.2, Ground.STONE)         // the rugged north coast
				.mass(-30, -58, 8, 5, 5, 1.3, Ground.STONE)
				.mass(12, -59, 9, 5, 6, 1.3, Ground.STONE)
				.mass(36, -57, 6, 4, 4, 1.3, Ground.STONE)
				.shoreMix(s(Blocks.STONE), s(Blocks.ANDESITE), s(Blocks.TUFF), s(Blocks.GRAVEL), s(Blocks.STONE))
				.reach(66);
		anchor(new Anchor(Type.ARRIVAL, 0, T + 1, 56, 0, 180));      // ships put players on the dock, facing the town
		anchor(Anchor.at(Type.QUEST, 5, T + 1, 15, 0));              // at the foot of the staircase
		anchor(Anchor.at(Type.HOME, -6, T + 1, 15, 0));
		anchor(Anchor.at(Type.FISHERMAN, 5, T + 1, 59, 90));
		anchor(Anchor.at(Type.DOCK, 9, 63, 57));
		anchor(Anchor.area(Type.ENEMY_CAMP, -51, -10, 6));           // west landform
		anchor(Anchor.area(Type.ENEMY_CAMP, 51, -10, 6));            // east landform
		anchor(Anchor.area(Type.BOSS_ARENA, 0, -9, 6));              // the fortress courtyard
	}

	// ============================================================================================== geometry

	static double unit(int x, int z, int salt) {
		return CloudtopIslesV7.unit(x, z, salt);
	}

	/** Distance from (x, z) to the enclosure's centre line. */
	static double wallDistance(double x, double z) {
		double best = Double.MAX_VALUE;
		for (int i = 0; i < POLY.length; i++) {
			int[] a = POLY[i], b = POLY[(i + 1) % POLY.length];
			best = Math.min(best, segment(x, z, a[0], a[1], b[0], b[1]));
		}
		return best;
	}

	static double segment(double px, double pz, double ax, double az, double bx, double bz) {
		double vx = bx - ax, vz = bz - az, len = vx * vx + vz * vz;
		double t = len == 0 ? 0 : Math.max(0, Math.min(1, ((px - ax) * vx + (pz - az) * vz) / len));
		return Math.hypot(px - (ax + t * vx), pz - (az + t * vz));
	}

	/** Inside the enclosure's centre line. */
	static boolean inside(double x, double z) {
		boolean in = false;
		for (int i = 0, j = POLY.length - 1; i < POLY.length; j = i++) {
			double xi = POLY[i][0], zi = POLY[i][1], xj = POLY[j][0], zj = POLY[j][1];
			if ((zi > z) != (zj > z) && x < (xj - xi) * (z - zi) / (zj - zi) + xi) {
				in = !in;
			}
		}
		return in;
	}

	/** The wall is 3 blocks thick: cells (measured at their centres) within 1 block of the centre line. */
	static boolean wallCell(int x, int z) {
		return wallDistance(x, z) <= 1.1;
	}

	/** Inside the wall ring: the courtyard (and the great towers' ground). */
	static boolean courtyard(int x, int z) {
		return inside(x, z) && !wallCell(x, z);
	}

	static boolean onTerrace(int x, int z) {
		return new Terrain.Platform(-44, -54, 44, 7, 8, F, Ground.STONE).contains(x, z);
	}

	static boolean inTown(int x, int z) {
		return new Terrain.Platform(-40, 8, 40, 48, 8, T, Ground.STONE).contains(x, z);
	}

	/** The cardinal direction pointing from the centre (cx, cz) out through (x, z). */
	static Direction outward(double x, double z, double cx, double cz) {
		double dx = x - cx, dz = z - cz;
		return Math.abs(dx) >= Math.abs(dz) ? (dx > 0 ? Direction.EAST : Direction.WEST) : (dz > 0 ? Direction.SOUTH : Direction.NORTH);
	}

	/** Runs {@code body} for every island-local column of the chunk being generated. */
	static void columns(Canvas c, java.util.function.BiConsumer<Integer, Integer> body) {
		var box = c.chunkBox();
		var o = c.world(0, 0, 0);
		int x0 = box.minX() - o.getX(), z0 = box.minZ() - o.getZ();
		for (int x = x0; x < x0 + 16; x++) {
			for (int z = z0; z < z0 + 16; z++) {
				body.accept(x, z);
			}
		}
	}

	static BlockState log(Block b, Direction.Axis axis) {
		return s(b).setValue(RotatedPillarBlock.AXIS, axis);
	}

	static BlockState stair(Block b, Direction facing, boolean top) {
		return Kit.stairs(b, facing, top);
	}

	static BlockState slab(Block b, boolean top) {
		return Kit.slab(b, top);
	}

	// ============================================================================================== build

	@Override
	public void build(Canvas c) {
		grounds(c);
		walls(c);
		for (double[] g : GREAT) {
			if (c.near((int) g[0], (int) g[1], 14)) {
				greatTower(c, g);
			}
		}
		for (int[] t : SMALL) {
			if (c.near(t[0], t[1], 8)) {
				smallTower(c, t[0], t[1], t[2] == 1);
			}
		}
		mainGate(c);
		sideGate(c, -1);
		sideGate(c, 1);
		postern(c);
		staircase(c);
		octagon(c);
		for (int[] h : HOUSES) {
			if (c.near((h[0] + h[2]) / 2, (h[1] + h[3]) / 2, 14)) {
				house(c, h);
			}
		}
		dock(c);
		for (int[] w : WATCH) {
			if (c.near(w[0] + 3, w[1] + 3, 8)) {
				watchPlatform(c, w[0], w[1], w[2] == 1);
			}
		}
		landscape(c);
	}

	// ============================================================================================== grounds

	/** Courtyard paving: a mostly even grey-beige stone floor, soft patches of andesite, a smooth stone grid. */
	static BlockState courtyardPaving(int x, int z) {
		if (Math.floorMod(x, 9) == 0 || Math.floorMod(z, 9) == 0) {
			return s(Blocks.SMOOTH_STONE);
		}
		double v = (Math.sin(x * 0.21) + Math.sin(z * 0.17 + 1.3) + Math.sin((x + z) * 0.09)) / 3.0;
		return v > 0.45 ? s(Blocks.ANDESITE) : v < -0.55 ? s(Blocks.COBBLESTONE) : s(Blocks.STONE);
	}

	private void grounds(Canvas c) {
		columns(c, (x, z) -> {
			if (onTerrace(x, z)) {
				BlockState top;
				if (courtyard(x, z)) {
					top = wallDistance(x, z) < 2.2 ? s(Blocks.SMOOTH_STONE) : courtyardPaving(x, z);
				} else if (z >= 2 && Math.abs(x) <= 6) {
					top = Math.abs(x) == 6 ? s(Blocks.SMOOTH_STONE) : courtyardPaving(x, z); // the broad way to the gate
				} else if (z <= -49 && x >= 8 && x <= 18) {
					top = s(Blocks.SMOOTH_STONE); // the rear battery walk outside the postern
				} else if (z >= -11 && z <= -7 && Math.abs(x) >= 38) {
					top = s(Blocks.COARSE_DIRT); // paths out of the side gates
				} else {
					top = s(Blocks.GRASS_BLOCK);
				}
				c.set(x, F, z, top);
				if (z == 7 && Math.abs(x) <= 40) {
					// The white retaining wall facing the town, with a low parapet (open at the staircase).
					c.fill(x, T + 1, z, x, F - 1, z, s(Blocks.SMOOTH_QUARTZ));
					c.set(x, F, z, Math.abs(x) <= 6 ? s(Blocks.SMOOTH_QUARTZ) : s(Blocks.POLISHED_DIORITE));
					if (Math.abs(x) > 7) {
						c.set(x, F + 1, z, slab(Blocks.SMOOTH_QUARTZ_SLAB, false));
					}
				}
			} else if (inTown(x, z)) {
				c.set(x, T, z, townGround(x, z));
				if (z == 48) {
					// The developed waterfront: a white retaining wall down to the sea, with a parapet (open at the dock).
					c.fill(x, SEA - 2, z, x, T - 1, z, s(Blocks.SMOOTH_QUARTZ));
					c.set(x, T, z, s(Blocks.POLISHED_DIORITE));
					if (Math.abs(x) > 2) {
						c.set(x, T + 1, z, slab(Blocks.SMOOTH_QUARTZ_SLAB, false));
					}
				}
			}
		});
	}

	/** The town's ground: plaza, streets, the dock road, the promenade, else grass. */
	static BlockState townGround(int x, int z) {
		int ax = Math.abs(x);
		if (z >= 13 && z <= 34 && ax <= 16) {
			return ax == 16 || z == 13 || z == 34 ? s(Blocks.STONE_BRICKS)
					: Math.floorMod(x, 4) == 0 || Math.floorMod(z, 4) == 0 ? s(Blocks.SMOOTH_STONE) : s(Blocks.POLISHED_ANDESITE);
		}
		if (z >= 45) {
			return Math.floorMod(x, 5) == 0 ? s(Blocks.STONE) : s(Blocks.POLISHED_ANDESITE); // the promenade
		}
		if (z == 12 && ax <= 7) {
			return s(Blocks.SMOOTH_STONE); // the foot of the staircase
		}
		boolean road = (z >= 35 && ax <= 2) || (z == 35 && ax <= 18) || (z >= 12 && z <= 35 && (ax == 17 || ax == 18));
		if (road) {
			double v = unit(x, z, 61);
			return v < 0.45 ? s(Blocks.COARSE_DIRT) : v < 0.8 ? s(Blocks.PACKED_MUD) : s(Blocks.STONE);
		}
		if (z >= 35 && ax == 3) {
			return s(Blocks.MUD_BRICKS); // the dock road's edging
		}
		return s(Blocks.GRASS_BLOCK);
	}

	// ============================================================================================== walls

	/** The red brick enclosure with its white battlements, following the fortress grounds. */
	private void walls(Canvas c) {
		columns(c, (x, z) -> {
			if (!onTerrace(x, z) || !wallCell(x, z)) {
				return;
			}
			double d = wallDistance(x, z);
			boolean outer = d >= 0.9 && !inside(x, z);
			c.set(x, F, z, s(Blocks.POLISHED_GRANITE));
			for (int y = F + 1; y <= F + 6; y++) {
				BlockState b = y == F + 1 ? s(Blocks.POLISHED_GRANITE)
						: unit(x * 7 + y, z, 17) < 0.08 ? s(Blocks.GRANITE) : s(Blocks.BRICKS);
				c.set(x, y, z, b);
			}
			c.set(x, F + 7, z, s(Blocks.WHITE_CONCRETE));
			c.set(x, F + 8, z, s(Blocks.SMOOTH_QUARTZ));
			if (outer && Math.floorMod(Math.floorDiv(x + z, 2), 2) == 0) {
				c.set(x, F + 9, z, s(Blocks.WHITE_CONCRETE));   // merlon
				c.set(x, F + 10, z, s(Blocks.WHITE_CONCRETE));
				c.set(x, F + 11, z, slab(Blocks.STONE_BRICK_SLAB, false));
			} else {
				c.set(x, F + 9, z, slab(Blocks.SMOOTH_QUARTZ_SLAB, false)); // the wall walk
			}
		});
		// Blue and white naval banners on the outer faces.
		for (int[] b : new int[][]{{-17, 2, 0}, {17, 2, 0}, {-25, 2, 0}, {25, 2, 0}, {-38, -20, 1}, {38, -20, 2}, {-38, -40, 1}, {38, -40, 2}}) {
			Direction facing = b[2] == 0 ? Direction.SOUTH : b[2] == 1 ? Direction.WEST : Direction.EAST;
			RedbrickPort.crestBanner(c, b[0], F + 6, b[1], facing);
		}
	}

	// ============================================================================================== towers

	/** Exterior of a tower shaft: smooth quartz, a little calcite, white concrete just under the observation level. */
	static BlockState shaftMaterial(int x, int y, int z, int top) {
		if (y >= top - 1) {
			return s(Blocks.WHITE_CONCRETE);
		}
		return unit(x * 13 + y, z, 29) < 0.03 ? s(Blocks.CALCITE) : s(Blocks.SMOOTH_QUARTZ);
	}

	/**
	 * A great tower: a white shaft in stepped circular tiers (each about a block narrower, with a diorite ledge ring),
	 * staggered small dark recessed windows, blue observation level(s) with continuous windows, a rounded burgundy dome
	 * and a small blue flag.
	 */
	private void greatTower(Canvas c, double[] g) {
		int cx = (int) g[0], cz = (int) g[1];
		double rb = g[2], rt = g[3];
		int top = (int) g[4];
		boolean central = top == 97;
		int y0 = F + 1;
		// Tier radii, one block apart, from the base radius down to just above the top radius.
		int tiers = Math.max(2, (int) Math.floor(rb - rt)); // W/E: 21, 19, 17 across; central: 23, 21, 19, 17
		int start = y0 + 2, span = top - start + 1;
		int reach = (int) Math.ceil(rb) + 2;
		for (int x = cx - reach; x <= cx + reach; x++) {
			for (int z = cz - reach; z <= cz + reach; z++) {
				if (!c.inChunk(x, z)) {
					continue;
				}
				double dx = x - cx, dz = z - cz, d = Math.hypot(dx, dz);
				double angle = Math.atan2(dz, dx);
				// Plinth: two stepped diorite rings at the foot.
				if (d <= rb + 1) {
					c.set(x, y0, z, s(Blocks.POLISHED_DIORITE));
				}
				if (d <= rb + 0.5) {
					c.set(x, y0 + 1, z, s(Blocks.POLISHED_DIORITE));
				}
				for (int y = start; y <= top; y++) {
					int tier = Math.min(tiers, (y - start) * (tiers + 1) / span);
					double r = rb - tier;
					double below = tier == 0 ? r : rb - (tier - 1);
					boolean tierStart = tier > 0 && (y - 1 - start) * (tiers + 1) / span < tier;
					if (d > r) {
						if (tierStart && d <= below) {
							c.set(x, y, z, slab(Blocks.POLISHED_DIORITE_SLAB, false)); // the ledge ring of each step
						}
						continue;
					}
					// Staggered small windows: two rows per tier, every ~6 blocks round, alternate rows offset; some 1 x 2.
					// Recessed: the outer cell open, dark blocks behind it.
					int k = y - start;
					int row = k / 4;
					boolean windowRow = k % 4 == 2 || (k % 4 == 3 && row % 3 == 1);
					double arc = angle * r + (row % 2) * 3.0 + 100;
					boolean windowCol = Math.floorMod((int) Math.floor(arc), 6) == 0;
					if (windowRow && windowCol && y <= top - 3 && d > r - 2.4) {
						c.set(x, y, z, d > r - 0.8 ? s(Blocks.AIR) : row % 2 == 0 ? s(Blocks.BLACK_CONCRETE) : s(Blocks.BLACK_STAINED_GLASS));
						continue;
					}
					if (d <= r - 1.6) {
						c.set(x, y, z, s(Blocks.SMOOTH_QUARTZ)); // the solid core
						continue;
					}
					c.set(x, y, z, shaftMaterial(x, y, z, top));
				}
			}
		}
		int y = top + 1;
		if (central) {
			y = observation(c, cx, cz, y, 8.5, 4);              // lower observation level
			ring(c, cx, cz, y, 9.0, s(Blocks.POLISHED_DIORITE), s(Blocks.SMOOTH_QUARTZ));
			y = observation(c, cx, cz, y + 1, 7.5, 5);          // upper observation level
			dome(c, cx, cz, y, 7.5);
		} else {
			y = observation(c, cx, cz, y, 8.0, 5);
			dome(c, cx, cz, y, 8.0);
		}
	}

	/** A solid disc: {@code edge} on its outer ring, {@code fill} inside. */
	private static void ring(Canvas c, int cx, int cz, int y, double r, BlockState edge, BlockState fill) {
		int reach = (int) Math.ceil(r);
		for (int x = cx - reach; x <= cx + reach; x++) {
			for (int z = cz - reach; z <= cz + reach; z++) {
				double d = Math.hypot(x - cx, z - cz);
				if (d <= r) {
					c.set(x, y, z, d > r - 1 ? edge : fill);
				}
			}
		}
	}

	/**
	 * A blue observation level {@code height} tall from {@code y}: a diorite slab lip, a cyan sill, a continuous band of
	 * blue-framed windows (blue concrete mullions, light blue and blue glass), a blue cornice; floor, ceiling and a hidden
	 * light inside. Returns the first y above it.
	 */
	private static int observation(Canvas c, int cx, int cz, int y, double r, int height) {
		int reach = (int) Math.ceil(r) + 1;
		int topY = y + height - 1;
		for (int x = cx - reach; x <= cx + reach; x++) {
			for (int z = cz - reach; z <= cz + reach; z++) {
				if (!c.inChunk(x, z)) {
					continue;
				}
				double dx = x - cx, dz = z - cz, d = Math.hypot(dx, dz);
				if (d > r && d <= r + 1) {
					c.set(x, y, z, slab(Blocks.POLISHED_DIORITE_SLAB, false)); // the lip
					continue;
				}
				if (d > r) {
					continue;
				}
				boolean edge = d > r - 1;
				for (int yy = y; yy <= topY; yy++) {
					BlockState b;
					if (!edge) {
						b = yy == y || yy == topY ? s(Blocks.SMOOTH_QUARTZ) : s(Blocks.AIR); // floor, room, ceiling
					} else if (yy == y) {
						b = s(Blocks.CYAN_CONCRETE);
					} else if (yy == topY) {
						b = s(Blocks.BLUE_CONCRETE);
					} else {
						double arc = Math.atan2(dz, dx) * r + 100;
						boolean mullion = Math.floorMod((int) Math.floor(arc), 3) == 0;
						b = mullion ? s(Blocks.BLUE_CONCRETE) : yy == y + (height - 1) / 2 ? s(Blocks.BLUE_STAINED_GLASS)
								: s(Blocks.LIGHT_BLUE_STAINED_GLASS);
					}
					c.set(x, yy, z, b);
				}
				if (d <= 1) {
					c.set(x, topY, z, s(Blocks.GLOWSTONE)); // concealed in the ceiling, glowing through the windows
				}
			}
		}
		return topY + 1;
	}

	/** A rounded burgundy dome in shrinking circular layers (stairs round each edge), with a flag on top. */
	private static void dome(Canvas c, int cx, int cz, int y, double r) {
		double[] layers = {r, r - 1, r - 2.5, r - 4.5};
		for (int i = 0; i < layers.length; i++) {
			double rl = layers[i];
			int reach = (int) Math.ceil(rl);
			for (int x = cx - reach; x <= cx + reach; x++) {
				for (int z = cz - reach; z <= cz + reach; z++) {
					double dx = x - cx, dz = z - cz, d = Math.hypot(dx, dz);
					if (d > rl) {
						continue;
					}
					BlockState b;
					if (d > rl - 1) {
						Direction in = outward(cx, cz, x, z); // toward the centre
						b = Kit.stairs(Blocks.RED_NETHER_BRICK_STAIRS, in, false);
					} else {
						double u = unit(x, z, 41 + i);
						b = u < 0.06 ? s(Blocks.RED_TERRACOTTA) : u < 0.1 ? s(Blocks.CRIMSON_PLANKS) : s(Blocks.RED_NETHER_BRICKS);
					}
					c.set(x, y + i, z, b);
				}
			}
		}
		int capY = y + layers.length;
		c.fill(cx - 1, capY, cz, cx + 1, capY, cz, slab(Blocks.RED_NETHER_BRICK_SLAB, false));
		c.fill(cx, capY, cz - 1, cx, capY, cz + 1, slab(Blocks.RED_NETHER_BRICK_SLAB, false));
		c.set(cx, capY, cz, s(Blocks.NETHER_BRICKS));
		flag(c, cx, capY + 1, cz, 5);
	}

	/** A slim flagpole {@code height} tall with a small blue swallowtail pennant. */
	static void flag(Canvas c, int x, int y, int z, int height) {
		c.fill(x, y, z, x, y + height - 2, z, Kit.connected(Blocks.IRON_BARS, true).setValue(net.minecraft.world.level.block.IronBarsBlock.EAST, false)
				.setValue(net.minecraft.world.level.block.IronBarsBlock.WEST, false));
		c.set(x, y + height - 1, z, s(Blocks.LIGHTNING_ROD));
		int fy = y + height - 2;
		c.set(x + 1, fy, z, s(Blocks.BLUE_WOOL));
		c.set(x + 2, fy, z, s(Blocks.BLUE_WOOL));
		c.set(x + 3, fy, z, s(Blocks.BLUE_WOOL));
		c.set(x + 1, fy - 1, z, s(Blocks.BLUE_WOOL));
		c.set(x + 2, fy - 1, z, s(Blocks.BLUE_CONCRETE));
		c.set(x + 1, fy - 2, z, s(Blocks.BLUE_WOOL));
		c.set(x + 2, fy - 2, z, s(Blocks.BLUE_WOOL));
		c.set(x + 3, fy - 2, z, s(Blocks.BLUE_WOOL));
	}

	/**
	 * A small defensive tower: a slightly tapered white shaft with narrow 1 x 3 window slits, a diorite corbel, a round
	 * platform with crenellations, and (some) a decorative naval cannon aimed out to sea.
	 */
	private void smallTower(Canvas c, int cx, int cz, boolean cannon) {
		int y0 = F + 1, top = F + 16;
		for (int x = cx - 6; x <= cx + 6; x++) {
			for (int z = cz - 6; z <= cz + 6; z++) {
				if (!c.inChunk(x, z)) {
					continue;
				}
				double dx = x - cx, dz = z - cz, d = Math.hypot(dx, dz);
				for (int y = y0; y <= top; y++) {
					double r = y - y0 < 8 ? 4.5 : 4.0;
					if (d > r) {
						continue;
					}
					int course = y - y0;
					boolean edge = d > r - 1;
					boolean slit = edge && (dx == 0 || dz == 0) && ((course >= 4 && course <= 6) || (course >= 10 && course <= 12));
					if (slit) {
						c.set(x, y, z, d > r - 0.8 ? s(Blocks.AIR) : s(Blocks.BLACK_CONCRETE));
						continue;
					}
					c.set(x, y, z, !edge ? s(Blocks.SMOOTH_QUARTZ) : course == 0 ? s(Blocks.POLISHED_DIORITE)
							: course % 5 == 2 ? s(Blocks.WHITE_CONCRETE) : unit(x + y * 5, z, 33) < 0.07 ? s(Blocks.CALCITE) : s(Blocks.SMOOTH_QUARTZ));
				}
				// The battlement: a diorite corbel, the platform, then merlons with stone brick slab caps.
				if (d <= 5.5) {
					c.set(x, top + 1, z, d > 4 ? s(Blocks.POLISHED_DIORITE) : s(Blocks.SMOOTH_QUARTZ));
					c.set(x, top + 2, z, s(Blocks.SMOOTH_QUARTZ));
					if (d > 4.5) {
						double arc = Math.atan2(dz, dx) * 5.5 + 100;
						if (Math.floorMod((int) Math.floor(arc), 2) == 0) {
							c.set(x, top + 3, z, s(Blocks.WHITE_CONCRETE));
							c.set(x, top + 4, z, slab(Blocks.STONE_BRICK_SLAB, false));
						}
					}
				}
			}
		}
		c.set(cx, top + 1, cz, s(Blocks.GLOWSTONE)); // a hidden light under the platform floor
		if (cannon) {
			Direction out = outward(cx, cz, 0, -20);
			cannon(c, cx - out.getStepX(), top + 3, cz - out.getStepZ(), out);
		}
	}

	/**
	 * The decorative naval cannon (4 long, aimed {@code dir}): a polished blackstone stair at the back, an anvil body with
	 * iron trapdoor wheels, and a two-block polished blackstone wall barrel on a blackstone slab. Purely decorative: the
	 * adventure rules keep players from using the anvil.
	 */
	static void cannon(Canvas c, int x, int y, int z, Direction dir) {
		int dx = dir.getStepX(), dz = dir.getStepZ();
		Direction side = dir.getClockWise();
		c.set(x - dx, y, z - dz, Kit.stairs(Blocks.POLISHED_BLACKSTONE_STAIRS, dir.getOpposite(), false));
		c.set(x, y, z, s(Blocks.ANVIL).setValue(net.minecraft.world.level.block.AnvilBlock.FACING, side));
		for (int k : new int[]{-1, 1}) {
			Direction face = k < 0 ? side.getOpposite() : side;
			c.set(x + side.getStepX() * k, y, z + side.getStepZ() * k, s(Blocks.IRON_TRAPDOOR).setValue(TrapDoorBlock.FACING, face)
					.setValue(TrapDoorBlock.OPEN, true));
		}
		BlockState barrel = wall(Blocks.POLISHED_BLACKSTONE_WALL, dir.getAxis());
		c.set(x + dx, y, z + dz, barrel);
		c.set(x + 2 * dx, y, z + 2 * dz, barrel);
		c.set(x + dx, y - 1, z + dz, slab(Blocks.BLACKSTONE_SLAB, true));
	}

	/** A wall block joined along {@code axis} (a barrel or rail), with no post on top. */
	static BlockState wall(Block block, Direction.Axis axis) {
		var st = s(block).setValue(net.minecraft.world.level.block.WallBlock.UP, false);
		var low = net.minecraft.world.level.block.state.properties.WallSide.LOW;
		return axis == Direction.Axis.X
				? st.setValue(net.minecraft.world.level.block.WallBlock.EAST_WALL, low).setValue(net.minecraft.world.level.block.WallBlock.WEST_WALL, low)
				: st.setValue(net.minecraft.world.level.block.WallBlock.NORTH_WALL, low).setValue(net.minecraft.world.level.block.WallBlock.SOUTH_WALL, low);
	}

	// ============================================================================================== gates

	/** A round white gatepost (3 across), {@code height} tall from {@code y0}, crowned with a little crenellated ring. */
	private static void gatepost(Canvas c, int cx, int cz, int y0, int height) {
		int topY = y0 + height - 1;
		for (int x = cx - 2; x <= cx + 2; x++) {
			for (int z = cz - 2; z <= cz + 2; z++) {
				double d = Math.hypot(x - cx, z - cz);
				if (d <= 1.2) {
					for (int y = y0; y <= topY; y++) {
						int k = y - y0;
						c.set(x, y, z, k == 0 ? s(Blocks.POLISHED_DIORITE) : k % 4 == 0 ? s(Blocks.CALCITE) : s(Blocks.SMOOTH_QUARTZ));
					}
				}
				if (d <= 2.3) {
					c.set(x, topY + 1, z, d > 1.2 ? s(Blocks.POLISHED_DIORITE) : s(Blocks.SMOOTH_QUARTZ));
					if (d > 1.2 && Math.floorMod(x + z, 2) == 0) {
						c.set(x, topY + 2, z, s(Blocks.SMOOTH_QUARTZ));
					}
				}
			}
		}
	}

	/**
	 * One open golden door leaf lying against the inner face of the wall: stripped birch edges, birch planks, yellow
	 * terracotta bands, dark oak button rivets and birch trapdoor panels on the face toward {@code face}; its far top
	 * corner rounded. The leaf runs from (x, z) along {@code along} for {@code width} cells, {@code height} tall.
	 */
	private static void doorLeaf(Canvas c, int x, int z, Direction along, int width, int height, Direction face) {
		int y0 = F + 1, top = y0 + height - 1;
		for (int i = 0; i < width; i++) {
			int px = x + along.getStepX() * i, pz = z + along.getStepZ() * i;
			boolean edge = i == 0 || i == width - 1;
			for (int y = y0; y <= top; y++) {
				if (i == width - 1 && y == top) {
					c.set(px, y, pz, s(Blocks.AIR)); // the rounded upper corner
					continue;
				}
				int k = y - y0;
				boolean band = k == 2 || k == height - 2;
				c.set(px, y, pz, edge ? log(Blocks.STRIPPED_BIRCH_LOG, Direction.Axis.Y) : band ? s(Blocks.YELLOW_TERRACOTTA) : s(Blocks.BIRCH_PLANKS));
				int fx = px + face.getStepX(), fz = pz + face.getStepZ();
				if (!edge && band) {
					c.set(fx, y, fz, s(Blocks.DARK_OAK_BUTTON).setValue(ButtonBlock.FACE, AttachFace.WALL)
							.setValue(ButtonBlock.FACING, face)); // rivets
				} else if (!edge && k == height / 2) {
					c.set(fx, y, fz, s(Blocks.BIRCH_TRAPDOOR).setValue(TrapDoorBlock.FACING, face).setValue(TrapDoorBlock.OPEN, true));
				}
			}
			if (i == width - 2) {
				c.set(px, top, pz, Kit.stairs(Blocks.BIRCH_STAIRS, along.getOpposite(), true));
			}
		}
	}

	/**
	 * The main gate in the south wall: a 7 x 9 opening between two round white gateposts 12 tall, a white lintel, two
	 * huge golden doors standing open against the inner wall, and a large blue banner either side on gold supports.
	 */
	private void mainGate(Canvas c) {
		if (!c.near(0, 0, 16)) {
			return;
		}
		c.fill(-3, F + 1, -1, 3, F + 9, 1, s(Blocks.AIR));
		c.fill(-3, F, -1, 3, F, 1, s(Blocks.SMOOTH_STONE));
		c.fill(-4, F + 10, -1, 4, F + 10, 1, s(Blocks.POLISHED_DIORITE));
		c.fill(-4, F + 11, -1, 4, F + 11, 1, s(Blocks.SMOOTH_QUARTZ));
		c.fill(-4, F + 12, -1, 4, F + 12, 1, slab(Blocks.SMOOTH_QUARTZ_SLAB, false));
		c.fill(-1, F + 10, 2, 1, F + 11, 2, s(Blocks.BLUE_CONCRETE)); // a small blue plaque on the lintel
		c.set(0, F + 10, 2, s(Blocks.WHITE_CONCRETE));
		gatepost(c, -5, 0, F + 1, 12);
		gatepost(c, 5, 0, F + 1, 12);
		doorLeaf(c, -4, -2, Direction.WEST, 4, 9, Direction.NORTH);
		doorLeaf(c, 4, -2, Direction.EAST, 4, 9, Direction.NORTH);
		bigBanner(c, -11, 2);
		bigBanner(c, 9, 2);
	}

	/** A large blue banner (3 x 6) with a white naval anchor, hung on a birch rod with gold finials, from (x..x+2, z). */
	private static void bigBanner(Canvas c, int x, int z) {
		String[] anchor = {".W.", "WWW", ".W.", ".W.", "WWW", "W.W"};
		for (int r = 0; r < anchor.length; r++) {
			int y = F + 7 - r;
			for (int k = 0; k < 3; k++) {
				c.set(x + k, y, z, anchor[r].charAt(k) == 'W' ? s(Blocks.WHITE_CONCRETE) : s(Blocks.BLUE_WOOL));
			}
		}
		c.fill(x - 1, F + 8, z, x + 3, F + 8, z, log(Blocks.STRIPPED_BIRCH_LOG, Direction.Axis.X));
		c.set(x - 1, F + 8, z, s(Blocks.GOLD_BLOCK));
		c.set(x + 3, F + 8, z, s(Blocks.GOLD_BLOCK));
	}

	/** A side gate in the west ({@code side} -1) or east wall: 5 x 7, smaller gateposts, golden doors, steps down. */
	private void sideGate(Canvas c, int side) {
		int wx = 36 * side;
		if (!c.near(wx, -9, 14)) {
			return;
		}
		c.fill(wx - 1, F + 1, -11, wx + 1, F + 7, -7, s(Blocks.AIR));
		c.fill(wx - 1, F, -11, wx + 1, F, -7, s(Blocks.SMOOTH_STONE));
		gatepost(c, wx, -13, F + 1, 10);
		gatepost(c, wx, -5, F + 1, 10);
		Direction inward = side < 0 ? Direction.EAST : Direction.WEST;
		int ix = wx - 2 * side;
		doorLeaf(c, ix, -12, Direction.NORTH, 3, 7, inward);
		doorLeaf(c, ix, -6, Direction.SOUTH, 3, 7, inward);
		// Stone steps down from the terrace edge to the landform outside.
		for (int k = 1; k <= 4; k++) {
			int x = (44 + k) * side;
			int y = F - k + 1;
			for (int z = -10; z <= -8; z++) {
				c.foundation(x, z, x, z, y - 1, s(Blocks.STONE_BRICKS));
				c.set(x, y, z, Kit.stairs(Blocks.STONE_BRICK_STAIRS, inward, false));
				c.fill(x, y + 1, z, x, y + 3, z, s(Blocks.AIR));
			}
		}
	}

	/** The rear postern: a small doorway through the north wall to the ocean-facing battery walk. */
	private void postern(Canvas c) {
		if (!c.near(13, -47, 8)) {
			return;
		}
		c.fill(12, F + 1, -48, 14, F + 4, -46, s(Blocks.AIR));
		c.fill(12, F, -48, 14, F, -46, s(Blocks.SMOOTH_STONE));
		c.fill(11, F + 5, -48, 15, F + 5, -48, s(Blocks.POLISHED_DIORITE));
	}

	// ============================================================================================== staircase and plaza

	/** The broad white staircase from the town (y 66) up to the fortress terrace (y 71), with white cheek walls. */
	private void staircase(Canvas c) {
		if (!c.near(0, 10, 10)) {
			return;
		}
		for (int z = 8; z <= 12; z++) {
			int y = T + 1 + (12 - z);
			for (int x = -6; x <= 6; x++) {
				c.fill(x, T, z, x, y - 1, z, s(Blocks.POLISHED_DIORITE));
				c.set(x, y, z, Kit.stairs(Math.abs(x) <= 4 ? Blocks.SMOOTH_QUARTZ_STAIRS : Blocks.POLISHED_DIORITE_STAIRS, Direction.NORTH, false));
				c.fill(x, y + 1, z, x, y + 4, z, s(Blocks.AIR));
			}
			for (int x : new int[]{-7, 7}) {
				c.fill(x, T, z, x, y, z, s(Blocks.SMOOTH_QUARTZ));
				c.set(x, y + 1, z, slab(Blocks.SMOOTH_QUARTZ_SLAB, false));
			}
		}
		c.fill(-6, F, 5, 6, F, 7, s(Blocks.SMOOTH_QUARTZ)); // the landing at the top
	}

	/**
	 * The octagonal plaza pattern (19 x 19) near the waterfront, flush with the paving: a stone and light grey border, a
	 * broad blue octagonal band, a beige interior with a white four-lobed outline and a blue-and-white diamond at its
	 * centre. Symmetrical across both axes and diagonals (the diamond's quarters alternate blue and white).
	 */
	private void octagon(Canvas c) {
		if (!c.near(OCT_X, OCT_Z, 12)) {
			return;
		}
		for (int u = -9; u <= 9; u++) {
			for (int v = -9; v <= 9; v++) {
				int m = Math.max(Math.max(Math.abs(u), Math.abs(v)), (int) Math.ceil((Math.abs(u) + Math.abs(v)) * 9 / 13.0));
				if (m > 9) {
					continue;
				}
				BlockState b;
				if (m == 9) {
					b = s(Blocks.SMOOTH_STONE);
				} else if (m == 8) {
					b = s(Blocks.LIGHT_GRAY_CONCRETE);
				} else if (m == 7) {
					b = s(Blocks.BLUE_CONCRETE);
				} else if (m == 6) {
					b = s(Blocks.BLUE_TERRACOTTA); // the darker inner blue
				} else if (m == 5) {
					b = s(Blocks.POLISHED_DIORITE);
				} else if (Math.abs(u) + Math.abs(v) <= 2) {
					boolean axis = u == 0 || v == 0;
					b = u == 0 && v == 0 ? s(Blocks.LIGHT_GRAY_CONCRETE) : axis ? s(Blocks.LIGHT_BLUE_CONCRETE)
							: (u > 0) == (v > 0) ? s(Blocks.WHITE_CONCRETE) : s(Blocks.BLUE_CONCRETE);
				} else if (lobeOutline(u, v)) {
					b = s(Blocks.WHITE_CONCRETE);
				} else {
					b = s(Blocks.SMOOTH_SANDSTONE);
				}
				c.set(OCT_X + u, T, OCT_Z + v, b);
			}
		}
	}

	/** The four-lobed figure: four circles (radius 2.5) on the axes, 2.5 from the centre. */
	static boolean inLobes(int u, int v) {
		return Math.hypot(u - 2.5, v) <= 2.6 || Math.hypot(u + 2.5, v) <= 2.6 || Math.hypot(u, v - 2.5) <= 2.6 || Math.hypot(u, v + 2.5) <= 2.6;
	}

	static boolean lobeOutline(int u, int v) {
		return inLobes(u, v) && (!inLobes(u + 1, v) || !inLobes(u - 1, v) || !inLobes(u, v + 1) || !inLobes(u, v - 1));
	}

	// ============================================================================================== town houses

	static final Block[] PLASTER = {Blocks.WHITE_TERRACOTTA, Blocks.SMOOTH_SANDSTONE, Blocks.BIRCH_PLANKS, Blocks.CALCITE};

	/**
	 * A timber-framed townhouse (decorative: closed, lit inside, no furniture): a stone plinth with stone corner supports,
	 * cream walls between dark oak posts and beams, gridded light blue windows, cross-bracing on the upper floor, a door
	 * with an awning on the front, a narrow chimney and a pitched roof (orange, blue, brown or green) with eaves.
	 */
	private void house(Canvas c, int[] h) {
		int x1 = h[0], z1 = h[1], x2 = h[2], z2 = h[3], roof = h[4];
		Block plaster = PLASTER[h[5]];
		Direction front = h[6] == 0 ? Direction.EAST : h[6] == 1 ? Direction.WEST : Direction.NORTH;
		int base = T + 1, beam1 = T + 5, beam2 = T + 10;
		// Plinth and the hollow, lit shell.
		c.foundation(x1, z1, x2, z2, T, s(Blocks.STONE_BRICKS));
		for (int x = x1; x <= x2; x++) {
			for (int z = z1; z <= z2; z++) {
				boolean ex = x == x1 || x == x2, ez = z == z1 || z == z2;
				if (ex && ez) {
					c.set(x, T, z, s(Blocks.POLISHED_ANDESITE));
					c.fill(x, base, z, x, beam1 - 1, z, s(Blocks.STONE_BRICKS)); // stone corner supports
					c.fill(x, beam1, z, x, beam2, z, log(Blocks.DARK_OAK_LOG, Direction.Axis.Y));
					continue;
				}
				if (!ex && !ez) {
					c.fill(x, base, z, x, beam2 - 1, z, s(Blocks.AIR));
					c.set(x, base - 1, z, s(Blocks.DARK_OAK_PLANKS));
					c.set(x, beam1, z, s(Blocks.DARK_OAK_PLANKS));
					c.set(x, beam2, z, s(Blocks.DARK_OAK_PLANKS));
					continue;
				}
				boolean alongX = ez; // this wall runs along x
				int i = alongX ? x - x1 : z - z1;
				Direction out = alongX ? (z == z1 ? Direction.NORTH : Direction.SOUTH) : (x == x1 ? Direction.WEST : Direction.EAST);
				wallColumn(c, x, z, i, alongX, out, out == front, plaster, base, beam1, beam2);
			}
		}
		// Hidden lights: in the middle of each ceiling.
		int mx = (x1 + x2) / 2, mz = (z1 + z2) / 2;
		c.set(mx, beam1, mz, s(Blocks.GLOWSTONE));
		c.set(mx, beam2, mz, s(Blocks.GLOWSTONE));
		// The door, centred on the front, with a little awning over it.
		int dx = front == Direction.NORTH ? (x1 + x2) / 2 : front == Direction.EAST ? x2 : x1;
		int dz = front == Direction.NORTH ? z1 : (z1 + z2) / 2;
		int along = front == Direction.NORTH ? dx - x1 : dz - z1;
		if (along % 4 == 0) {
			if (front == Direction.NORTH) {
				dx++;
			} else {
				dz++;
			}
		}
		Kit.door(c, dx, base, dz, Blocks.DARK_OAK_DOOR, front);
		c.set(dx, base + 2, dz, s(Blocks.DARK_OAK_PLANKS));
		c.set(dx + front.getStepX(), base + 3, dz + front.getStepZ(), s(Blocks.DARK_OAK_TRAPDOOR)
				.setValue(TrapDoorBlock.FACING, front).setValue(TrapDoorBlock.HALF, Half.TOP));
		if (h[2] - h[0] >= 14 || h[3] - h[1] >= 14 || (h[0] == -31 || h[0] == 20)) {
			RedbrickPort.crestBanner(c, dx + front.getClockWise().getStepX() * 2 + front.getStepX(), beam1 + 2,
					dz + front.getClockWise().getStepZ() * 2 + front.getStepZ(), front);
		}
		roof(c, x1, z1, x2, z2, beam2 + 1, roof, plaster);
	}

	/** One column of a house wall (not a corner): posts, beams, plaster, windows, bracing, awnings. */
	private static void wallColumn(Canvas c, int x, int z, int i, boolean alongX, Direction out, boolean front, Block plaster,
			int base, int beam1, int beam2) {
		Direction.Axis axis = alongX ? Direction.Axis.X : Direction.Axis.Z;
		boolean post = i % 4 == 0;
		int k = i % 4; // 1..3 inside a panel
		int panel = i / 4;
		for (int y = base; y <= beam2; y++) {
			BlockState b;
			if (y == beam1 || y == beam2) {
				b = post ? log(Blocks.DARK_OAK_LOG, Direction.Axis.Y) : log(Blocks.STRIPPED_DARK_OAK_LOG, axis);
			} else if (post) {
				b = log(Blocks.DARK_OAK_LOG, Direction.Axis.Y);
			} else if (y < beam1) {
				boolean window = k <= 2 && (y == base + 1 || y == base + 2);
				b = window ? Kit.connected(Blocks.LIGHT_BLUE_STAINED_GLASS_PANE, alongX) : s(plaster);
			} else {
				int row = y - beam1; // 1..4
				boolean window = (front || panel % 2 == 0) && k <= 2 && (row == 2 || row == 3);
				boolean brace = !window && !front && row <= 3 && (row == k || row == 4 - k);
				b = window ? Kit.connected(front ? Blocks.LIGHT_BLUE_STAINED_GLASS_PANE : Blocks.BLACK_STAINED_GLASS_PANE, alongX)
						: brace ? s(Blocks.DARK_OAK_PLANKS) : s(plaster);
			}
			c.set(x, y, z, b);
		}
		if (front && !post && k <= 2) {
			// A small shop awning over the ground floor windows.
			c.set(x + out.getStepX(), beam1 - 1, z + out.getStepZ(), s(Blocks.SPRUCE_TRAPDOOR)
					.setValue(TrapDoorBlock.FACING, out).setValue(TrapDoorBlock.HALF, Half.TOP));
		}
	}

	/**
	 * A pitched roof over the footprint with a one-block overhang, its ridge along the longer side; gable ends filled
	 * with plaster and a dark oak king post; a narrow stone chimney. Roof kinds: 0 orange (acacia, orange terracotta
	 * ridge), 1 dark blue (blue terracotta courses, dark prismarine eaves and ridge), 2 brown (spruce, dark oak eaves),
	 * 3 green (green terracotta courses, dark prismarine eaves, moss ridge).
	 */
	private static void roof(Canvas c, int x1, int z1, int x2, int z2, int y, int kind, Block plaster) {
		boolean ridgeX = (x2 - x1) >= (z2 - z1);
		int p1 = (ridgeX ? z1 : x1) - 1, p2 = (ridgeX ? z2 : x2) + 1; // across the ridge, with eaves
		int q1 = (ridgeX ? x1 : z1) - 1, q2 = (ridgeX ? x2 : z2) + 1; // along the ridge, with eaves
		Direction up1 = ridgeX ? Direction.SOUTH : Direction.EAST;      // climbing from the p1 side
		int ridgeY = y;
		for (int r = 0; p1 + r <= p2 - r; r++) {
			int a = p1 + r, b = p2 - r, yy = y + r;
			ridgeY = yy;
			for (int q = q1; q <= q2; q++) {
				for (int side = 0; side < 2; side++) {
					int p = side == 0 ? a : b;
					Direction facing = side == 0 ? up1 : up1.getOpposite();
					boolean ridge = a >= b - 1;
					BlockState st;
					if (ridge && a == b) {
						st = switch (kind) {
							case 0 -> s(Blocks.ORANGE_TERRACOTTA);
							case 1 -> slab(Blocks.DARK_PRISMARINE_SLAB, false);
							case 2 -> slab(Blocks.SPRUCE_SLAB, false);
							default -> s(Blocks.MOSS_BLOCK);
						};
					} else if (kind == 0) {
						st = Kit.stairs(Blocks.ACACIA_STAIRS, facing, false);
					} else if (kind == 2) {
						st = Kit.stairs(r == 0 ? Blocks.DARK_OAK_STAIRS : Blocks.SPRUCE_STAIRS, facing, false);
					} else if (r == 0) {
						st = Kit.stairs(Blocks.DARK_PRISMARINE_STAIRS, facing, false);
					} else {
						st = s(kind == 1 ? Blocks.BLUE_TERRACOTTA : Blocks.GREEN_TERRACOTTA);
					}
					setRoof(c, ridgeX, q, p, yy, st);
				}
				// The attic: air inside, plaster gable ends (with a dark oak king post), open eaves.
				for (int p = a + 1; p <= b - 1; p++) {
					boolean end = q == q1 + 1 || q == q2 - 1;
					boolean outsideWalls = q == q1 || q == q2;
					BlockState fill = outsideWalls ? s(Blocks.AIR) : end ? (p == (p1 + p2) / 2 ? log(Blocks.DARK_OAK_LOG, Direction.Axis.Y) : s(plaster)) : s(Blocks.AIR);
					setRoof(c, ridgeX, q, p, yy, fill);
				}
			}
		}
		// The narrow chimney near one gable end, rising past the ridge.
		int chq = q1 + 3, chp = (p1 + p2) / 2 + 1;
		for (int yy = y - 1; yy <= ridgeY + 2; yy++) {
			setRoof(c, ridgeX, chq, chp, yy, yy == ridgeY + 2 ? s(Blocks.POLISHED_ANDESITE) : s(Blocks.STONE_BRICKS));
		}
	}

	private static void setRoof(Canvas c, boolean ridgeX, int q, int p, int y, BlockState b) {
		if (ridgeX) {
			c.set(q, y, p, b);
		} else {
			c.set(p, y, q, b);
		}
	}

	// ============================================================================================== dock and watch posts

	/**
	 * The main dock: a light stone walkway (5 x 12) straight out from the waterfront, ending in a fork of two short
	 * projecting sections (3 x 6), on stone brick piers; stone brick slab edging.
	 */
	private void dock(Canvas c) {
		if (!c.near(0, 56, 12)) {
			return;
		}
		for (int x = -5; x <= 5; x++) {
			for (int z = 49; z <= 62; z++) {
				boolean walk = Math.abs(x) <= 2 && z <= 60;
				boolean prong = Math.abs(x) >= 3 && z >= 57;
				if (!walk && !prong) {
					continue;
				}
				boolean edge = Math.abs(x) == 5 || (walk && Math.abs(x) == 2 && z < 57) || z == 62 || (walk && !prong && z == 60 && Math.abs(x) <= 2);
				c.set(x, T, z, edge ? s(Blocks.POLISHED_DIORITE) : s(Blocks.SMOOTH_STONE));
				c.set(x, T - 1, z, slab(Blocks.POLISHED_DIORITE_SLAB, true));
				c.fill(x, T + 1, z, x, T + 4, z, s(Blocks.AIR));
				boolean pier = (Math.abs(x) == 2 || Math.abs(x) == 5) && (z == 52 || z == 56 || z == 60 || z == 62);
				if (pier) {
					c.fill(x, c.ground(x, z), z, x, T - 1, z, s(Blocks.STONE_BRICKS));
				}
				if ((Math.abs(x) == 5 || (Math.abs(x) == 2 && z < 57)) && z % 2 == 0) {
					c.set(x, T + 1, z, slab(Blocks.STONE_BRICK_SLAB, false)); // low edging
				}
			}
		}
		// The gap between the prongs stays open water.
		c.fill(-2, T - 1, 61, 2, T, 62, s(Blocks.AIR));
		c.fill(-2, T - 1, 61, 2, T - 1, 62, s(Blocks.AIR));
	}

	/** A dark wooden lookout (6 x 6, 15 tall): corner posts, fence cross-bracing, a railed deck and a slab roof. */
	private void watchPlatform(Canvas c, int x0, int z0, boolean cannon) {
		int g = c.ground(x0 + 2, z0 + 2);
		int deck = g + 11, roofY = g + 15;
		int[][] posts = {{x0, z0}, {x0 + 5, z0}, {x0, z0 + 5}, {x0 + 5, z0 + 5}};
		for (int[] p : posts) {
			c.foundation(p[0], p[1], p[0], p[1], g, s(Blocks.STONE_BRICKS));
			c.fill(p[0], g + 1, p[1], p[0], roofY - 1, p[1], log(Blocks.DARK_OAK_LOG, Direction.Axis.Y));
		}
		// Cross-bracing on the four faces, below the deck.
		for (int k = 1; k <= 4; k++) {
			int ya = g + 2 + k, yb = g + 7 - k;
			for (int[] f : new int[][]{{x0 + k, z0, 1}, {x0 + k, z0 + 5, 1}, {x0, z0 + k, 0}, {x0 + 5, z0 + k, 0}}) {
				BlockState fence = Kit.connected(Blocks.DARK_OAK_FENCE, f[2] == 1);
				c.set(f[0], ya, f[1], fence);
				c.set(f[0], yb, f[1], fence);
			}
		}
		for (int x = x0 - 1; x <= x0 + 6; x++) {
			for (int z = z0 - 1; z <= z0 + 6; z++) {
				boolean inner = x >= x0 && x <= x0 + 5 && z >= z0 && z <= z0 + 5;
				if (inner) {
					c.set(x, deck, z, s(Blocks.DARK_OAK_PLANKS));
					boolean rim = x == x0 || x == x0 + 5 || z == z0 || z == z0 + 5;
					boolean corner = (x == x0 || x == x0 + 5) && (z == z0 || z == z0 + 5);
					if (rim && !corner) {
						boolean openFront = cannon && z == z0 + 5 && (x == x0 + 2 || x == x0 + 3);
						c.set(x, deck + 1, z, openFront ? s(Blocks.AIR) : Kit.connected(Blocks.DARK_OAK_FENCE, z == z0 || z == z0 + 5));
					}
				} else {
					Direction out = x < x0 ? Direction.WEST : x > x0 + 5 ? Direction.EAST : z < z0 ? Direction.NORTH : Direction.SOUTH;
					c.set(x, deck, z, s(Blocks.SPRUCE_TRAPDOOR).setValue(TrapDoorBlock.FACING, out).setValue(TrapDoorBlock.HALF, Half.TOP));
				}
				c.set(x, roofY, z, slab(Blocks.DARK_OAK_SLAB, false));
			}
		}
		if (cannon) {
			cannon(c, x0 + 2, deck + 1, z0 + 3, Direction.SOUTH);
		}
	}

	// ============================================================================================== landscaping

	private void landscape(Canvas c) {
		for (int t = 0; t < TREES.length; t++) {
			if (c.near(TREES[t][0], TREES[t][1], 6)) {
				tree(c, TREES[t][0], TREES[t][1], t);
			}
		}
		// Small, evenly spaced hedges: along the terrace parapet and below it in the town.
		for (int x = 9; x <= 24; x += 3) {
			for (int sx : new int[]{-1, 1}) {
				hedge(c, sx * x, 6, F + 1);
				hedge(c, sx * (x + 3), 9, T + 1);
			}
		}
		// Decorative lampposts round the plaza and on the promenade.
		for (int[] l : new int[][]{{-15, 12}, {15, 12}, {-15, 35}, {15, 35}, {-9, 46}, {9, 46}}) {
			if (c.near(l[0], l[1], 2)) {
				Kit.lamp(c, l[0], l[1], T);
			}
		}
		// Stone benches facing the octagon.
		for (int[] b : new int[][]{{-12, 18}, {12, 18}, {-12, 29}, {12, 29}}) {
			Direction back = b[0] < 0 ? Direction.WEST : Direction.EAST;
			c.set(b[0], T + 1, b[1], Kit.stairs(Blocks.STONE_STAIRS, back, false));
			c.set(b[0], T + 1, b[1] + 1, Kit.stairs(Blocks.STONE_STAIRS, back, false));
		}
		// A few small coastal rocks.
		for (int[] r : new int[][]{{-30, -63}, {18, -64}, {-62, -34}, {62, -22}, {-56, 38}, {57, 41}, {-27, 56}, {30, 57}}) {
			if (!c.near(r[0], r[1], 2)) {
				continue;
			}
			for (int dx = 0; dx <= 1; dx++) {
				for (int dz = 0; dz <= 1; dz++) {
					int g = c.ground(r[0] + dx, r[1] + dz);
					if (g >= SEA || g < SEA - 4) {
						continue;
					}
					int top = SEA + 1 + ((dx + dz) % 2 == 0 ? 1 : 0);
					c.fill(r[0] + dx, g, r[1] + dz, r[0] + dx, top, r[1] + dz, (dx + dz) % 2 == 0 ? s(Blocks.STONE) : s(Blocks.ANDESITE));
				}
			}
		}
	}

	private static void hedge(Canvas c, int x, int z, int y) {
		c.set(x, y, z, Kit.leaves(Blocks.OAK_LEAVES));
		c.set(x + 1, y, z, Kit.leaves(Blocks.AZALEA_LEAVES));
	}

	/** A broad, blocky shade tree: a short trunk and layered angular canopies of oak, dark oak and azalea leaves. */
	private static void tree(Canvas c, int x, int z, int index) {
		int g = c.ground(x, z);
		if (g < SEA + 1) {
			return;
		}
		Block trunk = index % 2 == 0 ? Blocks.DARK_OAK_LOG : Blocks.SPRUCE_LOG;
		c.fill(x, g + 1, z, x, g + 4, z, log(trunk, Direction.Axis.Y));
		int[][] layers = {{2, 4}, {3, 5}, {2, 6}, {1, 7}}; // half-width, height above the ground
		Block[] leaves = {Blocks.OAK_LEAVES, Blocks.DARK_OAK_LEAVES, Blocks.AZALEA_LEAVES, Blocks.OAK_LEAVES};
		for (int i = 0; i < layers.length; i++) {
			int w = layers[i][0], y = g + layers[i][1];
			for (int dx = -w; dx <= w; dx++) {
				for (int dz = -w; dz <= w; dz++) {
					boolean corner = Math.abs(dx) == w && Math.abs(dz) == w && w > 1;
					if (corner || (dx == 0 && dz == 0 && y <= g + 4)) {
						continue;
					}
					c.set(x + dx, y, z + dz, Kit.leaves(leaves[(i + Math.floorMod(dx * 3 + dz, 2)) % leaves.length]));
				}
			}
		}
	}
}
