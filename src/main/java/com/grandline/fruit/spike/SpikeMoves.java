package com.grandline.fruit.spike;

import com.grandline.combat.MovementGuards;
import com.grandline.combat.ability.AbilityContext;
import com.grandline.combat.ability.AbilityDef;
import com.grandline.combat.ability.AbilityHelpers;
import com.grandline.combat.ability.ChargeService;
import com.grandline.combat.ability.GroundSpikes;
import com.grandline.combat.ability.KineticBody;
import com.grandline.combat.ability.TimedEffects;
import com.grandline.combat.fx.FxEvents;
import com.grandline.technique.CombatContext;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
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
 * Spike Fruit (Natural, Common): Spike Summon (targeted ground spikes, tap or charge up to six), Whirlwind (spiked
 * arms spinning while rising, tap or hold up to 3 s), Spiky Ball (a 3 s rolling spiked-ball transformation that drags
 * enemies) and Spike Barrage (a steerable row of ever larger spikes, tap or charge up to 3 s). No M1, no F, no passive.
 * Every spike is a {@link GroundSpikes} eruption with its own validated position and cone hitbox; nothing is placed in
 * the world. "spike_*" beats drive the client's SpikeFx.
 *
 * <p>Damage is a share of Missile Fist ({@link #REF} = 8): Spike Summon 100% tapped (central spike) to 130% charged
 * (central + secondaries on a target they all reach), Whirlwind 105% tapped (5 hits) to 150% held (10 hits), Spiky
 * Ball 145% (contact + 4 drag hits), Spike Barrage 110% tapped to 145% charged (the last two spikes on one target).
 */
public final class SpikeMoves {
	public static final float REF = com.grandline.fruit.rocket.RocketMoves.BASE_DAMAGE;
	public static final String FRUIT_ID = "grandline:spike";

	// --- Z: Spike Summon ---
	public static final double SUMMON_RANGE = 22, SUMMON_HEIGHT_TAP = 4, SUMMON_HEIGHT_FULL = 7, SUMMON_AREA = 4.5;
	public static final int SUMMON_MAX_CHARGE = 20, SUMMON_SPIKES = 6;
	static final float SUMMON_CORE = REF, SUMMON_SIDE = REF * 0.15f;

	// --- X: Whirlwind ---
	public static final int WHIRL_TAP = 30, WHIRL_MAX = 60, WHIRL_INTERVAL = 6;
	public static final double WHIRL_RADIUS_TAP = 2.5, WHIRL_RADIUS_FULL = 5, WHIRL_RISE_TAP = 1.5, WHIRL_RISE_FULL = 3;
	static final float WHIRL_HIT_EARLY = REF * 1.05f / 5, WHIRL_HIT_LATE = REF * 0.45f / 5;

	// --- C: Spiky Ball ---
	public static final int BALL_TICKS = 60, BALL_RECOVERY = 10, BALL_DRAG_INTERVAL = 10, BALL_DRAG_HITS = 4;
	public static final double BALL_SPEED = 0.35, BALL_CONTACT = 1.5, BALL_DIAMETER = 2.2, BALL_TURN = Math.toRadians(9);
	static final float BALL_CONTACT_DAMAGE = REF * 0.45f, BALL_DRAG_DAMAGE = REF * 0.25f;

	// --- V: Spike Barrage ---
	public static final int BARRAGE_MAX_CHARGE = 60, BARRAGE_TAP_COUNT = 4, BARRAGE_FULL_COUNT = 7;
	public static final double BARRAGE_RANGE_TAP = 14, BARRAGE_RANGE_FULL = 26, BARRAGE_HEIGHT_TAP = 5, BARRAGE_HEIGHT_FULL = 8;
	public static final double BARRAGE_INTERVAL = 2.4, BARRAGE_TURN = Math.toRadians(25);

	static AbilityDef summon, whirl, barrage;
	private static final Map<UUID, Vec3> AIM = new HashMap<>();
	private static final Map<UUID, int[]> WHIRLING = new HashMap<>(); // {held ticks, released (0/1)}
	private static final Set<UUID> ROLLING = new HashSet<>();

	private SpikeMoves() {
	}

	public static List<AbilityDef> moves() {
		summon = new AbilityDef("spike_summon", 1, 120, 0.20, AbilityDef.Activation.HELD, SpikeMoves::summonRelease);
		ChargeService.held(summon, SpikeMoves::summonHeld);
		ChargeService.maxTicks(summon, SUMMON_MAX_CHARGE);
		whirl = new AbilityDef("spike_whirlwind", 25, 210, 0.35, AbilityDef.Activation.HELD, SpikeMoves::whirlRelease);
		ChargeService.held(whirl, SpikeMoves::whirlHeld);
		ChargeService.maxTicks(whirl, WHIRL_MAX);
		barrage = new AbilityDef("spike_barrage", 75, 240, 0.40, AbilityDef.Activation.HELD, SpikeMoves::barrageRelease);
		ChargeService.held(barrage, SpikeMoves::barrageHeld);
		ChargeService.maxTicks(barrage, BARRAGE_MAX_CHARGE);
		// No weapon attacks from inside the ball.
		AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> ROLLING.contains(player.getUUID()) ? InteractionResult.FAIL
				: InteractionResult.PASS);
		return List.of(summon, whirl, new AbilityDef("spike_ball", 50, 270, 0.50, SpikeMoves::ball), barrage, AbilityDef.empty());
	}

	public static boolean rolling(ServerPlayer player) {
		return ROLLING.contains(player.getUUID());
	}

	static boolean valid(ServerPlayer player, ServerLevel level) {
		return player.isAlive() && !player.isRemoved() && player.level() == level
				&& FRUIT_ID.equals(com.grandline.player.PlayerProfileService.get(player).powers().fruit());
	}

	static double charge(int ticks, int max) {
		return Math.max(0, Math.min(1, (ticks - 2) / (double) (max - 2)));
	}

	// ============================================================================================== Z: Spike Summon

	/** The validated target: the look ray clipped by terrain (22 blocks), settled onto real ground below it. */
	static Vec3 target(ServerPlayer player) {
		Vec3 eye = player.getEyePosition();
		Vec3 end = eye.add(player.getLookAngle().scale(SUMMON_RANGE));
		BlockHitResult hit = player.serverLevel().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		Vec3 p = hit.getType() == HitResult.Type.MISS ? end : hit.getLocation().add(Vec3.atLowerCornerOf(hit.getDirection().getNormal()).scale(0.2));
		return GroundSpikes.ground(player, p, 1.0, 24);
	}

	static void summonHeld(AbilityContext ctx, int ticks) {
		ServerPlayer player = ctx.player();
		Vec3 t = target(player);
		if (t != null) {
			AIM.put(player.getUUID(), t);
		}
		if (ticks <= 1) {
			FxEvents.send(player, "spike_summon_cast", player.position(), player.getLookAngle(), 0, SUMMON_MAX_CHARGE, 0);
		}
		if (ticks % 2 == 0 && t != null) {
			FxEvents.send(player, "spike_summon_aim", t, Vec3.ZERO, charge(ticks, SUMMON_MAX_CHARGE), 0, 1.2 + 3.3 * charge(ticks, SUMMON_MAX_CHARGE));
		}
		HELD.put(player.getUUID(), ticks);
	}

	private static final Map<UUID, Integer> HELD = new HashMap<>();

	static int summonRelease(AbilityContext ctx) {
		ServerPlayer player = ctx.player();
		Integer held = HELD.remove(player.getUUID());
		double c = charge(held == null ? 0 : held, SUMMON_MAX_CHARGE);
		Vec3 at = target(player);
		if (at == null) {
			at = AIM.get(player.getUUID());
		}
		AIM.remove(player.getUUID());
		FxEvents.send(player, "spike_summon_release", player.position(), player.getLookAngle(), c, 0, 0);
		if (at == null) {
			return 0; // no valid ground in range: nothing erupts (energy already spent, like any missed move)
		}
		double h = SUMMON_HEIGHT_TAP + (SUMMON_HEIGHT_FULL - SUMMON_HEIGHT_TAP) * c;
		double r = 0.7 + 0.5 * c;
		GroundSpikes.erupt(ctx, new GroundSpikes.Spike(at, h, r, 0, 4, 30, SUMMON_CORE * 0.7f, SUMMON_CORE, 0.75 + 0.45 * c, "spike_rise", c > 0.9 ? 1 : 0),
				(e, core) -> AbilityHelpers.stagger(e, 10, 1));
		int extra = (int) Math.floor((SUMMON_SPIKES - 1) * c + 1e-6);
		double start = player.getRandom().nextDouble() * Math.PI * 2;
		for (int i = 0; i < extra; i++) {
			double a = start + i * Math.PI * 2 / Math.max(1, extra) + (player.getRandom().nextDouble() - 0.5) * 0.4;
			double dist = 1.6 + player.getRandom().nextDouble() * (SUMMON_AREA - 2.4);
			Vec3 g = GroundSpikes.ground(player, at.add(Math.cos(a) * dist, 0, Math.sin(a) * dist), 2, 3);
			if (g == null) {
				continue; // no valid ground for this one (wall, gap): it simply doesn't appear
			}
			double sh = 2 + 2 * player.getRandom().nextDouble() * (0.5 + 0.5 * c);
			GroundSpikes.erupt(ctx, new GroundSpikes.Spike(g, sh, 0.45, 1 + i, 3, 26, SUMMON_SIDE, SUMMON_SIDE, 0.5, "spike_rise", 2), null);
		}
		FxEvents.send(player, "spike_summon_impact", at, new Vec3(0, 1, 0), c, extra + 1, 1.2 + 3.3 * c);
		return 1;
	}

	// ============================================================================================== X: Whirlwind

	static void whirlHeld(AbilityContext ctx, int ticks) {
		ServerPlayer player = ctx.player();
		int[] st = WHIRLING.get(player.getUUID());
		if (st == null) {
			st = new int[]{0, 0};
			WHIRLING.put(player.getUUID(), st);
			whirlStart(ctx);
		}
		st[0] = ticks;
	}

	static int whirlRelease(AbilityContext ctx) {
		int[] st = WHIRLING.get(ctx.player().getUUID());
		if (st != null) {
			st[1] = 1;
		}
		return 1;
	}

	static double whirlRadius(int t) {
		return WHIRL_RADIUS_TAP + (WHIRL_RADIUS_FULL - WHIRL_RADIUS_TAP) * Math.max(0, Math.min(1, (t - WHIRL_TAP) / (double) (WHIRL_MAX - WHIRL_TAP)));
	}

	static double whirlRise(int t) {
		return WHIRL_RISE_TAP * Math.min(1, t / (double) WHIRL_TAP)
				+ (WHIRL_RISE_FULL - WHIRL_RISE_TAP) * Math.max(0, Math.min(1, (t - WHIRL_TAP) / (double) (WHIRL_MAX - WHIRL_TAP)));
	}

	/** The spin runs on its own timeline: at least the tapped 1.5 s, longer while held, never past 3 s. */
	static void whirlStart(AbilityContext ctx) {
		ServerPlayer player = ctx.player();
		ServerLevel level = player.serverLevel();
		UUID id = player.getUUID();
		double baseY = player.getY();
		FxEvents.send(player, "spike_whirl", player.position(), Vec3.ZERO, 0, WHIRL_MAX, WHIRL_RADIUS_TAP);
		MovementGuards.guard(player, WHIRL_MAX + 40);
		int[] hits = {0};
		TimedEffects.start(age -> {
			int[] st = WHIRLING.get(id);
			boolean go = st != null && valid(player, level) && !AbilityHelpers.inWater(player);
			int t = age;
			if (go && (t >= WHIRL_MAX || st[1] == 1 && t >= WHIRL_TAP)) {
				go = false;
			}
			if (!go) {
				WHIRLING.remove(id);
				MovementGuards.release(player);
				FxEvents.send(player, "spike_whirl_end", player.position(), Vec3.ZERO, t, 0, whirlRadius(t));
				return false;
			}
			// Rise smoothly toward the target height (ceilings stop it); horizontal movement is held back.
			double want = baseY + whirlRise(t);
			BlockHitResult ceil = level.clip(new ClipContext(player.position().add(0, player.getBbHeight(), 0),
					player.position().add(0, player.getBbHeight() + 0.4, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
			double vy = ceil.getType() == HitResult.Type.MISS ? Math.max(-0.1, Math.min(0.2, (want - player.getY()) * 0.35)) : 0;
			Vec3 v = player.getDeltaMovement();
			player.setDeltaMovement(v.x * 0.15, vy, v.z * 0.15);
			player.hurtMarked = true;
			player.fallDistance = 0;
			double r = whirlRadius(t);
			if (t % 4 == 0) {
				FxEvents.send(player, "spike_whirl_tick", player.position(), Vec3.ZERO, t, 0, r);
			}
			if (t % WHIRL_INTERVAL == WHIRL_INTERVAL / 2) { // 3, 9, 15 …: 5 rotations in the tapped 1.5 s, 10 in 3 s
				float dmg = hits[0] < 5 ? WHIRL_HIT_EARLY : WHIRL_HIT_LATE;
				hits[0]++;
				Vec3 c = player.position();
				for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(r + 1, 2, r + 1), e -> e != player
						&& e.isAlive() && !e.isSpectator() && !(e instanceof net.minecraft.world.entity.player.Player p && p.isCreative()))) {
					double d = Math.hypot(e.getX() - c.x, e.getZ() - c.z) - e.getBbWidth() / 2;
					if (d > r || e.getY() > c.y + 2.6 || e.getY() + e.getBbHeight() < c.y - 1.6) {
						continue;
					}
					boolean caught = d <= r * 0.7; // inside the main rotating attack: breaks Instinct; the edge is dodgeable
					e.invulnerableTime = 0; // each rotation interval is a separate hit (this target, this hit only)
					boolean landed = caught ? CombatContext.breaking(() -> ctx.damage(e, dmg)) : ctx.damage(e, dmg);
					if (landed) {
						stun(e, 14);
						FxEvents.send(player, "spike_whirl_hit", e.getBoundingBox().getCenter(), e.position().subtract(c).normalize(), 0, 0, 0);
					}
				}
			}
			return true;
		});
	}

	static void stun(LivingEntity e, int ticks) {
		int t = (int) Math.round(ticks * AbilityHelpers.ccScale(e));
		if (t <= 0 || com.grandline.technique.Dodges.justEvaded(e)) {
			return;
		}
		e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, t, 6, false, false));
		e.setDeltaMovement(e.getDeltaMovement().multiply(0.3, 1, 0.3));
		e.hurtMarked = true;
	}

	// ============================================================================================== C: Spiky Ball

	private record Captured(LivingEntity e, int[] hits, long[] lastHit, boolean drag) {
	}

	static int ball(AbilityContext ctx) {
		ServerPlayer player = ctx.player();
		ServerLevel level = player.serverLevel();
		if (AbilityHelpers.inWater(player) || ROLLING.contains(player.getUUID())) {
			return 0;
		}
		ROLLING.add(player.getUUID());
		Vec3 look = player.getLookAngle().multiply(1, 0, 1);
		Vec3[] dir = {look.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : look.normalize()};
		Vec3[] body = {player.position()};
		double[] vy = {0};
		boolean[] grounded = {player.onGround()};
		Map<Integer, Captured> caught = new HashMap<>();
		MovementGuards.guard(player, BALL_TICKS + BALL_RECOVERY + 20);
		FxEvents.send(player, "spike_ball", player.position(), dir[0], BALL_SPEED, BALL_TICKS, BALL_DIAMETER);
		TimedEffects.start(age -> {
			boolean ok = valid(player, level) && !AbilityHelpers.inWater(player);
			if (!ok || age > BALL_TICKS) {
				ROLLING.remove(player.getUUID());
				caught.values().forEach(c -> c.e().setDeltaMovement(dir[0].scale(0.35).add(0, 0.25, 0)));
				if (player.isAlive()) {
					// Recovery: a brief half-second stagger out of the ball.
					player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, BALL_RECOVERY, 4, false, false));
					player.setDeltaMovement(dir[0].scale(0.15));
					player.hurtMarked = true;
				}
				MovementGuards.release(player);
				FxEvents.send(player, "spike_ball_end", player.position(), dir[0], 0, BALL_RECOVERY, 0);
				return false;
			}
			// Steering: turn toward the camera's heading, a limited angle per tick.
			Vec3 want = player.getLookAngle().multiply(1, 0, 1);
			if (want.lengthSqr() > 1e-4) {
				want = want.normalize();
				double cur = Math.atan2(dir[0].z, dir[0].x), tgt = Math.atan2(want.z, want.x);
				double diff = Math.atan2(Math.sin(tgt - cur), Math.cos(tgt - cur));
				double a = cur + Math.max(-BALL_TURN, Math.min(BALL_TURN, diff));
				dir[0] = new Vec3(Math.cos(a), 0, Math.sin(a));
			}
			// Ordinary ground jumps: the client's own jump shows up as the player rising above the body (no air jumps).
			if (grounded[0] && player.getY() > body[0].y + 0.25) {
				vy[0] = 0.42;
				grounded[0] = false;
				FxEvents.send(player, "spike_ball_jump", body[0], Vec3.ZERO, 0, 0, 0);
			}
			vy[0] = Math.max(-1.5, vy[0] - 0.08);
			Vec3 from = body[0];
			KineticBody.Step step = KineticBody.sweep(player, from, dir[0].scale(BALL_SPEED).add(0, vy[0], 0));
			// Stairs and slopes: if a low ledge blocks the roll, step up one block.
			if (step.normal() != null && Math.abs(step.normal().y) < 0.3 && grounded[0]) {
				Vec3 up = from.add(0, 1.05, 0);
				if (KineticBody.free(player, up)) {
					KineticBody.Step s2 = KineticBody.sweep(player, up, dir[0].scale(BALL_SPEED));
					if (s2.moved() > step.moved() + 0.05) {
						step = s2;
					}
				}
			}
			boolean landed = step.normal() != null && step.normal().y > 0.5 || !KineticBody.free(player, step.pos().subtract(0, 0.06, 0));
			if (landed && vy[0] < 0) {
				if (!grounded[0] && vy[0] < -0.3) {
					FxEvents.send(player, "spike_ball_land", step.pos(), Vec3.ZERO, -vy[0], 0, 0);
				}
				vy[0] = 0;
			}
			if (step.normal() != null && step.normal().y < -0.5) {
				vy[0] = Math.min(0, vy[0]); // ceiling
			}
			grounded[0] = landed;
			body[0] = step.pos();
			drive(player, body[0]);
			// Contacts: first touch damages (breaks Instinct) and stuns; draggable enemies are carried ahead and hit again.
			long now = level.getGameTime();
			for (LivingEntity e : KineticBody.touched(player, from, body[0], BALL_CONTACT - 0.3, new HashSet<>(caught.keySet()))) {
				if (e instanceof com.grandline.entity.Islander || e.isInvulnerable()) {
					continue; // merchants and quest NPCs are never hit or moved
				}
				boolean hit = CombatContext.breaking(() -> ctx.damage(e, BALL_CONTACT_DAMAGE));
				if (hit) {
					stun(e, 20);
					FxEvents.send(player, "spike_ball_hit", e.getBoundingBox().getCenter(), dir[0], 0, 0, 0);
				}
				caught.put(e.getId(), new Captured(e, new int[]{0}, new long[]{now}, AbilityHelpers.ccScale(e) > 0.35));
			}
			for (var it = caught.values().iterator(); it.hasNext(); ) {
				Captured c = it.next();
				LivingEntity e = c.e();
				if (!e.isAlive() || e.level() != level || e.distanceTo(player) > 4.5) {
					it.remove();
					continue;
				}
				if (c.drag()) {
					// Carried along the rolling path, in front of the ball, moved with the entity's own collision.
					Vec3 targetPos = body[0].add(dir[0].scale(BALL_DIAMETER / 2 + e.getBbWidth() / 2 + 0.2));
					Vec3 delta = targetPos.subtract(e.position());
					e.move(MoverType.SELF, new Vec3(delta.x, Math.max(-0.5, Math.min(0.6, delta.y)), delta.z));
					e.setDeltaMovement(Vec3.ZERO);
					e.hurtMarked = true;
				}
				if (c.hits()[0] < BALL_DRAG_HITS && now - c.lastHit()[0] >= BALL_DRAG_INTERVAL && e.distanceTo(player) < BALL_DIAMETER + 1) {
					c.hits()[0]++;
					c.lastHit()[0] = now;
					e.invulnerableTime = 0;
					if (CombatContext.breaking(() -> ctx.damage(e, BALL_DRAG_DAMAGE))) {
						stun(e, 12);
						FxEvents.send(player, "spike_ball_hit", e.getBoundingBox().getCenter(), dir[0], 1, 0, 0);
					}
				}
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

	// ============================================================================================== V: Spike Barrage

	static void barrageHeld(AbilityContext ctx, int ticks) {
		ServerPlayer player = ctx.player();
		HELD.put(player.getUUID(), ticks);
		if (ticks <= 1) {
			FxEvents.send(player, "spike_barrage_cast", player.position(), player.getLookAngle(), 0, BARRAGE_MAX_CHARGE, 0);
		}
		if (ticks % 10 == 0) {
			FxEvents.send(player, "spike_barrage_charge", player.position(), Vec3.ZERO, charge(ticks, BARRAGE_MAX_CHARGE), 0, 0);
		}
	}

	static int barrageRelease(AbilityContext ctx) {
		ServerPlayer player = ctx.player();
		ServerLevel level = player.serverLevel();
		Integer held = HELD.remove(player.getUUID());
		double c = charge(held == null ? 0 : held, BARRAGE_MAX_CHARGE);
		int n = BARRAGE_TAP_COUNT + (int) Math.round((BARRAGE_FULL_COUNT - BARRAGE_TAP_COUNT) * c);
		double range = BARRAGE_RANGE_TAP + (BARRAGE_RANGE_FULL - BARRAGE_RANGE_TAP) * c;
		double top = BARRAGE_HEIGHT_TAP + (BARRAGE_HEIGHT_FULL - BARRAGE_HEIGHT_TAP) * c;
		double step = range / n;
		float lo = (float) (0.35 + 0.15 * c), hi = (float) (0.75 + 0.20 * c);
		Vec3 look = player.getLookAngle().multiply(1, 0, 1);
		Vec3[] dir = {look.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : look.normalize()};
		Vec3 start = GroundSpikes.ground(player, player.position(), 0.6, 2.5);
		FxEvents.send(player, "spike_barrage_release", player.position(), dir[0], c, n, 0);
		if (start == null) {
			return 0; // needs ground under the caster
		}
		Vec3[] last = {start};
		int[] index = {0};
		double[] next = {1};
		TimedEffects.start(age -> {
			if (!valid(player, level) || index[0] >= n) {
				return false;
			}
			if (age < next[0]) {
				return true;
			}
			next[0] += BARRAGE_INTERVAL;
			int i = index[0]++;
			// Steering: the remaining sequence follows the caster's current aim (limited turn per spike).
			Vec3 want = player.getLookAngle().multiply(1, 0, 1);
			if (want.lengthSqr() > 1e-4 && i > 0) {
				want = want.normalize();
				double cur = Math.atan2(dir[0].z, dir[0].x), tgt = Math.atan2(want.z, want.x);
				double diff = Math.atan2(Math.sin(tgt - cur), Math.cos(tgt - cur));
				double a = cur + Math.max(-BARRAGE_TURN, Math.min(BARRAGE_TURN, diff));
				dir[0] = new Vec3(Math.cos(a), 0, Math.sin(a));
			}
			Vec3 guess = last[0].add(dir[0].scale(i == 0 ? step * 0.7 : step));
			// Terrain following: no passing through walls, steps of at most 2 blocks, no crossing big gaps.
			BlockHitResult wall = level.clip(new ClipContext(last[0].add(0, 1.2, 0), guess.add(0, 1.2, 0), ClipContext.Block.COLLIDER,
					ClipContext.Fluid.NONE, player));
			Vec3 g = wall.getType() == HitResult.Type.MISS ? GroundSpikes.ground(player, guess, 2.2, 3.5) : null;
			if (g == null) {
				index[0] = n; // no valid continuation: the sequence ends here
				FxEvents.send(player, "spike_barrage_stop", guess, dir[0], 0, 0, 0);
				return false;
			}
			last[0] = g;
			double k = n == 1 ? 1 : i / (double) (n - 1);
			double h = 2 + (top - 2) * k;
			float dmg = REF * (lo + (hi - lo) * (float) k);
			boolean finale = i == n - 1;
			GroundSpikes.erupt(ctx, new GroundSpikes.Spike(g, h, 0.55 + 0.45 * k, 0, 3, 24 + (finale ? 10 : 0), dmg * 0.8f, dmg, 0.45 + 0.35 * k,
					finale ? "spike_rise_final" : "spike_rise", finale ? 1 : 0), (e, core) -> AbilityHelpers.stagger(e, 8, 0));
			return index[0] < n;
		});
		return 1;
	}
}
