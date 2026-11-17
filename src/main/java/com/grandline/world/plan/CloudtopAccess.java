package com.grandline.world.plan;

/** Read-only access to the Cloudtop Isles' portal spots (island-local x, y, z, yaw) for the plan a world uses. */
public final class CloudtopAccess {
	private CloudtopAccess() {
	}

	/**
	 * {portal up, arrival up, portal down (return), arrival down}: x, standing y, z, yaw; null for a plan without portals
	 * (the original island).
	 */
	public static int[][] portals(IslandPlan plan) {
		if (plan instanceof CloudtopIsles) {
			return new int[][]{
					{CloudtopIsles.PORTAL_UP_X, CloudtopIsles.PORTAL_UP_Y, CloudtopIsles.PORTAL_UP_Z, 0},
					{CloudtopIsles.ARRIVE_UP_X, CloudtopIsles.ARRIVE_UP_Y, CloudtopIsles.ARRIVE_UP_Z, 0},
					{CloudtopIsles.PORTAL_DOWN_X, CloudtopIsles.PORTAL_DOWN_Y, CloudtopIsles.PORTAL_DOWN_Z, 0},
					{CloudtopIsles.ARRIVE_DOWN_X, CloudtopIsles.ARRIVE_DOWN_Y, CloudtopIsles.ARRIVE_DOWN_Z, 0}};
		}
		if (plan instanceof CloudtopIslesV12) {
			return new int[][]{
					{CloudtopIslesV12.PORTAL_UP_X, CloudtopIslesV12.PORTAL_UP_Y, CloudtopIslesV12.PORTAL_UP_Z, 0},
					{CloudtopIslesV12.ARRIVE_UP_X, CloudtopIslesV12.ARRIVE_UP_Y, CloudtopIslesV12.ARRIVE_UP_Z, 180},
					{CloudtopIslesV12.PORTAL_DOWN_X, CloudtopIslesV12.PORTAL_DOWN_Y, CloudtopIslesV12.PORTAL_DOWN_Z, 0},
					{CloudtopIslesV12.ARRIVE_DOWN_X, CloudtopIslesV12.ARRIVE_DOWN_Y, CloudtopIslesV12.ARRIVE_DOWN_Z, 0}};
		}
		if (plan instanceof CloudtopIslesV7) {
			return new int[][]{
					{CloudtopIslesV7.PORTAL_UP_X, CloudtopIslesV7.PORTAL_UP_Y, CloudtopIslesV7.PORTAL_UP_Z, 0},
					{CloudtopIslesV7.ARRIVE_UP_X, CloudtopIslesV7.ARRIVE_UP_Y, CloudtopIslesV7.ARRIVE_UP_Z, -90},
					{CloudtopIslesV7.PORTAL_DOWN_X, CloudtopIslesV7.PORTAL_DOWN_Y, CloudtopIslesV7.PORTAL_DOWN_Z, 0},
					{CloudtopIslesV7.ARRIVE_DOWN_X, CloudtopIslesV7.ARRIVE_DOWN_Y, CloudtopIslesV7.ARRIVE_DOWN_Z, 0}};
		}
		return null;
	}
}
