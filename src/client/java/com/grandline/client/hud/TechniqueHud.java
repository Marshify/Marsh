package com.grandline.client.hud;

import com.grandline.GrandLineMod;
import com.grandline.client.technique.ClientTechniques;
import com.grandline.client.technique.TechniqueKeys;
import com.grandline.client.ui.MarshUi;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * A compact row of technique tiles above the left end of the hotbar: Dash (its cooldown, only while it runs), Flash Step
 * (key, cooldown, or a lock until bought), Aura (lit while on, with its stage) and Instinct (lit while on, dodge charges
 * and the next charge's progress). Locked Aura and Instinct tiles only appear once the technique is owned, so a new
 * player sees just Dash and the locked Flash Step.
 */
public final class TechniqueHud {
	private static final int TILE = 16;
	private static final ResourceLocation DASH = icon("dash"), FLASH = icon("flash_step"), AURA = icon("aura"), INSTINCT = icon("instinct"),
			LOCK = icon("lock");

	private TechniqueHud() {
	}

	private static ResourceLocation icon(String name) {
		return GrandLineMod.id("textures/gui/technique/" + name + ".png");
	}

	public static void register() {
		HudRenderCallback.EVENT.register(TechniqueHud::render);
	}

	private static void render(GuiGraphics g, float partial) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.options.hideGui || mc.player.isSpectator() || com.grandline.client.ClientProfileCache.view() == null) {
			return;
		}
		Font font = mc.font;
		var own = ClientTechniques.own();
		int y = g.guiHeight() - 50;
		int x = g.guiWidth() / 2 - 92;
		// Dash: always available; the tile only shows a sweep while it recharges.
		x = tile(g, font, x, y, DASH, true, false, TechniqueKeys.DASH, ClientTechniques.dashCooldown(), GrandLineMod.config().dashCooldownTicks, null);
		// Flash Step: locked until bought.
		x = tile(g, font, x, y, FLASH, own.flashStep(), false, TechniqueKeys.FLASH_STEP, ClientTechniques.flashCooldown(),
				GrandLineMod.config().flashStepCooldownTicks, null);
		if (own.aura()) {
			x = tile(g, font, x, y, AURA, true, ClientTechniques.auraOn(), TechniqueKeys.AURA, 0, 1,
					ClientTechniques.auraOn() ? String.valueOf(ClientTechniques.auraStage()) : null);
		}
		if (own.instinct()) {
			int max = ClientTechniques.maxDodges();
			int tx = x;
			x = tile(g, font, x, y, INSTINCT, true, ClientTechniques.instinctOn(), TechniqueKeys.INSTINCT, 0, 1, own.dodges() + "/" + max);
			// The next charge fills under the tile.
			if (own.dodges() < max) {
				int w = TILE;
				g.fill(tx, y + TILE + 1, tx + w, y + TILE + 3, 0xA0000000);
				g.fill(tx, y + TILE + 1, tx + Math.round(w * ClientTechniques.regenProgress()), y + TILE + 3, 0xFF9FD4FF);
			}
		}
	}

	/** One tile; returns the next tile's x. */
	private static int tile(GuiGraphics g, Font font, int x, int y, ResourceLocation icon, boolean unlocked, boolean active, KeyMapping key,
			int cooldown, int total, String badge) {
		MarshUi.abilitySlot(g, x - 2, y - 2, TILE + 4, active ? 1 : 0);
		if (active) {
			g.fill(x - 1, y - 1, x + TILE + 1, y + TILE + 1, 0x403FA8FF);
		}
		g.setColor(unlocked ? 1f : 0.45f, unlocked ? 1f : 0.45f, unlocked ? 1f : 0.5f, 1f);
		g.blit(icon, x, y, 0, 0, TILE, TILE, TILE, TILE);
		g.setColor(1f, 1f, 1f, 1f);
		if (!unlocked) {
			g.blit(LOCK, x + 7, y + 7, 0, 0, 9, 9, 9, 9);
		} else if (cooldown > 0 && total > 0) {
			float f = Mth.clamp(cooldown / (float) total, 0, 1);
			int h = Math.round(TILE * f);
			g.fill(x, y + TILE - h, x + TILE, y + TILE, 0xA0101418);
			String secs = cooldown >= 20 ? String.valueOf((cooldown + 19) / 20) : String.format(java.util.Locale.ROOT, "%.1f", cooldown / 20f);
			g.pose().pushPose();
			g.pose().translate(0, 0, 200);
			g.drawString(font, secs, x + TILE / 2 - font.width(secs) / 2, y + 4, 0xFFFFFFFF, true);
			g.pose().popPose();
		}
		// Key letter in the corner.
		String k = key.getTranslatedKeyMessage().getString();
		if (k.length() > 3) {
			k = k.substring(0, 3);
		}
		g.pose().pushPose();
		g.pose().translate(x - 1, y - 6, 210);
		g.pose().scale(0.6f, 0.6f, 1);
		g.drawString(font, Component.literal(k), 0, 0, 0xFFE8ECF2, true);
		g.pose().popPose();
		if (badge != null) {
			g.pose().pushPose();
			g.pose().translate(x + TILE - font.width(badge) * 0.65f + 1, y + TILE - 5, 210);
			g.pose().scale(0.65f, 0.65f, 1);
			g.drawString(font, badge, 0, 0, active ? 0xFFFFE07A : 0xFFC9D3E0, true);
			g.pose().popPose();
		}
		return x + TILE + 6;
	}
}
