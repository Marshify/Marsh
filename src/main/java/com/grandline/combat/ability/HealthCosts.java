package com.grandline.combat.ability;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Health as an ability resource (reusable). {@link #spend} takes health directly (no damage event), so armor, Defense,
 * Aura, absorption, Instinct and damage immunities cannot reduce it, it never kills (a floor is kept), never heals, and
 * grants no invulnerability frames. {@link Penalty} is a repeated-use escalation: each use inside the recovery window
 * costs the next fraction of max health; waiting out the window resets it.
 */
public final class HealthCosts {
	private HealthCosts() {
	}

	/** Takes up to {@code amount} health, never below {@code floor}. Returns what was actually taken. */
	public static float spend(ServerPlayer player, float amount, float floor) {
		if (amount <= 0 || !player.isAlive()) {
			return 0;
		}
		float health = player.getHealth();
		float take = Math.min(amount, Math.max(0, health - floor));
		if (take > 0) {
			player.setHealth(health - take);
		}
		return take;
	}

	/**
	 * Repeated-use escalation. {@code fractions[n]} of max health is the cost of the n-th use in a row (index 0 = the
	 * first use after full recovery; usually 0); uses past the end repeat the last value.
	 */
	public static final class Penalty {
		private record Use(int streak, long tick) {
		}

		public final int recoveryTicks;
		private final float[] fractions;
		private final Map<UUID, Use> uses = new HashMap<>();

		public Penalty(int recoveryTicks, float... fractions) {
			this.recoveryTicks = recoveryTicks;
			this.fractions = fractions;
			ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> uses.remove(handler.getPlayer().getUUID()));
			ServerLifecycleEvents.SERVER_STOPPED.register(server -> uses.clear());
		}

		/** The streak index the next use would have (0 = recovered). */
		public int stage(ServerPlayer player) {
			Use u = uses.get(player.getUUID());
			return u == null || player.serverLevel().getGameTime() - u.tick() > recoveryTicks ? 0 : u.streak();
		}

		/** The max-health fraction the next use would cost. */
		public float nextCost(ServerPlayer player) {
			return fraction(stage(player));
		}

		public float fraction(int stage) {
			return fractions.length == 0 ? 0 : fractions[Math.min(stage, fractions.length - 1)];
		}

		/** Records a use and returns the max-health fraction it costs. */
		public float use(ServerPlayer player) {
			int s = stage(player);
			uses.put(player.getUUID(), new Use(s + 1, player.serverLevel().getGameTime()));
			return fraction(s);
		}

		/** Ticks until fully recovered (0 when recovered). */
		public int remaining(ServerPlayer player) {
			Use u = uses.get(player.getUUID());
			return u == null ? 0 : (int) Math.max(0, recoveryTicks - (player.serverLevel().getGameTime() - u.tick()));
		}

		public void reset(ServerPlayer player) {
			uses.remove(player.getUUID());
		}
	}
}
