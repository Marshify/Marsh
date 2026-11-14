package com.grandline.client;

import com.grandline.client.fishing.FishingScreen;
import com.grandline.network.payload.FishingEndPayload;
import com.grandline.network.payload.FishingMasteryPayload;
import com.grandline.network.payload.FishingStartPayload;
import com.grandline.network.payload.FishingStatePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;

/** Client side of fishing: the player's Fishing Mastery (for menus) and the catch minigame screen. */
public final class ClientFishing {
	private static FishingMasteryPayload mastery = new FishingMasteryPayload(1, 0, 100, 0);

	private ClientFishing() {
	}

	public static int level() {
		return mastery.level();
	}

	public static FishingMasteryPayload mastery() {
		return mastery;
	}

	public static void register() {
		ClientPlayNetworking.registerGlobalReceiver(FishingMasteryPayload.TYPE, (packet, player, sender) -> mastery = packet);
		ClientPlayNetworking.registerGlobalReceiver(FishingStartPayload.TYPE, (packet, player, sender) ->
				Minecraft.getInstance().setScreen(new FishingScreen(packet)));
		ClientPlayNetworking.registerGlobalReceiver(FishingStatePayload.TYPE, (packet, player, sender) -> {
			if (Minecraft.getInstance().screen instanceof FishingScreen screen) {
				screen.update(packet);
			}
		});
		ClientPlayNetworking.registerGlobalReceiver(FishingEndPayload.TYPE, (packet, player, sender) -> {
			if (Minecraft.getInstance().screen instanceof FishingScreen screen) {
				screen.finish(packet);
			}
		});
	}
}
