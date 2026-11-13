package com.grandline.entity;

import com.grandline.combat.ability.TimedEffects;
import com.grandline.combat.fx.FxEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * The Flying Saber Slash: a crescent of sword energy that the server moves {@link #SPEED} blocks a tick in a straight,
 * level line. Each tick it sweeps its path for walls and for living things (1.4 blocks wide, 1.6 tall); it hits the
 * first one once and dissipates, or breaks on a wall or after {@link #RANGE} blocks. Clients draw the crescent from the
 * launch beat and are told where it ended.
 */
public final class SaberWave {
	public static final double SPEED = 0.9;
	public static final double RANGE = 22;
	static final double HALF_WIDTH = 0.7;

	private SaberWave() {
	}

	public static void launch(SaberBoss boss, Vec3 start, Vec3 direction, float damage) {
		ServerLevel level = (ServerLevel) boss.level();
		Vec3 dir = direction.multiply(1, 0, 1).normalize();
		Vec3 velocity = dir.scale(SPEED);
		FxEvents.sendFrom(boss, "boss_wave", start, dir, SPEED, RANGE, 1.0);
		level.playSound(null, start.x, start.y, start.z, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.2F, 1.4F);
		Vec3[] pos = {start};
		double[] travelled = {0};
		TimedEffects.start(age -> {
			if (boss.level() != level) {
				return false;
			}
			Vec3 from = pos[0];
			Vec3 to = from.add(velocity);
			BlockHitResult block = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, boss));
			Vec3 limit = block.getType() == HitResult.Type.MISS ? to : block.getLocation();
			LivingEntity struck = null;
			double best = Double.MAX_VALUE;
			AABB sweep = new AABB(from, limit).inflate(HALF_WIDTH + 1, 1.2, HALF_WIDTH + 1);
			for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, sweep,
					e -> e != boss && e.isAlive() && !e.isSpectator() && !(e instanceof Islander) && !(e instanceof GrandLineEnemy))) {
				Optional<Vec3> clip = e.getBoundingBox().inflate(HALF_WIDTH, 0.3, HALF_WIDTH).clip(from, limit);
				if (clip.isPresent() && clip.get().distanceToSqr(from) < best) {
					best = clip.get().distanceToSqr(from);
					struck = e;
				}
			}
			if (struck != null) {
				boss.hitWithSword(struck, damage);
				struck.knockback(0.6, -velocity.x, -velocity.z);
				end(boss, level, struck.position().add(0, 1, 0));
				return false;
			}
			travelled[0] += limit.distanceTo(from);
			if (block.getType() != HitResult.Type.MISS || travelled[0] >= RANGE) {
				end(boss, level, limit);
				return false;
			}
			pos[0] = to;
			return true;
		});
	}

	private static void end(SaberBoss boss, ServerLevel level, Vec3 at) {
		FxEvents.sendFrom(boss, "boss_wave_end", at, Vec3.ZERO, 0, 0, 1.0);
		level.playSound(null, at.x, at.y, at.z, SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.HOSTILE, 1.0F, 0.7F);
	}
}
