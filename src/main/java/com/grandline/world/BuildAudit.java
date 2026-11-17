package com.grandline.world;

import com.grandline.world.plan.IslandPlans;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;

/**
 * Dev tool behind {@code /grandline audit <island>}: finds blocks on a built island that are floating or would break
 * the moment they are updated: anything vanilla says cannot survive where it is (ladders, torches, wall banners,
 * carpets, doors...), standing lanterns, pots and flags with nothing solid under them (or a half slab with a gap),
 * hanging lanterns and chains attached to nothing, and lone decorative blocks hanging in the air.
 */
public final class BuildAudit {
	private BuildAudit() {
	}

	public record Finding(BlockPos pos, String block, String problem) {
	}

	private static boolean fullTop(ServerLevel level, BlockPos below) {
		VoxelShape shape = level.getBlockState(below).getCollisionShape(level, below);
		return !shape.isEmpty() && shape.max(Direction.Axis.Y) >= 0.99;
	}

	private static boolean solidish(ServerLevel level, BlockPos p) {
		BlockState s = level.getBlockState(p);
		return !s.isAir() && !s.getCollisionShape(level, p).isEmpty() || s.is(Blocks.CHAIN) || s.getBlock() instanceof LanternBlock;
	}

	public static List<Finding> audit(ServerLevel level, Island island) {
		var plan = IslandPlans.get(island);
		int r = plan != null ? plan.extent() : island.radius();
		List<Finding> out = new ArrayList<>();
		BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
		for (int x = island.x() - r; x <= island.x() + r; x++) {
			for (int z = island.z() - r; z <= island.z() + r; z++) {
				if (!level.hasChunk(x >> 4, z >> 4)) {
					continue;
				}
				for (int y = 50; y < 200; y++) {
					BlockState s = level.getBlockState(m.set(x, y, z));
					if (s.isAir() || !s.getFluidState().isEmpty() && s.getBlock() == Blocks.WATER) {
						continue;
					}
					BlockPos p = m.immutable();
					Block b = s.getBlock();
					String id = BuiltInRegistries.BLOCK.getKey(b).getPath();
					if (!s.canSurvive(level, p)) {
						out.add(new Finding(p, id, "cannot survive here (pops off when updated)"));
						continue;
					}
					if (b instanceof LanternBlock) {
						boolean hanging = s.getValue(LanternBlock.HANGING);
						if (hanging ? !solidish(level, p.above()) : !solidish(level, p.below())) {
							out.add(new Finding(p, id, hanging ? "hanging from nothing" : "standing on nothing"));
						}
					} else if (b == Blocks.CHAIN) {
						if (!solidish(level, p.above()) && !solidish(level, p.below())
								&& s.getValue(net.minecraft.world.level.block.ChainBlock.AXIS) == Direction.Axis.Y) {
							out.add(new Finding(p, id, "chain attached to nothing"));
						}
					} else if (b == Blocks.DECORATED_POT || b == Blocks.FLOWER_POT || id.startsWith("potted_") || b == Blocks.CAMPFIRE
							|| id.endsWith("_pressure_plate")) {
						if (!fullTop(level, p.below())) {
							out.add(new Finding(p, id, "not resting on a full block (floats)"));
						}
					} else if (id.endsWith("_wool") || id.endsWith("_concrete") || id.endsWith("_terracotta")) {
						// decorative blocks with nothing on any side are floating
						int touching = 0;
						for (Direction d : Direction.values()) {
							if (!level.getBlockState(p.relative(d)).isAir()) {
								touching++;
							}
						}
						if (touching == 0) {
							out.add(new Finding(p, id, "floating on its own"));
						}
					}
				}
			}
		}
		return out;
	}
}
