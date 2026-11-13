package com.grandline.test;

import com.grandline.GrandLineMod;
import com.grandline.combat.ability.AbilityContext;
import com.grandline.combat.ability.AbilityDef;
import com.grandline.combat.ability.Power;
import com.grandline.fruit.FruitService;
import com.grandline.fruit.Fruits;
import com.grandline.network.GrandLineNetworking;
import com.grandline.network.payload.FruitActionPayload;
import com.grandline.player.PlayerProfileService;
import com.grandline.player.StatType;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/** Milestone 4 game tests: fruits, eating, the stash, the gacha and every fruit move. */
public final class GrandLineFruitGameTests implements FabricGameTest {
	@GameTest(template = EMPTY_STRUCTURE)
	public void rosterIsCompleteAndConsistent(GameTestHelper helper) {
		eq(helper, Fruits.all().size(), 42, "fruit count");
		Set<String> abilityIds = new HashSet<>();
		for (Fruits.FruitDef fruit : Fruits.all()) {
			eq(helper, fruit.power().abilities().size(), 5, fruit.id() + " move count");
			eq(helper, fruit.power().scalingStat(), StatType.FRUIT, fruit.id() + " scales with Fruit");
			for (AbilityDef ability : fruit.power().abilities()) {
				helper.assertTrue(abilityIds.add(fruit.power().abilityId(ability).toString()), "duplicate move id " + ability.key());
			}
		}
		helper.succeed();
	}

