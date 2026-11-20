package com.grandline.client.mixin;

import com.grandline.client.render.sword.HeldSwords;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Multi-blade swords use their single-blade model in the hands and the mouth blade on the head (see HeldSwords). */
@Mixin(ItemRenderer.class)
public abstract class ItemRendererHeldSwordMixin {
	@ModifyVariable(method = "render", at = @At("HEAD"), argsOnly = true)
	private BakedModel grandline$heldModel(BakedModel model, ItemStack stack, ItemDisplayContext context, boolean leftHand, PoseStack pose,
			MultiBufferSource buffers, int light, int overlay) {
		return HeldSwords.model(stack, context, model);
	}
}
