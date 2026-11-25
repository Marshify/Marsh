package com.grandline.fruit.spring;

import com.grandline.combat.MovementGuards;
import com.grandline.combat.ability.AbilityContext;
import com.grandline.combat.ability.AbilityDef;
import com.grandline.combat.ability.AbilityHelpers;
import com.grandline.combat.ability.KineticBody;
import com.grandline.combat.ability.Projectiles;
import com.grandline.combat.ability.TimedEffects;
import com.grandline.combat.fx.FxEvents;
import com.grandline.technique.CombatContext;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Spring Fruit: limbs become steel springs for a moment. Every move is a server timeline: the server moves the body
 * ({@link KineticBody}: swept, never into blocks), decides every hit, and sends beats for the client's SpringFx
 * (procedural springs on the limbs, afterimages, rings, dust). Movement is fall-protected only while a move moves the
 * player ({@link MovementGuards}).
 *
 * <p>Instinct: a direct hit (close to the centre line of the punch or path, or the middle of the slam) breaks through
 * Instinct; a glancing hit at the edge stays dodgeable.
 *
 * <p>Damage is a share of Rocket's Missile Fist ({@link #REF} = 8): Knock 80%, Spring Snipe 110%, Spring Cannon 130%
 * (centre), Spring Emperor 40% per collision, at most 4 on one target (160%).
 */
public final class SpringMoves {
	public static final float REF = com.grandline.fruit.rocket.RocketMoves.BASE_DAMAGE;
	public static final String FRUIT_ID = "grandline:spring";

	// --- Z: Knock ---
	public static final double KNOCK_RANGE = 8;
	public static final int KNOCK_HIT_TICK = 2;
	public static final double KNOCK_DIRECT_RANGE = 5, KNOCK_DIRECT_LATERAL = 0.6;
	static final float KNOCK_DAMAGE = REF * 0.80f;

	// --- X: Spring Snipe ---
	public static final int SNIPE_WINDUP = 4;
	public static final double SNIPE_RANGE = 25, SNIPE_SPEED = 3.0, SNIPE_DIRECT = 0.8;
	static final float SNIPE_DAMAGE = REF * 1.10f;

	// --- C: Spring Cannon ---
	public static final int CANNON_WINDUP = 3, CANNON_RISE = 6, CANNON_HOLD = 2, CANNON_SLAM = 4;
	public static final double CANNON_HEIGHT = 8, CANNON_RADIUS = 4.5, CANNON_CORE = 2.0;
	static final float CANNON_DAMAGE = REF * 1.30f;

	// --- V: Spring Emperor ---
	public static final double EMPEROR_HALF = 8; // the square is 16 blocks across
	public static final int EMPEROR_TICKS = 60;
	public static final double EMPEROR_SPEED = 1.3;
	public static final int EMPEROR_HIT_INTERVAL = 10, EMPEROR_MAX_HITS = 4;
	static final float EMPEROR_HIT = REF * 0.40f;

	// --- F: Spring Leap (momentum rebounds) ---
	public static final double LEAP_START_SPEED = 1.1;   // blocks/tick (22 blocks/s)
	public static final double LEAP_MAX_SPEED = 2.0;     // 40 blocks/s cap
	public static final double LEAP_WALL_GAIN = 1.18;    // per valid wall rebound
	public static final double LEAP_DECAY = 0.975;       // per tick once rebounds stop
	public static final int LEAP_DECAY_AFTER = 12;       // ticks without a wall rebound before decay
	public static final int LEAP_MIN_INTERVAL = 3;       // ticks between rebounds
	public static final int LEAP_SAME_SURFACE = 10;      // the same wall again within this gives no gain
	public static final int LEAP_MAX_TICKS = 160;
	static final double LEAP_GRAVITY = 0.07;

	private static final Map<UUID, Integer> MOVING = new HashMap<>();
	private static final Map<UUID, Leap> LEAPS = new HashMap<>();

