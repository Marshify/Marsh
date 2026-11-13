package com.grandline.world;

import com.grandline.GrandLineMod;
import com.grandline.data.ModAttachments;
import com.grandline.network.payload.IslandListPayload;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** World features: island generation, the starter-island spawn point, island discovery and map sync. */
public final class ModWorld {
	public static final IslandFeature ISLAND = Registry.register(BuiltInRegistries.FEATURE, GrandLineMod.id("island"),
			new IslandFeature(NoneFeatureConfiguration.CODEC));
	public static final SeaIslandsFeature SEA_ISLANDS = Registry.register(BuiltInRegistries.FEATURE, GrandLineMod.id("sea_islands"),
			new SeaIslandsFeature(NoneFeatureConfiguration.CODEC));
	public static final ResourceKey<PlacedFeature> ISLAND_PLACED = ResourceKey.create(Registries.PLACED_FEATURE, GrandLineMod.id("island"));

	/** Islands within this many blocks of a player get discovered. */
	public static final int DISCOVERY_RANGE = 160;

	private ModWorld() {
	}

	public static void register() {
		IslandRegistry.register();
		BiomeModifications.addFeature(BiomeSelectors.tag(BiomeTags.IS_OCEAN), GenerationStep.Decoration.RAW_GENERATION, ISLAND_PLACED);
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents.LOAD.register((server, level) -> {
			if (level.dimension() == net.minecraft.world.level.Level.OVERWORLD && isGrandLineWorld(level)) {
				registerDesignedIslands(IslandRegistry.get(level));
			}
		});
		ServerLifecycleEvents.SERVER_STARTED.register(ModWorld::placeSpawnOnStarterIsland);
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			discoverStarter(handler.getPlayer());
			sync(handler.getPlayer());
		});
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % 40 == 0) {
				for (ServerPlayer player : server.getPlayerList().getPlayers()) {
					checkDiscovery(player);
				}
			}
		});
	}

	/** True for worlds created with the Grand Line world type (their ocean is the Grand Line Sea biome). */
	public static boolean isGrandLineWorld(ServerLevel overworld) {
		return overworld.getBiome(new BlockPos(0, 62, 0)).unwrapKey()
				.map(key -> key.location().equals(GrandLineMod.id("grand_line_sea"))).orElse(false);
	}

	/** Adds every designed island of all seas to the registry (they exist from the start, like a real map). */
	private static void registerDesignedIslands(IslandRegistry registry) {
		removeRetiredDesignedIslands(registry);
		for (SeaLayout.Spec spec : SeaLayout.all()) {
			registry.add(new Island(SeaLayout.idOf(spec), spec.x(), spec.z(), spec.radius(), spec.level(), spec.name(), spec.starter(),
					false, spec.theme().name().toLowerCase(java.util.Locale.ROOT)));
		}
	}

	/**
	 * Drops designed islands that a world saved but {@link SeaLayout} no longer has (e.g. Hollow Islet), so they leave
	 * the chart, travel and island lookups. Already-generated blocks and entities in those chunks are left as they are.
	 * Random (undesigned) islands are never touched. Returns how many were removed.
	 */
	public static int removeRetiredDesignedIslands(IslandRegistry registry) {
		java.util.Set<Long> current = new java.util.HashSet<>();
		for (SeaLayout.Spec spec : SeaLayout.all()) {
			current.add(SeaLayout.idOf(spec));
		}
		java.util.List<String> names = new java.util.ArrayList<>();
		int removed = registry.removeIf(island -> {
			boolean retired = island.designed() && !current.contains(island.id());
			if (retired) {
				names.add(island.name());
			}
			return retired;
		});
		if (removed > 0) {
			GrandLineMod.LOGGER.info("Removed {} retired designed island(s) from the island registry: {}", removed, names);
		}
		return removed;
	}

	/** First start of a world with a starter island: move the world spawn onto it. */
	private static void placeSpawnOnStarterIsland(MinecraftServer server) {
		ServerLevel overworld = server.overworld();
		IslandRegistry registry = IslandRegistry.get(overworld);
		if (registry.spawnPlaced()) {
			return;
		}
		overworld.getChunk(0, 0); // make sure the starter chunk is generated
		for (Island island : registry.all()) {
			if (island.starter() && (!island.designed() || island.sea() == 1)) {
				int y = overworld.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, island.x(), island.z());
				overworld.setDefaultSpawnPos(new BlockPos(island.x(), y, island.z()), 0);
				GrandLineMod.LOGGER.info("World spawn moved onto {} at {}, {}, {}", island.name(), island.x(), y, island.z());
				break;
			}
		}
		registry.markSpawnPlaced();
	}

	public static Set<Long> discovered(ServerPlayer player) {
		return new HashSet<>(player.getAttachedOrCreate(ModAttachments.DISCOVERED_ISLANDS));
	}

	private static void discoverStarter(ServerPlayer player) {
		for (Island island : IslandRegistry.get(player.server).all()) {
			if (island.starter()) {
				discover(player, island, false);
			}
		}
	}

	private static void checkDiscovery(ServerPlayer player) {
		if (player.level().dimension() != net.minecraft.world.level.Level.OVERWORLD) {
			return;
		}
		for (Island island : IslandRegistry.nearby(player.server, player.getBlockX(), player.getBlockZ(), DISCOVERY_RANGE)) {
			discover(player, island, true);
		}
	}

	/** Marks {@code island} as discovered. Returns true if it was new. */
	public static boolean discover(ServerPlayer player, Island island, boolean announce) {
		Set<Long> known = discovered(player);
		if (!known.add(island.id())) {
			return false;
		}
		player.setAttached(ModAttachments.DISCOVERED_ISLANDS, List.copyOf(known));
		if (announce) {
			player.sendSystemMessage(Component.translatable("message.grandline.island.discovered", island.name(), island.level(),
					island.sea()).withStyle(ChatFormatting.AQUA));
		}
		sync(player);
		return true;
	}

	/** Sends the player every island they have discovered (for the world map). */
	public static void sync(ServerPlayer player) {
		if (!ServerPlayNetworking.canSend(player, IslandListPayload.TYPE)) {
			return;
		}
		Set<Long> known = discovered(player);
		List<Island> visible = new ArrayList<>();
		for (Island island : IslandRegistry.get(player.server).all()) {
			// Designed islands are always on the chart (like a real sea map); random islands appear once discovered.
			if (island.designed() || known.contains(island.id())) {
				visible.add(island);
			}
		}
		ServerPlayNetworking.send(player, new IslandListPayload(visible));
	}
}
