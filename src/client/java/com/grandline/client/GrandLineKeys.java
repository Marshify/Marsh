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
	public static final KeyMapping STASH = new KeyMapping("key.grandline.stash", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, CATEGORY);
	public static final KeyMapping MAP = new KeyMapping("key.grandline.map", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_M, CATEGORY);
	public static final KeyMapping SWITCH_POWER = new KeyMapping("key.grandline.switch_power", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, CATEGORY);
	public static final KeyMapping[] MOVES = {
			new KeyMapping("key.grandline.move_1", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Z, CATEGORY),
			new KeyMapping("key.grandline.move_2", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_X, CATEGORY),
			new KeyMapping("key.grandline.move_3", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_C, CATEGORY),
			new KeyMapping("key.grandline.move_4", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, CATEGORY),
			new KeyMapping("key.grandline.move_5", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, CATEGORY)
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
		KeyBindingHelper.registerKeyBinding(STASH);
		KeyBindingHelper.registerKeyBinding(MAP);
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

		while (MAP.consumeClick()) {
			if (client.screen == null && client.player != null) {
				client.setScreen(new com.grandline.client.screen.WorldMapScreen());
			}
		}

		while (STASH.consumeClick()) {
			if (client.screen == null && client.player != null) {
				client.setScreen(new com.grandline.client.screen.StashScreen());
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

		// Move keys are read from the physical key state instead of consumeClick(): in 1.20.1 only one KeyMapping can own
		// a key, and vanilla already binds X and C (toolbar save/load), so their clicks would never reach us. The bound
		// key is still read from our mapping, so rebinding in Controls works.
		long window = client.getWindow().getWindow();
		for (int slot = 0; slot < MOVES.length; slot++) {
			InputConstants.Key key = KeyBindingHelper.getBoundKeyOf(MOVES[slot]);
			boolean down = key.getType() == InputConstants.Type.KEYSYM && key.getValue() != InputConstants.UNKNOWN.getValue()
					&& InputConstants.isKeyDown(window, key.getValue());
			while (MOVES[slot].consumeClick()) {
				down = true; // also honour clicks if we do own the key (e.g. after rebinding to a free key)
			}
			if (down && !moveKeyWasDown[slot] && client.player != null && client.screen == null) {
				ClientNetworking.requestAbilityActivation(activeSource(), slot);
				heldSource[slot] = activeSource();
			}
			// Letting go of a hold-to-charge move fires it (the server times the charge itself).
			if (!down && moveKeyWasDown[slot] && heldSource[slot] != null && isCharged(heldSource[slot], slot)) {
				ClientNetworking.requestAbilityRelease(heldSource[slot], slot);
			}
			if (!down) {
				heldSource[slot] = null;
			}
			moveKeyWasDown[slot] = down;
		}
	}

	private static final boolean[] moveKeyWasDown = new boolean[5];
	private static final PowerSource[] heldSource = new PowerSource[5];

	private static boolean isCharged(PowerSource source, int slot) {
		var profile = ClientProfileCache.profile();
		if (profile == null) {
			return false;
		}
		var power = com.grandline.combat.ability.Powers.get(source == PowerSource.FRUIT ? profile.powers().fruit() : profile.powers().fightingStyle());
		return power != null && slot < power.abilities().size()
				&& power.abilities().get(slot).activation() == com.grandline.combat.ability.AbilityDef.Activation.CHARGED;
	}
}
