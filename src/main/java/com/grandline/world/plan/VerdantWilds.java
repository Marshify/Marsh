package com.grandline.world.plan;

import com.grandline.world.plan.Anchor.Type;
import com.grandline.world.plan.Terrain.Ground;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * Verdant Wilds (Lv 15): two jungle islands split by a channel. The main island has a dock on its south shore, a trail
 * to a quest clearing and on up to a stone temple on a raised terrace; a branch crosses a jungle-plank bridge to the
 * smaller west island and its boss clearing under a giant tree. Island-local coordinates: +x east, +z south, sea y 62.
 *
 * <p>Built entirely from code (no structure files), like every planned island, so it is identical in every world.
 */
final class VerdantWilds extends BasePlan {
	// ---- layout (island-local) ----
	static final int TERRACE_Y = 70;
	/** Temple plinth footprint (29 x 25) and its top (the temple floor). */
	static final int TX1 = -14, TX2 = 14, TZ1 = -29, TZ2 = -5;
	static final int FLOOR = 75;
	/** Highest temple block. */
	static final int TOP = 92;

	static final double[] MAIN_TRAIL = {1, 46, -2, 37, 2, 29, 1, 20, 0, 10, 0, 1};
	static final double[] WEST_TRAIL = {-3, 24, -20, 19, -38, 12, -54, 6};
	static final double[] CAMP_TRAIL = {6, 24, 18, 15, 27, 8};
	static final double[] ISLET_TRAIL = {-74, 6, -86, 2, -94, -1};

	static final int QUEST_X = 2, QUEST_Z = 25;
	static final int CAMP_X = 31, CAMP_Z = 5;
	static final int BOSS_X = -101, BOSS_Z = -4, BOSS_R = 12;
	static final int GIANT_X = -117, GIANT_Z = -17;
	static final int BRIDGE_Z = 6, BRIDGE_X1 = -84, BRIDGE_X2 = -46, BRIDGE_Y = 64;
	static final int DOCK_X1 = -2, DOCK_X2 = 2, DOCK_HEAD_Z = 56, DOCK_END_Z = 60;

	/** A tree: kind 0 giant (2x2), 1 medium, 2 bush, 3 colossal (3x3). */
	record Tree(int x, int z, int kind, int height) {
	}

	private final List<Tree> trees = new ArrayList<>();
	private final List<int[]> boulders = new ArrayList<>();

	VerdantWilds() {
		super("Verdant Wilds", 0.04);
		BlockState path = s(Blocks.DIRT_PATH);
		terrain.mass(0, 0, 54, 43, 6, 2.2, Ground.PODZOL)          // main island (~112 x 90 with its beach)
				.mass(0, -16, 28, 24, 9, 1.6, Ground.PODZOL)        // rise under the temple
				.mass(-104, -4, 29, 25, 5, 2.0, Ground.PODZOL)      // west island (~60 x 52 with its beach)
				.flat(0, -16, 23, 19, TERRACE_Y, 9, null)           // temple terrace
				.flat(QUEST_X, QUEST_Z, 7, 6, 67, 4, null)          // quest clearing
				.flat(CAMP_X, CAMP_Z, 8, 7, 67, 4, null)            // first enemy clearing
				.flat(BOSS_X, BOSS_Z, BOSS_R, BOSS_R, 66, 5, null)  // boss clearing
				.path(path, 4, MAIN_TRAIL)
				.path(path, 3, WEST_TRAIL)
				.path(path, 3, CAMP_TRAIL)
				.path(path, 3, ISLET_TRAIL)
				.floorMix(s(Blocks.GRASS_BLOCK), s(Blocks.PODZOL), s(Blocks.GRASS_BLOCK), s(Blocks.MOSS_BLOCK), s(Blocks.PODZOL))
				.shoreMix(s(Blocks.STONE), s(Blocks.ANDESITE), s(Blocks.GRAVEL), s(Blocks.STONE), s(Blocks.ANDESITE));
		anchor(Anchor.of(Type.QUEST, QUEST_X + 5, QUEST_Z));
		anchor(Anchor.of(Type.FISHERMAN, -6, 41));
		anchor(Anchor.at(Type.DOCK, 7, 63, 58));
		anchor(Anchor.area(Type.ENEMY_CAMP, CAMP_X, CAMP_Z, 7));
		anchor(Anchor.of(Type.BOSS_ARENA, BOSS_X, BOSS_Z));
		anchor(Anchor.of(Type.TREASURE, -36, -27));
		placeTrees();
		placeBoulders();
	}

	// ==================================================================================================== layout logic

