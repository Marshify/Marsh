package com.grandline.world;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The hand-designed islands of the three Grand Line seas. Each sea is a separate area of ocean; ships carry players
 * between them. Positions are block coordinates of each island's centre.
 */
public final class SeaLayout {
	/** One designed island. {@code radius} is the beach radius in blocks. */
	public record Spec(int sea, String name, IslandTheme theme, int x, int z, int radius, int level, boolean starter) {
	}

	/** X offset of each sea's centre: seas are 12,000 blocks apart so they never meet. */
	public static final int[] SEA_CENTER_X = {0, 12_000, 24_000};
	/** Minimum level to sail to Sea 2 and Sea 3. */
	public static final int[] SEA_MIN_LEVEL = {1, 700, 1500};

	private static final List<Spec> ISLANDS = new ArrayList<>();

	static {
		// --- Sea 1 (levels 1-700). Layout follows a classic first-sea chart around a central starter town. ---
		s1("Dawnreach Harbor", IslandTheme.TOWN, 0, 0, 40, 1, true);
		s1("Verdant Wilds", IslandTheme.JUNGLE, -60, -400, 36, 15, false);
		s1("Millbrook Village", IslandTheme.VILLAGE, 630, 120, 34, 30, false);
		s1("Redbrick Port", IslandTheme.BRICK_TOWN, -630, 120, 34, 60, false);
		s1("Sunscorch Dunes", IslandTheme.DESERT, 570, 540, 36, 75, false);
		s1("Frostpeak Isle", IslandTheme.SNOW, 630, -420, 38, 90, false);
		s1("Ironhold Bastion", IslandTheme.FORTRESS, 1380, 0, 38, 120, false);
		s1("Cloudtop Isles", IslandTheme.SKY, -1500, -540, 30, 150, false);
		s1("Stonewall Prison", IslandTheme.PRISON, -1170, 270, 32, 190, false);
		s1("Gladiator's Ring", IslandTheme.COLOSSEUM, -630, -540, 38, 225, false);
		s1("Emberfall Isle", IslandTheme.VOLCANO, -1380, 660, 38, 300, false);
		s1("Saltmarket Docks", IslandTheme.HARBOR, -30, 540, 40, 375, false);
		s1("Lotus Palace", IslandTheme.PALACE, 1200, -630, 36, 450, false);
		s1("Crystal Fountain", IslandTheme.CRYSTAL, 1200, 660, 36, 625, false);

		// --- Sea 2 (levels 700-1500) ---
		s2("Kingsport Haven", IslandTheme.HARBOR, 0, 0, 42, 700, true);
		s2("Rosewater Town", IslandTheme.TOWN, 600, -300, 36, 725, false);
		s2("Mossgreen Ruins", IslandTheme.JUNGLE, -650, -250, 38, 775, false);
		s2("Glacier Rift", IslandTheme.SNOW, -500, 550, 38, 850, false);
		s2("Scarlet Bastion", IslandTheme.FORTRESS, 800, 450, 38, 925, false);
		s2("Ashen Caldera", IslandTheme.VOLCANO, 1300, -100, 40, 1000, false);
		s2("Mirage Sands", IslandTheme.DESERT, -1250, 200, 38, 1100, false);
		s2("Iron Depths Prison", IslandTheme.PRISON, 200, 1000, 32, 1200, false);
		s2("Amethyst Hollow", IslandTheme.CRYSTAL, -1100, -800, 36, 1300, false);
		s2("Skyward Spires", IslandTheme.SKY, 1100, -900, 32, 1425, false);

		// --- Sea 3 (levels 1500-2800) ---
		s3("Tidecrest Citadel", IslandTheme.PALACE, 0, 0, 42, 1500, true);
		s3("Hearthwind Village", IslandTheme.VILLAGE, -600, 300, 36, 1575, false);
		s3("Thornwood Expanse", IslandTheme.JUNGLE, 650, 350, 40, 1700, false);
		s3("Coliseum of Echoes", IslandTheme.COLOSSEUM, -700, -450, 40, 1850, false);
		s3("Obsidian Peak", IslandTheme.VOLCANO, 900, -500, 42, 2000, false);
		s3("Whiteveil Summit", IslandTheme.SNOW, -1300, 0, 40, 2150, false);
		s3("Gilded Bazaar", IslandTheme.BRICK_TOWN, 1350, 250, 38, 2300, false);
		s3("Seraph's Reach", IslandTheme.SKY, 0, -1100, 34, 2450, false);
		s3("Prism Sanctum", IslandTheme.CRYSTAL, 300, 1100, 38, 2600, false);
		s3("The Last Bastion", IslandTheme.FORTRESS, -400, 1200, 40, 2750, false);
	}

	private SeaLayout() {
	}

	private static void add(int sea, String name, IslandTheme theme, int dx, int z, int radius, int level, boolean starter) {
		ISLANDS.add(new Spec(sea, name, theme, SEA_CENTER_X[sea - 1] + dx, z, radius, level, starter));
	}

	private static void s1(String n, IslandTheme t, int x, int z, int r, int l, boolean s) {
		add(1, n, t, x, z, r, l, s);
	}

	private static void s2(String n, IslandTheme t, int x, int z, int r, int l, boolean s) {
		add(2, n, t, x, z, r, l, s);
	}

	private static void s3(String n, IslandTheme t, int x, int z, int r, int l, boolean s) {
		add(3, n, t, x, z, r, l, s);
	}

	public static List<Spec> all() {
		return Collections.unmodifiableList(ISLANDS);
	}

	public static Spec starter(int sea) {
		for (Spec spec : ISLANDS) {
			if (spec.sea() == sea && spec.starter()) {
				return spec;
			}
		}
		throw new IllegalArgumentException("No starter island for sea " + sea);
	}

	/** Which sea a block position is in (by nearest sea centre). */
	public static int seaAt(double x) {
		int best = 1;
		double bestDistance = Double.MAX_VALUE;
		for (int i = 0; i < SEA_CENTER_X.length; i++) {
			double d = Math.abs(x - SEA_CENTER_X[i]);
			if (d < bestDistance) {
				bestDistance = d;
				best = i + 1;
			}
		}
		return best;
	}

	/** Island ids are the packed chunk position of the island centre (same scheme as random islands). */
	public static long idOf(Spec spec) {
		return net.minecraft.world.level.ChunkPos.asLong(spec.x() >> 4, spec.z() >> 4);
	}
}
