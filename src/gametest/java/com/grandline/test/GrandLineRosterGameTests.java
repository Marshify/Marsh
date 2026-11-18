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
 * icon from {@code art/fruit_icons}. Saves naming removed fruits are cleaned on join.
 */
public final class GrandLineRosterGameTests implements FabricGameTest {
	@GameTest(template = EMPTY_STRUCTURE, batch = "roster")
	public void fruitIdsMatchNamesAndOldIdsMigrate(GameTestHelper helper) {
		JsonObject lang = json("/assets/grandline/lang/en_us.json").getAsJsonObject();
		for (Fruits.FruitDef fruit : Fruits.all()) {
			String path = new ResourceLocation(fruit.id()).getPath();
			String name = lang.get("power.grandline." + path).getAsString();
			String expected = name.toLowerCase(java.util.Locale.ROOT).replace('-', '_').replace(' ', '_');
			helper.assertTrue(path.equals(expected), fruit.id() + " should be named after " + name);
		}
		com.grandline.fruit.FruitRenames.ITEMS.forEach((from, to) -> {
			helper.assertFalse(BuiltInRegistries.ITEM.containsKey(new ResourceLocation(from)), from + " is still registered");
			var tag = new net.minecraft.nbt.CompoundTag();
			tag.putString("id", from);
			tag.putByte("Count", (byte) 3);
			var stack = net.minecraft.world.item.ItemStack.of(tag);
			helper.assertTrue(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(to) && stack.getCount() == 3,
					from + " loads as " + to);
		});
		com.grandline.fruit.FruitRenames.POWERS.forEach((from, to) -> {
			var old = new com.grandline.player.PowerState("grandline:brawler", from, java.util.Map.of(from, 7L), java.util.Map.of(from, 1), 0L);
			helper.assertTrue(old.fruit().equals(to) && old.mastery().get(to) == 7L && old.stash().get(to) == 1, from + " migrates to " + to);
			helper.assertTrue(Fruits.get(to) != null, to + " exists");
		});
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "roster")
	public void everyFruitHasItsNameAndApprovedIcon(GameTestHelper helper) {
		JsonArray roster = json("/fruit_roster.json").getAsJsonArray();
		JsonObject lang = json("/assets/grandline/lang/en_us.json").getAsJsonObject();
		Path icons = projectRoot().resolve("art/fruit_icons");
		helper.assertTrue(roster.size() == 42, "roster file has " + roster.size() + " entries");
		helper.assertTrue(Fruits.all().size() == 42, "fruits: " + Fruits.all().size());
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

	@GameTest(template = EMPTY_STRUCTURE, batch = "roster_removed")
	public void savesNamingRemovedFruitsAreCleanedUp(GameTestHelper helper) {
		helper.assertTrue(Fruits.get("grandline:thorn") == null, "retired fruits are gone");
		net.minecraft.server.level.ServerPlayer player = helper.makeMockServerPlayerInLevel();
		com.grandline.player.PlayerProfileService.update(player, p -> p.withPowers(p.powers().withFruit("grandline:solar")
				.withStashCount("grandline:thorn", 2).withStashCount(Fruits.ICE.id(), 1).withMasteryXp("grandline:solar", 900)));
		helper.assertTrue(FruitService.cleanUpRemovedFruits(player), "something was cleaned");
		var powers = com.grandline.player.PlayerProfileService.get(player).powers();
		helper.assertTrue(!powers.hasFruit(), "removed eaten fruit cleared");
		helper.assertTrue(powers.stashCount("grandline:thorn") == 0, "removed fruit dropped from the stash");
		helper.assertTrue(powers.stashCount(Fruits.ICE.id()) == 1, "real fruits stay in the stash");
		helper.assertTrue(!FruitService.cleanUpRemovedFruits(player), "second run changes nothing");
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
