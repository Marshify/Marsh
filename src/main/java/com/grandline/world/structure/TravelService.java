package com.grandline.world.structure;

import com.grandline.GrandLineMod;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.CommandBlockEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Plate travel between structures.
 *
 * <p>Normal path: a stone pressure plate sits on an impulse command block (Needs Redstone) whose command is
 * {@code grandline travel @a[dx=0,dy=1,dz=0] <arrival>}. The selector box starts at the command block and covers the
 * block above it, so only a player standing on that plate is sent. The command loads the destination, finds a solid
 * floor with two blocks of headroom near the arrival marker and teleports there.
 *
 * <p>Fallback: when the server has command blocks disabled ({@code enable-command-block=false}), this service watches
 * the same plates and runs the same travel for the player on the plate. It never acts while command blocks are enabled,
 * so exactly one path fires. A short per-player cooldown stops arrival plates from bouncing players back.
 */
public final class TravelService {
	public static final String COMMAND_PREFIX = "grandline travel";
	private static final int COOLDOWN_TICKS = 40;
	private static final Map<UUID, Integer> LAST_TRAVEL = new HashMap<>();

	private TravelService() {
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (commandBlocksEnabled(server) || server.getTickCount() % 2 != 0) {
				return;
			}
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				fallback(player);
			}
		});
	}

	/** Test hook: when non-null, overrides the server's enable-command-block setting. */
	static Boolean commandBlocksOverride;

	static boolean commandBlocksEnabled(MinecraftServer server) {
		return commandBlocksOverride != null ? commandBlocksOverride : server.isCommandBlockEnabled();
	}

	/** Fallback for worlds with command blocks disabled: the plate under the player reads the block's command. */
	static void fallback(ServerPlayer player) {
		if (commandBlocksEnabled(player.server)) {
			return; // the command block handles it; never fire both paths
		}
		BlockPos feet = player.blockPosition();
		if (!player.serverLevel().getBlockState(feet).is(Blocks.STONE_PRESSURE_PLATE)) {
			return;
		}
		if (!(player.serverLevel().getBlockEntity(feet.below()) instanceof CommandBlockEntity commandBlock)) {
			return;
		}
		String command = commandBlock.getCommandBlock().getCommand().trim();
		if (command.startsWith("/")) {
			command = command.substring(1);
		}
		if (!command.startsWith(COMMAND_PREFIX)) {
			return;
		}
		String[] parts = command.split("\\s+");
		travel(player, parts[parts.length - 1]);
	}

	/** Sends a player to an arrival. Returns false (and does nothing) if unknown, on cooldown, or no safe floor. */
	public static boolean travel(ServerPlayer player, String arrival) {
		MinecraftServer server = player.server;
		Integer last = LAST_TRAVEL.get(player.getUUID());
		if (last != null && server.getTickCount() - last < COOLDOWN_TICKS) {
			return false;
		}
		BlockPos target = StructurePlacements.arrival(server, arrival);
		if (target == null) {
			GrandLineMod.LOGGER.warn("Travel to unknown arrival '{}'", arrival);
			return false;
		}
		ServerLevel level = server.overworld();
		level.getChunk(target.getX() >> 4, target.getZ() >> 4); // load (or generate) the destination first
		BlockPos safe = safeSpot(level, target);
		if (safe == null) {
			GrandLineMod.LOGGER.warn("No safe landing near arrival '{}' at {}", arrival, target);
			return false;
		}
		LAST_TRAVEL.put(player.getUUID(), server.getTickCount());
		player.teleportTo(level, safe.getX() + 0.5, safe.getY(), safe.getZ() + 0.5, player.getYRot(), player.getXRot());
		player.fallDistance = 0;
		level.playSound(null, safe, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8F, 1.2F);
		return true;
	}

	/** The marker spot if it has a solid floor and two free blocks above, else the nearest such spot a few blocks up/down. */
	static BlockPos safeSpot(ServerLevel level, BlockPos target) {
		for (int dy = 0; dy <= 4; dy++) {
			for (int sign : new int[]{1, -1}) {
				BlockPos p = target.above(dy * sign);
				if (level.getBlockState(p.below()).isFaceSturdy(level, p.below(), net.minecraft.core.Direction.UP)
						&& level.getBlockState(p).getCollisionShape(level, p).isEmpty() && level.getFluidState(p).isEmpty()
						&& level.getBlockState(p.above()).getCollisionShape(level, p.above()).isEmpty()) {
					return p;
				}
			}
		}
		return null;
	}

	/** Test hook: forget cooldowns. */
	public static void resetCooldowns() {
		LAST_TRAVEL.clear();
	}
}
