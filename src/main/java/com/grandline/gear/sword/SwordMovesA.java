package com.grandline.gear.sword;

import com.grandline.combat.ability.AbilityContext;
import com.grandline.combat.ability.AbilityDef;
import com.grandline.combat.ability.AbilityHelpers;
import com.grandline.combat.ability.TimedEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.grandline.gear.sword.SwordKit.layer;
import static com.grandline.gear.sword.SwordKit.later;

/**
 * Katana, Cutlass, Dual Katana, Iron Mace, Reefsaw, Triple Katana, Boiler Pipe and Stonewall Blade. Each move is a timed
 * sequence run on the server; every visual is sent as a beat on the tick it happens (see SwordKit), and the hit
 * volumes use the same sizes, speeds and timings as the visuals.
 */
final class SwordMovesA extends SwordMoveBase {
	private SwordMovesA() {
	}

	// =================================================================================================== Katana

	static List<AbilityDef> katana() {
		return List.of(def("katana_silent_step", 1, 7, 15, SwordMovesA::silentStep), def("katana_crescent_edge", 20, 10, 25, SwordMovesA::crescentEdge));
	}

	/** Z: a flash-step forward through enemies, thin white cuts along the path and an afterimage left behind. */
	static int silentStep(AbilityContext ctx) {
		ServerPlayer p = ctx.player();
		pose(p, POSE_THRUST, 2);
		SwordKit.sound(p, p.position(), layer(SoundEvents.PLAYER_ATTACK_SWEEP, 0.9f, 1.7f), layer(SoundEvents.PHANTOM_FLAP, 0.7f, 2.0f),
				later(SoundEvents.PLAYER_ATTACK_CRIT, 0.8f, 1.6f, 3));
		TimedEffects.later(2, () -> dash(ctx, "white", 1, p.getLookAngle(), 7.5, 4, 1.2, 9f, (t, at) -> {
			SwordKit.fx(p, "cut", "white", 0, t.position().add(0, t.getBbHeight() * 0.6, 0), SwordKit.forward(p), 1.0);
			SwordKit.push(t, at, 0.25, 0.15);
		}));
		return 0;
	}

	/** X: a short crescent of compressed wind that knocks enemies back. */
	static int crescentEdge(AbilityContext ctx) {
		ServerPlayer p = ctx.player();
		pose(p, POSE_SLASH_L, 3);
		SwordKit.sound(p, p.position(), layer(SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 1.2f), later(SoundEvents.PHANTOM_FLAP, 1.0f, 1.4f, 3),
				later(SoundEvents.ELYTRA_FLYING, 0.25f, 2.0f, 3));
		TimedEffects.later(3, () -> {
			Vec3 start = SwordKit.chest(p).add(SwordKit.forward(p).scale(0.8));
			blade(ctx, "crescent", "white", 0, start, aim(p, start, 10), 1.3, 10, 1.1, 2.4, 12f, (t, at) -> {
				SwordKit.push(t, p.position(), 1.1, 0.35);
				SwordKit.sound(p, at, layer(SoundEvents.PLAYER_ATTACK_KNOCKBACK, 0.9f, 1.3f));
			});
		});
		return 0;
	}

	// =================================================================================================== Cutlass

	static List<AbilityDef> cutlass() {
		return List.of(def("cutlass_swift_advance", 1, 7, 15, SwordMovesA::swiftAdvance), def("cutlass_wind_cutter", 20, 10, 25, SwordMovesA::windCutter));
	}

