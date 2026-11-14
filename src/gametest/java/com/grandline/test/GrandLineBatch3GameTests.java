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

	@GameTest(template = EMPTY_STRUCTURE, batch = "b3_teams", timeoutTicks = 20)
	public void marinesStartOnTheirOwnIsland(GameTestHelper helper) {
		IslandRegistry registry = IslandRegistry.get(helper.getLevel().getServer());
		var config = com.grandline.GrandLineMod.config();
		Island pirates = new Island(990040, 70_000, 70_000, 20, 1, "Test Pirate Start", false, true, "village");
		Island marines = new Island(990041, 71_000, 70_000, 20, 1, "Test Marine Start", false, true, "brick_town");
		registry.add(pirates);
		registry.add(marines);
		String oldSpawn = config.spawnIsland, oldMarine = config.marineSpawnIsland;
		config.spawnIsland = pirates.name();
		config.marineSpawnIsland = marines.name();
		try {
			ServerPlayer marine = survivor(helper);
			ServerPlayer pirate = survivor(helper);
			ServerPlayer veteran = survivor(helper);
			marine.setAttached(ModAttachments.TEAM_SPAWN_PENDING, true);
			pirate.setAttached(ModAttachments.TEAM_SPAWN_PENDING, true);
			veteran.removeAttached(ModAttachments.TEAM_SPAWN_PENDING); // has played before: not waiting for a start
			Vec3 stay = helper.absoluteVec(new Vec3(1.5, 2, 1.5));
			veteran.teleportTo(stay.x, stay.y, stay.z);
			com.grandline.player.TeamService.choose(marine, com.grandline.player.TeamService.Side.MARINES);
			com.grandline.player.TeamService.choose(pirate, com.grandline.player.TeamService.Side.PIRATES);
			com.grandline.player.TeamService.choose(veteran, com.grandline.player.TeamService.Side.MARINES);
			helper.assertTrue(Math.abs(marine.getX() - marines.x()) < 80 && Math.abs(marine.getZ() - marines.z()) < 80,
					"a new Marine starts on the Marines' island: " + marine.blockPosition());
			helper.assertTrue(Math.abs(pirate.getX() - pirates.x()) < 80, "a new Pirate starts on the Pirates' island: " + pirate.blockPosition());
			helper.assertTrue(Math.abs(veteran.getX() - marines.x()) > 1000, "an established player is never moved by picking a side");
			helper.assertTrue(com.grandline.world.IslandSpawns.respawnIsland(veteran, com.grandline.world.IslandSpawns.NO_ISLAND).id() == marines.id(),
					"a Marine without a home who dies at sea returns to the Marines' island");
			veteran.setAttached(ModAttachments.HOME_ISLAND, pirates.id());
			helper.assertTrue(com.grandline.world.IslandSpawns.respawnIsland(veteran, com.grandline.world.IslandSpawns.NO_ISLAND).id() == pirates.id(),
					"a home always wins over the team island");
			remove(helper, marine, pirate, veteran);
		} finally {
			config.spawnIsland = oldSpawn;
			config.marineSpawnIsland = oldMarine;
			registry.removeIf(i -> i.id() == pirates.id() || i.id() == marines.id());
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "b3_keep", timeoutTicks = 40)
	public void deathKeepsEveryItemWithoutDropping(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer p = survivor(helper);
		net.minecraft.world.item.ItemStack sword = new net.minecraft.world.item.ItemStack(com.grandline.gear.Gear.KATANA);
		sword.setDamageValue(37);
		sword.setHoverName(net.minecraft.network.chat.Component.literal("Old Faithful"));
		p.getInventory().setItem(0, sword);
		p.getInventory().setItem(5, new net.minecraft.world.item.ItemStack(com.grandline.npc.FishingService.SILVERFIN, 12));
		p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_HELMET));
		PlayerProfileService.update(p, pr -> pr.withCurrency(4321));
		p.kill();
		helper.runAfterDelay(2, () -> {
			var drops = level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, p.getBoundingBox().inflate(6));
			helper.assertTrue(drops.isEmpty(), "nothing drops on death: " + drops.size());
			ServerPlayer back = level.getServer().getPlayerList().respawn(p, false);
			var stack = back.getInventory().getItem(0);
			helper.assertTrue(stack.is(com.grandline.gear.Gear.KATANA) && stack.getDamageValue() == 37
					&& stack.getHoverName().getString().equals("Old Faithful"), "sword kept with its damage and name");
			helper.assertTrue(back.getInventory().countItem(com.grandline.npc.FishingService.SILVERFIN) == 12, "fish kept");
			helper.assertTrue(back.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).is(net.minecraft.world.item.Items.IRON_HELMET), "armour kept");
			helper.assertTrue(PlayerProfileService.get(back).currency() == 4321, "currency kept");
			helper.assertTrue(p.getInventory().isEmpty(), "the old body holds nothing (no duplicates)");
			remove(helper, back);
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "b3_fish_play", timeoutTicks = 700)
	public void fishingMinigameRewardsSkillOnlyOnce(GameTestHelper helper) {
		ServerPlayer good = survivor(helper);
		ServerPlayer idle = survivor(helper);
		for (ServerPlayer p : new ServerPlayer[]{good, idle}) {
			p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new net.minecraft.world.item.ItemStack(com.grandline.registry.ModItems.STARTER_FISHING_ROD));
			p.setAttached(ModAttachments.FISHING_MASTERY, com.grandline.fishing.FishingMastery.START);
		}
		var silverfin = com.grandline.fishing.FishingRules.byItem(com.grandline.npc.FishingService.SILVERFIN);
		com.grandline.fishing.FishingSessions.startFightForTest(good, silverfin);
		com.grandline.fishing.FishingSessions.startFightForTest(idle, silverfin);
		boolean[] goodDone = {false}, idleDone = {false};
		helper.onEachTick(() -> {
			if (!goodDone[0]) {
				goodDone[0] = !com.grandline.fishing.FishingSessions.stepForTest(good, true);
			}
			if (!idleDone[0]) {
				idleDone[0] = !com.grandline.fishing.FishingSessions.stepForTest(idle, false);
			}
		});
		helper.runAfterDelay(600, () -> {
			helper.assertTrue(goodDone[0] && good.getInventory().countItem(com.grandline.npc.FishingService.SILVERFIN) == 1,
					"keeping the fish in the net lands exactly one fish: done=" + goodDone[0] + " fish=" + good.getInventory().countItem(com.grandline.npc.FishingService.SILVERFIN) + " mastery=" + good.getAttached(ModAttachments.FISHING_MASTERY));
			var m = good.getAttached(ModAttachments.FISHING_MASTERY);
			helper.assertTrue(m.catches() == 1 && (m.xp() > 0 || m.level() > 1), "and earns Fishing Mastery: " + m);
			helper.assertTrue(idleDone[0] && idle.getInventory().countItem(com.grandline.npc.FishingService.SILVERFIN) == 0, "doing nothing loses it");
			helper.assertTrue(idle.getAttached(ModAttachments.FISHING_MASTERY).catches() == 0, "and earns nothing");
			helper.assertTrue(!com.grandline.fishing.FishingSessions.stepForTest(good, true), "a finished fight cannot pay out again");
			remove(helper, good, idle);
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "b3_fish_rules", timeoutTicks = 20)
	public void fishingMasteryUnlocksAndPersists(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		var random = net.minecraft.util.RandomSource.create(42);
		Vec3 at = helper.absoluteVec(new Vec3(2, 2, 2));
		for (int i = 0; i < 400; i++) {
			var f = com.grandline.fishing.FishingRules.roll(level, at, 1, random);
			helper.assertTrue(f.minMastery() <= 1, "a beginner only hooks beginner fish: " + f.key());
		}
		var m = com.grandline.fishing.FishingMastery.START;
		for (int i = 0; i < 30; i++) {
			m = m.withCatch(50);
		}
		helper.assertTrue(m.level() > 5 && m.catches() == 30, "mastery levels up with catches: " + m);
		ServerPlayer p = survivor(helper);
		p.setAttached(ModAttachments.FISHING_MASTERY, m);
		p.kill();
		ServerPlayer back = level.getServer().getPlayerList().respawn(p, false);
		helper.assertTrue(m.equals(back.getAttached(ModAttachments.FISHING_MASTERY)), "Fishing Mastery survives death");
		PlayerProfileService.update(back, pr -> pr.withCurrency(com.grandline.GrandLineMod.config().fishingRodPrice - 1));
		helper.assertTrue(com.grandline.npc.NpcService.buyRod(back).equals("rod_money"), "a rod costs money");
		PlayerProfileService.update(back, pr -> pr.withCurrency(com.grandline.GrandLineMod.config().fishingRodPrice));
		helper.assertTrue(com.grandline.npc.NpcService.buyRod(back).equals("rod_bought")
				&& back.getInventory().countItem(com.grandline.registry.ModItems.STARTER_FISHING_ROD) == 1
				&& PlayerProfileService.get(back).currency() == 0, "the Fisherman sells the Starter Fishing Rod");
		remove(helper, back);
		helper.succeed();
	}
}
