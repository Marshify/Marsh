package com.grandline.world.plan;

import java.util.List;

/**
 * One hand-designed island: its terrain, its structures and its gameplay anchors. Plans are deterministic and never
 * read the world, so the island is identical in every world and builds correctly in any chunk order. Add a new island
 * by writing a plan and registering it in {@link IslandPlans}; nothing else needs to change.
 */
public interface IslandPlan {
	/** Must match the island's name in {@link com.grandline.world.SeaLayout}. */
	String name();

	Terrain terrain();

	/** Places every structure, tree and decoration. Called once per chunk the island touches; writes are clipped. */
	void build(Canvas canvas);

	List<Anchor> anchors();

	/**
	 * A last pass over the chunk after the world's own decoration (ocean plants and the like) has run: lets a plan undo
	 * what that put where it must not be. Called once per chunk the island touches; writes are clipped.
	 */
	default void afterDecoration(Canvas canvas) {
	}

	/** Whether island-local (lx, lz) gets the island's biome (its land and beach; see IslandBiomes). */
	default boolean biomeFootprint(int lx, int lz) {
		return terrain().ground(lx, lz) >= Terrain.WATER_TOP;
	}

	/** Highest block any structure reaches above the island centre's ground (used only for documentation/tests). */
	default int extent() {
		return terrain().extent();
	}
}
