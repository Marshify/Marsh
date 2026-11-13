package com.grandline.npc;

import com.grandline.GrandLineMod;
import com.grandline.gear.Gear;
import com.grandline.player.PlayerProfileService;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** The Gunsmith's shop: a fixed stock of guns bought with Grand Line money. */
public final class GunShop {
	public static final List<Item> STOCK = List.of(Gear.SLINGSHOT, Gear.MUSKET);

	private GunShop() {
	}

	public static long price(int index) {
		return GrandLineMod.config().gunPrices[index];
	}

	public static List<String> ids() {
		List<String> ids = new ArrayList<>();
		STOCK.forEach(item -> ids.add(BuiltInRegistries.ITEM.getKey(item).toString()));
		return ids;
	}

	public static List<Long> prices() {
		List<Long> prices = new ArrayList<>();
		for (int i = 0; i < STOCK.size(); i++) {
			prices.add(price(i));
		}
		return prices;
	}

	/** Returns a result key: gun_bought or gun_money. */
	public static String buy(ServerPlayer player, int index) {
		if (index < 0 || index >= STOCK.size()) {
			return "too_far";
		}
		long price = price(index);
		if (PlayerProfileService.get(player).currency() < price) {
			return "gun_money";
		}
		PlayerProfileService.update(player, p -> p.withCurrency(p.currency() - price));
		ItemStack stack = new ItemStack(STOCK.get(index));
		if (!player.getInventory().add(stack)) {
			player.drop(stack, false);
		}
		return "gun_bought";
	}
}
