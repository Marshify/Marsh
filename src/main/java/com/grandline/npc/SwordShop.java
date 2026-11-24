package com.grandline.npc;

import com.grandline.GrandLineMod;
import com.grandline.gear.Gear;
import com.grandline.player.PlayerProfileService;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.List;

/** The Swordsmith's shop (Millbrook Village): a fixed stock of swords bought with Grand Line money. Prices are checked and
 * taken on the server; the sword is only handed over after the money is gone. */
public final class SwordShop {
	public static final List<Item> STOCK = List.of(Gear.KATANA, Gear.CUTLASS, Gear.DUAL_KATANA, Gear.IRON_MACE, Gear.REEFSAW,
			Gear.TRIPLE_KATANA, Gear.BOILER_PIPE, Gear.STONEWALL_BLADE, Gear.TWINFANG_GLAIVE, Gear.WRAITH_CANE, Gear.TRIDENT,
			Gear.BISENTO, Gear.STORMSTAFF, Gear.ECLIPSE_BLADE);

	private SwordShop() {
	}

	/**
	 * The STOCK indices this island's sword dealer sells: Emberfall's Living Skeleton only the Soul Cane (the Wraith
	 * Cane's island name), every other sword dealer everything else.
	 */
	public static List<Integer> stockFor(com.grandline.world.Island island) {
		int cane = STOCK.indexOf(Gear.WRAITH_CANE);
		List<Integer> out = new ArrayList<>();
		boolean emberfall = island != null && "Emberfall Isle".equals(island.name());
		for (int i = 0; i < STOCK.size(); i++) {
			if ((i == cane) == emberfall) {
				out.add(i);
			}
		}
		return out;
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
		// Equipment is owned once, for good: an owned item cannot be bought again (checked before any money is taken).
		if (com.grandline.gear.EquipmentOwnership.owns(player, STOCK.get(index))) {
			return "already_owned";
		}
		long price = price(index);
		if (PlayerProfileService.get(player).currency() < price) {
			return "sword_money";
		}
		PlayerProfileService.update(player, p -> p.withCurrency(p.currency() - price));
		com.grandline.gear.EquipmentOwnership.grant(player, STOCK.get(index));
		return "sword_bought";
	}
}
