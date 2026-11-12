package com.grandline.entity;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/** Enemy archetypes. Islands pick the kind by their recommended level. */
public enum EnemyKind {
	BANDIT("bandit", Items.IRON_SWORD, 1.0, 1.0),
	RAIDER("raider", Items.IRON_AXE, 1.3, 1.2),
	CORSAIR("corsair", Items.DIAMOND_SWORD, 1.7, 1.4),
	CAPTAIN("captain", Items.NETHERITE_SWORD, 9.0, 1.8);

	private final String id;
	private final Item weapon;
	private final double healthScale;
	private final double damageScale;

	EnemyKind(String id, Item weapon, double healthScale, double damageScale) {
		this.id = id;
		this.weapon = weapon;
		this.healthScale = healthScale;
		this.damageScale = damageScale;
	}

	public String id() {
		return id;
	}

	public Item weapon() {
		return weapon;
	}

	public double healthScale() {
		return healthScale;
	}

	public double damageScale() {
		return damageScale;
	}

	/** Regular enemy kind for an island of the given recommended level. */
	public static EnemyKind forLevel(int level) {
		return level < 250 ? BANDIT : level < 1000 ? RAIDER : CORSAIR;
	}
}