	private SpringMoves() {
	}

	public static List<AbilityDef> moves() {
		return List.of(
				new AbilityDef("spring_knock", 1, 120, 0.15, SpringMoves::knock),
				new AbilityDef("spring_snipe", 20, 180, 0.25, SpringMoves::snipe),
				new AbilityDef("spring_cannon", 50, 270, 0.35, SpringMoves::cannon),
				new AbilityDef("spring_emperor", 125, 390, 0.65, SpringMoves::emperor),
				new AbilityDef("spring_leap", 75, 120, 0.35, SpringMoves::leap));
	}

	/** Still a Spring user, alive, in the same world and not removed. */
	static boolean valid(ServerPlayer player, Level level) {
		return player.isAlive() && !player.isRemoved() && player.level() == level
				&& FRUIT_ID.equals(com.grandline.player.PlayerProfileService.get(player).powers().fruit());
	}

	/** Damage that breaks Instinct when {@code direct} (a clean hit), dodgeable otherwise. */
	static boolean hit(AbilityContext ctx, LivingEntity target, float damage, boolean direct) {
		return direct ? CombatContext.breaking(() -> ctx.damage(target, damage)) : ctx.damage(target, damage);
	}

	/** Keeps the player's real position following the server's body: velocity toward it, or a snap if far behind. */
	static void drive(ServerPlayer player, Vec3 body) {
		Vec3 delta = body.subtract(player.position());
		if (delta.length() > 3) {
			player.teleportTo(body.x, body.y, body.z);
			player.setDeltaMovement(Vec3.ZERO);
		} else {
			player.setDeltaMovement(delta);
		}
		player.hurtMarked = true;
		player.fallDistance = 0;
	}

	static void finish(ServerPlayer player, Vec3 body) {
		if (player.position().distanceTo(body) > 1.5) {
			player.teleportTo(body.x, body.y, body.z);
		}
		player.setDeltaMovement(Vec3.ZERO);
		player.hurtMarked = true;
		MovementGuards.release(player);
		MOVING.remove(player.getUUID());
	}

	/** Starts a moving move; a newer one replaces it (the older timeline sees its token change and stops). */
	static int claim(ServerPlayer player) {
		int token = player.server.getTickCount() * 7 + player.getRandom().nextInt(7);
		MOVING.put(player.getUUID(), token);
		LEAPS.remove(player.getUUID());
		return token;
	}

	static boolean current(ServerPlayer player, int token) {
		return Integer.valueOf(token).equals(MOVING.get(player.getUUID()));
	}

	public static boolean moving(ServerPlayer player) {
		return MOVING.containsKey(player.getUUID());
	}

	// ============================================================================================== Z  Knock

