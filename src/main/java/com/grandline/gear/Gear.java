package com.grandline.gear;

import com.grandline.GrandLineMod;
import com.grandline.combat.ability.AbilityAction;
import com.grandline.combat.ability.AbilityDef;
import com.grandline.combat.ability.Power;
import com.grandline.combat.ability.PowerSource;
import com.grandline.combat.ability.Powers;
import com.grandline.fruit.Element;
import com.grandline.fruit.FruitMoves;
import com.grandline.player.StatType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Tiers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Swords, guns and accessories (Gear Batch 01). Each sword and gun is also a {@link Power}: while it is in your main
 * hand the move keys (Z, X, C) use its moves, they scale with the Sword or Gun stat, and kills with it train its own
 * mastery. Textures: textures/item/&lt;id&gt;.png.
 */
public final class Gear {
	private static final List<Item> ALL = new ArrayList<>();
	private static final Map<Item, Power> WEAPON_POWERS = new HashMap<>();

	/** Mastery needed for a weapon's Z, X and C moves. */
	private static final int[] MASTERY = {0, 20, 50};
	private static final int[] COOLDOWN = {60, 140, 240};
	private static final double[] ENERGY = {0.05, 0.1, 0.16};

	// Swords: tier sets durability, the int is bonus attack damage, the float attack speed modifier.
	public static final Item KATANA = sword("katana", Tiers.IRON, 3, -2.2F, Rarity.UNCOMMON, 0xB8C4D6,
			move("katana_draw", FruitMoves.dash(Element.METAL, 2.2, 9f)),
			move("katana_crescent", FruitMoves.bolt(Element.METAL, 10, 2.2, 11f)),
			move("katana_flurry", FruitMoves.lingering(Element.METAL, 6, 3.5, 6f, 3f, 3)));
	public static final Item CUTLASS = sword("cutlass", Tiers.IRON, 2, -2.0F, Rarity.COMMON, 0xD8C08A,
			move("cutlass_plunder", FruitMoves.burst(Element.METAL, 4, 8f)),
			move("cutlass_lunge", FruitMoves.dash(Element.METAL, 2.4, 10f)));
	public static final Item SABER = sword("saber", Tiers.DIAMOND, 3, -2.3F, Rarity.RARE, 0xC9D3E0,
			move("saber_thrust", FruitMoves.strike(Element.METAL, 6, 10f)),
			move("saber_twin_arc", FruitMoves.bolt(Element.METAL, 12, 2, 12f)),
			move("saber_flourish", FruitMoves.burst(Element.METAL, 5, 14f)));
	public static final Item TRIDENT = sword("trident", Tiers.DIAMOND, 4, -2.8F, Rarity.RARE, 0x7FB7C9,
			move("trident_thrust", FruitMoves.strike(Element.WATER, 8, 12f)),
			move("trident_riptide", FruitMoves.vortex(Element.WATER, 8, 5, 12f)),
			move("trident_throw", FruitMoves.bolt(Element.WATER, 18, 1.4, 16f)));

	// Guns: damage, shot speed, reload ticks, knockback for right-click; moves on Z/X/C.
	public static final Item SLINGSHOT = gun("slingshot", 4, 2.0F, 12, 0, Rarity.COMMON, 0xB07A3A,
			move("slingshot_volley", FruitMoves.bolt(Element.EARTH, 16, 1.4, 6f)));
	public static final Item FLINTLOCK = gun("flintlock", 8, 3.2F, 25, 0, Rarity.COMMON, 0x9A6A3A,
			move("flintlock_quickdraw", FruitMoves.strike(Element.EXPLOSION, 20, 9f)),
			move("flintlock_smoke", FruitMoves.lingering(Element.MIST, 16, 3, 6f, 2f, 3)));
	public static final Item MUSKET = gun("musket", 13, 4.0F, 40, 1, Rarity.UNCOMMON, 0x7A5A3A,
			move("musket_longshot", FruitMoves.strike(Element.EXPLOSION, 30, 14f)),
			move("musket_volley", FruitMoves.bolt(Element.EXPLOSION, 26, 1.6, 12f)));
	public static final Item CANNON = gun("cannon", 20, 2.4F, 70, 3, Rarity.RARE, 0x3A3A44,
			move("cannon_ball", FruitMoves.eruption(Element.EXPLOSION, 22, 4, 16f, 0.6)),
			move("cannon_grapeshot", FruitMoves.bolt(Element.EXPLOSION, 16, 3, 12f)),
			move("cannon_broadside", FruitMoves.lingering(Element.EXPLOSION, 20, 5, 10f, 4f, 3)));

