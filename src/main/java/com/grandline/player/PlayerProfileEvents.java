package com.grandline.player;

import com.grandline.combat.EnergyService;
import com.grandline.combat.StatEffects;
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
			// Creates the default profile for new players; stat modifiers are transient, so re-apply them on every join.
			StatEffects.apply(handler.getPlayer(), PlayerProfileService.get(handler.getPlayer()));
			PlayerProfileService.sync(handler.getPlayer());
			AbilityCooldownService.sync(handler.getPlayer());
			EnergyService.sync(handler.getPlayer(), true);
		});

		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> ServerRequestGuard.forget(handler.getPlayer().getUUID()));

		// The profile attachment is copyOnDeath, so the new player entity already holds it; re-apply effects and resync.
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
			StatEffects.apply(newPlayer, PlayerProfileService.get(newPlayer));
			if (!alive) {
				newPlayer.setHealth(newPlayer.getMaxHealth()); // respawn after death at full (stat-boosted) health
				EnergyService.refill(newPlayer);
			}
			PlayerProfileService.sync(newPlayer);
			AbilityCooldownService.sync(newPlayer);
		});

		ServerLifecycleEvents.SERVER_STOPPED.register(server -> ServerRequestGuard.clear());
	}
}
