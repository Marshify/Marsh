package com.grandline.client.screen;

import com.grandline.client.ClientProfileCache;

import com.grandline.client.ui.DialogScreen;
import com.grandline.entity.EnemyKind;
import com.grandline.entity.Islander;
import com.grandline.network.payload.NpcActionPayload;
import com.grandline.network.payload.OpenNpcPayload;
import com.grandline.npc.QuestState;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** Builds the conversation for each NPC role from the server's {@link OpenNpcPayload}. */
public final class NpcScreens {
	private NpcScreens() {
	}

	public static void open(OpenNpcPayload data) {
		Minecraft.getInstance().setScreen(switch (Islander.Role.byId(data.role())) {
			case QUEST_GIVER -> questGiver(data);
			case FRUIT_DEALER -> fruitDealer(data);
			case FISHERMAN -> fisherman(data);
			case NAVIGATOR -> navigator(data);
			case GACHA -> new GachaScreen(data); // the cart keeper opens the gacha straight away
			case GUNSMITH -> gunsmith(data);
			case INNKEEPER -> innkeeper(data);
			case SWORDSMITH -> swordsmith(data);
			case RACE_MYSTIC, TINKERER, GRAVEKEEPER, TRIAL_KEEPER -> new com.grandline.client.race.RaceScreen(null); // normally opened by the race sync
			case MOVEMENT_INSTRUCTOR -> TrainerScreens.movementInstructor(data);
			case FLASH_STEP_MASTER, AURA_MASTER -> TrainerScreens.abilityMaster(data);
			case INSTINCT_SAGE -> TrainerScreens.instinctSage(data);
			case BOAT_DEALER, LUXURY_BOAT_DEALER -> boatDealer(data);
			case MARINE_RECRUITER -> recruiter(data, true);
			case PIRATE_RECRUITER -> recruiter(data, false);
			case DARK_STEP_TEACHER, BLACKSMITH, DESERT_MERCHANT, MAD_SCIENTIST -> talker(data);
			case BOSS_QUEST_GIVER -> questGiver(data);
			case FRUIT_REMOVER -> fruitRemover(data);
			case MILITARY_DETECTIVE -> detective(data);
			case ACCESSORY_DEALER -> accessoryDealer(data);
		});
	}

	static Component quest(QuestState q) {
		Component target = com.grandline.names.GrandLineNames.npc(com.grandline.world.plan.IslandRoster.targetName(q.target()), q.islandName());
		return Component.translatable(q.boss() ? "quest.grandline.boss" : "quest.grandline.kill", q.required(), target, com.grandline.names.GrandLineNames.island(q.islandName()));
	}

	private static DialogScreen questGiver(OpenNpcPayload data) {
		List<Component> lines = new ArrayList<>();
		lines.add(Component.translatable("npc.grandline.quest.greeting", com.grandline.names.GrandLineNames.island(data.islandName()), data.islandLevel()));
		List<DialogScreen.Option> options = new ArrayList<>();
		if (data.activeQuest().isPresent()) {
			QuestState active = data.activeQuest().get();
			lines.add(Component.translatable("npc.grandline.quest.current", quest(active), active.progress(), active.required())
					.withStyle(ChatFormatting.YELLOW));
			options.add(DialogScreen.Option.of(Component.translatable("npc.grandline.quest.abandon"),
					() -> send(data, NpcActionPayload.ABANDON_QUEST)));
		} else if (!data.rosterOffers().isEmpty()) {
			int level = ClientProfileCache.profile() == null ? 1 : ClientProfileCache.profile().level();
			for (int i = 0; i < data.rosterOffers().size(); i++) {
				QuestState q = data.rosterOffers().get(i);
				int need = data.rosterLevels().get(i);
				int action = NpcActionPayload.ACCEPT_ROSTER + i;
				Component label = level < need
						? Component.translatable("npc.grandline.quest.locked", quest(q), need).withStyle(ChatFormatting.GRAY)
						: Component.translatable("npc.grandline.quest.accept", quest(q), q.rewardExperience(), q.rewardCurrency())
								.withStyle(q.boss() ? ChatFormatting.RED : ChatFormatting.RESET);
				options.add(DialogScreen.Option.of(label, () -> send(data, action)));
			}
		} else {
			data.questOffer().ifPresent(q -> options.add(DialogScreen.Option.of(
					Component.translatable("npc.grandline.quest.accept", quest(q), q.rewardExperience(), q.rewardCurrency()),
					() -> send(data, NpcActionPayload.ACCEPT_QUEST))));
			data.bossOffer().ifPresent(q -> options.add(DialogScreen.Option.of(
					Component.translatable("npc.grandline.quest.accept_boss", q.rewardExperience(), q.rewardCurrency())
							.withStyle(ChatFormatting.RED), () -> send(data, NpcActionPayload.ACCEPT_BOSS))));
		}
		options.add(DialogScreen.Option.of(Component.translatable("dialog.grandline.nevermind"), () -> { }));
		return new DialogScreen(title(data, "entity.grandline.islander." + com.grandline.entity.Islander.Role.byId(data.role()).name().toLowerCase(java.util.Locale.ROOT)), lines, options);
	}

