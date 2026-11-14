package com.grandline.world.plan;

/** Read-only access to the rebuilt Cloudtop Isles' portal spots (island-local x, y, z, yaw). */
public final class CloudtopAccess {
	private CloudtopAccess() {
	}

	/** {portal up, arrival up, portal down (return doorway), arrival down}: x, standing y, z, yaw. */
	public static int[][] portals() {
		return new int[][]{
				{CloudtopIsles.PORTAL_UP_X, CloudtopIsles.PORTAL_UP_Y, CloudtopIsles.PORTAL_UP_Z, 0},
				{CloudtopIsles.ARRIVE_UP_X, CloudtopIsles.ARRIVE_UP_Y, CloudtopIsles.ARRIVE_UP_Z, -90},
				{CloudtopIsles.PORTAL_DOWN_X, CloudtopIsles.PORTAL_DOWN_Y, CloudtopIsles.PORTAL_DOWN_Z, 0},
				{CloudtopIsles.ARRIVE_DOWN_X, CloudtopIsles.ARRIVE_DOWN_Y, CloudtopIsles.ARRIVE_DOWN_Z, 0}};
	}
}
