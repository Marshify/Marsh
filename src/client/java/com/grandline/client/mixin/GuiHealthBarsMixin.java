package com.grandline.client.mixin;

import com.grandline.GrandLineMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hides the vanilla hearts, hunger and armour rows (Grand Line's status panel shows Health, and hunger is always full);
 * only the air bubbles are kept, drawn where vanilla puts them, while the player is under water or short of air.
 */
@Mixin(Gui.class)
public abstract class GuiHealthBarsMixin {
	private static final ResourceLocation GRANDLINE$ICONS = new ResourceLocation("textures/gui/icons.png");

	@Inject(method = "renderPlayerHealth", at = @At("HEAD"), cancellable = true)
	private void grandline$hideHealthAndHunger(GuiGraphics graphics, CallbackInfo ci) {
		if (!GrandLineMod.config().hideVanillaHealthBars) {
			return;
		}
		ci.cancel();
		Player player = Minecraft.getInstance().player;
		if (player == null) {
			return;
		}
		int maxAir = player.getMaxAirSupply();
		int air = Math.min(player.getAirSupply(), maxAir);
		if (!player.isEyeInFluid(FluidTags.WATER) && air >= maxAir) {
			return;
		}
		int right = graphics.guiWidth() / 2 + 91, y = graphics.guiHeight() - 49;
		int full = Mth.ceil((air - 2) * 10.0 / maxAir);
		int popping = Mth.ceil(air * 10.0 / maxAir) - full;
		for (int i = 0; i < full + popping; i++) {
			graphics.blit(GRANDLINE$ICONS, right - i * 8 - 9, y, i < full ? 16 : 25, 18, 9, 9);
		}
	}
}
