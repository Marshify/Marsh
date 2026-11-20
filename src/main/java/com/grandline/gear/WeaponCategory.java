package com.grandline.gear;

import com.grandline.fishing.StarterRodItem;
import net.minecraft.world.item.Item;

/**
 * The equipment categories with one equipped weapon each: a sword, a gun and a fishing rod. Equipping a weapon replaces
 * the one equipped in its category (in the same hotbar slot); every other owned weapon waits in the Backpack.
 * (The fighting style and fruit have their own single slots in the profile; accessories have their gear slots.)
 */
public enum WeaponCategory {
	SWORD("sword"), GUN("gun"), ROD("rod");

	public final String key;

	WeaponCategory(String key) {
		this.key = key;
	}

	/** The item's category, or null if it is not a sword, gun or rod. */
	public static WeaponCategory of(Item item) {
		if (item instanceof GearSwordItem) {
			return SWORD;
		}
		if (item instanceof GunItem) {
			return GUN;
		}
		if (item instanceof StarterRodItem) {
			return ROD;
		}
		return null;
	}

	/** Swords and guns live in hotbar slots 1-4 (the loadout keys); a rod in any hotbar slot. */
	public boolean loadoutSlot() {
		return this != ROD;
	}
}
