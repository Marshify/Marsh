package com.grandline.world.structure;

import com.grandline.GrandLineMod;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/**
 * Removes marker structure blocks when a template is placed. Markers that mark water ("dock") keep whatever block the
 * world already has there (the sea); every other marker becomes air, so a marker inside a dry room never leaves a
 * pocket of ocean water behind.
 */
public final class MarkerProcessor extends StructureProcessor {
	public static final MarkerProcessor INSTANCE = new MarkerProcessor();
	public static final Codec<MarkerProcessor> CODEC = Codec.unit(() -> INSTANCE);
	public static final StructureProcessorType<MarkerProcessor> TYPE = Registry.register(BuiltInRegistries.STRUCTURE_PROCESSOR,
			GrandLineMod.id("markers"), () -> CODEC);

	private MarkerProcessor() {
	}

	@Override
	public StructureTemplate.StructureBlockInfo processBlock(LevelReader level, BlockPos offset, BlockPos pos,
			StructureTemplate.StructureBlockInfo original, StructureTemplate.StructureBlockInfo current, StructurePlaceSettings settings) {
		if (!current.state().is(Blocks.STRUCTURE_BLOCK)) {
			return current;
		}
		String name = current.nbt() == null ? "" : current.nbt().getString("metadata").trim();
		if (name.equals("dock")) {
			return null; // leave the world's block (water) untouched
		}
		return new StructureTemplate.StructureBlockInfo(current.pos(), Blocks.AIR.defaultBlockState(), null);
	}

	@Override
	protected StructureProcessorType<?> getType() {
		return TYPE;
	}

	/** Forces registration during mod init. */
	public static void register() {
		GrandLineMod.LOGGER.debug("Registered structure processor {}", BuiltInRegistries.STRUCTURE_PROCESSOR.getKey(TYPE));
	}
}
