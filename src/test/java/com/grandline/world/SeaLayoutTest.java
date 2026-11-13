package com.grandline.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SeaLayoutTest {
	@Test
	void hollowIsletIsRemovedAndFirstSeaHasFourteenIslands() {
		assertTrue(SeaLayout.all().stream().noneMatch(spec -> spec.name().equals("Hollow Islet")), "Hollow Islet is gone");
		assertEquals(14, SeaLayout.all().stream().filter(spec -> spec.sea() == 1).count(), "First Sea islands");
		assertEquals(10, SeaLayout.all().stream().filter(spec -> spec.sea() == 2).count(), "Second Sea unchanged");
		assertEquals(10, SeaLayout.all().stream().filter(spec -> spec.sea() == 3).count(), "Third Sea unchanged");
	}
}
