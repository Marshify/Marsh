package com.grandline.ship;

import com.grandline.GrandLineMod;
import net.minecraft.network.chat.Component;

/**
 * The three ship classes. Sizes are in blocks: {@code width} is the hitbox (square, as for every entity), {@code length}
 * the drawn hull, {@code deck} the deck height above the hitbox bottom. Seats are (sideways, forward) offsets from the
 * ship's centre; seat 0 is the helm, always taken by the owner when aboard.
 */
public enum ShipClass {
	SLOOP("sloop", 3.0F, 1.0F, 7, 0.015, 0, new float[][]{{0, -2.2F}, {0, 1.2F}}),
	BRIG("brig", 4.0F, 1.25F, 10, 0.025, 1.0F, new float[][]{{0, -3.4F}, {-1, 0.3F}, {1, 0.3F}, {0, 2.6F}}),
	GALLEON("galleon", 5.0F, 1.5F, 14, 0.035, 1.5F, new float[][]{{0, -4.6F}, {-1.4F, -1.0F}, {1.4F, -1.0F}, {-1.4F, 1.6F}, {1.4F, 1.6F}, {0, 4.2F}});

	public final String id;
	public final float width;
	public final float deck;
	public final int length;
	/** Extra forward push per tick on top of a vanilla boat's 0.04 while the helm holds forward. */
	public final double thrust;
	/** The helm stands on the stern castle: seat 0 is this much higher than the deck. */
	public final float helmHeight;
	private final float[][] seats;

	ShipClass(String id, float width, float deck, int length, double thrust, float helmHeight, float[][] seats) {
		this.id = id;
		this.width = width;
		this.deck = deck;
		this.length = length;
		this.thrust = thrust;
		this.helmHeight = helmHeight;
		this.seats = seats;
	}

	public int seats() {
		return seats.length;
	}

	/** Seat {@code i}: {sideways, forward} in blocks. */
	public float[] seat(int i) {
		return seats[Math.min(i, seats.length - 1)];
	}

	public long price() {
		return GrandLineMod.config().shipPrices[ordinal()];
	}

	public int minLevel() {
		return GrandLineMod.config().shipMinLevels[ordinal()];
	}

	public double maxHull() {
		return GrandLineMod.config().shipHull[ordinal()];
	}

	public Component displayName() {
		return Component.translatable("ship.grandline." + id);
	}

	public static ShipClass byId(String id) {
		for (ShipClass c : values()) {
			if (c.id.equals(id)) {
				return c;
			}
		}
		return null;
	}
}
