package com.grandline;

import com.grandline.combat.StatEffects;
import com.grandline.command.GrandLineCommands;
import com.grandline.config.GrandLineConfig;
import com.grandline.data.ModAttachments;
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
		ModCreativeTabs.register();
		ModAttachments.register();
		GrandLineNetworking.registerServerReceivers();
		PlayerProfileEvents.register();
		StatEffects.register();
		MobKillRewards.register();
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
