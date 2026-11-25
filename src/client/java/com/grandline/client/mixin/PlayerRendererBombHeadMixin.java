package com.grandline.client.mixin;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Self Destruct: the real head (and hat layer) is hidden only while the bomb head replaces it (rendering only). */
@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererBombHeadMixin {
	@Inject(method = "setModelProperties", at = @At("TAIL"))
	private void grandline$bombHead(AbstractClientPlayer player, CallbackInfo ci) {
		if (com.grandline.client.fx.BombFx.headActive(player.getId())) {
			var model = ((PlayerRenderer) (Object) this).getModel();
			model.head.visible = false;
			model.hat.visible = false;
		}
	}
}
