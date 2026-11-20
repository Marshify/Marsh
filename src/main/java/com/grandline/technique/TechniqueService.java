package com.grandline.technique;

import com.grandline.GrandLineMod;
import com.grandline.combat.EnergyService;
import com.grandline.combat.fx.FxEvents;
import com.grandline.data.ModAttachments;
import com.grandline.gear.sword.Stuns;
import com.grandline.network.payload.TechniqueLookPayload;
import com.grandline.network.payload.TechniqueSyncPayload;
import com.grandline.player.PlayerProfileService;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.UnaryOperator;

/**
 * Dash, Flash Step, Aura and Instinct on the server: ownership and progression (persistent, see {@link Techniques}),
 * the on/off state of Aura and Instinct, cooldowns, dodge-charge regeneration, and keeping the owner's HUD and every
 * nearby player's view (Aura coating) in sync. Clients only ask; every check happens here.
 */
public final class TechniqueService {
	/** Per-player state that does not need saving: toggles start off on every login. */
	static final class Runtime {
		boolean auraOn;
		boolean instinctOn;
		long dashReadyAt;
		long flashReadyAt;
		long lastAuraXpTick = -1000;
		int lastStage = -1;
		boolean inTutorial;
		int tutorial;
	}

	private static final Map<UUID, Runtime> RUNTIME = new HashMap<>();

