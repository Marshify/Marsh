package com.grandline.test;

import com.grandline.GrandLineMod;
import com.grandline.combat.EnergyService;
import com.grandline.combat.ability.AbilityActivationHandler;
import com.grandline.combat.ability.AbilityActivationHandler.Outcome;
import com.grandline.combat.ability.AbilityCooldownService;
import com.grandline.combat.ability.AbilityDef;
import com.grandline.combat.ability.ChargeService;
import com.grandline.combat.ability.FlightService;
import com.grandline.combat.ability.PowerSource;
import com.grandline.fruit.Fruits;
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

import java.util.List;
import java.util.Objects;

/**
 * Flame Fruit (base form) moveset: shapes, timing, charge and flight, all through the real activation pipeline. Each test
 * runs in its own batch so explosions from one test cannot hit another test's dummies.
 */
public final class GrandLineFlameGameTests implements FabricGameTest {
	private static ServerPlayer flameUser(GameTestHelper helper, int mastery) {
		// Mock players are never removed by the test framework and all spawn at the world spawn; clear earlier ones so
		// they can't block this test's projectiles, then place the caster inside this test's own area.
		var players = helper.getLevel().getServer().getPlayerList();
		for (ServerPlayer old : java.util.List.copyOf(players.getPlayers())) {
			if (old.getGameProfile().getName().equals("test-mock-player")) {
				players.remove(old);
			}
		}
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.moveTo(helper.absoluteVec(new net.minecraft.world.phys.Vec3(1.5, 3, 1.5)));
		player.setGameMode(GameType.SURVIVAL);
		PlayerProfileService.update(player, p -> p.withPowers(p.powers().withFruit(Fruits.EMBER.id())));
		if (mastery > 1) {
			PlayerProfileService.setMasteryLevel(player, Fruits.EMBER.power(), mastery);
		}
		player.setYRot(-90); // look along +X
		player.setXRot(0);
		return player;
	}

	/** A stationary zombie {@code forward} blocks ahead of the player (+X) and {@code side} blocks to the side (+Z). */
	private static Zombie dummy(GameTestHelper helper, ServerPlayer player, double forward, double side) {
		Zombie zombie = helper.spawn(EntityType.HUSK, new BlockPos(1, 2, 1)); // husks don't burn in daylight
		zombie.setNoAi(true);
		zombie.setNoGravity(true);
		zombie.moveTo(player.getX() + forward, player.getY(), player.getZ() + side);
		return zombie;
	}

