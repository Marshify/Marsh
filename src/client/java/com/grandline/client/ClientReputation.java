package com.grandline.client;

import com.grandline.network.payload.LeaderboardPayload;
import com.grandline.network.payload.ReputationSyncPayload;

/** Client copy of Bounty/Honor, the active side, the combat tag and the last leaderboard (display only). */
public final class ClientReputation {
	private static ReputationSyncPayload state = new ReputationSyncPayload(0, 0, -1, 0, false);
	private static long receivedAt;
	private static LeaderboardPayload board;

	private ClientReputation() {
	}

	public static void accept(ReputationSyncPayload payload) {
		state = payload;
		receivedAt = System.currentTimeMillis();
	}

	public static ReputationSyncPayload state() {
		return state;
	}

	/** Seconds of combat tag left, counted down locally between syncs. */
	public static int tagSeconds() {
		long left = state.tagTicks() * 50L - (System.currentTimeMillis() - receivedAt);
		return left <= 0 ? 0 : (int) Math.ceil(left / 1000.0);
	}

	public static void acceptBoard(LeaderboardPayload payload) {
		board = payload;
	}

	public static LeaderboardPayload board() {
		return board;
	}

	public static void clear() {
		state = new ReputationSyncPayload(0, 0, -1, 0, false);
		board = null;
	}
}
