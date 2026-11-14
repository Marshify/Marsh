package com.grandline.config;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One race's balance, each value given per evolution stage {V1, V2, V3, V4} (fractions: 0.05 = 5%). Stored in
 * {@code config/grandline.json} under {@code races}, keyed by race id.
 */
public final class RaceBalance {
	/** Walking speed bonus. */
	public double[] speed = {0, 0, 0, 0};
	/** Damage dealt bonus (every attack: melee, weapons, fruit moves). */
	public double[] damage = {0, 0, 0, 0};
	/** Damage taken reduction. */
	public double[] reduction = {0, 0, 0, 0};
	/** Extra passive regeneration (added to the 1%/s base, as a fraction of it). */
	public double[] regen = {0, 0, 0, 0};
	/** Share of damage dealt that heals the attacker (Ghoul: at night only for V1). */
	public double[] lifesteal = {0, 0, 0, 0};
	/** Jump height bonus. */
	public double[] jump = {0, 0, 0, 0};
	/** Swimming speed bonus. */
	public double[] swim = {0, 0, 0, 0};
	/** Multiplier on sea weakness damage for fruit users (1 = normal; Shark protection). */
	public double[] ocean = {1, 1, 1, 1};
	/** Energy cost reduction for moves. */
	public double[] energy = {0, 0, 0, 0};
	/** Move cooldown reduction. */
	public double[] cooldown = {0, 0, 0, 0};
	/** Extra damage dealt at night. */
	public double[] night = {0, 0, 0, 0};
	/** V3 ability: duration and cooldown in seconds, and its power (meaning depends on the race). */
	public double v3Duration = 10, v3Cooldown = 60, v3Power = 0.3;
	/** V4 awakening: duration in seconds and power. */
	public double v4Duration = 20, v4Power = 0.4;

	static RaceBalance of(java.util.function.Consumer<RaceBalance> setup) {
		RaceBalance b = new RaceBalance();
		setup.accept(b);
		return b;
	}

	public static Map<String, RaceBalance> defaults() {
		Map<String, RaceBalance> m = new LinkedHashMap<>();
		m.put("human", of(b -> {
			b.damage = new double[]{0.05, 0.08, 0.10, 0.12};
			b.speed = new double[]{0.02, 0.04, 0.05, 0.06};
			b.v3Duration = 12; b.v3Cooldown = 70; b.v3Power = 0.5;   // Last Stand: up to +50% damage at low health
			b.v4Power = 0.35;                                         // Berserker: +35% damage, faster attacks
		}));
		m.put("shark", of(b -> {
			b.reduction = new double[]{0.05, 0.08, 0.10, 0.12};
			b.swim = new double[]{0.3, 0.5, 0.7, 0.9};
			b.ocean = new double[]{0.85, 0.7, 0.55, 0.4};
			b.v3Duration = 10; b.v3Cooldown = 75; b.v3Power = 0.45;  // Water Body: -45% damage taken
			b.v4Power = 0.5;                                          // Leviathan: -50% damage taken, shockwaves
		}));
		m.put("angel", of(b -> {
			b.jump = new double[]{0.15, 0.25, 0.3, 0.35};
			b.regen = new double[]{0.25, 0.5, 0.6, 0.75};
			b.energy = new double[]{0.05, 0.1, 0.12, 0.15};
			b.v3Duration = 10; b.v3Cooldown = 80; b.v3Power = 0.03;  // Heavenly Blood: heal 3% max health per second
			b.v4Power = 0.04;                                         // Celestial: 4%/s regen, flight-like leaps
		}));
		m.put("rabbit", of(b -> {
			b.speed = new double[]{0.08, 0.12, 0.15, 0.18};
			b.jump = new double[]{0.1, 0.15, 0.18, 0.2};
			b.v3Duration = 8; b.v3Cooldown = 50; b.v3Power = 0.6;    // Agility: +60% speed
			b.v4Power = 0.9;                                          // Velocity: +90% speed, dashes
		}));
		m.put("cyborg", of(b -> {
			b.reduction = new double[]{0.04, 0.06, 0.08, 0.10};
			b.energy = new double[]{0.1, 0.15, 0.18, 0.2};
			b.v3Duration = 6; b.v3Cooldown = 60; b.v3Power = 12;     // Energy Core: 12 damage pulse (+level scaling)
			b.v4Power = 8;                                            // Overdrive: pulses every 2 s
		}));
		m.put("ghoul", of(b -> {
			b.lifesteal = new double[]{0.03, 0.05, 0.06, 0.08};
			b.night = new double[]{0.08, 0.12, 0.15, 0.18};
			b.cooldown = new double[]{0.03, 0.05, 0.07, 0.10};
			b.v3Duration = 10; b.v3Cooldown = 70; b.v3Power = 0.12;  // Heightened Senses: +12% lifesteal, speed, cooldowns
			b.v4Power = 0.15;                                         // Blood Eclipse: +15% lifesteal and more
		}));
		return m;
	}

	/** Value for stage 1-4 (clamped), tolerating short arrays from hand-edited configs. */
	public static double at(double[] values, int stage) {
		if (values == null || values.length == 0) {
			return 0;
		}
		return values[Math.max(0, Math.min(values.length - 1, stage - 1))];
	}
}
