package com.grandline.fruit.flame;

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
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Flame Fruit. Every move is a timed sequence (windup, release, travel or active hit, impact, recovery). The server
 * runs the timeline and the hit detection and sends a visual beat ({@link FxEvents}) at the exact tick each step
 * happens; clients draw the shapes from those beats (see the client's FlameFx). Hit volumes are computed from the
 * same speeds, sizes and ticks the visuals use, so what players see is what hits.
 *
 * <ul>
 *   <li>Z Cinder Shots (ranged): fire coils round the hand, then three compact fireballs, one per arm thrust.</li>
 *   <li>X Blaze Wave (directional): an arm sweep sends a broad curved wall of flame forward along the ground.</li>
 *   <li>C Pyre Ring (close burst): feet planted, flames drawn inward, then a low ring of fire races outward.</li>
 *   <li>V Inferno Nova (heavy, hold to charge): a flame mass grows over the raised hands, is hurled with the whole
 *   body, and explodes into a fast ring of fire; its blast area is marked on the ground while it flies.</li>
 *   <li>F Ember Wings (flight): ignition and lift-off, flame streamers that follow every turn, fade on landing.</li>
 * </ul>
 */
public final class FlameMoves {
	// --- Z: Cinder Shots ---
	public static final int SHOT_COIL_TICKS = 5;
	public static final int SHOTS = 3;
	public static final int SHOT_INTERVAL = 4;
	public static final double SHOT_SPEED = 2.4;
	public static final double SHOT_RANGE = 34;
	public static final double SHOT_SIZE = 0.45;
	static final float SHOT_DAMAGE = 6.0f;

	// --- X: Blaze Wave ---
	public static final int WAVE_WINDUP_TICKS = 7;
	public static final double WAVE_SPEED = 0.9;
	public static final double WAVE_RANGE = 14;
	public static final double WAVE_WIDTH = 7;
	public static final double WAVE_HEIGHT = 2.4;
	/** How far the middle of the wave leads its ends (it is curved, so the leading edge reads clearly). */
	public static final double WAVE_BOW = 1.2;
	static final float WAVE_DAMAGE = 15.0f;

	// --- C: Pyre Ring ---
	public static final int RING_GATHER_TICKS = 8;
	public static final double RING_SPEED = 0.8;
	public static final double RING_RADIUS = 7.5;
	public static final double RING_HEIGHT = 1.4;
	static final float RING_DAMAGE = 13.0f;

	// --- V: Inferno Nova ---
	public static final double NOVA_RANGE = 42;
	public static final double NOVA_SPEED = 1.1;
	static final float NOVA_DAMAGE = 18.0f;
	static final float NOVA_BONUS_DAMAGE = 16.0f;

	/** Fireball size for a charge (0..1). */
	public static double novaSize(double charge) {
		return 1.0 + charge * 1.2;
	}

	/** Blast radius for a fireball of {@code size} (clients use this to draw the area). */
	public static double novaRadius(double size) {
		return 5.0 + 3.5 * (size - 1.0) / 1.2;
	}

	// --- F: Ember Wings ---
	static final int FLIGHT_MAX_TICKS = 240;
	static final double FLIGHT_DRAIN_PER_TICK = 0.012;
	static final float FLIGHT_SPEED = 0.075f;

	private FlameMoves() {
	}

	public static List<AbilityDef> moves() {
		AbilityDef nova = new AbilityDef("flame_destroyer", 100, 300, 0.30, AbilityDef.Activation.CHARGED, FlameMoves::infernoNova);
		ChargeService.visual(nova, FlameMoves::chargingNova);
		return List.of(
				new AbilityDef("flame_bullets", 1, 50, 0.07, FlameMoves::cinderShots),
				new AbilityDef("flame_blast", 25, 140, 0.15, FlameMoves::blazeWave),
				new AbilityDef("flame_column", 50, 200, 0.20, FlameMoves::pyreRing),
				nova,
				new AbilityDef("flame_flight", 10, 100, 0.05, AbilityDef.Activation.TOGGLE, FlameMoves::emberWings));
	}

	private static boolean gone(ServerPlayer player) {
		return !player.isAlive() || player.isRemoved();
	}

	// ============================================================================================== Z  Cinder Shots

	static int cinderShots(AbilityContext ctx) {
		ServerPlayer player = ctx.player();
		// Windup: fire coils tightly round the casting hand.
		FxEvents.send(player, "flame_shot_coil", player.position());
		sound(player, SoundEvents.FIRECHARGE_USE, 0.5F, 1.9F);
		TimedEffects.start(age -> {
			if (gone(player)) {
				return false;
			}
			int t = age - SHOT_COIL_TICKS;
			if (t < 0 || t % SHOT_INTERVAL != 0) {
				return true;
			}
			int shot = t / SHOT_INTERVAL;
			if (shot >= SHOTS) {
				return false;
			}
			// Release: the arm extends and the fireball leaves the hand, aimed at the crosshair at that moment.
			Vec3 start = handPoint(player, shot % 2 == 0);
			Vec3 dir = Projectiles.crosshairAim(player, SHOT_RANGE).subtract(start).normalize();
			FxEvents.send(player, "flame_shot", start, dir, SHOT_SPEED, SHOT_RANGE, shot);
			sound(player, SoundEvents.BLAZE_SHOOT, 0.7F, 1.5F + shot * 0.08F);
			Projectiles.fire(player, start, dir, SHOT_SPEED, SHOT_RANGE, 0.35, (level, pos, a) -> {
			}, impact -> {
				boolean hit = impact.entity().isPresent();
				// Impact at the real contact point: 2 = struck an enemy, 1 = struck terrain, 0 = burnt out at range.
				boolean terrain = !hit && impact.position().distanceTo(start) < SHOT_RANGE - 0.5;
				FxEvents.send(player, "flame_shot_hit", impact.position(), dir, 0, 0, hit ? 2 : terrain ? 1 : 0);
				ServerLevel level = player.serverLevel();
				if (hit || terrain) {
					level.playSound(null, impact.position().x, impact.position().y, impact.position().z, SoundEvents.FIRECHARGE_USE,
							SoundSource.PLAYERS, hit ? 0.9F : 0.5F, hit ? 1.2F : 1.6F);
				}
				impact.entity().ifPresent(target -> {
					if (ctx.damage(target, SHOT_DAMAGE)) {
						target.setSecondsOnFire(2);
						AbilityHelpers.knockAway(target, start, 0.15, 0.05);
					}
				});
			});
			return true;
		});
		return 0;
	}

	/** Where a fireball leaves the hand: just ahead of the right (or left) shoulder at chest height. */
	static Vec3 handPoint(ServerPlayer player, boolean right) {
		Vec3 look = player.getLookAngle();
		Vec3 side = look.cross(new Vec3(0, 1, 0)).normalize().scale(right ? 0.3 : -0.3);
		return player.getEyePosition().subtract(0, 0.3, 0).add(look.scale(0.7)).add(side);
	}

	// ============================================================================================== X  Blaze Wave

	static int blazeWave(AbilityContext ctx) {
		ServerPlayer player = ctx.player();
		// Windup: the casting arm crosses the chest, flames running along it.
		FxEvents.send(player, "flame_wave_windup", player.position());
		sound(player, SoundEvents.FIRECHARGE_USE, 0.8F, 0.8F);
		TimedEffects.later(WAVE_WINDUP_TICKS, () -> {
			if (gone(player)) {
				return;
			}
			// Release: the arm sweeps out; the wave starts a block ahead of the feet and travels flat along the look yaw.
			Vec3 dir = flat(player.getLookAngle());
			Vec3 origin = player.position().add(dir.scale(1.0));
			FxEvents.send(player, "flame_wave", origin, dir, WAVE_SPEED, WAVE_RANGE, WAVE_WIDTH);
			sound(player, SoundEvents.BLAZE_SHOOT, 1.0F, 0.6F);
			sound(player, SoundEvents.FIRE_AMBIENT, 1.0F, 0.7F);
			Vec3 side = new Vec3(-dir.z, 0, dir.x);
			Set<Integer> struck = new HashSet<>();
			TimedEffects.start(age -> {
				ServerLevel level = player.serverLevel();
				double front = WAVE_SPEED * (age + 1);
				// Walls stop the wave where its middle runs into them.
				Vec3 mid = origin.add(dir.scale(front));
				if (front > WAVE_RANGE || solid(level, mid.add(0, 0.5, 0)) && solid(level, mid.add(0, 1.5, 0))) {
					FxEvents.send(player, "flame_wave_end", mid, dir, 0, 0, Math.min(front, WAVE_RANGE));
					return false;
				}
				for (LivingEntity target : AbilityHelpers.inRadius(player, mid, WAVE_WIDTH)) {
					if (struck.contains(target.getId())) {
						continue;
					}
					Vec3 rel = target.position().subtract(origin);
					double along = rel.dot(dir);
					double lateral = rel.dot(side);
					double half = WAVE_WIDTH / 2;
					double frontHere = front - WAVE_BOW * Math.min(1, (lateral / half) * (lateral / half));
					double dy = target.getY() - origin.y;
					if (Math.abs(lateral) <= half + target.getBbWidth() / 2 && along <= frontHere + 0.4 && along >= frontHere - 1.0 - WAVE_SPEED
							&& dy > -1.2 && dy < WAVE_HEIGHT + 0.2) {
						struck.add(target.getId());
						if (ctx.damage(target, WAVE_DAMAGE)) {
							target.setSecondsOnFire(4);
							target.setDeltaMovement(target.getDeltaMovement().add(dir.scale(1.0)).add(0, 0.35, 0));
							target.hurtMarked = true;
							FxEvents.send(player, "flame_wave_hit", target.position().add(0, target.getBbHeight() / 2, 0), dir, 0, 0, 1);
							level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS,
									0.9F, 0.9F);
						}
					}
				}
				return true;
			});
		});
		return 0;
	}

	// ============================================================================================== C  Pyre Ring

	static int pyreRing(AbilityContext ctx) {
		ServerPlayer player = ctx.player();
		Vec3 center = player.position();
		// Windup: feet planted, flames drawn inward along the ground.
		FxEvents.send(player, "flame_ring_gather", center);
		sound(player, SoundEvents.FIRECHARGE_USE, 0.8F, 0.5F);
		TimedEffects.later(RING_GATHER_TICKS, () -> {
			if (gone(player)) {
				return;
			}
			// Release: a low ring races outward; each enemy is hit the tick the ring's front reaches them.
			FxEvents.send(player, "flame_ring", center, Vec3.ZERO, RING_SPEED, RING_RADIUS, RING_HEIGHT);
			sound(player, SoundEvents.GENERIC_EXPLODE, 0.8F, 1.5F);
			sound(player, SoundEvents.BLAZE_SHOOT, 0.9F, 0.7F);
			Set<Integer> struck = new HashSet<>();
			TimedEffects.start(age -> {
				double r = RING_SPEED * (age + 1);
				if (r > RING_RADIUS + RING_SPEED) {
					return false;
				}
				for (LivingEntity target : AbilityHelpers.inRadius(player, center, r + 1.0)) {
					double d = Math.hypot(target.getX() - center.x, target.getZ() - center.z);
					double dy = target.getY() - center.y;
					if (struck.contains(target.getId()) || d > r + target.getBbWidth() / 2 || d < r - RING_SPEED - 0.8 || dy < -1.5 || dy > 2.5) {
						continue;
					}
					struck.add(target.getId());
					if (ctx.damage(target, RING_DAMAGE)) {
						target.setSecondsOnFire(4);
						AbilityHelpers.knockAway(target, center, 0.45, 0.85);
					}
				}
				return true;
			});
		});
		return 0;
	}

	// ============================================================================================== V  Inferno Nova

	/** While V is held the server re-sends the charge so every client grows the same flame mass. */
	static void chargingNova(ServerPlayer player, double charge, int ticks) {
		if (ticks % 4 == 0) {
			FxEvents.send(player, "flame_nova_charge", player.position(), charge);
		}
		if (ticks % 8 == 0) {
			sound(player, SoundEvents.FIRE_AMBIENT, 0.8F, 0.6F + (float) charge * 0.6F);
		}
	}

	static int infernoNova(AbilityContext ctx) {
		ServerPlayer player = ctx.player();
		double charge = ctx.charge();
		double size = novaSize(charge);
		double radius = novaRadius(size);
		// Release from above the raised hands; the full-body throw plays on every client from this beat.
		Vec3 start = player.position().add(0, player.getBbHeight() + 1.0 + size * 0.5, 0);
		Vec3 aim = Projectiles.crosshairAim(player, NOVA_RANGE);
		Vec3 dir = aim.subtract(start).normalize();
		double distance = Math.min(NOVA_RANGE, aim.distanceTo(start));
		double speed = NOVA_SPEED - charge * 0.25;
		FxEvents.send(player, "flame_nova", start, dir, speed, distance, size);
		sound(player, SoundEvents.BLAZE_SHOOT, 1.0F, 0.45F);
		sound(player, SoundEvents.WITHER_SHOOT, 0.5F, 0.6F);
		Projectiles.fire(player, start, dir, speed, NOVA_RANGE, size * 0.7, (level, pos, age) -> {
		}, impact -> {
			Vec3 at = impact.position();
			FxEvents.send(player, "flame_nova_impact", at, dir, 0, 0, radius);
			ServerLevel level = player.serverLevel();
			level.playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.6F, 0.8F);
			level.playSound(null, at.x, at.y, at.z, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.2F, 0.6F);
			float damage = (float) (NOVA_DAMAGE + NOVA_BONUS_DAMAGE * charge);
			for (LivingEntity target : AbilityHelpers.inRadius(player, at, radius)) {
				double falloff = 1.0 - 0.4 * Math.min(1.0, target.position().distanceTo(at) / radius);
				if (ctx.damage(target, (float) (damage * falloff))) {
					target.setSecondsOnFire(6);
					AbilityHelpers.knockAway(target, at, 1.6 + charge * 0.6, 0.6);
				}
			}
		});
		return 0;
	}

	// ============================================================================================== F  Ember Wings

	static int emberWings(AbilityContext ctx) {
		ServerPlayer player = ctx.player();
		// Ignition and lift-off; clients draw the streamers until the "flight_end" beat (FlightService.stop).
		FxEvents.send(player, "flame_flight_start", player.position());
		sound(player, SoundEvents.FIRECHARGE_USE, 1.0F, 0.9F);
		sound(player, SoundEvents.BLAZE_SHOOT, 0.6F, 1.3F);
		FlightService.start(player, ctx.power(), ctx.ability(), FLIGHT_MAX_TICKS, FLIGHT_DRAIN_PER_TICK, FLIGHT_SPEED, (flier, ticks) -> {
			if (ticks % 20 == 0) {
				sound(flier, SoundEvents.FIRE_AMBIENT, 0.7F, 1.0F);
			}
		});
		return 0;
	}

	// ============================================================================================== shared

	static Vec3 flat(Vec3 look) {
		Vec3 f = new Vec3(look.x, 0, look.z);
		return f.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : f.normalize();
	}

	private static boolean solid(ServerLevel level, Vec3 at) {
		BlockPos pos = BlockPos.containing(at);
		return !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
	}

	private static void sound(ServerPlayer player, SoundEvent sound, float volume, float pitch) {
		player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
	}
}
