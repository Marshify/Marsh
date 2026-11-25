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
import com.grandline.fruit.spike.SpikeMoves;
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

/** Spike Fruit rework: the four moves through the real activation pipeline. */
public final class GrandLineSpikeGameTests implements FabricGameTest {
	/** A Spike user standing on a stone floor at the test's base (so ground-dependent moves behave as in play). */
	private static ServerPlayer spikeUser(GameTestHelper helper, int mastery) {
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
		PlayerProfileService.update(player, p -> p.withPowers(p.powers().withFruit(Fruits.SPIKE.id())));
		if (mastery > 1) {
			PlayerProfileService.setMasteryLevel(player, Fruits.SPIKE.power(), mastery);
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

	private static Zombie grounded(GameTestHelper helper, Vec3 at) {
		Zombie z = dummy(helper, at, false);
		z.setNoGravity(false);
		return z;
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "spike_1")
	public void spikeKeepsIdentityAndHasItsMoves(GameTestHelper helper) {
		eq(helper, Fruits.SPIKE.id(), "grandline:spike", "power id kept");
		eq(helper, Fruits.SPIKE.rarity(), FruitRarity.COMMON, "rarity kept");
		eq(helper, FruitService.price(Fruits.SPIKE), 180_000L, "dealer price");
		List<AbilityDef> all = Fruits.SPIKE.power().abilities();
		eq(helper, all.get(4).isEmpty(), true, "no F move");
		List<AbilityDef> moves = all.stream().filter(a -> !a.isEmpty()).toList();
		eq(helper, moves.stream().map(AbilityDef::key).toList(), List.of("spike_summon", "spike_whirlwind", "spike_ball", "spike_barrage"), "Z X C V");
		eq(helper, moves.stream().map(AbilityDef::requiredMastery).toList(), List.of(1, 25, 50, 75), "mastery");
		eq(helper, moves.stream().map(AbilityDef::cooldownTicks).toList(), List.of(120, 210, 270, 240), "cooldowns");
		eq(helper, moves.stream().map(AbilityDef::energyCost).toList(), List.of(0.2, 0.35, 0.5, 0.4), "energy");
		helper.assertFalse(com.grandline.combat.ability.FruitStrikes.has("grandline:spike"), "no M1");
		ServerPlayer player = spikeUser(helper, 1);
		helper.assertFalse(com.grandline.combat.ElementalReflex.hasReflex(player), "Natural: no Elemental Reflex");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "spike_2", timeoutTicks = 40)
	public void spikeSummonTapLaunchesTarget(GameTestHelper helper) {
		ServerPlayer player = spikeUser(helper, 1);
		Zombie z = grounded(helper, player.position().add(6, 0, 0));
		player.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, z.position());
		double m = mult(player);
		eq(helper, use(player, 0), Outcome.USED, "Spike Summon");
		helper.runAfterDelay(1, () -> AbilityActivationHandler.releaseCharge(player, 0));
		helper.runAfterDelay(4, () -> helper.assertTrue(z.getDeltaMovement().y > 0.3, "launched: " + z.getDeltaMovement()));
		helper.runAfterDelay(15, () -> {
			near(helper, lost(z), SpikeMoves.REF * m, "central spike 100%");
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "spike_3", timeoutTicks = 60)
	public void spikeSummonFullChargeHitsHarder(GameTestHelper helper) {
		ServerPlayer player = spikeUser(helper, 1);
		Zombie z = grounded(helper, player.position().add(6, 0, 0));
		player.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, z.position());
		double m = mult(player);
		eq(helper, use(player, 0), Outcome.USED, "Spike Summon (held)");
		helper.runAfterDelay(40, () -> {
			helper.assertTrue(lost(z) >= SpikeMoves.REF * m - 0.05 && lost(z) <= SpikeMoves.REF * 1.3 * m + 0.6, "charged: " + lost(z));
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "spike_4", timeoutTicks = 100)
	public void whirlwindTapAndHeldRadius(GameTestHelper helper) {
		ServerPlayer player = spikeUser(helper, 25);
		Zombie near = dummy(helper, player.position().add(1.8, 0, 0), false);
		Zombie far = dummy(helper, player.position().add(0, 0, 4.0), false);
		double m = mult(player);
		eq(helper, use(player, 1), Outcome.USED, "Whirlwind (held)");
		helper.runAfterDelay(26, () -> helper.assertTrue(lost(far) == 0, "outside the tapped radius early"));
		helper.runAfterDelay(80, () -> {
			helper.assertTrue(lost(far) > 0, "the held radius grows to reach 4 blocks: " + lost(far));
			near(helper, lost(near), SpikeMoves.REF * 1.5 * m, "10 rotations = 150%");
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "spike_5", timeoutTicks = 60)
	public void whirlwindTapLastsOneAndAHalfSeconds(GameTestHelper helper) {
		ServerPlayer player = spikeUser(helper, 25);
		Zombie near = dummy(helper, player.position().add(1.8, 0, 0), false);
		double m = mult(player);
		eq(helper, use(player, 1), Outcome.USED, "Whirlwind (tap)");
		helper.runAfterDelay(1, () -> AbilityActivationHandler.releaseCharge(player, 1));
		helper.runAfterDelay(50, () -> {
			near(helper, lost(near), SpikeMoves.REF * 1.05 * m, "5 rotations = 105%");
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "spike_6", timeoutTicks = 100)
	public void spikyBallRollsHitsAndEnds(GameTestHelper helper) {
		ServerPlayer player = spikeUser(helper, 50);
		Zombie z = grounded(helper, player.position().add(4, 0, 0));
		double m = mult(player);
		eq(helper, use(player, 2), Outcome.USED, "Spiky Ball");
		helper.runAfterDelay(5, () -> helper.assertTrue(SpikeMoves.rolling(player), "rolling"));
		helper.runAfterDelay(SpikeMoves.BALL_TICKS + 5, () -> {
			helper.assertFalse(SpikeMoves.rolling(player), "transformation ended");
			float got = lost(z);
			helper.assertTrue(got >= SpikeMoves.REF * 0.45 * m - 0.05 && got <= SpikeMoves.REF * 1.45 * m + 0.05, "contact + drag hits: " + got);
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "spike_7", timeoutTicks = 60)
	public void spikeBarrageTapFourSequentialSpikes(GameTestHelper helper) {
		ServerPlayer player = spikeUser(helper, 75);
		double step = SpikeMoves.BARRAGE_RANGE_TAP / SpikeMoves.BARRAGE_TAP_COUNT;
		Zombie second = grounded(helper, player.position().add(step * 0.7 + step, 0, 0));
		Zombie beyond = grounded(helper, player.position().add(step * 0.7 + step * 4 + 1, 0, 0));
		double m = mult(player);
		eq(helper, use(player, 3), Outcome.USED, "Spike Barrage (tap)");
		helper.runAfterDelay(1, () -> AbilityActivationHandler.releaseCharge(player, 3));
		helper.runAfterDelay(30, () -> {
			near(helper, lost(second), SpikeMoves.REF * (0.35 + 0.4 / 3) * m, "second spike core");
			eq(helper, lost(beyond), 0f, "nothing past the four spikes");
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "spike_8", timeoutTicks = 120)
	public void spikeBarrageFullChargeReachesFinalSpike(GameTestHelper helper) {
		ServerPlayer player = spikeUser(helper, 75);
		double step = SpikeMoves.BARRAGE_RANGE_FULL / SpikeMoves.BARRAGE_FULL_COUNT;
		Zombie last = grounded(helper, player.position().add(step * 0.7 + step * 6, 0, 0));
		double m = mult(player);
		eq(helper, use(player, 3), Outcome.USED, "Spike Barrage (held)");
		helper.runAfterDelay(100, () -> {
			near(helper, lost(last), SpikeMoves.REF * 0.95 * m, "seventh (final, largest) spike core");
			helper.succeed();
		});
	}
}
