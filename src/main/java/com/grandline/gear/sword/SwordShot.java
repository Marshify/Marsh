package com.grandline.gear.sword;

import com.grandline.combat.ability.TimedEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A sword move's travelling hit volume (a crescent of air, a vortex, a flame serpent...). Each tick it moves
 * {@code speed} blocks, sweeps that stretch with its {@code radius} and reports every living thing it touches for the
 * first time. A piercing shot keeps going through enemies; any shot ends on a wall or at its range. The client draws
 * the same shot from the launch beat (same start, direction, speed and range) and ends it on the impact beat, so the
 * visual and the hit volume travel together.
 */
public final class SwordShot {
	/** What happens when the shot first touches an enemy. */
	@FunctionalInterface
	public interface OnHit {
		void hit(LivingEntity target, Vec3 shotPosition);
	}

	/** Called every tick while flying (to carry enemies along, say). */
	@FunctionalInterface
	public interface OnTick {
		void tick(Vec3 position, int age, List<LivingEntity> carried);
	}

	/** Called once where the shot ends: {@code wall} is true if it ran into terrain, {@code target} if it stopped on one. */
	@FunctionalInterface
	public interface OnEnd {
		void end(Vec3 position, boolean wall, LivingEntity target);
	}

	private final ServerPlayer owner;
	private final Vec3 start;
	private final Vec3 dir;
	private final double speed;
	private final double range;
	private final double radius;
	private boolean pierce = true;
	private boolean carry = false;
	private OnHit onHit = (t, p) -> { };
	private OnTick onTick = (p, a, c) -> { };
	private OnEnd onEnd = (p, w, t) -> { };

	private SwordShot(ServerPlayer owner, Vec3 start, Vec3 dir, double speed, double range, double radius) {
		this.owner = owner;
		this.start = start;
		this.dir = dir.normalize();
		this.speed = speed;
		this.range = range;
		this.radius = radius;
	}

	public static SwordShot of(ServerPlayer owner, Vec3 start, Vec3 dir, double speed, double range, double radius) {
		return new SwordShot(owner, start, dir, speed, range, radius);
	}

	/** Stops on the first enemy instead of passing through. */
	public SwordShot single() {
		this.pierce = false;
		return this;
	}

	/** Enemies it touches travel with it until it ends. */
	public SwordShot carrying() {
		this.carry = true;
		return this;
	}

	public SwordShot onHit(OnHit onHit) {
		this.onHit = onHit;
		return this;
	}

	public SwordShot onTick(OnTick onTick) {
		this.onTick = onTick;
		return this;
	}

	public SwordShot onEnd(OnEnd onEnd) {
		this.onEnd = onEnd;
		return this;
	}

	public void fire() {
		Vec3 velocity = dir.scale(speed);
		Vec3[] pos = {start};
		double[] travelled = {0};
		Set<Integer> struck = new HashSet<>();
		List<LivingEntity> carried = new ArrayList<>();
		TimedEffects.start(age -> {
			if (owner.isRemoved()) {
				return false;
			}
			ServerLevel level = owner.serverLevel();
			Vec3 from = pos[0];
			double step = Math.min(speed, range - travelled[0]);
			Vec3 to = from.add(dir.scale(step));
			BlockHitResult block = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, owner));
			boolean wall = block.getType() != HitResult.Type.MISS;
			Vec3 limit = wall ? block.getLocation() : to;

			for (LivingEntity e : SwordKit.swept(owner, from, limit, radius)) {
				if (!struck.add(e.getId())) {
					continue;
				}
				onHit.hit(e, limit);
				if (carry && e.isAlive()) {
					carried.add(e);
				}
				if (!pierce) {
					finish(limit, false, e, carried);
					return false;
				}
			}
			carried.removeIf(e -> !e.isAlive() || e.isRemoved());
			if (carry) {
				Vec3 ahead = limit.add(dir.scale(0.6)).subtract(0, 0.6, 0);
				for (LivingEntity e : carried) {
					SwordKit.carry(e, ahead);
				}
			}
			onTick.tick(limit, age, carried);
			travelled[0] += limit.distanceTo(from);
			if (wall || travelled[0] >= range - 1e-3) {
				finish(limit, wall, null, carried);
				return false;
			}
			pos[0] = to;
			return true;
		});
		velocity.length(); // (velocity kept for clarity: the client moves the visual by the same speed)
	}

	private void finish(Vec3 at, boolean wall, LivingEntity target, List<LivingEntity> carried) {
		onEnd.end(at, wall, target);
	}
}
