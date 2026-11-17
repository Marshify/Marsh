package com.grandline.client.lighting;

import com.grandline.client.settings.ClientSettings;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;

/**
 * Stylized world lighting: no black "shadows" under floating islands.
 *
 * <p>The cause: vanilla sky light only travels straight down from open sky, and spreads sideways losing one level per
 * block. A column under a floating island has no open sky above it, so under anything wider than ~30 blocks the light
 * falls to 0 and the ocean and terrain below render nearly black, in the exact footprint of the island. (Ambient
 * occlusion only darkens corners and creases; it is not the cause, and is left alone.)
 *
 * <p>The fix, client-side and only for rendering: where a spot has a tall open gap above it ({@link #GAP} blocks of air,
 * the space under a floating island), or a long drop of air below it (the underside of the island itself), it gets
 * full sky light ({@link #DIFFUSE}) instead of the propagated value, so there is no shadow at all.
 * Rooms, halls and caves have ceilings closer than that, so they keep vanilla light (no glowing interiors); the light
 * is still sky light, so day, sunset and night darken it as usual; block light (torches, lava) is untouched. The server's
 * light data, world generation and gameplay (mob spawning, crops) are not changed. Toggle: Settings, Visuals.
 */
public final class StylizedLighting {
	/** Blocks of open air needed above a spot for it to count as "under the sky" (taller than any room or hall). */
	public static final int GAP = 14;
	/**
	 * The sky light such spots get: full daylight, so a floating island casts no shadow at all on the islands or the
	 * water below it (0.45.1, at the owner's request; it was a soft shade of 13 before).
	 */
	public static final int DIFFUSE = 15;
	/** Below this height nothing is changed (sea floor, underwater, caves). */
	public static final int MIN_Y = 58;

	private StylizedLighting() {
	}

	/** Height from which cloud blocks are treated as floating-island cloud (no ambient occlusion). */
	public static final int CLOUD_MIN_Y = 75;

	/**
	 * Whether this block is part of a floating island's cloud base (white wool or snow block, high above the sea), drawn
	 * with flat lighting instead of ambient occlusion. Checked in game: with ambient occlusion the stepped cloud sides went
	 * grey-black even at full light; without it they read as soft bright cloud.
	 */
	public static boolean flatCloud(net.minecraft.world.level.block.state.BlockState state, BlockPos pos) {
		return pos.getY() >= CLOUD_MIN_Y && enabled()
				&& (state.is(net.minecraft.world.level.block.Blocks.WHITE_WOOL) || state.is(net.minecraft.world.level.block.Blocks.SNOW_BLOCK));
	}

	public static boolean enabled() {
		return ClientSettings.get().stylizedLighting;
	}

	/** Sky light at {@code pos} as it should be rendered (the vanilla value, raised under floating islands). */
	/** Dev: GRANDLINE_LIGHT_DEBUG=full renders every spot above MIN_Y with full sky light (to tell light from shading). */
	private static final boolean DEBUG_FULL = "full".equals(System.getenv("GRANDLINE_LIGHT_DEBUG"));

	public static int skyLight(BlockAndTintGetter level, BlockPos pos, int vanillaSky) {
		if (DEBUG_FULL && pos.getY() >= MIN_Y) {
			return 15;
		}
		if (vanillaSky >= DIFFUSE || !enabled()) {
			return vanillaSky;
		}
		if (pos.getY() < MIN_Y) {
			return underwater(level, pos, vanillaSky);
		}
		if (!(openAbove(level, pos) || openBelow(level, pos))) {
			return vanillaSky;
		}
		return DIFFUSE;
	}

	/** How deep under the surface a sea-floor spot may be and still be lit as under open water. */
	public static final int MAX_DEPTH = 24;

