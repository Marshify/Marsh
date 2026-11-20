package com.grandline.client.technique;

import com.grandline.network.payload.DashMovePayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

/**
 * Carries out an approved Dash on this client: each tick, before the player moves, its motion is set to an even share
 * of the distance (normal collision stops it at walls), with any fall held to a gentle drift; at the end only a little
 * momentum is left.
 */
public final class ClientDash {
	private static Vec3 step = Vec3.ZERO;
	private static int ticksLeft;

	private ClientDash() {
	}

	public static void register() {
		ClientPlayNetworking.registerGlobalReceiver(DashMovePayload.TYPE, (packet, player, sender) -> {
			Vec3 dir = new Vec3(packet.dirX(), 0, packet.dirZ());
			step = dir.lengthSqr() < 1e-6 ? Vec3.ZERO : dir.normalize().scale(packet.distance() / Math.max(1, packet.ticks()));
			ticksLeft = packet.ticks();
		});
		ClientTickEvents.START_CLIENT_TICK.register(ClientDash::tick);
	}

	public static boolean dashing() {
		return ticksLeft > 0;
	}

	private static void tick(Minecraft mc) {
		if (mc.player == null || mc.isPaused()) {
			return;
		}
		var p = mc.player;
		if (ticksLeft <= 0) {
			if (endMomentum) {
				// The dash is over: keep only a little of its speed.
				endMomentum = false;
				Vec3 v = p.getDeltaMovement();
				p.setDeltaMovement(step.x * 0.05, v.y, step.z * 0.05);
			}
			return;
		}
		double vy = Math.max(Math.min(p.getDeltaMovement().y, 0.1) * 0.2, -0.04);
		p.setDeltaMovement(step.x, vy, step.z);
		p.fallDistance = 0;
		ticksLeft--;
		endMomentum = ticksLeft == 0;
	}

	private static boolean endMomentum;
}