	static double distanceToPath(double x, double z, double[] pts) {
		double best = Double.MAX_VALUE;
		for (int i = 0; i + 3 < pts.length; i += 2) {
			double ax = pts[i], az = pts[i + 1], bx = pts[i + 2], bz = pts[i + 3];
			double vx = bx - ax, vz = bz - az, len = vx * vx + vz * vz;
			double t = len == 0 ? 0 : Math.max(0, Math.min(1, ((x - ax) * vx + (z - az) * vz) / len));
			double dx = x - (ax + t * vx), dz = z - (az + t * vz);
			best = Math.min(best, Math.sqrt(dx * dx + dz * dz));
		}
		return best;
	}

	private static boolean inEllipse(double x, double z, double cx, double cz, double rx, double rz) {
		double dx = (x - cx) / rx, dz = (z - cz) / rz;
		return dx * dx + dz * dz <= 1;
	}

	/** Places nothing big here: trails (the main one keeps a wide sightline), clearings, the temple, dock and bridge. */
	static boolean keepClear(int x, int z, double trailMargin) {
		return distanceToPath(x, z, MAIN_TRAIL) < trailMargin + 2
				|| distanceToPath(x, z, WEST_TRAIL) < trailMargin
				|| distanceToPath(x, z, CAMP_TRAIL) < trailMargin
				|| distanceToPath(x, z, ISLET_TRAIL) < trailMargin
				|| inEllipse(x, z, QUEST_X, QUEST_Z, 10, 9)
				|| inEllipse(x, z, CAMP_X, CAMP_Z, 11, 10)
				|| inEllipse(x, z, BOSS_X, BOSS_Z, BOSS_R + 2, BOSS_R + 2)
				|| (x >= TX1 - 4 && x <= TX2 + 4 && z >= TZ1 - 4 && z <= 4)
				|| (Math.abs(x) <= 18 && z > 4 && z <= 12)   // keep the facade in view from the trail
				|| (Math.abs(x) <= 11 && z > 12 && z <= 18)
				|| (x >= -9 && x <= 9 && z >= 38)
				|| (Math.abs(z - BRIDGE_Z) <= 4 && x >= BRIDGE_X1 - 2 && x <= BRIDGE_X2 + 2)
				|| inEllipse(x, z, GIANT_X, GIANT_Z, 7, 7);
	}

	static long hash(int x, int z) {
		long h = x * 0x9E3779B97F4A7C15L ^ z * 0xC2B2AE3D27D4EB4FL;
		h ^= h >>> 29;
		h *= 0xBF58476D1CE4E5B9L;
		return h ^ (h >>> 32);
	}

	private void placeTrees() {
		// Framing giants: two pairs flank the trail so the temple shows between them on the walk up.
		int[][] framing = {{-11, 36, 19}, {11, 33, 20}, {-13, 13, 18}, {13, 11, 19}, {-24, -16, 18}, {24, -18, 18}, {-23, 2, 17}, {24, 1, 17}};
		for (int[] f : framing) {
			trees.add(new Tree(f[0], f[1], 0, f[2]));
		}
		trees.add(new Tree(GIANT_X, GIANT_Z, 3, 27));
		for (int gx = -138; gx <= 60; gx += 5) {
			for (int gz = -54; gz <= 54; gz += 5) {
				long h = hash(gx, gz);
				int x = gx + (int) Math.floorMod(h, 4) - 2;
				int z = gz + (int) Math.floorMod(h >>> 8, 4) - 2;
				int g = terrain.ground(x, z);
				if (g <= 64 || keepClear(x, z, 4.5)) {
					continue;
				}
				boolean nearFraming = false;
				for (Tree t : trees) {
					if (t.kind() == 0 && Math.abs(t.x() - x) < 5 && Math.abs(t.z() - z) < 5) {
						nearFraming = true;
					}
				}
				if (nearFraming) {
					continue;
				}
				int roll = (int) Math.floorMod(h >>> 20, 100);
				if (roll < 16) {
					// Giants stay below the temple roof on the main island.
					int height = 15 + (int) Math.floorMod(h >>> 30, 5);
					height = Math.min(height, TOP - 4 - g);
					trees.add(new Tree(x, z, 0, height));
				} else if (roll < 66) {
					trees.add(new Tree(x, z, 1, 6 + (int) Math.floorMod(h >>> 34, 4)));
				} else {
					trees.add(new Tree(x, z, 2, 1));
				}
			}
		}
	}

	private void placeBoulders() {
		for (int gx = -140; gx <= 62; gx += 7) {
			for (int gz = -56; gz <= 56; gz += 7) {
				long h = hash(gx * 3 + 1, gz * 5 + 2);
				int x = gx + (int) Math.floorMod(h, 6) - 3;
				int z = gz + (int) Math.floorMod(h >>> 9, 6) - 3;
				int g = terrain.ground(x, z);
				if (g < 61 || g > 64 || Math.floorMod(h >>> 17, 3) == 0 || keepClear(x, z, 3)) {
					continue;
				}
				boulders.add(new int[]{x, z, 1 + (int) Math.floorMod(h >>> 23, 2)});
			}
		}
	}

