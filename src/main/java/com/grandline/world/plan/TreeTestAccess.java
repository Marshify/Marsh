package com.grandline.world.plan;

import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Test hooks for Verdant Wilds' giant trees (models are package-private). */
public final class TreeTestAccess {
	private TreeTestAccess() {
	}

	/** One giant tree's blocks: {x, y, z} positions (island-local) and states, plus its trunk origin. */
	public record TreeBlocks(int x, int z, boolean grand, List<int[]> positions, List<BlockState> states) {
	}

	public static List<TreeBlocks> verdantGiants() {
		VerdantWilds plan = new VerdantWilds();
		List<TreeBlocks> out = new ArrayList<>();
		for (VerdantWilds.Tree t : plan.trees()) {
			if (t.kind() != 0 && t.kind() != 3) {
				continue;
			}
			JungleTrees.Model m = plan.giantModel(t);
			List<int[]> pos = new ArrayList<>();
			List<BlockState> states = new ArrayList<>();
			for (Map.Entry<Long, BlockState> e : m.blocks.entrySet()) {
				long k = e.getKey();
				pos.add(new int[]{JungleTrees.Model.x(k), JungleTrees.Model.y(k), JungleTrees.Model.z(k)});
				states.add(e.getValue());
			}
			out.add(new TreeBlocks(t.x(), t.z(), t.kind() == 3, pos, states));
		}
		return out;
	}

	/** Writes the tree's blocks the way world generation does: one chunk at a time, clipped to that chunk. */
	public static void writeByChunks(WorldGenLevel level, TreeBlocks tree, int originX, int originZ) {
		JungleTrees.Model m = new JungleTrees.Model();
		for (int i = 0; i < tree.positions().size(); i++) {
			int[] p = tree.positions().get(i);
			m.put(p[0], p[1], p[2], tree.states().get(i));
		}
		Terrain terrain = new Terrain("tree test", 0);
		for (int cx = (originX + m.minX) >> 4; cx <= (originX + m.maxX) >> 4; cx++) {
			for (int cz = (originZ + m.minZ) >> 4; cz <= (originZ + m.maxZ) >> 4; cz++) {
				m.write(new Canvas(level, terrain, originX, originZ, cx << 4, cz << 4));
			}
		}
	}
}
