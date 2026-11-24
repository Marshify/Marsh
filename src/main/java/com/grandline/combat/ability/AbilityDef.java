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
 * @param instinctBreaking hits from this move cannot be evaded by Instinct
 */
public record AbilityDef(String key, int requiredMastery, int cooldownTicks, double energyCost, Activation activation, AbilityAction action,
		boolean instinctBreaking) {
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
		this(key, requiredMastery, cooldownTicks, energyCost, Activation.INSTANT, action, false);
	}

	public AbilityDef(String key, int requiredMastery, int cooldownTicks, double energyCost, Activation activation, AbilityAction action) {
		this(key, requiredMastery, cooldownTicks, energyCost, activation, action, false);
	}

	/** This move, but its hits break through Instinct. */
	public AbilityDef breaksInstinct() {
		return new AbilityDef(key, requiredMastery, cooldownTicks, energyCost, activation, action, true);
	}

	/** The key of an unused slot (a power with fewer moves keeps its later moves on their usual keys). */
	public static final String EMPTY_KEY = "empty";

	/** An unused slot: nothing to activate, not shown in the HUD, tooltips or shops. */
	public static AbilityDef empty() {
		return new AbilityDef(EMPTY_KEY, Integer.MAX_VALUE, 0, 0, ctx -> 0);
	}

	public boolean isEmpty() {
		return EMPTY_KEY.equals(key);
	}

	public String translationKey() {
		return "ability.grandline." + key;
	}
}
