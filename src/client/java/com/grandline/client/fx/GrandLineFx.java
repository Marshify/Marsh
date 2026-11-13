package com.grandline.client.fx;

import com.grandline.network.payload.FxEventPayload;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Client side of move visuals. The server sends each beat ({@link FxEventPayload}) on the tick it happens to every
 * player nearby; the fruit's effect class (e.g. {@link FlameFx}) turns it into shaped effects drawn with Grand Line's
 * own textures ({@link FxDraw}). Effects tied to a caster end when the caster dies, leaves or changes dimension.
 */
public final class GrandLineFx {
	/** An effect alive for a few ticks: ticked 20 times a second, drawn every frame. */
	abstract static class Effect {
		final int caster;
		int age;
		boolean done;

		Effect(int caster) {
			this.caster = caster;
		}

		/** Returns false when finished. */
		abstract boolean tick(ClientLevel level);

		abstract void render(FxDraw d, ClientLevel level);

		/** Whether the effect is held by its caster (and so ends when the caster is gone). */
		boolean followsCaster() {
			return caster >= 0;
		}

		/** The caster ended (death, logout, dimension change): stop now; effects may override to fade instead. */
		void cancel() {
			done = true;
		}

		Entity caster(ClientLevel level) {
			Entity e = caster < 0 ? null : level.getEntity(caster);
			return e != null && e.isAlive() && !e.isRemoved() ? e : null;
		}
	}

	/** An effect that travels and is ended by the server's impact beat. */
	interface Projectile {
		Vec3 position();

		void end(Vec3 at);
	}

	private static final List<Effect> EFFECTS = new ArrayList<>();
	private static final List<Effect> PENDING = new ArrayList<>();
	private static ClientLevel lastLevel;

	private GrandLineFx() {
	}

	public static void register() {
		ClientPlayNetworking.registerGlobalReceiver(FxEventPayload.TYPE, (packet, player, sender) -> handle(packet));
		ClientTickEvents.END_CLIENT_TICK.register(client -> tick());
		WorldRenderEvents.AFTER_TRANSLUCENT.register(GrandLineFx::render);
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> clearAll());
	}

	static void add(Effect effect) {
		PENDING.add(effect);
	}

	@SuppressWarnings("unchecked")
	static <T extends Effect> T find(Class<T> type, int caster) {
		for (List<Effect> list : List.of(EFFECTS, PENDING)) {
			for (Effect e : list) {
				if (type.isInstance(e) && e.caster == caster && !e.done) {
					return (T) e;
				}
			}
		}
		return null;
	}

	/** Ends the caster's projectile closest to the server's impact point. */
	static void endProjectile(int caster, Vec3 at) {
		Effect best = null;
		double bestD = Double.MAX_VALUE;
		for (List<Effect> list : List.of(EFFECTS, PENDING)) {
			for (Effect e : list) {
				if (e instanceof Projectile p && e.caster == caster && !e.done) {
					double dd = p.position().distanceToSqr(at);
					if (dd < bestD) {
						bestD = dd;
						best = e;
					}
				}
			}
		}
		if (best != null) {
			((Projectile) best).end(at);
		}
	}

	static void endWave(int caster, double distance) {
		FlameFx.Wave wave = find(FlameFx.Wave.class, caster);
		if (wave != null) {
			wave.stop(distance);
		}
	}

	static void endMarker(int caster) {
		FlameFx.TargetMarker marker = find(FlameFx.TargetMarker.class, caster);
		if (marker != null) {
			marker.done = true;
		}
	}

	private static void handle(FxEventPayload packet) {
		Minecraft mc = Minecraft.getInstance();
		ClientLevel level = mc.level;
		if (level == null) {
			return;
		}
		boolean own = mc.player != null && packet.caster() == mc.player.getId();
		// Settings: other players' move effects can be hidden (their poses and damage still happen).
		if (!own && !com.grandline.client.settings.ClientSettings.get().allyVfx && !packet.effect().equals("flight_end")
				&& !packet.effect().startsWith("boss_")) {
			return;
		}
		Entity caster = level.getEntity(packet.caster());
		if (FlameFx.handles(packet.effect())) {
			FlameFx.handle(packet, level, caster, own);
		} else if (SaberFx.handles(packet.effect())) {
			SaberFx.handle(packet, level, caster, own);
		}
	}

	private static void clearAll() {
		EFFECTS.clear();
		PENDING.clear();
		FxSprites.clear();
		MovePoses.clearAll();
	}

	private static void tick() {
		Minecraft mc = Minecraft.getInstance();
		ClientLevel level = mc.level;
		if (level != lastLevel) {
			clearAll(); // dimension change or new world: nothing carries over
			lastLevel = level;
		}
		if (level == null || mc.isPaused()) {
			return;
		}
		EFFECTS.addAll(PENDING);
		PENDING.clear();
		for (Iterator<Effect> it = EFFECTS.iterator(); it.hasNext(); ) {
			Effect e = it.next();
			if (!e.done && e.followsCaster() && e.caster(level) == null) {
				e.cancel();
			}
			if (e.done || !e.tick(level)) {
				it.remove();
			}
		}
		FxSprites.tick();
	}

	private static void render(WorldRenderContext context) {
		Minecraft mc = Minecraft.getInstance();
		ClientLevel level = mc.level;
		if (level == null || EFFECTS.isEmpty() && !FxSprites.any()) {
			return;
		}
		PoseStack pose = context.matrixStack();
		MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
		FxDraw draw = new FxDraw(pose, context.camera(), buffers, context.tickDelta());
		for (Effect e : EFFECTS) {
			if (!e.done) {
				e.render(draw, level);
			}
		}
		FxSprites.render(draw);
		buffers.endBatch();
	}

	// ------------------------------------------------------------------------------------------------ body points

	/** The casting hand: in first person in front of the camera, otherwise beside the shoulder. */
	static Vec3 handPosition(Entity caster, float partial, boolean left) {
		Minecraft mc = Minecraft.getInstance();
		Vec3 look = caster.getViewVector(partial);
		float yaw = Mth.lerp(partial, caster.yRotO, caster.getYRot()) * Mth.DEG_TO_RAD;
		Vec3 right = new Vec3(-Mth.cos(yaw), 0, -Mth.sin(yaw)).scale(left ? -1 : 1);
		if (caster == mc.player && mc.options.getCameraType().isFirstPerson()) {
			return caster.getEyePosition(partial).add(look.scale(1.1)).add(right.scale(0.42)).add(0, -0.38, 0);
		}
		Vec3 forward = new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw));
		double height = caster instanceof LivingEntity l && l.isCrouching() ? 1.0 : 1.25;
		return caster.getPosition(partial).add(0, height, 0).add(right.scale(0.38)).add(forward.scale(0.35));
	}

	static Vec3 shoulderPosition(Entity caster, float partial) {
		float yaw = Mth.lerp(partial, caster.yRotO, caster.getYRot()) * Mth.DEG_TO_RAD;
		Vec3 right = new Vec3(-Mth.cos(yaw), 0, -Mth.sin(yaw));
		return caster.getPosition(partial).add(0, 1.4, 0).add(right.scale(0.36));
	}
}
