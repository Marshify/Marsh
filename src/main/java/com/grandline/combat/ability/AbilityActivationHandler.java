package com.grandline.combat.ability;

import com.grandline.GrandLineMod;
import com.grandline.combat.EnergyService;
import com.grandline.network.ServerRequestGuard;
import com.grandline.network.payload.AbilityActivationRequestPayload;
import com.grandline.player.PlayerProfile;
import com.grandline.player.PlayerProfileService;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Server-side pipeline for move activation requests: guard → slot → player state → power ownership → mastery →
 * cooldown → energy → execute → cooldown + mastery experience. Every check uses server data only.
 */
public final class AbilityActivationHandler {
	/** Number of move slots (Z, X, C, V, R). */
	public static final int SLOT_COUNT = Power.SLOT_COUNT;

	private AbilityActivationHandler() {
	}

	public enum Outcome {
		REJECTED_BY_GUARD,
		INVALID_SLOT,
		NOT_ALLOWED,
		NO_POWER,
		LOCKED,
		ON_COOLDOWN,
		NO_ENERGY,
		USED
	}

	public static Outcome handle(ServerPlayer player, AbilityActivationRequestPayload request) {
		if (ServerRequestGuard.check(player, AbilityActivationRequestPayload.TYPE) != ServerRequestGuard.Result.ACCEPTED) {
			return Outcome.REJECTED_BY_GUARD;
		}

		PowerSource source = PowerSource.byId(request.source());
		if (source == null || request.slot() < 0 || request.slot() >= SLOT_COUNT) {
			ServerRequestGuard.reject(player, AbilityActivationRequestPayload.TYPE, ServerRequestGuard.Result.REJECTED_INVALID);
			return Outcome.INVALID_SLOT;
		}

		if (!player.isAlive() || player.isSpectator()) {
			ServerRequestGuard.reject(player, AbilityActivationRequestPayload.TYPE, ServerRequestGuard.Result.REJECTED_PLAYER_STATE);
			return Outcome.NOT_ALLOWED;
		}

		PlayerProfile profile = PlayerProfileService.get(player);
		Power power = resolve(profile, source);
		if (power == null) {
			tell(player, "message.grandline.ability.no_fruit");
			return Outcome.NO_POWER;
		}

		if (request.slot() >= power.abilities().size()) {
			ServerRequestGuard.reject(player, AbilityActivationRequestPayload.TYPE, ServerRequestGuard.Result.REJECTED_INVALID);
			return Outcome.INVALID_SLOT;
		}
		AbilityDef ability = power.abilities().get(request.slot());

		// A toggle that is already on is switched off without any further checks.
		if (ability.activation() == AbilityDef.Activation.TOGGLE && FlightService.isFlying(player)) {
			FlightService.stop(player, true);
			return Outcome.USED;
		}
		if (ChargeService.isCharging(player)) {
			return Outcome.NOT_ALLOWED; // already winding up another move
		}
		int mastery = masteryRules().level(profile.powers().masteryXp(power.id().toString()));
		if (mastery < ability.requiredMastery()) {
			tell(player, "message.grandline.ability.locked", ability.requiredMastery());
			return Outcome.LOCKED;
		}

		ResourceLocation abilityId = power.abilityId(ability);
		if (AbilityCooldownService.isOnCooldown(player, abilityId)) {
			return Outcome.ON_COOLDOWN;
		}

		double multiplier = PlayerProfileService.statRules().abilityMultiplier(profile.stats(), power.scalingStat());

		if (ability.activation() == AbilityDef.Activation.CHARGED) {
			// Energy is checked now and spent on release; the server times the charge itself.
			if (EnergyService.current(player) + 1.0e-9 < EnergyService.max(player) * ability.energyCost()) {
				tell(player, "message.grandline.ability.no_energy");
				return Outcome.NO_ENERGY;
			}
			ChargeService.begin(player, power, ability, request.slot(), multiplier);
			return Outcome.USED;
		}

		if (!EnergyService.trySpend(player, ability.energyCost())) {
			tell(player, "message.grandline.ability.no_energy");
			return Outcome.NO_ENERGY;
		}
		ability.action().execute(new AbilityContext(player, power, ability, multiplier));
		if (ability.activation() == AbilityDef.Activation.INSTANT) {
			AbilityCooldownService.start(player, abilityId, ability.cooldownTicks()); // toggles start theirs when switched off
		}

		return Outcome.USED;
	}

	/** Handles the release of a held key: fires the charged move with the server-measured charge. */
	public static Outcome release(ServerPlayer player, com.grandline.network.payload.AbilityReleasePayload request) {
		if (request.slot() < 0 || request.slot() >= SLOT_COUNT) {
			return Outcome.INVALID_SLOT;
		}
		return releaseCharge(player, request.slot());
	}

	/** Fires the move the player is charging in {@code slot}. Also used for the automatic release at full charge. */
	public static Outcome releaseCharge(ServerPlayer player, int slot) {
		ChargeService.Charging charging = ChargeService.end(player, slot);
		if (charging == null) {
			return Outcome.NOT_ALLOWED;
		}
		if (!player.isAlive()) {
			return Outcome.NOT_ALLOWED;
		}
		if (!EnergyService.trySpend(player, charging.ability().energyCost())) {
			tell(player, "message.grandline.ability.no_energy");
			return Outcome.NO_ENERGY;
		}
		double charge = charging.charge(player.server.getTickCount());
		charging.ability().action().execute(new AbilityContext(player, charging.power(), charging.ability(), charging.multiplier(), charge));
		AbilityCooldownService.start(player, charging.power().abilityId(charging.ability()), charging.ability().cooldownTicks());
		return Outcome.USED;
	}

	/** The power the player owns for {@code source}, or null if they have none (or it no longer exists). */
	public static Power resolve(PlayerProfile profile, PowerSource source) {
		return switch (source) {
			case STYLE -> Powers.get(profile.powers().fightingStyle());
			case FRUIT -> profile.powers().hasFruit() ? Powers.get(profile.powers().fruit()) : null;
		};
	}

	public static MasteryRules masteryRules() {
		return MasteryRules.fromConfig(GrandLineMod.config());
	}

	private static void tell(ServerPlayer player, String key, Object... args) {
		player.displayClientMessage(Component.translatable(key, args).withStyle(ChatFormatting.RED), true);
	}
}
