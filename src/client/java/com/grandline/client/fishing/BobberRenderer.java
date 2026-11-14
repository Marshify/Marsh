package com.grandline.client.fishing;

import com.grandline.fishing.BobberEntity;
import com.grandline.fishing.StarterRodItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * The bobber (a camera-facing float) and the line from the rod tip, sagging between them; taut when a fish is on. The
 * rod-tip position follows the angler's hand in first and third person, as for a vanilla rod.
 */
public class BobberRenderer extends EntityRenderer<BobberEntity> {
	private static final ResourceLocation TEXTURE = new ResourceLocation("textures/entity/fishing_hook.png");
	private static final RenderType BOBBER = RenderType.entityCutout(TEXTURE);

	public BobberRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public void render(BobberEntity bobber, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
		Player player = bobber.owner();
		if (player == null) {
			return;
		}
		pose.pushPose();
		pose.pushPose();
		pose.scale(0.5F, 0.5F, 0.5F);
		pose.mulPose(entityRenderDispatcher.cameraOrientation());
		pose.mulPose(Axis.YP.rotationDegrees(180.0F));
		PoseStack.Pose last = pose.last();
		VertexConsumer vc = buffers.getBuffer(BOBBER);
		quadVertex(vc, last.pose(), last.normal(), light, 0, 0, 0, 1);
		quadVertex(vc, last.pose(), last.normal(), light, 1, 0, 1, 1);
		quadVertex(vc, last.pose(), last.normal(), light, 1, 1, 1, 0);
		quadVertex(vc, last.pose(), last.normal(), light, 0, 1, 0, 0);
		pose.popPose();

		int side = player.getMainArm() == HumanoidArm.RIGHT ? 1 : -1;
		if (!(player.getMainHandItem().getItem() instanceof StarterRodItem)) {
			side = -side;
		}
		float swing = Mth.sin(Mth.sqrt(player.getAttackAnim(partial)) * Mth.PI);
		float body = Mth.lerp(partial, player.yBodyRotO, player.yBodyRot) * Mth.DEG_TO_RAD;
		double sin = Mth.sin(body), cos = Mth.cos(body);
		double handX, handY, handZ;
		float eye;
		if ((entityRenderDispatcher.options == null || entityRenderDispatcher.options.getCameraType().isFirstPerson())
				&& player == Minecraft.getInstance().player) {
			double fov = 960.0 / entityRenderDispatcher.options.fov().get();
			Vec3 p = entityRenderDispatcher.camera.getNearPlane().getPointOnPlane(side * 0.525F, -0.1F).scale(fov)
					.yRot(swing * 0.5F).xRot(-swing * 0.7F);
			handX = Mth.lerp(partial, player.xo, player.getX()) + p.x;
			handY = Mth.lerp(partial, player.yo, player.getY()) + p.y;
			handZ = Mth.lerp(partial, player.zo, player.getZ()) + p.z;
			eye = player.getEyeHeight();
		} else {
			handX = Mth.lerp(partial, player.xo, player.getX()) - cos * side * 0.35 - sin * 0.8;
			handY = player.yo + player.getEyeHeight() + (player.getY() - player.yo) * partial - 0.45;
			handZ = Mth.lerp(partial, player.zo, player.getZ()) - sin * side * 0.35 + cos * 0.8;
			eye = player.isCrouching() ? -0.1875F : 0.0F;
		}
		double bx = Mth.lerp(partial, bobber.xo, bobber.getX());
		double by = Mth.lerp(partial, bobber.yo, bobber.getY()) + 0.25;
		double bz = Mth.lerp(partial, bobber.zo, bobber.getZ());
		float dx = (float) (handX - bx), dy = (float) (handY - by) + eye, dz = (float) (handZ - bz);
		boolean taut = bobber.state() == BobberEntity.HOOKED;
		VertexConsumer line = buffers.getBuffer(RenderType.lineStrip());
		PoseStack.Pose lp = pose.last();
		for (int i = 0; i <= 16; i++) {
			lineVertex(dx, dy, dz, line, lp, i / 16f, (i + 1) / 16f, taut);
		}
		pose.popPose();
		super.render(bobber, yaw, partial, pose, buffers, light);
	}

	private static void quadVertex(VertexConsumer vc, Matrix4f m, Matrix3f n, int light, float x, int y, int u, int v) {
		vc.vertex(m, x - 0.5F, y - 0.5F, 0).color(255, 255, 255, 255).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY)
				.uv2(light).normal(n, 0, 1, 0).endVertex();
	}

	/** A point along the line: a gentle sag when slack, nearly straight when a fish pulls. */
	private static void lineVertex(float x, float y, float z, VertexConsumer vc, PoseStack.Pose pose, float t1, float t2, boolean taut) {
		float sag = taut ? 0.15f : 1f;
		float ax = x * t1, ay = y * (t1 * t1 * sag + t1 * (2 - sag)) * 0.5F + 0.25F, az = z * t1;
		float bx = x * t2 - ax, by = y * (t2 * t2 * sag + t2 * (2 - sag)) * 0.5F + 0.25F - ay, bz = z * t2 - az;
		float len = Mth.sqrt(bx * bx + by * by + bz * bz);
		vc.vertex(pose.pose(), ax, ay, az).color(40, 40, 46, 255).normal(pose.normal(), bx / len, by / len, bz / len).endVertex();
	}

	@Override
	public ResourceLocation getTextureLocation(BobberEntity entity) {
		return TEXTURE;
	}
}
