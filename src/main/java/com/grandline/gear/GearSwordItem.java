package com.grandline.gear;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** A Grand Line sword: a normal melee sword whose tooltip also lists its moves. */
public class GearSwordItem extends SwordItem {
	public GearSwordItem(Tier tier, int damage, float speed, Properties properties) {
		super(tier, damage, speed, properties);
	}

	@Override
	public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
		WeaponTooltips.moves(this, tooltip);
	}
}
