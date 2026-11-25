package com.grandline.combat;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Narrow, temporary protection against fall (and fly-into-wall) damage caused by a move's own movement (reusable by any
 * dash, launch or bounce). A move guards its user for as long as it moves them plus a short landing grace, and clears
 * the guard when it ends; guards expire on their own, are dropped on death, logout and server stop, and never cover
 * any other damage. Ordinary falls outside a guarded move hurt as usual.
 */
public final class MovementGuards {
	/** Landing grace after a guarded move ends (ticks). */
	public static final int GRACE = 20;

	private static final Map<UUID, Long> UNTIL = new HashMap<>();

	private MovementGuards() {
	}

	public static void register() {
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			if (!(entity instanceof ServerPlayer player) || !(source.is(DamageTypes.FALL) || source.is(DamageTypes.FLY_INTO_WALL))) {
				return true;
			}
			return !guarded(player);
		});
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> UNTIL.remove(entity.getUUID()));
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> UNTIL.remove(handler.getPlayer().getUUID()));
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> UNTIL.clear());
	}

	/** Protects {@code player} for the next {@code ticks} ticks (extends, never shortens, a running guard). */
	public static void guard(ServerPlayer player, int ticks) {
		long until = player.serverLevel().getGameTime() + ticks;
		UNTIL.merge(player.getUUID(), until, Math::max);
	}

	/** The move ended: keep only the landing grace. */
	public static void release(ServerPlayer player) {
		if (UNTIL.containsKey(player.getUUID())) {
			UNTIL.put(player.getUUID(), player.serverLevel().getGameTime() + GRACE);
		}
		player.fallDistance = 0;
	}

	public static boolean guarded(ServerPlayer player) {
		Long until = UNTIL.get(player.getUUID());
		if (until == null) {
			return false;
		}
		if (player.serverLevel().getGameTime() > until) {
			UNTIL.remove(player.getUUID());
			return false;
		}
		return true;
	}
}
