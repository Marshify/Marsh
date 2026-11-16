package com.grandline.combat;

import com.grandline.GrandLineMod;
import com.grandline.data.ModAttachments;
import com.grandline.network.payload.EnergySyncPayload;
import com.grandline.player.PlayerProfileService;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

/**
 * Server-authoritative energy.
 * <ul>
 *   <li>Max energy = {@code baseEnergy} + Melee points x {@code energyPerMeleePoint} (5) + accessory bonus. It is
 *   computed live from the allocated Melee stat (never from level), so allocating or resetting Melee changes it at once;
 *   current energy is kept (and clamped when the max drops), and new capacity fills by regeneration.</li>
 *   <li>Regeneration: {@code energyRegenPerSecond} of max per second (8%), every server tick, never above max.</li>
 *   <li>Costs: a fixed amount per move, its cost fraction x {@code baseEnergy} (see {@link #cost}), times the race
 *   multiplier; checked and spent here, only when a move actually fires; never below zero.</li>
 *   <li>Energy is not saved: players join and respawn full. The client bar is synced every 4 ticks and at once on
 *   spends and stat changes.</li>
 * </ul>
 */
public final class EnergyService {
	/** Mutable per-player state stored in a transient attachment. */
	public static final class Energy {
		double current = -1; // -1 = uninitialised → full
		float lastSentCurrent = Float.NaN;
		float lastSentMax = Float.NaN;
	}

	private EnergyService() {
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			boolean syncTick = server.getTickCount() % 4 == 0;
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				Energy energy = state(player);
				double max = max(player);
				double regen = max * GrandLineMod.config().energyRegenPerSecond / 20.0;
				energy.current = Math.min(max, current(player) + regen);
				if (syncTick) {
					sync(player, false);
				}
			}
		});
	}

	private static Energy state(ServerPlayer player) {
		return player.getAttachedOrCreate(ModAttachments.ENERGY);
	}

	public static double max(ServerPlayer player) {
		return max(PlayerProfileService.get(player).stats().get(com.grandline.player.StatType.MELEE))
				+ com.grandline.gear.GearService.bonusEnergy(player);
	}

	/** Max energy for {@code melee} allocated Melee points, before accessory bonuses: 100 + 5 per point. */
	public static double max(int melee) {
		return GrandLineMod.config().baseEnergy + GrandLineMod.config().energyPerMeleePoint * Math.max(0, melee);
	}

	/** What a move with cost fraction {@code fraction} costs this player, in energy. */
	public static double cost(ServerPlayer player, double fraction) {
		double pool = GrandLineMod.config().energyCostsScaleWithMax ? max(player) : GrandLineMod.config().baseEnergy;
		return pool * Math.max(0, fraction) * com.grandline.race.RacePassives.energyMultiplier(player);
	}

	/** Whether the player has the energy for a move with cost fraction {@code fraction} (spends nothing). */
	public static boolean canAfford(ServerPlayer player, double fraction) {
		return current(player) + 1.0e-9 >= cost(player, fraction);
	}

	/** Lowers stored energy to the current cap (after gear is removed). Never raises it. */
	public static void clampToMax(ServerPlayer player) {
		Energy energy = state(player);
		if (energy.current > max(player)) {
			energy.current = max(player);
		}
	}

	public static double current(ServerPlayer player) {
		Energy energy = state(player);
		if (energy.current < 0) {
			energy.current = max(player);
		}
		return Math.min(energy.current, max(player));
	}

	/** Spends {@code fraction} of max energy if available. Returns false (nothing spent) if there is not enough. */
	public static boolean trySpend(ServerPlayer player, double fraction) {
		double cost = cost(player, fraction);
		double now = current(player);
		if (now + 1.0e-9 < cost) {
			return false;
		}
		state(player).current = Math.max(0, now - cost);
		sync(player, true);
		return true;
	}

	/** Like {@link #trySpend} but without an immediate sync packet (for per-tick drains; the regular sync covers it). */
	public static boolean trySpendQuiet(ServerPlayer player, double fraction) {
		double cost = cost(player, fraction);
		double now = current(player);
		if (now + 1.0e-9 < cost) {
			return false;
		}
		state(player).current = Math.max(0, now - cost);
		return true;
	}

	/** Restores {@code fraction} of max energy (never above max). */
	public static void restore(ServerPlayer player, double fraction) {
		state(player).current = Math.min(max(player), current(player) + max(player) * Math.max(0, fraction));
		sync(player, true);
	}

	public static void refill(ServerPlayer player) {
		state(player).current = max(player);
		sync(player, true);
	}

	public static void sync(ServerPlayer player, boolean force) {
		Energy energy = state(player);
		float current = (float) current(player);
		float max = (float) max(player);
		if (!force && Math.abs(current - energy.lastSentCurrent) < 0.5f && max == energy.lastSentMax) {
			return;
		}
		if (ServerPlayNetworking.canSend(player, EnergySyncPayload.TYPE)) {
			ServerPlayNetworking.send(player, new EnergySyncPayload(current, max));
			energy.lastSentCurrent = current;
			energy.lastSentMax = max;
		}
	}
}
