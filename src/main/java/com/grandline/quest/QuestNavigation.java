package com.grandline.quest;

import com.grandline.GrandLineMod;
import com.grandline.data.ModAttachments;
import com.grandline.entity.Islander;
import com.grandline.network.payload.NavTargetPayload;
import com.grandline.network.payload.NavTogglePayload;
import com.grandline.npc.QuestService;
import com.grandline.npc.QuestState;
import com.grandline.player.PlayerProfileService;
import com.grandline.player.TeamService;
import com.grandline.world.Island;
import com.grandline.world.IslandRegistry;
import com.grandline.world.SeaLayout;
import com.grandline.world.plan.Anchor;
import com.grandline.world.plan.IslandPlans;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Quest navigation: for each player, the quest giver they should talk to next. With an active quest, that is the quest
 * giver of the quest's island; otherwise the island that fits their level best (the highest-level island with quests
 * they have reached, in their own sea; below Lv 15 that is their side's starting island: Millbrook for Pirates, the
 * Marines' island for Marines). The target is sent to that player alone every half second: the client draws the
 * compass and outlines that one NPC for that player only. The NPC's entity id is sent only when it is loaded nearby,
 * so far-away or unloaded quest givers are pointed at by position without touching their chunks.
 */
public final class QuestNavigation {
	private static final Map<UUID, NavTargetPayload> LAST_SENT = new HashMap<>();

	private QuestNavigation() {
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % 10 == 0) {
				for (ServerPlayer player : server.getPlayerList().getPlayers()) {
					update(player, false);
				}
			}
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> LAST_SENT.remove(handler.getPlayer().getUUID()));
		ServerPlayNetworking.registerGlobalReceiver(NavTogglePayload.TYPE, (packet, player, sender) -> {
			player.setAttached(ModAttachments.QUEST_NAVIGATION, packet.enabled());
			update(player, true);
		});
	}

	public static boolean enabled(ServerPlayer player) {
		return player.getAttachedOrElse(ModAttachments.QUEST_NAVIGATION, true);
	}

	/** The island whose quest giver this player should visit next, or null. */
	public static Island targetIsland(ServerPlayer player) {
		MinecraftServer server = player.server;
		QuestState quest = QuestService.active(player);
		List<Island> all = List.copyOf(IslandRegistry.get(server).all());
		if (quest != null) {
			for (Island island : all) {
				if (island.name().equals(quest.islandName())) {
					return island;
				}
			}
		}
		int level = PlayerProfileService.get(player).level();
		boolean marine = TeamService.get(player) == TeamService.Side.MARINES;
		String ownStart = marine ? GrandLineMod.config().marineSpawnIsland : GrandLineMod.config().spawnIsland;
		String otherStart = marine ? GrandLineMod.config().spawnIsland : GrandLineMod.config().marineSpawnIsland;
		int sea = SeaLayout.seaAt(player.getX());
		java.util.Set<Long> designed = new java.util.HashSet<>();
		for (SeaLayout.Spec spec : SeaLayout.all()) {
			if (!spec.starter()) {
				designed.add(SeaLayout.idOf(spec));
			}
		}
		Island best = null;
		for (Island island : all) {
			if (!designed.contains(island.id()) || island.sea() != sea || island.level() > level || island.name().equals(otherStart)) {
				continue;
			}
			if (best == null || island.level() > best.level()
					|| (island.level() == best.level() && island.name().equals(ownStart))) {
				best = island;
			}
		}
		return best;
	}

	/** Where the island's quest giver stands: its plan's quest spot, else the island centre. */
	public static BlockPos questGiverSpot(Island island) {
		List<Anchor> quest = IslandPlans.anchors(island, Anchor.Type.QUEST);
		var plan = IslandPlans.get(island);
		if (!quest.isEmpty() && plan != null) {
			Anchor a = quest.get(0);
			int y = a.y() != Anchor.SURFACE ? a.y() : plan.terrain().ground(a.x(), a.z()) + 1;
			return new BlockPos(island.x() + a.x(), y, island.z() + a.z());
		}
		return new BlockPos(island.x(), 66, island.z());
	}

	static void update(ServerPlayer player, boolean force) {
		NavTargetPayload payload;
		Island island = enabled(player) ? targetIsland(player) : null;
		if (island == null) {
			payload = new NavTargetPayload(enabled(player), "", "", 0, 0, 0, -1);
		} else {
			BlockPos spot = questGiverSpot(island);
			int entityId = -1;
			if (player.level() == player.server.overworld() && player.blockPosition().closerThan(spot, 128)
					&& player.serverLevel().isPositionEntityTicking(spot)) {
				for (Islander npc : player.serverLevel().getEntitiesOfClass(Islander.class, new AABB(spot).inflate(24),
						n -> n.role() == Islander.Role.QUEST_GIVER && n.islandId() == island.id())) {
					entityId = npc.getId();
					spot = npc.blockPosition();
					break;
				}
			}
			payload = new NavTargetPayload(true, "entity.grandline.islander.quest_giver", island.name(), spot.getX(), spot.getY(), spot.getZ(), entityId);
		}
		if (!force && payload.equals(LAST_SENT.get(player.getUUID()))) {
			return;
		}
		LAST_SENT.put(player.getUUID(), payload);
		if (ServerPlayNetworking.canSend(player, NavTargetPayload.TYPE)) {
			ServerPlayNetworking.send(player, payload);
		}
	}
}
