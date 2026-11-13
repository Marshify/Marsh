package com.grandline.reputation;

import com.grandline.network.ServerRequestGuard;
import com.grandline.network.payload.LeaderboardPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import java.util.Comparator;
import java.util.List;
import java.util.function.ToLongFunction;

/** Top Bounty and Honor holders (online or offline), sent on request. */
public final class Leaderboards {
	public static final int SIZE = 10;

	private Leaderboards() {
	}

	public static void register() {
		ServerPlayNetworking.registerGlobalReceiver(LeaderboardPayload.TYPE, (packet, player, sender) -> {
			if (ServerRequestGuard.check(player, LeaderboardPayload.TYPE) != ServerRequestGuard.Result.ACCEPTED) {
				return;
			}
			ReputationData data = ReputationService.data(player.server);
			sender.sendPacket(new LeaderboardPayload(top(data, ReputationData.Entry::bounty), top(data, ReputationData.Entry::honor)));
		});
	}

	static List<LeaderboardPayload.Row> top(ReputationData data, ToLongFunction<ReputationData.Entry> value) {
		return data.board.values().stream()
				.filter(e -> value.applyAsLong(e) > 0)
				.sorted(Comparator.comparingLong(value).reversed())
				.limit(SIZE)
				.map(e -> new LeaderboardPayload.Row(e.name(), value.applyAsLong(e)))
				.toList();
	}
}
