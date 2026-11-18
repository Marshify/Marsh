package com.grandline.world.plan;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SeaPickleBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Lotus Palace's cavern overhaul (layout 18, see docs/LOTUS_CAVERN_OVERHAUL.md): the walls of the palace pocket,
 * monumental columns, the whirlpool plaza, small plazas, coral basins, rim vegetation and the lighting pass. Runs on
 * the palace canvas (origin P, garden level G); the walls are written in the late world-generation pass, after the
 * world's own ores, so none show in them.
 *
 * <p>The wall is described in wall coordinates: the angle round the pocket (0 east, 90 south), the distance d in
 * front of the pocket's edge (positive into the pocket) and the height y. It is cut into 24 bays of 15 degrees, each
 * a natural cliff (strata, ledges, crevices, small caves, hanging formations) or architecture (a great arch, an arch
 * with a coral balcony, a mosaic, a waterfall, or the accessible gallery on the north side), with monumental columns
 * between architectural bays. Every bay's top rolls forward into a rocky overhang that meets the sea above.
 */
final class LotusCavern {
	private LotusCavern() {
	}

	static final int G = LotusPalace.G;
	/** Wall foot and wall top (the rock gives way to the sea above it). */
	static final int YB = G + 3, YT = LotusPalace.RIM_ROCK;
	/** Bay count, and the bays' kinds (bay i is centred on 15 i degrees). */
	static final int BAYS = 24;
	static final char[] KIND = {'A', 'N', 'W', 'A', 'N', 'T', 'M', 'T', 'N', 'A', 'W', 'N', 'M', 'A', 'N', 'A', 'N', 'G', 'G', 'G', 'N', 'A', 'W', 'N'};
	/** The gallery: its walkway height and the angles it spans. */
	static final int WALK = YB + 9;
	static final double GAL_A1 = Math.toRadians(247.5), GAL_A2 = Math.toRadians(292.5);

	static BlockState s(Block b) {
		return b.defaultBlockState();
	}

	// =============================================================================================== wall geometry

	/** The pocket edge's distance from its centre at angle {@code t} (radians, same as LotusPalace.pocket). */
	static double edge(double t) {
		double w = 1 + 0.05 * Math.sin(3 * t + 0.7) + 0.035 * Math.sin(5 * t + 2);
		return w / Math.sqrt(Math.pow(Math.cos(t) / LotusPalace.POCKET_RX, 2) + Math.pow(Math.sin(t) / LotusPalace.POCKET_RZ, 2));
	}

	static boolean architectural(int bay) {
		return KIND[Math.floorMod(bay, BAYS)] != 'N';
	}

	/** One wall column's coordinates. */
	record Col(double t, double r, double d, int bay, char kind, double tb, double c, double width, boolean column, double s) {
	}

	static Col col(int x, int z) {
		double dz = z - LotusPalace.POCKET_CZ;
		double t = Math.atan2(dz, x);
		if (t < 0) {
			t += Math.PI * 2;
		}
		double r = Math.hypot(x, dz), R = edge(t);
		double u = t / (Math.PI * 2) * BAYS + 0.5; // bay i centred on 15 i degrees
		int bay = (int) Math.floor(u) % BAYS;
		double fu = u - Math.floor(u);
		double width = Math.PI * 2 * R / BAYS;
		double tb = (fu - 0.5) * width;
		double c = Math.min(fu, 1 - fu) * width;
		int neighbour = fu < 0.5 ? bay - 1 : bay + 1;
		char k = KIND[bay], kn = KIND[Math.floorMod(neighbour, BAYS)];
		// a column where either side is architecture, except inside the gallery's run
		boolean column = (k != 'N' || kn != 'N') && !(k == 'G' && kn == 'G');
		double sGal = (t - GAL_A1) * R;
		return new Col(t, r, R - r, bay, k, tb, c, width, column, sGal);
	}

	// ======================================================================================================= walls

	/** The designed walls: every column within 10 blocks behind or 7 in front of the pocket's edge, foot to top. */
	static void walls(Canvas p) {
		for (int x = -100; x <= 100; x++) {
			for (int z = -96; z <= 88; z++) {
				if (!p.inChunk(x, z)) {
					continue;
				}
				double e = LotusPalace.pocket(x, z);
				if (e < 0.88 || e > 1.16) {
					continue;
				}
				Col w = col(x, z);
				if (w.d() > 7 || w.d() < -10) {
					continue;
				}
				int ground = w.d() > 0 ? LotusPalace.gardenY(x, z) : YB;
				for (int y = YB - 6; y <= YT; y++) {
					if (w.d() > 0 && y <= ground) {
						continue; // the garden in front of the wall stays
					}
					BlockState b = block(w, y, x, z);
					if (b != null) {
						p.set(x, y, z, b);
					}
				}
			}
		}
	}

