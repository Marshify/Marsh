package com.grandline.world.plan;

import com.grandline.world.plan.Anchor.Type;
import com.grandline.world.plan.Kit.Interior;
import com.grandline.world.plan.Kit.Palette;
import com.grandline.world.plan.Terrain.Ground;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

/** Tree helper for surfaces the terrain function does not know about (floating islets). */
final class BlockStateHolder {
	private BlockStateHolder() {
	}

	static void tree(Canvas c, int x, int z, int y) {
		var leaf = Kit.leaves(Blocks.BIRCH_LEAVES);
		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) {
				for (int dy = -1; dy <= 1; dy++) {
					if (dx * dx + dz * dz + dy * dy * 2 <= 5) {
						c.set(x + dx, y + 5 + dy, z + dz, leaf);
					}
				}
			}
		}
		c.fill(x, y + 1, z, x, y + 5, z, Blocks.BIRCH_LOG.defaultBlockState());
	}
}

/**
 * The fourteen hand-designed islands of the First Sea (levels 1-700), in progression order. Island-local
 * coordinates: +x east, +z south, sea level y = 62. Each island has a distinct silhouette, a landmark you can see from
 * the water, a dock, walkable routes, enterable buildings and spots for its NPCs, enemies, boss and treasure.
 */
public final class FirstSeaPlans {
	private FirstSeaPlans() {
	}

	/** Pre-redesign plans for worlds created with island layout 1. */
	public static List<IslandPlan> legacy() {
		return List.of(new VerdantWildsLegacy(), new DawnreachHarborLegacy());
	}

	public static List<IslandPlan> all() {
		return List.of(new com.grandline.world.plan.DawnreachHarbor(), new VerdantWilds(), new MillbrookVillage(), new RedbrickPort(), new SunscorchDunes(),
				new FrostpeakIsle(), new IronholdBastion(), new CloudtopIsles(), new StonewallPrison(), new GladiatorsRing(),
				new EmberfallIsle(), new SaltmarketDocks(), new LotusPalace(), new CrystalFountain());
	}

	// ==================================================================================================== Lv 1

	/**
	 * The original Dawnreach Harbor (fountain plaza, ring road, hall with a clock tower, two piers). Kept for worlds
	 * created before the redesign; new worlds use {@link com.grandline.world.plan.DawnreachHarbor}.
	 */
	static final class DawnreachHarborLegacy extends BasePlan {
		DawnreachHarborLegacy() {
			super("Dawnreach Harbor", 0.06);
			terrain.mass(0, 0, 48, 38, 4, 2.4, Ground.GRASS)
					.mass(-16, -18, 20, 15, 9, 1.8, Ground.GRASS)
					.flat(0, 4, 14, 12, 65, 6, s(Blocks.POLISHED_ANDESITE))
					.flat(-16, -20, 11, 8, 71, 6, s(Blocks.STONE_BRICKS))
					.path(s(Blocks.STONE_BRICKS), 4, -14, -6, 14, -6, 18, 4, 14, 16, -14, 16, -18, 4, -14, -6)
					.path(s(Blocks.STONE_BRICKS), 3, 14, 4, 38, 4)
					.path(s(Blocks.STONE_BRICKS), 3, -8, -6, -14, -12)
					.path(s(Blocks.STONE_BRICKS), 3, 0, 16, 0, 32)
					.reach(70);
			anchor(Anchor.of(Type.QUEST, 0, -3));
			anchor(Anchor.at(Type.FRUIT_DEALER, 22, 66, 15));
			anchor(Anchor.of(Type.FISHERMAN, 50, 4));
			anchor(Anchor.of(Type.NAVIGATOR, 38, 7));
			anchor(Anchor.at(Type.DOCK, 54, 63, 9));
			anchor(Anchor.of(Type.TREASURE, -32, 22));
		}

		@Override
		public void build(Canvas c) {
			Kit.fountain(c, 0, 4, 65, 4, Blocks.SMOOTH_STONE_SLAB);
			// Town hall on the hill with its clock tower.
			Kit.house(c, -24, -26, -8, -14, 71, 2, Palette.BLUE_TOWN, Direction.SOUTH, Interior.HALL);
			Kit.tower(c, -16, -30, 3, 71, 17, Blocks.SMOOTH_SANDSTONE, Blocks.DARK_PRISMARINE_STAIRS, Blocks.DARK_PRISMARINE_SLAB, Direction.SOUTH);
			c.set(-16, 84, -26, s(Blocks.QUARTZ_BLOCK)); // clock face
			c.set(-16, 85, -26, s(Blocks.BELL));
			// Houses around the ring road, doors facing the plaza.
			Kit.house(c, 4, -20, 12, -12, 65, 1, Palette.OAK_TOWN, Direction.SOUTH, Interior.HOME);
			Kit.house(c, 22, -14, 30, -6, 65, 2, Palette.OAK_TOWN, Direction.WEST, Interior.HOME);
			Kit.house(c, 22, 10, 30, 20, 65, 1, Palette.OAK_TOWN, Direction.WEST, Interior.SHOP);
			Kit.house(c, 4, 22, 12, 30, 65, 1, Palette.OAK_TOWN, Direction.NORTH, Interior.HOME);
			Kit.house(c, -14, 22, -6, 30, 65, 2, Palette.OAK_TOWN, Direction.NORTH, Interior.HOME);
			Kit.house(c, -32, 0, -24, 10, 65, 1, Palette.OAK_TOWN, Direction.EAST, Interior.HOME);
			// Market stalls on the plaza's south side.
			Kit.stall(c, -8, 13, 65, Blocks.RED_WOOL, Direction.NORTH);
			Kit.stall(c, 0, 14, 65, Blocks.YELLOW_WOOL, Direction.NORTH);
			Kit.stall(c, 8, 13, 65, Blocks.LIGHT_BLUE_WOOL, Direction.NORTH);
			for (int[] l : new int[][]{{-12, -4}, {12, -4}, {-12, 12}, {12, 12}, {28, 4}, {0, 28}}) {
				Kit.lamp(c, l[0], l[1], c.ground(l[0], l[1]));
			}
			// Harbour: long east pier with the navigator's landing, short south pier, lighthouse on the point.
			Kit.pier(c, 36, 4, Direction.EAST, 22, 2);
			Kit.pier(c, 0, 33, Direction.SOUTH, 12, 1);
			Kit.tower(c, 38, -20, 2, c.ground(38, -20) + 1, 14, Blocks.WHITE_CONCRETE, Blocks.RED_NETHER_BRICK_STAIRS, Blocks.RED_NETHER_BRICK_SLAB, Direction.WEST);
			c.set(38, c.ground(38, -20) + 16, -20, s(Blocks.GLOWSTONE));
			for (int[] t : new int[][]{{-34, -12}, {-38, 12}, {30, -26}, {-2, -30}, {34, 22}, {-24, 30}, {16, 30}}) {
				Kit.roundTree(c, t[0], t[1], 5, (t[0] + t[1]) % 2 == 0 ? Blocks.OAK_LOG : Blocks.BIRCH_LOG,
						(t[0] + t[1]) % 2 == 0 ? Blocks.OAK_LEAVES : Blocks.BIRCH_LEAVES);
			}
			treasure(c);
		}
	}

