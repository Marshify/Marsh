package com.grandline.config;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * How one kind of hostile NPC fights (see GrandLineEnemy). Stored in {@code config/grandline.json} under
 * {@code enemyBehavior}, keyed by enemy kind ({@code bandit}, {@code raider}, {@code corsair}, {@code captain}) or
 * special group ({@code trial}: race trial waves). Distances are in blocks; every value is validated after loading.
 */
public final class EnemyBehavior {
	/** A player this close (and in sight, if {@link #lineOfSight}) is attacked. */
	public double aggressionRadius = 3;
	/** When not fighting, it stays and wanders this close to its spawn spot. */
	public double idleRadius = 3;
	/** It gives up the chase once it is this far from its spawn spot (measured from the spawn spot). */
	public double pursuitRadius = 20;
	/** No fighting this close to friendly NPCs (quest givers, merchants, teachers), arrival docks and the spawn point. */
	public double protectionRadius = 8;
	/** Players are only noticed when the enemy can see them (never through walls or terrain). */
	public boolean lineOfSight = true;
	/** Attacking it also provokes its neighbours. */
	public boolean groupAggression = false;
	/** It is healed to full once it is back at its spawn spot after giving up a fight. */
	public boolean healOnReset = true;

	public static Map<String, EnemyBehavior> defaults() {
		Map<String, EnemyBehavior> map = new LinkedHashMap<>();
		map.put("bandit", new EnemyBehavior());
		EnemyBehavior trainee = new EnemyBehavior(); // Ironhold's training field: a short leash keeps them inside the courtyard
		trainee.pursuitRadius = 12;
		map.put("marine_trainee", trainee);
		map.put("raider", new EnemyBehavior());
		map.put("corsair", new EnemyBehavior());
		EnemyBehavior captain = new EnemyBehavior(); // island bosses: a bigger presence in their arena
		captain.aggressionRadius = 6;
		captain.idleRadius = 4;
		captain.pursuitRadius = 28;
		map.put("captain", captain);
		EnemyBehavior trial = new EnemyBehavior(); // race trial waves: the whole arena is the fight
		trial.aggressionRadius = 40;
		trial.idleRadius = 6;
		trial.pursuitRadius = 64;
		trial.protectionRadius = 0;
		trial.lineOfSight = false;
		trial.healOnReset = false;
		map.put("trial", trial);
		return map;
	}

	void validate() {
		aggressionRadius = clamp(aggressionRadius, 0, 64);
		idleRadius = clamp(idleRadius, 0, 32);
		pursuitRadius = clamp(pursuitRadius, 1, 128);
		protectionRadius = clamp(protectionRadius, 0, 64);
	}

	private static double clamp(double v, double min, double max) {
		return Double.isNaN(v) ? min : Math.max(min, Math.min(max, v));
	}
}
