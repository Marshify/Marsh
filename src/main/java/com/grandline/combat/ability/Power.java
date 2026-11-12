package com.grandline.combat.ability;

import com.grandline.player.StatType;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * A fighting style or a fruit: an id, the stat that scales its damage, and exactly four moves (Z, X, C, V).
 *
 * @param rarity display rarity (fruits); fighting styles use {@code "style"}
 * @param color  HUD/UI accent colour (0xRRGGBB)
 */
public record Power(ResourceLocation id, PowerSource source, StatType scalingStat, String rarity, int color, List<AbilityDef> abilities) {
	public static final int SLOT_COUNT = 4;

	public Power {
		if (abilities.size() != SLOT_COUNT) {
			throw new IllegalArgumentException(id + " must have exactly " + SLOT_COUNT + " abilities");
		}
		abilities = List.copyOf(abilities);
	}

	public String translationKey() {
		return "power." + id.getNamespace() + "." + id.getPath();
	}

	/** Cooldown key for one move, unique across all powers. */
	public ResourceLocation abilityId(AbilityDef ability) {
		return new ResourceLocation(id.getNamespace(), id.getPath() + "/" + ability.key());
	}
}
