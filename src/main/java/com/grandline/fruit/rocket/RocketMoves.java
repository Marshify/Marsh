package com.grandline.fruit.rocket;

import com.grandline.combat.ability.AbilityContext;
import com.grandline.combat.ability.AbilityDef;
import com.grandline.combat.ability.AbilityHelpers;
import com.grandline.combat.ability.FlightService;
import com.grandline.combat.ability.Projectiles;
import com.grandline.combat.ability.TimedEffects;
import com.grandline.combat.fx.FxEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Rocket Fruit. Every move is a server timeline (windup, launch, travel, impact, recovery): the server decides every
 * hit from the same speeds, sizes and ticks the clients draw, and sends one visual beat per step ({@link FxEvents});
 * the client's RocketFx draws the rocket model, exhaust, smoke and blasts from those beats.
 *
 * <ul>
 *   <li>Z Missile Fist: the fist fires off as a rocket and bursts on contact (100%).</li>
 *   <li>X Air Strike: five rockets leave the back, climb, and dive one after another onto the aimed spot (120% at most
 *   per target).</li>
 *   <li>C Rocket Crash: ignition at the feet, the body launches along the aim and explodes where it lands (140%).</li>
 *   <li>V: unused (the Rocket has four moves; Blast Off keeps the movement key).</li>
 *   <li>F Blast Off: sustained rocket flight, steered with the normal flight controls, draining energy.</li>
 * </ul>
 *
 * Damage is a share of {@link #BASE_DAMAGE}; energy costs are shares of the player's max energy.
 */
public final class RocketMoves {
	/** 100% damage, before mastery and stat scaling ({@link AbilityContext#damage}). */
	public static final float BASE_DAMAGE = 8.0f;

	// --- Z: Missile Fist ---
	public static final int FIST_WINDUP_TICKS = 4;
	public static final double FIST_SPEED = 1.9;
	public static final double FIST_RANGE = 32;
	public static final double FIST_BLAST = 2.6;

	// --- X: Air Strike ---
	public static final int STRIKE_ROCKETS = 5;
	public static final int STRIKE_WINDUP_TICKS = 6;
	public static final int STRIKE_INTERVAL = 3;
	/** Ticks from a rocket leaving the back to its impact (climb then dive; the client draws the same arc). */
	public static final int STRIKE_FLIGHT_TICKS = 18;
	public static final double STRIKE_RANGE = 36;
	public static final double STRIKE_SPREAD = 2.4;
	public static final double STRIKE_BLAST = 2.6;
	/** Each rocket deals 40%; a target takes at most three, so 120% in all. */
	public static final int STRIKE_MAX_HITS = 3;

	// --- C: Rocket Crash ---
	public static final int CRASH_WINDUP_TICKS = 6;
	public static final double CRASH_SPEED = 1.15;
	public static final double CRASH_RANGE = 16;
	public static final int CRASH_MAX_TICKS = 20;
	public static final double CRASH_BLAST = 4.2;

	// --- F: Blast Off ---
	static final int FLIGHT_MAX_TICKS = 200;
	static final double FLIGHT_DRAIN_PER_TICK = 0.012;
	static final float FLIGHT_SPEED = 0.09f;

	/** Players mid-crash (so a second crash, death or logout ends the first cleanly). */
	private static final Map<UUID, Integer> CRASHING = new HashMap<>();

	private RocketMoves() {
	}

	public static List<AbilityDef> moves() {
		return List.of(
				new AbilityDef("rocket_missile_fist", 1, 150, 0.15, RocketMoves::missileFist),
				new AbilityDef("rocket_air_strike", 20, 210, 0.25, RocketMoves::airStrike),
				new AbilityDef("rocket_crash", 50, 285, 0.35, RocketMoves::rocketCrash),
				AbilityDef.empty(),
				new AbilityDef("rocket_blast_off", 75, 150, 0.35, AbilityDef.Activation.TOGGLE, RocketMoves::blastOff));
	}

	public static boolean isCrashing(ServerPlayer player) {
		return CRASHING.containsKey(player.getUUID());
	}

	private static boolean gone(ServerPlayer player) {
		return !player.isAlive() || player.isRemoved();
	}

	// ============================================================================================== Z  Missile Fist

	static int missileFist(AbilityContext ctx) {
		ServerPlayer player = ctx.player();
		FxEvents.send(player, "rocket_fist_windup", player.position());
		TimedEffects.later(FIST_WINDUP_TICKS, () -> {
			if (gone(player)) {
				return;
			}
			Vec3 start = handPoint(player);
			Vec3 dir = Projectiles.crosshairAim(player, FIST_RANGE).subtract(start).normalize();
			FxEvents.send(player, "rocket_fist", start, dir, FIST_SPEED, FIST_RANGE);
			Projectiles.fire(player, start, dir, FIST_SPEED, FIST_RANGE, 0.45, (level, pos, a) -> {
			}, impact -> {
				Vec3 at = impact.position();
				boolean terrain = impact.entity().isEmpty() && at.distanceTo(start) < FIST_RANGE - 0.5;
				boolean hit = impact.entity().isPresent() || terrain;
				// 2 = struck an enemy, 1 = struck terrain, 0 = burnt out at range (a small pop, no damage).
				FxEvents.send(player, "rocket_fist_hit", at, dir, 0, 0, impact.entity().isPresent() ? 2 : terrain ? 1 : 0);
				if (hit) {
					blast(ctx, at, FIST_BLAST, BASE_DAMAGE, 0.6, 0.35, null);
				}
			});
		});
		return 0;
	}

	// ============================================================================================== X  Air Strike

	static int airStrike(AbilityContext ctx) {
		ServerPlayer player = ctx.player();
		Vec3 target = groundBelow(player, Projectiles.crosshairTarget(player, STRIKE_RANGE));
		// The marked area shows on every client from the first beat, so the target can see it coming.
		FxEvents.send(player, "rocket_strike_mark", target, Vec3.ZERO, 0, 0, STRIKE_SPREAD + STRIKE_BLAST);
		Map<Integer, Integer> hits = new HashMap<>();
		float perRocket = BASE_DAMAGE * 1.2f / STRIKE_MAX_HITS;
		for (int i = 0; i < STRIKE_ROCKETS; i++) {
			int n = i;
			// The first rocket lands on the mark, the rest round it.
			double angle = n * Math.PI * 2 / (STRIKE_ROCKETS - 1) + player.getYRot() * Math.PI / 180;
			Vec3 spot = n == 0 ? target : groundBelow(player, target.add(Math.cos(angle) * STRIKE_SPREAD, 1.5, Math.sin(angle) * STRIKE_SPREAD));
			TimedEffects.later(STRIKE_WINDUP_TICKS + n * STRIKE_INTERVAL, () -> {
				if (gone(player)) {
					return;
				}
				Vec3 from = backPoint(player, n);
				FxEvents.send(player, "rocket_strike_launch", from, spot.subtract(from), STRIKE_FLIGHT_TICKS, 0, n);
				TimedEffects.later(STRIKE_FLIGHT_TICKS, () -> {
					if (!player.isRemoved()) {
						FxEvents.send(player, "rocket_strike_hit", spot, Vec3.ZERO, 0, 0, n);
						blast(ctx, spot, STRIKE_BLAST, perRocket, 0.3, 0.45, hits);
					}
				});
			});
		}
		return 0;
	}

	// ============================================================================================== C  Rocket Crash

	static int rocketCrash(AbilityContext ctx) {
		ServerPlayer player = ctx.player();
		UUID id = player.getUUID();
		int run = player.server.getTickCount();
		CRASHING.put(id, run);
		Vec3[] body = {player.position()}; // the server's own position of the flying body
		FxEvents.send(player, "rocket_crash_windup", player.position());
		TimedEffects.start(age -> {
			if (!Integer.valueOf(run).equals(CRASHING.get(id))) {
				return false; // replaced by a newer crash
			}
			if (gone(player)) {
				CRASHING.remove(id);
				FxEvents.send(player, "rocket_crash_end", player.position());
				return false;
			}
			player.fallDistance = 0;
			if (age < CRASH_WINDUP_TICKS) {
				player.setDeltaMovement(player.getDeltaMovement().multiply(0.3, 0.3, 0.3));
				player.hurtMarked = true;
				return true;
			}
			int t = age - CRASH_WINDUP_TICKS;
			Vec3 dir = crashDirection(player);
			if (t == 0) {
				body[0] = player.position();
				FxEvents.send(player, "rocket_crash_launch", player.position(), dir, CRASH_SPEED, CRASH_RANGE);
			}
			// The server runs the flight path itself (the client is only sent the matching velocity): the next step
			// stops at a wall or the ground, or at an enemy in the way.
			Vec3 from = body[0].add(0, player.getBbHeight() / 2, 0);
			Vec3 step = dir.scale(CRASH_SPEED);
			BlockHitResult wall = player.serverLevel().clip(new ClipContext(from, from.add(step.scale(1.4)), ClipContext.Block.COLLIDER,
					ClipContext.Fluid.NONE, player));
			BlockHitResult feet = player.serverLevel().clip(new ClipContext(body[0].add(0, 0.2, 0), body[0].add(step).subtract(0, 0.05, 0),
					ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
			boolean enemy = !AbilityHelpers.inRadius(player, from.add(step), 1.3).isEmpty();
			boolean landed = t > 2 && step.y < -0.05 && feet.getType() != HitResult.Type.MISS;
			if (wall.getType() != HitResult.Type.MISS || enemy || landed || t * CRASH_SPEED >= CRASH_RANGE || t >= CRASH_MAX_TICKS) {
				Vec3 at = wall.getType() != HitResult.Type.MISS ? wall.getLocation() : enemy ? from.add(step) : body[0];
				CRASHING.remove(id);
				player.setDeltaMovement(Vec3.ZERO);
				player.hurtMarked = true;
				// Server authority: the body ends where the server's path ended (corrects any client drift).
				if (player.position().distanceTo(body[0]) > 1.5) {
					player.teleportTo(body[0].x, body[0].y, body[0].z);
				}
				FxEvents.send(player, "rocket_crash_impact", at, dir, 0, 0, CRASH_BLAST);
				blast(ctx, at, CRASH_BLAST, BASE_DAMAGE * 1.4f, 1.3, 0.7, null);
				return false;
			}
			body[0] = body[0].add(step);
			player.setDeltaMovement(step);
			player.hurtMarked = true;
			return true;
		});
		return 0;
	}

	/** The crash's path: the aim, kept between a 30° dive and a 35° climb so it always travels. */
	static Vec3 crashDirection(ServerPlayer player) {
		Vec3 look = player.getLookAngle();
		double flat = Math.hypot(look.x, look.z);
		double pitch = Math.max(-Math.toRadians(30), Math.min(Math.toRadians(35), Math.atan2(look.y, flat)));
		Vec3 h = flat < 1e-4 ? new Vec3(0, 0, 1) : new Vec3(look.x / flat, 0, look.z / flat);
		return h.scale(Math.cos(pitch)).add(0, Math.sin(pitch), 0).normalize();
	}

	// ============================================================================================== F  Blast Off

	static int blastOff(AbilityContext ctx) {
		ServerPlayer player = ctx.player();
		// Ignition and lift-off; clients draw the exhaust until the "flight_end" beat (FlightService.stop).
		FxEvents.send(player, "rocket_flight_start", player.position());
		FlightService.start(player, ctx.power(), ctx.ability(), FLIGHT_MAX_TICKS, FLIGHT_DRAIN_PER_TICK, FLIGHT_SPEED, (flier, ticks) -> {
		});
		return 0;
	}

	// ============================================================================================== shared

	/**
	 * An explosion at {@code at}: full damage at the centre, {@code edge} of it at the rim. {@code hits} (when given)
	 * counts each target's hits so one strike can't deal more than {@link #STRIKE_MAX_HITS} rockets' worth.
	 */
	static void blast(AbilityContext ctx, Vec3 at, double radius, float damage, double knock, double lift, Map<Integer, Integer> hits) {
		Set<Integer> struck = new HashSet<>();
		for (LivingEntity target : AbilityHelpers.inRadius(ctx.player(), at, radius)) {
			if (!struck.add(target.getId())) {
				continue;
			}
			if (hits != null) {
				int n = hits.getOrDefault(target.getId(), 0);
				if (n >= STRIKE_MAX_HITS) {
					continue;
				}
				hits.put(target.getId(), n + 1);
			}
			double falloff = 1.0 - 0.4 * Math.min(1.0, target.getBoundingBox().getCenter().distanceTo(at) / radius);
			if (ctx.damage(target, (float) (damage * falloff))) {
				AbilityHelpers.knockAway(target, at, knock, lift);
			}
		}
	}

	/** The punching hand, just ahead of the right shoulder. */
	static Vec3 handPoint(ServerPlayer player) {
		Vec3 look = player.getLookAngle();
		Vec3 side = look.cross(new Vec3(0, 1, 0)).normalize().scale(0.32);
		return player.getEyePosition().subtract(0, 0.3, 0).add(look.scale(0.8)).add(side);
	}

	/** Where strike rocket {@code n} leaves the back: five ports spread across the shoulders. */
	static Vec3 backPoint(ServerPlayer player, int n) {
		double yaw = Math.toRadians(player.getYRot());
		Vec3 back = new Vec3(Math.sin(yaw), 0, -Math.cos(yaw)).scale(0.45);
		Vec3 side = new Vec3(Math.cos(yaw), 0, Math.sin(yaw)).scale((n - 2) * 0.28);
		return player.position().add(0, 1.55 + (n % 2) * 0.15, 0).add(back).add(side);
	}

	/** The first solid surface at or below {@code at} (within 12 blocks), so strikes land on the ground. */
	public static Vec3 groundBelow(ServerPlayer player, Vec3 at) {
		BlockHitResult hit = player.serverLevel().clip(new ClipContext(at.add(0, 0.5, 0), at.subtract(0, 12, 0), ClipContext.Block.COLLIDER,
				ClipContext.Fluid.ANY, player));
		return hit.getType() == HitResult.Type.MISS ? at : hit.getLocation();
	}

	/** Clears every running crash (server stop). */
	public static void clear() {
		CRASHING.clear();
	}
}
