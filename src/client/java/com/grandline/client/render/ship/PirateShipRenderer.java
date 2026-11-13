package com.grandline.client.render.ship;

import com.grandline.GrandLineMod;
import com.grandline.ship.PirateShip;
import com.grandline.ship.ShipClass;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Draws a ship: the hull model with its class texture, then the sails in the owner's side colours. */
public final class PirateShipRenderer extends EntityRenderer<PirateShip> {
	private static final ResourceLocation[] SAILS = {
			GrandLineMod.id("textures/entity/ship/sails_neutral.png"),
			GrandLineMod.id("textures/entity/ship/sails_pirate.png"),
			GrandLineMod.id("textures/entity/ship/sails_marine.png")};

	private final ShipClass ship;
	private final ModelPart hull;
	private final ModelPart sails;
	private final ResourceLocation hullTexture;

	public PirateShipRenderer(EntityRendererProvider.Context context, ShipClass ship) {
		super(context);
		this.ship = ship;
		this.hull = context.bakeLayer(ShipModels.hullLayer(ship));
		this.sails = context.bakeLayer(ShipModels.sailLayer(ship));
		this.hullTexture = GrandLineMod.id("textures/entity/ship/" + ship.id + ".png");
		this.shadowRadius = ship.width * 0.6F;
		ShipModels.Layout layout = ShipModels.layout(ship);
		for (int i = 0; i < layout.masts().length; i++) {
			ModelPart sail = sails.getChild("sail" + i);
			sail.xScale = layout.masts()[i][2] * 8 / 64F;
			sail.yScale = layout.masts()[i][3] * 8 / 48F;
		}
		ModelPart flag = sails.getChild("flag");
		flag.xScale = 12 / 64F;
		flag.yScale = 8 / 48F;
	}

	public static void register() {
		for (ShipClass ship : ShipClass.values()) {
			EntityModelLayerRegistry.registerModelLayer(ShipModels.hullLayer(ship), () -> ShipModels.hull(ship));
			EntityModelLayerRegistry.registerModelLayer(ShipModels.sailLayer(ship), () -> ShipModels.sails(ship));
			EntityRendererRegistry.register(com.grandline.ship.ShipEntities.typeOf(ship), ctx -> new PirateShipRenderer(ctx, ship));
		}
	}

	@Override
	public void render(PirateShip entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
		pose.pushPose();
		pose.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));
		float hurt = entity.getHurtTime() - partialTick;
		if (hurt > 0) {
			pose.mulPose(Axis.ZP.rotationDegrees(Mth.sin(hurt) * hurt * 0.6F * entity.getHurtDir()));
		}
		// Gentle roll and pitch on the waves.
		float t = (entity.tickCount + partialTick) * 0.05F;
		pose.mulPose(Axis.ZP.rotationDegrees(Mth.sin(t) * 1.2F));
		pose.mulPose(Axis.XP.rotationDegrees(Mth.sin(t * 0.7F + 1) * 0.6F));
		pose.scale(-2, -2, 2);
		hull.render(pose, buffers.getBuffer(RenderType.entityCutout(hullTexture)), light, OverlayTexture.NO_OVERLAY);
		sails.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(SAILS[Math.max(0, Math.min(2, entity.side()))])), light,
				OverlayTexture.NO_OVERLAY);
		pose.popPose();
		super.render(entity, yaw, partialTick, pose, buffers, light);
	}

	/** Keep drawing while any part of the long hull or the masts is on screen, not only the square hitbox. */
	@Override
	public boolean shouldRender(PirateShip entity, Frustum frustum, double x, double y, double z) {
		return entity.shouldRender(x, y, z) && frustum.isVisible(entity.getBoundingBox().inflate(ship.length / 2.0, 12, ship.length / 2.0));
	}

	@Override
	public ResourceLocation getTextureLocation(PirateShip entity) {
		return hullTexture;
	}
}
