package com.grandline.world;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * No natural pumpkins, melons or mushrooms in Grand Line worlds. The Grand Line sea biome does not list them (its
 * worldgen file), and for the vanilla biomes given to islands (plains, jungle, meadow...) the vanilla features that scatter pumpkin and melon
 * patches, red and brown mushroom patches and giant mushrooms are skipped while chunks of a Grand Line overworld are
 * generated (see ConfiguredFeatureMixin). Every biome, island and sea is covered, including islands added later, because
 * it works on the features themselves, not on where they are used. Only world generation is affected: bone meal still
 * grows giant mushrooms, and players can still place, farm and grow all of these. Chunks generated before this change
 * keep what they have; island decorations placed by the island plans (scarecrows, market stalls) are not features and
 * are kept.
 */
public final class NaturalCrops {
	public static final List<ResourceKey<ConfiguredFeature<?, ?>>> BLOCKED = List.of(
			key("patch_pumpkin"), key("patch_melon"), key("patch_brown_mushroom"), key("patch_red_mushroom"),
			key("huge_brown_mushroom"), key("huge_red_mushroom"), key("mushroom_island_vegetation"));

	/** How many blocked features were skipped since the server started (logged when it stops). */
	public static final java.util.concurrent.atomic.AtomicLong SKIPPED = new java.util.concurrent.atomic.AtomicLong();
	private static final Map<ServerLevel, Boolean> GRAND_LINE = Collections.synchronizedMap(new WeakHashMap<>());
	private static volatile Object registryOwner;
	private static volatile Set<ConfiguredFeature<?, ?>> blocked = Set.of();

	private NaturalCrops() {
	}

	public static void register() {
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			long n = SKIPPED.getAndSet(0);
			if (n > 0) {
				com.grandline.GrandLineMod.LOGGER.info("Kept {} natural pumpkin, melon and mushroom feature(s) out of newly generated chunks", n);
			}
		});
	}

	private static ResourceKey<ConfiguredFeature<?, ?>> key(String path) {
		return ResourceKey.create(Registries.CONFIGURED_FEATURE, new ResourceLocation("minecraft", path));
	}

	/** Whether {@code feature} must not be placed now: one of the blocked features, during generation of a Grand Line overworld chunk. */
	public static boolean skip(ConfiguredFeature<?, ?> feature, WorldGenLevel level) {
		if (!(level instanceof WorldGenRegion region)) {
			return false; // bone meal, commands and other placement outside chunk generation
		}
		ServerLevel server = region.getLevel();
		if (server.dimension() != Level.OVERWORLD || !GRAND_LINE.computeIfAbsent(server, ModWorld::isGrandLineWorld)) {
			return false;
		}
		if (blockedFeatures(server).contains(feature)) {
			SKIPPED.incrementAndGet();
			return true;
		}
		return false;
	}

	/** The blocked features as registered in this server's registries (compared by identity, as the game stores them). */
	public static Set<ConfiguredFeature<?, ?>> blockedFeatures(ServerLevel server) {
		var registries = server.registryAccess();
		if (registryOwner != registries) {
			Set<ConfiguredFeature<?, ?>> set = Collections.newSetFromMap(new IdentityHashMap<>());
			var registry = registries.registryOrThrow(Registries.CONFIGURED_FEATURE);
			for (var k : BLOCKED) {
				var f = registry.get(k);
				if (f != null) {
					set.add(f);
				}
			}
			blocked = Collections.unmodifiableSet(set);
			registryOwner = registries;
		}
		return blocked;
	}
}