	// ==================================================================================================== Lv 15

	/**
	 * The original Verdant Wilds (one island, four-tier temple hill). Kept for worlds created before the redesign, so
	 * their half-explored island never mixes with the new one; new worlds use {@link VerdantWilds}.
	 */
	static final class VerdantWildsLegacy extends BasePlan {
		VerdantWildsLegacy() {
			super("Verdant Wilds", 0.1);
			terrain.mass(0, 0, 50, 44, 8, 1.7, Ground.GRASS)
					.mass(6, -12, 24, 22, 16, 1.4, Ground.PODZOL)
					.flat(6, -12, 14, 14, 79, 6, null)
					.path(s(Blocks.COARSE_DIRT), 3, 0, 44, 2, 22, 6, 4)
					.path(s(Blocks.COARSE_DIRT), 2, 2, 22, -26, 12)
					.path(s(Blocks.COARSE_DIRT), 2, 2, 22, 24, 24);
			anchor(Anchor.of(Type.QUEST, 3, 34));
			anchor(Anchor.of(Type.FISHERMAN, 0, 56));
			anchor(Anchor.at(Type.DOCK, 4, 63, 58));
			anchor(Anchor.area(Type.ENEMY_CAMP, -26, 12, 8));
			anchor(Anchor.area(Type.ENEMY_CAMP, 24, 24, 8));
			anchor(Anchor.at(Type.BOSS_ARENA, 6, 92, -12));
			anchor(Anchor.of(Type.TREASURE, -36, -14));
		}

		@Override
		public void build(Canvas c) {
			int base = 79;
			Block[] stone = {Blocks.MOSSY_STONE_BRICKS, Blocks.STONE_BRICKS, Blocks.CRACKED_STONE_BRICKS};
			for (int tier = 0; tier < 4; tier++) {
				int half = 12 - tier * 3;
				int y0 = base + tier * 3;
				for (int x = -half; x <= half; x++) {
					for (int z = -half; z <= half; z++) {
						Block b = stone[Math.floorMod(x * 7 + z * 13 + tier, 3)];
						c.foundation(6 + x, -12 + z, 6 + x, -12 + z, y0 + 2, net.minecraft.world.level.block.Blocks.STONE_BRICKS.defaultBlockState());
						c.fill(6 + x, y0, -12 + z, 6 + x, y0 + 2, -12 + z, s(b));
					}
				}
			}
			// Hollow inner chamber in the lower two tiers, entered from the south through an arched doorway.
			c.fill(6 - 5, base + 1, -12 - 5, 6 + 5, base + 5, -12 + 5, s(Blocks.AIR));
			c.fill(6 - 1, base + 1, -12 + 6, 6 + 1, base + 3, -12 + 12, s(Blocks.AIR));
			for (int[] p : new int[][]{{-4, -4}, {4, -4}, {-4, 4}, {4, 4}}) {
				c.set(6 + p[0], base + 4, -12 + p[1], s(Blocks.SHROOMLIGHT));
			}
			c.set(6, base + 1, -12 - 4, s(Blocks.CHISELED_STONE_BRICKS));
			c.set(6, base + 2, -12 - 4, s(Blocks.LODESTONE));
			// Grand stairway up the south face to the summit.
			for (int step = 0; step <= 12; step++) {
				for (int w = -2; w <= 2; w++) {
					int y = base + step;
					int z = -12 + 13 - step;
					c.set(6 + w, y, z, Kit.stairs(Blocks.STONE_BRICK_STAIRS, Direction.NORTH, false));
					c.fill(6 + w, y + 1, z, 6 + w, y + 4, z, s(Blocks.AIR));
				}
			}
			c.fill(6 - 1, base + 1, -12 + 6, 6 + 1, base + 3, -12 + 12, s(Blocks.AIR)); // keep the doorway open under the stairs
			// Summit shrine: open pillared hall with a hipped roof and a glowing altar.
			int top = base + 12;
			c.fill(6 - 3, top, -12 - 3, 6 + 3, top, -12 + 3, s(Blocks.CHISELED_STONE_BRICKS));
			for (int[] p : new int[][]{{-3, -3}, {3, -3}, {-3, 3}, {3, 3}}) {
				c.fill(6 + p[0], top + 1, -12 + p[1], 6 + p[0], top + 4, -12 + p[1], s(Blocks.MOSSY_STONE_BRICK_WALL));
			}
			Kit.hipRoof(c, 6 - 4, -12 - 4, 6 + 4, -12 + 4, top + 5, Blocks.MOSSY_STONE_BRICK_STAIRS, Blocks.MOSSY_STONE_BRICK_SLAB, Blocks.MOSSY_STONE_BRICKS);
			c.set(6, top + 1, -12, s(Blocks.SEA_LANTERN));
			// Giant trees around the temple, palms on the beaches.
			for (int[] t : new int[][]{{-24, -20}, {-30, 4}, {-14, 22}, {28, -6}, {32, 14}, {20, -32}, {-8, -34}, {-34, -30}, {14, 34}}) {
				Kit.bigJungleTree(c, t[0], t[1], 14 + Math.floorMod(t[0] * t[1], 6));
			}
			for (int[] t : new int[][]{{-42, 18}, {40, 30}, {-20, 42}, {44, -10}}) {
				Kit.palm(c, t[0], t[1], 6);
			}
			Kit.tent(c, -26, 12, c.ground(-26, 12), Blocks.BROWN_WOOL);
			Kit.tent(c, 24, 24, c.ground(24, 24), Blocks.GREEN_WOOL);
			Kit.pier(c, 1, 44, Direction.SOUTH, 14, 1);
			treasure(c);
		}
	}

	// ==================================================================================================== Lv 30

	/** Farming village: two windmills, three crop fields, cottages around a square, a barn and a pond. */
	static final class MillbrookVillage extends BasePlan {
		MillbrookVillage() {
			super("Millbrook Village", 0.07);
			terrain.mass(0, 0, 48, 40, 4, 2.8, Ground.GRASS)
					.flat(0, 0, 10, 8, 65, 5, s(Blocks.DIRT_PATH))
					.flat(-4, 24, 7, 5, 62, 3, s(Blocks.WATER))
					.path(s(Blocks.DIRT_PATH), 3, -44, 0, 44, 0)
					.path(s(Blocks.DIRT_PATH), 3, 0, -34, 0, 34);
			anchor(Anchor.of(Type.QUEST, 2, -2));
			anchor(Anchor.of(Type.FISHERMAN, -56, 0));
			anchor(Anchor.at(Type.DOCK, -58, 63, 4));
			anchor(Anchor.area(Type.ENEMY_CAMP, 30, 26, 8));
			anchor(Anchor.area(Type.ENEMY_CAMP, -30, -28, 7));
			anchor(Anchor.of(Type.TREASURE, 36, -30));
		}

