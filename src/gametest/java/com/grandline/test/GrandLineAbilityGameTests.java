package com.grandline.test;

import com.grandline.GrandLineMod;
import com.grandline.combat.EnergyService;
import com.grandline.combat.ability.AbilityActivationHandler;
import com.grandline.combat.ability.AbilityActivationHandler.Outcome;
import com.grandline.combat.ability.PowerSource;
import com.grandline.combat.style.FightingStyles;
import com.grandline.network.payload.AbilityActivationRequestPayload;
import com.grandline.player.PlayerProfileService;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;

/** Milestone 3 game tests: the move pipeline with the Brawler fighting style. */
public final class GrandLineAbilityGameTests implements FabricGameTest {
	private static final int WAIT = 0;

	private static Outcome use(ServerPlayer player, PowerSource source, int slot) {
		return AbilityActivationHandler.handle(player, new AbilityActivationRequestPayload(source.ordinal(), slot));
	}

	private static int gap() {
		return GrandLineMod.config().requestCooldownTicks + 1;
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "ability_1")
	public void slamDamagesNearbyMobsAndGrantsMastery(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		PlayerProfileService.setMasteryLevel(player, FightingStyles.BRAWLER, 25);
		Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, 1));
		zombie.setNoAi(true);
		zombie.moveTo(player.getX() + 1.5, player.getY(), player.getZ());
		float healthBefore = zombie.getHealth();
		long masteryBefore = PlayerProfileService.get(player).powers().masteryXp(FightingStyles.BRAWLER.id().toString());

		eq(helper, use(player, PowerSource.STYLE, 2), Outcome.USED, "Ground Slam");
		helper.assertTrue(zombie.getHealth() < healthBefore, "zombie took damage");
		long afterHit = PlayerProfileService.get(player).powers().masteryXp(FightingStyles.BRAWLER.id().toString());
		eq(helper, afterHit, masteryBefore, "hitting alone gives no mastery");

		// Finish it with another move: the kill credits the Brawler style.
		helper.runAfterDelay(Math.max(gap(), 12), () -> { // past the zombie's post-hit invulnerability
			zombie.setHealth(1f);
			zombie.setDeltaMovement(0, 0, 0);
			zombie.moveTo(player.getX() + 1.5, player.getY(), player.getZ());
			player.setYRot(-90);
			player.setXRot(0);
			eq(helper, use(player, PowerSource.STYLE, 0), Outcome.USED, "finishing jab");
			helper.assertTrue(zombie.isDeadOrDying(), "zombie killed");
			long afterKill = PlayerProfileService.get(player).powers().masteryXp(FightingStyles.BRAWLER.id().toString());
			eq(helper, afterKill - masteryBefore, (long) Math.round(zombie.getMaxHealth() * GrandLineMod.config().masteryPerKilledHealth),
					"kill mastery scales with the zombie's max health");
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "ability_2")
	public void lockedMovesAndMissingFruitAreRejected(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		eq(helper, use(player, PowerSource.STYLE, 3), Outcome.LOCKED, "Uppercut needs mastery 50");
		helper.runAfterDelay(gap(), () -> {
			eq(helper, use(player, PowerSource.FRUIT, 0), Outcome.NO_POWER, "no fruit eaten");
			helper.runAfterDelay(gap(), () -> {
				eq(helper, AbilityActivationHandler.handle(player, new AbilityActivationRequestPayload(7, 0)),
						Outcome.INVALID_SLOT, "unknown power source");
				helper.succeed();
			});
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "ability_3")
	public void cooldownBlocksReuse(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		eq(helper, use(player, PowerSource.STYLE, 0), Outcome.USED, "first jab");
		helper.runAfterDelay(gap(), () -> {
			eq(helper, use(player, PowerSource.STYLE, 0), Outcome.ON_COOLDOWN, "second jab during cooldown");
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "ability_4")
	public void movesNeedEnergy(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		helper.assertTrue(EnergyService.trySpend(player, 0.97), "drain energy");
		eq(helper, use(player, PowerSource.STYLE, 0), Outcome.NO_ENERGY, "jab without energy");
		helper.assertTrue(EnergyService.current(player) < EnergyService.max(player) * 0.1, "nothing refunded");
		helper.runAfterDelay(40, () -> {
			helper.assertTrue(EnergyService.current(player) > EnergyService.max(player) * 0.05, "energy regenerates");
			helper.succeed();
		});
	}

	private static void eq(GameTestHelper helper, Object actual, Object expected, String what) {
		helper.assertTrue(java.util.Objects.equals(actual, expected), what + ": expected " + expected + " but got " + actual);
	}
}
