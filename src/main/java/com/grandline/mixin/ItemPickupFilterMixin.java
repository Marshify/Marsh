package com.grandline.mixin;

import com.grandline.world.AdventureRules;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adventure mode: players only pick up Grand Line items (and the configured allowlist). */
@Mixin(ItemEntity.class)
public abstract class ItemPickupFilterMixin {
	@Inject(method = "playerTouch", at = @At("HEAD"), cancellable = true)
	private void grandline$onlyApprovedItems(Player player, CallbackInfo ci) {
		ItemEntity self = (ItemEntity) (Object) this;
		if (!self.level().isClientSide && AdventureRules.restricted(player) && !AdventureRules.allowedItem(self.getItem())) {
			ci.cancel();
		}
		// Equipment is never picked up as a loose item: owned equipment is delivered to its owner, never duplicated.
		if (!self.level().isClientSide && com.grandline.gear.EquipmentOwnership.isEquipment(self.getItem().getItem())) {
			ci.cancel();
		}
	}
}
