package com.grandline.npc;

import com.grandline.GrandLineMod;
import com.grandline.fruit.Fruits;
import com.grandline.player.PlayerProfileService;
import net.fabricmc.fabric.api.loot.v2.LootTableEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Grand Line fishing: vanilla fishing additionally yields original sea creatures (sold to the fisherman for currency)
 * and, very rarely, a fruit.
 */
public final class FishingService {
	public record Catch(Item item, long value, int weight) {
	}

	private static final ResourceLocation FISHING = new ResourceLocation("minecraft", "gameplay/fishing");
	private static final Map<Item, Catch> CATCHES = new LinkedHashMap<>();

	public static final Item SILVERFIN = fish("silverfin", 15, 40, Rarity.COMMON);
	public static final Item REEF_SNAPPER = fish("reef_snapper", 30, 25, Rarity.COMMON);
	public static final Item SUNSCALE_TANG = fish("sunscale_tang", 60, 14, Rarity.UNCOMMON);
	public static final Item STORM_EEL = fish("storm_eel", 120, 9, Rarity.UNCOMMON);
	public static final Item GLASS_SQUID = fish("glass_squid", 250, 6, Rarity.RARE);
	public static final Item EMBER_KOI = fish("ember_koi", 500, 3, Rarity.RARE);
	public static final Item ABYSS_ANGLER = fish("abyss_angler", 1_200, 2, Rarity.EPIC);
	public static final Item CROWN_MARLIN = fish("crown_marlin", 3_000, 1, Rarity.EPIC);

	private FishingService() {
	}

	private static Item fish(String name, long value, int weight, Rarity rarity) {
		Item item = Registry.register(BuiltInRegistries.ITEM, GrandLineMod.id(name), new Item(new Item.Properties().rarity(rarity)));
		CATCHES.put(item, new Catch(item, value, weight));
		return item;
	}

	public static void register() {
		LootTableEvents.MODIFY.register((resourceManager, lootManager, id, tableBuilder, source) -> {
			if (source.isBuiltin() && BuiltInLootTables.BURIED_TREASURE.equals(id)) {
				// Buried treasure always holds exactly one Grand Line fruit, weighted by rarity.
				LootPool.Builder treasure = LootPool.lootPool().setRolls(ConstantValue.exactly(1));
				Fruits.all().forEach(f -> treasure.add(LootItem.lootTableItem(f.item()).setWeight(f.rarity().gachaWeight())));
				tableBuilder.withPool(treasure);
				return;
			}
			if (!source.isBuiltin() || !FISHING.equals(id)) {
				return;
			}
			LootPool.Builder fishPool = LootPool.lootPool().setRolls(ConstantValue.exactly(1))
					.when(LootItemRandomChanceCondition.randomChance(0.6F));
			CATCHES.values().forEach(c -> fishPool.add(LootItem.lootTableItem(c.item()).setWeight(c.weight())));
			tableBuilder.withPool(fishPool);

			LootPool.Builder fruitPool = LootPool.lootPool().setRolls(ConstantValue.exactly(1))
					.when(LootItemRandomChanceCondition.randomChance(0.005F));
			Fruits.all().forEach(f -> fruitPool.add(LootItem.lootTableItem(f.item()).setWeight(6 - f.rarity().ordinal())));
			tableBuilder.withPool(fruitPool);
		});
	}

	public static long value(Item item) {
		Catch c = CATCHES.get(item);
		return c == null ? 0 : c.value();
	}

	/** Total currency the player's carried catch is worth. */
	public static long inventoryValue(ServerPlayer player) {
		long total = 0;
		for (ItemStack stack : player.getInventory().items) {
			total += value(stack.getItem()) * stack.getCount();
		}
		return total;
	}

	/** Sells every Grand Line catch in the inventory. Returns the currency earned. */
	public static long sellAll(ServerPlayer player) {
		long total = 0;
		for (ItemStack stack : player.getInventory().items) {
			long each = value(stack.getItem());
			if (each > 0) {
				total += each * stack.getCount();
				stack.setCount(0);
			}
		}
		if (total > 0) {
			PlayerProfileService.addCurrency(player, total);
		}
		return total;
	}

	public static Iterable<Item> items() {
		return CATCHES.keySet();
	}
}
