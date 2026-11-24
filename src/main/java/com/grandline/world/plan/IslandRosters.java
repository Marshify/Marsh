package com.grandline.world.plan;

import com.grandline.entity.EnemyKind;

import java.util.List;

import static com.grandline.world.plan.IslandRoster.seconds;

/**
 * The fixed populations of Verdant Wilds, Saltmarket Docks and Sunscorch Dunes (their rebuilt layouts). Enemy kinds stay
 * the shared Bandit, Raider and Island Captain types; each island gives them its own skin (textures/entity) and its own
 * names (names.json, island_npcs). Island-local coordinates of each island's plan.
 */
final class IslandRosters {
	private IslandRosters() {
	}

	private static List<int[]> at(int... xz) {
		int[][] spots = new int[xz.length / 2][];
		for (int i = 0; i < spots.length; i++) {
			spots[i] = new int[]{xz[i * 2], xz[i * 2 + 1]};
		}
		return List.of(spots);
	}

	/**
	 * Verdant Wilds: seven Canopy Primates in the main island's forest round the temple, four Ironbacks on the west
	 * island, and Crownback in his own clearing on that island's north side (right of the bridge as you cross).
	 * Every spot is clear of the bridge ends by more than its leash.
	 */
	static final IslandRoster VERDANT = new IslandRoster(List.of(
			new IslandRoster.Group(EnemyKind.BANDIT, "canopy_primate", 10, 18, seconds(20),
					at(-30, -25, -38, -10, -30, 20, -20, 32, 30, 32, 35, 8, 25, -28)),
			new IslandRoster.Group(EnemyKind.RAIDER, "ironback", 17, 25, seconds(20),
					at(-124, -4, -114, 8, -100, 8, -120, 14)),
			new IslandRoster.Group(EnemyKind.CAPTAIN, "crownback", 28, 28, seconds(60), at(-100, -16))),
			island -> List.of(
					new IslandRoster.Slot("QUEST_GIVER", 7, 0, 0),             // the Adventurer, beside the temple staircase
					new IslandRoster.Slot("BOAT_DEALER", -4, 64, 58, -90)),    // on the main dock's head
			List.of(new IslandRoster.Quest(EnemyKind.BANDIT, 6, 10), new IslandRoster.Quest(EnemyKind.RAIDER, 8, 10),
					new IslandRoster.Quest(EnemyKind.CAPTAIN, 1, 10)));

	/**
	 * Saltmarket Docks: Redcoat Raiders in the west clearing behind the landward wall, Dock Enforcers in the east
	 * clearing, and the Galley Master at its far end, apart from both. The town's NPCs stand inside the walls.
	 */
	static final IslandRoster SALTMARKET = new IslandRoster(List.of(
			new IslandRoster.Group(EnemyKind.BANDIT, "redcoat_raider", 35, 35, seconds(15),
					at(-60, -88, -46, -84, -32, -86, -30, -100, -44, -108, -60, -106)),
			new IslandRoster.Group(EnemyKind.RAIDER, "dock_enforcer", 45, 45, seconds(15),
					at(24, -86, 36, -88, 50, -86, 58, -96, 28, -98, 40, -96)),
			new IslandRoster.Group(EnemyKind.CAPTAIN, "galley_master", 55, 55, seconds(300), at(46, -114))),
			island -> List.of(
					new IslandRoster.Slot("QUEST_GIVER", 5, 68, -31, 0),        // Pirate Adventurer, inside the landward gate
					new IslandRoster.Slot("DARK_STEP_TEACHER", -37, 68, -22, -90), // the quiet gap between the north-west houses
					new IslandRoster.Slot("BLACKSMITH", 36, 68, 14, 90),         // by the chimneyed workshop
					new IslandRoster.Slot("SWORDSMITH", -20, 68, 28, -90),       // Sword Dealer of the West, by the awning house
					new IslandRoster.Slot("PIRATE_RECRUITER", -16, 68, 18, -90), // the plaza's west side
					new IslandRoster.Slot("INNKEEPER", 6, 68, 44, 0),            // Set Home Point, inside the harbour gate
					new IslandRoster.Slot("BOAT_DEALER", 18, 65, 58, 0)),        // on the promenade
			List.of(new IslandRoster.Quest(EnemyKind.BANDIT, 8, 30), new IslandRoster.Quest(EnemyKind.RAIDER, 8, 40),
					new IslandRoster.Quest(EnemyKind.CAPTAIN, 1, 55)));

