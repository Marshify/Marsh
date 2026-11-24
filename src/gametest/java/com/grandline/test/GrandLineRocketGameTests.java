package com.grandline.test;

import com.grandline.combat.EnergyService;
import com.grandline.combat.ability.AbilityActivationHandler;
import com.grandline.combat.ability.AbilityActivationHandler.Outcome;
import com.grandline.combat.ability.AbilityCooldownService;
import com.grandline.combat.ability.AbilityDef;
import com.grandline.combat.ability.FlightService;
import com.grandline.combat.ability.PowerSource;
import com.grandline.combat.ability.Projectiles;
import com.grandline.fruit.FruitRarity;
import com.grandline.fruit.Fruits;
import com.grandline.fruit.rocket.RocketMoves;
import com.grandline.network.payload.AbilityActivationRequestPayload;
import com.grandline.player.PlayerProfileService;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Objects;

/** Rocket Fruit rework: the four moves through the real activation pipeline, each test in its own batch. */
public final class GrandLineRocketGameTests implements FabricGameTest {
	private static ServerPlayer rocketUser(GameTestHelper helper, int mastery) {
		var players = helper.getLevel().getServer().getPlayerList();
		for (ServerPlayer old : List.copyOf(players.getPlayers())) {
			if (old.getGameProfile().getName().equals("test-mock-player")) {
				players.remove(old);
			}
		}
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		GrandLineRaceGameTests.neutral(player);
		player.moveTo(helper.absoluteVec(new Vec3(1.5, 3, 1.5)));
		player.setGameMode(GameType.SURVIVAL);
		PlayerProfileService.update(player, p -> p.withPowers(p.powers().withFruit(Fruits.ROCKET.id())));
		if (mastery > 1) {
			PlayerProfileService.setMasteryLevel(player, Fruits.ROCKET.power(), mastery);
		}
		player.setYRot(-90);
		player.setXRot(0);
		return player;
	}

	private static Zombie dummy(GameTestHelper helper, Vec3 at) {
		Zombie zombie = helper.spawn(EntityType.HUSK, new BlockPos(1, 2, 1));
		zombie.setNoAi(true);
		zombie.setNoGravity(true);
		zombie.moveTo(at.x, at.y, at.z);
		return zombie;
	}

	private static Outcome use(ServerPlayer player, int slot) {
		return AbilityActivationHandler.handle(player, new AbilityActivationRequestPayload(PowerSource.FRUIT.ordinal(), slot));
	}

