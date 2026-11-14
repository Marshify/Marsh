package com.grandline.world.plan;

import com.grandline.world.plan.Anchor.Type;
import com.grandline.world.plan.Terrain.Ground;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.WallBannerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/**
 * Stonewall Prison, rebuilt: a rocky offshore island (about 135 x 130) that is one great prison fortress.
 * <ul>
 * <li>Outer walls 96 x 86, 3 thick, 22 high (floor y 68, wall-walk y 90) with buttresses, barred slit windows and a
 * crenellated wall-walk; four corner towers 13 x 13, 30 high, each with an inside stair, slit windows, an observation
 * deck and battlements.</li>
 * <li>South: the gatehouse (a broad stair, a 7-wide passage under a raised portcullis, flanking guard posts, banners, a
 * checkpoint) facing the pier and the forecourt where the quest giver and innkeeper stand.</li>
 * <li>Inside: a central courtyard (46 x 40) ringed by two-storey wings with a gallery walkway: west, Cell Block A and the
 * high-security cells; east, Cell Block B and the Commander's chamber; north, the Warden's arena (quest bosses fight
 * here); south, administration (office, briefing, storage, records, observation balcony). Below the courtyard, the
 * Overseer's chamber.</li>
 * <li>Combat areas: the courtyard (guards, basic) and the north patrol yard outside the walls (stronger).</li>
 * </ul>
 * Every way in is an opening or an open door (adventure mode). Island-local coordinates, +x east, +z south; built from
 * code. Older worlds keep the original prison ({@link FirstSeaPlans} legacy plan).
 */
final class StonewallPrison extends BasePlan {
	static final int G = 66, F = 68, F2 = 77, ROOF = 86, WT = 90, TT = 98;
	static final int X1 = -48, X2 = 47, Z1 = -52, Z2 = 33;           // outer faces of the curtain wall
	static final int CX1 = -23, CX2 = 22, CZ1 = -28, CZ2 = 11;       // the courtyard
	static final int UX1 = -15, UX2 = 14, UZ1 = -26, UZ2 = -2, UY = 56; // the Overseer's chamber (below)
	static final int DOCK_LAND_Z = 55, DOCK_END_Z = 80, DOCK_Y = 64;

	StonewallPrison() {
		super("Stonewall Prison", 0.13);
		terrain.mass(0, -8, 76, 72, 3.8, 6.0, Ground.STONE)
				.mass(-40, 44, 20, 13, 4.0, 3.0, Ground.STONE)
				.mass(44, 40, 18, 14, 4.5, 3.0, Ground.STONE)
				.flat(0, -10, 54, 48, G, 4, null)
				.flat(0, 48, 12, 7, G, 3, null)                            // the forecourt
				.path(s(Blocks.STONE_BRICKS), 7, 0, 42, 0, 54)
				.path(s(Blocks.GRAVEL), 3, X2 + 4, 30, X2 + 6, -40, 20, Z1 - 6, -20, Z1 - 6, X1 - 6, -40, X1 - 4, 30)
				.shoreMix(s(Blocks.STONE), s(Blocks.COBBLESTONE), s(Blocks.GRAVEL), s(Blocks.ANDESITE), s(Blocks.STONE))
				.reach(DOCK_END_Z + 4);
		anchor(Anchor.at(Type.QUEST, 7, G + 1, 47, 180));                      // the forecourt, by the gate, facing in
		anchor(new Anchor(Type.ARRIVAL, 0, Anchor.SURFACE, DOCK_LAND_Z - 2, 0, 180));
		anchor(Anchor.at(Type.HOME, -7, G + 1, 50, 90));
		anchor(Anchor.at(Type.FISHERMAN, 4, DOCK_Y + 1, DOCK_END_Z - 2, 0));
		anchor(Anchor.at(Type.DOCK, -8, 63, DOCK_END_Z - 3));
		anchor(new Anchor(Type.ENEMY_CAMP, 0, F, -8, 9));                     // the courtyard guards (basic)
		anchor(Anchor.area(Type.ENEMY_CAMP, 0, Z1 - 9, 6));                    // the north patrol yard (stronger)
		anchor(Anchor.at(Type.BOSS_ARENA, 0, F, -39));                        // the Warden's arena
		anchor(Anchor.of(Type.TREASURE, 50, 46));
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

	/** A lantern hung on {@code len} links of chain below the ceiling block at {@code ceil}. */
	static void hang(Canvas c, int x, int ceil, int z, int len, boolean soul) {
		for (int k = 1; k <= len; k++) {
			c.set(x, ceil - k, z, s(Blocks.CHAIN));
		}
		c.set(x, ceil - len - 1, z, s(soul ? Blocks.SOUL_LANTERN : Blocks.LANTERN).setValue(LanternBlock.HANGING, true));
	}

	/** A lantern on a short stone post. */
	static void post(Canvas c, int x, int y, int z) {
		c.set(x, y, z, s(Blocks.STONE_BRICK_WALL));
		c.set(x, y + 1, z, lantern(false));
	}

	static BlockState lantern(boolean hanging) {
		return s(Blocks.LANTERN).setValue(LanternBlock.HANGING, hanging);
	}

	/** Muted, weathered prison stone: stone brick with cracked, mossy and cobble courses. */
	static BlockState wall(int x, int y, int z) {
		double u = unit(x * 3 + y, z * 5 - y, 11);
		return u < 0.12 ? s(Blocks.CRACKED_STONE_BRICKS) : u < 0.18 ? s(Blocks.MOSSY_STONE_BRICKS) : u < 0.22 ? s(Blocks.COBBLESTONE) : s(Blocks.STONE_BRICKS);
	}

	static BlockState openDoor(Block door, Direction facing, DoubleBlockHalf half) {
		return s(door).setValue(BlockStateProperties.HORIZONTAL_FACING, facing).setValue(BlockStateProperties.OPEN, true)
				.setValue(BlockStateProperties.DOOR_HINGE, DoorHingeSide.LEFT).setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, half);
	}

	/** A door standing open in a wall (adventure mode can't open doors). */
	static void door(Canvas c, int x, int y, int z, Block door, Direction facing) {
		c.set(x, y, z, openDoor(door, facing, DoubleBlockHalf.LOWER));
		c.set(x, y + 1, z, openDoor(door, facing, DoubleBlockHalf.UPPER));
	}

	/** A solid box of a material (inclusive). */
	static void box(Canvas c, int x1, int y1, int z1, int x2, int y2, int z2, BlockState b) {
		c.fill(x1, y1, z1, x2, y2, z2, b);
	}

	/** A room: walls of weathered stone, floor, ceiling, cleared inside. */
	static void room(Canvas c, int x1, int z1, int x2, int z2, int floor, int ceil, Block floorBlock) {
		for (int x = x1; x <= x2; x++) {
			for (int z = z1; z <= z2; z++) {
				if (!c.inChunk(x, z)) {
					continue;
				}
				boolean w = x == x1 || x == x2 || z == z1 || z == z2;
				c.set(x, floor, z, s(floorBlock));
				for (int y = floor + 1; y < ceil; y++) {
					c.set(x, y, z, w ? wall(x, y, z) : s(Blocks.AIR));
				}
				c.set(x, ceil, z, s(Blocks.STONE_BRICKS));
			}
		}
	}

	// ==================================================================================================== build

	@Override
	public void build(Canvas c) {
		if (c.near(0, -10, 70)) {
			base(c);
			curtainWalls(c);
			for (int[] t : new int[][]{{X1, Z1}, {X2 - 12, Z1}, {X1, Z2 - 12}, {X2 - 12, Z2 - 12}}) {
				tower(c, t[0], t[1]);
			}
			courtyard(c);
			westWing(c);
			eastWing(c);
			northWing(c);
			southWing(c);
			galleryStairs(c);
			overseer(c);
			gatehouse(c);
			cornerYards(c);
			patrolYard(c);
		}
		rocks(c);
		dock(c);
		treasure(c);
	}

	/** The fortress floor: a stone plinth two blocks above the island under the whole complex. */
	private void base(Canvas c) {
		for (int x = X1 - 3; x <= X2 + 3; x++) {
			for (int z = Z1 - 3; z <= Z2 + 3; z++) {
				if (!c.inChunk(x, z)) {
					continue;
				}
				c.fill(x, G - 2, z, x, F, z, s(Blocks.STONE));
				c.set(x, F, z, s(Blocks.SMOOTH_STONE));
				c.fill(x, F + 1, z, x, TT + 3, z, s(Blocks.AIR));
			}
		}
	}

