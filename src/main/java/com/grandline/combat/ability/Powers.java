package com.grandline.combat.ability;

import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Registry of every fighting style and fruit. Populated during mod initialization, identical on client and server. */
public final class Powers {
	private static final Map<ResourceLocation, Power> POWERS = new LinkedHashMap<>();

	private Powers() {
	}

	public static Power register(Power power) {
		if (POWERS.putIfAbsent(power.id(), power) != null) {
			throw new IllegalStateException("Duplicate power " + power.id());
		}
		return power;
	}

	public static Power get(String id) {
		ResourceLocation location = ResourceLocation.tryParse(id);
		return location == null ? null : POWERS.get(location);
	}

	public static Power get(ResourceLocation id) {
		return POWERS.get(id);
	}

	public static Collection<Power> all() {
		return Collections.unmodifiableCollection(POWERS.values());
	}
}
