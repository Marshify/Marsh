package com.grandline.player.rules;

import com.grandline.GrandLineMod;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;

/**
 * Death never costs items: the dead player's inventory (main slots, armour, off hand, with every stack's NBT, durability
 * and attributes) moves to the respawned player. The drop itself is cancelled in PlayerKeepInventoryMixin. Levels,
 * currency, mastery, equipped gear and the rest of the profile are player attachments copied on death already.
 */
public final class KeepInventory {
	private KeepInventory() {
	}

	public static void register() {
		ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
			if (!alive && GrandLineMod.config().keepInventoryOnDeath) {
				newPlayer.getInventory().replaceWith(oldPlayer.getInventory());
				oldPlayer.getInventory().clearContent(); // the old body is gone; nothing of it can be picked up twice
			}
		});
	}
}
