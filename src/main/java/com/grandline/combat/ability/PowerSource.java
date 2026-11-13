package com.grandline.combat.ability;

/** Where a set of moves comes from. The client sends this with an activation request; the server checks ownership. */
public enum PowerSource {
	/** The equipped fighting style (always present; default Brawler). */
	STYLE,
	/** The eaten fruit (only if the player has one). */
	FRUIT,
	/** The sword or gun in the player's main hand (only while holding one). */
	WEAPON;

	public static PowerSource byId(int id) {
		PowerSource[] values = values();
		return id >= 0 && id < values.length ? values[id] : null;
	}
}
