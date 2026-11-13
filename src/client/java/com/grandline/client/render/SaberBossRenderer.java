package com.grandline.client.render;

import com.grandline.GrandLineMod;
import com.grandline.entity.SaberBoss;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * The Saber Warden: the full player model (both skin layers, standard 64x64 UV layout) with its own skin, holding the
 * Saber, posed through each attack: blade raised for the Saber Slash, drawn back for the Flying Slash, a crouch and
 * three cuts for the Heavy Combo, then a winded stance.
 */
public class SaberBossRenderer extends HumanoidMobRenderer<SaberBoss, SaberBossRenderer.Model> {
	static final ResourceLocation SKIN = GrandLineMod.id("textures/entity/saber_boss.png");
	static final float SCALE = 1.08F;

	public SaberBossRenderer(EntityRendererProvider.Context context) {
		super(context, new Model(context.bakeLayer(ModelLayers.PLAYER)), 0.5F * SCALE);
	}

	@Override
	public ResourceLocation getTextureLocation(SaberBoss entity) {
		return SKIN;
	}

	@Override
	protected void scale(SaberBoss entity, PoseStack poseStack, float partialTick) {
		poseStack.scale(SCALE, SCALE, SCALE);
	}

	public static final class Model extends PlayerModel<SaberBoss> {
		Model(ModelPart root) {
			super(root, false);
		}

