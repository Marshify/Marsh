package com.grandline.combat.fx;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Server → client trigger for effects in the client's VFX library ({@code assets/grandline/vfx/effects/*.json}). Any
 * gameplay code (fruits, styles, swords, guns, bosses, races, techniques, quests, levelling) can play a registered
 * effect by id at a position, optionally scaled, recoloured or attached to an entity. Cosmetic only: the server
 * decides gameplay, the client draws. Sent to players within {@link FxEvents#VIEW_DISTANCE}.
 */
public final class Vfx {
	private Vfx() {
	}

	/** Plays {@code effectId} (e.g. "grandline:combat/impact_heavy") at {@code pos} facing {@code dir}. */
	public static void play(Entity source, String effectId, Vec3 pos, Vec3 dir) {
		play(source, effectId, pos, dir, 1, -1, -1);
	}

	/**
	 * @param scale  size multiplier
	 * @param anchor entity id the effect follows, or -1
	 * @param colour 0xRRGGBB primary colour override, or -1 for the effect's own palette
	 */
	public static void play(Entity source, String effectId, Vec3 pos, Vec3 dir, float scale, int anchor, int colour) {
		FxEvents.sendFrom(source, "vfx:" + effectId, pos, dir == null ? Vec3.ZERO : dir, scale, anchor, colour);
	}
}
