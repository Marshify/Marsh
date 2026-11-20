package com.grandline.test;

import com.grandline.GrandLineMod;
import com.grandline.combat.ability.AbilityContext;
import com.grandline.combat.ability.AbilityDef;
import com.grandline.combat.ability.ChargeService;
import com.grandline.combat.ability.Power;
import com.grandline.gear.Gear;
import com.grandline.gear.GearSwordItem;
import com.grandline.gear.sword.Stuns;
import com.grandline.gear.sword.SwordCombos;
import com.grandline.gear.sword.SwordUpgrades;
import com.grandline.gear.sword.Swords;
import com.grandline.npc.SwordShop;
import com.grandline.player.PlayerProfileService;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * The First Sea sword overhaul: the fifteen swords, their Z/X moves (mastery, cooldown, energy as specified), each move
 * actually hitting a target in front of the player, the normal-attack combos, stuns, upgrades and the shop.
 */
public final class GrandLineSwordGameTests implements FabricGameTest {
	/** id → {Z mastery, Z cooldown ticks, Z energy %, X mastery, X cooldown ticks, X energy %}. */
	static final Map<String, int[]> SPEC = Map.ofEntries(
			Map.entry("katana", new int[]{1, 140, 15, 20, 200, 25}),
			Map.entry("cutlass", new int[]{1, 140, 15, 20, 200, 25}),
			Map.entry("dual_katana", new int[]{1, 140, 15, 30, 260, 25}),
			Map.entry("iron_mace", new int[]{1, 140, 15, 30, 220, 25}),
			Map.entry("reefsaw", new int[]{10, 160, 15, 60, 240, 25}),
			Map.entry("triple_katana", new int[]{20, 150, 15, 60, 240, 25}),
			Map.entry("boiler_pipe", new int[]{1, 150, 15, 50, 210, 25}),
			Map.entry("stonewall_blade", new int[]{35, 180, 15, 80, 300, 25}),
			Map.entry("twinfang_glaive", new int[]{20, 150, 15, 100, 200, 25}),
			Map.entry("wraith_cane", new int[]{40, 150, 25, 120, 270, 30}),
			Map.entry("bisento", new int[]{50, 180, 20, 150, 270, 30}),
			Map.entry("trident", new int[]{50, 120, 30, 125, 220, 25}),
			Map.entry("stormstaff", new int[]{80, 150, 15, 180, 210, 25}),
			Map.entry("saber", new int[]{50, 150, 15, 125, 280, 30}),
			Map.entry("eclipse_blade", new int[]{1, 170, 20, 50, 260, 40}));

	static final List<Item> ALL = List.of(Gear.KATANA, Gear.CUTLASS, Gear.DUAL_KATANA, Gear.IRON_MACE, Gear.REEFSAW, Gear.TRIPLE_KATANA,
			Gear.BOILER_PIPE, Gear.STONEWALL_BLADE, Gear.TWINFANG_GLAIVE, Gear.WRAITH_CANE, Gear.BISENTO, Gear.TRIDENT, Gear.STORMSTAFF,
			Gear.SABER, Gear.ECLIPSE_BLADE);

