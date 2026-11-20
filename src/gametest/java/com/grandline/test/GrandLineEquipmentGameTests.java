package com.grandline.test;

import com.grandline.gear.EquipmentOwnership;
import com.grandline.gear.Gear;
import com.grandline.gear.OwnedEquipment;
import com.grandline.gear.WeaponCategory;
import com.grandline.data.ModAttachments;
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
		// A sword is already equipped: the new one waits in the Backpack (owned, not held).
		helper.assertTrue(p.getInventory().countItem(Gear.CUTLASS) == 0 && p.getInventory().countItem(Gear.KATANA) == 1,
				"the bought cutlass goes to the Backpack; the katana stays equipped");
		helper.assertTrue(EquipmentOwnership.equipped(p, WeaponCategory.SWORD) == Gear.KATANA, "katana still the equipped sword");
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

	// ------------------------------------------------------------------------------------------------ Backpack equipment

	private static void remove(GameTestHelper helper, ServerPlayer p) {
		helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "equipment")
	public void equippingASwordReplacesTheOldOneInItsSlot(GameTestHelper helper) {
		ServerPlayer p = player(helper);
		p.getInventory().clearContent();
		EquipmentOwnership.grant(p, Gear.ECLIPSE_BLADE);
		EquipmentOwnership.grant(p, Gear.FLINTLOCK);
		int slot = p.getInventory().findSlotMatchingItem(new ItemStack(Gear.ECLIPSE_BLADE));
		helper.assertTrue(slot >= 0 && slot < 4, "the first sword is equipped in a loadout slot: " + slot);
		// The player rearranged the hotbar: the sword now sits in slot 3 and is in hand.
		ItemStack blade = p.getInventory().removeItemNoUpdate(slot);
		p.getInventory().setItem(2, blade);
		p.getInventory().selected = 2;
		var blades = Gear.powerFor(Gear.ECLIPSE_BLADE);
		var katanas = Gear.powerFor(Gear.KATANA);
		PlayerProfileService.update(p, pr -> pr.withPowers(pr.powers().withMasteryXp(blades.id().toString(), 12_345)
				.withMasteryXp(katanas.id().toString(), 678)));
		var cooled = blades.abilityId(blades.abilities().get(0));
		com.grandline.combat.ability.AbilityCooldownService.start(p, cooled, 400);
		helper.assertTrue(EquipmentOwnership.grant(p, Gear.KATANA) == EquipmentOwnership.Grant.STORED, "a second sword is stored, not equipped");
		helper.assertTrue(p.getInventory().countItem(Gear.KATANA) == 0, "a stored sword is not held");

		helper.assertTrue(EquipmentOwnership.setEquipped(p, Gear.KATANA, true) == EquipmentOwnership.Change.EQUIPPED, "equip the katana");
		helper.assertTrue(p.getInventory().getItem(2).is(Gear.KATANA), "the katana takes the old sword's slot");
		helper.assertTrue(p.getInventory().selected == 2, "and is in hand, as the old one was");
		helper.assertTrue(p.getInventory().countItem(Gear.ECLIPSE_BLADE) == 0, "the Eclipse Blade went back to the Backpack");
		helper.assertTrue(EquipmentOwnership.owns(p, Gear.ECLIPSE_BLADE), "still owned");
		helper.assertTrue(EquipmentOwnership.equipped(p, WeaponCategory.SWORD) == Gear.KATANA, "the katana is the equipped sword");
		helper.assertTrue(p.getInventory().countItem(Gear.FLINTLOCK) == 1, "the gun is untouched");
		int swords = 0;
		for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
			if (p.getInventory().getItem(i).getItem() instanceof com.grandline.gear.GearSwordItem) {
				swords++;
			}
		}
		helper.assertTrue(swords == 1, "never two swords: " + swords);
		helper.assertTrue(EquipmentOwnership.setEquipped(p, Gear.KATANA, true) == EquipmentOwnership.Change.ALREADY, "equipping again does nothing");
		EquipmentOwnership.reconcile(p);
		helper.assertTrue(p.getInventory().getItem(2).is(Gear.KATANA) && p.getInventory().countItem(Gear.ECLIPSE_BLADE) == 0,
				"the reconciler agrees");
		// Back again: mastery and cooldowns belong to the weapon and are untouched.
		helper.assertTrue(EquipmentOwnership.setEquipped(p, Gear.ECLIPSE_BLADE, true) == EquipmentOwnership.Change.EQUIPPED, "swap back");
		helper.assertTrue(p.getInventory().getItem(2).is(Gear.ECLIPSE_BLADE), "same slot again");
		var powers = PlayerProfileService.get(p).powers();
		helper.assertTrue(powers.masteryXp(blades.id().toString()) == 12_345 && powers.masteryXp(katanas.id().toString()) == 678,
				"both weapons keep their mastery");
		helper.assertTrue(com.grandline.combat.ability.AbilityCooldownService.isOnCooldown(p, cooled), "switching does not reset a cooldown");
		remove(helper, p);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "equipment")
	public void unequippingKeepsTheWeaponOwned(GameTestHelper helper) {
		ServerPlayer p = player(helper);
		p.getInventory().clearContent();
		EquipmentOwnership.grant(p, Gear.MUSKET);
		helper.assertTrue(EquipmentOwnership.setEquipped(p, Gear.MUSKET, false) == EquipmentOwnership.Change.UNEQUIPPED, "unequip");
		helper.assertTrue(p.getInventory().countItem(Gear.MUSKET) == 0, "out of the hotbar");
		helper.assertTrue(EquipmentOwnership.owns(p, Gear.MUSKET) && !EquipmentOwnership.isEquipped(p, Gear.MUSKET), "owned, not equipped");
		EquipmentOwnership.reconcile(p);
		helper.assertTrue(p.getInventory().countItem(Gear.MUSKET) == 0, "the reconciler does not bring it back");
		helper.assertTrue(EquipmentOwnership.setEquipped(p, Gear.MUSKET, false) == EquipmentOwnership.Change.NOT_EQUIPPED, "unequip twice: nothing");
		helper.assertTrue(EquipmentOwnership.setEquipped(p, Gear.CANNON, true) == EquipmentOwnership.Change.NOT_OWNED, "cannot equip what is not owned");
		helper.assertTrue(EquipmentOwnership.setEquipped(p, Items.DIAMOND_SWORD, true) == EquipmentOwnership.Change.NOT_WEAPON, "only Grand Line weapons");
		helper.assertTrue(p.getInventory().countItem(Gear.CANNON) == 0, "and nothing was given");
		helper.assertTrue(EquipmentOwnership.setEquipped(p, Gear.MUSKET, true) == EquipmentOwnership.Change.EQUIPPED, "equip it again");
		helper.assertTrue(p.getInventory().countItem(Gear.MUSKET) == 1, "back in the hotbar");
		remove(helper, p);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "equipment")
	public void storedAndExtraCopiesAreNeverHeld(GameTestHelper helper) {
		ServerPlayer p = player(helper);
		p.getInventory().clearContent();
		EquipmentOwnership.grant(p, Gear.KATANA);
		EquipmentOwnership.grant(p, Gear.TRIDENT); // stored
		// Copies made by inventory tricks: extra katanas, the stored trident, in the inventory, the cursor and the grid.
		p.getInventory().setItem(20, new ItemStack(Gear.KATANA));
		p.getInventory().setItem(7, new ItemStack(Gear.TRIDENT));
		p.containerMenu.setCarried(new ItemStack(Gear.KATANA));
		p.inventoryMenu.getCraftSlots().setItem(0, new ItemStack(Gear.TRIDENT));
		EquipmentOwnership.reconcile(p);
		helper.assertTrue(p.getInventory().countItem(Gear.KATANA) == 1 && p.containerMenu.getCarried().isEmpty(), "one katana");
		helper.assertTrue(p.getInventory().countItem(Gear.TRIDENT) == 0 && p.inventoryMenu.getCraftSlots().getItem(0).isEmpty(),
				"a stored sword is never held");
		helper.assertTrue(EquipmentOwnership.owns(p, Gear.TRIDENT), "and still owned");
		// Rapid requests: the server takes one per interval; repeats are harmless anyway.
		com.grandline.network.ServerRequestGuard.forget(p.getUUID());
		var first = com.grandline.network.GrandLineNetworking.handleEquipWeapon(p,
				new com.grandline.network.payload.EquipWeaponPayload("grandline:trident", true));
		var second = com.grandline.network.GrandLineNetworking.handleEquipWeapon(p,
				new com.grandline.network.payload.EquipWeaponPayload("grandline:trident", true));
		helper.assertTrue(first == EquipmentOwnership.Change.EQUIPPED, "the first request is done: " + first);
		helper.assertTrue(second == null || second == EquipmentOwnership.Change.ALREADY, "the spam is ignored: " + second);
		helper.assertTrue(p.getInventory().countItem(Gear.TRIDENT) == 1 && p.getInventory().countItem(Gear.KATANA) == 0, "one sword");
		remove(helper, p);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "equipment")
	public void oldSavesKeepEveryWeaponAndEquipTheOneInHand(GameTestHelper helper) {
		ServerPlayer p = player(helper);
		p.getInventory().clearContent();
		// A save from before the Backpack: two swords and a gun owned and held, a rod (not yet owned equipment) in the bar.
		p.setAttached(ModAttachments.OWNED_EQUIPMENT, new OwnedEquipment(java.util.Set.of("grandline:katana", "grandline:cutlass",
				"grandline:slingshot"), true));
		p.getInventory().setItem(1, new ItemStack(Gear.KATANA));
		p.getInventory().setItem(0, new ItemStack(Gear.CUTLASS));
		p.getInventory().setItem(15, new ItemStack(Gear.SLINGSHOT));
		p.getInventory().setItem(6, new ItemStack(com.grandline.registry.ModItems.STARTER_FISHING_ROD));
		p.getInventory().selected = 1;
		EquipmentOwnership.reconcile(p);
		helper.assertTrue(EquipmentOwnership.equipped(p, WeaponCategory.SWORD) == Gear.KATANA, "the sword in hand is the equipped one");
		helper.assertTrue(EquipmentOwnership.equipped(p, WeaponCategory.GUN) == Gear.SLINGSHOT, "the only gun is equipped");
		helper.assertTrue(EquipmentOwnership.equipped(p, WeaponCategory.ROD) == com.grandline.registry.ModItems.STARTER_FISHING_ROD,
				"the held rod becomes owned and equipped");
		helper.assertTrue(EquipmentOwnership.owns(p, Gear.CUTLASS) && p.getInventory().countItem(Gear.CUTLASS) == 0,
				"the other sword is kept, in the Backpack");
		helper.assertTrue(p.getInventory().getItem(1).is(Gear.KATANA), "the equipped sword stays where it was");
		helper.assertTrue(p.getInventory().countItem(com.grandline.registry.ModItems.STARTER_FISHING_ROD) == 1, "one rod");
		helper.assertTrue(com.grandline.npc.NpcService.buyRod(p).equals("rod_owned"), "an owned rod is not sold again");
		helper.assertTrue(EquipmentOwnership.record(p).equipVersion() == OwnedEquipment.EQUIP_VERSION, "migrated once");
		remove(helper, p);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "equipment")
	public void equipmentRecordSavesAndLoads(GameTestHelper helper) {
		OwnedEquipment own = new OwnedEquipment(java.util.Set.of("grandline:katana", "grandline:musket", "grandline:bisento"), true)
				.withEquipped("sword", "grandline:katana").withEquipped("gun", "grandline:musket").withEquipVersion(OwnedEquipment.EQUIP_VERSION);
		var tag = OwnedEquipment.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, own).getOrThrow(false, e -> { });
		var back = OwnedEquipment.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE, tag).getOrThrow(false, e -> { });
		helper.assertTrue(back.equals(own), "the record round-trips: " + back);
		// An old record (ids only) loads with nothing equipped and the Backpack migration still to do.
		var old = new net.minecraft.nbt.CompoundTag();
		var ids = new net.minecraft.nbt.ListTag();
		ids.add(net.minecraft.nbt.StringTag.valueOf("grandline:katana"));
		old.put("ids", ids);
		old.putBoolean("migrated", true);
		var loaded = OwnedEquipment.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE, old).getOrThrow(false, e -> { });
		helper.assertTrue(loaded.owns("grandline:katana") && loaded.equipped().isEmpty() && loaded.equipVersion() == 0, "old saves load");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "equipment_death", timeoutTicks = 40)
	public void deathKeepsEquippedAndStoredWeapons(GameTestHelper helper) {
		ServerPlayer p = GrandLineBatch3GameTests.survivor(helper);
		p.getInventory().clearContent();
		EquipmentOwnership.grant(p, Gear.KATANA);
		EquipmentOwnership.grant(p, Gear.BISENTO); // stored
		EquipmentOwnership.setEquipped(p, Gear.BISENTO, true);
		int slot = p.getInventory().findSlotMatchingItem(new ItemStack(Gear.BISENTO));
		p.kill();
		helper.runAfterDelay(2, () -> {
			ServerPlayer back = helper.getLevel().getServer().getPlayerList().respawn(p, false);
			EquipmentOwnership.reconcile(back);
			helper.assertTrue(EquipmentOwnership.owns(back, Gear.KATANA) && EquipmentOwnership.owns(back, Gear.BISENTO), "both still owned");
			helper.assertTrue(EquipmentOwnership.equipped(back, WeaponCategory.SWORD) == Gear.BISENTO, "the equipped one stays equipped");
			helper.assertTrue(back.getInventory().countItem(Gear.BISENTO) == 1 && back.getInventory().getItem(slot).is(Gear.BISENTO),
					"held once, in its slot");
			helper.assertTrue(back.getInventory().countItem(Gear.KATANA) == 0, "the stored one is still stored");
			GrandLineBatch3GameTests.remove(helper, back);
			helper.succeed();
		});
	}
}
