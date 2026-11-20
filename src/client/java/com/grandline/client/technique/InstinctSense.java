package com.grandline.client.technique;

import com.grandline.GrandLineMod;
import com.grandline.entity.GrandLineEnemy;
import com.grandline.entity.Islander;
import com.grandline.entity.SaberBoss;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;

/**
 * Instinct's awareness, drawn for this player only: threats within the detection radius get an outline (visible
 * through walls, coloured by kind: hostile and enemy players red, bosses gold, allied players green) and a small label
 * with their name, level and health. Nothing is applied to the entities themselves, so nobody else sees it. Passive
 * animals, items, armour stands, islanders and shopkeepers are never marked.
 */
public final class InstinctSense {
	public static final int HOSTILE = 0xFF4A4A, BOSS = 0xFFC23A, ALLY = 0x5BE36B;
	private static final Map<Integer, Integer> MARKED = new HashMap<>();
	private static int pulseTicks;

	private InstinctSense() {
	}

	public static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(InstinctSense::tick);
		// LAST, not AFTER_TRANSLUCENT: text drawn there is lost under the later passes.
		WorldRenderEvents.LAST.register(InstinctSense::labels);
		HudRenderCallback.EVENT.register(InstinctSense::screenPulse);
	}

	public static void clear() {
		MARKED.clear();
	}

	/** The brief screen pulse when Instinct switches on. */
	public static void pulse() {
		pulseTicks = 10;
	}

	/** Whether this player's Instinct marks {@code entity} (used by the outline mixins). */
	public static boolean marked(Entity entity) {
		return !MARKED.isEmpty() && MARKED.containsKey(entity.getId());
	}

	public static int color(Entity entity) {
		return MARKED.getOrDefault(entity.getId(), HOSTILE);
	}

	/** The outline colour for an entity, or -1 if Instinct ignores it. */
	public static int classify(Entity e, Player self) {
		if (e == self || !e.isAlive() || !(e instanceof LivingEntity living) || living.isInvisible() && !(e instanceof Player)) {
			return -1;
		}
		if (e instanceof Islander) {
			return -1;
		}
		if (e instanceof SaberBoss) {
			return BOSS;
		}
		if (e instanceof Player other) {
			if (other.isSpectator()) {
				return -1;
			}
			var team = other.getTeam();
			boolean ally = team != null && team == self.getTeam() && !team.isAllowFriendlyFire();
			return ally ? ALLY : HOSTILE;
		}
		if (e instanceof GrandLineEnemy || e instanceof Enemy) {
			return HOSTILE;
		}
		return -1; // animals, golems, villagers, decorations
	}

	private static void tick(Minecraft mc) {
		if (pulseTicks > 0) {
			pulseTicks--;
		}
		if (mc.level == null || mc.player == null || !ClientTechniques.instinctOn()) {
			if (!MARKED.isEmpty()) {
				MARKED.clear();
			}
			return;
		}
		if (mc.player.tickCount % 4 != 0) {
			return;
		}
		double r = GrandLineMod.config().instinctDetectRadius;
		MARKED.clear();
		for (Entity e : mc.level.entitiesForRendering()) {
			if (e.distanceToSqr(mc.player) > r * r) {
				continue;
			}
			int c = classify(e, mc.player);
			if (c >= 0) {
				MARKED.put(e.getId(), c);
			}
		}
	}

	/** Name, level and health over each marked entity, readable through walls, facing the camera. */
	private static void labels(WorldRenderContext ctx) {
		Minecraft mc = Minecraft.getInstance();
		if (MARKED.isEmpty() || mc.level == null || mc.options.hideGui) {
			return;
		}
		Camera camera = ctx.camera();
		PoseStack pose = ctx.matrixStack();
		MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
		Font font = mc.font;
		float partial = ctx.tickDelta();
		for (Map.Entry<Integer, Integer> m : MARKED.entrySet()) {
			Entity e = mc.level.getEntity(m.getKey());
			if (!(e instanceof LivingEntity living)) {
				continue;
			}
			Vec3 at = e.getPosition(partial).add(0, e.getBbHeight() + 0.75, 0).subtract(camera.getPosition());
			double dist = Math.sqrt(at.lengthSqr());
			pose.pushPose();
			pose.translate(at.x, at.y, at.z);
			pose.mulPose(camera.rotation());
			float scale = (float) (0.032 * Math.max(1, dist / 10));
			pose.scale(-scale, -scale, scale);
			Matrix4f matrix = pose.last().pose();
			Component name = e.hasCustomName() ? e.getCustomName() : e.getDisplayName();
			String health = String.format(java.util.Locale.ROOT, "%.0f / %.0f", Math.max(0, living.getHealth()), living.getMaxHealth());
			int color = m.getValue();
			draw(font, name, -font.width(name) / 2f, -10, 0xFF000000 | color, matrix, buffers);
			Component hp = Component.literal("❤ " + health);
			draw(font, hp, -font.width(hp) / 2f, 0, 0xFFE8ECF2, matrix, buffers);
			pose.popPose();
		}
		buffers.endBatch();
	}

	private static void draw(Font font, Component text, float x, float y, int color, Matrix4f matrix, MultiBufferSource buffers) {
		font.drawInBatch(text, x, y, color, false, matrix, buffers, Font.DisplayMode.SEE_THROUGH, 0x40000000, LightTexture.FULL_BRIGHT);
	}

	/** A soft pale vignette that fades over half a second when Instinct switches on. */
	private static void screenPulse(GuiGraphics g, float partial) {
		if (pulseTicks <= 0) {
			return;
		}
		float t = (pulseTicks - partial) / 10f;
		int a = (int) (Math.max(0, t) * 70);
		int w = g.guiWidth(), h = g.guiHeight();
		int edge = Math.max(12, w / 14);
		int c = (a << 24) | 0xBFE4FF;
		g.fillGradient(0, 0, w, edge, c, 0x00BFE4FF);
		g.fillGradient(0, h - edge, w, h, 0x00BFE4FF, c);
		for (int i = 0; i < edge; i += 2) {
			int ai = (int) (a * (1 - i / (float) edge));
			g.fill(i, 0, i + 2, h, (ai << 24) | 0xBFE4FF);
			g.fill(w - i - 2, 0, w - i, h, (ai << 24) | 0xBFE4FF);
		}
	}
}
