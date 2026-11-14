package com.grandline.quest;

import com.grandline.data.ModAttachments;
import com.grandline.network.payload.QuestUnlockPayload;
import com.grandline.player.PlayerProfileService;
import com.grandline.world.Island;
import com.grandline.world.IslandRegistry;
import com.grandline.world.SeaLayout;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * "New quest available" milestones. Every designed island that has quests (not a safe-zone starter town) opens at its
 * recommended level, read from the island registry; the first time a player reaches that level they get a card naming
 * the island and its level. The islands a player has been told about are saved, so a card never repeats. Players who
 * already passed a milestone before cards existed are marked as told when they join, without a flood of cards. Both
 * teams' starting islands are Lv 1, so the ladder after them is the same for everyone.
 */
public final class QuestUnlocks {
	private QuestUnlocks() {
	}

	public static void register() {
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayer player = handler.getPlayer();
			if (player.getAttached(ModAttachments.QUEST_UNLOCKS_SEEN) == null) {
				List<Long> reached = new ArrayList<>();
				for (Island island : milestones(player)) {
					if (island.level() <= PlayerProfileService.get(player).level()) {
						reached.add(island.id());
					}
				}
				player.setAttached(ModAttachments.QUEST_UNLOCKS_SEEN, reached);
			}
		});
	}

	/** Islands with quests above the starting level, lowest first. */
	public static List<Island> milestones(ServerPlayer player) {
		Set<Long> designed = new HashSet<>();
		for (SeaLayout.Spec spec : SeaLayout.all()) {
			if (!spec.starter()) {
				designed.add(SeaLayout.idOf(spec));
			}
		}
		List<Island> list = new ArrayList<>();
		for (Island island : IslandRegistry.get(player.server).all()) {
			if (designed.contains(island.id()) && island.level() > 1) {
				list.add(island);
			}
		}
		list.sort(Comparator.comparingInt(Island::level).thenComparingInt(Island::sea));
		return list;
	}

	/** Called after the player's level changes: shows the card for the highest newly reached island. */
	public static void onLevelChanged(ServerPlayer player, int oldLevel, int newLevel) {
		if (newLevel <= oldLevel) {
			return;
		}
		List<Long> seen = new ArrayList<>(player.getAttachedOrElse(ModAttachments.QUEST_UNLOCKS_SEEN, List.of()));
		Island newest = null;
		for (Island island : milestones(player)) {
			if (island.level() <= newLevel && !seen.contains(island.id())) {
				seen.add(island.id());
				newest = island;
			}
		}
		if (newest == null) {
			return;
		}
		player.setAttached(ModAttachments.QUEST_UNLOCKS_SEEN, seen);
		if (ServerPlayNetworking.canSend(player, QuestUnlockPayload.TYPE)) {
			ServerPlayNetworking.send(player, new QuestUnlockPayload(newest.name(), newest.level(), newest.sea()));
		}
		player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.grandline.quest_unlock", newest.name(),
				newest.level()).withStyle(net.minecraft.ChatFormatting.GOLD));
	}
}
