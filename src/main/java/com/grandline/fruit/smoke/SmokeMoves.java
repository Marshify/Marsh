package com.grandline.fruit.smoke;

import com.grandline.combat.ElementalReflex;
import com.grandline.combat.MovementGuards;
import com.grandline.combat.ability.AbilityContext;
import com.grandline.combat.ability.AbilityCooldownService;
import com.grandline.combat.ability.AbilityDef;
import com.grandline.combat.ability.AbilityHelpers;
import com.grandline.combat.ability.ChargeService;
import com.grandline.combat.ability.DamageZones;
import com.grandline.combat.ability.FlightService;
import com.grandline.combat.ability.Projectiles;
import com.grandline.combat.ability.TimedEffects;
import com.grandline.combat.fx.FxEvents;
import com.grandline.technique.CombatContext;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Smoke Fruit (Elemental, Common): Smoke Slam, Smoke Blast (a lingering cloud of smoke slashes), Smoke Liberation
 * (an expanding smoke vortex) and Smoke Bomber (slow sustained flight that keeps dropping smoke bombs), plus the
 * Elemental Reflex passive (registered with {@link ElementalReflex}). Everything that matters is decided on the
 * server; "smoke_*" beats drive the client's SmokeFx. No terrain is changed.
 *
 * <p>Damage is a share of Missile Fist ({@link #REF} = 8): Smoke Slam 100%, Smoke Blast 5 × 40% = 200% if the target
 * stays in the cloud, Smoke Liberation 3 × 50% = 150%, Smoke Bomber 30% per bomb.
 */
public final class SmokeMoves {
	public static final float REF = com.grandline.fruit.rocket.RocketMoves.BASE_DAMAGE;
	public static final String FRUIT_ID = "grandline:smoke";

	// --- Z: Smoke Slam ---
	public static final double SLAM_RANGE = 6, SLAM_RADIUS = 3.5, SLAM_CORE = 1.4;
	public static final int SLAM_HIT = 6;
	static final float SLAM_DAMAGE = REF;

	// --- X: Smoke Blast ---
	public static double BLAST_SPEED = 1.5, BLAST_RANGE = 25, BLAST_RADIUS = 4, BLAST_HEIGHT = 3;
	public static int BLAST_DURATION = 40, BLAST_INTERVAL = 8; // 2 s cloud, a slash every 0.4 s (5 pulses)
	public static boolean BLAST_DIRECT_BREAKS = true, BLAST_CLOUD_BREAKS = false;
	static final float BLAST_PULSE = REF * 0.40f;

	// --- C: Smoke Liberation ---
	public static final double LIB_RADIUS = 7, LIB_HEIGHT = 10, LIB_CLOSE = 3;
	public static final int LIB_EXPAND = 8, LIB_TICKS = 30, LIB_SECOND = 15, LIB_FINAL = 27, LIB_STUN = 30;
	static final float LIB_HIT = REF * 0.50f;

	// --- F: Smoke Bomber ---
	/** Vanilla flying speed for about 5 blocks/second (vanilla 0.05 ≈ 10.9 blocks/s). */
	public static final float BOMBER_FLY_SPEED = 0.023f;
	public static final int BOMBER_INTERVAL = 15, BOMB_LIFE = 60;
	public static final double BOMB_RADIUS = 2.5, BOMB_GRAVITY = 0.05;
	static final float BOMB_DAMAGE = REF * 0.30f;

	static AbilityDef bomber;
	private static final Map<UUID, Float> BOMBER_HEALTH = new HashMap<>();

	private SmokeMoves() {
	}

	public static List<AbilityDef> moves() {
		ElementalReflex.register(new ElementalReflex.Profile(FRUIT_ID, p -> true, (p, s) -> false, ElementalReflex.beat("smoke_reflex")));
		bomber = new AbilityDef("smoke_bomber", 10, 90, 0.50, AbilityDef.Activation.HELD, SmokeMoves::bomberEnd);
		ChargeService.held(bomber, SmokeMoves::bomberHeld);
		ChargeService.maxTicks(bomber, Integer.MAX_VALUE / 4); // no time limit: it lasts while F is held
		return List.of(
				new AbilityDef("smoke_slam", 1, 105, 0.15, SmokeMoves::slam),
				new AbilityDef("smoke_blast", 20, 180, 0.25, SmokeMoves::blast),
				new AbilityDef("smoke_liberation", 50, 270, 0.50, SmokeMoves::liberation),
				AbilityDef.empty(),
				bomber);
	}

	static boolean valid(ServerPlayer player, ServerLevel level) {
		return player.isAlive() && !player.isRemoved() && player.level() == level
				&& FRUIT_ID.equals(com.grandline.player.PlayerProfileService.get(player).powers().fruit());
	}

	static boolean hit(AbilityContext ctx, LivingEntity target, float damage, boolean breaks, boolean fresh) {
		if (fresh) {
			target.invulnerableTime = 0; // a separate hit of a multi-hit move (only this target, only now)
		}
		return breaks ? CombatContext.breaking(() -> ctx.damage(target, damage)) : ctx.damage(target, damage);
	}

	static boolean clear(ServerPlayer player, Vec3 from, LivingEntity to) {
		return player.serverLevel().clip(new ClipContext(from, to.getBoundingBox().getCenter(), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player))
				.getType() == HitResult.Type.MISS;
	}

	static Vec3 ground(ServerPlayer player, Vec3 p, double depth) {
		BlockHitResult hit = player.serverLevel().clip(new ClipContext(p.add(0, 0.5, 0), p.subtract(0, depth, 0), ClipContext.Block.COLLIDER,
				ClipContext.Fluid.NONE, player));
		return hit.getType() == HitResult.Type.MISS ? p : hit.getLocation();
	}

	// ============================================================================================== Z: Smoke Slam

	static int slam(AbilityContext ctx) {
		ServerPlayer player = ctx.player();
		ServerLevel level = player.serverLevel();
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getLookAngle();
		Vec3 flat = look.multiply(1, 0, 1).lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : look.multiply(1, 0, 1).normalize();
		Vec3 dir = flat.add(0, Math.min(0, look.y), 0).normalize();
		BlockHitResult wall = level.clip(new ClipContext(eye, eye.add(dir.scale(SLAM_RANGE)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		Vec3 reach = wall.getType() == HitResult.Type.MISS ? eye.add(dir.scale(SLAM_RANGE)) : wall.getLocation().subtract(dir.scale(0.3));
		Vec3 at = ground(player, reach, 5).add(0, 0.05, 0);
		FxEvents.send(player, "smoke_slam", player.position(), at.subtract(player.position()), SLAM_HIT, 0, SLAM_RADIUS);
		TimedEffects.later(SLAM_HIT, () -> {
			if (!valid(player, level)) {
				return;
			}
			Vec3 from = at.add(0, 0.6, 0);
			for (LivingEntity e : DamageZones.inside(player, at, SLAM_RADIUS, 3)) {
				if (!clear(player, from, e)) {
					continue; // terrain shields
				}
				double d = Math.hypot(e.getX() - at.x, e.getZ() - at.z);
				float k = (float) (1 - 0.4 * Math.min(1, d / SLAM_RADIUS));
				if (hit(ctx, e, SLAM_DAMAGE * k, d <= SLAM_CORE, false)) {
					AbilityHelpers.pushResisted(e, new Vec3(0, 0.85 * k + 0.15, 0));
					AbilityHelpers.stagger(e, 20, 1);
				}
			}
			FxEvents.send(player, "smoke_slam_impact", at, new Vec3(0, -1, 0), 0, 0, SLAM_RADIUS);
		});
		return 1;
	}

	// ============================================================================================== X: Smoke Blast

	static int blast(AbilityContext ctx) {
		ServerPlayer player = ctx.player();
		ServerLevel level = player.serverLevel();
		Vec3 look = player.getLookAngle();
		Vec3 right = new Vec3(-look.z, 0, look.x).normalize();
		Vec3[] pos = {player.getEyePosition().add(right.scale(0.35)).subtract(0, 0.2, 0)};
		Vec3 dir = Projectiles.crosshairAim(player, BLAST_RANGE).subtract(pos[0]).normalize();
		int id = player.getRandom().nextInt(1 << 20);
		FxEvents.send(player, "smoke_blast", pos[0], dir, BLAST_SPEED, id, 0);
		int life = (int) Math.ceil(BLAST_RANGE / BLAST_SPEED);
		TimedEffects.start(age -> {
			if (!valid(player, level)) {
				FxEvents.send(player, "smoke_blast_end", pos[0], dir, 0, id, 0);
				return false;
			}
			Vec3 from = pos[0], to = from.add(dir.scale(BLAST_SPEED));
			BlockHitResult block = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
			Vec3 end = block.getType() == HitResult.Type.MISS ? to : block.getLocation();
			EntityHitResult ent = ProjectileUtil.getEntityHitResult(level, player, from, end, new AABB(from, end).inflate(0.7),
					e -> e instanceof LivingEntity l && l.isAlive() && !e.isSpectator() && !(e instanceof Player p && p.isCreative()), 0.4f);
			boolean last = age >= life;
			if (ent != null || block.getType() != HitResult.Type.MISS || last) {
				LivingEntity direct = ent != null ? (LivingEntity) ent.getEntity() : null;
				Vec3 at = direct != null ? direct.position() : ground(player, end, 3);
				cloud(ctx, at, direct);
				FxEvents.send(player, "smoke_blast_end", at, dir, 1, id, BLAST_RADIUS);
				return false;
			}
			pos[0] = to;
			return true;
		});
		return 1;
	}

	/** The lingering cloud: stays where it formed; a smoke slash cuts everyone inside every interval. */
	static void cloud(AbilityContext ctx, Vec3 at, LivingEntity direct) {
		ServerPlayer player = ctx.player();
		FxEvents.send(player, "smoke_cloud", at, Vec3.ZERO, BLAST_DURATION, BLAST_INTERVAL, BLAST_RADIUS);
		DamageZones.start(player, at, BLAST_RADIUS, BLAST_HEIGHT, BLAST_DURATION, BLAST_INTERVAL, (index, inside) -> {
			int n = 0;
			for (LivingEntity e : inside) {
				boolean breaks = e == direct && index == 0 ? BLAST_DIRECT_BREAKS : BLAST_CLOUD_BREAKS;
				if (hit(ctx, e, BLAST_PULSE, breaks, true)) {
					AbilityHelpers.stagger(e, 6, 1);
					if (n++ < 4) {
						FxEvents.send(player, "smoke_slash", e.getBoundingBox().getCenter(), Vec3.ZERO, index, 0, 0);
					}
				}
			}
			FxEvents.send(player, "smoke_cloud_pulse", at, Vec3.ZERO, index, 0, BLAST_RADIUS);
		});
	}

	// ============================================================================================== C: Smoke Liberation

	static int liberation(AbilityContext ctx) {
		ServerPlayer player = ctx.player();
		ServerLevel level = player.serverLevel();
		Vec3 look = player.getLookAngle().multiply(1, 0, 1);
		Vec3 flat = look.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : look.normalize();
		Vec3 centre = ground(player, player.position().add(flat.scale(1.5)).add(0, 1, 0), 3);
		FxEvents.send(player, "smoke_liberation", centre, Vec3.ZERO, LIB_TICKS, LIB_HEIGHT, LIB_RADIUS);
		Map<Integer, Integer> hits = new HashMap<>();
		TimedEffects.start(age -> {
			if (!valid(player, level) || age > LIB_TICKS) {
				return false;
			}
			double r = LIB_RADIUS * (1 - Math.pow(1 - Math.min(1, age / (double) LIB_EXPAND), 3));
			for (LivingEntity e : DamageZones.inside(player, centre, r, LIB_HEIGHT)) {
				double d = Math.hypot(e.getX() - centre.x, e.getZ() - centre.z);
				int n = hits.getOrDefault(e.getId(), 0);
				boolean due = n == 0 || n == 1 && age >= LIB_SECOND || n == 2 && age >= LIB_FINAL;
				if (!due) {
					continue;
				}
				hits.put(e.getId(), n + 1);
				// The close initial impact breaks Instinct; later and outer hits can be dodged.
				if (hit(ctx, e, LIB_HIT, n == 0 && d <= LIB_CLOSE, true)) {
					if (n == 0) {
						stun(e, LIB_STUN);
					}
					Vec3 out = e.position().subtract(centre).multiply(1, 0, 1);
					out = out.lengthSqr() < 1e-4 ? flat : out.normalize();
					Vec3 swirl = new Vec3(-out.z, 0, out.x).scale(0.25);
					AbilityHelpers.pushResisted(e, n == 2 ? out.scale(0.55).add(0, 0.35, 0) : swirl.add(0, 0.12, 0));
					FxEvents.send(player, "smoke_liberation_hit", e.getBoundingBox().getCenter(), out, n, 0, 0);
				}
			}
			return true;
		});
		return 1;
	}

	/** A short stun (heavy slow + no jumping), shorter on crowd-control-resistant targets, never stacking. */
	static void stun(LivingEntity e, int ticks) {
		int t = (int) Math.round(ticks * AbilityHelpers.ccScale(e));
		if (t <= 0 || com.grandline.technique.Dodges.justEvaded(e)) {
			return;
		}
		e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, t, 6, false, false));
		e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, t, 1, false, false));
		e.setDeltaMovement(e.getDeltaMovement().multiply(0.2, 1, 0.2));
		e.hurtMarked = true;
	}

	// ============================================================================================== F: Smoke Bomber

	static void bomberHeld(AbilityContext ctx, int ticks) {
		ServerPlayer player = ctx.player();
		if (!FlightService.isFlying(player)) {
			if (ticks > 2) {
				return; // flight already ended (interrupted); the hold winds down
			}
			if (AbilityHelpers.inWater(player)) {
				interrupt(player, "water");
				return;
			}
			BOMBER_HEALTH.put(player.getUUID(), player.getHealth());
			FxEvents.send(player, "smoke_bomber_start", player.position(), Vec3.ZERO, 0, 0, 0);
			FlightService.start(player, ctx.power(), ctx.ability(), Integer.MAX_VALUE, 0, BOMBER_FLY_SPEED, (flier, t) -> bomberTick(flier));
			MovementGuards.guard(player, 40);
		}
		MovementGuards.guard(player, 10);
		if (ticks % BOMBER_INTERVAL == BOMBER_INTERVAL / 2 && FlightService.isFlying(player)) {
			drop(ctx);
		}
	}

	/** While flying: water ends it, and so does any damage that actually landed (a negated hit is not damage). */
	static void bomberTick(ServerPlayer player) {
		if (AbilityHelpers.inWater(player)) {
			interrupt(player, "water");
			return;
		}
		Float last = BOMBER_HEALTH.get(player.getUUID());
		float now = player.getHealth();
		if (last != null && now < last - 0.01f) {
			interrupt(player, "hit");
			return;
		}
		BOMBER_HEALTH.put(player.getUUID(), now);
		if (player.server.getTickCount() % 10 == 0) {
			FxEvents.send(player, "smoke_bomber", player.position(), player.getDeltaMovement(), 0, 0, 0);
		}
	}

	static void interrupt(ServerPlayer player, String why) {
		boolean wasHolding = ChargeService.isCharging(player);
		ChargeService.cancel(player);
		FlightService.stop(player, false);
		BOMBER_HEALTH.remove(player.getUUID());
		MovementGuards.release(player);
		var power = com.grandline.combat.ability.Powers.get(com.grandline.player.PlayerProfileService.get(player).powers().fruit());
		if (wasHolding && power != null && bomber != null) {
			AbilityCooldownService.start(player, power.abilityId(bomber), bomber.cooldownTicks());
		}
		FxEvents.send(player, "smoke_bomber_end", player.position(), Vec3.ZERO, 1, 0, 0);
	}

	/** Releasing F lands the smoke body (the cooldown starts now). */
	static int bomberEnd(AbilityContext ctx) {
		ServerPlayer player = ctx.player();
		if (FlightService.isFlying(player)) {
			FlightService.stop(player, false);
		}
		BOMBER_HEALTH.remove(player.getUUID());
		MovementGuards.release(player);
		FxEvents.send(player, "smoke_bomber_end", player.position(), Vec3.ZERO, 0, 0, 0);
		return 1;
	}

	/** One falling smoke bomb: gravity, swept collision, smoke detonation. */
	static void drop(AbilityContext ctx) {
		ServerPlayer player = ctx.player();
		ServerLevel level = player.serverLevel();
		Vec3[] pos = {player.position().add(0, 0.3, 0)};
		Vec3 v = player.getDeltaMovement();
		Vec3[] vel = {new Vec3(v.x * 0.6, Math.min(0, v.y) - 0.15, v.z * 0.6)};
		int id = player.getRandom().nextInt(1 << 20);
		FxEvents.send(player, "smoke_bomb", pos[0], vel[0], BOMB_GRAVITY, id, 0);
		Set<Integer> skip = new HashSet<>();
		TimedEffects.start(age -> {
			if (player.level() != level || !player.isAlive()) {
				FxEvents.send(player, "smoke_bomb_hit", pos[0], Vec3.ZERO, 0, id, 0);
				return false;
			}
			Vec3 from = pos[0], to = from.add(vel[0]);
			BlockHitResult block = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, player));
			Vec3 end = block.getType() == HitResult.Type.MISS ? to : block.getLocation();
			EntityHitResult ent = ProjectileUtil.getEntityHitResult(level, player, from, end, new AABB(from, end).inflate(0.5),
					e -> e instanceof LivingEntity l && l.isAlive() && !e.isSpectator() && !(e instanceof Player p && p.isCreative()), 0.3f);
			if (ent != null || block.getType() != HitResult.Type.MISS || age >= BOMB_LIFE) {
				Vec3 at = ent != null ? ent.getLocation() : end;
				if (age < BOMB_LIFE || ent != null) {
					LivingEntity direct = ent != null ? (LivingEntity) ent.getEntity() : null;
					if (direct != null && hit(ctx, direct, BOMB_DAMAGE, true, false)) { // direct impacts break Instinct
						skip.add(direct.getId());
						AbilityHelpers.stagger(direct, 10, 1);
					}
					for (LivingEntity e : DamageZones.inside(player, at.subtract(0, 0.5, 0), BOMB_RADIUS, 2.5)) {
						if (skip.contains(e.getId())) {
							continue;
						}
						double d = e.position().distanceTo(at);
						if (hit(ctx, e, BOMB_DAMAGE * (float) (1 - 0.35 * Math.min(1, d / BOMB_RADIUS)), false, false)) {
							Vec3 out = e.position().subtract(at).multiply(1, 0, 1);
							AbilityHelpers.pushResisted(e, (out.lengthSqr() < 1e-4 ? Vec3.ZERO : out.normalize().scale(0.3)).add(0, 0.15, 0));
							AbilityHelpers.stagger(e, 8, 0);
						}
					}
				}
				FxEvents.send(player, "smoke_bomb_hit", at, Vec3.ZERO, 0, id, age < BOMB_LIFE || ent != null ? BOMB_RADIUS : 0);
				return false;
			}
			pos[0] = to;
			vel[0] = vel[0].scale(0.98).add(0, -BOMB_GRAVITY, 0);
			return true;
		});
	}
}
