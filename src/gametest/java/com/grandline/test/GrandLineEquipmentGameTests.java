package com.grandline.test;

import com.grandline.gear.EquipmentOwnership;
import com.grandline.gear.Gear;
import com.grandline.npc.SwordShop;
import com.grandline.player.PlayerProfileService;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Permanent equipment ownership (one copy each, any source) and no durability. */
public final class GrandLineEquipmentGameTests implements FabricGameTest {
	private static ServerPlayer player(GameTestHelper helper) {
		ServerPlayer p = helper.makeMockServerPlayerInLevel();
		p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL); // creative keeps creative-menu equipment
		GrandLineRaceGameTests.neutral(p);
		return p;
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "equipment")
	public void grantsOnceAndShopsRefuseOwnedItems(GameTestHelper helper) {
		ServerPlayer p = player(helper);
		helper.assertTrue(EquipmentOwnership.grant(p, Gear.KATANA) == EquipmentOwnership.Grant.GRANTED, "first grant gives it");
		helper.assertTrue(EquipmentOwnership.grant(p, Gear.KATANA) == EquipmentOwnership.Grant.ALREADY_OWNED, "second grant gives nothing");
		helper.assertTrue(EquipmentOwnership.grant(p, Items.APPLE) == EquipmentOwnership.Grant.NOT_EQUIPMENT, "only equipment is owned");
		helper.assertTrue(p.getInventory().countItem(Gear.KATANA) == 1, "one katana held");
		PlayerProfileService.update(p, pr -> pr.withCurrency(1_000_000));
		int katana = SwordShop.STOCK.indexOf(Gear.KATANA);
		helper.assertTrue(SwordShop.buy(p, katana).equals("already_owned"), "the shop refuses an owned sword");
		helper.assertTrue(PlayerProfileService.get(p).currency() == 1_000_000, "no money taken");
		int cutlass = SwordShop.STOCK.indexOf(Gear.CUTLASS);
		helper.assertTrue(SwordShop.buy(p, cutlass).equals("sword_bought") && EquipmentOwnership.owns(p, Gear.CUTLASS), "a new sword is bought");
		helper.assertTrue(SwordShop.buy(p, cutlass).equals("already_owned"), "and only once");
		helper.assertTrue(p.getInventory().countItem(Gear.CUTLASS) == 1, "one cutlass held");
		helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "equipment")
	public void migrationMergesDuplicatesAndReconcileHoldsExactlyOne(GameTestHelper helper) {
		ServerPlayer p = player(helper);
		// An old save: two katanas (one damaged) and a damaged fishing rod, no ownership record yet.
		p.setAttached(com.grandline.data.ModAttachments.OWNED_EQUIPMENT, com.grandline.gear.OwnedEquipment.EMPTY);
		ItemStack worn = new ItemStack(Gear.KATANA);
		worn.setDamageValue(40);
		p.getInventory().setItem(0, new ItemStack(Gear.KATANA));
		p.getInventory().setItem(10, worn);
		ItemStack rod = new ItemStack(Items.FISHING_ROD);
		rod.setDamageValue(30);
		p.getInventory().setItem(11, rod);
		EquipmentOwnership.reconcile(p);
		helper.assertTrue(EquipmentOwnership.owns(p, Gear.KATANA), "held equipment becomes owned");
		helper.assertTrue(p.getInventory().countItem(Gear.KATANA) == 1, "duplicates merged to one");
		helper.assertTrue(p.getInventory().getItem(0).is(Gear.KATANA), "the hotbar copy is the one kept");
		helper.assertTrue(p.getInventory().getItem(11).getDamageValue() == 0, "old damage mended");
		EquipmentOwnership.reconcile(p);
		helper.assertTrue(p.getInventory().countItem(Gear.KATANA) == 1, "running again changes nothing (idempotent)");
		// After migration: an item the player does not own is taken away; an owned item that went missing comes back.
		p.getInventory().setItem(5, new ItemStack(Gear.TRIDENT));
		p.getInventory().setItem(0, ItemStack.EMPTY);
		EquipmentOwnership.reconcile(p);
		helper.assertTrue(p.getInventory().countItem(Gear.TRIDENT) == 0, "unowned equipment removed");
		helper.assertTrue(p.getInventory().countItem(Gear.KATANA) == 1, "the owned katana comes back");
		helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "equipment")
	public void nothingTakesDurabilityDamage(GameTestHelper helper) {
		ServerPlayer p = player(helper);
		ItemStack sword = new ItemStack(Gear.KATANA);
		ItemStack rod = new ItemStack(Items.FISHING_ROD);
		sword.hurtAndBreak(500, p, e -> { });
		rod.hurtAndBreak(500, p, e -> { });
		helper.assertTrue(!sword.isEmpty() && sword.getDamageValue() == 0 && !sword.isDamaged() && !sword.isBarVisible(), "swords never wear");
		helper.assertTrue(!rod.isEmpty() && rod.getDamageValue() == 0 && !rod.isBarVisible(), "rods never wear");
		helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
		helper.succeed();
	}
}
