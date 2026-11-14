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
		if (npc.role() == Islander.Role.FISHERMAN) {
			stock.add(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(com.grandline.registry.ModItems.STARTER_FISHING_ROD).toString());
			prices.add(GrandLineMod.config().fishingRodPrice);
		}
		if (npc.role() == Islander.Role.SWORDSMITH) {
			stock.addAll(SwordShop.ids());
			prices.addAll(SwordShop.prices());
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
			case NpcActionPayload.BUY_SWORD, NpcActionPayload.BUY_SWORD + 1 -> npc.role() == Islander.Role.SWORDSMITH
					? SwordShop.buy(player, packet.action() - NpcActionPayload.BUY_SWORD) : "too_far";
			case NpcActionPayload.BUY_ROD -> npc.role() == Islander.Role.FISHERMAN ? buyRod(player) : "too_far";
			case NpcActionPayload.SET_HOME -> npc.role() == Islander.Role.INNKEEPER ? setHome(player, npc) : "too_far";
			default -> "too_far";
		};
		boolean success = result.equals("accepted") || result.equals("abandoned") || result.equals("sold") || result.equals("sailed")
				|| result.equals("ship_launched") || result.equals("ship_bought") || result.equals("gun_bought")
				|| result.equals("sword_bought") || result.equals("rod_bought");
		if (result.equals("home_set")) {
			return result; // the confirmation names the island (see setHome)
		}
		return reply(player, success, result);
	}

	/**
	 * Makes the Innkeeper's island the player's home: where they respawn after dying at sea or off any island (dying on
	 * an island still brings them back on that island). Can be changed any number of times.
	 */
	public static String setHome(ServerPlayer player, Islander npc) {
		Island island = IslandRegistry.get(player.server).get(npc.islandId());
		if (island == null) {
			return "home_invalid";
		}
		player.setAttached(com.grandline.data.ModAttachments.HOME_ISLAND, island.id());
		net.minecraft.network.chat.Component done = net.minecraft.network.chat.Component.translatable("message.grandline.home_set", island.name())
				.withStyle(net.minecraft.ChatFormatting.GREEN);
		player.sendSystemMessage(done);
		player.displayClientMessage(done, true);
		player.serverLevel().playSound(null, npc.blockPosition(), net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP,
				net.minecraft.sounds.SoundSource.NEUTRAL, 0.6F, 1.4F);
		return "home_set";
	}

	/** Sells a Starter Fishing Rod for Grand Line money (checked and taken here, before the rod is handed over). */
	public static String buyRod(ServerPlayer player) {
		long price = GrandLineMod.config().fishingRodPrice;
		if (com.grandline.player.PlayerProfileService.get(player).currency() < price) {
			return "rod_money";
		}
		com.grandline.player.PlayerProfileService.update(player, p -> p.withCurrency(p.currency() - price));
		net.minecraft.world.item.ItemStack rod = new net.minecraft.world.item.ItemStack(com.grandline.registry.ModItems.STARTER_FISHING_ROD);
		if (!player.getInventory().add(rod)) {
			player.drop(rod, false);
		}
		return "rod_bought";
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
