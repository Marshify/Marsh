package com.grandline.npc;

import com.grandline.data.ModAttachments;
import com.grandline.entity.EnemyKind;
import com.grandline.entity.GrandLineEnemy;
import com.grandline.entity.GrandLineEntities;
import com.grandline.entity.IslandBoss;
import com.grandline.entity.Islander;
import com.grandline.network.payload.QuestSyncPayload;
import com.grandline.player.PlayerProfileService;
import com.grandline.world.Island;
import com.grandline.world.IslandRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.levelgen.Heightmap;

/** Island quests: offers scaled to the island level, kill tracking, rewards. One active quest per player. */
public final class QuestService {
	public static final int KILL_COUNT = 6;
	/** Boss quests are offered on islands of at least this level. */
	public static final int BOSS_MIN_ISLAND_LEVEL = 15; // Verdant Wilds (Lv 15) is the first island with a boss

	public enum Result {
		ACCEPTED, ALREADY_ON_QUEST, NO_BOSS_HERE, TOO_FAR, ABANDONED, NO_QUEST
	}

	private QuestService() {
	}

	public static QuestState offer(Island island, Islander npc, boolean boss) {
		int level = island == null ? npc.islandLevel() : island.level();
		String name = island == null ? "?" : island.name();
		int x = island == null ? npc.getBlockX() : island.x();
		int z = island == null ? npc.getBlockZ() : island.z();
		long xp = 120 + level * 60L;
		long currency = 50 + level * 15L;
		if (boss) {
			return new QuestState(EnemyKind.CAPTAIN.name(), name, 1, 0, xp * 5, currency * 5, x, z, true);
		}
		return new QuestState(EnemyKind.forLevel(level).name(), name, KILL_COUNT, 0, xp, currency, x, z, false);
	}

	public static QuestState active(ServerPlayer player) {
		return player.getAttached(ModAttachments.ACTIVE_QUEST);
	}

	public static Result accept(ServerPlayer player, Islander npc, boolean boss) {
		if (npc.role() != Islander.Role.QUEST_GIVER || npc.distanceTo(player) > 8) {
			return Result.TOO_FAR;
		}
		if (active(player) != null) {
			return Result.ALREADY_ON_QUEST;
		}
		Island island = IslandRegistry.get(player.server).get(npc.islandId());
		int level = island == null ? npc.islandLevel() : island.level();
		if (boss && level < BOSS_MIN_ISLAND_LEVEL) {
			return Result.NO_BOSS_HERE;
		}

		QuestState quest = offer(island, npc, boss);
		player.setAttached(ModAttachments.ACTIVE_QUEST, quest);
		if (boss) {
			spawnBoss(player, npc, level + 10);
		}
		player.sendSystemMessage(Component.translatable("message.grandline.quest.accepted", describe(quest)).withStyle(ChatFormatting.GOLD));
		sync(player);
		return Result.ACCEPTED;
	}

	public static Result abandon(ServerPlayer player) {
		if (active(player) == null) {
			return Result.NO_QUEST;
		}
		player.removeAttached(ModAttachments.ACTIVE_QUEST);
		sync(player);
		return Result.ABANDONED;
	}

	/** Called for every kill by a player. Advances and completes matching quests. */
	public static void onKill(ServerPlayer player, LivingEntity killed) {
		QuestState quest = active(player);
		if (quest == null || !(killed instanceof GrandLineEnemy enemy) || !enemy.kind().name().equals(quest.target())) {
			return;
		}
		QuestState updated = quest.withProgress(quest.progress() + 1);
		if (updated.complete()) {
			player.removeAttached(ModAttachments.ACTIVE_QUEST);
			PlayerProfileService.addCurrency(player, updated.rewardCurrency());
			PlayerProfileService.addExperience(player, updated.rewardExperience());
			player.sendSystemMessage(Component.translatable("message.grandline.quest.complete", updated.rewardExperience(),
					updated.rewardCurrency()).withStyle(ChatFormatting.GREEN));
			player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.8F, 1.0F);
		} else {
			player.setAttached(ModAttachments.ACTIVE_QUEST, updated);
		}
		sync(player);
	}

	private static void spawnBoss(ServerPlayer player, Islander npc, int level) {
		IslandBoss boss = GrandLineEntities.CAPTAIN.create(player.serverLevel());
		if (boss == null) {
			return;
		}
		double angle = player.getRandom().nextDouble() * Math.PI * 2;
		int x = npc.getBlockX() + (int) Math.round(Math.cos(angle) * 8);
		int z = npc.getBlockZ() + (int) Math.round(Math.sin(angle) * 8);
		int y = Integer.MIN_VALUE;
		// Planned islands have a boss arena: the boss waits there instead of next to the quest giver.
		com.grandline.world.Island island = com.grandline.world.IslandRegistry.get(player.server).get(npc.islandId());
		java.util.List<com.grandline.world.plan.Anchor> arenas = com.grandline.world.plan.IslandPlans.anchors(island,
				com.grandline.world.plan.Anchor.Type.BOSS_ARENA);
		if (!arenas.isEmpty()) {
			com.grandline.world.plan.Anchor arena = arenas.get(0);
			x = island.x() + arena.x();
			z = island.z() + arena.z();
			if (arena.y() != com.grandline.world.plan.Anchor.SURFACE) {
				y = arena.y() + 1;
			}
		}
		if (y == Integer.MIN_VALUE) {
			y = player.serverLevel().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
		}
		boss.moveTo(new BlockPos(x, y, z), 0, 0);
		boss.finalizeSpawn(player.serverLevel(), player.serverLevel().getCurrentDifficultyAt(boss.blockPosition()), MobSpawnType.EVENT, null, null);
		boss.equip();
		boss.setGrandLineLevel(level);
		player.serverLevel().addFreshEntity(boss);
	}

	public static Component describe(QuestState quest) {
		Component target = Component.translatable("entity.grandline." + EnemyKind.valueOf(quest.target()).id());
		return Component.translatable(quest.boss() ? "quest.grandline.boss" : "quest.grandline.kill", quest.required(), target, quest.islandName());
	}

	public static void sync(ServerPlayer player) {
		if (ServerPlayNetworking.canSend(player, QuestSyncPayload.TYPE)) {
			ServerPlayNetworking.send(player, new QuestSyncPayload(java.util.Optional.ofNullable(active(player))));
		}
	}
}
