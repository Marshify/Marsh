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
	public static final int BOSS_MIN_ISLAND_LEVEL = 10; // Verdant Wilds (Lv 10-30) is the first island with a boss

	public enum Result {
		ACCEPTED, ALREADY_ON_QUEST, NO_BOSS_HERE, TOO_FAR, ABANDONED, NO_QUEST
	}

	private QuestService() {
	}

	/** The First Sea stage this quest giver hands out (its island and region), or null off the ladder. */
	public static com.grandline.world.FirstSeaProgression.Stage stageOf(Island island, Islander npc) {
		return island == null ? null : com.grandline.world.FirstSeaProgression.stage(island.name(), npc.region());
	}

	/** The level a quest giver's quests are for: its stage's unlock level (off the ladder: the island's level). */
	public static int questLevel(Island island, Islander npc) {
		var stage = stageOf(island, npc);
		return stage != null ? stage.min() : island == null ? npc.islandLevel() : island.level();
	}

	public static QuestState offer(Island island, Islander npc, boolean boss) {
		int level = questLevel(island, npc);
		String name = island == null ? "?" : island.name();
		int x = island == null ? npc.getBlockX() : island.x();
		int z = island == null ? npc.getBlockZ() : island.z();
		var stage = stageOf(island, npc);
		long xp = stage != null ? com.grandline.world.FirstSeaProgression.questExperience(stage) : 120 + level * 60L;
		long currency = stage != null ? com.grandline.world.FirstSeaProgression.questCurrency(stage) : 50 + level * 15L;
		if (boss) {
			return new QuestState(EnemyKind.CAPTAIN.name(), name, 1, 0, xp * 5, currency * 5, x, z, true);
		}
		return new QuestState(EnemyKind.forLevel(level).name(), name, KILL_COUNT, 0, xp, currency, x, z, false);
	}

	/**
	 * Drops the player's quest if it targets a starter island: those are safe zones without enemies, so it could never
	 * be finished (worlds from before Dawnreach's rebuild could hand these out). Called when the player joins.
	 */
	public static void dropImpossibleQuest(ServerPlayer player) {
		QuestState quest = active(player);
		if (quest == null) {
			return;
		}
		// A regular quest saved before its island moved up the First Sea ladder may ask for an enemy kind the island no
		// longer has: it now asks for the island's current kind, keeping its progress and rewards.
		if (!quest.boss()) {
			for (com.grandline.world.Island island : com.grandline.world.IslandRegistry.get(player.server).all()) {
				if (island.name().equals(quest.islandName()) && island.sea() == 1
						&& com.grandline.world.FirstSeaProgression.stage(island.name(), com.grandline.world.FirstSeaProgression.LOWER) != null) {
					java.util.Set<String> kinds = new java.util.HashSet<>();
					for (com.grandline.world.FirstSeaProgression.Stage st : com.grandline.world.FirstSeaProgression.STAGES) {
						if (st.island().equals(island.name())) {
							kinds.add(EnemyKind.forLevel(st.min()).name());
						}
					}
					if (!kinds.contains(quest.target())) {
						String now = EnemyKind.forLevel(island.level()).name();
						quest = new QuestState(now, quest.islandName(), quest.required(), quest.progress(), quest.rewardExperience(),
								quest.rewardCurrency(), quest.x(), quest.z(), false);
						player.setAttached(ModAttachments.ACTIVE_QUEST, quest);
						sync(player);
					}
					break;
				}
			}
		}
		for (com.grandline.world.Island island : com.grandline.world.IslandRegistry.get(player.server).all()) {
			if (island.starter() && island.name().equals(quest.islandName())) {
				player.removeAttached(ModAttachments.ACTIVE_QUEST);
				sync(player);
				player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.grandline.quest.safe_zone", com.grandline.names.GrandLineNames.island(quest.islandName()))
						.withStyle(net.minecraft.ChatFormatting.YELLOW));
				return;
			}
		}
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
		int level = questLevel(island, npc);
		if (boss && level < BOSS_MIN_ISLAND_LEVEL) {
			return Result.NO_BOSS_HERE;
		}

		QuestState quest = offer(island, npc, boss);
		player.setAttached(ModAttachments.ACTIVE_QUEST, quest);
		if (boss) {
			var stage = stageOf(island, npc);
			spawnBoss(player, npc, stage != null ? stage.bossLevel() : level + 10);
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
			for (com.grandline.world.plan.Anchor a : arenas) {
				if (a.region() == npc.region()) {
					arena = a; // the arena of the quest giver's own region
					break;
				}
			}
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
		return Component.translatable(quest.boss() ? "quest.grandline.boss" : "quest.grandline.kill", quest.required(), target, com.grandline.names.GrandLineNames.island(quest.islandName()));
	}

	public static void sync(ServerPlayer player) {
		if (ServerPlayNetworking.canSend(player, QuestSyncPayload.TYPE)) {
			ServerPlayNetworking.send(player, new QuestSyncPayload(java.util.Optional.ofNullable(active(player))));
		}
	}
}
