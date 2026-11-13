package com.grandline.client;

import com.grandline.gear.EquippedGear;
import com.grandline.gear.GearBonuses;

/** Client copy of the player's equipped accessories and their server-computed bonuses (display only). */
public final class ClientGear {
	private static EquippedGear gear = EquippedGear.EMPTY;
	private static GearBonuses bonuses = GearBonuses.NONE;

	private ClientGear() {
	}

	public static void accept(EquippedGear newGear, GearBonuses newBonuses) {
		gear = newGear;
		bonuses = newBonuses;
	}

	public static EquippedGear gear() {
		return gear;
	}

	public static GearBonuses bonuses() {
		return bonuses;
	}

	public static void clear() {
		accept(EquippedGear.EMPTY, GearBonuses.NONE);
	}
}
