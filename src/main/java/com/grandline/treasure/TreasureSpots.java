package com.grandline.treasure;

import net.minecraft.core.Direction;

import java.util.List;
import java.util.Map;

/**
 * Where every treasure chest is, island by island (island-local coordinates of the current island plans). Each spot was
 * picked from the generated island (see docs/TREASURE.md and {@code /grandline treasure scan}) and checked in game.
 */
public final class TreasureSpots {
	private static final Map<String, List<TreasureSpot>> SPOTS = Map.ofEntries(
			Map.entry("Cloudtop Isles", List.of(
				new TreasureSpot(TreasureTier.LEGENDARY, 96, 240, -199, Direction.SOUTH, "Grand Sanctuary, on the temple's summit platform beside the pavilion"),
				new TreasureSpot(TreasureTier.RARE, 58, 205, -208, Direction.EAST, "Grand Sanctuary, by the leaning ruined arch"),
				new TreasureSpot(TreasureTier.COMMON, -52, 201, -134, Direction.WEST, "Ancient Tree landmass, at the edge of the combat field"),
				new TreasureSpot(TreasureTier.COMMON, 24, 99, 2, Direction.SOUTH, "Mistfall, on the grass by the trees"),
				new TreasureSpot(TreasureTier.COMMON, -30, 101, -36, Direction.NORTH, "Bloomhold, between the towers off the east path"))),
			Map.entry("Crystal Fountain", List.of(
				new TreasureSpot(TreasureTier.LEGENDARY, 206, 67, -28, Direction.WEST, "scrapyard, by the foundry engine and the boss yard"),
				new TreasureSpot(TreasureTier.RARE, 22, 77, 22, Direction.NORTH, "the landmark's courtyard, inside the enclosure"),
				new TreasureSpot(TreasureTier.COMMON, -67, 73, -58, Direction.NORTH, "north-west fountain plaza"),
				new TreasureSpot(TreasureTier.COMMON, 30, 66, 190, Direction.SOUTH, "harbour plaza, by the east pier"))),
			Map.entry("Dawnreach Harbor", List.of(
				new TreasureSpot(TreasureTier.RARE, 24, 68, -12, Direction.SOUTH, "inside a market hall, by crates"),
				new TreasureSpot(TreasureTier.COMMON, 22, 68, 10, Direction.NORTH, "by crates in a market hall"),
				new TreasureSpot(TreasureTier.COMMON, -62, 68, 32, Direction.WEST, "by crates near the west harbour"))),
			Map.entry("Emberfall Isle", List.of(
				new TreasureSpot(TreasureTier.LEGENDARY, -4, 105, -33, Direction.WEST, "raised 39, walls es"),
				new TreasureSpot(TreasureTier.RARE, -33, 79, -77, Direction.EAST, "by crates, indoors, raised 13, walls sw"),
				new TreasureSpot(TreasureTier.COMMON, -25, 67, 59, Direction.WEST, "by crates, walls e"),
				new TreasureSpot(TreasureTier.COMMON, -6, 65, 96, Direction.SOUTH, "by crates, walls n"))),
			Map.entry("Gladiator's Ring", List.of(
				new TreasureSpot(TreasureTier.LEGENDARY, -39, 88, -25, Direction.EAST, "raised 21, walls w"),
				new TreasureSpot(TreasureTier.RARE, 40, 66, -45, Direction.EAST, "by crates, indoors, walls w"),
				new TreasureSpot(TreasureTier.COMMON, 23, 67, 55, Direction.EAST, "by crates, walls w"),
				new TreasureSpot(TreasureTier.COMMON, -62, 68, -22, Direction.WEST, "by crates, walls e"))),
			Map.entry("Lotus Palace", List.of(
				new TreasureSpot(TreasureTier.LEGENDARY, 22, 12, -134, Direction.WEST, "in the palace hall, north-east, behind the column rows"),
				new TreasureSpot(TreasureTier.RARE, -31, 12, -58, Direction.EAST, "inside the tall shell house by the south-west path"),
				new TreasureSpot(TreasureTier.COMMON, 4, 66, -24, Direction.SOUTH, "on the north islet, by the landing"),
				new TreasureSpot(TreasureTier.COMMON, 30, 66, 22, Direction.NORTH, "on the south-east islet, among the palms"))),
			Map.entry("Millbrook Village", List.of(
				new TreasureSpot(TreasureTier.LEGENDARY, 54, 67, 18, Direction.WEST, "at the bandit boss camp, guarded"),
				new TreasureSpot(TreasureTier.RARE, 17, 67, -20, Direction.EAST, "by crates, indoors, walls w"),
				new TreasureSpot(TreasureTier.COMMON, -18, 67, 28, Direction.SOUTH, "by crates, walls n"),
				new TreasureSpot(TreasureTier.COMMON, 6, 67, 26, Direction.SOUTH, "by crates, walls n"))),
			Map.entry("Redbrick Port", List.of(
				new TreasureSpot(TreasureTier.LEGENDARY, 3, 79, 2, Direction.WEST, "in the headquarters' crown room, up the hall ladder and across the artillery platform"),
				new TreasureSpot(TreasureTier.RARE, 35, 83, -22, Direction.WEST, "in the north-east watchtower's chamber, at the top of its ladder"),
				new TreasureSpot(TreasureTier.COMMON, -22, 66, 25, Direction.WEST, "on the south-west quay, behind the field cannons"),
				new TreasureSpot(TreasureTier.COMMON, 13, 66, -52, Direction.EAST, "on the green behind the wall, beside the east guard post"))),
			Map.entry("Saltmarket Docks", List.of(
				new TreasureSpot(TreasureTier.LEGENDARY, -100, 114, 47, Direction.EAST, "on the lighthouse balcony, at the top of the ladder"),
				new TreasureSpot(TreasureTier.RARE, 46, 69, -107, Direction.NORTH, "inside the boss ring at the far camp, by crates"),
				new TreasureSpot(TreasureTier.COMMON, -33, 65, 57, Direction.NORTH, "by cargo on the harbour promenade"),
				new TreasureSpot(TreasureTier.COMMON, -45, 68, -29, Direction.NORTH, "back alley against the landward wall, by crates"),
				new TreasureSpot(TreasureTier.COMMON, 59, 66, -137, Direction.WEST, "among the rocks on the east headland"))),
			Map.entry("Stonewall Prison", List.of(
				new TreasureSpot(TreasureTier.LEGENDARY, 4, 102, 0, Direction.WEST, "on the central tower's roof (ladder up from the furnished room)"),
				new TreasureSpot(TreasureTier.RARE, 0, 76, -23, Direction.SOUTH, "on the inner enclosure's ledge, north"),
				new TreasureSpot(TreasureTier.COMMON, 30, 67, -8, Direction.WEST, "east grass courtyard"),
				new TreasureSpot(TreasureTier.COMMON, -49, 64, -2, Direction.EAST, "the waterfront landing"))),
			Map.entry("Sunscorch Dunes", List.of(
				new TreasureSpot(TreasureTier.LEGENDARY, -23, 71, -51, Direction.EAST, "dark, covered, raised 5, walls w"),
				new TreasureSpot(TreasureTier.RARE, -20, 71, -30, Direction.EAST, "indoors, raised 5, walls w"),
				new TreasureSpot(TreasureTier.COMMON, -24, 67, 17, Direction.SOUTH, "by crates, walls n"),
				new TreasureSpot(TreasureTier.COMMON, 9, 67, 26, Direction.SOUTH, "by crates, walls n"))),
			Map.entry("Verdant Wilds", List.of(
				new TreasureSpot(TreasureTier.LEGENDARY, -121, 67, -2, Direction.WEST, "by crates, dark, covered, walls e"),
				new TreasureSpot(TreasureTier.RARE, -109, 67, -18, Direction.WEST, "by crates, dark, covered, walls e"),
				new TreasureSpot(TreasureTier.COMMON, 14, 69, 14, Direction.SOUTH, "by crates, walls n"),
				new TreasureSpot(TreasureTier.COMMON, -23, 67, 28, Direction.EAST, "by crates, walls w"))));

