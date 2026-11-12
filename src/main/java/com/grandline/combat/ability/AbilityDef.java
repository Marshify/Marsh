package com.grandline.combat.ability;

/**
 * One move of a power.
 *
 * @param key             unique name within the power, also used for the translation key {@code ability.grandline.<key>}
 * @param requiredMastery mastery level needed to use it
 * @param cooldownTicks   server-enforced cooldown
 * @param energyCost      fraction of the player's max energy consumed (0.1 = 10%)
 * @param action          what happens
 */
public record AbilityDef(String key, int requiredMastery, int cooldownTicks, double energyCost, AbilityAction action) {
	public String translationKey() {
		return "ability.grandline." + key;
	}
}
