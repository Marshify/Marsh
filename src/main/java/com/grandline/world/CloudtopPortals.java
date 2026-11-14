package com.grandline.world;

import com.grandline.world.plan.IslandPlans;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The Cloudtop Isles portals (rebuilt island only): stepping into the cloud shaft of the sanctuary gate carries a player
 * up to the Upper Sanctuary; walking through the return doorway there brings them back to the gate. Nothing has to be
 * broken or clicked, it works in adventure mode, and anyone can pass (the Upper Sanctuary is the Lv 450-625 region,
 * which the message says). Both portals shimmer with cloud and light while players are near.
 */
public final class CloudtopPortals {
	static final String ISLAND = "Cloudtop Isles";
	private static final Map<UUID, Long> COOLDOWN = new HashMap<>();

	private CloudtopPortals() {
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % 4 == 0) {
				tick(server);
			}
		});
	}

	/** Island-local portal data from the rebuilt plan, or null when this world uses the original island. */
	static int[][] spots(Island island) {
		var plan = IslandPlans.get(island);
		if (plan == null || !plan.getClass().getSimpleName().equals("CloudtopIsles")) {
			return null;
		}
		return com.grandline.world.plan.CloudtopAccess.portals();
	}

	private static void tick(MinecraftServer server) {
		ServerLevel level = server.overworld();
		if (level.players().isEmpty()) {
			return;
		}
		Island island = null;
		for (Island i : IslandRegistry.get(server).all()) {
			if (i.name().equals(ISLAND)) {
				island = i;
				break;
			}
		}
		if (island == null) {
			return;
		}
		int[][] p = spots(island);
		if (p == null) {
			return;
		}
		long now = level.getGameTime();
		int ox = island.x(), oz = island.z();
		boolean shimmer = server.getTickCount() % 20 == 0;
		for (ServerPlayer player : level.players()) {
			if (Math.abs(player.getX() - ox) > 250 || Math.abs(player.getZ() - oz) > 250) {
				continue;
			}
			if (shimmer) {
				for (int k = 0; k < 2; k++) {
					int[] at = p[k * 2];
					if (player.distanceToSqr(ox + at[0] + 0.5, at[1], oz + at[2] + 0.5) < 40 * 40) {
						level.sendParticles(player, ParticleTypes.CLOUD, true, ox + at[0] + 0.5, at[1] + 1.5, oz + at[2] + 0.5, 12, 0.6, 1.2, 0.6, 0.01);
						level.sendParticles(player, ParticleTypes.END_ROD, true, ox + at[0] + 0.5, at[1] + 2, oz + at[2] + 0.5, 4, 0.5, 1.5, 0.5, 0.02);
					}
				}
			}
			if (COOLDOWN.getOrDefault(player.getUUID(), 0L) > now) {
				continue;
			}
			for (int k = 0; k < 2; k++) {
				int[] from = p[k * 2], to = p[k * 2 + 1];
				double dx = player.getX() - (ox + from[0] + 0.5), dz = player.getZ() - (oz + from[2] + 0.5), dy = player.getY() - from[1];
				if (Math.abs(dx) <= 1.6 && Math.abs(dz) <= 1.6 && dy >= -0.5 && dy <= 4) {
					player.teleportTo(level, ox + to[0] + 0.5, to[1], oz + to[2] + 0.5, to[3], 0);
					player.fallDistance = 0;
					COOLDOWN.put(player.getUUID(), now + 60);
					level.playSound(null, player.blockPosition(), SoundEvents.ELYTRA_FLYING, SoundSource.PLAYERS, 0.4F, 1.6F);
					player.displayClientMessage(Component.translatable(k == 0 ? "message.grandline.cloudtop.up" : "message.grandline.cloudtop.down")
							.withStyle(ChatFormatting.AQUA), true);
					break;
				}
			}
		}
	}
}
