package com.grandline.names;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

import java.util.Locale;
import java.util.Map;

/**
 * The mod author's renaming tool. It exists only when the game runs from the mod's source folder (development), where it
 * writes the mod's own names file (src/main/resources/assets/grandline/names.json, built into every jar); release jars
 * have no such command, so nobody else can rename anything. Commands:
 * <pre>
 * /grandline names set &lt;island|npc|item|ability&gt; "&lt;name&gt;" &lt;new name&gt;   rename (tab completes the names)
 * /grandline names reset &lt;island|npc|item|ability&gt; "&lt;name&gt;"           give it its own name back
 * /grandline names list &lt;island|npc|item|ability&gt;                      the renames of that kind
 * /grandline names set island_npc "&lt;island&gt;" "&lt;npc&gt;" &lt;new name&gt;   an NPC or enemy on one island only
 * /grandline names reset island_npc "&lt;island&gt;" "&lt;npc&gt;"
 * /grandline names reload                                               after editing the names file by hand
 * </pre>
 * Names with spaces go in quotes. Either the original name or its current new name can be used.
 */
public final class NamesCommand {
	private static final int MAX_LENGTH = 48;

	private NamesCommand() {
	}

	public static void register() {
		if (!net.fabricmc.loader.api.FabricLoader.getInstance().isDevelopmentEnvironment()) {
			return; // release jars: the names are part of the mod and only its author changes them
		}
		CommandRegistrationCallback.EVENT.register((dispatcher, registry, environment) -> {
			LiteralArgumentBuilder<CommandSourceStack> names = Commands.literal("names").requires(s -> s.hasPermission(2));
			LiteralArgumentBuilder<CommandSourceStack> set = Commands.literal("set");
			LiteralArgumentBuilder<CommandSourceStack> reset = Commands.literal("reset");
			LiteralArgumentBuilder<CommandSourceStack> list = Commands.literal("list");
			for (GrandLineNames.Category category : GrandLineNames.Category.values()) {
				String word = category.name().toLowerCase(Locale.ROOT);
				SuggestionProvider<CommandSourceStack> known = (ctx, builder) -> SharedSuggestionProvider.suggest(
						GrandLineNames.namesOf(category).stream().map(StringArgumentType::escapeIfRequired), builder);
				SuggestionProvider<CommandSourceStack> renamed = (ctx, builder) -> SharedSuggestionProvider.suggest(
						GrandLineNames.renames(category).keySet().stream().map(StringArgumentType::escapeIfRequired), builder);
				set.then(Commands.literal(word).then(Commands.argument("name", StringArgumentType.string()).suggests(known)
						.then(Commands.argument("new_name", StringArgumentType.greedyString())
								.executes(ctx -> set(ctx, category)))));
				reset.then(Commands.literal(word).then(Commands.argument("name", StringArgumentType.string()).suggests(renamed)
						.executes(ctx -> reset(ctx, category))));
				list.then(Commands.literal(word).executes(ctx -> list(ctx, category)));
			}
			SuggestionProvider<CommandSourceStack> islands = (ctx, builder) -> SharedSuggestionProvider.suggest(
					GrandLineNames.namesOf(GrandLineNames.Category.ISLAND).stream().map(StringArgumentType::escapeIfRequired), builder);
			SuggestionProvider<CommandSourceStack> npcs = (ctx, builder) -> SharedSuggestionProvider.suggest(
					GrandLineNames.namesOf(GrandLineNames.Category.NPC).stream().map(StringArgumentType::escapeIfRequired), builder);
			set.then(Commands.literal("island_npc").then(Commands.argument("island", StringArgumentType.string()).suggests(islands)
					.then(Commands.argument("name", StringArgumentType.string()).suggests(npcs)
							.then(Commands.argument("new_name", StringArgumentType.greedyString()).executes(NamesCommand::setOnIsland)))));
			reset.then(Commands.literal("island_npc").then(Commands.argument("island", StringArgumentType.string()).suggests(islands)
					.then(Commands.argument("name", StringArgumentType.string()).suggests(npcs).executes(NamesCommand::resetOnIsland))));
			list.then(Commands.literal("island_npc").executes(NamesCommand::listOnIslands));
			names.then(set).then(reset).then(list).then(Commands.literal("reload").executes(NamesCommand::reload));
			dispatcher.register(Commands.literal("grandline").then(names));
		});
	}

	private static int set(CommandContext<CommandSourceStack> ctx, GrandLineNames.Category category) {
		String typed = StringArgumentType.getString(ctx, "name");
		String newName = StringArgumentType.getString(ctx, "new_name").trim();
		String original = GrandLineNames.resolve(category, typed);
		if (original == null) {
			ctx.getSource().sendFailure(Component.literal("No " + label(category) + " is called \"" + typed + "\". Press Tab after the kind to see the names."));
			return 0;
		}
		if (newName.isEmpty() || newName.length() > MAX_LENGTH || newName.chars().anyMatch(c -> c < 32 || c == '\u00a7')) {
			ctx.getSource().sendFailure(Component.literal("The new name must be 1-" + MAX_LENGTH + " characters, without colour codes."));
			return 0;
		}
		if (newName.equals(original)) {
			boolean had = GrandLineNames.reset(category, original);
			ctx.getSource().sendSuccess(() -> Component.literal(had ? "\"" + original + "\" has its own name again." : "\"" + original + "\" already has that name."), true);
			return 1;
		}
		GrandLineNames.rename(category, original, newName);
		ctx.getSource().sendSuccess(() -> Component.literal("Renamed " + label(category) + " \"" + original + "\" to \"" + newName + "\".")
				.withStyle(ChatFormatting.GREEN), true);
		return 1;
	}