	/** Z: closes in on the enemy under the crosshair (or straight ahead), sharp cuts along the way. */
	static int swiftAdvance(AbilityContext ctx) {
		ServerPlayer p = ctx.player();
		LivingEntity target = AbilityHelpers.firstInFront(p, 11);
		Vec3 dir = SwordKit.forward(p);
		double distance = 6.5;
		if (target != null) {
			Vec3 to = target.position().subtract(p.position());
			dir = SwordKit.flat(to);
			distance = Math.max(1.5, Math.hypot(to.x, to.z) - 0.9);
		}
		pose(p, POSE_SLASH_R, 1);
		SwordKit.sound(p, p.position(), layer(SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 1.45f), later(SoundEvents.PLAYER_ATTACK_STRONG, 0.8f, 1.3f, 4));
		dash(ctx, "cutlass", 0, dir, distance, 5, 1.3, 9f, (t, at) -> {
			SwordKit.fx(p, "cut", "cutlass", 1, t.position().add(0, t.getBbHeight() * 0.6, 0), SwordKit.forward(p), 1.1);
			SwordKit.push(t, at, 0.3, 0.2);
		});
		return 0;
	}

	/** X: a curved blade of compressed air that travels forward and knocks enemies back. */
	static int windCutter(AbilityContext ctx) {
		ServerPlayer p = ctx.player();
		pose(p, POSE_SLASH_R, 3);
		SwordKit.sound(p, p.position(), layer(SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 0.95f), later(SoundEvents.ENDER_DRAGON_FLAP, 0.6f, 1.8f, 3));
		TimedEffects.later(3, () -> {
			Vec3 start = SwordKit.chest(p).add(SwordKit.forward(p).scale(0.8));
			blade(ctx, "crescent", "cutlass", 1, start, aim(p, start, 15), 1.0, 15, 1.2, 2.8, 12f, (t, at) -> {
				SwordKit.push(t, p.position(), 1.2, 0.35);
				SwordKit.sound(p, at, layer(SoundEvents.PLAYER_ATTACK_KNOCKBACK, 0.9f, 1.1f));
			});
		});
		return 0;
	}

	// =================================================================================================== Dual Katana

	static List<AbilityDef> dualKatana() {
		return List.of(def("dual_katana_cyclone_edge", 1, 7, 15, SwordMovesA::cycloneEdge), def("dual_katana_spiral_tempest", 30, 13, 25, SwordMovesA::spiralTempest));
	}

	/** Z: a rolling sphere of curved blade ribbons that carries enemies along, then flings them away. */
	static int cycloneEdge(AbilityContext ctx) {
		ServerPlayer p = ctx.player();
		pose(p, POSE_SLASH_L, 3);
		SwordKit.sound(p, p.position(), layer(SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 1.6f), later(SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 1.3f, 2),
				later(SoundEvents.ELYTRA_FLYING, 0.35f, 1.6f, 3));
		TimedEffects.later(3, () -> {
			Vec3 start = SwordKit.chest(p).add(SwordKit.forward(p).scale(1.2));
			Vec3 dir = aim(p, start, 14);
			SwordKit.fx(p, "orb", "teal", 0, start, dir, 0.7, 14, 1.5);
			SwordShot.of(p, start, dir, 0.7, 14, 1.4).carrying()
					.onHit((t, at) -> SwordKit.hitFresh(ctx, t, 6f))
					.onEnd((at, wall, t) -> {
						end(p, "teal", at, 1.6);
						SwordKit.sound(p, at, layer(SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 0.9f), layer(SoundEvents.PLAYER_ATTACK_KNOCKBACK, 0.9f, 0.9f));
						for (LivingEntity e : SwordKit.sphere(p, at, 2.6)) {
							SwordKit.hitFresh(ctx, e, 5f);
							SwordKit.push(e, at.subtract(0, 0.5, 0), 1.3, 0.55);
						}
					}).fire();
		});
		return 0;
	}

	static final double TEMPEST_RADIUS = 4.5;
	static final int TEMPEST_TICKS = 30;

