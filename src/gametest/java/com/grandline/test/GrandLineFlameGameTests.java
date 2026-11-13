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

import com.grandline.fruit.flame.FlameMoves;

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
		PlayerProfileService.update(player, p -> p.withPowers(p.powers().withFruit(Fruits.FLAME.id())));
		if (mastery > 1) {
			PlayerProfileService.setMasteryLevel(player, Fruits.FLAME.power(), mastery);
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
		eq(helper, Fruits.FLAME.id(), "grandline:flame", "power id renamed to flame");
		eq(helper, net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(Fruits.FLAME.item()).toString(), "grandline:flame_fruit", "item id renamed");
		// Old saves: the ember id loads as flame everywhere, and old ember_fruit stacks become Flame Fruits.
		var old = new com.grandline.player.PowerState("grandline:brawler", "grandline:ember", java.util.Map.of("grandline:ember", 40L),
				java.util.Map.of("grandline:ember", 2), 0L);
		eq(helper, old.fruit(), "grandline:flame", "equipped ember fruit migrates");
		helper.assertTrue(old.mastery().get("grandline:flame") == 40L && old.stash().get("grandline:flame") == 2, "mastery and stash migrate");
		eq(helper, ((com.grandline.fruit.RenamedItem) Fruits.EMBER_FRUIT_LEGACY).replacement(), Fruits.FLAME.item(), "old item turns into the flame fruit");
		List<AbilityDef> moves = Fruits.FLAME.power().abilities();
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
	public void blazeWaveSweepsItsWidthAndPushesForward(GameTestHelper helper) {
		ServerPlayer player = flameUser(helper, 25);
		Zombie front = dummy(helper, player, 9, 0);
		Zombie beside = dummy(helper, player, 10, 2.5);  // inside the 7-wide wave
		Zombie far = dummy(helper, player, 9, 9);        // well outside it
		Zombie early = dummy(helper, player, 12, 0);
		float frontBefore = front.getHealth();
		eq(helper, use(player, 1), Outcome.USED, "Blaze Wave at mastery 25");
		// Windup 7 ticks, then the front moves 0.9 blocks a tick: it reaches 9 blocks around tick 16, 12 blocks around tick 19.
		helper.runAfterDelay(FlameMoves.WAVE_WINDUP_TICKS + 6, () -> eq(helper, early.getHealth(), early.getMaxHealth(),
				"the wave only hits where its front has reached"));
		helper.runAfterDelay(30, () -> {
			helper.assertTrue(front.getHealth() < frontBefore, "the wave hits along its path");
			helper.assertTrue(beside.getHealth() < beside.getMaxHealth(), "the wave hits across its width");
			eq(helper, far.getHealth(), far.getMaxHealth(), "enemies beside the wave are untouched");
			helper.assertTrue(beside.getX() > player.getX() + 10.05 || beside.getDeltaMovement().x > 0, "hit enemies are pushed forward");
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
		helper.runAfterDelay(FlameMoves.RING_GATHER_TICKS + 6, () -> {
			helper.assertTrue(close.getHealth() < close.getMaxHealth(), "the ring damages enemies around the caster");
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
			helper.assertTrue(AbilityCooldownService.isOnCooldown(player, Fruits.FLAME.power().abilityId(Fruits.FLAME.power().abilities().get(3))),
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
			helper.assertTrue(AbilityCooldownService.isOnCooldown(player, Fruits.FLAME.power().abilityId(Fruits.FLAME.power().abilities().get(4))),
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
