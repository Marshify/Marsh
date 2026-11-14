package com.grandline.quest;

import com.grandline.world.FirstSeaProgression;

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

	/** One "new quests" card: its saved key, the place name, its level and sea. */
	public record Milestone(long key, String name, int level, int sea) {
	}

	public static void register() {
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayer player = handler.getPlayer();
			int level = PlayerProfileService.get(player).level();
			List<Long> seen = player.getAttached(ModAttachments.QUEST_UNLOCKS_SEEN);
			List<Long> reached = seen == null ? new ArrayList<>() : new ArrayList<>(seen);
			boolean changed = seen == null;
			for (Milestone m : milestones(player)) {
				// Players already past a milestone (new saves, or a milestone that moved in a rebalance) are marked as
				// told, without a flood of cards.
				if (m.level() <= level && !reached.contains(m.key())) {
					reached.add(m.key());
					changed = true;
				}
			}
			if (changed) {
				player.setAttached(ModAttachments.QUEST_UNLOCKS_SEEN, reached);
			}
		});
	}

	/**
	 * Milestones above the starting level, lowest first. The First Sea follows {@link FirstSeaProgression} (Lv 10, 30,
	 * ... 450 for the upper Cloudtop Isles, 625); other seas: each designed island with quests at its level.
	 */
	public static List<Milestone> milestones(ServerPlayer player) {
		List<Milestone> list = new ArrayList<>();
		java.util.Map<String, Island> byName = new java.util.HashMap<>();
		for (Island island : IslandRegistry.get(player.server).all()) {
			byName.putIfAbsent(island.name(), island);
		}
		for (FirstSeaProgression.Stage stage : FirstSeaProgression.STAGES) {
			Island island = byName.get(stage.island());
			if (stage.min() <= 1 || island == null) {
				continue;
			}
			long key = stage.region() == FirstSeaProgression.LOWER ? island.id() : ~island.id();
			list.add(new Milestone(key, stage.label(), stage.min(), 1));
		}
		Set<Long> designed = new HashSet<>();
		for (SeaLayout.Spec spec : SeaLayout.all()) {
			if (!spec.starter() && spec.sea() > 1) {
				designed.add(SeaLayout.idOf(spec));
			}
		}
		for (Island island : IslandRegistry.get(player.server).all()) {
			if (designed.contains(island.id()) && island.level() > 1) {
				list.add(new Milestone(island.id(), island.name(), island.level(), island.sea()));
			}
		}
		list.sort(Comparator.comparingInt(Milestone::level).thenComparingInt(Milestone::sea));
		return list;
	}

	/** Called after the player's level changes: shows the card for the highest newly reached milestone. */
	public static void onLevelChanged(ServerPlayer player, int oldLevel, int newLevel) {
		if (newLevel <= oldLevel) {
			return;
		}
		List<Long> seen = new ArrayList<>(player.getAttachedOrElse(ModAttachments.QUEST_UNLOCKS_SEEN, List.of()));
		Milestone newest = null;
		for (Milestone m : milestones(player)) {
			if (m.level() <= newLevel && !seen.contains(m.key())) {
				seen.add(m.key());
				newest = m;
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
