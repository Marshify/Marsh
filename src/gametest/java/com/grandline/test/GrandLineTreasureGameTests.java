package com.grandline.test;

import com.grandline.data.ModAttachments;
import com.grandline.player.PlayerProfileService;
import com.grandline.treasure.TreasureBlocks;
import com.grandline.treasure.TreasureChestBlock;
import com.grandline.treasure.TreasureService;
import com.grandline.treasure.TreasureSpot;
import com.grandline.treasure.TreasureSpots;
import com.grandline.treasure.TreasureTier;
import com.grandline.world.FirstSeaProgression;
import com.grandline.world.SeaLayout;
import com.grandline.world.plan.IslandPlans;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Treasure chests: per-player Beli and refill timers, tier rewards, and the hand-placed spot table. */
public final class GrandLineTreasureGameTests implements FabricGameTest {
	@GameTest(template = EMPTY_STRUCTURE, batch = "treasure")
	public void openingPaysOncePerPlayerUntilItRefills(GameTestHelper helper) {
		BlockPos rel = new BlockPos(1, 2, 1);
		helper.setBlock(rel, TreasureBlocks.get(TreasureTier.RARE).defaultBlockState().setValue(TreasureChestBlock.FACING, Direction.NORTH));
		BlockPos pos = helper.absolutePos(rel);
		BlockState state = helper.getLevel().getBlockState(pos);
		ServerPlayer a = helper.makeMockServerPlayerInLevel();
		ServerPlayer b = helper.makeMockServerPlayerInLevel();
		long before = PlayerProfileService.get(a).currency();
		long paid = TreasureService.open(a, pos, state);
		long base = TreasureService.baseReward(null, TreasureTier.RARE);
		helper.assertTrue(paid >= Math.round(base * 0.85) - 1 && paid <= Math.round(base * 1.15) + 1, "a rare chest pays about " + base + ": " + paid);
		helper.assertTrue(PlayerProfileService.get(a).currency() == before + paid, "the Beli is added");
		helper.assertTrue(TreasureService.open(a, pos, state) == 0, "the same player cannot open it again while it refills");
		long refill = TreasureService.refillsAt(a, pos) - helper.getLevel().getGameTime();
		helper.assertTrue(refill > 20L * 60 * 40 && refill <= 20L * 60 * 45, "a rare chest refills in 45 minutes: " + refill);
		helper.assertTrue(TreasureService.open(b, pos, state) > 0, "another player still finds it full");
		// Once the timer has run out it pays again.
		Map<String, Long> refills = new HashMap<>(a.getAttached(ModAttachments.TREASURE_REFILLS));
		refills.replaceAll((k, v) -> helper.getLevel().getGameTime() - 1);
		a.setAttached(ModAttachments.TREASURE_REFILLS, refills);
		helper.assertTrue(TreasureService.open(a, pos, state) > 0, "refilled: pays again");
		helper.getLevel().removePlayerImmediately(a, Entity.RemovalReason.DISCARDED);
		helper.getLevel().removePlayerImmediately(b, Entity.RemovalReason.DISCARDED);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "treasure")
	public void rewardsRiseWithTierAndIsland(GameTestHelper helper) {
		long c = TreasureService.baseReward(null, TreasureTier.COMMON), r = TreasureService.baseReward(null, TreasureTier.RARE),
				l = TreasureService.baseReward(null, TreasureTier.LEGENDARY);
		helper.assertTrue(c < r && r < l, "common < rare < legendary: " + c + " " + r + " " + l);
		helper.assertTrue(c == FirstSeaProgression.questCurrency(FirstSeaProgression.STAGES.get(0)), "a common chest is worth one quest");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "treasure")
	public void everyIslandHasSensiblySpacedChests(GameTestHelper helper) {
		java.util.Set<String> islands = new java.util.HashSet<>();
		for (SeaLayout.Spec spec : SeaLayout.all()) {
			islands.add(spec.name());
		}
		for (var plan : IslandPlans.all()) {
			if (!islands.contains(plan.name()) || SeaLayout.all().stream().noneMatch(s -> s.name().equals(plan.name()) && s.sea() == 1)) {
				continue;
			}
			if (TreasureSpots.NONE_BY_DESIGN.contains(plan.name())) {
				continue;
			}
			List<TreasureSpot> spots = TreasureSpots.forIsland(plan.name());
			helper.assertTrue(spots.size() >= 3, plan.name() + " has treasure chests: " + spots.size());
			helper.assertTrue(spots.stream().filter(s -> s.tier() == TreasureTier.LEGENDARY).count() <= 1, plan.name() + ": at most one legendary");
			for (int i = 0; i < spots.size(); i++) {
				for (int j = i + 1; j < spots.size(); j++) {
					TreasureSpot s = spots.get(i), t = spots.get(j);
					helper.assertTrue(Math.abs(s.x() - t.x()) + Math.abs(s.z() - t.z()) >= 12, plan.name() + ": chests spread out " + s + " " + t);
				}
				TreasureSpot s = spots.get(i);
				helper.assertTrue(s.x() * s.x() + s.z() * s.z() <= plan.extent() * plan.extent(), plan.name() + ": chest on the island " + s);
			}
		}
		helper.succeed();
	}
}
