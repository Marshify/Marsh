package com.grandline.world.plan;

import com.grandline.world.plan.Anchor.Type;
import com.grandline.world.plan.Terrain.Ground;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.WallBannerBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Saltmarket Docks, rebuilt as a fortified pirate harbour town (about 220 x 225; see docs/PIRATE_HARBOR_BLUEPRINT.md).
 * <ul>
 * <li>Front (south, +z): a pale stone promenade under a long pink-brick harbour wall with arches, cannons and
 * battlements; three wooden piers; a tall white lighthouse on its own rock to the west, reached by a causeway.</li>
 * <li>Middle: the walled town on a level platform: a central hall with a veranda on the plaza, a windmill with blue
 * lattice sails, a tall green-roofed tower and eighteen timber-framed houses, each its own shape.</li>
 * <li>Rear (north): wild grassland behind the landward wall, with two worn clearings (the enemy camps; the boss stands at
 * the far end of the east one), rock outcrops and tree clusters, on a rugged rocky coast.</li>
 * </ul>
 * Island-local coordinates, +x east, +z south (toward the harbour). Every way in is an opening or an open door
 * (adventure mode). Older worlds keep the original Saltmarket ({@link FirstSeaPlans} legacy plan).
 */
public final class SaltmarketDocks extends BasePlan {
	/** Levels: sea 62, promenade 64, town platform 67, harbour wall walk 79, landward wall walk 77. */
	static final int PROM = 64, TOWN = 67, WALK = 79, BACK_WALK = 77;
	/** Town platform and walls (outer faces). */
	static final int WX1 = -74, WX2 = 71, WZ1 = -36, WZ2 = 51;
	/** Harbour wall (z 48..51), promenade (z 52..66). */
	static final int HZ1 = 48, HZ2 = 51, PZ1 = 52, PZ2 = 66;
	/** Lighthouse rock centre and top. */
	static final int LX = -100, LZ = 42, LTOP = PROM;

