package com.grandline.client.audio;

import com.grandline.GrandLineMod;
import com.grandline.client.ClientProfileCache;
import com.grandline.client.settings.ClientSettings;
import com.grandline.network.payload.BossMusicPayload;
import com.grandline.world.Island;
import com.grandline.world.SeaLayout;
import com.grandline.world.plan.IslandPlan;
import com.grandline.world.plan.IslandPlans;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * The Grand Line soundtrack, chosen on the client for this player only. Priority: an active boss fight (told by the
 * server, see BossEncounters), a special region of an island (the Upper Cloudtop Isles), the island the player is on,
 * the open ocean. Each track loops; a change of track crossfades over {@link #FADE_TICKS}.
 *
 * <ul>
 * <li>No flicker at island edges: an island is entered within its plan's extent and left only {@link #LEAVE_MARGIN}
 * blocks further out, and a new choice must hold for {@link #SETTLE_TICKS} before the music changes (a boss fight
 * starts its music at once).</li>
 * <li>The current track is never restarted: choosing it again keeps it playing, and a track that is fading out fades
 * back in where it is.</li>
 * <li>Non-positional, streamed from disk, in the Master category, scaled by the Soundtrack volume setting (0-100%,
 * applied every tick so changes are immediate).</li>
 * <li>Vanilla background music is silenced in Grand Line worlds (see MusicManagerMixin); other sounds are untouched.</li>
 * <li>Disconnecting stops everything at once; teleports, respawns and dimension changes simply choose again.</li>
 * </ul>
 */
public final class MusicController {
	static final int FADE_TICKS = 60;
	static final int SETTLE_TICKS = 20;
	static final int LEAVE_MARGIN = 24;

	/** One playing track and its crossfade state. */
	static final class Track extends AbstractTickableSoundInstance {
		final String id;
		/** Crossfade position 0..1 and where it is heading. */
		float fade;
		float target = 1;
		int inactiveTicks;

		Track(String id, ResourceLocation sound) {
			super(SoundEvent.createVariableRangeEvent(sound), SoundSource.MASTER, RandomSource.create());
			this.id = id;
			this.looping = true;
			this.delay = 0;
			this.relative = true;
			this.attenuation = SoundInstance.Attenuation.NONE;
			this.x = this.y = this.z = 0;
			this.volume = 0.0001f;
		}

		@Override
		public void tick() {
			// Equal-power crossfade, scaled by the player's soundtrack volume.
			float gain = (float) Math.sin(fade * Math.PI / 2);
			volume = Math.max(0.0001f, gain * ClientSettings.get().soundtrackVolume / 100f);
		}

		@Override
		public boolean canStartSilent() {
			return true;
		}

		void end() {
			stop();
		}
	}

	private static final List<Track> playing = new ArrayList<>();
	/** The track id chosen now ("" for silence) and since when it has been the choice. */
	private static String pending = "";
	private static int pendingTicks;
	private static String current = "";
	private static Island currentIsland;
	private static int bossTier;
	private static int bossSea = 1;

	private MusicController() {
	}

	public static void register() {
		SoundtrackData.register();
		ClientPlayNetworking.registerGlobalReceiver(BossMusicPayload.TYPE, (packet, player, sender) -> {
			bossTier = packet.tier();
			bossSea = packet.sea();
		});
		ClientTickEvents.END_CLIENT_TICK.register(MusicController::tick);
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(MusicController::stopAll));
	}

	/** Whether vanilla background music is silenced right now: in a Grand Line world's overworld. */
	public static boolean suppressVanilla() {
		Minecraft mc = Minecraft.getInstance();
		return mc.level != null && mc.level.dimension() == Level.OVERWORLD && grandLineWorld();
	}

	private static boolean grandLineWorld() {
		for (Island island : ClientProfileCache.islands()) {
			if (island.designed()) {
				return true;
			}
		}
		return false;
	}

	/** The track this player should hear now, by priority, or "" for none. */
	static String choose(Minecraft mc) {
		if (mc.player == null || mc.level == null || !suppressVanilla()) {
			currentIsland = null;
			return "";
		}
		double x = mc.player.getX(), y = mc.player.getY(), z = mc.player.getZ();
		String boss = SoundtrackData.bossTrack(bossSea, bossTier);
		if (bossTier > 0 && SoundtrackData.sound(boss) != null) {
			return boss;
		}
		currentIsland = islandAt(x, z);
		if (currentIsland != null) {
			String region = SoundtrackData.regionTrack(currentIsland.name(), x, y, z, currentIsland.x(), currentIsland.z());
			if (region != null && SoundtrackData.sound(region) != null) {
				return region;
			}
			String own = SoundtrackData.islandTrack(currentIsland.name());
			return SoundtrackData.sound(own) != null ? own : ""; // an island without its music yet stays quiet
		}
		String ocean = SoundtrackData.oceanTrack(SeaLayout.seaAt(x));
		return SoundtrackData.sound(ocean) != null ? ocean : "";
	}

	/** The island whose music area holds (x, z): the current one until it is left by a margin, else the nearest entered. */
	private static Island islandAt(double x, double z) {
		if (currentIsland != null && distance(currentIsland, x, z) <= reach(currentIsland) + LEAVE_MARGIN) {
			for (Island island : ClientProfileCache.islands()) {
				if (island.id() == currentIsland.id()) {
					return island;
				}
			}
		}
		Island best = null;
		double bestDistance = Double.MAX_VALUE;
		for (Island island : ClientProfileCache.islands()) {
			if (!SoundtrackData.hasIsland(island.name())) {
				continue; // small random islands keep the ocean music
			}
			double d = distance(island, x, z);
			if (d <= reach(island) && d < bestDistance) {
				best = island;
				bestDistance = d;
			}
		}
		return best;
	}

	private static double distance(Island island, double x, double z) {
		return Math.hypot(island.x() + 0.5 - x, island.z() + 0.5 - z);
	}

	/** How far an island's music reaches: its plan's built extent (at least its radius). */
	private static int reach(Island island) {
		IslandPlan plan = IslandPlans.get(island);
		return Math.max(island.radius(), plan != null ? plan.extent() : 0);
	}

	private static void tick(Minecraft mc) {
		if (mc.isPaused()) {
			return; // the sound engine is paused too: keep every fade where it is
		}
		String want = choose(mc);
		if (!want.equals(pending)) {
			pending = want;
			pendingTicks = 0;
		} else {
			pendingTicks++;
		}
		boolean bossStart = bossTier > 0 && want.equals(SoundtrackData.bossTrack(bossSea, bossTier));
		if (!want.equals(current) && (pendingTicks >= SETTLE_TICKS || bossStart || current.isEmpty())) {
			switchTo(mc, want);
		}
		float step = 1f / FADE_TICKS;
		for (int i = playing.size() - 1; i >= 0; i--) {
			Track t = playing.get(i);
			t.fade = t.target > t.fade ? Math.min(t.target, t.fade + step) : Math.max(t.target, t.fade - step);
			if (t.target == 0 && t.fade <= 0) {
				mc.getSoundManager().stop(t);
				playing.remove(i);
				continue;
			}
			// Restarts a track the sound engine dropped (a sound device change or a resource reload stops every sound).
			if (t.target > 0 && !mc.getSoundManager().isActive(t)) {
				if (++t.inactiveTicks > 40) {
					playing.remove(i);
					Track again = new Track(t.id, SoundtrackData.sound(t.id));
					again.fade = t.fade;
					playing.add(again);
					mc.getSoundManager().play(again);
				}
			} else {
				t.inactiveTicks = 0;
			}
		}
	}

	private static void switchTo(Minecraft mc, String want) {
		current = want;
		Track keep = null;
		for (Track t : playing) {
			if (t.id.equals(want)) {
				keep = t; // already playing (perhaps fading out): bring it back, no restart
			} else {
				t.target = 0;
			}
		}
		if (keep != null) {
			keep.target = 1;
		} else if (!want.isEmpty()) {
			ResourceLocation sound = SoundtrackData.sound(want);
			if (sound != null) {
				Track t = new Track(want, sound);
				playing.add(t);
				mc.getSoundManager().play(t);
			}
		}
		if (GrandLineMod.LOGGER.isDebugEnabled() || Boolean.getBoolean("grandline.music.debug") || "1".equals(System.getenv("GRANDLINE_AUDIO_DEBUG"))) {
			GrandLineMod.LOGGER.info("Soundtrack -> {}", want.isEmpty() ? "(none)" : want);
		}
	}

	/** Stops everything at once (disconnecting, leaving the world). */
	static void stopAll() {
		Minecraft mc = Minecraft.getInstance();
		for (Track t : playing) {
			mc.getSoundManager().stop(t);
		}
		playing.clear();
		current = "";
		pending = "";
		pendingTicks = 0;
		currentIsland = null;
		bossTier = 0;
	}

	/** For the debug overlay and tests: the track chosen now and the tracks sounding. */
	public static String current() {
		return current;
	}

	public static int playingCount() {
		return playing.size();
	}
}
