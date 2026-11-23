package com.grandline.world.plan;

import com.grandline.world.plan.Anchor.Type;
import com.grandline.world.plan.Terrain.Ground;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.WallBannerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Half;

/**
 * Gladiator's Ring, rebuilt: a grassy, rock-edged island about 160 x 145 blocks dominated by an oval colosseum.
 * <ul>
 * <li>The colosseum (92 x 80 outside, 30 blocks tall): a three-storey arcaded facade of stone brick with polished
 * andesite pilasters and smooth-stone cornices, a crenellated attic; inside, a sand fighting floor (45 x 35) behind a
 * podium wall, a lower and an upper tier of seating split by a walkway, aisles, a top promenade, an ambulatory and an
 * upper gallery inside the facade with stairs between them, four entry tunnels, banners and lanterns.</li>
 * <li>Under the south seating: the preparation room (with a stair up to the arena floor), a corridor of holding cells and
 * a pillared training hall, reached by a stair from the ambulatory.</li>
 * <li>East: the outdoor training grounds (walled courtyard, dummies, platforms, obstacles, racks). West: the ancient
 * warrior grounds (ruined columns, raised combat platforms, worn paving). Both are combat areas; quest bosses fight on
 * the arena floor.</li>
 * <li>South: a monumental stair and gate facing the dock, a plaza with two statues and the quest giver, a broad road to
 * a small settlement (barracks, supply shop, tavern, two houses) and the pier.</li>
 * </ul>
 * Island-local coordinates: +x east, +z south, sea level 62. Built entirely from code (identical in every world,
 * assembled chunk by chunk). Worlds created before this design keep the original ring ({@link FirstSeaPlans} legacy).
 */
final class GladiatorsRing extends BasePlan {
	/** General ground, and the colosseum plateau / arena floor level. */
	static final int G = 66, F = 69;

	// ---- the colosseum ----
	static final int CX = 0, CZ = -8;
	/** Arena floor semi-axes, outer facade semi-axes. */
	static final double AX = 22.5, AZ = 17.5, RX = 46, RZ = 40;
	/** Ring depth in blocks from the arena edge (0) to the outer facade (23). */
	static final int DEPTH = 24;
	static final int PODIUM_TOP = F + 4;               // 73
	static final int MID_WALK = 81, TOP_WALK = 87;     // walkway blocks (stand one above)
	static final int L2_FLOOR = 79;                    // upper gallery floor inside the facade
	static final int ATTIC = 94, TOP = 99;
	static final int BAYS = 56;
	/** Angle covered by one stair step (each flight has 18 steps, about one block apart along the ring). */
	static final double STAIR_STEP = 0.026;

	// ---- areas ----
	static final int PLAZA_Z = 41, GATE_Z = (int) (CZ + RZ);      // 32
	static final int TRAIN_X = 64, TRAIN_Z = -8;                  // training grounds (east)
	static final int RUINS_X = -64, RUINS_Z = -10;                // warrior grounds (west)
	static final int DOCK_LAND_Z = 63, DOCK_END_Z = 92, DOCK_Y = 64;
	static final int QUEST_X = 11, QUEST_Z = 38;

	static final double[] MAIN_ROAD = {0, 36, 0, 48, 0, 62};
	static final double[] EAST_ROAD = {12, 40, 30, 34, 44, 22, 52, 8, TRAIN_X - 12, TRAIN_Z};
	static final double[] WEST_ROAD = {-12, 40, -30, 34, -44, 22, -52, 6, RUINS_X + 10, RUINS_Z};
	static final double[] NORTH_LOOP = {TRAIN_X - 8, TRAIN_Z - 10, 40, -40, 0, -52, -40, -40, RUINS_X + 6, RUINS_Z - 10};

	/** A settlement building: wall box, floors, door side, use. */
	record House(int x1, int z1, int x2, int z2, int floors, Direction front, Use use) {
	}

	enum Use { BARRACKS, SHOP, TAVERN, HOUSE_A, HOUSE_B }

	static final House[] HOUSES = {
			new House(-28, 41, -16, 49, 1, Direction.EAST, Use.BARRACKS),
			new House(13, 42, 21, 48, 1, Direction.WEST, Use.SHOP),
			new House(12, 51, 22, 59, 2, Direction.WEST, Use.TAVERN),
			new House(-13, 52, -7, 58, 1, Direction.EAST, Use.HOUSE_A),
			new House(-27, 53, -20, 59, 1, Direction.NORTH, Use.HOUSE_B)};

	static final int[][] CYPRESSES = {{-36, 40}, {-34, 26}, {32, 28}, {36, 44}, {-50, 30}, {50, 28}, {-44, -44}, {46, -42}, {-8, -56},
			{14, -58}, {-70, 14}, {70, 14}, {28, 58}, {-36, 58}};
	static final int[][] OAKS = {{-58, 34}, {58, 36}, {-20, -58}, {30, -56}, {-74, -24}, {74, -28}};
	static final int[][] LAMPS = {{-3, 46}, {3, 46}, {-3, 54}, {3, 54}, {-3, 61}, {3, 61}, {22, 36}, {38, 28}, {50, 14}, {-22, 36},
			{-38, 28}, {-50, 12}, {-14, 37}, {14, 34}};

