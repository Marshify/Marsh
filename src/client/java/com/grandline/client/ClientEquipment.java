package com.grandline.client;

import com.grandline.gear.EquipmentOwnership;
import com.grandline.gear.WeaponCategory;
import com.grandline.network.payload.EquipWeaponPayload;
import com.grandline.network.payload.EquipmentSyncPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This client's view of its owned equipment and equipped weapons (sent by the server, which decides everything), for the
 * Backpack. Equip / Unequip only ask the server.
 */
public final class ClientEquipment {
	private static Set<String> owned = Set.of();
	private static Map<String, String> equipped = Map.of();

	private ClientEquipment() {
	}

	public static void register() {
		ClientPlayNetworking.registerGlobalReceiver(EquipmentSyncPayload.TYPE, (packet, player, sender) -> {
			owned = Set.copyOf(packet.owned());
			equipped = Map.copyOf(packet.equipped());
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			owned = Set.of();
			equipped = Map.of();
		});
	}

	/** Every owned sword, gun and rod, in a fixed order (swords, guns, rods; registry order within each). */
	public static List<Item> ownedWeapons() {
		List<Item> out = new ArrayList<>();
		for (WeaponCategory category : WeaponCategory.values()) {
			for (Item item : EquipmentOwnership.all()) {
				if (WeaponCategory.of(item) == category && owned.contains(EquipmentOwnership.id(item))) {
					out.add(item);
				}
			}
		}
		return out;
	}

	public static boolean isEquipped(Item item) {
		return equipped.containsValue(EquipmentOwnership.id(item));
	}

	/** The equipped weapon of {@code category}, or null. */
	public static Item equipped(WeaponCategory category) {
		String id = equipped.get(category.key);
		return id == null ? null : EquipmentOwnership.item(id);
	}

	public static void request(Item item, boolean equip) {
		if (ClientPlayNetworking.canSend(EquipWeaponPayload.TYPE)) {
			ClientPlayNetworking.send(new EquipWeaponPayload(EquipmentOwnership.id(item), equip));
		}
	}
}
