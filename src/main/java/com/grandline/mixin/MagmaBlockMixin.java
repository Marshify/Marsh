package com.grandline.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.MagmaBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Magma blocks are scenery: standing on one does not hurt a player. */
@Mixin(MagmaBlock.class)
public abstract class MagmaBlockMixin {
	@Inject(method = "stepOn", at = @At("HEAD"), cancellable = true)
	private void grandline$noHotFloor(Level level, BlockPos pos, BlockState state, Entity entity, CallbackInfo ci) {
		if (entity instanceof Player) {
			ci.cancel();
		}
	}
}
