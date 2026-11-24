package com.grandline.npc;

import com.grandline.GrandLineMod;
import com.grandline.entity.GrandLineEnemy;
import com.grandline.entity.GrandLineEntities;
import com.grandline.entity.Islander;
import com.grandline.world.Island;
import com.grandline.world.IslandRegistry;
import com.grandline.world.IslandSpawns;
import com.grandline.world.plan.Anchor;
import com.grandline.world.plan.IslandRoster;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Keeps islands with a fixed population ({@link IslandRoster}: Millbrook Village, Ironhold Bastion) to it, in new and
 * existing worlds:
 * <ul>
 * <li>Friendly NPCs, each at its own spot. A missing one is placed, a misplaced one walked back, extras and roles the
 * island no longer has removed, and each carries the island's name for its role (names.json).</li>
 * <li>Enemies (Lv per roster), one per spawn spot. Each comes back its group's respawn time after it falls, at its own
 * spot, on its own timer. Enemies from before the roster (no spot) and bosses are removed; a spot never has two.</li>
 * </ul>
 * Only while the whole island is loaded, so nothing standing in an unloaded chunk is doubled.
 */
public final class RosterNpcs {
	/** Tag carrying an enemy's spawn spot index (the name dates from Millbrook, the first roster island). */
	public static final String SPOT_TAG = "grandline.millbrook_spot.";
	static final int CHECK_TICKS = 5; // a quarter second, so respawns land on time
	/** How far around the island centre the roster is enforced (covers every spot and the landings). */
	static final int REACH = 90;

	/** Game time each spot's enemy fell, per island (spots not listed have not lost theirs since the server started). */
	private static final Map<Long, Map<Integer, Long>> FALLEN = new HashMap<>();

