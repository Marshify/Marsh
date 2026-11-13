package com.grandline.test;

import com.grandline.combat.EnergyService;
import com.grandline.combat.ability.AbilityContext;
import com.grandline.combat.ability.Power;
import com.grandline.fruit.Fruits;
import com.grandline.gear.Gear;
import com.grandline.gear.GearService;
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
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Accessories: each one boosts exactly its intended damage sources (abilities by their category, normal sword/melee
 * hits through attack damage, gun shots), adds its HP/energy, and loses all of it when unequipped, without ever
 * refilling health or energy.
 */
public final class GrandLineGearGameTests implements FabricGameTest {
	private static final float BASE = 10f;

	private static ServerPlayer player(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		Vec3 home = helper.absoluteVec(new Vec3(1.5, 2, 1.5));
		player.moveTo(home.x, home.y, home.z, 0, 0);
		player.getInventory().clearContent();
		return player;
	}

	private static void equip(ServerPlayer player, Item item) {
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item));
		GearService.equip(player, InteractionHand.MAIN_HAND);
	}

	/** Damage an ability of {@code power} deals to a fresh 100 HP target (golems have no armour). */
	private static float abilityDamage(GameTestHelper helper, ServerPlayer player, Power power) {
		IronGolem golem = helper.spawn(EntityType.IRON_GOLEM, new BlockPos(3, 2, 3));
		golem.setNoAi(true);
		float before = golem.getHealth();
		new AbilityContext(player, power, power.abilities().get(0), 1.0).damage(golem, BASE);
		float dealt = before - golem.getHealth();
		golem.discard();
		return dealt;
	}

	private static void near(GameTestHelper helper, double actual, double expected, String what) {
		helper.assertTrue(Math.abs(actual - expected) < 0.01, what + ": expected " + expected + " but got " + actual);
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "gear_1")
	public void blackCapeBoostsAllCombatAddsHealthAndEnergy(GameTestHelper helper) {
		ServerPlayer player = player(helper);
		double hp = player.getMaxHealth();
		double energy = EnergyService.max(player);
		EnergyService.trySpend(player, 0.5);
		double energyNow = EnergyService.current(player);
		equip(player, Gear.BLACK_CAPE);
		near(helper, abilityDamage(helper, player, Fruits.FLAME.power()), BASE * 1.05, "fruit ability");
		near(helper, abilityDamage(helper, player, Gear.powerFor(Gear.KATANA)), BASE * 1.05, "sword ability");
		near(helper, abilityDamage(helper, player, Gear.powerFor(Gear.MUSKET)), BASE * 1.05, "gun ability");
		near(helper, abilityDamage(helper, player, com.grandline.combat.style.FightingStyles.BRAWLER), BASE * 1.05, "melee ability");
		near(helper, player.getMaxHealth(), hp + 4, "max HP +4");
		near(helper, EnergyService.max(player), energy + 10, "max energy +10");
		near(helper, EnergyService.current(player), energyNow, "equipping does not refill energy");
		GearService.unequip(player, 0);
		near(helper, abilityDamage(helper, player, Fruits.FLAME.power()), BASE, "no bonus after unequip");
		near(helper, player.getMaxHealth(), hp, "max HP back");
		near(helper, EnergyService.max(player), energy, "max energy back");
		helper.assertTrue(player.getInventory().countItem(Gear.BLACK_CAPE) == 1, "cape returned to the inventory");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "gear_2")
	public void pinkCoatBoostsOnlyGunsAndAddsHealth(GameTestHelper helper) {
		ServerPlayer player = player(helper);
		double hp = player.getMaxHealth();
		equip(player, Gear.PINK_COAT);
		near(helper, abilityDamage(helper, player, Gear.powerFor(Gear.FLINTLOCK)), BASE * 1.10, "gun ability +10%");
		near(helper, abilityDamage(helper, player, Gear.powerFor(Gear.KATANA)), BASE, "sword unaffected");
		near(helper, abilityDamage(helper, player, Fruits.ICE.power()), BASE, "fruit unaffected");
		near(helper, GearService.damageMultiplier(player, StatType.GUN), 1.10, "gun shots use the same multiplier");
		near(helper, player.getMaxHealth(), hp + 8, "max HP +8");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "gear_3")
	public void swordsmanHatBoostsSwordAbilitiesAndSwordHits(GameTestHelper helper) {
		ServerPlayer player = player(helper);
		equip(player, Gear.SWORDSMAN_HAT);
		near(helper, abilityDamage(helper, player, Gear.powerFor(Gear.SABER)), BASE * 1.10, "sword ability +10%");
		near(helper, abilityDamage(helper, player, Fruits.MAGMA.power()), BASE, "fruit unaffected");
		// Normal hits: attack damage with the katana in hand is 10% higher than without the hat.
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Gear.KATANA));
		helper.runAfterDelay(2, () -> {
			double withHat = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
			GearService.unequip(player, 0);
			helper.runAfterDelay(2, () -> {
				double without = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
				near(helper, withHat, without * 1.10, "sword hit damage +10% (" + withHat + " vs " + without + ")");
				helper.succeed();
			});
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "gear_4")
	public void tomoeRingBoostsFruitEvenWhileHoldingAWeapon(GameTestHelper helper) {
		ServerPlayer player = player(helper);
		equip(player, Gear.TOMOE_RING);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Gear.KATANA)); // holding a sword
		near(helper, abilityDamage(helper, player, Fruits.LIGHTNING.power()), BASE * 1.10, "fruit ability +10% while holding a sword");
		near(helper, abilityDamage(helper, player, Gear.powerFor(Gear.KATANA)), BASE, "sword unaffected");
		helper.assertTrue(GearService.get(player).ring1().equals("grandline:tomoe_ring"), "ring in a ring slot");
		GearService.unequip(player, 1);
		near(helper, abilityDamage(helper, player, Fruits.LIGHTNING.power()), BASE, "no bonus after unequip");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "gear_5")
	public void swappingGearNeverRefillsHealthOrEnergy(GameTestHelper helper) {
		ServerPlayer player = player(helper);
		equip(player, Gear.PINK_COAT);
		player.setHealth(player.getMaxHealth()); // full at 28
		GearService.unequip(player, 0);
		near(helper, player.getHealth(), player.getMaxHealth(), "health clamped down to the lower cap");
		player.setHealth(6);
		equip(player, Gear.PINK_COAT);
		near(helper, player.getHealth(), 6, "equipping does not heal");
		GearService.unequip(player, 0);
		equip(player, Gear.BLACK_CAPE);
		EnergyService.trySpend(player, 1.0); // empty
		double empty = EnergyService.current(player);
		GearService.unequip(player, 0);
		equip(player, Gear.BLACK_CAPE);
		near(helper, EnergyService.current(player), empty, "re-equipping does not refill energy");
		helper.succeed();
	}
}
