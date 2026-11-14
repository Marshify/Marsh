package com.grandline.test;

import com.grandline.GrandLineMod;
import com.grandline.combat.EnergyService;
import com.grandline.data.ModAttachments;
import com.grandline.entity.GrandLineEnemy;
import com.grandline.entity.GrandLineEntities;
import com.grandline.entity.Islander;
import com.grandline.player.PlayerProfileService;
import com.grandline.player.TeamService;
import com.grandline.race.Race;
import com.grandline.race.RaceAbilities;
import com.grandline.race.RaceAwakening;
import com.grandline.race.RaceData;
import com.grandline.race.RaceNpcs;
import com.grandline.race.RacePassives;
import com.grandline.race.RaceService;
import com.grandline.race.RaceTrials;
import com.grandline.race.RaceUpgrades;
import com.grandline.registry.ModItems;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** The race system: assignment, persistence, passives, V3/V4, progression, unlocks, reroll, upgrades and trials. */
public final class GrandLineRaceGameTests implements FabricGameTest {
	/**
	 * Gives a mock player a race with no combat, health or energy bonus (Rabbit: speed and jumps only), so tests of
	 * other features measure only that feature.
	 */
	static void neutral(ServerPlayer p) {
		RaceNpcs.changeRace(p, Race.RABBIT);
	}

	private static ServerPlayer player(GameTestHelper helper) {
		return GrandLineBatch3GameTests.survivor(helper);
	}

	private static void as(ServerPlayer p, Race race, int stage) {
		RaceNpcs.changeRace(p, race);
		RaceService.update(p, d -> d.withStage(stage));
	}