	/**
	 * The curtain walls: 3 thick, F..WT, with a plinth course, buttresses every 8 blocks on the outside, barred slit
	 * windows in two rows, a smooth-stone string course, the wall-walk on top with crenellations on the outer edge and
	 * a rail on the inner edge, and lanterns.
	 */
	private void curtainWalls(Canvas c) {
		for (int x = X1; x <= X2; x++) {
			for (int z = Z1; z <= Z2; z++) {
				if (!c.inChunk(x, z)) {
					continue;
				}
				int dx = Math.min(x - X1, X2 - x), dz = Math.min(z - Z1, Z2 - z);
				int d = Math.min(dx, dz);
				if (d > 2) {
					continue;
				}
				boolean outer = d == 0;
				boolean alongX = dz <= 2 && dz <= dx; // a north or south wall
				int along = alongX ? x : z;
				for (int y = F + 1; y <= WT; y++) {
					BlockState b = y <= F + 2 ? s(Blocks.STONE_BRICKS) : y == F + 12 ? s(Blocks.SMOOTH_STONE) : wall(x, y, z);
					boolean slit = outer && Math.floorMod(along, 8) == 4 && (y == F + 6 || y == F + 7 || y == F + 15 || y == F + 16);
					if (slit) {
						b = Kit.connected(Blocks.IRON_BARS, alongX);
					}
					c.set(x, y, z, b);
				}
				c.set(x, WT, z, s(Blocks.POLISHED_ANDESITE));
				if (outer) {
					c.set(x, WT + 1, z, Math.floorMod(along, 2) == 0 ? s(Blocks.STONE_BRICKS) : Kit.slab(Blocks.STONE_BRICK_SLAB, false));
				} else if (d == 2) {
					c.set(x, WT + 1, z, s(Blocks.STONE_BRICK_WALL));
				}
				if (d == 2 && Math.floorMod(along, 12) == 6) {
					c.set(x, WT + 2, z, lantern(false));
				}
			}
		}
		// Buttresses on the outside.
		for (int x = X1; x <= X2; x += 8) {
			buttress(c, x, Z1 - 1, Direction.NORTH);
			buttress(c, x, Z2 + 1, Direction.SOUTH);
		}
		for (int z = Z1; z <= Z2; z += 8) {
			buttress(c, X1 - 1, z, Direction.WEST);
			buttress(c, X2 + 1, z, Direction.EAST);
		}
	}

	private static void buttress(Canvas c, int x, int z, Direction out) {
		if (!c.inChunk(x, z) || (Math.abs(x) <= 9 && z > 0)) {
			return; // never across the gate
		}
		c.fill(x, G - 1, z, x, F + 10, z, s(Blocks.STONE_BRICKS));
		c.set(x, F + 11, z, stair(Blocks.STONE_BRICK_STAIRS, out.getOpposite(), false));
		int ox = x + out.getStepX(), oz = z + out.getStepZ();
		c.fill(ox, G - 1, oz, ox, F + 4, oz, s(Blocks.STONE_BRICKS));
		c.set(ox, F + 5, oz, stair(Blocks.STONE_BRICK_STAIRS, out.getOpposite(), false));
	}

	/**
	 * A corner tower (13 x 13 at (x0, z0), F..TT): walls two thick with barred slits. Inside, a stair climbs round the
	 * walls from the ground door (from the corner yard) to a landing at wall-walk height, with doorways onto both
	 * wall-walks; a second flight climbs from the landing through a hatch onto the observation deck with battlements.
	 */
	private void tower(Canvas c, int x0, int z0) {
		int x1 = x0 + 12, z1 = z0 + 12;
		for (int x = x0; x <= x1; x++) {
			for (int z = z0; z <= z1; z++) {
				if (!c.inChunk(x, z)) {
					continue;
				}
				boolean shell = x == x0 || x == x1 || z == z0 || z == z1;
				boolean shell2 = x == x0 + 1 || x == x1 - 1 || z == z0 + 1 || z == z1 - 1;
				for (int y = F + 1; y <= TT; y++) {
					if (shell || shell2) {
						int along = (x == x0 || x == x1) ? z : x;
						boolean slit = shell && Math.floorMod(along - z0, 6) == 3 && (y - F) % 6 == 4 && y < WT;
						c.set(x, y, z, slit ? Kit.connected(Blocks.IRON_BARS, !(x == x0 || x == x1)) : y == F + 12 || y == TT - 4 ? s(Blocks.SMOOTH_STONE) : wall(x, y, z));
					} else {
						c.set(x, y, z, s(Blocks.AIR));
					}
				}
				c.set(x, TT, z, s(Blocks.POLISHED_ANDESITE));
				if (shell) {
					c.set(x, TT + 1, z, (x + z) % 2 == 0 ? s(Blocks.STONE_BRICKS) : Kit.slab(Blocks.STONE_BRICK_SLAB, false));
				}
			}
		}
		boolean west = x0 == X1, north = z0 == Z1;
		int ix0 = x0 + 2, ix1 = x1 - 2, iz0 = z0 + 2, iz1 = z1 - 2;
		int ixIn = west ? ix1 : ix0, ixWall = west ? ix0 : ix1;   // the ring column toward the courtyard / the side wall
		int izIn = north ? iz1 : iz0, izWall = north ? iz0 : iz1;
		int sx = Integer.signum(ixWall - ixIn), sz = Integer.signum(izWall - izIn);
		// The ring: from the inner corner along the inner row to the side wall, along it to the outer wall, back along that.
		int[][] ring = new int[22][];
		int n = 0;
		for (int k = 0; k < 8; k++) {
			ring[n++] = new int[]{ixIn + sx * k, izIn};
		}
		for (int k = 0; k < 8; k++) {
			ring[n++] = new int[]{ixWall, izIn + sz * k};
		}
		for (int k = 0; k < 6; k++) {
			ring[n++] = new int[]{ixWall - sx * k, izWall};
		}
		for (int k = 0; k < ring.length; k++) {
			int[] cell = ring[k];
			int[] next = k + 1 < ring.length ? ring[k + 1] : new int[]{cell[0] - sx, cell[1]};
			int y = F + 1 + k;
			Direction up = next[0] > cell[0] ? Direction.EAST : next[0] < cell[0] ? Direction.WEST : next[1] > cell[1] ? Direction.SOUTH : Direction.NORTH;
			if (k > 0) {
				c.fill(cell[0], F + 1, cell[1], cell[0], y - 1, cell[1], s(Blocks.STONE_BRICKS));
			}
			c.set(cell[0], y, cell[1], stair(Blocks.STONE_BRICK_STAIRS, up, false));
		}
		// The landing at WT: everything inside except the last steps of the stair (which arrive at it).
		for (int x = ix0; x <= ix1; x++) {
			for (int z = iz0; z <= iz1; z++) {
				boolean open = false;
				for (int k = 19; k < ring.length; k++) {
					open |= ring[k][0] == x && ring[k][1] == z;
				}
				if (!open) {
					c.set(x, WT, z, s(Blocks.STONE_BRICKS));
				}
			}
		}
		// The second flight: along the middle row, from the landing up to the deck (its last step is part of the deck).
		int cz = (iz0 + iz1) / 2;
		for (int k = 0; k <= TT - WT - 1; k++) {
			int x = ix0 + 1 + k, y = WT + 1 + k;
			c.fill(x, WT + 1, cz, x, y - 1, cz, s(Blocks.STONE_BRICKS));
			c.set(x, y, cz, stair(Blocks.STONE_BRICK_STAIRS, Direction.EAST, false));
			c.fill(x, y + 1, cz, x, y + 3, cz, s(Blocks.AIR));
		}
		int mx = (ix0 + ix1) / 2;
		c.set(mx, F + 1, cz, lantern(false));
		c.set(mx, WT + 1, cz - 2, lantern(false));
		c.set(mx, TT - 1, cz + 2, lantern(true));
		// The ground door, from the corner yard into the first step.
		int gx = west ? x1 : x0, in = west ? -1 : 1;
		c.fill(gx, F + 1, izIn, gx + in, F + 3, izIn, s(Blocks.AIR));
		// Doorways onto the two wall-walks (through the rail on the walk's inner edge).
		c.fill(gx, WT + 1, izWall, gx + in, WT + 3, izWall, s(Blocks.AIR));
		c.set(gx - in, WT + 1, izWall, s(Blocks.AIR));
		int gz = north ? z1 : z0, inz = north ? -1 : 1;
		c.fill(ixWall, WT + 1, gz, ixWall, WT + 3, gz + inz, s(Blocks.AIR));
		c.set(ixWall, WT + 1, gz - inz, s(Blocks.AIR));
	}

