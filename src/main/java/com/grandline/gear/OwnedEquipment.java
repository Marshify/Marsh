package com.grandline.gear;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * The equipment a player owns: registered item ids ({@code grandline:katana}, ...), each at most once, and which owned
 * weapon is equipped in each category ({@code equipped}: "sword", "gun", "rod" → item id; a missing category has none
 * equipped). Only equipped weapons are held as items; the rest wait in the Backpack.
 *
 * <p>{@code migrated} is set once the player's existing items have been counted in (see
 * {@link EquipmentOwnership#migrate}); {@code equipVersion} once the equipped weapons were chosen from what the player
 * held (saves from before the Backpack equipment system).
 */
public record OwnedEquipment(Set<String> ids, boolean migrated, Map<String, String> equipped, int equipVersion) {
	public static final int EQUIP_VERSION = 1;
	public static final OwnedEquipment EMPTY = new OwnedEquipment(Set.of(), false, Map.of(), 0);
	public static final Codec<OwnedEquipment> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.STRING.listOf().optionalFieldOf("ids", List.of()).forGetter(o -> List.copyOf(o.ids())),
			Codec.BOOL.optionalFieldOf("migrated", false).forGetter(OwnedEquipment::migrated),
			Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("equipped", Map.of()).forGetter(OwnedEquipment::equipped),
			Codec.INT.optionalFieldOf("equip_version", 0).forGetter(OwnedEquipment::equipVersion)
	).apply(instance, (ids, migrated, equipped, version) -> new OwnedEquipment(new TreeSet<>(ids), migrated, equipped, version)));

	public OwnedEquipment {
		ids = Set.copyOf(new TreeSet<>(ids));
		TreeMap<String, String> eq = new TreeMap<>();
		equipped.forEach((k, v) -> {
			if (v != null && !v.isEmpty()) {
				eq.put(k, v);
			}
		});
		equipped = Map.copyOf(eq);
	}

	/** Pre-Backpack constructor (ids and migration only). */
	public OwnedEquipment(Set<String> ids, boolean migrated) {
		this(ids, migrated, Map.of(), 0);
	}

	public boolean owns(String id) {
		return ids.contains(id);
	}

	public OwnedEquipment with(String id) {
		TreeSet<String> next = new TreeSet<>(ids);
		next.add(id);
		return new OwnedEquipment(next, migrated, equipped, equipVersion);
	}

	/** Takes ownership away (and unequips it). */
	public OwnedEquipment without(String id) {
		TreeSet<String> next = new TreeSet<>(ids);
		next.remove(id);
		TreeMap<String, String> eq = new TreeMap<>(equipped);
		eq.values().removeIf(id::equals);
		return new OwnedEquipment(next, migrated, eq, equipVersion);
	}

	public OwnedEquipment markMigrated() {
		return new OwnedEquipment(ids, true, equipped, equipVersion);
	}

	/** The equipped item id in {@code category}, or "" when none is. */
	public String equippedIn(String category) {
		return equipped.getOrDefault(category, "");
	}

	public boolean isEquipped(String id) {
		return equipped.containsValue(id);
	}

	/** Equips {@code id} in {@code category} ("" unequips the category). */
	public OwnedEquipment withEquipped(String category, String id) {
		TreeMap<String, String> eq = new TreeMap<>(equipped);
		if (id == null || id.isEmpty()) {
			eq.remove(category);
		} else {
			eq.put(category, id);
		}
		return new OwnedEquipment(ids, migrated, eq, equipVersion);
	}

	public OwnedEquipment withEquipVersion(int version) {
		return new OwnedEquipment(ids, migrated, equipped, version);
	}
}
