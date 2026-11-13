package com.grandline.world;

import com.grandline.network.ServerRequestGuard;
import com.grandline.network.payload.MapTeleportPayload;
import com.grandline.world.plan.Anchor;
import com.grandline.world.plan.IslandPlans;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.List;

/**
 * Creative-only teleport from the sea chart. The server checks the player's game mode itself (a modified client
 * cannot use this in survival), loads the destination and lands the player on the surface at the island's quest
 * spot (planned islands) or its centre.
 */
public final class MapTeleport {
	private MapTeleport() {
	}

	public static void register() {
		ServerPlayNetworking.registerGlobalReceiver(MapTeleportPayload.TYPE, (packet, player, sender) -> {
			if (ServerRequestGuard.check(player, MapTeleportPayload.TYPE) != ServerRequestGuard.Result.ACCEPTED) {
				return;
			}
			if (!teleport(player, packet.islandId())) {
				ServerRequestGuard.reject(player, MapTeleportPayload.TYPE, ServerRequestGuard.Result.REJECTED_INVALID);
			}
		});
	}

	public static boolean teleport(ServerPlayer player, long islandId) {
		if (!player.isCreative()) {
			return false;
		}
		Island island = IslandRegistry.get(player.server).get(islandId);
		if (island == null) {
			return false;
		}
		int x = island.x();
		int z = island.z();
		List<Anchor> quest = IslandPlans.anchors(island, Anchor.Type.QUEST);
		if (!quest.isEmpty()) {
			x += quest.get(0).x() + 2; // beside the quest giver, not on top of them
			z += quest.get(0).z();
		}
		ServerLevel level = player.server.overworld();
		level.getChunk(x >> 4, z >> 4); // load (or generate) before landing
		int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
		player.teleportTo(level, x + 0.5, y, z + 0.5, player.getYRot(), player.getXRot());
		player.fallDistance = 0;
		ModWorld.discover(player, island, true);
		return true;
	}
}
