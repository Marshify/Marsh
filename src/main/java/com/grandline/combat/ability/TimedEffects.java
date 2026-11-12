package com.grandline.combat.ability;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Tiny server-thread scheduler for multi-tick move effects (projectiles, eruptions, volleys). Each effect is ticked
 * once per server tick until it reports it is finished. Effects are in memory only and simply end on shutdown.
 */
public final class TimedEffects {
	/** One running effect. Return {@code false} when finished. */
	@FunctionalInterface
	public interface Effect {
		boolean tick(int age);
	}

	private record Running(Effect effect, int[] age) {
	}

	private static final List<Running> RUNNING = new ArrayList<>();
	private static final List<Running> PENDING = new ArrayList<>();

	private TimedEffects() {
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> tick());
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			RUNNING.clear();
			PENDING.clear();
		});
	}

	/** Starts {@code effect}; its first tick runs on the next server tick. */
	public static void start(Effect effect) {
		PENDING.add(new Running(effect, new int[]{0}));
	}

	/** Runs {@code action} once after {@code delayTicks}. */
	public static void later(int delayTicks, Runnable action) {
		start(age -> {
			if (age >= delayTicks) {
				action.run();
				return false;
			}
			return true;
		});
	}

	public static int running() {
		return RUNNING.size() + PENDING.size();
	}

	private static void tick() {
		RUNNING.addAll(PENDING);
		PENDING.clear();
		Iterator<Running> iterator = RUNNING.iterator();
		while (iterator.hasNext()) {
			Running running = iterator.next();
			boolean keep;
			try {
				keep = running.effect().tick(running.age()[0]++);
			} catch (RuntimeException e) {
				com.grandline.GrandLineMod.LOGGER.error("Move effect failed", e);
				keep = false;
			}
			if (!keep) {
				iterator.remove();
			}
		}
	}
}