	/** Spots on islands that older worlds still have in their pre-rebuild layout (see IslandPlans.REDESIGNED_IN). */
	private static final Map<String, List<TreasureSpot>> LEGACY = Map.of(
			"Ironhold Bastion", List.of(
				new TreasureSpot(TreasureTier.LEGENDARY, 19, 66, -24, Direction.NORTH, "back corner of the bastion"),
				new TreasureSpot(TreasureTier.RARE, -7, 67, -8, Direction.EAST, "indoors in the keep"),
				new TreasureSpot(TreasureTier.COMMON, -13, 67, 21, Direction.NORTH, "walls s"),
				new TreasureSpot(TreasureTier.COMMON, -23, 65, -26, Direction.NORTH, "rocky back corner")),
			"Lotus Palace", List.of(
				new TreasureSpot(TreasureTier.LEGENDARY, 1, 70, -26, Direction.EAST, "raised garden behind the palace"),
				new TreasureSpot(TreasureTier.RARE, -33, 64, 24, Direction.SOUTH, "by crates at the west camp, guarded"),
				new TreasureSpot(TreasureTier.COMMON, -1, 64, 15, Direction.SOUTH, "walls n"),
				new TreasureSpot(TreasureTier.COMMON, 13, 64, 23, Direction.NORTH, "sheltered corner by the shore")),
			"Redbrick Port", List.of(
				new TreasureSpot(TreasureTier.LEGENDARY, -16, 76, -23, Direction.SOUTH, "raised 11, walls nw"),
				new TreasureSpot(TreasureTier.RARE, 24, 65, -25, Direction.NORTH, "under a shelter by crates"),
				new TreasureSpot(TreasureTier.COMMON, 35, 66, 6, Direction.EAST, "by crates"),
				new TreasureSpot(TreasureTier.COMMON, -37, 65, -14, Direction.WEST, "by crates under a shelter")),
			"Crystal Fountain", List.of(
				new TreasureSpot(TreasureTier.LEGENDARY, -28, 64, 28, Direction.SOUTH, "beach camp shelter, guarded by the west camp"),
				new TreasureSpot(TreasureTier.RARE, 26, 64, 31, Direction.SOUTH, "beside the east camp on the beach"),
				new TreasureSpot(TreasureTier.COMMON, -25, 67, -7, Direction.SOUTH, "at the foot of a crystal pillar"),
				new TreasureSpot(TreasureTier.COMMON, 19, 67, -17, Direction.SOUTH, "at the foot of a crystal pillar")),
			"Frostpeak Isle", List.of(
				new TreasureSpot(TreasureTier.LEGENDARY, 32, 67, 4, Direction.SOUTH, "dark, covered, walls ne"),
				new TreasureSpot(TreasureTier.RARE, -17, 66, 7, Direction.NORTH, "by crates, indoors, walls s"),
				new TreasureSpot(TreasureTier.COMMON, 24, 66, 24, Direction.SOUTH, "walls n"),
				new TreasureSpot(TreasureTier.COMMON, 2, 63, 23, Direction.NORTH, "walls s")),
			"Saltmarket Docks", List.of(
				new TreasureSpot(TreasureTier.LEGENDARY, -19, 66, -19, Direction.WEST, "under the warehouse awning, by crates"),
				new TreasureSpot(TreasureTier.RARE, 19, 66, -19, Direction.EAST, "under the warehouse awning, by crates"),
				new TreasureSpot(TreasureTier.COMMON, 4, 64, 28, Direction.WEST, "by crates, walls e"),
				new TreasureSpot(TreasureTier.COMMON, -3, 66, -24, Direction.SOUTH, "walls ne")));

