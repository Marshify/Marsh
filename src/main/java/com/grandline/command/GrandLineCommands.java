package com.grandline.command;

import com.grandline.GrandLineMod;
import com.grandline.player.PlayerProfile;
import com.grandline.player.PlayerProfileService;
import com.grandline.registry.ModItems;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.function.BiFunction;

/**
 * Server-side {@code /grandline} command tree. Commands run on the server, so they are an authoritative way to inspect
 * and (for operators) modify profiles.
 *
 * <pre>
 * /grandline test                                   - any player: reports mod/server status
 * /grandline profile                                - any player: shows own profile
 * /grandline profile &lt;player&gt;                       - gamemaster (op 2): shows another player's profile
 * /grandline admin addxp &lt;players&gt; &lt;amount&gt;          - gamemaster
 * /grandline admin addcurrency &lt;players&gt; &lt;amount&gt;    - gamemaster (negative removes)
 * /grandline admin setlevel &lt;players&gt; &lt;level&gt;        - gamemaster
 * /grandline admin reset &lt;players&gt;                  - gamemaster
 * </pre>
 */
public final class GrandLineCommands {
	private GrandLineCommands() {
	}

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) -> registerTree(dispatcher));
	}

	private static void registerTree(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("grandline")
				.then(Commands.literal("test").executes(GrandLineCommands::runTest))
				.then(Commands.literal("profile")
						.executes(context -> showProfile(context.getSource(), context.getSource().getPlayerOrException()))
						.then(Commands.argument("player", EntityArgument.player())
								.requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
								.executes(context -> showProfile(context.getSource(), EntityArgument.getPlayer(context, "player")))))
				.then(Commands.literal("admin")
						.requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
						.then(Commands.literal("addxp")
								.then(Commands.argument("players", EntityArgument.players())
										.then(Commands.argument("amount", LongArgumentType.longArg(0))
												.executes(context -> modify(context, "addxp",
														(player, ctx) -> PlayerProfileService.addExperience(player, LongArgumentType.getLong(ctx, "amount")))))))
						.then(Commands.literal("addcurrency")
								.then(Commands.argument("players", EntityArgument.players())
										.then(Commands.argument("amount", LongArgumentType.longArg())
												.executes(context -> modify(context, "addcurrency",
														(player, ctx) -> PlayerProfileService.addCurrency(player, LongArgumentType.getLong(ctx, "amount")))))))
						.then(Commands.literal("setlevel")
								.then(Commands.argument("players", EntityArgument.players())
										.then(Commands.argument("level", IntegerArgumentType.integer(PlayerProfile.MIN_LEVEL))
												.executes(context -> modify(context, "setlevel",
														(player, ctx) -> PlayerProfileService.setLevel(player, IntegerArgumentType.getInteger(ctx, "level")))))))
						.then(Commands.literal("reset")
								.then(Commands.argument("players", EntityArgument.players())
										.executes(context -> modify(context, "reset", (player, ctx) -> PlayerProfileService.reset(player)))))));
	}

	private static int runTest(CommandContext<CommandSourceStack> context) {
		CommandSourceStack source = context.getSource();
		boolean dedicated = source.getServer().isDedicatedServer();
		boolean crystalRegistered = BuiltInRegistries.ITEM.containsKey(GrandLineMod.id("grand_line_crystal"))
				&& BuiltInRegistries.ITEM.get(GrandLineMod.id("grand_line_crystal")) == ModItems.GRAND_LINE_CRYSTAL;

		source.sendSuccess(() -> Component.translatable("commands.grandline.test.header", GrandLineMod.modVersion())
				.withStyle(ChatFormatting.AQUA), false);
		source.sendSuccess(() -> Component.translatable("commands.grandline.test.server",
				Component.translatable(dedicated ? "commands.grandline.test.dedicated" : "commands.grandline.test.integrated")), false);
		source.sendSuccess(() -> Component.translatable("commands.grandline.test.item", yesNo(crystalRegistered)), false);
		return crystalRegistered ? 1 : 0;
	}

	private static int showProfile(CommandSourceStack source, ServerPlayer target) {
		PlayerProfile profile = PlayerProfileService.get(target);
		long toNext = PlayerProfileService.rules().experienceToNextLevel(profile.level());

		source.sendSuccess(() -> Component.translatable("commands.grandline.profile.header", target.getDisplayName())
				.withStyle(ChatFormatting.GOLD), false);
		source.sendSuccess(() -> Component.translatable("commands.grandline.profile.level",
				profile.level(), GrandLineMod.config().maxLevel), false);
		source.sendSuccess(() -> Component.translatable("commands.grandline.profile.experience",
				profile.experience(), toNext), false);
		source.sendSuccess(() -> Component.translatable("commands.grandline.profile.currency", profile.currency()), false);
		source.sendSuccess(() -> Component.translatable("commands.grandline.profile.stat_points", profile.unallocatedStatPoints()), false);
		return profile.level();
	}

	private static int modify(CommandContext<CommandSourceStack> context, String action,
			BiFunction<ServerPlayer, CommandContext<CommandSourceStack>, PlayerProfile> operation) throws CommandSyntaxException {
		Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "players");

		for (ServerPlayer target : targets) {
			PlayerProfile result = operation.apply(target, context);
			GrandLineMod.LOGGER.info("{} ran /grandline admin {} on {} -> {}",
					context.getSource().getTextName(), action, target.getName().getString(), result);
		}

		int count = targets.size();
		context.getSource().sendSuccess(() -> Component.translatable("commands.grandline.admin.success", action, count), true);
		return count;
	}

	private static MutableComponent yesNo(boolean value) {
		return Component.translatable(value ? "commands.grandline.yes" : "commands.grandline.no")
				.withStyle(value ? ChatFormatting.GREEN : ChatFormatting.RED);
	}
}
