package com.grandline.world.plan;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * Analytic island terrain: a set of overlapping land masses, flattened areas and paths. Every height and surface block
 * is a pure function of island-local (x, z), so any chunk can be generated on its own and the pieces always line up.
 * The coastline wobble is seeded from the island name only, so every world gets the same designed island.
 */
public final class Terrain {
	public static final int WATER_TOP = 62;
	public static final int FLOOR = 39;

	/** Surface style of a land mass. */
	public enum Ground {
		GRASS(Blocks.GRASS_BLOCK, Blocks.DIRT), SAND(Blocks.SAND, Blocks.SANDSTONE), SNOW(Blocks.SNOW_BLOCK, Blocks.PACKED_ICE),
		STONE(Blocks.STONE, Blocks.STONE), BASALT(Blocks.BLACKSTONE, Blocks.BASALT), CALCITE(Blocks.CALCITE, Blocks.STONE),
		PODZOL(Blocks.PODZOL, Blocks.DIRT), MOSS(Blocks.MOSS_BLOCK, Blocks.DIRT), RED_SAND(Blocks.RED_SAND, Blocks.TERRACOTTA);

		final BlockState top;
		final BlockState under;

		Ground(net.minecraft.world.level.block.Block top, net.minecraft.world.level.block.Block under) {
			this.top = top.defaultBlockState();
			this.under = under.defaultBlockState();
		}
	}

	/** An elliptical hill: {@code height} blocks above sea level at its centre, falling off with {@code shape}. */
	public record Mass(double cx, double cz, double rx, double rz, double height, double shape, Ground ground) {
	}

	/** An area levelled to {@code y} (blending into the hills over {@code blend} blocks), optionally paved. */
	public record Flat(double cx, double cz, double rx, double rz, int y, double blend, BlockState paving) {
	}

	/** A road along a polyline: surface replaced by {@code block} within {@code width / 2} of the line. */
	public record Path(double[] points, double width, BlockState block) {
	}

	private final List<Mass> masses = new ArrayList<>();
	private final List<Flat> flats = new ArrayList<>();
	private final List<Path> paths = new ArrayList<>();
	private final double phaseA;
	private final double phaseB;
	private final double wobble;
	private int extent;

	public Terrain(String name, double wobble) {
		java.util.Random random = new java.util.Random(name.hashCode() * 0x9E3779B97F4A7C15L);
		this.phaseA = random.nextDouble() * Math.PI * 2;
		this.phaseB = random.nextDouble() * Math.PI * 2;
		this.wobble = wobble;
	}

	public Terrain mass(double cx, double cz, double rx, double rz, double height, double shape, Ground ground) {
		masses.add(new Mass(cx, cz, rx, rz, height, shape, ground));
		extent = Math.max(extent, (int) Math.ceil(Math.max(Math.abs(cx) + rx, Math.abs(cz) + rz) * 1.35) + 6);
		return this;
	}

	public Terrain flat(double cx, double cz, double rx, double rz, int y, double blend, BlockState paving) {
		flats.add(new Flat(cx, cz, rx, rz, y, blend, paving));
		return this;
	}

	public Terrain path(BlockState block, double width, double... points) {
		paths.add(new Path(points, width, block));
		return this;
	}

	/** Makes sure structures out to {@code blocks} from the centre are generated (e.g. floating islets, long piers). */
	public Terrain reach(int blocks) {
		extent = Math.max(extent, blocks);
		return this;
	}

	/** Farthest any part of the island (shelf included) reaches from the centre. */
	public int extent() {
		return extent;
	}

	/** 0 at a mass centre, 1 at its (wobbly) edge. */
	private double norm(Mass m, double x, double z) {
		double dx = (x - m.cx()) / m.rx();
		double dz = (z - m.cz()) / m.rz();
		double angle = Math.atan2(dz, dx);
		double edge = 1.0 + wobble * (Math.sin(angle * 3 + phaseA) * 0.6 + Math.sin(angle * 5 + phaseB) * 0.4);
		return Math.sqrt(dx * dx + dz * dz) / edge;
	}

	/** Raw height above sea level from the masses alone (negative = under water). */
	private double raw(double x, double z) {
		double best = -40;
		for (Mass m : masses) {
			double d = norm(m, x, z);
			double h = d <= 1.0 ? 1.0 + m.height() * (1.0 - Math.pow(d, m.shape())) : 1.0 - (d - 1.0) * 18;
			best = Math.max(best, h);
		}
		return best;
	}

	/** Surface y (the top solid block) at island-local (x, z). Below {@link #WATER_TOP} means under water. */
	public int ground(int x, int z) {
		double h = WATER_TOP + raw(x, z);
		for (Flat f : flats) {
			double dx = (x - f.cx()) / f.rx();
			double dz = (z - f.cz()) / f.rz();
			double d = Math.sqrt(dx * dx + dz * dz);
			double edgeDistance = (d - 1.0) * Math.min(f.rx(), f.rz());
			if (edgeDistance <= 0) {
				h = f.y();
			} else if (edgeDistance < f.blend()) {
				double t = edgeDistance / f.blend();
				t = t * t * (3 - 2 * t);
				h = f.y() * (1 - t) + h * t;
			}
		}
		return (int) Math.round(h);
	}

	/** The mass whose surface style applies at (x, z): the one this point is most inside of. */
	private Ground groundStyle(double x, double z) {
		Ground best = Ground.GRASS;
		double bestD = Double.MAX_VALUE;
		for (Mass m : masses) {
			double d = norm(m, x, z) - m.height() * 0.001;
			if (d < bestD) {
				bestD = d;
				best = m.ground();
			}
		}
		return best;
	}

	/** Paving at (x, z) from flats and paths, or null. */
	public BlockState paving(int x, int z) {
		for (Path p : paths) {
			double[] pts = p.points();
			for (int i = 0; i + 3 < pts.length; i += 2) {
				if (distanceToSegment(x, z, pts[i], pts[i + 1], pts[i + 2], pts[i + 3]) <= p.width() / 2) {
					return p.block();
				}
			}
		}
		for (Flat f : flats) {
			if (f.paving() == null) {
				continue;
			}
			double dx = (x - f.cx()) / f.rx();
			double dz = (z - f.cz()) / f.rz();
			if (dx * dx + dz * dz <= 1.0) {
				return f.paving();
			}
		}
		return null;
	}

	public BlockState top(int x, int z) {
		int g = ground(x, z);
		BlockState paved = paving(x, z);
		if (paved != null && g >= WATER_TOP) {
			return paved;
		}
		Ground style = groundStyle(x, z);
		if (g <= WATER_TOP + 1 && style != Ground.SNOW && style != Ground.BASALT && style != Ground.STONE) {
			return Blocks.SAND.defaultBlockState(); // beaches and shallows
		}
		return style.top;
	}

	public BlockState under(int x, int z) {
		int g = ground(x, z);
		Ground style = groundStyle(x, z);
		if (g <= WATER_TOP + 1 && style != Ground.SNOW && style != Ground.BASALT && style != Ground.STONE) {
			return Blocks.SANDSTONE.defaultBlockState();
		}
		return style.under;
	}

	private static double distanceToSegment(double px, double pz, double ax, double az, double bx, double bz) {
		double vx = bx - ax;
		double vz = bz - az;
		double len = vx * vx + vz * vz;
		double t = len == 0 ? 0 : Math.max(0, Math.min(1, ((px - ax) * vx + (pz - az) * vz) / len));
		double dx = px - (ax + t * vx);
		double dz = pz - (az + t * vz);
		return Math.sqrt(dx * dx + dz * dz);
	}
}
