package com.grandline.client.ui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Conversation-style dialog: a name plate and prompt at the bottom of the screen and a column of choices on the right.
 * Used for fruits ("What do you wish to do with this fruit?") and island NPCs.
 */
public class DialogScreen extends Screen {
	/** One choice. {@code action} runs on click; the dialog closes afterwards unless {@code keepOpen}. */
	public record Option(Component label, Runnable action, boolean keepOpen) {
		public static Option of(Component label, Runnable action) {
			return new Option(label, action, false);
		}
	}

	private final Component speaker;
	private final List<Component> lines;
	private final List<Option> options;

	public DialogScreen(Component speaker, List<Component> lines, List<Option> options) {
		super(speaker);
		this.speaker = speaker;
		this.lines = lines;
		this.options = options;
	}

	@Override
	protected void init() {
		int buttonWidth = Math.min(150, width / 3);
		int buttonHeight = 20;
		int x = width - buttonWidth - 12;
		int bottomOfButtons = height - 70;
		int y = bottomOfButtons - options.size() * (buttonHeight + 4);
		for (Option option : options) {
			addRenderableWidget(new GoldButton(x, y, buttonWidth, buttonHeight, option.label(), b -> {
				option.action().run();
				if (!option.keepOpen()) {
					onClose();
				}
			}, 0xFF2C3A52) {
				@Override
				protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
					// Dialog choices: dark plate with gold text and a speech-bubble marker.
					int bx = getX();
					int by = getY();
					int color = isHoveredOrFocused() ? 0xF03A4C6B : 0xF0202B3D;
					graphics.fill(bx - 1, by - 1, bx + width + 1, by + height + 1, GrandLineUi.BORDER);
					graphics.fill(bx, by, bx + width, by + height, color);
					graphics.fill(bx, by, bx + 3, by + height, GrandLineUi.GOLD);
					graphics.drawString(font, "»", bx + 8, by + 6, GrandLineUi.GOLD);
					graphics.drawString(font, getMessage(), bx + 20, by + 6, 0xFFFFFF);
				}
			});
			y += buttonHeight + 4;
		}
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		// Bottom conversation band.
		int bandTop = height - 62;
		graphics.fillGradient(0, bandTop - 20, width, height, 0x00000000, 0xE0000000);
		int plateWidth = Math.max(120, font.width(speaker) * 2 + 30);
		int plateX = width / 2 - plateWidth / 2;
		graphics.fill(plateX, bandTop, plateX + plateWidth, bandTop + 18, GrandLineUi.GOLD);
		graphics.fill(plateX, bandTop + 15, plateX + plateWidth, bandTop + 18, GrandLineUi.GOLD_DARK);
		GrandLineUi.scaled(graphics, font, speaker, width / 2f - font.width(speaker) * 0.65f, bandTop + 4, 1.3f, 0x1A1205, false);

		int y = bandTop + 26;
		for (Component line : lines) {
			graphics.drawCenteredString(font, line, width / 2, y, 0xFFFFFF);
			y += 12;
		}
		super.render(graphics, mouseX, mouseY, partialTick);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
