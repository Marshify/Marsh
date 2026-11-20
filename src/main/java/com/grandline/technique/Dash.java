package com.grandline.technique;

import com.grandline.combat.EnergyService;
import com.grandline.combat.fx.FxEvents;
import com.grandline.gear.sword.SwordKit;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Directional Dash (everyone, from level 1): a quick burst of movement in the direction the player is pressing (W, A,
 * S, D and diagonals, forward when nothing is held), on the ground or in the air. The server checks and pays for it
 * (cooldown, stun, energy) and tells the player's client to move: over a few ticks, stopping at walls, with the fall
 * held while it lasts. No damage.
 */
public final class Dash {
	private Dash() {
	}

	/**
	 * The world direction for movement input {@code forward} (W = +1, S = -1) and {@code strafe} (A = +1, D = -1) at
	 * {@code yaw} degrees, flat and normalized (diagonals are no longer than straight dashes). Nothing held: forward.
	 */
	public static Vec3 direction(float yaw, float forward, float strafe) {
		float f = Mth.clamp(forward, -1, 1), s = Mth.clamp(strafe, -1, 1);
		if (Math.abs(f) < 0.01f && Math.abs(s) < 0.01f) {
			f = 1;
		}
		double len = Math.sqrt(f * f + s * s);
		double x = s / len, z = f / len;
		double sin = Math.sin(yaw * Mth.DEG_TO_RAD), cos = Math.cos(yaw * Mth.DEG_TO_RAD);
		return new Vec3(x * cos - z * sin, 0, z * cos + x * sin).normalize();
	}

	static String perform(ServerPlayer player, float forward, float strafe) {
		TechniqueService.Runtime r = TechniqueService.runtime(player);
		long now = TechniqueService.now(player);
		if (now < r.dashReadyAt) {
			return "cooldown";
		}
		if (TechniqueService.immobilized(player)) {
			return "immobilized";
		}
		var config = TechniqueService.config();
		if (!EnergyService.trySpend(player, config.dashEnergy / 100.0)) {
			TechniqueService.tell(player, "message.grandline.ability.no_energy");
			return "no_energy";
		}
		r.dashReadyAt = now + config.dashCooldownTicks;
		Vec3 dir = direction(player.getYRot(), forward, strafe);
		Vec3 start = player.position();
		Vec3 end = SwordKit.reach(player, start, dir, config.dashDistance);
		double distance = end.distanceTo(start);
		int ticks = config.dashTicks;
		boolean grounded = player.onGround();
		FxEvents.send(player, "tq:dash", start.add(0, 0.9, 0), dir, distance / ticks, distance, grounded ? 1 : 0);
		player.serverLevel().playSound(null, start.x, start.y, start.z, SoundEvents.PHANTOM_FLAP, net.minecraft.sounds.SoundSource.PLAYERS, 0.7f, 1.9f);
		player.serverLevel().playSound(null, start.x, start.y, start.z, SoundEvents.ELYTRA_FLYING, net.minecraft.sounds.SoundSource.PLAYERS, 0.18f, 2.0f);
		// The player's client carries the dash out (players move themselves in Minecraft); walls stop it there.
		if (net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.canSend(player, com.grandline.network.payload.DashMovePayload.TYPE)) {
			net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player,
					new com.grandline.network.payload.DashMovePayload((float) dir.x, (float) dir.z, (float) config.dashDistance, ticks));
		}
		player.fallDistance = 0;
		TechniqueTrainers.dashed(player, forward, strafe, !grounded);
		TechniqueService.sync(player);
		return "ok";
	}
}