	/** The block at height y in wall column w (null: leave what is there). */
	static BlockState block(Col w, int y, int x, int z) {
		double d = w.d();
		if (d < -5.5 && y > YT) {
			return null;
		}
		// The overhang and the rock top, shared by every bay: a rolling ledge into the sea above.
		if (y >= YT - 4) {
			double f = natural(w, y) + (y - (YT - 5)) * 1.1;
			return d < f ? naturalMaterial(w, y, d, f) : s(Blocks.AIR);
		}
		// Waterfall basins reach out into the garden.
		if (w.kind() == 'W') {
			BlockState basin = basin(w, y);
			if (basin != null) {
				return basin;
			}
		}
		// Columns between architectural bays (in front of everything else).
		if (w.column()) {
			BlockState c = column(w, y);
			if (c != null) {
				return c;
			}
		}
		return switch (w.kind()) {
			case 'N' -> {
				double f = natural(w, y);
				yield d < f ? naturalMaterial(w, y, d, f) : s(Blocks.AIR);
			}
			case 'A', 'T' -> arch(w, y);
			case 'M' -> mosaic(w, y);
			case 'W' -> waterfall(w, y);
			case 'G' -> gallery(w, y);
			default -> null;
		};
	}

	// --------------------------------------------------------------------------------------------- natural cliffs

	/** How far a natural cliff face stands out at this height (negative: a recess). */
	static double natural(Col w, int y) {
		int layer = Math.floorDiv(y - YB, 4);
		double t = w.t();
		double f = 1.2 + 1.6 * Math.sin(layer * 1.9 + w.bay() * 2.3 + t * 5) + 0.7 * Math.sin(t * 31 + layer * 0.7);
		double crack = (t * BAYS * 2.7 + w.bay() * 0.37) % 1.0;
		if (crack < 0.03 && y > YB + 1) {
			f = -3; // a vertical crevice
		}
		double cave = (t * BAYS * 0.9 + 0.2) % 1.0;
		if (cave < 0.07 && y >= YB + 1 && y <= YB + 5) {
			f = -4; // a small cave at the foot
		}
		// hanging formations under the top overhang
		double hang = (t * BAYS * 4.1) % 1.0;
		if (hang < 0.12 && y >= YT - 10 && y < YT - 4) {
			f = Math.max(f, 1.5 + (y - (YT - 10)) * 0.7);
		}
		return Math.max(-4, Math.min(5, f));
	}

	/**
	 * Natural rock material: horizontal strata of stone, andesite, tuff, calcite and diorite (each layer its own
	 * stone, so the colour runs in bands, not noise), smooth stone and polished andesite on ledge tops, prismarine
	 * where the rock meets the sea at the top.
	 */
	static BlockState naturalMaterial(Col w, int y, double d, double f) {
		if (f - d > 1.2) {
			return s(Blocks.STONE); // inside the rock
		}
		int layer = Math.floorDiv(y - YB, 4) + w.bay() % 3;
		boolean top = natural(w, y + 1) + (y + 1 >= YT - 4 ? (y + 1 - (YT - 5)) * 1.1 : 0) <= d;
		if (y >= YT - 2 && Math.floorMod(layer, 2) == 0) {
			return s(Blocks.PRISMARINE);
		}
		if (top) {
			return Math.floorMod(layer, 2) == 0 ? s(Blocks.SMOOTH_STONE) : s(Blocks.POLISHED_ANDESITE);
		}
		Block[] strata = {Blocks.STONE, Blocks.ANDESITE, Blocks.TUFF, Blocks.STONE, Blocks.CALCITE, Blocks.ANDESITE, Blocks.DIORITE};
		return s(strata[Math.floorMod(layer, strata.length)]);
	}

	// ------------------------------------------------------------------------------------------- architecture

	/**
	 * The architectural wall face: a dark prismarine plinth with lit insets, a polished diorite string course, calcite
	 * walling in courses, a prismarine frieze with cyan dentils, a stepped smooth quartz cornice.
	 */
	static BlockState plane(Col w, int y) {
		double d = w.d();
		int k = y - YB;
		double f = 0;
		BlockState face;
		if (k <= 2) {
			face = k == 1 && Math.floorMod((int) Math.round(w.tb()), 6) == 3 ? s(Blocks.SEA_LANTERN) : s(Blocks.DARK_PRISMARINE);
		} else if (k == 3) {
			f = 1;
			face = s(Blocks.POLISHED_DIORITE);
		} else if (y <= YT - 8) {
			face = Math.floorMod(k, 6) == 4 ? s(Blocks.POLISHED_DIORITE) : s(Blocks.CALCITE);
		} else if (y == YT - 7) {
			boolean dentil = Math.floorMod((int) Math.round(w.tb()), 2) == 0;
			f = 0; // flush: on the curved wall a dentil standing proud can end up touching nothing
			face = dentil ? s(Blocks.CYAN_CONCRETE) : s(Blocks.PRISMARINE_BRICKS);
		} else if (y == YT - 6) {
			face = s(Blocks.PRISMARINE_BRICKS);
		} else {
			f = y - (YT - 6); // YT-5: 1
			face = s(Blocks.SMOOTH_QUARTZ);
		}
		if (d >= f) {
			return s(Blocks.AIR);
		}
		return f - d > 1.2 ? s(Blocks.STONE) : face;
	}

