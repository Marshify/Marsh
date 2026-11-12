package com.grandline.combat.ability;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

/** Server-side targeting, movement and effect helpers shared by all moves. */
public final class AbilityHelpers {
	private AbilityHelpers() {
	}

	/** Living entities (not the caster, not spectators) within {@code radius} of {@code center}. */
	public static List<LivingEntity> inRadius(ServerPlayer caster, Vec3 center, double radius) {
		AABB box = new AABB(center, center).inflate(radius);
		return caster.serverLevel().getEntitiesOfClass(LivingEntity.class, box, entity -> entity != caster
				&& entity.isAlive() && !entity.isSpectator() && !(entity instanceof Player p && p.isCreative())
				&& entity.position().distanceToSqr(center) <= radius * radius);
	}

	/** Entities in front of the caster: within {@code range} and inside a cone of {@code halfAngleDegrees}. */
	public static List<LivingEntity> inCone(ServerPlayer caster, double range, double halfAngleDegrees) {
		Vec3 eye = caster.getEyePosition();
		Vec3 look = caster.getLookAngle().normalize();
		double minDot = Math.cos(Math.toRadians(halfAngleDegrees));
		return inRadius(caster, eye, range).stream()
				.filter(entity -> {
					Vec3 to = entity.getBoundingBox().getCenter().subtract(eye);
					double length = to.length();
					return length < 1.0 || to.scale(1.0 / length).dot(look) >= minDot;
				})
				.sorted(Comparator.comparingDouble(entity -> entity.distanceToSqr(caster)))
				.toList();
	}

	/** The closest entity in a narrow cone in front of the caster, or null. */
	public static LivingEntity firstInFront(ServerPlayer caster, double range) {
		List<LivingEntity> targets = inCone(caster, range, 25);
		return targets.isEmpty() ? null : targets.get(0);
	}

	/** Entities along a straight line from the caster's eyes, within {@code width} of the line. */
	public static List<LivingEntity> alongLine(ServerPlayer caster, double length, double width) {
		Vec3 start = caster.getEyePosition();
		Vec3 dir = caster.getLookAngle().normalize();
		return inRadius(caster, start.add(dir.scale(length / 2)), length / 2 + width).stream()
				.filter(entity -> {
					Vec3 to = entity.getBoundingBox().getCenter().subtract(start);
					double along = to.dot(dir);
					return along >= 0 && along <= length && to.subtract(dir.scale(along)).length() <= width;
				})
				.toList();
	}

	/** Pushes the caster in the look direction (horizontal) and syncs the motion to their client. */
	public static void dash(ServerPlayer caster, double strength, double lift) {
		Vec3 look = caster.getLookAngle();
		Vec3 horizontal = new Vec3(look.x, 0, look.z).normalize().scale(strength);
		caster.setDeltaMovement(horizontal.x, lift, horizontal.z);
		caster.hurtMarked = true;
		caster.fallDistance = 0;
	}

	/** Knocks {@code target} away from {@code from}. */
	public static void knockAway(LivingEntity target, Vec3 from, double strength, double lift) {
		Vec3 away = target.position().subtract(from);
		Vec3 horizontal = new Vec3(away.x, 0, away.z);
		horizontal = horizontal.lengthSqr() < 1.0e-4 ? new Vec3(0, 0, 0) : horizontal.normalize().scale(strength);
		target.setDeltaMovement(target.getDeltaMovement().add(horizontal.x, lift, horizontal.z));
		target.hurtMarked = true;
	}

	/** Draws a line of particles from the caster's eyes. */
	public static void particleLine(ServerPlayer caster, ParticleOptions particle, double length) {
		Vec3 start = caster.getEyePosition().subtract(0, 0.3, 0);
		Vec3 dir = caster.getLookAngle().normalize();
		for (double d = 0.5; d <= length; d += 0.5) {
			Vec3 p = start.add(dir.scale(d));
			caster.serverLevel().sendParticles(particle, p.x, p.y, p.z, 2, 0.1, 0.1, 0.1, 0.01);
		}
	}

	/** A ring/burst of particles around a point. */
	public static void burst(ServerPlayer caster, ParticleOptions particle, Vec3 center, double radius, int count) {
		caster.serverLevel().sendParticles(particle, center.x, center.y, center.z, count, radius / 2, 0.4, radius / 2, 0.05);
	}

	public static void sound(ServerPlayer caster, SoundEvent sound, float pitch) {
		caster.serverLevel().playSound(null, caster.blockPosition(), sound, SoundSource.PLAYERS, 1.0F, pitch);
	}
}
