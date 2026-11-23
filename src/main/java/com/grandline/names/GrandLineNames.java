package com.grandline.names;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.grandline.GrandLineMod;
import com.grandline.world.SeaLayout;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * The mod's renamed names: islands, NPCs and enemies (everywhere, or on one island), items and abilities can be shown
 * under new names chosen by the mod's author.
 *
 * <p><b>Part of the mod.</b> The renames are the file {@code assets/grandline/names.json} inside the mod (in the source:
 * {@code src/main/resources/assets/grandline/names.json}), so every copy of every later version shows them, on every
 * server and in singleplayer. Players and server operators cannot change them: release jars have no command and read no
 * config file. The author changes them by editing that file in the repository (or, when running the mod from its source,
 * with the {@code /grandline names} command, which writes the file; see NamesCommand).
 *
 * <p><b>Display only.</b> Nothing is renamed inside the game: island names stay the ids that saves, quests and island
 * builds use; items, abilities and NPCs keep their registry ids and translation keys. Only what players read changes:
 * <ul>
 *   <li>items, abilities, NPCs and enemies: their translation keys show the new text ({@link NamesLanguage});</li>
 *   <li>islands: shown through {@link #island}/{@link #islandText} (and swapped inside translated text);</li>
 *   <li>an NPC or enemy renamed on one island only: named through {@link #npc}.</li>
 * </ul>
 * So new code keeps working unchanged: show an item, ability or NPC with its translation key as usual (an NPC or enemy
 * that belongs to an island through {@link #npc}), and an island name through {@link #island}.
 *
 * <p>Each section maps an <i>original</i> English name (as in the mod's en_us.json, or the island's name) to its new name.
 * An entry whose original no longer exists (the mod renamed it later) stays in the file and is reported in the log.
 */
public final class GrandLineNames {
	/** A kind of name, with the translation-key prefixes it covers. */
	public enum Category {
		ISLAND("islands"),
		NPC("npcs", "entity.grandline."),
		ITEM("items", "item.grandline.", "block.grandline."),
		ABILITY("abilities", "ability.grandline.", "power.grandline.", "technique.grandline.");

		public final String section;
		final String[] prefixes;

		Category(String section, String... prefixes) {
			this.section = section;
			this.prefixes = prefixes;
		}
	}

	/** The section for NPCs and enemies renamed on one island only: island name → (original name → new name). */
	public static final String ISLAND_NPCS = "island_npcs";
	/** Where the names are inside the mod. */
	public static final String RESOURCE = "/assets/grandline/names.json";

	/** What is shown: translation key → new text, original island name → new name, island → (NPC key → new name). */
	public record Active(Map<String, String> keys, Map<String, String> islands, Map<String, Map<String, String>> islandNpcs) {
		public static final Active EMPTY = new Active(Map.of(), Map.of(), Map.of());
	}

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
	/** The renames as written in the file (kept even if unresolved). */
	private static final Map<Category, LinkedHashMap<String, String>> FILE = new EnumMap<>(Category.class);
	private static final LinkedHashMap<String, LinkedHashMap<String, String>> FILE_ISLAND_NPCS = new LinkedHashMap<>();
	private static volatile Active active;
	private static Map<String, String> originals;

	static {
		for (Category c : Category.values()) {
			FILE.put(c, new LinkedHashMap<>());
		}
	}

	private GrandLineNames() {
	}

	// ============================================================================================== display

	/** The name to show for island {@code original} (itself unless renamed). */
	public static String island(String original) {
		if (original == null) {
			return null;
		}
		String renamed = active().islands().get(original);
		return renamed != null ? renamed : original;
	}

	/** {@code text} with any renamed island names in it swapped for their new names (labels such as "Upper Cloudtop Isles"). */
	public static String islandText(String text) {
		Map<String, String> islands = active().islands();
		return islands.isEmpty() || text == null ? text : replaceIslands(text, islands);
	}

	/**
	 * The name of an NPC or enemy shown by translation key {@code key} that belongs to island {@code islandName} (may be
	 * null): its name on that island if it has one there, else the usual (possibly renamed) name.
	 */
	public static MutableComponent npc(String key, String islandName) {
		if (islandName != null) {
			Map<String, String> here = active().islandNpcs().get(islandName);
			String renamed = here == null ? null : here.get(key);
			if (renamed != null) {
				return Component.literal(renamed);
			}
		}
		return Component.translatable(key);
	}

	/** The (original) name of the island with id {@code islandId} in the running world, or null. */
	public static String islandNameOf(long islandId) {
		var registry = com.grandline.world.IslandRegistry.current();
		var island = registry == null ? null : registry.get(islandId);
		return island == null ? null : island.name();
	}

	static String replaceIslands(String text, Map<String, String> islands) {
		String out = text;
		for (Map.Entry<String, String> e : islands.entrySet()) {
			if (out.contains(e.getKey())) {
				out = out.replace(e.getKey(), e.getValue());
			}
		}
		return out;
	}

	/** The names in effect (read from the mod on first use). */
	public static Active active() {
		Active a = active;
		if (a == null) {
			synchronized (GrandLineNames.class) {
				if (active == null) {
					load();
				}
				a = active;
			}
		}
		return a;
	}

	// ============================================================================================== originals

	/** The mod's own English text per translation key (read from the jar, so it is the original even when renamed). */
	public static synchronized Map<String, String> originals() {
		if (originals == null) {
			Map<String, String> map = new LinkedHashMap<>();
			try (InputStream in = GrandLineNames.class.getResourceAsStream("/assets/grandline/lang/en_us.json")) {
				if (in != null) {
					JsonObject json = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
					for (Map.Entry<String, JsonElement> e : json.entrySet()) {
						if (e.getValue().isJsonPrimitive()) {
							map.put(e.getKey(), e.getValue().getAsString());
						}
					}
				}
			} catch (Exception e) {
				GrandLineMod.LOGGER.warn("Could not read the mod's names", e);
			}
			originals = Collections.unmodifiableMap(map);
		}
		return originals;
	}

	/** Every original name of {@code category} (sorted, no duplicates): what can be renamed. */
	public static List<String> namesOf(Category category) {
		Set<String> out = new java.util.TreeSet<>(String.CASE_INSENSITIVE_ORDER);
		if (category == Category.ISLAND) {
			for (SeaLayout.Spec spec : SeaLayout.all(com.grandline.world.IslandRegistry.CURRENT_LAYOUT)) {
				out.add(spec.name());
			}
		} else {
			for (Map.Entry<String, String> e : originals().entrySet()) {
				if (matches(category, e.getKey()) && isName(e.getValue())) {
					out.add(e.getValue());
				}
			}
		}
		return new ArrayList<>(out);
	}

	/** A short display name, not a sentence or description (only those are offered for renaming). */
	private static boolean isName(String text) {
		return !text.isBlank() && text.length() <= 40 && !text.contains("%") && !text.endsWith(".") && !text.contains("\n");
	}

	private static boolean matches(Category category, String key) {
		for (String p : category.prefixes) {
			if (key.startsWith(p)) {
				return true;
			}
		}
		return false;
	}

	/** The translation keys an original name of {@code category} stands for (a weapon's item also names its moves panel). */
	static Set<String> keysFor(Category category, String original) {
		Set<String> keys = new LinkedHashSet<>();
		for (Map.Entry<String, String> e : originals().entrySet()) {
			if (matches(category, e.getKey()) && e.getValue().equals(original)) {
				keys.add(e.getKey());
				if (e.getKey().startsWith("item.grandline.")) {
					String power = "power.grandline." + e.getKey().substring("item.grandline.".length());
					if (original.equals(originals().get(power))) {
						keys.add(power);
					}
				}
			}
		}
		return keys;
	}

	/** The original name {@code typed} refers to in {@code category} (an original or a new name, any case), or null. */
	public static synchronized String resolve(Category category, String typed) {
		active();
		String t = typed.trim();
		for (String name : namesOf(category)) {
			if (name.equalsIgnoreCase(t)) {
				return name;
			}
		}
		for (Map.Entry<String, String> e : FILE.get(category).entrySet()) {
			if (e.getValue().equalsIgnoreCase(t)) {
				return e.getKey();
			}
		}
		return null;
	}

	// ============================================================================================== changes (author only)

	public static synchronized Map<String, String> renames(Category category) {
		active();
		return new LinkedHashMap<>(FILE.get(category));
	}

	public static synchronized Map<String, Map<String, String>> islandNpcRenames() {
		active();
		Map<String, Map<String, String>> out = new LinkedHashMap<>();
		FILE_ISLAND_NPCS.forEach((k, v) -> out.put(k, new LinkedHashMap<>(v)));
		return out;
	}

	/** Renames {@code original} to {@code newName} and writes the mod's names file (when running from the source). */
	public static synchronized void rename(Category category, String original, String newName) {
		active();
		FILE.get(category).put(original, newName);
		changed();
	}

	/** Names NPC/enemy {@code original} as {@code newName} on island {@code islandName} only. */
	public static synchronized void renameOnIsland(String islandName, String original, String newName) {
		active();
		FILE_ISLAND_NPCS.computeIfAbsent(islandName, k -> new LinkedHashMap<>()).put(original, newName);
		changed();
	}

	public static synchronized boolean reset(Category category, String original) {
		active();
		boolean had = FILE.get(category).remove(original) != null;
		if (had) {
			changed();
		}
		return had;
	}

	public static synchronized boolean resetOnIsland(String islandName, String original) {
		active();
		Map<String, String> here = FILE_ISLAND_NPCS.get(islandName);
		boolean had = here != null && here.remove(original) != null;
		if (had) {
			if (here.isEmpty()) {
				FILE_ISLAND_NPCS.remove(islandName);
			}
			changed();
		}
		return had;
	}

	/** Re-reads the names (after the file was edited). */
	public static synchronized void reload() {
		load();
	}

	private static void changed() {
		save();
		rebuild();
	}

	/** Builds what is shown from the file; entries whose original no longer exists are kept but reported. */
	private static synchronized void rebuild() {
		Map<String, String> keys = new TreeMap<>();
		Map<String, String> islands = new LinkedHashMap<>();
		Set<String> islandNames = new java.util.HashSet<>(namesOf(Category.ISLAND));
		for (Category c : Category.values()) {
			for (Map.Entry<String, String> e : FILE.get(c).entrySet()) {
				if (c == Category.ISLAND) {
					if (islandNames.contains(e.getKey())) {
						islands.put(e.getKey(), e.getValue());
					} else {
						GrandLineMod.LOGGER.warn("names.json: no island is called \"{}\" (kept in the file, not used)", e.getKey());
					}
					continue;
				}
				Set<String> found = keysFor(c, e.getKey());
				if (found.isEmpty()) {
					GrandLineMod.LOGGER.warn("names.json: no {} is called \"{}\" (kept in the file, not used)", c.section, e.getKey());
				}
				for (String key : found) {
					keys.put(key, e.getValue());
				}
			}
		}
		Map<String, Map<String, String>> islandNpcs = new LinkedHashMap<>();
		for (Map.Entry<String, LinkedHashMap<String, String>> isl : FILE_ISLAND_NPCS.entrySet()) {
			if (!islandNames.contains(isl.getKey())) {
				GrandLineMod.LOGGER.warn("names.json: no island is called \"{}\" (its NPC names are kept, not used)", isl.getKey());
				continue;
			}
			Map<String, String> here = new LinkedHashMap<>();
			for (Map.Entry<String, String> e : isl.getValue().entrySet()) {
				Set<String> found = keysFor(Category.NPC, e.getKey());
				if (found.isEmpty()) {
					GrandLineMod.LOGGER.warn("names.json: no NPC is called \"{}\" (on {}: kept, not used)", e.getKey(), isl.getKey());
				}
				for (String key : found) {
					here.put(key, e.getValue());
				}
			}
			islandNpcs.put(isl.getKey(), Collections.unmodifiableMap(here));
		}
		// Longest island names first, so "Cloudtop Isles" is replaced before a shorter name inside it could be.
		List<Map.Entry<String, String>> sorted = new ArrayList<>(islands.entrySet());
		sorted.sort((a, b) -> Integer.compare(b.getKey().length(), a.getKey().length()));
		Map<String, String> ordered = new LinkedHashMap<>();
		sorted.forEach(e -> ordered.put(e.getKey(), e.getValue()));
		active = new Active(Collections.unmodifiableMap(keys), Collections.unmodifiableMap(ordered), Collections.unmodifiableMap(islandNpcs));
	}

	// ============================================================================================== file

	/**
	 * The names file in the mod's source ({@code src/main/resources/assets/grandline/names.json}), found when the game is
	 * run from the source folder; null in a normal install (then the copy inside the jar is used, read only).
	 */
	public static Path sourceFile() {
		boolean tests = System.getProperty("fabric-api.gametest") != null || System.getProperty("fabric-api.gametest.report-file") != null;
		if (!FabricLoader.getInstance().isDevelopmentEnvironment() || tests) {
			return null; // a normal install, or the automated tests (which must never change the real file)
		}
		Path dir = Path.of("").toAbsolutePath();
		for (int i = 0; i < 4 && dir != null; i++, dir = dir.getParent()) {
			Path candidate = dir.resolve("src/main/resources" + RESOURCE);
			if (Files.exists(dir.resolve("src/main/resources/fabric.mod.json"))) {
				return candidate;
			}
		}
		return null;
	}

	private static synchronized void load() {
		for (Category c : Category.values()) {
			FILE.get(c).clear();
		}
		FILE_ISLAND_NPCS.clear();
		Path source = sourceFile();
		try (Reader reader = source != null && Files.exists(source) ? Files.newBufferedReader(source, StandardCharsets.UTF_8) : bundled()) {
			if (reader != null) {
				JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
				for (Category c : Category.values()) {
					read(json.get(c.section), FILE.get(c));
				}
				JsonElement perIsland = json.get(ISLAND_NPCS);
				if (perIsland != null && perIsland.isJsonObject()) {
					for (Map.Entry<String, JsonElement> isl : perIsland.getAsJsonObject().entrySet()) {
						LinkedHashMap<String, String> here = new LinkedHashMap<>();
						read(isl.getValue(), here);
						if (!here.isEmpty()) {
							FILE_ISLAND_NPCS.put(isl.getKey(), here);
						}
					}
				}
			}
		} catch (Exception e) {
			GrandLineMod.LOGGER.error("Could not read the mod's names file: {}", e.getMessage());
		}
		rebuild();
	}

	private static Reader bundled() {
		InputStream in = GrandLineNames.class.getResourceAsStream(RESOURCE);
		return in == null ? null : new InputStreamReader(in, StandardCharsets.UTF_8);
	}

	private static void read(JsonElement section, Map<String, String> into) {
		if (section != null && section.isJsonObject()) {
			for (Map.Entry<String, JsonElement> e : section.getAsJsonObject().entrySet()) {
				String value = e.getValue().isJsonPrimitive() ? e.getValue().getAsString().trim() : "";
				if (!value.isEmpty() && !e.getKey().startsWith("_")) {
					into.put(e.getKey(), value);
				}
			}
		}
	}

	/** Writes the names into the mod's source file (only when running from the source; otherwise kept in memory only). */
	private static synchronized void save() {
		Path source = sourceFile();
		if (source == null) {
			GrandLineMod.LOGGER.warn("Names can only be saved when the mod runs from its source folder; this change lasts until restart");
			return;
		}
		JsonObject json = new JsonObject();
		JsonArray help = new JsonArray();
		help.add("Grand Line's renamed names. Part of the mod: every player of every later version sees them.");
		help.add("Each section maps an ORIGINAL name to the name shown. Delete a line to give the original name back.");
		help.add("island_npcs renames an NPC or enemy on one island only: { \"Verdant Wilds\": { \"Bandit\": \"Lion\" } }.");
		help.add("Only what players see changes: saves, quests and the mod's ids keep the original names.");
		json.add("_help", help);
		for (Category c : Category.values()) {
			JsonObject section = new JsonObject();
			FILE.get(c).forEach(section::addProperty);
			json.add(c.section, section);
		}
		JsonObject perIsland = new JsonObject();
		FILE_ISLAND_NPCS.forEach((isl, names) -> {
			JsonObject here = new JsonObject();
			names.forEach(here::addProperty);
			perIsland.add(isl, here);
		});
		json.add(ISLAND_NPCS, perIsland);
		try {
			Files.createDirectories(source.getParent());
			Files.writeString(source, GSON.toJson(json) + "\n", StandardCharsets.UTF_8);
		} catch (Exception e) {
			GrandLineMod.LOGGER.error("Could not save {}: {}", source, e.getMessage());
		}
	}
}
