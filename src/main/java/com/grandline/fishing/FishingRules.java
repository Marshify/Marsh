package com.grandline.fishing;

import com.grandline.GrandLineMod;
import com.grandline.npc.FishingService;
import com.grandline.world.IslandRegistry;
import com.grandline.world.SeaLayout;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * What can be caught, where, and how hard it fights. Each species has a rarity tier, a difficulty (1 easy .. 5 brutal),
 * a movement pattern for the catch bar, a home water (anywhere, open sea far from islands, or later seas), a base
 * weight and the Fishing Mastery level that unlocks it. Weights and unlock levels can be overridden in the config
 * ({@code fishWeights}, {@code fishMinMastery}, keyed by item id path).
 */
public final class FishingRules {
	/** How the fish moves on the catch bar. */
	public enum Pattern { SMOOTH, DART, FLOATER, SINKER, MIXED }

	/** Where it lives: near any shore, only in open water (far from islands), or only from the given sea onward. */
	public enum Water { ANY, OPEN_SEA }

	public enum Tier {
		COMMON(1, 0xD8D8D8), UNCOMMON(2, 0x6FE06F), RARE(4, 0x58A8FF), EPIC(8, 0xC070FF), LEGENDARY(16, 0xFFB43C);

		public final int xpMultiplier;
		public final int color;

		Tier(int xpMultiplier, int color) {
			this.xpMultiplier = xpMultiplier;
			this.color = color;
		}
	}

	public record Fish(Item item, Tier tier, double difficulty, Pattern pattern, Water water, int minSea, int weight, int minMastery) {
		public String key() {
			return BuiltInRegistries.ITEM.getKey(item).getPath();
		}
	}

	public static final List<Fish> FISH = List.of(
			new Fish(FishingService.SILVERFIN, Tier.COMMON, 1.0, Pattern.SMOOTH, Water.ANY, 1, 40, 1),
			new Fish(FishingService.REEF_SNAPPER, Tier.COMMON, 1.6, Pattern.FLOATER, Water.ANY, 1, 28, 1),
			new Fish(FishingService.SUNSCALE_TANG, Tier.UNCOMMON, 2.2, Pattern.SMOOTH, Water.ANY, 1, 16, 3),
			new Fish(FishingService.STORM_EEL, Tier.UNCOMMON, 3.0, Pattern.DART, Water.ANY, 1, 10, 6),
			new Fish(FishingService.GLASS_SQUID, Tier.RARE, 3.2, Pattern.SINKER, Water.ANY, 1, 6, 10),
			new Fish(FishingService.EMBER_KOI, Tier.RARE, 3.6, Pattern.MIXED, Water.ANY, 1, 4, 14),
			new Fish(FishingService.ABYSS_ANGLER, Tier.EPIC, 4.4, Pattern.SINKER, Water.OPEN_SEA, 1, 3, 20),
			new Fish(FishingService.CROWN_MARLIN, Tier.LEGENDARY, 5.0, Pattern.DART, Water.OPEN_SEA, 2, 1, 30));

	private FishingRules() {
	}

	public static Fish byItem(Item item) {
		for (Fish f : FISH) {
			if (f.item() == item) {
				return f;
			}
		}
		return null;
	}

	static int weight(Fish fish) {
		Map<String, Integer> overrides = GrandLineMod.config().fishWeights;
		return overrides != null && overrides.containsKey(fish.key()) ? Math.max(0, overrides.get(fish.key())) : fish.weight();
	}

	static int minMastery(Fish fish) {
		Map<String, Integer> overrides = GrandLineMod.config().fishMinMastery;
		return overrides != null && overrides.containsKey(fish.key()) ? Math.max(1, overrides.get(fish.key())) : fish.minMastery();
	}

	/** Open water: no registered island within 120 blocks. */
	public static boolean openSea(ServerLevel level, Vec3 at) {
		return IslandRegistry.nearby(level.getServer(), (int) at.x, (int) at.z, 120).isEmpty();
	}

	/**
	 * The fish that bites at {@code at} for a player of this mastery: species they have unlocked that live in this
	 * water, weighted (rarer fish weigh more with mastery, and open water favours big game).
	 */
	public static Fish roll(ServerLevel level, Vec3 at, int mastery, RandomSource random) {
		int sea = SeaLayout.seaAt(at.x);
		boolean open = openSea(level, at);
		List<Fish> pool = new ArrayList<>();
		List<Double> weights = new ArrayList<>();
		double total = 0;
		for (Fish f : FISH) {
			if (mastery < minMastery(f) || sea < f.minSea() || (f.water() == Water.OPEN_SEA && !open)) {
				continue;
			}
			double w = weight(f) * (1 + mastery * 0.02 * f.tier().ordinal());
			if (open && f.tier().ordinal() >= Tier.RARE.ordinal()) {
				w *= 2;
			}
			w *= 1 + (sea - 1) * 0.5 * f.tier().ordinal();
			if (w > 0) {
				pool.add(f);
				weights.add(w);
				total += w;
			}
		}
		if (pool.isEmpty()) {
			return FISH.get(0);
		}
		double pick = random.nextDouble() * total;
		for (int i = 0; i < pool.size(); i++) {
			pick -= weights.get(i);
			if (pick <= 0) {
				return pool.get(i);
			}
		}
		return pool.get(pool.size() - 1);
	}

	// ---- mastery ----

	public static long xpToNext(int level) {
		return 100 + 40L * (level - 1);
	}

	public static int maxLevel() {
		return 100;
	}

	/** Mastery experience for landing this fish: rarer and harder fish teach more. */
	public static long catchXp(Fish fish) {
		return Math.round(10 * fish.tier().xpMultiplier * (0.6 + 0.2 * fish.difficulty()) * GrandLineMod.config().fishingMasteryXpMultiplier);
	}

	/** Catch zone height (fraction of the bar): bigger with mastery, smaller for hard fish. */
	public static double zoneSize(int mastery, double difficulty) {
		return Math.min(0.42, 0.24 + mastery * 0.0025) - difficulty * 0.012;
	}

	/** Ticks until a bite: shorter with mastery. */
	public static int biteDelay(int mastery, RandomSource random) {
		var config = GrandLineMod.config();
		double min = config.fishingBiteMinSeconds, max = Math.max(min, config.fishingBiteMaxSeconds);
		double s = min + random.nextDouble() * (max - min);
		return (int) Math.round(s * 20 * Math.max(0.45, 1 - mastery * 0.008));
	}

	/** How long the player has to hook a bite, in ticks: longer with mastery. */
	public static int hookWindow(int mastery) {
		return 18 + Math.min(14, mastery / 3);
	}
}
