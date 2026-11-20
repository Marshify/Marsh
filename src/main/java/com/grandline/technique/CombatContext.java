package com.grandline.technique;

import com.grandline.combat.ability.AbilityDef;
import com.grandline.combat.ability.Power;
import com.grandline.player.StatType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;

/**
 * What kind of attack the damage being dealt right now is. Moves run their damage inside {@link #during} so the damage
 * hooks (Aura, Instinct, elemental immunity) know which power and move it came from; anything else (a sword swing, a
 * mob's claw) is read from the damage source. All on the server thread.
 */
public final class CombatContext {
	private static Power power;
	private static AbilityDef ability;
	private static int breaking;

	private CombatContext() {
	}

	/** Runs {@code hit} as damage from {@code power}'s move {@code ability}. */
	public static boolean during(Power p, AbilityDef a, java.util.function.BooleanSupplier hit) {
		Power oldPower = power;
		AbilityDef oldAbility = ability;
		power = p;
		ability = a;
		try {
			return hit.getAsBoolean();
		} finally {
			power = oldPower;
			ability = oldAbility;
		}
	}

	/** Runs {@code hit} as an Instinct-breaking attack (boss finishers and other attacks that are not moves). */
	public static boolean breaking(java.util.function.BooleanSupplier hit) {
		breaking++;
		try {
			return hit.getAsBoolean();
		} finally {
			breaking--;
		}
	}

	/** Whether the damage being dealt now ignores Instinct. */
	public static boolean instinctBreaking() {
		return breaking > 0 || ability != null && ability.instinctBreaking();
	}

	/** The attacking player, if a player (or their projectile) is behind {@code source}. */
	public static ServerPlayer playerAttacker(DamageSource source) {
		return source.getEntity() instanceof ServerPlayer p ? p : null;
	}

	/**
	 * A physical attack: a punch or sword swing, a fighting-style, sword or gun move, or a gun shot. Fruit moves and
	 * environmental damage are not.
	 */
	public static boolean physical(DamageSource source) {
		if (!(source.getEntity() instanceof LivingEntity)) {
			return false;
		}
		if (power != null) {
			StatType stat = power.scalingStat();
			return stat == StatType.MELEE || stat == StatType.SWORD || stat == StatType.GUN;
		}
		return source.is(DamageTypes.PLAYER_ATTACK) || source.is(DamageTypes.MOB_ATTACK) || source.is(DamageTypes.MOB_ATTACK_NO_AGGRO)
				|| source.getDirectEntity() instanceof AbstractArrow;
	}

	/**
	 * Combat damage Instinct can evade: something living attacked (directly or with a projectile or move), as opposed
	 * to the world (falls, drowning, fire, the void, the ocean's weakness) or damage that ignores everything.
	 */
	public static boolean combat(DamageSource source, LivingEntity target) {
		return source.getEntity() instanceof LivingEntity attacker && attacker != target
				&& !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)
				&& !source.is(DamageTypes.FELL_OUT_OF_WORLD) && !source.is(DamageTypes.DROWN) && !source.is(DamageTypes.ON_FIRE)
				&& !source.is(DamageTypes.IN_FIRE) && !source.is(DamageTypes.LAVA) && !source.is(DamageTypes.FALL);
	}
}
