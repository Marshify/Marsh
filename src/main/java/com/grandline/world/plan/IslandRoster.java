package com.grandline.world.plan;

import com.grandline.entity.EnemyKind;
import com.grandline.world.Island;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * An island with a fixed population, kept exactly to it by {@link com.grandline.npc.RosterNpcs} in new and existing
 * worlds: friendly NPCs at their own spots, and enemy groups on fixed spawn spots, each enemy with its own respawn.
 * Islands without one use the general population systems.
 *
 * @param groups the enemy groups (spot indices run through the groups in order)
 * @param slots  the friendly NPCs (computed per island: some stand where the plan's terrain puts them)
 * @param quests what the island's quest giver offers, in order
 */
public record IslandRoster(List<Group> groups, Function<Island, List<Slot>> slots, List<Quest> quests) {
	/** A friendly NPC's spot: role name (Islander.Role), island-local x, y (or {@link Anchor#SURFACE}), z and yaw. */
	public record Slot(String role, int x, int y, int z, float yaw) {
		public Slot(String role, int x, int z, float yaw) {
			this(role, x, Anchor.SURFACE, z, yaw);
		}
	}

	/**
	 * Enemies of one kind and look: an island skin under textures/entity (empty: the kind's own), a level range, the
	 * delay before a fallen one returns, and their spawn spots (island-local x, z).
	 */
	public record Group(EnemyKind kind, String skin, int minLevel, int maxLevel, int respawnTicks, List<int[]> spots) {
		public boolean boss() {
			return kind == EnemyKind.CAPTAIN;
		}
	}

	/**
	 * A quest: defeat {@code count} of a kind (1 for a boss), offered from {@code minLevel}. {@code skin} names one
	 * look when an island has several of a kind (Stonewall's three bosses); empty: any of the kind.
	 */
	public record Quest(EnemyKind kind, int count, int minLevel, String skin) {
		public Quest(EnemyKind kind, int count, int minLevel) {
			this(kind, count, minLevel, "");
		}

		public boolean boss() {
			return kind == EnemyKind.CAPTAIN;
		}

		/** The quest's target as saved: the kind, or "KIND#skin". */
		public String target() {
			return skin.isEmpty() ? kind.name() : kind.name() + "#" + skin;
		}
	}

	/** The kind of a saved quest target ("BANDIT" or "CAPTAIN#iron_jailer"). */
	public static EnemyKind targetKind(String target) {
		return EnemyKind.valueOf(target.split("#")[0]);
	}

	/** The translation key naming a saved quest target: a named look's own name, else the kind's. */
	public static String targetName(String target) {
		int i = target.indexOf('#');
		return i < 0 ? "entity.grandline." + EnemyKind.valueOf(target).id() : "entity.grandline." + target.substring(i + 1);
	}

	/** One spawn spot: its group and island-local position; y is {@link Anchor#SURFACE} (the island's ground) or exact. */
	public record Spot(Group group, int x, int y, int z) {
	}

	/** Every spawn spot, numbered through the groups in order (the number is the spot index enemies carry). */
	public List<Spot> spots() {
		List<Spot> all = new ArrayList<>();
		for (Group g : groups) {
			for (int[] p : g.spots()) {
				all.add(p.length == 3 ? new Spot(g, p[0], p[1], p[2]) : new Spot(g, p[0], Anchor.SURFACE, p[1]));
			}
		}
		return all;
	}

	/** The group of this enemy kind (the first, if several share it), or null. */
	public Group group(EnemyKind kind) {
		for (Group g : groups) {
			if (g.kind() == kind) {
				return g;
			}
		}
		return null;
	}

	/**
	 * Whether the roster covers only the island's lower region (Cloudtop Isles): its upper region keeps its own quest
	 * giver, trainer, camps and respawns, run by the general systems.
	 */
	public static boolean lowerOnly(Island island) {
		return island != null && IslandPlans.get(island) instanceof CloudtopIsles;
	}

	/** Whether a spot or an NPC of this region belongs to the island's roster. */
	public static boolean covers(Island island, int region) {
		return of(island) != null && (!lowerOnly(island) || region == com.grandline.world.FirstSeaProgression.LOWER);
	}

	public static int seconds(int s) {
		return s * 20;
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
		if (plan instanceof VerdantWilds) {
			return IslandRosters.VERDANT;
		}
		if (plan instanceof SaltmarketDocks) {
			return IslandRosters.SALTMARKET;
		}
		if (plan instanceof SunscorchDunes) {
			return IslandRosters.SUNSCORCH;
		}
		if (plan instanceof FrostpeakIsle) {
			return IslandRosters.FROSTPEAK;
		}
		if (plan instanceof StonewallPrison) {
			return IslandRosters.STONEWALL;
		}
		if (plan instanceof GladiatorsRing) {
			return IslandRosters.GLADIATOR;
		}
		if (plan instanceof RedbrickPort) {
			return IslandRosters.REDBRICK;
		}
		if (plan instanceof CloudtopIsles) {
			return IslandRosters.CLOUDTOP;
		}
		return null;
	}
}
