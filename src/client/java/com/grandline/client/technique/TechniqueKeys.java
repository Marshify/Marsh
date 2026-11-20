package com.grandline.client.technique;

import com.grandline.client.settings.ClientSettings;
import com.grandline.network.payload.TechniqueActionPayload;
import com.grandline.technique.TechniqueService;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/**
 * Technique keys (Grand Line section of Controls): Dash on Left Shift, Flash Step on R, Aura on J, Instinct on E. Each
 * press sends one request; holding a key does not repeat it. The server decides what happens.
 *
 * <p>The first time this version runs, controls that were on those keys move once: Shift Lock to Left Alt, the race
 * ability to O, Call Ship to P (vanilla Social Interactions gives P up), and the vanilla inventory to Caps Lock. Any key
 * rebound afterwards in Controls is respected.
 */
public final class TechniqueKeys {
	private static final String CATEGORY = "key.categories.grandline";
	public static final KeyMapping DASH = new KeyMapping("key.grandline.dash", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_SHIFT, CATEGORY);
	public static final KeyMapping FLASH_STEP = new KeyMapping("key.grandline.flash_step", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, CATEGORY);
	public static final KeyMapping AURA = new KeyMapping("key.grandline.aura", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_J, CATEGORY);
	public static final KeyMapping INSTINCT = new KeyMapping("key.grandline.instinct", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_E, CATEGORY);

	private TechniqueKeys() {
	}

	public static void register() {
		KeyBindingHelper.registerKeyBinding(DASH);
		KeyBindingHelper.registerKeyBinding(FLASH_STEP);
		KeyBindingHelper.registerKeyBinding(AURA);
		KeyBindingHelper.registerKeyBinding(INSTINCT);
		ClientLifecycleEvents.CLIENT_STARTED.register(TechniqueKeys::moveConflictingKeys);
		ClientTickEvents.END_CLIENT_TICK.register(TechniqueKeys::tick);
	}

	private static void tick(Minecraft mc) {
		boolean free = mc.player != null && mc.screen == null && !mc.player.isSpectator();
		while (DASH.consumeClick()) {
			if (free && !mc.player.isPassenger() && !(mc.player.getAbilities().flying)) {
				// Movement keys held right now: W/S forward and back, A/D left and right (diagonals are normalized).
				float forward = (mc.options.keyUp.isDown() ? 1 : 0) - (mc.options.keyDown.isDown() ? 1 : 0);
				float strafe = (mc.options.keyLeft.isDown() ? 1 : 0) - (mc.options.keyRight.isDown() ? 1 : 0);
				send(TechniqueService.DASH, forward, strafe);
			}
		}
		while (FLASH_STEP.consumeClick()) {
			if (free) {
				com.grandline.client.camera.GrandLineCamera.faceCamera(true); // step to where the reticle points
				send(TechniqueService.FLASH_STEP, 0, 0);
			}
		}
		while (AURA.consumeClick()) {
			if (free) {
				send(TechniqueService.TOGGLE_AURA, 0, 0);
			}
		}
		while (INSTINCT.consumeClick()) {
			if (free) {
				send(TechniqueService.TOGGLE_INSTINCT, 0, 0);
			}
		}
	}

	private static void send(int action, float forward, float strafe) {
		if (ClientPlayNetworking.canSend(TechniqueActionPayload.TYPE)) {
			ClientPlayNetworking.send(new TechniqueActionPayload(action, forward, strafe));
		}
	}

	/** Moves, once, controls that sat on the new technique keys (see the class comment). */
	private static void moveConflictingKeys(Minecraft mc) {
		ClientSettings settings = ClientSettings.get();
		if (settings.techniqueKeysMoved) {
			return;
		}
		var options = mc.options;
		move(com.grandline.client.camera.ShiftLockKeys.SHIFT_LOCK, GLFW.GLFW_KEY_LEFT_SHIFT, GLFW.GLFW_KEY_LEFT_ALT);
		move(com.grandline.client.race.ClientRace.ABILITY, GLFW.GLFW_KEY_R, GLFW.GLFW_KEY_O);
		move(com.grandline.client.GrandLineKeys.CALL_SHIP, GLFW.GLFW_KEY_J, GLFW.GLFW_KEY_P);
		move(options.keySocialInteractions, GLFW.GLFW_KEY_P, -1);
		move(options.keyInventory, GLFW.GLFW_KEY_E, GLFW.GLFW_KEY_CAPS_LOCK);
		move(options.keyShift, GLFW.GLFW_KEY_LEFT_SHIFT, -1);
		KeyMapping.resetMapping();
		options.save();
		settings.techniqueKeysMoved = true;
		settings.save();
	}

	private static void move(KeyMapping key, int from, int to) {
		if (isOn(key, from)) {
			key.setKey(to < 0 ? InputConstants.UNKNOWN : InputConstants.Type.KEYSYM.getOrCreate(to));
		}
	}

	private static boolean isOn(KeyMapping key, int code) {
		return KeyBindingHelper.getBoundKeyOf(key).equals(InputConstants.Type.KEYSYM.getOrCreate(code));
	}
}
