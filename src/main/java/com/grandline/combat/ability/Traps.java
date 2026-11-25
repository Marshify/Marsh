package com.grandline.combat.ability;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiPredicate;

/**
 * Persistent explosive traps with server-authoritative chain reactions (reusable by fruits, guns, styles and bosses).
 *
 * <p>A trap arms after {@link Config#armTicks}, then detonates when an eligible entity comes within
 * {@link Config#triggerRadius} (proximity), when an explosion of its owner reaches it ({@link #triggerNear}), when its
 * owner sets it off ({@link #detonateOwned}), or as part of a chain: every detonation schedules the owner's other traps
 * within {@link Config#chainRadius} after {@link Config#chainDelay} ticks. Each trap moves through
 * ARMING → ARMED → PENDING → gone, so it detonates exactly once and a chain can never loop. At most
 * {@link #DETONATIONS_PER_TICK} traps go off per server tick (the rest wait a tick), traps expire after
 * {@link Config#lifeTicks}, each owner keeps at most {@link Config#maxPerOwner} (the oldest is removed), and every trap
 * is removed when its owner leaves, dies, changes dimension or stops being valid ({@link Config#ownerValid}).
 */
public final class Traps {
	/** Most detonations processed in one server tick (big chains spread over a few ticks). */
	public static final int DETONATIONS_PER_TICK = 6;

	public enum Cause {
		PROXIMITY, EXPLOSION, OWNER, CHAIN
	}

	public enum Removal {
		EXPIRED, REPLACED, OWNER_GONE
	}

	/** What a trap kind does. Called on the server thread. */
	public interface Listener {
		void detonate(Trap trap, ServerPlayer owner, Cause cause);

		void removed(Trap trap, Removal why);
	}

	/**
	 * @param ownerValid whether the owner may still keep traps (e.g. still has the fruit); checked every tick
	 * @param eligible   whether an entity sets the trap off by proximity
	 */
	public record Config(String kind, int armTicks, int lifeTicks, double triggerRadius, double chainRadius, int chainDelay, int maxPerOwner,
			BiPredicate<ServerPlayer, Trap> ownerValid, BiPredicate<ServerPlayer, LivingEntity> eligible, Listener listener) {
	}

	private enum State {
		ARMING, ARMED, PENDING
	}

	public static final class Trap {
		public final int id;
		public final UUID owner;
		public final ServerLevel level;
		public final Vec3 pos;
		public final Vec3 normal;
		public final Config config;
		public final long placed;
		State state = State.ARMING;
		long detonateAt;
		Cause cause;

		Trap(int id, UUID owner, ServerLevel level, Vec3 pos, Vec3 normal, Config config, long placed) {
			this.id = id;
			this.owner = owner;
			this.level = level;
			this.pos = pos;
			this.normal = normal;
			this.config = config;
			this.placed = placed;
		}

		public boolean armed() {
			return state == State.ARMED;
		}
	}

	private static final Map<Integer, Trap> TRAPS = new LinkedHashMap<>();
	private static int nextId = 1;

