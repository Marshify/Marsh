package com.grandline.fruit.bomb;

import com.grandline.GrandLineMod;
import com.grandline.combat.MovementGuards;
import com.grandline.combat.ability.AbilityActivationHandler;
import com.grandline.combat.ability.AbilityContext;
import com.grandline.combat.ability.AbilityDef;
import com.grandline.combat.ability.AbilityHelpers;
import com.grandline.combat.ability.AttachedCharges;
import com.grandline.combat.ability.ChargeService;
import com.grandline.combat.ability.FruitStrikes;
import com.grandline.combat.ability.HealthCosts;
import com.grandline.combat.ability.KineticBody;
import com.grandline.combat.ability.TimedEffects;
import com.grandline.combat.ability.Traps;
import com.grandline.combat.fx.FxEvents;
import com.grandline.technique.CombatContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.projectile.ProjectileUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Bomb Fruit (Natural, Common): explosive projectiles, targeted detonations, a delayed attached bomb, land mines with
 * chain reactions, a health-sacrificing self-destruct and explosive propulsion. Every hit, timer, trap and health cost
 * is decided here on the server; visual beats ({@link FxEvents}, "bomb_*") drive the client's BombFx. No explosion
 * touches terrain (no vanilla explosions are created): craters, scorch and debris are client-side cosmetics.
 *
 * <p>Damage is a share of Rocket's Missile Fist ({@link #REF} = 8, before mastery and Fruit-stat scaling): Blast Toss
 * 45% tapped / 75% charged, Targeted Bomb 100%, Bomb Grab 115% (35% grab + 80% delayed), Land Mines 8 × 31.25% = 250%
 * when every explosion lands, Self Destruct 120% tapped / 220% charged, Explosive Jump 60%.
 *
 * <p>Instinct: "core" or direct hits break it ({@link CombatContext#breaking}), edge hits stay dodgeable. Land Mines
 * always break it; the Bomb Grab delayed blast does while {@link #GRAB_DELAYED_BREAKS_INSTINCT}.
 */
public final class BombMoves {
	public static final float REF = com.grandline.fruit.rocket.RocketMoves.BASE_DAMAGE;
	public static final String FRUIT_ID = "grandline:bomb";

	// --- M1: Blast Toss ---
	public static final int TOSS_MAX_CHARGE = 30;      // 1.5 s to full charge
	public static final int TOSS_TAP = 4;              // releases this short are taps
	public static final double TOSS_SPEED = 1.45;      // blocks/tick at release
	public static final double TOSS_GRAVITY = 0.03;
	public static final int TOSS_LIFE = 40;            // ≈ 30 blocks of flight
	public static final double TOSS_RADIUS_TAP = 2.5, TOSS_RADIUS_FULL = 4.0;
	static final float TOSS_DAMAGE_TAP = REF * 0.45f, TOSS_DAMAGE_FULL = REF * 0.75f;

	// --- Z: Targeted Bomb ---
	public static final double TARGET_RANGE = 18, TARGET_RADIUS = 4, TARGET_CORE = 1.6;
	public static final int TARGET_MAX_HOLD = 60;
	static final float TARGET_DAMAGE = REF;

	// --- X: Bomb Grab ---
	public static final double GRAB_SPEED = 1.9;
	public static final int GRAB_DASH_TICKS = 8;       // ≈ 15 blocks
	public static final int GRAB_HOLD = 6, GRAB_FUSE = 60; // the attached bomb goes off exactly 3 s after it is placed
	public static final boolean GRAB_DELAYED_BREAKS_INSTINCT = true; // provisional (configurable)
	static final float GRAB_INITIAL = REF * 0.35f, GRAB_DELAYED = REF * 0.80f, GRAB_GLANCE = REF * 0.15f;

	// --- C: Land Mines ---
	public static final int RING_BOMBS = 8;
	public static final double RING_RADIUS = 5, MINE_RADIUS = 2.5;
	static final float MINE_DAMAGE = REF * 0.3125f; // × 8 = 250%
	public static final int FIELD_MIN = 4, FIELD_MAX = 8, MINE_LIFE = 600, MINE_ARM = 20, MINE_LIMIT = 12, MINE_CHAIN_DELAY = 4;
	public static final double MINE_TRIGGER = 1.3, MINE_CHAIN = 4.5;

	// --- V: Self Destruct ---
	public static final int SELF_MAX = 80;             // 4 s
	public static final float SELF_HEALTH = 0.25f;     // of max health at full charge (linear)
	public static float SELF_MIN_HEALTH = 2f;          // configurable floor: the charge never takes the last heart
	public static final double SELF_RADIUS_TAP = 4, SELF_RADIUS_FULL = 10;
	static final float SELF_DAMAGE_TAP = REF * 1.2f, SELF_DAMAGE_FULL = REF * 2.2f;

	// --- F: Explosive Jump ---
	public static final double JUMP_UP = 1.25, JUMP_FORWARD = 1.75, JUMP_FORWARD_UP = 0.85, JUMP_RADIUS = 3.5;
	static final float JUMP_DAMAGE = REF * 0.6f;
	/** Recovery window 8 s; the n-th jump in a row costs 0, 6, 12, 20, 30% of max health. Provisional. */
	public static final HealthCosts.Penalty JUMP_PENALTY = new HealthCosts.Penalty(160, 0f, 0.06f, 0.12f, 0.20f, 0.30f);
	public static float JUMP_MIN_HEALTH = 1f;

	public static final AbilityDef TOSS = new AbilityDef("bomb_blast_toss", 0, 60, 0.05, ctx -> 0);
	static AbilityDef target, mines, self;

	private static final Map<UUID, Vec3> AIM = new HashMap<>();
	private static final Map<UUID, int[]> SELF = new HashMap<>(); // {held ticks, health milli-spent}
	private static final Map<UUID, Float> SELF_SPENT = new HashMap<>();
	/** Health the last Self Destruct of each player actually consumed (for tests and tuning). */
	public static final Map<UUID, Float> LAST_SACRIFICE = new HashMap<>();

	private BombMoves() {
	}

	public static List<AbilityDef> moves() {
		FruitStrikes.charged(GrandLineMod.id("bomb"), new FruitStrikes.Charged(TOSS, TOSS_MAX_CHARGE, BombMoves::tossPress, BombMoves::tossRelease));
		target = new AbilityDef("bomb_targeted", 1, 90, 0.15, AbilityDef.Activation.HELD, BombMoves::targetRelease);
		ChargeService.held(target, BombMoves::targetHeld);
		ChargeService.maxTicks(target, TARGET_MAX_HOLD);
		mines = new AbilityDef("bomb_land_mines", 75, 220, 0.50, AbilityDef.Activation.HELD, BombMoves::minesRelease);
		ChargeService.held(mines, BombMoves::minesHeld);
		self = new AbilityDef("bomb_self_destruct", 125, 450, 1.0, AbilityDef.Activation.HELD, BombMoves::selfRelease);
		ChargeService.held(self, BombMoves::selfHeld);
		ChargeService.maxTicks(self, SELF_MAX);
		return List.of(
				target,
				new AbilityDef("bomb_grab", 50, 180, 0.25, BombMoves::grab),
				mines,
				self,
				new AbilityDef("bomb_jump", 25, 120, 0.25, BombMoves::jump));
	}

	static boolean valid(ServerPlayer player, Level level) {
		return player.isAlive() && !player.isRemoved() && player.level() == level
				&& FRUIT_ID.equals(com.grandline.player.PlayerProfileService.get(player).powers().fruit());
	}

	static boolean isBomb(ServerPlayer player) {
		return FRUIT_ID.equals(com.grandline.player.PlayerProfileService.get(player).powers().fruit());
	}

	// ============================================================================================== shared explosion

	/** How an explosion treats one target. */
	enum Hit {
		/** core breaks Instinct, edge dodgeable */
		CORE_BREAKS,
		/** every hit breaks Instinct */
		ALL_BREAK
	}

	/**
	 * One explosion: every living entity (never the caster) whose body is within {@code radius} takes {@code damage}
	 * scaled from 100% at the centre to {@code edge} at the rim, once. Hits within {@code core} (or all, see
	 * {@link Hit}) break Instinct. {@code freshFrames}: this explosion is a separate hit even right after another one
	 * (each mine of a chain), so the target's invulnerability window is cleared for this hit only. Sets off the
	 * caster's mines in reach. Returns the entities hit.
	 */
	static List<LivingEntity> explode(AbilityContext ctx, Vec3 at, double radius, float damage, double core, float edge, Hit mode,
			boolean freshFrames, Set<Integer> skip, Knock knock) {
		ServerPlayer player = ctx.player();
		List<LivingEntity> hit = new ArrayList<>();
		AABB box = new AABB(at, at).inflate(radius + 2);
		for (LivingEntity e : player.serverLevel().getEntitiesOfClass(LivingEntity.class, box, e -> e != player && e.isAlive() && !e.isSpectator()
				&& !(e instanceof Player p && p.isCreative()))) {
			if (skip != null && skip.contains(e.getId())) {
				continue;
			}
			double d = distanceToBox(at, e.getBoundingBox());
			if (d > radius) {
				continue;
			}
			float k = (float) (1 - (1 - edge) * Math.min(1, d / radius));
			boolean breaks = mode == Hit.ALL_BREAK || d <= core;
			if (freshFrames) {
				e.invulnerableTime = 0;
			}
			boolean landed = breaks ? CombatContext.breaking(() -> ctx.damage(e, damage * k)) : ctx.damage(e, damage * k);
			if (landed) {
				hit.add(e);
				if (knock != null) {
					AbilityHelpers.pushResisted(e, knock.push(e, d));
				}
			}
		}
		Traps.triggerNear(player, at, radius);
		return hit;
	}

	@FunctionalInterface
	interface Knock {
		Vec3 push(LivingEntity target, double distance);
	}

	static Knock outward(Vec3 at, double strength, double lift, double radius) {
		return (e, d) -> {
			Vec3 out = e.getBoundingBox().getCenter().subtract(at).multiply(1, 0, 1);
			out = out.lengthSqr() < 1e-4 ? new Vec3(1, 0, 0) : out.normalize();
			double f = 1 - 0.5 * Math.min(1, d / radius);
			return out.scale(strength * f).add(0, lift * f, 0);
		};
	}

	static double distanceToBox(Vec3 p, AABB b) {
		double dx = Math.max(Math.max(b.minX - p.x, 0), p.x - b.maxX);
		double dy = Math.max(Math.max(b.minY - p.y, 0), p.y - b.maxY);
		double dz = Math.max(Math.max(b.minZ - p.z, 0), p.z - b.maxZ);
		return Math.sqrt(dx * dx + dy * dy + dz * dz);
	}

	/** The ground under {@code p} (within {@code depth} blocks), or null. */
	static Vec3 ground(Entity by, Vec3 p, double depth) {
		BlockHitResult hit = by.level().clip(new ClipContext(p.add(0, 0.5, 0), p.subtract(0, depth, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, by));
		return hit.getType() == HitResult.Type.MISS ? null : hit.getLocation();
	}

	// ============================================================================================== M1: Blast Toss

	static void tossPress(AbilityContext ctx) {
		ServerPlayer player = ctx.player();
		FxEvents.send(player, "bomb_toss_hold", player.position(), player.getLookAngle(), 0, TOSS_MAX_CHARGE, 0);
	}

	static void tossRelease(AbilityContext ctx, int held) {
		ServerPlayer player = ctx.player();
		ServerLevel level = player.serverLevel();
		double charge = held <= TOSS_TAP ? 0 : Math.min(1, (held - TOSS_TAP) / (double) (TOSS_MAX_CHARGE - TOSS_TAP));
		double radius = TOSS_RADIUS_TAP + (TOSS_RADIUS_FULL - TOSS_RADIUS_TAP) * charge;
		float damage = TOSS_DAMAGE_TAP + (TOSS_DAMAGE_FULL - TOSS_DAMAGE_TAP) * (float) charge;
		Vec3 look = player.getLookAngle();
		Vec3 right = new Vec3(-look.z, 0, look.x).normalize();
		Vec3[] pos = {player.getEyePosition().add(right.scale(0.35)).subtract(0, 0.15, 0)};
		// Aim at what the crosshair points at (lofted a touch for the arc), so the throw lands where the player looks.
		Vec3 aim = com.grandline.combat.ability.Projectiles.crosshairAim(player, 30);
		Vec3 dir = aim.subtract(pos[0]).normalize();
		double dist = aim.distanceTo(pos[0]);
		double ticks = dist / TOSS_SPEED;
		Vec3[] vel = {dir.scale(TOSS_SPEED).add(0, TOSS_GRAVITY * ticks * 0.5, 0)};
		int id = player.getRandom().nextInt(1 << 20);
		FxEvents.send(player, "bomb_toss", pos[0], vel[0], charge, id, 0.25 + 0.2 * charge);
		TimedEffects.start(age -> {
			if (player.level() != level || age > TOSS_LIFE) {
				FxEvents.send(player, "bomb_toss_hit", pos[0], Vec3.ZERO, charge, id, 0);
				return false;
			}
			Vec3 from = pos[0], to = from.add(vel[0]);
			BlockHitResult block = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, player));
			Vec3 end = block.getType() == HitResult.Type.MISS ? to : block.getLocation();
			EntityHitResult ent = ProjectileUtil.getEntityHitResult(level, player, from, end, new AABB(from, end).inflate(0.6),
					e -> e instanceof LivingEntity l && l.isAlive() && !e.isSpectator() && !(e instanceof Player p && p.isCreative()), 0.3f);
			if (ent != null || block.getType() != HitResult.Type.MISS) {
				Vec3 at = ent != null ? ent.getLocation() : end;
				LivingEntity direct = ent != null ? (LivingEntity) ent.getEntity() : null;
				Set<Integer> skip = new HashSet<>();
				if (direct != null) {
					skip.add(direct.getId());
					if (CombatContext.breaking(() -> ctx.damage(direct, damage))) { // direct hits break Instinct
						AbilityHelpers.pushResisted(direct, vel[0].normalize().multiply(1, 0, 1).scale(0.7 + 0.4 * charge).add(0, 0.35, 0));
					}
					at = direct.getBoundingBox().getCenter();
				}
				explode(ctx, at, radius, damage, 0, 0.55f, Hit.CORE_BREAKS, false, skip, outward(at, 0.6 + 0.4 * charge, 0.3, radius));
				Vec3 normal = block.getType() == HitResult.Type.BLOCK && ent == null ? Vec3.atLowerCornerOf(block.getDirection().getNormal()) : new Vec3(0, 1, 0);
				FxEvents.send(player, "bomb_toss_hit", at, normal, charge, id, radius);
				return false;
			}
			pos[0] = to;
			vel[0] = vel[0].scale(0.99).add(0, -TOSS_GRAVITY, 0);
			return true;
		});
	}

	// ============================================================================================== Z: Targeted Bomb

	/** The validated aim point: the look ray clipped by terrain within range, settled onto the ground below. */
	static Vec3 aimPoint(ServerPlayer player) {
		Vec3 eye = player.getEyePosition();
		Vec3 end = eye.add(player.getLookAngle().scale(TARGET_RANGE));
		BlockHitResult hit = player.serverLevel().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		Vec3 p = hit.getType() == HitResult.Type.MISS ? end : hit.getLocation().add(Vec3.atLowerCornerOf(hit.getDirection().getNormal()).scale(0.1));
		Vec3 g = ground(player, p, 6);
		return g != null ? g.add(0, 0.05, 0) : p;
	}

	static void targetHeld(AbilityContext ctx, int ticks) {
		ServerPlayer player = ctx.player();
		Vec3 want = aimPoint(player);
		Vec3 prev = AIM.get(player.getUUID());
		boolean first = prev == null || ticks <= 1;
		// The charge follows the aim smoothly (server-side), clamped to the range and never through terrain.
		Vec3 now = first ? want : prev.lerp(want, 0.45);
		if (!first && !clearLine(player, now)) {
			now = want;
		}
		AIM.put(player.getUUID(), now);
		if (first) {
			FxEvents.send(player, "bomb_target_start", now, Vec3.ZERO, 0, TARGET_RADIUS, 0);
		} else if (ticks % 2 == 0) {
			FxEvents.send(player, "bomb_target", now, Vec3.ZERO, Math.min(1, ticks / 30.0), TARGET_RADIUS, ticks);
		}
	}

	static boolean clearLine(ServerPlayer player, Vec3 p) {
		Vec3 eye = player.getEyePosition();
		if (eye.distanceTo(p) > TARGET_RANGE + 1) {
			return false;
		}
		Vec3 to = p.add(0, 0.3, 0);
		return player.serverLevel().clip(new ClipContext(eye, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)).getType()
				== HitResult.Type.MISS;
	}

	static int targetRelease(AbilityContext ctx) {
		ServerPlayer player = ctx.player();
		ServerLevel level = player.serverLevel();
		Vec3 at = AIM.remove(player.getUUID());
		if (at == null || !clearLine(player, at)) {
			at = aimPoint(player); // final server validation
		}
		Vec3 spot = at;
		FxEvents.send(player, "bomb_target_end", spot, Vec3.ZERO, 0, TARGET_RADIUS, 0);
		TimedEffects.later(2, () -> {
			if (!valid(player, level)) {
				return;
			}
			// Upward force: a vertical plume launches everyone caught; the core breaks Instinct.
			explode(ctx, spot, TARGET_RADIUS, TARGET_DAMAGE, TARGET_CORE, 0.6f, Hit.CORE_BREAKS, false, null, (e, d) -> {
				Vec3 out = e.position().subtract(spot).multiply(1, 0, 1);
				out = out.lengthSqr() < 1e-4 ? Vec3.ZERO : out.normalize().scale(0.18);
				return out.add(0, 0.95 * (1 - 0.35 * Math.min(1, d / TARGET_RADIUS)), 0);
			});
			FxEvents.send(player, "bomb_target_blast", spot, new Vec3(0, 1, 0), 0, 0, TARGET_RADIUS);
		});
		return 1;
	}

	// ============================================================================================== X: Bomb Grab

	static int grab(AbilityContext ctx) {
		ServerPlayer player = ctx.player();
		ServerLevel level = player.serverLevel();
		Vec3 look = player.getLookAngle();
		double pitch = Math.max(-0.45, Math.min(0.35, look.y));
		Vec3 flat = look.multiply(1, 0, 1).lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : look.multiply(1, 0, 1).normalize();
		Vec3 dir = flat.scale(Math.sqrt(1 - pitch * pitch)).add(0, pitch, 0).normalize();
		Vec3[] body = {player.position()};
		Set<Integer> struck = new HashSet<>();
		MovementGuards.guard(player, GRAB_DASH_TICKS + GRAB_HOLD + 40);
		FxEvents.send(player, "bomb_grab_launch", player.position().add(0, 0.9, 0), dir, GRAB_SPEED, 0, 0);
		explode(ctx, player.position().subtract(dir.scale(0.8)), 1.8, GRAB_GLANCE, 0, 0.5f, Hit.CORE_BREAKS, false, null, null);
		TimedEffects.start(age -> {
			if (!valid(player, level)) {
				MovementGuards.release(player);
				return false;
			}
			if (age > GRAB_DASH_TICKS) {
				end(player, dir);
				return false;
			}
			Vec3 from = body[0];
			KineticBody.Step step = KineticBody.sweep(player, from, dir.scale(GRAB_SPEED));
			body[0] = step.pos();
			LivingEntity caught = null;
			for (LivingEntity e : KineticBody.touched(player, from, step.pos(), 0.45, struck)) {
				Vec3 a = player.getBoundingBox().move(from.subtract(player.position())).getCenter();
				Vec3 b = player.getBoundingBox().move(step.pos().subtract(player.position())).getCenter();
				boolean direct = KineticBody.distanceToSegment(e.getBoundingBox().getCenter(), a, b) <= e.getBbWidth() / 2 + 0.35;
				if (direct && caught == null) {
					caught = e;
				} else if (ctx.damage(e, GRAB_GLANCE)) { // glancing: minor, no grab
					Vec3 side = e.position().subtract(step.pos()).multiply(1, 0, 1);
					AbilityHelpers.pushResisted(e, (side.lengthSqr() < 1e-4 ? dir : side.normalize()).scale(0.4).add(0, 0.2, 0));
					FxEvents.send(player, "bomb_grab_glance", e.getBoundingBox().getCenter(), dir, 0, 0, 0);
				}
			}
			drive(player, body[0]);
			if (caught != null) {
				seize(ctx, caught, dir);
				return false;
			}
			if (step.normal() != null && step.moved() < GRAB_SPEED * 0.3) {
				end(player, dir); // hit a wall
				return false;
			}
			return true;
		});
		return 1;
	}

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

	static void end(ServerPlayer player, Vec3 dir) {
		player.setDeltaMovement(dir.scale(0.3));
		player.hurtMarked = true;
		MovementGuards.release(player);
		FxEvents.send(player, "bomb_grab_end", player.position(), dir, 0, 0, 0);
	}

	/** A direct hit: grab, small blast (dodgeable), bomb on the head, release; the bomb goes off 3 s later. */
	static void seize(AbilityContext ctx, LivingEntity target, Vec3 dir) {
		ServerPlayer player = ctx.player();
		ServerLevel level = player.serverLevel();
		player.setDeltaMovement(Vec3.ZERO);
		player.hurtMarked = true;
		boolean landed = ctx.damage(target, GRAB_INITIAL);
		if (!landed && com.grandline.technique.Dodges.justEvaded(target)) {
			end(player, dir); // Instinct slipped the grab: no bomb
			return;
		}
		boolean holdable = AbilityHelpers.ccScale(target) > 0.35; // bosses that resist displacement are not carried
		FxEvents.send(player, "bomb_grab_hit", target.getBoundingBox().getCenter(), dir, 0, target.getId(), holdable ? 1 : 0);
		TimedEffects.start(age -> {
			if (!valid(player, level) || !target.isAlive()) {
				MovementGuards.release(player);
				return false;
			}
			if (holdable && age < GRAB_HOLD) {
				Vec3 front = player.position().add(dir.multiply(1, 0, 1).normalize().scale(1.1 + target.getBbWidth() / 2));
				target.teleportTo(front.x, target.getY(), front.z);
				target.setDeltaMovement(Vec3.ZERO);
				target.hurtMarked = true;
			}
			if (age == 3) {
				if (AttachedCharges.attach(player, "bomb_grab", target, GRAB_FUSE, new DelayedBomb(ctx))) {
					FxEvents.send(player, "bomb_attach", target.getEyePosition(), dir, GRAB_FUSE, target.getId(), target.getBbHeight());
				}
			}
			if (age >= GRAB_HOLD) {
				AbilityHelpers.pushResisted(target, dir.multiply(1, 0, 1).normalize().scale(0.55).add(0, 0.25, 0));
				player.setDeltaMovement(dir.scale(-0.35).add(0, 0.2, 0));
				player.hurtMarked = true;
				MovementGuards.release(player);
				FxEvents.send(player, "bomb_grab_end", player.position(), dir, 0, 0, 1);
				return false;
			}
			return true;
		});
	}

	/** The attached bomb: blasts its carrier toward the caster's position at that moment. */
	record DelayedBomb(AbilityContext ctx) implements AttachedCharges.Fuse {
		@Override
		public void detonate(ServerPlayer owner, LivingEntity target) {
			Vec3 at = target.getEyePosition();
			Vec3 toward = owner.position().subtract(target.position()).multiply(1, 0, 1);
			toward = toward.lengthSqr() < 1e-4 ? Vec3.ZERO : toward.normalize();
			Vec3 push = toward.scale(1.1).add(0, 0.45, 0);
			target.invulnerableTime = 0; // a separate hit from the grab, three seconds apart
			boolean landed = GRAB_DELAYED_BREAKS_INSTINCT ? CombatContext.breaking(() -> ctx.damage(target, GRAB_DELAYED)) : ctx.damage(target, GRAB_DELAYED);
			if (landed) {
				AbilityHelpers.pushResisted(target, push);
			}
			Traps.triggerNear(owner, at, 2.5);
			FxEvents.send(owner, "bomb_attach_blast", at, toward, 0, target.getId(), 2.5);
		}

		@Override
		public void lost(ServerPlayer owner, Vec3 lastPos, int targetId) {
			if (owner != null) {
				FxEvents.send(owner, "bomb_attach_lost", lastPos.add(0, 1.5, 0), Vec3.ZERO, 0, targetId, 0);
			}
		}
	}

	// ============================================================================================== C: Land Mines

	static void minesHeld(AbilityContext ctx, int ticks) {
		if (ticks <= 1) {
			FxEvents.send(ctx.player(), "bomb_mines_hold", ctx.player().position(), ctx.player().getLookAngle(), 0, 0, 0);
		}
	}

	static int minesRelease(AbilityContext ctx) {
		ServerPlayer player = ctx.player();
		// Recasting sets off the mines already down (one chain), then the new cast happens.
		int old = Traps.detonateOwned(player, "bomb_mine");
		if (ctx.charge() <= 0) {
			ring(ctx);
		} else {
			field(ctx, FIELD_MIN + (int) Math.round((FIELD_MAX - FIELD_MIN) * ctx.charge()));
		}
		return 1 + old;
	}

	/** Tapped: eight bombs thrown out in a ring, each its own explosion going off in quick succession. */
	static void ring(AbilityContext ctx) {
		ServerPlayer player = ctx.player();
		ServerLevel level = player.serverLevel();
		Vec3 centre = player.position();
		double start = player.getRandom().nextDouble() * Math.PI * 2;
		FxEvents.send(player, "bomb_ring", centre.add(0, 1, 0), Vec3.ZERO, RING_BOMBS, start, RING_RADIUS);
		for (int i = 0; i < RING_BOMBS; i++) {
			double a = start + i * Math.PI * 2 / RING_BOMBS;
			Vec3 p = centre.add(Math.cos(a) * RING_RADIUS, 0.6, Math.sin(a) * RING_RADIUS);
			Vec3 g = ground(player, p.add(0, 1, 0), 4);
			Vec3 spot = g != null ? g.add(0, 0.2, 0) : p;
			int delay = 7 + i * 2;
			TimedEffects.later(delay, () -> {
				if (!valid(player, level)) {
					return;
				}
				explode(ctx, spot, MINE_RADIUS, MINE_DAMAGE, MINE_RADIUS, 0.85f, Hit.ALL_BREAK, true, null, outward(centre, 0.55, 0.3, RING_RADIUS + MINE_RADIUS));
				FxEvents.send(player, "bomb_mine_blast", spot, new Vec3(0, 1, 0), 0, -1, MINE_RADIUS);
			});
		}
	}

	static final Traps.Config MINE = new Traps.Config("bomb_mine", MINE_ARM, MINE_LIFE, MINE_TRIGGER, MINE_CHAIN, MINE_CHAIN_DELAY, MINE_LIMIT,
			(owner, trap) -> isBomb(owner),
			(owner, e) -> !(e instanceof Player p && (p.isCreative() || p == owner)) && !(e instanceof net.minecraft.world.entity.decoration.ArmorStand),
			new Traps.Listener() {
				@Override
				public void detonate(Traps.Trap trap, ServerPlayer owner, Traps.Cause cause) {
					AbilityContext ctx = contextFor(owner);
					if (ctx != null) {
						explode(ctx, trap.pos.add(0, 0.2, 0), MINE_RADIUS, MINE_DAMAGE, MINE_RADIUS, 0.85f, Hit.ALL_BREAK, true, null,
								outward(trap.pos, 0.5, 0.45, MINE_RADIUS));
					}
					FxEvents.send(owner, "bomb_mine_blast", trap.pos, trap.normal, cause.ordinal(), trap.id, MINE_RADIUS);
				}

				@Override
				public void removed(Traps.Trap trap, Traps.Removal why) {
					// From the owner when online (so their client always hears it), else anyone in that world (matched by id).
					ServerPlayer owner = trap.level.getServer().getPlayerList().getPlayer(trap.owner);
					if (owner != null) {
						FxEvents.send(owner, "bomb_mine_gone", trap.pos, Vec3.ZERO, why.ordinal(), trap.id, 0);
						return;
					}
					List<ServerPlayer> around = trap.level.players();
					if (!around.isEmpty()) {
						FxEvents.sendFrom(around.get(0), "bomb_mine_gone", trap.pos, Vec3.ZERO, why.ordinal(), trap.id, 0);
					}
				}
			});

	/** A fresh ability context for a player (traps that outlive the cast use the owner's current stats). */
	static AbilityContext contextFor(ServerPlayer owner) {
		var profile = com.grandline.player.PlayerProfileService.get(owner);
		var power = com.grandline.combat.ability.Powers.get(profile.powers().fruit());
		if (power == null || mines == null) {
			return null;
		}
		double m = com.grandline.player.PlayerProfileService.statRules().abilityMultiplier(profile.stats(), com.grandline.player.StatType.FRUIT);
		return new AbilityContext(owner, power, mines, m);
	}

	/** Held: a field of {@code count} mines on the ground around the caster. */
	static void field(AbilityContext ctx, int count) {
		ServerPlayer player = ctx.player();
		ServerLevel level = player.serverLevel();
		Vec3 centre = player.position();
		FxEvents.send(player, "bomb_mines_deploy", centre, player.getLookAngle(), count, 0, 0);
		double start = player.getRandom().nextDouble() * Math.PI * 2;
		int placed = 0;
		for (int i = 0; i < count * 3 && placed < count; i++) {
			double a = start + placed * Math.PI * 2 / count + (player.getRandom().nextDouble() - 0.5) * 0.5;
			double r = 2.5 + player.getRandom().nextDouble() * 4;
			Vec3 p = centre.add(Math.cos(a) * r, 1.5, Math.sin(a) * r);
			Vec3 g = ground(player, p, 5);
			if (g == null || !level.getBlockState(BlockPos.containing(g.x, g.y - 0.05, g.z)).isFaceSturdy(level, BlockPos.containing(g.x, g.y - 0.05, g.z), Direction.UP)
					|| !level.getFluidState(BlockPos.containing(g)).isEmpty()) {
				continue;
			}
			Traps.Trap trap = Traps.place(player, g, new Vec3(0, 1, 0), MINE);
			int delay = 2 + placed * 2;
			placed++;
			FxEvents.send(player, "bomb_mine", g, centre.add(0, 1.2, 0), delay, trap.id, MINE_LIFE);
		}
	}

	// ============================================================================================== V: Self Destruct

	static void selfHeld(AbilityContext ctx, int ticks) {
		ServerPlayer player = ctx.player();
		UUID id = player.getUUID();
		if (ticks <= 1 || !SELF.containsKey(id)) {
			SELF.put(id, new int[]{0});
			SELF_SPENT.put(id, 0f);
			FxEvents.send(player, "bomb_head_start", player.position(), Vec3.ZERO, 0, SELF_MAX, 0);
		}
		int t = Math.min(SELF_MAX, ticks);
		SELF.get(id)[0] = t;
		// Stationary: no walking, no jumping (falling still works).
		Vec3 v = player.getDeltaMovement();
		player.setDeltaMovement(0, Math.min(0, v.y), 0);
		player.hurtMarked = true;
		player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 3, 6, false, false));
		// Health sacrifice: linear to 25% of MAX health at 4 s, taken directly (armor/Aura/immunity cannot reduce it).
		float wanted = player.getMaxHealth() * SELF_HEALTH * t / SELF_MAX;
		float spent = SELF_SPENT.getOrDefault(id, 0f);
		float took = HealthCosts.spend(player, wanted - spent, SELF_MIN_HEALTH);
		SELF_SPENT.put(id, spent + took);
		boolean starved = wanted - (spent + took) > 0.05f;
		if (ticks % 2 == 0) {
			FxEvents.send(player, "bomb_head", player.position(), Vec3.ZERO, t / (double) SELF_MAX, SELF_MAX, 0);
		}
		if (t == SELF_MAX - 1) {
			FxEvents.send(player, "bomb_head_max", player.position(), Vec3.ZERO, 1, SELF_MAX, 0);
		}
		if (starved) {
			// Not enough health to keep charging: it goes off now, at the charge actually paid for.
			AbilityActivationHandler.releaseCharge(player, 3);
		}
	}

	static int selfRelease(AbilityContext ctx) {
		ServerPlayer player = ctx.player();
		int[] st = SELF.remove(player.getUUID());
		Float paid = SELF_SPENT.remove(player.getUUID());
		LAST_SACRIFICE.put(player.getUUID(), paid == null ? 0 : paid);
		int t = st == null ? 0 : st[0];
		double c = t / (double) SELF_MAX;
		double radius = SELF_RADIUS_TAP + (SELF_RADIUS_FULL - SELF_RADIUS_TAP) * c;
		float damage = SELF_DAMAGE_TAP + (SELF_DAMAGE_FULL - SELF_DAMAGE_TAP) * (float) c;
		Vec3 at = player.position().add(0, 1.4, 0);
		MovementGuards.guard(player, 30);
		explode(ctx, at, radius, damage, radius * 0.6, 0.45f, Hit.CORE_BREAKS, false, null, outward(at, 0.9 + 1.3 * c, 0.45 + 0.45 * c, radius));
		FxEvents.send(player, "bomb_self_destruct", at, new Vec3(0, 1, 0), c, 0, radius);
		return 1;
	}

	// ============================================================================================== F: Explosive Jump

	static int jump(AbilityContext ctx) {
		ServerPlayer player = ctx.player();
		Vec3 feet = player.position();
		// Moving or standing (server-side position change since last tick).
		Vec3 moved = new Vec3(player.getX() - player.xo, 0, player.getZ() - player.zo);
		boolean moving = moved.length() > 0.06;
		Vec3 flat = player.getLookAngle().multiply(1, 0, 1);
		flat = flat.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : flat.normalize();
		Vec3 heading = moving ? moved.normalize().scale(0.6).add(flat.scale(0.4)).normalize() : flat;
		Vec3 launch = moving ? heading.scale(JUMP_FORWARD).add(0, JUMP_FORWARD_UP, 0) : new Vec3(0, JUMP_UP, 0);
		// Repeated-use self-damage: free after recovery, escalating within the window (separate from the cooldown).
		float cost = JUMP_PENALTY.use(player);
		if (cost > 0) {
			HealthCosts.spend(player, player.getMaxHealth() * cost, JUMP_MIN_HEALTH);
			FxEvents.send(player, "bomb_jump_hurt", player.position().add(0, 1, 0), Vec3.ZERO, cost, 0, 0);
		}
		player.setDeltaMovement(launch);
		player.hurtMarked = true;
		player.fallDistance = 0;
		MovementGuards.guard(player, 100);
		explode(ctx, feet.add(0, 0.3, 0), JUMP_RADIUS, JUMP_DAMAGE, JUMP_RADIUS * 0.6, 0.5f, Hit.CORE_BREAKS, false, null, outward(feet, 0.6, 0.4, JUMP_RADIUS));
		FxEvents.send(player, "bomb_jump", feet, launch.normalize(), moving ? 1 : 0, 0, JUMP_RADIUS);
		heat(player);
		ServerLevel level = player.serverLevel();
		TimedEffects.start(age -> {
			if (!player.isAlive() || player.level() != level || age > 100) {
				return false;
			}
			if (age > 4 && (player.onGround() || player.isInWater())) {
				MovementGuards.release(player);
				FxEvents.send(player, "bomb_jump_land", player.position(), Vec3.ZERO, 0, 0, 0);
				return false;
			}
			return true;
		});
		return 1;
	}

	/** Tells the caster's HUD how long until Explosive Jump is safe again (red name until then). */
	static void heat(ServerPlayer player) {
		FxEvents.send(player, "bomb_jump_heat", player.position(), Vec3.ZERO, JUMP_PENALTY.remaining(player), JUMP_PENALTY.stage(player),
				JUMP_PENALTY.nextCost(player));
	}
}