		@Override
		public void build(Canvas c) {
			windmill(c, 22, 16, 65, Direction.SOUTH);
			windmill(c, -10, -26, 65, Direction.NORTH);
			Kit.field(c, -32, -18, -12, -6, 65, Blocks.WHEAT);
			Kit.field(c, 12, -26, 30, -10, 65, Blocks.CARROTS);
			Kit.field(c, -32, 8, -16, 22, 65, Blocks.POTATOES);
			Kit.house(c, 6, 4, 13, 11, 65, 1, Palette.COTTAGE, Direction.WEST, Interior.HOME);
			Kit.house(c, -14, 3, -7, 10, 65, 1, Palette.COTTAGE, Direction.EAST, Interior.HOME);
			Kit.house(c, 6, -12, 13, -5, 65, 1, Palette.COTTAGE, Direction.WEST, Interior.HOME);
			Kit.house(c, 26, -4, 38, 6, 65, 1, new Palette(Blocks.RED_TERRACOTTA, Blocks.DARK_OAK_LOG, Blocks.SPRUCE_PLANKS,
					Blocks.DARK_OAK_STAIRS, Blocks.DARK_OAK_SLAB, Blocks.SPRUCE_DOOR, Blocks.GLASS_PANE), Direction.WEST, Interior.STORE);
			c.fill(30, 66, 8, 33, 67, 10, s(Blocks.HAY_BLOCK));
			Kit.well(c, 0, 0, 65);
			for (int[] t : new int[][]{{-38, -8}, {38, 18}, {-20, 32}, {18, 32}, {36, -18}, {-38, 24}}) {
				Kit.roundTree(c, t[0], t[1], 5, Blocks.OAK_LOG, Blocks.OAK_LEAVES);
			}
			Kit.tent(c, 30, 26, c.ground(30, 26), Blocks.GRAY_WOOL);
			Kit.tent(c, -30, -28, c.ground(-30, -28), Blocks.GRAY_WOOL);
			Kit.pier(c, -44, 0, Direction.WEST, 14, 1);
			treasure(c);
		}
	}

	// ==================================================================================================== Lv 60

	/** Hillside port: three brick terraces joined by a stone stair street, a keep on top and a harbour below. */
	static final class RedbrickPort extends BasePlan {
		RedbrickPort() {
			super("Redbrick Port", 0.08);
			terrain.mass(0, 0, 46, 40, 5, 2.2, Ground.GRASS)
					.mass(-6, -14, 28, 20, 15, 1.5, Ground.STONE)
					.flat(0, 18, 30, 9, 64, 4, s(Blocks.STONE_BRICKS))
					.flat(-6, -2, 22, 7, 69, 3, null)
					.flat(-6, -18, 12, 8, 75, 4, s(Blocks.POLISHED_ANDESITE))
					.path(s(Blocks.STONE_BRICKS), 4, 0, 40, 0, 18, -6, -2, -6, -18);
			anchor(Anchor.of(Type.QUEST, 3, 22));
			anchor(Anchor.of(Type.FISHERMAN, 0, 52));
			anchor(Anchor.at(Type.DOCK, 5, 63, 54));
			anchor(Anchor.area(Type.ENEMY_CAMP, -30, 6, 8));
			anchor(Anchor.area(Type.ENEMY_CAMP, 28, -8, 8));
			anchor(Anchor.area(Type.BOSS_ARENA, -6, -16, 6));
			anchor(Anchor.of(Type.TREASURE, 32, 26));
		}

		@Override
		public void build(Canvas c) {
			for (int i = 0; i < 4; i++) {
				int x = -28 + i * 14;
				if (x > -4 && x < 4) {
					continue;
				}
				Kit.house(c, x, 22, x + 8, 30, 64, 1 + (i % 2), Palette.BRICK, Direction.NORTH, Interior.HOME);
			}
			Kit.house(c, -26, -6, -18, 1, 69, 2, Palette.BRICK, Direction.SOUTH, Interior.HOME);
			Kit.house(c, 6, -6, 14, 1, 69, 1, Palette.BRICK, Direction.SOUTH, Interior.STORE);
			// The keep: three storeys of stone with two flanking towers.
			Kit.house(c, -14, -30, 2, -22, 75, 3, Palette.STONE, Direction.SOUTH, Interior.HALL);
			Kit.tower(c, -18, -26, 3, 75, 16, Blocks.STONE_BRICKS, Blocks.DEEPSLATE_TILE_STAIRS, Blocks.DEEPSLATE_TILE_SLAB, Direction.EAST);
			Kit.tower(c, 6, -26, 3, 75, 16, Blocks.STONE_BRICKS, Blocks.DEEPSLATE_TILE_STAIRS, Blocks.DEEPSLATE_TILE_SLAB, Direction.WEST);
			for (int[] l : new int[][]{{-4, 14}, {4, 14}, {-10, 0}, {-2, -12}, {-10, -12}}) {
				Kit.lamp(c, l[0], l[1], c.ground(l[0], l[1]));
			}
			// Sea wall along the harbour front and a broad pier.
			c.fill(-30, 63, 28, -4, 65, 28, s(Blocks.STONE_BRICK_WALL));
			c.fill(4, 63, 28, 30, 65, 28, s(Blocks.STONE_BRICK_WALL));
			Kit.pier(c, 0, 36, Direction.SOUTH, 18, 3);
			for (int[] t : new int[][]{{-36, -14}, {34, 6}, {24, -24}, {-36, 20}}) {
				Kit.roundTree(c, t[0], t[1], 5, Blocks.DARK_OAK_LOG, Blocks.DARK_OAK_LEAVES);
			}
			Kit.tent(c, -30, 6, c.ground(-30, 6), Blocks.RED_WOOL);
			treasure(c);
		}
	}

	// ==================================================================================================== Lv 75

	/** Desert: rolling dunes, a stepped pyramid with an entrance tunnel and burial chamber, a palm oasis. */
	static final class SunscorchDunes extends BasePlan {
		SunscorchDunes() {
			super("Sunscorch Dunes", 0.1);
			terrain.mass(0, 0, 50, 44, 4, 2.0, Ground.SAND)
					.mass(24, 20, 14, 10, 8, 1.3, Ground.SAND)
					.mass(-26, -16, 12, 10, 7, 1.3, Ground.SAND)
					.flat(6, -12, 17, 17, 66, 5, s(Blocks.SMOOTH_SANDSTONE))
					.flat(-20, 18, 7, 5, 62, 3, s(Blocks.WATER))
					.path(s(Blocks.SMOOTH_SANDSTONE), 3, 44, 0, 6, 8, 6, 5);
			anchor(Anchor.of(Type.QUEST, 10, 10));
			anchor(Anchor.of(Type.FISHERMAN, 58, 0));
			anchor(Anchor.at(Type.DOCK, 60, 63, 4));
			anchor(Anchor.area(Type.ENEMY_CAMP, 28, -26, 8));
			anchor(Anchor.area(Type.ENEMY_CAMP, -30, -30, 7));
			anchor(Anchor.at(Type.BOSS_ARENA, 6, 67, -12));
			anchor(Anchor.of(Type.TREASURE, -36, 2));
		}

