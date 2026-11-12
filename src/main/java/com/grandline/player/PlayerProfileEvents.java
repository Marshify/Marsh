package com.grandline.player;

import com.grandline.combat.ability.AbilityCooldownService;
import com.grandline.network.ServerRequestGuard;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

/** Server-side lifecycle hooks that keep profiles initialized and clients in sync. */
public final class PlayerProfileEvents {
	private PlayerProfileEvents() {
	}

	public static void register() {
		// Fires once the client is ready to receive play packets.
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			PlayerProfileService.get(handler.getPlayer()); // creates the default profile for new players
			PlayerProfileService.sync(handler.getPlayer());
			AbilityCooldownService.sync(handler.getPlayer());
		});

		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> ServerRequestGuard.forget(handler.getPlayer().getUUID()));

		// The profile attachment is copyOnDeath, so the new player entity already holds it; just resync.
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
			PlayerProfileService.sync(newPlayer);
			AbilityCooldownService.sync(newPlayer);
		});

		ServerLifecycleEvents.SERVER_STOPPED.register(server -> ServerRequestGuard.clear());
	}
}
