package com.grandline.mixin;

import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Lets tests end a new player's spawn protection (they are never ticked long enough for it to run out). */
@Mixin(ServerPlayer.class)
public interface ServerPlayerSpawnAccessor {
	@Accessor("spawnInvulnerableTime")
	void grandline$setSpawnInvulnerableTime(int ticks);
}