		@Override
		public void build(Canvas c) {
			int base = 66;
			for (int tier = 0; tier < 8; tier++) {
				int half = 15 - tier * 2;
				int y0 = base + 1 + tier * 2;
				c.fill(6 - half, y0, -12 - half, 6 + half, y0 + 1, -12 + half, s(tier % 2 == 0 ? Blocks.SANDSTONE : Blocks.CUT_SANDSTONE));
			}
			c.set(6, base + 17, -12, s(Blocks.GOLD_BLOCK));
			// Entrance tunnel from the south into a lit burial chamber.
			c.fill(6 - 1, base + 1, -12 + 4, 6 + 1, base + 3, -12 + 15, s(Blocks.AIR));
			c.fill(6 - 4, base + 1, -12 - 4, 6 + 4, base + 5, -12 + 4, s(Blocks.AIR));
			c.fill(6 - 2, base + 4, -12 + 15, 6 + 2, base + 4, -12 + 15, s(Blocks.CHISELED_SANDSTONE));
			for (int[] p : new int[][]{{-4, -4}, {4, -4}, {-4, 4}, {4, 4}}) {
				c.fill(6 + p[0], base + 1, -12 + p[1], 6 + p[0], base + 5, -12 + p[1], s(Blocks.CUT_SANDSTONE));
				c.set(6 + p[0], base + 3, -12 + p[1] + (p[1] > 0 ? -1 : 1), s(Blocks.GLOWSTONE));
			}
			c.set(6, base + 1, -12 - 3, s(Blocks.CHISELED_SANDSTONE));
			// Oasis and market tents.
			for (int[] t : new int[][]{{-28, 12}, {-12, 14}, {-24, 26}, {-14, 24}, {30, 4}, {40, -14}, {-8, 34}}) {
				Kit.palm(c, t[0], t[1], 6 + Math.floorMod(t[0], 3));
			}
			Kit.stall(c, 22, 6, c.ground(22, 6), Blocks.ORANGE_WOOL, Direction.WEST);
			Kit.stall(c, 22, 14, c.ground(22, 14), Blocks.WHITE_WOOL, Direction.WEST);
			Kit.tent(c, 28, -26, c.ground(28, -26), Blocks.ORANGE_WOOL);
			Kit.tent(c, -30, -30, c.ground(-30, -30), Blocks.ORANGE_WOOL);
			Kit.pier(c, 44, 0, Direction.EAST, 14, 1);
			treasure(c);
		}
	}

	// ==================================================================================================== Lv 90

	/** Snow island: a stone peak with a snow cap, a frozen waterfall into an ice pond, pines, a lodge and igloos. */
	static final class FrostpeakIsle extends BasePlan {
		FrostpeakIsle() {
			super("Frostpeak Isle", 0.1);
			terrain.mass(0, 0, 48, 42, 5, 2.0, Ground.SNOW)
					.mass(4, -10, 26, 24, 26, 1.0, Ground.STONE)
					.flat(4, 18, 7, 5, 62, 2, s(Blocks.ICE))
					.flat(-20, 14, 10, 8, 65, 4, null)
					.path(s(Blocks.PACKED_ICE), 3, 0, 44, -6, 24, -20, 14);
			anchor(Anchor.of(Type.QUEST, -18, 22));
			anchor(Anchor.of(Type.FISHERMAN, 2, 56));
			anchor(Anchor.at(Type.DOCK, 6, 63, 58));
			anchor(Anchor.area(Type.ENEMY_CAMP, -28, -22, 8));
			anchor(Anchor.area(Type.ENEMY_CAMP, 30, -18, 8));
			anchor(Anchor.area(Type.BOSS_ARENA, 4, -10, 4));
			anchor(Anchor.of(Type.TREASURE, 34, 24));
		}

		@Override
		public void build(Canvas c) {
			// Snow cap and the frozen waterfall down the south face.
			for (int x = -20; x <= 28; x++) {
				for (int z = -34; z <= 14; z++) {
					int g = c.ground(x, z);
					if (g >= 80) {
						c.set(x, g, z, s(Blocks.SNOW_BLOCK));
						c.set(x, g + 1, z, s(Blocks.SNOW));
					}
				}
			}
			for (int z = -2; z <= 14; z++) {
				for (int x = 3; x <= 5; x++) {
					int g = c.ground(x, z);
					c.set(x, g, z, s(Blocks.BLUE_ICE));
					c.set(x, g + 1, z, s(x == 4 ? Blocks.PACKED_ICE : Blocks.AIR));
				}
			}
			Kit.house(c, -28, 8, -14, 18, 65, 2, Palette.SNOW_LODGE, Direction.SOUTH, Interior.HALL);
			c.fill(-21, 66, 12, -21, 67, 12, s(Blocks.CAMPFIRE));
			for (int[] ig : new int[][]{{22, 20}, {30, 6}}) {
				igloo(c, ig[0], ig[1], c.ground(ig[0], ig[1]));
			}
			for (int[] t : new int[][]{{-34, -6}, {-30, 30}, {-12, 34}, {24, 32}, {38, -4}, {-38, 10}, {16, 40}, {-24, -32}, {34, -30}}) {
				Kit.pine(c, t[0], t[1], 9 + Math.floorMod(t[0] + t[1], 4), true);
			}
			Kit.tent(c, -28, -22, c.ground(-28, -22), Blocks.LIGHT_GRAY_WOOL);
			Kit.tent(c, 30, -18, c.ground(30, -18), Blocks.LIGHT_GRAY_WOOL);
			Kit.pier(c, 2, 44, Direction.SOUTH, 14, 1);
			treasure(c);
		}

		private static void igloo(Canvas c, int x, int z, int y) {
			for (int dx = -4; dx <= 4; dx++) {
				for (int dz = -4; dz <= 4; dz++) {
					for (int dy = 0; dy <= 4; dy++) {
						double d = Math.sqrt(dx * dx + dz * dz + dy * dy * 1.4);
						if (d <= 4.4) {
							c.set(x + dx, y + 1 + dy, z + dz, d > 3.4 ? s(Blocks.SNOW_BLOCK) : s(Blocks.AIR));
						}
					}
				}
			}
			c.fill(x - 3, y, z - 3, x + 3, y, z + 3, s(Blocks.SNOW_BLOCK));
			c.fill(x, y + 1, z + 4, x, y + 2, z + 5, s(Blocks.AIR));
			c.set(x, y + 1, z, s(Blocks.LANTERN));
		}
	}

	// ==================================================================================================== Lv 120