	private static Outcome use(ServerPlayer player, int slot) {
		return AbilityActivationHandler.handle(player, new AbilityActivationRequestPayload(PowerSource.FRUIT.ordinal(), slot));
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "flame_1")
	public void flameKeepsIdsAndUsesBloxMasteryLevels(GameTestHelper helper) {
		eq(helper, Fruits.EMBER.id(), "grandline:ember", "power id kept for saves");
		eq(helper, net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(Fruits.EMBER.item()).toString(), "grandline:ember_fruit", "item id kept");
		List<AbilityDef> moves = Fruits.EMBER.power().abilities();
		eq(helper, moves.stream().map(AbilityDef::requiredMastery).toList(), List.of(1, 25, 50, 100, 10), "mastery Z X C V R");
		eq(helper, moves.get(3).activation(), AbilityDef.Activation.CHARGED, "Flame Destroyer charges");
		eq(helper, moves.get(4).activation(), AbilityDef.Activation.TOGGLE, "Fire Flight toggles");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "flame_2")
	public void fireBulletsTravelThenBurn(GameTestHelper helper) {
		ServerPlayer player = flameUser(helper, 1);
		Zombie zombie = dummy(helper, player, 10, 0);
		float before = zombie.getHealth();
		eq(helper, use(player, 0), Outcome.USED, "Fire Bullets at mastery 1");
		eq(helper, zombie.getHealth(), before, "bullets are projectiles: no instant hit");
		helper.runAfterDelay(20, () -> {
			helper.assertTrue(zombie.getHealth() < before, "volley hit the zombie");
			helper.assertTrue(zombie.isOnFire(), "bullets set the target on fire");
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "flame_3")
	public void burningBlastExplodesAndKnocksBack(GameTestHelper helper) {
		ServerPlayer player = flameUser(helper, 25);
		Zombie front = dummy(helper, player, 9, 0);
		Zombie beside = dummy(helper, player, 10, 2.5);
		Zombie far = dummy(helper, player, 9, 9);
		float frontBefore = front.getHealth();
		eq(helper, use(player, 1), Outcome.USED, "Burning Blast at mastery 25");
		helper.runAfterDelay(15, () -> {
			helper.assertTrue(front.getHealth() < frontBefore, "direct target damaged");
			helper.assertTrue(beside.getHealth() < beside.getMaxHealth(), "explosion splashes nearby enemies");
			eq(helper, far.getHealth(), far.getMaxHealth(), "enemies outside the blast are untouched");
			helper.assertTrue(beside.getZ() > player.getZ() + 2.5 + 0.1 || beside.getDeltaMovement().z > 0, "splash target knocked away");
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "flame_4")
	public void fireColumnLiftsNearbyEnemies(GameTestHelper helper) {
		ServerPlayer player = flameUser(helper, 50);
		Zombie close = dummy(helper, player, 2.5, 0);
		Zombie far = dummy(helper, player, 9, 0);
		double startY = close.getY();
		eq(helper, use(player, 2), Outcome.USED, "Fire Column at mastery 50");
		helper.runAfterDelay(8, () -> {
			helper.assertTrue(close.getHealth() < close.getMaxHealth(), "column damages enemies around the caster");
			helper.assertTrue(close.getDeltaMovement().y > 0.2 || close.getY() > startY + 0.5, "column lifts them");
			eq(helper, far.getHealth(), far.getMaxHealth(), "column is centred on the caster");
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "flame_5")
	public void flameDestroyerTapIsSmallerThanFullCharge(GameTestHelper helper) {
		// Tap: quick cast; an enemy 6.5 blocks to the side of the impact survives untouched.
		ServerPlayer player = flameUser(helper, 100);
		Zombie target = dummy(helper, player, 12, 0);
		Zombie side = dummy(helper, player, 12, 6.5);
		eq(helper, use(player, 3), Outcome.USED, "start charging");
		helper.assertTrue(ChargeService.isCharging(player), "charging after press");
		eq(helper, AbilityActivationHandler.releaseCharge(player, 3), Outcome.USED, "quick release");
		helper.runAfterDelay(25, () -> {
			helper.assertTrue(target.getHealth() < target.getMaxHealth(), "quick fireball hits");
			eq(helper, side.getHealth(), side.getMaxHealth(), "tap explosion stays small");
			helper.assertTrue(AbilityCooldownService.isOnCooldown(player, Fruits.EMBER.power().abilityId(Fruits.EMBER.power().abilities().get(3))),
					"cooldown starts on release");
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "flame_6", timeoutTicks = 200)
	public void flameDestroyerFullChargeAutoReleasesWithWiderBlast(GameTestHelper helper) {
		ServerPlayer player = flameUser(helper, 100);
		Zombie target = dummy(helper, player, 12, 0);
		Zombie side = dummy(helper, player, 12, 6.5);
		double energyBefore = EnergyService.current(player);
		eq(helper, use(player, 3), Outcome.USED, "start charging");
		helper.runAfterDelay(10, () -> {
			helper.assertTrue(ChargeService.isCharging(player), "still charging while held");
			eq(helper, target.getHealth(), target.getMaxHealth(), "nothing fired yet");
		});
		helper.runAfterDelay(ChargeService.MAX_CHARGE_TICKS + 40, () -> {
			helper.assertFalse(ChargeService.isCharging(player), "auto-released at full charge");
			helper.assertTrue(EnergyService.current(player) < energyBefore, "energy spent on release");
			helper.assertTrue(side.getHealth() < side.getMaxHealth(), "charged explosion is wider (target hp " + target.getHealth()
					+ ", side hp " + side.getHealth() + ", player " + player.position() + ", side " + side.position() + ")");
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "flame_7", timeoutTicks = 120)
	public void fireFlightTogglesSafely(GameTestHelper helper) {
		ServerPlayer player = flameUser(helper, 10);
		eq(helper, use(player, 4), Outcome.USED, "take off");
		helper.assertTrue(FlightService.isFlying(player) && player.getAbilities().mayfly && player.getAbilities().flying, "flying");
		double energyStart = EnergyService.current(player);
		helper.runAfterDelay(30, () -> {
			helper.assertTrue(EnergyService.current(player) < energyStart, "flight drains energy");
			eq(helper, use(player, 4), Outcome.USED, "land");
			helper.assertFalse(FlightService.isFlying(player), "flight ended");
			// Mock test players always report creative mode, where flight is (correctly) left alone, so the survival
			// revoke path is covered by the real-client checklist instead.
			helper.assertTrue(AbilityCooldownService.isOnCooldown(player, Fruits.EMBER.power().abilityId(Fruits.EMBER.power().abilities().get(4))),
					"cooldown starts on landing");
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "flame_8")
	public void lockedFlameMovesRespectMastery(GameTestHelper helper) {
		ServerPlayer player = flameUser(helper, 1);
		eq(helper, use(player, 3), Outcome.LOCKED, "Flame Destroyer needs mastery 100");
		helper.runAfterDelay(GrandLineMod.config().requestCooldownTicks + 1, () -> {
			eq(helper, use(player, 4), Outcome.LOCKED, "Fire Flight needs mastery 10");
			helper.succeed();
		});
	}

	private static void eq(GameTestHelper helper, Object actual, Object expected, String what) {
		boolean equal = actual instanceof Number a && expected instanceof Number b
				? Math.abs(a.doubleValue() - b.doubleValue()) < 1e-6 : Objects.equals(actual, expected);
		helper.assertTrue(equal, what + ": expected " + expected + " but got " + actual);
	}
}
