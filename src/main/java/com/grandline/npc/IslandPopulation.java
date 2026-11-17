package com.grandline.npc;

import com.grandline.world.FirstSeaProgression;

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
		// Enemies saved before their island was retuned (Millbrook's Lv 30 bandits) take the island's level when they load.
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			if (entity instanceof GrandLineEnemy enemy && EnemyRespawns.respawns(enemy) && level.dimension() == Level.OVERWORLD) {
				Island island = enemy.islandId() != Long.MIN_VALUE ? IslandRegistry.get(level.getServer()).get(enemy.islandId())
						: EnemyRespawns.islandAt(level, enemy.blockPosition());
				BlockPos home = enemy.origin() != null ? enemy.origin() : enemy.blockPosition();
				if (island != null && com.grandline.world.plan.IslandPlans.get(island) != null
						&& FirstSeaProgression.stage(island.name(), FirstSeaProgression.LOWER) != null && enemy.kind() != kindAt(island, home)) {
					enemy.discard(); // a kind this island no longer has (it moved up the ladder): the island refills with the right kind
					return;
				}
				if (island != null && com.grandline.world.plan.IslandPlans.get(island) != null && outOfLine(enemy.grandLineLevel(), island)) {
					float ratio = enemy.getHealth() / enemy.getMaxHealth();
					enemy.setGrandLineLevel(levelAt(island, enemy.origin() != null ? enemy.origin() : enemy.blockPosition(), level.getRandom()));
					enemy.setHealth(enemy.getMaxHealth() * ratio);
				}
			}
		});
	}

	private static void placeNpcs(ServerLevel level, Island island) {
		Island current = IslandRegistry.get(level.getServer()).get(island.id());
		if (current == null || current.npcsSpawned()) {
			return;
		}
		com.grandline.world.plan.IslandPlan plan = com.grandline.world.plan.IslandPlans.get(island);
		if (plan != null) {
			placePlannedNpcs(level, island, plan);
			IslandRegistry.get(level.getServer()).update(current.withNpcsSpawned());
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

	/** NPCs (and a boat at each dock) at the spots the island's plan marks for them. */
	private static void placePlannedNpcs(ServerLevel level, Island island, com.grandline.world.plan.IslandPlan plan) {
		for (com.grandline.world.plan.Anchor anchor : plan.anchors()) {
			Islander.Role role = switch (anchor.type()) {
				case QUEST -> Islander.Role.QUEST_GIVER;
				case FRUIT_DEALER -> Islander.Role.FRUIT_DEALER;
				case FISHERMAN -> Islander.Role.FISHERMAN;
				case NAVIGATOR -> island.starter() ? Islander.Role.NAVIGATOR : null;
				case GACHA -> island.starter() ? Islander.Role.GACHA : null;
				case GUNSMITH -> Islander.Role.GUNSMITH;
				default -> null;
			};
			if (role == Islander.Role.QUEST_GIVER && anchor.region() != FirstSeaProgression.LOWER) {
				continue; // a region's own quest giver is kept in place by KeeperNpcs (also in worlds already populated)
			}
			if (role != null) {
				spawnNpcAt(level, island, role, anchor);
			} else if (anchor.type() == com.grandline.world.plan.Anchor.Type.DOCK) {
				net.minecraft.world.entity.vehicle.Boat boat = net.minecraft.world.entity.EntityType.BOAT.create(level);
				if (boat != null) {
					boat.moveTo(island.x() + anchor.x() + 0.5, com.grandline.world.plan.Terrain.WATER_TOP + 1, island.z() + anchor.z() + 0.5, 0, 0);
					level.addFreshEntity(boat);
				}
			}
		}
	}

	private static void spawnNpcAt(ServerLevel level, Island island, Islander.Role role, com.grandline.world.plan.Anchor anchor) {
		Islander npc = GrandLineEntities.ISLANDER.create(level);
		if (npc == null) {
			return;
		}
		int x = island.x() + anchor.x();
		int z = island.z() + anchor.z();
		int y = anchor.y() != com.grandline.world.plan.Anchor.SURFACE ? anchor.y()
				: level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
		if (anchor.y() == com.grandline.world.plan.Anchor.SURFACE && !level.hasChunk(x >> 4, z >> 4)) {
			// The spot's chunk isn't generated yet (the height map would put the NPC at the bottom of the world):
			// stand it on the plan's own ground instead.
			com.grandline.world.plan.IslandPlan plan = com.grandline.world.plan.IslandPlans.get(island);
			if (plan != null) {
				y = plan.terrain().ground(anchor.x(), anchor.z()) + 1;
			}
		}
		float yaw = Float.isNaN(anchor.yaw()) ? level.getRandom().nextFloat() * 360 : anchor.yaw();
		npc.moveTo(x + 0.5, y, z + 0.5, yaw, 0);
		npc.setYHeadRot(yaw);
		npc.setYBodyRot(yaw);
		npc.setup(role, island.id(), island.level());
		npc.setRegion(anchor.region());
		npc.setPersistenceRequired();
		level.addFreshEntity(npc);
	}

	/** Random spawn point inside one of the island's enemy camps, or null if it has none. */
	/** Regular enemies alive on (and just around) the island. */
	public static int enemiesOn(ServerLevel level, Island island) {
		com.grandline.world.plan.IslandPlan plan = com.grandline.world.plan.IslandPlans.get(island);
		int r = plan != null ? Math.max(island.radius(), plan.extent()) : island.radius();
		AABB area = new AABB(island.x() - r - 8, level.getMinBuildHeight(), island.z() - r - 8, island.x() + r + 8,
				level.getMaxBuildHeight(), island.z() + r + 8);
		return level.getEntitiesOfClass(GrandLineEnemy.class, area, EnemyRespawns::respawns).size();
	}

	/** A level around the island's recommended level (a tenth either way). */
	public static int levelFor(Island island, RandomSource random) {
		FirstSeaProgression.Stage stage = FirstSeaProgression.stage(island.name(), FirstSeaProgression.LOWER);
		if (stage != null) {
			return stage.enemyLevel(0, random);
		}
		int spread = Math.max(1, island.level() / 10);
		return Math.max(1, island.level() - spread + random.nextInt(spread * 2 + 1));
	}

	/**
	 * The level for an enemy whose camp is nearest {@code at}: on the First Sea ladder, the camp's region picks the stage
	 * and its place among that region's camps picks the band (the first camp holds the basic enemies, later camps the
	 * stronger ones). Off the ladder: around the island's level.
	 */
	public static int levelAt(Island island, BlockPos at, RandomSource random) {
		java.util.List<com.grandline.world.plan.Anchor> camps = com.grandline.world.plan.IslandPlans.anchors(island,
				com.grandline.world.plan.Anchor.Type.ENEMY_CAMP);
		com.grandline.world.plan.Anchor nearest = null;
		double best = Double.MAX_VALUE;
		for (com.grandline.world.plan.Anchor camp : camps) {
			double d = at == null ? 0 : Math.pow(island.x() + camp.x() - at.getX(), 2) + Math.pow(island.z() + camp.z() - at.getZ(), 2);
			if (d < best) {
				best = d;
				nearest = camp;
			}
		}
		if (nearest == null) {
			return levelFor(island, random);
		}
		FirstSeaProgression.Stage stage = FirstSeaProgression.stage(island.name(), nearest.region());
		if (stage == null) {
			return levelFor(island, random);
		}
		int band = 0;
		for (com.grandline.world.plan.Anchor camp : camps) {
			if (camp == nearest) {
				break;
			}
			if (camp.region() == nearest.region()) {
				band = 1;
			}
		}
		return stage.enemyLevel(band, random);
	}

	/** The progression region of the camp nearest {@code at} (the island's main region if it has no camps). */
	public static int regionAt(Island island, BlockPos at) {
		com.grandline.world.plan.Anchor nearest = null;
		double best = Double.MAX_VALUE;
		for (com.grandline.world.plan.Anchor camp : com.grandline.world.plan.IslandPlans.anchors(island, com.grandline.world.plan.Anchor.Type.ENEMY_CAMP)) {
			double d = at == null ? 0 : Math.pow(island.x() + camp.x() - at.getX(), 2) + Math.pow(island.z() + camp.z() - at.getZ(), 2);
			if (d < best) {
				best = d;
				nearest = camp;
			}
		}
		return nearest == null ? FirstSeaProgression.LOWER : nearest.region();
	}

	/** The regular enemy kind for the camp nearest {@code at}: by its stage's level (quests ask for the same kind). */
	public static EnemyKind kindAt(Island island, BlockPos at) {
		FirstSeaProgression.Stage stage = FirstSeaProgression.stage(island.name(), regionAt(island, at));
		return EnemyKind.forLevel(stage != null ? stage.min() : island.level());
	}

	/** Whether an enemy's level is out of line with its island's (the island was retuned): it is then re-levelled. */
	public static boolean outOfLine(int level, Island island) {
		int[] span = FirstSeaProgression.islandSpan(island.name());
		if (span != null) {
			return level < span[0] || level > span[1];
		}
		int spread = Math.max(1, island.level() / 10);
		return Math.abs(level - island.level()) > spread;
	}

	/** Keeps a camp enemy wandering inside its camp (it still chases anyone who comes for it). */
	static void leash(GrandLineEnemy enemy, Island island) {
		java.util.List<com.grandline.world.plan.Anchor> camps = com.grandline.world.plan.IslandPlans.anchors(island,
				com.grandline.world.plan.Anchor.Type.ENEMY_CAMP);
		if (!camps.isEmpty() && enemy.origin() != null) {
			int radius = 0;
			for (com.grandline.world.plan.Anchor camp : camps) {
				radius = Math.max(radius, camp.radius());
			}
			enemy.restrictTo(enemy.origin(), radius + 6);
		}
	}

	/** A spot at one of the island's camps within 128 blocks of {@code near} (the closest region's), or null. */
	static BlockPos campSpotNear(ServerLevel level, Island island, RandomSource random, BlockPos near) {
		java.util.List<com.grandline.world.plan.Anchor> camps = new java.util.ArrayList<>();
		for (com.grandline.world.plan.Anchor a : com.grandline.world.plan.IslandPlans.anchors(island, com.grandline.world.plan.Anchor.Type.ENEMY_CAMP)) {
			if (Math.hypot(island.x() + a.x() - near.getX(), island.z() + a.z() - near.getZ()) <= 128) {
				camps.add(a);
			}
		}
		if (camps.isEmpty()) {
			return null;
		}
		return spotAt(level, island, camps.get(random.nextInt(camps.size())), random);
	}

	static BlockPos spotAt(ServerLevel level, Island island, com.grandline.world.plan.Anchor camp, RandomSource random) {
		double angle = random.nextDouble() * Math.PI * 2;
		double distance = random.nextDouble() * Math.max(1, camp.radius());
		int x = island.x() + camp.x() + (int) Math.round(Math.cos(angle) * distance);
		int z = island.z() + camp.z() + (int) Math.round(Math.sin(angle) * distance);
		int y = camp.y() != com.grandline.world.plan.Anchor.SURFACE ? camp.y() + 1 : level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
		return new BlockPos(x, y, z);
	}

	static BlockPos campSpot(ServerLevel level, Island island, RandomSource random) {
		java.util.List<com.grandline.world.plan.Anchor> camps = com.grandline.world.plan.IslandPlans.anchors(island,
				com.grandline.world.plan.Anchor.Type.ENEMY_CAMP);
		if (camps.isEmpty()) {
			return null;
		}
		com.grandline.world.plan.Anchor camp = camps.get(random.nextInt(camps.size()));
		double angle = random.nextDouble() * Math.PI * 2;
		double distance = random.nextDouble() * Math.max(1, camp.radius());
		int x = island.x() + camp.x() + (int) Math.round(Math.cos(angle) * distance);
		int z = island.z() + camp.z() + (int) Math.round(Math.sin(angle) * distance);
		int y = camp.y() != com.grandline.world.plan.Anchor.SURFACE ? camp.y() + 1 : level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
		return new BlockPos(x, y, z);
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
			for (Island island : IslandRegistry.reaching(server, player.getBlockX(), player.getBlockZ(), 0)) {
				if (island.starter()) {
					continue; // safe zone
				}
				boolean nearCentre = Math.hypot(island.x() - player.getX(), island.z() - player.getZ()) <= 64;
				int x;
				int z;
				int y;
				BlockPos camp = campSpotNear(level, island, random, player.blockPosition());
				if (camp == null && nearCentre) {
					camp = campSpot(level, island, random);
				}
				if (camp != null) {
					x = camp.getX();
					z = camp.getZ();
					y = camp.getY();
				} else if (nearCentre && com.grandline.world.plan.IslandPlans.anchors(island, com.grandline.world.plan.Anchor.Type.ENEMY_CAMP).isEmpty()) {
					double angle = random.nextDouble() * Math.PI * 2;
					double distance = random.nextDouble() * island.radius() * 0.7;
					x = island.x() + (int) Math.round(Math.cos(angle) * distance);
					z = island.z() + (int) Math.round(Math.sin(angle) * distance);
					y = Integer.MIN_VALUE;
				} else {
					continue; // the player is out on the island, away from any camp
				}
				// At most MAX_ENEMIES_PER_ISLAND around this spot (each region of a large island fills on its own).
				AABB area = new AABB(x - 72, level.getMinBuildHeight(), z - 72, x + 72, level.getMaxBuildHeight(), z + 72);
				if (level.getEntitiesOfClass(GrandLineEnemy.class, area, e -> !(e instanceof com.grandline.entity.IslandBoss)
						&& !(e instanceof com.grandline.entity.SaberBoss)).size() + EnemyRespawns.pending(level, island.id())
						>= MAX_ENEMIES_PER_ISLAND) {
					continue;
				}
				if (!level.hasChunk(x >> 4, z >> 4)) {
					continue;
				}
				if (y == Integer.MIN_VALUE) {
					y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
				}
				BlockPos ground = new BlockPos(x, y - 1, z);
				if (!level.getFluidState(ground).isEmpty() || level.getBlockState(ground).is(Blocks.WATER)
						|| player.distanceToSqr(x, y, z) < 12 * 12) {
					continue;
				}
				EnemyKind kind = kindAt(island, new BlockPos(x, y, z));
				GrandLineEnemy enemy = GrandLineEntities.typeOf(kind).create(level);
				if (enemy == null) {
					continue;
				}
				enemy.moveTo(x + 0.5, y, z + 0.5, random.nextFloat() * 360, 0);
				enemy.finalizeSpawn(level, level.getCurrentDifficultyAt(enemy.blockPosition()), MobSpawnType.EVENT, null, null);
				enemy.equip();
				enemy.setGrandLineLevel(levelAt(island, new BlockPos(x, y, z), random));
				enemy.setOrigin(new BlockPos(x, y, z), island.id());
				leash(enemy, island);
				level.addFreshEntity(enemy);
			}
		}
	}
}
