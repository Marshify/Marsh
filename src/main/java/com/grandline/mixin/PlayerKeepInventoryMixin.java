package com.grandline.mixin;

import com.grandline.GrandLineMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Players keep everything when they die (whatever the keepInventory gamerule says): nothing drops, and
 * {@link com.grandline.player.rules.KeepInventory} hands the same stacks to the respawned player, so items can neither
 * be lost nor duplicated.
 */
@Mixin(Player.class)
public abstract class PlayerKeepInventoryMixin {
	@Inject(method = "dropEquipment", at = @At("HEAD"), cancellable = true)
	private void grandline$keepEverything(CallbackInfo ci) {
		if ((Object) this instanceof ServerPlayer && GrandLineMod.config().keepInventoryOnDeath) {
			ci.cancel();
		}
	}
}
