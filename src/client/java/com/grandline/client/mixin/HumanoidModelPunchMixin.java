package com.grandline.client.mixin;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Overlays move cast poses ({@link com.grandline.client.fx.MovePoses}) after vanilla posing; sleeves follow the arms. */
@Mixin(HumanoidModel.class)
public abstract class HumanoidModelPunchMixin {
	@Shadow @Final public ModelPart rightArm;
	@Shadow @Final public ModelPart leftArm;
	@Shadow @Final public ModelPart body;
	@Shadow @Final public ModelPart head;

	@Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
	private void grandline$punch(LivingEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
			float headPitch, CallbackInfo ci) {
		if (!(entity instanceof Player)) {
			return;
		}
		float age = entity.tickCount + (ageInTicks - (int) ageInTicks);
		com.grandline.client.fx.MovePoses.apply(entity.getId(), age, head, body, rightArm, leftArm);
	}
}
