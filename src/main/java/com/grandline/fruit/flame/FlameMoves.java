package com.grandline.fruit.flame;

import com.grandline.combat.ability.AbilityContext;
import com.grandline.combat.ability.AbilityDef;
import com.grandline.combat.ability.AbilityHelpers;
import com.grandline.combat.ability.ChargeService;
import com.grandline.combat.ability.FlightService;
import com.grandline.combat.ability.Projectiles;
import com.grandline.combat.ability.TimedEffects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Flame Fruit (base form), modelled on the unawakened Flame moveset of Blox Fruits:
 * <ul>
 *     <li>Z Fire Bullets: a quick volley of small fire shots toward the crosshair, each bursting on impact.</li>
 *     <li>X Burning Blast: a big flaming fist that explodes where it lands, knocking enemies back.</li>
 *     <li>C Fire Column: a pillar of fire erupts around the caster, launching nearby enemies upward.</li>
 *     <li>V Flame Destroyer: tap for a quick fireball, hold to charge a giant one with a wider explosion.</li>
 *     <li>R Fire Flight: steerable flight with a flame trail; movement only, no damage.</li>
 * </ul>
 * All aiming uses the server's view of the player's look direction (the crosshair); all hits are computed here.
 */
public final class FlameMoves {
	// --- Balance (tuned for Grand Line's health and stat scaling; damage is multiplied by the Fruit stat). ---
	static final int BULLETS = 5;
	static final int BULLET_INTERVAL_TICKS = 2;
	static final float BULLET_DAMAGE = 4.0f;
	static final double BULLET_SPEED = 2.2;
	static final double BULLET_RANGE = 34;

	static final float BLAST_DAMAGE = 15.0f;
	static final double BLAST_RADIUS = 4.0;
	static final double BLAST_SPEED = 1.5;
	static final double BLAST_RANGE = 32;

	static final float COLUMN_DAMAGE = 13.0f;
	static final double COLUMN_RADIUS = 4.5;
	static final int COLUMN_HEIGHT = 9;
	static final int COLUMN_WINDUP_TICKS = 4;

	static final float DESTROYER_DAMAGE = 18.0f;
	static final float DESTROYER_BONUS_DAMAGE = 16.0f;
	static final double DESTROYER_RADIUS = 5.0;
	static final double DESTROYER_BONUS_RADIUS = 3.5;
	static final double DESTROYER_SPEED = 1.15;
	static final double DESTROYER_RANGE = 42;

	static final int FLIGHT_MAX_TICKS = 240;
	static final double FLIGHT_DRAIN_PER_TICK = 0.012;
	static final float FLIGHT_SPEED = 0.075f;

	private FlameMoves() {
	}

	public static List<AbilityDef> moves() {
		AbilityDef destroyer = new AbilityDef("flame_destroyer", 100, 300, 0.30, AbilityDef.Activation.CHARGED, FlameMoves::flameDestroyer);
		ChargeService.visual(destroyer, FlameMoves::chargingFireball);
		return List.of(
				new AbilityDef("flame_bullets", 1, 50, 0.07, FlameMoves::fireBullets),
				new AbilityDef("flame_blast", 25, 140, 0.15, FlameMoves::burningBlast),
				new AbilityDef("flame_column", 50, 200, 0.20, FlameMoves::fireColumn),
				destroyer,
				new AbilityDef("flame_flight", 10, 100, 0.05, AbilityDef.Activation.TOGGLE, FlameMoves::fireFlight));
	}

	// ------------------------------------------------------------------------------------------------ Z