	// ==================================================================================================== build

	@Override
	public void build(Canvas c) {
		groundCover(c);
		for (int[] b : boulders) {
			if (c.near(b[0], b[1], 3)) {
				boulder(c, b[0], b[1], b[2]);
			}
		}
		for (Tree t : trees) {
			if (!c.near(t.x(), t.z(), t.kind() == 3 ? 12 : 7)) {
				continue;
			}
			switch (t.kind()) {
				case 0 -> giantTree(c, t.x(), t.z(), t.height());
				case 1 -> jungleTree(c, t.x(), t.z(), t.height());
				case 2 -> bush(c, t.x(), t.z());
				default -> colossalTree(c, t.x(), t.z(), t.height());
			}
		}
		if (c.near(0, -17, 22)) {
			temple(c);
		}
		dock(c);
		bridge(c);
		trailLanterns(c);
		questClearing(c);
		enemyCamp(c);
		bossClearing(c);
		treasure(c);
	}

	// ---------------------------------------------------------------------------------------------------- nature

	private static BlockState vine(Direction attachedTo) {
		BlockState v = Blocks.VINE.defaultBlockState();
		return switch (attachedTo) {
			case NORTH -> v.setValue(VineBlock.NORTH, true);
			case SOUTH -> v.setValue(VineBlock.SOUTH, true);
			case EAST -> v.setValue(VineBlock.EAST, true);
			case WEST -> v.setValue(VineBlock.WEST, true);
			default -> v.setValue(VineBlock.UP, true);
		};
	}

	/** Grass and ferns on the forest floor (never on trails, clearings or the shore). */
	private void groundCover(Canvas c) {
		for (int x = -140; x <= 62; x++) {
			for (int z = -56; z <= 56; z++) {
				if (!c.inChunk(x, z)) {
					continue;
				}
				int g = c.ground(x, z);
				if (g <= 64 || terrain.paving(x, z) != null || keepClear(x, z, 2.5)) {
					continue;
				}
				int roll = (int) Math.floorMod(hash(x, z) >>> 12, 100);
				if (roll < 20) {
					c.set(x, g + 1, z, s(Blocks.GRASS));
				} else if (roll < 30) {
					c.set(x, g + 1, z, s(Blocks.FERN));
				}
			}
		}
	}

	private static void boulder(Canvas c, int x, int z, int r) {
		int g = c.ground(x, z);
		for (int dx = -r; dx <= r; dx++) {
			for (int dz = -r; dz <= r; dz++) {
				for (int dy = 0; dy <= r; dy++) {
					if (dx * dx + dz * dz + dy * dy * 2 <= r * r + 1) {
						Block b = Math.floorMod(hash(x + dx, z + dz + dy), 3) == 0 ? Blocks.STONE : Blocks.ANDESITE;
						c.set(x + dx, g + dy, z + dz, s(b));
					}
				}
			}
		}
	}

	/** Vines down the four sides of a trunk spanning (x1..x2, z1..z2), sparse and seeded. */
	private static void trunkVines(Canvas c, int x1, int z1, int x2, int z2, int fromY, int toY) {
		for (int y = fromY; y <= toY; y++) {
			for (int x = x1; x <= x2; x++) {
				if (Math.floorMod(hash(x, y * 7 + z1), 3) == 0) {
					c.set(x, y, z1 - 1, vine(Direction.SOUTH));
				}
				if (Math.floorMod(hash(x + 11, y * 7 + z2), 3) == 0) {
					c.set(x, y, z2 + 1, vine(Direction.NORTH));
				}
			}
			for (int z = z1; z <= z2; z++) {
				if (Math.floorMod(hash(x1 + 5, y * 7 + z), 3) == 0) {
					c.set(x1 - 1, y, z, vine(Direction.EAST));
				}
				if (Math.floorMod(hash(x2 + 3, y * 7 + z + 1), 3) == 0) {
					c.set(x2 + 1, y, z, vine(Direction.WEST));
				}
			}
		}
	}

	/** Vines hanging from a canopy ring of radius {@code r} under {@code y}. */
	private static void hangingVines(Canvas c, int cx, int cz, int r, int y, int maxLength) {
		for (int dx = -r; dx <= r; dx++) {
			for (int dz = -r; dz <= r; dz++) {
				int d2 = dx * dx + dz * dz;
				if (d2 > r * r || d2 <= (r - 1) * (r - 1)) {
					continue;
				}
				long h = hash(cx + dx, cz + dz + y);
				if (Math.floorMod(h, 3) != 0) {
					continue;
				}
				int length = 1 + (int) Math.floorMod(h >>> 7, maxLength);
				for (int i = 1; i <= length; i++) {
					c.set(cx + dx, y - i, cz + dz, vine(Direction.UP));
				}
			}
		}
	}

