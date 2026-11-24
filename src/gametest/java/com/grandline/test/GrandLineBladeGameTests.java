package com.grandline.test;

import com.grandline.combat.EnergyService;
import com.grandline.combat.ability.AbilityActivationHandler;
import com.grandline.combat.ability.AbilityActivationHandler.Outcome;
import com.grandline.combat.ability.AbilityCooldownService;
import com.grandline.combat.ability.AbilityDef;
import com.grandline.combat.ability.FlightService;
import com.grandline.combat.ability.FruitStrikes;
import com.grandline.combat.ability.PowerSource;
import com.grandline.combat.ability.Power;
import com.grandline.fruit.FruitRarity;
import com.grandline.fruit.FruitService;
import com.grandline.fruit.Fruits;
import com.grandline.fruit.blade.BladeMoves;
import com.grandline.network.payload.AbilityActivationRequestPayload;
import com.grandline.player.PlayerProfileService;
import com.grandline.player.StatType;
import com.grandline.technique.CombatContext;
import com.grandline.technique.TechniqueService;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Objects;

/** Blade Fruit rework: M1 combo, the four moves and the Blade Body passive, through the real pipelines. */
public final class GrandLineBladeGameTests implements FabricGameTest {
	private static ServerPlayer bladeUser(GameTestHelper helper, int mastery) {
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
		player.getInventory().selected = 0;
		PlayerProfileService.update(player, p -> p.withPowers(p.powers().withFruit(Fruits.BLADE.id())));
		if (mastery > 1) {
			PlayerProfileService.setMasteryLevel(player, Fruits.BLADE.power(), mastery);
		}
		player.setYRot(-90);
		player.setXRot(0);
		// Dummies left by neighbouring tests must not stand in this test's line of fire.
		helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.monster.Husk.class, player.getBoundingBox().inflate(16)).forEach(e -> e.discard());
		return player;
	}

	private static Zombie dummy(GameTestHelper helper, Vec3 at) {
		Zombie zombie = helper.spawn(EntityType.HUSK, new BlockPos(1, 2, 1));
		zombie.setNoAi(true);
		zombie.setNoGravity(true);
		zombie.getAttribute(Attributes.ARMOR).setBaseValue(0);
		zombie.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);
		zombie.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
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

	private static float lost(Zombie z) {
		return 100 - z.getHealth();
	}

	private static void near(GameTestHelper helper, double actual, double expected, String what) {
		helper.assertTrue(Math.abs(actual - expected) < 0.05, what + ": expected " + expected + ", got " + actual);
	}

	private static void eq(GameTestHelper helper, Object actual, Object expected, String what) {
		helper.assertTrue(Objects.equals(actual, expected), what + ": expected " + expected + ", got " + actual);
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "blade_1")
	public void bladeKeepsIdentityAndHasItsMoves(GameTestHelper helper) {
		eq(helper, Fruits.BLADE.id(), "grandline:blade", "power id kept");
		eq(helper, Fruits.BLADE.rarity(), FruitRarity.COMMON, "rarity kept");
		eq(helper, FruitService.price(Fruits.BLADE), 30_000L, "dealer price");
		helper.assertTrue(FruitStrikes.has(Fruits.BLADE.id()), "Blade has an M1 combo");
		List<AbilityDef> used = Fruits.BLADE.power().abilities().stream().filter(m -> !m.isEmpty()).toList();
		eq(helper, used.stream().map(AbilityDef::key).toList(), List.of("blade_razor_rush", "blade_dance", "blade_barrage", "blade_flight"), "moves");
		eq(helper, Fruits.BLADE.power().abilities().get(3).isEmpty(), true, "V unused, flight on F");
		eq(helper, used.stream().map(AbilityDef::requiredMastery).toList(), List.of(1, 20, 50, 80), "mastery");
		eq(helper, used.stream().map(AbilityDef::cooldownTicks).toList(), List.of(120, 200, 260, 150), "cooldowns");
		eq(helper, used.stream().map(AbilityDef::energyCost).toList(), List.of(0.15, 0.25, 0.35, 0.40), "energy");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "blade_2", timeoutTicks = 80)
	public void bladeStrikesComboDeals55PercentWithFruitScalingAndNoEnergy(GameTestHelper helper) {
		ServerPlayer player = bladeUser(helper, 1);
		Zombie target = dummy(helper, player.position().add(2, 0, 0));
		double energy = EnergyService.current(player);
		eq(helper, FruitStrikes.swing(player), 0, "first strike");
		eq(helper, FruitStrikes.swing(player), -1, "too soon: ignored");
		helper.runAfterDelay(11, () -> eq(helper, FruitStrikes.swing(player), 1, "second strike"));
		helper.runAfterDelay(22, () -> eq(helper, FruitStrikes.swing(player), 2, "third strike"));
		helper.runAfterDelay(30, () -> {
			near(helper, lost(target), BladeMoves.REF * 0.55 * mult(player), "three strikes = 55% of Missile Fist (Fruit-scaled)");
			helper.assertTrue(EnergyService.current(player) >= energy - 1e-6, "no energy spent");
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "blade_3")
	public void bladeBodyBlocksOrdinarySwordsOnly(GameTestHelper helper) {
		ServerPlayer target = bladeUser(helper, 1);
		ServerPlayer attacker = helper.makeMockServerPlayerInLevel();
		attacker.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
		target.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD)); // still immune while holding a weapon
		// Player attackers: the server's own validation (the test server's PvP rules would hide the result of hurt()).
		helper.assertTrue(com.grandline.combat.DefensivePassives.blocked(target, target.damageSources().playerAttack(attacker)), "a plain sword hit glances off");
		Power sword = com.grandline.gear.Gear.powerFor(com.grandline.gear.Gear.SABER);
		helper.assertTrue(CombatContext.during(sword, sword.abilities().get(0),
				() -> com.grandline.combat.DefensivePassives.blocked(target, target.damageSources().playerAttack(attacker))), "a sword move glances off");
		Power flame = Fruits.FLAME.power();
		helper.assertFalse(CombatContext.during(flame, flame.abilities().get(0),
				() -> com.grandline.combat.DefensivePassives.blocked(target, target.damageSources().playerAttack(attacker))), "fruit moves are not blocked");
		TechniqueService.grant(attacker, "aura");
		TechniqueService.toggleAura(attacker);
		helper.assertFalse(com.grandline.combat.DefensivePassives.blocked(target, target.damageSources().playerAttack(attacker)), "an Aura sword cuts through");
		TechniqueService.toggleAura(attacker);
		attacker.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		helper.assertFalse(com.grandline.combat.DefensivePassives.blocked(target, target.damageSources().playerAttack(attacker)), "fists are not blocked");
		// NPC attackers, through the real hurt(): no damage from a sword, normal damage otherwise.
		float hp = target.getHealth();
		Zombie swordsman = dummy(helper, target.position().add(1, 0, 0));
		swordsman.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
		helper.assertTrue(com.grandline.combat.DefensivePassives.blocked(target, target.damageSources().mobAttack(swordsman)), "an NPC's sword is blocked");
		helper.assertFalse(target.hurt(target.damageSources().mobAttack(swordsman), 4f), "an NPC's sword glances off");
		eq(helper, target.getHealth(), hp, "no damage from it");
		Zombie brawler = dummy(helper, target.position().add(-1, 0, 0));
		helper.assertFalse(com.grandline.combat.DefensivePassives.blocked(target, target.damageSources().mobAttack(brawler)), "an NPC's fist is not blocked");
		helper.assertFalse(com.grandline.combat.DefensivePassives.blocked(target, target.damageSources().fall()), "falls are not blocked");
		// Losing the fruit ends the passive at once.
		PlayerProfileService.update(target, p -> p.withPowers(p.powers().withFruit(Fruits.FLAME.id())));
		target.invulnerableTime = 0;
		helper.assertFalse(com.grandline.combat.DefensivePassives.blocked(target, target.damageSources().mobAttack(swordsman)), "without Blade, swords cut again");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "blade_4", timeoutTicks = 80)
	public void razorRushCutsThePathStopsAtWallsAndDeals90Percent(GameTestHelper helper) {
		ServerPlayer player = bladeUser(helper, 1);
		Vec3 start = player.position();
		Zombie target = dummy(helper, start.add(3, 0, 0));
		BlockPos wall = BlockPos.containing(start.x + 5, start.y, start.z); // the dash ends right after the target
		for (int dz = -2; dz <= 2; dz++) {
			for (int dy = -4; dy < 4; dy++) {
				helper.getLevel().setBlockAndUpdate(wall.offset(0, dy, dz), Blocks.STONE.defaultBlockState());
			}
		}
		eq(helper, use(player, 0), Outcome.USED, "Razor Rush");
		helper.runAfterDelay(25, () -> {
			helper.assertTrue(player.getX() > start.x + 3, "dashed forward (" + (player.getX() - start.x) + ")");
			helper.assertTrue(player.getX() + 0.3 <= wall.getX() + 1e-6, "stopped before the wall");
			near(helper, lost(target), BladeMoves.REF * 0.90 * mult(player), "path cut + crossing finisher = 90%");
			helper.assertTrue(helper.getLevel().getBlockState(wall).is(Blocks.STONE), "no block broken");
			helper.assertTrue(player.fallDistance == 0, "no fall damage build-up");
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "blade_5", timeoutTicks = 80)
	public void bladeDanceDeals115PercentAround(GameTestHelper helper) {
		ServerPlayer player = bladeUser(helper, 20);
		Zombie front = dummy(helper, player.position().add(3, 0, 0));
		Zombie behind = dummy(helper, player.position().add(-3, 0, 1));
		Zombie far = dummy(helper, player.position().add(9, 0, 0));
		eq(helper, use(player, 1), Outcome.USED, "Blade Dance at mastery 20");
		helper.runAfterDelay(BladeMoves.DANCE_TICKS + 4, () -> {
			near(helper, lost(front), BladeMoves.REF * 1.15 * mult(player), "three cuts + final sweep = 115%");
			near(helper, lost(behind), BladeMoves.REF * 1.15 * mult(player), "all round the caster");
			eq(helper, lost(far), 0f, "nothing beyond 5 blocks");
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "blade_6", timeoutTicks = 100)
	public void bladeBarrageFiresSevenBladesFor130Percent(GameTestHelper helper) {
		ServerPlayer player = bladeUser(helper, 50);
		Zombie target = dummy(helper, player.position().add(10, 0, 0));
		eq(helper, use(player, 2), Outcome.USED, "Blade Barrage at mastery 50");
		helper.runAfterDelay(BladeMoves.BARRAGE_FIRST - 1, () -> eq(helper, lost(target), 0f, "nothing before the first throw"));
		helper.runAfterDelay(BladeMoves.BARRAGE_FINAL + 15, () -> {
			near(helper, lost(target), BladeMoves.REF * 1.30 * mult(player), "six blades + finisher = 130%");
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "blade_7", timeoutTicks = 80)
	public void bladeFlightSpeedCostAndWater(GameTestHelper helper) {
		ServerPlayer player = bladeUser(helper, 80);
		double energy = EnergyService.current(player);
		eq(helper, use(player, 4), Outcome.USED, "Blade Flight at mastery 80");
		float speed = player.getAbilities().getFlyingSpeed();
		helper.assertTrue(speed > com.grandline.fruit.spin.SpinMoves.FLIGHT_SPEED && speed < 0.09f, "between Helicopter Flight and Blast Off");
		helper.assertTrue(energy - EnergyService.current(player) >= EnergyService.cost(player, 0.4) - 1e-6, "40% to take off");
		helper.runAfterDelay(10, () -> helper.getLevel().setBlockAndUpdate(player.blockPosition(), Blocks.WATER.defaultBlockState()));
		helper.runAfterDelay(14, () -> {
			helper.assertFalse(FlightService.isFlying(player), "water ended the flight");
			helper.assertTrue(AbilityCooldownService.isOnCooldown(player, Fruits.BLADE.power().abilityId(Fruits.BLADE.power().abilities().get(4))), "cooldown on exit");
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "blade_8")
	public void masteryLocksAndM1NeedsAnEmptyHand(GameTestHelper helper) {
		ServerPlayer player = bladeUser(helper, 1);
		eq(helper, use(player, 1), Outcome.LOCKED, "Blade Dance needs 20");
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(com.grandline.gear.Gear.SABER));
		eq(helper, FruitStrikes.swing(player), -1, "a sword in hand: no fruit M1");
		helper.succeed();
	}
}
