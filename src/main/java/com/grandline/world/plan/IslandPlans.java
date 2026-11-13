package com.grandline.world.plan;

import com.grandline.world.Island;
import com.grandline.world.SeaLayout;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Registry of hand-designed island plans, by island name. To add an island: put its spec in {@link SeaLayout} and its
 * plan here. Islands without a plan fall back to the generic themed builder.
 */
public final class IslandPlans {
	private static final Map<String, IslandPlan> PLANS = new HashMap<>();
	/** Old plans that worlds created before an island's redesign keep using, by island name. */
	private static final Map<String, IslandPlan> LEGACY = new HashMap<>();
	/** The world layout version in which each redesigned island got its new plan. */
	private static final Map<String, Integer> REDESIGNED_IN = Map.of("Verdant Wilds", 2, "Dawnreach Harbor", 3);

	static {
		FirstSeaPlans.all().forEach(IslandPlans::add);
		FirstSeaPlans.legacy().forEach(plan -> LEGACY.put(plan.name(), plan));
	}

	private IslandPlans() {
	}

	public static void add(IslandPlan plan) {
		if (PLANS.put(plan.name(), plan) != null) {
			throw new IllegalStateException("Duplicate island plan " + plan.name());
		}
	}

	/** The plan this world uses for the island: the redesigned one, or the old one in worlds created before it. */
	public static IslandPlan get(String islandName) {
		com.grandline.world.IslandRegistry registry = com.grandline.world.IslandRegistry.current();
		if (registry != null && registry.layout() < REDESIGNED_IN.getOrDefault(islandName, 0)) {
			IslandPlan legacy = LEGACY.get(islandName);
			if (legacy != null) {
				return legacy;
			}
		}
		return PLANS.get(islandName);
	}

	/** The pre-redesign plan of an island, if it has one (tests, tools). */
	public static IslandPlan legacy(String islandName) {
		return LEGACY.get(islandName);
	}

	public static IslandPlan get(Island island) {
		return island == null ? null : get(island.name());
	}

	public static List<IslandPlan> all() {
		return List.copyOf(PLANS.values());
	}

	/** Anchors of {@code type} for an island (empty if it has no plan). */
	public static List<Anchor> anchors(Island island, Anchor.Type type) {
		IslandPlan plan = get(island);
		return plan == null ? List.of() : plan.anchors().stream().filter(a -> a.type() == type).toList();
	}

	/**
	 * Where players arrive on an island (world spawn, sea travel): its plan's ARRIVAL anchor in world coordinates, or
	 * null when the plan has none (callers keep their old behaviour).
	 */
	public static net.minecraft.core.BlockPos arrival(String islandName, int islandX, int islandZ) {
		IslandPlan plan = get(islandName);
		if (plan == null) {
			return null;
		}
		for (Anchor a : plan.anchors()) {
			if (a.type() == Anchor.Type.ARRIVAL) {
				return new net.minecraft.core.BlockPos(islandX + a.x(), a.y(), islandZ + a.z());
			}
		}
		return null;
	}

	/** Whether any part of the planned island can touch the chunk with these minimum block coordinates. */
	public static boolean touches(SeaLayout.Spec spec, IslandPlan plan, int chunkMinX, int chunkMinZ) {
		int reach = plan.extent();
		int nearestX = Math.max(chunkMinX, Math.min(spec.x(), chunkMinX + 15));
		int nearestZ = Math.max(chunkMinZ, Math.min(spec.z(), chunkMinZ + 15));
		long dx = nearestX - spec.x();
		long dz = nearestZ - spec.z();
		return dx * dx + dz * dz <= (long) reach * reach;
	}

	/** Builds the part of a planned island inside one chunk: terrain columns first, then structures. */
	public static void build(net.minecraft.world.level.WorldGenLevel level, SeaLayout.Spec spec, IslandPlan plan, int chunkMinX, int chunkMinZ) {
		Terrain terrain = plan.terrain();
		Canvas canvas = new Canvas(level, terrain, spec.x(), spec.z(), chunkMinX, chunkMinZ);
		net.minecraft.world.level.block.state.BlockState air = net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
		for (int wx = chunkMinX; wx < chunkMinX + 16; wx++) {
			for (int wz = chunkMinZ; wz < chunkMinZ + 16; wz++) {
				int x = wx - spec.x();
				int z = wz - spec.z();
				if (x * x + z * z > plan.extent() * plan.extent()) {
					continue;
				}
				int g = terrain.ground(x, z);
				if (g <= Terrain.FLOOR + 1) {
					continue; // open sea: leave the generated ocean alone
				}
				for (int y = Terrain.FLOOR; y < g; y++) {
					canvas.set(x, y, z, terrain.under(x, z));
				}
				canvas.set(x, g, z, terrain.top(x, z));
				if (g >= Terrain.WATER_TOP) {
					for (int y = g + 1; y <= g + 24; y++) {
						canvas.set(x, y, z, air);
					}
				}
			}
		}
		plan.build(canvas);
	}
}
