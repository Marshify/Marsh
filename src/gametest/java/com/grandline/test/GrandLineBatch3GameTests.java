package com.grandline.test;

import com.grandline.combat.CombatCredit;
import com.grandline.data.ModAttachments;
import com.grandline.entity.GrandLineEnemy;
import com.grandline.entity.GrandLineEntities;
import com.grandline.npc.QuestState;
import com.grandline.player.PlayerProfileService;
import com.grandline.quest.QuestUnlocks;
import com.grandline.world.Island;
import com.grandline.world.IslandRegistry;
import com.grandline.world.SeaLayout;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Update batch 3: kill credit, quest milestones, team starts, keep-inventory, fishing, economy, world lock, navigation. */
public final class GrandLineBatch3GameTests implements FabricGameTest {
	static ServerPlayer survivor(GameTestHelper helper) {
		ServerPlayer p = helper.makeMockServerPlayerInLevel();
		p.setGameMode(GameType.SURVIVAL);
		try {
			var field = ServerPlayer.class.getDeclaredField("spawnInvulnerableTime");
			field.setAccessible(true);
			field.setInt(p, 0);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(e);
		}
		Vec3 home = helper.absoluteVec(new Vec3(1.5, 2, 1.5));
		p.moveTo(home.x, home.y, home.z, 0, 0);
		return p;
	}

	static void remove(GameTestHelper helper, Entity... entities) {
		for (Entity e : entities) {
			if (e instanceof ServerPlayer p) {
				helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
			} else if (e != null) {
				e.discard();
			}
		}
	}

	private static GrandLineEnemy bandit(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		GrandLineEnemy b = GrandLineEntities.BANDIT.create(level);
		Vec3 at = helper.absoluteVec(new Vec3(4.5, 2, 4.5));
		b.moveTo(at.x, at.y, at.z, 0, 0);
		b.setGrandLineLevel(5);
		b.setNoAi(true);
		level.addFreshEntity(b);
		return b;
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "b3_credit", timeoutTicks = 60)
	public void burningAndFallKillsCountForTheAttacker(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer p = survivor(helper);
		PlayerProfileService.update(p, pr -> pr.withCurrency(0));
		p.setAttached(ModAttachments.ACTIVE_QUEST, new QuestState("BANDIT", "Test", 6, 0, 10, 10, 0, 0, false));
		GrandLineEnemy burned = bandit(helper);
		GrandLineEnemy fell = bandit(helper);
		GrandLineEnemy untouched = bandit(helper);
		GrandLineEnemy mobbed = bandit(helper);
		burned.hurt(level.damageSources().playerAttack(p), 1);
		fell.hurt(level.damageSources().playerAttack(p), 1);
		mobbed.hurt(level.damageSources().playerAttack(p), 1);
		var zombie = EntityType.ZOMBIE.create(level);
		zombie.moveTo(burned.position());
		helper.runAfterDelay(12, () -> {
			burned.invulnerableTime = 0;
			burned.hurt(level.damageSources().onFire(), 10_000);
			fell.invulnerableTime = 0;
			fell.hurt(level.damageSources().fall(), 10_000);
			untouched.hurt(level.damageSources().fall(), 10_000);
			mobbed.invulnerableTime = 0;
			mobbed.hurt(level.damageSources().mobAttack(zombie), 10_000);
		});
		helper.runAfterDelay(14, () -> {
			QuestState q = p.getAttached(ModAttachments.ACTIVE_QUEST);
			helper.assertTrue(q != null && q.progress() == 2, "burn and fall kills count for the quest: " + (q == null ? null : q.progress()));
			helper.assertTrue(PlayerProfileService.get(p).currency() > 0, "and pay currency");
			helper.assertTrue(CombatCredit.killer(burned, level.damageSources().onFire()) == null, "a death is only credited once");
			zombie.discard();
			remove(helper, p);
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "b3_unlock", timeoutTicks = 20)
	public void questUnlockCardShowsOncePerIsland(GameTestHelper helper) {
		IslandRegistry registry = IslandRegistry.get(helper.getLevel().getServer());
		SeaLayout.Spec verdant = SeaLayout.all().stream().filter(s -> s.name().equals("Verdant Wilds")).findFirst().orElseThrow();
		Island island = new Island(SeaLayout.idOf(verdant), verdant.x(), verdant.z(), verdant.radius(), verdant.level(), verdant.name(), false, true, "jungle");
		boolean had = registry.get(island.id()) != null;
		if (!had) {
			registry.add(island);
		}
		ServerPlayer p = survivor(helper);
		p.setAttached(ModAttachments.QUEST_UNLOCKS_SEEN, List.of());
		QuestUnlocks.onLevelChanged(p, 1, 14);
		helper.assertTrue(!p.getAttached(ModAttachments.QUEST_UNLOCKS_SEEN).contains(island.id()), "not yet at Lv 14");
		QuestUnlocks.onLevelChanged(p, 14, 15);
		helper.assertTrue(p.getAttached(ModAttachments.QUEST_UNLOCKS_SEEN).contains(island.id()), "Verdant unlocked at its level (read from the registry)");
		int size = p.getAttached(ModAttachments.QUEST_UNLOCKS_SEEN).size();
		QuestUnlocks.onLevelChanged(p, 15, 16);
		helper.assertTrue(p.getAttached(ModAttachments.QUEST_UNLOCKS_SEEN).size() == size, "no repeat");
		if (!had) {
			registry.removeIf(i -> i.id() == island.id());
		}
		remove(helper, p);
		helper.succeed();
	}
}
