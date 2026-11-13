package com.grandline.test;

import com.grandline.GrandLineMod;
import com.grandline.world.plan.TreeTestAccess;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Verdant Wilds giant trees: one connected piece each, persistent leaves, identical when assembled chunk by chunk. */
public final class GrandLineTreeGameTests implements FabricGameTest {
	private static long key(int x, int y, int z) {
		return ((long) x << 40) ^ ((long) y << 20) ^ (z & 0xFFFFFL);
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "trees_shape")
	public void everyGiantIsOneConnectedTreeWithPersistentLeaves(GameTestHelper helper) {
		List<TreeTestAccess.TreeBlocks> trees = TreeTestAccess.verdantGiants();
		helper.assertTrue(trees.size() >= 20, "Verdant Wilds has its giants: " + trees.size());
		Set<Integer> silhouettes = new HashSet<>();
		int leaves = 0;
		for (TreeTestAccess.TreeBlocks t : trees) {
			Map<Long, BlockState> blocks = new HashMap<>();
			for (int i = 0; i < t.positions().size(); i++) {
				int[] p = t.positions().get(i);
				blocks.put(key(p[0], p[1], p[2]), t.states().get(i));
			}
			// Flood fill from the trunk base through face-touching logs, leaves and vines.
			int minY = t.positions().stream().filter(p -> p[0] == t.x() && p[2] == t.z()).mapToInt(p -> p[1]).min().orElseThrow();
			ArrayDeque<int[]> queue = new ArrayDeque<>();
			Set<Long> seen = new HashSet<>();
			queue.add(new int[]{t.x(), minY, t.z()});
			seen.add(key(t.x(), minY, t.z()));
			int[][] dirs = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
			while (!queue.isEmpty()) {
				int[] p = queue.poll();
				for (int[] d : dirs) {
					long k = key(p[0] + d[0], p[1] + d[1], p[2] + d[2]);
					if (blocks.containsKey(k) && seen.add(k)) {
						queue.add(new int[]{p[0] + d[0], p[1] + d[1], p[2] + d[2]});
					}
				}
			}
			if (seen.size() != blocks.size()) {
				StringBuilder sample = new StringBuilder();
				for (int i = 0; i < t.positions().size() && sample.length() < 400; i++) {
					int[] p = t.positions().get(i);
					if (!seen.contains(key(p[0], p[1], p[2]))) {
						sample.append(" ").append(p[0] - t.x()).append(",").append(p[1] - minY).append(",").append(p[2] - t.z())
								.append("=").append(t.states().get(i).getBlock().getName().getString());
					}
				}
				helper.fail("tree at " + t.x() + "," + t.z() + ": " + (blocks.size() - seen.size()) + " blocks float free of the trunk:" + sample);
			}
			int width = 0;
			for (BlockState s : blocks.values()) {
				if (s.is(Blocks.JUNGLE_LEAVES)) {
					leaves++;
					helper.assertTrue(s.getValue(LeavesBlock.PERSISTENT), "leaves are persistent (never decay)");
				}
			}
			int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxY = 0;
			for (int[] p : t.positions()) {
				minX = Math.min(minX, p[0]);
				maxX = Math.max(maxX, p[0]);
				maxY = Math.max(maxY, p[1]);
			}
			width = maxX - minX;
			silhouettes.add(width * 1000 + (maxY - minY) * 10 + Math.floorMod(blocks.size(), 7));
		}
		GrandLineMod.LOGGER.info("Verdant giants: {} trees, {} leaf blocks, {} distinct silhouettes", trees.size(), leaves, silhouettes.size());
		helper.assertTrue(silhouettes.size() > trees.size() * 0.6, "crowns vary: " + silhouettes.size() + " shapes for " + trees.size() + " trees");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "trees_chunks", timeoutTicks = 200)
	public void giantAssembledChunkByChunkMatchesItsModel(GameTestHelper helper) {
		// The boss-clearing tree is the widest; place it where its crown crosses several chunk borders.
		TreeTestAccess.TreeBlocks tree = TreeTestAccess.verdantGiants().stream().filter(TreeTestAccess.TreeBlocks::grand).findFirst().orElseThrow();
		BlockPos at = helper.absolutePos(new BlockPos(0, 0, 0));
		int originX = (at.getX() & ~15) + 8 - tree.x(); // trunk 8 blocks into a chunk: crown spills into neighbours
		int originZ = (at.getZ() & ~15) + 8 - tree.z();
		TreeTestAccess.writeByChunks(helper.getLevel(), tree, originX, originZ);
		Set<Long> chunks = new HashSet<>();
		int wrong = 0;
		for (int i = 0; i < tree.positions().size(); i++) {
			int[] p = tree.positions().get(i);
			BlockPos pos = new BlockPos(originX + p[0], p[1], originZ + p[2]);
			chunks.add(((long) (pos.getX() >> 4) << 32) ^ (pos.getZ() >> 4));
			BlockState placed = helper.getLevel().getBlockState(pos), want = tree.states().get(i);
			// A live level re-computes leaf distance on neighbour changes (world generation does not); compare the rest.
			boolean same = placed.getBlock() == want.getBlock() && (!want.is(Blocks.JUNGLE_LEAVES) || placed.getValue(LeavesBlock.PERSISTENT));
			if (!same) {
				wrong++;
				if (wrong <= 6) {
					GrandLineMod.LOGGER.info("tree chunk test: at {} want {} got {}", pos, want, placed);
				}
			}
		}
		helper.assertTrue(chunks.size() >= 4, "the tree spans several chunks: " + chunks.size());
		helper.assertTrue(wrong == 0, wrong + " blocks differ from the model after chunk-by-chunk assembly");
		// Leaves stay put while the world ticks (no decay).
		helper.runAfterDelay(100, () -> {
			int gone = 0;
			for (int i = 0; i < tree.positions().size(); i++) {
				int[] p = tree.positions().get(i);
				BlockPos pos = new BlockPos(originX + p[0], p[1], originZ + p[2]);
				if (tree.states().get(i).is(Blocks.JUNGLE_LEAVES) && !helper.getLevel().getBlockState(pos).is(Blocks.JUNGLE_LEAVES)) {
					gone++;
				}
				helper.getLevel().setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
			}
			helper.assertTrue(gone == 0, gone + " leaves disappeared");
			helper.succeed();
		});
	}
}
