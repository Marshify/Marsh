package com.grandline.player.loadout;

import com.grandline.gear.GearSwordItem;
import com.grandline.gear.GunItem;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * Keeps the hotbar laid out for the Grand Line bar: slots 1-4 (indices 0-3) hold only the sword and gun, whose tiles
 * are the loadout's Sword and Gun keys; anything else there moves to slots 5-9 (or the main inventory if those are
 * full), where it gets its own tile. A sword or gun sitting in slots 5-9 moves into a free slot among 1-4. Runs every
 * half second; never drops or deletes anything.
 */
public final class HotbarLayout {
	private HotbarLayout() {
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % 10 != 0) {
				return;
			}
			for (ServerPlayer p : server.getPlayerList().getPlayers()) {
				if (!p.isSpectator() && !p.isCreative()) {
					tidy(p);
				}
			}
		});
	}

	static boolean weapon(ItemStack s) {
		return s.getItem() instanceof GearSwordItem || s.getItem() instanceof GunItem;
	}

	/** Returns true if anything moved. */
	public static boolean tidy(ServerPlayer p) {
		Inventory inv = p.getInventory();
		boolean moved = false;
		for (int i = 0; i < LoadoutOrder.SIZE; i++) {
			ItemStack s = inv.getItem(i);
			if (!s.isEmpty() && !weapon(s)) {
				int to = free(inv, LoadoutOrder.SIZE, 9);
				if (to < 0) {
					to = free(inv, 9, 36);
				}
				if (to >= 0) {
					inv.setItem(to, s);
					inv.setItem(i, ItemStack.EMPTY);
					if (inv.selected == i && to < 9) {
						inv.selected = to;
					}
					moved = true;
				}
			}
		}
		for (int i = LoadoutOrder.SIZE; i < 9; i++) {
			ItemStack s = inv.getItem(i);
			if (weapon(s)) {
				int to = free(inv, 0, LoadoutOrder.SIZE);
				if (to >= 0) {
					inv.setItem(to, s);
					inv.setItem(i, ItemStack.EMPTY);
					if (inv.selected == i) {
						inv.selected = to;
					}
					moved = true;
				}
			}
		}
		if (moved) {
			p.inventoryMenu.broadcastChanges();
			p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket(inv.selected));
		}
		return moved;
	}

	private static int free(Inventory inv, int from, int to) {
		for (int i = from; i < to; i++) {
			if (inv.getItem(i).isEmpty()) {
				return i;
			}
		}
		return -1;
	}
}