	// Accessories and rings: slot and bonuses (see AccessoryItem.Kind; HP and energy values are in the config).
	public static final Item BLACK_CAPE = register("black_cape", new AccessoryItem(new Item.Properties().rarity(Rarity.UNCOMMON),
			AccessoryItem.Slot.ACCESSORY, AccessoryItem.Kind.BLACK_CAPE));
	public static final Item PINK_COAT = register("pink_coat", new AccessoryItem(new Item.Properties().rarity(Rarity.RARE),
			AccessoryItem.Slot.ACCESSORY, AccessoryItem.Kind.PINK_COAT));
	public static final Item SWORDSMAN_HAT = register("swordsman_hat", new AccessoryItem(new Item.Properties().rarity(Rarity.UNCOMMON),
			AccessoryItem.Slot.ACCESSORY, AccessoryItem.Kind.SWORDSMAN_HAT));
	public static final Item TOMOE_RING = register("tomoe_ring", new AccessoryItem(new Item.Properties().rarity(Rarity.RARE),
			AccessoryItem.Slot.RING, AccessoryItem.Kind.TOMOE_RING));

	private Gear() {
	}

	private record Move(String key, AbilityAction action) {
	}

	private static Move move(String key, AbilityAction action) {
		return new Move(key, action);
	}

	private static Item sword(String name, Tiers tier, int damage, float speed, Rarity rarity, int color, Move... moves) {
		Item item = register(name, new GearSwordItem(tier, damage, speed, new Item.Properties().rarity(rarity)));
		weaponPower(item, name, StatType.SWORD, rarity, color, moves);
		return item;
	}

	private static Item gun(String name, double damage, float velocity, int reload, int knockback, Rarity rarity, int color, Move... moves) {
		Item item = register(name, new GunItem(new Item.Properties().stacksTo(1).rarity(rarity), damage, velocity, reload, knockback,
				name.equals("slingshot") ? SoundEvents.CROSSBOW_SHOOT : SoundEvents.GENERIC_EXPLODE));
		weaponPower(item, name, StatType.GUN, rarity, color, moves);
		return item;
	}

	private static void weaponPower(Item item, String name, StatType stat, Rarity rarity, int color, Move... moves) {
		List<AbilityDef> defs = new ArrayList<>();
		for (int i = 0; i < moves.length; i++) {
			defs.add(new AbilityDef(moves[i].key(), MASTERY[i], COOLDOWN[i], ENERGY[i], moves[i].action()));
		}
		Power power = Powers.register(new Power(GrandLineMod.id(name), PowerSource.WEAPON, stat,
				rarity.name().toLowerCase(java.util.Locale.ROOT), color, defs));
		WEAPON_POWERS.put(item, power);
	}

	private static Item register(String name, Item item) {
		Registry.register(BuiltInRegistries.ITEM, GrandLineMod.id(name), item);
		ALL.add(item);
		return item;
	}

	/** The weapon power of a sword or gun item, or null for anything else. */
	public static Power powerFor(Item item) {
		return WEAPON_POWERS.get(item);
	}

	public static List<Item> all() {
		return List.copyOf(ALL);
	}

	public static List<Power> weaponPowers() {
		return List.copyOf(WEAPON_POWERS.values());
	}

	/** Forces class loading during mod initialization. */
	public static void register() {
		GrandLineMod.LOGGER.debug("Registered {} gear items", ALL.size());
	}
}
