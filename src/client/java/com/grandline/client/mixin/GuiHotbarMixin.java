package com.grandline.client.mixin;

import com.grandline.client.hud.LoadoutHotbar;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Replaces the vanilla hotbar drawing with the Grand Line loadout hotbar (same slots, keys and scrolling). */
@Mixin(Gui.class)
public abstract class GuiHotbarMixin {
	@Inject(method = "renderHotbar(FLnet/minecraft/client/gui/GuiGraphics;)V", at = @At("HEAD"), cancellable = true)
	private void grandline$hotbar(float partialTick, GuiGraphics graphics, CallbackInfo ci) {
		if (LoadoutHotbar.replacesVanilla()) {
			LoadoutHotbar.render(graphics, partialTick);
			ci.cancel();
		}
	}
}