	/** X: a large wind vortex round the caster: repeated cuts, enemies drawn in, then launched outward. */
	static int spiralTempest(AbilityContext ctx) {
		ServerPlayer p = ctx.player();
		pose(p, POSE_SPIN, 0);
		SwordKit.fx(p, "vortex", "teal", 0, p.position(), Vec3.ZERO, TEMPEST_TICKS, TEMPEST_RADIUS, 4.0);
		SwordKit.sound(p, p.position(), layer(SoundEvents.ELYTRA_FLYING, 0.7f, 1.3f), layer(SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 0.8f),
				later(SoundEvents.PLAYER_ATTACK_SWEEP, 0.8f, 1.1f, 10), later(SoundEvents.PLAYER_ATTACK_SWEEP, 0.8f, 1.3f, 20),
				later(SoundEvents.ENDER_DRAGON_FLAP, 1.0f, 1.2f, TEMPEST_TICKS));
		TimedEffects.start(age -> {
			if (SwordKit.gone(p)) {
				return false;
			}
			Vec3 c = p.position();
			if (age < TEMPEST_TICKS) {
				if (age % 6 == 0) {
					for (LivingEntity e : SwordKit.arc(p, c, Vec3.ZERO, TEMPEST_RADIUS, 180, 1.5, 4)) {
						SwordKit.hitFresh(ctx, e, 3.5f);
						SwordKit.pull(e, c.add(0, 0.6, 0), 0.35);
					}
				}
				return true;
			}
			for (LivingEntity e : SwordKit.arc(p, c, Vec3.ZERO, TEMPEST_RADIUS + 0.5, 180, 1.5, 4)) {
				SwordKit.hitFresh(ctx, e, 7f);
				SwordKit.push(e, c, 1.5, 0.75);
			}
			SwordKit.fx(p, "burst", "teal", 0, c.add(0, 1, 0), Vec3.ZERO, TEMPEST_RADIUS);
			return false;
		});
		return 0;
	}

	// =================================================================================================== Iron Mace

	static List<AbilityDef> ironMace() {
		return List.of(def("iron_mace_fracturing_impact", 1, 7, 15, SwordMovesA::fracturingImpact), def("iron_mace_crushing_sequence", 30, 11, 25, SwordMovesA::crushingSequence));
	}

	static final int IMPACT_WINDUP = 7;

	/** Z: the mace raised overhead and slammed down: shockwave ring, three fractures, debris and dust. */
	static int fracturingImpact(AbilityContext ctx) {
		ServerPlayer p = ctx.player();
		pose(p, POSE_OVERHEAD, IMPACT_WINDUP);
		SwordKit.sound(p, p.position(), layer(SoundEvents.ARMOR_EQUIP_IRON, 1.0f, 0.6f));
		TimedEffects.later(IMPACT_WINDUP, () -> {
			if (SwordKit.gone(p)) {
				return;
			}
			Vec3 f = SwordKit.forward(p);
			Vec3 at = SwordKit.ground(p.serverLevel(), p.position().add(f.scale(1.8)));
			SwordKit.fx(p, "slam", "stone", 3, at, f, 0, 5.0, 5.0);
			SwordKit.sound(p, at, layer(SoundEvents.ANVIL_LAND, 1.0f, 0.55f), layer(SoundEvents.GENERIC_EXPLODE, 0.6f, 0.6f),
					layer(SoundEvents.DEEPSLATE_BREAK, 1.0f, 0.5f), later(SoundEvents.GRAVEL_BREAK, 1.0f, 0.7f, 3));
			for (LivingEntity e : SwordKit.arc(p, at, f, 4.5, 180, 1.5, 2.5)) {
				SwordKit.hitFresh(ctx, e, 11f);
				SwordKit.push(e, at, 0.7, 0.55);
			}
		});
		return 0;
	}

	static final int CRUSH_SWINGS = 5;
	static final int CRUSH_INTERVAL = 4;

