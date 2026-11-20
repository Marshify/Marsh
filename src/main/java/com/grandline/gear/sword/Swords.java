package com.grandline.gear.sword;

import com.grandline.combat.ability.AbilityDef;
import net.minecraft.world.item.Item;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The fifteen First Sea swords: display rarity, the two moves (Z, X), the normal-attack combo and the colour palette
 * the client draws them in. Registered as items by {@link com.grandline.gear.Gear}.
 */
public final class Swords {
	/** How a combo's last swing finishes. */
	public enum Finisher {
		/** A wide sweeping slash round the front. */
		SWEEP,
		/** A straight forward thrust that reaches further. */
		THRUST,
		/** Two blades crossing in front. */
		CROSS,
		/** An area-of-effect slam on the ground ahead. */
		SLAM,
		/** A short forward dash through the enemy. */
		DASH,
		/** A full turn hitting all round. */
		SPIN,
		/** A heavy strike with extra knockback. */
		HEAVY,
		/** A sweeping strike followed by a forward lunge. */
		LUNGE
	}

	/**
	 * A normal-attack combo: the trail style of each swing (see the client's SwordFx: 0 flat right-to-left, 1 rising,
	 * 2 falling, 3 overhead, 4 thrust, 5 spin, 6 flat left-to-right) and how the last swing finishes.
	 */
	public record Combo(int[] styles, Finisher finisher) {
		public int steps() {
			return styles.length;
		}
	}

	/** Everything about one sword besides its item stats. */
	public record Spec(String rarity, String palette, List<AbilityDef> moves, Combo combo, boolean upgradeable) {
	}

	public static final Map<Item, Spec> SPECS = new HashMap<>();

	private Swords() {
	}

	private static Spec spec(String rarity, String palette, List<AbilityDef> moves, Finisher finisher, int... styles) {
		return new Spec(rarity, palette, moves, new Combo(styles, finisher), false);
	}

	public static Spec katana() {
		return spec("common", "white", SwordMovesA.katana(), Finisher.SWEEP, 0, 6, 1, 5);
	}

	public static Spec cutlass() {
		return spec("common", "cutlass", SwordMovesA.cutlass(), Finisher.THRUST, 0, 6, 0, 4);
	}

	public static Spec dualKatana() {
		return spec("common", "teal", SwordMovesA.dualKatana(), Finisher.CROSS, 0, 6, 1, 2, 7);
	}

	public static Spec ironMace() {
		return spec("uncommon", "stone", SwordMovesA.ironMace(), Finisher.SLAM, 0, 6, 3);
	}

	public static Spec reefsaw() {
		return spec("uncommon", "reef", SwordMovesA.reefsaw(), Finisher.SLAM, 0, 6, 1, 3);
	}

	public static Spec tripleKatana() {
		return spec("uncommon", "azure", SwordMovesA.tripleKatana(), Finisher.DASH, 0, 6, 1, 2, 4);
	}

	public static Spec boilerPipe() {
		return spec("rare", "fire", SwordMovesA.boilerPipe(), Finisher.SWEEP, 0, 6, 3, 5);
	}

	public static Spec stonewallBlade() {
		return spec("rare", "warden", SwordMovesA.stonewallBlade(), Finisher.HEAVY, 0, 6, 0, 3);
	}

	public static Spec twinfangGlaive() {
		return spec("rare", "twin", SwordMovesB.twinfangGlaive(), Finisher.SPIN, 5, 5, 5, 5);
	}

	public static Spec wraithCane() {
		return spec("rare", "wraith", SwordMovesB.wraithCane(), Finisher.THRUST, 0, 6, 0, 4);
	}

	public static Spec trident() {
		return spec("rare", "tide", SwordMovesB.trident(), Finisher.LUNGE, 0, 6, 4, 4);
	}

	public static Spec bisento() {
		Spec s = spec("legendary", "bisento", SwordMovesB.bisento(), Finisher.SLAM, 0, 6, 0, 3);
		return new Spec(s.rarity(), s.palette(), s.moves(), s.combo(), true);
	}

	public static Spec stormstaff() {
		return spec("legendary", "storm", SwordMovesB.stormstaff(), Finisher.SWEEP, 0, 6, 3, 5);
	}

	public static Spec saber() {
		return spec("legendary", "saber", SwordMovesB.saber(), Finisher.THRUST, 0, 6, 1, 4);
	}

	public static Spec eclipseBlade() {
		Spec s = spec("mythical", "eclipse", SwordMovesB.eclipseBlade(), Finisher.DASH, 0, 6, 3, 4);
		return new Spec(s.rarity(), s.palette(), s.moves(), s.combo(), true);
	}

	public static Spec spec(Item item) {
		return SPECS.get(item);
	}
}
