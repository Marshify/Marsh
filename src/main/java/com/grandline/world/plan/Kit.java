package com.grandline.world.plan;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;

/**
 * Reusable structure pieces for planned islands: houses with interiors and doors, stepped roofs, towers, docks, trees
 * and street furniture. Every method takes island-local coordinates and writes through a {@link Canvas}, so pieces
 * can span chunk borders freely. Nothing here reads the world.
 */
public final class Kit {
	private Kit() {
	}

	/** Building materials. {@code roof} must be a stair block, {@code roofSlab} its slab. */
	public record Palette(Block wall, Block frame, Block floor, Block roof, Block roofSlab, Block door, Block window) {
		public static final Palette OAK_TOWN = new Palette(Blocks.WHITE_TERRACOTTA, Blocks.STRIPPED_OAK_LOG, Blocks.OAK_PLANKS,
				Blocks.BRICK_STAIRS, Blocks.BRICK_SLAB, Blocks.OAK_DOOR, Blocks.GLASS_PANE);
		public static final Palette BLUE_TOWN = new Palette(Blocks.SMOOTH_SANDSTONE, Blocks.STRIPPED_SPRUCE_LOG, Blocks.SPRUCE_PLANKS,
				Blocks.DARK_PRISMARINE_STAIRS, Blocks.DARK_PRISMARINE_SLAB, Blocks.SPRUCE_DOOR, Blocks.GLASS_PANE);
		public static final Palette BRICK = new Palette(Blocks.BRICKS, Blocks.STONE_BRICKS, Blocks.DARK_OAK_PLANKS,
				Blocks.DARK_OAK_STAIRS, Blocks.DARK_OAK_SLAB, Blocks.DARK_OAK_DOOR, Blocks.GLASS_PANE);
		public static final Palette COTTAGE = new Palette(Blocks.OAK_PLANKS, Blocks.OAK_LOG, Blocks.SPRUCE_PLANKS,
				Blocks.SPRUCE_STAIRS, Blocks.SPRUCE_SLAB, Blocks.SPRUCE_DOOR, Blocks.GLASS_PANE);
		public static final Palette STONE = new Palette(Blocks.STONE_BRICKS, Blocks.POLISHED_ANDESITE, Blocks.SPRUCE_PLANKS,
				Blocks.DEEPSLATE_TILE_STAIRS, Blocks.DEEPSLATE_TILE_SLAB, Blocks.IRON_DOOR, Blocks.IRON_BARS);
		public static final Palette SANDSTONE = new Palette(Blocks.SMOOTH_SANDSTONE, Blocks.CUT_SANDSTONE, Blocks.SANDSTONE,
				Blocks.SANDSTONE_STAIRS, Blocks.SANDSTONE_SLAB, Blocks.JUNGLE_DOOR, Blocks.GLASS_PANE);
		public static final Palette SNOW_LODGE = new Palette(Blocks.SPRUCE_PLANKS, Blocks.SPRUCE_LOG, Blocks.SPRUCE_PLANKS,
				Blocks.DEEPSLATE_BRICK_STAIRS, Blocks.DEEPSLATE_BRICK_SLAB, Blocks.SPRUCE_DOOR, Blocks.GLASS_PANE);
		public static final Palette PAGODA = new Palette(Blocks.RED_TERRACOTTA, Blocks.STRIPPED_CHERRY_LOG, Blocks.CHERRY_PLANKS,
				Blocks.DARK_PRISMARINE_STAIRS, Blocks.DARK_PRISMARINE_SLAB, Blocks.CHERRY_DOOR, Blocks.GLASS_PANE);
		public static final Palette QUARTZ = new Palette(Blocks.QUARTZ_BLOCK, Blocks.QUARTZ_PILLAR, Blocks.SMOOTH_QUARTZ,
				Blocks.QUARTZ_STAIRS, Blocks.QUARTZ_SLAB, Blocks.BIRCH_DOOR, Blocks.GLASS_PANE);
	}

	/** What goes inside a building. */
	public enum Interior { HOME, SHOP, HALL, STORE, NONE }

	// ------------------------------------------------------------------------------------------------ block states

