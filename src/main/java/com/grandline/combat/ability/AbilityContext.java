package com.grandline.combat.ability;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * Everything a move needs while executing on the server. {@link #damage} applies the stat multiplier and attributes
 * the damage to the player, so kills count for rewards and quests.
 */
public record AbilityContext(ServerPlayer player, Power power, AbilityDef ability, double multiplier) {
	public ServerLevel level() {
		return player.serverLevel();
	}

	/** Deals {@code baseDamage × multiplier} to {@code target}. Returns true if the hit landed. */
	public boolean damage(LivingEntity target, float baseDamage) {
		if (target == player || !target.isAlive()) {
			return false;
		}
		return target.hurt(level().damageSources().playerAttack(player), (float) (baseDamage * multiplier));
	}
}
