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
		List<com.grandline.world.plan.IslandRoster.Spot> spots = MillbrookRoster.ROSTER.spots();
		helper.assertTrue(spots.size() == 10, "ten Bandits");
		for (int i = 0; i < spots.size(); i++) {
			for (int j = i + 1; j < spots.size(); j++) {
				double d = Math.hypot(spots.get(i).x() - spots.get(j).x(), spots.get(i).z() - spots.get(j).z());
				helper.assertTrue(d >= 4, "spots " + i + " and " + j + " are spaced out");
				if ((i < 5) != (j < 5)) { // spots 0-4 the meadow, 5-9 the camp
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
				bandit.setOrigin(helper.absolutePos(new net.minecraft.core.BlockPos(1, 2, 1)), island.id()); // one of Millbrook's own
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
		var trainees = roster == null ? null : roster.group(com.grandline.entity.EnemyKind.MARINE_TRAINEE);
		helper.assertTrue(trainees != null, "Ironhold is managed, with Marine Trainees");
		helper.assertTrue(roster.spots().size() == 7 && trainees.minLevel() == 1 && trainees.maxLevel() == 2, "seven Trainees, Lv 1-2");
		var slots = roster.slots().apply(island);
		helper.assertTrue(slots.size() == 8, "eight friendly NPCs");
		var leader = slots.stream().filter(sl -> sl.role().equals("QUEST_GIVER")).findFirst().orElseThrow();
		var all = roster.spots();
		for (var s : all) {
			helper.assertTrue(Math.hypot(s.x() - leader.x(), s.z() - leader.z()) > 11, "the Marine Leader stands clear of the field");
			for (var t : all) {
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

	private static Island island(GameTestHelper helper, String name) {
		IslandRegistry registry = IslandRegistry.get(helper.getLevel().getServer());
		SeaLayout.Spec spec = SeaLayout.all().stream().filter(sp -> sp.name().equals(name)).findFirst().orElseThrow();
		long id = SeaLayout.idOf(spec);
		if (registry.get(id) == null) {
			registry.add(new Island(id, spec.x(), spec.z(), spec.radius(), spec.level(), spec.name(), false, true, spec.theme().name()));
		}
		return registry.get(id);
	}

	/** Counts per group (kind/skin), friendly roles, quests and leashes of Verdant Wilds, Saltmarket Docks and Sunscorch Dunes. */
	@GameTest(template = EMPTY_STRUCTURE, batch = "island_rosters", timeoutTicks = 20)
	public void verdantSaltmarketSunscorchRosters(GameTestHelper helper) {
		IslandRegistry registry = IslandRegistry.get(helper.getLevel().getServer());
		int before = registry.layout();
		try {
			setLayout(registry, IslandRegistry.CURRENT_LAYOUT);
			check(helper, "Verdant Wilds", new int[]{7, 4, 1}, new String[]{"canopy_primate", "ironback", "crownback"}, 2, new int[]{6, 8, 1});
			check(helper, "Saltmarket Docks", new int[]{6, 6, 1}, new String[]{"redcoat_raider", "dock_enforcer", "galley_master"}, 7, new int[]{8, 8, 1});
			check(helper, "Sunscorch Dunes", new int[]{4, 4}, new String[]{"dune_prowler", "sandwatch_captain"}, 5, new int[]{8, 6});
			check(helper, "Frostpeak Isle", new int[]{6, 4, 1}, new String[]{"frost_rover", "rime_construct", "glacier_maw"}, 5, new int[]{7, 8, 1});
			var salt = com.grandline.world.plan.IslandRoster.of(island(helper, "Saltmarket Docks"));
			helper.assertTrue(salt.groups().get(2).respawnTicks() == 300 * 20 && salt.groups().get(0).respawnTicks() == 15 * 20, "Saltmarket respawn times");
			helper.assertTrue(salt.quests().get(0).minLevel() == 30 && salt.quests().get(1).minLevel() == 40 && salt.quests().get(2).minLevel() == 55, "Saltmarket quest levels");
			// Kills count only on the quest's own island: another island's Raider is not an Ironback.
			Island verdant = island(helper, "Verdant Wilds");
			Islander giver = GrandLineEntities.ISLANDER.create(helper.getLevel());
			giver.setup(Islander.Role.QUEST_GIVER, verdant.id(), verdant.level());
			ServerPlayer p = GrandLineBatch3GameTests.survivor(helper);
			try {
				QuestState ironbacks = QuestService.rosterOffers(verdant, giver).get(1);
				p.setAttached(ModAttachments.ACTIVE_QUEST, ironbacks);
				GrandLineEnemy elsewhere = GrandLineEntities.RAIDER.create(helper.getLevel());
				elsewhere.setOrigin(helper.absolutePos(new net.minecraft.core.BlockPos(1, 2, 1)), island(helper, "Saltmarket Docks").id());
				QuestService.onKill(p, elsewhere);
				helper.assertTrue(QuestService.active(p).progress() == 0, "a Saltmarket Raider does not count for Ironbacks");
				GrandLineEnemy here = GrandLineEntities.RAIDER.create(helper.getLevel());
				here.setOrigin(helper.absolutePos(new net.minecraft.core.BlockPos(1, 2, 1)), verdant.id());
				QuestService.onKill(p, here);
				helper.assertTrue(QuestService.active(p).progress() == 1, "an Ironback does");
				helper.assertTrue(ironbacks.rewardExperience() > QuestService.rosterOffers(verdant, giver).get(0).rewardExperience(),
						"eight Ironbacks pay more than six Canopy Primates");
				elsewhere.discard();
				here.discard();
			} finally {
				GrandLineBatch3GameTests.remove(helper, p, giver);
			}
		} finally {
			setLayout(registry, before);
		}
		helper.succeed();
	}

	private static void check(GameTestHelper helper, String name, int[] counts, String[] skins, int friendlies, int[] questCounts) {
		Island island = island(helper, name);
		var roster = com.grandline.world.plan.IslandRoster.of(island);
		helper.assertTrue(roster != null, name + " is managed");
		helper.assertTrue(roster.groups().size() == counts.length, name + ": groups");
		for (int g = 0; g < counts.length; g++) {
			helper.assertTrue(roster.groups().get(g).spots().size() == counts[g] && roster.groups().get(g).skin().equals(skins[g]),
					name + ": " + counts[g] + " " + skins[g]);
		}
		var spots = roster.spots();
		for (var a : spots) {
			for (var b : spots) {
				helper.assertTrue(a == b || Math.hypot(a.x() - b.x(), a.z() - b.z()) >= 4, name + ": spots spaced out");
			}
		}
		var slots = roster.slots().apply(island);
		helper.assertTrue(slots.size() == friendlies, name + ": " + friendlies + " friendly NPCs, not " + slots.size());
		for (var slot : slots) {
			for (var s : spots) {
				helper.assertTrue(Math.hypot(s.x() - slot.x(), s.z() - slot.z()) > 11, name + ": " + slot.role() + " stands clear of every enemy spot");
			}
		}
		helper.assertTrue(roster.quests().size() == questCounts.length, name + ": quests");
		for (int q = 0; q < questCounts.length; q++) {
			helper.assertTrue(roster.quests().get(q).count() == questCounts[q], name + ": quest " + q);
		}
	}
}
