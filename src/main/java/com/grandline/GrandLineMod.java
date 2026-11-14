package com.grandline;

import com.grandline.combat.EnergyService;
import com.grandline.combat.StatEffects;
import com.grandline.combat.style.FightingStyles;
import com.grandline.command.GrandLineCommands;
import com.grandline.config.GrandLineConfig;
import com.grandline.data.ModAttachments;
import com.grandline.fruit.FruitService;
import com.grandline.fruit.Fruits;
import com.grandline.fruit.LingeringAreas;
import com.grandline.network.GrandLineNetworking;
import com.grandline.player.PlayerProfileEvents;
import com.grandline.player.reward.MobKillRewards;
import com.grandline.registry.ModCreativeTabs;
import com.grandline.registry.ModItems;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common (client + dedicated server) entrypoint for Grand Line.
 *
 * <p>This class only wires subsystems together. Each subsystem owns its own registration so that
 * gameplay logic, networking, persistence and configuration stay in separate packages.
 */
public final class GrandLineMod implements ModInitializer {
	public static final String MOD_ID = "grandline";
	public static final Logger LOGGER = LoggerFactory.getLogger("GrandLine");

	private static GrandLineConfig config = GrandLineConfig.defaults();

	@Override
	public void onInitialize() {
		config = GrandLineConfig.loadOrCreate(FabricLoader.getInstance().getConfigDir().resolve(MOD_ID + ".json"));

		// Order matters only in that everything must be registered before registries freeze.
		ModItems.register();
		com.grandline.gear.Gear.register();
		com.grandline.gear.GearService.register();
		com.grandline.player.TeamService.register();
		com.grandline.reputation.ReputationService.register();
		com.grandline.player.loadout.LoadoutOrderService.register();
		com.grandline.ship.ShipEntities.register();
		com.grandline.ship.ShipService.register();
		com.grandline.world.VanillaSpawns.register();
		com.grandline.player.rules.CoreRules.register();
		com.grandline.world.IslandSpawns.register();
		com.grandline.world.IslandArrival.register();
		com.grandline.world.ClearWeather.register();
		net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.JOIN.register(
				(handler, sender, server) -> com.grandline.npc.QuestService.dropImpossibleQuest(handler.getPlayer()));
		com.grandline.reputation.Leaderboards.register();
		com.grandline.world.structure.MarkerProcessor.register();
		com.grandline.world.structure.StructurePlacements.register();
		com.grandline.world.structure.StructurePopulation.register();
		com.grandline.world.structure.TravelService.register();
		com.grandline.world.MapTeleport.register();
		ModCreativeTabs.register();
		ModAttachments.register();
		FightingStyles.register();
		Fruits.register();
		GrandLineNetworking.registerServerReceivers();
		PlayerProfileEvents.register();
		StatEffects.register();
		EnergyService.register();
		com.grandline.combat.ability.TimedEffects.register();
		com.grandline.combat.ability.ChargeService.register();
		com.grandline.combat.ability.FlightService.register();
		com.grandline.combat.CombatCredit.register();
		MobKillRewards.register();
		LingeringAreas.register();
		FruitService.register();
		com.grandline.world.ModWorld.register();
		com.grandline.entity.GrandLineEntities.register();
		com.grandline.npc.FishingService.register();
		com.grandline.fishing.FishingSessions.register();
		com.grandline.npc.IslandPopulation.register();
		com.grandline.npc.KeeperNpcs.register();
		com.grandline.quest.QuestUnlocks.register();
		com.grandline.quest.QuestNavigation.register();
		com.grandline.world.AdventureRules.register();
		com.grandline.npc.EnemyRespawns.register();
		com.grandline.npc.TempleBosses.register();
		com.grandline.race.RaceService.register();
		com.grandline.race.RacePassives.register();
		com.grandline.race.RaceAbilities.register();
		com.grandline.race.RaceAwakening.register();
		com.grandline.race.RaceProgress.register();
		com.grandline.race.RaceTrials.register();
		com.grandline.registry.ModSpawnEggs.register();
		GrandLineCommands.register();

		LOGGER.info("Grand Line {} initialized", modVersion());
	}

	/** Builds an identifier in the {@code grandline} namespace. */
	public static ResourceLocation id(String path) {
		return new ResourceLocation(MOD_ID, path);
	}

	/** The active, validated configuration. Never null. */
	public static GrandLineConfig config() {
		return config;
	}

	public static String modVersion() {
		return FabricLoader.getInstance()
				.getModContainer(MOD_ID)
				.map(container -> container.getMetadata().getVersion().getFriendlyString())
				.orElse("unknown");
	}
}
