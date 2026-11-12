package com.grandline.network;

import com.grandline.combat.ability.AbilityActivationHandler;
import com.grandline.combat.ability.AbilityCooldownService;
import com.grandline.network.payload.AbilityActivationRequestPayload;
import com.grandline.network.payload.RequestProfileSyncPayload;
import com.grandline.player.PlayerProfileService;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

/**
 * Server-side registration point for every client → server packet.
 *
 * <p>Direction is enforced by registration: the server only registers receivers for the intent packets below.
 * Clientbound packets ({@code ProfileSyncPayload}, {@code CooldownSyncPayload}) have no server receiver, so a
 * modified client cannot push profile or cooldown data to the server. Every receiver validates its input, and
 * {@code FabricPacket} handlers run on the server thread.
 */
public final class GrandLineNetworking {
	private GrandLineNetworking() {
	}

	public static void registerServerReceivers() {
		ServerPlayNetworking.registerGlobalReceiver(RequestProfileSyncPayload.TYPE, (packet, player, responseSender) -> {
			if (ServerRequestGuard.check(player, RequestProfileSyncPayload.TYPE) != ServerRequestGuard.Result.ACCEPTED) {
				return;
			}

			PlayerProfileService.sync(player);
			AbilityCooldownService.sync(player);
		});

		ServerPlayNetworking.registerGlobalReceiver(AbilityActivationRequestPayload.TYPE,
				(packet, player, responseSender) -> AbilityActivationHandler.handle(player, packet));
	}
}
