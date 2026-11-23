package com.grandline.test;

import com.grandline.data.ModAttachments;
import com.grandline.player.PlayerProfileService;
import com.grandline.treasure.TreasureBlocks;
import com.grandline.treasure.TreasureChestBlock;
import com.grandline.treasure.TreasureService;
import com.grandline.treasure.TreasureSpot;
import com.grandline.treasure.TreasureSpots;
import com.grandline.treasure.TreasureTier;
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
		long[] range = TreasureService.rewardRange(TreasureService.seaOf(null, pos), TreasureTier.RARE);
		helper.assertTrue(paid >= range[0] && paid <= range[1], "a rare chest pays within " + range[0] + "-" + range[1] + ": " + paid);
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
	public void rewardsDependOnlyOnSeaAndRarity(GameTestHelper helper) {
		// Islands from Lv 1 to Lv 625 of the First Sea: every rarity pays exactly the same range on all of them.
		List<String> names = List.of("Millbrook Village", "Ironhold Bastion", "Dawnreach Harbor", "Frostpeak Isle", "Redbrick Port",
				"Stonewall Prison", "Lotus Palace", "Crystal Fountain");
		for (TreasureTier tier : TreasureTier.values()) {
			long[] first = null;
			for (String name : names) {
				SeaLayout.Spec spec = SeaLayout.all().stream().filter(x -> x.name().equals(name)).findFirst().orElseThrow();
				var island = new com.grandline.world.Island(SeaLayout.idOf(spec), spec.x(), spec.z(), spec.radius(), spec.level(), spec.name(),
						false, true, spec.theme().name());
				long[] range = TreasureService.rewardRange(TreasureService.seaOf(island, new BlockPos(spec.x(), 64, spec.z())), tier);
				if (first == null) {
					first = range;
				}
				helper.assertTrue(java.util.Arrays.equals(first, range), tier + " on " + name + " (Lv " + spec.level() + ") pays "
						+ range[0] + "-" + range[1] + ", the same as on Millbrook: " + first[0] + "-" + first[1]);
				// Same seed, same payout, whatever the island's level.
				long a = TreasureService.rollReward(TreasureService.seaOf(island, BlockPos.ZERO), tier, net.minecraft.util.RandomSource.create(42));
				long b = TreasureService.rollReward(1, tier, net.minecraft.util.RandomSource.create(42));
				helper.assertTrue(a == b, tier + " on " + name + ": " + a + " vs " + b);
			}
		}
		// Rolls stay inside the range and reach both ends' neighbourhoods; rarer chests and later seas pay more.
		var random = net.minecraft.util.RandomSource.create(7);
		for (int sea = 1; sea <= 3; sea++) {
			long previousMax = -1;
			for (TreasureTier tier : TreasureTier.values()) {
				long[] r = TreasureService.rewardRange(sea, tier);
				long lo = Long.MAX_VALUE, hi = Long.MIN_VALUE;
				for (int i = 0; i < 2000; i++) {
					long v = TreasureService.rollReward(sea, tier, random);
					lo = Math.min(lo, v);
					hi = Math.max(hi, v);
				}
				helper.assertTrue(lo >= r[0] && hi <= r[1], "sea " + sea + " " + tier + " rolls stay in " + r[0] + "-" + r[1] + ": " + lo + "-" + hi);
				helper.assertTrue(lo - r[0] <= (r[1] - r[0]) / 50 && r[1] - hi <= (r[1] - r[0]) / 50, "the whole range is used: " + lo + "-" + hi);
				helper.assertTrue(r[0] > previousMax, "sea " + sea + ": " + tier + " pays more than the rarity below it");
				previousMax = r[1];
				if (sea > 1) {
					helper.assertTrue(r[0] > TreasureService.rewardRange(sea - 1, tier)[0], tier + " pays more in sea " + sea);
				}
			}
		}
		helper.assertTrue(TreasureService.rewardRange(4, TreasureTier.COMMON)[0] == TreasureService.rewardRange(3, TreasureTier.COMMON)[0],
				"a sea past the table uses the last sea's rewards");
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
