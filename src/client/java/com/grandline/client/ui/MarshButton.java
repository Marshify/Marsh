package com.grandline.client.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/** A clickable Marsh UI sprite button. */
public class MarshButton extends AbstractButton {
	private final Runnable action;
	private MarshUi.Color color;

	public MarshButton(int x, int y, int w, int h, Component message, MarshUi.Color color, Runnable action) {
		super(x, y, w, h, message);
		this.color = color;
		this.action = action;
	}

	public void setColor(MarshUi.Color color) {
		this.color = color;
	}

	@Override
	public void onPress() {
		action.run();
	}

	@Override
	protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		MarshUi.State state = !active ? MarshUi.State.PRESSED : isHoveredOrFocused() ? MarshUi.State.HOVER : MarshUi.State.NORMAL;
		MarshUi.button(g, getX(), getY(), width, height, active ? color : MarshUi.Color.GRAY, state);
		var font = Minecraft.getInstance().font;
		MarshUi.outlined(g, font, getMessage(), getX() + width / 2 - font.width(getMessage()) / 2, getY() + (height - 8) / 2,
				active ? 0xFFFFFF : 0xC8C8C8);
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
		defaultButtonNarrationText(output);
	}
}
