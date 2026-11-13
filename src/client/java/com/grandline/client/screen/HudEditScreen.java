package com.grandline.client.screen;

import com.grandline.client.hud.ProfileHud;
import com.grandline.client.settings.ClientSettings;
import com.grandline.client.ui.MarshButton;
import com.grandline.client.ui.MarshUi;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Drag HUD panels (status, move list, quest tracker) to new positions. Saved when you press Done. */
public final class HudEditScreen extends Screen {
	private final Screen parent;
	private String dragging;
	private double grabX;
	private double grabY;

	public HudEditScreen(Screen parent) {
		super(Component.translatable("screen.grandline.hud_edit.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		addRenderableWidget(new MarshButton(width / 2 - 50, height / 2 - 10, 100, 20, Component.translatable("screen.grandline.hud_edit.done"),
				MarshUi.Color.GREEN, this::onClose));
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		g.fill(0, 0, width, height, 0x60000000);
		ProfileHud.renderAll(g);
		g.pose().pushPose();
		g.pose().translate(0, 0, 500);
		for (String id : ProfileHud.ELEMENTS) {
			int[] b = ProfileHud.bounds(id);
			if (b == null) {
				continue;
			}
			boolean active = id.equals(dragging);
			int color = active ? 0xFFFFD24B : 0xC0FFFFFF;
			g.fill(b[0] - 2, b[1] - 2, b[0] + b[2] + 2, b[1] - 1, color);
			g.fill(b[0] - 2, b[1] + b[3] + 1, b[0] + b[2] + 2, b[1] + b[3] + 2, color);
			g.fill(b[0] - 2, b[1] - 2, b[0] - 1, b[1] + b[3] + 2, color);
			g.fill(b[0] + b[2] + 1, b[1] - 2, b[0] + b[2] + 2, b[1] + b[3] + 2, color);
		}
		g.drawCenteredString(font, Component.translatable("screen.grandline.hud_edit.hint"), width / 2, height / 2 - 24, 0xFFFFFF);
		g.pose().popPose();
		super.render(g, mouseX, mouseY, partialTick);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (super.mouseClicked(mouseX, mouseY, button)) {
			return true;
		}
		for (String id : ProfileHud.ELEMENTS) {
			int[] b = ProfileHud.bounds(id);
			if (b != null && mouseX >= b[0] && mouseX <= b[0] + b[2] && mouseY >= b[1] && mouseY <= b[1] + b[3]) {
				int[] offset = ClientSettings.get().offset(id);
				dragging = id;
				grabX = mouseX - offset[0];
				grabY = mouseY - offset[1];
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
		if (dragging != null) {
			ClientSettings.get().setOffset(dragging, (int) Math.round(mouseX - grabX), (int) Math.round(mouseY - grabY));
			return true;
		}
		return super.mouseDragged(mouseX, mouseY, button, dx, dy);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		dragging = null;
		return super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	public void onClose() {
		ClientSettings.get().save();
		minecraft.setScreen(parent);
	}
}