	private static DialogScreen fruitDealer(OpenNpcPayload data) {
		long minutes = data.restockMillis() / 60_000;
		List<Component> lines = List.of(Component.translatable("npc.grandline.dealer.greeting"),
				Component.translatable("npc.grandline.dealer.restock", minutes / 60, minutes % 60).withStyle(ChatFormatting.GRAY));
		boolean tooLow = (ClientProfileCache.profile() == null ? 1 : ClientProfileCache.profile().level()) < data.gachaMinLevel();
		Component gacha = tooLow ? Component.translatable("npc.grandline.dealer.gacha_level", data.gachaMinLevel())
				: data.gachaRemainingMillis() > 0
				? Component.translatable("npc.grandline.dealer.gacha_wait", data.gachaRemainingMillis() / 60_000 + 1)
				: Component.translatable("npc.grandline.dealer.gacha", java.text.NumberFormat.getIntegerInstance(java.util.Locale.US).format(data.gachaCost()));
		return new DialogScreen(title(data, "entity.grandline.islander.fruit_dealer"), lines, List.of(
				new DialogScreen.Option(Component.translatable("npc.grandline.dealer.browse"),
						() -> Minecraft.getInstance().setScreen(new FruitShopScreen(data)), true),
				new DialogScreen.Option(gacha, () -> Minecraft.getInstance().setScreen(new GachaScreen(data)), true),
				DialogScreen.Option.of(Component.translatable("dialog.grandline.nevermind"), () -> { })));
	}

	private static DialogScreen gunsmith(OpenNpcPayload data) {
		List<DialogScreen.Option> options = new ArrayList<>();
		java.text.NumberFormat numbers = java.text.NumberFormat.getIntegerInstance(java.util.Locale.ROOT);
		for (int i = 0; i < data.stock().size() && i < data.prices().size(); i++) {
			var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(new net.minecraft.resources.ResourceLocation(data.stock().get(i)));
			int index = i;
			if (data.prices().get(i) < 0) {
				// Already owned (the server checks too): shown as Owned, with nothing to buy.
				options.add(new DialogScreen.Option(Component.translatable("npc.grandline.shop.owned", item.getDescription())
						.withStyle(net.minecraft.ChatFormatting.DARK_GRAY), () -> { }, true));
				continue;
			}
			options.add(DialogScreen.Option.of(Component.translatable("npc.grandline.gunsmith.buy", item.getDescription(),
					numbers.format(data.prices().get(i))), () -> send(data, NpcActionPayload.BUY_GUN + index)));
		}
		options.add(DialogScreen.Option.of(Component.translatable("dialog.grandline.nevermind"), () -> { }));
		return new DialogScreen(title(data, "entity.grandline.islander.gunsmith"),
				List.of(Component.translatable("npc.grandline.gunsmith.greeting")), options);
	}

