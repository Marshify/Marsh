package com.grandline.fruit;

import com.grandline.combat.ability.AbilityAction;
import com.grandline.combat.ability.AbilityContext;
import com.grandline.combat.ability.AbilityHelpers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Reusable move templates. Each fruit combines these shapes with its {@link Element}, so 30 moves share one tested set
 * of targeting code. Returned values are "targets hit" (used for mastery).
 */
public final class FruitMoves {
	private FruitMoves() {
	}

	/** A projectile-like bolt: hits everything along a line in front of the player. */
	public static AbilityAction bolt(Element element, double range, double width, float damage) {
		return ctx -> {
			AbilityHelpers.particleLine(ctx.player(), element.particle(), range);
			AbilityHelpers.sound(ctx.player(), element.sound(), 1.2F);
			return hitAll(ctx, element, AbilityHelpers.alongLine(ctx.player(), range, width), damage);
		};
	}

	/** A burst around the player. */
	public static AbilityAction burst(Element element, double radius, float damage) {
		return ctx -> {
			Vec3 center = ctx.player().position();
			AbilityHelpers.burst(ctx.player(), element.particle(), center.add(0, 1, 0), radius, 60);
			AbilityHelpers.sound(ctx.player(), element.sound(), 0.8F);
			return hitAll(ctx, element, AbilityHelpers.inRadius(ctx.player(), center, radius), damage);
		};
	}

	/** An eruption at the point the player is looking at (up to {@code range} away), launching targets upward. */
	public static AbilityAction eruption(Element element, double range, double radius, float damage, double launch) {
		return ctx -> {
			Vec3 point = targetPoint(ctx.player(), range);
			AbilityHelpers.burst(ctx.player(), element.particle(), point.add(0, 1, 0), radius, 80);
			ctx.level().playSound(null, point.x, point.y, point.z, element.sound(), net.minecraft.sounds.SoundSource.PLAYERS, 1.2F, 0.7F);
			if (element == Element.LIGHTNING) {
				LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(ctx.level());
				if (bolt != null) {
					bolt.moveTo(point);
					bolt.setVisualOnly(true);
					ctx.level().addFreshEntity(bolt);
				}
			}
			int hits = 0;
			for (LivingEntity target : AbilityHelpers.inRadius(ctx.player(), point, radius)) {
				if (ctx.damage(target, damage)) {
					element.onHit(ctx, target);
					target.setDeltaMovement(target.getDeltaMovement().add(0, launch, 0));
					target.hurtMarked = true;
					hits++;
				}
			}
			return hits;
		};
	}

	/** Leaves a damaging area at the target point for {@code seconds}, plus an initial hit. */
	public static AbilityAction lingering(Element element, double range, double radius, float initialDamage, float pulseDamage, int seconds) {
		return ctx -> {
			Vec3 point = targetPoint(ctx.player(), range);
			LingeringAreas.add(ctx.player(), point, radius, seconds * 20, element, pulseDamage, ctx.multiplier());
			AbilityHelpers.burst(ctx.player(), element.particle(), point.add(0, 0.5, 0), radius, 50);
			AbilityHelpers.sound(ctx.player(), element.sound(), 0.6F);
			return hitAll(ctx, element, AbilityHelpers.inRadius(ctx.player(), point, radius), initialDamage);
		};
	}

	/** Dashes forward, damaging everything along the way. */
	public static AbilityAction dash(Element element, double strength, float damage) {
		return ctx -> {
			List<LivingEntity> targets = AbilityHelpers.alongLine(ctx.player(), strength * 4, 1.5);
			AbilityHelpers.particleLine(ctx.player(), element.particle(), strength * 4);
			AbilityHelpers.dash(ctx.player(), strength, 0.25);
			AbilityHelpers.sound(ctx.player(), element.sound(), 1.4F);
			return damage <= 0 ? 0 : hitAll(ctx, element, targets, damage);
		};
	}

