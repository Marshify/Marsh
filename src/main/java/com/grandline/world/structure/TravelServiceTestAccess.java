package com.grandline.world.structure;

import net.minecraft.server.level.ServerPlayer;

/** Lets game tests run the plate fallback check for one player (mock players are not in the server's player list). */
public final class TravelServiceTestAccess {
	private TravelServiceTestAccess() {
	}

	public static void fallback(ServerPlayer player) {
		TravelService.fallback(player);
	}

	/** Simulates enable-command-block (null = use the server's real setting). */
	public static void commandBlocks(Boolean enabled) {
		TravelService.commandBlocksOverride = enabled;
	}
}
