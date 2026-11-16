package com.grandline.treasure;

import net.minecraft.core.Direction;

/**
 * A hand-picked chest spot on an island, in island-local block coordinates (the block the chest stands in) and the
 * way its front faces. {@code note} says why it is there (for the docs and reviews).
 */
public record TreasureSpot(TreasureTier tier, int x, int y, int z, Direction facing, String note) {
}