	// ---------------------------------------------------------------------------------------------------- courtyard

	/**
	 * The courtyard: stone and gravel paving with a patrol ring, two guard stations (roofed posts with a lantern and a
	 * barrel), weapon racks, barrels, stone cover and lamp posts; the middle stays open for fighting. The stair down to
	 * the Overseer's chamber opens in the north-east corner behind iron bars.
	 */
	private void courtyard(Canvas c) {
		for (int x = CX1; x <= CX2; x++) {
			for (int z = CZ1; z <= CZ2; z++) {
				if (!c.inChunk(x, z)) {
					continue;
				}
				boolean ring = x == CX1 + 2 || x == CX2 - 2 || z == CZ1 + 2 || z == CZ2 - 2;
				double u = unit(x, z, 21);
				c.set(x, F, z, ring ? s(Blocks.STONE_BRICKS) : u < 0.4 ? s(Blocks.GRAVEL) : u < 0.55 ? s(Blocks.COBBLESTONE) : s(Blocks.STONE));
			}
		}
		for (int[] p : new int[][]{{CX1 + 5, CZ2 - 5}, {CX2 - 6, CZ2 - 5}}) {
			for (int[] o : new int[][]{{-1, -1}, {1, -1}, {-1, 1}, {1, 1}}) {
				c.fill(p[0] + o[0], F + 1, p[1] + o[1], p[0] + o[0], F + 3, p[1] + o[1], s(Blocks.DARK_OAK_FENCE));
			}
			c.fill(p[0] - 1, F + 4, p[1] - 1, p[0] + 1, F + 4, p[1] + 1, Kit.slab(Blocks.STONE_BRICK_SLAB, false));
			c.set(p[0], F + 3, p[1], lantern(true));
			c.set(p[0], F + 1, p[1], Canvas.facing(s(Blocks.BARREL), Direction.UP));
		}
		for (int x : new int[]{-19, -8, 5}) {
			rack(c, x, CZ1 + 1);
		}
		for (int[] b : new int[][]{{CX1 + 1, CZ1 + 1}, {CX1 + 2, CZ1 + 1}, {CX1 + 1, CZ1 + 2}, {CX2 - 1, CZ2 - 1}, {CX2 - 2, CZ2 - 1}, {CX1 + 1, CZ2 - 1}}) {
			c.set(b[0], F + 1, b[1], Canvas.facing(s(Blocks.BARREL), Direction.UP));
		}
		// Stone cover: two low wall stubs.
		for (int k = -2; k <= 2; k++) {
			c.set(-10 + k, F + 1, -16, s(Blocks.STONE_BRICK_WALL));
			c.set(10, F + 1, k, s(Blocks.STONE_BRICK_WALL));
		}
		// Lamp posts.
		for (int[] l : new int[][]{{CX1 + 2, CZ1 + 2}, {CX2 - 2, CZ1 + 9}, {CX1 + 2, CZ2 - 2}, {CX2 - 2, CZ2 - 2}, {-1, -8}}) {
			c.fill(l[0], F + 1, l[1], l[0], F + 3, l[1], s(Blocks.STONE_BRICK_WALL));
			c.set(l[0], F + 4, l[1], lantern(false));
		}
	}

	/** Stairs from the courtyard up to the gallery on its west and east sides (rising north), through a gap in its rail. */
	private void galleryStairs(Canvas c) {
		for (int gx : new int[]{CX1 + 1, CX2 - 1}) {
			for (int k = 0; k < F2 - F; k++) {
				int z = -4 - k, y = F + 1 + k;
				c.fill(gx, F + 1, z, gx, y - 1, z, s(Blocks.STONE_BRICKS));
				c.set(gx, y, z, stair(Blocks.STONE_BRICK_STAIRS, Direction.NORTH, false));
				c.fill(gx, y + 1, z, gx, y + 3, z, s(Blocks.AIR));
			}
		}
	}

	private static void rack(Canvas c, int x, int z) {
		c.fill(x, F + 1, z, x, F + 2, z, s(Blocks.DARK_OAK_FENCE));
		c.fill(x + 2, F + 1, z, x + 2, F + 2, z, s(Blocks.DARK_OAK_FENCE));
		c.set(x + 1, F + 2, z, Kit.slab(Blocks.DARK_OAK_SLAB, true));
		c.set(x + 1, F + 1, z, s(Blocks.LIGHTNING_ROD));
	}

	/**
	 * A two-storey wing between the curtain wall and the courtyard: outer walls, the courtyard face with barred windows,
	 * the upper floor (lit, reached by {@code doors} on the gallery) and the roof with a parapet. The courtyard face
	 * carries a gallery walkway (stone deck on corbels with a barred rail) at the upper floor, over the courtyard only, so
	 * the four galleries meet at the corners.
	 */
	private void wing(Canvas c, int x1, int z1, int x2, int z2, Direction face, int... doors) {
		for (int x = x1; x <= x2; x++) {
			for (int z = z1; z <= z2; z++) {
				if (!c.inChunk(x, z)) {
					continue;
				}
				boolean w = x == x1 || x == x2 || z == z1 || z == z2;
				for (int y = F + 1; y < ROOF; y++) {
					c.set(x, y, z, w ? wall(x, y, z) : s(Blocks.AIR));
				}
				c.set(x, F2, z, s(Blocks.STONE_BRICKS));
				c.set(x, ROOF, z, s(Blocks.STONE_BRICKS));
				if (w) {
					c.set(x, ROOF + 1, z, (x + z) % 2 == 0 ? s(Blocks.STONE_BRICK_WALL) : Kit.slab(Blocks.STONE_BRICK_SLAB, false));
				}
			}
		}
		int fx = face == Direction.EAST ? x2 : face == Direction.WEST ? x1 : 0, fz = face == Direction.SOUTH ? z2 : face == Direction.NORTH ? z1 : 0;
		boolean alongX = face.getAxis() == Direction.Axis.Z;
		// Lanterns hung from the roof of the upper floor, down the middle.
		for (int t = (alongX ? x1 : z1) + 4; t <= (alongX ? x2 : z2) - 2; t += 8) {
			hang(c, alongX ? t : (x1 + x2) / 2, ROOF, alongX ? (z1 + z2) / 2 : t, 2, false);
		}
		int lo = alongX ? CX1 : CZ1, hi = alongX ? CX2 : CZ2;
		// Barred windows on the courtyard face, both floors.
		for (int t = Math.max(lo, alongX ? x1 : z1) + 2; t <= Math.min(hi, alongX ? x2 : z2) - 2; t += 4) {
			for (int y : new int[]{F + 3, F2 + 3}) {
				c.set(alongX ? t : fx, y, alongX ? fz : t, Kit.connected(Blocks.IRON_BARS, alongX));
			}
		}
		// The gallery outside the courtyard face.
		for (int t = Math.max(lo, alongX ? x1 : z1); t <= Math.min(hi, alongX ? x2 : z2); t++) {
			int gx = alongX ? t : fx + face.getStepX(), gz = alongX ? fz + face.getStepZ() : t;
			c.set(gx, F2, gz, s(Blocks.POLISHED_ANDESITE));
			c.set(gx, F2 - 1, gz, stair(Blocks.STONE_BRICK_STAIRS, face.getOpposite(), true));
			if (t > lo && t < hi) {
				int rx = gx + face.getStepX(), rz = gz + face.getStepZ();
				c.set(rx, F2 + 1, rz, Kit.connected(Blocks.IRON_BARS, alongX));
				c.set(rx, F2, rz, Kit.slab(Blocks.STONE_BRICK_SLAB, true));
				if (Math.floorMod(t, 8) == 4) {
					c.set(rx, F2 + 2, rz, lantern(false));
				}
			}
		}
		for (int t : doors) {
			c.fill(alongX ? t : fx, F2 + 1, alongX ? fz : t, alongX ? t : fx, F2 + 2, alongX ? fz : t, s(Blocks.AIR));
		}
	}

