package com.grandline.client.camera;

import com.grandline.client.mixin.CameraAccessor;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

/**
 * Places the camera each frame in the third-person back view: rotation from the Grand Line camera, pivot at the eyes
 * (with a slight height lag on jumps and landings), over the right shoulder while facing is locked, then pulled back to
 * the wanted distance, never further than the first block in the way (vanilla's 8-ray check, so it cannot clip).
 * No objects are allocated.
 */
public final class CameraRig {
	private static final double SHOULDER = 0.9, SHOULDER_UP = 0.25, ELEVATION = 0.2;

	private CameraRig() {
	}

	public static void place(Camera camera, Entity entity, float partial) {
		Minecraft mc = Minecraft.getInstance();
		if (entity != mc.player || !GrandLineCamera.active()) {
			return;
		}
		CameraAccessor rig = (CameraAccessor) camera;
		double dt = GrandLineCamera.frameSeconds();
		double want = GrandLineCamera.frame(dt);
		float yaw = GrandLineCamera.yaw(), pitch = GrandLineCamera.pitch();
		rig.grandline$setRotation(yaw, pitch);
		double x = Mth.lerp(partial, entity.xo, entity.getX());
		double z = Mth.lerp(partial, entity.zo, entity.getZ());
		double eye = Mth.lerp(partial, entity.yo, entity.getY()) + entity.getEyeHeight();
		double y = GrandLineCamera.pivotY(eye, dt) + ELEVATION;
		double s = GrandLineCamera.shoulderAmount();
		double r = Math.toRadians(yaw);
		// Right of the view direction is -(cos yaw, sin yaw) in Minecraft's axes.
		rig.grandline$setPosition(x - Math.cos(r) * SHOULDER * s, y + SHOULDER_UP * s, z - Math.sin(r) * SHOULDER * s);
		double distance = GrandLineCamera.settle(rig.grandline$getMaxZoom(want), dt);
		rig.grandline$move(-distance, 0, 0);
	}
}
