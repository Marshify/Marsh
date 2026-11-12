package com.grandline.npc;

import com.grandline.GrandLineMod;
import com.grandline.entity.Islander;
import com.grandline.fruit.FruitService;
import com.grandline.fruit.Fruits;
import com.grandline.network.ServerRequestGuard;
import com.grandline.network.payload.ActionResultPayload;
import com.grandline.network.payload.NpcActionPayload;
import com.grandline.network.payload.OpenNpcPayload;
import com.grandline.world.Island;
import com.grandline.world.IslandRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.List;

/** Opens NPC conversations (server → client snapshot) and handles the choices made in them. */
public final class NpcService {
	private NpcService() {
	}

	public static void open(ServerPlayer player, Islander npc) {
		if (!ServerPlayNetworking.canSend(player, OpenNpcPayload.TYPE)) {
			GrandLineMod.LOGGER.warn("Client of {} cannot receive the NPC screen", player.getName().getString());
			return;
		}
		Island island = IslandRegistry.get(player.server).get(npc.islandId());
		String islandName = island == null ? "" : island.name();
		int level = island == null ? npc.islandLevel() : island.level();

		QuestState offer = npc.role() == Islander.Role.QUEST_GIVER ? QuestService.offer(island, npc, false) : null;
		QuestState bossOffer = npc.role() == Islander.Role.QUEST_GIVER && level >= QuestService.BOSS_MIN_ISLAND_LEVEL
				? QuestService.offer(island, npc, true) : null;

		List<String> stock = new ArrayList<>();
		List<Long> prices = new ArrayList<>();
		if (npc.role() == Islander.Role.FRUIT_DEALER) {
			for (Fruits.FruitDef fruit : FruitService.dealerStock(player.server)) {
				stock.add(fruit.id());
				prices.add(FruitService.price(fruit));
			}
		}

		ServerPlayNetworking.send(player, new OpenNpcPayload(npc.getId(), npc.role().ordinal(), islandName, level,
				java.util.Optional.ofNullable(offer), java.util.Optional.ofNullable(bossOffer),
				java.util.Optional.ofNullable(QuestService.active(player)), stock, prices, FruitService.millisUntilRestock(),
				FruitService.gachaCost(player), FruitService.gachaMillisRemaining(player), FishingService.inventoryValue(player), GrandLineMod.config().gachaMinLevel));
	}

	/** Validates and runs an NPC choice. Returns the result key (exposed for tests). */
	public static String handle(ServerPlayer player, NpcActionPayload packet) {
		if (ServerRequestGuard.check(player, NpcActionPayload.TYPE) != ServerRequestGuard.Result.ACCEPTED) {
			return "rate_limited";
		}
		Entity entity = player.serverLevel().getEntity(packet.entityId());
		if (!(entity instanceof Islander npc) || npc.distanceTo(player) > 8) {
			ServerRequestGuard.reject(player, NpcActionPayload.TYPE, ServerRequestGuard.Result.REJECTED_INVALID);
			return reply(player, false, "too_far");
		}

		String result = switch (packet.action()) {
			case NpcActionPayload.ACCEPT_QUEST -> QuestService.accept(player, npc, false).name().toLowerCase(java.util.Locale.ROOT);
			case NpcActionPayload.ACCEPT_BOSS -> QuestService.accept(player, npc, true).name().toLowerCase(java.util.Locale.ROOT);
			case NpcActionPayload.ABANDON_QUEST -> QuestService.abandon(player).name().toLowerCase(java.util.Locale.ROOT);
			case NpcActionPayload.SELL_FISH -> npc.role() == Islander.Role.FISHERMAN
					? (FishingService.sellAll(player) > 0 ? "sold" : "nothing_to_sell") : "too_far";
			case NpcActionPayload.SAIL_TO_SEA, NpcActionPayload.SAIL_TO_SEA + 1, NpcActionPayload.SAIL_TO_SEA + 2 ->
					npc.role() == Islander.Role.NAVIGATOR ? SeaTravel.sail(player, packet.action() - NpcActionPayload.SAIL_TO_SEA + 1) : "too_far";
			default -> "too_far";
		};
		boolean success = result.equals("accepted") || result.equals("abandoned") || result.equals("sold") || result.equals("sailed");
		return reply(player, success, result);
	}

	private static String reply(ServerPlayer player, boolean success, String result) {
		if (ServerPlayNetworking.canSend(player, ActionResultPayload.TYPE)) {
			ServerPlayNetworking.send(player, new ActionResultPayload(success, "npc.grandline.result." + result));
		}
		return result;
	}
}
