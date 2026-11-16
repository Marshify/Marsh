package com.grandline.treasure;

import net.minecraft.ChatFormatting;

/** The three kinds of treasure chest. Rewards and refill times are in the config (see TreasureService). */
public enum TreasureTier {
	COMMON(ChatFormatting.WHITE),
	RARE(ChatFormatting.AQUA),
	LEGENDARY(ChatFormatting.GOLD);

	public final ChatFormatting color;

	TreasureTier(ChatFormatting color) {
		this.color = color;
	}

	public String id() {
		return name().toLowerCase(java.util.Locale.ROOT);
	}
}
