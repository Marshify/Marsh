package com.grandline.test;

import com.grandline.GrandLineMod;
import com.grandline.network.GrandLineNetworking;
import com.grandline.network.payload.AllocateStatPayload;
import com.grandline.player.PlayerProfile;
import com.grandline.player.PlayerProfileService;
import com.grandline.player.StatRules;
import com.grandline.player.StatType;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Objects;

/** Milestone 2 game tests: stat allocation, stat effects and mob kill rewards, inside a real server. */
public final class GrandLineStatsGameTests implements FabricGameTest {
	private static ServerPlayer playerWithPoints(GameTestHelper helper, int points) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		PlayerProfileService.update(player, profile -> profile.withUnallocatedStatPoints(points));
		return player;
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void allocationRequestsAreValidated(GameTestHelper helper) {
		ServerPlayer player = playerWithPoints(helper, 5);

		eq(helper, GrandLineNetworking.handleAllocateStat(player, new AllocateStatPayload(StatType.DEFENSE.ordinal(), 3)),
				StatRules.AllocationResult.SUCCESS, "valid allocation");
		PlayerProfile afterValid = PlayerProfileService.get(player);
		eq(helper, afterValid.stats().get(StatType.DEFENSE), 3, "defense points");
		eq(helper, afterValid.unallocatedStatPoints(), 2, "remaining points");

		// Each of these must be rejected and leave the profile untouched (run after the rate-limit window).
		helper.runAfterDelay(GrandLineMod.config().requestCooldownTicks + 1, () -> {
			eq(helper, GrandLineNetworking.handleAllocateStat(player, new AllocateStatPayload(StatType.MELEE.ordinal(), AllocateStatPayload.MAX_AMOUNT_PER_REQUEST + 1)),
					StatRules.AllocationResult.INVALID_AMOUNT, "amount above per-request limit");
			eq(helper, PlayerProfileService.get(player), afterValid, "profile unchanged");
		});
		helper.runAfterDelay(2L * (GrandLineMod.config().requestCooldownTicks + 1), () -> {
			eq(helper, GrandLineNetworking.handleAllocateStat(player, new AllocateStatPayload(99, 1)),
					StatRules.AllocationResult.INVALID_AMOUNT, "unknown stat id");
			eq(helper, PlayerProfileService.get(player), afterValid, "profile unchanged");
		});
		helper.runAfterDelay(3L * (GrandLineMod.config().requestCooldownTicks + 1), () -> {
			eq(helper, GrandLineNetworking.handleAllocateStat(player, new AllocateStatPayload(StatType.SWORD.ordinal(), 3)),
					StatRules.AllocationResult.NOT_ENOUGH_POINTS, "more points than available");
			eq(helper, PlayerProfileService.get(player), afterValid, "profile unchanged");
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void defenseRaisesMaxHealthAndSurvivesSaveLoad(GameTestHelper helper) {
		ServerPlayer player = playerWithPoints(helper, 50);
		double base = player.getAttributeValue(Attributes.MAX_HEALTH);
		PlayerProfileService.allocateStat(player, StatType.DEFENSE, 50);

		double expectedBonus = PlayerProfileService.statRules().bonusHealth(PlayerProfileService.get(player).stats());
		helper.assertTrue(expectedBonus > 0, "config gives a defense bonus");
		eq(helper, player.getAttributeValue(Attributes.MAX_HEALTH), base + expectedBonus, "max health with defense");

		player.setHealth(player.getMaxHealth());
		float boostedHealth = player.getHealth();
		helper.assertTrue(boostedHealth > base, "current health above the vanilla maximum");

		CompoundTag saved = player.saveWithoutId(new CompoundTag());
		ServerPlayer reloaded = helper.makeMockServerPlayerInLevel();
		reloaded.load(saved);
		eq(helper, reloaded.getAttributeValue(Attributes.MAX_HEALTH), base + expectedBonus, "max health after reload");
		eq(helper, reloaded.getHealth(), boostedHealth, "boosted current health kept after reload");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void meleeAndSwordBonusesDependOnHeldItem(GameTestHelper helper) {
		ServerPlayer player = playerWithPoints(helper, 40);
		PlayerProfileService.allocateStat(player, StatType.MELEE, 20);
		helper.runAfterDelay(GrandLineMod.config().requestCooldownTicks + 1, () -> PlayerProfileService.allocateStat(player, StatType.SWORD, 10));

		StatRules rules = PlayerProfileService.statRules();
		helper.runAfterDelay(GrandLineMod.config().requestCooldownTicks + 3, () -> {
			PlayerProfile profile = PlayerProfileService.get(player);
			double base = player.getAttribute(Attributes.ATTACK_DAMAGE).getBaseValue();

			player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			helper.runAfterDelay(1, () -> {
				eq(helper, player.getAttributeValue(Attributes.ATTACK_DAMAGE), base + rules.bonusMeleeDamage(profile.stats()), "fist bonus");

				player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
				helper.runAfterDelay(1, () -> {
					eq(helper, player.getAttributeValue(Attributes.ATTACK_DAMAGE), base, "no bonus with a non-weapon");

					player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
					helper.runAfterDelay(1, () -> {
						double swordModifiers = player.getAttributeValue(Attributes.ATTACK_DAMAGE) - base;
						helper.assertTrue(Math.abs(player.getAttribute(Attributes.ATTACK_DAMAGE)
										.getModifier(java.util.UUID.fromString("6f1d2c4e-0b5a-4f3e-9c1a-8a7e5d2b1c02")).getAmount()
										- rules.bonusSwordDamage(profile.stats())) < 1e-9,
								"sword bonus modifier present while holding a sword (total bonus " + swordModifiers + ")");
						helper.succeed();
					});
				});
			});
		});
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void killingHostileMobGrantsReward(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		PlayerProfile before = PlayerProfileService.get(player);

		Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, 1));
		float zombieHealth = zombie.getMaxHealth();
		zombie.hurt(helper.getLevel().damageSources().playerAttack(player), 1000f);
		helper.assertTrue(zombie.isDeadOrDying(), "zombie died");

		PlayerProfile after = PlayerProfileService.get(player);
		long expectedXp = Math.round(zombieHealth * GrandLineMod.config().experiencePerMobHealth);
		long expectedCurrency = Math.round(zombieHealth * GrandLineMod.config().currencyPerMobHealth);
		eq(helper, after.currency() - before.currency(), expectedCurrency, "currency from zombie");
		long totalBefore = before.experience();
		helper.assertTrue(after.level() > before.level() || after.experience() - totalBefore == expectedXp,
				"experience from zombie (expected +" + expectedXp + ", profile " + after + ")");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void killingPassiveMobGrantsNothing(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		PlayerProfile before = PlayerProfileService.get(player);

		Cow cow = helper.spawn(EntityType.COW, new BlockPos(1, 2, 1));
		cow.hurt(helper.getLevel().damageSources().playerAttack(player), 1000f);
		helper.assertTrue(cow.isDeadOrDying(), "cow died");

		eq(helper, PlayerProfileService.get(player), before, "no reward for passive mobs");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void mobDeathWithoutPlayerGrantsNothing(GameTestHelper helper) {
		ServerPlayer bystander = helper.makeMockServerPlayerInLevel();
		PlayerProfile before = PlayerProfileService.get(bystander);

		Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, 1));
		zombie.hurt(helper.getLevel().damageSources().genericKill(), 1000f);
		helper.assertTrue(zombie.isDeadOrDying(), "zombie died");

		eq(helper, PlayerProfileService.get(bystander), before, "no reward without a killer");
		helper.succeed();
	}

	private static void eq(GameTestHelper helper, Object actual, Object expected, String what) {
		boolean equal = actual instanceof Number a && expected instanceof Number b
				? Math.abs(a.doubleValue() - b.doubleValue()) < 1e-6
				: Objects.equals(actual, expected);
		helper.assertTrue(equal, what + ": expected " + expected + " but got " + actual);
	}
}
