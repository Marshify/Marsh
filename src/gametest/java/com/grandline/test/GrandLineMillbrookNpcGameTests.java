package com.grandline.test;

import com.grandline.data.ModAttachments;
import com.grandline.entity.GrandLineEnemy;
import com.grandline.entity.GrandLineEntities;
import com.grandline.entity.Islander;
import com.grandline.npc.MillbrookNpcs;
import com.grandline.npc.NpcService;
import com.grandline.npc.QuestService;
import com.grandline.npc.QuestState;
import com.grandline.player.PlayerProfileService;
import com.grandline.ship.ShipClass;
import com.grandline.world.Island;
import com.grandline.world.IslandRegistry;
import com.grandline.world.SeaLayout;
import com.grandline.world.plan.MillbrookRoster;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Millbrook's NPC roster: ten Bandits in two fields, seven friendly NPCs, and the 5-Bandit quest. */
public class GrandLineMillbrookNpcGameTests implements FabricGameTest {
	private static Island millbrook(GameTestHelper helper) {
		IslandRegistry registry = IslandRegistry.get(helper.getLevel().getServer());
		SeaLayout.Spec spec = SeaLayout.all().stream().filter(sp -> sp.name().equals("Millbrook Village")).findFirst().orElseThrow();
		long id = SeaLayout.idOf(spec);
		if (registry.get(id) == null) {
			registry.add(new Island(id, spec.x(), spec.z(), spec.radius(), spec.level(), spec.name(), false, true, spec.theme().name()));
		}
		return registry.get(id);
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "millbrook_roster", timeoutTicks = 20)
	public void rosterHasTwoFieldsOfFiveAndSevenFriendlies(GameTestHelper helper) {
		Island island = millbrook(helper);
		helper.assertTrue(MillbrookNpcs.manages(island), "the rebuilt Millbrook is managed");
		List<MillbrookRoster.Spot> spots = MillbrookRoster.BANDITS;
		helper.assertTrue(spots.size() == 10, "ten Bandits");
		helper.assertTrue(spots.stream().filter(s -> s.field() == 0).count() == 5 && spots.stream().filter(s -> s.field() == 1).count() == 5, "five per field");
		for (int i = 0; i < spots.size(); i++) {
			for (int j = i + 1; j < spots.size(); j++) {
				double d = Math.hypot(spots.get(i).x() - spots.get(j).x(), spots.get(i).z() - spots.get(j).z());
				helper.assertTrue(d >= 4, "spots " + i + " and " + j + " are spaced out");
				if (spots.get(i).field() != spots.get(j).field()) {
					helper.assertTrue(d >= 15, "the two fields are separate areas");
				}
			}
		}
		Map<String, Integer> roles = new HashMap<>();
		for (MillbrookRoster.Slot slot : MillbrookRoster.slots(island)) {
			roles.merge(slot.role(), 1, Integer::sum);
			// The quest giver is out of every Bandit's reach: aggression radius plus its protected area.
			if (slot.role().equals("QUEST_GIVER")) {
				for (MillbrookRoster.Spot s : spots) {
					helper.assertTrue(Math.hypot(s.x() - slot.x(), s.z() - slot.z()) > 11, "the quest giver stands clear of the fields");
				}
			}
		}
		helper.assertTrue(roles.equals(Map.of("QUEST_GIVER", 1, "BOAT_DEALER", 2, "LUXURY_BOAT_DEALER", 1, "MARINE_RECRUITER", 1,
				"INNKEEPER", 1, "SWORDSMITH", 1)), "the friendly roster: " + roles);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "millbrook_quest", timeoutTicks = 20)
	public void banditQuestIsFiveBanditsFromEitherField(GameTestHelper helper) {
		Island island = millbrook(helper);
		Islander giver = GrandLineEntities.ISLANDER.create(helper.getLevel());
		giver.setup(Islander.Role.QUEST_GIVER, island.id(), island.level());
		QuestState offer = QuestService.offer(island, giver, false);
		helper.assertTrue(offer.required() == 5 && offer.target().equals("BANDIT"), "defeat 5 Bandits");
		ServerPlayer p = GrandLineBatch3GameTests.survivor(helper);
		try {
			p.setAttached(ModAttachments.ACTIVE_QUEST, offer);
			long before = PlayerProfileService.get(p).currency();
			for (int i = 0; i < 5; i++) {
				GrandLineEnemy bandit = GrandLineEntities.BANDIT.create(helper.getLevel());
				bandit.addTag(MillbrookNpcs.SPOT_TAG + (i < 3 ? i : 5 + i)); // both fields count
				helper.assertTrue(MillbrookNpcs.spotOf(bandit) >= 0, "the Bandit knows its spot");
				QuestService.onKill(p, bandit);
			}
			helper.assertTrue(QuestService.active(p) == null, "complete after five");
			helper.assertTrue(PlayerProfileService.get(p).currency() == before + offer.rewardCurrency(), "Beli reward paid");
			helper.assertTrue(QuestService.offer(island, giver, false).required() == 5, "and offered again (repeatable)");
		} finally {
			GrandLineBatch3GameTests.remove(helper, p, giver);
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "millbrook_dealers", timeoutTicks = 20)
	public void dealersSellTheirOwnShips(GameTestHelper helper) {
		Islander boat = GrandLineEntities.ISLANDER.create(helper.getLevel());
		boat.setup(Islander.Role.BOAT_DEALER, 0, 1);
		Islander luxury = GrandLineEntities.ISLANDER.create(helper.getLevel());
		luxury.setup(Islander.Role.LUXURY_BOAT_DEALER, 0, 1);
		helper.assertTrue(NpcService.sellsShip(boat, ShipClass.SLOOP) && NpcService.sellsShip(boat, ShipClass.BRIG)
				&& !NpcService.sellsShip(boat, ShipClass.GALLEON), "Boat Dealer: Sloop and Brig");
		helper.assertTrue(NpcService.sellsShip(luxury, ShipClass.GALLEON) && !NpcService.sellsShip(luxury, ShipClass.SLOOP),
				"Luxury Boat Dealer: the Galleon");
		boat.discard();
		luxury.discard();
		helper.succeed();
	}
}
