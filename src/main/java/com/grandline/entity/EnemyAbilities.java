package com.grandline.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.Set;

/**
 * What some island looks can do beyond fighting (by {@link GrandLineEnemy#skin()}):
 * <ul>
 * <li>Aura: some spawn with it (a chance per look): +25% damage and the dark coating on their arms.</li>
 * <li>Flash Step: now and then, with a target 5-12 blocks away, a blink to just in front of it, only onto clear, safe
 * ground inside their own leash (never into a wall, lava or past their combat area).</li>
 * <li>Magma moves (Emberfall's Marshal, after the Magma Fruit): lobbed magma shots, a burning pool under the target, a
 * geyser that erupts under the target after a warning. They hurt players only and change no blocks.</li>
 * </ul>
 */
public final class EnemyAbilities {
	/** Chance to spawn with Aura, by look. */
	public static final Map<String, Double> AURA_CHANCE = Map.of("ashguard_trooper", 0.5, "cinder_infiltrator", 1.0);
	public static final Set<String> FLASH_STEP = Set.of("cinder_infiltrator");
	public static final Set<String> MAGMA = Set.of("emberfall_marshal");
	public static final double AURA_DAMAGE = 0.25;
	private static final java.util.UUID AURA_ID = java.util.UUID.fromString("5d2c7a51-6b9e-4c7f-9a51-1e2f3a4b5c6d");

	private EnemyAbilities() {
	}

	/** On spawn: rolls the look's Aura. */
	public static void onSpawn(GrandLineEnemy enemy) {
		double chance = AURA_CHANCE.getOrDefault(enemy.skin(), 0.0);
		setAura(enemy, chance > 0 && enemy.getRandom().nextDouble() < chance);
	}

