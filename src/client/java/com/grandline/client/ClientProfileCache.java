package com.grandline.client;

import com.grandline.network.payload.CooldownSyncPayload;
import com.grandline.player.PlayerProfile;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Read-only client copy of data the server has synchronized to us. Used for display (HUD, screens) only:
 * nothing in here is ever sent back to the server, and changing it has no effect on gameplay.
 * Accessed only from the client thread.
 */
public final class ClientProfileCache {
	@Nullable
	private static PlayerProfile profile;
	private static List<CooldownSyncPayload.Entry> cooldowns = List.of();
	private static long cooldownsReceivedAtMillis;

	private ClientProfileCache() {
	}

	@Nullable
	public static PlayerProfile profile() {
		return profile;
	}

	public static List<CooldownSyncPayload.Entry> cooldowns() {
		return cooldowns;
	}

	public static long cooldownsReceivedAtMillis() {
		return cooldownsReceivedAtMillis;
	}

	public static void acceptProfile(PlayerProfile newProfile) {
		profile = newProfile;
	}

	public static void acceptCooldowns(List<CooldownSyncPayload.Entry> entries) {
		cooldowns = List.copyOf(entries);
		cooldownsReceivedAtMillis = System.currentTimeMillis();
	}

	public static void clear() {
		profile = null;
		cooldowns = List.of();
	}
}
