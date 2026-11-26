package com.grandline.client.fx;

import com.grandline.client.settings.ClientSettings;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

/**
 * Developer-only VFX preview state shared by the browser screen and the {@code /glvfx} commands: which effect, at what
 * scale, colour and distance, optionally attached to the player, looping; plus the quality preset and the forced
 * shader-fallback toggle used for comparisons. Everything here is client-side and cosmetic.
 */
public final class VfxPreview {
	/** Colour presets the browser cycles through (null = the effect's own palette). */
	public static final Integer[] COLOURS = {null, 0xFF5A3C, 0xFFCE54, 0x6FE08A, 0x4AA0FF, 0xB07CFF, 0xFFFFFF, 0x30343A};
	public static final String[] COLOUR_NAMES = {"Default", "Red", "Gold", "Green", "Blue", "Violet", "White", "Dark"};

	public static String selected;
	public static float scale = 1;
	public static int colour;
	public static double distance = 5;
	public static boolean loop, anchored;
	private static VfxInstance current;
	private static int loopWait;
	/** Set by the command; the browser opens on the next tick (after the chat screen closes). */
	public static boolean openNext;

	private VfxPreview() {
	}

	public static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(mc -> {
			if (openNext && mc.screen == null) {
				openNext = false;
				mc.setScreen(new VfxBrowserScreen());
			}
			if (loop && selected != null && (current == null || current.cleaned)) {
				if (++loopWait > 10) {
					loopWait = 0;
					play();
				}
			}
		});
	}

	/** Developer tools: in a development environment, or for operators (permission level 2+). */
	public static boolean allowed() {
		Minecraft mc = Minecraft.getInstance();
		return FabricLoader.getInstance().isDevelopmentEnvironment() || mc.player != null && mc.player.hasPermissions(2);
	}

	/** Plays the selected effect in front of the player (or on the player when anchored). */
	public static VfxInstance play() {
		Minecraft mc = Minecraft.getInstance();
		if (selected == null || mc.player == null) {
			return null;
		}
		Vec3 look = mc.player.getLookAngle();
		Vec3 flat = new Vec3(look.x, 0, look.z).normalize();
		Vec3 at = anchored ? mc.player.position().add(0, 1, 0) : mc.player.position().add(flat.scale(distance)).add(0, 1, 0);
		VfxInstance.Params p = VfxInstance.Params.at(at).dir(anchored ? look : flat).scale(scale).colors(COLOURS[colour], null);
		if (anchored) {
			p.anchor(mc.player.getId());
		}
		current = VfxInstance.play(selected, p);
		return current;
	}

	public static String quality() {
		return switch (ClientSettings.get().fxDensity) {
			case 0 -> "Low";
			case 1 -> "Medium";
			case 2 -> "High";
			default -> "Ultra";
		};
	}

	public static void cycleQuality() {
		ClientSettings s = ClientSettings.get();
		s.fxDensity = (s.fxDensity + 1) % 4;
		s.save();
	}

	/** One-line diagnostics: active instances and particles, sprites, budget, render path. */
	public static String stats() {
		Minecraft mc = Minecraft.getInstance();
		return String.format("effects %d | particles %d/%d | sprites %d | %s | path: %s%s | fps %d", VfxInstance.activeInstances(),
				VfxInstance.activeParticles(), VfxInstance.particleBudget(), FxSprites.active(), quality(),
				WindRender.shaderPackActive() ? "shader pack (fallback)" : WindRender.forceFallback ? "forced fallback" : "Grand Line shaders",
				VfxLibrary.errors().isEmpty() ? "" : " | " + VfxLibrary.errors().size() + " load problems", mc.getFps());
	}
}