	/** 2x2 jungle giant with a layered canopy, trunk vines and hanging vines. */
	private static void giantTree(Canvas c, int x, int z, int height) {
		int g = c.ground(x, z);
		if (g <= Terrain.WATER_TOP) {
			return;
		}
		Kit.bigJungleTree(c, x, z, height);
		trunkVines(c, x, z, x + 1, z + 1, g + 3, g + height - 4);
		hangingVines(c, x, z, 5, g + height - 2, 4);
	}

	/** Medium jungle tree: single trunk, round crown, a few vines. */
	private static void jungleTree(Canvas c, int x, int z, int height) {
		int g = c.ground(x, z);
		if (g <= Terrain.WATER_TOP) {
			return;
		}
		BlockState leaf = Kit.leaves(Blocks.JUNGLE_LEAVES);
		int top = g + height;
		for (int dx = -3; dx <= 3; dx++) {
			for (int dz = -3; dz <= 3; dz++) {
				for (int dy = -2; dy <= 2; dy++) {
					if (dx * dx + dz * dz + dy * dy * 2.4 <= 10.5) {
						c.set(x + dx, top + dy, z + dz, leaf);
					}
				}
			}
		}
		c.fill(x, g + 1, z, x, top, z, s(Blocks.JUNGLE_LOG));
		hangingVines(c, x, z, 3, top - 1, 3);
		if (Math.floorMod(hash(x, z), 2) == 0) {
			trunkVines(c, x, z, x, z, g + 2, top - 3);
		}
	}

