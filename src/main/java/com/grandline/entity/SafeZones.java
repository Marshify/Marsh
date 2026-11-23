package com.grandline.entity;

import com.grandline.world.Island;
import com.grandline.world.IslandRegistry;
import com.grandline.world.plan.Anchor;
import com.grandline.world.plan.IslandPlans;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.Set;

/**
 * Protected areas where no fight happens: around every friendly NPC (quest givers, merchants, ability teachers,
 * innkeepers... every islander), the planned spots of an island's NPCs, its arrival dock and docks, and the world spawn.
 * Hostile NPCs never notice, chase or attack a player inside one, give up a chase that enters one, and cannot be hurt by
 * a player standing in one (so it cannot be used to attack without risk). Server side only.
 */
public final class SafeZones {
	/** Anchors whose spots are protected (where friendly NPCs stand, and where players arrive). */
	private static final Set<Anchor.Type> PROTECTED = EnumSet.of(Anchor.Type.QUEST, Anchor.Type.FRUIT_DEALER, Anchor.Type.FISHERMAN,
			Anchor.Type.NAVIGATOR, Anchor.Type.DOCK, Anchor.Type.ARRIVAL, Anchor.Type.GACHA, Anchor.Type.GUNSMITH, Anchor.Type.HOME,
			Anchor.Type.SWORDSMITH);
	/** Height (up and down) a protected area reaches. */
	private static final double HEIGHT = 6;

	private SafeZones() {
	}

	/** Whether {@code pos} lies within {@code radius} blocks of a protected spot. */
	public static boolean isProtected(ServerLevel level, Vec3 pos, double radius) {
		if (radius <= 0) {
			return false;
		}
		// Friendly NPCs, wherever they stand.
		AABB box = new AABB(pos, pos).inflate(radius, HEIGHT, radius);
		for (Islander npc : level.getEntitiesOfClass(Islander.class, box, Islander::isAlive)) {
			if (horizontal(npc.position(), pos) <= radius) {
				return true;
			}
		}
		// The world spawn of a Grand Line world (where new players appear, on the starting island).
		if (level.dimension() == net.minecraft.world.level.Level.OVERWORLD && grandLineWorld(level)
				&& within(Vec3.atBottomCenterOf(level.getSharedSpawnPos()), pos, radius)) {
			return true;
		}
		// The planned NPC spots, docks and arrival points of the island here.
		IslandRegistry registry = IslandRegistry.current();
		if (registry == null || level.dimension() != net.minecraft.world.level.Level.OVERWORLD) {
			return false;
		}
		for (Island island : IslandRegistry.reaching(level.getServer(), (int) pos.x, (int) pos.z, (int) Math.ceil(radius))) {
			var plan = IslandPlans.get(island);
			if (plan == null) {
				continue;
			}
			for (Anchor a : plan.anchors()) {
				if (!PROTECTED.contains(a.type())) {
					continue;
				}
				double ax = island.x() + a.x() + 0.5, az = island.z() + a.z() + 0.5;
				if (Math.hypot(ax - pos.x, az - pos.z) > radius) {
					continue;
				}
				if (a.y() == Anchor.SURFACE || Math.abs(a.y() - pos.y) <= HEIGHT + 4) {
					return true;
				}
			}
		}
		return false;
	}

	private static final java.util.Map<ServerLevel, Boolean> GRAND_LINE = java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());

	private static boolean grandLineWorld(ServerLevel level) {
		return GRAND_LINE.computeIfAbsent(level, com.grandline.world.ModWorld::isGrandLineWorld);
	}

	private static boolean within(Vec3 a, Vec3 b, double radius) {
		return horizontal(a, b) <= radius && Math.abs(a.y - b.y) <= HEIGHT + 4;
	}

	private static double horizontal(Vec3 a, Vec3 b) {
		return Math.hypot(a.x - b.x, a.z - b.z);
	}
}
