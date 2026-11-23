package com.grandline.world;

import com.grandline.world.plan.Anchor;
import com.grandline.world.plan.IslandPlans;
import net.minecraft.server.level.ServerPlayer;

/**
 * The Upper Cloudtop Isles: in the world they are the upper region of the Cloudtop Isles plan, but on the chart and for
 * discovery they are an island of their own. Their discovery is stored with the island ids under {@link #id}, derived
 * from Cloudtop's id, so saves and the island list need no new fields.
 */
public final class UpperIsles {
	public static final String HOST = "Cloudtop Isles";
	public static final String NAME = "Upper Cloudtop Isles";
	public static final int LEVEL = 450;
	private static final long TAG = 0x5550_0000_0000_0000L;

	private UpperIsles() {
	}

	public static boolean hosts(Island island) {
		return HOST.equals(island.name());
	}

	/** The discovery id of the Upper Cloudtop Isles above {@code cloudtop}. */
	public static long id(Island cloudtop) {
		return cloudtop.id() ^ TAG;
	}

	/** Whether the player stands up on the upper isles: near one of the upper region's spots, at its height. */
	public static boolean inArea(ServerPlayer player, Island cloudtop) {
		if (player.getVehicle() instanceof net.minecraft.world.entity.vehicle.Boat) {
			return false;
		}
		for (Anchor.Type type : Anchor.Type.values()) {
			for (Anchor a : IslandPlans.anchors(cloudtop, type)) {
				if (a.region() != FirstSeaProgression.UPPER) {
					continue;
				}
				double dx = player.getX() - (cloudtop.x() + a.x()), dz = player.getZ() - (cloudtop.z() + a.z());
				boolean high = a.y() == Anchor.SURFACE ? player.getY() > 120 : Math.abs(player.getY() - a.y()) < 40;
				if (high && dx * dx + dz * dz < 90 * 90) {
					return true;
				}
			}
		}
		return false;
	}
}
