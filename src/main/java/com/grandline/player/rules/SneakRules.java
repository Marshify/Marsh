package com.grandline.player.rules;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

/**
 * No crouching in Grand Line play: a survival or adventure player on foot never stays sneaking (the client never asks;
 * this catches anything that does). Riding keeps sneak so players can get off ships; creative and spectator keep it.
 */
public final class SneakRules {
	private SneakRules() {
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerPlayer p : server.getPlayerList().getPlayers()) {
				GameType mode = p.gameMode.getGameModeForPlayer();
				if (p.isShiftKeyDown() && !p.isPassenger() && (mode == GameType.SURVIVAL || mode == GameType.ADVENTURE)) {
					p.setShiftKeyDown(false);
				}
			}
		});
	}
}
