package com.grandline.test;

import com.grandline.combat.EnergyService;
import com.grandline.combat.ability.AbilityActivationHandler;
import com.grandline.combat.ability.AbilityActivationHandler.Outcome;
import com.grandline.combat.ability.AbilityCooldownService;
import com.grandline.combat.ability.AbilityDef;
import com.grandline.combat.ability.ChargeService;
import com.grandline.combat.ability.FlightService;
import com.grandline.combat.ability.PowerSource;
import com.grandline.fruit.FruitRarity;
import com.grandline.fruit.FruitService;
import com.grandline.fruit.Fruits;
import com.grandline.fruit.spin.SpinMoves;
import com.grandline.network.payload.AbilityActivationRequestPayload;
import com.grandline.network.payload.AbilityReleasePayload;
import com.grandline.player.PlayerProfileService;
import com.grandline.player.StatType;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Objects;

/** Spin Fruit rework: the four moves through the real activation pipeline, each test in its own batch. */
public final class GrandLineSpinGameTests implements FabricGameTest {
	private static ServerPlayer spinUser(GameTestHelper helper, int mastery) {
		var players = helper.getLevel().getServer().getPlayerList();
		for (ServerPlayer old : List.copyOf(players.getPlayers())) {
			if (old.getGameProfile().getName().equals("test-mock-player")) {
				players.remove(old);
			}
		}
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		GrandLineRaceGameTests.neutral(player);
		player.moveTo(helper.absoluteVec(new Vec3(1.5, 2, 1.5)));
		player.setGameMode(GameType.SURVIVAL);
		PlayerProfileService.update(player, p -> p.withPowers(p.powers().withFruit(Fruits.SPIN.id())));
		if (mastery > 1) {
			PlayerProfileService.setMasteryLevel(player, Fruits.SPIN.power(), mastery);
		}
		player.setYRot(-90); // look along +X
		player.setXRot(0);
		return player;
	}

