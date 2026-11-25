package com.grandline.combat.ability;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Lingering server-authoritative damage zones (reusable: smoke clouds, poison pools, fire fields). A zone stays where it
 * was created, pulses every {@code interval} ticks for {@code duration} ticks, and each pulse hands the living
 * entities inside (cylinder of {@code radius} × {@code height}, never the owner) to {@link Pulse} exactly once, so an
 * entity can never be hit twice in one interval and anyone who walks out stops being hit. It ends early if the owner
 * dies, leaves or changes world.
 */
public final class DamageZones {
	@FunctionalInterface
	public interface Pulse {
		void pulse(int index, List<LivingEntity> inside);
	}

	private DamageZones() {
	}

	public static void start(ServerPlayer owner, Vec3 at, double radius, double height, int duration, int interval, Pulse pulse) {
		ServerLevel level = owner.serverLevel();
		int[] index = {0};
		TimedEffects.start(age -> {
			if (!owner.isAlive() || owner.isRemoved() || owner.level() != level) {
				return false;
			}
			int t = age - 1;
			if (t >= duration) {
				return false;
			}
			if (t % interval == 0) {
				pulse.pulse(index[0]++, inside(owner, at, radius, height));
			}
			return true;
		});
	}

	public static List<LivingEntity> inside(ServerPlayer owner, Vec3 at, double radius, double height) {
		AABB box = new AABB(at.x - radius, at.y - 1, at.z - radius, at.x + radius, at.y + height, at.z + radius);
		return owner.serverLevel().getEntitiesOfClass(LivingEntity.class, box, e -> e != owner && e.isAlive() && !e.isSpectator()
				&& !(e instanceof Player p && p.isCreative()) && Math.hypot(e.getX() - at.x, e.getZ() - at.z) <= radius + e.getBbWidth() / 2);
	}
}
