package com.grandline.network;

import com.grandline.combat.ability.AbilityActivationHandler;
import com.grandline.combat.ability.AbilityCooldownService;
import com.grandline.network.payload.AbilityActivationRequestPayload;
import com.grandline.network.payload.ActionResultPayload;
import com.grandline.network.payload.AllocateStatPayload;
import com.grandline.network.payload.FruitActionPayload;
import com.grandline.network.payload.GachaResultPayload;
import com.grandline.network.payload.RequestProfileSyncPayload;
import com.grandline.GrandLineMod;
import com.grandline.fruit.FruitDealer;
import com.grandline.fruit.FruitService;
import com.grandline.player.PlayerProfileService;
import com.grandline.player.StatRules;
import com.grandline.player.StatType;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/**
 * Server-side registration point for every client → server packet.
 *
 * <p>Direction is enforced by registration: the server only registers receivers for the intent packets below
 * (profile sync request, ability activation, stat allocation, fruit actions).
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

		ServerPlayNetworking.registerGlobalReceiver(com.grandline.network.payload.AbilityReleasePayload.TYPE,
				(packet, player, responseSender) -> AbilityActivationHandler.release(player, packet));

		ServerPlayNetworking.registerGlobalReceiver(AllocateStatPayload.TYPE,
				(packet, player, responseSender) -> handleAllocateStat(player, packet));

		ServerPlayNetworking.registerGlobalReceiver(com.grandline.network.payload.NpcActionPayload.TYPE,
				(packet, player, responseSender) -> com.grandline.npc.NpcService.handle(player, packet));

		ServerPlayNetworking.registerGlobalReceiver(FruitActionPayload.TYPE,
				(packet, player, responseSender) -> handleFruitAction(player, packet));
	}

	/** Validates and runs a fruit / shop / gacha / stash / refund request. Returns the result (exposed for tests). */
	public static FruitService.Result handleFruitAction(ServerPlayer player, FruitActionPayload packet) {
		if (ServerRequestGuard.check(player, FruitActionPayload.TYPE) != ServerRequestGuard.Result.ACCEPTED) {
			return null;
		}

		FruitService.Result result;
		switch (packet.action()) {
			case FruitActionPayload.HELD_EAT, FruitActionPayload.HELD_STORE, FruitActionPayload.HELD_DROP -> {
				FruitService.FruitAction action = FruitService.FruitAction.byId(packet.action());
				result = FruitService.useHeldFruit(player, packet.arg() == 0, action);
			}
			case FruitActionPayload.STASH_WITHDRAW -> result = FruitService.withdrawFromStash(player, packet.fruitId());
			case FruitActionPayload.SHOP_BUY -> result = nearDealer(player, packet.arg())
					? FruitService.buy(player, packet.fruitId()) : FruitService.Result.NOT_IN_STOCK;
			case FruitActionPayload.GACHA_ROLL -> {
				if (!nearDealer(player, packet.arg())) {
					result = FruitService.Result.NOT_IN_STOCK;
				} else {
					FruitService.GachaRoll roll = FruitService.rollGacha(player);
					result = roll.result();
					if (roll.fruit() != null && ServerPlayNetworking.canSend(player, GachaResultPayload.TYPE)) {
						ServerPlayNetworking.send(player, new GachaResultPayload(roll.fruit().id()));
					}
				}
			}
			case FruitActionPayload.REFUND_STATS -> result = refundStats(player);
			default -> {
				ServerRequestGuard.reject(player, FruitActionPayload.TYPE, ServerRequestGuard.Result.REJECTED_INVALID);
				return null;
			}
		}

		if (result != FruitService.Result.SUCCESS) {
			ServerRequestGuard.reject(player, FruitActionPayload.TYPE, ServerRequestGuard.Result.REJECTED_INVALID);
		}
		if (ServerPlayNetworking.canSend(player, ActionResultPayload.TYPE)) {
			ServerPlayNetworking.send(player, new ActionResultPayload(result == FruitService.Result.SUCCESS,
					"result.grandline." + result.name().toLowerCase(java.util.Locale.ROOT)));
		}
		return result;
	}

	private static boolean nearDealer(ServerPlayer player, int entityId) {
		Entity entity = player.serverLevel().getEntity(entityId);
		return entity instanceof FruitDealer dealer && dealer.isFruitDealer() && entity.isAlive()
				&& entity.distanceTo(player) <= FruitDealer.INTERACTION_RANGE;
	}

	private static FruitService.Result refundStats(ServerPlayer player) {
		long cost = GrandLineMod.config().statRefundCost;
		if (PlayerProfileService.get(player).currency() < cost) {
			return FruitService.Result.NOT_ENOUGH_CURRENCY;
		}
		PlayerProfileService.addCurrency(player, -cost);
		PlayerProfileService.resetStats(player);
		return FruitService.Result.SUCCESS;
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
