package com.grandline.world.plan;

import java.util.List;

/**
 * Ironhold Bastion's fixed population (layout 20 only; older worlds' original Ironhold keeps its old NPCs): seven
 * Marine Trainees (Lv 1-2) in the fortress courtyard, the elevated training field, and eight friendly NPCs.
 * Island-local coordinates (see {@link IronholdBastion}).
 */
public final class IronholdRoster {
	/** Spread across the courtyard inside the main gate, clear of the three great towers and the gate itself. */
	public static final List<int[]> TRAINEES = List.of(
			new int[]{-24, -5}, new int[]{-14, -8}, new int[]{-5, -12},
			new int[]{5, -12}, new int[]{14, -8}, new int[]{24, -5},
			new int[]{0, -18});

	/** Where the Marine Leader stands: on the terrace just outside the main gate, facing the staircase. */
	public static final int LEADER_X = 6, LEADER_Z = 3;

	public static final List<IslandRoster.Slot> SLOTS = List.of(
			new IslandRoster.Slot("QUEST_GIVER", LEADER_X, LEADER_Z, 0),        // Marine Leader, at the training field's gate
			new IslandRoster.Slot("FRUIT_DEALER", -10, 15, -90),                // the street at the foot of the staircase, west side
			new IslandRoster.Slot("SWORDSMITH", 17, 22, 90),                    // beside the east house, facing the plaza
			new IslandRoster.Slot("BOAT_DEALER", -14, 47, 0),                   // the waterfront promenade, west
			new IslandRoster.Slot("BOAT_DEALER", 14, 47, 0),                    // the waterfront promenade, east
			new IslandRoster.Slot("LUXURY_BOAT_DEALER", -4, 59, -90),           // the main dock's west prong
			new IslandRoster.Slot("PIRATE_RECRUITER", -10, 32, -90),            // the plaza's west side
			new IslandRoster.Slot("INNKEEPER", 4, 57, 90));                     // the main dock, beside the Marines' arrival point

	/** Seven Marine Trainees; the Marine Leader's quest asks for five. */
	public static final IslandRoster ROSTER = new IslandRoster(
			List.of(new IslandRoster.Group(com.grandline.entity.EnemyKind.MARINE_TRAINEE, "", 1, 2, IslandRoster.seconds(8), TRAINEES)),
			island -> SLOTS, List.of(new IslandRoster.Quest(com.grandline.entity.EnemyKind.MARINE_TRAINEE, 5, 1)));

	private IronholdRoster() {
	}
}
