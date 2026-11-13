package com.grandline.test;

import com.grandline.data.ModAttachments;
import com.grandline.player.loadout.LoadoutOrder;
import com.grandline.player.loadout.LoadoutOrderService;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/** Loadout order: default, validation, saving with the player (relog/restart) and keeping it through death. */
public final class GrandLineLoadoutGameTests implements FabricGameTest {
	private static final List<Integer> FRUIT_GUN_SWAPPED = List.of(0, 3, 2, 1);

	@GameTest(template = EMPTY_STRUCTURE, batch = "loadout_order")
	public void orderDefaultsValidatesAndPersists(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		helper.assertTrue(LoadoutOrderService.get(player).equals(LoadoutOrder.DEFAULT), "new players get Style, Fruit, Sword, Gun");
		helper.assertTrue(!LoadoutOrderService.set(player, List.of(0, 0, 1, 2)), "a duplicate entry is refused");
		helper.assertTrue(LoadoutOrderService.get(player).equals(LoadoutOrder.DEFAULT), "a refused order changes nothing");
		helper.assertTrue(LoadoutOrderService.set(player, FRUIT_GUN_SWAPPED), "a permutation is accepted");

		// Relog / restart: the order is written into the player data and read back.
		CompoundTag saved = new CompoundTag();
		player.saveWithoutId(saved);
		ServerPlayer reloaded = helper.makeMockServerPlayerInLevel(); // a fresh player, as after a relog
		reloaded.load(saved);
		helper.assertTrue(FRUIT_GUN_SWAPPED.equals(reloaded.getAttached(ModAttachments.LOADOUT_ORDER)), "order survives save and load");

		// Death: the respawned player keeps the order.
		ServerPlayer respawned = helper.makeMockServerPlayerInLevel();
		respawned.restoreFrom(player, false);
		helper.assertTrue(FRUIT_GUN_SWAPPED.equals(LoadoutOrderService.get(respawned)), "order survives death");
		// Mock players are not cleaned up by the framework; leftovers would be hit by later tests' moves.
		for (ServerPlayer p : List.of(player, reloaded, respawned)) {
			helper.getLevel().removePlayerImmediately(p, net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
		}
		helper.succeed();
	}
}
