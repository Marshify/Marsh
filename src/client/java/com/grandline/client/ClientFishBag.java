package com.grandline.client;

import java.util.Map;

/** This client's copy of the fish bag (from FishBagPayload). */
public final class ClientFishBag {
	private static Map<String, Integer> fish = Map.of();

	private ClientFishBag() {
	}

	public static void accept(Map<String, Integer> contents) {
		fish = Map.copyOf(contents);
	}

	public static Map<String, Integer> fish() {
		return fish;
	}

	public static void clear() {
		fish = Map.of();
	}
}
