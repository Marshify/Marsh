package com.grandline.mixin;

import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Grand Line has no durability: no item can be damaged, so nothing wears out or breaks (attacks, abilities, fishing,
 * using items on blocks or entities), no durability bar or tooltip line shows, and repair, Unbreaking and Mending have
 * nothing to do. Runs on both sides, so the client never predicts damage either. Old damage is mended by
 * EquipmentOwnership's reconcile.
 */
@Mixin(ItemStack.class)
public abstract class ItemStackNoDurabilityMixin {
	@Inject(method = "isDamageableItem", at = @At("HEAD"), cancellable = true)
	private void grandline$noDurability(CallbackInfoReturnable<Boolean> cir) {
		cir.setReturnValue(false);
	}
}