	/** X: a rapid string of mace swings, each one hitting everything in front. */
	static int crushingSequence(AbilityContext ctx) {
		ServerPlayer p = ctx.player();
		TimedEffects.start(age -> {
			if (SwordKit.gone(p)) {
				return false;
			}
			if (age % CRUSH_INTERVAL != 0) {
				return true;
			}
			int swing = age / CRUSH_INTERVAL;
			if (swing >= CRUSH_SWINGS) {
				return false;
			}
			boolean last = swing == CRUSH_SWINGS - 1;
			Vec3 f = SwordKit.forward(p);
			pose(p, last ? POSE_OVERHEAD : swing % 2 == 0 ? POSE_SLASH_R : POSE_SLASH_L, 0);
			SwordKit.fx(p, "trail", "stone", last ? 3 : swing % 2, SwordKit.chest(p), f, 3.0);
			SwordKit.sound(p, p.position(), layer(SoundEvents.PLAYER_ATTACK_STRONG, 1.0f, 0.75f + swing * 0.05f),
					layer(SoundEvents.ANVIL_PLACE, 0.25f, 0.6f + swing * 0.08f));
			List<LivingEntity> hit = SwordKit.arc(p, p.position(), f, 3.6, 70, 1, 2.6);
			for (LivingEntity e : hit) {
				SwordKit.hitFresh(ctx, e, last ? 6f : 4.5f);
				SwordKit.push(e, p.position(), last ? 0.9 : 0.2, last ? 0.4 : 0.05);
				SwordKit.fx(p, "cut", "stone", 2, e.position().add(0, e.getBbHeight() * 0.6, 0), f, 0.9);
			}
			if (last) {
				Vec3 at = SwordKit.ground(p.serverLevel(), p.position().add(f.scale(1.8)));
				SwordKit.fx(p, "slam", "stone", 0, at, f, 0, 2.5, 2.5);
				SwordKit.sound(p, at, layer(SoundEvents.ANVIL_LAND, 0.7f, 0.7f));
			}
			return true;
		});
		return 0;
	}

	// =================================================================================================== Reefsaw

	static List<AbilityDef> reefsaw() {
		return List.of(def("reefsaw_serrated_fury", 10, 8, 15, SwordMovesA::serratedFury), def("reefsaw_crimson_grinder", 60, 12, 25, SwordMovesA::crimsonGrinder));
	}

	/** Z: two diagonal slashes and a vertical finisher in black and dark crimson; the finisher knocks back. */
	static int serratedFury(AbilityContext ctx) {
		ServerPlayer p = ctx.player();
		int[] styles = {1, 2, 3};
		int[] poses = {POSE_SLASH_L, POSE_SLASH_R, POSE_OVERHEAD};
		for (int i = 0; i < 3; i++) {
			int n = i;
			TimedEffects.later(i * 5, () -> {
				if (SwordKit.gone(p)) {
					return;
				}
				boolean last = n == 2;
				Vec3 f = SwordKit.forward(p);
				pose(p, poses[n], 0);
				SwordKit.fx(p, "trail", "reef", styles[n], SwordKit.chest(p), f, last ? 4.2 : 3.6);
				SwordKit.sound(p, p.position(), layer(SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, last ? 0.7f : 0.95f),
						layer(SoundEvents.GRINDSTONE_USE, 0.35f, last ? 0.8f : 1.3f));
				for (LivingEntity e : SwordKit.arc(p, p.position(), f, 4.0, 75, 1, 2.8)) {
					SwordKit.hitFresh(ctx, e, last ? 7f : 5f);
					SwordKit.fx(p, "cut", "reef", n == 2 ? 3 : n + 1, e.position().add(0, e.getBbHeight() * 0.6, 0), f, last ? 1.3 : 1.0);
					if (last) {
						SwordKit.push(e, p.position(), 1.3, 0.4);
					}
				}
			});
		}
		return 0;
	}

	static final int GRINDER_TICKS = 40;
	static final double GRINDER_RADIUS = 3.5;

