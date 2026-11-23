package com.grandline.client;

import com.grandline.names.GrandLineNames;
import com.grandline.network.payload.NamesSyncPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.locale.Language;

/** Applies the server's renamed names on this client (see GrandLineNames); back to the originals when leaving. */
public final class ClientNames {
	private ClientNames() {
	}

	public static void register() {
		ClientPlayNetworking.registerGlobalReceiver(NamesSyncPayload.TYPE, (packet, player, sender) ->
				Minecraft.getInstance().execute(() -> apply(new GrandLineNames.Active(copy(packet.keys()), copy(packet.islands())))));
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(() -> apply(GrandLineNames.Active.EMPTY)));
	}

	private static java.util.Map<String, String> copy(java.util.Map<String, String> m) {
		return java.util.Collections.unmodifiableMap(new java.util.LinkedHashMap<>(m));
	}

	private static void apply(GrandLineNames.Active names) {
		GrandLineNames.setActive(names);
		// Re-installing the language lays the new names on top and makes cached text translate again.
		Language.inject(Language.getInstance());
	}
}
