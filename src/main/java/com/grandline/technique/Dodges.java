package com.grandline.technique;

import com.grandline.combat.fx.FxEvents;
import com.grandline.entity.Islander;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Where techniques meet damage (called from TechniqueDamageMixin, in this order):
 * <ol>
 *   <li>{@link #evades}: Instinct evades an eligible attack (a charge is spent, the hit and its knockback and effects
 *   are cancelled), (Elemental Reflex is checked first: see ElementalReflex).</li>
 *   <li>{@link #modify}: Aura adds its physical damage bonus to the attacker's hit and takes its reduction off hits
 *   on the wearer. Temporary: stats are never changed.</li>
 *   <li>{@link #afterHurt}: a landed physical hit with Aura on gives Aura XP (capped, and spaced out).</li>
 * </ol>
 */
public final class Dodges {
	/** Entity → game time of its last evasion: knockback and effects arriving the same tick are dropped. */
	private static final Map<Entity, Long> EVADED = new WeakHashMap<>();

	private Dodges() {
	}

	static void register() {
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> EVADED.clear());
	}

	/** Whether {@code target} just evaded a hit (this tick or the last): follow-up pushes, stuns and effects skip it. */
	public static boolean justEvaded(Entity target) {
		Long t = EVADED.get(target);
		return t != null && target.level().getGameTime() - t <= 1;
	}

	/** True if the hit should be cancelled entirely. */
	public static boolean evades(LivingEntity target, DamageSource source, float amount) {
		if (!(target instanceof ServerPlayer player) || target.level().isClientSide) {
			return false;
		}
		// Elemental Reflex first: a hit the body already lets through never spends an Instinct charge.
		if (com.grandline.combat.ElementalReflex.negates(player, source)) {
			return true;
		}
		if (justEvaded(player) && CombatContext.combat(source, player) && !CombatContext.instinctBreaking()) {
			return true; // the rest of the attack that was just evaded (a multi-hit move landing the same tick)
		}
		if (TechniqueService.instinctOn(player) && CombatContext.combat(source, player) && !CombatContext.instinctBreaking()
				&& !(player.invulnerableTime > 10)) {
			Techniques t = TechniqueService.get(player);
			if (t.dodges() > 0) {
				TechniqueService.update(player, x -> x.withDodges(x.dodges() - 1, x.regenTicks()).withInstinctXp(x.instinctXp() + 1));
				EVADED.put(player, player.level().getGameTime());
				Vec3 away = source.getSourcePosition() == null ? player.getLookAngle().scale(-1)
						: player.position().subtract(source.getSourcePosition()).multiply(1, 0, 1);
				FxEvents.send(player, "tq:dodge", player.position().add(0, 0.9, 0), away.lengthSqr() < 1e-6 ? Vec3.ZERO : away.normalize(), 0, 0,
						player.getYRot());
				player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_NODAMAGE, SoundSource.PLAYERS, 1.0f, 1.6f);
				player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PHANTOM_FLAP, SoundSource.PLAYERS, 0.6f, 2.0f);
				TechniqueService.sync(player);
				return true;
			}
		}
		return false;
	}

	/** Aura's temporary bonus and reduction. */
	public static float modify(LivingEntity target, DamageSource source, float amount) {
		if (target.level().isClientSide) {
			return amount;
		}
		ServerPlayer attacker = CombatContext.playerAttacker(source);
		if (attacker != null && attacker != target && TechniqueService.auraOn(attacker) && CombatContext.physical(source)) {
			amount *= (float) (1 + TechniqueRules.auraDamageBonus(TechniqueService.auraStage(attacker)));
		}
		if (target instanceof ServerPlayer wearer && TechniqueService.auraOn(wearer) && CombatContext.combat(source, wearer)) {
			amount *= (float) (1 - TechniqueRules.auraReduction(TechniqueService.auraStage(wearer)));
		}
		return amount;
	}

	/** Aura XP for a landed physical hit on a real opponent. */
	public static void afterHurt(LivingEntity target, DamageSource source, float amount, boolean landed) {
		if (!landed || target.level().isClientSide || amount <= 0) {
			return;
		}
		ServerPlayer attacker = CombatContext.playerAttacker(source);
		if (attacker == null || attacker == target || !TechniqueService.auraOn(attacker) || !CombatContext.physical(source) || !worthXp(attacker, target)) {
			return;
		}
		TechniqueService.Runtime r = TechniqueService.runtime(attacker);
		long now = TechniqueService.now(attacker);
		var config = TechniqueService.config();
		if (now - r.lastAuraXpTick < config.auraXpHitGapTicks) {
			return; // a burst of hits in the same instant counts once
		}
		r.lastAuraXpTick = now;
		long xp = Math.max(1, Math.min(config.auraXpPerHitCap, Math.round(amount)));
		TechniqueService.addAuraXp(attacker, xp);
	}

	/** Real opponents only: no shopkeepers or islanders, armour stands, or players on the attacker's own side. */
	static boolean worthXp(ServerPlayer attacker, LivingEntity target) {
		if (target instanceof Islander || target instanceof ArmorStand || target.isInvulnerable()) {
			return false;
		}
		if (target instanceof ServerPlayer other) {
			var a = com.grandline.player.TeamService.get(attacker);
			return a == null || a != com.grandline.player.TeamService.get(other);
		}
		return true;
	}
}
