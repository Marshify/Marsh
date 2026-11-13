package com.grandline.client;

import com.grandline.network.payload.ShipSyncPayload;
import com.grandline.ship.ShipClass;

import java.util.List;

/** The local player's ships, as last sent by the server (for the Navigator dialog). */
public final class ClientShips {
	private static ShipSyncPayload state = new ShipSyncPayload(List.of(), "", 0);

	private ClientShips() {
	}

	public static void accept(ShipSyncPayload payload) {
		state = payload;
	}

	public static void clear() {
		state = new ShipSyncPayload(List.of(), "", 0);
	}

	public static boolean owns(ShipClass ship) {
		return state.owned().contains(ship.id);
	}
}
