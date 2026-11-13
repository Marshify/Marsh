package com.grandline.combat.ability;

import com.grandline.GrandLineMod;
import com.grandline.player.PlayerProfileService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Decides which power earns mastery for a kill: the power whose move landed the last hit (within a few seconds),
 * otherwise the fighting style when the player finished the enemy bare-handed. Only kills give mastery.
 */
public final class KillCredit {
	/** A move hit counts for the kill if it landed within this many ticks before death. */
	public static final int WINDOW_TICKS = 60;

	private record Hit(UUID player, Power power, int tick) {
	}

	private static final Map<LivingEntity, Hit> LAST_HIT = new WeakHashMap<>();

	private KillCredit() {
	}

	static void record(LivingEntity target, ServerPlayer player, Power power) {
		LAST_HIT.put(target, new Hit(player.getUUID(), power, player.server.getTickCount()));
	}

	/** The power to credit for {@code killer} killing {@code target}, or null if none (e.g. a kill with a vanilla tool). */
	public static Power creditedPower(ServerPlayer killer, LivingEntity target) {
		Hit hit = LAST_HIT.remove(target);
		if (hit != null && hit.player().equals(killer.getUUID()) && killer.server.getTickCount() - hit.tick() <= WINDOW_TICKS) {
			return hit.power();
		}
		Power weapon = com.grandline.gear.Gear.powerFor(killer.getMainHandItem().getItem());
		if (weapon != null) {
			return weapon; // melee or shot kills with a sword or gun train that weapon's mastery
		}
		if (killer.getMainHandItem().isEmpty()) {
			return Powers.get(PlayerProfileService.get(killer).powers().fightingStyle());
		}
		return null;
	}

	/** Grants kill mastery. Returns the experience granted. */
	public static long onKill(ServerPlayer killer, LivingEntity target) {
		Power power = creditedPower(killer, target);
		if (power == null) {
			return 0;
		}
		long experience = AbilityActivationHandler.masteryRules().experienceForKill(target.getMaxHealth());
		if (experience > 0) {
			PlayerProfileService.addMastery(killer, power, experience);
			GrandLineMod.LOGGER.debug("{} gained {} {} mastery", killer.getName().getString(), experience, power.id());
		}
		return experience;
	}
}
