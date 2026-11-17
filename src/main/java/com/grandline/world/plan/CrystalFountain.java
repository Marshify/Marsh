package com.grandline.world.plan;

import com.grandline.world.plan.Anchor.Type;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.WallBannerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;

import java.util.ArrayList;
import java.util.List;

/**
 * Crystal Fountain (layout 14), built to the owner's specification and the approved blueprint
 * (docs/CRYSTAL_FOUNTAIN_BLUEPRINT.md): a circular walled ocean city round a central landmark.
 * <pre>
 *  ring / piece            radius or box (local)          ground y
 *  central landmark        octagon 52 across               76 (top y 201)
 *  courtyard               r 26-33                         76
 *  enclosure wall          r 33-39, 20 high                76-96
 *  ring road A             r 39-51                         74
 *  inner district          r 51-66                         74
 *  main canal              r 66-80 (water 68-78, y 70)
 *  ring road B             r 80-90                         72
 *  middle district         r 90-120 (four fountain plazas) 72
 *  ring road C             r 120-130                       72
 *  outer district          r 130-148                       72
 *  outer wall              r 150-160, 24 high              72-96
 *  harbour                 south: gate, staircase, plaza 90 x 35 (y 65), three piers (y 64), twin towers
 *  scrapyard               east, about 115 x 95            66
 * </pre>
 * Eight radial streets (the harbour avenue 14 wide), three canal outlets through arches in the outer wall, bridges.
 * About a hundred townhouses, generated once from a fixed layout (the same in every world). Island-local coordinates
 * from the landmark's centre: +x east, +z south. Worlds made before layout 14 keep the original island.
 */
public final class CrystalFountain extends BasePlan {
	static final int SEA = 62, G_OUT = 72, G_IN = 74, G_CT = 76;
	static final int R_LANDMARK = 26, R_COURT = 33, R_ENCL = 39, R_ROAD_A = 51, R_INNER = 66, R_CANAL = 80, R_ROAD_B = 90,
			R_MID = 120, R_ROAD_C = 130, R_OUTER = 148, R_WALL_IN = 150, R_WALL = 160;
	static final int WALL_TOP = 96, ENCL_TOP = 96, CANAL_WATER = 70;
	/** Radial streets (degrees; 0 east, 90 south): the harbour avenue at 90 is 14 wide, the rest 8. */
	static final int[] STREETS = {0, 45, 90, 135, 180, 225, 270, 315};
	/** Canal outlets to the sea (degrees), each 8 wide. */
	static final double[] OUTLETS = {157.5, 247.5, 337.5};
	/** Fountain centres. */
	static final int[][] FOUNTAINS = {{67, 80}, {-67, 80}, {67, -80}, {-67, -80}};
	static final int PLAZA_R = 24;
	// Harbour.
	static final int GATE_HALF = 15, PLAZA_Z1 = 160, PLAZA_Z2 = 195, PLAZA_X = 45, PLAZA_Y = 65, PIER_Y = 64;
	// Scrapyard.
	static final int YARD_X = 215, YARD_Z = -8, YARD_RX = 57, YARD_RZ = 47, YARD_Y = 66;
	static final int ENGINE_X = 220, ENGINE_Z = -6;

	CrystalFountain() {
		super("Crystal Fountain", 0.04);
		terrain.reach(290);
		anchor(new Anchor(Type.ARRIVAL, 0, PLAZA_Y + 1, 178, 0, 180));
		anchor(Anchor.at(Type.FISHERMAN, 0, PIER_Y + 1, 236, 180));
		anchor(Anchor.at(Type.DOCK, 9, 63, 246));
		anchor(Anchor.at(Type.QUEST, 8, G_OUT + 1, 140, 0));
		anchor(new Anchor(Type.ENEMY_CAMP, 0, G_OUT, -105, 8));
		anchor(new Anchor(Type.ENEMY_CAMP, 205, YARD_Y, 25, 10));
		anchor(Anchor.at(Type.BOSS_ARENA, 190, YARD_Y, -20));
	}

	protected static BlockState s(Block b) {
		return b.defaultBlockState();
	}

	static double unit(int x, int z, int salt) {
		return CloudtopIslesV7.unit(x, z, salt);
	}

	static double unit3(int x, int y, int z, int salt) {
		return CloudtopIslesV7.unit(x * 7 + y, z * 3 - y, salt);
	}

	/** Octagon "radius" (flat faces on the axes and diagonals). */
	static double oct(double dx, double dz) {
		return Math.max(Math.max(Math.abs(dx), Math.abs(dz)), (Math.abs(dx) + Math.abs(dz)) / Math.sqrt(2));
	}

	/** Angle in degrees 0..360 (0 east, 90 south). */
	static double angle(double x, double z) {
		double a = Math.toDegrees(Math.atan2(z, x));
		return a < 0 ? a + 360 : a;
	}

	/** Distance from (x, z) to a radial line at {@code deg}, and the distance along it (negative behind the centre). */
	static double[] radial(double x, double z, double deg) {
		double t = Math.toRadians(deg), ux = Math.cos(t), uz = Math.sin(t);
		return new double[]{Math.abs(-uz * x + ux * z), ux * x + uz * z};
	}

	static int streetHalf(int deg) {
		return deg == 90 ? 7 : 4;
	}

	/** Whether (x, z) is on a radial street (between the enclosure and the outer wall). */
	static boolean onStreet(double x, double z, double margin) {
		for (int deg : STREETS) {
			double[] p = radial(x, z, deg);
			if (p[1] > R_ENCL - 2 && p[1] < R_WALL + 4 && p[0] <= streetHalf(deg) + margin) {
				return true;
			}
		}
		return false;
	}

	/** Whether (x, z) is in a canal outlet channel (water 3 either side of the line, edges 1 more). */
	static boolean inOutlet(double x, double z, double margin) {
		for (double deg : OUTLETS) {
			double[] p = radial(x, z, deg);
			if (p[1] > R_CANAL - 3 && p[1] < R_WALL + 3 && p[0] <= 4 + margin) {
				return true;
			}
		}
		return false;
	}

	static boolean inPlaza(double x, double z, double margin) {
		for (int[] f : FOUNTAINS) {
			if (Math.hypot(x - f[0], z - f[1]) <= PLAZA_R + margin) {
				return true;
			}
		}
		return false;
	}

	static int groundAt(double r) {
		return r < R_COURT ? G_CT : r < R_INNER ? G_IN : G_OUT;
	}

	// ==================================================================================================== houses

	/** A townhouse: box, floors, walls, roof colour, roof kind, facing. */
	record House(int x1, int z1, int w, int d, int y, int floors, Block wall, int roof, boolean mansard, Direction front) {
		int x2() {
			return x1 + w - 1;
		}

		int z2() {
			return z1 + d - 1;
		}
	}

	/** Roof colours: red, green, blue, grey. */
	static final Block[] ROOF_STAIRS = {Blocks.BRICK_STAIRS, Blocks.DARK_PRISMARINE_STAIRS, Blocks.BLUE_TERRACOTTA, Blocks.DEEPSLATE_TILE_STAIRS};
	static final Block[] ROOF_FULL = {Blocks.BRICKS, Blocks.DARK_PRISMARINE, Blocks.BLUE_TERRACOTTA, Blocks.DEEPSLATE_TILES};
	static final Block[] WALLS = {Blocks.WHITE_CONCRETE, Blocks.CALCITE, Blocks.SMOOTH_SANDSTONE, Blocks.END_STONE_BRICKS};

	static final List<House> HOUSES = new ArrayList<>();
	/** Little gardens (a tree and a bench) in the inner district. */
	static final List<int[]> GARDENS = new ArrayList<>();

	static {
		// Inner district: small and medium townhouses facing road A (green and red roofs), a garden every few.
		ring(R_ROAD_A + 1, R_INNER - 1, true, new int[][]{{9, 12, 2}, {12, 14, 3}}, new int[]{1, 0, 1, 0, 0}, 0, true);
		// Middle district: a row facing road B and a row facing road C (backs together).
		ring(R_ROAD_B + 1, 105, true, new int[][]{{9, 12, 2}, {12, 14, 3}}, new int[]{0, 2, 3, 0, 1}, 1, false);
		ring(106, R_MID - 1, false, new int[][]{{12, 13, 3}, {9, 12, 2}}, new int[]{3, 0, 2, 0}, 2, false);
		// Outer district: taller, denser, facing road C.
		ring(R_ROAD_C + 1, R_OUTER - 1, true, new int[][]{{16, 17, 4}, {12, 15, 3}}, new int[]{0, 3, 2, 0, 3}, 3, false);
	}

	/**
	 * Lays a ring of townhouses between radii r1 and r2, fronting the road on the inside ({@code faceIn}) or the outside,
	 * each turned (in 90-degree steps) to face that road, skipping streets, outlets, plazas, the gates and each other.
	 */
	static void ring(int r1, int r2, boolean faceIn, int[][] sizes, int[] roofs, int salt, boolean gardens) {
		double rMid = (r1 + r2) / 2.0;
		double a = 3 + salt * 4;
		int n = 0;
		while (a < 363 + salt * 4) {
			double t = Math.toRadians(a);
			double dirX = Math.cos(t), dirZ = Math.sin(t);
			// Face the road: the cardinal closest to inward (faceIn) or outward.
			double fx = faceIn ? -dirX : dirX, fz = faceIn ? -dirZ : dirZ;
			Direction front = Math.abs(fx) >= Math.abs(fz) ? (fx > 0 ? Direction.EAST : Direction.WEST) : (fz > 0 ? Direction.SOUTH : Direction.NORTH);
			// Try this slot's size, then the others, then a small 9 x 9; slide in and out within the ring to fit.
			List<int[]> tries = new ArrayList<>();
			tries.add(sizes[n % sizes.length]);
			for (int[] sz : sizes) {
				if (sz != sizes[n % sizes.length]) {
					tries.add(sz);
				}
			}
			tries.add(new int[]{9, 9, 2});
			House found = null;
			for (int[] size : tries) {
				int w = front.getAxis() == Direction.Axis.Z ? size[0] : size[1];
				int d = front.getAxis() == Direction.Axis.Z ? size[1] : size[0];
				for (int off : new int[]{0, -1, 1, -2, 2, -3, 3, -4, 4}) {
					double rm = rMid + off;
					int cx = (int) Math.round(rm * dirX), cz = (int) Math.round(rm * dirZ);
					int x1 = cx - w / 2, z1 = cz - d / 2;
					House h = new House(x1, z1, w, d, groundAt(rMid), size[2], WALLS[(int) (unit(x1, z1, 5) * 4)], roofs[n % roofs.length],
							size[2] >= 4 && n % 2 == 0, front);
					if (fits(h, r1, r2)) {
						found = h;
						break;
					}
				}
				if (found != null) {
					break;
				}
			}
			if (found != null && gardens && n % 4 == 3) {
				GARDENS.add(new int[]{(found.x1() + found.x2()) / 2, (found.z1() + found.z2()) / 2});
				a += Math.toDegrees(7.0 / rMid);
				n++;
			} else if (found != null) {
				HOUSES.add(found);
				a += Math.toDegrees((Math.max(found.w(), found.d()) * 0.8 + 2) / rMid);
				n++;
			} else {
				a += Math.toDegrees(1.5 / rMid);
			}
		}
	}

	static boolean fits(House h, int r1, int r2) {
		int[][] pts = {{h.x1(), h.z1()}, {h.x2(), h.z1()}, {h.x1(), h.z2()}, {h.x2(), h.z2()}, {(h.x1() + h.x2()) / 2, h.z1()},
				{(h.x1() + h.x2()) / 2, h.z2()}, {h.x1(), (h.z1() + h.z2()) / 2}, {h.x2(), (h.z1() + h.z2()) / 2}};
		for (int[] p : pts) {
			double r = Math.hypot(p[0], p[1]);
			if (r < r1 || r > r2 || onStreet(p[0], p[1], 1.5) || inOutlet(p[0], p[1], 1.5) || inPlaza(p[0], p[1], 1.5)) {
				return false;
			}
		}
		// A street line passing between the corners.
		for (int deg : STREETS) {
			double t = Math.toRadians(deg), ux = Math.cos(t), uz = Math.sin(t);
			double min = Double.MAX_VALUE, max = -Double.MAX_VALUE;
			for (int[] p : pts) {
				double side = -uz * p[0] + ux * p[1];
				min = Math.min(min, side);
				max = Math.max(max, side);
			}
			double along = ux * (h.x1() + h.x2()) / 2.0 + uz * (h.z1() + h.z2()) / 2.0;
			if (min < 0 && max > 0 && along > 0) {
				return false;
			}
		}
		for (House o : HOUSES) {
			if (h.x1() <= o.x2() + 2 && h.x2() >= o.x1() - 2 && h.z1() <= o.z2() + 2 && h.z2() >= o.z1() - 2) {
				return false;
			}
		}
		return true;
	}

