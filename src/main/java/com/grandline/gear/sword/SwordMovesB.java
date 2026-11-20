package com.grandline.gear.sword;

import com.grandline.GrandLineMod;
import com.grandline.combat.ability.AbilityContext;
import com.grandline.combat.ability.AbilityDef;
import com.grandline.combat.ability.ChargeService;
import com.grandline.combat.ability.Projectiles;
import com.grandline.combat.ability.TimedEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.grandline.gear.sword.SwordKit.layer;
import static com.grandline.gear.sword.SwordKit.later;

/** Twinfang Glaive, Wraith Cane, Bisento, Trident, Stormstaff, Saber and Eclipse Blade (see SwordMovesA for the pattern). */
final class SwordMovesB extends SwordMoveBase {
	private SwordMovesB() {
	}

	// =================================================================================================== Twinfang Glaive

	static List<AbilityDef> twinfangGlaive() {
		return List.of(def("twinfang_glaive_revolving_edge", 20, 7.5, 15, SwordMovesB::revolvingEdge),
				def("twinfang_glaive_triple_vortex", 100, 10, 25, SwordMovesB::tripleVortex));
	}

	/** Z: a spinning dash, both blade ends drawing circles round the caster, shimmering motes along the way. */
	static int revolvingEdge(AbilityContext ctx) {
		ServerPlayer p = ctx.player();
		pose(p, POSE_SPIN, 0);
		SwordKit.sound(p, p.position(), layer(SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 1.2f), later(SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 1.35f, 2),
				later(SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 1.5f, 4), layer(SoundEvents.AMETHYST_BLOCK_CHIME, 0.8f, 1.6f));
		dash(ctx, "twin", 3, p.getLookAngle(), 7, 6, 2.0, 13f, (t, at) -> {
			SwordKit.fx(p, "cut", "twin", 4, t.position().add(0, t.getBbHeight() * 0.6, 0), SwordKit.forward(p), 1.2);
			SwordKit.push(t, at, 0.6, 0.3);
		});
		return 0;
	}

	/** X: three wind vortices fired in a fan toward the crosshair, each travelling and hitting on its own. */
	static int tripleVortex(AbilityContext ctx) {
		ServerPlayer p = ctx.player();
		pose(p, POSE_SLASH_R, 4);
		SwordKit.sound(p, p.position(), layer(SoundEvents.ELYTRA_FLYING, 0.8f, 1.1f), later(SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 0.9f, 4),
				later(SoundEvents.ENDER_DRAGON_FLAP, 0.8f, 1.5f, 4));
		TimedEffects.later(4, () -> {
			if (SwordKit.gone(p)) {
				return;
			}
			Vec3 start = SwordKit.chest(p).add(SwordKit.forward(p).scale(1.0));
			Vec3 dir = aim(p, start, 18);
			for (int i = -1; i <= 1; i++) {
				Vec3 d = yaw(dir, i * 13);
				SwordKit.fx(p, "spinner", "twin", i + 1, start, d, 0.85, 18, 1.0);
				SwordShot.of(p, start, d, 0.85, 18, 1.0)
						.onHit((t, at) -> {
							SwordKit.hitFresh(ctx, t, 7f);
							SwordKit.push(t, at.subtract(d), 0.5, 0.35);
						})
						.onEnd((at, wall, t) -> end(p, "twin", at, 1.0)).fire();
			}
		});
		return 0;
	}

	// =================================================================================================== Wraith Cane

	static List<AbilityDef> wraithCane() {
		return List.of(def("wraith_cane_spirit_lance", 40, 7.5, 25, SwordMovesB::spiritLance),
				def("wraith_cane_phantom_sequence", 120, 13.5, 30, SwordMovesB::phantomSequence));
	}

	static final double LANCE_RANGE = 24;