	@GameTest(template = EMPTY_STRUCTURE, batch = "swords_registry")
	public void fifteenSwordsWithTheirSpecifiedMoves(GameTestHelper helper) {
		helper.assertTrue(ALL.size() == 15 && ALL.stream().distinct().count() == 15, "fifteen distinct swords");
		com.google.gson.JsonObject lang = json("/assets/grandline/lang/en_us.json");
		for (Item item : ALL) {
			String id = BuiltInRegistries.ITEM.getKey(item).getPath();
			helper.assertTrue(item instanceof GearSwordItem, id + " is a sword (Sword stat scaling, sword slot)");
			Power power = Gear.powerFor(item);
			Swords.Spec spec = Swords.spec(item);
			helper.assertTrue(power != null && spec != null, id + " has its moves");
			helper.assertTrue(power.id().toString().equals("grandline:" + id), id + " power id matches the item id (mastery is kept by id)");
			helper.assertTrue(power.abilities().size() == 2, id + " has exactly Z and X");
			int[] want = SPEC.get(id);
			helper.assertTrue(want != null, id + " is in the spec table");
			for (int k = 0; k < 2; k++) {
				AbilityDef a = power.abilities().get(k);
				helper.assertTrue(a.requiredMastery() == want[k * 3] && a.cooldownTicks() == want[k * 3 + 1]
								&& Math.round(a.energyCost() * 100) == want[k * 3 + 2],
						id + " " + a.key() + ": mastery " + a.requiredMastery() + ", cooldown " + a.cooldownTicks() + ", energy " + a.energyCost());
				helper.assertTrue(lang.has(a.translationKey()), "name for " + a.key());
			}
			helper.assertTrue(lang.has("item.grandline." + id) && lang.has("power.grandline." + id), "names for " + id);
			helper.assertTrue(resource("/assets/grandline/textures/item/" + id + ".png") && resource("/assets/grandline/models/item/" + id + ".json"),
					id + " has its texture and model");
			helper.assertTrue(spec.combo().steps() >= 3, id + " has a normal-attack combo");
		}
		for (String old : List.of("katana", "cutlass", "saber", "trident")) {
			helper.assertTrue(BuiltInRegistries.ITEM.containsKey(GrandLineMod.id(old)), "existing id kept: " + old);
		}
		helper.assertFalse(lang.has("ability.grandline.katana_flurry"), "the old Katana C move is gone");
		helper.assertTrue(resource("/assets/grandline/textures/item/eclipse_blade.png.mcmeta"), "the Eclipse Blade's aura is animated");
		helper.assertTrue(SwordShop.STOCK.size() == 14 && !SwordShop.STOCK.contains(Gear.SABER), "the shop sells every sword but the Saber");
		helper.assertTrue(GrandLineMod.config().swordPrices.length == SwordShop.STOCK.size(), "a price for every sword in stock");
		helper.succeed();
	}

	// ------------------------------------------------------------------------------------------------ every move hits

	private static ServerPlayer player(GameTestHelper helper, Item sword, int mastery) {
		ServerPlayer p = helper.makeMockServerPlayerInLevel();
		GrandLineRaceGameTests.neutral(p);
		p.getInventory().clearContent();
		// Swords are owned equipment: owning it puts it in the hotbar, and unowned copies are taken away.
		com.grandline.gear.EquipmentOwnership.grant(p, sword);
		for (int i = 0; i < 9; i++) {
			if (p.getInventory().getItem(i).is(sword)) {
				p.getInventory().selected = i;
			}
		}
		Vec3 home = helper.absoluteVec(new Vec3(1.5, 1, 1.5));
		p.moveTo(home.x, home.y, home.z, 0, 22);
		p.setYHeadRot(0);
		PlayerProfileService.setMasteryLevel(p, Gear.powerFor(sword), mastery);
		return p;
	}

	private static IronGolem dummy(GameTestHelper helper, ServerPlayer p) {
		IronGolem golem = helper.spawn(EntityType.IRON_GOLEM, new BlockPos(1, 2, 5));
		golem.setNoAi(true);
		golem.moveTo(p.getX(), p.getY(), p.getZ() + 4.0);
		return golem;
	}

	/** Runs the sword's Z, then its X against fresh dummies, and checks each one dealt damage. */
	private static void bothMovesHit(GameTestHelper helper, Item sword, int mastery) {
		ServerPlayer p = player(helper, sword, mastery);
		Power power = Gear.powerFor(sword);
		IronGolem first = dummy(helper, p);
		run(p, power, 0);
		helper.runAfterDelay(70, () -> {
			helper.assertTrue(first.getHealth() < first.getMaxHealth(), power.abilities().get(0).key() + " hit the target");
			first.discard();
			Vec3 home = helper.absoluteVec(new Vec3(1.5, 1, 1.5));
			p.moveTo(home.x, home.y, home.z, 0, 22);
			p.setDeltaMovement(Vec3.ZERO);
			IronGolem second = dummy(helper, p);
			Vec3 aimed = com.grandline.combat.ability.Projectiles.crosshairTarget(p, 30);
			run(p, power, 1);
			helper.runAfterDelay(80, () -> {
				helper.assertTrue(second.getHealth() < second.getMaxHealth(), power.abilities().get(1).key() + " hit the target (player "
						+ p.position() + ", target " + second.position() + ", aim " + aimed + ")");
				second.discard();
				p.discard();
				helper.succeed();
			});
		});
	}

