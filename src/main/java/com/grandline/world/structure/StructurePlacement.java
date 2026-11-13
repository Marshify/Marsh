package com.grandline.world.structure;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;

import java.util.Locale;

/**
 * One supplied structure placed into the world, read from data/&lt;namespace&gt;/structure_placements/&lt;id&gt;.json:
 * <pre>{
 *   "template": "grandline:islands/millbrook",   // data/grandline/structures/islands/millbrook.nbt
 *   "position": [630, 62, 120],                   // world position of the template's "origin" marker
 *   "rotation": "none",                           // none | clockwise_90 | 180 | counterclockwise_90
 *   "mirror": "none",                             // none | left_right | front_back
 *   "island": "Millbrook Village"                 // optional: the SeaLayout island its NPCs belong to
 * }</pre>
 * Without an "origin" marker the template's minimum corner is placed at "position".
 */
public record StructurePlacement(ResourceLocation id, ResourceLocation template, BlockPos position, Rotation rotation, Mirror mirror,
		String island) {
	static StructurePlacement parse(ResourceLocation id, JsonObject json) {
		ResourceLocation template = new ResourceLocation(GsonHelper.getAsString(json, "template"));
		JsonArray pos = GsonHelper.getAsJsonArray(json, "position");
		if (pos.size() != 3) {
			throw new IllegalArgumentException("position must be [x, y, z]");
		}
		BlockPos position = new BlockPos(pos.get(0).getAsInt(), pos.get(1).getAsInt(), pos.get(2).getAsInt());
		Rotation rotation = switch (GsonHelper.getAsString(json, "rotation", "none").toLowerCase(Locale.ROOT)) {
			case "none" -> Rotation.NONE;
			case "clockwise_90" -> Rotation.CLOCKWISE_90;
			case "180", "clockwise_180" -> Rotation.CLOCKWISE_180;
			case "counterclockwise_90" -> Rotation.COUNTERCLOCKWISE_90;
			default -> throw new IllegalArgumentException("unknown rotation");
		};
		Mirror mirror = switch (GsonHelper.getAsString(json, "mirror", "none").toLowerCase(Locale.ROOT)) {
			case "none" -> Mirror.NONE;
			case "left_right" -> Mirror.LEFT_RIGHT;
			case "front_back" -> Mirror.FRONT_BACK;
			default -> throw new IllegalArgumentException("unknown mirror");
		};
		return new StructurePlacement(id, template, position, rotation, mirror, GsonHelper.getAsString(json, "island", ""));
	}
}
