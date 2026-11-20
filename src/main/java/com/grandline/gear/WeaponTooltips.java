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

	/** The same rarity colours as fruits. */
	static int rarityColor(String rarity) {
		return com.grandline.fruit.FruitRarity.valueOf(rarity.toUpperCase(java.util.Locale.ROOT)).color();
	}

	static void moves(Item item, List<Component> tooltip) {
		Power power = Gear.powerFor(item);
		if (power == null) {
			return;
		}
		var spec = com.grandline.gear.sword.Swords.spec(item);
		if (spec != null) {
			tooltip.add(Component.translatable("tooltip.grandline.sword_rarity", Component.translatable("rarity.grandline." + spec.rarity()))
					.withStyle(style -> style.withColor(rarityColor(spec.rarity()))));
		}
		for (int i = 0; i < power.abilities().size(); i++) {
			AbilityDef ability = power.abilities().get(i);
			tooltip.add(Component.literal("[" + KEYS[i] + "] ").withStyle(ChatFormatting.GOLD)
					.append(Component.translatable(ability.translationKey()).withStyle(ChatFormatting.WHITE))
					.append(Component.literal("  ★" + ability.requiredMastery()).withStyle(ChatFormatting.YELLOW)));
		}
		if (spec != null && spec.upgradeable()) {
			tooltip.add(Component.translatable("tooltip.grandline.sword_upgrade", com.grandline.GrandLineMod.config().swordUpgradeMastery)
					.withStyle(ChatFormatting.AQUA));
		}
		tooltip.add(Component.translatable("tooltip.grandline.weapon_hint").withStyle(ChatFormatting.DARK_GRAY));
	}
}