	// ==================================================================================================== build

	@Override
	public void build(Canvas c) {
		foundation(c);
		canals(c);
		roads(c);
		enclosure(c);
		landmark(c);
		for (int[] f : FOUNTAINS) {
			fountain(c, f[0], f[1]);
		}
		for (House h : HOUSES) {
			if (c.near((h.x1() + h.x2()) / 2, (h.z1() + h.z2()) / 2, Math.max(h.w(), h.d()) / 2 + 3)) {
				townhouse(c, h);
			}
		}
		for (int[] g : GARDENS) {
			garden(c, g[0], g[1]);
		}
		outerWall(c);
		harbour(c);
		gateway(c);
		scrapyard(c);
		lamps(c);
	}

	// ---------------------------------------------------------------------------------------------------- ground

	/** The city disc (from the sea bed up to each ring's height), the harbour plaza and the scrapyard's land. */
	private void foundation(Canvas c) {
		for (int x = -R_WALL - 2; x <= YARD_X + YARD_RX + 8; x++) {
			for (int z = -R_WALL - 2; z <= R_WALL + 2; z++) {
				if (!c.inChunk(x, z)) {
					continue;
				}
				double r = Math.hypot(x, z);
				int bed = Math.min(c.ground(x, z), SEA - 4);
				if (r <= R_WALL_IN) {
					int g = groundAt(r);
					c.fill(x, bed, z, x, g - 1, z, s(Blocks.STONE));
					c.set(x, g, z, district(x, z, r));
					c.fill(x, g + 1, z, x, g + 6, z, s(Blocks.AIR));
				} else if (x > R_WALL - 4) {
					double e = yardEdge(x, z);
					if (e <= 1.0) {
						c.fill(x, bed, z, x, YARD_Y - 1, z, e > 0.9 ? s(Blocks.TUFF) : s(Blocks.DEEPSLATE));
						c.set(x, YARD_Y, z, yardGround(x, z, e));
						c.fill(x, YARD_Y + 1, z, x, YARD_Y + 8, z, s(Blocks.AIR));
					}
				}
			}
		}
	}

	/** Ground in the districts (overwritten later by roads, buildings and paving): pale stone with grass courts. */
	static BlockState district(int x, int z, double r) {
		if (r < R_COURT) {
			return Math.floorMod(x + z, 4) == 0 ? s(Blocks.QUARTZ_BRICKS) : s(Blocks.SMOOTH_STONE);
		}
		// Lawns in broad patches in the inner district; pale paving elsewhere, with a darker square grid.
		if (r < R_INNER && Math.sin(x * 0.09 + 0.5) + Math.sin(z * 0.08 + 1.7) > 0.6) {
			return s(Blocks.GRASS_BLOCK);
		}
		if (Math.floorMod(x, 9) == 0 && Math.floorMod(z, 9) <= 3 || Math.floorMod(z, 9) == 0 && Math.floorMod(x, 9) <= 3) {
			return s(Blocks.POLISHED_ANDESITE);
		}
		return unit(x, z, 7) < 0.85 ? s(Blocks.SMOOTH_STONE) : s(Blocks.STONE_BRICKS);
	}

	static double yardEdge(int x, int z) {
		double dx = (x - YARD_X) / (double) YARD_RX, dz = (z - YARD_Z) / (double) YARD_RZ;
		double ang = Math.atan2(dz, dx);
		double wob = 1 + 0.12 * Math.sin(3 * ang + 1) + 0.08 * Math.sin(5 * ang) + 0.05 * Math.sin(9 * ang + 2);
		return Math.sqrt(dx * dx + dz * dz) / wob;
	}

	/** Dark scrapyard ground with irregular rust patches. */
	static BlockState yardGround(int x, int z, double e) {
		double rust = Math.sin(x * 0.11 + 1.3) + Math.sin(z * 0.13 + x * 0.05) + Math.sin((x - z) * 0.07) + unit(x / 3, z / 3, 9);
		double u = unit(x, z, 10);
		if (rust > 1.9) {
			return u < 0.5 ? s(Blocks.ORANGE_TERRACOTTA) : u < 0.8 ? s(Blocks.EXPOSED_COPPER) : s(Blocks.CUT_COPPER);
		}
		if (e > 0.93) {
			return u < 0.6 ? s(Blocks.TUFF) : s(Blocks.DEEPSLATE);
		}
		// Broad regions of one dark ground each (low-frequency), with only a light scatter of other blocks.
		double region = unit(Math.floorDiv(x, 7), Math.floorDiv(z, 7), 11) * 0.7 + unit(Math.floorDiv(x + 3, 4), Math.floorDiv(z + 2, 4), 12) * 0.3;
		if (u < 0.12) {
			return u < 0.06 ? s(Blocks.TUFF) : s(Blocks.POLISHED_BASALT);
		}
		return region < 0.45 ? s(Blocks.GRAY_CONCRETE) : region < 0.7 ? s(Blocks.TUFF) : region < 0.85 ? s(Blocks.DEEPSLATE) : s(Blocks.BLACK_CONCRETE);
	}

	// ---------------------------------------------------------------------------------------------------- water

	/** The main canal (stone-lined, water at y 70) and the three outlet channels out through the wall to the sea. */
	private void canals(Canvas c) {
		for (int x = -R_WALL - 4; x <= R_WALL + 4; x++) {
			for (int z = -R_WALL - 4; z <= R_WALL + 4; z++) {
				if (!c.inChunk(x, z)) {
					continue;
				}
				double r = Math.hypot(x, z);
				if (r > R_INNER && r <= R_CANAL) {
					boolean edgeIn = r <= R_INNER + 2, edgeOut = r > R_CANAL - 2;
					if (edgeIn || edgeOut) {
						int top = edgeIn ? G_IN : G_OUT;
						c.fill(x, 64, z, x, top - 1, z, s(Blocks.STONE_BRICKS));
						c.set(x, top, z, s(Blocks.SMOOTH_STONE));
					} else {
						c.fill(x, 64, z, x, 65, z, s(Blocks.STONE_BRICKS));
						c.fill(x, 66, z, x, CANAL_WATER, z, s(Blocks.WATER));
						c.fill(x, CANAL_WATER + 1, z, x, G_IN + 6, z, s(Blocks.AIR));
					}
				}
				for (double deg : OUTLETS) {
					double[] p = radial(x, z, deg);
					if (p[1] < R_CANAL - 3 || p[1] > R_WALL + 1.5) {
						continue;
					}
					if (p[0] <= 3) {
						c.fill(x, 64, z, x, 65, z, s(Blocks.STONE_BRICKS));
						c.fill(x, 66, z, x, CANAL_WATER, z, s(Blocks.WATER));
						if (p[1] < R_WALL_IN) {
							c.fill(x, CANAL_WATER + 1, z, x, G_OUT + 6, z, s(Blocks.AIR));
						}
					} else if (p[0] <= 4.2 && p[1] > R_CANAL - 1 && p[1] < R_WALL_IN) {
						c.fill(x, 64, z, x, G_OUT - 1, z, s(Blocks.STONE_BRICKS));
						c.set(x, G_OUT, z, Kit.slab(Blocks.STONE_BRICK_SLAB, false));
					}
				}
			}
		}
		// The waterfalls: the outlets spill over the wall's foot into the sea.
		for (double deg : OUTLETS) {
			double t = Math.toRadians(deg), ux = Math.cos(t), uz = Math.sin(t);
			for (int k = -2; k <= 2; k++) {
				int x = (int) Math.round(ux * (R_WALL + 2) - uz * k), z = (int) Math.round(uz * (R_WALL + 2) + ux * k);
				if (!c.inChunk(x, z)) {
					continue;
				}
				BlockState falling = s(Blocks.WATER).setValue(LiquidBlock.LEVEL, 8);
				for (int y = CANAL_WATER; y >= SEA + 1; y--) {
					c.set(x, y, z, falling);
				}
			}
		}
	}

	// ---------------------------------------------------------------------------------------------------- roads

	/** Pale stone paving with arranged darker markings. */
	static BlockState paving(int x, int z, boolean marked) {
		if (marked) {
			return s(Blocks.POLISHED_ANDESITE);
		}
		double u = unit(x, z, 21);
		return u < 0.8 ? s(Blocks.SMOOTH_STONE) : u < 0.93 ? s(Blocks.STONE_BRICKS) : s(Blocks.ANDESITE);
	}

	/** Ring roads, radial streets, the inner lane by the wall, the fountain plazas, bridges over the canal and outlets. */
	private void roads(Canvas c) {
		for (int x = -R_WALL; x <= R_WALL; x++) {
			for (int z = -R_WALL; z <= R_WALL; z++) {
				if (!c.inChunk(x, z)) {
					continue;
				}
				double r = Math.hypot(x, z);
				if (r <= R_ENCL || r > R_WALL_IN) {
					continue;
				}
				double arc = Math.toRadians(angle(x, z)) * r;
				boolean ringA = r > R_ENCL && r <= R_ROAD_A, ringB = r > R_CANAL && r <= R_ROAD_B, ringC = r > R_MID && r <= R_ROAD_C;
				boolean lane = r > R_OUTER && r <= R_WALL_IN;
				boolean street = onStreet(x, z, 0);
				boolean plaza = inPlaza(x, z, 0);
				boolean canal = r > R_INNER && r <= R_CANAL;
				boolean outlet = inOutlet(x, z, 0.3);
				if (!(ringA || ringB || ringC || lane || street || plaza)) {
					continue;
				}
				int g = groundAt(r);
				if (canal || outlet && !(r < R_CANAL)) {
					if (street) {
						bridgeDeck(c, x, z, r, canal);
					}
					continue;
				}
				boolean marked;
				if (plaza) {
					int[] f = nearestFountain(x, z);
					double pr = Math.hypot(x - f[0], z - f[1]);
					marked = Math.floorMod(x - f[0], 6) == 0 && Math.floorMod(z - f[1], 6) <= 2 || Math.abs(pr - PLAZA_R + 1) < 0.6;
				} else if (street) {
					double best = 99;
					int deg0 = 0;
					for (int deg : STREETS) {
						double[] p = radial(x, z, deg);
						if (p[1] > 0 && p[0] < best) {
							best = p[0];
							deg0 = deg;
						}
					}
					double along = radial(x, z, deg0)[1];
					marked = best < 1.0 && Math.floorMod((int) Math.floor(along), 6) < 3;
				} else {
					double mid = ringA ? (R_ENCL + R_ROAD_A) / 2.0 : ringB ? (R_CANAL + R_ROAD_B) / 2.0 : ringC ? (R_MID + R_ROAD_C) / 2.0 : -1;
					marked = mid > 0 && Math.abs(r - mid) < 0.7 && Math.floorMod((int) Math.floor(arc), 6) < 3;
				}
				c.set(x, g, z, paving(x, z, marked));
				c.fill(x, g + 1, z, x, g + 5, z, s(Blocks.AIR));
			}
		}
	}

	static int[] nearestFountain(int x, int z) {
		int[] best = FOUNTAINS[0];
		for (int[] f : FOUNTAINS) {
			if (Math.hypot(x - f[0], z - f[1]) < Math.hypot(x - best[0], z - best[1])) {
				best = f;
			}
		}
		return best;
	}

	/** A street crossing the canal or an outlet: a stone bridge, its deck rising to y 74 over the water. */
	private static void bridgeDeck(Canvas c, int x, int z, double r, boolean canal) {
		int deck = canal ? G_IN : G_OUT + 1;
		if (canal && r > R_CANAL - 4) {
			deck = G_OUT + (int) Math.round((R_CANAL - r) / 2.0) + 0; // the outer end steps down to road B
		}
		deck = Math.max(G_OUT, Math.min(G_IN, deck));
		c.fill(x, CANAL_WATER + 2, z, x, deck - 1, z, s(Blocks.STONE_BRICKS));
		c.set(x, CANAL_WATER + 1, z, Kit.stairs(Blocks.STONE_BRICK_STAIRS, Direction.NORTH, true));
		c.set(x, deck, z, unit(x, z, 23) < 0.85 ? s(Blocks.SMOOTH_STONE) : s(Blocks.STONE_BRICKS));
		c.fill(x, deck + 1, z, x, deck + 4, z, s(Blocks.AIR));
	}

	// ---------------------------------------------------------------------------------------------------- walls

