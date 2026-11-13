package com.grandline.world.plan;

import com.grandline.world.plan.Anchor.Type;
import com.grandline.world.plan.Terrain.Ground;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;

import java.util.ArrayList;
import java.util.List;

/**
 * Dawnreach Harbor (Lv 1, starter island): a low, paved harbour town on an elongated platform. Fountain courtyard in
 * the middle, a domed civic hall to its right, fourteen buildings and pavilions (A-N) around a street loop, three stone
 * piers on each of the west and east waterfronts, landscaped strips along the long north and south edges.
 *
 * <p>Laid out on the town plan's own grid: fountain centre at plan (0, 0), +x east, +z south. The whole plan is shifted
 * by {@link #OX} so the town as a whole is centred on the island anchor. Every coordinate below is a plan coordinate;
 * {@link P} converts to island-local when writing blocks.
 */
public final class DawnreachHarbor extends BasePlan {
	/** Island-local x = plan x + OX (centres the town, x -63..98, on the anchor). */
	public static final int OX = -17;
	/** Street surface block; people walk at STREET + 1. */
	public static final int STREET = 65;
	public static final int WALK = STREET + 1;
	/** Pier deck block (walk at 64): two steps below the street. */
	static final int DECK = 63;
	/** Platform (plan coordinates). */
	static final int PX1 = -51, PX2 = 84, PZ1 = -44, PZ2 = 44, CHAMFER = 8;

	/** Set GRANDLINE_LAYOUT_ONLY=1 to generate footprints, streets and piers only (layout check before roofs). */
	static final boolean LAYOUT_ONLY = "1".equals(System.getenv("GRANDLINE_LAYOUT_ONLY"));

	// ==================================================================================================== geometry

	/** A polygon in plan coordinates (edge coordinates: cell (x, z) covers [x, x+1] x [z, z+1]). */
	static final class Poly {
		final double[] xs, zs;
		final int minX, maxX, minZ, maxZ;

		Poly(double... pts) {
			xs = new double[pts.length / 2];
			zs = new double[pts.length / 2];
			double a = Double.MAX_VALUE, b = -Double.MAX_VALUE, c = Double.MAX_VALUE, d = -Double.MAX_VALUE;
			for (int i = 0; i < xs.length; i++) {
				xs[i] = pts[2 * i];
				zs[i] = pts[2 * i + 1];
				a = Math.min(a, xs[i]);
				b = Math.max(b, xs[i]);
				c = Math.min(c, zs[i]);
				d = Math.max(d, zs[i]);
			}
			minX = (int) Math.floor(a);
			maxX = (int) Math.ceil(b);
			minZ = (int) Math.floor(c);
			maxZ = (int) Math.ceil(d);
		}

		/** Block rectangle x1..x2, z1..z2 inclusive. */
		static Poly rect(int x1, int x2, int z1, int z2) {
			return new Poly(x1, z1, x2 + 1, z1, x2 + 1, z2 + 1, x1, z2 + 1);
		}

		boolean contains(double px, double pz) {
			boolean in = false;
			for (int i = 0, j = xs.length - 1; i < xs.length; j = i++) {
				if ((zs[i] > pz) != (zs[j] > pz) && px < (xs[j] - xs[i]) * (pz - zs[i]) / (zs[j] - zs[i]) + xs[i]) {
					in = !in;
				}
			}
			return in;
		}

		double edgeDistance(double px, double pz) {
			double best = Double.MAX_VALUE;
			for (int i = 0, j = xs.length - 1; i < xs.length; j = i++) {
				double vx = xs[i] - xs[j], vz = zs[i] - zs[j], len = vx * vx + vz * vz;
				double t = len == 0 ? 0 : Math.max(0, Math.min(1, ((px - xs[j]) * vx + (pz - zs[j]) * vz) / len));
				best = Math.min(best, Math.hypot(px - (xs[j] + t * vx), pz - (zs[j] + t * vz)));
			}
			return best;
		}

		/** Distance from the cell's centre to the outline, or -1 outside. */
		double d(int x, int z) {
			return contains(x + 0.5, z + 0.5) ? edgeDistance(x + 0.5, z + 0.5) : -1;
		}

		boolean in(int x, int z) {
			return contains(x + 0.5, z + 0.5);
		}

		double cx() {
			double s = 0;
			for (double x : xs) {
				s += x;
			}
			return s / xs.length;
		}

		double cz() {
			double s = 0;
			for (double z : zs) {
				s += z;
			}
			return s / zs.length;
		}
	}

	/** Plan-coordinate writer over the chunk canvas. */
	static final class P {
		final Canvas c;

		P(Canvas c) {
			this.c = c;
		}

		void set(int x, int y, int z, BlockState s) {
			c.set(x + OX, y, z, s);
		}

		void fill(int x1, int y1, int z1, int x2, int y2, int z2, BlockState s) {
			c.fill(x1 + OX, y1, z1, x2 + OX, y2, z2, s);
		}

		boolean in(int x, int z) {
			return c.inChunk(x + OX, z);
		}

		boolean near(int x, int z, int r) {
			return c.near(x + OX, z, r);
		}

		boolean near(Poly p, int margin) {
			int cx = (p.minX + p.maxX) / 2, cz = (p.minZ + p.maxZ) / 2;
			return c.near(cx + OX, cz, Math.max(p.maxX - p.minX, p.maxZ - p.minZ) / 2 + margin);
		}
	}

	/** Roof material: stairs for slopes, a full block for ridges and steps, and a slab when the family has one. */
	record Roof(Block stairs, Block ridge, Block slab) {
	}

	static final Roof ROSE = new Roof(Blocks.CRIMSON_STAIRS, Blocks.RED_TERRACOTTA, null);
	static final Roof TAN = new Roof(Blocks.SMOOTH_SANDSTONE_STAIRS, Blocks.CUT_SANDSTONE, null);
	static final Roof BLUE = new Roof(Blocks.DARK_PRISMARINE_STAIRS, Blocks.BLUE_CONCRETE, null);
	static final Roof TEAL = new Roof(Blocks.DARK_PRISMARINE_STAIRS, Blocks.DARK_PRISMARINE, null);
	static final Roof WARM = new Roof(Blocks.BRICK_STAIRS, Blocks.BRICKS, Blocks.BRICK_SLAB);
	static final Roof COPPER = new Roof(Blocks.OXIDIZED_CUT_COPPER_STAIRS, Blocks.OXIDIZED_CUT_COPPER, Blocks.OXIDIZED_CUT_COPPER_SLAB);

	/** One building of the plan, by its outer roof outline. */
	record Building(String id, Poly outline, Block wall, int stories, Roof roof, int pitch, double doorX, double doorZ, Block glass,
			int windowEvery, Block shutter) {
	}

	// ==================================================================================================== the plan

