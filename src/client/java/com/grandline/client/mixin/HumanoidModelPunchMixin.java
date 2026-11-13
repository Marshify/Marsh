package com.grandline.client.mixin;

import com.grandline.client.fx.ArmAnimations;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Overlays the move punch on the player model after vanilla posing: shoulder windup (arm drawn back, torso turned),
 * a sharp snap straight along the look direction on the launch tick, then a short follow-through back to the normal
 * pose. Runs before PlayerModel copies arms to sleeves, so sleeves follow.
 */
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
		ArmAnimations.Phase phase = ArmAnimations.phase(entity.getId(), age);
		if (phase == null) {
			return;
		}
		float forward = -Mth.HALF_PI + head.xRot; // arm pointing where the player looks
		float armX;
		float armY;
		float armZ;
		float twist;
		switch (phase.kind()) {
			case WINDUP -> {
				float t = ease(phase.t());
				armX = Mth.lerp(t, rightArm.xRot, 0.55f);
				armY = Mth.lerp(t, rightArm.yRot, 0.45f);
				armZ = Mth.lerp(t, rightArm.zRot, 0.35f);
				twist = 0.38f * t; // right shoulder pulled back
			}
			case SNAP -> {
				float t = 1 - (1 - phase.t()) * (1 - phase.t()); // fast out
				armX = Mth.lerp(t, 0.55f, forward - 0.05f);
				armY = Mth.lerp(t, 0.45f, -0.12f);
				armZ = Mth.lerp(t, 0.35f, 0f);
				twist = Mth.lerp(t, 0.38f, -0.32f);
			}
			default -> {
				float t = ease(phase.t());
				armX = Mth.lerp(t, forward - 0.05f, rightArm.xRot);
				armY = Mth.lerp(t, -0.12f, rightArm.yRot);
				armZ = Mth.lerp(t, 0f, rightArm.zRot);
				twist = Mth.lerp(t, -0.32f, 0f);
			}
		}
		// Turn the torso and move both shoulders with it (like vanilla's attack swing does).
		body.yRot = twist;
		rightArm.z = Mth.sin(twist) * 5.0F;
		rightArm.x = -Mth.cos(twist) * 5.0F;
		leftArm.z = -Mth.sin(twist) * 5.0F;
		leftArm.x = Mth.cos(twist) * 5.0F;
		leftArm.yRot += twist;
		rightArm.xRot = armX;
		rightArm.yRot = armY + twist;
		rightArm.zRot = armZ;
	}

	private static float ease(float t) {
		return t * t * (3 - 2 * t);
	}
}
