package com.grandline.race;

import net.minecraft.server.level.ServerPlayer;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The V4 upgrade tree: three tiers per race, two choices per tier. Only one choice per tier can be owned, each tier
 * needs the one before it, and each costs Awakening Points (tier 1: 2, tier 2: 3, tier 3: 4 = 9, the most points a
 * player can hold). Points come from clearing the race's trial (first clear and repeats, capped). Every bonus is a
 * fixed amount added once: owning an upgrade can never stack with itself.
 */
public final class RaceUpgrades {
	public record Upgrade(String id, Race race, int tier, String key, double amount) {
		public int cost() {
			return tier + 1;
		}
	}

	public static final Map<String, Upgrade> ALL = new LinkedHashMap<>();

	static {
		// key: what it adds to. "damage"/"reduction"/"lifesteal"/"energy"/"cooldown" add while awakened;
		// "duration" adds seconds to the awakening; "gain" multiplies awakening energy gain; "always_damage" is permanent.
		add(Race.HUMAN, 1, "a", "damage", 0.08);
		add(Race.HUMAN, 1, "b", "duration", 4);
		add(Race.HUMAN, 2, "a", "gain", 0.25);
		add(Race.HUMAN, 2, "b", "reduction", 0.10);
		add(Race.HUMAN, 3, "a", "damage", 0.12);
		add(Race.HUMAN, 3, "b", "always_damage", 0.04);

		add(Race.SHARK, 1, "a", "reduction", 0.06);
		add(Race.SHARK, 1, "b", "duration", 4);
		add(Race.SHARK, 2, "a", "gain", 0.25);
		add(Race.SHARK, 2, "b", "damage", 0.10);
		add(Race.SHARK, 3, "a", "reduction", 0.08);
		add(Race.SHARK, 3, "b", "always_damage", 0.04);

		add(Race.ANGEL, 1, "a", "heal", 0.01);
		add(Race.ANGEL, 1, "b", "duration", 4);
		add(Race.ANGEL, 2, "a", "gain", 0.25);
		add(Race.ANGEL, 2, "b", "reduction", 0.08);
		add(Race.ANGEL, 3, "a", "heal", 0.015);
		add(Race.ANGEL, 3, "b", "always_damage", 0.04);

		add(Race.RABBIT, 1, "a", "speed", 0.10);
		add(Race.RABBIT, 1, "b", "duration", 4);
		add(Race.RABBIT, 2, "a", "gain", 0.25);
		add(Race.RABBIT, 2, "b", "damage", 0.10);
		add(Race.RABBIT, 3, "a", "speed", 0.15);
		add(Race.RABBIT, 3, "b", "always_damage", 0.04);

		add(Race.CYBORG, 1, "a", "energy", 0.10);
		add(Race.CYBORG, 1, "b", "duration", 4);
		add(Race.CYBORG, 2, "a", "gain", 0.25);
		add(Race.CYBORG, 2, "b", "reduction", 0.08);
		add(Race.CYBORG, 3, "a", "pulse", 6);
		add(Race.CYBORG, 3, "b", "always_damage", 0.04);

		add(Race.GHOUL, 1, "a", "lifesteal", 0.03);
		add(Race.GHOUL, 1, "b", "duration", 4);
		add(Race.GHOUL, 2, "a", "gain", 0.25);
		add(Race.GHOUL, 2, "b", "cooldown", 0.10);
		add(Race.GHOUL, 3, "a", "lifesteal", 0.04);
		add(Race.GHOUL, 3, "b", "always_damage", 0.04);
	}

	private RaceUpgrades() {
	}

	private static void add(Race race, int tier, String choice, String key, double amount) {
		String id = race.id() + "_t" + tier + choice;
		ALL.put(id, new Upgrade(id, race, tier, key, amount));
	}

	public static List<Upgrade> forRace(Race race) {
		return ALL.values().stream().filter(u -> u.race() == race).toList();
	}

	/** Sum of the player's owned upgrades for {@code key}, for their current race only. */
	public static double bonus(ServerPlayer player, String key) {
		return bonus(RaceService.get(player), key);
	}

	public static double bonus(RaceData data, String key) {
		double total = 0;
		for (String id : data.upgrades()) {
			Upgrade u = ALL.get(id);
			if (u != null && u.race() == data.kind() && u.key().equals(key)) {
				total += u.amount();
			}
		}
		return total;
	}

	public static int pointsSpent(RaceData data) {
		int spent = 0;
		for (String id : data.upgrades()) {
			Upgrade u = ALL.get(id);
			if (u != null) {
				spent += u.cost();
			}
		}
		return spent;
	}

	public static int pointsFree(RaceData data) {
		return (int) data.counter("awaken_points") - pointsSpent(data);
	}

	/** Why the upgrade can't be bought, or null if it can. */
	public static String problem(RaceData data, String id) {
		Upgrade u = ALL.get(id);
		if (u == null || u.race() != data.kind()) {
			return "race.grandline.upgrade.unknown";
		}
		if (data.stage() < 4) {
			return "race.grandline.upgrade.need_v4";
		}
		if (data.upgrades().contains(id)) {
			return "race.grandline.upgrade.owned";
		}
		for (String owned : data.upgrades()) {
			Upgrade o = ALL.get(owned);
			if (o != null && o.race() == u.race() && o.tier() == u.tier()) {
				return "race.grandline.upgrade.tier_taken";
			}
		}
		if (u.tier() > 1) {
			boolean previous = false;
			for (String owned : data.upgrades()) {
				Upgrade o = ALL.get(owned);
				if (o != null && o.race() == u.race() && o.tier() == u.tier() - 1) {
					previous = true;
				}
			}
			if (!previous) {
				return "race.grandline.upgrade.need_previous";
			}
		}
		if (pointsFree(data) < u.cost()) {
			return "race.grandline.upgrade.points";
		}
		return null;
	}
}
