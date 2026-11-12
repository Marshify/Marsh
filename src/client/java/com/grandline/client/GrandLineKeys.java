package com.grandline.client;

import com.grandline.client.network.ClientNetworking;
import com.grandline.client.screen.StatsScreen;
import com.grandline.combat.ability.PowerSource;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** Grand Line key bindings (rebindable in Options → Controls → Key Binds → Grand Line). */
public final class GrandLineKeys {
	private static final String CATEGORY = "key.categories.grandline";

	public static final KeyMapping STATS = new KeyMapping("key.grandline.stats", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, CATEGORY);
	public static final KeyMapping SWITCH_POWER = new KeyMapping("key.grandline.switch_power", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, CATEGORY);
	public static final KeyMapping[] MOVES = {
			new KeyMapping("key.grandline.move_1", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Z, CATEGORY),
			new KeyMapping("key.grandline.move_2", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_X, CATEGORY),
			new KeyMapping("key.grandline.move_3", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_C, CATEGORY),
			new KeyMapping("key.grandline.move_4", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, CATEGORY)
	};

	/** Which power the move keys currently use. Purely a client display/selection choice; the server checks ownership. */
	private static PowerSource activeSource = PowerSource.STYLE;

	private GrandLineKeys() {
	}

	public static PowerSource activeSource() {
		// Fall back to the style if the fruit was lost (e.g. replaced or removed).
		if (activeSource == PowerSource.FRUIT && (ClientProfileCache.profile() == null || !ClientProfileCache.profile().powers().hasFruit())) {
			activeSource = PowerSource.STYLE;
		}
		return activeSource;
	}

	public static void register() {
		KeyBindingHelper.registerKeyBinding(STATS);
		KeyBindingHelper.registerKeyBinding(SWITCH_POWER);
		for (KeyMapping move : MOVES) {
			KeyBindingHelper.registerKeyBinding(move);
		}

		ClientTickEvents.END_CLIENT_TICK.register(GrandLineKeys::tick);
	}

	private static void tick(Minecraft client) {
		while (STATS.consumeClick()) {
			if (client.screen == null && client.player != null) {
				client.setScreen(new StatsScreen());
			}
		}

		while (SWITCH_POWER.consumeClick()) {
			if (client.player == null) {
				continue;
			}
			if (activeSource() == PowerSource.STYLE && ClientProfileCache.profile() != null && ClientProfileCache.profile().powers().hasFruit()) {
				activeSource = PowerSource.FRUIT;
			} else if (activeSource() == PowerSource.FRUIT) {
				activeSource = PowerSource.STYLE;
			} else {
				client.player.displayClientMessage(Component.translatable("message.grandline.ability.no_fruit").withStyle(ChatFormatting.RED), true);
			}
		}

		for (int slot = 0; slot < MOVES.length; slot++) {
			while (MOVES[slot].consumeClick()) {
				if (client.player != null && client.screen == null) {
					ClientNetworking.requestAbilityActivation(activeSource(), slot);
				}
			}
		}
	}
}