	/** X: spins forward toward the crosshair inside a storm of dark-red cuts, dragging enemies in, then a big outward strike. */
	static int crimsonGrinder(AbilityContext ctx) {
		ServerPlayer p = ctx.player();
		pose(p, POSE_SPIN, 0);
		SwordKit.fx(p, "grinder", "reef", 0, p.position(), Vec3.ZERO, GRINDER_TICKS, GRINDER_RADIUS, 3.0);
		SwordKit.sound(p, p.position(), layer(SoundEvents.GRINDSTONE_USE, 1.0f, 0.6f), later(SoundEvents.GRINDSTONE_USE, 1.0f, 0.75f, 12),
				later(SoundEvents.GRINDSTONE_USE, 1.0f, 0.9f, 24), layer(SoundEvents.ELYTRA_FLYING, 0.5f, 0.8f));
		Vec3 aimPoint = com.grandline.combat.ability.Projectiles.crosshairTarget(p, 16);
		TimedEffects.start(age -> {
			if (SwordKit.gone(p)) {
				return false;
			}
			Vec3 c = p.position();
			if (age < GRINDER_TICKS) {
				Vec3 to = SwordKit.flat(aimPoint.subtract(c));
				if (aimPoint.subtract(c).multiply(1, 0, 1).length() > 1.2) {
					SwordKit.steer(p, SwordKit.reach(p, c, to, 0.28), 0.28);
				}
				if (age % 5 == 0) {
					for (LivingEntity e : SwordKit.arc(p, c, Vec3.ZERO, GRINDER_RADIUS + 1.5, 180, 1.5, 3)) {
						SwordKit.pull(e, c.add(SwordKit.forward(p).scale(1.2)).add(0, 0.3, 0), 0.45);
						if (e.position().distanceTo(c) <= GRINDER_RADIUS + 0.5) {
							SwordKit.hitFresh(ctx, e, 3f);
						}
					}
				}
				return true;
			}
			pose(p, POSE_SLASH_R, 0);
			SwordKit.fx(p, "burst", "reef", 1, c.add(0, 1, 0), Vec3.ZERO, GRINDER_RADIUS + 1);
			SwordKit.sound(p, c, layer(SoundEvents.PLAYER_ATTACK_STRONG, 1.0f, 0.6f), layer(SoundEvents.GENERIC_EXPLODE, 0.4f, 1.3f));
			for (LivingEntity e : SwordKit.arc(p, c, Vec3.ZERO, GRINDER_RADIUS + 1, 180, 1.5, 3)) {
				SwordKit.hitFresh(ctx, e, 9f);
				SwordKit.push(e, c, 1.6, 0.6);
			}
			return false;
		});
		return 0;
	}

	// =================================================================================================== Triple Katana

	static List<AbilityDef> tripleKatana() {
		return List.of(def("triple_katana_azure_barrage", 20, 7.5, 15, SwordMovesA::azureBarrage), def("triple_katana_azure_pursuit", 60, 12, 25, SwordMovesA::azurePursuit));
	}

	static final int BARRAGE_SHOTS = 5;

	/** Z: five luminous blue crescents in quick succession, each flying and hitting on its own. */
	static int azureBarrage(AbilityContext ctx) {
		ServerPlayer p = ctx.player();
		TimedEffects.start(age -> {
			if (SwordKit.gone(p)) {
				return false;
			}
			if (age % 3 != 0) {
				return true;
			}
			int shot = age / 3;
			if (shot >= BARRAGE_SHOTS) {
				return false;
			}
			pose(p, shot % 2 == 0 ? POSE_SLASH_R : POSE_SLASH_L, 0);
			Vec3 start = SwordKit.chest(p).add(SwordKit.forward(p).scale(0.8)).add(0, (shot % 3 - 1) * 0.25, 0);
			Vec3 dir = aim(p, start, 22);
			SwordKit.sound(p, p.position(), layer(SoundEvents.PLAYER_ATTACK_SWEEP, 0.8f, 1.5f + shot * 0.08f),
					layer(SoundEvents.AMETHYST_BLOCK_CHIME, 0.6f, 1.2f + shot * 0.1f));
			SwordKit.fx(p, "crescent", "azure", shot % 2 == 0 ? 2 : 3, start, dir, 1.6, 22, 1.3);
			SwordShot.of(p, start, dir, 1.6, 22, 0.6).single()
					.onHit((t, at) -> SwordKit.hitFresh(ctx, t, 4.2f))
					.onEnd((at, wall, t) -> end(p, "azure", at, 0.8)).fire();
			return true;
		});
		return 0;
	}

