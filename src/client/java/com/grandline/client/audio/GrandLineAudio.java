package com.grandline.client.audio;

import com.grandline.audio.GrandLineSounds;
import com.grandline.audio.GrandLineSounds.Cue;
import com.grandline.client.settings.ClientSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;

/**
 * Plays Grand Line's sound cues on this client, from the server's ability beats. Each client plays its own copy at the
 * right place (so every nearby player hears it and nobody hears it twice) and the server plays nothing for these moves.
 *
 * <ul>
 *   <li>One-shots ({@link #play}): positional, a little random pitch variation, skipped beyond the cue's range,
 *   de-duplicated (the same cue at the same spot within a tick plays once) and capped per cue.</li>
 *   <li>Loops ({@link #loop}): follow a moving point while an effect lives, fade out when it ends or is interrupted, and
 *   can play a closing cue at that moment (a travel loop ending in a fizzle, for instance). At most {@link #MAX_LOOPS}.</li>
 *   <li>Volume: Minecraft's Master and Players sliders, times the Grand Line category volume in Settings.</li>
 *   <li>Assets: a cue uses its own .ogg when sounds.json has it, otherwise its placeholder (or nothing).</li>
 * </ul>
 */
public final class GrandLineAudio {
	static final int MAX_LOOPS = 12;
	static final int MAX_PER_CUE = 6;
	private static final RandomSource RANDOM = RandomSource.create();
	/** Set the environment variable GRANDLINE_AUDIO_DEBUG=1 to log every cue played (and whether it used its own file). */
	private static final boolean DEBUG = System.getenv("GRANDLINE_AUDIO_DEBUG") != null;

	private static void debug(String what, Cue cue, String source) {
		if (DEBUG) {
			com.grandline.GrandLineMod.LOGGER.info("[GL audio] {} {} ({})", what, cue.id(), source);
		}
	}
	private static final Map<String, long[]> LAST = new HashMap<>(); // cue id -> {gameTime, packed position}
	private static final List<SoundInstance> ONE_SHOTS = new ArrayList<>();
	private static final List<Loop> LOOPS = new ArrayList<>();

	private GrandLineAudio() {
	}

	/** The Grand Line volume (0..1) for a cue's category, from Settings. */
	public static float categoryVolume(GrandLineSounds.Category category) {
		ClientSettings s = ClientSettings.get();
		int level = switch (category) {
			case ABILITY -> s.abilitySfx;
			case WEAPON -> s.weaponSfx;
			case MOVEMENT -> s.movementSfx;
			case ENVIRONMENT -> s.environmentSfx;
		};
		return switch (level) {
			case 0 -> 0f;
			case 1 -> 0.4f;
			case 2 -> 0.7f;
			default -> 1f;
		};
	}

	/** Whether this cue's own audio file is installed (listed in sounds.json). */
	public static boolean hasAsset(Cue cue) {
		return Minecraft.getInstance().getSoundManager().getSoundEvent(cue.location()) != null;
	}

	private static boolean inRange(Cue cue, Vec3 at) {
		Minecraft mc = Minecraft.getInstance();
		return mc.player == null || mc.player.position().distanceToSqr(at) <= (double) cue.range() * cue.range();
	}

