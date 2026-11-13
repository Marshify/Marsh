package com.grandline.ship;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShipOwnershipTest {
	@Test
	void callShipUsesTheSelectedShipElseTheBiggest() {
		assertNull(ShipOwnership.EMPTY.callable(), "no ships, nothing to call");
		ShipOwnership both = ShipOwnership.EMPTY.withShip(ShipClass.GALLEON).withShip(ShipClass.SLOOP);
		assertEquals(ShipClass.SLOOP, both.callable(), "buying selects the new ship");
		assertEquals(ShipClass.GALLEON, both.select(ShipClass.GALLEON).callable());
		assertEquals(ShipClass.GALLEON, new ShipOwnership(both.owned(), "unknown").callable(), "falls back to the biggest owned");
		assertEquals(2, both.withShip(ShipClass.SLOOP).owned().size(), "owning twice is a no-op");
	}

	@Test
	void classesGrowAndKeepTheHelmSeat() {
		ShipClass prev = null;
		for (ShipClass c : ShipClass.values()) {
			assertTrue(c.seats() >= 2, c + " has a helm and at least one crew seat");
			assertTrue(c.seat(0)[1] < 0, c + " helm is behind the middle");
			if (prev != null) {
				assertTrue(c.seats() > prev.seats() && c.width > prev.width && c.thrust > prev.thrust, c + " is bigger and faster");
			}
			prev = c;
		}
	}
}
