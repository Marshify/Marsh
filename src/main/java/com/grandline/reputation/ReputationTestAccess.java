package com.grandline.reputation;

import net.minecraft.server.level.ServerPlayer;

/** Game-test hooks (simulated clock, disconnect handling, tag inspection). */
public final class ReputationTestAccess {
	private ReputationTestAccess() {
	}

	public static void clock(Long millis) {
		ReputationService.clockOverride = millis;
	}

	public static void disconnect(ServerPlayer player) {
		ReputationService.onDisconnect(player);
	}

	public static int pairCount(ServerPlayer a, ServerPlayer b) {
		long now = ReputationService.clockOverride != null ? ReputationService.clockOverride : System.currentTimeMillis();
		return ReputationService.data(a.server).pairCount(a.getUUID(), b.getUUID(), now,
				com.grandline.GrandLineMod.config().pvpPairWindowHours * 3_600_000L);
	}

	public static void reset() {
		ReputationService.clearTags();
		ReputationService.clockOverride = null;
	}
}