	static final Poly A = new Poly(-31, -24, -25, -29, -16, -18, -22, -13);
	static final Poly F = Poly.rect(17, 44, -15, 13);
	static final double DOME_X = 31.5, DOME_Z = -2.5, DRUM_R = 7.5;
	/** L: main block with two projecting bays toward the plaza-side lane. */
	static final Poly L = new Poly(-4, 20, -2, 20, -2, 17, 5, 17, 5, 20, 10, 20, 10, 17, 17, 17, 17, 20, 19, 20, 19, 36, -4, 36);
	/** N: a pavilion turned 45 degrees to follow the diagonal approach. */
	static final Poly N_ROOF = new Poly(-32, 15.5, -26.5, 10, -21, 15.5, -26.5, 21);
	static final Poly D_ROOF = Poly.rect(23, 32, -32, -22);

	static final List<Building> BUILDINGS = List.of(
			new Building("A", A, Blocks.END_STONE_BRICKS, 2, ROSE, 1, -19, -15.5, Blocks.GLASS_PANE, 3, null),
			new Building("B", Poly.rect(-15, 0, -33, -18), Blocks.SANDSTONE, 2, TAN, 2, -7, -18, Blocks.GLASS_PANE, 2, null),
			new Building("C", Poly.rect(2, 16, -32, -18), Blocks.WHITE_TERRACOTTA, 2, BLUE, 1, 9, -18, Blocks.GLASS_PANE, 3, null),
			new Building("E", Poly.rect(34, 49, -37, -22), Blocks.END_STONE_BRICKS, 3, TEAL, 2, 41, -22, Blocks.GLASS_PANE, 3, null),
			new Building("G", Poly.rect(50, 65, -22, -13), Blocks.WHITE_TERRACOTTA, 2, ROSE, 1, 59, -13, Blocks.GLASS_PANE, 3, null),
			new Building("H", Poly.rect(51, 66, -10, 8), Blocks.LIGHT_BLUE_TERRACOTTA, 2, WARM, 2, 51, -1, Blocks.GLASS_PANE, 3,
					Blocks.CRIMSON_TRAPDOOR),
			new Building("I", Poly.rect(51, 66, 10, 25), Blocks.WHITE_TERRACOTTA, 2, BLUE, 1, 51, 17, Blocks.GLASS_PANE, 3, null),
			new Building("J", Poly.rect(35, 51, 22, 38), Blocks.SANDSTONE, 2, TAN, 2, 43, 22, Blocks.GLASS_PANE, 3, null),
			new Building("K", Poly.rect(24, 33, 22, 38), Blocks.WHITE_TERRACOTTA, 2, ROSE, 1, 28, 22, Blocks.GLASS_PANE, 3, null),
			new Building("L", L, Blocks.SMOOTH_QUARTZ, 2, BLUE, 2, 7.5, 20, Blocks.LIGHT_BLUE_STAINED_GLASS_PANE, 3, null),
			new Building("M", Poly.rect(-20, -6, 18, 33), Blocks.WHITE_TERRACOTTA, 2, TEAL, 1, -13, 18, Blocks.GLASS_PANE, 3, null));

	/** Piers: x1..x2 (outer projection), z1..z2. */
	static final int[][] PIERS = {{-61, -52, -17, -13}, {-62, -53, -2, 2}, {-63, -54, 13, 18},
			{85, 96, -17, -12}, {87, 97, -2, 3}, {88, 98, 13, 18}};
	static final String[] PIER_NAMES = {"Left upper", "Left middle", "Left lower", "Right upper", "Right middle", "Right lower"};

	/** Courtyard x -23..15, z -12..12 with its left corners clipped. */
	static final Poly COURTYARD = new Poly(-18, -12, 16, -12, 16, 13, -18, 13, -23, 8, -23, -7);

	/** Planted beds: continuous strips on the long edges, wedges inside the west diagonals, hedged beds between the east pier approaches. */
	record Bed(Poly shape, boolean hedge, int trees) {
	}

	static final List<Bed> BEDS = List.of(
			new Bed(new Poly(-42, -43, 77, -43, 77, -38, -42, -38), false, 12),   // upper strip
			new Bed(new Poly(-42, 39, 77, 39, 77, 44, -42, 44), false, 12),       // lower strip
			new Bed(new Poly(-49, -4, -30, -4, -49, -11), true, 1),               // wedge inside the upper diagonal
			new Bed(new Poly(-49, 5, -30, 5, -49, 12), true, 1),                  // wedge inside the lower diagonal
			new Bed(new Poly(-49, -36, -35, -36, -49, -21), false, 2),            // outer wedge, north-west
			new Bed(new Poly(-49, 37, -35, 37, -49, 22), false, 2),               // outer wedge, south-west
			new Bed(Poly.rect(70, 81, -9, -5), true, 1),                          // between right upper and middle approaches
			new Bed(Poly.rect(70, 81, 7, 11), true, 1),                           // between right middle and lower approaches
			new Bed(Poly.rect(72, 80, -34, -23), false, 2),                       // north-east corner
			new Bed(Poly.rect(72, 80, 25, 35), false, 2));                        // south-east corner

	/** Fruit Gacha cart: centre (48, -9), on the H side of the lane behind the civic hall; keeper faces the lane (west). */
	static final int CART_X = 48, CART_Z = -9;

	public DawnreachHarbor() {
		super("Dawnreach Harbor", 0.0);
		terrain.platform(PX1 + OX, PZ1, PX2 + OX, PZ2, CHAMFER, STREET, Ground.STONE).reach(100);
		anchor(Anchor.at(Type.ARRIVAL, -12 + OX, WALK, 0, -90));
		anchor(Anchor.at(Type.QUEST, -9 + OX, WALK, -7, -45));
		anchor(Anchor.at(Type.FRUIT_DEALER, -7 + OX, WALK, -26, 0));
		anchor(Anchor.at(Type.GACHA, CART_X + OX, WALK, CART_Z, 90));
		anchor(Anchor.at(Type.NAVIGATOR, -46 + OX, WALK, -4, 90));
		anchor(Anchor.at(Type.FISHERMAN, 92 + OX, DECK + 1, 0, -90));
		anchor(Anchor.at(Type.DOCK, -58 + OX, DECK, -5));
		anchor(Anchor.at(Type.DOCK, 92 + OX, DECK, -5));
		anchor(Anchor.of(Type.TREASURE, 60 + OX, -41));
	}

	static boolean onPlatform(int x, int z) {
		Terrain.Platform p = new Terrain.Platform(PX1, PZ1, PX2, PZ2, CHAMFER, STREET, Ground.STONE);
		return p.contains(x, z);
	}

	static long hash(int x, int z) {
		long h = x * 0x9E3779B97F4A7C15L ^ z * 0xC2B2AE3D27D4EB4FL;
		h ^= h >>> 29;
		h *= 0xBF58476D1CE4E5B9L;
		return h ^ (h >>> 32);
	}

	// ==================================================================================================== build