		@Override
		public void setupAnim(SaberBoss boss, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
			super.setupAnim(boss, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
			float t = ageInTicks - boss.clientAttackStart;
			switch (boss.clientAttack) {
				case SaberBoss.SLASH -> slash(t);
				case SaberBoss.FLYING -> flying(t);
				case SaberBoss.COMBO -> combo(t);
				default -> {
					// Guard stance between attacks: blade held forward and low.
					rightArm.xRot = Mth.lerp(0.6F, rightArm.xRot, -0.55F);
					rightArm.yRot = -0.15F;
				}
			}
			copySleeves();
		}

		private void slash(float t) {
			if (t < SaberBoss.SLASH_HIT) { // wind-up: blade lifted high behind the shoulder
				float k = ease(t / SaberBoss.SLASH_HIT);
				rightArm.xRot = Mth.lerp(k, -0.5F, -2.7F);
				rightArm.yRot = Mth.lerp(k, 0, -0.6F);
				body.yRot = Mth.lerp(k, 0, -0.35F);
				leftArm.xRot = Mth.lerp(k, 0, -0.6F);
			} else { // the cut: across the body, then settle
				float k = ease(Math.min(1, (t - SaberBoss.SLASH_HIT) / 3F));
				float back = ease(Math.max(0, Math.min(1, (t - SaberBoss.SLASH_HIT - 4) / 8F)));
				rightArm.xRot = Mth.lerp(back, Mth.lerp(k, -2.7F, -1.35F), -0.55F);
				rightArm.yRot = Mth.lerp(back, Mth.lerp(k, -0.6F, 0.95F), -0.15F);
				body.yRot = Mth.lerp(back, Mth.lerp(k, -0.35F, 0.45F), 0);
			}
			sync();
		}

		private void flying(float t) {
			if (t < SaberBoss.FLY_RELEASE) { // coil: blade drawn back low, free hand aiming
				float k = ease(t / SaberBoss.FLY_RELEASE);
				rightArm.xRot = Mth.lerp(k, -0.5F, 0.7F);
				rightArm.yRot = Mth.lerp(k, 0, 0.5F);
				rightArm.zRot = Mth.lerp(k, 0, 0.4F);
				leftArm.xRot = Mth.lerp(k, 0, -1.5F);
				leftArm.yRot = Mth.lerp(k, 0, 0.3F);
				body.yRot = Mth.lerp(k, 0, -0.55F);
			} else { // throw: a flat, wide sweep forward
				float k = ease(Math.min(1, (t - SaberBoss.FLY_RELEASE) / 3F));
				float back = ease(Math.max(0, Math.min(1, (t - SaberBoss.FLY_RELEASE - 4) / 6F)));
				rightArm.xRot = Mth.lerp(back, Mth.lerp(k, 0.7F, -1.6F), -0.55F);
				rightArm.yRot = Mth.lerp(back, Mth.lerp(k, 0.5F, -0.9F), -0.15F);
				rightArm.zRot = Mth.lerp(back, Mth.lerp(k, 0.4F, 0), 0);
				leftArm.xRot = Mth.lerp(back, -1.5F, 0);
				body.yRot = Mth.lerp(back, Mth.lerp(k, -0.55F, 0.5F), 0);
			}
			sync();
		}

		private void combo(float t) {
			int[] hits = {SaberBoss.COMBO_HITS[0], SaberBoss.COMBO_HITS[1], SaberBoss.COMBO_HITS[2]};
			if (t < hits[0]) { // crouch, blade low and back
				float k = ease(t / hits[0]);
				body.xRot = Mth.lerp(k, 0, 0.35F);
				head.xRot = Mth.lerp(k, head.xRot, -0.2F);
				rightArm.xRot = Mth.lerp(k, -0.5F, 0.4F);
				rightArm.yRot = Mth.lerp(k, 0, 0.7F);
				leftArm.xRot = Mth.lerp(k, 0, -0.3F);
			} else if (t < hits[1]) { // cut 1: left to right
				float k = ease(Math.min(1, (t - hits[0]) / 3F));
				body.xRot = 0.2F;
				rightArm.xRot = -1.45F;
				rightArm.yRot = Mth.lerp(k, 0.8F, -1.0F);
				body.yRot = Mth.lerp(k, 0.4F, -0.45F);
			} else if (t < hits[2]) { // cut 2: right to left (after a short rewind)
				float k = ease(Math.max(0, Math.min(1, (t - hits[1]) / 3F)));
				body.xRot = 0.2F;
				rightArm.xRot = -1.45F;
				rightArm.yRot = Mth.lerp(k, -1.0F, 0.95F);
				body.yRot = Mth.lerp(k, -0.45F, 0.5F);
				if (t > hits[1] + 4) { // raise for the finisher
					float r = ease(Math.min(1, (t - hits[1] - 4) / (hits[2] - hits[1] - 4F)));
					rightArm.xRot = Mth.lerp(r, -1.45F, -3.0F);
					rightArm.yRot = Mth.lerp(r, 0.95F, 0);
					leftArm.xRot = Mth.lerp(r, 0, -2.8F);
					body.yRot = Mth.lerp(r, 0.5F, 0);
					body.xRot = Mth.lerp(r, 0.2F, -0.15F);
				}
			} else { // cut 3: overhead chop, then winded
				float k = ease(Math.min(1, (t - hits[2]) / 3F));
				float tired = ease(Math.max(0, Math.min(1, (t - hits[2] - 6) / 8F)));
				rightArm.xRot = Mth.lerp(k, -3.0F, -0.5F);
				leftArm.xRot = Mth.lerp(k, -2.8F, -0.5F);
				body.xRot = Mth.lerp(k, -0.15F, 0.45F);
				if (tired > 0) {
					rightArm.xRot = Mth.lerp(tired, -0.5F, 0.15F);
					leftArm.xRot = Mth.lerp(tired, -0.5F, 0.1F);
					body.xRot = Mth.lerp(tired, 0.45F, 0.3F);
					head.xRot = 0.35F;
				}
			}
			sync();
		}

		/** Arms and head follow a bent or turned body. */
		private void sync() {
			if (body.xRot != 0) {
				float lean = body.xRot;
				head.y = lean * 4F;
				rightArm.y = 2.0F + lean * 2F;
				leftArm.y = 2.0F + lean * 2F;
				rightArm.z = -lean * 3F;
				leftArm.z = -lean * 3F;
			}
			if (body.yRot != 0) {
				rightArm.z = Mth.sin(body.yRot) * 5.0F;
				rightArm.x = -Mth.cos(body.yRot) * 5.0F;
				leftArm.z = -Mth.sin(body.yRot) * 5.0F;
				leftArm.x = Mth.cos(body.yRot) * 5.0F;
			}
		}

		private void copySleeves() {
			rightSleeve.copyFrom(rightArm);
			leftSleeve.copyFrom(leftArm);
			jacket.copyFrom(body);
			hat.copyFrom(head);
			rightPants.copyFrom(rightLeg);
			leftPants.copyFrom(leftLeg);
		}

		private static float ease(float x) {
			x = Math.max(0, Math.min(1, x));
			return x * x * (3 - 2 * x);
		}
	}
}