	/** Runs every move of every fruit next to a zombie: none may throw, and damaging moves must be able to hit. */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 400, batch = "fruit_moves")
	public void everyFruitMoveExecutes(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.moveTo(helper.absolutePos(new BlockPos(1, 2, 1)).getCenter()); // own test area, away from world spawn
		int totalHits = 0;
		for (Fruits.FruitDef fruit : Fruits.all()) {
			for (AbilityDef ability : fruit.power().abilities()) {
				Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, 1));
				zombie.setNoAi(true);
				zombie.moveTo(player.getX() + 1.5, player.getY(), player.getZ());
				player.setYRot(-90); // look towards +X
				player.setXRot(0);
				try {
					totalHits += ability.action().execute(new AbilityContext(player, fruit.power(), ability, 1.0));
				} catch (RuntimeException e) {
					helper.fail(fruit.id() + "/" + ability.key() + " threw " + e);
					return;
				}
				zombie.discard();
				com.grandline.combat.ability.FlightService.stop(player, false); // flight moves are toggles
				player.moveTo(helper.absolutePos(new BlockPos(1, 2, 1)).getCenter()); // undo dashes/blinks
				player.setDeltaMovement(0, 0, 0);
			}
		}
		helper.assertTrue(totalHits > 100, "most damaging moves hit an adjacent zombie (hits: " + totalHits + ")");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void eatingReplacesFruitAndConsumesItem(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL); // creative players keep items by design
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Fruits.FLAME.item(), 1));
		eq(helper, FruitService.useHeldFruit(player, true, FruitService.FruitAction.EAT), FruitService.Result.SUCCESS, "eat ember");
		eq(helper, PlayerProfileService.get(player).powers().fruit(), Fruits.FLAME.id(), "fruit set");
		helper.assertTrue(player.getMainHandItem().isEmpty(), "fruit consumed");

		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Fruits.FLAME.item(), 1));
		eq(helper, FruitService.useHeldFruit(player, true, FruitService.FruitAction.EAT), FruitService.Result.ALREADY_HAVE_FRUIT, "same fruit");
		helper.assertFalse(player.getMainHandItem().isEmpty(), "not consumed when refused");

		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Fruits.ICE.item(), 1));
		eq(helper, FruitService.useHeldFruit(player, true, FruitService.FruitAction.EAT), FruitService.Result.SUCCESS, "replace with frost");
		eq(helper, PlayerProfileService.get(player).powers().fruit(), Fruits.ICE.id(), "fruit replaced");

		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		eq(helper, FruitService.useHeldFruit(player, true, FruitService.FruitAction.EAT), FruitService.Result.NOT_HOLDING_FRUIT, "empty hand");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void stashStoresWithdrawsAndRespectsCapacity(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Fruits.SPIN.item(), 1));
		eq(helper, FruitService.useHeldFruit(player, true, FruitService.FruitAction.STORE), FruitService.Result.SUCCESS, "store");
		eq(helper, PlayerProfileService.get(player).powers().stashCount(Fruits.SPIN.id()), 1, "in stash");
		helper.assertTrue(player.getMainHandItem().isEmpty(), "item moved into stash");

		eq(helper, FruitService.withdrawFromStash(player, Fruits.SPIN.id()), FruitService.Result.SUCCESS, "withdraw");
		eq(helper, FruitService.withdrawFromStash(player, Fruits.SPIN.id()), FruitService.Result.NOT_IN_STASH, "nothing left");
		helper.assertTrue(player.getInventory().countItem(Fruits.SPIN.item()) == 1, "withdrawn fruit in inventory");

		for (int i = 0; i < GrandLineMod.config().stashCapacity; i++) {
			FruitService.storeInStash(player, Fruits.SAND.id());
		}
		eq(helper, FruitService.storeInStash(player, Fruits.SAND.id()), FruitService.Result.STASH_FULL, "capacity enforced");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void gachaChargesCurrencyAndHasCooldown(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		eq(helper, FruitService.rollGacha(player).result(), FruitService.Result.LEVEL_TOO_LOW, "level 1 cannot spin");
		PlayerProfileService.setLevel(player, 100);
		eq(helper, FruitService.rollGacha(player).result(), FruitService.Result.NOT_ENOUGH_CURRENCY, "broke");

		long cost = FruitService.gachaCost(player);
		eq(helper, cost, 39_850L, "price at level 100 = 25,000 + 150 x 99");
		PlayerProfileService.addCurrency(player, cost * 2);
		FruitService.GachaRoll roll = FruitService.rollGacha(player);
		eq(helper, roll.result(), FruitService.Result.SUCCESS, "first roll");
		helper.assertTrue(roll.fruit() != null && player.getInventory().countItem(roll.fruit().item()) == 1, "fruit received");
		eq(helper, PlayerProfileService.get(player).currency(), cost, "cost deducted");

		eq(helper, FruitService.rollGacha(player).result(), FruitService.Result.ON_COOLDOWN, "second roll blocked");
		eq(helper, PlayerProfileService.get(player).currency(), cost, "no charge while on cooldown");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void shopRequiresDealerAndStock(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		PlayerProfileService.addCurrency(player, 10_000_000);
		long before = PlayerProfileService.get(player).currency();
		Fruits.FruitDef inStock = FruitService.dealerStock(player.server).get(0);

		// Without a dealer nearby (no such entity id), the network handler refuses.
		eq(helper, GrandLineNetworking.handleFruitAction(player, new FruitActionPayload(FruitActionPayload.SHOP_BUY, inStock.id(), -1)),
				FruitService.Result.NOT_IN_STOCK, "no dealer nearby");
		eq(helper, PlayerProfileService.get(player).currency(), before, "nothing charged");

		// The service itself sells only stocked fruits, at the rarity price.
		eq(helper, FruitService.buy(player, inStock.id()), FruitService.Result.SUCCESS, "buy stocked fruit");
		eq(helper, PlayerProfileService.get(player).currency(), before - FruitService.price(inStock), "price charged");
		Fruits.FruitDef notInStock = Fruits.all().stream().filter(f -> !FruitService.dealerStock(player.server).contains(f)).findFirst().orElseThrow();
		eq(helper, FruitService.buy(player, notInStock.id()), FruitService.Result.NOT_IN_STOCK, "unstocked fruit");
		eq(helper, FruitService.buy(player, "grandline:not_a_fruit"), FruitService.Result.UNKNOWN_FRUIT, "unknown fruit");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void refundCostsCurrency(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		PlayerProfileService.update(player, p -> p.withUnallocatedStatPoints(10));
		PlayerProfileService.allocateStat(player, StatType.MELEE, 10);
		eq(helper, GrandLineNetworking.handleFruitAction(player, new FruitActionPayload(FruitActionPayload.REFUND_STATS, "", 0)),
				FruitService.Result.NOT_ENOUGH_CURRENCY, "refund without money");
		helper.runAfterDelay(GrandLineMod.config().requestCooldownTicks + 1, () -> {
			PlayerProfileService.addCurrency(player, GrandLineMod.config().statRefundCost);
			eq(helper, GrandLineNetworking.handleFruitAction(player, new FruitActionPayload(FruitActionPayload.REFUND_STATS, "", 0)),
					FruitService.Result.SUCCESS, "refund");
			eq(helper, PlayerProfileService.get(player).unallocatedStatPoints(), 10, "points refunded");
			eq(helper, PlayerProfileService.get(player).currency(), 0L, "cost paid");
			helper.succeed();
		});
	}

	private static void eq(GameTestHelper helper, Object actual, Object expected, String what) {
		helper.assertTrue(Objects.equals(actual, expected), what + ": expected " + expected + " but got " + actual);
	}
}
