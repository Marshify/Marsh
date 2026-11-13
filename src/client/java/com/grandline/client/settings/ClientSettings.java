package com.grandline.client.settings;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.grandline.GrandLineMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/** Per-player client options (config/grandline-client.json): visuals, HUD panels and HUD positions. */
public final class ClientSettings {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("grandline-client.json");
	private static ClientSettings instance;

	/** Show move effects cast by other players. */
	public boolean allyVfx = true;
	/** Move-effect density: 0 low, 1 medium, 2 high. Secondary sparks, embers and smoke scale; core shapes always show. */
	public int fxDensity = 2;
	/** Shake the camera on nearby move impacts. */
	public boolean cameraShake = true;
	/** Show the move list (bottom right). */
	public boolean showMoveList = true;
	/** Show the active quest tracker. */
	public boolean showQuestTracker = true;
	/** Draw the Grand Line loadout hotbar instead of the vanilla hotbar. */
	public boolean customHotbar = true;
	/** Show the Menu button under the status bars. */
	public boolean showMenuButton = true;
	/** Set once the vanilla player list has been moved off Tab for the loadout editor (only done once). */
	public boolean playerListKeyMoved = false;
	/** HUD element offsets from their default positions, by element id ("status", "moves", "quest"). */
	public Map<String, int[]> hudOffsets = new HashMap<>();

	public static ClientSettings get() {
		if (instance == null) {
			instance = load();
		}
		return instance;
	}

	private static ClientSettings load() {
		try {
			if (Files.exists(FILE)) {
				ClientSettings loaded = GSON.fromJson(Files.readString(FILE), ClientSettings.class);
				if (loaded != null) {
					if (loaded.hudOffsets == null) {
						loaded.hudOffsets = new HashMap<>();
					}
					return loaded;
				}
			}
		} catch (IOException | RuntimeException e) {
			GrandLineMod.LOGGER.warn("Could not read {}: {}", FILE, e.toString());
		}
		return new ClientSettings();
	}

	public void save() {
		try {
			Files.writeString(FILE, GSON.toJson(this));
		} catch (IOException e) {
			GrandLineMod.LOGGER.warn("Could not save {}: {}", FILE, e.toString());
		}
	}

	public int[] offset(String element) {
		return hudOffsets.getOrDefault(element, new int[]{0, 0});
	}

	public void setOffset(String element, int dx, int dy) {
		hudOffsets.put(element, new int[]{dx, dy});
	}
}
