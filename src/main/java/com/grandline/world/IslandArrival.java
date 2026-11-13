package com.grandline.world;

import com.grandline.network.payload.IslandArrivalPayload;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Island arrival banners. Twice a second each player's island is looked up (the island whose land they stand on);
 * stepping onto a different island than the last one announced sends its name. The last island is only forgotten
 * after {@link #FORGET_TICKS} away from it (water inside its outline does not count), so walking along a shoreline, a pier or
 * through a fountain never makes the banner flicker.
 * A player who joins standing on an island gets its banner too.
 */
public final class IslandArrival {
	static final int CHECK_TICKS = 10;
	static final int FORGET_TICKS = 200;

	private static final class State {
		long announced = -1;
		Island last;
		int offIsland;
	}

	private static final Map<UUID, State> STATES = new HashMap<>();

	private IslandArrival() {
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(IslandArrival::tick);
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> STATES.remove(handler.getPlayer().getUUID()));
	}

	private static void tick(MinecraftServer server) {
		if (server.getTickCount() % CHECK_TICKS != 0) {
			return;
		}
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			State state = STATES.computeIfAbsent(player.getUUID(), u -> new State());
			Island island = player.level().dimension() == Level.OVERWORLD && !player.isSpectator()
					? IslandSpawns.islandAt(player.serverLevel(), player.getBlockX(), player.getBlockZ()) : null;
			if (island == null) {
				// Water inside the island's outline (a fountain, a harbour basin) does not count as having left it.
				if (state.last == null || !withinOutline(player, state.last)) {
					state.offIsland += CHECK_TICKS;
				}
				if (state.offIsland >= FORGET_TICKS) {
					state.announced = -1;
					state.last = null;
				}
				continue;
			}
			state.offIsland = 0;
			if (island.id() != state.announced) {
				state.announced = island.id();
				state.last = island;
				if (ServerPlayNetworking.canSend(player, IslandArrivalPayload.TYPE)) {
					ServerPlayNetworking.send(player, new IslandArrivalPayload(island.name(), island.level(), island.sea()));
				}
			}
		}
	}

	private static boolean withinOutline(ServerPlayer player, Island island) {
		var plan = com.grandline.world.plan.IslandPlans.get(island);
		long r = plan != null ? plan.extent() : island.radius();
		long dx = player.getBlockX() - island.x();
		long dz = player.getBlockZ() - island.z();
		return dx * dx + dz * dz <= r * r;
	}
}