	/**
	 * A monumental column, 6 wide (base 8), standing 4 out from the wall: a stepped base of dark prismarine, prismarine
	 * bricks and polished diorite with a lit inset; a smooth quartz shaft with quartz pillar flutes and prismarine
	 * bands every 7 rows (cyan centre); a capital of polished diorite with a gold boss and sea lanterns; a quartz
	 * abacus that carries the cornice.
	 */
	static BlockState column(Col w, int y) {
		double c = w.c(), d = w.d();
		int k = y - YB;
		double f, half;
		BlockState face;
		if (k <= 3) {
			half = 4;
			f = 5;
			face = k == 0 ? s(Blocks.DARK_PRISMARINE) : k == 3 ? s(Blocks.POLISHED_DIORITE) : s(Blocks.PRISMARINE_BRICKS);
			if (k == 2 && c < 0.6) {
				face = s(Blocks.SEA_LANTERN);
			}
		} else if (k == 4) {
			half = 3.5;
			f = 4.5;
			face = s(Blocks.POLISHED_DIORITE);
		} else if (y <= YT - 9) {
			half = 3;
			f = 4;
			boolean band = Math.floorMod(k - 5, 7) == 6;
			if (band) {
				face = c < 0.6 ? s(Blocks.CYAN_TERRACOTTA) : s(Blocks.PRISMARINE_BRICKS);
			} else {
				face = Math.abs(c - 1.5) < 0.5 ? s(Blocks.QUARTZ_PILLAR) : s(Blocks.SMOOTH_QUARTZ);
			}
		} else if (y <= YT - 6) {
			half = 4;
			f = 5;
			face = y == YT - 7 && c < 0.6 ? s(Blocks.GOLD_BLOCK)
					: y == YT - 7 && c > 1.5 && c < 2.6 ? s(Blocks.SEA_LANTERN) : s(Blocks.POLISHED_DIORITE);
		} else {
			half = 4.5;
			f = 5.5;
			face = s(Blocks.SMOOTH_QUARTZ);
		}
		if (c > half) {
			return null;
		}
		if (d >= f) {
			return s(Blocks.AIR);
		}
		return face;
	}

	/** Inside a great arch's opening (12 wide, springing 13 up, round head 19 up)? */
	static boolean archOpen(double tb, int y) {
		int spring = YB + 13;
		return Math.abs(tb) <= 6 && y >= YB + 1 && (y <= spring || Math.pow(y - spring, 2) + tb * tb <= 36);
	}

	/**
	 * A great recessed arch (A), optionally with a coral balcony (T): a smooth quartz frame with a gold keystone, a
	 * recess 4 deep whose back wall is prismarine brick with a cyan terracotta lattice and three lit alcoves, two
	 * slender quartz pillars standing in it, a dark prismarine tympanum with a cyan rosette, a smooth quartz floor.
	 */
	static BlockState arch(Col w, int y) {
		double tb = w.tb(), d = w.d();
		boolean open = archOpen(tb, y);
		boolean frame = !open && (archOpen(tb + 1, y) || archOpen(tb - 1, y) || archOpen(tb, y - 1));
		int spring = YB + 13;
		if (w.kind() == 'T') {
			BlockState b = balcony(w, y);
			if (b != null) {
				return b;
			}
		}
		if (frame) {
			boolean key = Math.abs(tb) < 0.6 && y > spring;
			if (d < 1) {
				return key ? s(Blocks.GOLD_BLOCK) : s(Blocks.SMOOTH_QUARTZ);
			}
			return s(Blocks.AIR);
		}
		if (!open) {
			if (y == YB && Math.abs(tb) <= 6 && d < 0 && d > -4.5) {
				return s(Blocks.SMOOTH_QUARTZ); // the recess floor
			}
			return plane(w, y);
		}
		// inside the opening
		int rt = (int) Math.round(tb);
		boolean alcove = (rt == 0 || Math.abs(rt) == 3) && (y == YB + 5 || y == YB + 6);
		double back = alcove ? -5 : -4;
		if (Math.abs(rt) == 4 && d >= -3 && d < -2 && y <= spring) {
			return y == spring ? s(Blocks.SMOOTH_QUARTZ) : s(Blocks.QUARTZ_PILLAR);
		}
		if (d >= back) {
			return s(Blocks.AIR);
		}
		if (d < back - 1.2) {
			return s(Blocks.STONE);
		}
		if (alcove) {
			return s(Blocks.SEA_LANTERN);
		}
		if (y > spring) {
			double rr = Math.hypot(tb, y - (spring + 3));
			return rr <= 1 ? s(Blocks.CYAN_CONCRETE) : rr <= 2 ? s(Blocks.BLUE_CONCRETE) : s(Blocks.DARK_PRISMARINE);
		}
		int a = Math.abs(rt), k = y - YB;
		boolean lattice = Math.floorMod(a + k, 4) == 0 || Math.floorMod(a - k, 4) == 0;
		return lattice ? s(Blocks.CYAN_TERRACOTTA) : s(Blocks.PRISMARINE_BRICKS);
	}

