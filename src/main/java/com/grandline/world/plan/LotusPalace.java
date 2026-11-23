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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Lotus Palace (layout 17), built to the owner's brief (docs/LOTUS_PALACE_BLUEPRINT.md): three tropical islets round
 * a whirlpool at the surface, and far below, north of them, a sunken royal palace in a dry air pocket held back by an
 * invisible barrier (no glass dome), reached through the linked whirlpools (see com.grandline.world.LotusWhirlpools).
 * Island-local coordinates from the surface whirlpool: +x east, +z south. The palace has its own origin P (palace
 * code runs on a canvas shifted to it); its garden level is y {@link #G}.
 */
public class LotusPalace extends BasePlan {
	static final int SEA = 62;
	/** The palace origin (island-local z) and its garden level. */
	public static final int PZ = -120, G = 9;
	/** Foundation terrace / hall floor, hall wall top, the roof terrace. */
	static final int FLOOR = G + 2, WALL_TOP = G + 13, ROOF = G + 14, TERRACE = G + 18;
	/** The air pocket (palace-local oval) and its invisible ceiling. */
	static final double POCKET_CZ = -6, POCKET_RX = 86, POCKET_RZ = 78;
	static final int CEILING = 55, RIM_ROCK = 44;
	/**
	 * Water that nothing can root in: an invisible, unlit light block holding water. Laid over the invisible ceiling and
	 * the whirlpool's footing, so the sea's own kelp and seagrass (placed after the island) cannot grow on them.
	 */
	static final BlockState SEALED_WATER = Blocks.LIGHT.defaultBlockState().setValue(net.minecraft.world.level.block.LightBlock.LEVEL, 0)
			.setValue(BlockStateProperties.WATERLOGGED, true);
	/** Whirlpools: the surface one (island-local) and the palace one (palace-local), and their trigger radius. */
	public static final int[] SURFACE_WHIRL = {0, SEA, 0};
	public static final int[] PALACE_WHIRL = {0, G + 1, 60};
	public static final double SURFACE_R = 11.5, PALACE_R = 6, TRIGGER_R = 2.5;
	/** Conch towers (palace-local). */
	static final int[][] TOWERS = {{-58, -10}, {-25, -43}, {25, -43}, {58, -10}};

	/** Layout 18: the cavern overhaul (LotusCavern); layout-17 worlds keep the plan without it (LotusPalaceV17). */
	final boolean overhaul;

	LotusPalace() {
		this(true);
	}

	protected LotusPalace(boolean overhaul) {
		super("Lotus Palace", 0.14);
		this.overhaul = overhaul;
		terrain.mass(0, -30, 14, 11.5, 2, 1.6, Ground.GRASS)
				.mass(-26, 18, 15, 10.5, 2, 1.6, Ground.GRASS)
				.mass(27, 19, 17, 13, 2, 1.6, Ground.GRASS)
				// the palace grounds count as the island's land (discovery, respawns); the plan carves the pocket itself
				.flat(0, PZ + POCKET_CZ, POCKET_RX * 0.88, POCKET_RZ * 0.88, SEA + 1, 0.5, null)
				.reach(232);
		anchor(new Anchor(Type.ARRIVAL, 0, Anchor.SURFACE, -25, 0, 0)); // on the north islet, facing the whirlpool
		anchor(Anchor.at(Type.FISHERMAN, 0, SEA + 1, -12, 0));
		anchor(Anchor.at(Type.DOCK, 3, 63, -14));
		anchor(Anchor.at(Type.QUEST, 7, G + 1, PZ + 34, 0));
		anchor(Anchor.at(Type.HOME, -7, G + 1, PZ + 34, 0));
		anchor(new Anchor(Type.ENEMY_CAMP, -56, G, PZ + 18, 6));
		anchor(new Anchor(Type.ENEMY_CAMP, 56, G, PZ + 18, 6));
		anchor(Anchor.at(Type.BOSS_ARENA, 0, FLOOR + 3, PZ - 13));
	}

	/** The islets only: the palace grounds lie under the open sea, which stays sea (their "land" is only for gameplay). */
	@Override
	public boolean biomeFootprint(int lx, int lz) {
		return lz > -50 && terrain().ground(lx, lz) >= Terrain.WATER_TOP;
	}

	// ======================================================================================================= helpers

	protected static BlockState s(Block b) {
		return b.defaultBlockState();
	}

	static double unit(int x, int z, int salt) {
		return CloudtopIslesV7.unit(x, z, salt);
	}

	static double unit3(int x, int y, int z, int salt) {
		return CloudtopIslesV7.unit(x * 7 + y, z * 3 - y, salt);
	}

	static double angle(double x, double z) {
		double a = Math.atan2(z, x);
		return a < 0 ? a + Math.PI * 2 : a;
	}

	/** Pocket "radius" of a palace-local column: below 1 inside, the rim just past 1. */
	static double pocket(double x, double z) {
		double dz = z - POCKET_CZ;
		double q = Math.sqrt(Math.pow(x / POCKET_RX, 2) + Math.pow(dz / POCKET_RZ, 2));
		double t = Math.atan2(dz, x);
		return q / (1 + 0.05 * Math.sin(3 * t + 0.7) + 0.035 * Math.sin(5 * t + 2));
	}

	/** Octagon-ish clipped rectangle test. */
	static boolean clipped(int x, int z, int hx, int hz, int clip) {
		int ax = Math.abs(x), az = Math.abs(z);
		return ax <= hx && az <= hz && ax + az <= hx + hz - clip;
	}

	static void leaves(Canvas c, int x, int y, int z, Block b) {
		if (!c.inChunk(x, z)) {
			return;
		}
		BlockState there = c.level().getBlockState(c.world(x, y, z));
		if (there.isAir() || there.canBeReplaced() || there.is(Blocks.WATER)) {
			c.set(x, y, z, Kit.leaves(b));
		}
	}

	// ========================================================================================================= build

	@Override
	public void build(Canvas c) {
		whirlpoolSupport(c);
		for (int i = 0; i < ISLETS.length; i++) {
			islet(c, i);
		}
		landing(c);
		Canvas p = c.shifted(0, PZ);
		pocketAndGardens(p);
		courtAndPaths(p);
		pools(p);
		if (!overhaul) {
			rockFormations(p); // the overhaul's designed walls take their place
		}
		terraceAndHall(p);
		hallInterior(p);
		broadRoof(p);
		roofGarden(p);
		nautilusDome(p);
		rearWall(p);
		for (int[] t : TOWERS) {
			conchTower(p, t[0], t[1]);
		}
		wing(p, -1);
		wing(p, 1);
		waveArches(p);
		shellHouses(p);
		corals(p);
		jellyfish(p);
		pathLights(p);
		// Accessibility: sandstone half-steps up the north islet from its landing toward the whirlpool (no jumping).
		Kit.stepsUp(c, 14, -33, 14, -27, Direction.WEST, Blocks.SANDSTONE_STAIRS);
		Kit.stepsUp(c, 11, -31, 11, -25, Direction.WEST, Blocks.SANDSTONE_STAIRS);
	}

	/**
	 * After the world's ocean decoration: the sea's own plants grow on top of the pocket's invisible ceiling and on the
	 * whirlpool's hidden footing, where they would show as dark specks; put plain water back there.
	 */
	@Override
	public void afterDecoration(Canvas c) {
		for (int x = -12; x <= 12; x++) {
			for (int z = -12; z <= 12; z++) {
				if (Math.hypot(x, z) <= SURFACE_R && c.inChunk(x, z)) {
					c.set(x, SEA, z, SEALED_WATER);
				}
			}
		}
		Canvas p = c.shifted(0, PZ);
		for (int x = -96; x <= 96; x++) {
			for (int z = -92; z <= 84; z++) {
				if (p.inChunk(x, z) && pocket(x, z) < 1.06) {
					p.set(x, CEILING + 1, z, SEALED_WATER);
					p.fill(x, CEILING + 2, z, x, SEA, z, s(Blocks.WATER));
				}
			}
		}
		if (overhaul) {
			// after the world's ores, so none show in the designed walls
			LotusCavern.walls(p);
			LotusCavern.galleryStairs(p);
			LotusCavern.grandColumns(p);
			LotusCavern.rimLife(p);
			LotusCavern.lights(p);
		}
	}

	// ======================================================================================================= surface

	/** Islets: centre x, z, half sizes x, z (as in the terrain masses), seed. */
	static final int[][] ISLETS = {{0, -30, 14, 11, 1}, {-26, 18, 15, 10, 2}, {27, 19, 17, 13, 3}};

	/**
	 * The hidden footing under the surface whirlpool: a disc of barrier just under the water, so players stand in the
	 * whirlpool instead of sinking (the barrier also keeps boats and swimmers from diving through it).
	 */
	private void whirlpoolSupport(Canvas c) {
		if (!c.near(0, 0, 13)) {
			return;
		}
		for (int x = -12; x <= 12; x++) {
			for (int z = -12; z <= 12; z++) {
				if (Math.hypot(x, z) <= SURFACE_R) {
					c.set(x, SEA - 1, z, s(Blocks.BARRIER));
					c.set(x, SEA, z, SEALED_WATER);
				}
			}
		}
	}

	/**
	 * One islet: boulders clustered on its outer side (sandy openings toward the whirlpool), about three palms (some
	 * leaning), a couple of broadleaf trees and shrubs behind them, and its foundation carried down to the seabed.
	 */
	private void islet(Canvas c, int index) {
		int[] is = ISLETS[index];
		int cx = is[0], cz = is[1], rx = is[2], rz = is[3], seed = is[4];
		if (!c.near(cx, cz, Math.max(rx, rz) + 14)) {
			return;
		}
		// foundation: rock under the land down to the seabed
		for (int x = cx - rx - 6; x <= cx + rx + 6; x++) {
			for (int z = cz - rz - 6; z <= cz + rz + 6; z++) {
				if (!c.inChunk(x, z)) {
					continue;
				}
				int g = c.ground(x, z);
				if (g >= SEA) {
					c.fill(x, 30, z, x, g - 3, z, s(Blocks.STONE));
				}
			}
		}
		// the outward direction (away from the whirlpool)
		double ox = cx, oz = cz, ol = Math.hypot(ox, oz);
		ox /= ol;
		oz /= ol;
		// boulders: 3-4 clusters on the outer half; the biggest 5-9 high
		int[][] rocks = {{0, 0}, {-1, 1}, {1, 1}, {0, 2}};
		for (int k = 0; k < rocks.length; k++) {
			double side = (k - 1.5) * 0.55;
			double bx = cx + ox * rx * 0.55 - oz * rx * side, bz = cz + oz * rz * 0.55 + ox * rz * side;
			int h = k == 0 ? 7 + seed % 3 : 3 + (k + seed) % 3;
			double r = k == 0 ? 3.6 : 2.3;
			boulder(c, (int) Math.round(bx), (int) Math.round(bz), r, h, seed * 7 + k);
		}
		// palms between the rocks and the shore, leaning toward the water
		for (int k = 0; k < 3; k++) {
			double a = Math.atan2(-oz, -ox) + (k - 1) * 0.9;
			int px = (int) Math.round(cx + Math.cos(a) * rx * 0.35 + ox * 2), pz = (int) Math.round(cz + Math.sin(a) * rz * 0.35 + oz * 2);
			int g = c.ground(px, pz);
			if (g > SEA) {
				palm(c, px, g, pz, 8 + (seed + k) % 5, k == 1 ? 0 : 2 + k % 2, Math.cos(a), Math.sin(a), seed * 5 + k);
			}
		}
		// broadleaf trees and shrubs behind the palms
		for (int k = 0; k < 2; k++) {
			double side = k == 0 ? -0.6 : 0.6;
			int tx = (int) Math.round(cx + ox * rx * 0.25 - oz * rx * side), tz = (int) Math.round(cz + oz * rz * 0.25 + ox * rz * side);
			int g = c.ground(tx, tz);
			if (g > SEA) {
				broadleaf(c, tx, g, tz, seed * 3 + k);
			}
		}
		for (int k = 0; k < 4; k++) {
			double a = k * 1.6 + seed;
			int sx = (int) Math.round(cx + Math.cos(a) * rx * 0.55), sz = (int) Math.round(cz + Math.sin(a) * rz * 0.55);
			int g = c.ground(sx, sz);
			if (g > SEA && c.near(sx, sz, 2)) {
				leaves(c, sx, g + 1, sz, Blocks.JUNGLE_LEAVES);
				leaves(c, sx + 1, g + 1, sz, Blocks.JUNGLE_LEAVES);
				leaves(c, sx, g + 1, sz + 1, Blocks.JUNGLE_LEAVES);
				leaves(c, sx, g + 2, sz, Blocks.JUNGLE_LEAVES);
			}
		}
	}

	/** An irregular boulder: a lumpy ellipsoid of andesite, stone and tuff rising {@code h} above the ground. */
	static void boulder(Canvas c, int cx, int cz, double r, int h, int seed) {
		if (!c.near(cx, cz, (int) r + 2)) {
			return;
		}
		int base = Math.max(SEA - 2, c.ground(cx, cz) - 1);
		for (int x = cx - (int) r - 1; x <= cx + r + 1; x++) {
			for (int z = cz - (int) r - 1; z <= cz + r + 1; z++) {
				for (int y = base - 3; y <= base + h; y++) {
					double dy = (y - base) / (double) h;
					double rr = r * (1.1 - dy * 0.7) * (0.85 + 0.3 * unit(x * 3 + y, z, seed));
					if (Math.hypot(x - cx, z - cz) <= rr) {
						double u = unit3(x, y, z, seed);
						c.set(x, y, z, u < 0.45 ? s(Blocks.ANDESITE) : u < 0.8 ? s(Blocks.STONE) : s(Blocks.TUFF));
					}
				}
			}
		}
	}

	/**
	 * A palm: a stripped birch trunk {@code h} tall that steps sideways {@code lean} times toward (dx, dz), and seven
	 * fronds of jungle leaves radiating 4-6 blocks from the crown with drooping tips.
	 */
	static void palm(Canvas c, int x, int g, int z, int h, int lean, double dx, double dz, int seed) {
		if (!c.near(x, z, 10)) {
			return;
		}
		int sx = (int) Math.signum(Math.round(dx * 2)), sz = (int) Math.signum(Math.round(dz * 2));
		int px = x, pz = z;
		for (int k = 1; k <= h; k++) {
			if (lean > 0 && k > h / 3 && (k - h / 3) % Math.max(1, (h - h / 3) / lean) == 0) {
				px += sx;
				pz += sz;
			}
			c.set(px, g + k, pz, s(Blocks.STRIPPED_BIRCH_LOG));
		}
		int top = g + h;
		leaves(c, px, top + 1, pz, Blocks.JUNGLE_LEAVES);
		for (int f = 0; f < 7; f++) {
			double a = f * Math.PI * 2 / 7 + seed;
			int len = 4 + (f + seed) % 3;
			for (int k = 1; k <= len; k++) {
				int fx = px + (int) Math.round(Math.cos(a) * k), fz = pz + (int) Math.round(Math.sin(a) * k);
				int fy = top + (k <= 2 ? 1 : k <= len - 1 ? 0 : -1);
				leaves(c, fx, fy, fz, Blocks.JUNGLE_LEAVES);
				if (k == len) {
					leaves(c, fx, fy - 1, fz, Blocks.JUNGLE_LEAVES);
				}
			}
		}
	}

	/** A short broadleaf jungle tree: a 4-5 high trunk and a rounded, wider-than-tall crown. */
	static void broadleaf(Canvas c, int x, int g, int z, int seed) {
		if (!c.near(x, z, 5)) {
			return;
		}
		int h = 4 + seed % 2;
		c.fill(x, g + 1, z, x, g + h, z, s(Blocks.JUNGLE_LOG));
		for (int dx = -3; dx <= 3; dx++) {
			for (int dz = -3; dz <= 3; dz++) {
				double r = Math.hypot(dx, dz);
				if (r <= 3.2) {
					leaves(c, x + dx, g + h, z + dz, Blocks.JUNGLE_LEAVES);
				}
				if (r <= 2.2) {
					leaves(c, x + dx, g + h + 1, z + dz, Blocks.JUNGLE_LEAVES);
				}
				if (r <= 1.2) {
					leaves(c, x + dx, g + h + 2, z + dz, Blocks.JUNGLE_LEAVES);
				}
			}
		}
	}

	/** The north landing: 9 long, 3 wide, a dark oak deck just above the sea on log posts, toward the whirlpool. */
	private void landing(Canvas c) {
		if (!c.near(0, -15, 8)) {
			return;
		}
		for (int z = -19; z <= -11; z++) {
			for (int x = -1; x <= 1; x++) {
				if (c.ground(x, z) <= SEA) {
					c.set(x, SEA, z, Kit.slab(Blocks.DARK_OAK_SLAB, true));
					c.fill(x, SEA + 1, z, x, SEA + 3, z, s(Blocks.AIR));
				}
			}
		}
		for (int[] p : new int[][]{{-1, -11}, {1, -11}, {-1, -15}, {1, -15}}) {
			c.fill(p[0], 40, p[1], p[0], SEA - 1, p[1], s(Blocks.DARK_OAK_LOG));
			c.set(p[0], SEA + 1, p[1], s(Blocks.DARK_OAK_FENCE));
		}
		c.set(0, SEA, -11, s(Blocks.DARK_OAK_PLANKS));
	}

	// ============================================================================================== pocket, gardens

	/** Garden height of a palace-local column inside the pocket (G inside, rising 1-3 toward the rock walls). */
	static int gardenY(int x, int z) {
		double e = pocket(x, z);
		if (e < 0.8) {
			return G;
		}
		return G + (int) Math.round(Math.min(3, (e - 0.8) / 0.2 * 3 + (unit(x / 4, z / 4, 3) - 0.5)));
	}

	/** The seabed's top block: clay with soft patches of light grey and cyan terracotta, scattered blue and violet. */
	static BlockState seabed(int x, int z) {
		double f = Math.sin(x * 0.11) + Math.sin(z * 0.13 + 1) + Math.sin((x + z) * 0.07);
		double g = Math.sin(x * 0.23 + 2) + Math.sin(z * 0.19 - 1);
		if (g > 1.62) {
			return s(Blocks.BLUE_TERRACOTTA);
		}
		if (g < -1.7) {
			return s(Blocks.PURPLE_TERRACOTTA);
		}
		return f > 1.3 ? s(Blocks.CYAN_TERRACOTTA) : f < -1.3 ? s(Blocks.LIGHT_GRAY_TERRACOTTA) : s(Blocks.CLAY);
	}

	/**
	 * The pocket: inside, rock up to the garden, air up to the invisible ceiling, the sea above it; on the rim, a rock
	 * wall up to the old seabed and an invisible barrier wall from there to the ceiling (the sea stays outside).
	 */
	private void pocketAndGardens(Canvas p) {
		for (int x = -96; x <= 96; x++) {
			for (int z = -92; z <= 84; z++) {
				if (!p.inChunk(x, z)) {
					continue;
				}
				double e = pocket(x, z);
				if (e >= 1.06) {
					continue;
				}
				if (e < 1) {
					int gy = gardenY(x, z);
					p.fill(x, G - 10, z, x, gy - 2, z, s(Blocks.STONE));
					p.set(x, gy - 1, z, s(Blocks.CLAY));
					p.set(x, gy, z, seabed(x, z));
					p.fill(x, gy + 1, z, x, CEILING - 1, z, s(Blocks.AIR));
					if (Math.floorMod(x, 9) == 4 && Math.floorMod(z, 9) == 4) {
						// invisible light, so the grounds are not gloomy under the sea
						p.set(x, gy + 7, z, s(Blocks.LIGHT).setValue(net.minecraft.world.level.block.LightBlock.LEVEL, overhaul ? 15 : 13));
					}
				} else {
					for (int y = G - 10; y <= RIM_ROCK; y++) {
						double u = unit3(x, y, z, 11);
						p.set(x, y, z, u < 0.4 ? s(Blocks.TUFF) : u < 0.75 ? s(Blocks.ANDESITE) : s(Blocks.STONE));
					}
					p.fill(x, RIM_ROCK + 1, z, x, CEILING - 1, z, s(Blocks.BARRIER));
				}
				p.set(x, CEILING, z, s(Blocks.BARRIER));
				p.set(x, CEILING + 1, z, SEALED_WATER);
				p.fill(x, CEILING + 2, z, x, SEA, z, s(Blocks.WATER));
				p.fill(x, SEA + 1, z, x, SEA + 26, z, s(Blocks.AIR));
			}
		}
	}

	/**
	 * The arrival court (round, with the whirlpool's floor medallion), the straight approach to the palace steps, and
	 * the pale winding paths through the front garden to the houses.
	 */
	private void courtAndPaths(Canvas p) {
		// winding paths (3 wide, 5 at junctions): sand down the middle, sandstone edges
		int[][][] paths = {
				{{0, 44}, {-12, 46}, {-22, 52}, {-30, 58}}, {{-22, 52}, {-40, 57}, {-60, 52}}, {{-60, 52}, {-66, 30}, {-72, 6}, {-70, -16}, {-64, -34}},
				{{0, 44}, {12, 46}, {22, 52}, {30, 58}}, {{22, 52}, {40, 57}, {60, 52}}, {{60, 52}, {66, 30}, {72, 6}, {70, -16}, {64, -34}},
				{{-70, -16}, {-56, 18}}, {{70, -16}, {56, 18}}};
		for (int[][] path : paths) {
			for (int i = 0; i + 1 < path.length; i++) {
				line(p, path[i][0], path[i][1], path[i + 1][0], path[i + 1][1], 1.6, overhaul);
			}
			for (int[] j : path) {
				disc(p, j[0], j[1], 2.6, overhaul);
			}
		}
		// the straight approach, 5 wide, from the court to the steps
		for (int z = 27; z <= 50; z++) {
			for (int x = -2; x <= 2; x++) {
				if (p.inChunk(x, z)) {
					BlockState approach = Math.abs(x) == 2 ? s(Blocks.SANDSTONE) : s(Blocks.SMOOTH_SANDSTONE);
					if (overhaul) {
						// smooth quartz with prismarine bands every 4, polished diorite edges lit every 6
						approach = Math.abs(x) == 2 ? (Math.floorMod(z, 6) == 0 ? s(Blocks.SEA_LANTERN) : s(Blocks.POLISHED_DIORITE))
								: Math.floorMod(z, 4) == 0 ? s(Blocks.PRISMARINE_BRICKS) : x == 0 ? s(Blocks.CYAN_TERRACOTTA) : s(Blocks.SMOOTH_QUARTZ);
					}
					p.set(x, G, z, approach);
				}
			}
		}
		// the arrival court: a round pale floor 27 across with the whirlpool medallion (12 across) in the middle
		int cx = PALACE_WHIRL[0], cz = PALACE_WHIRL[2];
		for (int x = cx - 14; x <= cx + 14; x++) {
			for (int z = cz - 14; z <= cz + 14; z++) {
				double d = Math.hypot(x - cx, z - cz);
				if (d > 13.5 || !p.inChunk(x, z)) {
					continue;
				}
				BlockState b;
				if (d <= 1.5) {
					b = s(Blocks.BLUE_TERRACOTTA);
				} else if (d <= PALACE_R) {
					double a = angle(x - cx, z - cz);
					double arm = ((a / (Math.PI * 2)) * 3 + d / 4.0) % 1.0;
					b = arm < 0.33 ? s(Blocks.CYAN_TERRACOTTA) : arm < 0.66 ? s(Blocks.LIGHT_BLUE_CONCRETE) : s(Blocks.BLUE_CONCRETE);
				} else if (d <= PALACE_R + 1) {
					b = s(Blocks.WHITE_CONCRETE);
				} else if (d > 12.5) {
					b = s(Blocks.SANDSTONE);
				} else {
					b = s(Blocks.SMOOTH_SANDSTONE);
				}
				p.set(x, G, z, b);
				p.fill(x, G + 1, z, x, G + 6, z, s(Blocks.AIR));
			}
		}
		if (overhaul) {
			LotusCavern.whirlpoolPlaza(p, cx, cz, (int) PALACE_R);
			LotusCavern.plazas(p);
		}
	}

	static void line(Canvas p, int x1, int z1, int x2, int z2, double half, boolean fancy) {
		int n = Math.max(1, (int) Math.ceil(Math.hypot(x2 - x1, z2 - z1) * 2));
		for (int i = 0; i <= n; i++) {
			double t = i / (double) n;
			disc(p, x1 + (x2 - x1) * t, z1 + (z2 - z1) * t, half, fancy);
		}
	}

	static void disc(Canvas p, double cx, double cz, double r, boolean fancy) {
		for (int x = (int) Math.floor(cx - r); x <= Math.ceil(cx + r); x++) {
			for (int z = (int) Math.floor(cz - r); z <= Math.ceil(cz + r); z++) {
				double d = Math.hypot(x - cx, z - cz);
				if (d > r || !p.inChunk(x, z) || pocket(x, z) > 0.97) {
					continue;
				}
				int gy = gardenY(x, z);
				p.set(x, gy, z, fancy ? LotusCavern.path(x, z, d > r - 0.8)
						: d > r - 0.8 ? s(Blocks.SANDSTONE) : unit(x, z, 21) < 0.8 ? s(Blocks.SAND) : s(Blocks.SANDSTONE));
			}
		}
	}

	/**
	 * Shallow coral pools in the gardens (the palace grounds are dry): one block of water over sand, with living coral,
	 * fans and seagrass in it, rimmed with sandstone.
	 */
	private void pools(Canvas p) {
		int[][] pools = overhaul
				? new int[][]{{-40, 34, 4}, {40, 34, 4}, {-46, -22, 3}, {46, -22, 3}, {-20, 66, 3}, {20, 66, 3}, {-62, -46, 3}, {62, -46, 3}}
				: new int[][]{{-40, 34, 4}, {40, 34, 4}, {-46, -22, 3}, {46, -22, 3}, {-20, 66, 3}, {20, 66, 3}, {-76, -40, 3}, {76, -40, 3}};
		Block[] coral = {Blocks.TUBE_CORAL, Blocks.BRAIN_CORAL, Blocks.BUBBLE_CORAL, Blocks.FIRE_CORAL, Blocks.HORN_CORAL};
		Block[] fans = {Blocks.TUBE_CORAL_FAN, Blocks.BRAIN_CORAL_FAN, Blocks.BUBBLE_CORAL_FAN, Blocks.FIRE_CORAL_FAN, Blocks.HORN_CORAL_FAN};
		for (int[] pool : pools) {
			if (!p.near(pool[0], pool[1], pool[2] + 2)) {
				continue;
			}
			for (int x = pool[0] - pool[2] - 1; x <= pool[0] + pool[2] + 1; x++) {
				for (int z = pool[1] - pool[2] - 1; z <= pool[1] + pool[2] + 1; z++) {
					double d = Math.hypot(x - pool[0], z - pool[1]);
					int gy = gardenY(x, z);
					if (d <= pool[2]) {
						p.set(x, gy - 1, z, s(Blocks.SAND));
						double u = unit(x, z, pool[0]);
						BlockState b = u < 0.22 ? s(coral[(int) (u * 100) % 5])
								: u < 0.4 ? s(fans[(int) (u * 100) % 5])
								: u < 0.6 ? s(Blocks.SEAGRASS) : s(Blocks.WATER);
						if (b.hasProperty(BlockStateProperties.WATERLOGGED)) {
							b = b.setValue(BlockStateProperties.WATERLOGGED, true);
						}
						p.set(x, gy, z, b);
						if (b.is(Blocks.SEAGRASS) || b.is(Blocks.WATER) || b.hasProperty(BlockStateProperties.WATERLOGGED)) {
							continue;
						}
					} else if (d <= pool[2] + 1) {
						p.set(x, gy, z, s(Blocks.SANDSTONE));
					}
				}
			}
		}
	}

	/** Enormous crooked rock arches and pointed spires round the edge of the grounds, framing the complex. */
	private void rockFormations(Canvas p) {
		int[][] spires = {{-80, -40, 26}, {-84, 10, 20}, {-70, 58, 18}, {80, -42, 24}, {84, 12, 22}, {70, 58, 20}, {-40, -76, 28}, {40, -76, 30}, {0, -82, 22}};
		for (int[] s : spires) {
			spire(p, s[0], s[1], s[2], s[0] * 3 + s[1]);
		}
		int[][] arches = {{-86, -18, -66, -56, 26}, {86, -18, 66, -56, 24}, {-30, -82, 14, -84, 30}};
		for (int[] a : arches) {
			rockArch(p, a[0], a[1], a[2], a[3], a[4]);
		}
	}

	static void spire(Canvas p, int cx, int cz, int h, int seed) {
		if (!p.near(cx, cz, 7)) {
			return;
		}
		int g = gardenY(cx, cz);
		double lx = (unit(seed, 1, 5) - 0.5) * 0.25, lz = (unit(seed, 2, 5) - 0.5) * 0.25;
		for (int y = g - 2; y <= g + h; y++) {
			double t = (y - g) / (double) h;
			double r = 4.6 * (1 - t) + 0.4;
			double ccx = cx + lx * (y - g), ccz = cz + lz * (y - g);
			for (int x = (int) (ccx - r - 1); x <= ccx + r + 1; x++) {
				for (int z = (int) (ccz - r - 1); z <= ccz + r + 1; z++) {
					if (Math.hypot(x - ccx, z - ccz) <= r * (0.88 + 0.24 * unit3(x, y / 3, z, seed))) {
						double u = unit3(x, y, z, seed + 1);
						p.set(x, y, z, u < 0.45 ? s(Blocks.TUFF) : u < 0.8 ? s(Blocks.ANDESITE) : s(Blocks.STONE));
					}
				}
			}
		}
	}

	/** A crooked rock arch: a thick, lumpy tube of rock along a bent curve from (x1, z1) to (x2, z2), rising {@code rise}. */
	static void rockArch(Canvas p, int x1, int z1, int x2, int z2, int rise) {
		int mx = (x1 + x2) / 2, mz = (z1 + z2) / 2;
		if (!p.near(mx, mz, (int) Math.hypot(x2 - x1, z2 - z1) / 2 + 6)) {
			return;
		}
		int n = (int) (Math.hypot(x2 - x1, z2 - z1) * 3);
		for (int i = 0; i <= n; i++) {
			double t = i / (double) n;
			double x = x1 + (x2 - x1) * t + Math.sin(t * Math.PI * 2) * 3;
			double z = z1 + (z2 - z1) * t;
			double y = gardenY((int) x, (int) z) + Math.sin(Math.PI * t) * rise;
			double r = 3.2 - Math.sin(Math.PI * t) * 1.2 + unit(i / 6, 0, 4) * 0.8;
			for (int bx = (int) (x - r); bx <= x + r; bx++) {
				for (int bz = (int) (z - r); bz <= z + r; bz++) {
					for (int by = (int) (y - r); by <= y + r; by++) {
						if (Math.sqrt((bx - x) * (bx - x) + (bz - z) * (bz - z) + (by - y) * (by - y)) <= r) {
							double u = unit3(bx, by, bz, 33);
							p.set(bx, by, bz, u < 0.4 ? s(Blocks.TUFF) : u < 0.75 ? s(Blocks.ANDESITE) : s(Blocks.STONE));
						}
					}
				}
			}
		}
	}

	// ============================================================================================= the central hall

	/** Hall walls: 57 x 39 with clipped corners; bays every 6 (pale columns), a gold band, the south entrance. */
	static final int HX = 28, HZ = 19, CLIP = 3;

	private void terraceAndHall(Canvas p) {
		if (!p.near(0, 0, 40)) {
			return;
		}
		// the foundation terrace: 67 x 49, two above the garden, tuff and andesite edges
		for (int x = -34; x <= 34; x++) {
			for (int z = -25; z <= 25; z++) {
				if (!p.inChunk(x, z) || !clipped(x, z, 33, 24, 6)) {
					continue;
				}
				boolean edge = !clipped(x, z, 32, 23, 6);
				p.fill(x, G - 2, z, x, FLOOR - 1, z, s(Blocks.STONE));
				p.set(x, FLOOR, z, edge ? s(Blocks.TUFF) : (Math.floorMod(x + z, 2) == 0 ? s(Blocks.SMOOTH_SANDSTONE) : s(Blocks.SANDSTONE)));
				p.fill(x, FLOOR + 1, z, x, FLOOR + 4, z, s(Blocks.AIR));
			}
		}
		// steps up to the entrance
		for (int x = -4; x <= 4; x++) {
			p.set(x, G + 1, 26, Kit.stairs(Blocks.SMOOTH_QUARTZ_STAIRS, Direction.NORTH, false));
			p.set(x, G + 1, 25, s(Blocks.SMOOTH_QUARTZ));
			p.set(x, FLOOR, 25, Kit.stairs(Blocks.SMOOTH_QUARTZ_STAIRS, Direction.NORTH, false));
		}
		// walls
		for (int x = -HX; x <= HX; x++) {
			for (int z = -HZ; z <= HZ; z++) {
				if (!p.inChunk(x, z) || !clipped(x, z, HX, HZ, CLIP)) {
					continue;
				}
				boolean shell = !clipped(x, z, HX - 1, HZ - 1, CLIP);
				if (!shell) {
					continue;
				}
				boolean south = z == HZ, north = z == -HZ, side = Math.abs(x) == HX;
				int along = south || north ? x : side ? z : 999; // straight faces only
				for (int y = FLOOR + 1; y <= WALL_TOP; y++) {
					p.set(x, y, z, hallWall(along, y, south));
				}
			}
		}
		// pale columns standing proud of the faces every 6
		for (int k = -3; k <= 3; k++) {
			for (int sign : new int[]{-1, 1}) {
				int cx = sign * (3 + 6 * Math.abs(k));
				if (Math.abs(cx) < HX - CLIP) {
					column(p, cx, HZ + 1);
					column(p, cx, -HZ - 1);
				}
			}
		}
		for (int cz = -15; cz <= 15; cz += 6) {
			column(p, HX + 1, cz);
			column(p, -HX - 1, cz);
		}
		// the entrance: 5 wide, 8 high, a stepped rounded arch in a gold frame
		for (int x = -4; x <= 4; x++) {
			int ax = Math.abs(x);
			for (int y = FLOOR + 1; y <= FLOOR + 10; y++) {
				int k = y - FLOOR;
				int open = ax <= 1 ? 8 : ax == 2 ? 7 : 0;
				int frame = ax <= 1 ? 9 : ax == 2 ? 8 : ax == 3 ? 7 : 0;
				if (k <= open) {
					p.set(x, y, HZ, s(Blocks.AIR));
					p.set(x, y, HZ - 1, s(Blocks.AIR));
				} else if (k <= frame || ax == 3 && k <= 7 || ax == 4 && k <= 6 && k >= 1) {
					p.set(x, y, HZ, (k + ax) % 2 == 0 ? s(Blocks.YELLOW_TERRACOTTA) : s(Blocks.CUT_SANDSTONE));
				}
			}
		}
	}

	/**
	 * A hall wall block at {@code along} (position along a straight face, 999 on the clipped corners) and height y:
	 * columns every 6 (pale), and between them a bay: three dark recesses low down, a teal window with a stepped arch,
	 * a narrow gold border and a four-point pattern; a gold band near the top and a pale cornice.
	 */
	static BlockState hallWall(int along, int y, boolean south) {
		int k = y - FLOOR; // 1..13
		if (k == 11) {
			return s(Blocks.YELLOW_TERRACOTTA);
		}
		if (k >= 12) {
			return s(Blocks.WHITE_CONCRETE);
		}
		if (along == 999) {
			return s(Blocks.LIGHT_BLUE_CONCRETE);
		}
		int m = Math.floorMod(along - 3, 6); // 0 = column
		if (m == 0) {
			return s(Blocks.QUARTZ_PILLAR);
		}
		if (south && Math.abs(along) <= 2) {
			return s(Blocks.LIGHT_BLUE_CONCRETE); // the entrance (cut later)
		}
		int b = m - 1; // 0..4 across the bay
		if (k <= 2) {
			return b % 2 == 0 ? s(Blocks.DARK_PRISMARINE) : s(Blocks.LIGHT_BLUE_CONCRETE);
		}
		boolean window = b >= 1 && b <= 3 && k >= 4 && k <= 8 || b == 2 && k == 9;
		if (window) {
			boolean pattern = k == 6 && (b == 1 || b == 3) || b == 2 && (k == 5 || k == 7);
			return pattern ? s(Blocks.BLUE_CONCRETE) : s(Blocks.CYAN_STAINED_GLASS);
		}
		boolean border = (b == 0 || b == 4) && k >= 3 && k <= 8 || k == 3 && b >= 1 && b <= 3 || k == 9 && (b == 1 || b == 3) || k == 10 && b == 2;
		return border ? s(Blocks.YELLOW_TERRACOTTA) : s(Blocks.LIGHT_BLUE_CONCRETE);
	}

	static void column(Canvas p, int x, int z) {
		if (!p.inChunk(x, z)) {
			return;
		}
		p.set(x, FLOOR + 1, z, s(Blocks.SMOOTH_QUARTZ));
		p.fill(x, FLOOR + 2, z, x, WALL_TOP - 2, z, s(Blocks.QUARTZ_PILLAR));
		p.set(x, WALL_TOP - 1, z, s(Blocks.YELLOW_TERRACOTTA));
		p.set(x, WALL_TOP, z, s(Blocks.SMOOTH_QUARTZ));
	}

	/**
	 * The ceremonial hall: 51 x 33, 10 high; four rows of six twisting blue columns, a blue floor with violet inlays,
	 * a black aisle with yellow borders, a panelled ceiling with hidden lights,
	 * green banners and the raised platform at the north end.
	 */
	private void hallInterior(Canvas p) {
		if (!p.near(0, 0, 30)) {
			return;
		}
		int top = FLOOR + 10; // ceiling row
		for (int x = -HX + 1; x <= HX - 1; x++) {
			for (int z = -HZ + 1; z <= HZ - 1; z++) {
				if (!p.inChunk(x, z) || !clipped(x, z, HX - 1, HZ - 1, CLIP)) {
					continue;
				}
				BlockState floor;
				int ax = Math.abs(x);
				double v = Math.sin(x * 0.21 + 1) + Math.sin(z * 0.27) + Math.sin((x - z) * 0.11);
				if (ax <= 2) {
					floor = s(Blocks.BLACK_CONCRETE);
				} else if (ax == 3) {
					floor = s(Blocks.YELLOW_TERRACOTTA);
				} else if (v > 1.25) {
					floor = s(Blocks.PURPLE_CONCRETE);
				} else if (v > 1.0) {
					floor = s(Blocks.PURPLE_TERRACOTTA);
				} else {
					floor = (Math.floorMod(x, 4) == 0 || Math.floorMod(z, 4) == 0) ? s(Blocks.BLUE_TERRACOTTA) : s(Blocks.BLUE_CONCRETE);
				}
				p.set(x, FLOOR, z, floor);
				p.fill(x, FLOOR + 1, z, x, top - 1, z, s(Blocks.AIR));
				BlockState ceil = (Math.floorMod(x, 4) == 0 || Math.floorMod(z, 4) == 0) ? s(Blocks.BROWN_TERRACOTTA)
						: Math.floorMod(x / 4 + z / 4, 2) == 0 ? s(Blocks.YELLOW_TERRACOTTA) : s(Blocks.ORANGE_TERRACOTTA);
				if (Math.floorMod(x, 8) == 2 && Math.floorMod(z, 8) == 2) {
					ceil = s(Blocks.SEA_LANTERN);
				}
				p.set(x, top, z, ceil);
			}
		}
		// the entrance passage stays clear
		p.fill(-2, FLOOR + 1, HZ - 1, 2, FLOOR + 8, HZ, s(Blocks.AIR));
		// four rows of six columns (2 x 2, a stripe of gold twisting round them)
		for (int cx : new int[]{-20, -11, 10, 19}) {
			for (int cz = -15; cz <= 15; cz += 6) {
				for (int dx = 0; dx <= 1; dx++) {
					for (int dz = 0; dz <= 1; dz++) {
						int x = cx + dx, z = cz + dz;
						int corner = dx == 0 ? (dz == 0 ? 0 : 3) : (dz == 0 ? 1 : 2);
						for (int y = FLOOR + 1; y < top; y++) {
							int k = y - FLOOR;
							BlockState b;
							if (k == 1 || k == 9) {
								b = s(Blocks.SMOOTH_QUARTZ);
							} else if (k == 2 || k == 8) {
								b = s(Blocks.YELLOW_TERRACOTTA);
							} else if (Math.floorMod(k, 4) == corner) {
								b = s(Blocks.YELLOW_TERRACOTTA);
							} else {
								b = (k + corner) % 2 == 0 ? s(Blocks.LAPIS_BLOCK) : s(Blocks.BLUE_CONCRETE);
							}
							p.set(x, y, z, b);
						}
					}
				}
			}
		}
		// the far platform: 13 x 7, two shallow steps, burgundy edging, a violet top
		for (int x = -6; x <= 6; x++) {
			for (int z = -16; z <= -10; z++) {
				boolean edge = Math.abs(x) == 6 || z == -10 || z == -16;
				p.set(x, FLOOR + 1, z, edge ? s(Blocks.RED_NETHER_BRICKS) : s(Blocks.PURPLE_CONCRETE));
				if (!edge) {
					boolean edge2 = Math.abs(x) == 5 || z == -11;
					p.set(x, FLOOR + 2, z, edge2 ? s(Blocks.RED_NETHER_BRICKS) : s(Blocks.PURPLE_CONCRETE));
				}
			}
			p.set(x, FLOOR + 1, -9, Kit.slab(Blocks.RED_NETHER_BRICK_SLAB, false));
		}
		// green banners with yellow borders on the inner columns and the far wall
		for (int cz : new int[]{-9, 3, 15}) {
			greenBanner(p, -10, FLOOR + 7, cz + 2, Direction.SOUTH);
			greenBanner(p, 10, FLOOR + 7, cz + 2, Direction.SOUTH);
		}
		for (int x : new int[]{-9, -3, 3, 9}) {
			greenBanner(p, x, FLOOR + 8, -HZ + 1, Direction.SOUTH);
		}
	}

	static void greenBanner(Canvas p, int x, int y, int z, Direction facing) {
		if (!p.inChunk(x, z)) {
			return;
		}
		p.set(x, y, z, s(Blocks.GREEN_WALL_BANNER).setValue(WallBannerBlock.FACING, facing));
		if (p.level().getBlockEntity(p.world(x, y, z)) instanceof BannerBlockEntity banner) {
			ListTag patterns = new ListTag();
			for (Object[] pt : new Object[][]{{"bo", DyeColor.YELLOW}, {"mc", DyeColor.YELLOW}}) {
				CompoundTag t = new CompoundTag();
				t.putString("Pattern", (String) pt[0]);
				t.putInt("Color", ((DyeColor) pt[1]).getId());
				patterns.add(t);
			}
			CompoundTag tag = new CompoundTag();
			tag.put("Patterns", patterns);
			ItemStack stack = new ItemStack(Items.GREEN_BANNER);
			BlockItem.setBlockEntityData(stack, BlockEntityType.BANNER, tag);
			banner.fromItem(stack, DyeColor.GREEN);
			banner.setChanged();
		}
	}

	// ========================================================================================================= roof

	/**
	 * The broad roof: 65 x 47 with clipped corners, a 4-block overhang, red nether brick rising in shallow steps to the
	 * roof terrace, stair and slab eaves with a pale underside, purple edging and corners turned up.
	 */
	private void broadRoof(Canvas p) {
		if (!p.near(0, 0, 36)) {
			return;
		}
		for (int x = -32; x <= 32; x++) {
			for (int z = -23; z <= 23; z++) {
				if (!p.inChunk(x, z) || !clipped(x, z, 32, 23, 7)) {
					continue;
				}
				// steps in from the edge
				int in = 0;
				while (in < 6 && clipped(x, z, 32 - in - 1, 23 - in - 1, 7)) {
					in++;
				}
				int y = ROOF + Math.min(in, 6) / 2;
				if (in == 0) {
					Direction out = Math.abs(x) * 23 >= Math.abs(z) * 32 ? (x > 0 ? Direction.EAST : Direction.WEST) : (z > 0 ? Direction.SOUTH : Direction.NORTH);
					boolean corner = Math.abs(x) + Math.abs(z) >= 32 + 23 - 9;
					p.set(x, corner ? y + 1 : y, z, Kit.stairs(Blocks.RED_NETHER_BRICK_STAIRS, out.getOpposite(), false));
					if (corner) {
						p.set(x, y, z, s(Blocks.RED_NETHER_BRICKS));
					}
					if (!clipped(x, z, HX, HZ, CLIP)) {
						p.set(x, y - 1, z, Kit.slab(Blocks.SMOOTH_QUARTZ_SLAB, true)); // pale underside
					}
				} else if (in == 1) {
					p.set(x, y, z, s(Blocks.PURPLE_TERRACOTTA));
					if (!clipped(x, z, HX, HZ, CLIP)) {
						p.set(x, y - 1, z, Kit.slab(Blocks.SMOOTH_QUARTZ_SLAB, true));
					}
				} else {
					p.fill(x, ROOF, z, x, y, z, s(Blocks.RED_NETHER_BRICKS));
					if (in % 2 == 1 && in < 6) {
						p.set(x, y + 1, z, Kit.slab(Blocks.RED_NETHER_BRICK_SLAB, false));
					}
				}
			}
		}
		// the roof terrace (flat, pale) inside the stepped rim
		for (int x = -26; x <= 26; x++) {
			for (int z = -17; z <= 17; z++) {
				if (p.inChunk(x, z) && clipped(x, z, 26, 17, 7)) {
					p.fill(x, ROOF, z, x, TERRACE - 1, z, s(Blocks.RED_NETHER_BRICKS));
					p.set(x, TERRACE, z, (Math.floorMod(x, 3) == 0 && Math.floorMod(z, 3) == 0) ? s(Blocks.CALCITE) : s(Blocks.WHITE_CONCRETE));
				}
			}
		}
	}

	/**
	 * The roof garden on the southern terrace round the dome: two big purple fan corals, two branching red/brown coral
	 * sculptures, blue sponge formations and the 7 x 7 gold-framed emblem facing the entrance.
	 */
	private void roofGarden(Canvas p) {
		if (!p.near(0, 8, 28)) {
			return;
		}
		int y = TERRACE + 1;
		fanCoral(p, -18, y, 10, 4, Direction.Axis.X);
		fanCoral(p, 18, y, 10, 4, Direction.Axis.X);
		branchCoral(p, -21, y, 2, Blocks.RED_TERRACOTTA, 1);
		branchCoral(p, 21, y, 2, Blocks.BROWN_TERRACOTTA, 2);
		for (int[] s : new int[][]{{-12, 14, 3}, {12, 14, 3}, {-23, -6, 4}, {23, -6, 4}, {-8, 15, 2}, {8, 15, 2}}) {
			sponge(p, s[0], y, s[1], s[2]);
		}
		// the emblem: a 7 x 7 stepped octagon standing at the terrace's south edge, facing south
		String[] emblem = {"..yyy..", ".ygggy.", "yggoggy", "ygoGogy", "yggoggy", ".ygggy.", "..yyy.."};
		for (int r = 0; r < 7; r++) {
			for (int k = 0; k < 7; k++) {
				char ch = emblem[r].charAt(k);
				BlockState b = switch (ch) {
					case 'y' -> s(Blocks.YELLOW_TERRACOTTA);
					case 'g' -> s(Blocks.GREEN_CONCRETE);
					case 'o' -> s(Blocks.YELLOW_TERRACOTTA);
					case 'G' -> s(Blocks.GOLD_BLOCK);
					default -> null;
				};
				if (b != null) {
					p.set(k - 3, y + 6 - r, 16, b);
				}
			}
		}
		p.fill(-3, y, 15, 3, y + 2, 15, s(Blocks.WHITE_CONCRETE)); // the emblem's backing plinth
	}

	/** A big built fan coral: a thick branching stem and a broad flat crown of purple concrete and terracotta. */
	static void fanCoral(Canvas p, int x, int y, int z, int h, Direction.Axis plane) {
		if (!p.near(x, z, 6)) {
			return;
		}
		p.fill(x, y, z, x, y + h - 1, z, s(Blocks.PURPLE_TERRACOTTA));
		for (int dy = 0; dy <= 4; dy++) {
			int w = dy <= 1 ? 3 - dy : dy == 2 ? 4 : 3;
			for (int k = -w; k <= w; k++) {
				int bx = plane == Direction.Axis.X ? x + k : x, bz = plane == Direction.Axis.Z ? z + k : z;
				if (dy < 4 || Math.abs(k) < w || unit(bx, bz + dy, 9) < 0.7) {
					p.set(bx, y + h + dy - 1, bz, (k + dy) % 3 == 0 ? s(Blocks.PURPLE_TERRACOTTA) : s(Blocks.PURPLE_CONCRETE));
				}
			}
		}
	}

	/** A branching coral sculpture: a trunk splitting into angled limbs of red or brown terracotta. */
	static void branchCoral(Canvas p, int x, int y, int z, Block b, int seed) {
		if (!p.near(x, z, 5)) {
			return;
		}
		p.fill(x, y, z, x, y + 2, z, s(b));
		int[][] limbs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
		for (int i = 0; i < 4; i++) {
			int[] l = limbs[(i + seed) % 4];
			int len = 2 + (i + seed) % 2;
			for (int k = 1; k <= len; k++) {
				p.set(x + l[0] * k, y + 1 + k, z + l[1] * k, s(b)); // each step joined to the one before
				p.set(x + l[0] * k, y + 2 + k, z + l[1] * k, s(b));
			}
			p.set(x + l[0] * len, y + 3 + len, z + l[1] * len, s(b));
		}
		p.fill(x, y + 3, z, x, y + 4, z, s(b));
	}

	/** A blue sponge tower: a lumpy column of blue concrete and lapis, {@code h} high, with a hollow-looking cap. */
	static void sponge(Canvas p, int x, int y, int z, int h) {
		if (!p.inChunk(x, z)) {
			return;
		}
		for (int k = 0; k < h; k++) {
			p.set(x, y + k, z, k % 2 == 0 ? s(Blocks.BLUE_CONCRETE) : s(Blocks.LAPIS_BLOCK));
		}
		p.set(x, y + h, z, s(Blocks.BLUE_STAINED_GLASS));
	}

	// ============================================================================================ the nautilus dome

	/**
	 * The nautilus dome (the owner-approved own design in place of the stacked pavilion): a pale blue drum 25 across
	 * with quartz ribs and a cyan window band, flaring to a 35-wide shell rim, then a spiral shell of red nether brick
	 * with three gold spiral seams and one lit cyan spiral band, closing to a gold finial at G + 43.
	 */
	private void nautilusDome(Canvas p) {
		if (!p.near(0, 0, 19)) {
			return;
		}
		int y0 = TERRACE + 1, drumTop = y0 + 6, flare = drumTop + 1, apex = G + 43;
		for (int x = -18; x <= 18; x++) {
			for (int z = -18; z <= 18; z++) {
				if (!p.inChunk(x, z)) {
					continue;
				}
				double d = Math.hypot(x, z);
				double a = angle(x, z);
				// drum
				if (d <= 12.5) {
					boolean shell = d > 11.5;
					for (int y = y0; y <= drumTop; y++) {
						BlockState b;
						if (!shell) {
							b = d <= 1.2 ? s(Blocks.SEA_LANTERN) : s(Blocks.AIR);
						} else if (Math.floorMod((int) Math.round(Math.toDegrees(a) / 15), 2) == 0 && y < drumTop) {
							b = s(Blocks.QUARTZ_PILLAR);
						} else if (y == drumTop - 1) {
							b = s(Blocks.CYAN_STAINED_GLASS);
						} else {
							b = s(Blocks.LIGHT_BLUE_CONCRETE);
						}
						p.set(x, y, z, b);
					}
				}
				// the flared shell rim
				if (d <= 17.5 && d > 12.5) {
					p.set(x, flare, z, d > 16.5 ? Kit.slab(Blocks.RED_NETHER_BRICK_SLAB, false) : s(Blocks.RED_NETHER_BRICKS));
					if (d > 16.5) {
						p.set(x, flare - 1, z, Kit.slab(Blocks.SMOOTH_QUARTZ_SLAB, true));
					}
				}
				// the spiral shell
				for (int y = flare; y <= apex - 2; y++) {
					double t = (y - flare) / (double) (apex - 2 - flare);
					double r = 16.0 * Math.pow(1 - t, 0.75) + 0.6;
					double ri = r - 1.3;
					if (d <= r && d > ri) {
						double seam = (a / (Math.PI * 2) * 3 + t * 2.2) % 1.0;
						double band = (a / (Math.PI * 2) + t * 1.4) % 1.0;
						BlockState b;
						if (seam < 0.09) {
							b = s(Blocks.YELLOW_TERRACOTTA);
						} else if (band > 0.47 && band < 0.55) {
							b = s(Blocks.CYAN_STAINED_GLASS);
						} else {
							b = s(Blocks.RED_NETHER_BRICKS);
						}
						p.set(x, y, z, b);
					} else if (d <= ri && d <= 1.2) {
						p.set(x, y, z, s(Blocks.SEA_LANTERN)); // a lit core behind the cyan band
					}
				}
			}
		}
		p.set(0, apex - 1, 0, s(Blocks.GOLD_BLOCK));
		p.set(0, apex, 0, s(Blocks.YELLOW_TERRACOTTA));
	}

	// ===================================================================================== towers and the rear wall

	/**
	 * A conch tower (own design): a pale blue shaft 9 across with a gold spiral band, a flared support, a gold-edged
	 * gallery 13 across, a blue-and-pale chamber with narrow windows, and a conch tip of red nether brick 19 across at
	 * its base with a purple spiral line, closing to a gold finial 39 above the garden.
	 */
	private void conchTower(Canvas p, int cx, int cz) {
		if (!p.near(cx, cz, 11)) {
			return;
		}
		int g = G, shaftTop = g + 24, gallery = g + 27, chamberTop = g + 31, tipBase = g + 32, apex = g + 39;
		for (int x = cx - 10; x <= cx + 10; x++) {
			for (int z = cz - 10; z <= cz + 10; z++) {
				if (!p.inChunk(x, z)) {
					continue;
				}
				double d = Math.hypot(x - cx, z - cz), a = angle(x - cx, z - cz);
				if (d <= 5.5) {
					p.fill(x, G - 4, z, x, g, z, s(Blocks.STONE));
				}
				if (d <= 4.5) {
					boolean shell = d > 3.5;
					for (int y = g + 1; y <= shaftTop; y++) {
						double band = (a / (Math.PI * 2) + (y - g) / 9.0) % 1.0;
						p.set(x, y, z, !shell ? s(Blocks.AIR) : band < 0.12 ? s(Blocks.YELLOW_TERRACOTTA) : s(Blocks.LIGHT_BLUE_CONCRETE));
					}
				}
				// flared support
				if (d <= 5.5 && d > 4.5) {
					p.set(x, shaftTop, z, Kit.slab(Blocks.SMOOTH_QUARTZ_SLAB, true));
					p.set(x, shaftTop + 1, z, s(Blocks.SMOOTH_QUARTZ));
				}
				if (d <= 6.5 && d > 5.5) {
					p.set(x, shaftTop + 1, z, Kit.slab(Blocks.SMOOTH_QUARTZ_SLAB, true));
					p.set(x, shaftTop + 2, z, s(Blocks.SMOOTH_QUARTZ));
				}
				// gallery
				if (d <= 6.5) {
					p.set(x, gallery, z, d > 5.5 ? s(Blocks.CUT_SANDSTONE) : s(Blocks.WHITE_CONCRETE));
					if (d > 5.5) {
						p.set(x, gallery + 1, z, Kit.slab(Blocks.SANDSTONE_SLAB, false));
					}
				}
				// chamber
				if (d <= 4.5) {
					boolean shell = d > 3.5;
					for (int y = gallery + 1; y <= chamberTop; y++) {
						BlockState b;
						if (!shell) {
							b = y == gallery + 2 && d <= 1 ? s(Blocks.SEA_LANTERN) : s(Blocks.AIR);
						} else if (y == gallery + 2) {
							b = s(Blocks.CYAN_STAINED_GLASS);
						} else {
							b = Math.floorMod((int) Math.round(Math.toDegrees(a) / 30), 2) == 0 ? s(Blocks.BLUE_CONCRETE) : s(Blocks.WHITE_CONCRETE);
						}
						p.set(x, y, z, b);
					}
				}
				// the conch tip
				for (int y = tipBase; y <= apex - 1; y++) {
					double t = (y - tipBase) / (double) (apex - 1 - tipBase);
					double r = 9.5 * Math.pow(1 - t, 1.2) + 0.5;
					if (d <= r && d > r - 1.4) {
						double line = (a / (Math.PI * 2) + t * 2.5) % 1.0;
						p.set(x, y, z, line < 0.1 ? s(Blocks.PURPLE_TERRACOTTA) : s(Blocks.RED_NETHER_BRICKS));
					}
				}
			}
		}
		p.set(cx, apex, cz, s(Blocks.GOLD_BLOCK));
	}

	/**
	 * The scalloped rear wall (own design): pale blue, 19 high, between the towers; each 6-block bay rises in a shell
	 * curve under a burgundy cap with a thin gold border, blue diamonds high up, and tall recessed panels in every other
	 * bay. Open toward the gardens (it only runs round the back).
	 */
	private void rearWall(Canvas p) {
		if (!p.near(0, -30, 70)) {
			return;
		}
		for (int i = 0; i + 1 < TOWERS.length; i++) {
			int[] a = TOWERS[i], b = TOWERS[i + 1];
			double len = Math.hypot(b[0] - a[0], b[1] - a[1]);
			int n = (int) (len * 2);
			for (int k = 0; k <= n; k++) {
				double t = k / (double) n, s = t * len;
				double x = a[0] + (b[0] - a[0]) * t, z = a[1] + (b[1] - a[1]) * t;
				if (s < 5 || s > len - 5) {
					continue; // inside the towers
				}
				double bay = (s % 6) / 6.0;
				int top = G + 17 + (int) Math.round(2 * Math.sin(Math.PI * bay));
				boolean panel = ((int) (s / 6)) % 2 == 0 && bay > 0.25 && bay < 0.75;
				for (int w = 0; w <= 1; w++) {
					// two thick, stepping across the line toward the inside (south)
					double nx = -(b[1] - a[1]) / len, nz = (b[0] - a[0]) / len;
					if (nz < 0) {
						nx = -nx;
						nz = -nz;
					}
					int bx = (int) Math.round(x + nx * w), bz = (int) Math.round(z + nz * w);
					if (!p.inChunk(bx, bz)) {
						continue;
					}
					p.fill(bx, G - 3, bz, bx, G, bz, s(Blocks.STONE));
					for (int y = G + 1; y <= top; y++) {
						BlockState st = s(Blocks.LIGHT_BLUE_CONCRETE);
						if (y == top) {
							st = s(Blocks.RED_NETHER_BRICKS);
						} else if (y == top - 1) {
							st = s(Blocks.YELLOW_TERRACOTTA);
						} else if (y >= top - 4 && y <= top - 3 && Math.abs(bay - 0.5) < 0.18) {
							st = s(Blocks.BLUE_TERRACOTTA);
						} else if (w == 1 && panel && y >= G + 3 && y <= G + 13) {
							st = s(Blocks.DARK_PRISMARINE);
						}
						p.set(bx, y, bz, st);
					}
					p.set(bx, top + 1, bz, Kit.slab(Blocks.RED_NETHER_BRICK_SLAB, false));
				}
			}
		}
	}

	// ================================================================================================= side wings

	/**
	 * A side wing (west for side -1): 9 wide along three angled segments (about 39 long), pale columns, open
	 * passages at the foot, light blue walls with large arched teal windows, purple and gold bands, a burgundy roof
	 * 14 above the garden with blue tube sponges on its corners. Its south end stops short of the approach.
	 */
	private void wing(Canvas p, int side) {
		int[][] pts = {{side * 31, 24}, {side * 42, 33}, {side * 44, 46}, {side * 38, 58}};
		if (!p.near(side * 38, 40, 30)) {
			return;
		}
		int roof = G + 14;
		for (int x = side * 24; side < 0 ? x >= -52 : x <= 52; x += side) {
			for (int z = 16; z <= 66; z++) {
				if (!p.inChunk(x, z)) {
					continue;
				}
				double best = 99, along = 0, run = 0;
				for (int i = 0; i + 1 < pts.length; i++) {
					double ax = pts[i][0], az = pts[i][1], bx = pts[i + 1][0], bz = pts[i + 1][1];
					double vx = bx - ax, vz = bz - az, l2 = vx * vx + vz * vz;
					double t = Math.max(0, Math.min(1, ((x - ax) * vx + (z - az) * vz) / l2));
					double dd = Math.hypot(x - (ax + t * vx), z - (az + t * vz));
					if (dd < best) {
						best = dd;
						along = run + t * Math.sqrt(l2);
					}
					run += Math.sqrt(l2);
				}
				if (best > 5.6) {
					continue;
				}
				if (best > 4.6) {
					p.set(x, roof, z, Kit.slab(Blocks.RED_NETHER_BRICK_SLAB, false)); // eave
					continue;
				}
				p.fill(x, G - 2, z, x, G, z, s(Blocks.STONE));
				p.set(x, G, z, s(Blocks.SMOOTH_SANDSTONE));
				boolean edge = best > 3.6;
				int m = (int) Math.floor(along) % 5;
				for (int y = G + 1; y < roof; y++) {
					BlockState b = s(Blocks.AIR);
					if (edge) {
						int k = y - G;
						if (m == 0) {
							b = s(Blocks.QUARTZ_PILLAR);
						} else if (k <= 3) {
							b = s(Blocks.AIR); // open passages at the foot
						} else if (k == 4) {
							b = s(Blocks.SMOOTH_QUARTZ);
						} else if (k == 12) {
							b = s(Blocks.YELLOW_TERRACOTTA);
						} else if (k == 13) {
							b = s(Blocks.PURPLE_TERRACOTTA);
						} else if (m >= 2 && m <= 3 && k >= 6 && k <= 10) {
							b = s(Blocks.CYAN_STAINED_GLASS);
						} else if ((m == 1 || m == 4) && k == 10) {
							b = s(Blocks.CYAN_STAINED_GLASS); // the arch's shoulders
						} else {
							b = s(Blocks.LIGHT_BLUE_CONCRETE);
						}
					} else if (y == G + 4) {
						b = s(Blocks.SMOOTH_QUARTZ); // an upper floor over the passage
					}
					p.set(x, y, z, b);
				}
				p.set(x, roof, z, s(Blocks.RED_NETHER_BRICKS));
				p.set(x, roof + 1, z, best < 2.6 ? Kit.slab(Blocks.RED_NETHER_BRICK_SLAB, false) : s(Blocks.AIR));
			}
		}
		for (int[] pt : pts) {
			sponge(p, pt[0], roof + 1, pt[1], 2);
			sponge(p, pt[0] + 1, roof + 1, pt[1], 3);
		}
	}

	// ============================================================================================ breaking waves

	/**
	 * Six breaking-wave arches (own design): translucent ribbons of light blue glass with cyan edges, rising from one
	 * foot and curling over at the crest before coming down to the other; sea lanterns hidden under the feet.
	 * Each: x1, z1, x2, z2, rise, width.
	 */
	static final int[][] WAVES = {{-48, -56, -72, -24, 18, 4}, {48, -56, 72, -24, 18, 4}, {-60, 6, -58, 38, 15, 3},
			{60, 6, 58, 38, 15, 3}, {-34, -26, -12, -60, 23, 5}, {34, -26, 12, -60, 23, 5}};

	private void waveArches(Canvas p) {
		for (int[] w : WAVES) {
			waveArch(p, w[0], w[1], w[2], w[3], w[4], w[5]);
		}
	}

	static void waveArch(Canvas p, int x1, int z1, int x2, int z2, int rise, int width) {
		double len = Math.hypot(x2 - x1, z2 - z1);
		if (!p.near((x1 + x2) / 2, (z1 + z2) / 2, (int) len / 2 + width + 4)) {
			return;
		}
		double ux = (x2 - x1) / len, uz = (z2 - z1) / len, nx = -uz, nz = ux;
		int g1 = gardenY(x1, z1), g2 = gardenY(x2, z2);
		java.util.List<double[]> pts = new java.util.ArrayList<>();
		// the main sweep (asymmetric: steep rise, crest at 70 %)
		int n = (int) (len * 3);
		for (int i = 0; i <= n; i++) {
			double t = i / (double) n;
			double h = t < 0.7 ? Math.sin(Math.PI / 2 * t / 0.7) : Math.cos(Math.PI / 2 * (t - 0.7) / 0.3);
			double base = g1 + (g2 - g1) * t;
			pts.add(new double[]{t * len, base + rise * Math.pow(Math.max(0, h), 0.8)});
		}
		// the curl: from the crest, a hook rolling forward and down
		double cs = 0.7 * len, ch = g1 + (g2 - g1) * 0.7 + rise;
		double cr = Math.max(2.5, rise * 0.18);
		for (int i = 0; i <= 40; i++) {
			double a = Math.PI / 2 - i / 40.0 * Math.PI * 1.25;
			pts.add(new double[]{cs + Math.cos(a) * cr, ch - cr + Math.sin(a) * cr});
		}
		for (double[] q : pts) {
			double cx = x1 + ux * q[0], cz = z1 + uz * q[0];
			for (int k = 0; k < width; k++) {
				double off = k - (width - 1) / 2.0;
				int bx = (int) Math.round(cx + nx * off), bz = (int) Math.round(cz + nz * off);
				int by = (int) Math.round(q[1]);
				if (!p.inChunk(bx, bz) || by <= gardenY(bx, bz)) {
					continue;
				}
				boolean edgeK = k == 0 || k == width - 1;
				p.set(bx, by, bz, edgeK ? s(Blocks.CYAN_STAINED_GLASS) : s(Blocks.LIGHT_BLUE_STAINED_GLASS));
			}
		}
		for (int[] f : new int[][]{{x1, z1}, {x2, z2}}) {
			for (int k = 0; k < width; k++) {
				double off = k - (width - 1) / 2.0;
				int bx = (int) Math.round(f[0] + nx * off), bz = (int) Math.round(f[1] + nz * off);
				if (p.inChunk(bx, bz)) {
					p.set(bx, gardenY(bx, bz), bz, s(Blocks.SEA_LANTERN));
				}
			}
		}
	}

	// ============================================================================================== shell houses

	/** Eight houses round the outer paths: x, z, diameter, tall (spiral roof) or low, door direction (0 S, 1 W, 2 N, 3 E). */
	static final int[][] HOUSES = {{-64, -34, 11, 0, 3}, {-74, 4, 9, 1, 3}, {-62, 52, 12, 0, 2}, {-30, 60, 10, 1, 2},
			{64, -34, 9, 1, 1}, {74, 4, 11, 0, 1}, {62, 52, 9, 1, 2}, {30, 60, 13, 0, 2}};

	private void shellHouses(Canvas p) {
		for (int[] h : HOUSES) {
			shellHouse(p, h[0], h[1], h[2], h[3] == 1, h[4]);
		}
	}

	/**
	 * A shell house: round walls of white and light blue concrete with small cyan windows and an arched doorway;
	 * either a low, broad layered roof of orange terracotta stepping in, or a taller spiral roof of terracotta with a
	 * cut red sandstone spiral edge.
	 */
	static void shellHouse(Canvas p, int cx, int cz, int dia, boolean tall, int door) {
		if (!p.near(cx, cz, dia + 2)) {
			return;
		}
		int g = gardenY(cx, cz);
		double r = dia / 2.0;
		int wallTop = g + (tall ? 5 : 4);
		Direction dd = Direction.from2DDataValue(door);
		for (int x = cx - dia; x <= cx + dia; x++) {
			for (int z = cz - dia; z <= cz + dia; z++) {
				if (!p.inChunk(x, z)) {
					continue;
				}
				double d = Math.hypot(x - cx, z - cz), a = angle(x - cx, z - cz);
				if (d <= r) {
					p.fill(x, g - 2, z, x, g, z, s(Blocks.STONE));
					p.set(x, g, z, s(Blocks.SMOOTH_SANDSTONE));
					boolean shell = d > r - 1;
					for (int y = g + 1; y <= wallTop; y++) {
						BlockState b;
						if (!shell) {
							b = s(Blocks.AIR);
						} else if (y == g + 3 && Math.floorMod((int) Math.round(Math.toDegrees(a) / 45), 2) == 0) {
							b = s(Blocks.CYAN_STAINED_GLASS);
						} else {
							b = y <= g + 1 ? s(Blocks.LIGHT_BLUE_CONCRETE) : s(Blocks.WHITE_CONCRETE);
						}
						p.set(x, y, z, b);
					}
				}
				// roof
				if (tall) {
					for (int y = wallTop + 1; y <= wallTop + (int) (r * 1.8); y++) {
						double t = (y - wallTop - 1) / (r * 1.8);
						double rr = (r + 0.8) * (1 - t) + 0.4;
						if (d <= rr && d > rr - 1.3) {
							double spiral = (a / (Math.PI * 2) + t * 3) % 1.0;
							p.set(x, y, z, spiral < 0.14 ? s(Blocks.CUT_RED_SANDSTONE) : s(Blocks.TERRACOTTA));
						}
					}
				} else {
					for (int layer = 0; layer < 4; layer++) {
						double rr = r + 1.5 - layer * 1.6;
						if (d <= rr && d > rr - 1.6) {
							int y = wallTop + 1 + layer;
							boolean rim = d > rr - 0.8;
							p.set(x, y, z, rim && layer == 0 ? Kit.slab(Blocks.CUT_RED_SANDSTONE_SLAB, false)
									: layer % 2 == 0 ? s(Blocks.ORANGE_TERRACOTTA) : s(Blocks.TERRACOTTA));
						} else if (d <= rr - 1.6 && layer == 3) {
							p.set(x, wallTop + 1 + layer, z, s(Blocks.ORANGE_TERRACOTTA));
						}
					}
				}
			}
		}
		// the arched doorway
		int ex = cx + (int) Math.round(dd.getStepX() * (r - 0.5)), ez = cz + (int) Math.round(dd.getStepZ() * (r - 0.5));
		Direction side = dd.getClockWise();
		for (int w = -1; w <= 1; w++) {
			int bx = ex + side.getStepX() * w, bz = ez + side.getStepZ() * w;
			int h = w == 0 ? 3 : 2;
			p.fill(bx, g + 1, bz, bx, g + h, bz, s(Blocks.AIR));
			p.fill(bx - dd.getStepX(), g + 1, bz - dd.getStepZ(), bx - dd.getStepX(), g + h, bz - dd.getStepZ(), s(Blocks.AIR));
		}
		p.set(cx, wallTop, cz, s(Blocks.SMOOTH_QUARTZ));
		p.set(cx, wallTop - 1, cz, CloudtopIsles.lantern(Blocks.LANTERN, true));
	}

	// ======================================================================================== corals and lights

	/** Built coral gardens: purple fans, sponge towers, tube sponge clusters, pink coral flowers, branching sculptures. */
	private void corals(Canvas p) {
		int[][] fans = {{-46, 2}, {46, 2}, {-18, 40}, {18, 40}, {-70, 40}, {70, 40}, {-52, -48}, {52, -48}, {0, -62}};
		for (int i = 0; i < fans.length; i++) {
			fanCoral(p, fans[i][0], gardenY(fans[i][0], fans[i][1]) + 1, fans[i][1], 3 + i % 3, i % 2 == 0 ? Direction.Axis.X : Direction.Axis.Z);
		}
		int[][] sponges = overhaul ? new int[][]{{-36, 18, 5}, {36, 18, 6}, {-12, 34, 4}, {12, 34, 3}, {-54, 64, 7}, {54, 64, 5}, {-64, -4, 6},
				{64, -4, 4}, {-30, -62, 5}, {30, -62, 7}, {-8, 72, 3}, {8, 72, 4}}
				: new int[][]{{-36, 18, 5}, {36, 18, 6}, {-12, 34, 4}, {12, 34, 3}, {-54, 64, 7}, {54, 64, 5}, {-80, -14, 6},
				{80, -14, 4}, {-30, -62, 5}, {30, -62, 7}, {-8, 72, 3}, {8, 72, 4}};
		for (int[] s : sponges) {
			int y = gardenY(s[0], s[1]) + 1;
			sponge(p, s[0], y, s[1], s[2]);
			sponge(p, s[0] + 1, y, s[1], Math.max(2, s[2] - 2));
			sponge(p, s[0], y, s[1] + 1, Math.max(2, s[2] - 3));
		}
		int[][] branches = {{-26, 44}, {26, 44}, {-66, -8}, {66, -8}, {-44, 70}, {44, 70}};
		for (int i = 0; i < branches.length; i++) {
			branchCoral(p, branches[i][0], gardenY(branches[i][0], branches[i][1]) + 1, branches[i][1],
					i % 2 == 0 ? Blocks.RED_TERRACOTTA : Blocks.BROWN_TERRACOTTA, i);
		}
		// tall pink coral-like flowers: a stem of pink terracotta topped with pink and magenta
		int[][] flowers = overhaul ? new int[][]{{-18, 46}, {18, 46}, {-34, 30}, {34, 30}, {-50, 30}, {50, 30}, {-16, 70}, {16, 70}, {-60, -16}, {60, -16}}
				: new int[][]{{-8, 52}, {8, 52}, {-34, 30}, {34, 30}, {-50, 30}, {50, 30}, {-16, 70}, {16, 70}, {-60, -16}, {60, -16}};
		for (int[] f : flowers) {
			if (!p.inChunk(f[0], f[1])) {
				continue;
			}
			int y = gardenY(f[0], f[1]) + 1;
			p.fill(f[0], y, f[1], f[0], y + 2, f[1], s(Blocks.PINK_TERRACOTTA));
			p.set(f[0], y + 3, f[1], s(Blocks.MAGENTA_CONCRETE));
			p.set(f[0], y + 4, f[1], s(Blocks.PINK_CONCRETE));
		}
		// kelp and seagrass in the open sea round the pocket rim
		for (int x = -100; x <= 100; x += 3) {
			for (int z = -96; z <= 88; z += 3) {
				double e = pocket(x, z);
				if (e < 1.08 || e > 1.25 || !p.inChunk(x, z)) {
					continue;
				}
				int top = p.level().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR_WG,
						p.world(x, 0, z).getX(), p.world(x, 0, z).getZ());
				if (top < 30 || top > SEA - 6) {
					continue;
				}
				double u = unit(x, z, 77);
				if (u < 0.5) {
					int h = 3 + (int) (u * 12);
					for (int k = 0; k < h && top + k < SEA - 1; k++) {
						p.set(x, top + k, z, k == h - 1 ? s(Blocks.KELP) : s(Blocks.KELP_PLANT));
					}
				} else if (u < 0.8) {
					p.set(x, top, z, s(Blocks.SEAGRASS));
				}
			}
		}
	}

	/** Ten jellyfish: shallow pale bells of white and light blue glass round a sea lantern core, thin dangling tentacles. */
	private void jellyfish(Canvas p) {
		int[][] jelly = {{-30, 24, 16, 5}, {30, 26, 20, 6}, {-50, -6, 22, 7}, {50, -4, 18, 5}, {-20, 52, 14, 4},
				{22, 54, 24, 6}, {-66, 26, 18, 5}, {66, 24, 26, 7}, {-40, -40, 28, 6}, {40, -40, 21, 5}};
		for (int[] j : jelly) {
			int cx = j[0], cz = j[1], y = G + j[2];
			double r = j[3] / 2.0;
			if (!p.near(cx, cz, j[3])) {
				continue;
			}
			for (int x = cx - j[3]; x <= cx + j[3]; x++) {
				for (int z = cz - j[3]; z <= cz + j[3]; z++) {
					double d = Math.hypot(x - cx, z - cz);
					if (d <= r) {
						p.set(x, y, z, d > r - 1 ? s(Blocks.LIGHT_BLUE_STAINED_GLASS) : s(Blocks.WHITE_STAINED_GLASS));
					}
					if (d <= r - 1.2) {
						p.set(x, y + 1, z, s(Blocks.WHITE_STAINED_GLASS));
					}
				}
			}
			p.set(cx, y, cz, s(Blocks.SEA_LANTERN));
			int[][] tent = {{-1, 0, 4}, {1, 1, 5}, {0, -1, 3}};
			for (int[] t : tent) {
				for (int k = 1; k <= t[2]; k++) {
					p.set(cx + t[0], y - k, cz + t[1], Kit.connected(Blocks.LIGHT_BLUE_STAINED_GLASS_PANE, k % 2 == 0));
				}
			}
		}
	}

	/** Small cyan path lights: a prismarine wall support, a sea lantern, a cyan glass cap. */
	private void pathLights(Canvas p) {
		int[][] lights = {{-4, 30}, {4, 30}, {-4, 40}, {4, 40}, {-14, 48}, {14, 48}, {-26, 56}, {26, 56}, {-46, 58}, {46, 58},
				{-64, 40}, {64, 40}, {-70, 18}, {70, 18}, {-74, -6}, {74, -6}, {-16, 62}, {16, 62}};
		for (int[] l : lights) {
			if (!p.inChunk(l[0], l[1])) {
				continue;
			}
			int y = gardenY(l[0], l[1]) + 1;
			p.set(l[0], y, l[1], s(Blocks.PRISMARINE_WALL));
			p.set(l[0], y + 1, l[1], s(Blocks.SEA_LANTERN));
			p.set(l[0], y + 2, l[1], s(Blocks.CYAN_STAINED_GLASS));
		}
	}
}
