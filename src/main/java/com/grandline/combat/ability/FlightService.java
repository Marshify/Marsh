package com.grandline.combat.ability;

import com.grandline.combat.EnergyService;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Ability flight (e.g. Flame's Fire Flight): temporarily grants vanilla creative-style flight so the player steers
 * with normal controls (look + WASD, Space/Shift to climb/descend), while the server drains energy, draws the trail,
 * enforces the time limit, and ends it safely. Vanilla's own server movement checks still apply.
 */
public final class FlightService {
	/** Per-tick behaviour while flying (trail particles, etc.). */
	@FunctionalInterface
	public interface FlightTick {
		void tick(ServerPlayer player, int ticks);
	}

	public record Flight(Power power, AbilityDef ability, int startTick, int maxTicks, double drainPerTick, float speed, FlightTick trail) {
	}

	public static final float DEFAULT_FLY_SPEED = 0.05f;

	private static final Map<UUID, Flight> FLYING = new HashMap<>();

	private FlightService() {
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(FlightService::tick);
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> stop(handler.getPlayer(), false));
		// Safety net: a player who logged out mid-flight must never keep survival flight.
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayer player = handler.getPlayer();
			if (!FLYING.containsKey(player.getUUID()) && player.getAbilities().mayfly && !player.isCreative() && !player.isSpectator()) {
				revoke(player);
			}
		});
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> server.getPlayerList().getPlayers().forEach(p -> stop(p, false)));
	}

	public static boolean isFlying(ServerPlayer player) {
		return FLYING.containsKey(player.getUUID());
	}

	public static void start(ServerPlayer player, Power power, AbilityDef ability, int maxTicks, double drainPerTick, float speed,
			FlightTick trail) {
		FLYING.put(player.getUUID(), new Flight(power, ability, player.server.getTickCount(), maxTicks, drainPerTick, speed, trail));
		player.getAbilities().mayfly = true;
		player.getAbilities().flying = true;
		player.getAbilities().setFlyingSpeed(speed);
		player.onUpdateAbilities();
		player.setDeltaMovement(player.getDeltaMovement().add(0, 0.5, 0));
		player.hurtMarked = true;
	}

	/** Ends the flight. Starts the move's cooldown when {@code cooldown} is true. */
	public static void stop(ServerPlayer player, boolean cooldown) {
		Flight flight = FLYING.remove(player.getUUID());
		if (flight == null) {
			return;
		}
		revoke(player);
		player.fallDistance = 0;
		player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 60, 0, false, false));
		if (cooldown) {
			AbilityCooldownService.start(player, flight.power().abilityId(flight.ability()), flight.ability().cooldownTicks());
		}
	}

	private static void revoke(ServerPlayer player) {
		if (!player.isCreative() && !player.isSpectator()) {
			player.getAbilities().mayfly = false;
			player.getAbilities().flying = false;
		}
		player.getAbilities().setFlyingSpeed(DEFAULT_FLY_SPEED);
		player.onUpdateAbilities();
	}

	private static void tick(MinecraftServer server) {
		if (FLYING.isEmpty()) {
			return;
		}
		int now = server.getTickCount();
		for (Map.Entry<UUID, Flight> entry : Map.copyOf(FLYING).entrySet()) {
			ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
			Flight flight = entry.getValue();
			if (player == null) {
				FLYING.remove(entry.getKey());
				continue;
			}
			int ticks = now - flight.startTick();
			if (!player.isAlive() || ticks >= flight.maxTicks() || !EnergyService.trySpendQuiet(player, flight.drainPerTick())) {
				stop(player, true);
				continue;
			}
			if (!player.getAbilities().flying) {
				// The player toggled vanilla flight off (double-tap jump): keep it a controlled ability flight.
				player.getAbilities().flying = true;
				player.onUpdateAbilities();
			}
			player.fallDistance = 0;
			flight.trail().tick(player, ticks);
		}
	}
}
