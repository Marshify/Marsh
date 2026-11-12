package com.grandline.combat.ability;

import com.grandline.network.ServerRequestGuard;
import com.grandline.network.payload.AbilityActivationRequestPayload;
import net.minecraft.server.level.ServerPlayer;

/**
 * Server-side validation pipeline for ability activation requests.
 *
 * <p>Milestone 1 status: the pipeline (player state, rate limit and slot validation) is complete, but no abilities
 * exist yet, so no slot is bound and every request ends with {@link Outcome#NO_ABILITY_BOUND}. The fruit and combat
 * milestones will plug ability lookup, cooldown checks and execution into {@link #handle}.
 */
public final class AbilityActivationHandler {
	/** Number of ability hotbar slots a player can trigger. */
	public static final int SLOT_COUNT = 4;

	private AbilityActivationHandler() {
	}

	public enum Outcome {
		REJECTED_BY_GUARD,
		INVALID_SLOT,
		NOT_ALLOWED,
		NO_ABILITY_BOUND
	}

	public static Outcome handle(ServerPlayer player, AbilityActivationRequestPayload request) {
		if (ServerRequestGuard.check(player, AbilityActivationRequestPayload.TYPE) != ServerRequestGuard.Result.ACCEPTED) {
			return Outcome.REJECTED_BY_GUARD;
		}

		if (request.slot() < 0 || request.slot() >= SLOT_COUNT) {
			ServerRequestGuard.reject(player, AbilityActivationRequestPayload.TYPE, ServerRequestGuard.Result.REJECTED_INVALID);
			return Outcome.INVALID_SLOT;
		}

		if (!player.isAlive() || player.isSpectator()) {
			ServerRequestGuard.reject(player, AbilityActivationRequestPayload.TYPE, ServerRequestGuard.Result.REJECTED_PLAYER_STATE);
			return Outcome.NOT_ALLOWED;
		}

		// No ability system yet: nothing is bound to any slot.
		return Outcome.NO_ABILITY_BOUND;
	}
}
