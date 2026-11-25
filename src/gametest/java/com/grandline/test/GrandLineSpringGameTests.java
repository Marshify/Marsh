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
import com.grandline.fruit.spring.SpringMoves;
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

/** Spring Fruit rework: the five moves through the real activation pipeline. */
public final class GrandLineSpringGameTests implements FabricGameTest {
	/** A Spring user standing on a stone floor at the test's base (so ground-dependent moves behave as in play). */
	private static ServerPlayer springUser(GameTestHelper helper, int mastery) {
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
		PlayerProfileService.update(player, p -> p.withPowers(p.powers().withFruit(Fruits.SPRING.id())));
		if (mastery > 1) {
			PlayerProfileService.setMasteryLevel(player, Fruits.SPRING.power(), mastery);
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

	@GameTest(template = EMPTY_STRUCTURE, batch = "spring_1")
	public void springKeepsIdentityAndHasItsFiveMoves(GameTestHelper helper) {
		eq(helper, Fruits.SPRING.id(), "grandline:spring", "power id kept");
		eq(helper, Fruits.SPRING.rarity(), FruitRarity.COMMON, "rarity kept");
		eq(helper, FruitService.price(Fruits.SPRING), 60_000L, "dealer price");
		List<AbilityDef> moves = Fruits.SPRING.power().abilities();
		eq(helper, moves.stream().map(AbilityDef::key).toList(), List.of("spring_knock", "spring_snipe", "spring_cannon", "spring_emperor", "spring_leap"), "Z X C V F");
		eq(helper, moves.stream().map(AbilityDef::requiredMastery).toList(), List.of(1, 20, 50, 125, 75), "mastery");
		eq(helper, moves.stream().map(AbilityDef::cooldownTicks).toList(), List.of(120, 180, 270, 390, 120), "cooldowns");
		eq(helper, moves.stream().map(AbilityDef::energyCost).toList(), List.of(0.15, 0.25, 0.35, 0.65, 0.35), "energy");
		// No defensive passive: swords cut a Spring user like anyone else.
		ServerPlayer player = springUser(helper, 1);
		Zombie swordsman = dummy(helper, player.position().add(1, 0, 0), false);
		swordsman.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
		helper.assertFalse(DefensivePassives.blocked(player, player.damageSources().mobAttack(swordsman)), "no sword immunity");
		helper.assertFalse(MovementGuards.guarded(player), "no fall protection outside a move");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "spring_2", timeoutTicks = 40)
	public void knockHitsEveryoneOnTheLineOnceAndStopsAtWalls(GameTestHelper helper) {
		ServerPlayer player = springUser(helper, 1);
		Zombie near = dummy(helper, player.position().add(3, 0, 0), false);
		Zombie far = dummy(helper, player.position().add(5, 0, 0), false);
		wall(helper, player, 6);
		Zombie behindWall = dummy(helper, player.position().add(7.5, 0, 0), false);
		eq(helper, use(player, 0), Outcome.USED, "Knock");
		helper.runAfterDelay(SpringMoves.KNOCK_HIT_TICK + 3, () -> {
			near(helper, lost(near), SpringMoves.REF * 0.80 * mult(player), "80% on the first enemy");
			near(helper, lost(far), SpringMoves.REF * 0.80 * mult(player), "and on the next one on the line");
			eq(helper, lost(behindWall), 0f, "nothing behind the wall");
			helper.assertTrue(near.getDeltaMovement().x > 0.3, "knocked back along the punch");
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "spring_3", timeoutTicks = 60)
	public void springSnipeTravelsHitsAndStopsAtWalls(GameTestHelper helper) {
		ServerPlayer player = springUser(helper, 20);
		Vec3 start = player.position();
		Zombie target = dummy(helper, start.add(5, 0, 0), false);
		wall(helper, player, 12);
		eq(helper, use(player, 1), Outcome.USED, "Spring Snipe");
		helper.assertTrue(MovementGuards.guarded(player), "fall-protected while moving");
		helper.runAfterDelay(SpringMoves.SNIPE_WINDUP + 10, () -> {
			helper.assertTrue(player.getX() > start.x + 8, "travelled (" + (player.getX() - start.x) + ")");
			helper.assertTrue(player.getX() + 0.3 <= start.x + 12 + 1e-6, "stopped before the wall");
			near(helper, lost(target), SpringMoves.REF * 1.10 * mult(player), "110% to what it hit");
			helper.assertFalse(SpringMoves.moving(player), "the move ended");
		});
		helper.runAfterDelay(SpringMoves.SNIPE_WINDUP + 10 + MovementGuards.GRACE + 3, () -> {
			helper.assertFalse(MovementGuards.guarded(player), "fall protection cleared after landing");
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "spring_4", timeoutTicks = 60)
	public void springCannonRisesSlamsAndLaunchesUp(GameTestHelper helper) {
		ServerPlayer player = springUser(helper, 50);
		Vec3 start = player.position();
		Zombie core = dummy(helper, start.add(1.5, 0, 0), false);
		Zombie rim = dummy(helper, start.add(4, 0, 0), false);
		Zombie outside = dummy(helper, start.add(8, 0, 0), false);
		double[] peak = {0};
		eq(helper, use(player, 2), Outcome.USED, "Spring Cannon");
		for (int t = 1; t < 15; t++) {
			helper.runAfterDelay(t, () -> peak[0] = Math.max(peak[0], player.getY() - start.y));
		}
		int impact = SpringMoves.CANNON_WINDUP + SpringMoves.CANNON_RISE + SpringMoves.CANNON_HOLD + SpringMoves.CANNON_SLAM + 2;
		helper.runAfterDelay(impact + 1, () -> {
			helper.assertTrue(peak[0] > 5, "rose high on the springs (" + peak[0] + ")");
			helper.assertTrue(Math.abs(player.getY() - start.y) < 0.2, "back on the ground it left");
			near(helper, lost(core), SpringMoves.REF * 1.30 * mult(player), "130% in the core");
			helper.assertTrue(lost(rim) > 0 && lost(rim) < lost(core), "less at the rim");
			eq(helper, lost(outside), 0f, "nothing outside the impact");
			helper.assertTrue(core.getDeltaMovement().y > 0.5, "launched up (" + core.getDeltaMovement() + ")");
			helper.assertTrue(Math.abs(core.getDeltaMovement().x) < 0.2, "barely sideways (" + core.getDeltaMovement() + ")");
			helper.assertTrue(helper.getLevel().getBlockState(BlockPos.containing(start.x, start.y - 1, start.z)).is(Blocks.STONE), "terrain untouched");
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "spring_5", timeoutTicks = 100)
	public void springEmperorOnlyHitsThroughCollisionsWithinItsCaps(GameTestHelper helper) {
		ServerPlayer player = springUser(helper, 125);
		Vec3 start = player.position();
		Zombie a = dummy(helper, start.add(4, 0, 3), true);
		Zombie b = dummy(helper, start.add(6, 0, -5), true);
		Zombie c = dummy(helper, start.add(-5, 0, 4), true);
		Zombie outside = dummy(helper, start.add(22, 0, 0), true);
		eq(helper, use(player, 3), Outcome.USED, "Spring Emperor at mastery 125");
		helper.runAfterDelay(SpringMoves.EMPEROR_TICKS + 4, () -> {
			double cap = SpringMoves.REF * 0.40 * SpringMoves.EMPEROR_MAX_HITS * mult(player) + 0.05;
			for (Zombie z : List.of(a, b, c)) {
				helper.assertTrue(lost(z) <= cap, "never more than 160% on one target (" + lost(z) + ")");
				double hits = lost(z) / (SpringMoves.REF * 0.40 * mult(player));
				helper.assertTrue(Math.abs(hits - Math.round(hits)) < 0.02, "damage only in whole collisions (" + hits + ")");
			}
			eq(helper, lost(outside), 0f, "nothing outside the square");
			Vec3 end = player.position();
			helper.assertTrue(Math.abs(end.x - start.x) <= SpringMoves.EMPEROR_HALF + 1 && Math.abs(end.z - start.z) <= SpringMoves.EMPEROR_HALF + 1,
					"stayed inside its square");
			helper.assertFalse(SpringMoves.moving(player), "ended on its own");
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "spring_6", timeoutTicks = 120)
	public void springLeapReboundsOffWallsGainingSpeedWithoutDamage(GameTestHelper helper) {
		ServerPlayer player = springUser(helper, 75);
		wall(helper, player, 5);
		Zombie inPath = dummy(helper, player.position().add(2.5, 1.5, 0), false);
		double[] fastest = {0};
		eq(helper, use(player, 4), Outcome.USED, "Spring Leap at mastery 75");
		for (int t = 1; t < 40; t++) {
			helper.runAfterDelay(t, () -> fastest[0] = Math.max(fastest[0], SpringMoves.leapSpeed(player)));
		}
		helper.runAfterDelay(12, () -> helper.assertTrue(SpringMoves.leapRebounds(player) >= 1, "rebounded off the wall"));
		helper.runAfterDelay(40, () -> {
			helper.assertTrue(fastest[0] > SpringMoves.LEAP_START_SPEED * 0.99, "a wall rebound adds speed (" + fastest[0] + ")");
			helper.assertTrue(fastest[0] <= SpringMoves.LEAP_MAX_SPEED + 1e-6, "never past the cap");
			eq(helper, lost(inPath), 0f, "Spring Leap deals no damage");
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "spring_7", timeoutTicks = 60)
	public void springLeapEndsInWaterAndLocksRespectMastery(GameTestHelper helper) {
		ServerPlayer player = springUser(helper, 75);
		eq(helper, use(player, 3), Outcome.LOCKED, "Spring Emperor needs 125");
		helper.runAfterDelay(com.grandline.GrandLineMod.config().requestCooldownTicks + 1, () -> {
			eq(helper, use(player, 4), Outcome.USED, "Spring Leap");
		});
		helper.runAfterDelay(com.grandline.GrandLineMod.config().requestCooldownTicks + 4, () -> {
			BlockPos at = BlockPos.containing(player.position());
			helper.getLevel().setBlockAndUpdate(at, Blocks.WATER.defaultBlockState());
			player.teleportTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
		});
		helper.runAfterDelay(com.grandline.GrandLineMod.config().requestCooldownTicks + 8, () -> {
			helper.assertFalse(SpringMoves.moving(player), "water ended Spring Leap");
			helper.succeed();
		});
	}
}