	private static DialogScreen innkeeper(OpenNpcPayload data) {
		return new DialogScreen(title(data, "entity.grandline.islander.innkeeper"),
				List.of(Component.translatable("npc.grandline.innkeeper.ask")), List.of(
				DialogScreen.Option.of(Component.translatable("npc.grandline.innkeeper.set_home"), () -> send(data, NpcActionPayload.SET_HOME)),
				DialogScreen.Option.of(Component.translatable("npc.grandline.innkeeper.cancel"), () -> { })));
	}

	private static DialogScreen swordsmith(OpenNpcPayload data) {
		return new DialogScreen(title(data, "entity.grandline.islander.swordsmith"),
				List.of(Component.translatable("npc.grandline.swordsmith.greeting")), List.of(
				new DialogScreen.Option(Component.translatable("npc.grandline.swordsmith.browse"),
						() -> Minecraft.getInstance().setScreen(new SwordShopScreen(data)), true),
				DialogScreen.Option.of(Component.translatable("dialog.grandline.nevermind"), () -> { })));
	}

	private static DialogScreen fisherman(OpenNpcPayload data) {
		List<Component> lines = List.of(Component.translatable("npc.grandline.fisher.greeting"),
				Component.translatable("npc.grandline.fisher.value", data.fishValue()).withStyle(ChatFormatting.GOLD));
		lines = new ArrayList<>(lines);
		lines.add(Component.translatable("npc.grandline.fisher.mastery", com.grandline.client.ClientFishing.level()).withStyle(ChatFormatting.AQUA));
		long rodPrice = data.prices().isEmpty() ? 0 : data.prices().get(0);
		return new DialogScreen(title(data, "entity.grandline.islander.fisherman"), lines, List.of(
				DialogScreen.Option.of(Component.translatable("npc.grandline.fisher.sell", data.fishValue()),
						() -> send(data, NpcActionPayload.SELL_FISH)),
				DialogScreen.Option.of(Component.translatable("npc.grandline.fisher.buy_rod",
						java.text.NumberFormat.getIntegerInstance(java.util.Locale.ROOT).format(rodPrice)), () -> send(data, NpcActionPayload.BUY_ROD)),
				DialogScreen.Option.of(Component.translatable("dialog.grandline.nevermind"), () -> { })));
	}

	private static DialogScreen navigator(OpenNpcPayload data) {
		int level = ClientProfileCache.profile() == null ? 1 : ClientProfileCache.profile().level();
		List<DialogScreen.Option> options = new ArrayList<>();
		for (int sea = 1; sea <= com.grandline.world.SeaLayout.SEA_CENTER_X.length; sea++) {
			int required = com.grandline.world.SeaLayout.SEA_MIN_LEVEL[sea - 1];
			int target = sea;
			Component label = level >= required ? Component.translatable("npc.grandline.navigator.sail", sea)
					: Component.translatable("npc.grandline.navigator.locked", sea, required).withStyle(ChatFormatting.GRAY);
			options.add(DialogScreen.Option.of(label, () -> send(data, NpcActionPayload.SAIL_TO_SEA + target - 1)));
		}
		// Ships: launch the ones you own, buy the others (buying launches it here).
		java.text.NumberFormat numbers = java.text.NumberFormat.getIntegerInstance(java.util.Locale.ROOT);
		for (com.grandline.ship.ShipClass ship : com.grandline.ship.ShipClass.values()) {
			Component label;
			int action;
			if (com.grandline.client.ClientShips.owns(ship)) {
				label = Component.translatable("npc.grandline.navigator.launch_ship", ship.displayName());
				action = NpcActionPayload.LAUNCH_SHIP + ship.ordinal();
			} else if (level < ship.minLevel()) {
				label = Component.translatable("npc.grandline.navigator.ship_locked", ship.displayName(), ship.minLevel()).withStyle(ChatFormatting.GRAY);
				action = NpcActionPayload.BUY_SHIP + ship.ordinal();
			} else {
				label = Component.translatable("npc.grandline.navigator.buy_ship", ship.displayName(), numbers.format(ship.price()));
				action = NpcActionPayload.BUY_SHIP + ship.ordinal();
			}
			options.add(DialogScreen.Option.of(label, () -> send(data, action)));
		}
		options.add(DialogScreen.Option.of(Component.translatable("dialog.grandline.nevermind"), () -> { }));
		return new DialogScreen(title(data, "entity.grandline.islander.navigator"),
				List.of(Component.translatable("npc.grandline.navigator.greeting")), options);
	}

