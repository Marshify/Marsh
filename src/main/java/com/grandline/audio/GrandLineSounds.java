package com.grandline.audio;

import com.grandline.GrandLineMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

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
	// All Flame cues have their own audio (generated with ElevenLabs, mixed and levelled; see docs/AUDIO.md): no placeholders.

	// Z Cinder Shots: coil, three releases, each fireball's rush, and what it hits (enemy, terrain, or burning out).
	public static final Cue FLAME_SHOTS_CHARGE = ability("fruits.flame.cinder_shots.charge", Stage.CHARGE, 16, null, 0, 1);
	public static final Cue FLAME_SHOTS_CAST = ability("fruits.flame.cinder_shots.cast", Stage.CAST, 24, null, 0, 1);
	public static final Cue FLAME_SHOTS_TRAVEL = abilityLoop("fruits.flame.cinder_shots.travel", Stage.TRAVEL, 16, null, 0, 1);
	public static final Cue FLAME_SHOTS_IMPACT = ability("fruits.flame.cinder_shots.impact", Stage.IMPACT, 32, null, 0, 1);
	public static final Cue FLAME_SHOTS_IMPACT_TERRAIN = ability("fruits.flame.cinder_shots.impact_terrain", Stage.IMPACT, 24, null, 0, 1);
	public static final Cue FLAME_SHOTS_FIZZLE = ability("fruits.flame.cinder_shots.fizzle", Stage.AFTERMATH, 16, null, 0, 1);

	// X Blaze Wave: arm windup, the sweep, the wall of fire rolling forward, each enemy struck, the wave dying out.
	public static final Cue FLAME_WAVE_CHARGE = ability("fruits.flame.blaze_wave.charge", Stage.CHARGE, 16, null, 0, 1);
	public static final Cue FLAME_WAVE_CAST = ability("fruits.flame.blaze_wave.cast", Stage.CAST, 32, null, 0, 1);
	public static final Cue FLAME_WAVE_TRAVEL = abilityLoop("fruits.flame.blaze_wave.travel", Stage.TRAVEL, 24, null, 0, 1);
	public static final Cue FLAME_WAVE_IMPACT = ability("fruits.flame.blaze_wave.impact", Stage.IMPACT, 32, null, 0, 1);
	public static final Cue FLAME_WAVE_AFTERMATH = ability("fruits.flame.blaze_wave.aftermath", Stage.AFTERMATH, 20, null, 0, 1);

	// C Pyre Ring: flames drawn inward, the ring bursting out, its rush across the ground, embers settling.
	public static final Cue FLAME_RING_CHARGE = ability("fruits.flame.pyre_ring.charge", Stage.CHARGE, 16, null, 0, 1);
	public static final Cue FLAME_RING_CAST = ability("fruits.flame.pyre_ring.cast", Stage.CAST, 40, null, 0, 1);
	public static final Cue FLAME_RING_TRAVEL = abilityLoop("fruits.flame.pyre_ring.travel", Stage.TRAVEL, 24, null, 0, 1);
	public static final Cue FLAME_RING_AFTERMATH = ability("fruits.flame.pyre_ring.aftermath", Stage.AFTERMATH, 20, null, 0, 1);

	// V Inferno Nova: the growing flame mass (held), the throw, the roaring flight, the explosion, the burning tail.
	public static final Cue FLAME_NOVA_CHARGE = abilityLoop("fruits.flame.inferno_nova.charge", Stage.CHARGE, 24, null, 0, 1);
	public static final Cue FLAME_NOVA_CAST = ability("fruits.flame.inferno_nova.cast", Stage.CAST, 48, null, 0, 1);
	public static final Cue FLAME_NOVA_TRAVEL = abilityLoop("fruits.flame.inferno_nova.travel", Stage.TRAVEL, 32, null, 0, 1);
	public static final Cue FLAME_NOVA_IMPACT = ability("fruits.flame.inferno_nova.impact", Stage.IMPACT, 64, null, 0, 1);
	public static final Cue FLAME_NOVA_AFTERMATH = ability("fruits.flame.inferno_nova.aftermath", Stage.AFTERMATH, 40, null, 0, 1);

	// F Ember Wings: ignition and lift-off, the steady roar of flight, the flames going out on landing.
	public static final Cue FLAME_WINGS_CAST = ability("fruits.flame.ember_wings.cast", Stage.CAST, 32, null, 0, 1);
	public static final Cue FLAME_WINGS_TRAVEL = abilityLoop("fruits.flame.ember_wings.travel", Stage.TRAVEL, 20, null, 0, 1);
	public static final Cue FLAME_WINGS_AFTERMATH = ability("fruits.flame.ember_wings.aftermath", Stage.AFTERMATH, 16, null, 0, 1);

	// ============================================================================ Rocket (own audio, ElevenLabs, see docs/AUDIO.md)
	public static final Cue ROCKET_FIST_CAST = ability("fruits.rocket.missile_fist.cast", Stage.CAST, 24, null, 0, 1);
	public static final Cue ROCKET_FIST_TRAVEL = abilityLoop("fruits.rocket.missile_fist.travel", Stage.TRAVEL, 16, null, 0, 1);
	public static final Cue ROCKET_FIST_IMPACT = ability("fruits.rocket.missile_fist.impact", Stage.IMPACT, 32, null, 0, 1);
	public static final Cue ROCKET_STRIKE_CAST = ability("fruits.rocket.air_strike.cast", Stage.CAST, 24, null, 0, 1);
	public static final Cue ROCKET_STRIKE_TRAVEL = ability("fruits.rocket.air_strike.travel", Stage.TRAVEL, 24, null, 0, 1);
	public static final Cue ROCKET_STRIKE_IMPACT = ability("fruits.rocket.air_strike.impact", Stage.IMPACT, 32, null, 0, 1);
	public static final Cue ROCKET_CRASH_CHARGE = ability("fruits.rocket.rocket_crash.charge", Stage.CHARGE, 16, null, 0, 1);
	public static final Cue ROCKET_CRASH_CAST = ability("fruits.rocket.rocket_crash.cast", Stage.CAST, 32, null, 0, 1);
	public static final Cue ROCKET_CRASH_IMPACT = ability("fruits.rocket.rocket_crash.impact", Stage.IMPACT, 40, null, 0, 1);
	public static final Cue ROCKET_FLIGHT_CAST = ability("fruits.rocket.blast_off.cast", Stage.CAST, 32, null, 0, 1);
	public static final Cue ROCKET_FLIGHT_TRAVEL = abilityLoop("fruits.rocket.blast_off.travel", Stage.TRAVEL, 20, null, 0, 1);
	public static final Cue ROCKET_FLIGHT_AFTERMATH = ability("fruits.rocket.blast_off.aftermath", Stage.AFTERMATH, 16, null, 0, 1);

	// ============================================================================ Spin (own audio, ElevenLabs, see docs/AUDIO.md)
	public static final Cue SPIN_RAZOR_CHARGE = ability("fruits.spin.razor_wind.charge", Stage.CHARGE, 20, null, 0, 1);
	public static final Cue SPIN_RAZOR_LOOP = abilityLoop("fruits.spin.razor_wind.travel", Stage.TRAVEL, 20, null, 0, 1);
	public static final Cue SPIN_RAZOR_IMPACT = ability("fruits.spin.razor_wind.impact", Stage.IMPACT, 24, null, 0, 1);
	public static final Cue SPIN_RAZOR_AFTERMATH = ability("fruits.spin.razor_wind.aftermath", Stage.AFTERMATH, 16, null, 0, 1);
	public static final Cue SPIN_TORNADO_CAST = ability("fruits.spin.tornado_assault.cast", Stage.CAST, 32, null, 0, 1);
	public static final Cue SPIN_TORNADO_LOOP = abilityLoop("fruits.spin.tornado_assault.travel", Stage.TRAVEL, 28, null, 0, 1);
	public static final Cue SPIN_TORNADO_IMPACT = ability("fruits.spin.tornado_assault.impact", Stage.IMPACT, 24, null, 0, 1);
	public static final Cue SPIN_TORNADO_AFTERMATH = ability("fruits.spin.tornado_assault.aftermath", Stage.AFTERMATH, 36, null, 0, 1);
	public static final Cue SPIN_SLASHER_CAST = ability("fruits.spin.air_slasher.cast", Stage.CAST, 28, null, 0, 1);
	public static final Cue SPIN_SLASHER_CAST_FINAL = ability("fruits.spin.air_slasher.cast_final", Stage.CAST, 36, null, 0, 1);
	public static final Cue SPIN_SLASHER_TRAVEL = abilityLoop("fruits.spin.air_slasher.travel", Stage.TRAVEL, 14, null, 0, 1);
	public static final Cue SPIN_SLASHER_IMPACT = ability("fruits.spin.air_slasher.impact", Stage.IMPACT, 28, null, 0, 1);
	public static final Cue SPIN_SLASHER_IMPACT_FINAL = ability("fruits.spin.air_slasher.impact_final", Stage.IMPACT, 40, null, 0, 1);
	public static final Cue SPIN_HELI_CAST = ability("fruits.spin.helicopter_flight.cast", Stage.CAST, 28, null, 0, 1);
	public static final Cue SPIN_HELI_LOOP = abilityLoop("fruits.spin.helicopter_flight.travel", Stage.TRAVEL, 24, null, 0, 1);
	public static final Cue SPIN_HELI_AFTERMATH = ability("fruits.spin.helicopter_flight.aftermath", Stage.AFTERMATH, 20, null, 0, 1);

	// ============================================================================ Blade (own audio, ElevenLabs, see docs/AUDIO.md)
	public static final Cue BLADE_DEFLECT = ability("fruits.blade.blade_body.deflect", Stage.IMPACT, 20, null, 0, 1);
	public static final Cue BLADE_STRIKE_CHARGE = ability("fruits.blade.blade_strikes.charge", Stage.CHARGE, 16, null, 0, 1);
	public static final Cue BLADE_STRIKE_CAST = ability("fruits.blade.blade_strikes.cast", Stage.CAST, 20, null, 0, 1);
	public static final Cue BLADE_STRIKE_CAST_FINAL = ability("fruits.blade.blade_strikes.cast_final", Stage.CAST, 24, null, 0, 1);
	public static final Cue BLADE_STRIKE_IMPACT = ability("fruits.blade.blade_strikes.impact", Stage.IMPACT, 24, null, 0, 1);
	public static final Cue BLADE_STRIKE_IMPACT_FINAL = ability("fruits.blade.blade_strikes.impact_final", Stage.IMPACT, 28, null, 0, 1);
	public static final Cue BLADE_RUSH_CAST = ability("fruits.blade.razor_rush.cast", Stage.CAST, 32, null, 0, 1);
	public static final Cue BLADE_RUSH_IMPACT = ability("fruits.blade.razor_rush.impact", Stage.IMPACT, 32, null, 0, 1);
	public static final Cue BLADE_DANCE_CAST = ability("fruits.blade.blade_dance.cast", Stage.CAST, 28, null, 0, 1);
	public static final Cue BLADE_DANCE_IMPACT = ability("fruits.blade.blade_dance.impact", Stage.IMPACT, 20, null, 0, 1);
	public static final Cue BLADE_DANCE_CAST_FINAL = ability("fruits.blade.blade_dance.cast_final", Stage.CAST, 32, null, 0, 1);
	public static final Cue BLADE_BARRAGE_CAST = ability("fruits.blade.blade_barrage.cast", Stage.CAST, 24, null, 0, 1);
	public static final Cue BLADE_BARRAGE_TRAVEL = abilityLoop("fruits.blade.blade_barrage.travel", Stage.TRAVEL, 14, null, 0, 1);
	public static final Cue BLADE_BARRAGE_IMPACT = ability("fruits.blade.blade_barrage.impact", Stage.IMPACT, 24, null, 0, 1);
	public static final Cue BLADE_BARRAGE_IMPACT_FINAL = ability("fruits.blade.blade_barrage.impact_final", Stage.IMPACT, 36, null, 0, 1);
	public static final Cue BLADE_FLIGHT_CAST = ability("fruits.blade.blade_flight.cast", Stage.CAST, 28, null, 0, 1);
	public static final Cue BLADE_FLIGHT_LOOP = abilityLoop("fruits.blade.blade_flight.travel", Stage.TRAVEL, 24, null, 0, 1);
	public static final Cue BLADE_FLIGHT_AFTERMATH = ability("fruits.blade.blade_flight.aftermath", Stage.AFTERMATH, 20, null, 0, 1);

	// ============================================================================ Spring (own audio, ElevenLabs, see docs/AUDIO.md)
	public static final Cue SPRING_KNOCK_CAST = ability("fruits.spring.knock.cast", Stage.CAST, 24, null, 0, 1);
	public static final Cue SPRING_KNOCK_IMPACT = ability("fruits.spring.knock.impact", Stage.IMPACT, 28, null, 0, 1);
	public static final Cue SPRING_SNIPE_CAST = ability("fruits.spring.spring_snipe.cast", Stage.CAST, 28, null, 0, 1);
	public static final Cue SPRING_SNIPE_IMPACT = ability("fruits.spring.spring_snipe.impact", Stage.IMPACT, 28, null, 0, 1);
	public static final Cue SPRING_CANNON_CAST = ability("fruits.spring.spring_cannon.cast", Stage.CAST, 28, null, 0, 1);
	public static final Cue SPRING_CANNON_IMPACT = ability("fruits.spring.spring_cannon.impact", Stage.IMPACT, 40, null, 0, 1);
	public static final Cue SPRING_REBOUND = ability("fruits.spring.spring_emperor.rebound", Stage.IMPACT, 24, null, 0, 1);
	public static final Cue SPRING_LEAP_LOOP = abilityLoop("fruits.spring.spring_leap.travel", Stage.TRAVEL, 20, null, 0, 1);
	public static final Cue SPRING_RETRACT = ability("fruits.spring.spring_leap.aftermath", Stage.AFTERMATH, 16, null, 0, 1);

	// ============================================================================ Bomb (own audio, ElevenLabs, see docs/AUDIO.md)
	public static final Cue BOMB_IGNITE = ability("fruits.bomb.blast_toss.cast", Stage.CAST, 20, null, 0, 1);
	public static final Cue BOMB_FUSE_LOOP = abilityLoop("fruits.bomb.blast_toss.travel", Stage.TRAVEL, 14, null, 0, 1);
	public static final Cue BOMB_THROW = ability("fruits.bomb.blast_toss.cast_throw", Stage.CAST, 24, null, 0, 1);
	public static final Cue BOMB_EXPLODE = ability("fruits.bomb.blast_toss.impact", Stage.IMPACT, 40, null, 0, 1);
	public static final Cue BOMB_EXPLODE_HEAVY = ability("fruits.bomb.blast_toss.impact_heavy", Stage.IMPACT, 48, null, 0, 1);
	public static final Cue BOMB_TARGET_CHARGE = abilityLoop("fruits.bomb.targeted_bomb.charge", Stage.CHARGE, 20, null, 0, 1);
	public static final Cue BOMB_TARGET_BLAST = ability("fruits.bomb.targeted_bomb.impact", Stage.IMPACT, 48, null, 0, 1);
	public static final Cue BOMB_GRAB_LAUNCH = ability("fruits.bomb.bomb_grab.cast", Stage.CAST, 32, null, 0, 1);
	public static final Cue BOMB_GRAB_ATTACH = ability("fruits.bomb.bomb_grab.impact", Stage.IMPACT, 28, null, 0, 1);
	public static final Cue BOMB_DELAYED_BLAST = ability("fruits.bomb.bomb_grab.impact_delayed", Stage.IMPACT, 44, null, 0, 1);
	public static final Cue BOMB_MINE_DEPLOY = ability("fruits.bomb.land_mines.cast", Stage.CAST, 24, null, 0, 1);
	public static final Cue BOMB_MINE_ARM = ability("fruits.bomb.land_mines.charge", Stage.CHARGE, 12, null, 0, 1);
	public static final Cue BOMB_MINE_TRIGGER = ability("fruits.bomb.land_mines.cast_trigger", Stage.CAST, 20, null, 0, 1);
	public static final Cue BOMB_HEAD = ability("fruits.bomb.self_destruct.cast", Stage.CAST, 28, null, 0, 1);
	public static final Cue BOMB_SELF_CHARGE = abilityLoop("fruits.bomb.self_destruct.charge", Stage.CHARGE, 28, null, 0, 1);
	public static final Cue BOMB_SELF_MAX = ability("fruits.bomb.self_destruct.charge_max", Stage.CHARGE, 28, null, 0, 1);
	public static final Cue BOMB_SELF_BLAST = ability("fruits.bomb.self_destruct.impact", Stage.IMPACT, 64, null, 0, 1);
	public static final Cue BOMB_RESIDUE = ability("fruits.bomb.self_destruct.aftermath", Stage.AFTERMATH, 28, null, 0, 1);
	public static final Cue BOMB_JUMP = ability("fruits.bomb.explosive_jump.cast", Stage.CAST, 32, null, 0, 1);
	public static final Cue BOMB_JUMP_HURT = ability("fruits.bomb.explosive_jump.fizzle", Stage.AFTERMATH, 12, null, 0, 1);

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