	private static void run(ServerPlayer p, Power power, int slot) {
		AbilityDef a = power.abilities().get(slot);
		a.action().execute(new AbilityContext(p, power, a, 1.0, 0.0));
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "swords_m1", timeoutTicks = 260)
	public void katanaMovesHit(GameTestHelper helper) {
		bothMovesHit(helper, Gear.KATANA, 200);
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "swords_m2", timeoutTicks = 260)
	public void cutlassMovesHit(GameTestHelper helper) {
		bothMovesHit(helper, Gear.CUTLASS, 200);
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "swords_m3", timeoutTicks = 260)
	public void dualKatanaMovesHit(GameTestHelper helper) {
		bothMovesHit(helper, Gear.DUAL_KATANA, 200);
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "swords_m4", timeoutTicks = 260)
	public void ironMaceMovesHit(GameTestHelper helper) {
		bothMovesHit(helper, Gear.IRON_MACE, 200);
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "swords_m5", timeoutTicks = 260)
	public void reefsawMovesHit(GameTestHelper helper) {
		bothMovesHit(helper, Gear.REEFSAW, 200);
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "swords_m6", timeoutTicks = 260)
	public void tripleKatanaMovesHit(GameTestHelper helper) {
		bothMovesHit(helper, Gear.TRIPLE_KATANA, 200);
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "swords_m7", timeoutTicks = 260)
	public void boilerPipeMovesHit(GameTestHelper helper) {
		bothMovesHit(helper, Gear.BOILER_PIPE, 200);
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "swords_m8", timeoutTicks = 260)
	public void stonewallBladeMovesHit(GameTestHelper helper) {
		bothMovesHit(helper, Gear.STONEWALL_BLADE, 200);
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "swords_m9", timeoutTicks = 260)
	public void twinfangGlaiveMovesHit(GameTestHelper helper) {
		bothMovesHit(helper, Gear.TWINFANG_GLAIVE, 200);
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "swords_m10", timeoutTicks = 260)
	public void wraithCaneMovesHit(GameTestHelper helper) {
		bothMovesHit(helper, Gear.WRAITH_CANE, 200);
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "swords_m11", timeoutTicks = 260)
	public void bisentoMovesHit(GameTestHelper helper) {
		bothMovesHit(helper, Gear.BISENTO, 200);
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "swords_m12", timeoutTicks = 260)
	public void bisentoUpgradedMovesHit(GameTestHelper helper) {
		bothMovesHit(helper, Gear.BISENTO, 320);
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "swords_m13", timeoutTicks = 260)
	public void tridentMovesHit(GameTestHelper helper) {
		bothMovesHit(helper, Gear.TRIDENT, 200);
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "swords_m14", timeoutTicks = 260)
	public void stormstaffMovesHit(GameTestHelper helper) {
		bothMovesHit(helper, Gear.STORMSTAFF, 200);
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "swords_m15", timeoutTicks = 260)
	public void saberMovesHit(GameTestHelper helper) {
		bothMovesHit(helper, Gear.SABER, 200);
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "swords_m16", timeoutTicks = 260)
	public void eclipseBladeMovesHit(GameTestHelper helper) {
		bothMovesHit(helper, Gear.ECLIPSE_BLADE, 200);
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "swords_m17", timeoutTicks = 260)
	public void eclipseBladeUpgradedMovesHit(GameTestHelper helper) {
		bothMovesHit(helper, Gear.ECLIPSE_BLADE, 320);
	}

	// ------------------------------------------------------------------------------------------------ rules

