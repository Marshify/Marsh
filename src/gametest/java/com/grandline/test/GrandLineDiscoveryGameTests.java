package com.grandline.test;

import com.grandline.data.ModAttachments;
import com.grandline.world.Island;
import com.grandline.world.ModWorld;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

import java.util.List;

/** Chart discovery: per player, only by arriving on an island's land, never by sailing past; saved with the player. */
public final class GrandLineDiscoveryGameTests implements FabricGameTest {
	@GameTest(template = EMPTY_STRUCTURE, batch = "discovery")
	public void discoveryIsPerPlayerByArrivalAndPersists(GameTestHelper helper) {
		// A random-style island (no plan): its gameplay area is its radius around the centre.
		Island island = new Island(987654321L, 10_000, 10_000, 20, 5, "Test Isle", false, true, "village");
		ServerPlayer alice = helper.makeMockServerPlayerInLevel();
		ServerPlayer bob = helper.makeMockServerPlayerInLevel();
		alice.moveTo(10_000 + 30, 70, 10_000);                        // just offshore
		helper.assertTrue(!ModWorld.inGameplayArea(alice, island), "near the island but offshore: not arrived");
		var boat = EntityType.BOAT.create(helper.getLevel());
		boat.moveTo(10_000 + 5, 63, 10_000);
		helper.getLevel().addFreshEntity(boat);
		alice.moveTo(10_000 + 5, 64, 10_000);
		alice.startRiding(boat, true);
		helper.assertTrue(!ModWorld.inGameplayArea(alice, island), "in a boat over the island's edge: not arrived");
		alice.stopRiding();
		alice.moveTo(10_000 + 5, 70, 10_000);
		helper.assertTrue(ModWorld.inGameplayArea(alice, island), "standing on the island: arrived");
		ModWorld.discover(alice, island, false);
		helper.assertTrue(ModWorld.discovered(alice).contains(island.id()), "Alice discovered it");
		helper.assertTrue(!ModWorld.discovered(bob).contains(island.id()), "Bob has not");
		// Saved with the player (relog / restart) and kept through death.
		CompoundTag saved = new CompoundTag();
		alice.saveWithoutId(saved);
		ServerPlayer reloaded = helper.makeMockServerPlayerInLevel();
		reloaded.load(saved);
		helper.assertTrue(ModWorld.discovered(reloaded).contains(island.id()), "discovery survives save and load");
		ServerPlayer respawned = helper.makeMockServerPlayerInLevel();
		respawned.restoreFrom(alice, false);
		helper.assertTrue(ModWorld.discovered(respawned).contains(island.id()), "discovery survives death");
		for (ServerPlayer p : List.of(alice, bob, reloaded, respawned)) {
			helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
		}
		boat.discard();
		helper.succeed();
	}
}
