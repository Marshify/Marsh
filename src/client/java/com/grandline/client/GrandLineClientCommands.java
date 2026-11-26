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
		com.grandline.client.fx.VfxPreview.register();
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, buildContext) -> dispatcher.register(vfx()));
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

	/** {@code /glvfx}: developer VFX tools (dev environment or operators only). */
	private static com.mojang.brigadier.builder.LiteralArgumentBuilder<net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource> vfx() {
		var idArg = com.mojang.brigadier.arguments.StringArgumentType.greedyString();
		return ClientCommandManager.literal("glvfx")
				.requires(src -> com.grandline.client.fx.VfxPreview.allowed())
				.executes(ctx -> {
					com.grandline.client.fx.VfxPreview.openNext = true;
					return 1;
				})
				.then(ClientCommandManager.literal("list").executes(ctx -> {
					com.grandline.client.fx.VfxLibrary.byCategory().forEach((cat, list) -> ctx.getSource().sendFeedback(Component.literal("§e" + cat + "§r: "
							+ String.join(", ", list.stream().map(d -> d.id().toString()).toList()))));
					return 1;
				}))
				.then(ClientCommandManager.literal("stats").executes(ctx -> {
					ctx.getSource().sendFeedback(Component.literal(com.grandline.client.fx.VfxPreview.stats()));
					com.grandline.client.fx.VfxLibrary.errors().forEach(e -> ctx.getSource().sendError(Component.literal(e)));
					return 1;
				}))
				.then(ClientCommandManager.literal("reload").executes(ctx -> {
					net.minecraft.client.Minecraft.getInstance().reloadResourcePacks();
					ctx.getSource().sendFeedback(Component.literal("Reloading resources (VFX library included)..."));
					return 1;
				}))
				.then(ClientCommandManager.literal("play").then(ClientCommandManager.argument("args", idArg).executes(ctx -> {
					// play <id> [scale] [#RRGGBB]
					String[] a = com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "args").trim().split("\\s+");
					com.grandline.client.fx.VfxPreview.selected = a[0];
					if (a.length > 1) {
						com.grandline.client.fx.VfxPreview.scale = Float.parseFloat(a[1]);
					}
					Integer colour = a.length > 2 ? Integer.parseInt(a[2].replace("#", ""), 16) : null;
					var inst = colour == null ? com.grandline.client.fx.VfxPreview.play() : playColoured(colour);
					if (inst == null) {
						ctx.getSource().sendError(Component.literal("Unknown or culled effect: " + a[0]));
						return 0;
					}
					return 1;
				})));
	}

	private static com.grandline.client.fx.VfxInstance playColoured(int colour) {
		var mc = net.minecraft.client.Minecraft.getInstance();
		var look = mc.player.getLookAngle();
		var flat = new net.minecraft.world.phys.Vec3(look.x, 0, look.z).normalize();
		return com.grandline.client.fx.VfxInstance.play(com.grandline.client.fx.VfxPreview.selected,
				com.grandline.client.fx.VfxInstance.Params.at(mc.player.position().add(flat.scale(com.grandline.client.fx.VfxPreview.distance)).add(0, 1, 0))
						.dir(flat).scale(com.grandline.client.fx.VfxPreview.scale).colors(colour, null));
	}
}