	/** A cell: a barred front with an open iron door, a ceiling and a hanging light, a bed, a bench, a barred vent. */
	private static void cell(Canvas c, int x1, int z1, int x2, int z2, Direction front, int floor, boolean secure) {
		for (int x = x1; x <= x2; x++) {
			for (int z = z1; z <= z2; z++) {
				boolean w = x == x1 || x == x2 || z == z1 || z == z2;
				boolean isFront = (front == Direction.EAST && x == x2) || (front == Direction.WEST && x == x1)
						|| (front == Direction.SOUTH && z == z2) || (front == Direction.NORTH && z == z1);
				for (int y = floor + 1; y <= floor + 3; y++) {
					BlockState b;
					if (!w) {
						b = s(Blocks.AIR);
					} else if (isFront && !((x == x1 || x == x2) && (z == z1 || z == z2))) {
						b = Kit.connected(Blocks.IRON_BARS, front.getAxis() == Direction.Axis.Z);
					} else {
						b = secure ? s(Blocks.POLISHED_ANDESITE) : wall(x, y, z);
					}
					c.set(x, y, z, b);
				}
				c.set(x, floor + 4, z, secure ? s(Blocks.POLISHED_ANDESITE) : s(Blocks.STONE_BRICKS));
			}
		}
		int dx = front.getAxis() == Direction.Axis.X ? (front == Direction.EAST ? x2 : x1) : (x1 + x2) / 2;
		int dz = front.getAxis() == Direction.Axis.Z ? (front == Direction.SOUTH ? z2 : z1) : (z1 + z2) / 2;
		door(c, dx, floor + 1, dz, Blocks.IRON_DOOR, front);
		Direction back = front.getOpposite();
		int bx = dx + back.getStepX() * (front.getAxis() == Direction.Axis.X ? (x2 - x1 - 1) : 0);
		int bz = dz + back.getStepZ() * (front.getAxis() == Direction.Axis.Z ? (z2 - z1 - 1) : 0);
		Direction side = front.getClockWise();
		int ex = bx + side.getStepX(), ez = bz + side.getStepZ();
		c.set(ex, floor + 1, ez, s(Blocks.WHITE_BED).setValue(BedBlock.FACING, back).setValue(BedBlock.PART, BedPart.HEAD));
		c.set(ex - back.getStepX(), floor + 1, ez - back.getStepZ(), s(Blocks.WHITE_BED).setValue(BedBlock.FACING, back).setValue(BedBlock.PART, BedPart.FOOT));
		c.set(bx - side.getStepX(), floor + 1, bz - side.getStepZ(), Kit.slab(Blocks.STONE_BRICK_SLAB, false));
		c.set(bx + back.getStepX(), floor + 2, bz + back.getStepZ(), Kit.connected(Blocks.IRON_BARS, front.getAxis() == Direction.Axis.Z));
		c.set((x1 + x2) / 2, floor + 3, (z1 + z2) / 2, secure ? s(Blocks.SOUL_LANTERN).setValue(LanternBlock.HANGING, true) : lantern(true));
	}

	/** A doorway (w wide, 3 high) through a wall running along z at x, or along x at z. */
	private static void opening(Canvas c, int x1, int z1, int x2, int z2, int h) {
		c.fill(x1, F + 1, z1, x2, F + h, z2, s(Blocks.AIR));
	}

	// ---------------------------------------------------------------------------------------------------- wings

	/**
	 * West wing: Cell Block A (south: a corridor with cells both sides and a guard desk at its entrance) and the
	 * high-security block (north: larger reinforced cells off their own corridor, behind a barred divider).
	 */
	private void westWing(Canvas c) {
		int x1 = X1 + 3, x2 = CX1 - 1, z1 = Z1 + 13, z2 = Z2 - 13;
		if (!c.near((x1 + x2) / 2, (z1 + z2) / 2, 50)) {
			return;
		}
		wing(c, x1, z1, x2, z2, Direction.EAST, -16, 4);
		int cx = (x1 + x2) / 2;
		// Cell Block A: corridor cx-1..cx+1 from z -7 to the south end; the courtyard way in at z 6..8.
		for (int z = -7; z <= z2 - 1; z++) {
			for (int x = cx - 1; x <= cx + 1; x++) {
				c.set(x, F, z, s(Blocks.POLISHED_ANDESITE));
			}
			if (Math.floorMod(z, 6) == 0) {
				hang(c, cx, F2, z, 2, false);
			}
		}
		for (int z = -5; z + 4 <= z2 - 1; z += 5) {
			cell(c, x1 + 1, z, cx - 2, z + 4, Direction.EAST, F, false);
			if (z != 5) {
				cell(c, cx + 2, z, x2 - 1, z + 4, Direction.WEST, F, false);
			}
		}
		opening(c, cx + 2, 6, x2, 8, 3);
		c.set(cx + 4, F + 1, 9, Kit.slab(Blocks.DARK_OAK_SLAB, true));
		c.set(cx + 4, F + 2, 9, s(Blocks.LANTERN));
		c.set(cx + 5, F + 1, 9, stair(Blocks.DARK_OAK_STAIRS, Direction.EAST, false));
		// High-security block: z z1+1 .. -9, a corridor along cx and 7-deep cells of polished andesite.
		for (int z = z1 + 1; z <= -9; z++) {
			for (int x = cx - 1; x <= cx + 1; x++) {
				c.set(x, F, z, s(Blocks.SMOOTH_STONE));
			}
			if (Math.floorMod(z, 7) == 0) {
				hang(c, cx, F2, z, 3, Math.floorMod(z, 14) == 0);
			}
		}
		for (int z : new int[]{z1 + 1, z1 + 8, z1 + 15, z1 + 22}) {
			cell(c, x1 + 1, z, cx - 2, z + 6, Direction.EAST, F, true);
			if (z != z1 + 15) {
				cell(c, cx + 2, z, x2 - 1, z + 6, Direction.WEST, F, true);
			}
		}
		opening(c, cx + 2, z1 + 17, x2, z1 + 19, 3);
		// The divider between the blocks: solid, with a barred gate (a two-high gap) on the corridor.
		for (int x = x1 + 1; x <= x2 - 1; x++) {
			c.fill(x, F + 1, -8, x, F2 - 1, -8, s(Blocks.POLISHED_ANDESITE));
		}
		c.fill(cx - 1, F + 1, -8, cx + 1, F + 4, -8, Kit.connected(Blocks.IRON_BARS, true));
		c.fill(cx, F + 1, -8, cx, F + 2, -8, s(Blocks.AIR));
	}

