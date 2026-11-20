package com.grandline.technique;

import com.grandline.combat.EnergyService;
import com.grandline.combat.fx.FxEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Flash Step (bought at level 100): an instant relocation to where the crosshair points, up to {@code flashStepRange}
 * blocks, up onto ledges or down to lower ground. The destination is worked out on the server: never through a wall,
 * never inside blocks (the whole body must fit), never into unloaded chunks, outside the world border or the world's
 * height. If nowhere valid is found the step is cancelled and costs nothing.
 */
public final class FlashStep {
	private FlashStep() {
	}

	static String perform(ServerPlayer player) {
		var config = TechniqueService.config();
		if (!TechniqueService.get(player).flashStep()) {
			TechniqueService.tell(player, "message.grandline.technique.not_learned", net.minecraft.network.chat.Component.translatable("technique.grandline.flash_step"));
			return "not_owned";
		}
		TechniqueService.Runtime r = TechniqueService.runtime(player);
		long now = TechniqueService.now(player);
		if (now < r.flashReadyAt) {
			return "cooldown";
		}
		if (TechniqueService.immobilized(player)) {
			return "immobilized";
		}
		if (!EnergyService.canAfford(player, config.flashStepEnergy / 100.0)) {
			TechniqueService.tell(player, "message.grandline.ability.no_energy");
			return "no_energy";
		}
		Vec3 to = destination(player, config.flashStepRange);
		if (to == null) {
			TechniqueService.tell(player, "message.grandline.flash_step.blocked");
			return "no_destination";
		}
		EnergyService.trySpend(player, config.flashStepEnergy / 100.0);
		r.flashReadyAt = now + config.flashStepCooldownTicks;
		Vec3 from = player.position();
		Vec3 dir = to.subtract(from);
		dir = dir.lengthSqr() < 1e-6 ? player.getLookAngle() : dir.normalize();
		ServerLevel level = player.serverLevel();
		FxEvents.send(player, "tq:flash_out", from.add(0, 0.9, 0), dir, 0, from.distanceTo(to), player.getYRot());
		level.playSound(null, from.x, from.y, from.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.35f, 1.9f);
		level.playSound(null, from.x, from.y, from.z, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.9f, 2.0f);
		player.connection.teleport(to.x, to.y, to.z, player.getYRot(), player.getXRot());
		player.setDeltaMovement(Vec3.ZERO);
		player.hurtMarked = true;
		player.fallDistance = 0;
		player.resetFallDistance();
		FxEvents.send(player, "tq:flash_in", to.add(0, 0.9, 0), dir, 0, 0, 0);
		level.playSound(null, to.x, to.y, to.z, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.25f, 2.0f);
		level.playSound(null, to.x, to.y, to.z, SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.PLAYERS, 1.0f, 1.7f);
		TechniqueService.sync(player);
		return "ok";
	}

	/**
	 * Where the player would land: along the crosshair ray, stopping short of the first solid block, then pulled back
	 * until the whole body fits. Prefers standing on ground just below the point (so stepping onto a ledge or down to a
	 * lower floor lands on it); otherwise hangs in the air there. Null if no spot in reach is valid.
	 */
	public static Vec3 destination(ServerPlayer player, double range) {
		ServerLevel level = player.serverLevel();
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getLookAngle();
		BlockHitResult hit = level.clip(new ClipContext(eye, eye.add(look.scale(range)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		double reach = hit.getType() == HitResult.Type.MISS ? range : hit.getLocation().distanceTo(eye);
		double eyeHeight = player.getEyeHeight();
		for (double d = reach - 0.3; d >= 1.0; d -= 0.25) {
			Vec3 point = eye.add(look.scale(d));
			// Standing on the ground under the aimed point (up to 2.5 blocks down), else the body hanging from it.
			Vec3 standing = standOn(level, player, point);
			if (standing != null && valid(level, player, standing, eye)) {
				return standing;
			}
			Vec3 hanging = point.subtract(0, eyeHeight * 0.5, 0);
			if (valid(level, player, hanging, eye)) {
				return hanging;
			}
		}
		return null;
	}

	private static Vec3 standOn(ServerLevel level, ServerPlayer player, Vec3 point) {
		BlockPos start = BlockPos.containing(point);
		for (int dy = 0; dy <= 3; dy++) {
			BlockPos pos = start.below(dy);
			if (!level.hasChunkAt(pos)) {
				return null;
			}
			var shape = level.getBlockState(pos).getCollisionShape(level, pos);
			if (!shape.isEmpty()) {
				double top = pos.getY() + shape.max(net.minecraft.core.Direction.Axis.Y);
				if (point.y - top > 2.6 || top > point.y + 0.5) {
					return null;
				}
				return new Vec3(point.x, top, point.z);
			}
		}
		return null;
	}

	/** The body fits at {@code feet}, the spot is loaded, inside the world and the border, and visible from the eye. */
	static boolean valid(ServerLevel level, ServerPlayer player, Vec3 feet, Vec3 eye) {
		AABB box = player.getDimensions(player.getPose()).makeBoundingBox(feet);
		if (box.minY < level.getMinBuildHeight() || box.maxY > level.getMaxBuildHeight()) {
			return false;
		}
		if (!level.getWorldBorder().isWithinBounds(box) || !level.hasChunksAt(BlockPos.containing(box.minX, box.minY, box.minZ),
				BlockPos.containing(box.maxX, box.maxY, box.maxZ))) {
			return false;
		}
		if (!level.noCollision(player, box) || level.containsAnyLiquid(box.deflate(0.1)) && level.getFluidState(BlockPos.containing(feet.add(0, 1.5, 0))).isSource()) {
			return false;
		}
		// Never through a wall: the body's centre must be in plain sight of the eye.
		Vec3 centre = feet.add(0, player.getBbHeight() * 0.5, 0);
		BlockHitResult wall = level.clip(new ClipContext(eye, centre, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		return wall.getType() == HitResult.Type.MISS || wall.getLocation().distanceToSqr(centre) < 0.04;
	}
}
