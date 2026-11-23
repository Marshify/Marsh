package com.grandline.test;

import com.grandline.ship.ShipEntities;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Stepping off a ship lands on the nearest dock or landing beside the hull. */
public final class GrandLineDisembarkGameTests implements FabricGameTest {
	@GameTest(template = EMPTY_STRUCTURE, batch = "disembark", timeoutTicks = 60)
	public void steppingOffLandsOnTheLanding(GameTestHelper helper) {
		// Water 2 deep, a plank landing (top one block above the water) along x = 1..2.
		for (int x = 0; x <= 9; x++) {
			for (int z = 0; z <= 6; z++) {
				helper.setBlock(new BlockPos(x, 1, z), Blocks.WATER);
				helper.setBlock(new BlockPos(x, 2, z), Blocks.WATER);
				helper.setBlock(new BlockPos(x, 3, z), x <= 2 ? Blocks.SPRUCE_PLANKS : Blocks.AIR);
			}
		}
		var ship = ShipEntities.SLOOP.create(helper.getLevel());
		Vec3 at = helper.absoluteVec(new Vec3(5.0, 3.0, 3.5));
		ship.moveTo(at.x, at.y, at.z, 0, 0);
		helper.getLevel().addFreshEntity(ship);
		ServerPlayer player = GrandLineBatch3GameTests.survivor(helper);
		Vec3 direct = ship.getDismountLocationForPassenger(player);
		Vec3 landing = helper.absoluteVec(new Vec3(1.5, 4.0, 3.5));
		helper.assertTrue(direct.y >= landing.y - 0.01 && direct.x < helper.absoluteVec(new Vec3(3.0, 0, 0)).x,
				"the step-off spot is on the landing: " + helper.relativeVec(direct));
		player.startRiding(ship, true);
		helper.assertTrue(player.getVehicle() == ship, "riding");
		player.stopRiding();
		Vec3 after = helper.relativeVec(player.position());
		helper.assertTrue(after.x < 3.0 && after.y >= 3.99, "a rider stepping off lands on the landing: " + after);
		ship.discard();
		GrandLineBatch3GameTests.remove(helper, player);
		helper.succeed();
	}
}