	/** The enclosure round the landmark: bricks, a pale stone band, three gates, the tall water passage south. */
	private void enclosure(Canvas c) {
		if (!c.near(0, 0, R_ENCL + 2)) {
			return;
		}
		for (int x = -R_ENCL; x <= R_ENCL; x++) {
			for (int z = -R_ENCL; z <= R_ENCL; z++) {
				double r = Math.hypot(x, z);
				if (r <= R_COURT || r > R_ENCL || !c.inChunk(x, z)) {
					continue;
				}
				for (int y = G_IN - 2; y <= ENCL_TOP; y++) {
					c.set(x, y, z, y == G_CT + 10 || y == ENCL_TOP ? s(Blocks.SMOOTH_STONE) : y < G_CT ? s(Blocks.STONE_BRICKS) : s(Blocks.BRICKS));
				}
				c.set(x, ENCL_TOP + 1, z, r > R_ENCL - 1 || r < R_COURT + 1.2 ? Kit.slab(Blocks.BRICK_SLAB, false) : s(Blocks.AIR));
			}
		}
		// Gates east, west and north, with steps up from road A to the courtyard.
		for (int[] g : new int[][]{{1, 0}, {-1, 0}, {0, -1}}) {
			for (int along = R_COURT - 1; along <= R_ENCL + 1; along++) {
				for (int k = -3; k <= 3; k++) {
					int x = g[0] != 0 ? g[0] * along : k, z = g[1] != 0 ? g[1] * along : k;
					int top = Math.abs(k) == 3 ? 7 : 8;
					int floor = along >= R_ENCL ? G_IN : along >= R_ENCL - 1 ? G_IN + 1 : G_CT;
					c.set(x, floor, z, s(Blocks.SMOOTH_STONE));
					c.fill(x, floor + 1, z, x, G_CT + top, z, s(Blocks.AIR));
					if (along == R_ENCL - 1 || along == R_ENCL) {
						Direction up = g[0] > 0 ? Direction.WEST : g[0] < 0 ? Direction.EAST : Direction.SOUTH;
						c.set(x, floor, z, along == R_ENCL ? Kit.stairs(Blocks.STONE_BRICK_STAIRS, up, false) : s(Blocks.SMOOTH_STONE));
					}
				}
				for (int k : new int[]{-4, 4}) {
					int x = g[0] != 0 ? g[0] * along : k, z = g[1] != 0 ? g[1] * along : k;
					c.fill(x, G_CT, z, x, G_CT + 9, z, s(Blocks.STONE_BRICKS));
				}
			}
		}
		// The water passage south: a tall pointed arch on pale supports, the channel through it.
		for (int z = R_COURT - 1; z <= R_ENCL + 1; z++) {
			for (int x = -6; x <= 6; x++) {
				int ax = Math.abs(x);
				if (ax >= 5) {
					c.fill(x, G_CT, z, x, G_CT + 17, z, ax == 6 ? s(Blocks.QUARTZ_PILLAR) : s(Blocks.SMOOTH_QUARTZ));
					continue;
				}
				int open = 16 - (ax >= 3 ? (ax - 2) * 2 : 0);
				c.fill(x, G_CT + 1, z, x, G_CT + open, z, s(Blocks.AIR));
				c.set(x, G_CT + open + 1, z, s(Blocks.SMOOTH_QUARTZ));
			}
		}
		channel(c);
	}

	/**
	 * The water channel from the landmark's south face, through the enclosure's passage and down the middle of the
	 * harbour avenue into the main canal, stepping down in small falls; road A passes over it.
	 */
	private static void channel(Canvas c) {
		if (!c.near(0, 46, 24)) {
			return;
		}
		for (int z = R_LANDMARK; z <= R_INNER + 1; z++) {
			int water = z <= R_ENCL ? G_CT - 1 : G_IN - 1;
			boolean road = z > R_ENCL && z <= R_ROAD_A;
			for (int x = -2; x <= 2; x++) {
				boolean edge = Math.abs(x) == 2;
				int g = z <= R_COURT ? G_CT : z <= R_INNER ? G_IN : G_OUT;
				if (edge) {
					c.fill(x, water - 2, z, x, g, z, s(Blocks.SMOOTH_QUARTZ));
					continue;
				}
				c.set(x, water - 1, z, s(Blocks.LIGHT_BLUE_STAINED_GLASS));
				c.set(x, water, z, s(Blocks.WATER));
				c.set(x, water + 1, z, road ? s(Blocks.SMOOTH_STONE) : s(Blocks.AIR));
				if (!road) {
					c.fill(x, water + 2, z, x, g + 3, z, s(Blocks.AIR));
				}
			}
		}
		// Falls into the canal.
		for (int x = -1; x <= 1; x++) {
			c.fill(x, CANAL_WATER + 1, R_INNER + 1, x, G_IN - 1, R_INNER + 1, s(Blocks.WATER).setValue(LiquidBlock.LEVEL, 8));
			c.set(x, G_IN - 2, R_INNER, s(Blocks.WATER));
		}
		// The falls where the passage meets the avenue (75 -> 73).
		for (int x = -1; x <= 1; x++) {
			c.set(x, G_CT - 1, R_ENCL + 1, s(Blocks.WATER).setValue(LiquidBlock.LEVEL, 8));
		}
	}

	/** The outer wall: bricks on a stone brick footing in the sea, a pale band, a stone cap; arches for the outlets. */
	private void outerWall(Canvas c) {
		for (int x = -R_WALL - 1; x <= R_WALL + 1; x++) {
			for (int z = -R_WALL - 1; z <= R_WALL + 1; z++) {
				double r = Math.hypot(x, z);
				if (r <= R_WALL_IN || r > R_WALL || !c.inChunk(x, z)) {
					continue;
				}
				// The gates: south (harbour) and east (scrapyard) openings.
				boolean southGate = z > 0 && Math.abs(x) <= GATE_HALF;
				boolean eastGate = x > 0 && Math.abs(z) <= 5;
				int bed = Math.min(c.ground(x, z), SEA - 4);
				for (int y = bed; y <= WALL_TOP; y++) {
					BlockState b;
					if (y < G_OUT) {
						b = s(Blocks.STONE_BRICKS);
					} else if (y == G_OUT + 12 || y == WALL_TOP) {
						b = s(Blocks.SMOOTH_STONE);
					} else {
						b = s(Blocks.BRICKS);
					}
					c.set(x, y, z, b);
				}
				c.set(x, WALL_TOP + 1, z, r > R_WALL - 1 ? Kit.slab(Blocks.BRICK_SLAB, false) : s(Blocks.AIR));
				if (southGate) {
					c.set(x, G_OUT, z, paving(x, z, false));
					c.fill(x, G_OUT + 1, z, x, G_OUT + 12, z, s(Blocks.AIR));
				}
				if (eastGate) {
					c.set(x, G_OUT, z, paving(x, z, Math.abs(z) <= 0));
					int open = Math.abs(z) >= 4 ? 8 : 10;
					c.fill(x, G_OUT + 1, z, x, G_OUT + open, z, s(Blocks.AIR));
				}
			}
		}
		// Outlet arches: a round stone opening over the water through the wall.
		for (double deg : OUTLETS) {
			double t = Math.toRadians(deg), ux = Math.cos(t), uz = Math.sin(t);
			for (double along = R_WALL_IN - 0.5; along <= R_WALL + 1; along += 0.5) {
				for (double k = -5.5; k <= 5.5; k += 0.5) {
					int x = (int) Math.round(ux * along - uz * k), z = (int) Math.round(uz * along + ux * k);
					if (!c.inChunk(x, z)) {
						continue;
					}
					double ak = Math.abs(k);
					int open = ak <= 3.5 ? CANAL_WATER + 1 + (int) Math.round(Math.sqrt(Math.max(0, 16 - ak * ak)) + 5) : 0;
					if (open > 0) {
						c.fill(x, CANAL_WATER + 1, z, x, open, z, s(Blocks.AIR));
						c.set(x, open + 1, z, s(Blocks.STONE_BRICKS));
						c.set(x, 64, z, s(Blocks.STONE_BRICKS));
						c.fill(x, 66, z, x, CANAL_WATER, z, ak <= 3 ? s(Blocks.WATER) : s(Blocks.STONE_BRICKS));
					} else if (ak <= 5) {
						c.fill(x, CANAL_WATER, z, x, CANAL_WATER + 10, z, s(Blocks.STONE_BRICKS));
					}
				}
			}
		}
	}

	// ---------------------------------------------------------------------------------------------------- landmark

	/**
	 * The central landmark: a stepped octagonal spire of white quartz. A 52-wide base with flying buttresses at its corners
	 * and tall lancet windows, an octagonal shaft, a crystal lantern room of blue and cyan glass round a core of sea
	 * lanterns, and a faceted blue spire with a quartz finial. On the south face a tall blue channel of falling water.
	 */
	private void landmark(Canvas c) {
		if (!c.near(0, 0, R_COURT + 2)) {
			return;
		}
		int y0 = G_CT;
		int baseTop = y0 + 30, shaftTop = 160, roomTop = 180, apex = 201;
		for (int x = -R_LANDMARK - 7; x <= R_LANDMARK + 7; x++) {
			for (int z = -R_LANDMARK - 7; z <= R_LANDMARK + 7; z++) {
				if (!c.inChunk(x, z)) {
					continue;
				}
				double o = oct(x, z);
				double ang = angle(x, z);
				// Base.
				if (o <= R_LANDMARK + 0.4) {
					boolean shell = o > R_LANDMARK - 1.6;
					c.fill(x, y0 - 3, z, x, y0, z, s(Blocks.STONE_BRICKS));
					for (int y = y0 + 1; y <= baseTop; y++) {
						c.set(x, y, z, shell ? wallBlock(x, y, z, o, R_LANDMARK, ang, y0 + 3, y0 + 22) : y == y0 + 1 ? floorBlock(x, z) : s(Blocks.AIR));
					}
					c.set(x, baseTop + 1, z, s(Blocks.SMOOTH_QUARTZ));
					if (o > R_LANDMARK - 1) {
						c.set(x, baseTop + 2, z, Kit.slab(Blocks.SMOOTH_QUARTZ_SLAB, false));
					}
				}
				// Flying buttresses at the eight corners (vertex directions 22.5 + 45k).
				double off = ((ang - 22.5) % 45 + 45) % 45;
				double r = Math.hypot(x, z);
				if ((off < 2.5 || off > 42.5) && r > R_LANDMARK && r <= R_LANDMARK + 6.5) {
					int top = baseTop - 2 - (int) Math.round((r - R_LANDMARK) * 3.5);
					int bottom = r > R_LANDMARK + 5 ? y0 + 1 : top - 3;
					for (int y = bottom; y <= top; y++) {
						c.set(x, y, z, y == top ? Kit.slab(Blocks.SMOOTH_QUARTZ_SLAB, false) : s(Blocks.SMOOTH_QUARTZ));
					}
					if (r > R_LANDMARK + 5) {
						c.set(x, y0, z, s(Blocks.STONE_BRICKS));
					}
				}
				// Shaft.
				double rs = 20;
				if (o <= rs + 0.4) {
					boolean shell = o > rs - 1.6;
					for (int y = baseTop + 1; y <= shaftTop; y++) {
						c.set(x, y, z, shell ? wallBlock(x, y, z, o, rs, ang, baseTop + 8, baseTop + 40) : s(Blocks.AIR));
					}
				}
				// Lantern room.
				double rr = 13;
				if (o <= rr + 0.4) {
					c.set(x, shaftTop + 1, z, o > rr - 1 ? s(Blocks.SMOOTH_QUARTZ) : s(Blocks.QUARTZ_BRICKS));
					boolean shell = o > rr - 1;
					boolean vertex = shell && Math.abs(((ang - 22.5) % 45 + 45) % 45 - 22.5) > 19;
					for (int y = shaftTop + 2; y <= roomTop; y++) {
						BlockState b;
						if (!shell) {
							b = Math.abs(x) <= 1 && Math.abs(z) <= 1 ? s(Blocks.SEA_LANTERN) : s(Blocks.AIR);
						} else if (vertex || y == roomTop) {
							b = s(Blocks.QUARTZ_PILLAR);
						} else {
							b = (y + (int) (ang / 45)) % 2 == 0 ? s(Blocks.LIGHT_BLUE_STAINED_GLASS) : s(Blocks.CYAN_STAINED_GLASS);
						}
						c.set(x, y, z, b);
					}
				}
				// Faceted spire.
				for (int y = roomTop + 1; y <= apex; y++) {
					double rk = 15 - (y - roomTop - 1) * 0.72;
					if (rk < 0.5) {
						if (x == 0 && z == 0) {
							c.set(x, y, z, s(Blocks.BLUE_CONCRETE));
						}
						continue;
					}
					if (o <= rk + 0.4 && o > rk - 1.4) {
						boolean rib = Math.abs(((ang - 22.5) % 45 + 45) % 45 - 22.5) > 18.5;
						c.set(x, y, z, rib ? s(Blocks.DARK_PRISMARINE) : s(Blocks.BLUE_CONCRETE));
					} else if (o <= rk - 1.4 && y == roomTop + 1) {
						c.set(x, y, z, s(Blocks.QUARTZ_BRICKS));
					}
				}
			}
		}
		c.fill(0, apex + 1, 0, 0, apex + 4, 0, s(Blocks.DIORITE_WALL));
		c.fill(0, apex + 5, 0, 0, apex + 7, 0, s(Blocks.END_ROD));
		// The base's floors: a gallery floor halfway up the hall, reached by a ladder.
		for (int x = -R_LANDMARK + 2; x <= R_LANDMARK - 2; x++) {
			for (int z = -R_LANDMARK + 2; z <= R_LANDMARK - 2; z++) {
				double o = oct(x, z);
				if (o <= R_LANDMARK - 1.6 && o > R_LANDMARK - 7) {
					c.set(x, y0 + 15, z, s(Blocks.SMOOTH_QUARTZ));
				}
			}
		}
		for (int y = y0 + 2; y <= y0 + 16; y++) {
			c.set(3, y, -R_LANDMARK + 2, Canvas.facing(s(Blocks.LADDER), Direction.SOUTH)); // against the wall between two windows
		}
		// Pillars and lanterns in the hall.
		for (int k = 0; k < 8; k++) {
			double t = Math.toRadians(k * 45 + 22.5);
			int px = (int) Math.round(Math.cos(t) * 12), pz = (int) Math.round(Math.sin(t) * 12);
			c.fill(px, y0 + 2, pz, px, baseTop, pz, s(Blocks.QUARTZ_PILLAR));
			c.set((int) Math.round(Math.cos(t) * 21), y0 + 14, (int) Math.round(Math.sin(t) * 21), CloudtopIsles.lantern(Blocks.LANTERN, true));
		}
		c.set(0, y0 + 2, 0, s(Blocks.SEA_LANTERN));
		c.fill(-1, y0 + 2, -1, 1, y0 + 2, 1, s(Blocks.CHISELED_QUARTZ_BLOCK));
		c.set(0, y0 + 3, 0, s(Blocks.LIGHT_BLUE_STAINED_GLASS));
		// Doors: arched openings east, west and north.
		for (int[] g : new int[][]{{1, 0}, {-1, 0}, {0, -1}}) {
			for (int along = R_LANDMARK - 2; along <= R_LANDMARK + 1; along++) {
				for (int k = -2; k <= 2; k++) {
					int x = g[0] != 0 ? g[0] * along : k, z = g[1] != 0 ? g[1] * along : k;
					int top = Math.abs(k) == 2 ? 5 : Math.abs(k) == 1 ? 6 : 7;
					c.fill(x, y0 + 2, z, x, y0 + top, z, s(Blocks.AIR));
					c.set(x, y0 + 1, z, s(Blocks.SMOOTH_QUARTZ));
				}
			}
		}
		// The tall blue channel on the south face: water falls in a recess backed with blue glass into a pool.
		for (int x = -2; x <= 2; x++) {
			for (int y = y0 + 1; y <= baseTop - 2; y++) {
				if (Math.abs(x) == 2) {
					c.set(x, y, R_LANDMARK, s(Blocks.QUARTZ_PILLAR));
					continue;
				}
				c.set(x, y, R_LANDMARK - 1, s(Blocks.BLUE_STAINED_GLASS));
				c.set(x, y, R_LANDMARK, y == baseTop - 2 ? s(Blocks.WATER) : s(Blocks.WATER).setValue(LiquidBlock.LEVEL, 8));
			}
			c.set(x, baseTop - 1, R_LANDMARK, s(Blocks.SMOOTH_QUARTZ));
		}
	}

