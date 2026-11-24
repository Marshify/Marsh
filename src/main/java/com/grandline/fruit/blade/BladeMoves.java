package com.grandline.fruit.blade;

import com.grandline.GrandLineMod;
import com.grandline.combat.DefensivePassives;
import com.grandline.combat.ability.AbilityContext;
import com.grandline.combat.ability.AbilityDef;
import com.grandline.combat.ability.AbilityHelpers;
import com.grandline.combat.ability.FlightService;
import com.grandline.combat.ability.FruitStrikes;
import com.grandline.combat.ability.Projectiles;
import com.grandline.combat.ability.TimedEffects;
import com.grandline.combat.fx.FxEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Blade Fruit: the body turns to steel. Forearm blades for a three-strike M1 combo, a cutting dash, a spinning blade
 * flurry, a volley of thrown blades and rotor-blade flight, plus the Blade Body passive: ordinary (non-Aura) sword
 * attacks glance off. Server timelines decide every hit and movement step; visual beats ({@link FxEvents}) drive the
 * client's BladeFx (attachments, slash meshes, afterimages, sparks).
 *
 * Damage is a share of Rocket's Missile Fist ({@link #REF}, 8 before mastery and Fruit-stat scaling): M1 combo 55%,
 * Razor Rush 90%, Blade Dance 115%, Blade Barrage 130%.
 */
public final class BladeMoves {
	public static final float REF = com.grandline.fruit.rocket.RocketMoves.BASE_DAMAGE;
	public static final String FRUIT_ID = "grandline:blade";

	// --- M1: Blade Strikes (1.3 + 1.3 + 1.8 = 4.4 = 55%) ---
	public static final int STRIKE_INTERVAL = 10; // = vanilla invulnerability, so every strike lands
	public static final int STRIKE_RESET = 30;
	public static final int STRIKE_HIT_DELAY = 2; // the hit lands as the slash passes, not on the click
	public static final double STRIKE_RANGE = 4;
	static final float[] STRIKE_DAMAGE = {REF * 0.1625f, REF * 0.1625f, REF * 0.225f};
	static final double[] STRIKE_ARC = {55, 50, 75};

	// --- Z: Razor Rush (path 50% + crossing finisher 40% = 90%) ---
	public static final int RUSH_WINDUP = 3;
	public static final double RUSH_RANGE = 15;
	public static final double RUSH_SPEED = 3.0;
	static final float RUSH_PATH = REF * 0.50f;
	static final float RUSH_FINISH = REF * 0.40f;

	// --- X: Blade Dance (3 × 20% + 55% finisher = 115%) ---
	public static final int DANCE_TICKS = 36;
	public static final int[] DANCE_HITS = {2, 12, 22};
	public static final int DANCE_FINISH = 32;
	public static final double DANCE_RADIUS = 5;
	static final float DANCE_HIT = REF * 0.20f;
	static final float DANCE_FINAL = REF * 0.55f;

	// --- C: Blade Barrage (6 × 15% + 40% = 130%) ---
	public static final int BARRAGE_SMALL = 6;
	public static final int BARRAGE_FIRST = 4, BARRAGE_INTERVAL = 3, BARRAGE_FINAL = 27;
	public static final double BARRAGE_RANGE = 30;
	static final float BARRAGE_DAMAGE = REF * 0.15f;
	static final float BARRAGE_FINAL_DAMAGE = REF * 0.40f;

	// --- F: Blade Flight ---
	static final int FLIGHT_MAX_TICKS = 600;
	/** 0.7% of max energy per tick (14%/s) against 8%/s regeneration: about 10 s from a full bar after take-off. */
	public static final double FLIGHT_DRAIN_PER_TICK = 0.007;
	/** About 8 blocks per second: between Spin's Helicopter Flight (0.032) and Rocket's Blast Off (0.09). */
	public static final float FLIGHT_SPEED = 0.037f;

	public static final AbilityDef STRIKES = new AbilityDef("blade_strikes", 1, 0, 0, ctx -> 0);

	private BladeMoves() {
	}

