package com.grandline.audio;

import com.grandline.GrandLineMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Grand Line's combat sound cues. Each cue is one stage of one ability (charge, cast, travel, impact, aftermath) and is
 * registered as a sound event {@code grandline:<id>}. Its audio file is {@code assets/grandline/sounds/<id with dots as
 * slashes>.ogg} (variants {@code _2}, {@code _3}... are picked at random); {@code tools/sounds/sync_sounds.py} writes
 * {@code sounds.json} from the files that exist.
 *
 * <p>Until a cue's file is supplied it plays its <b>placeholder</b>: the vanilla sound it used before the overhaul, or
 * nothing. Placeholders are only a stand-in; a cue counts as having custom audio once its .ogg is in the mod.
 *
 * <p>Cues are played on each client from the server's ability beats (see the client's GrandLineAudio), so they line up
 * with the visuals, are positional, and are never doubled by the server also playing them.
 */
public final class GrandLineSounds {
	/** Settings group a cue's volume follows (on top of Minecraft's master and Players volume). */
	public enum Category {
		ABILITY, WEAPON, MOVEMENT, ENVIRONMENT
	}

	public enum Stage {
		CHARGE, CAST, TRAVEL, IMPACT, AFTERMATH
	}

	/**
	 * One sound cue. {@code loop}: a sustained sound (charge, travel, flight) that follows an effect and fades out when it
	 * ends. {@code range}: blocks at which it fades to silence. {@code placeholder} (may be null) with its volume and pitch:
	 * what plays until the cue's .ogg exists.
	 */
	public record Cue(String id, Category category, Stage stage, boolean loop, int range, SoundEvent placeholder, float placeholderVolume,
			float placeholderPitch) {
		public ResourceLocation location() {
			return GrandLineMod.id(id);
		}

		/** The asset path inside assets/grandline/sounds/, without the extension. */
		public String path() {
			return id.replace('.', '/');
		}
	}

	private static final List<Cue> CUES = new ArrayList<>();

	private static Cue cue(String id, Category category, Stage stage, boolean loop, int range, SoundEvent placeholder, float volume, float pitch) {
		Cue c = new Cue(id, category, stage, loop, range, placeholder, volume, pitch);
		CUES.add(c);
		return c;
	}

	private static Cue ability(String id, Stage stage, int range, SoundEvent placeholder, float volume, float pitch) {
		return cue(id, Category.ABILITY, stage, false, range, placeholder, volume, pitch);
	}

	private static Cue abilityLoop(String id, Stage stage, int range, SoundEvent placeholder, float volume, float pitch) {
		return cue(id, Category.ABILITY, stage, true, range, placeholder, volume, pitch);
	}

	// ============================================================================ Flame (the first complete fruit)
	// Placeholders are the vanilla sounds these moves used before; null means silent until the file is supplied.

	// Z Cinder Shots: coil, three releases, each fireball's rush, and what it hits (enemy, terrain, or burning out).
	public static final Cue FLAME_SHOTS_CHARGE = ability("fruits.flame.cinder_shots.charge", Stage.CHARGE, 16, SoundEvents.FIRECHARGE_USE, 0.5F, 1.9F);
	public static final Cue FLAME_SHOTS_CAST = ability("fruits.flame.cinder_shots.cast", Stage.CAST, 24, SoundEvents.BLAZE_SHOOT, 0.7F, 1.5F);
	public static final Cue FLAME_SHOTS_TRAVEL = abilityLoop("fruits.flame.cinder_shots.travel", Stage.TRAVEL, 16, null, 0, 1);
	public static final Cue FLAME_SHOTS_IMPACT = ability("fruits.flame.cinder_shots.impact", Stage.IMPACT, 32, SoundEvents.FIRECHARGE_USE, 0.9F, 1.2F);
	public static final Cue FLAME_SHOTS_IMPACT_TERRAIN = ability("fruits.flame.cinder_shots.impact_terrain", Stage.IMPACT, 24,
			SoundEvents.FIRECHARGE_USE, 0.5F, 1.6F);
	public static final Cue FLAME_SHOTS_FIZZLE = ability("fruits.flame.cinder_shots.fizzle", Stage.AFTERMATH, 16, null, 0, 1);

