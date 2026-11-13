package com.grandline.test;

import com.grandline.GrandLineMod;
import com.grandline.data.ModAttachments;
import com.grandline.fruit.Fruits;
import com.grandline.player.PlayerProfileService;
import com.grandline.player.rules.OceanWeakness;
import com.grandline.world.Island;
import com.grandline.world.IslandRegistry;
import com.grandline.world.IslandSpawns;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Part 1 core rules: vanilla XP, hunger, regeneration, fall damage, announcements, ocean weakness, respawn islands, level-up heal. */
public final class GrandLineCoreRulesGameTests implements FabricGameTest {
	private static ServerPlayer survivor(GameTestHelper helper) {
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

	private static void remove(GameTestHelper helper, ServerPlayer... players) {
		for (ServerPlayer p : players) {
			helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "core_xp")
	public void vanillaExperienceIsGone(GameTestHelper helper) {
		ServerPlayer p = survivor(helper);
		p.giveExperiencePoints(500);
		p.giveExperienceLevels(10);
		helper.assertTrue(p.experienceLevel == 0 && p.totalExperience == 0, "no vanilla XP gained");
		Vec3 at = helper.absoluteVec(new Vec3(2, 2, 2));
		ExperienceOrb.award(helper.getLevel(), at, 100);
		helper.assertTrue(helper.getLevel().getEntitiesOfClass(ExperienceOrb.class, new AABB(at, at).inflate(3)).isEmpty(), "no orbs spawn");
		long before = PlayerProfileService.get(p).experience();
		PlayerProfileService.addExperience(p, 30);
		helper.assertTrue(PlayerProfileService.get(p).experience() > before || PlayerProfileService.get(p).level() > 1, "Grand Line XP still works");
		remove(helper, p);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "core_hunger")
	public void hungerStaysFull(GameTestHelper helper) {
		ServerPlayer p = survivor(helper);
		p.getFoodData().setFoodLevel(3);
		p.getFoodData().setSaturation(0);
		p.getFoodData().addExhaustion(40);
		p.getFoodData().tick(p);
		helper.assertTrue(p.getFoodData().getFoodLevel() == 20 && p.getFoodData().getSaturationLevel() == 20, "food and saturation refilled");
		helper.assertTrue(p.getFoodData().getExhaustionLevel() == 0, "no exhaustion");
		remove(helper, p);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "core_regen", timeoutTicks = 100)
	public void regenIsOnePercentPerSecondAndCapped(GameTestHelper helper) {
		ServerPlayer p = survivor(helper);
		p.setHealth(10);
		float max = p.getMaxHealth();
		helper.runAfterDelay(40, () -> {
			// 2 seconds at 1% of max per second, no vanilla regeneration on top (full saturation would heal far faster).
			float expected = 10 + max * 0.02f;
			helper.assertTrue(Math.abs(p.getHealth() - expected) < max * 0.004f, "regen " + p.getHealth() + " expected ~" + expected);
			p.setHealth(max - 0.01f);
		});
		helper.runAfterDelay(60, () -> {
			helper.assertTrue(p.getHealth() == max, "never above maximum: " + p.getHealth());
			remove(helper, p);
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "core_fall")
	public void noFallDamage(GameTestHelper helper) {
		ServerPlayer p = survivor(helper);
		float before = p.getHealth();
		helper.assertTrue(!p.hurt(helper.getLevel().damageSources().fall(), 50), "fall damage cancelled");
		helper.assertTrue(p.getHealth() == before, "health unchanged");
		helper.assertTrue(p.hurt(helper.getLevel().damageSources().generic(), 2), "other damage still applies");
		helper.assertTrue(!helper.getLevel().getServer().getGameRules().getBoolean(GameRules.RULE_ANNOUNCE_ADVANCEMENTS), "advancements not announced");
		remove(helper, p);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "core_ocean", timeoutTicks = 120)
	public void seaWeakensOnlyFruitUsers(GameTestHelper helper) {
		boolean oceanOnly = GrandLineMod.config().oceanOnly;
		GrandLineMod.config().oceanOnly = false; // the test area is not an ocean biome; the water rules are tested in game
		for (int x = 0; x <= 4; x++) {
			for (int z = 0; z <= 4; z++) {
				helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
				helper.setBlock(new BlockPos(x, 2, z), Blocks.WATER);
				helper.setBlock(new BlockPos(x, 3, z), Blocks.WATER);
			}
		}
		ServerPlayer fruit = survivor(helper);
		ServerPlayer plain = survivor(helper);
		ServerPlayer immune = survivor(helper);
		PlayerProfileService.update(fruit, pr -> pr.withPowers(pr.powers().withFruit(Fruits.FLAME.id())));
		PlayerProfileService.update(immune, pr -> pr.withPowers(pr.powers().withFruit(Fruits.FLAME.id())));
		immune.addTag("grandline_test_sea_immune");
		OceanWeakness.addModifier(pl -> pl.getTags().contains("grandline_test_sea_immune") ? 0 : 1);
		for (ServerPlayer p : new ServerPlayer[]{fruit, plain, immune}) {
			Vec3 in = helper.absoluteVec(new Vec3(2.5, 2, 2.5));
			p.moveTo(in.x, in.y, in.z);
			p.setDeltaMovement(Vec3.ZERO);
		}
		// Mock players have no connection, so the game never runs their per-tick update; run the part that detects water.
		helper.onEachTick(() -> {
			for (ServerPlayer p : new ServerPlayer[]{fruit, plain, immune}) {
				if (!p.isRemoved()) {
					p.baseTick();
				}
			}
		});
		helper.runAfterDelay(10, () -> helper.assertTrue(OceanWeakness.affected(fruit) && !OceanWeakness.affected(plain), "only the fruit user is weakened"));
		helper.runAfterDelay(45, () -> {
			float max = fruit.getMaxHealth();
			helper.assertTrue(fruit.getHealth() < max - max * 0.05f, "fruit user loses ~5% max HP per second in the sea: " + fruit.getHealth());
			helper.assertTrue(plain.getHealth() == plain.getMaxHealth(), "no fruit: no sea damage");
			helper.assertTrue(immune.getHealth() == immune.getMaxHealth(), "a 0 multiplier makes the sea harmless");
			GrandLineMod.config().oceanOnly = oceanOnly;
			remove(helper, fruit, plain, immune);
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "core_respawn")
	public void respawnPicksDeathIslandThenHomeThenSpawn(GameTestHelper helper) {
		IslandRegistry registry = IslandRegistry.get(helper.getLevel().getServer());
		Island spawn = new Island(990001, 50_000, 50_000, 20, 30, GrandLineMod.config().spawnIsland, false, true, "village");
		Island died = new Island(990002, 51_000, 50_000, 20, 15, "Test Death Isle", false, true, "jungle");
		Island home = new Island(990003, 52_000, 50_000, 20, 15, "Test Home Isle", false, true, "town");
		boolean hadSpawn = IslandSpawns.spawnIsland(helper.getLevel().getServer()) != null;
		registry.add(died);
		registry.add(home);
		if (!hadSpawn) {
			registry.add(spawn);
		}
		ServerPlayer p = survivor(helper);
		helper.assertTrue(IslandSpawns.respawnIsland(p, died.id()).id() == died.id(), "died on an island: back on that island");
		helper.assertTrue(IslandSpawns.respawnIsland(p, -1).name().equals(GrandLineMod.config().spawnIsland), "no home: spawn island");
		p.setAttached(ModAttachments.HOME_ISLAND, home.id());
		helper.assertTrue(IslandSpawns.respawnIsland(p, -1).id() == home.id(), "died at sea: home island");
		helper.assertTrue(IslandSpawns.islandAt(helper.getLevel(), died.x() + 5, died.z()) != null, "on land: the island is found");
		helper.assertTrue(IslandSpawns.islandAt(helper.getLevel(), died.x() + 60, died.z()) == null, "out at sea: no island");
		remove(helper, p);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "core_levelup")
	public void levelUpRestoresFullHealth(GameTestHelper helper) {
		ServerPlayer p = survivor(helper);
		p.setHealth(4);
		int before = PlayerProfileService.get(p).level();
		PlayerProfileService.addExperience(p, PlayerProfileService.rules().experienceToNextLevel(before) * 3);
		helper.assertTrue(PlayerProfileService.get(p).level() > before, "levelled up");
		helper.assertTrue(p.getHealth() == p.getMaxHealth(), "full health after level up: " + p.getHealth());
		remove(helper, p);
		helper.succeed();
	}
}
