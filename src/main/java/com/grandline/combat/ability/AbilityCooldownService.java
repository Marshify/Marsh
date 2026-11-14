package com.grandline.combat.ability;

import com.grandline.data.ModAttachments;
import com.grandline.network.payload.CooldownSyncPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.ResourceLocation;
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
		return player.server.getTickCount();
	}

	public static void start(ServerPlayer player, ResourceLocation abilityId, int durationTicks) {
		com.grandline.race.RaceProgress.onMoveUsed(player);
		state(player).start(abilityId, now(player), (int) Math.round(durationTicks * com.grandline.race.RacePassives.cooldownMultiplier(player)));
		sync(player);
	}

	public static boolean isOnCooldown(ServerPlayer player, ResourceLocation abilityId) {
		return state(player).isOnCooldown(abilityId, now(player));
	}

	public static int remaining(ServerPlayer player, ResourceLocation abilityId) {
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
		for (Map.Entry<ResourceLocation, AbilityCooldowns.Window> entry : state(player).active(now).entrySet()) {
			if (entries.size() >= CooldownSyncPayload.MAX_ENTRIES) {
				break;
			}
			AbilityCooldowns.Window window = entry.getValue();
			entries.add(new CooldownSyncPayload.Entry(entry.getKey(), Math.max(0, window.endTick() - now), window.total()));
		}

		ServerPlayNetworking.send(player, new CooldownSyncPayload(entries));
	}
}
