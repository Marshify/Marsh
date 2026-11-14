package com.grandline.client.race;

import com.grandline.network.payload.RaceActionPayload;
import com.grandline.network.payload.RaceLookPayload;
import com.grandline.network.payload.RaceSyncPayload;
import com.grandline.race.Race;
import com.grandline.race.RaceData;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.entity.Entity;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;

/**
 * The client's copy of race state: this player's full progression (from the server, for the race menu and HUD) and
 * every visible player's race, stage and transformation (for cosmetics). Keys: R V3 ability, Y awaken, U race menu
 * (all rebindable in Controls).
 */
public final class ClientRace {
	public static final KeyMapping ABILITY = new KeyMapping("key.grandline.race_ability", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, "key.categories.grandline");
	public static final KeyMapping AWAKEN = new KeyMapping("key.grandline.race_awaken", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Y, "key.categories.grandline");
	public static final KeyMapping MENU = new KeyMapping("key.grandline.race_menu", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_U, "key.categories.grandline");

	public record Look(Race race, int stage, int mode) {
	}

	private static RaceData self;
	private static net.minecraft.nbt.CompoundTag view = new net.minecraft.nbt.CompoundTag();
	private static int v3Ticks, v4Ticks, readyTicks;
	private static final Map<Integer, Look> LOOKS = new HashMap<>();

	private ClientRace() {
	}

	public static RaceData self() {
		return self;
	}

	/** The server's latest menu view: "view" (requirement lines), "points" (free upgrade points). */
	public static net.minecraft.nbt.CompoundTag view() {
		return view;
	}

	public static int v3Ticks() {
		return v3Ticks;
	}

	public static int v4Ticks() {
		return v4Ticks;
	}

	public static int readyTicks() {
		return readyTicks;
	}

	/** The race look of a player entity (null if unknown). */
	public static Look look(Entity entity) {
		return LOOKS.get(entity.getId());
	}

	public static void register() {
		KeyBindingHelper.registerKeyBinding(ABILITY);
		KeyBindingHelper.registerKeyBinding(AWAKEN);
		KeyBindingHelper.registerKeyBinding(MENU);
		ClientPlayNetworking.registerGlobalReceiver(RaceSyncPayload.TYPE, (packet, player, sender) -> {
			self = RaceData.CODEC.parse(NbtOps.INSTANCE, packet.data()).result().orElse(self);
			v3Ticks = packet.v3Ticks();
			v4Ticks = packet.v4Ticks();
			readyTicks = packet.v3Ready();
			view = packet.data();
			if (view.contains("npc")) {
				Minecraft.getInstance().setScreen(new RaceScreen(view.getCompound("npc")));
			}
		});
		ClientPlayNetworking.registerGlobalReceiver(RaceLookPayload.TYPE, (packet, player, sender) ->
				LOOKS.put(packet.entityId(), new Look(Race.byId(packet.race()), packet.stage(), packet.mode())));
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			self = null;
			LOOKS.clear();
			v3Ticks = v4Ticks = readyTicks = 0;
		});
		ClientTickEvents.END_CLIENT_TICK.register(ClientRace::tick);
	}

	private static void tick(Minecraft client) {
		if (v3Ticks > 0) {
			v3Ticks--;
		}
		if (v4Ticks > 0) {
			v4Ticks--;
		}
		if (readyTicks > 0) {
			readyTicks--;
		}
		if (client.level != null && client.level.getGameTime() % 200 == 0) {
			LOOKS.keySet().removeIf(id -> client.level.getEntity(id) == null);
		}
		while (ABILITY.consumeClick()) {
			send(RaceActionPayload.V3, 0);
		}
		while (AWAKEN.consumeClick()) {
			send(RaceActionPayload.V4, 0);
		}
		while (MENU.consumeClick()) {
			if (client.screen == null && self != null) {
				client.setScreen(new RaceScreen(null));
			}
		}
	}

	public static void send(int action, int arg) {
		if (ClientPlayNetworking.canSend(RaceActionPayload.TYPE)) {
			ClientPlayNetworking.send(new RaceActionPayload(action, arg));
		}
	}
}
