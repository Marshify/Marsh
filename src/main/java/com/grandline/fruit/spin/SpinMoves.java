package com.grandline.fruit.spin;

import com.grandline.combat.ability.AbilityContext;
import com.grandline.combat.ability.AbilityDef;
import com.grandline.combat.ability.AbilityHelpers;
import com.grandline.combat.ability.ChargeService;
import com.grandline.combat.ability.FlightService;
import com.grandline.combat.ability.Projectiles;
import com.grandline.combat.ability.TimedEffects;
import com.grandline.combat.fx.FxEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Spin Fruit: rapid rotation, centrifugal force and cutting wind. Every move is a server timeline; the server decides
 * every hit, pull and movement step, and sends visual beats ({@link FxEvents}) that the client's SpinFx turns into
 * meshes, ribbons and poses.
 *
 * <ul>
 *   <li>Z Razor Wind (held): whirling arms throw a 6-deep, 5-wide field of cutting wind ahead; tap for a 0.6 s burst,
 *   hold up to 2.5 s. Hits every {@link #RAZOR_INTERVAL} ticks.</li>
 *   <li>X Tornado Assault: a 2 s steerable whirlwind (up to 12 blocks/s), pulling enemies in, ending in a lift.</li>
 *   <li>C Air Slasher: five fast wind blades in step with the arm sweeps, then one big finishing blade.</li>
 *   <li>V: unused (Spin has three attacks and one movement move).</li>
 *   <li>F Helicopter Flight: rotor-arm flight at about 7 blocks/s, draining energy, ended by water.</li>
 * </ul>
 *
 * Damage is a share of {@link #MISSILE_FIST}, the Rocket's Missile Fist (8 base, before mastery and Fruit-stat scaling):
 * Razor Wind 45% tapped / 95% held fully, Tornado Assault 110% for all hits, Air Slasher 125% for all blades.
 */
public final class SpinMoves {
	/** Rocket's Missile Fist base damage: the balance reference for Spin. */
	public static final float MISSILE_FIST = com.grandline.fruit.rocket.RocketMoves.BASE_DAMAGE;

	// --- Z: Razor Wind ---
	public static final int RAZOR_TAP_TICKS = 12;   // a tap still lasts 0.6 s
	public static final int RAZOR_MAX_TICKS = ChargeService.MAX_CHARGE_TICKS; // 2.5 s (server-enforced)
	/** Ticks between hits on the same field; matches vanilla's 10-tick invulnerability, so no hit is ever swallowed. */
	public static final int RAZOR_INTERVAL = 10;
	public static final double RAZOR_RANGE = 6;
	public static final double RAZOR_WIDTH = 5;
	/** First two hits (the tap): 22.5% each = 45%. Later three hits: 16.67% each, so a full hold totals 95%. */
	static final float RAZOR_EARLY_HIT = MISSILE_FIST * 0.225f;
	static final float RAZOR_LATE_HIT = MISSILE_FIST * (0.95f - 0.45f) / 3f;

	// --- X: Tornado Assault ---
	public static final int TORNADO_WINDUP_TICKS = 5;
	public static final int TORNADO_TICKS = 40;
	public static final double TORNADO_SPEED = 0.6; // 12 blocks per second
	public static final double TORNADO_RADIUS = 4;
	public static final int TORNADO_INTERVAL = 10;
	/** Four whirl hits of 20% and a 30% finisher = 110%. */
	static final float TORNADO_HIT = MISSILE_FIST * 0.20f;
	static final float TORNADO_FINISH = MISSILE_FIST * 0.30f;
	static final double TORNADO_PULL = 0.07;
	static final double TORNADO_LIFT = 0.55;

	// --- C: Air Slasher ---
	public static final int SLASHER_WINDUP_TICKS = 6;
	public static final int SLASHER_BLADES = 5;
	public static final int SLASHER_INTERVAL = 3;
	public static final int SLASHER_FINAL_TICK = 27; // after the two-arm finishing sweep
	public static final double SLASHER_RANGE = 35;
	public static final double BLADE_SPEED = 2.2;
	public static final double FINAL_SPEED = 1.8;
	static final double ASSIST_DEGREES = 6;
	/** Five blades of 15% and a 50% finisher = 125%. */
	static final float BLADE_DAMAGE = MISSILE_FIST * 0.15f;
	static final float FINAL_DAMAGE = MISSILE_FIST * 0.50f;

	// --- F: Helicopter Flight ---
	static final int FLIGHT_MAX_TICKS = 600;
	/** 0.65% of max energy per tick (13%/s) against 8%/s regeneration: about 10 s of flight from a full bar. */
	public static final double FLIGHT_DRAIN_PER_TICK = 0.0065;
	/** Vanilla flying speed giving about 7 blocks per second (Rocket's Blast Off is 0.09). */
	public static final float FLIGHT_SPEED = 0.032f;

	private static final Map<UUID, Integer> RAZOR_START = new HashMap<>();
	private static final Map<UUID, Integer> TORNADO = new HashMap<>();

	private SpinMoves() {
	}

	public static List<AbilityDef> moves() {
		AbilityDef razor = new AbilityDef("spin_razor_wind", 1, 150, 0.15, AbilityDef.Activation.HELD, SpinMoves::razorRelease);
		ChargeService.held(razor, SpinMoves::razorHeld);
		return List.of(
				razor,
				new AbilityDef("spin_tornado_assault", 50, 220, 0.25, SpinMoves::tornadoAssault),
				new AbilityDef("spin_air_slasher", 90, 285, 0.35, SpinMoves::airSlasher),
				AbilityDef.empty(),
				new AbilityDef("spin_helicopter_flight", 25, 150, 0.50, AbilityDef.Activation.TOGGLE, SpinMoves::helicopterFlight));
	}

	private static boolean gone(ServerPlayer player, Level level) {
		return !player.isAlive() || player.isRemoved() || player.level() != level;
	}

	public static boolean isTornado(ServerPlayer player) {
		return TORNADO.containsKey(player.getUUID());
	}

	// ============================================================================================== Z  Razor Wind

	/** Every server tick while Z is held (the server caps it at {@link #RAZOR_MAX_TICKS} and releases it). */
	static void razorHeld(AbilityContext ctx, int ticks) {
		ServerPlayer player = ctx.player();
		// The first held tick is 0 or 1 depending on when in the tick the key press arrived (packets are handled
		// between ticks); a stale start (a hold cut short by death) is replaced.
		if (ticks <= 1 && (ticks == 0 || !RAZOR_START.containsKey(player.getUUID())
				|| RAZOR_START.get(player.getUUID()) < player.server.getTickCount() - 1)) {
			RAZOR_START.put(player.getUUID(), player.server.getTickCount() - ticks);
			FxEvents.send(player, "spin_razor_start", player.position());
		}
		razorStep(ctx, ticks);
	}

	/** On release (or the automatic release at the cap): a tap keeps going to 0.6 s, a hold ends now. */
	static int razorRelease(AbilityContext ctx) {
		ServerPlayer player = ctx.player();
		Integer start = RAZOR_START.remove(player.getUUID());
		int held = start == null ? RAZOR_TAP_TICKS : player.server.getTickCount() - start;
		Level level = player.level();
		if (held >= RAZOR_TAP_TICKS) {
			FxEvents.send(player, "spin_razor_end", player.position(), held);
			return 0;
		}
		TimedEffects.start(age -> {
			int t = held + age;
			if (gone(player, level) || t >= RAZOR_TAP_TICKS) {
				FxEvents.send(player, "spin_razor_end", player.position(), t);
				return false;
			}
			razorStep(ctx, t);
			return true;
		});
		return 0;
	}

	/** One tick of the cutting field: a hit every {@link #RAZOR_INTERVAL} ticks to whatever stands inside it. */
	static void razorStep(AbilityContext ctx, int t) {
		if (t % RAZOR_INTERVAL != 1) {
			return;
		}
		ServerPlayer player = ctx.player();
		int n = t / RAZOR_INTERVAL;
		float damage = n < 2 ? RAZOR_EARLY_HIT : RAZOR_LATE_HIT;
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getLookAngle();
		Vec3 flat = flat(look);
		Vec3 side = new Vec3(-flat.z, 0, flat.x);
		for (LivingEntity target : AbilityHelpers.inRadius(player, eye.add(look.scale(RAZOR_RANGE / 2)), RAZOR_RANGE)) {
			Vec3 rel = target.getBoundingBox().getCenter().subtract(eye);
			double along = rel.dot(look);
			double lateral = Math.abs(rel.dot(side));
			double vertical = Math.abs(rel.subtract(look.scale(along)).y);
			if (along < -0.5 || along > RAZOR_RANGE + target.getBbWidth() / 2 || lateral > RAZOR_WIDTH / 2 + target.getBbWidth() / 2
					|| vertical > 1.8 || !player.hasLineOfSight(target)) {
				continue; // outside the field: stepping out escapes it
			}
			if (ctx.damage(target, damage)) {
				AbilityHelpers.stagger(target, 8, 1);
				AbilityHelpers.pushResisted(target, flat.scale(0.06));
				FxEvents.send(player, "spin_razor_hit", target.getBoundingBox().getCenter(), flat, 0, 0, n);
			}
		}
	}

	// ============================================================================================== X  Tornado Assault

	static int tornadoAssault(AbilityContext ctx) {
		ServerPlayer player = ctx.player();
		UUID id = player.getUUID();
		int run = player.server.getTickCount();
		TORNADO.put(id, run);
		Level level = player.level();
		Vec3[] body = {player.position()};
		FxEvents.send(player, "spin_tornado_start", player.position());
		TimedEffects.start(age -> {
			if (!Integer.valueOf(run).equals(TORNADO.get(id))) {
				return false;
			}
			if (gone(player, level)) {
				TORNADO.remove(id);
				FxEvents.send(player, "spin_tornado_end", player.position(), 0);
				return false;
			}
			player.fallDistance = 0;
			if (age < TORNADO_WINDUP_TICKS) {
				return true;
			}
			int t = age - TORNADO_WINDUP_TICKS;
			if (t == 0) {
				body[0] = player.position();
				FxEvents.send(player, "spin_tornado", player.position(), Vec3.ZERO, TORNADO_SPEED, TORNADO_TICKS, TORNADO_RADIUS);
			}
			if (t >= TORNADO_TICKS) {
				TORNADO.remove(id);
				finishTornado(ctx, body[0]);
				return false;
			}
			// Speed eases in over 6 ticks and out over the last 6; steered by the camera's yaw.
			double ease = Math.min(1, Math.min((t + 1) / 6.0, (TORNADO_TICKS - t) / 6.0));
			Vec3 step = flat(player.getLookAngle()).scale(TORNADO_SPEED * ease);
			Vec3 next = moveBody(player, body[0], step);
			Vec3 motion = next.subtract(body[0]);
			body[0] = next;
			player.setDeltaMovement(motion.x, Math.max(-0.4, motion.y), motion.z);
			player.hurtMarked = true;
			// Pull: everything in the whirl drifts toward its centre (never closer than 1.2 blocks, so no jitter).
			Vec3 centre = body[0];
			for (LivingEntity target : AbilityHelpers.inRadius(player, centre.add(0, 1, 0), TORNADO_RADIUS + 0.5)) {
				Vec3 to = new Vec3(centre.x - target.getX(), 0, centre.z - target.getZ());
				double d = to.length();
				if (d > 1.2 && Math.abs(target.getY() - centre.y) < 3) {
					AbilityHelpers.pushResisted(target, to.scale(TORNADO_PULL / d).add(motion.scale(0.35)));
				}
			}
			if (t % TORNADO_INTERVAL == 0) {
				for (LivingEntity target : tornadoTargets(player, centre)) {
					if (ctx.damage(target, TORNADO_HIT)) {
						AbilityHelpers.stagger(target, 6, 0);
						FxEvents.send(player, "spin_tornado_hit", target.getBoundingBox().getCenter(), Vec3.ZERO, 0, 0, 1);
					}
				}
			}
			return true;
		});
		return 0;
	}

	private static List<LivingEntity> tornadoTargets(ServerPlayer player, Vec3 centre) {
		return AbilityHelpers.inRadius(player, centre.add(0, 1, 0), TORNADO_RADIUS + 1).stream()
				.filter(e -> Math.hypot(e.getX() - centre.x, e.getZ() - centre.z) <= TORNADO_RADIUS + e.getBbWidth() / 2
						&& e.getY() - centre.y > -1.5 && e.getY() - centre.y < 3.5)
				.toList();
	}

	/** The finishing burst: a pressure wave that hits once more and lifts what it hits (scaled by resistance). */
	static void finishTornado(AbilityContext ctx, Vec3 centre) {
		ServerPlayer player = ctx.player();
		player.setDeltaMovement(Vec3.ZERO);
		player.hurtMarked = true;
		if (player.position().distanceTo(centre) > 1.5) {
			player.teleportTo(centre.x, centre.y, centre.z); // server authority over the whirl's path
		}
		FxEvents.send(player, "spin_tornado_end", centre, 1);
		for (LivingEntity target : tornadoTargets(player, centre)) {
			if (ctx.damage(target, TORNADO_FINISH)) {
				Vec3 out = new Vec3(target.getX() - centre.x, 0, target.getZ() - centre.z);
				out = out.lengthSqr() < 1e-4 ? Vec3.ZERO : out.normalize().scale(0.25);
				AbilityHelpers.pushResisted(target, out.add(0, TORNADO_LIFT, 0));
			}
		}
	}

	/**
	 * One step of a ground-hugging moving body: walls stop it (it climbs one-block steps), it follows the ground down
	 * slopes and ledges, and never passes through blocks. Reusable by any dash-like move.
	 */
	public static Vec3 moveBody(ServerPlayer player, Vec3 from, Vec3 step) {
		ServerLevel level = player.serverLevel();
		if (step.lengthSqr() < 1e-6) {
			return from;
		}
		Vec3 dir = step.normalize();
		Vec3 target = from;
		for (double y : new double[]{0.0, 1.05}) { // straight on, or up one block
			Vec3 base = from.add(0, y, 0);
			if (y > 0 && blocked(level, player, from.add(0, 0.3, 0), from.add(0, 0.3 + y + 1.6, 0))) {
				break; // no head room to climb
			}
			boolean clear = !blocked(level, player, base.add(0, 0.3, 0), base.add(0, 0.3, 0).add(step).add(dir.scale(0.35)))
					&& !blocked(level, player, base.add(0, 1.5, 0), base.add(0, 1.5, 0).add(step).add(dir.scale(0.35)));
			if (clear) {
				target = base.add(step);
				break;
			}
		}
		// Settle on the ground (down a slope or off a ledge at most 0.5 per tick).
		BlockHitResult down = level.clip(new ClipContext(target.add(0, 0.5, 0), target.subtract(0, 0.55, 0), ClipContext.Block.COLLIDER,
				ClipContext.Fluid.NONE, player));
		return down.getType() == HitResult.Type.MISS ? target.subtract(0, 0.5, 0) : new Vec3(target.x, down.getLocation().y, target.z);
	}

	private static boolean blocked(ServerLevel level, ServerPlayer player, Vec3 a, Vec3 b) {
		return level.clip(new ClipContext(a, b, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)).getType() != HitResult.Type.MISS;
	}

	// ============================================================================================== C  Air Slasher

	static int airSlasher(AbilityContext ctx) {
		ServerPlayer player = ctx.player();
		Level level = player.level();
		FxEvents.send(player, "spin_slasher_windup", player.position());
		for (int i = 0; i <= SLASHER_BLADES; i++) {
			boolean last = i == SLASHER_BLADES;
			int n = i;
			int at = last ? SLASHER_FINAL_TICK : SLASHER_WINDUP_TICKS + i * SLASHER_INTERVAL;
			if (last) {
				// The two-arm finishing sweep winds up for a beat before the big blade leaves.
				TimedEffects.later(SLASHER_FINAL_TICK - 6, () -> {
					if (!gone(player, level)) {
						FxEvents.send(player, "spin_slasher_finale", player.position());
					}
				});
			}
			TimedEffects.later(at, () -> {
				if (gone(player, level)) {
					return;
				}
				// Released from the sweeping hand (alternating), aimed at the crosshair or an assisted target.
				Vec3 start = handPoint(player, last || n % 2 == 0, last);
				Vec3 aim = assistedAim(player, start);
				Vec3 dir = aim.subtract(start).normalize();
				double speed = last ? FINAL_SPEED : BLADE_SPEED;
				FxEvents.send(player, "spin_blade", start, dir, speed, SLASHER_RANGE, last ? 5 : n);
				Projectiles.fire(player, start, dir, speed, SLASHER_RANGE, last ? 1.0 : 0.55, (lvl, pos, a) -> {
				}, impact -> {
					boolean hit = impact.entity().isPresent();
					boolean terrain = !hit && impact.position().distanceTo(start) < SLASHER_RANGE - 0.5;
					FxEvents.send(player, "spin_blade_hit", impact.position(), dir, hit ? 2 : terrain ? 1 : 0, 0, last ? 5 : n);
					impact.entity().ifPresent(target -> {
						// Each blade hits once; the blades come faster than vanilla's invulnerability window, so this
						// blade's own hit clears it (only for this hit, never globally).
						target.invulnerableTime = 0;
						if (ctx.damage(target, last ? FINAL_DAMAGE : BLADE_DAMAGE)) {
							if (last) {
								AbilityHelpers.pushResisted(target, dir.multiply(1, 0, 1).normalize().scale(0.9).add(0, 0.3, 0));
							} else {
								AbilityHelpers.stagger(target, 6, 1);
								AbilityHelpers.pushResisted(target, dir.multiply(1, 0, 1).normalize().scale(0.12));
							}
						}
					});
				});
			});
		}
		return 0;
	}

	/** The crosshair target, or the nearest visible enemy within a narrow cone around it (never through walls). */
	static Vec3 assistedAim(ServerPlayer player, Vec3 start) {
		Vec3 aim = Projectiles.crosshairAim(player, SLASHER_RANGE);
		Vec3 look = player.getLookAngle();
		double minDot = Math.cos(Math.toRadians(ASSIST_DEGREES));
		return AbilityHelpers.inCone(player, SLASHER_RANGE, ASSIST_DEGREES).stream()
				.filter(player::hasLineOfSight)
				.max(Comparator.comparingDouble(e -> e.getBoundingBox().getCenter().subtract(player.getEyePosition()).normalize().dot(look)))
				.filter(e -> e.getBoundingBox().getCenter().subtract(player.getEyePosition()).normalize().dot(look) >= minDot)
				.map(e -> e.getBoundingBox().getCenter())
				.orElse(aim);
	}

	/** The releasing hand: right or left shoulder, a little ahead; the finisher leaves from between both hands. */
	static Vec3 handPoint(ServerPlayer player, boolean right, boolean both) {
		Vec3 look = player.getLookAngle();
		Vec3 side = look.cross(new Vec3(0, 1, 0)).normalize().scale(both ? 0 : right ? 0.4 : -0.4);
		return player.getEyePosition().subtract(0, 0.35, 0).add(look.scale(0.8)).add(side);
	}

	// ============================================================================================== F  Helicopter Flight

	static int helicopterFlight(AbilityContext ctx) {
		ServerPlayer player = ctx.player();
		if (inWater(player)) {
			return 0; // can't take off from water (the fruit's weakness)
		}
		FxEvents.send(player, "spin_heli_start", player.position());
		FlightService.start(player, ctx.power(), ctx.ability(), FLIGHT_MAX_TICKS, FLIGHT_DRAIN_PER_TICK, FLIGHT_SPEED, (flier, ticks) -> {
			if (inWater(flier)) {
				FlightService.stop(flier, true); // water ends the flight at once
			}
		});
		return 0;
	}

	// ============================================================================================== shared

	/** In water: the vanilla flag, or water at the feet or body (checked directly, so it never lags a tick). */
	static boolean inWater(ServerPlayer player) {
		BlockPos feet = player.blockPosition();
		return player.isInWater() || player.level().getFluidState(feet).is(net.minecraft.tags.FluidTags.WATER)
				|| player.level().getFluidState(feet.above()).is(net.minecraft.tags.FluidTags.WATER);
	}

	static Vec3 flat(Vec3 look) {
		Vec3 f = new Vec3(look.x, 0, look.z);
		return f.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : f.normalize();
	}

	/** Clears all running moves (server stop). */
	public static void clear() {
		RAZOR_START.clear();
		TORNADO.clear();
	}
}
