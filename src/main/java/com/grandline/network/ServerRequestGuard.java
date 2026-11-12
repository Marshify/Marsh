package com.grandline.network;

import com.grandline.GrandLineMod;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * First line of defence for every client → server request.
 *
 * <p>Requests are processed on the server thread only (Fabric dispatches play payload handlers there), so plain maps
 * are safe. The guard rejects requests from dead/removed players and enforces a per-player, per-request-type minimum
 * interval to stop packet spam from flooding gameplay logic.
 */
public final class ServerRequestGuard {
	private static final Map<UUID, Map<PacketType<?>, Integer>> LAST_ACCEPTED_TICK = new HashMap<>();
	private static final AtomicLong REJECTED = new AtomicLong();

	private ServerRequestGuard() {
	}

	public enum Result {
		ACCEPTED,
		REJECTED_PLAYER_STATE,
		REJECTED_RATE_LIMIT,
		REJECTED_INVALID
	}

	/**
	 * Checks generic preconditions and rate limits for a request. Callers must still validate the request's own
	 * contents and call {@link #reject} for any content they refuse.
	 */
	public static Result check(ServerPlayer player, PacketType<?> type) {
		if (player.isRemoved() || player.hasDisconnected()) {
			return reject(player, type, Result.REJECTED_PLAYER_STATE);
		}

		int now = player.server.getTickCount();
		int interval = GrandLineMod.config().requestCooldownTicks;
		Map<PacketType<?>, Integer> perType = LAST_ACCEPTED_TICK.computeIfAbsent(player.getUUID(), uuid -> new HashMap<>());
		Integer last = perType.get(type);

		if (last != null && now - last < interval) {
			return reject(player, type, Result.REJECTED_RATE_LIMIT);
		}

		perType.put(type, now);
		return Result.ACCEPTED;
	}

	public static Result reject(ServerPlayer player, PacketType<?> type, Result reason) {
		REJECTED.incrementAndGet();
		GrandLineMod.LOGGER.debug("Rejected {} from {}: {}", type.getId(), player.getName().getString(), reason);
		return reason;
	}

	/** Total requests rejected since server start. Exposed for diagnostics and tests. */
	public static long rejectedCount() {
		return REJECTED.get();
	}

	/** Releases per-player state when the player disconnects. */
	public static void forget(UUID player) {
		LAST_ACCEPTED_TICK.remove(player);
	}

	/** Clears all state, e.g. when an integrated server shuts down. */
	public static void clear() {
		LAST_ACCEPTED_TICK.clear();
	}
}
