package com.grandline.fruit;

import net.minecraft.ChatFormatting;

/** Fruit rarity: drives shop price, gacha weight and UI colour. */
public enum FruitRarity {
	COMMON(0xB0B7C3, ChatFormatting.GRAY, 50),
	UNCOMMON(0x4FC3E8, ChatFormatting.AQUA, 28),
	RARE(0x6F6BFF, ChatFormatting.BLUE, 14),
	LEGENDARY(0xC54BFF, ChatFormatting.LIGHT_PURPLE, 6),
	MYTHICAL(0xFF4B4B, ChatFormatting.RED, 2);

	private final int color;
	private final ChatFormatting formatting;
	private final int gachaWeight;

	FruitRarity(int color, ChatFormatting formatting, int gachaWeight) {
		this.color = color;
		this.formatting = formatting;
		this.gachaWeight = gachaWeight;
	}

	public int color() {
		return color;
	}

	public ChatFormatting formatting() {
		return formatting;
	}

	public int gachaWeight() {
		return gachaWeight;
	}

	public String translationKey() {
		return "rarity.grandline." + name().toLowerCase(java.util.Locale.ROOT);
	}
}
