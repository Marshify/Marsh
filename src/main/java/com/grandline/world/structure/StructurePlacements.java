package com.grandline.world.structure;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.grandline.GrandLineMod;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Places supplied structure templates (.nbt from Structure Blocks) into the Grand Line sea as chunks generate.
 *
 * <ul>
 *   <li>Placements come from data packs: data/&lt;ns&gt;/structure_placements/*.json ({@link StructurePlacement}).</li>
 *   <li>Markers are Structure Blocks in DATA mode inside the template; their data string names them ("origin",
 *   "dock", "npc:quest_giver", "arrival:upper_sky", "quest"...). They are read here and never placed.</li>
 *   <li>Each chunk places only the part of each template inside it, once, when the chunk is first generated. Existing
 *   chunks (and anything players built in them) are never touched again, so reloading cannot overwrite builds.</li>
 * </ul>
 */
public final class StructurePlacements {
	/** A named marker resolved to world coordinates. */
	public record Marker(String name, BlockPos pos, StructurePlacement placement, int index) {
	}

	/** A placement resolved against its template: where it starts, how big it is, and its markers. */
	public record Resolved(StructurePlacement placement, StructureTemplate template, BlockPos corner, BoundingBox bounds, List<Marker> markers) {
	}

	private static volatile List<StructurePlacement> loaded = List.of();
	private static volatile List<Resolved> resolved;
	/** Arrival points added by code (tests); merged with the "arrival:*" markers of placed structures. */
	private static final Map<String, BlockPos> EXTRA_ARRIVALS = new HashMap<>();

	private StructurePlacements() {
	}

	public static void register() {
		ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
			@Override
			public ResourceLocation getFabricId() {
				return GrandLineMod.id("structure_placements");
			}

			@Override
			public void onResourceManagerReload(ResourceManager manager) {
				List<StructurePlacement> list = new ArrayList<>();
				manager.listResources("structure_placements", path -> path.getPath().endsWith(".json")).forEach((file, resource) -> {
					String p = file.getPath();
					ResourceLocation id = new ResourceLocation(file.getNamespace(), p.substring("structure_placements/".length(), p.length() - 5));
					try (Reader reader = resource.openAsReader()) {
						JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
						list.add(StructurePlacement.parse(id, json));
					} catch (Exception e) {
						GrandLineMod.LOGGER.error("Bad structure placement {}: {}", file, e.toString());
					}
				});
				loaded = List.copyOf(list);
				resolved = null; // re-resolve against templates on next use
				GrandLineMod.LOGGER.info("Loaded {} structure placement(s)", list.size());
			}
		});
	}

	/** Resolves every placement against its template (cached until the next data reload). Safe from any thread. */
	public static List<Resolved> resolved(MinecraftServer server) {
		List<Resolved> current = resolved;
		if (current != null) {
			return current;
		}
		synchronized (StructurePlacements.class) {
			if (resolved == null) {
				resolved = resolveAll(server.getStructureManager());
			}
			return resolved;
		}
	}

	private static List<Resolved> resolveAll(StructureTemplateManager templates) {
		List<Resolved> out = new ArrayList<>();
		for (StructurePlacement placement : loaded) {
			StructureTemplate template = templates.get(placement.template()).orElse(null);
			if (template == null) {
				GrandLineMod.LOGGER.error("Structure placement {}: template {} not found", placement.id(), placement.template());
				continue;
			}
			StructurePlaceSettings settings = new StructurePlaceSettings().setRotation(placement.rotation()).setMirror(placement.mirror());
			// Markers in template-local coordinates, transformed by rotation/mirror.
			List<StructureTemplate.StructureBlockInfo> infos = template.filterBlocks(BlockPos.ZERO, settings, Blocks.STRUCTURE_BLOCK);
			BlockPos originLocal = BlockPos.ZERO;
			for (StructureTemplate.StructureBlockInfo info : infos) {
				if ("origin".equals(markerName(info.nbt()))) {
					originLocal = info.pos();
				}
			}
			BlockPos corner = placement.position().subtract(originLocal);
			List<Marker> markers = new ArrayList<>();
			int index = 0;
			for (StructureTemplate.StructureBlockInfo info : infos) {
				String name = markerName(info.nbt());
				if (name != null) {
					markers.add(new Marker(name, corner.offset(info.pos()), placement, index++));
				}
			}
			BoundingBox bounds = template.getBoundingBox(settings, corner);
			out.add(new Resolved(placement, template, corner, bounds, List.copyOf(markers)));
			GrandLineMod.LOGGER.info("Structure {} at {} spans {} with {} marker(s)", placement.id(), corner, bounds, markers.size());
		}
		return List.copyOf(out);
	}

	/** The data string of a DATA-mode structure block, or null. */
	private static String markerName(CompoundTag nbt) {
		if (nbt == null || !"DATA".equals(nbt.getString("mode"))) {
			return null;
		}
		String data = nbt.getString("metadata").trim();
		return data.isEmpty() ? null : data;
	}

	/** Places the part of every structure that lies in {@code chunk}. Called once per chunk from world generation. */
	public static boolean placeInChunk(WorldGenLevel level, ChunkPos chunk) {
		List<Resolved> all = resolved(level.getServer());
		if (all.isEmpty()) {
			return false;
		}
		BoundingBox chunkBox = new BoundingBox(chunk.getMinBlockX(), level.getMinBuildHeight(), chunk.getMinBlockZ(),
				chunk.getMaxBlockX(), level.getMaxBuildHeight() - 1, chunk.getMaxBlockZ());
		boolean placed = false;
		for (Resolved r : all) {
			if (!r.bounds().intersects(chunkBox)) {
				continue;
			}
			StructurePlaceSettings settings = new StructurePlaceSettings()
					.setRotation(r.placement().rotation())
					.setMirror(r.placement().mirror())
					.setBoundingBox(chunkBox)            // only this chunk's slice
					.setKeepLiquids(false)                // supplied air stays air: no waterlogging or ocean inside builds
					.addProcessor(MarkerProcessor.INSTANCE); // markers are data: air in rooms, untouched sea at docks
			RandomSource random = RandomSource.create(r.placement().id().hashCode());
			r.template().placeInWorld(level, r.corner(), r.corner(), settings, random, 2);
			placed = true;
		}
		return placed;
	}

	/** World position of an arrival ("arrival:&lt;name&gt;" marker or a code-registered one), or null. */
	public static BlockPos arrival(MinecraftServer server, String name) {
		synchronized (EXTRA_ARRIVALS) {
			BlockPos extra = EXTRA_ARRIVALS.get(name);
			if (extra != null) {
				return extra;
			}
		}
		for (Resolved r : resolved(server)) {
			for (Marker m : r.markers()) {
				if (m.name().equals("arrival:" + name)) {
					return m.pos();
				}
			}
		}
		return null;
	}

	/** Registers an arrival point from code (used by tests and future hand-coded islands). */
	public static void registerArrival(String name, BlockPos pos) {
		synchronized (EXTRA_ARRIVALS) {
			EXTRA_ARRIVALS.put(name, pos.immutable());
		}
	}

	public static List<Marker> markers(MinecraftServer server) {
		List<Marker> all = new ArrayList<>();
		resolved(server).forEach(r -> all.addAll(r.markers()));
		return all;
	}
}