	private RosterNpcs() {
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % CHECK_TICKS == 0) {
				tick(server);
			}
		});
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (entity instanceof GrandLineEnemy enemy && entity.level() instanceof ServerLevel level && level.dimension() == Level.OVERWORLD) {
				int spot = spotOf(enemy);
				if (spot >= 0) {
					FALLEN.computeIfAbsent(enemy.islandId(), k -> new HashMap<>()).put(spot, level.getGameTime());
				}
			}
		});
	}

	/** Whether this island's NPCs and enemies are managed here (other systems leave it alone). */
	public static boolean manages(Island island) {
		return IslandRoster.of(island) != null;
	}

	/** An enemy's spawn spot index, or -1. */
	public static int spotOf(GrandLineEnemy enemy) {
		for (String tag : enemy.getTags()) {
			if (tag.startsWith(SPOT_TAG)) {
				try {
					return Integer.parseInt(tag.substring(SPOT_TAG.length()));
				} catch (NumberFormatException e) {
					return -1;
				}
			}
		}
		return -1;
	}

	private static void tick(MinecraftServer server) {
		ServerLevel level = server.overworld();
		if (level.players().isEmpty()) {
			return;
		}
		for (Island island : IslandRegistry.get(server).all()) {
			IslandRoster roster = IslandRoster.of(island);
			if (roster != null && level.isPositionEntityTicking(new BlockPos(island.x(), 64, island.z()))) {
				enforce(level, island, roster);
			}
		}
	}

	/** Every chunk within {@code r} blocks of island-local (x, z) is ticking entities (so nothing standing there is missed). */
	static boolean loaded(ServerLevel level, Island island, int x, int z, int r) {
		for (int dx = -r; dx <= r; dx += 8) {
			for (int dz = -r; dz <= r; dz += 8) {
				if (!level.isPositionEntityTicking(new BlockPos(island.x() + x + dx, 64, island.z() + z + dz))) {
					return false;
				}
			}
		}
		return true;
	}

	/** Blocks around an enemy spot that must be loaded before it is checked: its leash and a margin. */
	static final int SPOT_CLEARANCE = 32;

	/** Brings the island to its roster now (exposed for tests). */
	public static void enforce(ServerLevel level, Island island, IslandRoster roster) {
		EnemyRespawns.forget(level, island.id(), pos -> IslandRoster.covers(island, IslandPopulation.regionAt(island, pos))); // own timers instead
		var plan = com.grandline.world.plan.IslandPlans.get(island);
		int reach = Math.max(REACH, plan != null ? plan.extent() : 0) + 32; // the whole island, however large
		AABB area = new AABB(island.x() - reach, level.getMinBuildHeight(), island.z() - reach,
				island.x() + reach, level.getMaxBuildHeight(), island.z() + reach);
		friendlies(level, island, roster, area);
		enemies(level, island, roster, area);
	}

	// ------------------------------------------------------------------------------------------------ friendly NPCs

	private static void friendlies(ServerLevel level, Island island, IslandRoster roster, AABB area) {
		for (IslandRoster.Slot slot : roster.slots().apply(island)) {
			if (!loaded(level, island, slot.x(), slot.z(), 16)) {
				return; // part of the town is not loaded: leave its NPCs until it is (no doubles)
			}
		}
		List<Islander> npcs = new ArrayList<>(level.getEntitiesOfClass(Islander.class, area,
				n -> n.isAlive() && n.islandId() == island.id() && IslandRoster.covers(island, n.region())
						&& !(IslandRoster.lowerOnly(island) && n.role() == Islander.Role.INSTINCT_SAGE))); // the upper region's own
		List<Islander> kept = new ArrayList<>();
		for (IslandRoster.Slot slot : roster.slots().apply(island)) {
			Islander.Role role = Islander.Role.valueOf(slot.role());
			BlockPos spot = standAt(level, island, slot.x(), slot.y(), slot.z());
			Islander best = null;
			double bestD = Double.MAX_VALUE;
			for (Islander npc : npcs) {
				if (npc.role() != role || kept.contains(npc)) {
					continue;
				}
				double d = npc.distanceToSqr(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5);
				if (d < bestD) {
					bestD = d;
					best = npc;
				}
			}
			if (best == null) {
				best = GrandLineEntities.ISLANDER.create(level);
				if (best == null) {
					continue;
				}
				best.setup(role, island.id(), island.level());
				best.setPersistenceRequired();
				place(best, spot, slot.yaw());
				level.addFreshEntity(best);
				GrandLineMod.LOGGER.info("{}: placed {} at {}", island.name(), role, spot);
			} else if (bestD > 2.25) {
				place(best, spot, slot.yaw());
				GrandLineMod.LOGGER.info("{}: moved {} to {}", island.name(), role, spot);
			}
			best.updateName(); // the island's names (names.json), also on NPCs placed before they were renamed
			kept.add(best);
		}
		for (Islander npc : npcs) {
			if (!kept.contains(npc)) {
				GrandLineMod.LOGGER.info("{}: removed {} at {}", island.name(), npc.role(), npc.blockPosition());
				npc.discard(); // an extra, or a role the island no longer has
			}
		}
	}

	private static void place(Islander npc, BlockPos spot, float yaw) {
		npc.moveTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, yaw, 0);
		npc.setYHeadRot(yaw);
		npc.setYBodyRot(yaw);
	}

	/** The standing spot at island-local (x, z): the given height, or on top of whatever is built there. */
	static BlockPos standAt(ServerLevel level, Island island, int x, int y, int z) {
		int wx = island.x() + x, wz = island.z() + z;
		return new BlockPos(wx, y != Anchor.SURFACE ? y : level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, wx, wz), wz);
	}

	// ------------------------------------------------------------------------------------------------------ enemies

	private static void enemies(ServerLevel level, Island island, IslandRoster roster, AABB area) {
		// Every hostile on the island that is not one of its spot enemies goes: old camp enemies, other kinds, bosses.
		List<GrandLineEnemy> enemies = level.getEntitiesOfClass(GrandLineEnemy.class, area, e -> e.isAlive()
				&& !e.getTags().contains(com.grandline.race.RaceTrials.TAG)
				&& !(e instanceof com.grandline.entity.SaberBoss) // the temple's secret boss keeps its own rules (TempleBosses)
				&& !e.getTags().contains(RaidEvents.TAG) // the raid boss is not part of the population
				&& IslandRoster.covers(island, IslandPopulation.regionAt(island, e.origin() != null ? e.origin() : e.blockPosition()))
				&& (e.islandId() == island.id() || e.islandId() == Long.MIN_VALUE));
		List<IslandRoster.Spot> spots = roster.spots();
		Map<Integer, GrandLineEnemy> bySpot = new HashMap<>();
		for (GrandLineEnemy enemy : enemies) {
			int spot = spotOf(enemy);
			if (spot >= 0 && spot < spots.size() && !loaded(level, island, spots.get(spot).x(), spots.get(spot).z(), SPOT_CLEARANCE)) {
				continue; // its spot's surroundings are not all loaded yet: judged once they are
			}
			if (spot < 0 || spot >= spots.size() || enemy.kind() != spots.get(spot).group().kind() || bySpot.containsKey(spot)
					|| movedSpot(island, spots.get(spot), enemy)) {
				enemy.discard(); // from before the roster, the wrong kind, a boss, or a second enemy on one spot
				continue;
			}
			bySpot.put(spot, enemy);
			if (!enemy.skin().equals(spots.get(spot).group().skin())) {
				enemy.setSkin(spots.get(spot).group().skin()); // an enemy spawned before its island had its own look
			}
		}
		long now = level.getGameTime();
		Map<Integer, Long> fallen = FALLEN.computeIfAbsent(island.id(), k -> new HashMap<>());
		for (int i = 0; i < spots.size(); i++) {
			if (!loaded(level, island, spots.get(i).x(), spots.get(i).z(), SPOT_CLEARANCE)) {
				continue;
			}
			if (!COVERED.contains(island.name() + "#" + i)) { // a spot under a roof or an arch is reported once
				COVERED.add(island.name() + "#" + i);
				BlockPos top = standAt(level, island, spots.get(i).x(), Anchor.SURFACE, spots.get(i).z());
				int ground = standY(island, spots.get(i), top.getY());
				if (top.getY() > ground + 2) {
					GrandLineMod.LOGGER.warn("{}: enemy spot {} ({}, {}) is covered {} blocks up", island.name(), i, spots.get(i).x(), spots.get(i).z(), top.getY() - ground);
				}
			}
			if (bySpot.containsKey(i)) {
				continue;
			}
			Long when = fallen.get(i);
			if (when != null && now - when < spots.get(i).group().respawnTicks() && now >= when) {
				continue; // still on its own timer
			}
			fallen.remove(i);
			spawn(level, island, spots.get(i), i);
		}
	}

	private static void spawn(ServerLevel level, Island island, IslandRoster.Spot spot, int index) {
		IslandRoster.Group group = spot.group();
		BlockPos top = standAt(level, island, spot.x(), Anchor.SURFACE, spot.z());
		BlockPos at = new BlockPos(top.getX(), standY(island, spot, top.getY()), top.getZ()); // on the ground, never a roof
		if (spot.y() != Anchor.SURFACE && !IslandSpawns.isSafe(level, at)) {
			for (int dy = 4; dy >= -8; dy--) { // a floating island's surface rolls: find the footing near the given height
				BlockPos p = at.above(dy);
				if (IslandSpawns.isSafe(level, p)) {
					at = p;
					break;
				}
			}
		}
		if (!IslandSpawns.isSafe(level, at)) {
			BlockPos near = nearestSafe(level, at);
			if (near == null) {
				return;
			}
			at = near;
		}
		GrandLineEnemy enemy = GrandLineEntities.typeOf(group.kind()).create(level);
		if (enemy == null) {
			return;
		}
		enemy.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, level.getRandom().nextFloat() * 360, 0);
		enemy.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.EVENT, null, null);
		enemy.equip();
		enemy.setSkin(group.skin());
		com.grandline.entity.EnemyAbilities.onSpawn(enemy); // Aura, by look
		enemy.setOrigin(at, island.id());
		enemy.setGrandLineLevel(group.minLevel() + level.getRandom().nextInt(group.maxLevel() - group.minLevel() + 1));
		enemy.addTag(SPOT_TAG + index);
		enemy.setPersistenceRequired();
		level.addFreshEntity(enemy);
	}

	/** Where a spot's enemy stands: its exact height, or on the island's ground (never a roof). */
	static int standY(Island island, IslandRoster.Spot spot, int fallback) {
		if (spot.y() != Anchor.SURFACE) {
			return spot.y();
		}
		var plan = com.grandline.world.plan.IslandPlans.get(island);
		return plan != null ? plan.terrain().ground(spot.x(), spot.z()) + 1 : fallback;
	}

	/** An enemy whose home is not its spot as the roster now has it (the spot moved, or it was placed on a roof): replaced. */
	private static boolean movedSpot(Island island, IslandRoster.Spot spot, GrandLineEnemy enemy) {
		BlockPos home = enemy.origin();
		if (home == null) {
			return true;
		}
		int ground = standY(island, spot, home.getY());
		return Math.abs(home.getX() - (island.x() + spot.x())) > 3 || Math.abs(home.getZ() - (island.z() + spot.z())) > 3
				|| (spot.y() != Anchor.SURFACE ? home.getY() - ground > 4 || ground - home.getY() > 8 : Math.abs(home.getY() - ground) > 2);
	}

	private static final java.util.Set<String> COVERED = new java.util.HashSet<>();

	private static BlockPos nearestSafe(ServerLevel level, BlockPos at) {
		for (int r = 1; r <= 2; r++) {
			for (int dx = -r; dx <= r; dx++) {
				for (int dz = -r; dz <= r; dz++) {
					for (int dy : new int[]{0, 1, -1}) {
						BlockPos p = new BlockPos(at.getX() + dx, at.getY() + dy, at.getZ() + dz);
						if (IslandSpawns.isSafe(level, p)) {
							return p;
						}
					}
				}
			}
		}
		return null;
	}
}
