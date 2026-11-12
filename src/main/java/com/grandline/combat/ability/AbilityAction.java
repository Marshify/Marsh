package com.grandline.combat.ability;

/** The server-side effect of a move. */
@FunctionalInterface
public interface AbilityAction {
	/**
	 * Executes the move. Returns the number of entities hit (0 is a valid "whiff"; the cooldown still starts).
	 */
	int execute(AbilityContext context);
}
