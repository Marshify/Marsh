package com.grandline.world.plan;

/**
 * A gameplay spot on a planned island, in island-local block coordinates. Population, enemy spawning, quests and
 * treasure read these instead of guessing positions, so each island decides where its NPCs and fights belong.
 */
public record Anchor(Type type, int x, int y, int z, int radius, float yaw) {
	/** {@link #y} value meaning "stand on the surface at (x, z)". */
	public static final int SURFACE = Integer.MIN_VALUE;

	public enum Type {
		/** Quest giver (the island's main NPC). */
		QUEST,
		/** Fruit dealer's stall or shop. */
		FRUIT_DEALER,
		FISHERMAN,
		/** Sea travel NPC (starter islands). */
		NAVIGATOR,
		/** Area where island enemies spawn and roam. */
		ENEMY_CAMP,
		/** Where quest bosses appear. */
		BOSS_ARENA,
		/** Where boats wait. */
		DOCK,
		/** Buried treasure chest (placed during generation). */
		TREASURE,
		/** Where players arrive: the world spawn on a starter island and sea-travel landings. */
		ARRIVAL,
		/** The Fruit Gacha NPC (starter islands). */
		GACHA,
		/** The gun shop keeper (starter islands, which have no quests: they are safe zones). */
		GUNSMITH
	}

	/** {@link #yaw} value meaning "any facing". */
	public static final float ANY_YAW = Float.NaN;

	public Anchor(Type type, int x, int y, int z, int radius) {
		this(type, x, y, z, radius, ANY_YAW);
	}

	/** A spot at an exact height, facing {@code yaw} degrees (Minecraft yaw: 0 south, 90 west, 180 north, -90 east). */
	public static Anchor at(Type type, int x, int y, int z, float yaw) {
		return new Anchor(type, x, y, z, 0, yaw);
	}

	public static Anchor of(Type type, int x, int z) {
		return new Anchor(type, x, SURFACE, z, 0);
	}

	/** An area (enemy camp, boss arena) of the given radius on the surface. */
	public static Anchor area(Type type, int x, int z, int radius) {
		return new Anchor(type, x, SURFACE, z, radius);
	}

	/** A spot at an exact height (inside a building, on a floating islet). */
	public static Anchor at(Type type, int x, int y, int z) {
		return new Anchor(type, x, y, z, 0);
	}
}