	private static void eq(GameTestHelper helper, Object actual, Object expected, String what) {
		helper.assertTrue(Objects.equals(actual, expected), what + ": expected " + expected + ", got " + actual);
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "rocket_1")
	public void rocketKeepsIdentityAndHasItsFourMoves(GameTestHelper helper) {
		eq(helper, Fruits.ROCKET.id(), "grandline:rocket", "power id kept");
		eq(helper, Fruits.ROCKET.rarity(), FruitRarity.COMMON, "rarity kept");
		List<AbilityDef> moves = Fruits.ROCKET.power().abilities();
		eq(helper, moves.stream().map(AbilityDef::key).toList(),
				List.of("rocket_missile_fist", "rocket_air_strike", "rocket_crash", AbilityDef.EMPTY_KEY, "rocket_blast_off"), "Z X C (V unused) F");
		eq(helper, moves.stream().filter(m -> !m.isEmpty()).map(AbilityDef::requiredMastery).toList(), List.of(1, 20, 50, 75), "mastery");
		eq(helper, moves.stream().filter(m -> !m.isEmpty()).map(AbilityDef::cooldownTicks).toList(), List.of(150, 210, 285, 150), "cooldowns");
		eq(helper, moves.stream().filter(m -> !m.isEmpty()).map(AbilityDef::energyCost).toList(), List.of(0.15, 0.25, 0.35, 0.35), "energy");
		eq(helper, moves.get(4).activation(), AbilityDef.Activation.TOGGLE, "Blast Off toggles");
		ServerPlayer player = rocketUser(helper, 100);
		eq(helper, use(player, 3), Outcome.INVALID_SLOT, "the unused V slot does nothing");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "rocket_2")
	public void missileFistFliesAndExplodes(GameTestHelper helper) {
		ServerPlayer player = rocketUser(helper, 1);
		Zombie target = dummy(helper, player.position().add(10, 0, 0));
		Zombie beside = dummy(helper, player.position().add(10.5, 0, 1.6)); // inside the blast, off the line
		float before = target.getHealth();
		eq(helper, use(player, 0), Outcome.USED, "Missile Fist at mastery 1");
		eq(helper, target.getHealth(), before, "a projectile: no instant hit");
		helper.runAfterDelay(20, () -> {
			helper.assertTrue(target.getHealth() < before, "missile hit the target");
			helper.assertTrue(beside.getHealth() < beside.getMaxHealth(), "the blast hurts what stands next to it");
			helper.assertTrue(AbilityCooldownService.isOnCooldown(player, Fruits.ROCKET.power().abilityId(Fruits.ROCKET.power().abilities().get(0))), "cooldown");
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "rocket_3", timeoutTicks = 120)
	public void airStrikeRainsOnTheMarkAndCapsAt120Percent(GameTestHelper helper) {
		ServerPlayer player = rocketUser(helper, 20);
		player.setXRot(25);
		Vec3 mark = RocketMoves.groundBelow(player, Projectiles.crosshairTarget(player, RocketMoves.STRIKE_RANGE));
		Zombie target = dummy(helper, mark);
		target.setHealth(target.getMaxHealth());
		eq(helper, use(player, 1), Outcome.USED, "Air Strike at mastery 20");
		helper.runAfterDelay(RocketMoves.STRIKE_WINDUP_TICKS - 1, () -> eq(helper, target.getHealth(), target.getMaxHealth(), "nothing lands before the first rocket"));
		int last = RocketMoves.STRIKE_WINDUP_TICKS + RocketMoves.STRIKE_INTERVAL * (RocketMoves.STRIKE_ROCKETS - 1) + RocketMoves.STRIKE_FLIGHT_TICKS + 3;
		helper.runAfterDelay(last, () -> {
			float lost = target.getMaxHealth() - target.getHealth();
			helper.assertTrue(lost > 0, "rockets hit the mark (at " + mark + ")");
			helper.assertTrue(lost <= RocketMoves.BASE_DAMAGE * 1.2f + 0.01f, "at most 120% in all, took " + lost);
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "rocket_4", timeoutTicks = 100)
	public void rocketCrashLaunchesTheCasterAndExplodes(GameTestHelper helper) {
		ServerPlayer player = rocketUser(helper, 50);
		Vec3 start = player.position();
		Zombie target = dummy(helper, start.add(6, 0, 0));
		eq(helper, use(player, 2), Outcome.USED, "Rocket Crash at mastery 50");
		helper.assertTrue(RocketMoves.isCrashing(player), "crash running");
		helper.runAfterDelay(40, () -> {
			helper.assertFalse(RocketMoves.isCrashing(player), "crash finished");
			helper.assertTrue(target.getHealth() < target.getMaxHealth(), "crash blast hit the target");
			helper.assertTrue(player.fallDistance == 0, "no fall damage build-up");
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "rocket_5", timeoutTicks = 120)
	public void blastOffFliesDrainsAndLands(GameTestHelper helper) {
		ServerPlayer player = rocketUser(helper, 75);
		double energyStart = EnergyService.current(player);
		eq(helper, use(player, 4), Outcome.USED, "Blast Off at mastery 75");
		helper.assertTrue(FlightService.isFlying(player), "flying");
		helper.assertTrue(EnergyService.current(player) < energyStart, "start cost spent");
		helper.runAfterDelay(30, () -> {
			eq(helper, use(player, 4), Outcome.USED, "land");
			helper.assertFalse(FlightService.isFlying(player), "flight ended");
			helper.assertTrue(AbilityCooldownService.isOnCooldown(player, Fruits.ROCKET.power().abilityId(Fruits.ROCKET.power().abilities().get(4))), "cooldown on landing");
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "rocket_6")
	public void lockedRocketMovesRespectMastery(GameTestHelper helper) {
		ServerPlayer player = rocketUser(helper, 1);
		eq(helper, use(player, 4), Outcome.LOCKED, "Blast Off needs mastery 75");
		helper.succeed();
	}
}
