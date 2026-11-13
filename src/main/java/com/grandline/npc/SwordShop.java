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

/** The Swordsmith's shop (Millbrook Village): a fixed stock of swords bought with Grand Line money. Prices are checked and
 * taken on the server; the sword is only handed over after the money is gone. */
public final class SwordShop {
	public static final List<Item> STOCK = List.of(Gear.KATANA, Gear.CUTLASS);

	private SwordShop() {
	}

	public static long price(int index) {
		return GrandLineMod.config().swordPrices[index];
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

	/** Returns a result key: sword_bought or sword_money. */
	public static String buy(ServerPlayer player, int index) {
		if (index < 0 || index >= STOCK.size()) {
			return "too_far";
		}
		long price = price(index);
		if (PlayerProfileService.get(player).currency() < price) {
			return "sword_money";
		}
		PlayerProfileService.update(player, p -> p.withCurrency(p.currency() - price));
		ItemStack stack = new ItemStack(STOCK.get(index));
		if (!player.getInventory().add(stack)) {
			player.drop(stack, false);
		}
		return "sword_bought";
	}
}