	/** Spots of in-between rebuilds that older worlds keep (see IslandPlans.INTERIM), by plan class name. */
	private static final Map<String, List<TreasureSpot>> INTERIM = Map.of(
			"LotusPalaceV17", List.of(
				new TreasureSpot(TreasureTier.LEGENDARY, 22, 12, -134, Direction.WEST, "in the palace hall, north-east, behind the column rows"),
				new TreasureSpot(TreasureTier.RARE, -31, 12, -58, Direction.EAST, "inside the tall shell house by the south-west path"),
				new TreasureSpot(TreasureTier.COMMON, 4, 66, -24, Direction.SOUTH, "on the north islet, by the landing"),
				new TreasureSpot(TreasureTier.COMMON, 30, 66, 22, Direction.NORTH, "on the south-east islet, among the palms")),
			"CloudtopIslesV12", List.of(
				new TreasureSpot(TreasureTier.LEGENDARY, -44, 201, -128, Direction.EAST, "citadel lawn, west of the courtyard, guarded by the west camp"),
				new TreasureSpot(TreasureTier.RARE, 50, 115, -71, Direction.WEST, "Azure Rise, between the temple and the colonnade"),
				new TreasureSpot(TreasureTier.COMMON, 24, 99, 2, Direction.SOUTH, "Mistfall, on the grass by the trees"),
				new TreasureSpot(TreasureTier.COMMON, -30, 101, -36, Direction.NORTH, "Bloomhold, between the towers off the east path")),
			"StonewallPrisonV9", List.of(
				new TreasureSpot(TreasureTier.LEGENDARY, -35, 91, -51, Direction.EAST, "raised 23, walls w"),
				new TreasureSpot(TreasureTier.RARE, -44, 69, -9, Direction.EAST, "dark, covered, walls sw"),
				new TreasureSpot(TreasureTier.COMMON, 15, 67, 39, Direction.EAST, "by crates, walls w"),
				new TreasureSpot(TreasureTier.COMMON, 36, 68, 41, Direction.NORTH, "walls s")),
			"CloudtopIslesV7", List.of(
				new TreasureSpot(TreasureTier.LEGENDARY, -69, 96, -35, Direction.WEST, "dark, covered, walls e"),
				new TreasureSpot(TreasureTier.RARE, 55, 144, -104, Direction.EAST, "indoors, walls w"),
				new TreasureSpot(TreasureTier.COMMON, 42, 126, -68, Direction.EAST, "walls w"),
				new TreasureSpot(TreasureTier.COMMON, 6, 123, -37, Direction.SOUTH, "walls nw")));

	/** Islands that, by their owner's design, have no treasure chests. */
	public static final java.util.Set<String> NONE_BY_DESIGN = java.util.Set.of("Frostpeak Isle", "Ironhold Bastion");

	private TreasureSpots() {
	}

	public static List<TreasureSpot> forIsland(String island) {
		return SPOTS.getOrDefault(island, List.of());
	}

	/** The spots of an island's pre-rebuild layout, for older worlds (empty if it has none). */
	public static List<TreasureSpot> legacy(String island) {
		return LEGACY.getOrDefault(island, List.of());
	}

	/** The spots of an in-between rebuild (by its plan), for the worlds that keep it (empty if it has none). */
	public static List<TreasureSpot> interim(com.grandline.world.plan.IslandPlan plan) {
		return plan == null ? List.of() : INTERIM.getOrDefault(plan.getClass().getSimpleName(), List.of());
	}

	public static Map<String, List<TreasureSpot>> all() {
		return SPOTS;
	}
}
