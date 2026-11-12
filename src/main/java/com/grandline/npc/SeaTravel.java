package com.grandline.npc;

import com.grandline.player.PlayerProfileService;
import com.grandline.world.IslandBuilder;
import com.grandline.world.SeaLayout;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.levelgen.Heightmap;

/** Ship travel between the three seas, gated by level. */
public final class SeaTravel {
	private SeaTravel() {
	}

	/** Returns a result key: sailed, level_too_low or already_there. */
	public static String sail(ServerPlayer player, int sea) {
		if (sea < 1 || sea > SeaLayout.SEA_CENTER_X.length) {
			return "too_far";
		}
		if (PlayerProfileService.get(player).level() < SeaLayout.SEA_MIN_LEVEL[sea - 1]) {
			return "level_too_low";
		}
		if (SeaLayout.seaAt(player.getX()) == sea) {
			return "already_there";
		}
		SeaLayout.Spec harbor = SeaLayout.starter(sea);
		ServerLevel level = player.server.overworld();
		int x = harbor.x() + 3;
		int z = harbor.z() + 3;
		level.getChunk(x >> 4, z >> 4); // generate the destination first
		int y = Math.max(level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), IslandBuilder.WATER_TOP + 1);
		player.teleportTo(level, x + 0.5, y, z + 0.5, player.getYRot(), player.getXRot());
		player.sendSystemMessage(Component.translatable("message.grandline.sailed", sea, harbor.name()).withStyle(ChatFormatting.AQUA));
		level.playSound(null, BlockPos.containing(x, y, z), SoundEvents.BELL_BLOCK, SoundSource.PLAYERS, 1.0F, 0.8F);
		return "sailed";
	}
}
