package com.grandline.client.screen;

import com.grandline.client.GrandLineKeys;
import com.grandline.client.hud.LoadoutHotbar;
import com.grandline.client.ui.MarshUi;
import com.grandline.player.loadout.LoadoutOrder;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * Rearranges the four loadout slots, in a small translucent popup just above the hotbar. Drag a slot onto another to swap them; the icon, label and selection move
 * together and the hotbar updates live. Only the order changes: nothing in the vanilla inventory moves, nothing is
 * dropped and no move fires. The order is saved to the server when the editor closes (edit key or Escape).
 */
public final class LoadoutEditScreen extends Screen {
	private static final int SLOT = 24;
	private static final int GAP = 16;
	private static final int PAD = 8;
	private static final int WIDTH = 4 * SLOT + 3 * GAP + 2 * PAD;
	private static final int HEIGHT = 70;
	/** Gap between the popup and the bottom of the screen: clears the hotbar, hearts and its labels. */
	private static final int ABOVE_HOTBAR = 46;

	private final List<Integer> opened;
	private int left;
	private int top;
	private int slotsX;
	private int slotsY;
	private int dragFrom = -1;
	private double dragX;
	private double dragY;

	public LoadoutEditScreen() {
		super(Component.translatable("screen.grandline.loadout_edit.title"));
		this.opened = LoadoutHotbar.order();
	}

	@Override
	protected void init() {
		left = width / 2 - WIDTH / 2;
		top = height - ABOVE_HOTBAR - HEIGHT;
		slotsX = left + PAD;
		slotsY = top + 17;
	}

	private int slotAt(double mx, double my) {
		for (int pos = 0; pos < LoadoutOrder.SIZE; pos++) {
			int x = slotsX + pos * (SLOT + GAP);
			if (mx >= x && mx < x + SLOT && my >= slotsY && my < slotsY + SLOT) {
				return pos;
			}
		}
		return -1;
	}

	/** The small "Reset" link, centred at the bottom: {x, y, w, h}. */
	private int[] resetBox() {
		Component reset = Component.translatable("screen.grandline.loadout_edit.reset");
		int w = (int) Math.ceil(font.width(reset) * 0.75f);
		return new int[]{left + WIDTH / 2 - w / 2, top + HEIGHT - 10, w, 7};
	}

	private boolean overReset(double mx, double my) {
		int[] r = resetBox();
		return mx >= r[0] - 2 && mx < r[0] + r[2] + 2 && my >= r[1] - 2 && my < r[1] + r[3] + 2;
	}

	private void small(GuiGraphics g, Component text, int x, int y, int color) {
		g.pose().pushPose();
		g.pose().translate(x, y, 200);
		g.pose().scale(0.75f, 0.75f, 1);
		g.drawString(font, text, 0, 0, color, true);
		g.pose().popPose();
	}

	private void smallCentered(GuiGraphics g, Component text, int cx, int y, int color) {
		small(g, text, cx - Math.round(font.width(text) * 0.375f), y, color);
	}

