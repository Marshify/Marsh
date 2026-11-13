package com.grandline.npc;

import com.grandline.GrandLineMod;
import com.grandline.entity.EnemyKind;
import com.grandline.entity.GrandLineEnemy;
import com.grandline.entity.GrandLineEntities;
import com.grandline.entity.IslandBoss;
import com.grandline.entity.SaberBoss;
import com.grandline.world.Island;
import com.grandline.world.IslandRegistry;
import com.grandline.world.IslandSpawns;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Regular island enemies (bandits, raiders, corsairs) come back {@code enemyRespawnSeconds} after they die, at the spot
 * the island placed them, with the same kind and level. Each death queues exactly one respawn, so the island never ends
 * up with more enemies than it had. The queue is saved with the world, and a respawn whose spot is not loaded waits
 * until it is. Bosses never use this (they have their own timers).
 */
public final class EnemyRespawns {
	private EnemyRespawns() {
	}

	record Pending(EnemyKind kind, int level, BlockPos pos, long islandId, long due) {
	}

	public static void register() {
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (entity instanceof GrandLineEnemy enemy && entity.level() instanceof ServerLevel level
					&& level.dimension() == Level.OVERWORLD) {
				onDeath(level, enemy);
			}
		});
		ServerTickEvents.END_SERVER_TICK.register(EnemyRespawns::tick); // every tick, so the delay is exact (cheap when idle)
	}

	/** Whether this enemy is a regular island enemy that respawns. */
	public static boolean respawns(GrandLineEnemy enemy) {
		return !(enemy instanceof IslandBoss) && !(enemy instanceof SaberBoss) && enemy.kind() != EnemyKind.CAPTAIN;
	}

	static void onDeath(ServerLevel level, GrandLineEnemy enemy) {
		if (!respawns(enemy)) {
			return;
		}
		BlockPos origin = enemy.origin();
		long islandId = enemy.islandId();
		if (origin == null) {
			// Enemies from before respawning existed: they belong to the island they fell on, back at one of its camps.
			Island island = islandAt(level, enemy.blockPosition());
			if (island == null) {
				return; // not an island enemy (spawn egg, command)
			}
			islandId = island.id();
			origin = IslandPopulation.campSpot(level, island, level.getRandom());
			if (origin == null) {
				origin = enemy.blockPosition();
			}
		}
		long delay = Math.round(GrandLineMod.config().enemyRespawnSeconds * 20);
		Data data = Data.get(level);
		data.pending.add(new Pending(enemy.kind(), enemy.grandLineLevel(), origin, islandId, level.getGameTime() + delay));
		data.setDirty();
	}

	private static Island islandAt(ServerLevel level, BlockPos pos) {
		for (Island island : IslandRegistry.nearby(level.getServer(), pos.getX(), pos.getZ(), 260)) {
			long dx = pos.getX() - island.x(), dz = pos.getZ() - island.z();
			var plan = com.grandline.world.plan.IslandPlans.get(island);
			long r = (plan != null ? plan.extent() : island.radius()) + 16;
			if (dx * dx + dz * dz <= r * r) {
				return island;
			}
		}
		return null;
	}

	/** Respawns queued for the island (they count toward its enemy limit). */
	public static int pending(ServerLevel level, long islandId) {
		int n = 0;
		for (Pending p : Data.get(level).pending) {
			if (p.islandId() == islandId) {
				n++;
			}
		}
		return n;
	}

	static void tick(MinecraftServer server) {
		ServerLevel level = server.overworld();
		Data data = Data.get(level);
		if (data.pending.isEmpty()) {
			return;
		}
		long now = level.getGameTime();
		for (Iterator<Pending> it = data.pending.iterator(); it.hasNext(); ) {
			Pending p = it.next();
			if (p.due() > now || !level.isPositionEntityTicking(p.pos())) {
				continue;
			}
			it.remove(); // removed before spawning: one death, one respawn
			data.setDirty();
			spawn(level, p);
		}
	}

	private static void spawn(ServerLevel level, Pending p) {
		GrandLineEnemy enemy = GrandLineEntities.typeOf(p.kind()).create(level);
		if (enemy == null) {
			return;
		}
		BlockPos at = IslandSpawns.isSafe(level, p.pos()) ? p.pos() : surface(level, p.pos());
		enemy.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, level.getRandom().nextFloat() * 360, 0);
		enemy.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.EVENT, null, null);
		enemy.equip();
		enemy.setGrandLineLevel(p.level());
		enemy.setOrigin(p.pos(), p.islandId());
		level.addFreshEntity(enemy);
	}

	/** The surface at the spot (a block was placed or removed there since): stand on top. */
	private static BlockPos surface(ServerLevel level, BlockPos pos) {
		BlockPos top = new BlockPos(pos.getX(), level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ()), pos.getZ());
		return IslandSpawns.isSafe(level, top) ? top : pos;
	}

	/** Queued respawns, for tests. */
	public static int queued(ServerLevel level) {
		return Data.get(level).pending.size();
	}

	static final class Data extends SavedData {
		final List<Pending> pending = new ArrayList<>();

		static Data get(ServerLevel level) {
			return level.getServer().overworld().getDataStorage().computeIfAbsent(Data::load, Data::new, "grandline_enemy_respawns");
		}

		static Data load(CompoundTag tag) {
			Data data = new Data();
			ListTag list = tag.getList("pending", Tag.TAG_COMPOUND);
			for (int i = 0; i < list.size(); i++) {
				CompoundTag e = list.getCompound(i);
				EnemyKind kind;
				try {
					kind = EnemyKind.valueOf(e.getString("kind"));
				} catch (IllegalArgumentException ex) {
					continue;
				}
				data.pending.add(new Pending(kind, e.getInt("level"), BlockPos.of(e.getLong("pos")), e.getLong("island"), e.getLong("due")));
			}
			return data;
		}

		@Override
		public CompoundTag save(CompoundTag tag) {
			ListTag list = new ListTag();
			for (Pending p : pending) {
				CompoundTag e = new CompoundTag();
				e.putString("kind", p.kind().name());
				e.putInt("level", p.level());
				e.putLong("pos", p.pos().asLong());
				e.putLong("island", p.islandId());
				e.putLong("due", p.due());
				list.add(e);
			}
			tag.put("pending", list);
			return tag;
		}
	}
}
