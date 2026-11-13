package com.grandline.combat.fx;

import com.grandline.network.payload.FxEventPayload;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/** Sends move visual beats to every player close enough to see them (the caster included). */
public final class FxEvents {
	/** Players farther than this from the beat do not get it. */
	public static final double VIEW_DISTANCE = 96;

	private FxEvents() {
	}

	public static void send(ServerPlayer caster, String effect, Vec3 pos, Vec3 dir, double speed, double range) {
		send(caster, effect, pos, dir, speed, range, 0);
	}

	public static void send(ServerPlayer caster, String effect, Vec3 pos, Vec3 dir, double speed, double range, double size) {
		FxEventPayload payload = new FxEventPayload(effect, caster.getId(), pos, dir, (float) speed, (float) range, (float) size);
		ServerLevel level = caster.serverLevel();
		for (ServerPlayer viewer : PlayerLookup.around(level, pos, VIEW_DISTANCE)) {
			if (ServerPlayNetworking.canSend(viewer, FxEventPayload.TYPE)) {
				ServerPlayNetworking.send(viewer, payload);
			}
		}
	}

	public static void send(ServerPlayer caster, String effect, Vec3 pos) {
		send(caster, effect, pos, Vec3.ZERO, 0, 0, 0);
	}

	public static void send(ServerPlayer caster, String effect, Vec3 pos, double size) {
		send(caster, effect, pos, Vec3.ZERO, 0, 0, size);
	}
}