	/** Fortress on a rock plateau: curtain walls, four corner towers, a gatehouse, barracks and a central keep. */
	static final class IronholdBastion extends BasePlan {
		IronholdBastion() {
			super("Ironhold Bastion", 0.06);
			terrain.mass(0, 0, 40, 40, 5, 3.0, Ground.STONE)
					.flat(0, 0, 23, 23, 66, 4, s(Blocks.POLISHED_ANDESITE))
					.path(s(Blocks.STONE_BRICKS), 3, 0, 22, 0, 40);
			anchor(Anchor.of(Type.QUEST, 2, 32));
			anchor(Anchor.of(Type.FISHERMAN, 0, 50));
			anchor(Anchor.at(Type.DOCK, 4, 63, 52));
			anchor(Anchor.area(Type.ENEMY_CAMP, 0, 12, 10));
			anchor(Anchor.area(Type.ENEMY_CAMP, -14, -14, 6));
			anchor(Anchor.area(Type.BOSS_ARENA, 0, 8, 6));
			anchor(Anchor.of(Type.TREASURE, -32, 30));
		}

		@Override
		public void build(Canvas c) {
			curtainWall(c, -22, -22, 22, 22, 66, 9, Blocks.STONE_BRICKS, 0, 22);
			for (int[] t : new int[][]{{-22, -22}, {22, -22}, {-22, 22}, {22, 22}}) {
				Kit.tower(c, t[0], t[1], 3, 66, 14, Blocks.STONE_BRICKS, Blocks.DEEPSLATE_TILE_STAIRS, Blocks.DEEPSLATE_TILE_SLAB,
						t[1] < 0 ? Direction.SOUTH : Direction.NORTH);
			}
			c.fill(-2, 71, 22, 2, 71, 22, Kit.connected(Blocks.IRON_BARS, true)); // portcullis above the gate
			Kit.house(c, -8, -14, 8, 0, 66, 3, Palette.STONE, Direction.SOUTH, Interior.HALL);
			Kit.hipRoof(c, -9, -15, 9, 1, 79, Blocks.DEEPSLATE_TILE_STAIRS, Blocks.DEEPSLATE_TILE_SLAB, Blocks.DEEPSLATE_TILES);
			Kit.house(c, 12, -16, 19, -4, 66, 1, Palette.STONE, Direction.WEST, Interior.STORE);
			Kit.house(c, -19, 4, -12, 16, 66, 1, Palette.STONE, Direction.EAST, Interior.HOME);
			for (int[] l : new int[][]{{-6, 10}, {6, 10}, {-6, 18}, {6, 18}}) {
				Kit.lamp(c, l[0], l[1], 66);
			}
			Kit.pier(c, 0, 40, Direction.SOUTH, 10, 2);
			treasure(c);
		}
	}

	// ==================================================================================================== Lv 150

	/** Sky islands: a sea-level landing with a stair tower up to four floating islets joined by rope bridges. */
	static final class CloudtopIsles extends BasePlan {
		CloudtopIsles() {
			super("Cloudtop Isles", 0.08);
			terrain.mass(0, 0, 24, 20, 3, 2.0, Ground.GRASS)
					.flat(0, -4, 8, 6, 65, 3, s(Blocks.SMOOTH_STONE))
					.reach(90);
			anchor(Anchor.of(Type.QUEST, 4, 6));
			anchor(Anchor.of(Type.FISHERMAN, 0, 34));
			anchor(Anchor.at(Type.DOCK, 4, 63, 36));
			anchor(Anchor.at(Type.ENEMY_CAMP, 28, 105, -48));
			anchor(Anchor.at(Type.ENEMY_CAMP, -26, 113, -54));
			anchor(Anchor.at(Type.BOSS_ARENA, 0, 125, -78));
			anchor(Anchor.of(Type.TREASURE, -14, 10));
		}

		@Override
		public void build(Canvas c) {
			// Stair tower from the landing up to the first islet.
			Kit.tower(c, 0, -12, 3, 65, 27, Blocks.SMOOTH_STONE, Blocks.QUARTZ_STAIRS, Blocks.QUARTZ_SLAB, Direction.SOUTH);
			c.fill(0, 93, -16, 0, 94, -16, s(Blocks.AIR));
			floatingIslet(c, 0, -32, 10, 92, Blocks.GRASS_BLOCK, Blocks.STONE);
			floatingIslet(c, 28, -48, 8, 104, Blocks.GRASS_BLOCK, Blocks.STONE);
			floatingIslet(c, -26, -54, 10, 112, Blocks.GRASS_BLOCK, Blocks.STONE);
			floatingIslet(c, 0, -78, 13, 124, Blocks.CALCITE, Blocks.STONE);
			bridge(c, 0, 93, -15, 0, 92, -22);
			bridge(c, 8, 92, -38, 21, 104, -45);
			bridge(c, -8, 92, -38, -18, 112, -48);
			bridge(c, 28, 104, -56, 6, 124, -68);
			bridge(c, -22, 112, -63, -8, 124, -70);
			// Islet buildings, the summit shrine and trees.
			Kit.house(c, -30, -58, -22, -50, 113, 1, Palette.COTTAGE, Direction.SOUTH, Interior.HOME);
			Kit.house(c, 24, -52, 31, -45, 105, 1, Palette.COTTAGE, Direction.WEST, Interior.STORE);
			for (int[] p : new int[][]{{-5, -83}, {5, -83}, {-5, -73}, {5, -73}}) {
				c.fill(p[0], 125, p[1], p[0], 130, p[1], s(Blocks.QUARTZ_PILLAR));
			}
			Kit.hipRoof(c, -6, -84, 6, -72, 131, Blocks.QUARTZ_STAIRS, Blocks.QUARTZ_SLAB, Blocks.QUARTZ_BLOCK);
			c.set(0, 125, -78, s(Blocks.SEA_LANTERN));
			for (int[] t : new int[][]{{-4, -36}, {6, -28}, {32, -44}}) {
				islandTree(c, t[0], t[1], t[0] == 32 ? 105 : 93);
			}
			Kit.roundTree(c, -12, 6, 5, Blocks.BIRCH_LOG, Blocks.BIRCH_LEAVES);
			Kit.roundTree(c, 14, 4, 5, Blocks.BIRCH_LOG, Blocks.BIRCH_LEAVES);
			Kit.pier(c, 2, 20, Direction.SOUTH, 14, 1);
			treasure(c);
		}
	}

	/** Small birch tree standing on a floating islet top at {@code y}. */
	static void islandTree(Canvas c, int x, int z, int y) {
		BlockStateHolder.tree(c, x, z, y);
	}

	// ==================================================================================================== Lv 190

	/** Prison: walled compound with two cell blocks, a central watchtower and an exercise yard. */
	static final class StonewallPrison extends BasePlan {
		StonewallPrison() {
			super("Stonewall Prison", 0.06);
			terrain.mass(0, 0, 44, 38, 4, 3.0, Ground.STONE)
					.flat(0, 0, 27, 23, 66, 4, s(Blocks.GRAVEL))
					.path(s(Blocks.STONE_BRICKS), 3, 26, 0, 44, 0);
			anchor(Anchor.of(Type.QUEST, 34, 2));
			anchor(Anchor.of(Type.FISHERMAN, 54, 0));
			anchor(Anchor.at(Type.DOCK, 56, 63, 4));
			anchor(Anchor.area(Type.ENEMY_CAMP, -6, 0, 12));
			anchor(Anchor.area(Type.BOSS_ARENA, 4, 4, 6));
			anchor(Anchor.of(Type.TREASURE, -34, -28));
		}

