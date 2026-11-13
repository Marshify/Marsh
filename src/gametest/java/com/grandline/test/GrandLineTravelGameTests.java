package com.grandline.test;

import com.grandline.world.structure.StructurePlacements;
import com.grandline.world.structure.TravelService;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Plate travel: only the player standing on the plate moves; a bystander next to it stays. Covers the command block
 * command (selector scoped to the plate) and the server fallback used when command blocks are disabled.
 */
public final class GrandLineTravelGameTests implements FabricGameTest {
	private record Setup(BlockPos plate, BlockPos commandBlock, BlockPos landing, ServerPlayer rider, ServerPlayer bystander) {
	}

	private static Setup setup(GameTestHelper helper, String arrival) {
		helper.getLevel().getServer().getPlayerList().getPlayers(); // (mock players are not in the list)
		BlockPos cmd = helper.absolutePos(new BlockPos(2, 1, 2));
		BlockPos plate = cmd.above();
		helper.getLevel().setBlockAndUpdate(cmd, Blocks.COMMAND_BLOCK.defaultBlockState());
		if (helper.getLevel().getBlockEntity(cmd) instanceof CommandBlockEntity be) {
			be.getCommandBlock().setCommand(TravelService.COMMAND_PREFIX + " @a[dx=0,dy=1,dz=0] " + arrival);
		}
		helper.getLevel().setBlockAndUpdate(plate, Blocks.STONE_PRESSURE_PLATE.defaultBlockState());
		// Landing: a solid floor 6 blocks away with headroom.
		BlockPos landing = helper.absolutePos(new BlockPos(8, 2, 2));
		helper.getLevel().setBlockAndUpdate(landing.below(), Blocks.STONE.defaultBlockState());
		StructurePlacements.registerArrival(arrival, landing);
		TravelService.resetCooldowns();
		ServerPlayer rider = helper.makeMockServerPlayerInLevel();
		ServerPlayer bystander = helper.makeMockServerPlayerInLevel();
		rider.teleportTo(plate.getX() + 0.5, plate.getY(), plate.getZ() + 0.5);
		bystander.teleportTo(plate.getX() + 1.5, plate.getY(), plate.getZ() + 0.5); // on the next block, touching the plate's edge
		return new Setup(plate, cmd, landing, rider, bystander);
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "travel_1")
	public void commandBlockSendsOnlyThePlayerOnThePlate(GameTestHelper helper) {
		Setup s = setup(helper, "test_arrival_cmd");
		Vec3 bystanderBefore = s.bystander().position();
		// Run the exact command the command block holds, from the command block's position (as vanilla does).
		var server = helper.getLevel().getServer();
		CommandSourceStack source = server.createCommandSourceStack().withPosition(Vec3.atLowerCornerOf(s.commandBlock()))
				.withLevel(helper.getLevel()).withPermission(2).withSuppressedOutput();
		String command = ((CommandBlockEntity) helper.getLevel().getBlockEntity(s.commandBlock())).getCommandBlock().getCommand();
		server.getCommands().performPrefixedCommand(source, command);
		helper.assertTrue(s.rider().blockPosition().equals(s.landing()), "rider landed at " + s.landing() + " but is at " + s.rider().blockPosition());
		helper.assertTrue(s.bystander().position().equals(bystanderBefore), "bystander stayed put");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "travel_2")
	public void fallbackPlateSendsOnlyThePlayerOnThePlate(GameTestHelper helper) {
		Setup s = setup(helper, "test_arrival_fallback");
		Vec3 bystanderBefore = s.bystander().position();
		// 1) Command blocks enabled: the fallback must do nothing (only the command block may fire).
		com.grandline.world.structure.TravelServiceTestAccess.commandBlocks(true);
		com.grandline.world.structure.TravelServiceTestAccess.fallback(s.rider());
		helper.assertTrue(s.rider().blockPosition().equals(s.plate()), "fallback stays idle while command blocks are enabled");
		// 2) Command blocks disabled: the same plate sends the rider, and only the rider.
		com.grandline.world.structure.TravelServiceTestAccess.commandBlocks(false);
		helper.runAfterDelay(4, () -> {
			// Mock players are not in the player list the tick handler walks, so drive the same per-player check directly.
			com.grandline.world.structure.TravelServiceTestAccess.fallback(s.rider());
			com.grandline.world.structure.TravelServiceTestAccess.fallback(s.bystander());
			helper.assertTrue(s.rider().blockPosition().equals(s.landing()), "rider landed at " + s.landing() + " but is at " + s.rider().blockPosition());
			helper.assertTrue(s.bystander().position().equals(bystanderBefore), "bystander stayed put");
			com.grandline.world.structure.TravelServiceTestAccess.commandBlocks(null);
			helper.succeed();
		});
	}
}