	/**
	 * The sea floor (and the water) under a floating island: when the column above the spot is only water up to the
	 * surface and the surface counts as open sky, the spot gets the light it would have under open water (full sky light,
	 * less one per block of water), so the island leaves no shadow on the sea bed either. Caves are under stone, so the
	 * walk up stops at a solid block and they keep vanilla light.
	 */
	static int underwater(BlockAndTintGetter level, BlockPos pos, int vanillaSky) {
		BlockPos.MutableBlockPos m = pos.mutable();
		for (int i = 0; i <= MAX_DEPTH; i++) {
			m.setY(pos.getY() + i);
			if (level.getFluidState(m).is(net.minecraft.tags.FluidTags.WATER)) {
				continue;
			}
			if (i == 0 || m.getY() < MIN_Y || !openAbove(level, m.immutable())) {
				return vanillaSky;
			}
			return Math.max(vanillaSky, DIFFUSE - i);
		}
		return vanillaSky;
	}

	/** A packed light value (as LevelRenderer.getLightColor returns) with its sky part adjusted. */
	public static int adjustPacked(BlockAndTintGetter level, BlockPos pos, int packed) {
		int sky = LightTexture.sky(packed);
		if (!DEBUG_FULL && (sky >= DIFFUSE || !enabled())) {
			return packed;
		}
		int adjusted = skyLight(level, pos, sky);
		return adjusted == sky ? packed : LightTexture.pack(LightTexture.block(packed), adjusted);
	}

	/**
	 * Whether the spot is open with a long drop of air under it ({@link #GAP} blocks): the underside of a floating
	 * island, lit by the bright sea and sky around it. Rooms and halls always have a floor closer than that.
	 */
	static boolean openBelow(BlockAndTintGetter level, BlockPos pos) {
		BlockPos.MutableBlockPos m = pos.mutable();
		if (!passable(level, m)) {
			return false;
		}
		for (int i = 1; i <= GAP; i++) {
			m.setY(pos.getY() - i);
			if (!passable(level, m)) {
				return false;
			}
		}
		return true;
	}

	/** Open space for light: air, water, and plants or other non-solid blocks (grass, flowers, torches). Not leaves. */
	static boolean passable(BlockAndTintGetter level, BlockPos p) {
		var state = level.getBlockState(p);
		return state.isAir() || !state.canOcclude() && state.getCollisionShape(level, p).isEmpty();
	}

	/**
	 * Whether the spot itself is open and either the {@link #GAP} blocks above it are all open, or the first thing above
	 * it is the underside of a floating island's cloud (see {@link #cloudAbove}), however close.
	 */
	static boolean openAbove(BlockAndTintGetter level, BlockPos pos) {
		BlockPos.MutableBlockPos m = pos.mutable();
		if (!passable(level, m)) {
			return false;
		}
		for (int i = 1; i <= GAP; i++) {
			m.setY(pos.getY() + i);
			if (!passable(level, m)) {
				return cloudAbove(level, m);
			}
		}
		return true;
	}

	/** Lowest height at which a cloud underside counts (island clouds hang lower than {@link #CLOUD_MIN_Y}). */
	public static final int CLOUD_UNDERSIDE_MIN_Y = 70;
	/** How many cloud blocks must stack up from the underside: a thick cloud mass, never a one-block roof. */
	public static final int CLOUD_THICKNESS = 4;

	/**
	 * Whether {@code from} (the first solid block above a spot) is the bottom of a floating island's cloud: white wool,
	 * snow block or white concrete, at least {@link #CLOUD_THICKNESS} deep, high above the sea. Then the spot under it is
	 * lit as open sky, so floating islands and cloud platforms cast no shadow even when they float lower than
	 * {@link #GAP} over the water or another island. Roofs and ceilings are never thick white cloud, so rooms stay dark.
	 */
	static boolean cloudAbove(BlockAndTintGetter level, BlockPos.MutableBlockPos from) {
		if (from.getY() < CLOUD_UNDERSIDE_MIN_Y) {
			return false;
		}
		int y0 = from.getY();
		for (int k = 0; k < CLOUD_THICKNESS; k++) {
			from.setY(y0 + k);
			if (!cloud(level.getBlockState(from))) {
				return false;
			}
		}
		return true;
	}

	static boolean cloud(net.minecraft.world.level.block.state.BlockState state) {
		return state.is(net.minecraft.world.level.block.Blocks.WHITE_WOOL) || state.is(net.minecraft.world.level.block.Blocks.SNOW_BLOCK)
				|| state.is(net.minecraft.world.level.block.Blocks.WHITE_CONCRETE);
	}
}
