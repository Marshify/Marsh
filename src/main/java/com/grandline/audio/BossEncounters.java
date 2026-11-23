package com.grandline.audio;

import com.grandline.combat.CombatCredit;
import com.grandline.entity.GrandLineEnemy;
import com.grandline.entity.IslandBoss;
import com.grandline.entity.SaberBoss;
import com.grandline.network.payload.BossMusicPayload;
import com.grandline.world.FirstSeaProgression;
import com.grandline.world.Island;
import com.grandline.world.IslandRegistry;
import com.grandline.world.SeaLayout;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Who is taking part in a boss fight, for the boss music. A player joins a fight when they hurt a boss or a boss hurts
 * them; the fight lasts while the boss is alive, the player is alive within {@link #RANGE} blocks of it, and they have
 * traded blows in the last {@link #TIMEOUT_TICKS}. Being near a boss is not enough. Each player's client is told only
 * when their own state changes, so the music is per player. Server side.
 */
public final class BossEncounters {
	public static final int NONE = 0, STANDARD = 1, MAJOR = 2, FINAL = 3;
	static final double RANGE = 64;
	static final int TIMEOUT_TICKS = 30 * 20;

	private record Fight(UUID boss, int tier, int sea, long lastExchange) {
	}

	private static final Map<UUID, Fight> FIGHTS = new HashMap<>();
	/** The tier each player's client was last told (missing = 0). */
	private static final Map<UUID, Integer> SENT = new HashMap<>();

	private BossEncounters() {
	}

	public static void register() {
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			if (amount <= 0) {
				return true;
			}
			ServerPlayer attacker = CombatCredit.playerBehind(source);
			if (attacker != null && tier(entity) != NONE) {
				engage(attacker, (GrandLineEnemy) entity);
			}
			if (entity instanceof ServerPlayer victim && source.getEntity() != null && tier(source.getEntity()) != NONE) {
				engage(victim, (GrandLineEnemy) source.getEntity());
			}
			return true;
		});
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % 10 == 0) {
				tick(server);
			}
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			FIGHTS.remove(handler.getPlayer().getUUID());
			SENT.remove(handler.getPlayer().getUUID());
		});
	}

	/** The boss tier of an entity: a temple boss is major, the last First Sea island's quest boss is final. */
	public static int tier(Entity entity) {
		if (entity instanceof SaberBoss) {
			return MAJOR;
		}
		if (entity instanceof IslandBoss boss) {
			Island island = islandOf(boss);
			String last = FirstSeaProgression.STAGES.get(FirstSeaProgression.STAGES.size() - 1).island();
			return island != null && island.name().equals(last) ? FINAL : STANDARD;
		}
		return NONE;
	}

	private static Island islandOf(GrandLineEnemy enemy) {
		IslandRegistry registry = IslandRegistry.current();
		return registry == null || enemy.islandId() == Long.MIN_VALUE ? null : registry.get(enemy.islandId());
	}

	static void engage(ServerPlayer player, GrandLineEnemy boss) {
		Island island = islandOf(boss);
		int sea = island != null ? island.sea() : SeaLayout.seaAt(boss.getX());
		FIGHTS.put(player.getUUID(), new Fight(boss.getUUID(), tier(boss), sea, player.server.getTickCount()));
	}

	/** The tier of the fight this player is in now (0 none). Exposed for tests. */
	public static int current(ServerPlayer player) {
		Fight fight = FIGHTS.get(player.getUUID());
		if (fight == null || !player.isAlive() || player.server.getTickCount() - fight.lastExchange() > TIMEOUT_TICKS) {
			return NONE;
		}
		Entity boss = ((ServerLevel) player.level()).getEntity(fight.boss());
		if (boss == null || !boss.isAlive() || boss.distanceTo(player) > RANGE) {
			return NONE;
		}
		return fight.tier();
	}

	private static void tick(MinecraftServer server) {
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			UUID id = player.getUUID();
			int tier = current(player);
			if (tier == NONE) {
				FIGHTS.remove(id); // the fight is over: the island music comes back
			}
			if (SENT.getOrDefault(id, NONE) != tier && ServerPlayNetworking.canSend(player, BossMusicPayload.TYPE)) {
				Fight fight = FIGHTS.get(id);
				ServerPlayNetworking.send(player, new BossMusicPayload(tier, fight != null ? fight.sea() : 1));
				SENT.put(id, tier);
			}
		}
	}
}