	GladiatorsRing() {
		super("Gladiator's Ring", 0.06);
		BlockState road = s(Blocks.STONE_BRICKS);
		BlockState path = s(Blocks.COBBLESTONE);
		BlockState gravel = s(Blocks.GRAVEL);
		terrain.mass(0, -4, 78, 70, 3.6, 4.0, Ground.GRASS)        // the island: broad grassy shoulders
				.mass(-52, -46, 22, 16, 4.0, 3.2, Ground.STONE)    // rocky headland (north-west)
				.mass(56, -44, 20, 14, 3.8, 3.2, Ground.STONE)     // rocky headland (north-east)
				.mass(66, 34, 15, 12, 3.4, 3.2, Ground.STONE)      // rocks on the south-east shore
				.mass(-68, 30, 15, 12, 3.2, 3.2, Ground.STONE)     // rocks on the south-west shore
				.mass(-34, 40, 14, 9, 4.6, 2.6, Ground.GRASS)      // a soft rise behind the barracks
				.flat(CX, CZ, RX + 6, RZ + 6, F, 6, null)           // the colosseum plateau
				.flat(0, PLAZA_Z + 2, 15, 8, G, 3, null)             // the entrance plaza
				.flat(0, 52, 26, 10, G, 5, null)                     // the settlement
				.flat(TRAIN_X, TRAIN_Z, 14, 14, 67, 5, null)
				.flat(RUINS_X, RUINS_Z, 13, 13, 67, 5, null)
				.flat(0, 61, 6, 3, 65, 3, s(Blocks.STONE_BRICKS))   // the arrival landing
				.path(road, 5, MAIN_ROAD).path(path, 3, EAST_ROAD).path(path, 3, WEST_ROAD).path(gravel, 2.4, NORTH_LOOP)
				.shoreMix(s(Blocks.STONE), s(Blocks.COBBLESTONE), s(Blocks.GRAVEL), s(Blocks.ANDESITE), s(Blocks.STONE))
				.reach(DOCK_END_Z + 4);
		anchor(Anchor.at(Type.QUEST, QUEST_X, G + 1, QUEST_Z, 90));                       // on the plaza, by the gate
		anchor(new Anchor(Type.ARRIVAL, 0, Anchor.SURFACE, DOCK_LAND_Z - 3, 0, 180));     // the landing, facing the colosseum
		anchor(Anchor.at(Type.HOME, 8, G + 1, 55, 90));                                   // the Innkeeper, by the tavern door
		anchor(Anchor.at(Type.FISHERMAN, 5, DOCK_Y + 1, DOCK_END_Z - 2, 0));
		anchor(Anchor.at(Type.DOCK, -8, 63, DOCK_END_Z - 3));
		anchor(Anchor.area(Type.ENEMY_CAMP, TRAIN_X, TRAIN_Z, 8));                         // the training grounds
		anchor(Anchor.area(Type.ENEMY_CAMP, RUINS_X, RUINS_Z, 8));                         // the warrior grounds
		anchor(Anchor.at(Type.BOSS_ARENA, CX, F, CZ - 2));                           // quest bosses fight on the arena floor
		anchor(Anchor.of(Type.TREASURE, -46, 50));
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

	static BlockState stair(Block b, Direction facing, boolean top) {
		return Kit.stairs(b, facing, top);
	}

	static BlockState lantern(boolean hanging) {
		return s(Blocks.LANTERN).setValue(LanternBlock.HANGING, hanging);
	}

	/** Weathered stone brick: mostly plain, some cracked and mossy. */
	static BlockState aged(int x, int y, int z) {
		double u = unit(x * 7 + y, z, 3);
		return u < 0.12 ? s(Blocks.CRACKED_STONE_BRICKS) : u < 0.2 ? s(Blocks.MOSSY_STONE_BRICKS) : s(Blocks.STONE_BRICKS);
	}

	/**
	 * Ring position of a column: t = 0 on the arena edge, 1 on the outer facade, interpolating the two ellipses (so every
	 * ring is an ellipse); negative inside the arena. Solved by bisection.
	 */
	static double ringT(int x, int z) {
		double px = x - CX, pz = z - CZ;
		double lo = -1.0, hi = 1.6;
		for (int i = 0; i < 22; i++) {
			double t = (lo + hi) / 2;
			double a = AX + (RX - AX) * t, b = AZ + (RZ - AZ) * t;
			double v = (px / a) * (px / a) + (pz / b) * (pz / b);
			if (v > 1) {
				lo = t;
			} else {
				hi = t;
			}
		}
		return (lo + hi) / 2;
	}

	/** Ring depth in whole blocks (0 = first block outside the arena floor). */
	static int depth(int x, int z) {
		return (int) Math.floor(ringT(x, z) * DEPTH);
	}

	/** Angle around the colosseum in [0, 1) (0 = east, 0.25 = south). */
	static double around(int x, int z) {
		double a = Math.atan2((z - CZ) / RZ, (x - CX) / RX);
		return (a / (Math.PI * 2) + 1) % 1.0;
	}

	/** Direction from a column toward the arena centre (for stairs and banners). */
	static Direction inward(int x, int z) {
		double dx = (x - CX) / RX, dz = (z - CZ) / RZ;
		return Math.abs(dx) > Math.abs(dz) ? (dx > 0 ? Direction.WEST : Direction.EAST) : (dz > 0 ? Direction.NORTH : Direction.SOUTH);
	}

	/** Columns where the four entry tunnels run (half-width 3 on the south main axis, 1 elsewhere). */
	static boolean tunnel(int x, int z) {
		int px = x - CX, pz = z - CZ;
		return (pz > 0 && Math.abs(px) <= 3) || (pz < 0 && Math.abs(px) <= 1) || (Math.abs(pz) <= 1 && px != 0);
	}

	/** Columns of the radial seating aisles (every 7 bays, offset from the tunnels). */
	static boolean aisle(int x, int z) {
		double a = around(x, z) * BAYS;
		double frac = a - Math.floor(a);
		int bay = (int) Math.floor(a);
		return bay % 7 == 3 && frac < 0.34;
	}

	static boolean inColosseum(int x, int z, int margin) {
		double px = (x - CX) / (RX + margin), pz = (z - CZ) / (RZ + margin);
		return px * px + pz * pz <= 1;
	}

	/** Areas kept clear of plants and rocks. */
	static boolean built(int x, int z) {
		if (inColosseum(x, z, 6)) {
			return true;
		}
		for (House h : HOUSES) {
			if (x >= h.x1() - 3 && x <= h.x2() + 3 && z >= h.z1() - 3 && z <= h.z2() + 3) {
				return true;
			}
		}
		return near(x, z, TRAIN_X, TRAIN_Z, 16) || near(x, z, RUINS_X, RUINS_Z, 15) || (Math.abs(x) <= 16 && z >= 32 && z <= 48)
				|| (Math.abs(x) <= 8 && z >= 46);
	}

	// ==================================================================================================== build

	@Override
	public void build(Canvas c) {
		groundCover(c);
		if (c.near(CX, CZ, (int) RX + 4)) {
			colosseum(c);
			underground(c);
		}
		entrance(c);
		dock(c);
		for (House h : HOUSES) {
			if (c.near((h.x1() + h.x2()) / 2, (h.z1() + h.z2()) / 2, 14)) {
				house(c, h);
			}
		}
		if (c.near(TRAIN_X, TRAIN_Z, 18)) {
			trainingGrounds(c);
		}
		if (c.near(RUINS_X, RUINS_Z, 18)) {
			warriorGrounds(c);
		}
		ruins(c);
		for (int[] t : CYPRESSES) {
			if (c.near(t[0], t[1], 3)) {
				cypress(c, t[0], t[1], 8 + (int) (unit(t[0], t[1], 5) * 4));
			}
		}
		for (int[] t : OAKS) {
			if (c.near(t[0], t[1], 5)) {
				Kit.roundTree(c, t[0], t[1], 5 + (int) (unit(t[0], t[1], 6) * 2), Blocks.OAK_LOG, Blocks.OAK_LEAVES);
			}
		}
		for (int[] l : LAMPS) {
			if (c.inChunk(l[0], l[1])) {
				int g = c.ground(l[0], l[1]);
				c.set(l[0], g + 1, l[1], s(Blocks.STONE_BRICK_WALL));
				c.set(l[0], g + 2, l[1], s(Blocks.STONE_BRICK_WALL));
				c.set(l[0], g + 3, l[1], s(Blocks.DARK_OAK_FENCE));
				c.set(l[0], g + 4, l[1], lantern(false));
			}
		}
		treasure(c);
	}

	// ---------------------------------------------------------------------------------------------------- ground

	/** Grass with exposed stone and cobble patches, worn road edges, short grass and a few flowers. */
	private void groundCover(Canvas c) {
		for (int x = -84; x <= 84; x++) {
			for (int z = -80; z <= 80; z++) {
				if (!c.inChunk(x, z)) {
					continue;
				}
				int g = c.ground(x, z);
				if (g <= Terrain.WATER_TOP + 1 || built(x, z)) {
					continue;
				}
				BlockState paved = c.terrain().paving(x, z);
				double r = unit(x, z, 7);
				if (paved != null) {
					if (paved.getBlock() == Blocks.STONE_BRICKS) {
						c.set(x, g, z, r < 0.12 ? s(Blocks.CRACKED_STONE_BRICKS) : r < 0.2 ? s(Blocks.MOSSY_STONE_BRICKS) : paved);
					} else if (paved.getBlock() == Blocks.COBBLESTONE && r < 0.2) {
						c.set(x, g, z, r < 0.1 ? s(Blocks.MOSSY_COBBLESTONE) : s(Blocks.STONE_BRICKS));
					}
					continue;
				}
				if (c.terrain().top(x, z).getBlock() != Blocks.GRASS_BLOCK) {
					continue;
				}
				double patch = (Math.sin(x * 0.17 + 1.1) + Math.sin(z * 0.21 + 0.3) + Math.sin((x + z) * 0.09)) / 3.0;
				if (patch > 0.62) {
					c.set(x, g, z, r < 0.4 ? s(Blocks.STONE) : r < 0.7 ? s(Blocks.COBBLESTONE) : r < 0.85 ? s(Blocks.ANDESITE) : s(Blocks.MOSSY_COBBLESTONE));
				} else if (patch > 0.55 && r < 0.5) {
					c.set(x, g, z, s(Blocks.COARSE_DIRT));
				} else if (r > 0.86) {
					c.set(x, g + 1, z, s(Blocks.GRASS));
				} else if (r < 0.012) {
					c.set(x, g + 1, z, s(r < 0.006 ? Blocks.POPPY : Blocks.CORNFLOWER));
				}
			}
		}
	}

	/** A Mediterranean cypress: a tall, narrow column of spruce leaves on a short trunk. */
	static void cypress(Canvas c, int x, int z, int h) {
		int g = c.ground(x, z);
		if (g <= Terrain.WATER_TOP + 1) {
			return;
		}
		c.fill(x, g + 1, z, x, g + h - 1, z, s(Blocks.SPRUCE_LOG));
		BlockState leaf = Kit.leaves(Blocks.SPRUCE_LEAVES);
		for (int y = 2; y <= h + 1; y++) {
			int r = y <= 3 || y >= h ? 0 : 1;
			for (int dx = -r; dx <= r; dx++) {
				for (int dz = -r; dz <= r; dz++) {
					if ((dx != 0 || dz != 0) && (Math.abs(dx) + Math.abs(dz) <= 1 || (y > 4 && y < h - 1))) {
						c.set(x + dx, g + y, z + dz, leaf);
					}
				}
			}
			if (y > h - 1) {
				c.set(x, g + y, z, leaf);
			}
		}
	}

	// ---------------------------------------------------------------------------------------------------- colosseum

	/**
	 * The colosseum, column by column by its ring depth:
	 * <pre>
	 *  d < 0       arena floor (sand), open sky
	 *  d 0..1      podium wall to y 73, railing on top
	 *  d 2..9      lower seating: a stair row per block, rising 1 per row (y 74..81)
	 *  d 10..11    mid walkway (y 81)
	 *  d 12..17    upper seating (y 82..87)
	 *  d 18..21    ambulatory (ground), upper gallery (floor 79), top promenade (87)
	 *  d 22        facade wall: three storeys of arches between pilasters, attic, parapet (top y 99)
	 *  d 23        pilasters (polished andesite) and cornices
	 * </pre>
	 */
	private void colosseum(Canvas c) {
		for (int x = CX - (int) RX - 2; x <= CX + (int) RX + 2; x++) {
			for (int z = CZ - (int) RZ - 2; z <= CZ + (int) RZ + 2; z++) {
				if (!c.inChunk(x, z)) {
					continue;
				}
				int d = depth(x, z);
				if (d > 23) {
					continue;
				}
				Direction in = inward(x, z);
				Direction out = in.getOpposite();
				double a = around(x, z) * BAYS;
				int bay = (int) Math.floor(a);
				double frac = a - bay;
				boolean tun = tunnel(x, z);
				if (d < 0) {
					arenaFloor(c, x, z);
					continue;
				}
				// Foundation under every ring column.
				c.fill(x, F - 6, z, x, F, z, s(Blocks.STONE_BRICKS));
				if (d <= 1) {
					c.fill(x, F + 1, z, x, PODIUM_TOP, z, d == 0 ? aged(x, 70, z) : s(Blocks.STONE_BRICKS));
					if (d == 0) {
						c.set(x, PODIUM_TOP - 1, z, s(Blocks.CHISELED_STONE_BRICKS).getBlock() == Blocks.CHISELED_STONE_BRICKS && bay % 4 == 0
								? s(Blocks.CHISELED_STONE_BRICKS) : aged(x, 72, z));
						c.set(x, PODIUM_TOP + 1, z, s(Blocks.STONE_BRICK_WALL));
						if (bay % 6 == 0 && frac < 0.25) {
							c.set(x, PODIUM_TOP + 2, z, lantern(false));
						}
						if (bay % 4 == 2 && frac > 0.4 && frac < 0.7) {
							c.set(x + in.getStepX(), PODIUM_TOP - 1, z + in.getStepZ(),
									s(Blocks.RED_WALL_BANNER).setValue(WallBannerBlock.FACING, in));
						}
					} else {
						c.set(x, PODIUM_TOP, z, s(Blocks.SMOOTH_STONE));
					}
				} else if (d <= 17) {
					int seat = d <= 9 ? 72 + d : d <= 11 ? MID_WALK : 70 + d;
					c.fill(x, F + 1, z, x, seat - 1, z, s(Blocks.STONE));
					boolean walk = d == 10 || d == 11;
					boolean ais = aisle(x, z);
					if (walk) {
						c.set(x, seat, z, s(Blocks.POLISHED_ANDESITE));
						if (d == 10 && !ais) {
							c.set(x, seat + 1, z, Kit.slab(Blocks.SMOOTH_STONE_SLAB, false)); // a low kerb
						}
					} else if (ais) {
						c.set(x, seat, z, stair(Blocks.STONE_BRICK_STAIRS, out, false));
					} else {
						c.set(x, seat, z, stair(d % 2 == 0 ? Blocks.SMOOTH_SANDSTONE_STAIRS : Blocks.STONE_BRICK_STAIRS, out, false));
					}
					c.fill(x, seat + 1, z, x, TOP + 2, z, s(Blocks.AIR));
					if (walk && d == 11 && bay % 6 == 0 && frac < 0.25) {
						c.set(x, seat + 1, z, lantern(false));
					}
				} else if (d <= 21) {
					// Ambulatory (ground), upper gallery (floor 79) and the top promenade (87).
					c.set(x, F, z, s(Blocks.POLISHED_ANDESITE));
					c.fill(x, F + 1, z, x, L2_FLOOR - 1, z, s(Blocks.AIR));
					c.set(x, L2_FLOOR, z, s(Blocks.STONE_BRICKS));
					c.fill(x, L2_FLOOR + 1, z, x, TOP_WALK - 1, z, s(Blocks.AIR));
					c.set(x, TOP_WALK, z, s(Blocks.POLISHED_ANDESITE));
					c.fill(x, TOP_WALK + 1, z, x, TOP + 2, z, s(Blocks.AIR));
					if (d == 18) {
						// The inner wall of the galleries (the back of the seating), with recessed arches.
						c.fill(x, F + 1, z, x, L2_FLOOR - 1, z, aged(x, 74, z));
						c.fill(x, L2_FLOOR + 1, z, x, TOP_WALK - 1, z, aged(x, 83, z));
						if (frac > 0.35 && frac < 0.75) {
							c.set(x, F + 3, z, s(Blocks.AIR));
							c.set(x, F + 4, z, s(Blocks.AIR));
						}
						c.set(x, TOP_WALK + 1, z, s(Blocks.STONE_BRICK_WALL)); // railing above the top seats
					}
					if (d == 20 && bay % 4 == 1 && frac > 0.4 && frac < 0.6) {
						c.set(x, L2_FLOOR - 1, z, lantern(true));
						c.set(x, TOP_WALK - 1, z, lantern(true));
					}
				} else if (d == 22) {
					facadeWall(c, x, z, frac, bay, in);
				} else {
					pilaster(c, x, z, frac);
				}
				// Entry tunnels through the podium and seating at ground level.
				if (tun && d <= 21) {
					int h = (z - CZ > 0 && Math.abs(x - CX) <= 3) ? 4 : 3;
					c.fill(x, F + 1, z, x, F + h, z, s(Blocks.AIR));
					c.set(x, F, z, s(Blocks.POLISHED_ANDESITE));
					if (d % 5 == 2 && Math.abs(x - CX) == 3) {
						c.set(x, F + h, z, lantern(true));
					}
				}
			}
		}
		galleryStairs(c);
	}

	/** The fighting floor: sand with a ring of coarse sand-coloured stone, a central disc, and four drains. */
	private static void arenaFloor(Canvas c, int x, int z) {
		double px = (x - CX) / AX, pz = (z - CZ) / AZ;
		double e = Math.sqrt(px * px + pz * pz);
		c.fill(x, F - 6, z, x, F - 1, z, s(Blocks.STONE));
		BlockState top = e > 0.94 ? s(Blocks.SMOOTH_SANDSTONE) : Math.abs(e - 0.45) < 0.035 ? s(Blocks.CUT_SANDSTONE) : s(Blocks.SAND);
		c.set(x, F, z, top);
		c.fill(x, F + 1, z, x, TOP + 2, z, s(Blocks.AIR));
	}

	/** One facade column: storeys of arches, cornice bands, the attic and the crenellated parapet. */
	private static void facadeWall(Canvas c, int x, int z, double frac, int bay, Direction in) {
		boolean pierZone = frac < 0.2 || frac > 0.86;
		boolean gate = (z - CZ) > 0 && Math.abs(x - CX) <= 4;
		for (int y = F - 6; y <= TOP; y++) {
			BlockState b = aged(x, y, z);
			if (y == F + 9 || y == L2_FLOOR + 8 || y == ATTIC - 1) {
				b = s(Blocks.SMOOTH_STONE); // cornice bands
			}
			boolean open = false;
			if (!pierZone) {
				// Arch openings: storey 1 (y 70-76, round top), storey 2 (80-85), storey 3 (88-92).
				double m = Math.abs(frac - 0.53);
				open = (y >= F + 1 && y <= F + 6) || (y == F + 7 && m < 0.2)
						|| (y >= L2_FLOOR + 1 && y <= L2_FLOOR + 5) || (y == L2_FLOOR + 6 && m < 0.2)
						|| (y >= TOP_WALK + 1 && y <= TOP_WALK + 4) || (y == TOP_WALK + 5 && m < 0.18);
			}
			if (gate && y >= F + 1 && y <= F + 8) {
				open = true; // the main gate
			}
			if (y > TOP - 2 && y < TOP) {
				b = s(Blocks.STONE_BRICKS);
			}
			if (y == TOP) {
				b = bay % 2 == 0 && frac < 0.5 ? s(Blocks.STONE_BRICKS) : Kit.slab(Blocks.STONE_BRICK_SLAB, false);
			}
			c.set(x, y, z, open ? s(Blocks.AIR) : b);
		}
		if (!pierZone) {
			// Railings in the upper arches, keystones above the lower ones.
			c.set(x, L2_FLOOR + 1, z, s(Blocks.STONE_BRICK_WALL));
			c.set(x, TOP_WALK + 1, z, s(Blocks.STONE_BRICK_WALL));
		}
		if (bay % 8 == 4 && frac > 0.48 && frac < 0.62 && !gate) {
			c.set(x, ATTIC + 1, z, s(Blocks.CHISELED_STONE_BRICKS));
		}
	}

	/** Pilaster column on the facade's face (between arches) and the cornice ledges. */
	private static void pilaster(Canvas c, int x, int z, double frac) {
		boolean column = frac < 0.16 || frac > 0.9;
		boolean gate = (z - CZ) > 0 && Math.abs(x - CX) <= 4;
		if (gate) {
			return;
		}
		c.fill(x, F - 6, z, x, F, z, s(Blocks.STONE_BRICKS));
		if (column) {
			c.set(x, F + 1, z, s(Blocks.STONE_BRICKS)); // plinth
			c.fill(x, F + 2, z, x, ATTIC - 2, z, s(Blocks.POLISHED_ANDESITE));
			c.set(x, F + 8, z, s(Blocks.CHISELED_STONE_BRICKS));          // capitals
			c.set(x, L2_FLOOR + 7, z, s(Blocks.CHISELED_STONE_BRICKS));
			c.set(x, TOP_WALK + 6, z, s(Blocks.CHISELED_STONE_BRICKS));
		}
		// Cornice ledges along the whole face.
		c.set(x, F + 9, z, Kit.slab(Blocks.SMOOTH_STONE_SLAB, false));
		c.set(x, L2_FLOOR + 8, z, Kit.slab(Blocks.SMOOTH_STONE_SLAB, false));
		c.set(x, ATTIC - 1, z, Kit.slab(Blocks.SMOOTH_STONE_SLAB, false));
	}

	/**
	 * Stairs inside the facade: at the four diagonals, one flight along the ring from the ambulatory, past the upper
	 * gallery (level with step 9), to the top promenade.
	 */
	private void galleryStairs(Canvas c) {
		for (int x = CX - (int) RX; x <= CX + (int) RX; x++) {
			for (int z = CZ - (int) RZ; z <= CZ + (int) RZ; z++) {
				if (!c.inChunk(x, z)) {
					continue;
				}
				int d = depth(x, z);
				if (d < 18 || d > 19) {
					continue; // the two lanes against the inner wall; depth 20-21 stays a passage
				}
				double ang = Math.atan2(z - CZ, x - CX);
				if (ang < 0) {
					ang += Math.PI * 2;
				}
				for (double quarter : new double[]{0.125, 0.375, 0.625, 0.875}) {
					double rel = ang - quarter * Math.PI * 2;
					int step = (int) Math.floor(rel / STAIR_STEP);
					if (rel < 0 || step >= 18) {
						continue;
					}
					// One continuous flight: y 70 (ground) .. 79 (level with the gallery floor) .. 87 (the top promenade).
					// Clearing three blocks above every step opens the stairwell through both floors.
					int y = F + 1 + step;
					double tx = -Math.sin(ang), tz = Math.cos(ang); // the way up: increasing angle
					Direction up = Math.abs(tx) >= Math.abs(tz) ? (tx > 0 ? Direction.EAST : Direction.WEST) : (tz > 0 ? Direction.SOUTH : Direction.NORTH);
					c.fill(x, F + 1, z, x, y - 1, z, s(Blocks.STONE_BRICKS));
					c.set(x, y, z, stair(Blocks.STONE_BRICK_STAIRS, up, false));
					c.fill(x, y + 1, z, x, y + 3, z, s(Blocks.AIR));
				}
			}
		}
	}

	// ---------------------------------------------------------------------------------------------------- below

	/**
	 * Underground chambers below the south seating (floor y 59, 5 high): the preparation room in the middle with a stair
	 * up to the arena floor, holding cells along a corridor to the west, the training hall to the east, and a stair down
	 * from the ambulatory beside the main tunnel.
	 */
	private void underground(Canvas c) {
		int fy = 59, top = 64;
		int pz1 = CZ + 22, pz2 = CZ + 30;   // prep room z 14..22
		// Preparation room (x -7..7).
		room(c, -7, pz1, 7, pz2, fy, top);
		for (int x = -6; x <= 6; x += 4) {
			c.fill(x, fy + 1, pz1, x, fy + 1, pz1, Canvas.facing(s(Blocks.BARREL), Direction.UP));
		}
		c.set(-6, fy + 1, pz2, s(Blocks.SMITHING_TABLE));
		c.set(-5, fy + 1, pz2, s(Blocks.GRINDSTONE));
		c.set(-4, fy + 1, pz2, s(Blocks.ANVIL));
		for (int x = -2; x <= 2; x++) {
			c.set(x, fy + 1, pz2, stair(Blocks.DARK_OAK_STAIRS, Direction.SOUTH, false)); // seating
		}
		weaponRack(c, 6, fy + 1, pz2 - 2, Direction.WEST);
		c.set(5, fy + 1, pz2, Canvas.facing(s(Blocks.BARREL), Direction.UP));
		c.set(5, fy + 2, pz2, Canvas.facing(s(Blocks.BARREL), Direction.UP));
		// Stair up to the arena: 3 wide at x 8..10, from z 18 north to the arena floor, through a gate in the podium.
		for (int i = 0; i <= 10; i++) {
			int z = pz1 + 4 - i;
			int y = fy + 1 + i;
			for (int x = 8; x <= 10; x++) {
				c.fill(x, fy, z, x, y - 1, z, s(Blocks.STONE_BRICKS));
				c.set(x, y, z, i < 10 ? stair(Blocks.STONE_BRICK_STAIRS, Direction.NORTH, false) : s(Blocks.POLISHED_ANDESITE));
				c.fill(x, y + 1, z, x, y + 3, z, s(Blocks.AIR));
			}
			c.fill(7, y + 1, z, 7, y + 2, z, s(Blocks.STONE_BRICKS));
			c.fill(11, y + 1, z, 11, y + 2, z, s(Blocks.STONE_BRICKS));
			if (i % 4 == 2) {
				c.set(11, y + 2, z, s(Blocks.WALL_TORCH).setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST));
			}
		}
		c.fill(7, fy + 1, pz1, 7, fy + 3, pz1 + 4, s(Blocks.AIR)); // from the prep room into the stair
		// Holding cells corridor (west): z 17..19, x -8..-30, cells to the north and south.
		room(c, -30, pz1 + 3, -8, pz1 + 5, fy, top);
		c.fill(-8, fy + 1, pz1 + 3, -7, fy + 3, pz1 + 5, s(Blocks.AIR));
		for (int i = 0; i < 5; i++) {
			int x1 = -12 - i * 4;
			cell(c, x1, pz1, fy, true);
			cell(c, x1, pz1 + 6, fy, false);
		}
		// Training hall (east): x 12..30, z 14..24.
		room(c, 12, pz1, 30, pz1 + 10, fy, top + 1);
		c.fill(11, fy + 1, pz1 + 6, 11, fy + 3, pz1 + 8, s(Blocks.AIR));
		c.fill(8, fy + 1, pz1 + 6, 10, fy + 3, pz1 + 8, s(Blocks.AIR));
		c.fill(8, fy, pz1 + 6, 10, fy, pz1 + 8, s(Blocks.STONE_BRICKS));
		for (int x : new int[]{16, 21, 26}) {
			for (int z : new int[]{pz1 + 2, pz1 + 8}) {
				c.fill(x, fy + 1, z, x, top + 1, z, s(Blocks.POLISHED_ANDESITE));
				c.set(x, fy + 1, z, s(Blocks.CHISELED_STONE_BRICKS));
			}
		}
		for (int x : new int[]{18, 23, 28}) {
			dummy(c, x, fy + 1, pz1 + 5);
		}
		c.set(30, fy + 2, pz1 + 3, s(Blocks.TARGET));
		c.set(30, fy + 2, pz1 + 7, s(Blocks.TARGET));
		weaponRack(c, 13, fy + 1, pz1 + 1, Direction.SOUTH);
		weaponRack(c, 13, fy + 1, pz1 + 9, Direction.NORTH);
		c.set(29, fy + 1, pz1 + 9, Canvas.facing(s(Blocks.BARREL), Direction.UP));
		// Stair down from the ambulatory (west of the main tunnel) to the prep room.
		int sz = CZ + 33;
		for (int i = 0; i <= 10; i++) {
			int z = sz - i;
			int y = F - i;
			for (int x = -6; x <= -5; x++) {
				c.set(x, y, z, stair(Blocks.STONE_BRICK_STAIRS, Direction.SOUTH, false));
				c.fill(x, y + 1, z, x, y + 3, z, s(Blocks.AIR));
			}
			if (i % 4 == 1) {
				c.set(-7, y + 2, z, s(Blocks.WALL_TORCH).setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST));
			}
		}
		c.fill(-6, fy + 1, pz1, -5, fy + 3, pz1 + 2, s(Blocks.AIR));
	}

	/** A stone-walled room with a floor, walls, a ceiling and hanging lanterns. */
	private static void room(Canvas c, int x1, int z1, int x2, int z2, int floorY, int topY) {
		for (int x = x1 - 1; x <= x2 + 1; x++) {
			for (int z = z1 - 1; z <= z2 + 1; z++) {
				boolean wall = x < x1 || x > x2 || z < z1 || z > z2;
				c.set(x, floorY, z, s(Blocks.STONE_BRICKS));
				for (int y = floorY + 1; y < topY; y++) {
					c.set(x, y, z, wall ? aged(x, y, z) : s(Blocks.AIR));
				}
				c.set(x, topY, z, s(Blocks.STONE_BRICKS));
				if (!wall && (x - x1) % 5 == 2 && (z - z1) % 5 == 2) {
					c.set(x, topY - 1, z, lantern(true));
				}
			}
		}
	}

	/** A holding cell (3 x 3 inside) off the corridor: iron-bar front with a gap, a bench, a bed of hay. */
	private static void cell(Canvas c, int x1, int z1, int fy, boolean north) {
		int front = north ? z1 + 2 : z1;
		int back = north ? z1 : z1 + 2;
		for (int x = x1 - 3; x <= x1; x++) {
			for (int z = Math.min(front, back); z <= Math.max(front, back); z++) {
				boolean side = x == x1 - 3 || x == x1;
				for (int y = fy + 1; y <= fy + 3; y++) {
					if (side) {
						c.set(x, y, z, aged(x, y, z));
					} else if (z == front) {
						c.set(x, y, z, x == x1 - 2 && y <= fy + 2 ? s(Blocks.AIR) : Kit.connected(Blocks.IRON_BARS, true));
					} else {
						c.set(x, y, z, s(Blocks.AIR));
					}
				}
			}
		}
		c.set(x1 - 1, fy + 1, back, stair(Blocks.DARK_OAK_STAIRS, north ? Direction.NORTH : Direction.SOUTH, false));
		c.set(x1 - 2, fy + 1, back, s(Blocks.HAY_BLOCK));
		c.set(x1 - 1, fy + 3, back, s(Blocks.IRON_BARS));
	}

	/** A training dummy: a fence post, a hay body, a carved pumpkin head. */
	private static void dummy(Canvas c, int x, int y, int z) {
		c.set(x, y, z, s(Blocks.DARK_OAK_FENCE));
		c.set(x, y + 1, z, s(Blocks.HAY_BLOCK));
		c.set(x, y + 2, z, Canvas.facing(s(Blocks.CARVED_PUMPKIN), Direction.WEST));
	}

	/** A weapon rack: two posts, a shelf, a grindstone and a stack of "spears" (lightning rods). */
	private static void weaponRack(Canvas c, int x, int y, int z, Direction along) {
		Direction side = along.getAxis() == Direction.Axis.X ? Direction.SOUTH : Direction.EAST;
		int sx = side.getStepX(), sz = side.getStepZ();
		c.fill(x, y, z, x, y + 1, z, s(Blocks.DARK_OAK_FENCE));
		c.fill(x + sx * 2, y, z + sz * 2, x + sx * 2, y + 1, z + sz * 2, s(Blocks.DARK_OAK_FENCE));
		c.set(x + sx, y + 1, z + sz, Kit.slab(Blocks.DARK_OAK_SLAB, true));
		c.set(x + sx, y, z + sz, s(Blocks.LIGHTNING_ROD));
	}

	// ---------------------------------------------------------------------------------------------------- entrance

	/**
	 * The monumental south entrance: a wide stair from the plaza up to the plateau, a portico of four columns with a
	 * pediment in front of the gate, banners, two statues and benches on the plaza, and the quest giver's post.
	 */
	private void entrance(Canvas c) {
		if (!c.near(0, PLAZA_Z - 4, 22)) {
			return;
		}
		// Plaza paving (stone brick with an andesite border).
		for (int x = -14; x <= 14; x++) {
			for (int z = PLAZA_Z - 5; z <= PLAZA_Z + 7; z++) {
				boolean border = Math.abs(x) == 14 || z == PLAZA_Z + 7;
				c.set(x, G, z, border ? s(Blocks.POLISHED_ANDESITE) : aged(x, G, z));
				c.fill(x, G + 1, z, x, G + 6, z, s(Blocks.AIR));
			}
		}
		// The stair: 3 steps, 17 wide, from the plaza (66) to the plateau (69).
		for (int i = 0; i < 3; i++) {
			int z = GATE_Z + 3 - i, y = G + 1 + i;
			for (int x = -8; x <= 8; x++) {
				c.fill(x, G, z, x, y - 1, z, s(Blocks.STONE_BRICKS));
				c.set(x, y, z, stair(Blocks.STONE_BRICK_STAIRS, Direction.NORTH, false));
				c.fill(x, y + 1, z, x, y + 9, z, s(Blocks.AIR));
			}
			c.fill(-9, G, z, -9, F, z, s(Blocks.STONE_BRICKS));
			c.fill(9, G, z, 9, F, z, s(Blocks.STONE_BRICKS));
		}
		for (int x = -8; x <= 8; x++) {
			c.set(x, F, GATE_Z + 1, s(Blocks.POLISHED_ANDESITE));
			c.fill(x, F + 1, GATE_Z + 1, x, F + 10, GATE_Z + 1, s(Blocks.AIR));
		}
		// Portico: four columns, an entablature and a stepped pediment.
		for (int x : new int[]{-7, -4, 4, 7}) {
			c.set(x, F + 1, GATE_Z + 1, s(Blocks.CHISELED_STONE_BRICKS));
			c.fill(x, F + 2, GATE_Z + 1, x, F + 9, GATE_Z + 1, s(Blocks.QUARTZ_PILLAR));
			c.set(x, F + 10, GATE_Z + 1, s(Blocks.CHISELED_STONE_BRICKS));
		}
		for (int x = -8; x <= 8; x++) {
			c.set(x, F + 11, GATE_Z + 1, s(Blocks.SMOOTH_STONE));
			c.set(x, F + 11, GATE_Z, s(Blocks.SMOOTH_STONE));
			int ped = 8 - Math.abs(x);
			for (int k = 0; k < ped / 2 + 1 && k < 4; k++) {
				c.set(x, F + 12 + k, GATE_Z + 1, k == ped / 2 || k == 3 ? Kit.slab(Blocks.STONE_BRICK_SLAB, false) : s(Blocks.STONE_BRICKS));
			}
		}
		// Banners hanging under the entablature and beside the gate.
		for (int x : new int[]{-6, -5, 5, 6}) {
			c.set(x, F + 10, GATE_Z + 2, s(Blocks.RED_WALL_BANNER).setValue(WallBannerBlock.FACING, Direction.SOUTH));
		}
		// Statues flanking the stair.
		statue(c, -12, PLAZA_Z - 3);
		statue(c, 12, PLAZA_Z - 3);
		// Benches.
		for (int x : new int[]{-9, -8, 8, 9}) {
			c.set(x, G + 1, PLAZA_Z + 5, stair(Blocks.STONE_BRICK_STAIRS, Direction.SOUTH, false));
		}
		// The quest giver's post: a small awning on posts with a lectern.
		c.fill(QUEST_X + 2, G + 1, QUEST_Z - 1, QUEST_X + 2, G + 3, QUEST_Z - 1, s(Blocks.DARK_OAK_FENCE));
		c.fill(QUEST_X + 2, G + 1, QUEST_Z + 1, QUEST_X + 2, G + 3, QUEST_Z + 1, s(Blocks.DARK_OAK_FENCE));
		c.fill(QUEST_X + 1, G + 4, QUEST_Z - 1, QUEST_X + 2, G + 4, QUEST_Z + 1, s(Blocks.RED_WOOL));
		c.set(QUEST_X + 2, G + 1, QUEST_Z, s(Blocks.LECTERN));
	}

	/** A gladiator statue on a plinth: legs, a cuirass, a helmet with a crest, a shield and a spear. */
	private static void statue(Canvas c, int x, int z) {
		c.fill(x - 1, G + 1, z - 1, x + 1, G + 2, z + 1, s(Blocks.STONE_BRICKS));
		c.fill(x - 1, G + 3, z - 1, x + 1, G + 3, z + 1, Kit.slab(Blocks.SMOOTH_STONE_SLAB, false));
		c.set(x, G + 3, z, s(Blocks.POLISHED_ANDESITE));
		c.set(x, G + 4, z, s(Blocks.POLISHED_ANDESITE));
		c.set(x, G + 5, z, s(Blocks.CHISELED_STONE_BRICKS));
		c.set(x, G + 6, z, s(Blocks.POLISHED_DIORITE));
		c.set(x, G + 7, z, s(Blocks.CHISELED_STONE_BRICKS));
		c.set(x, G + 8, z, Kit.slab(Blocks.RED_NETHER_BRICK_SLAB, false)); // the crest
		c.set(x + (x < 0 ? 1 : -1), G + 5, z, s(Blocks.POLISHED_ANDESITE)); // shield arm
		c.set(x + (x < 0 ? -1 : 1), G + 4, z, s(Blocks.LIGHTNING_ROD));
		c.set(x + (x < 0 ? -1 : 1), G + 5, z, s(Blocks.LIGHTNING_ROD));
		c.set(x + (x < 0 ? -1 : 1), G + 6, z, s(Blocks.LIGHTNING_ROD));
	}

	// ---------------------------------------------------------------------------------------------------- dock

	/** The pier: a five-wide deck on log posts with rails and lanterns, a T-head with cargo, a guard shelter. */
	private void dock(Canvas c) {
		if (!c.near(0, (DOCK_LAND_Z + DOCK_END_Z) / 2, 22)) {
			return;
		}
		for (int z = DOCK_LAND_Z; z <= DOCK_END_Z; z++) {
			boolean head = z >= DOCK_END_Z - 5;
			int half = head ? 7 : 2;
			for (int x = -half; x <= half; x++) {
				boolean edgeX = Math.abs(x) == half;
				c.set(x, DOCK_Y, z, x == 0 && !head ? s(Blocks.STRIPPED_DARK_OAK_LOG).setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z)
						: s(Blocks.SPRUCE_PLANKS));
				c.fill(x, DOCK_Y + 1, z, x, DOCK_Y + 4, z, s(Blocks.AIR));
				boolean post = edgeX && ((z - DOCK_LAND_Z) % 4 == 0 || z == DOCK_END_Z || z == DOCK_END_Z - 5)
						|| (head && (z == DOCK_END_Z) && x % 3 == 0);
				if (post) {
					int floor = Math.min(c.ground(x, z), Terrain.WATER_TOP - 1);
					c.fill(x, floor, z, x, DOCK_Y - 1, z, s(Blocks.STONE_BRICKS)); // stone footings
					c.set(x, DOCK_Y + 1, z, s(Blocks.DARK_OAK_FENCE));
					if ((z - DOCK_LAND_Z) % 8 == 0 || z == DOCK_END_Z) {
						c.set(x, DOCK_Y + 2, z, lantern(false));
					}
				} else if (edgeX && !head && (z - DOCK_LAND_Z) % 8 != 6) {
					c.set(x, DOCK_Y + 1, z, Kit.connected(Blocks.DARK_OAK_FENCE, false));
				} else if (head && z == DOCK_END_Z) {
					c.set(x, DOCK_Y + 1, z, Kit.connected(Blocks.DARK_OAK_FENCE, true));
				}
			}
		}
		int y = DOCK_Y + 1, hz = DOCK_END_Z - 4;
		c.set(-6, y, hz, Canvas.facing(s(Blocks.BARREL), Direction.UP));
		c.set(-5, y, hz, Canvas.facing(s(Blocks.BARREL), Direction.UP));
		c.set(-6, y + 1, hz, Canvas.facing(s(Blocks.BARREL), Direction.UP));
		c.set(-6, y, hz + 1, s(Blocks.HAY_BLOCK));
		// Guard shelter on the landing: stone posts, a slab roof, a bench and a lantern.
		int sz = DOCK_LAND_Z - 4;
		for (int[] p : new int[][]{{6, sz - 1}, {9, sz - 1}, {6, sz + 2}, {9, sz + 2}}) {
			int g = c.ground(p[0], p[1]);
			c.fill(p[0], g + 1, p[1], p[0], g + 3, p[1], s(Blocks.STONE_BRICK_WALL));
		}
		int g = c.ground(7, sz);
		c.fill(6, g + 4, sz - 1, 9, g + 4, sz + 2, Kit.slab(Blocks.STONE_BRICK_SLAB, false));
		c.set(7, g + 3, sz, lantern(true));
		c.set(8, g + 1, sz + 1, stair(Blocks.SPRUCE_STAIRS, Direction.EAST, false));
		c.set(9, g + 1, sz, Canvas.facing(s(Blocks.BARREL), Direction.UP));
		// Accessibility: a boat landing one block above the water, with stairs up to the deck (see Kit.boatLanding).
		for (int x = -1; x <= 1; x++) { // the dock's first boards step up to the shore
			c.set(x, DOCK_Y + 1, DOCK_LAND_Z, Kit.stairs(Blocks.DARK_OAK_STAIRS, Direction.NORTH, false));
		}
		Kit.boatLanding(c, 2, DOCK_LAND_Z + 22, Direction.EAST, 0, 1, DOCK_Y, Blocks.DARK_OAK_STAIRS,
				s(Blocks.DARK_OAK_PLANKS), s(Blocks.STONE_BRICKS), Blocks.DARK_OAK_FENCE);
	}

	// ---------------------------------------------------------------------------------------------------- settlement

	/**
	 * A settlement building: a stone brick plinth and base course, white plaster walls in a dark-oak timber frame, a
	 * gable roof of brick (terracotta-red) with an overhang, shuttered windows, an open door, and a furnished inside.
	 */
	private static void house(Canvas c, House h) {
		int fy = G + 1, wallTop = fy + 4 * h.floors();
		c.foundation(h.x1(), h.z1(), h.x2(), h.z2(), fy, s(Blocks.STONE_BRICKS));
		for (int x = h.x1(); x <= h.x2(); x++) {
			for (int z = h.z1(); z <= h.z2(); z++) {
				boolean ex = x == h.x1() || x == h.x2(), ez = z == h.z1() || z == h.z2();
				if (!ex && !ez) {
					c.set(x, fy, z, s(Blocks.SPRUCE_PLANKS));
					c.fill(x, fy + 1, z, x, wallTop - 1, z, s(Blocks.AIR));
					for (int f = 1; f < h.floors(); f++) {
						c.set(x, fy + 4 * f, z, s(Blocks.SPRUCE_PLANKS));
					}
					continue;
				}
				boolean post = (ex && ez) || (ex ? (z - h.z1()) % 3 == 0 : (x - h.x1()) % 3 == 0);
				for (int y = fy + 1; y < wallTop; y++) {
					boolean beam = (y - fy) % 4 == 0;
					BlockState b = post ? s(Blocks.DARK_OAK_LOG)
							: beam ? s(Blocks.STRIPPED_DARK_OAK_LOG).setValue(RotatedPillarBlock.AXIS, ex ? Direction.Axis.Z : Direction.Axis.X)
							: (y - fy) % 4 == 1 ? s(Blocks.STONE_BRICKS) : s(Blocks.WHITE_TERRACOTTA);
					c.set(x, y, z, b);
				}
				c.set(x, wallTop, z, s(Blocks.STRIPPED_DARK_OAK_LOG).setValue(RotatedPillarBlock.AXIS, ex ? Direction.Axis.Z : Direction.Axis.X));
			}
		}
		// Windows (glass panes with shutters) between the posts, on every storey.
		for (int f = 0; f < h.floors(); f++) {
			int y = fy + 2 + 4 * f;
			for (int x = h.x1() + 1; x < h.x2(); x++) {
				if ((x - h.x1()) % 3 == 2 && x != (h.x1() + h.x2()) / 2) {
					c.set(x, y, h.z1(), Kit.connected(Blocks.GLASS_PANE, true));
					c.set(x, y, h.z2(), Kit.connected(Blocks.GLASS_PANE, true));
				}
			}
			for (int z = h.z1() + 1; z < h.z2(); z++) {
				if ((z - h.z1()) % 3 == 2 && z != (h.z1() + h.z2()) / 2) {
					c.set(h.x1(), y, z, Kit.connected(Blocks.GLASS_PANE, false));
					c.set(h.x2(), y, z, Kit.connected(Blocks.GLASS_PANE, false));
				}
			}
		}
		// The door, standing open, with a step and a lantern.
		int dx = h.front() == Direction.EAST ? h.x2() : h.front() == Direction.WEST ? h.x1() : (h.x1() + h.x2()) / 2;
		int dz = h.front() == Direction.NORTH ? h.z1() : h.front() == Direction.SOUTH ? h.z2() : (h.z1() + h.z2()) / 2;
		BlockState door = s(Blocks.SPRUCE_DOOR).setValue(BlockStateProperties.HORIZONTAL_FACING, h.front()).setValue(BlockStateProperties.OPEN, true)
				.setValue(BlockStateProperties.DOOR_HINGE, DoorHingeSide.LEFT);
		c.set(dx, fy + 1, dz, door.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER));
		c.set(dx, fy + 2, dz, door.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER));
		c.set(dx, fy + 3, dz, s(Blocks.STRIPPED_DARK_OAK_LOG));
		int ox = dx + h.front().getStepX(), oz = dz + h.front().getStepZ();
		c.set(ox, fy, oz, stair(Blocks.STONE_BRICK_STAIRS, h.front().getOpposite(), false));
		c.set(ox + h.front().getClockWise().getStepX(), fy + 3, oz + h.front().getClockWise().getStepZ(), lantern(true));
		gableRoof(c, h, wallTop);
		furnish(c, h, fy);
	}

	/** A brick gable roof (ridge along the longer side) with a one-block overhang and slab ridge. */
	private static void gableRoof(Canvas c, House h, int y) {
		boolean alongX = (h.x2() - h.x1()) >= (h.z2() - h.z1());
		int lo = (alongX ? h.z1() : h.x1()) - 1, hi = (alongX ? h.z2() : h.x2()) + 1;
		int a1 = (alongX ? h.x1() : h.z1()) - 1, a2 = (alongX ? h.x2() : h.z2()) + 1;
		for (int i = 0; lo + i <= hi - i; i++) {
			int p = lo + i, q = hi - i, yy = y + 1 + i;
			for (int t = a1; t <= a2; t++) {
				boolean gable = t == a1 + 1 || t == a2 - 1;
				if (p == q) {
					set(c, alongX, t, yy, p, Kit.slab(Blocks.BRICK_SLAB, false));
				} else {
					set(c, alongX, t, yy, p, stair(Blocks.BRICK_STAIRS, alongX ? Direction.SOUTH : Direction.EAST, false));
					set(c, alongX, t, yy, q, stair(Blocks.BRICK_STAIRS, alongX ? Direction.NORTH : Direction.WEST, false));
				}
				if (gable) { // every row, the bottom one too (it closes the slot between the wall top and the roof)
					for (int m = p + 1; m < q; m++) {
						set(c, alongX, t, yy, m, s(Blocks.WHITE_TERRACOTTA));
					}
				}
			}
		}
	}

	private static void set(Canvas c, boolean alongX, int t, int y, int m, BlockState state) {
		if (alongX) {
			c.set(t, y, m, state);
		} else {
			c.set(m, y, t, state);
		}
	}

	private static void bed(Canvas c, int x, int y, int z, Block bed, Direction head) {
		c.set(x, y, z, s(bed).setValue(BedBlock.FACING, head).setValue(BedBlock.PART, BedPart.HEAD));
		c.set(x - head.getStepX(), y, z - head.getStepZ(), s(bed).setValue(BedBlock.FACING, head).setValue(BedBlock.PART, BedPart.FOOT));
	}

	private static void furnish(Canvas c, House h, int fy) {
		int y = fy + 1;
		switch (h.use()) {
			case BARRACKS -> {
				for (int x = h.x1() + 1; x <= h.x2() - 3; x += 2) {
					bed(c, x, y, h.z1() + 1, Blocks.RED_BED, Direction.NORTH);
					bed(c, x, y, h.z2() - 1, Blocks.RED_BED, Direction.SOUTH);
				}
				weaponRack(c, h.x2() - 1, y, h.z1() + 1, Direction.EAST);
				dummy(c, h.x2() - 2, y, (h.z1() + h.z2()) / 2 + 2);
				c.set(h.x2() - 1, y, h.z2() - 1, Canvas.facing(s(Blocks.BARREL), Direction.UP));
			}
			case SHOP -> {
				for (int z = h.z1() + 1; z <= h.z2() - 1; z++) {
					if (z != (h.z1() + h.z2()) / 2) {
						c.set(h.x1() + 3, y, z, Kit.slab(Blocks.SPRUCE_SLAB, true));
					}
					c.set(h.x2() - 1, y, z, z % 2 == 0 ? Canvas.facing(s(Blocks.BARREL), Direction.WEST) : s(Blocks.CHEST));
					c.set(h.x2() - 1, y + 1, z, s(Blocks.BOOKSHELF));
				}
				c.set(h.x1() + 3, y + 1, h.z1() + 1, s(Blocks.FLOWER_POT));
				weaponRack(c, h.x2() - 3, y, h.z1() + 1, Direction.EAST);
				c.set(h.x2() - 2, y, h.z2() - 1, s(Blocks.SMITHING_TABLE));
			}
			case TAVERN -> {
				for (int z = h.z1() + 1; z <= h.z1() + 4; z++) {
					c.set(h.x2() - 2, y, z, Kit.slab(Blocks.SPRUCE_SLAB, true));
					c.set(h.x2() - 1, y, z, Canvas.facing(s(Blocks.BARREL), Direction.WEST));
				}
				for (int[] t : new int[][]{{h.x1() + 2, h.z2() - 2}, {h.x1() + 5, h.z2() - 2}, {h.x1() + 2, h.z1() + 2}}) {
					c.set(t[0], y, t[1], s(Blocks.DARK_OAK_FENCE));
					c.set(t[0], y + 1, t[1], s(Blocks.DARK_OAK_PRESSURE_PLATE));
					c.set(t[0] - 1, y, t[1], stair(Blocks.SPRUCE_STAIRS, Direction.WEST, false));
					c.set(t[0] + 1, y, t[1], stair(Blocks.SPRUCE_STAIRS, Direction.EAST, false));
				}
				c.set(h.x2() - 1, y, h.z2() - 1, Canvas.facing(s(Blocks.BARREL), Direction.UP));
				c.set(h.x2() - 1, y + 1, h.z2() - 1, Canvas.facing(s(Blocks.BARREL), Direction.UP));
				// Ladder to the upstairs room.
				for (int yy = y; yy <= fy + 4; yy++) {
					c.set(h.x2() - 1, yy, h.z2() - 2, s(Blocks.LADDER).setValue(LadderBlock.FACING, Direction.WEST));
				}
				c.set(h.x2() - 1, fy + 4, h.z2() - 2, s(Blocks.LADDER).setValue(LadderBlock.FACING, Direction.WEST));
				bed(c, h.x1() + 1, fy + 5, h.z1() + 2, Blocks.BROWN_BED, Direction.NORTH);
				c.set(h.x1() + 2, fy + 5, h.z1() + 1, Canvas.facing(s(Blocks.BARREL), Direction.UP));
				c.set(h.x1() + 4, fy + 5, h.z1() + 1, s(Blocks.CRAFTING_TABLE));
				c.set((h.x1() + h.x2()) / 2, fy + 7, (h.z1() + h.z2()) / 2, lantern(true));
			}
			case HOUSE_A -> {
				bed(c, h.x1() + 1, y, h.z1() + 2, Blocks.WHITE_BED, Direction.NORTH);
				c.set(h.x1() + 2, y, h.z1() + 1, s(Blocks.FURNACE));
				c.set(h.x1() + 3, y, h.z1() + 1, s(Blocks.CRAFTING_TABLE));
				c.set(h.x1() + 1, y, h.z2() - 1, Canvas.facing(s(Blocks.BARREL), Direction.UP));
				c.set(h.x1() + 3, y, h.z2() - 2, s(Blocks.DARK_OAK_FENCE));
				c.set(h.x1() + 3, y + 1, h.z2() - 2, s(Blocks.DARK_OAK_PRESSURE_PLATE));
			}
			case HOUSE_B -> {
				bed(c, h.x2() - 1, y, h.z2() - 2, Blocks.LIGHT_GRAY_BED, Direction.SOUTH);
				c.set(h.x1() + 1, y, h.z2() - 1, s(Blocks.SMOKER));
				c.set(h.x1() + 2, y, h.z2() - 1, s(Blocks.BARREL));
				c.set(h.x1() + 1, y, h.z1() + 2, s(Blocks.FLOWER_POT));
				// Small walled courtyard to the west.
				for (int z = h.z1(); z <= h.z2(); z++) {
					c.set(h.x1() - 4, c.ground(h.x1() - 4, z) + 1, z, s(Blocks.COBBLESTONE_WALL));
				}
				for (int x = h.x1() - 4; x < h.x1(); x++) {
					c.set(x, c.ground(x, h.z2()) + 1, h.z2(), s(Blocks.COBBLESTONE_WALL));
				}
				c.set(h.x1() - 2, c.ground(h.x1() - 2, h.z1() + 3) + 1, h.z1() + 3, s(Blocks.POTTED_RED_TULIP));
			}
		}
	}

	// ---------------------------------------------------------------------------------------------------- combat areas

	/**
	 * The outdoor training grounds: a gravel courtyard (25 x 25) inside a low stone wall with gates on the west and north,
	 * columns at the corners, dummies, two raised training platforms, low obstacle walls and weapon racks; the middle is
	 * left open.
	 */
	private void trainingGrounds(Canvas c) {
		int y = 67, x1 = TRAIN_X - 12, x2 = TRAIN_X + 12, z1 = TRAIN_Z - 12, z2 = TRAIN_Z + 12;
		for (int x = x1; x <= x2; x++) {
			for (int z = z1; z <= z2; z++) {
				if (!c.inChunk(x, z)) {
					continue;
				}
				boolean edge = x == x1 || x == x2 || z == z1 || z == z2;
				c.set(x, y, z, edge ? s(Blocks.STONE_BRICKS) : unit(x, z, 91) < 0.2 ? s(Blocks.COARSE_DIRT) : s(Blocks.GRAVEL));
				c.fill(x, y + 1, z, x, y + 4, z, s(Blocks.AIR));
				boolean gate = (x == x1 && Math.abs(z - TRAIN_Z) <= 2) || (z == z1 && Math.abs(x - TRAIN_X) <= 2);
				if (edge && !gate) {
					c.set(x, y + 1, z, s(Blocks.STONE_BRICK_WALL));
				}
			}
		}
		for (int[] p : new int[][]{{x1, z1}, {x2, z1}, {x1, z2}, {x2, z2}}) {
			c.fill(p[0], y + 1, p[1], p[0], y + 4, p[1], s(Blocks.POLISHED_ANDESITE));
			c.set(p[0], y + 5, p[1], s(Blocks.CHISELED_STONE_BRICKS));
			c.set(p[0], y + 6, p[1], lantern(false));
		}
		for (int[] d : new int[][]{{-7, -8}, {-4, -8}, {-1, -8}, {2, -8}}) {
			dummy(c, TRAIN_X + d[0], y + 1, TRAIN_Z + d[1]);
		}
		for (int[] p : new int[][]{{7, -7}, {7, 6}}) {
			c.fill(TRAIN_X + p[0] - 2, y + 1, TRAIN_Z + p[1] - 2, TRAIN_X + p[0] + 2, y + 1, TRAIN_Z + p[1] + 2, s(Blocks.SMOOTH_STONE));
			c.fill(TRAIN_X + p[0] - 2, y + 1, TRAIN_Z + p[1] - 2, TRAIN_X + p[0] + 2, y + 1, TRAIN_Z + p[1] - 2, Kit.slab(Blocks.SMOOTH_STONE_SLAB, false));
		}
		for (int k = 0; k < 4; k++) {
			c.set(TRAIN_X - 8, y + 1, TRAIN_Z + 4 + k, s(Blocks.STONE_BRICKS)); // an obstacle wall
		}
		c.set(TRAIN_X - 8, y + 2, TRAIN_Z + 5, Kit.slab(Blocks.STONE_BRICK_SLAB, false));
		weaponRack(c, TRAIN_X - 3, y + 1, z2 - 1, Direction.EAST);
		weaponRack(c, TRAIN_X + 3, y + 1, z2 - 1, Direction.EAST);
		c.set(x2 - 1, y + 1, TRAIN_Z, s(Blocks.TARGET));
		c.set(x2 - 1, y + 1, TRAIN_Z - 3, s(Blocks.TARGET));
		c.set(x2 - 1, y + 1, TRAIN_Z + 3, s(Blocks.HAY_BLOCK));
	}

	/**
	 * The ancient warrior grounds: worn, broken paving, three raised round combat platforms, the stubs of a colonnade
	 * (broken columns of differing heights, one fallen), a ruined arch and torches. Mossy and cracked stone throughout.
	 */
	private void warriorGrounds(Canvas c) {
		int y = 67;
		for (int x = RUINS_X - 12; x <= RUINS_X + 12; x++) {
			for (int z = RUINS_Z - 12; z <= RUINS_Z + 12; z++) {
				if (!c.inChunk(x, z) || !near(x, z, RUINS_X, RUINS_Z, 12)) {
					continue;
				}
				double u = unit(x, z, 101);
				if (u < 0.55) {
					c.set(x, y, z, u < 0.15 ? s(Blocks.MOSSY_STONE_BRICKS) : u < 0.3 ? s(Blocks.CRACKED_STONE_BRICKS) : s(Blocks.STONE_BRICKS));
				} else if (u < 0.65) {
					c.set(x, y, z, s(Blocks.MOSSY_COBBLESTONE));
				}
				c.fill(x, y + 1, z, x, y + 4, z, s(Blocks.AIR));
			}
		}
		for (int[] p : new int[][]{{-5, -4}, {5, -3}, {0, 6}}) {
			for (int dx = -3; dx <= 3; dx++) {
				for (int dz = -3; dz <= 3; dz++) {
					if (dx * dx + dz * dz <= 10) {
						int x = RUINS_X + p[0] + dx, z = RUINS_Z + p[1] + dz;
						c.set(x, y + 1, z, dx * dx + dz * dz > 6 ? aged(x, y + 1, z) : s(Blocks.SMOOTH_STONE));
					}
				}
			}
		}
		int[][] cols = {{-11, -8, 5}, {-11, -2, 2}, {-11, 4, 4}, {11, -8, 3}, {11, -2, 6}, {11, 4, 1}};
		for (int[] col : cols) {
			int x = RUINS_X + col[0], z = RUINS_Z + col[1];
			c.set(x, y + 1, z, s(Blocks.CHISELED_STONE_BRICKS));
			c.fill(x, y + 2, z, x, y + col[2], z, s(Blocks.POLISHED_ANDESITE));
			if (col[2] >= 5) {
				c.set(x, y + col[2] + 1, z, s(Blocks.TORCH));
			}
		}
		for (int k = 0; k < 4; k++) {
			c.set(RUINS_X + 7 + k, y + 1, RUINS_Z + 9, s(Blocks.POLISHED_ANDESITE).getBlock() == Blocks.POLISHED_ANDESITE && k == 0
					? s(Blocks.CHISELED_STONE_BRICKS) : s(Blocks.POLISHED_ANDESITE)); // a fallen column
		}
		// A ruined arch on the north edge.
		int ax = RUINS_X, az = RUINS_Z - 12;
		c.fill(ax - 3, y + 1, az, ax - 3, y + 6, az, aged(ax - 3, y, az));
		c.fill(ax + 3, y + 1, az, ax + 3, y + 4, az, aged(ax + 3, y, az));
		c.fill(ax - 2, y + 6, az, ax, y + 6, az, s(Blocks.STONE_BRICKS));
		c.set(ax - 2, y + 5, az, stair(Blocks.STONE_BRICK_STAIRS, Direction.EAST, true));
		c.set(ax + 1, y + 6, az, stair(Blocks.STONE_BRICK_STAIRS, Direction.WEST, false));
		c.set(ax - 4, y + 1, az + 1, s(Blocks.MOSSY_COBBLESTONE_WALL));
	}

	/** Small ruins around the island: a broken colonnade and a monument on the north loop, a ruined wall in the west. */
	private void ruins(Canvas c) {
		if (c.near(0, -54, 10)) {
			for (int i = -2; i <= 2; i++) {
				int x = i * 3, z = -54, g = c.ground(x, z);
				int h = 2 + (int) (unit(x, z, 111) * 5);
				c.set(x, g + 1, z, s(Blocks.CHISELED_STONE_BRICKS));
				c.fill(x, g + 2, z, x, g + h, z, s(Blocks.POLISHED_ANDESITE));
				if (h >= 5) {
					c.set(x, g + h + 1, z, Kit.slab(Blocks.SMOOTH_STONE_SLAB, false));
				}
			}
		}
		if (c.near(-40, -30, 6)) {
			for (int k = 0; k < 7; k++) {
				int x = -43 + k, z = -30, g = c.ground(x, z);
				int h = 1 + (int) (unit(x, z, 112) * 3);
				for (int y = 1; y <= h; y++) {
					c.set(x, g + y, z, y == 1 ? s(Blocks.MOSSY_STONE_BRICKS) : aged(x, g + y, z));
				}
			}
		}
		if (c.near(40, -32, 4)) {
			int g = c.ground(40, -32);
			c.fill(39, g + 1, -33, 41, g + 2, -31, s(Blocks.STONE_BRICKS));
			c.set(40, g + 3, -32, s(Blocks.CHISELED_STONE_BRICKS));
			c.set(40, g + 4, -32, s(Blocks.POLISHED_ANDESITE));
			c.set(40, g + 5, -32, s(Blocks.POLISHED_ANDESITE));
			c.set(40, g + 6, -32, s(Blocks.STONE_BRICK_WALL));
		}
	}
}
