package com.grandline.client.camera;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * With the Grand Line camera, what the reticle is on is what you get: the ray is cast from the camera through the
 * screen centre (not from the character's eyes), starting at the character so nothing behind them is picked. The hit
 * still has to be within normal reach of the character's eyes (blocks: the game mode's pick range; entities: 3, or 6 in
 * creative), as vanilla, so the server accepts every use and attack.
 */
public final class CrosshairPick {
	private CrosshairPick() {
	}

	/** Picks for this frame; returns false when the Grand Line camera is not in charge (vanilla picks instead). */
	public static boolean pick(Minecraft mc, float partial) {
		Entity player = mc.getCameraEntity();
		if (player == null || player != mc.player || mc.level == null || mc.gameMode == null || !GrandLineCamera.active()
				|| !GrandLineCamera.hasCameraPosition()) {
			return false;
		}
		mc.crosshairPickEntity = null;
		Vec3 eye = player.getEyePosition(partial);
		Vec3 dir = GrandLineCamera.lookVector();
		Vec3 cam = GrandLineCamera.cameraPosition();
		double along = eye.subtract(cam).dot(dir);
		Vec3 start = cam.add(dir.scale(Math.max(0, along - 0.6)));
		double blockReach = mc.gameMode.getPickRange();
		double entityReach = mc.gameMode.hasFarPickRange() ? 6.0 : 3.0;
		double length = 0.6 + Math.max(blockReach, entityReach) + 1.5;
		Vec3 end = start.add(dir.scale(length));
		BlockHitResult block = mc.level.clip(new ClipContext(start, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
		boolean blockOk = block.getType() == HitResult.Type.BLOCK && block.getLocation().distanceTo(eye) <= blockReach;
		double maxSq = block.getType() == HitResult.Type.BLOCK ? start.distanceToSqr(block.getLocation()) : length * length;
		AABB box = new AABB(start, end).inflate(1.0);
		EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, start, end, box,
				e -> e != player && !e.isSpectator() && e.isPickable() && !player.isPassengerOfSameVehicle(e), maxSq);
		HitResult result;
		if (hit != null) {
			if (hit.getLocation().distanceToSqr(eye) <= entityReach * entityReach) {
				result = hit;
				if (hit.getEntity() instanceof LivingEntity || hit.getEntity() instanceof ItemFrame) {
					mc.crosshairPickEntity = hit.getEntity();
				}
			} else {
				result = miss(hit.getLocation(), dir);
			}
		} else {
			result = blockOk ? block : miss(blockOk || block.getType() != HitResult.Type.BLOCK ? end : block.getLocation(), dir);
		}
		mc.hitResult = result;
		return true;
	}

	private static BlockHitResult miss(Vec3 at, Vec3 dir) {
		return BlockHitResult.miss(at, Direction.getNearest(dir.x, dir.y, dir.z), BlockPos.containing(at));
	}

	/**
	 * Where the reticle points in the world (for aiming moves): the first block or entity along the camera's ray, up to
	 * {@code range} blocks past the character, or the end of that ray.
	 */
	public static Vec3 aimPoint(Minecraft mc, double range) {
		Entity player = mc.player;
		Vec3 dir = GrandLineCamera.lookVector();
		Vec3 eye = player.getEyePosition();
		Vec3 cam = GrandLineCamera.hasCameraPosition() ? GrandLineCamera.cameraPosition() : eye;
		double along = eye.subtract(cam).dot(dir);
		Vec3 start = cam.add(dir.scale(Math.max(0, along - 0.6)));
		Vec3 end = start.add(dir.scale(range));
		BlockHitResult block = mc.level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		Vec3 point = block.getType() == HitResult.Type.BLOCK ? block.getLocation() : end;
		EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, start, point, new AABB(start, point).inflate(1.0),
				e -> e != player && !e.isSpectator() && e.isPickable(), start.distanceToSqr(point));
		return hit != null ? hit.getLocation() : point;
	}
}
