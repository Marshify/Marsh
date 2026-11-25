package com.grandline.combat.ability;

import com.grandline.combat.fx.FxEvents;
import com.grandline.technique.CombatContext;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;

/**
 * Server-authoritative ground eruptions (reusable: spikes, ice pillars, sand columns, magma vents). Each eruption has
 * its own validated ground position, height (capped by the real overhead clearance), base radius, emergence delay,
 * active damage window and lifetime; its collision is a cone from the ground to the tip, and it hits each entity at
 * most once. Separate eruptions are separate hits (a large enemy crossing two spikes is hit by both: that target's
 * invulnerability window is cleared for that hit only). Hits on the inner core of the cone break Instinct, hits on its
 * flank can be dodged. Nothing is placed in the world: the visible spike is the client's "beat" geometry.
 */
public final class GroundSpikes {
	/** One eruption. {@code knockUp} is the upward launch at the core; {@code beat} the client effect name. */
	public record Spike(Vec3 base, double height, double radius, int delay, int active, int life, float damage, float coreDamage, double knockUp,
			String beat, int variant) {
	}

	@FunctionalInterface
	public interface OnHit {
		void hit(LivingEntity target, boolean core);
	}

	private GroundSpikes() {
	}

	/**
	 * The ground under {@code p} searched from {@code up} above to {@code down} below, or null (no support, or the
	 * point is inside a solid block).
	 */
	public static Vec3 ground(Entity by, Vec3 p, double up, double down) {
		Vec3 from = p.add(0, up, 0);
		BlockHitResult hit = by.level().clip(new ClipContext(from, p.subtract(0, down, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, by));
		if (hit.getType() == HitResult.Type.MISS || hit.isInside() || hit.getDirection() != net.minecraft.core.Direction.UP) {
			return null;
		}
		return hit.getLocation();
	}

	/** How tall a spike at {@code base} can be before it meets a ceiling (at most {@code want}). */
	public static double clearance(Entity by, Vec3 base, double want) {
		Vec3 from = base.add(0, 0.05, 0);
		BlockHitResult hit = by.level().clip(new ClipContext(from, from.add(0, want, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, by));
		return hit.getType() == HitResult.Type.MISS ? want : Math.max(0, hit.getLocation().y - base.y - 0.05);
	}

	/** Schedules an eruption (height already capped by the caller or here). Returns the effective height (0 = none). */
	public static double erupt(AbilityContext ctx, Spike s, OnHit onHit) {
		ServerPlayer player = ctx.player();
		ServerLevel level = player.serverLevel();
		double h = clearance(player, s.base(), s.height());
		if (h < 0.6) {
			return 0;
		}
		Set<Integer> struck = new HashSet<>();
		TimedEffects.start(age -> {
			if (!player.isAlive() || player.level() != level) {
				return false;
			}
			int t = age - 1 - s.delay();
			if (t == 0) {
				FxEvents.send(player, s.beat(), s.base(), new Vec3(0, 1, 0), h, s.life(), s.radius() + s.variant() * 100);
			}
			if (t >= 0 && t < s.active()) {
				double grow = Math.min(1, (t + 1) / 2.0); // the spike is fully out after two ticks
				double top = s.base().y + h * grow;
				AABB box = new AABB(s.base().x - s.radius(), s.base().y - 0.2, s.base().z - s.radius(), s.base().x + s.radius(), top, s.base().z + s.radius())
						.inflate(1);
				for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, e -> e != player && e.isAlive() && !e.isSpectator()
						&& !(e instanceof Player p && p.isCreative()) && !struck.contains(e.getId()))) {
					AABB b = e.getBoundingBox();
					double low = Math.max(s.base().y, b.minY);
					if (low > top || b.maxY < s.base().y) {
						continue;
					}
					double coneR = s.radius() * (1 - (low - s.base().y) / h);
					double dx = Math.max(Math.abs(e.getX() - s.base().x) - b.getXsize() / 2, 0), dz = Math.max(Math.abs(e.getZ() - s.base().z) - b.getZsize() / 2, 0);
					double d = Math.sqrt(dx * dx + dz * dz);
					if (d > coneR) {
						continue;
					}
					struck.add(e.getId());
					boolean core = Math.hypot(e.getX() - s.base().x, e.getZ() - s.base().z) <= s.radius() * 0.5 + b.getXsize() / 2;
					e.invulnerableTime = 0; // a separate eruption is a separate hit (this target, this hit only)
					float dmg = core ? s.coreDamage() : s.damage();
					boolean landed = core ? CombatContext.breaking(() -> ctx.damage(e, dmg)) : ctx.damage(e, dmg);
					if (landed) {
						AbilityHelpers.pushResisted(e, new Vec3((e.getX() - s.base().x) * 0.08, s.knockUp() * (core ? 1 : 0.6), (e.getZ() - s.base().z) * 0.08));
						if (onHit != null) {
							onHit.hit(e, core);
						}
					}
				}
			}
			return t < s.active();
		});
		return h;
	}
}
