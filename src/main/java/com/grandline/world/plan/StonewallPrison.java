package com.grandline.world.plan;

import com.grandline.world.plan.Anchor.Type;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.ChainBlock;
import net.minecraft.world.level.block.LightningRodBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.WallBannerBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Stonewall Prison (layout 15), built to the owner's specification: a round ocean fortress.
 * <pre>
 *  piece                    radius (local, from the tower's centre)      y
 *  outer wall               r ~42-44 (irregular), 5 high, battlements   ground 66, top 71
 *  guard towers             five in the wall, 5 across, blue domes       67-75, domes to 80
 *  gatehouse                south, twin towers, portcullis, blue roof    to 82
 *  entrance bridge          over the moat inside the gate                66
 *  outer courtyard          paved ring r 35-42; grass sectors r 27-34
 *  inner enclosure          ring r 21-26, curved corridor inside, ledge  66-75, ledge 75
 *  central courtyard        r 12-21: grass, dark paths, radial bridges   66; bridges at 75
 *  central tower            lower r 12 (atrium, 4 cell levels), middle r 10, upper r 8 (furnished room), roof 101
 *  secondary chamber        underground, r 20, towers, chains, cages     47-65 (the boss arena)
 *  waterfront landing       west, at sea level                           63-65
 * </pre>
 * Island-local coordinates: +x east, +z south. Deterministic; older worlds keep their prison.
 */
final class StonewallPrison extends BasePlan {
	static final int SEA = 62, G = 66, WALL_TOP = 71, ENC_TOP = 75;
	static final double R_ENC_IN = 21, R_ENC_OUT = 26, R_TOWER = 12;
	/** Atrium floors: ground and three galleries; the atrium's ceiling (the middle floor), the upper floor, the roof. */
	static final int[] LEVELS = {66, 71, 75, 79};
	static final int MID_FLOOR = 85, UP_FLOOR = 94, ROOF = 101;
	/** The underground chamber. */
	static final int CH_FLOOR = 47, CH_CEIL = 65;
	static final double CH_R = 20;
	/** Perimeter towers (degrees: 0 east, 90 south). */
	static final int[] TOWERS = {30, 150, 210, 270, 330};
	static final int GATE_Z = 42;

	StonewallPrison() {
		super("Stonewall Prison", 0.04);
		// The fortress floor as land (keepers, spawns and the island's gameplay area read the terrain).
		terrain.flat(0, 0, 42, 42, G, 1, null).reach(82);
		anchor(new Anchor(Type.ARRIVAL, 0, 65, 53, 0, 180));
		anchor(Anchor.at(Type.QUEST, 12, G + 1, 36, 0));
		anchor(Anchor.at(Type.HOME, -12, G + 1, 36, 0));
		anchor(Anchor.at(Type.FISHERMAN, 0, 65, 72, 180));
		anchor(Anchor.at(Type.DOCK, -4, 63, 68));
		anchor(new Anchor(Type.ENEMY_CAMP, 0, G, -38, 6));       // the outer courtyard, north (basic)
		anchor(new Anchor(Type.ENEMY_CAMP, 0, G, -16, 4));       // the central courtyard (stronger)
		anchor(Anchor.at(Type.BOSS_ARENA, 0, CH_FLOOR, -10));    // the secondary chamber, underground
	}

	protected static BlockState s(Block b) {
		return b.defaultBlockState();
	}

	static double unit(int x, int z, int salt) {
		return CloudtopIslesV7.unit(x, z, salt);
	}

	static double angle(double x, double z) {
		double a = Math.toDegrees(Math.atan2(z, x));
		return a < 0 ? a + 360 : a;
	}

	static double angDiff(double a, double b) {
		double d = Math.abs(((a - b) % 360 + 360) % 360);
		return Math.min(d, 360 - d);
	}

	/** The outer wall's inner radius at a bearing: about 42, gently irregular. */
	static double rIn(double deg) {
		double t = Math.toRadians(deg);
		return 42 + 0.9 * Math.sin(3 * t + 0.7) + 0.6 * Math.sin(5 * t + 1.9);
	}

	static Direction outward(double dx, double dz) {
		return Math.abs(dx) >= Math.abs(dz) ? (dx > 0 ? Direction.EAST : Direction.WEST) : (dz > 0 ? Direction.SOUTH : Direction.NORTH);
	}

	/** Direction of travel along a circle at a bearing (increasing angle). */
	static Direction tangent(double deg) {
		double t = Math.toRadians(deg);
		return outward(-Math.sin(t), Math.cos(t));
	}

	static BlockState lantern(boolean hanging) {
		return CloudtopIsles.lantern(Blocks.LANTERN, hanging);
	}

	static BlockState masonry(int x, int y, int z) {
		return unit(x * 3 + y, z, 17) < 0.06 ? s(Blocks.CRACKED_STONE_BRICKS) : s(Blocks.STONE_BRICKS);
	}

	/** A black wall banner (the fortress colours). */
	static void banner(Canvas c, int x, int y, int z, Direction facing, Block b) {
		c.set(x, y, z, s(b).setValue(WallBannerBlock.FACING, facing));
	}

	// ==================================================================================================== build

	@Override
	public void build(Canvas c) {
		if (!c.near(0, 10, 75)) {
			return;
		}
		foundation(c);
		outerWall(c);
		for (int deg : TOWERS) {
			double t = Math.toRadians(deg);
			double r = rIn(deg) + 1;
			guardTower(c, (int) Math.round(Math.cos(t) * r), (int) Math.round(Math.sin(t) * r), 2.5, G + 1, 9, 3, null, true);
		}
		gatehouse(c);
		courtyards(c);
		enclosure(c);
		tower(c);
		atrium(c);
		upperRooms(c);
		centralCourtyard(c);
		chamber(c);
		landing(c);
	}

	// ---------------------------------------------------------------------------------------------------- ground

	/** The island: solid stone from the sea bed up under the fortress, its surface at y 66. */
	private void foundation(Canvas c) {
		for (int x = -48; x <= 48; x++) {
			for (int z = -48; z <= 48; z++) {
				if (!c.inChunk(x, z)) {
					continue;
				}
				double d = Math.hypot(x, z);
				double ri = rIn(angle(x, z));
				if (d > ri + 2.2) {
					continue;
				}
				int bed = Math.min(c.ground(x, z), SEA - 4);
				c.fill(x, bed, z, x, G - 1, z, s(Blocks.STONE));
				c.set(x, G, z, s(Blocks.SMOOTH_STONE));
				c.fill(x, G + 1, z, x, G + 12, z, s(Blocks.AIR));
			}
		}
	}

	// ---------------------------------------------------------------------------------------------------- outer wall

	/**
	 * The outer wall: two thick, from the sea to y 71, a walk on top with square battlements on the outer edge, a white
	 * band (calcite with white concrete diamonds) round the outer face, and buttresses widening toward the sea.
	 */
	private void outerWall(Canvas c) {
		for (int x = -48; x <= 48; x++) {
			for (int z = -48; z <= 48; z++) {
				if (!c.inChunk(x, z)) {
					continue;
				}
				double d = Math.hypot(x, z), a = angle(x, z), ri = rIn(a);
				boolean gate = z > 30 && Math.abs(x) <= 3;
				if (d <= ri || d > ri + 2.2 || gate) {
					continue;
				}
				boolean outer = d > ri + 1.1;
				int bed = Math.min(c.ground(x, z), SEA - 4);
				double arc = Math.toRadians(a) * ri;
				int diamond = (int) Math.round(Math.abs(((arc % 12) + 12) % 12 - 6));
				for (int y = bed; y <= WALL_TOP; y++) {
					BlockState b = y < G - 1 ? s(Blocks.STONE_BRICKS) : masonry(x, y, z);
					if (outer && y == G + 3) {
						b = s(Blocks.CALCITE);
					}
					if (outer && diamond <= 1 && (y == G + 2 || y == G + 4) && diamond == 0) {
						b = s(Blocks.WHITE_CONCRETE);
					}
					if (outer && diamond == 1 && y == G + 3) {
						b = s(Blocks.WHITE_CONCRETE);
					}
					c.set(x, y, z, b);
				}
				c.set(x, WALL_TOP, z, outer ? s(Blocks.STONE_BRICKS) : s(Blocks.SMOOTH_STONE));
				if (outer) {
					c.set(x, WALL_TOP + 1, z, Math.floorMod((int) Math.round(arc), 3) == 0 ? s(Blocks.STONE_BRICK_WALL) : s(Blocks.STONE_BRICKS));
					if (Math.floorMod((int) Math.round(arc), 3) == 0) {
						c.set(x, WALL_TOP + 1, z, s(Blocks.AIR));
					}
				}
			}
		}
		// Buttresses every 24 degrees (not at towers or the gate), widening toward the sea.
		for (int deg = 6; deg < 360; deg += 24) {
			boolean nearTower = false;
			for (int t : TOWERS) {
				nearTower |= angDiff(deg, t) < 9;
			}
			if (nearTower || angDiff(deg, 90) < 14 || angDiff(deg, 180) < 6) {
				continue;
			}
			double t = Math.toRadians(deg), ux = Math.cos(t), uz = Math.sin(t);
			double r0 = rIn(deg) + 2;
			for (int step = 1; step <= 2; step++) {
				for (double k = -1; k <= 1; k += 0.5) {
					int x = (int) Math.round(ux * (r0 + step) - uz * k), z = (int) Math.round(uz * (r0 + step) + ux * k);
					if (!c.inChunk(x, z)) {
						continue;
					}
					int top = step == 1 ? G + 2 : G - 1;
					int bed = Math.min(c.ground(x, z), SEA - 4);
					c.fill(x, bed, z, x, top, z, Math.abs(k) < 0.6 ? s(Blocks.POLISHED_ANDESITE) : s(Blocks.STONE_BRICKS));
					c.set(x, top + 1, z, Kit.stairs(Blocks.STONE_BRICK_STAIRS, outward(-ux, -uz), false));
				}
			}
		}
		// Black banners on the wall's inner face.
		for (int deg = 18; deg < 360; deg += 36) {
			if (angDiff(deg, 90) < 15) {
				continue;
			}
			double t = Math.toRadians(deg);
			double r = rIn(deg) - 0.2;
			int x = (int) Math.round(Math.cos(t) * r), z = (int) Math.round(Math.sin(t) * r);
			Direction in = outward(-Math.cos(t), -Math.sin(t));
			if (c.inChunk(x, z) && Math.hypot(x - in.getStepX(), z - in.getStepZ()) > 0) {
				int wx = x - in.getStepX(), wz = z - in.getStepZ();
				if (Math.hypot(wx, wz) > rIn(angle(wx, wz))) {
					banner(c, x, G + 4, z, in, Blocks.BLACK_WALL_BANNER);
				}
			}
		}
	}

	/**
	 * A round guard tower: a wider foundation in the sea, a pale body of {@code r} radius with three narrow barred slits
	 * facing out, a door in and a ladder to the wall walk, a projecting rim, a layered blue dome, a small black flag.
	 */
	private static void guardTower(Canvas c, int cx, int cz, double r, int y0, int h, int domeR, Direction door, boolean wallWalk) {
		if (!c.near(cx, cz, (int) r + 4)) {
			return;
		}
		Direction out = outward(cx, cz), in = out.getOpposite();
		for (int dx = -(int) r - 2; dx <= r + 2; dx++) {
			for (int dz = -(int) r - 2; dz <= r + 2; dz++) {
				double d = Math.hypot(dx, dz);
				int x = cx + dx, z = cz + dz;
				int bed = Math.min(c.ground(x, z), SEA - 4);
				if (d <= r + 1.2) {
					c.fill(x, bed, z, x, y0 - 1, z, d > r + 0.4 ? s(Blocks.POLISHED_ANDESITE) : s(Blocks.STONE_BRICKS));
					if (d > r + 0.4) {
						c.set(x, y0, z, Kit.slab(Blocks.STONE_BRICK_SLAB, false));
					}
				}
				if (d > r + 0.4) {
					continue;
				}
				boolean shell = d > r - 1.1;
				for (int y = y0; y < y0 + h; y++) {
					BlockState b = shell ? (y >= y0 + 3 ? s(Blocks.END_STONE_BRICKS) : s(Blocks.STONE_BRICKS)) : s(Blocks.AIR);
					c.set(x, y, z, b);
				}
				if (!shell) {
					c.set(x, y0 - 1, z, s(Blocks.SMOOTH_STONE));
				}
				c.set(x, y0 + h, z, s(Blocks.STONE_BRICKS));
			}
		}
		// Three narrow slits facing out.
		Direction side = out.getClockWise();
		for (int k = -1; k <= 1; k++) {
			int sx = cx + out.getStepX() * (int) Math.floor(r) + side.getStepX() * k, sz = cz + out.getStepZ() * (int) Math.floor(r) + side.getStepZ() * k;
			if (k != 0) {
				sx -= out.getStepX();
				sz -= out.getStepZ();
			}
			c.fill(sx, y0 + 3, sz, sx, y0 + 5, sz, Kit.connected(Blocks.IRON_BARS, out.getAxis() == Direction.Axis.Z));
		}
		// Door inward at the bottom, ladder up to an opening onto the wall walk.
		Direction dd = door != null ? door : in;
		int ix = cx + dd.getStepX() * (int) Math.floor(r), iz = cz + dd.getStepZ() * (int) Math.floor(r);
		c.fill(ix, y0, iz, ix, y0 + 1, iz, s(Blocks.AIR));
		c.set(ix, y0 - 1, iz, s(Blocks.SMOOTH_STONE));
		int ld = (int) Math.floor(r - 1.1);
		Direction ls = door == side ? side.getOpposite() : side; // never against the door
		int lx = cx + ls.getStepX() * ld, lz = cz + ls.getStepZ() * ld;
		for (int y = y0; y < (wallWalk ? WALL_TOP + 1 : y0 + h); y++) {
			c.set(lx, y, lz, Canvas.facing(s(Blocks.LADDER), ls.getOpposite()));
		}
		for (Direction sd : wallWalk ? new Direction[]{side.getOpposite()} : new Direction[0]) {
			int ox = cx + sd.getStepX() * (int) Math.floor(r), oz = cz + sd.getStepZ() * (int) Math.floor(r);
			c.fill(ox, WALL_TOP + 1, oz, ox, WALL_TOP + 2, oz, s(Blocks.AIR));
		}
		// Rim and dome.
		int ry = y0 + h + 1;
		for (int dx = -domeR - 1; dx <= domeR + 1; dx++) {
			for (int dz = -domeR - 1; dz <= domeR + 1; dz++) {
				double d = Math.hypot(dx, dz);
				int x = cx + dx, z = cz + dz;
				if (d <= domeR + 1.3 && d > domeR + 0.3) {
					c.set(x, ry, z, Kit.slab(Blocks.DARK_PRISMARINE_SLAB, false));
				}
				for (int k = 0; k <= domeR; k++) {
					double rk = Math.sqrt(Math.max(0, (domeR + 0.4) * (domeR + 0.4) - k * k));
					if (d <= rk) {
						boolean edge = d > rk - 1;
						BlockState b = k == 0 ? s(Blocks.BLUE_TERRACOTTA) : edge && k < domeR && d > rk - 0.6 ? Kit.stairs(Blocks.DARK_PRISMARINE_STAIRS, outward(-dx, -dz), false)
								: s(Blocks.BLUE_CONCRETE);
						if (k > 0 && !edge) {
							b = s(Blocks.BLUE_CONCRETE);
						}
						c.set(x, ry + k, z, b);
					}
				}
			}
		}
		int top = ry + domeR + 1;
		c.fill(cx, top, cz, cx, top + 1, cz, s(Blocks.POLISHED_BLACKSTONE_WALL));
		c.set(cx, top + 2, cz, s(Blocks.POLISHED_BLACKSTONE));
		banner(c, cx + out.getStepX(), top + 2, cz + out.getStepZ(), out, Blocks.BLACK_WALL_BANNER);
		c.set(cx, top + 3, cz, s(Blocks.LIGHTNING_ROD));
	}

	// ---------------------------------------------------------------------------------------------------- gate

	/**
	 * The gatehouse on the south: two big round towers with blue domes, the stone gatehouse between them with a tall
	 * pointed arch, a raised iron portcullis with spikes, chains either side, dark spikes along the sloped blue roof, a
	 * small stone arrival platform and the pier; inside, the wooden bridge over the moat and the barred checkpoint.
	 */
	private void gatehouse(Canvas c) {
		if (!c.near(0, 48, 28)) {
			return;
		}
		int y = G;
		// The gatehouse block.
		for (int x = -4; x <= 4; x++) {
			for (int z = GATE_Z - 2; z <= GATE_Z + 8; z++) {
				int bed = Math.min(c.ground(x, z), SEA - 4);
				c.fill(x, bed, z, x, y, z, s(Blocks.STONE_BRICKS));
				for (int k = 1; k <= 10; k++) {
					boolean passage = Math.abs(x) <= 2 && k <= archTop(x);
					c.set(x, y + k, z, passage ? s(Blocks.AIR) : k == 4 && (z == GATE_Z + 8 || z == GATE_Z - 2) ? s(Blocks.CALCITE) : masonry(x, y + k, z));
				}
				c.set(x, y, z, Math.abs(x) <= 2 ? (Math.floorMod(x + z, 2) == 0 ? s(Blocks.SMOOTH_STONE) : s(Blocks.POLISHED_ANDESITE)) : s(Blocks.STONE_BRICKS));
			}
		}
		// Arch shoulders (stairs) on both faces.
		for (int z : new int[]{GATE_Z - 2, GATE_Z + 8}) {
			for (int x : new int[]{-2, 2}) {
				c.set(x, y + archTop(x) + 1, z, Kit.stairs(Blocks.STONE_BRICK_STAIRS, x < 0 ? Direction.EAST : Direction.WEST, true));
			}
		}
		// The portcullis: raised, its bars filling the arch's top, spikes pointing down.
		int pz = GATE_Z + 6;
		for (int x = -2; x <= 2; x++) {
			int top = archTop(x);
			for (int k = 6; k <= top; k++) {
				c.set(x, y + k, pz, Kit.connected(Blocks.IRON_BARS, true));
			}
			c.set(x, y + 5, pz, s(Blocks.LIGHTNING_ROD).setValue(LightningRodBlock.FACING, Direction.DOWN));
		}
		// Chains either side of the arch on the front, spikes along the roof edge.
		for (int x : new int[]{-3, 3}) {
			c.fill(x, y + 3, GATE_Z + 9, x, y + 8, GATE_Z + 9, s(Blocks.CHAIN));
			c.set(x, y + 9, GATE_Z + 9, s(Blocks.POLISHED_BLACKSTONE));
		}
		// Sloped roof (a gable along z), blue concrete over blue terracotta, dark prismarine edges, spikes on the ridge.
		for (int x = -5; x <= 5; x++) {
			int ax = Math.abs(x);
			int rise = 5 - ax;
			for (int z = GATE_Z - 3; z <= GATE_Z + 9; z++) {
				for (int k = 0; k < rise; k++) {
					c.set(x, y + 11 + k, z, s(Blocks.BLUE_TERRACOTTA));
				}
				c.set(x, y + 11 + rise, z, ax == 0 ? Kit.slab(Blocks.DARK_PRISMARINE_SLAB, false) : ax == 5 ? Kit.stairs(Blocks.DARK_PRISMARINE_STAIRS, x < 0 ? Direction.EAST : Direction.WEST, false)
						: s(Blocks.BLUE_CONCRETE));
			}
		}
		for (int z = GATE_Z - 3; z <= GATE_Z + 9; z += 3) {
			c.set(0, y + 17, z, s(Blocks.LIGHTNING_ROD));
		}
		// Twin towers.
		guardTower(c, -7, GATE_Z + 4, 3.5, y + 1, 12, 4, Direction.EAST, false);
		guardTower(c, 7, GATE_Z + 4, 3.5, y + 1, 12, 4, Direction.WEST, false);
		for (int sx : new int[]{-3, 3}) {
			c.fill(sx, y + 1, GATE_Z + 4, sx, y + 2, GATE_Z + 4, s(Blocks.AIR)); // the towers' doors open onto the passage
		}
		// The arrival platform and steps up to the gate, then the pier.
		for (int x = -6; x <= 6; x++) {
			for (int z = GATE_Z + 9; z <= GATE_Z + 14; z++) {
				int bed = Math.min(c.ground(x, z), SEA - 4);
				int top = z == GATE_Z + 9 ? y : z == GATE_Z + 10 ? y - 1 : 64;
				c.fill(x, bed, z, x, top - 1, z, s(Blocks.STONE_BRICKS));
				c.set(x, top, z, z <= GATE_Z + 10 && Math.abs(x) <= 2 ? Kit.stairs(Blocks.STONE_BRICK_STAIRS, Direction.NORTH, false)
						: Math.abs(x) == 6 || z == GATE_Z + 14 ? s(Blocks.STONE_BRICKS) : s(Blocks.SMOOTH_STONE));
				c.fill(x, top + 1, z, x, top + 4, z, s(Blocks.AIR));
			}
		}
		for (int z = GATE_Z + 15; z <= 74; z++) {
			for (int x = -1; x <= 1; x++) {
				c.set(x, 64, z, x == 0 ? s(Blocks.STRIPPED_SPRUCE_LOG).setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z) : s(Blocks.SPRUCE_PLANKS));
				c.fill(x, 65, z, x, 67, z, s(Blocks.AIR));
			}
			for (int x : new int[]{-2, 2}) {
				if (z % 4 == 0 || z == 74) {
					c.fill(x, Math.min(c.ground(x, z), SEA - 4), z, x, 64, z, s(Blocks.DARK_OAK_PLANKS));
					c.set(x, 65, z, s(Blocks.DARK_OAK_FENCE));
					if (z % 8 == 0) {
						c.set(x, 66, z, lantern(false));
					}
				} else {
					c.set(x, 64, z, s(Blocks.SPRUCE_PLANKS));
					c.set(x, 65, z, Kit.connected(Blocks.DARK_OAK_FENCE, false));
				}
			}
		}
		// Accessibility: a boat landing one block above the water, with stairs up to the deck (see Kit.boatLanding).
		Kit.boatLanding(c, 0, 74, Direction.SOUTH, 1, 2, 64, Blocks.SPRUCE_STAIRS, s(Blocks.SPRUCE_PLANKS),
				s(Blocks.SPRUCE_LOG), Blocks.DARK_OAK_FENCE);
		// Inside: the moat (open sea water) and the wooden bridge over it, the barred checkpoint at its inner end.
		for (int x = -6; x <= 6; x++) {
			for (int z = 33; z <= GATE_Z - 3; z++) {
				if (!c.inChunk(x, z)) {
					continue;
				}
				int bed = Math.min(c.ground(x, z), SEA - 4);
				boolean edge = Math.abs(x) == 6;
				c.fill(x, bed, z, x, bed + 1, z, s(Blocks.STONE_BRICKS));
				c.fill(x, bed + 2, z, x, SEA, z, edge ? s(Blocks.STONE_BRICKS) : s(Blocks.WATER));
				c.fill(x, SEA + 1, z, x, G, z, edge ? s(Blocks.STONE_BRICKS) : s(Blocks.AIR));
				if (Math.abs(x) <= 2) {
					c.set(x, G, z, Math.abs(x) == 2 ? s(Blocks.DARK_OAK_PLANKS) : s(Blocks.SPRUCE_PLANKS));
					c.set(x, G - 1, z, Math.abs(x) == 2 ? s(Blocks.DARK_OAK_PLANKS) : s(Blocks.AIR));
					if (Math.abs(x) == 2) {
						c.set(x, G + 1, z, z % 2 == 0 ? s(Blocks.DARK_OAK_FENCE) : Kit.connected(Blocks.IRON_BARS, false));
					}
				}
				if (edge) {
					c.set(x, G + 1, z, Kit.connected(Blocks.IRON_BARS, false));
				}
			}
		}
		for (int x = -2; x <= 2; x++) {
			c.set(x, G, 32, s(Blocks.POLISHED_ANDESITE));
			c.set(x, G, GATE_Z - 2, s(Blocks.POLISHED_ANDESITE));
		}
		for (int x = 3; x <= 7; x++) {
			for (int sx : new int[]{-1, 1}) {
				c.set(sx * x, G + 1, 32, Kit.connected(Blocks.IRON_BARS, true));
				c.set(sx * x, G + 2, 32, Kit.connected(Blocks.IRON_BARS, true));
			}
		}
		for (int sx : new int[]{-1, 1}) {
			c.fill(sx * 3, G + 1, 31, sx * 3, G + 4, 31, s(Blocks.POLISHED_ANDESITE));
			banner(c, sx * 3, G + 3, 30, Direction.NORTH, Blocks.BLACK_WALL_BANNER);
		}
		for (int x = -3; x <= 3; x++) {
			c.set(x, G + 5, 31, Math.abs(x) == 3 ? s(Blocks.POLISHED_ANDESITE) : Kit.connected(Blocks.IRON_BARS, true));
		}
		for (int sx : new int[]{-4, 4}) {
			banner(c, sx, G + 6, GATE_Z - 3, Direction.NORTH, Blocks.BLACK_WALL_BANNER);
		}
	}

	/** Height of the pointed gate arch above the floor at x (centre 7). */
	static int archTop(int x) {
		int ax = Math.abs(x);
		return ax == 0 ? 7 : ax == 1 ? 7 : 6;
	}

	// ---------------------------------------------------------------------------------------------------- courtyards

	/**
	 * The outer courtyard (a paved ring by the wall, square tiles) and the grass sectors between it and the inner
	 * enclosure (low walls, small angular trees, coarse dirt and tuff), with banners, fences, lanterns, barrels, debris
	 * and small cages.
	 */
	private void courtyards(Canvas c) {
		for (int x = -44; x <= 44; x++) {
			for (int z = -44; z <= 44; z++) {
				if (!c.inChunk(x, z)) {
					continue;
				}
				double d = Math.hypot(x, z), a = angle(x, z);
				if (d <= R_ENC_OUT || d > rIn(a)) {
					continue;
				}
				if (z > 32 && Math.abs(x) <= 6 && z < GATE_Z - 2) {
					continue; // the moat
				}
				boolean path = angDiff(a, 0) * Math.PI / 180 * d <= 2 || angDiff(a, 90) * Math.PI / 180 * d <= 2.5
						|| angDiff(a, 180) * Math.PI / 180 * d <= 2 || angDiff(a, 270) * Math.PI / 180 * d <= 2;
				if (d > 34.5 || path) {
					boolean dark = (Math.floorMod(x, 4) < 2) == (Math.floorMod(z, 4) < 2);
					c.set(x, G, z, dark ? s(Blocks.POLISHED_ANDESITE) : s(Blocks.SMOOTH_STONE));
				} else {
					double u = unit(x / 2, z / 2, 5);
					c.set(x, G, z, u < 0.08 ? s(Blocks.COARSE_DIRT) : u < 0.12 ? s(Blocks.TUFF) : s(Blocks.GRASS_BLOCK));
					c.set(x, G - 1, z, s(Blocks.DIRT));
					// Low curved walls round the sectors (stone bricks, slab cap), open at each sector's middle.
					if (d > 33.6 && d <= 34.5) {
						double mid = ((a % 90) + 90) % 90;
						if (Math.abs(mid - 45) * Math.PI / 180 * d > 2) {
							c.set(x, G + 1, z, s(Blocks.STONE_BRICKS));
							c.set(x, G + 2, z, Kit.slab(Blocks.STONE_BRICK_SLAB, false));
						}
					}
				}
			}
		}
		// Small angular trees in clusters in the grass sectors.
		for (int[] t : new int[][]{{-22, -22}, {-25, -17}, {22, -22}, {18, -26}, {-22, 22}, {-26, 17}, {24, 20}, {19, 25}}) {
			tree(c, t[0], t[1]);
		}
		// Props along the outer courtyard.
		for (int deg = 10; deg < 360; deg += 40) {
			if (angDiff(deg, 90) < 16) {
				continue;
			}
			double t = Math.toRadians(deg);
			int x = (int) Math.round(Math.cos(t) * 38), z = (int) Math.round(Math.sin(t) * 38);
			if (!c.near(x, z, 3)) {
				continue;
			}
			int kind = (deg / 40) % 4;
			switch (kind) {
				case 0 -> {
					c.set(x, G + 1, z, s(Blocks.POLISHED_BLACKSTONE_WALL));
					c.set(x, G + 2, z, s(Blocks.POLISHED_BLACKSTONE_WALL));
					c.set(x, G + 3, z, lantern(false));
				}
				case 1 -> {
					c.set(x, G + 1, z, Canvas.facing(s(Blocks.BARREL), Direction.UP));
					c.set(x + 1, G + 1, z, Canvas.facing(s(Blocks.BARREL), Direction.UP));
					c.set(x, G + 2, z, Canvas.facing(s(Blocks.BARREL), Direction.UP));
					c.set(x - 1, G + 1, z + 1, Kit.slab(Blocks.SPRUCE_SLAB, false));
				}
				case 2 -> cage(c, x, G + 1, z, false);
				default -> {
					c.set(x, G + 1, z, Kit.slab(Blocks.STONE_BRICK_SLAB, false));
					c.set(x + 1, G + 1, z - 1, s(Blocks.COBBLESTONE));
					c.set(x - 1, G + 1, z, Kit.connected(Blocks.IRON_BARS, true));
					c.set(x, G + 1, z + 1, Kit.connected(Blocks.IRON_BARS, true));
				}
			}
		}
	}

	/** A short angular tree: dark oak trunk, a blocky crown of green concrete with green terracotta shading. */
	private static void tree(Canvas c, int x, int z) {
		if (!c.near(x, z, 4)) {
			return;
		}
		int h = 3 + (int) (unit(x, z, 3) * 2);
		c.fill(x, G + 1, z, x, G + h, z, s(Blocks.DARK_OAK_LOG));
		for (int dy = 0; dy <= 2; dy++) {
			int r = dy == 2 ? 1 : 2;
			for (int dx = -r; dx <= r; dx++) {
				for (int dz = -r; dz <= r; dz++) {
					if (dy == 0 && Math.abs(dx) == 2 && Math.abs(dz) == 2) {
						continue;
					}
					if ((dx != 0 || dz != 0) || dy > 0) {
						c.set(x + dx, G + h + dy, z + dz, dy == 0 || unit(x + dx, z + dz, dy) < 0.3 ? s(Blocks.GREEN_TERRACOTTA) : s(Blocks.GREEN_CONCRETE));
					}
				}
			}
		}
	}

	/** A small iron cage (2 x 2 x 2 of bars with a slab roof), standing or hanging (with a chain above). */
	private static void cage(Canvas c, int x, int y, int z, boolean hanging) {
		for (int dx = 0; dx <= 1; dx++) {
			for (int dz = 0; dz <= 1; dz++) {
				for (int k = 0; k <= 1; k++) {
					c.set(x + dx, y + k, z + dz, s(Blocks.IRON_BARS).setValue(net.minecraft.world.level.block.IronBarsBlock.EAST, dx == 0)
							.setValue(net.minecraft.world.level.block.IronBarsBlock.WEST, dx == 1).setValue(net.minecraft.world.level.block.IronBarsBlock.SOUTH, dz == 0)
							.setValue(net.minecraft.world.level.block.IronBarsBlock.NORTH, dz == 1));
				}
				c.set(x + dx, y + 2, z + dz, Kit.slab(Blocks.POLISHED_BLACKSTONE_SLAB, false));
				if (hanging) {
					c.set(x + dx, y - 1, z + dz, Kit.slab(Blocks.POLISHED_BLACKSTONE_SLAB, true));
				}
			}
		}
		if (hanging) {
			c.set(x, y + 3, z, s(Blocks.CHAIN));
		}
	}

	// ---------------------------------------------------------------------------------------------------- inner enclosure

	/**
	 * The inner enclosure: a thick ring (r 21-26) with a curved corridor inside (barred windows, lanterns, ladders up to
	 * the broad top ledge, hanging cages, barrels, debris, chains strung overhead), four arched gates, a broad ledge on
	 * top with battlements, long black banners outside.
	 */
	private void enclosure(Canvas c) {
		if (!c.near(0, 0, 28)) {
			return;
		}
		for (int x = -27; x <= 27; x++) {
			for (int z = -27; z <= 27; z++) {
				if (!c.inChunk(x, z)) {
					continue;
				}
				double d = Math.hypot(x, z), a = angle(x, z);
				if (d <= R_ENC_IN - 0.5 || d > R_ENC_OUT) {
					continue;
				}
				boolean innerWall = d <= R_ENC_IN + 0.6, outerWall = d > R_ENC_OUT - 1.2;
				boolean gate = false;
				for (int g = 0; g < 360; g += 90) {
					gate |= angDiff(a, g) * Math.PI / 180 * d <= 2.2;
				}
				c.set(x, G, z, (Math.floorMod(x, 2) == Math.floorMod(z, 2)) ? s(Blocks.SMOOTH_STONE) : s(Blocks.POLISHED_ANDESITE));
				for (int y = G + 1; y < ENC_TOP; y++) {
					int k = y - G;
					BlockState b;
					if (innerWall || outerWall) {
						boolean window = outerWall && !gate && k >= 2 && k <= 3 && Math.floorMod((int) Math.round(Math.toRadians(a) * d), 7) == 0;
						b = window ? Kit.connected(Blocks.IRON_BARS, Math.abs(Math.sin(Math.toRadians(a))) > 0.7) : k == 7 ? s(Blocks.CALCITE) : masonry(x, y, z);
					} else {
						b = k <= 4 ? s(Blocks.AIR) : s(Blocks.STONE_BRICKS);
					}
					if (gate && k <= (angDiffWidth(a, d) <= 1.2 ? 5 : 4)) {
						b = s(Blocks.AIR);
					}
					c.set(x, y, z, b);
				}
				c.set(x, ENC_TOP, z, s(Blocks.SMOOTH_STONE));
				boolean rimOut = d > R_ENC_OUT - 0.8, rimIn = d <= R_ENC_IN + 0.3;
				if ((rimOut || rimIn) && Math.floorMod((int) Math.round(Math.toRadians(a) * d), 3) != 0) {
					c.set(x, ENC_TOP + 1, z, s(Blocks.STONE_BRICK_WALL));
				}
			}
		}
		// Corridor details: lanterns, cages, barrels, debris, chains overhead; ladders up to the ledge.
		for (int deg = 15; deg < 360; deg += 30) {
			double t = Math.toRadians(deg);
			int x = (int) Math.round(Math.cos(t) * 23.5), z = (int) Math.round(Math.sin(t) * 23.5);
			if (!c.near(x, z, 2)) {
				continue;
			}
			c.set(x, G + 4, z, lantern(true));
			int kind = (deg / 30) % 4;
			Direction tan = tangent(deg);
			int px = x + tan.getStepX() * 2, pz = z + tan.getStepZ() * 2;
			switch (kind) {
				case 0 -> c.set(px, G + 1, pz, Canvas.facing(s(Blocks.BARREL), Direction.UP));
				case 1 -> {
					c.set(px, G + 3, pz, s(Blocks.CHAIN));
					c.set(px, G + 2, pz, Kit.connected(Blocks.IRON_BARS, tan.getAxis() == Direction.Axis.X));
				}
				case 2 -> {
					c.set(px, G + 1, pz, Kit.slab(Blocks.STONE_BRICK_SLAB, false));
					c.set(px + 1, G + 1, pz, s(Blocks.COBBLESTONE));
				}
				default -> {
					// A ladder against the inner wall up through the ceiling to the ledge.
					double rr = R_ENC_IN + 1.5;
					int lx = (int) Math.round(Math.cos(t) * rr), lz = (int) Math.round(Math.sin(t) * rr);
					Direction face = outward(Math.cos(t), Math.sin(t));
					int bx = lx - face.getStepX(), bz = lz - face.getStepZ();
					if (Math.hypot(bx, bz) <= R_ENC_IN + 0.6 && Math.hypot(lx, lz) > R_ENC_IN + 0.6 && Math.hypot(lx, lz) <= R_ENC_OUT - 1.2) {
						for (int y = G + 1; y <= ENC_TOP; y++) {
							c.set(lx, y, lz, Canvas.facing(s(Blocks.LADDER), face));
						}
						c.set(bx, ENC_TOP, bz, s(Blocks.SMOOTH_STONE));
					}
				}
			}
			// Chains strung under the ceiling, across the corridor.
			Direction across = outward(Math.cos(t), Math.sin(t));
			c.set(x, G + 4, z, lantern(true));
			int cx2 = x + tan.getStepX() * -2, cz2 = z + tan.getStepZ() * -2;
			c.set(cx2, G + 4, cz2, s(Blocks.CHAIN).setValue(ChainBlock.AXIS, across.getAxis()));
		}
		// Long black banners on the outside, between the gates.
		for (int deg = 45; deg < 360; deg += 90) {
			for (int off : new int[]{-12, 12}) {
				double t = Math.toRadians(deg + off);
				int x = (int) Math.round(Math.cos(t) * (R_ENC_OUT + 0.6)), z = (int) Math.round(Math.sin(t) * (R_ENC_OUT + 0.6));
				Direction out = outward(Math.cos(t), Math.sin(t));
				int wx = x - out.getStepX(), wz = z - out.getStepZ();
				if (Math.hypot(wx, wz) <= R_ENC_OUT && Math.hypot(wx, wz) > R_ENC_OUT - 1.2 && Math.hypot(x, z) > R_ENC_OUT) {
					banner(c, x, G + 6, z, out, Blocks.BLACK_WALL_BANNER);
					banner(c, x, G + 4, z, out, Blocks.BLACK_WALL_BANNER);
				}
			}
		}
	}

	static double angDiffWidth(double a, double d) {
		double best = 99;
		for (int g = 0; g < 360; g += 90) {
			best = Math.min(best, angDiff(a, g) * Math.PI / 180 * d);
		}
		return best;
	}

	// ---------------------------------------------------------------------------------------------------- central courtyard

	/**
	 * The central courtyard: grass with dark paths radiating from the tower's four entrances to the enclosure gates,
	 * rocky patches, barrels, debris, lanterns, a ladder up the enclosure's inner face; the four wooden bridges from the
	 * enclosure's ledge to the tower's third level, two on dark triangular supports.
	 */
	private void centralCourtyard(Canvas c) {
		if (!c.near(0, 0, 22)) {
			return;
		}
		for (int x = -21; x <= 21; x++) {
			for (int z = -21; z <= 21; z++) {
				if (!c.inChunk(x, z)) {
					continue;
				}
				double d = Math.hypot(x, z), a = angle(x, z);
				if (d <= R_TOWER + 0.4 || d > R_ENC_IN - 0.5) {
					continue;
				}
				boolean path = angDiffWidth(a, d) <= 1.6;
				double u = unit(x, z, 9);
				BlockState b;
				if (path) {
					b = u < 0.35 ? s(Blocks.TUFF) : u < 0.65 ? s(Blocks.POLISHED_ANDESITE) : u < 0.85 ? s(Blocks.STONE) : s(Blocks.COARSE_DIRT);
				} else {
					double patch = unit(x / 3, z / 3, 10);
					b = patch < 0.12 ? s(Blocks.TUFF) : patch < 0.2 ? s(Blocks.COARSE_DIRT) : s(Blocks.GRASS_BLOCK);
				}
				c.set(x, G, z, b);
			}
		}
		for (int[] p : new int[][]{{14, 6}, {-15, -6}, {7, -15}}) {
			if (c.near(p[0], p[1], 2)) {
				c.set(p[0], G + 1, p[1], Canvas.facing(s(Blocks.BARREL), Direction.UP));
				c.set(p[0] + 1, G + 1, p[1], Canvas.facing(s(Blocks.BARREL), Direction.UP));
			}
		}
		for (int[] p : new int[][]{{-7, 16}, {16, -8}}) {
			if (c.near(p[0], p[1], 2)) {
				c.set(p[0], G + 1, p[1], s(Blocks.COBBLESTONE));
				c.set(p[0] + 1, G + 1, p[1], Kit.slab(Blocks.STONE_BRICK_SLAB, false));
			}
		}
		for (int deg = 22; deg < 360; deg += 90) {
			double t = Math.toRadians(deg);
			int x = (int) Math.round(Math.cos(t) * 17), z = (int) Math.round(Math.sin(t) * 17);
			if (c.near(x, z, 2)) {
				c.set(x, G + 1, z, s(Blocks.POLISHED_BLACKSTONE_WALL));
				c.set(x, G + 2, z, lantern(false));
			}
		}
		// The ladder up the enclosure's inner face (north-east).
		double t = Math.toRadians(300);
		int lx = (int) Math.round(Math.cos(t) * (R_ENC_IN - 1)), lz = (int) Math.round(Math.sin(t) * (R_ENC_IN - 1));
		Direction face = outward(-Math.cos(t), -Math.sin(t));
		if (Math.hypot(lx - face.getStepX(), lz - face.getStepZ()) > R_ENC_IN - 0.5) {
			for (int y = G + 1; y <= ENC_TOP; y++) {
				c.set(lx, y, lz, Canvas.facing(s(Blocks.LADDER), face));
			}
		}
		// Radial bridges (diagonals) from the ledge (y 75) to the tower's third level.
		for (int deg = 45; deg < 360; deg += 90) {
			double tt = Math.toRadians(deg), ux = Math.cos(tt), uz = Math.sin(tt);
			for (double r = R_TOWER - 0.5; r <= R_ENC_IN + 0.5; r += 0.5) {
				for (double k = -0.9; k <= 0.9; k += 0.45) {
					int x = (int) Math.round(ux * r - uz * k), z = (int) Math.round(uz * r + ux * k);
					if (!c.inChunk(x, z)) {
						continue;
					}
					c.set(x, LEVELS[2], z, Math.abs(k) > 0.6 ? s(Blocks.DARK_OAK_PLANKS) : s(Blocks.SPRUCE_PLANKS));
					c.fill(x, LEVELS[2] + 1, z, x, LEVELS[2] + 3, z, s(Blocks.AIR));
				}
			}
			if (deg == 45 || deg == 225) {
				// The dark triangular support under the middle.
				for (int i = 0; i <= 4; i++) {
					double r = 14 + i;
					int x = (int) Math.round(ux * r), z = (int) Math.round(uz * r);
					int drop = Math.min(i, 4 - i);
					c.fill(x, LEVELS[2] - 1 - drop, z, x, LEVELS[2] - 1, z, s(Blocks.DARK_OAK_PLANKS));
				}
				int x = (int) Math.round(ux * 16), z = (int) Math.round(uz * 16);
				c.fill(x, G + 1, z, x, LEVELS[2] - 4, z, s(Blocks.POLISHED_BLACKSTONE_WALL));
			}
			// Fence railings along the bridge edges.
			for (double r = R_TOWER + 1; r <= R_ENC_IN - 1; r += 1) {
				for (double k : new double[]{-1.3, 1.3}) {
					int x = (int) Math.round(ux * r - uz * k), z = (int) Math.round(uz * r + ux * k);
					if (c.inChunk(x, z)) {
						c.set(x, LEVELS[2], z, s(Blocks.DARK_OAK_PLANKS));
						c.set(x, LEVELS[2] + 1, z, s(Blocks.DARK_OAK_FENCE));
					}
				}
			}
		}
	}

	// ---------------------------------------------------------------------------------------------------- central tower

	/**
	 * The central tower's shell: the lower section (r 12: stone bricks with andesite pilasters, a white band, four large
	 * arched entrances, barred cell windows, wall lanterns, a projecting ledge with battlements), the middle (r 10: small
	 * arched windows), the upper (r 8: end stone bricks, tall arched blue windows), the flat round roof with battlements,
	 * the dark blue ring under its edge, white lights.
	 */
	private void tower(Canvas c) {
		if (!c.near(0, 0, 15)) {
			return;
		}
		for (int x = -14; x <= 14; x++) {
			for (int z = -14; z <= 14; z++) {
				if (!c.inChunk(x, z)) {
					continue;
				}
				double d = Math.hypot(x, z), a = angle(x, z);
				// Lower section.
				if (d <= R_TOWER + 0.4) {
					boolean shell = d > R_TOWER - 1.1;
					if (shell) {
						for (int y = G; y < MID_FLOOR; y++) {
							int k = y - G;
							boolean pilaster = Math.floorMod((int) Math.round(a), 30) <= 2;
							BlockState b = k == 4 ? s(Blocks.CALCITE) : pilaster ? s(Blocks.POLISHED_ANDESITE) : masonry(x, y, z);
							// Barred windows into the cells (centred on each cell, levels 1-3).
							double cellMid = ((a - 15) % 30 + 30) % 30;
							for (int L = 1; L < LEVELS.length; L++) {
								if (y == LEVELS[L] + 2 && Math.abs(cellMid - 15) < 3 && Math.abs(cellMid - 15) * Math.PI / 180 * d < 0.7) {
									b = Kit.connected(Blocks.IRON_BARS, Math.abs(Math.sin(Math.toRadians(a))) > 0.7);
								}
							}
							c.set(x, y, z, b);
						}
					}
					c.set(x, MID_FLOOR, z, shell ? s(Blocks.STONE_BRICKS) : s(Blocks.SMOOTH_STONE));
				}
				// The projecting ledge with battlements.
				if (d > R_TOWER + 0.4 && d <= R_TOWER + 1.6) {
					c.set(x, MID_FLOOR, z, Kit.slab(Blocks.STONE_BRICK_SLAB, true));
					c.set(x, MID_FLOOR - 1, z, Kit.stairs(Blocks.STONE_BRICK_STAIRS, outward(-x, -z), true));
				}
				if (d > R_TOWER - 0.6 && d <= R_TOWER + 1.6 && Math.floorMod((int) Math.round(Math.toRadians(a) * d), 3) != 0) {
					c.set(x, MID_FLOOR + 1, z, s(Blocks.STONE_BRICK_WALL));
				}
				// Middle section.
				if (d <= 10.4) {
					boolean shell = d > 9.3;
					for (int y = MID_FLOOR + 1; y < UP_FLOOR; y++) {
						int k = y - MID_FLOOR;
						boolean window = Math.floorMod((int) Math.round(a), 45) <= 3 && k >= 3 && k <= 4;
						BlockState b = !shell ? s(Blocks.AIR) : window ? Kit.connected(Blocks.IRON_BARS, Math.abs(Math.sin(Math.toRadians(a))) > 0.7)
								: k == 5 && Math.floorMod((int) Math.round(a), 45) <= 3 ? Kit.stairs(Blocks.STONE_BRICK_STAIRS, outward(-x, -z), true)
								: Math.floorMod((int) Math.round(a), 45) >= 20 && Math.floorMod((int) Math.round(a), 45) <= 24 ? s(Blocks.POLISHED_ANDESITE) : masonry(x, y, z);
						if (shell || y == MID_FLOOR + 1) {
							c.set(x, y, z, b);
						}
					}
					c.set(x, UP_FLOOR, z, shell ? s(Blocks.STONE_BRICKS) : s(Blocks.SPRUCE_PLANKS));
				}
				if (d > 10.4 && d <= 11.4) {
					c.set(x, UP_FLOOR, z, Kit.slab(Blocks.STONE_BRICK_SLAB, false));
				}
				// Upper section.
				if (d <= 8.4) {
					boolean shell = d > 7.3;
					for (int y = UP_FLOOR + 1; y < ROOF; y++) {
						int k = y - UP_FLOOR;
						double m = ((a % 60) + 60) % 60;
						boolean win = Math.abs(m - 30) < 4.5;
						boolean frame = Math.abs(m - 30) < 9 && !win;
						BlockState b;
						if (!shell) {
							b = s(Blocks.AIR);
						} else if (win && k >= 1 && k <= 5) {
							b = s(Blocks.LIGHT_BLUE_STAINED_GLASS); // full blocks: panes leave gaps on a curved wall
						} else if (frame && k <= 5) {
							b = s(Blocks.POLISHED_ANDESITE);
						} else if (Math.floorMod((int) Math.round(a), 60) == 0 && k == 3) {
							b = s(Blocks.SEA_LANTERN);
						} else {
							b = s(Blocks.END_STONE_BRICKS);
						}
						c.set(x, y, z, b);
					}
					c.set(x, ROOF, z, s(Blocks.SMOOTH_STONE));
					if (d > 7.4 && Math.floorMod((int) Math.round(Math.toRadians(a) * d), 3) != 0) {
						c.set(x, ROOF + 1, z, s(Blocks.STONE_BRICK_WALL));
					}
				}
				// The dark blue ring under the roof's edge, with white lights.
				if (d > 8.4 && d <= 9.5) {
					boolean light = Math.floorMod((int) Math.round(a), 45) <= 2;
					c.set(x, ROOF, z, light ? s(Blocks.SEA_LANTERN) : Kit.slab(Blocks.DARK_PRISMARINE_SLAB, true));
				}
			}
		}
		for (int deg = 0; deg < 360; deg += 90) {
			double t = Math.toRadians(deg + 45);
			int x = (int) Math.round(Math.cos(t) * 7.8), z = (int) Math.round(Math.sin(t) * 7.8);
			c.set(x, ROOF + 2, z, s(Blocks.END_ROD));
		}
		// Four large arched entrances at the bottom (N, E, S, W) and wall lanterns between them.
		for (int deg = 0; deg < 360; deg += 90) {
			double t = Math.toRadians(deg), ux = Math.cos(t), uz = Math.sin(t);
			for (double r = R_TOWER - 1.5; r <= R_TOWER + 0.6; r += 0.5) {
				for (int k = -1; k <= 1; k++) {
					int x = (int) Math.round(ux * r - uz * k), z = (int) Math.round(uz * r + ux * k);
					c.fill(x, G + 1, z, x, G + (k == 0 ? 5 : 4), z, s(Blocks.AIR));
					c.set(x, G, z, s(Blocks.POLISHED_ANDESITE));
				}
			}
			double tl = Math.toRadians(deg + 45);
			int lx = (int) Math.round(Math.cos(tl) * (R_TOWER + 1)), lz = (int) Math.round(Math.sin(tl) * (R_TOWER + 1));
			Direction out = outward(Math.cos(tl), Math.sin(tl));
			if (Math.hypot(lx - out.getStepX(), lz - out.getStepZ()) <= R_TOWER + 0.4) {
				c.set(lx, G + 4, lz, s(Blocks.POLISHED_ANDESITE));
				c.set(lx, G + 3, lz, lantern(true));
			}
		}
	}

	// ---------------------------------------------------------------------------------------------------- atrium

	/**
	 * The prison atrium inside the lower tower: open from the floor to the ceiling, cells round the wall on four levels
	 * (arched barred fronts, small chambers), curved galleries with low parapets, curved spruce staircases between the
	 * levels, the doorways to the four radial bridges, the stone bridge across at the top level, the great spiked beam
	 * above, and the spiral staircase down to the chamber below.
	 */
	private void atrium(Canvas c) {
		if (!c.near(0, 0, 13)) {
			return;
		}
		// Clear the inside, lay the floor.
		for (int x = -11; x <= 11; x++) {
			for (int z = -11; z <= 11; z++) {
				double d = Math.hypot(x, z);
				if (d > R_TOWER - 1.1 || !c.inChunk(x, z)) {
					continue;
				}
				c.fill(x, G + 1, z, x, MID_FLOOR - 1, z, s(Blocks.AIR));
				c.set(x, G, z, (Math.floorMod(x, 4) < 2) == (Math.floorMod(z, 4) < 2) ? s(Blocks.SMOOTH_STONE) : s(Blocks.POLISHED_ANDESITE));
			}
		}
		for (int L = 0; L < LEVELS.length; L++) {
			int fy = LEVELS[L];
			int ceil = L + 1 < LEVELS.length ? LEVELS[L + 1] : fy + 4;
			for (int x = -11; x <= 11; x++) {
				for (int z = -11; z <= 11; z++) {
					if (!c.inChunk(x, z)) {
						continue;
					}
					double d = Math.hypot(x, z), a = angle(x, z);
					if (d > R_TOWER - 1.1) {
						continue;
					}
					double cellMid = ((a % 30) + 30) % 30; // dividers at 0, the cell door at 15
					boolean bridgeDoor = L == 2 && Math.floorMod((int) Math.round(a) - 45, 90) <= 4 || L == 2 && Math.floorMod((int) Math.round(a) - 45, 90) >= 86;
					boolean entrance = L == 0 && angDiffWidth(a, d) <= 1.6;
					// Gallery floor (levels 1-3): r 6-8, a parapet on the inner edge.
					if (L > 0 && d > 6 && d <= 8.4) {
						c.set(x, fy, z, (x + z) % 2 == 0 ? s(Blocks.SMOOTH_STONE) : s(Blocks.POLISHED_ANDESITE));
						if (d <= 6.7) {
							c.set(x, fy + 1, z, s(Blocks.STONE_BRICK_WALL));
						}
					}
					// The cell ring: front wall (r 8-9) with arched barred doors, chambers (r 9-11), dividers.
					if (d > 8.4 && d <= R_TOWER - 1.1) {
						c.set(x, fy, z, s(Blocks.STONE_BRICKS));
						boolean front = d <= 9.4;
						boolean divider = cellMid < 3.5 || cellMid > 26.5;
						for (int y = fy + 1; y < ceil; y++) {
							int k = y - fy;
							BlockState b;
							if (entrance || bridgeDoor) {
								b = k <= 3 ? s(Blocks.AIR) : s(Blocks.STONE_BRICKS);
							} else if (front) {
								double off = Math.abs(cellMid - 15) * Math.PI / 180 * d;
								if (off <= 1.0 && k <= 2) {
									b = Kit.connected(Blocks.IRON_BARS, Math.abs(Math.sin(Math.toRadians(a))) > 0.7);
								} else if (off <= 1.0 && k == 3) {
									b = Kit.slab(Blocks.STONE_BRICK_SLAB, true);
								} else if (off <= 1.7 && k == 3) {
									b = Kit.stairs(Blocks.STONE_BRICK_STAIRS, tangent(a + (cellMid < 15 ? 0 : 180)), true);
								} else {
									b = off <= 2.2 ? s(Blocks.POLISHED_ANDESITE) : s(Blocks.STONE_BRICKS);
								}
							} else if (divider) {
								b = s(Blocks.STONE_BRICKS);
							} else {
								b = k == ceil - fy - 1 ? s(Blocks.STONE_BRICKS) : s(Blocks.AIR);
							}
							c.set(x, y, z, b);
						}
					}
				}
			}
			// A bench in each cell and a chain from its ceiling.
			for (int deg = 15; deg < 360; deg += 30) {
				double t = Math.toRadians(deg);
				int x = (int) Math.round(Math.cos(t) * 10.2), z = (int) Math.round(Math.sin(t) * 10.2);
				if (!c.inChunk(x, z) || (L == 0 && angDiffWidth(deg, 10) < 2) || L == 2 && Math.floorMod(deg - 45, 90) <= 4) {
					continue;
				}
				if (Math.hypot(x, z) > 9.4 && Math.hypot(x, z) <= R_TOWER - 1.1) {
					c.set(x, fy + 1, z, (deg / 30) % 2 == 0 ? Kit.slab(Blocks.SPRUCE_SLAB, false) : s(Blocks.CHAIN));
				}
			}
		}
		// Curved staircases between the levels (two wide, along the gallery ring, spruce steps), with holes above.
		int[][] runs = {{LEVELS[0], LEVELS[1], 100}, {LEVELS[1], LEVELS[2], 190}, {LEVELS[2], LEVELS[3], 280}, {LEVELS[3], MID_FLOOR, 10}};
		for (int[] run : runs) {
			curvedStair(c, run[0], run[1], run[2], 6.6, 7.6);
		}
		// The stone bridge across the top level (north-south) and the great spiked beam above (east-west).
		for (int z = -8; z <= 8; z++) {
			for (int x = -1; x <= 0; x++) {
				c.set(x, LEVELS[3], z, (z % 2 == 0) ? s(Blocks.SMOOTH_STONE) : s(Blocks.POLISHED_ANDESITE));
			}
			if (Math.abs(z) <= 6) {
				c.set(-2, LEVELS[3], z, Kit.slab(Blocks.STONE_BRICK_SLAB, true));
				c.set(1, LEVELS[3], z, Kit.slab(Blocks.STONE_BRICK_SLAB, true));
				c.set(-2, LEVELS[3] + 1, z, s(Blocks.STONE_BRICK_WALL));
				c.set(1, LEVELS[3] + 1, z, s(Blocks.STONE_BRICK_WALL));
			}
		}
		for (int x = -11; x <= 11; x++) {
			for (int z = 0; z <= 1; z++) {
				c.set(x, MID_FLOOR - 1, z, Math.floorMod(x, 4) == 0 ? s(Blocks.POLISHED_BLACKSTONE) : s(Blocks.STRIPPED_SPRUCE_LOG).setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
			}
			if (Math.floorMod(x, 3) == 1 && Math.abs(x) <= 9) {
				c.set(x, MID_FLOOR - 2, 0, s(Blocks.LIGHTNING_ROD).setValue(LightningRodBlock.FACING, Direction.DOWN));
				c.set(x, MID_FLOOR - 2, 1, s(Blocks.LIGHTNING_ROD).setValue(LightningRodBlock.FACING, Direction.DOWN));
			}
		}
		// Lanterns hanging from the gallery undersides.
		for (int deg = 0; deg < 360; deg += 45) {
			double t = Math.toRadians(deg + 22);
			int x = (int) Math.round(Math.cos(t) * 7), z = (int) Math.round(Math.sin(t) * 7);
			for (int L = 1; L < LEVELS.length; L++) {
				double run = new double[]{100, 190, 280}[L - 1]; // the stair below this gallery opens its floor
				if (angDiff(deg + 22, run + 24) < 32) {
					continue;
				}
				c.set(x, LEVELS[L] - 1, z, lantern(true));
			}
		}
		// The spiral staircase down to the chamber, round a central pillar, with a stone rail round the opening.
		spiral(c, G, CH_FLOOR, true);
		spiralRail(c, G + 1, CH_FLOOR, G, Blocks.POLISHED_ANDESITE);
	}

	/**
	 * A curved staircase from y1 up to y2 along the ring between radii r1 and r2, starting at bearing {@code start} and
	 * climbing one block per block of arc; the floor above is opened over the steps for headroom.
	 */
	private static void curvedStair(Canvas c, int y1, int y2, double start, double r1, double r2) {
		double rm = (r1 + r2) / 2;
		double step = Math.toDegrees(1.0 / rm);
		int rise = y2 - y1;
		for (int i = 1; i <= rise; i++) {
			double a = start + step * i;
			int y = y1 + i;
			for (double r = r1; r <= r2 + 0.01; r += 0.5) {
				double t = Math.toRadians(a);
				int x = (int) Math.round(Math.cos(t) * r), z = (int) Math.round(Math.sin(t) * r);
				if (!c.inChunk(x, z)) {
					continue;
				}
				c.fill(x, y1 + 1, z, x, y - 1, z, s(Blocks.STONE_BRICKS));
				c.set(x, y, z, i == rise ? s(Blocks.SPRUCE_PLANKS) : Kit.stairs(Blocks.SPRUCE_STAIRS, tangent(a), false));
				c.fill(x, y + 1, z, x, Math.min(y + 3, y2 + 3), z, s(Blocks.AIR));
			}
		}
		// Polished andesite trim along the outer edge of the run.
		for (int i = 1; i <= rise; i++) {
			double t = Math.toRadians(start + step * i);
			int x = (int) Math.round(Math.cos(t) * (r2 + 0.9)), z = (int) Math.round(Math.sin(t) * (r2 + 0.9));
			if (c.inChunk(x, z) && Math.hypot(x, z) <= 8.4) {
				c.set(x, y1 + i, z, s(Blocks.POLISHED_ANDESITE));
			}
		}
	}

	/** One cell of the square spiral: x, z, y, and the stair's facing (-1 for a flat landing). */
	record Step(int x, int z, int y, int facing) {
	}

	/**
	 * The square spiral staircase from {@code yBottom} (a floor) up to {@code yTop} (the floor above): a 7 x 7 shaft
	 * round a 3 x 3 pillar, a two-wide stair climbing three steps along each side with a flat 2 x 2 landing at each
	 * corner (12 up per turn, so there is always headroom). The last step is level with the upper floor.
	 */
	static java.util.List<Step> spiralSteps(int yBottom, int yTop) {
		java.util.List<Step> out = new java.util.ArrayList<>();
		// Sides in climbing order: east (going south), south (west), west (north), north (east); then the next corner.
		int[][] sideRows = {{-1, 0, 1}, {1, 0, -1}, {1, 0, -1}, {-1, 0, 1}};
		Direction[] travel = {Direction.SOUTH, Direction.WEST, Direction.NORTH, Direction.EAST};
		int y = yBottom;
		for (int side = 0; y < yTop; side = (side + 1) % 4) {
			for (int row : sideRows[side]) {
				y++;
				boolean last = y >= yTop;
				for (int across : new int[]{2, 3}) {
					int x, z;
					switch (side) {
						case 0 -> { x = across; z = row; }
						case 1 -> { x = row; z = across; }
						case 2 -> { x = -across; z = row; }
						default -> { x = row; z = -across; }
					}
					out.add(new Step(x, z, y, last ? -1 : travel[side].ordinal()));
				}
				if (last) {
					return out;
				}
			}
			// The corner landing after this side (level with its last step).
			int[] sx = side == 0 ? new int[]{2, 3} : side == 1 ? new int[]{-3, -2} : side == 2 ? new int[]{-3, -2} : new int[]{2, 3};
			int[] sz = side == 0 ? new int[]{2, 3} : side == 1 ? new int[]{2, 3} : side == 2 ? new int[]{-3, -2} : new int[]{-3, -2};
			for (int x : sx) {
				for (int z : sz) {
					out.add(new Step(x, z, y, -1));
				}
			}
		}
		return out;
	}

	/** The way off the top of the spiral: the floor cells just outside its last step (for leaving a gap in the rail). */
	static java.util.Set<Long> spiralExit(int yBottom, int yTop) {
		java.util.Set<Long> exit = new java.util.HashSet<>();
		for (Step st : spiralSteps(yBottom, yTop)) {
			if (st.y() == yTop) {
				for (Direction d : Direction.Plane.HORIZONTAL) {
					int x = st.x() + d.getStepX(), z = st.z() + d.getStepZ();
					if (Math.max(Math.abs(x), Math.abs(z)) == 4) {
						exit.add(((long) x << 32) ^ (z & 0xffffffffL));
					}
				}
			}
		}
		return exit;
	}

	/**
	 * Builds the square spiral from {@code yTop} down to {@code yBottom} at the centre: the shaft cleared (the upper
	 * floor opened), the pillar, the steps and landings in spruce on a stone brick underside.
	 */
	private static void spiral(Canvas c, int yTop, int yBottom, boolean pillar) {
		if (!c.near(0, 0, 5)) {
			return;
		}
		for (int x = -3; x <= 3; x++) {
			for (int z = -3; z <= 3; z++) {
				if (!c.inChunk(x, z)) {
					continue;
				}
				boolean core = Math.abs(x) <= 1 && Math.abs(z) <= 1;
				c.fill(x, yBottom + 1, z, x, yTop, z, core && pillar ? s(Blocks.STONE_BRICKS) : s(Blocks.AIR));
				if (core && pillar) {
					c.set(x, yTop, z, s(Blocks.POLISHED_ANDESITE));
				}
			}
		}
		for (Step st : spiralSteps(yBottom, yTop)) {
			if (!c.inChunk(st.x(), st.z())) {
				continue;
			}
			c.set(st.x(), st.y(), st.z(), st.facing() < 0 ? s(Blocks.SPRUCE_PLANKS)
					: Kit.stairs(Blocks.SPRUCE_STAIRS, Direction.values()[st.facing()], false));
			if (st.y() - 1 > yBottom) {
				c.set(st.x(), st.y() - 1, st.z(), s(Blocks.STONE_BRICKS));
			}
		}
	}

	/** Whether the shaft column (x, z) has to stay open at the top floor (a step below it needs the headroom). */
	static boolean spiralHole(java.util.List<Step> steps, int x, int z, int yTop) {
		for (Step st : steps) {
			if (st.x() == x && st.z() == z && st.y() >= yTop - 3 && st.y() < yTop) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Floors over the top of the spiral (only the stairwell over its last steps stays open), the last step level with
	 * the floor, and a low rail round the stairwell (not across the way off the stair).
	 */
	private static void spiralRail(Canvas c, int y, int yBottom, int yTop, Block border) {
		java.util.List<Step> steps = spiralSteps(yBottom, yTop);
		java.util.Set<Long> top = new java.util.HashSet<>();
		for (Step st : steps) {
			if (st.y() == yTop) {
				top.add(((long) st.x() << 32) ^ (st.z() & 0xffffffffL));
			}
		}
		for (int x = -4; x <= 4; x++) {
			for (int z = -4; z <= 4; z++) {
				if (!c.inChunk(x, z)) {
					continue;
				}
				boolean inShaft = Math.max(Math.abs(x), Math.abs(z)) <= 3;
				boolean hole = inShaft && spiralHole(steps, x, z, yTop);
				long key = ((long) x << 32) ^ (z & 0xffffffffL);
				if (inShaft && !hole && !top.contains(key)) {
					c.set(x, yTop, z, s(border));
				}
				if (hole || top.contains(key)) {
					continue;
				}
				boolean edge = false;
				for (Direction d : Direction.Plane.HORIZONTAL) {
					int nx = x + d.getStepX(), nz = z + d.getStepZ();
					edge |= Math.max(Math.abs(nx), Math.abs(nz)) <= 3 && spiralHole(steps, nx, nz, yTop);
				}
				if (edge) {
					c.set(x, y, z, s(Blocks.STONE_BRICK_WALL));
				}
			}
		}
	}


	// ---------------------------------------------------------------------------------------------------- upper rooms

	/**
	 * The middle floor (a guard room with barrels and a table round the stair opening) and the furnished upper room:
	 * spruce floor, end stone and andesite walls, tall blue windows with iron frames and white curtains, the golden
	 * chandelier, the fireplace with its chimney, bookshelves and a desk, the pale-bordered stair opening, a ladder up to
	 * the roof.
	 */
	private void upperRooms(Canvas c) {
		if (!c.near(0, 0, 11)) {
			return;
		}
		// Middle floor: a spiral up to the upper room in the middle, barrels and a table.
		spiral(c, UP_FLOOR, MID_FLOOR, true);
		c.set(6, MID_FLOOR + 1, -5, Canvas.facing(s(Blocks.BARREL), Direction.UP));
		c.set(7, MID_FLOOR + 1, -4, Canvas.facing(s(Blocks.BARREL), Direction.UP));
		// A table (two upper spruce slabs) with a chair on each long side, both facing it.
		c.set(-6, MID_FLOOR + 1, 4, Kit.slab(Blocks.SPRUCE_SLAB, true));
		c.set(-6, MID_FLOOR + 1, 5, Kit.slab(Blocks.SPRUCE_SLAB, true));
		c.set(-5, MID_FLOOR + 1, 4, Kit.stairs(Blocks.SPRUCE_STAIRS, Direction.EAST, false));
		c.set(-5, MID_FLOOR + 1, 5, Kit.stairs(Blocks.SPRUCE_STAIRS, Direction.EAST, false));
		c.set(0, UP_FLOOR - 1, 6, lantern(true));
		c.set(0, UP_FLOOR - 1, -6, lantern(true));
		// The upper room: the stair opening with a pale border and a low rail.
		spiralRail(c, UP_FLOOR + 1, MID_FLOOR, UP_FLOOR, Blocks.SMOOTH_QUARTZ);
		// Chandelier: chains from the ceiling, a gold hub with arms, end rods and sea lanterns.
		int cy = ROOF - 1;
		c.fill(0, cy - 1, 0, 0, cy, 0, s(Blocks.CHAIN));
		c.set(0, cy - 2, 0, s(Blocks.GOLD_BLOCK));
		for (Direction d : Direction.Plane.HORIZONTAL) {
			c.set(d.getStepX(), cy - 2, d.getStepZ(), s(Blocks.END_ROD).setValue(net.minecraft.world.level.block.EndRodBlock.FACING, d));
			c.set(d.getStepX() * 2, cy - 2, d.getStepZ() * 2, s(Blocks.GOLD_BLOCK));
			c.set(d.getStepX() * 2, cy - 1, d.getStepZ() * 2, s(Blocks.CHAIN));
			c.set(d.getStepX() * 2, cy, d.getStepZ() * 2, s(Blocks.CHAIN));
			c.set(d.getStepX() * 2, cy - 3, d.getStepZ() * 2, s(Blocks.SEA_LANTERN));
		}
		// Fireplace on the west wall, a chimney through the roof.
		int fx = -7;
		for (int z = -2; z <= 2; z++) {
			for (int k = 1; k <= 4; k++) {
				boolean mouth = Math.abs(z) <= 1 && k <= 2;
				c.set(fx, UP_FLOOR + k, z, mouth ? s(Blocks.AIR) : s(Blocks.STONE_BRICKS));
			}
			c.set(fx + 1, UP_FLOOR + 4, z, Kit.slab(Blocks.STONE_BRICK_SLAB, true));
		}
		c.set(fx, UP_FLOOR + 1, 0, s(Blocks.CAMPFIRE).setValue(CampfireBlock.FACING, Direction.EAST));
		c.set(fx + 1, UP_FLOOR + 1, -1, Kit.connected(Blocks.IRON_BARS, false));
		c.set(fx + 1, UP_FLOOR + 1, 1, Kit.connected(Blocks.IRON_BARS, false));
		c.fill(fx, UP_FLOOR + 5, 0, fx, ROOF + 4, 0, s(Blocks.STONE_BRICKS));
		c.set(fx, ROOF + 5, 0, Kit.slab(Blocks.STONE_BRICK_SLAB, false));
		// Bookshelves on the east wall and a desk.
		for (int z = -3; z <= 3; z++) {
			int bx = (int) Math.floor(Math.sqrt(Math.max(0, 7.2 * 7.2 - z * z)));
			c.set(bx, UP_FLOOR + 1, z, s(Blocks.BOOKSHELF));
			c.set(bx, UP_FLOOR + 2, z, s(Blocks.BOOKSHELF));
		}
		// The desk: a row of upper spruce slabs at table height, a lantern on it, a chair facing it.
		for (int x = 2; x <= 4; x++) {
			c.set(x, UP_FLOOR + 1, -5, Kit.slab(Blocks.SPRUCE_SLAB, true));
		}
		c.set(2, UP_FLOOR + 2, -5, lantern(false));
		c.set(3, UP_FLOOR + 1, -4, Kit.stairs(Blocks.SPRUCE_STAIRS, Direction.SOUTH, false));
		// White curtains beside the windows (banners on the wall, facing in).
		for (int k = 0; k < 6; k++) {
			for (int side : new int[]{-1, 1}) {
				double a = 30 + k * 60 + side * 11;
				double t = Math.toRadians(a);
				int x = (int) Math.round(Math.cos(t) * 6.8), z = (int) Math.round(Math.sin(t) * 6.8);
				Direction in = outward(-Math.cos(t), -Math.sin(t));
				int wx = x - in.getStepX(), wz = z - in.getStepZ();
				if (Math.hypot(wx, wz) > 7.3 && Math.hypot(x, z) <= 7.3 && Math.abs(x) < 6 && c.inChunk(x, z)) {
					banner(c, x, UP_FLOOR + 4, z, in, Blocks.WHITE_WALL_BANNER);
				}
			}
		}
		// Ladder from the room to the roof.
		for (int y = UP_FLOOR + 1; y <= ROOF; y++) {
			c.set(-3, y, 6, Canvas.facing(s(Blocks.LADDER), Direction.NORTH)); // against the wall between two windows
		}
	}

	// ---------------------------------------------------------------------------------------------------- chamber

	/**
	 * The secondary chamber, underground below the central courtyard: a tall curved wall, five narrow towers round it
	 * with steep blue pointed roofs, curved platforms on the walls (ladders up), chains between the towers, hollow iron
	 * cages hanging from the ceiling, a broad dark floor with a dark railing round part of it, scattered fragments.
	 */
	private void chamber(Canvas c) {
		if (!c.near(0, 0, 23)) {
			return;
		}
		for (int x = -22; x <= 22; x++) {
			for (int z = -22; z <= 22; z++) {
				if (!c.inChunk(x, z)) {
					continue;
				}
				double d = Math.hypot(x, z);
				if (d > CH_R + 1.5) {
					continue;
				}
				if (d <= CH_R) {
					c.set(x, CH_FLOOR, z, ((int) Math.round(d)) % 4 == 0 ? s(Blocks.POLISHED_ANDESITE) : s(Blocks.TUFF));
					c.fill(x, CH_FLOOR + 1, z, x, CH_CEIL - 1, z, s(Blocks.AIR));
					c.set(x, CH_CEIL, z, s(Blocks.STONE_BRICKS));
				} else {
					for (int y = CH_FLOOR; y <= CH_CEIL; y++) {
						c.set(x, y, z, (y - CH_FLOOR) % 6 == 0 ? s(Blocks.POLISHED_ANDESITE) : masonry(x, y, z));
					}
				}
			}
		}
		// The spiral staircase passes through the middle (the carving above cleared it): rebuild it and its top floor.
		spiral(c, G, CH_FLOOR, true);
		spiralRail(c, G + 1, CH_FLOOR, G, Blocks.POLISHED_ANDESITE);
		int[] towers = {30, 102, 174, 246, 318};
		int[][] tops = new int[towers.length][];
		for (int i = 0; i < towers.length; i++) {
			double t = Math.toRadians(towers[i]);
			int cx = (int) Math.round(Math.cos(t) * 17.5), cz = (int) Math.round(Math.sin(t) * 17.5);
			tops[i] = new int[]{cx, cz};
			for (int dx = -2; dx <= 2; dx++) {
				for (int dz = -2; dz <= 2; dz++) {
					double d = Math.hypot(dx, dz);
					if (d <= 1.6) {
						c.fill(cx + dx, CH_FLOOR + 1, cz + dz, cx + dx, CH_FLOOR + 10, cz + dz, d > 0.6 ? s(Blocks.END_STONE_BRICKS) : s(Blocks.STONE_BRICKS));
					}
					// Steep pointed roof.
					for (int k = 0; k <= 5; k++) {
						double rk = 2.4 - k * 0.45;
						if (d <= rk && d > rk - 1) {
							c.set(cx + dx, CH_FLOOR + 11 + k, cz + dz, k == 0 ? Kit.stairs(Blocks.DARK_PRISMARINE_STAIRS, outward(-dx, -dz), false) : s(Blocks.BLUE_CONCRETE));
						} else if (d <= rk - 1) {
							c.set(cx + dx, CH_FLOOR + 11 + k, cz + dz, s(Blocks.BLUE_CONCRETE));
						}
					}
				}
			}
			c.set(cx, CH_FLOOR + 17, cz, s(Blocks.LIGHTNING_ROD));
			c.set(cx + outward(-cx, -cz).getStepX() * 2, CH_FLOOR + 1, cz + outward(-cx, -cz).getStepZ() * 2, lantern(false));
		}
		// Chains strung between neighbouring towers (stepped lines).
		for (int i = 0; i < towers.length; i++) {
			int[] a = tops[i], b = tops[(i + 1) % towers.length];
			int n = Math.max(Math.abs(b[0] - a[0]), Math.abs(b[1] - a[1]));
			for (int k = 1; k < n; k++) {
				int x = (int) Math.round(a[0] + (b[0] - a[0]) * (k / (double) n)), z = (int) Math.round(a[1] + (b[1] - a[1]) * (k / (double) n));
				int sag = (int) Math.round(2 * Math.sin(Math.PI * k / n));
				c.set(x, CH_FLOOR + 10 - sag, z, s(Blocks.CHAIN).setValue(ChainBlock.AXIS, Math.abs(b[0] - a[0]) >= Math.abs(b[1] - a[1]) ? Direction.Axis.X : Direction.Axis.Z));
			}
		}
		// Curved platforms on the wall between the towers (r 17.5-20), stone wall rails, ladders up.
		for (int i = 0; i < towers.length; i++) {
			double from = towers[i] + 12, to = towers[i] + 60;
			for (double a = from; a <= to; a += 1.5) {
				for (double r = 18.2; r <= CH_R + 0.4; r += 0.5) {
					double t = Math.toRadians(a);
					int x = (int) Math.round(Math.cos(t) * r), z = (int) Math.round(Math.sin(t) * r);
					if (!c.inChunk(x, z)) {
						continue;
					}
					c.set(x, CH_FLOOR + 8, z, s(Blocks.SMOOTH_STONE));
					if (r < 18.7) {
						c.set(x, CH_FLOOR + 9, z, s(Blocks.STONE_BRICK_WALL));
					}
				}
			}
			int[] spot = wallSpot(from + 6, CH_R);
			if (spot != null) {
				Direction in = Direction.values()[spot[2]];
				for (int y = CH_FLOOR + 1; y <= CH_FLOOR + 9; y++) {
					c.set(spot[0], y, spot[1], Canvas.facing(s(Blocks.LADDER), in));
				}
			}
		}
		// Hanging cages.
		for (int deg : new int[]{66, 138, 210, 282}) {
			double t = Math.toRadians(deg);
			int x = (int) Math.round(Math.cos(t) * 10), z = (int) Math.round(Math.sin(t) * 10);
			c.fill(x, CH_FLOOR + 11, z, x, CH_CEIL - 1, z, s(Blocks.CHAIN));
			cage(c, x, CH_FLOOR + 8, z, true);
		}
		// The dark railing round the south half of the floor, fragments, lanterns.
		for (double a = 200; a <= 340; a += 1.5) {
			double t = Math.toRadians(a);
			int x = (int) Math.round(Math.cos(t) * 13), z = (int) Math.round(Math.sin(t) * 13);
			if (c.inChunk(x, z)) {
				c.set(x, CH_FLOOR + 1, z, s(Blocks.POLISHED_BLACKSTONE_WALL));
			}
		}
		for (int[] p : new int[][]{{8, 9}, {-11, 4}, {5, -14}, {-6, 12}}) {
			if (c.inChunk(p[0], p[1])) {
				c.set(p[0], CH_FLOOR + 1, p[1], p[0] % 2 == 0 ? s(Blocks.COBBLESTONE) : Kit.slab(Blocks.STONE_BRICK_SLAB, false));
			}
		}
		for (int deg = 0; deg < 360; deg += 60) {
			double t = Math.toRadians(deg + 30);
			int x = (int) Math.round(Math.cos(t) * 6), z = (int) Math.round(Math.sin(t) * 6);
			c.fill(x, CH_FLOOR + 14, z, x, CH_CEIL - 1, z, s(Blocks.CHAIN));
			c.set(x, CH_FLOOR + 13, z, lantern(true));
		}
		c.set(0, CH_FLOOR + 1, -16, s(Blocks.ANVIL));
	}

	/**
	 * A ladder spot against a round wall of radius {@code wallR} near a bearing: an open block inside the wall whose
	 * neighbour outward (along an axis) is in the wall. Returns {x, z, facing ordinal} or null.
	 */
	static int[] wallSpot(double deg, double wallR) {
		for (double da = 0; da <= 12; da += 1) {
			for (double sgn : new double[]{1, -1}) {
				double t = Math.toRadians(deg + da * sgn);
				int x = (int) Math.round(Math.cos(t) * (wallR - 0.6)), z = (int) Math.round(Math.sin(t) * (wallR - 0.6));
				if (Math.hypot(x, z) > wallR) {
					continue;
				}
				for (Direction out : Direction.Plane.HORIZONTAL) {
					if (out.getStepX() * Math.cos(t) + out.getStepZ() * Math.sin(t) < 0.5) {
						continue;
					}
					if (Math.hypot(x + out.getStepX(), z + out.getStepZ()) > wallR) {
						return new int[]{x, z, out.getOpposite().ordinal()};
					}
				}
			}
		}
		return null;
	}

	// ---------------------------------------------------------------------------------------------------- landing

	/**
	 * The waterfront landing on the west: a small stepped stone landing at sea level, a pale-stone doorway through the
	 * wall with steps up, a black flag, planks, barrels, debris and a short wooden ramp into the water.
	 */
	private void landing(Canvas c) {
		if (!c.near(-48, 0, 10)) {
			return;
		}
		for (int x = -52; x <= -44; x++) {
			for (int z = -5; z <= 5; z++) {
				double edge = Math.abs(z) + (x + 52) * 0.4 + unit(x, z, 21) * 1.5;
				if (edge > 7 || !c.inChunk(x, z)) {
					continue;
				}
				int bed = Math.min(c.ground(x, z), SEA - 4);
				int top = x >= -45 ? 64 : 63;
				c.fill(x, bed, z, x, top - 1, z, s(Blocks.STONE_BRICKS));
				c.set(x, top, z, Math.abs(z) == 0 && x >= -46 ? s(Blocks.POLISHED_ANDESITE) : Kit.slab(Blocks.SMOOTH_STONE_SLAB, true));
				c.fill(x, top + 1, z, x, top + 4, z, s(Blocks.AIR));
			}
		}
		// The doorway through the wall: steps up from the landing to the courtyard, a pale frame.
		double ri = rIn(180);
		int wallOut = (int) -Math.ceil(ri + 2.2), wallIn = (int) -Math.floor(ri);
		for (int x = wallOut - 1; x <= wallIn + 1; x++) {
			for (int z = -1; z <= 1; z++) {
				int floor = x <= wallOut - 1 ? 64 : x == wallOut ? 65 : G;
				c.set(x, floor, z, x <= wallOut ? Kit.stairs(Blocks.STONE_BRICK_STAIRS, Direction.EAST, false) : s(Blocks.SMOOTH_STONE));
				if (x <= wallOut - 1) {
					c.set(x, floor, z, s(Blocks.POLISHED_ANDESITE));
				}
				c.fill(x, floor + 1, z, x, G + 3, z, s(Blocks.AIR));
			}
			c.set(x, G + 4, -2, s(Blocks.CALCITE));
			c.set(x, G + 4, 2, s(Blocks.CALCITE));
			c.fill(x, G + 4, -1, x, G + 4, 1, s(Blocks.CALCITE));
		}
		for (int z : new int[]{-2, 2}) {
			c.fill(wallOut, 65, z, wallOut, G + 3, z, s(Blocks.CALCITE));
		}
		// The flag, planks, barrels, debris, a ramp into the water.
		c.fill(-50, 64, 3, -50, 67, 3, s(Blocks.POLISHED_BLACKSTONE_WALL));
		c.set(-50, 68, 3, s(Blocks.POLISHED_BLACKSTONE));
		banner(c, -51, 68, 3, Direction.WEST, Blocks.BLACK_WALL_BANNER);
		c.set(-47, 65, -3, Canvas.facing(s(Blocks.BARREL), Direction.UP));
		c.set(-48, 65, -3, Canvas.facing(s(Blocks.BARREL), Direction.UP));
		c.set(-47, 66, -3, Canvas.facing(s(Blocks.BARREL), Direction.UP));
		c.set(-49, 64, 1, Kit.slab(Blocks.SPRUCE_SLAB, false));
		c.set(-48, 64, 2, s(Blocks.SPRUCE_PLANKS));
		c.set(-50, 64, -2, s(Blocks.COBBLESTONE));
		for (int z = -1; z <= 1; z++) {
			c.set(-53, 63, z, Kit.stairs(Blocks.SPRUCE_STAIRS, Direction.EAST, false));
			c.set(-54, 62, z, Kit.stairs(Blocks.SPRUCE_STAIRS, Direction.EAST, false));
		}
	}
}
