package com.grandline.client;

import com.grandline.client.hud.ProfileHud;
import com.grandline.client.network.ClientNetworking;
import net.fabricmc.api.ClientModInitializer;

/**
 * Client-only entrypoint. Lives in the {@code client} source set, so the common/server code cannot accidentally
 * reference client classes (which do not exist on a dedicated server).
 */
public final class GrandLineClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ClientNetworking.register();
		GrandLineClientCommands.register();
		GrandLineKeys.register();
		ProfileHud.register();
	}
}
