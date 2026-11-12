package com.grandline.client.screen;

import com.grandline.client.ClientProfileCache;
import com.grandline.client.network.ClientNetworking;
import com.grandline.network.payload.ProfileSyncPayload;
import com.grandline.player.PlayerProfile;
import com.grandline.player.StatType;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Stat allocation screen. Buttons only send {@code AllocateStatPayload} intents; the values shown always come from the
 * server's latest sync, so the screen can never display a value the server did not accept.
 */
public final class StatsScreen extends Screen {
	private static final int[] AMOUNTS = {1, 5, 10};
	private static final int ROW_HEIGHT = 24;
	private static final int BUTTON_WIDTH = 30;

	private final List<StatButton> statButtons = new ArrayList<>();
	private int top;

	private record StatButton(Button button, StatType stat, int amount) {
	}

	public StatsScreen() {
		super(Component.translatable("screen.grandline.stats.title"));
	}

	@Override
	protected void init() {
		statButtons.clear();
		top = Math.max(30, height / 2 - (StatType.values().length * ROW_HEIGHT) / 2);
		int buttonsLeft = width / 2 + 92;

		for (StatType stat : StatType.values()) {
			int y = top + stat.ordinal() * ROW_HEIGHT;
			for (int i = 0; i < AMOUNTS.length; i++) {
				int amount = AMOUNTS[i];
				Button button = Button.builder(Component.literal("+" + amount), b -> ClientNetworking.requestStatAllocation(stat, amount))
						.bounds(buttonsLeft + i * (BUTTON_WIDTH + 2), y, BUTTON_WIDTH, 20)
						.build();
				statButtons.add(new StatButton(addRenderableWidget(button), stat, amount));
			}
		}

		addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
				.bounds(width / 2 - 50, top + StatType.values().length * ROW_HEIGHT + 10, 100, 20)
				.build());
		updateButtons();
	}

	@Override
	public void tick() {
		updateButtons();
	}

	private void updateButtons() {
		ProfileSyncPayload view = ClientProfileCache.view();
		for (StatButton entry : statButtons) {
			boolean allowed = view != null
					&& entry.amount() <= view.profile().unallocatedStatPoints()
					&& view.profile().stats().get(entry.stat()) + entry.amount() <= view.maxPointsPerStat();
			entry.button().active = allowed;
		}
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		renderBackground(graphics);
		graphics.drawCenteredString(font, title, width / 2, top - 26, 0xFFD27F);

		ProfileSyncPayload view = ClientProfileCache.view();
		if (view == null) {
			graphics.drawCenteredString(font, Component.translatable("screen.grandline.stats.waiting"), width / 2, top, 0xAAAAAA);
			super.render(graphics, mouseX, mouseY, partialTick);
			return;
		}

		PlayerProfile profile = view.profile();
		graphics.drawCenteredString(font, Component.translatable("screen.grandline.stats.points", profile.unallocatedStatPoints())
				.withStyle(profile.unallocatedStatPoints() > 0 ? ChatFormatting.GREEN : ChatFormatting.GRAY), width / 2, top - 14, 0xFFFFFF);

		int left = width / 2 - 190;
		for (StatType stat : StatType.values()) {
			int y = top + stat.ordinal() * ROW_HEIGHT + 6;
			graphics.drawString(font, Component.translatable(stat.translationKey()).withStyle(ChatFormatting.WHITE), left, y, 0xFFFFFF);
			graphics.drawString(font, Component.literal(profile.stats().get(stat) + " / " + view.maxPointsPerStat()), left + 52, y, 0xB0D8FF);
			graphics.drawString(font, effectText(stat, view), left + 118, y, stat.hasEffect() ? 0x9CE89C : 0x888888);
		}

		super.render(graphics, mouseX, mouseY, partialTick);
	}

	private static Component effectText(StatType stat, ProfileSyncPayload view) {
		return switch (stat) {
			case DEFENSE -> Component.translatable("stat.grandline.effect.health", format(view.bonusHealth()));
			case MELEE -> Component.translatable("stat.grandline.effect.melee", format(view.bonusMeleeDamage()));
			case SWORD -> Component.translatable("stat.grandline.effect.sword", format(view.bonusSwordDamage()));
			case GUN, FRUIT -> Component.translatable("stat.grandline.effect.none");
		};
	}

	private static String format(double value) {
		return String.format(Locale.ROOT, "%.2f", value);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