		@Override
		public void build(Canvas c) {
			curtainWall(c, -26, -22, 26, 22, 66, 10, Blocks.STONE_BRICKS, 26, 0);
			for (int[] t : new int[][]{{-26, -22}, {26, -22}, {-26, 22}, {26, 22}}) {
				Kit.tower(c, t[0], t[1], 2, 66, 14, Blocks.STONE_BRICKS, Blocks.STONE_BRICK_STAIRS, Blocks.STONE_BRICK_SLAB,
						t[0] < 0 ? Direction.EAST : Direction.WEST);
			}
			cellBlock(c, -22, -20, 18, -12, Direction.SOUTH);
			cellBlock(c, -22, 12, 18, 20, Direction.NORTH);
			Kit.tower(c, 0, 0, 3, 66, 20, Blocks.POLISHED_ANDESITE, Blocks.DEEPSLATE_TILE_STAIRS, Blocks.DEEPSLATE_TILE_SLAB, Direction.EAST);
			c.set(0, 88, 0, s(Blocks.REDSTONE_LAMP));
			Kit.pier(c, 44, 0, Direction.EAST, 14, 2);
			treasure(c);
		}

		private static void cellBlock(Canvas c, int x1, int z1, int x2, int z2, Direction front) {
			Kit.house(c, x1, z1, x2, z2, 66, 2, new Palette(Blocks.STONE_BRICKS, Blocks.POLISHED_ANDESITE, Blocks.SMOOTH_STONE,
					Blocks.STONE_BRICK_STAIRS, Blocks.STONE_BRICK_SLAB, Blocks.IRON_DOOR, Blocks.IRON_BARS), front, Kit.Interior.NONE);
			int inner = front == Direction.SOUTH ? z1 + 3 : z2 - 3;
			for (int x = x1 + 1; x < x2; x++) {
				for (int floor = 0; floor < 2; floor++) {
					int y = 67 + floor * 4;
					boolean wall = (x - x1) % 4 == 0;
					for (int dy = 0; dy < 3; dy++) {
						c.set(x, y + dy, inner, wall ? s(Blocks.STONE_BRICKS) : Kit.connected(Blocks.IRON_BARS, true));
					}
				}
			}
		}
	}

	// ==================================================================================================== Lv 225

	/** Colosseum: oval sand arena ringed by stepped stone seating and an arched outer wall. */
	static final class GladiatorsRing extends BasePlan {
		GladiatorsRing() {
			super("Gladiator's Ring", 0.07);
			terrain.mass(0, 0, 46, 42, 4, 3.0, Ground.SAND)
					.flat(0, -4, 30, 26, 64, 4, s(Blocks.SMOOTH_SANDSTONE))
					.path(s(Blocks.SMOOTH_SANDSTONE), 3, 0, 22, 0, 40);
			anchor(Anchor.of(Type.QUEST, 3, 28));
			anchor(Anchor.of(Type.FISHERMAN, 0, -54));
			anchor(Anchor.at(Type.DOCK, 4, 63, -56));
			anchor(Anchor.area(Type.ENEMY_CAMP, 0, -4, 10));
			anchor(Anchor.area(Type.ENEMY_CAMP, 32, 26, 7));
			anchor(Anchor.area(Type.BOSS_ARENA, 0, -4, 8));
			anchor(Anchor.of(Type.TREASURE, -36, 24));
		}

		@Override
		public void build(Canvas c) {
			int y = 64;
			double inRx = 14, inRz = 11;
			for (int x = -27; x <= 27; x++) {
				for (int z = -27; z <= 19; z++) {
					double dx = x, dz = z + 4;
					double e = Math.sqrt((dx / inRx) * (dx / inRx) + (dz / inRz) * (dz / inRz));
					if (e <= 1.0) {
						c.set(x, y, z, s(Blocks.SAND));
						c.fill(x, y + 1, z, x, y + 16, z, s(Blocks.AIR));
						continue;
					}
					// Ring index outward from the arena wall (each ring = 2 blocks wide, 2 blocks higher).
					double ringD = (e - 1.0) * Math.min(inRx, inRz);
					int ring = (int) (ringD / 2.0);
					boolean gate = Math.abs(x) <= 1 && z > 0;
					if (ring < 6) {
						int top = y + 2 + ring * 2;
						c.fill(x, y, z, x, top - 1, z, s(Blocks.SANDSTONE));
						Direction toArena = Math.abs(dx / inRx) > Math.abs(dz / inRz) ? (dx > 0 ? Direction.WEST : Direction.EAST)
								: (dz > 0 ? Direction.NORTH : Direction.SOUTH);
						c.set(x, top, z, Kit.stairs(Blocks.SANDSTONE_STAIRS, toArena.getOpposite(), false));
						if (gate) {
							c.fill(x, y + 1, z, x, y + 3, z, s(Blocks.AIR));
						}
					} else if (ring == 6) {
						// Outer wall with arches.
						for (int yy = y; yy <= y + 16; yy++) {
							boolean arch = yy >= y + 1 && yy <= y + 3 && Math.floorMod((int) Math.round(Math.atan2(dz, dx) * 20), 4) == 0;
							c.set(x, yy, z, arch || (gate && yy <= y + 3) ? s(Blocks.AIR) : s(yy % 5 == 0 ? Blocks.CHISELED_SANDSTONE : Blocks.CUT_SANDSTONE));
						}
					}
				}
			}
			for (int[] p : new int[][]{{-14, -4}, {14, -4}, {0, -15}, {0, 7}}) {
				c.set(p[0], y + 1, p[1], s(Blocks.CAMPFIRE));
			}
			for (int[] t : new int[][]{{-36, -20}, {36, -16}, {-30, 30}, {38, 10}}) {
				Kit.palm(c, t[0], t[1], 6);
			}
			Kit.tent(c, 32, 26, c.ground(32, 26), Blocks.RED_WOOL);
			Kit.pier(c, 0, -40, Direction.NORTH, 14, 1);
			treasure(c);
		}
	}

	// ==================================================================================================== Lv 300

	/** Volcano: blackstone cone with a lava crater, magma streams, obsidian, dead trees and a basalt arena ledge. */
	static final class EmberfallIsle extends BasePlan {
		EmberfallIsle() {
			super("Emberfall Isle", 0.1);
			terrain.mass(0, 0, 44, 40, 5, 2.0, Ground.BASALT)
					.mass(0, -6, 26, 24, 32, 1.05, Ground.BASALT)
					.flat(-22, 20, 9, 7, 66, 3, s(Blocks.POLISHED_BLACKSTONE_BRICKS))
					.path(s(Blocks.POLISHED_BLACKSTONE), 3, 0, 44, -10, 26, -22, 20);
			anchor(Anchor.of(Type.QUEST, 3, 36));
			anchor(Anchor.of(Type.FISHERMAN, 0, 58));
			anchor(Anchor.at(Type.DOCK, 4, 63, 60));
			anchor(Anchor.area(Type.ENEMY_CAMP, 26, 22, 8));
			anchor(Anchor.area(Type.ENEMY_CAMP, -28, -18, 7));
			anchor(Anchor.area(Type.BOSS_ARENA, -22, 20, 6));
			anchor(Anchor.of(Type.TREASURE, 34, -20));
		}