	private Traps() {
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(Traps::tick);
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> TRAPS.clear());
	}

	/** Places a trap (removing the owner's oldest of the same kind past the limit). Returns it. */
	public static Trap place(ServerPlayer owner, Vec3 pos, Vec3 normal, Config config) {
		List<Trap> mine = owned(owner.getUUID(), config.kind());
		while (mine.size() >= config.maxPerOwner()) {
			Trap oldest = mine.remove(0);
			TRAPS.remove(oldest.id);
			config.listener().removed(oldest, Removal.REPLACED);
		}
		Trap trap = new Trap(nextId++, owner.getUUID(), owner.serverLevel(), pos, normal, config, owner.serverLevel().getGameTime());
		TRAPS.put(trap.id, trap);
		return trap;
	}

	/** The owner's live traps of {@code kind}, oldest first. */
	public static List<Trap> owned(UUID owner, String kind) {
		List<Trap> out = new ArrayList<>();
		for (Trap t : TRAPS.values()) {
			if (t.owner.equals(owner) && t.config.kind().equals(kind)) {
				out.add(t);
			}
		}
		return out;
	}

	public static int count() {
		return TRAPS.size();
	}

	/** An explosion of {@code owner} at {@code at} sets off their armed or arming traps within {@code radius}. */
	public static void triggerNear(ServerPlayer owner, Vec3 at, double radius) {
		long now = owner.serverLevel().getGameTime();
		for (Trap t : TRAPS.values()) {
			if (t.owner.equals(owner.getUUID()) && t.level == owner.serverLevel() && t.state != State.PENDING
					&& t.pos.distanceToSqr(at) <= radius * radius) {
				schedule(t, now + t.config.chainDelay(), Cause.EXPLOSION);
			}
		}
	}

	/** The owner sets off every trap of {@code kind}, nearest first, one chain step apart. Returns how many. */
	public static int detonateOwned(ServerPlayer owner, String kind) {
		long now = owner.serverLevel().getGameTime();
		List<Trap> mine = owned(owner.getUUID(), kind);
		mine.sort(Comparator.comparingDouble(t -> t.pos.distanceToSqr(owner.position())));
		int n = 0;
		for (Trap t : mine) {
			if (t.state != State.PENDING) {
				schedule(t, now + 1 + n * Math.max(1, t.config.chainDelay() / 2), Cause.OWNER);
				n++;
			}
		}
		return n;
	}

	/** Removes all of an owner's traps silently (cleanup). */
	public static void clearOwner(UUID owner) {
		Iterator<Trap> it = TRAPS.values().iterator();
		List<Trap> gone = new ArrayList<>();
		while (it.hasNext()) {
			Trap t = it.next();
			if (t.owner.equals(owner)) {
				it.remove();
				gone.add(t);
			}
		}
		gone.forEach(t -> t.config.listener().removed(t, Removal.OWNER_GONE));
	}

	private static void schedule(Trap t, long at, Cause cause) {
		if (t.state == State.PENDING) {
			return; // already going off: never twice
		}
		t.state = State.PENDING;
		t.detonateAt = at;
		t.cause = cause;
	}

	private static void tick(MinecraftServer server) {
		if (TRAPS.isEmpty()) {
			return;
		}
		List<Trap> fire = new ArrayList<>();
		List<Trap> removed = new ArrayList<>();
		List<Removal> why = new ArrayList<>();
		for (Iterator<Trap> it = TRAPS.values().iterator(); it.hasNext(); ) {
			Trap t = it.next();
			ServerPlayer owner = server.getPlayerList().getPlayer(t.owner);
			long now = t.level.getGameTime();
			if (owner == null || !owner.isAlive() || owner.level() != t.level || !t.config.ownerValid().test(owner, t)) {
				it.remove();
				removed.add(t);
				why.add(Removal.OWNER_GONE);
				continue;
			}
			if (t.state != State.PENDING && now - t.placed >= t.config.lifeTicks()) {
				it.remove();
				removed.add(t);
				why.add(Removal.EXPIRED);
				continue;
			}
			if (t.state == State.ARMING && now - t.placed >= t.config.armTicks()) {
				t.state = State.ARMED;
			}
			if (t.state == State.ARMED && (now + t.id) % 2 == 0) {
				double r = t.config.triggerRadius();
				AABB box = new AABB(t.pos, t.pos).inflate(r, r + 1, r);
				boolean stepped = !t.level.getEntitiesOfClass(LivingEntity.class, box, e -> e != owner && e.isAlive() && !e.isSpectator()
						&& t.config.eligible().test(owner, e)).isEmpty();
				if (stepped) {
					schedule(t, now + 2, Cause.PROXIMITY);
				}
			}
			if (t.state == State.PENDING && now >= t.detonateAt && fire.size() < DETONATIONS_PER_TICK) {
				it.remove();
				fire.add(t);
			}
		}
		for (int i = 0; i < removed.size(); i++) {
			removed.get(i).config.listener().removed(removed.get(i), why.get(i));
		}
		for (Trap t : fire) {
			ServerPlayer owner = server.getPlayerList().getPlayer(t.owner);
			if (owner == null) {
				continue;
			}
			// Chain: the owner's other traps nearby go off one step later (each only once).
			long now = t.level.getGameTime();
			double cr = t.config.chainRadius();
			for (Trap o : TRAPS.values()) {
				if (o.owner.equals(t.owner) && o.level == t.level && o.state != State.PENDING && o.pos.distanceToSqr(t.pos) <= cr * cr) {
					schedule(o, now + t.config.chainDelay(), Cause.CHAIN);
				}
			}
			t.config.listener().detonate(t, owner, t.cause);
		}
	}
}
