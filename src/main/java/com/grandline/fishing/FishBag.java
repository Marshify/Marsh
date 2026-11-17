package com.grandline.fishing;

import com.grandline.data.ModAttachments;
import com.grandline.network.payload.FishBagPayload;
import com.grandline.npc.FishingService;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * The fish bag: every catch goes here (shown in the Items screen's Backpack tab and sold to the fisherman from here),
 * never into the inventory or hotbar. Saved with the player, kept through death. A fish item that reaches the inventory
 * any other way (old saves, a dropped fish picked up) is moved into the bag.
 */
public final class FishBag {
	private FishBag() {
	}

	public static void register() {
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			absorb(handler.getPlayer());
			sync(handler.getPlayer());
		});
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> sync(newPlayer));
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % 5 == 0) {
				server.getPlayerList().getPlayers().forEach(FishBag::absorb);
			}
		});
	}

	public static Map<String, Integer> contents(ServerPlayer player) {
		return player.getAttachedOrElse(ModAttachments.FISH_BAG, Map.of());
	}

	public static void add(ServerPlayer player, Item fish, int count) {
		Map<String, Integer> bag = new TreeMap<>(contents(player));
		bag.merge(BuiltInRegistries.ITEM.getKey(fish).toString(), count, Integer::sum);
		player.setAttached(ModAttachments.FISH_BAG, Map.copyOf(bag));
		sync(player);
	}

	/** Moves any catch in the inventory into the bag. */
	public static void absorb(ServerPlayer player) {
		var items = player.getInventory().items;
		Map<String, Integer> moved = new HashMap<>();
		for (ItemStack stack : items) {
			if (!stack.isEmpty() && FishingService.value(stack.getItem()) > 0) {
				moved.merge(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(), stack.getCount(), Integer::sum);
				stack.setCount(0);
			}
		}
		if (!moved.isEmpty()) {
			Map<String, Integer> bag = new TreeMap<>(contents(player));
			moved.forEach((id, n) -> bag.merge(id, n, Integer::sum));
			player.setAttached(ModAttachments.FISH_BAG, Map.copyOf(bag));
			player.inventoryMenu.broadcastChanges();
			sync(player);
		}
	}

	/** Total value of the bag. */
	public static long value(ServerPlayer player) {
		long total = 0;
		for (var e : contents(player).entrySet()) {
			Item item = BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(e.getKey()));
			total += FishingService.value(item) * e.getValue();
		}
		return total;
	}

	/** Empties the bag, returning what it was worth. */
	public static long sellAll(ServerPlayer player) {
		long total = value(player);
		player.setAttached(ModAttachments.FISH_BAG, Map.of());
		sync(player);
		return total;
	}

	public static void sync(ServerPlayer player) {
		if (ServerPlayNetworking.canSend(player, FishBagPayload.TYPE)) {
			ServerPlayNetworking.send(player, new FishBagPayload(new TreeMap<>(contents(player))));
		}
	}
}
