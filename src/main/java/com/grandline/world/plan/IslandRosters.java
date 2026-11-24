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

	/** Spots at an exact standing height (floating islands, where the island's ground map is the sea floor). */
	private static List<int[]> atY(int y, int... xz) {
		int[][] spots = new int[xz.length / 2][];
		for (int i = 0; i < spots.length; i++) {
			spots[i] = new int[]{xz[i * 2], y, xz[i * 2 + 1]};
		}
		return List.of(spots);
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

	/**
	 * Redbrick Port: Harbor Sentinels in two groups of four on the grass behind the crescent wall, west and east of the
	 * headquarters; the Bastion Commander in the headquarters' hall. The Quake Monarch is a raid boss (RaidEvents), not
	 * part of this population. Parlus sells the Black Cape in the south-west watchtower's chamber.
	 */
	static final IslandRoster REDBRICK = new IslandRoster(List.of(
			new IslandRoster.Group(EnemyKind.BANDIT, "harbor_sentinel", 120, 120, seconds(15),
					at(-50, -38, -34, -52, -22, -56, -40, -58, 46, -44, 34, -52, 22, -56, 40, -58)),
			new IslandRoster.Group(EnemyKind.CAPTAIN, "bastion_commander", 130, 130, seconds(300), at(0, 0))),
			island -> List.of(
					new IslandRoster.Slot("QUEST_GIVER", -5, RedbrickPort.G + 1, 24, -90),   // Marine Officer, left of the path to the HQ
					new IslandRoster.Slot("GUNSMITH", 24, RedbrickPort.G + 1, 20, 90),       // Advanced Weapon Dealer, by the south-east hall
					new IslandRoster.Slot("ACCESSORY_DEALER", -58, 83, 32, 0)),             // Parlus, in the south-west watchtower's chamber
			List.of(new IslandRoster.Quest(EnemyKind.BANDIT, 8, 120), new IslandRoster.Quest(EnemyKind.CAPTAIN, 1, 130)));

	/**
	 * Cloudtop Isles, lower region only (the upper region keeps its own population): Cloudstep Raiders round the temple on
	 * Azure Rise, Duskwardens on Skyreach Citadel's grounds. No boss. NPCs on Cloudfoot Cove and in Bloomhold village.
	 */
	static final IslandRoster CLOUDTOP = new IslandRoster(List.of(
			new IslandRoster.Group(EnemyKind.BANDIT, "cloudstep_raider", 150, 150, seconds(15), atY(CloudtopIsles.T_Y + 1, 18, -72, 56, -62, 36, -88, 20, -48)),
			new IslandRoster.Group(EnemyKind.RAIDER, "duskwarden", 175, 175, seconds(15), atY(CloudtopIsles.K_Y + 1, -170, -16, -131, -11, -185, 35, -115, 35))),
			island -> List.of(
					new IslandRoster.Slot("MAD_SCIENTIST", -18, 24, 180),                        // Cloudfoot Cove, by the rocks at the back
					new IslandRoster.Slot("BOAT_DEALER", -4, 44, 0),                             // at the wharf
					new IslandRoster.Slot("LUXURY_BOAT_DEALER", 4, 44, 0),                       // at the wharf
					new IslandRoster.Slot("FISHERMAN", 22, 14, -90),                             // the Angler, by the pool under the falls
					new IslandRoster.Slot("QUEST_GIVER", CloudtopIsles.V_X + 4, CloudtopIsles.V_Y + 1, CloudtopIsles.V_Z + 6, 180), // Sky Adventurer
					new IslandRoster.Slot("SWORDSMITH", CloudtopIsles.V_X + 14, CloudtopIsles.V_Y + 1, CloudtopIsles.V_Z + 10, 90), // Master Sword Dealer
					new IslandRoster.Slot("ACCESSORY_DEALER", -37, CloudtopIsles.V_Y + 1, -52, 0),   // Yoshi, outside the pink house
					new IslandRoster.Slot("INNKEEPER", CloudtopIsles.V_X - 5, CloudtopIsles.V_Y + 1, CloudtopIsles.V_Z + 4, 90)), // Set Home Point
			List.of(new IslandRoster.Quest(EnemyKind.BANDIT, 7, 150), new IslandRoster.Quest(EnemyKind.RAIDER, 8, 175)));

	/**
	 * Stonewall Prison: Stonewall Inmates in the west of the outer enclosure, High Security Inmates in the east; the Iron
	 * Jailer and the Emerald Overseer in the north-west and north-east of the central courtyard; the Rose Baron inside
	 * the central tower. The Jail Keeper gives the inmate quests, the Head Jailer the boss quests.
	 */
	static final IslandRoster STONEWALL = new IslandRoster(List.of(
			new IslandRoster.Group(EnemyKind.BANDIT, "stonewall_inmate", 190, 190, 9 * 20, at(-36, -6, -38, 6, -32, 16, -30, -16, -22, -26)),
			new IslandRoster.Group(EnemyKind.RAIDER, "high_security_inmate", 210, 210, 12 * 20, at(36, -6, 38, 6, 32, 16, 30, -16, 22, -26)),
			new IslandRoster.Group(EnemyKind.CAPTAIN, "iron_jailer", 220, 220, seconds(240), at(-10, -15)),
			new IslandRoster.Group(EnemyKind.CAPTAIN, "emerald_overseer", 230, 230, seconds(600), at(10, -15)),
			new IslandRoster.Group(EnemyKind.CAPTAIN, "rose_baron", 240, 240, seconds(1800), at(0, 0))),
			island -> List.of(
					new IslandRoster.Slot("QUEST_GIVER", 12, StonewallPrison.G + 1, 36, 0),          // Jail Keeper, inside the gate
					new IslandRoster.Slot("BOSS_QUEST_GIVER", -4, StonewallPrison.G + 1, 14, 0),     // Head Jailer, left of the tower's door
					new IslandRoster.Slot("MILITARY_DETECTIVE", 5, 53, 180),                         // by the main entrance
					new IslandRoster.Slot("FRUIT_REMOVER", -25, StonewallPrison.G + 1, 30, 90),      // a quiet corner of the south-west yard
					new IslandRoster.Slot("BOAT_DEALER", -4, 65, 56, 0)),                            // the main dock's landing
			List.of(new IslandRoster.Quest(EnemyKind.BANDIT, 8, 190), new IslandRoster.Quest(EnemyKind.RAIDER, 8, 210),
					new IslandRoster.Quest(EnemyKind.CAPTAIN, 1, 220, "iron_jailer"),
					new IslandRoster.Quest(EnemyKind.CAPTAIN, 1, 230, "emerald_overseer"),
					new IslandRoster.Quest(EnemyKind.CAPTAIN, 1, 240, "rose_baron")));

	/**
	 * Gladiator's Ring: Arena Vanguards on the west warrior grounds (left, coming from the dock), Gilt Champions on the
	 * east training grounds. The arena itself stays free of enemies; no boss.
	 */
	static final IslandRoster GLADIATOR = new IslandRoster(List.of(
			new IslandRoster.Group(EnemyKind.BANDIT, "arena_vanguard", 250, 250, seconds(20),
					at(GladiatorsRing.RUINS_X - 6, GladiatorsRing.RUINS_Z - 6, GladiatorsRing.RUINS_X + 6, GladiatorsRing.RUINS_Z - 6,
							GladiatorsRing.RUINS_X, GladiatorsRing.RUINS_Z + 6, GladiatorsRing.RUINS_X - 8, GladiatorsRing.RUINS_Z + 8,
							GladiatorsRing.RUINS_X + 8, GladiatorsRing.RUINS_Z + 8)),
			new IslandRoster.Group(EnemyKind.RAIDER, "gilt_champion", 275, 275, seconds(20),
					at(GladiatorsRing.TRAIN_X - 6, GladiatorsRing.TRAIN_Z - 6, GladiatorsRing.TRAIN_X + 6, GladiatorsRing.TRAIN_Z - 6,
							GladiatorsRing.TRAIN_X, GladiatorsRing.TRAIN_Z + 6, GladiatorsRing.TRAIN_X - 8, GladiatorsRing.TRAIN_Z + 8,
							GladiatorsRing.TRAIN_X + 8, GladiatorsRing.TRAIN_Z + 8))),
			island -> List.of(
					new IslandRoster.Slot("QUEST_GIVER", GladiatorsRing.QUEST_X, GladiatorsRing.G + 1, GladiatorsRing.QUEST_Z, 90), // by the gate pillars
					new IslandRoster.Slot("BOAT_DEALER", -6, 66, 62, 0),             // the landing, west
					new IslandRoster.Slot("LUXURY_BOAT_DEALER", 6, 66, 62, 0),       // the landing, east
					new IslandRoster.Slot("INNKEEPER", 8, GladiatorsRing.G + 1, 55, 90)), // Set Home Point, on the entrance path
			List.of(new IslandRoster.Quest(EnemyKind.BANDIT, 7, 250), new IslandRoster.Quest(EnemyKind.RAIDER, 8, 275)));

	/**
	 * Emberfall Isle: Ashguard Troopers (swords, half with Aura) on the training ground between the village and the
	 * volcano, Cinder Infiltrators (Aura, Flash Step) on the plateau behind it, the Emberfall Marshal (magma moves) on the
	 * platform in the volcano's chamber. The Living Skeleton sells the Soul Cane in the hidden mine's corridor.
	 */
	static final IslandRoster EMBERFALL = new IslandRoster(List.of(
			new IslandRoster.Group(EnemyKind.BANDIT, "ashguard_trooper", 300, 300, seconds(20),
					at(EmberfallIsle.TRAIN_X, EmberfallIsle.TRAIN_Z - 6, EmberfallIsle.TRAIN_X + 6, EmberfallIsle.TRAIN_Z - 6,
							EmberfallIsle.TRAIN_X, EmberfallIsle.TRAIN_Z + 2, EmberfallIsle.TRAIN_X - 6, EmberfallIsle.TRAIN_Z + 6,
							EmberfallIsle.TRAIN_X + 6, EmberfallIsle.TRAIN_Z + 6)),
			new IslandRoster.Group(EnemyKind.RAIDER, "cinder_infiltrator", 325, 325, seconds(25),
					atY(EmberfallIsle.PLATEAU_Y + 1, -10, -70, 10, -70, -14, -80, 8, -80)),
			new IslandRoster.Group(EnemyKind.CAPTAIN, "emberfall_marshal", 350, 350, seconds(480),
					atY(EmberfallIsle.CHAMBER_Y + 1, EmberfallIsle.VX, EmberfallIsle.VZ))),
			island -> List.of(
					new IslandRoster.Slot("QUEST_GIVER", EmberfallIsle.QUEST_X, EmberfallIsle.G + 1, EmberfallIsle.QUEST_Z, 180), // the Mayor
					new IslandRoster.Slot("FLASH_STEP_MASTER", 8, EmberfallIsle.G + 1, 52, 180),   // the Ability Teacher, on the village square
					new IslandRoster.Slot("SWORDSMITH", -46, EmberfallIsle.G + 1, -74, -90),       // the Living Skeleton, in the hidden mine
					new IslandRoster.Slot("INNKEEPER", 6, EmberfallIsle.G + 1, 68, 90)),           // Set Home Point, by the dock road
			List.of(new IslandRoster.Quest(EnemyKind.BANDIT, 7, 300), new IslandRoster.Quest(EnemyKind.RAIDER, 8, 325),
					new IslandRoster.Quest(EnemyKind.CAPTAIN, 1, 350)));

	/**
	 * Lotus Palace: in the sunken city's dry air pocket, Tideborn Bladesmen on the west side, Abyssal Guards on the east,
	 * the Deepcourt Sovereign before the throne. King Neptune and the Water Kung Fu Teacher below; the surface islets keep
	 * the Fisherman and the Set Home Point.
	 */
	static final IslandRoster LOTUS = new IslandRoster(List.of(
			new IslandRoster.Group(EnemyKind.BANDIT, "tideborn_bladesman", 375, 375, seconds(30),
					atY(LotusPalace.G + 1, -56, -102, -48, -94, -62, -90, -50, -112, -66, -104, -58, -80, -42, -104)),
			new IslandRoster.Group(EnemyKind.RAIDER, "abyssal_guard", 400, 400, seconds(20),
					atY(LotusPalace.G + 1, 56, -102, 48, -94, 62, -90, 50, -112, 66, -104, 42, -104)),
			new IslandRoster.Group(EnemyKind.CAPTAIN, "deepcourt_sovereign", 425, 425, seconds(480),
					atY(LotusPalace.FLOOR + 3, 0, LotusPalace.PZ - 13))),
			island -> List.of(
					new IslandRoster.Slot("QUEST_GIVER", -30, LotusPalace.G + 1, -90, 90),         // King Neptune, before the west corals
					new IslandRoster.Slot("WATER_KUNG_FU_TEACHER", 25, LotusPalace.G + 1, LotusPalace.PZ - 43, 180), // the rear-right tower
					new IslandRoster.Slot("FISHERMAN", 0, LotusPalace.SEA + 1, -12, 0),             // surface: the islet's jetty
					new IslandRoster.Slot("INNKEEPER", -4, -27, 0)),                                // surface: Set Home Point by the arrival
			List.of(new IslandRoster.Quest(EnemyKind.BANDIT, 8, 375), new IslandRoster.Quest(EnemyKind.RAIDER, 7, 400),
					new IslandRoster.Quest(EnemyKind.CAPTAIN, 1, 425)));

	/**
	 * Crystal Fountain: Steelcrest Raiders on the outer level by the south gate, Steelcrest Captains (Aura) in the north
	 * of the inner ring, Ironvolt on the central court by the fountain.
	 */
	static final IslandRoster CRYSTAL = new IslandRoster(List.of(
			new IslandRoster.Group(EnemyKind.BANDIT, "steelcrest_raider", 625, 625, 16 * 20,
					atY(CrystalFountain.G_OUT + 1, -11, 126, 4, 139, -20, 130, 0, 130, -4, 122, -19, 122)),
			new IslandRoster.Group(EnemyKind.RAIDER, "steelcrest_captain", 650, 650, seconds(20),
					atY(CrystalFountain.G_IN + 1, 0, -49, -10, -50, 10, -48, 0, -58, -7, -41, -22, -47, 22, -47, -9, -62)),
			new IslandRoster.Group(EnemyKind.CAPTAIN, "ironvolt", 675, 675, seconds(720), atY(CrystalFountain.G_CT + 1, 0, -30))),
			island -> List.of(
					new IslandRoster.Slot("QUEST_GIVER", -8, CrystalFountain.G_OUT + 1, 140, 0),    // City Quest Giver, left of the gate
					new IslandRoster.Slot("BOAT_DEALER", -20, CrystalFountain.PLAZA_Y + 1, 180, 0),  // the harbour plaza
					new IslandRoster.Slot("INNKEEPER", 20, CrystalFountain.PLAZA_Y + 1, 170, 0)),    // Set Home Point, before the gate
			List.of(new IslandRoster.Quest(EnemyKind.BANDIT, 8, 625), new IslandRoster.Quest(EnemyKind.RAIDER, 9, 650),
					new IslandRoster.Quest(EnemyKind.CAPTAIN, 1, 675)));

	/** Dawnreach Harbor: the First Sea's hub, a safe zone with no enemies; every service at its own spot. */
	static final IslandRoster DAWNREACH = new IslandRoster(List.of(),
			island -> List.of(
					new IslandRoster.Slot("FRUIT_DEALER", -7 + DawnreachHarbor.OX, DawnreachHarbor.WALK, -26, 0),
					new IslandRoster.Slot("GACHA", DawnreachHarbor.CART_X + DawnreachHarbor.OX, DawnreachHarbor.WALK, DawnreachHarbor.CART_Z, 90),
					new IslandRoster.Slot("GUNSMITH", -9 + DawnreachHarbor.OX, DawnreachHarbor.WALK, -7, -45),    // Weapon Dealer
					new IslandRoster.Slot("SWORDSMITH", -42, 26, -90),                                             // by the south-west shops
					new IslandRoster.Slot("NAVIGATOR", -46 + DawnreachHarbor.OX, DawnreachHarbor.WALK, -4, 90),  // Experienced Captain
					new IslandRoster.Slot("BOAT_DEALER", -62, 10, 90),                                             // the west piers
					new IslandRoster.Slot("LUXURY_BOAT_DEALER", 62, -10, -90),                                     // the east piers
					new IslandRoster.Slot("INNKEEPER", -10, 10, 180),                                              // Set Home Point, by the fountain
					new IslandRoster.Slot("AURA_EDITOR", 40, -30, 180),                                            // among the north-east houses
					new IslandRoster.Slot("DARK_BLADE_SMITH", -9, -12, 180),                                       // at the cyan-roofed house
					new IslandRoster.Slot("RACE_MYSTIC", -30, 12, 180)),                                           // kept: race evolution
			List.of());
}
