package com.grandline.world;

import com.grandline.GrandLineMod;
import com.grandline.world.plan.IslandPlan;
import com.grandline.world.plan.IslandPlans;
import com.grandline.world.plan.Terrain;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;

import java.util.ArrayList;
import java.util.List;

/**
 * Island biomes. Every Grand Line world is one sea biome; each island's footprint (its land and the beach round it)
 * is given a fitting vanilla biome instead: desert for desert islands, snowy plains for snowy ones, and so on (grass
 * and foliage colours, sky, ambience). It is written into the chunk's biome cells (4 x 4 columns, full height) while
 * the island generates; {@link #migrate} does the same for chunks generated before (the /grandline biomes command).
 * Only the look changes: no vanilla mobs spawn (see VanillaSpawns), weather stays as configured, and sea-level water
 * never freezes in Grand Line worlds (see BiomeFreezeMixin).
 */
public final class IslandBiomes {
	private IslandBiomes() {
	}

	/** The biome for an island theme, or null to leave it as sea. */
	public static ResourceKey<Biome> biomeFor(IslandTheme theme) {
		String id = switch (theme) {
			case DESERT -> "desert";
			case SNOW -> "snowy_plains";
			case JUNGLE, PALACE -> "jungle";
			case VOLCANO -> "badlands";
			case SKY -> "meadow";
			case PRISON, CAVE -> "stony_shore";
			case COLOSSEUM -> "savanna";
			case TOWN, VILLAGE, BRICK_TOWN, FORTRESS, HARBOR, CRYSTAL, PLAIN -> "plains";
		};
		return ResourceKey.create(Registries.BIOME, new ResourceLocation("minecraft", id));
	}

	/** Whether island-local (lx, lz) is part of the island's footprint (land or beach). */
	static boolean footprint(SeaLayout.Spec spec, IslandPlan plan, IslandBuilder builder, int lx, int lz) {
		if (plan != null) {
			return (long) lx * lx + (long) lz * lz <= (long) plan.extent() * plan.extent() && plan.biomeFootprint(lx, lz);
		}
		return builder.ground(lx, lz) >= Terrain.WATER_TOP;
	}

	/**
	 * Writes island biomes into every 4 x 4 cell of {@code chunk} that touches an island's footprint (a cell counts
	 * when its middle or any corner column is land, so the beach is included). Returns the cells changed.
	 */
	public static int apply(WorldGenLevel level, ChunkAccess chunk) {
		ChunkPos pos = chunk.getPos();
		int minX = pos.getMinBlockX(), minZ = pos.getMinBlockZ();
		int changed = 0;
		for (SeaLayout.Spec spec : SeaLayout.all()) {
			IslandPlan plan = IslandPlans.get(spec.name());
			boolean near = plan != null ? IslandPlans.touches(spec, plan, minX, minZ) : IslandBuilder.touches(spec, minX, minZ);
			if (!near) {
				continue;
			}
			Holder<Biome> biome = level.registryAccess().registryOrThrow(Registries.BIOME).getHolder(biomeFor(spec.theme())).orElse(null);
			if (biome == null) {
				continue;
			}
			IslandBuilder builder = plan == null ? new IslandBuilder(level, spec, minX, minZ) : null;
			for (int qx = 0; qx < 4; qx++) {
				for (int qz = 0; qz < 4; qz++) {
					int x0 = minX + qx * 4 - spec.x(), z0 = minZ + qz * 4 - spec.z();
					boolean land = footprint(spec, plan, builder, x0 + 2, z0 + 2);
					for (int k = 0; k < 4 && !land; k++) {
						land = footprint(spec, plan, builder, x0 + (k % 2) * 3, z0 + (k / 2) * 3);
					}
					if (!land) {
						continue;
					}
					for (LevelChunkSection section : chunk.getSections()) {
						if (section.getBiomes() instanceof PalettedContainer<Holder<Biome>> biomes) {
							for (int qy = 0; qy < 4; qy++) {
								biomes.getAndSet(qx, qy, qz, biome);
							}
						}
					}
					changed++;
				}
			}
		}
		return changed;
	}

	/**
	 * Migration for worlds made before island biomes: gives the islands' already generated chunks their biomes (chunks
	 * not generated yet get them as they generate). {@code only} limits it to one island (null for all). Returns the
	 * chunks changed; players nearby see the new biomes at once.
	 */
	public static int migrate(ServerLevel level, String only) {
		List<ChunkAccess> changed = new ArrayList<>();
		var chunkMap = level.getChunkSource().chunkMap;
		for (SeaLayout.Spec spec : SeaLayout.all()) {
			if (only != null && !spec.name().equalsIgnoreCase(only)) {
				continue;
			}
			IslandPlan plan = IslandPlans.get(spec.name());
			int reach = plan != null ? plan.extent() + 4 : (int) (spec.radius() * 1.45) + 4;
			for (int cx = (spec.x() - reach) >> 4; cx <= (spec.x() + reach) >> 4; cx++) {
				for (int cz = (spec.z() - reach) >> 4; cz <= (spec.z() + reach) >> 4; cz++) {
					ChunkPos cp = new ChunkPos(cx, cz);
					boolean loaded = level.getChunkSource().getChunkNow(cx, cz) != null;
					if (!loaded && chunkMap.read(cp).join().isEmpty()) {
						continue; // never generated: it will get its biomes when it is
					}
					var chunk = level.getChunk(cx, cz);
					if (apply(level, chunk) > 0) {
						chunk.setUnsaved(true);
						changed.add(chunk);
					}
				}
			}
		}
		if (!changed.isEmpty()) {
			chunkMap.resendBiomesForChunks(changed);
		}
		GrandLineMod.LOGGER.info("Island biomes: migrated {} chunks{}", changed.size(), only == null ? "" : " of " + only);
		return changed.size();
	}
}