	/** An armourless, unmoving dummy (so damage is exact). */
	private static Zombie dummy(GameTestHelper helper, Vec3 at) {
		Zombie zombie = helper.spawn(EntityType.HUSK, new BlockPos(1, 2, 1));
		zombie.setNoAi(true);
		zombie.setNoGravity(true);
		zombie.getAttribute(Attributes.ARMOR).setBaseValue(0);
		zombie.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);
		zombie.setHealth(100);
		zombie.moveTo(at.x, at.y, at.z);
		return zombie;
	}

	private static double mult(ServerPlayer player) {
		return PlayerProfileService.statRules().abilityMultiplier(PlayerProfileService.get(player).stats(), StatType.FRUIT);
	}

	private static Outcome use(ServerPlayer player, int slot) {
		return AbilityActivationHandler.handle(player, new AbilityActivationRequestPayload(PowerSource.FRUIT.ordinal(), slot));
	}

	private static void release(ServerPlayer player, int slot) {
		AbilityActivationHandler.release(player, new AbilityReleasePayload(PowerSource.FRUIT.ordinal(), slot));
	}

	private static float lost(Zombie z) {
		return 100 - z.getHealth();
	}

	private static void near(GameTestHelper helper, double actual, double expected, String what) {
		helper.assertTrue(Math.abs(actual - expected) < 0.05, what + ": expected " + expected + ", got " + actual);
	}

	private static void eq(GameTestHelper helper, Object actual, Object expected, String what) {
		helper.assertTrue(Objects.equals(actual, expected), what + ": expected " + expected + ", got " + actual);
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "spin_1")
	public void spinKeepsIdentityAndHasItsFourMoves(GameTestHelper helper) {
		eq(helper, Fruits.SPIN.id(), "grandline:spin", "power id kept");
		eq(helper, Fruits.SPIN.rarity(), FruitRarity.COMMON, "rarity kept");
		eq(helper, FruitService.price(Fruits.SPIN), 7_500L, "dealer price");
		List<AbilityDef> moves = Fruits.SPIN.power().abilities();
		eq(helper, moves.stream().map(AbilityDef::key).toList(), List.of("spin_razor_wind", "spin_tornado_assault", "spin_air_slasher",
				AbilityDef.EMPTY_KEY, "spin_helicopter_flight"), "Z X C (V unused) F");
		List<AbilityDef> used = moves.stream().filter(m -> !m.isEmpty()).toList();
		eq(helper, used.stream().map(AbilityDef::requiredMastery).toList(), List.of(1, 50, 90, 25), "mastery");
		eq(helper, used.stream().map(AbilityDef::cooldownTicks).toList(), List.of(150, 220, 285, 150), "cooldowns");
		eq(helper, used.stream().map(AbilityDef::energyCost).toList(), List.of(0.15, 0.25, 0.35, 0.50), "energy");
		eq(helper, moves.get(0).activation(), AbilityDef.Activation.HELD, "Razor Wind is held");
		eq(helper, moves.get(4).activation(), AbilityDef.Activation.TOGGLE, "Helicopter Flight toggles");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "spin_2", timeoutTicks = 60)
	public void razorWindTapLastsSixTenthsAndDeals45Percent(GameTestHelper helper) {
		ServerPlayer player = spinUser(helper, 1);
		Zombie target = dummy(helper, player.position().add(3, 0, 0));
		double energy = EnergyService.current(player);
		eq(helper, use(player, 0), Outcome.USED, "Razor Wind pressed");
		helper.assertTrue(EnergyService.current(player) < energy, "energy spent on press");
		helper.runAfterDelay(3, () -> release(player, 0));
		helper.runAfterDelay(30, () -> {
			helper.assertFalse(ChargeService.isCharging(player), "hold ended on release");
			near(helper, lost(target), SpinMoves.MISSILE_FIST * 0.45 * mult(player), "tap = 2 hits = 45% of Missile Fist");
			helper.assertTrue(AbilityCooldownService.isOnCooldown(player, Fruits.SPIN.power().abilityId(Fruits.SPIN.power().abilities().get(0))), "cooldown");
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "spin_3", timeoutTicks = 100)
	public void razorWindHeldStopsAtTheCapAndDeals95Percent(GameTestHelper helper) {
		ServerPlayer player = spinUser(helper, 1);
		Zombie target = dummy(helper, player.position().add(4, 0, 1));
		eq(helper, use(player, 0), Outcome.USED, "Razor Wind held");
		helper.runAfterDelay(SpinMoves.RAZOR_MAX_TICKS + 10, () -> {
			helper.assertFalse(ChargeService.isCharging(player), "the server ended the hold at 2.5 s");
			near(helper, lost(target), SpinMoves.MISSILE_FIST * 0.95 * mult(player), "full hold = 5 hits = 95%");
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "spin_4", timeoutTicks = 100)
	public void razorWindCanBeEscapedAndIgnoresTargetsOutsideItsField(GameTestHelper helper) {
		ServerPlayer player = spinUser(helper, 1);
		Zombie runner = dummy(helper, player.position().add(3, 0, 0));
		Zombie behind = dummy(helper, player.position().add(-3, 0, 0));
		Zombie far = dummy(helper, player.position().add(9, 0, 0));
		eq(helper, use(player, 0), Outcome.USED, "Razor Wind held");
		helper.runAfterDelay(5, () -> runner.moveTo(runner.getX(), runner.getY(), runner.getZ() + 6)); // steps out after one hit
		helper.runAfterDelay(SpinMoves.RAZOR_MAX_TICKS + 10, () -> {
			near(helper, lost(runner), SpinMoves.MISSILE_FIST * 0.225 * mult(player), "only the first hit lands once it escapes");
			eq(helper, lost(behind), 0f, "nothing behind the caster");
			eq(helper, lost(far), 0f, "nothing past 6 blocks");
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "spin_5", timeoutTicks = 120)
	public void tornadoAssaultMovesPullsAndDeals110Percent(GameTestHelper helper) {
		ServerPlayer player = spinUser(helper, 50);
		Vec3 start = player.position();
		Zombie target = dummy(helper, start.add(4, 0, 2));
		eq(helper, use(player, 1), Outcome.USED, "Tornado Assault at mastery 50");
		helper.assertTrue(SpinMoves.isTornado(player), "whirl running");
		helper.runAfterDelay(SpinMoves.TORNADO_WINDUP_TICKS + SpinMoves.TORNADO_TICKS + 5, () -> {
			helper.assertFalse(SpinMoves.isTornado(player), "whirl finished");
			helper.assertTrue(player.getX() - start.x > 6, "travelled forward (" + (player.getX() - start.x) + ")");
			helper.assertTrue(player.getX() - start.x <= SpinMoves.TORNADO_SPEED * SpinMoves.TORNADO_TICKS + 0.5, "never faster than 12 blocks/s");
			helper.assertTrue(lost(target) > 0, "the whirl hit what it passed");
			helper.assertTrue(lost(target) <= SpinMoves.MISSILE_FIST * 1.10 * mult(player) + 0.05, "at most 110% (" + lost(target) + ")");
			helper.assertTrue(player.fallDistance == 0, "no fall damage build-up");
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "spin_6", timeoutTicks = 120)
	public void tornadoAssaultStopsAtWalls(GameTestHelper helper) {
		ServerPlayer player = spinUser(helper, 50);
		BlockPos wall = BlockPos.containing(player.getX() + 4, player.getY(), player.getZ());
		for (int dz = -2; dz <= 2; dz++) {
			for (int dy = -4; dy < 3; dy++) { // from the floor up (the mock player starts above the floor)
				helper.getLevel().setBlockAndUpdate(wall.offset(0, dy, dz), Blocks.STONE.defaultBlockState());
			}
		}
		eq(helper, use(player, 1), Outcome.USED, "Tornado Assault");
		helper.runAfterDelay(SpinMoves.TORNADO_WINDUP_TICKS + SpinMoves.TORNADO_TICKS + 5, () -> {
			helper.assertTrue(player.getX() + 0.3 <= wall.getX(), "stopped before the wall (x " + player.getX() + ", wall " + wall.getX() + ")");
			helper.assertTrue(helper.getLevel().getBlockState(wall).is(Blocks.STONE), "no block was broken");
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "spin_7", timeoutTicks = 120)
	public void airSlasherFiresSixBladesFor125Percent(GameTestHelper helper) {
		ServerPlayer player = spinUser(helper, 90);
		Zombie target = dummy(helper, player.position().add(10, 0, 0));
		eq(helper, use(player, 2), Outcome.USED, "Air Slasher at mastery 90");
		helper.runAfterDelay(SpinMoves.SLASHER_WINDUP_TICKS - 1, () -> eq(helper, lost(target), 0f, "no blade before the first sweep"));
		helper.runAfterDelay(SpinMoves.SLASHER_FINAL_TICK + 20, () -> {
			near(helper, lost(target), SpinMoves.MISSILE_FIST * 1.25 * mult(player), "five blades + finisher = 125%");
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "spin_8", timeoutTicks = 120)
	public void helicopterFlightFliesSlowerThanRocketAndWaterEndsIt(GameTestHelper helper) {
		ServerPlayer player = spinUser(helper, 25);
		double energy = EnergyService.current(player);
		eq(helper, use(player, 4), Outcome.USED, "Helicopter Flight at mastery 25");
		helper.assertTrue(FlightService.isFlying(player), "flying");
		helper.assertTrue(player.getAbilities().getFlyingSpeed() < 0.09f, "slower than Blast Off");
		helper.assertTrue(energy - EnergyService.current(player) >= EnergyService.cost(player, 0.5) - 1e-6, "50% to take off");
		helper.runAfterDelay(20, () -> {
			helper.getLevel().setBlockAndUpdate(player.blockPosition(), Blocks.WATER.defaultBlockState());
			player.moveTo(player.getX(), player.getY(), player.getZ());
		});
		helper.runAfterDelay(26, () -> {
			helper.assertFalse(FlightService.isFlying(player), "water ended the flight");
			helper.assertTrue(AbilityCooldownService.isOnCooldown(player, Fruits.SPIN.power().abilityId(Fruits.SPIN.power().abilities().get(4))), "cooldown on exit");
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "spin_9")
	public void masteryLocksAndLowEnergy(GameTestHelper helper) {
		ServerPlayer player = spinUser(helper, 25);
		eq(helper, use(player, 1), Outcome.LOCKED, "Tornado Assault needs 50");
		helper.runAfterDelay(com.grandline.GrandLineMod.config().requestCooldownTicks + 1, () -> {
			eq(helper, use(player, 2), Outcome.LOCKED, "Air Slasher needs 90");
			helper.runAfterDelay(com.grandline.GrandLineMod.config().requestCooldownTicks + 1, () -> {
				EnergyService.trySpend(player, 0.95);
				eq(helper, use(player, 0), Outcome.NO_ENERGY, "Razor Wind needs 15%");
				helper.assertFalse(ChargeService.isCharging(player), "no hold starts without energy");
				helper.succeed();
			});
		});
	}
}
