package com.grandline.gear.sword;

import com.grandline.combat.ability.AbilityAction;
import com.grandline.combat.ability.AbilityContext;
import com.grandline.combat.ability.AbilityDef;
import com.grandline.combat.ability.Projectiles;
import com.grandline.combat.ability.TimedEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;

/** Helpers shared by every sword's moves: move definitions, dashes, crescent shots, poses and end beats. */
class SwordMoveBase {
	/** Pose kinds the client plays for sword moves (see MovePoses). */
	static final int POSE_SLASH_R = 0, POSE_SLASH_L = 1, POSE_OVERHEAD = 2, POSE_THRUST = 3, POSE_SPIN = 4, POSE_RAISE = 5,
			POSE_GROUND = 6, POSE_THROW = 7;

	/** A move: mastery, cooldown in seconds and energy (percent of max energy, so 15 = 15%). */
	static AbilityDef def(String key, int mastery, double cooldownSeconds, int energy, AbilityAction action) {
		return new AbilityDef(key, mastery, (int) Math.round(cooldownSeconds * 20), energy / 100.0, action);
	}

	static AbilityDef charged(String key, int mastery, double cooldownSeconds, int energy, AbilityAction action) {
		return new AbilityDef(key, mastery, (int) Math.round(cooldownSeconds * 20), energy / 100.0, AbilityDef.Activation.CHARGED, action);
	}

	/** Plays a body pose on every viewer: windup now, released after {@code releaseIn} ticks (0 = straight away). */
	static void pose(ServerPlayer p, int kind, int releaseIn) {
		SwordKit.fx(p, "pose", "none", kind, p.position(), Vec3.ZERO, releaseIn);
	}

	/** Ends the caster's nearest travelling visual at {@code at} with an impact flash of {@code flash} size. */
	static void end(ServerPlayer p, String palette, Vec3 at, double flash) {
		SwordKit.fx(p, "end", palette, 0, at, Vec3.ZERO, flash);
	}

	static Vec3 aim(ServerPlayer p, Vec3 from, double range) {
		Vec3 target = Projectiles.crosshairAim(p, range);
		Vec3 d = target.subtract(from);
		return d.lengthSqr() < 1e-4 ? p.getLookAngle() : d.normalize();
	}

	/** {@code dir} turned by {@code degrees} around the vertical axis. */
	static Vec3 yaw(Vec3 dir, double degrees) {
		double a = Math.toRadians(degrees), c = Math.cos(a), s = Math.sin(a);
		return new Vec3(dir.x * c - dir.z * s, dir.y, dir.x * s + dir.z * c);
	}

	/**
	 * A dash: the caster is carried {@code distance} blocks along {@code dir} (flat) over {@code ticks} ticks, stopping at
	 * walls. Everything the body passes within {@code width} is hit once with {@code damage}; {@code onHit} runs for each.
	 * Sends the "dash" beat (variant 1 adds an afterimage) and returns the set of enemies struck (filled as it goes).
	 */
	static Set<Integer> dash(AbilityContext ctx, String palette, int variant, Vec3 dir, double distance, int ticks, double width,
			float damage, BiConsumer<LivingEntity, Vec3> onHit) {
		ServerPlayer p = ctx.player();
		Vec3 start = p.position();
		Vec3 flat = SwordKit.flat(dir);
		Vec3 end = SwordKit.reach(p, start, flat, distance);
		double real = end.distanceTo(start);
		SwordKit.fx(p, "dash", palette, variant, start.add(0, 0.9, 0), flat, real / Math.max(1, ticks), real, width);
		Set<Integer> struck = new HashSet<>();
		Vec3[] last = {start};
		TimedEffects.start(age -> {
			if (SwordKit.gone(p)) {
				return false;
			}
			double t = Math.min(1, (age + 1) / (double) ticks);
			Vec3 target = start.add(end.subtract(start).scale(t));
			SwordKit.steer(p, target, distance);
			Vec3 now = target;
			for (LivingEntity e : SwordKit.swept(p, last[0].add(0, 0.9, 0), now.add(0, 0.9, 0), width)) {
				if (struck.add(e.getId()) && SwordKit.hitFresh(ctx, e, damage)) {
					onHit.accept(e, now);
				}
			}
			last[0] = now;
			if (age + 1 >= ticks) {
				// Arrive with a little momentum left, not a dead stop.
				p.setDeltaMovement(flat.scale(0.25));
				p.hurtMarked = true;
				return false;
			}
			return true;
		});
		return struck;
	}

	/**
	 * A travelling crescent (or any piercing blade shot): sends the launch beat {@code shape} and runs the hit volume.
	 * {@code onHit} runs once per enemy struck.
	 */
	static void blade(AbilityContext ctx, String shape, String palette, int variant, Vec3 start, Vec3 dir, double speed, double range,
			double radius, double size, float damage, BiConsumer<LivingEntity, Vec3> onHit) {
		ServerPlayer p = ctx.player();
		SwordKit.fx(p, shape, palette, variant, start, dir, speed, range, size);
		SwordShot.of(p, start, dir, speed, range, radius)
				.onHit((t, at) -> {
					if (SwordKit.hitFresh(ctx, t, damage)) {
						onHit.accept(t, at);
					}
				})
				.onEnd((at, wall, t) -> end(p, palette, at, size * 0.5))
				.fire();
	}

	static Vec3 hand(ServerPlayer p) {
		Vec3 look = p.getLookAngle();
		Vec3 side = SwordKit.right(SwordKit.flat(look)).scale(0.35);
		return p.getEyePosition().subtract(0, 0.35, 0).add(look.scale(0.8)).add(side);
	}
}