	/**
	 * The coral balcony in a T arch: a smooth quartz deck 9 up, carried on upturned stair brackets with lights under
	 * it, a prismarine wall railing, and a contained trough of water along its back with living coral and sea pickles
	 * (purely decorative: it is not reachable and is filled by the trough).
	 */
	static BlockState balcony(Col w, int y) {
		double tb = w.tb(), d = w.d();
		int deck = YB + 9;
		if (Math.abs(tb) > 5.5 || d < -4 || d >= 3) {
			return null;
		}
		int rt = (int) Math.round(tb);
		if (y == deck - 1) {
			if (d >= 1) {
				return Math.floorMod(rt, 3) == 0 ? s(Blocks.SEA_LANTERN) : Kit.stairs(Blocks.SMOOTH_QUARTZ_STAIRS, Direction.NORTH, true);
			}
			return s(Blocks.SMOOTH_QUARTZ);
		}
		if (y == deck) {
			boolean trough = d >= -3 && d < 0 && Math.abs(tb) <= 4.5;
			if (!trough) {
				return s(Blocks.SMOOTH_QUARTZ);
			}
			double u = LotusPalace.unit(rt, (int) Math.floor(d), 41 + w.bay());
			if (u < 0.35) {
				return waterlogged(new Block[]{Blocks.TUBE_CORAL_FAN, Blocks.BRAIN_CORAL_FAN, Blocks.HORN_CORAL_FAN, Blocks.FIRE_CORAL_FAN}[(int) (u * 100) % 4]);
			}
			if (u < 0.5) {
				return s(Blocks.SEA_PICKLE).setValue(SeaPickleBlock.PICKLES, 3).setValue(BlockStateProperties.WATERLOGGED, true);
			}
			return s(Blocks.WATER);
		}
		if (y == deck + 1 && d >= 2 && d < 3) {
			return s(Blocks.PRISMARINE_WALL);
		}
		return null;
	}

	static BlockState waterlogged(Block b) {
		BlockState st = s(b);
		return st.hasProperty(BlockStateProperties.WATERLOGGED) ? st.setValue(BlockStateProperties.WATERLOGGED, true) : st;
	}

	/**
	 * A wall monument (M): on the south wall a lotus — a sea lantern heart and quartz bud, eight calcite petals in
	 * relief on rings of cyan and blue with a light blue terracotta ring, a dark prismarine frame lit every 30 degrees;
	 * on the west wall a geometric carving of nested diamonds.
	 */
	static BlockState mosaic(Col w, int y) {
		double tb = w.tb(), d = w.d();
		int cy = YB + 15;
		double rho = Math.hypot(tb, y - cy);
		boolean lotus = w.bay() == 6;
		if (lotus) {
			if (rho > 7.8) {
				return plane(w, y);
			}
			double phi = Math.toDegrees(Math.atan2(y - cy, tb));
			double off = Math.abs(Math.floorMod((int) Math.round(phi + 22.5), 45) - 22.5);
			BlockState b;
			double f = 0;
			if (rho <= 1) {
				b = s(Blocks.SEA_LANTERN);
				f = 1;
			} else if (rho <= 2) {
				b = s(Blocks.SMOOTH_QUARTZ);
				f = 1;
			} else if (rho <= 6 && off < 3 + 9 * (1 - (rho - 2) / 5)) {
				b = s(Blocks.CALCITE);
				f = 1;
			} else if (rho <= 4.5) {
				b = s(Blocks.CYAN_CONCRETE);
			} else if (rho <= 5.2) {
				b = s(Blocks.LIGHT_BLUE_TERRACOTTA);
			} else if (rho <= 6) {
				b = s(Blocks.BLUE_CONCRETE);
			} else if (rho <= 7) {
				f = 1;
				b = Math.floorMod((int) Math.round(phi), 30) < 4 ? s(Blocks.SEA_LANTERN) : s(Blocks.DARK_PRISMARINE);
			} else {
				b = s(Blocks.PRISMARINE_BRICKS);
			}
			if (d >= f) {
				return s(Blocks.AIR);
			}
			return f - d > 1.2 ? s(Blocks.STONE) : b;
		}
		double dia = Math.abs(tb) + Math.abs(y - cy);
		if (dia > 8.5) {
			return plane(w, y);
		}
		int ring = (int) Math.floor(dia);
		BlockState b = ring == 0 ? s(Blocks.SEA_LANTERN) : ring == 8 ? s(Blocks.DARK_PRISMARINE)
				: ring % 2 == 0 ? s(Blocks.CYAN_TERRACOTTA) : ring == 3 || ring == 7 ? s(Blocks.SMOOTH_QUARTZ) : s(Blocks.PRISMARINE_BRICKS);
		double f = ring % 2 == 1 ? 1 : 0;
		if (ring == 0) {
			f = 1;
		}
		if (d >= f) {
			return s(Blocks.AIR);
		}
		return f - d > 1.2 ? s(Blocks.STONE) : b;
	}

