package com.grandline.client.mixin;

import com.grandline.client.render.sword.HeldSwords;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** First person: the empty off hand shows the second blade of a Dual or Triple Katana held in the main hand. */
@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererSwordMixin {
	@ModifyVariable(method = "renderArmWithItem", at = @At("HEAD"), argsOnly = true)
	private ItemStack grandline$secondBlade(ItemStack stack, AbstractClientPlayer player, float partialTicks, float pitch, InteractionHand hand) {
		if (hand != InteractionHand.OFF_HAND || !stack.isEmpty()) {
			return stack;
		}
		ItemStack main = player.getMainHandItem();
		return HeldSwords.multiBlade(main) ? main : stack;
	}
}
