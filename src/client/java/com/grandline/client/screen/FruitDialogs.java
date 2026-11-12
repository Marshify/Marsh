package com.grandline.client.screen;

import com.grandline.client.ClientProfileCache;
import com.grandline.client.ui.DialogScreen;
import com.grandline.combat.ability.Power;
import com.grandline.combat.ability.Powers;
import com.grandline.fruit.FruitItem;
import com.grandline.fruit.FruitRarity;
import com.grandline.network.payload.FruitActionPayload;
import com.grandline.player.PlayerProfile;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;

import java.util.ArrayList;
import java.util.List;

/** Builds the Eat / Drop / Store / Nevermind dialog for a held fruit. */
public final class FruitDialogs {
	private FruitDialogs() {
	}

	public static void register() {
		FruitItem.openDialogHook = (item, hand) -> Minecraft.getInstance().setScreen(create(item, hand));
	}

	private static DialogScreen create(FruitItem item, InteractionHand hand) {
		Power power = Powers.get(item.powerId());
		FruitRarity rarity = item.rarity();
		Component fruitName = Component.translatable(power.translationKey())
				.withStyle(rarity == null ? ChatFormatting.WHITE : rarity.formatting());
		int handArg = hand == InteractionHand.MAIN_HAND ? 0 : 1;

		List<Component> lines = new ArrayList<>();
		lines.add(Component.translatable("dialog.grandline.fruit.prompt", fruitName));
		PlayerProfile profile = ClientProfileCache.profile();
		if (profile != null && profile.powers().hasFruit() && !profile.powers().fruit().equals(item.powerId().toString())) {
			Power current = Powers.get(profile.powers().fruit());
			if (current != null) {
				lines.add(Component.translatable("dialog.grandline.fruit.replace_warning",
						Component.translatable(current.translationKey())).withStyle(ChatFormatting.RED));
			}
		}

		return new DialogScreen(Component.translatable(item.getDescriptionId()), lines, List.of(
				DialogScreen.Option.of(Component.translatable("dialog.grandline.eat"), () -> send(FruitActionPayload.HELD_EAT, handArg)),
				DialogScreen.Option.of(Component.translatable("dialog.grandline.drop"), () -> send(FruitActionPayload.HELD_DROP, handArg)),
				DialogScreen.Option.of(Component.translatable("dialog.grandline.store"), () -> send(FruitActionPayload.HELD_STORE, handArg)),
				DialogScreen.Option.of(Component.translatable("dialog.grandline.nevermind"), () -> { })));
	}

	static void send(int action, int arg) {
		send(action, "", arg);
	}

	public static void send(int action, String fruitId, int arg) {
		if (ClientPlayNetworking.canSend(FruitActionPayload.TYPE)) {
			ClientPlayNetworking.send(new FruitActionPayload(action, fruitId, arg));
		}
	}
}
