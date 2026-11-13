package com.grandline.combat;

import com.grandline.GrandLineMod;
import com.grandline.data.ModAttachments;
import com.grandline.network.payload.EnergySyncPayload;
import com.grandline.player.PlayerProfileService;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

/**
 * Server-authoritative energy. Moves spend a fraction of max energy; energy regenerates every tick. Max energy grows
 * with level. Energy is not saved (players log in full), like most action RPGs.
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
		int level = PlayerProfileService.get(player).level();
		return GrandLineMod.config().baseEnergy + GrandLineMod.config().energyPerLevel * Math.max(0, level - 1)
				+ com.grandline.gear.GearService.bonusEnergy(player);
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
		double cost = max(player) * Math.max(0, fraction);
		double now = current(player);
		if (now + 1.0e-9 < cost) {
			return false;
		}
		state(player).current = now - cost;
		sync(player, true);
		return true;
	}

	/** Like {@link #trySpend} but without an immediate sync packet (for per-tick drains; the regular sync covers it). */
	public static boolean trySpendQuiet(ServerPlayer player, double fraction) {
		double cost = max(player) * Math.max(0, fraction);
		double now = current(player);
		if (now + 1.0e-9 < cost) {
			return false;
		}
		state(player).current = now - cost;
		return true;
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
