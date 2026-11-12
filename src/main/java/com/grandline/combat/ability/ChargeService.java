package com.grandline.combat.ability;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Hold-to-charge moves. The charge time is measured on the server between the activation request and the release
 * request (or the automatic release at {@link #MAX_CHARGE_TICKS}). The client never reports how long it held the key.
 */
public final class ChargeService {
	/** Holding longer than this releases automatically at full charge. */
	public static final int MAX_CHARGE_TICKS = 50;
	/** Releases shorter than this count as a quick (tap) cast. */
	public static final int TAP_TICKS = 6;

	/** Called every tick while charging (e.g. to draw a growing fireball). */
	@FunctionalInterface
	public interface ChargeVisual {
		void draw(ServerPlayer player, double charge, int ticks);
	}

	public record Charging(Power power, AbilityDef ability, int slot, int startTick, double multiplier, ChargeVisual visual) {
		public double charge(int nowTick) {
			int held = nowTick - startTick;
			return held < TAP_TICKS ? 0.0 : Math.min(1.0, (held - TAP_TICKS) / (double) (MAX_CHARGE_TICKS - TAP_TICKS));
		}
	}

	private static final Map<UUID, Charging> CHARGING = new HashMap<>();
	private static final Map<AbilityDef, ChargeVisual> VISUALS = new HashMap<>();

	private ChargeService() {
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(ChargeService::tick);
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> CHARGING.remove(handler.getPlayer().getUUID()));
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> CHARGING.clear());
	}

	/** Registers the charging visual for a charged move. */
	public static void visual(AbilityDef ability, ChargeVisual visual) {
		VISUALS.put(ability, visual);
	}

	public static boolean isCharging(ServerPlayer player) {
		return CHARGING.containsKey(player.getUUID());
	}

	public static void begin(ServerPlayer player, Power power, AbilityDef ability, int slot, double multiplier) {
		CHARGING.put(player.getUUID(), new Charging(power, ability, slot, player.server.getTickCount(), multiplier,
				VISUALS.getOrDefault(ability, (p, c, t) -> { })));
	}

	/** Ends the charge and returns it (or null if the player was not charging that slot). */
	public static Charging end(ServerPlayer player, int slot) {
		Charging charging = CHARGING.get(player.getUUID());
		if (charging == null || charging.slot() != slot) {
			return null;
		}
		CHARGING.remove(player.getUUID());
		return charging;
	}

	private static void tick(MinecraftServer server) {
		if (CHARGING.isEmpty()) {
			return;
		}
		int now = server.getTickCount();
		for (Map.Entry<UUID, Charging> entry : Map.copyOf(CHARGING).entrySet()) {
			ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
			Charging charging = entry.getValue();
			if (player == null || !player.isAlive()) {
				CHARGING.remove(entry.getKey());
				continue;
			}
			int held = now - charging.startTick();
			charging.visual().draw(player, charging.charge(now), held);
			// Charging slows the caster down, like winding up a big attack.
			player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 5, 1, false, false));
			if (held >= MAX_CHARGE_TICKS) {
				AbilityActivationHandler.releaseCharge(player, charging.slot());
			}
		}
	}
}
