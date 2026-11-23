package com.grandline.client.audio;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.grandline.GrandLineMod;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The soundtrack assignments, read from assets/grandline/music/soundtrack.json (written by tools/music/prepare.py from
 * tools/music/soundtrack.json). Tracks are named by id ("island.millbrook_village"); only tracks with a prepared audio
 * file are listed under "tracks", so an assignment to a track that is not in the game yet simply plays nothing.
 * Islands are matched by their registry name (the original name, whatever names.json displays). Reloaded with the
 * resource packs, so a resource pack can replace or add music without code.
 */
public final class SoundtrackData {
	public static final ResourceLocation FILE = GrandLineMod.id("music/soundtrack.json");

	/** A special area of an island with its own track: from {@code minY} up (and to {@code maxY}), optionally only
	 * inside an island-relative box. */
	public record Region(String island, String track, int minY, int maxY, int[] box) {
		boolean contains(String islandName, double x, double y, double z, int islandX, int islandZ) {
			if (!island.equals(islandName) || y < minY || y > maxY) {
				return false;
			}
			if (box == null) {
				return true;
			}
			double lx = x - islandX, lz = z - islandZ;
			return lx >= box[0] && lz >= box[1] && lx <= box[2] && lz <= box[3];
		}
	}

	private static Map<String, ResourceLocation> tracks = Map.of();
	private static Map<String, String> islands = Map.of();
	private static List<Region> regions = List.of();
	private static Map<Integer, String> ocean = Map.of();
	private static Map<Integer, Map<String, String>> bosses = Map.of();

	private SoundtrackData() {
	}

	public static void register() {
		ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
			@Override
			public ResourceLocation getFabricId() {
				return GrandLineMod.id("soundtrack");
			}

			@Override
			public void onResourceManagerReload(ResourceManager manager) {
				load(manager);
			}
		});
	}

	static void load(ResourceManager manager) {
		Map<String, ResourceLocation> t = new HashMap<>();
		Map<String, String> is = new HashMap<>();
		List<Region> rs = new ArrayList<>();
		Map<Integer, String> oc = new HashMap<>();
		Map<Integer, Map<String, String>> bs = new HashMap<>();
		var resource = manager.getResource(FILE);
		if (resource.isPresent()) {
			try (Reader reader = resource.get().openAsReader()) {
				JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
				for (var e : obj(root, "tracks").entrySet()) {
					t.put(e.getKey(), new ResourceLocation(e.getValue().getAsString()));
				}
				for (var e : obj(root, "islands").entrySet()) {
					is.put(e.getKey(), e.getValue().getAsString());
				}
				if (root.has("regions")) {
					for (JsonElement el : root.getAsJsonArray("regions")) {
						JsonObject r = el.getAsJsonObject();
						int[] box = null;
						if (r.has("box")) {
							var a = r.getAsJsonArray("box");
							box = new int[]{a.get(0).getAsInt(), a.get(1).getAsInt(), a.get(2).getAsInt(), a.get(3).getAsInt()};
						}
						rs.add(new Region(r.get("island").getAsString(), r.get("track").getAsString(),
								r.has("min_y") ? r.get("min_y").getAsInt() : Integer.MIN_VALUE,
								r.has("max_y") ? r.get("max_y").getAsInt() : Integer.MAX_VALUE, box));
					}
				}
				for (var e : obj(root, "ocean").entrySet()) {
					oc.put(Integer.parseInt(e.getKey()), e.getValue().getAsString());
				}
				for (var e : obj(root, "bosses").entrySet()) {
					Map<String, String> tiers = new HashMap<>();
					for (var tier : e.getValue().getAsJsonObject().entrySet()) {
						tiers.put(tier.getKey(), tier.getValue().getAsString());
					}
					bs.put(Integer.parseInt(e.getKey()), tiers);
				}
			} catch (Exception e) {
				GrandLineMod.LOGGER.warn("Could not read {}: {}", FILE, e.toString());
			}
		}
		tracks = t;
		islands = is;
		regions = rs;
		ocean = oc;
		bosses = bs;
		GrandLineMod.LOGGER.info("Soundtrack: {} track(s) available, {} island assignment(s)", t.size(), is.size());
	}

	private static JsonObject obj(JsonObject root, String key) {
		return root.has(key) ? root.getAsJsonObject(key) : new JsonObject();
	}

	/** The sound event of a track, or null when the track is not in the game. */
	public static ResourceLocation sound(String track) {
		return track == null ? null : tracks.get(track);
	}

	/** Whether the island has a soundtrack assignment (in the game or still to come). */
	public static boolean hasIsland(String island) {
		return islands.containsKey(island);
	}

	public static String islandTrack(String island) {
		return islands.get(island);
	}

	public static String regionTrack(String island, double x, double y, double z, int islandX, int islandZ) {
		for (Region r : regions) {
			if (r.contains(island, x, y, z, islandX, islandZ)) {
				return r.track();
			}
		}
		return null;
	}

	public static String oceanTrack(int sea) {
		return ocean.getOrDefault(sea, ocean.get(1));
	}

	/** A boss tier's track for a sea (falls back to the First Sea's). */
	public static String bossTrack(int sea, int tier) {
		String name = switch (tier) {
			case 1 -> "standard";
			case 2 -> "major";
			case 3 -> "final";
			default -> null;
		};
		if (name == null) {
			return null;
		}
		Map<String, String> forSea = bosses.get(sea);
		String track = forSea != null ? forSea.get(name) : null;
		if (sound(track) == null && bosses.containsKey(1)) {
			track = bosses.get(1).get(name);
		}
		return track;
	}
}