	public static BlockState stairs(Block block, Direction facing, boolean upsideDown) {
		return block.defaultBlockState().setValue(StairBlock.FACING, facing).setValue(StairBlock.HALF, upsideDown ? Half.TOP : Half.BOTTOM);
	}

	public static BlockState slab(Block block, boolean top) {
		return block.defaultBlockState().setValue(SlabBlock.TYPE, top ? SlabType.TOP : SlabType.BOTTOM);
	}

	public static BlockState leaves(Block block) {
		return block.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
	}

	/** Pane, bars or fence with its connections set (generation does not update neighbours). */
	public static BlockState connected(Block block, boolean alongX) {
		BlockState state = block.defaultBlockState();
		if (!state.hasProperty(CrossCollisionBlock.EAST)) {
			return state;
		}
		return alongX
				? state.setValue(CrossCollisionBlock.EAST, true).setValue(CrossCollisionBlock.WEST, true)
				: state.setValue(CrossCollisionBlock.NORTH, true).setValue(CrossCollisionBlock.SOUTH, true);
	}

	public static void door(Canvas c, int x, int y, int z, Block door, Direction facing) {
		BlockState base = door.defaultBlockState().setValue(DoorBlock.FACING, facing).setValue(DoorBlock.HINGE, DoorHingeSide.LEFT);
		c.set(x, y, z, base.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
		c.set(x, y + 1, z, base.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
	}

	public static void lamp(Canvas c, int x, int z, int y) {
		c.fill(x, y + 1, z, x, y + 3, z, Blocks.DARK_OAK_FENCE.defaultBlockState());
		c.set(x, y + 4, z, Blocks.LANTERN.defaultBlockState());
	}

	// ------------------------------------------------------------------------------------------------ buildings

	/**
	 * A house: frame posts, walls with windows, a door in the middle of the {@code front} side, floors, a stepped
	 * gable roof and an interior. (x1, z1)-(x2, z2) is the outer footprint; {@code y} is the floor level.
	 */
	public static void house(Canvas c, int x1, int z1, int x2, int z2, int y, int floors, Palette p, Direction front, Interior interior) {
		int minX = Math.min(x1, x2), maxX = Math.max(x1, x2), minZ = Math.min(z1, z2), maxZ = Math.max(z1, z2);
		int storey = 4;
		int top = y + floors * storey;
		c.foundation(minX, minZ, maxX, maxZ, y - 1, Blocks.COBBLESTONE.defaultBlockState());
		c.fill(minX, y, minZ, maxX, y, maxZ, p.floor().defaultBlockState());
		for (int f = 0; f < floors; f++) {
			int fy = y + f * storey;
			for (int x = minX; x <= maxX; x++) {
				for (int z = minZ; z <= maxZ; z++) {
					boolean edgeX = x == minX || x == maxX;
					boolean edgeZ = z == minZ || z == maxZ;
					if (!edgeX && !edgeZ) {
						c.fill(x, fy + 1, z, x, fy + storey - 1, z, Blocks.AIR.defaultBlockState());
						if (f > 0) {
							c.set(x, fy, z, p.floor().defaultBlockState());
						}
						continue;
					}
					boolean corner = edgeX && edgeZ;
					for (int dy = 1; dy < storey; dy++) {
						BlockState state = corner || dy == storey - 1 ? p.frame().defaultBlockState() : p.wall().defaultBlockState();
						boolean windowRow = dy == 2 && !corner && ((edgeX ? z : x) % 3 == 0);
						if (windowRow) {
							state = connected(p.window(), !edgeX);
						}
						c.set(x, fy + dy, z, state);
					}
					c.set(x, fy + storey, z, p.frame().defaultBlockState());
				}
			}
			c.set((minX + maxX) / 2, fy + storey - 1, (minZ + maxZ) / 2, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
		}
		// Ladder to upper floors in the back corner.
		if (floors > 1) {
			for (int yy = y + 1; yy < top; yy++) {
				c.set(maxX - 1, yy, maxZ - 1, Canvas.facing(Blocks.LADDER.defaultBlockState(), Direction.NORTH));
			}
		}
		// Door in the middle of the front wall, with a step outside.
		int dx = front == Direction.EAST ? maxX : front == Direction.WEST ? minX : (minX + maxX) / 2;
		int dz = front == Direction.SOUTH ? maxZ : front == Direction.NORTH ? minZ : (minZ + maxZ) / 2;
		door(c, dx, y + 1, dz, p.door(), front.getOpposite());
		c.set(dx + front.getStepX(), y, dz + front.getStepZ(), p.floor().defaultBlockState());
		c.set(dx + front.getStepX(), y + 3, dz + front.getStepZ(), Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, false));
		gableRoof(c, minX - 1, minZ - 1, maxX + 1, maxZ + 1, top, p, maxX - minX >= maxZ - minZ);
		furnish(c, minX + 1, minZ + 1, maxX - 1, maxZ - 1, y, interior, front);
	}

	/** Stepped stair roof. With {@code ridgeAlongX} the slopes face north and south. */
	public static void gableRoof(Canvas c, int minX, int minZ, int maxX, int maxZ, int y, Palette p, boolean ridgeAlongX) {
		int span = ridgeAlongX ? maxZ - minZ : maxX - minX;
		for (int i = 0; i <= span / 2; i++) {
			int yy = y + i;
			if (ridgeAlongX) {
				int za = minZ + i, zb = maxZ - i;
				if (za == zb) {
					c.fill(minX, yy, za, maxX, yy, za, slab(p.roofSlab(), false));
				} else if (za < zb) {
					c.fill(minX, yy, za, maxX, yy, za, stairs(p.roof(), Direction.SOUTH, false));
					c.fill(minX, yy, zb, maxX, yy, zb, stairs(p.roof(), Direction.NORTH, false));
					c.fill(minX + 1, yy, za + 1, maxX - 1, yy, zb - 1, Blocks.AIR.defaultBlockState());
					c.fill(minX + 1, yy, za + 1, minX + 1, yy, zb - 1, p.wall().defaultBlockState());
					c.fill(maxX - 1, yy, za + 1, maxX - 1, yy, zb - 1, p.wall().defaultBlockState());
				}
			} else {
				int xa = minX + i, xb = maxX - i;
				if (xa == xb) {
					c.fill(xa, yy, minZ, xa, yy, maxZ, slab(p.roofSlab(), false));
				} else if (xa < xb) {
					c.fill(xa, yy, minZ, xa, yy, maxZ, stairs(p.roof(), Direction.EAST, false));
					c.fill(xb, yy, minZ, xb, yy, maxZ, stairs(p.roof(), Direction.WEST, false));
					c.fill(xa + 1, yy, minZ + 1, xb - 1, yy, minZ + 1, p.wall().defaultBlockState());
					c.fill(xa + 1, yy, maxZ - 1, xb - 1, yy, maxZ - 1, p.wall().defaultBlockState());
				}
			}
		}
	}

	/** Hipped (four-sided) stepped roof, used for towers and halls. */
	public static void hipRoof(Canvas c, int minX, int minZ, int maxX, int maxZ, int y, Block stair, Block slabBlock, Block fill) {
		int i = 0;
		while (minX + i <= maxX - i && minZ + i <= maxZ - i) {
			int x1 = minX + i, x2 = maxX - i, z1 = minZ + i, z2 = maxZ - i, yy = y + i;
			if (x1 == x2 || z1 == z2) {
				c.fill(x1, yy, z1, x2, yy, z2, slab(slabBlock, false));
				break;
			}
			c.fill(x1, yy, z1, x2, yy, z1, stairs(stair, Direction.SOUTH, false));
			c.fill(x1, yy, z2, x2, yy, z2, stairs(stair, Direction.NORTH, false));
			c.fill(x1, yy, z1 + 1, x1, yy, z2 - 1, stairs(stair, Direction.EAST, false));
			c.fill(x2, yy, z1 + 1, x2, yy, z2 - 1, stairs(stair, Direction.WEST, false));
			c.fill(x1 + 1, yy, z1 + 1, x2 - 1, yy, z2 - 1, fill.defaultBlockState());
			i++;
		}
	}

	public static void furnish(Canvas c, int minX, int minZ, int maxX, int maxZ, int y, Interior interior, Direction front) {
		switch (interior) {
			case HOME -> {
				Direction bedFacing = Direction.NORTH;
				BlockState bed = Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, bedFacing);
				c.set(minX, y + 1, minZ + 1, bed.setValue(BedBlock.PART, BedPart.FOOT));
				c.set(minX, y + 1, minZ, bed.setValue(BedBlock.PART, BedPart.HEAD));
				c.set(maxX, y + 1, minZ, Blocks.CRAFTING_TABLE.defaultBlockState());
				c.set(maxX, y + 1, maxZ, Canvas.facing(Blocks.BARREL.defaultBlockState(), Direction.UP));
				c.set((minX + maxX) / 2, y + 1, minZ, Blocks.OAK_FENCE.defaultBlockState());
				c.set((minX + maxX) / 2, y + 2, minZ, Blocks.OAK_PRESSURE_PLATE.defaultBlockState());
				c.set(minX, y + 1, maxZ, Blocks.POTTED_RED_TULIP.defaultBlockState());
			}
			case SHOP -> {
				// Counter across the room, goods on shelves behind it.
				int cz = (minZ + maxZ) / 2;
				for (int x = minX; x <= maxX; x++) {
					c.set(x, y + 1, cz - (front == Direction.SOUTH ? 1 : -1), stairs(Blocks.SPRUCE_STAIRS, front.getOpposite(), true));
				}
				int backZ = front == Direction.SOUTH ? minZ : maxZ;
				for (int x = minX; x <= maxX; x += 2) {
					c.set(x, y + 1, backZ, Canvas.facing(Blocks.BARREL.defaultBlockState(), Direction.UP));
					c.set(x, y + 2, backZ, Blocks.BOOKSHELF.defaultBlockState());
				}
			}
			case HALL -> {
				for (int x = minX + 1; x <= maxX - 1; x += 3) {
					c.set(x, y + 1, (minZ + maxZ) / 2, Blocks.SPRUCE_FENCE.defaultBlockState());
					c.set(x, y + 2, (minZ + maxZ) / 2, Blocks.SPRUCE_PRESSURE_PLATE.defaultBlockState());
				}
				c.fill(minX, y, minZ, maxX, y, maxZ, Blocks.RED_CARPET.defaultBlockState());
				c.fill(minX, y, minZ, maxX, y, maxZ, Blocks.SPRUCE_PLANKS.defaultBlockState());
				c.fill(minX + 1, y + 1, minZ + 1, maxX - 1, y + 1, minZ + 1, Blocks.RED_CARPET.defaultBlockState());
			}
			case STORE -> {
				for (int x = minX; x <= maxX; x++) {
					for (int z = minZ; z <= maxZ; z += 2) {
						if ((x + z) % 3 != 0) {
							c.set(x, y + 1, z, Canvas.facing(Blocks.BARREL.defaultBlockState(), Direction.UP));
						}
					}
				}
			}
			case NONE -> {
			}
		}
	}

	/** Round tower with a conical stepped cap and a ring of windows. */
	public static void tower(Canvas c, int cx, int cz, int radius, int y, int height, Block wall, Block capStair, Block capSlab, Direction doorSide) {
		c.foundation(cx - radius, cz - radius, cx + radius, cz + radius, y - 1, Blocks.COBBLESTONE.defaultBlockState());
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dz = -radius; dz <= radius; dz++) {
				double d = Math.sqrt(dx * dx + dz * dz);
				if (d > radius + 0.4) {
					continue;
				}
				boolean shell = d > radius - 0.8;
				c.set(cx + dx, y, cz + dz, Blocks.SPRUCE_PLANKS.defaultBlockState());
				for (int yy = y + 1; yy <= y + height; yy++) {
					boolean window = shell && (yy - y) % 6 == 3 && (dx == 0 || dz == 0);
					c.set(cx + dx, yy, cz + dz, shell ? (window ? Blocks.GLASS.defaultBlockState() : wall.defaultBlockState()) : Blocks.AIR.defaultBlockState());
				}
			}
		}
		for (int yy = y + 1; yy <= y + height; yy++) {
			c.set(cx - radius + 1, yy, cz, Canvas.facing(Blocks.LADDER.defaultBlockState(), Direction.EAST));
		}
		for (int i = 0; i <= radius + 1; i++) {
			int r = radius + 1 - i;
			for (int dx = -r; dx <= r; dx++) {
				for (int dz = -r; dz <= r; dz++) {
					double d = Math.sqrt(dx * dx + dz * dz);
					if (d <= r + 0.4 && d > r - 1.0) {
						Direction out = Math.abs(dx) >= Math.abs(dz) ? (dx > 0 ? Direction.WEST : Direction.EAST) : (dz > 0 ? Direction.NORTH : Direction.SOUTH);
						c.set(cx + dx, y + height + 1 + i, cz + dz, r == 0 ? slab(capSlab, false) : stairs(capStair, out, false));
					}
				}
			}
		}
		int ddx = doorSide.getStepX() * radius;
		int ddz = doorSide.getStepZ() * radius;
		door(c, cx + ddx, y + 1, cz + ddz, Blocks.SPRUCE_DOOR, doorSide.getOpposite());
	}

	/** Wooden pier from land out to sea along {@code direction}, with posts, rails and lanterns. */
	public static void pier(Canvas c, int x, int z, Direction direction, int length, int halfWidth) {
		int y = Terrain.WATER_TOP + 1;
		for (int i = 0; i <= length; i++) {
			for (int w = -halfWidth; w <= halfWidth; w++) {
				int px = x + direction.getStepX() * i + (direction.getAxis() == Direction.Axis.Z ? w : 0);
				int pz = z + direction.getStepZ() * i + (direction.getAxis() == Direction.Axis.X ? w : 0);
				c.set(px, y, pz, Blocks.SPRUCE_PLANKS.defaultBlockState());
				c.fill(px, y + 1, pz, px, y + 3, pz, Blocks.AIR.defaultBlockState());
				boolean edge = Math.abs(w) == halfWidth;
				if (edge && i % 4 == 0) {
					c.fill(px, Terrain.FLOOR, pz, px, y - 1, pz, Blocks.SPRUCE_LOG.defaultBlockState());
				}
				if (edge && i > 2) {
					c.set(px, y + 1, pz, connected(Blocks.SPRUCE_FENCE, direction.getAxis() == Direction.Axis.X));
					if (i % 6 == 0) {
						c.set(px, y + 2, pz, Blocks.LANTERN.defaultBlockState());
					}
				}
			}
		}
	}

	// ------------------------------------------------------------------------------------------------ nature

	/** Rounded blocky tree (oak, birch, cherry...). */
	public static void roundTree(Canvas c, int x, int z, int height, Block log, Block leaves) {
		int g = c.ground(x, z);
		if (g <= Terrain.WATER_TOP) {
			return;
		}
		BlockState leaf = leaves(leaves);
		int r = height >= 7 ? 3 : 2;
		for (int dy = -r; dy <= r; dy++) {
			for (int dx = -r; dx <= r; dx++) {
				for (int dz = -r; dz <= r; dz++) {
					if (dx * dx + dy * dy * 1.6 + dz * dz <= r * r + 1) {
						c.set(x + dx, g + height + dy, z + dz, leaf);
					}
				}
			}
		}
		c.fill(x, g + 1, z, x, g + height, z, log.defaultBlockState());
	}

	/** Tall jungle tree with a wide layered canopy and hanging vines. */
	public static void bigJungleTree(Canvas c, int x, int z, int height) {
		int g = c.ground(x, z);
		if (g <= Terrain.WATER_TOP) {
			return;
		}
		BlockState leaf = leaves(Blocks.JUNGLE_LEAVES);
		for (int layer = 0; layer < 3; layer++) {
			int r = 5 - layer;
			int yy = g + height - 2 + layer * 2;
			for (int dx = -r; dx <= r; dx++) {
				for (int dz = -r; dz <= r; dz++) {
					if (dx * dx + dz * dz <= r * r) {
						c.set(x + dx, yy, z + dz, leaf);
						if (layer == 0 && dx * dx + dz * dz > (r - 1) * (r - 1) && ((dx + dz) & 3) == 0) {
							c.fill(x + dx, yy - 3, z + dz, x + dx, yy - 1, z + dz, Blocks.AIR.defaultBlockState());
							c.set(x + dx, yy - 1, z + dz, Blocks.MOSS_CARPET.defaultBlockState());
						}
					}
				}
			}
		}
		c.fill(x, g + 1, z, x + 1, g + height, z + 1, Blocks.JUNGLE_LOG.defaultBlockState());
	}

	public static void palm(Canvas c, int x, int z, int height) {
		int g = c.ground(x, z);
		if (g <= Terrain.WATER_TOP) {
			return;
		}
		c.fill(x, g + 1, z, x, g + height, z, Blocks.JUNGLE_LOG.defaultBlockState());
		BlockState leaf = leaves(Blocks.JUNGLE_LEAVES);
		c.set(x, g + height + 1, z, leaf);
		for (Direction d : Direction.Plane.HORIZONTAL) {
			for (int i = 1; i <= 3; i++) {
				c.set(x + d.getStepX() * i, g + height + (i == 3 ? 0 : 1), z + d.getStepZ() * i, leaf);
			}
		}
		c.set(x + 1, g + height - 1, z, Blocks.COCOA.defaultBlockState().setValue(net.minecraft.world.level.block.CocoaBlock.FACING, Direction.WEST));
	}

	public static void pine(Canvas c, int x, int z, int height, boolean snowy) {
		int g = c.ground(x, z);
		if (g <= Terrain.WATER_TOP) {
			return;
		}
		BlockState leaf = leaves(Blocks.SPRUCE_LEAVES);
		for (int i = 2; i <= height; i++) {
			int r = Math.max(0, (height - i) / 2 + (i % 2 == 0 ? 0 : -1));
			for (int dx = -r; dx <= r; dx++) {
				for (int dz = -r; dz <= r; dz++) {
					if (Math.abs(dx) + Math.abs(dz) <= r + 1) {
						c.set(x + dx, g + i, z + dz, leaf);
						if (snowy) {
							c.set(x + dx, g + i + 1, z + dz, Blocks.SNOW.defaultBlockState());
						}
					}
				}
			}
		}
		c.set(x, g + height + 1, z, leaf);
		c.fill(x, g + 1, z, x, g + height - 1, z, Blocks.SPRUCE_LOG.defaultBlockState());
	}

	public static void deadTree(Canvas c, int x, int z, int height) {
		int g = c.ground(x, z);
		c.fill(x, g + 1, z, x, g + height, z, Blocks.DARK_OAK_LOG.defaultBlockState());
		c.set(x + 1, g + height - 1, z, Blocks.DARK_OAK_LOG.defaultBlockState());
		c.set(x - 1, g + height - 2, z, Blocks.DARK_OAK_LOG.defaultBlockState());
		c.set(x, g + height - 1, z + 1, Blocks.DARK_OAK_LOG.defaultBlockState());
	}

	/** A field of ripe crops with water channels and a fence. */
	public static void field(Canvas c, int x1, int z1, int x2, int z2, int y, Block crop) {
		int minX = Math.min(x1, x2), maxX = Math.max(x1, x2), minZ = Math.min(z1, z2), maxZ = Math.max(z1, z2);
		c.foundation(minX - 1, minZ - 1, maxX + 1, maxZ + 1, y, Blocks.DIRT.defaultBlockState());
		for (int x = minX; x <= maxX; x++) {
			for (int z = minZ; z <= maxZ; z++) {
				if ((x - minX) % 5 == 2) {
					c.set(x, y, z, Blocks.WATER.defaultBlockState());
					continue;
				}
				c.set(x, y, z, Blocks.FARMLAND.defaultBlockState().setValue(net.minecraft.world.level.block.FarmBlock.MOISTURE, 7));
				BlockState plant = crop.defaultBlockState();
				if (plant.hasProperty(CropBlock.AGE)) {
					plant = plant.setValue(CropBlock.AGE, 7);
				}
				c.set(x, y + 1, z, plant);
			}
		}
		for (int x = minX - 1; x <= maxX + 1; x++) {
			c.set(x, y + 1, minZ - 1, connected(Blocks.OAK_FENCE, true));
			c.set(x, y + 1, maxZ + 1, connected(Blocks.OAK_FENCE, true));
		}
		for (int z = minZ; z <= maxZ; z++) {
			c.set(minX - 1, y + 1, z, connected(Blocks.OAK_FENCE, false));
			c.set(maxX + 1, y + 1, z, connected(Blocks.OAK_FENCE, false));
		}
		c.set((minX + maxX) / 2, y + 1, maxZ + 1, Blocks.OAK_FENCE_GATE.defaultBlockState());
	}

	/** Stone fountain: round basin, raised bowl and a water spout. */
	public static void fountain(Canvas c, int cx, int cz, int y, int radius, Block rim) {
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dz = -radius; dz <= radius; dz++) {
				double d = Math.sqrt(dx * dx + dz * dz);
				if (d <= radius + 0.4) {
					c.set(cx + dx, y, cz + dz, Blocks.STONE_BRICKS.defaultBlockState());
					c.set(cx + dx, y + 1, cz + dz, d > radius - 0.8 ? rim.defaultBlockState() : Blocks.WATER.defaultBlockState());
				}
			}
		}
		c.fill(cx, y + 1, cz, cx, y + 3, cz, Blocks.STONE_BRICK_WALL.defaultBlockState());
		for (Direction d : Direction.Plane.HORIZONTAL) {
			c.set(cx + d.getStepX(), y + 4, cz + d.getStepZ(), rim.defaultBlockState());
		}
		c.set(cx, y + 4, cz, Blocks.WATER.defaultBlockState());
		c.set(cx, y + 5, cz, Blocks.SEA_LANTERN.defaultBlockState());
	}

	/** Village well: cobblestone ring, water, a little roof on posts. */
	public static void well(Canvas c, int x, int z, int y) {
		c.fill(x - 2, y, z - 2, x + 2, y + 1, z + 2, Blocks.COBBLESTONE.defaultBlockState());
		c.fill(x - 1, y - 4, z - 1, x + 1, y + 1, z + 1, Blocks.WATER.defaultBlockState());
		for (int dx : new int[]{-2, 2}) {
			for (int dz : new int[]{-2, 2}) {
				c.fill(x + dx, y + 2, z + dz, x + dx, y + 3, z + dz, Blocks.OAK_FENCE.defaultBlockState());
			}
		}
		c.fill(x - 2, y + 4, z - 2, x + 2, y + 4, z + 2, slab(Blocks.OAK_SLAB, false));
	}

	/** Covered market stall with a coloured awning and a counter. */
	public static void stall(Canvas c, int x, int z, int y, Block awning, Direction front) {
		c.foundation(x - 1, z - 1, x + 1, z + 1, y, Blocks.SPRUCE_PLANKS.defaultBlockState());
		for (int dx : new int[]{-1, 1}) {
			for (int dz : new int[]{-1, 1}) {
				c.fill(x + dx, y + 1, z + dz, x + dx, y + 3, z + dz, Blocks.SPRUCE_FENCE.defaultBlockState());
			}
		}
		c.fill(x - 1, y + 4, z - 1, x + 1, y + 4, z + 1, awning.defaultBlockState());
		c.set(x + front.getStepX(), y + 1, z + front.getStepZ(), Canvas.facing(Blocks.BARREL.defaultBlockState(), Direction.UP));
		c.set(x, y + 1, z, Blocks.SPRUCE_SLAB.defaultBlockState());
	}

	/** Small canvas tent (enemy camps). */
	public static void tent(Canvas c, int x, int z, int y, Block wool) {
		for (int i = 0; i <= 2; i++) {
			c.fill(x - 2 + i, y + 1 + i, z - 2, x - 2 + i, y + 1 + i, z + 2, wool.defaultBlockState());
			c.fill(x + 2 - i, y + 1 + i, z - 2, x + 2 - i, y + 1 + i, z + 2, wool.defaultBlockState());
		}
		c.set(x, y + 1, z + 3, Blocks.CAMPFIRE.defaultBlockState());
	}

	/** Buries a treasure chest (vanilla buried-treasure loot, which includes a Grand Line fruit) under the surface. */
	public static void treasure(Canvas c, int x, int z, long seed) {
		if (!c.inChunk(x, z)) {
			return;
		}
		int y = c.ground(x, z) - 2;
		c.set(x, y, z, Blocks.CHEST.defaultBlockState());
		net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity.setLootTable(c.level(),
				net.minecraft.util.RandomSource.create(seed ^ (x * 31L + z)), c.world(x, y, z),
				net.minecraft.world.level.storage.loot.BuiltInLootTables.BURIED_TREASURE);
	}
}
