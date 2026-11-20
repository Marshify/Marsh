package com.grandline.client.mixin;

import com.grandline.client.render.sword.HeldSwords;
import com.grandline.gear.Gear;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Third person: with a Dual or Triple Katana in the main hand and nothing in the off hand, the off hand holds the second
 * blade; with the Triple Katana the third blade is held crosswise in the mouth.
 */
@Mixin(ItemInHandLayer.class)
public abstract class ItemInHandLayerSwordMixin {
	@ModifyVariable(method = "renderArmWithItem", at = @At("HEAD"), argsOnly = true)
	private ItemStack grandline$secondBlade(ItemStack stack, LivingEntity entity, ItemStack same, ItemDisplayContext context, HumanoidArm arm) {
		return HeldSwords.offHand(entity, stack, arm);
	}

	@Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V",
			at = @At("TAIL"))
	private void grandline$mouthBlade(PoseStack pose, MultiBufferSource buffers, int light, LivingEntity entity, float limbSwing, float limbSwingAmount,
			float partialTick, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
		ItemStack main = entity.getMainHandItem();
		if (!main.is(Gear.TRIPLE_KATANA) || !(((ItemInHandLayer<?, ?>) (Object) this).getParentModel() instanceof HumanoidModel<?> model)) {
			return;
		}
		pose.pushPose();
		model.head.translateAndRotate(pose);
		pose.translate(0.0, -0.14, -0.29);
		pose.mulPose(Axis.ZP.rotationDegrees(90));
		pose.scale(0.85f, 0.85f, 0.85f);
		Minecraft.getInstance().getItemRenderer().renderStatic(entity, main, ItemDisplayContext.HEAD, false, pose, buffers, entity.level(), light,
				OverlayTexture.NO_OVERLAY, entity.getId());
		pose.popPose();
	}
}