	private TechniqueService() {
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(TechniqueService::tick);
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			RUNTIME.remove(handler.getPlayer().getUUID());
			sync(handler.getPlayer());
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> RUNTIME.remove(handler.getPlayer().getUUID()));
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> RUNTIME.clear());
		EntityTrackingEvents.START_TRACKING.register((entity, viewer) -> {
			if (entity instanceof ServerPlayer tracked && ServerPlayNetworking.canSend(viewer, TechniqueLookPayload.TYPE)) {
				ServerPlayNetworking.send(viewer, look(tracked));
			}
		});
		Dodges.register();
	}

	// ------------------------------------------------------------------------------------------------ state

	public static Techniques get(ServerPlayer player) {
		Techniques t = player.getAttached(ModAttachments.TECHNIQUES);
		return t == null ? Techniques.NONE : t;
	}

	public static Techniques update(ServerPlayer player, UnaryOperator<Techniques> op) {
		Techniques t = op.apply(get(player));
		player.setAttached(ModAttachments.TECHNIQUES, t);
		return t;
	}

	static Runtime runtime(ServerPlayer player) {
		return RUNTIME.computeIfAbsent(player.getUUID(), id -> new Runtime());
	}

	public static boolean auraOn(ServerPlayer player) {
		Runtime r = RUNTIME.get(player.getUUID());
		return r != null && r.auraOn && get(player).aura();
	}

	public static boolean instinctOn(ServerPlayer player) {
		Runtime r = RUNTIME.get(player.getUUID());
		return r != null && r.instinctOn && get(player).instinct();
	}

	public static int auraStage(ServerPlayer player) {
		return TechniqueRules.auraStage(get(player).auraXp());
	}

	// ------------------------------------------------------------------------------------------------ requests

	/** Client request ids (see TechniqueActionPayload). */
	public static final int DASH = 0, FLASH_STEP = 1, TOGGLE_AURA = 2, TOGGLE_INSTINCT = 3;

	/** Handles a request. Returns a result key (for tests): "ok" or why not. */
	public static String handle(ServerPlayer player, int action, float forward, float strafe) {
		if (!player.isAlive() || player.isSpectator()) {
			return "not_allowed";
		}
		return switch (action) {
			case DASH -> Dash.perform(player, forward, strafe);
			case FLASH_STEP -> FlashStep.perform(player);
			case TOGGLE_AURA -> toggleAura(player);
			case TOGGLE_INSTINCT -> toggleInstinct(player);
			default -> "not_allowed";
		};
	}

	/** Whether the player can move by themselves right now (stuns stop Dash and Flash Step). */
	static boolean immobilized(ServerPlayer player) {
		return Stuns.stunned(player) || player.isPassenger() || player.isSleeping();
	}

	static void tell(ServerPlayer player, String key, Object... args) {
		player.displayClientMessage(Component.translatable(key, args).withStyle(ChatFormatting.RED), true);
	}

	public static String toggleAura(ServerPlayer player) {
		if (!get(player).aura()) {
			tell(player, "message.grandline.technique.not_learned", Component.translatable("technique.grandline.aura"));
			return "not_owned";
		}
		Runtime r = runtime(player);
		r.auraOn = !r.auraOn;
		Vec3 at = player.position().add(0, 1.0, 0);
		FxEvents.send(player, r.auraOn ? "tq:aura_on" : "tq:aura_off", at, Vec3.ZERO, 0, 0, auraStage(player));
		sound(player, r.auraOn ? SoundEvents.RESPAWN_ANCHOR_CHARGE : SoundEvents.BEACON_DEACTIVATE, 0.8f, r.auraOn ? 0.6f : 1.4f);
		if (r.auraOn) {
			sound(player, SoundEvents.ARMOR_EQUIP_NETHERITE, 0.9f, 0.7f);
		}
		broadcastLook(player);
		sync(player);
		return "ok";
	}

	public static String toggleInstinct(ServerPlayer player) {
		if (!get(player).instinct()) {
			tell(player, "message.grandline.technique.not_learned", Component.translatable("technique.grandline.instinct"));
			return "not_owned";
		}
		Runtime r = runtime(player);
		r.instinctOn = !r.instinctOn;
		FxEvents.send(player, r.instinctOn ? "tq:instinct_on" : "tq:instinct_off", player.position().add(0, 0.1, 0), Vec3.ZERO, 0, 0, 0);
		sound(player, r.instinctOn ? SoundEvents.AMETHYST_BLOCK_RESONATE : SoundEvents.AMETHYST_BLOCK_STEP, 1.0f, r.instinctOn ? 1.6f : 0.8f);
		if (r.instinctOn) {
			sound(player, SoundEvents.BEACON_POWER_SELECT, 0.5f, 1.9f);
		}
		broadcastLook(player);
		sync(player);
		return "ok";
	}

	static void sound(ServerPlayer player, SoundEvent sound, float volume, float pitch) {
		player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
	}

	// ------------------------------------------------------------------------------------------------ progression

	/** Adds Aura XP; a new stage is announced and shown on every viewer. */
	public static void addAuraXp(ServerPlayer player, long xp) {
		int before = auraStage(player);
		update(player, t -> t.withAuraXp(t.auraXp() + xp));
		int after = auraStage(player);
		if (after > before) {
			player.displayClientMessage(Component.translatable("message.grandline.aura.stage_up", after).withStyle(ChatFormatting.DARK_AQUA), false);
			FxEvents.send(player, "tq:aura_on", player.position().add(0, 1.0, 0), Vec3.ZERO, 0, 0, after);
			sound(player, SoundEvents.PLAYER_LEVELUP, 0.7f, 0.6f);
			broadcastLook(player);
		}
		sync(player);
	}

	/** Grants Flash Step, Aura or Instinct (purchases, commands, tests). New Instinct starts with full charges. */
	public static void grant(ServerPlayer player, String technique) {
		switch (technique) {
			case "flash_step" -> update(player, t -> t.withFlashStep(true));
			case "aura" -> update(player, t -> t.withAura(true));
			case "instinct" -> update(player, t -> t.instinct() ? t
					: t.withInstinct(true).withDodges(TechniqueRules.maxDodges(t.instinctXp()), 0));
			default -> {
				return;
			}
		}
		sync(player);
	}

	/** One server tick: dodge charges come back one at a time, only while the player is online (on or off). */
	private static void tick(MinecraftServer server) {
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			Techniques t = get(player);
			if (!t.instinct()) {
				continue;
			}
			int max = TechniqueRules.maxDodges(t.instinctXp());
			if (t.dodges() >= max) {
				if (t.regenTicks() != 0) {
					update(player, x -> x.withDodges(x.dodges(), 0));
				}
				continue;
			}
			int regen = t.regenTicks() + 1;
			if (regen >= TechniqueRules.regenTicks()) {
				update(player, x -> x.withDodges(Math.min(max, x.dodges() + 1), 0));
				sync(player);
			} else {
				player.setAttached(ModAttachments.TECHNIQUES, t.withDodges(t.dodges(), regen));
			}
		}
	}

	// ------------------------------------------------------------------------------------------------ sync

	/** Sends the owner everything their HUD and trainers show (ownership, XP, toggles, charges, cooldowns). */
	public static void sync(ServerPlayer player) {
		if (!ServerPlayNetworking.canSend(player, TechniqueSyncPayload.TYPE)) {
			return;
		}
		Runtime r = runtime(player);
		long now = player.serverLevel().getGameTime();
		ServerPlayNetworking.send(player, new TechniqueSyncPayload(get(player), r.auraOn, r.instinctOn,
				(int) Math.max(0, r.dashReadyAt - now), (int) Math.max(0, r.flashReadyAt - now)));
	}

	static TechniqueLookPayload look(ServerPlayer player) {
		return new TechniqueLookPayload(player.getId(), auraOn(player) ? auraStage(player) : -1, instinctOn(player));
	}

	/** Tells the player and everyone who can see them how they look (Aura coating stage, or none). */
	public static void broadcastLook(ServerPlayer player) {
		TechniqueLookPayload payload = look(player);
		if (ServerPlayNetworking.canSend(player, TechniqueLookPayload.TYPE)) {
			ServerPlayNetworking.send(player, payload);
		}
		for (ServerPlayer viewer : PlayerLookup.tracking(player)) {
			if (viewer != player && ServerPlayNetworking.canSend(viewer, TechniqueLookPayload.TYPE)) {
				ServerPlayNetworking.send(viewer, payload);
			}
		}
	}

	/** Forgets toggles and cooldowns (tests). */
	public static void reset(ServerPlayer player) {
		RUNTIME.remove(player.getUUID());
		player.setAttached(ModAttachments.TECHNIQUES, Techniques.NONE);
	}

	static int level(ServerPlayer player) {
		return PlayerProfileService.get(player).level();
	}

	static long now(ServerPlayer player) {
		return player.serverLevel().getGameTime();
	}

	static com.grandline.config.GrandLineConfig config() {
		return GrandLineMod.config();
	}
}
