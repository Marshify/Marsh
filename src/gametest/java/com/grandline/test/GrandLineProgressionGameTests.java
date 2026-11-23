package com.grandline.test;

import com.grandline.entity.EnemyKind;
import com.grandline.npc.QuestService;
import com.grandline.world.FirstSeaProgression;
import com.grandline.world.FirstSeaProgression.Stage;
import com.grandline.world.SeaLayout;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;

import java.util.List;

/** The First Sea ladder (Lv 1-700): ranges, island levels, enemy and boss levels, rewards, navigation targets. */
public final class GrandLineProgressionGameTests implements FabricGameTest {
	private static final List<String> ORDER = List.of("Verdant Wilds", "Saltmarket Docks", "Sunscorch Dunes", "Frostpeak Isle",
			"Redbrick Port", "Cloudtop Isles", "Stonewall Prison", "Gladiator's Ring", "Emberfall Isle", "Lotus Palace", "Cloudtop Isles",
			"Crystal Fountain");
	private static final int[] MINS = {10, 30, 60, 90, 120, 150, 190, 250, 300, 375, 450, 625};

	@GameTest(template = EMPTY_STRUCTURE, batch = "progression")
	public void ladderMatchesTheThirteenStages(GameTestHelper helper) {
		List<Stage> after = FirstSeaProgression.STAGES.stream().filter(s -> s.number() > 1).toList();
		helper.assertTrue(after.size() == 12, "12 stages after the starters: " + after.size());
		for (int i = 0; i < after.size(); i++) {
			Stage s = after.get(i);
			helper.assertTrue(s.island().equals(ORDER.get(i)) && s.min() == MINS[i] && s.number() == i + 2, "stage " + (i + 2) + ": " + s);
			int next = i + 1 < after.size() ? after.get(i + 1).min() : 700;
			helper.assertTrue(s.max() == next, s.label() + " ends where the next begins: " + s.max() + " vs " + next);
		}
		helper.assertTrue(FirstSeaProgression.stage("Cloudtop Isles", FirstSeaProgression.UPPER).min() == 450, "upper Cloudtop is its own stage");
		Stage mill = FirstSeaProgression.stage("Millbrook Village", 0), iron = FirstSeaProgression.stage("Ironhold Bastion", 0);
		helper.assertTrue(mill.min() == 1 && mill.max() == 10 && iron.min() == 1 && iron.max() == 10, "both starters are Lv 1-10");
		helper.assertTrue(Boolean.FALSE.equals(mill.marine()) && Boolean.TRUE.equals(iron.marine()), "Millbrook for Pirates, Ironhold for Marines");
		Stage red = FirstSeaProgression.stage("Redbrick Port", 0);
		helper.assertTrue(red.min() == 120 && red.max() == 150 && red.marine() == null, "Redbrick Port took Ironhold's old Lv 120-150 stage");
		// The island layout uses the same levels (and every name exists: no renames, no new islands).
		for (Stage s : FirstSeaProgression.STAGES) {
			SeaLayout.Spec spec = SeaLayout.all().stream().filter(x -> x.name().equals(s.island())).findFirst().orElse(null);
			helper.assertTrue(spec != null && spec.sea() == 1, s.island() + " is a First Sea island");
			if (s.region() == FirstSeaProgression.LOWER) {
				helper.assertTrue(spec.level() == s.min(), s.island() + " island level " + spec.level() + " = " + s.min());
			}
		}
		SeaLayout.Spec hub = SeaLayout.all().stream().filter(x -> x.name().equals("Dawnreach Harbor")).findFirst().orElseThrow();
		helper.assertTrue(hub.level() == 1 && hub.starter(), "Dawnreach stays the Lv 1 hub (no quests)");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "progression")
	public void boundariesSendPlayersToTheRightIsland(GameTestHelper helper) {
		Object[][] cases = {{1, false, "Millbrook Village", 0}, {1, true, "Ironhold Bastion", 0}, {9, true, "Ironhold Bastion", 0}, {120, false, "Redbrick Port", 0}, {149, true, "Redbrick Port", 0}, {9, false, "Millbrook Village", 0},
				{10, false, "Verdant Wilds", 0}, {10, true, "Verdant Wilds", 0}, {29, false, "Verdant Wilds", 0}, {30, false, "Saltmarket Docks", 0},
				{60, false, "Sunscorch Dunes", 0}, {150, false, "Cloudtop Isles", 0}, {189, false, "Cloudtop Isles", 0},
				{190, false, "Stonewall Prison", 0}, {250, false, "Gladiator's Ring", 0}, {300, false, "Emberfall Isle", 0},
				{375, false, "Lotus Palace", 0}, {449, false, "Lotus Palace", 0}, {450, false, "Cloudtop Isles", 1},
				{624, false, "Cloudtop Isles", 1}, {625, false, "Crystal Fountain", 0}, {700, false, "Crystal Fountain", 0},
				{1200, false, "Crystal Fountain", 0}};
		for (Object[] c : cases) {
			Stage s = FirstSeaProgression.stageFor((int) c[0], (boolean) c[1]);
			helper.assertTrue(s != null && s.island().equals(c[2]) && s.region() == (int) c[3], "Lv " + c[0] + (((boolean) c[1]) ? " Marine" : "")
					+ " -> " + s);
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "progression")
	public void enemiesBossesAndRewardsScaleAlongTheLadder(GameTestHelper helper) {
		RandomSource random = RandomSource.create(7);
		long prevXp = 0, prevMoney = 0;
		int prevBoss = 0;
		for (Stage s : FirstSeaProgression.STAGES) {
			int lowMax = 0, highMin = Integer.MAX_VALUE;
			for (int i = 0; i < 300; i++) {
				int basic = s.enemyLevel(0, random), strong = s.enemyLevel(1, random);
				helper.assertTrue(basic >= s.min() && basic <= s.max() && strong >= s.min() && strong <= s.max(), s.label() + " enemies in range");
				lowMax = Math.max(lowMax, basic);
				highMin = Math.min(highMin, strong);
			}
			helper.assertTrue(highMin >= s.min() + (s.max() - s.min()) / 4, s.label() + ": the second camp's enemies are the stronger ones");
			helper.assertTrue(s.bossLevel() >= s.min() + (s.max() - s.min()) * 3 / 4 && s.bossLevel() <= s.max(), s.label() + " boss near the top: " + s.bossLevel());
			if (s.number() > 1) {
				long xp = FirstSeaProgression.questExperience(s), money = FirstSeaProgression.questCurrency(s);
				helper.assertTrue(xp > prevXp && money > prevMoney && s.bossLevel() > prevBoss, s.label() + " rewards and boss rise along the ladder");
				prevXp = xp;
				prevMoney = money;
				prevBoss = s.bossLevel();
			}
		}
		// Millbrook's beginner enemies stay beginners, and no starter enemy outranks a later island's.
		Stage mill = FirstSeaProgression.stage("Millbrook Village", 0);
		for (int i = 0; i < 200; i++) {
			helper.assertTrue(mill.enemyLevel(0, random) <= 5, "Millbrook's first camp holds Lv 1-5 enemies");
		}
		// Quest pacing: one regular quest moves a player at the start of a stage by at least a level, and at the end by
		// at least two thirds of one; a whole stage takes a sensible number of quests.
		for (Stage s : FirstSeaProgression.STAGES) {
			long xp = FirstSeaProgression.questExperience(s);
			long atStart = 100 + 50L * (s.min() - 1), atEnd = 100 + 50L * (s.max() - 2);
			helper.assertTrue(xp >= atStart && xp * 3 >= atEnd * 2, s.label() + " quest pacing: " + xp + " vs " + atStart + "/" + atEnd);
			long total = 0;
			for (int l = s.min(); l < s.max(); l++) {
				total += 100 + 50L * (l - 1);
			}
			long quests = total / xp;
			helper.assertTrue(quests <= 140, s.label() + " takes about " + quests + " quests");
		}
		// The enemy kind each stage's quests ask for is the kind its camps hold.
		helper.assertTrue(EnemyKind.forLevel(FirstSeaProgression.stage("Cloudtop Isles", 1).min()) == EnemyKind.RAIDER
				&& EnemyKind.forLevel(FirstSeaProgression.stage("Cloudtop Isles", 0).min()) == EnemyKind.BANDIT, "Cloudtop regions have their own kinds");
		helper.assertTrue(QuestService.BOSS_MIN_ISLAND_LEVEL == 10, "boss quests from Verdant Wilds on");
		helper.succeed();
	}
}