	public SaltmarketDocks() {
		super("Saltmarket Docks", 0.12);
		terrain
				// the rear wilderness: overlapping hills of different sizes, so it rolls instead of ringing
				.mass(-4, -82, 88, 62, 4.0, 3.0, Ground.GRASS)
				.mass(-52, -70, 30, 22, 7.0, 2.4, Ground.GRASS)
				.mass(-20, -118, 26, 20, 8.5, 2.0, Ground.GRASS)
				.mass(30, -66, 22, 18, 6.5, 2.6, Ground.GRASS)
				.mass(58, -96, 26, 30, 7.5, 2.2, Ground.GRASS)
				.mass(-2, -96, 12, 30, 10.0, 1.6, Ground.STONE)    // the central rock ridge
				.mass(-62, -112, 20, 16, 8.0, 2.8, Ground.GRASS)
				.mass(16, -50, 26, 12, 5.0, 3.0, Ground.GRASS)
				// three rear headlands
				.mass(-80, -126, 30, 24, 6.0, 2.0, Ground.GRASS)
				.mass(6, -142, 34, 16, 5.5, 2.0, Ground.GRASS)
				.mass(78, -120, 28, 30, 7.0, 2.0, Ground.GRASS)
				// rocky flanks
				.mass(-96, -56, 18, 22, 5.0, 2.0, Ground.STONE)
				.mass(92, -72, 14, 24, 6.0, 2.0, Ground.STONE)
				.mass(-30, -150, 10, 7, 4.0, 1.5, Ground.STONE)
				.mass(52, -150, 9, 8, 5.0, 1.5, Ground.STONE)
				// under the town
				.mass(-2, 6, 86, 52, 6.0, 4.0, Ground.GRASS)
				// the lighthouse rock
				.mass(LX, LZ, 14, 13, 4.0, 3.0, Ground.STONE)
				.platform(WX1, WZ1, WX2, HZ2, 4, TOWN, Ground.GRASS)
				.platform(-82, PZ1, 70, PZ2, 3, PROM, Ground.STONE)
				.flat(LX, LZ, 8, 8, LTOP, 3, null)
				.shoreMix(s(Blocks.STONE), s(Blocks.ANDESITE), s(Blocks.TUFF), s(Blocks.COBBLESTONE), s(Blocks.GRAVEL))
				.reach(150);
		anchor(Anchor.at(Type.QUEST, 0, TOWN + 1, 6, 0));
		anchor(Anchor.at(Type.HOME, 10, TOWN + 1, 3, 0));
		anchor(Anchor.at(Type.FRUIT_DEALER, -16, TOWN + 1, 18, 90));
		anchor(new Anchor(Type.ARRIVAL, 12, PROM + 1, 60, 0, 180));
		anchor(Anchor.at(Type.FISHERMAN, 12, PROM + 1, 108, 0));
		anchor(Anchor.at(Type.DOCK, 16, 63, 100));
		anchor(new Anchor(Type.ENEMY_CAMP, -45, Anchor.SURFACE, -95, 12));
		anchor(new Anchor(Type.ENEMY_CAMP, 42, Anchor.SURFACE, -92, 12));
		anchor(Anchor.area(Type.BOSS_ARENA, 46, -112, 6));
		anchor(Anchor.of(Type.TREASURE, -88, -118));
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

	/** The muted pink brick of the walls: bricks softened with granite. */
	static BlockState brick(int x, int y, int z) {
		double u = unit(x * 7 + y * 3, z * 5 - y, 3);
		return u < 0.55 ? s(Blocks.BRICKS) : u < 0.85 ? s(Blocks.POLISHED_GRANITE) : s(Blocks.GRANITE);
	}

	/** Rough coastal rock. */
	static BlockState rock(int x, int y, int z) {
		double u = unit(x * 3 + y * 11, z * 7 + y, 5);
		return u < 0.45 ? s(Blocks.STONE) : u < 0.7 ? s(Blocks.ANDESITE) : u < 0.88 ? s(Blocks.TUFF) : s(Blocks.COBBLESTONE);
	}

	static BlockState lantern(boolean hanging) {
		return s(Blocks.LANTERN).setValue(LanternBlock.HANGING, hanging);
	}

	static BlockState log(Block b, Direction.Axis axis) {
		return s(b).setValue(RotatedPillarBlock.AXIS, axis);
	}

	// ==================================================================================================== build

	@Override
	public void build(Canvas c) {
		coast(c);
		surfaces(c);
		harbourWall(c);
		sideAndBackWalls(c);
		promenade(c);
		piers(c);
		lighthouse(c);
		windmill(c);
		greenTower(c);
		hall(c);
		houses(c);
		plaza(c);
		tavern(c);
		wilderness(c);
		treasure(c);
	}

	/**
	 * Rocky coast: wherever the ground drops away (cliffs, ledges, the sea edge), the exposed face is rock instead of
	 * dirt, with a grass lip on top.
	 */
	private void coast(Canvas c) {
		net.minecraft.world.level.levelgen.structure.BoundingBox box = c.chunkBox();
		for (int wx = box.minX(); wx <= box.maxX(); wx++) {
			for (int wz = box.minZ(); wz <= box.maxZ(); wz++) {
				int x = wx - c.world(0, 0, 0).getX();
				int z = wz - c.world(0, 0, 0).getZ();
				int g = c.ground(x, z);
				if (g <= Terrain.WATER_TOP || (x >= WX1 && x <= WX2 && z >= WZ1 && z <= PZ2)) {
					continue; // sea, and the built-up town and promenade
				}
				int low = Math.min(Math.min(c.ground(x + 1, z), c.ground(x - 1, z)), Math.min(c.ground(x, z + 1), c.ground(x, z - 1)));
				low = Math.min(low, Math.min(c.ground(x + 2, z), c.ground(x, z + 2)));
				low = Math.min(low, Math.min(c.ground(x - 2, z), c.ground(x, z - 2)));
				if (low < g - 1 || g <= Terrain.WATER_TOP + 3) {
					int from = Math.max(Terrain.WATER_TOP - 4, low - 2);
					for (int y = from; y < g; y++) {
						c.set(x, y, z, rock(x, y, z));
					}
					if (g - low >= 4 && unit(x, z, 9) < 0.5) {
						c.set(x, g, z, rock(x, g, z)); // bare rock edges on the higher cliffs
					}
				}
			}
		}
	}

	/** Paving of the town platform (sandstone and packed mud, worn into patches) and of the promenade. */
	private void surfaces(Canvas c) {
		for (int x = WX1; x <= WX2; x++) {
			for (int z = WZ1; z <= HZ2; z++) {
				if (!c.inChunk(x, z)) {
					continue;
				}
				double patch = unit(Math.floorDiv(x, 6), Math.floorDiv(z, 6), 21);
				double u = unit(x, z, 22);
				c.set(x, TOWN, z, u < 0.8 ? s(Blocks.SMOOTH_SANDSTONE)
						: u < 0.94 ? s(Blocks.SANDSTONE) : s(Blocks.CUT_SANDSTONE));
				// retaining face of the platform toward the rear and sides (pale stone)
				c.fill(x, TOWN - 6, z, x, TOWN - 1, z, s(Blocks.STONE_BRICKS));
			}
		}
		for (int x = -82; x <= 70; x++) {
			for (int z = PZ1; z <= PZ2; z++) {
				if (!c.inChunk(x, z) || !terrainHas(c, x, z, PROM)) {
					continue;
				}
				boolean edge = z == PZ2 || z == PZ2 - 1 && (x + z) % 2 == 0;
				c.set(x, PROM, z, z == PZ2 ? s(Blocks.SMOOTH_STONE) : unit(x, z, 23) < 0.8 ? s(Blocks.SMOOTH_SANDSTONE) : s(Blocks.CUT_SANDSTONE));
				c.fill(x, Terrain.WATER_TOP - 4, z, x, PROM - 1, z, z >= PZ2 - 1 ? s(Blocks.STONE_BRICKS) : s(Blocks.SANDSTONE));
				if (edge && z == PZ2) {
					c.set(x, PROM + 1, z, x % 6 == 0 ? s(Blocks.STONE_BRICK_WALL) : Kit.slab(Blocks.SMOOTH_STONE_SLAB, false));
				}
			}
		}
	}

	private static boolean terrainHas(Canvas c, int x, int z, int y) {
		return c.ground(x, z) == y;
	}

	// ---------------------------------------------------------------------------------------------- harbour wall

	/**
	 * The harbour wall (x WX1..WX2, z 48..51): pink brick from the promenade to the wall walk at y 79, battlements on
	 * the sea side, a parapet on the town side; eleven arches on the sea face (a deep alcove each, the middle one a
	 * gateway with steps up into the town); pale stone arch borders and pillars; cannons and blue banners.
	 */
	private void harbourWall(Canvas c) {
		for (int x = WX1; x <= WX2; x++) {
			for (int z = HZ1; z <= HZ2; z++) {
				if (!c.inChunk(x, z)) {
					continue;
				}
				for (int y = Terrain.WATER_TOP - 3; y <= WALK; y++) {
					c.set(x, y, z, brick(x, y, z));
				}
				c.set(x, WALK, z, s(Blocks.SMOOTH_STONE));
				c.fill(x, WALK + 1, z, x, WALK + 4, z, s(Blocks.AIR));
			}
			// pale string courses
			c.set(x, PROM + 1, HZ2, s(Blocks.STONE_BRICKS));
			c.set(x, WALK - 1, HZ2, s(Blocks.SMOOTH_STONE));
			// battlements (sea side) and a low parapet (town side)
			c.set(x, WALK + 1, HZ2, s(Blocks.STONE_BRICKS));
			if (Math.floorMod(x, 2) == 0) {
				c.set(x, WALK + 2, HZ2, s(Blocks.STONE_BRICKS));
			}
			c.set(x, WALK + 1, HZ1, s(Blocks.STONE_BRICK_WALL));
		}
		// Arches: centres every 13 blocks; the middle one (x 0) is the gateway.
		for (int i = -5; i <= 5; i++) {
			int ax = i * 13;
			if (ax - 3 < WX1 + 4 || ax + 3 > WX2 - 4) {
				continue;
			}
			arch(c, ax, i == 0);
			// pillar between arches: pale stone, with a lantern
			int px = ax + 6;
			if (i < 5 && px + 1 < WX2 - 4) {
				c.fill(px, PROM + 1, HZ2 + 1, px + 1, WALK - 2, HZ2 + 1, s(Blocks.STONE_BRICKS));
				c.set(px, PROM + 5, HZ2 + 2, lantern(false));
				c.set(px, PROM + 4, HZ2 + 2, s(Blocks.STONE_BRICK_WALL));
				c.fill(px, PROM + 1, HZ2 + 2, px, PROM + 3, HZ2 + 2, s(Blocks.STONE_BRICK_WALL));
			}
			// blue banner over the arch
			banner(c, ax, WALK - 3, HZ2 + 1, Direction.SOUTH, Blocks.BLUE_WALL_BANNER);
			// a cannon on the wall walk above each arch, aimed out to sea
			cannon(c, ax + 3, HZ2 - 1, Direction.SOUTH);
		}
		// Ladders up the sea face of the wall at both ends (on the bricks), through a gap in the battlements.
		for (int lx : new int[]{WX1 + 6, WX2 - 6}) {
			for (int y = PROM + 1; y <= WALK; y++) {
				c.set(lx, y, HZ2 + 1, s(Blocks.LADDER).setValue(LadderBlock.FACING, Direction.SOUTH));
			}
			c.fill(lx, WALK + 1, HZ2, lx, WALK + 3, HZ2, s(Blocks.AIR));
			c.set(lx, WALK, HZ2, s(Blocks.SMOOTH_STONE));
		}
	}

	/**
	 * One arch in the sea face, centred on x {@code ax}: 5 wide, 9 high with a rounded top of stairs, framed in pale
	 * stone. An alcove 3 deep (crates and barrels), or for the gateway a passage right through, with steps up to the town.
	 */
	private void arch(Canvas c, int ax, boolean gate) {
		int y0 = PROM + 1;
		int depth = gate ? 4 : 3;
		int[] open = {6, 8, 9, 8, 6};                     // opening height per column, dx -2..2
		for (int dx = -2; dx <= 2; dx++) {
			int h = open[dx + 2];
			for (int d = 0; d < depth; d++) {
				int z = HZ2 - d;
				for (int y = y0; y < y0 + h; y++) {
					c.set(ax + dx, y, z, s(Blocks.AIR));
				}
				c.set(ax + dx, y0 - 1, z, s(Blocks.SMOOTH_SANDSTONE));
			}
		}
		// a pale stone border that follows the curve: jambs, curved shoulders (upside-down stairs whose lower half sits on the
		// outer, lower side, so each step rounds the corner instead of notching it) and a keystone
		for (int y = y0; y <= y0 + 5; y++) {
			c.set(ax - 3, y, HZ2, s(Blocks.SMOOTH_QUARTZ));
			c.set(ax + 3, y, HZ2, s(Blocks.SMOOTH_QUARTZ));
		}
		c.set(ax - 3, y0 + 6, HZ2, s(Blocks.SMOOTH_QUARTZ));
		c.set(ax + 3, y0 + 6, HZ2, s(Blocks.SMOOTH_QUARTZ));
		c.set(ax - 2, y0 + 6, HZ2, stair(Blocks.QUARTZ_STAIRS, Direction.WEST, true));
		c.set(ax + 2, y0 + 6, HZ2, stair(Blocks.QUARTZ_STAIRS, Direction.EAST, true));
		c.set(ax - 2, y0 + 7, HZ2, s(Blocks.SMOOTH_QUARTZ));
		c.set(ax + 2, y0 + 7, HZ2, s(Blocks.SMOOTH_QUARTZ));
		c.set(ax - 1, y0 + 8, HZ2, stair(Blocks.QUARTZ_STAIRS, Direction.WEST, true));
		c.set(ax + 1, y0 + 8, HZ2, stair(Blocks.QUARTZ_STAIRS, Direction.EAST, true));
		c.set(ax - 1, y0 + 9, HZ2, s(Blocks.SMOOTH_QUARTZ));
		c.set(ax + 1, y0 + 9, HZ2, s(Blocks.SMOOTH_QUARTZ));
		c.set(ax, y0 + 9, HZ2, s(Blocks.CHISELED_QUARTZ_BLOCK));
		if (gate) {
			// steps up from the promenade (y 64) to the town (y 67) inside the passage: they climb north (towards the
			// town), so each step faces north (its tall half on the town side)
			for (int dx = -2; dx <= 2; dx++) {
				c.set(ax + dx, PROM + 1, HZ1 + 2, stair(Blocks.SANDSTONE_STAIRS, Direction.NORTH, false));
				c.set(ax + dx, PROM + 1, HZ1 + 1, s(Blocks.SMOOTH_SANDSTONE));
				c.set(ax + dx, PROM + 2, HZ1 + 1, stair(Blocks.SANDSTONE_STAIRS, Direction.NORTH, false));
				c.set(ax + dx, PROM + 1, HZ1, s(Blocks.SMOOTH_SANDSTONE));
				c.set(ax + dx, PROM + 2, HZ1, s(Blocks.SMOOTH_SANDSTONE));
				c.set(ax + dx, PROM + 3, HZ1, stair(Blocks.SANDSTONE_STAIRS, Direction.NORTH, false));
				for (int y = PROM + 4; y < y0 + 8; y++) {
					c.set(ax + dx, y, HZ1, s(Blocks.AIR));
				}
			}
			// torches on the passage walls
			c.set(ax - 2, y0 + 5, HZ1 + 1, s(Blocks.WALL_TORCH).setValue(net.minecraft.world.level.block.WallTorchBlock.FACING, Direction.EAST));
			c.set(ax + 2, y0 + 5, HZ1 + 1, s(Blocks.WALL_TORCH).setValue(net.minecraft.world.level.block.WallTorchBlock.FACING, Direction.WEST));
		} else {
			// cargo in the alcove
			c.set(ax - 1, y0, HZ2 - 2, Canvas.facing(s(Blocks.BARREL), Direction.UP));
			c.set(ax + 1, y0, HZ2 - 2, s(Blocks.SPRUCE_PLANKS));
			c.set(ax + 1, y0 + 1, HZ2 - 2, Canvas.facing(s(Blocks.BARREL), Direction.UP));
			c.set(ax, y0 + 4, HZ2 - 2, lantern(false));
			c.set(ax, y0 + 3, HZ2 - 2, s(Blocks.SPRUCE_FENCE));
		}
	}

	/** A cannon on a gun carriage at (x, WALK + 1, z), aimed {@code dir} through the battlements. */
	private void cannon(Canvas c, int x, int z, Direction dir) {
		int y = WALK + 1;
		c.set(x, y, z, stair(Blocks.DARK_OAK_STAIRS, dir.getOpposite(), false));
		c.set(x, y, z + dir.getStepZ() * -1 + 0, s(Blocks.DARK_OAK_SLAB));
		c.set(x, y + 1, z, log(Blocks.POLISHED_BASALT, dir.getAxis()));
		c.set(x + dir.getStepX(), y + 1, z + dir.getStepZ(), log(Blocks.POLISHED_BASALT, dir.getAxis()));
		c.set(x, y + 1, z - dir.getStepZ(), s(Blocks.POLISHED_BLACKSTONE_BUTTON));
	}

	static void banner(Canvas c, int x, int y, int z, Direction facing, Block banner) {
		c.set(x, y, z, s(banner).setValue(WallBannerBlock.FACING, facing));
	}

	// ---------------------------------------------------------------------------------------------- side and back walls

	/**
	 * The west and east walls (4 thick, wall walk y 79) with corner bastions, and the landward wall at the back (wall
	 * walk y 77) with an open arched gateway (x -4..4) into the wilderness and a tower either side.
	 */
	private void sideAndBackWalls(Canvas c) {
		for (int z = WZ1; z < HZ1; z++) {
			for (int t = 0; t < 4; t++) {
				wallColumn(c, WX1 + t, z, WALK, t == 0, t == 3);
				wallColumn(c, WX2 - t, z, WALK, t == 0, t == 3);
			}
		}
		for (int x = WX1 + 4; x <= WX2 - 4; x++) {
			for (int t = 0; t < 4; t++) {
				int z = WZ1 + t;
				if (Math.abs(x) <= 4) {
					continue; // the gateway
				}
				wallColumn(c, x, z, BACK_WALK, t == 0, t == 3);
			}
		}
		// the landward gateway: an arch over the opening and a tower either side
		for (int x = -4; x <= 4; x++) {
			int h = Math.abs(x) == 4 ? 5 : Math.abs(x) == 3 ? 7 : 8;
			for (int t = 0; t < 4; t++) {
				int z = WZ1 + t;
				for (int y = TOWN + h + 1; y <= BACK_WALK; y++) {
					c.set(x, y, z, brick(x, y, z));
				}
				c.set(x, BACK_WALK, z, s(Blocks.SMOOTH_STONE));
				c.set(x, TOWN + h, z, Math.abs(x) <= 2 ? s(Blocks.SMOOTH_QUARTZ) : stair(Blocks.QUARTZ_STAIRS, x < 0 ? Direction.EAST : Direction.WEST, true));
				c.set(x, TOWN, z, s(Blocks.SMOOTH_SANDSTONE));
			}
		}
		for (int tx : new int[]{-10, 10}) {
			gateTower(c, tx, WZ1 + 1);
		}
		// corner bastions
		bastion(c, WX1 + 1, HZ1 - 1);
		bastion(c, WX2 - 1, HZ1 - 1);
		bastion(c, WX1 + 1, WZ1 + 1);
		bastion(c, WX2 - 1, WZ1 + 1);
		// sea gate in the west wall toward the lighthouse causeway (at the promenade's west end)
		for (int z = 40; z <= 44; z++) {
			for (int y = TOWN + 1; y <= TOWN + 4; y++) {
				for (int t = 0; t < 4; t++) {
					c.set(WX1 + t, y, z, s(Blocks.AIR));
				}
			}
			for (int t = 0; t < 4; t++) {
				c.set(WX1 + t, TOWN, z, s(Blocks.SMOOTH_SANDSTONE));
				c.set(WX1 + t, TOWN + 5, z, s(Blocks.SMOOTH_QUARTZ));
			}
		}
		// stairs from the west sea gate down to the causeway
		for (int i = 0; i < 4; i++) {
			for (int z = 40; z <= 44; z++) {
				c.set(WX1 - 1 - i, TOWN - i, z, stair(Blocks.STONE_BRICK_STAIRS, Direction.EAST, false));
				c.fill(WX1 - 1 - i, Terrain.WATER_TOP - 3, z, WX1 - 1 - i, TOWN - i - 1, z, s(Blocks.STONE_BRICKS));
				c.fill(WX1 - 1 - i, TOWN - i + 1, z, WX1 - 1 - i, TOWN + 3, z, s(Blocks.AIR));
			}
		}
		causeway(c);
	}

	/** One column of curtain wall up to {@code walk}: outer battlements, inner parapet. */
	private void wallColumn(Canvas c, int x, int z, int walk, boolean outer, boolean inner) {
		if (!c.inChunk(x, z)) {
			return;
		}
		int from = Math.min(c.ground(x, z), TOWN) - 4;
		for (int y = from; y < walk; y++) {
			c.set(x, y, z, brick(x, y, z));
		}
		c.set(x, walk, z, s(Blocks.SMOOTH_STONE));
		c.fill(x, walk + 1, z, x, walk + 4, z, s(Blocks.AIR));
		if (outer) {
			c.set(x, walk + 1, z, s(Blocks.STONE_BRICKS));
			if (Math.floorMod(x + z, 2) == 0) {
				c.set(x, walk + 2, z, s(Blocks.STONE_BRICKS));
			}
			c.set(x, walk - 2, z, s(Blocks.SMOOTH_STONE));
		}
		if (inner) {
			c.set(x, walk + 1, z, s(Blocks.STONE_BRICK_WALL));
		}
	}

	/** A square gate tower (7 x 7) with a stair inside up to its roof terrace. */
	private void gateTower(Canvas c, int cx, int cz) {
		int top = BACK_WALK + 6;
		for (int x = cx - 3; x <= cx + 3; x++) {
			for (int z = cz - 3; z <= cz + 3; z++) {
				boolean edge = Math.abs(x - cx) == 3 || Math.abs(z - cz) == 3;
				int from = Math.min(c.ground(x, z), TOWN) - 4;
				for (int y = from; y <= top; y++) {
					c.set(x, y, z, edge || y <= TOWN || y == top ? brick(x, y, z) : s(Blocks.AIR));
				}
				c.set(x, top, z, s(Blocks.SMOOTH_STONE));
				if (edge) {
					c.set(x, top + 1, z, s(Blocks.STONE_BRICKS));
					if (Math.floorMod(x + z, 2) == 0) {
						c.set(x, top + 2, z, s(Blocks.STONE_BRICKS));
					}
					if ((z == cz || x == cx && z == cz + 3) && (top - 4) > TOWN) {   // not on the north face (the ladder's wall)
						c.set(x, top - 4, z, s(Blocks.BLUE_STAINED_GLASS_PANE));
					}
				}
			}
		}
		// doorway from the town side, and a ladder up the inside
		int dz = cz + 3;
		c.fill(cx, TOWN + 1, dz, cx, TOWN + 3, dz, s(Blocks.AIR));
		for (int y = TOWN + 1; y < top; y++) {
			c.set(cx, y, cz - 2, s(Blocks.LADDER).setValue(LadderBlock.FACING, Direction.SOUTH));
		}
		c.set(cx, top, cz - 2, s(Blocks.AIR));
		c.set(cx + 1, TOWN + 1, cz, lantern(false));
		// open onto the landward wall walk
		for (int t = 0; t < 4; t++) {
			int side = cx < 0 ? cx + 3 : cx - 3;
			c.fill(side, BACK_WALK + 1, WZ1 + t, side, BACK_WALK + 3, WZ1 + t, s(Blocks.AIR));
		}
	}

	/** A corner bastion: a 9 x 9 platform raised over the wall walk, with cannons on two sea-facing sides. */
	private void bastion(Canvas c, int cx, int cz) {
		int top = WALK + 2;
		for (int x = cx - 4; x <= cx + 4; x++) {
			for (int z = cz - 4; z <= cz + 4; z++) {
				int from = Math.min(c.ground(x, z), TOWN) - 4;
				for (int y = from; y < top; y++) {
					c.set(x, y, z, brick(x, y, z));
				}
				c.set(x, top, z, s(Blocks.SMOOTH_STONE));
				c.fill(x, top + 1, z, x, top + 4, z, s(Blocks.AIR));
				boolean edge = Math.abs(x - cx) == 4 || Math.abs(z - cz) == 4;
				if (edge) {
					c.set(x, top + 1, z, s(Blocks.STONE_BRICKS));
					if (Math.floorMod(x + z, 2) == 0) {
						c.set(x, top + 2, z, s(Blocks.STONE_BRICKS));
					}
				}
			}
		}
		// steps down to the wall walk on the town side
		int sx = cx < 0 ? cx + 4 : cx - 4;
		int sz = cz;
		c.set(sx, top + 1, sz, s(Blocks.AIR));
		c.set(sx, top + 2, sz, s(Blocks.AIR));
		c.set(sx, top, sz, Kit.slab(Blocks.SMOOTH_STONE_SLAB, false));
		c.set(cx, top + 1, cz, s(Blocks.STONE_BRICK_WALL));
		c.set(cx, top + 2, cz, lantern(false));
	}

	/** A stone causeway from the west sea gate across the channel to the lighthouse rock. */
	private void causeway(Canvas c) {
		int y = PROM;
		for (int x = LX + 7; x <= WX1 - 5; x++) {
			for (int z = 40; z <= 44; z++) {
				c.fill(x, Terrain.FLOOR + 5, z, x, y - 1, z, z == 40 || z == 44 ? s(Blocks.STONE_BRICKS) : s(Blocks.STONE));
				c.set(x, y, z, z == 40 || z == 44 ? s(Blocks.STONE_BRICKS) : s(Blocks.POLISHED_ANDESITE));
				c.fill(x, y + 1, z, x, y + 4, z, s(Blocks.AIR));
				if ((z == 40 || z == 44)) {
					c.set(x, y + 1, z, Math.floorMod(x, 5) == 0 ? s(Blocks.STONE_BRICK_WALL) : Kit.slab(Blocks.STONE_BRICK_SLAB, false));
					if (Math.floorMod(x, 10) == 0) {
						c.set(x, y + 2, z, lantern(false));
					}
				}
			}
		}
		// from the causeway up onto the lighthouse rock
		for (int x = LX + 2; x < LX + 7; x++) {
			for (int z = 40; z <= 44; z++) {
				c.set(x, y, z, s(Blocks.POLISHED_ANDESITE));
				c.fill(x, y + 1, z, x, y + 4, z, s(Blocks.AIR));
			}
		}
	}

	// ---------------------------------------------------------------------------------------------- promenade and piers

	/** Promenade dressing: cargo stacks, benches, lamp posts and mooring bollards along the quay. */
	private void promenade(Canvas c) {
		for (int x = -78; x <= 66; x += 9) {
			Kit.lamp(c, x, PZ2 - 2, PROM);
		}
		int[][] cargo = {{-60, 56}, {-34, 58}, {-8, 56}, {28, 57}, {52, 56}};
		for (int[] k : cargo) {
			c.set(k[0], PROM + 1, k[1], Canvas.facing(s(Blocks.BARREL), Direction.UP));
			c.set(k[0] + 1, PROM + 1, k[1], s(Blocks.SPRUCE_PLANKS));
			c.set(k[0] + 1, PROM + 2, k[1], Canvas.facing(s(Blocks.BARREL), Direction.UP));
			c.set(k[0], PROM + 1, k[1] + 1, s(Blocks.HAY_BLOCK));
			c.set(k[0] + 2, PROM + 1, k[1] + 1, stair(Blocks.SPRUCE_STAIRS, Direction.NORTH, false));
		}
		for (int x = -70; x <= 64; x += 16) {
			c.set(x, PROM + 1, PZ1 + 1, stair(Blocks.DARK_OAK_STAIRS, Direction.NORTH, false));
			c.set(x + 1, PROM + 1, PZ1 + 1, stair(Blocks.DARK_OAK_STAIRS, Direction.NORTH, false));
		}
	}

	/** Three wooden piers (P1 x -20, P2 x 12, P3 x 44) with posts, rails, lanterns, mooring posts and a crane. */
	private void piers(Canvas c) {
		pier(c, -20, 104, 2);
		pier(c, 12, 110, 2);
		pier(c, 44, 84, 1);
		// side platforms off P2 and P1
		deck(c, 15, 92, 20, 98);
		deck(c, -27, 84, -23, 90);
		crane(c, 16, 95);
		// mooring bollards along the quay edge
		for (int x = -76; x <= 66; x += 12) {
			c.set(x, PROM + 1, PZ2, s(Blocks.POLISHED_BLACKSTONE_WALL));
		}
	}

	private void pier(Canvas c, int x, int endZ, int half) {
		int y = PROM;
		for (int z = PZ2 + 1; z <= endZ; z++) {
			for (int w = -half; w <= half; w++) {
				c.set(x + w, y, z, s(Blocks.SPRUCE_PLANKS));
				c.fill(x + w, y + 1, z, x + w, y + 4, z, s(Blocks.AIR));
				boolean edge = Math.abs(w) == half;
				if (edge && (z - PZ2) % 4 == 0) {
					c.fill(x + w, Terrain.FLOOR + 5, z, x + w, y - 1, z, log(Blocks.SPRUCE_LOG, Direction.Axis.Y));
					c.set(x + w, y + 1, z, s(Blocks.SPRUCE_FENCE));
					if ((z - PZ2) % 8 == 0) {
						c.set(x + w, y + 2, z, lantern(false));
					}
				} else if (edge) {
					c.set(x + w, y + 1, z, Kit.connected(Blocks.SPRUCE_FENCE, false));
				}
			}
		}
		// the end: a T-head
		for (int w = -half - 2; w <= half + 2; w++) {
			for (int z = endZ - 2; z <= endZ; z++) {
				c.set(x + w, y, z, s(Blocks.SPRUCE_PLANKS));
				c.fill(x + w, y + 1, z, x + w, y + 4, z, s(Blocks.AIR));
			}
			c.fill(x + w, Terrain.FLOOR + 5, endZ, x + w, y - 1, endZ, Math.abs(w) == half + 2 ? log(Blocks.SPRUCE_LOG, Direction.Axis.Y)
					: s(Blocks.WATER));
		}
		c.set(x - half - 2, y + 1, endZ, s(Blocks.SPRUCE_FENCE));
		c.set(x + half + 2, y + 1, endZ, s(Blocks.SPRUCE_FENCE));
		c.set(x - half - 2, y + 2, endZ, lantern(false));
		c.set(x + half + 2, y + 2, endZ, lantern(false));
	}

	private void deck(Canvas c, int x1, int z1, int x2, int z2) {
		for (int x = x1; x <= x2; x++) {
			for (int z = z1; z <= z2; z++) {
				c.set(x, PROM, z, s(Blocks.SPRUCE_PLANKS));
				c.fill(x, PROM + 1, z, x, PROM + 4, z, s(Blocks.AIR));
				if ((x == x1 || x == x2) && (z == z1 || z == z2)) {
					c.fill(x, Terrain.FLOOR + 5, z, x, PROM - 1, z, log(Blocks.SPRUCE_LOG, Direction.Axis.Y));
				}
			}
		}
		c.set(x2, PROM + 1, z1 + 1, Canvas.facing(s(Blocks.BARREL), Direction.UP));
		c.set(x2, PROM + 1, z1 + 2, s(Blocks.HAY_BLOCK));
	}

	private void crane(Canvas c, int x, int z) {
		c.fill(x, PROM + 1, z, x, PROM + 9, z, log(Blocks.SPRUCE_LOG, Direction.Axis.Y));
		c.fill(x + 1, PROM + 9, z, x + 5, PROM + 9, z, log(Blocks.SPRUCE_LOG, Direction.Axis.X));
		c.set(x + 1, PROM + 8, z, stair(Blocks.SPRUCE_STAIRS, Direction.WEST, true));
		c.fill(x + 5, PROM + 5, z, x + 5, PROM + 8, z, s(Blocks.CHAIN));
		c.set(x + 5, PROM + 4, z, Canvas.facing(s(Blocks.BARREL), Direction.UP));
	}

	// ==================================================================================================== shared pieces

	static boolean inCircle(int dx, int dz, double r) {
		return dx * dx + dz * dz <= r * r + r * 0.6;
	}

	/** A steep stepped pyramid roof (two blocks up per step in): full blocks then a row of stairs. */
	static void steepRoof(Canvas c, int minX, int minZ, int maxX, int maxZ, int y, Block full, Block stairs) {
		int i = 0;
		int yy = y;
		while (minX + i <= maxX - i && minZ + i <= maxZ - i) {
			int x1 = minX + i, x2 = maxX - i, z1 = minZ + i, z2 = maxZ - i;
			c.fill(x1, yy, z1, x2, yy, z2, s(full));
			if (x1 == x2 || z1 == z2) {
				c.fill(x1, yy + 1, z1, x2, yy + 1, z2, s(full));
				break;
			}
			int top = yy + 1;
			if (stairs != null) {
				c.fill(x1, top, z1, x2, top, z1, stair(stairs, Direction.SOUTH, false));
				c.fill(x1, top, z2, x2, top, z2, stair(stairs, Direction.NORTH, false));
				c.fill(x1, top, z1 + 1, x1, top, z2 - 1, stair(stairs, Direction.EAST, false));
				c.fill(x2, top, z1 + 1, x2, top, z2 - 1, stair(stairs, Direction.WEST, false));
				c.fill(x1 + 1, top, z1 + 1, x2 - 1, top, z2 - 1, s(full));
			} else {
				c.fill(x1, top, z1, x2, top, z2, s(full));
			}
			yy += 2;
			i++;
		}
	}

	/**
	 * A gable roof of {@code stairs} over the box, ridge along x or z, rising one block per step (with the given
	 * overhang already included in the box); the gable ends are filled with {@code gable} up to the slopes.
	 */
	static void gable(Canvas c, int minX, int minZ, int maxX, int maxZ, int y, Block stairs, Block slab, BlockState gable, boolean ridgeAlongX) {
		int span = ridgeAlongX ? maxZ - minZ : maxX - minX;
		for (int i = 0; i <= span / 2; i++) {
			int yy = y + i;
			if (ridgeAlongX) {
				int za = minZ + i, zb = maxZ - i;
				if (za == zb) {
					c.fill(minX, yy, za, maxX, yy, za, Kit.slab(slab, false));
					break;
				}
				c.fill(minX, yy, za, maxX, yy, za, stair(stairs, Direction.SOUTH, false));
				c.fill(minX, yy, zb, maxX, yy, zb, stair(stairs, Direction.NORTH, false));
				if (zb - za > 1) {
					c.fill(minX + 1, yy, za + 1, minX + 1, yy, zb - 1, gable);
					c.fill(maxX - 1, yy, za + 1, maxX - 1, yy, zb - 1, gable);
					if (i > 0) {
						c.fill(minX + 2, yy, za + 1, maxX - 2, yy, zb - 1, s(Blocks.AIR)); // the attic (the top ceiling stays)
					}
				}
			} else {
				int xa = minX + i, xb = maxX - i;
				if (xa == xb) {
					c.fill(xa, yy, minZ, xa, yy, maxZ, Kit.slab(slab, false));
					break;
				}
				c.fill(xa, yy, minZ, xa, yy, maxZ, stair(stairs, Direction.EAST, false));
				c.fill(xb, yy, minZ, xb, yy, maxZ, stair(stairs, Direction.WEST, false));
				if (xb - xa > 1) {
					c.fill(xa + 1, yy, minZ + 1, xb - 1, yy, minZ + 1, gable);
					c.fill(xa + 1, yy, maxZ - 1, xb - 1, yy, maxZ - 1, gable);
					if (i > 0) {
						c.fill(xa + 1, yy, minZ + 2, xb - 1, yy, maxZ - 2, s(Blocks.AIR)); // the attic (the top ceiling stays)
					}
				}
			}
		}
	}

	/**
	 * Timber-framed plaster walls (outer box x1..x2, z1..z2) from {@code y} for {@code floors} storeys of
	 * {@code storey} blocks: dark oak corner and middle posts, a beam at every floor, diagonal braces in the end panels,
	 * blue windows in the middle panels, a pale stone foundation course, and plank floors inside.
	 */
	static void timber(Canvas c, int x1, int z1, int x2, int z2, int y, int floors, int storey) {
		int top = y + floors * storey;
		c.foundation(x1, z1, x2, z2, y, s(Blocks.STONE_BRICKS));
		for (int x = x1; x <= x2; x++) {
			for (int z = z1; z <= z2; z++) {
				boolean edgeX = x == x1 || x == x2, edgeZ = z == z1 || z == z2;
				if (!edgeX && !edgeZ) {
					for (int f = 0; f < floors; f++) {
						c.set(x, y + f * storey, z, s(Blocks.SPRUCE_PLANKS));
						c.fill(x, y + f * storey + 1, z, x, y + (f + 1) * storey - 1, z, s(Blocks.AIR));
					}
					c.set(x, top, z, s(Blocks.SPRUCE_PLANKS));
					continue;
				}
				int along = edgeZ ? x - x1 : z - z1;
				int len = edgeZ ? x2 - x1 : z2 - z1;
				boolean post = (edgeX && edgeZ) || along % 4 == 0 || along == len;
				for (int yy = y + 1; yy <= top; yy++) {
					int inStorey = (yy - y) % storey;
					BlockState b;
					if (post) {
						b = log(Blocks.STRIPPED_DARK_OAK_LOG, Direction.Axis.Y);
					} else if (inStorey == 0) {
						b = log(Blocks.STRIPPED_DARK_OAK_LOG, edgeZ ? Direction.Axis.X : Direction.Axis.Z);
					} else {
						b = s(Blocks.SMOOTH_SANDSTONE);
						int panel = along / 4;
						int panels = Math.max(1, (len + 3) / 4);
						int u = along % 4;                  // 1..3 inside a panel
						boolean braced = panels >= 3 ? (panel == 0 || panel == panels - 1) : panels == 2 && panel == 0 && storey >= 5;
						int rows = storey - 1;
						if (braced && rows >= 3) {
							// an X brace: corners at the bottom and top rows, the crossing in the middle
							boolean brace = ((inStorey == 1 || inStorey == rows) && u != 2) || (inStorey == (rows + 1) / 2 && u == 2);
							if (brace) {
								b = s(Blocks.DARK_OAK_PLANKS);
							}
						} else if (u == 2 && inStorey >= 1 && inStorey <= Math.min(2, rows - 1)) {
							b = Kit.connected(Blocks.BLUE_STAINED_GLASS_PANE, edgeZ);
						}
					}
					c.set(x, yy, z, b);
				}
			}
		}
	}

	/** An open doorway (3 high) through a wall at (x, y+1..y+3, z), with a lantern above on the outside. */
	static void doorway(Canvas c, int x, int y, int z, Direction out, int width) {
		Direction side = out.getClockWise();
		for (int w = 0; w < width; w++) {
			int dx = x + side.getStepX() * w, dz = z + side.getStepZ() * w;
			c.fill(dx, y + 1, dz, dx, y + 3, dz, s(Blocks.AIR));
			c.set(dx, y, dz, s(Blocks.DARK_OAK_PLANKS));
			c.set(dx + out.getStepX(), y, dz + out.getStepZ(), stair(Blocks.STONE_BRICK_STAIRS, out.getOpposite(), false));
		}
		// a torch on the wall above the doorway
		c.set(x + out.getStepX(), y + 4, z + out.getStepZ(), s(Blocks.WALL_TORCH).setValue(net.minecraft.world.level.block.WallTorchBlock.FACING, out));
	}

	// ==================================================================================================== landmarks

	/**
	 * The lighthouse on its rock: a dark stone base, a tall tapering white shaft (calcite, white concrete bands, dark
	 * trim rings, narrow blue windows), a balcony with dark railings, the lantern room and a pointed blue cone; inside,
	 * a ladder all the way up. About 64 tall, far above the town.
	 */
	private void lighthouse(Canvas c) {
		int cx = LX, cz = LZ;
		if (!c.near(cx, cz, 14)) {
			return;
		}
		// the paved round top of the rock
		for (int dx = -9; dx <= 9; dx++) {
			for (int dz = -9; dz <= 9; dz++) {
				if (inCircle(dx, dz, 8.5)) {
					c.set(cx + dx, LTOP, cz + dz, inCircle(dx, dz, 7.5) ? (Math.floorMod(dx + dz, 3) == 0 ? s(Blocks.STONE_BRICKS) : s(Blocks.POLISHED_ANDESITE))
							: s(Blocks.STONE_BRICKS));
					c.fill(cx + dx, LTOP + 1, cz + dz, cx + dx, LTOP + 70, cz + dz, s(Blocks.AIR));
					if (!inCircle(dx, dz, 7.5) && Math.floorMod(dx * 3 + dz, 4) == 0) {
						c.set(cx + dx, LTOP + 1, cz + dz, s(Blocks.STONE_BRICK_WALL));
					}
				}
			}
		}
		int base = LTOP + 1, shaft = base + 5, balcony = 113, room = balcony + 1, cone = room + 5;
		for (int y = base; y <= balcony; y++) {
			double r = y < shaft ? 6.5 : y < 92 ? 5 : 4.2;
			for (int dx = -7; dx <= 7; dx++) {
				for (int dz = -7; dz <= 7; dz++) {
					if (!inCircle(dx, dz, r)) {
						continue;
					}
					boolean wall = !inCircle(dx, dz, r - 1.2);
					BlockState b;
					if (y == balcony) {
						b = s(Blocks.POLISHED_DEEPSLATE);
					} else if (!wall) {
						b = s(Blocks.AIR);
					} else if (y < shaft) {
						b = y == shaft - 1 ? s(Blocks.POLISHED_DEEPSLATE) : s(Blocks.POLISHED_ANDESITE);
					} else if ((y - shaft) % 11 == 10) {
						b = s(Blocks.POLISHED_DEEPSLATE);                       // dark trim ring
					} else {
						b = (y - shaft) % 11 < 2 ? s(Blocks.WHITE_CONCRETE) : s(Blocks.CALCITE);
					}
					c.set(cx + dx, y, cz + dz, b);
				}
			}
			// narrow blue windows, two high, turning a quarter each level
			if (y >= shaft + 3 && (y - shaft) % 8 < 2) {
				int k = ((y - shaft) / 8) % 4;
				int rr = y < 92 ? 5 : 4;
				int[][] at = {{0, rr}, {rr, 0}, {0, -rr}, {-rr, 0}};
				for (int j = 0; j < 4; j += 2) {
					int[] w = at[(k + j) % 4];
					c.set(cx + w[0], y, cz + w[1], s(Blocks.BLUE_STAINED_GLASS));
				}
			}
		}
		// door toward the causeway (east), and the ladder inside
		c.fill(cx + 6, base, cz, cx + 6, base + 2, cz, s(Blocks.AIR));
		c.fill(cx + 5, base, cz, cx + 5, base + 2, cz, s(Blocks.AIR));
		c.set(cx + 7, base + 3, cz, s(Blocks.WALL_TORCH).setValue(net.minecraft.world.level.block.WallTorchBlock.FACING, Direction.EAST));
		for (int y = base; y < balcony; y++) {
			c.set(cx - 3, y, cz, s(Blocks.LADDER).setValue(LadderBlock.FACING, Direction.EAST));
			c.set(cx - 4, y, cz, y < shaft ? s(Blocks.POLISHED_ANDESITE) : s(Blocks.CALCITE));
		}
		c.set(cx - 3, balcony, cz, s(Blocks.AIR));
		// balcony: a wider ring with railings
		for (int dx = -7; dx <= 7; dx++) {
			for (int dz = -7; dz <= 7; dz++) {
				if (inCircle(dx, dz, 6.2) && !inCircle(dx, dz, 4.2)) {
					c.set(cx + dx, balcony, cz + dz, s(Blocks.POLISHED_DEEPSLATE));
					c.set(cx + dx, balcony - 1, cz + dz, stair(Blocks.POLISHED_DEEPSLATE_STAIRS,
							Math.abs(dx) > Math.abs(dz) ? (dx > 0 ? Direction.WEST : Direction.EAST) : (dz > 0 ? Direction.NORTH : Direction.SOUTH), true));
					if (!inCircle(dx, dz, 5.2)) {
						c.set(cx + dx, balcony + 1, cz + dz, s(Blocks.DARK_OAK_FENCE));
					}
				}
			}
		}
		// the lantern room: dark posts, glass, a bright light inside
		for (int y = room; y < cone; y++) {
			for (int dx = -3; dx <= 3; dx++) {
				for (int dz = -3; dz <= 3; dz++) {
					if (!inCircle(dx, dz, 2.8)) {
						continue;
					}
					boolean wall = !inCircle(dx, dz, 1.6);
					boolean post = wall && (Math.abs(dx) == Math.abs(dz) || dx == 0 || dz == 0) && (Math.abs(dx) + Math.abs(dz)) >= 3;
					c.set(cx + dx, y, cz + dz, !wall ? s(Blocks.AIR) : post && (Math.abs(dx) == Math.abs(dz)) ? s(Blocks.DARK_OAK_LOG)
							: s(Blocks.LIGHT_BLUE_STAINED_GLASS));
				}
			}
		}
		c.set(cx, room, cz, s(Blocks.POLISHED_DEEPSLATE));
		c.set(cx, room + 1, cz, s(Blocks.SEA_LANTERN));
		c.set(cx, room + 2, cz, s(Blocks.GLOWSTONE));
		// the blue cone
		double r = 4.2;
		for (int y = cone; r > 0.2; y++) {
			for (int dx = -5; dx <= 5; dx++) {
				for (int dz = -5; dz <= 5; dz++) {
					if (inCircle(dx, dz, r)) {
						c.set(cx + dx, y, cz + dz, r > 3 ? s(Blocks.BLUE_CONCRETE) : s(Blocks.BLUE_CONCRETE));
					}
				}
			}
			r -= 0.65;
		}
		// a dark banner hanging under the balcony toward the sea
		banner(c, cx, balcony - 2, cz + 5, Direction.SOUTH, Blocks.BLACK_WALL_BANNER);
		banner(c, cx + 5, balcony - 2, cz, Direction.EAST, Blocks.BLUE_WALL_BANNER);
	}

	/**
	 * The windmill west of the plaza: a 9 x 9 timber-framed body on a stone base narrowing to 7 x 7, a steep blue
	 * roof, and four open-lattice blue sails on an axle facing the harbour.
	 */
	private void windmill(Canvas c) {
		int cx = -40, cz = 4;
		if (!c.near(cx, cz, 26)) {
			return;
		}
		timber(c, cx - 4, cz - 4, cx + 4, cz + 4, TOWN, 3, 5);         // y 67..82
		for (int x = cx - 4; x <= cx + 4; x++) {                         // stone base course
			for (int z = cz - 4; z <= cz + 4; z++) {
				if (Math.abs(x - cx) == 4 || Math.abs(z - cz) == 4) {
					c.fill(x, TOWN + 1, z, x, TOWN + 2, z, Math.abs(x - cx) == 4 && Math.abs(z - cz) == 4 ? s(Blocks.STONE_BRICKS) : s(Blocks.COBBLESTONE));
				}
			}
		}
		timber(c, cx - 3, cz - 3, cx + 3, cz + 3, TOWN + 15, 3, 5);    // y 82..97
		steepRoof(c, cx - 4, cz - 4, cx + 4, cz + 4, TOWN + 31, Blocks.BLUE_CONCRETE, null);
		// overhang ring under the roof
		for (int x = cx - 4; x <= cx + 4; x++) {
			for (int z = cz - 4; z <= cz + 4; z++) {
				if (Math.abs(x - cx) == 4 || Math.abs(z - cz) == 4) {
					c.set(x, TOWN + 30, z, s(Blocks.DARK_OAK_PLANKS));
				}
			}
		}
		doorway(c, cx, TOWN, cz + 4, Direction.SOUTH, 1);
		for (int y = TOWN + 1; y < TOWN + 30; y++) {
			c.set(cx - 2, y, cz - 2, s(Blocks.LADDER).setValue(LadderBlock.FACING, Direction.SOUTH));
		}
		for (int f = 1; f <= 5; f++) {
			c.set(cx - 2, TOWN + f * 5, cz - 2, s(Blocks.LADDER).setValue(LadderBlock.FACING, Direction.SOUTH));
		}
		// axle and hub
		int hubY = TOWN + 25, hubZ = cz + 6;
		c.fill(cx, hubY, cz + 4, cx, hubY, hubZ, log(Blocks.DARK_OAK_LOG, Direction.Axis.Z));
		c.set(cx, hubY, hubZ + 1, s(Blocks.DARK_OAK_PLANKS));
		int sz = hubZ + 1;
		// four sails: a spar and an open lattice beside it (turning the same way round the hub)
		int[][] dirs = {{1, 0}, {0, 1}, {-1, 0}, {0, -1}};
		for (int k = 0; k < 4; k++) {
			int ax = dirs[k][0], ay = dirs[k][1];
			int px = dirs[(k + 1) % 4][0], py = dirs[(k + 1) % 4][1];   // the side the lattice hangs on
			for (int i = 1; i <= 15; i++) {
				c.set(cx + ax * i, hubY + ay * i, sz, log(Blocks.DARK_OAK_LOG, ax != 0 ? Direction.Axis.X : Direction.Axis.Y));
				if (i < 3) {
					continue;
				}
				for (int w = 1; w <= 5; w++) {
					boolean frame = i == 3 || i == 15 || w == 5 || (i - 3) % 3 == 0 || w == 1;
					if (frame) {
						c.set(cx + ax * i + px * w, hubY + ay * i + py * w, sz, s(Blocks.BLUE_CONCRETE));
					}
				}
			}
		}
	}

	/** The tall narrow green-roofed tower at the east edge of town: five floors, a ladder, a steep green spire. */
	private void greenTower(Canvas c) {
		int cx = 52, cz = -20;
		if (!c.near(cx, cz, 10)) {
			return;
		}
		timber(c, cx - 3, cz - 3, cx + 3, cz + 3, TOWN, 5, 6);          // y 67..97
		for (int x = cx - 4; x <= cx + 4; x++) {
			for (int z = cz - 4; z <= cz + 4; z++) {
				if (Math.abs(x - cx) == 4 || Math.abs(z - cz) == 4) {
					c.set(x, TOWN + 30, z, stair(Blocks.DARK_OAK_STAIRS, Math.abs(x - cx) == 4 ? (x > cx ? Direction.WEST : Direction.EAST)
							: (z > cz ? Direction.NORTH : Direction.SOUTH), true));
				}
			}
		}
		steepRoof(c, cx - 4, cz - 4, cx + 4, cz + 4, TOWN + 31, Blocks.DARK_PRISMARINE, Blocks.DARK_PRISMARINE_STAIRS);
		c.fill(cx, TOWN + 41, cz, cx, TOWN + 43, cz, s(Blocks.DARK_OAK_FENCE));
		c.set(cx, TOWN + 44, cz, s(Blocks.LIGHTNING_ROD));
		doorway(c, cx, TOWN, cz + 3, Direction.SOUTH, 1);
		for (int y = TOWN + 1; y < TOWN + 30; y++) {
			c.set(cx - 2, y, cz - 2, s(Blocks.LADDER).setValue(LadderBlock.FACING, Direction.SOUTH));
		}
	}

	/**
	 * The central hall on the north side of the plaza (x -14..14, z -24..-4): two storeys, a broad orange roof over the
	 * west part and a railed roof terrace with a flagpole over the east end; a veranda on posts along the plaza side.
	 */
	private void hall(Canvas c) {
		int x1 = -14, x2 = 14, z1 = -24, z2 = -4;
		if (!c.near(0, -14, 26)) {
			return;
		}
		timber(c, x1, z1, x2, z2, TOWN, 2, 8);                            // floors y 67 and 75, top 83
		gable(c, x1 - 1, z1 - 1, 7, z2 + 1, TOWN + 16, Blocks.ACACIA_STAIRS, Blocks.ACACIA_SLAB, s(Blocks.SMOOTH_SANDSTONE), true);
		// roof terrace over the east end
		for (int x = 8; x <= x2; x++) {
			for (int z = z1; z <= z2; z++) {
				c.set(x, TOWN + 16, z, s(Blocks.DARK_OAK_PLANKS));
				if (x == x2 || z == z1 || z == z2) {
					c.set(x, TOWN + 17, z, Kit.connected(Blocks.DARK_OAK_FENCE, z == z1 || z == z2));
				}
			}
		}
		c.fill(12, TOWN + 17, -14, 12, TOWN + 32, -14, s(Blocks.DARK_OAK_FENCE));
		c.fill(13, TOWN + 28, -14, 15, TOWN + 31, -14, s(Blocks.BLUE_WOOL));
		c.set(12, TOWN + 33, -14, s(Blocks.LANTERN));
		// main doorway (3 wide) onto the veranda, and a side door east
		doorway(c, -1, TOWN, z2, Direction.SOUTH, 3);
		// side door east: an open door in a timber frame, a step and a torch
		c.fill(x2, TOWN + 1, -12, x2, TOWN + 2, -12, s(Blocks.AIR));
		StonewallPrisonV9.door(c, x2, TOWN + 1, -12, Blocks.DARK_OAK_DOOR, Direction.EAST);
		c.set(x2, TOWN + 3, -12, log(Blocks.STRIPPED_DARK_OAK_LOG, Direction.Axis.Z));
		c.set(x2 + 1, TOWN, -12, stair(Blocks.STONE_BRICK_STAIRS, Direction.WEST, false));
		c.set(x2 + 1, TOWN + 3, -12, s(Blocks.WALL_TORCH).setValue(net.minecraft.world.level.block.WallTorchBlock.FACING, Direction.EAST));
		// veranda: dark oak deck on the plaza side, posts, a sloped acacia roof
		for (int x = x1; x <= x2; x++) {
			for (int z = z2 + 1; z <= z2 + 5; z++) {
				c.set(x, TOWN, z, s(Blocks.DARK_OAK_PLANKS));
				c.fill(x, TOWN + 1, z, x, TOWN + 5, z, s(Blocks.AIR));
			}
			c.set(x, TOWN + 6, z2 + 1, stair(Blocks.ACACIA_STAIRS, Direction.NORTH, false));
			c.set(x, TOWN + 5, z2 + 2, stair(Blocks.ACACIA_STAIRS, Direction.NORTH, false));
			c.set(x, TOWN + 5, z2 + 3, stair(Blocks.ACACIA_STAIRS, Direction.NORTH, false));
			c.set(x, TOWN + 4, z2 + 4, stair(Blocks.ACACIA_STAIRS, Direction.NORTH, false));
			c.set(x, TOWN + 4, z2 + 5, stair(Blocks.ACACIA_STAIRS, Direction.NORTH, false));
			if ((x - x1) % 4 == 0) {
				c.fill(x, TOWN + 1, z2 + 5, x, TOWN + 3, z2 + 5, log(Blocks.STRIPPED_DARK_OAK_LOG, Direction.Axis.Y));
				c.set(x, TOWN + 3, z2 + 4, lantern(true));            // hanging from the veranda roof
			} else if (Math.abs(x) > 2) {
				c.set(x, TOWN + 1, z2 + 5, Kit.connected(Blocks.DARK_OAK_FENCE, true));
			}
		}
		for (int x = -2; x <= 2; x++) {
			c.set(x, TOWN, z2 + 6, stair(Blocks.DARK_OAK_STAIRS, Direction.NORTH, false));
		}
		// upper floor: an opening for the stair (interiors come later)
		c.fill(-12, TOWN + 8, -22, -9, TOWN + 8, -22, s(Blocks.AIR));
	}

	// ==================================================================================================== houses

	/** One house of the town: footprint, storeys, roof colour and ridge, which way its door faces, and extras. */
	record House(int x1, int z1, int x2, int z2, int floors, boolean orange, boolean ridgeX, Direction door, String extras) {
	}

	static final House[] HOUSES = {
			new House(-64, 10, -54, 20, 2, true, true, Direction.SOUTH, "chimney shedW"),
			new House(-52, 14, -44, 24, 3, false, false, Direction.EAST, ""),
			new House(-30, 16, -22, 26, 2, true, true, Direction.EAST, "awning"),
			new House(-66, 26, -56, 38, 2, false, false, Direction.EAST, "chimney"),
			new House(-50, 30, -40, 40, 1, true, true, Direction.NORTH, "barrels"),
			new House(24, 4, 34, 14, 2, false, true, Direction.WEST, "awning"),
			new House(38, 2, 46, 12, 3, true, false, Direction.WEST, "chimney"),
			new House(24, 20, 32, 30, 2, true, false, Direction.WEST, ""),
			new House(36, 18, 48, 28, 2, false, true, Direction.NORTH, "wing"),
			new House(50, 30, 62, 40, 2, true, true, Direction.NORTH, "chimney"),
			new House(-20, 34, -10, 42, 2, false, true, Direction.NORTH, ""),
			new House(6, 34, 16, 42, 2, true, true, Direction.NORTH, "barrels"),
			new House(-66, -28, -56, -16, 2, true, false, Direction.EAST, "shedS"),
			new House(-52, -28, -44, -18, 3, false, false, Direction.SOUTH, ""),
			new House(22, -30, 32, -20, 2, true, true, Direction.SOUTH, "chimney"),
			new House(36, -28, 44, -18, 2, false, false, Direction.SOUTH, ""),
			new House(58, -6, 66, 4, 2, true, false, Direction.WEST, "chimney"),
			new House(-30, -20, -22, -10, 2, true, true, Direction.SOUTH, "")};

	private void houses(Canvas c) {
		for (House h : HOUSES) {
			if (c.near((h.x1() + h.x2()) / 2, (h.z1() + h.z2()) / 2, 18)) {
				house(c, h);
			}
		}
	}

	private void house(Canvas c, House h) {
		int storey = 4;
		int top = TOWN + h.floors() * storey;
		timber(c, h.x1(), h.z1(), h.x2(), h.z2(), TOWN, h.floors(), storey);
		Block roof = h.orange() ? Blocks.ACACIA_STAIRS : Blocks.DARK_PRISMARINE_STAIRS;
		Block slab = h.orange() ? Blocks.ACACIA_SLAB : Blocks.DARK_PRISMARINE_SLAB;
		gable(c, h.x1() - 1, h.z1() - 1, h.x2() + 1, h.z2() + 1, top, roof, slab, s(Blocks.SMOOTH_SANDSTONE), h.ridgeX());
		// door in the middle of its side, standing open, with a step and a lantern
		int dx, dz;
		switch (h.door()) {
			case NORTH -> { dx = (h.x1() + h.x2()) / 2; dz = h.z1(); }
			case SOUTH -> { dx = (h.x1() + h.x2()) / 2; dz = h.z2(); }
			case WEST -> { dx = h.x1(); dz = (h.z1() + h.z2()) / 2; }
			default -> { dx = h.x2(); dz = (h.z1() + h.z2()) / 2; }
		}
		if (Math.floorMod(dx - h.x1(), 4) == 0 && h.door().getAxis() == Direction.Axis.Z) {
			dx++; // not on a post
		}
		if (Math.floorMod(dz - h.z1(), 4) == 0 && h.door().getAxis() == Direction.Axis.X) {
			dz++;
		}
		StonewallPrisonV9.door(c, dx, TOWN + 1, dz, Blocks.DARK_OAK_DOOR, h.door());
		c.set(dx, TOWN + 3, dz, log(Blocks.STRIPPED_DARK_OAK_LOG, h.door().getAxis() == Direction.Axis.Z ? Direction.Axis.X : Direction.Axis.Z));
		c.set(dx + h.door().getStepX(), TOWN + 3, dz + h.door().getStepZ(), s(Blocks.LANTERN).setValue(LanternBlock.HANGING, false));
		c.set(dx + h.door().getStepX(), TOWN + 2, dz + h.door().getStepZ(), s(Blocks.AIR));
		c.set(dx + h.door().getStepX(), TOWN + 3, dz + h.door().getStepZ(), s(Blocks.AIR));
		c.set(dx + h.door().getStepX() + h.door().getClockWise().getStepX(), TOWN + 3, dz + h.door().getStepZ() + h.door().getClockWise().getStepZ(),
				lantern(false));
		c.set(dx + h.door().getStepX() + h.door().getClockWise().getStepX(), TOWN + 2, dz + h.door().getStepZ() + h.door().getClockWise().getStepZ(),
				s(Blocks.DARK_OAK_FENCE));
		c.set(dx + h.door().getStepX() + h.door().getClockWise().getStepX(), TOWN + 1, dz + h.door().getStepZ() + h.door().getClockWise().getStepZ(),
				s(Blocks.DARK_OAK_FENCE));
		// inside: ladder up through the floors, a lamp, simple furniture
		int lx = h.x1() + 1, lz = h.z1() + 1;
		if (h.door() == Direction.WEST && dz <= lz + 1 || h.door() == Direction.NORTH && dx <= lx + 1) {
			lx = h.x2() - 1;
			lz = h.z2() - 1;
		}
		for (int y = TOWN + 1; y < top; y++) {
			c.set(lx, y, lz, s(Blocks.LADDER).setValue(LadderBlock.FACING, lz == h.z1() + 1 ? Direction.SOUTH : Direction.NORTH));
		}
		Kit.furnish(c, h.x1() + 2, h.z1() + 2, h.x2() - 2, h.z2() - 2, TOWN, Kit.Interior.HOME, h.door());
		for (int f = 0; f < h.floors(); f++) {
			c.set((h.x1() + h.x2()) / 2, TOWN + f * storey + storey - 1, (h.z1() + h.z2()) / 2, lantern(true));
		}
		if (h.extras().contains("chimney")) {
			int cx = h.ridgeX() ? h.x2() - 1 : (h.x1() + h.x2()) / 2 + 1;
			int cz = h.ridgeX() ? (h.z1() + h.z2()) / 2 + 1 : h.z2() - 1;
			int ridge = top + ((h.ridgeX() ? h.z2() - h.z1() : h.x2() - h.x1()) + 2) / 2;
			c.fill(cx, top, cz, cx, ridge + 2, cz, s(Blocks.STONE_BRICKS));
			c.set(cx, ridge + 3, cz, s(Blocks.STONE_BRICK_WALL));
			c.set(cx, ridge + 2, cz, s(Blocks.CAMPFIRE));
			c.set(cx, ridge + 1, cz, s(Blocks.STONE_BRICKS));
		}
		if (h.extras().contains("shedW") || h.extras().contains("shedS")) {
			boolean west = h.extras().contains("shedW");
			int sx1 = west ? h.x1() - 5 : h.x1() + 2, sx2 = west ? h.x1() : h.x1() + 7;
			int sz1 = west ? h.z1() + 2 : h.z2(), sz2 = west ? h.z1() + 7 : h.z2() + 5;
			timber(c, sx1, sz1, sx2, sz2, TOWN, 1, 4);
			gable(c, sx1 - 1, sz1 - 1 + (west ? 0 : 1), sx2 + (west ? 0 : 1), sz2 + 1, TOWN + 4, roof, slab, s(Blocks.SMOOTH_SANDSTONE), !west);
			if (west) {
				c.fill(h.x1(), TOWN + 1, sz1 + 2, h.x1(), TOWN + 2, sz1 + 3, s(Blocks.AIR));
				c.fill(sx1, TOWN + 1, sz1 + 2, sx1, TOWN + 2, sz1 + 2, s(Blocks.AIR));
			} else {
				c.fill(sx1 + 2, TOWN + 1, h.z2(), sx1 + 3, TOWN + 2, h.z2(), s(Blocks.AIR));
				c.fill(sx1 + 2, TOWN + 1, sz2, sx1 + 2, TOWN + 2, sz2, s(Blocks.AIR));
			}
			c.set(sx1 + 1, TOWN + 1, sz1 + 1, Canvas.facing(s(Blocks.BARREL), Direction.UP));
			c.set(sx1 + 1, TOWN + 1, sz2 - 1, s(Blocks.HAY_BLOCK));
		}
		if (h.extras().contains("wing")) {
			timber(c, 40, 28, 45, 35, TOWN, 1, 4);
			gable(c, 39, 28, 46, 36, TOWN + 4, roof, slab, s(Blocks.SMOOTH_SANDSTONE), false);
			c.fill(42, TOWN + 1, 28, 43, TOWN + 2, 28, s(Blocks.AIR));
		}
		if (h.extras().contains("awning")) {
			Direction d = h.door();
			Direction side = d.getClockWise();
			for (int w = -3; w <= 3; w++) {
				for (int k = 1; k <= 3; k++) {
					int ax = dx + d.getStepX() * k + side.getStepX() * w, az = dz + d.getStepZ() * k + side.getStepZ() * w;
					c.set(ax, TOWN + 4, az, Math.floorMod(w, 2) == 0 ? s(Blocks.BLUE_WOOL) : s(Blocks.WHITE_WOOL));
					if (k == 3 && Math.abs(w) == 3) {
						c.fill(ax, TOWN + 1, az, ax, TOWN + 3, az, s(Blocks.DARK_OAK_FENCE));
					}
				}
			}
			c.set(dx + d.getStepX() * 2 + side.getStepX() * 2, TOWN + 1, dz + d.getStepZ() * 2 + side.getStepZ() * 2, Canvas.facing(s(Blocks.BARREL), Direction.UP));
			c.set(dx + d.getStepX() * 2 - side.getStepX() * 2, TOWN + 1, dz + d.getStepZ() * 2 - side.getStepZ() * 2, s(Blocks.CARTOGRAPHY_TABLE));
		}
		if (h.extras().contains("barrels")) {
			int bx = h.x2() + 1, bz = h.z2() - 1;
			c.set(bx, TOWN + 1, bz, Canvas.facing(s(Blocks.BARREL), Direction.UP));
			c.set(bx, TOWN + 1, bz - 1, Canvas.facing(s(Blocks.BARREL), Direction.UP));
			c.set(bx, TOWN + 2, bz, Canvas.facing(s(Blocks.BARREL), Direction.UP));
		}
	}

	// ==================================================================================================== the plaza

	/**
	 * The plaza between the hall and the harbour gate (x -20..20, z 2..30): a paved square with four planted trees,
	 * benches, lamp posts, a well, and strings of little pennants between the lamp posts.
	 */
	private void plaza(Canvas c) {
		if (!c.near(0, 16, 30)) {
			return;
		}
		for (int x = -20; x <= 20; x++) {
			for (int z = 3; z <= 31; z++) {
				boolean ring = Math.abs(x) == 20 || z == 3 || z == 31;
				double u = unit(x, z, 41);
				c.set(x, TOWN, z, ring ? s(Blocks.SMOOTH_STONE) : Math.floorMod(x + z, 2) == 0 ? (u < 0.85 ? s(Blocks.SMOOTH_SANDSTONE) : s(Blocks.SANDSTONE))
						: (u < 0.85 ? s(Blocks.CUT_SANDSTONE) : s(Blocks.SMOOTH_SANDSTONE)));
			}
		}
		// the road from the plaza down to the harbour gate
		for (int x = -3; x <= 3; x++) {
			for (int z = 32; z < HZ1; z++) {
				c.set(x, TOWN, z, Math.abs(x) == 3 ? s(Blocks.SMOOTH_STONE) : s(Blocks.CUT_SANDSTONE));
			}
		}
		for (int[] p : new int[][]{{-13, 11}, {13, 11}, {-13, 24}, {13, 24}}) {
			planter(c, p[0], p[1]);
		}
		Kit.well(c, 0, 18, TOWN);
		for (int[] b : new int[][]{{-6, 12}, {6, 12}, {-6, 24}, {6, 24}}) {
			c.set(b[0], TOWN + 1, b[1], stair(Blocks.DARK_OAK_STAIRS, b[0] < 0 ? Direction.WEST : Direction.EAST, false));
			c.set(b[0], TOWN + 1, b[1] + 1, stair(Blocks.DARK_OAK_STAIRS, b[0] < 0 ? Direction.WEST : Direction.EAST, false));
		}
		int[][] poles = {{-19, 4}, {19, 4}, {-19, 30}, {19, 30}, {-19, 17}, {19, 17}};
		for (int[] p : poles) {
			c.fill(p[0], TOWN + 1, p[1], p[0], TOWN + 6, p[1], s(Blocks.DARK_OAK_FENCE));
			c.set(p[0], TOWN + 7, p[1], lantern(false));
		}
		pennants(c, -18, 4, 18, 4, TOWN + 6);
		pennants(c, -18, 30, 18, 30, TOWN + 6);
		pennants(c, -18, 17, 18, 17, TOWN + 7);
	}

	private void planter(Canvas c, int x, int z) {
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				c.set(x + dx, TOWN + 1, z + dz, Math.abs(dx) + Math.abs(dz) == 0 ? s(Blocks.GRASS_BLOCK) : s(Blocks.STONE_BRICKS));
				if (Math.abs(dx) + Math.abs(dz) == 1) {
					c.set(x + dx, TOWN + 1, z + dz, s(Blocks.GRASS_BLOCK));
					c.set(x + dx, TOWN + 2, z + dz, Math.floorMod(x + dx + z, 2) == 0 ? s(Blocks.RED_TULIP) : s(Blocks.CORNFLOWER));
				}
			}
		}
		c.fill(x, TOWN + 2, z, x, TOWN + 5, z, log(Blocks.OAK_LOG, Direction.Axis.Y));
		BlockState leaf = Kit.leaves(Blocks.OAK_LEAVES);
		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) {
				for (int dy = 5; dy <= 7; dy++) {
					if (dx * dx + dz * dz + (dy - 6) * (dy - 6) * 2 <= 5 && !(dx == 0 && dz == 0 && dy == 5)) {
						c.set(x + dx, TOWN + dy, z + dz, leaf);
					}
				}
			}
		}
	}

	/** A string of small pennants: a chain from (x1, z) to (x2, z) at {@code y} with coloured flags hanging under it. */
	private void pennants(Canvas c, int x1, int z1, int x2, int z2, int y) {
		Block[] flags = {Blocks.BLUE_STAINED_GLASS_PANE, Blocks.WHITE_STAINED_GLASS_PANE, Blocks.RED_STAINED_GLASS_PANE, Blocks.YELLOW_STAINED_GLASS_PANE};
		for (int x = x1; x <= x2; x++) {
			int sag = (int) Math.round(Math.sin((x - x1) / (double) (x2 - x1) * Math.PI) * 1.4);
			c.set(x, y - sag, z1, s(Blocks.CHAIN).setValue(net.minecraft.world.level.block.ChainBlock.AXIS, Direction.Axis.X));
			if (Math.floorMod(x, 2) == 0) {
				c.set(x, y - sag - 1, z1, s(flags[Math.floorMod(x / 2, flags.length)]));
			}
		}
	}

	// ==================================================================================================== interiors

	/**
	 * The hall inside. Ground floor: the tavern (dark wood floor with a raised end, heavy posts, round tables and stools,
	 * rugs, barrels and shelves, an L-shaped counter under a red canopy, blue chains from the beams). Upper floor: the
	 * kitchen (black and white checkerboard, a brick-based island, a brick oven, cupboards and shelves), reached by a
	 * ladder from the tavern.
	 */
	private void tavern(Canvas c) {
		if (!c.near(0, -14, 20)) {
			return;
		}
		int y = TOWN, up = TOWN + 8;
		for (int x = -13; x <= 13; x++) {
			for (int z = -23; z <= -5; z++) {
				c.set(x, y, z, Math.floorMod(x, 3) == 0 ? s(Blocks.DARK_OAK_PLANKS) : s(Blocks.SPRUCE_PLANKS));
				c.set(x, up, z, Math.floorMod(x + z, 2) == 0 ? s(Blocks.BLACK_CONCRETE) : s(Blocks.WHITE_CONCRETE));
				c.set(x, up - 1, z, Math.floorMod(x, 4) == 0 ? log(Blocks.STRIPPED_DARK_OAK_LOG, Direction.Axis.Z) : s(Blocks.SMOOTH_SANDSTONE));
			}
		}
		// raised west end, two steps up
		for (int x = -13; x <= -8; x++) {
			for (int z = -23; z <= -5; z++) {
				c.set(x, y + 1, z, x == -8 ? Kit.slab(Blocks.DARK_OAK_SLAB, false) : s(Blocks.DARK_OAK_PLANKS));
			}
		}
		// heavy posts
		for (int x = -8; x <= 12; x += 6) {
			for (int z : new int[]{-18, -10}) {
				c.fill(x, y + 1, z, x, up - 1, z, log(Blocks.STRIPPED_DARK_OAK_LOG, Direction.Axis.Y));
			}
		}
		// round tables (post and top) with stools
		for (int[] t : new int[][]{{-3, -16}, {3, -16}, {-3, -9}, {3, -9}, {9, -13}}) {
			c.set(t[0], y + 1, t[1], s(Blocks.DARK_OAK_FENCE));
			c.set(t[0], y + 2, t[1], s(Blocks.DARK_OAK_PRESSURE_PLATE));
			for (Direction d : Direction.Plane.HORIZONTAL) {
				c.set(t[0] + d.getStepX(), y + 1, t[1] + d.getStepZ(), stair(Blocks.SPRUCE_STAIRS, d, false));
			}
			c.set(t[0] + 1, y + 1, t[1] + 1, s(Blocks.BLUE_CARPET)); // a rug on the floor, not in it
		}
		for (int x = -2; x <= 2; x++) {
			for (int z = -14; z <= -11; z++) {
				c.set(x, y + 1, z, Math.abs(x) == 2 || z == -14 || z == -11 ? s(Blocks.RED_CARPET) : s(Blocks.BLUE_CARPET));
			}
		}
		// the L-shaped counter on the raised end, under a red canopy
		for (int z = -21; z <= -9; z++) {
			c.set(-9, y + 2, z, s(Blocks.DARK_OAK_PLANKS));
			c.set(-9, y + 3, z, Kit.slab(Blocks.SPRUCE_SLAB, false));
			c.set(-9, up - 2, z, s(Blocks.RED_WOOL));
		}
		for (int x = -12; x <= -9; x++) {
			c.set(x, y + 2, -9, s(Blocks.DARK_OAK_PLANKS));
			c.set(x, y + 3, -9, Kit.slab(Blocks.SPRUCE_SLAB, false));
		}
		// shelves and barrels behind the counter, lattice panels
		for (int z = -22; z <= -10; z++) {
			c.set(-13, y + 2, z, Math.floorMod(z, 3) == 0 ? s(Blocks.BOOKSHELF) : Canvas.facing(s(Blocks.BARREL), Direction.EAST));
			c.set(-13, y + 3, z, Math.floorMod(z, 2) == 0 ? s(Blocks.DECORATED_POT) : s(Blocks.DARK_OAK_TRAPDOOR));
			c.set(-13, y + 4, z, s(Blocks.DARK_OAK_FENCE));
		}
		// blue chains with lanterns from the beams
		for (int[] l : new int[][]{{-3, -13}, {3, -13}, {9, -18}, {9, -8}, {-11, -15}}) {
			c.fill(l[0], up - 3, l[1], l[0], up - 1, l[1], s(Blocks.CHAIN));
			c.set(l[0], up - 4, l[1], lantern(true));
		}
		for (int z : new int[]{-22, -18, -6}) {                          // clear of the side door at z -12
			c.set(13, y + 1, z, Canvas.facing(s(Blocks.BARREL), Direction.UP));
			c.set(13, y + 2, z, Canvas.facing(s(Blocks.BARREL), Direction.UP));
		}
		// ladder up to the kitchen: against the north wall, through a one-block hatch with a rail either side
		for (int yy = y + 2; yy <= up; yy++) {
			c.set(-11, yy, -23, s(Blocks.LADDER).setValue(LadderBlock.FACING, Direction.SOUTH));
		}
		// the kitchen: a central island, a brick oven, cupboards, shelves, utensils, banners
		for (int x = -3; x <= 3; x++) {
			for (int z = -16; z <= -12; z++) {
				c.set(x, up + 1, z, s(Blocks.BRICKS));
				c.set(x, up + 2, z, s(Blocks.DARK_OAK_PLANKS)); // a full top, so things stand on it
			}
		}
		c.set(-2, up + 3, -14, s(Blocks.DECORATED_POT));
		c.set(1, up + 3, -13, s(Blocks.FLOWER_POT));
		for (int x = 8; x <= 12; x++) {
			for (int z = -23; z <= -20; z++) {
				boolean shell = x == 8 || x == 12 || z == -20;
				c.fill(x, up + 1, z, x, up + 3, z, shell ? s(Blocks.BRICKS) : s(Blocks.AIR));
				c.set(x, up + 4, z, x == 8 || x == 12 ? stair(Blocks.BRICK_STAIRS, x == 8 ? Direction.EAST : Direction.WEST, false) : s(Blocks.BRICKS));
			}
		}
		c.set(10, up + 1, -20, Canvas.facing(s(Blocks.FURNACE), Direction.SOUTH));
		c.set(10, up + 2, -20, Canvas.facing(s(Blocks.SMOKER), Direction.SOUTH));
		c.set(10, up + 1, -22, s(Blocks.CAMPFIRE));
		c.set(-12, up + 1, -23, Kit.connected(Blocks.DARK_OAK_FENCE, false));
		c.set(-10, up + 1, -23, Kit.connected(Blocks.DARK_OAK_FENCE, false));
		for (int x = -8; x <= 6; x++) {
			c.set(x, up + 1, -23, x % 3 == 0 ? Canvas.facing(s(Blocks.BARREL), Direction.SOUTH) : s(Blocks.DARK_OAK_PLANKS));
			c.set(x, up + 2, -23, Kit.slab(Blocks.POLISHED_ANDESITE_SLAB, false));
			if (x % 2 == 0) {
				c.set(x, up + 4, -23, s(Blocks.DARK_OAK_TRAPDOOR)); // shelves on the wall
			}
		}
		for (int z = -19; z <= -6; z += 4) {
			banner(c, 13, up + 4, z, Direction.WEST, Blocks.BLUE_WALL_BANNER); // against the east wall (x 14)
		}
		c.set(0, up + 6, -14, lantern(true));
		c.set(0, up + 7, -14, s(Blocks.CHAIN));                         // from the kitchen ceiling (y up + 8)
	}

	// ==================================================================================================== wilderness

	/** The wild back half: two worn clearings with camps, a path from the landward gate, rock outcrops and tree clusters. */
	private void wilderness(Canvas c) {
		clearing(c, -45, -95, 26, 20, 61);
		clearing(c, 42, -100, 24, 23, 62);
		path(c, 0, -38, -20, -60, -38, -82);
		path(c, 0, -38, 18, -62, 38, -84);
		camp(c, -45, -95, false);
		camp(c, 42, -100, true);
		int[][] rocks = {{-8, -70, 4}, {-2, -112, 5}, {4, -104, 3}, {-95, -60, 4}, {88, -75, 5}, {-70, -135, 4}, {60, -140, 4},
				{-30, -128, 3}, {22, -78, 3}, {70, -52, 3}, {-60, -52, 3}};
		for (int[] r : rocks) {
			boulders(c, r[0], r[1], r[2]);
		}
		int[][] trees = {{-85, -80, 0}, {-80, -86, 1}, {-88, -74, 2}, {-20, -88, 0}, {-24, -96, 1}, {10, -130, 2}, {16, -124, 0},
				{4, -136, 1}, {80, -115, 2}, {86, -108, 0}, {76, -122, 1}, {-55, -140, 0}, {-62, -132, 2}, {25, -55, 1}, {32, -60, 0},
				{-30, -55, 2}, {60, -78, 0}, {-70, -70, 1}, {-12, -140, 0}, {64, -130, 2}, {-40, -120, 1}, {22, -112, 0}};
		for (int[] t : trees) {
			wildTree(c, t[0], t[1], t[2]);
		}
	}

	/** A worn, irregular clearing: coarse dirt, dirt and packed mud with grass tufts at its ragged edge. */
	private void clearing(Canvas c, int cx, int cz, int rx, int rz, int salt) {
		for (int x = cx - rx - 4; x <= cx + rx + 4; x++) {
			for (int z = cz - rz - 4; z <= cz + rz + 4; z++) {
				if (!c.inChunk(x, z)) {
					continue;
				}
				double a = Math.atan2(z - cz, x - cx);
				double edge = 1 + 0.18 * Math.sin(a * 3 + salt) + 0.1 * Math.sin(a * 7 + salt * 0.3);
				double d = Math.hypot((x - cx) / (double) rx, (z - cz) / (double) rz) / edge;
				int g = c.ground(x, z);
				if (g <= Terrain.WATER_TOP + 1 || d > 1.05) {
					continue;
				}
				double u = unit(x, z, salt);
				if (d > 0.92 && u < 0.5) {
					continue;
				}
				c.set(x, g, z, u < 0.45 ? s(Blocks.COARSE_DIRT) : u < 0.75 ? s(Blocks.DIRT) : u < 0.9 ? s(Blocks.PACKED_MUD) : s(Blocks.ROOTED_DIRT));
			}
		}
	}

	/** A trodden dirt path through the grass along the points. */
	private void path(Canvas c, int... pts) {
		for (int i = 0; i + 3 < pts.length; i += 2) {
			int x1 = pts[i], z1 = pts[i + 1], x2 = pts[i + 2], z2 = pts[i + 3];
			int n = Math.max(Math.abs(x2 - x1), Math.abs(z2 - z1));
			for (int k = 0; k <= n; k++) {
				int x = x1 + (x2 - x1) * k / Math.max(1, n), z = z1 + (z2 - z1) * k / Math.max(1, n);
				for (int w = -1; w <= 1; w++) {
					for (int v = -1; v <= 1; v++) {
						if (!c.inChunk(x + w, z + v) || Math.abs(w) + Math.abs(v) == 2 && unit(x + w, z + v, 71) < 0.6) {
							continue;
						}
						int g = c.ground(x + w, z + v);
						if (g > Terrain.WATER_TOP + 1 && g < TOWN + 12) {
							c.set(x + w, g, z + v, unit(x + w, z + v, 72) < 0.75 ? s(Blocks.DIRT_PATH) : s(Blocks.COARSE_DIRT));
						}
					}
				}
			}
		}
	}

	/** A bandit camp in a clearing: tents, a campfire ring, log seats, crates; the stronger camp has a palisade corner and the boss's banner. */
	private void camp(Canvas c, int cx, int cz, boolean strong) {
		int g = c.ground(cx, cz);
		Kit.tent(c, cx - 6, cz - 4, c.ground(cx - 6, cz - 4), strong ? Blocks.RED_WOOL : Blocks.BROWN_WOOL);
		Kit.tent(c, cx + 6, cz + 3, c.ground(cx + 6, cz + 3), strong ? Blocks.BLACK_WOOL : Blocks.WHITE_WOOL);
		c.set(cx, g + 1, cz, s(Blocks.CAMPFIRE));
		for (Direction d : Direction.Plane.HORIZONTAL) {
			c.set(cx + d.getStepX() * 3, c.ground(cx + d.getStepX() * 3, cz + d.getStepZ() * 3) + 1, cz + d.getStepZ() * 3,
					log(Blocks.OAK_LOG, d.getAxis() == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X));
		}
		for (int[] k : new int[][]{{-3, 6}, {-2, 6}, {4, -6}}) {
			int x = cx + k[0], z = cz + k[1];
			c.set(x, c.ground(x, z) + 1, z, Canvas.facing(s(Blocks.BARREL), Direction.UP));
		}
		if (strong) {
			// the boss's spot at the far end: a ring of standing stones and a black banner on a pole
			int bx = 46, bz = -112, bg = c.ground(bx, bz);
			for (int k = 0; k < 8; k++) {
				double a = k * Math.PI / 4;
				int x = bx + (int) Math.round(Math.cos(a) * 7), z = bz + (int) Math.round(Math.sin(a) * 7);
				int sg = c.ground(x, z);
				c.fill(x, sg + 1, z, x, sg + 2 + (k % 2), z, rock(x, sg, z));
			}
			// the boss's flag: a dark pole with black banners hanging from its top
			c.fill(bx, bg + 1, bz - 8, bx, bg + 9, bz - 8, log(Blocks.DARK_OAK_LOG, Direction.Axis.Y));
			c.set(bx, bg + 10, bz - 8, s(Blocks.DARK_OAK_FENCE));
			banner(c, bx + 1, bg + 8, bz - 8, Direction.EAST, Blocks.BLACK_WALL_BANNER);
			banner(c, bx - 1, bg + 8, bz - 8, Direction.WEST, Blocks.BLACK_WALL_BANNER);
			banner(c, bx, bg + 8, bz - 7, Direction.SOUTH, Blocks.BLACK_WALL_BANNER);
		}
	}

	/** A cluster of rough boulders around (x, z). */
	private void boulders(Canvas c, int x, int z, int size) {
		for (int dx = -size - 1; dx <= size + 1; dx++) {
			for (int dz = -size - 1; dz <= size + 1; dz++) {
				int wx = x + dx, wz = z + dz;
				if (!c.inChunk(wx, wz)) {
					continue;
				}
				double h = size * 0.9 - Math.hypot(dx, dz * 1.2) + unit(wx, wz, 81) * 1.6;
				int g = c.ground(wx, wz);
				if (h <= 0 || g <= Terrain.WATER_TOP) {
					continue;
				}
				for (int y = g + 1; y <= g + (int) Math.round(h); y++) {
					c.set(wx, y, wz, unit(wx, wz + y, 82) < 0.15 ? s(Blocks.MOSSY_COBBLESTONE) : rock(wx, y, wz));
				}
				if (unit(wx, wz, 83) < 0.3) {
					c.set(wx, g + (int) Math.round(h) + 1, wz, s(Blocks.MOSS_CARPET));
				}
			}
		}
	}

	/**
	 * A wild tree with a broad, flat dark canopy: a dark oak trunk (leaning one way for some), a wide lower layer of
	 * leaves and a smaller top.
	 */
	private void wildTree(Canvas c, int x, int z, int kind) {
		int g = c.ground(x, z);
		if (g <= Terrain.WATER_TOP + 1 || !c.near(x, z, 7)) {
			return;
		}
		int h = 6 + kind;
		int lx = kind == 2 ? 1 : 0;                                   // a leaning trunk
		for (int y = 1; y <= h; y++) {
			c.set(x + (y > h / 2 ? lx : 0), g + y, z, log(Blocks.DARK_OAK_LOG, Direction.Axis.Y));
		}
		int tx = x + lx, ty = g + h;
		BlockState leaf = Kit.leaves(kind == 1 ? Blocks.OAK_LEAVES : Blocks.DARK_OAK_LEAVES);
		for (int dx = -5; dx <= 5; dx++) {
			for (int dz = -5; dz <= 5; dz++) {
				double d = Math.hypot(dx, dz) + unit(tx + dx, tz(z, dz), 91) * 1.2;
				if (d <= 4.6) {
					c.set(tx + dx, ty, z + dz, leaf);
				}
				if (d <= 3.4) {
					c.set(tx + dx, ty + 1, z + dz, leaf);
				}
				if (d <= 1.8) {
					c.set(tx + dx, ty + 2, z + dz, leaf);
				}
			}
		}
		c.set(tx, ty, z, log(Blocks.DARK_OAK_LOG, Direction.Axis.Y));
		for (Direction d : Direction.Plane.HORIZONTAL) {
			c.set(tx + d.getStepX() * 2, ty - 1, z + d.getStepZ() * 2, log(Blocks.DARK_OAK_LOG, d.getAxis()));
			c.set(tx + d.getStepX(), ty - 1, z + d.getStepZ(), log(Blocks.DARK_OAK_LOG, d.getAxis()));
		}
	}

	private static int tz(int z, int dz) {
		return z + dz;
	}

	/**
	 * Worlds built before the fix had the gateway steps facing south (back to front). Turns exactly those blocks (bottom
	 * sandstone stairs facing south at the 15 step positions, inside chunk {@code cp}) to face north. Returns how many.
	 */
	public static int repairGateSteps(net.minecraft.server.level.ServerLevel level, com.grandline.world.Island island,
			net.minecraft.world.level.ChunkPos cp) {
		int fixed = 0;
		int[][] steps = {{PROM + 1, HZ1 + 2}, {PROM + 2, HZ1 + 1}, {PROM + 3, HZ1}};
		for (int dx = -2; dx <= 2; dx++) {
			for (int[] st : steps) {
				net.minecraft.core.BlockPos p = new net.minecraft.core.BlockPos(island.x() + dx, st[0], island.z() + st[1]);
				if (p.getX() >> 4 != cp.x || p.getZ() >> 4 != cp.z) {
					continue;
				}
				var state = level.getBlockState(p);
				if (state.is(Blocks.SANDSTONE_STAIRS) && state.getValue(net.minecraft.world.level.block.StairBlock.FACING) == Direction.SOUTH
						&& state.getValue(net.minecraft.world.level.block.StairBlock.HALF) == net.minecraft.world.level.block.state.properties.Half.BOTTOM) {
					level.setBlock(p, state.setValue(net.minecraft.world.level.block.StairBlock.FACING, Direction.NORTH), 2);
					fixed++;
				}
			}
		}
		return fixed;
	}
}
