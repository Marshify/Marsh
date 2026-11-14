package com.grandline.race;

import com.grandline.GrandLineMod;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The awakening meter (V4 only): fills from damage dealt and kills, capped per second so it can't be farmed with
 * many weak hits, saved with the player, and emptied when they awaken. It never fills while awakened.
 */
public final class RaceAwakening {
	/** Energy gained in the current second, per player (second index, amount). */
	private static final Map<UUID, long[]> THIS_SECOND = new HashMap<>();

	private RaceAwakening() {
	}

	public static void register() {
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (entity instanceof ServerPlayer) {
				return;
			}
			ServerPlayer killer = com.grandline.combat.CombatCredit.recentAttacker(entity);
			if (killer != null) {
				gain(killer, GrandLineMod.config().awakeningEnergyPerKill);
			}
		});
	}

	static void onDealt(ServerPlayer attacker, float dealt) {
		gain(attacker, dealt * GrandLineMod.config().awakeningEnergyPerDamage);
	}

	/** Adds awakening energy (V4 players, not while awakened), within the per-second cap. Returns what was added. */
	public static int gain(ServerPlayer player, double amount) {
		RaceData data = RaceService.get(player);
		if (data.stage() < 4 || RaceService.v4Active(player) || data.energy() >= 100 || amount <= 0) {
			return 0;
		}
		amount *= 1 + RaceUpgrades.bonus(data, "gain");
		long second = player.serverLevel().getGameTime() / 20;
		long[] slot = THIS_SECOND.computeIfAbsent(player.getUUID(), u -> new long[]{second, 0});
		if (slot[0] != second) {
			slot[0] = second;
			slot[1] = 0;
		}
		int room = (int) Math.max(0, GrandLineMod.config().awakeningEnergyPerSecondCap - slot[1]);
		int add = (int) Math.min(room, Math.max(1, Math.round(amount)));
		add = Math.min(add, 100 - data.energy());
		if (add <= 0) {
			return 0;
		}
		slot[1] += add;
		int added = add;
		RaceService.update(player, d -> d.withEnergy(d.energy() + added));
		return added;
	}

	static void forget(UUID id) {
		THIS_SECOND.remove(id);
	}
}
