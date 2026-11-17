package com.grandline.world;

import com.grandline.network.payload.FxEventPayload;
import com.grandline.player.rules.OceanWeakness;
import com.grandline.world.plan.IslandPlans;
import com.grandline.world.plan.LotusPalace;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Lotus Palace's linked whirlpools (rebuilt island only). Standing in (or swimming into) the middle of the surface
 * whirlpool carries a player down into the palace whirlpool in the arrival court, and the palace whirlpool carries
 * them back up; no key, no swimming down. All on the server: the destination chunk is loaded first, the player lands
 * in the middle of the other whirlpool on safe footing, and cannot be sent back until they have stepped out of it
 * (plus a short cooldown). Fruit users take no sea damage on the surface whirlpool or for a moment after a transfer.
 * Both whirlpools send a visual beat (WhirlpoolFx on the client) and a looping water sound to players nearby.
 */
public final class LotusWhirlpools {
	static final String ISLAND = "Lotus Palace";
	static final int COOLDOWN_TICKS = 40, SAFE_TICKS = 60;
	private static final Map<UUID, Long> COOLDOWN = new HashMap<>();
	/** The whirlpool (0 surface, 1 palace) a player arrived in and has not yet stepped out of. */
	private static final Map<UUID, Integer> LOCK = new HashMap<>();
	private static final Map<UUID, Long> SAFE_UNTIL = new HashMap<>();

