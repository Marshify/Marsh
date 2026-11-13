package com.grandline.client.screen;

import com.grandline.client.GrandLineKeys;
import com.grandline.client.hud.LoadoutHotbar;
import com.grandline.client.ui.MarshButton;
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
 * Rearranges the four loadout slots. Drag a slot onto another to swap them; the icon, label and selection move
 * together and the hotbar updates live. Only the order changes: nothing in the vanilla inventory moves, nothing is
 * dropped and no move fires. The order is saved to the server when the editor closes (edit key or Escape).
 */
public final class LoadoutEditScreen extends Screen {
	private static final int SLOT = 36;
	private static final int GAP = 10;
	private static final int WIDTH = 4 * SLOT + 3 * GAP + 40;
	private static final int HEIGHT = 150;

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
		top = Math.max(4, height / 2 - HEIGHT / 2 - 20);
		slotsX = left + (WIDTH - (4 * SLOT + 3 * GAP)) / 2;
		slotsY = top + MarshUi.TITLE_HEIGHT + 30;
		addRenderableWidget(new MarshButton(left + WIDTH / 2 - 50, top + HEIGHT - 28, 100, 18,
				Component.translatable("screen.grandline.loadout_edit.reset"), MarshUi.Color.GRAY, () -> {
					LoadoutHotbar.setOrder(LoadoutOrder.DEFAULT);
					dragFrom = -1;
				}));
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

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		g.fill(0, 0, width, height, 0x50000000); // keep the world and hotbar visible
		MarshUi.window(g, font, left, top, WIDTH, HEIGHT, title);
		Component hint = Component.translatable("screen.grandline.loadout_edit.hint");
		g.drawCenteredString(font, hint, left + WIDTH / 2, top + MarshUi.TITLE_HEIGHT + 6, 0xA9B2C0);
		Component close = Component.translatable("screen.grandline.loadout_edit.close", GrandLineKeys.EDIT_LOADOUT.getTranslatedKeyMessage());
		g.drawCenteredString(font, close, left + WIDTH / 2, top + MarshUi.TITLE_HEIGHT + 16, 0x7E8796);

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
		super.render(g, mouseX, mouseY, partialTick);
		if (dragFrom >= 0) {
			LoadoutHotbar.Loadout entry = LoadoutHotbar.at(dragFrom);
			g.pose().pushPose();
			g.pose().translate(0, 0, 300);
			drawEntry(g, entry, LoadoutHotbar.icon(entry), (int) dragX - SLOT / 2, (int) dragY - SLOT / 2, entry == active, false);
			g.pose().popPose();
		}
	}

	/** Icon (2x) and, unless it is the dragged copy, the label under the slot. */
	private void drawEntry(GuiGraphics g, LoadoutHotbar.Loadout entry, ItemStack icon, int x, int y, boolean active, boolean withLabel) {
		if (!icon.isEmpty()) {
			g.pose().pushPose();
			g.pose().translate(x + 2, y + 2, 0);
			g.pose().scale(2, 2, 1);
			g.renderFakeItem(icon, 0, 0);
			g.pose().popPose();
		}
		if (!withLabel) {
			return;
		}
		Component label = icon.isEmpty() ? Component.translatable("screen.grandline.loadout_edit.empty", entry.displayName()) : entry.displayName();
		g.pose().pushPose();
		g.pose().translate(0, 0, 200);
		g.drawCenteredString(font, label, x + SLOT / 2, y + SLOT + 4, active ? 0xFFD25A : icon.isEmpty() ? 0x7E8796 : 0xFFFFFF);
		g.pose().popPose();
	}

	@Override
	public boolean mouseClicked(double mx, double my, int button) {
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