	/**
	 * East wing: Cell Block B (south: a narrow corridor, small reinforced cells, a barred checkpoint at the way in) and
	 * the Commander's chamber (north: a tall hall with iron-bar partitions, a central platform, a raised balcony with
	 * banners along the outer wall reached by a stair, and an overlook from the upper floor). No boss is registered for it.
	 */
	private void eastWing(Canvas c) {
		int x1 = CX2 + 1, x2 = X2 - 3, z1 = Z1 + 13, z2 = Z2 - 13;
		if (!c.near((x1 + x2) / 2, (z1 + z2) / 2, 50)) {
			return;
		}
		wing(c, x1, z1, x2, z2, Direction.WEST, 6);
		int cx = (x1 + x2) / 2;
		// Cell Block B: corridor cx..cx+1, cells of polished andesite both sides; the way in at z 4..6.
		for (int z = -4; z <= z2 - 1; z++) {
			c.set(cx, F, z, s(Blocks.SMOOTH_STONE));
			c.set(cx + 1, F, z, s(Blocks.SMOOTH_STONE));
			if (Math.floorMod(z, 4) == 0) {
				hang(c, cx, F2, z, 3, Math.floorMod(z, 8) == 0);
			}
		}
		for (int z = -3; z + 4 <= z2 - 1; z += 5) {
			if (z != 2) {
				cell(c, x1 + 1, z, cx - 1, z + 4, Direction.EAST, F, true);
			}
			cell(c, cx + 2, z, x2 - 1, z + 4, Direction.WEST, F, true);
		}
		opening(c, x1, 4, cx - 1, 6, 3);
		c.fill(cx - 1, F + 1, 3, cx - 1, F + 3, 6, Kit.connected(Blocks.IRON_BARS, false));
		c.fill(cx - 1, F + 1, 5, cx - 1, F + 2, 5, s(Blocks.AIR));
		c.set(x1 + 2, F + 1, 2, Kit.slab(Blocks.DARK_OAK_SLAB, true));
		c.set(x1 + 2, F + 2, 2, s(Blocks.LANTERN));
		c.set(x1 + 3, F + 1, 2, stair(Blocks.DARK_OAK_STAIRS, Direction.EAST, false));
		// The Commander's chamber: z z1+1 .. -6, open to the roof.
		int hz1 = z1 + 1, hz2 = -6;
		for (int x = x1 + 1; x <= x2 - 1; x++) {
			for (int z = hz1; z <= hz2; z++) {
				c.set(x, F, z, Math.floorMod(x + z, 4) == 0 ? s(Blocks.CHISELED_STONE_BRICKS) : s(Blocks.POLISHED_ANDESITE));
				c.fill(x, F + 1, z, x, ROOF - 1, z, s(Blocks.AIR));
			}
			c.fill(x, F + 1, hz2 + 1, x, F2 - 1, hz2 + 1, s(Blocks.POLISHED_ANDESITE));
			if (x < x2 - 2) {
				c.set(x, F2 + 1, hz2 + 1, Kit.connected(Blocks.IRON_BARS, true)); // the overlook's rail
			}
		}
		opening(c, cx - 1, hz2 + 1, cx + 1, hz2 + 1, 4);                         // from Cell Block B
		opening(c, x1, -20, x1, -17, 4);                                          // from the courtyard
		for (int z = hz1 + 4; z <= hz2 - 4; z += 6) {
			if (z < -21 || z > -16) {
				c.fill(x1 + 1, F + 1, z, x1 + 3, F + 4, z, Kit.connected(Blocks.IRON_BARS, true));
			}
			c.fill(x2 - 4, F + 1, z, x2 - 3, F + 4, z, Kit.connected(Blocks.IRON_BARS, true));
		}
		// The balcony along the outer wall, its rail, banners and the stair up from the floor.
		for (int z = hz1; z <= hz2; z++) {
			c.set(x2 - 1, F2, z, s(Blocks.POLISHED_ANDESITE));
			c.set(x2 - 2, F2, z, s(Blocks.POLISHED_ANDESITE));
			c.set(x2 - 1, F2 - 1, z, stair(Blocks.STONE_BRICK_STAIRS, Direction.EAST, true));
			c.set(x2 - 2, F2 - 1, z, stair(Blocks.STONE_BRICK_STAIRS, Direction.EAST, true));
			c.set(x2 - 3, F2 + 1, z, Kit.connected(Blocks.IRON_BARS, false));
			if (Math.floorMod(z, 6) == 0) {
				c.set(x2 - 1, F2 + 3, z, s(Blocks.RED_WALL_BANNER).setValue(WallBannerBlock.FACING, Direction.WEST));
			}
		}
		for (int k = 0; k < F2 - F; k++) {
			int z = hz2 - 1 - k, y = F + 1 + k;
			c.fill(x2 - 2, F + 1, z, x2 - 2, y - 1, z, s(Blocks.STONE_BRICKS));
			c.set(x2 - 2, y, z, stair(Blocks.STONE_BRICK_STAIRS, Direction.NORTH, false));
			c.fill(x2 - 2, y + 1, z, x2 - 2, y + 3, z, s(Blocks.AIR));
			c.set(x2 - 3, y + 1, z, s(Blocks.AIR));
		}
		int pz = (hz1 + hz2) / 2;
		c.fill(cx - 2, F + 1, pz - 2, cx + 2, F + 1, pz + 2, s(Blocks.POLISHED_ANDESITE));
		c.set(cx, F + 2, pz, s(Blocks.CHISELED_STONE_BRICKS));
		for (int dz : new int[]{-12, -6, 0, 6, 12}) {
			hang(c, cx, ROOF, pz + dz, 8, false);
			hang(c, cx - 6, ROOF, pz + dz, 8, false);
		}
		for (int z = hz1 + 1; z <= hz2 - 1; z += 4) {
			post(c, x1 + 1, F + 1, z);
		}
		for (int z : new int[]{hz1 + 2, hz2 - 2}) {
			c.set(x1 + 1, F + 1, z, lantern(false));
		}
	}

	/**
	 * North wing: the Warden's arena (44 x 19 floor, open to the roof): pillars, a raised platform at the far end,
	 * barred windows high up, banners and light, three entrances from the courtyard and doors from the corner yards.
	 * Quest bosses fight here.
	 */
	private void northWing(Canvas c) {
		int x1 = CX1, x2 = CX2, z1 = Z1 + 3, z2 = CZ1 - 1;
		if (!c.near(0, (z1 + z2) / 2, 40)) {
			return;
		}
		wing(c, x1, z1, x2, z2, Direction.SOUTH);
		for (int x = x1 + 1; x <= x2 - 1; x++) {
			for (int z = z1 + 1; z <= z2 - 1; z++) {
				c.set(x, F, z, (Math.floorMod(x, 4) == 0 && Math.floorMod(z, 4) == 0) ? s(Blocks.CHISELED_STONE_BRICKS)
						: unit(x, z, 31) < 0.2 ? s(Blocks.CRACKED_STONE_BRICKS) : s(Blocks.STONE_BRICKS));
				c.fill(x, F + 1, z, x, ROOF - 1, z, s(Blocks.AIR));
			}
		}
		for (int x = x1 + 5; x <= x2 - 5; x += 9) {
			for (int z : new int[]{z1 + 4, z2 - 4}) {
				c.fill(x, F + 1, z, x, ROOF - 1, z, s(Blocks.POLISHED_ANDESITE));
				c.set(x, F + 1, z, s(Blocks.CHISELED_STONE_BRICKS));
				c.set(x, F + 8, z, s(Blocks.CHISELED_STONE_BRICKS));
			}
		}
		for (int x = -6; x <= 6; x++) {
			c.set(x, F + 1, z1 + 1, s(Blocks.POLISHED_ANDESITE));
			c.set(x, F + 1, z1 + 2, stair(Blocks.STONE_BRICK_STAIRS, Direction.NORTH, false));
		}
		for (int x = x1 + 3; x <= x2 - 3; x += 4) {
			c.set(x, F2 + 4, z1, Kit.connected(Blocks.IRON_BARS, true));
		}
		for (int x = x1 + 4; x <= x2 - 4; x += 6) {
			for (int z : new int[]{z1 + 6, z2 - 6}) {
				hang(c, x, ROOF, z, 8, false);
			}
		}
		for (int x = x1 + 2; x <= x2 - 2; x += 4) {
			if (Math.abs(x) > 7) {
				post(c, x, F + 1, z1 + 1);
			}
		}
		for (int z = z1 + 2; z <= z2 - 2; z += 4) {
			post(c, x1 + 1, F + 1, z);
			post(c, x2 - 1, F + 1, z);
		}
		for (int x : new int[]{-12, 0, 12}) {
			opening(c, x - 1, z2, x + 1, z2, 4);
			c.set(x, F + 5, z2, s(Blocks.CHISELED_STONE_BRICKS));
		}
		for (int x : new int[]{-9, -3, 3, 9}) {
			c.set(x, F + 7, z1 + 1, s(Blocks.RED_WALL_BANNER).setValue(WallBannerBlock.FACING, Direction.SOUTH));
		}
		for (int x : new int[]{x1 + 1, x2 - 1}) {
			for (int z : new int[]{z1 + 2, z2 - 2}) {
				c.set(x, F + 1, z, lantern(false));
			}
		}
		// Doors from the corner yards on either side.
		opening(c, x1, -45, x1, -44, 3);
		opening(c, x2, -45, x2, -44, 3);
	}

