package com.grandline.player.loadout;

import com.mojang.serialization.Codec;

import java.util.ArrayList;
import java.util.List;

/**
 * The order of the four loadout entries on the Grand Line hotbar. Entry ids: 0 Fighting Style, 1 Fruit, 2 Sword,
 * 3 Gun. Position {@code i} of the order is loadout key {@code i + 1}; the default order is Style, Fruit, Sword, Gun.
 */
public final class LoadoutOrder {
	public static final int SIZE = 4;
	public static final List<Integer> DEFAULT = List.of(0, 1, 2, 3);
	/** Stored as a list of entry ids; anything that is not a permutation of 0..3 loads as the default order. */
	public static final Codec<List<Integer>> CODEC = Codec.INT.listOf().xmap(LoadoutOrder::sanitize, list -> list);

	private LoadoutOrder() {
	}

	/** True if {@code order} holds each entry id 0..3 exactly once. */
	public static boolean isValid(List<Integer> order) {
		if (order == null || order.size() != SIZE) {
			return false;
		}
		boolean[] seen = new boolean[SIZE];
		for (Integer id : order) {
			if (id == null || id < 0 || id >= SIZE || seen[id]) {
				return false;
			}
			seen[id] = true;
		}
		return true;
	}

	/** {@code order} if it is valid, otherwise the default order. */
	public static List<Integer> sanitize(List<Integer> order) {
		return isValid(order) ? List.copyOf(order) : DEFAULT;
	}

	/** The order with positions {@code a} and {@code b} swapped. */
	public static List<Integer> swap(List<Integer> order, int a, int b) {
		List<Integer> copy = new ArrayList<>(sanitize(order));
		if (a >= 0 && a < SIZE && b >= 0 && b < SIZE) {
			Integer tmp = copy.get(a);
			copy.set(a, copy.get(b));
			copy.set(b, tmp);
		}
		return List.copyOf(copy);
	}
}
