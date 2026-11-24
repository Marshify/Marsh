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
		// Wide enough for the longest choice (up to half the screen), never narrower than before.
		int longest = 0;
		for (Option option : options) {
			longest = Math.max(longest, font.width(option.label()));
		}
		// As wide as the longest choice needs (up to the screen's width), and a choice still too long wraps onto more lines.
		int buttonWidth = Math.max(Math.min(150, width / 3), Math.min(longest + 28, width - 24));
		int x = width - buttonWidth - 12;
		int textWidth = buttonWidth - 26;
		int total = 0;
		for (Option option : options) {
			total += font.split(option.label(), textWidth).size() * 10 + 10 + 4;
		}
		int y = Math.max(8, Math.min(height - 140 - total, height / 2 - total / 2 - 30));
		for (Option option : options) {
			java.util.List<net.minecraft.util.FormattedCharSequence> lines = font.split(option.label(), textWidth);
			int buttonHeight = lines.size() * 10 + 10;
			addRenderableWidget(new GoldButton(x, y, buttonWidth, buttonHeight, option.label(), b -> {
				option.action().run();
				if (!option.keepOpen()) {
					onClose();
				}
			}, 0xFF2C3A52) {
				@Override
				protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
					// Dialog choices: dark plate with gold text and a speech-bubble marker; long text wraps.
					int bx = getX();
					int by = getY();
					int color = isHoveredOrFocused() ? 0xF03A4C6B : 0xF0202B3D;
					graphics.fill(bx - 1, by - 1, bx + width + 1, by + height + 1, GrandLineUi.BORDER);
					graphics.fill(bx, by, bx + width, by + height, color);
					graphics.fill(bx, by, bx + 3, by + height, GrandLineUi.GOLD);
					graphics.drawString(font, "»", bx + 8, by + 6, GrandLineUi.GOLD);
					int ly = by + 6;
					for (net.minecraft.util.FormattedCharSequence line : lines) {
						graphics.drawString(font, line, bx + 20, ly, 0xFFFFFF);
						ly += 10;
					}
				}
			});
			y += buttonHeight + 4;
		}
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		// Bottom conversation band.
		int bandTop = height - 118; // well above the hotbar so it's easy to notice
		graphics.fillGradient(0, bandTop - 20, width, bandTop + 60, 0x00000000, 0xE0000000);
		graphics.fillGradient(0, bandTop + 60, width, height, 0xE0000000, 0x00000000);
		int plateWidth = Math.max(120, font.width(speaker) * 2 + 30);
		int plateX = width / 2 - plateWidth / 2;
		graphics.fill(plateX, bandTop, plateX + plateWidth, bandTop + 18, GrandLineUi.GOLD);
		graphics.fill(plateX, bandTop + 15, plateX + plateWidth, bandTop + 18, GrandLineUi.GOLD_DARK);
		GrandLineUi.scaled(graphics, font, speaker, width / 2f - font.width(speaker) * 0.65f, bandTop + 4, 1.3f, 0x1A1205, false);

		int y = bandTop + 26;
		int wrap = Math.min(width - 40, 460); // long lines wrap instead of running off the screen
		for (Component line : lines) {
			for (net.minecraft.util.FormattedCharSequence part : font.split(line, wrap)) {
				graphics.drawString(font, part, width / 2 - font.width(part) / 2, y, 0xFFFFFF);
				y += 11;
			}
			y += 1;
		}
		super.render(graphics, mouseX, mouseY, partialTick);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