	/**
	 * A waterfall (W): a channel 3 wide recessed 2 into the wall, dark prismarine behind, framed by prismarine brick
	 * with lights; a hidden source under a lip near the top feeds falling water down into a basin at the foot.
	 */
	static BlockState waterfall(Col w, int y) {
		double tb = w.tb(), d = w.d();
		int top = YT - 7;
		double a = Math.abs(tb);
		if (a <= 1.5 && y >= YB + 1 && y <= top) {
			if (d >= -2 && d < -1) {
				return y == top ? s(Blocks.WATER) : s(Blocks.WATER).setValue(LiquidBlock.LEVEL, 8);
			}
			if (d >= -1) {
				return y == top ? (d < 0 ? s(Blocks.DARK_PRISMARINE) : s(Blocks.AIR)) : s(Blocks.AIR);
			}
			return d < -3.2 ? s(Blocks.STONE) : s(Blocks.DARK_PRISMARINE);
		}
		if (a > 1.5 && a <= 2.5 && y >= YB + 1 && y <= top + 1) {
			if (d < 1) {
				return Math.floorMod(y - YB, 5) == 2 ? s(Blocks.SEA_LANTERN) : s(Blocks.PRISMARINE_BRICKS);
			}
			return s(Blocks.AIR);
		}
		return plane(w, y);
	}

	/** The basin a waterfall drops into: water over prismarine at the garden's level, a quartz rim, coral in it. */
	static BlockState basin(Col w, int y) {
		double tb = w.tb(), d = w.d();
		double a = Math.abs(tb);
		if (a > 4 || d < -2 || d >= 4) {
			return null;
		}
		boolean rim = a > 3 || d >= 3;
		if (y == YB - 1) {
			return s(Blocks.PRISMARINE_BRICKS);
		}
		if (y == YB) {
			if (rim) {
				return s(Blocks.POLISHED_DIORITE);
			}
			int rt = (int) Math.round(tb);
			if (d >= 0.5 && d < 2.5 && Math.floorMod(rt, 2) == 0) {
				return rt == 0 ? s(Blocks.SEA_PICKLE).setValue(SeaPickleBlock.PICKLES, 4).setValue(BlockStateProperties.WATERLOGGED, true)
						: waterlogged(rt > 0 ? Blocks.TUBE_CORAL_FAN : Blocks.BUBBLE_CORAL_FAN);
			}
			return s(Blocks.WATER);
		}
		if (y == YB + 1 && rim && d >= 0) {
			return Kit.slab(Blocks.SMOOTH_QUARTZ_SLAB, false);
		}
		return null;
	}

	/**
	 * The north gallery (G, accessible): a walkway 9 up running 45 degrees of the wall, 4 deep, on quartz pillars, with
	 * a prismarine wall railing; behind it a colonnade of round-headed arches 6 apart, each lit at the back. Stairs lead
	 * down from its middle (see galleryStairs).
	 */
	static BlockState gallery(Col w, int y) {
		double d = w.d(), s = w.s();
		double len = (GAL_A2 - GAL_A1) * (w.r() + d);
		boolean ends = s < 1 || s > len - 1;
		double mid = len / 2;
		int rs = (int) Math.round(s);
		if (y == WALK) {
			return d < 4 ? (Math.floorMod(rs, 4) == 0 ? s(Blocks.PRISMARINE_BRICKS) : s(Blocks.SMOOTH_QUARTZ)) : s(Blocks.AIR);
		}
		if (y == WALK - 1) {
			return d < 4 ? (d >= 3 ? s(Blocks.DARK_PRISMARINE) : s(Blocks.SMOOTH_QUARTZ)) : s(Blocks.AIR);
		}
		if (y < WALK - 1 && y > YB - 7) {
			if (d >= 3 && d < 4 && Math.floorMod(rs, 6) == 0) {
				return s(Blocks.QUARTZ_PILLAR);
			}
			if (d >= 0) {
				return y <= YB ? null : s(Blocks.AIR);
			}
			return plane(w, y);
		}
		if (y == WALK + 1 && d >= 3 && d < 4 && Math.abs(s - mid) > 1.5) {
			return s(Blocks.PRISMARINE_WALL);
		}
		if (y == WALK + 1 && ends && d >= 0 && d < 4) {
			return s(Blocks.PRISMARINE_WALL);
		}
		if (y > WALK && y <= WALK + 7) {
			double a = Math.floorMod(rs, 6) - 3;
			boolean pillar = Math.abs(a) >= 2.6;
			boolean open = !pillar && (y <= WALK + 5 || Math.pow(y - (WALK + 5), 2) + a * a <= 5);
			double back = open ? -3 : 0;
			if (d >= back) {
				return s(Blocks.AIR);
			}
			if (pillar && d >= -1) {
				return s(Blocks.QUARTZ_PILLAR);
			}
			if (open && Math.abs(a) < 0.6 && y == WALK + 3) {
				return s(Blocks.SEA_LANTERN);
			}
			return d < back - 1.2 ? s(Blocks.STONE) : open ? s(Blocks.PRISMARINE_BRICKS) : s(Blocks.SMOOTH_QUARTZ);
		}
		return plane(w, y);
	}

