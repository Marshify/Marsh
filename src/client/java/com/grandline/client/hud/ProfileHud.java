package com.grandline.client.hud;

import com.grandline.client.ClientProfileCache;
import com.grandline.client.GrandLineKeys;
import com.grandline.combat.ability.AbilityDef;
import com.grandline.combat.ability.Power;
import com.grandline.combat.ability.PowerSource;
import com.grandline.combat.ability.Powers;
import com.grandline.network.payload.ProfileSyncPayload;
import com.grandline.player.PlayerProfile;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Grand Line HUD. Bottom-left: currency, level, XP bar, Health and Energy bars. Bottom-right: the active power with
 * its mastery bar and the four moves (mastery requirement, key, live cooldown). Draws only server-synced data.
 * Nautical navy/gold styling. Hidden with F1 and while the F3 screen is open.
 */
public final class ProfileHud {
	private static final int NAVY = 0xC0101C2E;
	private static final int NAVY_LIGHT = 0xE0233A57;
	private static final int GOLD = 0xFFE8B84A;
	private static final int PANEL_BORDER = 0xFF0A1220;
	private static final NumberFormat NUMBERS = NumberFormat.getIntegerInstance(Locale.US);

	private ProfileHud() {
	}

	public static void register() {
		HudRenderCallback.EVENT.register((graphics, tickDelta) -> render(graphics));
	}

	private static void render(GuiGraphics graphics) {
		Minecraft client = Minecraft.getInstance();
		ProfileSyncPayload view = ClientProfileCache.view();
		if (view == null || client.player == null || client.options.hideGui || client.options.renderDebug) {
			return;
		}

		renderStatus(graphics, client, view);
		renderPowerPanel(graphics, client, view);
	}

	/** Bottom-left status block. */
	private static void renderStatus(GuiGraphics graphics, Minecraft client, ProfileSyncPayload view) {
		Font font = client.font;
		PlayerProfile profile = view.profile();
		int width = 124;
		int x = 6;
		int bottom = graphics.guiHeight() - 6;

		int energyY = bottom - 12;
		int healthY = energyY - 14;
		int xpY = healthY - 8;
		int levelY = xpY - 19;
		int currencyY = levelY - 13;

		if (profile.unallocatedStatPoints() > 0) {
			graphics.drawString(font, Component.translatable("hud.grandline.points_hint", profile.unallocatedStatPoints(),
					GrandLineKeys.STATS.getTranslatedKeyMessage()), x, currencyY - 11, 0x7CFC7C);
		}

		graphics.drawString(font, "$ " + NUMBERS.format(profile.currency()), x, currencyY, 0x6BE36B);
		drawScaled(graphics, font, Component.translatable("hud.grandline.level_big", profile.level()), x, levelY, 2.0f, 0xFFFFFF);

		// XP bar with text.
		double xpProgress = view.experienceToNextLevel() <= 0 ? 1.0 : Math.min(1.0, (double) profile.experience() / view.experienceToNextLevel());
		bar(graphics, x, xpY, width, 6, xpProgress, 0xFF9B59D6, 0xFF2A1A3A);
		Component xpText = view.experienceToNextLevel() <= 0 ? Component.translatable("hud.grandline.max_level")
				: Component.literal(NUMBERS.format(profile.experience()) + " / " + NUMBERS.format(view.experienceToNextLevel()));
		drawScaled(graphics, font, xpText, x + width / 2f - font.width(xpText) * 0.35f, xpY, 0.7f, 0xFFFFFF);

		float health = client.player.getHealth();
		float maxHealth = client.player.getMaxHealth();
		labelledBar(graphics, font, x, healthY, width, Component.translatable("hud.grandline.health"),
				Math.round(health) + "/" + Math.round(maxHealth), maxHealth <= 0 ? 0 : health / maxHealth, 0xFF3FBF4F, 0xFF1D3B22);

		float energy = ClientProfileCache.energy();
		float maxEnergy = ClientProfileCache.maxEnergy();
		labelledBar(graphics, font, x, energyY, width, Component.translatable("hud.grandline.energy"),
				Math.round(energy) + "/" + Math.round(maxEnergy), maxEnergy <= 0 ? 0 : energy / maxEnergy, 0xFF3FA9E8, 0xFF16304A);
	}