	/** An NPC's name as its island shows it (names.json), for dialog titles. */
	static Component title(OpenNpcPayload data, String key) {
		return com.grandline.names.GrandLineNames.npc(key, data.islandName());
	}

	/** One ship's option: launch it if owned, else buy it (greyed out below its level). */
	private static DialogScreen.Option shipOption(OpenNpcPayload data, com.grandline.ship.ShipClass ship, int level) {
		java.text.NumberFormat numbers = java.text.NumberFormat.getIntegerInstance(java.util.Locale.ROOT);
		Component label;
		int action;
		if (com.grandline.client.ClientShips.owns(ship)) {
			label = Component.translatable("npc.grandline.navigator.launch_ship", ship.displayName());
			action = NpcActionPayload.LAUNCH_SHIP + ship.ordinal();
		} else if (level < ship.minLevel()) {
			label = Component.translatable("npc.grandline.navigator.ship_locked", ship.displayName(), ship.minLevel()).withStyle(ChatFormatting.GRAY);
			action = NpcActionPayload.BUY_SHIP + ship.ordinal();
		} else {
			label = Component.translatable("npc.grandline.navigator.buy_ship", ship.displayName(), numbers.format(ship.price()));
			action = NpcActionPayload.BUY_SHIP + ship.ordinal();
		}
		return DialogScreen.Option.of(label, () -> send(data, action));
	}

	/** Boat Dealer (Sloop, Brig) and Luxury Boat Dealer (Galleon). */
	private static DialogScreen boatDealer(OpenNpcPayload data) {
		int level = ClientProfileCache.profile() == null ? 1 : ClientProfileCache.profile().level();
		boolean luxury = data.role() == com.grandline.entity.Islander.Role.LUXURY_BOAT_DEALER.ordinal();
		List<DialogScreen.Option> options = new ArrayList<>();
		for (com.grandline.ship.ShipClass ship : com.grandline.ship.ShipClass.values()) {
			if (luxury == (ship == com.grandline.ship.ShipClass.GALLEON)) {
				options.add(shipOption(data, ship, level));
			}
		}
		options.add(DialogScreen.Option.of(Component.translatable("dialog.grandline.nevermind"), () -> { }));
		String role = luxury ? "luxury_boat_dealer" : "boat_dealer";
		return new DialogScreen(title(data, "entity.grandline.islander." + role), List.of(Component.translatable("npc.grandline." + role + ".greeting")), options);
	}

	/** The Fruit Removal NPC: the fee, then a confirmation before anything is taken. */
	private static DialogScreen fruitRemover(OpenNpcPayload data) {
		String price = java.text.NumberFormat.getIntegerInstance(java.util.Locale.ROOT).format(data.fishValue()); // the fee rides in fishValue
		Component title = title(data, "entity.grandline.islander.fruit_remover");
		DialogScreen confirm = new DialogScreen(title, List.of(Component.translatable("npc.grandline.fruit_remover.confirm", price).withStyle(ChatFormatting.RED)),
				List.of(DialogScreen.Option.of(Component.translatable("npc.grandline.fruit_remover.yes").withStyle(ChatFormatting.RED), () -> send(data, NpcActionPayload.REMOVE_FRUIT)),
						DialogScreen.Option.of(Component.translatable("dialog.grandline.nevermind"), () -> { })));
		return new DialogScreen(title, List.of(Component.translatable("npc.grandline.fruit_remover.greeting", price)),
				List.of(new DialogScreen.Option(Component.translatable("npc.grandline.fruit_remover.remove", price), () -> net.minecraft.client.Minecraft.getInstance().setScreen(confirm), true),
						DialogScreen.Option.of(Component.translatable("dialog.grandline.nevermind"), () -> { })));
	}