	/**
	 * South wing either side of the gate passage: administration (west: the warden's office with a desk, shelves and a
	 * map table; the records room of shelves and a lectern; upstairs, the observation balcony over the courtyard) and the
	 * guard block (east: the briefing room with benches round a table; the storage room of barrels and chests). Over the
	 * passage, a bridge joins the two upper floors.
	 */
	private void southWing(Canvas c) {
		int z1 = CZ2 + 1, z2 = Z2 - 3;
		if (!c.near(0, (z1 + z2) / 2, 40)) {
			return;
		}
		wing(c, CX1, z1, -5, z2, Direction.NORTH);
		wing(c, 4, z1, CX2, z2, Direction.NORTH, 13);
		// The passage roof and the bridge room above it.
		for (int x = -4; x <= 3; x++) {
			for (int z = z1; z <= z2; z++) {
				boolean w = z == z1 || z == z2;
				c.set(x, F2, z, s(Blocks.STONE_BRICKS));
				for (int y = F2 + 1; y < ROOF; y++) {
					c.set(x, y, z, w ? wall(x, y, z) : s(Blocks.AIR));
				}
				c.set(x, ROOF, z, s(Blocks.STONE_BRICKS));
			}
			c.set(x, F2 + 3, z1, x % 2 == 0 ? Kit.connected(Blocks.IRON_BARS, true) : wall(x, F2 + 3, z1));
			c.set(x, F2, z1 - 1, s(Blocks.POLISHED_ANDESITE)); // the gallery over the gate passage
			c.set(x, F2 - 1, z1 - 1, stair(Blocks.STONE_BRICK_STAIRS, Direction.SOUTH, true));
			c.set(x, F2, z1 - 2, Kit.slab(Blocks.STONE_BRICK_SLAB, true));
			c.set(x, F2 + 1, z1 - 2, Kit.connected(Blocks.IRON_BARS, true));
		}
		c.fill(-1, F2 + 1, z1, 0, F2 + 2, z1, s(Blocks.AIR));
		hang(c, 0, ROOF, (z1 + z2) / 2, 2, false);
		for (int x : new int[]{-5, 4}) {
			c.fill(x, F2 + 1, 20, x, F2 + 2, 21, s(Blocks.AIR));
		}
		// The warden's office (x -23..-12) and the records room (x -12..-5).
		room(c, CX1, z1, -12, z2, F, F2, Blocks.DARK_OAK_PLANKS);
		room(c, -12, z1, -5, z2, F, F2, Blocks.SPRUCE_PLANKS);
		door(c, -17, F + 1, z1, Blocks.DARK_OAK_DOOR, Direction.NORTH);
		door(c, -8, F + 1, z1, Blocks.SPRUCE_DOOR, Direction.NORTH);
		c.fill(-12, F + 1, 21, -12, F + 2, 21, s(Blocks.AIR));
		c.fill(CX1, F + 1, 24, CX1, F + 3, 25, s(Blocks.AIR)); // from the south-west corner yard
		for (int x = -20; x <= -16; x++) {
			c.set(x, F + 1, z2 - 4, Kit.slab(Blocks.DARK_OAK_SLAB, true));
		}
		c.set(-18, F + 1, z2 - 3, stair(Blocks.DARK_OAK_STAIRS, Direction.SOUTH, false));
		c.set(-18, F + 2, z2 - 4, s(Blocks.LANTERN));
		c.set(-20, F + 2, z2 - 4, s(Blocks.FLOWER_POT));
		for (int x = CX1 + 1; x <= -13; x++) {
			c.set(x, F + 1, z2 - 1, s(Blocks.BOOKSHELF));
			c.set(x, F + 2, z2 - 1, s(Blocks.BOOKSHELF));
		}
		c.set(-14, F + 1, z1 + 2, s(Blocks.CARTOGRAPHY_TABLE));
		c.set(-22, F + 1, z1 + 1, Canvas.facing(s(Blocks.BARREL), Direction.UP));
		c.set(-17, F + 5, z2 - 1, s(Blocks.RED_WALL_BANNER).setValue(WallBannerBlock.FACING, Direction.NORTH));
		hang(c, -17, F2, (z1 + z2) / 2, 3, false);
		hang(c, -17, F2, z1 + 3, 3, false);
		for (int z = z1 + 2; z <= z2 - 1; z += 2) {
			c.set(-11, F + 1, z, s(Blocks.BOOKSHELF));
			c.set(-11, F + 2, z, s(Blocks.BOOKSHELF));
			c.set(-6, F + 1, z, s(Blocks.BOOKSHELF));
			c.set(-6, F + 2, z, s(Blocks.BOOKSHELF));
		}
		c.set(-8, F + 1, z2 - 2, s(Blocks.LECTERN));
		hang(c, -8, F2, (z1 + z2) / 2, 3, false);
		hang(c, -8, F2, z1 + 3, 3, false);
		// The briefing room (x 4..14) and storage (x 14..22).
		room(c, 4, z1, 14, z2, F, F2, Blocks.SPRUCE_PLANKS);
		room(c, 14, z1, CX2, z2, F, F2, Blocks.STONE);
		door(c, 9, F + 1, z1, Blocks.SPRUCE_DOOR, Direction.NORTH);
		door(c, 18, F + 1, z1, Blocks.SPRUCE_DOOR, Direction.NORTH);
		c.fill(CX2, F + 1, 24, CX2, F + 3, 25, s(Blocks.AIR)); // from the south-east corner yard
		c.fill(7, F + 1, z2 - 5, 11, F + 1, z2 - 5, Kit.slab(Blocks.DARK_OAK_SLAB, true));
		for (int x = 6; x <= 12; x++) {
			c.set(x, F + 1, z2 - 3, stair(Blocks.SPRUCE_STAIRS, Direction.SOUTH, false));
			c.set(x, F + 1, z2 - 7, stair(Blocks.SPRUCE_STAIRS, Direction.NORTH, false));
		}
		c.set(9, F + 2, z2 - 5, s(Blocks.LANTERN));
		c.set(9, F + 4, z2 - 1, s(Blocks.RED_WALL_BANNER).setValue(WallBannerBlock.FACING, Direction.NORTH));
		hang(c, 9, F2, (z1 + z2) / 2, 3, false);
		hang(c, 9, F2, z1 + 3, 3, false);
		for (int x = 15; x <= CX2 - 1; x++) {
			for (int z = z1 + 2; z <= z2 - 1; z += 2) {
				if (x != 18) {
					c.set(x, F + 1, z, unit(x, z, 41) < 0.5 ? Canvas.facing(s(Blocks.BARREL), Direction.UP) : s(Blocks.CHEST));
				}
			}
		}
		hang(c, 18, F2, (z1 + z2) / 2, 3, false);
		hang(c, 18, F2, z1 + 3, 3, false);
		// The observation balcony: the administration's upper floor opens onto the gallery, with seats looking out.
		c.fill(-20, F2 + 1, z1, -8, F2 + 2, z1, s(Blocks.AIR));
		for (int x = -19; x <= -9; x += 2) {
			c.set(x, F2 + 1, z1 + 2, stair(Blocks.DARK_OAK_STAIRS, Direction.SOUTH, false));
		}
		c.set(-14, F2 + 1, z1 + 4, lantern(false));
	}

