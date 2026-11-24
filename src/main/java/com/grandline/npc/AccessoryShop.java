package com.grandline.npc;

import com.grandline.GrandLineMod;
import com.grandline.gear.Gear;
import com.grandline.player.PlayerProfileService;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.List;

/** The accessory dealer's stock (Sunscorch Dunes' Guy): accessories bought once and kept, like swords. */
public final class AccessoryShop {
	public static final List<Item> STOCK = List.of(Gear.SWORDSMAN_HAT);

	private AccessoryShop() {
	}

	public static long price(int index) {
		return GrandLineMod.config().accessoryPrices[index];
	}

	public static List<String> ids() {
		List<String> ids = new ArrayList<>();
		STOCK.forEach(item -> ids.add(BuiltInRegistries.ITEM.getKey(item).toString()));
		return ids;
	}

	/** Returns a result key: accessory_bought, already_owned or accessory_money. */
	public static String buy(ServerPlayer player, int index) {
		if (index < 0 || index >= STOCK.size()) {
			return "too_far";
		}
		if (com.grandline.gear.EquipmentOwnership.owns(player, STOCK.get(index))) {
			return "already_owned";
		}
		long price = price(index);
		if (PlayerProfileService.get(player).currency() < price) {
			return "accessory_money";
		}
		PlayerProfileService.update(player, p -> p.withCurrency(p.currency() - price));
		com.grandline.gear.EquipmentOwnership.grant(player, STOCK.get(index));
		return "accessory_bought";
	}
}
