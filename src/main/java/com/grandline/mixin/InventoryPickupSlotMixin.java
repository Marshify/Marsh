package com.grandline.mixin;

import com.grandline.gear.GearSwordItem;
import com.grandline.gear.GunItem;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Where a newly picked-up or given item lands: the Grand Line hotbar keeps slots 1-4 for the sword and gun, so any other
 * item goes straight to slots 5-9 (then the main inventory), where it gets its tile at once; a sword or gun goes to a
 * free slot among 1-4 first. Only the choice of empty slot changes; stacking onto existing stacks is vanilla's.
 */
@Mixin(Inventory.class)
public abstract class InventoryPickupSlotMixin {
	@Redirect(method = "addResource(Lnet/minecraft/world/item/ItemStack;)I",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Inventory;getFreeSlot()I"))
	private int grandline$freeSlotFor(Inventory inv, ItemStack stack) {
		boolean weapon = stack.getItem() instanceof GearSwordItem || stack.getItem() instanceof GunItem;
		int[][] ranges = weapon ? new int[][]{{0, 4}, {4, 9}, {9, 36}} : new int[][]{{4, 9}, {9, 36}, {0, 4}};
		for (int[] r : ranges) {
			for (int i = r[0]; i < r[1]; i++) {
				if (inv.items.get(i).isEmpty()) {
					return i;
				}
			}
		}
		return -1;
	}
}