	@Override
	public void renderBackground(GuiGraphics g) {
		// Nothing: the world stays fully visible behind the popup.
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		// A low-key translucent strip: soft dark fill with a faint edge, no title bar.
		g.fill(left + 1, top, left + WIDTH - 1, top + HEIGHT, 0x99101418);
		g.fill(left, top + 1, left + 1, top + HEIGHT - 1, 0x99101418);
		g.fill(left + WIDTH - 1, top + 1, left + WIDTH, top + HEIGHT - 1, 0x99101418);
		g.fill(left + 1, top, left + WIDTH - 1, top + 1, 0x40FFFFFF);
		Component hint = Component.translatable("screen.grandline.loadout_edit.hint").append(" \u00B7 ")
				.append(Component.translatable("screen.grandline.loadout_edit.close", GrandLineKeys.EDIT_LOADOUT.getTranslatedKeyMessage()));
		smallCentered(g, hint, left + WIDTH / 2, top + 4, 0xC8CED8);

		LoadoutHotbar.Loadout active = LoadoutHotbar.active();
		int hover = dragFrom >= 0 ? slotAt(mouseX, mouseY) : -1;
		for (int pos = 0; pos < LoadoutOrder.SIZE; pos++) {
			LoadoutHotbar.Loadout entry = LoadoutHotbar.at(pos);
			int x = slotsX + pos * (SLOT + GAP);
			ItemStack icon = LoadoutHotbar.icon(entry);
			MarshUi.abilitySlot(g, x, slotsY, SLOT, entry == active ? 1 : icon.isEmpty() ? 2 : 0);
			if (hover == pos && hover != dragFrom) {
				g.fill(x + 2, slotsY + 2, x + SLOT - 2, slotsY + SLOT - 2, 0x40FFD25A); // drop target
			}
			if (pos != dragFrom) {
				drawEntry(g, entry, icon, x, slotsY, entry == active, true);
			}
			LoadoutHotbar.keyNumber(g, font, String.valueOf(pos + 1), x + 1, slotsY - 4);
		}
		Component reset = Component.translatable("screen.grandline.loadout_edit.reset");
		int[] r = resetBox();
		boolean overR = overReset(mouseX, mouseY);
		small(g, overR ? reset.copy().withStyle(net.minecraft.ChatFormatting.UNDERLINE) : reset, r[0], r[1], overR ? 0xFFD25A : 0x9AA3B0);
		super.render(g, mouseX, mouseY, partialTick);
		if (dragFrom >= 0) {
			LoadoutHotbar.Loadout entry = LoadoutHotbar.at(dragFrom);
			g.pose().pushPose();
			g.pose().translate(0, 0, 300);
			drawEntry(g, entry, LoadoutHotbar.icon(entry), (int) dragX - SLOT / 2, (int) dragY - SLOT / 2, entry == active, false);
			g.pose().popPose();
		}
	}

	/** Icon (1.25x) and, unless it is the dragged copy, a small label under the slot (grey when empty). */
	private void drawEntry(GuiGraphics g, LoadoutHotbar.Loadout entry, ItemStack icon, int x, int y, boolean active, boolean withLabel) {
		if (!icon.isEmpty()) {
			g.pose().pushPose();
			g.pose().translate(x + 2, y + 2, 0);
			g.pose().scale(1.25f, 1.25f, 1);
			g.renderFakeItem(icon, 0, 0);
			g.pose().popPose();
		}
		if (!withLabel) {
			return;
		}
		smallCentered(g, entry.displayName(), x + SLOT / 2, y + SLOT + 3, active ? 0xFFD25A : icon.isEmpty() ? 0x7E8796 : 0xFFFFFF);
	}

	@Override
	public boolean mouseClicked(double mx, double my, int button) {
		if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && overReset(mx, my)) {
			LoadoutHotbar.setOrder(LoadoutOrder.DEFAULT);
			dragFrom = -1;
			minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.2f));
			return true;
		}
		int pos = slotAt(mx, my);
		if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && pos >= 0) {
			dragFrom = pos;
			dragX = mx;
			dragY = my;
			return true;
		}
		return super.mouseClicked(mx, my, button);
	}

	@Override
	public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
		if (dragFrom >= 0) {
			dragX = mx;
			dragY = my;
			return true;
		}
		return super.mouseDragged(mx, my, button, dx, dy);
	}

	@Override
	public boolean mouseReleased(double mx, double my, int button) {
		if (dragFrom >= 0) {
			int to = slotAt(mx, my);
			if (to >= 0 && to != dragFrom) {
				LoadoutHotbar.setOrder(LoadoutOrder.swap(LoadoutHotbar.order(), dragFrom, to));
				minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.2f));
			}
			dragFrom = -1;
			return true;
		}
		return super.mouseReleased(mx, my, button);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		// The edit key (Tab by default) closes the editor instead of moving widget focus.
		if (GrandLineKeys.EDIT_LOADOUT.matches(keyCode, scanCode)) {
			onClose();
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public void removed() {
		// Saved however the editor closes (key, Escape, or another screen replacing it).
		if (!LoadoutHotbar.order().equals(opened)) {
			LoadoutHotbar.saveOrder();
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
