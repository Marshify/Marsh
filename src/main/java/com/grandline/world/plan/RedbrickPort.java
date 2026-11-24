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
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.WallBannerBlock;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Redbrick Port (layout 16): the Marines' naval fortress, built to the owner's specification and the approved
 * blueprint (docs/REDBRICK_PORT_BLUEPRINT.md). Island-local coordinates from the headquarters' centre: +x east,
 * +z south (the dock is south, the crescent wall north).
 * <pre>
 *  piece                    where (local)                         y
 *  plaza (six platforms)     core octagon -30..30 x -38..26,        65 (walk 66)
 *                            wings x 27..50, quays z 22..32, apron
 *  headquarters              round, 21 across; hall 66-76;          65-100
 *                            blue platform 77-78 (23 across, six cannons);
 *                            crown drum 79-92 (15 across, fins, lit clerestory); roof 93-100
 *  crescent wall             r 39-41, angles 215-325, 66-79         walkway 78
 *  watchtowers               NW (-33,-23), NE (33,-23), SW (-57,32), SE (57,32)
 *  dock                      x -2..2, z 39..46                      65
 * </pre>
 * Worlds made before layout 16 keep the original Redbrick Port.
 */
public final class RedbrickPort extends BasePlan {
	static final int G = 65, SEA = 62;
	/** Headquarters heights. */
	static final int HALL_TOP = 76, PLAT = 77, CROWN = 79, CROWN_TOP = 92, ROOF = 93;
	static final double R_HQ = 10.5, R_PLAT = 11.5, R_DRUM = 7.5;
	/** Crescent wall. */
	static final double WALL_R_IN = 38.5, WALL_R_OUT = 41.5;
	static final double WALL_A1 = 215, WALL_A2 = 325;
	static final int WALL_TOP = 77, WALK = 78;
	/** Watchtower corners (x0, z0 = north-west corner of the 6 x 6 shaft). */
	static final int[][] TOWERS = {{-36, -26}, {31, -26}, {-60, 29}, {55, 29}};
	/** Plaza platforms: x1, z1, x2, z2, chamfer. */
	static final int[][] PLAZA = {{-30, -38, 30, 26, 10}, {-9, 20, 9, 38, 3}, {-50, -16, -27, 18, 6}, {27, -16, 50, 18, 6},
			{-44, 22, -20, 32, 3}, {20, 22, 44, 32, 3}};

	/** Crests, top row first. N navy ring, p pale field, a anchor, S compass star, '.' none. */
	static final String[] CREST9 = {"..NNNNN..", ".NppSppN.", "NppaaappN", "NppapappN", "NSaaaaaSN", "NpppapppN",
			"NappappaN", ".NaaaaaN.", "..NNNNN.."};
	static final String[] CREST7 = {".NNNNN.", "NppSppN", "NpaaapN", "NSaaaSN", "NppappN", "NaaaaaN", ".NNNNN."};
	static final String[] CREST6 = {".NNNN.", "NpaapN", "NSaaSN", "NpaapN", "NaaaaN", ".NNNN."};

	RedbrickPort() {
		super("Redbrick Port", 0.08);
		for (int[] p : PLAZA) {
			terrain.platform(p[0], p[1], p[2], p[3], p[4], G, Ground.STONE);
		}
		terrain.mass(0, -42, 58, 21, 3, 1.6, Ground.GRASS) // the green land behind the wall
				.mass(-34, -25, 9, 8, 3, 1.5, Ground.GRASS)
				.mass(34, -25, 9, 8, 3, 1.5, Ground.GRASS)
				.mass(-57, 32, 9, 8, 3, 1.5, Ground.GRASS)  // the south-west tower's islet
				.mass(57, 32, 9, 8, 3, 1.5, Ground.GRASS)   // the south-east tower's islet
				.shoreMix(s(Blocks.STONE), s(Blocks.ANDESITE), s(Blocks.COARSE_DIRT), s(Blocks.STONE))
				.reach(76);
		anchor(new Anchor(Type.ARRIVAL, 0, G + 1, 34, 0, 180)); // the Marines arrive on the harbour apron
		// NPCs and enemies follow IslandRosters.REDBRICK; these anchors mark the same places for navigation and safe zones.
		anchor(Anchor.at(Type.QUEST, -5, G + 1, 24, -90));         // the Marine Officer, left of the path to the HQ
		anchor(Anchor.at(Type.DOCK, 7, 63, 42));
		anchor(Anchor.area(Type.ENEMY_CAMP, -34, -52, 10));         // Harbor Sentinels, west
		anchor(Anchor.area(Type.ENEMY_CAMP, 34, -52, 10));          // Harbor Sentinels, east
		anchor(Anchor.at(Type.BOSS_ARENA, 0, G + 1, 0));            // the Bastion Commander, in the headquarters' hall
	}

	// ------------------------------------------------------------------------------------------------------- helpers

	static boolean inPlaza(int x, int z) {
		for (int[] p : PLAZA) {
			if (new Terrain.Platform(p[0], p[1], p[2], p[3], p[4], G, Ground.STONE).contains(x, z)) {
				return true;
			}
		}
		return false;
	}

	/** Angle in degrees 0..360 (0 east, 90 south, 270 north). */
	static double angle(double x, double z) {
		double a = Math.toDegrees(Math.atan2(z, x));
		return a < 0 ? a + 360 : a;
	}

	/** The cardinal direction closest to the given angle. */
	static Direction cardinal(double deg) {
		double t = Math.toRadians(deg), ux = Math.cos(t), uz = Math.sin(t);
		return Math.abs(ux) >= Math.abs(uz) ? (ux > 0 ? Direction.EAST : Direction.WEST) : (uz > 0 ? Direction.SOUTH : Direction.NORTH);
	}

	static double unit(int x, int z, int salt) {
		return CloudtopIslesV7.unit(x, z, salt);
	}

	/**
	 * A crest of blocks in a vertical plane, facing {@code facing} (the side people look at it from); {@code leftX,
	 * leftZ} is the bottom-left cell as seen by them, {@code bottomY} its row.
	 */
	static void badge(Canvas c, String[] rows, int leftX, int leftZ, int bottomY, Direction facing) {
		Direction right = facing.getCounterClockWise();
		for (int r = 0; r < rows.length; r++) {
			int y = bottomY + rows.length - 1 - r;
			for (int k = 0; k < rows[r].length(); k++) {
				BlockState b = switch (rows[r].charAt(k)) {
					case 'N' -> s(Blocks.BLUE_TERRACOTTA);
					case 'p' -> s(Blocks.WHITE_CONCRETE);
					case 'a' -> s(Blocks.BLUE_CONCRETE);
					case 'S' -> s(Blocks.LIGHT_BLUE_CONCRETE);
					default -> null;
				};
				if (b != null) {
					c.set(leftX + right.getStepX() * k, y, leftZ + right.getStepZ() * k, b);
				}
			}
		}
	}