	/** The stairs from the gallery's middle down to the garden: 3 wide, straight out from the wall, railed. */
	static void galleryStairs(Canvas p) {
		double t = Math.toRadians(270);
		double R = edge(t);
		double zEdge = LotusPalace.POCKET_CZ - R;
		if (!p.near(0, (int) zEdge + 8, 12)) {
			return;
		}
		for (int k = 1; k <= 12; k++) {
			int z = (int) Math.floor(zEdge + 3.5 + k);
			int y = WALK - k;
			int ground = LotusPalace.gardenY(0, z);
			if (y < ground) {
				break;
			}
			for (int x = -2; x <= 2; x++) {
				p.fill(x, ground, z, x, y - 1, z, s(Blocks.PRISMARINE_BRICKS));
				if (Math.abs(x) == 2) {
					p.set(x, y, z, s(Blocks.PRISMARINE_BRICKS));
					p.set(x, y + 1, z, s(Blocks.PRISMARINE_WALL));
				} else {
					p.set(x, y, z, Kit.stairs(Blocks.SMOOTH_QUARTZ_STAIRS, Direction.NORTH, false));
					p.fill(x, y + 1, z, x, y + 3, z, s(Blocks.AIR));
				}
			}
		}
	}

	// ============================================================================================ grand columns

	/** Four monumental freestanding columns in the northern gardens, each joined to the wall by an arched beam. */
	static final double[] GRAND = {235, 257, 283, 305};

	static void grandColumns(Canvas p) {
		for (double deg : GRAND) {
			double t = Math.toRadians(deg), R = edge(t);
			double rc = R * 0.8;
			int cx = (int) Math.round(Math.cos(t) * rc), cz = (int) Math.round(LotusPalace.POCKET_CZ + Math.sin(t) * rc);
			if (!p.near(cx, cz, 26)) {
				continue;
			}
			int g = LotusPalace.gardenY(cx, cz);
			int top = YT;
			for (int x = cx - 5; x <= cx + 5; x++) {
				for (int z = cz - 5; z <= cz + 5; z++) {
					if (!p.inChunk(x, z)) {
						continue;
					}
					double r = Math.hypot(x - cx, z - cz), a = Math.toDegrees(Math.atan2(z - cz, x - cx));
					for (int y = g - 3; y <= top; y++) {
						int k = y - g;
						double half;
						BlockState b;
						if (k <= 1) {
							half = 4.6;
							b = k <= 0 ? s(Blocks.DARK_PRISMARINE) : s(Blocks.POLISHED_DIORITE);
						} else if (k <= 3) {
							half = 4.0;
							b = k == 3 && r > 3.2 && Math.floorMod((int) Math.round(a), 90) < 12 ? s(Blocks.SEA_LANTERN) : s(Blocks.PRISMARINE_BRICKS);
						} else if (y <= top - 7) {
							half = 3.1;
							boolean band = Math.floorMod(k - 4, 6) == 5;
							boolean flute = Math.floorMod((int) Math.round(a), 30) < 8;
							b = band ? (r > 2.4 && Math.floorMod((int) Math.round(a), 90) < 20 ? s(Blocks.CYAN_TERRACOTTA) : s(Blocks.PRISMARINE_BRICKS))
									: flute && r > 2.2 ? s(Blocks.QUARTZ_PILLAR) : s(Blocks.SMOOTH_QUARTZ);
						} else if (y <= top - 4) {
							half = 4.1;
							boolean front = r > 3.2;
							b = front && y == top - 5 && Math.floorMod((int) Math.round(a), 90) < 10 ? s(Blocks.GOLD_BLOCK)
									: front && y == top - 6 && Math.floorMod((int) Math.round(a + 45), 90) < 12 ? s(Blocks.SEA_LANTERN)
									: s(Blocks.POLISHED_DIORITE);
						} else {
							half = 4.6;
							b = y == top ? s(Blocks.POLISHED_DIORITE) : s(Blocks.SMOOTH_QUARTZ);
						}
						if (r <= half) {
							p.set(x, y, z, b);
						}
					}
				}
			}
			// the arched beam to the wall: 3 wide, flat on top, its underside curving up between column and wall
			double L = R - rc + 2;
			double ux = Math.cos(t), uz = Math.sin(t), nx = -uz, nz = ux;
			for (double u = 3.5; u <= L; u += 0.4) {
				double frac = (u - 3.5) / (L - 3.5);
				int low = top - 2 - (int) Math.round(5 * (1 - Math.sin(Math.PI * frac)));
				for (double o = -1; o <= 1; o += 1) {
					int bx = (int) Math.round(cx + ux * u + nx * o), bz = (int) Math.round(cz + uz * u + nz * o);
					if (!p.inChunk(bx, bz)) {
						continue;
					}
					for (int y = low; y <= top; y++) {
						BlockState b = y == top ? s(Blocks.POLISHED_DIORITE) : y == low
								? (Math.floorMod((int) Math.round(u), 4) == 0 && o == 0 ? s(Blocks.SEA_LANTERN) : s(Blocks.DARK_PRISMARINE))
								: s(Blocks.PRISMARINE_BRICKS);
						p.set(bx, y, bz, b);
					}
				}
			}
		}
	}

	// ===================================================================================== floor and plazas

