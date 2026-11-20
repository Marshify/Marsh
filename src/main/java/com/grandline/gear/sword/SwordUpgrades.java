package com.grandline.gear.sword;

import com.grandline.GrandLineMod;
import com.grandline.combat.ability.AbilityActivationHandler;
import com.grandline.combat.ability.Power;
import com.grandline.player.PlayerProfileService;
import net.minecraft.server.level.ServerPlayer;

/**
 * Upgraded sword forms (Bisento, Eclipse Blade): a sword is upgraded once its own mastery reaches
 * {@code swordUpgradeMastery} (config, default 300). Upgrading changes how its moves behave; nothing is lost or replaced.
 */
public final class SwordUpgrades {
	private SwordUpgrades() {
	}

	public static int upgradeMastery() {
		return GrandLineMod.config().swordUpgradeMastery;
	}

	public static boolean upgraded(ServerPlayer player, Power power) {
		long xp = PlayerProfileService.get(player).powers().masteryXp(power.id().toString());
		return AbilityActivationHandler.masteryRules().level(xp) >= upgradeMastery();
	}
}
