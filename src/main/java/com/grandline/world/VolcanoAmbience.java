package com.grandline.world;

import com.grandline.network.payload.FxEventPayload;
import com.grandline.world.plan.IslandPlan;
import com.grandline.world.plan.IslandPlans;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Emberfall's volcano breathes: every two seconds, players within 220 blocks of the rebuilt island get a light visual
 * beat at the summit, which their client turns into drifting smoke rings, a plume and embers (nothing is placed in the
 * world, nothing collides). Distance-limited and cheap.
 */
public final class VolcanoAmbience {
	static final String ISLAND = "Emberfall Isle";
	static final int VX = 0, VZ = -18;
	private static int beat;

	private VolcanoAmbience() {
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % 40 != 0) {
				return;
			}
			var level = server.overworld();
			if (level.players().isEmpty()) {
				return;
			}
			Island island = null;
			for (Island i : IslandRegistry.get(server).all()) {
				if (i.name().equals(ISLAND)) {
					island = i;
					break;
				}
			}
			if (island == null) {
				return;
			}
			IslandPlan plan = IslandPlans.get(island);
			if (plan == null || !plan.getClass().getSimpleName().equals("EmberfallIsle")) {
				return; // older worlds keep the original island
			}
			int top = plan.terrain().ground(VX, VZ);
			Vec3 summit = new Vec3(island.x() + VX + 0.5, top + 2, island.z() + VZ + 0.5);
			beat++;
			FxEventPayload payload = new FxEventPayload("volcano_smoke", -1, summit, Vec3.ZERO, 0, 0, beat % 3);
			for (ServerPlayer p : level.players()) {
				if (p.position().distanceToSqr(summit) < 220 * 220 && ServerPlayNetworking.canSend(p, FxEventPayload.TYPE)) {
					ServerPlayNetworking.send(p, payload);
				}
			}
		});
	}
}
