package com.grandline.race;

import com.grandline.GrandLineMod;
import com.grandline.config.RaceBalance;
import com.grandline.data.ModAttachments;
import com.grandline.network.payload.RaceLookPayload;
import com.grandline.network.payload.RaceSyncPayload;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.UnaryOperator;

/**
 * Race storage, assignment and sync. A player without race data (new, or from before races existed) is given one of
 * the four standard races once, by the configured weights; it is saved with the player and kept through death,
 * logout, side changes and respawns. Every change is pushed to the player (full progression) and to everyone who can
 * see them (race, stage and transformation, for cosmetics). Live ability state is held here, server side.
 */
public final class RaceService {
	/** World time each player's V3 ability and V4 awakening end (0 / absent = not active). */
	static final Map<UUID, Long> V3_UNTIL = new HashMap<>();
	static final Map<UUID, Long> V4_UNTIL = new HashMap<>();

	private RaceService() {
	}

	public static void register() {
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ensure(handler.getPlayer());
			sync(handler.getPlayer());
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			V3_UNTIL.remove(handler.getPlayer().getUUID());
			V4_UNTIL.remove(handler.getPlayer().getUUID());
		});
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
			// Temporary states end with death; the saved race and progression come across untouched.
			V3_UNTIL.remove(newPlayer.getUUID());
			V4_UNTIL.remove(newPlayer.getUUID());
			sync(newPlayer);
		});
		EntityTrackingEvents.START_TRACKING.register((entity, viewer) -> {
			if (entity instanceof ServerPlayer target) {
				sendLook(target, viewer);
			}
		});
	}

	/** The player's race data (assigning a starting race first if they have none). */
	public static RaceData get(ServerPlayer player) {
		return ensure(player);
	}

	public static Race race(ServerPlayer player) {
		return get(player).kind();
	}

	public static RaceBalance balance(Race race) {
		RaceBalance b = GrandLineMod.config().races.get(race.id());
		return b != null ? b : RaceBalance.defaults().get(race.id());
	}

	public static RaceBalance balance(ServerPlayer player) {
		return balance(race(player));
	}

	static RaceData ensure(ServerPlayer player) {
		RaceData data = player.getAttached(ModAttachments.RACE);
		if (data == null) {
			data = RaceData.fresh(rollStarting(player.getRandom(), null));
			player.setAttached(ModAttachments.RACE, data);
		}
		return data;
	}

	/** A standard race by the configured weights, never {@code except} (if given). */
	public static Race rollStarting(RandomSource random, Race except) {
		Map<String, Integer> weights = GrandLineMod.config().raceWeights;
		int total = 0;
		for (Race r : Race.values()) {
			if (r.standard && r != except) {
				total += Math.max(0, weights.getOrDefault(r.id(), 0));
			}
		}
		if (total <= 0) {
			return except == Race.HUMAN ? Race.SHARK : Race.HUMAN;
		}
		int pick = random.nextInt(total);
		for (Race r : Race.values()) {
			if (r.standard && r != except) {
				pick -= Math.max(0, weights.getOrDefault(r.id(), 0));
				if (pick < 0) {
					return r;
				}
			}
		}
		return Race.HUMAN;
	}

	/** Changes the player's race data and pushes it to them and to everyone watching. */
	public static RaceData update(ServerPlayer player, UnaryOperator<RaceData> op) {
		RaceData before = get(player);
		RaceData after = op.apply(before);
		if (!after.equals(before)) {
			player.setAttached(ModAttachments.RACE, after);
		}
		sync(player);
		return after;
	}

	public static boolean v3Active(ServerPlayer player) {
		return V3_UNTIL.getOrDefault(player.getUUID(), 0L) > player.serverLevel().getGameTime();
	}

	public static boolean v4Active(ServerPlayer player) {
		return V4_UNTIL.getOrDefault(player.getUUID(), 0L) > player.serverLevel().getGameTime();
	}

	public static int mode(ServerPlayer player) {
		return v4Active(player) ? 2 : v3Active(player) ? 1 : 0;
	}

	public static void sync(ServerPlayer player) {
		sync(player, null);
	}

	/**
	 * Sends the player their race data plus a view for the menu (next-step requirements, free upgrade points) and, when
	 * {@code npc} is given, opens the menu on that race NPC's page.
	 */
	public static void sync(ServerPlayer player, CompoundTag npc) {
		RaceData data = get(player);
		long now = player.serverLevel().getGameTime();
		if (ServerPlayNetworking.canSend(player, RaceSyncPayload.TYPE)) {
			CompoundTag tag = (CompoundTag) RaceData.CODEC.encodeStart(NbtOps.INSTANCE, data).result().orElse(new CompoundTag());
			tag.put("view", RaceProgress.toTag(RaceProgress.requirements(player)));
			tag.putInt("points", RaceUpgrades.pointsFree(data));
			if (npc != null) {
				tag.put("npc", npc);
			}
			int v3 = (int) Math.max(0, V3_UNTIL.getOrDefault(player.getUUID(), 0L) - now);
			int v4 = (int) Math.max(0, V4_UNTIL.getOrDefault(player.getUUID(), 0L) - now);
			int ready = (int) Math.max(0, data.counter("v3_ready") - now);
			ServerPlayNetworking.send(player, new RaceSyncPayload(tag, v3, v4, ready));
		}
		sendLook(player, player);
		for (ServerPlayer viewer : PlayerLookup.tracking(player)) {
			sendLook(player, viewer);
		}
	}

	static void sendLook(ServerPlayer target, ServerPlayer viewer) {
		RaceData data = get(target);
		if (ServerPlayNetworking.canSend(viewer, RaceLookPayload.TYPE)) {
			ServerPlayNetworking.send(viewer, new RaceLookPayload(target.getId(), data.race(), data.stage(), mode(target)));
		}
	}
}