	/** A one-shot cue at {@code at}. {@code volume} and {@code pitch} scale the cue (1 = as designed). */
	public static void play(Cue cue, Vec3 at, float volume, float pitch) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || !inRange(cue, at)) {
			return;
		}
		float cat = categoryVolume(cue.category());
		if (cat <= 0) {
			return;
		}
		long now = mc.level.getGameTime();
		long here = ((long) Math.floor(at.x) & 0x1FFFFF) << 42 | ((long) Math.floor(at.y) & 0x1FFFFF) << 21 | ((long) Math.floor(at.z) & 0x1FFFFF);
		long[] last = LAST.get(cue.id());
		if (last != null && now - last[0] <= 1 && last[1] == here) {
			return; // the same cue, same spot, same moment: one copy is enough
		}
		ONE_SHOTS.removeIf(s -> !mc.getSoundManager().isActive(s));
		if (ONE_SHOTS.stream().filter(s -> s.getLocation().equals(cue.location()) || s.getLocation().equals(placeholderId(cue))).count() >= MAX_PER_CUE) {
			return;
		}
		LAST.put(cue.id(), new long[]{now, here});
		SoundEvent event;
		float v = volume * cat;
		float p = pitch * (0.96f + RANDOM.nextFloat() * 0.08f);
		if (hasAsset(cue)) {
			event = SoundEvent.createVariableRangeEvent(cue.location());
		} else if (cue.placeholder() != null) {
			event = cue.placeholder();
			v *= cue.placeholderVolume();
			p *= cue.placeholderPitch();
		} else {
			return;
		}
		debug("play", cue, event.getLocation().getNamespace().equals("grandline") ? "own file" : "placeholder " + event.getLocation());
		SimpleSoundInstance sound = new SimpleSoundInstance(event, SoundSource.PLAYERS, v, p, RANDOM, at.x, at.y, at.z);
		ONE_SHOTS.add(sound);
		mc.getSoundManager().play(sound);
	}

	private static net.minecraft.resources.ResourceLocation placeholderId(Cue cue) {
		return cue.placeholder() == null ? cue.location() : cue.placeholder().getLocation();
	}

	/**
	 * A sustained cue that follows {@code where} while {@code alive} is true, then fades out over a few ticks and plays
	 * {@code closing} (may be null) where it ended. {@code pitch} is read every tick (a charge can rise in pitch).
	 * Returns the loop, or null if it was not started (no audio for the cue, volume off, too many loops, out of range).
	 */
	public static Loop loop(Cue cue, Supplier<Vec3> where, BooleanSupplier alive, float volume, DoubleSupplier pitch, Cue closing) {
		Minecraft mc = Minecraft.getInstance();
		Vec3 start = where.get();
		if (mc.level == null || start == null || !inRange(cue, start)) {
			return null;
		}
		float cat = categoryVolume(cue.category());
		LOOPS.removeIf(l -> l.isStopped());
		SoundEvent event;
		float v = volume * cat;
		float pitchScale = 1f;
		if (hasAsset(cue)) {
			event = SoundEvent.createVariableRangeEvent(cue.location());
		} else if (cue.placeholder() != null) {
			event = cue.placeholder();
			v *= cue.placeholderVolume();
			pitchScale = cue.placeholderPitch();
		} else {
			event = null;
		}
		if (event == null || cat <= 0 || LOOPS.size() >= MAX_LOOPS) {
			// No sound to sustain, but the closing cue still belongs to the end of the effect.
			if (closing != null) {
				Loop watcher = new Loop(null, where, alive, 0, pitch, 1f, closing);
				watcher.silent = true;
				SILENT.add(watcher);
			}
			return null;
		}
		debug("loop", cue, event.getLocation().getNamespace().equals("grandline") ? "own file" : "placeholder " + event.getLocation());
		Loop loop = new Loop(event, where, alive, v, pitch, pitchScale, closing);
		LOOPS.add(loop);
		mc.getSoundManager().play(loop);
		return loop;
	}

	/** Watchers for loops that had nothing to play: ticked here so their closing cues still fire on time. */
	private static final List<Loop> SILENT = new ArrayList<>();

	/** Called every client tick. */
	public static void tick() {
		SILENT.removeIf(l -> {
			l.tick();
			return l.isStopped();
		});
	}

	public static void clear() {
		LAST.clear();
		ONE_SHOTS.clear();
		for (Loop l : LOOPS) {
			l.finish();
		}
		LOOPS.clear();
		SILENT.clear();
	}

	/** A cue sustained while its effect lives; fades out over {@link #FADE} ticks when it ends. */
	public static final class Loop extends AbstractTickableSoundInstance {
		static final int FADE = 6;
		private final Supplier<Vec3> where;
		private final BooleanSupplier alive;
		private final DoubleSupplier pitchSource;
		private final float pitchScale;
		private final float fullVolume;
		private final Cue closing;
		private int fading = -1;
		boolean silent;
		private boolean done;

		Loop(SoundEvent event, Supplier<Vec3> where, BooleanSupplier alive, float volume, DoubleSupplier pitch, float pitchScale, Cue closing) {
			super(event == null ? SoundEvent.createVariableRangeEvent(GrandLineSounds.all().get(0).location()) : event, SoundSource.PLAYERS,
					SoundInstance.createUnseededRandom());
			this.where = where;
			this.alive = alive;
			this.pitchSource = pitch;
			this.pitchScale = pitchScale;
			this.fullVolume = volume;
			this.closing = closing;
			this.looping = true;
			this.delay = 0;
			this.volume = volume;
			this.pitch = (float) pitch.getAsDouble() * pitchScale;
			Vec3 at = where.get();
			this.x = at.x;
			this.y = at.y;
			this.z = at.z;
		}

		@Override
		public void tick() {
			if (done) {
				return;
			}
			if (fading < 0) {
				Vec3 at = where.get();
				if (at != null) {
					x = at.x;
					y = at.y;
					z = at.z;
				}
				pitch = (float) pitchSource.getAsDouble() * pitchScale;
				if (!alive.getAsBoolean()) {
					fading = 0;
					if (DEBUG) {
						com.grandline.GrandLineMod.LOGGER.info("[GL audio] loop end {}", getLocation());
					}
					if (closing != null) {
						play(closing, new Vec3(x, y, z), 1f, 1f);
					}
				}
			}
			if (fading >= 0) {
				fading++;
				volume = fullVolume * Math.max(0, 1f - fading / (float) FADE);
				if (fading >= FADE) {
					finish();
				}
			}
		}

		void finish() {
			done = true;
			stop();
		}

		@Override
		public boolean canStartSilent() {
			return true;
		}
	}
}
