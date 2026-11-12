package com.grandline.combat.ability;

/**
 * One move of a power.
 *
 * @param key             unique name within the power, also used for the translation key {@code ability.grandline.<key>}
 * @param requiredMastery mastery level needed to use it
 * @param cooldownTicks   server-enforced cooldown (charged moves: starts on release; toggles: starts when switched off)
 * @param energyCost      fraction of the player's max energy consumed (0.1 = 10%)
 * @param activation      how the key is used: tap, hold-and-release, or on/off
 * @param action          what happens
 */
public record AbilityDef(String key, int requiredMastery, int cooldownTicks, double energyCost, Activation activation, AbilityAction action) {
	/** How a move is triggered. */
	public enum Activation {
		/** Fires immediately on key press. */
		INSTANT,
		/** Starts charging on press, fires on release (or at full charge); {@link AbilityContext#charge()} is 0..1. */
		CHARGED,
		/** Press to switch on, press again (or run out of energy) to switch off. */
		TOGGLE
	}

	public AbilityDef(String key, int requiredMastery, int cooldownTicks, double energyCost, AbilityAction action) {
		this(key, requiredMastery, cooldownTicks, energyCost, Activation.INSTANT, action);
	}

	public String translationKey() {
		return "ability.grandline." + key;
	}
}
