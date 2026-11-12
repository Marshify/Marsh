package com.grandline.registry;

import com.grandline.GrandLineMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
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

	private static <T extends Item> T register(String name, Function<Item.Properties, T> factory, Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, GrandLineMod.id(name));
		T item = factory.apply(properties.setId(key));
		return Registry.register(BuiltInRegistries.ITEM, key, item);
	}

	/** Forces class loading so items are registered during mod initialization. */
	public static void register() {
		GrandLineMod.LOGGER.debug("Registered item {}", BuiltInRegistries.ITEM.getKey(GRAND_LINE_CRYSTAL));
	}
}
