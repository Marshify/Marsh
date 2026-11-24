package com.grandline.world.plan;

import com.grandline.entity.EnemyKind;
import com.grandline.world.Island;

import java.util.List;
import java.util.function.Function;

/**
 * An island with a fixed population, kept exactly to it by {@link com.grandline.npc.RosterNpcs} in new and existing
 * worlds: friendly NPCs at their own spots and enemies of one kind on fixed spawn spots, each with its own respawn.
 * Islands without one use the general population systems.
 *
 * @param kind       the island's enemy kind (quests ask for it)
 * @param minLevel   lowest enemy level
 * @param maxLevel   highest enemy level
 * @param questKills enemies the island's quest asks for
 * @param spots      enemy spawn spots
 * @param slots      the friendly NPCs (computed per island: some stand where the plan's terrain puts them)
 */
public record IslandRoster(EnemyKind kind, int minLevel, int maxLevel, int questKills, List<Spot> spots,
		Function<Island, List<Slot>> slots) {
	/** A friendly NPC's spot: role name (Islander.Role), island-local x, y (or {@link Anchor#SURFACE}), z and yaw. */
	public record Slot(String role, int x, int y, int z, float yaw) {
		public Slot(String role, int x, int z, float yaw) {
			this(role, x, Anchor.SURFACE, z, yaw);
		}
	}

	/** An enemy's spawn spot (island-local) and its group. */
	public record Spot(int x, int z, int group) {
	}

	/** The roster of this island, or null (worlds on an island's original layout keep their old population). */
	public static IslandRoster of(Island island) {
		if (island == null) {
			return null;
		}
		IslandPlan plan = IslandPlans.get(island);
		if (plan instanceof MillbrookVillage) {
			return MillbrookRoster.ROSTER;
		}
		if (plan instanceof IronholdBastion) {
			return IronholdRoster.ROSTER;
		}
		return null;
	}
}
