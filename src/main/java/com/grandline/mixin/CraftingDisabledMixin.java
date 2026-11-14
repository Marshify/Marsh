package com.grandline.mixin;

import com.grandline.world.AdventureRules;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adventure mode: vanilla crafting (the inventory 2x2 grid included) produces nothing for regular players. */
@Mixin(CraftingMenu.class)
public abstract class CraftingDisabledMixin {
	@Inject(method = "slotChangedCraftingGrid", at = @At("HEAD"), cancellable = true)
	private static void grandline$noCrafting(AbstractContainerMenu menu, Level level, Player player, CraftingContainer grid,
			ResultContainer result, CallbackInfo ci) {
		if (!level.isClientSide && AdventureRules.restricted(player)) {
			result.setItem(0, ItemStack.EMPTY);
			ci.cancel();
		}
	}
}
