package com.grandline.combat.ability;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * Server-simulated projectiles for moves. Each projectile moves a fixed distance per tick along the direction it was
 * fired, sweeps that segment for blocks and living entities (with a hit radius), draws its own particles so every
 * nearby client sees it travel, and calls {@code onImpact} once where it lands. Nothing about the flight or the hit
 * comes from the client.
 */
public final class Projectiles {
	/** What a projectile looks like each tick. */
	@FunctionalInterface
	public interface Trail {
		void draw(ServerLevel level, Vec3 position, int age);
	}

	/** Where a projectile ended: the point, and the entity it struck (empty for a block or the end of its range). */
	public record Impact(Vec3 position, Optional<LivingEntity> entity) {
	}

	private Projectiles() {
	}

	/** The point the player's crosshair is aimed at, up to {@code range} blocks away (blocks only). */
	public static Vec3 crosshairTarget(ServerPlayer player, double range) {
		Vec3 eye = player.getEyePosition();
		Vec3 end = eye.add(player.getLookAngle().scale(range));
		BlockHitResult hit = player.serverLevel().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		return hit.getType() == HitResult.Type.MISS ? end : hit.getLocation();
	}

	/**
	 * What the crosshair points at: the body centre of the first living entity on the look ray within {@code range},
	 * else the block point. Use this when a projectile starts away from the eye (e.g. above the head), so it flies to
	 * the enemy the player is aiming at instead of the ground behind them.
	 */
	public static Vec3 crosshairAim(ServerPlayer player, double range) {
		Vec3 eye = player.getEyePosition();
		Vec3 blockPoint = crosshairTarget(player, range);
		Vec3 look = player.getLookAngle();
		AABB box = player.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0);
		net.minecraft.world.phys.EntityHitResult hit = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(
				player.serverLevel(), player, eye, blockPoint, box, e -> e instanceof LivingEntity && e.isAlive() && !e.isSpectator());
		return hit == null ? blockPoint : hit.getEntity().getBoundingBox().getCenter();
	}

	/**
	 * Fires a projectile.
	 *
	 * @param start     launch point
	 * @param direction travel direction (normalised internally)
	 * @param speed     blocks per tick
	 * @param range     maximum travel distance in blocks
	 * @param radius    hit radius around the projectile's path
	 */
	public static void fire(ServerPlayer owner, Vec3 start, Vec3 direction, double speed, double range, double radius, Trail trail,
			Consumer<Impact> onImpact) {
		Vec3 velocity = direction.normalize().scale(speed);
		Vec3[] position = {start};
		double[] travelled = {0};
		TimedEffects.start(age -> {
			if (owner.isRemoved()) {
				return false;
			}
			ServerLevel level = owner.serverLevel();
			Vec3 from = position[0];
			Vec3 to = from.add(velocity);

			// Blocks.
			BlockHitResult block = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, owner));
			Vec3 limit = block.getType() == HitResult.Type.MISS ? to : block.getLocation();

			// Living entities along the swept segment, nearest first.
			LivingEntity struck = null;
			double best = Double.MAX_VALUE;
			AABB sweep = new AABB(from, limit).inflate(radius + 1.0);
			for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, sweep, e -> e != owner && e.isAlive() && !e.isSpectator())) {
				Optional<Vec3> clip = entity.getBoundingBox().inflate(radius).clip(from, limit);
				if (clip.isPresent()) {
					double distance = clip.get().distanceToSqr(from);
					if (distance < best) {
						best = distance;
						struck = entity;
					}
				}
			}

			Vec3 drawTo = struck != null ? from.add(limit.subtract(from).normalize().scale(Math.sqrt(best))) : limit;
			for (double d = 0; d <= drawTo.distanceTo(from); d += 0.5) {
				trail.draw(level, from.add(velocity.normalize().scale(d)), age);
			}

			if (struck != null) {
				onImpact.accept(new Impact(drawTo, Optional.of(struck)));
				return false;
			}
			travelled[0] += limit.distanceTo(from);
			if (block.getType() != HitResult.Type.MISS || travelled[0] >= range) {
				onImpact.accept(new Impact(limit, Optional.empty()));
				return false;
			}
			position[0] = to;
			return true;
		});
	}

	/** Particles visible from far away (moves are big and should be seen across an island). */
	public static void particles(ServerLevel level, ParticleOptions particle, Vec3 at, int count, double spread, double speed) {
		for (ServerPlayer viewer : level.players()) {
			if (viewer.distanceToSqr(at) < 128 * 128) {
				level.sendParticles(viewer, particle, true, at.x, at.y, at.z, count, spread, spread, spread, speed);
			}
		}
	}
}
