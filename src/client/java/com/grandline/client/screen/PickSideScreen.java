package com.grandline.client.screen;

import com.grandline.client.network.ClientNetworking;
import com.grandline.client.ui.MarshUi;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Pick a Side: Pirates or Marines, shown every time you join (and from the Menu). Two big panels with an emblem and
 * the side's rules; click one to join. Before your first choice the screen cannot be closed without picking.
 */
public final class PickSideScreen extends Screen {
	private final int current;
	private final boolean required;

	/** @param current the side the server has saved (-1 = none); {@code required}: on join with no side yet. */
	public PickSideScreen(int current, boolean required) {
		super(Component.translatable("screen.grandline.side.title"));
		this.current = current;
		this.required = required;
	}

	private int[] panel(int side) {
		int w = Math.min(170, width / 2 - 30);
		int h = Math.min(190, height - 90);
		int gap = 40;
		int x = width / 2 + (side == 0 ? -gap / 2 - w : gap / 2);
		int y = height / 2 - h / 2 + 14;
		return new int[]{x, y, w, h};
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		g.fillGradient(0, 0, width, height, 0x90000814, 0xB0000814);
		// Title strip.
		int stripW = Math.min(width - 40, 360);
		g.fill(width / 2 - stripW / 2, 18, width / 2 + stripW / 2, 46, 0xD0000000);
		g.pose().pushPose();
		g.pose().translate(width / 2f - font.width(title) * 1.25f, 25, 0);
		g.pose().scale(2.5f, 2.5f, 1);
		g.drawString(font, title, 0, 0, 0xFFFFFF, true);
		g.pose().popPose();
		for (int side = 0; side < 2; side++) {
			drawPanel(g, side, mouseX, mouseY);
		}
		g.pose().pushPose();
		g.pose().translate(width / 2f - font.width("VS") * 1.5f, panel(0)[1] + 40, 0);
		g.pose().scale(3f, 3f, 1);
		g.drawString(font, "VS", 0, 0, 0xFFFFFF, true);
		g.pose().popPose();
		if (current >= 0) {
			Component hint = Component.translatable("screen.grandline.side.current",
					Component.translatable(current == 0 ? "team.grandline.pirates" : "team.grandline.marines"));
			g.drawCenteredString(font, hint, width / 2, height - 22, 0xC9D3E0);
		}
		super.render(g, mouseX, mouseY, partialTick);
	}

	private void drawPanel(GuiGraphics g, int side, int mouseX, int mouseY) {
		int[] b = panel(side);
		boolean over = mouseX >= b[0] && mouseX < b[0] + b[2] && mouseY >= b[1] && mouseY < b[1] + b[3];
		int lift = over ? -3 : 0;
		int y = b[1] + lift;
		MarshUi.row(g, b[0], y, b[2], b[3], side == 0 ? MarshUi.Row.MELEE : MarshUi.Row.DEFENSE);
		int border = current == side ? 0xFFFFD24B : over ? 0xFFFFFFFF : side == 0 ? 0xFFB02020 : 0xFF2050C0;
		g.fill(b[0] - 2, y - 2, b[0] + b[2] + 2, y, border);
		g.fill(b[0] - 2, y + b[3], b[0] + b[2] + 2, y + b[3] + 2, border);
		g.fill(b[0] - 2, y, b[0], y + b[3], border);
		g.fill(b[0] + b[2], y, b[0] + b[2] + 2, y + b[3], border);
		// Emblem.
		ItemStack emblem = new ItemStack(side == 0 ? Items.SKELETON_SKULL : Items.SHIELD);
		g.pose().pushPose();
		g.pose().translate(b[0] + b[2] / 2f - 24, y + 14, 0);
		g.pose().scale(3f, 3f, 1);
		g.renderItem(emblem, 0, 0);
		g.pose().popPose();
		// Name banner.
		Component name = Component.translatable(side == 0 ? "team.grandline.pirates" : "team.grandline.marines");
		g.fill(b[0], y + 72, b[0] + b[2], y + 96, side == 0 ? 0xF0A01818 : 0xF01848B0);
		g.pose().pushPose();
		g.pose().translate(b[0] + b[2] / 2f - font.width(name), y + 76, 0);
		g.pose().scale(2f, 2f, 1);
		g.drawString(font, name, 0, 0, 0xFFFFFF, true);
		g.pose().popPose();
		// Rules.
		String base = "screen.grandline.side." + (side == 0 ? "pirates" : "marines") + ".line";
		int ly = y + 106;
		for (int i = 0; i < 3; i++) {
			Component line = Component.translatable(base + (i + 1));
			for (var part : font.split(line, b[2] - 12)) {
				g.drawCenteredString(font, part, b[0] + b[2] / 2, ly, 0xFFFFFF);
				ly += 10;
			}
			ly += 8;
		}
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		for (int side = 0; side < 2; side++) {
			int[] b = panel(side);
			if (button == 0 && mouseX >= b[0] && mouseX < b[0] + b[2] && mouseY >= b[1] && mouseY < b[1] + b[3]) {
				ClientNetworking.requestChooseTeam(side);
				minecraft.setScreen(null);
				return true;
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return !required;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
