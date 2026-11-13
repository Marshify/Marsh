package com.grandline.fruit;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.function.Supplier;

/**
 * An item whose id was renamed. Minecraft 1.20.1 has no registry aliases, so the old id stays registered and any stack
 * of it becomes the new item (same count and tag) as soon as it sits in a player's inventory.
 */
public final class RenamedItem extends Item {
	private final Supplier<Item> replacement;

	public RenamedItem(Supplier<Item> replacement, Properties properties) {
		super(properties);
		this.replacement = replacement;
	}

	public Item replacement() {
		return replacement.get();
	}

	@Override
	public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
		if (level.isClientSide || !(entity instanceof Player player)) {
			return;
		}
		ItemStack renamed = new ItemStack(replacement.get(), stack.getCount());
		if (stack.getTag() != null) {
			renamed.setTag(stack.getTag().copy());
		}
		if (player.getInventory().getItem(slot) == stack) {
			player.getInventory().setItem(slot, renamed);
		} else if (player.getOffhandItem() == stack) {
			player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, renamed);
		}
	}

	@Override
	public net.minecraft.network.chat.Component getName(ItemStack stack) {
		return replacement.get().getName(stack);
	}
}
