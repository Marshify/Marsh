package com.grandline.network;

import com.grandline.combat.ability.AbilityActivationHandler;
import com.grandline.combat.ability.AbilityCooldownService;
import com.grandline.network.payload.AbilityActivationRequestPayload;
import com.grandline.network.payload.CooldownSyncPayload;
import com.grandline.network.payload.ProfileSyncPayload;
import com.grandline.network.payload.RequestProfileSyncPayload;
import com.grandline.player.PlayerProfileService;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

/**
 * Central registration point for every Grand Line packet.
 *
 * <p>Direction is enforced by registration: clientbound payloads are only registered with
 * {@link PayloadTypeRegistry#clientboundPlay()}, so the server has no decoder (and no handler) for them. A modified
 * client therefore cannot push profile or cooldown data to the server; it can only send the intent payloads
 * registered with {@link PayloadTypeRegistry#serverboundPlay()}, each of which is validated server-side.
 */
public final class GrandLineNetworking {
	private GrandLineNetworking() {
	}

	/** Registers payload types. Runs on both physical sides. */
	public static void registerCommon() {
		// Server -> client
		PayloadTypeRegistry.clientboundPlay().register(ProfileSyncPayload.TYPE, ProfileSyncPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(CooldownSyncPayload.TYPE, CooldownSyncPayload.CODEC);

		// Client -> server
		PayloadTypeRegistry.serverboundPlay().register(RequestProfileSyncPayload.TYPE, RequestProfileSyncPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(AbilityActivationRequestPayload.TYPE, AbilityActivationRequestPayload.CODEC);
	}

	/** Registers server-side handlers for client requests. Handlers run on the server thread. */
	public static void registerServerReceivers() {
		ServerPlayNetworking.registerGlobalReceiver(RequestProfileSyncPayload.TYPE, (payload, context) -> {
			if (ServerRequestGuard.check(context.player(), RequestProfileSyncPayload.TYPE) != ServerRequestGuard.Result.ACCEPTED) {
				return;
			}

			PlayerProfileService.sync(context.player());
			AbilityCooldownService.sync(context.player());
		});

		ServerPlayNetworking.registerGlobalReceiver(AbilityActivationRequestPayload.TYPE,
				(payload, context) -> AbilityActivationHandler.handle(context.player(), payload));
	}
}
