package com.grandline.combat.ability;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Timed charges attached to an entity (reusable: delayed bombs, curses, marks). The server owns the timer: a charge
 * follows its target and goes off exactly once after its fuse, through {@link Fuse#detonate}. If the target dies, is
 * removed, teleports to another dimension, or the owner leaves, the charge is dropped through {@link Fuse#lost} instead
 * (no damage is credited to a player who is gone). One charge per (owner, key, target): attaching again refreshes nothing.
 */
public final class AttachedCharges {
	public interface Fuse {
		void detonate(ServerPlayer owner, LivingEntity target);

		default void lost(ServerPlayer owner, Vec3 lastPos, int targetId) {
		}
	}

	private static final class Charge {
		final int id;
		final UUID owner;
		final String key;
		final int target;
		final ServerLevel level;
		final long detonateAt;
		final Fuse fuse;
		Vec3 last;

		Charge(int id, UUID owner, String key, int target, ServerLevel level, long detonateAt, Fuse fuse, Vec3 last) {
			this.id = id;
			this.owner = owner;
			this.key = key;
			this.target = target;
			this.level = level;
			this.detonateAt = detonateAt;
			this.fuse = fuse;
			this.last = last;
		}
	}

	private static final Map<Integer, Charge> CHARGES = new LinkedHashMap<>();
	private static int nextId = 1;

	private AttachedCharges() {
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(AttachedCharges::tick);
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> CHARGES.clear());
	}

	/** Attaches a charge that goes off after {@code fuseTicks}. Returns false if this owner's {@code key} is already on the target. */
	public static boolean attach(ServerPlayer owner, String key, LivingEntity target, int fuseTicks, Fuse fuse) {
		for (Charge c : CHARGES.values()) {
			if (c.target == target.getId() && c.owner.equals(owner.getUUID()) && c.key.equals(key)) {
				return false;
			}
		}
		ServerLevel level = (ServerLevel) target.level();
		Charge c = new Charge(nextId++, owner.getUUID(), key, target.getId(), level, level.getGameTime() + fuseTicks, fuse, target.position());
		CHARGES.put(c.id, c);
		return true;
	}

	public static boolean carries(LivingEntity target, String key) {
		return CHARGES.values().stream().anyMatch(c -> c.target == target.getId() && c.key.equals(key));
	}

	public static int count() {
		return CHARGES.size();
	}

	private static void tick(MinecraftServer server) {
		if (CHARGES.isEmpty()) {
			return;
		}
		List<Runnable> run = new ArrayList<>();
		for (Iterator<Charge> it = CHARGES.values().iterator(); it.hasNext(); ) {
			Charge c = it.next();
			ServerPlayer owner = server.getPlayerList().getPlayer(c.owner);
			Entity e = c.level.getEntity(c.target);
			if (owner == null || !(e instanceof LivingEntity target) || !target.isAlive() || target.isRemoved() || target.level() != c.level) {
				it.remove();
				Vec3 last = c.last;
				run.add(() -> c.fuse.lost(owner, last, c.target));
				continue;
			}
			c.last = target.position();
			if (c.level.getGameTime() >= c.detonateAt) {
				it.remove();
				run.add(() -> c.fuse.detonate(owner, target));
			}
		}
		run.forEach(Runnable::run);
	}
}
