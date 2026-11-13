package com.grandline.client.screen;

import com.grandline.client.ClientProfileCache;
import com.grandline.client.ui.GoldButton;
import com.grandline.client.ui.GrandLineUi;
import com.grandline.combat.ability.AbilityDef;
import com.grandline.fruit.Fruits;
import com.grandline.network.payload.FruitActionPayload;
import com.grandline.network.payload.OpenNpcPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * The dealer's catalogue: every fruit in the game, rarest last, with the current stock at the top. Each row shows the
 * rarity-framed fruit, name, rarity, price, its moves (key + mastery) and Buy, or "Out of stock". Scroll with the mouse
 * wheel. Buying sends a request; the server checks stock, distance and money.
 */
public final class FruitShopScreen extends Screen {
	private static final String[] KEYS = {"Z", "X", "C", "V", "F"};
	private static final int ROW = 52;
	private static final NumberFormat NUMBERS = NumberFormat.getIntegerInstance(Locale.US);

	private final OpenNpcPayload data;
	private final List<Fruits.FruitDef> fruits = new ArrayList<>();
	private int left;
	private int top;
	private int panelWidth;
	private int panelHeight;
	private double scroll;

	public FruitShopScreen(OpenNpcPayload data) {
		super(Component.translatable("screen.grandline.shop.title"));
		this.data = data;
		for (String id : data.stock()) {
			Fruits.FruitDef fruit = Fruits.get(id);
			if (fruit != null) {
				fruits.add(fruit);
			}
		}
		fruits.sort(Comparator.<Fruits.FruitDef>comparingInt(f -> inStock(f) ? 0 : 1).thenComparingInt(f -> f.rarity().ordinal()));
	}

	private boolean inStock(Fruits.FruitDef fruit) {
		return data.available().contains(fruit.id());
	}

	private long price(Fruits.FruitDef fruit) {
		int index = data.stock().indexOf(fruit.id());
		return index >= 0 && index < data.prices().size() ? data.prices().get(index) : 0;
	}

	@Override
	protected void init() {
		panelWidth = Math.min(380, width - 20);
		panelHeight = Math.min(height - 16, 340);
		left = width / 2 - panelWidth / 2;
		top = height / 2 - panelHeight / 2;
		addRenderableWidget(new GoldButton(left + panelWidth - 60, top + 2, 54, 16, Component.translatable("screen.grandline.back"),
				b -> NpcScreens.open(data)));
	}

	private int listTop() {
		return top + 40;
	}

	private int listBottom() {
		return top + panelHeight - 18;
	}