	/** The Military Detective: the Second Sea's requirement, and whether the player meets it. */
	private static DialogScreen detective(OpenNpcPayload data) {
		int level = ClientProfileCache.profile() == null ? 1 : ClientProfileCache.profile().level();
		int need = com.grandline.world.SeaLayout.SEA_MIN_LEVEL[1];
		return new DialogScreen(title(data, "entity.grandline.islander.military_detective"),
				List.of(Component.translatable("npc.grandline.military_detective.greeting", need),
						Component.translatable(level >= need ? "npc.grandline.military_detective.ready" : "npc.grandline.military_detective.not_yet", level, need)
								.withStyle(level >= need ? ChatFormatting.GREEN : ChatFormatting.YELLOW)),
				List.of(DialogScreen.Option.of(Component.translatable("dialog.grandline.nevermind"), () -> { })));
	}

	/** An NPC with something to say and nothing to sell yet. */
	private static DialogScreen talker(OpenNpcPayload data) {
		String role = com.grandline.entity.Islander.Role.byId(data.role()).name().toLowerCase(java.util.Locale.ROOT);
		return new DialogScreen(title(data, "entity.grandline.islander." + role),
				List.of(Component.translatable("npc.grandline." + role + ".greeting")),
				List.of(DialogScreen.Option.of(Component.translatable("dialog.grandline.nevermind"), () -> { })));
	}

	/** The accessory dealer: each item with its price, or Owned. */
	private static DialogScreen accessoryDealer(OpenNpcPayload data) {
		java.text.NumberFormat numbers = java.text.NumberFormat.getIntegerInstance(java.util.Locale.ROOT);
		List<DialogScreen.Option> options = new ArrayList<>();
		for (int i = 0; i < data.stock().size(); i++) {
			var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(new net.minecraft.resources.ResourceLocation(data.stock().get(i)));
			long price = data.prices().get(i);
			int action = NpcActionPayload.BUY_ACCESSORY + i;
			options.add(price < 0
					? DialogScreen.Option.of(Component.translatable("npc.grandline.accessory_dealer.owned", item.getDescription()).withStyle(ChatFormatting.GRAY), () -> { })
					: DialogScreen.Option.of(Component.translatable("npc.grandline.accessory_dealer.buy", item.getDescription(), numbers.format(price)),
							() -> send(data, action)));
		}
		options.add(DialogScreen.Option.of(Component.translatable("dialog.grandline.nevermind"), () -> { }));
		return new DialogScreen(title(data, "entity.grandline.islander.accessory_dealer"),
				List.of(Component.translatable("npc.grandline.accessory_dealer.greeting")), options);
	}

	/** Marine or Pirate Recruiter: join that side. */
	private static DialogScreen recruiter(OpenNpcPayload data, boolean marines) {
		var self = net.minecraft.client.Minecraft.getInstance().player;
		String team = marines ? "grandline_marines" : "grandline_pirates";
		boolean member = self != null && self.getTeam() != null && team.equals(self.getTeam().getName());
		String key = marines ? "recruiter" : "pirate_recruiter";
		List<DialogScreen.Option> options = new ArrayList<>();
		if (!member) {
			options.add(DialogScreen.Option.of(Component.translatable("npc.grandline." + key + ".join").withStyle(marines ? ChatFormatting.BLUE : ChatFormatting.RED),
					() -> send(data, marines ? NpcActionPayload.JOIN_MARINES : NpcActionPayload.JOIN_PIRATES)));
		}
		options.add(DialogScreen.Option.of(Component.translatable("dialog.grandline.nevermind"), () -> { }));
		return new DialogScreen(title(data, "entity.grandline.islander." + (marines ? "marine_recruiter" : "pirate_recruiter")),
				List.of(Component.translatable("npc.grandline." + key + (member ? ".already" : ".greeting"))), options);
	}

	static void send(OpenNpcPayload data, int action) {
		if (ClientPlayNetworking.canSend(NpcActionPayload.TYPE)) {
			ClientPlayNetworking.send(new NpcActionPayload(data.entityId(), action));
		}
	}
}
