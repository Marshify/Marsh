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

		// The dealer lists every fruit; only the current stock can be bought.
		List<String> stock = new ArrayList<>();
		List<Long> prices = new ArrayList<>();
		List<String> available = new ArrayList<>();
		if (npc.role() == Islander.Role.GUNSMITH) {
			stock.addAll(GunShop.ids());
			prices.addAll(GunShop.prices());
		}
		if (npc.role() == Islander.Role.FRUIT_DEALER) {
			for (Fruits.FruitDef fruit : Fruits.all()) {
				stock.add(fruit.id());
				prices.add(FruitService.price(fruit));
			}
			for (Fruits.FruitDef fruit : FruitService.dealerStock(player.server)) {
				available.add(fruit.id());
			}
		}

		ServerPlayNetworking.send(player, new OpenNpcPayload(npc.getId(), npc.role().ordinal(), islandName, level,
				java.util.Optional.ofNullable(offer), java.util.Optional.ofNullable(bossOffer),
				java.util.Optional.ofNullable(QuestService.active(player)), stock, prices, available, FruitService.millisUntilRestock(),
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
			case NpcActionPayload.BUY_SHIP, NpcActionPayload.BUY_SHIP + 1, NpcActionPayload.BUY_SHIP + 2 -> npc.role() == Islander.Role.NAVIGATOR
					? buyShip(player, npc, com.grandline.ship.ShipClass.values()[packet.action() - NpcActionPayload.BUY_SHIP]) : "too_far";
			case NpcActionPayload.LAUNCH_SHIP, NpcActionPayload.LAUNCH_SHIP + 1, NpcActionPayload.LAUNCH_SHIP + 2 -> npc.role() == Islander.Role.NAVIGATOR
					? com.grandline.ship.ShipService.launchFromNavigator(player, com.grandline.ship.ShipClass.values()[packet.action() - NpcActionPayload.LAUNCH_SHIP], npc)
					: "too_far";
			case NpcActionPayload.BUY_GUN, NpcActionPayload.BUY_GUN + 1 -> npc.role() == Islander.Role.GUNSMITH
					? GunShop.buy(player, packet.action() - NpcActionPayload.BUY_GUN) : "too_far";
			default -> "too_far";
		};
		boolean success = result.equals("accepted") || result.equals("abandoned") || result.equals("sold") || result.equals("sailed")
				|| result.equals("ship_launched") || result.equals("ship_bought") || result.equals("gun_bought");
		return reply(player, success, result);
	}

	/** Buys a ship and launches it beside the Navigator (if that fails, the ship is still bought). */
	private static String buyShip(ServerPlayer player, Islander npc, com.grandline.ship.ShipClass ship) {
		String bought = com.grandline.ship.ShipService.buy(player, ship);
		if (!bought.equals("ship_bought")) {
			return bought;
		}
		String launched = com.grandline.ship.ShipService.launchFromNavigator(player, ship, npc);
		return launched.equals("ship_launched") ? "ship_launched" : bought;
	}

	private static String reply(ServerPlayer player, boolean success, String result) {
		if (ServerPlayNetworking.canSend(player, ActionResultPayload.TYPE)) {
			ServerPlayNetworking.send(player, new ActionResultPayload(success, "npc.grandline.result." + result));
		}
		return result;
	}
}
