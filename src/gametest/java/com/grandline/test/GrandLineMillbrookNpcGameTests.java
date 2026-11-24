package com.grandline.test;

import com.grandline.data.ModAttachments;
import com.grandline.entity.GrandLineEnemy;
import com.grandline.entity.GrandLineEntities;
import com.grandline.entity.Islander;
import com.grandline.npc.RosterNpcs;
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
		helper.assertTrue(RosterNpcs.manages(island), "the rebuilt Millbrook is managed");
		List<com.grandline.world.plan.IslandRoster.Spot> spots = MillbrookRoster.BANDITS;
		helper.assertTrue(spots.size() == 10, "ten Bandits");
		helper.assertTrue(spots.stream().filter(s -> s.group() == 0).count() == 5 && spots.stream().filter(s -> s.group() == 1).count() == 5, "five per field");
		for (int i = 0; i < spots.size(); i++) {
			for (int j = i + 1; j < spots.size(); j++) {
				double d = Math.hypot(spots.get(i).x() - spots.get(j).x(), spots.get(i).z() - spots.get(j).z());
				helper.assertTrue(d >= 4, "spots " + i + " and " + j + " are spaced out");
				if (spots.get(i).group() != spots.get(j).group()) {
					helper.assertTrue(d >= 15, "the two fields are separate areas");
				}
			}
		}
		Map<String, Integer> roles = new HashMap<>();
		for (com.grandline.world.plan.IslandRoster.Slot slot : MillbrookRoster.slots(island)) {
			roles.merge(slot.role(), 1, Integer::sum);
			// The quest giver is out of every Bandit's reach: aggression radius plus its protected area.
			if (slot.role().equals("QUEST_GIVER")) {
				for (com.grandline.world.plan.IslandRoster.Spot s : spots) {
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
				bandit.addTag(RosterNpcs.SPOT_TAG + (i < 3 ? i : 5 + i)); // both fields count
				helper.assertTrue(RosterNpcs.spotOf(bandit) >= 0, "the Bandit knows its spot");
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

	@GameTest(template = EMPTY_STRUCTURE, batch = "ironhold_roster", timeoutTicks = 20)
	public void ironholdHasSevenTraineesAndEightFriendlies(GameTestHelper helper) {
		IslandRegistry registry = IslandRegistry.get(helper.getLevel().getServer());
		SeaLayout.Spec spec = SeaLayout.all().stream().filter(sp -> sp.name().equals("Ironhold Bastion")).findFirst().orElseThrow();
		long id = SeaLayout.idOf(spec);
		if (registry.get(id) == null) {
			registry.add(new Island(id, spec.x(), spec.z(), spec.radius(), spec.level(), spec.name(), false, true, spec.theme().name()));
		}
		Island island = registry.get(id);
		int before = registry.layout();
		try {
			setLayout(registry, 19);
			helper.assertTrue(com.grandline.world.plan.IslandRoster.of(island) == null, "a world on the original Ironhold keeps its old NPCs");
			setLayout(registry, IslandRegistry.CURRENT_LAYOUT);
			ironhold(helper, island);
		} finally {
			setLayout(registry, before);
		}
		helper.succeed();
	}

	private static void setLayout(IslandRegistry registry, int layout) {
		try {
			var field = IslandRegistry.class.getDeclaredField("layout");
			field.setAccessible(true);
			field.setInt(registry, layout);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(e);
		}
	}

	private static void ironhold(GameTestHelper helper, Island island) {
		var roster = com.grandline.world.plan.IslandRoster.of(island);
		helper.assertTrue(roster != null && roster.kind() == com.grandline.entity.EnemyKind.MARINE_TRAINEE, "Ironhold is managed, with Marine Trainees");
		helper.assertTrue(roster.spots().size() == 7 && roster.minLevel() == 1 && roster.maxLevel() == 2, "seven Trainees, Lv 1-2");
		var slots = roster.slots().apply(island);
		helper.assertTrue(slots.size() == 8, "eight friendly NPCs");
		var leader = slots.stream().filter(sl -> sl.role().equals("QUEST_GIVER")).findFirst().orElseThrow();
		for (var s : roster.spots()) {
			helper.assertTrue(Math.hypot(s.x() - leader.x(), s.z() - leader.z()) > 11, "the Marine Leader stands clear of the field");
			for (var t : roster.spots()) {
				helper.assertTrue(s == t || Math.hypot(s.x() - t.x(), s.z() - t.z()) >= 4, "spaced out");
			}
		}
		Islander npc = GrandLineEntities.ISLANDER.create(helper.getLevel());
		npc.setup(Islander.Role.QUEST_GIVER, island.id(), island.level());
		QuestState offer = QuestService.offer(island, npc, false);
		Island mb = millbrook(helper);
		Islander mbNpc = GrandLineEntities.ISLANDER.create(helper.getLevel());
		mbNpc.setup(Islander.Role.QUEST_GIVER, mb.id(), mb.level());
		QuestState mbOffer = QuestService.offer(mb, mbNpc, false);
		helper.assertTrue(offer.required() == 5 && offer.target().equals("MARINE_TRAINEE"), "defeat 5 Marine Trainees");
		helper.assertTrue(offer.rewardExperience() == mbOffer.rewardExperience() && offer.rewardCurrency() == mbOffer.rewardCurrency(),
				"the same rewards as Millbrook's starting quest");
		npc.discard();
		mbNpc.discard();
	}
}
