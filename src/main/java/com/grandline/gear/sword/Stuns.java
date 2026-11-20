package com.grandline.gear.sword;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Short immobilisations from sword moves. While stunned an entity cannot walk, jump or steer (its horizontal motion is
 * held at zero every tick and mobs stop pathing); it still falls, and knockback applied by the move that stunned it
 * plays out over the first few ticks. Nothing is saved: a stun is at most a couple of seconds.
 */
public final class Stuns {
	private record Stun(LivingEntity entity, long until, long knockUntil) {
	}

	private static final Map<Integer, Stun> STUNS = new HashMap<>();

	private Stuns() {
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(Stuns::tick);
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> STUNS.clear());
	}

	/** Stuns {@code target} for {@code ticks}; {@code knockTicks} lets a knockback from the same hit carry first. */
	public static void apply(LivingEntity target, int ticks, int knockTicks) {
		if (!(target.level() instanceof ServerLevel level) || !target.isAlive() || com.grandline.technique.Dodges.justEvaded(target)) {
			return;
		}
		long now = level.getGameTime();
		Stun old = STUNS.get(target.getId());
		long until = Math.max(now + ticks, old == null ? 0 : old.until());
		STUNS.put(target.getId(), new Stun(target, until, now + knockTicks));
		target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 9, false, false));
		target.addEffect(new MobEffectInstance(MobEffects.JUMP, ticks, 128, false, false)); // amplifier 128 = no jumping
		if (target instanceof Mob mob) {
			mob.getNavigation().stop();
		}
	}

	public static void apply(LivingEntity target, int ticks) {
		apply(target, ticks, 0);
	}

	public static boolean stunned(LivingEntity target) {
		Stun s = STUNS.get(target.getId());
		return s != null && s.entity().level().getGameTime() < s.until();
	}

	private static void tick(MinecraftServer server) {
		for (Iterator<Stun> it = STUNS.values().iterator(); it.hasNext(); ) {
			Stun s = it.next();
			LivingEntity e = s.entity();
			long now = e.level().getGameTime();
			if (!e.isAlive() || e.isRemoved() || now >= s.until()) {
				it.remove();
				continue;
			}
			if (now < s.knockUntil()) {
				continue;
			}
			Vec3 v = e.getDeltaMovement();
			e.setDeltaMovement(0, Math.min(v.y, 0), 0);
			e.hurtMarked = true;
			if (e instanceof Mob mob) {
				mob.getNavigation().stop();
				mob.setJumping(false);
			}
		}
	}
}
