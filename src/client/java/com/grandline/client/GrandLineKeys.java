package com.grandline.client;

import com.grandline.client.screen.StatsScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

/** Grand Line key bindings (rebindable in Options → Controls → Key Binds → Grand Line). */
public final class GrandLineKeys {
	public static final KeyMapping STATS = new KeyMapping("key.grandline.stats", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K,
			"key.categories.grandline");

	private GrandLineKeys() {
	}

	public static void register() {
		KeyBindingHelper.registerKeyBinding(STATS);
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (STATS.consumeClick()) {
				if (client.screen == null && client.player != null) {
					client.setScreen(new StatsScreen());
				}
			}
		});
	}
}
