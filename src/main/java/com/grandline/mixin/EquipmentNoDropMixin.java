package com.grandline.mixin;

import com.grandline.gear.EquipmentOwnership;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Owned equipment cannot be dropped (Q, throwing it out of a screen): it goes straight back into the inventory. */
@Mixin(ServerPlayer.class)
public abstract class EquipmentNoDropMixin {
	@Inject(method = "drop(Lnet/minecraft/world/item/ItemStack;ZZ)Lnet/minecraft/world/entity/item/ItemEntity;", at = @At("HEAD"), cancellable = true)
	private void grandline$keepEquipment(ItemStack stack, boolean dropAround, boolean traceItem, CallbackInfoReturnable<ItemEntity> cir) {
		if (stack.isEmpty() || !EquipmentOwnership.isEquipment(stack.getItem())) {
			return;
		}
		ServerPlayer self = (ServerPlayer) (Object) this;
		if (self.isAlive()) {
			self.getInventory().add(stack.copy()); // if there is no room, the reconciler brings it back later
			self.displayClientMessage(Component.translatable("message.grandline.equipment.no_drop").withStyle(ChatFormatting.GRAY), true);
		}
		stack.setCount(0);
		cir.setReturnValue(null);
	}
}
