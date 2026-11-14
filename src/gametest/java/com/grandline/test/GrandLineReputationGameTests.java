package com.grandline.test;

import com.grandline.GrandLineMod;
import com.grandline.entity.EnemyKind;
import com.grandline.entity.GrandLineEntities;
import com.grandline.entity.GrandLineEnemy;
import com.grandline.player.PlayerProfileService;
import com.grandline.player.TeamService;
import com.grandline.reputation.ReputationService;
import com.grandline.reputation.ReputationTestAccess;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.phys.Vec3;

/** Bounty and Honor: sides, caps, the 2.5M loss rule, the pair limit, combat-tag disconnects and single payouts. */
public final class GrandLineReputationGameTests implements FabricGameTest {
	private static ServerPlayer player(GameTestHelper helper, TeamService.Side side, int level) {
		helper.getLevel().getServer().setPvpAllowed(true); // the game-test server starts with PvP off
		ServerPlayer p = helper.makeMockServerPlayerInLevel();
		GrandLineRaceGameTests.neutral(p); // no race bonus in this feature's numbers
		p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL); // mock players start in creative (cannot be hurt)
		try { // and with spawn protection, which mock players never tick down
			var field = ServerPlayer.class.getDeclaredField("spawnInvulnerableTime");
			field.setAccessible(true);
			field.setInt(p, 0);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(e);
		}
		Vec3 home = helper.absoluteVec(new Vec3(1.5, 2, 1.5));
		p.moveTo(home.x, home.y, home.z, 0, 0);
		TeamService.choose(p, side);
		PlayerProfileService.update(p, prof -> prof.withLevel(level));
		ReputationService.setTotal(p, true, 0);
		ReputationService.setTotal(p, false, 0);
		return p;
	}

	private static void eq(GameTestHelper helper, long actual, long expected, String what) {
		helper.assertTrue(actual == expected, what + ": expected " + expected + " but got " + actual);
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "rep_1")
	public void sidesKeepSeparateTotals(GameTestHelper helper) {
		ReputationTestAccess.reset();
		ServerPlayer p = player(helper, TeamService.Side.PIRATES, 100);
		ReputationService.setTotal(p, true, 5_000);
		eq(helper, ReputationService.active(p), 5_000, "pirate shows bounty");
		TeamService.choose(p, TeamService.Side.MARINES);
		eq(helper, ReputationService.active(p), 0, "marine shows honor (starts at 0)");
		ReputationService.setTotal(p, false, 700);
		TeamService.choose(p, TeamService.Side.PIRATES);
		eq(helper, ReputationService.active(p), 5_000, "switching back restores bounty");
		eq(helper, ReputationService.get(p).honor(), 700, "honor kept");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "rep_2")
	public void enemyAndBossRewardsStopAtTheirCaps(GameTestHelper helper) {
		ReputationTestAccess.reset();
		ServerPlayer p = player(helper, TeamService.Side.PIRATES, 300);
		GrandLineEnemy enemy = GrandLineEntities.typeOf(EnemyKind.forLevel(200)).create(helper.getLevel());
		enemy.setGrandLineLevel(200);
		ReputationService.onMobKilled(p, enemy);
		eq(helper, ReputationService.active(p), 200, "enemy gives its level");
		ReputationService.setTotal(p, true, 249_900);
		ReputationService.onMobKilled(p, enemy);
		eq(helper, ReputationService.active(p), 250_000, "clamped at the enemy cap");
		ReputationService.onMobKilled(p, enemy);
		eq(helper, ReputationService.active(p), 250_000, "no more from enemies");
		var boss = GrandLineEntities.CAPTAIN.create(helper.getLevel());
		boss.setGrandLineLevel(300);
		ReputationService.onMobKilled(p, boss);
		eq(helper, ReputationService.active(p), 250_000 + 25 * 300, "boss gives 25 x level past the enemy cap");
		ReputationService.setTotal(p, true, 2_499_000);
		ReputationService.onMobKilled(p, boss);
		eq(helper, ReputationService.active(p), 2_500_000, "clamped at the boss cap");
		ReputationService.onMobKilled(p, boss);
		eq(helper, ReputationService.active(p), 2_500_000, "PvE stops at 2.5M");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "rep_3")
	public void pvpLossStartsAt2500000AndCapAt30M(GameTestHelper helper) {
		ReputationTestAccess.reset();
		ReputationTestAccess.clock(1_000_000L);
		ServerPlayer a = player(helper, TeamService.Side.PIRATES, 500);
		ServerPlayer b = player(helper, TeamService.Side.PIRATES, 500);
		ReputationService.setTotal(b, true, 2_499_999);
		ReputationService.pvpKill(a, b);
		eq(helper, ReputationService.active(b), 2_499_999, "no loss below 2.5M");
		eq(helper, ReputationService.active(a), 1_750, "3.5 x 500");
		ReputationService.setTotal(b, true, 2_500_000);
		ReputationService.setTotal(a, true, 0);
		ReputationService.pvpKill(a, b);
		eq(helper, ReputationService.active(a), 2_250, "level 500 at exactly 2.5M pays 2,250");
		eq(helper, ReputationService.active(b), 2_500_000 - 2_250, "and costs 2,250");
		ServerPlayer c = player(helper, TeamService.Side.MARINES, 500);
		ServerPlayer d = player(helper, TeamService.Side.PIRATES, 500);
		ReputationService.setTotal(c, false, 29_999_000);
		ReputationService.setTotal(d, true, 10_000_000);
		ReputationService.pvpKill(c, d);
		eq(helper, ReputationService.active(c), 30_000_000, "winner capped at 30M");
		eq(helper, ReputationService.active(d), 10_000_000 - 2 * 3_500, "victim loss uses the full payout (7 x 500 x 2)");
		ReputationTestAccess.reset();
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "rep_4")
	public void fourthResultBetweenTheSamePairCountsForNothing(GameTestHelper helper) {
		ReputationTestAccess.reset();
		ReputationTestAccess.clock(5_000_000L);
		ServerPlayer a = player(helper, TeamService.Side.PIRATES, 100);
		ServerPlayer b = player(helper, TeamService.Side.PIRATES, 100);
		var first = ReputationService.pvpKill(a, b);
		helper.assertTrue(first == ReputationService.Result.AWARDED, "kill 1 (a wins): " + first + " pvp=" + helper.getLevel().getServer().isPvpAllowed());
		eq(helper, ReputationService.pvpKill(a, b) == ReputationService.Result.AWARDED ? 1 : 0, 1, "kill 2 (a wins)");
		eq(helper, ReputationService.pvpKill(b, a) == ReputationService.Result.AWARDED ? 1 : 0, 1, "kill 3 (b wins, shared limit)");
		long aBefore = ReputationService.active(a);
		long bBefore = ReputationService.active(b);
		helper.assertTrue(ReputationService.pvpKill(a, b) == ReputationService.Result.PAIR_LIMIT, "4th result rejected");
		eq(helper, ReputationService.active(a), aBefore, "no gain");
		eq(helper, ReputationService.active(b), bBefore, "no loss");
		// The history is per pair of UUIDs, so switching sides keeps it. (Mock players share one name, so they would share a
		// scoreboard team here; check the stored history directly instead of fighting across teams.)
		TeamService.choose(a, TeamService.Side.MARINES);
		eq(helper, ReputationTestAccess.pairCount(a, b), 3, "pair history survives a side switch");
		TeamService.choose(a, TeamService.Side.PIRATES);
		ReputationTestAccess.clock(5_000_000L + 72L * 3_600_000L);
		helper.assertTrue(ReputationService.pvpKill(a, b) == ReputationService.Result.AWARDED, "window over after 72 hours");
		ReputationTestAccess.reset();
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "rep_5")
	public void disconnectWhileTaggedTakesTheLossOnce(GameTestHelper helper) {
		ReputationTestAccess.reset();
		ReputationTestAccess.clock(9_000_000L);
		ServerPlayer attacker = player(helper, TeamService.Side.MARINES, 400);
		ServerPlayer runner = player(helper, TeamService.Side.PIRATES, 400);
		ReputationService.setTotal(runner, true, 3_000_000);
		runner.hurt(helper.getLevel().damageSources().playerAttack(attacker), 1f);
		helper.assertTrue(ReputationService.isTagged(runner) && ReputationService.isTagged(attacker), "both tagged");
		TeamService.choose(runner, TeamService.Side.MARINES);
		helper.assertTrue(TeamService.get(runner) == TeamService.Side.PIRATES, "cannot switch sides while tagged");
		long payout = ReputationService.rules().payout(400, 3_000_000);
		ReputationTestAccess.disconnect(runner);
		eq(helper, ReputationService.get(runner).bounty(), 3_000_000 - payout, "runner loses 1x payout (2.5M-4M)");
		eq(helper, ReputationService.get(attacker).honor(), payout, "attacker credited once");
		helper.assertTrue(!ReputationService.get(runner).notice().isEmpty(), "result shown at next login");
		ReputationTestAccess.disconnect(runner);
		eq(helper, ReputationService.get(runner).bounty(), 3_000_000 - payout, "tag cleared: no second loss");
		// An attacker who only dealt damage leaves freely.
		ReputationService.setTotal(attacker, false, 3_000_000);
		ReputationTestAccess.disconnect(attacker);
		eq(helper, ReputationService.get(attacker).honor(), 3_000_000, "attacker without received damage keeps everything");
		ReputationTestAccess.reset();
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "rep_6", timeoutTicks = 100)
	public void oneFruitKillPaysOnceAndBonusesIgnoreMobs(GameTestHelper helper) {
		ReputationTestAccess.reset();
		ReputationTestAccess.clock(20_000_000L);
		ServerPlayer killer = player(helper, TeamService.Side.PIRATES, 300);
		ServerPlayer victim = player(helper, TeamService.Side.PIRATES, 300);
		victim.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
		victim.setHealth(2f);
		var fruit = com.grandline.fruit.Fruits.FLAME.power();
		new com.grandline.combat.ability.AbilityContext(killer, fruit, fruit.abilities().get(0), 1.0).damage(victim, 50f);
		helper.assertTrue(victim.isDeadOrDying(), "victim died to the fruit hit");
		eq(helper, ReputationService.active(killer), Math.round(3.5 * 300), "exactly one payout");
		// PvP bonuses on: a mob takes exactly the base damage.
		var config = GrandLineMod.config();
		boolean old = config.reputationCombatBonuses;
		config.reputationCombatBonuses = true;
		ReputationService.setTotal(killer, true, 20_000_000);
		IronGolem golem = helper.spawn(EntityType.IRON_GOLEM, new net.minecraft.core.BlockPos(3, 2, 3));
		golem.setNoAi(true);
		float before = golem.getHealth();
		golem.hurt(helper.getLevel().damageSources().playerAttack(killer), 10f);
		config.reputationCombatBonuses = old;
		helper.assertTrue(Math.abs(before - golem.getHealth() - 10f) < 0.01, "mob damage unchanged by PvP bonuses: " + (before - golem.getHealth()));
		ReputationTestAccess.reset();
		helper.succeed();
	}
}
