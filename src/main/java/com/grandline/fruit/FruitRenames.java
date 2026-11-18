package com.grandline.fruit;

import java.util.HashMap;
import java.util.Map;

/**
 * Fruit ids that were renamed. Old saves load with the new ids: the equipped fruit, mastery and stash through
 * {@link com.grandline.player.PowerState}, and stored fruit items (inventories, chests, dropped items) through
 * {@link com.grandline.mixin.ItemStackRenameMixin}. The old ids are not registered any more, so they never show up in
 * commands.
 */
public final class FruitRenames {
	/** Old fruit name to new fruit name (without namespace). */
	private static final Map<String, String> NAMES = Map.ofEntries(
			Map.entry("ember", "flame"), Map.entry("gale", "spin"), Map.entry("blast", "bomb"),
			Map.entry("mist", "smoke"), Map.entry("frost", "ice"), Map.entry("dune", "sand"),
			Map.entry("umbra", "dark"), Map.entry("quartz", "diamond"), Map.entry("feather", "eagle"),
			Map.entry("nova", "light"), Map.entry("bounce", "rubber"), Map.entry("lava", "magma"),
			Map.entry("tremor", "quake"), Map.entry("echo", "sound"), Map.entry("volt", "lightning"),
			Map.entry("orbit", "gravity"), Map.entry("ink", "shadow"), Map.entry("toxin", "venom"));

	/** grandline:old to grandline:new, for power ids. */
	public static final Map<String, String> POWERS;
	/** grandline:old_fruit to grandline:new_fruit, for item ids. */
	public static final Map<String, String> ITEMS;

	static {
		Map<String, String> powers = new HashMap<>();
		Map<String, String> items = new HashMap<>();
		NAMES.forEach((from, to) -> {
			powers.put("grandline:" + from, "grandline:" + to);
			items.put("grandline:" + from + "_fruit", "grandline:" + to + "_fruit");
		});
		POWERS = Map.copyOf(powers);
		ITEMS = Map.copyOf(items);
	}

	private FruitRenames() {
	}

	public static String power(String id) {
		return id == null ? null : POWERS.getOrDefault(id, id);
	}

	public static String item(String id) {
		return id == null ? null : ITEMS.getOrDefault(id, id);
	}
}