	static int knock(AbilityContext ctx) {
		ServerPlayer player = ctx.player();
		Level level = player.level();
		Vec3 eye = player.getEyePosition();
		Vec3 dir = player.getLookAngle();
		Vec3 side = dir.cross(new Vec3(0, 1, 0)).normalize().scale(0.34);
		Vec3 start = eye.subtract(0, 0.35, 0).add(side);
		BlockHitResult wall = level.clip(new ClipContext(start, start.add(dir.scale(KNOCK_RANGE)), ClipContext.Block.COLLIDER,
				ClipContext.Fluid.NONE, player));
		double reach = wall.getType() == HitResult.Type.MISS ? KNOCK_RANGE : wall.getLocation().distanceTo(start) - 0.2;
		// Out to the first enemy on the line (the fist stops there), else to the wall or full range.
		Vec3 end = start.add(dir.scale(reach));
		double first = reach;
		for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(start, end).inflate(0.6), e -> e != player && e.isAlive())) {
			Optional<Vec3> c = e.getBoundingBox().inflate(0.45).clip(start, end);
			if (c.isPresent()) {
				first = Math.min(first, c.get().distanceTo(start));
			}
		}
		double shown = Math.min(reach, first + 0.6);
		FxEvents.send(player, "spring_knock", start, dir, 0, shown, 0);
		Vec3 segEnd = start.add(dir.scale(reach));
		TimedEffects.later(KNOCK_HIT_TICK, () -> {
			if (!valid(player, level)) {
				return;
			}
			for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new AABB(start, segEnd).inflate(1.0),
					e -> e != player && e.isAlive() && !e.isSpectator())) {
				Optional<Vec3> c = target.getBoundingBox().inflate(0.45).clip(start, segEnd);
				if (c.isEmpty() || !player.hasLineOfSight(target)) {
					continue;
				}
				Vec3 centre = target.getBoundingBox().getCenter();
				double along = centre.subtract(start).dot(dir);
				double lateral = KineticBody.distanceToSegment(centre, start, segEnd);
				boolean direct = along <= KNOCK_DIRECT_RANGE && lateral <= KNOCK_DIRECT_LATERAL + target.getBbWidth() / 2;
				if (hit(ctx, target, KNOCK_DAMAGE, direct)) {
					AbilityHelpers.pushResisted(target, dir.scale(1.2).add(0, 0.25, 0));
					FxEvents.send(player, "spring_knock_hit", c.get(), dir, 0, 0, direct ? 1 : 0);
				}
			}
		});
		return 0;
	}

	// ============================================================================================== X  Spring Snipe

	static int snipe(AbilityContext ctx) {
		ServerPlayer player = ctx.player();
		Level level = player.level();
		int token = claim(player);
		Vec3 look = player.getLookAngle();
		double pitch = Math.max(-Math.toRadians(60), Math.min(Math.toRadians(60), Math.asin(look.y)));
		double flat = Math.hypot(look.x, look.z);
		Vec3 h = flat < 1e-4 ? new Vec3(0, 0, 1) : new Vec3(look.x / flat, 0, look.z / flat);
		Vec3 dir = h.scale(Math.cos(pitch)).add(0, Math.sin(pitch), 0).normalize();
		double reach = Math.min(SNIPE_RANGE, Projectiles.crosshairTarget(player, SNIPE_RANGE).distanceTo(player.getEyePosition()));
		Vec3[] body = {player.position()};
		double[] travelled = {0};
		Set<Integer> struck = new HashSet<>();
		MovementGuards.guard(player, SNIPE_WINDUP + 20);
		FxEvents.send(player, "spring_snipe_windup", player.position(), dir, 0, 0, 0);
		TimedEffects.start(age -> {
			if (!current(player, token)) {
				return false;
			}
			if (!valid(player, level)) {
				finish(player, player.position());
				FxEvents.send(player, "spring_move_end", player.position(), dir, 0, 0, 0);
				return false;
			}
			if (age < SNIPE_WINDUP) {
				player.setDeltaMovement(player.getDeltaMovement().multiply(0.2, 1, 0.2));
				player.hurtMarked = true;
				return true;
			}
			if (age == SNIPE_WINDUP) {
				body[0] = player.position();
				FxEvents.send(player, "spring_snipe", player.position(), dir, SNIPE_SPEED, reach, 0);
			}
			Vec3 from = body[0];
			KineticBody.Step step = KineticBody.sweep(player, from, dir.scale(Math.min(SNIPE_SPEED, reach - travelled[0])));
			body[0] = step.pos();
			travelled[0] += step.moved();
			for (LivingEntity target : KineticBody.touched(player, from, step.pos(), 0.3, struck)) {
				Vec3 c = target.getBoundingBox().getCenter();
				boolean direct = KineticBody.distanceToSegment(c, from.add(0, 0.9, 0), step.pos().add(0, 0.9, 0)) <= SNIPE_DIRECT + target.getBbWidth() / 2;
				if (hit(ctx, target, SNIPE_DAMAGE, direct)) {
					AbilityHelpers.pushResisted(target, dir.scale(1.0).add(0, 0.3, 0));
					FxEvents.send(player, "spring_collide", c, dir, 0, 0, direct ? 1 : 0);
				}
			}
			drive(player, body[0]);
			if (step.normal() != null || travelled[0] >= reach - 0.01 || age > SNIPE_WINDUP + 12) {
				finish(player, body[0]);
				FxEvents.send(player, "spring_move_end", body[0], step.normal() != null ? step.normal() : dir, 0, 0, 1);
				return false;
			}
			return true;
		});
		return 0;
	}

	// ============================================================================================== C  Spring Cannon

	static int cannon(AbilityContext ctx) {
		ServerPlayer player = ctx.player();
		Level level = player.level();
		int token = claim(player);
		Vec3 base = player.position();
		// Grounded (stands on something within 1.5 blocks): the full move. In the air: a weaker, lower one.
		boolean grounded = !KineticBody.free(player, base.subtract(0, 1.5, 0)) || player.onGround();
		double height = grounded ? CANNON_HEIGHT : CANNON_HEIGHT / 2;
		double radius = grounded ? CANNON_RADIUS : 3;
		float damage = grounded ? CANNON_DAMAGE : CANNON_DAMAGE * 0.5f;
		Vec3[] body = {base};
		double[] top = {0};
		MovementGuards.guard(player, CANNON_WINDUP + CANNON_RISE + CANNON_HOLD + CANNON_SLAM + 30);
		FxEvents.send(player, "spring_cannon", base, new Vec3(0, 1, 0), 0, height, grounded ? 1 : 0);
		int slamStart = CANNON_WINDUP + CANNON_RISE + CANNON_HOLD;
		TimedEffects.start(age -> {
			if (!current(player, token)) {
				return false;
			}
			if (!valid(player, level)) {
				finish(player, player.position());
				FxEvents.send(player, "spring_move_end", player.position(), Vec3.ZERO, 0, 0, 0);
				return false;
			}
			if (age < CANNON_WINDUP) {
				player.setDeltaMovement(Vec3.ZERO);
				player.hurtMarked = true;
				return true;
			}
			if (age < CANNON_WINDUP + CANNON_RISE) {
				// Rise: the leg springs push the body straight up; a ceiling stops it (no clipping).
				double want = height / CANNON_RISE;
				KineticBody.Step step = KineticBody.sweep(player, body[0], new Vec3(0, want, 0));
				body[0] = step.pos();
				top[0] = body[0].y - base.y;
				drive(player, body[0]);
				return true;
			}
			if (age < slamStart) {
				drive(player, body[0]);
				return true;
			}
			if (age == slamStart) {
				FxEvents.send(player, "spring_cannon_slam", body[0], new Vec3(0, -1, 0), 0, top[0], 0);
			}
			// Slam: pulled back down to the ground it left (or until something stops it).
			double fall = Math.max(top[0], 1) / CANNON_SLAM * 1.25;
			KineticBody.Step step = KineticBody.sweep(player, body[0], new Vec3(0, -fall, 0));
			body[0] = step.pos();
			drive(player, body[0]);
			boolean landed = step.normal() != null && step.normal().y > 0.5 || body[0].y <= base.y - 0.01 && grounded;
			if (landed || age >= slamStart + CANNON_SLAM + 8) {
				finish(player, body[0]);
				impact(ctx, body[0], radius, damage);
				return false;
			}
			return true;
		});
		return 0;
	}

	/** The slam: everything close is launched straight up (direct in the core); the rim takes less and can dodge. */
	static void impact(AbilityContext ctx, Vec3 at, double radius, float damage) {
		ServerPlayer player = ctx.player();
		FxEvents.send(player, "spring_cannon_impact", at, new Vec3(0, 1, 0), 0, 0, radius);
		for (LivingEntity target : AbilityHelpers.inRadius(player, at.add(0, 0.5, 0), radius + 1)) {
			double d = Math.hypot(target.getX() - at.x, target.getZ() - at.z);
			if (d > radius + target.getBbWidth() / 2 || Math.abs(target.getY() - at.y) > 3) {
				continue;
			}
			boolean direct = d <= CANNON_CORE;
			float dealt = direct ? damage : (float) (damage * (1 - 0.35 * Math.min(1, (d - CANNON_CORE) / (radius - CANNON_CORE))));
			if (hit(ctx, target, dealt, direct)) {
				Vec3 out = new Vec3(target.getX() - at.x, 0, target.getZ() - at.z);
				out = out.lengthSqr() < 1e-4 ? Vec3.ZERO : out.normalize().scale(0.08); // up, barely outward
				Vec3 v = target.getDeltaMovement();
				target.setDeltaMovement(v.x * 0.15, v.y, v.z * 0.15); // drop the hit's ordinary sideways knockback
				AbilityHelpers.pushResisted(target, out.add(0, 0.95, 0));
			}
		}
	}

	// ============================================================================================== V  Spring Emperor

	static int emperor(AbilityContext ctx) {
		ServerPlayer player = ctx.player();
		Level level = player.level();
		int token = claim(player);
		Vec3 centre = player.position();
		Vec3 axis = new Vec3(player.getLookAngle().x, 0, player.getLookAngle().z);
		axis = axis.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : axis.normalize();
		Vec3 side = new Vec3(-axis.z, 0, axis.x);
		double a0 = Math.toRadians(35);
		Vec3[] dir = {axis.scale(Math.cos(a0)).add(side.scale(Math.sin(a0)))};
		Vec3[] body = {centre};
		Map<Integer, int[]> hits = new HashMap<>(); // id -> {last tick, count}
		int[] pause = {0};
		Vec3 ax = axis;
		MovementGuards.guard(player, EMPEROR_TICKS + 20);
		FxEvents.send(player, "spring_emperor", centre, axis, EMPEROR_SPEED, EMPEROR_TICKS, EMPEROR_HALF);
		TimedEffects.start(age -> {
			if (!current(player, token)) {
				return false;
			}
			if (!valid(player, level) || age >= EMPEROR_TICKS) {
				finish(player, valid(player, level) ? body[0] : player.position());
				FxEvents.send(player, "spring_move_end", body[0], dir[0], 0, 0, 1);
				return false;
			}
			if (age < 3 || pause[0] > 0) { // launch stance, and a one-tick compression at each rebound
				pause[0] = Math.max(0, pause[0] - 1);
				drive(player, body[0]);
				return true;
			}
			Vec3 from = body[0];
			KineticBody.Step step = KineticBody.sweep(player, from, dir[0].scale(EMPEROR_SPEED));
			Vec3 to = step.pos();
			// Stay on the ground: settle down slopes and ledges (up to half a block a tick).
			KineticBody.Step settle = KineticBody.sweep(player, to, new Vec3(0, -0.5, 0));
			to = settle.pos();
			Vec3 normal = step.normal() != null && Math.abs(step.normal().y) < 0.7 ? step.normal() : null;
			// The square's sides rebound too.
			Vec3 rel = to.subtract(centre);
			double u = rel.dot(ax), v = rel.dot(side);
			if (Math.abs(u) > EMPEROR_HALF) {
				normal = ax.scale(-Math.signum(u));
			} else if (Math.abs(v) > EMPEROR_HALF) {
				normal = side.scale(-Math.signum(v));
			}
			// Collisions along the actual path: each target at most once per 10 ticks and 4 times in all.
			for (LivingEntity target : KineticBody.touched(player, from, to, 0.25, new HashSet<>())) {
				int[] h = hits.computeIfAbsent(target.getId(), k -> new int[]{-100, 0});
				if (age - h[0] < EMPEROR_HIT_INTERVAL || h[1] >= EMPEROR_MAX_HITS) {
					continue;
				}
				Vec3 c = target.getBoundingBox().getCenter();
				boolean direct = KineticBody.distanceToSegment(c, from.add(0, 0.9, 0), to.add(0, 0.9, 0)) <= 0.9 + target.getBbWidth() / 2;
				if (hit(ctx, target, EMPEROR_HIT, direct)) {
					h[0] = age;
					h[1]++;
					AbilityHelpers.pushResisted(target, dir[0].scale(0.35).add(0, 0.15, 0));
					FxEvents.send(player, "spring_collide", c, dir[0], 0, 0, direct ? 1 : 0);
				}
			}
			body[0] = to;
			if (normal != null) {
				// Rebound: reflect off the surface, with a little variation so the route isn't a fixed loop.
				Vec3 d = dir[0];
				Vec3 r = d.subtract(normal.scale(2 * d.dot(normal)));
				double jitter = (player.getRandom().nextDouble() - 0.5) * 0.4;
				r = r.yRot((float) jitter).multiply(1, 0, 1);
				if (r.dot(normal) < 0.2) {
					r = r.add(normal.scale(0.5));
				}
				dir[0] = r.normalize();
				pause[0] = 1;
				FxEvents.send(player, "spring_rebound", body[0], normal, EMPEROR_SPEED, 0, 0);
			}
			drive(player, body[0]);
			return true;
		});
		return 0;
	}

	// ============================================================================================== F  Spring Leap

	/** A running Spring Leap (momentum state). */
	static final class Leap {
		Vec3 pos, vel;
		int lastRebound = -100, sinceWall, stuck, rebounds;
		Vec3 lastNormal;
	}

	/** The current Spring Leap speed (blocks/tick), or 0 (tests, HUD). */
	public static double leapSpeed(ServerPlayer player) {
		Leap l = LEAPS.get(player.getUUID());
		return l == null ? 0 : l.vel.length();
	}

	public static int leapRebounds(ServerPlayer player) {
		Leap l = LEAPS.get(player.getUUID());
		return l == null ? 0 : l.rebounds;
	}

	static int leap(AbilityContext ctx) {
		ServerPlayer player = ctx.player();
		Level level = player.level();
		if (AbilityHelpers.inWater(player)) {
			return 0;
		}
		int token = claim(player);
		Leap leap = new Leap();
		Vec3 look = player.getLookAngle();
		// Launch along the camera, never flatter than 20° up so the first bound clears the ground.
		double pitch = Math.max(Math.toRadians(20), Math.asin(Math.max(-1, Math.min(1, look.y))));
		double flat = Math.hypot(look.x, look.z);
		Vec3 h = flat < 1e-4 ? new Vec3(0, 0, 1) : new Vec3(look.x / flat, 0, look.z / flat);
		leap.pos = player.position();
		leap.vel = h.scale(Math.cos(pitch)).add(0, Math.sin(pitch), 0).normalize().scale(LEAP_START_SPEED);
		LEAPS.put(player.getUUID(), leap);
		MovementGuards.guard(player, LEAP_MAX_TICKS + 20);
		FxEvents.send(player, "spring_leap", leap.pos, leap.vel.normalize(), LEAP_START_SPEED, 0, 0);
		TimedEffects.start(age -> {
			if (!current(player, token)) {
				LEAPS.remove(player.getUUID());
				return false;
			}
			if (!valid(player, level) || AbilityHelpers.inWater(player) || age >= LEAP_MAX_TICKS || leap.stuck >= 6) {
				endLeap(player, leap);
				return false;
			}
			// Steering: the horizontal heading turns toward the camera (up to 6° a tick), speed kept.
			Vec3 hv = new Vec3(leap.vel.x, 0, leap.vel.z);
			Vec3 want = new Vec3(player.getLookAngle().x, 0, player.getLookAngle().z);
			if (hv.lengthSqr() > 1e-4 && want.lengthSqr() > 1e-4) {
				double cur = Math.atan2(hv.z, hv.x), tgt = Math.atan2(want.z, want.x);
				double diff = Math.atan2(Math.sin(tgt - cur), Math.cos(tgt - cur));
				double turn = Math.max(-Math.toRadians(6), Math.min(Math.toRadians(6), diff));
				double len = hv.length();
				leap.vel = new Vec3(Math.cos(cur + turn) * len, leap.vel.y, Math.sin(cur + turn) * len);
			}
			leap.vel = leap.vel.add(0, -LEAP_GRAVITY, 0);
			leap.sinceWall++;
			if (leap.sinceWall > LEAP_DECAY_AFTER) {
				leap.vel = new Vec3(leap.vel.x * LEAP_DECAY, leap.vel.y, leap.vel.z * LEAP_DECAY);
			}
			KineticBody.Step step = KineticBody.sweep(player, leap.pos, leap.vel);
			leap.stuck = step.moved() < 0.05 ? leap.stuck + 1 : 0;
			leap.pos = step.pos();
			Vec3 n = step.normal();
			if (n != null) {
				rebound(player, leap, n, age);
			}
			drive(player, leap.pos);
			// Stops once it has run out of bounce on the ground.
			if (n != null && n.y > 0.7 && Math.hypot(leap.vel.x, leap.vel.z) < 0.25 && leap.vel.y < 0.6) {
				endLeap(player, leap);
				return false;
			}
			return true;
		});
		return 0;
	}

	/** One contact: reflect off the surface normal and add momentum for a fresh wall, within the limits. */
	static void rebound(ServerPlayer player, Leap leap, Vec3 n, int age) {
		Vec3 v = leap.vel;
		boolean tooSoon = age - leap.lastRebound < LEAP_MIN_INTERVAL;
		if (tooSoon) {
			leap.vel = v.subtract(n.scale(Math.min(0, v.dot(n)))); // just stop pushing into it
			return;
		}
		if (n.y > 0.7) {
			// Ground: bounce up again, losing a little height each time; no speed gain.
			double up = Math.max(-v.y * 0.72, 0.45);
			leap.vel = new Vec3(v.x * 0.98, up, v.z * 0.98);
		} else if (n.y < -0.7) {
			leap.vel = new Vec3(v.x, -Math.abs(v.y) * 0.5, v.z); // ceiling: knocked back down
		} else {
			boolean sameWall = leap.lastNormal != null && n.dot(leap.lastNormal) > 0.9 && age - leap.lastRebound < LEAP_SAME_SURFACE;
			double speed = Math.min(LEAP_MAX_SPEED, v.length() * (sameWall ? 0.9 : LEAP_WALL_GAIN));
			Vec3 reflected = v.subtract(n.scale(2 * v.dot(n))).normalize();
			Vec3 look = player.getLookAngle();
			Vec3 dir = reflected.scale(0.65).add(look.scale(0.35)).normalize();
			if (dir.dot(n) < 0.2) {
				dir = dir.add(n.scale(0.5)).normalize(); // always leave the wall
			}
			leap.vel = dir.scale(speed);
			leap.vel = new Vec3(leap.vel.x, Math.max(leap.vel.y, 0.25), leap.vel.z);
			leap.sinceWall = 0;
			if (!sameWall) {
				leap.rebounds++;
			}
		}
		leap.lastRebound = age;
		leap.lastNormal = n;
		FxEvents.send(player, "spring_rebound", leap.pos, n, leap.vel.length(), LEAP_MAX_SPEED, 1);
	}

	static void endLeap(ServerPlayer player, Leap leap) {
		LEAPS.remove(player.getUUID());
		finish(player, leap.pos);
		FxEvents.send(player, "spring_move_end", leap.pos, Vec3.ZERO, 0, 0, 2);
	}

	public static void clear() {
		MOVING.clear();
		LEAPS.clear();
	}
}
