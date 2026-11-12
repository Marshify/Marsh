package com.grandline.npc;

import com.grandline.entity.EnemyKind;
import com.grandline.entity.GrandLineEnemy;
import com.grandline.entity.GrandLineEntities;
import com.grandline.entity.Islander;
import com.grandline.world.Island;
import com.grandline.world.IslandRegistry;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

/**
 * Brings islands to life: places NPCs the first time an island's centre chunk loads, and keeps a few level-appropriate
 * enemies around non-starter islands while players are near. The starter island is a safe zone.
 */
public final class IslandPopulation {
	public static final int MAX_ENEMIES_PER_ISLAND = 4;

	private static final java.util.Queue<Long> PENDING = new java.util.concurrent.ConcurrentLinkedQueue<>();

	private IslandPopulation() {
	}

	public static void register() {
		ServerChunkEvents.CHUNK_LOAD.register((level, chunk) -> {
			if (level.dimension() != Level.OVERWORLD) {
				return;
			}
			// Island ids are the chunk position of their centre chunk.
			Island island = IslandRegistry.get(level.getServer()).get(chunk.getPos().toLong());
			if (island != null && !island.npcsSpawned()) {
				// Never touch the world inside a chunk-load callback (the chunk is not ready yet; doing so deadlocks).
				PENDING.add(island.id());
			}
		});
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			Long id;
			while ((id = PENDING.poll()) != null) {
				Island island = IslandRegistry.get(server).get(id);
				ServerLevel overworld = server.overworld();
				if (island != null && overworld.hasChunk(island.x() >> 4, island.z() >> 4)) {
					placeNpcs(overworld, island);
				}
			}
		});
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % 100 == 0) {
				spawnEnemies(server);
			}
		});
	}

	private static void placeNpcs(ServerLevel level, Island island) {
		Island current = IslandRegistry.get(level.getServer()).get(island.id());
		if (current == null || current.npcsSpawned()) {
			return;
		}
		spawnNpc(level, island, Islander.Role.QUEST_GIVER, 0, 0);
		if (island.starter() || Math.floorMod(island.id(), 4) == 0) {
			spawnNpc(level, island, Islander.Role.FRUIT_DEALER, 3, 1);
		}
		if (island.starter() || Math.floorMod(island.id(), 3) == 0) {
			spawnNpc(level, island, Islander.Role.FISHERMAN, -3, -1);
		}
		if (island.starter() && island.designed()) {
			spawnNpc(level, island, Islander.Role.NAVIGATOR, 1, -4);
		}
		IslandRegistry.get(level.getServer()).update(current.withNpcsSpawned());
	}

	private static void spawnNpc(ServerLevel level, Island island, Islander.Role role, int dx, int dz) {
		Islander npc = GrandLineEntities.ISLANDER.create(level);
		if (npc == null) {
			return;
		}
		int x = island.x() + dx;
		int z = island.z() + dz;
		int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
		npc.moveTo(x + 0.5, y, z + 0.5, level.getRandom().nextFloat() * 360, 0);
		npc.setup(role, island.id(), island.level());
		level.addFreshEntity(npc);
	}

	private static void spawnEnemies(MinecraftServer server) {
		ServerLevel level = server.overworld();
		RandomSource random = level.getRandom();
		for (ServerPlayer player : level.players()) {
			for (Island island : IslandRegistry.nearby(server, player.getBlockX(), player.getBlockZ(), 64)) {
				if (island.starter()) {
					continue; // safe zone
				}
				int r = island.radius();
				AABB area = new AABB(island.x() - r - 8, level.getMinBuildHeight(), island.z() - r - 8, island.x() + r + 8,
						level.getMaxBuildHeight(), island.z() + r + 8);
				if (level.getEntitiesOfClass(GrandLineEnemy.class, area).size() >= MAX_ENEMIES_PER_ISLAND) {
					continue;
				}
				double angle = random.nextDouble() * Math.PI * 2;
				double distance = random.nextDouble() * r * 0.7;
				int x = island.x() + (int) Math.round(Math.cos(angle) * distance);
				int z = island.z() + (int) Math.round(Math.sin(angle) * distance);
				if (!level.hasChunk(x >> 4, z >> 4)) {
					continue;
				}
				int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
				BlockPos ground = new BlockPos(x, y - 1, z);
				if (!level.getFluidState(ground).isEmpty() || level.getBlockState(ground).is(Blocks.WATER)
						|| player.distanceToSqr(x, y, z) < 12 * 12) {
					continue;
				}
				EnemyKind kind = EnemyKind.forLevel(island.level());
				GrandLineEnemy enemy = GrandLineEntities.typeOf(kind).create(level);
				if (enemy == null) {
					continue;
				}
				enemy.moveTo(x + 0.5, y, z + 0.5, random.nextFloat() * 360, 0);
				enemy.finalizeSpawn(level, level.getCurrentDifficultyAt(enemy.blockPosition()), MobSpawnType.EVENT, null, null);
				enemy.equip();
				int spread = Math.max(1, island.level() / 10);
				enemy.setGrandLineLevel(Math.max(1, island.level() - spread + random.nextInt(spread * 2 + 1)));
				level.addFreshEntity(enemy);
			}
		}
	}
}
