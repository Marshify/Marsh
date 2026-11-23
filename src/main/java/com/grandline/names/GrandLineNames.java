package com.grandline.names;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.grandline.GrandLineMod;
import com.grandline.network.payload.NamesSyncPayload;
import com.grandline.world.SeaLayout;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

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
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Renamed names: islands, NPCs (and enemies), items and abilities can be given new names, for good, without touching the
 * game's code or data.
 *
 * <p><b>Display only.</b> Nothing is renamed inside the game: island names stay the ids that saves, quests and island
 * builds use; items, abilities and NPCs keep their registry ids and translation keys. Only what players read changes:
 * <ul>
 *   <li>items, abilities, NPCs: their translation keys get the new text ({@link NamesLanguage} on the client);</li>
 *   <li>islands: shown through {@link #island}/{@link #islandText} (and swapped inside translated text).</li>
 * </ul>
 * So new code keeps working unchanged: show an item, ability or NPC with its translation key as usual, and an island
 * name through {@link #island} (or {@link #islandText} for text that contains one).
 *
 * <p><b>Where renames live.</b> {@code config/grandline-names.json}, a readable file with one section per kind, mapping
 * each <i>original</i> English name (as in the mod's en_us.json, or the island's name) to its new name. Keyed by the
 * original, so renaming twice or resetting is simple. Edited with {@code /grandline names ...} (see NamesCommand) or by
 * hand (then {@code /grandline names reload}). An entry whose original no longer exists (the mod renamed it) is kept in
 * the file and reported in the log, never dropped.
 *
 * <p><b>Multiplayer.</b> The server's file decides: it is sent to every player on join and on every change.
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

		public static Category bySection(String s) {
			for (Category c : values()) {
				if (c.section.equals(s) || c.name().equalsIgnoreCase(s)) {
					return c;
				}
			}
			return null;
		}
	}

	/** What is shown right now: translation key → new text, original island name → new island name. */
	public record Active(Map<String, String> keys, Map<String, String> islands) {
		public static final Active EMPTY = new Active(Map.of(), Map.of());
	}

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
	/** The renames as written in the file: per kind, original name → new name (kept even if unresolved). */
	private static final Map<Category, LinkedHashMap<String, String>> FILE = new EnumMap<>(Category.class);
	private static volatile Active active = Active.EMPTY;
	private static Map<String, String> originals;
	private static MinecraftServer server;

	static {
		for (Category c : Category.values()) {
			FILE.put(c, new LinkedHashMap<>());
		}
	}

	private GrandLineNames() {
	}

	public static void register() {
		ServerLifecycleEvents.SERVER_STARTING.register(s -> {
			server = s;
			load();
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(s -> server = null);
		ServerPlayConnectionEvents.JOIN.register((handler, sender, s) -> send(handler.getPlayer()));
	}

	// ============================================================================================== display

	/** The name to show for island {@code original} (itself unless renamed). */
	public static String island(String original) {
		if (original == null) {
			return null;
		}
		String renamed = active.islands().get(original);
		return renamed != null ? renamed : original;
	}

	/** {@code text} with any renamed island names in it swapped for their new names (labels such as "Upper Cloudtop Isles"). */
	public static String islandText(String text) {
		Map<String, String> islands = active.islands();
		return islands.isEmpty() || text == null ? text : replaceIslands(text, islands);
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

	public static Active active() {
		return active;
	}

	/** Client: the names the server sent. */
	public static void setActive(Active names) {
		active = names == null ? Active.EMPTY : names;
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

	/**
	 * The original name {@code typed} refers to in {@code category}: an original name (any letter case) or a new name it
	 * was given. Null if there is none.
	 */
	public static String resolve(Category category, String typed) {
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

	// ============================================================================================== changes

	/** The renames of {@code category} as in the file (original → new). */
	public static synchronized Map<String, String> renames(Category category) {
		return new LinkedHashMap<>(FILE.get(category));
	}

	/** Renames {@code original} (an original name of the category) to {@code newName}, saves and tells every player. */
	public static synchronized void rename(Category category, String original, String newName) {
		FILE.get(category).put(original, newName);
		changed();
	}

	/** Gives {@code original} its own name back. Returns false if it was not renamed. */
	public static synchronized boolean reset(Category category, String original) {
		boolean had = FILE.get(category).remove(original) != null;
		if (had) {
			changed();
		}
		return had;
	}

	/** Re-reads the file (after editing it by hand) and tells every player. */
	public static synchronized void reload() {
		load();
	}

	private static void changed() {
		save();
		rebuild();
		broadcast();
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
						GrandLineMod.LOGGER.warn("grandline-names.json: no island is called \"{}\" (kept in the file, not used)", e.getKey());
					}
					continue;
				}
				Set<String> found = keysFor(c, e.getKey());
				if (found.isEmpty()) {
					GrandLineMod.LOGGER.warn("grandline-names.json: no {} is called \"{}\" (kept in the file, not used)", c.section, e.getKey());
				}
				for (String key : found) {
					keys.put(key, e.getValue());
				}
			}
		}
		// Longest island names first, so "Cloudtop Isles" is replaced before a shorter name inside it could be.
		List<Map.Entry<String, String>> sorted = new ArrayList<>(islands.entrySet());
		sorted.sort((a, b) -> Integer.compare(b.getKey().length(), a.getKey().length()));
		Map<String, String> ordered = new LinkedHashMap<>();
		sorted.forEach(e -> ordered.put(e.getKey(), e.getValue()));
		active = new Active(Collections.unmodifiableMap(keys), Collections.unmodifiableMap(ordered));
	}

	// ============================================================================================== file

	public static Path file() {
		return FabricLoader.getInstance().getConfigDir().resolve("grandline-names.json");
	}

	private static synchronized void load() {
		for (Category c : Category.values()) {
			FILE.get(c).clear();
		}
		Path path = file();
		if (Files.exists(path)) {
			try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
				JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
				for (Category c : Category.values()) {
					JsonElement section = json.get(c.section);
					if (section != null && section.isJsonObject()) {
						for (Map.Entry<String, JsonElement> e : section.getAsJsonObject().entrySet()) {
							String value = e.getValue().isJsonPrimitive() ? e.getValue().getAsString().trim() : "";
							if (!value.isEmpty()) {
								FILE.get(c).put(e.getKey(), value);
							}
						}
					}
				}
			} catch (Exception e) {
				GrandLineMod.LOGGER.error("Could not read {} (names unchanged until it is fixed): {}", path, e.getMessage());
			}
		} else {
			save(); // a starting file with the instructions
		}
		rebuild();
		broadcast();
	}

	private static synchronized void save() {
		JsonObject json = new JsonObject();
		JsonArray help = new JsonArray();
		help.add("Rename things shown in Grand Line. Each section maps an ORIGINAL name to the name you want shown.");
		help.add("Example: \"islands\": { \"Saltmarket Docks\": \"Port Royal\" }, \"items\": { \"Katana\": \"Wado Blade\" }");
		help.add("Delete a line to give the original name back. Easier: in game, /grandline names set|reset|list|reload.");
		help.add("Only what players see changes; saves, quests and the mod's ids keep the original names.");
		json.add("_help", help);
		for (Category c : Category.values()) {
			JsonObject section = new JsonObject();
			FILE.get(c).forEach(section::addProperty);
			json.add(c.section, section);
		}
		try {
			Files.createDirectories(file().getParent());
			Files.writeString(file(), GSON.toJson(json), StandardCharsets.UTF_8);
		} catch (Exception e) {
			GrandLineMod.LOGGER.error("Could not save {}: {}", file(), e.getMessage());
		}
	}

	// ============================================================================================== sync

	private static NamesSyncPayload payload() {
		Active a = active;
		return new NamesSyncPayload(new LinkedHashMap<>(a.keys()), new LinkedHashMap<>(a.islands()));
	}

	private static void send(ServerPlayer player) {
		if (ServerPlayNetworking.canSend(player, NamesSyncPayload.TYPE)) {
			ServerPlayNetworking.send(player, payload());
		}
	}

	private static void broadcast() {
		MinecraftServer s = server;
		if (s != null && s.getPlayerList() != null) { // no player list while the server starts: nobody to tell yet
			s.execute(() -> {
				if (s.getPlayerList() != null) {
					s.getPlayerList().getPlayers().forEach(GrandLineNames::send);
				}
			});
		}
	}

	/** For messages: the lower-case section name of each kind ("islands", "npcs", ...). */
	public static String sections() {
		List<String> out = new ArrayList<>();
		for (Category c : Category.values()) {
			out.add(c.name().toLowerCase(Locale.ROOT));
		}
		return String.join(", ", out);
	}
}
