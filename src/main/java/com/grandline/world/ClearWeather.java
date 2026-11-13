package com.grandline.world;

import com.grandline.GrandLineMod;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;

/**
 * Grand Line worlds keep permanently clear weather (no rain, thunder or snowfall); the weather cycle is switched off
 * and any weather that starts anyway (commands, other mods) is cleared within a second. Only the Grand Line overworld
 * is touched; snow blocks and terrain are unaffected.
 */
public final class ClearWeather {
	private ClearWeather() {
	}

	public static void register() {
		ServerLifecycleEvents.SERVER_STARTED.register(ClearWeather::clear);
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % 20 == 0) {
				ServerLevel overworld = server.overworld();
				if (overworld.isRaining() || overworld.isThundering()) {
					clear(server);
				}
			}
		});
	}

	private static void clear(MinecraftServer server) {
		ServerLevel overworld = server.overworld();
		if (!GrandLineMod.config().disableWeather || !ModWorld.isGrandLineWorld(overworld)) {
			return;
		}
		server.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false, server);
		overworld.setWeatherParameters(1_000_000, 0, false, false);
	}
}
