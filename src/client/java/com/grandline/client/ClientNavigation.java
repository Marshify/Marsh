package com.grandline.client;

import com.grandline.network.payload.NavTargetPayload;
import com.grandline.network.payload.NavTogglePayload;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.lwjgl.glfw.GLFW;

/**
 * Client side of quest navigation: the server's current target for this player, the N key that toggles it (the choice
 * is saved on the server), and the one entity this player sees outlined.
 */
public final class ClientNavigation {
	public static final KeyMapping TOGGLE = new KeyMapping("key.grandline.navigation", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_N,
			"key.categories.grandline");
	private static NavTargetPayload target;

	private ClientNavigation() {
	}

	public static NavTargetPayload target() {
		return target;
	}

	public static boolean enabled() {
		return target != null && target.enabled();
	}

	/** Whether this entity is the quest giver this player is being guided to (outlined for them alone). */
	public static boolean highlighted(Entity entity) {
		return target != null && target.enabled() && target.entityId() >= 0 && entity.getId() == target.entityId();
	}

	public static void register() {
		KeyBindingHelper.registerKeyBinding(TOGGLE);
		ClientPlayNetworking.registerGlobalReceiver(NavTargetPayload.TYPE, (packet, player, sender) -> target = packet);
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> target = null);
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (TOGGLE.consumeClick()) {
				boolean on = !enabled();
				if (ClientPlayNetworking.canSend(NavTogglePayload.TYPE)) {
					ClientPlayNetworking.send(new NavTogglePayload(on));
				}
				// Show the change at once; the server's next update confirms it.
				target = new NavTargetPayload(on, target == null ? "" : target.name(), target == null ? "" : target.island(),
						target == null ? 0 : target.x(), target == null ? 0 : target.y(), target == null ? 0 : target.z(),
						on && target != null ? target.entityId() : -1);
				if (client.player != null) {
					client.player.displayClientMessage(Component.translatable(on ? "message.grandline.nav.on" : "message.grandline.nav.off")
							.withStyle(on ? ChatFormatting.GOLD : ChatFormatting.GRAY), true);
				}
			}
		});
	}
}
