package com.grandline.combat.ability;

import net.minecraft.resources.Identifier;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Mutable, server-only cooldown state for one player. Times are measured in server ticks.
 * Access it through {@link AbilityCooldownService}, which also handles synchronization.
 */
public final class AbilityCooldowns {
	private final Map<Identifier, Window> windows = new LinkedHashMap<>();

	public record Window(int startTick, int endTick) {
		public int total() {
			return endTick - startTick;
		}
	}

	public void start(Identifier abilityId, int nowTick, int durationTicks) {
		if (durationTicks <= 0) {
			windows.remove(abilityId);
			return;
		}

		windows.put(abilityId, new Window(nowTick, nowTick + durationTicks));
	}

	public int remaining(Identifier abilityId, int nowTick) {
		Window window = windows.get(abilityId);
		return window == null ? 0 : Math.max(0, window.endTick() - nowTick);
	}

	public boolean isOnCooldown(Identifier abilityId, int nowTick) {
		return remaining(abilityId, nowTick) > 0;
	}

	/** Drops expired entries and returns the active ones. */
	public Map<Identifier, Window> active(int nowTick) {
		windows.values().removeIf(window -> window.endTick() <= nowTick);
		return Collections.unmodifiableMap(windows);
	}

	public void clear() {
		windows.clear();
	}
}
