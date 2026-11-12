package com.grandline.fruit;

/** Marker for entities that run the fruit shop and gacha. Shop and gacha requests are only accepted near one. */
public interface FruitDealer {
	/** Max distance (blocks) between a player and the dealer for shop/gacha requests. */
	double INTERACTION_RANGE = 8.0;

	boolean isFruitDealer();
}