	/** Z: a narrow white beam; a pale-blue spirit explosion where it ends pins enemies for about 1.5 s. */
	static int spiritLance(AbilityContext ctx) {
		ServerPlayer p = ctx.player();
		pose(p, POSE_THRUST, 3);
		SwordKit.sound(p, p.position(), layer(SoundEvents.SOUL_ESCAPE, 1.0f, 1.4f), later(SoundEvents.BEACON_POWER_SELECT, 0.8f, 1.8f, 2));
		TimedEffects.later(3, () -> {
			if (SwordKit.gone(p)) {
				return;
			}
			Vec3 start = hand(p);
			Vec3 dir = aim(p, start, LANCE_RANGE);
			ServerLevel level = p.serverLevel();
			BlockHitResult wall = level.clip(new ClipContext(start, start.add(dir.scale(LANCE_RANGE)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
			Vec3 endPoint = wall.getType() == HitResult.Type.MISS ? start.add(dir.scale(LANCE_RANGE)) : wall.getLocation();
			// The beam stops on the first enemy in its path.
			LivingEntity first = null;
			double best = Double.MAX_VALUE;
			for (LivingEntity e : SwordKit.swept(p, start, endPoint, 0.35)) {
				double d = e.position().distanceToSqr(start);
				if (d < best) {
					best = d;
					first = e;
				}
			}
			if (first != null) {
				endPoint = start.add(dir.scale(Math.max(0.5, Math.sqrt(best) - 0.2))).add(0, 0, 0);
				endPoint = new Vec3(endPoint.x, first.getY() + first.getBbHeight() * 0.55, endPoint.z);
			}
			SwordKit.fx(p, "beam", "wraith", 0, start, endPoint.subtract(start), 0, start.distanceTo(endPoint), 0.22);
			SwordKit.fx(p, "spirit_burst", "wraith", 0, endPoint, Vec3.ZERO, 3.0);
			SwordKit.sound(p, endPoint, layer(SoundEvents.SOUL_ESCAPE, 1.0f, 0.7f), layer(SoundEvents.GLASS_BREAK, 0.6f, 1.6f),
					layer(SoundEvents.BEACON_DEACTIVATE, 0.8f, 1.9f));
			for (LivingEntity e : SwordKit.sphere(p, endPoint, 3.0)) {
				if (SwordKit.hitFresh(ctx, e, 13f)) {
					Stuns.apply(e, 30);
				}
			}
		});
		return 0;
	}

	static final int PHANTOM_ARCS = 4;

	/** X: overlapping spirit arcs in rapid succession: repeated hits that pin enemies for about 2 s. */
	static int phantomSequence(AbilityContext ctx) {
		ServerPlayer p = ctx.player();
		SwordKit.sound(p, p.position(), layer(SoundEvents.SOUL_ESCAPE, 1.0f, 1.0f));
		TimedEffects.start(age -> {
			if (SwordKit.gone(p)) {
				return false;
			}
			if (age % 4 != 0) {
				return true;
			}
			int n = age / 4;
			if (n >= PHANTOM_ARCS) {
				return false;
			}
			Vec3 f = SwordKit.forward(p);
			pose(p, n % 2 == 0 ? POSE_SLASH_R : POSE_SLASH_L, 0);
			SwordKit.fx(p, "spirit_arc", "wraith", n, SwordKit.chest(p).add(f.scale(1.2)), f, 5.5);
			SwordKit.sound(p, p.position(), layer(SoundEvents.PLAYER_ATTACK_SWEEP, 0.9f, 1.4f + n * 0.1f),
					layer(SoundEvents.SOUL_ESCAPE, 0.6f, 1.2f + n * 0.15f));
			for (LivingEntity e : SwordKit.arc(p, p.position(), f, 6.0, 80, 1.5, 3)) {
				if (SwordKit.hitFresh(ctx, e, 5f)) {
					Stuns.apply(e, 40);
				}
			}
			return true;
		});
		return 0;
	}

	// =================================================================================================== Bisento

	static List<AbilityDef> bisento() {
		return List.of(def("bisento_atmospheric_cleave", 50, 9, 20, SwordMovesB::atmosphericCleave),
				def("bisento_seismic_core", 150, 13.5, 30, SwordMovesB::seismicCore));
	}

	static final int CLEAVE_WINDUP = 7;

	/** Z: overhead and down: a large pressure wave travels forward (three of them once upgraded). */
	static int atmosphericCleave(AbilityContext ctx) {
		ServerPlayer p = ctx.player();
		boolean upgraded = SwordUpgrades.upgraded(p, ctx.power());
		pose(p, POSE_OVERHEAD, CLEAVE_WINDUP);
		SwordKit.sound(p, p.position(), layer(SoundEvents.PLAYER_ATTACK_SWEEP, 0.6f, 0.5f));
		TimedEffects.later(CLEAVE_WINDUP, () -> {
			if (SwordKit.gone(p)) {
				return;
			}
			SwordKit.sound(p, p.position(), layer(SoundEvents.WARDEN_SONIC_BOOM, 0.5f, 1.6f), layer(SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 0.6f),
					layer(SoundEvents.ENDER_DRAGON_FLAP, 1.0f, 0.7f));
			Vec3 start = SwordKit.chest(p).add(SwordKit.forward(p).scale(1.2)).subtract(0, 0.3, 0);
			Vec3 dir = SwordKit.flat(aim(p, start, 18));
			for (int i = upgraded ? -1 : 0; i <= (upgraded ? 1 : 0); i++) {
				Vec3 d = yaw(dir, i * 12);
				blade(ctx, "pressure", "bisento", i + 1, start, d, 1.1, 18, 1.8, 3.6, 16f,
						(t, at) -> SwordKit.push(t, at.subtract(d), 1.2, 0.45));
			}
		});
		return 0;
	}

	static final int CORE_WINDUP = 6;
	static final double CORE_RADIUS = 7;

	/**
	 * X: the butt of the weapon driven into the ground: a translucent white shockwave sphere, fractures and energy rings
	 * (upgraded: an energy sphere forms at the tip, flies to the target, pulls enemies in, swells and detonates).
	 */
	static int seismicCore(AbilityContext ctx) {
		ServerPlayer p = ctx.player();
		if (SwordUpgrades.upgraded(p, ctx.power())) {
			return seismicCoreUpgraded(ctx);
		}
		pose(p, POSE_GROUND, CORE_WINDUP);
		SwordKit.sound(p, p.position(), layer(SoundEvents.ARMOR_EQUIP_NETHERITE, 1.0f, 0.5f));
		TimedEffects.later(CORE_WINDUP, () -> {
			if (SwordKit.gone(p)) {
				return;
			}
			Vec3 c = SwordKit.ground(p.serverLevel(), p.position());
			SwordKit.fx(p, "core", "bisento", 0, c, SwordKit.forward(p), 0, CORE_RADIUS, CORE_RADIUS);
			SwordKit.sound(p, c, layer(SoundEvents.WARDEN_SONIC_BOOM, 1.0f, 0.55f), layer(SoundEvents.GENERIC_EXPLODE, 1.0f, 0.45f),
					layer(SoundEvents.ANVIL_LAND, 0.8f, 0.4f), later(SoundEvents.DEEPSLATE_BREAK, 1.0f, 0.4f, 2));
			for (LivingEntity e : SwordKit.sphere(p, c.add(0, 1, 0), CORE_RADIUS)) {
				SwordKit.hitFresh(ctx, e, 21f);
				SwordKit.push(e, c, 1.5, 0.6);
			}
		});
		return 0;
	}

	static final int ORB_FORM = 10;
	static final int ORB_PULL = 12;

	private static int seismicCoreUpgraded(AbilityContext ctx) {
		ServerPlayer p = ctx.player();
		pose(p, POSE_RAISE, ORB_FORM);
		SwordKit.fx(p, "orb_form", "bisento", 0, p.position(), Vec3.ZERO, ORB_FORM);
		SwordKit.sound(p, p.position(), layer(SoundEvents.BEACON_ACTIVATE, 1.0f, 1.4f), later(SoundEvents.WARDEN_SONIC_CHARGE, 0.6f, 1.6f, 2));
		TimedEffects.later(ORB_FORM, () -> {
			if (SwordKit.gone(p)) {
				return;
			}
			pose(p, POSE_THRUST, 0);
			Vec3 start = p.position().add(0, p.getBbHeight() + 0.9, 0).add(SwordKit.forward(p).scale(0.9));
			Vec3 dir = aim(p, start, 26);
			SwordKit.fx(p, "energy_orb", "bisento", 0, start, dir, 1.0, 26, 1.3);
			SwordKit.sound(p, p.position(), layer(SoundEvents.WARDEN_SONIC_BOOM, 0.4f, 1.9f));
			SwordShot.of(p, start, dir, 1.0, 26, 1.1).single()
					.onEnd((at, wall, t) -> {
						end(p, "bisento", at, 0.1);
						SwordKit.fx(p, "core_orb", "bisento", 0, at, dir, ORB_PULL, CORE_RADIUS, 6.0);
						SwordKit.sound(p, at, layer(SoundEvents.WARDEN_SONIC_CHARGE, 1.0f, 1.2f), layer(SoundEvents.BEACON_POWER_SELECT, 1.0f, 0.6f));
						TimedEffects.start(age -> {
							if (age < ORB_PULL) {
								if (age % 2 == 0) {
									for (LivingEntity e : SwordKit.sphere(p, at, CORE_RADIUS)) {
										SwordKit.pull(e, at.subtract(0, 0.4, 0), 0.4);
									}
								}
								return true;
							}
							SwordKit.sound(p, at, layer(SoundEvents.GENERIC_EXPLODE, 1.0f, 0.5f), layer(SoundEvents.WARDEN_SONIC_BOOM, 1.0f, 0.7f),
									later(SoundEvents.DEEPSLATE_BREAK, 1.0f, 0.5f, 2));
							for (LivingEntity e : SwordKit.sphere(p, at, 6.0)) {
								SwordKit.hitFresh(ctx, e, 24f);
								SwordKit.push(e, at, 1.6, 0.7);
							}
							return false;
						});
					}).fire();
		});
		return 0;
	}

	// =================================================================================================== Trident

	static List<AbilityDef> trident() {
		return List.of(def("trident_tidal_grapple", 50, 6, 30, SwordMovesB::tidalGrapple), def("trident_abyssal_surge", 125, 11, 25, SwordMovesB::abyssalSurge));
	}

	static final double GRAPPLE_RANGE = 22;
	static final double GRAPPLE_SPEED = 1.8;

	/**
	 * Z: the trident is thrown at the crosshair on a dark tether. An enemy it strikes is reeled in to the caster; if it
	 * sticks in terrain the caster is pulled to it. Then it returns to the hand.
	 */
	static int tidalGrapple(AbilityContext ctx) {
		ServerPlayer p = ctx.player();
		pose(p, POSE_THROW, 3);
		SwordKit.sound(p, p.position(), layer(SoundEvents.TRIDENT_THROW, 1.0f, 0.9f));
		TimedEffects.later(3, () -> {
			if (SwordKit.gone(p)) {
				return;
			}
			Vec3 start = hand(p);
			Vec3 dir = aim(p, start, GRAPPLE_RANGE);
			SwordKit.fx(p, "grapple", "tide", 0, start, dir, GRAPPLE_SPEED, GRAPPLE_RANGE, 1.0);
			SwordShot.of(p, start, dir, GRAPPLE_SPEED, GRAPPLE_RANGE, 0.6).single()
					.onHit((t, at) -> SwordKit.hitFresh(ctx, t, 13f))
					.onEnd((at, wall, t) -> {
						SwordKit.fx(p, "grapple_hit", "tide", t != null ? 1 : wall ? 2 : 0, at, dir, 1.0);
						if (t != null) {
							SwordKit.sound(p, at, layer(SoundEvents.TRIDENT_HIT, 1.0f, 0.9f), layer(SoundEvents.CHAIN_PLACE, 1.0f, 0.8f));
							reel(p, t, () -> returnTrident(p));
						} else if (wall) {
							SwordKit.sound(p, at, layer(SoundEvents.TRIDENT_HIT_GROUND, 1.0f, 0.9f), layer(SoundEvents.CHAIN_PLACE, 1.0f, 0.7f));
							zip(p, at, () -> returnTrident(p));
						} else {
							returnTrident(p);
						}
					}).fire();
		});
		return 0;
	}

	/** Pulls {@code target} in to the caster over a few ticks (the tether follows it on clients). */
	private static void reel(ServerPlayer p, LivingEntity target, Runnable done) {
		SwordKit.fx(p, "tether", "tide", target.getId(), target.position(), Vec3.ZERO, 14);
		TimedEffects.start(age -> {
			if (SwordKit.gone(p) || !target.isAlive() || age >= 14 || target.distanceTo(p) < 2.2) {
				done.run();
				return false;
			}
			SwordKit.pull(target, p.position().add(SwordKit.forward(p).scale(1.4)).add(0, 0.4, 0), 0.9);
			return true;
		});
	}

	/** Pulls the caster to {@code at} (the trident stuck in terrain). */
	private static void zip(ServerPlayer p, Vec3 at, Runnable done) {
		Vec3 stand = at.subtract(at.subtract(p.position()).normalize().scale(1.0));
		TimedEffects.start(age -> {
			if (SwordKit.gone(p) || age >= 18 || p.position().distanceTo(stand) < 1.3) {
				p.setDeltaMovement(p.getDeltaMovement().scale(0.3));
				p.hurtMarked = true;
				p.fallDistance = 0;
				done.run();
				return false;
			}
			Vec3 d = stand.subtract(p.position());
			Vec3 v = d.normalize().scale(Math.min(1.2, d.length()));
			p.setDeltaMovement(v.x, v.y + 0.08, v.z);
			p.hurtMarked = true;
			p.fallDistance = 0;
			return true;
		});
	}

	private static void returnTrident(ServerPlayer p) {
		if (!SwordKit.gone(p)) {
			SwordKit.fx(p, "grapple_return", "tide", 0, p.position(), Vec3.ZERO, 1.0);
			SwordKit.sound(p, p.position(), layer(SoundEvents.TRIDENT_RETURN, 1.0f, 1.0f));
		}
	}

	static final int SURGE_TICKS = 24;
	static final double SURGE_RADIUS = 6;

	/** X: the trident spun overhead raises a ring of water round the caster that drags enemies in, then bursts outward. */
	static int abyssalSurge(AbilityContext ctx) {
		ServerPlayer p = ctx.player();
		pose(p, POSE_SPIN, 0);
		Vec3 c0 = p.position();
		SwordKit.fx(p, "surge", "tide", 0, c0, Vec3.ZERO, SURGE_TICKS, SURGE_RADIUS, 3.0);
		SwordKit.sound(p, c0, layer(SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_INSIDE, 1.0f, 0.8f), layer(SoundEvents.TRIDENT_RIPTIDE_1, 1.0f, 0.8f),
				later(SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_INSIDE, 1.0f, 1.0f, 12));
		TimedEffects.start(age -> {
			if (SwordKit.gone(p)) {
				return false;
			}
			Vec3 c = p.position();
			if (age < SURGE_TICKS) {
				if (age % 2 == 0) {
					for (LivingEntity e : SwordKit.arc(p, c, Vec3.ZERO, SURGE_RADIUS + 1, 180, 2, 4)) {
						SwordKit.pull(e, c.add(0, 0.3, 0), 0.32);
						if (age % 6 == 0) {
							SwordKit.hitFresh(ctx, e, 2.5f);
						}
					}
				}
				return true;
			}
			SwordKit.fx(p, "water_burst", "tide", 0, c.add(0, 0.5, 0), Vec3.ZERO, SURGE_RADIUS);
			SwordKit.sound(p, c, layer(SoundEvents.GENERIC_SPLASH, 1.0f, 0.6f), layer(SoundEvents.PLAYER_SPLASH_HIGH_SPEED, 1.0f, 0.7f),
					layer(SoundEvents.GENERIC_EXPLODE, 0.6f, 0.8f));
			for (LivingEntity e : SwordKit.arc(p, c, Vec3.ZERO, SURGE_RADIUS + 0.5, 180, 2, 4)) {
				SwordKit.hitFresh(ctx, e, 12f);
				SwordKit.push(e, c, 1.5, 0.6);
			}
			return false;
		});
		return 0;
	}

	// =================================================================================================== Stormstaff

	static List<AbilityDef> stormstaff() {
		return List.of(def("stormstaff_storm_detonation", 80, 7.5, 15, SwordMovesB::stormDetonation),
				def("stormstaff_thunderfall", 180, 10.5, 25, SwordMovesB::thunderfall));
	}

	/** Z: an overhead swing throws a compact thundercloud that bursts into branching lightning on impact, pinning enemies. */
	static int stormDetonation(AbilityContext ctx) {
		ServerPlayer p = ctx.player();
		pose(p, POSE_OVERHEAD, 4);
		SwordKit.sound(p, p.position(), layer(SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 0.8f), later(SoundEvents.BEACON_POWER_SELECT, 0.6f, 2.0f, 2));
		TimedEffects.later(4, () -> {
			if (SwordKit.gone(p)) {
				return;
			}
			Vec3 start = p.getEyePosition().add(0, 0.5, 0).add(SwordKit.forward(p).scale(1.0));
			Vec3 dir = aim(p, start, 20);
			SwordKit.fx(p, "storm_cloud", "storm", 0, start, dir, 0.9, 20, 1.0);
			SwordShot.of(p, start, dir, 0.9, 20, 1.2).single()
					.onEnd((at, wall, t) -> {
						end(p, "storm", at, 0.1);
						SwordKit.fx(p, "storm_burst", "storm", 0, at, Vec3.ZERO, 3.5);
						SwordKit.sound(p, at, layer(SoundEvents.LIGHTNING_BOLT_IMPACT, 1.0f, 1.3f), layer(SoundEvents.GENERIC_EXPLODE, 0.6f, 1.4f),
								layer(SoundEvents.BEACON_DEACTIVATE, 0.6f, 2.0f));
						for (LivingEntity e : SwordKit.sphere(p, at, 3.5)) {
							if (SwordKit.hitFresh(ctx, e, 16f)) {
								Stuns.apply(e, 16);
							}
						}
					}).fire();
		});
		return 0;
	}

	static final int[] BOLT_TICKS = {12, 20, 28};
	static final double BOLT_RADIUS = 2.8;

	/** X: a storm cloud gathers over the target spot and three lightning bolts strike beneath it, one after another. */
	static int thunderfall(AbilityContext ctx) {
		ServerPlayer p = ctx.player();
		pose(p, POSE_RAISE, 6);
		Vec3 target = SwordKit.ground(p.serverLevel(), Projectiles.crosshairTarget(p, 30));
		SwordKit.fx(p, "thundercloud", "storm", 0, target, Vec3.ZERO, BOLT_TICKS[2] + 10, 3, 3.2);
		SwordKit.sound(p, target, layer(SoundEvents.LIGHTNING_BOLT_THUNDER, 0.5f, 1.6f));
		for (int i = 0; i < 3; i++) {
			int n = i;
			TimedEffects.later(BOLT_TICKS[i], () -> {
				if (SwordKit.gone(p)) {
					return;
				}
				Vec3 at = target.add((n - 1) * 0.9, 0, (n % 2 == 0 ? 0.6 : -0.6));
				SwordKit.fx(p, "bolt", "storm", n, at, Vec3.ZERO, 0, 9, BOLT_RADIUS);
				SwordKit.sound(p, at, layer(SoundEvents.LIGHTNING_BOLT_IMPACT, 1.2f, 1.0f + n * 0.1f), layer(SoundEvents.LIGHTNING_BOLT_THUNDER, 0.35f, 1.8f));
				for (LivingEntity e : SwordKit.arc(p, at, Vec3.ZERO, BOLT_RADIUS, 180, 1.5, 4)) {
					SwordKit.hitFresh(ctx, e, 8f);
					SwordKit.push(e, at, 0.3, 0.2);
				}
			});
		}
		return 0;
	}

	// =================================================================================================== Saber

	static List<AbilityDef> saber() {
		return List.of(def("saber_crimson_onslaught", 50, 7.5, 15, SwordMovesB::crimsonOnslaught),
				def("saber_scarlet_crossfire", 125, 14, 30, SwordMovesB::scarletCrossfire));
	}

	/** A Saber move's base damage times {@code saberMoveDamageMultiplier} (read when the move is used). */
	static float saberMove(float base) {
		return (float) (base * GrandLineMod.config().saberMoveDamageMultiplier);
	}

	static final int ONSLAUGHT_CHARGE = 5;

	/**
	 * Z: a moment's crimson charge, then a dash wrapped in crossing red cuts while two large crimson blades fly ahead.
	 * Dash and blades hit separately; an enemy struck by both takes an extra combined hit.
	 */
	static int crimsonOnslaught(AbilityContext ctx) {
		ServerPlayer p = ctx.player();
		pose(p, POSE_RAISE, ONSLAUGHT_CHARGE);
		SwordKit.fx(p, "charge", "saber", 0, p.position(), Vec3.ZERO, ONSLAUGHT_CHARGE);
		SwordKit.sound(p, p.position(), layer(SoundEvents.BEACON_POWER_SELECT, 0.7f, 1.6f), layer(SoundEvents.FIRECHARGE_USE, 0.5f, 1.8f));
		TimedEffects.later(ONSLAUGHT_CHARGE, () -> {
			if (SwordKit.gone(p)) {
				return;
			}
			pose(p, POSE_THRUST, 0);
			SwordKit.sound(p, p.position(), layer(SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 0.9f), layer(SoundEvents.PLAYER_ATTACK_STRONG, 1.0f, 0.8f),
					layer(SoundEvents.WARDEN_SONIC_BOOM, 0.25f, 2.0f));
			Set<Integer> byBlade = new HashSet<>();
			Set<Integer> byDash = new HashSet<>();
			Set<Integer> combined = new HashSet<>();
			Vec3 look = p.getLookAngle();
			Vec3 start = SwordKit.chest(p).add(SwordKit.forward(p).scale(1.0));
			Vec3 dir = aim(p, start, 18);
			for (int i = -1; i <= 1; i += 2) {
				Vec3 d = yaw(dir, i * 7);
				blade(ctx, "crescent", "saber", i < 0 ? 4 : 5, start, d, 1.5, 18, 1.2, 3.0, saberMove(9f), (t, at) -> {
					byBlade.add(t.getId());
					if (byDash.contains(t.getId()) && combined.add(t.getId())) {
						SwordKit.hitFresh(ctx, t, saberMove(6f));
						SwordKit.fx(p, "flash", "saber", 0, t.position().add(0, t.getBbHeight() * 0.6, 0), Vec3.ZERO, 1.4);
					}
				});
			}
			byDash.addAll(dash(ctx, "saber", 4, look, 7.5, 5, 1.4, saberMove(10f), (t, at) -> {
				byDash.add(t.getId());
				SwordKit.fx(p, "cut", "saber", 5, t.position().add(0, t.getBbHeight() * 0.6, 0), SwordKit.forward(p), 1.3);
				if (byBlade.contains(t.getId()) && combined.add(t.getId())) {
					SwordKit.hitFresh(ctx, t, saberMove(6f));
					SwordKit.fx(p, "flash", "saber", 0, t.position().add(0, t.getBbHeight() * 0.6, 0), Vec3.ZERO, 1.4);
				}
				SwordKit.push(t, at, 0.4, 0.25);
			}));
		});
		return 0;
	}

	static final int CROSSFIRE_WINDUP = 5;

	/** X: one powerful swing releases four crossing crimson blades that travel as a single formation. */
	static int scarletCrossfire(AbilityContext ctx) {
		ServerPlayer p = ctx.player();
		pose(p, POSE_SLASH_L, CROSSFIRE_WINDUP);
		SwordKit.fx(p, "charge", "saber", 1, p.position(), Vec3.ZERO, CROSSFIRE_WINDUP);
		SwordKit.sound(p, p.position(), layer(SoundEvents.BEACON_POWER_SELECT, 0.8f, 1.3f));
		TimedEffects.later(CROSSFIRE_WINDUP, () -> {
			if (SwordKit.gone(p)) {
				return;
			}
			SwordKit.sound(p, p.position(), layer(SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 0.6f), layer(SoundEvents.WARDEN_SONIC_BOOM, 0.5f, 1.5f),
					layer(SoundEvents.ENDER_DRAGON_FLAP, 1.0f, 1.1f));
			Vec3 start = SwordKit.chest(p).add(SwordKit.forward(p).scale(1.2));
			Vec3 dir = aim(p, start, 22);
			blade(ctx, "crossfire", "saber", 0, start, dir, 1.3, 22, 2.4, 3.4, saberMove(24f), (t, at) -> {
				SwordKit.fx(p, "flash", "saber", 1, t.position().add(0, t.getBbHeight() * 0.6, 0), dir, 2.0);
				SwordKit.launch(t, t.getDeltaMovement().add(dir.multiply(1, 0, 1).normalize().scale(1.1)).add(0, 0.3, 0));
				SwordKit.sound(p, at, layer(SoundEvents.PLAYER_ATTACK_CRIT, 1.0f, 0.7f), layer(SoundEvents.GENERIC_EXPLODE, 0.3f, 1.6f));
			});
		});
		return 0;
	}

	// =================================================================================================== Eclipse Blade

	static List<AbilityDef> eclipseBlade() {
		AbilityDef storm = charged("eclipse_blade_emerald_storm", 1, 8.5, 20, SwordMovesB::emeraldStorm);
		AbilityDef cleaver = charged("eclipse_blade_eclipse_cleaver", 50, 13, 40, SwordMovesB::eclipseCleaver);
		// Only the upgraded blade charges: otherwise both fire the moment the key goes down.
		ChargeService.chargeIf(storm, SwordUpgrades::upgraded);
		ChargeService.chargeIf(cleaver, SwordUpgrades::upgraded);
		ChargeService.visual(storm, (pl, charge, ticks) -> {
			if (ticks % 4 == 0) {
				SwordKit.fx(pl, "eclipse_charge", "eclipse", 0, pl.position(), Vec3.ZERO, charge);
			}
		});
		ChargeService.visual(cleaver, (pl, charge, ticks) -> {
			if (ticks % 4 == 0) {
				SwordKit.fx(pl, "eclipse_charge", "eclipse", 1, pl.position(), Vec3.ZERO, charge);
			}
		});
		return List.of(storm, cleaver);
	}

	static final int STORM_HITS = 6;

	/**
	 * Z: an almost instant barrage of crossing emerald blades in front of the caster: several hits, a short stun and an
	 * emerald flash to finish. Upgraded and held before release, the caster dashes forward with the barrage.
	 */
	static int emeraldStorm(AbilityContext ctx) {
		ServerPlayer p = ctx.player();
		boolean dashing = ctx.charge() > 0 && SwordUpgrades.upgraded(p, ctx.power());
		pose(p, POSE_SLASH_R, 0);
		SwordKit.fx(p, "blade_storm", "eclipse", dashing ? 1 : 0, p.position(), SwordKit.forward(p), STORM_HITS * 2, 5, 4.0);
		SwordKit.sound(p, p.position(), layer(SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 1.3f), later(SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 1.5f, 3),
				later(SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 1.7f, 6), layer(SoundEvents.AMETHYST_CLUSTER_BREAK, 1.0f, 0.8f),
				later(SoundEvents.WARDEN_SONIC_BOOM, 0.3f, 1.9f, STORM_HITS * 2));
		if (dashing) {
			dash(ctx, "eclipse", 1, p.getLookAngle(), 4 + 4 * ctx.charge(), STORM_HITS * 2, 1.4, 5f, (t, at) -> { });
		}
		TimedEffects.start(age -> {
			if (SwordKit.gone(p)) {
				return false;
			}
			Vec3 f = SwordKit.forward(p);
			if (age % 2 == 0 && age / 2 < STORM_HITS) {
				for (LivingEntity e : SwordKit.arc(p, p.position(), f, 5.0, 60, 1.2, 3)) {
					if (SwordKit.hitFresh(ctx, e, 3.4f)) {
						Stuns.apply(e, 12);
					}
				}
				return true;
			}
			if (age / 2 >= STORM_HITS) {
				Vec3 at = SwordKit.chest(p).add(f.scale(2.8));
				SwordKit.fx(p, "flash", "eclipse", 2, at, f, 2.6);
				return false;
			}
			return true;
		});
		return 0;
	}

	/**
	 * X: an enormous vertical emerald crescent with a dark core and glowing edges. Upgraded and held, it charges: faster
	 * and longer-ranged the longer the key is held.
	 */
	static int eclipseCleaver(AbilityContext ctx) {
		ServerPlayer p = ctx.player();
		double charge = SwordUpgrades.upgraded(p, ctx.power()) ? ctx.charge() : 0;
		double speed = 1.2 + charge;
		double range = 20 + 16 * charge;
		pose(p, POSE_OVERHEAD, 3);
		SwordKit.sound(p, p.position(), layer(SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 0.55f), later(SoundEvents.WARDEN_SONIC_BOOM, 0.6f, 1.3f, 3),
				later(SoundEvents.AMETHYST_CLUSTER_BREAK, 1.0f, 0.6f, 3));
		TimedEffects.later(3, () -> {
			if (SwordKit.gone(p)) {
				return;
			}
			Vec3 start = SwordKit.chest(p).add(SwordKit.forward(p).scale(1.3)).subtract(0, 0.4, 0);
			Vec3 dir = aim(p, start, range);
			blade(ctx, "cleaver", "eclipse", (int) Math.round(charge * 10), start, dir, speed, range, 2.2, 5.0, 25f, (t, at) -> {
				SwordKit.push(t, at.subtract(dir), 1.3, 0.45);
				SwordKit.fx(p, "flash", "eclipse", 1, t.position().add(0, t.getBbHeight() * 0.6, 0), dir, 1.8);
				SwordKit.sound(p, at, layer(SoundEvents.PLAYER_ATTACK_CRIT, 1.0f, 0.6f));
			});
		});
		return 0;
	}
}
