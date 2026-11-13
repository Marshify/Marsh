package com.grandline.world;

import com.grandline.GrandLineMod;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;

/**
 * Worlds made with the Grand Line world type have no vanilla mobs: natural spawning (every dimension, including mobs
 * placed while chunks generate) is cancelled by {@code NaturalSpawnerMixin}, the Grand Line sea biome lists no spawns,
 * and the special spawners (pillager patrols, wandering traders, phantoms) are switched off with game rules. Grand
 * Line's own enemies and NPCs use their own spawning and are unaffected. Other world types are untouched.
 */
public final class VanillaSpawns {
	/** True while a Grand Line world is loaded. Read from spawning code on the server thread. */
	private static volatile boolean grandLineWorld;

	private VanillaSpawns() {
	}

	public static boolean blocked() {
		return grandLineWorld;
	}

	public static void register() {
		ServerWorldEvents.LOAD.register((server, level) -> {
			if (level.dimension() == Level.OVERWORLD) {
				grandLineWorld = ModWorld.isGrandLineWorld(level);
				if (grandLineWorld) {
					disableSpecialSpawners(server);
				}
			}
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> grandLineWorld = false);
	}

	private static void disableSpecialSpawners(MinecraftServer server) {
		GameRules rules = server.getGameRules();
		rules.getRule(GameRules.RULE_DO_PATROL_SPAWNING).set(false, server);
		rules.getRule(GameRules.RULE_DO_TRADER_SPAWNING).set(false, server);
		rules.getRule(GameRules.RULE_DOINSOMNIA).set(false, server);
		GrandLineMod.LOGGER.info("Grand Line world: vanilla mob spawning is off (natural spawns, patrols, traders, phantoms)");
	}
}
