package com.grandline.npc;

import com.grandline.GrandLineMod;
import com.grandline.gear.Gear;
import com.grandline.player.PlayerProfileService;
import com.grandline.world.Island;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.List;

/**
 * Accessory dealers: Sunscorch Dunes' Guy (Swordsman Hat), Redbrick Port's Parlus (Black Cape), Cloudtop Isles' Yoshi
 * (Tomoe Ring). Accessories are bought once and kept, like swords. Prices: config {@code accessoryPrices}, in
 * {@link #CATALOG} order.
 */
public final class AccessoryShop {
	public static final List<Item> CATALOG = List.of(Gear.SWORDSMAN_HAT, Gear.BLACK_CAPE, Gear.TOMOE_RING);

	private AccessoryShop() {
	}

	/** What the dealer on this island sells (catalog items). */
	public static List<Item> stock(Island island) {
		String name = island == null ? "" : island.name();
		return switch (name) {
			case "Redbrick Port" -> List.of(Gear.BLACK_CAPE);
			case "Cloudtop Isles" -> List.of(Gear.TOMOE_RING);
			default -> List.of(Gear.SWORDSMAN_HAT);
		};
	}

	public static long price(Item item) {
		return GrandLineMod.config().accessoryPrices[CATALOG.indexOf(item)];
	}

	public static List<String> ids(Island island) {
		List<String> ids = new ArrayList<>();
		stock(island).forEach(item -> ids.add(BuiltInRegistries.ITEM.getKey(item).toString()));
		return ids;
	}

	/** Buys item number {@code index} of this island's stock. Returns a result key: accessory_bought, already_owned or accessory_money. */
	public static String buy(ServerPlayer player, Island island, int index) {
		List<Item> stock = stock(island);
		if (index < 0 || index >= stock.size()) {
			return "too_far";
		}
		Item item = stock.get(index);
		if (com.grandline.gear.EquipmentOwnership.owns(player, item)) {
			return "already_owned";
		}
		long price = price(item);
		if (PlayerProfileService.get(player).currency() < price) {
			return "accessory_money";
		}
		PlayerProfileService.update(player, p -> p.withCurrency(p.currency() - price));
		com.grandline.gear.EquipmentOwnership.grant(player, item);
		return "accessory_bought";
	}
}
