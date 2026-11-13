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
		});
	}

	static Component quest(QuestState q) {
		Component target = Component.translatable("entity.grandline." + EnemyKind.valueOf(q.target()).id());
		return Component.translatable(q.boss() ? "quest.grandline.boss" : "quest.grandline.kill", q.required(), target, q.islandName());
	}

	private static DialogScreen questGiver(OpenNpcPayload data) {
		List<Component> lines = new ArrayList<>();
		lines.add(Component.translatable("npc.grandline.quest.greeting", data.islandName(), data.islandLevel()));
		List<DialogScreen.Option> options = new ArrayList<>();
		if (data.activeQuest().isPresent()) {
			QuestState active = data.activeQuest().get();
			lines.add(Component.translatable("npc.grandline.quest.current", quest(active), active.progress(), active.required())
					.withStyle(ChatFormatting.YELLOW));
			options.add(DialogScreen.Option.of(Component.translatable("npc.grandline.quest.abandon"),
					() -> send(data, NpcActionPayload.ABANDON_QUEST)));
		} else {
			data.questOffer().ifPresent(q -> options.add(DialogScreen.Option.of(
					Component.translatable("npc.grandline.quest.accept", quest(q), q.rewardExperience(), q.rewardCurrency()),
					() -> send(data, NpcActionPayload.ACCEPT_QUEST))));
			data.bossOffer().ifPresent(q -> options.add(DialogScreen.Option.of(
					Component.translatable("npc.grandline.quest.accept_boss", q.rewardExperience(), q.rewardCurrency())
							.withStyle(ChatFormatting.RED), () -> send(data, NpcActionPayload.ACCEPT_BOSS))));
		}
		options.add(DialogScreen.Option.of(Component.translatable("dialog.grandline.nevermind"), () -> { }));
		return new DialogScreen(Component.translatable("entity.grandline.islander.quest_giver"), lines, options);
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
		return new DialogScreen(Component.translatable("entity.grandline.islander.fruit_dealer"), lines, List.of(
				new DialogScreen.Option(Component.translatable("npc.grandline.dealer.browse"),
						() -> Minecraft.getInstance().setScreen(new FruitShopScreen(data)), true),
				new DialogScreen.Option(gacha, () -> Minecraft.getInstance().setScreen(new GachaScreen(data)), true),
				DialogScreen.Option.of(Component.translatable("dialog.grandline.nevermind"), () -> { })));
	}

	private static DialogScreen fisherman(OpenNpcPayload data) {
		List<Component> lines = List.of(Component.translatable("npc.grandline.fisher.greeting"),
				Component.translatable("npc.grandline.fisher.value", data.fishValue()).withStyle(ChatFormatting.GOLD));
		return new DialogScreen(Component.translatable("entity.grandline.islander.fisherman"), lines, List.of(
				DialogScreen.Option.of(Component.translatable("npc.grandline.fisher.sell", data.fishValue()),
						() -> send(data, NpcActionPayload.SELL_FISH)),
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
		return new DialogScreen(Component.translatable("entity.grandline.islander.navigator"),
				List.of(Component.translatable("npc.grandline.navigator.greeting")), options);
	}

	static void send(OpenNpcPayload data, int action) {
		if (ClientPlayNetworking.canSend(NpcActionPayload.TYPE)) {
			ClientPlayNetworking.send(new NpcActionPayload(data.entityId(), action));
		}
	}
}