	static final int PURSUIT_TICKS = 8;

	/** X: charges forward wrapped in blue cutting arcs, leaving short-lived blue trails behind. */
	static int azurePursuit(AbilityContext ctx) {
		ServerPlayer p = ctx.player();
		pose(p, POSE_THRUST, 0);
		SwordKit.fx(p, "pursuit", "azure", 0, p.position(), SwordKit.forward(p), PURSUIT_TICKS + 6, 0, 2.2);
		SwordKit.sound(p, p.position(), layer(SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 1.3f), later(SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 1.5f, 3),
				later(SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 1.7f, 6), layer(SoundEvents.AMETHYST_CLUSTER_BREAK, 0.8f, 1.4f));
		dash(ctx, "azure", 2, p.getLookAngle(), 11, PURSUIT_TICKS, 1.9, 15f, (t, at) -> {
			SwordKit.fx(p, "cut", "azure", 1, t.position().add(0, t.getBbHeight() * 0.6, 0), SwordKit.forward(p), 1.4);
			SwordKit.push(t, at, 0.5, 0.3);
		});
		return 0;
	}

	// =================================================================================================== Boiler Pipe

	static List<AbilityDef> boilerPipe() {
		return List.of(def("boiler_pipe_ignition_strike", 1, 7.5, 15, SwordMovesA::ignitionStrike), def("boiler_pipe_ember_serpent", 50, 10.5, 25, SwordMovesA::emberSerpent));
	}

	/**
	 * Z: a short advancing strike. On contact the enemy is pinned, a dragon-shaped burst of orange fire rises round it
	 * and detonates; on a miss, a small burst of sparks.
	 */
	static int ignitionStrike(AbilityContext ctx) {
		ServerPlayer p = ctx.player();
		pose(p, POSE_THRUST, 2);
		SwordKit.sound(p, p.position(), layer(SoundEvents.PLAYER_ATTACK_STRONG, 1.0f, 0.9f));
		boolean[] landed = {false};
		dash(ctx, "fire", 0, p.getLookAngle(), 3.2, 3, 1.1, 4f, (t, at) -> {
			if (landed[0]) {
				return;
			}
			landed[0] = true;
			Stuns.apply(t, 14);
			Vec3 body = t.position().add(0, t.getBbHeight() * 0.5, 0);
			SwordKit.fx(p, "dragon", "fire", 0, body, SwordKit.forward(p), 12);
			SwordKit.sound(p, body, layer(SoundEvents.BLAZE_SHOOT, 1.0f, 0.7f), layer(SoundEvents.FIRE_AMBIENT, 1.0f, 1.0f),
					later(SoundEvents.ENDER_DRAGON_GROWL, 0.35f, 1.8f, 4));
			TimedEffects.later(12, () -> {
				SwordKit.fx(p, "blast", "fire", 0, body, Vec3.ZERO, 2.8);
				SwordKit.sound(p, body, layer(SoundEvents.GENERIC_EXPLODE, 0.9f, 1.1f), layer(SoundEvents.FIRECHARGE_USE, 1.0f, 0.7f));
				for (LivingEntity e : SwordKit.sphere(p, body, 2.8)) {
					SwordKit.hitFresh(ctx, e, e == t ? 9f : 5f);
					e.setSecondsOnFire(3);
					SwordKit.push(e, body.subtract(0, 0.5, 0), 0.6, 0.4);
				}
			});
		});
		TimedEffects.later(4, () -> {
			if (!landed[0] && !SwordKit.gone(p)) {
				Vec3 tip = SwordKit.chest(p).add(SwordKit.forward(p).scale(1.4));
				SwordKit.fx(p, "sparks", "fire", 0, tip, SwordKit.forward(p), 1.0);
				SwordKit.sound(p, tip, layer(SoundEvents.FLINTANDSTEEL_USE, 1.0f, 1.2f), layer(SoundEvents.ANVIL_PLACE, 0.2f, 2.0f));
			}
		});
		return 0;
	}

