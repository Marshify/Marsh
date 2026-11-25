package com.grandline.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Self Destruct: the helmet is not drawn while the bomb head replaces the head (it reappears with the head). */
@Mixin(HumanoidArmorLayer.class)
public abstract class HumanoidArmorLayerBombHeadMixin {
	@Inject(method = "renderArmorPiece", at = @At("HEAD"), cancellable = true)
	private void grandline$bombHead(PoseStack pose, MultiBufferSource buffers, LivingEntity entity, EquipmentSlot slot, int light, HumanoidModel<?> model,
			CallbackInfo ci) {
		if (slot == EquipmentSlot.HEAD && com.grandline.client.fx.BombFx.headActive(entity.getId())) {
			ci.cancel();
		}
	}
}