	static int fireBullets(AbilityContext ctx) {
		ServerPlayer player = ctx.player();
		sound(player, SoundEvents.BLAZE_SHOOT, 1.4F);
		TimedEffects.start(age -> {
			if (age % BULLET_INTERVAL_TICKS != 0) {
				return true;
			}
			int shot = age / BULLET_INTERVAL_TICKS;
			if (shot >= BULLETS || !player.isAlive()) {
				return false;
			}
			// Re-aim every shot at the current crosshair, with a slight alternating spread.
			Vec3 target = Projectiles.crosshairTarget(player, BULLET_RANGE);
			Vec3 side = player.getLookAngle().cross(new Vec3(0, 1, 0)).normalize().scale((shot % 2 == 0 ? 1 : -1) * 0.35);
			Vec3 start = player.getEyePosition().subtract(0, 0.25, 0).add(side);
			Projectiles.fire(player, start, target.subtract(start), BULLET_SPEED, BULLET_RANGE, 0.35,
					(level, pos, a) -> Projectiles.particles(level, ParticleTypes.FLAME, pos, 1, 0.02, 0.0),
					impact -> {
						ServerLevel level = player.serverLevel();
						Projectiles.particles(level, ParticleTypes.FLAME, impact.position(), 10, 0.25, 0.05);
						Projectiles.particles(level, ParticleTypes.LAVA, impact.position(), 1, 0.1, 0.0);
						impact.entity().ifPresent(target2 -> {
							if (ctx.damage(target2, BULLET_DAMAGE)) {
								target2.setSecondsOnFire(2);
								AbilityHelpers.knockAway(target2, player.position(), 0.15, 0.05);
							}
						});
					});
			if (shot > 0) {
				sound(player, SoundEvents.FIRECHARGE_USE, 1.6F + shot * 0.05F);
			}
			return true;
		});
		return 0;
	}

	// ------------------------------------------------------------------------------------------------ X

	static int burningBlast(AbilityContext ctx) {
		ServerPlayer player = ctx.player();
		Vec3 start = player.getEyePosition().subtract(0, 0.3, 0).add(player.getLookAngle().scale(0.8));
		Vec3 target = Projectiles.crosshairTarget(player, BLAST_RANGE);
		sound(player, SoundEvents.BLAZE_SHOOT, 0.7F);
		Projectiles.fire(player, start, target.subtract(start), BLAST_SPEED, BLAST_RANGE, 0.9,
				(level, pos, age) -> {
					// A big fist-sized fireball with a smoky tail.
					Projectiles.particles(level, ParticleTypes.FLAME, pos, 6, 0.35, 0.01);
					Projectiles.particles(level, ParticleTypes.LARGE_SMOKE, pos, 1, 0.2, 0.0);
				},
				impact -> explode(ctx, impact.position(), BLAST_RADIUS, BLAST_DAMAGE, 1.3, 0.45, 4));
		return 0;
	}

	// ------------------------------------------------------------------------------------------------ C

	static int fireColumn(AbilityContext ctx) {
		ServerPlayer player = ctx.player();
		Vec3 center = player.position();
		sound(player, SoundEvents.FIRECHARGE_USE, 0.6F);
		TimedEffects.start(age -> {
			ServerLevel level = player.serverLevel();
			// Rising pillar: particles climb over ~16 ticks after a short wind-up ring.
			double height = Math.min(COLUMN_HEIGHT, age * 0.9);
			for (int ring = 0; ring < 10; ring++) {
				double angle = ring * Math.PI / 5 + age * 0.4;
				double r = COLUMN_RADIUS * (age < COLUMN_WINDUP_TICKS ? 0.9 : 0.55);
				Vec3 p = center.add(Math.cos(angle) * r, height * ((ring % 3) / 3.0), Math.sin(angle) * r);
				Projectiles.particles(level, ParticleTypes.FLAME, p, 3, 0.2, 0.02);
			}
			Projectiles.particles(level, ParticleTypes.FLAME, center.add(0, height / 2, 0), 12, 0.6, 0.08);
			if (age == COLUMN_WINDUP_TICKS) {
				sound(player, SoundEvents.GENERIC_EXPLODE, 1.4F);
				Projectiles.particles(level, ParticleTypes.LAVA, center.add(0, 0.5, 0), 12, 1.5, 0.0);
				for (LivingEntity target : AbilityHelpers.inRadius(player, center, COLUMN_RADIUS + 1.0)) {
					double dy = target.getY() - center.y;
					if (Math.hypot(target.getX() - center.x, target.getZ() - center.z) <= COLUMN_RADIUS && dy > -2 && dy < COLUMN_HEIGHT
							&& ctx.damage(target, COLUMN_DAMAGE)) {
						target.setSecondsOnFire(5);
						target.setDeltaMovement(target.getDeltaMovement().x * 0.3, 1.15, target.getDeltaMovement().z * 0.3);
						target.hurtMarked = true;
					}
				}
			}
			return age < 18;
		});
		return 0;
	}

