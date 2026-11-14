package com.grandline.registry;

import com.grandline.GrandLineMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

import java.util.function.Function;

/** Item registrations. */
public final class ModItems {
	/** Original test item: a sea-blue crystal said to resonate with the currents of the Grand Line. */
	public static final Item GRAND_LINE_CRYSTAL = register("grand_line_crystal", Item::new,
			new Item.Properties().rarity(Rarity.RARE).stacksTo(16).fireResistant());

	private ModItems() {
	}

	/** The Starter Fishing Rod (sold by Fishermen). */
	public static final Item STARTER_FISHING_ROD = register("starter_fishing_rod", com.grandline.fishing.StarterRodItem::new,
			new Item.Properties().stacksTo(1));

	/** Race unlock parts and the awakening key (see race/RaceProgress). */
	public static final Item MACHINE_CORE = register("machine_core", Item::new, new Item.Properties().rarity(Rarity.UNCOMMON).stacksTo(16));
	public static final Item BLOOD_VIAL = register("blood_vial", Item::new, new Item.Properties().rarity(Rarity.UNCOMMON).stacksTo(16));
	public static final Item AWAKENING_KEY = register("awakening_key", Item::new, new Item.Properties().rarity(Rarity.EPIC).stacksTo(1).fireResistant());

	private static <T extends Item> T register(String name, Function<Item.Properties, T> factory, Item.Properties properties) {
		return Registry.register(BuiltInRegistries.ITEM, GrandLineMod.id(name), factory.apply(properties));
	}

	/** Forces class loading so items are registered during mod initialization. */
	public static void register() {
		GrandLineMod.LOGGER.debug("Registered item {}", BuiltInRegistries.ITEM.getKey(GRAND_LINE_CRYSTAL));
	}
}
