package com.grandline.fruit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GachaPricingTest {
	@Test
	void priceScalesWithLevel() {
		assertEquals(32_350, FruitService.gachaCost(50));
		assertEquals(39_850, FruitService.gachaCost(100));
		assertEquals(174_850, FruitService.gachaCost(1_000));
		assertEquals(444_850, FruitService.gachaCost(2_800));
	}
}
