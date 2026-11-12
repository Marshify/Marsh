package com.grandline.test;

import com.grandline.GrandLineMod;
import com.grandline.entity.EnemyKind;
import com.grandline.entity.GrandLineEnemy;
import com.grandline.entity.GrandLineEntities;
import com.grandline.entity.IslandBoss;
import com.grandline.entity.Islander;
import com.grandline.network.payload.NpcActionPayload;
import com.grandline.npc.FishingService;
import com.grandline.npc.NpcService;
import com.grandline.npc.QuestService;
import com.grandline.npc.QuestState;
import com.grandline.player.PlayerProfileService;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.util.Objects;

/** Milestone 6 game tests: NPCs, quests, enemies, the boss and fishing. */
public final class GrandLineNpcGameTests implements FabricGameTest {
	private static Islander npc(GameTestHelper helper, Islander.Role role, int level) {
		Islander npc = helper.spawn(GrandLineEntities.ISLANDER, new BlockPos(1, 2, 1));
		npc.setup(role, 0L, level);
		return npc;
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void enemiesScaleWithLevel(GameTestHelper helper) {
		GrandLineEnemy low = helper.spawn(GrandLineEntities.BANDIT, new BlockPos(1, 2, 1));
		low.setGrandLineLevel(1);
		GrandLineEnemy high = helper.spawn(GrandLineEntities.BANDIT, new BlockPos(2, 2, 2));
		high.setGrandLineLevel(300);
		helper.assertTrue(high.getMaxHealth() > low.getMaxHealth() * 5, "health scales with level");
		eq(helper, EnemyKind.forLevel(10), EnemyKind.BANDIT, "low islands have bandits");
		eq(helper, EnemyKind.forLevel(2000), EnemyKind.CORSAIR, "far islands have corsairs");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void killQuestTracksKillsAndPaysOut(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		Islander giver = npc(helper, Islander.Role.QUEST_GIVER, 10);
		player.moveTo(giver.position());
		eq(helper, QuestService.accept(player, giver, false), QuestService.Result.ACCEPTED, "accept");
		eq(helper, QuestService.accept(player, giver, false), QuestService.Result.ALREADY_ON_QUEST, "one quest at a time");
		QuestState quest = QuestService.active(player);
		long xpBefore = PlayerProfileService.get(player).experience() + PlayerProfileService.get(player).level() * 1_000_000L;
		long currencyBefore = PlayerProfileService.get(player).currency();

		for (int i = 0; i < quest.required(); i++) {
			GrandLineEnemy bandit = helper.spawn(GrandLineEntities.typeOf(EnemyKind.valueOf(quest.target())), new BlockPos(1, 2, 1));
			bandit.setGrandLineLevel(10);
			bandit.hurt(helper.getLevel().damageSources().playerAttack(player), 100_000f);
		}
		helper.assertTrue(QuestService.active(player) == null, "quest completed and cleared");
		helper.assertTrue(PlayerProfileService.get(player).currency() >= currencyBefore + quest.rewardCurrency(), "currency reward paid");
		long xpAfter = PlayerProfileService.get(player).experience() + PlayerProfileService.get(player).level() * 1_000_000L;
		helper.assertTrue(xpAfter > xpBefore, "experience reward paid");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void bossQuestSpawnsBoss(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		Islander lowGiver = npc(helper, Islander.Role.QUEST_GIVER, 5);
		player.moveTo(lowGiver.position());
		eq(helper, QuestService.accept(player, lowGiver, true), QuestService.Result.NO_BOSS_HERE, "no boss on low islands");
		Islander giver = npc(helper, Islander.Role.QUEST_GIVER, QuestService.BOSS_MIN_ISLAND_LEVEL + 20);
		eq(helper, QuestService.accept(player, giver, true), QuestService.Result.ACCEPTED, "boss quest");
		helper.runAfterDelay(2, () -> {
			var bosses = helper.getLevel().getEntitiesOfClass(IslandBoss.class, new AABB(giver.blockPosition()).inflate(20));
			helper.assertTrue(!bosses.isEmpty(), "boss spawned near the quest giver");
			helper.assertTrue(bosses.get(0).getMaxHealth() > 100, "boss is tough");
			bosses.forEach(b -> b.discard());
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void npcActionsAreValidated(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		Islander fisher = npc(helper, Islander.Role.FISHERMAN, 1);
		player.moveTo(fisher.position());
		eq(helper, NpcService.handle(player, new NpcActionPayload(-5, NpcActionPayload.SELL_FISH)), "too_far", "unknown NPC");
		helper.runAfterDelay(GrandLineMod.config().requestCooldownTicks + 1, () -> {
			player.getInventory().add(new ItemStack(FishingService.CROWN_MARLIN, 2));
			long before = PlayerProfileService.get(player).currency();
			eq(helper, NpcService.handle(player, new NpcActionPayload(fisher.getId(), NpcActionPayload.SELL_FISH)), "sold", "sell");
			eq(helper, PlayerProfileService.get(player).currency(), before + 2 * FishingService.value(FishingService.CROWN_MARLIN), "paid");
			eq(helper, player.getInventory().countItem(FishingService.CROWN_MARLIN), 0, "fish taken");
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void npcsCannotBeHurt(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
		Islander npc = npc(helper, Islander.Role.QUEST_GIVER, 1);
		npc.hurt(helper.getLevel().damageSources().playerAttack(player), 1000f);
		helper.assertTrue(npc.isAlive() && npc.getHealth() == npc.getMaxHealth(), "NPC unharmed");
		helper.succeed();
	}

	private static void eq(GameTestHelper helper, Object actual, Object expected, String what) {
		helper.assertTrue(Objects.equals(actual, expected), what + ": expected " + expected + " but got " + actual);
	}
}
