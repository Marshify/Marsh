package com.grandline.registry;

import com.grandline.GrandLineMod;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
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
			FabricCreativeModeTab.builder()
					.title(Component.translatable("itemGroup.grandline.main"))
					.icon(() -> new ItemStack(ModItems.GRAND_LINE_CRYSTAL))
					.displayItems((parameters, output) -> output.accept(ModItems.GRAND_LINE_CRYSTAL))
					.build());

	private ModCreativeTabs() {
	}

	public static void register() {
		GrandLineMod.LOGGER.debug("Registered creative tab {}", MAIN_KEY.identifier());
	}
}
