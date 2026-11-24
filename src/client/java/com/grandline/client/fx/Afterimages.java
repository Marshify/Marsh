package com.grandline.client.fx;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Player afterimages (reusable): while recording, a snapshot of the caster's position and body yaw is kept every tick;
 * each is drawn as a translucent, steel-tinted copy of the player's own model (same skin, current pose) that fades and
 * dissolves over a few ticks. Pure rendering: no entities are created, so nothing can be left behind.
 */
public final class Afterimages extends GrandLineFx.Effect {
	private record Snap(Vec3 pos, float yaw, int born) {
	}

	private final List<Snap> snaps = new ArrayList<>();
	private final int fade;
	private final float r, g, b;
	boolean recording = true;

	public Afterimages(int caster, int fadeTicks, int tint) {
		super(caster);
		this.fade = fadeTicks;
		this.r = (tint >> 16 & 255) / 255f;
		this.g = (tint >> 8 & 255) / 255f;
		this.b = (tint & 255) / 255f;
	}

	public void stop() {
		recording = false;
	}

	@Override
	void cancel() {
		recording = false;
	}

	@Override
	boolean tick(ClientLevel level) {
		age++;
		Entity c = caster(level);
		if (recording && c != null) {
			snaps.add(new Snap(c.position(), c instanceof net.minecraft.world.entity.LivingEntity l ? l.yBodyRot : c.getYRot(), age));
		}
		snaps.removeIf(s -> age - s.born() > fade);
		return recording && c != null || !snaps.isEmpty();
	}

	@Override
	void render(FxDraw d, ClientLevel level) {
		Entity c = caster(level);
		if (!(c instanceof AbstractClientPlayer player)) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		if (!(mc.getEntityRenderDispatcher().getRenderer(player) instanceof PlayerRenderer renderer)) {
			return;
		}
		var model = renderer.getModel();
		var buffer = d.buffers.getBuffer(RenderType.entityTranslucent(player.getSkinTextureLocation()));
		PoseStack pose = d.pose;
		for (Snap s : snaps) {
			float t = (age - s.born() + d.partial) / fade;
			if (t >= 1 || s.pos().distanceToSqr(player.position()) < 0.6 || s.pos().add(0, 1, 0).distanceToSqr(d.camPos) < 2.5 * 2.5) {
				continue; // never on top of the player themselves, nor filling the view when it trails through the camera
			}
			pose.pushPose();
			pose.translate(s.pos().x - d.camPos.x, s.pos().y - d.camPos.y, s.pos().z - d.camPos.z);
			pose.mulPose(Axis.YP.rotationDegrees(180 - s.yaw()));
			pose.scale(-1, -1, 1);
			pose.translate(0, -1.501, 0);
			model.renderToBuffer(pose, buffer, 0xF000F0, OverlayTexture.NO_OVERLAY, r, g, b, 0.42f * (1 - t));
			pose.popPose();
		}
	}
}
