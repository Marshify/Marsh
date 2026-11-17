package com.grandline.world.plan;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.WallBannerBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A last pass over each island chunk once its plan has built: fixes decorations that a later piece of the build left
 * hanging on nothing (they look fine until a neighbouring block updates, then drop off). A ladder whose wall was cut
 * away (by a window, say) gets its backing back; a lantern with nothing to stand on or hang from is turned the other
 * way if that side holds, or else removed; a wall banner with no wall is removed. Only supports inside the same chunk
 * are judged, since the chunk next door may not be built yet.
 */
final class SupportFix {
	private SupportFix() {
	}

	static void run(WorldGenLevel level, int chunkMinX, int chunkMinZ, int minY, int maxY) {
		BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
		for (int x = chunkMinX; x < chunkMinX + 16; x++) {
			for (int z = chunkMinZ; z < chunkMinZ + 16; z++) {
				for (int y = minY; y <= maxY; y++) {
					BlockState s = level.getBlockState(m.set(x, y, z));
					if (s.isAir() || !(s.getBlock() instanceof LadderBlock || s.getBlock() instanceof LanternBlock || s.getBlock() instanceof WallBannerBlock)) {
						continue;
					}
					BlockPos p = m.immutable();
					if (s.canSurvive(level, p)) {
						continue;
					}
					if (s.getBlock() instanceof LadderBlock) {
						BlockPos back = p.relative(s.getValue(LadderBlock.FACING).getOpposite());
						if (inChunk(back, chunkMinX, chunkMinZ)) {
							level.setBlock(back, backing(level, back), 2);
						}
					} else if (s.getBlock() instanceof LanternBlock) {
						BlockPos support = s.getValue(LanternBlock.HANGING) ? p.above() : p.below();
						if (!inChunk(support, chunkMinX, chunkMinZ)) {
							continue;
						}
						BlockState flipped = s.setValue(LanternBlock.HANGING, !s.getValue(LanternBlock.HANGING));
						level.setBlock(p, flipped.canSurvive(level, p) ? flipped : Blocks.AIR.defaultBlockState(), 2);
					} else {
						BlockPos wall = p.relative(s.getValue(WallBannerBlock.FACING).getOpposite());
						if (inChunk(wall, chunkMinX, chunkMinZ)) {
							level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
						}
					}
				}
			}
		}
	}

	private static boolean inChunk(BlockPos p, int chunkMinX, int chunkMinZ) {
		return p.getX() >= chunkMinX && p.getX() < chunkMinX + 16 && p.getZ() >= chunkMinZ && p.getZ() < chunkMinZ + 16;
	}

	/** The wall material around a gap: the first full block above or below it, or beside it, else stone bricks. */
	private static BlockState backing(WorldGenLevel level, BlockPos p) {
		for (BlockPos q : new BlockPos[]{p.above(), p.below(), p.above(2), p.below(2)}) {
			BlockState b = level.getBlockState(q);
			if (b.isCollisionShapeFullBlock(level, q) && !b.hasBlockEntity()) {
				return b;
			}
		}
		for (Direction d : Direction.Plane.HORIZONTAL) {
			BlockState b = level.getBlockState(p.relative(d));
			if (b.isCollisionShapeFullBlock(level, p.relative(d)) && !b.hasBlockEntity()) {
				return b;
			}
		}
		return Blocks.STONE_BRICKS.defaultBlockState();
	}
}
