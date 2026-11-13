package com.grandline.player.loadout;

import com.grandline.data.ModAttachments;
import com.grandline.network.ServerRequestGuard;
import com.grandline.network.payload.LoadoutOrderPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/** Saves each player's loadout order (persistent, survives death) and sends it back on join. */
public final class LoadoutOrderService {
	private LoadoutOrderService() {
	}

	public static void register() {
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> sync(handler.getPlayer()));
		ServerPlayNetworking.registerGlobalReceiver(LoadoutOrderPayload.TYPE, (packet, player, sender) -> {
			if (ServerRequestGuard.check(player, LoadoutOrderPayload.TYPE) != ServerRequestGuard.Result.ACCEPTED) {
				sync(player); // keep the client on the saved order if the request was dropped
				return;
			}
			if (!set(player, packet.order())) {
				ServerRequestGuard.reject(player, LoadoutOrderPayload.TYPE, ServerRequestGuard.Result.REJECTED_INVALID);
				sync(player);
			}
		});
	}

	public static List<Integer> get(ServerPlayer player) {
		return LoadoutOrder.sanitize(player.getAttached(ModAttachments.LOADOUT_ORDER));
	}

	/** Saves {@code order} if it is a valid permutation. Returns false (and changes nothing) otherwise. */
	public static boolean set(ServerPlayer player, List<Integer> order) {
		if (!LoadoutOrder.isValid(order)) {
			return false;
		}
		player.setAttached(ModAttachments.LOADOUT_ORDER, List.copyOf(order));
		return true;
	}

	public static void sync(ServerPlayer player) {
		if (ServerPlayNetworking.canSend(player, LoadoutOrderPayload.TYPE)) {
			ServerPlayNetworking.send(player, new LoadoutOrderPayload(get(player)));
		}
	}
}
