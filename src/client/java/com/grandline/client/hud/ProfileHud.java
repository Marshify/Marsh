package com.grandline.client.hud;

import com.grandline.client.ClientProfileCache;
import com.grandline.client.GrandLineKeys;
import com.grandline.network.payload.ProfileSyncPayload;
import com.grandline.player.PlayerProfile;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Top-left HUD panel: level, experience bar, currency and a hint when stat points are waiting to be spent.
 * Draws only data synced from the server ({@link ClientProfileCache}); hidden with F1 and while the F3 screen is open.
 */
public final class ProfileHud {
	private static final int X = 4;
	private static final int Y = 4;
	private static final int BAR_WIDTH = 102;
	private static final int BAR_HEIGHT = 5;

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

		Font font = client.font;
		PlayerProfile profile = view.profile();
		int y = Y;

		graphics.fill(X - 2, Y - 2, X + BAR_WIDTH + 2, Y + (profile.unallocatedStatPoints() > 0 ? 42 : 32), 0x66000000);

		graphics.drawString(font, Component.translatable("hud.grandline.level", profile.level(), view.maxLevel())
				.withStyle(ChatFormatting.GOLD), X, y, 0xFFFFFF);
		y += 10;

		// Experience bar (full at the level cap).
		double progress = view.experienceToNextLevel() <= 0 ? 1.0
				: Math.min(1.0, (double) profile.experience() / view.experienceToNextLevel());
		graphics.fill(X, y, X + BAR_WIDTH, y + BAR_HEIGHT, 0xFF1B2A3A);
		graphics.fill(X, y, X + (int) Math.round(BAR_WIDTH * progress), y + BAR_HEIGHT, 0xFF3FB6FF);
		y += BAR_HEIGHT + 2;

		Component experience = view.experienceToNextLevel() <= 0
				? Component.translatable("hud.grandline.max_level")
				: Component.translatable("hud.grandline.experience", profile.experience(), view.experienceToNextLevel());
		graphics.drawString(font, experience, X, y, 0xB0D8FF);
		y += 10;

		graphics.drawString(font, Component.translatable("hud.grandline.currency", profile.currency()), X, y, 0xFFE07A);
		y += 10;

		if (profile.unallocatedStatPoints() > 0) {
			graphics.drawString(font, Component.translatable("hud.grandline.points_hint", profile.unallocatedStatPoints(),
					GrandLineKeys.STATS.getTranslatedKeyMessage()), X, y, 0x7CFC7C);
		}
	}
}
