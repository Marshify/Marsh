package com.grandline.client.technique;

import com.grandline.network.payload.TechniqueLookPayload;
import com.grandline.network.payload.TechniqueSyncPayload;
import com.grandline.technique.TechniqueRules;
import com.grandline.technique.Techniques;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

import java.util.HashMap;
import java.util.Map;

/**
 * What this client knows about techniques: its own (from the server's sync: ownership, XP, charges, toggles, cooldowns)
 * and how other players look (their Aura stage). Cooldowns and charge regeneration count down locally between syncs so
 * the HUD moves smoothly; the server's numbers replace them on every sync.
 */
public final class ClientTechniques {
	private static Techniques own = Techniques.NONE;
	private static boolean auraOn;
	private static boolean instinctOn;
	private static int dashCooldown;
	private static int flashCooldown;
	private static int regenTicks;
	private static final Map<Integer, Integer> AURA_STAGES = new HashMap<>();

	private ClientTechniques() {
	}

	public static void register() {
		ClientPlayNetworking.registerGlobalReceiver(TechniqueSyncPayload.TYPE, (packet, player, sender) -> {
			own = packet.techniques();
			auraOn = packet.auraOn();
			boolean wasOn = instinctOn;
			instinctOn = packet.instinctOn();
			dashCooldown = packet.dashCooldown();
			flashCooldown = packet.flashCooldown();
			regenTicks = own.regenTicks();
			if (wasOn && !instinctOn) {
				InstinctSense.clear();
			}
		});
		ClientPlayNetworking.registerGlobalReceiver(TechniqueLookPayload.TYPE, (packet, player, sender) -> {
			if (packet.auraStage() < 0) {
				AURA_STAGES.remove(packet.entityId());
			} else {
				AURA_STAGES.put(packet.entityId(), packet.auraStage());
			}
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			own = Techniques.NONE;
			auraOn = false;
			instinctOn = false;
			AURA_STAGES.clear();
			InstinctSense.clear();
		});
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.isPaused()) {
				return;
			}
			if (dashCooldown > 0) {
				dashCooldown--;
			}
			if (flashCooldown > 0) {
				flashCooldown--;
			}
			if (own.instinct() && own.dodges() < maxDodges()) {
				regenTicks = Math.min(TechniqueRules.regenTicks(), regenTicks + 1);
			}
		});
	}

	public static Techniques own() {
		return own;
	}

	public static boolean auraOn() {
		return auraOn && own.aura();
	}

	public static boolean instinctOn() {
		return instinctOn && own.instinct();
	}

	public static int auraStage() {
		return TechniqueRules.auraStage(own.auraXp());
	}

	public static int maxDodges() {
		return TechniqueRules.maxDodges(own.instinctXp());
	}

	/** 0..1 toward the next dodge charge. */
	public static float regenProgress() {
		return own.dodges() >= maxDodges() ? 1f : regenTicks / (float) TechniqueRules.regenTicks();
	}

	public static int dashCooldown() {
		return dashCooldown;
	}

	public static int flashCooldown() {
		return flashCooldown;
	}

	/** The Aura coating stage a player shows (-1: none). */
	public static int auraStageOf(Entity entity) {
		Minecraft mc = Minecraft.getInstance();
		if (entity == mc.player) {
			return auraOn() ? auraStage() : AURA_STAGES.getOrDefault(entity.getId(), -1);
		}
		return AURA_STAGES.getOrDefault(entity.getId(), -1);
	}

	/** Test/preview hook: sets how an entity looks. */
	public static void setLook(int entityId, int stage) {
		if (stage < 0) {
			AURA_STAGES.remove(entityId);
		} else {
			AURA_STAGES.put(entityId, stage);
		}
	}
}