	/** Bottom-right power panel. */
	private static void renderPowerPanel(GuiGraphics graphics, Minecraft client, ProfileSyncPayload view) {
		Font font = client.font;
		PlayerProfile profile = view.profile();
		PowerSource source = GrandLineKeys.activeSource();
		Power power = source == PowerSource.FRUIT ? Powers.get(profile.powers().fruit()) : Powers.get(profile.powers().fightingStyle());
		if (power == null) {
			return;
		}

		int width = 128;
		int rowHeight = 17;
		int x = graphics.guiWidth() - width - 6;
		int movesHeight = rowHeight * power.abilities().size();
		int bottom = graphics.guiHeight() - 6;
		int movesTop = bottom - movesHeight;
		int titleTop = movesTop - 16;
		int masteryTop = titleTop - 20;

		long masteryXp = profile.powers().masteryXp(power.id().toString());
		int masteryLevel = view.masteryExperiencePerLevel() <= 0 ? 0 : (int) Math.min(view.maxMastery(), masteryXp / view.masteryExperiencePerLevel());
		double masteryProgress = masteryLevel >= view.maxMastery() ? 1.0
				: (double) (masteryXp % view.masteryExperiencePerLevel()) / view.masteryExperiencePerLevel();

		Component masteryText = Component.translatable("hud.grandline.mastery", masteryLevel);
		drawScaled(graphics, font, masteryText, x + width / 2f - font.width(masteryText) * 0.75f, masteryTop, 1.5f, 0xFFFFFF);
		bar(graphics, x, masteryTop + 13, width, 4, masteryProgress, power.color() | 0xFF000000, 0xFF1A1A1A);

		// Title plate with the power name and a hint for switching.
		graphics.fill(x - 1, titleTop - 1, x + width + 1, movesTop + movesHeight + 1, PANEL_BORDER);
		graphics.fill(x, titleTop, x + width, titleTop + 15, NAVY_LIGHT);
		graphics.fill(x, titleTop + 14, x + width, titleTop + 15, power.color() | 0xFF000000);
		graphics.drawString(font, Component.translatable(power.translationKey()), x + 4, titleTop + 4, GOLD);
		Component switchHint = Component.literal("[" + GrandLineKeys.SWITCH_POWER.getTranslatedKeyMessage().getString() + "]");
		graphics.drawString(font, switchHint, x + width - font.width(switchHint) - 3, titleTop + 4, 0x8FA6C1);

		for (int slot = 0; slot < power.abilities().size(); slot++) {
			AbilityDef ability = power.abilities().get(slot);
			int y = movesTop + slot * rowHeight;
			boolean unlocked = masteryLevel >= ability.requiredMastery();
			int cooldown = ClientProfileCache.remainingCooldownTicks(power.abilityId(ability));

			graphics.fill(x, y, x + width, y + rowHeight - 1, NAVY);
			if (cooldown > 0 && ability.cooldownTicks() > 0) {
				int filled = (int) Math.round(width * (double) cooldown / ability.cooldownTicks());
				graphics.fill(x, y, x + filled, y + rowHeight - 1, 0x90000000);
			}

			int nameColor = !unlocked ? 0x6E7785 : cooldown > 0 ? 0x9AA7B8 : 0xFFFFFF;
			graphics.drawString(font, Component.translatable(ability.translationKey()), x + 4, y + 4, nameColor);

			String key = "[" + GrandLineKeys.MOVES[slot].getTranslatedKeyMessage().getString() + "]";
			String right = cooldown > 0 ? String.format(Locale.ROOT, "%.1fs", cooldown / 20.0) : key;
			graphics.drawString(font, right, x + width - font.width(right) - 3, y + 4, cooldown > 0 ? 0xFF8A65 : GOLD);

			Component mastery = Component.translatable("hud.grandline.move_mastery", ability.requiredMastery());
			drawScaled(graphics, font, mastery, x + width - 3 - font.width(mastery) * 0.6f - font.width(right) - 4, y + 5, 0.6f,
					unlocked ? 0xA9C4E0 : 0xD46A6A);
		}
	}

	private static void labelledBar(GuiGraphics graphics, Font font, int x, int y, int width, Component label, String value,
			double progress, int color, int background) {
		bar(graphics, x, y, width, 11, progress, color, background);
		graphics.drawString(font, label, x + 3, y + 2, 0xFFFFFF);
		graphics.drawString(font, value, x + width - font.width(value) - 3, y + 2, 0xFFFFFF);
	}

	private static void bar(GuiGraphics graphics, int x, int y, int width, int height, double progress, int color, int background) {
		graphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, PANEL_BORDER);
		graphics.fill(x, y, x + width, y + height, background);
		graphics.fill(x, y, x + (int) Math.round(width * Math.max(0, Math.min(1, progress))), y + height, color);
		graphics.fill(x, y, x + width, y + 1, 0x30FFFFFF);
	}

	private static void drawScaled(GuiGraphics graphics, Font font, Component text, float x, float y, float scale, int color) {
		graphics.pose().pushPose();
		graphics.pose().translate(x, y, 0);
		graphics.pose().scale(scale, scale, 1);
		graphics.drawString(font, text, 0, 0, color);
		graphics.pose().popPose();
	}
}
