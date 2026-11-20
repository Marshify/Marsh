package com.grandline.client.render.sword;

import com.grandline.GrandLineMod;
import com.grandline.gear.Gear;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Multi-blade swords in the hand. The Dual Katana's icon shows both blades crossed; in the hands each hand holds one
 * blade. The Triple Katana holds one blade in each hand and the third crosswise in the mouth (third person). The
 * blades are cut from the item icons (textures/item/*_blade.png, *_mouth.png).
 */
public final class HeldSwords {
	public static final ResourceLocation DUAL_HELD = GrandLineMod.id("item/dual_katana_held");
	public static final ResourceLocation TRIPLE_HELD = GrandLineMod.id("item/triple_katana_held");
	public static final ResourceLocation TRIPLE_MOUTH = GrandLineMod.id("item/triple_katana_mouth");

	private HeldSwords() {
	}

	public static void register() {
		ModelLoadingPlugin.register(ctx -> ctx.addModels(DUAL_HELD, TRIPLE_HELD, TRIPLE_MOUTH));
	}

	public static boolean multiBlade(ItemStack stack) {
		return stack.is(Gear.DUAL_KATANA) || stack.is(Gear.TRIPLE_KATANA);
	}

	static boolean inHand(ItemDisplayContext context) {
		return context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND || context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
				|| context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND || context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
	}

	/** The model to draw {@code stack} with in {@code context} (the single blade in hand, the mouth blade on the head). */
	public static BakedModel model(ItemStack stack, ItemDisplayContext context, BakedModel original) {
		if (!multiBlade(stack)) {
			return original;
		}
		var manager = Minecraft.getInstance().getModelManager();
		if (inHand(context)) {
			BakedModel m = manager.getModel(stack.is(Gear.DUAL_KATANA) ? DUAL_HELD : TRIPLE_HELD);
			return m == null ? original : m;
		}
		if (context == ItemDisplayContext.HEAD && stack.is(Gear.TRIPLE_KATANA)) {
			BakedModel m = manager.getModel(TRIPLE_MOUTH);
			return m == null ? original : m;
		}
		return original;
	}

	/** What the empty off hand holds: the second blade of a multi-blade sword in the main hand. */
	public static ItemStack offHand(LivingEntity entity, ItemStack offStack, HumanoidArm arm) {
		if (!offStack.isEmpty() || arm == entity.getMainArm()) {
			return offStack;
		}
		ItemStack main = entity.getMainHandItem();
		return multiBlade(main) ? main : offStack;
	}
}