	@GameTest(template = EMPTY_STRUCTURE, batch = "swords_rules")
	public void upgradesUnlockAtTheirMastery(GameTestHelper helper) {
		ServerPlayer p = player(helper, Gear.ECLIPSE_BLADE, SwordUpgrades.upgradeMastery() - 1);
		Power eclipse = Gear.powerFor(Gear.ECLIPSE_BLADE);
		helper.assertFalse(SwordUpgrades.upgraded(p, eclipse), "not upgraded below the mastery");
		helper.assertFalse(ChargeService.charges(p, eclipse, eclipse.abilities().get(1)), "the plain Eclipse Cleaver fires on press");
		PlayerProfileService.setMasteryLevel(p, eclipse, SwordUpgrades.upgradeMastery());
		helper.assertTrue(SwordUpgrades.upgraded(p, eclipse), "upgraded at the mastery");
		helper.assertTrue(ChargeService.charges(p, eclipse, eclipse.abilities().get(1)), "the upgraded Eclipse Cleaver charges while held");
		helper.assertTrue(Swords.spec(Gear.BISENTO).upgradeable() && Swords.spec(Gear.ECLIPSE_BLADE).upgradeable()
				&& !Swords.spec(Gear.SABER).upgradeable(), "only the Bisento and the Eclipse Blade have upgraded forms");
		p.discard();
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "swords_combo", timeoutTicks = 200)
	public void normalAttacksChainIntoAFinisher(GameTestHelper helper) {
		ServerPlayer p = player(helper, Gear.IRON_MACE, 1);
		IronGolem golem = dummy(helper, p);
		golem.moveTo(p.getX(), p.getY(), p.getZ() + 2.0);
		int steps = Swords.spec(Gear.IRON_MACE).combo().steps();
		helper.assertTrue(SwordCombos.swing(p) == 0, "first swing starts the combo");
		helper.assertTrue(SwordCombos.swing(p) == -1, "a swing faster than the mace's attack speed is ignored");
		int[] seen = {1};
		helper.onEachTick(() -> {
		});
		for (int i = 1; i < steps; i++) {
			int n = i;
			helper.runAfterDelay(n * 20L, () -> {
				float before = golem.getHealth();
				int step = SwordCombos.swing(p);
				helper.assertTrue(step == n, "swing " + n + " is step " + n + " (got " + step + ")");
				if (n == steps - 1) {
					helper.assertTrue(golem.getHealth() < before, "the finisher (ground slam) hits");
				}
				seen[0]++;
			});
		}
		helper.runAfterDelay(steps * 20L + SwordCombos.RESET_TICKS + 4, () -> {
			helper.assertTrue(SwordCombos.swing(p) == 0, "a pause starts the combo over");
			golem.discard();
			p.discard();
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "swords_stun", timeoutTicks = 100)
	public void stunsHoldAndExpire(GameTestHelper helper) {
		IronGolem golem = helper.spawn(EntityType.IRON_GOLEM, new BlockPos(1, 2, 1));
		Stuns.apply(golem, 20);
		helper.assertTrue(Stuns.stunned(golem), "stunned");
		golem.setDeltaMovement(1, 0, 1);
		helper.runAfterDelay(2, () -> helper.assertTrue(golem.getDeltaMovement().horizontalDistance() < 1e-3, "cannot move while stunned"));
		helper.runAfterDelay(25, () -> {
			helper.assertFalse(Stuns.stunned(golem), "the stun wears off");
			golem.discard();
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "swords_rules")
	public void oldSwordPricesAreKept(GameTestHelper helper) {
		var config = new com.grandline.config.GrandLineConfig();
		config.swordPrices = new long[]{777, 333};
		config.validate();
		helper.assertTrue(config.swordPrices.length == SwordShop.STOCK.size() && config.swordPrices[0] == 777 && config.swordPrices[1] == 333,
				"a server's Katana and Cutlass prices survive; the new swords get defaults");
		helper.succeed();
	}

	// ------------------------------------------------------------------------------------------------ helpers

	private static boolean resource(String path) {
		try (InputStream in = GrandLineSwordGameTests.class.getResourceAsStream(path)) {
			return in != null;
		} catch (java.io.IOException e) {
			return false;
		}
	}

	private static com.google.gson.JsonObject json(String path) {
		try (InputStream in = GrandLineSwordGameTests.class.getResourceAsStream(path)) {
			return com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
		} catch (java.io.IOException e) {
			throw new IllegalStateException(e);
		}
	}

	static ResourceLocation id(Item item) {
		return BuiltInRegistries.ITEM.getKey(item);
	}
}