	private static int reset(CommandContext<CommandSourceStack> ctx, GrandLineNames.Category category) {
		String typed = StringArgumentType.getString(ctx, "name");
		String original = GrandLineNames.resolve(category, typed);
		if (original == null || !GrandLineNames.reset(category, original)) {
			ctx.getSource().sendFailure(Component.literal("\"" + typed + "\" isn't renamed."));
			return 0;
		}
		ctx.getSource().sendSuccess(() -> Component.literal("\"" + original + "\" has its own name again."), true);
		return 1;
	}

	private static int list(CommandContext<CommandSourceStack> ctx, GrandLineNames.Category category) {
		Map<String, String> renames = GrandLineNames.renames(category);
		int total = GrandLineNames.namesOf(category).size();
		if (renames.isEmpty()) {
			ctx.getSource().sendSuccess(() -> Component.literal("No " + category.section + " renamed (" + total + " can be). "
					+ "Use /grandline names set " + category.name().toLowerCase(Locale.ROOT) + " and press Tab for the names."), false);
			return 0;
		}
		ctx.getSource().sendSuccess(() -> Component.literal(renames.size() + " of " + total + " " + category.section + " renamed:")
				.withStyle(ChatFormatting.GOLD), false);
		renames.forEach((from, to) -> ctx.getSource().sendSuccess(() -> Component.literal("  " + from + " → " + to), false));
		return renames.size();
	}

	private static int setOnIsland(CommandContext<CommandSourceStack> ctx) {
		String island = GrandLineNames.resolve(GrandLineNames.Category.ISLAND, StringArgumentType.getString(ctx, "island"));
		String typed = StringArgumentType.getString(ctx, "name");
		String original = GrandLineNames.resolve(GrandLineNames.Category.NPC, typed);
		String newName = StringArgumentType.getString(ctx, "new_name").trim();
		if (island == null || original == null) {
			ctx.getSource().sendFailure(Component.literal(island == null ? "No island is called \"" + StringArgumentType.getString(ctx, "island") + "\"."
					: "No NPC is called \"" + typed + "\". Press Tab to see the names."));
			return 0;
		}
		if (newName.isEmpty() || newName.length() > MAX_LENGTH || newName.chars().anyMatch(c -> c < 32 || c == '\u00a7')) {
			ctx.getSource().sendFailure(Component.literal("The new name must be 1-" + MAX_LENGTH + " characters, without colour codes."));
			return 0;
		}
		GrandLineNames.renameOnIsland(island, original, newName);
		ctx.getSource().sendSuccess(() -> Component.literal("On " + island + ", \"" + original + "\" is now \"" + newName
				+ "\" (new and reloaded NPCs; others on their next load).").withStyle(ChatFormatting.GREEN), true);
		return 1;
	}

	private static int resetOnIsland(CommandContext<CommandSourceStack> ctx) {
		String island = GrandLineNames.resolve(GrandLineNames.Category.ISLAND, StringArgumentType.getString(ctx, "island"));
		String original = GrandLineNames.resolve(GrandLineNames.Category.NPC, StringArgumentType.getString(ctx, "name"));
		if (island == null || original == null || !GrandLineNames.resetOnIsland(island, original)) {
			ctx.getSource().sendFailure(Component.literal("That NPC isn't renamed on that island."));
			return 0;
		}
		ctx.getSource().sendSuccess(() -> Component.literal("On " + island + ", \"" + original + "\" has its usual name again."), true);
		return 1;
	}

	private static int listOnIslands(CommandContext<CommandSourceStack> ctx) {
		var all = GrandLineNames.islandNpcRenames();
		if (all.isEmpty()) {
			ctx.getSource().sendSuccess(() -> Component.literal("No NPCs renamed on single islands."), false);
			return 0;
		}
		all.forEach((island, names) -> names.forEach((from, to) ->
				ctx.getSource().sendSuccess(() -> Component.literal("  " + island + ": " + from + " → " + to), false)));
		return all.size();
	}

	private static int reload(CommandContext<CommandSourceStack> ctx) {
		GrandLineNames.reload();
		ctx.getSource().sendSuccess(() -> Component.literal("Names reloaded from the mod's names file."), true);
		return 1;
	}

	private static String label(GrandLineNames.Category category) {
		return switch (category) {
			case ISLAND -> "island";
			case NPC -> "NPC";
			case ITEM -> "item";
			case ABILITY -> "ability";
		};
	}
}