	private double maxScroll() {
		return Math.max(0, fruits.size() * ROW - (listBottom() - listTop()));
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		renderBackground(graphics);
		GrandLineUi.panel(graphics, font, left, top, panelWidth, panelHeight, title);
		long minutes = data.restockMillis() / 60_000;
		graphics.drawString(font, Component.translatable("screen.grandline.shop.stock_info", data.available().size(), fruits.size(),
				minutes / 60, minutes % 60), left + 8, top + 26, 0xC9D3E0);

		String owned = ClientProfileCache.profile() == null ? "" : ClientProfileCache.profile().powers().fruit();
		long money = ClientProfileCache.profile() == null ? 0 : ClientProfileCache.profile().currency();
		graphics.enableScissor(left + 2, listTop(), left + panelWidth - 2, listBottom());
		for (int i = 0; i < fruits.size(); i++) {
			int y = listTop() + i * ROW - (int) scroll;
			if (y + ROW < listTop() || y > listBottom()) {
				continue;
			}
			Fruits.FruitDef fruit = fruits.get(i);
			boolean stocked = inStock(fruit);
			graphics.fill(left + 4, y, left + panelWidth - 4, y + ROW - 4, stocked ? 0xF0303846 : 0xF0222833);
			GrandLineUi.tile(graphics, left + 8, y + 3, 42, 42, fruit.rarity().color(), false);
			graphics.pose().pushPose();
			graphics.pose().translate(left + 13, y + 8, 0);
			graphics.pose().scale(2, 2, 1);
			graphics.renderItem(new ItemStack(fruit.item()), 0, 0);
			graphics.pose().popPose();

			GrandLineUi.scaled(graphics, font, Component.translatable(fruit.power().translationKey()), left + 56, y + 4, 1.4f,
					stocked ? 0xFFFFFF : 0x9AA3AE, true);
			graphics.drawString(font, Component.translatable(fruit.rarity().translationKey()), left + 56, y + 19, fruit.rarity().color());
			graphics.drawString(font, "$" + NUMBERS.format(price(fruit)), left + 56, y + 31, stocked ? 0x6BE36B : 0x5F7A5F);

			// Buy button / status.
			int bx = left + 150;
			int by = y + 26;
			if (fruit.id().equals(owned)) {
				graphics.drawString(font, Component.translatable("screen.grandline.shop.owned"), bx, by + 4, 0xFFD24B);
			} else if (stocked) {
				boolean affordable = money >= price(fruit);
				boolean hover = mouseX >= bx && mouseX < bx + 56 && mouseY >= by && mouseY < by + 16 && mouseY >= listTop() && mouseY < listBottom();
				int face = !affordable ? 0xFF5A6270 : hover ? 0xFFFFD36A : GrandLineUi.GOLD;
				graphics.fill(bx - 1, by - 1, bx + 57, by + 17, GrandLineUi.BORDER);
				graphics.fill(bx, by, bx + 56, by + 16, face);
				graphics.drawCenteredString(font, Component.translatable("screen.grandline.shop.buy"), bx + 28, by + 4, 0xFFFFFF);
			} else {
				graphics.drawString(font, Component.translatable("screen.grandline.shop.out_of_stock"), bx, by + 4, 0xD46A6A);
			}

			int mx = left + panelWidth - 70;
			for (int m = 0; m < fruit.power().abilities().size(); m++) {
				AbilityDef ability = fruit.power().abilities().get(m);
				int my = y + 2 + m * 9;
				graphics.fill(mx, my, mx + 9, my + 8, 0xFF3A3F4A);
				graphics.drawString(font, KEYS[m], mx + 2, my, 0xFFFFFF);
				graphics.drawString(font, "★ " + ability.requiredMastery(), mx + 13, my, 0xFFD24B);
			}
		}
		graphics.disableScissor();

		// Scrollbar.
		double max = maxScroll();
		if (max > 0) {
			int trackTop = listTop();
			int trackHeight = listBottom() - listTop();
			int thumb = Math.max(20, trackHeight * trackHeight / (fruits.size() * ROW));
			int thumbY = trackTop + (int) ((trackHeight - thumb) * (scroll / max));
			graphics.fill(left + panelWidth - 6, trackTop, left + panelWidth - 3, trackTop + trackHeight, 0x60000000);
			graphics.fill(left + panelWidth - 6, thumbY, left + panelWidth - 3, thumbY + thumb, 0xFFC9D3E0);
		}
		graphics.drawString(font, Component.translatable("screen.grandline.shop.footer"), left + 6, top + panelHeight - 13, 0xC9D3E0);
		super.render(graphics, mouseX, mouseY, partialTick);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
		scroll = Mth.clamp(scroll - delta * ROW * 0.8, 0, maxScroll());
		return true;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (mouseY >= listTop() && mouseY < listBottom()) {
			for (int i = 0; i < fruits.size(); i++) {
				int y = listTop() + i * ROW - (int) scroll;
				int bx = left + 150;
				int by = y + 26;
				if (mouseX >= bx && mouseX < bx + 56 && mouseY >= by && mouseY < by + 16 && inStock(fruits.get(i))) {
					FruitDialogs.send(FruitActionPayload.SHOP_BUY, fruits.get(i).id(), data.entityId());
					return true;
				}
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
