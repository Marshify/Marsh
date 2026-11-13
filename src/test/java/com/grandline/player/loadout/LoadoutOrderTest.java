package com.grandline.player.loadout;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoadoutOrderTest {
	@Test
	void defaultIsStyleFruitSwordGun() {
		assertEquals(List.of(0, 1, 2, 3), LoadoutOrder.DEFAULT);
	}

	@Test
	void swappingFruitAndGunPutsGunOnTwoAndFruitOnFour() {
		List<Integer> swapped = LoadoutOrder.swap(LoadoutOrder.DEFAULT, 1, 3);
		assertEquals(List.of(0, 3, 2, 1), swapped);
		assertEquals(LoadoutOrder.DEFAULT, LoadoutOrder.swap(swapped, 3, 1), "swapping back restores the order");
	}

	@Test
	void onlyPermutationsAreValid() {
		assertTrue(LoadoutOrder.isValid(List.of(3, 2, 1, 0)));
		assertFalse(LoadoutOrder.isValid(List.of(0, 0, 1, 2)), "duplicate entry");
		assertFalse(LoadoutOrder.isValid(List.of(0, 1, 2)), "missing entry");
		assertFalse(LoadoutOrder.isValid(List.of(0, 1, 2, 4)), "unknown entry");
		assertFalse(LoadoutOrder.isValid(Arrays.asList(0, 1, null, 3)), "null entry");
		assertEquals(LoadoutOrder.DEFAULT, LoadoutOrder.sanitize(List.of(9, 9)), "bad data loads as the default");
	}
}
