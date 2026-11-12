package com.grandline.registry;

import com.grandline.GrandLineMod;
import com.grandline.entity.GrandLineEntities;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;

import java.util.ArrayList;
import java.util.List;

/** Spawn eggs for every Grand Line mob (creative mode and JEI). */
public final class ModSpawnEggs {
	public static final List<Item> ALL = new ArrayList<>();

	public static final Item ISLANDER = egg("islander", GrandLineEntities.ISLANDER, 0x284698, 0xE6BE50);
	public static final Item BANDIT = egg("bandit", GrandLineEntities.BANDIT, 0x785032, 0xB42828);
	public static final Item RAIDER = egg("raider", GrandLineEntities.RAIDER, 0x3C3C46, 0x961E1E);
	public static final Item CORSAIR = egg("corsair", GrandLineEntities.CORSAIR, 0x192346, 0xC82828);
	public static final Item CAPTAIN = egg("captain", GrandLineEntities.CAPTAIN, 0x50141E, 0xF0C83C);

	private ModSpawnEggs() {
	}

	private static Item egg(String name, EntityType<? extends Mob> type, int base, int spots) {
		Item item = Registry.register(BuiltInRegistries.ITEM, GrandLineMod.id(name + "_spawn_egg"),
				new SpawnEggItem(type, base, spots, new Item.Properties()));
		ALL.add(item);
		return item;
	}

	public static void register() {
		GrandLineMod.LOGGER.debug("Registered {} spawn eggs", ALL.size());
	}
}
