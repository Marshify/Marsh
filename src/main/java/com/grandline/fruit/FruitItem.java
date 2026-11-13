package com.grandline.fruit;

import com.grandline.combat.ability.AbilityDef;
import com.grandline.combat.ability.Power;
import com.grandline.combat.ability.Powers;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A fruit item. Right-click opens a dialog (Eat / Drop / Store / Nevermind); the chosen action is sent to the server,
 * which validates it in {@link FruitService#useHeldFruit}. Eating replaces the player's current fruit.
 */
public final class FruitItem extends Item {
	private final ResourceLocation powerId;

	public FruitItem(ResourceLocation powerId, Properties properties) {
		super(properties);
		this.powerId = powerId;
	}

	public ResourceLocation powerId() {
		return powerId;
	}

	/**
	 * Client hook that opens the Eat / Drop / Store dialog. Set by the client entrypoint; null on a dedicated server.
	 * Kept as a plain Runnable-style hook so this common class never references client classes.
	 */
	public static java.util.function.BiConsumer<FruitItem, InteractionHand> openDialogHook;

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (level.isClientSide && openDialogHook != null) {
			openDialogHook.accept(this, hand);
		}
		// Nothing happens server-side here: the dialog sends a FruitActionPayload that the server validates.
		return InteractionResultHolder.success(stack);
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
		FruitRarity rarity = rarity();
		if (rarity != null) {
			tooltip.add(Component.translatable(rarity.translationKey()).withStyle(rarity.formatting()));
		}
		Power power = Powers.get(powerId);
		if (power != null) {
			String[] keys = {"Z", "X", "C", "V", "F"};
			for (int i = 0; i < power.abilities().size(); i++) {
				AbilityDef ability = power.abilities().get(i);
				tooltip.add(Component.literal("[" + keys[i] + "] ").withStyle(ChatFormatting.GOLD)
						.append(Component.translatable(ability.translationKey()).withStyle(ChatFormatting.WHITE))
						.append(Component.literal("  ★" + ability.requiredMastery()).withStyle(ChatFormatting.YELLOW)));
			}
		}
		tooltip.add(Component.translatable("item.grandline.fruit.hint").withStyle(ChatFormatting.DARK_GRAY));
	}

	@Nullable
	public FruitRarity rarity() {
		Fruits.FruitDef def = Fruits.get(powerId.toString());
		return def == null ? null : def.rarity();
	}
}
