package com.grandline.client.mixin;

import com.grandline.GrandLineMod;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hides the vanilla experience bar and level number (Grand Line has its own level bar in the status panel). */
@Mixin(Gui.class)
public abstract class GuiExperienceBarMixin {
	@Inject(method = "renderExperienceBar", at = @At("HEAD"), cancellable = true)
	private void grandline$hideXpBar(GuiGraphics graphics, int x, CallbackInfo ci) {
		if (GrandLineMod.config().disableVanillaExperience) {
			ci.cancel();
		}
	}
}
