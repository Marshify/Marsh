package com.grandline.world;

import com.grandline.GrandLineMod;
import com.grandline.data.ModAttachments;
import com.grandline.world.plan.Anchor;
import com.grandline.world.plan.IslandPlan;
import com.grandline.world.plan.IslandPlans;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Where players appear: new players start on the spawn island (Millbrook Village by default); after death a player
 * respawns on the island they died on, else on their home island, else on the spawn island. Spots come from the
 * island plan (its arrival point, then its NPC spots, then its centre) and are checked for a solid floor and two free
 * blocks before use; chunks are generated first. Only active in worlds whose registry has the spawn island.
 */
public final class IslandSpawns {
	private static final Map<UUID, Long> DEATH_ISLAND = new HashMap<>();

	private IslandSpawns() {
	}

	public static void register() {
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> firstJoin(handler.getPlayer()));
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (entity instanceof ServerPlayer player) {
				Island island = player.level().dimension() == Level.OVERWORLD ? islandAt(player.serverLevel(), player.getBlockX(), player.getBlockZ()) : null;
				DEATH_ISLAND.put(player.getUUID(), island == null ? -1L : island.id());
			}
		});
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
			Long died = DEATH_ISLAND.remove(newPlayer.getUUID());
			if (!alive && GrandLineMod.config().islandRespawn) {
				respawn(newPlayer, died == null ? -1L : died);
			}
		});
	}

	public static Island spawnIsland(net.minecraft.server.MinecraftServer server) {
		String name = GrandLineMod.config().spawnIsland;
		for (Island island : IslandRegistry.get(server).all()) {
			if (island.name().equals(name)) {
				return island;
			}
		}
		return null;
	}

	/** The registered island whose land contains column (x, z), or null (open sea, or not on an island). */
	public static Island islandAt(ServerLevel level, int x, int z) {
		for (Island island : IslandRegistry.nearby(level.getServer(), x, z, 260)) {
			if (ModWorld.inGameplayArea(x, z, island)) {
				return island;
			}
		}
		return null;
	}

	/** The island this respawn should use: where they died, else home, else the spawn island (may be null). */
	public static Island respawnIsland(ServerPlayer player, long diedOn) {
		IslandRegistry registry = IslandRegistry.get(player.server);
		Island island = diedOn >= 0 ? registry.get(diedOn) : null;
		if (island == null) {
			Long home = player.getAttached(ModAttachments.HOME_ISLAND);
			island = home == null ? null : registry.get(home);
		}
		return island != null ? island : spawnIsland(player.server);
	}

	private static void respawn(ServerPlayer player, long diedOn) {
		Island island = respawnIsland(player, diedOn);
		if (island != null) {
			moveTo(player, island);
		}
	}

	/** First join ever: start on the spawn island. Players who already played keep where they are. */
	private static void firstJoin(ServerPlayer player) {
		if (Boolean.TRUE.equals(player.getAttached(ModAttachments.FIRST_SPAWN_DONE))) {
			return;
		}
		player.setAttached(ModAttachments.FIRST_SPAWN_DONE, true);
		boolean brandNew = player.getStats().getValue(Stats.CUSTOM.get(Stats.PLAY_TIME)) < 40;
		Island island = spawnIsland(player.server);
		if (brandNew && island != null) {
			moveTo(player, island);
		}
	}

	public static boolean moveTo(ServerPlayer player, Island island) {
		ServerLevel overworld = player.server.overworld();
		BlockPos spot = safeSpot(overworld, island);
		if (spot == null) {
			return false;
		}
		player.teleportTo(overworld, spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, player.getYRot(), 0);
		player.resetFallDistance();
		return true;
	}

	/** A safe standing spot on the island (arrival, NPC spots, centre; each searched nearby), or null. */
	public static BlockPos safeSpot(ServerLevel level, Island island) {
		List<BlockPos> candidates = new ArrayList<>();
		IslandPlan plan = IslandPlans.get(island);
		BlockPos arrival = IslandPlans.arrival(island.name(), island.x(), island.z());
		if (arrival != null) {
			candidates.add(arrival);
		}
		if (plan != null) {
			for (Anchor.Type type : new Anchor.Type[]{Anchor.Type.QUEST, Anchor.Type.GUNSMITH, Anchor.Type.FRUIT_DEALER}) {
				for (Anchor a : plan.anchors()) {
					if (a.type() == type) {
						candidates.add(new BlockPos(island.x() + a.x(), a.y() == Anchor.SURFACE ? Integer.MIN_VALUE : a.y(), island.z() + a.z()));
					}
				}
			}
		}
		candidates.add(new BlockPos(island.x(), Integer.MIN_VALUE, island.z()));
		for (BlockPos c : candidates) {
			level.getChunk(c.getX() >> 4, c.getZ() >> 4); // generate before reading
			if (c.getY() != Integer.MIN_VALUE && isSafe(level, c)) {
				return c;
			}
			BlockPos found = searchNear(level, c.getX(), c.getZ(), 8);
			if (found != null) {
				return found;
			}
		}
		return null;
	}

	private static BlockPos searchNear(ServerLevel level, int cx, int cz, int radius) {
		for (int r = 0; r <= radius; r++) {
			for (int dx = -r; dx <= r; dx++) {
				for (int dz = -r; dz <= r; dz++) {
					if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
						continue;
					}
					int x = cx + dx, z = cz + dz;
					level.getChunk(x >> 4, z >> 4);
					BlockPos p = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
					if (isSafe(level, p)) {
						return p;
					}
				}
			}
		}
		return null;
	}

	/** Solid, dry floor below; feet and head free of blocks and fluids. */
	public static boolean isSafe(ServerLevel level, BlockPos feet) {
		BlockPos below = feet.below();
		return !level.getBlockState(below).getCollisionShape(level, below).isEmpty() && level.getFluidState(below).isEmpty()
				&& free(level, feet) && free(level, feet.above());
	}

	private static boolean free(ServerLevel level, BlockPos p) {
		return level.getBlockState(p).getCollisionShape(level, p).isEmpty() && level.getFluidState(p).isEmpty();
	}
}
