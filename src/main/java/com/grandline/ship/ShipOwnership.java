package com.grandline.ship;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.List;

/** Ships a player owns (class ids) and the one Call Ship launches. Stored on the player, kept through death. */
public record ShipOwnership(List<String> owned, String selected) {
	public static final ShipOwnership EMPTY = new ShipOwnership(List.of(), "");
	public static final Codec<ShipOwnership> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.STRING.listOf().fieldOf("owned").forGetter(ShipOwnership::owned),
			Codec.STRING.optionalFieldOf("selected", "").forGetter(ShipOwnership::selected)
	).apply(i, ShipOwnership::new));

	public boolean owns(ShipClass ship) {
		return owned.contains(ship.id);
	}

	public ShipOwnership withShip(ShipClass ship) {
		List<String> list = new ArrayList<>(owned);
		if (!list.contains(ship.id)) {
			list.add(ship.id);
		}
		return new ShipOwnership(List.copyOf(list), ship.id);
	}

	public ShipOwnership select(ShipClass ship) {
		return new ShipOwnership(owned, ship.id);
	}

	/** The ship Call Ship launches: the selected one, else the biggest owned, else null. */
	public ShipClass callable() {
		ShipClass chosen = ShipClass.byId(selected);
		if (chosen != null && owns(chosen)) {
			return chosen;
		}
		ShipClass best = null;
		for (ShipClass c : ShipClass.values()) {
			if (owns(c)) {
				best = c;
			}
		}
		return best;
	}
}