	/** Low jungle bush: a log stub inside a leaf mound. */
	private static void bush(Canvas c, int x, int z) {
		int g = c.ground(x, z);
		if (g <= Terrain.WATER_TOP) {
			return;
		}
		BlockState leaf = Kit.leaves(Blocks.JUNGLE_LEAVES);
		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) {
				for (int dy = 0; dy <= 2; dy++) {
					if (dx * dx + dz * dz + dy * dy * 1.8 <= 5) {
						c.set(x + dx, g + 1 + dy, z + dz, leaf);
					}
				}
			}
		}
		c.set(x, g + 1, z, s(Blocks.JUNGLE_LOG));
	}

	/** The giant tree beside the boss clearing: 3x3 trunk, buttress roots, a huge layered crown and side boughs. */
	private static void colossalTree(Canvas c, int x, int z, int height) {
		int g = c.ground(x, z);
		BlockState log = s(Blocks.JUNGLE_LOG);
		BlockState leaf = Kit.leaves(Blocks.JUNGLE_LEAVES);
		c.foundation(x - 1, z - 1, x + 1, z + 1, g, s(Blocks.ROOTED_DIRT));
		c.fill(x - 1, g + 1, z - 1, x + 1, g + height, z + 1, log);
		// Buttress roots spreading on all sides.
		int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {-1, 1}, {1, -1}, {-1, -1}};
		for (int[] d : dirs) {
			int len = (d[0] != 0 && d[1] != 0) ? 3 : 4;
			for (int i = 1; i <= len; i++) {
				int rx = x + d[0] * (1 + i), rz = z + d[1] * (1 + i);
				int top = g + Math.max(0, 3 - i);
				int rg = c.ground(rx, rz);
				BlockState axis = log.setValue(RotatedPillarBlock.AXIS, d[0] != 0 && d[1] == 0 ? Direction.Axis.X
						: d[1] != 0 && d[0] == 0 ? Direction.Axis.Z : Direction.Axis.Y);
				c.fill(rx, Math.min(rg, top), rz, rx, top, rz, axis);
			}
		}
		// Crown: four layers, widest at the bottom.
		int[][] layers = {{9, -3}, {8, -1}, {6, 1}, {4, 3}};
		for (int[] layer : layers) {
			int r = layer[0];
			int yy = g + height + layer[1];
			for (int dx = -r; dx <= r; dx++) {
				for (int dz = -r; dz <= r; dz++) {
					int d2 = dx * dx + dz * dz;
					if (d2 <= r * r && (d2 < (r - 1) * (r - 1) || Math.floorMod(hash(x + dx, z + dz + yy), 4) != 0)) {
						c.set(x + dx, yy, z + dz, leaf);
						if (Math.floorMod(hash(x + dx, z + dz), 5) == 0) {
							c.set(x + dx, yy + 1, z + dz, leaf);
						}
					}
				}
			}
		}
		// Side boughs with leaf clumps halfway up.
		int[][] boughs = {{1, 0, 10}, {-1, 0, 13}, {0, 1, 16}, {0, -1, 11}};
		for (int[] b : boughs) {
			int by = g + b[2];
			for (int i = 2; i <= 6; i++) {
				c.set(x + b[0] * i, by + i / 3, z + b[1] * i, log.setValue(RotatedPillarBlock.AXIS, b[0] != 0 ? Direction.Axis.X : Direction.Axis.Z));
			}
			int bx = x + b[0] * 7, bz = z + b[1] * 7;
			for (int dx = -3; dx <= 3; dx++) {
				for (int dz = -3; dz <= 3; dz++) {
					for (int dy = -1; dy <= 2; dy++) {
						if (dx * dx + dz * dz + dy * dy * 2 <= 9) {
							c.set(bx + dx, by + 2 + dy, bz + dz, leaf);
						}
					}
				}
			}
		}
		trunkVines(c, x - 1, z - 1, x + 1, z + 1, g + 4, g + height - 5);
		hangingVines(c, x, z, 9, g + height - 3, 6);
	}

	// ---------------------------------------------------------------------------------------------------- temple

	/** Weathered stone: mostly stone bricks, mossy near the ground and in seeded patches, some cracked. */
	private static BlockState brick(int x, int y, int z) {
		int roll = (int) Math.floorMod(hash(x * 31 + y, z * 17 - y), 100);
		int mossy = y < 79 ? 34 : 22;
		if (roll < mossy) {
			return s(Blocks.MOSSY_STONE_BRICKS);
		}
		if (roll < mossy + 12) {
			return s(Blocks.CRACKED_STONE_BRICKS);
		}
		return s(Blocks.STONE_BRICKS);
	}

	private static void brickFill(Canvas c, int x1, int y1, int z1, int x2, int y2, int z2) {
		for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
			for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) {
				for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++) {
					c.set(x, y, z, brick(x, y, z));
				}
			}
		}
	}

	/** Eave of stairs around a rectangle at height y, stepping up towards the middle. */
	private static void eave(Canvas c, int x1, int z1, int x2, int z2, int y) {
		c.fill(x1, y, z1, x2, y, z1, Kit.stairs(Blocks.STONE_BRICK_STAIRS, Direction.SOUTH, false));
		c.fill(x1, y, z2, x2, y, z2, Kit.stairs(Blocks.STONE_BRICK_STAIRS, Direction.NORTH, false));
		c.fill(x1, y, z1 + 1, x1, y, z2 - 1, Kit.stairs(Blocks.STONE_BRICK_STAIRS, Direction.EAST, false));
		c.fill(x2, y, z1 + 1, x2, y, z2 - 1, Kit.stairs(Blocks.STONE_BRICK_STAIRS, Direction.WEST, false));
	}

	private void temple(Canvas c) {
		BlockState air = s(Blocks.AIR);
		BlockState chiseled = s(Blocks.CHISELED_STONE_BRICKS);
		// Plinth: solid to the terrace, weathered faces, chiseled band along the top edge.
		c.foundation(TX1, TZ1, TX2, TZ2, FLOOR, s(Blocks.STONE_BRICKS));
		for (int x = TX1; x <= TX2; x++) {
			for (int z = TZ1; z <= TZ2; z++) {
				boolean edge = x == TX1 || x == TX2 || z == TZ1 || z == TZ2;
				if (edge) {
					for (int y = TERRACE_Y + 1; y < FLOOR; y++) {
						c.set(x, y, z, brick(x, y, z));
					}
					c.set(x, FLOOR, z, (x + z) % 4 == 0 ? chiseled : s(Blocks.STONE_BRICKS));
				}
			}
		}
		// Broad front staircase (17 wide) from the terrace to the floor, with stepped cheek walls.
		for (int k = 0; k <= 4; k++) {
			int z = -k;
			int y = TERRACE_Y + 1 + k;
			for (int x = -8; x <= 8; x++) {
				c.foundation(x, z, x, z, y - 1, s(Blocks.STONE_BRICKS));
				c.set(x, y, z, Kit.stairs(Blocks.STONE_BRICK_STAIRS, Direction.NORTH, false));
				c.fill(x, y + 1, z, x, y + 4, z, air);
			}
			for (int x : new int[]{-9, 9}) {
				c.foundation(x, z, x, z, y, s(Blocks.STONE_BRICKS));
				c.set(x, y, z, brick(x, y, z));
				c.set(x, y + 1, z, Kit.slab(Blocks.STONE_BRICK_SLAB, false));
			}
		}
		// Hall walls (2 thick at the sides for alcoves), y 76-83, ceiling at 84.
		int wallTop = FLOOR + 8;
		brickFill(c, -13, FLOOR + 1, -28, 13, wallTop, -9);
		c.fill(-11, FLOOR + 1, -27, 11, wallTop, -10, air);
		// Recessed doorway: a surround standing out from the wall, the opening 3 wide and 5 tall.
		brickFill(c, -3, FLOOR + 1, -8, 3, FLOOR + 7, -8);
		c.fill(-1, FLOOR + 1, -9, 1, FLOOR + 5, -8, air);
		c.fill(-1, FLOOR + 6, -8, 1, FLOOR + 6, -8, chiseled);
		c.set(0, FLOOR + 7, -8, chiseled);
		// Portico: six thick 2x2 pillars in front of the facade.
		for (int px : new int[]{-13, -9, -5, 4, 8, 12}) {
			for (int y = FLOOR + 1; y <= wallTop; y++) {
				BlockState b = (y == FLOOR + 1 || y == wallTop) ? chiseled : brick(px, y, -7);
				c.fill(px, y, -7, px + 1, y, -6, b);
			}
		}
		// Roof over the hall and portico, eave all round, then two stepped tiers and a crown.
		brickFill(c, TX1, wallTop + 1, TZ1, TX2, wallTop + 1, TZ2);
		eave(c, TX1, TZ1, TX2, TZ2, wallTop + 2);
		c.fill(TX1 + 1, wallTop + 2, TZ1 + 1, TX2 - 1, wallTop + 2, TZ2 - 1, Kit.slab(Blocks.STONE_BRICK_SLAB, false));
		int t1 = wallTop + 2;                 // 85
		brickFill(c, -10, t1, -25, 10, t1 + 3, -9);
		for (int x = -10; x <= 10; x += 4) {
			c.fill(x, t1 + 3, -9, x, t1 + 3, -9, chiseled);   // band of carved blocks on the front face
		}
		c.fill(-2, t1, -9, 2, t1 + 2, -9, s(Blocks.CHISELED_STONE_BRICKS)); // carved panel above the door
		eave(c, -10, -25, 10, -9, t1 + 4);
		c.fill(-9, t1 + 4, -24, 9, t1 + 4, -10, Kit.slab(Blocks.STONE_BRICK_SLAB, false));
		int t2 = t1 + 4;                      // 89
		brickFill(c, -6, t2, -21, 6, t2 + 2, -12);
		c.fill(-1, t2, -12, 1, t2 + 1, -12, chiseled);
		eave(c, -6, -21, 6, -12, TOP);
		c.fill(-5, TOP, -20, 5, TOP, -13, Kit.slab(Blocks.STONE_BRICK_SLAB, false));
		c.fill(-1, TOP, -17, 1, TOP, -16, chiseled);
		// ---- interior ----
		// Floor: stone bricks with a chiseled processional line down the middle.
		for (int z = -27; z <= -9; z++) {
			for (int x = -11; x <= 11; x++) {
				c.set(x, FLOOR, z, x == 0 && z % 3 == 0 ? chiseled : brick(x, FLOOR, z));
			}
		}
		// Arcade pillars between the central chamber and the side aisles.
		for (int z : new int[]{-12, -16, -20, -24}) {
			for (int x : new int[]{-6, 6}) {
				for (int y = FLOOR + 1; y <= wallTop; y++) {
					c.set(x, y, z, y == FLOOR + 1 || y == wallTop ? chiseled : brick(x, y, z));
				}
			}
		}
		// Upper gallery over both aisles (floor y 79), with a low wall along the chamber side.
		int gallery = FLOOR + 4;
		for (int side : new int[]{-1, 1}) {
			int inner = 7 * side, outer = 11 * side;
			c.fill(inner, gallery, -27, outer, gallery, -15, s(Blocks.STONE_BRICKS));
			for (int z = -27; z <= -15; z++) {
				if (z % 4 != 0) {
					c.set(inner, gallery + 1, z, s(Blocks.STONE_BRICK_WALL));
				}
			}
			// Stairs up the front of each aisle.
			for (int k = 0; k <= 3; k++) {
				int z = -11 - k;
				int y = FLOOR + 1 + k;
				for (int x = Math.min(8 * side, 11 * side); x <= Math.max(8 * side, 11 * side); x++) {
					c.fill(x, FLOOR + 1, z, x, y - 1, z, s(Blocks.STONE_BRICKS));
					c.set(x, y, z, Kit.stairs(Blocks.STONE_BRICK_STAIRS, Direction.NORTH, false));
				}
			}
			// Side alcoves under the gallery, each with a lantern; windows from the gallery.
			for (int z : new int[]{-17, -21, -25}) {
				c.fill(12 * side, FLOOR + 1, z, 12 * side, FLOOR + 3, z - 1, air);
				c.set(12 * side, FLOOR, z, chiseled);
				c.set(12 * side, FLOOR + 1, z - 1, s(Blocks.LANTERN));
				c.fill(12 * side, gallery + 1, z, 13 * side, gallery + 2, z, air);
			}
		}
		// Altar on a low dais at the back, lanterns hanging from the ceiling.
		c.fill(-3, FLOOR + 1, -27, 3, FLOOR + 1, -25, Kit.slab(Blocks.STONE_BRICK_SLAB, false));
		c.fill(0, FLOOR + 1, -27, 0, FLOOR + 2, -27, chiseled);
		c.set(0, FLOOR + 3, -27, s(Blocks.LANTERN));
		for (int z : new int[]{-13, -19, -25}) {
			c.set(0, wallTop, z, s(Blocks.LANTERN).setValue(net.minecraft.world.level.block.LanternBlock.HANGING, true));
		}
		c.set(-4, FLOOR + 1, -8, s(Blocks.LANTERN));
		c.set(4, FLOOR + 1, -8, s(Blocks.LANTERN));
		// Vines creeping down the outer walls and pillars.
		for (int z = TZ1 + 1; z < -9; z++) {
			for (int side : new int[]{-1, 1}) {
				long h = hash(side * 50, z);
				int len = (int) Math.floorMod(h, 6);
				for (int i = 0; i < len; i++) {
					c.set(14 * side, wallTop - i, z, vine(side < 0 ? Direction.EAST : Direction.WEST));
				}
			}
		}
		for (int x = -12; x <= 12; x++) {
			long h = hash(x, 777);
			int len = (int) Math.floorMod(h, 5);
			for (int i = 0; i < len; i++) {
				c.set(x, wallTop - i, TZ1, vine(Direction.SOUTH));
			}
		}
	}

	// ---------------------------------------------------------------------------------------------------- wood

	/** Jungle-plank dock: a 5-wide pier south from the beach to a T-shaped head where boats tie up. */
	private void dock(Canvas c) {
		if (!c.near(0, 50, 14)) {
			return;
		}
		int y = Terrain.WATER_TOP + 1;
		for (int z = 38; z <= DOCK_END_Z; z++) {
			boolean head = z >= DOCK_HEAD_Z;
			int x1 = head ? -6 : DOCK_X1, x2 = head ? 6 : DOCK_X2;
			for (int x = x1; x <= x2; x++) {
				if (c.ground(x, z) > y) {
					continue; // still on the beach
				}
				c.set(x, y, z, s(Blocks.JUNGLE_PLANKS));
				c.fill(x, y + 1, z, x, y + 3, z, s(Blocks.AIR));
				boolean side = x == x1 || x == x2;
				boolean end = head && z == DOCK_END_Z;
				boolean headFront = head && z == DOCK_HEAD_Z && (x < DOCK_X1 || x > DOCK_X2);
				if ((side || end || headFront) && (z % 4 == 0 || end && x % 4 == 0 || side && head && z == DOCK_HEAD_Z)) {
					c.fill(x, Terrain.FLOOR, z, x, y - 1, z, s(Blocks.JUNGLE_LOG));
				}
				if (side && !(head && x >= DOCK_X1 && x <= DOCK_X2) || end || headFront) {
					c.set(x, y + 1, z, Kit.connected(Blocks.JUNGLE_FENCE, end || headFront));
				}
			}
		}
		// Lanterns on the dock head corners and where the pier meets the beach.
		for (int[] l : new int[][]{{-6, DOCK_END_Z}, {6, DOCK_END_Z}, {-6, DOCK_HEAD_Z}, {6, DOCK_HEAD_Z}}) {
			c.set(l[0], y + 1, l[1], s(Blocks.JUNGLE_FENCE));
			c.set(l[0], y + 2, l[1], s(Blocks.LANTERN));
		}
	}

	/** Short jungle-plank bridge over the channel to the west island, on log piles, with fence rails. */
	private void bridge(Canvas c) {
		if (!c.near((BRIDGE_X1 + BRIDGE_X2) / 2, BRIDGE_Z, 24)) {
			return;
		}
		for (int x = BRIDGE_X1; x <= BRIDGE_X2; x++) {
			for (int z = BRIDGE_Z - 1; z <= BRIDGE_Z + 1; z++) {
				int g = c.ground(x, z);
				if (g > BRIDGE_Y) {
					continue; // on land
				}
				c.set(x, BRIDGE_Y, z, s(Blocks.JUNGLE_PLANKS));
				c.fill(x, BRIDGE_Y + 1, z, x, BRIDGE_Y + 3, z, s(Blocks.AIR));
				if (z != BRIDGE_Z) {
					c.set(x, BRIDGE_Y + 1, z, Kit.connected(Blocks.JUNGLE_FENCE, true));
					if (x % 5 == 0 && g < BRIDGE_Y) {
						c.fill(x, Math.max(Terrain.FLOOR, g), z, x, BRIDGE_Y - 1, z, s(Blocks.JUNGLE_LOG));
						c.set(x, BRIDGE_Y + 1, z, s(Blocks.JUNGLE_FENCE));
					}
				}
			}
		}
	}

	/** Lantern posts along the main trail every ~11 blocks, alternating sides, so the way up reads at night too. */
	private static void trailLanterns(Canvas c) {
		double[] p = MAIN_TRAIL;
		int n = 0;
		for (int i = 0; i + 3 < p.length; i += 2) {
			double ax = p[i], az = p[i + 1], bx = p[i + 2], bz = p[i + 3];
			double len = Math.hypot(bx - ax, bz - az);
			for (double d = 3; d < len; d += 11) {
				double t = d / len;
				double px = ax + (bx - ax) * t, pz = az + (bz - az) * t;
				double nx = -(bz - az) / len, nz = (bx - ax) / len;
				int side = (n++ % 2 == 0) ? 1 : -1;
				int x = (int) Math.round(px + nx * 3.5 * side), z = (int) Math.round(pz + nz * 3.5 * side);
				if (!c.near(x, z, 1) || c.ground(x, z) <= 64) {
					continue;
				}
				int g = c.ground(x, z);
				c.set(x, g + 1, z, s(Blocks.JUNGLE_FENCE));
				c.set(x, g + 2, z, s(Blocks.JUNGLE_FENCE));
				c.set(x, g + 3, z, s(Blocks.LANTERN));
			}
		}
	}

	// ---------------------------------------------------------------------------------------------------- clearings

	private static void questClearing(Canvas c) {
		if (!c.near(QUEST_X, QUEST_Z, 10)) {
			return;
		}
		// A fallen log to sit on and two lanterns.
		int g = c.ground(QUEST_X + 7, QUEST_Z - 4);
		c.fill(QUEST_X + 6, g + 1, QUEST_Z - 4, QUEST_X + 9, g + 1, QUEST_Z - 4,
				s(Blocks.JUNGLE_LOG).setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
		c.set(QUEST_X + 8, g + 2, QUEST_Z - 4, s(Blocks.MOSS_CARPET));
		for (int[] l : new int[][]{{QUEST_X - 5, QUEST_Z - 3}, {QUEST_X + 8, QUEST_Z + 4}}) {
			int lg = c.ground(l[0], l[1]);
			c.fill(l[0], lg + 1, l[1], l[0], lg + 2, l[1], s(Blocks.JUNGLE_FENCE));
			c.set(l[0], lg + 3, l[1], s(Blocks.LANTERN));
		}
	}

	private static void enemyCamp(Canvas c) {
		if (!c.near(CAMP_X, CAMP_Z, 12)) {
			return;
		}
		Kit.tent(c, CAMP_X + 4, CAMP_Z - 3, c.ground(CAMP_X + 4, CAMP_Z - 3), Blocks.BROWN_WOOL);
		Kit.tent(c, CAMP_X - 4, CAMP_Z + 4, c.ground(CAMP_X - 4, CAMP_Z + 4), Blocks.GREEN_WOOL);
		c.set(CAMP_X, c.ground(CAMP_X, CAMP_Z) + 1, CAMP_Z, s(Blocks.CAMPFIRE));
		for (int[] b : new int[][]{{CAMP_X + 6, CAMP_Z + 3}, {CAMP_X + 6, CAMP_Z + 4}, {CAMP_X + 5, CAMP_Z + 4}}) {
			c.set(b[0], c.ground(b[0], b[1]) + 1, b[1], s(Blocks.BARREL));
		}
	}

	/** Open boss ring on the west island: room to dodge, standing stones around it, the giant tree on its edge. */
	private static void bossClearing(Canvas c) {
		if (!c.near(BOSS_X, BOSS_Z, BOSS_R + 4)) {
			return;
		}
		for (int angle = 40; angle < 360; angle += 64) {
			double a = Math.toRadians(angle);
			int x = BOSS_X + (int) Math.round(Math.cos(a) * (BOSS_R + 1));
			int z = BOSS_Z + (int) Math.round(Math.sin(a) * (BOSS_R + 1));
			if (distanceToPath(x, z, ISLET_TRAIL) < 3) {
				continue; // keep the way in open
			}
			int g = c.ground(x, z);
			int h = 3 + (int) Math.floorMod(hash(x, z), 2);
			for (int y = g + 1; y <= g + h; y++) {
				c.set(x, y, z, y == g + h ? s(Blocks.CHISELED_STONE_BRICKS) : brick(x, y, z));
			}
		}
	}
}