	/** X: a serpent of flame that winds toward the crosshair, carries enemies along, then explodes and keeps burning. */
	static int emberSerpent(AbilityContext ctx) {
		ServerPlayer p = ctx.player();
		pose(p, POSE_SLASH_R, 4);
		SwordKit.sound(p, p.position(), layer(SoundEvents.BLAZE_SHOOT, 1.0f, 0.6f), later(SoundEvents.FIRE_AMBIENT, 1.0f, 0.8f, 2),
				later(SoundEvents.ENDER_DRAGON_GROWL, 0.3f, 2.0f, 4));
		TimedEffects.later(4, () -> {
			if (SwordKit.gone(p)) {
				return;
			}
			Vec3 start = SwordKit.chest(p).add(SwordKit.forward(p).scale(1.0));
			Vec3 dir = aim(p, start, 18);
			SwordKit.fx(p, "serpent", "fire", 0, start, dir, 0.9, 18, 1.2);
			SwordShot.of(p, start, dir, 0.9, 18, 1.3).carrying()
					.onHit((t, at) -> {
						SwordKit.hitFresh(ctx, t, 5f);
						t.setSecondsOnFire(2);
					})
					.onEnd((at, wall, t) -> {
						end(p, "fire", at, 2.0);
						SwordKit.fx(p, "blast", "fire", 1, at, Vec3.ZERO, 3.5);
						SwordKit.sound(p, at, layer(SoundEvents.GENERIC_EXPLODE, 1.0f, 0.9f), layer(SoundEvents.BLAZE_SHOOT, 0.8f, 0.5f));
						List<LivingEntity> burnt = SwordKit.sphere(p, at, 3.5);
						for (LivingEntity e : burnt) {
							SwordKit.hitFresh(ctx, e, 12f);
							e.setSecondsOnFire(5);
							SwordKit.push(e, at.subtract(0, 0.5, 0), 0.9, 0.5);
						}
						// The flames keep burning: two more fire hits after the explosion.
						for (int k = 1; k <= 2; k++) {
							TimedEffects.later(k * 12, () -> {
								for (LivingEntity e : burnt) {
									if (e.isAlive()) {
										SwordKit.hitFresh(ctx, e, 2.5f);
										SwordKit.fx(p, "embers", "fire", 0, e.position().add(0, e.getBbHeight() * 0.5, 0), Vec3.ZERO, 0.8);
									}
								}
							});
						}
					}).fire();
		});
		return 0;
	}

	// =================================================================================================== Stonewall Blade

	static List<AbilityDef> stonewallBlade() {
		return List.of(def("stonewall_blade_azure_rupture", 35, 9, 15, SwordMovesA::azureRupture), def("stonewall_blade_rising_vortex", 80, 15, 25, SwordMovesA::risingVortex));
	}

	static final int RUPTURE_WINDUP = 6;
	static final double RUPTURE_RADIUS = 6.0;
	static final double RUPTURE_SPEED = 0.75;

