package com.grandline.combat.ability;

import com.grandline.player.PlayerProfile;
import com.grandline.player.PlayerProfileService;
import com.grandline.player.StatType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Fruit M1 combos (reusable by any fruit or fighting style): a registered power gets a dedicated multi-step basic attack
 * on the attack button. The server owns the combo: step order, pacing ({@link Combo#interval}), the reset after a pause,
 * and every hit (run by the combo's {@link Strike} inside the normal ability damage path, so Fruit-stat scaling, Aura,
 * Instinct, kill credit and mastery all apply). Costs no energy and has no cooldown beyond its pacing.
 */
public final class FruitStrikes {
	/** One strike of the combo; {@code step} is 0-based, {@code last} marks the finisher. */
	@FunctionalInterface
	public interface Strike {
		void strike(AbilityContext ctx, int step, boolean last);
	}

	/**
	 * @param ability  the move id used for kill credit and Instinct (not shown on a key)
	 * @param steps    strikes in the combo
	 * @param interval minimum ticks between strikes (10 keeps every hit inside vanilla's invulnerability rules)
	 * @param reset    a pause longer than this starts the combo over
	 */
	public record Combo(AbilityDef ability, int steps, int interval, int reset, Strike strike) {
	}

	private record State(int step, long lastTick) {
	}

	private static final Map<ResourceLocation, Combo> COMBOS = new HashMap<>();
	private static final Map<UUID, State> STATES = new HashMap<>();

	private FruitStrikes() {
	}

	public static void register() {
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> STATES.remove(handler.getPlayer().getUUID()));
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> STATES.clear());
	}

	public static void combo(ResourceLocation power, Combo combo) {
		COMBOS.put(power, combo);
	}

	/** Whether this power has an M1 combo (both sides: the client checks it before sending). */
	public static boolean has(String powerId) {
		return powerId != null && !powerId.isEmpty() && COMBOS.containsKey(ResourceLocation.tryParse(powerId));
	}

	public static int step(ServerPlayer player) {
		State s = STATES.get(player.getUUID());
		return s == null ? 0 : s.step();
	}

	/** Handles one press of the attack button. Returns the step played, or -1 if ignored (too soon, no combo, wrong hand). */
	public static int swing(ServerPlayer player) {
		if (!player.isAlive() || player.isSpectator()
				|| player.getInventory().selected >= com.grandline.player.loadout.LoadoutOrder.SIZE
				|| com.grandline.gear.Gear.powerFor(player.getMainHandItem().getItem()) != null) {
			return -1;
		}
		PlayerProfile profile = PlayerProfileService.get(player);
		if (!profile.powers().hasFruit()) {
			return -1;
		}
		Power power = Powers.get(profile.powers().fruit());
		Combo combo = power == null ? null : COMBOS.get(power.id());
		if (combo == null) {
			return -1;
		}
		long now = player.serverLevel().getGameTime();
		State old = STATES.get(player.getUUID());
		if (old != null && now - old.lastTick() < combo.interval()) {
			return -1;
		}
		boolean fresh = old == null || now - old.lastTick() > combo.reset();
		int step = fresh ? 0 : (old.step() + 1) % combo.steps();
		STATES.put(player.getUUID(), new State(step, now));
		double multiplier = PlayerProfileService.statRules().abilityMultiplier(profile.stats(), StatType.FRUIT);
		combo.strike().strike(new AbilityContext(player, power, combo.ability(), multiplier), step, step == combo.steps() - 1);
		return step;
	}
}
