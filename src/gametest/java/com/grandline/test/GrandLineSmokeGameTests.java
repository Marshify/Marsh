package com.grandline.test;

import com.grandline.combat.DefensivePassives;
import com.grandline.combat.MovementGuards;
import com.grandline.combat.ability.AbilityActivationHandler;
import com.grandline.combat.ability.AbilityActivationHandler.Outcome;
import com.grandline.combat.ability.AbilityDef;
import com.grandline.combat.ability.PowerSource;
import com.grandline.fruit.FruitRarity;
import com.grandline.fruit.FruitService;
import com.grandline.fruit.Fruits;
import com.grandline.fruit.smoke.SmokeMoves;
import com.grandline.combat.ElementalReflex;
import com.grandline.combat.ability.FlightService;
import com.grandline.combat.ability.AbilityCooldownService;
import com.grandline.combat.EnergyService;
import com.grandline.combat.ability.AttachedCharges;
import com.grandline.combat.ability.FruitStrikes;
import com.grandline.combat.ability.Traps;
import com.grandline.network.payload.AbilityActivationRequestPayload;
import com.grandline.player.PlayerProfileService;
import com.grandline.player.StatType;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Objects;

/** Smoke Fruit rework: Elemental Reflex and the four moves through the real pipeline. */
public final class GrandLineSmokeGameTests implements FabricGameTest {
	/** A Smoke user standing on a stone floor at the test's base (so ground-dependent moves behave as in play). */
	private static ServerPlayer smokeUser(GameTestHelper helper, int mastery) {
		var players = helper.getLevel().getServer().getPlayerList();
		for (ServerPlayer old : List.copyOf(players.getPlayers())) {
			if (old.getGameProfile().getName().equals("test-mock-player")) {
				players.remove(old);
			}
		}
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		GrandLineRaceGameTests.neutral(player);
		BlockPos floor = helper.absolutePos(new BlockPos(1, 1, 1));
		for (int dx = -2; dx <= 26; dx++) {
			for (int dz = -9; dz <= 9; dz++) {
				helper.getLevel().setBlockAndUpdate(floor.offset(dx, 0, dz), Blocks.STONE.defaultBlockState());
			}
		}
		player.moveTo(floor.getX() + 0.5, floor.getY() + 1, floor.getZ() + 0.5);
		player.setGameMode(GameType.SURVIVAL);
		player.getInventory().selected = 0;
		PlayerProfileService.update(player, p -> p.withPowers(p.powers().withFruit(Fruits.SMOKE.id())));
		if (mastery > 1) {
			PlayerProfileService.setMasteryLevel(player, Fruits.SMOKE.power(), mastery);
		}
		player.setYRot(-90); // along +X
		player.setXRot(0);
		helper.getLevel().getEntitiesOfClass(Husk.class, player.getBoundingBox().inflate(30)).forEach(e -> e.discard());
		return player;
	}

	private static Zombie dummy(GameTestHelper helper, Vec3 at, boolean resist) {
		Zombie zombie = helper.spawn(EntityType.HUSK, new BlockPos(1, 2, 1));
		zombie.setNoAi(true);
		zombie.setNoGravity(true);
		zombie.getAttribute(Attributes.ARMOR).setBaseValue(0);
		zombie.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);
		zombie.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(resist ? 1 : 0);
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

