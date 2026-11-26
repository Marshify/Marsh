package com.grandline.client.fx;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.grandline.GrandLineMod;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * The central VFX library: every reusable effect in Grand Line, by id and category.
 *
 * <p>Effects are defined in JSON ({@code assets/<namespace>/vfx/effects/<path>.json}, id {@code namespace:path}) and
 * reloaded with resource packs (F3+T), or registered from code with {@link #register}. An effect is a list of timed
 * layers (sprites, flipbooks, particle emitters, rings/shockwaves, beams, ribbons, slashes, custom meshes, procedural
 * built-ins, distortion, sound, camera shake) combined into one multilayered animation; {@link VfxInstance} plays it.
 * Gameplay code triggers effects by id (client: {@link VfxInstance#play}; server: {@code combat.fx.Vfx}). The JSON
 * schema is documented in docs/VFX.md.
 */
public final class VfxLibrary {
	/** Categories for browsing (any string is allowed; these are the conventional ones). */
	public static final List<String> CATEGORIES = List.of("combat", "elemental", "spatial", "environmental", "transformation", "progression", "aura",
			"imported", "other");

	// ------------------------------------------------------------------------------------------------ curves

	/** A keyframed float curve over normalised time 0..1 (linear, or one constant value). */
	public record Curve(float[] t, float[] v) {
		public static Curve of(float value) {
			return new Curve(new float[]{0}, new float[]{value});
		}

		public float at(float x) {
			if (t.length == 1 || x <= t[0]) {
				return v[0];
			}
			for (int i = 1; i < t.length; i++) {
				if (x <= t[i]) {
					float k = (x - t[i - 1]) / Math.max(1e-6f, t[i] - t[i - 1]);
					return v[i - 1] + (v[i] - v[i - 1]) * k;
				}
			}
			return v[v.length - 1];
		}

		static Curve parse(JsonElement e, float def) {
			if (e == null) {
				return of(def);
			}
			if (e.isJsonPrimitive()) {
				return of(e.getAsFloat());
			}
			JsonArray a = e.getAsJsonArray();
			if (a.size() == 2 && a.get(0).isJsonPrimitive() && a.get(1).isJsonPrimitive() && !a.get(0).isJsonArray()) {
				// [from, to] shorthand
				return new Curve(new float[]{0, 1}, new float[]{a.get(0).getAsFloat(), a.get(1).getAsFloat()});
			}
			float[] t = new float[a.size()], v = new float[a.size()];
			for (int i = 0; i < a.size(); i++) {
				JsonArray k = a.get(i).getAsJsonArray();
				t[i] = k.get(0).getAsFloat();
				v[i] = k.get(1).getAsFloat();
			}
			return new Curve(t, v);
		}
	}

	/**
	 * A colour + opacity gradient over normalised time. Keyframes {@code [t, "#RRGGBB" | "$primary" | "$secondary", alpha]};
	 * the {@code $} tokens take the colours passed at play time (so one effect serves many palettes).
	 */
	public record Gradient(float[] t, String[] colour, float[] alpha) {
		public static Gradient of(String colour, float alpha) {
			return new Gradient(new float[]{0}, new String[]{colour}, new float[]{alpha});
		}

		/** RGBA at {@code x}, resolving tokens against {@code primary}/{@code secondary}. */
		public float[] at(float x, int primary, int secondary) {
			int i = 0;
			while (i < t.length - 1 && x > t[i + 1]) {
				i++;
			}
			int j = Math.min(t.length - 1, i + 1);
			float k = j == i ? 0 : Math.max(0, Math.min(1, (x - t[i]) / Math.max(1e-6f, t[j] - t[i])));
			if (x <= t[0]) {
				k = 0;
				j = i = 0;
			}
			int a = resolve(colour[i], primary, secondary), b = resolve(colour[j], primary, secondary);
			float r = lerp((a >> 16 & 255) / 255f, (b >> 16 & 255) / 255f, k);
			float g = lerp((a >> 8 & 255) / 255f, (b >> 8 & 255) / 255f, k);
			float bl = lerp((a & 255) / 255f, (b & 255) / 255f, k);
			return new float[]{r, g, bl, lerp(alpha[i], alpha[j], k)};
		}

		static int resolve(String c, int primary, int secondary) {
			if (c.equals("$primary")) {
				return primary;
			}
			if (c.equals("$secondary")) {
				return secondary;
			}
			return Integer.parseInt(c.substring(c.startsWith("#") ? 1 : 0), 16);
		}

		static Gradient parse(JsonElement e) {
			if (e == null) {
				return of("#FFFFFF", 1);
			}
			if (e.isJsonPrimitive()) {
				return of(e.getAsString(), 1);
			}
			JsonArray a = e.getAsJsonArray();
			float[] t = new float[a.size()], al = new float[a.size()];
			String[] c = new String[a.size()];
			for (int i = 0; i < a.size(); i++) {
				JsonArray k = a.get(i).getAsJsonArray();
				t[i] = k.get(0).getAsFloat();
				c[i] = k.get(1).getAsString();
				al[i] = k.size() > 2 ? k.get(2).getAsFloat() : 1;
				resolve(c[i], 0, 0); // validate now
			}
			return new Gradient(t, c, al);
		}
	}

	static float lerp(float a, float b, float k) {
		return a + (b - a) * k;
	}

	// ------------------------------------------------------------------------------------------------ definitions

	/** One layer: a type plus its own settings (kept as JSON for the type-specific fields) and the common timing/look. */
	public record Layer(String type, int delay, int life, ResourceLocation texture, int columns, int rows, int frameTime, boolean additive,
			Curve size, Gradient colour, Curve alpha, float[] offset, String facing, float rotation, float spin, JsonObject json) {
		public float f(String key, float def) {
			return json.has(key) ? json.get(key).getAsFloat() : def;
		}

		public int i(String key, int def) {
			return json.has(key) ? json.get(key).getAsInt() : def;
		}

		public String s(String key, String def) {
			return json.has(key) ? json.get(key).getAsString() : def;
		}

		public boolean b(String key, boolean def) {
			return json.has(key) ? json.get(key).getAsBoolean() : def;
		}

		public Curve curve(String key, float def) {
			return Curve.parse(json.get(key), def);
		}

		/** A [min, max] range (or a single number). */
		public float[] range(String key, float lo, float hi) {
			JsonElement e = json.get(key);
			if (e == null) {
				return new float[]{lo, hi};
			}
			if (e.isJsonPrimitive()) {
				return new float[]{e.getAsFloat(), e.getAsFloat()};
			}
			return new float[]{e.getAsJsonArray().get(0).getAsFloat(), e.getAsJsonArray().get(1).getAsFloat()};
		}
	}

	/**
	 * A registered effect. {@code duration} in ticks (the instance lives until its last layer ends, at least this long);
	 * {@code primary}/{@code secondary} the default palette for {@code $} colour tokens.
	 */
	public record Definition(ResourceLocation id, String category, String description, int duration, double renderDistance, int primary,
			int secondary, List<Layer> layers, String source) {
		public int length() {
			int end = duration;
			for (Layer l : layers) {
				end = Math.max(end, l.delay() + l.life());
			}
			return end;
		}
	}

	private static final Map<ResourceLocation, Definition> CODE = new LinkedHashMap<>();
	private static final Map<ResourceLocation, Definition> LOADED = new LinkedHashMap<>();
	private static final List<String> ERRORS = new ArrayList<>();

	private VfxLibrary() {
	}

	public static void register() {
		ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
			@Override
			public ResourceLocation getFabricId() {
				return GrandLineMod.id("vfx_library");
			}

			@Override
			public void onResourceManagerReload(ResourceManager manager) {
				VfxLibrary.reload(manager);
			}
		});
	}

	/** Registers an effect from code (survives resource reloads; a JSON effect with the same id overrides it). */
	public static void register(Definition definition) {
		CODE.put(definition.id(), definition);
	}

	public static Definition get(ResourceLocation id) {
		Definition d = LOADED.get(id);
		return d != null ? d : CODE.get(id);
	}

	public static Definition get(String id) {
		ResourceLocation rl = ResourceLocation.tryParse(id.contains(":") ? id : GrandLineMod.MOD_ID + ":" + id);
		return rl == null ? null : get(rl);
	}

	/** All effects, sorted by id, grouped by category. */
	public static Map<String, List<Definition>> byCategory() {
		Map<String, List<Definition>> out = new TreeMap<>();
		Map<ResourceLocation, Definition> all = new TreeMap<>(CODE);
		all.putAll(LOADED);
		for (Definition d : all.values()) {
			out.computeIfAbsent(d.category(), k -> new ArrayList<>()).add(d);
		}
		return out;
	}

	public static int size() {
		Map<ResourceLocation, Definition> all = new LinkedHashMap<>(CODE);
		all.putAll(LOADED);
		return all.size();
	}

	/** Problems found by the last reload (bad JSON, unknown layer types, missing textures), for the browser and log. */
	public static List<String> errors() {
		return Collections.unmodifiableList(ERRORS);
	}

	static void reload(ResourceManager manager) {
		LOADED.clear();
		ERRORS.clear();
		VfxMeshes.clear();
		Map<ResourceLocation, Resource> found = manager.listResources("vfx/effects", rl -> rl.getPath().endsWith(".json"));
		for (Map.Entry<ResourceLocation, Resource> e : found.entrySet()) {
			ResourceLocation file = e.getKey();
			String path = file.getPath().substring("vfx/effects/".length(), file.getPath().length() - ".json".length());
			ResourceLocation id = new ResourceLocation(file.getNamespace(), path);
			try (Reader r = e.getValue().openAsReader()) {
				Definition d = parse(id, JsonParser.parseReader(r).getAsJsonObject(), file.toString());
				for (Layer l : d.layers()) {
					if (l.texture() != null && manager.getResource(l.texture()).isEmpty()) {
						ERRORS.add(id + ": missing texture " + l.texture());
					}
				}
				LOADED.put(id, d);
			} catch (Exception ex) {
				ERRORS.add(id + ": " + ex.getMessage());
			}
		}
		ERRORS.forEach(m -> GrandLineMod.LOGGER.warn("VFX: {}", m));
		GrandLineMod.LOGGER.info("VFX library: {} effects ({} from JSON, {} problems)", size(), LOADED.size(), ERRORS.size());
	}

	/** Parses one effect definition (public so code and tools can build effects from JSON text too). */
	public static Definition parse(ResourceLocation id, JsonObject o, String source) {
		List<Layer> layers = new ArrayList<>();
		for (JsonElement le : o.getAsJsonArray("layers")) {
			JsonObject l = le.getAsJsonObject();
			String type = l.get("type").getAsString();
			if (!VfxInstance.TYPES.contains(type)) {
				throw new IllegalArgumentException("unknown layer type '" + type + "' (known: " + VfxInstance.TYPES + ")");
			}
			ResourceLocation tex = l.has("texture") ? ResourceLocation.tryParse(l.get("texture").getAsString()) : null;
			int cols = 1, rows = 1;
			if (l.has("frames")) {
				JsonElement f = l.get("frames");
				if (f.isJsonArray()) {
					cols = f.getAsJsonArray().get(0).getAsInt();
					rows = f.getAsJsonArray().get(1).getAsInt();
				} else {
					cols = f.getAsInt();
				}
			}
			float[] off = {0, 0, 0};
			if (l.has("offset")) {
				JsonArray a = l.getAsJsonArray("offset");
				off = new float[]{a.get(0).getAsFloat(), a.get(1).getAsFloat(), a.get(2).getAsFloat()};
			}
			int life = l.has("life") ? l.get("life").getAsInt() : o.has("duration") ? o.get("duration").getAsInt() : 20;
			layers.add(new Layer(type, l.has("delay") ? l.get("delay").getAsInt() : 0, life, tex, cols, rows,
					l.has("frame_time") ? l.get("frame_time").getAsInt() : 0, !l.has("blend") || l.get("blend").getAsString().equals("additive"),
					Curve.parse(l.get("size"), 1), Gradient.parse(l.get("color")), Curve.parse(l.get("alpha"), 1), off,
					l.has("facing") ? l.get("facing").getAsString() : "camera", l.has("rotation") ? l.get("rotation").getAsFloat() : 0,
					l.has("spin") ? l.get("spin").getAsFloat() : 0, l));
		}
		int primary = o.has("primary") ? Gradient.resolve(o.get("primary").getAsString(), 0, 0) : 0xFFFFFF;
		int secondary = o.has("secondary") ? Gradient.resolve(o.get("secondary").getAsString(), 0, 0) : primary;
		return new Definition(id, o.has("category") ? o.get("category").getAsString() : "other", o.has("description") ? o.get("description").getAsString() : "",
				o.has("duration") ? o.get("duration").getAsInt() : 20, o.has("render_distance") ? o.get("render_distance").getAsDouble() : 96, primary,
				secondary, List.copyOf(layers), source);
	}
}