	/**
	 * The Overseer's chamber under the courtyard (floor y 56, 9 high): a stair down from the courtyard's north-east
	 * corner behind iron bars, massive pillars, a raised walkway round three walls (reached by a short stair), a central
	 * platform, soul lanterns. No boss is registered for it.
	 */
	private void overseer(Canvas c) {
		if (!c.near(0, (UZ1 + UZ2) / 2, 30)) {
			return;
		}
		int top = UY + 9;
		for (int x = UX1 - 1; x <= UX2 + 1; x++) {
			for (int z = UZ1 - 1; z <= UZ2 + 1; z++) {
				if (!c.inChunk(x, z)) {
					continue;
				}
				boolean w = x < UX1 || x > UX2 || z < UZ1 || z > UZ2;
				c.set(x, UY, z, w ? s(Blocks.STONE_BRICKS) : Math.floorMod(x + z, 3) == 0 ? s(Blocks.POLISHED_ANDESITE) : s(Blocks.SMOOTH_STONE));
				for (int y = UY + 1; y < top; y++) {
					c.set(x, y, z, w ? wall(x, y, z) : s(Blocks.AIR));
				}
				c.set(x, top, z, s(Blocks.STONE_BRICKS));
				boolean walk = !w && (x <= UX1 + 1 || x >= UX2 - 1 || z <= UZ1 + 1);
				if (walk) {
					c.set(x, UY + 4, z, s(Blocks.POLISHED_ANDESITE));
					boolean edge = (x == UX1 + 1 && z > UZ1 + 1) || (x == UX2 - 1 && z > UZ1 + 1) || (z == UZ1 + 1 && x > UX1 + 1 && x < UX2 - 1);
					if (edge) {
						c.set(x, UY + 5, z, Kit.connected(Blocks.IRON_BARS, z == UZ1 + 1));
					}
				}
			}
		}
		for (int x = UX1 + 5; x <= UX2 - 5; x += 9) {
			for (int z = UZ1 + 6; z <= UZ2 - 4; z += 10) {
				c.fill(x - 1, UY + 1, z - 1, x, top - 1, z, s(Blocks.POLISHED_DEEPSLATE));
				c.set(x + 1, UY + 1, z + 1, s(Blocks.SOUL_LANTERN));
			}
		}
		int cx = (UX1 + UX2) / 2, cz = (UZ1 + UZ2) / 2;
		c.fill(cx - 3, UY + 1, cz - 3, cx + 3, UY + 1, cz + 3, s(Blocks.POLISHED_ANDESITE));
		c.fill(cx - 2, UY + 2, cz - 2, cx + 2, UY + 2, cz + 2, Kit.slab(Blocks.POLISHED_ANDESITE_SLAB, false));
		for (int dx : new int[]{-8, 0, 8}) {
			c.set(cx + dx, top - 1, cz, s(Blocks.SOUL_LANTERN).setValue(LanternBlock.HANGING, true));
			c.set(cx + dx, top - 1, UZ1 + 4, s(Blocks.SOUL_LANTERN).setValue(LanternBlock.HANGING, true));
		}
		for (int dx : new int[]{-5, 5}) {
			hang(c, cx + dx, top, cz + 6, 2, false);
			hang(c, cx + dx, top, cz - 6, 2, false);
		}
		// A short stair from the floor up to the north walkway.
		for (int k = 0; k < 4; k++) {
			int z = UZ1 + 5 - k, y = UY + 1 + k;
			c.fill(4, UY + 1, z, 4, y - 1, z, s(Blocks.STONE_BRICKS));
			c.set(4, y, z, stair(Blocks.STONE_BRICK_STAIRS, Direction.NORTH, false));
			c.fill(4, y + 1, z, 4, y + 3, z, s(Blocks.AIR));
		}
		c.set(4, UY + 5, UZ1 + 1, s(Blocks.AIR));
		// The stair down from the courtyard (x 18..20, z -26 south, F down to UY) behind iron bars.
		for (int k = 0; k <= F - UY; k++) {
			int z = -26 + k, y = F - k;
			for (int x = 18; x <= 20; x++) {
				c.set(x, y, z, stair(Blocks.STONE_BRICK_STAIRS, Direction.NORTH, false));
				c.fill(x, y + 1, z, x, y + 4, z, s(Blocks.AIR));
			}
			c.set(17, y + 1, z, s(Blocks.STONE_BRICKS));
			c.set(21, y + 1, z, s(Blocks.STONE_BRICKS));
			if (k > 0 && k % 4 == 0) {
				c.set(19, y + 4, z, s(Blocks.SOUL_LANTERN).setValue(LanternBlock.HANGING, true));
			}
		}
		for (int z = -26; z <= -21; z++) {
			c.set(17, F + 1, z, Kit.connected(Blocks.IRON_BARS, false));
			c.set(21, F + 1, z, Kit.connected(Blocks.IRON_BARS, false));
		}
		c.fill(18, F + 1, -21, 20, F + 1, -21, Kit.connected(Blocks.IRON_BARS, true));
		c.fill(UX2 + 1, UY + 1, -14, 20, UY + 4, -12, s(Blocks.AIR));
		c.fill(UX2 + 1, UY, -14, 20, UY, -12, s(Blocks.STONE_BRICKS));
		c.set(UX2 + 1, UY + 4, -14, Kit.connected(Blocks.IRON_BARS, false));
		c.set(UX2 + 1, UY + 4, -12, Kit.connected(Blocks.IRON_BARS, false));
	}

	/**
	 * The gatehouse: a projecting block with the 7-wide passage (a raised portcullis of iron bars in its top course), a
	 * broad stair down to the forecourt, two guard posts, banners, a checkpoint desk, and a crenellated platform on top
	 * that joins the wall-walk.
	 */
	private void gatehouse(Canvas c) {
		if (!c.near(0, Z2 + 3, 20)) {
			return;
		}
		int gz1 = Z2 - 2, gz2 = Z2 + 6;
		for (int x = -8; x <= 8; x++) {
			for (int z = gz1; z <= gz2; z++) {
				boolean passage = Math.abs(x) <= 3;
				for (int y = F + 1; y <= WT + 2; y++) {
					boolean open = passage && y <= F + 8;
					boolean crenel = y >= WT + 1 && (z == gz2 || (Math.abs(x) == 8 && z > Z2));
					BlockState b = open ? s(Blocks.AIR) : crenel ? (y == WT + 1 ? s(Blocks.STONE_BRICKS) : (x + z) % 2 == 0 ? s(Blocks.STONE_BRICKS) : s(Blocks.AIR))
							: y > WT ? s(Blocks.AIR) : y == WT ? s(Blocks.POLISHED_ANDESITE) : wall(x, y, z);
					c.set(x, y, z, b);
				}
				c.fill(x, G - 3, z, x, F - 1, z, s(Blocks.STONE_BRICKS));
				c.set(x, F, z, passage ? s(Blocks.POLISHED_ANDESITE) : s(Blocks.STONE_BRICKS));
			}
		}
		for (int x = -3; x <= 3; x++) {
			c.set(x, F + 8, gz2 - 1, Kit.connected(Blocks.IRON_BARS, true)); // the raised portcullis
			c.set(x, F + 9, gz2, s(Blocks.CHISELED_STONE_BRICKS));
		}
		for (int x : new int[]{-4, 4}) {
			c.set(x, F + 9, gz2, s(Blocks.CHISELED_STONE_BRICKS));
		}
		// The passage on through the south wing to the courtyard.
		for (int z = CZ2 + 1; z < gz1; z++) {
			for (int x = -3; x <= 3; x++) {
				c.set(x, F, z, s(Blocks.POLISHED_ANDESITE));
				c.fill(x, F + 1, z, x, F + 8, z, s(Blocks.AIR));
			}
			c.fill(-4, F + 1, z, -4, F2 - 1, z, wall(-4, F + 1, z));
			c.fill(4, F + 1, z, 4, F2 - 1, z, wall(4, F + 1, z));
			if (z % 5 == 0) {
				c.set(-3, F + 8, z, lantern(true));
				c.set(3, F + 8, z, lantern(true));
			}
		}
		for (int z = gz1; z <= gz2; z += 4) {
			c.set(-3, F + 8, z, lantern(true));
			c.set(3, F + 8, z, lantern(true));
		}
		// Broad stair from the forecourt (66) to the floor (68).
		for (int k = 0; k < 2; k++) {
			for (int x = -9; x <= 9; x++) {
				c.set(x, G + 1 + k, gz2 + 2 - k, stair(Blocks.STONE_BRICK_STAIRS, Direction.NORTH, false));
				c.fill(x, G - 2, gz2 + 2 - k, x, G + k, gz2 + 2 - k, s(Blocks.STONE_BRICKS));
				c.fill(x, G + 2 + k, gz2 + 2 - k, x, G + 5, gz2 + 2 - k, s(Blocks.AIR));
			}
		}
		// Guard posts either side.
		for (int sx : new int[]{-12, 12}) {
			for (int x = sx - 2; x <= sx + 2; x++) {
				for (int z = gz2 - 3; z <= gz2 + 1; z++) {
					boolean w = Math.abs(x - sx) == 2 || z == gz2 - 3 || z == gz2 + 1;
					for (int y = G + 1; y <= F + 10; y++) {
						c.set(x, y, z, w ? wall(x, y, z) : s(Blocks.AIR));
					}
					c.set(x, F + 11, z, (x + z) % 2 == 0 ? s(Blocks.STONE_BRICKS) : Kit.slab(Blocks.STONE_BRICK_SLAB, false));
					c.fill(x, G - 2, z, x, G, z, s(Blocks.STONE_BRICKS));
				}
			}
			c.fill(sx, G + 1, gz2 + 1, sx, G + 2, gz2 + 1, s(Blocks.AIR));
			c.set(sx - 1, G + 1, gz2 - 2, lantern(false));
			c.set(sx + 1, G + 1, gz2 - 2, Canvas.facing(s(Blocks.BARREL), Direction.UP));
			c.set(sx, G + 4, gz2 + 1, Kit.connected(Blocks.IRON_BARS, true));
			c.set(sx, G + 8, gz2 + 2, s(Blocks.RED_WALL_BANNER).setValue(WallBannerBlock.FACING, Direction.SOUTH));
		}
		// Banners and the checkpoint.
		for (int x : new int[]{-6, -5, 5, 6}) {
			c.set(x, F + 7, gz2 + 1, s(Blocks.RED_WALL_BANNER).setValue(WallBannerBlock.FACING, Direction.SOUTH));
		}
		c.fill(2, F + 1, gz1 - 2, 3, F + 1, gz1 - 2, Kit.slab(Blocks.DARK_OAK_SLAB, true));
		c.set(3, F + 2, gz1 - 2, s(Blocks.LANTERN));
	}