	public static List<AbilityDef> moves() {
		FruitStrikes.combo(GrandLineMod.id("blade"), new FruitStrikes.Combo(STRIKES, 3, STRIKE_INTERVAL, STRIKE_RESET, BladeMoves::strike));
		DefensivePassives.passive(FRUIT_ID, new DefensivePassives.Passive(BladeMoves::bladeBodyBlocks, BladeMoves::deflect));
		return List.of(
				new AbilityDef("blade_razor_rush", 1, 120, 0.15, BladeMoves::razorRush),
				new AbilityDef("blade_dance", 20, 200, 0.25, BladeMoves::bladeDance),
				new AbilityDef("blade_barrage", 50, 260, 0.35, BladeMoves::bladeBarrage),
				AbilityDef.empty(),
				new AbilityDef("blade_flight", 80, 150, 0.40, AbilityDef.Activation.TOGGLE, BladeMoves::bladeFlight));
	}

	private static boolean gone(ServerPlayer player, Level level) {
		return !player.isAlive() || player.isRemoved() || player.level() != level;
	}

	// ============================================================================================== Passive: Blade Body

	/** Ordinary sword attacks (no Aura) glance off; everything else lands normally. */
	static boolean bladeBodyBlocks(DamageSource source) {
		return DefensivePassives.swordAttack(source) && !DefensivePassives.auraEnhanced(source);
	}

	/** The glance: sparks at the real point of contact, on the side facing the attacker. */
	static void deflect(ServerPlayer player, DamageSource source) {
		Vec3 centre = player.getBoundingBox().getCenter();
		Vec3 from = source.getEntity() != null ? source.getEntity().getEyePosition() : centre.add(player.getLookAngle());
		Vec3 dir = centre.subtract(from);
		dir = dir.lengthSqr() < 1e-6 ? new Vec3(0, 0, 1) : dir.normalize();
		Vec3 contact = centre.subtract(dir.multiply(1, 0, 1).scale(player.getBbWidth() * 0.55))
				.add(0, Math.max(-0.6, Math.min(0.5, from.y - centre.y)) * 0.6, 0);
		FxEvents.send(player, "blade_deflect", contact, dir, 0, 0, player.getRandom().nextInt(4));
	}

	// ============================================================================================== M1: Blade Strikes

	static void strike(AbilityContext ctx, int step, boolean last) {
		ServerPlayer player = ctx.player();
		Level level = player.level();
		FxEvents.send(player, "blade_strike", player.position(), player.getLookAngle(), 0, 0, step);
		TimedEffects.later(STRIKE_HIT_DELAY, () -> {
			if (gone(player, level)) {
				return;
			}
			Vec3 eye = player.getEyePosition();
			for (LivingEntity target : AbilityHelpers.inArc(player, STRIKE_RANGE, STRIKE_ARC[step])) {
				if (ctx.damage(target, STRIKE_DAMAGE[step])) {
					if (last) {
						Vec3 out = target.position().subtract(player.position()).multiply(1, 0, 1);
						out = out.lengthSqr() < 1e-4 ? player.getLookAngle().multiply(1, 0, 1) : out;
						AbilityHelpers.pushResisted(target, out.normalize().scale(0.55).add(0, 0.25, 0));
					} else {
						AbilityHelpers.stagger(target, 5, 0);
					}
					FxEvents.send(player, "blade_strike_hit", target.getBoundingBox().getCenter(), target.getBoundingBox().getCenter().subtract(eye).normalize(), 0, 0, step);
				}
			}
		});
	}

	// ============================================================================================== Z: Razor Rush

