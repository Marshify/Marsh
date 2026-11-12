package com.grandline.client.network;

import com.grandline.GrandLineMod;
import com.grandline.client.ClientProfileCache;
import com.grandline.combat.ability.AbilityActivationHandler;
import com.grandline.combat.ability.PowerSource;
import com.grandline.network.payload.AbilityActivationRequestPayload;
import com.grandline.network.payload.AllocateStatPayload;
import com.grandline.network.payload.CooldownSyncPayload;
import com.grandline.network.payload.EnergySyncPayload;
import com.grandline.network.payload.ProfileSyncPayload;
import com.grandline.network.payload.RequestProfileSyncPayload;
import com.grandline.player.StatType;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/**
 * Client-side packet handling. Receivers only update the read-only {@link ClientProfileCache}; the send helpers only
 * transmit intents that the server validates.
 */
public final class ClientNetworking {
	private ClientNetworking() {
	}

	public static void register() {
		ClientPlayNetworking.registerGlobalReceiver(ProfileSyncPayload.TYPE,
				(packet, player, responseSender) -> {
					GrandLineMod.LOGGER.debug("Received profile sync: {}", packet.profile());
					ClientProfileCache.acceptProfile(packet);
				});
		ClientPlayNetworking.registerGlobalReceiver(CooldownSyncPayload.TYPE,
				(packet, player, responseSender) -> ClientProfileCache.acceptCooldowns(packet.cooldowns()));

		ClientPlayNetworking.registerGlobalReceiver(EnergySyncPayload.TYPE,
				(packet, player, responseSender) -> ClientProfileCache.acceptEnergy(packet.current(), packet.max()));

		ClientPlayNetworking.registerGlobalReceiver(com.grandline.network.payload.IslandListPayload.TYPE,
				(packet, player, responseSender) -> ClientProfileCache.acceptIslands(packet.islands()));

		ClientPlayNetworking.registerGlobalReceiver(com.grandline.network.payload.OpenNpcPayload.TYPE,
				(packet, player, responseSender) -> com.grandline.client.screen.NpcScreens.open(packet));
		ClientPlayNetworking.registerGlobalReceiver(com.grandline.network.payload.GachaResultPayload.TYPE,
				(packet, player, responseSender) -> com.grandline.client.screen.GachaScreen.onResult(packet.fruitId()));
		ClientPlayNetworking.registerGlobalReceiver(com.grandline.network.payload.QuestSyncPayload.TYPE,
				(packet, player, responseSender) -> ClientProfileCache.acceptQuest(packet.quest().orElse(null)));
		ClientPlayNetworking.registerGlobalReceiver(com.grandline.network.payload.ActionResultPayload.TYPE,
				(packet, player, responseSender) -> player.displayClientMessage(net.minecraft.network.chat.Component.translatable(packet.key())
						.withStyle(packet.success() ? net.minecraft.ChatFormatting.GREEN : net.minecraft.ChatFormatting.RED), true));

		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ClientProfileCache.clear());
	}

	/** Asks the server to resend profile and cooldowns. Returns false if the server does not have the mod. */
	public static boolean requestProfileSync() {
		if (!ClientPlayNetworking.canSend(RequestProfileSyncPayload.TYPE)) {
			return false;
		}
		ClientPlayNetworking.send(RequestProfileSyncPayload.INSTANCE);
		return true;
	}

	/** Asks the server to spend {@code amount} unallocated points on {@code stat}. The server validates everything. */
	public static boolean requestStatAllocation(StatType stat, int amount) {
		if (amount <= 0 || amount > AllocateStatPayload.MAX_AMOUNT_PER_REQUEST || !ClientPlayNetworking.canSend(AllocateStatPayload.TYPE)) {
			return false;
		}
		ClientPlayNetworking.send(new AllocateStatPayload(stat.ordinal(), amount));
		return true;
	}

	/** Tells the server a held move key was released (fires charged moves). */
	public static void requestAbilityRelease(PowerSource source, int slot) {
		if (ClientPlayNetworking.canSend(com.grandline.network.payload.AbilityReleasePayload.TYPE)) {
			ClientPlayNetworking.send(new com.grandline.network.payload.AbilityReleasePayload(source.ordinal(), slot));
		}
	}

	/** Sends a move activation intent. The server decides what (if anything) happens. */
	public static boolean requestAbilityActivation(PowerSource source, int slot) {
		if (slot < 0 || slot >= AbilityActivationHandler.SLOT_COUNT || !ClientPlayNetworking.canSend(AbilityActivationRequestPayload.TYPE)) {
			return false;
		}
		ClientPlayNetworking.send(new AbilityActivationRequestPayload(source.ordinal(), slot));
		return true;
	}
}
