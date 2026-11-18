package com.grandline.mixin;

import com.grandline.fruit.FruitRenames;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Stored stacks of a renamed fruit (old saves: inventories, chests, dropped items) load as the renamed fruit. */
@Mixin(ItemStack.class)
public abstract class ItemStackRenameMixin {
	@Inject(method = "of", at = @At("HEAD"))
	private static void grandline$renameFruit(CompoundTag tag, CallbackInfoReturnable<ItemStack> cir) {
		if (tag.contains("id", 8)) {
			String id = tag.getString("id");
			String renamed = FruitRenames.item(id);
			if (!renamed.equals(id)) {
				tag.putString("id", renamed);
			}
		}
	}
}
