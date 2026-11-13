package com.grandline.world.plan;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Island-local block writer for one chunk. Coordinates are relative to the island centre; every write outside the
 * chunk being generated is dropped, so a structure spanning many chunks is assembled piece by piece without ever
 * reading or writing a neighbouring chunk.
 */
public final class Canvas {
	private final WorldGenLevel level;
	private final Terrain terrain;
	private final int originX;
	private final int originZ;
	private final int minX;
	private final int minZ;
	private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

	public Canvas(WorldGenLevel level, Terrain terrain, int originX, int originZ, int chunkMinX, int chunkMinZ) {
		this.level = level;
		this.terrain = terrain;
		this.originX = originX;
		this.originZ = originZ;
		this.minX = chunkMinX;
		this.minZ = chunkMinZ;
	}

	public Terrain terrain() {
		return terrain;
	}

	public int ground(int x, int z) {
		return terrain.ground(x, z);
	}

	public WorldGenLevel level() {
		return level;
	}

	/** Whether island-local (x, z) is inside the chunk being generated. */
	public boolean inChunk(int x, int z) {
		int wx = originX + x;
		int wz = originZ + z;
		return wx >= minX && wx < minX + 16 && wz >= minZ && wz < minZ + 16;
	}

	/** Whether anything within {@code r} blocks of island-local (x, z) can land in this chunk (skip far-away pieces). */
	public boolean near(int x, int z, int r) {
		int wx = originX + x;
		int wz = originZ + z;
		return wx + r >= minX && wx - r < minX + 16 && wz + r >= minZ && wz - r < minZ + 16;
	}

	/** The chunk being generated, full height. */
	public net.minecraft.world.level.levelgen.structure.BoundingBox chunkBox() {
		return new net.minecraft.world.level.levelgen.structure.BoundingBox(minX, level.getMinBuildHeight(), minZ, minX + 15,
				level.getMaxBuildHeight() - 1, minZ + 15);
	}

	public BlockPos world(int x, int y, int z) {
		return new BlockPos(originX + x, y, originZ + z);
	}

	public void set(int x, int y, int z, BlockState state) {
		if (!inChunk(x, z) || y < level.getMinBuildHeight() || y >= level.getMaxBuildHeight()) {
			return;
		}
		level.setBlock(pos.set(originX + x, y, originZ + z), state, 2);
	}

	/** Asks the world to update the water at (x, y, z) once the chunk is live, so placed water starts flowing. */
	public void tickFluid(int x, int y, int z) {
		if (inChunk(x, z)) {
			level.scheduleTick(new BlockPos(originX + x, y, originZ + z), net.minecraft.world.level.material.Fluids.WATER, 1);
		}
	}

	public void fill(int x1, int y1, int z1, int x2, int y2, int z2, BlockState state) {
		for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
			for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) {
				if (!inChunk(x, z)) {
					continue;
				}
				for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++) {
					set(x, y, z, state);
				}
			}
		}
	}

	/** Solid foundation from below the ground up to {@code topY} under a footprint, so buildings never float. */
	public void foundation(int x1, int z1, int x2, int z2, int topY, BlockState state) {
		for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
			for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) {
				if (!inChunk(x, z)) {
					continue;
				}
				int from = Math.min(ground(x, z), topY) - 3;
				for (int y = from; y <= topY; y++) {
					set(x, y, z, state);
				}
			}
		}
	}

	public static BlockState facing(BlockState state, Direction direction) {
		return state.hasProperty(BlockStateProperties.HORIZONTAL_FACING) ? state.setValue(BlockStateProperties.HORIZONTAL_FACING, direction) : state;
	}
}
