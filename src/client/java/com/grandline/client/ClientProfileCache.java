package com.grandline.client;

import com.grandline.network.payload.CooldownSyncPayload;
import com.grandline.network.payload.ProfileSyncPayload;
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
	private static ProfileSyncPayload view;
	private static List<CooldownSyncPayload.Entry> cooldowns = List.of();
	private static long cooldownsReceivedAtMillis;

	private ClientProfileCache() {
	}

	/** The last full profile view received from the server, or {@code null} before the first sync. */
	@Nullable
	public static ProfileSyncPayload view() {
		return view;
	}

	@Nullable
	public static PlayerProfile profile() {
		return view == null ? null : view.profile();
	}

	public static List<CooldownSyncPayload.Entry> cooldowns() {
		return cooldowns;
	}

	public static long cooldownsReceivedAtMillis() {
		return cooldownsReceivedAtMillis;
	}

	public static void acceptProfile(ProfileSyncPayload newView) {
		view = newView;
	}

	public static void acceptCooldowns(List<CooldownSyncPayload.Entry> entries) {
		cooldowns = List.copyOf(entries);
		cooldownsReceivedAtMillis = System.currentTimeMillis();
	}

	public static void clear() {
		view = null;
		cooldowns = List.of();
	}
}
