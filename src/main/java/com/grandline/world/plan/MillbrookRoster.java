package com.grandline.world.plan;

import com.grandline.world.Island;

import java.util.ArrayList;
import java.util.List;

/**
 * Millbrook Village's fixed population (the rebuilt island only): where each friendly NPC stands and each Bandit
 * spawns, in island-local coordinates. Read by {@link com.grandline.npc.RosterNpcs}, which keeps the island to
 * exactly this roster in new and existing worlds.
 */
public final class MillbrookRoster {
	/** Five Bandits per field (spots 0-4 the training meadow, 5-9 the bandit camp), clear of the props. */
	public static final List<int[]> BANDITS = List.of(
			new int[]{41, -1}, new int[]{46, -6}, new int[]{51, -1}, new int[]{47, 4}, new int[]{42, 5},
			new int[]{42, 29}, new int[]{46, 23}, new int[]{47, 29}, new int[]{52, 33}, new int[]{42, 34});

	/** Ten Bandits (Lv 1-2) in two fields, back 8 s after they fall; the quest asks for five. */
	public static final IslandRoster ROSTER = new IslandRoster(
			List.of(new IslandRoster.Group(com.grandline.entity.EnemyKind.BANDIT, "", 1, 2, IslandRoster.seconds(8), BANDITS)),
			MillbrookRoster::slots, List.of(new IslandRoster.Quest(com.grandline.entity.EnemyKind.BANDIT, 5, 1)));

	private MillbrookRoster() {
	}

	/** The eight friendly NPCs. The landings' Boat Dealers stand on the shore beside each jetty. */
	public static List<IslandRoster.Slot> slots(Island island) {
		IslandPlan plan = IslandPlans.get(island);
		List<IslandRoster.Slot> slots = new ArrayList<>();
		slots.add(new IslandRoster.Slot("QUEST_GIVER", MillbrookVillage.QUEST_X, MillbrookVillage.QUEST_Z, 90));   // where the field roads fork
		slots.add(new IslandRoster.Slot("LUXURY_BOAT_DEALER", MillbrookVillage.DOCK_X - 4, 48, -90));               // main dock plaza, west side
		slots.add(new IslandRoster.Slot("INNKEEPER", MillbrookVillage.DOCK_X + 4, 48, 90));                         // by the arrival point (spawn)
		slots.add(new IslandRoster.Slot("MARINE_RECRUITER", MillbrookVillage.LAWN_X + 5, MillbrookVillage.LAWN_Z, -90)); // village green
		slots.add(new IslandRoster.Slot("SWORDSMITH", 14, 16, 180));                                                  // outside the smithy
		// West landing (jetty runs west from the shore at z 4) and north-east landing (runs north at x 40).
		int wx = shoreX(plan, -64, MillbrookVillage.WEST_LANDING_Z);
		slots.add(new IslandRoster.Slot("BOAT_DEALER", wx + 1, MillbrookVillage.WEST_LANDING_Z - 3, 90));
		int nz = shoreZ(plan, MillbrookVillage.NE_LANDING_X, -45);
		slots.add(new IslandRoster.Slot("BOAT_DEALER", MillbrookVillage.NE_LANDING_X - 3, nz + 1, 180));
		return slots;
	}

	/** First land column walking east from (x, z) (the west jetty's shore end). */
	private static int shoreX(IslandPlan plan, int x, int z) {
		for (int i = 0; i < 12 && plan.terrain().ground(x, z) <= Terrain.WATER_TOP; i++) {
			x++;
		}
		return x;
	}

	/** First land row walking south from (x, z) (the north-east jetty's shore end). */
	private static int shoreZ(IslandPlan plan, int x, int z) {
		for (int i = 0; i < 12 && plan.terrain().ground(x, z) <= Terrain.WATER_TOP; i++) {
			z++;
		}
		return z;
	}
}