		@Override
		public void build(Canvas c) {
			int peak = c.ground(0, -6);
			// Crater with a lava lake.
			for (int dx = -8; dx <= 8; dx++) {
				for (int dz = -8; dz <= 8; dz++) {
					double d = Math.sqrt(dx * dx + dz * dz);
					if (d > 8) {
						continue;
					}
					int g = c.ground(dx, -6 + dz);
					int floor = peak - 7 + (int) Math.round(d * 0.4);
					c.fill(dx, floor + 1, -6 + dz, dx, g, -6 + dz, s(Blocks.AIR));
					c.set(dx, floor, -6 + dz, d < 6 ? s(Blocks.LAVA) : s(Blocks.MAGMA_BLOCK));
					c.set(dx, floor - 1, -6 + dz, s(Blocks.BASALT));
				}
			}
			// Magma streams down three flanks (recessed so nothing flows).
			for (double angle : new double[]{0.4, 2.3, 4.2}) {
				for (int r = 8; r <= 30; r++) {
					int x = (int) Math.round(Math.cos(angle) * r);
					int z = -6 + (int) Math.round(Math.sin(angle) * r);
					int g = c.ground(x, z);
					c.set(x, g, z, s(Blocks.MAGMA_BLOCK));
					c.set(x + 1, g, z, s(r % 5 == 0 ? Blocks.SHROOMLIGHT : Blocks.MAGMA_BLOCK));
				}
			}
			for (int[] o : new int[][]{{20, -20}, {-30, 4}, {14, 30}, {-14, -30}}) {
				c.set(o[0], c.ground(o[0], o[1]) + 1, o[1], s(Blocks.OBSIDIAN));
			}
			for (int[] t : new int[][]{{-34, 0}, {30, 8}, {-16, 34}, {22, -32}, {36, -6}, {-8, 38}}) {
				Kit.deadTree(c, t[0], t[1], 5);
			}
			for (int[] p : new int[][]{{-28, 14}, {-16, 14}, {-28, 26}, {-16, 26}}) {
				c.fill(p[0], 67, p[1], p[0], 70, p[1], s(Blocks.POLISHED_BLACKSTONE_BRICK_WALL));
				c.set(p[0], 71, p[1], s(Blocks.SOUL_LANTERN));
			}
			Kit.tent(c, 26, 22, c.ground(26, 22), Blocks.BLACK_WOOL);
			Kit.pier(c, 1, 44, Direction.SOUTH, 14, 1);
			treasure(c);
		}
	}

	// ==================================================================================================== Lv 375

	/** Trade port: a two-storey trade hall with a clock tower, warehouses, cranes and a broad pier network. */
	static final class SaltmarketDocks extends BasePlan {
		SaltmarketDocks() {
			super("Saltmarket Docks", 0.05);
			terrain.mass(0, 0, 46, 34, 4, 2.6, Ground.GRASS)
					.flat(0, 0, 32, 22, 65, 4, s(Blocks.STONE_BRICKS))
					.path(s(Blocks.SMOOTH_STONE), 4, -30, 10, 30, 10);
			anchor(Anchor.of(Type.QUEST, 0, 4));
			anchor(Anchor.of(Type.FRUIT_DEALER, -12, 12));
			anchor(Anchor.of(Type.FISHERMAN, -20, 40));
			anchor(Anchor.at(Type.DOCK, 8, 63, 52));
			anchor(Anchor.area(Type.ENEMY_CAMP, 32, 6, 8));
			anchor(Anchor.at(Type.BOSS_ARENA, 0, 64, 48));
			anchor(Anchor.of(Type.TREASURE, -38, -18));
		}

		@Override
		public void build(Canvas c) {
			Kit.house(c, -18, -22, 18, -6, 65, 2, Palette.BRICK, Direction.SOUTH, Interior.SHOP);
			Kit.tower(c, 0, -26, 3, 65, 22, Blocks.BRICKS, Blocks.DARK_OAK_STAIRS, Blocks.DARK_OAK_SLAB, Direction.SOUTH);
			c.set(0, 82, -23, s(Blocks.QUARTZ_BLOCK));
			for (int i = 0; i < 3; i++) {
				int x = -30 + i * 22;
				Kit.house(c, x, 14, x + 10, 22, 65, 1, Palette.COTTAGE, Direction.NORTH, Interior.STORE);
			}
			for (int i = 0; i < 4; i++) {
				Kit.stall(c, -18 + i * 12, 8, 65, i % 2 == 0 ? Blocks.CYAN_WOOL : Blocks.ORANGE_WOOL, Direction.NORTH);
			}
			Kit.pier(c, 0, 26, Direction.SOUTH, 26, 4);
			Kit.pier(c, -20, 26, Direction.SOUTH, 14, 2);
			Kit.pier(c, 20, 26, Direction.SOUTH, 14, 2);
			for (int[] cr : new int[][]{{-6, 40}, {6, 34}}) {
				crane(c, cr[0], cr[1]);
			}
			for (int[] b : new int[][]{{3, 30}, {-3, 46}, {-20, 34}, {21, 36}}) {
				c.set(b[0], 64, b[1], Canvas.facing(s(Blocks.BARREL), Direction.UP));
			}
			Kit.tent(c, 32, 6, 65, Blocks.BLUE_WOOL);
			treasure(c);
		}

		private static void crane(Canvas c, int x, int z) {
			c.fill(x, 64, z, x, 72, z, s(Blocks.SPRUCE_LOG));
			c.fill(x + 1, 72, z, x + 5, 72, z, Kit.connected(Blocks.SPRUCE_FENCE, true));
			c.fill(x + 5, 68, z, x + 5, 71, z, s(Blocks.CHAIN));
			c.set(x + 5, 67, z, Canvas.facing(s(Blocks.BARREL), Direction.UP));
		}
	}

	// ==================================================================================================== Lv 450