	private static GrandLineEnemy dummy(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		GrandLineEnemy b = GrandLineEntities.BANDIT.create(level);
		Vec3 at = helper.absoluteVec(new Vec3(4.5, 2, 4.5));
		b.moveTo(at.x, at.y, at.z, 0, 0);
		b.setGrandLineLevel(5);
		b.setNoAi(true);
		level.addFreshEntity(b);
		return b;
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "race_assign")
	public void startingRacesFollowTheWeightsAndNeverUnlockables(GameTestHelper helper) {
		RandomSource random = RandomSource.create(42);
		Map<Race, Integer> counts = new EnumMap<>(Race.class);
		int n = 16_000;
		for (int i = 0; i < n; i++) {
			counts.merge(RaceService.rollStarting(random, null), 1, Integer::sum);
		}
		helper.assertTrue(counts.getOrDefault(Race.CYBORG, 0) == 0 && counts.getOrDefault(Race.GHOUL, 0) == 0, "no unlockable race at start");
		double human = counts.get(Race.HUMAN) / (double) n;
		helper.assertTrue(Math.abs(human - 0.625) < 0.02, "Human about 62.5%: " + human);
		for (Race r : List.of(Race.SHARK, Race.ANGEL, Race.RABBIT)) {
			double share = counts.get(r) / (double) n;
			helper.assertTrue(Math.abs(share - 0.125) < 0.015, r + " about 12.5%: " + share);
		}
		for (int i = 0; i < 200; i++) {
			helper.assertTrue(RaceService.rollStarting(random, Race.HUMAN) != Race.HUMAN, "a reroll never gives the same race");
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "race_assign")
	public void raceIsAssignedOnceAndKeptThroughDeathAndSideChanges(GameTestHelper helper) {
		ServerPlayer a = player(helper);
		a.removeAttached(ModAttachments.RACE);
		RaceData first = RaceService.get(a);
		helper.assertTrue(first.kind().standard && first.stage() == 1, "a standard race at V1: " + first);
		for (int i = 0; i < 20; i++) {
			helper.assertTrue(RaceService.get(a).race().equals(first.race()), "asking again never rerolls");
		}
		RaceService.update(a, d -> d.withStage(3).withFlag("unlock:ghoul"));
		TeamService.choose(a, TeamService.Side.MARINES);
		helper.assertTrue(RaceService.get(a).race().equals(first.race()) && RaceService.get(a).stage() == 3, "a side change keeps the race");
		// Respawn after death: the new player entity gets the saved race (copied on death).
		ServerPlayer b = helper.makeMockServerPlayerInLevel();
		b.restoreFrom(a, false);
		RaceData after = b.getAttached(ModAttachments.RACE);
		helper.assertTrue(after != null && after.race().equals(first.race()) && after.stage() == 3 && after.has("unlock:ghoul"),
				"race and progression survive death: " + after);
		// Another player has their own race.
		ServerPlayer c = player(helper);
		c.removeAttached(ModAttachments.RACE);
		as(c, Race.RABBIT, 2);
		helper.assertTrue(RaceService.get(b).stage() == 3 && RaceService.race(c) == Race.RABBIT, "players are independent");
		GrandLineBatch3GameTests.remove(helper, a, b, c);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "race_passives")
	public void damageBonusesReductionsAndCaps(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer human = player(helper);
		ServerPlayer shark = player(helper);
		GrandLineEnemy target = dummy(helper);
		as(human, Race.HUMAN, 1);
		as(shark, Race.SHARK, 4);
		float dealt = RacePassives.modifyDamage(target, level.damageSources().playerAttack(human), 10);
		helper.assertTrue(Math.abs(dealt - 10.5f) < 0.01, "Human V1 +5% damage: " + dealt);
		float taken = RacePassives.modifyDamage(shark, level.damageSources().mobAttack(target), 10);
		helper.assertTrue(Math.abs(taken - 8.8f) < 0.01, "Shark V4 -12% damage taken: " + taken);
		RaceService.update(shark, d -> d.withEnergy(100));
		RaceAbilities.useV3(shark);
		RaceAbilities.awaken(shark);
		helper.assertTrue(RaceService.v3Active(shark) && RaceService.v4Active(shark), "Water Body and Leviathan both on");
		float capped = RacePassives.modifyDamage(shark, level.damageSources().mobAttack(target), 10);
		helper.assertTrue(Math.abs(capped - 2.5f) < 0.01, "stacked reductions are capped at 75%: " + capped);
		helper.assertTrue(RacePassives.oceanMultiplier(shark) >= 0.15 - 1e-9 && RacePassives.oceanMultiplier(shark) < 0.4,
				"Shark sea protection grows but never reaches immunity: " + RacePassives.oceanMultiplier(shark));
		as(shark, Race.SHARK, 1);
		helper.assertTrue(Math.abs(RacePassives.oceanMultiplier(shark) - 0.85) < 1e-9, "V1 Shark only softens sea weakness");
		as(human, Race.HUMAN, 1);
		helper.assertTrue(RacePassives.oceanMultiplier(human) == 1, "other races take full sea weakness");
		GrandLineBatch3GameTests.remove(helper, human, shark, target);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "race_passives", timeoutTicks = 60)
	public void ghoulLifestealIsCappedAndNeedsNightAtV1(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		long time = level.getDayTime();
		ServerPlayer ghoul = player(helper);
		as(ghoul, Race.GHOUL, 1);
		level.setDayTime(6000);
		helper.assertTrue(RacePassives.lifesteal(ghoul) == 0, "V1 Ghouls don't feed by day");
		level.setDayTime(18000);
		helper.assertTrue(Math.abs(RacePassives.lifesteal(ghoul) - 0.03) < 1e-9, "V1 Ghoul lifesteal at night");
		ghoul.setHealth(4);
		GrandLineEnemy target = dummy(helper);
		target.hurt(level.damageSources().playerAttack(ghoul), 500);
		float healed = ghoul.getHealth() - 4;
		helper.assertTrue(healed > 0 && healed <= ghoul.getMaxHealth() * 0.06f + 0.01f, "lifesteal heals at most 6% per hit: " + healed);
		level.setDayTime(time);
		GrandLineBatch3GameTests.remove(helper, ghoul, target);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "race_passives")
	public void energyAndCooldownMultipliers(GameTestHelper helper) {
		ServerPlayer cyborg = player(helper);
		as(cyborg, Race.CYBORG, 1);
		helper.assertTrue(Math.abs(RacePassives.energyMultiplier(cyborg) - 0.9) < 1e-9, "Cyborg V1 -10% energy");
		EnergyService.refill(cyborg);
		double before = EnergyService.current(cyborg);
		EnergyService.trySpend(cyborg, 0.1);
		double spent = before - EnergyService.current(cyborg);
		helper.assertTrue(Math.abs(spent - EnergyService.max(cyborg) * 0.09) < 1e-6, "a move costs 10% less: " + spent);
		ServerPlayer ghoul = player(helper);
		as(ghoul, Race.GHOUL, 4);
		helper.assertTrue(Math.abs(RacePassives.cooldownMultiplier(ghoul) - 0.9) < 1e-9, "Ghoul V4 -10% cooldowns");
		GrandLineBatch3GameTests.remove(helper, cyborg, ghoul);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "race_v3")
	public void raceAbilityUnlocksAtV3AndHasACooldown(GameTestHelper helper) {
		ServerPlayer p = player(helper);
		as(p, Race.RABBIT, 2);
		helper.assertTrue(RaceAbilities.useV3(p) != null && !RaceService.v3Active(p), "locked below V3");
		RaceService.update(p, d -> d.withStage(3));
		helper.assertTrue(RaceAbilities.useV3(p) == null && RaceService.v3Active(p), "V3 Agility starts");
		helper.assertTrue("race.grandline.v3.active".equals(RaceAbilities.useV3(p)), "can't stack the same ability");
		long ready = RaceService.get(p).counter("v3_ready");
		long now = p.serverLevel().getGameTime();
		helper.assertTrue(ready - now >= (8 + 50) * 20 - 2, "cooldown counts from the start: " + (ready - now));
		// A clock that went backwards (another world) never locks the ability for longer than its cooldown.
		RaceService.update(p, d -> d.withCounter("v3_ready", now + 10_000_000L));
		RaceNpcs.changeRace(p, Race.RABBIT);
		RaceService.update(p, d -> d.withStage(3).withCounter("v3_ready", now + 10_000_000L));
		helper.assertTrue(RaceAbilities.useV3(p) == null, "far-future cooldowns are treated as stale");
		GrandLineBatch3GameTests.remove(helper, p);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "race_v4")
	public void awakeningMeterFillsCappedAndEmptiesOnAwaken(GameTestHelper helper) {
		ServerPlayer p = player(helper);
		as(p, Race.HUMAN, 3);
		helper.assertTrue(RaceAwakening.gain(p, 50) == 0, "no meter before V4");
		RaceService.update(p, d -> d.withStage(4));
		int got = 0;
		for (int i = 0; i < 20; i++) {
			got += RaceAwakening.gain(p, 50);
		}
		int cap = GrandLineMod.config().awakeningEnergyPerSecondCap;
		helper.assertTrue(got <= cap * 2 && got > 0, "the meter's gain is capped per second: " + got);
		helper.assertTrue("race.grandline.v4.energy".equals(RaceAbilities.awaken(p)), "can't awaken until full");
		RaceService.update(p, d -> d.withEnergy(100));
		helper.assertTrue(RaceAbilities.awaken(p) == null && RaceService.v4Active(p) && RaceService.get(p).energy() == 0,
				"awakening empties the meter");
		helper.assertTrue(RaceAwakening.gain(p, 50) == 0, "no gain while awakened");
		GrandLineBatch3GameTests.remove(helper, p);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "race_v4")
	public void upgradeTreeRules(GameTestHelper helper) {
		ServerPlayer p = player(helper);
		as(p, Race.SHARK, 4);
		List<RaceUpgrades.Upgrade> tree = RaceUpgrades.forRace(Race.SHARK);
		helper.assertTrue(tree.size() == 6, "three tiers of two");
		helper.assertTrue(RaceAbilities.buyUpgrade(p, 0) != null, "no points, no upgrade");
		RaceService.update(p, d -> d.withCounter("awaken_points", 9));
		helper.assertTrue(RaceAbilities.buyUpgrade(p, 2) != null, "tier 2 needs tier 1");
		helper.assertTrue(RaceAbilities.buyUpgrade(p, 0) == null, "tier 1 bought");
		helper.assertTrue(RaceAbilities.buyUpgrade(p, 0) != null && RaceAbilities.buyUpgrade(p, 1) != null, "one choice per tier, no repeats");
		helper.assertTrue(RaceAbilities.buyUpgrade(p, 2) == null && RaceAbilities.buyUpgrade(p, 4) == null, "tiers 2 and 3");
		RaceData d = RaceService.get(p);
		helper.assertTrue(d.upgrades().size() == 3 && RaceUpgrades.pointsFree(d) == 0, "all 9 points spent: " + d.upgrades());
		helper.assertTrue(Math.abs(RaceUpgrades.bonus(d, "reduction") - 0.14) < 1e-9, "each bonus counts once: " + RaceUpgrades.bonus(d, "reduction"));
		GrandLineBatch3GameTests.remove(helper, p);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "race_progress")
	public void evolvingToV2NeedsTheQuestAndPays(GameTestHelper helper) {
		ServerPlayer p = player(helper);
		as(p, Race.ANGEL, 1);
		PlayerProfileService.update(p, pr -> pr.withLevel(60).withCurrency(30_000));
		helper.assertTrue(RaceNpcs.act(p, RaceNpcs.EVOLVE, Islander.Role.RACE_MYSTIC) != null, "requirements not met yet");
		helper.assertTrue(RaceNpcs.act(p, RaceNpcs.START_QUEST, Islander.Role.RACE_MYSTIC) == null && RaceService.get(p).has("q2"), "quest begun");
		RaceService.update(p, d -> d.withCounter("v2_kills", GrandLineMod.config().raceV2Kills).withFlag("isl:1").withFlag("isl:2"));
		helper.assertTrue(RaceNpcs.act(p, RaceNpcs.EVOLVE, Islander.Role.RACE_MYSTIC) != null, "still one island short");
		RaceService.update(p, d -> d.withFlag("isl:3"));
		helper.assertTrue(RaceNpcs.act(p, RaceNpcs.EVOLVE, Islander.Role.TINKERER) != null, "only the Mystic evolves V1-V3");
		helper.assertTrue(RaceNpcs.act(p, RaceNpcs.EVOLVE, Islander.Role.RACE_MYSTIC) == null, "evolves");
		RaceData d = RaceService.get(p);
		helper.assertTrue(d.stage() == 2 && !d.has("q2") && !d.has("isl:1") && d.counter("v2_kills") == 0, "V2, quest cleared: " + d);
		helper.assertTrue(PlayerProfileService.get(p).currency() == 5_000, "paid 25,000");
		GrandLineBatch3GameTests.remove(helper, p);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "race_progress")
	public void rerollAndUnlockFollowTheSameRules(GameTestHelper helper) {
		ServerPlayer p = player(helper);
		as(p, Race.HUMAN, 4);
		RaceService.update(p, d -> d.withUpgrades(List.of("human_t1a")).withCounter("awaken_points", 5).withEnergy(70));
		PlayerProfileService.update(p, pr -> pr.withCurrency(10));
		helper.assertTrue(RaceNpcs.act(p, RaceNpcs.REROLL, Islander.Role.RACE_MYSTIC) != null && RaceService.race(p) == Race.HUMAN, "can't afford");
		PlayerProfileService.update(p, pr -> pr.withCurrency(GrandLineMod.config().raceRerollCost));
		helper.assertTrue(RaceNpcs.act(p, RaceNpcs.REROLL, Islander.Role.RACE_MYSTIC) == null, "rerolled");
		RaceData d = RaceService.get(p);
		helper.assertTrue(d.kind() != Race.HUMAN && d.kind().standard && d.stage() == 1 && d.upgrades().isEmpty() && d.energy() == 0
				&& d.counter("awaken_points") == 0 && PlayerProfileService.get(p).currency() == 0, "new race, V1, progress reset: " + d);
		// Cyborg: parts + boss + cost, then later switches cost the reroll price only.
		PlayerProfileService.update(p, pr -> pr.withCurrency(GrandLineMod.config().raceUnlockCost));
		helper.assertTrue(RaceNpcs.act(p, RaceNpcs.BECOME, Islander.Role.TINKERER) != null, "needs parts and the boss");
		p.getInventory().add(new ItemStack(ModItems.MACHINE_CORE, GrandLineMod.config().raceUnlockParts));
		RaceService.update(p, x -> x.withFlag("boss:cyborg"));
		helper.assertTrue(RaceNpcs.act(p, RaceNpcs.BECOME, Islander.Role.TINKERER) == null && RaceService.race(p) == Race.CYBORG, "Cyborg unlocked");
		helper.assertTrue(p.getInventory().countItem(ModItems.MACHINE_CORE) == 0 && PlayerProfileService.get(p).currency() == 0, "parts and cost taken");
		PlayerProfileService.update(p, pr -> pr.withCurrency(GrandLineMod.config().raceRerollCost));
		helper.assertTrue(RaceNpcs.act(p, RaceNpcs.REROLL, Islander.Role.RACE_MYSTIC) == null && RaceService.race(p) != Race.CYBORG
				&& RaceService.race(p).standard, "a reroll gives a standard race");
		helper.assertTrue(RaceService.get(p).has("unlock:cyborg"), "unlocks are kept");
		PlayerProfileService.update(p, pr -> pr.withCurrency(GrandLineMod.config().raceRerollCost));
		helper.assertTrue(RaceNpcs.act(p, RaceNpcs.BECOME, Islander.Role.TINKERER) == null && RaceService.race(p) == Race.CYBORG,
				"switching back to an unlocked race needs no parts");
		GrandLineBatch3GameTests.remove(helper, p);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "race_trials", timeoutTicks = 200)
	public void soloTrialBuildsAnArenaReturnsThePlayerAndGivesPoints(GameTestHelper helper) {
		ServerPlayer p = player(helper);
		as(p, Race.HUMAN, 3);
		Vec3 start = p.position();
		helper.assertTrue(RaceNpcs.act(p, RaceNpcs.TRIAL, Islander.Role.TRIAL_KEEPER) != null, "the trial needs the discovery first");
		RaceService.update(p, d -> d.withFlag("lore").withFlag("found_keeper"));
		helper.assertTrue(RaceTrials.start(p) == null, "trial starts");
		BlockPos arena = RaceTrials.arena(p);
		helper.assertTrue(arena != null && !p.serverLevel().getBlockState(arena).isAir(), "the arena floor is built");
		helper.assertTrue(p.position().distanceTo(Vec3.atCenterOf(arena)) < 4, "the player is in the arena");
		helper.assertTrue(RaceTrials.start(p) != null, "one trial at a time");
		helper.runAfterDelay(25, () -> {
			long enemies = p.serverLevel().getEntitiesOfClass(GrandLineEnemy.class, new net.minecraft.world.phys.AABB(arena).inflate(20),
					e -> e.getTags().contains(RaceTrials.TAG)).size();
			helper.assertTrue(enemies >= 3, "the first wave is there: " + enemies);
			RaceTrials.finish(p, true);
			helper.assertTrue(p.position().distanceTo(start) < 1, "returned to where they started");
			helper.assertTrue(p.serverLevel().getBlockState(arena).isAir(), "the arena is removed");
			RaceData d = RaceService.get(p);
			helper.assertTrue(d.has("trial:human") && d.counter("awaken_points") == 3, "first clear: 3 points: " + d);
			for (int i = 0; i < 10; i++) {
				RaceTrials.start(p);
				RaceTrials.finish(p, true);
			}
			helper.assertTrue(RaceService.get(p).counter("awaken_points") == 9, "points are capped at 9");
			RaceTrials.start(p);
			RaceTrials.finish(p, false);
			helper.assertTrue(RaceService.get(p).counter("awaken_points") == 9, "a failed trial gives nothing");
			GrandLineBatch3GameTests.remove(helper, p);
			helper.succeed();
		});
	}
}
