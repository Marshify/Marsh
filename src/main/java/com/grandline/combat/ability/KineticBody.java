package com.grandline.combat.ability;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Set;

/**
 * Collision-aware movement of a player-sized body for server-simulated moves (dashes, launches, rebounds; reusable).
 * {@link #sweep} moves the body's real bounding box in short sub-steps, never into blocks, and reports the contact
 * normal of whatever stopped it (axis tests: a wall gives ±x or ±z, the ground +y, a ceiling −y; corners combine),
 * sliding along the free axes so corners redirect safely instead of sticking.
 */
public final class KineticBody {
	/** Where the body ended, how far it went, and the contact normal (null: no contact). */
	public record Step(Vec3 pos, double moved, Vec3 normal) {
	}

	private KineticBody() {
	}

	public static boolean free(ServerPlayer player, Vec3 at) {
		AABB box = player.getBoundingBox().move(at.subtract(player.position()));
		return player.level().noCollision(player, box);
	}

	public static Step sweep(ServerPlayer player, Vec3 from, Vec3 velocity) {
		double len = velocity.length();
		if (len < 1e-6) {
			return new Step(from, 0, null);
		}
		int n = Math.max(1, (int) Math.ceil(len / 0.35));
		Vec3 sub = velocity.scale(1.0 / n);
		Vec3 cur = from;
		Vec3 normal = null;
		for (int i = 0; i < n; i++) {
			Vec3 next = cur.add(sub);
			if (free(player, next)) {
				cur = next;
				continue;
			}
			// Contact: find which axes are blocked (the surface normal), and slide along the free ones.
			double nx = 0, ny = 0, nz = 0;
			Vec3 slide = cur;
			if (sub.x != 0) {
				if (free(player, slide.add(sub.x, 0, 0))) {
					slide = slide.add(sub.x, 0, 0);
				} else {
					nx = -Math.signum(sub.x);
				}
			}
			if (sub.z != 0) {
				if (free(player, slide.add(0, 0, sub.z))) {
					slide = slide.add(0, 0, sub.z);
				} else {
					nz = -Math.signum(sub.z);
				}
			}
			if (sub.y != 0) {
				if (free(player, slide.add(0, sub.y, 0))) {
					slide = slide.add(0, sub.y, 0);
				} else {
					ny = -Math.signum(sub.y);
				}
			}
			if (nx == 0 && ny == 0 && nz == 0) {
				nx = -Math.signum(sub.x); // diagonal-only obstruction (an edge): push back along the travel
				nz = -Math.signum(sub.z);
			}
			normal = new Vec3(nx, ny, nz).normalize();
			cur = slide;
			break;
		}
		return new Step(cur, cur.distanceTo(from), normal);
	}

	/**
	 * Living targets whose boxes (grown by {@code reach}) the body's box touched on the way {@code from} → {@code to},
	 * not yet in {@code struck}; they are added to it.
	 */
	public static List<LivingEntity> touched(ServerPlayer player, Vec3 from, Vec3 to, double reach, Set<Integer> struck) {
		Level level = player.level();
		AABB a = player.getBoundingBox().move(from.subtract(player.position()));
		AABB b = player.getBoundingBox().move(to.subtract(player.position()));
		AABB swept = a.minmax(b).inflate(reach);
		Vec3 centreA = a.getCenter(), centreB = b.getCenter();
		return level.getEntitiesOfClass(LivingEntity.class, swept, e -> e != player && e.isAlive() && !e.isSpectator()
				&& !(e instanceof net.minecraft.world.entity.player.Player p && p.isCreative()) && !struck.contains(e.getId())
				&& distanceToSegment(e.getBoundingBox().getCenter(), centreA, centreB) <= e.getBbWidth() / 2 + player.getBbWidth() / 2 + reach)
				.stream().peek(e -> struck.add(e.getId())).toList();
	}

	public static double distanceToSegment(Vec3 p, Vec3 a, Vec3 b) {
		Vec3 ab = b.subtract(a);
		double len2 = ab.lengthSqr();
		double t = len2 < 1e-9 ? 0 : Math.max(0, Math.min(1, p.subtract(a).dot(ab) / len2));
		return p.distanceTo(a.add(ab.scale(t)));
	}
}
