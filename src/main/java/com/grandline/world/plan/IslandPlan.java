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

	/** Highest block any structure reaches above the island centre's ground (used only for documentation/tests). */
	default int extent() {
		return terrain().extent();
	}
}