	/** The four corner yards between the towers and the wings: paved, lit, with doors into the tower and the wings. */
	private void cornerYards(Canvas c) {
		for (int[] y : new int[][]{{-30, -45}, {28, -45}, {-30, 25}, {28, 25}}) {
			c.fill(y[0], F + 1, y[1], y[0], F + 3, y[1], s(Blocks.STONE_BRICK_WALL));
			c.set(y[0], F + 4, y[1], lantern(false));
			c.set(y[0] + 2, F + 1, y[1] + 2, Canvas.facing(s(Blocks.BARREL), Direction.UP));
		}
	}

	/** The north patrol yard outside the walls: a gravel yard with a watch post, crates and a low wall to the sea. */
	private void patrolYard(Canvas c) {
		int cz = Z1 - 9;
		if (!c.near(0, cz, 14)) {
			return;
		}
		for (int x = -12; x <= 12; x++) {
			for (int z = cz - 6; z <= cz + 5; z++) {
				if (c.inChunk(x, z) && c.ground(x, z) > Terrain.WATER_TOP + 1) {
					c.set(x, c.ground(x, z), z, unit(x, z, 51) < 0.6 ? s(Blocks.GRAVEL) : s(Blocks.COBBLESTONE));
				}
			}
		}
		for (int x = -12; x <= 12; x++) {
			int z = cz - 7;
			if (c.inChunk(x, z)) {
				int g = c.ground(x, z);
				c.set(x, g + 1, z, s(Blocks.COBBLESTONE_WALL));
			}
		}
		int g = c.ground(10, cz);
		c.fill(9, g + 1, cz - 1, 9, g + 3, cz - 1, s(Blocks.DARK_OAK_FENCE));
		c.fill(11, g + 1, cz + 1, 11, g + 3, cz + 1, s(Blocks.DARK_OAK_FENCE));
		c.fill(9, g + 4, cz - 1, 11, g + 4, cz + 1, Kit.slab(Blocks.STONE_BRICK_SLAB, false));
		c.set(10, g + 3, cz, lantern(true));
		c.set(-10, c.ground(-10, cz) + 1, cz, Canvas.facing(s(Blocks.BARREL), Direction.UP));
		c.set(-9, c.ground(-9, cz) + 1, cz, Canvas.facing(s(Blocks.BARREL), Direction.UP));
	}

	/**
	 * The rocky coast: boulder heaps of grey stone along the shore and a few sea stacks offshore, so the island reads as
	 * a crag rather than smooth terraces. Kept clear of the fortress, the forecourt and the pier.
	 */
	private void rocks(Canvas c) {
		for (int gx = -84; gx <= 84; gx += 6) {
			for (int gz = -84; gz <= 96; gz += 6) {
				int x = gx + (int) (unit(gx, gz, 61) * 5) - 2, z = gz + (int) (unit(gx, gz, 62) * 5) - 2;
				if (!c.near(x, z, 6) || (x >= X1 - 5 && x <= X2 + 5 && z >= Z1 - 5 && z <= Z2 + 9)
						|| (Math.abs(x) <= 16 && z >= Z2 && z <= DOCK_END_Z + 6) || (Math.abs(x) <= 14 && z >= Z1 - 16 && z <= Z1 - 2)) {
					continue;
				}
				int g = c.ground(x, z);
				double u = unit(x, z, 63);
				if (g >= Terrain.WATER_TOP - 1 && g <= G && u < 0.55) {
					boulder(c, x, z, 1.6 + u * 3.2, 2 + (int) (u * 6));
				} else if (g < Terrain.WATER_TOP - 1 && g >= Terrain.WATER_TOP - 7 && u < 0.07) {
					boulder(c, x, z, 2.0 + u * 10, Terrain.WATER_TOP - g + 4 + (int) (u * 40));
				}
			}
		}
	}

	/** A rounded heap of weathered stone, {@code h} tall over the ground (columns taper toward the edge). */
	private static void boulder(Canvas c, int x0, int z0, double r, int h) {
		int ri = (int) Math.ceil(r);
		for (int dx = -ri; dx <= ri; dx++) {
			for (int dz = -ri; dz <= ri; dz++) {
				double d = Math.sqrt(dx * dx + dz * dz) / r + (unit(x0 + dx, z0 + dz, 64) - 0.5) * 0.35;
				int x = x0 + dx, z = z0 + dz;
				if (d > 1.0 || !c.inChunk(x, z)) {
					continue;
				}
				int g = c.ground(x, z);
				int top = g + (int) Math.round(h * Math.sqrt(1 - d * d));
				for (int y = Math.min(g, Terrain.WATER_TOP - 6); y <= top; y++) {
					double v = unit(x * 7 + y, z * 3 - y, 65);
					c.set(x, y, z, v < 0.45 ? s(Blocks.STONE) : v < 0.65 ? s(Blocks.ANDESITE) : v < 0.8 ? s(Blocks.COBBLESTONE)
							: v < 0.9 ? s(Blocks.TUFF) : y >= top - 1 ? s(Blocks.MOSSY_COBBLESTONE) : s(Blocks.STONE));
				}
			}
		}
	}

	/** A short pier on stone footings with a guard checkpoint at the landing, cargo and lanterns. */
	private void dock(Canvas c) {
		if (!c.near(0, (DOCK_LAND_Z + DOCK_END_Z) / 2, 20)) {
			return;
		}
		for (int z = DOCK_LAND_Z; z <= DOCK_END_Z; z++) {
			boolean head = z >= DOCK_END_Z - 4;
			int half = head ? 6 : 2;
			for (int x = -half; x <= half; x++) {
				boolean edgeX = Math.abs(x) == half;
				c.set(x, DOCK_Y, z, s(Blocks.SPRUCE_PLANKS));
				c.fill(x, DOCK_Y + 1, z, x, DOCK_Y + 4, z, s(Blocks.AIR));
				if (edgeX && ((z - DOCK_LAND_Z) % 4 == 0 || z == DOCK_END_Z)) {
					c.fill(x, Math.min(c.ground(x, z), Terrain.WATER_TOP - 1), z, x, DOCK_Y - 1, z, s(Blocks.STONE_BRICKS));
					c.set(x, DOCK_Y + 1, z, s(Blocks.DARK_OAK_FENCE));
					if ((z - DOCK_LAND_Z) % 8 == 0) {
						c.set(x, DOCK_Y + 2, z, lantern(false));
					}
				} else if (edgeX && !head) {
					c.set(x, DOCK_Y + 1, z, Kit.connected(Blocks.DARK_OAK_FENCE, false));
				}
			}
		}
		c.set(-5, DOCK_Y + 1, DOCK_END_Z - 2, Canvas.facing(s(Blocks.BARREL), Direction.UP));
		c.set(-4, DOCK_Y + 1, DOCK_END_Z - 2, Canvas.facing(s(Blocks.BARREL), Direction.UP));
		// The checkpoint booth at the landing.
		int bz = DOCK_LAND_Z - 4;
		for (int[] p : new int[][]{{5, bz - 1}, {8, bz - 1}, {5, bz + 2}, {8, bz + 2}}) {
			int g = c.ground(p[0], p[1]);
			c.fill(p[0], g + 1, p[1], p[0], g + 3, p[1], s(Blocks.STONE_BRICK_WALL));
		}
		int g = c.ground(6, bz);
		c.fill(5, g + 4, bz - 1, 8, g + 4, bz + 2, Kit.slab(Blocks.STONE_BRICK_SLAB, false));
		c.set(6, g + 3, bz, lantern(true));
		c.set(7, g + 1, bz + 1, Kit.slab(Blocks.DARK_OAK_SLAB, true));
	}
}
