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
 * <li>Enemies (Lv per roster), one per spawn spot. Each comes back {@link #RESPAWN_TICKS} after it falls, at its own
 * spot, on its own timer. Enemies from before the roster (no spot) and bosses are removed; a spot never has two.</li>
 * </ul>
 * Only while the whole island is loaded, so nothing standing in an unloaded chunk is doubled.
 */
public final class RosterNpcs {
	public static final int RESPAWN_TICKS = 8 * 20;
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
			if (roster != null && loaded(level, island)) {
				enforce(level, island, roster);
			}
		}
	}

	/** Every chunk the roster touches is ticking entities. */
	static boolean loaded(ServerLevel level, Island island) {
		for (int dx = -REACH; dx <= REACH; dx += 16) {
			for (int dz = -REACH; dz <= REACH; dz += 16) {
				if (!level.isPositionEntityTicking(new BlockPos(island.x() + dx, 64, island.z() + dz))) {
					return false;
				}
			}
		}
		return true;
	}

	/** Brings the island to its roster now (exposed for tests). */
	public static void enforce(ServerLevel level, Island island, IslandRoster roster) {
		EnemyRespawns.forget(level, island.id()); // the island's own timers replace the general respawn queue
		AABB area = new AABB(island.x() - REACH - 16, level.getMinBuildHeight(), island.z() - REACH - 16,
				island.x() + REACH + 16, level.getMaxBuildHeight(), island.z() + REACH + 16);
		friendlies(level, island, roster, area);
		enemies(level, island, roster, area);
	}

	// ------------------------------------------------------------------------------------------------ friendly NPCs

	private static void friendlies(ServerLevel level, Island island, IslandRoster roster, AABB area) {
		List<Islander> npcs = new ArrayList<>(level.getEntitiesOfClass(Islander.class, area,
				n -> n.isAlive() && n.islandId() == island.id()));
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
				&& (e.islandId() == island.id() || e.islandId() == Long.MIN_VALUE));
		Map<Integer, GrandLineEnemy> bySpot = new HashMap<>();
		for (GrandLineEnemy enemy : enemies) {
			int spot = spotOf(enemy);
			if (spot < 0 || spot >= roster.spots().size() || enemy.kind() != roster.kind() || bySpot.containsKey(spot)) {
				enemy.discard(); // from before the roster, the wrong kind, a boss, or a second enemy on one spot
				continue;
			}
			bySpot.put(spot, enemy);
		}
		long now = level.getGameTime();
		Map<Integer, Long> fallen = FALLEN.computeIfAbsent(island.id(), k -> new HashMap<>());
		for (int i = 0; i < roster.spots().size(); i++) {
			if (bySpot.containsKey(i)) {
				continue;
			}
			Long when = fallen.get(i);
			if (when != null && now - when < RESPAWN_TICKS && now >= when) {
				continue; // still on its own timer
			}
			fallen.remove(i);
			spawn(level, island, roster, i);
		}
	}

	private static void spawn(ServerLevel level, Island island, IslandRoster roster, int index) {
		IslandRoster.Spot spot = roster.spots().get(index);
		BlockPos at = standAt(level, island, spot.x(), Anchor.SURFACE, spot.z());
		if (!IslandSpawns.isSafe(level, at)) {
			BlockPos near = nearestSafe(level, at);
			if (near == null) {
				return;
			}
			at = near;
		}
		GrandLineEnemy enemy = GrandLineEntities.typeOf(roster.kind()).create(level);
		if (enemy == null) {
			return;
		}
		enemy.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, level.getRandom().nextFloat() * 360, 0);
		enemy.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.EVENT, null, null);
		enemy.equip();
		enemy.setGrandLineLevel(roster.minLevel() + level.getRandom().nextInt(roster.maxLevel() - roster.minLevel() + 1));
		enemy.setOrigin(at, island.id());
		enemy.addTag(SPOT_TAG + index);
		enemy.setPersistenceRequired();
		level.addFreshEntity(enemy);
	}

	private static BlockPos nearestSafe(ServerLevel level, BlockPos at) {
		for (int r = 1; r <= 2; r++) {
			for (int dx = -r; dx <= r; dx++) {
				for (int dz = -r; dz <= r; dz++) {
					BlockPos p = new BlockPos(at.getX() + dx, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, at.getX() + dx, at.getZ() + dz), at.getZ() + dz);
					if (IslandSpawns.isSafe(level, p)) {
						return p;
					}
				}
			}
		}
		return null;
	}
}