	@Override
	public void build(Canvas canvas) {
		P p = new P(canvas);
		ground(p);
		piers(p);
		if (p.near(0, 0, 16)) {
			if (!placeFountainTemplate(canvas)) {
				fountain(p);
			}
		}
		for (Building b : BUILDINGS) {
			if (p.near(b.outline(), 6)) {
				building(p, b);
			}
		}
		if (p.near(F, 12)) {
			civicHall(p);
		}
		if (p.near(D_ROOF, 4)) {
			pavilion(p, D_ROOF, Poly.rect(22, 33, -33, -21), TEAL, new int[][]{{24, -31}, {31, -31}, {24, -23}, {31, -23}, {24, -27}, {31, -27}});
		}
		if (p.near(N_ROOF, 4)) {
			pavilion(p, N_ROOF, new Poly(-31, 13, -29, 11, -24, 11, -22, 13, -22, 18, -24, 20, -29, 20, -31, 18), ROSE,
					new int[][]{{-29, 15}, {-27, 12}, {-24, 15}, {-27, 18}});
		}
		if (!LAYOUT_ONLY) {
			if (p.near(CART_X, CART_Z, 6)) {
				gachaCart(p);
			}
			lamps(p);
		}
		treasure(canvas);
	}

	// ---------------------------------------------------------------------------------------------------- ground

	/** Which building (or pavilion) outline covers a cell, for paving the strip under the eaves. */
	static boolean underEaves(int x, int z) {
		for (Building b : BUILDINGS) {
			if (b.outline().in(x, z)) {
				return true;
			}
		}
		return F.in(x, z) || D_ROOF.in(x, z) || N_ROOF.in(x, z);
	}

	/** Paving, beds, the courtyard pattern, retaining walls and the rocky base around the platform. */
	private void ground(P p) {
		BlockState stone = s(Blocks.SMOOTH_STONE), edge = s(Blocks.POLISHED_ANDESITE), quartz = s(Blocks.SMOOTH_QUARTZ);
		for (int x = PX1 - 4; x <= PX2 + 4; x++) {
			for (int z = PZ1 - 4; z <= PZ2 + 4; z++) {
				if (!p.in(x, z)) {
					continue;
				}
				if (!onPlatform(x, z)) {
					rubble(p, x, z);
					continue;
				}
				boolean rim = !onPlatform(x + 1, z) || !onPlatform(x - 1, z) || !onPlatform(x, z + 1) || !onPlatform(x, z - 1);
				if (rim) {
					// Pale retaining wall with a curb on top (left open where pier stairs come up).
					p.fill(x, 56, z, x, STREET, z, quartz);
					if (!pierStairs(x, z)) {
						p.set(x, WALK, z, Kit.slab(Blocks.SMOOTH_STONE_SLAB, false));
					}
					continue;
				}
				Bed bed = bedAt(x, z);
				if (bed != null) {
					bedCell(p, bed, x, z);
				} else if (COURTYARD.in(x, z)) {
					double r = Math.hypot(x, z);
					double d = COURTYARD.edgeDistance(x + 0.5, z + 0.5);
					boolean ring = d < 1 || (r > 8.4 && r < 9.4) || (r > 13.4 && r < 14.4);
					p.set(x, STREET, z, ring ? edge : stone);
				} else {
					p.set(x, STREET, z, underEaves(x, z) ? edge : stone);
				}
			}
		}
	}

	/** Rocky base around the platform: exposed stone along the long edges, under water at the quays. */
	private void rubble(P p, int x, int z) {
		int dist = 9;
		for (int r = 1; r <= 3 && dist == 9; r++) {
			for (int dx = -r; dx <= r && dist == 9; dx++) {
				for (int dz = -r; dz <= r; dz++) {
					if (Math.abs(dx) + Math.abs(dz) <= r && onPlatform(x + dx, z + dz)) {
						dist = r;
						break;
					}
				}
			}
		}
		if (dist > 3) {
			return;
		}
		boolean quay = x <= PX1 || x >= PX2;
		int top = quay ? 60 - dist : (dist == 1 ? 63 : dist == 2 ? 62 : 60);
		long h = hash(x, z);
		BlockState rock = s(switch ((int) Math.floorMod(h, 5)) {
			case 0, 1 -> Blocks.STONE;
			case 2, 3 -> Blocks.ANDESITE;
			default -> Blocks.GRAVEL;
		});
		p.fill(x, Terrain.FLOOR, z, x, top - 1, z, s(Blocks.STONE));
		p.set(x, top, z, rock);
	}

	static boolean pierStairs(int x, int z) {
		for (int[] pier : PIERS) {
			boolean west = pier[0] < 0;
			if (z >= pier[2] && z <= pier[3] && (west ? x == PX1 || x == PX1 + 1 : x == PX2 || x == PX2 - 1)) {
				return true;
			}
		}
		return false;
	}

	static Bed bedAt(int x, int z) {
		for (Bed b : BEDS) {
			if (b.shape().in(x, z)) {
				return b;
			}
		}
		return null;
	}

	private void bedCell(P p, Bed bed, int x, int z) {
		double d = bed.shape().edgeDistance(x + 0.5, z + 0.5);
		p.set(x, STREET, z, s(Blocks.GRASS_BLOCK));
		if (d < 1) {
			p.set(x, WALK, z, Kit.slab(Blocks.SMOOTH_QUARTZ_SLAB, false)); // low quartz border
		} else if (bed.hedge() && d < 2) {
			p.set(x, WALK, z, Kit.leaves(Blocks.OAK_LEAVES));
		}
	}

	/** Short blocky trees in the beds, evenly spread. */
	private void bedTrees(P p) {
		for (Bed bed : BEDS) {
			Poly sh = bed.shape();
			for (int i = 0; i < bed.trees(); i++) {
				// Spread along the bed's long axis, at its middle line.
				boolean wide = sh.maxX - sh.minX >= sh.maxZ - sh.minZ;
				double t = (i + 0.5) / bed.trees();
				int x = wide ? (int) Math.round(sh.minX + 3 + t * (sh.maxX - sh.minX - 6)) : (int) Math.round(sh.cx());
				int z = wide ? (int) Math.round((sh.minZ + sh.maxZ) / 2.0) : (int) Math.round(sh.minZ + 3 + t * (sh.maxZ - sh.minZ - 6));
				if (bed.trees() == 1 || sh.xs.length == 3) {
					x = (int) Math.round(sh.cx());
					z = (int) Math.round(sh.cz());
				}
				if (!sh.in(x, z) || sh.edgeDistance(x + 0.5, z + 0.5) < 2 || !p.near(x, z, 2)) {
					continue;
				}
				blockyTree(p, x, z, 2 + (int) Math.floorMod(hash(x, z), 2));
			}
		}
	}

	private void blockyTree(P p, int x, int z, int trunk) {
		p.fill(x, WALK, z, x, WALK + trunk - 1, z, s(Blocks.OAK_LOG));
		BlockState leaf = Kit.leaves(Blocks.OAK_LEAVES);
		p.fill(x - 1, WALK + trunk, z - 1, x + 1, WALK + trunk + 1, z + 1, leaf);
		p.set(x, WALK + trunk + 2, z, leaf);
	}

