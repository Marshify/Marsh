package com.grandline.test;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.grandline.fruit.FruitService;
import com.grandline.fruit.Fruits;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Checks the 42-fruit roster against {@code fruit_roster.json}: every fruit is registered under the expected id,
 * shows the expected name, its item model points at its texture, and that texture is byte-for-byte the approved
 * icon from {@code art/fruit_icons}. Legacy fruits must stay registered but out of every pool.
 */
public final class GrandLineRosterGameTests implements FabricGameTest {
	@GameTest(template = EMPTY_STRUCTURE, batch = "roster")
	public void everyFruitHasItsNameAndApprovedIcon(GameTestHelper helper) {
		JsonArray roster = json("/fruit_roster.json").getAsJsonArray();
		JsonObject lang = json("/assets/grandline/lang/en_us.json").getAsJsonObject();
		Path icons = projectRoot().resolve("art/fruit_icons");
		helper.assertTrue(roster.size() == 42, "roster file has " + roster.size() + " entries");
		helper.assertTrue(Fruits.all().size() == 42, "active fruits: " + Fruits.all().size());
		Set<String> active = new HashSet<>();
		Fruits.all().forEach(f -> active.add(f.id()));
		for (JsonElement element : roster) {
			JsonObject entry = element.getAsJsonObject();
			String id = entry.get("id").getAsString();
			String itemId = entry.get("item").getAsString();
			Fruits.FruitDef fruit = Fruits.get(id);
			helper.assertTrue(fruit != null && active.contains(id), id + " is an active fruit");
			helper.assertTrue(BuiltInRegistries.ITEM.getKey(fruit.item()).toString().equals(itemId), id + " item id");
			ResourceLocation item = new ResourceLocation(itemId);
			String nameKey = "item." + item.getNamespace() + "." + item.getPath();
			helper.assertTrue(lang.has(nameKey) && lang.get(nameKey).getAsString().equals(entry.get("name").getAsString()),
					nameKey + " should be " + entry.get("name").getAsString());
			JsonObject model = json("/assets/grandline/models/item/" + item.getPath() + ".json").getAsJsonObject();
			String layer = model.getAsJsonObject("textures").get("layer0").getAsString();
			helper.assertTrue(layer.equals("grandline:item/" + item.getPath()), itemId + " model points at " + layer);
			byte[] texture = bytes("/assets/grandline/textures/item/" + item.getPath() + ".png");
			byte[] approved = read(icons.resolve(entry.get("icon").getAsString()));
			helper.assertTrue(Arrays.equals(texture, approved), itemId + " texture is not the approved " + entry.get("icon").getAsString());
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "roster_legacy")
	public void legacyFruitsStayLoadableButNeverDrop(GameTestHelper helper) {
		long legacy = Fruits.allIncludingLegacy().stream().filter(Fruits::isLegacy).count();
		helper.assertTrue(legacy == 24, "legacy fruits: " + legacy);
		for (Fruits.FruitDef fruit : Fruits.allIncludingLegacy()) {
			if (Fruits.isLegacy(fruit)) {
				helper.assertTrue(Fruits.get(fruit.id()) == fruit, fruit.id() + " still resolves for old saves");
				helper.assertTrue(!Fruits.all().contains(fruit), fruit.id() + " is out of the active pools");
				helper.assertTrue(!FruitService.dealerStock(helper.getLevel().getServer()).contains(fruit), fruit.id() + " not sold");
			}
		}
		helper.succeed();
	}

	private static JsonElement json(String resource) {
		return JsonParser.parseString(new String(bytes(resource), StandardCharsets.UTF_8));
	}

	private static byte[] bytes(String resource) {
		try (InputStream in = GrandLineRosterGameTests.class.getResourceAsStream(resource)) {
			if (in == null) {
				throw new IllegalStateException("missing resource " + resource);
			}
			return in.readAllBytes();
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	private static byte[] read(Path path) {
		try {
			return Files.readAllBytes(path);
		} catch (IOException e) {
			throw new IllegalStateException("missing approved icon " + path, e);
		}
	}

	/** The game test server runs inside the project's run directory; walk up to the folder with art/. */
	private static Path projectRoot() {
		Path dir = Path.of("").toAbsolutePath();
		while (dir != null && !Files.isDirectory(dir.resolve("art/fruit_icons"))) {
			dir = dir.getParent();
		}
		if (dir == null) {
			throw new IllegalStateException("art/fruit_icons not found above " + Path.of("").toAbsolutePath());
		}
		return dir;
	}
}
