package com.grandline.treasure;

import com.grandline.GrandLineMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import java.util.EnumMap;
import java.util.Map;

/** The three treasure chest blocks and their items (the items are for builders and admins; players never get them). */
public final class TreasureBlocks {
	private static final Map<TreasureTier, TreasureChestBlock> BLOCKS = new EnumMap<>(TreasureTier.class);

	static {
		for (TreasureTier tier : TreasureTier.values()) {
			TreasureChestBlock block = new TreasureChestBlock(tier, BlockBehaviour.Properties.of()
					.mapColor(tier == TreasureTier.LEGENDARY ? MapColor.COLOR_RED : MapColor.WOOD)
					.strength(-1.0f, 3_600_000f) // like bedrock: never broken in play
					.sound(SoundType.WOOD).noOcclusion().noLootTable()
					.lightLevel(state -> tier == TreasureTier.LEGENDARY ? 4 : 0));
			Registry.register(BuiltInRegistries.BLOCK, GrandLineMod.id("treasure_chest_" + tier.id()), block);
			Registry.register(BuiltInRegistries.ITEM, GrandLineMod.id("treasure_chest_" + tier.id()),
					new BlockItem(block, new Item.Properties().rarity(switch (tier) {
						case COMMON -> Rarity.COMMON;
						case RARE -> Rarity.RARE;
						case LEGENDARY -> Rarity.EPIC;
					})));
			BLOCKS.put(tier, block);
		}
	}

	private TreasureBlocks() {
	}

	public static TreasureChestBlock get(TreasureTier tier) {
		return BLOCKS.get(tier);
	}

	/** Forces class loading during mod initialization. */
	public static void register() {
	}
}