	static BlockState floorBlock(int x, int z) {
		return (Math.floorMod(x, 4) == 0 || Math.floorMod(z, 4) == 0) ? s(Blocks.QUARTZ_BRICKS) : s(Blocks.SMOOTH_QUARTZ);
	}

	/**
	 * A landmark wall block: smooth quartz with calcite, trim bands, and two rows of tall lancet windows (blue panes,
	 * blackstone outlines, pointed tops) centred on each face.
	 */
	static BlockState wallBlock(int x, int y, int z, double o, double rad, double ang, int row1, int row2) {
		double faceAng = ((ang % 45) + 45) % 45; // 0 at a face's middle (faces on the axes and diagonals)
		double fromMid = Math.min(faceAng, 45 - faceAng); // degrees from the face's middle
		double across = Math.toRadians(fromMid) * rad; // blocks from the face's middle
		for (int row : new int[]{row1, row2}) {
			int h = 14;
			if (y >= row && y <= row + h + 2) {
				for (double centre : new double[]{0, 6}) {
					double dx = Math.abs(across - centre);
					if (centre == 6 && rad < 22) {
						continue;
					}
					int top = row + h - (dx > 0.6 ? 1 : 0);
					if (dx <= 1.1 && y <= top && y > row) {
						return Kit.connected(Blocks.LIGHT_BLUE_STAINED_GLASS_PANE, Math.abs(Math.cos(Math.toRadians(ang))) < 0.5);
					}
					if (dx <= 2.1 && y >= row && y <= top + 1) {
						return s(Blocks.POLISHED_BLACKSTONE);
					}
				}
			}
		}
		if (y == row1 - 2 || y == row2 - 2 || y == row1 + 18 || y == row2 + 18) {
			return s(Blocks.CHISELED_QUARTZ_BLOCK);
		}
		return unit3(x, y, z, 31) < 0.15 ? s(Blocks.CALCITE) : s(Blocks.SMOOTH_QUARTZ);
	}

	// ---------------------------------------------------------------------------------------------------- fountains

	/** Terrace radii (octagon) and tops above the plaza. */
	static final double[] TERRACE_R = {17, 13, 9.5, 6.5, 3.5};
	static final int[] TERRACE_H = {8, 16, 24, 31, 37};

