package com.grandline.client;

import com.grandline.client.hud.ProfileHud;
import com.grandline.client.network.ClientNetworking;
import net.fabricmc.api.ClientModInitializer;

/**
 * Client-only entrypoint. Lives in the {@code client} source set, so the common/server code cannot accidentally
 * reference client classes (which do not exist on a dedicated server).
 */
public final class GrandLineClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		com.grandline.client.fx.GrandLineFx.register();
		com.grandline.client.render.sword.HeldSwords.register();
		com.grandline.client.technique.ClientTechniques.register();
		com.grandline.client.technique.TechniqueKeys.register();
		com.grandline.client.technique.ClientDash.register();
		com.grandline.client.technique.InstinctSense.register();
		com.grandline.client.hud.TechniqueHud.register();
		net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback.EVENT.register((type, renderer, helper, ctx) -> {
			if (renderer instanceof net.minecraft.client.renderer.entity.player.PlayerRenderer player) {
				helper.register(new com.grandline.client.render.aura.AuraLayer(player));
			}
		});
		ClientNetworking.register();
		GrandLineClientCommands.register();
		GrandLineKeys.register();
		ProfileHud.register();
		com.grandline.client.hud.IslandBanner.register();
		com.grandline.client.hud.QuestUnlockBanner.register();
		ClientFishing.register();
		ClientEquipment.register();
		ClientNames.register();
		ClientNavigation.register();
		com.grandline.client.camera.ShiftLockKeys.register();
		com.grandline.client.race.ClientRace.register();
		com.grandline.client.hud.QuestCompass.register();
		com.grandline.client.render.GrandLineRenderers.register();
		com.grandline.client.screen.FruitDialogs.register();
	}
}
