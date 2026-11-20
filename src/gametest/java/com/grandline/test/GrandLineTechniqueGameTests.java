package com.grandline.test;

import com.grandline.GrandLineMod;
import com.grandline.combat.EnergyService;
import com.grandline.combat.ability.AbilityContext;
import com.grandline.combat.ability.AbilityDef;
import com.grandline.combat.ability.Power;
import com.grandline.entity.Islander;
import com.grandline.gear.Gear;
import com.grandline.gear.sword.Stuns;
import com.grandline.npc.KeeperNpcs;
import com.grandline.player.PlayerProfileService;
import com.grandline.technique.Dash;
import com.grandline.technique.FlashStep;
import com.grandline.technique.TechniqueRules;
import com.grandline.technique.TechniqueService;
import com.grandline.technique.TechniqueTrainers;
import com.grandline.technique.Techniques;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Dash, Flash Step, Aura, Instinct and their trainers. */
public final class GrandLineTechniqueGameTests implements FabricGameTest {
	private static ServerPlayer player(GameTestHelper helper) {
		ServerPlayer p = helper.makeMockServerPlayerInLevel();
		p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL); // test players start in creative, which ignores damage
		((com.grandline.mixin.ServerPlayerSpawnAccessor) p).grandline$setSpawnInvulnerableTime(0);
		GrandLineRaceGameTests.neutral(p);
		p.getInventory().clearContent();
		TechniqueService.reset(p);
		Vec3 home = helper.absoluteVec(new Vec3(1.5, 1, 1.5));
		p.moveTo(home.x, home.y, home.z, 0, 0);
		EnergyService.refill(p);
		return p;
	}

	private static void near(GameTestHelper helper, double a, double b, String what) {
		helper.assertTrue(Math.abs(a - b) < 1e-3, what + ": " + a + " vs " + b);
	}

	// ------------------------------------------------------------------------------------------------ Dash

	@GameTest(template = EMPTY_STRUCTURE, batch = "tq_dash")
	public void dashFollowsMovementKeys(GameTestHelper helper) {
		// Facing south (yaw 0, +Z).
		Vec3 fwd = Dash.direction(0, 1, 0), back = Dash.direction(0, -1, 0), left = Dash.direction(0, 0, 1), right = Dash.direction(0, 0, -1);
		near(helper, fwd.z, 1, "W dashes forward");
		near(helper, back.z, -1, "S dashes back");
		near(helper, left.x, 1, "A dashes left (east when facing south)");
		near(helper, right.x, -1, "D dashes right");
		Vec3 diag = Dash.direction(0, 1, 1);
		near(helper, diag.length(), 1, "diagonals are no longer than straight dashes");
		near(helper, diag.x, diag.z, "W+A goes half-way between");
		near(helper, Dash.direction(90, 0, 0).x, -1, "nothing held: forward (facing west)");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "tq_dash")
	public void dashCostsEnergyHasACooldownAndNeedsFreedom(GameTestHelper helper) {
		ServerPlayer p = player(helper);
		double full = EnergyService.current(p);
		helper.assertTrue(TechniqueService.handle(p, TechniqueService.DASH, 1, 0).equals("ok"), "dash works for everyone from level 1");
		near(helper, full - EnergyService.current(p), EnergyService.cost(p, GrandLineMod.config().dashEnergy / 100.0), "dash spends its energy");
		helper.assertTrue(TechniqueService.handle(p, TechniqueService.DASH, 1, 0).equals("cooldown"), "a second dash waits for the cooldown");
		helper.runAfterDelay(GrandLineMod.config().dashCooldownTicks + 1, () -> {
			Stuns.apply(p, 40);
			helper.assertTrue(TechniqueService.handle(p, TechniqueService.DASH, 1, 0).equals("immobilized"), "no dash while stunned");
			p.discard();
			helper.succeed();
		});
	}

	// ------------------------------------------------------------------------------------------------ Flash Step

	@GameTest(template = EMPTY_STRUCTURE, batch = "tq_flash", timeoutTicks = 60)
	public void flashStepStopsAtWallsAndCostsNothingWhenBlocked(GameTestHelper helper) {
		ServerPlayer p = player(helper);
		helper.assertTrue(TechniqueService.handle(p, TechniqueService.FLASH_STEP, 0, 0).equals("not_owned"), "Flash Step must be bought");
		TechniqueService.grant(p, "flash_step");
		// A wall 5 blocks ahead (south): the destination stops in front of it.
		for (int y = 1; y <= 4; y++) {
			for (int x = -1; x <= 3; x++) {
				helper.setBlock(new BlockPos(x, y, 7), Blocks.STONE);
			}
		}
		Vec3 to = FlashStep.destination(p, 20);
		helper.assertTrue(to != null && to.z < helper.absoluteVec(new Vec3(1.5, 1, 7)).z - 0.29, "stops before the wall: " + to);
		double energy = EnergyService.current(p);
		Vec3 before = p.position();
		helper.assertTrue(TechniqueService.handle(p, TechniqueService.FLASH_STEP, 0, 0).equals("ok"), "steps");
		helper.assertTrue(p.position().distanceTo(before) > 3, "moved instantly: " + before + " -> " + p.position());
		helper.assertTrue(EnergyService.current(p) < energy, "spent energy");
		helper.assertTrue(TechniqueService.handle(p, TechniqueService.FLASH_STEP, 0, 0).equals("cooldown"), "then waits");
		// Boxed in: nowhere to go, so nothing is spent.
		ServerPlayer q = player(helper);
		TechniqueService.grant(q, "flash_step");
		Vec3 home = helper.absoluteVec(new Vec3(4.5, 1, 1.5));
		q.moveTo(home.x, home.y, home.z, 0, 0);
		for (int y = 1; y <= 3; y++) {
			helper.setBlock(new BlockPos(4, y, 2), Blocks.STONE);
		}
		double qe = EnergyService.current(q);
		helper.assertTrue(TechniqueService.handle(q, TechniqueService.FLASH_STEP, 0, 0).equals("no_destination"), "blocked step is cancelled");
		near(helper, EnergyService.current(q), qe, "a cancelled step costs no energy");
		helper.assertFalse(TechniqueService.handle(q, TechniqueService.FLASH_STEP, 0, 0).equals("cooldown"), "and starts no cooldown");
		p.discard();
		q.discard();
		helper.succeed();
	}

	// ------------------------------------------------------------------------------------------------ Aura

	@GameTest(template = EMPTY_STRUCTURE, batch = "tq_aura")
	public void auraStagesFollowXp(GameTestHelper helper) {
		long[] t = GrandLineMod.config().auraStageXp;
		helper.assertTrue(TechniqueRules.auraStage(0) == 0 && TechniqueRules.auraStage(t[0] - 1) == 0 && TechniqueRules.auraStage(t[0]) == 1
				&& TechniqueRules.auraStage(t[4]) == 5, "stages at 4k/12k/24k/48k/60k");
		near(helper, TechniqueRules.auraReduction(0), 0, "stage 0: no reduction");
		near(helper, TechniqueRules.auraReduction(5), 0.30, "stage 5: 30% reduction");
		near(helper, TechniqueRules.auraDamageBonus(3), 0.10, "10% damage at every stage");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "tq_aura2", timeoutTicks = 60)
	public void auraBoostsPhysicalHitsReducesDamageAndTrains(GameTestHelper helper) {
		ServerPlayer p = player(helper);
		helper.assertTrue(TechniqueService.toggleAura(p).equals("not_owned"), "Aura must be bought");
		TechniqueService.grant(p, "aura");
		IronGolem a = helper.spawn(EntityType.IRON_GOLEM, new BlockPos(1, 2, 4));
		a.setNoAi(true);
		Power sword = Gear.powerFor(Gear.KATANA);
		AbilityDef move = sword.abilities().get(0);
		float before = a.getHealth();
		new AbilityContext(p, sword, move, 1.0).damage(a, 10f);
		float plain = before - a.getHealth();
		TechniqueService.toggleAura(p);
		helper.assertTrue(TechniqueService.auraOn(p), "Aura on");
		a.invulnerableTime = 0;
		before = a.getHealth();
		new AbilityContext(p, sword, move, 1.0).damage(a, 10f);
		float boosted = before - a.getHealth();
		near(helper, boosted, plain * 1.10, "sword hit +10% with Aura");
		helper.assertTrue(TechniqueService.get(p).auraXp() > 0, "the landed hit trained Aura");
		long xp = TechniqueService.get(p).auraXp();
		a.invulnerableTime = 0;
		new AbilityContext(p, sword, move, 1.0).damage(a, 10f);
		helper.assertTrue(TechniqueService.get(p).auraXp() == xp, "hits in the same instant count once");
		// A friendly islander gives nothing.
		Islander npc = helper.spawn(com.grandline.entity.GrandLineEntities.ISLANDER, new BlockPos(3, 2, 4));
		helper.runAfterDelay(GrandLineMod.config().auraXpHitGapTicks + 1, () -> {
			long before2 = TechniqueService.get(p).auraXp();
			new AbilityContext(p, sword, move, 1.0).damage(npc, 4f);
			helper.assertTrue(TechniqueService.get(p).auraXp() == before2, "no XP from friendly NPCs");
			// Incoming damage is reduced by the stage's share (stage 5: 30%).
			TechniqueService.update(p, t -> t.withAuraXp(GrandLineMod.config().auraStageXp[4]));
			Zombie z = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, 3));
			z.setNoAi(true);
			p.setHealth(p.getMaxHealth());
			p.invulnerableTime = 0;
			float hp = p.getHealth();
			p.hurt(p.damageSources().mobAttack(z), 10f);
			near(helper, hp - p.getHealth(), 7f, "stage 5 Aura takes 30% off");
			p.discard();
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "tq_aura3")
	public void auraBreaksElementalImmunity(GameTestHelper helper) {
		ServerPlayer attacker = player(helper);
		ServerPlayer smoke = player(helper);
		PlayerProfileService.update(smoke, pr -> pr.withPowers(pr.powers().withFruit("grandline:smoke")));
		var punch = smoke.damageSources().playerAttack(attacker);
		helper.assertTrue(com.grandline.technique.Dodges.evades(smoke, punch, 4f), "a plain punch passes through an elemental body");
		helper.assertFalse(com.grandline.technique.Dodges.evades(attacker, attacker.damageSources().playerAttack(smoke), 4f),
				"an ordinary body is hit normally");
		TechniqueService.grant(attacker, "aura");
		TechniqueService.toggleAura(attacker);
		helper.assertFalse(com.grandline.technique.Dodges.evades(smoke, punch, 4f), "with Aura the punch lands");
		Power flame = com.grandline.fruit.Fruits.FLAME.power();
		boolean[] fruitHit = new boolean[1];
		TechniqueService.toggleAura(attacker);
		com.grandline.technique.CombatContext.during(flame, flame.abilities().get(0), () -> {
			fruitHit[0] = !com.grandline.technique.Dodges.evades(smoke, punch, 4f);
			return true;
		});
		helper.assertTrue(fruitHit[0], "fruit moves are not physical: elemental bodies do not ignore them");
		attacker.discard();
		smoke.discard();
		helper.succeed();
	}

	// ------------------------------------------------------------------------------------------------ Instinct

	@GameTest(template = EMPTY_STRUCTURE, batch = "tq_instinct")
	public void instinctChargesFollowXp(GameTestHelper helper) {
		int[] want = {2, 3, 4, 5, 6, 7, 8};
		long[] xp = {0, 50, 350, 850, 1400, 2100, 2800};
		for (int i = 0; i < xp.length; i++) {
			helper.assertTrue(TechniqueRules.maxDodges(xp[i]) == want[i], xp[i] + " XP gives " + want[i] + " charges");
		}
		helper.assertTrue(TechniqueRules.maxDodges(49) == 2 && TechniqueRules.instinctNextXp(49) == 50, "next tier at 50");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "tq_instinct2", timeoutTicks = 80)
	public void instinctEvadesUntilChargesRunOut(GameTestHelper helper) {
		ServerPlayer p = player(helper);
		TechniqueService.grant(p, "instinct");
		helper.assertTrue(TechniqueService.get(p).dodges() == 2, "starts with 2 charges");
		TechniqueService.toggleInstinct(p);
		Zombie z = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, 3));
		z.setNoAi(true);
		float hp = p.getHealth();
		p.hurt(p.damageSources().mobAttack(z), 5f);
		near(helper, p.getHealth(), hp, "first attack evaded");
		helper.assertTrue(TechniqueService.get(p).dodges() == 1 && TechniqueService.get(p).instinctXp() == 1, "a charge spent, 1 XP");
		// Environmental damage is never evaded.
		p.invulnerableTime = 0;
		p.hurt(p.damageSources().drown(), 2f);
		helper.assertTrue(p.getHealth() < hp && TechniqueService.get(p).dodges() == 1, "drowning is not evaded and spends nothing");
		helper.runAfterDelay(3, () -> {
			p.setHealth(p.getMaxHealth());
			p.invulnerableTime = 0;
			// An Instinct-breaking move lands even with charges left.
			Power saber = Gear.powerFor(Gear.SABER);
			AbilityDef crossfire = saber.abilities().get(1);
			helper.assertTrue(crossfire.instinctBreaking(), "Scarlet Crossfire breaks Instinct");
			// The breaking move's hits are not evaded (checked on the rule itself: player-versus-player damage is off here)...
			boolean[] evaded = new boolean[1];
			com.grandline.technique.CombatContext.during(saber, crossfire, () -> {
				evaded[0] = com.grandline.technique.Dodges.evades(p, p.damageSources().mobAttack(z), 4f);
				return true;
			});
			helper.assertFalse(evaded[0], "Scarlet Crossfire is not evaded");
			// ...and a boss finisher (an instinct-breaking attack that is not a move) lands for real and spends nothing.
			float h0 = p.getHealth();
			com.grandline.technique.CombatContext.breaking(() -> p.hurt(p.damageSources().mobAttack(z), 4f));
			helper.assertTrue(p.getHealth() < h0 && TechniqueService.get(p).dodges() == 1, "breaking hit lands and spends nothing");
			p.invulnerableTime = 0;
			p.hurt(p.damageSources().mobAttack(z), 1f); // second charge
			helper.assertTrue(TechniqueService.get(p).dodges() == 0, "last charge spent");
			helper.runAfterDelay(3, () -> {
				p.invulnerableTime = 0;
				float h1 = p.getHealth();
				p.hurt(p.damageSources().mobAttack(z), 1f);
				helper.assertTrue(p.getHealth() < h1, "out of charges: hits land");
				// Toggling never refills.
				TechniqueService.toggleInstinct(p);
				TechniqueService.toggleInstinct(p);
				helper.assertTrue(TechniqueService.get(p).dodges() == 0, "toggling does not refill charges");
				// Regeneration: one charge after the regen time (counted while online, on or off).
				TechniqueService.update(p, t -> t.withDodges(0, TechniqueRules.regenTicks() - 2));
				helper.runAfterDelay(4, () -> {
					helper.assertTrue(TechniqueService.get(p).dodges() == 1, "a charge came back");
					p.discard();
					helper.succeed();
				});
			});
		});
	}

	// ------------------------------------------------------------------------------------------------ trainers and saves

	@GameTest(template = EMPTY_STRUCTURE, batch = "tq_trainers")
	public void trainersCheckLevelMoneyAndOwnership(GameTestHelper helper) {
		ServerPlayer p = player(helper);
		var cfg = GrandLineMod.config();
		PlayerProfileService.setLevel(p, cfg.flashStepLevel - 1);
		PlayerProfileService.update(p, pr -> pr.withCurrency(cfg.flashStepPrice));
		helper.assertTrue(TechniqueTrainers.buy(p, TechniqueTrainers.FLASH_STEP).equals("technique_level"), "level 100 needed");
		helper.assertTrue(PlayerProfileService.get(p).currency() == cfg.flashStepPrice, "no money taken");
		PlayerProfileService.setLevel(p, cfg.flashStepLevel);
		PlayerProfileService.update(p, pr -> pr.withCurrency(cfg.flashStepPrice - 1));
		helper.assertTrue(TechniqueTrainers.buy(p, TechniqueTrainers.FLASH_STEP).equals("technique_money"), "price needed");
		PlayerProfileService.update(p, pr -> pr.withCurrency(cfg.flashStepPrice + 5));
		helper.assertTrue(TechniqueTrainers.buy(p, TechniqueTrainers.FLASH_STEP).equals("technique_learned"), "bought");
		helper.assertTrue(PlayerProfileService.get(p).currency() == 5 && TechniqueService.get(p).flashStep(), "paid once, learned for good");
		helper.assertTrue(TechniqueTrainers.buy(p, TechniqueTrainers.FLASH_STEP).equals("technique_owned"), "not sold twice");
		PlayerProfileService.update(p, pr -> pr.withCurrency(cfg.auraPrice));
		helper.assertTrue(TechniqueTrainers.buy(p, TechniqueTrainers.AURA).equals("technique_learned"), "Aura has no level requirement");
		PlayerProfileService.setLevel(p, cfg.instinctLevel - 1);
		PlayerProfileService.update(p, pr -> pr.withCurrency(cfg.instinctPrice));
		helper.assertTrue(TechniqueTrainers.buy(p, TechniqueTrainers.INSTINCT).equals("technique_level"), "Instinct needs level 300");
		p.discard();
		helper.succeed();
	}

	private static com.grandline.world.Island island(String name) {
		return new com.grandline.world.Island(1, 0, 0, 40, 1, name, false, true, "plains");
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "tq_trainers")
	public void techniquesSaveAndTrainersHaveTheirIslands(GameTestHelper helper) {
		Techniques t = new Techniques(true, true, 12_345, true, 400, 3, 77);
		var encoded = Techniques.CODEC.encodeStart(NbtOps.INSTANCE, t).result().orElseThrow();
		Techniques back = Techniques.CODEC.parse(NbtOps.INSTANCE, encoded).result().orElseThrow();
		helper.assertTrue(back.equals(t), "techniques survive saving: " + back);
		Techniques old = Techniques.CODEC.parse(NbtOps.INSTANCE, new net.minecraft.nbt.CompoundTag()).result().orElseThrow();
		helper.assertTrue(old.equals(Techniques.NONE), "an old save without techniques loads empty");
		List<Islander.Role> mill = KeeperNpcs.rolesFor(island(KeeperNpcs.SWORD_ISLAND));
		List<Islander.Role> marine = KeeperNpcs.rolesFor(island(GrandLineMod.config().marineSpawnIsland));
		List<Islander.Role> frost = KeeperNpcs.rolesFor(island(KeeperNpcs.FROST_ISLAND));
		List<Islander.Role> sky = KeeperNpcs.rolesFor(island(KeeperNpcs.SKY_ISLAND));
		helper.assertTrue(mill.contains(Islander.Role.MOVEMENT_INSTRUCTOR) && marine.contains(Islander.Role.MOVEMENT_INSTRUCTOR), "Movement Instructors");
		helper.assertTrue(frost.contains(Islander.Role.FLASH_STEP_MASTER) && frost.contains(Islander.Role.AURA_MASTER), "Frostpeak's masters");
		helper.assertTrue(sky.contains(Islander.Role.INSTINCT_SAGE), "the Instinct Sage on Cloudtop");
		helper.succeed();
	}
}
