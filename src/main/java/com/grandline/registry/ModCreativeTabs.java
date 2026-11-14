package com.grandline.registry;

import com.grandline.GrandLineMod;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

/** The Grand Line creative inventory tab. */
public final class ModCreativeTabs {
	public static final ResourceKey<CreativeModeTab> MAIN_KEY = ResourceKey.create(Registries.CREATIVE_MODE_TAB, GrandLineMod.id("main"));

	public static final CreativeModeTab MAIN = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, MAIN_KEY,
			FabricItemGroup.builder()
					.title(Component.translatable("itemGroup.grandline.main"))
					.icon(() -> new ItemStack(ModItems.GRAND_LINE_CRYSTAL))
					.displayItems((parameters, output) -> {
						// Everything the mod adds: crystal, fruits by rarity, sea creatures, spawn eggs.
						output.accept(ModItems.GRAND_LINE_CRYSTAL);
						output.accept(ModItems.STARTER_FISHING_ROD);
						com.grandline.fruit.Fruits.all().stream()
								.sorted(java.util.Comparator.comparingInt(f -> f.rarity().ordinal()))
								.forEach(f -> output.accept(f.item()));
						com.grandline.gear.Gear.all().forEach(output::accept);
						com.grandline.npc.FishingService.items().forEach(output::accept);
						ModSpawnEggs.ALL.forEach(output::accept);
					})
					.build());

	private ModCreativeTabs() {
	}

	public static void register() {
		GrandLineMod.LOGGER.debug("Registered creative tab {}", MAIN_KEY.location());
	}
}