	public static void setAura(GrandLineEnemy enemy, boolean on) {
		enemy.setAura(on);
		var attack = enemy.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
		if (attack == null) {
			return;
		}
		attack.removeModifier(AURA_ID);
		if (on) {
			attack.addPermanentModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(AURA_ID, "Aura",
					AURA_DAMAGE, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.MULTIPLY_TOTAL));
		}
	}

	/** Every server tick of an enemy with a target. */
	public static void tick(GrandLineEnemy enemy) {
		if (!(enemy.level() instanceof ServerLevel level) || !(enemy.getTarget() instanceof ServerPlayer target) || !target.isAlive()) {
			return;
		}
		String skin = enemy.skin();
		if (FLASH_STEP.contains(skin) && enemy.tickCount % 20 == 0 && enemy.getRandom().nextFloat() < 0.25f) {
			flashStep(level, enemy, target);
		}
		if (MAGMA.contains(skin)) {
			magma(level, enemy, target);
		}
	}

	// ------------------------------------------------------------------------------------------------- Flash Step

	static boolean flashStep(ServerLevel level, GrandLineEnemy enemy, LivingEntity target) {
		double d = enemy.distanceTo(target);
		if (d < 5 || d > 12) {
			return false;
		}
		Vec3 dir = target.position().subtract(enemy.position()).multiply(1, 0, 1).normalize();
		Vec3 dest = target.position().subtract(dir.scale(1.6));
		BlockPos feet = BlockPos.containing(dest);
		// Inside its own combat area, on safe ground (no lava, fire or drop), with room to stand: never through walls.
		if (enemy.origin() != null && Math.hypot(feet.getX() - enemy.origin().getX(), feet.getZ() - enemy.origin().getZ()) > enemy.behavior().pursuitRadius
				|| !GrandLineEnemy.safeGround(level, feet) || level.getBlockState(feet.below()).isAir()
				|| level.getBlockState(feet.below()).is(Blocks.LAVA) || !level.noCollision(enemy, enemy.getBoundingBox().move(dest.subtract(enemy.position())))
				|| !enemy.hasLineOfSight(target)) {
			return false;
		}
		level.sendParticles(ParticleTypes.CLOUD, enemy.getX(), enemy.getY() + 1, enemy.getZ(), 12, 0.3, 0.5, 0.3, 0.02);
		enemy.teleportTo(dest.x, dest.y, dest.z);
		enemy.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, target.getEyePosition());
		level.playSound(null, enemy.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 0.6f, 1.6f);
		level.sendParticles(ParticleTypes.CLOUD, dest.x, dest.y + 1, dest.z, 12, 0.3, 0.5, 0.3, 0.02);
		return true;
	}

	// ------------------------------------------------------------------------------------------------ Magma moves

	private static void magma(ServerLevel level, GrandLineEnemy boss, ServerPlayer target) {
		int t = boss.tickCount % 200; // a 10-second cycle: shots, pool, geyser
		float damage = (float) boss.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
		if (t == 20 || t == 30 || t == 40) {
			// Magma shot: a lobbed glob that lands where the target stands.
			Vec3 from = boss.getEyePosition(), to = target.position();
			for (int i = 0; i <= 10; i++) {
				Vec3 p = from.lerp(to, i / 10.0).add(0, Math.sin(Math.PI * i / 10.0) * 2.5, 0);
				level.sendParticles(ParticleTypes.LAVA, p.x, p.y, p.z, 1, 0, 0, 0, 0);
				level.sendParticles(ParticleTypes.FLAME, p.x, p.y, p.z, 2, 0.05, 0.05, 0.05, 0.01);
			}
			level.playSound(null, boss.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 0.8f, 0.7f);
			hurtAround(level, boss, to, 1.6, damage * 0.6f, 3);
		}
		if (t >= 80 && t < 140) {
			// Burning pool: a 2.5-block patch under where the target stood when it began.
			Vec3 at = boss.poolCenter();
			if (t == 80 || at == null) {
				at = target.position();
				boss.setPoolCenter(at);
				level.playSound(null, BlockPos.containing(at), SoundEvents.LAVA_AMBIENT, SoundSource.HOSTILE, 1f, 0.8f);
			}
			level.sendParticles(ParticleTypes.FLAME, at.x, at.y + 0.1, at.z, 6, 1.2, 0.05, 1.2, 0.01);
			level.sendParticles(ParticleTypes.LAVA, at.x, at.y + 0.1, at.z, 1, 1.0, 0, 1.0, 0);
			if (t % 10 == 0) {
				hurtAround(level, boss, at, 2.5, damage * 0.25f, 2);
			}
		}
		if (t == 160) {
			boss.setPoolCenter(target.position()); // the geyser's spot, shown for a second first
		}
		if (t >= 160 && t < 180 && boss.poolCenter() != null) {
			Vec3 at = boss.poolCenter();
			level.sendParticles(ParticleTypes.SMOKE, at.x, at.y + 0.1, at.z, 4, 0.6, 0.05, 0.6, 0.01);
		}
		if (t == 180 && boss.poolCenter() != null) {
			Vec3 at = boss.poolCenter();
			for (int y = 0; y < 6; y++) {
				level.sendParticles(ParticleTypes.LAVA, at.x, at.y + y, at.z, 4, 0.4, 0.2, 0.4, 0);
				level.sendParticles(ParticleTypes.FLAME, at.x, at.y + y, at.z, 8, 0.5, 0.3, 0.5, 0.02);
			}
			level.playSound(null, BlockPos.containing(at), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 0.8f, 1.3f);
			for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, new AABB(at, at).inflate(1.8, 3, 1.8), ServerPlayer::isAlive)) {
				if (p.hurt(boss.damageSources().mobAttack(boss), damage * 1.2f)) {
					p.setDeltaMovement(p.getDeltaMovement().add(0, 0.9, 0));
					p.hurtMarked = true;
					p.setSecondsOnFire(3);
				}
			}
			boss.setPoolCenter(null);
		}
	}

	private static void hurtAround(ServerLevel level, GrandLineEnemy boss, Vec3 at, double r, float damage, int fireSeconds) {
		for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, new AABB(at, at).inflate(r, 2, r), ServerPlayer::isAlive)) {
			if (p.hurt(boss.damageSources().mobAttack(boss), damage)) {
				p.setSecondsOnFire(fireSeconds);
			}
		}
	}
}
