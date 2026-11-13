package com.grandline.gear;

import com.grandline.combat.ability.AbilityDef;
import com.grandline.combat.ability.Power;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;

import java.util.List;

/** Lists a weapon's moves (key, name, mastery needed) in its tooltip. */
final class WeaponTooltips {
	private static final String[] KEYS = {"Z", "X", "C", "V", "F"};

	private WeaponTooltips() {
	}

	static void moves(Item item, List<Component> tooltip) {
		Power power = Gear.powerFor(item);
		if (power == null) {
			return;
		}
		for (int i = 0; i < power.abilities().size(); i++) {
			AbilityDef ability = power.abilities().get(i);
			tooltip.add(Component.literal("[" + KEYS[i] + "] ").withStyle(ChatFormatting.GOLD)
					.append(Component.translatable(ability.translationKey()).withStyle(ChatFormatting.WHITE))
					.append(Component.literal("  ★" + ability.requiredMastery()).withStyle(ChatFormatting.YELLOW)));
		}
		tooltip.add(Component.translatable("tooltip.grandline.weapon_hint").withStyle(ChatFormatting.DARK_GRAY));
	}
}