	/** Palace: a four-tier pagoda on a raised purple stone plinth, a lotus pond with a bridge, cherry trees. */
	static final class LotusPalace extends BasePlan {
		LotusPalace() {
			super("Lotus Palace", 0.07);
			terrain.mass(0, 0, 44, 40, 5, 2.2, Ground.GRASS)
					.flat(0, -8, 20, 18, 69, 3, s(Blocks.PURPUR_BLOCK))
					.flat(0, 22, 11, 6, 62, 3, s(Blocks.WATER))
					.path(s(Blocks.PURPUR_BLOCK), 3, 0, 44, 0, 30)
					.path(s(Blocks.PURPUR_BLOCK), 3, 0, 14, 0, 10);
			anchor(Anchor.of(Type.QUEST, 3, 32));
			anchor(Anchor.of(Type.FISHERMAN, 28, 50));
			anchor(Anchor.at(Type.DOCK, 32, 63, 52));
			anchor(Anchor.area(Type.ENEMY_CAMP, -28, 18, 8));
			anchor(Anchor.area(Type.ENEMY_CAMP, 28, -24, 7));
			anchor(Anchor.area(Type.BOSS_ARENA, 0, 4, 6));
			anchor(Anchor.of(Type.TREASURE, 32, -28));
		}

		@Override
		public void build(Canvas c) {
			// Plinth rim of purpur pillars.
			for (int x = -20; x <= 20; x += 4) {
				for (int z : new int[]{-26, 10}) {
					c.fill(x, 70, z, x, 71, z, s(Blocks.PURPUR_PILLAR));
					c.set(x, 72, z, s(Blocks.LANTERN));
				}
			}
			int y = 69;
			for (int tier = 0; tier < 4; tier++) {
				int half = 8 - tier * 2;
				int y0 = y + tier * 5;
				Kit.house(c, -half, -10 - half, half, -10 + half, y0, 1, Palette.PAGODA, Direction.SOUTH,
						tier == 0 ? Interior.HALL : Interior.NONE);
				Kit.hipRoof(c, -half - 2, -10 - half - 2, half + 2, -10 + half + 2, y0 + 4, Blocks.DARK_PRISMARINE_STAIRS,
						Blocks.DARK_PRISMARINE_SLAB, Blocks.DARK_PRISMARINE);
			}
			c.fill(0, y + 22, -10, 0, y + 25, -10, s(Blocks.GOLD_BLOCK));
			// Bridge over the lotus pond with lily pads.
			for (int z = 14; z <= 30; z++) {
				int lift = 3 - Math.abs(z - 22) / 3;
				c.fill(-1, 62 + lift, z, 1, 62 + lift, z, s(Blocks.CHERRY_PLANKS));
				c.set(-2, 63 + lift, z, Kit.connected(Blocks.CHERRY_FENCE, false));
				c.set(2, 63 + lift, z, Kit.connected(Blocks.CHERRY_FENCE, false));
			}
			for (int[] p : new int[][]{{-7, 20}, {6, 24}, {-4, 26}, {8, 19}, {-9, 24}}) {
				c.set(p[0], 63, p[1], s(Blocks.LILY_PAD));
			}
			for (int[] t : new int[][]{{-30, -10}, {30, -4}, {-26, 6}, {26, 14}, {-16, 34}, {18, 34}, {-34, 24}, {36, -20}, {-30, -28}}) {
				Kit.roundTree(c, t[0], t[1], 6, Blocks.CHERRY_LOG, Blocks.CHERRY_LEAVES);
			}
			Kit.tent(c, -28, 18, c.ground(-28, 18), Blocks.PURPLE_WOOL);
			Kit.pier(c, 28, 38, Direction.SOUTH, 14, 1);
			treasure(c);
		}
	}

	// ==================================================================================================== Lv 625

	/** Crystal temple: three tiers of quartz basins around a crystal spire, ringed by six glowing crystal spikes. */
	static final class CrystalFountain extends BasePlan {
		CrystalFountain() {
			super("Crystal Fountain", 0.06);
			terrain.mass(0, 0, 42, 40, 4, 2.6, Ground.CALCITE)
					.flat(0, 0, 22, 22, 66, 4, s(Blocks.QUARTZ_BRICKS))
					.path(s(Blocks.QUARTZ_BRICKS), 3, 0, 22, 0, 40);
			anchor(Anchor.of(Type.QUEST, 3, 28));
			anchor(Anchor.of(Type.FISHERMAN, 0, -54));
			anchor(Anchor.at(Type.DOCK, 4, 63, -56));
			anchor(Anchor.area(Type.ENEMY_CAMP, 28, 28, 7));
			anchor(Anchor.area(Type.ENEMY_CAMP, -28, 28, 7));
			anchor(Anchor.at(Type.BOSS_ARENA, 0, 70, 14));
			anchor(Anchor.of(Type.TREASURE, -30, -30));
		}

		@Override
		public void build(Canvas c) {
			int[] radii = {17, 11, 6};
			for (int tier = 0; tier < 3; tier++) {
				int r = radii[tier];
				int y = 66 + tier * 4;
				for (int dx = -r; dx <= r; dx++) {
					for (int dz = -r; dz <= r; dz++) {
						double d = Math.sqrt(dx * dx + dz * dz);
						if (d > r + 0.4) {
							continue;
						}
						c.fill(dx, 66, dz, dx, y, dz, s(Blocks.QUARTZ_BLOCK));
						if (d > r - 2.5 && d <= r - 0.6) {
							c.set(dx, y, dz, s(Blocks.WATER)); // basin ring
						} else if (d > r - 0.6) {
							c.set(dx, y + 1, dz, Kit.slab(Blocks.QUARTZ_SLAB, false));
						}
					}
				}
				for (int i = 0; i < 8; i++) {
					double a = i * Math.PI / 4;
					int px = (int) Math.round(Math.cos(a) * (r - 3.5));
					int pz = (int) Math.round(Math.sin(a) * (r - 3.5));
					if (tier < 2) {
						c.fill(px, y + 1, pz, px, y + 3, pz, s(Blocks.QUARTZ_PILLAR));
						c.set(px, y + 4, pz, s(Blocks.SEA_LANTERN));
					}
				}
			}
			// Central crystal spire.
			for (int yy = 79; yy <= 92; yy++) {
				int w = yy < 84 ? 1 : 0;
				c.fill(-w, yy, -w, w, yy, w, s(yy % 3 == 0 ? Blocks.GLASS : Blocks.AMETHYST_BLOCK));
			}
			c.set(0, 93, 0, s(Blocks.END_ROD));
			// Six crystal spikes around the plaza.
			for (int i = 0; i < 6; i++) {
				double a = i * Math.PI / 3 + 0.3;
				int x = (int) Math.round(Math.cos(a) * 26);
				int z = (int) Math.round(Math.sin(a) * 26);
				int g = c.ground(x, z);
				for (int h = 1; h <= 8; h++) {
					c.set(x, g + h, z, s(h > 6 ? Blocks.PRISMARINE_BRICKS : Blocks.AMETHYST_BLOCK));
				}
				c.set(x, g + 9, z, s(Blocks.SEA_LANTERN));
				c.set(x + 1, g + 1, z, s(Blocks.AMETHYST_CLUSTER));
			}
			Kit.tent(c, 28, 28, c.ground(28, 28), Blocks.LIGHT_BLUE_WOOL);
			Kit.tent(c, -28, 28, c.ground(-28, 28), Blocks.LIGHT_BLUE_WOOL);
			Kit.pier(c, 0, -40, Direction.NORTH, 14, 1);
			treasure(c);
		}
	}
}