	private LotusWhirlpools() {
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % 2 == 0) {
				tick(server);
			}
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			UUID id = handler.getPlayer().getUUID();
			COOLDOWN.remove(id);
			LOCK.remove(id);
			SAFE_UNTIL.remove(id);
		});
		OceanWeakness.addModifier(player -> shielded(player) ? 0 : 1);
	}

	static Island island(MinecraftServer server) {
		for (Island i : IslandRegistry.get(server).all()) {
			if (i.name().equals(ISLAND)) {
				return i;
			}
		}
		return null;
	}

	/** World centres (block centre, feet height) of the two whirlpools, or null when this world has the old island. */
	public static Vec3[] centres(MinecraftServer server) {
		Island island = island(server);
		if (island == null || !(IslandPlans.get(island) instanceof LotusPalace)) {
			return null;
		}
		int[] s = LotusPalace.SURFACE_WHIRL, p = LotusPalace.PALACE_WHIRL;
		return new Vec3[]{new Vec3(island.x() + s[0] + 0.5, s[1], island.z() + s[2] + 0.5),
				new Vec3(island.x() + p[0] + 0.5, p[1], island.z() + LotusPalace.PZ + p[2] + 0.5)};
	}

	/** Whether the player is in a whirlpool's trigger (index 0 surface, 1 palace), or -1. */
	static int inTrigger(ServerPlayer player, Vec3[] c, double extra) {
		for (int k = 0; k < 2; k++) {
			double dx = player.getX() - c[k].x, dz = player.getZ() - c[k].z, dy = player.getY() - c[k].y;
			if (dx * dx + dz * dz <= Math.pow(LotusPalace.TRIGGER_R + extra, 2) && dy >= -2 - extra && dy <= 3 + extra) {
				return k;
			}
		}
		return -1;
	}

	/** No sea damage while standing in the surface whirlpool, or for a moment after a transfer. */
	static boolean shielded(ServerPlayer player) {
		Long until = SAFE_UNTIL.get(player.getUUID());
		if (until != null && until > player.serverLevel().getGameTime()) {
			return true;
		}
		Vec3[] c = centres(player.server);
		if (c == null) {
			return false;
		}
		double dx = player.getX() - c[0].x, dz = player.getZ() - c[0].z;
		return dx * dx + dz * dz <= Math.pow(LotusPalace.SURFACE_R + 0.5, 2) && player.getY() >= c[0].y - 3;
	}

	private static void tick(MinecraftServer server) {
		ServerLevel level = server.overworld();
		if (level.players().isEmpty()) {
			return;
		}
		Vec3[] c = centres(server);
		if (c == null) {
			return;
		}
		long now = level.getGameTime();
		int t = server.getTickCount();
		if (t % 20 == 0) {
			beat(level, "whirlpool", c[0], LotusPalace.SURFACE_R, 0);
			beat(level, "whirlpool", c[1], LotusPalace.PALACE_R, 1);
		}
		if (t % 40 == 0) {
			for (Vec3 at : c) {
				level.playSound(null, at.x, at.y, at.z, SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_AMBIENT, SoundSource.AMBIENT, 1.4F, 0.8F);
			}
		}
		for (ServerPlayer player : level.players()) {
			if (player.isSpectator() || player.isPassenger()) {
				continue;
			}
			Integer lock = LOCK.get(player.getUUID());
			if (lock != null) {
				// stays locked until they step out of the whirlpool they arrived in
				double dx = player.getX() - c[lock].x, dz = player.getZ() - c[lock].z, dy = player.getY() - c[lock].y;
				if (dx * dx + dz * dz > Math.pow(LotusPalace.TRIGGER_R + 1, 2) || dy < -4 || dy > 6) {
					LOCK.remove(player.getUUID());
				}
				continue;
			}
			if (COOLDOWN.getOrDefault(player.getUUID(), 0L) > now) {
				continue;
			}
			int k = inTrigger(player, c, 0);
			if (k < 0) {
				continue;
			}
			int to = 1 - k;
			Vec3 dest = c[to];
			level.getChunk((int) Math.floor(dest.x) >> 4, (int) Math.floor(dest.z) >> 4); // make sure it is there
			SAFE_UNTIL.put(player.getUUID(), now + SAFE_TICKS);
			beat(level, "whirlpool_burst", c[k], k == 0 ? LotusPalace.SURFACE_R : LotusPalace.PALACE_R, k);
			level.playSound(null, c[k].x, c[k].y, c[k].z, SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_INSIDE, SoundSource.PLAYERS, 1.0F, 1.0F);
			player.stopRiding();
			player.teleportTo(level, dest.x, dest.y, dest.z, 180, 10);
			player.setDeltaMovement(Vec3.ZERO);
			player.hurtMarked = true;
			player.fallDistance = 0;
			player.clearFire();
			LOCK.put(player.getUUID(), to);
			COOLDOWN.put(player.getUUID(), now + COOLDOWN_TICKS);
			beat(level, "whirlpool_burst", dest, to == 0 ? LotusPalace.SURFACE_R : LotusPalace.PALACE_R, to);
			level.playSound(null, dest.x, dest.y, dest.z, SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_INSIDE, SoundSource.PLAYERS, 1.0F, 1.2F);
			level.playSound(null, dest.x, dest.y, dest.z, SoundEvents.PLAYER_SPLASH, SoundSource.PLAYERS, 0.6F, 1.0F);
			player.displayClientMessage(Component.translatable(to == 1 ? "message.grandline.lotus.down" : "message.grandline.lotus.up")
					.withStyle(ChatFormatting.AQUA), true);
		}
	}

	/** A whirlpool beat for the players near it: effect, centre, radius; speed carries which whirlpool (0 surface). */
	static void beat(ServerLevel level, String effect, Vec3 at, double radius, int which) {
		FxEventPayload payload = new FxEventPayload(effect, -1, at, Vec3.ZERO, which, 0, (float) radius);
		for (ServerPlayer viewer : PlayerLookup.around(level, at, 112)) {
			if (ServerPlayNetworking.canSend(viewer, FxEventPayload.TYPE)) {
				ServerPlayNetworking.send(viewer, payload);
			}
		}
	}

	/** For tests: forget a player's lock and cooldown. */
	public static void reset(ServerPlayer player) {
		LOCK.remove(player.getUUID());
		COOLDOWN.remove(player.getUUID());
	}

	/** For tests: the whirlpool the player is locked in (still standing in after arriving), or -1. */
	public static int lock(ServerPlayer player) {
		return LOCK.getOrDefault(player.getUUID(), -1);
	}
}
