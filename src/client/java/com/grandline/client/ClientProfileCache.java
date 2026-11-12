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
	private static List<com.grandline.world.Island> islands = List.of();
	@Nullable
	private static com.grandline.npc.QuestState quest;
	private static float energy;
	private static float maxEnergy;

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

	public static List<com.grandline.world.Island> islands() {
		return islands;
	}

	public static void acceptIslands(List<com.grandline.world.Island> list) {
		islands = List.copyOf(list);
	}

	@Nullable
	public static com.grandline.npc.QuestState quest() {
		return quest;
	}

	public static void acceptQuest(@Nullable com.grandline.npc.QuestState newQuest) {
		quest = newQuest;
	}

	public static float energy() {
		return energy;
	}

	public static float maxEnergy() {
		return maxEnergy;
	}

	public static void acceptEnergy(float current, float max) {
		energy = current;
		maxEnergy = max;
	}

	/** Remaining cooldown in ticks for {@code abilityId}, estimated from the last sync and elapsed real time. */
	public static int remainingCooldownTicks(net.minecraft.resources.ResourceLocation abilityId) {
		long elapsedTicks = (System.currentTimeMillis() - cooldownsReceivedAtMillis) / 50L;
		for (CooldownSyncPayload.Entry entry : cooldowns) {
			if (entry.abilityId().equals(abilityId)) {
				return (int) Math.max(0, entry.remainingTicks() - elapsedTicks);
			}
		}
		return 0;
	}

	public static void clear() {
		view = null;
		cooldowns = List.of();
		energy = 0;
		maxEnergy = 0;
		islands = List.of();
		quest = null;
	}
}
