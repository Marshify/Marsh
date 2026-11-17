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
 * diffuse sky light ({@link #DIFFUSE}) instead of the propagated value.
 * Rooms, halls and caves have ceilings closer than that, so they keep vanilla light (no glowing interiors); the light
 * is still sky light, so day, sunset and night darken it as usual; block light (torches, lava) is untouched. The server's
 * light data, world generation and gameplay (mob spawning, crops) are not changed. Toggle: Settings, Visuals.
 */
public final class StylizedLighting {
	/** Blocks of open air needed above a spot for it to count as "under the sky" (taller than any room or hall). */
	public static final int GAP = 14;
	/** The sky light such spots get: a soft shade, not full sun. */
	public static final int DIFFUSE = 13;
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
		if (vanillaSky >= DIFFUSE || !enabled() || pos.getY() < MIN_Y || !(openAbove(level, pos) || openBelow(level, pos))) {
			return vanillaSky;
		}
		return DIFFUSE;
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

	/** Whether the spot itself is open and the {@link #GAP} blocks above it are all air (stops at the first block). */
	static boolean openAbove(BlockAndTintGetter level, BlockPos pos) {
		BlockPos.MutableBlockPos m = pos.mutable();
		if (!passable(level, m)) {
			return false;
		}
		for (int i = 1; i <= GAP; i++) {
			m.setY(pos.getY() + i);
			if (!passable(level, m)) {
				return false;
			}
		}
		return true;
	}
}
