package com.grandline.client;

import com.grandline.player.PlayerProfile;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.minecraft.network.chat.Component;

/**
 * {@code /grandlineclient profile} — shows the client's synchronized copy of the profile. Useful for verifying that
 * server → client sync works; compare its output with the server-side {@code /grandline profile}.
 */
final class GrandLineClientCommands {
	private GrandLineClientCommands() {
	}

	static void register() {
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, buildContext) -> dispatcher.register(
				ClientCommandManager.literal("grandlineclient").then(ClientCommandManager.literal("profile").executes(context -> {
					PlayerProfile profile = ClientProfileCache.profile();
					if (profile == null) {
						context.getSource().sendError(Component.translatable("commands.grandline_client.profile.none"));
						return 0;
					}

					context.getSource().sendFeedback(Component.translatable("commands.grandline_client.profile.header"));
					context.getSource().sendFeedback(Component.translatable("commands.grandline_client.profile.values",
							profile.level(), profile.experience(), profile.currency(), profile.unallocatedStatPoints()));
					context.getSource().sendFeedback(Component.translatable("commands.grandline_client.cooldowns",
							ClientProfileCache.cooldowns().size()));
					return 1;
				}))));
	}
}
