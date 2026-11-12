package com.grandline.client.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** Grand Line styled button: gold face with a darker base, grey when disabled, brighter on hover. */
public class GoldButton extends Button {
	private final int face;

	public GoldButton(int x, int y, int width, int height, Component message, OnPress onPress) {
		this(x, y, width, height, message, onPress, 0xFFE8B84A);
	}

	public GoldButton(int x, int y, int width, int height, Component message, OnPress onPress, int face) {
		super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
		this.face = face;
	}

	@Override
	protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		int x = getX();
		int y = getY();
		int color = !active ? 0xFF5A6270 : isHoveredOrFocused() ? GrandLineUi.darken(face, 1.0f) | 0x00202020 : face;
		graphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, GrandLineUi.BORDER);
		graphics.fill(x, y, x + width, y + height, GrandLineUi.darken(color, 0.7f));
		graphics.fill(x, y, x + width, y + height - 2, color);
		graphics.fill(x, y, x + width, y + 1, 0x40FFFFFF);
		int textColor = active ? 0x1A1205 : 0xC0C4CC;
		var font = Minecraft.getInstance().font;
		graphics.drawString(font, getMessage(), x + (width - font.width(getMessage())) / 2, y + (height - 9) / 2, textColor, false);
	}
}
