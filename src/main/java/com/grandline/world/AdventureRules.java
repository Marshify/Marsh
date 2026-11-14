package com.grandline.world;

import com.grandline.GrandLineMod;
import com.grandline.entity.Islander;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecartContainer;
import net.minecraft.world.entity.vehicle.ChestBoat;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Grand Line worlds play as an adventure RPG. Regular players cannot break or place blocks, open containers, use vanilla
 * crafting or utility blocks (crafting tables, furnaces, anvils, enchanting tables, brewing stands, lecterns...),
 * toggle doors, trapdoors, gates, buttons or levers, or touch item frames, armour stands and storage carts. NPCs,
 * combat, fishing, shops and every Grand Line screen are untouched: they are entities and items, not blocks.
 * Explosions break no blocks, fire does not spread and mobs do not grief. Everything is checked on the server (the
 * client checks too, only so nothing flickers). Operators at {@code adventureBypassPermission} bypass it all while in
 * creative mode or tagged {@code grandline_build} ({@code /tag <player> add grandline_build}).
 */
public final class AdventureRules {
	public static final String BUILD_TAG = "grandline_build";

	private AdventureRules() {
	}

	public static void register() {
		PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) -> !restricted(player));
		AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) ->
				restricted(player) ? InteractionResult.FAIL : InteractionResult.PASS);
		UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
			if (!restricted(player)) {
				return InteractionResult.PASS;
			}
			ItemStack held = player.getItemInHand(hand);
			if (held.getItem() instanceof BlockItem) {
				return InteractionResult.FAIL; // no placing
			}
			BlockState state = level.getBlockState(hit.getBlockPos());
			if (interactive(level, hit.getBlockPos(), state) && !allowedBlock(state)) {
				return InteractionResult.FAIL;
			}
			return InteractionResult.PASS; // plain blocks: the held item (rod, gun, fruit) is used as normal
		});
		UseEntityCallback.EVENT.register((player, level, hand, entity, hit) ->
				restricted(player) && protectedEntity(entity) ? InteractionResult.FAIL : InteractionResult.PASS);
		AttackEntityCallback.EVENT.register((player, level, hand, entity, hit) ->
				restricted(player) && protectedEntity(entity) ? InteractionResult.FAIL : InteractionResult.PASS);
		ServerLifecycleEvents.SERVER_STARTED.register(AdventureRules::gameRules);
	}

	/** Fire spread and mob griefing (endermen, creepers' terrain damage) off in adventure worlds. */
	private static void gameRules(MinecraftServer server) {
		if (activeIn(server.overworld())) {
			server.getGameRules().getRule(GameRules.RULE_DOFIRETICK).set(false, server);
			server.getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(false, server);
		}
	}

	/** Whether adventure mode applies in this world. */
	public static boolean activeIn(Level level) {
		var config = GrandLineMod.config();
		if (!config.adventureMode) {
			return false;
		}
		if (config.adventureModeAllWorlds) {
			return true;
		}
		if (level.getServer() == null) {
			return GrandLineWorldClientFlag.grandLineWorld;
		}
		return ModWorld.isGrandLineWorld(level.getServer().overworld());
	}

	/** Whether this player is held to adventure mode (not a bypassing operator). */
	public static boolean restricted(Player player) {
		if (!activeIn(player.level())) {
			return false;
		}
		boolean op = player.hasPermissions(GrandLineMod.config().adventureBypassPermission);
		if (op && player.level().isClientSide) {
			return false; // build tags are not known on the client: let the server decide for operators
		}
		return !(op && (player.isCreative() || player.getTags().contains(BUILD_TAG)));
	}

	/** Blocks with a use of their own that adventure mode blocks: storage, crafting, utilities, doors and switches. */
	public static boolean interactive(Level level, BlockPos pos, BlockState state) {
		Block b = state.getBlock();
		if (state.getMenuProvider(level, pos) != null || level.getBlockEntity(pos) instanceof net.minecraft.world.Container) {
			return true; // chests, barrels, shulkers, hoppers, dispensers, furnaces, crafting/anvil/grindstone/smithing/stonecutter...
		}
		return state.is(BlockTags.DOORS) || state.is(BlockTags.TRAPDOORS) || state.is(BlockTags.FENCE_GATES)
				|| state.is(BlockTags.BUTTONS) || state.is(BlockTags.BEDS) || state.is(BlockTags.CANDLES) || state.is(BlockTags.ANVIL)
				|| state.is(BlockTags.CAULDRONS) || state.is(BlockTags.FLOWER_POTS) || state.is(BlockTags.CAMPFIRES)
				|| b instanceof LeverBlock || b instanceof LecternBlock || b instanceof EnchantmentTableBlock || b instanceof BrewingStandBlock
				|| b instanceof CraftingTableBlock || b instanceof AbstractFurnaceBlock || b instanceof GrindstoneBlock
				|| b instanceof StonecutterBlock || b instanceof LoomBlock || b instanceof CartographyTableBlock
				|| b instanceof NoteBlock || b instanceof RepeaterBlock || b instanceof ComparatorBlock || b instanceof DaylightDetectorBlock
				|| b instanceof JukeboxBlock || b instanceof ComposterBlock || b instanceof BellBlock || b instanceof CakeBlock
				|| b instanceof RespawnAnchorBlock || b instanceof BeaconBlock || b instanceof ChiseledBookShelfBlock
				|| b instanceof DecoratedPotBlock || b instanceof SignBlock || b instanceof DragonEggBlock || b instanceof TntBlock;
	}

	static boolean allowedBlock(BlockState state) {
		return GrandLineMod.config().allowedBlockInteractions.contains(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
	}

	/** Decorations and storage entities players may not use or break (item frames, paintings, armour stands, chest carts). */
	static boolean protectedEntity(Entity entity) {
		return !(entity instanceof Islander) && (entity instanceof HangingEntity || entity instanceof ArmorStand
				|| entity instanceof AbstractMinecartContainer || entity instanceof ChestBoat);
	}

	/** Items a player may pick up or end up with: every Grand Line item, plus the configured allowlist. */
	public static boolean allowedItem(ItemStack stack) {
		var key = BuiltInRegistries.ITEM.getKey(stack.getItem());
		return key.getNamespace().equals(GrandLineMod.MOD_ID) || GrandLineMod.config().allowedItems.contains(key.toString());
	}

	/** Client-side copy of "this server runs a Grand Line world" (set from the island sync), for client-side checks. */
	public static final class GrandLineWorldClientFlag {
		public static volatile boolean grandLineWorld;

		private GrandLineWorldClientFlag() {
		}
	}
}