	// ---------------------------------------------------------------------------------------------------- piers

	/** Stone-brick piers on log-free stone piles, joined to the quay, with stairs up to the street and mooring posts. */
	private void piers(P p) {
		for (int[] pier : PIERS) {
			boolean west = pier[0] < 0;
			int inner = west ? PX1 - 1 : PX2 + 1;               // first cell outside the quay wall
			int outer = west ? pier[0] : pier[1];
			int from = Math.min(inner, outer), to = Math.max(inner, outer);
			if (!p.near((from + to) / 2, (pier[2] + pier[3]) / 2, (to - from) / 2 + 6)) {
				continue;
			}
			for (int x = from; x <= to; x++) {
				for (int z = pier[2]; z <= pier[3]; z++) {
					boolean side = z == pier[2] || z == pier[3];
					boolean end = x == outer;
					p.set(x, DECK, z, s(Blocks.STONE_BRICKS));
					p.fill(x, DECK + 1, z, x, DECK + 3, z, s(Blocks.AIR));
					if ((side || end) && Math.floorMod(x, 4) == 0 || (side && end)) {
						p.fill(x, Terrain.FLOOR, z, x, DECK - 1, z, s(Blocks.STONE_BRICKS)); // pile
					}
					if (side || end) {
						boolean post = (side && end) || Math.floorMod(x, 4) == 0;
						p.set(x, DECK + 1, z, post ? s(Blocks.SPRUCE_FENCE) : Kit.slab(Blocks.STONE_BRICK_SLAB, false));
					}
				}
			}
			// Two steps up through the quay wall to the street.
			Direction up = west ? Direction.EAST : Direction.WEST;
			int wallX = west ? PX1 : PX2, stepX = west ? PX1 + 1 : PX2 - 1;
			for (int z = pier[2]; z <= pier[3]; z++) {
				p.set(wallX, DECK, z, Kit.stairs(Blocks.STONE_BRICK_STAIRS, up, false));
				p.fill(wallX, DECK + 1, z, wallX, WALK + 2, z, s(Blocks.AIR));
				p.set(stepX, DECK + 1, z, Kit.stairs(Blocks.STONE_BRICK_STAIRS, up, false));
				p.fill(stepX, DECK + 2, z, stepX, WALK + 2, z, s(Blocks.AIR));
			}
		}
	}

	// ---------------------------------------------------------------------------------------------------- fountain

	/** A hand-built fountain saved with a Structure Block; when the mod ships it, it replaces the generated fountain. */
	public static final net.minecraft.resources.ResourceLocation FOUNTAIN_TEMPLATE =
			new net.minecraft.resources.ResourceLocation("grandline", "dawnreach/fountain");

	/**
	 * Places the saved fountain exactly as built: every block state, flowing water levels included, is copied from the
	 * file. Its footprint is centred on the fountain point (0, 0) and its bottom layer goes at the plaza paving level
	 * (STREET). Returns false when there is no such file (the generated fountain is used instead).
	 */
	/** Final-step pass (see FountainFeature): places the saved fountain's part in this chunk, if Dawnreach uses it. */
	public static boolean placeFountainInChunk(net.minecraft.world.level.WorldGenLevel level, int chunkMinX, int chunkMinZ) {
		com.grandline.world.SeaLayout.Spec spec = com.grandline.world.SeaLayout.starter(1);
		if (!(com.grandline.world.plan.IslandPlans.get(spec.name()) instanceof DawnreachHarbor plan)) {
			return false; // older worlds keep their old Dawnreach
		}
		int fx = spec.x() + OX, fz = spec.z();
		if (chunkMinX > fx + 16 || chunkMinX + 15 < fx - 16 || chunkMinZ > fz + 16 || chunkMinZ + 15 < fz - 16) {
			return false;
		}
		return placeFountainTemplate(new Canvas(level, plan.terrain(), spec.x(), spec.z(), chunkMinX, chunkMinZ));
	}

	private static boolean placeFountainTemplate(Canvas canvas) {
		var server = canvas.level().getServer();
		if (server == null) {
			return false;
		}
		var template = server.getStructureManager().get(FOUNTAIN_TEMPLATE).orElse(null);
		if (template == null) {
			return false;
		}
		var size = template.getSize();
		net.minecraft.core.BlockPos corner = canvas.world(-(size.getX() - 1) / 2 + OX, STREET, -(size.getZ() - 1) / 2);
		var settings = new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings()
				.setBoundingBox(canvas.chunkBox())
				.setKeepLiquids(false)
				.addProcessor(com.grandline.world.structure.MarkerProcessor.INSTANCE);
		template.placeInWorld(canvas.level(), corner, corner, settings, net.minecraft.util.RandomSource.create(42), 2);
		return true;
	}

	/** The basin: an 11 x 11 cross (arms 5 wide) with 3 x 3 notches at the corners. */
	static boolean basinCell(int x, int z) {
		return Math.abs(x) <= 5 && Math.abs(z) <= 5 && !(Math.abs(x) >= 3 && Math.abs(z) >= 3);
	}

