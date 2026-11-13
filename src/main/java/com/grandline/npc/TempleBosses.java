package com.grandline.npc;

import com.grandline.GrandLineMod;
import com.grandline.entity.GrandLineEntities;
import com.grandline.entity.SaberBoss;
import com.grandline.world.Island;
import com.grandline.world.IslandRegistry;
import com.grandline.world.IslandSpawns;
import com.grandline.world.plan.Anchor;
import com.grandline.world.plan.IslandPlans;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Temple bosses (Verdant Wilds' Saber boss): one per island with a {@link Anchor.Type#TEMPLE_BOSS} spot. While a player
 * is near and the spot is loaded, the boss is there; after it is defeated it comes back {@code saberBossRespawnSeconds}
 * later (world time, so the timer survives restarts and unloaded chunks). The island remembers its current boss, so a
 * stray copy (say, one that walked into an unloaded chunk and reappears) removes itself.
 */
public final class TempleBosses {
	static final int CHECK_TICKS = 20;
	static final int NEAR = 96;

	private TempleBosses() {
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % CHECK_TICKS == 0) {
				tick(server);
			}
		});
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (entity instanceof SaberBoss boss && entity.level() instanceof ServerLevel level) {
				defeated(level, boss);
			}
		});
	}

	public static Anchor spot(Island island) {
		List<Anchor> anchors = IslandPlans.anchors(island, Anchor.Type.TEMPLE_BOSS);
		return anchors.isEmpty() ? null : anchors.get(0);
	}

	public static BlockPos home(Island island) {
		Anchor a = spot(island);
		return a == null ? null : new BlockPos(island.x() + a.x(), a.y(), island.z() + a.z());
	}

	static void defeated(ServerLevel level, SaberBoss boss) {
		Data data = Data.get(level);
		long island = boss.islandId();
		State state = data.states.computeIfAbsent(island, k -> new State());
		if (boss.getUUID().equals(state.boss)) {
			state.boss = null;
		}
		state.nextSpawn = level.getGameTime() + Math.round(GrandLineMod.config().saberBossRespawnSeconds * 20);
		data.setDirty();
	}

	private static void tick(MinecraftServer server) {
		ServerLevel level = server.overworld();
		if (level.players().isEmpty()) {
			return;
		}
		java.util.Set<Long> done = new java.util.HashSet<>();
		for (ServerPlayer player : level.players()) {
			for (Island island : IslandRegistry.nearby(server, player.getBlockX(), player.getBlockZ(), 260)) {
				BlockPos home = home(island);
				if (home != null && done.add(island.id()) && player.blockPosition().closerThan(home, NEAR)) {
					ensure(level, island);
				}
			}
		}
	}

	/** Makes sure the island's boss is present (or its respawn timer is running). Exposed for tests. */
	public static SaberBoss ensure(ServerLevel level, Island island) {
		BlockPos home = home(island);
		if (home == null || !level.isPositionEntityTicking(home)) {
			return null;
		}
		Data data = Data.get(level);
		State state = data.states.computeIfAbsent(island.id(), k -> new State());
		List<SaberBoss> found = level.getEntitiesOfClass(SaberBoss.class, new net.minecraft.world.phys.AABB(home).inflate(40),
				b -> b.isAlive() && b.islandId() == island.id());
		SaberBoss keep = null;
		for (SaberBoss b : found) {
			if (keep == null && (state.boss == null || b.getUUID().equals(state.boss))) {
				keep = b;
			}
		}
		if (keep == null && !found.isEmpty() && state.boss != null && level.getEntity(state.boss) == null) {
			keep = found.get(0); // the recorded boss is gone; adopt the one that is here
		}
		for (SaberBoss b : found) {
			if (b != keep) {
				b.discard(); // never two
			}
		}
		if (keep != null) {
			if (!keep.getUUID().equals(state.boss)) {
				state.boss = keep.getUUID();
				data.setDirty();
			}
			return keep;
		}
		if (state.boss != null) {
			Entity elsewhere = level.getEntity(state.boss);
			if (elsewhere instanceof SaberBoss b && b.isAlive()) {
				return b; // loaded but outside the search box: it walks home on its own
			}
			state.boss = null; // gone without a defeat (chunk data lost, removed by a command): replace it
			data.setDirty();
		}
		long cooldown = Math.round(GrandLineMod.config().saberBossRespawnSeconds * 20);
		if (state.nextSpawn > level.getGameTime() + cooldown) {
			state.nextSpawn = level.getGameTime() + cooldown; // the world clock went back (or the cooldown was shortened)
			data.setDirty();
		}
		if (level.getGameTime() < state.nextSpawn) {
			return null;
		}
		return spawn(level, island, home, data, state);
	}

	private static SaberBoss spawn(ServerLevel level, Island island, BlockPos home, Data data, State state) {
		SaberBoss boss = GrandLineEntities.SABER_BOSS.create(level);
		if (boss == null) {
			return null;
		}
		BlockPos at = home;
		if (!IslandSpawns.isSafe(level, at)) {
			at = null;
			for (int r = 1; r <= 3 && at == null; r++) {
				for (BlockPos p : BlockPos.betweenClosed(home.offset(-r, 0, -r), home.offset(r, 1, r))) {
					if (IslandSpawns.isSafe(level, p)) {
						at = p.immutable();
						break;
					}
				}
			}
			if (at == null) {
				GrandLineMod.LOGGER.warn("No room for the temple boss of {} at {}", island.name(), home);
				return null;
			}
		}
		Anchor a = spot(island);
		float yaw = a == null || Float.isNaN(a.yaw()) ? 0 : a.yaw();
		boss.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, yaw, 0);
		boss.setYHeadRot(yaw);
		boss.setYBodyRot(yaw);
		boss.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.EVENT, null, null);
		boss.equip();
		boss.setup(home, yaw, island.id(), island.level() + 5);
		level.addFreshEntity(boss);
		state.boss = boss.getUUID();
		data.setDirty();
		GrandLineMod.LOGGER.info("Temple boss of {} spawned at {}", island.name(), at);
		return boss;
	}

	/** World time when the island's boss may next appear (0 = whenever). For tests and the report. */
	public static long nextSpawn(ServerLevel level, long islandId) {
		State s = Data.get(level).states.get(islandId);
		return s == null ? 0 : s.nextSpawn;
	}

	/** Forgets an island's boss state (tests). */
	public static void forget(ServerLevel level, long islandId) {
		Data data = Data.get(level);
		data.states.remove(islandId);
		data.setDirty();
	}

	static final class State {
		UUID boss;
		long nextSpawn;
	}

	static final class Data extends SavedData {
		final Map<Long, State> states = new HashMap<>();

		static Data get(ServerLevel level) {
			return level.getServer().overworld().getDataStorage().computeIfAbsent(Data::load, Data::new, "grandline_temple_bosses");
		}

		static Data load(CompoundTag tag) {
			Data data = new Data();
			for (String key : tag.getAllKeys()) {
				CompoundTag e = tag.getCompound(key);
				State s = new State();
				if (e.hasUUID("boss")) {
					s.boss = e.getUUID("boss");
				}
				s.nextSpawn = e.getLong("next");
				try {
					data.states.put(Long.parseLong(key), s);
				} catch (NumberFormatException ignored) {
					// not an island entry
				}
			}
			return data;
		}

		@Override
		public CompoundTag save(CompoundTag tag) {
			states.forEach((id, s) -> {
				CompoundTag e = new CompoundTag();
				if (s.boss != null) {
					e.putUUID("boss", s.boss);
				}
				e.putLong("next", s.nextSpawn);
				tag.put(Long.toString(id), e);
			});
			return tag;
		}
	}
}