	// X Blaze Wave: arm windup, the sweep, the wall of fire rolling forward, each enemy struck, the wave dying out.
	public static final Cue FLAME_WAVE_CHARGE = ability("fruits.flame.blaze_wave.charge", Stage.CHARGE, 16, SoundEvents.FIRECHARGE_USE, 0.8F, 0.8F);
	public static final Cue FLAME_WAVE_CAST = ability("fruits.flame.blaze_wave.cast", Stage.CAST, 32, SoundEvents.BLAZE_SHOOT, 1.0F, 0.6F);
	public static final Cue FLAME_WAVE_TRAVEL = abilityLoop("fruits.flame.blaze_wave.travel", Stage.TRAVEL, 24, SoundEvents.FIRE_AMBIENT, 1.0F, 0.7F);
	public static final Cue FLAME_WAVE_IMPACT = ability("fruits.flame.blaze_wave.impact", Stage.IMPACT, 32, SoundEvents.FIRECHARGE_USE, 0.9F, 0.9F);
	public static final Cue FLAME_WAVE_AFTERMATH = ability("fruits.flame.blaze_wave.aftermath", Stage.AFTERMATH, 20, null, 0, 1);

	// C Pyre Ring: flames drawn inward, the ring bursting out, its rush across the ground, embers settling.
	public static final Cue FLAME_RING_CHARGE = ability("fruits.flame.pyre_ring.charge", Stage.CHARGE, 16, SoundEvents.FIRECHARGE_USE, 0.8F, 0.5F);
	public static final Cue FLAME_RING_CAST = ability("fruits.flame.pyre_ring.cast", Stage.CAST, 40, SoundEvents.GENERIC_EXPLODE, 0.8F, 1.5F);
	public static final Cue FLAME_RING_TRAVEL = abilityLoop("fruits.flame.pyre_ring.travel", Stage.TRAVEL, 24, SoundEvents.BLAZE_SHOOT, 0.9F, 0.7F);
	public static final Cue FLAME_RING_AFTERMATH = ability("fruits.flame.pyre_ring.aftermath", Stage.AFTERMATH, 20, null, 0, 1);

	// V Inferno Nova: the growing flame mass (held), the throw, the roaring flight, the explosion, the burning tail.
	public static final Cue FLAME_NOVA_CHARGE = abilityLoop("fruits.flame.inferno_nova.charge", Stage.CHARGE, 24, SoundEvents.FIRE_AMBIENT, 0.8F, 0.6F);
	public static final Cue FLAME_NOVA_CAST = ability("fruits.flame.inferno_nova.cast", Stage.CAST, 48, SoundEvents.BLAZE_SHOOT, 1.0F, 0.45F);
	public static final Cue FLAME_NOVA_TRAVEL = abilityLoop("fruits.flame.inferno_nova.travel", Stage.TRAVEL, 32, null, 0, 1);
	public static final Cue FLAME_NOVA_IMPACT = ability("fruits.flame.inferno_nova.impact", Stage.IMPACT, 64, SoundEvents.GENERIC_EXPLODE, 1.6F, 0.8F);
	public static final Cue FLAME_NOVA_AFTERMATH = ability("fruits.flame.inferno_nova.aftermath", Stage.AFTERMATH, 40, SoundEvents.FIRECHARGE_USE, 1.2F, 0.6F);

	// F Ember Wings: ignition and lift-off, the steady roar of flight, the flames going out on landing.
	public static final Cue FLAME_WINGS_CAST = ability("fruits.flame.ember_wings.cast", Stage.CAST, 32, SoundEvents.BLAZE_SHOOT, 0.6F, 1.3F);
	public static final Cue FLAME_WINGS_TRAVEL = abilityLoop("fruits.flame.ember_wings.travel", Stage.TRAVEL, 20, SoundEvents.FIRE_AMBIENT, 0.7F, 1.0F);
	public static final Cue FLAME_WINGS_AFTERMATH = ability("fruits.flame.ember_wings.aftermath", Stage.AFTERMATH, 16, null, 0, 1);

	private GrandLineSounds() {
	}

	public static List<Cue> all() {
		return Collections.unmodifiableList(CUES);
	}

	/** Registers every cue as a sound event (so /playsound and other mods can use them). */
	public static void register() {
		for (Cue c : CUES) {
			Registry.register(BuiltInRegistries.SOUND_EVENT, c.location(), SoundEvent.createVariableRangeEvent(c.location()));
		}
	}
}
