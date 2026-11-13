package com.grandline.player;

import net.minecraft.util.StringRepresentable;

/**
 * The stats a player can invest unallocated points into.
 *
 * <p>The serialized names are part of the save format and the network protocol: never rename them, only add new ones.
 */
public enum StatType implements StringRepresentable {
	/** Bonus damage when fighting with an empty hand. */
	MELEE("melee", true),
	/** Bonus maximum health. */
	DEFENSE("defense", true),
	/** Bonus damage while holding a sword. */
	SWORD("sword", true),
	/** Bonus damage for gun moves. */
	GUN("gun", true),
	/** Bonus damage for fruit moves. */
	FRUIT("fruit", true);

	public static final StringRepresentable.EnumCodec<StatType> CODEC = StringRepresentable.fromEnum(StatType::values);

	private final String serializedName;
	private final boolean hasEffect;

	StatType(String serializedName, boolean hasEffect) {
		this.serializedName = serializedName;
		this.hasEffect = hasEffect;
	}

	@Override
	public String getSerializedName() {
		return serializedName;
	}

	/** Whether investing in this stat currently does anything in game. */
	public boolean hasEffect() {
		return hasEffect;
	}

	public String translationKey() {
		return "stat.grandline." + serializedName;
	}

	/** Looks a stat up by its network/command id, or returns {@code null} for unknown values. */
	public static StatType byId(int id) {
		StatType[] values = values();
		return id >= 0 && id < values.length ? values[id] : null;
	}
}