	/**
	 * The ceremonial plaza round the palace whirlpool (centre unchanged, the whirlpool's own medallion kept): rings of
	 * smooth quartz, dark prismarine, prismarine brick with cyan glazed terracotta, polished diorite, a lit quartz
	 * ring, radial dark prismarine spokes pointing to the centre, a low quartz border open at the four approaches, and
	 * eight short lit columns round it.
	 */
	static void whirlpoolPlaza(Canvas p, int cx, int cz, int medallion) {
		if (!p.near(cx, cz, 18)) {
			return;
		}
		for (int x = cx - 17; x <= cx + 17; x++) {
			for (int z = cz - 17; z <= cz + 17; z++) {
				if (!p.inChunk(x, z)) {
					continue;
				}
				double d = Math.hypot(x - cx, z - cz);
				if (d > 16.6 || d <= medallion) {
					continue;
				}
				double a = Math.toDegrees(Math.atan2(z - cz, x - cx));
				double sp = Math.abs(Math.floorMod((int) Math.round(a), 45) - 22.5);
				boolean gap = Math.abs(x - cx) <= 2 || Math.abs(z - cz) <= 2;
				BlockState b;
				if (d <= medallion + 1) {
					b = s(Blocks.SMOOTH_QUARTZ);
				} else if (d <= medallion + 2) {
					b = s(Blocks.DARK_PRISMARINE);
				} else if (d <= 10.5) {
					b = Math.abs(d - 9) < 0.5 && Math.floorMod((int) Math.round(a), 45) < 9
							? s(Blocks.CYAN_GLAZED_TERRACOTTA) : s(Blocks.PRISMARINE_BRICKS);
				} else if (d <= 11.5) {
					b = s(Blocks.POLISHED_DIORITE);
				} else if (d <= 12.5) {
					b = Math.floorMod((int) Math.round(a), 30) < 5 ? s(Blocks.SEA_LANTERN) : s(Blocks.SMOOTH_QUARTZ);
				} else if (d <= 14.5) {
					b = sp > 20.5 ? s(Blocks.DARK_PRISMARINE) : s(Blocks.PRISMARINE_BRICKS);
				} else if (d <= 15.5) {
					b = s(Blocks.POLISHED_DIORITE);
				} else {
					b = s(Blocks.SMOOTH_QUARTZ);
				}
				p.set(x, G, z, b);
				p.fill(x, G + 1, z, x, G + 8, z, s(Blocks.AIR));
				if (d > 15.5 && !gap) {
					p.set(x, G + 1, z, Kit.slab(Blocks.SMOOTH_QUARTZ_SLAB, false)); // the low border
				}
			}
		}
		for (int k = 0; k < 8; k++) {
			double a = Math.toRadians(22.5 + 45 * k);
			int px = cx + (int) Math.round(Math.cos(a) * 17.5), pz = cz + (int) Math.round(Math.sin(a) * 17.5);
			if (!p.inChunk(px, pz)) {
				continue;
			}
			p.set(px, G, pz, s(Blocks.DARK_PRISMARINE));
			p.set(px, G + 1, pz, s(Blocks.POLISHED_DIORITE));
			p.set(px, G + 2, pz, s(Blocks.QUARTZ_PILLAR));
			p.set(px, G + 3, pz, s(Blocks.QUARTZ_PILLAR));
			p.set(px, G + 4, pz, s(Blocks.SEA_LANTERN));
			p.set(px, G + 5, pz, Kit.slab(Blocks.SMOOTH_QUARTZ_SLAB, false));
		}
	}

	/** Paving for the winding paths: a smooth quartz way with polished diorite edges and cyan terracotta studs. */
	static BlockState path(int x, int z, boolean edge) {
		if (edge) {
			return s(Blocks.POLISHED_DIORITE);
		}
		return Math.floorMod(x, 4) == 0 && Math.floorMod(z, 4) == 0 ? s(Blocks.CYAN_TERRACOTTA)
				: Math.floorMod(x + z, 7) == 0 ? s(Blocks.PRISMARINE_BRICKS) : s(Blocks.SMOOTH_QUARTZ);
	}

	/**
	 * Small plazas on the paths: a patterned round floor (a lit dark prismarine heart, prismarine brick, polished
	 * diorite, a quartz border), four end-rod lamp posts on the diagonals and two quartz benches, the middle left open.
	 */
	static final int[][] PLAZAS = {{-20, 50}, {20, 50}, {-70, -16}, {70, -16}};

	static void plazas(Canvas p) {
		for (int[] pl : PLAZAS) {
			int cx = pl[0], cz = pl[1];
			if (!p.near(cx, cz, 7)) {
				continue;
			}
			int g = LotusPalace.gardenY(cx, cz);
			for (int x = cx - 6; x <= cx + 6; x++) {
				for (int z = cz - 6; z <= cz + 6; z++) {
					double d = Math.hypot(x - cx, z - cz);
					if (d > 5.5 || !p.inChunk(x, z)) {
						continue;
					}
					BlockState b = d <= 0.6 ? s(Blocks.SEA_LANTERN) : d <= 1.6 ? s(Blocks.DARK_PRISMARINE) : d <= 3.4 ? s(Blocks.PRISMARINE_BRICKS)
							: d <= 4.5 ? s(Blocks.POLISHED_DIORITE) : s(Blocks.SMOOTH_QUARTZ);
					p.set(x, g, z, b);
					p.fill(x, g + 1, z, x, g + 4, z, s(Blocks.AIR));
				}
			}
			for (int[] o : new int[][]{{4, 4}, {-4, 4}, {4, -4}, {-4, -4}}) {
				int x = cx + o[0], z = cz + o[1];
				if (p.inChunk(x, z)) {
					p.set(x, g + 1, z, s(Blocks.POLISHED_DIORITE));
					p.set(x, g + 2, z, s(Blocks.QUARTZ_PILLAR));
					p.set(x, g + 3, z, s(Blocks.END_ROD));
				}
			}
			for (int[] o : new int[][]{{0, 4, 2}, {0, -4, 0}}) { // benches facing the middle
				for (int dx = -1; dx <= 1; dx++) {
					int x = cx + o[0] + dx, z = cz + o[1];
					if (p.inChunk(x, z)) {
						p.set(x, g + 1, z, Kit.stairs(Blocks.SMOOTH_QUARTZ_STAIRS, Direction.from2DDataValue(o[2]), false));
					}
				}
			}
		}
	}

