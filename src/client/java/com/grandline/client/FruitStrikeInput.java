package com.grandline.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;

/** Held fruit M1 (e.g. Bomb's Blast Toss): reports the release of the attack button the press started. */
public final class FruitStrikeInput {
	private static boolean holding;

	private FruitStrikeInput() {
	}

	public static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(FruitStrikeInput::tick);
	}

	/** The press was sent for {@code fruit}: watch for the release if its M1 is held. */
	public static void pressed(String fruit) {
		holding = com.grandline.combat.ability.FruitStrikes.held(fruit);
	}

	private static void tick(Minecraft mc) {
		if (!holding) {
			return;
		}
		if (mc.player == null || !mc.options.keyAttack.isDown() || mc.screen != null) {
			holding = false;
			if (mc.player != null && ClientPlayNetworking.canSend(com.grandline.network.payload.FruitStrikePayload.TYPE)) {
				ClientPlayNetworking.send(new com.grandline.network.payload.FruitStrikePayload(true));
			}
		}
	}
}
