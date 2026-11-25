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
import com.grandline.fruit.bomb.BombMoves;
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

/** Bomb Fruit rework: M1 and the five moves through the real activation pipeline. */
public final class GrandLineBombGameTests implements FabricGameTest {
	/** A Bomb user standing on a stone floor at the test's base (so ground-dependent moves behave as in play). */
	private static ServerPlayer bombUser(GameTestHelper helper, int mastery) {
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
		PlayerProfileService.update(player, p -> p.withPowers(p.powers().withFruit(Fruits.BOMB.id())));
		if (mastery > 1) {
			PlayerProfileService.setMasteryLevel(player, Fruits.BOMB.power(), mastery);
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

	@GameTest(template = EMPTY_STRUCTURE, batch = "bomb_1")
	public void bombKeepsIdentityAndHasItsMoves(GameTestHelper helper) {
		eq(helper, Fruits.BOMB.id(), "grandline:bomb", "power id kept");
		eq(helper, Fruits.BOMB.rarity(), FruitRarity.COMMON, "rarity kept");
		eq(helper, FruitService.price(Fruits.BOMB), 80_000L, "dealer price");
		List<AbilityDef> moves = Fruits.BOMB.power().abilities();
		eq(helper, moves.stream().map(AbilityDef::key).toList(), List.of("bomb_targeted", "bomb_grab", "bomb_land_mines", "bomb_self_destruct", "bomb_jump"), "Z X C V F");
		eq(helper, moves.stream().map(AbilityDef::requiredMastery).toList(), List.of(1, 50, 75, 125, 25), "mastery");
		eq(helper, moves.stream().map(AbilityDef::cooldownTicks).toList(), List.of(90, 180, 220, 450, 120), "cooldowns");
		eq(helper, moves.stream().map(AbilityDef::energyCost).toList(), List.of(0.15, 0.25, 0.5, 1.0, 0.25), "energy");
		eq(helper, BombMoves.TOSS.cooldownTicks(), 60, "M1 cooldown");
		eq(helper, BombMoves.TOSS.energyCost(), 0.05, "M1 energy");
		helper.assertTrue(FruitStrikes.held("grandline:bomb"), "Blast Toss is a held M1");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "bomb_2", timeoutTicks = 80)
	public void blastTossHitsAndRespectsCooldown(GameTestHelper helper) {
		ServerPlayer player = bombUser(helper, 1);
		Zombie z = dummy(helper, player.position().add(6, 0, 0), false);
		player.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, z.getBoundingBox().getCenter());
		double m = mult(player);
		helper.assertTrue(FruitStrikes.swing(player) == 0, "press starts the hold");
		helper.runAfterDelay(1, () -> FruitStrikes.release(player));
		helper.runAfterDelay(3, () -> helper.assertTrue(FruitStrikes.swing(player) == -1, "cooldown blocks a second toss"));
		helper.runAfterDelay(30, () -> {
			near(helper, lost(z), BombMoves.REF * 0.45 * m, "tapped Blast Toss (direct hit, once)");
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "bomb_3", timeoutTicks = 60)
	public void targetedBombLaunchesUp(GameTestHelper helper) {
		ServerPlayer player = bombUser(helper, 1);
		Zombie z = dummy(helper, player.position().add(7, 0, 0), false);
		z.setNoGravity(false);
		player.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, z.position());
		double m = mult(player);
		eq(helper, use(player, 0), Outcome.USED, "Targeted Bomb");
		helper.runAfterDelay(6, () -> AbilityActivationHandler.releaseCharge(player, 0));
		helper.runAfterDelay(9, () -> helper.assertTrue(z.getDeltaMovement().y > 0.3, "launched upward: " + z.getDeltaMovement()));
		helper.runAfterDelay(20, () -> {
			near(helper, lost(z), BombMoves.REF * m, "Targeted Bomb centre damage");
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "bomb_4", timeoutTicks = 120)
	public void bombGrabAttachesAndDetonatesAfterThreeSeconds(GameTestHelper helper) {
		ServerPlayer player = bombUser(helper, 50);
		Zombie z = dummy(helper, player.position().add(5, 0, 0), false);
		double m = mult(player);
		eq(helper, use(player, 1), Outcome.USED, "Bomb Grab");
		helper.runAfterDelay(12, () -> {
			near(helper, lost(z), BombMoves.REF * 0.35 * m, "grab blast");
			helper.assertTrue(AttachedCharges.carries(z, "bomb_grab"), "bomb attached");
		});
		helper.runAfterDelay(60, () -> helper.assertTrue(AttachedCharges.carries(z, "bomb_grab"), "still ticking before 3 s"));
		helper.runAfterDelay(90, () -> {
			near(helper, lost(z), BombMoves.REF * 1.15 * m, "grab + delayed blast = 115%");
			helper.assertFalse(AttachedCharges.carries(z, "bomb_grab"), "detonated once and gone");
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "bomb_5", timeoutTicks = 200)
	public void landMinesRingAndMinefieldChain(GameTestHelper helper) {
		ServerPlayer player = bombUser(helper, 75);
		Zombie z = dummy(helper, player.position().add(BombMoves.RING_RADIUS, 0, 0), false);
		double m = mult(player);
		eq(helper, use(player, 2), Outcome.USED, "Land Mines (tap)");
		helper.runAfterDelay(1, () -> AbilityActivationHandler.releaseCharge(player, 2));
		helper.runAfterDelay(35, () -> {
			float got = lost(z);
			helper.assertTrue(got >= BombMoves.REF * 0.3125 * m * 0.8 && got <= BombMoves.REF * 2.5 * m + 0.05, "ring hits independently: " + got);
			eq(helper, Traps.owned(player.getUUID(), "bomb_mine").size(), 0, "tap leaves no mines");
			com.grandline.combat.ability.AbilityCooldownService.clear(player);
			eq(helper, use(player, 2), Outcome.USED, "Land Mines (hold)");
		});
		helper.runAfterDelay(70, () -> AbilityActivationHandler.releaseCharge(player, 2));
		helper.runAfterDelay(80, () -> {
			int n = Traps.owned(player.getUUID(), "bomb_mine").size();
			helper.assertTrue(n >= BombMoves.FIELD_MIN && n <= BombMoves.FIELD_MAX, "minefield placed: " + n);
			helper.assertTrue(Traps.detonateOwned(player, "bomb_mine") == n, "owner detonation schedules every mine once");
			helper.assertTrue(Traps.detonateOwned(player, "bomb_mine") == 0, "no mine is scheduled twice");
		});
		helper.runAfterDelay(140, () -> {
			eq(helper, Traps.owned(player.getUUID(), "bomb_mine").size(), 0, "all mines gone after the chain");
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "bomb_6", timeoutTicks = 140)
	public void selfDestructCostsMaxHealthAndScales(GameTestHelper helper) {
		ServerPlayer player = bombUser(helper, 125);
		player.setHealth(player.getMaxHealth());
		Zombie z = dummy(helper, player.position().add(2.5, 0, 0), false);
		double m = mult(player);
		float max = player.getMaxHealth();
		eq(helper, use(player, 3), Outcome.USED, "Self Destruct");
		helper.runAfterDelay(BombMoves.SELF_MAX + 10, () -> {
			near(helper, BombMoves.LAST_SACRIFICE.get(player.getUUID()), max * 0.25, "full charge consumed 25% of max health");
			helper.assertTrue(player.getHealth() <= max * 0.75 + 1.01, "health was really taken: " + player.getHealth());
			helper.assertTrue(lost(z) >= BombMoves.REF * 2.2 * m * 0.6, "full-charge blast landed: " + lost(z));
			helper.assertTrue(player.isAlive(), "never kills");
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "bomb_7", timeoutTicks = 40)
	public void explosiveJumpLaunchesAndEscalates(GameTestHelper helper) {
		ServerPlayer player = bombUser(helper, 25);
		player.setHealth(player.getMaxHealth());
		BombMoves.JUMP_PENALTY.reset(player);
		eq(helper, use(player, 4), Outcome.USED, "Explosive Jump");
		helper.assertTrue(player.getDeltaMovement().y > 1.0, "launched up: " + player.getDeltaMovement());
		near(helper, player.getHealth(), player.getMaxHealth(), "first use is free");
		helper.assertTrue(MovementGuards.guarded(player), "fall protection while airborne");
		near(helper, BombMoves.JUMP_PENALTY.nextCost(player), 0.06, "next use would cost 6%");
		near(helper, BombMoves.JUMP_PENALTY.use(player), 0.06, "second use 6%");
		near(helper, BombMoves.JUMP_PENALTY.use(player), 0.12, "third use 12%");
		BombMoves.JUMP_PENALTY.reset(player);
		near(helper, BombMoves.JUMP_PENALTY.nextCost(player), 0, "recovered");
		helper.succeed();
	}
}