	static int razorRush(AbilityContext ctx) {
		ServerPlayer player = ctx.player();
		Level level = player.level();
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getLookAngle();
		// Toward the crosshair, kept between a 35° dive and climb so it always travels.
		double flat = Math.hypot(look.x, look.z);
		double pitch = Math.max(-Math.toRadians(35), Math.min(Math.toRadians(35), Math.atan2(look.y, flat)));
		Vec3 h = flat < 1e-4 ? new Vec3(0, 0, 1) : new Vec3(look.x / flat, 0, look.z / flat);
		Vec3 dir = h.scale(Math.cos(pitch)).add(0, Math.sin(pitch), 0).normalize();
		double reach = Math.min(RUSH_RANGE, Projectiles.crosshairTarget(player, RUSH_RANGE).distanceTo(eye));
		Vec3[] body = {player.position()};
		double[] travelled = {0};
		Set<Integer> struck = new HashSet<>();
		FxEvents.send(player, "blade_rush_windup", player.position(), dir, 0, 0, 0);
		TimedEffects.start(age -> {
			if (gone(player, level)) {
				FxEvents.send(player, "blade_rush_end", player.position(), dir, 0, 0, 0);
				return false;
			}
			player.fallDistance = 0;
			if (age < RUSH_WINDUP) {
				player.setDeltaMovement(player.getDeltaMovement().multiply(0.2, 1, 0.2));
				player.hurtMarked = true;
				return true;
			}
			if (age == RUSH_WINDUP) {
				body[0] = player.position();
				FxEvents.send(player, "blade_rush", player.position(), dir, RUSH_SPEED, reach, 0);
			}
			// Swept movement in half-block steps: the body's box must stay free of blocks (no clipping into walls).
			double step = Math.min(RUSH_SPEED, Math.max(0, reach - travelled[0]));
			boolean blocked = false;
			Vec3 start = body[0];
			for (double s = 0.5; s <= step + 1e-6; s += 0.5) {
				Vec3 next = start.add(dir.scale(s));
				AABB box = player.getBoundingBox().move(next.subtract(player.position()));
				if (!level.noCollision(player, box)) {
					blocked = true;
					break;
				}
				body[0] = next;
				for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box.inflate(0.6),
						e -> e != player && e.isAlive() && !e.isSpectator() && !struck.contains(e.getId()))) {
					struck.add(target.getId());
					if (ctx.damage(target, RUSH_PATH)) {
						AbilityHelpers.stagger(target, 6, 1);
						FxEvents.send(player, "blade_rush_hit", target.getBoundingBox().getCenter(), dir, 0, 0, 0);
					}
				}
			}
			travelled[0] += body[0].distanceTo(start);
			Vec3 motion = body[0].subtract(start);
			player.setDeltaMovement(motion);
			player.hurtMarked = true;
			if (blocked || travelled[0] >= reach - 0.01 || age > RUSH_WINDUP + 8) {
				finishRush(ctx, body[0], dir);
				return false;
			}
			return true;
		});
		return 0;
	}

	/** The crossing finisher at the end of the dash: one more cut to everything just ahead (and those cut on the way). */
	static void finishRush(AbilityContext ctx, Vec3 at, Vec3 dir) {
		ServerPlayer player = ctx.player();
		player.setDeltaMovement(Vec3.ZERO);
		player.hurtMarked = true;
		if (player.position().distanceTo(at) > 1.5) {
			player.teleportTo(at.x, at.y, at.z); // server authority over the dash path
		}
		player.fallDistance = 0;
		player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 10, 0, false, false)); // no fall from a downward dash
		FxEvents.send(player, "blade_rush_end", at, dir, 0, 0, 1);
		Vec3 centre = at.add(0, 1, 0).add(dir.multiply(1, 0, 1).scale(1.2));
		for (LivingEntity target : AbilityHelpers.inRadius(player, centre, 3.2)) {
			target.invulnerableTime = 0; // the finisher is its own cut, a few ticks after the pass-through
			if (ctx.damage(target, RUSH_FINISH)) {
				Vec3 out = target.position().subtract(at).multiply(1, 0, 1);
				out = out.lengthSqr() < 1e-4 ? dir.multiply(1, 0, 1) : out;
				AbilityHelpers.pushResisted(target, out.normalize().scale(0.7).add(0, 0.3, 0));
				FxEvents.send(player, "blade_rush_hit", target.getBoundingBox().getCenter(), dir, 0, 0, 1);
			}
		}
	}

	// ============================================================================================== X: Blade Dance

	static int bladeDance(AbilityContext ctx) {
		ServerPlayer player = ctx.player();
		Level level = player.level();
		FxEvents.send(player, "blade_dance", player.position(), Vec3.ZERO, 0, DANCE_TICKS, DANCE_RADIUS);
		TimedEffects.start(age -> {
			if (gone(player, level)) {
				FxEvents.send(player, "blade_dance_end", player.position());
				return false;
			}
			player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 3, 1, false, false)); // limited movement
			for (int i = 0; i < DANCE_HITS.length; i++) {
				if (age == DANCE_HITS[i]) {
					for (LivingEntity target : around(player)) {
						if (ctx.damage(target, DANCE_HIT)) {
							AbilityHelpers.stagger(target, 6, 0);
							FxEvents.send(player, "blade_dance_hit", target.getBoundingBox().getCenter(), Vec3.ZERO, 0, 0, i);
						}
					}
				}
			}
			if (age == DANCE_FINISH) {
				FxEvents.send(player, "blade_dance_final", player.position(), Vec3.ZERO, 0, 0, DANCE_RADIUS);
				for (LivingEntity target : around(player)) {
					if (ctx.damage(target, DANCE_FINAL)) {
						Vec3 out = target.position().subtract(player.position()).multiply(1, 0, 1);
						out = out.lengthSqr() < 1e-4 ? player.getLookAngle().multiply(1, 0, 1) : out;
						AbilityHelpers.pushResisted(target, out.normalize().scale(0.8).add(0, 0.25, 0));
						FxEvents.send(player, "blade_dance_hit", target.getBoundingBox().getCenter(), Vec3.ZERO, 0, 0, 3);
					}
				}
			}
			return age < DANCE_TICKS;
		});
		return 0;
	}

	private static List<LivingEntity> around(ServerPlayer player) {
		Vec3 c = player.position();
		return AbilityHelpers.inRadius(player, c.add(0, 1, 0), DANCE_RADIUS + 1).stream()
				.filter(e -> Math.hypot(e.getX() - c.x, e.getZ() - c.z) <= DANCE_RADIUS + e.getBbWidth() / 2 && Math.abs(e.getY() - c.y) < 3)
				.filter(player::hasLineOfSight)
				.toList();
	}

	// ============================================================================================== C: Blade Barrage

	static int bladeBarrage(AbilityContext ctx) {
		ServerPlayer player = ctx.player();
		Level level = player.level();
		FxEvents.send(player, "blade_barrage_windup", player.position());
		TimedEffects.later(BARRAGE_FINAL - 5, () -> {
			if (!gone(player, level)) {
				FxEvents.send(player, "blade_barrage_finale", player.position());
			}
		});
		for (int i = 0; i <= BARRAGE_SMALL; i++) {
			boolean last = i == BARRAGE_SMALL;
			int n = i;
			TimedEffects.later(last ? BARRAGE_FINAL : BARRAGE_FIRST + i * BARRAGE_INTERVAL, () -> {
				if (gone(player, level)) {
					return;
				}
				Vec3 start = hand(player, last || n % 2 == 0, last);
				Vec3 dir = AbilityHelpers.assistedAim(player, BARRAGE_RANGE, 5).subtract(start).normalize();
				double speed = last ? 2.0 : 2.4;
				FxEvents.send(player, "blade_proj", start, dir, speed, BARRAGE_RANGE, last ? 6 : n);
				Projectiles.fire(player, start, dir, speed, BARRAGE_RANGE, last ? 0.8 : 0.4, (lvl, pos, a) -> {
				}, impact -> {
					boolean hit = impact.entity().isPresent();
					boolean terrain = !hit && impact.position().distanceTo(start) < BARRAGE_RANGE - 0.5;
					FxEvents.send(player, "blade_proj_hit", impact.position(), dir, hit ? 2 : terrain ? 1 : 0, 0, last ? 6 : n);
					impact.entity().ifPresent(target -> {
						target.invulnerableTime = 0; // each blade hits once; this clears the window for this hit only
						if (ctx.damage(target, last ? BARRAGE_FINAL_DAMAGE : BARRAGE_DAMAGE)) {
							Vec3 flat = dir.multiply(1, 0, 1).normalize();
							if (last) {
								AbilityHelpers.pushResisted(target, flat.scale(0.8).add(0, 0.25, 0));
							} else {
								AbilityHelpers.stagger(target, 5, 1);
							}
						}
					});
				});
			});
		}
		return 0;
	}

	/** The throwing hand (right or left forearm, a little ahead); the finisher leaves from between both. */
	static Vec3 hand(ServerPlayer player, boolean right, boolean both) {
		Vec3 look = player.getLookAngle();
		Vec3 side = look.cross(new Vec3(0, 1, 0)).normalize().scale(both ? 0 : right ? 0.38 : -0.38);
		return player.getEyePosition().subtract(0, 0.35, 0).add(look.scale(0.75)).add(side);
	}

	// ============================================================================================== F: Blade Flight

	static int bladeFlight(AbilityContext ctx) {
		ServerPlayer player = ctx.player();
		if (AbilityHelpers.inWater(player)) {
			return 0;
		}
		FxEvents.send(player, "blade_flight_start", player.position());
		FlightService.start(player, ctx.power(), ctx.ability(), FLIGHT_MAX_TICKS, FLIGHT_DRAIN_PER_TICK, FLIGHT_SPEED, (flier, ticks) -> {
			if (AbilityHelpers.inWater(flier)) {
				FlightService.stop(flier, true);
			}
		});
		return 0;
	}
}