	/** A blue wall banner with the crest in banner patterns (a pale roundel, a light-blue star cross, the blue anchor). */
	static void crestBanner(Canvas c, int x, int y, int z, Direction facing) {
		if (!c.inChunk(x, z)) {
			return;
		}
		c.set(x, y, z, s(Blocks.BLUE_WALL_BANNER).setValue(WallBannerBlock.FACING, facing));
		if (c.level().getBlockEntity(c.world(x, y, z)) instanceof BannerBlockEntity banner) {
			ListTag patterns = new ListTag();
			for (Object[] p : new Object[][]{{"mc", DyeColor.WHITE}, {"sc", DyeColor.LIGHT_BLUE}, {"cs", DyeColor.BLUE},
					{"bt", DyeColor.BLUE}, {"cbo", DyeColor.BLUE}}) {
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

	/**
	 * The Redbrick cannon (the owner's design), 7 long and 3 wide, aimed {@code dir}; (x, z) is its axle (the fourth
	 * cell from the back) and y the row it stands in. Along the axis from the back: a spruce slab on the ground, a
	 * spruce plank leg, the spruce beam (top slabs), the axle with open spruce trapdoor wheels either side, a spruce
	 * fence leg, and the beam running on past the muzzle; on top a barrel of five blackstone blocks with a blackstone
	 * button for a fuse near its back.
	 */
	static void cannon(Canvas c, int x, int y, int z, Direction dir) {
		int dx = dir.getStepX(), dz = dir.getStepZ();
		Direction side = dir.getClockWise();
		for (int i = 0; i <= 6; i++) {
			int px = x + (i - 3) * dx, pz = z + (i - 3) * dz;
			BlockState low = switch (i) {
				case 0 -> Kit.slab(Blocks.SPRUCE_SLAB, false);
				case 1 -> s(Blocks.SPRUCE_PLANKS);
				case 4 -> Kit.connected(Blocks.SPRUCE_FENCE, false).setValue(net.minecraft.world.level.block.FenceBlock.NORTH, false)
						.setValue(net.minecraft.world.level.block.FenceBlock.SOUTH, false);
				default -> Kit.slab(Blocks.SPRUCE_SLAB, true);
			};
			c.set(px, y, pz, low);
			c.fill(px, y + 1, pz, px, y + 2, pz, s(Blocks.AIR));
			if (i >= 1 && i <= 5) {
				c.set(px, y + 1, pz, s(Blocks.BLACKSTONE));
			}
		}
		c.set(x - dx, y + 2, z - dz, s(Blocks.POLISHED_BLACKSTONE_BUTTON)
				.setValue(net.minecraft.world.level.block.ButtonBlock.FACE, net.minecraft.world.level.block.state.properties.AttachFace.FLOOR)
				.setValue(net.minecraft.world.level.block.ButtonBlock.FACING, dir));
		for (Direction w : new Direction[]{side, side.getOpposite()}) {
			c.set(x + w.getStepX(), y, z + w.getStepZ(), s(Blocks.SPRUCE_TRAPDOOR)
					.setValue(net.minecraft.world.level.block.TrapDoorBlock.OPEN, true)
					.setValue(net.minecraft.world.level.block.TrapDoorBlock.FACING, w));
		}
	}

	/** A round plinth (5 across) of polished andesite with a smooth quartz top, under a large cannon. */
	static void plinth(Canvas c, int cx, int cz, int y) {
		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) {
				if (Math.hypot(dx, dz) <= 2.4) {
					c.set(cx + dx, y, cz + dz, Math.hypot(dx, dz) > 1.5 ? s(Blocks.POLISHED_ANDESITE) : s(Blocks.SMOOTH_QUARTZ));
				}
			}
		}
	}

	/** A broad coastal shade tree on ground y: a dark trunk with two angled branches and a wide, flat canopy. */
	static void shadeTree(Canvas c, int x, int y, int z, int seed) {
		if (!c.near(x, z, 6)) {
			return;
		}
		for (int k = 1; k <= 5; k++) {
			c.set(x, y + k, z, s(Blocks.DARK_OAK_LOG));
		}
		Direction a = Direction.from2DDataValue(seed & 3), b = a.getOpposite();
		for (Direction d : new Direction[]{a, b}) {
			BlockState log = s(Blocks.DARK_OAK_LOG).setValue(RotatedPillarBlock.AXIS, d.getAxis());
			c.set(x + d.getStepX(), y + 4, z + d.getStepZ(), log);
			c.set(x + 2 * d.getStepX(), y + 5, z + 2 * d.getStepZ(), log);
		}
		for (int dx = -4; dx <= 4; dx++) {
			for (int dz = -4; dz <= 4; dz++) {
				double r = Math.hypot(dx, dz);
				double u = unit(x + dx, z + dz, seed + 7);
				Block leaf = u < 0.2 ? Blocks.SPRUCE_LEAVES : Blocks.DARK_OAK_LEAVES;
				if (r <= 3.9 - (u < 0.15 ? 0.6 : 0)) {
					set(c, x + dx, y + 6, z + dz, Kit.leaves(leaf));
				}
				if (r <= 2.6) {
					set(c, x + dx, y + 7, z + dz, Kit.leaves(leaf));
				}
				if (r <= 3.2 && r > 1.2 && u > 0.55) {
					set(c, x + dx, y + 5, z + dz, Kit.leaves(Blocks.DARK_OAK_LEAVES));
				}
			}
		}
	}

	/** Sets a block unless something solid is already there (for leaves round logs). */
	static void set(Canvas c, int x, int y, int z, BlockState b) {
		if (!c.inChunk(x, z)) {
			return;
		}
		BlockState there = c.level().getBlockState(c.world(x, y, z));
		if (there.isAir() || there.getBlock() instanceof net.minecraft.world.level.block.LeavesBlock || there.canBeReplaced()) {
			c.set(x, y, z, b);
		}
	}

	// --------------------------------------------------------------------------------------------------------- build

	@Override
	public void build(Canvas c) {
		plaza(c);
		dock(c);
		headquarters(c);
		hall(c);
		platform(c);
		crown(c);
		wall(c);
		for (int i = 0; i < TOWERS.length; i++) {
			tower(c, TOWERS[i][0], TOWERS[i][1], i);
		}
		buildings(c);
		coastalPlatforms(c);
		guardPosts(c);
		artillery(c);
		landscape(c);
	}

	// --------------------------------------------------------------------------------------------------------- plaza

	/** Paving: smooth stone tiles on a polished andesite grid, darker tiles in a regular pattern, quay walls of stone brick. */
	private void plaza(Canvas c) {
		for (int x = -52; x <= 52; x++) {
			for (int z = -40; z <= 40; z++) {
				if (!c.inChunk(x, z) || !inPlaza(x, z)) {
					continue;
				}
				boolean edge = !inPlaza(x + 1, z) || !inPlaza(x - 1, z) || !inPlaza(x, z + 1) || !inPlaza(x, z - 1);
				BlockState top;
				double d = Math.hypot(x, z);
				if (edge) {
					top = s(Blocks.POLISHED_ANDESITE);
					c.fill(x, 55, z, x, G - 1, z, s(Blocks.STONE_BRICKS));
				} else if (Math.abs(x) <= 2 && z > 11) {
					top = (z % 4 == 0 || Math.abs(x) == 2 && z % 4 == 2) ? s(Blocks.POLISHED_ANDESITE) : s(Blocks.LIGHT_GRAY_CONCRETE);
				} else if (Math.abs(x) == 3 && z > 11) {
					top = s(Blocks.POLISHED_ANDESITE);
				} else if (d > 12.5 && d <= 13.6) {
					top = s(Blocks.LIGHT_GRAY_CONCRETE);
				} else if (Math.floorMod(x, 6) == 0 || Math.floorMod(z, 6) == 0) {
					top = s(Blocks.POLISHED_ANDESITE);
				} else if (Math.floorMod(Math.floorDiv(x, 6), 3) == 1 && Math.floorMod(Math.floorDiv(z, 6), 3) == 1) {
					top = s(Blocks.POLISHED_ANDESITE);
				} else {
					top = s(Blocks.SMOOTH_STONE);
				}
				c.set(x, G, z, top);
				c.fill(x, G + 1, z, x, G + 12, z, s(Blocks.AIR));
			}
		}
	}

	/** The front dock: 5 wide, 8 long, with two short side sections at its end, on dark oak posts. */
	private void dock(Canvas c) {
		if (!c.near(0, 42, 8)) {
			return;
		}
		for (int z = 39; z <= 46; z++) {
			for (int x = -2; x <= 2; x++) {
				c.set(x, G, z, s(Blocks.DARK_OAK_PLANKS));
				c.fill(x, G + 1, z, x, G + 3, z, s(Blocks.AIR));
			}
		}
		for (int z = 44; z <= 46; z++) {
			for (int side : new int[]{-1, 1}) {
				for (int k = 3; k <= 5; k++) {
					c.set(side * k, G, z, s(Blocks.DARK_OAK_PLANKS));
				}
			}
		}
		for (int[] p : new int[][]{{-2, 39}, {2, 39}, {-2, 43}, {2, 43}, {-5, 44}, {5, 44}, {-5, 46}, {5, 46}, {-2, 46}, {2, 46}}) {
			c.fill(p[0], 54, p[1], p[0], G - 1, p[1], s(Blocks.DARK_OAK_LOG));
		}
		for (int[] p : new int[][]{{-5, 46}, {5, 46}, {-2, 39}, {2, 39}}) {
			c.set(p[0], G + 1, p[1], s(Blocks.DARK_OAK_FENCE));
		}
		c.set(-5, G + 2, 46, CloudtopIsles.lantern(Blocks.LANTERN, false));
		c.set(5, G + 2, 46, CloudtopIsles.lantern(Blocks.LANTERN, false));
		// slab edging along the long sides
		for (int z = 40; z <= 42; z++) {
			c.set(-3, G, z, Kit.slab(Blocks.DARK_OAK_SLAB, true));
			c.set(3, G, z, Kit.slab(Blocks.DARK_OAK_SLAB, true));
		}
		// Accessibility: a boat landing one block above the water, with stairs up to the deck (see Kit.boatLanding).
		Kit.boatLanding(c, 0, 46, Direction.SOUTH, 1, 2, G, Blocks.DARK_OAK_STAIRS,
				s(Blocks.DARK_OAK_PLANKS), s(Blocks.DARK_OAK_LOG), Blocks.DARK_OAK_FENCE);
	}

	// -------------------------------------------------------------------------------------------------- headquarters

	/**
	 * The base: a round wall 21 across and 11 high of smooth quartz (calcite plinth and cornice), tall narrow cyan
	 * windows all round, and on the south face the pointed dark oak doors, the rope-and-wave frieze rising from beside
	 * them, and the crest medallion above.
	 */
	private void headquarters(Canvas c) {
		if (!c.near(0, 0, 16)) {
			return;
		}
		for (int x = -12; x <= 12; x++) {
			for (int z = -12; z <= 12; z++) {
				double d = Math.hypot(x, z);
				if (d > R_HQ + 1.2) {
					continue;
				}
				c.fill(x, 56, z, x, G, z, s(Blocks.POLISHED_ANDESITE));
				if (d > R_HQ && d <= R_HQ + 1.2) {
					// a low polished andesite skirting step round the foot of the wall
					c.set(x, G + 1, z, s(Blocks.POLISHED_ANDESITE));
					continue;
				}
				if (d <= R_HQ - 1) {
					continue; // the hall (see hall())
				}
				double a = angle(x, z);
				double u = Math.toRadians(a - 90) * R_HQ; // arc length from the south point (+ toward west)
				if (u > Math.PI * R_HQ) {
					u -= 2 * Math.PI * R_HQ;
				}
				boolean ladderBack = x == -6 && z == -8;
				for (int y = G + 1; y <= HALL_TOP; y++) {
					BlockState b = y == G + 1 || y == HALL_TOP ? s(Blocks.CALCITE) : s(Blocks.SMOOTH_QUARTZ);
					boolean front = Math.abs(u) <= 11 && z > 0;
					if (!front && !ladderBack && Math.floorMod(Math.round(u), 3) == 0) {
						if (y >= 68 && y <= 73) {
							b = s(Blocks.CYAN_STAINED_GLASS);
						} else if (y == 67) {
							b = s(Blocks.DARK_PRISMARINE);
						} else if (y == 74) {
							b = s(Blocks.BLUE_TERRACOTTA);
						}
					}
					if (front) {
						double au = Math.abs(u);
						if (au >= 4 && au <= 11) {
							// the wave: two bands rising from the foot of the wall to the medallion
							int yt = G + 1 + (int) Math.round(8 * (1 - Math.pow((Math.min(au, 10) - 4) / 6.0, 2)));
							if (y == yt) {
								b = s(Blocks.BLUE_CONCRETE);
							} else if (y == yt - 1) {
								b = s(Blocks.LIGHT_BLUE_CONCRETE);
							} else if (y == yt - 2 && au > 6) {
								b = s(Blocks.BLUE_CONCRETE);
							}
						}
					}
					c.set(x, y, z, b);
				}
			}
		}
		// The doorway (south, x -2..2): a pointed opening 7 high in a blackstone frame and a navy outer frame.
		for (int x = -4; x <= 4; x++) {
			int zo = (int) Math.floor(Math.sqrt(R_HQ * R_HQ - x * x));
			int ax = Math.abs(x);
			for (int y = G + 1; y <= G + 9; y++) {
				int k = y - G; // 1..9
				int openTop = ax <= 2 ? (ax == 2 ? 5 : ax == 1 ? 6 : 7) : 0;
				int frameTop = ax == 3 ? 5 : ax == 2 ? 6 : ax == 1 ? 7 : 8;
				int navyTop = ax == 4 ? 5 : ax == 3 ? 6 : ax == 2 ? 7 : ax == 1 ? 8 : 9;
				if (k <= openTop) {
					c.set(x, y, zo, s(Blocks.DARK_OAK_PLANKS));
					for (int zz = zo - 1; zz >= zo - 1 && Math.hypot(x, zz) > R_HQ - 1; zz--) {
						c.set(x, y, zz, s(Blocks.AIR));
					}
				} else if (k == openTop + 1 && ax <= 3 || ax == 3 && k <= frameTop) {
					c.set(x, y, zo, s(Blocks.POLISHED_BLACKSTONE));
				} else if (ax <= 4 && (k <= navyTop && (ax == 4 || k == frameTop + 1))) {
					c.set(x, y, zo, s(Blocks.BLUE_TERRACOTTA));
				}
			}
		}
		// the double doors (x -1 and 1, a dark oak post between) and a band across the upper door
		Kit.door(c, -1, G + 1, 10, Blocks.DARK_OAK_DOOR, Direction.NORTH);
		Kit.door(c, 1, G + 1, 10, Blocks.DARK_OAK_DOOR, Direction.NORTH);
		c.fill(0, G + 1, 10, 0, G + 2, 10, s(Blocks.DARK_OAK_LOG));
		c.fill(0, G + 1, 9, 0, G + 2, 9, s(Blocks.AIR));
		for (int x = -2; x <= 2; x++) {
			c.set(x, G + 4, 10, s(Blocks.STRIPPED_DARK_OAK_LOG).setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
		}
		// lanterns on blackstone brackets either side
		for (int side : new int[]{-1, 1}) {
			int x = side * 6, zo = (int) Math.floor(Math.sqrt(R_HQ * R_HQ - x * x)) + 1;
			c.set(x, G + 5, zo, s(Blocks.POLISHED_BLACKSTONE_WALL));
			c.set(x, G + 4, zo, CloudtopIsles.lantern(Blocks.LANTERN, true));
		}
		// The crest medallion over the doors, standing proud of the wall and the platform's rim.
		for (int k = 0; k < 9; k++) {
			for (int r = 0; r < 9; r++) {
				if (CREST9[r].charAt(k) != '.') {
					int y = G + 8 + 8 - r;
					c.set(k - 4, y, 11, y <= HALL_TOP || y >= CROWN ? s(Blocks.SMOOTH_QUARTZ) : s(Blocks.BLUE_CONCRETE));
				}
			}
		}
		badge(c, CREST9, -4, 12, G + 8, Direction.SOUTH);
	}

	/**
	 * The ceremonial hall inside the base: a tiled floor, a continuous blue skirting along the wall, lights hidden in
	 * the ceiling, crest banners high on the walls, the crest over the doorway inside, a carpeted aisle to a raised
	 * dais at the north, and the ladder up to the artillery platform.
	 */
	private void hall(Canvas c) {
		if (!c.near(0, 0, 12)) {
			return;
		}
		for (int x = -10; x <= 10; x++) {
			for (int z = -10; z <= 10; z++) {
				double d = Math.hypot(x, z);
				if (d > R_HQ - 1) {
					continue;
				}
				boolean line = Math.floorMod(x, 4) == 0 || Math.floorMod(z, 4) == 0;
				c.set(x, G, z, line ? s(Blocks.POLISHED_ANDESITE) : s(Blocks.SMOOTH_STONE));
				c.fill(x, G + 1, z, x, HALL_TOP, z, s(Blocks.AIR));
				if (d > R_HQ - 2) {
					c.set(x, G + 1, z, s(Blocks.BLUE_CONCRETE));
				}
				if (Math.floorMod(x, 4) == 2 && Math.floorMod(z, 4) == 2) {
					c.set(x, PLAT, z, s(Blocks.SEA_LANTERN));
				}
			}
		}
		// keep the doorway clear of the skirting
		c.fill(-2, G + 1, 9, 2, G + 1, 9, s(Blocks.AIR));
		c.set(0, G + 1, 9, s(Blocks.AIR));
		// the aisle and the dais
		for (int z = -5; z <= 9; z++) {
			for (int x = -1; x <= 1; x++) {
				c.set(x, G + 1, z, s(Blocks.BLUE_CARPET));
			}
		}
		for (int x = -4; x <= 4; x++) {
			for (int z = -9; z <= -6; z++) {
				if (Math.hypot(x, z) <= R_HQ - 1.5) {
					c.set(x, G + 1, z, s(Blocks.POLISHED_ANDESITE));
				}
			}
			c.set(x, G + 1, -5, x >= -1 && x <= 1 ? s(Blocks.BLUE_CARPET) : Kit.slab(Blocks.STONE_SLAB, false));
		}
		c.set(0, G + 2, -8, s(Blocks.LECTERN).setValue(net.minecraft.world.level.block.LecternBlock.FACING, Direction.SOUTH));
		// the crest over the doorway, inside
		badge(c, CREST7, 3, 9, G + 5, Direction.NORTH);
		// crest banners high on the walls, facing in
		for (int a = 0; a < 360; a += 45) {
			if (a == 90) {
				continue;
			}
			double t = Math.toRadians(a);
			int x = (int) Math.round(Math.cos(t) * 9), z = (int) Math.round(Math.sin(t) * 9);
			Direction in = cardinal(a + 180);
			int bx = x - in.getStepX(), bz = z - in.getStepZ();
			double bu = Math.toRadians(angle(bx, bz) - 90) * R_HQ;
			boolean window = Math.floorMod(Math.round(bu > Math.PI * R_HQ ? bu - 2 * Math.PI * R_HQ : bu), 3) == 0;
			if (Math.hypot(bx, bz) > R_HQ - 1 && !window) {
				crestBanner(c, x, G + 8, z, in);
			}
		}
		// the ladder to the artillery platform (against the wall, north-west), through the ceiling
		for (int y = G + 1; y <= PLAT + 1; y++) {
			c.set(-6, y, -7, Canvas.facing(s(Blocks.LADDER), Direction.SOUTH));
		}
	}

	/** The artillery platform: a blue disc 23 across over the hall, six large cannons round it aimed outward. */
	private void platform(Canvas c) {
		if (!c.near(0, 0, 14)) {
			return;
		}
		for (int x = -12; x <= 12; x++) {
			for (int z = -12; z <= 12; z++) {
				double d = Math.hypot(x, z);
				if (d > R_PLAT) {
					continue;
				}
				if (d <= R_HQ - 1 && Math.floorMod(x, 4) == 2 && Math.floorMod(z, 4) == 2) {
					c.set(x, PLAT + 1, z, s(Blocks.BLUE_CONCRETE)); // over a ceiling light
				} else {
					c.fill(x, PLAT, z, x, PLAT + 1, z, s(Blocks.BLUE_CONCRETE));
				}
				if (d > R_PLAT - 1) {
					c.set(x, PLAT, z, s(Blocks.BLUE_TERRACOTTA));
				}
			}
		}
		c.fill(-6, PLAT, -7, -6, PLAT + 1, -7, Canvas.facing(s(Blocks.LADDER), Direction.SOUTH));
		// six cannons round the crown, their barrels run out over the rim
		for (int a : new int[]{0, 45, 135, 180, 225, 315}) {
			double t = Math.toRadians(a);
			cannon(c, (int) Math.round(Math.cos(t) * 11), CROWN, (int) Math.round(Math.sin(t) * 11), cardinal(a));
		}
	}

	/**
	 * The crown: a white drum 15 across from the platform to y 92, eight blue buttress fins (polished andesite and
	 * orange terracotta feet) round it, one tall lit clerestory of light-blue glass with dark prismarine mullions and
	 * sea-lantern rings behind, a shallow quartz roof with a projecting rim, and a small glowing beacon cupola.
	 */
	private void crown(Canvas c) {
		if (!c.near(0, 0, 11)) {
			return;
		}
		for (int x = -10; x <= 10; x++) {
			for (int z = -10; z <= 10; z++) {
				double d = Math.hypot(x, z);
				if (d > R_DRUM) {
					if (d <= R_DRUM + 2) {
						c.set(x, ROOF, z, Kit.slab(Blocks.SMOOTH_QUARTZ_SLAB, false)); // projecting rim
					}
					continue;
				}
				boolean shell = d > R_DRUM - 1;
				double u = Math.toRadians(angle(x, z)) * R_DRUM;
				for (int y = CROWN; y <= CROWN_TOP; y++) {
					BlockState b;
					if (!shell) {
						b = d > R_DRUM - 2 && (y == 82 || y == 86 || y == 90) ? s(Blocks.SEA_LANTERN) : s(Blocks.AIR);
						if (x == 0 && z == 0) {
							b = (y - CROWN) % 3 == 2 ? s(Blocks.SEA_LANTERN) : s(Blocks.QUARTZ_PILLAR);
						}
					} else if (y <= CROWN + 1 || y >= CROWN_TOP - 1) {
						b = s(Blocks.SMOOTH_QUARTZ);
					} else {
						b = Math.floorMod(Math.round(u), 2) == 0 ? s(Blocks.DARK_PRISMARINE) : s(Blocks.LIGHT_BLUE_STAINED_GLASS);
					}
					c.set(x, y, z, b);
				}
				if (!shell) {
					c.set(x, PLAT + 1, z, s(Blocks.SMOOTH_QUARTZ));
				}
				// roof
				c.set(x, ROOF, z, s(Blocks.SMOOTH_QUARTZ));
				if (d <= 6) {
					c.set(x, ROOF + 1, z, d > 5 ? s(Blocks.BLUE_TERRACOTTA) : s(Blocks.SMOOTH_QUARTZ));
				}
				if (d <= 4) {
					c.set(x, ROOF + 2, z, d > 3 ? Kit.slab(Blocks.SMOOTH_QUARTZ_SLAB, false) : s(Blocks.SMOOTH_QUARTZ));
				}
				if (d <= 2.2) {
					for (int y = ROOF + 3; y <= ROOF + 4; y++) {
						c.set(x, y, z, x == 0 && z == 0 ? s(Blocks.SEA_LANTERN) : s(Blocks.LIGHT_BLUE_STAINED_GLASS));
					}
					c.set(x, ROOF + 5, z, Kit.slab(Blocks.SMOOTH_QUARTZ_SLAB, false));
				}
			}
		}
		c.set(0, ROOF + 5, 0, s(Blocks.SMOOTH_QUARTZ));
		c.set(0, ROOF + 6, 0, s(Blocks.QUARTZ_PILLAR));
		c.set(0, ROOF + 7, 0, s(Blocks.END_ROD));
		// buttress fins at 22.5 + 45k: 2 deep, the outer half stepping down
		for (int k = 0; k < 8; k++) {
			double t = Math.toRadians(22.5 + 45 * k);
			for (double r = R_DRUM + 0.3; r <= R_DRUM + 2.1; r += 0.35) {
				int x = (int) Math.round(Math.cos(t) * r), z = (int) Math.round(Math.sin(t) * r);
				if (Math.hypot(x, z) <= R_DRUM - 0.5) {
					continue;
				}
				int top = r < R_DRUM + 1.1 ? CROWN_TOP : CROWN_TOP - 4;
				for (int y = CROWN; y <= top; y++) {
					c.set(x, y, z, y == CROWN ? s(Blocks.POLISHED_ANDESITE) : y == CROWN + 1 ? s(Blocks.ORANGE_TERRACOTTA) : s(Blocks.BLUE_CONCRETE));
				}
				if (top < CROWN_TOP) {
					c.set(x, top + 1, z, Kit.slab(Blocks.SMOOTH_QUARTZ_SLAB, false));
				}
			}
		}
		// the door into the crown room, on the north side, from the platform
		c.fill(0, CROWN, -6, 0, CROWN + 1, -6, s(Blocks.AIR));
		Kit.door(c, 0, CROWN, -7, Blocks.DARK_OAK_DOOR, Direction.SOUTH);
	}

	// ---------------------------------------------------------------------------------------------------- the wall

	/**
	 * The crescent wall round the north side (radius 39-41, from the north-west tower to the north-east one): smooth
	 * quartz on a calcite foot, tall white ribs on both faces, narrow barred openings between them, a blue band under
	 * a dark-blue walkway with alternating merlons, cannons aimed out to sea, a gate in the middle and crest banners.
	 */
	private void wall(Canvas c) {
		if (!c.near(0, -30, 46)) {
			return;
		}
		for (int x = -44; x <= 44; x++) {
			for (int z = -44; z <= -12; z++) {
				if (!c.inChunk(x, z)) {
					continue;
				}
				double d = Math.hypot(x, z), a = angle(x, z);
				if (a < WALL_A1 - 2 || a > WALL_A2 + 2 || d <= WALL_R_IN - 1 || d > WALL_R_OUT + 1) {
					continue;
				}
				double arc = Math.toRadians(a - WALL_A1) * 40;
				boolean body = d > WALL_R_IN && d <= WALL_R_OUT && a >= WALL_A1 && a <= WALL_A2;
				boolean rib = Math.floorMod(Math.round(arc), 5) == 0 && a > WALL_A1 + 1 && a < WALL_A2 - 1;
				if (body) {
					c.fill(x, 55, z, x, G, z, s(Blocks.STONE_BRICKS));
					boolean opening = Math.floorMod(Math.round(arc), 5) == 2 || Math.floorMod(Math.round(arc), 5) == 3;
					boolean gate = Math.abs(x) <= 1;
					for (int y = G + 1; y <= WALL_TOP; y++) {
						BlockState b = y <= G + 2 ? s(Blocks.CALCITE) : y >= WALL_TOP - 1 ? s(Blocks.BLUE_CONCRETE) : s(Blocks.SMOOTH_QUARTZ);
						if (opening && Math.floorMod(Math.round(arc), 5) == 2 && y >= G + 3 && y <= G + 9 && !gate) {
							b = d <= WALL_R_IN + 1 ? Kit.connected(Blocks.IRON_BARS, Math.abs(z) > Math.abs(x)) : s(Blocks.AIR);
						}
						if (gate && y <= G + 5 + (Math.abs(x) == 0 ? 1 : 0)) {
							b = s(Blocks.AIR);
						}
						c.set(x, y, z, b);
					}
					c.set(x, WALK, z, s(Blocks.BLUE_CONCRETE));
					c.fill(x, WALK + 1, z, x, WALK + 3, z, s(Blocks.AIR));
					if (d > WALL_R_OUT - 1 && Math.floorMod((int) Math.floor(arc / 2), 2) == 0) {
						c.set(x, WALK + 1, z, s(Blocks.BLUE_TERRACOTTA)); // merlons on the seaward edge
					}
				} else if (rib && (d <= WALL_R_IN && d > WALL_R_IN - 1 || d > WALL_R_OUT && d <= WALL_R_OUT + 1)) {
					int top = d <= WALL_R_IN ? WALK + 1 : WALL_TOP;
					c.fill(x, 55, z, x, top, z, s(Blocks.SMOOTH_QUARTZ));
				}
			}
		}
		// the gate's arch trim
		c.set(-1, G + 6, -40, Kit.stairs(Blocks.QUARTZ_STAIRS, Direction.EAST, true));
		c.set(1, G + 6, -40, Kit.stairs(Blocks.QUARTZ_STAIRS, Direction.WEST, true));
		// cannons aimed out to sea, each on a gun bay built out from the walkway (5 wide, quartz corbel under it,
		// a navy parapet along its sides)
		for (int a : new int[]{232, 252, 288, 308}) {
			double t = Math.toRadians(a);
			Direction out = cardinal(a), side = out.getClockWise();
			int x = (int) Math.round(Math.cos(t) * 43), z = (int) Math.round(Math.sin(t) * 43);
			for (int i = 0; i <= 5; i++) {
				for (int w = -2; w <= 2; w++) {
					int bx = x + (i - 3) * out.getStepX() + w * side.getStepX(), bz = z + (i - 3) * out.getStepZ() + w * side.getStepZ();
					double bd = Math.hypot(bx, bz);
					if (bd <= WALL_R_IN) {
						continue;
					}
					if (bd > WALL_R_OUT) {
						c.set(bx, WALK, bz, s(Blocks.BLUE_CONCRETE));
						c.set(bx, WALK - 1, bz, s(Blocks.SMOOTH_QUARTZ));
						c.set(bx, WALK - 2, bz, Kit.stairs(Blocks.QUARTZ_STAIRS, out.getOpposite(), true));
					}
					c.fill(bx, WALK + 1, bz, bx, WALK + 3, bz, s(Blocks.AIR));
					if (Math.abs(w) == 2 && bd > WALL_R_OUT) {
						c.set(bx, WALK + 1, bz, s(Blocks.BLUE_TERRACOTTA));
					}
				}
			}
			cannon(c, x, WALK + 1, z, out);
		}
		// crest banners on the inner face
		for (int a : new int[]{240, 270, 300}) {
			double t = Math.toRadians(a);
			int x = (int) Math.round(Math.cos(t) * 38), z = (int) Math.round(Math.sin(t) * 38);
			crestBanner(c, x, WALL_TOP - 2, z, cardinal(a + 180));
		}
	}

	// ------------------------------------------------------------------------------------------------------ towers

	/**
	 * A watchtower: a heavy grey foundation, a 6 x 6 white shaft with blue slit windows, a grey ledge under the 8 x 8
	 * chamber with small dark arched windows, a steep dark prismarine roof, a flagpole with a blue pennant. The two
	 * southern towers carry the crest on their harbour face; the two northern ones open onto the wall walk.
	 */
	private void tower(Canvas c, int x0, int z0, int index) {
		if (!c.near(x0 + 3, z0 + 3, 9)) {
			return;
		}
		boolean south = index >= 2;
		int base = G + 1, shaftTop = 81, ledge = 82, chamTop = 86;
		// foundation: 8 x 8 of andesite and polished andesite, down to the sea floor
		for (int x = x0 - 1; x <= x0 + 6; x++) {
			for (int z = z0 - 1; z <= z0 + 6; z++) {
				boolean rim = x == x0 - 1 || x == x0 + 6 || z == z0 - 1 || z == z0 + 6;
				c.fill(x, 52, z, x, base - 2, z, s(Blocks.ANDESITE));
				c.set(x, base - 1, z, rim ? s(Blocks.POLISHED_ANDESITE) : s(Blocks.ANDESITE));
				c.set(x, base, z, rim ? s(Blocks.POLISHED_ANDESITE) : s(Blocks.SPRUCE_PLANKS));
			}
		}
		for (int x = x0; x <= x0 + 5; x++) {
			for (int z = z0; z <= z0 + 5; z++) {
				boolean ex = x == x0 || x == x0 + 5, ez = z == z0 || z == z0 + 5;
				for (int y = base + 1; y <= shaftTop; y++) {
					if (!ex && !ez) {
						c.set(x, y, z, y == WALK ? s(Blocks.SPRUCE_PLANKS) : s(Blocks.AIR));
						continue;
					}
					int col = ex ? z - z0 : x - x0;
					boolean slit = !(ex && ez) && (col == 1 || col == 4) && (y >= 70 && y <= 73 || y >= 75 && y <= 78);
					c.set(x, y, z, ex && ez ? s(Blocks.CALCITE) : slit ? s(Blocks.BLUE_TERRACOTTA) : s(Blocks.SMOOTH_QUARTZ));
				}
			}
		}
		// ledge and chamber
		for (int x = x0 - 2; x <= x0 + 7; x++) {
			for (int z = z0 - 2; z <= z0 + 7; z++) {
				boolean ring = x == x0 - 2 || x == x0 + 7 || z == z0 - 2 || z == z0 + 7;
				c.set(x, ledge, z, ring ? s(Blocks.STONE_BRICKS) : x >= x0 && x <= x0 + 5 && z >= z0 && z <= z0 + 5 ? s(Blocks.SPRUCE_PLANKS) : s(Blocks.STONE_BRICKS));
			}
		}
		for (int x = x0 - 1; x <= x0 + 6; x++) {
			for (int z = z0 - 1; z <= z0 + 6; z++) {
				boolean ex = x == x0 - 1 || x == x0 + 6, ez = z == z0 - 1 || z == z0 + 6;
				for (int y = ledge + 1; y <= chamTop; y++) {
					if (!ex && !ez) {
						c.set(x, y, z, s(Blocks.AIR));
						continue;
					}
					int col = ex ? z - (z0 - 1) : x - (x0 - 1);
					boolean window = !(ex && ez) && (col == 2 || col == 5) && (y == ledge + 2 || y == ledge + 3);
					c.set(x, y, z, ex && ez ? s(Blocks.CALCITE) : window ? s(Blocks.AIR) : s(Blocks.SMOOTH_QUARTZ));
				}
			}
		}
		c.set(x0 + 3, chamTop, z0 + 3, CloudtopIsles.lantern(Blocks.LANTERN, true));
		Kit.hipRoof(c, x0 - 1, z0 - 1, x0 + 6, z0 + 6, chamTop + 1, Blocks.DARK_PRISMARINE_STAIRS, Blocks.DARK_PRISMARINE_SLAB, Blocks.DARK_PRISMARINE);
		// flagpole and pennant
		int poleTop = chamTop + 10;
		c.fill(x0 + 2, chamTop + 5, z0 + 2, x0 + 2, poleTop, z0 + 2, s(Blocks.DARK_OAK_FENCE));
		c.fill(x0 + 3, poleTop - 2, z0 + 2, x0 + 3, poleTop, z0 + 2, s(Blocks.BLUE_WOOL));
		c.fill(x0 + 4, poleTop - 1, z0 + 2, x0 + 4, poleTop, z0 + 2, s(Blocks.BLUE_WOOL));
		c.set(x0 + 5, poleTop, z0 + 2, s(Blocks.BLUE_WOOL));
		// ladder inside from the ground to the chamber
		for (int y = base + 1; y <= ledge; y++) {
			c.set(x0 + 2, y, z0 + 1, Canvas.facing(s(Blocks.LADDER), Direction.SOUTH));
		}
		// doors: the southern towers' ground door faces the bridge, the northern ones face south; the northern ones also
		// open onto the wall walk
		if (south) {
			boolean west = index == 2;
			int dx = west ? x0 + 5 : x0;
			Kit.door(c, dx, base + 1, z0 + 3, Blocks.DARK_OAK_DOOR, west ? Direction.WEST : Direction.EAST);
			badge(c, CREST6, x0, z0 + 6, 74, Direction.SOUTH);
			c.fill(x0, 74, z0 + 5, x0 + 5, 79, z0 + 5, s(Blocks.SMOOTH_QUARTZ));
		} else {
			Kit.door(c, x0 + 3, base + 1, z0 + 5, Blocks.DARK_OAK_DOOR, Direction.NORTH);
			boolean west = index == 0;
			int wx = west ? x0 + 5 : x0;
			Kit.door(c, wx, WALK + 1, z0 + 1, Blocks.DARK_OAK_DOOR, west ? Direction.WEST : Direction.EAST);
		}
	}

	// --------------------------------------------------------------------------------------------- waterfront town

	record Building(char style, int x1, int z1, int x2, int z2, Direction door) {
	}

	static final Building[] BUILDINGS = {
			new Building('A', -47, -13, -43, -9, Direction.EAST),
			new Building('B', -49, -6, -43, -1, Direction.EAST),
			new Building('C', -48, 3, -43, 8, Direction.EAST),
			new Building('A', -47, 10, -43, 14, Direction.EAST),
			new Building('C', 42, -13, 47, -8, Direction.WEST),
			new Building('A', 43, -4, 47, 0, Direction.WEST),
			new Building('B', 41, 4, 47, 9, Direction.WEST),
			new Building('A', 43, 10, 47, 14, Direction.WEST),
			new Building('A', -42, 24, -38, 28, Direction.EAST),
			new Building('C', -35, 24, -30, 29, Direction.EAST),
			new Building('B', 30, 24, 36, 29, Direction.WEST),
			new Building('A', 38, 24, 42, 28, Direction.WEST)};

	private void buildings(Canvas c) {
		for (Building b : BUILDINGS) {
			if (!c.near((b.x1() + b.x2()) / 2, (b.z1() + b.z2()) / 2, 10)) {
				continue;
			}
			switch (b.style()) {
				case 'A' -> townhouse(c, b);
				case 'B' -> admin(c, b);
				default -> coastal(c, b);
			}
		}
	}

	/** The door cell of a building: the middle of the wall on its door side. */
	static int[] doorCell(Building b) {
		int mx = (b.x1() + b.x2()) / 2, mz = (b.z1() + b.z2()) / 2;
		return switch (b.door()) {
			case EAST -> new int[]{b.x2(), mz};
			case WEST -> new int[]{b.x1(), mz};
			case SOUTH -> new int[]{mx, b.z2()};
			default -> new int[]{mx, b.z1()};
		};
	}

	/** Building A: a 5 x 5 townhouse four storeys high, pale peach walls on terracotta floor bands, blue windows, red roof. */
	private void townhouse(Canvas c, Building b) {
		int top = G + 12;
		for (int x = b.x1(); x <= b.x2(); x++) {
			for (int z = b.z1(); z <= b.z2(); z++) {
				boolean ex = x == b.x1() || x == b.x2(), ez = z == b.z1() || z == b.z2();
				c.set(x, G, z, s(Blocks.SPRUCE_PLANKS));
				for (int y = G + 1; y <= top; y++) {
					if (!ex && !ez) {
						c.set(x, y, z, y == top ? s(Blocks.SPRUCE_PLANKS) : s(Blocks.AIR));
						continue;
					}
					int col = ex ? z - b.z1() : x - b.x1();
					int k = (y - G - 1) % 3;
					boolean window = !(ex && ez) && (col == 1 || col == 3) && k >= 1 && y < top;
					c.set(x, y, z, k == 0 || y == top ? s(Blocks.TERRACOTTA) : window ? s(Blocks.LIGHT_BLUE_STAINED_GLASS) : s(Blocks.WHITE_TERRACOTTA));
				}
			}
		}
		gable(c, b.x1() - 1, b.z1() - 1, b.x2() + 1, b.z2() + 1, top + 1, Blocks.BRICK_STAIRS, Blocks.BRICK_SLAB, s(Blocks.WHITE_TERRACOTTA));
		int[] d = doorCell(b);
		Kit.door(c, d[0], G + 1, d[1], Blocks.DARK_OAK_DOOR, b.door().getOpposite());
		c.set(d[0] + b.door().getStepX(), G + 3, d[1] + b.door().getStepZ(),
				s(Blocks.DARK_OAK_TRAPDOOR).setValue(net.minecraft.world.level.block.TrapDoorBlock.FACING, b.door())
						.setValue(net.minecraft.world.level.block.TrapDoorBlock.HALF, net.minecraft.world.level.block.state.properties.Half.TOP));
		c.set((b.x1() + b.x2()) / 2, top - 1, (b.z1() + b.z2()) / 2, CloudtopIsles.lantern(Blocks.LANTERN, true));
	}

	/**
	 * Building B: a white administrative block (about 7 x 6, 10 high) with polished diorite corners and bands, rows
	 * of cyan windows, small arches along the ground floor of its front and a dark-green (dark prismarine) hip roof.
	 */
	private void admin(Canvas c, Building b) {
		int top = G + 10;
		for (int x = b.x1(); x <= b.x2(); x++) {
			for (int z = b.z1(); z <= b.z2(); z++) {
				boolean ex = x == b.x1() || x == b.x2(), ez = z == b.z1() || z == b.z2();
				c.set(x, G, z, s(Blocks.SMOOTH_STONE));
				for (int y = G + 1; y <= top; y++) {
					if (!ex && !ez) {
						c.set(x, y, z, y == top ? s(Blocks.SMOOTH_QUARTZ) : s(Blocks.AIR));
						continue;
					}
					int col = ex ? z - b.z1() : x - b.x1();
					int span = ex ? b.z2() - b.z1() : b.x2() - b.x1();
					boolean corner = ex && ez;
					boolean frontFace = b.door() == Direction.EAST ? x == b.x2() : b.door() == Direction.WEST ? x == b.x1() : false;
					BlockState st;
					if (corner || y == G + 4 || y == top) {
						st = s(Blocks.POLISHED_DIORITE);
					} else if ((y == G + 6 || y == G + 7 || y == G + 8) && col % 2 == 1 && col < span) {
						st = s(Blocks.CYAN_STAINED_GLASS);
					} else if (frontFace && y <= G + 2 && col % 2 == 1 && col < span) {
						st = s(Blocks.AIR); // a small arch
					} else if (frontFace && y == G + 3 && col % 2 == 1 && col < span) {
						st = Kit.stairs(Blocks.STONE_BRICK_STAIRS, ex ? Direction.NORTH : Direction.EAST, true);
					} else if (!frontFace && y == G + 2 && col % 2 == 1 && col < span) {
						st = s(Blocks.CYAN_STAINED_GLASS);
					} else {
						st = s(Blocks.SMOOTH_QUARTZ);
					}
					c.set(x, y, z, st);
				}
			}
		}
		// a stone brick cornice
		for (int x = b.x1() - 1; x <= b.x2() + 1; x++) {
			for (int z = b.z1() - 1; z <= b.z2() + 1; z++) {
				boolean ring = x == b.x1() - 1 || x == b.x2() + 1 || z == b.z1() - 1 || z == b.z2() + 1;
				boolean cornerOut = (x == b.x1() - 1 || x == b.x2() + 1) && (z == b.z1() - 1 || z == b.z2() + 1);
				if (ring && !cornerOut) {
					Direction out = x == b.x1() - 1 ? Direction.WEST : x == b.x2() + 1 ? Direction.EAST : z == b.z1() - 1 ? Direction.NORTH : Direction.SOUTH;
					c.set(x, top, z, Kit.stairs(Blocks.STONE_BRICK_STAIRS, out.getOpposite(), true));
				}
			}
		}
		Kit.hipRoof(c, b.x1() - 1, b.z1() - 1, b.x2() + 1, b.z2() + 1, top + 1, Blocks.DARK_PRISMARINE_STAIRS, Blocks.DARK_PRISMARINE_SLAB, Blocks.DARK_PRISMARINE);
		c.set((b.x1() + b.x2()) / 2, top - 1, (b.z1() + b.z2()) / 2, CloudtopIsles.lantern(Blocks.LANTERN, true));
	}

	/**
	 * Building C: a 6 x 6 coastal house, brick lower walls, smooth sandstone above in a stripped spruce frame, tall
	 * light-blue windows, a dark prismarine gable roof with a small dormer on each slope.
	 */
	private void coastal(Canvas c, Building b) {
		int top = G + 8;
		for (int x = b.x1(); x <= b.x2(); x++) {
			for (int z = b.z1(); z <= b.z2(); z++) {
				boolean ex = x == b.x1() || x == b.x2(), ez = z == b.z1() || z == b.z2();
				c.set(x, G, z, s(Blocks.SPRUCE_PLANKS));
				for (int y = G + 1; y <= top; y++) {
					if (!ex && !ez) {
						c.set(x, y, z, y == top ? s(Blocks.SPRUCE_PLANKS) : s(Blocks.AIR));
						continue;
					}
					int col = ex ? z - b.z1() : x - b.x1();
					BlockState st;
					if (ex && ez) {
						st = s(Blocks.STRIPPED_SPRUCE_LOG);
					} else if (y == G + 4 || y == top) {
						st = s(Blocks.STRIPPED_SPRUCE_LOG).setValue(RotatedPillarBlock.AXIS, ex ? Direction.Axis.Z : Direction.Axis.X);
					} else if (y >= G + 5 && y <= G + 7 && (col == 1 || col == 4)) {
						st = y == G + 7 ? s(Blocks.LIGHT_BLUE_STAINED_GLASS) : s(Blocks.LIGHT_BLUE_STAINED_GLASS);
					} else if (y <= G + 3) {
						st = y == G + 2 && (col == 1 || col == 4) ? s(Blocks.LIGHT_BLUE_STAINED_GLASS) : s(Blocks.BRICKS);
					} else {
						st = s(Blocks.SMOOTH_SANDSTONE);
					}
					c.set(x, y, z, st);
				}
			}
		}
		gable(c, b.x1() - 1, b.z1() - 1, b.x2() + 1, b.z2() + 1, top + 1, Blocks.DARK_PRISMARINE_STAIRS, Blocks.DARK_PRISMARINE_SLAB, s(Blocks.SMOOTH_SANDSTONE));
		// dormers: a small window box on each slope (the ridge runs along x)
		int mx = (b.x1() + b.x2()) / 2;
		for (int side : new int[]{-1, 1}) {
			int z = side < 0 ? b.z1() : b.z2();
			c.set(mx, top + 2, z, s(Blocks.LIGHT_BLUE_STAINED_GLASS));
			c.set(mx, top + 3, z, Kit.slab(Blocks.DARK_PRISMARINE_SLAB, false));
		}
		int[] d = doorCell(b);
		Kit.door(c, d[0], G + 1, d[1], Blocks.DARK_OAK_DOOR, b.door().getOpposite());
		c.set((b.x1() + b.x2()) / 2, top - 1, (b.z1() + b.z2()) / 2, CloudtopIsles.lantern(Blocks.LANTERN, true));
	}

	/** A gable roof (ridge along x) over the box, overhang included; the gable ends filled with {@code fill}, every row. */
	static void gable(Canvas c, int x1, int z1, int x2, int z2, int y, Block stairs, Block slab, BlockState fill) {
		for (int i = 0; z1 + i <= z2 - i; i++) {
			int za = z1 + i, zb = z2 - i, yy = y + i;
			if (za == zb) {
				c.fill(x1, yy, za, x2, yy, za, Kit.slab(slab, false));
				break;
			}
			c.fill(x1, yy, za, x2, yy, za, Kit.stairs(stairs, Direction.SOUTH, false));
			c.fill(x1, yy, zb, x2, yy, zb, Kit.stairs(stairs, Direction.NORTH, false));
			if (za + 1 <= zb - 1) {
				c.fill(x1 + 1, yy, za + 1, x2 - 1, yy, zb - 1, s(Blocks.AIR));
				c.fill(x1 + 1, yy, za + 1, x1 + 1, yy, zb - 1, fill);
				c.fill(x2 - 1, yy, za + 1, x2 - 1, yy, zb - 1, fill);
			}
		}
	}

	// ------------------------------------------------------------------------------------------- platforms and posts

	/** Small round platforms in the harbour (5 across, 4 above the sea): some with a tree, some with a cannon. */
	private void coastalPlatforms(Canvas c) {
		int[][] spots = {{-16, 47, 1}, {16, 47, 1}, {-36, 44, 0}, {36, 44, 0}, {-62, 8, 1}, {62, 8, 1}};
		for (int[] p : spots) {
			if (!c.near(p[0], p[1], 6)) {
				continue;
			}
			for (int dx = -3; dx <= 3; dx++) {
				for (int dz = -3; dz <= 3; dz++) {
					double r = Math.hypot(dx, dz);
					if (r > 2.5) {
						continue;
					}
					int x = p[0] + dx, z = p[1] + dz;
					boolean rim = r > 1.5;
					c.fill(x, 50, z, x, G - 1, z, rim && (dx + dz) % 2 == 0 ? s(Blocks.CALCITE) : s(Blocks.SMOOTH_QUARTZ));
					c.set(x, G - 2, z, s(Blocks.POLISHED_ANDESITE)); // a dark band
					c.set(x, G, z, p[2] == 0 ? s(Blocks.GRASS_BLOCK) : rim ? s(Blocks.SMOOTH_QUARTZ) : s(Blocks.POLISHED_ANDESITE));
				}
			}
			if (p[2] == 0) {
				shadeTree(c, p[0], G, p[1], p[0] + p[1]);
			} else {
				cannon(c, p[0], G + 1, p[1], cardinal(angle(p[0], p[1])));
			}
		}
	}

	/**
	 * Short white guard posts: two inside the wall (flat roofs) and two on the green land behind it (lean-to spruce
	 * roofs), each with narrow dark openings and a lantern on a blackstone bracket.
	 */
	private void guardPosts(Canvas c) {
		int[][] posts = {{-19, -27, 1}, {16, -27, 1}, {-18, -54, 0}, {15, -54, 0}};
		for (int[] p : posts) {
			if (!c.near(p[0] + 2, p[1] + 2, 6)) {
				continue;
			}
			int y = p[2] == 1 ? G : Math.max(c.ground(p[0] + 1, p[1] + 1), G - 1);
			for (int x = p[0]; x <= p[0] + 3; x++) {
				for (int z = p[1]; z <= p[1] + 3; z++) {
					boolean ex = x == p[0] || x == p[0] + 3, ez = z == p[1] || z == p[1] + 3;
					c.fill(x, 56, z, x, y, z, s(Blocks.POLISHED_ANDESITE));
					for (int k = 1; k <= 4; k++) {
						if (!ex && !ez) {
							c.set(x, y + k, z, s(Blocks.AIR));
							continue;
						}
						int col = ex ? z - p[1] : x - p[0];
						boolean slit = !(ex && ez) && col == 1 && (k == 2 || k == 3);
						c.set(x, y + k, z, slit ? s(Blocks.AIR) : ex && ez ? s(Blocks.SMOOTH_QUARTZ) : s(Blocks.CALCITE));
					}
					if (p[2] == 1) {
						c.set(x, y + 5, z, ex || ez ? Kit.slab(Blocks.POLISHED_ANDESITE_SLAB, false) : s(Blocks.AIR));
						c.set(x, y + 4, z, ex || ez ? s(Blocks.POLISHED_ANDESITE) : s(Blocks.SPRUCE_PLANKS));
					}
				}
			}
			if (p[2] == 0) {
				// a lean-to of spruce, sloping south
				// the back (north) half rises one more course; the roof steps down from it to the front
				c.fill(p[0], y + 5, p[1], p[0] + 3, y + 5, p[1] + 1, s(Blocks.CALCITE));
				for (int x = p[0] - 1; x <= p[0] + 4; x++) {
					for (int z = p[1] - 1; z <= p[1] + 4; z++) {
						int k = z - (p[1] - 1);
						BlockState roof = k <= 1 ? Kit.slab(Blocks.SPRUCE_SLAB, false) : k == 2 ? Kit.stairs(Blocks.SPRUCE_STAIRS, Direction.NORTH, false)
								: Kit.slab(Blocks.SPRUCE_SLAB, false);
						c.set(x, k <= 1 ? y + 6 : y + 5, z, roof);
					}
				}
			}
			Kit.door(c, p[0] + 2, y + 1, p[1] + 3, Blocks.SPRUCE_DOOR, Direction.NORTH);
			c.set(p[0] + 3, y + 3, p[1] + 4, s(Blocks.POLISHED_BLACKSTONE_WALL));
			c.set(p[0] + 3, y + 2, p[1] + 4, CloudtopIsles.lantern(Blocks.LANTERN, true));
		}
	}

	/** The harbour's guns: field cannons along the front, big ones by the doors and on the wing corners, rows by the HQ. */
	private void artillery(Canvas c) {
		int y = G + 1;
		for (int x : new int[]{-8, -5, 5, 8}) {
			cannon(c, x, y, 34, Direction.SOUTH);
		}
		for (int x : new int[]{-28, -24, 24, 28}) {
			cannon(c, x, y, 28, Direction.SOUTH);
		}
		for (int z : new int[]{-4, 2}) {
			cannon(c, -15, y, z, Direction.WEST);
			cannon(c, 15, y, z, Direction.EAST);
		}
		for (int side : new int[]{-1, 1}) {
			plinth(c, side * 8, 15, y);
			cannon(c, side * 8, y + 1, 15, Direction.SOUTH);
			plinth(c, side * 39, -13, y);
			cannon(c, side * 39, y + 1, -13, side < 0 ? Direction.WEST : Direction.EAST);
		}
		c.set(0, y, 22, CloudtopIsles.lantern(Blocks.LANTERN, false));
	}

	// ------------------------------------------------------------------------------------------------- landscape

	/** Bridges to the southern tower islets, broad shade trees on the green land and the islets, lamps on the plaza. */
	private void landscape(Canvas c) {
		for (int side : new int[]{-1, 1}) {
			for (int k = 45; k <= 50; k++) {
				int x = side * k;
				for (int z = 28; z <= 29; z++) {
					c.set(x, G, z, s(Blocks.DARK_OAK_PLANKS));
					c.fill(x, G + 1, z, x, G + 3, z, s(Blocks.AIR));
				}
				c.set(x, G + 1, 27, s(Blocks.DARK_OAK_FENCE));
				c.set(x, G + 1, 30, s(Blocks.DARK_OAK_FENCE));
				if (k == 45 || k == 50) {
					c.fill(x, 54, 27, x, G - 1, 27, s(Blocks.DARK_OAK_LOG));
					c.fill(x, 54, 30, x, G - 1, 30, s(Blocks.DARK_OAK_LOG));
				}
			}
		}
		int[][] trees = {{-44, -44}, {-28, -52}, {-6, -58}, {22, -50}, {40, -44}, {50, -34}, {-52, -32},
				{-62, 36}, {-52, 38}, {62, 36}, {52, 38}, {-40, -28}, {40, -28}};
		for (int i = 0; i < trees.length; i++) {
			int x = trees[i][0], z = trees[i][1];
			int gy = c.ground(x, z);
			if (gy > SEA + 1 && !inPlaza(x, z)) {
				shadeTree(c, x, gy, z, i * 3 + 1);
			}
		}
		for (int[] l : new int[][]{{-6, 24}, {6, 24}, {-6, 32}, {6, 32}, {-20, 8}, {20, 8}, {-20, -12}, {20, -12}}) {
			if (c.near(l[0], l[1], 1)) {
				Kit.lamp(c, l[0], l[1], G);
			}
		}
	}
}
