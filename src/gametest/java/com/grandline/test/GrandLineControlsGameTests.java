package com.grandline.test;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;

/** No player-made crouching in Grand Line play (the server side of it). */
public final class GrandLineControlsGameTests implements FabricGameTest {
	@GameTest(template = EMPTY_STRUCTURE, batch = "controls")
	public void survivalPlayersCannotStayCrouched(GameTestHelper helper) {
		ServerPlayer onFoot = helper.makeMockServerPlayerInLevel();
		ServerPlayer creative = helper.makeMockServerPlayerInLevel();
		onFoot.setGameMode(GameType.ADVENTURE);
		creative.setGameMode(GameType.CREATIVE);
		onFoot.setShiftKeyDown(true);
		creative.setShiftKeyDown(true);
		helper.runAfterDelay(2, () -> {
			helper.assertTrue(!onFoot.isShiftKeyDown() && !onFoot.isCrouching(), "an adventure player on foot is not left crouching");
			helper.assertTrue(creative.isShiftKeyDown(), "creative keeps it (flying down)");
			helper.getLevel().removePlayerImmediately(onFoot, Entity.RemovalReason.DISCARDED);
			helper.getLevel().removePlayerImmediately(creative, Entity.RemovalReason.DISCARDED);
			helper.succeed();
		});
	}
}
