package com.grandline.gear;

import com.grandline.GrandLineMod;
import com.grandline.combat.ability.AbilityAction;
import com.grandline.combat.ability.AbilityDef;
import com.grandline.combat.ability.Power;
import com.grandline.combat.ability.PowerSource;
import com.grandline.combat.ability.Powers;
import com.grandline.fruit.Element;
import com.grandline.fruit.FruitMoves;
import com.grandline.gear.sword.Swords;
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

	// Swords (First Sea sword overhaul): bonus attack damage over the tier, attack speed modifier, rarity, colour, and
	// the hand-made Z/X moves from gear.sword. Ids of the four older swords (katana, cutlass, saber, trident) are kept,
	// so ownership and mastery carry over.
	public static final Item KATANA = sword("katana", Tiers.IRON, 3, -2.2F, Rarity.COMMON, 0xE8EEF5, Swords.katana());
	public static final Item CUTLASS = sword("cutlass", Tiers.IRON, 3, -2.0F, Rarity.COMMON, 0xD8C08A, Swords.cutlass());
	public static final Item DUAL_KATANA = sword("dual_katana", Tiers.IRON, 3, -1.9F, Rarity.COMMON, 0x8FE3D2, Swords.dualKatana());
	public static final Item IRON_MACE = sword("iron_mace", Tiers.IRON, 5, -2.8F, Rarity.UNCOMMON, 0x9A9A9A, Swords.ironMace());
	public static final Item REEFSAW = sword("reefsaw", Tiers.IRON, 5, -2.6F, Rarity.UNCOMMON, 0xA4161A, Swords.reefsaw());
	public static final Item TRIPLE_KATANA = sword("triple_katana", Tiers.IRON, 4, -2.0F, Rarity.UNCOMMON, 0x3FA8FF, Swords.tripleKatana());
	public static final Item BOILER_PIPE = sword("boiler_pipe", Tiers.DIAMOND, 5, -2.4F, Rarity.RARE, 0xFF7A1A, Swords.boilerPipe());
	public static final Item STONEWALL_BLADE = sword("stonewall_blade", Tiers.DIAMOND, 6, -2.4F, Rarity.RARE, 0x2F7BFF, Swords.stonewallBlade());
	public static final Item TWINFANG_GLAIVE = sword("twinfang_glaive", Tiers.DIAMOND, 5, -2.2F, Rarity.RARE, 0x9EE8FF, Swords.twinfangGlaive());
	public static final Item WRAITH_CANE = sword("wraith_cane", Tiers.DIAMOND, 5, -2.0F, Rarity.RARE, 0xB7D8FF, Swords.wraithCane());
	public static final Item TRIDENT = sword("trident", Tiers.DIAMOND, 6, -2.5F, Rarity.RARE, 0x1E7FD6, Swords.trident());
	public static final Item BISENTO = sword("bisento", Tiers.NETHERITE, 7, -2.7F, Rarity.EPIC, 0xF4F6FF, Swords.bisento());
	public static final Item STORMSTAFF = sword("stormstaff", Tiers.NETHERITE, 6, -2.3F, Rarity.EPIC, 0xFFE14A, Swords.stormstaff());
	// The Saber (Verdant Wilds' temple boss drop): melee damage, attack speed and move damage come from the config.
	public static final Item SABER = sword("saber", new SaberItem(new Item.Properties().rarity(Rarity.EPIC)), Rarity.EPIC, 0xE0142A, Swords.saber());
	public static final Item ECLIPSE_BLADE = sword("eclipse_blade", Tiers.NETHERITE, 8, -2.3F, Rarity.EPIC, 0x2BFF6A, Swords.eclipseBlade());

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

	private static Item sword(String name, Tiers tier, int damage, float speed, Rarity rarity, int color, Swords.Spec spec) {
		return sword(name, new GearSwordItem(tier, damage, speed, new Item.Properties().rarity(rarity)), rarity, color, spec);
	}

	private static Item sword(String name, Item sword, Rarity rarity, int color, Swords.Spec spec) {
		Item item = register(name, sword);
		Power power = Powers.register(new Power(GrandLineMod.id(name), PowerSource.WEAPON, StatType.SWORD, spec.rarity(), color, spec.moves()));
		WEAPON_POWERS.put(item, power);
		Swords.SPECS.put(item, spec);
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
