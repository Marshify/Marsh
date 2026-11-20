package com.grandline.client.render.aura;

import com.grandline.GrandLineMod;
import com.grandline.client.technique.ClientTechniques;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * Aura's coating: a glossy charcoal shell drawn over the player's body (skin and clothes stay untouched underneath) on
 * a model a hair larger than the skin's outer layer. Each stage has its own coverage mask: hands, then arms, torso,
 * head, the upper legs, then the whole body. Works for wide and slim models and follows every animation (the shell's
 * parts copy the body's pose each frame). The tint is a parameter so coloured Aura can be added later.
 */
public final class AuraLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
	private static final ResourceLocation[] STAGES = new ResourceLocation[6];
	static {
		for (int i = 0; i < STAGES.length; i++) {
			STAGES[i] = GrandLineMod.id("textures/entity/aura/stage" + i + ".png");
		}
	}

	private static PlayerModel<AbstractClientPlayer> wide;
	private static PlayerModel<AbstractClientPlayer> slim;

	/** Coating colour (multiplied with the texture): plain for now; future Aura colours change only this. */
	public static float[] tint = {1f, 1f, 1f};

	public AuraLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
		super(parent);
	}

	public static ResourceLocation texture(int stage) {
		return STAGES[Math.max(0, Math.min(5, stage))];
	}

	/** The shell model for a player's skin shape (built once). */
	public static PlayerModel<AbstractClientPlayer> model(AbstractClientPlayer player) {
		boolean isSlim = "slim".equals(player.getModelName());
		if (isSlim) {
			if (slim == null) {
				slim = build(true);
			}
			return slim;
		}
		if (wide == null) {
			wide = build(false);
		}
		return wide;
	}

	private static PlayerModel<AbstractClientPlayer> build(boolean slimArms) {
		ModelPart root = LayerDefinition.create(PlayerModel.createMesh(new CubeDeformation(0.3F), slimArms), 64, 64).bakeRoot();
		PlayerModel<AbstractClientPlayer> m = new PlayerModel<>(root, slimArms);
		m.hat.visible = false;
		m.jacket.visible = false;
		m.leftSleeve.visible = false;
		m.rightSleeve.visible = false;
		m.leftPants.visible = false;
		m.rightPants.visible = false;
		return m;
	}

	@Override
	public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
			float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
		int stage = ClientTechniques.auraStageOf(player);
		if (stage < 0 || player.isInvisible()) {
			return;
		}
		PlayerModel<AbstractClientPlayer> shell = model(player);
		getParentModel().copyPropertiesTo(shell);
		shell.crouching = getParentModel().crouching;
		shell.hat.visible = false;
		shell.jacket.visible = false;
		shell.leftSleeve.visible = false;
		shell.rightSleeve.visible = false;
		shell.leftPants.visible = false;
		shell.rightPants.visible = false;
		VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucent(texture(stage)));
		shell.renderToBuffer(pose, consumer, light, OverlayTexture.NO_OVERLAY, tint[0], tint[1], tint[2], 1f);
	}

	/** First person: the coating over the visible arm (hands are coated from the first stage). */
	public static void renderArm(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player, ModelPart arm, boolean right) {
		int stage = ClientTechniques.auraStageOf(player);
		if (stage < 0) {
			return;
		}
		PlayerModel<AbstractClientPlayer> shell = model(player);
		ModelPart coat = right ? shell.rightArm : shell.leftArm;
		coat.copyFrom(arm);
		coat.visible = true;
		coat.render(pose, buffers.getBuffer(RenderType.entityTranslucent(texture(stage))), light, OverlayTexture.NO_OVERLAY, tint[0], tint[1], tint[2], 1f);
	}
}