	// ------------------------------------------------------------------------------------------------ V

	/** While V is held: a fireball grows above the caster's head. */
	static void chargingFireball(ServerPlayer player, double charge, int ticks) {
		Vec3 above = player.position().add(0, player.getBbHeight() + 1.2 + charge, 0);
		double size = 0.4 + charge * 1.4;
		Projectiles.particles(player.serverLevel(), ParticleTypes.FLAME, above, (int) (6 + charge * 20), size, 0.01);
		if (ticks % 6 == 0) {
			sound(player, SoundEvents.FIRE_AMBIENT, 0.6F + (float) charge);
		}
	}

	static int flameDestroyer(AbilityContext ctx) {
		ServerPlayer player = ctx.player();
		double charge = ctx.charge();
		double size = 1.0 + charge * 1.2;
		Vec3 start = player.position().add(0, player.getBbHeight() + 1.2 + charge, 0);
		Vec3 target = Projectiles.crosshairTarget(player, DESTROYER_RANGE);
		sound(player, SoundEvents.BLAZE_SHOOT, 0.5F);
		Projectiles.fire(player, start, target.subtract(start), DESTROYER_SPEED - charge * 0.25, DESTROYER_RANGE, size,
				(level, pos, age) -> {
					Projectiles.particles(level, ParticleTypes.FLAME, pos, (int) (14 + charge * 26), size * 0.6, 0.01);
					Projectiles.particles(level, ParticleTypes.LAVA, pos, 1, size * 0.4, 0.0);
				},
				impact -> explode(ctx, impact.position(), DESTROYER_RADIUS + DESTROYER_BONUS_RADIUS * charge,
						(float) (DESTROYER_DAMAGE + DESTROYER_BONUS_DAMAGE * charge), 1.6 + charge * 0.6, 0.6, 6));
		return 0;
	}

	// ------------------------------------------------------------------------------------------------ R

	static int fireFlight(AbilityContext ctx) {
		ServerPlayer player = ctx.player();
		sound(player, SoundEvents.FIRECHARGE_USE, 1.2F);
		FlightService.start(player, ctx.power(), ctx.ability(), FLIGHT_MAX_TICKS, FLIGHT_DRAIN_PER_TICK, FLIGHT_SPEED,
				(flier, ticks) -> {
					Vec3 feet = flier.position();
					Projectiles.particles(flier.serverLevel(), ParticleTypes.FLAME, feet, 4, 0.15, 0.02);
					if (ticks % 3 == 0) {
						Projectiles.particles(flier.serverLevel(), ParticleTypes.SMOKE, feet.subtract(0, 0.3, 0), 2, 0.2, 0.01);
					}
					if (ticks % 20 == 0) {
						sound(flier, SoundEvents.FIRE_AMBIENT, 1.0F);
					}
				});
		return 0;
	}

	// ------------------------------------------------------------------------------------------------ shared

	/** Fire explosion: damage falls off to 60% at the edge, enemies are knocked away from the centre. No block damage. */
	static int explode(AbilityContext ctx, Vec3 center, double radius, float damage, double knockback, double lift, int burnSeconds) {
		ServerPlayer player = ctx.player();
		ServerLevel level = player.serverLevel();
		level.playSound(null, center.x, center.y, center.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.3F, 0.9F);
		Projectiles.particles(level, ParticleTypes.EXPLOSION, center, Math.max(2, (int) radius), radius * 0.35, 0.0);
		Projectiles.particles(level, ParticleTypes.FLAME, center, (int) (radius * 18), radius * 0.4, 0.12);
		int hits = 0;
		for (LivingEntity target : AbilityHelpers.inRadius(player, center, radius)) {
			double falloff = 1.0 - 0.4 * Math.min(1.0, target.position().distanceTo(center) / radius);
			if (ctx.damage(target, (float) (damage * falloff))) {
				target.setSecondsOnFire(burnSeconds);
				AbilityHelpers.knockAway(target, center, knockback, lift);
				hits++;
			}
		}
		return hits;
	}

	private static void sound(ServerPlayer player, SoundEvent sound, float pitch) {
		player.serverLevel().playSound(null, player.blockPosition(), sound, SoundSource.PLAYERS, 1.0F, pitch);
	}
}
