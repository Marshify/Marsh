package com.grandline.gear;

import com.grandline.player.StatType;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * A wearable accessory (cape, coat, hat) or a ring. Right-click to equip: accessories go in the accessory slot, rings
 * in the first free ring slot (or replace the first). Bonuses apply on the server while equipped ({@link GearService});
 * the tooltip shows the values from {@link Kind#bonuses()}.
 */
public class AccessoryItem extends Item {
	public enum Slot { ACCESSORY, RING }

	/** The four Batch 01 accessories. Damage percentages are fixed; HP and energy come from the config. */
	public enum Kind {
		BLACK_CAPE, PINK_COAT, SWORDSMAN_HAT, TOMOE_RING;

		public GearBonuses bonuses() {
			var config = com.grandline.GrandLineMod.config();
			return switch (this) {
				case BLACK_CAPE -> new GearBonuses(0.05, 0.05, 0.05, 0.05, config.blackCapeHealth, config.blackCapeEnergy);
				case PINK_COAT -> new GearBonuses(0, 0, 0.10, 0, config.pinkCoatHealth, 0);
				case SWORDSMAN_HAT -> new GearBonuses(0, 0.10, 0, 0, 0, 0);
				case TOMOE_RING -> new GearBonuses(0, 0, 0, 0.10, 0, 0);
			};
		}
	}

	private final Slot slot;
	private final Kind kind;

	public AccessoryItem(Properties properties, Slot slot, Kind kind) {
		super(properties.stacksTo(1));
		this.slot = slot;
		this.kind = kind;
	}

	public Slot slot() {
		return slot;
	}

	public Kind kind() {
		return kind;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
			GearService.equip(serverPlayer, hand);
		}
		return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
	}

	@Override
	public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(Component.translatable(slot == Slot.RING ? "tooltip.grandline.ring" : "tooltip.grandline.accessory")
				.withStyle(ChatFormatting.GRAY));
		tooltip.addAll(kind.bonuses().describe());
	}

	/** Damage percentage for a combat category (MELEE, SWORD, GUN, FRUIT). */
	static double damage(GearBonuses b, StatType category) {
		return b.damage(category);
	}
}
