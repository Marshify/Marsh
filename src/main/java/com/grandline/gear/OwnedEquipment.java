package com.grandline.gear;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * The equipment a player owns: registered item ids ({@code grandline:katana}, ...), each at most once. {@code migrated}
 * is set once the player's existing items have been counted in (see {@link EquipmentOwnership#migrate}).
 */
public record OwnedEquipment(Set<String> ids, boolean migrated) {
	public static final OwnedEquipment EMPTY = new OwnedEquipment(Set.of(), false);
	public static final Codec<OwnedEquipment> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.STRING.listOf().optionalFieldOf("ids", List.of()).forGetter(o -> List.copyOf(o.ids())),
			Codec.BOOL.optionalFieldOf("migrated", false).forGetter(OwnedEquipment::migrated)
	).apply(instance, (ids, migrated) -> new OwnedEquipment(new TreeSet<>(ids), migrated)));

	public OwnedEquipment {
		ids = Set.copyOf(new TreeSet<>(ids));
	}

	public boolean owns(String id) {
		return ids.contains(id);
	}

	public OwnedEquipment with(String id) {
		TreeSet<String> next = new TreeSet<>(ids);
		next.add(id);
		return new OwnedEquipment(next, migrated);
	}

	public OwnedEquipment without(String id) {
		TreeSet<String> next = new TreeSet<>(ids);
		next.remove(id);
		return new OwnedEquipment(next, migrated);
	}

	public OwnedEquipment markMigrated() {
		return new OwnedEquipment(ids, true);
	}
}
