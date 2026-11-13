package com.grandline.world.plan;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/** Shared plumbing for island plans: name, terrain, anchors, and a few compound pieces several islands use. */
public abstract class BasePlan implements IslandPlan {
	private final String name;
	protected final Terrain terrain;
	private final List<Anchor> anchors = new ArrayList<>();

	protected BasePlan(String name, double wobble) {
		this.name = name;
		this.terrain = new Terrain(name, wobble);
	}

	@Override
	public final String name() {
		return name;
	}

	@Override
	public final Terrain terrain() {
		return terrain;
	}

	@Override
	public final List<Anchor> anchors() {
		return List.copyOf(anchors);
	}

	protected void anchor(Anchor anchor) {
		anchors.add(anchor);
	}

	/** Places this island's buried treasure (from its TREASURE anchors). */
	protected void treasure(Canvas c) {
		for (Anchor a : anchors) {
			if (a.type() == Anchor.Type.TREASURE) {
				Kit.treasure(c, a.x(), a.z(), name.hashCode());
			}
		}
	}

	protected static BlockState s(Block block) {
		return block.defaultBlockState();
	}

	/** Floating islet: a grassy top at {@code topY} over a tapering rock underside. */
	protected static void floatingIslet(Canvas c, int cx, int cz, int radius, int topY, Block top, Block under) {
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dz = -radius; dz <= radius; dz++) {
				double d = Math.sqrt(dx * dx + dz * dz) / radius;
				if (d > 1.0) {
					continue;
				}
				int depth = (int) Math.round((1.0 - d * d) * radius * 0.9) + 1;
				c.set(cx + dx, topY, cz + dz, s(top));
				c.fill(cx + dx, topY - depth, cz + dz, cx + dx, topY - 1, cz + dz, s(under));
			}
		}
	}

	/** Plank bridge with rope (fence) rails between two points, sloping evenly. */
	protected static void bridge(Canvas c, int x1, int y1, int z1, int x2, int y2, int z2) {
		int steps = Math.max(Math.abs(x2 - x1), Math.abs(z2 - z1));
		boolean alongX = Math.abs(x2 - x1) >= Math.abs(z2 - z1);
		for (int i = 0; i <= steps; i++) {
			double t = steps == 0 ? 0 : i / (double) steps;
			int x = (int) Math.round(x1 + (x2 - x1) * t);
			int z = (int) Math.round(z1 + (z2 - z1) * t);
			int y = (int) Math.round(y1 + (y2 - y1) * t);
			for (int w = -1; w <= 1; w++) {
				int bx = alongX ? x : x + w;
				int bz = alongX ? z + w : z;
				c.set(bx, y, bz, s(Blocks.SPRUCE_PLANKS));
				c.fill(bx, y + 1, bz, bx, y + 2, bz, s(Blocks.AIR));
				if (w != 0) {
					c.set(bx, y + 1, bz, Kit.connected(Blocks.SPRUCE_FENCE, alongX));
				}
			}
		}
	}

	/** Square curtain wall with crenellations, from (x1, z1) to (x2, z2), {@code height} tall above {@code y}. */
	protected static void curtainWall(Canvas c, int x1, int z1, int x2, int z2, int y, int height, Block wall, int gateX, int gateZ) {
		for (int x = x1; x <= x2; x++) {
			for (int z = z1; z <= z2; z++) {
				boolean edge = x == x1 || x == x2 || z == z1 || z == z2;
				if (!edge) {
					continue;
				}
				c.foundation(x, z, x, z, y, s(wall));
				boolean gate = Math.abs(x - gateX) <= 1 && Math.abs(z - gateZ) <= 1;
				for (int yy = y + 1; yy <= y + height; yy++) {
					c.set(x, yy, z, gate && yy <= y + 4 ? s(Blocks.AIR) : s(wall));
				}
				if (((x + z) & 1) == 0) {
					c.set(x, y + height + 1, z, s(wall));
				}
			}
		}
	}

	/** Windmill: round stone base, plank cap, four sails facing {@code facing}. */
	protected static void windmill(Canvas c, int cx, int cz, int y, Direction facing) {
		Kit.tower(c, cx, cz, 3, y, 11, Blocks.COBBLESTONE, Blocks.SPRUCE_STAIRS, Blocks.SPRUCE_SLAB, facing.getOpposite());
		int hubX = cx + facing.getStepX() * 4;
		int hubZ = cz + facing.getStepZ() * 4;
		int hubY = y + 10;
		c.set(hubX, hubY, hubZ, s(Blocks.SPRUCE_LOG));
		boolean alongX = facing.getAxis() == Direction.Axis.Z; // sails spread across the facing axis
		for (int i = 1; i <= 7; i++) {
			int[][] arms = {{i, 0}, {-i, 0}, {0, i}, {0, -i}};
			for (int[] arm : arms) {
				int ax = alongX ? hubX + arm[0] : hubX;
				int az = alongX ? hubZ : hubZ + arm[0];
				c.set(ax, hubY + arm[1], az, s(Blocks.STRIPPED_SPRUCE_LOG));
				int sx = alongX ? ax : ax;
				int sz = alongX ? az : az;
				if (arm[1] == 0 && i > 1) {
					c.set(sx, hubY + (arm[0] > 0 ? 1 : -1), sz, s(Blocks.WHITE_WOOL));
				} else if (arm[1] != 0 && i > 1) {
					int ox = alongX ? (arm[1] > 0 ? -1 : 1) : 0;
					int oz = alongX ? 0 : (arm[1] > 0 ? -1 : 1);
					c.set(ax + ox, hubY + arm[1], az + oz, s(Blocks.WHITE_WOOL));
				}
			}
		}
	}
}