	/**
	 * Sunscorch Dunes: Dune Prowlers round the village's courtyards, Sandwatch Captains in the ruins between the two
	 * pyramids. No boss. Guy sells the Swordsman Hat in the Great Pyramid's altar chamber, out of every leash's reach.
	 */
	static final IslandRoster SUNSCORCH = new IslandRoster(List.of(
			new IslandRoster.Group(EnemyKind.BANDIT, "dune_prowler", 60, 60, seconds(15), at(-32, -8, -40, 16, -30, 32, 24, -6)),
			new IslandRoster.Group(EnemyKind.RAIDER, "sandwatch_captain", 75, 75, seconds(15), at(SunscorchDunes.RUINS_X - 6, SunscorchDunes.RUINS_Z + 3, SunscorchDunes.RUINS_X + 6, SunscorchDunes.RUINS_Z + 3,
					SunscorchDunes.RUINS_X - 4, SunscorchDunes.RUINS_Z - 4, SunscorchDunes.RUINS_X + 4, SunscorchDunes.RUINS_Z - 4))),
			island -> List.of(
					new IslandRoster.Slot("QUEST_GIVER", -15, 5, 90),            // Desert Adventurer, by the village courtyards
					new IslandRoster.Slot("BOAT_DEALER", 13, 50, 0),             // at the harbour
					new IslandRoster.Slot("INNKEEPER", 3, 46, 180),              // Set Home Point, at the arrival plaza
					new IslandRoster.Slot("ACCESSORY_DEALER", SunscorchDunes.GREAT_X - 4, SunscorchDunes.GREAT_BASE + 2,
							SunscorchDunes.GREAT_Z + 2, -90),                    // Guy, in the pyramid's altar chamber
					new IslandRoster.Slot("DESERT_MERCHANT", 8, 17, 90)),        // under the market stalls
			List.of(new IslandRoster.Quest(EnemyKind.BANDIT, 8, 60), new IslandRoster.Quest(EnemyKind.RAIDER, 6, 75)));

	/**
	 * Frostpeak Isle: Frost Rovers among the village's north-eastern houses, Rime Constructs in the frozen arena to the
	 * west, Glacier Maw alone on the northern ridge. The Ability Teacher keeps the ice cavern (no enemy reaches it).
	 */
	static final IslandRoster FROSTPEAK = new IslandRoster(List.of(
			new IslandRoster.Group(EnemyKind.BANDIT, "frost_rover", 90, 90, seconds(15),
					at(28, -30, 42, -26, 46, 1, 36, -8, 56, 4, 24, -14)),
			new IslandRoster.Group(EnemyKind.RAIDER, "rime_construct", 100, 100, seconds(15),
					at(FrostpeakIsle.ARENA_X - 14, FrostpeakIsle.ARENA_Z - 3, FrostpeakIsle.ARENA_X + 10, FrostpeakIsle.ARENA_Z - 7,
							FrostpeakIsle.ARENA_X, FrostpeakIsle.ARENA_Z - 17, FrostpeakIsle.ARENA_X, FrostpeakIsle.ARENA_Z + 17)),
			new IslandRoster.Group(EnemyKind.CAPTAIN, "glacier_maw", 110, 110, seconds(180), at(20, -46))),
			island -> List.of(
					new IslandRoster.Slot("QUEST_GIVER", 8, FrostpeakIsle.VILLAGE + 1, 12, 180),   // the Villager, at the top of the ladder
					new IslandRoster.Slot("BOAT_DEALER", -1, FrostpeakIsle.DOCK + 1, 48, 0),       // the main dock's landing
					new IslandRoster.Slot("SWORDSMITH", 2, FrostpeakIsle.VILLAGE + 1, -8, -90),    // Sword Dealer of the East, by the west tower
					new IslandRoster.Slot("INNKEEPER", 14, FrostpeakIsle.VILLAGE + 1, -2, 180),    // Set Home Point, at the village entrance
					new IslandRoster.Slot("FLASH_STEP_MASTER", FrostpeakIsle.ABILITY_MASTER_SPOT[0], FrostpeakIsle.ABILITY_MASTER_SPOT[1],
							FrostpeakIsle.ABILITY_MASTER_SPOT[2], FrostpeakIsle.ABILITY_MASTER_YAW)), // the Ability Teacher, in the ice cavern
			List.of(new IslandRoster.Quest(EnemyKind.BANDIT, 7, 90), new IslandRoster.Quest(EnemyKind.RAIDER, 8, 100),
					new IslandRoster.Quest(EnemyKind.CAPTAIN, 1, 105)));
}