	/** Launches the player upward and grants slow falling (a flight-like escape). */
	public static AbilityAction updraft(Element element) {
		return ctx -> {
			ServerPlayer player = ctx.player();
			player.setDeltaMovement(player.getDeltaMovement().x, 1.6, player.getDeltaMovement().z);
			player.hurtMarked = true;
			player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 120, 0, false, false));
			AbilityHelpers.burst(player, element.particle(), player.position(), 2.0, 40);
			AbilityHelpers.sound(player, element.sound(), 1.6F);
			return 0;
		};
	}

	/** Instantly moves the player up to {@code distance} blocks forward, stopping before walls. */
	public static AbilityAction blink(Element element, double distance) {
		return ctx -> {
			ServerPlayer player = ctx.player();
			Vec3 start = player.position().add(0, 0.5, 0);
			Vec3 dir = new Vec3(player.getLookAngle().x, 0, player.getLookAngle().z).normalize();
			BlockHitResult hit = player.serverLevel().clip(new ClipContext(start, start.add(dir.scale(distance)),
					ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
			Vec3 end = hit.getType() == HitResult.Type.MISS ? start.add(dir.scale(distance)) : hit.getLocation().subtract(dir.scale(0.8));
			AbilityHelpers.particleLine(player, element.particle(), start.distanceTo(end));
			player.teleportTo(end.x, player.getY(), end.z);
			player.fallDistance = 0;
			AbilityHelpers.sound(player, element.sound(), 1.8F);
			return 0;
		};
	}

	/** Heals the player and nearby players, with brief regeneration. */
	public static AbilityAction heal(Element element, float amount, double radius) {
		return ctx -> {
			ServerPlayer caster = ctx.player();
			float scaled = (float) (amount * ctx.multiplier());
			for (ServerPlayer ally : caster.serverLevel().getEntitiesOfClass(ServerPlayer.class, caster.getBoundingBox().inflate(radius))) {
				ally.heal(scaled);
				ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 1), caster);
			}
			AbilityHelpers.burst(caster, element.particle(), caster.position().add(0, 1, 0), radius, 50);
			AbilityHelpers.sound(caster, element.sound(), 1.4F);
			return 0;
		};
	}

	/** Shields the player: damage resistance and absorption hearts. */
	public static AbilityAction barrier(Element element, int seconds, int absorptionLevel) {
		return ctx -> {
			ServerPlayer caster = ctx.player();
			caster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, seconds * 20, 1), caster);
			caster.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, seconds * 20, absorptionLevel), caster);
			AbilityHelpers.burst(caster, element.particle(), caster.position().add(0, 1, 0), 1.5, 40);
			AbilityHelpers.sound(caster, element.sound(), 1.0F);
			return 0;
		};
	}

	/** Pulls everything near the target point toward it, then damages it. */
	public static AbilityAction vortex(Element element, double range, double radius, float damage) {
		return ctx -> {
			Vec3 point = targetPoint(ctx.player(), range);
			AbilityHelpers.burst(ctx.player(), element.particle(), point.add(0, 1, 0), radius, 80);
			ctx.level().playSound(null, point.x, point.y, point.z, element.sound(), net.minecraft.sounds.SoundSource.PLAYERS, 1.2F, 0.6F);
			int hits = 0;
			for (LivingEntity target : AbilityHelpers.inRadius(ctx.player(), point, radius)) {
				Vec3 pull = point.subtract(target.position()).scale(0.35);
				target.setDeltaMovement(pull.x, 0.25, pull.z);
				target.hurtMarked = true;
				if (ctx.damage(target, damage)) {
					element.onHit(ctx, target);
					hits++;
				}
			}
			return hits;
		};
	}

	/** Leaps forward and up, slamming everything around the take-off point. */
	public static AbilityAction leap(Element element, double radius, float damage) {
		return ctx -> {
			int hits = hitAll(ctx, element, AbilityHelpers.inRadius(ctx.player(), ctx.player().position(), radius), damage);
			AbilityHelpers.dash(ctx.player(), 1.4, 1.0);
			ctx.player().addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 40, 0, false, false));
			AbilityHelpers.burst(ctx.player(), element.particle(), ctx.player().position(), radius, 40);
			AbilityHelpers.sound(ctx.player(), element.sound(), 1.0F);
			return hits;
		};
	}

	/** A long-range strike on the first enemy in front (stretching punch, grabbing claw, etc.). */
	public static AbilityAction strike(Element element, double range, float damage) {
		return ctx -> {
			LivingEntity target = AbilityHelpers.firstInFront(ctx.player(), range);
			AbilityHelpers.particleLine(ctx.player(), element.particle(), target == null ? range : ctx.player().distanceTo(target));
			AbilityHelpers.sound(ctx.player(), element.sound(), 1.1F);
			if (target != null && ctx.damage(target, damage)) {
				element.onHit(ctx, target);
				return 1;
			}
			return 0;
		};
	}

	/** Toggled, steerable flight with an element trail. Movement only; energy drains while airborne. */
	public static AbilityAction flight(Element element, int maxTicks, float speed) {
		return ctx -> {
			AbilityHelpers.sound(ctx.player(), element.sound(), 1.3F);
			com.grandline.combat.ability.FlightService.start(ctx.player(), ctx.power(), ctx.ability(), maxTicks, 0.012, speed,
					(flier, ticks) -> com.grandline.combat.ability.Projectiles.particles(flier.serverLevel(), element.particle(),
							flier.position(), 3, 0.2, 0.02));
			return 0;
		};
	}

	private static int hitAll(AbilityContext ctx, Element element, List<LivingEntity> targets, float damage) {
		int hits = 0;
		for (LivingEntity target : targets) {
			if (ctx.damage(target, damage)) {
				element.onHit(ctx, target);
				hits++;
			}
		}
		return hits;
	}

	/** Block the player looks at within {@code range}, or the point {@code range} blocks ahead. */
	private static Vec3 targetPoint(ServerPlayer player, double range) {
		Vec3 eye = player.getEyePosition();
		Vec3 end = eye.add(player.getLookAngle().scale(range));
		BlockHitResult hit = player.serverLevel().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, player));
		return hit.getType() == HitResult.Type.MISS ? end : hit.getLocation();
	}
}