	/**
	 * A fountain: a stepped octagonal cascade of five quartz terraces, each with its pool, water spilling over every face
	 * of every terrace in curtains to the pool below and into a basin ring at the foot; a crystal obelisk on top.
	 */
	private void fountain(Canvas c, int fx, int fz) {
		if (!c.near(fx, fz, PLAZA_R + 2)) {
			return;
		}
		int g = G_OUT;
		// Basin ring at the foot.
		for (int dx = -21; dx <= 21; dx++) {
			for (int dz = -21; dz <= 21; dz++) {
				double o = oct(dx, dz);
				int x = fx + dx, z = fz + dz;
				if (o > TERRACE_R[0] && o <= TERRACE_R[0] + 3.4) {
					c.set(x, g - 1, z, s(Blocks.SMOOTH_QUARTZ));
					c.set(x, g, z, s(Blocks.WATER));
					c.fill(x, g + 1, z, x, g + 3, z, s(Blocks.AIR));
				} else if (o > TERRACE_R[0] + 3.4 && o <= TERRACE_R[0] + 4.4) {
					c.set(x, g, z, s(Blocks.SMOOTH_QUARTZ));
					c.set(x, g + 1, z, Kit.slab(Blocks.SMOOTH_QUARTZ_SLAB, false));
				}
			}
		}
		for (int k = 0; k < TERRACE_R.length; k++) {
			double R = TERRACE_R[k];
			int top = g + TERRACE_H[k];
			int bottom = k == 0 ? g : g + TERRACE_H[k - 1];
			for (int dx = (int) -R - 1; dx <= R + 1; dx++) {
				for (int dz = (int) -R - 1; dz <= R + 1; dz++) {
					double o = oct(dx, dz);
					if (o > R + 0.4) {
						continue;
					}
					int x = fx + dx, z = fz + dz;
					boolean rim = o > R - 1.1;
					double ang = angle(dx, dz);
					boolean vertex = Math.abs(((ang - 22.5) % 45 + 45) % 45 - 22.5) > 17;
					for (int y = bottom; y < top; y++) {
						c.set(x, y, z, rim ? (vertex ? s(Blocks.QUARTZ_PILLAR) : y == top - 1 ? s(Blocks.SEA_LANTERN) : s(Blocks.SMOOTH_QUARTZ)) : s(Blocks.QUARTZ_BRICKS));
					}
					boolean pool = o <= R - 1.1 && (k == TERRACE_R.length - 1 || o > TERRACE_R[k + 1] + 0.5);
					if (rim) {
						c.set(x, top, z, vertex ? s(Blocks.QUARTZ_PILLAR) : s(Blocks.SMOOTH_QUARTZ));
					} else if (pool) {
						c.set(x, top - 1, z, s(Blocks.LIGHT_BLUE_STAINED_GLASS));
						c.set(x, top, z, s(Blocks.WATER));
					} else {
						c.set(x, top, z, s(Blocks.QUARTZ_BRICKS));
					}
				}
			}
			// Spillways: a notch in the middle of each face; a curtain of water down the face to the pool below.
			for (int f = 0; f < 8; f++) {
				double t = Math.toRadians(f * 45);
				double ux = Math.cos(t), uz = Math.sin(t);
				for (int w = -1; w <= 1; w++) {
					int nx = (int) Math.round(ux * (R - 0.5) - uz * w), nz = (int) Math.round(uz * (R - 0.5) + ux * w);
					int ox = (int) Math.round(ux * (R + 1) - uz * w), oz = (int) Math.round(uz * (R + 1) + ux * w);
					c.set(fx + nx, top, fz + nz, s(Blocks.WATER));
					int land = k == 0 ? g : g + TERRACE_H[k - 1];
					for (int y = top; y >= land; y--) {
						c.set(fx + ox, y, fz + oz, s(Blocks.WATER).setValue(LiquidBlock.LEVEL, 8));
					}
				}
			}
		}
		// The crystal obelisk.
		int base = g + TERRACE_H[TERRACE_H.length - 1] + 1;
		for (int y = base; y <= base + 50; y++) {
			int w = y < base + 20 ? 1 : y < base + 40 ? 1 : 0;
			for (int dx = -w; dx <= w; dx++) {
				for (int dz = -w; dz <= w; dz++) {
					boolean core = dx == 0 && dz == 0;
					BlockState b = core ? ((y - base) % 6 == 5 ? s(Blocks.SEA_LANTERN) : s(Blocks.QUARTZ_PILLAR))
							: (y - base) % 4 < 2 ? s(Blocks.LIGHT_BLUE_STAINED_GLASS) : s(Blocks.CYAN_STAINED_GLASS);
					c.set(fx + dx, y, fz + dz, b);
				}
			}
		}
		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) {
				c.set(fx + dx, base, fz + dz, Math.abs(dx) == 2 || Math.abs(dz) == 2 ? Kit.slab(Blocks.SMOOTH_QUARTZ_SLAB, false) : s(Blocks.SMOOTH_QUARTZ));
			}
		}
		c.set(fx, base + 51, fz, s(Blocks.SEA_LANTERN));
		c.set(fx, base + 52, fz, s(Blocks.END_ROD));
	}

	// ---------------------------------------------------------------------------------------------------- townhouses

	/**
	 * A townhouse: walls in its colour with dark oak frames and floor bands, tall light blue windows on every floor, a
	 * spruce door with a step, floors joined by a stair, a bed, table and lantern on each floor, and a steep roof (gable,
	 * or mansard on some large ones) with a chimney and a little round attic window.
	 */
	private static void townhouse(Canvas c, House h) {
		int x1 = h.x1(), x2 = h.x2(), z1 = h.z1(), z2 = h.z2(), y = h.y();
		int fh = 5, top = y + h.floors() * fh;
		c.fill(x1, y - 3, z1, x2, y, z2, s(Blocks.STONE_BRICKS));
		for (int x = x1; x <= x2; x++) {
			for (int z = z1; z <= z2; z++) {
				boolean ex = x == x1 || x == x2, ez = z == z1 || z == z2;
				if (!ex && !ez) {
					c.fill(x, y + 1, z, x, top + 12, z, s(Blocks.AIR));
					for (int f = 0; f < h.floors(); f++) {
						c.set(x, y + f * fh, z, s(Blocks.DARK_OAK_PLANKS));
					}
					c.set(x, top, z, s(Blocks.DARK_OAK_PLANKS));
					continue;
				}
				boolean corner = ex && ez;
				int along = ex ? z - z1 : x - x1;
				int span = (ex ? z2 - z1 : x2 - x1);
				for (int k = 1; k <= h.floors() * fh; k++) {
					int inF = k % fh;
					BlockState b;
					if (corner || inF == 0) {
						b = s(Blocks.DARK_OAK_PLANKS);
					} else if ((inF == 2 || inF == 3) && along % 3 == 1 && along > 0 && along < span) {
						b = Kit.connected(Blocks.LIGHT_BLUE_STAINED_GLASS_PANE, !ex);
					} else if (inF == 4 && along % 3 == 1 && along > 0 && along < span) {
						b = s(Blocks.DARK_OAK_PLANKS);
					} else {
						b = s(h.wall());
					}
					c.set(x, y + k, z, b);
				}
			}
		}
		// Door on the street side, a step and a lantern.
		Direction front = h.front();
		int dx = front == Direction.EAST ? x2 : front == Direction.WEST ? x1 : (x1 + x2) / 2;
		int dz = front == Direction.SOUTH ? z2 : front == Direction.NORTH ? z1 : (z1 + z2) / 2;
		Kit.door(c, dx, y + 1, dz, Blocks.SPRUCE_DOOR, front.getOpposite());
		c.set(dx, y + 3, dz, s(h.wall()));
		int ox = dx + front.getStepX(), oz = dz + front.getStepZ();
		c.set(ox, y, oz, s(Blocks.SMOOTH_STONE));
		c.fill(ox, y + 1, oz, ox, y + 3, oz, s(Blocks.AIR));
		Direction side = front.getClockWise();
		c.set(dx + side.getStepX() * 2 + front.getStepX(), y + 3, dz + side.getStepZ() * 2 + front.getStepZ(), s(Blocks.AIR));
		c.set(dx + side.getStepX() * 2, y + 3, dz + side.getStepZ() * 2, s(Blocks.DARK_OAK_PLANKS));
		// Inside: a stair up each floor along the back wall, furniture, lanterns.
		Direction back = front.getOpposite();
		boolean alongX = front.getAxis() == Direction.Axis.Z; // the back wall runs along x
		for (int f = 0; f < h.floors(); f++) {
			int fy = y + f * fh;
			if (f < h.floors() - 1) {
				for (int k = 1; k <= fh; k++) {
					int sx, sz;
					Direction climb;
					if (alongX) {
						sx = x1 + k;
						sz = back == Direction.NORTH ? z1 + 1 : z2 - 1;
						climb = Direction.EAST;
					} else {
						sz = z1 + k;
						sx = back == Direction.WEST ? x1 + 1 : x2 - 1;
						climb = Direction.SOUTH;
					}
					c.set(sx, fy + k, sz, Kit.stairs(Blocks.SPRUCE_STAIRS, climb, false));
					c.fill(sx, fy + k + 1, sz, sx, fy + k + 3, sz, s(Blocks.AIR));
					if (k < fh) {
						c.fill(sx, fy + 1, sz, sx, fy + k - 1, sz, s(Blocks.DARK_OAK_PLANKS));
					}
				}
			}
			// Furniture in the front corner opposite the stair.
			int bx = alongX ? x2 - 2 : (front == Direction.EAST ? x2 - 2 : x1 + 2);
			int bz = alongX ? (front == Direction.SOUTH ? z2 - 2 : z1 + 2) : z2 - 2;
			if (f % 2 == 1 || h.floors() == 2 && f == 1) {
				Direction bedDir = alongX ? Direction.NORTH : Direction.WEST;
				BlockState bed = s(Blocks.BLUE_BED).setValue(BedBlock.FACING, bedDir);
				c.set(bx, fy + 1, bz, bed.setValue(BedBlock.PART, BedPart.FOOT));
				c.set(bx + bedDir.getStepX(), fy + 1, bz + bedDir.getStepZ(), bed.setValue(BedBlock.PART, BedPart.HEAD));
			} else {
				c.set(bx, fy + 1, bz, s(Blocks.SPRUCE_FENCE));
				c.set(bx, fy + 2, bz, s(Blocks.SPRUCE_PRESSURE_PLATE));
				c.set(bx - (alongX ? 1 : 0), fy + 1, bz - (alongX ? 0 : 1), Kit.stairs(Blocks.SPRUCE_STAIRS, alongX ? Direction.EAST : Direction.SOUTH, false));
			}
			c.set((x1 + x2) / 2, fy + fh - 1, (z1 + z2) / 2, CloudtopIsles.lantern(Blocks.LANTERN, true));
		}
		roof(c, h, top);
	}

	/** A steep gable (or a mansard) along the house's longer side, a chimney, a round attic window in each gable end. */
	private static void roof(Canvas c, House h, int top) {
		int x1 = h.x1() - 1, x2 = h.x2() + 1, z1 = h.z1() - 1, z2 = h.z2() + 1;
		boolean ridgeX = (x2 - x1) >= (z2 - z1);
		int span = ridgeX ? z2 - z1 : x2 - x1;
		int half = span / 2;
		Block stair = ROOF_STAIRS[h.roof()], full = ROOF_FULL[h.roof()];
		boolean stairs = stair != Blocks.BLUE_TERRACOTTA;
		for (int i = 0; i <= half; i++) {
			int yy = top + 1 + (h.mansard() && i > 1 ? 1 + (i - 1) / 2 : i);
			if (h.mansard() && i <= 1) {
				yy = top + 1 + i * 2;
			}
			for (int a = (ridgeX ? x1 : z1); a <= (ridgeX ? x2 : z2); a++) {
				int[] lo = ridgeX ? new int[]{a, z1 + i} : new int[]{x1 + i, a};
				int[] hi = ridgeX ? new int[]{a, z2 - i} : new int[]{x2 - i, a};
				Direction upLo = ridgeX ? Direction.SOUTH : Direction.EAST;
				boolean end = a == (ridgeX ? x1 : z1) || a == (ridgeX ? x2 : z2);
				if (lo[0] == hi[0] && lo[1] == hi[1] || (ridgeX ? lo[1] > hi[1] : lo[0] > hi[0])) {
					c.set(lo[0], yy, lo[1], stairs ? Kit.slab(slabOf(stair), false) : s(full));
					continue;
				}
				c.set(lo[0], yy, lo[1], stairs ? Kit.stairs(stair, upLo, false) : s(full));
				c.set(hi[0], yy, hi[1], stairs ? Kit.stairs(stair, upLo.getOpposite(), false) : s(full));
				if (h.mansard() && i <= 1) {
					c.set(lo[0], yy - 1, lo[1], s(full));
					c.set(hi[0], yy - 1, hi[1], s(full));
				}
				// Gable ends: wall up to the roof, a round attic window in the middle.
				if (end) {
					int from = ridgeX ? lo[1] + 1 : lo[0] + 1, to = ridgeX ? hi[1] - 1 : hi[0] - 1;
					for (int b = from; b <= to; b++) {
						int ex = ridgeX ? a + (a == x1 ? 1 : -1) : b, ez = ridgeX ? b : a + (a == z1 ? 1 : -1);
						for (int yb = top + 1; yb <= yy; yb++) {
							c.set(ex, yb, ez, s(h.wall()));
						}
					}
				}
			}
		}
		// The round attic windows.
		int mx = (h.x1() + h.x2()) / 2, mz = (h.z1() + h.z2()) / 2;
		for (int end : ridgeX ? new int[]{h.x1(), h.x2()} : new int[]{h.z1(), h.z2()}) {
			int wx = ridgeX ? end : mx, wz = ridgeX ? mz : end;
			int wy = top + 2;
			c.set(wx, wy, wz, Kit.connected(Blocks.LIGHT_BLUE_STAINED_GLASS_PANE, !ridgeX));
			c.set(wx, wy + 1, wz, s(Blocks.DARK_OAK_PLANKS));
			c.set(wx, wy - 1, wz, s(Blocks.DARK_OAK_PLANKS));
		}
		// Chimney at one end of the ridge.
		int cx = ridgeX ? h.x1() + 1 : mx + 1, cz = ridgeX ? mz + 1 : h.z1() + 1;
		int ridge = top + 1 + (h.mansard() ? 3 + (half - 1) / 2 : half);
		c.fill(cx, top + 1, cz, cx, ridge + 2, cz, s(Blocks.BRICKS));
	}

	static Block slabOf(Block stair) {
		if (stair == Blocks.BRICK_STAIRS) {
			return Blocks.BRICK_SLAB;
		}
		if (stair == Blocks.DARK_PRISMARINE_STAIRS) {
			return Blocks.DARK_PRISMARINE_SLAB;
		}
		return Blocks.DEEPSLATE_TILE_SLAB;
	}

	/** A small garden in the inner district: grass, a compact angular tree, a bench. */
	private static void garden(Canvas c, int x, int z) {
		if (!c.near(x, z, 6)) {
			return;
		}
		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) {
				c.set(x + dx, G_IN, z + dz, s(Blocks.GRASS_BLOCK));
			}
		}
		boolean spruce = unit(x, z, 3) < 0.5;
		Block log = spruce ? Blocks.SPRUCE_LOG : Blocks.DARK_OAK_LOG, leaves = spruce ? Blocks.SPRUCE_LEAVES : Blocks.DARK_OAK_LEAVES;
		c.fill(x, G_IN + 1, z, x, G_IN + 5, z, s(log));
		for (int dy = 3; dy <= 7; dy++) {
			int r = dy <= 4 ? 2 : dy <= 6 ? 1 : 0;
			for (int dx = -r; dx <= r; dx++) {
				for (int dz = -r; dz <= r; dz++) {
					if ((dx != 0 || dz != 0 || dy > 5) && Math.abs(dx) + Math.abs(dz) <= r + 1) {
						c.set(x + dx, G_IN + dy, z + dz, Kit.leaves(leaves));
					}
				}
			}
		}
		c.set(x + 2, G_IN + 1, z, Kit.stairs(Blocks.SPRUCE_STAIRS, Direction.EAST, false));
		c.set(x + 2, G_IN + 1, z + 1, Kit.stairs(Blocks.SPRUCE_STAIRS, Direction.EAST, false));
	}

	/** Lamp posts along the ring roads. */
	private static void lamps(Canvas c) {
		for (int[] ring : new int[][]{{R_ROAD_A, G_IN}, {R_ROAD_B, G_OUT}, {R_ROAD_C, G_OUT}}) {
			double r = ring[0] - 1;
			for (int k = 0; k < 360; k += 15) {
				double t = Math.toRadians(k + 7.5);
				int x = (int) Math.round(Math.cos(t) * r), z = (int) Math.round(Math.sin(t) * r);
				if (c.inChunk(x, z) && !onStreet(x, z, 1) && !inOutlet(x, z, 1) && !inPlaza(x, z, 1)) {
					CloudtopIsles.lampPost(c, x, ring[1], z);
				}
			}
		}
	}

	// ---------------------------------------------------------------------------------------------------- harbour

	/**
	 * The harbour: the staircase from the gate down to the arrival plaza (pale paving, darker markings, a raised centre
	 * piece, market stalls and barrels, two side stairs down to boat landings), three piers with hoists, and the two
	 * harbour towers.
	 */
	private void harbour(Canvas c) {
		if (!c.near(0, 200, 70)) {
			return;
		}
		// The plaza.
		for (int x = -PLAZA_X; x <= PLAZA_X; x++) {
			for (int z = PLAZA_Z1; z <= PLAZA_Z2; z++) {
				if (!c.inChunk(x, z)) {
					continue;
				}
				int bed = Math.min(c.ground(x, z), SEA - 4);
				boolean rim = Math.abs(x) == PLAZA_X || z == PLAZA_Z2;
				c.fill(x, bed, z, x, PLAZA_Y - 1, z, s(Blocks.STONE_BRICKS));
				boolean marked = Math.floorMod(x, 10) >= 2 && Math.floorMod(x, 10) <= 7 && (Math.floorMod(z - PLAZA_Z1, 8) == 3)
						&& Math.abs(x) > 16;
				c.set(x, PLAZA_Y, z, rim ? s(Blocks.STONE_BRICKS) : marked ? s(Blocks.POLISHED_ANDESITE) : paving(x, z, false));
				c.fill(x, PLAZA_Y + 1, z, x, PLAZA_Y + 8, z, s(Blocks.AIR));
				if (rim && z == PLAZA_Z2 && Math.abs(x) > 6 && !(Math.abs(Math.abs(x) - 30) <= 4)) {
					c.set(x, PLAZA_Y + 1, z, Kit.slab(Blocks.STONE_BRICK_SLAB, false));
				}
			}
		}
		// The main staircase (30 wide) from the plaza up to the gate.
		for (int x = -GATE_HALF; x < GATE_HALF; x++) {
			for (int i = 0; i < G_OUT - PLAZA_Y; i++) {
				int z = PLAZA_Z1 + (G_OUT - PLAZA_Y) - i, yy = PLAZA_Y + 1 + i;
				c.fill(x, PLAZA_Y, z, x, yy - 1, z, s(Blocks.STONE_BRICKS));
				c.set(x, yy, z, Kit.stairs(Blocks.STONE_BRICK_STAIRS, Direction.NORTH, false));
				c.fill(x, yy + 1, z, x, yy + 5, z, s(Blocks.AIR));
			}
			c.set(x, G_OUT, PLAZA_Z1, paving(x, PLAZA_Z1, false));
		}
		for (int i = 0; i <= G_OUT - PLAZA_Y; i++) {
			int z = PLAZA_Z1 + (G_OUT - PLAZA_Y) - i;
			for (int x : new int[]{-GATE_HALF - 1, GATE_HALF}) {
				c.fill(x, PLAZA_Y, z, x, PLAZA_Y + 1 + i, z, s(Blocks.STONE_BRICKS));
				c.set(x, PLAZA_Y + 2 + i, z, Kit.slab(Blocks.STONE_BRICK_SLAB, false));
			}
		}
		// The raised centre piece: an octagonal dais with a small crystal fountain.
		for (int dx = -5; dx <= 5; dx++) {
			for (int dz = -5; dz <= 5; dz++) {
				double o = oct(dx, dz);
				int x = dx, z = 182 + dz;
				if (o <= 5.4) {
					c.set(x, PLAZA_Y + 1, z, o > 4.4 ? Kit.stairs(Blocks.STONE_BRICK_STAIRS, inward(dx, dz), false) : s(Blocks.SMOOTH_STONE));
					if (o <= 3.4 && o > 2.4) {
						c.set(x, PLAZA_Y + 2, z, s(Blocks.SMOOTH_QUARTZ));
					} else if (o <= 2.4) {
						c.set(x, PLAZA_Y + 1, z, s(Blocks.SMOOTH_QUARTZ));
						c.set(x, PLAZA_Y + 2, z, s(Blocks.WATER));
					}
				}
			}
		}
		c.fill(0, PLAZA_Y + 2, 182, 0, PLAZA_Y + 4, 182, s(Blocks.QUARTZ_PILLAR));
		c.set(0, PLAZA_Y + 5, 182, s(Blocks.SEA_LANTERN));
		c.set(0, PLAZA_Y + 6, 182, s(Blocks.CYAN_STAINED_GLASS));
		// Market stalls and barrels at the edges.
		for (int sx : new int[]{-1, 1}) {
			for (int z : new int[]{170, 186}) {
				stall(c, sx * 38, z, z == 170 ? Blocks.RED_WOOL : Blocks.BLUE_WOOL);
			}
			for (int z = 176; z <= 179; z++) {
				c.set(sx * 42, PLAZA_Y + 1, z, Canvas.facing(s(Blocks.BARREL), Direction.UP));
				if (z % 2 == 0) {
					c.set(sx * 42, PLAZA_Y + 2, z, Canvas.facing(s(Blocks.BARREL), Direction.UP));
				}
			}
			// Side stairs down to a boat landing.
			for (int i = 0; i < 3; i++) {
				int z = PLAZA_Z2 + 1 + i;
				for (int x = sx * 40 - 2; x <= sx * 40 + 2; x++) {
					c.fill(x, Math.min(c.ground(x, z), SEA - 4), z, x, PLAZA_Y - 1 - i, z, s(Blocks.STONE_BRICKS));
					c.set(x, PLAZA_Y - i, z, Kit.stairs(Blocks.STONE_BRICK_STAIRS, Direction.NORTH, false));
				}
			}
			for (int x = sx * 40 - 2; x <= sx * 40 + 2; x++) {
				c.set(x, PLAZA_Y + 1, PLAZA_Z2, s(Blocks.AIR));
				c.set(x, PLAZA_Y, PLAZA_Z2, paving(x, PLAZA_Z2, false));
			}
		}
		pier(c, -5, 10, 55);
		pier(c, -34, 8, 48);
		pier(c, 27, 8, 48);
		harbourTower(c, -58);
		harbourTower(c, 47);
	}

	static Direction inward(int dx, int dz) {
		return Math.abs(dx) >= Math.abs(dz) ? (dx > 0 ? Direction.WEST : Direction.EAST) : (dz > 0 ? Direction.NORTH : Direction.SOUTH);
	}

	/** A market stall on the plaza: four posts, a wool awning, a counter and a barrel. */
	private static void stall(Canvas c, int x, int z, Block wool) {
		int y = PLAZA_Y;
		for (int dx : new int[]{-1, 1}) {
			for (int dz : new int[]{-1, 1}) {
				c.fill(x + dx, y + 1, z + dz, x + dx, y + 3, z + dz, s(Blocks.SPRUCE_FENCE));
			}
		}
		c.fill(x - 2, y + 4, z - 2, x + 2, y + 4, z + 2, s(wool));
		c.set(x, y + 1, z, s(Blocks.SPRUCE_PLANKS));
		c.set(x, y + 1, z + 1, Canvas.facing(s(Blocks.BARREL), Direction.UP));
	}

	/** A pier of pale stone (x from x1, width w, length len from the plaza), stone piers below, a hoist near the end. */
	private static void pier(Canvas c, int x1, int w, int len) {
		int z1 = PLAZA_Z2 + 1, z2 = PLAZA_Z2 + len;
		for (int x = x1; x < x1 + w; x++) {
			for (int z = z1; z <= z2; z++) {
				if (!c.inChunk(x, z)) {
					continue;
				}
				boolean edge = x == x1 || x == x1 + w - 1 || z == z2;
				c.set(x, PIER_Y, z, edge ? s(Blocks.STONE_BRICKS) : unit(x, z, 41) < 0.8 ? s(Blocks.SMOOTH_STONE) : s(Blocks.POLISHED_ANDESITE));
				c.set(x, PIER_Y - 1, z, s(Blocks.STONE_BRICKS));
				c.fill(x, PIER_Y + 1, z, x, PIER_Y + 4, z, s(Blocks.AIR));
				if (edge && (z - z1) % 6 == 0) {
					c.fill(x, Math.min(c.ground(x, z), SEA - 4), z, x, PIER_Y - 2, z, s(Blocks.STONE_BRICKS));
				}
				if (edge && (x == x1 || x == x1 + w - 1) && (z - z1) % 6 != 0) {
					c.set(x, PIER_Y + 1, z, Kit.slab(Blocks.STONE_BRICK_SLAB, false));
				}
			}
		}
		// The hoist: two posts, a beam across, chains with a crate.
		int hz = z2 - 4;
		for (int x : new int[]{x1 + 1, x1 + w - 2}) {
			c.fill(x, PIER_Y + 1, hz, x, PIER_Y + 7, hz, s(Blocks.STRIPPED_SPRUCE_LOG));
		}
		for (int x = x1; x < x1 + w; x++) {
			c.set(x, PIER_Y + 8, hz, s(Blocks.STRIPPED_SPRUCE_LOG).setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
		}
		int mx = x1 + w / 2;
		c.fill(mx, PIER_Y + 5, hz, mx, PIER_Y + 7, hz, s(Blocks.CHAIN));
		c.set(mx, PIER_Y + 4, hz, Canvas.facing(s(Blocks.BARREL), Direction.UP));
		c.set(x1, PIER_Y + 1, hz, s(Blocks.DARK_OAK_FENCE));
		c.set(x1 + w - 1, PIER_Y + 1, hz, s(Blocks.DARK_OAK_FENCE));
	}

	/**
	 * A harbour tower: a 12 x 12 calcite shaft with smooth quartz corners, narrow dark windows behind iron bars, a 16 x 16
	 * upper chamber on corbels, a steep green pyramid roof with dark prismarine hips, an iron bar finial, a dark emblem on
	 * the sea face; a door and a ladder inside.
	 */
	private static void harbourTower(Canvas c, int x1) {
		int z1 = 168, x2 = x1 + 11, z2 = z1 + 11, y = PLAZA_Y;
		if (!c.near(x1 + 6, z1 + 6, 14)) {
			return;
		}
		int shaftTop = y + 20, chamTop = y + 27;
		for (int x = x1; x <= x2; x++) {
			for (int z = z1; z <= z2; z++) {
				c.fill(x, Math.min(c.ground(x, z), SEA - 4), z, x, y, z, s(Blocks.STONE_BRICKS));
				boolean ex = x == x1 || x == x2, ez = z == z1 || z == z2;
				for (int yy = y + 1; yy <= shaftTop; yy++) {
					if (!ex && !ez) {
						c.set(x, yy, z, s(Blocks.AIR));
						continue;
					}
					int along = ex ? z - z1 : x - x1;
					boolean window = !(ex && ez) && (along == 4 || along == 7) && (yy - y) % 6 >= 2 && (yy - y) % 6 <= 4;
					c.set(x, yy, z, ex && ez ? s(Blocks.SMOOTH_QUARTZ) : window ? Kit.connected(Blocks.IRON_BARS, !ex) : yy <= y + 2 ? s(Blocks.STONE_BRICKS)
							: s(Blocks.CALCITE));
				}
			}
		}
		// Corbels and the wider chamber.
		for (int x = x1 - 2; x <= x2 + 2; x++) {
			for (int z = z1 - 2; z <= z2 + 2; z++) {
				boolean ex = x == x1 - 2 || x == x2 + 2, ez = z == z1 - 2 || z == z2 + 2;
				boolean inShaft = x >= x1 && x <= x2 && z >= z1 && z <= z2;
				if (!inShaft) {
					c.set(x, shaftTop, z, Kit.stairs(Blocks.QUARTZ_STAIRS, inward(x - (x1 + x2) / 2, z - (z1 + z2) / 2), true));
				}
				c.set(x, shaftTop + 1, z, s(Blocks.SMOOTH_QUARTZ));
				for (int yy = shaftTop + 2; yy <= chamTop; yy++) {
					if (!ex && !ez) {
						c.set(x, yy, z, s(Blocks.AIR));
						continue;
					}
					int along = ex ? z - (z1 - 2) : x - (x1 - 2);
					boolean window = !(ex && ez) && along % 3 == 1 && yy >= shaftTop + 3 && yy <= shaftTop + 5;
					c.set(x, yy, z, ex && ez ? s(Blocks.SMOOTH_QUARTZ) : window ? s(Blocks.BLACK_STAINED_GLASS) : s(Blocks.CALCITE));
				}
			}
		}
		// Steep roof: 12 high over the 16 x 16 chamber (two layers per step in), hips of dark prismarine.
		for (int i = 0; i <= 8; i++) {
			int a1x = x1 - 2 + i, a2x = x2 + 2 - i, a1z = z1 - 2 + i, a2z = z2 + 2 - i;
			for (int layer = 0; layer < (i < 4 ? 1 : 2); layer++) {
				int yy = chamTop + 1 + (i < 4 ? i : 4 + (i - 4) * 2 + layer);
				if (a1x > a2x) {
					break;
				}
				for (int x = a1x; x <= a2x; x++) {
					for (int z = a1z; z <= a2z; z++) {
						boolean rim = x == a1x || x == a2x || z == a1z || z == a2z;
						boolean hip = (x == a1x || x == a2x) && (z == a1z || z == a2z);
						if (rim) {
							c.set(x, yy, z, hip ? s(Blocks.DARK_PRISMARINE) : s(Blocks.GREEN_TERRACOTTA));
						}
					}
				}
			}
		}
		int cxm = (x1 + x2) / 2, czm = (z1 + z2) / 2;
		c.fill(cxm, chamTop + 13, czm, cxm, chamTop + 16, czm, s(Blocks.IRON_BARS));
		// The dark emblem on the sea face (south): a ring with a diamond.
		for (int a = -3; a <= 3; a++) {
			for (int b = -3; b <= 3; b++) {
				double d = Math.hypot(a, b);
				boolean ring = d > 2.4 && d <= 3.4, diamond = Math.abs(a) + Math.abs(b) <= 1;
				if (ring || diamond) {
					c.set(cxm + a, y + 13 + b, z2, s(Blocks.BLACK_CONCRETE));
				}
			}
		}
		// Door toward the plaza and a ladder.
		boolean west = x1 < 0;
		int dx = west ? x2 : x1;
		Kit.door(c, dx, y + 1, czm, Blocks.SPRUCE_DOOR, west ? Direction.WEST : Direction.EAST);
		for (int yy = y + 1; yy <= shaftTop + 1; yy++) {
			c.set(cxm, yy, z1 + 1, Canvas.facing(s(Blocks.LADDER), Direction.SOUTH));
		}
		c.set(cxm + 2, shaftTop + 2, czm, CloudtopIsles.lantern(Blocks.LANTERN, false));
	}

	/**
	 * The gateway: two 13 x 13 white towers on stone brick bases straddling the outer wall, three levels of tall blue
	 * windows, green pyramid roofs, hanging banners; a white bridge between them over the open passage with narrow
	 * pointed windows, a big round blue window, a tall pointed central roof and two sloping wings, lanterns beneath.
	 */
	private void gateway(Canvas c) {
		if (!c.near(0, 154, 40)) {
			return;
		}
		int y = G_OUT, z1 = 147, z2 = 159;
		for (int x1 : new int[]{-28, 16}) {
			int x2 = x1 + 12;
			for (int x = x1; x <= x2; x++) {
				for (int z = z1; z <= z2; z++) {
					c.fill(x, Math.min(c.ground(x, z), SEA - 4), z, x, y, z, s(Blocks.STONE_BRICKS));
					boolean ex = x == x1 || x == x2, ez = z == z1 || z == z2;
					for (int k = 1; k <= 24; k++) {
						if (!ex && !ez) {
							c.set(x, y + k, z, k % 8 == 0 ? s(Blocks.SMOOTH_QUARTZ) : s(Blocks.AIR));
							continue;
						}
						int along = ex ? z - z1 : x - x1;
						int inF = (k - 1) % 8;
						boolean window = !(ex && ez) && (along == 4 || along == 8) && inF >= 2 && inF <= 6;
						boolean point = !(ex && ez) && (along == 4 || along == 8) && inF == 7;
						c.set(x, y + k, z, ex && ez ? s(Blocks.QUARTZ_PILLAR) : k <= 3 ? s(Blocks.STONE_BRICKS)
								: window ? (inF == 6 ? s(Blocks.LIGHT_BLUE_STAINED_GLASS) : s(Blocks.BLUE_STAINED_GLASS))
								: point ? s(Blocks.POLISHED_BLACKSTONE) : s(Blocks.SMOOTH_QUARTZ));
					}
				}
			}
			// Green pyramid roof (8 high).
			for (int i = 0; i <= 7; i++) {
				for (int x = x1 - 1 + i; x <= x2 + 1 - i; x++) {
					for (int z = z1 - 1 + i; z <= z2 + 1 - i; z++) {
						boolean rim = x == x1 - 1 + i || x == x2 + 1 - i || z == z1 - 1 + i || z == z2 + 1 - i;
						if (rim) {
							c.set(x, y + 25 + i, z, s(Blocks.DARK_PRISMARINE));
						}
					}
				}
			}
			c.fill((x1 + x2) / 2, y + 33, (z1 + z2) / 2, (x1 + x2) / 2, y + 35, (z1 + z2) / 2, s(Blocks.DIORITE_WALL));
			// Banners on the upper level, facing the harbour.
			for (int bx : new int[]{x1 + 3, x2 - 3}) {
				c.set(bx, y + 20, z2 + 1, s(Blocks.BLUE_WALL_BANNER).setValue(WallBannerBlock.FACING, Direction.SOUTH));
			}
			// Doors into the towers from the passage.
			int dx = x1 < 0 ? x2 : x1;
			Kit.door(c, dx, y + 1, 153, Blocks.SPRUCE_DOOR, x1 < 0 ? Direction.WEST : Direction.EAST);
			for (int k = 1; k <= 23; k++) {
				c.set(x1 < 0 ? x1 + 1 : x2 - 1, y + k, z1 + 1, Canvas.facing(s(Blocks.LADDER), Direction.SOUTH));
			}
		}
		// The bridge over the passage (x -15..15), its underside from y 85.
		int by = y + 13, bTop = y + 23;
		for (int x = -15; x <= 15; x++) {
			for (int z = z1 + 2; z <= z2 - 2; z++) {
				boolean ez = z == z1 + 2 || z == z2 - 2;
				c.set(x, by, z, s(Blocks.SMOOTH_QUARTZ));
				for (int yy = by + 1; yy <= bTop; yy++) {
					BlockState b = s(Blocks.AIR);
					if (ez) {
						boolean lancet = Math.floorMod(x, 4) == 2 && yy >= by + 3 && yy <= by + 7 && Math.abs(x) > 5;
						double rose = Math.hypot(x, yy - (by + 6));
						if (rose <= 3.4) {
							b = rose > 2.4 ? s(Blocks.POLISHED_BLACKSTONE) : (x + yy) % 2 == 0 ? s(Blocks.BLUE_STAINED_GLASS) : s(Blocks.LIGHT_BLUE_STAINED_GLASS);
						} else {
							b = lancet ? s(Blocks.LIGHT_BLUE_STAINED_GLASS) : s(Blocks.SMOOTH_QUARTZ);
						}
					} else if (yy == by + 1) {
						b = s(Blocks.SMOOTH_QUARTZ);
					}
					c.set(x, yy, z, b);
				}
				c.set(x, by - 1, z, Math.abs(x) % 5 == 0 ? Kit.stairs(Blocks.QUARTZ_STAIRS, Direction.NORTH, true) : s(Blocks.AIR));
			}
			// Roof: a tall pointed centre (x -6..6) and two lower wings sloping toward the towers.
			int ax = Math.abs(x);
			int roofTop = ax <= 6 ? bTop + 1 + (7 - ax) * 2 : bTop + 1 + Math.max(0, (15 - ax) / 3);
			for (int z = z1 + 1; z <= z2 - 1; z++) {
				for (int yy = bTop + 1; yy <= roofTop; yy++) {
					boolean surface = yy == roofTop;
					c.set(x, yy, z, surface ? s(Blocks.DARK_PRISMARINE) : s(Blocks.SMOOTH_QUARTZ));
				}
			}
			if (ax % 6 == 3) {
				c.set(x, by - 1, (z1 + z2) / 2, CloudtopIsles.lantern(Blocks.LANTERN, true));
			}
		}
		c.fill(0, bTop + 17, (z1 + z2) / 2, 0, bTop + 19, (z1 + z2) / 2, s(Blocks.DIORITE_WALL));
		// The passage floor through the wall.
		for (int x = -15; x <= 14; x++) {
			for (int z = z1 - 4; z <= z2 + 1; z++) {
				if (c.inChunk(x, z) && Math.hypot(x, z) > R_OUTER) {
					c.set(x, G_OUT, z, paving(x, z, Math.abs(x) <= 1 && Math.floorMod(z, 6) < 3));
					c.fill(x, G_OUT + 1, z, x, by - 2, z, s(Blocks.AIR));
				}
			}
		}
	}

	// ---------------------------------------------------------------------------------------------------- scrapyard

	/**
	 * The scrapyard: a ramp down from the east gate, four great cranes with hanging cargo, gears standing and half
	 * buried, broken pipes, coloured cables across the ground, scrap heaps, and the foundry engine in the middle.
	 */
	private void scrapyard(Canvas c) {
		if (!c.near(YARD_X, YARD_Z, YARD_RX + 30)) {
			return;
		}
		// The ramp from the gate (y 72) down to the yard (66).
		for (int x = R_WALL; x <= R_WALL + 8; x++) {
			for (int z = -5; z <= 5; z++) {
				int yy = Math.max(YARD_Y, G_OUT - (x - R_WALL));
				c.fill(x, YARD_Y - 2, z, x, yy - 1, z, s(Blocks.DEEPSLATE_TILES));
				c.set(x, yy, z, yy < G_OUT && yy > YARD_Y ? Kit.stairs(Blocks.STONE_BRICK_STAIRS, Direction.WEST, false) : paving(x, z, false));
				c.fill(x, yy + 1, z, x, yy + 5, z, s(Blocks.AIR));
			}
		}
		crane(c, 185, -38, 42, 32, 0);
		crane(c, 252, -25, 58, 50, 160);
		crane(c, 200, 30, 36, 30, -30);
		crane(c, 258, 20, 50, 40, 200);
		gear(c, 240, YARD_Y + 5, 32, 11, true);
		gear(c, 178, YARD_Y + 1, 12, 6, false);
		gear(c, 232, YARD_Y + 4, -42, 6, false);
		gear(c, 196, YARD_Y + 2, -14, 3, true);
		gear(c, 168, YARD_Y + 3, -22, 3, false);
		pipe(c, 170, YARD_Y, -18, 198, YARD_Y + 5, -24, 2.5);
		pipe(c, 236, YARD_Y + 1, -46, 262, YARD_Y + 8, -40, 2.5);
		pipe(c, 190, YARD_Y - 1, 20, 186, YARD_Y + 6, 24, 2);
		pipe(c, 262, YARD_Y, 0, 268, YARD_Y + 1, 18, 2);
		for (int[] heap : new int[][]{{176, 30, 4}, {248, -5, 3}, {205, -40, 3}, {265, 32, 3}, {230, 40, 3}, {182, -5, 2}}) {
			heap(c, heap[0], heap[1], heap[2]);
		}
		cable(c, Blocks.RED_CONCRETE, 212, -2, 190, -20, 185, -34);
		cable(c, Blocks.BLUE_CONCRETE, 228, -2, 245, -18, 252, -22);
		cable(c, Blocks.GREEN_CONCRETE, 220, 3, 210, 20, 200, 27);
		cable(c, Blocks.RED_CONCRETE, 228, 2, 250, 15, 257, 19);
		engine(c);
	}

	/** Lattice of polished deepslate corners and iron bar infill (3 x 3), from y1 to y2 at (x, z). */
	private static void lattice(Canvas c, int x, int z, int y1, int y2) {
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				for (int y = y1; y <= y2; y++) {
					boolean corner = dx != 0 && dz != 0;
					boolean centre = dx == 0 && dz == 0;
					c.set(x + dx, y, z + dz, centre ? s(Blocks.AIR) : corner ? s(Blocks.POLISHED_DEEPSLATE) : y % 4 == 0 ? s(Blocks.DEEPSLATE_TILES)
							: Kit.connected(Blocks.IRON_BARS, dz == 0));
				}
			}
		}
	}

	/**
	 * A crane: a heavy deepslate foundation, a lattice tower {@code h} high, a cab with a gear on its side, a long arm
	 * angled up toward {@code deg}, a counter-jib with iron counterweights, chains from the arm's end to a hanging cargo
	 * platform of iron with a timber load.
	 */
	private static void crane(Canvas c, int x, int z, int h, int arm, double deg) {
		if (!c.near(x, z, arm + 8)) {
			return;
		}
		int y = YARD_Y;
		for (int dx = -3; dx <= 3; dx++) {
			for (int dz = -3; dz <= 3; dz++) {
				c.fill(x + dx, y - 1, z + dz, x + dx, y + 2, z + dz, Math.abs(dx) == 3 || Math.abs(dz) == 3 ? s(Blocks.DEEPSLATE_TILES) : s(Blocks.POLISHED_DEEPSLATE));
			}
		}
		lattice(c, x, z, y + 3, y + h);
		int top = y + h;
		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) {
				for (int k = 1; k <= 4; k++) {
					boolean shell = Math.abs(dx) == 2 || Math.abs(dz) == 2 || k == 4;
					c.set(x + dx, top + k, z + dz, !shell ? s(Blocks.AIR) : k == 2 && (dx == 0 || dz == 0) ? s(Blocks.BLACK_CONCRETE) : s(Blocks.GRAY_CONCRETE));
				}
				c.set(x + dx, top, z + dz, s(Blocks.IRON_BLOCK));
			}
		}
		double t = Math.toRadians(deg), ux = Math.cos(t), uz = Math.sin(t);
		// The cab gear (on the side away from the arm).
		gear(c, x - (int) Math.round(uz * 3), top + 2, z + (int) Math.round(ux * 3), 3, Math.abs(ux) < Math.abs(uz));
		// Arm: 2 x 2 beam rising 1 in 3.
		int tipX = 0, tipY = 0, tipZ = 0;
		for (int i = 2; i <= arm; i++) {
			int ax = x + (int) Math.round(ux * i), az = z + (int) Math.round(uz * i);
			int ay = top + 3 + i / 3;
			for (int k = 0; k <= 1; k++) {
				c.set(ax, ay + k, az, i % 4 == 0 ? s(Blocks.POLISHED_DEEPSLATE) : k == 0 ? s(Blocks.DEEPSLATE_TILES) : Kit.connected(Blocks.IRON_BARS, Math.abs(ux) > 0.7));
			}
			tipX = ax;
			tipY = ay;
			tipZ = az;
		}
		// Counter-jib and weights.
		for (int i = 2; i <= arm / 4 + 3; i++) {
			int ax = x - (int) Math.round(ux * i), az = z - (int) Math.round(uz * i);
			c.set(ax, top + 3, az, s(Blocks.DEEPSLATE_TILES));
			if (i >= arm / 4) {
				c.fill(ax, top + 1, az, ax, top + 2, az, s(Blocks.IRON_BLOCK));
			}
		}
		// Cables to the cargo.
		int cargoY = Math.max(y + 8, tipY - 14);
		c.fill(tipX, cargoY + 2, tipZ, tipX, tipY - 1, tipZ, s(Blocks.CHAIN));
		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 1; dz++) {
				c.set(tipX + dx, cargoY, tipZ + dz, s(Blocks.IRON_BLOCK));
				if (Math.abs(dx) <= 1 && dz >= -1 && dz <= 0) {
					c.set(tipX + dx, cargoY + 1, tipZ + dz, dz == 0 ? s(Blocks.OAK_PLANKS) : s(Blocks.STRIPPED_OAK_LOG).setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
				}
			}
		}
		for (int[] cc : new int[][]{{-2, -2}, {2, -2}, {-2, 1}, {2, 1}}) {
			c.set(tipX + cc[0], cargoY + 1, tipZ + cc[1], s(Blocks.CHAIN));
		}
		c.set(tipX, cargoY + 1, tipZ - 2, s(Blocks.CHAIN));
	}

	/**
	 * A toothed gear of radius {@code r} centred at (x, y, z), standing (in the plane along x when {@code alongX}) or lying
	 * flat (when {@code alongX} is false and the gear is small enough to lie): polished deepslate rim, iron teeth, grey
	 * spokes, a hole in the middle. Below the ground it is simply buried.
	 */
	private static void gear(Canvas c, int x, int y, int z, int r, boolean alongX) {
		if (!c.near(x, z, r + 2)) {
			return;
		}
		boolean flat = !alongX && r <= 6 && y <= YARD_Y + 1;
		int teeth = Math.max(8, r * 2);
		for (int a = -r - 1; a <= r + 1; a++) {
			for (int b = -r - 1; b <= r + 1; b++) {
				double d = Math.hypot(a, b);
				double ang = Math.atan2(b, a);
				boolean tooth = d > r + 0.3 && d <= r + 1.3 && Math.floorMod((int) Math.floor(ang / (2 * Math.PI) * teeth * 2), 2) == 0;
				boolean rim = d > r - 1.3 && d <= r + 0.3;
				boolean spoke = d <= r - 1.3 && d > 1.4 && (Math.abs(a) <= 0 || Math.abs(b) <= 0);
				boolean hub = d > 0.9 && d <= 1.6;
				BlockState bs = tooth ? s(Blocks.IRON_BLOCK) : rim ? s(Blocks.POLISHED_DEEPSLATE) : spoke ? s(Blocks.GRAY_CONCRETE) : hub ? s(Blocks.IRON_BLOCK) : null;
				if (bs == null) {
					continue;
				}
				int px, py, pz;
				if (flat) {
					px = x + a;
					py = y;
					pz = z + b;
				} else if (alongX) {
					px = x + a;
					py = y + b;
					pz = z;
				} else {
					px = x;
					py = y + b;
					pz = z + a;
				}
				if (py >= YARD_Y - 1) {
					c.set(px, py, pz, bs);
				}
			}
		}
	}

	/** A hollow pipe from a to b (ring section, iron bands every four blocks, flared rings at both ends). */
	private static void pipe(Canvas c, double ax, double ay, double az, double bx, double by, double bz, double r) {
		double len = Math.sqrt((bx - ax) * (bx - ax) + (by - ay) * (by - ay) + (bz - az) * (bz - az));
		if (!c.near((int) ((ax + bx) / 2), (int) ((az + bz) / 2), (int) (len / 2 + r + 3))) {
			return;
		}
		double ux = (bx - ax) / len, uy = (by - ay) / len, uz = (bz - az) / len;
		int steps = (int) Math.ceil(len * 2);
		java.util.Set<Long> inside = new java.util.HashSet<>();
		java.util.List<int[]> shell = new java.util.ArrayList<>();
		for (int i = 0; i <= steps; i++) {
			double t = i / (double) steps * len;
			double px = ax + ux * t, py = ay + uy * t, pz = az + uz * t;
			boolean end = t < 1 || t > len - 1;
			double rr = end ? r + 0.8 : r;
			for (int x = (int) Math.floor(px - rr - 1); x <= Math.ceil(px + rr + 1); x++) {
				for (int y = (int) Math.floor(py - rr - 1); y <= Math.ceil(py + rr + 1); y++) {
					for (int z = (int) Math.floor(pz - rr - 1); z <= Math.ceil(pz + rr + 1); z++) {
						double wx = x - px, wy = y - py, wz = z - pz;
						double along = wx * ux + wy * uy + wz * uz;
						if (Math.abs(along) > 0.6) {
							continue;
						}
						double perp = Math.sqrt(Math.max(0, wx * wx + wy * wy + wz * wz - along * along));
						long key = ((long) x * 73856093L) ^ ((long) y * 19349663L) ^ ((long) z * 83492791L);
						if (perp <= rr - 1.1) {
							inside.add(key);
						} else if (perp <= rr) {
							int band = (int) Math.floor(t) % 4 == 0 ? 1 : 0;
							shell.add(new int[]{x, y, z, end ? 2 : band});
						}
					}
				}
			}
		}
		for (int[] p : shell) {
			long key = ((long) p[0] * 73856093L) ^ ((long) p[1] * 19349663L) ^ ((long) p[2] * 83492791L);
			if (inside.contains(key) || p[1] < YARD_Y - 2) {
				continue;
			}
			c.set(p[0], p[1], p[2], p[3] == 2 ? s(Blocks.POLISHED_ANDESITE) : p[3] == 1 ? s(Blocks.IRON_BLOCK) : s(Blocks.GRAY_CONCRETE));
		}
	}

	/** A scrap heap: a low mound of iron, concrete, copper and basalt. */
	private static void heap(Canvas c, int x, int z, int r) {
		if (!c.near(x, z, r + 2)) {
			return;
		}
		for (int dx = -r - 1; dx <= r + 1; dx++) {
			for (int dz = -r - 1; dz <= r + 1; dz++) {
				double d = Math.hypot(dx, dz) + unit(x + dx, z + dz, 51) * 1.2;
				int h = (int) Math.round(r - d + 1);
				for (int k = 1; k <= h; k++) {
					double u = unit3(x + dx, k, z + dz, 52);
					c.set(x + dx, YARD_Y + k, z + dz, u < 0.25 ? s(Blocks.IRON_BLOCK) : u < 0.45 ? s(Blocks.GRAY_CONCRETE) : u < 0.6 ? s(Blocks.EXPOSED_COPPER)
							: u < 0.75 ? s(Blocks.POLISHED_BASALT) : u < 0.88 ? s(Blocks.BLACK_CONCRETE) : s(Blocks.CUT_COPPER));
				}
			}
		}
	}

	/** A coloured cable along the ground (a one-wide line set into the surface). */
	private static void cable(Canvas c, Block colour, int... pts) {
		for (int i = 0; i + 3 < pts.length; i += 2) {
			int x1 = pts[i], z1 = pts[i + 1], x2 = pts[i + 2], z2 = pts[i + 3];
			int n = Math.max(Math.abs(x2 - x1), Math.abs(z2 - z1));
			for (int k = 0; k <= n; k++) {
				int x = (int) Math.round(x1 + (x2 - x1) * (k / (double) n)), z = (int) Math.round(z1 + (z2 - z1) * (k / (double) n));
				if (c.inChunk(x, z) && yardEdge(x, z) <= 0.97) {
					c.set(x, YARD_Y, z, s(colour));
				}
			}
		}
	}

	/**
	 * The foundry engine: a heavy deepslate plinth, a boiler house of bricks below and riveted iron above with deepslate
	 * pilasters, a glowing row of blast furnaces, a long boiler drum on top, three brick smokestacks, a big flywheel gear
	 * on the east side and copper pipes running down into the ground.
	 */
	private static void engine(Canvas c) {
		int x0 = ENGINE_X, z0 = ENGINE_Z, y = YARD_Y;
		if (!c.near(x0, z0, 20)) {
			return;
		}
		for (int dx = -8; dx <= 7; dx++) {
			for (int dz = -8; dz <= 7; dz++) {
				c.fill(x0 + dx, y - 1, z0 + dz, x0 + dx, y + 2, z0 + dz, Math.abs(dx + 0.5) > 7 || Math.abs(dz + 0.5) > 7 ? s(Blocks.DEEPSLATE_TILES)
						: s(Blocks.POLISHED_DEEPSLATE));
			}
		}
		for (int dx = -6; dx <= 5; dx++) {
			for (int dz = -6; dz <= 5; dz++) {
				boolean ex = dx == -6 || dx == 5, ez = dz == -6 || dz == 5;
				for (int k = 3; k <= 20; k++) {
					BlockState b;
					if (!ex && !ez) {
						b = k == 20 ? s(Blocks.IRON_BLOCK) : s(Blocks.AIR);
					} else if (ex && ez || (ex ? dz : dx) % 4 == 0) {
						b = s(Blocks.POLISHED_DEEPSLATE);
					} else if (k <= 10) {
						b = s(Blocks.BRICKS);
					} else if (k == 11 || k == 19) {
						b = s(Blocks.DEEPSLATE_TILES);
					} else {
						b = k == 15 ? s(Blocks.GRAY_CONCRETE) : s(Blocks.IRON_BLOCK);
					}
					c.set(x0 + dx, y + k, z0 + dz, b);
				}
			}
		}
		// Glowing furnaces along the south face, a door.
		for (int dx = -4; dx <= 3; dx += 2) {
			c.set(x0 + dx, y + 4, z0 + 5, s(Blocks.BLAST_FURNACE).setValue(net.minecraft.world.level.block.BlastFurnaceBlock.FACING, Direction.SOUTH)
					.setValue(net.minecraft.world.level.block.BlastFurnaceBlock.LIT, true));
		}
		Kit.door(c, x0 - 6, y + 3, z0, Blocks.IRON_DOOR, Direction.EAST);
		c.set(x0 - 7, y + 4, z0 + 1, s(Blocks.STONE_BUTTON).setValue(net.minecraft.world.level.block.ButtonBlock.FACING, Direction.WEST)
				.setValue(net.minecraft.world.level.block.ButtonBlock.FACE, net.minecraft.world.level.block.state.properties.AttachFace.WALL));
		c.set(x0 - 1, y + 19, z0, CloudtopIsles.lantern(Blocks.LANTERN, true));
		// Boiler drum along x on the roof.
		for (int dx = -5; dx <= 4; dx++) {
			for (int a = -3; a <= 3; a++) {
				for (int b = -3; b <= 3; b++) {
					double d = Math.hypot(a, b);
					if (d <= 3.3 && y + 23 + b > y + 20) {
						c.set(x0 + dx, y + 23 + b, z0 + a, dx % 3 == 0 && d > 2.3 ? s(Blocks.IRON_BLOCK) : d > 2.3 ? s(Blocks.CUT_COPPER) : s(Blocks.GRAY_CONCRETE));
					}
				}
			}
		}
		// Smokestacks.
		for (int[] st : new int[][]{{-4, -4}, {0, -4}, {4, -4}}) {
			for (int k = 21; k <= 28; k++) {
				for (int dx = -1; dx <= 1; dx++) {
					for (int dz = -1; dz <= 1; dz++) {
						boolean centre = dx == 0 && dz == 0;
						c.set(x0 + st[0] + dx, y + k, z0 + st[1] + dz, centre ? s(Blocks.AIR) : k >= 27 ? s(Blocks.BLACK_CONCRETE) : s(Blocks.BRICKS));
					}
				}
			}
		}
		// The flywheel on the east side.
		gear(c, x0 + 7, y + 10, z0, 7, false);
		c.fill(x0 + 6, y + 10, z0, x0 + 6, y + 10, z0, s(Blocks.IRON_BLOCK));
		// Copper pipes down to the ground.
		for (int[] p : new int[][]{{-7, 3}, {-7, -3}, {6, 4}}) {
			c.fill(x0 + p[0], y + 3, z0 + p[1], x0 + p[0], y + 16, z0 + p[1], s(Blocks.CUT_COPPER));
			c.set(x0 + p[0], y + 17, z0 + p[1], s(Blocks.EXPOSED_COPPER));
		}
	}
}
