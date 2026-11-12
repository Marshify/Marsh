package com.grandline.combat.ability;

import com.grandline.data.ModAttachments;
import com.grandline.network.payload.CooldownSyncPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Server-authoritative ability cooldowns. The client is told about cooldowns for display purposes only; readiness is
 * always decided here.
 */
public final class AbilityCooldownService {
	private AbilityCooldownService() {
	}

	private static AbilityCooldowns state(ServerPlayer player) {
		return player.getAttachedOrCreate(ModAttachments.ABILITY_COOLDOWNS);
	}

	private static int now(ServerPlayer player) {
		return player.level().getServer().getTickCount();
	}

	public static void start(ServerPlayer player, Identifier abilityId, int durationTicks) {
		state(player).start(abilityId, now(player), durationTicks);
		sync(player);
	}

	public static boolean isOnCooldown(ServerPlayer player, Identifier abilityId) {
		return state(player).isOnCooldown(abilityId, now(player));
	}

	public static int remaining(ServerPlayer player, Identifier abilityId) {
		return state(player).remaining(abilityId, now(player));
	}

	public static void clear(ServerPlayer player) {
		state(player).clear();
		sync(player);
	}

	/** Sends every active cooldown to the owning client. */
	public static void sync(ServerPlayer player) {
		if (!ServerPlayNetworking.canSend(player, CooldownSyncPayload.TYPE)) {
			return;
		}

		int now = now(player);
		List<CooldownSyncPayload.Entry> entries = new ArrayList<>();
		for (Map.Entry<Identifier, AbilityCooldowns.Window> entry : state(player).active(now).entrySet()) {
			if (entries.size() >= CooldownSyncPayload.MAX_ENTRIES) {
				break;
			}
			AbilityCooldowns.Window window = entry.getValue();
			entries.add(new CooldownSyncPayload.Entry(entry.getKey(), Math.max(0, window.endTick() - now), window.total()));
		}

		ServerPlayNetworking.send(player, new CooldownSyncPayload(entries));
	}
}
