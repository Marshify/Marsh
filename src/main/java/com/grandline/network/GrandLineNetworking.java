package com.grandline.network;

import com.grandline.combat.ability.AbilityActivationHandler;
import com.grandline.combat.ability.AbilityCooldownService;
import com.grandline.network.payload.AbilityActivationRequestPayload;
import com.grandline.network.payload.AllocateStatPayload;
import com.grandline.network.payload.RequestProfileSyncPayload;
import com.grandline.player.PlayerProfileService;
import com.grandline.player.StatRules;
import com.grandline.player.StatType;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

/**
 * Server-side registration point for every client → server packet.
 *
 * <p>Direction is enforced by registration: the server only registers receivers for the intent packets below
 * (profile sync request, ability activation, stat allocation).
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

		ServerPlayNetworking.registerGlobalReceiver(AllocateStatPayload.TYPE,
				(packet, player, responseSender) -> handleAllocateStat(player, packet));
	}

	/** Validates and applies a stat allocation request. Returns the outcome (exposed for tests). */
	public static StatRules.AllocationResult handleAllocateStat(ServerPlayer player, AllocateStatPayload packet) {
		if (ServerRequestGuard.check(player, AllocateStatPayload.TYPE) != ServerRequestGuard.Result.ACCEPTED) {
			PlayerProfileService.sync(player); // undo any optimistic client display
			return StatRules.AllocationResult.INVALID_AMOUNT;
		}

		StatType stat = StatType.byId(packet.statId());
		if (stat == null || packet.amount() <= 0 || packet.amount() > AllocateStatPayload.MAX_AMOUNT_PER_REQUEST) {
			ServerRequestGuard.reject(player, AllocateStatPayload.TYPE, ServerRequestGuard.Result.REJECTED_INVALID);
			PlayerProfileService.sync(player);
			return StatRules.AllocationResult.INVALID_AMOUNT;
		}

		StatRules.AllocationResult result = PlayerProfileService.allocateStat(player, stat, packet.amount());
		if (result != StatRules.AllocationResult.SUCCESS) {
			ServerRequestGuard.reject(player, AllocateStatPayload.TYPE, ServerRequestGuard.Result.REJECTED_INVALID);
		}
		return result;
	}
}