	/** Z: the blade driven into the ground: a blue shockwave rolls outward, hitting, pinning and pushing enemies. */
	static int azureRupture(AbilityContext ctx) {
		ServerPlayer p = ctx.player();
		pose(p, POSE_GROUND, RUPTURE_WINDUP);
		SwordKit.sound(p, p.position(), layer(SoundEvents.ARMOR_EQUIP_NETHERITE, 1.0f, 0.7f));
		TimedEffects.later(RUPTURE_WINDUP, () -> {
			if (SwordKit.gone(p)) {
				return;
			}
			Vec3 c = SwordKit.ground(p.serverLevel(), p.position().add(SwordKit.forward(p).scale(0.8)));
			SwordKit.fx(p, "shockwave", "warden", 0, c, Vec3.ZERO, RUPTURE_SPEED, RUPTURE_RADIUS, 1.6);
			SwordKit.sound(p, c, layer(SoundEvents.ANVIL_LAND, 0.8f, 0.8f), layer(SoundEvents.BEACON_DEACTIVATE, 1.0f, 1.6f),
					layer(SoundEvents.GENERIC_EXPLODE, 0.4f, 1.4f));
			Set<Integer> struck = new HashSet<>();
			TimedEffects.start(age -> {
				double r = RUPTURE_SPEED * (age + 1);
				for (LivingEntity e : SwordKit.arc(p, c, Vec3.ZERO, r, 180, 1.5, 2.5)) {
					if (struck.add(e.getId()) && SwordKit.hitFresh(ctx, e, 13f)) {
						SwordKit.push(e, c, 0.85, 0.35);
						Stuns.apply(e, 22, 6);
					}
				}
				return r < RUPTURE_RADIUS;
			});
		});
		return 0;
	}

	static final double TORNADO_SPEED = 0.45;
	static final double TORNADO_RANGE = 16;

	/** X: a travelling blue-white tornado that lifts enemies into the air and grinds them as it advances. */
	static int risingVortex(AbilityContext ctx) {
		ServerPlayer p = ctx.player();
		pose(p, POSE_SLASH_L, 4);
		SwordKit.sound(p, p.position(), layer(SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 0.8f), later(SoundEvents.ELYTRA_FLYING, 0.8f, 0.7f, 4));
		TimedEffects.later(4, () -> {
			if (SwordKit.gone(p)) {
				return;
			}
			Vec3 f = SwordKit.forward(p);
			Vec3 start = p.position().add(f.scale(1.5));
			SwordKit.fx(p, "tornado", "warden", 0, start, f, TORNADO_SPEED, TORNADO_RANGE, 1.8);
			java.util.Map<Integer, Integer> lifted = new java.util.HashMap<>();
			SwordShot.of(p, start.add(0, 1.0, 0), f, TORNADO_SPEED, TORNADO_RANGE, 1.6).carrying()
					.onHit((t, at) -> {
						SwordKit.hitFresh(ctx, t, 4f);
						lifted.put(t.getId(), 0);
					})
					.onTick((at, age, carried) -> {
						for (LivingEntity e : carried) {
							int n = lifted.merge(e.getId(), 1, Integer::sum);
							double h = Math.min(3.5, n * 0.18);
							double spin = n * 0.7;
							SwordKit.carry(e, at.add(Math.cos(spin) * 0.8, h - 0.5, Math.sin(spin) * 0.8));
							if (n % 6 == 0) {
								SwordKit.hitFresh(ctx, e, 3.2f);
							}
						}
						if (age % 10 == 0) {
							SwordKit.sound(p, at, layer(SoundEvents.ELYTRA_FLYING, 0.4f, 0.6f + age * 0.01f));
						}
					})
					.onEnd((at, wall, t) -> {
						end(p, "warden", at, 1.5);
						SwordKit.sound(p, at, layer(SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.0f, 0.8f));
						for (LivingEntity e : SwordKit.sphere(p, at, 2.4)) {
							SwordKit.launch(e, f.scale(0.3).add(0, 0.25, 0));
						}
					}).fire();
		});
		return 0;
	}
}
