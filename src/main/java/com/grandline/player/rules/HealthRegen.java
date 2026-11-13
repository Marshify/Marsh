package com.grandline.player.rules;

import com.grandline.GrandLineMod;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToDoubleFunction;

/**
 * Passive health regeneration, replacing vanilla's: {@code baseHealthRegenPercentPerSecond} of maximum health per
 * second (1% by default), applied a twentieth every tick so it is smooth and fractional, never above maximum, with no
 * delay after damage. Races, accessories and fruits can add multipliers through {@link #addModifier}.
 */
public final class HealthRegen {
	private static final List<ToDoubleFunction<ServerPlayer>> MODIFIERS = new ArrayList<>();

	private HealthRegen() {
	}

	/** Adds a regeneration multiplier source (1.0 = unchanged). Multipliers stack by multiplying. */
	public static void addModifier(ToDoubleFunction<ServerPlayer> modifier) {
		MODIFIERS.add(modifier);
	}

	static void register() {
		ServerTickEvents.END_SERVER_TICK.register(HealthRegen::tick);
	}

	/** Health regained per tick for this player right now. */
	public static float perTick(ServerPlayer player) {
		double multiplier = 1.0;
		for (ToDoubleFunction<ServerPlayer> m : MODIFIERS) {
			multiplier *= Math.max(0, m.applyAsDouble(player));
		}
		return (float) (player.getMaxHealth() * GrandLineMod.config().baseHealthRegenPercentPerSecond / 100.0 / 20.0 * multiplier);
	}

	private static void tick(MinecraftServer server) {
		if (GrandLineMod.config().baseHealthRegenPercentPerSecond <= 0) {
			return;
		}
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (player.isAlive() && !player.isSpectator() && player.getHealth() < player.getMaxHealth()) {
				player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + perTick(player)));
			}
		}
	}
}
