package com.grandline.world.plan;

import com.grandline.world.Island;

import java.util.ArrayList;
import java.util.List;

/**
 * Millbrook Village's fixed population (the rebuilt island only): where each friendly NPC stands and each Bandit
 * spawns, in island-local coordinates. Read by {@link com.grandline.npc.MillbrookNpcs}, which keeps the island to
 * exactly this roster in new and existing worlds.
 */
public final class MillbrookRoster {
	/** A friendly NPC's spot: role name (Islander.Role), x, z, yaw (0 south, 90 west, 180 north, -90 east). */
	public record Slot(String role, int x, int z, float yaw) {
	}

	/** A Bandit's spawn spot and its combat field (0: the training meadow, 1: the bandit camp). */
	public record Spot(int x, int z, int field) {
	}

	/** Five Bandits per field, spread out, clear of the dummies, boulders, tent, fire and wagon. */
	public static final List<Spot> BANDITS = List.of(
			new Spot(41, -1, 0), new Spot(46, -6, 0), new Spot(51, -1, 0), new Spot(47, 4, 0), new Spot(42, 5, 0),
			new Spot(42, 29, 1), new Spot(46, 23, 1), new Spot(47, 29, 1), new Spot(52, 33, 1), new Spot(42, 34, 1));

	private MillbrookRoster() {
	}

	/** Whether {@code island} is the rebuilt Millbrook Village (older worlds' original Millbrook keeps its old NPCs). */
	public static boolean applies(Island island) {
		return island != null && IslandPlans.get(island) instanceof MillbrookVillage;
	}

	/** The eight friendly NPCs. The landings' Boat Dealers stand on the shore beside each jetty. */
	public static List<Slot> slots(Island island) {
		IslandPlan plan = IslandPlans.get(island);
		List<Slot> slots = new ArrayList<>();
		slots.add(new Slot("QUEST_GIVER", MillbrookVillage.QUEST_X, MillbrookVillage.QUEST_Z, 90));   // where the field roads fork
		slots.add(new Slot("LUXURY_BOAT_DEALER", MillbrookVillage.DOCK_X - 4, 48, -90));               // main dock plaza, west side
		slots.add(new Slot("INNKEEPER", MillbrookVillage.DOCK_X + 4, 48, 90));                         // by the arrival point (spawn)
		slots.add(new Slot("MARINE_RECRUITER", MillbrookVillage.LAWN_X + 5, MillbrookVillage.LAWN_Z, -90)); // village green
		slots.add(new Slot("SWORDSMITH", 14, 16, 180));                                                  // outside the smithy
		// West landing (jetty runs west from the shore at z 4) and north-east landing (runs north at x 40).
		int wx = shoreX(plan, -64, MillbrookVillage.WEST_LANDING_Z);
		slots.add(new Slot("BOAT_DEALER", wx + 1, MillbrookVillage.WEST_LANDING_Z - 3, 90));
		int nz = shoreZ(plan, MillbrookVillage.NE_LANDING_X, -45);
		slots.add(new Slot("BOAT_DEALER", MillbrookVillage.NE_LANDING_X - 3, nz + 1, 180));
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
