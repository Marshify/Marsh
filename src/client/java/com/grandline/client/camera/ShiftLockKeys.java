package com.grandline.client.camera;

import com.grandline.client.settings.ClientSettings;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.level.GameType;
import org.lwjgl.glfw.GLFW;

/**
 * The camera's keys (Shift Lock on Left Alt, zoom on = and -), the reticle, and crouching: there is no player-made
 * crouching in Grand Line play. Vanilla's Sneak key gives Left Shift up to Dash (it can be bound to another key in
 * Controls); riding still gets off with Left Shift, creative flight and spectator still fly down with it.
 */
public final class ShiftLockKeys {
	private static final String CATEGORY = "key.categories.grandline";
	public static final KeyMapping SHIFT_LOCK = new KeyMapping("key.grandline.shift_lock", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_ALT, CATEGORY);
	public static final KeyMapping ZOOM_IN = new KeyMapping("key.grandline.zoom_in", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_EQUAL, CATEGORY);
	public static final KeyMapping ZOOM_OUT = new KeyMapping("key.grandline.zoom_out", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_MINUS, CATEGORY);

	private ShiftLockKeys() {
	}

	public static void register() {
		KeyBindingHelper.registerKeyBinding(SHIFT_LOCK);
		KeyBindingHelper.registerKeyBinding(ZOOM_IN);
		KeyBindingHelper.registerKeyBinding(ZOOM_OUT);
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			freeLeftShift(client);
			GrandLineCamera.onJoin();
		});
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (SHIFT_LOCK.consumeClick()) {
				if (client.screen == null) {
					GrandLineCamera.toggleShiftLock();
				}
			}
			while (ZOOM_IN.consumeClick()) {
				GrandLineCamera.zoom(-1);
			}
			while (ZOOM_OUT.consumeClick()) {
				GrandLineCamera.zoom(1);
			}
			GrandLineCamera.tick();
		});
		HudRenderCallback.EVENT.register(ShiftLockKeys::reticle);
	}

	/** Vanilla Sneak gives Left Shift up to Dash (players can bind Sneak to any other key in Controls). */
	private static void freeLeftShift(Minecraft mc) {
		ClientSettings s = ClientSettings.get();
		// Checked on every join: if Sneak shares Shift Lock's key (Controls reset, an old options file), only one of the
		// two would ever work, so Sneak gives the key up again.
		if (mc.options.keyShift.same(SHIFT_LOCK) || mc.options.keyShift.same(com.grandline.client.technique.TechniqueKeys.DASH)) {
			mc.options.keyShift.setKey(InputConstants.UNKNOWN);
			KeyMapping.resetMapping();
			mc.options.save();
		}
		s.sneakKeyFreed = true;
		s.save();
	}

	/**
	 * Whether the player may crouch this tick: only to get off what they ride, or (creative, spectator) to fly down. In
	 * survival and adventure on foot, never; the server enforces the same (see SneakRules).
	 */
	public static boolean sneakAllowed() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.gameMode == null) {
			return false;
		}
		boolean held = com.grandline.client.technique.TechniqueKeys.DASH.isDown() || mc.options.keyShift.isDown();
		GameType mode = mc.gameMode.getPlayerMode();
		if (mode == GameType.SPECTATOR) {
			return held;
		}
		if (mode == GameType.CREATIVE) {
			return held && mc.player.getAbilities().flying; // fly down; on the ground Shift is Dash
		}
		return mc.player.isPassenger() && held;
	}

	/** A subtle reticle at the screen centre: a ring with a dot while Shift Lock is on, a faint dot otherwise. */
	private static void reticle(GuiGraphics g, float partial) {
		Minecraft mc = Minecraft.getInstance();
		if (!GrandLineCamera.active() || mc.screen != null || mc.options.hideGui) {
			return;
		}
		int cx = g.guiWidth() / 2, cy = g.guiHeight() / 2;
		if (GrandLineCamera.shiftLock()) { // the ring follows the Shift Lock toggle only
			int c = 0xB4FFFFFF, shade = 0x50000000;
			g.fill(cx, cy, cx + 1, cy + 1, c);
			for (int[] t : new int[][]{{-5, 0, -3, 1}, {4, 0, 6, 1}, {0, -5, 1, -3}, {0, 4, 1, 6}}) {
				g.fill(cx + t[0] + 1, cy + t[1] + 1, cx + t[2] + 1, cy + t[3] + 1, shade);
				g.fill(cx + t[0], cy + t[1], cx + t[2], cy + t[3], c);
			}
		} else {
			g.fill(cx, cy, cx + 1, cy + 1, 0x78FFFFFF);
		}
	}
}