	/** The fountain's whole footprint: the basin plus the low step around it (13 x 13). */
	static boolean fountainCell(int x, int z) {
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				if (basinCell(x + dx, z + dz)) {
					return true;
				}
			}
		}
		return false;
	}

	/** The upper basin: a 5 x 5 cross (corners cut) carried on the centre column. */
	static boolean trayCell(int x, int z) {
		return Math.abs(x) <= 2 && Math.abs(z) <= 2 && !(Math.abs(x) == 2 && Math.abs(z) == 2);
	}

	/**
	 * The plaza fountain: a cross-shaped quartz basin with a raised rim and a low step around it, light-blue tiles with
	 * four glowing panels under the water, and a quartz column carrying a smaller cross-shaped basin. Water fills the
	 * upper basin and pours from its four cut corners into the pool; a carved block and cap crown the column.
	 */
	private void fountain(P p) {
		BlockState quartz = s(Blocks.SMOOTH_QUARTZ);
		BlockState water = s(Blocks.WATER);
		BlockState air = s(Blocks.AIR);
		for (int x = -6; x <= 6; x++) {
			for (int z = -6; z <= 6; z++) {
				if (!fountainCell(x, z)) {
					continue;
				}
				p.fill(x, WALK, z, x, WALK + 6, z, air);
				p.set(x, STREET - 1, z, quartz);
				if (!basinCell(x, z)) {
					p.set(x, STREET, z, quartz);
					p.set(x, WALK, z, Kit.slab(Blocks.SMOOTH_QUARTZ_SLAB, false)); // low step around the basin
					continue;
				}
				boolean rim = !basinCell(x + 1, z) || !basinCell(x - 1, z) || !basinCell(x, z + 1) || !basinCell(x, z - 1);
				if (rim) {
					p.set(x, STREET, z, quartz);
					p.set(x, WALK, z, quartz);
				} else {
					boolean glow = (Math.abs(x) == 3 && z == 0) || (x == 0 && Math.abs(z) == 3);
					p.set(x, STREET, z, glow ? s(Blocks.SEA_LANTERN) : s(Blocks.LIGHT_BLUE_TERRACOTTA));
					p.set(x, WALK, z, water);
				}
			}
		}
		// Fluted centre column, three blocks of it showing above the pool.
		BlockState pillar = s(Blocks.QUARTZ_PILLAR);
		p.set(0, STREET, 0, quartz);
		p.fill(0, WALK, 0, 0, WALK + 2, 0, pillar);
		// Upper basin: a cross with a lip underneath its arms, a rim, water inside, and the crown in the middle.
		int tray = WALK + 3;
		for (int x = -2; x <= 2; x++) {
			for (int z = -2; z <= 2; z++) {
				if (!trayCell(x, z)) {
					continue;
				}
				boolean edge = Math.abs(x) == 2 || Math.abs(z) == 2;
				p.set(x, tray, z, quartz);
				if (edge) {
					p.set(x, tray - 1, z, Kit.slab(Blocks.SMOOTH_QUARTZ_SLAB, true)); // lip
					p.set(x, tray + 1, z, quartz);                                   // rim
				} else if (x != 0 || z != 0) {
					p.set(x, tray + 1, z, water);
				}
			}
		}
		p.fill(0, tray + 1, 0, 0, tray + 2, 0, s(Blocks.CHISELED_QUARTZ_BLOCK));
		p.set(0, tray + 3, 0, Kit.slab(Blocks.SMOOTH_QUARTZ_SLAB, false));
		// Waterfalls from the four cut corners down into the pool: a source at the top, falling water below it
		// (placed directly, since water set during world generation does not start flowing on its own).
		BlockState falling = water.setValue(net.minecraft.world.level.block.LiquidBlock.LEVEL, 8);
		for (int[] c : new int[][]{{2, 2}, {-2, 2}, {2, -2}, {-2, -2}}) {
			p.set(c[0], tray + 1, c[1], water);
			p.fill(c[0], WALK + 1, c[1], c[0], tray, c[1], falling);
			// A source in open air also pours sideways; invisible barriers on its two open sides keep each spout a
			// single clean stream (they sit level with the upper basin, out of reach from the plaza).
			int ox = Integer.signum(c[0]), oz = Integer.signum(c[1]);
			p.set(c[0] + ox, tray + 1, c[1], s(Blocks.BARRIER));
			p.set(c[0], tray + 1, c[1] + oz, s(Blocks.BARRIER));
			p.c.tickFluid(c[0] + OX, tray + 1, c[1]);
		}
	}

	// ---------------------------------------------------------------------------------------------------- buildings

	private static final Direction[] DIRS = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};

	/** Outward wall face of a wall cell (a side whose neighbour is under the eaves or outside), or null at corners. */
	static Direction outward(Poly poly, int x, int z) {
		Direction found = null;
		int count = 0;
		for (Direction d : DIRS) {
			if (poly.d(x + d.getStepX(), z + d.getStepZ()) < 1) {
				found = d;
				count++;
			}
		}
		return count == 1 ? found : null;
	}

	static boolean isWall(double d) {
		return d >= 1 && d < 2.2;
	}

	static int wallTop(int stories) {
		return STREET + 4 * stories;
	}

	/** A building from its outer roof outline: walls one block in, stories with floors, windows, door, ladder, roof. */
	private void building(P p, Building b) {
		Poly poly = b.outline();
		int top = LAYOUT_ONLY ? STREET + 3 : wallTop(b.stories());
		BlockState wall = s(b.wall());
		BlockState floor = s(Blocks.SPRUCE_PLANKS);
		// Door: the wall cell nearest the door point.
		int doorX = 0, doorZ = 0;
		double best = Double.MAX_VALUE;
		for (int x = poly.minX; x <= poly.maxX; x++) {
			for (int z = poly.minZ; z <= poly.maxZ; z++) {
				if (isWall(poly.d(x, z)) && outward(poly, x, z) != null) {
					double dd = Math.hypot(x + 0.5 - b.doorX(), z + 0.5 - b.doorZ());
					if (dd < best) {
						best = dd;
						doorX = x;
						doorZ = z;
					}
				}
			}
		}
		int[] ladder = null;
		int[] lamp = null;
		for (int x = poly.minX; x <= poly.maxX; x++) {
			for (int z = poly.minZ; z <= poly.maxZ; z++) {
				double d = poly.d(x, z);
				if (d < 1) {
					continue;
				}
				if (isWall(d)) {
					p.fill(x, STREET - 2, z, x, top, z, wall);
					Direction out = outward(poly, x, z);
					if (out != null && !LAYOUT_ONLY) {
						windows(p, b, x, z, out, top);
					}
				} else {
					p.set(x, STREET, z, floor);
					for (int s = 1; s <= b.stories() && !LAYOUT_ONLY; s++) {
						p.fill(x, STREET + 4 * (s - 1) + 1, z, x, STREET + 4 * s - 1, z, s(Blocks.AIR));
						p.set(x, STREET + 4 * s, z, floor);
					}
					if (ladder == null && isWall(poly.d(x - 1, z)) && Math.hypot(x - doorX, z - doorZ) > 3) {
						ladder = new int[]{x, z};
					}
					if (poly.d(x + 1, z) >= 1 && isWall(poly.d(x + 1, z)) && isWall(poly.d(x, z + 1))) {
						lamp = new int[]{x, z};
					}
				}
			}
		}
		if (LAYOUT_ONLY) {
			return;
		}
		Direction doorFace = outward(poly, doorX, doorZ);
		if (doorFace != null) {
			Kit.door(p.c, doorX + OX, WALK, doorZ, Blocks.SPRUCE_DOOR, doorFace);
			p.set(doorX, WALK + 2, doorZ, Kit.slab(Blocks.SMOOTH_QUARTZ_SLAB, true)); // trim over the door
		}
		if (ladder != null && b.stories() > 1) {
			BlockState rung = s(Blocks.LADDER).setValue(LadderBlock.FACING, Direction.EAST);
			p.fill(ladder[0], WALK, ladder[1], ladder[0], STREET + 4 * (b.stories() - 1), ladder[1], rung);
		}
		if (lamp != null) {
			for (int s = 0; s < b.stories(); s++) {
				p.set(lamp[0], WALK + 4 * s, lamp[1], s(Blocks.LANTERN));
			}
		}
		roof(p, poly, top + 1, b.roof(), b.pitch());
		extras(p, b, top);
	}

	private void windows(P p, Building b, int x, int z, Direction out, int top) {
		int along = out.getAxis() == Direction.Axis.Z ? x : z;
		if (b.outline() == A) {
			along = x + z;
		}
		if (Math.floorMod(along, b.windowEvery()) != 1) {
			return;
		}
		boolean alongX = out.getAxis() == Direction.Axis.Z;
		for (int s = 0; s < b.stories(); s++) {
			int y0 = WALK + 4 * s;
			if (s == 0 && Math.abs(along - (alongX ? Math.round(b.doorX()) : Math.round(b.doorZ()))) <= 1
					&& out == outward(b.outline(), (int) Math.floor(b.doorX()), (int) Math.floor(b.doorZ()))) {
				continue;
			}
			p.set(x, y0 + 1, z, Kit.connected(b.glass(), alongX));
			p.set(x, y0 + 2, z, Kit.connected(b.glass(), alongX));
			p.set(x, y0 + 3, z, Kit.slab(Blocks.SMOOTH_QUARTZ_SLAB, true)); // trim
			if (b.shutter() != null) {
				for (int side : new int[]{-1, 1}) {
					int sx = x + (alongX ? side : 0) + out.getStepX(), sz = z + (alongX ? 0 : side) + out.getStepZ();
					BlockState shutter = s(b.shutter()).setValue(TrapDoorBlock.FACING, out).setValue(TrapDoorBlock.OPEN, true)
							.setValue(TrapDoorBlock.HALF, Half.BOTTOM);
					p.set(sx, y0 + 1, sz, shutter);
					p.set(sx, y0 + 2, sz, shutter.setValue(TrapDoorBlock.HALF, Half.TOP));
				}
			}
		}
	}

	/**
	 * Hipped roof over a polygon: each ring of cells in from the outline steps up (pitch 1: a stair per ring; pitch 2:
	 * stair and full-block steps, or slabs when the family has them). Ridges are full blocks; the inside is filled.
	 */
	static void roof(P p, Poly poly, int base, Roof roof, int pitch) {
		for (int x = poly.minX; x <= poly.maxX; x++) {
			for (int z = poly.minZ; z <= poly.maxZ; z++) {
				double d = poly.d(x, z);
				if (d < 0) {
					continue;
				}
				int k = (int) Math.floor(d);
				int y = base + k / pitch;
				Direction upward = null;
				int bestK = k;
				for (Direction dir : DIRS) {
					double nd = poly.d(x + dir.getStepX(), z + dir.getStepZ());
					int nk = nd < 0 ? -1 : (int) Math.floor(nd);
					if (nk > bestK) {
						bestK = nk;
						upward = dir;
					}
				}
				BlockState block;
				if (upward == null) {
					block = s(roof.ridge());
				} else if (pitch == 2 && roof.slab() != null) {
					block = s(roof.slab()).setValue(SlabBlock.TYPE, k % 2 == 0 ? SlabType.BOTTOM : SlabType.TOP);
				} else if (pitch == 2 && k % 2 == 1) {
					block = s(roof.ridge()); // stepped edge
				} else {
					block = Kit.stairs(roof.stairs(), upward, false);
				}
				p.set(x, y, z, block);
				if (k >= 1) {
					p.fill(x, base, z, x, y - 1, z, s(roof.ridge()));
				}
			}
		}
	}

	/** Raised square tower or roof section: walls from the roof base up {@code rise} blocks, windows, its own roof. */
	private void raised(P p, int x1, int x2, int z1, int z2, int fromY, int rise, Block wall, Roof roof, Block glass) {
		for (int x = x1; x <= x2; x++) {
			for (int z = z1; z <= z2; z++) {
				p.fill(x, fromY, z, x, fromY + rise - 1, z, s(wall));
				boolean edgeX = x == x1 || x == x2, edgeZ = z == z1 || z == z2;
				if ((edgeX ^ edgeZ) && Math.floorMod(edgeZ ? x : z, 2) == 0 && rise >= 3) {
					p.fill(x, fromY + rise - 3, z, x, fromY + rise - 2, z, Kit.connected(glass, edgeZ));
				}
			}
		}
		roof(p, Poly.rect(x1 - 1, x2 + 1, z1 - 1, z2 + 1), fromY + rise, roof, 1);
	}

	/** Highest y of a building's main roof (its ridge). */
	static int roofTop(Building b, int top) {
		double max = 0;
		Poly poly = b.outline();
		for (int x = poly.minX; x <= poly.maxX; x++) {
			for (int z = poly.minZ; z <= poly.maxZ; z++) {
				max = Math.max(max, poly.d(x, z));
			}
		}
		return top + 1 + (int) Math.floor(max) / b.pitch();
	}

	/** Per-building features: towers and raised sections that clear the main roof, dormers, a cupola, the dealer's shop. */
	private void extras(P p, Building b, int top) {
		int ridge = roofTop(b, top);
		switch (b.id()) {
			case "A" -> raised(p, -26, -22, -23, -19, top + 1, ridge - top + 4, Blocks.END_STONE_BRICKS, ROSE, Blocks.GLASS_PANE);
			case "G" -> raised(p, 52, 56, -20, -16, top + 1, ridge - top + 4, Blocks.WHITE_TERRACOTTA, ROSE, Blocks.GLASS_PANE);
			case "C" -> raised(p, 6, 12, -28, -22, top + 1, ridge - top - 1, Blocks.WHITE_TERRACOTTA, BLUE, Blocks.GLASS_PANE);
			case "I" -> raised(p, 55, 62, 14, 21, top + 1, ridge - top - 2, Blocks.WHITE_TERRACOTTA, BLUE, Blocks.GLASS_PANE);
			case "K" -> raised(p, 28, 29, 29, 31, ridge, 2, Blocks.WHITE_TERRACOTTA, ROSE, Blocks.GLASS_PANE);
			case "B" -> {
				dormers(p, b.outline(), top + 1, Direction.SOUTH, Blocks.SANDSTONE, TAN, b.roof());
				fruitShop(p);
			}
			case "E" -> dormers(p, b.outline(), top + 1, Direction.SOUTH, Blocks.END_STONE_BRICKS, TEAL, b.roof());
			case "J" -> dormers(p, b.outline(), top + 1, Direction.NORTH, Blocks.SANDSTONE, TAN, b.roof());
			default -> {
			}
		}
	}

	/** Small dormers along one slope: a window box with its own little ridge, every five blocks. */
	private void dormers(P p, Poly poly, int base, Direction side, Block wall, Roof cap, Roof roof) {
		int z = side == Direction.SOUTH ? poly.maxZ - 2 : poly.minZ + 1;  // the wall ring row on that side
		for (int x = poly.minX + 4; x <= poly.maxX - 4; x += 5) {
			int y = base;
			for (int dx = -1; dx <= 1; dx++) {
				p.fill(x + dx, y, z, x + dx, y + 1, z, s(wall));
				p.fill(x + dx, y, z - side.getStepZ(), x + dx, y + 1, z - side.getStepZ(), s(wall));
			}
			p.set(x, y + 1, z, Kit.connected(Blocks.GLASS_PANE, true));
			p.set(x, y, z, Kit.slab(Blocks.SMOOTH_QUARTZ_SLAB, true));
			p.set(x - 1, y + 2, z, Kit.stairs(cap.stairs(), Direction.EAST, false));
			p.set(x, y + 2, z, s(cap.ridge()));
			p.set(x + 1, y + 2, z, Kit.stairs(cap.stairs(), Direction.WEST, false));
			p.set(x - 1, y + 2, z - side.getStepZ(), Kit.stairs(cap.stairs(), Direction.EAST, false));
			p.set(x, y + 2, z - side.getStepZ(), s(cap.ridge()));
			p.set(x + 1, y + 2, z - side.getStepZ(), Kit.stairs(cap.stairs(), Direction.WEST, false));
		}
	}

	/** The Fruit Dealer's shop inside B: a counter across the room, shelves at the back. */
	private void fruitShop(P p) {
		for (int x = -12; x <= -3; x++) {
			if (x == -3) {
				continue; // gap to get behind the counter
			}
			p.set(x, WALK, -24, Kit.slab(Blocks.SPRUCE_SLAB, true));
		}
		for (int x = -12; x <= -3; x += 3) {
			p.set(x, WALK, -31, s(Blocks.BARREL));
			p.set(x + 1, WALK, -31, s(Blocks.COMPOSTER));
		}
		p.set(-10, WALK + 1, -24, s(Blocks.MELON));
		p.set(-5, WALK + 1, -24, s(Blocks.PUMPKIN));
	}

	// ---------------------------------------------------------------------------------------------------- civic hall

	/** F: white civic hall, two long copper-roofed wings, a tall drum and a stepped copper dome with a lantern. */
	private void civicHall(P p) {
		Building hall = new Building("F", F, Blocks.SMOOTH_QUARTZ, 2, COPPER, 2, 17, -2.5, Blocks.LIGHT_BLUE_STAINED_GLASS_PANE, 3, null);
		int top = wallTop(2);
		if (LAYOUT_ONLY) {
			building(p, hall);
			return;
		}
		building(p, hall);
		// Replace the single roof with two wing roofs and a flat roof between them.
		BlockState air = s(Blocks.AIR);
		p.fill(F.minX, top + 1, F.minZ, F.maxX, top + 12, F.maxZ, air);
		Poly north = Poly.rect(17, 44, -15, -8), south = Poly.rect(17, 44, 3, 13);
		p.fill(18, top + 1, -8, 43, top + 1, 2, Kit.slab(Blocks.OXIDIZED_CUT_COPPER_SLAB, false));
		roof(p, north, top + 1, COPPER, 2);
		roof(p, south, top + 1, COPPER, 2);
		// Portico: a wide entrance toward the fountain with quartz columns.
		p.fill(18, WALK, -4, 18, WALK + 3, -1, air);
		for (int z = -6; z <= 1; z += 7) {
			p.fill(17, WALK, z, 17, top, z, s(Blocks.SMOOTH_QUARTZ));
		}
		// Drum: a ring of quartz rising above the wings, with tall windows; open rotunda inside.
		int drumTop = top + 11;
		for (int x = 23; x <= 40; x++) {
			for (int z = -11; z <= 6; z++) {
				double d = Math.hypot(x + 0.5 - DOME_X, z + 0.5 - DOME_Z);
				if (d > DRUM_R) {
					continue;
				}
				if (d > DRUM_R - 1.2) {
					p.fill(x, WALK, z, x, drumTop, z, s(Blocks.SMOOTH_QUARTZ));
					if (Math.floorMod(x * 3 + z * 5, 3) == 0) {
						p.fill(x, top + 3, z, x, drumTop - 2, z, s(Blocks.LIGHT_BLUE_STAINED_GLASS_PANE));
					}
				} else {
					p.fill(x, WALK, z, x, drumTop, z, air);
					p.set(x, STREET, z, (x + z) % 2 == 0 ? s(Blocks.SMOOTH_QUARTZ) : s(Blocks.POLISHED_ANDESITE));
				}
			}
		}
		// Doorways from the rotunda into the hall on the fountain side.
		p.fill(23, WALK, -4, 25, WALK + 3, -1, air);
		// Dome: stepped copper rings, a shell over the open rotunda.
		int[][] tops = new int[19][19];
		for (int i = 0; i < 19; i++) {
			for (int j = 0; j < 19; j++) {
				int x = 22 + i, z = -12 + j;
				double d = Math.hypot(x + 0.5 - DOME_X, z + 0.5 - DOME_Z);
				tops[i][j] = d > DRUM_R ? drumTop : drumTop + 1 + (int) Math.floor(7.0 * Math.sqrt(Math.max(0, 1 - (d / DRUM_R) * (d / DRUM_R))));
			}
		}
		for (int i = 1; i < 18; i++) {
			for (int j = 1; j < 18; j++) {
				int x = 22 + i, z = -12 + j;
				if (Math.hypot(x + 0.5 - DOME_X, z + 0.5 - DOME_Z) > DRUM_R) {
					continue;
				}
				int y = tops[i][j];
				int low = Math.min(Math.min(tops[i + 1][j], tops[i - 1][j]), Math.min(tops[i][j + 1], tops[i][j - 1]));
				p.fill(x, Math.min(low + 1, y), z, x, y - 1, z, s(Blocks.OXIDIZED_CUT_COPPER));
				Direction up = null;
				int best = y;
				int[][] n = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
				Direction[] nd = {Direction.EAST, Direction.WEST, Direction.SOUTH, Direction.NORTH};
				for (int q = 0; q < 4; q++) {
					if (tops[i + n[q][0]][j + n[q][1]] > best) {
						best = tops[i + n[q][0]][j + n[q][1]];
						up = nd[q];
					}
				}
				p.set(x, y, z, up == null ? s(Blocks.OXIDIZED_CUT_COPPER) : Kit.stairs(Blocks.OXIDIZED_CUT_COPPER_STAIRS, up, false));
			}
		}
		// Lantern tower on the crown.
		int crown = drumTop + 8;
		for (int[] c : new int[][]{{30, -4}, {32, -4}, {30, -2}, {32, -2}}) {
			p.fill(c[0], crown, c[1], c[0], crown + 2, c[1], s(Blocks.SMOOTH_QUARTZ));
		}
		p.fill(31, crown, -4, 31, crown + 2, -4, Kit.connected(Blocks.LIGHT_BLUE_STAINED_GLASS_PANE, true));
		p.fill(31, crown, -2, 31, crown + 2, -2, Kit.connected(Blocks.LIGHT_BLUE_STAINED_GLASS_PANE, true));
		p.fill(30, crown, -3, 30, crown + 2, -3, Kit.connected(Blocks.LIGHT_BLUE_STAINED_GLASS_PANE, false));
		p.fill(32, crown, -3, 32, crown + 2, -3, Kit.connected(Blocks.LIGHT_BLUE_STAINED_GLASS_PANE, false));
		p.set(31, crown, -3, s(Blocks.LANTERN));
		p.fill(30, crown + 3, -4, 32, crown + 3, -2, Kit.slab(Blocks.OXIDIZED_CUT_COPPER_SLAB, false));
		p.set(31, crown + 3, -3, s(Blocks.OXIDIZED_CUT_COPPER));
		p.set(31, crown + 4, -3, s(Blocks.LANTERN));
		// Hanging lanterns in the rotunda.
		p.set(31, drumTop, -3, s(Blocks.LANTERN).setValue(LanternBlock.HANGING, true));
	}

	/** Highest block of the dome's lantern (tests). */
	static int domeTop() {
		return wallTop(2) + 11 + 8 + 4;
	}

	// ---------------------------------------------------------------------------------------------------- pavilions

	private void pavilion(P p, Poly roofShape, Poly base, Roof roof, int[][] pillars) {
		for (int x = base.minX; x <= base.maxX; x++) {
			for (int z = base.minZ; z <= base.maxZ; z++) {
				double d = base.d(x, z);
				if (d < 0) {
					continue;
				}
				p.set(x, WALK, z, d < 1 ? Kit.slab(Blocks.SMOOTH_QUARTZ_SLAB, false) : s(Blocks.SMOOTH_QUARTZ));
			}
		}
		if (LAYOUT_ONLY) {
			return;
		}
		for (int[] c : pillars) {
			p.fill(c[0], WALK + 1, c[1], c[0], WALK + 4, c[1], s(Blocks.SMOOTH_QUARTZ));
		}
		roof(p, roofShape, WALK + 5, roof, 1);
		int cx = (int) Math.floor(roofShape.cx()), cz = (int) Math.floor(roofShape.cz());
		p.set(cx, WALK + 4, cz, s(Blocks.LANTERN).setValue(LanternBlock.HANGING, true));
	}

	// ---------------------------------------------------------------------------------------------------- gacha cart

	/**
	 * The Fruit Gacha cart: a spruce cart on the H side of the lane (cart body x 49-50, z -11..-7), two big dark wheels
	 * on its lane side, a counter, posts and a white canvas canopy that reaches over the keeper at (48, -9).
	 */
	private void gachaCart(P p) {
		BlockState log = s(Blocks.STRIPPED_SPRUCE_LOG).setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z);
		BlockState post = s(Blocks.STRIPPED_SPRUCE_LOG);
		// Chassis and bed.
		p.fill(49, WALK, -11, 50, WALK, -7, log);
		p.fill(50, WALK + 1, -10, 50, WALK + 1, -8, Kit.slab(Blocks.SPRUCE_SLAB, false));
		// Counter on the lane side, between the posts.
		p.fill(49, WALK + 1, -10, 49, WALK + 1, -8, Kit.slab(Blocks.SPRUCE_SLAB, true));
		p.fill(51, WALK + 1, -10, 51, WALK + 1, -8, s(Blocks.SPRUCE_TRAPDOOR).setValue(TrapDoorBlock.FACING, Direction.EAST)
				.setValue(TrapDoorBlock.OPEN, true)); // side board on the far side
		// Wheels: 2 x 2 dark faces on both sides.
		for (int[] w : new int[][]{{-11, -10}, {-8, -7}}) {
			for (int z = w[0]; z <= w[1]; z++) {
				for (int y = WALK; y <= WALK + 1; y++) {
					p.set(48, y, z, s(Blocks.DARK_OAK_TRAPDOOR).setValue(TrapDoorBlock.FACING, Direction.WEST).setValue(TrapDoorBlock.OPEN, true));
					if (y == WALK) {
						p.set(51, y, z, s(Blocks.DARK_OAK_TRAPDOOR).setValue(TrapDoorBlock.FACING, Direction.EAST).setValue(TrapDoorBlock.OPEN, true));
					}
				}
			}
		}
		p.fill(48, WALK, -9, 48, WALK + 2, -9, s(Blocks.AIR)); // the keeper's spot between the wheels
		// Handles.
		p.set(49, WALK + 1, -12, s(Blocks.SPRUCE_FENCE));
		p.set(50, WALK + 1, -12, s(Blocks.SPRUCE_FENCE));
		// Posts and canopy (canvas over the cart and the keeper).
		for (int[] c : new int[][]{{49, -11}, {49, -7}, {50, -11}, {50, -7}}) {
			p.fill(c[0], WALK + 1, c[1], c[0], WALK + 3, c[1], post);
		}
		p.fill(47, WALK + 4, -11, 50, WALK + 4, -7, s(Blocks.WHITE_WOOL));
		p.fill(48, WALK + 5, -11, 49, WALK + 5, -7, s(Blocks.WHITE_CARPET));
		for (int z = -11; z <= -7; z++) {
			p.set(46, WALK + 4, z, s(Blocks.SPRUCE_TRAPDOOR).setValue(TrapDoorBlock.FACING, Direction.WEST)
					.setValue(TrapDoorBlock.OPEN, true).setValue(TrapDoorBlock.HALF, Half.TOP)); // canopy valance
		}
		p.set(49, WALK + 3, -9, s(Blocks.LANTERN).setValue(LanternBlock.HANGING, true));
	}

	// ---------------------------------------------------------------------------------------------------- lamps

	private void lamps(P p) {
		int[][] spots = {{-20, -10}, {-20, 10}, {13, -11}, {13, 11}, {-46, -9}, {-46, 8}, {80, -14}, {80, 0}, {80, 15},
				{47, -21}, {46, 15}, {-2, -15}, {6, 15}, {20, -18}, {33, 18}};
		for (int[] l : spots) {
			if (p.near(l[0], l[1], 1) && onPlatform(l[0], l[1])) {
				Kit.lamp(p.c, l[0] + OX, l[1], STREET);
			}
		}
		bedTrees(p);
	}

	// ---------------------------------------------------------------------------------------------------- tests

	/** Open paved street at plan (x, z): on the platform, not under a roof, not a planted bed or the fountain (tests). */
	public static boolean openStreet(int x, int z) {
		return onPlatform(x, z) && !underEaves(x, z) && bedAt(x, z) == null && !fountainCell(x, z);
	}

	/** Plan-coordinate labels for the overhead review: id, x, z. */
	public static List<Object[]> labels() {
		List<Object[]> out = new ArrayList<>();
		for (Building b : BUILDINGS) {
			out.add(new Object[]{b.id(), b.outline().cx(), b.outline().cz()});
		}
		out.add(new Object[]{"F", F.cx(), F.cz()});
		out.add(new Object[]{"D", D_ROOF.cx(), D_ROOF.cz()});
		out.add(new Object[]{"N", N_ROOF.cx(), N_ROOF.cz()});
		for (int i = 0; i < PIERS.length; i++) {
			out.add(new Object[]{PIER_NAMES[i], (PIERS[i][0] + PIERS[i][1]) / 2.0, (PIERS[i][2] + PIERS[i][3]) / 2.0});
		}
		out.add(new Object[]{"Fountain", 0.5, 0.5});
		out.add(new Object[]{"Gacha cart", CART_X + 0.5, CART_Z + 0.5});
		return out;
	}
}