	private static void wall(GameTestHelper helper, ServerPlayer player, double ahead) {
		BlockPos w = BlockPos.containing(player.getX() + ahead, player.getY(), player.getZ());
		for (int dz = -4; dz <= 4; dz++) {
			for (int dy = 0; dy < 6; dy++) {
				helper.getLevel().setBlockAndUpdate(w.offset(0, dy, dz), Blocks.STONE.defaultBlockState());
			}
		}
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "smoke_1")
	public void smokeKeepsIdentityAndHasItsMoves(GameTestHelper helper) {
		eq(helper, Fruits.SMOKE.id(), "grandline:smoke", "power id kept");
		eq(helper, Fruits.SMOKE.rarity(), FruitRarity.COMMON, "rarity kept");
		eq(helper, FruitService.price(Fruits.SMOKE), 100_000L, "dealer price");
		List<AbilityDef> moves = Fruits.SMOKE.power().abilities().stream().filter(a -> !a.isEmpty()).toList();
		eq(helper, moves.stream().map(AbilityDef::key).toList(), List.of("smoke_slam", "smoke_blast", "smoke_liberation", "smoke_bomber"), "Z X C F");
		eq(helper, Fruits.SMOKE.power().abilities().get(3).isEmpty(), true, "no V move");
		eq(helper, moves.stream().map(AbilityDef::requiredMastery).toList(), List.of(1, 20, 50, 10), "mastery");
		eq(helper, moves.stream().map(AbilityDef::cooldownTicks).toList(), List.of(105, 180, 270, 90), "cooldowns");
		eq(helper, moves.stream().map(AbilityDef::energyCost).toList(), List.of(0.15, 0.25, 0.5, 0.5), "energy");
		helper.assertFalse(com.grandline.combat.ability.FruitStrikes.has("grandline:smoke"), "no M1");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "smoke_2", timeoutTicks = 40)
	public void elementalReflexRules(GameTestHelper helper) {
		ServerPlayer player = smokeUser(helper, 1);
		PlayerProfileService.update(player, p -> new com.grandline.player.PlayerProfile(p.dataVersion(), 30, p.experience(), p.currency(),
				p.unallocatedStatPoints(), p.stats(), p.powers()));
		var sources = helper.getLevel().damageSources();
		Zombie husk = dummy(helper, player.position().add(1, 0, 0), false);
		// Conventional NPC melee (vanilla mob, default threshold): negated, and it is not an Instinct dodge.
		helper.assertTrue(ElementalReflex.negates(player, sources.mobAttack(husk)), "ordinary melee passes through smoke");
		// A Grand Line bandit above the player's level: below its threshold, so it lands.
		var bandit = com.grandline.entity.GrandLineEntities.typeOf(com.grandline.entity.EnemyKind.BANDIT).create(helper.getLevel());
		int lvl = PlayerProfileService.get(player).level();
		bandit.moveTo(player.getX() + 2, player.getY(), player.getZ());
		helper.getLevel().addFreshEntity(bandit);
		bandit.setGrandLineLevel(lvl + 20); // after spawning (spawning assigns the island's level)
		eq(helper, ElementalReflex.classify(player, sources.mobAttack(bandit)), ElementalReflex.Category.NPC_BELOW_THRESHOLD, "level threshold (player " + PlayerProfileService.get(player).level() + ", bandit "
				+ bandit.grandLineLevel() + ", rule " + ElementalReflex.rule(bandit) + ", type " + net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(bandit.getType()) + ")");
		helper.assertFalse(ElementalReflex.negates(player, sources.mobAttack(bandit)), "high-level bandit still hits");
		bandit.setGrandLineLevel(Math.max(1, lvl - 10));
		helper.assertTrue(ElementalReflex.negates(player, sources.mobAttack(bandit)), "lower-level bandit is negated");
		bandit.discard();
		// Environment, falls and drowning always land.
		helper.assertFalse(ElementalReflex.negates(player, sources.fall()), "fall damage bypasses");
		helper.assertFalse(ElementalReflex.negates(player, sources.drown()), "water bypasses");
		helper.assertFalse(ElementalReflex.negates(player, sources.lava()), "environment bypasses");
		// Bosses bypass.
		var boss = com.grandline.entity.GrandLineEntities.SABER_BOSS.create(helper.getLevel());
		eq(helper, ElementalReflex.classify(player, sources.mobAttack(boss)), ElementalReflex.Category.BOSS, "boss");
		// Natural fruits never inherit it; replacing Smoke removes it at once.
		PlayerProfileService.update(player, p -> p.withPowers(p.powers().withFruit(Fruits.BOMB.id())));
		helper.assertFalse(ElementalReflex.negates(player, sources.mobAttack(husk)), "Bomb (Natural) has no reflex");
		PlayerProfileService.update(player, p -> p.withPowers(p.powers().withFruit(Fruits.SMOKE.id())));
		helper.assertTrue(ElementalReflex.negates(player, sources.mobAttack(husk)), "back with Smoke");
		// Blade keeps its own sword passive (unaffected).
		helper.assertTrue(ElementalReflex.hasReflex(player), "Smoke registered");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "smoke_3", timeoutTicks = 40)
	public void smokeSlamLaunches(GameTestHelper helper) {
		ServerPlayer player = smokeUser(helper, 1);
		Zombie z = dummy(helper, player.position().add(5.5, 0, 0), false);
		z.setNoGravity(false);
		double m = mult(player);
		eq(helper, use(player, 0), Outcome.USED, "Smoke Slam");
		helper.runAfterDelay(SmokeMoves.SLAM_HIT + 2, () -> helper.assertTrue(z.getDeltaMovement().y > 0.3, "launched up: " + z.getDeltaMovement()));
		helper.runAfterDelay(20, () -> {
			helper.assertTrue(lost(z) >= SmokeMoves.REF * 0.6 * m && lost(z) <= SmokeMoves.REF * m + 0.05, "Smoke Slam damage: " + lost(z));
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "smoke_4", timeoutTicks = 100)
	public void smokeBlastCloudHitsRepeatedlyAndCanBeEscaped(GameTestHelper helper) {
		ServerPlayer player = smokeUser(helper, 20);
		Zombie stay = dummy(helper, player.position().add(8, 0, 0), false);
		Zombie leave = dummy(helper, player.position().add(8, 0, 2.5), false);
		double m = mult(player);
		eq(helper, use(player, 1), Outcome.USED, "Smoke Blast");
		helper.runAfterDelay(16, () -> leave.moveTo(leave.getX(), leave.getY(), leave.getZ() + 12));
		helper.runAfterDelay(70, () -> {
			near(helper, lost(stay), SmokeMoves.REF * 2.0 * m, "staying inside: 5 slashes = 200%");
			helper.assertTrue(lost(leave) > 0 && lost(leave) < lost(stay) - 1, "escaped early: " + lost(leave));
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "smoke_5", timeoutTicks = 80)
	public void smokeLiberationHitsThreeTimesAndStuns(GameTestHelper helper) {
		ServerPlayer player = smokeUser(helper, 50);
		Zombie z = dummy(helper, player.position().add(3.5, 0, 0), false);
		double m = mult(player);
		eq(helper, use(player, 2), Outcome.USED, "Smoke Liberation");
		helper.runAfterDelay(6, () -> helper.assertTrue(z.hasEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN), "stunned"));
		helper.runAfterDelay(45, () -> {
			near(helper, lost(z), SmokeMoves.REF * 1.5 * m, "3 × 50% = 150%");
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "smoke_6", timeoutTicks = 120)
	public void smokeBomberFliesBombsAndIsInterruptedOnlyByRealDamage(GameTestHelper helper) {
		ServerPlayer player = smokeUser(helper, 10);
		Zombie z = dummy(helper, player.position().add(1.2, 0, 0), false);
		eq(helper, use(player, 4), Outcome.USED, "Smoke Bomber");
		double[] energy = new double[1];
		helper.runAfterDelay(4, () -> {
			helper.assertTrue(FlightService.isFlying(player), "flying");
			energy[0] = EnergyService.current(player);
		});
		helper.runAfterDelay(50, () -> {
			helper.assertTrue(FlightService.isFlying(player), "still flying: no time limit");
			helper.assertTrue(EnergyService.current(player) >= energy[0] - 0.01, "no energy drain");
			helper.assertTrue(lost(z) > 0, "bombs dropped and hit: " + lost(z));
			// A negated hit does not interrupt.
			ElementalReflex.negates(player, helper.getLevel().damageSources().mobAttack(z));
		});
		helper.runAfterDelay(53, () -> {
			helper.assertTrue(FlightService.isFlying(player), "negated hit did not interrupt");
			player.setHealth(player.getHealth() - 2); // real damage
		});
		helper.runAfterDelay(57, () -> {
			helper.assertFalse(FlightService.isFlying(player), "real damage interrupts");
			helper.assertTrue(AbilityCooldownService.isOnCooldown(player, Fruits.SMOKE.power().abilityId(Fruits.SMOKE.power().abilities().get(4))),
					"cooldown started");
			AbilityCooldownService.clear(player);
			eq(helper, use(player, 4), Outcome.USED, "Smoke Bomber again");
		});
		helper.runAfterDelay(65, () -> AbilityActivationHandler.releaseCharge(player, 4));
		helper.runAfterDelay(68, () -> {
			helper.assertFalse(FlightService.isFlying(player), "releasing F ends flight");
			helper.succeed();
		});
	}
}