	// ====================================================================================== rim, lights

	/**
	 * Where the rock top meets the sea (outside the pocket, in real water): kelp, seagrass, living coral and sea
	 * pickles on the rock top along the rim, in bands, so the walls give way to a living seabed above.
	 */
	static void rimLife(Canvas p) {
		Block[] coral = {Blocks.TUBE_CORAL_BLOCK, Blocks.BRAIN_CORAL_BLOCK, Blocks.BUBBLE_CORAL_BLOCK, Blocks.FIRE_CORAL_BLOCK, Blocks.HORN_CORAL_BLOCK};
		Block[] fans = {Blocks.TUBE_CORAL_FAN, Blocks.BRAIN_CORAL_FAN, Blocks.BUBBLE_CORAL_FAN, Blocks.FIRE_CORAL_FAN, Blocks.HORN_CORAL_FAN};
		for (int x = -100; x <= 100; x++) {
			for (int z = -96; z <= 88; z++) {
				if (!p.inChunk(x, z)) {
					continue;
				}
				double e = LotusPalace.pocket(x, z);
				if (e < 1.02 || e > 1.16) {
					continue;
				}
				Col w = col(x, z);
				if (w.d() >= -6 || w.d() < -10) {
					continue;
				}
				double band = Math.sin(w.t() * 37) + Math.sin(w.t() * 11 + 1);
				double u = LotusPalace.unit(x, z, 61);
				int y = YT + 1;
				if (!p.level().getBlockState(p.world(x, y, z)).is(Blocks.WATER)) {
					continue;
				}
				if (band > 0.9 && u < 0.5) {
					int h = 3 + (int) (u * 14);
					for (int k = 0; k < h && y + k < LotusPalace.CEILING; k++) {
						p.set(x, y + k, z, k == h - 1 ? s(Blocks.KELP) : s(Blocks.KELP_PLANT));
					}
				} else if (band < -0.8 && u < 0.35) {
					p.set(x, y, z, s(coral[(int) (u * 100) % 5]));
					p.set(x, y + 1, z, waterlogged(fans[(int) (u * 77) % 5]));
				} else if (u < 0.12) {
					p.set(x, y, z, s(Blocks.SEA_PICKLE).setValue(SeaPickleBlock.PICKLES, 2).setValue(BlockStateProperties.WATERLOGGED, true));
				} else if (u < 0.4) {
					p.set(x, y, z, s(Blocks.SEAGRASS));
				}
			}
		}
	}

	/**
	 * The lighting pass (all concealed: invisible light blocks, only in open air): an upper grid so the walls read from
	 * across the cavern, floodlighting round the hall and at the towers, so no gameplay area sits in heavy shadow.
	 */
	static void lights(Canvas p) {
		for (int x = -96; x <= 96; x += 1) {
			for (int z = -92; z <= 84; z += 1) {
				if (!p.inChunk(x, z) || LotusPalace.pocket(x, z) > 0.98) {
					continue;
				}
				if (Math.floorMod(x, 12) == 6 && Math.floorMod(z, 12) == 6) {
					light(p, x, 32, z, 15);
				}
			}
		}
		for (int x = -36; x <= 36; x += 6) {
			for (int z : new int[]{-27, 27}) {
				light(p, x, LotusPalace.FLOOR + 3, z, 15);
			}
		}
		for (int z = -24; z <= 24; z += 6) {
			for (int x : new int[]{-37, 37}) {
				light(p, x, LotusPalace.FLOOR + 3, z, 15);
			}
		}
		for (int[] t : LotusPalace.TOWERS) {
			for (int k = 0; k < 6; k++) {
				double a = k * Math.PI / 3;
				int x = t[0] + (int) Math.round(Math.cos(a) * 7), z = t[1] + (int) Math.round(Math.sin(a) * 7);
				light(p, x, G + 4, z, 15);
				light(p, x, G + 22, z, 14);
			}
		}
	}

	static void light(Canvas p, int x, int y, int z, int level) {
		if (p.inChunk(x, z) && p.level().getBlockState(p.world(x, y, z)).isAir()) {
			p.set(x, y, z, s(Blocks.LIGHT).setValue(LightBlock.LEVEL, level));
		}
	}
}
